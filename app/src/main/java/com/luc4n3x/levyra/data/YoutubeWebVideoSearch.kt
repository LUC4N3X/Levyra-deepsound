package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.data.collections.await
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.data.network.YoutubeRegionProfile
import com.luc4n3x.levyra.domain.parseCompactViewCount
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

internal data class VideoUploadEvidence(
    val videoId: String,
    val title: String,
    val channelName: String,
    val channelVerified: Boolean,
    val officialArtistChannel: Boolean,
    val durationMs: Long,
    val viewCount: Long,
    val thumbnailUrl: String = ""
)

internal class YoutubeWebVideoSearch(
    private val client: () -> OkHttpClient = { LevyraHttpClientFactory.youtubePlayer() }
) {
    suspend fun search(query: String, languageCode: String, limit: Int): List<VideoUploadEvidence> {
        val clean = query.trim()
        if (clean.length < 2) return emptyList()
        val locale = YoutubeRegionProfile.effectiveLocale(languageCode)
        val payload = JSONObject()
            .put(
                "context",
                JSONObject().put(
                    "client",
                    JSONObject()
                        .put("clientName", YoutubeWebClientIdentity.CLIENT_NAME)
                        .put("clientVersion", YoutubeWebClientIdentity.CLIENT_VERSION)
                        .put("hl", locale.hl)
                        .put("gl", locale.gl)
                        .put("platform", "DESKTOP")
                )
            )
            .put("query", clean)
            .put("params", VIDEOS_ONLY_PARAMS)
        val request = Request.Builder()
            .url(SEARCH_URL)
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("User-Agent", YoutubeWebClientIdentity.USER_AGENT)
            .header("X-Youtube-Client-Name", WEB_CLIENT_HEADER_NAME)
            .header("X-Youtube-Client-Version", YoutubeWebClientIdentity.CLIENT_VERSION)
            .build()
        val body = client().newCall(request).await().use { response ->
            if (!response.isSuccessful) throw IOException("YouTube video search HTTP ${response.code}")
            val source = response.body.source()
            if (source.request(MAX_RESPONSE_BYTES + 1)) throw IOException("YouTube video search response too large")
            source.buffer.readUtf8()
        }
        return parseYoutubeWebVideoResults(JSONObject(body), limit)
    }

    private companion object {
        const val SEARCH_URL = "https://www.youtube.com/youtubei/v1/search?prettyPrint=false"
        const val VIDEOS_ONLY_PARAMS = "EgIQAQ=="
        const val WEB_CLIENT_HEADER_NAME = "1"
        const val MAX_RESPONSE_BYTES = 4L * 1024L * 1024L
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

internal fun parseYoutubeWebVideoResults(root: JSONObject, limit: Int): List<VideoUploadEvidence> {
    val renderers = ArrayList<JSONObject>()
    collectVideoRenderers(root, renderers, limit.coerceAtLeast(0) * 2)
    return renderers.asSequence()
        .mapNotNull(::videoUploadEvidence)
        .distinctBy { it.videoId }
        .take(limit)
        .toList()
}

private fun collectVideoRenderers(value: Any?, out: MutableList<JSONObject>, cap: Int) {
    if (out.size >= cap) return
    when (value) {
        is JSONObject -> {
            value.optJSONObject("videoRenderer")?.let(out::add)
            value.keys().forEach { key -> if (key != "videoRenderer") collectVideoRenderers(value.opt(key), out, cap) }
        }
        is JSONArray -> for (index in 0 until value.length()) collectVideoRenderers(value.opt(index), out, cap)
    }
}

private fun videoUploadEvidence(renderer: JSONObject): VideoUploadEvidence? {
    val videoId = renderer.optString("videoId").takeIf(YOUTUBE_WEB_VIDEO_ID::matches) ?: return null
    val title = runsText(renderer.optJSONObject("title"))
    if (title.isBlank()) return null
    val badges = renderer.optJSONArray("ownerBadges")
        ?.let { array -> (0 until array.length()).mapNotNull { array.optJSONObject(it)?.optJSONObject("metadataBadgeRenderer")?.optString("style") } }
        .orEmpty()
    return VideoUploadEvidence(
        videoId = videoId,
        title = title,
        channelName = runsText(renderer.optJSONObject("ownerText")),
        channelVerified = badges.any { it == BADGE_VERIFIED || it == BADGE_VERIFIED_ARTIST },
        officialArtistChannel = BADGE_VERIFIED_ARTIST in badges,
        durationMs = clockTextMs(renderer.optJSONObject("lengthText")?.optString("simpleText").orEmpty()),
        viewCount = parseCompactViewCount(simpleOrRunsText(renderer.optJSONObject("viewCountText"))),
        thumbnailUrl = renderer.optJSONObject("thumbnail")
            ?.optJSONArray("thumbnails")
            ?.let { thumbnails -> thumbnails.optJSONObject(thumbnails.length() - 1)?.optString("url") }
            .orEmpty()
    )
}

private fun runsText(value: JSONObject?): String {
    val runs = value?.optJSONArray("runs") ?: return value?.optString("simpleText").orEmpty()
    return buildString { for (index in 0 until runs.length()) append(runs.optJSONObject(index)?.optString("text").orEmpty()) }.trim()
}

private fun simpleOrRunsText(value: JSONObject?): String =
    value?.optString("simpleText")?.takeIf(String::isNotBlank) ?: runsText(value)

internal fun clockTextMs(text: String): Long {
    val parts = text.trim().split(':')
    if (parts.size !in 2..3) return 0L
    val numbers = parts.map { it.toLongOrNull() ?: return 0L }
    return numbers.fold(0L) { total, part -> total * 60L + part } * 1_000L
}

private val YOUTUBE_WEB_VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")
private const val BADGE_VERIFIED = "BADGE_STYLE_TYPE_VERIFIED"
private const val BADGE_VERIFIED_ARTIST = "BADGE_STYLE_TYPE_VERIFIED_ARTIST"
