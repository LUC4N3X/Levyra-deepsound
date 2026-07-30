package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Resolves album copy without making the album screen depend on one provider.
 *
 * YouTube Music remains the first source. When it has no description we try a
 * verified Wikipedia article, then Wikidata's CC0 entity description. Spotify
 * is used only when an actual open.spotify.com album URL is already present;
 * generic "Listen on Spotify" marketing copy is deliberately rejected.
 */

internal fun wikipediaAlbumTitleMatches(albumTitle: String, pageTitle: String): Boolean {
    val albumKey = wikipediaAlbumTitleKey(albumTitle)
    val pageKey = wikipediaAlbumTitleKey(pageTitle.substringBeforeLast(" ("))
    return albumKey.isNotBlank() && pageKey == albumKey
}

private fun wikipediaAlbumTitleKey(value: String): String {
    var key = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("""\p{M}+"""), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("""[^\p{L}\p{N}]+"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
    val editionSuffixes = listOf(
        " deluxe edition", " deluxe", " expanded edition", " expanded",
        " anniversary edition", " remastered edition", " remastered",
        " special edition", " collector edition", " bonus tracks"
    )
    editionSuffixes.firstOrNull(key::endsWith)?.let { suffix ->
        key = key.removeSuffix(suffix).trim()
    }
    return key
}

internal fun remainingAlbumDescriptionBudgetMillis(
    deadlineNanos: Long,
    nowNanos: Long = System.nanoTime()
): Long {
    val remainingNanos = deadlineNanos - nowNanos
    if (remainingNanos <= 0L) return 0L
    return TimeUnit.NANOSECONDS.toMillis(remainingNanos).coerceAtLeast(1L)
}

internal class AlbumDescriptionRepository(context: Context?) {
    private val client = LevyraHttpClientFactory.media(context?.applicationContext).newBuilder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .writeTimeout(4, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
    private val cache = ConcurrentHashMap<String, String>()

    suspend fun resolve(
        album: AlbumHit,
        languageCode: String,
        youtubeDescription: String,
        trackCount: Int
    ): String {
        val native = cleanDescription(youtubeDescription)
        if (native.length >= MIN_DESCRIPTION_LENGTH) return native

        val language = LevyraLanguageCatalog.normalize(languageCode)
        val key = "${descriptionKey(album.title)}|${descriptionKey(album.artist)}|$language"
        cache[key]?.let { return it }

        val deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(REMOTE_TIMEOUT_MS)
        val remote = withContext(Dispatchers.IO) {
            wikipediaDescription(album, language, deadlineNanos)
                ?: wikidataDescription(album, language, deadlineNanos)
                ?: spotifyDescription(album.canonicalUrl, deadlineNanos)
        }
        val resolved = cleanDescription(remote.orEmpty())
            .takeIf { it.length >= MIN_DESCRIPTION_LENGTH }
            ?: factualFallback(album, language, trackCount)
        cache[key] = resolved
        return resolved
    }

    private fun wikipediaDescription(album: AlbumHit, languageCode: String, deadlineNanos: Long): String? {
        val languages = linkedSetOf(languageCode, "en")
        for (language in languages) {
            if (remainingAlbumDescriptionBudgetMillis(deadlineNanos) <= 0L) break
            val url = "https://$language.wikipedia.org/w/api.php".toHttpUrl().newBuilder()
                .addQueryParameter("action", "query")
                .addQueryParameter("generator", "search")
                .addQueryParameter("gsrsearch", "\"${album.title}\" \"${album.artist}\" album")
                .addQueryParameter("gsrnamespace", "0")
                .addQueryParameter("gsrlimit", "6")
                .addQueryParameter("prop", "extracts|pageprops")
                .addQueryParameter("exintro", "1")
                .addQueryParameter("explaintext", "1")
                .addQueryParameter("exsentences", "3")
                .addQueryParameter("redirects", "1")
                .addQueryParameter("format", "json")
                .addQueryParameter("formatversion", "2")
                .addQueryParameter("origin", "*")
                .build()
            val pages = requestJson(url, deadlineNanos)
                ?.optJSONObject("query")
                ?.optJSONArray("pages")
                ?: continue
            var bestText: String? = null
            var bestScore = Int.MIN_VALUE
            for (index in 0 until pages.length()) {
                val page = pages.optJSONObject(index) ?: continue
                if (page.optJSONObject("pageprops")?.has("disambiguation") == true) continue
                val title = page.optString("title").trim()
                val extract = cleanDescription(page.optString("extract"))
                if (extract.length < MIN_DESCRIPTION_LENGTH) continue
                val score = wikipediaScore(album, title, extract)
                if (score > bestScore) {
                    bestScore = score
                    bestText = extract
                }
            }
            if (bestScore >= MIN_WIKIPEDIA_SCORE && !bestText.isNullOrBlank()) return bestText
        }
        return null
    }

    private fun wikidataDescription(album: AlbumHit, languageCode: String, deadlineNanos: Long): String? {
        val url = "https://www.wikidata.org/w/api.php".toHttpUrl().newBuilder()
            .addQueryParameter("action", "wbsearchentities")
            .addQueryParameter("search", "${album.title} ${album.artist}")
            .addQueryParameter("language", languageCode)
            .addQueryParameter("uselang", languageCode)
            .addQueryParameter("type", "item")
            .addQueryParameter("limit", "8")
            .addQueryParameter("format", "json")
            .addQueryParameter("origin", "*")
            .build()
        val results = requestJson(url, deadlineNanos)?.optJSONArray("search") ?: return null
        val albumKey = descriptionKey(album.title)
        val artistKey = descriptionKey(album.artist)
        var best: String? = null
        var bestScore = Int.MIN_VALUE
        for (index in 0 until results.length()) {
            val item = results.optJSONObject(index) ?: continue
            val label = item.optString("label").trim()
            val description = cleanDescription(item.optString("description"))
            if (description.length < 12) continue
            val labelKey = descriptionKey(label)
            val descriptionKey = descriptionKey(description)
            var score = 0
            if (labelKey == albumKey) score += 120
            else if (labelKey.contains(albumKey) || albumKey.contains(labelKey)) score += 70
            if (artistKey.isNotBlank() && descriptionKey.contains(artistKey)) score += 70
            if (descriptionKey.contains("album")) score += 30
            if (score > bestScore) {
                bestScore = score
                best = description
            }
        }
        return best?.takeIf { bestScore >= MIN_WIKIDATA_SCORE }
    }

    private fun spotifyDescription(canonicalUrl: String, deadlineNanos: Long): String? {
        val url = canonicalUrl.trim().toHttpUrlOrNull() ?: return null
        if (!url.isHttps || !url.host.equals("open.spotify.com", ignoreCase = true)) return null
        if (url.pathSegments.firstOrNull() != "album") return null
        val html = requestText(url, "text/html", deadlineNanos) ?: return null
        val description = sequenceOf(
            SPOTIFY_OG_DESCRIPTION.find(html)?.groupValues?.getOrNull(1),
            SPOTIFY_OG_DESCRIPTION_REVERSED.find(html)?.groupValues?.getOrNull(1),
            SPOTIFY_TWITTER_DESCRIPTION.find(html)?.groupValues?.getOrNull(1)
        ).filterNotNull().map(::decodeHtml).map(::cleanDescription).firstOrNull { it.length >= MIN_DESCRIPTION_LENGTH }
            ?: return null
        val normalized = description.lowercase(Locale.ROOT)
        if (normalized.startsWith("listen to ") && normalized.contains(" on spotify")) return null
        return description
    }

    private fun requestJson(url: HttpUrl, deadlineNanos: Long): JSONObject? {
        val body = requestText(url, "application/json", deadlineNanos) ?: return null
        return runCatching { JSONObject(body) }.getOrNull()
    }

    private fun requestText(url: HttpUrl, accept: String, deadlineNanos: Long): String? {
        val remainingMs = remainingAlbumDescriptionBudgetMillis(deadlineNanos)
        if (remainingMs <= 0L) return null
        val request = Request.Builder()
            .url(url)
            .header("Accept", accept)
            .header("Accept-Language", "en-US,en;q=0.8")
            .header("User-Agent", "Levyra/Android album-metadata")
            .build()
        val call = client.newCall(request)
        call.timeout().timeout(remainingMs, TimeUnit.MILLISECONDS)
        return runCatching {
            call.execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body.string().take(MAX_RESPONSE_CHARS)
            }
        }.getOrNull()
    }

    private fun wikipediaScore(album: AlbumHit, pageTitle: String, extract: String): Int {
        if (!wikipediaAlbumTitleMatches(album.title, pageTitle)) return Int.MIN_VALUE
        val artistKey = descriptionKey(album.artist)
        val extractKey = descriptionKey(extract)
        var score = 130
        if (artistKey.isNotBlank() && extractKey.contains(artistKey)) score += 75
        if (extractKey.contains("album")) score += 25
        if (pageTitle.contains("song", ignoreCase = true) || pageTitle.contains("singolo", ignoreCase = true)) score -= 90
        return score
    }

    private fun factualFallback(album: AlbumHit, languageCode: String, trackCount: Int): String {
        val year = album.year.ifBlank { album.releaseDate.take(4).takeIf { it.length == 4 && it.all(Char::isDigit) }.orEmpty() }
        return when (languageCode) {
            "it" -> buildString {
                append(album.title).append(" è un album di ").append(album.artist)
                if (year.isNotBlank()) append(", pubblicato nel ").append(year)
                if (trackCount > 0) append(". Contiene ").append(trackCount).append(if (trackCount == 1) " brano" else " brani")
                append('.')
            }
            "es" -> buildString {
                append(album.title).append(" es un álbum de ").append(album.artist)
                if (year.isNotBlank()) append(", publicado en ").append(year)
                if (trackCount > 0) append(". Incluye ").append(trackCount).append(if (trackCount == 1) " canción" else " canciones")
                append('.')
            }
            "fr" -> buildString {
                append(album.title).append(" est un album de ").append(album.artist)
                if (year.isNotBlank()) append(", publié en ").append(year)
                if (trackCount > 0) append(". Il contient ").append(trackCount).append(if (trackCount == 1) " titre" else " titres")
                append('.')
            }
            "de" -> buildString {
                append(album.title).append(" ist ein Album von ").append(album.artist)
                if (year.isNotBlank()) append(" aus dem Jahr ").append(year)
                if (trackCount > 0) append(". Es enthält ").append(trackCount).append(if (trackCount == 1) " Titel" else " Titel")
                append('.')
            }
            "pt" -> buildString {
                append(album.title).append(" é um álbum de ").append(album.artist)
                if (year.isNotBlank()) append(", lançado em ").append(year)
                if (trackCount > 0) append(". Contém ").append(trackCount).append(if (trackCount == 1) " faixa" else " faixas")
                append('.')
            }
            else -> buildString {
                append(album.title).append(" is an album by ").append(album.artist)
                if (year.isNotBlank()) append(", released in ").append(year)
                if (trackCount > 0) append(". It contains ").append(trackCount).append(if (trackCount == 1) " track" else " tracks")
                append('.')
            }
        }
    }

    private fun cleanDescription(value: String): String = decodeHtml(value)
        .replace(Regex("<[^>]+>"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(MAX_DESCRIPTION_CHARS)

    private fun descriptionKey(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun decodeHtml(value: String): String = value
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&nbsp;", " ")

    private companion object {
        const val REMOTE_TIMEOUT_MS = 7_000L
        const val MIN_DESCRIPTION_LENGTH = 24
        const val MIN_WIKIPEDIA_SCORE = 130
        const val MIN_WIKIDATA_SCORE = 120
        const val MAX_RESPONSE_CHARS = 240_000
        const val MAX_DESCRIPTION_CHARS = 900
        val SPOTIFY_OG_DESCRIPTION = Regex(
            "<meta[^>]+(?:property|name)=[\\\"']og:description[\\\"'][^>]+content=[\\\"']([^\\\"']+)[\\\"']",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val SPOTIFY_OG_DESCRIPTION_REVERSED = Regex(
            "<meta[^>]+content=[\\\"']([^\\\"']+)[\\\"'][^>]+(?:property|name)=[\\\"']og:description[\\\"']",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val SPOTIFY_TWITTER_DESCRIPTION = Regex(
            "<meta[^>]+(?:property|name)=[\\\"']twitter:description[\\\"'][^>]+content=[\\\"']([^\\\"']+)[\\\"']",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
    }
}
