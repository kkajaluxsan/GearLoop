package com.gearloop.app.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.gearloop.app.data.PhotoStore
import com.gearloop.app.ui.icons.GearIcon
import com.gearloop.app.ui.icons.IconSize
import com.gearloop.app.ui.icons.Lucide
import com.gearloop.app.ui.theme.GearColor
import com.gearloop.app.ui.theme.GearShape
import com.gearloop.app.ui.theme.GearSurface
import com.gearloop.app.ui.theme.GearText
import com.gearloop.app.ui.theme.GearType
import com.gearloop.app.ui.theme.Space
import com.gearloop.app.ui.theme.Metrics
import com.gearloop.app.ui.theme.Text
import com.google.zxing.MultiFormatReader
import com.gearloop.app.data.TagCode
import java.io.File
import java.util.concurrent.Executors

/*
 * The camera, as the app uses it: a live viewfinder inside the screen's own
 * frame (so the guide outline sits over the real picture), a QR reader for gear
 * tags, a "take or choose" photo picker, the phone's location for geotagging,
 * and the signature pad. Everything taken is written to the app's private
 * storage by PhotoStore and never leaves the phone.
 */

// ============================================================ permissions

private fun Context.has(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

/** Whether the camera may be used, and how to ask. Re-checked when the screen returns. */
@Stable
class CameraPermission internal constructor(granted: Boolean, private val ask: () -> Unit) {
    var granted by mutableStateOf(granted)
        internal set
    var refusedOnce by mutableStateOf(false)
        internal set
    fun request() = ask()
}

@Composable
fun rememberCameraPermission(): CameraPermission {
    val context = LocalContext.current
    var permission: CameraPermission? = null
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        permission?.granted = ok
        if (!ok) permission?.refusedOnce = true
    }
    permission = remember { CameraPermission(context.has(Manifest.permission.CAMERA)) { launcher.launch(Manifest.permission.CAMERA) } }
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) permission.granted = context.has(Manifest.permission.CAMERA)
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }
    return permission
}

// ============================================================ the viewfinder

/** What a screen holds to take a picture with the viewfinder it is showing. */
@Stable
class CameraShutter {
    // State, not a plain field: the shutter button reads [ready], and has to
    // light up when the camera binds, not on whatever recomposes it next.
    internal var controller: LifecycleCameraController? by mutableStateOf(null)
    var busy by mutableStateOf(false)
        private set

    val ready: Boolean get() = controller != null

    /** Take a photo into a new file; [onDone] gets the file, or null if it failed. */
    fun take(context: Context, prefix: String, onDone: (File?) -> Unit) {
        val c = controller ?: return onDone(null)
        if (busy) return
        busy = true
        val file = PhotoStore.newFile(context, prefix)
        try {
            c.takePicture(
                ImageCapture.OutputFileOptions.Builder(file).build(),
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        busy = false
                        onDone(file)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        busy = false
                        file.delete()
                        onDone(null)
                    }
                },
            )
        } catch (_: IllegalStateException) {
            // Tapped before the camera has finished starting: a failed shot, not a crash.
            busy = false
            file.delete()
            onDone(null)
        }
    }
}

@Composable
fun rememberCameraShutter(): CameraShutter = remember { CameraShutter() }

/**
 * The live camera picture, filling its slot. With [onQr] it also reads any QR
 * code in view and hands over its text. Without camera permission it shows why
 * the camera is needed and asks for it, so the screen around it still works.
 */
@Composable
fun CameraViewfinder(
    modifier: Modifier = Modifier,
    shutter: CameraShutter? = null,
    front: Boolean = false,
    onQr: ((String) -> Unit)? = null,
    reason: String = "The camera records the condition of the gear, so both of you have the same photos if anything is disputed.",
) {
    val context = LocalContext.current
    val permission = rememberCameraPermission()
    val hasCamera = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    Box(modifier.background(GearColor.Viewport)) {
        when {
            !hasCamera -> CameraMessage(Lucide.Camera, "This phone has no camera", "Photos can be chosen from the gallery instead.", null, null)
            !permission.granted -> {
                val blocked = permission.refusedOnce && context.activity()?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) == false
                CameraMessage(
                    Lucide.Camera,
                    if (blocked) "Camera access is off" else "Allow the camera",
                    if (blocked) "Turn it on for GearLoop in your phone's settings. $reason" else reason,
                    if (blocked) "Open settings" else "Allow camera",
                ) {
                    if (blocked) {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                        )
                    } else {
                        permission.request()
                    }
                }
            }
            else -> LivePreview(Modifier.fillMaxSize(), shutter, front, onQr)
        }
    }
}

