package com.luc4n3x.levyra.ui.download

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.LevyraSkin
import com.luc4n3x.levyra.ui.i18n.LevyraDownloadQualityStrings
import com.luc4n3x.levyra.ui.theme.LevyraThemeController
import com.luc4n3x.levyra.ui.theme.LevyraThemes
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadQualitySkinTest {
    @After
    fun restoreDefault() {
        LevyraThemeController.apply(LevyraThemes.LEVYRA_AURA)
    }

    @Test
    fun everySkinOwnsADedicatedPickerPresentation() {
        val expected = mapOf(
            LevyraSkin.Default to DownloadQualitySkin.Expressive,
            LevyraSkin.Nocturne78 to DownloadQualitySkin.Nocturne,
            LevyraSkin.Prism to DownloadQualitySkin.Prism,
            LevyraSkin.Torque to DownloadQualitySkin.Torque,
            LevyraSkin.Ivory to DownloadQualitySkin.Ivory,
            LevyraSkin.SpotifyStyle to DownloadQualitySkin.Spotify,
            LevyraSkin.AppleMusicStyle to DownloadQualitySkin.Apple,
            LevyraSkin.YouTubeMusicStyle to DownloadQualitySkin.YouTube
        )
        assertEquals(LevyraSkin.entries.toSet(), expected.keys)
        expected.forEach { (skin, presentation) ->
            LevyraThemeController.apply(LevyraThemes.LEVYRA_AURA, skin = skin)
            assertEquals(skin.name, presentation, activeDownloadQualitySkin())
        }
    }

    @Test
    fun everyLanguageHasCompletePickerCopy() {
        LevyraLanguageCatalog.languages.forEach { language ->
            val copy = LevyraDownloadQualityStrings.forCode(language.code)
            listOf(
                copy.title, copy.checking, copy.checkingMore, copy.hiResLossless, copy.lossless, copy.high,
                copy.standard, copy.compact, copy.compressed, copy.best, copy.unavailableTitle, copy.unavailableBody,
                copy.errorTitle, copy.errorBody, copy.retry, copy.qualityLost
            ).forEach { assertTrue(language.code, it.isNotBlank()) }
        }
    }
}
