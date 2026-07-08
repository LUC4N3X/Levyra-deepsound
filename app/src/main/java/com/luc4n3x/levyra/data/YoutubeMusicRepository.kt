package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.security.GoogleApiKeyHeaders
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.AlbumDetail
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.CacheReport
import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.LevyraContentLocales
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.LevyraLocalizedDiscovery
import com.luc4n3x.levyra.domain.SearchResults
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import kotlin.math.absoluteValue

class YoutubeMusicRepository(private val context: Context? = null) {
    private val apiKey = BuildConfig.YOUTUBE_INNERTUBE_API_KEY
    private val clientVersion = "1.20260423.01.00"
    private val memory = LinkedHashMap<String, Track>()

    suspend fun search(query: String, limit: Int = 36, languageCode: String = LevyraLanguageCatalog.deviceDefault()): List<Track> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return@withContext emptyList()
        val remote = runCatching { searchInnerTube(cleanQuery, limit, languageCode) }.getOrDefault(emptyList())
        if (remote.isNotEmpty()) {
            remote.forEach { memory[it.id] = it }
            return@withContext remote
        }
        runCatching { searchYoutubeExtractor(cleanQuery, limit) }
            .getOrDefault(emptyList())
            .also { items -> items.forEach { memory[it.id] = it } }
    }

    /** First YouTube Music match for a query, used to make chart entries playable. */
    suspend fun searchOne(query: String, languageCode: String = LevyraLanguageCatalog.deviceDefault()): Track? = search(query, 1, languageCode).firstOrNull()

    suspend fun searchEverything(query: String, languageCode: String = LevyraLanguageCatalog.deviceDefault()): SearchResults = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return@withContext SearchResults()
        val root = runCatching { searchInnerTubeRaw(cleanQuery, languageCode) }.getOrNull() ?: return@withContext fallbackResults(cleanQuery, languageCode)
        val renderers = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicResponsiveListItemRenderer", renderers)
        val songs = LinkedHashMap<String, Track>()
        val artists = LinkedHashMap<String, ArtistHit>()
        val albums = LinkedHashMap<String, AlbumHit>()
        renderers.forEach { renderer ->
            val lines = extractFlexLines(renderer)
            val title = lines.firstOrNull()?.takeIf { it.isNotBlank() } ?: return@forEach
            val subtitleTokens = lines.drop(1).flatMap { it.split(" • ", " · ") }.map { it.trim() }
            val kind = subtitleTokens.firstOrNull()?.lowercase().orEmpty()
            val thumb = findBestThumbnail(renderer)
            when {
                kind.startsWith("artist") || kind.startsWith("artista") || kind.startsWith("artiste") || kind.startsWith("künstler") || kind.startsWith("kunstler") || kind.startsWith("artiest") || kind.startsWith("artysta") -> {
                    if (!artists.containsKey(title.lowercase())) {
                        val subs = subtitleTokens.firstOrNull { it.contains("scritt", ignoreCase = true) || it.contains("subscriber", ignoreCase = true) || it.contains("iscritt", ignoreCase = true) }.orEmpty()
                        val seed = stableSeed(title)
                        artists[title.lowercase()] = ArtistHit(
                            name = title,
                            subscribers = subs,
                            thumbnailUrl = upgradeThumbnail(thumb),
                            accentStart = palette(seed).first,
                            accentEnd = palette(seed).second
                        )
                    }
                }
                isAlbumLabel(kind) -> {
                    val albumArtist = subtitleTokens.getOrNull(1).orEmpty()
                    val year = subtitleTokens.firstNotNullOfOrNull { Regex("\\b(19|20)\\d{2}\\b").find(it)?.value }.orEmpty()
                    val key = "${title.lowercase()}|${albumArtist.lowercase()}"
                    if (!albums.containsKey(key)) {
                        albums[key] = AlbumHit(
                            title = title,
                            artist = albumArtist.ifBlank { "Album" },
                            year = year,
                            thumbnailUrl = upgradeThumbnail(thumb),
                            query = "$title $albumArtist",
                            browseId = extractAlbumBrowseId(renderer)
                        )
                    }
                }
                else -> {
                    val track = parseMusicRenderer(renderer, cleanQuery) ?: return@forEach
                    if (!songs.containsKey(track.id)) songs[track.id] = track
                }
            }
        }
        if (songs.isEmpty()) {
            val videoRenderers = mutableListOf<JSONObject>()
            collectObjectsByKey(root, "videoRenderer", videoRenderers)
            videoRenderers.forEach { renderer ->
                val track = parseVideoRenderer(renderer, cleanQuery) ?: return@forEach
                if (!songs.containsKey(track.id)) songs[track.id] = track
            }
        }
        songs.values.forEach { memory[it.id] = it }
        val songList = songs.values.toList()
        val results = SearchResults(
            topTrack = songList.firstOrNull(),
            songs = songList.take(20),
            artists = artists.values.take(8).toList(),
            albums = albums.values.take(10).toList()
        )
        if (results.isEmpty) fallbackResults(cleanQuery, languageCode) else results
    }

    private suspend fun fallbackResults(query: String, languageCode: String): SearchResults {
        val songs = search(query, 20, languageCode)
        return SearchResults(topTrack = songs.firstOrNull(), songs = songs)
    }

    private fun clientPayload(languageCode: String): JSONObject {
        val locale = LevyraContentLocales.forLanguage(languageCode)
        return JSONObject()
            .put("clientName", "WEB_REMIX")
            .put("clientVersion", clientVersion)
            .put("hl", locale.hl)
            .put("gl", locale.gl)
            .put("platform", "DESKTOP")
    }

    private fun searchInnerTubeRaw(query: String, languageCode: String): JSONObject? {
        val endpoint = "https://music.youtube.com/youtubei/v1/search?key=$apiKey&prettyPrint=false"
        val body = JSONObject()
            .put(
                "context",
                JSONObject().put("client", clientPayload(languageCode))
            )
            .put("query", query)
            .toString()
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 20000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Origin", "https://music.youtube.com")
            setRequestProperty("Referer", "https://music.youtube.com/search?q=${query.replace(" ", "+")}")
            setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36")
            setRequestProperty("X-Youtube-Client-Name", "67")
            setRequestProperty("X-Youtube-Client-Version", clientVersion)
            GoogleApiKeyHeaders.applyTo(this, context)
            setRequestProperty("Content-Length", bytes.size.toString())
        }
        connection.outputStream.use { it.write(bytes) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val response = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
        if (code !in 200..299) return null
        return JSONObject(response)
    }

    suspend fun home(
        queries: List<String> = LevyraContentLocales.forLanguage(LevyraLanguageCatalog.deviceDefault()).homeQueries,
        languageCode: String = LevyraLanguageCatalog.deviceDefault()
    ): List<Track> = withContext(Dispatchers.IO) {
        val results = queries.flatMap { query ->
            runCatching { search(query, 10, languageCode) }.getOrDefault(emptyList())
        }
        results.distinctBy { it.id }.take(40).also { items -> items.forEach { memory[it.id] = it } }
    }

    /** Real YouTube Music home feed parsed into titled sections, like the official app. */
    suspend fun homeFeed(languageCode: String = LevyraLanguageCatalog.deviceDefault()): List<HomeSection> = withContext(Dispatchers.IO) {
        val sections = runCatching { homeFeedInnerTube(languageCode) }.getOrDefault(emptyList())
        sections.forEach { section -> section.tracks.forEach { memory[it.id] = it } }
        sections
    }

    suspend fun homeAlbums(
        languageCode: String = LevyraLanguageCatalog.deviceDefault(),
        limit: Int = 10,
        seedQueries: List<String> = emptyList()
    ): List<AlbumHit> = withContext(Dispatchers.IO) {
        val personalizedAlbums = seedQueries
            .asSequence()
            .map { it.trim() }
            .filter { it.length >= 2 }
            .distinct()
            .take(8)
            .flatMap { query -> runCatching { searchAlbumHits(query, languageCode, limit) }.getOrDefault(emptyList()).asSequence() }
            .toList()
        val homeAlbums = if (personalizedAlbums.size >= limit) emptyList() else runCatching { homeAlbumFeedInnerTube(languageCode) }.getOrDefault(emptyList())
        val fallbackAlbums = if ((personalizedAlbums + homeAlbums).size >= limit) {
            emptyList()
        } else {
            albumRecommendationQueries(languageCode).flatMap { query ->
                runCatching { searchAlbumHits(query, languageCode, limit) }.getOrDefault(emptyList())
            }
        }
        (personalizedAlbums + homeAlbums + fallbackAlbums)
            .asSequence()
            .filter { it.title.isNotBlank() && it.artist.isNotBlank() && it.thumbnailUrl.isNotBlank() }
            .filter { it.browseId.isNotBlank() || it.query.isNotBlank() }
            .distinctBy { "${it.title.lowercase()}|${it.artist.lowercase()}" }
            .take(limit)
            .toList()
    }

    suspend fun albumDetail(album: AlbumHit, languageCode: String = LevyraLanguageCatalog.deviceDefault()): AlbumDetail = withContext(Dispatchers.IO) {
        val resolved = resolveAlbumHit(album, languageCode)
        val root = resolved.browseId.takeIf { it.isNotBlank() }?.let { requestMusicBrowseRoot(languageCode, it) }
        val headerAlbum = root?.let { parseAlbumHeader(it, resolved) } ?: resolved
        val cover = headerAlbum.thumbnailUrl.ifBlank { resolved.thumbnailUrl }
        val tracks = root?.let { parseAlbumTracks(it, headerAlbum.copy(thumbnailUrl = cover)) }.orEmpty()
        val fallbackTracks = if (tracks.isEmpty()) {
            searchInnerTube("${headerAlbum.title} ${headerAlbum.artist}", 24, languageCode)
                .map { track -> track.copy(album = headerAlbum.title, thumbnailUrl = track.thumbnailUrl.ifBlank { cover }, largeThumbnailUrl = track.largeThumbnailUrl.ifBlank { cover }) }
        } else {
            emptyList()
        }
        val finalTracks = (tracks + fallbackTracks)
            .distinctBy { it.id.ifBlank { "${it.title.lowercase()}|${it.artist.lowercase()}" } }
            .take(60)
        finalTracks.forEach { memory[it.id] = it }
        AlbumDetail(
            album = headerAlbum.copy(thumbnailUrl = cover, browseId = headerAlbum.browseId.ifBlank { resolved.browseId }),
            description = root?.let { parseAlbumDescription(it) }.orEmpty(),
            tracks = finalTracks
        )
    }

    private fun requestMusicHomeRoot(languageCode: String): JSONObject? = requestMusicBrowseRoot(languageCode, "FEmusic_home")

    private fun requestMusicBrowseRoot(languageCode: String, browseId: String): JSONObject? {
        if (browseId.isBlank()) return null
        val endpoint = "https://music.youtube.com/youtubei/v1/browse?key=$apiKey&prettyPrint=false"
        val body = JSONObject()
            .put(
                "context",
                JSONObject().put("client", clientPayload(languageCode))
            )
            .put("browseId", browseId)
            .toString()
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 20000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Origin", "https://music.youtube.com")
            setRequestProperty("Referer", "https://music.youtube.com/browse/$browseId")
            setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36")
            setRequestProperty("X-Youtube-Client-Name", "67")
            setRequestProperty("X-Youtube-Client-Version", clientVersion)
            GoogleApiKeyHeaders.applyTo(this, context)
            setRequestProperty("Content-Length", bytes.size.toString())
        }
        connection.outputStream.use { it.write(bytes) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val response = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
        if (code !in 200..299) return null
        return JSONObject(response)
    }

    private fun homeFeedInnerTube(languageCode: String): List<HomeSection> {
        val root = requestMusicHomeRoot(languageCode) ?: return emptyList()
        val shelves = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicCarouselShelfRenderer", shelves)
        val sections = mutableListOf<HomeSection>()
        shelves.forEach { shelf ->
            val title = shelfTitle(shelf).ifBlank { "Per te" }
            val contents = shelf.optJSONArray("contents") ?: JSONArray()
            val tracks = LinkedHashMap<String, Track>()
            for (i in 0 until contents.length()) {
                val item = contents.optJSONObject(i) ?: continue
                val track = parseCarouselItem(item)
                if (track != null && !tracks.containsKey(track.id)) tracks[track.id] = track
            }
            if (tracks.size >= 3) sections += HomeSection(title, tracks.values.take(20))
        }
        return sections.take(10)
    }

    private fun homeAlbumFeedInnerTube(languageCode: String): List<AlbumHit> {
        val root = requestMusicHomeRoot(languageCode) ?: return emptyList()
        val shelves = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicCarouselShelfRenderer", shelves)
        val albums = LinkedHashMap<String, AlbumHit>()
        shelves.forEach { shelf ->
            val contents = shelf.optJSONArray("contents") ?: JSONArray()
            for (i in 0 until contents.length()) {
                val item = contents.optJSONObject(i) ?: continue
                val album = parseCarouselAlbumHit(item) ?: continue
                val key = "${album.title.lowercase()}|${album.artist.lowercase()}"
                if (!albums.containsKey(key)) albums[key] = album
            }
        }
        return albums.values.take(12).toList()
    }

    private fun shelfTitle(shelf: JSONObject): String {
        return shelf.optJSONObject("header")
            ?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
            ?.optJSONObject("title")
            ?.optJSONArray("runs")
            ?.joinText()
            .orEmpty()
            .trim()
    }

    private val excludedTypes = setOf(
        "album", "playlist", "artist", "ep", "podcast", "episode", "channel", "profile", "mix",
        "artista", "canale", "profilo", "canción", "cancion", "artiste", "künstler", "kunstler",
        "álbum", "albumo", "artiest", "artysta", "artis", "canal", "chaîne", "kanal"
    )

    private fun albumRecommendationQueries(languageCode: String): List<String> {
        return when (LevyraLanguageCatalog.normalize(languageCode)) {
            "it" -> listOf(
                "nuovi album italiani",
                "album italiani",
                "album pop italiani",
                "album rap italiani",
                "album indie italiani"
            )
            "es" -> listOf("nuevos álbumes", "álbumes populares", "álbumes pop", "álbumes rap")
            "fr" -> listOf("nouveaux albums", "albums populaires", "albums pop", "albums rap")
            "de" -> listOf("neue alben", "beliebte alben", "pop alben", "rap alben")
            "pt" -> listOf("novos álbuns", "álbuns populares", "álbuns pop", "álbuns rap")
            else -> listOf("new albums", "popular albums", "pop albums", "rap albums", "indie albums")
        }
    }

    private fun searchAlbumHits(query: String, languageCode: String, limit: Int): List<AlbumHit> {
        val root = searchInnerTubeRaw(query, languageCode) ?: return emptyList()
        val albums = LinkedHashMap<String, AlbumHit>()
        val renderers = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicResponsiveListItemRenderer", renderers)
        renderers.forEach { renderer ->
            val album = parseAlbumHit(renderer) ?: return@forEach
            val key = "${album.title.lowercase()}|${album.artist.lowercase()}"
            if (!albums.containsKey(key)) albums[key] = album.copy(query = "${album.title} ${album.artist}")
        }
        val twoRows = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicTwoRowItemRenderer", twoRows)
        twoRows.forEach { renderer ->
            val album = parseTwoRowAlbumHit(renderer) ?: return@forEach
            val key = "${album.title.lowercase()}|${album.artist.lowercase()}"
            if (!albums.containsKey(key)) albums[key] = album.copy(query = "${album.title} ${album.artist}")
        }
        return albums.values.take(limit).toList()
    }

    private fun parseCarouselAlbumHit(item: JSONObject): AlbumHit? {
        item.optJSONObject("musicResponsiveListItemRenderer")?.let { renderer -> return parseAlbumHit(renderer) }
        val two = item.optJSONObject("musicTwoRowItemRenderer") ?: return null
        return parseTwoRowAlbumHit(two)
    }

    private fun parseTwoRowAlbumHit(two: JSONObject): AlbumHit? {
        val title = two.optJSONObject("title")?.optJSONArray("runs")?.joinText().orEmpty().trim()
        if (title.isBlank()) return null
        val subtitle = two.optJSONObject("subtitle")?.optJSONArray("runs")?.joinText().orEmpty()
        val tokens = subtitle.split(" • ", " · ", " - ").map { it.trim() }.filter { it.isNotBlank() }
        val kind = tokens.firstOrNull().orEmpty()
        if (!isAlbumLabel(kind)) return null
        val artist = tokens.drop(1).firstOrNull { isAlbumArtistToken(it) } ?: return null
        val year = tokens.firstNotNullOfOrNull { Regex("\\b(19|20)\\d{2}\\b").find(it)?.value }.orEmpty()
        val thumbnail = findBestThumbnail(two)
        if (thumbnail.isBlank()) return null
        return AlbumHit(
            title = title.cleanLabel(),
            artist = artist.cleanLabel(),
            year = year,
            thumbnailUrl = upgradeThumbnail(thumbnail),
            query = "$title $artist",
            browseId = extractAlbumBrowseId(two)
        )
    }

    private fun parseAlbumHit(renderer: JSONObject): AlbumHit? {
        val lines = extractFlexLines(renderer)
        val title = lines.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
        val tokens = lines.drop(1).flatMap { it.split(" • ", " · ", " - ") }.map { it.trim() }.filter { it.isNotBlank() }
        val kind = tokens.firstOrNull().orEmpty()
        if (!isAlbumLabel(kind)) return null
        val artist = tokens.drop(1).firstOrNull { isAlbumArtistToken(it) } ?: return null
        val year = tokens.firstNotNullOfOrNull { Regex("\\b(19|20)\\d{2}\\b").find(it)?.value }.orEmpty()
        val thumbnail = findBestThumbnail(renderer)
        if (thumbnail.isBlank()) return null
        return AlbumHit(
            title = title.cleanLabel(),
            artist = artist.cleanLabel(),
            year = year,
            thumbnailUrl = upgradeThumbnail(thumbnail),
            query = "$title $artist",
            browseId = extractAlbumBrowseId(renderer)
        )
    }

    private fun isAlbumLabel(token: String): Boolean {
        val normalized = token.trim().lowercase()
        return normalized == "album" || normalized == "álbum" || normalized == "albumo"
    }

    private fun isAlbumArtistToken(token: String): Boolean {
        val normalized = token.trim().lowercase()
        if (normalized.isBlank()) return false
        if (normalized in typeLabels) return false
        if (normalized.matches(Regex("\\d{4}"))) return false
        if (normalized.matches(Regex("\\d+:\\d{2}"))) return false
        if (normalized.contains("song") || normalized.contains("brani") || normalized.contains("songs")) return false
        return true
    }

    private fun resolveAlbumHit(album: AlbumHit, languageCode: String): AlbumHit {
        if (album.browseId.isNotBlank()) return album
        val query = album.query.ifBlank { "${album.title} ${album.artist}" }.trim()
        val candidates = searchAlbumHits(query, languageCode, 8)
        val normalizedTitle = album.title.trim().lowercase()
        val normalizedArtist = album.artist.trim().lowercase()
        val exact = candidates.firstOrNull { candidate ->
            candidate.title.trim().lowercase() == normalizedTitle && candidate.artist.trim().lowercase() == normalizedArtist && candidate.browseId.isNotBlank()
        }
        val sameTitle = candidates.firstOrNull { candidate ->
            candidate.title.trim().lowercase() == normalizedTitle && candidate.browseId.isNotBlank()
        }
        val withBrowse = exact ?: sameTitle ?: candidates.firstOrNull { it.browseId.isNotBlank() }
        return withBrowse?.let { found ->
            album.copy(
                title = found.title.ifBlank { album.title },
                artist = found.artist.ifBlank { album.artist },
                year = found.year.ifBlank { album.year },
                thumbnailUrl = found.thumbnailUrl.ifBlank { album.thumbnailUrl },
                query = found.query.ifBlank { album.query },
                browseId = found.browseId
            )
        } ?: album
    }

    private fun parseAlbumHeader(root: JSONObject, fallback: AlbumHit): AlbumHit {
        val headers = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicDetailHeaderRenderer", headers)
        collectObjectsByKey(root, "musicResponsiveHeaderRenderer", headers)
        collectObjectsByKey(root, "musicEditablePlaylistDetailHeaderRenderer", headers)
        val header = headers.firstOrNull()
        val title = header?.optJSONObject("title")?.optJSONArray("runs")?.joinText().orEmpty().ifBlank { fallback.title }
        val subtitles = listOf(
            header?.optJSONObject("subtitle")?.optJSONArray("runs")?.joinText().orEmpty(),
            header?.optJSONObject("secondSubtitle")?.optJSONArray("runs")?.joinText().orEmpty()
        ).filter { it.isNotBlank() }.joinToString(" • ")
        val tokens = subtitles.split(" • ", " · ", " - ").map { it.trim() }.filter { it.isNotBlank() }
        val artist = tokens.firstOrNull { isAlbumArtistToken(it) } ?: fallback.artist
        val year = tokens.firstNotNullOfOrNull { Regex("\\b(19|20)\\d{2}\\b").find(it)?.value }.orEmpty().ifBlank { fallback.year }
        val thumbnail = header?.let { findBestThumbnail(it) }.orEmpty().ifBlank { fallback.thumbnailUrl }
        val browseId = fallback.browseId.ifBlank { root.optString("browseId") }
        return fallback.copy(
            title = title.cleanLabel(),
            artist = artist.cleanLabel(),
            year = year,
            thumbnailUrl = upgradeThumbnail(thumbnail),
            query = "${title.cleanLabel()} ${artist.cleanLabel()}",
            browseId = browseId
        )
    }

    private fun parseAlbumDescription(root: JSONObject): String {
        val shelves = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicDescriptionShelfRenderer", shelves)
        collectObjectsByKey(root, "descriptionShelfRenderer", shelves)
        shelves.forEach { shelf ->
            val text = shelf.optJSONObject("description")?.optJSONArray("runs")?.joinText().orEmpty()
                .ifBlank { shelf.optJSONObject("description")?.optString("simpleText").orEmpty() }
                .cleanAlbumDescription()
            if (text.length >= 12) return text
        }
        return ""
    }

    private fun parseAlbumTracks(root: JSONObject, album: AlbumHit): List<Track> {
        val renderers = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicResponsiveListItemRenderer", renderers)
        return renderers.mapNotNull { renderer -> parseAlbumTrackRenderer(renderer, album) }
            .distinctBy { it.id.ifBlank { "${it.title.lowercase()}|${it.artist.lowercase()}" } }
    }

    private fun parseAlbumTrackRenderer(renderer: JSONObject, album: AlbumHit): Track? {
        val videoId = renderer.optJSONObject("playlistItemData")?.optString("videoId").orEmpty()
            .ifBlank { extractPrimaryMusicVideoId(renderer) }
        if (videoId.isBlank()) return null
        val lines = extractFlexLines(renderer)
        val title = lines.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
        if (isAlbumLabel(title)) return null
        val tokens = lines.drop(1).flatMap { it.split(" • ", " · ", " - ") }.map { it.trim() }.filter { it.isNotBlank() }
        val artist = tokens.firstOrNull { token ->
            val normalized = token.lowercase()
            !isTypeLabel(token) && !isAlbumLabel(token) && !normalized.matches(Regex("\\d{4}")) && !normalized.matches(Regex("\\d+:\\d{2}(?::\\d{2})?")) && !normalized.contains("views") && !normalized.contains("visualizz")
        } ?: album.artist
        val thumbnail = findBestThumbnail(renderer).ifBlank { album.thumbnailUrl }
        return buildTrack(
            id = videoId,
            title = title,
            artist = artist,
            album = album.title,
            durationMs = extractDuration(renderer.toString()),
            thumbnailUrl = thumbnail,
            largeThumbnailUrl = upgradeThumbnail(thumbnail),
            videoUrl = "https://www.youtube.com/watch?v=$videoId",
            query = album.query.ifBlank { "${album.title} ${album.artist}" },
            source = "YouTube Music Album"
        )
    }

    private fun extractAlbumBrowseId(renderer: JSONObject): String {
        val endpoints = mutableListOf<JSONObject>()
        collectObjectsByKey(renderer, "browseEndpoint", endpoints)
        return endpoints.firstNotNullOfOrNull { endpoint ->
            val browseId = endpoint.optString("browseId").orEmpty()
            val pageType = endpoint.optJSONObject("browseEndpointContextSupportedConfigs")
                ?.optJSONObject("browseEndpointContextMusicConfig")
                ?.optString("pageType")
                .orEmpty()
            browseId.takeIf { it.startsWith("MPRE", ignoreCase = true) || pageType.equals("MUSIC_PAGE_TYPE_ALBUM", ignoreCase = true) }
        }.orEmpty()
    }

    fun searchSuggestions(query: String, languageCode: String = LevyraLanguageCatalog.deviceDefault()): List<String> {
        if (query.isBlank()) return emptyList()
        val locale = LevyraContentLocales.forLanguage(languageCode)
        val url = "https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&hl=${locale.hl}&gl=${locale.gl}&q=${java.net.URLEncoder.encode(query, "UTF-8")}"
        val remote = runCatching {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val root = JSONArray(response)
            val suggestions = root.optJSONArray(1) ?: return@runCatching emptyList()
            val result = mutableListOf<String>()
            for (i in 0 until suggestions.length()) {
                result += suggestions.optString(i)
            }
            result
        }.getOrDefault(emptyList())
        return LevyraLocalizedDiscovery.suggestions(query, locale.languageCode, remote)
    }

    private fun parseCarouselItem(item: JSONObject): Track? {
        item.optJSONObject("musicResponsiveListItemRenderer")?.let { return parseMusicRenderer(it, "home") }
        val two = item.optJSONObject("musicTwoRowItemRenderer") ?: return null
        val title = two.optJSONObject("title")?.optJSONArray("runs")?.joinText().orEmpty().trim()
        if (title.isBlank()) return null
        val videoId = firstWatchVideoId(two).ifBlank { return null }
        val subtitle = two.optJSONObject("subtitle")?.optJSONArray("runs")?.joinText().orEmpty()
        
        val tokens = subtitle.split(" • ", " · ", " - ").map { it.trim() }
        if (tokens.isNotEmpty() && tokens[0].lowercase() in excludedTypes) return null
        
        val artist = tokens.firstOrNull()?.takeIf { it.isNotBlank() && it.lowercase() !in typeLabels } ?: "YouTube Music"
        val thumbnail = findBestThumbnail(two)
        return buildTrack(
            id = videoId,
            title = title,
            artist = artist,
            album = "YouTube Music",
            durationMs = 0L,
            thumbnailUrl = thumbnail,
            largeThumbnailUrl = upgradeThumbnail(thumbnail),
            videoUrl = "https://www.youtube.com/watch?v=$videoId",
            query = "home",
            source = "YouTube Music"
        )
    }

    private fun firstWatchVideoId(renderer: JSONObject): String {
        val endpoints = mutableListOf<JSONObject>()
        collectObjectsByKey(renderer, "watchEndpoint", endpoints)
        endpoints.forEach { endpoint ->
            if (endpoint.has("playlistId")) return@forEach
            val id = endpoint.optString("videoId")
            if (id.isNotBlank()) return id
        }
        return ""
    }

    fun cachedTracks(): List<Track> = memory.values.toList()

    fun replace(track: Track) {
        memory[track.id] = track
    }

    fun cacheReport(): CacheReport {
        val all = memory.values.toList()
        val resolved = all.count { it.streamUrl.isNotBlank() }
        return CacheReport(
            offlineReady = resolved,
            smartCached = all.count { it.cacheScore >= 70 },
            nextPreload = all.take(6).count(),
            totalTracks = all.size
        )
    }

    private fun searchInnerTube(query: String, limit: Int, languageCode: String): List<Track> {
        val endpoint = "https://music.youtube.com/youtubei/v1/search?key=$apiKey&prettyPrint=false"
        val body = JSONObject()
            .put(
                "context",
                JSONObject().put("client", clientPayload(languageCode))
            )
            .put("query", query)
            .toString()
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 20000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Origin", "https://music.youtube.com")
            setRequestProperty("Referer", "https://music.youtube.com/search?q=${query.replace(" ", "+")}")
            setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36")
            setRequestProperty("X-Youtube-Client-Name", "67")
            setRequestProperty("X-Youtube-Client-Version", clientVersion)
            GoogleApiKeyHeaders.applyTo(this, context)
            setRequestProperty("Content-Length", bytes.size.toString())
        }
        connection.outputStream.use { it.write(bytes) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val response = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
        if (code !in 200..299) return emptyList()
        val root = JSONObject(response)
        val renderers = mutableListOf<JSONObject>()
        collectObjectsByKey(root, "musicResponsiveListItemRenderer", renderers)
        val tracks = LinkedHashMap<String, Track>()
        renderers.forEach { renderer ->
            val track = parseMusicRenderer(renderer, query)
            if (track != null && !tracks.containsKey(track.id)) tracks[track.id] = track
        }
        if (tracks.isEmpty()) {
            val videoRenderers = mutableListOf<JSONObject>()
            collectObjectsByKey(root, "videoRenderer", videoRenderers)
            videoRenderers.forEach { renderer ->
                val track = parseVideoRenderer(renderer, query)
                if (track != null && !tracks.containsKey(track.id)) tracks[track.id] = track
            }
        }
        return tracks.values.take(limit)
    }

    private fun searchYoutubeExtractor(query: String, limit: Int): List<Track> {
        NewPipeRuntime.ensure()
        val service = org.schabi.newpipe.extractor.ServiceList.YouTube
        val factory = service.searchQHFactory
        val musicSongsFilter = findExtractorFilterItem(
            factory.getAvailableContentFilter(),
            org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory.MUSIC_SONGS
        )
        val handler = if (musicSongsFilter != null) {
            factory.fromQuery(
                query,
                mutableListOf(musicSongsFilter),
                mutableListOf<org.schabi.newpipe.extractor.search.filter.FilterItem>()
            )
        } else {
            factory.fromQuery(query)
        }
        val info = org.schabi.newpipe.extractor.search.SearchInfo.getInfo(service, handler)
        return info.relatedItems
            .filterIsInstance<org.schabi.newpipe.extractor.stream.StreamInfoItem>()
            .mapNotNull { item ->
                val id = extractVideoId(item.url).ifBlank { stableId(item.url) }
                if (id.isBlank()) return@mapNotNull null
                val thumbnail = item.thumbnailUrl.orEmpty()
                buildTrack(
                    id = id,
                    title = item.name,
                    artist = item.uploaderName.orEmpty(),
                    album = "YouTube Music",
                    durationMs = secondsToMs(item.duration),
                    thumbnailUrl = thumbnail,
                    largeThumbnailUrl = upgradeThumbnail(thumbnail),
                    videoUrl = item.url,
                    query = query,
                    source = "PipePipeExtractor Search"
                )
            }
            .distinctBy { it.id }
            .take(limit)
    }

    private fun findExtractorFilterItem(
        filter: org.schabi.newpipe.extractor.search.filter.Filter,
        name: String
    ): org.schabi.newpipe.extractor.search.filter.FilterItem? {
        return filter.filterGroups
            .asSequence()
            .flatMap { group -> group.filterItems.asSequence() }
            .firstOrNull { item -> item.name.equals(name, ignoreCase = true) }
    }

    private val typeLabels = setOf(
        "song", "video", "album", "playlist", "artist", "single", "ep", "episode", "podcast",
        "brano", "canzone", "video musicale", "video ufficiale", "artista", "singolo", "episodio",
        "canción", "cancion", "artiste", "chanson", "titre", "künstler", "kunstler", "lied", "nummer",
        "utwór", "piosenka", "melodie", "τραγούδι", "låt", "sang", "píseň", "skladba", "пісня"
    )

    private fun isTypeLabel(token: String): Boolean = token.trim().lowercase() in typeLabels

    private fun parseMusicRenderer(renderer: JSONObject, query: String): Track? {
        val lines = extractFlexLines(renderer)
        val title = lines.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
        val videoId = extractPrimaryMusicVideoId(renderer).takeIf { it.isNotBlank() } ?: return null
        val allText = renderer.toString()
        val duration = extractDuration(allText)
        
        val subtitleLines = lines.drop(1)
        val tokens = subtitleLines.flatMap { it.split(" • ", " · ", " - ") }.map { it.trim() }
        if (tokens.isNotEmpty() && tokens[0].lowercase() in excludedTypes) return null
        
        val artist = tokens
            .firstOrNull { token -> token.isNotBlank() && !isTypeLabel(token) && !token.matches(Regex("\\d+:\\d{2}")) }
            ?: "YouTube Music"
        val thumbnail = findBestThumbnail(renderer)
        return buildTrack(
            id = videoId,
            title = title,
            artist = artist,
            album = lines.drop(1).getOrNull(1)?.takeIf { it.isNotBlank() } ?: "YouTube Music",
            durationMs = duration,
            thumbnailUrl = thumbnail,
            largeThumbnailUrl = upgradeThumbnail(thumbnail),
            videoUrl = "https://www.youtube.com/watch?v=$videoId",
            query = query,
            source = "YouTube Music"
        )
    }

    private fun extractPrimaryMusicVideoId(renderer: JSONObject): String {
        renderer.optJSONObject("playlistItemData")?.optString("videoId")?.takeIf { it.isNotBlank() }?.let { return it }
        val columns = renderer.optJSONArray("flexColumns") ?: JSONArray()
        for (i in 0 until columns.length()) {
            val runs = columns.optJSONObject(i)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")
                ?: continue
            for (j in 0 until runs.length()) {
                val videoId = runs.optJSONObject(j)
                    ?.optJSONObject("navigationEndpoint")
                    ?.optJSONObject("watchEndpoint")
                    ?.optString("videoId")
                    .orEmpty()
                if (videoId.isNotBlank()) return videoId
            }
        }
        val playButtons = mutableListOf<JSONObject>()
        collectObjectsByKey(renderer, "musicPlayButtonRenderer", playButtons)
        playButtons.forEach { button ->
            val videoId = button.optJSONObject("playNavigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId")
                .orEmpty()
            if (videoId.isNotBlank()) return videoId
        }
        val watchEndpoints = mutableListOf<JSONObject>()
        collectObjectsByKey(renderer, "watchEndpoint", watchEndpoints)
        watchEndpoints.forEach { endpoint ->
            val videoId = endpoint.optString("videoId")
            if (videoId.isNotBlank()) return videoId
        }
        return ""
    }

    private fun parseVideoRenderer(renderer: JSONObject, query: String): Track? {
        val videoId = renderer.optString("videoId").takeIf { it.isNotBlank() } ?: return null
        val title = renderer.optJSONObject("title")?.optJSONArray("runs")?.joinText().orEmpty().ifBlank { return null }
        val artist = renderer.optJSONObject("ownerText")?.optJSONArray("runs")?.joinText().orEmpty().ifBlank { "YouTube" }
        val duration = renderer.optJSONObject("lengthText")?.optString("simpleText")?.durationToMs() ?: 0L
        val thumbnail = renderer.optJSONObject("thumbnail")?.optJSONArray("thumbnails")?.bestThumbnail().orEmpty()
        return buildTrack(
            id = videoId,
            title = title,
            artist = artist,
            album = "YouTube",
            durationMs = duration,
            thumbnailUrl = thumbnail,
            largeThumbnailUrl = upgradeThumbnail(thumbnail),
            videoUrl = "https://www.youtube.com/watch?v=$videoId",
            query = query,
            source = "YouTube"
        )
    }

    private fun buildTrack(
        id: String,
        title: String,
        artist: String,
        album: String,
        durationMs: Long,
        thumbnailUrl: String,
        largeThumbnailUrl: String,
        videoUrl: String,
        query: String,
        source: String
    ): Track {
        val seed = stableSeed("$id$title$artist$query")
        val normalized = "$title $artist $album $query".lowercase()
        val tags = buildSet {
            add("hit")
            if (normalized.contains("ital") || normalized.contains("sanremo")) add("italian")
            if (normalized.contains("rap") || normalized.contains("trap")) add("rap")
            if (normalized.contains("gym") || normalized.contains("workout") || normalized.contains("bass")) add("gym")
            if (normalized.contains("night") || normalized.contains("chill")) add("night")
            if (normalized.contains("focus") || normalized.contains("deep")) add("focus")
            if (normalized.contains("pop")) add("pop")
            if (normalized.contains("new")) add("new")
            if (isEmpty()) add("music")
        }
        return Track(
            id = id,
            title = title.cleanLabel(),
            artist = artist.cleanLabel(),
            album = album.cleanLabel(),
            durationMs = durationMs,
            streamUrl = "",
            videoUrl = videoUrl,
            thumbnailUrl = thumbnailUrl,
            largeThumbnailUrl = largeThumbnailUrl,
            source = source,
            moodTags = tags,
            energy = (45 + seed % 52).coerceIn(0, 100),
            vocal = (35 + (seed / 3) % 60).coerceIn(0, 100),
            replayScore = (62 + (seed / 7) % 38).coerceIn(0, 100),
            cacheScore = (48 + (seed / 11) % 50).coerceIn(0, 100),
            accentStart = palette(seed).first,
            accentEnd = palette(seed).second
        )
    }

    private fun collectObjectsByKey(value: Any?, key: String, out: MutableList<JSONObject>) {
        when (value) {
            is JSONObject -> {
                val keys = value.keys()
                while (keys.hasNext()) {
                    val current = keys.next()
                    val child = value.opt(current)
                    if (current == key && child is JSONObject) out += child
                    collectObjectsByKey(child, key, out)
                }
            }
            is JSONArray -> for (i in 0 until value.length()) collectObjectsByKey(value.opt(i), key, out)
        }
    }

    private fun findStringUnderKey(value: Any?, key: String): String? {
        when (value) {
            is JSONObject -> {
                val direct = value.optString(key).takeIf { it.isNotBlank() }
                if (direct != null) return direct
                val keys = value.keys()
                while (keys.hasNext()) {
                    val result = findStringUnderKey(value.opt(keys.next()), key)
                    if (!result.isNullOrBlank()) return result
                }
            }
            is JSONArray -> for (i in 0 until value.length()) {
                val result = findStringUnderKey(value.opt(i), key)
                if (!result.isNullOrBlank()) return result
            }
        }
        return null
    }

    private fun extractFlexLines(renderer: JSONObject): List<String> {
        val lines = mutableListOf<String>()
        val columns = renderer.optJSONArray("flexColumns") ?: JSONArray()
        for (i in 0 until columns.length()) {
            val text = columns.optJSONObject(i)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")
                ?.joinText()
                .orEmpty()
                .trim()
            if (text.isNotBlank()) lines += text
        }
        return lines.distinct()
    }

    private fun findBestThumbnail(renderer: JSONObject): String {
        val arrays = mutableListOf<JSONArray>()
        collectArraysByKey(renderer, "thumbnails", arrays)
        return arrays.asSequence().map { it.bestThumbnail() }.firstOrNull { it.isNotBlank() }.orEmpty()
    }

    private fun collectArraysByKey(value: Any?, key: String, out: MutableList<JSONArray>) {
        when (value) {
            is JSONObject -> {
                val keys = value.keys()
                while (keys.hasNext()) {
                    val current = keys.next()
                    val child = value.opt(current)
                    if (current == key && child is JSONArray) out += child
                    collectArraysByKey(child, key, out)
                }
            }
            is JSONArray -> for (i in 0 until value.length()) collectArraysByKey(value.opt(i), key, out)
        }
    }

    private fun extractDuration(text: String): Long {
        val match = Regex("\\b\\d{1,2}:\\d{2}(?::\\d{2})?\\b").find(text)?.value ?: return 0L
        return match.durationToMs()
    }

    private fun extractVideoId(url: String): String {
        val patterns = listOf(
            Regex("[?&]v=([^&]+)"),
            Regex("youtu\\.be/([^?&/]+)"),
            Regex("shorts/([^?&/]+)")
        )
        return patterns.firstNotNullOfOrNull { it.find(url)?.groupValues?.getOrNull(1) }.orEmpty()
    }

    private fun secondsToMs(seconds: Long): Long {
        return if (seconds > 0) seconds * 1000L else 0L
    }

    private fun JSONArray.joinText(): String {
        val parts = mutableListOf<String>()
        for (i in 0 until length()) {
            val text = optJSONObject(i)?.optString("text").orEmpty()
            if (text.isNotBlank()) parts += text
        }
        return parts.joinToString("").replace("  ", " ").trim()
    }

    private fun JSONArray.bestThumbnail(): String {
        var bestUrl = ""
        var bestScore = -1
        for (i in 0 until length()) {
            val item = optJSONObject(i) ?: continue
            val url = item.optString("url")
            val score = item.optInt("width", 0) * item.optInt("height", 0)
            if (url.isNotBlank() && score >= bestScore) {
                bestUrl = url
                bestScore = score
            }
        }
        return bestUrl
    }

    private fun String.durationToMs(): Long {
        val parts = split(":").mapNotNull { it.toLongOrNull() }
        return when (parts.size) {
            2 -> (parts[0] * 60L + parts[1]) * 1000L
            3 -> (parts[0] * 3600L + parts[1] * 60L + parts[2]) * 1000L
            else -> 0L
        }
    }

    private fun String.cleanLabel(): String {
        return replace("\\n", " ").replace(Regex("\\s+"), " ").trim()
    }

    private fun upgradeThumbnail(url: String): String {
        if (url.isBlank()) return url
        return url.replace(Regex("=w\\d+-h\\d+.*$"), "=w1200-h1200-l90-rj")
            .replace(Regex("=s\\d+.*$"), "=s1200")
    }

    private fun stableSeed(value: String): Int {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .take(4)
            .fold(0) { acc, byte -> (acc shl 8) or (byte.toInt() and 0xFF) }
            .absoluteValue
    }

    private fun stableId(value: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .take(8)
            .joinToString("") { "%02x".format(it) }
    }

    private fun palette(seed: Int): Pair<Int, Int> {
        val palettes = listOf(
            0xFF00E5FF.toInt() to 0xFF7B42FF.toInt(),
            0xFF1B5CFF.toInt() to 0xFFFF4FD8.toInt(),
            0xFFFF7A18.toInt() to 0xFF8E57FF.toInt(),
            0xFF00D4A6.toInt() to 0xFFFF3B5C.toInt(),
            0xFFFFB000.toInt() to 0xFF00E5FF.toInt()
        )
        return palettes[seed % palettes.size]
    }
}

internal fun String.cleanAlbumDescription(): String {
    if (isBlank()) return ""
    val normalized = replace("\\n", "\n").replace("\r\n", "\n").replace('\r', '\n')
    val cutoff = ALBUM_DESCRIPTION_ATTRIBUTION_PATTERNS
        .mapNotNull { pattern -> pattern.find(normalized)?.range?.first }
        .minOrNull()
    return normalized
        .let { text -> cutoff?.let { text.take(it) } ?: text }
        .replace(Regex("https?://\\S+"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private val ALBUM_DESCRIPTION_ATTRIBUTION_PATTERNS = listOf(
    Regex("\\b(?:da|dalla|from|de|von|source|fonte)\\s+wikipedia\\b", RegexOption.IGNORE_CASE),
    Regex("\\bwikipedia\\b", RegexOption.IGNORE_CASE),
    Regex("\\bcreative\\s+commons\\b", RegexOption.IGNORE_CASE),
    Regex("\\bcreativecommons\\.org\\b", RegexOption.IGNORE_CASE),
    Regex("\\bcc[- ]?by(?:[- ]?sa)?\\b", RegexOption.IGNORE_CASE)
)
