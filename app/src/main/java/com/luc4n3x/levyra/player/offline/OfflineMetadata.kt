package com.luc4n3x.levyra.player.offline

import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.player.offline.tagging.LevyraM4aMetadata
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

internal fun mergeOfflineMetadataTrack(original: Track, resolved: Track): Track {
    val originalProviderWins = original.metadataProvider.isNotBlank() &&
        (resolved.metadataProvider.isBlank() || original.metadataConfidence >= resolved.metadataConfidence)
    val metadataProvider = if (originalProviderWins) {
        original.metadataProvider
    } else {
        resolved.metadataProvider.ifBlank { original.metadataProvider }
    }
    val metadataConfidence = if (originalProviderWins) original.metadataConfidence else resolved.metadataConfidence
    return resolved.copy(
        id = original.id.ifBlank { resolved.id },
        title = original.title.ifBlank { resolved.title },
        artist = original.artist.ifBlank { resolved.artist },
        album = original.album.ifBlank { resolved.album },
        durationMs = original.durationMs.takeIf { it > 0L } ?: resolved.durationMs,
        videoUrl = original.videoUrl.ifBlank { resolved.videoUrl },
        thumbnailUrl = original.thumbnailUrl.ifBlank { resolved.thumbnailUrl },
        largeThumbnailUrl = original.largeThumbnailUrl.ifBlank { resolved.largeThumbnailUrl },
        source = original.source.ifBlank { resolved.source },
        moodTags = (original.moodTags + resolved.moodTags).filter { it.isNotBlank() }.toSet(),
        isrc = original.isrc.ifBlank { resolved.isrc },
        upc = original.upc.ifBlank { resolved.upc },
        releaseDate = original.releaseDate.ifBlank { resolved.releaseDate },
        year = original.year.ifBlank { resolved.year },
        trackNumber = original.trackNumber.takeIf { it > 0 } ?: resolved.trackNumber,
        discNumber = original.discNumber.takeIf { it > 0 } ?: resolved.discNumber,
        explicit = original.explicit || resolved.explicit,
        albumBrowseId = original.albumBrowseId.ifBlank { resolved.albumBrowseId },
        artistBrowseIds = original.artistBrowseIds.ifEmpty { resolved.artistBrowseIds },
        counterpartVideoId = original.counterpartVideoId.ifBlank { resolved.counterpartVideoId },
        videoType = original.videoType.ifBlank { resolved.videoType },
        metadataProvider = metadataProvider,
        metadataConfidence = metadataConfidence.coerceIn(0, 100),
        canonicalAlbumUrl = original.canonicalAlbumUrl.ifBlank { resolved.canonicalAlbumUrl },
        youtubeLikeCount = original.youtubeLikeCount.takeIf { it >= 0L } ?: resolved.youtubeLikeCount,
        youtubeViewCount = original.youtubeViewCount.takeIf { it >= 0L } ?: resolved.youtubeViewCount
    )
}

internal fun Track.toRichM4aMetadata(artwork: ByteArray?, lyrics: String): LevyraM4aMetadata = LevyraM4aMetadata(
    title = title,
    artist = artist,
    album = album.ifBlank { "Levyra" },
    albumArtist = artist,
    year = year,
    releaseDate = releaseDate.ifBlank { year },
    genres = offlineGenres(),
    trackNumber = trackNumber,
    discNumber = discNumber,
    lyrics = lyrics,
    explicit = explicit,
    isrc = isrc,
    upc = upc,
    sourceUrl = offlineSourceUrl(),
    sourceProvider = source,
    metadataProvider = metadataProvider,
    metadataConfidence = metadataConfidence,
    trackId = id,
    albumId = albumBrowseId,
    artistIds = artistBrowseIds,
    albumUrl = canonicalAlbumUrl,
    counterpartId = counterpartVideoId,
    mediaType = videoType,
    artworkData = artwork
)

internal fun Track.offlineGenres(): List<String> {
    val ignored = setOf("music", "shared", "youtube", "offline", "download", "hit", "video")
    val seen = HashSet<String>()
    return moodTags.asSequence()
        .map { it.trim().replace('_', ' ').replace(Regex("\\s+"), " ") }
        .filter { it.length >= 2 }
        .filterNot { it.lowercase(Locale.ROOT) in ignored }
        .filter { seen.add(it.lowercase(Locale.ROOT)) }
        .take(8)
        .toList()
}

internal fun Track.offlineSourceUrl(): String = videoUrl
    .ifBlank { canonicalAlbumUrl }
    .ifBlank { id.takeIf { it.isNotBlank() }?.let { "https://www.youtube.com/watch?v=$it" }.orEmpty() }

internal fun cachedLyricsText(payload: String): String {
    if (payload.isBlank()) return ""
    return runCatching {
        val lines = JSONObject(payload).optJSONArray("lines") ?: JSONArray()
        buildString {
            for (index in 0 until lines.length()) {
                val line = lines.optJSONObject(index) ?: continue
                if (line.optBoolean("metadata") || line.optBoolean("instrumental")) continue
                val text = line.optString("text").trim()
                if (text.isBlank()) continue
                if (isNotEmpty()) append('\n')
                append(text)
            }
        }.trim()
    }.getOrDefault("")
}
