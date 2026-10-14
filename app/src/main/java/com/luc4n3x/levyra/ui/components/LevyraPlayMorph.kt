package com.luc4n3x.levyra.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.toPath
import com.luc4n3x.levyra.ui.theme.LocalLevyraVisualCapabilities

private const val PlaySpinPeriodDegrees = 30f
private const val PlaySpinPeriodMillis = 5_200
private const val ArtworkSpinPeriodMillis = 48_000
private const val NowPlayingSpinPeriodMillis = 3_600

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val PlayMorph: Morph by lazy {
    Morph(MaterialShapes.Square.normalized(), MaterialShapes.Cookie12Sided.normalized())
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val AvatarPressMorph: Morph by lazy {
    Morph(MaterialShapes.Circle.normalized(), MaterialShapes.Cookie12Sided.normalized())
}

private const val ArtworkRestRounding = 0.42f

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val ArtworkMorph: Morph by lazy {
    Morph(
        RoundedPolygon.rectangle(rounding = CornerRounding(ArtworkRestRounding, smoothing = 1f)).normalized(),
        MaterialShapes.Puffy.normalized()
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val FavoriteMorph: Morph by lazy {
    Morph(MaterialShapes.Circle.normalized(), MaterialShapes.Cookie9Sided.normalized())
}

internal fun levyraCookieLandingShape(progress: Float, rotationDegrees: Float = 0f): Shape =
    LevyraMorphShape(PlayMorph, progress, rotationDegrees)

internal class LevyraMorphShape(
    private val morph: Morph,
    private val progress: Float,
    private val rotationDegrees: Float = 0f
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = morph.toPath(progress.coerceIn(0f, 1f), android.graphics.Path()).asComposePath()
        if (rotationDegrees != 0f) {
            path.transform(
                Matrix().apply {
                    translate(0.5f, 0.5f)
                    rotateZ(rotationDegrees)
                    translate(-0.5f, -0.5f)
                }
            )
        }
        path.transform(Matrix().apply { scale(size.width, size.height) })
        return Outline.Generic(path)
    }
}

@Composable
internal fun rememberLevyraPlayMorphShape(
    playing: Boolean,
    animated: Boolean,
    spinWhilePlaying: Boolean = false
): Shape = rememberSpinningMorphShape(
    morph = PlayMorph,
    active = playing,
    animated = animated,
    spin = spinWhilePlaying,
    spinDegrees = PlaySpinPeriodDegrees,
    spinMillis = PlaySpinPeriodMillis,
    label = "levyra-play-morph"
)

@Composable
internal fun rememberLevyraArtworkMorphShape(playing: Boolean, animated: Boolean): Shape =
    rememberSpinningMorphShape(
        morph = ArtworkMorph,
        active = playing,
        animated = animated,
        spin = true,
        spinDegrees = 360f,
        spinMillis = ArtworkSpinPeriodMillis,
        label = "levyra-artwork-morph"
    )

@Composable
private fun rememberSpinningMorphShape(
    morph: Morph,
    active: Boolean,
    animated: Boolean,
    spin: Boolean,
    spinDegrees: Float,
    spinMillis: Int,
    label: String
): Shape {
    val progress by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = if (animated) {
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
        } else {
            snap()
        },
        label = label
    )
    val spinning = spin && animated && (active || progress > 0f) &&
        LocalLevyraVisualCapabilities.current.decorativeMotion
    if (!spinning) {
        return remember(morph, progress) { LevyraMorphShape(morph, progress) }
    }
    val angle by rememberInfiniteTransition(label = "$label-spin").animateFloat(
        initialValue = 0f,
        targetValue = spinDegrees,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = spinMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "$label-spin-angle"
    )
    val rotation = angle * progress
    return remember(morph, progress, rotation) { LevyraMorphShape(morph, progress, rotation) }
}

@Composable
internal fun rememberLevyraNowPlayingShape(playing: Boolean, animated: Boolean): Shape =
    rememberSpinningMorphShape(
        morph = AvatarPressMorph,
        active = playing,
        animated = animated,
        spin = true,
        spinDegrees = PlaySpinPeriodDegrees,
        spinMillis = NowPlayingSpinPeriodMillis,
        label = "levyra-now-playing-morph"
    )

@Composable
internal fun rememberLevyraAvatarPressShape(pressed: Boolean, animated: Boolean): Shape {
    val progress by animateFloatAsState(
        targetValue = if (pressed && animated) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "levyra-avatar-press-morph"
    )
    return remember(progress) { LevyraMorphShape(AvatarPressMorph, progress) }
}

@Composable
internal fun rememberLevyraFavoriteMorphShape(favorite: Boolean, animated: Boolean): Shape {
    val progress by animateFloatAsState(
        targetValue = if (favorite) 1f else 0f,
        animationSpec = if (animated) {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
        } else {
            snap()
        },
        label = "levyra-favorite-morph"
    )
    return remember(progress) { LevyraMorphShape(FavoriteMorph, progress) }
}
