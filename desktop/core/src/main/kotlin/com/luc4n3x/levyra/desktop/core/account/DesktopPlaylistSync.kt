package com.luc4n3x.levyra.desktop.core.account

import com.luc4n3x.levyra.data.youtubemusic.KeyedMutex
import com.luc4n3x.levyra.data.youtubemusic.PlaylistSyncBaseline
import com.luc4n3x.levyra.data.youtubemusic.PlaylistSyncLocalTrack
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicAccountHttpException
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicEditRejected
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicLibraryLimit
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicOwnedPlaylist
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicOwnedPlaylistParser
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicPlaylistSnapshot
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicProtocolException
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicSessionExpired
import com.luc4n3x.levyra.data.youtubemusic.applyRemotePlan
import com.luc4n3x.levyra.data.youtubemusic.canonicalYoutubeMusicPlaylistId
import com.luc4n3x.levyra.data.youtubemusic.collectYoutubeMusicPages
import com.luc4n3x.levyra.data.youtubemusic.planYoutubeMusicPlaylistSync
import com.luc4n3x.levyra.data.youtubemusic.playlistSnapshot
import com.luc4n3x.levyra.data.youtubemusic.settlePlaylistSync
import com.luc4n3x.levyra.data.youtubemusic.settleRemoteOnlySync
import com.luc4n3x.levyra.data.youtubemusic.youtubeMusicPlaylistUrl
import com.luc4n3x.levyra.desktop.core.catalog.YoutubeMusicArtistPage
import com.luc4n3x.levyra.desktop.core.model.videoId
import com.luc4n3x.levyra.desktop.core.storage.LibraryStore
import com.luc4n3x.levyra.desktop.core.storage.LocalPlaylist
import com.luc4n3x.levyra.desktop.core.storage.PlaylistSyncLink
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

enum class DesktopPlaylistSyncError { SIGN_IN, ACCOUNT, MISSING, ALREADY_LINKED, NETWORK, SERVICE }

data class DesktopPlaylistSyncSummary(
    val addedLocally: Int = 0,
    val removedLocally: Int = 0,
    val addedRemotely: Int = 0,
    val removedRemotely: Int = 0,
    val renamed: Boolean = false,
    val waiting: Int = 0,
    val rejected: Int = 0
)

sealed interface DesktopPlaylistSyncOutcome {
    data class Synced(val summary: DesktopPlaylistSyncSummary) : DesktopPlaylistSyncOutcome
    data class Failed(val error: DesktopPlaylistSyncError) : DesktopPlaylistSyncOutcome
}

data class DesktopOwnedPlaylist(val playlistId: String, val title: String, val subtitle: String, val thumbnailUrl: String = "")

