package com.luc4n3x.levyra.feature.player.domain

import com.luc4n3x.levyra.architecture.mobius.LevyraNext
import com.luc4n3x.levyra.architecture.mobius.LevyraUpdate
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.domain.Track

object PlayerUpdate : LevyraUpdate<PlayerModel, PlayerEvent, PlayerEffect> {
    override fun update(model: PlayerModel, event: PlayerEvent): LevyraNext<PlayerModel, PlayerEffect> {
        val safeModel = model.normalized()
        return when (event) {
            is PlayerEvent.TrackRequested -> onTrackRequested(safeModel, event)
            PlayerEvent.PlayClicked -> onPlayClicked(safeModel)
            PlayerEvent.PauseClicked -> onPauseClicked(safeModel)
            PlayerEvent.TogglePlayClicked -> if (safeModel.isPlaying) onPauseClicked(safeModel) else onPlayClicked(safeModel)
            PlayerEvent.StopClicked -> LevyraNext.dispatch(safeModel.copy(isPlaying = false, isResolving = false, positionMs = 0L, pendingSeekMs = null), PlayerEffect.StopPlayback)
            PlayerEvent.NextClicked -> LevyraNext.dispatch(safeModel.copy(errorMessage = null), PlayerEffect.SelectNextTrack)
            PlayerEvent.PreviousClicked -> LevyraNext.dispatch(safeModel.copy(errorMessage = null), PlayerEffect.SelectPreviousTrack)
            is PlayerEvent.SeekRequested -> onSeekRequested(safeModel, event)
            is PlayerEvent.ProgressChanged -> LevyraNext.next(safeModel.copy(positionMs = event.positionMs.coerceAtLeast(0L), durationMs = event.durationMs.coerceAtLeast(0L)))
            PlayerEvent.ResolveStarted -> LevyraNext.next(safeModel.copy(isResolving = true, errorMessage = null))
            is PlayerEvent.ResolveSucceeded -> onResolveSucceeded(safeModel, event.track)
            is PlayerEvent.ResolveFailed -> onResolveFailed(safeModel, event.message)
            PlayerEvent.PlaybackStarted -> LevyraNext.next(safeModel.copy(isPlaying = true, isResolving = false, errorMessage = null))
            PlayerEvent.PlaybackPaused -> LevyraNext.dispatch(safeModel.copy(isPlaying = false), PlayerEffect.PersistSnapshot(safeModel.currentTrack, safeModel.safePositionMs, false))
            PlayerEvent.PlaybackCompleted -> onPlaybackCompleted(safeModel)
            is PlayerEvent.QueueChanged -> LevyraNext.next(safeModel.copy(queue = event.queue).normalized())
            is PlayerEvent.VideoModeChanged -> onVideoModeChanged(safeModel, event.enabled)
            is PlayerEvent.RepeatModeChanged -> LevyraNext.next(safeModel.copy(repeatMode = event.repeatMode))
            is PlayerEvent.ShuffleChanged -> LevyraNext.next(safeModel.copy(shuffleEnabled = event.enabled))
            PlayerEvent.ErrorConsumed -> LevyraNext.next(safeModel.copy(errorMessage = null))
        }
    }

    private fun onTrackRequested(model: PlayerModel, event: PlayerEvent.TrackRequested): LevyraNext<PlayerModel, PlayerEffect> {
        val queue = event.queue.ifEmpty { model.queue }
        val nextModel = model.copy(
            currentTrack = event.track,
            queue = queue,
            isPlaying = event.startPlaying,
            isResolving = event.startPlaying,
            positionMs = 0L,
            durationMs = event.track.durationMs.coerceAtLeast(0L),
            errorMessage = null,
            pendingSeekMs = null
        ).normalized()
        return if (event.startPlaying) {
            LevyraNext.dispatch(nextModel, PlayerEffect.ResolveTrack(event.track, nextModel.isVideoMode), PlayerEffect.LoadLyrics(event.track))
        } else {
            LevyraNext.dispatch(nextModel, PlayerEffect.LoadLyrics(event.track), PlayerEffect.PersistSnapshot(event.track, 0L, false))
        }
    }

