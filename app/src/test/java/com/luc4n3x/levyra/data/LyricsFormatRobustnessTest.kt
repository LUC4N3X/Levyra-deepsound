package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricVocalRole
import com.luc4n3x.levyra.domain.LyricWord
import java.util.Base64
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsFormatRobustnessTest {

    @Test
    fun ttmlKeepsSpacesBetweenWordSpansAndJoinsAdjacentSyllables() {
        val lines = UnifiedLyricsParser.parse(
            """<tt><body><div><p begin="1s" end="3s"><span begin="1s" end="1.4s">Hel</span><span begin="1.4s" end="1.8s">lo</span> <span begin="2s" end="2.5s">world,</span> <span begin="2.5s" end="3s">again</span></p></div></body></tt>"""
        )

        assertEquals(1, lines.size)
        assertEquals("Hello world, again", lines[0].text)
        assertEquals(listOf("Hel", "lo ", "world, ", "again"), lines[0].words.map { it.text })
    }

    @Test
    fun ttmlReadsNamespacePrefixedElements() {
        val lines = UnifiedLyricsParser.parse(
            """
            <tt:tt xmlns:tt="http://www.w3.org/ns/ttml">
              <tt:body><tt:div>
                <tt:p begin="00:01.000" end="00:02.000"><tt:span begin="00:01.000" end="00:01.500">Prefixed</tt:span> <tt:span begin="00:01.500" end="00:02.000">line</tt:span></tt:p>
              </tt:div></tt:body>
            </tt:tt>
            """.trimIndent()
        )

        assertEquals(1, lines.size)
        assertEquals("Prefixed line", lines[0].text)
        assertEquals(2, lines[0].words.size)
    }

    @Test
    fun ttmlMapsMultiplePersonAgentsAndKeepsGroupAgentsAsMainVocal() {
        val lines = UnifiedLyricsParser.parse(
            """
            <tt xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
              <head><metadata>
                <ttm:agent type="person" xml:id="v1"/>
                <ttm:agent type="person" xml:id="v2"/>
                <ttm:agent type="person" xml:id="v3"/>
                <ttm:agent type="group" xml:id="v1000"/>
              </metadata></head>
              <body><div>
                <p begin="1s" end="2s" ttm:agent="v1">First singer</p>
                <p begin="3s" end="4s" ttm:agent="v2">Second singer</p>
                <p begin="5s" end="6s" ttm:agent="v3">Third singer</p>
                <p begin="7s" end="8s" ttm:agent="v1000">Everyone together</p>
              </div></body>
            </tt>
            """.trimIndent()
        )

        val roles = lines.associate { it.text to it.role }
        assertEquals(LyricVocalRole.DUET_LEFT, roles["First singer"])
        assertEquals(LyricVocalRole.DUET_RIGHT, roles["Second singer"])
        assertEquals(LyricVocalRole.DUET_LEFT, roles["Third singer"])
        assertEquals(LyricVocalRole.MAIN, roles["Everyone together"])
    }

    @Test
    fun ttmlSinglePersonWithGroupChorusIsNotRenderedAsDuet() {
        val lines = UnifiedLyricsParser.parse(
            """
            <tt><head><metadata><ttm:agent type="group" xml:id="v1000"/></metadata></head>
              <body><div>
                <p begin="1s" end="2s" ttm:agent="v1">Solo verse</p>
                <p begin="3s" end="4s" ttm:agent="v1000">Group chorus</p>
              </div></body>
            </tt>
            """.trimIndent()
        )

        assertTrue(lines.all { it.role == LyricVocalRole.MAIN })
    }

    @Test
    fun ttmlBackgroundVocalsKeepTheirOwnTranslationAndRomanization() {
        val lines = UnifiedLyricsParser.parse(
            """
            <tt><body><div>
              <p begin="1s" end="4s">
                <span begin="1s" end="2s">Main</span> <span begin="2s" end="3s">line</span>
                <span ttm:role="x-translation">Riga principale</span>
                <span ttm:role="x-bg" begin="2.5s" end="3.5s"><span begin="2.5s" end="3s">(echo</span> <span begin="3s" end="3.5s">back)</span><span ttm:role="x-translation">eco</span><span ttm:role="x-roman">ekko</span></span>
              </p>
            </div></body></tt>
            """.trimIndent()
        )

        val main = lines.first { it.role != LyricVocalRole.BACKGROUND }
        val background = lines.first { it.role == LyricVocalRole.BACKGROUND }
        assertEquals("Main line", main.text)
        assertEquals("Riga principale", main.translated)
        assertEquals("(echo back)", background.text)
        assertEquals("eco", background.translated)
        assertEquals("ekko", background.romanized)
        assertEquals(2, background.words.size)
    }

    @Test
    fun ttmlRejectsDoctypeEntitiesWithoutThrowing() {
        val hostile = """<?xml version="1.0"?><!DOCTYPE tt [<!ENTITY xxe SYSTEM "file:///etc/passwd">]><tt><body><div><p begin="1s" end="2s">&xxe;</p></div></body></tt>"""

        assertTrue(UnifiedLyricsParser.parse(hostile).isEmpty())
    }

    @Test
    fun ttmlDecodesPredefinedEntitiesAndIgnoresInvalidTimes() {
        val lines = UnifiedLyricsParser.parse(
            """<tt><body><div><p begin="1s" end="2s">Rock &amp; roll</p><p begin="-1s" end="3s">Negative</p><p begin="NaNs">Broken</p></div></body></tt>"""
        )

        assertEquals(listOf("Rock & roll"), lines.map { it.text })
    }

    @Test
    fun yrcKeepsParenthesesInsideSyllablesAndSpaceOnlySyllables() {
        val lines = UnifiedLyricsParser.parse("[1000,2000](1000,400,0)(Oh)(1400,100,0) (1500,500,0)yeah")

        assertEquals(1, lines.size)
        assertEquals("(Oh) yeah", lines[0].text)
        assertEquals(listOf("(Oh) ", "yeah"), lines[0].words.map { it.text })
        assertEquals(1_500L, lines[0].words[1].startMs)
    }

    @Test
    fun qrcSuffixTimingParsesAsWordTimedLyrics() {
        val text = "[ti:Song]\n[1000,1500]Hel(1000,500)lo(1500,500) (2000,0)there(2000,500)"

        assertEquals(LyricsFormat.QRC, UnifiedLyricsParser.detect(text))
        val lines = UnifiedLyricsParser.parse(text)

        assertEquals(1, lines.size)
        assertEquals("Hello there", lines[0].text)
        assertEquals(listOf(1_000L, 1_500L, 2_000L), lines[0].words.map { it.startMs })
    }

    @Test
    fun qrcXmlWrapperIsUnwrappedAndEntitiesDecoded() {
        val wrapped = """<?xml version="1.0" encoding="utf-8"?><QrcInfos><LyricInfo LyricCount="1"><Lyric_1 LyricType="1" LyricContent="[ti:Song]
[500,1000]Tom(500,400) &amp; (900,0)Jerry(900,600)
"/></LyricInfo></QrcInfos>"""

        val lines = UnifiedLyricsParser.parse(wrapped)

        assertEquals(1, lines.size)
        assertEquals("Tom & Jerry", lines[0].text)
    }

    @Test
    fun angleBracketSyllablesDetectRelativeTimingWithoutKrcHeader() {
        val lines = UnifiedLyricsParser.parse(
            """
            [5000,1500]<0,500,0>Hel<500,500,0>lo
            [8000,1000]<0,1000,0>again
            """.trimIndent()
        )

        assertEquals(5_000L, lines[0].words[0].startMs)
        assertEquals(5_500L, lines[0].words[1].startMs)
        assertEquals(8_000L, lines[1].words[0].startMs)
    }

    @Test
    fun krcLanguageBlockAddsTranslationAndRomanization() {
        val language = Base64.getEncoder().encodeToString(
            """{"content":[{"type":0,"lyricContent":[["ko","n","ni","chi","wa"]]},{"type":1,"lyricContent":[["Hello"]]}]}""".toByteArray()
        )
        val lines = UnifiedLyricsParser.parse(
            """
            [language:$language]
            [1000,2000]<0,400,0>こ<400,400,0>ん<800,400,0>に<1200,400,0>ち<1600,400,0>は
            """.trimIndent()
        )

        assertEquals(1, lines.size)
        assertEquals("こんにちは", lines[0].text)
        assertEquals("Hello", lines[0].translated)
        assertEquals(listOf("ko", "n", "ni", "chi", "wa"), lines[0].words.map { it.romanized })
        assertEquals(2_600L, lines[0].words.last().startMs)
    }

    @Test
    fun krcMalformedLanguageBlockIsIgnored() {
        val lines = UnifiedLyricsParser.parse("[language:%%%not-base64%%%]\n[1000,1000]<0,1000,0>Still here")

        assertEquals(1, lines.size)
        assertEquals("Still here", lines[0].text)
        assertEquals("", lines[0].translated)
    }

    @Test
    fun enhancedLrcKeepsTextBeforeTheFirstWordMarker() {
        val lines = LrcLyricsParser.parse("[00:10.00]Hello <00:10.60>big <00:11.00>world")

        assertEquals("Hello big world", lines[0].text)
        assertEquals(3, lines[0].words.size)
        assertEquals(10_000L, lines[0].words[0].startMs)
        assertEquals(10_580L, lines[0].words[0].endMs)
    }

    @Test
    fun lrcBlankTimestampEndsThePreviousLine() {
        val lines = LrcLyricsParser.parse(
            """
            [00:05.00]Verse
            [00:08.00]
            [00:20.00]Chorus
            """.trimIndent()
        )

        assertEquals(2, lines.size)
        assertEquals(8_000L, lines[0].endMs)
    }

    @Test
    fun lrcBilingualDocumentPairsBecomeTranslations() {
        val lines = UnifiedLyricsParser.parse(
            """
            [00:05.00]Ciao mondo
            [00:05.00]Hello world
            [00:09.00]Resta qui
            [00:09.00]Stay here
            [00:13.00]Per sempre
            [00:13.00]Forever
            """.trimIndent()
        )

        assertEquals(listOf("Ciao mondo", "Resta qui", "Per sempre"), lines.map { it.text })
        assertEquals(listOf("Hello world", "Stay here", "Forever"), lines.map { it.translated })
    }

    @Test
    fun lrcOccasionalSameTimestampVocalsStayAsSeparateLines() {
        val lines = UnifiedLyricsParser.parse(
            """
            [00:01.00]First verse
            [00:05.00]Lead vocal line
            [00:05.00]Second singer line
            [00:09.00]Chorus
            [00:13.00]Bridge
            """.trimIndent()
        )

        assertEquals(5, lines.size)
        assertEquals(listOf("Lead vocal line", "Second singer line"), lines.filter { it.startMs == 5_000L }.map { it.text })
        assertTrue(lines.all { it.translated.isEmpty() })
    }

    @Test
    fun lrcSingleSameTimestampPairIsNotEnoughEvidenceForTranslation() {
        val lines = UnifiedLyricsParser.parse(
            """
            [00:05.00]Ciao mondo
            [00:05.00]Hello world
            """.trimIndent()
        )

        assertEquals(listOf("Ciao mondo", "Hello world"), lines.map { it.text })
        assertTrue(lines.all { it.translated.isEmpty() })
    }

    @Test
    fun lrcDuplicatedIdenticalTimestampsCollapse() {
        val lines = UnifiedLyricsParser.parse("[00:05.00][00:05.00]Echo\n[00:07.00]Next")

        assertEquals(listOf("Echo", "Next"), lines.map { it.text })
        assertTrue(lines.all { it.translated.isEmpty() })
    }

    @Test
    fun lrcMoreThanTwoSameTimestampLinesArePreservedEvenInBilingualDocuments() {
        val lines = UnifiedLyricsParser.parse(
            """
            [00:01.00]Uno
            [00:01.00]One
            [00:04.00]Due
            [00:04.00]Two
            [00:08.00]Tutti
            [00:08.00]Everyone
            [00:08.00]All together
            """.trimIndent()
        )

        assertEquals("One", lines.first { it.text == "Uno" }.translated)
        assertEquals(listOf("All together", "Everyone", "Tutti").sorted(), lines.filter { it.startMs == 8_000L }.map { it.text }.sorted())
        assertTrue(lines.filter { it.startMs == 8_000L }.all { it.translated.isEmpty() })
    }

    @Test
    fun malformedLrcLinesAreIgnoredWithoutFailingTheDocument() {
        val lines = UnifiedLyricsParser.parse(
            """
            [ti:Title]
            [xx:yy]Broken
            [00:1x]Broken too
            [00:02.00]Valid
            random text
            [00:04.00]Also valid
            """.trimIndent()
        )

        assertEquals(listOf("Valid", "Also valid"), lines.map { it.text })
    }

    @Test
    fun cleanerMakesOverlappingAndZeroDurationWordsMonotonic() {
        val cleaned = LyricsCleaner.clean(
            listOf(
                LyricLine(
                    startMs = 1_000L,
                    endMs = 3_000L,
                    text = "One two three",
                    translated = "",
                    words = listOf(
                        LyricWord(1_000L, 1_600L, "One "),
                        LyricWord(900L, 900L, "two "),
                        LyricWord(1_800L, 1_800L, "three")
                    )
                )
            )
        )

        val words = cleaned.single().words
        assertEquals(listOf(1_000L, 1_000L, 1_800L), words.map { it.startMs })
        assertTrue(words.all { it.endMs >= it.startMs })
    }

    @Test
    fun wordsExtendingBeyondLineEndStretchTheLineUntilTheNextLine() {
        val cleaned = LyricsCleaner.clean(
            listOf(
                LyricLine(1_000L, 2_000L, "Long tail", "", words = listOf(LyricWord(1_000L, 1_500L, "Long "), LyricWord(1_500L, 2_600L, "tail"))),
                LyricLine(2_500L, 4_000L, "Next", "")
            )
        )

        assertEquals(2_440L, cleaned[0].endMs)
    }

    @Test
    fun oversizedOrGarbageInputFailsGracefully() {
        assertTrue(UnifiedLyricsParser.parse("x".repeat(UnifiedLyricsParser.MAX_SOURCE_CHARS + 1)).isEmpty())
        assertTrue(UnifiedLyricsParser.parse("<tt><body><p begin=\"1s\">unterminated").isEmpty())
        assertTrue(UnifiedLyricsParser.parse("[1,2](((((").size <= 1)
        assertTrue(UnifiedLyricsParser.parse("\u0000\u0001\u0002").all { it.text.isNotBlank() })
    }

    @Test
    fun largeLrcDocumentParsesQuickly() {
        val source = buildString {
            repeat(8_000) { index ->
                val minutes = index / 60
                val seconds = index % 60
                append("[%02d:%02d.00]Line %d\n".format(minutes, seconds, index))
            }
        }

        val started = System.nanoTime()
        val lines = UnifiedLyricsParser.parse(source)
        val elapsedMs = (System.nanoTime() - started) / 1_000_000L

        assertEquals(8_000, lines.size)
        assertTrue("parsing took $elapsedMs ms", elapsedMs < 5_000L)
    }

    @Test
    fun rankerRejectsWrongSongEvenWhenItHasWordTiming() {
        val request = LyricsRequest("Blinding Lights", "The Weeknd", 200L)
        val wrongSong = candidate("LyricsPlus", title = "Save Your Tears", artist = "The Weeknd", durationSec = 215L, wordTimed = true)

        assertEquals(LyricsMatchStrength.REJECTED, LyricsMatcher.matchStrength(wrongSong, request))
        assertNull(LyricsResultRanker.best(listOf(wrongSong), request))
    }

    @Test
    fun manualVersionListKeepsRejectedCandidatesLastInsteadOfHidingThem() {
        val request = LyricsRequest("Song", "Artist", 180L)
        val extendedCut = candidate("LRCLIB Search", title = "Song", artist = "Artist", durationSec = 260L, wordTimed = false)
        val strong = candidate("LRCLIB Exact", title = "Song", artist = "Artist", durationSec = 180L, wordTimed = false, lineText = "Other")

        val ranked = LyricsResultRanker.rankedCandidates(listOf(extendedCut, strong), request)

        assertEquals(listOf("LRCLIB Exact", "LRCLIB Search"), ranked.map { it.result.provider })
        assertEquals("LRCLIB Exact", LyricsResultRanker.best(listOf(extendedCut, strong), request)?.provider)
    }

    @Test
    fun strongLineSyncedMatchBeatsPlausibleWordSyncedMatch() {
        val request = LyricsRequest("Song", "Artist", 180L)
        val strongLine = candidate("LRCLIB Exact", title = "Song", artist = "Artist", durationSec = 180L, wordTimed = false)
        val plausibleWords = candidate("LyricsPlus", title = "Song", artist = "Artist", durationSec = 195L, wordTimed = true)

        assertEquals(LyricsMatchStrength.STRONG, LyricsMatcher.matchStrength(strongLine, request))
        assertEquals(LyricsMatchStrength.PLAUSIBLE, LyricsMatcher.matchStrength(plausibleWords, request))
        assertEquals("LRCLIB Exact", LyricsResultRanker.best(listOf(plausibleWords, strongLine), request)?.provider)
    }

    @Test
    fun wordSyncedBeatsLineSyncedBeatsUnsyncedForTheSameStrongMatch() {
        val request = LyricsRequest("Song", "Artist", 180L)
        val unsynced = candidate("Lyrics.ovh", title = "Song", artist = "Artist", durationSec = 180L, wordTimed = false, synced = false)
        val line = candidate("LRCLIB Exact", title = "Song", artist = "Artist", durationSec = 180L, wordTimed = false)
        val word = candidate("LyricsPlus", title = "Song", artist = "Artist", durationSec = 180L, wordTimed = true)

        val ranked = LyricsResultRanker.rankedCandidates(listOf(unsynced, line, word), request)

        assertEquals(listOf("LyricsPlus", "LRCLIB Exact", "Lyrics.ovh"), ranked.map { it.result.provider })
    }

    @Test
    fun plausibleMatchesNeverReachTheEarlyExitThreshold() {
        val request = LyricsRequest("Song", "Artist", 180L)
        val plausible = candidate("YouTube Music", title = "Song", artist = "Artist", durationSec = 197L, wordTimed = true)

        val best = LyricsResultRanker.best(listOf(plausible), request)

        assertTrue((best?.confidence ?: 0) < 80)
    }

    @Test
    fun unknownCandidateDurationIsNeverAStrongMatch() {
        val request = LyricsRequest("Song", "Artist", 180L)
        val candidate = candidate("Lyrics.ovh", title = "Song", artist = "Artist", durationSec = 0L, wordTimed = false)

        assertEquals(LyricsMatchStrength.PLAUSIBLE, LyricsMatcher.matchStrength(candidate, request))
    }

    @Test
    fun unknownRequestDurationIsNeverAStrongMatch() {
        val request = LyricsRequest("Song", "Artist", 0L)
        val candidate = candidate("LRCLIB Exact", title = "Song", artist = "Artist", durationSec = 180L, wordTimed = false)

        assertEquals(LyricsMatchStrength.PLAUSIBLE, LyricsMatcher.matchStrength(candidate, request))
    }

    @Test
    fun bothDurationsUnknownIsNeverAStrongMatch() {
        val request = LyricsRequest("Song", "Artist", 0L)
        val candidate = candidate("LRCLIB Exact", title = "Song", artist = "Artist", durationSec = 0L, wordTimed = false)

        assertEquals(LyricsMatchStrength.PLAUSIBLE, LyricsMatcher.matchStrength(candidate, request))
        assertTrue((LyricsResultRanker.best(listOf(candidate), request)?.confidence ?: 0) < 80)
    }

    @Test
    fun knownDurationWithinFiveSecondsStaysStrong() {
        val request = LyricsRequest("Song", "Artist", 180L)
        val within = candidate("LRCLIB Exact", title = "Song", artist = "Artist", durationSec = 185L, wordTimed = false)
        val outside = candidate("LRCLIB Exact", title = "Song", artist = "Artist", durationSec = 186L, wordTimed = false)

        assertEquals(LyricsMatchStrength.STRONG, LyricsMatcher.matchStrength(within, request))
        assertEquals(LyricsMatchStrength.PLAUSIBLE, LyricsMatcher.matchStrength(outside, request))
    }

    @Test
    fun recordingIdentityStillDecidesWhenDurationsAreUnknown() {
        val request = LyricsRequest("Song", "Artist", 0L, recordingId = "video")
        val candidate = candidate("YouTube Music", title = "Song", artist = "Artist", durationSec = 0L, wordTimed = false)
            .copy(recordingId = "video")

        assertEquals(LyricsMatchStrength.STRONG, LyricsMatcher.matchStrength(candidate, request))
    }

    @Test
    fun recordingIdentityConflictIsRejected() {
        val request = LyricsRequest("Song", "Artist", 180L, recordingId = "wanted")
        val other = candidate("LyricsPlus", title = "Song", artist = "Artist", durationSec = 180L, wordTimed = true)
            .copy(recordingId = "other")

        assertEquals(LyricsMatchStrength.REJECTED, LyricsMatcher.matchStrength(other, request))
    }

    @Test
    fun fallbackSkipsRejectedProviderAndKeepsNextValidOne() {
        val request = LyricsRequest("Song", "Artist", 180L)
        val primaryWrong = candidate("YouTube Music", title = "Another Song Entirely", artist = "Somebody", durationSec = 260L, wordTimed = true)
        val secondaryValid = candidate("LRCLIB Search", title = "Song", artist = "Artist", durationSec = 181L, wordTimed = false)

        assertEquals("LRCLIB Search", LyricsResultRanker.best(listOf(primaryWrong, secondaryValid), request)?.provider)
    }

    @Test
    fun equalCandidatesAreOrderedDeterministicallyByProviderPriority() {
        val request = LyricsRequest("Song", "Artist", 180L)
        val first = candidate("LRCLIB Search", title = "Song", artist = "Artist", durationSec = 180L, wordTimed = false, lineText = "Alpha")
        val second = candidate("LRCLIB Search", title = "Song", artist = "Artist", durationSec = 180L, wordTimed = false, lineText = "Beta")
            .let { it.copy(result = it.result.copy(provider = "LRCLIB Exact")) }

        val forward = LyricsResultRanker.rankedCandidates(listOf(first, second), request).map { it.result.provider }
        val reversed = LyricsResultRanker.rankedCandidates(listOf(second, first), request).map { it.result.provider }

        assertEquals(forward, reversed)
    }

    @Test
    fun failingOrSlowProviderFallsBackInsteadOfBreakingTheFlow() = runBlocking {
        val failed = isolatedLyricsProviderCall(timeoutMs = 1_000L, fallback = { "fallback" }) {
            throw IllegalStateException("provider down")
        }
        val timedOut = isolatedLyricsProviderCall(timeoutMs = 20L, fallback = { "fallback" }) {
            delay(5_000L)
            "late"
        }

        assertEquals("fallback", failed)
        assertEquals("fallback", timedOut)
    }

    private fun candidate(
        provider: String,
        title: String,
        artist: String,
        durationSec: Long,
        wordTimed: Boolean,
        synced: Boolean = true,
        lineText: String = "Line"
    ): LyricsCandidate {
        val lines = (0 until 12).map { index ->
            val start = index * 14_000L
            val text = "$lineText number $index"
            LyricLine(
                startMs = start,
                endMs = start + 12_000L,
                text = text,
                translated = "",
                words = if (wordTimed) {
                    listOf(
                        LyricWord(start, start + 4_000L, "$lineText "),
                        LyricWord(start + 4_000L, start + 11_000L, "number $index")
                    )
                } else {
                    emptyList()
                }
            )
        }
        return LyricsCandidate(
            result = LyricsRepository.LyricsResult(synced, lines, provider, 90, false),
            title = title,
            artist = artist,
            durationSec = durationSec
        )
    }
}
