package com.luc4n3x.levyra.data.youtubemusic

import android.content.Context
import com.luc4n3x.levyra.data.PlaylistStore
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.local.PlaylistSyncEntity
import com.luc4n3x.levyra.data.playlistexport.planRemotePlaylistExport
import com.luc4n3x.levyra.data.playlistexport.youtubeVideoIdOrNull
import com.luc4n3x.levyra.data.spotify.SpotifyYouTubeMatchCache
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.items
import com.luc4n3x.levyra.domain.playableTracks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.util.UUID

enum class PlaylistSyncErrorKind {
    SIGN_IN,
    ACCOUNT_MISMATCH,
    NOT_FOUND,
    NETWORK,
    SERVICE;

    companion object {
        fun fromStorage(raw: String): PlaylistSyncErrorKind? =
            entries.firstOrNull { it.name == raw }
    }
}

data class PlaylistSyncSummary(
    val addedLocally: Int = 0,
    val removedLocally: Int = 0,
    val addedRemotely: Int = 0,
    val removedRemotely: Int = 0,
    val renamedLocally: Boolean = false,
    val renamedRemotely: Boolean = false,
    val unmatchedCount: Int = 0
)

sealed interface PlaylistSyncOutcome {
    data class Synced(val state: PlaylistSyncEntity, val summary: PlaylistSyncSummary) : PlaylistSyncOutcome
    data class Failed(val error: PlaylistSyncErrorKind, val state: PlaylistSyncEntity? = null) : PlaylistSyncOutcome
}

