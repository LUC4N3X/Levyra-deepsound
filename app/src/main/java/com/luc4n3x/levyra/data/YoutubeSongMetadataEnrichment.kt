package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.schabi.newpipe.extractor.stream.SongMetadata

internal fun Track.withYoutubeSongMetadata(metadata: SongMetadata?): Track {
    if (metadata == null) return this

    val extractedReleaseDate = metadata.releaseDate
        ?.offsetDateTime()
        ?.toLocalDate()
        ?.toString()
        .orEmpty()
    val extractedDurationMs = metadata.duration?.toMillis()?.takeIf { it > 0L }

    return copy(
        title = title.ifBlank { metadata.title },
        artist = artist.ifBlank { metadata.artist },
        album = album.ifBlank { metadata.album.orEmpty() },
        durationMs = durationMs.takeIf { it > 0L } ?: extractedDurationMs ?: durationMs,
        releaseDate = releaseDate.ifBlank { extractedReleaseDate },
        year = year.ifBlank { extractedReleaseDate.take(4) },
        trackNumber = trackNumber.takeIf { it > 0 }
            ?: metadata.track.takeIf { it > 0 }
            ?: trackNumber
    )
}
