package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale

object PlaybackSourceIdentity {
    private val youtubeIdPattern = Regex("[A-Za-z0-9_-]{11}")
    private val youtubeUrlPattern = Regex("(?:v=|/shorts/|/embed/|/live/|youtu\\.be/)([A-Za-z0-9_-]{11})")

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
            durationBucket.toString()
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

    fun matchKey(track: Track, videoMode: Boolean, audioQuality: String): String {
        val mode = if (videoMode) "video" else "audio"
        return "${canonicalKey(track)}|$mode|${audioQuality.trim().lowercase(Locale.ROOT)}"
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
