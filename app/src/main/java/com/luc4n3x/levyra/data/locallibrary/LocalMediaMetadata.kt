package com.luc4n3x.levyra.data.locallibrary

import com.luc4n3x.levyra.data.local.LocalMediaEntity
import java.text.Normalizer
import java.util.Locale

internal const val LEVYRA_DOWNLOAD_RELATIVE_PATH = "Music/Levyra/"
internal const val LOCAL_MEDIA_TRACK_ID_PREFIX = "local:"
internal const val LOCAL_MEDIA_SOURCE = "Offline"

private const val MEDIA_STORE_UNKNOWN = "<unknown>"
private val LocalDiacritics = Regex("\\p{M}+")
private val LocalNonAlphanumeric = Regex("[^\\p{L}\\p{N}]+")
private val LocalWhitespace = Regex("\\s+")
private val LocalFeaturingSplit = Regex("\\s*[(\\[]?\\s*\\b(?:feat\\.?|ft\\.?|featuring)\\s+", RegexOption.IGNORE_CASE)
private val LocalLeadingNumber = Regex("^\\s*(\\d+)")
private val LocalStorageRoot = Regex("^/storage/(?:emulated/\\d+|[^/]+)/")

data class ScannedLocalAudio(
    val volumeName: String,
    val mediaStoreId: Long,
    val contentUri: String,
    val filePath: String,
    val relativePath: String,
    val displayName: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val genre: String,
    val year: Int,
    val trackField: Int,
    val trackText: String,
    val discText: String,
    val durationMs: Long,
    val mimeType: String,
    val bitrate: Int,
    val sizeBytes: Long,
    val dateAddedMs: Long,
    val dateModifiedMs: Long,
    val albumId: Long
)

internal fun cleanMediaStoreText(value: String?): String {
    val trimmed = value?.trim().orEmpty()
    return if (trimmed.equals(MEDIA_STORE_UNKNOWN, ignoreCase = true)) "" else trimmed.replace(LocalWhitespace, " ")
}

internal fun localGroupKey(value: String): String =
    Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(LocalDiacritics, "")
        .lowercase(Locale.ROOT)
        .replace(LocalNonAlphanumeric, " ")
        .replace(LocalWhitespace, " ")
        .trim()

internal fun localTitleFallback(rawTitle: String, displayName: String, mediaStoreId: Long): String {
    cleanMediaStoreText(rawTitle).takeIf { it.isNotEmpty() }?.let { return it }
    val fromFile = displayName.substringBeforeLast('.', displayName).trim()
    if (fromFile.isNotEmpty()) return fromFile
    return "Audio $mediaStoreId"
}

internal fun localArtistFallback(rawArtist: String, rawAlbumArtist: String): String =
    cleanMediaStoreText(rawArtist).ifEmpty { cleanMediaStoreText(rawAlbumArtist) }

internal fun localPrimaryArtist(artist: String): String =
    artist.split(LocalFeaturingSplit, limit = 2).first().trim().trimEnd('(', '[', ',', ' ')

internal fun localTrackNumber(trackField: Int, trackText: String): Int {
    LocalLeadingNumber.find(trackText)?.groupValues?.get(1)?.toIntOrNull()?.let { return it.coerceIn(0, 9_999) }
    if (trackField <= 0) return 0
    return if (trackField >= 1_000) trackField % 1_000 else trackField
}

internal fun localDiscNumber(trackField: Int, discText: String): Int {
    LocalLeadingNumber.find(discText)?.groupValues?.get(1)?.toIntOrNull()?.let { return it.coerceIn(0, 999) }
    return if (trackField >= 1_000) trackField / 1_000 else 0
}

internal fun normalizeLocalRelativePath(relativePath: String): String {
    val cleaned = relativePath.replace('\\', '/').trim().trim('/')
    return if (cleaned.isEmpty()) "" else "$cleaned/"
}

internal fun relativePathFromFilePath(filePath: String): String {
    val normalized = filePath.replace('\\', '/')
    val parent = normalized.substringBeforeLast('/', "")
    if (parent.isEmpty()) return ""
    val withSlash = "$parent/"
    val root = LocalStorageRoot.find(withSlash)?.value.orEmpty()
    return normalizeLocalRelativePath(withSlash.removePrefix(root))
}

internal fun localFolderName(relativePath: String): String =
    relativePath.trimEnd('/').substringAfterLast('/')

internal fun localFolderKey(volumeName: String, relativePath: String): String =
    "${volumeName.lowercase(Locale.ROOT)}:${relativePath.lowercase(Locale.ROOT)}"

