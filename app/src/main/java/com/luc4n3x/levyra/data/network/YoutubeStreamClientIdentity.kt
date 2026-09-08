package com.luc4n3x.levyra.data.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal data class YoutubeStreamClientIdentity(
    val clientName: String,
    val clientHeaderName: String,
    val clientVersion: String,
    val userAgent: String,
    val requiresPoToken: Boolean,
    val videoId: String = "",
    val origin: String = "",
    val referer: String = "",
    val expiresAtMs: Long = 0L
) {
    fun mediaRequestHeaders(): Map<String, String> {
        val headers = linkedMapOf(
            "User-Agent" to userAgent,
            "Accept" to "*/*",
            "Accept-Encoding" to "identity"
        )
        val navigation = YoutubeClientIdentityInterceptor.mediaNavigationFor(clientHeaderName, videoId)
        if (navigation != null) {
            headers["Origin"] = navigation.origin
            headers["Referer"] = navigation.referer
            headers["Sec-Fetch-Dest"] = "empty"
            headers["Sec-Fetch-Mode"] = "cors"
            headers["Sec-Fetch-Site"] = "cross-site"
        }
        return headers
    }
}

internal object YoutubeStreamClientIdentityRegistry {
    private const val MAX_ENTRIES = 256

    private val entries = object : LinkedHashMap<String, YoutubeStreamClientIdentity>(64, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, YoutubeStreamClientIdentity>
        ): Boolean = size > MAX_ENTRIES
    }
    private val ambiguousMediaKeys = LinkedHashSet<String>()

    fun register(urls: Collection<String>, identity: YoutubeStreamClientIdentity) {
        val keys = urls.asSequence()
            .filter { it.isNotBlank() }
            .flatMap { keysFor(it).asSequence() }
            .toList()
        if (keys.isEmpty()) return
        synchronized(entries) {
            keys.forEach { key ->
                if (!key.startsWith("media\u0000")) {
                    entries[key] = identity
                } else if (key !in ambiguousMediaKeys) {
                    val existing = entries[key]
                    if (existing == null || existing == identity) {
                        entries[key] = identity
                    } else {
                        entries.remove(key)
                        if (ambiguousMediaKeys.size >= MAX_ENTRIES) ambiguousMediaKeys.clear()
                        ambiguousMediaKeys += key
                    }
                }
            }
        }
    }

    fun find(url: String): YoutubeStreamClientIdentity? {
        if (url.isBlank()) return null
        val keys = keysFor(url)
        val nowMs = System.currentTimeMillis()
        synchronized(entries) {
            keys.forEach { key ->
                val identity = entries[key] ?: return@forEach
                if (identity.expiresAtMs > 0L && identity.expiresAtMs <= nowMs) {
                    entries.remove(key)
                } else {
                    return identity
                }
            }
        }
        return null
    }

    fun clear() {
        synchronized(entries) {
            entries.clear()
            ambiguousMediaKeys.clear()
        }
    }

    private fun keysFor(url: String): List<String> {
        val keys = ArrayList<String>(2)
        keys += "url" + '\u0000' + url.substringBefore('#')
        val parsed = url.toHttpUrlOrNull()
        if (parsed != null) {
            val mediaId = parsed.queryParameter("id").orEmpty()
            val itag = parsed.queryParameter("itag").orEmpty()
            if (mediaId.isNotBlank() && itag.isNotBlank()) {
                keys += "media" + '\u0000' + mediaId + '\u0000' + itag
            }
        }
        return keys
    }
}
