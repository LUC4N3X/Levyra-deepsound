package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackFallbackMetadataTest {
    @Test
    fun recognizesOnlyKnownMisattributedArtistTitleSignature() {
        assertTrue(isKnownMisattributedPlaybackMetadata("neptune.", "SOTTOGONNA"))
        assertTrue(isKnownMisattributedPlaybackMetadata("NEPTUNE", "sottogonna!"))
        assertFalse(isKnownMisattributedPlaybackMetadata("NEPTUNE", "ALTALENA"))
        assertFalse(isKnownMisattributedPlaybackMetadata("BLANCO", "SOTTOGONNA"))
    }

    @Test
    fun canonicalMetadataMatchRejectsKnownMisattributionAndChoosesOfficialRecording() {
        val original = track(
            id = "chart-source",
            title = "SOTTOGONNA",
            artist = "neptune.",
            durationMs = 175_000L
        )
        val leaked = track(
            id = "leaked",
            title = "SOTTOGONNA",
            artist = "neptune.",
            durationMs = 175_000L
        )
        val official = track(
            id = "official",
            title = "SOTTOGONNA",
            artist = "BLANCO",
            durationMs = 175_000L,
            album = "SOTTOGONNA",
            isrc = "ITUM72600001",
            metadataConfidence = 95
        )

        val match = bestCanonicalPlaybackMetadataMatch(original, listOf(leaked, official))

        assertEquals("official", match?.id)
        assertEquals("BLANCO", match?.artist)
    }

    @Test
    fun canonicalMetadataMatchRejectsDifferentRecordingDuration() {
        val original = track(
            id = "chart-source",
            title = "ALTALENA",
            artist = "neptune.",
            durationMs = 190_000L
        )
        val unrelated = track(
            id = "other",
            title = "ALTALENA",
            artist = "Another Artist",
            durationMs = 260_000L
        )

        assertNull(bestCanonicalPlaybackMetadataMatch(original, listOf(unrelated)))
    }

    @Test
    fun misattributedArtistQueriesTryTitleOnlyOfficialResultsFirst() {
        val queries = playbackAlternativeSearchQueries(
            track(
                id = "chart-source",
                title = "SOTTOGONNA",
                artist = "neptune.",
                durationMs = 175_000L
            )
        )

        assertEquals("SOTTOGONNA official audio", queries.first())
        assertTrue("SOTTOGONNA" in queries)
    }

    @Test
    fun canonicalFallbackArtistAdoptsVerifiedOfficialArtist() {
        val original = track(
            id = "chart-source",
            title = "SOTTOGONNA",
            artist = "neptune.",
            durationMs = 175_000L
        )
        val candidate = track(
            id = "official",
            title = "SOTTOGONNA",
            artist = "BLANCO",
            durationMs = 175_000L
        )

        assertEquals(
            "BLANCO",
            canonicalPlaybackFallbackArtist(original, candidate, candidate)
        )
    }

    @Test
    fun verifiedFallbackAcceptsSameRecordingWithFeaturedArtistsCollapsed() {
        val original = track(
            id = "chart-source",
            title = "DALE (feat. Frezza, G.Mineiro & R3versal)",
            artist = "Yung Snapp, Frezza, G.Mineiro, R3versal",
            durationMs = 188_000L
        )
        val official = track(
            id = "Q1XvQgDjgQk",
            title = "DALE",
            artist = "Yung Snapp",
            durationMs = 188_000L
        )

        val matches = trustedPlaybackFallbackCandidates(original, listOf(official))

        assertEquals(listOf("Q1XvQgDjgQk"), matches.map { it.id })
    }

    @Test
    fun verifiedFallbackRejectsWrongArtistEvenWithSameTitleAndDuration() {
        val original = track(
            id = "chart-source",
            title = "DALE (feat. Frezza, G.Mineiro & R3versal)",
            artist = "Yung Snapp, Frezza, G.Mineiro, R3versal",
            durationMs = 188_000L
        )
        val wrong = track(
            id = "wrong-video",
            title = "DALE",
            artist = "Another Artist",
            durationMs = 188_000L
        )

        assertTrue(trustedPlaybackFallbackCandidates(original, listOf(wrong)).isEmpty())
    }

    @Test
    fun verifiedFallbackRejectsWrongDurationAndAlternateVariant() {
        val original = track(
            id = "chart-source",
            title = "DALE (feat. Frezza, G.Mineiro & R3versal)",
            artist = "Yung Snapp, Frezza, G.Mineiro, R3versal",
            durationMs = 188_000L
        )
        val wrongDuration = track(
            id = "wrong-duration",
            title = "DALE",
            artist = "Yung Snapp",
            durationMs = 245_000L
        )
        val remix = track(
            id = "remix",
            title = "DALE Remix",
            artist = "Yung Snapp",
            durationMs = 188_000L
        )

        assertTrue(trustedPlaybackFallbackCandidates(original, listOf(wrongDuration, remix)).isEmpty())
    }

    @Test
    fun verifiedFallbackPrefersExactIsrcOverMatchingMetadataWithDifferentIsrc() {
        val original = track(
            id = "chart-source",
            title = "DALE (feat. Frezza, G.Mineiro & R3versal)",
            artist = "Yung Snapp, Frezza, G.Mineiro, R3versal",
            durationMs = 188_000L,
            isrc = "ITABC2600001"
        )
        val metadataImpostor = track(
            id = "metadata-impostor",
            title = "DALE",
            artist = "Yung Snapp",
            durationMs = 188_000L,
            isrc = "ITXYZ2600002"
        )
        val official = track(
            id = "official-audio",
            title = "DALE",
            artist = "Yung Snapp",
            durationMs = 187_000L,
            isrc = "itabc2600001"
        )

        assertEquals(
            listOf("official-audio"),
            trustedPlaybackFallbackCandidates(original, listOf(metadataImpostor, official)).map { it.id }
        )
    }

    @Test
    fun verifiedFallbackRejectsConflictingIsrcWhenExactMatchIsUnavailable() {
        val original = track(
            id = "chart-source",
            title = "DALE",
            artist = "Yung Snapp",
            durationMs = 188_000L,
            isrc = "ITABC2600001"
        )
        val conflicting = track(
            id = "conflicting-recording",
            title = "DALE",
            artist = "Yung Snapp",
            durationMs = 188_000L,
            isrc = "ITXYZ2600002"
        )

        assertTrue(trustedPlaybackFallbackCandidates(original, listOf(conflicting)).isEmpty())
    }

    @Test
    fun verifiedFallbackDoesNotDowngradeKnownIsrcToMetadataOnlyCandidate() {
        val original = track(
            id = "chart-source",
            title = "DALE",
            artist = "Yung Snapp",
            durationMs = 188_000L,
            isrc = "ITABC2600001"
        )
        val metadataOnly = track(
            id = "metadata-only",
            title = "DALE",
            artist = "Yung Snapp",
            durationMs = 188_000L
        )

        assertTrue(trustedPlaybackFallbackCandidates(original, listOf(metadataOnly)).isEmpty())
    }

    @Test
    fun editorialDiscoveryPreservesSpecialRecoveryForKnownMisattribution() {
        val original = track(
            id = "catalog-track", title = "SOTTOGONNA",
            artist = "neptune.", durationMs = 175_000L
        ).copy(source = "Levyra Editorial Discovery")

        assertEquals("SOTTOGONNA official audio", playbackAlternativeSearchQueries(original).first())
    }

    @Test
    fun editorialDiscoveryCanUseSameVerifiedSearchResultWithoutIsrc() {
        val original = track(
            id = "catalog-track",
            title = "By my side",
            artist = "Tyson Yoshi, Hins Cheung",
            durationMs = 191_000L,
            isrc = "HKABC2600001"
        ).copy(source = "Levyra Editorial Discovery", audioVideoId = "AAAAAAAAAAA")
        val searchResult = track(
            id = "BBBBBBBBBBB",
            title = "By My Side",
            artist = "Tyson Yoshi",
            durationMs = 191_000L
        ).copy(videoUrl = "https://music.youtube.com/watch?v=BBBBBBBBBBB")

        assertEquals(
            listOf("BBBBBBBBBBB"),
            trustedPlaybackFallbackCandidates(original, listOf(searchResult)).map { it.id }
        )
        val rebased = rebaseEditorialFallbackSource(original, searchResult)
        assertEquals(original.id, rebased.id)
        assertEquals("BBBBBBBBBBB", rebased.audioVideoId)
        assertEquals(original.isrc, rebased.isrc)
    }

    @Test
    fun editorialDiscoveryAcceptsSearchResultsThatOmitDurationButKeepsRecordingChecks() {
        val original = track(
            id = "levyra-cff760d7cbf458dfaaba",
            title = "By my side",
            artist = "Tyson Yoshi, Hins Cheung",
            durationMs = 191_689L
        ).copy(source = "Levyra Editorial Discovery")
        val official = track(
            id = "_GYbg02dFOw", title = "By my side", artist = "Tyson Yoshi, 張敬軒", durationMs = 0L
        )
        val live = track(
            id = "V3IFwz1f77Q",
            title = "TYSON YOSHI & 張敬軒 Hins Cheung - By My Side (THE VILLAIN LIVE IN HONG KONG 2025)",
            artist = "Tyson Yoshi",
            durationMs = 0L
        )
        val sequel = track(
            id = "yOg2knMuAyk", title = "By my side 2", artist = "Tyson Yoshi", durationMs = 0L
        )
        val otherArtist = track(
            id = "sZvn_-S24uw", title = "By My Side", artist = "AP Dhillon, Shinda Kahlon", durationMs = 0L
        )
        val knownWrongDuration = track(
            id = "CCCCCCCCCCC", title = "By my side", artist = "Tyson Yoshi", durationMs = 240_000L
        )

        assertEquals(
            listOf("_GYbg02dFOw"),
            trustedPlaybackFallbackCandidates(
                original, listOf(official, live, sequel, otherArtist, knownWrongDuration)
            ).map { it.id }
        )
        assertFalse(isResolvedPlaybackFallbackDurationCompatible(original, official))
        assertTrue(isResolvedPlaybackFallbackDurationCompatible(original, official.copy(durationMs = 192_000L)))
    }

    @Test
    fun editorialDiscoveryStatusSurvivesAPreviousFallbackResolution() {
        val afterFirstFallback = track(
            id = "levyra-cff760d7cbf458dfaaba",
            title = "By my side",
            artist = "Tyson Yoshi, Hins Cheung",
            durationMs = 191_689L,
            isrc = "HKABC2600001"
        ).copy(
            source = "Android Reel audio · fallback _GYbg02dFOw",
            metadataProvider = "Levyra Editorial Discovery",
            audioVideoId = "_GYbg02dFOw"
        )
        val retry = track(id = "BBBBBBBBBBB", title = "By my side", artist = "Tyson Yoshi", durationMs = 192_000L)

        assertTrue(isEditorialDiscoveryPlayback(afterFirstFallback))
        assertEquals(listOf("BBBBBBBBBBB"), trustedPlaybackFallbackCandidates(afterFirstFallback, listOf(retry)).map { it.id })
        assertEquals("BBBBBBBBBBB", rebaseEditorialFallbackSource(afterFirstFallback, retry).audioVideoId)
    }

    @Test
    fun editorialReleaseTracksAreNotTreatedAsDiscovery() {
        val release = track(id = "release", title = "Song", artist = "Artist", durationMs = 180_000L)
            .copy(source = "Levyra Editorial Releases", metadataProvider = "Levyra Editorial Releases")

        assertFalse(isEditorialDiscoveryPlayback(release))
    }

    @Test
    fun onlyIdentityBoundAudioStrategiesRequireYoutubeIdentity() {
        assertTrue(PlaybackAudioStrategy.REEL_MUXED.requiresYoutubeIdentity())
        assertTrue(PlaybackAudioStrategy.REEL_AUDIO.requiresYoutubeIdentity())
        assertTrue(PlaybackAudioStrategy.DIRECT.requiresYoutubeIdentity())
        assertFalse(PlaybackAudioStrategy.PERSISTED.requiresYoutubeIdentity())
        assertFalse(PlaybackAudioStrategy.SEARCH.requiresYoutubeIdentity())
    }

    @Test
    fun editorialDiscoveryTriesNormalSearchBeforeQualifiedVariants() {
        val track = track(
            id = "catalog-track", title = "By my side",
            artist = "Tyson Yoshi, Hins Cheung", durationMs = 191_000L
        ).copy(source = "Levyra Editorial Discovery")

        assertEquals(
            "Tyson Yoshi, Hins Cheung By my side",
            playbackAlternativeSearchQueries(track).first()
        )
    }

    @Test
    fun editorialDiscoveryDoesNotAcceptUnrelatedOrConflictingIsrcResults() {
        val original = track(
            id = "catalog-track",
            title = "By my side",
            artist = "Tyson Yoshi, Hins Cheung",
            durationMs = 191_000L,
            isrc = "HKABC2600001"
        ).copy(source = "Levyra Editorial Discovery")
        val wrongArtist = track(
            id = "AAAAAAAAAAA", title = "By My Side", artist = "Someone Else", durationMs = 191_000L
        )
        val wrongDuration = track(
            id = "BBBBBBBBBBB", title = "By My Side", artist = "Tyson Yoshi", durationMs = 270_000L
        )
        val conflictingIsrc = track(
            id = "CCCCCCCCCCC", title = "By My Side", artist = "Tyson Yoshi",
            durationMs = 191_000L, isrc = "HKXYZ2600002"
        )
        assertTrue(
            trustedPlaybackFallbackCandidates(
                original, listOf(wrongArtist, wrongDuration, conflictingIsrc)
            ).isEmpty()
        )
    }

    @Test
    fun editorialDiscoveryRequiresKnownDurationForMetadataOnlyIsrcFallback() {
        val original = track(
            id = "catalog-track", title = "By my side", artist = "Tyson Yoshi",
            durationMs = 0L, isrc = "HKABC2600001"
        ).copy(source = "Levyra Editorial Discovery")
        val candidate = track(
            id = "BBBBBBBBBBB", title = "By my side", artist = "Tyson Yoshi",
            durationMs = 191_000L
        )
        assertTrue(trustedPlaybackFallbackCandidates(original, listOf(candidate)).isEmpty())
    }

    @Test
    fun regularPlaybackKeepsStrictIsrcAndOriginalIdentity() {
        val original = track(
            id = "search-track", title = "By my side", artist = "Tyson Yoshi",
            durationMs = 191_000L, isrc = "HKABC2600001"
        ).copy(audioVideoId = "AAAAAAAAAAA")
        val candidate = track(
            id = "BBBBBBBBBBB", title = "By my side", artist = "Tyson Yoshi",
            durationMs = 191_000L
        )
        assertTrue(trustedPlaybackFallbackCandidates(original, listOf(candidate)).isEmpty())
        assertEquals(original, rebaseEditorialFallbackSource(original, candidate))
    }

    @Test
    fun resolvedFallbackRejectsSourceWhoseRealDurationIsOnlyTwentyNineSeconds() {
        val original = track(
            id = "chart-source",
            title = "DALE (feat. Frezza, G.Mineiro & R3versal)",
            artist = "Yung Snapp, Frezza, G.Mineiro, R3versal",
            durationMs = 188_000L
        )
        val falseResolvedSource = track(
            id = "false-source",
            title = "DALE",
            artist = "Yung Snapp",
            durationMs = 29_000L
        )

        assertFalse(isResolvedPlaybackFallbackDurationCompatible(original, falseResolvedSource))
    }

    @Test
    fun resolvedFallbackAcceptsRealRecordingDurationWithinTolerance() {
        val original = track(
            id = "chart-source",
            title = "DALE (feat. Frezza, G.Mineiro & R3versal)",
            artist = "Yung Snapp, Frezza, G.Mineiro, R3versal",
            durationMs = 188_000L
        )
        val officialResolvedSource = track(
            id = "Q1XvQgDjgQk",
            title = "DALE",
            artist = "Yung Snapp",
            durationMs = 188_000L
        )

        assertTrue(isResolvedPlaybackFallbackDurationCompatible(original, officialResolvedSource))
    }

    @Test
    fun resolvedFallbackRequiresKnownActualDurationWhenCatalogDurationIsKnown() {
        val original = track(
            id = "chart-source",
            title = "DALE",
            artist = "Yung Snapp",
            durationMs = 188_000L
        )
        val unresolvedDuration = track(
            id = "unknown-duration",
            title = "DALE",
            artist = "Yung Snapp",
            durationMs = 0L
        )

        assertFalse(isResolvedPlaybackFallbackDurationCompatible(original, unresolvedDuration))
    }

    private fun track(
        id: String,
        title: String,
        artist: String,
        durationMs: Long,
        album: String = "",
        isrc: String = "",
        metadataConfidence: Int = 0
    ) = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 50,
        cacheScore = 50,
        accentStart = 0,
        accentEnd = 0,
        isrc = isrc,
        metadataConfidence = metadataConfidence
    )
}
