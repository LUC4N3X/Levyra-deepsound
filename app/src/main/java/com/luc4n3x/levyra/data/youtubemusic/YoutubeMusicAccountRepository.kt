package com.luc4n3x.levyra.data.youtubemusic

import android.content.Context
import com.luc4n3x.levyra.data.PlaylistStore
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.data.YoutubeMusicPlaylistPageParser
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

internal enum class YoutubeMusicCollection(val browseId: String) {
    Playlists("FEmusic_liked_playlists"),
    Likes("VLLM"),
    Albums("FEmusic_liked_albums"),
    Artists("FEmusic_library_corpus_artists")
}

internal data class YoutubeMusicLibraryEntry(
    val browseId: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String,
    val collection: YoutubeMusicCollection
)

internal data class YoutubeMusicLibraryPage(
    val entries: List<YoutubeMusicLibraryEntry> = emptyList(),
    val tracks: List<Track> = emptyList(),
    val continuation: String = ""
)

internal class YoutubeMusicAccountRepository(context: Context) {
    val sessions = YoutubeMusicSessionStore(context)
    val client = YoutubeMusicAccountClient()
    private val parser = YoutubeMusicRepository(context.applicationContext)
    private val playlists = PlaylistStore(context.applicationContext)

    suspend fun page(
        session: YoutubeMusicSession,
        collection: YoutubeMusicCollection,
        language: String,
        browseId: String = collection.browseId,
        continuation: String = ""
    ): YoutubeMusicLibraryPage = withContext(Dispatchers.IO) {
        val root = client.browse(session, browseId, language, continuation)
        val trackPage = collection == YoutubeMusicCollection.Likes || browseId != collection.browseId
        val content = if (trackPage) {
            if (!root.has("contents") && !root.has("continuationContents") &&
                !root.has("onResponseReceivedActions") && !root.has("onResponseReceivedEndpoints")) throw YoutubeMusicProtocolException()
            root
        } else YoutubeMusicAccountParser.content(root) ?: throw YoutubeMusicProtocolException()
        val overview = if (trackPage) null else parser.parseSearchOverview(JSONObject().put("contents", content), "library")
        val entries = when (collection) {
            YoutubeMusicCollection.Playlists -> overview?.playlists.orEmpty().map {
                YoutubeMusicLibraryEntry(it.browseId.ifBlank { "VL${it.playlistId}" }, it.title, it.author, it.thumbnailUrl, collection)
            }
            YoutubeMusicCollection.Albums -> overview?.albums.orEmpty().map {
                YoutubeMusicLibraryEntry(it.browseId, it.title, it.artist, it.thumbnailUrl, collection)
            }
            YoutubeMusicCollection.Artists -> overview?.artists.orEmpty().map {
                YoutubeMusicLibraryEntry(it.browseId, it.name, it.subscribers, it.thumbnailUrl, collection)
            }
            YoutubeMusicCollection.Likes -> emptyList()
        }.filter { it.browseId.isNotBlank() }.distinctBy { it.browseId }
        val tracks = if (trackPage) parser.parsePlaylistTracks(root, browseId) else emptyList()
        val next = if (trackPage) YoutubeMusicPlaylistPageParser.continuation(root) else YoutubeMusicAccountParser.continuation(root)
        YoutubeMusicLibraryPage(entries, tracks, next)
    }

    suspend fun fullTracks(session: YoutubeMusicSession, collection: YoutubeMusicCollection, browseId: String, language: String): List<Track> {
        return collectYoutubeMusicPages(key = Track::id) { continuation ->
            val page = page(session, collection, language, browseId, continuation)
            YoutubeMusicPage(page.tracks, page.continuation)
        }
    }

    suspend fun importTracks(session: YoutubeMusicSession, browseId: String, title: String, tracks: List<Track>): Int = withContext(Dispatchers.IO) {
        if (tracks.isEmpty()) return@withContext 0
        val id = "ytm-" + UUID.nameUUIDFromBytes("${session.account.id}:$browseId".toByteArray(Charsets.UTF_8))
        val existing = playlists.load(id)
        val clean = tracks.map { it.copy(streamUrl = "", videoStreamUrl = "") }
        if (existing == null) playlists.createWithTracks(title, clean, playlistId = id) else playlists.addTracks(id, clean)
        tracks.size
    }

}
