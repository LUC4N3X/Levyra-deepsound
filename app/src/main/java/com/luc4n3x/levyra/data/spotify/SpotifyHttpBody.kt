package com.luc4n3x.levyra.data.spotify

import okhttp3.ResponseBody

internal const val SPOTIFY_MAX_JSON_RESPONSE_BYTES = 1L * 1024L * 1024L

internal fun ResponseBody.readSpotifyJsonBody(): String? {
    val declaredLength = contentLength()
    if (declaredLength > SPOTIFY_MAX_JSON_RESPONSE_BYTES) return null
    val payload = source()
    payload.request(SPOTIFY_MAX_JSON_RESPONSE_BYTES + 1L)
    if (payload.buffer.size > SPOTIFY_MAX_JSON_RESPONSE_BYTES) return null
    return payload.buffer.readUtf8().takeIf(String::isNotEmpty)
}
