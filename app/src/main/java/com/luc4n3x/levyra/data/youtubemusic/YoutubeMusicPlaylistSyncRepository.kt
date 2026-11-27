package com.luc4n3x.levyra.data.youtubemusic

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import com.luc4n3x.levyra.data.PlaylistStore
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.local.PlaylistSyncEntity
import com.luc4n3x.levyra.data.playlistexport.youtubeVideoIdOrNull
import com.luc4n3x.levyra.domain.Playlist
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException

enum class PlaylistSyncError { SIGN_IN, ACCOUNT, MISSING, ALREADY_LINKED, NETWORK, SERVICE }

data class PlaylistSyncSummary(
    val addedLocally: Int = 0,
    val removedLocally: Int = 0,
    val addedRemotely: Int = 0,
    val removedRemotely: Int = 0,
    val renamed: Boolean = false,
    val unmatched: Int = 0,
    val keptLocal: Int = 0,
    val waiting: Int = 0,
    val rejected: Int = 0
) {
    val changedLocally: Boolean get() = addedLocally + removedLocally > 0 || renamed
}

sealed interface PlaylistSyncOutcome {
    data class Synced(val summary: PlaylistSyncSummary) : PlaylistSyncOutcome
    data class Failed(val error: PlaylistSyncError) : PlaylistSyncOutcome
}

internal sealed interface PlaylistSyncTarget {
    data object CreateNew : PlaylistSyncTarget
    data class Existing(val remotePlaylistId: String, val title: String) : PlaylistSyncTarget
}

