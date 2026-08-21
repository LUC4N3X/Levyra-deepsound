package com.luc4n3x.levyra.data

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackResolverGenerationContractTest {
    @Test
    fun `policy refresh invalidates stale cache publications`() {
        val resolver = readResolverSource()
        val clear = resolver
            .substringAfter("private suspend fun clearResolvedStreamCaches")
            .substringBefore("private fun normalizeAudioQuality")
        val resolution = resolver
            .substringAfter("private suspend fun resolveInternal")
            .substringBefore("suspend fun prefetch")
        val store = resolver
            .substringAfter("private fun store(")
            .substringBefore("private fun remove(")
        val persist = resolver
            .substringAfter("private suspend fun persistResolvedSource")
            .substringBefore("private suspend fun findAlternativeAudioCandidates")

        assertTrue(
            clear.indexOf("sourceMatchMutationMutex.withLock") <
                clear.indexOf("resolverGeneration.incrementAndGet()")
        )
        assertTrue(resolution.contains("val expectedGeneration = resolverGeneration.get()"))
        assertTrue(resolution.contains("_${'$'}expectedGeneration"))
        assertTrue(store.contains("resolverGeneration.get() != expectedGeneration"))
        assertTrue(persist.contains("sourceMatchMutationMutex.withLock"))
        assertTrue(persist.contains("resolverGeneration.get() != expectedGeneration"))
    }

    private fun readResolverSource(): String =
        Files.readString(sourceFile()).replace("\r\n", "\n")

    private fun sourceFile(): Path = sequenceOf(
        Path.of("app/src/main/java/com/luc4n3x/levyra/data/PlaybackResolver.kt"),
        Path.of("src/main/java/com/luc4n3x/levyra/data/PlaybackResolver.kt")
    ).firstOrNull(Files::exists) ?: error("PlaybackResolver source file not found")
}
