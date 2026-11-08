package com.luc4n3x.levyra.feature.jam

import com.google.zxing.qrcode.decoder.Decoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class JamInvitationQrTest {
    @Test
    fun invitationQrDecodesToExistingJoinableDeepLink() {
        val code = JamSessionCode("192.168.1.42", 42424, "00112233445566778899aabbccddeeff")
        val matrix = JamInvitationQr.encode(code.deepLink())
        val decoded = Decoder().decode(matrix.enclosingRectangle.let { bounds ->
            com.google.zxing.common.BitMatrix(bounds[2], bounds[3]).also { cropped ->
                for (y in 0 until bounds[3]) for (x in 0 until bounds[2]) {
                    if (matrix[x + bounds[0], y + bounds[1]]) cropped.set(x, y)
                }
            }
        }).text

        assertEquals(code.deepLink(), decoded)
        assertEquals(code, JamSessionCode.parse(decoded))
        for (margin in 0 until 4) {
            for (index in 0 until matrix.width) {
                assertFalse(matrix[margin, index])
                assertFalse(matrix[index, margin])
                assertFalse(matrix[matrix.width - 1 - margin, index])
                assertFalse(matrix[index, matrix.height - 1 - margin])
            }
        }
    }

    @Test
    fun manualCodeProducesTheSameInvitationQr() {
        val code = JamSessionCode("10.0.0.8", 54321, "aabbccddeeff00112233445566778899")
        assertEquals(JamInvitationQr.encode(code.deepLink()), JamInvitationQr.encode(code.formatted()))
    }

    @Test(expected = IllegalArgumentException::class)
    fun unrelatedQrPayloadCannotBeSharedAsAnInvitation() {
        JamInvitationQr.encode("https://example.com/")
    }
}
