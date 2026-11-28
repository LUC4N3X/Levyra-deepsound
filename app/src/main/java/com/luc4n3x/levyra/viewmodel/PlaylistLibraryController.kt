package com.luc4n3x.levyra.viewmodel

import android.content.Context
import android.net.Uri
import com.luc4n3x.levyra.data.PlaylistCoverCrop
import com.luc4n3x.levyra.data.PlaylistStore
import com.luc4n3x.levyra.data.SavedPlaylistsStore
import com.luc4n3x.levyra.data.playlistimport.toImportIdentity
import com.luc4n3x.levyra.data.playlistimport.withEquivalentPlaybackSource
import com.luc4n3x.levyra.data.runCatchingPreservingCancellation
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ChartsCatalog
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.PlaylistHitPreview
import com.luc4n3x.levyra.domain.PlaylistImportFailureKind
import com.luc4n3x.levyra.domain.SavedPlaylist
import com.luc4n3x.levyra.domain.SpeedDial
import com.luc4n3x.levyra.domain.SpeedDialPin
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.canBeSaved
import com.luc4n3x.levyra.domain.isValidPlaylistTagName
import com.luc4n3x.levyra.domain.resolvedWith
import com.luc4n3x.levyra.domain.savedPlaylistKey
import com.luc4n3x.levyra.feature.providers.LevyraProviderRouter
import com.luc4n3x.levyra.feature.sharedmedia.LevyraPlaylistShareCodec
import com.luc4n3x.levyra.feature.sharedmedia.LevyraSharedTrack
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.playlistImportHubCopy
import com.luc4n3x.levyra.ui.i18n.playlistProCopy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

private const val PLAYLIST_ALBUM_RECOVERY_TIMEOUT_MS = 8_000L
private const val REMOTE_PLAYLIST_PREVIEW_TRACK_LIMIT = 300
private const val MAX_IMPORT_FILE_BYTES = 8L * 1024 * 1024