@Composable
private fun LivePreview(modifier: Modifier, shutter: CameraShutter?, front: Boolean, onQr: ((String) -> Unit)?) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val onQrLatest by rememberUpdatedState(onQr)
    val controller = remember(front) {
        LifecycleCameraController(context).apply {
            cameraSelector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(CameraController.IMAGE_CAPTURE or (if (onQr != null) CameraController.IMAGE_ANALYSIS else 0))
        }
    }
    DisposableEffect(controller, owner) {
        val executor = Executors.newSingleThreadExecutor()
        if (onQr != null) {
            val reader = TagCode.reader()
            var frame = 0
            controller.setImageAnalysisAnalyzer(executor) { image ->
                frame++
                val text = if (frame % 3 == 0) decodeQr(reader, image) else null
                image.close()
                if (text != null) ContextCompat.getMainExecutor(context).execute { onQrLatest?.invoke(text) }
            }
        }
        controller.bindToLifecycle(owner)
        // A phone with no front camera would show black for the selfie, with a
        // shutter that could never save; it is taken on the back camera instead.
        if (front) {
            controller.initializationFuture.addListener({
                val noFront = runCatching { !controller.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) }.getOrDefault(false)
                if (noFront) controller.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            }, ContextCompat.getMainExecutor(context))
        }
        shutter?.controller = controller
        onDispose {
            shutter?.controller = null
            controller.clearImageAnalysisAnalyzer()
            controller.unbind()
            executor.shutdown()
        }
    }
    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                this.controller = controller
            }
        },
        update = { it.controller = controller },
        modifier = modifier.semantics { contentDescription = "Camera viewfinder" },
    )
}

/** A QR code in this frame's brightness plane, or null. */
private fun decodeQr(reader: MultiFormatReader, image: ImageProxy): String? {
    val plane = image.planes.firstOrNull() ?: return null
    val buffer = plane.buffer.duplicate()
    val data = ByteArray(buffer.remaining()).also { buffer.get(it) }
    return TagCode.read(reader, data, plane.rowStride, image.width, image.height)
}

@Composable
private fun CameraMessage(icon: com.gearloop.app.ui.icons.LucideIcon, title: String, body: String, action: String?, onAction: (() -> Unit)?) {
    Column(
        Modifier.fillMaxSize().padding(Space.l),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GearIcon(icon, null, size = IconSize.xl, tint = GearColor.HiVis500)
        VGap(Space.sm)
        Text(title, style = GearType.headingM, color = GearText.onDark, textAlign = TextAlign.Center)
        VGap(Space.s)
        Text(body, style = GearType.bodyS, color = GearColor.Chalk300, textAlign = TextAlign.Center)
        if (action != null && onAction != null) {
            VGap(Space.m)
            GearButton(action, onAction, size = ButtonSize.M, icon = Lucide.Camera)
        }
    }
}

// ============================================================ take or choose a photo

/**
 * A photo from the camera or the gallery, for anything that is not the guided
 * condition check: listing photos, claim evidence, closet items, verification.
 * Call [open]; the result arrives in the callbacks passed to [rememberPhotoPicker].
 */
@Stable
class PhotoPicker internal constructor(private val show: () -> Unit) {
    fun open() = show()
}

