package com.luc4n3x.levyra.nexus.playlistimport

import com.luc4n3x.levyra.nexus.identity.MusicIdentityText
import org.junit.Assert.*
import org.junit.Test

class PlaylistInternationalMatchingTest {
    @Test
    fun hyphenatedRemasterSpellingDoesNotChangeTheSongTitle() {
        val source = ImportedTrackIdentity(0, "Magic Fly - 2009 Re-master", listOf("Space"), durationMs = 262_060)
        val candidate = MatchCandidate("recording", "Magic Fly (2009 Remastered Version)", listOf("Space"),
            durationMs = 263_000, kind = CandidateKind.SONG)
        assertEquals("magic fly", MusicIdentityText.title(source.title).core)
        assertTrue(PlaylistMatchEngine.evaluate(source, candidate).confidence.autoAccepted)
        assertFalse(PlaylistMatchEngine.evaluate(source, candidate.copy(title = "Magic Fly (Live)")).confidence.autoAccepted)
    }

    @Test
    fun transliteratedCatalogEntriesMatchButAreNeverExact() {
        listOf(
            "Кино" to "Kino", "Виктор Цой" to "Viktor Tsoi", "Виктор Цой" to "Victor Tsoy",
            "Земфира" to "Zemfira"
        ).forEach { (sourceArtist, candidateArtist) ->
            val source = ImportedTrackIdentity(0, "Группа крови", listOf(sourceArtist), durationMs = 240_000)
            val candidate = MatchCandidate("a", "Gruppa krovi", listOf(candidateArtist), durationMs = 240_000, kind = CandidateKind.SONG)
            val evaluation = PlaylistMatchEngine.evaluate(source, candidate)
            assertEquals("$sourceArtist / $candidateArtist", MatchConfidence.GOOD, evaluation.confidence)
        }
        val source = ImportedTrackIdentity(0, "Знаешь ли ты", listOf("МакSим"))
        val official = MatchCandidate("official", "Znaesh` Li Ty", listOf("MakSim"), kind = CandidateKind.SONG)
        val duet = MatchCandidate("duet", source.title, listOf("Дима Билан", "МакSим"), kind = CandidateKind.SONG)
        val outcome = PlaylistMatchEngine.select(source, listOf(duet, official))
        assertEquals("official", outcome.selected?.candidate?.id)
        assertTrue(outcome.confidence.autoAccepted)
    }

    @Test
    fun equivalentUploadsOfTheSameSongAreNotAmbiguousWithoutSourceDuration() {
        val source = ImportedTrackIdentity(0, "Группа крови", listOf("Кино"))
        val album = MatchCandidate("album", "Группа крови", listOf("Кино"), durationMs = 285_000, kind = CandidateKind.SONG)
        val single = MatchCandidate("single", "Группа крови", listOf("Кино"), durationMs = 236_000, kind = CandidateKind.SONG)
        val orchestra = MatchCandidate("orchestra", "Группа крови (feat. Оркестр)", listOf("Кино"), durationMs = 300_000, kind = CandidateKind.SONG)
        val outcome = PlaylistMatchEngine.select(source, listOf(album, single, orchestra))
        assertEquals("album", outcome.selected?.candidate?.id)
        assertTrue(outcome.confidence.autoAccepted)
        assertFalse(PlaylistMatchEngine.evaluate(source, orchestra).confidence.autoAccepted)
        val timed = source.copy(durationMs = 260_000)
        assertFalse(PlaylistMatchEngine.select(timed, listOf(album, single)).confidence.autoAccepted)
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
        assertEquals(MusicIdentityText.normalize("ΕΛΛΑΔΑ"), MusicIdentityText.normalize("Ελλάδα"))
        assertEquals(MusicIdentityText.normalize("نسم علينا الهوى"), MusicIdentityText.normalize("نَسَّم عَلَينا الهَوى"))
        assertEquals(MusicIdentityText.normalize("שלום"), MusicIdentityText.normalize("שָׁלוֹם"))
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
