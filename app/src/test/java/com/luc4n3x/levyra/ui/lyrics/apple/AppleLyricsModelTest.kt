package com.luc4n3x.levyra.ui.lyrics.apple

import com.luc4n3x.levyra.domain.LevyraLyricsStudioSettings
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricVocalRole
import com.luc4n3x.levyra.domain.LyricWord
import com.luc4n3x.levyra.domain.LyricsStylePreset
import com.luc4n3x.levyra.domain.LyricsViewerStyle
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.ui.i18n.appleLyricsCopy
import com.luc4n3x.levyra.ui.i18n.appleLyricsLocalizationCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppleLyricsModelTest {
    private fun line(start: Long, end: Long, text: String = "line $start", role: LyricVocalRole = LyricVocalRole.MAIN) =
        LyricLine(startMs = start, endMs = end, text = text, role = role)

    @Test
    fun backgroundVocalsAttachToTheirMainLine() {
        val rows = appleLyricsRows(
            listOf(line(1_000, 3_000), line(2_000, 3_500, role = LyricVocalRole.BACKGROUND), line(4_000, 6_000)),
            synced = true, showSecondaryVoices = true
        )
        val first = rows[0] as AppleLyricsRow.Vocal
        assertEquals(2, rows.size)
        assertEquals(1, first.background.size)
        assertEquals(3_500L, first.endMs)
    }

    @Test
    fun hidingSecondaryVoicesDropsBackgroundOnly() {
        val rows = appleLyricsRows(
            listOf(line(1_000, 3_000), line(2_000, 3_500, role = LyricVocalRole.BACKGROUND), line(4_000, 6_000, role = LyricVocalRole.DUET_RIGHT)),
            synced = true, showSecondaryVoices = false
        )
        assertEquals(2, rows.size)
        assertTrue((rows[0] as AppleLyricsRow.Vocal).background.isEmpty())
        assertEquals(AppleLyricsSide.END, (rows[1] as AppleLyricsRow.Vocal).side)
        assertTrue((rows[1] as AppleLyricsRow.Vocal).duet)
    }

    @Test
    fun longGapsAndIntroBecomeInstrumentalRows() {
        val rows = appleLyricsRows(listOf(line(8_000, 10_000), line(18_000, 20_000), line(21_000, 22_000)), synced = true, showSecondaryVoices = true)
        assertTrue(rows[0] is AppleLyricsRow.Instrumental)
        assertTrue(rows[2] is AppleLyricsRow.Instrumental)
        assertEquals(5, rows.size)
        assertEquals(10_000L, rows[2].startMs)
        assertEquals(18_000L, rows[2].endMs)
    }

    @Test
    fun shortGapsStayVocal() {
        val rows = appleLyricsRows(listOf(line(1_000, 2_000), line(8_500, 9_000)), synced = true, showSecondaryVoices = true)
        assertTrue(rows.none { it is AppleLyricsRow.Instrumental })
    }

    @Test
    fun moveDurationFollowsTheAppleGapCurve() {
        assertEquals(480L, appleLyricsMoveDurationMs(0))
        assertEquals(480L, appleLyricsMoveDurationMs(200))
        assertEquals(750L, appleLyricsMoveDurationMs(750))
        assertEquals(750L, appleLyricsMoveDurationMs(5_000))
        assertEquals(615L, appleLyricsMoveDurationMs(475))
    }

    @Test
    fun focusAnticipatesTheNextLineByItsMoveDuration() {
        val rows = appleLyricsRows(listOf(line(1_000, 3_000), line(3_000, 5_000)), synced = true, showSecondaryVoices = true)
        val leads = appleLyricsLeads(rows)
        assertEquals(480L, leads[1])
        assertEquals(-1, appleLyricsFocusIndex(rows, leads, 500))
        assertEquals(0, appleLyricsFocusIndex(rows, leads, 2_400))
        assertEquals(1, appleLyricsFocusIndex(rows, leads, 2_600))
        assertEquals(1, appleLyricsFocusIndex(rows, leads, 60_000))
    }

    @Test
    fun seekBackwardsReturnsToEarlierFocus() {
        val rows = appleLyricsRows((0 until 200).map { line(it * 2_000L + 1_000, it * 2_000L + 2_800) }, synced = true, showSecondaryVoices = true)
        val leads = appleLyricsLeads(rows)
        assertEquals(150, appleLyricsFocusIndex(rows, leads, 301_500))
        assertEquals(10, appleLyricsFocusIndex(rows, leads, 21_500))
    }

    @Test
    fun staggerMatchesItemAnimatorDelays() {
        assertEquals(0L, appleLyricsStaggerDelayMs(4, 4))
        assertEquals(0L, appleLyricsStaggerDelayMs(2, 4))
        assertEquals(25L, appleLyricsStaggerDelayMs(5, 4))
        assertEquals(44L, appleLyricsStaggerDelayMs(6, 4))
        assertEquals(56L, appleLyricsStaggerDelayMs(7, 4))
        assertEquals(63L, appleLyricsStaggerDelayMs(8, 4))
        assertEquals(63L, appleLyricsStaggerDelayMs(30, 4))
    }

    @Test
    fun emphasisAppliesOnlyToLongWords() {
        assertTrue(appleLyricsEmphasized(LyricWord(0, 1_400, "love")))
        assertFalse(appleLyricsEmphasized(LyricWord(0, 600, "love")))
        assertFalse(appleLyricsEmphasized(LyricWord(0, 1_400, "a")))
        assertEquals(1f, appleLyricsEmphasisScale(1_000), 0.0001f)
        assertEquals(1.14f, appleLyricsEmphasisScale(5_000), 0.0001f)
        assertEquals(0f, appleLyricsEmphasisEnvelope(0, 0, 1_400, 0, 4), 0.0001f)
        assertTrue(appleLyricsEmphasisEnvelope(500, 0, 1_400, 0, 4) > 0f)
    }

    @Test
    fun liftSpringSettlesAtTarget() {
        assertEquals(0f, appleLyricsLiftFraction(0), 0.0001f)
        assertEquals(1f, appleLyricsLiftFraction(3_000), 0.02f)
    }

    @Test
    fun skinDefaultFollowsTheAppleSkinOnly() {
        val defaults = LevyraLyricsStudioSettings()
        assertTrue(defaults.usesAppleMusicLyrics(appleMusicSkin = true))
        assertFalse(defaults.usesAppleMusicLyrics(appleMusicSkin = false))
        assertTrue(defaults.copy(viewerStyle = LyricsViewerStyle.APPLE_MUSIC).usesAppleMusicLyrics(appleMusicSkin = false))
    }

    @Test
    fun viewerOptionsPersistAndSurvivePresets() {
        val settings = LevyraLyricsStudioSettings(
            viewerStyle = LyricsViewerStyle.APPLE_MUSIC, autoScrollEnabled = false, showOriginalLyrics = false,
            romanizationEnabled = false, appleBlurEnabled = true
        )
        assertEquals(settings, LevyraLyricsStudioSettings.decode(settings.encode()))
        val preset = settings.applyPreset(LyricsStylePreset.MINIMAL)
        assertEquals(LyricsViewerStyle.APPLE_MUSIC, preset.viewerStyle)
        assertFalse(preset.autoScrollEnabled)
        assertTrue(preset.appleBlurEnabled)
        assertEquals(LyricsViewerStyle.SKIN_DEFAULT, LevyraLyricsStudioSettings.decode("{}").viewerStyle)
    }

    @Test
    fun everySupportedLanguageShipsAppleLyricsCopy() {
        val supported = LevyraLanguageCatalog.languages.map { it.code }.toSet()
        assertEquals(supported, appleLyricsLocalizationCodes())
        supported.forEach { code ->
            val copy = appleLyricsCopy(code)
            assertTrue(code, listOf(copy.lyricsStyle, copy.skinDefault, copy.originalLyrics, copy.autoScroll).all(String::isNotBlank))
        }
    }
}
