package com.luc4n3x.levyra.feature.ambient

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.data.LyricsRepository
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.player.PlaybackService
import com.luc4n3x.levyra.ui.ambient.AmbientUiState
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

internal class AmbientSessionPresenter(
    context: Context,
    private val scope: CoroutineScope
) {
    private val appContext = context.applicationContext
    private val preferences = LevyraPreferences(appContext)
    private val lyricsRepository by lazy { LyricsRepository(appContext) }

    private val _state = MutableStateFlow(AmbientUiState())
    val state: StateFlow<AmbientUiState> = _state.asStateFlow()

    private var controller: MediaController? = null
    private var connectJob: Job? = null
    private var tickJob: Job? = null
    private var lyricsJob: Job? = null
    private var lyricsMediaId: String = ""
    private var lyricLines: List<LyricLine> = emptyList()

    private val listener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = publish()

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            publish()
            restartTicker()
        }

        override fun onPlaybackStateChanged(playbackState: Int) = publish()

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) = publish()
    }

    fun connect() {
        if (controller != null || connectJob?.isActive == true) return
        _state.value = _state.value.copy(settings = preferences.ambientSettings())
        val future = MediaController.Builder(
            appContext,
            SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        ).buildAsync()
        connectJob = scope.launch {
            val connected = try {
                withContext(Dispatchers.IO) { future.get(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS) }
            } catch (cancelled: CancellationException) {
                MediaController.releaseFuture(future)
                throw cancelled
            } catch (error: Exception) {
                MediaController.releaseFuture(future)
                Timber.w(error, "Ambient could not attach to the playback session")
                null
            }
            if (connected == null) return@launch
            if (!isActive) {
                connected.release()
                return@launch
            }
            controller = connected
            connected.addListener(listener)
            publish()
            restartTicker()
        }
    }

    fun release() {
        connectJob?.cancel()
        connectJob = null
        tickJob?.cancel()
        tickJob = null
        lyricsJob?.cancel()
        lyricsJob = null
        lyricLines = emptyList()
        lyricsMediaId = ""
        controller?.removeListener(listener)
        controller?.release()
        controller = null
    }

    fun togglePlay() {
        val active = controller ?: return
        if (active.isPlaying) active.pause() else active.play()
    }

    fun next() {
        controller?.takeIf { it.hasNextMediaItem() }?.seekToNextMediaItem()
    }

    fun previous() {
        controller?.seekToPreviousMediaItem()
    }

    private fun restartTicker() {
        tickJob?.cancel()
        val active = controller ?: return
        if (!active.isPlaying) return
        tickJob = scope.launch {
            while (isActive) {
                delay(POSITION_TICK_MS)
                publish()
            }
        }
    }

    private fun publish() {
        val active = controller
        val metadata = active?.mediaMetadata
        val mediaId = active?.currentMediaItem?.mediaId.orEmpty()
        val hasTrack = active != null && active.mediaItemCount > 0
        val positionMs = active?.currentPosition?.coerceAtLeast(0L) ?: 0L
        val durationMs = active?.duration?.takeIf { it > 0L } ?: 0L
        val title = metadata?.title?.toString().orEmpty()
        val artist = metadata?.artist?.toString().orEmpty()

        if (mediaId != lyricsMediaId) {
            lyricsMediaId = mediaId
            lyricLines = emptyList()
            loadLyrics(mediaId, title, artist, durationMs)
        }

        _state.value = _state.value.copy(
            hasTrack = hasTrack,
            title = title,
            artist = artist,
            artworkUrl = metadata?.artworkUri?.toString().orEmpty(),
            isPlaying = active?.isPlaying == true,
            positionMs = positionMs,
            durationMs = durationMs,
            lyricLine = currentLyricLine(positionMs)
        )
    }

    private fun currentLyricLine(positionMs: Long): String {
        if (lyricLines.isEmpty()) return ""
        val line = lyricLines.lastOrNull { it.startMs <= positionMs } ?: return ""
        if (line.endMs > 0L && positionMs > line.endMs + LYRIC_HOLD_MS) return ""
        return line.text
    }

    private fun loadLyrics(mediaId: String, title: String, artist: String, durationMs: Long) {
        lyricsJob?.cancel()
        if (!_state.value.settings.showLyrics || title.isBlank()) return
        lyricsJob = scope.launch {
            val result = try {
                withContext(Dispatchers.IO) {
                    lyricsRepository.fetch(
                        title = title,
                        artist = artist,
                        durationSec = TimeUnit.MILLISECONDS.toSeconds(durationMs.coerceAtLeast(0L)),
                        videoId = mediaId,
                        languageCode = preferences.languageCode()
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Ambient lyrics lookup failed")
                null
            }
            if (mediaId != lyricsMediaId) return@launch
            lyricLines = result?.takeIf { it.synced }?.lines.orEmpty()
            publish()
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_SECONDS = 5L
        const val POSITION_TICK_MS = 1_000L
        const val LYRIC_HOLD_MS = 4_000L
    }
}
