package com.luc4n3x.levyra.ui.player

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import java.lang.ref.WeakReference
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

/**
 * PlayerView surface that accepts transforms only from Levyra's Compose gesture coordinator.
 * Single-pointer taps/drags stay owned by the existing player gesture layer.
 */
@UnstableApi
class LevyraGesturePlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PlayerView(context, attrs, defStyleAttr) {
    private var videoScale = PLAYER_VIDEO_MIN_SCALE
    private var videoTranslationX = 0f
    private var videoTranslationY = 0f

    internal val isVideoZoomed: Boolean
        get() = videoScale > PLAYER_VIDEO_MIN_SCALE + PLAYER_VIDEO_RESET_EPSILON

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        resetVideoTransform()
        PlayerVideoTransformController.attach(this)
    }

    override fun onDetachedFromWindow() {
        PlayerVideoTransformController.detach(this)
        super.onDetachedFromWindow()
    }

    internal fun applyVideoTransform(
        centroidX: Float,
        centroidY: Float,
        panX: Float,
        panY: Float,
        zoomChange: Float
    ) {
        val surface = videoSurfaceView ?: return
        val previousScale = videoScale
        val nextScale = boundedPlayerVideoScale(previousScale, zoomChange)
        val ratio = if (previousScale > 0f) nextScale / previousScale else 1f
        val localFocusX = centroidX - surface.left
        val localFocusY = centroidY - surface.top
        val focusFromCenterX = localFocusX - surface.width / 2f
        val focusFromCenterY = localFocusY - surface.height / 2f

        videoTranslationX = ratio * videoTranslationX + (1f - ratio) * focusFromCenterX + panX
        videoTranslationY = ratio * videoTranslationY + (1f - ratio) * focusFromCenterY + panY
        videoScale = nextScale
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
        PlayerVideoTransformController.onViewStateChanged(false)
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
        PlayerVideoTransformController.onViewStateChanged(isVideoZoomed)
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

/** Keeps multi-touch transforms out of the Media3 player and inside the existing gesture owner. */
internal object PlayerVideoTransformController {
    private var viewRef = WeakReference<LevyraGesturePlayerView>(null)
    private var boundKey: Any? = null

    @Volatile
    var isZoomed: Boolean = false
        private set

    @Synchronized
    fun attach(view: LevyraGesturePlayerView) {
        viewRef = WeakReference(view)
        isZoomed = view.isVideoZoomed
    }

    @Synchronized
    fun detach(view: LevyraGesturePlayerView) {
        if (viewRef.get() === view) {
            viewRef.clear()
            isZoomed = false
        }
    }

    @Synchronized
    fun bind(key: Any?) {
        if (boundKey == key) return
        boundKey = key
        viewRef.get()?.resetVideoTransform()
        isZoomed = false
    }

    fun transform(
        centroidX: Float,
        centroidY: Float,
        panX: Float,
        panY: Float,
        zoomChange: Float
    ) {
        viewRef.get()?.applyVideoTransform(centroidX, centroidY, panX, panY, zoomChange)
    }

    fun panBy(deltaX: Float, deltaY: Float) {
        viewRef.get()?.panVideoBy(deltaX, deltaY)
    }

    fun reset() {
        viewRef.get()?.resetVideoTransform()
        isZoomed = false
    }

    fun onViewStateChanged(zoomed: Boolean) {
        isZoomed = zoomed
    }
}
