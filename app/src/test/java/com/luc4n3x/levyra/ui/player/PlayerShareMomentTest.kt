package com.luc4n3x.levyra.ui.player

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.SHARE_MOMENT_TIME_TOKEN
import com.luc4n3x.levyra.ui.i18n.shareMomentCopy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class PlayerShareMomentTest {

    @Test
    fun momentIsOfferedOnlyAfterTheOpeningSecondsAndBeforeTheEnd() {
        assertNull(shareMomentSeconds(positionMs = 4_999L, durationMs = 200_000L))
        assertEquals(83L, shareMomentSeconds(positionMs = 83_900L, durationMs = 200_000L))
        assertNull(shareMomentSeconds(positionMs = 200_000L, durationMs = 200_000L))
        assertEquals(12L, shareMomentSeconds(positionMs = 12_000L, durationMs = 0L))
    }

    @Test
    fun linkStartsPlaybackAtTheSharedSecond() {
        assertEquals("https://www.youtube.com/watch?v=abc123&t=83s", shareMomentUrl("abc123", 83L))
    }

    @Test
    fun timestampUsesHoursOnlyForLongTracks() {
        assertEquals("0:07", formatShareMomentTimestamp(7L))
        assertEquals("1:23", formatShareMomentTimestamp(83L))
        assertEquals("1:02:03", formatShareMomentTimestamp(3_723L))
    }

    @Test
    fun timestampAndLinkStayAsciiUnderAnArabicDigitLocale() {
        val previous = Locale.getDefault(Locale.Category.FORMAT)
        Locale.setDefault(Locale.Category.FORMAT, Locale.forLanguageTag("ar-u-nu-arab"))
        try {
            assertEquals("1:02:03", formatShareMomentTimestamp(3_723L))
            assertEquals(
                "Song (من 1:23)\nhttps://www.youtube.com/watch?v=abc123&t=83s",
                shareMomentText("Song", "", "abc123", 83L, "من $SHARE_MOMENT_TIME_TOKEN")
            )
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, previous)
        }
    }

    @Test
    fun sharedTextNamesTheMomentAndCarriesTheLink() {
        assertEquals(
            "Song — Artist (from 1:23)\nhttps://www.youtube.com/watch?v=abc123&t=83s",
            shareMomentText("Song", "Artist", "abc123", 83L, "from {time}")
        )
        assertEquals(
            "Song (0:07 から)\nhttps://www.youtube.com/watch?v=abc123&t=7s",
            shareMomentText("Song", "", "abc123", 7L, "{time} から")
        )
    }

    @Test
    fun subjectOmitsTheSeparatorWithoutAnArtist() {
        assertEquals("Song — Artist", shareMomentSubject("Song", "Artist"))
        assertEquals("Song", shareMomentSubject("Song", " "))
    }

    @Test
    fun everySupportedLanguageHasItsOwnLabelsAndATimeSlot() {
        val english = LevyraStrings.forCode("en").shareMomentCopy()
        LevyraLanguageCatalog.languages.map { it.code }.forEach { code ->
            val copy = LevyraStrings.forCode(code).shareMomentCopy()
            assertTrue(code, SHARE_MOMENT_TIME_TOKEN in copy.fromTemplate)
            if (code != "en") assertNotEquals(code, english.action, copy.action)
        }
    }
}
