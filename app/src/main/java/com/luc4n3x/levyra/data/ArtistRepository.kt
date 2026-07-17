package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.security.GoogleApiKeyHeaders
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.ArtistProfile
import com.luc4n3x.levyra.domain.ArtistRelease
import com.luc4n3x.levyra.domain.artistIdentityKey
import com.luc4n3x.levyra.domain.artistSearchMatchScore
import com.luc4n3x.levyra.domain.primaryArtistSegment
import com.luc4n3x.levyra.domain.LevyraContentLocales
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.absoluteValue

internal data class ArtistHeaderArtwork(
    val portraitUrl: String,
    val bannerUrl: String
)

internal fun parseArtistHeaderArtwork(header: JSONObject?): ArtistHeaderArtwork {
    fun bestThumbnail(array: JSONArray?): String {
        if (array == null) return ""
        var bestUrl = ""
        var bestArea = -1L
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val url = item.optString("url").trim()
            if (url.isBlank()) continue
            val width = item.optLong("width", 0L)
            val height = item.optLong("height", 0L)
            val area = width * height
            if (area >= bestArea) {
                bestArea = area
                bestUrl = url
            }
        }
        return bestUrl
    }

    fun thumbnailFrom(container: JSONObject?): String {
        container ?: return ""
        val renderer = container.optJSONObject("musicThumbnailRenderer")
            ?: container.optJSONObject("croppedSquareThumbnailRenderer")
            ?: container
        val thumbnails = renderer.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
            ?: renderer.optJSONArray("thumbnails")
        return bestThumbnail(thumbnails)
    }

    val immersive = header?.optJSONObject("musicImmersiveHeaderRenderer")
    val visual = header?.optJSONObject("musicVisualHeaderRenderer")
    val detail = header?.optJSONObject("musicDetailHeaderRenderer")
    val responsive = header?.optJSONObject("musicResponsiveHeaderRenderer")

    val portrait = sequenceOf(
        thumbnailFrom(immersive?.optJSONObject("thumbnail")),
        thumbnailFrom(visual?.optJSONObject("foregroundThumbnail")),
        thumbnailFrom(detail?.optJSONObject("thumbnail")),
        thumbnailFrom(responsive?.optJSONObject("thumbnail"))
    ).firstOrNull { it.isNotBlank() }.orEmpty()

    val banner = sequenceOf(
        thumbnailFrom(immersive?.optJSONObject("backgroundThumbnail")),
        thumbnailFrom(visual?.optJSONObject("backgroundThumbnail")),
        thumbnailFrom(responsive?.optJSONObject("backgroundThumbnail"))
    ).firstOrNull { it.isNotBlank() }.orEmpty()

    return ArtistHeaderArtwork(
        portraitUrl = portrait,
        bannerUrl = banner
    )
}

class ArtistRepository(private val music: YoutubeMusicRepository, private val context: Context? = null) {
    private val apiKey = BuildConfig.YOUTUBE_INNERTUBE_API_KEY
    private val clientVersion = "1.20260423.01.00"
    private val preferences = context?.applicationContext?.let { LevyraPreferences(it) }
    private val memory = ConcurrentHashMap<String, ArtistProfile>()
    private val artistHitMemory = ConcurrentHashMap<String, ArtistHit>()

    private companion object {
        const val MAX_RELEASE_PAGES = 8
        val ALBUM_SECTION_WORDS = setOf("album", "albums", "álbum", "álbumes", "alben", "albumi", "альбом", "альбомы", "アルバム", "앨범")
        val SINGLE_SECTION_WORDS = setOf("single", "singles", "singol", "singoli", "sencillo", "sencillos", "ep", "eps")
        val VIDEO_SECTION_WORDS = setOf("video", "videos", "vídeo", "vídeos", "clip", "clips", "videoclip", "music video")
        val BIOGRAPHY_MUSIC_TERMS = setOf(
            "singer", "rapper", "musician", "songwriter", "composer", "record producer", "disc jockey", "music group", "musical group", "band", "duo",
            "cantante", "rapper", "musicista", "cantautore", "cantautrice", "compositore", "compositrice", "produttore discografico", "produttrice discografica", "disc jockey", "gruppo musicale", "duo musicale",
            "chanteur", "chanteuse", "rappeur", "rappeuse", "musicien", "musicienne", "auteur-compositeur", "compositeur", "compositrice", "producteur de musique", "productrice de musique", "groupe musical",
            "sänger", "sängerin", "rapper", "rapperin", "musiker", "musikerin", "songwriter", "komponist", "komponistin", "musikproduzent", "musikproduzentin", "musikgruppe", "band",
            "cantor", "cantora", "rapero", "rapera", "músico", "música", "compositor", "compositora", "productor musical", "productora musical", "grupo musical", "banda"
        )
        val BIOGRAPHY_TITLE_TERMS = setOf(
            "rapper", "singer", "musician", "band", "music group", "dj", "cantante", "musicista", "gruppo musicale", "chanteur", "chanteuse", "musicien", "musicienne", "sänger", "sängerin", "musiker", "musikerin", "rapero", "rapera", "cantor", "cantora"
        )
    }

