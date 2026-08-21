package com.luc4n3x.levyra.ui

import androidx.compose.runtime.Immutable

@Immutable
internal data class MotionArtworkFit(val scaleX: Float, val scaleY: Float)

internal val MotionArtworkFitIdentity = MotionArtworkFit(1f, 1f)

internal const val MotionArtworkCardMaxZoom = 2.6f
internal const val MotionArtworkImmersiveMaxZoom = 1.32f
internal const val MotionArtworkStageMaxZoom = 1.56f

internal fun motionArtworkFit(
    videoWidth: Int,
    videoHeight: Int,
    pixelWidthHeightRatio: Float,
    containerWidth: Int,
    containerHeight: Int,
    maxZoom: Float
): MotionArtworkFit {
    if (videoWidth <= 0 || videoHeight <= 0) return MotionArtworkFitIdentity
    if (containerWidth <= 0 || containerHeight <= 0) return MotionArtworkFitIdentity
    val pixelRatio = if (pixelWidthHeightRatio > 0f && pixelWidthHeightRatio.isFinite()) {
        pixelWidthHeightRatio
    } else {
        1f
    }
    val videoAspect = videoWidth.toFloat() * pixelRatio / videoHeight.toFloat()
    val containerAspect = containerWidth.toFloat() / containerHeight.toFloat()
    if (!videoAspect.isFinite() || videoAspect <= 0f) return MotionArtworkFitIdentity
    if (!containerAspect.isFinite() || containerAspect <= 0f) return MotionArtworkFitIdentity

    val aspectRatioDelta = videoAspect / containerAspect
    val containScaleX = if (aspectRatioDelta >= 1f) 1f else aspectRatioDelta
    val containScaleY = if (aspectRatioDelta >= 1f) 1f / aspectRatioDelta else 1f
    val coverZoom = maxOf(aspectRatioDelta, 1f / aspectRatioDelta)
    val zoom = coverZoom.coerceAtMost(maxZoom.coerceAtLeast(1f))
    return MotionArtworkFit(
        scaleX = containScaleX * zoom,
        scaleY = containScaleY * zoom
    )
}
