package com.luc4n3x.levyra.player

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.luc4n3x.levyra.data.ChartsRepository
import com.luc4n3x.levyra.data.FavoritesStore
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.data.LevyraSmartMusicProfileStore
import com.luc4n3x.levyra.data.PlaybackResolver
import com.luc4n3x.levyra.data.PlaylistStore
import com.luc4n3x.levyra.data.TrackPayloadCodec
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.data.local.DownloadEntity
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.domain.LevyraContentLocales
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.SmartMusicProfile
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.player.queue.PersistentQueueEngine
import com.luc4n3x.levyra.player.queue.playbackQueueIdentity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.LinkedHashMap
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

internal data class AndroidAutoPageWindow(val offset: Int, val limit: Int) {
    companion object {
        fun of(page: Int, pageSize: Int, maxItems: Int = Int.MAX_VALUE): AndroidAutoPageWindow {
            val effectivePageSize = if (pageSize <= 0) maxItems else pageSize.coerceAtMost(maxItems)
            if (effectivePageSize <= 0) return AndroidAutoPageWindow(0, 0)
            val offset = page.coerceAtLeast(0).toLong() * effectivePageSize.toLong()
            if (offset > Int.MAX_VALUE) return AndroidAutoPageWindow(Int.MAX_VALUE, 0)
            return AndroidAutoPageWindow(offset.toInt(), effectivePageSize)
        }

        fun limited(limit: Int): AndroidAutoPageWindow = AndroidAutoPageWindow(0, limit.coerceAtLeast(0))
    }
}

internal fun <T> List<T>.androidAutoWindow(window: AndroidAutoPageWindow): List<T> {
    if (window.limit <= 0 || window.offset >= size) return emptyList()
    val end = (window.offset.toLong() + window.limit.toLong()).coerceAtMost(size.toLong()).toInt()
    return subList(window.offset, end).toList()
}

