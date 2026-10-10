package com.luc4n3x.levyra.player.offline

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineMetadataEmbeddingContractTest {
    @Test
    fun unsafeMetadataEmbeddingFallsBackToTheDownloadedAudio() {
        val source = sequenceOf(
            Path.of("app/src/main/java/com/luc4n3x/levyra/player/offline/OfflineAudioExporter.kt"),
            Path.of("src/main/java/com/luc4n3x/levyra/player/offline/OfflineAudioExporter.kt")
        ).firstOrNull(Files::exists) ?: error("OfflineAudioExporter.kt not found")
        val content = Files.readString(source)
        val guardStart = "if (!shouldEmbedFastMetadata(input.length(), track.durationMs)) {"
        val nextStage = "val output = File(workspace, \"tagged-"

        assertTrue(content.contains(guardStart))
        assertTrue(content.contains(nextStage))
        val guard = content.substringAfter(guardStart).substringBefore(nextStage)
        assertTrue(guard.contains("return PreparedAudioFile(input, fileName, container, fileMetadataEmbedded = false)"))
        assertFalse(guard.contains("throw IOException"))
    }

    @Test
    fun metadataFastPathKeepsItsDurationAndSizeLimits() {
        assertTrue(shouldEmbedFastMetadata(FAST_METADATA_EMBED_MAX_BYTES, FAST_METADATA_EMBED_MAX_DURATION_MS))
        assertFalse(shouldEmbedFastMetadata(1L, FAST_METADATA_EMBED_MAX_DURATION_MS + 1L))
        assertFalse(shouldEmbedFastMetadata(FAST_METADATA_EMBED_MAX_BYTES + 1L, 1L))
    }
}
