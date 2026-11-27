package com.luc4n3x.levyra.desktop.core.account

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Test

class LinkQrTest {
    @Test
    fun styledCodeStillScans() {
        val host = DesktopYoutubeMusicAccounts(Files.createTempDirectory("levyra-qr")) { "en" }.newLinkHost()
        host.start()
        val code = (host.state.value as YoutubeMusicLinkState.Waiting).code
        host.dispose()
        val matrix = QrMatrix.encode(code)
        listOf(3.0, 4.7).forEach { cell ->
            val image = render(matrix, cell)
            val result = QRCodeReader().decode(
                BinaryBitmap(HybridBinarizer(RGBLuminanceSource(image.width, image.height, image.getRGB(0, 0, image.width, image.height, null, 0, image.width)))),
                mapOf(DecodeHintType.TRY_HARDER to true)
            )
            assertEquals(code, result.text)
        }
    }

    private fun render(matrix: QrMatrix, cell: Double): BufferedImage {
        val quiet = cell * 5
        val side = (matrix.size * cell + quiet * 2).toInt()
        val image = BufferedImage(side, side, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.color = Color.WHITE
        g.fillRect(0, 0, side, side)
        val ink = Color(0x0B, 0x0E, 0x14)
        val dot = cell * 0.86
        val inset = (cell - dot) / 2
        g.color = ink
        for (y in 0 until matrix.size) for (x in 0 until matrix.size) {
            if (!matrix[x, y] || matrix.isFinder(x, y)) continue
            g.fill(RoundRectangle2D.Double(quiet + x * cell + inset, quiet + y * cell + inset, dot, dot, dot * 0.64, dot * 0.64))
        }
        val finder = cell * QrMatrix.FINDER
        val far = quiet + (matrix.size - QrMatrix.FINDER) * cell
        listOf(quiet to quiet, far to quiet, quiet to far).forEach { (ox, oy) ->
            g.color = ink
            g.fill(RoundRectangle2D.Double(ox, oy, finder, finder, cell * 3.6, cell * 3.6))
            g.color = Color.WHITE
            g.fill(RoundRectangle2D.Double(ox + cell, oy + cell, finder - cell * 2, finder - cell * 2, cell * 2.4, cell * 2.4))
            g.color = ink
            g.fill(RoundRectangle2D.Double(ox + cell * 2, oy + cell * 2, finder - cell * 4, finder - cell * 4, cell * 1.8, cell * 1.8))
        }
        g.dispose()
        return image
    }
}