    suspend fun profileFor(artistName: String): ArtistProfile? = withContext(Dispatchers.IO) {
        val clean = artistName.trim()
        if (clean.length < 2) return@withContext null
        memory[artistIdentityKey(clean)]?.let { return@withContext it }
        val resolvedArtist = runCatching { artistHitFor(clean) }.getOrNull()
        val profile = if (resolvedArtist != null && resolvedArtist.browseId.isNotBlank()) {
            runCatching { fetchProfile(resolvedArtist.browseId, resolvedArtist.name.ifBlank { clean }) }
                .getOrNull()
                ?.let { fetched ->
                    fetched.copy(
                        name = fetched.name.ifBlank { resolvedArtist.name.ifBlank { clean } },
                        thumbnailUrl = fetched.thumbnailUrl.ifBlank { resolvedArtist.thumbnailUrl }
                    )
                }
        } else {
            null
        }
        val resolved = profile ?: runCatching { fallbackProfile(clean, resolvedArtist) }.getOrNull()
        resolved?.also { profile ->
            memory[artistIdentityKey(clean)] = profile
            memory[artistIdentityKey(profile.name)] = profile
        }
    }

    suspend fun artistHitFor(artistName: String): ArtistHit? = withContext(Dispatchers.IO) {
        val clean = artistName.trim()
        if (clean.length < 2) return@withContext null
        val cacheKey = artistIdentityKey(clean)
        artistHitMemory[cacheKey]?.let { return@withContext it }
        val resolved = runCatching { resolveArtist(clean) }.getOrNull()
        resolved?.also { hit ->
            artistHitMemory[cacheKey] = hit
            artistHitMemory[artistIdentityKey(hit.name)] = hit
        }
    }

    suspend fun artistHit(browseId: String, fallbackName: String): ArtistHit? = withContext(Dispatchers.IO) {
        val cleanBrowseId = browseId.trim()
        val cleanName = fallbackName.trim()
        if (cleanBrowseId.isBlank() || cleanName.length < 2) return@withContext null
        val browseCacheKey = "browse:${cleanBrowseId.lowercase(Locale.ROOT)}"
        artistHitMemory[browseCacheKey]?.let { return@withContext it }
        memory[artistIdentityKey(cleanName)]
            ?.takeIf { it.browseId.equals(cleanBrowseId, ignoreCase = true) && it.thumbnailUrl.isNotBlank() }
            ?.let { profile ->
                val hit = ArtistHit(
                    name = profile.name,
                    subscribers = profile.subscribers,
                    thumbnailUrl = profile.thumbnailUrl,
                    accentStart = profile.accentStart,
                    accentEnd = profile.accentEnd,
                    browseId = profile.browseId
                )
                artistHitMemory[browseCacheKey] = hit
                return@withContext hit
            }
        val root = runCatching { postBrowse(cleanBrowseId) }.getOrNull() ?: return@withContext null
        val header = root.optJSONObject("header") ?: return@withContext null
        val resolvedName = headerText(header)
        if (resolvedName.isBlank() || artistIdentityKey(resolvedName) != artistIdentityKey(cleanName)) return@withContext null
        val artwork = parseArtistHeaderArtwork(header)
        val portrait = upgradeThumbnail(artwork.portraitUrl)
        if (portrait.isBlank()) return@withContext null
        val accent = palette(stableSeed(cleanBrowseId + resolvedName))
        val hit = ArtistHit(
            name = resolvedName,
            subscribers = extractSubscribers(header),
            thumbnailUrl = portrait,
            accentStart = accent.first,
            accentEnd = accent.second,
            browseId = cleanBrowseId
        )
        artistHitMemory[browseCacheKey] = hit
        hit
    }

    suspend fun profile(browseId: String, fallbackName: String): ArtistProfile? = withContext(Dispatchers.IO) {
        if (browseId.isBlank()) return@withContext profileFor(fallbackName)
        val cacheKey = artistIdentityKey(fallbackName)
        memory[cacheKey]?.takeIf { it.browseId.equals(browseId, ignoreCase = true) }?.let { return@withContext it }
        val resolved = runCatching { fetchProfile(browseId, fallbackName) }.getOrNull()
        resolved?.also { profile ->
            memory[cacheKey] = profile
            memory[artistIdentityKey(profile.name)] = profile
        }
    }

    private suspend fun resolveArtist(query: String): ArtistHit? {
        val languageCode = contentLanguage()
        val cleanQuery = query.trim()
        if (cleanQuery.length < 2) return null

        val primaryName = primaryArtistSegment(cleanQuery).ifBlank { cleanQuery }
        val primaryKey = artistIdentityKey(primaryName)
        val queryKey = artistIdentityKey(cleanQuery)

        val fullResults = runCatching {
            music.searchEverything(cleanQuery, languageCode).artists
        }.getOrDefault(emptyList())

        fullResults
            .firstOrNull { hit ->
                hit.browseId.isNotBlank() && artistIdentityKey(hit.name) == queryKey
            }
            ?.let { return it }

        val primaryResults = if (primaryKey == queryKey) {
            fullResults
        } else {
            runCatching {
                music.searchEverything(primaryName, languageCode).artists
            }.getOrDefault(emptyList())
        }

        primaryResults
            .firstOrNull { hit ->
                hit.browseId.isNotBlank() && artistIdentityKey(hit.name) == primaryKey
            }
            ?.let { return it }

        val filteredResults = runCatching {
            extractArtistSearchHits(postSearch(primaryName))
        }.getOrDefault(emptyList())

        val candidates = (fullResults + primaryResults + filteredResults)
            .filter { it.name.isNotBlank() && it.browseId.isNotBlank() }
            .distinctBy { it.browseId.lowercase(Locale.ROOT) }

        candidates
            .firstOrNull { artistIdentityKey(it.name) == queryKey }
            ?.let { return it }

        candidates
            .firstOrNull { artistIdentityKey(it.name) == primaryKey }
            ?.let { return it }

        val best = candidates
            .map { candidate -> candidate to artistSearchMatchScore(primaryName, candidate.name) }
            .maxByOrNull { it.second }
            ?: return null

        return best.first.takeIf { best.second >= 900 }
    }

