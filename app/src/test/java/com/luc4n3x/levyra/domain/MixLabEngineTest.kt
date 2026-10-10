package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MixLabEngineTest {

    private fun track(
        id: String,
        artist: String = "Artist $id",
        durationMs: Long = 180_000L,
        moodTags: Set<String> = emptySet(),
        isrc: String = "",
        releaseDate: String = "",
        album: String = ""
    ) = Track(
        id = id,
        title = "Title $id",
        artist = artist,
        album = album,
        durationMs = durationMs,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = moodTags,
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0,
        isrc = isrc,
        releaseDate = releaseDate
    )

    private fun candidate(
        id: String,
        artist: String = "Artist $id",
        album: String = "",
        durationMs: Long = 180_000L,
        moodTags: Set<String> = emptySet(),
        isrc: String = "",
        releaseDate: String = "",
        playCount: Int = 0,
        lastPlayedAt: Long = 0L,
        isFavorite: Boolean = false,
        isFollowedArtist: Boolean = false,
        isSimilarSeed: Boolean = false
    ) = MixLabCandidate(
        track = track(id, artist = artist, album = album, durationMs = durationMs, moodTags = moodTags, isrc = isrc, releaseDate = releaseDate),
        playCount = playCount,
        lastPlayedAt = lastPlayedAt,
        isFavorite = isFavorite,
        isFollowedArtist = isFollowedArtist,
        isSimilarSeed = isSimilarSeed
    )

    @Test
    fun familiarityBiasPrefersFavoritesWhenTunedToFamiliar() {
        val favorite = candidate("fav", playCount = 10, isFavorite = true)
        val unknown = candidate("unk", playCount = 0)
        val result = MixLabEngine.build(
            pool = listOf(favorite, unknown),
            params = MixLabParams(familiarity = 1f, trackCount = 1)
        )
        assertEquals("fav", result.tracks.single().id)
    }

    @Test
    fun discoveryBiasPrefersUnknownTracksWhenTunedToDiscovery() {
        val favorite = candidate("fav", playCount = 10, isFavorite = true)
        val unknown = candidate("unk", playCount = 0)
        val result = MixLabEngine.build(
            pool = listOf(favorite, unknown),
            params = MixLabParams(familiarity = 0f, trackCount = 1)
        )
        assertEquals("unk", result.tracks.single().id)
    }

    @Test
    fun artistCapLimitsOverrepresentation() {
        val pool = (1..10).map { candidate("same-artist-$it", artist = "Same Artist", playCount = it) }
        val result = MixLabEngine.build(pool = pool, params = MixLabParams(trackCount = 10))
        val cap = MixLabDefaults.artistCap(10)
        assertTrue(result.tracks.size <= cap)
    }

    @Test
    fun albumCapLimitsOverrepresentation() {
        val pool = (1..10).map { candidate("same-album-$it", artist = "Artist $it", album = "Same Album", playCount = it) }
        val result = MixLabEngine.build(pool = pool, params = MixLabParams(trackCount = 10))
        val cap = MixLabDefaults.albumCap(10)
        val inAlbum = result.tracks.count { it.album == "Same Album" }
        assertTrue(inAlbum <= cap)
    }

    @Test
    fun artistSpacingAvoidsBackToBackRepeatsWhenAlternativesExist() {
        val pool = listOf(
            candidate("a1", artist = "Artist A", playCount = 5),
            candidate("a2", artist = "Artist A", playCount = 4),
            candidate("b1", artist = "Artist B", playCount = 3),
            candidate("c1", artist = "Artist C", playCount = 2)
        )
        val result = MixLabEngine.build(pool = pool, params = MixLabParams(trackCount = 4))
        val artists = result.tracks.map { it.artist }
        for (index in 0 until artists.size - 1) {
            assertTrue(artists[index] != artists[index + 1])
        }
    }

    @Test
    fun albumSpacingAvoidsBackToBackRepeatsWhenAlternativesExist() {
        val pool = listOf(
            candidate("a1", artist = "Artist 1", album = "Album A", playCount = 5),
            candidate("a2", artist = "Artist 2", album = "Album A", playCount = 4),
            candidate("b1", artist = "Artist 3", album = "Album B", playCount = 3),
            candidate("c1", artist = "Artist 4", album = "Album C", playCount = 2)
        )
        val result = MixLabEngine.build(pool = pool, params = MixLabParams(familiarity = 1f, trackCount = 4))
        val albums = result.tracks.map { it.album }
        for (index in 0 until albums.size - 1) {
            assertTrue(albums[index] != albums[index + 1])
        }
    }

    @Test
    fun dedupPrefersIsrcOverTitleArtistFallback() {
        val original = candidate("v1", artist = "Artist X", isrc = "US1234567890", playCount = 3)
        val duplicateIsrc = candidate("v2", artist = "Artist X", isrc = "US1234567890", playCount = 7)
        val result = MixLabEngine.build(pool = listOf(original, duplicateIsrc), params = MixLabParams(trackCount = 10))
        assertEquals(1, result.tracks.size)
        assertEquals(1, result.candidatePoolSize)
    }

    @Test
    fun dedupFallsBackToTitleArtistDurationWhenIsrcMissing() {
        val original = MixLabCandidate(
            track = track("v1", artist = "Artist Y", durationMs = 200_000L).copy(title = "Same Song"),
            playCount = 1
        )
        val nearDuplicate = MixLabCandidate(
            track = track("v2", artist = "Artist Y", durationMs = 201_000L).copy(title = "Same Song"),
            playCount = 5
        )
        val result = MixLabEngine.build(pool = listOf(original, nearDuplicate), params = MixLabParams(trackCount = 10))
        assertEquals(1, result.tracks.size)
    }

    @Test
    fun recentlyPlayedPenaltySuppressesRepeatsWhenSeekingDiscovery() {
        val now = 1_000_000_000L
        val recentlyPlayed = candidate("recent", playCount = 5, lastPlayedAt = now - 60_000L)
        val untouched = candidate("fresh", playCount = 5, lastPlayedAt = 0L)
        val result = MixLabEngine.build(
            pool = listOf(recentlyPlayed, untouched),
            params = MixLabParams(familiarity = 0f, trackCount = 1),
            canonicalSources = emptyList(),
            nowMs = now
        )
        assertEquals("fresh", result.tracks.single().id)
    }

    @Test
    fun missingMoodMetadataIsNeverPenalized() {
        val tagged = candidate("tagged", moodTags = setOf("chill"), playCount = 5)
        val untagged = candidate("untagged", moodTags = emptySet(), playCount = 5)
        val result = MixLabEngine.build(
            pool = listOf(tagged, untagged),
            params = MixLabParams(moodTags = setOf("chill"), trackCount = 2)
        )
        assertEquals(2, result.tracks.size)
    }

    @Test
    fun moodMismatchWithRealTagsScoresBelowMoodMatch() {
        val matching = candidate("match", moodTags = setOf("chill"), playCount = 5)
        val mismatched = candidate("mismatch", moodTags = setOf("gym"), playCount = 5)
        val result = MixLabEngine.build(
            pool = listOf(matching, mismatched),
            params = MixLabParams(moodTags = setOf("chill"), trackCount = 1)
        )
        assertEquals("match", result.tracks.single().id)
    }

    @Test
    fun genreFilterExcludesNonMatchingCandidates() {
        val pop = candidate("pop", moodTags = setOf("pop"))
        val rock = candidate("rock", moodTags = setOf("rock"))
        val result = MixLabEngine.build(
            pool = listOf(pop, rock),
            params = MixLabParams(genres = setOf("pop"), trackCount = 10)
        )
        assertEquals(listOf("pop"), result.tracks.map { it.id })
    }

    @Test
    fun deterministicBehaviorWithFixedSeed() {
        val pool = (1..30).map { candidate("t$it", artist = "Artist ${it % 5}", playCount = it) }
        val params = MixLabParams(trackCount = 10, seed = 42L)
        val first = MixLabEngine.build(pool = pool, params = params).tracks.map { it.id }
        val second = MixLabEngine.build(pool = pool, params = params).tracks.map { it.id }
        assertEquals(first, second)
    }

    @Test
    fun regenerateWithDifferentSeedVariesResultWithoutBreakingIntent() {
        val pool = (1..40).map { candidate("t$it", artist = "Artist ${it % 8}", playCount = 40 - it) }
        val params = MixLabParams(trackCount = 15, seed = 1L)
        val first = MixLabEngine.build(pool = pool, params = params).tracks.map { it.id }
        val regenerated = MixLabEngine.build(pool = pool, params = params.copy(seed = 2L)).tracks.map { it.id }
        assertTrue(first != regenerated)
        assertTrue(first.toSet().intersect(regenerated.toSet()).isNotEmpty())
    }

    @Test
    fun candidateBudgetIsBounded() {
        val hugePool = (1..5000).map { candidate("t$it", artist = "Artist $it", playCount = it) }
        val result = MixLabEngine.build(pool = hugePool, params = MixLabParams(trackCount = 20))
        assertTrue(result.candidatePoolSize <= MixLabDefaults.MaxCandidates)
    }

    @Test
    fun emptyPoolProducesEmptyResultInsteadOfCrashing() {
        val result = MixLabEngine.build(pool = emptyList(), params = MixLabParams(trackCount = 20))
        assertEquals(emptyList<Any>(), result.tracks)
        assertEquals(0, result.candidatePoolSize)
    }

    @Test
    fun exclusionsRemoveExcludedArtistsEntirely() {
        val excludedArtist = candidate("excluded", artist = "Banned Artist")
        val allowed = candidate("allowed", artist = "Allowed Artist")
        val exclusions = ArtistExclusions.from(listOf(ExcludedArtist(browseId = "", name = "Banned Artist", excludedAt = 0L)))
        val result = MixLabEngine.build(
            pool = listOf(excludedArtist, allowed),
            params = MixLabParams(trackCount = 10),
            exclusions = exclusions
        )
        assertEquals(listOf("allowed"), result.tracks.map { it.id })
    }

    @Test
    fun durationBucketPrefersExactMatchOverAdjacent() {
        val short = candidate("short", durationMs = 120_000L)
        val medium = candidate("medium", durationMs = 240_000L)
        val long = candidate("long", durationMs = 400_000L)
        val result = MixLabEngine.build(
            pool = listOf(short, medium, long),
            params = MixLabParams(duration = MixLabDuration.Short, trackCount = 1)
        )
        assertEquals("short", result.tracks.single().id)
    }

    @Test
    fun recencyBiasPrefersNewerReleasesWhenTunedToNew() {
        val newer = candidate("newer", releaseDate = "2026-01-01", playCount = 1)
        val older = candidate("older", releaseDate = "1995-01-01", playCount = 1)
        val result = MixLabEngine.build(
            pool = listOf(older, newer),
            params = MixLabParams(recency = 1f, familiarity = 0.5f, trackCount = 1)
        )
        assertEquals("newer", result.tracks.single().id)
    }

    @Test
    fun recencyBiasPrefersOlderReleasesWhenTunedToClassics() {
        val newer = candidate("newer", releaseDate = "2026-01-01", playCount = 1)
        val older = candidate("older", releaseDate = "1995-01-01", playCount = 1)
        val result = MixLabEngine.build(
            pool = listOf(older, newer),
            params = MixLabParams(recency = 0f, familiarity = 0.5f, trackCount = 1)
        )
        assertEquals("older", result.tracks.single().id)
    }

    @Test
    fun artistSelectionKeepsOnlyChosenArtists() {
        val chosen = candidate("chosen", artist = "Artist A")
        val other = candidate("other", artist = "Artist B")
        val result = MixLabEngine.build(
            pool = listOf(chosen, other),
            params = MixLabParams(artistKeys = setOf("artist a"), trackCount = 10)
        )
        assertEquals(listOf("chosen"), result.tracks.map { it.id })
    }

    @Test
    fun partialMetadataAcrossPoolStillProducesRankedResult() {
        val richTrack = candidate("rich", moodTags = setOf("chill"), releaseDate = "2024-01-01", playCount = 3)
        val sparseTrack = candidate("sparse", playCount = 1)
        val result = MixLabEngine.build(
            pool = listOf(richTrack, sparseTrack),
            params = MixLabParams(moodTags = setOf("chill"), recency = 1f, trackCount = 2)
        )
        assertEquals(2, result.tracks.size)
    }
}
