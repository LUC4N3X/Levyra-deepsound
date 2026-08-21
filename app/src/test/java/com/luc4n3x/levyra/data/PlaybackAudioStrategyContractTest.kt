package com.luc4n3x.levyra.data

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackAudioStrategyContractTest {
    @Test
    fun `reel audio strategy cannot consume the muxed fallback`() {
        val resolver = readResolverSource()
        val policyDispatch = resolver
            .substringAfter("private suspend fun resolveAudioByCompatibilityPolicy")
            .substringBefore("private suspend fun resolveVideoByCompatibilityPolicy")
        val reelAudio = resolver
            .substringAfter("private suspend fun resolveAudioWithAndroidReel")
            .substringBefore("private suspend fun resolveVideoWithAndroidReel")

        assertTrue(policyDispatch.contains("PlaybackAudioStrategy.REEL_AUDIO"))
        assertTrue(policyDispatch.contains("resolveAudioWithAndroidReel(track, audioQuality)"))
        assertTrue(policyDispatch.contains("PlaybackAudioStrategy.REEL_MUXED"))
        assertTrue(policyDispatch.contains("resolveVideoWithAndroidReel(track)"))
        assertTrue(reelAudio.contains("mime.startsWith(\"audio/\", true)"))
        assertFalse(reelAudio.contains("mime.startsWith(\"video/\", true)"))
        assertTrue(reelAudio.contains("stream solo audio riproducibile"))
    }

    @Test
    fun `malformed persisted stream entries are removed during restore`() {
        val restore = readResolverSource()
            .substringAfter("private fun restoreCache()")
            .substringBefore("private fun store(")

        assertTrue(restore.contains("val json = parsePersistedCacheEntry(value)"))
        assertTrue(restore.contains("if (json == null)"))
        assertTrue(restore.contains("editor.remove(key)"))
    }

    private fun readResolverSource(): String = Files.readString(resolverSource()).replace("\r\n", "\n")

    private fun resolverSource(): Path = sequenceOf(
        Path.of("app/src/main/java/com/luc4n3x/levyra/data/PlaybackResolver.kt"),
        Path.of("src/main/java/com/luc4n3x/levyra/data/PlaybackResolver.kt")
    ).firstOrNull(Files::exists) ?: error("PlaybackResolver source not found")
}
