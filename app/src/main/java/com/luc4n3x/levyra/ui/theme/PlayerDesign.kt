package com.luc4n3x.levyra.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
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
    const val ArtworkTrackChangeScale: Float = 0.965f

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

    val Emphasized: Easing = LevyraMotion.Easings.Emphasized
    val Standard: Easing = LevyraMotion.Easings.Standard
    val Decelerate: Easing = LevyraMotion.Easings.Decelerate

    const val ExpressiveDamping: Float = LevyraMotion.Springs.ExpressiveDamping
    const val ExpressiveStiffness: Float = LevyraMotion.Springs.ExpressiveStiffness
    const val SmoothDamping: Float = LevyraMotion.Springs.SettleDamping
    const val SmoothStiffness: Float = LevyraMotion.Springs.SettleStiffness
    const val SnappyDamping: Float = LevyraMotion.Springs.SnappyDamping
    const val SnappyStiffness: Float = LevyraMotion.Springs.SnappyStiffness

    fun <T> expressiveSpring(): SpringSpec<T> = LevyraMotion.expressive.spec()

    fun <T> smoothSpring(): SpringSpec<T> = LevyraMotion.settle.spec()

    fun <T> snappySpring(): SpringSpec<T> = LevyraMotion.snappy.spec()

    const val PressDamping: Float = LevyraMotion.Springs.ReleaseDamping
    const val PressStiffness: Float = LevyraMotion.Springs.ReleaseStiffness
    const val ExpandDamping: Float = LevyraMotion.Springs.ExpandDamping
    const val ExpandStiffness: Float = LevyraMotion.Springs.ExpandStiffness
    const val CollapseDamping: Float = LevyraMotion.Springs.CollapseDamping
    const val CollapseStiffness: Float = LevyraMotion.Springs.CollapseStiffness
    const val PaletteMillis: Int = LevyraMotion.Durations.Palette

    fun <T> motion(animated: Boolean, spec: AnimationSpec<T>): AnimationSpec<T> =
        LevyraMotion.spec(animated, spec)

    fun <T> pressSpring(): SpringSpec<T> = LevyraMotion.release.spec()

    fun <T> expandSpring(): SpringSpec<T> = LevyraMotion.expand.spec()

    fun <T> collapseSpring(): SpringSpec<T> = LevyraMotion.collapse.spec()

    fun <T> paletteTween(): TweenSpec<T> = LevyraMotion.palette()

    fun <T> emphasizedTween(durationMillis: Int = 320): TweenSpec<T> =
        LevyraMotion.emphasized(durationMillis)

    fun <T> standardTween(durationMillis: Int = 220): TweenSpec<T> =
        LevyraMotion.standard(durationMillis)

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
