package com.gearloop.app.data

import com.gearloop.app.data.repo.EvidenceRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

/**
 * The gear tag's QR code, written as the tag screen draws it and read back the
 * way the pickup camera reads a frame: from the brightness plane, with the row
 * padding a camera adds and the noise a real picture has. If this fails, the
 * scan at handover fails with it, and only "type the code" is left.
 */
class TagCodeTest {

    private val payload = EvidenceRepository.TAG_PREFIX + "SU-1196"

    /**
     * A 640×480 camera frame with the tag in it: a mid-grey, noisy room with the
     * code printed [qrPx] square at ([left], [top]), each row padded to
     * [rowStride], and the last row stopping at the width, as a camera plane does.
     */
    private fun frame(qrPx: Int = 260, left: Int = 190, top: Int = 110, rowStride: Int = 704, code: String? = payload): ByteArray {
        val width = 640
        val height = 480
        val random = Random(7)
        val plane = ByteArray(rowStride * (height - 1) + width)
        val matrix = code?.let { TagCode.matrix(it, qrPx) }
        for (y in 0 until height) for (x in 0 until width) {
            val inCode = matrix != null && x in left until left + qrPx && y in top until top + qrPx
            val base = when {
                !inCode -> 120
                matrix!!.get(x - left, y - top) -> 35
                else -> 215
            }
            plane[y * rowStride + x] = (base + random.nextInt(-14, 15)).coerceIn(0, 255).toByte()
        }
        return plane
    }

    @Test
    fun `a tag in a camera frame reads back as what it was printed with`() {
        assertEquals(payload, TagCode.read(TagCode.reader(), frame(), rowStride = 704, width = 640, height = 480))
    }

    @Test
    fun `an unpadded plane reads the same`() {
        assertEquals(payload, TagCode.read(TagCode.reader(), frame(rowStride = 640), rowStride = 640, width = 640, height = 480))
    }

    @Test
    fun `a small tag at the edge of the frame still reads`() {
        assertEquals(payload, TagCode.read(TagCode.reader(), frame(qrPx = 150, left = 470, top = 300), rowStride = 704, width = 640, height = 480))
    }

    @Test
    fun `a frame with no tag in it is nothing, not an error`() {
        assertNull(TagCode.read(TagCode.reader(), frame(code = null), rowStride = 704, width = 640, height = 480))
    }

    @Test
    fun `one reader serves frame after frame`() {
        val reader = TagCode.reader()
        assertNull(TagCode.read(reader, frame(code = null), rowStride = 704, width = 640, height = 480))
        assertEquals(payload, TagCode.read(reader, frame(), rowStride = 704, width = 640, height = 480))
        assertEquals(payload, TagCode.read(reader, frame(), rowStride = 704, width = 640, height = 480))
    }
}
