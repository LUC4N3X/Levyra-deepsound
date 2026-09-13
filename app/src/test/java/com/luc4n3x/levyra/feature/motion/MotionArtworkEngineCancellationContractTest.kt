package com.luc4n3x.levyra.feature.motion

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionArtworkEngineCancellationContractTest {
    @Test
    fun `network policy block cancels pending provider lookups before leaving the scope`() {
        val engine = readSource("feature/motion/MotionArtworkEngine.kt").filterNot(Char::isWhitespace)

        assertTrue(
            engine.contains(
                "if(!shouldPublishMotionArtwork(networkPolicy.canResolveCurrent())){" +
                    "lookups.forEach{it.cancel()}" +
                    "return@supervisorScope" +
                    "}"
            )
        )
        assertTrue(!engine.contains("canResolveCurrent()))return@supervisorScope"))
    }

    private fun readSource(relativePath: String): String =
        Files.readString(sourceFile(relativePath)).replace("\r\n", "\n")

    private fun sourceFile(relativePath: String): Path = sequenceOf(
        Path.of("app/src/main/java/com/luc4n3x/levyra/$relativePath"),
        Path.of("src/main/java/com/luc4n3x/levyra/$relativePath")
    ).firstOrNull(Files::exists) ?: error("Source file not found: $relativePath")
}
