package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimilarSongsFeedbackTest {

    private fun track(
        id: String,
        artist: String,
        title: String = "Title $id",
        videoType: String = ""
    ): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = "Album",
        durationMs = 200_000L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "youtube",
        moodTags = setOf("music"),
        energy = 50,
        vocal = 50,
        replayScore = 50,
        cacheScore = 50,
        accentStart = 0,
        accentEnd = 0,
        videoType = videoType
    )

    private fun profileWith(vararg entries: Pair<Track, RecommendationFeedbackKind>): ListeningSignalProfile =
        ListeningSignalProfile(
            feedback = RecommendationFeedback.from(
                entries.mapIndexedNotNull { index, (track, kind) ->
                    RecommendationFeedback.entryFor(track, kind, nowMs = 1_000L + index)
                }
            )
        )

    @Test
    fun preferredCandidateMovesUpWithoutBreakingSelection() {
        val seed = track("seed", "Seed Artist", "Seed song")
        val preferred = track("c3", "Gamma")
        val pool = listOf(track("c1", "Alpha"), track("c2", "Beta"), preferred)
        val ordered = ListeningSignalRanker.rank(
            candidates = pool,
            profile = profileWith(preferred to RecommendationFeedbackKind.MORE_LIKE_THIS),
            limit = pool.size,
            contextArtist = seed.artist
        )

        val selected = SimilarSongsSelector.select(
            candidates = ordered,
            seed = seed,
            excludedIdentities = emptySet()
        )

        assertEquals("c3", selected.first().id)
        assertEquals(3, selected.size)
    }

    @Test
    fun queuedTracksStayExcludedAfterFeedbackOrdering() {
        val seed = track("seed", "Seed Artist", "Seed song")
        val queued = track("c1", "Alpha")
        val pool = listOf(queued, track("c2", "Beta"))
        val ordered = ListeningSignalRanker.rank(
            candidates = pool,
            profile = profileWith(queued to RecommendationFeedbackKind.MORE_LIKE_THIS),
            limit = pool.size
        )

        val selected = SimilarSongsSelector.select(
            candidates = ordered,
            seed = seed,
            excludedIdentities = setOf(LevyraPersonalOrbit.identityKey(queued))
        )

        assertEquals(listOf("c2"), selected.map { it.id })
    }

    @Test
    fun duplicateAndReuploadFilteringStillApplies() {
        val seed = track("seed", "Seed Artist", "Seed song")
        val original = track("c1", "Alpha", "Shared title")
        val duplicate = track("c1", "Alpha", "Shared title")
        val reupload = track("c9", "Random Uploader", "Seed song")
        val pool = listOf(original, duplicate, reupload, track("c2", "Beta"))
        val ordered = ListeningSignalRanker.rank(
            candidates = pool,
            profile = profileWith(reupload to RecommendationFeedbackKind.MORE_LIKE_THIS),
            limit = pool.size
        )

        val selected = SimilarSongsSelector.select(
            candidates = ordered,
            seed = seed,
            excludedIdentities = emptySet()
        )

        assertFalse(selected.any { it.id == "c9" })
        assertEquals(1, selected.count { it.id == "c1" })
    }

    @Test
    fun displayLimitStillCapsTheResult() {
        val seed = track("seed", "Seed Artist", "Seed song")
        val pool = (1..(SimilarSongsSelector.DISPLAY_LIMIT + 6)).map { track("c$it", "Artist $it") }
        val ordered = ListeningSignalRanker.rank(
            candidates = pool,
            profile = profileWith(pool.last() to RecommendationFeedbackKind.MORE_LIKE_THIS),
            limit = pool.size
        )

        val selected = SimilarSongsSelector.select(candidates = ordered, seed = seed, excludedIdentities = emptySet())

        assertEquals(SimilarSongsSelector.DISPLAY_LIMIT, selected.size)
    }

    @Test
    fun excludedArtistsAreRemovedBeforeFeedbackOrdering() {
        val seed = track("seed", "Seed Artist", "Seed song")
        val excludedTrack = track("c1", "Alpha")
        val exclusions = ArtistExclusions.from(listOf(ExcludedArtist("", "Alpha", 1L)))
        val pool = exclusions.filterTracks(listOf(excludedTrack, track("c2", "Beta")))
        val ordered = ListeningSignalRanker.rank(
            candidates = pool,
            profile = profileWith(excludedTrack to RecommendationFeedbackKind.MORE_LIKE_THIS),
            limit = pool.size
        )

        val selected = SimilarSongsSelector.select(candidates = ordered, seed = seed, excludedIdentities = emptySet())

        assertTrue(selected.none { it.artist == "Alpha" })
        assertEquals(listOf("c2"), selected.map { it.id })
    }

    @Test
    fun audioIsStillPreferredOverTheVideoCounterpart() {
        val seed = track("seed", "Seed Artist", "Seed song")
        val videoVersion = track("c1", "Alpha", "Same song", videoType = "MUSIC_VIDEO_TYPE_OMV")
        val audioVersion = track("c1", "Alpha", "Same song")

        val selected = SimilarSongsSelector.select(
            candidates = listOf(videoVersion, audioVersion),
            seed = seed,
            excludedIdentities = emptySet()
        )

        assertEquals(1, selected.size)
        assertTrue(selected.first().videoType.isBlank())
    }
}
