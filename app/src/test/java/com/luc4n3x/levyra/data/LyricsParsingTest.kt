package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricVocalRole
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
