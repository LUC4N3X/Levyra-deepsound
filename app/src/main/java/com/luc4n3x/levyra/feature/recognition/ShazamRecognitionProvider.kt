package com.luc4n3x.levyra.feature.recognition

import android.util.Base64
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.data.security.SafeImageUrlPolicy
import java.io.IOException
import java.net.URI
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume

class ShazamRecognitionProvider(
    private val endpointFactory: (String, String) -> String = ::defaultShazamEndpoint
) : RecognitionProvider {
    override val id: String = SHAZAM_PROVIDER_ID

    override suspend fun identify(fingerprint: AudioFingerprint): RecognitionOutcome {
        if (fingerprint.sampleRateHz != ShazamSignatureGenerator.SAMPLE_RATE_HZ) {
            return RecognitionOutcome.Error(RecognitionErrorKind.Fingerprint)
        }
        val signature = withContext(Dispatchers.Default) {
            ShazamSignatureGenerator().generate(fingerprint.samples)
        } ?: return RecognitionOutcome.NoMatch

        val body = requestBody(signature).toString().toByteArray(Charsets.UTF_8)
        if (body.size > MAX_REQUEST_BYTES) return RecognitionOutcome.Error(RecognitionErrorKind.Fingerprint)

        val request = Request.Builder()
            .url(
                endpointFactory(
                    UUID.randomUUID().toString().uppercase(Locale.ROOT),
                    UUID.randomUUID().toString().uppercase(Locale.ROOT)
                )
            )
            .header("User-Agent", USER_AGENT)
            .header("X-Shazam-Platform", SHAZAM_PLATFORM)
            .header("X-Shazam-AppVersion", SHAZAM_APP_VERSION)
            .header("Accept", "*/*")
            .header("Accept-Language", ACCEPT_LANGUAGE)
            .header("Content-Language", CONTENT_LANGUAGE)
            .post(body.toRequestBody(JSON))
            .build()

        return executeShazamRequest(request)
    }

    internal fun requestBody(signature: ShazamSignature): JSONObject {
        val timestampMillis = System.currentTimeMillis()
        val uri = ShazamSignatureGenerator.SIGNATURE_URI_PREFIX +
            Base64.encodeToString(signature.payload, Base64.NO_WRAP)
        return JSONObject().apply {
            put("timezone", TIMEZONE)
            put(
                "signature",
                JSONObject().apply {
                    put("uri", uri)
                    put("samplems", signature.sampleDurationMs)
                }
            )
            put("timestamp", timestampMillis)
            put("context", JSONObject())
            put("geolocation", JSONObject())
        }
    }

    private companion object {
        const val USER_AGENT = "Dalvik/2.1.0 (Linux; U; Android 6.0.1; SM-G920F Build/MMB29K)"
        const val SHAZAM_PLATFORM = "IPHONE"
        const val SHAZAM_APP_VERSION = "14.1.0"
        const val ACCEPT_LANGUAGE = "en-US"
        const val CONTENT_LANGUAGE = "en_US"
        const val TIMEZONE = "Europe/Moscow"
        const val MAX_REQUEST_BYTES = 512 * 1024
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}

private suspend fun executeShazamRequest(request: Request): RecognitionOutcome =
    suspendCancellableCoroutine { continuation ->
        val call = LevyraHttpClientFactory.externalIntegrations().newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) {
                    continuation.resume(RecognitionOutcome.Error(RecognitionErrorKind.Network))
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val outcome = runCatching {
                    response.use { current ->
                        if (!current.isSuccessful) return@use classifyShazamHttpFailure(current.code)
                        val source = current.body.source()
                        source.request(MAX_SHAZAM_RESPONSE_BYTES + 1L)
                        if (source.buffer.size > MAX_SHAZAM_RESPONSE_BYTES) {
                            RecognitionOutcome.Error(RecognitionErrorKind.Network)
                        } else {
                            parseShazamResponse(source.buffer.readUtf8())
                        }
                    }
                }.getOrDefault(RecognitionOutcome.Error(RecognitionErrorKind.Network))
                if (continuation.isActive) continuation.resume(outcome)
            }
        })
    }

internal const val SHAZAM_PROVIDER_ID = "shazam"
internal const val MAX_SHAZAM_RESPONSE_BYTES = 512L * 1024L
private const val HTTP_NOT_FOUND = 404
private const val HTTP_TOO_MANY_REQUESTS = 429

