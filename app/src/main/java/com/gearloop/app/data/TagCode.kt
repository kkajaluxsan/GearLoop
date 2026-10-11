package com.gearloop.app.data

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter

/**
 * The QR code on a gear tag, both ways: drawn for printing, and read back off
 * a camera frame at pickup. Kept apart from the camera and from Android's
 * bitmaps so that what a tag says, and whether a frame of it can be read, is
 * tested on its own (TagCodeTest).
 */
object TagCode {

    /** The modules of a QR code for [text], [px] square, with a one-module quiet zone. */
    fun matrix(text: String, px: Int): BitMatrix =
        QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, px, px, mapOf(EncodeHintType.MARGIN to 1))

    /** A reader for QR codes only, set to try hard: a frame comes every few dozen milliseconds. */
    fun reader(): MultiFormatReader = MultiFormatReader().apply {
        setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE), DecodeHintType.TRY_HARDER to true))
    }

    /**
     * The text of a QR code in a camera frame's brightness (Y) plane, or null.
     * [rowStride] is the plane's bytes per row, which a camera often pads past
     * [width]; the last row may stop at [width], so [luminance] can be short of
     * a full stride by the padding.
     */
    fun read(reader: MultiFormatReader, luminance: ByteArray, rowStride: Int, width: Int, height: Int): String? {
        val w = width.coerceAtMost(rowStride)
        return try {
            val source = PlanarYUVLuminanceSource(luminance, rowStride, height, 0, 0, w, height, false)
            reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text
        } catch (_: Exception) {
            null
        } finally {
            reader.reset()
        }
    }
}
