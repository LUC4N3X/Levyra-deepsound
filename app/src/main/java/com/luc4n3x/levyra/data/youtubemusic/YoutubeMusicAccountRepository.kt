package com.luc4n3x.levyra.data.youtubemusic

import android.content.Context
import com.luc4n3x.levyra.data.PlaylistStore
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.data.YoutubeMusicPlaylistPageParser
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    val collection: YoutubeMusicCollection,
    val owned: Boolean = false
)

internal data class YoutubeMusicLibraryPage(
    val entries: List<YoutubeMusicLibraryEntry> = emptyList(),
    val tracks: List<Track> = emptyList(),
    val continuation: String = ""
)

internal data class YoutubeMusicLibraryShelf(
    val accountId: String,
    val languageCode: String,
    val playlists: List<YoutubeMusicLibraryEntry>,
    val albums: List<YoutubeMusicLibraryEntry>
)

internal data class YoutubeMusicHome(
    val accountId: String,
    val languageCode: String,
    val orbit: List<Track>,
    val sections: List<HomeSection>,
    val albums: List<AlbumHit>
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
        continuation: String = "",
        album: YoutubeMusicLibraryEntry? = null
    ): YoutubeMusicLibraryPage = withContext(Dispatchers.IO) {
        val root = client.browse(session, browseId, language, continuation)
        val trackPage = collection == YoutubeMusicCollection.Likes || browseId != collection.browseId
        val content = if (trackPage) {
            if (!root.has("contents") && !root.has("continuationContents") &&
                !root.has("onResponseReceivedActions") && !root.has("onResponseReceivedEndpoints")) throw YoutubeMusicProtocolException()
            root
        } else YoutubeMusicAccountParser.content(root) ?: throw YoutubeMusicProtocolException()
        val entries = if (trackPage) emptyList() else libraryEntries(content, collection)
        val tracks = if (trackPage) {
            parser.parsePlaylistTracks(root, browseId).map { track -> if (album == null) track else withAlbum(track, album) }
        } else emptyList()
        val next = if (trackPage) YoutubeMusicPlaylistPageParser.continuation(root) else YoutubeMusicAccountParser.continuation(root)
        YoutubeMusicLibraryPage(entries, tracks, next)
    }

    private fun libraryEntries(content: Any, collection: YoutubeMusicCollection): List<YoutubeMusicLibraryEntry> {
        val overview = parser.parseSearchOverview(JSONObject().put("contents", content), "library")
        val entries = when (collection) {
            YoutubeMusicCollection.Playlists -> {
                val owned = YoutubeMusicAccountParser.ownedPlaylistBrowseIds(content)
                overview.playlists.map {
                    val browseId = it.browseId.ifBlank { "VL${it.playlistId}" }
                    YoutubeMusicLibraryEntry(browseId, it.title, it.author, it.thumbnailUrl, collection, owned = browseId in owned)
                }
            }
            YoutubeMusicCollection.Albums -> overview.albums.map {
                YoutubeMusicLibraryEntry(it.browseId, it.title, it.artist, it.thumbnailUrl, collection)
            }
            YoutubeMusicCollection.Artists -> overview.artists.map {
                YoutubeMusicLibraryEntry(it.browseId, it.name, it.subscribers, it.thumbnailUrl, collection)
            }
            YoutubeMusicCollection.Likes -> emptyList()
        }
        return entries.filter { it.browseId.isNotBlank() }.distinctBy { it.browseId }
    }

    suspend fun home(session: YoutubeMusicSession, language: String): YoutubeMusicHome = withContext(Dispatchers.IO) {
        val root = client.browse(session, "FEmusic_home", language)
        val sections = parser.parseHomeSections(root)
        val orbitIndex = sections.indexOfFirst { it.tracks.size >= 6 }
        YoutubeMusicHome(
            accountId = session.account.id,
            languageCode = language,
            orbit = sections.getOrNull(orbitIndex)?.tracks.orEmpty(),
            sections = sections.filterIndexed { index, _ -> index != orbitIndex },
            albums = parser.parseHomeAlbums(root)
        )
    }

    suspend fun libraryShelf(session: YoutubeMusicSession, language: String): YoutubeMusicLibraryShelf = coroutineScope {
        val playlists = async { runCatching { page(session, YoutubeMusicCollection.Playlists, language) } }
        val albums = async { runCatching { page(session, YoutubeMusicCollection.Albums, language) } }
        val playlistPage = playlists.await()
        val albumPage = albums.await()
        listOf(playlistPage, albumPage).forEach { result ->
            val error = result.exceptionOrNull() ?: return@forEach
            if (error is CancellationException || error is YoutubeMusicSessionExpired) throw error
        }
        if (playlistPage.isFailure && albumPage.isFailure) throw requireNotNull(playlistPage.exceptionOrNull())
        YoutubeMusicLibraryShelf(
            accountId = session.account.id,
            languageCode = language,
            playlists = youtubeMusicShelfPlaylists(playlistPage.getOrNull()?.entries.orEmpty()),
            albums = albumPage.getOrNull()?.entries.orEmpty().take(YOUTUBE_MUSIC_SHELF_LIMIT)
        )
    }

    private fun withAlbum(track: Track, album: YoutubeMusicLibraryEntry): Track = track.copy(
        artist = track.artist.ifBlank { album.subtitle },
        album = track.album.ifBlank { album.title },
        albumArtist = track.albumArtist.ifBlank { album.subtitle },
        albumBrowseId = track.albumBrowseId.ifBlank { album.browseId }
    )

    suspend fun fullTracks(session: YoutubeMusicSession, collection: YoutubeMusicCollection, browseId: String, language: String, album: YoutubeMusicLibraryEntry? = null): List<Track> {
        return collectYoutubeMusicPages(key = Track::id) { continuation ->
            val page = page(session, collection, language, browseId, continuation, album)
            YoutubeMusicPage(page.tracks, page.continuation)
        }
    }

    suspend fun importTracks(session: YoutubeMusicSession, browseId: String, title: String, tracks: List<Track>): Int = withContext(Dispatchers.IO) {
        if (tracks.isEmpty()) return@withContext 0
        val id = "ytm-" + UUID.nameUUIDFromBytes("${session.account.id}:$browseId".toByteArray(Charsets.UTF_8))
        val existing = playlists.load(id)
        val clean = tracks.map { it.copy(streamUrl = "", videoStreamUrl = "") }
        val valid = clean.filter { it.id.isNotBlank() && it.title.isNotBlank() }
        if (valid.isEmpty()) return@withContext 0
        if (existing == null) {
            playlists.createWithTracks(title, clean, playlistId = id).tracks.size
        } else {
            val knownIds = existing.tracks.mapTo(hashSetOf()) { it.id }
            val added = valid.distinctBy { it.id }.count { it.id !in knownIds }
            playlists.addTracks(id, clean)
            added
        }
    }

}

internal fun youtubeMusicShelfPlaylists(entries: List<YoutubeMusicLibraryEntry>): List<YoutubeMusicLibraryEntry> =
    entries
        .filterNot { entry -> entry.browseId in YOUTUBE_MUSIC_SYSTEM_PLAYLISTS }
        .sortedByDescending(YoutubeMusicLibraryEntry::owned)
        .take(YOUTUBE_MUSIC_SHELF_LIMIT)

private val YOUTUBE_MUSIC_SYSTEM_PLAYLISTS = setOf("VLLM", "VLSE")
private const val YOUTUBE_MUSIC_SHELF_LIMIT = 20
