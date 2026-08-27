package com.luc4n3x.levyra.feature.recognition

import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.data.security.AndroidKeystoreCredentialStore
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.CancellationException
import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AudDRecognitionProvider(private val credentials: AndroidKeystoreCredentialStore) : RecognitionProvider {
    override val id = "audd"

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
        return try {
            LevyraHttpClientFactory.externalIntegrations().newCall(request).execute().use { response ->
                parseAudDResponse(response.isSuccessful, response.body?.string().orEmpty())
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            RecognitionOutcome.Error(RecognitionErrorKind.Network)
        }
    }

    internal fun wav(fingerprint: AudioFingerprint): ByteArray? = audDWav(fingerprint)

    internal fun parse(successful: Boolean, payload: String): RecognitionOutcome = parseAudDResponse(successful, payload)

    private companion object {
        const val AUDD_URL = "https://api.audd.io/"
        const val AUDD_CREDENTIAL_SLOT = "audd_token"
        val WAV = "audio/wav".toMediaType()
    }
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
        if (title.isBlank() || artist.isBlank()) RecognitionOutcome.NoMatch else RecognitionOutcome.Match(
                RecognitionResult(
                    title = title,
                    artist = artist,
                    album = result.optString("album"),
                    externalId = result.optString("song_link").ifBlank { result.optString("id") }
                )
        )
    }.getOrElse { RecognitionOutcome.Error(RecognitionErrorKind.Network) }
}

internal const val AUDD_WAV_HEADER_BYTES = 44L
internal const val AUDD_MAX_WAV_BYTES = 9L * 1024L * 1024L
