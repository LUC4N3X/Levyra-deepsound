package com.luc4n3x.levyra.data.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

internal object YoutubeClientIdentityInterceptor : Interceptor {
    internal const val PO_TOKEN_WEB_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    private const val HEADER_CLIENT_NAME = "X-Youtube-Client-Name"
    private const val HEADER_CLIENT_VERSION = "X-Youtube-Client-Version"
    private const val HEADER_ORIGIN = "Origin"
    private const val HEADER_REFERER = "Referer"
    private const val HEADER_USER_AGENT = "User-Agent"

    private const val CLIENT_WEB = YoutubeClientRegistry.CLIENT_ID_WEB
    private const val CLIENT_ANDROID = YoutubeClientRegistry.CLIENT_ID_ANDROID
    private const val CLIENT_IOS = YoutubeClientRegistry.CLIENT_ID_IOS
    private const val CLIENT_ANDROID_MUSIC = YoutubeClientRegistry.CLIENT_ID_ANDROID_MUSIC
    private const val CLIENT_ANDROID_VR = YoutubeClientRegistry.CLIENT_ID_ANDROID_VR
    private const val CLIENT_WEB_EMBEDDED = YoutubeClientRegistry.CLIENT_ID_WEB_EMBEDDED
    private const val CLIENT_WEB_REMIX = YoutubeClientRegistry.CLIENT_ID_WEB_REMIX

    private val nativeClientNames = setOf(
        CLIENT_ANDROID,
        CLIENT_IOS,
        CLIENT_ANDROID_MUSIC,
        CLIENT_ANDROID_VR
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.proceed(normalize(chain.request()))
    }

    internal fun normalize(request: Request): Request {
        val clientName = request.header(HEADER_CLIENT_NAME) ?: return request
        val builder = request.newBuilder()

        when {
            clientName == CLIENT_ANDROID_MUSIC -> {
                request.header(HEADER_CLIENT_VERSION)
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
                    ?.let { version ->
                        builder.header(
                            HEADER_USER_AGENT,
                            "com.google.android.apps.youtube.music/$version " +
                                "(Linux; U; Android 15) gzip"
                        )
                    }
                builder.removeHeader(HEADER_ORIGIN)
                builder.removeHeader(HEADER_REFERER)
            }

            clientName in nativeClientNames -> {
                builder.removeHeader(HEADER_ORIGIN)
                builder.removeHeader(HEADER_REFERER)
            }

            clientName == CLIENT_WEB ||
                clientName == CLIENT_WEB_REMIX ||
                clientName == CLIENT_WEB_EMBEDDED -> {
                builder.header(HEADER_USER_AGENT, PO_TOKEN_WEB_USER_AGENT)
                normalizeBrowserNavigationHeaders(request, builder, clientName)
            }

            else -> return request
        }

        return builder.build()
    }

    private fun normalizeBrowserNavigationHeaders(
        request: Request,
        builder: Request.Builder,
        clientName: String
    ) {
        val existingOrigin = request.header(HEADER_ORIGIN)
        val existingReferer = request.header(HEADER_REFERER)
        if (existingOrigin == null && existingReferer == null) return

        val host = if (clientName == CLIENT_WEB_REMIX) {
            "music.youtube.com"
        } else {
            "www.youtube.com"
        }
        builder.header(HEADER_ORIGIN, "https://$host")
        builder.header(
            HEADER_REFERER,
            normalizeReferer(existingReferer, host, clientName == CLIENT_WEB_EMBEDDED)
        )
    }

    private fun normalizeReferer(
        existingReferer: String?,
        host: String,
        embedded: Boolean
    ): String {
        val parsed = existingReferer?.toHttpUrlOrNull()
        if (parsed == null) return "https://$host/"

        if (!embedded && parsed.host == "youtu.be") {
            val videoId = parsed.pathSegments.firstOrNull().orEmpty()
            if (videoId.isNotBlank()) return "https://$host/watch?v=$videoId"
        }

        if (embedded && !parsed.encodedPath.startsWith("/embed/")) {
            return "https://$host/"
        }

        return parsed.newBuilder()
            .scheme("https")
            .host(host)
            .build()
            .toString()
    }
}
