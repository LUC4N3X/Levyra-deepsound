package com.luc4n3x.levyra.feature.motion

import android.content.Context
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.concurrent.TimeUnit

sealed interface MotionArtworkVerificationResult {
    data object Verified : MotionArtworkVerificationResult
    data object Invalid : MotionArtworkVerificationResult
    data class Failed(val cause: Throwable? = null) : MotionArtworkVerificationResult
}

class MotionArtworkUrlVerifier(context: Context) {
    private val client: OkHttpClient = LevyraHttpClientFactory.media(context).newBuilder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun verify(candidate: MotionArtworkCandidate): MotionArtworkVerificationResult = withContext(Dispatchers.IO) {
        if (!candidate.url.startsWith("https://")) return@withContext MotionArtworkVerificationResult.Invalid
        try {
            val head = Request.Builder()
                .url(candidate.url)
                .head()
                .header("User-Agent", USER_AGENT)
                .build()
            client.newCall(head).execute().use { response ->
                if (response.isSuccessful) {
                    return@withContext if (contentLooksPlayable(response.header("Content-Type"), candidate)) {
                        MotionArtworkVerificationResult.Verified
                    } else {
                        MotionArtworkVerificationResult.Invalid
                    }
                }
                when {
                    response.code in setOf(400, 403, 405) -> Unit
                    response.code == 404 || response.code == 410 -> return@withContext MotionArtworkVerificationResult.Invalid
                    else -> return@withContext MotionArtworkVerificationResult.Failed()
                }
            }
            val probe = Request.Builder()
                .url(candidate.url)
                .get()
                .header("Range", "bytes=0-1023")
                .header("User-Agent", USER_AGENT)
                .build()
            client.newCall(probe).execute().use { response ->
                when {
                    response.isSuccessful || response.code == 206 -> {
                        if (contentLooksPlayable(response.header("Content-Type"), candidate)) {
                            MotionArtworkVerificationResult.Verified
                        } else {
                            MotionArtworkVerificationResult.Invalid
                        }
                    }
                    response.code == 404 || response.code == 410 -> MotionArtworkVerificationResult.Invalid
                    else -> MotionArtworkVerificationResult.Failed()
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.d(error, "Motion artwork verification failed")
            MotionArtworkVerificationResult.Failed(error)
        }
    }

    private fun contentLooksPlayable(contentType: String?, candidate: MotionArtworkCandidate): Boolean {
        val normalized = contentType.orEmpty().substringBefore(';').trim().lowercase()
        if (normalized.startsWith("video/")) return true
        if (normalized.contains("mpegurl")) return true
        if (normalized.isNotBlank() && normalized != "application/octet-stream") return false
        return candidate.url.substringBefore('?').let {
            it.endsWith(".mp4", true) || it.endsWith(".m3u8", true)
        }
    }

    private companion object {
        const val USER_AGENT = "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/130 Mobile Safari/537.36"
    }
}
