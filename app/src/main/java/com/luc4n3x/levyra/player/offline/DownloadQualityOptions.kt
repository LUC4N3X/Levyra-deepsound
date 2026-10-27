package com.luc4n3x.levyra.player.offline

import com.luc4n3x.levyra.domain.DownloadQualityChoice
import com.luc4n3x.levyra.domain.DownloadQualitySource
import com.luc4n3x.levyra.domain.LevyraAudioQuality
import com.luc4n3x.levyra.domain.LevyraDownloadSettings
import com.luc4n3x.levyra.domain.PlaybackStreamDescriptor
import com.luc4n3x.levyra.domain.PlaybackStreamKind
import com.luc4n3x.levyra.domain.Track
import java.util.Locale
import kotlin.math.abs

internal const val MAX_DOWNLOAD_QUALITY_OPTIONS = 3
private const val HIGH_TIER_MIN_KBPS = 256
private const val STANDARD_TIER_MIN_KBPS = 112
private const val KBPS_TOLERANCE = 0.85
private const val DUPLICATE_KBPS_RATIO = 0.10
private val ContentLengthParameter = Regex("""[?&]clen=(\d+)""")

enum class DownloadQualityTier { HiResLossless, Lossless, High, Standard, Compact }

enum class DownloadQualityPhase { Loading, Ready, Unavailable, Error }

data class DownloadQualityOption(
    val source: DownloadQualitySource,
    val codecLabel: String,
    val kbps: Int = 0,
    val bitDepth: Int = 0,
    val sampleRateHz: Int = 0,
    val estimatedBytes: Long = 0L
) {
    val choice: DownloadQualityChoice
        get() = DownloadQualityChoice(source, kbps, bitDepth, sampleRateHz)

    val lossless: Boolean
        get() = source == DownloadQualitySource.Lossless

    val tier: DownloadQualityTier
        get() = when {
            lossless && choice.hiRes -> DownloadQualityTier.HiResLossless
            lossless -> DownloadQualityTier.Lossless
            kbps >= HIGH_TIER_MIN_KBPS -> DownloadQualityTier.High
            kbps in 1 until STANDARD_TIER_MIN_KBPS -> DownloadQualityTier.Compact
            else -> DownloadQualityTier.Standard
        }

    internal val rankScore: Long
        get() = if (lossless) {
            1_000_000L + bitDepth * 1_000L + sampleRateHz / 1_000L
        } else {
            kbps.coerceAtLeast(0).toLong()
        }
}

data class DownloadQualityProbeSnapshot(
    val options: List<DownloadQualityOption> = emptyList(),
    val pending: Int = 0,
    val failed: Int = 0
) {
    val phase: DownloadQualityPhase
        get() = when {
            options.isNotEmpty() -> DownloadQualityPhase.Ready
            pending > 0 -> DownloadQualityPhase.Loading
            failed > 0 -> DownloadQualityPhase.Error
            else -> DownloadQualityPhase.Unavailable
        }

    val checkingMore: Boolean
        get() = options.isNotEmpty() && pending > 0
}

internal fun rankDownloadQualityOptions(options: List<DownloadQualityOption>): List<DownloadQualityOption> {
    val ranked = options.sortedWith(
        compareByDescending<DownloadQualityOption> { it.rankScore }.thenBy { it.source.ordinal }
    )
    val distinct = ArrayList<DownloadQualityOption>(MAX_DOWNLOAD_QUALITY_OPTIONS)
    for (option in ranked) {
        if (distinct.none { it.duplicates(option) }) distinct += option
        if (distinct.size == MAX_DOWNLOAD_QUALITY_OPTIONS) break
    }
    return distinct
}

private fun DownloadQualityOption.duplicates(other: DownloadQualityOption): Boolean {
    if (lossless != other.lossless || codecLabel != other.codecLabel) return false
    if (lossless) return bitDepth == other.bitDepth && sampleRateHz == other.sampleRateHz
    if (kbps <= 0 || other.kbps <= 0) return kbps == other.kbps
    return abs(kbps - other.kbps) <= maxOf(kbps, other.kbps) * DUPLICATE_KBPS_RATIO
}