    private fun extractArtistSearchHits(root: JSONObject): List<ArtistHit> {
        val renderers = mutableListOf<JSONObject>()
        collectByKey(root, "musicResponsiveListItemRenderer", renderers)
        val hits = LinkedHashMap<String, ArtistHit>()
        renderers.forEach { renderer ->
            val name = flexLines(renderer).firstOrNull().orEmpty().trim()
            if (name.isBlank()) return@forEach
            val reference = music.extractYoutubeMusicArtistReference(renderer, name) ?: return@forEach
            val resolvedName = reference.name.ifBlank { name }
            val browseId = reference.browseId
            val subtitle = flexLines(renderer).drop(1).joinToString(" · ")
            val seed = stableSeed(browseId + resolvedName)
            val accent = palette(seed)
            val key = browseId.ifBlank { artistIdentityKey(resolvedName) }
            hits.putIfAbsent(
                key,
                ArtistHit(
                    name = resolvedName,
                    subscribers = subtitle,
                    thumbnailUrl = upgradeThumbnail(bestThumbnail(thumbnailsOf(renderer))),
                    accentStart = accent.first,
                    accentEnd = accent.second,
                    browseId = browseId
                )
            )
        }
        return hits.values.toList()
    }

    private suspend fun fetchProfile(browseId: String, fallbackName: String): ArtistProfile? {
        val root = postBrowse(browseId)
        val header = root.optJSONObject("header")
        val name = headerText(header)
        if (name.isBlank()) return null
        val inlineBio = extractBio(root)
        val subscribers = extractSubscribers(header)
        val monthly = extractMonthlyListeners(root)
        val artwork = parseArtistHeaderArtwork(header)
        val thumb = upgradeThumbnail(artwork.portraitUrl)
        val banner = artwork.bannerUrl
        val songsPointer = findSongsPointer(root)
        val albumPointer = findReleasePointer(root, "Album")
        val singlePointer = findReleasePointer(root, "Singol")
        val videoPointer = findVideoPointer(root)
        val initialSongs = extractTopSongs(root)
        val expanded = coroutineScope {
            val songsJob = async { songsPointer?.let(::fetchSongs).orEmpty() }
            val albumsJob = async { albumPointer?.let(::fetchReleases).orEmpty() }
            val singlesJob = async { singlePointer?.let(::fetchReleases).orEmpty() }
            val videosJob = async { videoPointer?.let { fetchVideos(it, name) }.orEmpty() }
            val bioJob = async { inlineBio.ifBlank { fetchExternalBiography(name) } }
            ArtistExpandedSections(
                songs = songsJob.await(),
                albums = albumsJob.await(),
                singles = singlesJob.await(),
                videos = videosJob.await(),
                bio = bioJob.await()
            )
        }
        val songs = (initialSongs + expanded.songs).distinctBy { it.id }.take(100)
        val albums = mergeReleases(extractReleases(root, "Album"), expanded.albums)
        val singles = mergeReleases(extractReleases(root, "Singol"), expanded.singles)
        val videos = (extractVideos(root, name) + expanded.videos).distinctBy { it.id }.take(100)
        val related = extractRelatedArtists(root, name)
        val seed = stableSeed(browseId + name)
        val accent = palette(seed)
        if (name.isBlank() && songs.isEmpty()) return null
        return ArtistProfile(
            browseId = browseId,
            name = name,
            bio = expanded.bio,
            subscribers = subscribers,
            monthlyListeners = monthly,
            thumbnailUrl = thumb,
            bannerUrl = banner.ifBlank { thumb },
            topSongs = songs,
            albums = albums,
            singles = singles,
            accentStart = accent.first,
            accentEnd = accent.second,
            relatedArtists = related,
            videos = videos,
            shufflePlaylistId = findPlaylistIdByMarker(root, listOf("SHUFFLE"))
                .ifBlank { findPlaylistId(header?.optJSONObject("playButton")) },
            radioPlaylistId = findPlaylistIdByMarker(root, listOf("RADIO", "START_RADIO"))
                .ifBlank { findPlaylistId(header?.optJSONObject("startRadioButton")) },
            songsBrowseId = songsPointer?.browseId.orEmpty(),
            albumsBrowseId = albumPointer?.browseId.orEmpty(),
            albumsParams = albumPointer?.params.orEmpty(),
            singlesBrowseId = singlePointer?.browseId.orEmpty(),
            singlesParams = singlePointer?.params.orEmpty(),
            videosBrowseId = videoPointer?.browseId.orEmpty(),
            videosParams = videoPointer?.params.orEmpty()
        )
    }

    private fun extractRelatedArtists(root: JSONObject, selfName: String): List<ArtistHit> {
        val cards = mutableListOf<JSONObject>()
        collectByKey(root, "musicTwoRowItemRenderer", cards)
        val out = LinkedHashMap<String, ArtistHit>()
        cards.forEach { card ->
            val endpoint = card.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint") ?: return@forEach
            val browseId = endpoint.optString("browseId")
            val pageType = endpoint.optJSONObject("browseEndpointContextSupportedConfigs")
                ?.optJSONObject("browseEndpointContextMusicConfig")
                ?.optString("pageType")
                .orEmpty()
            if (!pageType.contains("ARTIST", ignoreCase = true)) return@forEach
            val name = card.optJSONObject("title")?.optJSONArray("runs")?.joinText().orEmpty().trim()
            if (name.isBlank() || name.equals(selfName, ignoreCase = true)) return@forEach
            val subtitle = card.optJSONObject("subtitle")?.optJSONArray("runs")?.joinText().orEmpty().trim()
            val thumb = bestThumbnail(thumbnailsOf(card))
            val seed = stableSeed(browseId + name)
            val accent = palette(seed)
            val key = name.lowercase()
            if (!out.containsKey(key)) {
                out[key] = ArtistHit(
                    name = name,
                    subscribers = subtitle,
                    thumbnailUrl = upgradeThumbnail(thumb),
                    accentStart = accent.first,
                    accentEnd = accent.second,
                    browseId = browseId
                )
            }
        }
        return out.values.take(12).toList()
    }

