package com.luc4n3x.levyra.data.youtubemusic

import android.content.Context
import com.luc4n3x.levyra.data.YoutubeMusicPlaylistDetail
import com.luc4n3x.levyra.data.YoutubeMusicPlaylistPageParser
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.data.network.YoutubeRegionProfile
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

internal fun isYoutubeMusicGeneratedMix(playlistId: String): Boolean =
    playlistId.trim().removePrefix("VL").startsWith(GENERATED_MIX_PREFIX)

internal class YoutubeMusicMixSnapshots(private val maxEntries: Int = MAX_MIX_SNAPSHOTS) {
    private val entries = LinkedHashMap<String, YoutubeMusicPlaylistDetail>()
    private val locks = HashMap<String, Mutex>()

    suspend fun getOrLoad(key: String, load: suspend () -> YoutubeMusicPlaylistDetail?): YoutubeMusicPlaylistDetail? =
        lock(key).withLock {
            get(key) ?: load()?.also { put(key, it) }
        }

    @Synchronized
    fun get(key: String): YoutubeMusicPlaylistDetail? = entries[key]

    @Synchronized
    fun clear() {
        entries.clear()
    }

    @Synchronized
    private fun put(key: String, detail: YoutubeMusicPlaylistDetail) {
        entries.remove(key)
        entries[key] = detail
        while (entries.size > maxEntries) entries.remove(entries.keys.first())
    }

    @Synchronized
    private fun lock(key: String): Mutex = locks.getOrPut(key) { Mutex() }
}

internal class YoutubeMusicMixes(
    context: Context,
    private val repository: YoutubeMusicRepository
) {
    private val sessions = YoutubeMusicSessionStore(context.applicationContext)
    private val client = YoutubeMusicAccountClient()

    suspend fun playlist(playlistId: String, languageCode: String, limit: Int): YoutubeMusicPlaylistDetail? {
        val id = playlistId.trim().removePrefix("VL")
        if (!isYoutubeMusicGeneratedMix(id)) return null
        val session = withContext(Dispatchers.IO) { runCatching { sessions.load().active }.getOrNull() }
        val key = listOf(session?.account?.id.orEmpty(), id, YoutubeRegionProfile.cacheScope(languageCode)).joinToString("|")
        val detail = snapshots.getOrLoad(key) { fetch(session, id, languageCode) } ?: return null
        return detail.copy(tracks = detail.tracks.take(limit.coerceAtLeast(1)))
    }

    fun refresh() = snapshots.clear()

    private suspend fun fetch(session: YoutubeMusicSession?, id: String, languageCode: String): YoutubeMusicPlaylistDetail? {
        val personal = session?.let { active ->
            try {
                accountPlaylist(active, id, languageCode)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.w(error, "Account mix lookup failed; using the public mix")
                null
            }
        }
        return personal ?: repository.playlist(id, languageCode, MIX_TRACK_LIMIT)?.takeIf { it.tracks.isNotEmpty() }
    }

    private suspend fun accountPlaylist(session: YoutubeMusicSession, id: String, languageCode: String): YoutubeMusicPlaylistDetail? =
        withContext(Dispatchers.IO) {
            val root = client.browse(session, "VL$id", languageCode)
            val tracks = LinkedHashMap<String, Track>()
            repository.parsePlaylistTracks(root, id).forEach { tracks.putIfAbsent(it.id, it) }
            var continuation = YoutubeMusicPlaylistPageParser.continuation(root)
            var pages = 0
            while (continuation.isNotBlank() && tracks.size < MIX_TRACK_LIMIT && pages < MAX_MIX_CONTINUATIONS) {
                val page = client.browse(session, "", languageCode, continuation)
                val pageTracks = repository.parsePlaylistTracks(page, id)
                if (pageTracks.isEmpty()) break
                pageTracks.forEach { tracks.putIfAbsent(it.id, it) }
                continuation = YoutubeMusicPlaylistPageParser.continuation(page)
                pages++
            }
            if (tracks.isEmpty()) return@withContext null
            repository.parsePlaylistHeader(root, id).copy(tracks = tracks.values.take(MIX_TRACK_LIMIT))
        }

    private companion object {
        val snapshots = YoutubeMusicMixSnapshots()
    }
}

private const val GENERATED_MIX_PREFIX = "RDTMAK"
private const val MIX_TRACK_LIMIT = 200
private const val MAX_MIX_CONTINUATIONS = 3
private const val MAX_MIX_SNAPSHOTS = 48
