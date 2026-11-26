package com.luc4n3x.levyra.desktop.core.storage

import com.luc4n3x.levyra.desktop.core.model.Track
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneLibraryImportTest {

    private fun zip(vararg entries: Pair<String, String>): ByteArrayInputStream {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            entries.forEach { (name, body) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(body.toByteArray())
                zip.closeEntry()
            }
        }
        return ByteArrayInputStream(bytes.toByteArray())
    }

    private fun track(id: String) =
        """{"id":"$id","title":"T$id","artist":"A","videoUrl":"https://music.youtube.com/watch?v=$id","thumbnailUrl":"https://i/$id.jpg","durationMs":1000}"""

    @Test
    fun readsVaultSectionsAndSkipsUnplayableTracks() {
        val library = PhoneLibraryImport.read(
            zip(
                "manifest.json" to "{}",
                "data/favorites.json" to "[${track("a")},{\"id\":\"local\",\"videoUrl\":\"content://x\"}]",
                "data/playlists.json" to """[{"name":"Road","tracks":[${track("b")}]},{"name":" ","tracks":[]}]""",
                "data/history.json" to """[{"trackId":"c","title":"C","videoUrl":"https://youtube.com/watch?v=c","startedAt":42}]"""
            )
        )
        assertEquals(listOf("a"), library.favorites.map { it.id })
        assertEquals("https://i/a.jpg", library.favorites.single().artworkUrl)
        assertEquals(listOf("Road"), library.playlists.map { it.name })
        assertEquals(42L, library.history.single().playedAt)
    }

    @Test
    fun readsLegacyPayload() {
        val library = PhoneLibraryImport.read(zip("payload.json" to """{"favorites":[${track("x")}]}"""))
        assertEquals(listOf("x"), library.favorites.map { it.id })
    }

    @Test(expected = IOException::class)
    fun rejectsArchivesWithoutLibraryData() {
        PhoneLibraryImport.read(zip("other.txt" to "hello"))
    }

    @Test
    fun mergeKeepsExistingDataAndAvoidsDuplicates() {
        val existing = Track(id = "a", title = "A", videoUrl = "https://youtube.com/watch?v=a")
        val current = LibraryData(
            favorites = listOf(existing),
            playlists = listOf(LocalPlaylist("p1", "Road", 1L, listOf(existing)))
        )
        val incomingB = existing.copy(id = "b")
        val phone = PhoneLibrary(
            favorites = listOf(existing, incomingB),
            playlists = listOf(PhonePlaylist("road", listOf(existing, incomingB)), PhonePlaylist("Gym", listOf(incomingB))),
            history = listOf(HistoryEntry(incomingB, 10L))
        )
        val (merged, result) = PhoneLibraryImport.merge(current, phone, maxHistory = 200, nowMillis = 5L) { "new" }
        assertEquals(listOf("a", "b"), merged.favorites.map { it.id })
        assertEquals(listOf("a", "b"), merged.playlists.first { it.id == "p1" }.tracks.map { it.id })
        assertEquals(listOf("Road", "Gym"), merged.playlists.map { it.name })
        assertEquals(PhoneImportResult(favorites = 1, playlists = 1, history = 1), result)
    }
}
