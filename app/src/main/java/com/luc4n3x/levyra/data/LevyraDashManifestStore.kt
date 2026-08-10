package com.luc4n3x.levyra.data

import android.net.Uri
import androidx.media3.exoplayer.dash.manifest.DashManifest
import androidx.media3.exoplayer.dash.manifest.DashManifestParser
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.LinkedHashMap

internal object LevyraDashManifestStore {
    internal const val MAX_INLINE_DASH_BYTES = 1024 * 1024
    private const val MAX_ENTRIES = 16
    private val manifests = object : LinkedHashMap<String, DashManifest>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, DashManifest>?): Boolean =
            size > MAX_ENTRIES
    }

    @Synchronized
    fun register(sourceUrl: String, content: String): String? {
        if (sourceUrl.isBlank() || content.isBlank()) return null
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        if (bytes.size > MAX_INLINE_DASH_BYTES) return null
        val manifest = runCatching {
            DashManifestParser().parse(Uri.parse(sourceUrl), ByteArrayInputStream(bytes))
        }.getOrNull() ?: return null
        if (manifest.dynamic) return null
        val key = keyFor(sourceUrl, content)
        manifests[key] = manifest
        return key
    }

    @Synchronized
    fun get(key: String): DashManifest? = manifests[key]

    @Synchronized
    fun contains(key: String): Boolean = key.isNotBlank() && manifests.containsKey(key)

    fun keyFor(sourceUrl: String, content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(sourceUrl.toByteArray(StandardCharsets.UTF_8))
        digest.update(0.toByte())
        digest.update(content.toByteArray(StandardCharsets.UTF_8))
        return digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }
}