internal fun defaultShazamEndpoint(requestId: String, deviceId: String): String =
    "https://amp.shazam.com/discovery/v5/en-US/US/android/-/tag/$requestId/$deviceId" +
        "?sync=true&webv3=true&sampling=true&connected=&shazamapiversion=v3&sharehub=true" +
        "&hubv5minorversion=v5.1&hidelb=true&video=v3"

internal fun classifyShazamHttpFailure(code: Int): RecognitionOutcome = when (code) {
    HTTP_NOT_FOUND -> RecognitionOutcome.NoMatch
    HTTP_TOO_MANY_REQUESTS -> RecognitionOutcome.Error(RecognitionErrorKind.Unavailable)
    else -> RecognitionOutcome.Error(RecognitionErrorKind.Network)
}

internal fun parseShazamResponse(payload: String): RecognitionOutcome = runCatching {
    val root = JSONObject(payload)
    val track = root.optJSONObject("track") ?: return RecognitionOutcome.NoMatch
    val title = track.optString("title").trim()
    val artist = track.optString("subtitle").trim()
    if (title.isBlank() || artist.isBlank()) return RecognitionOutcome.NoMatch

    val songMetadata = shazamSectionMetadata(track.optJSONArray("sections"), "SONG")
    val images = track.optJSONObject("images")
    val coverArt = images?.optString("coverarthq").orEmpty().ifBlank {
        images?.optString("coverart").orEmpty()
    }

    RecognitionOutcome.Match(
        RecognitionResult(
            title = title,
            artist = artist,
            album = shazamMetadataValue(songMetadata, "Album"),
            externalId = SafeImageUrlPolicy.sanitize(track.optString("url")),
            provider = SHAZAM_PROVIDER_ID,
            providerTrackId = track.optString("key").trim(),
            artworkUrl = SafeImageUrlPolicy.sanitize(coverArt),
            isrc = track.optString("isrc").trim(),
            youtubeVideoId = shazamYoutubeVideoId(track.optJSONObject("hub")),
            year = shazamReleaseYear(shazamMetadataValue(songMetadata, "Released")),
            label = shazamMetadataValue(songMetadata, "Label"),
            genre = track.optJSONObject("genres")?.optString("primary").orEmpty().trim()
        )
    )
}.getOrElse { RecognitionOutcome.Error(RecognitionErrorKind.Network) }

private fun shazamSectionMetadata(sections: JSONArray?, type: String): JSONArray? {
    sections ?: return null
    for (index in 0 until sections.length()) {
        val section = sections.optJSONObject(index) ?: continue
        if (section.optString("type") == type) return section.optJSONArray("metadata")
    }
    return null
}

private fun shazamMetadataValue(metadata: JSONArray?, title: String): String {
    metadata ?: return ""
    for (index in 0 until metadata.length()) {
        val entry = metadata.optJSONObject(index) ?: continue
        if (entry.optString("title").equals(title, ignoreCase = true)) {
            return entry.optString("text").trim()
        }
    }
    return ""
}

private val RELEASE_YEAR_PATTERN = Regex("\\d{4}")
private val YOUTUBE_QUERY_ID_PATTERN = Regex("[?&]v=([A-Za-z0-9_-]{11})")
private val YOUTUBE_PATH_ID_PATTERN = Regex("/([A-Za-z0-9_-]{11})(?:[?&#]|$)")

private fun shazamReleaseYear(released: String): String =
    RELEASE_YEAR_PATTERN.find(released)?.value.orEmpty()

private fun shazamYoutubeVideoId(hub: JSONObject?): String {
    val options = hub?.optJSONArray("options") ?: return ""
    for (index in 0 until options.length()) {
        val option = options.optJSONObject(index) ?: continue
        if (!option.optString("type").contains("video", ignoreCase = true)) continue
        val actions = option.optJSONArray("actions") ?: continue
        for (actionIndex in 0 until actions.length()) {
            val uri = actions.optJSONObject(actionIndex)?.optString("uri").orEmpty()
            val id = extractYoutubeVideoId(uri)
            if (id.isNotBlank()) return id
        }
    }
    return ""
}

internal fun extractYoutubeVideoId(uri: String): String {
    if (uri.isBlank()) return ""
    YOUTUBE_QUERY_ID_PATTERN.find(uri)?.groupValues?.getOrNull(1)?.let { return it }
    return YOUTUBE_PATH_ID_PATTERN.find(uri)?.groupValues?.getOrNull(1).orEmpty()
}
