package com.luc4n3x.levyra.data.hqaudio

import com.luc4n3x.levyra.data.PlaybackSourceIdentity
import com.luc4n3x.levyra.data.runCatchingPreservingCancellation
import com.luc4n3x.levyra.domain.AlternativeAudioSource
import com.luc4n3x.levyra.domain.HighQualityAudioMode
import com.luc4n3x.levyra.domain.PlaybackDeliveryMethod
import com.luc4n3x.levyra.domain.PlaybackStreamDescriptor
import com.luc4n3x.levyra.domain.PlaybackStreamKind
import com.luc4n3x.levyra.domain.PlaybackStreamProvenance
import com.luc4n3x.levyra.domain.ResolvedPlaybackManifest
import com.luc4n3x.levyra.domain.Track

class HighQualityPlaybackCoordinator(
    private val resolver: HighQualityAudioResolver,
    private val clock: () -> Long = System::currentTimeMillis
) {
    var mode: HighQualityAudioMode
        get() = resolver.mode
        set(value) {
            resolver.mode = value
        }

    fun queryFor(track: Track, isVideoMode: Boolean, audioQuality: String): AlternativeTrackQuery? {
        if (!resolver.mode.enabled || isVideoMode) return null
        if (audioQuality.equals(DATA_SAVER_AUDIO_QUALITY, ignoreCase = true)) return null
        if (isLocalTrack(track)) return null
        if (track.title.isBlank() || track.artist.isBlank() || track.durationMs <= 0L) return null
        return AlternativeTrackQuery(
            title = track.title,
            artist = track.artist,
            album = track.album,
            durationMs = track.durationMs,
            explicit = if (track.explicit) true else null,
            isrc = track.isrc
        )
    }

    suspend fun resolve(
        track: Track,
        query: AlternativeTrackQuery,
        provenance: () -> PlaybackStreamProvenance,
        resolveNormal: suspend () -> Track
    ): Track {
        val startedAt = clock()
        val pending = resolver.begin(identityKey(track), query)
        val normal = runCatchingPreservingCancellation { resolveNormal() }
        val normalTrack = normal.getOrNull()
        if (normalTrack?.playbackManifest?.alternativeSource != null) return normalTrack
        val waitMs = waitBudgetMs(resolver.mode, clock() - startedAt, normalTrack != null)
        when (val resolution = resolver.await(pending, waitMs)) {
            is HighQualityResolution.Selected -> {
                val normalKbps = normalTrack?.let(::normalAudioKbps)
                if (HighQualityTierPolicy.accepts(resolution.stream.tier, normalKbps, normalTrack != null)) {
                    HighQualityAudioDiagnostics.selected(resolution.stream, resolution.evaluation, clock() - startedAt)
                    return applyStream(track, normalTrack, resolution, provenance())
                }
                HighQualityAudioDiagnostics.fallback(
                    HighQualityFallbackReason.QUALITY_NOT_HIGHER,
                    "alternative=${resolution.stream.tier.kbps}kbps normal=${normalKbps ?: "unknown"}kbps",
                    track.title
                )
            }
            is HighQualityResolution.Fallback ->
                HighQualityAudioDiagnostics.fallback(resolution.reason, resolution.detail, track.title)
        }
        return normal.getOrThrow()
    }

    fun cachedUpgrade(
        track: Track,
        normalCached: Track?,
        isVideoMode: Boolean,
        audioQuality: String,
        provenance: () -> PlaybackStreamProvenance
    ): Track? {
        if (normalCached?.playbackManifest?.alternativeSource != null) return null
        queryFor(track, isVideoMode, audioQuality) ?: return null
        val selection = resolver.cachedSelection(identityKey(track)) ?: return null
        val normalKbps = normalCached?.let(::normalAudioKbps)
        if (!HighQualityTierPolicy.accepts(selection.stream.tier, normalKbps, normalCached != null)) return null
        return applyStream(track, normalCached, selection, provenance())
    }

    fun handlesFailure(track: Track): Boolean = track.playbackManifest?.alternativeSource != null

    fun reportFailure(track: Track, reason: String) {
        val source = track.playbackManifest?.alternativeSource ?: return
        resolver.reportPlaybackFailure(identityKey(track), source.providerTrackId, reason)
    }

    internal fun applyStream(
        requested: Track,
        normal: Track?,
        selection: HighQualityResolution.Selected,
        provenance: PlaybackStreamProvenance
    ): Track {
        val now = clock()
        val stream = selection.stream
        val base = normal ?: requested
        val durationMs = requested.durationMs.takeIf { it > 0L } ?: base.durationMs
        val label = "$SOURCE_LABEL · ${resolver.providerName}"
        val descriptor = PlaybackStreamDescriptor(
            url = stream.url,
            kind = PlaybackStreamKind.AUDIO,
            deliveryMethod = PlaybackDeliveryMethod.PROGRESSIVE,
            container = stream.container,
            mimeType = stream.mimeType,
            codec = stream.codec,
            bitrate = stream.tier.kbps * 1_000,
            averageBitrate = stream.estimatedKbps * 1_000,
            qualityLabel = "${stream.tier.kbps} kbps",
            expiresAtMs = stream.expiresAtMs,
            selected = true
        )
        val manifest = ResolvedPlaybackManifest(
            sourceVideoId = PlaybackSourceIdentity.sourceVideoId(requested),
            provider = label,
            resolvedAtMs = now,
            expiresAtMs = stream.expiresAtMs,
            durationMs = durationMs,
            selectedAudioUrl = stream.url,
            selectedVideoUrl = "",
            streams = listOf(descriptor),
            provenance = provenance.copy(
                clientName = stream.providerId,
                resolvedAtMs = now,
                expiresAtMs = stream.expiresAtMs
            ),
            alternativeSource = AlternativeAudioSource(
                providerId = stream.providerId,
                providerTrackId = stream.providerTrackId,
                bitrateKbps = stream.tier.kbps,
                verdict = selection.evaluation.verdict,
                confidence = selection.evaluation.confidence
            )
        )
        return base.copy(
            durationMs = durationMs,
            streamUrl = stream.url,
            videoStreamUrl = "",
            videoSubtitleTracks = emptyList(),
            source = "$label ${stream.tier.kbps} kbps",
            youtubeLoudnessDb = null,
            youtubePerceptualLoudnessDb = null,
            playbackManifest = manifest
        )
    }

    internal fun normalAudioKbps(track: Track): Int? {
        val manifest = track.playbackManifest ?: return null
        val selected = manifest.streams.firstOrNull { it.selected && it.url == manifest.selectedAudioUrl }
            ?: manifest.streams.firstOrNull { it.selected }
            ?: return null
        if (selected.kind != PlaybackStreamKind.AUDIO) return null
        val bitsPerSecond = selected.averageBitrate.takeIf { it > 0 } ?: selected.bitrate
        return (bitsPerSecond / 1_000).takeIf { it > 0 }
    }

    private fun waitBudgetMs(mode: HighQualityAudioMode, normalElapsedMs: Long, normalAvailable: Boolean): Long {
        if (!normalAvailable) return (PREFER_320_WAIT_MS - normalElapsedMs).coerceAtLeast(0L)
        return when (mode) {
            HighQualityAudioMode.PREFER_320 -> (PREFER_320_WAIT_MS - normalElapsedMs).coerceAtLeast(0L)
            HighQualityAudioMode.AUTOMATIC ->
                minOf(AUTOMATIC_WAIT_MS - normalElapsedMs, AUTOMATIC_GRACE_MS).coerceAtLeast(0L)
            HighQualityAudioMode.OFF -> 0L
        }
    }

    private fun identityKey(track: Track): String = PlaybackSourceIdentity.canonicalKey(track)

    private fun isLocalTrack(track: Track): Boolean {
        val stream = track.streamUrl.trim()
        return track.source.equals(OFFLINE_SOURCE, ignoreCase = true) ||
            stream.startsWith("content://", ignoreCase = true) ||
            stream.startsWith("file://", ignoreCase = true)
    }

    companion object {
        const val AUTOMATIC_WAIT_MS = 2_500L
        const val AUTOMATIC_GRACE_MS = 1_200L
        const val PREFER_320_WAIT_MS = 6_000L
        const val SOURCE_LABEL = "Levyra HQ"
        private const val DATA_SAVER_AUDIO_QUALITY = "Low"
        private const val OFFLINE_SOURCE = "Offline"
    }
}
