package com.luc4n3x.levyra.player

internal object GooglevideoMediaNetwork {
    const val MAX_ALTERNATE_CANDIDATES = 3

    private const val HTTPS_PREFIX = "https://"
    private const val GOOGLEVIDEO_SUFFIX = ".googlevideo.com"
    private const val MEDIA_NETWORK_SEPARATOR = "---"
    private val hostPrefixPattern = Regex("^[a-z0-9-]{1,32}$")
    private val mediaNetworkPattern = Regex("^[a-z0-9-]{1,48}$")
    private val endpointFailureStatuses = setOf(404, 500, 502, 503, 504)

    fun isEndpointFailure(responseCode: Int?): Boolean =
        responseCode == null || responseCode in endpointFailureStatuses

    fun isGooglevideoHost(host: String): Boolean {
        val normalized = host.lowercase().trimEnd('.')
        return normalized.endsWith(GOOGLEVIDEO_SUFFIX) && normalized.length > GOOGLEVIDEO_SUFFIX.length
    }

    fun alternateUrls(url: String, limit: Int = MAX_ALTERNATE_CANDIDATES): List<String> {
        if (limit <= 0 || !url.startsWith(HTTPS_PREFIX, ignoreCase = true)) return emptyList()
        val authorityEnd = url.indexOfAnyOrEnd(HTTPS_PREFIX.length, '/', '?', '#')
        val authority = url.substring(HTTPS_PREFIX.length, authorityEnd)
        if (authority.isEmpty() || authority.contains('@') || authority.contains(':')) return emptyList()

        val host = authority.lowercase().trimEnd('.')
        if (!isGooglevideoHost(host)) return emptyList()
        val label = host.removeSuffix(GOOGLEVIDEO_SUFFIX)
        val separator = label.indexOf(MEDIA_NETWORK_SEPARATOR)
        if (separator <= 0) return emptyList()
        val prefix = label.substring(0, separator)
        val current = label.substring(separator + MEDIA_NETWORK_SEPARATOR.length)
        if (!hostPrefixPattern.matches(prefix) || !mediaNetworkPattern.matches(current)) return emptyList()

        val declared = queryParameter(url, authorityEnd, "mn") ?: return emptyList()
        val suffix = url.substring(authorityEnd)
        return declared.split(',')
            .asSequence()
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() && it != current && mediaNetworkPattern.matches(it) }
            .distinct()
            .take(limit)
            .map { network -> "$HTTPS_PREFIX$prefix$MEDIA_NETWORK_SEPARATOR$network$GOOGLEVIDEO_SUFFIX$suffix" }
            .toList()
    }

    private fun queryParameter(url: String, authorityEnd: Int, name: String): String? {
        val queryStart = url.indexOf('?', authorityEnd)
        if (queryStart < 0) return null
        val fragmentStart = url.indexOf('#', queryStart).takeIf { it >= 0 } ?: url.length
        val query = url.substring(queryStart + 1, fragmentStart)
        for (pair in query.split('&')) {
            val separator = pair.indexOf('=')
            if (separator <= 0) continue
            if (pair.regionMatches(0, name, 0, separator, ignoreCase = false) && separator == name.length) {
                return pair.substring(separator + 1).takeIf { it.isNotBlank() }
            }
        }
        return null
    }

    private fun String.indexOfAnyOrEnd(from: Int, vararg characters: Char): Int {
        for (index in from until length) {
            if (this[index] in characters) return index
        }
        return length
    }
}