@Composable
fun rememberPhotoPicker(
    title: String,
    onTaken: (File) -> Unit,
    onChosen: ((Uri) -> Unit)? = null,
    front: Boolean = false,
): PhotoPicker {
    var choosing by rememberSaveable { mutableStateOf(false) }
    var shooting by rememberSaveable { mutableStateOf(false) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onChosen?.invoke(uri)
    }
    val picker = remember { PhotoPicker { if (onChosen == null) shooting = true else choosing = true } }

    if (choosing) {
        GearDialog(
            title = title,
            onDismiss = { choosing = false },
            confirmLabel = "Take a photo",
            onConfirm = { choosing = false; shooting = true },
            dismissLabel = "Cancel",
            destructiveLabel = null,
        ) {
            Text("Use the camera, or pick a photo you already have.", style = GearType.bodyS, color = GearText.secondary)
            VGap(Space.sm)
            GearButton(
                "Choose from gallery",
                {
                    choosing = false
                    gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                Modifier.fillMaxWidth(),
                type = ButtonType.SECONDARY,
                size = ButtonSize.M,
                icon = Lucide.Image,
            )
        }
    }
    if (shooting) {
        CameraCaptureDialog(title = title, front = front, onDismiss = { shooting = false }) { file ->
            shooting = false
            onTaken(file)
        }
    }
    return picker
}

/** A full-screen camera with one shutter, for a single photo. */
@Composable
fun CameraCaptureDialog(title: String, front: Boolean = false, onDismiss: () -> Unit, onTaken: (File) -> Unit) {
    val context = LocalContext.current
    val shutter = rememberCameraShutter()
    var failed by rememberSaveable { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize().background(GearSurface.darkest).statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = Space.sm, vertical = Space.s), verticalAlignment = Alignment.CenterVertically) {
                GearIconButton(Lucide.X, "Close camera", onDismiss, tone = IconButtonTone.DARK, iconSize = IconSize.l)
                Text(title, style = GearType.headingM, color = GearText.onDark, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Spacer(Modifier.width(Metrics.tapTarget))
            }
            CameraViewfinder(Modifier.fillMaxWidth().weight(1f), shutter = shutter, front = front, reason = "The camera takes this photo. It stays on this phone.")
            if (failed) {
                Text(
                    "That photo didn't save. Try again.",
                    style = GearType.bodyS,
                    color = GearColor.Chalk300,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = Space.s),
                )
            }
            Box(Modifier.fillMaxWidth().padding(Space.m), contentAlignment = Alignment.Center) {
                Shutter("Take photo", enabled = shutter.ready && !shutter.busy) {
                    failed = false
                    shutter.take(context, "img") { file -> if (file != null) onTaken(file) else failed = true }
                }
            }
        }
    }
}

/** The round hi-vis shutter button. */
@Composable
fun Shutter(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .size(72.dp)
            .clip(CircleShape)
            .border(4.dp, GearSurface.overImage.copy(alpha = 0.25f), CircleShape)
            .background(if (enabled) GearColor.HiVis500 else GearColor.Chalk500)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        GearIcon(Lucide.Camera, label, size = IconSize.xl, tint = GearText.onHiVis)
    }
}

// ============================================================ location

/**
 * Where the phone is, for the record a condition photo carries — asked for
 * once, used only if allowed. Null until a fix arrives or when it is refused.
 */
