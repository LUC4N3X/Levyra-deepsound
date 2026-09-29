package com.luc4n3x.levyra.player.offline

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeRabbitOfflineRegressionTest {
    @Test
    fun flacInputBypassesMp4TransformerExtraction() {
        val content = extractorSource()
        val flacGuard = content.indexOf("if (isFlacInput(input))")
        val transformerPath = content.indexOf("runExtraction(context, input, output)")

        assertTrue("FLAC input must be detected before Transformer extraction", flacGuard >= 0)
        assertTrue("FLAC input must be copied without lossy transcoding", content.contains("input.copyTo(output, overwrite = true)"))
        assertTrue("FLAC passthrough must execute before the MP4 extraction path", transformerPath > flacGuard)
    }

    private fun extractorSource(): String {
        val source = sequenceOf(
            Path.of("app/src/main/java/com/luc4n3x/levyra/player/offline/OfflineAudioTrackExtractor.kt"),
            Path.of("src/main/java/com/luc4n3x/levyra/player/offline/OfflineAudioTrackExtractor.kt")
        ).firstOrNull(Files::exists) ?: error("OfflineAudioTrackExtractor.kt not found")
        return Files.readString(source)
    }
}
