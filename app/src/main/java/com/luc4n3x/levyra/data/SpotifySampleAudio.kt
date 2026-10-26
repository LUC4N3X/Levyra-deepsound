package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track

private val SPOTIFY_SAMPLE_AUDIO_URL = Regex("^https://p\\.scdn\\.co/mp3-preview/[a-f0-9]{40}(?:\\.mp3)?(?:\\?[A-Za-z0-9%=&._-]+)?$")

internal fun isSpotifySampleAudioUrl(url: String): Boolean = SPOTIFY_SAMPLE_AUDIO_URL.matches(url)

internal data class SpotifySampleAudio(val trackId: String, val url: String)

internal fun Track.withSpotifySampleAudio(audio: SpotifySampleAudio): Track = copy(
    streamUrl = audio.url,
    videoUrl = "https://open.spotify.com/track/${audio.trackId}",
    durationMs = 0L,
    videoStreamUrl = "",
    playbackManifest = null,
    audioVideoId = "",
    counterpartVideoId = "",
    sponsorSegments = emptyList(),
    videoSubtitleTracks = emptyList(),
    source = "Spotify Samples"
)
