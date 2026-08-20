package com.luc4n3x.levyra.desktop.core.localmusic

import com.luc4n3x.levyra.desktop.core.storage.JsonFileStore
import java.nio.file.Files
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalLibraryStoreTest {

    @Test
    fun removingParentFolderPreservesTracksCoveredByRemainingChildFolder() {
        val root = Files.createTempDirectory("levyra-local-store")
        val parent = root.resolve("Music")
        val child = parent.resolve("Albums")
        Files.createDirectories(child)
        val backing = JsonFileStore(
            file = root.resolve("localmusic.json"),
            serializer = LocalLibraryData.serializer(),
            defaultValue = { LocalLibraryData() },
            json = JsonFileStore.DEFAULT_JSON
        )
        val store = LocalLibraryStore(backing, nowMillis = { 1_000L })
        val parentFolder = requireNotNull(store.addFolder(parent.toString()))
        val childFolder = requireNotNull(store.addFolder(child.toString()))
        store.replaceTracks(
            listOf(
                LocalTrack(id = "parent", path = parent.resolve("loose.flac").toString(), folderId = parentFolder.id),
                LocalTrack(id = "child", path = child.resolve("album.flac").toString(), folderId = childFolder.id)
            )
        )

        store.removeFolder(parentFolder.id)

        assertEquals(listOf(childFolder.id), store.current.folders.map { it.id })
        assertEquals(listOf("child"), store.current.tracks.map { it.id })
        assertTrue(LocalMusicIdentity.isWithin(store.current.tracks.single().path, childFolder.path))
    }
}
