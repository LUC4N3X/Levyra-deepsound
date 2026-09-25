package com.luc4n3x.levyra.domain

import java.util.Locale

enum class VideoQualityTarget(val storageValue: String, val height: Int) {
    AUTO("auto", 0),
    P2160("2160p", 2160),
    P1440("1440p", 1440),
    P1080("1080p", 1080),
    P720("720p", 720),
    P480("480p", 480),
    P360("360p", 360);

    companion object {
        fun fromStorage(value: String?): VideoQualityTarget =
            entries.firstOrNull { it.storageValue == value } ?: AUTO

        fun resolveHeight(target: VideoQualityTarget, autoTargetHeight: Int): Int =
            if (target == AUTO) autoTargetHeight else target.height
    }
}

data class VideoQualityRung(
    val label: String,
    val height: Int,
    val width: Int,
    val itag: Int,
    val bitrate: Int,
    val mimeType: String,
    val codec: String,
    val url: String,
    val progressive: Boolean,
    val expiresAtMs: Long = 0L
)

fun Track.withSelectedVideoQuality(rung: VideoQualityRung, audioPartner: String): Track {
    val selectedAudioUrl = if (rung.progressive) rung.url else audioPartner
    val selectedVideoUrl = if (rung.progressive) "" else rung.url
    val selectedUrls = setOf(selectedAudioUrl, selectedVideoUrl).filterTo(HashSet()) { it.isNotBlank() }
    val manifest = playbackManifest
    val updatedManifest = manifest?.copy(
        selectedAudioUrl = selectedAudioUrl,
        selectedVideoUrl = selectedVideoUrl,
        streams = manifest.streams.map { descriptor ->
            descriptor.copy(selected = descriptor.url in selectedUrls)
        }
    )
    return copy(
        streamUrl = selectedAudioUrl,
        videoStreamUrl = selectedVideoUrl,
        playbackManifest = updatedManifest
    )
}

object VideoQualityLadder {
    private val labelPattern = Regex("""^\d+p(\d+)?$""")

    fun codecRank(codec: String, mimeType: String): Int {
        val raw = "${codec} ${mimeType}".lowercase(Locale.ROOT)
        return when {
            raw.contains("avc1") || raw.contains("h264") || raw.contains("avc") -> 0
            raw.contains("vp9") -> 1
            raw.contains("av01") || raw.contains("av1") -> 2
            raw.contains("hevc") || raw.contains("h265") -> 3
            else -> 4
        }
    }

    fun labelOf(descriptor: PlaybackStreamDescriptor): String =
        descriptor.qualityLabel.trim().takeIf(labelPattern::matches)
            ?: descriptor.height.takeIf { it > 0 }?.let { "${it}p" }
            ?: ""

    fun build(
        streams: List<PlaybackStreamDescriptor>,
        decoderSupported: (PlaybackStreamDescriptor) -> Boolean = { true }
    ): List<VideoQualityRung> {
        val byLabel = LinkedHashMap<String, PlaybackStreamDescriptor>()
        fun consider(descriptor: PlaybackStreamDescriptor) {
            val label = labelOf(descriptor)
            if (label.isBlank() || descriptor.url.isBlank()) return
            val current = byLabel[label]
            val take = when {
                current == null -> true
                current.kind == PlaybackStreamKind.MUXED -> false
                descriptor.kind == PlaybackStreamKind.MUXED -> true
                codecRank(descriptor.codec, descriptor.mimeType) != codecRank(current.codec, current.mimeType) ->
                    codecRank(descriptor.codec, descriptor.mimeType) < codecRank(current.codec, current.mimeType)
                else -> descriptor.bitrate > current.bitrate
            }
            if (take) byLabel[label] = descriptor
        }
        streams.asSequence()
            .filter { descriptor ->
                descriptor.kind == PlaybackStreamKind.VIDEO || descriptor.kind == PlaybackStreamKind.MUXED
            }
            .filter(decoderSupported)
            .forEach(::consider)
        return byLabel.values
            .map { descriptor -> descriptor.toRung() }
            .sortedWith(
                compareByDescending<VideoQualityRung> { it.height }
                    .thenByDescending { it.progressive }
                    .thenBy { codecRank(it.codec, it.mimeType) }
                    .thenByDescending { it.bitrate }
            )
    }

    fun selectRung(
        rungs: List<VideoQualityRung>,
        target: VideoQualityTarget,
        autoTargetHeight: Int
    ): VideoQualityRung? {
        if (rungs.isEmpty()) return null
        if (target == VideoQualityTarget.AUTO) {
            return rungs.filter { it.height <= autoTargetHeight }.maxByOrNull { it.height }
                ?: rungs.minByOrNull { it.height }
        }
        val targetHeight = target.height
        rungs.firstOrNull { it.label == target.storageValue }?.let { return it }
        val pool = rungs.filter { it.height <= targetHeight }
        if (pool.isNotEmpty()) {
            return pool.maxWithOrNull(
                compareBy<VideoQualityRung> { it.height }
                    .thenBy { it.label == target.storageValue }
                    .thenBy { it.bitrate }
            )
        }
        return rungs.minByOrNull { it.height }
    }

