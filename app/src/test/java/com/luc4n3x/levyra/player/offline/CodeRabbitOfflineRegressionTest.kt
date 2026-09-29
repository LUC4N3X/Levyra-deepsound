package com.luc4n3x.levyra.player.offline

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeRabbitOfflineRegressionTest {
    @Test
    fun parallelFlacDownloadDoesNotRequestMp4Extraction() {
        val content = exporterSource()

        assertFalse(
            "FLAC range downloads must not be sent through the MP4 audio extractor",
            content.contains("DownloadedAudio(temp, container, !isMp4AudioSource(contentType, track.streamUrl))")
        )
        assertTrue(
            "Parallel downloads should only request extraction for muxed MP4",
            content.contains("DownloadedAudio(temp, container, isMuxedMp4Source(contentType, track.streamUrl))")
        )
    }

    @Test
    fun validatedMimeHintIsUsedWhenDownloadResponsesAreOpaque() {
        val content = exporterSource()
        val directResponseCheck = "isSupportedOfflineSource(responseType, response.request.url.toString())"

        assertFalse(
            "Opaque responses must fall back to the already validated selected-stream MIME type",
            content.contains(directResponseCheck)
        )
        assertTrue(
            "Serial and range response validation should share the validated MIME fallback",
            content.countOccurrences("offlineResponseContentType(") >= 3
        )
    }

    private fun exporterSource(): String {
        val source = sequenceOf(
            Path.of("app/src/main/java/com/luc4n3x/levyra/player/offline/OfflineAudioExporter.kt"),
            Path.of("src/main/java/com/luc4n3x/levyra/player/offline/OfflineAudioExporter.kt")
        ).firstOrNull(Files::exists) ?: error("OfflineAudioExporter.kt not found")
        return Files.readString(source)
    }

    private fun String.countOccurrences(needle: String): Int =
        windowed(needle.length, 1).count { it == needle }
}
