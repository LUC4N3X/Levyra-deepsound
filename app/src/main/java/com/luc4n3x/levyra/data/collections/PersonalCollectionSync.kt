package com.luc4n3x.levyra.data.collections

import com.luc4n3x.levyra.data.local.PersonalCollectionTrackEntity
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

enum class CollectionSyncError {
    Offline,
    Timeout,
    Unauthorized,
    NotFound,
    Server,
    Tls,
    Cleartext,
    LocalNetwork,
    BlockedAddress,
    TooLarge,
    NotPlaylist,
    Empty,
    HlsManifest,
    Malformed,
    NoPlayableEntries,
    Storage,
    Unknown;

    companion object {
        fun fromStorage(value: String): CollectionSyncError? =
            entries.firstOrNull { it.name == value }

        fun from(parseError: CollectionParseError): CollectionSyncError = when (parseError) {
            CollectionParseError.Empty -> Empty
            CollectionParseError.TooLarge -> TooLarge
            CollectionParseError.HlsManifest -> HlsManifest
            CollectionParseError.Malformed -> Malformed
            CollectionParseError.NoPlayableEntries -> NoPlayableEntries
        }

        fun from(error: Throwable): CollectionSyncError = when (error) {
            is CollectionAccessDeniedException -> when (error.reason) {
                CollectionAccessDenial.Cleartext -> Cleartext
                CollectionAccessDenial.LocalNetwork -> LocalNetwork
                CollectionAccessDenial.BlockedAddress, CollectionAccessDenial.UnknownHost -> BlockedAddress
            }
            is CollectionHttpException -> when (error.code) {
                401, 403, 407 -> Unauthorized
                404, 410 -> NotFound
                else -> Server
            }
            is CollectionPayloadTooLargeException -> TooLarge
            is CollectionNotPlaylistException -> NotPlaylist
            is UnknownHostException, is ConnectException, is NoRouteToHostException -> Offline
            is SocketTimeoutException -> Timeout
            is SSLException -> Tls
            is InterruptedIOException -> Timeout
            is java.io.IOException -> Offline
            else -> Unknown
        }
    }
}

data class CollectionSyncPlan(
    val tracks: List<PersonalCollectionTrackEntity>,
    val removedEntryKeys: List<String>,
    val added: Int,
    val updated: Int
)

internal object PersonalCollectionSyncPlanner {
    fun plan(
        collectionId: String,
        existing: List<PersonalCollectionTrackEntity>,
        parsed: List<ParsedCollectionEntry>,
        now: Long
    ): CollectionSyncPlan {
        val previous = existing.associateBy { it.entryKey }
        var added = 0
        var updated = 0
        val next = parsed.mapIndexed { index, entry ->
            val old = previous[entry.entryKey]
            val keepArtwork = old != null && old.artworkUrl == entry.artworkUrl
            val row = PersonalCollectionTrackEntity(
                collectionId = collectionId,
                entryKey = entry.entryKey,
                trackId = personalCollectionTrackId(collectionId, entry.entryKey),
                position = index,
                title = entry.title,
                artist = entry.artist,
                album = entry.album,
                albumArtist = entry.albumArtist,
                durationMs = entry.durationMs,
                audioUrl = entry.audioUrl,
                videoUrl = entry.videoUrl,
                artworkUrl = entry.artworkUrl,
                artworkFile = if (keepArtwork) old.artworkFile.orEmpty() else "",
                trackNumber = entry.trackNumber,
                discNumber = entry.discNumber,
                year = entry.year,
                addedAt = old?.addedAt ?: now
            )
            when {
                old == null -> added++
                old.copy(position = row.position) != row -> updated++
            }
            row
        }
        val nextKeys = next.mapTo(HashSet()) { it.entryKey }
        val removed = existing.map { it.entryKey }.filterNot { it in nextKeys }
        return CollectionSyncPlan(next, removed, added, updated)
    }
}
