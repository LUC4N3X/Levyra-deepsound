package com.luc4n3x.levyra.data.hqaudio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlternativeTrackTextTest {
    @Test
    fun normalizationRemovesHarmlessRepresentationDifferences() {
        assertEquals("dont start now", AlternativeTrackText.normalize("  DON’T   Start Now! "))
        assertEquals("cafe del mar", AlternativeTrackText.normalize("Café del Mar"))
        assertEquals("rock and roll", AlternativeTrackText.normalize("Rock &amp; Roll"))
        assertEquals("rock and roll", AlternativeTrackText.normalize("Rock & Roll"))
        assertEquals("perche", AlternativeTrackText.normalize("Perché"))
    }

    @Test
    fun htmlEntitiesAreDecodedIncludingNumericForms() {
        assertEquals("Abel \"The Weeknd\" Tesfaye", AlternativeTrackText.decodeHtmlEntities("Abel &quot;The Weeknd&quot; Tesfaye"))
        assertEquals("fast forward >>", AlternativeTrackText.decodeHtmlEntities("fast forward &gt;&gt;"))
        assertEquals("It's", AlternativeTrackText.decodeHtmlEntities("It&#39;s"))
        assertEquals("é", AlternativeTrackText.decodeHtmlEntities("&#xE9;"))
    }

    @Test
    fun versionMarkersAreDetectedWithoutErasingThem() {
        assertEquals(setOf(TrackVersionMarker.LIVE), AlternativeTrackText.title("Blinding Lights - Live at the Kia Forum").markers)
        assertEquals(setOf(TrackVersionMarker.REMIX), AlternativeTrackText.title("Blinding Lights (Chromatics Remix)").markers)
        assertEquals(setOf(TrackVersionMarker.KARAOKE), AlternativeTrackText.title("Blinding Lights (Karaoke Version)").markers)
        assertEquals(
            setOf(TrackVersionMarker.SLOWED, TrackVersionMarker.REVERB),
            AlternativeTrackText.title("tuta gold (slowed + reverb)").markers
        )
        assertEquals(
            setOf(TrackVersionMarker.RADIO_EDIT, TrackVersionMarker.EDIT),
            AlternativeTrackText.title("Titanium (Radio Edit)").markers
        )
        assertEquals(setOf(TrackVersionMarker.REMASTER), AlternativeTrackText.title("Bohemian Rhapsody - Remastered 2011").markers)
        assertEquals(setOf(TrackVersionMarker.SPED_UP), AlternativeTrackText.title("TUTA GOLD (Nightcore)").markers)
        assertEquals(setOf(TrackVersionMarker.SPED_UP), AlternativeTrackText.title("tuta gold sped up").markers)
    }

    @Test
    fun ordinaryTitleWordsAreNotMistakenForVersions() {
        assertTrue(AlternativeTrackText.title("Live Forever").markers.isEmpty())
        assertTrue(AlternativeTrackText.title("Stereo Love").markers.isEmpty())
        assertTrue(AlternativeTrackText.title("Cover Me Up").markers.isEmpty())
        assertEquals("i cant get no satisfaction", AlternativeTrackText.title("(I Can't Get No) Satisfaction").core)
    }

    @Test
    fun neutralDescriptorsAndFeaturedCreditsLeaveTheCoreTitle() {
        val levitating = AlternativeTrackText.title("Levitating (feat. DaBaby)")
        assertEquals("levitating", levitating.core)
        assertEquals(setOf("dababy"), levitating.featuredArtists)
        assertTrue(levitating.markers.isEmpty())
        assertEquals("kesariya", AlternativeTrackText.title("Kesariya (From \"Brahmastra\")").core)
        assertEquals("blinding lights", AlternativeTrackText.title("Blinding Lights (Official Audio)").core)
        assertEquals("one kiss", AlternativeTrackText.title("One Kiss feat. Dua Lipa").core)
        assertTrue(AlternativeTrackText.title("One Kiss (Original Mix)").markers.isEmpty())
    }

    @Test
    fun genericVersionKeepsItsLabel() {
        val identity = AlternativeTrackText.title("Love Story (Taylor's Version)")
        assertEquals(setOf(TrackVersionMarker.VERSION), identity.markers)
        assertEquals(setOf("taylors version"), identity.versionLabels)
    }

    @Test
    fun explicitHintsAreReadFromDescriptors() {
        assertEquals(false, AlternativeTrackText.title("Song (Clean)").explicitHint)
        assertEquals(true, AlternativeTrackText.title("Song [Explicit]").explicitHint)
        assertNull(AlternativeTrackText.title("Song").explicitHint)
    }

    @Test
    fun artistCreditsSplitCommonSeparatorsAndIgnoreLeadingArticle() {
        assertEquals(listOf("calvin harris", "dua lipa"), AlternativeTrackText.artistNames("Calvin Harris & Dua Lipa"))
        assertEquals(listOf("dua lipa", "dababy"), AlternativeTrackText.artistNames("Dua Lipa feat. DaBaby"))
        assertEquals(listOf("weeknd"), AlternativeTrackText.artistNames("The Weeknd"))
        assertEquals(listOf("lil nas x"), AlternativeTrackText.artistNames("Lil Nas X"))
        assertEquals("beyonce", AlternativeTrackText.artistCredit("Beyoncé, JAY-Z").primary)
    }

    @Test
    fun albumEditionsAreClassifiedNotBlindlyStripped() {
        assertEquals(AlbumIdentity("after hours", setOf(AlbumEdition.DELUXE)), AlternativeTrackText.album("After Hours (Deluxe)"))
        assertEquals(setOf(AlbumEdition.REMASTERED), AlternativeTrackText.album("A Night at the Opera (2011 Remaster)").editions)
        assertEquals(AlbumIdentity("levitating", setOf(AlbumEdition.SINGLE)), AlternativeTrackText.album("Levitating - Single"))
        assertEquals("future nostalgia", AlternativeTrackText.album("Future Nostalgia (The Moonlight Edition)").core)
        assertEquals(setOf(AlbumEdition.SOUNDTRACK), AlternativeTrackText.album("Barbie (Original Motion Picture Soundtrack)").editions)
        assertEquals("levitating", AlternativeTrackText.album("Levitating (feat. DaBaby)").core)
    }
}
