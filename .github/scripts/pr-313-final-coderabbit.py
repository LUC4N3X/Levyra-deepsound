from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


samples_path = Path("app/src/main/java/com/luc4n3x/levyra/ui/ExploreSamplesScreen.kt")
samples = samples_path.read_text(encoding="utf-8")
samples = replace_once(
    samples,
    '''            ) as PlayerView).apply {
                keepScreenOn = true
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                this.player = player
            }
        },
        update = { view ->
            val active = PlaybackService.activePlayer
            if (view.player !== active) view.player = active
            view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            view.keepScreenOn = isPlaying
        },
''',
    '''            ) as PlayerView).apply {
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                this.player = player
            }
        },
        update = { view ->
            if (view.player !== player) view.player = player
            view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            view.keepScreenOn = isPlaying
        },
''',
    "drive PlayerView attachment from activePlayerFlow",
)
samples_path.write_text(samples, encoding="utf-8")

view_model_path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")
view_model = view_model_path.read_text(encoding="utf-8")
view_model = replace_once(
    view_model,
    '''                loopOnCompletion = loopCurrentQueueOnCompletion,
                wasPlaying = currentState.isPlaying,
                positionMs = currentState.positionMs
            )
''',
    '''                loopOnCompletion = loopCurrentQueueOnCompletion,
                wasPlaying = currentState.isPlaying,
                positionMs = player.positionMs.coerceAtLeast(0L).takeIf { it > 0L }
                    ?: currentState.positionMs
            )
''',
    "capture live playback position",
)
view_model = replace_once(
    view_model,
    '''        val durationMs = effectiveDuration(restoredTrack)
        val resumeMs = session.positionMs.coerceAtLeast(0L).let { position ->
            if (durationMs > 0L) position.coerceAtMost(durationMs) else position
        }
''',
    '''        val durationMs = restoredTrack.durationMs
        val resumeMs = session.positionMs.coerceAtLeast(0L).let { position ->
            if (durationMs > 0L) position.coerceAtMost(durationMs) else position
        }
''',
    "clamp against restored track duration",
)
view_model = replace_once(
    view_model,
    '''                positionMs = resumeMs,
                bufferedPositionMs = resumeMs,
                durationMs = durationMs,
                playerError = null
''',
    '''                positionMs = resumeMs,
                bufferedPositionMs = resumeMs,
                durationMs = effectiveDuration(restoredTrack),
                playerError = null
''',
    "publish restored live duration",
)
view_model = replace_once(
    view_model,
    '''    ) {
        val retryWhenOnline = autoRetryWhenOffline && !hasInternetCapableNetwork()
        player.stop()
''',
    '''    ) {
        val retryWhenOnline = autoRetryWhenOffline && !hasInternetCapableNetwork()
        pauseAfterNextPlaybackStart = false
        player.stop()
''',
    "clear deferred pause after resolve failure",
)
view_model_path.write_text(view_model, encoding="utf-8")

assert "val active = PlaybackService.activePlayer" not in samples
assert "positionMs = currentState.positionMs" not in view_model
assert "val durationMs = effectiveDuration(restoredTrack)" not in view_model
print("Applied all four remaining CodeRabbit fixes for PR #313")
