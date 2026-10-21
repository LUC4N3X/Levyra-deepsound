package com.luc4n3x.levyra.data.playlistimport

import com.luc4n3x.levyra.nexus.playlistimport.*
import org.junit.Assert.*
import org.junit.Test

class PlaylistImportMetadataTest {
    private val source = ImportedTrackIdentity(0, "Группа крови", listOf("Кино"))
    private val metadata = MatchCandidate("apple:1", source.title, source.artists, album = "Группа крови", durationMs = 282_000, kind = CandidateKind.SONG)

    @Test
    fun corroborationAddsOnlyMissingMetadata() {
        val enriched = corroborateImportIdentity(source, listOf(metadata))
        assertEquals(282_000L, enriched.durationMs)
        assertEquals(source.title, enriched.title)
        assertEquals(source.artists, enriched.artists)
        assertEquals("", enriched.directCatalogId)
        val known = source.copy(durationMs = 280_000, album = "Original album")
        assertEquals(known, corroborateImportIdentity(known, listOf(metadata)))
    }

    @Test
    fun isrcAndAlbumPickTheRecordingAmongConflictingVersions() {
        val single = metadata.copy(id = "single", album = "Лучшие песни", durationMs = 236_000)
        val withAlbum = source.copy(album = "Группа крови")
        assertEquals(282_000L, corroborateImportIdentity(withAlbum, listOf(metadata, single)).durationMs)
        val withIsrc = source.copy(isrc = "RUA1D0800001")
        val tagged = single.copy(title = "Gruppa krovi", artists = listOf("Kino"), isrc = "RUA1D0800001")
        assertEquals(236_000L, corroborateImportIdentity(withIsrc, listOf(metadata, tagged)).durationMs)
    }

    @Test
    fun isrcProvenMetadataSuppliesCanonicalNamesForAliasedArtists() {
        val korean = ImportedTrackIdentity(0, "봄날", listOf("방탄소년단"), isrc = "KRA381700117")
        val canonical = MatchCandidate("spotify:1", "Spring Day", listOf("BTS"), durationMs = 274_000, isrc = "KRA381700117", kind = CandidateKind.SONG)
        val enriched = corroborateImportIdentity(korean, listOf(canonical))
        assertEquals("Spring Day", enriched.title)
        assertEquals(listOf("BTS"), enriched.artists)
        assertEquals(274_000L, enriched.durationMs)
        assertEquals(korean, corroborateImportIdentity(korean, listOf(canonical.copy(isrc = ""))))
    }

    @Test
    fun conflictingDurationsAndWrongArtistsDoNotEnrich() {
        assertEquals(source, corroborateImportIdentity(source, listOf(metadata, metadata.copy(id = "other", durationMs = 300_000))))
        assertEquals(source, corroborateImportIdentity(source, listOf(metadata.copy(artists = listOf("Cover band")))))
        assertEquals(source, corroborateImportIdentity(source, listOf(metadata.copy(title = "Группа крови (Live)"))))
        assertEquals(source, corroborateImportIdentity(source, listOf(metadata.copy(title = "Gruppa krovi", artists = listOf("Kino")))))
    }
}
