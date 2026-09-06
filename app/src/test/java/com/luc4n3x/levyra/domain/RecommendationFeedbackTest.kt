package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationFeedbackTest {

    private fun track(id: String, artist: String, title: String = "Title $id"): Track = Track(
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
        accentEnd = 0
    )

    private fun feedbackOf(vararg entries: Pair<Track, RecommendationFeedbackKind>): RecommendationFeedback =
        RecommendationFeedback.from(
            entries.mapIndexedNotNull { index, (track, kind) ->
                RecommendationFeedback.entryFor(track, kind, nowMs = 1_000L + index)
            }
        )

    @Test
    fun moreLikeThisRaisesTheCandidateAboveNeutralOnes() {
        val preferred = track("a1", "Alpha")
        val neutral = track("b1", "Beta")
        val profile = ListeningSignalProfile(
            feedback = feedbackOf(preferred to RecommendationFeedbackKind.MORE_LIKE_THIS)
        )

        val ranked = ListeningSignalRanker.rank(listOf(neutral, preferred), profile)

        assertEquals(listOf("a1", "b1"), ranked.map { it.id })
        assertTrue(profile.trackScore(preferred) > profile.trackScore(neutral))
    }

    @Test
    fun lessLikeThisPushesTheCandidateBelowNeutralOnes() {
        val avoided = track("a1", "Alpha")
        val neutral = track("b1", "Beta")
        val profile = ListeningSignalProfile(
            feedback = feedbackOf(avoided to RecommendationFeedbackKind.LESS_LIKE_THIS)
        )

        val ranked = ListeningSignalRanker.rank(listOf(avoided, neutral), profile)

        assertEquals(listOf("b1", "a1"), ranked.map { it.id })
        assertTrue(profile.trackScore(avoided) < profile.trackScore(neutral))
    }

    @Test
    fun feedbackDoesNotRemoveCandidatesFromTheResult() {
        val avoided = track("a1", "Alpha")
        val neutral = track("b1", "Beta")
        val profile = ListeningSignalProfile(
            feedback = feedbackOf(avoided to RecommendationFeedbackKind.LESS_LIKE_THIS)
        )

        val ranked = ListeningSignalRanker.rank(listOf(avoided, neutral), profile)

        assertEquals(2, ranked.size)
    }

    @Test
    fun oneNegativeVoteDoesNotWipeTheWholeArtist() {
        val avoided = track("a1", "Alpha")
        val sibling = track("a2", "Alpha")
        val neutral = track("b1", "Beta")
        val profile = ListeningSignalProfile(
            feedback = feedbackOf(avoided to RecommendationFeedbackKind.LESS_LIKE_THIS)
        )

        val ranked = ListeningSignalRanker.rank(listOf(avoided, sibling, neutral), profile)

        assertEquals(3, ranked.size)
        assertTrue(ranked.map { it.id }.indexOf("a2") < ranked.map { it.id }.indexOf("a1"))
    }

    @Test
    fun artistAffinityStaysBounded() {
        val entries = (1..12).mapNotNull { index ->
            RecommendationFeedback.entryFor(
                track("t$index", "Alpha"),
                RecommendationFeedbackKind.MORE_LIKE_THIS,
                nowMs = 1_000L + index
            )
        }

        val feedback = RecommendationFeedback.from(entries)

        assertEquals(
            RecommendationFeedbackWeights.Default.artistAffinityCap,
            feedback.artistAffinity.getValue("alpha")
        )
    }

    @Test
    fun artistAffinityClampsAfterNettingAllVotes() {
        val entries = buildList {
            repeat(5) { index ->
                RecommendationFeedback.entryFor(
                    track("liked-$index", "Alpha"),
                    RecommendationFeedbackKind.MORE_LIKE_THIS,
                    nowMs = 1_000L + index
                )?.let(::add)
            }
            repeat(3) { index ->
                RecommendationFeedback.entryFor(
                    track("disliked-$index", "Alpha"),
                    RecommendationFeedbackKind.LESS_LIKE_THIS,
                    nowMs = 2_000L + index
                )?.let(::add)
            }
        }

        val feedback = RecommendationFeedback.from(entries)

        assertEquals(2, feedback.artistAffinity.getValue("alpha"))
    }

    @Test
    fun storedEntriesStayBounded() {
        val entries = (1..(RecommendationFeedback.MAX_ENTRIES * 2)).mapNotNull { index ->
            RecommendationFeedback.entryFor(
                track("t$index", "Artist $index"),
                RecommendationFeedbackKind.MORE_LIKE_THIS,
                nowMs = 1_000L + index
            )
        }

        val feedback = RecommendationFeedback.from(entries)

        assertEquals(RecommendationFeedback.MAX_ENTRIES, feedback.preferredTrackKeys.size)
        assertTrue(feedback.artistAffinity.size <= RecommendationFeedback.MAX_ENTRIES)
    }

    @Test
    fun feedbackKindIsReportedPerTrack() {
        val liked = track("a1", "Alpha")
        val disliked = track("b1", "Beta")
        val unknown = track("c1", "Gamma")
        val feedback = feedbackOf(
            liked to RecommendationFeedbackKind.MORE_LIKE_THIS,
            disliked to RecommendationFeedbackKind.LESS_LIKE_THIS
        )

        assertEquals(RecommendationFeedbackKind.MORE_LIKE_THIS, feedback.kindFor(liked))
        assertEquals(RecommendationFeedbackKind.LESS_LIKE_THIS, feedback.kindFor(disliked))
        assertNull(feedback.kindFor(unknown))
    }

    @Test
    fun tracksWithoutIdentityAreIgnored() {
        val anonymous = track(id = "", artist = "", title = "")

        assertNull(RecommendationFeedback.entryFor(anonymous, RecommendationFeedbackKind.MORE_LIKE_THIS))
    }

    @Test
    fun emptyFeedbackLeavesRankingUnchanged() {
        val candidates = listOf(track("a1", "Alpha"), track("b1", "Beta"), track("c1", "Gamma"))
        val profile = ListeningSignalProfile()

        assertFalse(profile.hasSignal)
        assertEquals(candidates.map { it.id }, ListeningSignalRanker.rank(candidates, profile).map { it.id })
    }

    @Test
    fun artistDiversityStillCapsConsecutiveRuns() {
        val candidates = listOf(
            track("a1", "Alpha"),
            track("a2", "Alpha"),
            track("a3", "Alpha"),
            track("b1", "Beta")
        )
        val profile = ListeningSignalProfile(
            feedback = feedbackOf(
                candidates[0] to RecommendationFeedbackKind.MORE_LIKE_THIS,
                candidates[1] to RecommendationFeedbackKind.MORE_LIKE_THIS,
                candidates[2] to RecommendationFeedbackKind.MORE_LIKE_THIS
            )
        )

        val ranked = ListeningSignalRanker.rank(candidates, profile, artistRunLimit = 2)

        assertEquals("b1", ranked[2].id)
    }

    @Test
    fun preferredTracksAreNeverSuppressed() {
        val preferred = track("a1", "Alpha")
        val key = ListenIdentity.trackKey(preferred.id, preferred.title, preferred.artist)
        val profile = ListeningSignalProfile(
            tracks = mapOf(
                key to TrackListeningSignal(
                    key = key,
                    plays = 6,
                    countedPlays = 0,
                    completionRatio = 0.05,
                    skips = 6,
                    earlySkips = 6,
                    lastPlayedAt = 0L
                )
            ),
            feedback = feedbackOf(preferred to RecommendationFeedbackKind.MORE_LIKE_THIS)
        )

        assertFalse(profile.isSuppressed(preferred))
    }

    @Test
    fun excludedArtistsStillWinOverPositiveFeedback() {
        val liked = track("a1", "Alpha")
        val exclusions = ArtistExclusions.from(listOf(ExcludedArtist("", "Alpha", 1L)))

        assertTrue(exclusions.excludesTrack(liked))
        assertTrue(exclusions.filterTracks(listOf(liked)).isEmpty())
    }
}
