package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal enum class PlayerLyricsFace {
    Player,
    Lyrics
}

internal enum class LyricsFlipAxis {
    Undecided,
    Horizontal,
    Vertical
}

private const val LyricsFlipHorizontalDominance = 1.3f
private const val LyricsFlipDragSpanFraction = 0.85f
private const val LyricsFlipCommitFraction = 0.3f
private const val LyricsFlipSettledEpsilon = 0.001f
private const val LyricsFlipDepthDurationMs = 380
private const val LyricsFlipFadeDurationMs = 220
private const val LyricsFlipMinDurationMs = 110
private const val LyricsFlipDepthScale = 0.06f
private const val LyricsFlipCameraDistance = 14f
private val LyricsFlipFlingVelocity = 400.dp
private val LyricsFlipMinFlingDistance = 24.dp

internal fun resolveLyricsFlipAxis(totalX: Float, totalY: Float, slopPx: Float): LyricsFlipAxis {
    val horizontal = abs(totalX.finiteOrZero())
    val vertical = abs(totalY.finiteOrZero())
    val slop = slopPx.finiteOrZero().coerceAtLeast(0f)
    if (horizontal < slop && vertical < slop) return LyricsFlipAxis.Undecided
    return if (horizontal >= vertical * LyricsFlipHorizontalDominance) {
        LyricsFlipAxis.Horizontal
    } else {
        LyricsFlipAxis.Vertical
    }
}

internal fun lyricsFlipDragProgress(
    startProgress: Float,
    dragPx: Float,
    widthPx: Float,
    rightToLeft: Boolean
): Float {
    val start = startProgress.finiteOrZero().coerceIn(0f, 1f)
    val span = widthPx.finiteOrZero() * LyricsFlipDragSpanFraction
    if (span <= 0f) return start
    val towardLyrics = if (rightToLeft) dragPx.finiteOrZero() else -dragPx.finiteOrZero()
    return (start + towardLyrics / span).coerceIn(0f, 1f)
}

internal fun lyricsFlipSettleTarget(
    startFace: PlayerLyricsFace,
    progress: Float,
    dragPx: Float,
    velocityPx: Float,
    flingVelocityPx: Float,
    minFlingDistancePx: Float,
    rightToLeft: Boolean
): PlayerLyricsFace {
    val towardLyricsDistance = if (rightToLeft) dragPx.finiteOrZero() else -dragPx.finiteOrZero()
    val towardLyricsVelocity = if (rightToLeft) velocityPx.finiteOrZero() else -velocityPx.finiteOrZero()
    val fling = abs(towardLyricsVelocity) >= flingVelocityPx &&
        abs(towardLyricsDistance) >= minFlingDistancePx &&
        (towardLyricsVelocity > 0f) == (towardLyricsDistance > 0f)
    if (fling) {
        return if (towardLyricsVelocity > 0f) PlayerLyricsFace.Lyrics else PlayerLyricsFace.Player
    }
    val threshold = if (startFace == PlayerLyricsFace.Player) {
        LyricsFlipCommitFraction
    } else {
        1f - LyricsFlipCommitFraction
    }
    return if (progress.finiteOrZero() >= threshold) PlayerLyricsFace.Lyrics else PlayerLyricsFace.Player
}

internal fun lyricsFlipReleaseVelocity(trackedVelocityPx: Float, travelPx: Float, elapsedMs: Long): Float {
    val tracked = trackedVelocityPx.finiteOrZero()
    if (tracked != 0f || elapsedMs <= 0L) return tracked
    return travelPx.finiteOrZero() * 1_000f / elapsedMs.toFloat()
}

internal fun lyricsFlipFaceAlpha(
    progress: Float,
    back: Boolean,
    depth: Boolean,
    frontVisible: Boolean = true
): Float {
    val p = progress.finiteOrZero().coerceIn(0f, 1f)
    return when {
        !frontVisible && !back -> 0f
        !frontVisible && depth -> if (p > 0f) 1f else 0f
        !frontVisible -> p
        depth -> if (back == (p >= 0.5f)) 1f else 0f
        back -> (p * 2f - 1f).coerceIn(0f, 1f)
        else -> (1f - p * 2f).coerceIn(0f, 1f)
    }
}

