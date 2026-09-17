package com.luc4n3x.levyra.viewmodel

import android.content.Context
import android.net.Uri
import com.luc4n3x.levyra.data.PLAYLIST_COVER_RENDER_PX
import com.luc4n3x.levyra.data.PlaylistCoverArtist
import com.luc4n3x.levyra.data.PlaylistCoverCrop
import com.luc4n3x.levyra.data.PlaylistStore
import com.luc4n3x.levyra.data.PlaylistStudioStoreSnapshot
import com.luc4n3x.levyra.domain.PlaylistCoverStyle
import com.luc4n3x.levyra.domain.PlaylistStudioDraft
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.buildPlaylistCoverPlan
import timber.log.Timber

internal class PlaylistStudioStoreGateway(
    context: Context,
    private val store: PlaylistStore,
    private val refresh: (String) -> Unit
) : PlaylistStudioGateway {
    private val artist = PlaylistCoverArtist(context)

    private data class StoreRollbackToken(
        val playlistId: String,
        val snapshot: PlaylistStudioStoreSnapshot
    ) : PlaylistStudioRollbackToken

    override suspend fun create(name: String, tracks: List<Track>): String =
        store.createForStudio(name, tracks).id

    override suspend fun captureRollback(playlistId: String): PlaylistStudioRollbackToken? =
        store.snapshotStudio(playlistId)?.let { StoreRollbackToken(playlistId, it) }

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
            store.restoreStudio(rollback.snapshot)
        } catch (error: Exception) {
            Timber.w(error, "Playlist Studio edited-playlist rollback failed")
            false
        }
    }

    override fun onSaved(playlistId: String) = refresh(playlistId)
}
