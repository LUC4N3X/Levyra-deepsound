package com.luc4n3x.levyra.data.lastfm

import android.content.Context
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.Locale

internal fun interleaveSimilarAlbums(perArtist: List<List<LastFmDiscoveryAlbum>>, limit: Int): List<LastFmDiscoveryAlbum> {
    val seen = HashSet<String>()
    val result = ArrayList<LastFmDiscoveryAlbum>()
    val deepest = perArtist.maxOfOrNull { it.size } ?: 0
    for (depth in 0 until deepest) {
        for (albums in perArtist) {
            val album = albums.getOrNull(depth) ?: continue
            if (seen.add("${album.artist}\u0000${album.title}".lowercase(Locale.ROOT))) result += album
            if (result.size >= limit) return result
        }
    }
    return result
}

class LastFmSimilarAlbumsRepository(
    context: Context,
    private val apiKey: String = BuildConfig.LASTFM_API_KEY
) {
    private val appContext = context.applicationContext
    private val httpClient: OkHttpClient by lazy { LevyraHttpClientFactory.feeds(appContext) }
    private val memory = object : LinkedHashMap<String, List<LastFmDiscoveryAlbum>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<LastFmDiscoveryAlbum>>?): Boolean = size > MEMORY_LIMIT
    }

    suspend fun similarAlbums(artist: String): List<LastFmDiscoveryAlbum> = withContext(Dispatchers.IO) {
        val name = artist.trim()
        if (name.isBlank() || apiKey.isBlank()) return@withContext emptyList()
        val key = name.lowercase(Locale.ROOT)
        synchronized(memory) { memory[key] }?.let { return@withContext it }
        val similar = lastFmGet(
            httpClient, apiKey,
            mapOf("method" to "artist.getsimilar", "artist" to name, "autocorrect" to "1", "limit" to SIMILAR_ARTISTS.toString()),
            LastFmGenreDiscoveryParser::parseSimilarArtists
        ) ?: return@withContext emptyList()
        if (similar.isEmpty()) {
            synchronized(memory) { memory[key] = emptyList() }
            return@withContext emptyList()
        }
        val perArtist = coroutineScope {
            similar.take(SIMILAR_ARTISTS).map { similarArtist ->
                async {
                    lastFmGet(
                        httpClient, apiKey,
                        mapOf("method" to "artist.gettopalbums", "artist" to similarArtist, "autocorrect" to "1", "limit" to ALBUMS_PER_ARTIST.toString())
                    ) { body -> LastFmGenreDiscoveryParser.parseTopAlbums(body, "topalbums") }.orEmpty()
                }
            }.awaitAll()
        }
        interleaveSimilarAlbums(perArtist, ALBUM_LIMIT).also { if (it.isNotEmpty()) synchronized(memory) { memory[key] = it } }
    }

    private companion object {
        const val SIMILAR_ARTISTS = 8
        const val ALBUMS_PER_ARTIST = 2
        const val ALBUM_LIMIT = 15
        const val MEMORY_LIMIT = 32
    }
}
