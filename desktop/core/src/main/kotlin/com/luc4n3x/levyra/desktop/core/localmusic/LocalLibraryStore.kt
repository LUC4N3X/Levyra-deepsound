package com.luc4n3x.levyra.desktop.core.localmusic

import com.luc4n3x.levyra.desktop.core.storage.AppPaths
import com.luc4n3x.levyra.desktop.core.storage.JsonFileStore
import java.nio.file.Path
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocalLibraryStore(
    private val store: JsonFileStore<LocalLibraryData>,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    private val state = MutableStateFlow(store.read())

    val data: StateFlow<LocalLibraryData> = state.asStateFlow()

    val current: LocalLibraryData get() = state.value

    @Synchronized
    fun addFolder(path: String): LocalFolder? {
        val normalized = runCatching { Path.of(path.trim()).toAbsolutePath().normalize().toString() }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: return null
        val key = LocalMusicIdentity.normalizePathKey(normalized)
        val existing = state.value.folders.firstOrNull {
            LocalMusicIdentity.normalizePathKey(it.path) == key
        }
        if (existing != null) return existing
        if (state.value.folders.size >= MAX_FOLDERS) return null
        val folder = LocalFolder(
            id = LocalMusicIdentity.hashOf(normalized),
            path = normalized,
            addedAtMs = nowMillis()
        )
        mutate { data -> data.copy(folders = data.folders + folder) }
        return folder
    }

    fun removeFolder(folderId: String) = mutate { data ->
        val folder = data.folders.firstOrNull { it.id == folderId } ?: return@mutate data
        data.copy(
            folders = data.folders.filterNot { it.id == folderId },
            tracks = data.tracks.filterNot { LocalMusicIdentity.isWithin(it.path, folder.path) }
        )
    }

    fun replaceTracks(tracks: List<LocalTrack>) = mutate { data ->
        data.copy(tracks = tracks, lastScanAtMs = nowMillis())
    }

    fun forgetUnavailable() = mutate { data ->
        data.copy(tracks = data.tracks.filter { it.available })
    }

    @Synchronized
    private fun mutate(transform: (LocalLibraryData) -> LocalLibraryData) {
        val updated = transform(state.value)
        if (updated == state.value) return
        store.write(updated)
        state.value = updated
    }

    companion object {
        const val MAX_FOLDERS = 24

        fun create(paths: AppPaths): LocalLibraryStore = LocalLibraryStore(
            JsonFileStore(
                file = paths.localMusicFile,
                serializer = LocalLibraryData.serializer(),
                defaultValue = { LocalLibraryData() }
            )
        )
    }
}