internal fun lyricsFlipFaceRotation(
    progress: Float,
    back: Boolean,
    rightToLeft: Boolean,
    frontVisible: Boolean = true
): Float {
    val p = progress.finiteOrZero().coerceIn(0f, 1f)
    val angle = when {
        !frontVisible -> if (back) (p - 1f) * 90f else 0f
        back -> (p - 1f) * 180f
        else -> p * 180f
    }
    return if (rightToLeft) -angle else angle
}

internal fun lyricsFlipFaceScale(progress: Float, depth: Boolean): Float {
    if (!depth) return 1f
    val p = progress.finiteOrZero().coerceIn(0f, 1f)
    return 1f - LyricsFlipDepthScale * sin(PI.toFloat() * p)
}

internal fun lyricsFlipSettleDurationMs(from: Float, to: Float, depth: Boolean): Int {
    val distance = abs(to.finiteOrZero() - from.finiteOrZero()).coerceIn(0f, 1f)
    val full = if (depth) LyricsFlipDepthDurationMs else LyricsFlipFadeDurationMs
    return (full * distance).roundToInt().coerceAtLeast(LyricsFlipMinDurationMs)
}

@Stable
internal class PlayerLyricsFlipState(private val scope: CoroutineScope) {
    private val progressState = mutableFloatStateOf(0f)
    private var settleJob: Job? = null
    private var dragStartProgress = 0f
    private var dragStartFace = PlayerLyricsFace.Player
    private var dragTotalPx = 0f

    var face by mutableStateOf(PlayerLyricsFace.Player)
        private set

    val progress: Float
        get() = progressState.floatValue

    val playerSettled by derivedStateOf {
        face == PlayerLyricsFace.Player && progressState.floatValue <= LyricsFlipSettledEpsilon
    }

    val lyricsSettled by derivedStateOf {
        face == PlayerLyricsFace.Lyrics && progressState.floatValue >= 1f - LyricsFlipSettledEpsilon
    }

    val lyricsComposed by derivedStateOf {
        face == PlayerLyricsFace.Lyrics || progressState.floatValue > LyricsFlipSettledEpsilon
    }

    fun beginDrag() {
        settleJob?.cancel()
        settleJob = null
        dragStartProgress = progressState.floatValue
        dragStartFace = face
        dragTotalPx = 0f
    }

    fun dragBy(deltaPx: Float, widthPx: Float, rightToLeft: Boolean) {
        dragTotalPx += deltaPx.finiteOrZero()
        progressState.floatValue = lyricsFlipDragProgress(dragStartProgress, dragTotalPx, widthPx, rightToLeft)
    }

    fun endDrag(
        velocityPx: Float,
        flingVelocityPx: Float,
        minFlingDistancePx: Float,
        rightToLeft: Boolean,
        depth: Boolean
    ) {
        val target = lyricsFlipSettleTarget(
            startFace = dragStartFace,
            progress = progressState.floatValue,
            dragPx = dragTotalPx,
            velocityPx = velocityPx,
            flingVelocityPx = flingVelocityPx,
            minFlingDistancePx = minFlingDistancePx,
            rightToLeft = rightToLeft
        )
        settleTo(target, depth)
    }

    fun cancelDrag(depth: Boolean) {
        settleTo(face, depth)
    }

    fun show(target: PlayerLyricsFace, depth: Boolean) {
        settleTo(target, depth)
    }

    fun snapTo(target: PlayerLyricsFace) {
        settleJob?.cancel()
        settleJob = null
        face = target
        progressState.floatValue = if (target == PlayerLyricsFace.Lyrics) 1f else 0f
    }

    private fun settleTo(target: PlayerLyricsFace, depth: Boolean) {
        settleJob?.cancel()
        face = target
        val start = progressState.floatValue
        val end = if (target == PlayerLyricsFace.Lyrics) 1f else 0f
        if (start == end) {
            settleJob = null
            return
        }
        settleJob = scope.launch {
            animate(
                initialValue = start,
                targetValue = end,
                animationSpec = tween(
                    durationMillis = lyricsFlipSettleDurationMs(start, end, depth),
                    easing = LevyraMotion.Easings.Emphasized
                )
            ) { value, _ -> progressState.floatValue = value }
        }
    }
}

@Composable
internal fun rememberPlayerLyricsFlipState(): PlayerLyricsFlipState {
    val scope = rememberCoroutineScope()
    return remember(scope) { PlayerLyricsFlipState(scope) }
}