    fun rungBelow(rungs: List<VideoQualityRung>, currentLabel: String?): VideoQualityRung? {
        if (currentLabel == null) return null
        val index = rungs.indexOfFirst { it.label == currentLabel }
        return if (index >= 0 && index + 1 < rungs.size) rungs[index + 1] else null
    }

    fun activeLabelFor(track: Track, rungs: List<VideoQualityRung>): String? {
        if (rungs.isEmpty()) return null
        val videoUrl = track.videoStreamUrl.trim()
        val streamUrl = track.streamUrl.trim()
        return rungs.firstOrNull { rung ->
            (rung.progressive && rung.url == streamUrl) || (!rung.progressive && rung.url == videoUrl)
        }?.label ?: rungs.firstOrNull { rung -> rung.url == streamUrl || rung.url == videoUrl }?.label
    }

    fun autoTargetHeight(
        lowRam: Boolean,
        powerSave: Boolean,
        unmetered: Boolean,
        fastTransport: Boolean,
        displayShortSidePx: Int,
        hasHardwareAv1: Boolean
    ): Int = when {
        lowRam || powerSave -> 720
        !unmetered || !fastTransport -> 720
        displayShortSidePx >= 1800 && hasHardwareAv1 -> 2160
        displayShortSidePx >= 1200 -> 1440
        displayShortSidePx >= 900 -> 1080
        else -> 720
    }

    private fun PlaybackStreamDescriptor.toRung(): VideoQualityRung = VideoQualityRung(
        label = labelOf(this),
        height = height.coerceAtLeast(0),
        width = width.coerceAtLeast(0),
        itag = itag,
        bitrate = bitrate.coerceAtLeast(0),
        mimeType = mimeType.substringBefore(';').trim(),
        codec = codec,
        url = url,
        progressive = kind == PlaybackStreamKind.MUXED,
        expiresAtMs = expiresAtMs
    )
}

enum class VideoStallKind {
    MID_PLAY,
    SEEK,
    QUALITY_SWITCH,
    PREPARE
}

class VideoRebufferPolicy(
    private val requiredStalls: Int = 2,
    private val windowMs: Long = 45_000L
) {
    private val midPlayStallsMs = ArrayDeque<Long>()

    fun onMidPlayStall(nowMs: Long) {
        midPlayStallsMs.addLast(nowMs)
    }

    fun shouldDowngrade(nowMs: Long): Boolean {
        while (midPlayStallsMs.isNotEmpty() && nowMs - midPlayStallsMs.first() > windowMs) {
            midPlayStallsMs.removeFirst()
        }
        return midPlayStallsMs.size >= requiredStalls
    }

    fun reset() {
        midPlayStallsMs.clear()
    }
}

fun classifyVideoStall(
    nowMs: Long,
    reachedReady: Boolean,
    lastSeekAtMs: Long?,
    lastQualitySwitchAtMs: Long?,
    seekGraceMs: Long = VIDEO_SEEK_GRACE_MS,
    qualitySwitchGraceMs: Long = VIDEO_QUALITY_SWITCH_GRACE_MS
): VideoStallKind {
    if (!reachedReady) return VideoStallKind.PREPARE
    lastSeekAtMs?.takeIf { nowMs - it <= seekGraceMs }?.let { return VideoStallKind.SEEK }
    lastQualitySwitchAtMs?.takeIf { nowMs - it <= qualitySwitchGraceMs }?.let { return VideoStallKind.QUALITY_SWITCH }
    return VideoStallKind.MID_PLAY
}

const val VIDEO_SEEK_GRACE_MS = 1_500L
const val VIDEO_QUALITY_SWITCH_GRACE_MS = 2_000L

fun audioPartnerForAdaptiveRung(manifest: ResolvedPlaybackManifest?, fallbackUrl: String): String {
    if (manifest == null) return fallbackUrl
    val nowMs = System.currentTimeMillis()
    return manifest.streams.asSequence()
        .filter { it.kind == PlaybackStreamKind.AUDIO && it.url.isNotBlank() }
        .filter { it.isFresh(nowMs) || it.url == manifest.selectedAudioUrl }
        .maxWithOrNull(
            compareBy<PlaybackStreamDescriptor> { it.url == manifest.selectedAudioUrl }
                .thenBy { it.averageBitrate.coerceAtLeast(it.bitrate) }
        )
        ?.url
        ?: manifest.selectedAudioUrl.takeIf { !manifest.isMuxed && it.isNotBlank() }
        ?: fallbackUrl
}
