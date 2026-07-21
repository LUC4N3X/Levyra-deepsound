package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale

object PlaybackSourceIdentity {
    private const val YOUTUBE_VIDEO_ID_PATTERN = "[A-Za-z0-9_-]{11}"
    private val youtubeIdPattern = Regex(YOUTUBE_VIDEO_ID_PATTERN)
    private val youtubeUrlPattern = Regex("(?:v=|/shorts/|/embed/|/live/|youtu\\.be/)($YOUTUBE_VIDEO_ID_PATTERN)")

    fun canonicalKey(track: Track): String {
        val isrc = track.isrc.trim().lowercase(Locale.ROOT)
        if (isrc.isNotBlank()) return "isrc:$isrc"
        val durationBucket = when {
            track.durationMs <= 0L -> 0L
            else -> (track.durationMs + 1_000L) / 2_000L
        }
        val payload = listOf(
            normalize(track.title),
            normalize(track.artist),
            normalize(track.album),
            durationBucket.toString(),
            if (track.explicit) "explicit" else "clean",
            track.discNumber.coerceAtLeast(0).toString(),
            track.trackNumber.coerceAtLeast(0).toString(),
            normalizeIdentifier(track.upc),
            recordingDiscriminator(track)
        ).joinToString("|")
        return "track:${sha256(payload).take(32)}"
    }

    fun sourceVideoId(track: Track): String {
        extractYoutubeVideoId(track.videoUrl).takeIf { it.isNotBlank() }?.let { return it }
        track.id.trim().takeIf { youtubeIdPattern.matches(it) }?.let { return it }
        track.counterpartVideoId.trim().takeIf { youtubeIdPattern.matches(it) }?.let { return it }
        return ""
    }

    fun extractYoutubeVideoId(value: String): String {
        val clean = value.trim()
        if (youtubeIdPattern.matches(clean)) return clean
        return youtubeUrlPattern.find(clean)?.groupValues?.getOrNull(1).orEmpty()
    }

    fun matchKey(
        track: Track,
        videoMode: Boolean,
        audioQuality: String,
        preferMp4Audio: Boolean = false
    ): String {
        val mode = when {
            videoMode -> "video"
            preferMp4Audio -> "audio-mp4"
            else -> "audio"
        }
        return "${canonicalKey(track)}|$mode|${audioQuality.trim().lowercase(Locale.ROOT)}"
    }

    private fun recordingDiscriminator(track: Track): String {
        normalizeIdentifier(track.id).takeIf { it.isNotBlank() }?.let { return "id:$it" }
        normalizeIdentifier(track.counterpartVideoId).takeIf { it.isNotBlank() }?.let { return "counterpart:$it" }
        extractYoutubeVideoId(track.videoUrl).lowercase(Locale.ROOT).takeIf { it.isNotBlank() }?.let { return "youtube:$it" }
        return "metadata-only"
    }

    private fun normalizeIdentifier(value: String): String {
        return value.trim().lowercase(Locale.ROOT)
    }

    private fun normalize(value: String): String {
        return value
            .lowercase(Locale.ROOT)
            .replace(Regex("\\([^)]*(official|audio|video|lyrics?|visuali[sz]er|remaster)[^)]*\\)", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\[[^]]*(official|audio|video|lyrics?|visuali[sz]er|remaster)[^]]*]", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { byte -> "%02x".format(byte) }
    }
}
