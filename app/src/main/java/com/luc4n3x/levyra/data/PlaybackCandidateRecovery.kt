package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.PlaybackStreamDescriptor
import com.luc4n3x.levyra.domain.PlaybackStreamKind
import com.luc4n3x.levyra.domain.ResolvedPlaybackManifest

/**
 * Number of streams of one manifest that may be burned by candidate promotion before the resolver
 * has to go back to a fresh player response. It bounds the promotion ladder without extra state.
 */
internal const val MAX_PROMOTED_PLAYBACK_CANDIDATES = 3

/**
 * A failure that describes one media candidate rather than the client, the security state or the
 * resolver. Rotating clients or regenerating tokens for these only multiplies traffic.
 */
internal fun isCandidateLevelPlaybackFailure(kind: PlaybackFailureKind): Boolean = when (kind) {
    PlaybackFailureKind.NotFound,
    PlaybackFailureKind.ServerError,
    PlaybackFailureKind.Truncated -> true
    else -> false
}

/**
 * Re-selects the manifest around the streams that are still usable, keeping the same resolved
 * player response. Returns null when no equivalent candidate survives, so the caller falls back to
 * its normal recovery ladder.
 */
internal fun promoteAlternatePlaybackCandidate(
    manifest: ResolvedPlaybackManifest,
    isVideoMode: Boolean,
    nowMs: Long = System.currentTimeMillis(),
    isBlocked: (String) -> Boolean
): ResolvedPlaybackManifest? {
    val audioFailed = manifest.selectedAudioUrl.isBlank() || isBlocked(manifest.selectedAudioUrl)
    val videoFailed = manifest.selectedVideoUrl.isNotBlank() && isBlocked(manifest.selectedVideoUrl)
    if (!audioFailed && !videoFailed) return null

    val currentAudio = manifest.streams.firstOrNull { it.url == manifest.selectedAudioUrl }
    val currentVideo = manifest.streams.firstOrNull { it.url == manifest.selectedVideoUrl }

    val nextAudio = if (audioFailed) {
        val kinds = if (currentAudio != null) {
            setOf(currentAudio.kind)
        } else if (isVideoMode) {
            setOf(PlaybackStreamKind.MUXED, PlaybackStreamKind.AUDIO)
        } else {
            setOf(PlaybackStreamKind.AUDIO, PlaybackStreamKind.MUXED)
        }
        bestAlternative(manifest, kinds, currentAudio, nowMs, isBlocked) ?: return null
    } else {
        currentAudio
    }

    val nextVideo = if (videoFailed) {
        bestAlternative(manifest, setOf(PlaybackStreamKind.VIDEO), currentVideo, nowMs, isBlocked) ?: return null
    } else {
        currentVideo
    }

    val audioUrl = nextAudio?.url ?: manifest.selectedAudioUrl
    if (audioUrl.isBlank() || isBlocked(audioUrl)) return null
    val videoUrl = nextVideo?.url.orEmpty()
    if (audioUrl == manifest.selectedAudioUrl && videoUrl == manifest.selectedVideoUrl) return null

    val promotedUrls = setOfNotNull(audioUrl, videoUrl.takeIf { it.isNotBlank() })
    return manifest.copy(
        selectedAudioUrl = audioUrl,
        selectedVideoUrl = videoUrl,
        streams = manifest.streams.map { stream -> stream.copy(selected = stream.url in promotedUrls) }
    )
}

private fun bestAlternative(
    manifest: ResolvedPlaybackManifest,
    kinds: Set<PlaybackStreamKind>,
    current: PlaybackStreamDescriptor?,
    nowMs: Long,
    isBlocked: (String) -> Boolean
): PlaybackStreamDescriptor? {
    val targetHeight = current?.height ?: 0
    return manifest.streams
        .asSequence()
        .filter { it.kind in kinds }
        .filter { it.url.isNotBlank() && it.url != current?.url && !isBlocked(it.url) }
        .filter { it.isFresh(nowMs, refreshAheadMs = 0L) }
        .sortedWith(
            compareBy<PlaybackStreamDescriptor> { candidate ->
                if (targetHeight > 0) {
                    // Stay as close as possible to what the user was already watching.
                    kotlin.math.abs(candidate.height - targetHeight)
                } else {
                    0
                }
            }.thenByDescending { it.averageBitrate.coerceAtLeast(it.bitrate) }
        )
        .firstOrNull()
}
