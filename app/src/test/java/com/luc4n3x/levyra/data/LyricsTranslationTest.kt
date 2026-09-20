package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricWord
import com.luc4n3x.levyra.domain.LyricsTranslationState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsTranslationTest {
    @Test
    fun onDeviceTranslationPreservesTimingOrderWordsAndRoles() = runBlocking {
        val original = listOf(
            LyricLine(
                startMs = 1_000L,
                endMs = 2_800L,
                text = "Hello world",
                words = listOf(
                    LyricWord(1_000L, 1_500L, "Hello"),
                    LyricWord(1_500L, 2_800L, "world")
                )
            ),
            LyricLine(3_000L, 4_000L, "Stay close")
        )
        val coordinator = LyricsTranslationCoordinator(EchoTranslationBackend())

        val outcome = coordinator.translate(original, "it")

        assertEquals(LyricsTranslationState.ON_DEVICE, outcome.state)
        assertEquals(listOf("translated: Hello world", "translated: Stay close"), outcome.lines.map { it.translated })
        assertEquals(original.map { it.startMs to it.endMs }, outcome.lines.map { it.startMs to it.endMs })
        assertEquals(original.map { it.words }, outcome.lines.map { it.words })
        assertEquals(original.map { it.role }, outcome.lines.map { it.role })
    }

    @Test
    fun malformedBatchFallsBackWithoutPartiallyChangingLyrics() = runBlocking {
        val original = listOf(LyricLine(1_000L, 2_000L, "First"), LyricLine(2_000L, 3_000L, "Second"))
        val backend = object : LyricsTranslationBackend {
            override val version = "malformed"
            override suspend fun translate(batches: List<String>, targetLanguageTag: String) =
                LyricsBatchTranslation.Success(listOf("merged output without markers"))
        }

        val outcome = LyricsTranslationCoordinator(backend).translate(original, "it")

        assertEquals(LyricsTranslationState.FAILED, outcome.state)
        assertEquals(original, outcome.lines)
    }

    @Test
    fun providerTranslationWinsAndDoesNotInvokeDeviceBackend() = runBlocking {
        val backend = EchoTranslationBackend()
        val original = listOf(LyricLine(1_000L, 2_000L, "Hello", translated = "Ciao"))

        val outcome = LyricsTranslationCoordinator(backend).translate(original, "it")

        assertEquals(LyricsTranslationState.PROVIDER, outcome.state)
        assertSame(original, outcome.lines)
        assertEquals(0, backend.calls)
    }

    @Test
    fun repeatedContentUsesBoundedCoordinatorCache() = runBlocking {
        val backend = EchoTranslationBackend()
        val coordinator = LyricsTranslationCoordinator(backend)
        val original = listOf(LyricLine(1_000L, 2_000L, "Hello"))

        val first = coordinator.translate(original, "it")
        val second = coordinator.translate(original, "IT")

        assertEquals(first.lines, second.lines)
        assertEquals(1, backend.calls)
    }

    @Test
    fun unavailableModelKeepsOriginalLyricsAndExposesState() = runBlocking {
        val original = listOf(LyricLine(1_000L, 2_000L, "Hello"))
        val backend = object : LyricsTranslationBackend {
            override val version = "download-required"
            override suspend fun translate(batches: List<String>, targetLanguageTag: String) =
                LyricsBatchTranslation.Unavailable(LyricsTranslationState.MODEL_DOWNLOAD_REQUIRED)
        }

        val outcome = LyricsTranslationCoordinator(backend).translate(original, "it")

        assertEquals(LyricsTranslationState.MODEL_DOWNLOAD_REQUIRED, outcome.state)
        assertEquals(original, outcome.lines)
    }

    @Test
    fun sourceMatchingTargetSkipsTranslation() = runBlocking {
        val original = listOf(LyricLine(1_000L, 2_000L, "Hello"))
        val backend = object : LyricsTranslationBackend {
            override val version = "same-language"
            override suspend fun translate(batches: List<String>, targetLanguageTag: String) =
                LyricsBatchTranslation.SameLanguage
        }

        val outcome = LyricsTranslationCoordinator(backend).translate(original, "en")

        assertEquals(LyricsTranslationState.SAME_LANGUAGE, outcome.state)
        assertEquals(original, outcome.lines)
    }

    @Test
    fun translationWaitsForFirstUseModelAndPublishesCompletedText() = runBlocking {
        val backend = object : LyricsTranslationBackend {
            override val version = "delayed-model"
            override suspend fun translate(batches: List<String>, targetLanguageTag: String): LyricsBatchTranslation {
                delay(10L)
                return LyricsBatchTranslation.Success(
                    batches.map { it.replace("Hello", "Ciao") }
                )
            }
        }

        val outcome = LyricsTranslationCoordinator(backend)
            .translate(listOf(LyricLine(1_000L, 2_000L, "Hello")), "it")

        assertEquals(LyricsTranslationState.ON_DEVICE, outcome.state)
        assertEquals("Ciao", outcome.lines.single().translated)
    }

    @Test
    fun chineseTargetsUseExplicitScriptsInBothDirections() {
        assertEquals("zh-Hans", canonicalTranslationLanguageTag("zh"))
        assertEquals("zh-Hant", canonicalTranslationLanguageTag("zh-TW"))
        assertTrue(!sameTranslationLanguageVariant("zh-Hant", "zh"))
        assertTrue(!sameTranslationLanguageVariant("zh", "zh-Hant"))
        assertTrue(sameTranslationLanguageVariant("zh-CN", "zh-Hans"))
    }

    @Test
    fun incompleteTranslationStatesRemainRetryableOnlyWhileEnabled() {
        assertTrue(needsLyricsTranslationRetry(LyricsTranslationState.DISABLED, true))
        assertTrue(needsLyricsTranslationRetry(LyricsTranslationState.PENDING, true))
        assertTrue(needsLyricsTranslationRetry(LyricsTranslationState.MODEL_DOWNLOADING, true))
        assertTrue(!needsLyricsTranslationRetry(LyricsTranslationState.ON_DEVICE, true))
        assertTrue(!needsLyricsTranslationRetry(LyricsTranslationState.PENDING, false))
    }

    @Test
    fun translationStateSurvivesLyricsCacheRoundTrip() {
        val repository = LyricsRepository()
        val original = LyricsRepository.LyricsResult(
            synced = true,
            lines = listOf(LyricLine(1_000L, 2_000L, "Hello", translated = "Ciao")),
            provider = "Provider",
            confidence = 90,
            cached = false,
            translationState = LyricsTranslationState.ON_DEVICE
        )

        val restored = repository.deserializeResult(repository.serializeResult(original))

        assertEquals(LyricsTranslationState.ON_DEVICE, restored?.translationState)
        assertEquals("Ciao", restored?.lines?.single()?.translated)
    }

    @Test
    fun failedProviderDoesNotCancelSiblingProviderWork() = runBlocking {
        val results = supervisorScope {
            val failed = async {
                isolatedLyricsProviderCall(timeoutMs = 1_000L, fallback = { "fallback" }) {
                    error("provider failure")
                }
            }
            val sibling = async {
                isolatedLyricsProviderCall(timeoutMs = 1_000L, fallback = { "fallback" }) {
                    "sibling result"
                }
            }
            listOf(failed.await(), sibling.await())
        }

        assertEquals(listOf("fallback", "sibling result"), results)
    }

    @Test(expected = CancellationException::class)
    fun providerIsolationStillPropagatesCancellation() = runBlocking {
        isolatedLyricsProviderCall(timeoutMs = 1_000L, fallback = { "fallback" }) {
            throw CancellationException("cancelled")
        }
    }

    private class EchoTranslationBackend : LyricsTranslationBackend {
        override val version = "echo-v1"
        var calls = 0

        override suspend fun translate(batches: List<String>, targetLanguageTag: String): LyricsBatchTranslation {
            calls++
            val translated = batches.map { batch ->
                batch.lineSequence().joinToString("\n") { line ->
                    val split = line.indexOf(' ')
                    assertTrue(split > 0)
                    "${line.substring(0, split)} translated: ${line.substring(split + 1)}"
                }
            }
            return LyricsBatchTranslation.Success(translated)
        }
    }
}
