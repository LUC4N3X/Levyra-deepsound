package com.luc4n3x.levyra.data.hqaudio.jiosaavn

import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackCandidate
import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackText
import org.json.JSONArray
import org.json.JSONObject

internal sealed interface JioSaavnSongDetails {
    data class Found(val candidate: AlternativeTrackCandidate) : JioSaavnSongDetails
    data object Missing : JioSaavnSongDetails
    data object Malformed : JioSaavnSongDetails
}

internal sealed interface JioSaavnMediaAuthorization {
    data class Granted(val url: String) : JioSaavnMediaAuthorization
    data object Denied : JioSaavnMediaAuthorization
    data object Malformed : JioSaavnMediaAuthorization
}

internal object JioSaavnPayloadParser {
    fun searchCandidates(body: String): List<AlternativeTrackCandidate>? {
        val root = parseObject(body) ?: return null
        val results = root.optJSONArray("results") ?: return null
        return objects(results).mapNotNull(::candidateOf)
    }

    fun songDetails(body: String, providerTrackId: String): JioSaavnSongDetails {
        val trimmed = body.trim()
        if (trimmed.startsWith("[")) {
            val array = runCatching { JSONArray(trimmed) }.getOrNull() ?: return JioSaavnSongDetails.Malformed
            return if (array.length() == 0) JioSaavnSongDetails.Missing else JioSaavnSongDetails.Malformed
        }
        val root = parseObject(trimmed) ?: return JioSaavnSongDetails.Malformed
        val entry = root.optJSONArray("songs")
            ?.let { songs -> objects(songs).firstOrNull { it.optString("id") == providerTrackId } }
            ?: root.optJSONObject(providerTrackId)
        if (entry == null) {
            val recognizedEnvelope = root.has("songs") || root.has("modules") || root.length() == 0
            return if (recognizedEnvelope) JioSaavnSongDetails.Missing else JioSaavnSongDetails.Malformed
        }
        return candidateOf(entry)?.let { JioSaavnSongDetails.Found(it) } ?: JioSaavnSongDetails.Missing
    }

    fun mediaAuthorization(body: String): JioSaavnMediaAuthorization {
        val root = parseObject(body) ?: return JioSaavnMediaAuthorization.Malformed
        if (!root.optString("status").equals("success", ignoreCase = true)) return JioSaavnMediaAuthorization.Denied
        val url = root.opt("auth_url") as? String ?: return JioSaavnMediaAuthorization.Denied
        return if (url.startsWith("https://")) JioSaavnMediaAuthorization.Granted(url) else JioSaavnMediaAuthorization.Denied
    }

    private fun candidateOf(json: JSONObject): AlternativeTrackCandidate? {
        val type = json.optString("type")
        if (type.isNotEmpty() && type != "song") return null
        val info = json.optJSONObject("more_info") ?: JSONObject()
        val artistMap = info.optJSONObject("artistMap")
        val providerTrackId = json.optString("id").trim()
        val title = decode(json.optString("title").ifBlank { json.optString("song") })
        val primaryArtists = names(artistMap?.optJSONArray("primary_artists"))
            .ifEmpty { splitNames(info.optString("primary_artists").ifBlank { json.optString("primary_artists") }) }
        val featuredArtists = names(artistMap?.optJSONArray("featured_artists"))
        val album = decode(info.optString("album").ifBlank { json.optString("album") })
        val durationSeconds = (info.opt("duration") ?: json.opt("duration"))?.toString()?.trim()?.toIntOrNull() ?: 0
        val explicit = when (json.optString("explicit_content").trim().lowercase()) {
            "1", "true" -> true
            "0", "false" -> false
            else -> null
        }
        val offers320 = (info.opt("320kbps") ?: json.opt("320kbps"))?.toString().equals("true", ignoreCase = true)
        val mediaToken = info.optString("encrypted_media_url").ifBlank { json.optString("encrypted_media_url") }.trim()
        if (providerTrackId.isBlank() || title.isBlank() || primaryArtists.isEmpty() || mediaToken.isBlank()) return null
        return AlternativeTrackCandidate(
            providerId = JIOSAAVN_PROVIDER_ID,
            providerTrackId = providerTrackId,
            title = title,
            primaryArtists = primaryArtists,
            featuredArtists = featuredArtists,
            album = album,
            durationSeconds = durationSeconds,
            explicit = explicit,
            offers320 = offers320,
            mediaToken = mediaToken
        )
    }

    private fun names(array: JSONArray?): List<String> =
        array?.let(::objects)
            ?.map { decode(it.optString("name")).trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty()

    private fun splitNames(value: String): List<String> =
        decode(value).split(',').map { it.trim() }.filter { it.isNotBlank() }

    private fun objects(array: JSONArray): List<JSONObject> =
        (0 until array.length()).mapNotNull(array::optJSONObject)

    private fun decode(value: String): String = AlternativeTrackText.decodeHtmlEntities(value).trim()

    private fun parseObject(body: String): JSONObject? = runCatching { JSONObject(body.trim()) }.getOrNull()
}