class DesktopPlaylistSync(
    private val accounts: DesktopYoutubeMusicAccounts,
    private val library: LibraryStore,
    private val language: () -> String,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    private val locks = KeyedMutex()
    private val running = MutableStateFlow<Set<String>>(emptySet())
    private val failedAt = HashMap<String, Long>()

    val syncing: StateFlow<Set<String>> = running.asStateFlow()

    suspend fun ownedPlaylists(): List<DesktopOwnedPlaylist> = withContext(Dispatchers.IO) {
        val session = accounts.activeSession() ?: throw YoutubeMusicSessionExpired()
        val linked = library.current.playlistSyncs.filter { it.accountId == session.account.id }.mapTo(hashSetOf()) { it.remotePlaylistId }
        collectYoutubeMusicPages(key = YoutubeMusicOwnedPlaylist::playlistId) { continuation ->
            YoutubeMusicOwnedPlaylistParser.page(accounts.playlistClient.browse(session, LIBRARY_PLAYLISTS, language(), continuation))
        }.filter { it.playlistId !in linked }.map { DesktopOwnedPlaylist(it.playlistId, it.title, it.subtitle, it.thumbnailUrl) }
    }

    suspend fun createAndLink(playlistId: String, autoSync: Boolean): DesktopPlaylistSyncOutcome =
        link(playlistId, null, autoSync)

    suspend fun linkExisting(playlistId: String, remote: DesktopOwnedPlaylist, autoSync: Boolean): DesktopPlaylistSyncOutcome =
        link(playlistId, remote, autoSync)

    suspend fun importAndLink(remote: DesktopOwnedPlaylist): DesktopPlaylistSyncOutcome = withContext(Dispatchers.IO) {
        val session = try {
            accounts.activeSession()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            return@withContext DesktopPlaylistSyncOutcome.Failed(error.toSyncError())
        } ?: return@withContext DesktopPlaylistSyncOutcome.Failed(DesktopPlaylistSyncError.SIGN_IN)
        val remoteId = canonicalYoutubeMusicPlaylistId(remote.playlistId)
        library.current.playlistSyncs.firstOrNull { it.accountId == session.account.id && it.remotePlaylistId == remoteId }?.let {
            return@withContext sync(it.playlistId)
        }
        val playlistId = library.createPlaylist(remote.title.ifBlank { "YouTube Music" })
        val outcome = link(playlistId, remote, autoSync = true)
        if (link(playlistId) == null) library.deletePlaylist(playlistId)
        outcome
    }

    suspend fun sync(playlistId: String): DesktopPlaylistSyncOutcome =
        runSync(playlistId, automatic = false) ?: DesktopPlaylistSyncOutcome.Failed(DesktopPlaylistSyncError.MISSING)

    private suspend fun runSync(playlistId: String, automatic: Boolean): DesktopPlaylistSyncOutcome? = withContext(Dispatchers.IO) {
        val outcome = locks.withLock(playlistId) {
            if (automatic && link(playlistId)?.autoSync != true) null else guarded { syncLocked(playlistId) }
        } ?: return@withContext null
        synchronized(failedAt) {
            if (outcome is DesktopPlaylistSyncOutcome.Failed) failedAt[playlistId] = nowMillis() else failedAt.remove(playlistId)
        }
        outcome
    }

    suspend fun syncDue(remoteRefreshMs: Long = REMOTE_REFRESH_MS): Int = withContext(Dispatchers.IO) {
        val links = library.current.playlistSyncs.filter { it.autoSync }
        if (links.isEmpty()) return@withContext 0
        val accountId = try {
            accounts.activeSession()?.account?.id
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        } ?: return@withContext 0
        val now = nowMillis()
        var count = 0
        links.filter { it.accountId == accountId }.forEach { link ->
            val failed = synchronized(failedAt) { failedAt[link.playlistId] }
            if (failed != null && now - failed < RETRY_BACKOFF_MS) return@forEach
            val playlist = library.current.playlists.firstOrNull { it.id == link.playlistId } ?: return@forEach
            if (link.synced && playlist.updatedAt <= link.localUpdatedAt && now - link.lastSyncedAt < remoteRefreshMs) return@forEach
            if (runSync(link.playlistId, automatic = true) != null) count++
        }
        count
    }

    fun remoteUrl(link: PlaylistSyncLink): String = youtubeMusicPlaylistUrl(link.remotePlaylistId)

    fun errorOf(error: Exception): DesktopPlaylistSyncError = error.toSyncError()

    fun setAutoSync(playlistId: String, enabled: Boolean) {
        val link = link(playlistId) ?: return
        library.updateSyncLink(playlistId, link.accountId, link.remotePlaylistId) { it.copy(autoSync = enabled) }
    }

    suspend fun unlink(playlistId: String) {
        locks.withLock(playlistId) { library.removeSyncLink(playlistId) }
        synchronized(failedAt) { failedAt.remove(playlistId) }
    }

    private suspend fun link(playlistId: String, remote: DesktopOwnedPlaylist?, autoSync: Boolean): DesktopPlaylistSyncOutcome =
        withContext(Dispatchers.IO) {
            locks.withLock(playlistId) {
                guarded {
                    val session = accounts.activeSession() ?: return@guarded DesktopPlaylistSyncOutcome.Failed(DesktopPlaylistSyncError.SIGN_IN)
                    val playlist = playlist(playlistId) ?: return@guarded DesktopPlaylistSyncOutcome.Failed(DesktopPlaylistSyncError.MISSING)
                    val saved = withContext(NonCancellable) {
                        val (remoteId, remoteTitle) = if (remote == null) {
                            val title = playlist.name.trim().ifBlank { "Levyra" }.take(MAX_TITLE_LENGTH)
                            canonicalYoutubeMusicPlaylistId(accounts.playlistClient.createPlaylist(session, title)) to title
                        } else {
                            canonicalYoutubeMusicPlaylistId(remote.playlistId) to remote.title
                        }
                        library.saveSyncLink(
                            PlaylistSyncLink(
                                playlistId = playlistId,
                                accountId = session.account.id,
                                remotePlaylistId = remoteId,
                                localTitle = playlist.name.trim(),
                                remoteTitle = remoteTitle,
                                autoSync = autoSync
                            )
                        )
                    }
                    if (!saved) return@guarded DesktopPlaylistSyncOutcome.Failed(DesktopPlaylistSyncError.ALREADY_LINKED)
                    syncLocked(playlistId, createdEmpty = remote == null)
                }
            }
        }

    private suspend fun syncLocked(playlistId: String, createdEmpty: Boolean = false): DesktopPlaylistSyncOutcome {
        val link = link(playlistId) ?: return DesktopPlaylistSyncOutcome.Failed(DesktopPlaylistSyncError.MISSING)
        val session = accounts.activeSession() ?: return fail(link, DesktopPlaylistSyncError.SIGN_IN)
        if (session.account.id != link.accountId) return fail(link, DesktopPlaylistSyncError.ACCOUNT)
        val playlist = playlist(playlistId) ?: return DesktopPlaylistSyncOutcome.Failed(DesktopPlaylistSyncError.MISSING)
        val client = accounts.playlistClient
        running.update { it + playlistId }
        try {
            val local = playlist.tracks.map { PlaylistSyncLocalTrack(it.id, it.videoId) }
            val snapshot = try {
                if (createdEmpty) YoutubeMusicPlaylistSnapshot(link.remoteTitle, emptyList(), complete = true)
                else client.playlistSnapshot(session, link.remotePlaylistId, language())
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                return fail(link, error.toSyncError())
            }
            val baseline = if (link.synced) PlaylistSyncBaseline(link.localTitle, link.remoteTitle, link.videoIds.toSet()) else null
            val plan = planYoutubeMusicPlaylistSync(playlist.name, local, snapshot, baseline)
            return withContext(NonCancellable) {
                val remote = try {
                    client.applyRemotePlan(session, link.remotePlaylistId, plan)
                } catch (error: Exception) {
                    return@withContext fail(link, error.toSyncError())
                }
                val current = playlist(playlistId) ?: return@withContext DesktopPlaylistSyncOutcome.Failed(DesktopPlaylistSyncError.MISSING)
                if (current.updatedAt != playlist.updatedAt || current.name != playlist.name) {
                    val settled = settleRemoteOnlySync(plan, local, snapshot, remote, playlist.name, link.remoteTitle)
                    val syncedAt = nowMillis()
                    library.updateSyncLink(playlistId, link.accountId, link.remotePlaylistId) {
                        it.copy(
                            localTitle = settled.localTitle,
                            remoteTitle = settled.remoteTitle,
                            videoIds = settled.videoIds.toList(),
                            synced = true,
                            localUpdatedAt = playlist.updatedAt,
                            lastSyncedAt = syncedAt,
                            lastError = ""
                        )
                    }
                    return@withContext DesktopPlaylistSyncOutcome.Synced(
                        DesktopPlaylistSyncSummary(
                            addedRemotely = plan.addRemote.size - remote.rejectedAdds.size,
                            removedRemotely = plan.removeRemote.map { it.videoId }.distinct().size - remote.rejectedRemovals.size,
                            renamed = plan.renameRemote != null
                        )
                    )
                }
                val additions = plan.addLocal.mapNotNull { YoutubeMusicArtistPage.track(it.renderer, "", videos = false, includeMusicVideos = true) }
                library.removeTracks(playlistId, plan.removeLocal)
                library.addToPlaylist(playlistId, additions)
                plan.renameLocal?.let { library.renamePlaylist(playlistId, it) }
                val updated = playlist(playlistId) ?: return@withContext DesktopPlaylistSyncOutcome.Failed(DesktopPlaylistSyncError.MISSING)
                val present = updated.tracks.mapTo(hashSetOf()) { it.videoId }
                val addedIds = plan.addLocal.mapNotNullTo(hashSetOf()) { row -> row.videoId.takeIf { it in present } }
                val settled = settlePlaylistSync(plan, local, snapshot, remote, addedIds, playlist.name, link.remoteTitle)
                val syncedAt = nowMillis()
                library.updateSyncLink(playlistId, link.accountId, link.remotePlaylistId) {
                    it.copy(
                        localTitle = settled.localTitle,
                        remoteTitle = settled.remoteTitle,
                        videoIds = settled.videoIds.toList(),
                        synced = true,
                        localUpdatedAt = updated.updatedAt,
                        lastSyncedAt = syncedAt,
                        lastError = "",
                        unmatched = plan.unmatched,
                        keptLocal = plan.keptLocal
                    )
                }
                DesktopPlaylistSyncOutcome.Synced(
                    DesktopPlaylistSyncSummary(
                        addedLocally = additions.size,
                        removedLocally = plan.removeLocal.size,
                        addedRemotely = plan.addRemote.size - remote.rejectedAdds.size,
                        removedRemotely = plan.removeRemote.map { it.videoId }.distinct().size - remote.rejectedRemovals.size,
                        renamed = plan.renameLocal != null || plan.renameRemote != null,
                        waiting = plan.heldBack,
                        rejected = remote.rejectedAdds.size + remote.rejectedRemovals.size
                    )
                )
            }
        } finally {
            running.update { it - playlistId }
        }
    }

    private fun playlist(playlistId: String): LocalPlaylist? = library.current.playlists.firstOrNull { it.id == playlistId }

    private fun link(playlistId: String): PlaylistSyncLink? = library.current.playlistSyncs.firstOrNull { it.playlistId == playlistId }

    private fun fail(link: PlaylistSyncLink, error: DesktopPlaylistSyncError): DesktopPlaylistSyncOutcome {
        library.updateSyncLink(link.playlistId, link.accountId, link.remotePlaylistId) { it.copy(lastError = error.name) }
        return DesktopPlaylistSyncOutcome.Failed(error)
    }

    private suspend fun guarded(block: suspend () -> DesktopPlaylistSyncOutcome): DesktopPlaylistSyncOutcome = try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        DesktopPlaylistSyncOutcome.Failed(error.toSyncError())
    }

    private fun Exception.toSyncError(): DesktopPlaylistSyncError = when (this) {
        is YoutubeMusicSessionExpired -> DesktopPlaylistSyncError.SIGN_IN
        is YoutubeMusicAccountHttpException -> when (status) {
            401, 403 -> DesktopPlaylistSyncError.SIGN_IN
            404 -> DesktopPlaylistSyncError.MISSING
            else -> DesktopPlaylistSyncError.SERVICE
        }
        is YoutubeMusicProtocolException, is YoutubeMusicEditRejected, is YoutubeMusicLibraryLimit -> DesktopPlaylistSyncError.SERVICE
        is IOException -> DesktopPlaylistSyncError.NETWORK
        else -> DesktopPlaylistSyncError.SERVICE
    }

    private companion object {
        const val LIBRARY_PLAYLISTS = "FEmusic_liked_playlists"
        const val MAX_TITLE_LENGTH = 150
        const val REMOTE_REFRESH_MS = 15 * 60_000L
        const val RETRY_BACKOFF_MS = 5 * 60_000L
    }
}