    private fun onPlayClicked(model: PlayerModel): LevyraNext<PlayerModel, PlayerEffect> {
        val track = model.currentTrack ?: return LevyraNext.next(model)
        return if (track.streamUrl.isBlank()) {
            LevyraNext.dispatch(model.copy(isPlaying = true, isResolving = true, errorMessage = null), PlayerEffect.ResolveTrack(track, model.isVideoMode))
        } else {
            LevyraNext.dispatch(model.copy(isPlaying = true, isResolving = false, errorMessage = null), PlayerEffect.StartPlayback(track))
        }
    }

    private fun onPauseClicked(model: PlayerModel): LevyraNext<PlayerModel, PlayerEffect> =
        LevyraNext.dispatch(model.copy(isPlaying = false), PlayerEffect.PausePlayback, PlayerEffect.PersistSnapshot(model.currentTrack, model.safePositionMs, false))

    private fun onSeekRequested(model: PlayerModel, event: PlayerEvent.SeekRequested): LevyraNext<PlayerModel, PlayerEffect> {
        val position = event.positionMs.coerceAtLeast(0L)
        return if (model.currentTrack == null) {
            LevyraNext.next(model.copy(positionMs = position, pendingSeekMs = position))
        } else {
            LevyraNext.dispatch(model.copy(positionMs = position, pendingSeekMs = position), PlayerEffect.SeekTo(position), PlayerEffect.PersistSnapshot(model.currentTrack, position, model.isPlaying))
        }
    }

    private fun onResolveSucceeded(model: PlayerModel, track: Track): LevyraNext<PlayerModel, PlayerEffect> {
        val updated = model.copy(currentTrack = track, isResolving = false, durationMs = track.durationMs.coerceAtLeast(0L), errorMessage = null)
        val effects = buildSet {
            if (model.isPlaying) add(PlayerEffect.StartPlayback(track))
            model.pendingSeekMs?.let { add(PlayerEffect.SeekTo(it)) }
            add(PlayerEffect.PersistSnapshot(track, updated.safePositionMs, updated.isPlaying))
        }
        return LevyraNext(updated.copy(pendingSeekMs = null), effects)
    }

    private fun onResolveFailed(model: PlayerModel, message: String): LevyraNext<PlayerModel, PlayerEffect> {
        val clean = message.trim().ifBlank { "Riproduzione non riuscita" }
        return LevyraNext.dispatch(model.copy(isPlaying = false, isResolving = false, errorMessage = clean), PlayerEffect.ReportPlaybackError(clean))
    }

    private fun onPlaybackCompleted(model: PlayerModel): LevyraNext<PlayerModel, PlayerEffect> =
        when {
            model.repeatMode == RepeatMode.One && model.currentTrack != null -> LevyraNext.dispatch(model.copy(positionMs = 0L, isPlaying = true), PlayerEffect.StartPlayback(model.currentTrack))
            model.queue.isNotEmpty() -> LevyraNext.dispatch(model.copy(positionMs = 0L), PlayerEffect.SelectNextTrack)
            else -> LevyraNext.dispatch(model.copy(isPlaying = false, positionMs = model.safeDurationMs), PlayerEffect.PersistSnapshot(model.currentTrack, model.safeDurationMs, false))
        }

    private fun onVideoModeChanged(model: PlayerModel, enabled: Boolean): LevyraNext<PlayerModel, PlayerEffect> {
        val track = model.currentTrack
        val updated = model.copy(isVideoMode = enabled, errorMessage = null)
        return if (track != null && model.isPlaying) {
            LevyraNext.dispatch(updated.copy(isResolving = true), PlayerEffect.ResolveTrack(track.copy(streamUrl = "", videoStreamUrl = ""), enabled))
        } else {
            LevyraNext.next(updated)
        }
    }
}
