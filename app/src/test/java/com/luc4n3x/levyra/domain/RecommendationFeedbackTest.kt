package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationFeedbackTest {

    private fun track(
        id: String,
        artist: String,
        title: String = "Title $id",
        album: String = "Album",
        albumBrowseId: String = "",
        moodTags: Set<String> = setOf("music")
    ): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = 200_000L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "youtube",
        moodTags = moodTags,
        energy = 50,
        vocal = 50,
        replayScore = 50,
        cacheScore = 50,
        accentStart = 0,
        accentEnd = 0,
        albumBrowseId = albumBrowseId
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
    fun lessLikeThisHardExcludesTheCandidate() {
        val avoided = track("a1", "Alpha")
        val neutral = track("b1", "Beta")
        val profile = ListeningSignalProfile(
            feedback = feedbackOf(avoided to RecommendationFeedbackKind.LESS_LIKE_THIS)
        )

        val ranked = ListeningSignalRanker.rank(listOf(avoided, neutral), profile)

        assertEquals(listOf("b1"), ranked.map { it.id })
        assertTrue(profile.feedback.isExplicitlyAvoided(avoided))
        assertTrue(profile.isSuppressed(avoided))
    }

    @Test
    fun hardExclusionStillWinsInReorderOnlyMode() {
        val avoided = track("a1", "Alpha")
        val neutral = track("b1", "Beta")
        val profile = ListeningSignalProfile(
            feedback = feedbackOf(avoided to RecommendationFeedbackKind.LESS_LIKE_THIS)
        )

        val ranked = ListeningSignalRanker.rank(
            candidates = listOf(avoided, neutral),
            profile = profile,
            limit = 2,
            dropSuppressed = false
        )

        assertEquals(listOf("b1"), ranked.map { it.id })
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

        assertFalse(ranked.any { it.id == "a1" })
        assertTrue(ranked.any { it.id == "a2" })
        assertEquals(2, ranked.size)
    }

    @Test
    fun smartOrbitDiversifiesAnEmptyProfileInReorderOnlyMode() {
        val candidates = listOf(
            track("a1", "Alpha"),
            track("a2", "Alpha"),
            track("a3", "Alpha"),
            track("b1", "Beta"),
            track("c1", "Charlie")
        )

        val ranked = ListeningSignalRanker.rank(
            candidates = candidates,
            profile = ListeningSignalProfile(),
            limit = 4,
            dropSuppressed = false
        )

        assertEquals(4, ranked.size)
        assertEquals(2, ranked.count { it.artist == "Alpha" })
        assertTrue(ranked.any { it.id == "b1" })
        assertTrue(ranked.any { it.id == "c1" })
    }

    @Test
    fun smartOrbitCapsAnArtistGloballyWhenAlternativesExist() {
        val a1 = track("a1", "Alpha")
        val a2 = track("a2", "Alpha")
        val a3 = track("a3", "Alpha")
        val b1 = track("b1", "Beta")
        val c1 = track("c1", "Charlie")
        val profile = ListeningSignalProfile(
            feedback = feedbackOf(
                a1 to RecommendationFeedbackKind.MORE_LIKE_THIS,
                a2 to RecommendationFeedbackKind.MORE_LIKE_THIS,
                a3 to RecommendationFeedbackKind.MORE_LIKE_THIS
            )
        )

        val ranked = ListeningSignalRanker.rank(
            candidates = listOf(a1, a2, a3, b1, c1),
            profile = profile,
            limit = 4,
            dropSuppressed = false
        )

        assertEquals(4, ranked.size)
        assertEquals(2, ranked.count { it.artist == "Alpha" })
        assertTrue(ranked.any { it.id == "b1" })
        assertTrue(ranked.any { it.id == "c1" })
    }

    @Test
    fun smartOrbitAlbumCapIsIndependentFromArtistCap() {
        val albumA1 = track("a1", "Alpha", album = "Album A", albumBrowseId = "ALBUM_A")
        val albumA2 = track("a2", "Alpha", album = "Album A", albumBrowseId = "ALBUM_A")
        val albumB = track("a3", "Alpha", album = "Album B", albumBrowseId = "ALBUM_B")
        val other = track("b1", "Beta", album = "Album C", albumBrowseId = "ALBUM_C")
        val profile = ListeningSignalProfile(
            feedback = feedbackOf(
                albumA1 to RecommendationFeedbackKind.MORE_LIKE_THIS,
                albumA2 to RecommendationFeedbackKind.MORE_LIKE_THIS,
                albumB to RecommendationFeedbackKind.MORE_LIKE_THIS
            )
        )

        val ranked = ListeningSignalRanker.rank(
            candidates = listOf(albumA1, albumA2, albumB, other),
            profile = profile,
            limit = 3,
            dropSuppressed = false
        )

        assertEquals(listOf("a1", "a3", "b1"), ranked.map { it.id })
    }

    @Test
    fun smartOrbitAlbumBrowseIdCapsCompilationAcrossTrackArtists() {
        val compilationA = track("a1", "Alpha", album = "Compilation", albumBrowseId = "COMP")
        val compilationB = track("b1", "Beta", album = "Compilation", albumBrowseId = "COMP")
        val other = track("c1", "Charlie", album = "Other", albumBrowseId = "OTHER")
        val profile = ListeningSignalProfile(
            feedback = feedbackOf(compilationA to RecommendationFeedbackKind.MORE_LIKE_THIS)
        )

        val ranked = ListeningSignalRanker.rank(
            candidates = listOf(compilationA, compilationB, other),
            profile = profile,
            limit = 2,
            dropSuppressed = false
        )

        assertEquals(listOf("a1", "c1"), ranked.map { it.id })
    }

    @Test
    fun smartOrbitMoodDiversityDoesNotDependOnSetIterationOrder() {
        val orderedRockFirst = track("m1", "Alpha", moodTags = linkedSetOf("rock", "pop"))
        val orderedPopFirst = orderedRockFirst.copy(moodTags = linkedSetOf("pop", "rock"))
        val rest = listOf(
            track("r2", "Beta", moodTags = setOf("rock")),
            track("r3", "Charlie", moodTags = setOf("rock")),
            track("j1", "Delta", moodTags = setOf("jazz")),
            track("p1", "Echo", moodTags = setOf("pop"))
        )
        val profileA = ListeningSignalProfile(
            feedback = feedbackOf(orderedRockFirst to RecommendationFeedbackKind.MORE_LIKE_THIS)
        )
        val profileB = ListeningSignalProfile(
            feedback = feedbackOf(orderedPopFirst to RecommendationFeedbackKind.MORE_LIKE_THIS)
        )

        val rankedA = ListeningSignalRanker.rank(
            candidates = listOf(orderedRockFirst) + rest,
            profile = profileA,
            limit = 4,
            dropSuppressed = false
        )
        val rankedB = ListeningSignalRanker.rank(
            candidates = listOf(orderedPopFirst) + rest,
            profile = profileB,
            limit = 4,
            dropSuppressed = false
        )

        assertEquals(rankedA.map { it.id }, rankedB.map { it.id })
        assertTrue(rankedA.any { it.id == "j1" })
        assertTrue(rankedA.any { it.id == "p1" })
    }

    @Test
    fun smartOrbitRelaxesDiversityToRefillButNeverRestoresAvoidedTracks() {
        val avoided = track("a0", "Alpha")
        val candidates = listOf(
            avoided,
            track("a1", "Alpha"),
            track("a2", "Alpha"),
            track("a3", "Alpha"),
            track("a4", "Alpha")
        )
        val profile = ListeningSignalProfile(
            feedback = feedbackOf(avoided to RecommendationFeedbackKind.LESS_LIKE_THIS)
        )

        val ranked = ListeningSignalRanker.rank(
            candidates = candidates,
            profile = profile,
            limit = 5,
            dropSuppressed = false
        )

        assertEquals(4, ranked.size)
        assertFalse(ranked.any { it.id == "a0" })
        assertEquals(setOf("a1", "a2", "a3", "a4"), ranked.map { it.id }.toSet())
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
