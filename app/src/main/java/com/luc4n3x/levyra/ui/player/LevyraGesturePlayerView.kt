package com.luc4n3x.levyra.ui.player

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import kotlin.math.abs

private const val PLAYER_VIDEO_MIN_SCALE = 1f
internal const val PLAYER_VIDEO_MAX_SCALE = 5f
private const val PLAYER_VIDEO_RESET_EPSILON = 0.015f

internal fun boundedPlayerVideoScale(current: Float, zoomChange: Float): Float {
    if (!current.isFinite() || !zoomChange.isFinite() || zoomChange <= 0f) return PLAYER_VIDEO_MIN_SCALE
    return (current * zoomChange).coerceIn(PLAYER_VIDEO_MIN_SCALE, PLAYER_VIDEO_MAX_SCALE)
}

internal fun boundedPlayerVideoTranslation(value: Float, dimensionPx: Float, scale: Float): Float {
    if (!value.isFinite() || !dimensionPx.isFinite() || dimensionPx <= 0f || scale <= PLAYER_VIDEO_MIN_SCALE) {
        return 0f
    }
    val maxTranslation = dimensionPx * (scale - PLAYER_VIDEO_MIN_SCALE) / 2f
    return value.coerceIn(-maxTranslation, maxTranslation)
}

@UnstableApi
class LevyraGesturePlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PlayerView(context, attrs, defStyleAttr) {
    private var videoScale = PLAYER_VIDEO_MIN_SCALE
    private var videoTranslationX = 0f
    private var videoTranslationY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var lastScaleFocusX = 0f
    private var lastScaleFocusY = 0f
    private var observedPlayer: Player? = null
    private var playerListenerAttached = false

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            resetVideoTransform()
        }
    }

    private val scaleDetector = ScaleGestureDetector(
        context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                lastScaleFocusX = detector.focusX
                lastScaleFocusY = detector.focusY
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val panX = detector.focusX - lastScaleFocusX
                val panY = detector.focusY - lastScaleFocusY
                lastScaleFocusX = detector.focusX
                lastScaleFocusY = detector.focusY
                applyVideoTransform(panX, panY, detector.scaleFactor)
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                if (!isVideoZoomed) parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
    )

    internal val isVideoZoomed: Boolean
        get() = videoScale > PLAYER_VIDEO_MIN_SCALE + PLAYER_VIDEO_RESET_EPSILON

    override fun setPlayer(player: Player?) {
        if (observedPlayer !== player) {
            detachPlayerListener()
            observedPlayer = player
        }
        super.setPlayer(player)
        if (isAttachedToWindow) attachPlayerListener()
        resetVideoTransform()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        observedPlayer = player
        attachPlayerListener()
        resetVideoTransform()
    }

    override fun onDetachedFromWindow() {
        detachPlayerListener()
        parent?.requestDisallowInterceptTouchEvent(false)
        resetVideoTransform()
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val zoomedBeforeEvent = isVideoZoomed
        scaleDetector.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                if (isVideoZoomed) parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress && event.pointerCount == 1 && isVideoZoomed) {
                    val deltaX = event.x - lastTouchX
                    val deltaY = event.y - lastTouchY
                    lastTouchX = event.x
                    lastTouchY = event.y
                    panVideoBy(deltaX, deltaY)
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
                lastTouchX = event.x
                lastTouchY = event.y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        if (scaleDetector.isInProgress || event.pointerCount > 1 || isVideoZoomed || zoomedBeforeEvent) {
            return true
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean = super.performClick()

    internal fun applyVideoTransform(panX: Float, panY: Float, zoomChange: Float) {
        val surface = videoSurfaceView ?: return
        videoScale = boundedPlayerVideoScale(videoScale, zoomChange)
        videoTranslationX += panX
        videoTranslationY += panY
        normalizeAndApply(surface)
    }

    internal fun panVideoBy(deltaX: Float, deltaY: Float) {
        if (!isVideoZoomed) return
        val surface = videoSurfaceView ?: return
        videoTranslationX += deltaX
        videoTranslationY += deltaY
        normalizeAndApply(surface)
    }

    internal fun resetVideoTransform() {
        videoScale = PLAYER_VIDEO_MIN_SCALE
        videoTranslationX = 0f
        videoTranslationY = 0f
        videoSurfaceView?.let(::applyToSurface)
    }

    private fun attachPlayerListener() {
        val current = observedPlayer ?: return
        if (playerListenerAttached) return
        current.addListener(playerListener)
        playerListenerAttached = true
    }

    private fun detachPlayerListener() {
        if (!playerListenerAttached) return
        observedPlayer?.removeListener(playerListener)
        playerListenerAttached = false
    }

    private fun normalizeAndApply(surface: View) {
        if (abs(videoScale - PLAYER_VIDEO_MIN_SCALE) <= PLAYER_VIDEO_RESET_EPSILON) {
            videoScale = PLAYER_VIDEO_MIN_SCALE
            videoTranslationX = 0f
            videoTranslationY = 0f
        } else {
            videoTranslationX = boundedPlayerVideoTranslation(
                videoTranslationX,
                surface.width.toFloat(),
                videoScale
            )
            videoTranslationY = boundedPlayerVideoTranslation(
                videoTranslationY,
                surface.height.toFloat(),
                videoScale
            )
        }
        applyToSurface(surface)
    }

    private fun applyToSurface(surface: View) {
        surface.pivotX = surface.width / 2f
        surface.pivotY = surface.height / 2f
        surface.scaleX = videoScale
        surface.scaleY = videoScale
        surface.translationX = videoTranslationX
        surface.translationY = videoTranslationY
    }
}