class AndroidAutoLibrary(context: Context) {
    private val appContext = context.applicationContext
    private val favoritesStore = FavoritesStore(appContext)
    private val playlistStore = PlaylistStore(appContext)
    private val preferences = LevyraPreferences(appContext)
    private val smartProfileStore = LevyraSmartMusicProfileStore(appContext)
    private val database = LevyraDatabase.get(appContext)
    private val chartsRepository = ChartsRepository(appContext)
    private val musicRepository = YoutubeMusicRepository(appContext)
    private val resolver = PlaybackResolver.getInstance(appContext)
    private val queueEngine = PersistentQueueEngine.get(appContext)
    private val catalog = ConcurrentHashMap<String, Track>()
    private val folders = ConcurrentHashMap<String, MediaItem>()
    private val albumFolders = ConcurrentHashMap<String, Pair<String, String>>()
    private val artistFolders = ConcurrentHashMap<String, String>()
    private val searchCacheLock = Any()
    private val searchCache = object : LinkedHashMap<String, TimedTracks>(
        MAX_SEARCH_CACHE_ENTRIES,
        0.75f,
        true
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, TimedTracks>?
        ): Boolean = size > MAX_SEARCH_CACHE_ENTRIES
    }
    private val searchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val searchInFlight = ConcurrentHashMap<String, Deferred<List<Track>>>()

    fun root(): MediaItem = folder(ID_ROOT, "Levyra", "Musica ottimizzata per Android Auto")

    suspend fun children(parentId: String, page: Int = 0, pageSize: Int = 0): List<MediaItem> = withContext(Dispatchers.IO) {
        val window = AndroidAutoPageWindow.of(page, pageSize, MAX_PAGE_ITEMS)
        when (parentId) {
            ID_ROOT -> rootChildren().androidAutoWindow(window)
            ID_HOME -> homeChildren().androidAutoWindow(window)
            ID_QUEUE -> queueTracks(window).map { trackItem(it) }
            ID_RECOMMENDED -> smartRecommended().androidAutoWindow(window).map { trackItem(it) }
            ID_ALBUMS -> albumChildren().androidAutoWindow(window)
            ID_ARTISTS -> artistChildren().androidAutoWindow(window)
            ID_HOME_DRIVER -> drivingMix().androidAutoWindow(window).map { trackItem(it) }
            ID_HOME_TOP_IT -> topLocal().androidAutoWindow(window).map { trackItem(it) }
            ID_HOME_OFFLINE -> downloads(window).map { trackItem(it) }
            ID_FLOW -> flowTracks().androidAutoWindow(window).map { trackItem(it) }
            ID_FAVORITES -> favorites(window).map { trackItem(it) }
            ID_DOWNLOADS -> downloads(window).map { trackItem(it) }
            ID_RECENTS -> recents(window).map { trackItem(it) }
            ID_PLAYLISTS -> playlists(window).map { playlist -> playlistFolder(playlist) }
            else -> when {
                parentId.startsWith(ID_PLAYLIST_PREFIX) ->
                    playlistTracks(parentId.removePrefix(ID_PLAYLIST_PREFIX), window).map { trackItem(it) }
                parentId.startsWith(ID_ALBUM_PREFIX) -> albumTracks(parentId).androidAutoWindow(window).map { trackItem(it) }
                parentId.startsWith(ID_ARTIST_PREFIX) -> artistTracks(parentId).androidAutoWindow(window).map { trackItem(it) }
                else -> emptyList()
            }
        }
    }

    suspend fun item(mediaId: String): MediaItem = withContext(Dispatchers.IO) {
        folders[mediaId] ?: catalog[mediaId]?.let { trackItem(it, mediaId) } ?: when {
            mediaId == ID_ROOT -> root()
            mediaId == ID_HOME -> folder(ID_HOME, "Home", "Mix veloci, offline e classifiche")
            mediaId == ID_QUEUE -> folder(ID_QUEUE, "Coda attuale", "Coda persistente condivisa con Levyra")
            mediaId == ID_RECOMMENDED -> folder(ID_RECOMMENDED, "Consigliati", "Profilo musicale, preferiti e ascolti recenti")
            mediaId == ID_ALBUMS -> folder(ID_ALBUMS, "Album", "Album locali e suggeriti")
            mediaId == ID_ARTISTS -> folder(ID_ARTISTS, "Artisti", "Artisti più ascoltati e seguiti")
            mediaId == ID_FLOW -> folder(ID_FLOW, "Flow", "Radio personale Levyra")
            mediaId == ID_FAVORITES -> folder(ID_FAVORITES, "Preferiti", "Brani salvati")
            mediaId == ID_DOWNLOADS -> folder(ID_DOWNLOADS, "Download", "Musica disponibile offline")
            mediaId == ID_PLAYLISTS -> folder(ID_PLAYLISTS, "Playlist", "Raccolte create nell'app")
            mediaId == ID_RECENTS -> folder(ID_RECENTS, "Ultimi ascolti", "Brani trovati di recente")
            mediaId.startsWith(ID_PLAYLIST_PREFIX) -> playlistStore.load(mediaId.removePrefix(ID_PLAYLIST_PREFIX))?.let { playlistFolder(it) }
                ?: folder(mediaId, "Playlist", "Playlist non disponibile")
            mediaId.startsWith(ID_ALBUM_PREFIX) -> albumFolders[mediaId]?.let { pair -> folder(mediaId, pair.first, pair.second) }
                ?: folder(mediaId, "Album", "Album non disponibile")
            mediaId.startsWith(ID_ARTIST_PREFIX) -> artistFolders[mediaId]?.let { artist -> folder(mediaId, artist, "Brani e album dell'artista") }
                ?: folder(mediaId, "Artista", "Artista non disponibile")
            else -> folder(mediaId, "Levyra", "Elemento non disponibile")
        }
    }

    fun preloadSearch(query: String) {
        val clean = query.voiceSearchQuery()
        if (clean.isBlank() || cachedSearch(clean) != null) return
        startOrJoinSearch(clean).invokeOnCompletion { error ->
            if (error != null && error !is CancellationException) {
                Timber.d(error, "Android Auto search preload failed")
            }
        }
    }

    suspend fun search(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val clean = query.voiceSearchQuery()
        if (clean.isBlank()) return@withContext flowTracks().map { trackItem(it) }
        val tracks = cachedSearch(clean) ?: startOrJoinSearch(clean).await()
        tracks.map { trackItem(it) }
    }

    fun close() {
        searchInFlight.values.forEach { it.cancel() }
        searchInFlight.clear()
        searchScope.cancel()
        synchronized(searchCacheLock) { searchCache.clear() }
    }

    private fun startOrJoinSearch(query: String): Deferred<List<Track>> {
        searchInFlight[query]?.let { return it }
        val deferred = searchScope.async(start = CoroutineStart.LAZY) {
            searchTracks(query)
        }
        deferred.invokeOnCompletion {
            searchInFlight.remove(query, deferred)
        }
        val existing = searchInFlight.putIfAbsent(query, deferred)
        if (existing != null) {
            deferred.cancel()
            return existing
        }
        deferred.start()
        return deferred
    }

    suspend fun playableItems(mediaItems: List<MediaItem>): List<MediaItem> = withContext(Dispatchers.IO) {
        mediaItems.mapNotNull { mediaItem ->
            runCatching { playableItem(mediaItem) }
                .onFailure { Timber.w(it, "Android Auto playable item failed") }
                .getOrNull()
        }
    }

    private fun rootChildren(): List<MediaItem> = listOf(
        folder(ID_HOME, "Home", "Mix veloci, offline e classifiche"),
        folder(ID_QUEUE, "Coda attuale", "Riprendi e controlla la coda persistente"),
        folder(ID_RECOMMENDED, "Consigliati", "Profilo musicale intelligente"),
        folder(ID_ALBUMS, "Album", "Album veri, playlist offline e raccolte locali"),
        folder(ID_ARTISTS, "Artisti", "Artisti più ascoltati e seguiti"),
        folder(ID_FLOW, "Flow", "Radio personale Levyra"),
        folder(ID_FAVORITES, "Preferiti", "Brani salvati"),
        folder(ID_DOWNLOADS, "Download", "Musica disponibile offline"),
        folder(ID_PLAYLISTS, "Playlist", "Raccolte create nell'app"),
        folder(ID_RECENTS, "Ultimi ascolti", "Brani trovati di recente")
    )

    private fun homeChildren(): List<MediaItem> = listOf(
        folder(ID_RECOMMENDED, "Per te", "Consigli dal profilo musicale"),
        folder(ID_HOME_DRIVER, "Per la guida", "Preferiti, recenti e brani ad alta energia"),
        folder(ID_HOME_OFFLINE, "Offline pronto", "Download riproducibili senza rete"),
        folder(ID_HOME_TOP_IT, "Top locali", "Classifica musicale aggiornata per la lingua scelta"),
        folder(ID_FLOW, "Flow Levyra", "Mix personale continuo")
    )

    private fun folder(id: String, title: String, subtitle: String): MediaItem {
        return folders.getOrPut(id) {
            MediaItem.Builder()
                .setMediaId(id)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(title)
                        .setSubtitle(subtitle)
                        .setDescription(subtitle)
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
        }
    }

    private fun playlistFolder(playlist: Playlist): MediaItem {
        val id = ID_PLAYLIST_PREFIX + playlist.id
        return folder(id, playlist.name, "${playlist.size} brani")
    }

    private fun trackItem(track: Track, forcedMediaId: String? = null): MediaItem {
        val mediaId = forcedMediaId ?: trackMediaId(track)
        catalog[mediaId] = track
        val art = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }
        val extras = Bundle().apply {
            putString(EXTRA_TRACK_PAYLOAD, TrackPayloadCodec.encode(track.copy(streamUrl = streamUrlForPayload(track))))
            putString(EXTRA_SOURCE, track.source)
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setSubtitle(track.artist.ifBlank { track.source })
            .setDescription(track.source)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setDurationMs(track.durationMs.takeIf { it > 0L } ?: C.TIME_UNSET)
            .setExtras(extras)
            .apply { if (art.isNotBlank()) setArtworkUri(Uri.parse(art)) }
            .build()
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(metadata)
            .apply { if (track.streamUrl.isNotBlank()) setUri(track.streamUrl) }
            .build()
    }

    private suspend fun playableItem(mediaItem: MediaItem): MediaItem {
        if (!mediaItem.mediaId.startsWith(ID_TRACK_PREFIX) && mediaItem.localConfiguration?.uri != null) return mediaItem
        val mediaId = mediaItem.mediaId
        val track = catalog[mediaId]
            ?: mediaItem.mediaMetadata.extras?.getString(EXTRA_TRACK_PAYLOAD)?.let(TrackPayloadCodec::decode)
            ?: return mediaItem
        val queueSnapshot = queueEngine.state.value
        val queueIndex = queueSnapshot.tracks.indexOfFirst { playbackQueueIdentity(it) == playbackQueueIdentity(track) }
        if (queueIndex >= 0) queueEngine.select(queueIndex, positionMs = 0L, rememberCurrent = true)
        val ready = when {
            track.streamUrl.isLocalUri() -> track
            else -> resolver.resolve(track.ensureYoutubeIdentity())
        }
        if (queueIndex >= 0) {
            queueEngine.updateTrackAt(queueIndex, ready)
        } else {
            queueEngine.replace(
                tracks = listOf(ready),
                currentIndex = 0,
                keepPlaybackModes = true,
                radioEnabled = !ready.streamUrl.isLocalUri()
            )
        }
        return buildPlaybackItem(ready)
    }

    private suspend fun Track.ensureYoutubeIdentity(): Track {
        if (streamUrl.isNotBlank() && !streamUrl.isLocalUri()) return this
        if (videoUrl.isNotBlank() && !id.startsWith("chart-", true) && !id.startsWith("auto-", true)) return this
        val match = musicRepository.searchSongMatch(title.cleanQuery(), artist.cleanQuery(), contentLanguage())
            ?: return this
        return match.copy(
            title = title.ifBlank { match.title },
            artist = artist.ifBlank { match.artist },
            album = album.ifBlank { match.album },
            thumbnailUrl = thumbnailUrl.ifBlank { match.thumbnailUrl },
            largeThumbnailUrl = largeThumbnailUrl.ifBlank { match.largeThumbnailUrl },
            accentStart = accentStart,
            accentEnd = accentEnd
        )
    }

    private fun buildPlaybackItem(track: Track): MediaItem {
        val art = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }
        val extras = Bundle().apply {
            if (track.videoStreamUrl.isNotBlank()) {
                putString(PlaybackService.EXTRA_VIDEO_URL, track.videoStreamUrl)
                putString(PlaybackService.EXTRA_VIDEO_CACHE_KEY, LevyraPlaybackCacheKey.video(track))
            }
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setSubtitle(track.artist)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setDurationMs(track.durationMs.takeIf { it > 0L } ?: C.TIME_UNSET)
            .setExtras(extras)
            .apply { if (art.isNotBlank()) setArtworkUri(Uri.parse(art)) }
            .build()
        return MediaItem.Builder()
            .setUri(track.streamUrl)
            .setMediaId(trackMediaId(track))
            .setMediaMetadata(metadata)
            .apply { if (!track.streamUrl.isLocalUri()) setCustomCacheKey(LevyraPlaybackCacheKey.stream(track)) }
            .build()
    }

    private fun trackMediaId(track: Track): String {
        val seed = listOf(track.id, track.title, track.artist, track.streamUrl.ifBlank { track.videoUrl }).joinToString("|")
        return ID_TRACK_PREFIX + seed.sha256().take(24)
    }

    private suspend fun queueTracks(window: AndroidAutoPageWindow = DERIVED_WINDOW): List<Track> = withContext(Dispatchers.IO) {
        if (queueEngine.state.value.tracks.isEmpty()) {
            queueEngine.restore(
                fallbackTracks = emptyList(),
                fallbackIndex = -1,
                fallbackPositionMs = 0L,
                fallbackRadioEnabled = true
            )
        }
        queueEngine.state.value.tracks.androidAutoWindow(window)
    }

    private suspend fun favorites(window: AndroidAutoPageWindow = DERIVED_WINDOW): List<Track> = withContext(Dispatchers.IO) {
        favoritesStore.load().distinctTracks().androidAutoWindow(window)
    }

    private suspend fun recents(window: AndroidAutoPageWindow = DERIVED_WINDOW): List<Track> = withContext(Dispatchers.IO) {
        preferences.loadRecentSearches().distinctTracks().androidAutoWindow(window)
    }

    private suspend fun downloads(window: AndroidAutoPageWindow = DERIVED_WINDOW): List<Track> = withContext(Dispatchers.IO) {
        database.downloadedTracksDao()
            .page(limit = window.limit, offset = window.offset)
            .map { it.toAutoTrack() }
    }

    private suspend fun playlists(window: AndroidAutoPageWindow = PLAYLIST_WINDOW): List<Playlist> = withContext(Dispatchers.IO) {
        playlistStore.loadAll().androidAutoWindow(window)
    }

    private suspend fun playlistTracks(
        playlistId: String,
        window: AndroidAutoPageWindow = DERIVED_WINDOW
    ): List<Track> = withContext(Dispatchers.IO) {
        playlistStore.load(playlistId)?.tracks.orEmpty().distinctTracks().androidAutoWindow(window)
    }

    private fun contentLanguage(): String = preferences.languageCode()

    private suspend fun topLocal(): List<Track> = withContext(Dispatchers.IO) {
        val locale = LevyraContentLocales.forLanguage(contentLanguage())
        runCatching { chartsRepository.topSongs(locale.chartCountry, 40) }.getOrDefault(emptyList()).distinctTracks().take(40)
    }

    private suspend fun drivingMix(): List<Track> = withContext(Dispatchers.IO) {
        val mixed = buildList {
            addAll(favorites().filter { it.energy >= 55 }.take(18))
            addAll(recents().filter { it.energy >= 50 }.take(18))
            addAll(downloads().take(12))
            addAll(topLocal().take(16))
        }
        mixed.distinctTracks().take(MAX_FOLDER_TRACKS)
    }

    private suspend fun flowTracks(): List<Track> = withContext(Dispatchers.IO) {
        val local = buildList {
            addAll(favorites())
            addAll(recents())
            addAll(downloads())
        }.distinctTracks()
        if (local.size >= 16) return@withContext local.sortedByDescending { it.replayScore + it.cacheScore + it.energy }.take(MAX_FOLDER_TRACKS)
        val languageCode = contentLanguage()
        val query = LevyraContentLocales.forLanguage(languageCode).homeQueries.joinToString(" ")
        val fallback = runCatching { musicRepository.search(query, 36, languageCode) }.getOrDefault(emptyList())
        (local + fallback).distinctTracks().take(MAX_FOLDER_TRACKS)
    }

    private suspend fun searchTracks(query: String): List<Track> = withContext(Dispatchers.IO) {
        cachedSearch(query)?.let { return@withContext it }
        val local = searchLocal(query)
        val languageCode = contentLanguage()
        val remote = try {
            musicRepository.search(query, 30, languageCode)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "Android Auto remote search failed")
            return@withContext local.distinctTracks().take(MAX_FOLDER_TRACKS)
        }
        val result = (local + remote).distinctTracks().take(MAX_FOLDER_TRACKS)
        cacheSearch(query, result)
        result
    }

    private fun cachedSearch(query: String): List<Track>? = synchronized(searchCacheLock) {
        val now = System.currentTimeMillis()
        val iterator = searchCache.entries.iterator()
        while (iterator.hasNext()) {
            if (now - iterator.next().value.createdAt >= SEARCH_TTL_MS) iterator.remove()
        }
        searchCache[query]?.tracks
    }

    private fun cacheSearch(query: String, tracks: List<Track>) {
        synchronized(searchCacheLock) {
            searchCache[query] = TimedTracks(tracks, System.currentTimeMillis())
        }
    }

    private suspend fun searchLocal(query: String): List<Track> = withContext(Dispatchers.IO) {
        val needle = query.lowercase(Locale.ROOT)
        val tokens = query.searchTokens()
        buildList {
            addAll(favorites())
            addAll(recents())
            addAll(downloads())
            playlists().forEach { addAll(it.tracks) }
        }.filter { track ->
            val searchable = listOf(track.title, track.artist, track.album)
                .joinToString(" ")
                .lowercase(Locale.ROOT)
            searchable.contains(needle) || tokens.isNotEmpty() && tokens.all(searchable::contains)
        }.sortedByDescending { track ->
            val title = track.title.lowercase(Locale.ROOT)
            val artist = track.artist.lowercase(Locale.ROOT)
            when {
                title == needle -> 5
                title.startsWith(needle) -> 4
                artist == needle -> 3
                title.contains(needle) -> 2
                artist.contains(needle) -> 2
                track.album.lowercase(Locale.ROOT).contains(needle) -> 1
                else -> 0
            }
        }.distinctTracks().take(24)
    }


    private suspend fun smartRecommended(): List<Track> = withContext(Dispatchers.IO) {
        val profile = smartProfileStore.load()
        val local = buildList {
            addAll(favorites())
            addAll(recents())
            addAll(downloads())
            playlists().take(12).forEach { addAll(it.tracks) }
        }.distinctTracks()
        val rankedLocal = local.sortedByDescending { smartWeight(it, profile) }.take(MAX_FOLDER_TRACKS)
        if (rankedLocal.size >= 18) return@withContext rankedLocal
        val languageCode = contentLanguage()
        val queries = (profile.albumQueries + profile.artistQueries).ifEmpty { LevyraContentLocales.forLanguage(languageCode).homeQueries }
        val remote = queries.take(4).flatMap { query ->
            runCatching { musicRepository.search(query, 12, languageCode) }.getOrDefault(emptyList())
        }
        (rankedLocal + remote).distinctTracks().sortedByDescending { smartWeight(it, profile) }.take(MAX_FOLDER_TRACKS)
    }

    private suspend fun albumChildren(): List<MediaItem> = withContext(Dispatchers.IO) {
        val profile = smartProfileStore.load()
        val local = buildList {
            addAll(downloads())
            addAll(favorites())
            addAll(recents())
            playlists().take(20).forEach { addAll(it.tracks) }
        }.distinctTracks()
        val grouped = local
            .filter { it.album.isNotBlank() && !it.album.equals(it.title, ignoreCase = true) }
            .groupBy { "${it.album.trim()}|${it.artist.trim()}" }
            .entries
            .sortedByDescending { entry -> entry.value.sumOf { smartWeight(it, profile) } + entry.value.size * 20 }
            .take(36)
        grouped.map { entry ->
            val parts = entry.key.split("|", limit = 2)
            val title = parts.getOrElse(0) { "Album" }
            val artist = parts.getOrElse(1) { "" }
            val id = ID_ALBUM_PREFIX + entry.key.sha256().take(22)
            albumFolders[id] = title to artist
            folder(id, title, listOf(artist, "${entry.value.size} brani").filter { it.isNotBlank() }.joinToString(" · "))
        }
    }

    private suspend fun artistChildren(): List<MediaItem> = withContext(Dispatchers.IO) {
        val profile = smartProfileStore.load()
        val localArtists = buildList {
            addAll(profile.topArtists.map { it.label })
            addAll(favorites().map { it.artist })
            addAll(recents().map { it.artist })
            addAll(downloads().map { it.artist })
        }.map { it.trim() }.filter { it.isNotBlank() && !it.equals("YouTube Music", ignoreCase = true) }.distinctBy { it.lowercase(Locale.ROOT) }.take(40)
        localArtists.map { artist ->
            val id = ID_ARTIST_PREFIX + artist.sha256().take(22)
            artistFolders[id] = artist
            folder(id, artist, "Brani, preferiti e suggerimenti")
        }
    }

    private suspend fun albumTracks(parentId: String): List<Track> = withContext(Dispatchers.IO) {
        val pair = albumFolders[parentId] ?: return@withContext emptyList()
        val local = buildList {
            addAll(downloads())
            addAll(favorites())
            addAll(recents())
            playlists().take(20).forEach { addAll(it.tracks) }
        }.filter { it.album.equals(pair.first, ignoreCase = true) && (pair.second.isBlank() || it.artist.equals(pair.second, ignoreCase = true)) }
            .distinctTracks()
        if (local.isNotEmpty()) return@withContext local.take(MAX_FOLDER_TRACKS)
        val query = listOf(pair.first, pair.second, "album").filter { it.isNotBlank() }.joinToString(" ")
        runCatching { musicRepository.search(query, 30, contentLanguage()) }.getOrDefault(emptyList()).distinctTracks().take(MAX_FOLDER_TRACKS)
    }

    private suspend fun artistTracks(parentId: String): List<Track> = withContext(Dispatchers.IO) {
        val artist = artistFolders[parentId] ?: return@withContext emptyList()
        val local = buildList {
            addAll(favorites())
            addAll(recents())
            addAll(downloads())
            playlists().take(20).forEach { addAll(it.tracks) }
        }.filter { it.artist.equals(artist, ignoreCase = true) }
            .distinctTracks()
        if (local.size >= 8) return@withContext local.take(MAX_FOLDER_TRACKS)
        val remote = runCatching { musicRepository.search(artist, 30, contentLanguage()) }.getOrDefault(emptyList())
        (local + remote).distinctTracks().take(MAX_FOLDER_TRACKS)
    }

    private fun smartWeight(track: Track, profile: SmartMusicProfile): Int {
        val artist = track.artist.trim().lowercase(Locale.ROOT)
        val album = track.album.trim().lowercase(Locale.ROOT)
        val artistScore = profile.topArtists.firstOrNull { it.label.lowercase(Locale.ROOT) == artist }?.weight ?: 0
        val albumScore = profile.topAlbums.firstOrNull { seed -> seed.label.lowercase(Locale.ROOT).contains(album) && seed.label.lowercase(Locale.ROOT).contains(artist) }?.weight ?: 0
        return track.replayScore + track.cacheScore + track.energy / 2 + artistScore * 2 + albumScore * 3
    }

    private fun DownloadEntity.toAutoTrack(): Track = Track(
        id = "download-$id-${trackId.ifBlank { fileName }}".sha256().take(20),
        title = title.ifBlank { fileName },
        artist = artist,
        album = album.ifBlank { "Download" },
        durationMs = durationMs,
        streamUrl = uri,
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "Download",
        moodTags = setOf("offline", "download"),
        energy = 64,
        vocal = 50,
        replayScore = 90,
        cacheScore = 100,
        accentStart = 0xFF00E5FF.toInt(),
        accentEnd = 0xFF7C4DFF.toInt()
    )

    private fun List<Track>.distinctTracks(): List<Track> {
        val seen = LinkedHashSet<String>()
        val result = ArrayList<Track>(size)
        forEach { track ->
            val key = when {
                track.streamUrl.isLocalUri() -> "local:${track.streamUrl}"
                track.videoUrl.isNotBlank() -> "video:${track.videoUrl}"
                else -> "meta:${track.id}:${track.title}:${track.artist}"
            }.lowercase(Locale.ROOT)
            if (seen.add(key) && track.title.isNotBlank()) result += track
        }
        return result
    }

    private fun String.cleanQuery(): String = trim()
        .replace(Regex("[^\\p{L}\\p{M}\\p{N} .,'’&+_-]+"), " ")
        .replace(Regex("\\s+"), " ")
        .take(90)
        .trim()

    private fun String.voiceSearchQuery(): String {
        var value = cleanQuery()
        while (true) {
            val prefix = VOICE_COMMAND_PREFIXES.firstOrNull {
                value.startsWith(it, ignoreCase = true)
            } ?: break
            value = value.drop(prefix.length).trim()
        }
        while (true) {
            val lowered = value.lowercase(Locale.ROOT)
            val suffix = VOICE_APP_SUFFIXES.firstOrNull(lowered::endsWith) ?: break
            value = value.dropLast(suffix.length).trim()
        }
        return value.cleanQuery()
    }

    private fun String.searchTokens(): List<String> = cleanQuery()
        .lowercase(Locale.ROOT)
        .split(' ')
        .map(String::trim)
        .filter { it.isNotEmpty() && it !in SEARCH_STOP_WORDS }
        .distinct()

    private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(StandardCharsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private fun String.isLocalUri(): Boolean {
        val lower = lowercase(Locale.ROOT)
        return lower.startsWith("content://") || lower.startsWith("file://")
    }

    private fun streamUrlForPayload(track: Track): String = if (track.streamUrl.isLocalUri()) track.streamUrl else ""

    private data class TimedTracks(val tracks: List<Track>, val createdAt: Long)

    companion object {
        const val ID_ROOT = "levyra:auto:root"
        const val ID_HOME = "levyra:auto:home"
        const val ID_QUEUE = "levyra:auto:queue"
        const val ID_HOME_DRIVER = "levyra:auto:home:driver"
        const val ID_HOME_TOP_IT = "levyra:auto:home:top-it"
        const val ID_HOME_OFFLINE = "levyra:auto:home:offline"
        const val ID_RECOMMENDED = "levyra:auto:recommended"
        const val ID_ALBUMS = "levyra:auto:albums"
        const val ID_ARTISTS = "levyra:auto:artists"
        const val ID_FLOW = "levyra:auto:flow"
        const val ID_FAVORITES = "levyra:auto:favorites"
        const val ID_DOWNLOADS = "levyra:auto:downloads"
        const val ID_PLAYLISTS = "levyra:auto:playlists"
        const val ID_RECENTS = "levyra:auto:recents"
        const val ID_PLAYLIST_PREFIX = "levyra:auto:playlist:"
        const val ID_ALBUM_PREFIX = "levyra:auto:album:"
        const val ID_ARTIST_PREFIX = "levyra:auto:artist:"
        const val ID_TRACK_PREFIX = "levyra:auto:track:"
        const val EXTRA_TRACK_PAYLOAD = "levyra.auto.TRACK_PAYLOAD"
        const val EXTRA_SOURCE = "levyra.auto.SOURCE"
        private const val MAX_FOLDER_TRACKS = 80
        private const val MAX_PLAYLISTS = 60
        private const val MAX_PAGE_ITEMS = 100
        private val DERIVED_WINDOW = AndroidAutoPageWindow.limited(MAX_FOLDER_TRACKS)
        private val PLAYLIST_WINDOW = AndroidAutoPageWindow.limited(MAX_PLAYLISTS)
        private const val SEARCH_TTL_MS = 3L * 60L * 1000L
        private const val MAX_SEARCH_CACHE_ENTRIES = 64
        private val VOICE_COMMAND_PREFIXES = listOf(
            "riproduci ", "suona ", "metti ", "ascolta ", "cerca ",
            "play ", "reproduce ", "toca ", "escucha ", "écoute ", "joue ",
            "spiele ", "suche ", "ouvir ", "toque "
        )
        private val VOICE_APP_SUFFIXES = listOf(
            " su levyra", " in levyra", " en levyra", " dans levyra", " mit levyra"
        )
        private val SEARCH_STOP_WORDS = setOf(
            "di", "del", "della", "dei", "degli", "delle", "da", "by", "the",
            "un", "una", "uno", "il", "lo", "la", "i", "gli", "le"
        )
    }
}