    private suspend fun fallbackProfile(requestedName: String, resolvedArtist: ArtistHit?): ArtistProfile {
        val languageCode = contentLanguage()
        val resolvedName = resolvedArtist?.name?.trim().orEmpty().ifBlank { requestedName.trim() }
        val songs = music.search(resolvedName, 18, languageCode)
            .filter { it.artist.contains(resolvedName, ignoreCase = true) }
            .ifEmpty { music.search(resolvedName, 12, languageCode) }
        val seed = stableSeed(resolvedArtist?.browseId.orEmpty() + resolvedName)
        val accent = palette(seed)
        val portrait = resolvedArtist?.thumbnailUrl.orEmpty()
        return ArtistProfile(
            browseId = resolvedArtist?.browseId.orEmpty(),
            name = resolvedName,
            bio = fetchExternalBiography(resolvedName),
            subscribers = resolvedArtist?.subscribers.orEmpty(),
            monthlyListeners = "",
            thumbnailUrl = portrait,
            bannerUrl = portrait,
            topSongs = songs.take(12),
            albums = emptyList(),
            singles = emptyList(),
            accentStart = accent.first,
            accentEnd = accent.second
        )
    }

    private fun extractBio(root: JSONObject): String {
        val sections = mutableListOf<JSONObject>()
        collectByKey(root, "musicDescriptionShelfRenderer", sections)
        sections.forEach { shelf ->
            val text = shelf.optJSONObject("description")?.optJSONArray("runs")?.joinText().orEmpty().trim()
            if (text.length > 24) return text
        }
        val descriptions = mutableListOf<JSONObject>()
        collectByKey(root, "description", descriptions)
        descriptions.forEach { node ->
            val text = node.optJSONArray("runs")?.joinText().orEmpty().trim()
            if (text.length > 80) return text
        }
        return ""
    }

    private suspend fun fetchExternalBiography(artistName: String): String = coroutineScope {
        val cleanName = artistName.trim()
        if (cleanName.length < 2) return@coroutineScope ""
        val languages = linkedSetOf(wikipediaLanguage(contentLanguage()), "en")
        val biographies = languages.map { language ->
            async(Dispatchers.IO) { fetchWikipediaBiography(cleanName, language) }
        }.map { it.await() }
        biographies.firstOrNull { it.isNotBlank() }.orEmpty()
    }

    private fun fetchWikipediaBiography(artistName: String, language: String): String {
        val query = URLEncoder.encode("$artistName music artist", StandardCharsets.UTF_8.name())
        val endpoint = "https://$language.wikipedia.org/w/api.php?action=query&generator=search&gsrsearch=$query&gsrnamespace=0&gsrlimit=6&prop=extracts&exintro=1&explaintext=1&redirects=1&format=json&formatversion=2"
        val root = runCatching { getJson(endpoint) }.getOrNull() ?: return ""
        val pages = root.optJSONObject("query")?.optJSONArray("pages") ?: return ""
        var bestText = ""
        var bestScore = Int.MIN_VALUE
        for (index in 0 until pages.length()) {
            val page = pages.optJSONObject(index) ?: continue
            val title = page.optString("title").trim()
            val extract = cleanBiography(page.optString("extract"))
            if (title.isBlank() || extract.length < 80 || isDisambiguationBiography(extract)) continue
            val baseTitle = title.substringBefore(" (").trim()
            var score = maxOf(
                artistSearchMatchScore(artistName, title),
                artistSearchMatchScore(artistName, baseTitle)
            )
            val normalizedExtract = extract.lowercase(Locale.ROOT)
            val normalizedTitle = title.lowercase(Locale.ROOT)
            val hasMusicRole = BIOGRAPHY_MUSIC_TERMS.any(normalizedExtract::contains) ||
                BIOGRAPHY_TITLE_TERMS.any(normalizedTitle::contains)
            if (!hasMusicRole) continue
            score += 260
            if (artistIdentityKey(baseTitle) == artistIdentityKey(artistName)) score += 300
            if (score > bestScore) {
                bestScore = score
                bestText = extract
            }
        }
        return bestText.takeIf { bestScore >= 500 }.orEmpty()
    }