internal fun Modifier.playerLyricsFlipDrag(
    state: PlayerLyricsFlipState,
    enabled: Boolean,
    rightToLeft: Boolean,
    depth: Boolean
): Modifier {
    if (!enabled) return this
    return pointerInput(state, rightToLeft, depth) {
        val slop = viewConfiguration.touchSlop
        val flingVelocity = LyricsFlipFlingVelocity.toPx()
        val minFlingDistance = LyricsFlipMinFlingDistance.toPx()
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val gesture = LyricsFlipGesture(
                state = state,
                slopPx = slop,
                flingVelocityPx = flingVelocity,
                minFlingDistancePx = minFlingDistance,
                widthPx = size.width.toFloat(),
                rightToLeft = rightToLeft,
                depth = depth
            )
            gesture.start(down)
            try {
                while (true) {
                    val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                    if (change.changedToUpIgnoreConsumed()) {
                        gesture.release(change)
                        break
                    }
                    if (!gesture.move(change)) break
                }
            } finally {
                gesture.cancelIfUnsettled()
            }
        }
    }
}

private class LyricsFlipGesture(
    private val state: PlayerLyricsFlipState,
    private val slopPx: Float,
    private val flingVelocityPx: Float,
    private val minFlingDistancePx: Float,
    private val widthPx: Float,
    private val rightToLeft: Boolean,
    private val depth: Boolean
) {
    private val velocityTracker = VelocityTracker()
    private var totalX = 0f
    private var totalY = 0f
    private var axis = LyricsFlipAxis.Undecided
    private var settled = false
    private var lockUptimeMs = 0L
    private var lockedTravelPx = 0f

    fun start(down: PointerInputChange) {
        velocityTracker.addPointerInputChange(down)
    }

    fun move(change: PointerInputChange): Boolean {
        if (axis == LyricsFlipAxis.Undecided && change.isConsumed) return false
        val delta = change.positionChange()
        velocityTracker.addPointerInputChange(change)
        if (axis != LyricsFlipAxis.Undecided) {
            change.consume()
            lockedTravelPx += delta.x
            state.dragBy(delta.x, widthPx, rightToLeft)
            return true
        }
        totalX += delta.x
        totalY += delta.y
        axis = resolveLyricsFlipAxis(totalX, totalY, slopPx)
        if (axis == LyricsFlipAxis.Horizontal) {
            state.beginDrag()
            lockUptimeMs = change.uptimeMillis
            change.consume()
        }
        return axis != LyricsFlipAxis.Vertical
    }

    fun release(change: PointerInputChange) {
        if (axis != LyricsFlipAxis.Horizontal) return
        velocityTracker.addPointerInputChange(change)
        val finalDelta = change.positionChangeIgnoreConsumed().x
        lockedTravelPx += finalDelta
        state.dragBy(finalDelta, widthPx, rightToLeft)
        state.endDrag(
            velocityPx = lyricsFlipReleaseVelocity(
                trackedVelocityPx = velocityTracker.calculateVelocity().x,
                travelPx = lockedTravelPx,
                elapsedMs = change.uptimeMillis - lockUptimeMs
            ),
            flingVelocityPx = flingVelocityPx,
            minFlingDistancePx = minFlingDistancePx,
            rightToLeft = rightToLeft,
            depth = depth
        )
        settled = true
        change.consume()
    }

    fun cancelIfUnsettled() {
        if (axis == LyricsFlipAxis.Horizontal && !settled) state.cancelDrag(depth)
    }
}

internal fun Modifier.playerLyricsFlipFace(
    state: PlayerLyricsFlipState,
    back: Boolean,
    depth: Boolean,
    rightToLeft: Boolean,
    frontVisible: Boolean = true
): Modifier = graphicsLayer {
    val progress = state.progress
    alpha = lyricsFlipFaceAlpha(progress, back, depth, frontVisible)
    if (depth) {
        rotationY = lyricsFlipFaceRotation(progress, back, rightToLeft, frontVisible)
        val scale = lyricsFlipFaceScale(progress, depth = true)
        scaleX = scale
        scaleY = scale
        cameraDistance = LyricsFlipCameraDistance * density
    }
}

private fun Float.finiteOrZero(): Float = if (isFinite()) this else 0f
