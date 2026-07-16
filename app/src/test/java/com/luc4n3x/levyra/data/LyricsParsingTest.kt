package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricSectionType
import com.luc4n3x.levyra.domain.LyricVocalRole
import com.luc4n3x.levyra.domain.LyricWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsParsingTest {
    @Test
    fun lrcParserExpandsMultipleTimestampsAndComputesEndTimes() {
        val lines = LrcLyricsParser.parse(
            """
            [00:10.00][00:20.50]Same hook
            [00:25.00]Next line
            """.trimIndent()
        )

        assertEquals(3, lines.size)
        assertEquals(10_000L, lines[0].startMs)
        assertEquals("Same hook", lines[0].text)
        assertEquals(20_500L, lines[1].startMs)
        assertEquals(24_920L, lines[1].endMs)
        assertEquals("Next line", lines[2].text)
    }

    @Test
    fun enhancedLrcAppliesOffsetAndKeepsWordTiming() {
        val lines = LrcLyricsParser.parse(
            """
            [offset:+250]
            [00:10.00]<00:10.00>Hello <00:10.50>world
            [00:20.00]Next line
            """.trimIndent()
        )

        assertEquals(2, lines.size)
        assertEquals(10_250L, lines[0].startMs)
        assertEquals("Hello world", lines[0].text)
        assertEquals(2, lines[0].words.size)
        assertEquals(10_250L, lines[0].words[0].startMs)
        assertEquals(10_750L, lines[0].words[1].startMs)
    }

    @Test
    fun ttmlParserKeepsLineTimingAndWordTiming() {
        val lines = TtmlLyricsParser.parse(
            """
            <tt>
              <body>
                <div>
                  <p begin="00:01.000" end="00:03.500">
                    <span begin="00:01.000" end="00:01.400">Hello </span>
                    <span begin="00:01.500" end="00:02.100">world</span>
                  </p>
                </div>
              </body>
            </tt>
            """.trimIndent()
        )

        assertEquals(1, lines.size)
        assertEquals(1_000L, lines[0].startMs)
        assertEquals(3_500L, lines[0].endMs)
        assertEquals("Hello world", lines[0].text)
        assertEquals(2, lines[0].words.size)
        assertEquals("Hello", lines[0].words[0].text.trim())
        assertEquals(1_400L, lines[0].words[0].endMs)
        assertEquals("world", lines[0].words[1].text)
    }

    @Test
    fun ttmlParserPreservesTranslationRomanizationBackgroundAndDuetRole() {
        val lines = TtmlLyricsParser.parse(
            """
            <tt>
              <body>
                <div>
                  <p begin="1s" end="4s" ttm:agent="v2">
                    <span begin="1s" end="2s">Hello </span>
                    <span begin="2s" end="3s">world</span>
                    <span ttm:role="x-translation">Ciao mondo</span>
                    <span ttm:role="x-roman">hello world</span>
                    <span ttm:role="x-bg" begin="2s" end="3.5s">ooh</span>
                  </p>
                </div>
              </body>
            </tt>
            """.trimIndent()
        )

        assertEquals(2, lines.size)
        val main = lines.first { it.role == LyricVocalRole.DUET_RIGHT }
        val background = lines.first { it.role == LyricVocalRole.BACKGROUND }
        assertEquals("Hello world", main.text)
        assertEquals("Ciao mondo", main.translated)
        assertEquals("hello world", main.romanized)
        assertEquals("ooh", background.text)
        assertEquals(2_000L, background.startMs)
    }

    @Test
    fun ttmlMainTranslationExcludesBackgroundDescendants() {
        val lines = TtmlLyricsParser.parse(
            """
            <tt>
              <body>
                <div>
                  <p begin="1s" end="4s">
                    <span begin="1s" end="2s">Main line</span>
                    <span ttm:role="x-translation">Main translation</span>
                    <span ttm:role="x-bg" begin="2s" end="3s">
                      <span begin="2s" end="2.5s">Background</span>
                      <span ttm:role="x-translation">Background translation</span>
                      <span ttm:role="x-roman">background romanized</span>
                    </span>
                  </p>
                </div>
              </body>
            </tt>
            """.trimIndent()
        )

        val main = lines.first { it.role == LyricVocalRole.MAIN }
        val background = lines.first { it.role == LyricVocalRole.BACKGROUND }
        assertEquals("Main translation", main.translated)
        assertEquals("", main.romanized)
        assertEquals("Background translation", background.translated)
        assertEquals("background romanized", background.romanized)
    }

    @Test
    fun cleanerStripsVocalPrefixFromTimedWordsWithoutChangingTiming() {
        val cleaned = LyricsCleaner.clean(
            listOf(
                LyricLine(
                    startMs = 1_000L,
                    endMs = 3_000L,
                    text = "voice 2: hello world",
                    translated = "",
                    words = listOf(
                        LyricWord(1_000L, 1_200L, "voice"),
                        LyricWord(1_200L, 1_400L, "2:"),
                        LyricWord(1_400L, 2_000L, "hello"),
                        LyricWord(2_000L, 2_600L, "world")
                    )
                )
            )
        )

        assertEquals(1, cleaned.size)
        assertEquals(LyricVocalRole.DUET_RIGHT, cleaned[0].role)
        assertEquals("hello world", cleaned[0].text)
        assertEquals(listOf("hello", "world"), cleaned[0].words.map { it.text })
        assertEquals(1_400L, cleaned[0].words[0].startMs)
        assertEquals(2_000L, cleaned[0].words[0].endMs)
    }

    @Test
    fun cleanerMergesComplementaryDuplicateEnrichmentFields() {
        val cleaned = LyricsCleaner.clean(
            listOf(
                LyricLine(
                    startMs = 1_000L,
                    endMs = 2_800L,
                    text = "Hello world",
                    translated = "",
                    words = listOf(
                        LyricWord(1_000L, 1_500L, "Hello", romanized = "he-llo"),
                        LyricWord(1_500L, 2_000L, "world")
                    ),
                    romanized = "hello world"
                ),
                LyricLine(
                    startMs = 1_000L,
                    endMs = 3_200L,
                    text = "Hello world",
                    translated = "Ciao mondo",
                    words = emptyList(),
                    romanized = ""
                )
            )
        )

        assertEquals(1, cleaned.size)
        assertEquals("Ciao mondo", cleaned[0].translated)
        assertEquals("hello world", cleaned[0].romanized)
        assertEquals(2, cleaned[0].words.size)
        assertEquals("he-llo", cleaned[0].words[0].romanized)
        assertEquals(3_200L, cleaned[0].endMs)
    }

    @Test
    fun yrcParserKeepsAbsoluteSyllableTiming() {
        val lines = YrcLyricsParser.parse("[1000,1500](1000,500,0)Hel(1500,500,0)lo")

        assertEquals(1, lines.size)
        assertEquals("Hello", lines[0].text)
        assertEquals(2, lines[0].words.size)
        assertEquals(1_500L, lines[0].words[1].startMs)
    }

    @Test
    fun qrcParserKeepsAbsoluteSyllableTiming() {
        val lines = QrcLyricsParser.parse("[1000,1500]<1000,500,0>Hel<1500,500,0>lo")

        assertEquals(1, lines.size)
        assertEquals("Hello", lines[0].text)
        assertEquals(1_500L, lines[0].words[1].startMs)
    }

    @Test
    fun krcParserConvertsRelativeSyllableTiming() {
        val lines = KrcLyricsParser.parse("[1000,1500]<0,500,0>Hel<500,500,0>lo")

        assertEquals(1, lines.size)
        assertEquals("Hello", lines[0].text)
        assertEquals(1_500L, lines[0].words[1].startMs)
    }

    @Test
    fun cleanerRemovesCreditsAndRecognizesBackgroundPrefixes() {
        val lines = LyricsCleaner.clean(
            listOf(
                LyricLine(0L, 1_000L, "Lyrics by: Someone", ""),
                LyricLine(1_000L, 2_000L, "BG: ooh", "")
            )
        )

        assertEquals(1, lines.size)
        assertEquals("ooh", lines[0].text)
        assertEquals(LyricVocalRole.BACKGROUND, lines[0].role)
    }

    @Test
    fun matcherNormalizesOfficialMetadataAndPenalizesWrongVersions() {
        assertTrue(LyricsMatcher.similarity("Song (Official Video)", "Song") >= 90)
        assertTrue(LyricsMatcher.versionMismatchPenalty("Song Live", "Song") > 0)
    }

    @Test
    fun rankerPrefersSyncedCloseDurationCandidate() {
        val requested = LyricsRequest("Song", "Artist", 180)
        val syncedClose = LyricsCandidate(
            result = LyricsRepository.LyricsResult(
                synced = true,
                lines = listOf(LrcLyricsParser.parse("[00:01.00]Line").first()),
                provider = "Synced",
                confidence = 0,
                cached = false
            ),
            title = "Song",
            artist = "Artist",
            durationSec = 181
        )
        val plainExact = syncedClose.copy(
            result = syncedClose.result.copy(synced = false, provider = "Plain"),
            durationSec = 180
        )

        val best = LyricsResultRanker.best(listOf(plainExact, syncedClose), requested)

        assertEquals("Synced", best?.provider)
        assertTrue((best?.confidence ?: 0) >= 80)
    }

    @Test
    fun providerSelectorUsesNativeTimedLyricsBeforeLrcLib() {
        val request = LyricsRequest("Song", "Artist", 180)
        val native = candidate("YouTube Music", synced = true)
        val lrc = candidate("LRCLIB", synced = true)

        val selected = LyricsProviderSelector.select(native, listOf(lrc), request)

        assertEquals("YouTube Music", selected?.provider)
    }

    @Test
    fun providerSelectorUsesSyncedLrcLibBeforeNativePlainLyrics() {
        val request = LyricsRequest("Song", "Artist", 180)
        val native = candidate("YouTube Music", synced = false)
        val lrc = candidate("LRCLIB", synced = true)

        val selected = LyricsProviderSelector.select(native, listOf(lrc), request)

        assertEquals("LRCLIB", selected?.provider)
    }

    @Test
    fun providerSelectorUsesNativePlainLyricsBeforeLrcLibPlainLyrics() {
        val request = LyricsRequest("Song", "Artist", 180)
        val native = candidate("YouTube Music", synced = false)
        val lrc = candidate("LRCLIB", synced = false)

        val selected = LyricsProviderSelector.select(native, listOf(lrc), request)

        assertEquals("YouTube Music", selected?.provider)
    }

    @Test
    fun sectionDetectorRemovesExplicitMarkersAndBuildsOrderedSections() {
        val detection = LyricsSectionDetector.detect(
            listOf(
                LyricLine(0L, 500L, "[Verse 1]", ""),
                LyricLine(500L, 1_500L, "First verse line", ""),
                LyricLine(1_500L, 2_500L, "Second verse line", ""),
                LyricLine(2_500L, 3_000L, "[Chorus]", ""),
                LyricLine(3_000L, 4_000L, "This is the hook", ""),
                LyricLine(4_000L, 5_000L, "Sing it again", "")
            )
        )

        assertEquals(listOf("First verse line", "Second verse line", "This is the hook", "Sing it again"), detection.lines.map { it.text })
        assertEquals(2, detection.sections.size)
        assertEquals(LyricSectionType.VERSE, detection.sections[0].type)
        assertEquals(0, detection.sections[0].startLineIndex)
        assertEquals(1, detection.sections[0].endLineIndex)
        assertEquals(LyricSectionType.CHORUS, detection.sections[1].type)
        assertEquals(2, detection.sections[1].startLineIndex)
        assertEquals(3_000L, detection.sections[1].startMs)
    }

    @Test
    fun sectionDetectorFindsRepeatedChorusBlocksWithoutMarkers() {
        val texts = listOf(
            "Opening line one",
            "Opening line two",
            "Hook line alpha",
            "Hook line beta",
            "Middle line one",
            "Middle line two",
            "Middle line three",
            "Hook line alpha",
            "Hook line beta",
            "Closing line"
        )
        val lines = texts.mapIndexed { index, text ->
            LyricLine(index * 1_000L, (index + 1) * 1_000L, text, "")
        }

        val detection = LyricsSectionDetector.detect(lines)
        val choruses = detection.sections.filter { it.type == LyricSectionType.CHORUS }

        assertEquals(2, choruses.size)
        assertEquals(2_000L, choruses[0].startMs)
        assertEquals(7_000L, choruses[1].startMs)
        assertTrue(choruses.all { it.confidence >= 80 })
    }


    @Test
    fun sectionDetectorDoesNotTreatDelayedFirstLyricAsIntroOrInventAnOutro() {
        val lines = (0 until 8).map { index ->
            LyricLine(
                startMs = 12_000L + index * 2_000L,
                endMs = 13_800L + index * 2_000L,
                text = "Unique verse line ${index + 1}"
            )
        }

        val detection = LyricsSectionDetector.detect(lines)

        assertEquals(1, detection.sections.size)
        assertEquals(LyricSectionType.VERSE, detection.sections.single().type)
        assertEquals(0, detection.sections.single().startLineIndex)
        assertEquals(7, detection.sections.single().endLineIndex)
    }

    @Test
    fun qualityEnginePrefersCompleteWordTimingOverNominalProviderConfidence() {
        val request = LyricsRequest("Song", "Artist", 120L)
        val timedLines = (0 until 12).map { index ->
            val start = index * 10_000L
            LyricLine(
                startMs = start,
                endMs = start + 8_000L,
                text = "Timed lyric line $index",
                translated = "",
                words = listOf(
                    LyricWord(start, start + 3_500L, "Timed"),
                    LyricWord(start + 3_500L, start + 7_500L, "lyric line $index")
                )
            )
        }
        val plainLines = timedLines.map { it.copy(words = emptyList()) }
        val timed = LyricsCandidate(
            result = LyricsRepository.LyricsResult(true, timedLines, "LRCLIB Search", 78, false),
            title = "Song",
            artist = "Artist",
            durationSec = 120L
        )
        val plain = LyricsCandidate(
            result = LyricsRepository.LyricsResult(true, plainLines, "YouTube Music", 100, false),
            title = "Song",
            artist = "Artist",
            durationSec = 120L
        )

        val best = LyricsResultRanker.best(listOf(plain, timed), request)

        assertEquals("LRCLIB Search", best?.provider)
        assertTrue((best?.confidence ?: 0) >= 85)
    }

    private fun candidate(provider: String, synced: Boolean): LyricsCandidate {
        return LyricsCandidate(
            result = LyricsRepository.LyricsResult(
                synced = synced,
                lines = listOf(LyricLine(1_000L, 2_000L, "Line", "")),
                provider = provider,
                confidence = 90,
                cached = false
            ),
            title = "Song",
            artist = "Artist",
            durationSec = 180
        )
    }
}
