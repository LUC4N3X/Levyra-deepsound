package com.luc4n3x.levyra.desktop.core.radio

import com.luc4n3x.levyra.desktop.core.extractor.ExtractorHttp
import com.luc4n3x.levyra.desktop.core.model.Track
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import kotlinx.serialization.Serializable

data class RadioStation(
    val uuid: String,
    val name: String,
    val streamUrl: String,
    val favicon: String,
    val country: String,
    val tags: List<String>,
    val codec: String,
    val bitrate: Int
) {
    fun toTrack(): Track = Track(
        id = "radio:$uuid",
        title = name,
        artist = listOfNotNull(country.takeIf(String::isNotBlank), tags.firstOrNull()).joinToString(" · "),
        videoUrl = streamUrl,
        artworkUrl = favicon,
        live = true
    )

    val details: String
        get() = listOfNotNull(
            country.takeIf(String::isNotBlank),
            tags.firstOrNull(),
            listOfNotNull(codec.takeIf(String::isNotBlank), bitrate.takeIf { it > 0 }?.let { "$it kbps" }).joinToString(" / ").takeIf(String::isNotBlank)
        ).joinToString(" · ")
}

@Serializable
data class SavedStation(
    val uuid: String,
    val name: String,
    val streamUrl: String,
    val favicon: String = "",
    val details: String = ""
) {
    fun toTrack(): Track = Track(id = "radio:$uuid", title = name, artist = details, videoUrl = streamUrl, artworkUrl = favicon, live = true)
}

fun RadioStation.toSaved(): SavedStation = SavedStation(uuid, name, streamUrl, favicon, details)

class RadioBrowser(
    private val client: OkHttpClient = ExtractorHttp.client,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun stations(countryCode: String?, tag: String?, limit: Int = DEFAULT_LIMIT): List<RadioStation> {
        val params = linkedMapOf<String, String>()
        countryCode?.takeIf { it.length == 2 }?.let { params["countrycode"] = it.uppercase(Locale.ROOT) }
        tag?.takeIf(String::isNotBlank)?.let { params["tag"] = it }
        params["order"] = if (countryCode == null) "clickcount" else "votes"
        params["reverse"] = "true"
        params["hidebroken"] = "true"
        params["limit"] = limit.coerceIn(1, MAX_LIMIT).toString()
        return request(params)
    }

    suspend fun search(query: String, limit: Int = DEFAULT_LIMIT): List<RadioStation> {
        val trimmed = query.trim().take(MAX_QUERY)
        if (trimmed.length < 2) return emptyList()
        return request(linkedMapOf("name" to trimmed, "order" to "clickcount", "reverse" to "true", "hidebroken" to "true", "limit" to limit.coerceIn(1, MAX_LIMIT).toString()))
    }

    private suspend fun request(params: Map<String, String>): List<RadioStation> = withContext(dispatcher) {
        var failure: Exception? = null
        for (server in SERVERS) {
            try {
                val url = HttpUrl.Builder().scheme("https").host(server).addPathSegments("json/stations/search").apply {
                    params.forEach { (key, value) -> addQueryParameter(key, value) }
                }.build()
                val body = client.newCall(Request.Builder().url(url).header("User-Agent", USER_AGENT).build()).execute().use { response ->
                    if (!response.isSuccessful) error("HTTP ${response.code}")
                    val source = response.body ?: error("empty")
                    if (source.contentLength() > MAX_BYTES) error("too large")
                    source.string().also { if (it.length > MAX_BYTES) error("too large") }
                }
                return@withContext parse(body)
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Exception) {
                failure = error
            }
        }
        throw failure ?: IllegalStateException("radio unavailable")
    }

    companion object {
        private val SERVERS = listOf("de1.api.radio-browser.info", "nl1.api.radio-browser.info", "at1.api.radio-browser.info")
        private const val USER_AGENT = "Levyra Desktop"
        private const val DEFAULT_LIMIT = 40
        private const val MAX_LIMIT = 100
        private const val MAX_QUERY = 80
        private const val MAX_BYTES = 2L * 1024L * 1024L

        fun parse(body: String): List<RadioStation> {
            val array = runCatching { JSONArray(body) }.getOrNull() ?: return emptyList()
            return (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val uuid = item.optString("stationuuid").trim()
                val name = item.optString("name").trim()
                val stream = item.optString("url_resolved").trim().ifBlank { item.optString("url").trim() }
                if (uuid.isEmpty() || name.isEmpty() || !stream.startsWith("http", ignoreCase = true)) return@mapNotNull null
                val favicon = item.optString("favicon").trim().takeIf { it.startsWith("https://", ignoreCase = true) }.orEmpty()
                RadioStation(
                    uuid = uuid,
                    name = name,
                    streamUrl = stream,
                    favicon = favicon,
                    country = item.optString("country").trim(),
                    tags = item.optString("tags").split(',').map(String::trim).filter(String::isNotEmpty).take(3),
                    codec = item.optString("codec").trim(),
                    bitrate = item.optInt("bitrate", 0)
                )
            }.distinctBy { it.uuid }
        }
    }
}
