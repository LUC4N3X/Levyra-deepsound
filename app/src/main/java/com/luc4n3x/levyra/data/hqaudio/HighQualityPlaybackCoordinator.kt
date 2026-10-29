package com.luc4n3x.levyra.data.hqaudio

import com.luc4n3x.levyra.data.MusicVideoTitle
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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.selects.select

class HighQualityPlaybackCoordinator(
    private val resolver: HighQualityAudioResolver,
    private val clock: () -> Long = System::currentTimeMillis
) {
    var qobuzEnabled: Boolean
        get() = resolver.qobuzEnabled
        set(value) { resolver.qobuzEnabled = value }

    var mode: HighQualityAudioMode
        get() = resolver.mode
        set(value) {
            resolver.mode = value
        }

    fun queryFor(track: Track, isVideoMode: Boolean, audioQuality: String): AlternativeTrackQuery? {
        if (!resolver.mode.enabled || isVideoMode) return null
        if (hasPreferredAlternative(track)) return null
        if (audioQuality.equals(DATA_SAVER_AUDIO_QUALITY, ignoreCase = true)) return null
        if (isLocalTrack(track)) return null
        if (track.title.isBlank() || track.artist.isBlank()) return null
        val recording = MusicVideoTitle.recordingFor(track)
        return AlternativeTrackQuery(
            title = recording.title,
            artist = recording.artist,
            album = track.album,
            durationMs = track.durationMs.coerceAtLeast(0L),
            explicit = if (track.explicit) true else null,
            isrc = track.isrc,
            alternateReading = recording.alternate?.let { alternate ->
                AlternativeTrackQuery(
                    title = alternate.title,
                    artist = alternate.artist,
                    album = track.album,
                    durationMs = track.durationMs.coerceAtLeast(0L),
                    explicit = if (track.explicit) true else null,
                    isrc = track.isrc
                )
            }
        )
    }

    suspend fun resolve(
        track: Track,
        query: AlternativeTrackQuery,
        provenance: () -> PlaybackStreamProvenance,
        resolveNormal: suspend () -> Track
    ): Track {
        val startedAt = clock()
        val background = currentCoroutineContext()[HighQualityBackgroundLookup] != null
        val upfront = query.takeIf { it.durationMs > 0L }?.let { resolver.begin(identityKey(track), it, background) }
        val initialResolution = upfront?.let { resolver.await(it, minOf(AUTOMATIC_GRACE_MS, waitBudgetMs(resolver.mode, 0L, true))) }
        if (initialResolution is HighQualityResolution.Selected &&
            (initialResolution.stream.tier == AudioQualityTier.KBPS_320 || initialResolution.stream.tier.lossless) &&
            acceptsStream(initialResolution.stream, null, true)
        ) {
            HighQualityAudioDiagnostics.selected(initialResolution.stream, initialResolution.evaluation, clock() - startedAt)
            return applyStream(track, null, initialResolution, provenance())
        }
        val normal = if (resolver.mode.lossless && upfront != null) {
            resolveNormalOrReadyAlternative(track, upfront, startedAt, provenance, resolveNormal)
        } else {
            runCatchingPreservingCancellation { resolveNormal() }
        }
        val normalTrack = normal.getOrNull()
        if (normalTrack != null && hasPreferredAlternative(normalTrack)) return normalTrack
        val pending = upfront
            ?: normalTrack?.durationMs?.takeIf { it > 0L }?.let { verified ->
                resolver.begin(
                    identityKey(track),
                    query.copy(durationMs = verified, alternateReading = query.alternateReading?.copy(durationMs = verified)),
                    background
                )
            }
            ?: return normal.getOrThrow()
        val waitMs = waitBudgetMs(resolver.mode, clock() - startedAt, normalTrack != null)
        when (val resolution = resolver.await(pending, waitMs)) {
            is HighQualityResolution.Selected -> {
                val normalKbps = normalTrack?.let(::normalAudioKbps)
                if (acceptsStream(resolution.stream, normalKbps, normalTrack != null)) {
                    HighQualityAudioDiagnostics.selected(resolution.stream, resolution.evaluation, clock() - startedAt)
                    return applyStream(track, normalTrack, resolution, provenance())
                }
                HighQualityAudioDiagnostics.fallback(
                    HighQualityFallbackReason.QUALITY_NOT_HIGHER,
                    "alternative=${resolution.stream.deliveredKbps}kbps normal=${normalKbps ?: "unknown"}kbps",
                    track.title
                )
            }
            is HighQualityResolution.Fallback ->
                HighQualityAudioDiagnostics.fallback(resolution.reason, resolution.detail, track.title)
        }
        return normal.getOrThrow()
    }

    private suspend fun resolveNormalOrReadyAlternative(
        track: Track,
        pending: Deferred<HighQualityResolution>,
        startedAt: Long,
        provenance: () -> PlaybackStreamProvenance,
        resolveNormal: suspend () -> Track
    ): Result<Track> = coroutineScope {
        val normal = async { runCatchingPreservingCancellation { resolveNormal() } }
        val alternative = async { resolver.await(pending, HighQualityAudioResolver.LOOKUP_BUDGET_MS) }
        try {
            select {
                normal.onAwait { it }
                alternative.onAwait { resolution ->
                    if (resolver.mode.enabled && resolution is HighQualityResolution.Selected &&
                        (resolution.stream.tier.lossless || resolution.stream.tier == AudioQualityTier.KBPS_320) &&
                        acceptsStream(resolution.stream, null, true)
                    ) {
                        HighQualityAudioDiagnostics.selected(resolution.stream, resolution.evaluation, clock() - startedAt)
                        Result.success(applyStream(track, null, resolution, provenance()))
                    } else {
                        normal.await()
                    }
                }
            }
        } finally {
            normal.cancel()
            alternative.cancel()
        }
    }

    fun cachedUpgrade(
        track: Track,
        normalCached: Track?,
        isVideoMode: Boolean,
        audioQuality: String,
        provenance: () -> PlaybackStreamProvenance
    ): Track? {
        if (hasPreferredAlternative(normalCached)) return null
        queryFor(track, isVideoMode, audioQuality) ?: return null
        val selection = resolver.cachedSelection(identityKey(track)) ?: return null
        val normalKbps = normalCached?.let(::normalAudioKbps)
        if (!acceptsStream(selection.stream, normalKbps, normalCached != null)) return null
        return applyStream(track, normalCached, selection, provenance())
    }

    /** A cached normal stream must not start playback while a higher-quality lookup can still succeed. */
    fun awaitsUpgrade(track: Track, normalCached: Track?, isVideoMode: Boolean, audioQuality: String): Boolean {
        if (hasPreferredAlternative(normalCached)) return false
        queryFor(track, isVideoMode, audioQuality) ?: return false
        return resolver.upgradePending(identityKey(track))
    }

    fun providerHealth(): List<ProviderBackendHealth> = resolver.providerHealth()

    suspend fun resolveLosslessDownload(
        track: Track,
        audioQuality: String,
        provenance: () -> PlaybackStreamProvenance
    ): Track? {
        val query = queryFor(downloadQueryTrack(track), isVideoMode = false, audioQuality = audioQuality) ?: return null
        if (!resolver.mode.lossless || query.durationMs <= 0L) return null
        val result = resolver.await(resolver.begin(identityKey(track), query), DOWNLOAD_LOOKUP_BUDGET_MS)
        if (result !is HighQualityResolution.Selected || !result.stream.tier.lossless || !resolver.mode.lossless) return null
        return applyStream(track, null, result, provenance())
    }

    suspend fun resolveLossyDownload(
        track: Track,
        audioQuality: String,
        provenance: () -> PlaybackStreamProvenance
    ): Track? {
        val query = queryFor(downloadQueryTrack(track), isVideoMode = false, audioQuality = audioQuality) ?: return null
        if (query.durationMs <= 0L) return null
        val result = resolver.resolveLossy(identityKey(track), query)
        if (result !is HighQualityResolution.Selected || result.stream.tier.lossless) return null
        if (result.stream.codec != MP4_AAC_CODEC || result.stream.mimeType != MP4_AUDIO_MIME_TYPE) return null
        return applyStream(track, null, result, provenance())
    }

    private fun downloadQueryTrack(track: Track): Track = track.copy(
        streamUrl = if (track.playbackManifest?.alternativeSource?.providerId == "tidal") "" else track.streamUrl,
        playbackManifest = null
    )

    private fun hasPreferredAlternative(track: Track?): Boolean {
        val manifest = track?.playbackManifest ?: return false
        if (manifest.alternativeSource == null) return false
        if (manifest.alternativeSource.providerId == "qobuz" && !resolver.qobuzEnabled) return false
        return !resolver.mode.lossless || manifest.streams.any {
            it.selected && it.url == manifest.selectedAudioUrl &&
                (it.mimeType == ProviderStreamValidator.FLAC_MIME_TYPE ||
                    (it.codec == "flac" && it.deliveryMethod == PlaybackDeliveryMethod.DASH))
        }
    }

    private fun acceptsStream(stream: ResolvedHighQualityStream, normalKbps: Int?, normalAvailable: Boolean): Boolean =
        resolver.mode.enabled && if (stream.tier.lossless) resolver.mode.lossless else
            HighQualityTierPolicy.accepts(stream.deliveredKbps, normalKbps, normalAvailable)

    fun handlesFailure(track: Track): Boolean = track.playbackManifest?.alternativeSource != null

    fun reportFailure(track: Track, reason: String) {
        val source = track.playbackManifest?.alternativeSource ?: return
        resolver.reportPlaybackFailure(identityKey(track), source.providerTrackId, reason, source.providerId)
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
        val label = "$SOURCE_LABEL · ${resolver.providerName(stream.providerId)}"
        val descriptor = PlaybackStreamDescriptor(
            url = stream.url,
            kind = PlaybackStreamKind.AUDIO,
            deliveryMethod = stream.deliveryMethod,
            container = stream.container,
            mimeType = stream.mimeType,
            codec = stream.codec,
            bitrate = stream.displayKbps * 1_000,
            averageBitrate = stream.deliveredKbps * 1_000,
            sampleRate = stream.sampleRateHz,
            bitDepth = stream.bitDepth,
            qualityLabel = stream.qualityLabel,
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
                bitrateKbps = stream.displayKbps,
                verdict = selection.evaluation.verdict,
                confidence = selection.evaluation.confidence
            )
        )
        return base.copy(
            durationMs = durationMs,
            streamUrl = stream.url,
            videoStreamUrl = "",
            videoSubtitleTracks = emptyList(),
            source = "$label ${stream.qualityLabel}",
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
            HighQualityAudioMode.PREFER_320,
            HighQualityAudioMode.PREFER_LOSSLESS -> (PREFER_320_WAIT_MS - normalElapsedMs).coerceAtLeast(0L)
            HighQualityAudioMode.AUTOMATIC ->
                minOf(AUTOMATIC_WAIT_MS - normalElapsedMs, AUTOMATIC_GRACE_MS).coerceAtLeast(0L)
            HighQualityAudioMode.OFF -> 0L
        }
    }

    private fun identityKey(track: Track): String = PlaybackSourceIdentity.canonicalKey(track)

    private fun isLocalTrack(track: Track): Boolean {
        val stream = track.streamUrl.trim()
        return track.source.equals(OFFLINE_SOURCE, ignoreCase = true) ||
            track.id.startsWith(com.luc4n3x.levyra.data.collections.PERSONAL_COLLECTION_TRACK_PREFIX) ||
            stream.startsWith("content://", ignoreCase = true) ||
            stream.startsWith("file://", ignoreCase = true)
    }

    companion object {
        const val DOWNLOAD_LOOKUP_BUDGET_MS = 35_000L
        const val DOWNLOAD_LANE_BUDGET_MS = 30_000L
        const val AUTOMATIC_WAIT_MS = 2_500L
        const val AUTOMATIC_GRACE_MS = 1_200L
        const val PREFER_320_WAIT_MS = 8_500L
        const val SOURCE_LABEL = "Levyra HQ"
        private const val DATA_SAVER_AUDIO_QUALITY = "Low"
        private const val MP4_AAC_CODEC = "mp4a"
        private const val MP4_AUDIO_MIME_TYPE = "audio/mp4"
        private const val OFFLINE_SOURCE = "Offline"
    }
}
