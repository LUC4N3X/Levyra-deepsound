package com.luc4n3x.levyra.desktop.core.extractor

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

internal class YoutubeMusicBrowse(private val client: OkHttpClient = ExtractorHttp.client) {
    fun browse(languageTag: String, country: String, browseId: String, params: String, continuation: String = "", visitorData: String = ""): JSONObject? {
        val client = JSONObject()
            .put("clientName", "WEB_REMIX")
            .put("clientVersion", CLIENT_VERSION)
            .put("hl", innerTubeLanguage(languageTag))
            .put("gl", country.trim().uppercase().take(2).ifBlank { "US" })
            .put("platform", "DESKTOP")
        if (visitorData.isNotBlank()) client.put("visitorData", visitorData)
        val payload = JSONObject().put("context", JSONObject().put("client", client))
        if (continuation.isNotBlank()) payload.put("continuation", continuation) else payload.put("browseId", browseId)
        if (continuation.isBlank() && params.isNotBlank()) payload.put("params", params)
        val request = Request.Builder()
            .url(BROWSE_URL)
            .post(payload.toString().toRequestBody(JSON))
            .header("Content-Type", "application/json")
            .header("Origin", ORIGIN)
            .header("Referer", "$ORIGIN/")
            .header("User-Agent", ExtractorHttp.DESKTOP_USER_AGENT)
            .header("X-Youtube-Client-Name", "67")
            .header("X-Youtube-Client-Version", CLIENT_VERSION)
            .apply { if (visitorData.isNotBlank()) header("X-Goog-Visitor-Id", visitorData) }
            .build()
        return runCatching {
            this.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val bytes = response.peekBody(MAX_BYTES + 1).bytes()
                if (bytes.size > MAX_BYTES) null else JSONObject(bytes.toString(Charsets.UTF_8))
            }
        }.getOrNull()
    }

    companion object {
        private const val ORIGIN = "https://music.youtube.com"
        private const val BROWSE_URL = "$ORIGIN/youtubei/v1/browse?prettyPrint=false"
        private const val CLIENT_VERSION = "1.20260804.16.00"
        private const val MAX_BYTES = 6L * 1024 * 1024
        private val JSON = "application/json".toMediaType()

        fun innerTubeLanguage(tag: String): String = when (tag) {
            "pt" -> "pt-PT"
            "zh" -> "zh-CN"
            "zh-Hant" -> "zh-TW"
            else -> tag.ifBlank { "en" }
        }
    }
}
