package com.luc4n3x.levyra.domain

import java.util.Locale

private val YOUTUBE_VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")
private val YOUTUBE_VIDEO_URL = Regex("(?:v=|/shorts/|/embed/|/live/|youtu\\.be/)([A-Za-z0-9_-]{11})")

/**
 * Shared reading of the YouTube Music `musicVideoType` metadata so playback identity and
 * video-candidate selection classify the same string in the same way.
 */
internal object YoutubeMusicVideoType {
    fun isArtTrack(videoType: String): Boolean = videoType.uppercase(Locale.ROOT).contains("ATV")

    fun isOfficialVideo(videoType: String): Boolean {
        if (isArtTrack(videoType)) return false
        val type = videoType.uppercase(Locale.ROOT)
        return type.contains("OMV") ||
            type.contains("OFFICIAL_MUSIC_VIDEO") ||
            type.contains("OFFICIAL_SOURCE_MUSIC")
    }

    fun isVideo(videoType: String): Boolean {
        if (isOfficialVideo(videoType)) return true
        if (isArtTrack(videoType)) return false
        return videoType.uppercase(Locale.ROOT).contains("UGC")
    }
}

private val COMPACT_COUNT_LEADING_NUMBER = Regex("""^[0-9][0-9.,  ]*""")
private val COMPACT_COUNT_DURATION = Regex("""^[0-9]+(:[0-9]{2})+$""")

private val COMPACT_COUNT_MULTIPLIERS = mapOf(
    "k" to 1_000L, "tys" to 1_000L, "tis" to 1_000L, "mil" to 1_000L, "тыс" to 1_000L, "тис" to 1_000L,
    "m" to 1_000_000L, "mi" to 1_000_000L, "mln" to 1_000_000L, "mio" to 1_000_000L, "млн" to 1_000_000L,
    "b" to 1_000_000_000L, "bn" to 1_000_000_000L, "mld" to 1_000_000_000L, "mrd" to 1_000_000_000L,
    "млрд" to 1_000_000_000L,
    "万" to 10_000L, "萬" to 10_000L, "만" to 10_000L,
    "億" to 100_000_000L, "亿" to 100_000_000L, "억" to 100_000_000L
)

internal fun parseCompactViewCount(label: String): Long {
    val text = label.trim()
    if (text.isEmpty() || COMPACT_COUNT_DURATION.matches(text)) return -1L
    if (text.none(Char::isLetter)) return -1L
    val numberPart = COMPACT_COUNT_LEADING_NUMBER.find(text)?.value?.trim() ?: return -1L

    val suffix = text.removePrefix(numberPart).trimStart()
    val magnitude = suffix.takeWhile { !it.isWhitespace() }
        .trimEnd('.')
        .lowercase(Locale.ROOT)
    val multiplier = COMPACT_COUNT_MULTIPLIERS[magnitude]

    return if (multiplier == null) {
        numberPart.filter(Char::isDigit).toLongOrNull() ?: -1L
    } else {
        val scalar = numberPart.replace(',', '.').filterNot(Char::isWhitespace)
        val value = scalar.toDoubleOrNull() ?: return -1L
        (value * multiplier).toLong()
    }
}

internal const val VIDEO_VIEW_COUNT_STEP = 500

internal fun videoViewCountBonus(views: Long): Int {
    if (views <= 0L) return 0
    var digits = 0
    var remaining = views
    while (remaining > 0L) {
        digits += 1
        remaining /= 10L
    }
    return digits.coerceAtMost(10) * VIDEO_VIEW_COUNT_STEP
}

internal fun Track.hasVerifiedYoutubeMusicVideoPairing(): Boolean {
    val audio = audioVideoId.trim()
    val video = counterpartVideoId.trim()
    if (!YOUTUBE_VIDEO_ID.matches(audio) || !YOUTUBE_VIDEO_ID.matches(video) || audio == video) return true

    val selectedVideo = YOUTUBE_VIDEO_URL
        .find(videoUrl.trim())
        ?.groupValues
        ?.getOrNull(1)
        .orEmpty()
    return selectedVideo == video
}

internal fun Track.hasVideoPlaybackPayload(): Boolean {
    if (streamUrl.isBlank() || !hasVerifiedYoutubeMusicVideoPairing()) return false
    if (videoStreamUrl.isNotBlank()) return true

    return playbackManifest
        ?.streams
        ?.asSequence()
        ?.filter { it.selected }
        ?.any { stream ->
            stream.kind == PlaybackStreamKind.MUXED ||
                stream.kind == PlaybackStreamKind.VIDEO ||
                stream.kind == PlaybackStreamKind.HLS
        } == true
}
