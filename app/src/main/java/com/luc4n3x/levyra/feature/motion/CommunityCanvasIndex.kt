package com.luc4n3x.levyra.feature.motion

import org.json.JSONObject
import java.security.MessageDigest
import java.util.Base64
import java.util.Locale

internal const val COMMUNITY_CANVAS_INDEX_VERSION = 2
internal const val COMMUNITY_CANVAS_INDEX_MIN_PREFIX_CHARS = 2
internal const val COMMUNITY_CANVAS_INDEX_MAX_PREFIX_CHARS = 5

internal data class CommunityCanvasIndexManifest(
    val version: Int,
    val contentDigest: String,
    val prefixChars: Int,
    val shardDirectory: String,
    val entryCount: Int,
    val keyCount: Int,
    val shardCount: Int,
    val largestShardBytes: Int,
    private val shardBitmap: ByteArray
) {
    val cacheKey: String = "$contentDigest:$shardDirectory"

    fun hasShard(prefix: String): Boolean {
        if (prefix.length != prefixChars || prefix.any { it.digitToIntOrNull(16) == null }) return false
        val index = prefix.toInt(16)
        val byteIndex = index / 8
        if (byteIndex !in shardBitmap.indices) return false
        val mask = 1 shl (index % 8)
        return ((shardBitmap[byteIndex].toInt() and 0xff) and mask) != 0
    }
}

internal data class CommunityCanvasIndexedEntry(
    val lookupHash: String,
    val url: String,
    val scope: MotionArtworkScope,
    val isrc: String,
    val width: Int?,
    val height: Int?
) {
    fun toCatalogEntry(identity: MotionTrackIdentity): CommunityCanvasEntry = CommunityCanvasEntry(
        song = identity.title,
        artist = identity.artists.joinToString(", "),
        album = identity.album,
        url = url,
        scope = scope,
        isrc = if (scope == MotionArtworkScope.TRACK) isrc else "",
        width = width,
        height = height
    )
}

internal fun communityCanvasLookupKeys(identity: MotionTrackIdentity): List<String> = buildList {
    val isrc = identity.isrc.trim().uppercase(Locale.ROOT)
    if (COMMUNITY_ISRC_PATTERN.matches(isrc)) add("i|$isrc")

    val artists = combinedArtistSignature(identity.artists)
    val title = normalizeMotionText(identity.title)
    val album = normalizeMotionText(identity.album)
    if (title.isNotBlank() && artists.isNotBlank() && album.isNotBlank()) {
        add("t|$title|$artists|$album")
    }
    if (artists.isNotBlank() && album.isNotBlank()) {
        add("a|$artists|$album")
    }
}.distinct()

private fun communityCanvasLookupDigest(key: String): ByteArray = MessageDigest.getInstance("SHA-256")
    .digest(key.toByteArray(Charsets.UTF_8))

internal fun communityCanvasLookupHash(key: String): String = Base64.getUrlEncoder()
    .withoutPadding()
    .encodeToString(communityCanvasLookupDigest(key))

internal fun communityCanvasShardPrefix(key: String, prefixChars: Int): String =
    communityCanvasLookupDigest(key)
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
        .take(prefixChars)

internal fun parseCommunityCanvasIndexManifest(payload: String): CommunityCanvasIndexManifest? {
    val root = runCatching { JSONObject(payload) }.getOrNull() ?: return null
    if (root.optInt("version") != COMMUNITY_CANVAS_INDEX_VERSION) return null
    if (!root.optString("hash").equals("sha256", ignoreCase = true)) return null
    if (!root.optString("hashEncoding").equals("base64url", ignoreCase = true)) return null

    val contentDigest = root.optString("contentDigest").trim().lowercase(Locale.ROOT)
    if (!COMMUNITY_CONTENT_DIGEST_PATTERN.matches(contentDigest)) return null
    val prefixChars = root.optInt("prefixChars")
    if (prefixChars !in COMMUNITY_CANVAS_INDEX_MIN_PREFIX_CHARS..COMMUNITY_CANVAS_INDEX_MAX_PREFIX_CHARS) {
        return null
    }
    val shardDirectory = root.optString("shardDirectory").trim()
    if (shardDirectory != "p$prefixChars" || !COMMUNITY_SHARD_DIRECTORY_PATTERN.matches(shardDirectory)) {
        return null
    }

    val entryCount = root.optInt("entryCount")
    val keyCount = root.optInt("keyCount")
    val shardCount = root.optInt("shardCount")
    val largestShardBytes = root.optInt("largestShardBytes")
    val maximumShards = 1 shl (prefixChars * 4)
    if (
        entryCount <= 0 ||
        keyCount < entryCount ||
        shardCount !in 1..maximumShards ||
        largestShardBytes <= 0
    ) {
        return null
    }

    val bitmap = runCatching {
        Base64.getDecoder().decode(root.optString("shardBitmap"))
    }.getOrNull() ?: return null
    val expectedBytes = (maximumShards + 7) / 8
    if (bitmap.size != expectedBytes) return null
    val bitmapShardCount = bitmap.sumOf { byte -> Integer.bitCount(byte.toInt() and 0xff) }
    if (bitmapShardCount != shardCount) return null

    return CommunityCanvasIndexManifest(
        version = COMMUNITY_CANVAS_INDEX_VERSION,
        contentDigest = contentDigest,
        prefixChars = prefixChars,
        shardDirectory = shardDirectory,
        entryCount = entryCount,
        keyCount = keyCount,
        shardCount = shardCount,
        largestShardBytes = largestShardBytes,
        shardBitmap = bitmap
    )
}

internal fun parseCommunityCanvasIndexShard(payload: String): List<CommunityCanvasIndexedEntry> {
    val root = runCatching { JSONObject(payload) }.getOrNull() ?: return emptyList()
    if (root.optInt("version") != COMMUNITY_CANVAS_INDEX_VERSION) return emptyList()
    val items = root.optJSONArray("items") ?: return emptyList()
    return buildList {
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val lookupHash = item.optString("h").trim()
            if (!COMMUNITY_LOOKUP_HASH_PATTERN.matches(lookupHash)) continue
            val rawUrl = item.optString("u").trim()
            if (communityCanvasMediaUrl(rawUrl) == null) continue
            val scope = when (item.optString("s").trim().lowercase(Locale.ROOT)) {
                "a" -> MotionArtworkScope.ALBUM
                "t" -> MotionArtworkScope.TRACK
                else -> continue
            }
            add(
                CommunityCanvasIndexedEntry(
                    lookupHash = lookupHash,
                    url = rawUrl,
                    scope = scope,
                    isrc = communityCanvasIsrc(item.optString("i")),
                    width = item.optInt("w").takeIf { it > 0 },
                    height = item.optInt("g").takeIf { it > 0 }
                )
            )
        }
    }
}

private val COMMUNITY_LOOKUP_HASH_PATTERN = Regex("^[A-Za-z0-9_-]{43}$")
private val COMMUNITY_CONTENT_DIGEST_PATTERN = Regex("^[0-9a-f]{64}$")
private val COMMUNITY_SHARD_DIRECTORY_PATTERN = Regex("^p[2-5]$")
