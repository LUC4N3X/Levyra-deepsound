package com.luc4n3x.levyra.viewmodel

import android.content.Context
import android.net.Uri
import com.luc4n3x.levyra.data.PLAYLIST_COVER_RENDER_PX
import com.luc4n3x.levyra.data.PlaylistCoverArtist
import com.luc4n3x.levyra.data.PlaylistCoverCrop
import com.luc4n3x.levyra.data.PlaylistCoverStore
import com.luc4n3x.levyra.data.PlaylistStore
import com.luc4n3x.levyra.data.PlaylistStudioStoreSnapshot
import com.luc4n3x.levyra.domain.PlaylistCoverMode
import com.luc4n3x.levyra.domain.PlaylistCoverStyle
import com.luc4n3x.levyra.domain.PlaylistStudioDraft
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.buildPlaylistCoverPlan
import java.io.IOException
import timber.log.Timber

internal class PlaylistStudioStoreGateway(
    context: Context,
    private val store: PlaylistStore,
    private val refresh: (String) -> Unit
) : PlaylistStudioGateway {
    private val artist = PlaylistCoverArtist(context)
    private val coverStore = PlaylistCoverStore(context.applicationContext)

    private data class StoreRollbackToken(
        val playlistId: String,
        val snapshot: PlaylistStudioStoreSnapshot,
        val coverBytes: ByteArray?
    ) : PlaylistStudioRollbackToken

    override suspend fun create(name: String, tracks: List<Track>): String =
        store.createForStudio(name, tracks).id

    override suspend fun captureRollback(playlistId: String): PlaylistStudioRollbackToken? {
        val snapshot = store.snapshotStudio(playlistId) ?: return null
        val playlist = snapshot.playlist
        val coverBytes = if (
            playlist.coverMode == PlaylistCoverMode.CUSTOM.name &&
            playlist.coverUrl.isNotBlank()
        ) {
            coverStore.readBackup(playlist.coverUrl)
                ?: throw IOException("Unable to snapshot the current playlist cover")
        } else {
            null
        }
        return StoreRollbackToken(playlistId, snapshot, coverBytes)
    }

    override suspend fun update(playlistId: String, name: String, tracks: List<Track>) {
        if (!store.applyStudioEdit(playlistId, name, tracks)) throw PlaylistStudioMissingException(playlistId)
    }

    override suspend fun applyCover(playlistId: String, draft: PlaylistStudioDraft): String? {
        when (draft.coverStyle) {
            PlaylistCoverStyle.Current -> return null
            PlaylistCoverStyle.Automatic -> {
                store.resetCover(playlistId)
                return ""
            }
            PlaylistCoverStyle.Photo -> {
                val photo = draft.photo ?: throw IllegalStateException("Photo cover selected without a source")
                store.setCustomCover(
                    playlistId,
                    Uri.parse(photo.uri),
                    PlaylistCoverCrop(photo.viewportSizePx, photo.zoom, photo.offsetX, photo.offsetY)
                )
            }
            PlaylistCoverStyle.Artwork,
            PlaylistCoverStyle.Mosaic,
            PlaylistCoverStyle.Spotlight,
            PlaylistCoverStyle.Signal -> {
                val plan = buildPlaylistCoverPlan(draft) ?: return null
                val bitmap = artist.render(plan, PLAYLIST_COVER_RENDER_PX)
                try {
                    store.setRenderedCover(playlistId, bitmap)
                } finally {
                    bitmap.recycle()
                }
            }
        }
        return store.load(playlistId)?.coverUrl
    }

    override suspend fun rollbackCreated(playlistId: String): Boolean = try {
        store.delete(playlistId)
        true
    } catch (error: Exception) {
        Timber.w(error, "Playlist Studio created-playlist rollback failed")
        false
    }

    override suspend fun rollbackUpdated(
        playlistId: String,
        rollbackState: PlaylistStudioRollbackToken?
    ): Boolean {
        val rollback = rollbackState as? StoreRollbackToken ?: return false
        if (rollback.playlistId != playlistId) return false
        return try {
            val current = store.load(playlistId)
            val previousWasCustom = rollback.snapshot.playlist.coverMode == PlaylistCoverMode.CUSTOM.name
            val restoredCoverUrl = if (previousWasCustom && rollback.snapshot.playlist.coverUrl.isNotBlank()) {
                val coverBytes = rollback.coverBytes ?: return false
                coverStore.restore(playlistId, coverBytes)
            } else {
                null
            }
            val snapshot = if (restoredCoverUrl != null) {
                rollback.snapshot.copy(
                    playlist = rollback.snapshot.playlist.copy(coverUrl = restoredCoverUrl)
                )
            } else {
                rollback.snapshot
            }
            val restored = store.restoreStudio(snapshot)
            if (restored) {
                if (
                    current?.coverMode == PlaylistCoverMode.CUSTOM &&
                    current.coverUrl.isNotBlank() &&
                    current.coverUrl != restoredCoverUrl
                ) {
                    coverStore.delete(current.coverUrl)
                }
            } else if (restoredCoverUrl != null) {
                coverStore.delete(restoredCoverUrl)
            }
            restored
        } catch (error: Exception) {
            Timber.w(error, "Playlist Studio edited-playlist rollback failed")
            false
        }
    }

    override fun onSaved(playlistId: String) = refresh(playlistId)
}
