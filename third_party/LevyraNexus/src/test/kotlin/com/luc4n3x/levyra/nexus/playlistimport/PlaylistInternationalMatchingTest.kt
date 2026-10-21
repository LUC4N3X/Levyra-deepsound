package com.luc4n3x.levyra.nexus.playlistimport

import com.luc4n3x.levyra.nexus.identity.MusicIdentityText
import org.junit.Assert.*
import org.junit.Test

class PlaylistInternationalMatchingTest {
    @Test
    fun transliterationRemainsReviewEvenWithMatchingDuration() {
        listOf(
            "Кино" to "Kino", "Виктор Цой" to "Viktor Tsoi", "Виктор Цой" to "Victor Tsoy",
            "Земфира" to "Zemfira"
        ).forEach { (sourceArtist, candidateArtist) ->
            val source = ImportedTrackIdentity(0, "Группа крови", listOf(sourceArtist), durationMs = 240_000)
            val candidate = MatchCandidate("a", "Gruppa krovi", listOf(candidateArtist), durationMs = 240_000, kind = CandidateKind.SONG)
            assertEquals("$sourceArtist / $candidateArtist", MatchConfidence.REVIEW, PlaylistMatchEngine.evaluate(source, candidate).confidence)
        }
    }

    @Test
    fun duetDoesNotReplaceSoloRecording() {
        val source = ImportedTrackIdentity(0, "Знаешь ли ты", listOf("МакSим"))
        val duet = MatchCandidate("duet", source.title, listOf("Дима Билан", "МакSим"), kind = CandidateKind.SONG)
        assertFalse(PlaylistMatchEngine.evaluate(source, duet).confidence.autoAccepted)
    }

    @Test
    fun unavailableDirectIdentityIsNotAccepted() {
        val source = ImportedTrackIdentity(0, "Song", listOf("Artist"), directCatalogId = "unavailable")
        assertFalse(PlaylistMatchEngine.evaluate(source, MatchCandidate("unavailable", "Song", listOf("Artist"), available = false)).confidence.autoAccepted)
    }

    @Test
    fun unicodeKeepsMeaningfulMarksAndNormalizesEditorialDifferences() {
        assertEquals(MusicIdentityText.normalize("Björk"), MusicIdentityText.normalize("Bjork"))
        assertEquals(MusicIdentityText.normalize("Алёна"), MusicIdentityText.normalize("Алена"))
        assertNotEquals(MusicIdentityText.normalize("й"), MusicIdentityText.normalize("и"))
        assertNotEquals(MusicIdentityText.normalize("ガ"), MusicIdentityText.normalize("カ"))
        listOf("봄날", "宇多田ヒカル", "周杰倫", "فيروز", "Обійми", "Beyoncé").forEach { text ->
            assertEquals(MusicIdentityText.normalize(text), MusicIdentityText.normalize(java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)))
        }
    }

    @Test
    fun localizedVersionsAndDifferentRemixersAreNotAutoAccepted() {
        listOf("(Концерт)", "(Караоке)", "(Кавер)").forEach { suffix ->
            val source = ImportedTrackIdentity(0, "Группа крови", listOf("Кино"), durationMs = 240_000)
            val candidate = MatchCandidate("a", "Группа крови $suffix", listOf("Кино"), durationMs = 240_000)
            assertFalse(suffix, PlaylistMatchEngine.evaluate(source, candidate).confidence.autoAccepted)
        }
        val source = ImportedTrackIdentity(0, "Song (Alpha Remix)", listOf("Artist"), durationMs = 240_000)
        val other = MatchCandidate("a", "Song (Beta Remix)", listOf("Artist"), durationMs = 240_000)
        assertFalse(PlaylistMatchEngine.evaluate(source, other).confidence.autoAccepted)
    }

    @Test
    fun errorsAreSeparateFromMissingAndCannotBeCommitted() {
        val entry = ImportEntry(ImportedTrackIdentity(0, "Song", listOf("Artist")), resolved = true, lookupFailed = true)
        assertEquals(ImportEntryStatus.ERROR, entry.status)
        assertEquals(1, PlaylistImportReview.counts(listOf(entry)).errors)
        assertEquals(0, PlaylistImportReview.counts(listOf(entry)).missing)
        assertTrue(PlaylistImportReview.commitSelection(listOf(entry)).isEmpty())
    }
}