internal class YoutubeMusicPlaylistSyncRepository(
    context: Context,
    private val playlistStore: PlaylistStore = PlaylistStore(context.applicationContext),
    private val accountRepository: YoutubeMusicAccountRepository = YoutubeMusicAccountRepository(context.applicationContext),
    private val resolveYoutubeVideoId: suspend (Track, String) -> String? = defaultYoutubeVideoIdResolver(context.applicationContext),
    private val onLocalPlaylistChanged: (String) -> Unit = {}
) {
    private val database = LevyraDatabase.get(context.applicationContext)
    private val syncDao = database.playlistSyncDao()
    private val locks = mutableMapOf<String, Mutex>()
    private val remoteLocks = mutableMapOf<String, Mutex>()
    private val bulkSyncMutex = Mutex()
    private val _activeSyncIds = MutableStateFlow<Set<String>>(emptySet())
    private val _activeRemoteBrowseIds = MutableStateFlow<Set<String>>(emptySet())

    val activeSyncIds: StateFlow<Set<String>> = _activeSyncIds.asStateFlow()
    val activeRemoteBrowseIds: StateFlow<Set<String>> = _activeRemoteBrowseIds.asStateFlow()

    val syncStates: Flow<Map<String, PlaylistSyncEntity>> = syncDao.observeAll().map { rows ->
        rows.associateBy(PlaylistSyncEntity::playlistId)
    }

    private fun lockFor(playlistId: String): Mutex = synchronized(locks) {
        locks.getOrPut(playlistId) { Mutex() }
    }

    private fun remoteLockFor(canonicalRemoteId: String): Mutex = synchronized(remoteLocks) {
        remoteLocks.getOrPut(canonicalRemoteId) { Mutex() }
    }

    suspend fun ownedRemotePlaylists(language: String): List<YoutubeMusicLibraryEntry> = withContext(Dispatchers.IO) {
        val session = accountRepository.sessions.load().active ?: throw YoutubeMusicSessionExpired()
        accountRepository.ownedPlaylists(session, language)
    }

    suspend fun currentAccountInfo(): YoutubeMusicAccount? = withContext(Dispatchers.IO) {
        runCatching { accountRepository.sessions.load().active?.account }.getOrNull()
    }

    suspend fun createAndLink(
        playlistId: String,
        autoSync: Boolean,
        language: String
    ): PlaylistSyncOutcome = withContext(Dispatchers.IO) {
        lockFor(playlistId).withLock {
            markActive(playlistId, active = true)
            try {
                val session = accountRepository.sessions.load().active
                    ?: return@withLock PlaylistSyncOutcome.Failed(PlaylistSyncErrorKind.SIGN_IN)
                val playlist = playlistStore.load(playlistId)
                    ?: return@withLock PlaylistSyncOutcome.Failed(PlaylistSyncErrorKind.NOT_FOUND)
                val cleanTitle = playlist.name.trim().ifBlank { "Playlist" }.take(MAX_TITLE_LENGTH)
                val directTrackIds = playlist.tracks.mapTo(hashSetOf()) { it.id }
                val resolution = resolveMissingYoutubeIds(
                    playlistId = playlistId,
                    directTrackIds = directTrackIds,
                    tracks = playlist.items().flatMap { it.playableTracks() },
                    language = language
                )
                val exportPlan = planRemotePlaylistExport(resolution.tracks)
                val createdId = try {
                    canonicalYoutubePlaylistId(accountRepository.client.createPlaylist(session, cleanTitle))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Timber.w(error, "Playlist sync creation failed")
                    return@withLock PlaylistSyncOutcome.Failed(error.toSyncErrorKind())
                }

                markRemoteActive(createdId, active = true)
                try {
                    withContext(NonCancellable) {
                        val now = System.currentTimeMillis()
                        val initialLink = PlaylistSyncEntity(
                            playlistId = playlistId,
                            accountId = session.account.id,
                            remotePlaylistId = createdId,
                            remoteTitle = cleanTitle,
                            syncedVideoIds = "",
                            autoSync = autoSync,
                            lastSyncedAt = 0L,
                            lastSyncAttemptAt = now,
                            lastSyncError = "",
                            lastUnmatchedCount = exportPlan.unmatched.size
                        )
                        syncDao.upsert(initialLink)

                        try {
                            exportPlan.videoIds.chunked(BATCH_SIZE).forEach { batch ->
                                accountRepository.client.addToPlaylist(session, createdId, batch)
                            }
                            val synced = initialLink.copy(
                                syncedVideoIds = encodeSyncedVideoIds(exportPlan.videoIds),
                                lastSyncedAt = System.currentTimeMillis(),
                                lastSyncAttemptAt = System.currentTimeMillis(),
                                lastSyncError = ""
                            )
                            syncDao.upsert(synced)
                            if (resolution.updatedLocalIds) onLocalPlaylistChanged(playlistId)
                            PlaylistSyncOutcome.Synced(
                                state = synced,
                                summary = PlaylistSyncSummary(
                                    addedRemotely = exportPlan.videoIds.size,
                                    unmatchedCount = exportPlan.unmatched.size
                                )
                            )
                        } catch (error: Exception) {
                            Timber.w(error, "Initial upload for synced playlist failed")
                            val kind = error.toSyncErrorKind()
                            val failed = initialLink.copy(
                                lastSyncAttemptAt = System.currentTimeMillis(),
                                lastSyncError = kind.name
                            )
                            syncDao.upsert(failed)
                            if (resolution.updatedLocalIds) onLocalPlaylistChanged(playlistId)
                            PlaylistSyncOutcome.Failed(kind, failed)
                        }
                    }
                } finally {
                    markRemoteActive(createdId, active = false)
                }
            } finally {
                markActive(playlistId, active = false)
            }
        }
    }

    suspend fun linkExisting(
        playlistId: String,
        remotePlaylistId: String,
        remoteTitle: String,
        autoSync: Boolean,
        language: String
    ): PlaylistSyncOutcome = withContext(Dispatchers.IO) {
        lockFor(playlistId).withLock {
            val session = accountRepository.sessions.load().active
                ?: return@withLock PlaylistSyncOutcome.Failed(PlaylistSyncErrorKind.SIGN_IN)
            val canonicalRemoteId = canonicalYoutubePlaylistId(remotePlaylistId)
            val existingEntity = syncDao.get(playlistId)
            val initial = PlaylistSyncEntity(
                playlistId = playlistId,
                accountId = session.account.id,
                remotePlaylistId = canonicalRemoteId,
                remoteTitle = remoteTitle.trim().ifBlank { existingEntity?.remoteTitle.orEmpty() },
                syncedVideoIds = "",
                autoSync = autoSync,
                lastSyncedAt = 0L,
                lastSyncAttemptAt = 0L,
                lastSyncError = "",
                lastUnmatchedCount = 0
            )
            withContext(NonCancellable) {
                syncDao.upsert(initial)
            }
            syncUnlocked(playlistId = playlistId, language = language, onlyIfAutoSync = false)
        }
    }

    suspend fun sync(
        playlistId: String,
        language: String,
        onlyIfAutoSync: Boolean = false
    ): PlaylistSyncOutcome = withContext(Dispatchers.IO) {
        lockFor(playlistId).withLock {
            syncUnlocked(playlistId = playlistId, language = language, onlyIfAutoSync = onlyIfAutoSync)
        }
    }

    suspend fun syncAllAutoSync(language: String): List<Pair<String, PlaylistSyncOutcome>> = withContext(Dispatchers.IO) {
        bulkSyncMutex.withLock {
            val session = accountRepository.sessions.load().active ?: return@withLock emptyList()
            val targets = syncDao.all().filter { it.autoSync && it.accountId == session.account.id }
            if (targets.isEmpty()) return@withLock emptyList()
            targets.map { entity ->
                entity.playlistId to sync(entity.playlistId, language, onlyIfAutoSync = true)
            }
        }
    }

    suspend fun linkAndSyncRemoteEntry(
        entry: YoutubeMusicLibraryEntry,
        language: String
    ): Pair<String?, PlaylistSyncOutcome> = withContext(Dispatchers.IO) {
        val canonicalRemoteId = canonicalYoutubePlaylistId(entry.browseId)
        remoteLockFor(canonicalRemoteId).withLock {
            markRemoteActive(canonicalRemoteId, active = true)
            try {
                val session = accountRepository.sessions.load().active
                    ?: return@withLock null to PlaylistSyncOutcome.Failed(PlaylistSyncErrorKind.SIGN_IN)
                val existingLink = syncDao.findByRemote(session.account.id, canonicalRemoteId)
                if (existingLink != null && playlistStore.load(existingLink.playlistId) != null) {
                    return@withLock existingLink.playlistId to sync(existingLink.playlistId, language)
                }
                val stableId = "ytm-" + UUID.nameUUIDFromBytes("${session.account.id}:${entry.browseId}".toByteArray(Charsets.UTF_8))
                val targetPlaylist = playlistStore.load(stableId) ?: run {
                    val remoteTracks = accountRepository.fullTracks(
                        session = session,
                        collection = YoutubeMusicCollection.Playlists,
                        browseId = entry.browseId,
                        language = language
                    )
                    val cleanTitle = entry.title.trim().ifBlank { "YouTube Music" }
                    if (remoteTracks.isNotEmpty()) {
                        playlistStore.createWithTracks(cleanTitle, remoteTracks, playlistId = stableId)
                    } else {
                        playlistStore.createWithTracksOrEmpty(stableId, cleanTitle)
                    }
                }
                onLocalPlaylistChanged(targetPlaylist.id)
                val outcome = linkExisting(
                    playlistId = targetPlaylist.id,
                    remotePlaylistId = canonicalRemoteId,
                    remoteTitle = entry.title,
                    autoSync = true,
                    language = language
                )
                targetPlaylist.id to outcome
            } finally {
                markRemoteActive(canonicalRemoteId, active = false)
            }
        }
    }

    suspend fun setAutoSync(playlistId: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        lockFor(playlistId).withLock {
            syncDao.setAutoSync(playlistId, enabled)
        }
    }

    suspend fun unlink(playlistId: String) = withContext(Dispatchers.IO) {
        lockFor(playlistId).withLock {
            syncDao.delete(playlistId)
        }
    }

    private suspend fun syncUnlocked(
        playlistId: String,
        language: String,
        onlyIfAutoSync: Boolean
    ): PlaylistSyncOutcome {
        val entity = syncDao.get(playlistId)
            ?: return PlaylistSyncOutcome.Failed(PlaylistSyncErrorKind.NOT_FOUND)
        if (onlyIfAutoSync && !entity.autoSync) {
            return PlaylistSyncOutcome.Synced(entity, PlaylistSyncSummary(unmatchedCount = entity.lastUnmatchedCount))
        }
        val canonicalRemoteId = canonicalYoutubePlaylistId(entity.remotePlaylistId)
        markActive(playlistId, active = true)
        markRemoteActive(canonicalRemoteId, active = true)
        try {
            val attemptAt = System.currentTimeMillis()
            val session = accountRepository.sessions.load().active
            if (session == null) {
                val updated = entity.copy(
                    lastSyncAttemptAt = attemptAt,
                    lastSyncError = PlaylistSyncErrorKind.SIGN_IN.name
                )
                syncDao.upsert(updated)
                return PlaylistSyncOutcome.Failed(PlaylistSyncErrorKind.SIGN_IN, updated)
            }
            if (session.account.id != entity.accountId) {
                val updated = entity.copy(
                    lastSyncAttemptAt = attemptAt,
                    lastSyncError = PlaylistSyncErrorKind.ACCOUNT_MISMATCH.name
                )
                syncDao.upsert(updated)
                return PlaylistSyncOutcome.Failed(PlaylistSyncErrorKind.ACCOUNT_MISMATCH, updated)
            }

            val playlist = playlistStore.load(playlistId)
                ?: return PlaylistSyncOutcome.Failed(PlaylistSyncErrorKind.NOT_FOUND)

            val directTrackIds = playlist.tracks.mapTo(hashSetOf()) { it.id }
            val resolution = resolveMissingYoutubeIds(
                playlistId = playlistId,
                directTrackIds = directTrackIds,
                tracks = playlist.items().flatMap { it.playableTracks() },
                language = language
            )

            val remote = try {
                accountRepository.fullPlaylistSnapshot(session, entity.remotePlaylistId, language)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Remote playlist snapshot fetch failed")
                val kind = error.toSyncErrorKind()
                val updated = entity.copy(lastSyncAttemptAt = attemptAt, lastSyncError = kind.name)
                withContext(NonCancellable) {
                    syncDao.upsert(updated)
                }
                return PlaylistSyncOutcome.Failed(kind, updated)
            }

            val baseline = if (entity.lastSyncedAt > 0L) {
                PlaylistSyncBaseline(
                    remoteTitle = entity.remoteTitle,
                    videoIds = decodeSyncedVideoIds(entity.syncedVideoIds)
                )
            } else {
                null
            }

            val plan = planYoutubeMusicTwoWaySync(
                localName = playlist.name,
                localTracks = resolution.tracks,
                remoteTitle = remote.title,
                remoteItems = remote.items,
                baseline = baseline
            )

            return withContext(NonCancellable) {
                try {
                    plan.renameRemoteTitle?.let { nextTitle ->
                        accountRepository.client.renamePlaylist(session, entity.remotePlaylistId, nextTitle)
                    }
                    plan.remoteItemsToRemove.chunked(BATCH_SIZE).forEach { batch ->
                        accountRepository.client.removeFromPlaylist(session, entity.remotePlaylistId, batch)
                    }
                    plan.videoIdsToAddRemotely.chunked(BATCH_SIZE).forEach { batch ->
                        accountRepository.client.addToPlaylist(session, entity.remotePlaylistId, batch)
                    }
                } catch (error: Exception) {
                    Timber.w(error, "Remote playlist sync update failed")
                    val kind = error.toSyncErrorKind()
                    val updated = entity.copy(lastSyncAttemptAt = attemptAt, lastSyncError = kind.name)
                    syncDao.upsert(updated)
                    if (resolution.updatedLocalIds) onLocalPlaylistChanged(playlistId)
                    return@withContext PlaylistSyncOutcome.Failed(kind, updated)
                }

                val removableLocalIds = plan.localTrackIdsToRemove.filterTo(LinkedHashSet()) { it in directTrackIds }
                if (removableLocalIds.isNotEmpty()) {
                    playlistStore.removeTracks(playlistId, removableLocalIds)
                }
                if (plan.tracksToAddLocally.isNotEmpty()) {
                    playlistStore.addTracks(playlistId, plan.tracksToAddLocally)
                }
                plan.renameLocalTitle?.let { nextTitle ->
                    playlistStore.rename(playlistId, nextTitle)
                }

                val albumOnlyRemovedVideoIds = if (removableLocalIds.size < plan.localTrackIdsToRemove.size) {
                    val albumOnlyIds = plan.localTrackIdsToRemove - removableLocalIds
                    resolution.tracks
                        .asSequence()
                        .filter { it.id in albumOnlyIds }
                        .mapNotNull { it.youtubeVideoIdOrNull() }
                        .toList()
                } else {
                    emptyList()
                }
                val finalBaselineIds = (plan.newBaselineVideoIds + albumOnlyRemovedVideoIds).distinct()

                val completedAt = System.currentTimeMillis()
                val updated = entity.copy(
                    remoteTitle = plan.newBaselineTitle,
                    syncedVideoIds = encodeSyncedVideoIds(finalBaselineIds),
                    lastSyncedAt = completedAt,
                    lastSyncAttemptAt = completedAt,
                    lastSyncError = "",
                    lastUnmatchedCount = plan.unmatchedLocalCount
                )
                syncDao.upsert(updated)

                val localMutated = resolution.updatedLocalIds ||
                    removableLocalIds.isNotEmpty() ||
                    plan.tracksToAddLocally.isNotEmpty() ||
                    plan.renameLocalTitle != null
                if (localMutated) onLocalPlaylistChanged(playlistId)

                PlaylistSyncOutcome.Synced(
                    state = updated,
                    summary = PlaylistSyncSummary(
                        addedLocally = plan.tracksToAddLocally.size,
                        removedLocally = removableLocalIds.size,
                        addedRemotely = plan.videoIdsToAddRemotely.size,
                        removedRemotely = plan.remoteItemsToRemove.size,
                        renamedLocally = plan.renameLocalTitle != null,
                        renamedRemotely = plan.renameRemoteTitle != null,
                        unmatchedCount = plan.unmatchedLocalCount
                    )
                )
            }
        } finally {
            markRemoteActive(canonicalRemoteId, active = false)
            markActive(playlistId, active = false)
        }
    }

    private suspend fun resolveMissingYoutubeIds(
        playlistId: String,
        directTrackIds: Set<String>,
        tracks: List<Track>,
        language: String
    ): TrackResolutionResult {
        val candidates = tracks.filter { track ->
            track.youtubeVideoIdOrNull() == null && !isLocalOnlyTrack(track) && track.title.isNotBlank()
        }
        if (candidates.isEmpty()) return TrackResolutionResult(tracks = tracks, updatedLocalIds = false)

        val semaphore = Semaphore(MATCH_RESOLUTION_CONCURRENCY)
        val resolvedByTrackId = coroutineScope {
            candidates
                .distinctBy { it.id }
                .map { track ->
                    async {
                        semaphore.withPermit {
                            val videoId = try {
                                resolveYoutubeVideoId(track, language)?.trim()?.takeIf(YOUTUBE_VIDEO_ID::matches)
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (error: Exception) {
                                Timber.w(error, "YouTube track match resolution failed for %s", track.id)
                                null
                            }
                            videoId?.let { track.id to it }
                        }
                    }
                }
                .awaitAll()
                .filterNotNull()
                .toMap()
        }
        if (resolvedByTrackId.isEmpty()) return TrackResolutionResult(tracks = tracks, updatedLocalIds = false)

        var persistedAny = false
        val updatedTracks = tracks.map { track ->
            val videoId = resolvedByTrackId[track.id] ?: return@map track
            val updated = track.copy(
                audioVideoId = videoId,
                videoUrl = track.videoUrl.ifBlank { "https://www.youtube.com/watch?v=$videoId" }
            )
            if (track.id in directTrackIds) {
                val replaced = withContext(NonCancellable) {
                    playlistStore.replaceTrack(playlistId, track.id, updated)
                }
                if (replaced) persistedAny = true
            }
            updated
        }
        return TrackResolutionResult(tracks = updatedTracks, updatedLocalIds = persistedAny)
    }

    private fun isLocalOnlyTrack(track: Track): Boolean {
        val id = track.id.trim()
        val stream = track.streamUrl.trim()
        return id.startsWith("local:", ignoreCase = true) ||
            id.startsWith("content:", ignoreCase = true) ||
            id.startsWith("file:", ignoreCase = true) ||
            (track.videoUrl.isBlank() && (
                stream.startsWith("content:", ignoreCase = true) ||
                    stream.startsWith("file:", ignoreCase = true)
                ))
    }

    private fun markActive(playlistId: String, active: Boolean) {
        _activeSyncIds.update { current ->
            if (active) current + playlistId else current - playlistId
        }
    }

    private fun markRemoteActive(canonicalRemoteId: String, active: Boolean) {
        if (canonicalRemoteId.isBlank()) return
        _activeRemoteBrowseIds.update { current ->
            if (active) current + canonicalRemoteId else current - canonicalRemoteId
        }
    }

    private fun Exception.toSyncErrorKind(): PlaylistSyncErrorKind = when (this) {
        is YoutubeMusicSessionExpired -> PlaylistSyncErrorKind.SIGN_IN
        is YoutubeMusicPlaylistNotFoundException -> PlaylistSyncErrorKind.NOT_FOUND
        is YoutubeMusicAccountHttpException -> when (status) {
            401, 403 -> PlaylistSyncErrorKind.SIGN_IN
            404 -> PlaylistSyncErrorKind.NOT_FOUND
            else -> PlaylistSyncErrorKind.SERVICE
        }
        is YoutubeMusicProtocolException, is YoutubeMusicLibraryLimit -> PlaylistSyncErrorKind.SERVICE
        is IOException -> PlaylistSyncErrorKind.NETWORK
        else -> PlaylistSyncErrorKind.SERVICE
    }

    private data class TrackResolutionResult(
        val tracks: List<Track>,
        val updatedLocalIds: Boolean
    )

    private companion object {
        const val BATCH_SIZE = 50
        const val MAX_TITLE_LENGTH = 150
        const val MATCH_RESOLUTION_CONCURRENCY = 3
        val YOUTUBE_VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")

        fun defaultYoutubeVideoIdResolver(appContext: Context): suspend (Track, String) -> String? {
            val repository = YoutubeMusicRepository(appContext)
            val cache = SpotifyYouTubeMatchCache.get(appContext)
            return { track, language ->
                val isSpotify = track.id.startsWith("spotify:", ignoreCase = true)
                val cached = if (isSpotify) {
                    cache.get(track.id)?.videoId?.trim()?.takeIf(YOUTUBE_VIDEO_ID::matches)
                } else {
                    null
                }
                if (cached != null) {
                    cached
                } else {
                    val matched = repository.searchSongMatch(track.title, track.artist, language)
                    val videoId = matched?.youtubeVideoIdOrNull()
                    if (isSpotify && matched != null && videoId != null) {
                        cache.put(track.id, matched)
                    }
                    videoId
                }
            }
        }
    }
}
