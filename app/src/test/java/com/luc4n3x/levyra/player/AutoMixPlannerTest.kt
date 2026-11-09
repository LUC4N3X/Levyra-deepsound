package com.luc4n3x.levyra.player

import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AutoMixPlannerTest {
    @Test
    fun crossfadeUsesEqualPowerCurve() {
        val start = equalPowerCrossfade(0f)
        val middle = equalPowerCrossfade(.5f)
        val end = equalPowerCrossfade(1f)

        assertEquals(1f, start.outgoing, .001f)
        assertEquals(0f, start.incoming, .001f)
        assertEquals(0.7071f, middle.outgoing, .001f)
        assertEquals(0.7071f, middle.incoming, .001f)
        assertEquals(0f, end.outgoing, .001f)
        assertEquals(1f, end.incoming, .001f)
    }

    @Test
    fun nativeVideoRepeatOneAndLowRamNeverStartSecondPlayer() {
        val settings = LevyraAudioSettings(crossfadeSeconds = 6)
        assertNull(planAutoMix(track(), track("next"), settings, RepeatMode.Off, videoMode = true, lowRam = false))
        assertNull(planAutoMix(track(), track("next"), settings, RepeatMode.One, videoMode = false, lowRam = false))
        assertNull(planAutoMix(track(), track("next"), settings, RepeatMode.Off, videoMode = false, lowRam = true))
    }

    @Test
    fun autoMixAdaptsDurationWithoutExceedingBounds() {
        val current = track().copy(energy = 70, vocal = 30)
        val next = track("next").copy(energy = 75, vocal = 30)
        val plan = planAutoMix(
            current,
            next,
            LevyraAudioSettings(crossfadeSeconds = 6, djSoftMode = true),
            RepeatMode.Off,
            videoMode = false,
            lowRam = false
        )
        assertEquals(7_500L, plan?.transitionMs)
    }

    @Test
    fun consecutiveTracksOfSameCatalogReleaseSkipCrossfade() {
        val current = albumTrack("a", track = 3)
        val next = albumTrack("b", track = 4)

        assertNull(planAutoMix(current, next, crossfade, RepeatMode.Off, videoMode = false, lowRam = false))
    }

    @Test
    fun discBoundaryOfSameReleaseStaysContinuous() {
        val current = albumTrack("a", track = 12, disc = 1)
        val next = albumTrack("b", track = 1, disc = 2)

        assertNull(planAutoMix(current, next, crossfade, RepeatMode.Off, videoMode = false, lowRam = false))
    }

    @Test
    fun sameReleaseOutOfRunningOrderStillCrossfades() {
        val current = albumTrack("a", track = 3)
        val next = albumTrack("b", track = 7)

        assertNotNull(planAutoMix(current, next, crossfade, RepeatMode.Off, videoMode = false, lowRam = false))
    }

    @Test
    fun leavingAlbumIntoRadioStillCrossfades() {
        val current = albumTrack("a", track = 10)
        val next = albumTrack("b", track = 11).copy(albumBrowseId = "MPREb_other")

        assertNotNull(planAutoMix(current, next, crossfade, RepeatMode.Off, videoMode = false, lowRam = false))
    }

    @Test
    fun catalogIdentityWithoutTrackNumbersDependsOnShuffle() {
        val current = albumTrack("a", track = 0)
        val next = albumTrack("b", track = 0)

        assertNull(planAutoMix(current, next, crossfade, RepeatMode.Off, videoMode = false, lowRam = false, shuffleEnabled = false))
        assertNotNull(planAutoMix(current, next, crossfade, RepeatMode.Off, videoMode = false, lowRam = false, shuffleEnabled = true))
    }

    @Test
    fun upcIdentifiesReleaseWhenBrowseIdIsMissing() {
        val current = albumTrack("a", track = 5).copy(albumBrowseId = "", upc = "0602435000000")
        val next = albumTrack("b", track = 6).copy(albumBrowseId = "", upc = "0602435000000")

        assertNull(planAutoMix(current, next, crossfade, RepeatMode.Off, videoMode = false, lowRam = false))
    }

    @Test
    fun conflictingUpcOverridesMatchingTitles() {
        val current = track("a").copy(album = "Album", artist = "Artist", upc = "111", trackNumber = 4)
        val next = track("b").copy(album = "Album", artist = "Artist", upc = "222", trackNumber = 5)

        assertNotNull(planAutoMix(current, next, crossfade, RepeatMode.Off, videoMode = false, lowRam = false))
    }

    @Test
    fun albumTitleAloneIsNotTrustedWithoutRunningOrder() {
        val current = track("a").copy(album = "Greatest Hits", artist = "Band")
        val next = track("b").copy(album = "Greatest Hits", artist = "Band")

        assertNotNull(planAutoMix(current, next, crossfade, RepeatMode.Off, videoMode = false, lowRam = false))
    }

    @Test
    fun albumTitleArtistAndRunningOrderIdentifyReleaseWithoutCatalogId() {
        val current = track("a").copy(album = "Live at Wembley", artist = "Band", trackNumber = 2)
        val next = track("b").copy(album = " live at wembley ", artist = "BAND", trackNumber = 3)

        assertNull(planAutoMix(current, next, crossfade, RepeatMode.Off, videoMode = false, lowRam = false))
    }

    @Test
    fun sameTitleByDifferentArtistStillCrossfades() {
        val current = track("a").copy(album = "Greatest Hits", artist = "Band", trackNumber = 2)
        val next = track("b").copy(album = "Greatest Hits", artist = "Other Band", trackNumber = 3)

        assertNotNull(planAutoMix(current, next, crossfade, RepeatMode.Off, videoMode = false, lowRam = false))
    }

    @Test
    fun existingGuardsStillWinForSameReleasePairs() {
        val current = albumTrack("a", track = 3)
        val next = albumTrack("b", track = 7)

        assertNull(planAutoMix(current, next, crossfade, RepeatMode.One, videoMode = false, lowRam = false))
        assertNull(planAutoMix(current, next, crossfade, RepeatMode.Off, videoMode = true, lowRam = false))
        assertNull(planAutoMix(current, next, crossfade, RepeatMode.Off, videoMode = false, lowRam = true))
    }

    private val crossfade = LevyraAudioSettings(crossfadeSeconds = 6)

    private fun albumTrack(id: String, track: Int, disc: Int = 1) = track(id).copy(
        albumBrowseId = "MPREb_release",
        trackNumber = track,
        discNumber = disc
    )

    private fun track(id: String = "current") = Track(
        id = id,
        title = id,
        artist = "Artist",
        album = "Album",
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0
    )
}
