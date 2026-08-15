package com.luc4n3x.levyra.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
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

    val HeaderButton: Dp = 42.dp
    val HeaderButtonCompact: Dp = 38.dp
    val UtilityButton: Dp = 44.dp
    val UtilityButtonCompact: Dp = 40.dp
    val TransportButton: Dp = 54.dp
    val TransportButtonCompact: Dp = 48.dp
    val TransportSquircleCorner: Dp = 18.dp
    val TransportSquircleCornerCompact: Dp = 16.dp
    val PrimaryWidth: Dp = 76.dp
    val PrimaryWidthCompact: Dp = 70.dp
    val PrimaryHeight: Dp = 64.dp
    val PrimaryHeightCompact: Dp = 58.dp
    val PrimaryCorner: Dp = 24.dp
    val MinimumTouchTarget: Dp = 48.dp

    val Hairline: Dp = 1.dp
    val TrackHeight: Dp = 4.5.dp
    val TrackHeightActive: Dp = 7.5.dp
    val WaveAmplitude: Dp = 3.5.dp
    val HandleWidth: Dp = 6.dp
    val HandleWidthActive: Dp = 8.dp
    val HandleHeight: Dp = 20.dp
    val HandleHeightActive: Dp = 26.dp

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

    fun <T> emphasizedTween(durationMillis: Int = 320): TweenSpec<T> =
        tween(durationMillis = durationMillis, easing = Emphasized)

    fun <T> standardTween(durationMillis: Int = 220): TweenSpec<T> =
        tween(durationMillis = durationMillis, easing = Standard)

    const val WavePeriodDp: Float = 26f
    const val WaveCycleMillis: Int = 1_600

    val GlassFill: Color = Color.White.copy(alpha = 0.055f)
    val GlassFillStrong: Color = Color.White.copy(alpha = 0.10f)
    val GlassFillSunken: Color = Color.Black.copy(alpha = 0.28f)
    val GlassBorderTop: Color = Color.White.copy(alpha = 0.14f)
    val GlassBorderBottom: Color = Color.White.copy(alpha = 0.05f)
    val GlassSpecular: Color = Color.White.copy(alpha = 0.025f)

    val TextPrimary: Color = Color.White
    val TextSecondary: Color = Color.White.copy(alpha = 0.76f)
    val TextTertiary: Color = Color.White.copy(alpha = 0.50f)
    val IconIdle: Color = Color.White.copy(alpha = 0.65f)
}
