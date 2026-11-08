package com.luc4n3x.levyra.feature.jam

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

internal object JamInvitationQr {
    fun encode(invitation: String): BitMatrix {
        val code = requireNotNull(JamSessionCode.parse(invitation))
        return QRCodeWriter().encode(
            code.deepLink(), BarcodeFormat.QR_CODE, 0, 0,
            mapOf(
                EncodeHintType.MARGIN to 4,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
            )
        )
    }
}
