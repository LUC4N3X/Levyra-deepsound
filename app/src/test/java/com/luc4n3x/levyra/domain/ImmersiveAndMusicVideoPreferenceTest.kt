package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ImmersiveAndMusicVideoPreferenceTest {
    private fun track(
        id: String,
        videoType: String = "",
        audioVideoId: String = "",
        counterpartVideoId: String = "",
        source: String = "YouTube Music"
    ) = Track(
        id = id,
        title = "Song $id",
        artist = "Artist",
        album = "Album",
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "https://www.youtube.com/watch?v=$id",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = source,
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 1,
        accentEnd = 2,
        videoType = videoType,
        audioVideoId = audioVideoId,
        counterpartVideoId = counterpartVideoId
    )

    @Test
    fun barsStayVisibleOutsideThePlayerUnlessAppliedEverywhere() {
        val settings = LevyraImmersiveSettings(hideStatusBar = true)
        assertEquals(ImmersiveBars.None, settings.hiddenBars(fullPlayerVisible = false))
        assertEquals(ImmersiveBars(status = true, navigation = false), settings.hiddenBars(fullPlayerVisible = true))
        assertEquals(
            ImmersiveBars(status = true, navigation = false),
            settings.copy(everywhere = true).hiddenBars(fullPlayerVisible = false)
        )
    }

    @Test
    fun fullImmersiveHidesBothBarsRegardlessOfIndividualToggles() {
        val settings = LevyraImmersiveSettings(fullImmersive = true)
        assertEquals(ImmersiveBars.All, settings.hiddenBars(fullPlayerVisible = true))
        assertFalse(LevyraImmersiveSettings().hiddenBars(fullPlayerVisible = true).any)
    }

    @Test
    fun automaticKeepsTheEntryPointMode() {
        val video = track("OMVvideo001", videoType = "MUSIC_VIDEO_TYPE_OMV")
        assertTrue(MusicVideoPlaybackPreference.Automatic.startsInVideo(video, requestedVideoMode = true, lastVideoMode = false))
        assertFalse(MusicVideoPlaybackPreference.Automatic.startsInVideo(video, requestedVideoMode = false, lastVideoMode = true))
    }

    @Test
    fun preferenceAppliesOnlyToMusicVideos() {
        val video = track("OMVvideo001", videoType = "MUSIC_VIDEO_TYPE_OMV")
        val song = track("ATVsong0001", videoType = "MUSIC_VIDEO_TYPE_ATV")
        assertFalse(MusicVideoPlaybackPreference.PreferAudio.startsInVideo(video, requestedVideoMode = true, lastVideoMode = true))
        assertTrue(MusicVideoPlaybackPreference.PreferVideo.startsInVideo(video, requestedVideoMode = false, lastVideoMode = false))
        assertFalse(MusicVideoPlaybackPreference.PreferVideo.startsInVideo(song, requestedVideoMode = false, lastVideoMode = true))
        assertTrue(MusicVideoPlaybackPreference.RememberLast.startsInVideo(video, requestedVideoMode = false, lastVideoMode = true))
        assertFalse(MusicVideoPlaybackPreference.RememberLast.startsInVideo(video, requestedVideoMode = true, lastVideoMode = false))
    }

    @Test
    fun localTracksNeverSwitchMode() {
        val local = track("local:42", videoType = "MUSIC_VIDEO_TYPE_OMV", source = "Offline")
        assertFalse(MusicVideoPlaybackPreference.PreferVideo.startsInVideo(local, requestedVideoMode = false, lastVideoMode = true))
    }

    @Test
    fun pairedArtTrackComesOnlyFromAVerifiedCounterpart() {
        val video = track("OMVvideo001", videoType = "MUSIC_VIDEO_TYPE_OMV", counterpartVideoId = "ATVsong0001")
        val paired = video.withPairedArtTrackAudio()
        assertEquals("ATVsong0001", paired.audioVideoId)
        assertEquals("OMVvideo001", paired.counterpartVideoId)
        assertEquals(video.id, paired.id)

        val unpaired = track("OMVvideo001", videoType = "MUSIC_VIDEO_TYPE_OMV")
        assertSame(unpaired, unpaired.withPairedArtTrackAudio())
        val alreadyPaired = track("OMVvideo001", videoType = "MUSIC_VIDEO_TYPE_OMV", audioVideoId = "ATVsong0002", counterpartVideoId = "ATVsong0001")
        assertSame(alreadyPaired, alreadyPaired.withPairedArtTrackAudio())
        val song = track("ATVsong0001", videoType = "MUSIC_VIDEO_TYPE_ATV", counterpartVideoId = "OMVvideo001")
        assertSame(song, song.withPairedArtTrackAudio())
        val malformed = track("OMVvideo001", videoType = "MUSIC_VIDEO_TYPE_OMV", counterpartVideoId = "bad id")
        assertSame(malformed, malformed.withPairedArtTrackAudio())
    }

    @Test
    fun preferenceStorageRoundTripsAndDefaultsToAutomatic() {
        MusicVideoPlaybackPreference.entries.forEach {
            assertEquals(it, MusicVideoPlaybackPreference.fromStorage(it.storageValue))
        }
        assertEquals(MusicVideoPlaybackPreference.Automatic, MusicVideoPlaybackPreference.fromStorage(null))
        assertEquals(MusicVideoPlaybackPreference.Automatic, MusicVideoPlaybackPreference.fromStorage("unknown"))
    }

    @Test
    fun liquidCapsuleStylesArePairedAndRoundTrip() {
        assertEquals(
            MiniPlayerStyle.LiquidCapsule,
            LevyraYtMusicPairing.miniStyleFor(NavStyle.Original, NavStyle.LiquidCapsule, MiniPlayerStyle.Floating)
        )
        assertEquals(
            NavStyle.LiquidCapsule,
            LevyraYtMusicPairing.navStyleFor(MiniPlayerStyle.Original, MiniPlayerStyle.LiquidCapsule, NavStyle.FloatingDock)
        )
        assertEquals(MiniPlayerStyle.LiquidCapsule, MiniPlayerStyle.from("liquid_capsule"))
        assertEquals(NavStyle.LiquidCapsule, NavStyle.from("liquid_capsule"))
        assertTrue(NavStyle.LiquidCapsule.floating)
        assertTrue(NavStyle.LiquidCapsule.separatesSearch)
        assertFalse(NavStyle.LiquidGlass.separatesSearch)
    }
}
