package com.luc4n3x.levyra.feature.recognition

import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.data.security.AndroidKeystoreCredentialStore
import com.luc4n3x.levyra.data.security.SafeImageUrlPolicy
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import kotlin.coroutines.resume

class AudDRecognitionProvider(private val credentials: AndroidKeystoreCredentialStore) : RecognitionProvider {
    override val id = AUDD_PROVIDER_ID

    fun isConfigured(): Boolean = credentials.read(AUDD_CREDENTIAL_SLOT) != null

    fun saveToken(token: String) = credentials.write(AUDD_CREDENTIAL_SLOT, token.trim())

    fun clear() = credentials.clear(AUDD_CREDENTIAL_SLOT)

    override suspend fun identify(fingerprint: AudioFingerprint): RecognitionOutcome {
        val credentialValue = credentials.read(AUDD_CREDENTIAL_SLOT)
            ?: return RecognitionOutcome.Error(RecognitionErrorKind.Unavailable)
        val wav = audDWav(fingerprint) ?: return RecognitionOutcome.Error(RecognitionErrorKind.Fingerprint)
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("api_token", credentialValue)
            .addFormDataPart("return", "apple_music,spotify")
            .addFormDataPart("file", "levyra.wav", wav.toRequestBody(WAV))
            .build()
        val request = Request.Builder().url(AUDD_URL).post(body).build()
        return executeAudDRequest(request)
    }

    internal fun wav(fingerprint: AudioFingerprint): ByteArray? = audDWav(fingerprint)

    internal fun parse(successful: Boolean, payload: String): RecognitionOutcome = parseAudDResponse(successful, payload)

    private companion object {
        const val AUDD_URL = "https://api.audd.io/"
        const val AUDD_CREDENTIAL_SLOT = "audd_token"
        val WAV = "audio/wav".toMediaType()
    }
}

private suspend fun executeAudDRequest(request: Request): RecognitionOutcome =
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
                        val responseBody = current.body
                        if (responseBody.contentLength() > AUDD_MAX_RESPONSE_BYTES) {
                            return@use RecognitionOutcome.Error(RecognitionErrorKind.Network)
                        }
                        val payload = responseBody.source()
                        payload.request(AUDD_MAX_RESPONSE_BYTES + 1L)
                        if (payload.buffer.size > AUDD_MAX_RESPONSE_BYTES) {
                            RecognitionOutcome.Error(RecognitionErrorKind.Network)
                        } else {
                            parseAudDResponse(current.isSuccessful, payload.buffer.readUtf8())
                        }
                    }
                }.getOrDefault(RecognitionOutcome.Error(RecognitionErrorKind.Network))
                if (continuation.isActive) continuation.resume(outcome)
            }
        })
    }

internal fun audDWav(fingerprint: AudioFingerprint): ByteArray? {
    if (fingerprint.sampleRateHz !in 8_000..48_000 || fingerprint.samples.isEmpty()) return null
    val dataBytes = fingerprint.samples.size.toLong() * 2L
    if (dataBytes + AUDD_WAV_HEADER_BYTES > AUDD_MAX_WAV_BYTES) return null
    return ByteBuffer.allocate((dataBytes + AUDD_WAV_HEADER_BYTES).toInt()).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt((dataBytes + 36L).toInt())
            put("WAVEfmt ".toByteArray())
            putInt(16)
            putShort(1)
            putShort(1)
            putInt(fingerprint.sampleRateHz)
            putInt(fingerprint.sampleRateHz * 2)
            putShort(2)
            putShort(16)
            put("data".toByteArray())
            putInt(dataBytes.toInt())
            fingerprint.samples.forEach(::putShort)
    }.array()
}

internal fun parseAudDResponse(successful: Boolean, payload: String): RecognitionOutcome {
    if (!successful) return RecognitionOutcome.Error(RecognitionErrorKind.Network)
    return runCatching {
        val root = JSONObject(payload)
        if (root.optString("status") != "success") return RecognitionOutcome.Error(RecognitionErrorKind.Network)
        val result = root.optJSONObject("result") ?: return RecognitionOutcome.NoMatch
        val title = result.optString("title")
        val artist = result.optString("artist")
        if (title.isBlank() || artist.isBlank()) {
            RecognitionOutcome.NoMatch
        } else {
            val appleMusic = result.optJSONObject("apple_music")
            val spotify = result.optJSONObject("spotify")
            RecognitionOutcome.Match(
                RecognitionResult(
                    title = title,
                    artist = artist,
                    album = result.optString("album"),
                    externalId = result.optString("song_link").ifBlank { result.optString("id") },
                    provider = AUDD_PROVIDER_ID,
                    providerTrackId = result.optString("id"),
                    artworkUrl = auddArtworkUrl(appleMusic),
                    isrc = result.optString("isrc").ifBlank {
                        spotify?.optJSONObject("external_ids")?.optString("isrc").orEmpty()
                    }.trim(),
                    year = auddReleaseYear(result.optString("release_date")),
                    label = result.optString("label")
                )
            )
        }
    }.getOrElse { RecognitionOutcome.Error(RecognitionErrorKind.Network) }
}

internal const val AUDD_PROVIDER_ID = "audd"
internal const val AUDD_MAX_RESPONSE_BYTES = 256L * 1024L
internal const val AUDD_WAV_HEADER_BYTES = 44L
internal const val AUDD_MAX_WAV_BYTES = 9L * 1024L * 1024L

private val AUDD_RELEASE_YEAR_PATTERN = Regex("""\d{4}""")

internal fun auddReleaseYear(releaseDate: String): String =
    AUDD_RELEASE_YEAR_PATTERN.find(releaseDate)?.value.orEmpty()

internal fun auddArtworkUrl(appleMusic: JSONObject?): String {
    val template = appleMusic?.optJSONObject("artwork")?.optString("url").orEmpty().trim()
    val formatted = template.replace("{w}", "600").replace("{h}", "600")
    return SafeImageUrlPolicy.sanitize(formatted)
}