internal class PlaylistLibraryController(
    private val scope: CoroutineScope,
    private val context: Context,
    private val uiState: MutableStateFlow<LevyraUiState>,
    private val playlistStore: PlaylistStore,
    private val savedPlaylistsStore: SavedPlaylistsStore,
    private val savedPlaylists: StateFlow<List<SavedPlaylist>>,
    private val providerRouter: LevyraProviderRouter,
    private val importEngine: Lazy<PlaylistImportEngine>,
    private val playlistSyncProvider: () -> PlaylistSyncController,
    private val carryFavorite: suspend (Track, Track) -> Unit,
    private val removeSpeedDialPin: (String) -> Unit,
    private val mutateSpeedDial: ((List<SpeedDialPin>) -> List<SpeedDialPin>) -> Unit,
    private val clearPlayerReturnDetail: () -> Unit
) {
    private val playlistImportCatalog get() = importEngine.value.catalog
    private val playlistImport: PlaylistImportController get() = importEngine.value.controller
    private val playlistSync: PlaylistSyncController get() = playlistSyncProvider()
    private var replacementSearchJob: Job? = null
    private var playlistHitJob: Job? = null

    fun cancelReplacementSearch() {
        replacementSearchJob?.cancel()
    }

    fun cancelPlaylistHit() {
        playlistHitJob?.cancel()
    }

    fun loadPlaylists() {
        scope.launch {
            val lists = playlistStore.loadAll()
            val tags = playlistStore.allTags()
            uiState.update { it.copy(playlists = lists, playlistTags = tags) }
            playlistSync.onPlaylistsChanged()
        }
    }

    fun setPlaylistHidden(playlistId: String, hidden: Boolean) {
        if (playlistId.isBlank()) return
        scope.launch {
            playlistStore.setHidden(playlistId, hidden)
            loadPlaylists()
            refreshOpenPlaylist(playlistId)
        }
    }

    fun createPlaylistTag(name: String, assignToPlaylistId: String? = null) {
        if (!isValidPlaylistTagName(name)) return
        scope.launch {
            val tag = playlistStore.createTag(name) ?: return@launch
            if (!assignToPlaylistId.isNullOrBlank()) {
                val existing = playlistStore.load(assignToPlaylistId)?.tags?.map { it.id }.orEmpty()
                if (tag.id !in existing) {
                    playlistStore.setPlaylistTags(assignToPlaylistId, existing + tag.id)
                }
            }
            loadPlaylists()
            assignToPlaylistId?.let { refreshOpenPlaylist(it) }
        }
    }

    fun renamePlaylistTag(tagId: String, name: String) {
        if (tagId.isBlank() || !isValidPlaylistTagName(name)) return
        scope.launch {
            playlistStore.renameTag(tagId, name)
            loadPlaylists()
            uiState.value.openPlaylist?.id?.let { refreshOpenPlaylist(it) }
        }
    }

    fun deletePlaylistTag(tagId: String) {
        if (tagId.isBlank()) return
        scope.launch {
            playlistStore.deleteTag(tagId)
            loadPlaylists()
            uiState.value.openPlaylist?.id?.let { refreshOpenPlaylist(it) }
        }
    }

    fun setPlaylistTags(playlistId: String, tagIds: List<String>) {
        if (playlistId.isBlank()) return
        scope.launch {
            playlistStore.setPlaylistTags(playlistId, tagIds)
            loadPlaylists()
            refreshOpenPlaylist(playlistId)
        }
    }

    fun createPlaylist(name: String, firstTrack: Track? = null) {
        scope.launch {
            playlistStore.create(name, firstTrack)
            loadPlaylists()
        }
    }

    fun createPlaylistWithTracks(name: String, tracks: List<Track>) {
        scope.launch {
            val cleanTracks = tracks.distinctBy { it.id }.filter { it.id.isNotBlank() }
            if (cleanTracks.isEmpty()) {
                uiState.update { it.copy(offlineExportMessage = LevyraStrings.forCode(it.languageCode).queueEmpty) }
                return@launch
            }
            val playlist = runCatchingPreservingCancellation {
                playlistStore.createWithTracks(name, cleanTracks.map { it.copy(streamUrl = "") })
            }.onFailure { error ->
                Timber.w(error, "Playlist batch create failed")
            }.getOrNull()
            if (playlist == null) {
                uiState.update {
                    it.copy(offlineExportMessage = LevyraStrings.forCode(it.languageCode).mixLabSaveFailed)
                }
                return@launch
            }
            loadPlaylists()
            uiState.update {
                it.copy(offlineExportMessage = LevyraStrings.forCode(it.languageCode).mixLabSaveSuccess)
            }
        }
    }

    fun importPlaylistFile(uri: android.net.Uri) {
        scope.launch {
            val appContext = context
            val (text, name, failure) = withContext(Dispatchers.IO) { readImportDocument(appContext, uri) }
            playlistImport.loadFile(text, name, failure)
        }
    }

    fun replacePlaylistTrack(playlistId: String, original: Track, replacement: Track) {
        scope.launch {
            val equivalent = withContext(Dispatchers.Default) {
                original.withEquivalentPlaybackSource(replacement)
            }
            val entry = equivalent ?: replacement
            val replaced = playlistStore.replaceTrack(playlistId, original.id, entry)
            if (replaced) {
                if (equivalent != null && original.id in uiState.value.favoriteIds) carryFavorite(original, entry)
                loadPlaylists()
                refreshOpenPlaylist(playlistId)
            } else {
                uiState.update { current ->
                    current.copy(offlineExportMessage = playlistImportHubCopy(current.languageCode).changeMatchConflict)
                }
            }
        }
    }

    fun searchPlaylistReplacements(
        reference: Track,
        query: String,
        origin: com.luc4n3x.levyra.nexus.playlistimport.CandidateOrigin,
        onResult: (List<Pair<com.luc4n3x.levyra.nexus.playlistimport.MatchEvaluation, Track>>) -> Unit
    ) {
        replacementSearchJob?.cancel()
        replacementSearchJob = scope.launch {
            val identity = reference.toImportIdentity(0)
            val results = withContext(Dispatchers.IO) {
                playlistImportCatalog.search(query, origin)
                    .filter { it.candidate.id != reference.id }
                    .map { com.luc4n3x.levyra.nexus.playlistimport.PlaylistMatchEngine.evaluate(identity, it.candidate) to it.track }
                    .sortedByDescending { it.first.score }
            }
            onResult(results)
        }
    }

    private fun readImportDocument(
        context: android.content.Context,
        uri: android.net.Uri
    ): Triple<String?, String, PlaylistImportFailureKind> {
        val name = runCatching {
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "playlist"
        val read = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                com.luc4n3x.levyra.data.readUtf8Bounded(stream, MAX_IMPORT_FILE_BYTES)
            }
        }.onFailure { Timber.w(it, "Unable to read playlist import file") }
        val failure = if (read.isSuccess) PlaylistImportFailureKind.TOO_LARGE else PlaylistImportFailureKind.FILE_MALFORMED
        return Triple(read.getOrNull(), name, failure)
    }

    fun playlistShareLink(playlist: com.luc4n3x.levyra.domain.Playlist): String? =
        LevyraPlaylistShareCodec.encodeLink(
            title = playlist.name,
            tracks = playlist.tracks.map { track ->
                LevyraSharedTrack(id = track.id, title = track.title, artist = track.artist)
            }
        )

    fun renamePlaylist(playlistId: String, name: String) {
        scope.launch {
            playlistStore.rename(playlistId, name)
            loadPlaylists()
            refreshOpenPlaylist(playlistId)
        }
    }

    fun deletePlaylist(playlistId: String) {
        SpeedDial.playlistKey(playlistId)?.let(removeSpeedDialPin)
        scope.launch {
            playlistSync.withPlaylistLock(playlistId) { playlistStore.delete(playlistId) }
            uiState.update { if (it.openPlaylist?.id == playlistId) it.copy(openPlaylist = null) else it }
            loadPlaylists()
        }
    }

    fun deletePlaylists(playlistIds: Collection<String>) {
        val uniqueIds = playlistIds.filter(String::isNotBlank).toSet()
        if (uniqueIds.isEmpty()) return
        val pinKeys = uniqueIds.mapNotNullTo(HashSet()) { SpeedDial.playlistKey(it) }
        if (pinKeys.isNotEmpty()) {
            mutateSpeedDial { pins -> pins.filterNot { it.key in pinKeys } }
        }
        scope.launch {
            uniqueIds.forEach { id -> playlistSync.withPlaylistLock(id) { playlistStore.delete(id) } }
            uiState.update { current ->
                if (current.openPlaylist?.id in uniqueIds) current.copy(openPlaylist = null) else current
            }
            loadPlaylists()
            uiState.update { it.copy(offlineExportMessage = "Playlist eliminate: ${uniqueIds.size}") }
        }
    }

    fun addToPlaylist(playlistId: String, track: Track) {
        scope.launch {
            playlistStore.addTrack(playlistId, track.copy(streamUrl = ""))
            loadPlaylists()
            refreshOpenPlaylist(playlistId)
        }
    }

    fun addTracksToPlaylist(playlistId: String, tracks: List<Track>) {
        val cleanTracks = tracks.distinctBy { it.id }.filter { it.id.isNotBlank() }
        if (cleanTracks.isEmpty()) return
        scope.launch {
            val saved = runCatchingPreservingCancellation {
                playlistStore.addTracks(playlistId, cleanTracks.map { it.copy(streamUrl = "") })
            }.onFailure { error ->
                Timber.w(error, "Playlist batch add failed")
            }.isSuccess
            if (!saved) {
                uiState.update {
                    it.copy(offlineExportMessage = LevyraStrings.forCode(it.languageCode).mixLabSaveFailed)
                }
                return@launch
            }
            loadPlaylists()
            refreshOpenPlaylist(playlistId)
            uiState.update {
                it.copy(offlineExportMessage = LevyraStrings.forCode(it.languageCode).mixLabSaveSuccess)
            }
        }
    }

    fun addAlbumToPlaylist(playlistId: String, album: AlbumHit, tracks: List<Track>) {
        if (tracks.isEmpty()) return
        scope.launch {
            val result = runCatchingPreservingCancellation {
                playlistStore.addAlbum(playlistId, album, tracks.map { it.copy(streamUrl = "") })
            }.onFailure { Timber.w(it, "Playlist album add failed") }
                .getOrDefault(com.luc4n3x.levyra.data.PlaylistAlbumAddResult.Unavailable)
            loadPlaylists()
            refreshOpenPlaylist(playlistId)
            uiState.update {
                val copy = com.luc4n3x.levyra.ui.i18n.playlistAlbumCopy(it.languageCode)
                val playlistName = it.playlists.firstOrNull { playlist -> playlist.id == playlistId }?.name.orEmpty()
                it.copy(
                    offlineExportMessage = when (result) {
                        com.luc4n3x.levyra.data.PlaylistAlbumAddResult.Added -> copy.addedTo(playlistName)
                        com.luc4n3x.levyra.data.PlaylistAlbumAddResult.AlreadyPresent -> copy.alreadyInPlaylist
                        com.luc4n3x.levyra.data.PlaylistAlbumAddResult.Unavailable -> copy.addFailed
                    }
                )
            }
        }
    }

    fun createPlaylistWithAlbum(name: String, album: AlbumHit, tracks: List<Track>) {
        if (tracks.isEmpty() || name.isBlank()) return
        scope.launch {
            val created = runCatchingPreservingCancellation {
                playlistStore.createWithAlbum(name, album, tracks.map { it.copy(streamUrl = "") })
            }.onFailure { Timber.w(it, "Playlist creation with album failed") }.getOrNull()
            loadPlaylists()
            uiState.update {
                val copy = com.luc4n3x.levyra.ui.i18n.playlistAlbumCopy(it.languageCode)
                it.copy(offlineExportMessage = if (created != null) copy.addedTo(created.name) else copy.addFailed)
            }
        }
    }

    fun removePlaylistItems(playlistId: String, items: List<com.luc4n3x.levyra.domain.PlaylistItem>) {
        val trackEntryIds = items.filterIsInstance<com.luc4n3x.levyra.domain.PlaylistItem.Song>()
            .mapNotNull { it.track.playlistEntryId.takeIf(String::isNotBlank) }
            .toSet()
        val albumEntryIds = items.filterIsInstance<com.luc4n3x.levyra.domain.PlaylistItem.Album>()
            .mapTo(hashSetOf()) { it.entry.entryId }
        val legacyTrackIds = items.filterIsInstance<com.luc4n3x.levyra.domain.PlaylistItem.Song>()
            .filter { it.track.playlistEntryId.isBlank() }
            .map { it.track.id }
            .filter(String::isNotBlank)
            .toSet()
        if (trackEntryIds.isEmpty() && albumEntryIds.isEmpty() && legacyTrackIds.isEmpty()) return
        scope.launch {
            playlistStore.removeEntries(playlistId, trackEntryIds, albumEntryIds)
            if (legacyTrackIds.isNotEmpty()) playlistStore.removeTracks(playlistId, legacyTrackIds)
            loadPlaylists()
            refreshOpenPlaylist(playlistId)
        }
    }

    fun reorderPlaylistItems(playlistId: String, orderedItems: List<com.luc4n3x.levyra.domain.PlaylistItem>) {
        val slots = orderedItems.map { item ->
            when (item) {
                is com.luc4n3x.levyra.domain.PlaylistItem.Song ->
                    com.luc4n3x.levyra.domain.PlaylistLayoutSlot(item.track.playlistEntryId, isAlbum = false)
                is com.luc4n3x.levyra.domain.PlaylistItem.Album ->
                    com.luc4n3x.levyra.domain.PlaylistLayoutSlot(item.entry.entryId, isAlbum = true)
            }
        }
        scope.launch {
            var saved = slots.none { it.entryId.isBlank() } && playlistStore.reorderItems(playlistId, slots)
            if (!saved && orderedItems.none { it is com.luc4n3x.levyra.domain.PlaylistItem.Album }) {
                playlistStore.reorder(
                    playlistId,
                    orderedItems.filterIsInstance<com.luc4n3x.levyra.domain.PlaylistItem.Song>().map { it.track }
                )
                saved = true
            }
            loadPlaylists()
            refreshOpenPlaylist(playlistId)
            uiState.update {
                it.copy(
                    offlineExportMessage = if (saved) "Ordine playlist salvato"
                    else LevyraStrings.forCode(it.languageCode).mixLabSaveFailed
                )
            }
        }
    }

    fun removeFromPlaylist(playlistId: String, trackId: String) {
        scope.launch {
            playlistStore.removeTrack(playlistId, trackId)
            loadPlaylists()
            refreshOpenPlaylist(playlistId)
        }
    }

    fun removeTracksFromPlaylist(playlistId: String, tracks: List<Track>) {
        val trackIds = tracks.map { it.id }.filter(String::isNotBlank).toSet()
        if (trackIds.isEmpty()) return
        scope.launch {
            playlistStore.removeTracks(playlistId, trackIds)
            loadPlaylists()
            refreshOpenPlaylist(playlistId)
            uiState.update { it.copy(offlineExportMessage = "Rimossi ${trackIds.size} brani dalla playlist") }
        }
    }

    fun reorderPlaylist(playlistId: String, orderedTracks: List<Track>) {
        scope.launch {
            playlistStore.reorder(playlistId, orderedTracks)
            loadPlaylists()
            refreshOpenPlaylist(playlistId)
            uiState.update { it.copy(offlineExportMessage = "Ordine playlist salvato") }
        }
    }

    fun openPlaylist(playlistId: String) {
        clearPlayerReturnDetail()
        scope.launch {
            val pl = playlistStore.load(playlistId)
            uiState.update { it.copy(openPlaylist = pl) }
        }
    }

    fun closePlaylist() {
        uiState.update { it.copy(openPlaylist = null) }
    }

    suspend fun withPlayableAlbums(playlist: Playlist): Playlist {
        val missing = playlist.albums.filter { it.tracks.isEmpty() }
        if (missing.isEmpty()) return playlist
        val languageCode = uiState.value.languageCode
        val recovered = coroutineScope {
            missing.map { entry ->
                async {
                    val tracks = withTimeoutOrNull(PLAYLIST_ALBUM_RECOVERY_TIMEOUT_MS) {
                        runCatchingPreservingCancellation { providerRouter.albumDetail(entry.album, languageCode) }
                            .onFailure { Timber.w(it, "Playlist album recovery failed") }
                            .getOrNull()?.tracks
                    }.orEmpty()
                    if (tracks.isNotEmpty()) playlistStore.refreshAlbumTracks(playlist.id, entry.entryId, tracks)
                    entry.entryId to tracks
                }
            }.awaitAll().toMap()
        }
        if (recovered.values.any { it.isEmpty() }) {
            uiState.update {
                it.copy(offlineExportMessage = com.luc4n3x.levyra.ui.i18n.playlistAlbumCopy(it.languageCode).albumUnavailable)
            }
        }
        return playlist.copy(
            albums = playlist.albums.map { entry ->
                recovered[entry.entryId]?.takeIf(List<Track>::isNotEmpty)?.let { entry.copy(tracks = it) } ?: entry
            }
        )
    }

    suspend fun refreshOpenPlaylist(playlistId: String) {
        if (uiState.value.openPlaylist?.id != playlistId) return
        val pl = playlistStore.load(playlistId)
        uiState.update { it.copy(openPlaylist = pl) }
    }

    fun setPlaylistCover(playlistId: String, source: Uri, crop: PlaylistCoverCrop) {
        scope.launch {
            try {
                playlistStore.setCustomCover(playlistId, source, crop)
                loadPlaylists()
                refreshOpenPlaylist(playlistId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Playlist cover update failed")
                val message = LevyraStrings.forCode(uiState.value.languageCode).playlistProCopy().coverUpdateFailed
                uiState.update { it.copy(offlineExportMessage = message) }
            }
        }
    }

    fun resetPlaylistCover(playlistId: String) {
        scope.launch {
            try {
                playlistStore.resetCover(playlistId)
                loadPlaylists()
                refreshOpenPlaylist(playlistId)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Playlist cover reset failed")
                val message = LevyraStrings.forCode(uiState.value.languageCode).playlistProCopy().coverUpdateFailed
                uiState.update { it.copy(offlineExportMessage = message) }
            }
        }
    }

    fun openPlaylistHit(playlist: PlaylistHit) {
        clearPlayerReturnDetail()
        val playlistId = playlist.playlistId.ifBlank { return }
        if (com.luc4n3x.levyra.data.apple.AppleCatalogIds.playlist(playlistId) != null) {
            openApplePlaylist(playlist, ChartsCatalog.region(uiState.value.selectedChartId).country)
            return
        }
        playlistHitJob?.cancel()
        uiState.update { it.copy(playlistHitPreview = PlaylistHitPreview(hit = playlist)) }
        playlistHitJob = scope.launch {
            val detail = runCatchingPreservingCancellation {
                providerRouter.playlist(playlistId, uiState.value.languageCode, REMOTE_PLAYLIST_PREVIEW_TRACK_LIMIT)
            }.onFailure { Timber.w(it, "playlist preview lookup failed") }.getOrNull()
            val tracks = detail?.tracks.orEmpty()
            uiState.update { current ->
                val resolved = current.playlistHitPreview?.resolvedWith(
                    playlistId = playlistId,
                    author = detail?.author.orEmpty(),
                    thumbnailUrl = detail?.thumbnailUrl.orEmpty(),
                    tracks = tracks,
                    description = detail?.description.orEmpty(),
                    stats = detail?.stats.orEmpty()
                )
                if (resolved == null) current else current.copy(playlistHitPreview = resolved)
            }
        }
    }

    fun openApplePlaylist(playlist: PlaylistHit, storefront: String) {
        clearPlayerReturnDetail()
        val playlistId = playlist.playlistId.ifBlank { return }
        playlistHitJob?.cancel()
        uiState.update { it.copy(playlistHitPreview = PlaylistHitPreview(hit = playlist, appleStorefront = storefront)) }
        playlistHitJob = scope.launch {
            val collection = runCatchingPreservingCancellation {
                com.luc4n3x.levyra.data.apple.AppleMusicEditorialRepository.get(context)
                    .playlist(storefront, playlistId, uiState.value.languageCode)
            }.onFailure { Timber.w(it, "Apple playlist preview lookup failed") }.getOrNull()
            uiState.update { current ->
                val resolved = current.playlistHitPreview?.resolvedWith(
                    playlistId = playlistId,
                    author = collection?.curator.orEmpty(),
                    thumbnailUrl = collection?.artworkUrl.orEmpty(),
                    tracks = collection?.tracks.orEmpty()
                )
                if (resolved == null) current else current.copy(playlistHitPreview = resolved)
            }
        }
    }

    fun toggleSaveOpenPlaylistHit() {
        val preview = uiState.value.playlistHitPreview ?: return
        val key = preview.savedPlaylistKey
        val alreadySaved = savedPlaylists.value.any { it.key == key }
        if (!alreadySaved && !preview.canBeSaved) return
        scope.launch {
            runCatchingPreservingCancellation {
                savedPlaylistsStore.toggle(com.luc4n3x.levyra.domain.SavedPlaylist(preview.hit, preview.appleStorefront))
            }.onFailure { Timber.w(it, "Saved playlist toggle failed") }
        }
    }

    fun removeSavedPlaylist(key: String) {
        scope.launch {
            runCatchingPreservingCancellation { savedPlaylistsStore.remove(key) }
                .onFailure { Timber.w(it, "Saved playlist removal failed") }
        }
    }

    fun openSavedPlaylist(saved: com.luc4n3x.levyra.domain.SavedPlaylist) {
        if (saved.appleStorefront.isBlank()) openPlaylistHit(saved.hit) else openApplePlaylist(saved.hit, saved.appleStorefront)
    }

    fun closePlaylistHit() {
        playlistHitJob?.cancel()
        playlistHitJob = null
        uiState.update { it.copy(playlistHitPreview = null) }
    }
}
