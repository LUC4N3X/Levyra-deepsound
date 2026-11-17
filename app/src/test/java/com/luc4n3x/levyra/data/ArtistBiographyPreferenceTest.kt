package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.data.apple.AppleArtistInfo
import com.luc4n3x.levyra.data.apple.AppleArtistPage
import com.luc4n3x.levyra.domain.ArtistBiography
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistBiographyPreferenceTest {
    private fun wikipedia(language: String) = ArtistBiography(
        text = "Wikipedia biography text in $language for this artist.",
        sourceLabel = "Wikipedia",
        sourceUrl = "https://$language.wikipedia.org/wiki/Artist",
        languageCode = language,
        confidence = 90
    )

    private fun apple(language: String) = ArtistBiography(
        text = "Apple Music biography text in $language for this artist.",
        sourceLabel = APPLE_MUSIC_BIOGRAPHY_SOURCE,
        languageCode = language,
        confidence = 100
    )

    @Test
    fun appleInUserLanguageBeatsWikipediaInUserLanguage() {
        assertTrue(shouldReplaceArtistBiography(wikipedia("de"), apple("de"), "de"))
        assertFalse(shouldReplaceArtistBiography(apple("de"), wikipedia("de"), "de"))
    }

    @Test
    fun wikipediaInUserLanguageBeatsAppleInEnglish() {
        assertFalse(shouldReplaceArtistBiography(wikipedia("ru"), apple("en"), "ru"))
        assertTrue(shouldReplaceArtistBiography(apple("en"), wikipedia("ru"), "ru"))
    }

    @Test
    fun appleInEnglishBeatsWikipediaInEnglishAndOtherLanguages() {
        assertTrue(shouldReplaceArtistBiography(wikipedia("en"), apple("en"), "ru"))
        assertTrue(shouldReplaceArtistBiography(wikipedia("de"), apple("en"), "ru"))
    }

    @Test
    fun wikipediaInAnotherLanguageIsStillUsedWhenNothingElseExists() {
        assertTrue(shouldReplaceArtistBiography(null, wikipedia("de"), "ru"))
        assertFalse(shouldReplaceArtistBiography(wikipedia("ru"), wikipedia("de"), "ru"))
    }

    @Test
    fun unknownSourcesNeverReplaceABiography() {
        val youtube = ArtistBiography(text = "YouTube description text for this artist.", sourceLabel = "YouTube")
        assertFalse(shouldReplaceArtistBiography(null, youtube, "en"))
        assertTrue(shouldReplaceArtistBiography(youtube, wikipedia("en"), "en"))
    }

    @Test
    fun appleBiographyStripsMarkupAndLinksTheCatalogPage() {
        val page = AppleArtistPage(
            id = "978839124",
            name = "HUGEL",
            artworkTemplate = "",
            squareMotion = null,
            wideMotion = null,
            latestRelease = null,
            topSongs = emptyList(),
            shelves = emptyList(),
            info = AppleArtistInfo(
                bio = "French house DJ <i>HUGEL</i> is best known for party tracks.<br/>He lives in Marseille.",
                origin = "",
                bornOrFormed = "",
                isGroup = false,
                genres = emptyList()
            ),
            fetchedAtMs = 0L
        )
        val biography = requireNotNull(appleArtistBiography(page, "ru", "en"))
        assertEquals("French house DJ HUGEL is best known for party tracks.\nHe lives in Marseille.", biography.text)
        assertEquals("https://music.apple.com/ru/artist/978839124", biography.sourceUrl)
        assertEquals("en", biography.languageCode)
        assertNull(appleArtistBiography(page.copy(info = page.info?.copy(bio = "")), "ru", "en"))
    }
}
