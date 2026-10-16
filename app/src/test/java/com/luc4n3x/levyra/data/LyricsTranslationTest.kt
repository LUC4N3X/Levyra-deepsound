package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricVocalRole
import com.luc4n3x.levyra.domain.LyricWord
import com.luc4n3x.levyra.domain.LyricsTranslationState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun mergedProviderOutputFallsBackWithoutPartiallyChangingLyrics() = runBlocking {
        val original = listOf(LyricLine(1_000L, 2_000L, "First"), LyricLine(2_000L, 3_000L, "Second"))
        val backend = object : LyricsTranslationBackend {
            override val version = "malformed"
            override suspend fun translate(texts: List<String>, targetLanguageTag: String) =
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
            override suspend fun translate(texts: List<String>, targetLanguageTag: String) =
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
            override suspend fun translate(texts: List<String>, targetLanguageTag: String) =
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
            override suspend fun translate(texts: List<String>, targetLanguageTag: String): LyricsBatchTranslation {
                delay(10L)
                return LyricsBatchTranslation.Success(
                    texts.map { it.replace("Hello", "Ciao") }
                )
            }
        }

        val outcome = LyricsTranslationCoordinator(backend)
            .translate(listOf(LyricLine(1_000L, 2_000L, "Hello")), "it")

        assertEquals(LyricsTranslationState.ON_DEVICE, outcome.state)
        assertEquals("Ciao", outcome.lines.single().translated)
    }

    @Test
    fun turkishTargetTranslatesEveryLineWithoutMarkersAndKeepsKaraokeData() = runBlocking {
        val original = listOf(
            LyricLine(
                startMs = 1_000L,
                endMs = 2_800L,
                text = "Hello world",
                words = listOf(LyricWord(1_000L, 1_500L, "Hello"), LyricWord(1_500L, 2_800L, "world")),
                role = LyricVocalRole.DUET_LEFT
            ),
            LyricLine(3_000L, 3_400L, "[Chorus]", isMetadata = true),
            LyricLine(3_400L, 4_000L, "", isInstrumental = true),
            LyricLine(4_000L, 5_000L, "Stay close", romanized = "stay close")
        )
        val backend = DictionaryBackend(
            dictionary = mapOf("Hello world" to "Merhaba dünya", "Stay close" to "Yakın dur"),
            expectedTarget = "tr"
        )

        val outcome = LyricsTranslationCoordinator(backend).translate(original, "tr")

        assertEquals(LyricsTranslationState.ON_DEVICE, outcome.state)
        assertEquals(listOf(listOf("Hello world", "Stay close")), backend.requests)
        assertTrue(backend.requests.flatten().none { it.contains("__LEVYRA") })
        assertEquals(listOf("Merhaba dünya", "", "", "Yakın dur"), outcome.lines.map { it.translated })
        assertEquals(original.map { it.copy(translated = "") }, outcome.lines.map { it.copy(translated = "") })
    }

    @Test
    fun italianAndEnglishTargetsMapTranslationsToTheirOwnLines() = runBlocking {
        val turkish = listOf(LyricLine(1_000L, 2_000L, "Seni seviyorum"), LyricLine(2_000L, 3_000L, "Gel bana"))
        val italian = LyricsTranslationCoordinator(
            DictionaryBackend(mapOf("Seni seviyorum" to "Ti amo", "Gel bana" to "Vieni da me"), "it")
        ).translate(turkish, "it")
        val english = LyricsTranslationCoordinator(
            DictionaryBackend(mapOf("Seni seviyorum" to "I love you", "Gel bana" to "Come to me"), "en")
        ).translate(turkish, "en")

        assertEquals(listOf("Ti amo", "Vieni da me"), italian.lines.map { it.translated })
        assertEquals(listOf("I love you", "Come to me"), english.lines.map { it.translated })
        assertEquals(LyricsTranslationState.ON_DEVICE, english.state)
    }

    @Test
    fun reflowedOrMarkerLikeProviderTextStaysOnItsOwnLine() = runBlocking {
        val backend = object : LyricsTranslationBackend {
            override val version = "reflow"
            override suspend fun translate(texts: List<String>, targetLanguageTag: String) =
                LyricsBatchTranslation.Success(listOf("  Merhaba\n   dünya  ", "__LEVYRA_SATIR_1__ Yakın dur"))
        }

        val outcome = LyricsTranslationCoordinator(backend)
            .translate(listOf(LyricLine(0L, 1_000L, "Hello world"), LyricLine(1_000L, 2_000L, "Stay close")), "tr")

        assertEquals(LyricsTranslationState.ON_DEVICE, outcome.state)
        assertEquals(listOf("Merhaba dünya", "__LEVYRA_SATIR_1__ Yakın dur"), outcome.lines.map { it.translated })
    }

    @Test
    fun missingLinesAreRetriedOnceAndThenCompleteInOrder() = runBlocking {
        val requests = mutableListOf<List<String>>()
        val backend = object : LyricsTranslationBackend {
            override val version = "partial-then-ok"
            override suspend fun translate(texts: List<String>, targetLanguageTag: String): LyricsBatchTranslation {
                requests += texts
                return LyricsBatchTranslation.Success(
                    if (requests.size == 1) listOf("Bir", null, "Üç") else texts.map { "İki" }
                )
            }
        }
        val original = listOf(LyricLine(0L, 1L, "One"), LyricLine(1L, 2L, "Two"), LyricLine(2L, 3L, "Three"))

        val outcome = LyricsTranslationCoordinator(backend).translate(original, "tr")

        assertEquals(listOf(listOf("One", "Two", "Three"), listOf("Two")), requests)
        assertEquals(listOf("Bir", "İki", "Üç"), outcome.lines.map { it.translated })
        assertEquals(LyricsTranslationState.ON_DEVICE, outcome.state)
    }

    @Test
    fun persistentPartialTranslationNeverPublishesIncompleteLyrics() = runBlocking {
        val backend = object : LyricsTranslationBackend {
            override val version = "partial"
            override suspend fun translate(texts: List<String>, targetLanguageTag: String) =
                LyricsBatchTranslation.Success(texts.map { if (it == "Two") " " else "x" })
        }
        val original = listOf(LyricLine(0L, 1L, "One"), LyricLine(1L, 2L, "Two"))

        val outcome = LyricsTranslationCoordinator(backend).translate(original, "tr")

        assertEquals(LyricsTranslationState.FAILED, outcome.state)
        assertEquals(original, outcome.lines)
    }

    @Test
    fun aSingleUntranslatableLineInALongSongKeepsTheOthersOnTheirOwnLines() = runBlocking {
        val original = (0 until 12).map { index ->
            LyricLine(index * 1_000L, index * 1_000L + 900L, if (index == 5) "Ooh" else "Line $index")
        }
        val backend = object : LyricsTranslationBackend {
            override val version = "untranslatable-line"
            override suspend fun translate(texts: List<String>, targetLanguageTag: String) =
                LyricsBatchTranslation.Success(texts.map { if (it == "Ooh") null else "Satır ${it.removePrefix("Line ")}" })
        }

        val outcome = LyricsTranslationCoordinator(backend).translate(original, "tr")

        assertEquals(LyricsTranslationState.ON_DEVICE, outcome.state)
        assertEquals("", outcome.lines[5].translated)
        outcome.lines.forEachIndexed { index, line ->
            if (index != 5) assertEquals("Satır $index", line.translated)
        }
    }

    @Test
    fun providerErrorsAndTimeoutsSurfaceFailureWithOriginalLyrics() = runBlocking {
        val original = listOf(LyricLine(1_000L, 2_000L, "Hello"))
        val throwing = object : LyricsTranslationBackend {
            override val version = "throwing"
            override suspend fun translate(texts: List<String>, targetLanguageTag: String): LyricsBatchTranslation =
                error("translation service died")
        }
        val hanging = object : LyricsTranslationBackend {
            override val version = "hanging"
            override suspend fun translate(texts: List<String>, targetLanguageTag: String): LyricsBatchTranslation {
                delay(10_000L)
                return LyricsBatchTranslation.Success(texts)
            }
        }

        val thrown = LyricsTranslationCoordinator(throwing).translate(original, "tr")
        val timedOut = LyricsTranslationCoordinator(hanging, timeoutMs = 20L).translate(original, "tr")

        assertEquals(LyricsTranslationState.FAILED, thrown.state)
        assertEquals(original, thrown.lines)
        assertEquals(LyricsTranslationState.FAILED, timedOut.state)
        assertEquals(original, timedOut.lines)
    }

    @Test
    fun unavailableTranslationIsNotCachedSoRetryCanSucceed() = runBlocking {
        var available = false
        val backend = object : LyricsTranslationBackend {
            override val version = "flaky"
            override suspend fun translate(texts: List<String>, targetLanguageTag: String) =
                if (available) {
                    LyricsBatchTranslation.Success(texts.map { "Merhaba" })
                } else {
                    LyricsBatchTranslation.Unavailable(LyricsTranslationState.UNAVAILABLE)
                }
        }
        val coordinator = LyricsTranslationCoordinator(backend)
        val original = listOf(LyricLine(1_000L, 2_000L, "Hello"))

        val first = coordinator.translate(original, "tr")
        available = true
        val second = coordinator.translate(original, "tr")

        assertEquals(LyricsTranslationState.UNAVAILABLE, first.state)
        assertEquals("Merhaba", second.lines.single().translated)
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
    fun cachedPayloadFromAnOlderParserIsRejectedButStillDeserializable() {
        val repository = LyricsRepository()
        val original = LyricsRepository.LyricsResult(
            synced = true,
            lines = listOf(LyricLine(1_000L, 2_000L, "Hello")),
            provider = "Provider",
            confidence = 90,
            cached = false
        )
        val current = repository.serializeResult(original)
        val olderParser = JSONObject(current).apply { remove("parserRevision") }.toString()

        assertTrue(repository.matchesParserRevision(current))
        assertFalse(repository.matchesParserRevision(olderParser))
        assertFalse(repository.matchesParserRevision("not json"))
        assertEquals("Hello", repository.deserializeResult(olderParser)?.lines?.single()?.text)
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
    fun providerIsolationStillPropagatesCancellation() {
        runBlocking {
            isolatedLyricsProviderCall(timeoutMs = 1_000L, fallback = { "fallback" }) {
                throw CancellationException("cancelled")
            }
        }
    }

    private class EchoTranslationBackend : LyricsTranslationBackend {
        override val version = "echo-v1"
        var calls = 0

        override suspend fun translate(texts: List<String>, targetLanguageTag: String): LyricsBatchTranslation {
            calls++
            return LyricsBatchTranslation.Success(texts.map { "translated: $it" })
        }
    }

    private class DictionaryBackend(
        private val dictionary: Map<String, String>,
        private val expectedTarget: String
    ) : LyricsTranslationBackend {
        override val version = "dictionary-v1"
        val requests = mutableListOf<List<String>>()

        override suspend fun translate(texts: List<String>, targetLanguageTag: String): LyricsBatchTranslation {
            assertEquals(expectedTarget, targetLanguageTag)
            requests += texts
            return LyricsBatchTranslation.Success(texts.map(dictionary::get))
        }
    }
}