@SuppressLint("MissingPermission")
@Composable
fun rememberLocationFix(): Pair<Double, Double>? {
    val context = LocalContext.current
    var fix by rememberSaveable { mutableStateOf<List<Double>?>(null) }
    var asked by rememberSaveable { mutableStateOf(false) }
    var granted by remember { mutableStateOf(context.has(Manifest.permission.ACCESS_FINE_LOCATION) || context.has(Manifest.permission.ACCESS_COARSE_LOCATION)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        granted = result.values.any { it }
    }
    LaunchedEffect(Unit) {
        if (!granted && !asked) {
            asked = true
            launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
    DisposableEffect(granted) {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val signal = androidx.core.os.CancellationSignal()
        if (granted && manager != null) {
            val last = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
                .mapNotNull { p -> runCatching { manager.getLastKnownLocation(p) }.getOrNull() }
                .maxByOrNull(Location::getTime)
            if (last != null) fix = listOf(last.latitude, last.longitude)
            val provider = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).firstOrNull { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
            if (provider != null) {
                runCatching {
                    LocationManagerCompat.getCurrentLocation(manager, provider, signal, ContextCompat.getMainExecutor(context)) { loc ->
                        if (loc != null) fix = listOf(loc.latitude, loc.longitude)
                    }
                }
            }
        }
        onDispose { signal.cancel() }
    }
    return fix?.let { it[0] to it[1] }
}

// ============================================================ signatures

private val strokesSaver = listSaver<SnapshotStateList<List<Offset>>, FloatArray>(
    save = { strokes -> strokes.map { s -> FloatArray(s.size * 2) { i -> if (i % 2 == 0) s[i / 2].x else s[i / 2].y } } },
    restore = { saved -> saved.map { a -> List(a.size / 2) { i -> Offset(a[i * 2], a[i * 2 + 1]) } }.toMutableStateList() },
)

/**
 * Sign with a finger. [onSigned] gets the drawing as a bitmap the size of the
 * pad, ink on a clear background, ready to be saved with the record.
 */
@Composable
fun SignaturePad(
    signer: String,
    onSigned: (android.graphics.Bitmap) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strokes = rememberSaveable(saver = strokesSaver) { mutableListOf<List<Offset>>().toMutableStateList() }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var padSize by remember { mutableStateOf(IntSize.Zero) }
    val ink = GearColor.Pine900
    val line = GearColor.Chalk300
    val inkArgb = ink.toArgb()
    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(GearShape.md)
                .background(GearColor.Chalk50)
                .border(com.gearloop.app.ui.theme.Border.medium, GearSurface.borderStrong, GearShape.md)
                .onSizeChanged { padSize = it }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { current = listOf(it) },
                        onDrag = { change, _ -> current = current + change.position },
                        onDragEnd = { if (current.size > 1) strokes.add(current); current = emptyList() },
                        onDragCancel = { current = emptyList() },
                    )
                }
                .semantics { contentDescription = "Signature pad for $signer" },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawLine(line, Offset(24f, size.height * 0.75f), Offset(size.width - 24f, size.height * 0.75f), 2f)
                (strokes + listOf(current)).forEach { s ->
                    if (s.size < 2) return@forEach
                    val path = Path().apply { moveTo(s[0].x, s[0].y); s.drop(1).forEach { lineTo(it.x, it.y) } }
                    drawPath(path, ink, style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
            if (strokes.isEmpty() && current.isEmpty()) {
                Text(
                    "$signer, sign here",
                    style = GearType.bodyM,
                    color = GearText.tertiary,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
        VGap(Space.s)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            GearButton("Clear", { strokes.clear() }, type = ButtonType.GHOST, size = ButtonSize.M, enabled = strokes.isNotEmpty())
            Spacer(Modifier.weight(1f))
            GearButton("Cancel", onCancel, type = ButtonType.SECONDARY, size = ButtonSize.M)
            GearButton(
                "Sign",
                {
                    val w = padSize.width.coerceAtLeast(1)
                    val h = padSize.height.coerceAtLeast(1)
                    val bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    val paint = android.graphics.Paint().apply {
                        color = inkArgb
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 3.5f * (w / 360f).coerceIn(1.5f, 4f)
                        strokeCap = android.graphics.Paint.Cap.ROUND
                        strokeJoin = android.graphics.Paint.Join.ROUND
                        isAntiAlias = true
                    }
                    strokes.forEach { s ->
                        val p = android.graphics.Path()
                        p.moveTo(s[0].x, s[0].y)
                        s.drop(1).forEach { p.lineTo(it.x, it.y) }
                        canvas.drawPath(p, paint)
                    }
                    onSigned(bitmap)
                },
                size = ButtonSize.M,
                enabled = strokes.sumOf { it.size } >= 8,
            )
        }
    }
}

// ============================================================ QR codes

/** [text] drawn as a QR code, dark on light, for a printable gear tag. */
@Composable
fun QrCode(text: String, modifier: Modifier = Modifier, contentDescription: String? = null) {
    val dark = GearColor.Pine900.toArgb()
    val light = GearColor.Chalk000.toArgb()
    BoxWithConstraints(modifier) {
        val px = constraints.maxWidth.coerceIn(128, 1024)
        val bitmap = remember(text, px, dark, light) { qrBitmap(text, px, dark, light) }
        Image(bitmap, contentDescription, Modifier.fillMaxSize())
    }
}

/** The pixels of a QR code, for drawing or for sharing as an image. */
fun qrBitmap(text: String, px: Int, dark: Int, light: Int): ImageBitmap = qrAndroidBitmap(text, px, dark, light).asImageBitmap()

fun qrAndroidBitmap(text: String, px: Int, dark: Int, light: Int): android.graphics.Bitmap {
    val matrix = TagCode.matrix(text, px)
    val pixels = IntArray(px * px) { i -> if (matrix.get(i % px, i / px)) dark else light }
    return android.graphics.Bitmap.createBitmap(pixels, px, px, android.graphics.Bitmap.Config.ARGB_8888)
}
