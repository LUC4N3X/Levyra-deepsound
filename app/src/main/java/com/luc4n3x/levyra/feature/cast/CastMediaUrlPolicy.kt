package com.luc4n3x.levyra.feature.cast

import java.net.URI
import java.util.Locale

internal fun isCastReceiverSafeMediaUrl(rawUrl: String): Boolean {
    val uri = runCatching { URI(rawUrl.trim()) }.getOrNull() ?: return false
    if (!uri.scheme.equals("https", ignoreCase = true)) return false
    if (uri.rawUserInfo != null) return false
    if (uri.port != -1 && uri.port != 443) return false

    val host = uri.host?.trim()?.lowercase(Locale.ROOT) ?: return false
    if (host != "googlevideo.com" && !host.endsWith(".googlevideo.com")) return false

    val path = uri.rawPath.orEmpty()
    return path.startsWith('/') && path.length > 1
}
