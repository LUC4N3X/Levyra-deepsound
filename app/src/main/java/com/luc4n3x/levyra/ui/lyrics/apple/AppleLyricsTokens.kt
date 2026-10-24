package com.luc4n3x.levyra.ui.lyrics.apple

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object AppleLyricsTokens {
    val lineTextSize = 34.sp
    val backgroundTextSize = 22.sp
    val secondaryTextSize = 20.sp
    val staticTextSize = 17.sp
    val staticLineHeight = 26.sp
    val headerTextSize = 17.sp
    val rowMargin = 20.dp
    val textPaddingHorizontal = 12.dp
    val textPaddingTop = 2.dp
    val textPaddingBottom = 10.dp
    val rowSpacing = 20.dp
    val backgroundTop = 22.dp
    val translationTop = 10.dp
    val rippleCorner = 16.dp
    val feather = 30.dp
    val lift = 2.dp
    val emphasisGlow = 5.dp
    val dotSize = 10.dp
    val dotGap = 6.dp
    val staticPadding = 32.dp
    val staticTop = 30.dp
    val staticSpacing = 40.dp
    val headerTop = 32.dp
    val headerStart = 32.dp
    val headerEnd = 16.dp
    val thumbnail = 60.dp
    val thumbnailCorner = 5.dp
    val thumbnailGap = 8.dp
    val headerIcon = 28.dp
    val headerIconPadding = 16.dp
    val seekbarInset = 32.dp
    val transportGap = 40.dp
    val skipIcon = 40.dp
    val playIcon = 38.dp
    val controlPadding = 11.dp
    val dockIcon = 28.dp
    val dockPadding = 16.dp
    val dockBottom = 14.dp
    val translationButtonBottom = 27.dp
    const val DUET_WIDTH = 0.85f
    const val INACTIVE_SCALE = 0.98f
    const val ANCHOR_FRACTION = 0.08f
    const val TOP_FADE_FRACTION = 0.07f
    const val BOTTOM_FADE_FRACTION = 0.2775f
    const val CONTROLS_FRACTION = 0.345f
    const val LETTER_SPACING_EM = -0.02f
    const val FOCUS_MS = 750
    const val CONTROLS_FADE_MS = 500
    const val CONTROLS_HIDE_DELAY_MS = 3_000L
    const val DOT_APPEAR_MS = 750
    const val DOT_STAGGER_MS = 50
    const val DOT_GROW_MS = 750
    const val DOT_SHRINK_MS = 250
    const val BREATHING_SCALE = 1.2f
    const val EXIT_SCALE = 0.5f
    val primary = Color.White.copy(alpha = 0.94f)
    val secondary = Color.White.copy(alpha = 0.45f)
    val tertiary = Color.White.copy(alpha = 0.18f)
    val quaternary = Color.White.copy(alpha = 0.08f)
    val backgroundActive = Color.White.copy(alpha = 0.35f)
    val loading = Color.White.copy(alpha = 0.30f)
    val thumbnailStroke = Color.Black.copy(alpha = 0.10f)
}

internal val AppleLyricsEase = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
internal val AppleLyricsMoveEase = CubicBezierEasing(0.4f, 0.1f, 0f, 1f)
internal val AppleLyricsDotEase = CubicBezierEasing(0f, 0.25f, 1f, 0.58f)
internal val AppleLyricsExitEase = CubicBezierEasing(0.25f, 0f, 1f, 0.2f)
internal val AppleLyricsAccelerate = CubicBezierEasing(0.4f, 0f, 1f, 1f)
