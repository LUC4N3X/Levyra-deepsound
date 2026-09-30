package com.luc4n3x.levyra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.GlassBackdropState
import com.luc4n3x.levyra.ui.LocalGlassBackdrop
import com.luc4n3x.levyra.ui.glassSurface
import com.luc4n3x.levyra.ui.playerMix
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign

val LocalLevyraLiquidGlassEnabled = staticCompositionLocalOf { false }

enum class LevyraGlassIntensity {
    Subtle,
    Standard,
    Strong
}

@Immutable
data class LevyraGlassTokens(
    val blurRadius: Dp,
    val tintAlpha: Float,
    val fallbackTintAlpha: Float,
    val borderAlpha: Float,
    val highlightAlpha: Float
)

object LevyraGlassDefaults {
    val Subtle = LevyraGlassTokens(
        blurRadius = 18.dp,
        tintAlpha = 0.16f,
        fallbackTintAlpha = 0.24f,
        borderAlpha = 0.26f,
        highlightAlpha = 0.10f
    )
    val Standard = LevyraGlassTokens(
        blurRadius = 24.dp,
        tintAlpha = 0.20f,
        fallbackTintAlpha = 0.30f,
        borderAlpha = 0.34f,
        highlightAlpha = 0.14f
    )
    val Strong = LevyraGlassTokens(
        blurRadius = 30.dp,
        tintAlpha = 0.26f,
        fallbackTintAlpha = 0.38f,
        borderAlpha = 0.42f,
        highlightAlpha = 0.18f
    )

    fun tokens(intensity: LevyraGlassIntensity): LevyraGlassTokens = when (intensity) {
        LevyraGlassIntensity.Subtle -> Subtle
        LevyraGlassIntensity.Standard -> Standard
        LevyraGlassIntensity.Strong -> Strong
    }
}

fun Modifier.levyraGlass(
    shape: Shape,
    baseTint: Color,
    fallbackColor: Color,
    fallbackBorderColor: Color,
    intensity: LevyraGlassIntensity = LevyraGlassIntensity.Standard,
    artworkTint: Color? = null,
    borderWidth: Dp = LevyraPlayerDesign.Hairline
): Modifier = composed {
    if (!LocalLevyraLiquidGlassEnabled.current) {
        return@composed this
            .background(fallbackColor, shape)
            .border(borderWidth, fallbackBorderColor, shape)
    }
    this.resolvedLevyraGlass(
        backdrop = LocalGlassBackdrop.current,
        shape = shape,
        baseTint = baseTint,
        fallbackBorderColor = fallbackBorderColor,
        intensity = intensity,
        artworkTint = artworkTint,
        borderWidth = borderWidth
    )
}

internal fun Modifier.resolvedLevyraGlass(
    backdrop: GlassBackdropState?,
    shape: Shape,
    baseTint: Color,
    fallbackBorderColor: Color,
    intensity: LevyraGlassIntensity,
    artworkTint: Color?,
    borderWidth: Dp
): Modifier {
    val tokens = LevyraGlassDefaults.tokens(intensity)
    val harmonizedTint = artworkTint
        ?.let { baseTint.playerMix(it, 0.10f) }
        ?: baseTint
    val glassTint = harmonizedTint.copy(alpha = tokens.tintAlpha)
    val lightweightFallback = harmonizedTint.copy(alpha = tokens.fallbackTintAlpha)
    val highlight = Color.White.copy(alpha = tokens.highlightAlpha)
    val border = fallbackBorderColor.playerMix(highlight, 0.45f)

    return if (backdrop?.enabled == true && backdrop.layer != null) {
        this.glassSurface(
            state = backdrop,
            shape = shape,
            tint = glassTint,
            fallbackColor = lightweightFallback,
            borderColor = border.copy(alpha = tokens.borderAlpha.coerceAtLeast(border.alpha)),
            blurRadius = tokens.blurRadius,
            borderWidth = borderWidth
        )
    } else {
        this
            .background(lightweightFallback, shape)
            .border(borderWidth, border, shape)
    }
}

@Composable
fun LevyraGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape,
    baseTint: Color,
    fallbackColor: Color,
    fallbackBorderColor: Color,
    intensity: LevyraGlassIntensity = LevyraGlassIntensity.Standard,
    artworkTint: Color? = null,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.levyraGlass(
            shape = shape,
            baseTint = baseTint,
            fallbackColor = fallbackColor,
            fallbackBorderColor = fallbackBorderColor,
            intensity = intensity,
            artworkTint = artworkTint
        ),
        contentAlignment = contentAlignment,
        content = content
    )
}

@Composable
fun LevyraGlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = LevyraPlayerDesign.HeaderButton,
    iconSize: Dp = 21.dp,
    tint: Color = LevyraPlayerDesign.TextPrimary,
    baseTint: Color = LevyraPlayerDesign.GlassFill,
    fallbackColor: Color = LevyraPlayerDesign.GlassFill,
    fallbackBorderColor: Color = LevyraPlayerDesign.GlassBorderTop
        .playerMix(LevyraPlayerDesign.GlassBorderBottom, 0.5f),
    artworkTint: Color? = null,
    intensity: LevyraGlassIntensity = LevyraGlassIntensity.Strong,
    shape: Shape = CircleShape,
    enabled: Boolean = true
) {
    SpringIconButton(
        onClick = onClick,
        modifier = modifier.sizeIn(
            minWidth = maxOf(size, 40.dp),
            minHeight = LevyraPlayerDesign.MinimumTouchTarget
        ),
        enabled = enabled,
        pressedScale = 0.94f,
        contentDescription = contentDescription
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .levyraGlass(
                    shape = shape,
                    baseTint = baseTint,
                    fallbackColor = fallbackColor,
                    fallbackBorderColor = fallbackBorderColor,
                    intensity = intensity,
                    artworkTint = artworkTint
                ),
            contentAlignment = Alignment.Center
        ) {
            PlayerIcon(icon = icon, tint = tint, modifier = Modifier.size(iconSize))
        }
    }
}

@Composable
fun LevyraGlassPill(
    modifier: Modifier = Modifier,
    shape: Shape,
    baseTint: Color,
    fallbackColor: Color,
    fallbackBorderColor: Color,
    artworkTint: Color? = null,
    intensity: LevyraGlassIntensity = LevyraGlassIntensity.Standard,
    content: @Composable BoxScope.() -> Unit
) {
    LevyraGlassSurface(
        modifier = modifier,
        shape = shape,
        baseTint = baseTint,
        fallbackColor = fallbackColor,
        fallbackBorderColor = fallbackBorderColor,
        intensity = intensity,
        artworkTint = artworkTint,
        contentAlignment = Alignment.Center,
        content = content
    )
}
