package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.nexus.identity.MusicIdentityText

internal data class MusicVideoRecording(val title: String, val artist: String)

internal object MusicVideoTitle {
    private val decorationSeparator = Regex("\\s+(?:\\||/)\\s+")
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
        val title = rawTitle
            .split(decorationSeparator)
            .first()
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
        return MusicVideoRecording(title = right, artist = left)
    }

    private fun describesVersionOnly(left: String, right: String): Boolean =
        MusicIdentityText.title("$left - $right").core == MusicIdentityText.title(left).core
}
