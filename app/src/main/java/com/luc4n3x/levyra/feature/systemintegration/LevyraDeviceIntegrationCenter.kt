package com.luc4n3x.levyra.feature.systemintegration

import android.content.Context
import androidx.media3.common.Player
import com.luc4n3x.levyra.data.LyricsLatencyProfiles
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.feature.audio.LevyraAudioOutputState
import com.luc4n3x.levyra.ui.lyrics.lyricsOffsetPosition
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal data class LevyraIslandPlayerState(
    val mediaId: String,
    val title: String,
    val artist: String,
    val active: Boolean,
    val playing: Boolean,
    val positionMs: Long,
    val durationMs: Long,
    val speed: Float
)

internal class LevyraDeviceIntegrationCenter(
    private val scope: CoroutineScope,
    adapters: List<LevyraIslandAdapter>,
    latencyProfiles: Flow<LyricsLatencyProfiles>,
    private val audioOutput: StateFlow<LevyraAudioOutputState>,
    private val playerState: () -> LevyraIslandPlayerState?,
    private val onAdapterFailure: (LevyraDeviceFeature, Throwable) -> Unit = ::logIslandFailure
) {
    private val enabledAdapters = adapters.toMutableList()
    private var lyrics: LevyraIslandLyrics? = null
    private var profiles = LyricsLatencyProfiles()
    private var tickJob: Job? = null
    private var idleJob: Job? = null
    private var released = false
    private val profilesJob = scope.launch {
        latencyProfiles.collect { updated ->
            if (updated == profiles) return@collect
            profiles = updated
            refresh()
        }
    }
    private val routeJob = scope.launch {
        audioOutput.map { it.active?.stableKey to (it.active?.bluetooth == true) }
            .distinctUntilChanged()
            .drop(1)
            .collect { refresh() }
    }

    fun onLyrics(mediaId: String, trackId: String, lines: List<LyricLine>, synced: Boolean) {
        if (released) return
        val playingMediaId = playerState()?.mediaId
        if (mediaId != playingMediaId && lyrics?.mediaId == playingMediaId) return
        val prepared = levyraIslandLyrics(mediaId, trackId, lines, synced)
        if (prepared == null && lyrics?.mediaId != mediaId) return
        lyrics = prepared
        refresh()
    }

    fun onPlayerEvents(events: Player.Events) {
        if (events.containsAny(*REFRESH_EVENTS)) refresh()
    }

    fun refresh() {
        if (released) return
        tickJob?.cancel()
        tickJob = null
        idleJob?.cancel()
        idleJob = null
        val state = playerState()
        if (state == null || state.mediaId.isBlank()) {
            dispatchClear()
            return
        }
        if (!state.active) {
            idleJob = scope.launch {
                delay(PAUSED_CLEAR_DELAY_MS)
                idleJob = null
                dispatchClear()
            }
        }
        val currentLyrics = lyrics?.takeIf { it.mediaId == state.mediaId }
        val lyricsPositionMs = currentLyrics?.let {
            val route = audioOutput.value.active
            val offsetMs = profiles.effectiveOffsetMs(it.trackId, route?.stableKey, route?.bluetooth == true)
            lyricsOffsetPosition(state.positionMs, offsetMs)
        }
        val frame = if (currentLyrics != null && lyricsPositionMs != null) {
            levyraIslandLyricFrame(currentLyrics.lines, lyricsPositionMs)
        } else {
            null
        }
        val snapshot = LevyraIslandSnapshot(
            mediaId = state.mediaId,
            title = state.title,
            artist = state.artist,
            active = state.active,
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            speed = state.speed,
            lyric = frame
        )
        dispatch { it.publish(snapshot) }
        if (!state.playing) return
        val lyricDelayMs = if (frame != null && lyricsPositionMs != null) {
            levyraIslandTickDelayMs(lyricsPositionMs, frame.nextBoundaryMs, state.speed)
        } else {
            null
        }
        val progressDelayMs = levyraIslandProgressTickDelayMs(state.positionMs, state.durationMs, state.speed)
        val delayMs = listOfNotNull(lyricDelayMs, progressDelayMs).minOrNull() ?: return
        tickJob = scope.launch {
            delay(delayMs)
            tickJob = null
            refresh()
        }
    }

    fun release() {
        if (released) return
        released = true
        tickJob?.cancel()
        tickJob = null
        idleJob?.cancel()
        idleJob = null
        profilesJob.cancel()
        routeJob.cancel()
        dispatchClear()
        lyrics = null
    }

    private fun dispatchClear() = dispatch { it.clear() }

    private fun dispatch(action: (LevyraIslandAdapter) -> Unit) {
        val iterator = enabledAdapters.iterator()
        while (iterator.hasNext()) {
            val adapter = iterator.next()
            try {
                action(adapter)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                onAdapterFailure(adapter.feature, error)
                iterator.remove()
                runCatching { adapter.clear() }
            }
        }
    }

    private companion object {
        const val PAUSED_CLEAR_DELAY_MS = 60_000L
        val REFRESH_EVENTS = intArrayOf(
            Player.EVENT_MEDIA_ITEM_TRANSITION,
            Player.EVENT_MEDIA_METADATA_CHANGED,
            Player.EVENT_IS_PLAYING_CHANGED,
            Player.EVENT_PLAY_WHEN_READY_CHANGED,
            Player.EVENT_PLAYBACK_STATE_CHANGED,
            Player.EVENT_POSITION_DISCONTINUITY,
            Player.EVENT_PLAYBACK_PARAMETERS_CHANGED
        )
    }
}

internal fun levyraIslandPlayerState(player: Player): LevyraIslandPlayerState? {
    val item = player.currentMediaItem ?: return null
    val playbackState = player.playbackState
    if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) return null
    val metadata = player.mediaMetadata
    return LevyraIslandPlayerState(
        mediaId = item.mediaId,
        title = (metadata.title ?: metadata.displayTitle ?: item.mediaMetadata.title)?.toString().orEmpty(),
        artist = (metadata.artist ?: metadata.albumArtist ?: item.mediaMetadata.artist)?.toString().orEmpty(),
        active = player.playWhenReady,
        playing = player.isPlaying,
        positionMs = player.currentPosition.coerceAtLeast(0L),
        durationMs = player.duration.takeIf { it > 0L } ?: 0L,
        speed = player.playbackParameters.speed
    )
}

internal fun levyraIslandAdapters(context: Context, profile: LevyraDeviceProfile): List<LevyraIslandAdapter> =
    buildList {
        if (profile.supports(LevyraDeviceFeature.SUPER_ISLAND)) add(HyperOsSuperIslandAdapter(context))
        if (profile.supports(LevyraDeviceFeature.ATOMIC_ISLAND)) add(VivoAtomicIslandAdapter(context))
    }
