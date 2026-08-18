package com.luc4n3x.levyra.feature.dearrow

import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

internal val DEARROW_VIDEO_ID_PATTERN = Regex("^[A-Za-z0-9_-]{11}$")

internal fun interface DeArrowHttpFetcher {
    fun fetch(url: String): DeArrowHttpResponse
}

internal class DeArrowHttpResponse(
    val code: Int,
    val declaredLength: Long,
    val body: InputStream?,
    private val closeAction: () -> Unit = {}
) : Closeable {
    override fun close() {
        runCatching { body?.close() }
        closeAction()
    }
}

class DeArrowApi internal constructor(
    private val fetcher: DeArrowHttpFetcher
) {
    constructor(client: OkHttpClient = defaultClient()) : this(OkHttpDeArrowFetcher(client))

    suspend fun branding(videoId: String): DeArrowBranding? = when (val outcome = brandingOutcome(videoId)) {
        is DeArrowBrandingOutcome.Resolved -> outcome.branding
        DeArrowBrandingOutcome.Inconclusive -> null
    }

    internal suspend fun brandingOutcome(videoId: String): DeArrowBrandingOutcome = withContext(Dispatchers.IO) {
        if (!DEARROW_VIDEO_ID_PATTERN.matches(videoId)) {
            return@withContext DeArrowBrandingOutcome.Resolved(null)
        }
        val url = "$BRANDING_ENDPOINT?videoID=$videoId"

        for (attempt in 1..MAX_ATTEMPTS) {
            try {
                return@withContext fetchOnce(url)
            } catch (error: CancellationException) {
                throw error
            } catch (error: IOException) {
                if (attempt >= MAX_ATTEMPTS) {
                    Timber.d(error, "DeArrow branding request failed")
                    return@withContext DeArrowBrandingOutcome.Inconclusive
                }
            } catch (error: Exception) {
                Timber.d(error, "DeArrow branding request failed")
                return@withContext DeArrowBrandingOutcome.Inconclusive
            }
        }
        DeArrowBrandingOutcome.Inconclusive
    }

    private fun fetchOnce(url: String): DeArrowBrandingOutcome = fetcher.fetch(url).use { response ->
        if (response.code == 404) return DeArrowBrandingOutcome.Resolved(null)
        if (response.code == 429 || response.code in 500..599) {
            throw IOException("Transient DeArrow HTTP ${response.code}")
        }
        if (response.code !in 200..299) return DeArrowBrandingOutcome.Inconclusive
        if (response.declaredLength > MAX_RESPONSE_BYTES) return DeArrowBrandingOutcome.Inconclusive
        val stream = response.body ?: return DeArrowBrandingOutcome.Inconclusive
        val body = readBoundedUtf8(stream, MAX_RESPONSE_BYTES)
            ?: return DeArrowBrandingOutcome.Inconclusive
        val branding = parseBranding(body) ?: return DeArrowBrandingOutcome.Inconclusive
        DeArrowBrandingOutcome.Resolved(branding)
    }

    companion object {
        const val BRANDING_ENDPOINT = "https://sponsor.ajay.app/api/branding"
        const val THUMBNAIL_ENDPOINT = "https://dearrow-thumb.ajay.app/api/v1/getThumbnail"
        const val MAX_RESPONSE_BYTES = 64L * 1024L
        const val MAX_ATTEMPTS = 2

        fun thumbnailUrl(videoId: String, timestampSeconds: Double): String? {
            if (!DEARROW_VIDEO_ID_PATTERN.matches(videoId)) return null
            if (!timestampSeconds.isFinite() || timestampSeconds < 0.0) return null
            return "$THUMBNAIL_ENDPOINT?videoID=$videoId&time=$timestampSeconds"
        }

        private fun defaultClient(): OkHttpClient = LevyraHttpClientFactory.general().newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .writeTimeout(2, TimeUnit.SECONDS)
            .callTimeout(4, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .build()
    }
}

private class OkHttpDeArrowFetcher(private val client: OkHttpClient) : DeArrowHttpFetcher {
    override fun fetch(url: String): DeArrowHttpResponse {
        val httpUrl = url.toHttpUrlOrNull() ?: throw IOException("Invalid DeArrow URL")
        val request = Request.Builder()
            .url(httpUrl)
            .get()
            .header("Accept", "application/json")
            .header("User-Agent", "Levyra-Android/1.0 (DeArrow branding, read-only)")
            .build()
        val response = client.newCall(request).execute()
        val body = response.body
        return DeArrowHttpResponse(
            code = response.code,
            declaredLength = body.contentLength(),
            body = body.byteStream(),
            closeAction = response::close
        )
    }
}

internal fun parseBranding(body: String): DeArrowBranding? {
    val root = runCatching { JSONObject(body) }.getOrNull() ?: return null
    val titles = parseTitles(root.optJSONArray("titles"))
    val thumbnails = parseThumbnails(root.optJSONArray("thumbnails"))
    return DeArrowBranding(titles = titles, thumbnails = thumbnails)
}

private fun parseTitles(array: JSONArray?): List<DeArrowTitle> {
    if (array == null) return emptyList()
    val output = ArrayList<DeArrowTitle>(array.length())
    for (index in 0 until array.length()) {
        val item = array.optJSONObject(index) ?: continue
        output += DeArrowTitle(
            title = item.optString("title", ""),
            locked = item.optBoolean("locked", false),
            votes = item.optInt("votes", 0),
            original = item.optBoolean("original", false)
        )
    }
    return output
}

private fun parseThumbnails(array: JSONArray?): List<DeArrowThumbnail> {
    if (array == null) return emptyList()
    val output = ArrayList<DeArrowThumbnail>(array.length())
    for (index in 0 until array.length()) {
        val item = array.optJSONObject(index) ?: continue
        val timestamp = if (!item.has("timestamp") || item.isNull("timestamp")) {
            null
        } else {
            item.optDouble("timestamp", Double.NaN).takeIf(Double::isFinite)
        }
        output += DeArrowThumbnail(
            timestamp = timestamp,
            locked = item.optBoolean("locked", false),
            votes = item.optInt("votes", 0),
            original = item.optBoolean("original", false)
        )
    }
    return output
}

private fun readBoundedUtf8(stream: InputStream, maxBytes: Long): String? {
    val buffer = ByteArray(8 * 1024)
    val output = ByteArrayOutputStream(minOf(maxBytes, 16L * 1024L).toInt())
    var total = 0L
    while (true) {
        val read = stream.read(buffer)
        if (read < 0) break
        total += read
        if (total > maxBytes) return null
        output.write(buffer, 0, read)
    }
    return output.toString(Charsets.UTF_8.name())
}
