package com.luc4n3x.levyra.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object LevyraPlayerDesign {

    val CornerXxs: Dp = 8.dp
    val CornerXs: Dp = 12.dp
    val CornerSm: Dp = 16.dp
    val CornerMd: Dp = 22.dp
    val CornerLg: Dp = 28.dp
    val CornerXl: Dp = 34.dp

    val ShapeXxs: Shape = RoundedCornerShape(CornerXxs)
    val ShapeXs: Shape = RoundedCornerShape(CornerXs)
    val ShapeSm: Shape = RoundedCornerShape(CornerSm)
    val ShapeMd: Shape = RoundedCornerShape(CornerMd)
    val ShapeLg: Shape = RoundedCornerShape(CornerLg)
    val ShapeXl: Shape = RoundedCornerShape(CornerXl)
    val ShapePill: Shape = RoundedCornerShape(percent = 50)

    val SpaceXxs: Dp = 2.dp
    val SpaceXs: Dp = 4.dp
    val SpaceSm: Dp = 8.dp
    val SpaceMd: Dp = 12.dp
    val SpaceLg: Dp = 18.dp
    val SpaceXl: Dp = 24.dp

    val GutterCompact: Dp = 16.dp
    val Gutter: Dp = 22.dp

    val HeaderButton: Dp = 40.dp
    val HeaderButtonCompact: Dp = 36.dp
    val ModeIndicator: Dp = 4.dp
    val PrimaryCornerPlaying: Dp = 22.dp
    val MinimumTouchTarget: Dp = 48.dp

    val TransportHeight: Dp = 76.dp
    val TransportHeightCompact: Dp = 64.dp
    val TransportGap: Dp = 5.dp
    val TransportInnerCorner: Dp = 12.dp
    val TransportGlyph: Dp = 30.dp
    val TransportGlyphCompact: Dp = 26.dp
    val TransportModeGlyph: Dp = 22.dp
    val TransportPlayGlyph: Dp = 38.dp
    val TransportPlayGlyphCompact: Dp = 32.dp
    val DockHeight: Dp = 50.dp
    val DockHeightCompact: Dp = 46.dp
    val DockMaxWidth: Dp = 340.dp
    val DockGap: Dp = 3.dp
    val DockInnerCorner: Dp = 6.dp
    val DockGlyph: Dp = 22.dp
    val SegmentPressedInnerCorner: Dp = 22.dp
    const val SegmentPressGrowth: Float = 0.16f

    const val ArtworkCornerRatio: Float = 0.075f
    val ArtworkCornerMin: Dp = 18.dp
    val ArtworkCornerMax: Dp = 32.dp
    const val ArtworkPausedScale: Float = 0.92f
    const val ArtworkTrackChangeScale: Float = 0.94f

    val MiniCorner: Dp = 20.dp
    val MiniArtwork: Dp = 46.dp
    val MiniArtworkCorner: Dp = 13.dp
    val MiniHeight: Dp = 64.dp
    val DockTrayCorner: Dp = 28.dp

    val Hairline: Dp = 1.dp
    val TrackHeight: Dp = 4.dp
    val TrackHeightActive: Dp = 6.dp
    val ThumbRadius: Dp = 6.dp
    val ThumbRadiusActive: Dp = 8.5.dp
    val HandleWidth: Dp = 12.dp
    val HandleWidthActive: Dp = 17.dp
    val HandleHeight: Dp = 12.dp
    val HandleHeightActive: Dp = 17.dp

    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Standard: Easing = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)
    val Decelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    const val ExpressiveDamping: Float = 0.64f
    const val ExpressiveStiffness: Float = 360f
    const val SmoothDamping: Float = 0.90f
    const val SmoothStiffness: Float = 540f
    const val SnappyDamping: Float = 0.82f
    const val SnappyStiffness: Float = 1_100f

    fun <T> expressiveSpring(): SpringSpec<T> =
        spring(dampingRatio = ExpressiveDamping, stiffness = ExpressiveStiffness)

    fun <T> smoothSpring(): SpringSpec<T> =
        spring(dampingRatio = SmoothDamping, stiffness = SmoothStiffness)

    fun <T> snappySpring(): SpringSpec<T> =
        spring(dampingRatio = SnappyDamping, stiffness = SnappyStiffness)

    const val PressDamping: Float = 0.58f
    const val PressStiffness: Float = 760f
    const val ExpandDamping: Float = 0.82f
    const val ExpandStiffness: Float = 360f
    const val CollapseDamping: Float = 0.86f
    const val CollapseStiffness: Float = 430f
    const val PaletteMillis: Int = 650

    fun <T> motion(animated: Boolean, spec: AnimationSpec<T>): AnimationSpec<T> =
        if (animated) spec else snap()

    fun <T> pressSpring(): SpringSpec<T> =
        spring(dampingRatio = PressDamping, stiffness = PressStiffness)

    fun <T> expandSpring(): SpringSpec<T> =
        spring(dampingRatio = ExpandDamping, stiffness = ExpandStiffness)

    fun <T> collapseSpring(): SpringSpec<T> =
        spring(dampingRatio = CollapseDamping, stiffness = CollapseStiffness)

    fun <T> paletteTween(): TweenSpec<T> =
        tween(durationMillis = PaletteMillis, easing = Decelerate)

    fun <T> emphasizedTween(durationMillis: Int = 320): TweenSpec<T> =
        tween(durationMillis = durationMillis, easing = Emphasized)

    fun <T> standardTween(durationMillis: Int = 220): TweenSpec<T> =
        tween(durationMillis = durationMillis, easing = Standard)

    val GlassFill: Color = Color.White.copy(alpha = 0.08f)
    val GlassFillStrong: Color = Color.White.copy(alpha = 0.14f)
    val GlassFillSunken: Color = Color.Black.copy(alpha = 0.32f)
    val GlassBorderTop: Color = Color.White.copy(alpha = 0.14f)
    val GlassBorderBottom: Color = Color.White.copy(alpha = 0.06f)

    val TextPrimary: Color = Color.White
    val TextSecondary: Color = Color.White.copy(alpha = 0.70f)
    val TextTertiary: Color = Color.White.copy(alpha = 0.48f)
    val TrackInactive: Color = Color.White.copy(alpha = 0.18f)
    val TrackBuffered: Color = Color.White.copy(alpha = 0.32f)
}
