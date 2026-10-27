package com.luc4n3x.levyra.player.offline

import com.luc4n3x.levyra.domain.Track
import java.util.Locale

internal fun Track.hasVerifiedLosslessSource(): Boolean {
    val manifest = playbackManifest ?: return false
    if (manifest.alternativeSource?.providerId !in setOf("monochrome", "arcod")) return false
    return manifest.streams.any {
        it.selected && it.url == streamUrl && it.url == manifest.selectedAudioUrl &&
            it.codec == "flac" && it.mimeType == "audio/flac" && it.container == "flac"
    }
}

internal fun allowsVerifiedFlacDownload(track: Track, contentType: String): Boolean {
    if (!track.hasVerifiedLosslessSource()) return false
    return contentType.substringBefore(';').trim().lowercase(Locale.ROOT) in
        setOf("audio/flac", "audio/x-flac", "application/octet-stream", "binary/octet-stream")
}

internal fun Track.hasFreshVerifiedLosslessSource(nowMs: Long = System.currentTimeMillis()): Boolean =
    hasVerifiedLosslessSource() && playbackManifest!!.streams.any {
        it.selected && it.url == streamUrl && it.expiresAtMs > 0L && it.isFresh(nowMs)
    }

internal fun isAllowedOfflineExportSource(track: Track): Boolean =
    track.hasVerifiedLosslessSource() || isAllowedOfflineReelSource(
        track.source, track.playbackManifest?.provider.orEmpty(), track.streamUrl
    )
