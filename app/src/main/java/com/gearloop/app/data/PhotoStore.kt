package com.gearloop.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max

/**
 * Pictures taken or chosen on this phone: condition frames, listing photos,
 * claim evidence, signatures.
 *
 * Every picture is written once, already upright and no wider than
 * [MAX_EDGE] pixels, into the app's private files. The database stores the
 * key ([Photos.FILE_PREFIX] + the path relative to `filesDir`), never an
 * absolute path, so a restored or moved data directory still resolves.
 * Nothing here leaves the device.
 */
object PhotoStore {

    /** The longest edge a stored photo keeps. Enough for a full-width hero at 3×. */
    const val MAX_EDGE = 1600

    private const val DIR = "photos"
    private const val QUALITY = 85

    /** Decoded bitmaps, keyed by photo and the size they were decoded for. */
    private val cache = object : LruCache<String, ImageBitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }

    fun directory(context: Context): File = File(context.filesDir, DIR).apply { mkdirs() }

    /** A fresh file for a new picture. Nothing is written to it yet. */
    fun newFile(context: Context, prefix: String = "img"): File =
        File(directory(context), "${prefix}_${UUID.randomUUID().toString().take(12)}.jpg")

    fun keyFor(file: File): String = Photos.FILE_PREFIX + DIR + "/" + file.name

    fun fileFor(context: Context, key: String): File? =
        if (Photos.isFile(key)) File(context.filesDir, key.removePrefix(Photos.FILE_PREFIX)) else null

    /**
     * A camera capture, made upright and resized in place. Returns its key, or
     * null if the file is not a readable image.
     */
    suspend fun adoptCapture(file: File): String? = withContext(Dispatchers.IO) {
        val upright = decodeUpright(file.absolutePath, MAX_EDGE) ?: return@withContext null
        FileOutputStream(file).use { upright.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
        upright.recycle()
        keyFor(file)
    }

    /** A picture chosen from the gallery, copied into the app's own storage. */
    suspend fun importUri(context: Context, uri: Uri, prefix: String = "img"): String? = withContext(Dispatchers.IO) {
        val tmp = File(context.cacheDir, "import_${UUID.randomUUID()}.jpg")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tmp).use { input.copyTo(it) }
            } ?: return@withContext null
            val upright = decodeUpright(tmp.absolutePath, MAX_EDGE) ?: return@withContext null
            val out = newFile(context, prefix)
            FileOutputStream(out).use { upright.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
            upright.recycle()
            keyFor(out)
        } catch (_: Exception) {
            null
        } finally {
            tmp.delete()
        }
    }

    /** A bitmap drawn by the app itself — a signature — saved as a PNG. */
    suspend fun saveBitmap(context: Context, bitmap: Bitmap, prefix: String): String = withContext(Dispatchers.IO) {
        val out = File(directory(context), "${prefix}_${UUID.randomUUID().toString().take(12)}.png")
        FileOutputStream(out).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        Photos.FILE_PREFIX + DIR + "/" + out.name
    }

    fun delete(context: Context, key: String?) {
        if (key == null) return
        fileFor(context, key)?.delete()
        cache.snapshot().keys.filter { it.startsWith("$key@") }.forEach(cache::remove)
    }

    /**
     * The picture for [key], decoded for a slot about [targetPx] wide. Cached,
     * so scrolling back to a photo does not decode it again.
     */
    suspend fun load(context: Context, key: String, targetPx: Int): ImageBitmap? {
        val bucket = when {
            targetPx <= 256 -> 256
            targetPx <= 640 -> 640
            else -> MAX_EDGE
        }
        val cacheKey = "$key@$bucket"
        cache.get(cacheKey)?.let { return it }
        val file = fileFor(context, key) ?: return null
        return withContext(Dispatchers.IO) {
            if (!file.exists()) return@withContext null
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, options)
            options.inSampleSize = sampleSize(options.outWidth, options.outHeight, bucket)
            options.inJustDecodeBounds = false
            BitmapFactory.decodeFile(file.absolutePath, options)?.asImageBitmap()?.also { cache.put(cacheKey, it) }
        }
    }

    /** The result of the capture-time check on a frame (FR-08). */
    enum class FrameCheck(val ok: Boolean, val label: String) {
        GOOD(true, "Sharp · well lit"),
        DARK(false, "Too dark — retake"),
        BLURRY(false, "Blurry — retake"),
    }

    /**
     * Brightness and sharpness, measured on a small grey copy of the frame.
     * Sharpness is the variance of a Laplacian; the thresholds are set to catch
     * a frame shot in a pocket or mid-swing, not to second-guess a plain wall.
     */
    suspend fun check(context: Context, key: String): FrameCheck = withContext(Dispatchers.IO) {
        val file = fileFor(context, key) ?: return@withContext FrameCheck.GOOD
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        options.inSampleSize = sampleSize(options.outWidth, options.outHeight, 256)
        options.inJustDecodeBounds = false
        val small = BitmapFactory.decodeFile(file.absolutePath, options) ?: return@withContext FrameCheck.GOOD
        val w = small.width
        val h = small.height
        val pixels = IntArray(w * h)
        small.getPixels(pixels, 0, w, 0, 0, w, h)
        small.recycle()
        val grey = IntArray(pixels.size) { i ->
            val c = pixels[i]
            ((c shr 16 and 0xFF) * 299 + (c shr 8 and 0xFF) * 587 + (c and 0xFF) * 114) / 1000
        }
        val mean = grey.average()
        if (mean < 28) return@withContext FrameCheck.DARK
        var sum = 0.0
        var sumSq = 0.0
        var n = 0
        for (y in 1 until h - 1) for (x in 1 until w - 1) {
            val i = y * w + x
            val lap = (grey[i - 1] + grey[i + 1] + grey[i - w] + grey[i + w] - 4 * grey[i]).toDouble()
            sum += lap
            sumSq += lap * lap
            n++
        }
        if (n == 0) return@withContext FrameCheck.GOOD
        val variance = sumSq / n - (sum / n) * (sum / n)
        if (variance < 6.0) FrameCheck.BLURRY else FrameCheck.GOOD
    }

    private fun sampleSize(width: Int, height: Int, target: Int): Int {
        var sample = 1
        val longest = max(width, height)
        while (longest / (sample * 2) >= target) sample *= 2
        return sample
    }

    /** Decode with EXIF rotation applied and the longest edge at most [maxEdge]. */
    private fun decodeUpright(path: String, maxEdge: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxEdge) }
        val decoded = BitmapFactory.decodeFile(path, options) ?: return null
        val rotation = runCatching {
            when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)
        val longest = max(decoded.width, decoded.height)
        val scale = if (longest > maxEdge) maxEdge.toFloat() / longest else 1f
        if (rotation == 0f && scale == 1f) return decoded
        val matrix = Matrix().apply {
            if (scale != 1f) postScale(scale, scale)
            if (rotation != 0f) postRotate(rotation)
        }
        val out = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        if (out !== decoded) decoded.recycle()
        return out
    }

}