internal class YoutubeMusicPlaylistSyncRepository(
    context: Context,
    private val playlists: PlaylistStore
) {
    private val appContext = context.applicationContext
    private val account by lazy { YoutubeMusicAccountRepository(appContext) }
    private val parser by lazy { YoutubeMusicRepository(appContext) }
    private val sessions get() = account.sessions
    private val client get() = account.client
    private val dao = LevyraDatabase.get(appContext).playlistSyncDao()
    private val locks = KeyedMutex()
    private val running = MutableStateFlow<Set<String>>(emptySet())
    private val failedAt = HashMap<String, Long>()

    val links: Flow<Map<String, PlaylistSyncEntity>> = dao.observeAll().map { rows -> rows.associateBy(PlaylistSyncEntity::playlistId) }
    val syncing: StateFlow<Set<String>> = running.asStateFlow()

    suspend fun activeAccount(): YoutubeMusicAccount? = withContext(Dispatchers.IO) {
        runCatching { sessions.load().active?.account }.onFailure { Timber.w(it, "youtube music session unavailable for sync") }.getOrNull()
    }

    suspend fun ownedPlaylists(language: String): List<YoutubeMusicLibraryEntry> = withContext(Dispatchers.IO) {
        val session = sessions.load().active ?: throw YoutubeMusicSessionExpired()
        val linked = dao.all().filter { it.accountId == session.account.id }.mapTo(hashSetOf()) { it.remotePlaylistId }
        collectYoutubeMusicPages(key = YoutubeMusicLibraryEntry::browseId) { continuation ->
            val page = account.page(session, YoutubeMusicCollection.Playlists, language, continuation = continuation)
            YoutubeMusicPage(
                page.entries.filter { it.owned && it.browseId !in YOUTUBE_MUSIC_SYSTEM_PLAYLISTS && canonicalYoutubeMusicPlaylistId(it.browseId) !in linked },
                page.continuation
            )
        }
    }

    suspend fun link(playlistId: String, target: PlaylistSyncTarget, autoSync: Boolean, language: String): PlaylistSyncOutcome =
        withContext(Dispatchers.IO) {
            locks.withLock(playlistId) {
                guarded(playlistId) {
                    val session = sessions.load().active ?: return@guarded PlaylistSyncOutcome.Failed(PlaylistSyncError.SIGN_IN)
                    val playlist = playlists.load(playlistId) ?: return@guarded PlaylistSyncOutcome.Failed(PlaylistSyncError.MISSING)
                    val claimed = withContext(NonCancellable) {
                        val (remoteId, remoteTitle) = when (target) {
                            PlaylistSyncTarget.CreateNew -> {
                                val title = playlist.name.trim().ifBlank { "Levyra" }
                                canonicalYoutubeMusicPlaylistId(client.createPlaylist(session, title.take(MAX_TITLE_LENGTH))) to title
                            }
                            is PlaylistSyncTarget.Existing -> canonicalYoutubeMusicPlaylistId(target.remotePlaylistId) to target.title.trim()
                        }
                        dao.claim(
                            PlaylistSyncEntity(
                                playlistId = playlistId,
                                accountId = session.account.id,
                                remotePlaylistId = remoteId,
                                localTitle = playlist.name.trim(),
                                remoteTitle = remoteTitle,
                                videoIds = "",
                                autoSync = autoSync,
                                synced = false,
                                localUpdatedAt = 0L,
                                lastSyncedAt = 0L,
                                lastError = "",
                                unmatched = 0,
                                keptLocal = 0
                            )
                        )
                    }
                    if (!claimed) return@guarded PlaylistSyncOutcome.Failed(PlaylistSyncError.ALREADY_LINKED)
                    syncLocked(playlistId, language, createdEmpty = target == PlaylistSyncTarget.CreateNew)
                }
            }
        }

    suspend fun linkRemote(entry: YoutubeMusicLibraryEntry, language: String): Pair<String?, PlaylistSyncOutcome> = withContext(Dispatchers.IO) {
        try {
            val session = sessions.load().active ?: return@withContext null to PlaylistSyncOutcome.Failed(PlaylistSyncError.SIGN_IN)
            val remoteId = canonicalYoutubeMusicPlaylistId(entry.browseId)
            dao.findByRemote(session.account.id, remoteId)?.let { existing ->
                return@withContext existing.playlistId to sync(existing.playlistId, language)
            }
            val playlistId = youtubeMusicImportPlaylistId(session.account.id, entry.browseId)
            if (playlists.load(playlistId) == null) playlists.createEmpty(playlistId, entry.title.trim().ifBlank { "YouTube Music" })
            playlistId to link(playlistId, PlaylistSyncTarget.Existing(remoteId, entry.title), autoSync = true, language = language)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.w(error, "youtube music playlist link failed")
            null to PlaylistSyncOutcome.Failed(error.toSyncError())
        }
    }

    suspend fun sync(playlistId: String, language: String): PlaylistSyncOutcome =
        runSync(playlistId, language, automatic = false) ?: PlaylistSyncOutcome.Failed(PlaylistSyncError.MISSING)

    private suspend fun runSync(playlistId: String, language: String, automatic: Boolean): PlaylistSyncOutcome? = withContext(Dispatchers.IO) {
        val outcome = locks.withLock(playlistId) {
            if (automatic && dao.get(playlistId)?.autoSync != true) null else guarded(playlistId) { syncLocked(playlistId, language) }
        } ?: return@withContext null
        synchronized(failedAt) {
            if (outcome is PlaylistSyncOutcome.Failed) failedAt[playlistId] = System.currentTimeMillis() else failedAt.remove(playlistId)
        }
        outcome
    }

    suspend fun syncDue(language: String, remoteRefreshMs: Long, now: Long = System.currentTimeMillis()): List<String> = withContext(Dispatchers.IO) {
        val links = dao.all().filter { it.autoSync }
        if (links.isEmpty()) return@withContext emptyList()
        val account = activeAccount() ?: return@withContext emptyList()
        val changed = mutableListOf<String>()
        links.filter { it.accountId == account.id }.forEach { link ->
            val failed = synchronized(failedAt) { failedAt[link.playlistId] }
            if (failed != null && now - failed < RETRY_BACKOFF_MS) return@forEach
            val playlist = playlists.load(link.playlistId) ?: return@forEach
            val due = !link.synced || playlist.updatedAt > link.localUpdatedAt || now - link.lastSyncedAt >= remoteRefreshMs
            if (!due) return@forEach
            val outcome = runSync(link.playlistId, language, automatic = true)
            if (outcome is PlaylistSyncOutcome.Synced && outcome.summary.changedLocally) changed += link.playlistId
        }
        changed
    }

    suspend fun setAutoSync(playlistId: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        locks.withLock(playlistId) { dao.setAutoSync(playlistId, enabled) }
    }

    suspend fun unlink(playlistId: String) = withContext(Dispatchers.IO) {
        locks.withLock(playlistId) { dao.delete(playlistId) }
        synchronized(failedAt) { failedAt.remove(playlistId) }
    }

    suspend fun <T> withPlaylistLock(playlistId: String, block: suspend () -> T): T = locks.withLock(playlistId) { block() }

    private suspend fun syncLocked(playlistId: String, language: String, createdEmpty: Boolean = false): PlaylistSyncOutcome {
        val link = dao.get(playlistId) ?: return PlaylistSyncOutcome.Failed(PlaylistSyncError.MISSING)
        val session = sessions.load().active ?: return fail(link, PlaylistSyncError.SIGN_IN)
        if (session.account.id != link.accountId) return fail(link, PlaylistSyncError.ACCOUNT)
        val playlist = playlists.load(playlistId) ?: return PlaylistSyncOutcome.Failed(PlaylistSyncError.MISSING)
        running.update { it + playlistId }
        try {
            val local = playlist.syncTracks()
            val snapshot = try {
                if (createdEmpty) YoutubeMusicPlaylistSnapshot(link.remoteTitle, emptyList(), complete = true)
                else client.playlistSnapshot(session, link.remotePlaylistId, language)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.w(error, "youtube music playlist snapshot failed")
                return fail(link, error.toSyncError())
            }
            val baseline = if (link.synced) {
                PlaylistSyncBaseline(link.localTitle, link.remoteTitle, decodePlaylistSyncVideoIds(link.videoIds))
            } else {
                null
            }
            val plan = planYoutubeMusicPlaylistSync(playlist.name, local, snapshot, baseline)
            return withContext(NonCancellable) {
                val remote = try {
                    client.applyRemotePlan(session, link.remotePlaylistId, plan)
                } catch (error: Exception) {
                    Timber.w(error, "youtube music playlist update failed")
                    return@withContext fail(link, error.toSyncError())
                }
                val current = playlists.load(playlistId) ?: return@withContext PlaylistSyncOutcome.Failed(PlaylistSyncError.MISSING)
                if (current.updatedAt != playlist.updatedAt || current.name != playlist.name) {
                    dao.recordError(playlistId, link.accountId, link.remotePlaylistId, "")
                    return@withContext PlaylistSyncOutcome.Synced(
                        PlaylistSyncSummary(
                            addedRemotely = plan.addRemote.size - remote.rejectedAdds.size,
                            removedRemotely = plan.removeRemote.map { it.videoId }.distinct().size - remote.rejectedRemovals.size,
                            renamed = plan.renameRemote != null
                        )
                    )
                }
                val browseId = "VL${link.remotePlaylistId}"
                val additions = plan.addLocal.mapNotNull { row ->
                    parser.parsePlaylistRenderer(row.renderer, browseId)?.copy(streamUrl = "", videoStreamUrl = "")
                }
                if (plan.removeLocal.isNotEmpty()) playlists.removeTracks(playlistId, plan.removeLocal)
                if (additions.isNotEmpty()) playlists.addTracks(playlistId, additions)
                plan.renameLocal?.let { playlists.rename(playlistId, it) }
                val updated = playlists.load(playlistId) ?: return@withContext PlaylistSyncOutcome.Failed(PlaylistSyncError.MISSING)
                val present = updated.syncTracks().mapNotNullTo(hashSetOf()) { it.videoId }
                val addedIds = plan.addLocal.mapNotNullTo(hashSetOf()) { row -> row.videoId.takeIf { it in present } }
                val settled = settlePlaylistSync(plan, local, snapshot, remote, addedIds, playlist.name, link.remoteTitle)
                dao.recordSync(
                    playlistId = playlistId,
                    accountId = link.accountId,
                    remotePlaylistId = link.remotePlaylistId,
                    localTitle = settled.localTitle,
                    remoteTitle = settled.remoteTitle,
                    videoIds = encodePlaylistSyncVideoIds(settled.videoIds),
                    localUpdatedAt = updated.updatedAt,
                    lastSyncedAt = System.currentTimeMillis(),
                    unmatched = plan.unmatched,
                    keptLocal = plan.keptLocal
                )
                PlaylistSyncOutcome.Synced(
                    PlaylistSyncSummary(
                        addedLocally = additions.size,
                        removedLocally = plan.removeLocal.size,
                        addedRemotely = plan.addRemote.size - remote.rejectedAdds.size,
                        removedRemotely = plan.removeRemote.map { it.videoId }.distinct().size - remote.rejectedRemovals.size,
                        renamed = plan.renameLocal != null || plan.renameRemote != null,
                        unmatched = plan.unmatched,
                        keptLocal = plan.keptLocal,
                        waiting = plan.heldBack,
                        rejected = remote.rejectedAdds.size + remote.rejectedRemovals.size
                    )
                )
            }
        } finally {
            running.update { it - playlistId }
        }
    }

    private suspend fun fail(link: PlaylistSyncEntity, error: PlaylistSyncError): PlaylistSyncOutcome {
        withContext(NonCancellable) { dao.recordError(link.playlistId, link.accountId, link.remotePlaylistId, error.name) }
        return PlaylistSyncOutcome.Failed(error)
    }

    private suspend fun guarded(playlistId: String, block: suspend () -> PlaylistSyncOutcome): PlaylistSyncOutcome = try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (error: SQLiteConstraintException) {
        Timber.w(error, "playlist %s disappeared during youtube music sync", playlistId)
        PlaylistSyncOutcome.Failed(PlaylistSyncError.MISSING)
    } catch (error: Exception) {
        Timber.w(error, "youtube music playlist sync failed")
        PlaylistSyncOutcome.Failed(error.toSyncError())
    }

    private fun Playlist.syncTracks(): List<PlaylistSyncLocalTrack> =
        tracks.map { PlaylistSyncLocalTrack(it.id, it.youtubeVideoIdOrNull()) } +
            albums.flatMap { album ->
                album.tracks.map { PlaylistSyncLocalTrack("album:${album.entryId}:${it.id}", it.youtubeVideoIdOrNull(), removable = false) }
            }

    private fun Exception.toSyncError(): PlaylistSyncError = when (this) {
        is YoutubeMusicSessionExpired -> PlaylistSyncError.SIGN_IN
        is YoutubeMusicAccountHttpException -> when (status) {
            401, 403 -> PlaylistSyncError.SIGN_IN
            404 -> PlaylistSyncError.MISSING
            else -> PlaylistSyncError.SERVICE
        }
        is YoutubeMusicProtocolException, is YoutubeMusicEditRejected, is YoutubeMusicLibraryLimit -> PlaylistSyncError.SERVICE
        is IOException -> PlaylistSyncError.NETWORK
        else -> PlaylistSyncError.SERVICE
    }

    private companion object {
        const val MAX_TITLE_LENGTH = 150
        const val RETRY_BACKOFF_MS = 5 * 60_000L
    }
}

internal fun youtubeMusicImportPlaylistId(accountId: String, browseId: String): String =
    "ytm-" + java.util.UUID.nameUUIDFromBytes("$accountId:$browseId".toByteArray(Charsets.UTF_8))
