package com.luc4n3x.levyra.domain

enum class PlaybackStreamKind {
    AUDIO,
    VIDEO,
    MUXED,
    HLS
}

enum class PlaybackDeliveryMethod {
    PROGRESSIVE,
    HLS,
    DASH,
    UNKNOWN
}

data class PlaybackStreamDescriptor(
    val url: String,
    val kind: PlaybackStreamKind,
    val deliveryMethod: PlaybackDeliveryMethod,
    val container: String = "",
    val mimeType: String = "",
    val codec: String = "",
    val bitrate: Int = 0,
    val averageBitrate: Int = 0,
    val sampleRate: Int = 0,
    val bitDepth: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val fps: Int = 0,
    val itag: Int = -1,
    val qualityLabel: String = "",
    val expiresAtMs: Long = 0L,
    val selected: Boolean = false
) {
    fun isFresh(nowMs: Long = System.currentTimeMillis(), refreshAheadMs: Long = 90_000L): Boolean {
        if (url.isBlank()) return false
        if (expiresAtMs <= 0L) return true
        return nowMs + refreshAheadMs < expiresAtMs
    }

    fun isMp4Audio(): Boolean {
        if (kind != PlaybackStreamKind.AUDIO || deliveryMethod == PlaybackDeliveryMethod.HLS) return false
        val normalizedContainer = container.trim().lowercase()
        val normalizedMimeType = mimeType.substringBefore(';').trim().lowercase()
        val normalizedUrl = url.lowercase()
        val path = normalizedUrl.substringBefore('?').substringBefore('#')
        return normalizedContainer == "m4a" ||
            normalizedContainer == "mp4" ||
            normalizedMimeType == "audio/mp4" ||
            normalizedUrl.contains("mime=audio%2fmp4") ||
            normalizedUrl.contains("mime=audio/mp4") ||
            path.endsWith(".m4a") ||
            path.endsWith(".mp4")
    }
}

data class ResolvedPlaybackManifest(
    val sourceVideoId: String,
    val provider: String,
    val resolvedAtMs: Long,
    val expiresAtMs: Long,
    val durationMs: Long,
    val selectedAudioUrl: String,
    val selectedVideoUrl: String,
    val streams: List<PlaybackStreamDescriptor>,
    val loudnessDb: Float? = null,
    val perceptualLoudnessDb: Float? = null
) {
    val isMuxed: Boolean
        get() = selectedAudioUrl.isNotBlank() && selectedVideoUrl.isBlank() &&
            streams.any { it.selected && it.kind == PlaybackStreamKind.MUXED }

    fun isFresh(nowMs: Long = System.currentTimeMillis(), refreshAheadMs: Long = 90_000L): Boolean {
        if (selectedAudioUrl.isBlank()) return false
        if (expiresAtMs > 0L && nowMs + refreshAheadMs >= expiresAtMs) return false
        val selectedStreams = streams.filter { it.selected }
        return selectedStreams.isNotEmpty() && selectedStreams.all { it.isFresh(nowMs, refreshAheadMs) }
    }

    fun supportsMp4AudioExport(): Boolean {
        if (selectedAudioUrl.isBlank() || selectedVideoUrl.isNotBlank()) return false
        return streams.firstOrNull { descriptor ->
            descriptor.selected && descriptor.url == selectedAudioUrl
        }?.isMp4Audio() == true
    }

    fun compact(maxStreams: Int = 10): ResolvedPlaybackManifest {
        val selected = streams.filter { it.selected }
        val alternatives = streams
            .asSequence()
            .filterNot { it.selected }
            .sortedWith(
                compareByDescending<PlaybackStreamDescriptor> { it.kind == PlaybackStreamKind.AUDIO }
                    .thenByDescending { it.averageBitrate.coerceAtLeast(it.bitrate) }
                    .thenByDescending { it.height }
            )
            .take((maxStreams - selected.size).coerceAtLeast(0))
            .toList()
        return copy(streams = (selected + alternatives).distinctBy { descriptor ->
            listOf(descriptor.kind.name, descriptor.itag.toString(), descriptor.url).joinToString("|")
        })
    }
}
