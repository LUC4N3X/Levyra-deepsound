package com.luc4n3x.levyra.desktop.core.stream

import com.luc4n3x.levyra.desktop.core.extractor.ExtractorHttp
import com.luc4n3x.levyra.desktop.core.extractor.ExtractorRuntime
import com.luc4n3x.levyra.desktop.core.model.*
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager

internal class VisionOsStreamSource {
    @Volatile private var visitor: Pair<String, Long>? = null
    private val client = ExtractorHttp.client.newBuilder()
        .dns(YoutubeStreamNetworkPolicy.validatingDns())
        .followRedirects(false).followSslRedirects(false)
        .callTimeout(5, TimeUnit.SECONDS).build()

    fun resolve(track: Track, quality: AudioQuality, codec: PreferredCodec, verify: (String) -> Boolean): ResolvedAudio? {
        val country = ExtractorRuntime.contentCountry().countryCode
        val language = ExtractorRuntime.localization().localizationCode
        val identity = JSONObject().put("clientName", "VISIONOS").put("clientVersion", "1.04")
            .put("deviceMake", "Apple").put("deviceModel", "RealityDevice17,1")
            .put("osName", "visionOS").put("osVersion", "26.6.0.23O770")
            .put("platform", "MOBILE").put("clientScreen", "WATCH")
            .put("hl", language).put("gl", country).put("utcOffsetMinutes", 0).put("timeZone", "UTC")
        val visitorData = visitor?.takeIf { System.currentTimeMillis() - it.second < 3_600_000L }?.first
            ?: loadVisitor(identity, country).also { if (it.isNotBlank()) visitor = it to System.currentTimeMillis() }
        if (visitorData.isNotBlank()) identity.put("visitorData", visitorData)
        val body = JSONObject().put("context", JSONObject().put("client", identity))
            .put("videoId", track.videoId).put("contentCheckOk", true).put("racyCheckOk", true)
            .put("playbackContext", JSONObject().put("contentPlaybackContext", JSONObject().put("html5Preference", "HTML5_PREF_WANTS")))
        val request = Request.Builder().url("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
            .header("User-Agent", visionOsUserAgent(country))
            .header("X-Youtube-Client-Name", "101").header("X-Youtube-Client-Version", "1.04")
            .header("Origin", "https://www.youtube.com").header("Referer", track.videoUrl)
            .apply { if (visitorData.isNotBlank()) header("X-Goog-Visitor-Id", visitorData) }
            .post(body.toString().toRequestBody("application/json".toMediaType())).build()
        val response = client.newCall(request).execute().use {
            if (!it.isSuccessful) {
                System.getLogger("Levyra.YouTube").log(System.Logger.Level.WARNING, "VISIONOS player HTTP ${it.code}")
                return null
            }
            JSONObject(it.peekBody(2_097_152).string())
        }
        if (response.optJSONObject("playabilityStatus")?.optString("status") != "OK") {
            System.getLogger("Levyra.YouTube").log(System.Logger.Level.WARNING,
                "VISIONOS playability=${response.optJSONObject("playabilityStatus")?.optString("status")}")
            return null
        }
        val data = response.optJSONObject("streamingData") ?: return null
        val formats = data.optJSONArray("adaptiveFormats") ?: return null
        val candidates = (0 until formats.length()).mapNotNull { index ->
            val format = formats.optJSONObject(index) ?: return@mapNotNull null
            val mime = format.optString("mimeType")
            if (!mime.startsWith("audio/")) return@mapNotNull null
            val raw = format.optString("url").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val url = if (raw.contains("&n=") || raw.contains("?n=")) {
                YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(track.videoId, raw)
            } else raw
            AudioCandidate(url, mime, if (mime.contains("mp4")) "m4a" else "webm", format.optInt("itag", -1),
                format.optInt("averageBitrate", format.optInt("bitrate", 0)) / 1_000)
        }.sortedByDescending { AudioStreamSelector.score(it, quality, codec) }
        val chosen = candidates.firstOrNull { verify(it.url) } ?: return null
        val details = response.optJSONObject("videoDetails")
        return ResolvedAudio(chosen.url, chosen.label,
            System.currentTimeMillis() + data.optLong("expiresInSeconds", 3_600) * 1_000L,
            details?.optString("lengthSeconds")?.toLongOrNull()?.times(1_000L) ?: track.durationMs,
            track.artworkUrl, track.title, track.artist)
    }

    private fun loadVisitor(identity: JSONObject, country: String): String {
        val body = JSONObject().put("context", JSONObject().put("client", identity))
        val request = Request.Builder().url("https://www.youtube.com/youtubei/v1/visitor_id?prettyPrint=false")
            .header("User-Agent", visionOsUserAgent(country))
            .header("X-Youtube-Client-Name", "101").header("X-Youtube-Client-Version", "1.04")
            .post(body.toString().toRequestBody("application/json".toMediaType())).build()
        return client.newCall(request).execute().use {
            if (!it.isSuccessful) return ""
            JSONObject(it.peekBody(65_536).string()).optJSONObject("responseContext")?.optString("visitorData").orEmpty()
        }
    }
}

internal fun visionOsUserAgent(country: String): String =
    "com.google.visionos.youtube/1.04(RealityDevice17,1; U; CPU visionOS 26_6_0 like Mac OS X; $country)"