internal fun localIdentityKey(volumeName: String, mediaStoreId: Long): String =
    "ms:${volumeName.lowercase(Locale.ROOT)}:$mediaStoreId"

internal fun localAlbumKey(album: String, albumArtist: String, folderKey: String): String {
    val albumPart = localGroupKey(album)
    val artistPart = localGroupKey(localPrimaryArtist(albumArtist))
    return when {
        albumPart.isEmpty() -> "folder:$folderKey"
        artistPart.isNotEmpty() -> "artist:$artistPart|$albumPart"
        else -> "folder:$folderKey|$albumPart"
    }
}

internal fun localArtistKey(artist: String): String = localGroupKey(localPrimaryArtist(artist))

internal fun localContentFingerprint(sizeBytes: Long, durationMs: Long, title: String): String {
    if (sizeBytes <= 0L || durationMs <= 0L) return ""
    return "$sizeBytes:${durationMs / 1_000L}:${localGroupKey(title)}"
}

internal fun isLevyraDownloadPath(relativePath: String): Boolean =
    relativePath.startsWith(LEVYRA_DOWNLOAD_RELATIVE_PATH, ignoreCase = true)

internal fun ScannedLocalAudio.toLocalMediaEntity(levyraTrackId: String, now: Long): LocalMediaEntity {
    val relative = normalizeLocalRelativePath(relativePath)
    val folderKey = localFolderKey(volumeName, relative)
    val resolvedTitle = localTitleFallback(title, displayName, mediaStoreId)
    val resolvedArtist = localArtistFallback(artist, albumArtist)
    val resolvedAlbumArtist = cleanMediaStoreText(albumArtist)
    val resolvedAlbum = cleanMediaStoreText(album)
    return LocalMediaEntity(
        identityKey = localIdentityKey(volumeName, mediaStoreId),
        contentUri = contentUri,
        volumeName = volumeName,
        mediaStoreId = mediaStoreId,
        filePath = filePath,
        relativePath = relative,
        displayName = displayName,
        folderKey = folderKey,
        folderName = localFolderName(relative),
        title = resolvedTitle,
        artist = resolvedArtist,
        album = resolvedAlbum,
        albumArtist = resolvedAlbumArtist,
        genre = cleanMediaStoreText(genre),
        composer = "",
        lyricist = "",
        comment = "",
        copyright = "",
        customTags = "",
        fullTagSearchText = buildLocalFullTagSearchText(
            resolvedTitle,
            resolvedArtist,
            resolvedAlbum,
            resolvedAlbumArtist,
            cleanMediaStoreText(genre),
            year.takeIf { it in 1..9_999 } ?: 0,
            localTrackNumber(trackField, trackText),
            localDiscNumber(trackField, discText),
            displayName,
            localFolderName(relative),
            "",
            "",
            "",
            "",
            ""
        ),
        year = year.takeIf { it in 1..9_999 } ?: 0,
        trackNumber = localTrackNumber(trackField, trackText),
        discNumber = localDiscNumber(trackField, discText),
        durationMs = durationMs.coerceAtLeast(0L),
        mimeType = mimeType,
        bitrate = bitrate.coerceAtLeast(0),
        sizeBytes = sizeBytes.coerceAtLeast(0L),
        dateAddedMs = dateAddedMs.coerceAtLeast(0L),
        dateModifiedMs = dateModifiedMs.coerceAtLeast(0L),
        albumId = albumId,
        albumKey = localAlbumKey(resolvedAlbum, resolvedAlbumArtist, folderKey),
        artistKey = localArtistKey(resolvedArtist),
        contentFingerprint = localContentFingerprint(sizeBytes, durationMs, resolvedTitle),
        levyraTrackId = levyraTrackId,
        isLevyraDownload = levyraTrackId.isNotEmpty() || isLevyraDownloadPath(relative),
        available = true,
        missingSince = 0L,
        lastSeenAt = now
    )
}

internal fun mediaStoreIdFromUri(uri: String): Long? {
    if (!uri.startsWith("content://media/", ignoreCase = true)) return null
    return uri.substringBefore('?').substringAfterLast('/').toLongOrNull()
}

internal fun mediaStoreIdentityFromUri(uri: String): String? {
    if (!uri.startsWith("content://media/", ignoreCase = true)) return null
    val path = uri.substringAfter("content://media/").substringBefore('?').trim('/')
    val volumeName = path.substringBefore('/').takeIf { it.isNotBlank() } ?: return null
    val mediaStoreId = path.substringAfterLast('/').toLongOrNull() ?: return null
    return localIdentityKey(volumeName, mediaStoreId)
}
