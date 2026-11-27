package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.nexus.identity.MusicIdentityText
import java.util.Locale

internal data class MusicVideoRecording(val title: String, val artist: String, val alternate: MusicVideoRecording? = null)

internal object MusicVideoTitle {
    private val pipeSeparator = Regex("\\s+\\|\\s+")
    private val slashSeparator = Regex("\\s+/\\s+")
    private val decorationWords = listOf("official", "video", "klip", "clip", "lyric", "audio", "visualizer", "премьера", "клип", "видео")
    private val artistTitleSeparator = Regex("\\s+(?:--|-|–|—)\\s+")
    private val videoNoise = Regex(
        "[(\\[][^)\\]]*(?:премьера|клип|видео|official|video|audio|lyric|visualizer)[^)\\]]*[)\\]]",
        RegexOption.IGNORE_CASE
    )
    private val whitespace = Regex("\\s+")
    private const val AUDIO_TRACK_VIDEO_TYPE = "MUSIC_VIDEO_TYPE_ATV"

    fun recordingFor(track: Track): MusicVideoRecording =
        if (isMusicVideo(track.videoType)) {
            recording(track.title, track.artist)
        } else {
            MusicVideoRecording(track.title, track.artist)
        }

    private fun isMusicVideo(videoType: String): Boolean =
        videoType.isNotBlank() && !videoType.equals(AUDIO_TRACK_VIDEO_TYPE, ignoreCase = true)

    fun recording(rawTitle: String, rawArtist: String): MusicVideoRecording {
        val title = withoutDecorations(rawTitle, rawArtist)
            .replace(videoNoise, " ")
            .replace(whitespace, " ")
            .trim()
            .ifBlank { return MusicVideoRecording(rawTitle, rawArtist) }
        val separator = artistTitleSeparator.find(title) ?: return MusicVideoRecording(title, rawArtist)
        val left = title.substring(0, separator.range.first).trim()
        val right = title.substring(separator.range.last + 1).trim()
        if (left.isBlank() || right.isBlank() || describesVersionOnly(left, right)) {
            return MusicVideoRecording(title, rawArtist)
        }
        val channel = MusicIdentityText.normalize(rawArtist)
        if (channel.isNotBlank() && channel !in MusicIdentityText.normalize(left) && channel in MusicIdentityText.normalize(right)) {
            return MusicVideoRecording(title = left, artist = right)
        }
        val channelNamed = channel.isNotBlank() && channel in MusicIdentityText.normalize(left)
        return MusicVideoRecording(
            title = right,
            artist = left,
            alternate = if (channelNamed) null else MusicVideoRecording(title = left, artist = right)
        )
    }

    private fun withoutDecorations(rawTitle: String, rawArtist: String): String {
        val head = rawTitle.split(pipeSeparator).first()
        val slash = slashSeparator.find(head) ?: return head
        val before = head.substring(0, slash.range.first)
        val after = head.substring(slash.range.last + 1)
        return if (slashStartsDecoration(before, after, rawArtist)) before else head
    }

    private fun slashStartsDecoration(before: String, after: String, rawArtist: String): Boolean {
        if (artistTitleSeparator.containsMatchIn(before)) return true
        val channel = MusicIdentityText.normalize(rawArtist)
        if (channel.isNotBlank() && channel in MusicIdentityText.normalize(after)) return true
        val tail = after.lowercase(Locale.ROOT)
        return decorationWords.any { it in tail }
    }

    private fun describesVersionOnly(left: String, right: String): Boolean =
        MusicIdentityText.title("$left - $right").core == MusicIdentityText.title(left).core
}
