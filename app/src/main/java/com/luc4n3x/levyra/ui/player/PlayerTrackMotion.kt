package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SwipeCarryFactor = 1.6f
private const val SwipeCarryHoldMs = 700L

/**
 * +1 when [currentId] directly follows [previousId] in [queue], -1 when it directly precedes it,
 * 0 otherwise (new queue, jump, repeat-one or unknown).
 */
internal fun trackStepDirection(
    queue: List<Track>,
    previousId: String?,
    currentId: String?,
    currentIndexHint: Int
): Int {
    if (previousId.isNullOrEmpty() || currentId.isNullOrEmpty() || previousId == currentId) return 0
    val current = if (queue.getOrNull(currentIndexHint)?.id == currentId) {
        currentIndexHint
    } else {
        queue.indexOfFirst { it.id == currentId }
    }
    if (current < 0) return 0
    return when (previousId) {
        queue.getOrNull(current - 1)?.id -> 1
        queue.getOrNull(current + 1)?.id -> -1
        else -> 0
    }
}

private class TrackStepMemory {
    var trackId: String? = null
}

/** Screen-space direction of the latest track change, already mirrored for RTL. */
@Composable
internal fun rememberTrackStepDirection(
    trackId: String?,
    queue: List<Track>,
    queueIndex: Int
): Int {
    val memory = remember { TrackStepMemory() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val direction = remember(trackId) {
        trackStepDirection(queue, memory.trackId, trackId, queueIndex)
    }
    SideEffect { memory.trackId = trackId }
    return if (rtl) -direction else direction
}

@Stable
internal class PlayerSwipeMotion(private val scope: CoroutineScope) {
    private val offset = Animatable(0f)
    private var ownerId by mutableStateOf<String?>(null)
    private var releaseJob: Job? = null
    internal var animated: Boolean = true
    internal var carryLimitPx: Float = Float.POSITIVE_INFINITY

    fun offsetFor(trackId: String?): Float =
        if (trackId != null && trackId == ownerId) offset.value else 0f

    fun follow(trackId: String, offsetPx: Float) {
        releaseJob?.cancel()
        releaseJob = null
        ownerId = trackId
        scope.launch { offset.snapTo(offsetPx) }
    }

    /**
     * Returns the content home. A committed swipe with [carry] first keeps travelling the way the
     * finger went, so the outgoing track leaves with the gesture instead of bouncing back before
     * the next one arrives. The outgoing track stays partly in view, dimmed, until the next one is
     * ready; if no track change follows, it springs home.
     */
    fun release(committed: Boolean, carry: Boolean) {
        releaseJob?.cancel()
        releaseJob = scope.launch {
            if (!animated) {
                offset.snapTo(0f)
                return@launch
            }
            if (committed && carry && offset.value != 0f) {
                val carried = (offset.value * SwipeCarryFactor)
                    .coerceIn(-maxOf(carryLimitPx, abs(offset.value)), maxOf(carryLimitPx, abs(offset.value)))
                offset.animateTo(
                    carried,
                    tween(LevyraMotion.Durations.Short, easing = LevyraMotion.Easings.Accelerate)
                )
                delay(SwipeCarryHoldMs)
            }
            offset.animateTo(0f, LevyraMotion.gesture.spec())
        }
    }
}

@Composable
internal fun rememberPlayerSwipeMotion(animated: Boolean): PlayerSwipeMotion {
    val scope = rememberCoroutineScope()
    val motion = remember(scope) { PlayerSwipeMotion(scope) }
    SideEffect { motion.animated = animated }
    return motion
}
