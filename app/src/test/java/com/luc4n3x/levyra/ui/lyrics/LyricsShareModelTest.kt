package com.luc4n3x.levyra.ui.lyrics

import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricWord
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsShareModelTest {
    private fun line(text: String, translated: String = "", romanized: String = "") =
        LyricLine(startMs = 0L, endMs = 0L, text = text, translated = translated, romanized = romanized)

    private val lines = listOf(
        line("one"),
        line("two"),
        line(""),
        line("three"),
        line("four"),
        line("five"),
        line("six"),
        line("seven"),
        line("eight")
    )

    private fun track(id: String, title: String = "Title") = Track(
        id = id,
        title = title,
        artist = "Artist",
        album = "",
        durationMs = 0L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0
    )

    @Test
    fun firstTapSelectsOnlyThatLine() {
        assertEquals(3..3, toggleLyricsShareSelection(null, 3, lines))
    }

    @Test
    fun blankInstrumentalAndMetadataLinesAreNeverSelectable() {
        assertNull(toggleLyricsShareSelection(null, 2, lines))
        assertEquals(1..1, toggleLyricsShareSelection(1..1, 2, lines))
        assertFalse(isLyricsShareSelectable(LyricLine(0, 0, "♪", isInstrumental = true)))
        assertFalse(isLyricsShareSelectable(LyricLine(0, 0, "Written by", isMetadata = true)))
    }

    @Test
    fun tappingASecondLineSelectsTheWholeContiguousSection() {
        val range = toggleLyricsShareSelection(3..3, 5, lines)
        assertEquals(3..5, range)
        assertEquals(1..5, toggleLyricsShareSelection(range, 1, lines))
    }

    @Test
    fun selectionNeverExceedsTheLineLimitAndKeepsTheTappedLine() {
        val forward = toggleLyricsShareSelection(0..0, 7, lines)
        assertEquals(LYRICS_SHARE_MAX_LINES, lyricsShareSelectableCount(forward, lines))
        assertEquals(7, forward?.last)
        val backward = toggleLyricsShareSelection(6..6, 0, lines)
        assertEquals(LYRICS_SHARE_MAX_LINES, lyricsShareSelectableCount(backward, lines))
        assertEquals(0, backward?.first)
    }

    @Test
    fun blankLinesInsideTheRangeDoNotCountTowardTheLimit() {
        val range = toggleLyricsShareSelection(0..0, 5, lines)
        assertEquals(0..5, range)
        assertEquals(5, lyricsShareSelectableCount(range, lines))
        assertEquals(listOf("one", "two", "three", "four", "five"), lyricsShareSelectedLines(range, lines).map { it.text })
    }

    @Test
    fun tappingAnEdgeShrinksAndTappingTheOnlyLineClearsTheSelection() {
        assertEquals(1..5, toggleLyricsShareSelection(0..5, 0, lines))
        assertEquals(0..4, toggleLyricsShareSelection(0..5, 5, lines))
        assertNull(toggleLyricsShareSelection(4..4, 4, lines))
    }

    @Test
    fun edgeShrinkSkipsBlankSeparators() {
        assertEquals(3..4, toggleLyricsShareSelection(1..4, 1, lines))
        assertEquals(0..1, toggleLyricsShareSelection(0..3, 3, lines))
    }

    @Test
    fun tappingInsideTheSelectionTrimsItToThatLine() {
        assertEquals(3..4, toggleLyricsShareSelection(3..6, 4, lines))
    }

    @Test
    fun outOfRangeTapsAreIgnored() {
        assertEquals(1..2, toggleLyricsShareSelection(1..2, 99, lines))
        assertNull(toggleLyricsShareSelection(null, -1, lines))
    }

    @Test
    fun snapshotKeepsTheOriginalLineAndAttachesTranslationAndRomanization() {
        val selected = listOf(
            LyricLine(
                startMs = 0L,
                endMs = 1L,
                text = " こんにちは ",
                translated = "Hello",
                words = listOf(LyricWord(0L, 1L, "こんにちは", romanized = "konnichiwa"))
            ),
            line("")
        )
        val snapshot = lyricsShareSnapshot(track("a"), selected)!!
        assertEquals(1, snapshot.lines.size)
        assertEquals("こんにちは", snapshot.lines.single().original)
        assertEquals("konnichiwa", snapshot.lines.single().romanization)
        assertEquals(
            listOf(LyricsShareTextMode.ORIGINAL, LyricsShareTextMode.TRANSLATION, LyricsShareTextMode.ROMANIZATION),
            snapshot.availableModes
        )
        assertEquals(listOf("Hello"), snapshot.textLines(LyricsShareTextMode.TRANSLATION))
    }

    @Test
    fun modesWithoutDataFallBackToTheOriginalLine() {
        val snapshot = lyricsShareSnapshot(track("a"), listOf(line("one"), line("two", translated = "due")))!!
        assertEquals(listOf("one", "due"), snapshot.textLines(LyricsShareTextMode.TRANSLATION))
        assertEquals(listOf("one", "two"), snapshot.textLines(LyricsShareTextMode.ORIGINAL))
        assertEquals(listOf("one", "two"), snapshot.textLines(LyricsShareTextMode.ROMANIZATION))
        assertEquals(listOf(LyricsShareTextMode.ORIGINAL, LyricsShareTextMode.TRANSLATION), snapshot.availableModes)
    }

    @Test
    fun snapshotIsBoundToTheTrackItWasCreatedFrom() {
        val first = lyricsShareSnapshot(track("a", "First"), listOf(line("lyrics of a")))!!
        val second = lyricsShareSnapshot(track("b", "Second"), listOf(line("lyrics of b")))!!
        assertEquals("a", first.trackId)
        assertEquals("First", lyricsShareCardContent(first, LyricsShareTextMode.ORIGINAL).title)
        assertEquals(listOf("lyrics of a"), lyricsShareCardContent(first, LyricsShareTextMode.ORIGINAL).lyrics)
        assertEquals("b", second.trackId)
        assertEquals(listOf("lyrics of b"), lyricsShareCardContent(second, LyricsShareTextMode.ORIGINAL).lyrics)
    }

    @Test
    fun snapshotRequiresATrackAndAtLeastOneSelectableLine() {
        assertNull(lyricsShareSnapshot(null, listOf(line("one"))))
        assertNull(lyricsShareSnapshot(track("a"), listOf(line(""), line("  "))))
    }

    @Test
    fun snapshotIsCappedAtTheSelectionLimit() {
        val snapshot = lyricsShareSnapshot(track("a"), lines)!!
        assertEquals(LYRICS_SHARE_MAX_LINES, snapshot.lines.size)
    }

    @Test
    fun cardTextPreservesUnicodeAndBoundsVeryLongLines() {
        val long = "🎵".repeat(LYRICS_SHARE_MAX_LINE_CODE_POINTS + 50)
        val snapshot = lyricsShareSnapshot(track("a"), listOf(line("שלום, עולם!"), line(long)))!!
        val content = lyricsShareCardContent(snapshot, LyricsShareTextMode.ORIGINAL)
        assertEquals("שלום, עולם!", content.lyrics.first())
        assertEquals(LYRICS_SHARE_MAX_LINE_CODE_POINTS, content.lyrics.last().codePointCount(0, content.lyrics.last().length))
    }

    @Test
    fun captionListsTitleAndArtistOnly() {
        val content = LyricsShareCardContent("Song", "Band", listOf("secret line"))
        assertEquals("Song — Band", lyricsShareCaption(content))
        assertEquals("Song", lyricsShareCaption(content.copy(artist = "")))
        assertTrue(!lyricsShareCaption(content).contains("secret"))
    }

    @Test
    fun cardBackgroundAlwaysKeepsWhiteTextReadable() {
        val bright = listOf(0xFFFFFF00.toInt(), 0xFFFFFFFF.toInt(), 0xFF00FFFF.toInt(), 0xFFFFA500.toInt(), 0xFF808080.toInt(), 0xFF000000.toInt())
        bright.forEach { color ->
            val background = LyricsShareCardColors.background(color, 0xFF26B2D6.toInt())
            assertTrue("$color -> $background", LyricsShareCardColors.contrastWithWhite(background) >= 4.5)
        }
        assertEquals(
            LyricsShareCardColors.background(0xFF26B2D6.toInt(), 0),
            LyricsShareCardColors.background(0, 0xFF26B2D6.toInt())
        )
    }
}
