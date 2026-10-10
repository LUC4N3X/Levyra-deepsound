package com.luc4n3x.levyra.nexus.playlistimport

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistImportReviewTest {
    @Test
    fun commitSelectionPreservesIntentionalRepeatedTracksInOrder() {
        val candidateA = MatchCandidate(
            id = "track-a",
            title = "Track A",
            artists = listOf("Artist"),
            origin = CandidateOrigin.ONLINE,
            kind = CandidateKind.SONG
        )
        val candidateB = MatchCandidate(
            id = "track-b",
            title = "Track B",
            artists = listOf("Artist"),
            origin = CandidateOrigin.ONLINE,
            kind = CandidateKind.SONG
        )

        fun matched(position: Int, candidate: MatchCandidate): ImportEntry {
            val evaluation = MatchEvaluation(
                candidate = candidate,
                score = 100,
                reasons = listOf(MatchReason(MatchSignal.DIRECT_ID))
            )
            return ImportEntry(
                identity = ImportedTrackIdentity(
                    position = position,
                    title = candidate.title,
                    artists = candidate.artists,
                    directCatalogId = candidate.id
                ),
                resolved = true,
                alternatives = listOf(evaluation),
                selected = evaluation,
                confidence = MatchConfidence.EXACT,
                automaticId = candidate.id,
                automaticConfidence = MatchConfidence.EXACT
            )
        }

        val selection = PlaylistImportReview.commitSelection(
            listOf(
                matched(0, candidateA),
                matched(1, candidateB),
                matched(2, candidateA)
            )
        )

        assertEquals(listOf("track-a", "track-b", "track-a"), selection.map { it.id })
    }
}
