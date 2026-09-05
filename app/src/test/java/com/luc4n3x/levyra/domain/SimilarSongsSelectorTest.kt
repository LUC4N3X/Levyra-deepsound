package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimilarSongsSelectorTest {

    @Test
    fun dropsTheSeedRecordingEvenWhenTheVideoIdDiffers() {
        val seed = track("aaaaaaaaaaa", "Blinding Lights", "The Weeknd")
        val candidates = listOf(
            track("bbbbbbbbbbb", "Blinding Lights", "The Weeknd", videoType = "MUSIC_VIDEO_TYPE_OMV"),
            track("ccccccccccc", "Save Your Tears", "The Weeknd")
        )

        val result = SimilarSongsSelector.select(candidates, seed, emptySet())

        assertEquals(listOf("ccccccccccc"), result.map { it.id })
    }

    @Test
    fun dropsRecordingsAlreadyPresentInTheQueue() {
        val seed = track("aaaaaaaaaaa", "Blinding Lights", "The Weeknd")
        val queued = track("ddddddddddd", "Starboy", "The Weeknd")
        val candidates = listOf(
            track("eeeeeeeeeee", "Starboy", "The Weeknd"),
            track("fffffffffff", "Take On Me", "a-ha")
        )

        val result = SimilarSongsSelector.select(
            candidates = candidates,
            seed = seed,
            excludedIdentities = setOf(LevyraPersonalOrbit.identityKey(queued))
        )

        assertEquals(listOf("fffffffffff"), result.map { it.id })
    }

    @Test
    fun dropsReuploadsAndCoversOfTheCurrentRecording() {
        val seed = track("aaaaaaaaaaa", "2 Hard 4 The Radio", "Drake")
        val candidates = listOf(
            track("ttttttttttt", "Drake 2 Hard 4 The Radio", "Deshawn Cavanaugh"),
            track("uuuuuuuuuuu", "2 Hard 4 The Radio (cover)", "Some Channel"),
            track("vvvvvvvvvvv", "2 Hard 4 the Fuckin Radio", "Mac Dre")
        )

        val result = SimilarSongsSelector.select(candidates, seed, emptySet())

        assertEquals(listOf("vvvvvvvvvvv"), result.map { it.id })
    }

    @Test
    fun keepsARemixCreditedToTheSameArtist() {
        val seed = track("aaaaaaaaaaa", "Blinding Lights", "The Weeknd")
        val candidates = listOf(track("wwwwwwwwwww", "Blinding Lights Remix", "The Weeknd"))

        val result = SimilarSongsSelector.select(candidates, seed, emptySet())

        assertEquals(listOf("wwwwwwwwwww"), result.map { it.id })
    }

    @Test
    fun doesNotTreatAShortSeedTitleAsASubstringMatch() {
        val seed = track("aaaaaaaaaaa", "Go", "Artist One")
        val candidates = listOf(track("xxxxxxxxxxx", "Gold", "Artist Two"))

        val result = SimilarSongsSelector.select(candidates, seed, emptySet())

        assertEquals(listOf("xxxxxxxxxxx"), result.map { it.id })
    }

    @Test
    fun collapsesRepeatedRecommendationsToASingleEntry() {
        val candidates = listOf(
            track("ggggggggggg", "After Hours", "The Weeknd"),
            track("hhhhhhhhhhh", "After Hours", "The Weeknd"),
            track("iiiiiiiiiii", "After Hours", "The Weeknd")
        )

        val result = SimilarSongsSelector.select(candidates, seed = null, excludedIdentities = emptySet())

        assertEquals(1, result.size)
    }

    @Test
    fun prefersTheAudioTrackOverTheMusicVideoDuplicate() {
        val candidates = listOf(
            track("jjjjjjjjjjj", "Take On Me", "a-ha", videoType = "MUSIC_VIDEO_TYPE_OMV"),
            track("kkkkkkkkkkk", "Take On Me", "a-ha", videoType = "MUSIC_VIDEO_TYPE_ATV")
        )

        val result = SimilarSongsSelector.select(candidates, seed = null, excludedIdentities = emptySet())

        assertEquals(listOf("kkkkkkkkkkk"), result.map { it.id })
    }

    @Test
    fun keepsTheMusicVideoWhenNoAudioVariantIsAvailable() {
        val candidates = listOf(track("lllllllllll", "Take On Me", "a-ha", videoType = "MUSIC_VIDEO_TYPE_OMV"))

        val result = SimilarSongsSelector.select(candidates, seed = null, excludedIdentities = emptySet())

        assertEquals(listOf("lllllllllll"), result.map { it.id })
    }

    @Test
    fun skipsCandidatesWithoutAPlayableIdentifierOrTitle() {
        val candidates = listOf(
            track("", "Ghost Entry", "Nobody"),
            track("mmmmmmmmmmm", "", "Nobody"),
            track("nnnnnnnnnnn", "Real Song", "Somebody")
        )

        val result = SimilarSongsSelector.select(candidates, seed = null, excludedIdentities = emptySet())

        assertEquals(listOf("nnnnnnnnnnn"), result.map { it.id })
    }

    @Test
    fun honoursTheRequestedLimitWhileStillUpgradingSelectedEntries() {
        val candidates = listOf(
            track("ooooooooooo", "First", "Artist", videoType = "MUSIC_VIDEO_TYPE_OMV"),
            track("ppppppppppp", "Second", "Artist"),
            track("qqqqqqqqqqq", "Third", "Artist"),
            track("rrrrrrrrrrr", "First", "Artist", videoType = "MUSIC_VIDEO_TYPE_ATV")
        )

        val result = SimilarSongsSelector.select(candidates, seed = null, excludedIdentities = emptySet(), limit = 2)

        assertEquals(2, result.size)
        assertEquals(listOf("rrrrrrrrrrr", "ppppppppppp"), result.map { it.id })
    }

    @Test
    fun returnsNothingForAnEmptyOrDisabledRequest() {
        val candidates = listOf(track("sssssssssss", "Song", "Artist"))

        assertTrue(SimilarSongsSelector.select(emptyList(), null, emptySet()).isEmpty())
        assertTrue(SimilarSongsSelector.select(candidates, null, emptySet(), limit = 0).isEmpty())
    }

    private fun track(
        id: String,
        title: String,
        artist: String,
        videoType: String = "",
        durationMs: Long = 180_000L
    ): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = title,
        durationMs = durationMs,
        streamUrl = "",
        videoUrl = if (id.isBlank()) "" else "https://www.youtube.com/watch?v=$id",
        thumbnailUrl = "https://example.test/$id.jpg",
        largeThumbnailUrl = "https://example.test/$id-large.jpg",
        source = "YouTube Music Radio",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 70,
        cacheScore = 70,
        accentStart = 0,
        accentEnd = 0,
        videoType = videoType
    )
}
