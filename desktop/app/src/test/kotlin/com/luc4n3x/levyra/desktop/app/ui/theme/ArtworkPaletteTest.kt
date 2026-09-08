package com.luc4n3x.levyra.desktop.app.ui.theme

import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ArtworkPaletteTest {

    @Test
    fun windowsArtworkPathIsResolvedLocallyInsteadOfCrashing() = runBlocking {
        assertNull(ArtworkPalette.accentFor("C:\\Users\\Luca\\Music\\Album\\missing-cover.jpg"))
    }

    @Test
    fun missingBlankAndUnsupportedReferencesFallBack() = runBlocking {
        assertNull(ArtworkPalette.accentFor(""))
        assertNull(ArtworkPalette.accentFor("   "))
        assertNull(ArtworkPalette.accentFor("/levyra/does-not-exist/cover.jpg"))
        assertNull(ArtworkPalette.accentFor("data:image/png;base64,iVBORw0KGgo="))
    }

    @Test
    fun localArtworkIsDecodedFromPathAndFileUri() = runBlocking {
        val cover = writeCover(Files.createTempFile("levyra-cover", ".png"))
        try {
            assertNotNull(ArtworkPalette.accentFor(cover.toString()))
            assertNotNull(ArtworkPalette.accentFor(cover.toUri().toString()))
        } finally {
            Files.deleteIfExists(cover)
        }
    }

    @Test
    fun anUnreadableCoverDoesNotPinTheAccentToTheFallback() = runBlocking {
        val cover = Files.createTempDirectory("levyra-artwork").resolve("cover.png")
        assertNull(ArtworkPalette.accentFor(cover.toString()))
        try {
            writeCover(cover)
            assertNotNull(ArtworkPalette.accentFor(cover.toString()))
        } finally {
            Files.deleteIfExists(cover)
        }
    }

    private fun writeCover(target: Path): Path {
        val image = BufferedImage(24, 24, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.color = java.awt.Color(46, 125, 50)
        graphics.fillRect(0, 0, image.width, image.height)
        graphics.dispose()
        ImageIO.write(image, "png", target.toFile())
        return target
    }
}
