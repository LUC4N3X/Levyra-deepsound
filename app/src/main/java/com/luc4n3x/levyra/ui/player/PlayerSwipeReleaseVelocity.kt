package com.luc4n3x.levyra.ui.player

import kotlin.math.abs

internal fun playerSwipeReleaseVelocity(
    offsetPx: Float,
    velocityPx: Float,
    minFlingDistancePx: Float
): Float = velocityPx
    .takeIf { offsetPx.isFinite() }
    ?.takeIf { it.isFinite() }
    ?.takeIf { minFlingDistancePx.isFinite() }
    ?.takeIf { abs(offsetPx) >= minFlingDistancePx.coerceAtLeast(0f) }
    ?.takeIf { it < 0f == offsetPx < 0f }
    ?: 0f
