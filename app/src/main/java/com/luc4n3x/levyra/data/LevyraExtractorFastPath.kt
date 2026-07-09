package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.schabi.newpipe.extractor.levyra.LevyraResolveRequest
import org.schabi.newpipe.extractor.levyra.LevyraYoutubeResolver

internal data class LevyraExtractorPreflight(
    val audioItag: Int,
    val videoItag: Int,
    val videoHeight: Int,
    val source: String
)

internal object LevyraExtractorFastPath {
    fun preflight(
        track: Track,
        isVideoMode: Boolean,
        preferMp4Audio: Boolean,
        resolver: LevyraYoutubeResolver = LevyraYoutubeResolver()
    ): LevyraExtractorPreflight? {
        val videoId = extractVideoId(track.videoUrl).ifBlank { extractVideoId(track.id) }
        if (videoId.isBlank()) return null

        val request = LevyraResolveRequest.forVideoId(videoId)
            .setVideoMode(isVideoMode)
            .setPreferMp4Audio(preferMp4Audio)
            .setRequireStreamingDownloader(true)
            .setMaxVideoHeight(1080)
            .build()
        val resolved = resolver.resolveSabrPreflight(request)
        if (!resolved.isResolved) return null

        val videoItag = if (isVideoMode) resolved.videoItag else -1
        val videoHeight = if (isVideoMode) resolved.videoHeight else -1
        val label = buildString {
            append("LevyraExtractor SABR preflight")
            if (videoHeight > 0) append(" ").append(videoHeight).append("p")
            if (resolved.diagnostics.isCacheHit) append(" cached")
            if (resolved.diagnostics.isInFlightJoin) append(" joined")
        }
        return LevyraExtractorPreflight(
            audioItag = resolved.audioItag,
            videoItag = videoItag,
            videoHeight = videoHeight,
            source = label
        )
    }

    private fun extractVideoId(value: String): String {
        if (value.isBlank()) return ""
        Regex("(?:v=|/shorts/|/embed/|youtu\\.be/)([A-Za-z0-9_-]{11})")
            .find(value)
            ?.groupValues
            ?.getOrNull(1)
            ?.let { return it }
        return value.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }.orEmpty()
    }
}
