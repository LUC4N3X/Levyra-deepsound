package com.luc4n3x.levyra.desktop.player

import com.luc4n3x.levyra.desktop.core.catalog.AppleArtistCanvas
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class VlcArtistCanvasNativeTest {
    @Test
    fun appleArtistCanvasProducesFramesThroughNativeVlc() = runBlocking {
        assumeTrue(System.getenv("LEVYRA_TEST_APPLE_CANVAS") == "1")
        assertTrue("VLC is required for the artist Canvas", VlcNativeLocator.discover().available)
        val clip = requireNotNull(AppleArtistCanvas.download("Billie Eilish", "US")) { "Apple artist Canvas unavailable" }
        try {
            val rendered = CountDownLatch(3)
            val player = requireNotNull(VlcArtistCanvas.open(clip, "", null) { rendered.countDown() })
            player.use {
                assertTrue("Expected rendered Canvas frames", rendered.await(12, TimeUnit.SECONDS))
            }
        } finally {
            Files.deleteIfExists(clip)
        }
    }
}
