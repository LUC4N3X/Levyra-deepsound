package com.luc4n3x.levyra.data.spotify

import okhttp3.ResponseBody
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume

internal const val SPOTIFY_MAX_JSON_RESPONSE_BYTES = 1L * 1024L * 1024L

internal fun ResponseBody.readSpotifyJsonBody(): String? {
    val declaredLength = contentLength()
    if (declaredLength > SPOTIFY_MAX_JSON_RESPONSE_BYTES) return null
    val payload = source()
    payload.request(SPOTIFY_MAX_JSON_RESPONSE_BYTES + 1L)
    if (payload.buffer.size > SPOTIFY_MAX_JSON_RESPONSE_BYTES) return null
    return payload.buffer.readUtf8().takeIf(String::isNotEmpty)
}

internal suspend fun Call.awaitSpotifyJson(
    observeResponse: (Response) -> Unit = {}
): JSONObject? = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) continuation.resume(null)
        }

        override fun onResponse(call: Call, response: Response) {
            val parsed = response.use {
                try {
                    observeResponse(it)
                    if (!it.isSuccessful) return@use null
                    it.body.readSpotifyJsonBody()?.let(::JSONObject)
                } catch (_: Exception) {
                    null
                }
            }
            if (continuation.isActive) continuation.resume(parsed)
        }
    })
}
