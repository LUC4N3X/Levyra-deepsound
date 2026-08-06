from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")
text = path.read_text(encoding="utf-8")

text = replace_once(
    text,
    '''private data class SamplesPlaybackSession(
    val queue: PlaybackQueueSnapshot,
    val currentTrack: Track?,
    val videoMode: Boolean,
    val loopOnCompletion: Boolean,
    val wasPlaying: Boolean,
    val positionMs: Long
)


internal fun monotonicDownloadProgress''',
    '''private data class SamplesPlaybackSession(
    val queue: PlaybackQueueSnapshot,
    val currentTrack: Track?,
    val videoMode: Boolean,
    val loopOnCompletion: Boolean,
    val wasPlaying: Boolean,
    val positionMs: Long
)

internal fun selectYoutubeShortSample(list: List<Track>, requested: Track): Track? {
    if (list.isEmpty()) return null
    val requestedIdentity = playbackIdentity(requested)
    val selected = list.firstOrNull { candidate -> playbackIdentity(candidate) == requestedIdentity }
        ?: requested
    return selected.takeIf(::isYoutubeShortTrack)
}


internal fun monotonicDownloadProgress''',
    "add validated Samples selection helper",
)

text = replace_once(
    text,
    '''    fun playSample(list: List<Track>, track: Track) {
        if (list.isEmpty()) return
        val currentState = _state.value
        if (samplesPlaybackSession == null) {
            samplesPlaybackSession = SamplesPlaybackSession(
                queue = queueEngine.state.value,
                currentTrack = currentState.currentTrack,
                videoMode = currentState.isVideoMode,
                loopOnCompletion = loopCurrentQueueOnCompletion,
                wasPlaying = currentState.isPlaying,
                positionMs = player.positionMs.coerceAtLeast(0L).takeIf { it > 0L }
                    ?: currentState.positionMs
            )
        }

        val selected = list.firstOrNull { candidate -> samePlayableTrack(candidate, track) } ?: track
        if (!isYoutubeShortTrack(selected)) return
        beginSamplesPlayback()
''',
    '''    fun playSample(list: List<Track>, track: Track) {
        val selected = selectYoutubeShortSample(list, track) ?: return
        val currentState = _state.value
        if (samplesPlaybackSession == null) {
            samplesPlaybackSession = SamplesPlaybackSession(
                queue = queueEngine.state.value,
                currentTrack = currentState.currentTrack,
                videoMode = currentState.isVideoMode,
                loopOnCompletion = loopCurrentQueueOnCompletion,
                wasPlaying = currentState.isPlaying,
                positionMs = player.positionMs.coerceAtLeast(0L).takeIf { it > 0L }
                    ?: currentState.positionMs
            )
        }

        beginSamplesPlayback()
''',
    "validate Short before session capture",
)

path.write_text(text, encoding="utf-8")
assert "val selected = selectYoutubeShortSample(list, track) ?: return" in text
assert text.index("val selected = selectYoutubeShortSample") < text.index("samplesPlaybackSession == null")
print("Validated Shorts before capturing the Samples playback session")