internal fun reconcileDownloadQualitySelection(
    options: List<DownloadQualityOption>,
    selected: DownloadQualityChoice?,
    userSelected: Boolean
): DownloadQualityChoice? =
    if (userSelected && options.any { it.choice == selected }) selected else options.firstOrNull()?.choice

internal fun downloadResolverAudioQuality(source: DownloadQualitySource?, settings: LevyraDownloadSettings): String? =
    when (source) {
        DownloadQualitySource.Lossless, DownloadQualitySource.HighBitrate -> LevyraAudioQuality.HIGH
        DownloadQualitySource.Standard, null -> settings.resolverAudioQuality
    }

internal fun Track.downloadQualityOption(source: DownloadQualitySource): DownloadQualityOption? {
    val manifest = playbackManifest ?: return null
    val stream = manifest.streams.firstOrNull { it.selected && it.url == manifest.selectedAudioUrl && it.url == streamUrl }
        ?: return null
    val duration = manifest.durationMs.takeIf { it > 0L } ?: durationMs
    return when (source) {
        DownloadQualitySource.Lossless -> if (!hasVerifiedLosslessSource()) null else DownloadQualityOption(
            source = source,
            codecLabel = "FLAC",
            kbps = stream.averageBitrate / 1_000,
            bitDepth = stream.bitDepth,
            sampleRateHz = stream.sampleRate,
            estimatedBytes = estimatedBytes(stream.averageBitrate, duration)
        )
        DownloadQualitySource.HighBitrate -> if (!hasVerifiedHighBitrateSource()) null else DownloadQualityOption(
            source = source,
            codecLabel = "AAC",
            kbps = stream.bitrate / 1_000,
            sampleRateHz = stream.sampleRate,
            estimatedBytes = estimatedBytes(stream.averageBitrate, duration)
        )
        DownloadQualitySource.Standard -> {
            if (hasVerifiedLosslessSource() || hasVerifiedHighBitrateSource() || !isAllowedOfflineExportSource(this)) return null
            val audioOnly = stream.kind == PlaybackStreamKind.AUDIO
            DownloadQualityOption(
                source = source,
                codecLabel = codecLabel(stream),
                kbps = if (audioOnly) (stream.averageBitrate.takeIf { it > 0 } ?: stream.bitrate) / 1_000 else 0,
                sampleRateHz = if (audioOnly) stream.sampleRate else 0,
                estimatedBytes = if (audioOnly) contentLengthFromUrl(stream.url) else 0L
            )
        }
    }
}

internal fun DownloadQualityChoice.isSatisfiedBy(track: Track): Boolean {
    val option = track.downloadQualityOption(source) ?: return false
    return when (source) {
        DownloadQualitySource.Lossless ->
            option.bitDepth >= bitDepth && option.sampleRateHz >= sampleRateHz
        DownloadQualitySource.HighBitrate, DownloadQualitySource.Standard ->
            kbps <= 0 || option.kbps >= kbps * KBPS_TOLERANCE
    }
}

private fun codecLabel(stream: PlaybackStreamDescriptor): String {
    val codec = stream.codec.lowercase(Locale.ROOT)
    return when {
        "flac" in codec -> "FLAC"
        "mp4a" in codec -> "AAC"
        "opus" in codec -> "Opus"
        else -> stream.container.uppercase(Locale.ROOT).ifBlank { "MP4" }
    }
}

private fun estimatedBytes(bitsPerSecond: Int, durationMs: Long): Long =
    if (bitsPerSecond > 0 && durationMs > 0L) bitsPerSecond.toLong() * durationMs / 8_000L else 0L

private fun contentLengthFromUrl(url: String): Long =
    ContentLengthParameter.find(url)?.groupValues?.getOrNull(1)?.toLongOrNull()?.takeIf { it > 0L } ?: 0L