    private fun getJson(endpoint: String): JSONObject {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 7_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Accept-Language", contentLanguage())
            setRequestProperty("User-Agent", "Levyra/${BuildConfig.VERSION_NAME} Android music client")
        }
        return try {
            val code = connection.responseCode
            if (code !in 200..299) return JSONObject()
            val response = BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)).use { it.readText() }
            JSONObject(response)
        } finally {
            connection.disconnect()
        }
    }

    private fun cleanBiography(value: String): String {
        val normalized = value
            .replace(Regex("\\s*\n+\\s*"), " ")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
        if (normalized.length <= 3_200) return normalized
        val cut = normalized.lastIndexOf('.', 3_200).takeIf { it >= 1_800 } ?: 3_200
        return normalized.substring(0, cut + if (cut < normalized.length && normalized[cut] == '.') 1 else 0).trim()
    }

    private fun isDisambiguationBiography(value: String): Boolean {
        val normalized = value.lowercase(Locale.ROOT)
        return normalized.contains("may refer to") ||
            normalized.contains("può riferirsi") ||
            normalized.contains("peut faire référence") ||
            normalized.contains("kann sich beziehen") ||
            normalized.contains("puede referirse")
    }

    private fun wikipediaLanguage(languageCode: String): String {
        return when (LevyraLanguageCatalog.normalize(languageCode).substringBefore('-')) {
            "zh" -> "zh"
            "pt" -> "pt"
            "uk" -> "uk"
            "ru" -> "ru"
            "tr" -> "tr"
            "el" -> "el"
            "sv" -> "sv"
            "da" -> "da"
            "cs" -> "cs"
            "pl" -> "pl"
            "ro" -> "ro"
            "nl" -> "nl"
            "de" -> "de"
            "fr" -> "fr"
            "es" -> "es"
            "it" -> "it"
            else -> "en"
        }
    }

    private fun extractSubscribers(header: JSONObject?): String {
        val text = header?.optJSONObject("subscriptionButton")
            ?.optJSONObject("subscribeButtonRenderer")
            ?.optJSONObject("subscriberCountText")
            ?.optJSONArray("runs")
            ?.joinText()
            .orEmpty()
            .trim()
        return text
    }

    private fun extractMonthlyListeners(root: JSONObject): String {
        val candidates = mutableListOf<JSONObject>()
        collectByKey(root, "subscriberCountText", candidates)
        candidates.forEach { node ->
            val text = node.optJSONArray("runs")?.joinText().orEmpty()
            if (text.contains("ascolt", ignoreCase = true) || text.contains("listener", ignoreCase = true)) return text.trim()
        }
        return ""
    }


    private fun extractTopSongs(root: JSONObject): List<Track> {
        val renderers = mutableListOf<JSONObject>()
        collectByKey(root, "musicResponsiveListItemRenderer", renderers)
        val tracks = LinkedHashMap<String, Track>()
        renderers.forEach { renderer ->
            val videoId = primaryVideoId(renderer)
            if (videoId.isBlank()) return@forEach
            val lines = flexLines(renderer)
            val title = lines.firstOrNull()?.takeIf { it.isNotBlank() } ?: return@forEach
            val artist = lines.getOrNull(1)?.split(" • ", " · ")?.firstOrNull()?.trim().orEmpty()
            val album = lines.getOrNull(2).orEmpty()
            val thumb = bestThumbnail(thumbnailsOf(renderer))
            val seed = stableSeed(videoId + title)
            val accent = palette(seed)
            if (!tracks.containsKey(videoId)) {
                tracks[videoId] = Track(
                    id = videoId,
                    title = title,
                    artist = artist.ifBlank { "YouTube Music" },
                    album = album.ifBlank { "YouTube Music" },
                    durationMs = durationOf(renderer.toString()),
                    streamUrl = "",
                    videoUrl = "https://www.youtube.com/watch?v=$videoId",
                    thumbnailUrl = thumb,
                    largeThumbnailUrl = upgradeThumbnail(thumb),
                    source = "YouTube Music",
                    moodTags = setOf("hit"),
                    energy = (45 + seed % 52).coerceIn(0, 100),
                    vocal = (35 + (seed / 3) % 60).coerceIn(0, 100),
                    replayScore = (62 + (seed / 7) % 38).coerceIn(0, 100),
                    cacheScore = (48 + (seed / 11) % 50).coerceIn(0, 100),
                    accentStart = accent.first,
                    accentEnd = accent.second
                )
            }
        }
        return tracks.values.take(20)
    }

    private fun extractReleases(root: JSONObject, kindHint: String): List<ArtistRelease> {
        val cards = mutableListOf<JSONObject>()
        collectByKey(root, "musicTwoRowItemRenderer", cards)
        val out = LinkedHashMap<String, ArtistRelease>()
        cards.forEach { card ->
            val title = card.optJSONObject("title")?.optJSONArray("runs")?.joinText().orEmpty().trim()
            if (title.isBlank()) return@forEach
            val subtitle = card.optJSONObject("subtitle")?.optJSONArray("runs")?.joinText().orEmpty().trim()
            if (kindHint.isNotBlank() && !releaseKindMatches(subtitle, kindHint)) return@forEach
            val navigation = card.optJSONObject("navigationEndpoint")
            val browseEndpoint = navigation?.optJSONObject("browseEndpoint")
            val browseId = browseEndpoint?.optString("browseId").orEmpty()
            val params = browseEndpoint?.optString("params").orEmpty()
            if (browseId.isBlank() || !browseId.startsWith("MPRE")) return@forEach
            val watchEndpoints = mutableListOf<JSONObject>()
            collectByKey(card, "watchEndpoint", watchEndpoints)
            val playlistId = watchEndpoints.firstNotNullOfOrNull { endpoint ->
                endpoint.optString("playlistId").takeIf { it.isNotBlank() }
            }.orEmpty()
            val thumb = bestThumbnail(thumbnailsOf(card))
            val year = Regex("\\b(19|20)\\d{2}\\b").find(subtitle)?.value.orEmpty()
            val key = browseId.ifBlank { title }
            if (!out.containsKey(key)) {
                out[key] = ArtistRelease(
                    browseId = browseId,
                    title = title,
                    subtitle = subtitle,
                    thumbnailUrl = upgradeThumbnail(thumb),
                    year = year,
                    params = params,
                    playlistId = playlistId,
                    explicit = card.toString().contains("MUSIC_ITEM_BADGE_EXPLICIT")
                )
            }
        }
        return out.values.take(100)
    }

    private data class ArtistSectionPointer(
        val browseId: String,
        val params: String
    )

    private data class ArtistExpandedSections(
        val songs: List<Track>,
        val albums: List<ArtistRelease>,
        val singles: List<ArtistRelease>,
        val videos: List<Track>,
        val bio: String
    )

    private fun findSongsPointer(root: JSONObject): ArtistSectionPointer? {
        val shelves = mutableListOf<JSONObject>()
        collectByKey(root, "musicShelfRenderer", shelves)
        shelves.forEach { shelf ->
            val renderers = mutableListOf<JSONObject>()
            collectByKey(shelf.optJSONArray("contents"), "musicResponsiveListItemRenderer", renderers)
            if (renderers.none { primaryVideoId(it).isNotBlank() }) return@forEach
            sectionPointer(shelf.optJSONObject("bottomEndpoint"))?.let { return it }
            sectionPointer(shelf.optJSONObject("title"))?.let { return it }
            sectionPointer(shelf.optJSONObject("header"))?.let { return it }
        }
        return null
    }

    private fun findReleasePointer(root: JSONObject, kindHint: String): ArtistSectionPointer? {
        val carousels = mutableListOf<JSONObject>()
        collectByKey(root, "musicCarouselShelfRenderer", carousels)
        carousels.forEach { carousel ->
            val title = sectionTitle(carousel)
            val matchingCards = extractReleases(carousel, kindHint)
            if (!releaseKindMatches(title, kindHint) && matchingCards.isEmpty()) return@forEach
            sectionPointer(carousel.optJSONObject("header"))?.let { return it }
            sectionPointer(carousel.optJSONObject("bottomEndpoint"))?.let { return it }
        }
        return null
    }

    private fun findVideoPointer(root: JSONObject): ArtistSectionPointer? {
        val carousels = mutableListOf<JSONObject>()
        collectByKey(root, "musicCarouselShelfRenderer", carousels)
        carousels.forEach { carousel ->
            val hasVideos = extractVideos(carousel, "YouTube Music").isNotEmpty()
            val title = sectionTitle(carousel).lowercase()
            if (!hasVideos && VIDEO_SECTION_WORDS.none { word -> title.contains(word) }) return@forEach
            sectionPointer(carousel.optJSONObject("header"))?.let { return it }
            sectionPointer(carousel.optJSONObject("bottomEndpoint"))?.let { return it }
        }
        return null
    }

    private fun sectionTitle(value: JSONObject): String {
        val header = value.optJSONObject("header")
        val direct = header?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
            ?.optJSONObject("title")
            ?.optJSONArray("runs")
            ?.joinText()
            .orEmpty()
            .trim()
        if (direct.isNotBlank()) return direct
        val titles = mutableListOf<JSONObject>()
        collectByKey(header, "title", titles)
        return titles.firstNotNullOfOrNull { node ->
            node.optJSONArray("runs")?.joinText()?.trim()?.takeIf { it.isNotBlank() }
                ?: node.optString("simpleText").trim().takeIf { it.isNotBlank() }
        }.orEmpty()
    }

    private fun releaseKindMatches(text: String, kindHint: String): Boolean {
        if (kindHint.isBlank()) return true
        val normalized = text.lowercase()
        val tokens = normalized.split(Regex("[^\\p{L}\\p{N}]+"))
            .filter(String::isNotBlank)
            .toSet()
        val words = if (kindHint.startsWith("Singol", ignoreCase = true)) SINGLE_SECTION_WORDS else ALBUM_SECTION_WORDS
        return words.any { word -> if (word.length <= 2) word in tokens else normalized.contains(word) }
    }

    private fun sectionPointer(value: Any?): ArtistSectionPointer? {
        val endpoints = mutableListOf<JSONObject>()
        collectByKey(value, "browseEndpoint", endpoints)
        endpoints.forEach { endpoint ->
            val browseId = endpoint.optString("browseId")
            val params = endpoint.optString("params")
            if (browseId.isNotBlank()) return ArtistSectionPointer(browseId, params)
        }
        return null
    }

    private fun fetchReleases(pointer: ArtistSectionPointer): List<ArtistRelease> {
        val releases = LinkedHashMap<String, ArtistRelease>()
        var response = runCatching { postBrowse(pointer.browseId, pointer.params) }.getOrDefault(JSONObject())
        var pages = 0
        while (response.length() > 0 && pages < MAX_RELEASE_PAGES) {
            extractReleases(response, "").forEach { release ->
                releases.putIfAbsent(release.browseId.ifBlank { "${release.title}|${release.year}" }, release)
            }
            val continuation = findContinuation(response)
            if (continuation.isBlank()) break
            response = runCatching { postBrowse("", continuation = continuation) }.getOrDefault(JSONObject())
            pages += 1
        }
        return releases.values.take(100).toList()
    }

    private fun fetchSongs(pointer: ArtistSectionPointer): List<Track> {
        val songs = LinkedHashMap<String, Track>()
        var response = runCatching { postBrowse(pointer.browseId, pointer.params) }.getOrDefault(JSONObject())
        var pages = 0
        while (response.length() > 0 && pages < MAX_RELEASE_PAGES) {
            extractTopSongs(response).forEach { track -> songs.putIfAbsent(track.id, track) }
            val continuation = findContinuation(response)
            if (continuation.isBlank() || songs.size >= 100) break
            response = runCatching { postBrowse("", continuation = continuation) }.getOrDefault(JSONObject())
            pages += 1
        }
        return songs.values.take(100).toList()
    }

    private fun fetchVideos(pointer: ArtistSectionPointer, artistName: String): List<Track> {
        val videos = LinkedHashMap<String, Track>()
        var response = runCatching { postBrowse(pointer.browseId, pointer.params) }.getOrDefault(JSONObject())
        var pages = 0
        while (response.length() > 0 && pages < MAX_RELEASE_PAGES) {
            extractVideos(response, artistName).forEach { track -> videos.putIfAbsent(track.id, track) }
            val continuation = findContinuation(response)
            if (continuation.isBlank() || videos.size >= 100) break
            response = runCatching { postBrowse("", continuation = continuation) }.getOrDefault(JSONObject())
            pages += 1
        }
        return videos.values.take(100).toList()
    }

    private fun mergeReleases(first: List<ArtistRelease>, second: List<ArtistRelease>): List<ArtistRelease> {
        val result = LinkedHashMap<String, ArtistRelease>()
        (first + second).forEach { release ->
            result.putIfAbsent(release.browseId.ifBlank { "${release.title.lowercase()}|${release.year}" }, release)
        }
        return result.values.take(100).toList()
    }

    private fun extractVideos(root: JSONObject, artistName: String): List<Track> {
        val cards = mutableListOf<JSONObject>()
        collectByKey(root, "musicTwoRowItemRenderer", cards)
        val result = LinkedHashMap<String, Track>()
        cards.forEach { card ->
            val endpoints = mutableListOf<JSONObject>()
            collectByKey(card, "watchEndpoint", endpoints)
            val endpoint = endpoints.firstOrNull { it.optString("videoId").isNotBlank() } ?: return@forEach
            val videoType = endpoint.optJSONObject("watchEndpointMusicSupportedConfigs")
                ?.optJSONObject("watchEndpointMusicConfig")
                ?.optString("musicVideoType")
                .orEmpty()
            if (!videoType.contains("OMV") && !videoType.contains("UGC")) return@forEach
            val videoId = endpoint.optString("videoId")
            val title = card.optJSONObject("title")?.optJSONArray("runs")?.joinText().orEmpty().trim()
            if (videoId.isBlank() || title.isBlank()) return@forEach
            val thumb = bestThumbnail(thumbnailsOf(card))
            val seed = stableSeed(videoId + title)
            val accent = palette(seed)
            result.putIfAbsent(
                videoId,
                Track(
                    id = videoId,
                    title = title,
                    artist = artistName,
                    album = "YouTube Music Video",
                    durationMs = durationOf(card.toString()),
                    streamUrl = "",
                    videoUrl = "https://www.youtube.com/watch?v=$videoId",
                    thumbnailUrl = thumb,
                    largeThumbnailUrl = upgradeThumbnail(thumb),
                    source = "YouTube Music Video",
                    moodTags = setOf("video"),
                    energy = (45 + seed % 52).coerceIn(0, 100),
                    vocal = (35 + (seed / 3) % 60).coerceIn(0, 100),
                    replayScore = (62 + (seed / 7) % 38).coerceIn(0, 100),
                    cacheScore = (48 + (seed / 11) % 50).coerceIn(0, 100),
                    accentStart = accent.first,
                    accentEnd = accent.second,
                    videoType = videoType
                )
            )
        }
        return result.values.take(50).toList()
    }

    private fun findPlaylistId(value: Any?): String {
        val endpoints = mutableListOf<JSONObject>()
        collectByKey(value, "watchEndpoint", endpoints)
        return endpoints.firstNotNullOfOrNull { endpoint -> endpoint.optString("playlistId").takeIf { it.isNotBlank() } }.orEmpty()
    }

    private fun findPlaylistIdByMarker(value: Any?, markers: List<String>): String {
        val buttons = mutableListOf<JSONObject>()
        collectByKey(value, "musicPlayButtonRenderer", buttons)
        collectByKey(value, "buttonRenderer", buttons)
        buttons.forEach { button ->
            val serialized = button.toString()
            if (markers.none { marker -> serialized.contains(marker, ignoreCase = true) }) return@forEach
            findPlaylistId(button).takeIf { it.isNotBlank() }?.let { return it }
        }
        val endpoints = mutableListOf<JSONObject>()
        collectByKey(value, "watchEndpoint", endpoints)
        return endpoints.firstNotNullOfOrNull { endpoint ->
            val serialized = endpoint.toString()
            endpoint.optString("playlistId").takeIf { playlistId ->
                playlistId.isNotBlank() && markers.any { marker -> serialized.contains(marker, ignoreCase = true) }
            }
        }.orEmpty()
    }

    private fun findContinuation(root: JSONObject): String {
        val keys = listOf("nextContinuationData", "reloadContinuationData", "continuationCommand")
        keys.forEach { key ->
            val nodes = mutableListOf<JSONObject>()
            collectByKey(root, key, nodes)
            nodes.forEach { node ->
                node.optString("continuation").takeIf { it.isNotBlank() }?.let { return it }
                node.optString("token").takeIf { it.isNotBlank() }?.let { return it }
            }
        }
        return ""
    }

    private fun postSearch(query: String): JSONObject {
        val endpoint = "https://music.youtube.com/youtubei/v1/search?key=$apiKey&prettyPrint=false"
        val body = JSONObject()
            .put("context", clientContext())
            .put("query", query)
            .put("params", "EgWKAQIgAWoMEAMQBBAJEAoQBRAV")
            .toString()
        return post(endpoint, body, "https://music.youtube.com/search?q=${query.replace(" ", "+")}")
    }

    private fun postBrowse(browseId: String, params: String = "", continuation: String = ""): JSONObject {
        val endpoint = "https://music.youtube.com/youtubei/v1/browse?key=$apiKey&prettyPrint=false"
        val payload = JSONObject().put("context", clientContext())
        if (browseId.isNotBlank()) payload.put("browseId", browseId)
        if (params.isNotBlank()) payload.put("params", params)
        if (continuation.isNotBlank()) payload.put("continuation", continuation)
        return post(endpoint, payload.toString(), "https://music.youtube.com/")
    }

    private fun contentLanguage(): String = preferences?.languageCode() ?: LevyraLanguageCatalog.deviceDefault()

    private fun clientContext(): JSONObject {
        val locale = LevyraContentLocales.forLanguage(contentLanguage())
        return JSONObject().put(
            "client",
            JSONObject()
                .put("clientName", "WEB_REMIX")
                .put("clientVersion", clientVersion)
                .put("hl", locale.hl)
                .put("gl", locale.gl)
                .put("platform", "DESKTOP")
        )
    }

    private fun post(endpoint: String, body: String, referer: String): JSONObject {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 20000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Origin", "https://music.youtube.com")
            setRequestProperty("Referer", referer)
            setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
            setRequestProperty("X-Youtube-Client-Name", "67")
            setRequestProperty("X-Youtube-Client-Version", clientVersion)
            GoogleApiKeyHeaders.applyTo(this, context)
            setRequestProperty("Content-Length", bytes.size.toString())
        }
        connection.outputStream.use { it.write(bytes) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val response = BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
        if (code !in 200..299) return JSONObject()
        return JSONObject(response)
    }

    private fun headerText(header: JSONObject?): String {
        header ?: return ""

        fun titleOf(renderer: JSONObject?): String {
            return renderer
                ?.optJSONObject("title")
                ?.optJSONArray("runs")
                ?.joinText()
                .orEmpty()
                .trim()
        }

        return sequenceOf(
            titleOf(header.optJSONObject("musicImmersiveHeaderRenderer")),
            titleOf(header.optJSONObject("musicVisualHeaderRenderer")),
            titleOf(header.optJSONObject("musicHeaderRenderer"))
        ).firstOrNull { it.isNotBlank() }.orEmpty()
    }


    private fun thumbnailsOf(node: JSONObject): JSONArray {
        val arrays = mutableListOf<JSONArray>()
        collectArrays(node, "thumbnails", arrays)
        return arrays.firstOrNull { it.length() > 0 } ?: JSONArray()
    }

    private fun primaryVideoId(renderer: JSONObject): String {
        val endpoints = mutableListOf<JSONObject>()
        collectByKey(renderer, "watchEndpoint", endpoints)
        endpoints.forEach { endpoint ->
            val id = endpoint.optString("videoId")
            if (id.isNotBlank()) return id
        }
        return ""
    }

    private fun flexLines(renderer: JSONObject): List<String> {
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

    private fun durationOf(text: String): Long {
        val match = Regex("\\b\\d{1,2}:\\d{2}(?::\\d{2})?\\b").find(text)?.value ?: return 0L
        val parts = match.split(":").mapNotNull { it.toLongOrNull() }
        return when (parts.size) {
            2 -> (parts[0] * 60L + parts[1]) * 1000L
            3 -> (parts[0] * 3600L + parts[1] * 60L + parts[2]) * 1000L
            else -> 0L
        }
    }

    private fun bestThumbnail(array: JSONArray): String {
        var bestUrl = ""
        var bestScore = -1
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val url = item.optString("url")
            val score = item.optInt("width", 0) * item.optInt("height", 0)
            if (url.isNotBlank() && score >= bestScore) {
                bestUrl = url
                bestScore = score
            }
        }
        return bestUrl
    }

    private fun upgradeThumbnail(url: String): String {
        if (url.isBlank()) return url
        return url.replace(Regex("=w\\d+-h\\d+.*$"), "=w1200-h1200-l90-rj")
            .replace(Regex("=s\\d+.*$"), "=s1200")
    }

    private fun collectByKey(value: Any?, key: String, out: MutableList<JSONObject>) {
        when (value) {
            is JSONObject -> {
                val keys = value.keys()
                while (keys.hasNext()) {
                    val current = keys.next()
                    val child = value.opt(current)
                    if (current == key && child is JSONObject) out += child
                    collectByKey(child, key, out)
                }
            }
            is JSONArray -> for (i in 0 until value.length()) collectByKey(value.opt(i), key, out)
        }
    }

    private fun collectArrays(value: Any?, key: String, out: MutableList<JSONArray>) {
        when (value) {
            is JSONObject -> {
                val keys = value.keys()
                while (keys.hasNext()) {
                    val current = keys.next()
                    val child = value.opt(current)
                    if (current == key && child is JSONArray) out += child
                    collectArrays(child, key, out)
                }
            }
            is JSONArray -> for (i in 0 until value.length()) collectArrays(value.opt(i), key, out)
        }
    }

    private fun JSONArray.joinText(): String {
        val parts = mutableListOf<String>()
        for (i in 0 until length()) {
            val text = optJSONObject(i)?.optString("text").orEmpty()
            if (text.isNotBlank()) parts += text
        }
        return parts.joinToString("").replace("  ", " ").trim()
    }

    private fun stableSeed(value: String): Int {
        return MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
            .take(4)
            .fold(0) { acc, byte -> (acc shl 8) or (byte.toInt() and 0xFF) }
            .absoluteValue
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
