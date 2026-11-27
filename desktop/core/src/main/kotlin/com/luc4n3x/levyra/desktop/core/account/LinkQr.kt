package com.luc4n3x.levyra.desktop.core.account

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

class QrMatrix internal constructor(val size: Int, private val cells: BooleanArray) {
    operator fun get(x: Int, y: Int): Boolean = x in 0 until size && y in 0 until size && cells[y * size + x]

    fun isFinder(x: Int, y: Int): Boolean {
        val far = size - FINDER
        return (x < FINDER && y < FINDER) || (x >= far && y < FINDER) || (x < FINDER && y >= far)
    }

    companion object {
        const val FINDER = 7

        fun encode(text: String): QrMatrix {
            val matrix = QRCodeWriter().encode(
                text, BarcodeFormat.QR_CODE, 0, 0,
                mapOf(EncodeHintType.MARGIN to 0, EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M)
            )
            val size = matrix.width
            return QrMatrix(size, BooleanArray(size * size) { matrix[it % size, it / size] })
        }
    }
}
