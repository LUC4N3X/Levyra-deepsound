package com.luc4n3x.levyra.data.spotify

import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.SearchResults
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

internal class SpotifySearchClient(
    private val tokenProvider: SpotifyTokenProvider = SpotifyTokenProvider.get(),
    private val clientFactory: () -> OkHttpClient = { LevyraHttpClientFactory.externalIntegrations() }
) {
    suspend fun search(query: String, limit: Int = 12): SearchResults = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return@withContext SearchResults()

        val bearer = runCatching { tokenProvider.token() }.getOrNull() ?: return@withContext SearchResults()
        val payload = buildSearchPayload(trimmed, limit)
        val request = Request.Builder()
            .url(GRAPHQL_URL)
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Authorization", "Bearer $bearer")
            .header("User-Agent", SpotifyTokenProvider.WEB_USER_AGENT)
            .header("app-platform", "WebPlayer")
            .header("Origin", "https://open.spotify.com")
            .header("Referer", "https://open.spotify.com/")
            .header("Accept", "application/json")
            .build()

        val client = clientFactory()
        val responseJson = runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext SearchResults()
                val body = response.body.string()
                if (body.isEmpty()) return@withContext SearchResults()
                JSONObject(body)
            }
        }.getOrNull() ?: return@withContext SearchResults()

        parseSearchResults(responseJson)
    }

    private fun buildSearchPayload(query: String, limit: Int): JSONObject {
        val variables = JSONObject()
            .put("searchTerm", query)
            .put("offset", 0)
            .put("limit", limit.coerceIn(1, 25))
            .put("numberOfTopResults", 5)
            .put("includeAudiobooks", false)
            .put("includeArtistHasConcertsField", false)
            .put("includePreReleases", false)
            .put("includeLocalConcertsField", false)
            .put("includeAuthors", false)

        val persistedQuery = JSONObject()
            .put("version", 1)
            .put("sha256Hash", SEARCH_QUERY_HASH)

        val extensions = JSONObject().put("persistedQuery", persistedQuery)

        return JSONObject()
            .put("variables", variables)
            .put("operationName", SEARCH_OPERATION)
            .put("extensions", extensions)
    }

    private fun parseSearchResults(root: JSONObject): SearchResults {
        val searchV2 = root.optJSONObject("data")?.optJSONObject("searchV2") ?: return SearchResults()

        val tracks = parseTracks(searchV2.optJSONObject("tracksV2"))
        val artists = parseArtists(searchV2.optJSONObject("artists"))
        val albums = parseAlbums(searchV2.optJSONObject("albumsV2"))
        val playlists = parsePlaylists(searchV2.optJSONObject("playlists"))
        val topTrack = parseTopTrack(searchV2.optJSONObject("topResultsV2")) ?: tracks.firstOrNull()

        return SearchResults(
            topTrack = topTrack,
            songs = tracks,
            artists = artists,
            albums = albums,
            playlists = playlists
        )
    }

    private fun parseTopTrack(topResults: JSONObject?): Track? {
        if (topResults == null) return null
        val items = topResults.optJSONArray("itemsV2") ?: return null
        for (index in 0 until items.length()) {
            val itemWrapper = items.optJSONObject(index)?.optJSONObject("item") ?: continue
            val typeName = itemWrapper.optString("__typename")
            if (typeName.equals("TrackResponseWrapper", ignoreCase = true)) {
                val trackData = itemWrapper.optJSONObject("data") ?: continue
                return parseTrack(trackData)
            }
        }
        return null
    }

    private fun parseTracks(tracksObject: JSONObject?): List<Track> {
        if (tracksObject == null) return emptyList()
        val items = tracksObject.optJSONArray("items") ?: return emptyList()
        val results = mutableListOf<Track>()
        for (index in 0 until items.length()) {
            val itemWrapper = items.optJSONObject(index)?.optJSONObject("item") ?: continue
            val data = itemWrapper.optJSONObject("data") ?: continue
            val track = parseTrack(data) ?: continue
            results.add(track)
        }
        return results
    }

    private fun parseTrack(data: JSONObject): Track? {
        val uri = data.optString("uri").trim()
        val rawId = data.optString("id").trim()
        val trackId = rawId.ifBlank { uri.substringAfterLast(':') }
        if (trackId.isBlank()) return null

        val name = data.optString("name").trim()
        if (name.isBlank()) return null

        val artistsArray = data.optJSONObject("artists")?.optJSONArray("items")
        val artists = parseArtistNames(artistsArray)
        val artistLine = artists.joinToString(", ")

        val albumObj = data.optJSONObject("albumOfTrack")
        val albumName = albumObj?.optString("name").orEmpty().trim()
        val coverSources = albumObj?.optJSONObject("coverArt")?.optJSONArray("sources")
        val (thumbnailUrl, largeThumbnailUrl) = selectArtworkUrls(coverSources)

        val durationMs = data.optJSONObject("duration")?.optLong("totalMilliseconds", 0L) ?: 0L
        val explicit = data.optJSONObject("contentRating")?.optString("label")?.equals("EXPLICIT", ignoreCase = true) == true

        return Track(
            id = "spotify:$trackId",
            title = name,
            artist = artistLine,
            album = albumName,
            durationMs = durationMs,
            streamUrl = "",
            videoUrl = "",
            thumbnailUrl = thumbnailUrl,
            largeThumbnailUrl = largeThumbnailUrl,
            source = "spotify",
            moodTags = emptySet(),
            energy = 0,
            vocal = 0,
            replayScore = 0,
            cacheScore = 0,
            accentStart = 0,
            accentEnd = 0,
            explicit = explicit,
            metadataProvider = "spotify",
            metadataConfidence = 95,
            youtubeViewCount = -1L
        )
    }

    private fun parseArtists(artistsObject: JSONObject?): List<ArtistHit> {
        if (artistsObject == null) return emptyList()
        val items = artistsObject.optJSONArray("items") ?: return emptyList()
        val results = mutableListOf<ArtistHit>()
        for (index in 0 until items.length()) {
            val wrapper = items.optJSONObject(index) ?: continue
            val data = wrapper.optJSONObject("data") ?: continue
            val name = data.optJSONObject("profile")?.optString("name").orEmpty().trim()
            if (name.isBlank()) continue
            val uri = data.optString("uri").trim()
            val sources = data.optJSONObject("visuals")?.optJSONObject("avatarImage")?.optJSONArray("sources")
            val (thumbnail, _) = selectArtworkUrls(sources)
            results.add(
                ArtistHit(
                    name = name,
                    browseId = uri,
                    thumbnailUrl = thumbnail,
                    subscribers = "",
                    accentStart = 0,
                    accentEnd = 0,
                    officialArtwork = true
                )
            )
        }
        return results
    }

    private fun parseAlbums(albumsObject: JSONObject?): List<AlbumHit> {
        if (albumsObject == null) return emptyList()
        val items = albumsObject.optJSONArray("items") ?: return emptyList()
        val results = mutableListOf<AlbumHit>()
        for (index in 0 until items.length()) {
            val wrapper = items.optJSONObject(index) ?: continue
            val data = wrapper.optJSONObject("data") ?: continue
            val name = data.optString("name").trim()
            if (name.isBlank()) continue
            val uri = data.optString("uri").trim()
            val artistsArray = data.optJSONObject("artists")?.optJSONArray("items")
            val artistLine = parseArtistNames(artistsArray).joinToString(", ")
            val year = data.optJSONObject("date")?.optString("year").orEmpty().trim()
            val coverSources = data.optJSONObject("coverArt")?.optJSONArray("sources")
            val (thumbnail, _) = selectArtworkUrls(coverSources)
            results.add(
                AlbumHit(
                    browseId = uri,
                    title = name,
                    artist = artistLine,
                    year = year,
                    thumbnailUrl = thumbnail,
                    query = "$name $artistLine".trim(),
                    explicit = false
                )
            )
        }
        return results
    }

    private fun parsePlaylists(playlistsObject: JSONObject?): List<PlaylistHit> {
        if (playlistsObject == null) return emptyList()
        val items = playlistsObject.optJSONArray("items") ?: return emptyList()
        val results = mutableListOf<PlaylistHit>()
        for (index in 0 until items.length()) {
            val wrapper = items.optJSONObject(index) ?: continue
            val data = wrapper.optJSONObject("data") ?: continue
            val name = data.optString("name").trim()
            if (name.isBlank()) continue
            val uri = data.optString("uri").trim()
            val owner = data.optJSONObject("ownerV2")?.optJSONObject("data")?.optString("name").orEmpty().trim()
            val imageSources = data.optJSONObject("images")?.optJSONArray("items")?.optJSONObject(0)?.optJSONArray("sources")
            val (thumbnail, _) = selectArtworkUrls(imageSources)
            results.add(
                PlaylistHit(
                    playlistId = uri,
                    browseId = uri,
                    title = name,
                    author = owner,
                    thumbnailUrl = thumbnail,
                    trackCountLabel = ""
                )
            )
        }
        return results
    }

    private fun parseArtistNames(items: JSONArray?): List<String> {
        if (items == null) return emptyList()
        val names = mutableListOf<String>()
        for (index in 0 until items.length()) {
            val profile = items.optJSONObject(index)?.optJSONObject("profile") ?: continue
            val name = profile.optString("name").trim()
            if (name.isNotEmpty()) names.add(name)
        }
        return names
    }

    private fun selectArtworkUrls(sources: JSONArray?): Pair<String, String> {
        if (sources == null || sources.length() == 0) return "" to ""
        var smallestUrl = ""
        var smallestArea = Long.MAX_VALUE
        var largestUrl = ""
        var largestArea = -1L

        for (index in 0 until sources.length()) {
            val source = sources.optJSONObject(index) ?: continue
            val url = source.optString("url").trim()
            if (url.isEmpty()) continue
            val width = source.optLong("width", 0L).coerceAtLeast(0L)
            val height = source.optLong("height", 0L).coerceAtLeast(0L)
            val area = width * height
            if (area < smallestArea) {
                smallestArea = area
                smallestUrl = url
            }
            if (area >= largestArea) {
                largestArea = area
                largestUrl = url
            }
        }
        val thumb = if (smallestUrl.isNotEmpty()) smallestUrl else largestUrl
        val large = if (largestUrl.isNotEmpty()) largestUrl else smallestUrl
        return thumb to large
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val GRAPHQL_URL = "https://api-partner.spotify.com/pathfinder/v2/query"
        private const val SEARCH_OPERATION = "searchDesktop"
        private const val SEARCH_QUERY_HASH =
            "4801118d4a100f756e833d33984436a3899cff359c532f8fd3aaf174b60b3b49"
    }
}
