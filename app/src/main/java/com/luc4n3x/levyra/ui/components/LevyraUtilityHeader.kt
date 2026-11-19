package com.luc4n3x.levyra.ui.components

import androidx.compose.foundation.lazy.LazyListState
import com.luc4n3x.levyra.ui.theme.levyraTonalRole
import com.luc4n3x.levyra.ui.theme.levyraOnSoftAccent
import com.luc4n3x.levyra.ui.theme.levyraSoftAccent
import androidx.compose.ui.graphics.luminance
import androidx.compose.runtime.remember
import androidx.compose.runtime.Immutable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraSoftAccentsActive
import com.luc4n3x.levyra.ui.theme.LevyraIsAppleMusic
import com.luc4n3x.levyra.ui.theme.LevyraIsAtelier
import com.luc4n3x.levyra.ui.theme.LevyraIsIvory
import com.luc4n3x.levyra.ui.theme.LevyraIsMono
import com.luc4n3x.levyra.ui.theme.LevyraIsNocturne
import com.luc4n3x.levyra.ui.theme.LevyraIsPrism
import com.luc4n3x.levyra.ui.theme.LevyraIsQobuz
import com.luc4n3x.levyra.ui.theme.LevyraIsSpotify
import com.luc4n3x.levyra.ui.theme.LevyraIsTorque
import com.luc4n3x.levyra.ui.theme.LevyraIsYouTubeMusic

internal val LevyraUtilitySeed = Color(0xFF8ECAE6)
internal val LevyraPastelLavender = Color(0xFFB9A2F5)
internal val LevyraPastelRose = Color(0xFFF2A4C4)
internal val LevyraPastelPeriwinkle = Color(0xFF9FB2FF)
internal val LevyraPastelAmber = Color(0xFFF4C27A)

@Immutable
internal data class LevyraUtilityColors(
    val accent: Color,
    val onAccent: Color,
    val container: Color,
    val onContainer: Color,
    val softContainer: Color
)

@Composable
internal fun levyraUtilityColors(seed: Color = LevyraCyan): LevyraUtilityColors {
    val light = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val source = if (LevyraSoftAccentsActive) seed else LevyraCyan
    return remember(source, light) {
        val accent = levyraSoftAccent(source, light)
        val role = levyraTonalRole(source, light, LevyraUtilityContainerChroma)
        LevyraUtilityColors(
            accent = accent,
            onAccent = levyraOnSoftAccent(source, light),
            container = role.container,
            onContainer = role.onContainer,
            softContainer = accent.copy(alpha = LevyraUtilitySoftAlpha)
        )
    }
}

private const val LevyraUtilityContainerChroma = 24.0
private const val LevyraUtilitySoftAlpha = 0.16f

internal val LevyraUtilityStyleActive: Boolean
    get() = !(LevyraIsAtelier || LevyraIsMono || LevyraIsAppleMusic || LevyraIsYouTubeMusic || LevyraIsQobuz || LevyraIsSpotify ||
        LevyraIsTorque || LevyraIsIvory || LevyraIsNocturne || LevyraIsPrism)

@Composable
internal fun LevyraUtilityBackdrop(
    modifier: Modifier = Modifier,
    accent: Color = LevyraCyan,
    strength: Float = 0.5f,
    listState: LazyListState? = null
) {
    val tint = lerp(accent, MaterialTheme.colorScheme.outline, LevyraBackdropMute)
    val density = LocalDensity.current
    val fadeDistance = with(density) { LevyraBackdropFadeDistance.toPx() }
    val washHeight = with(density) { LevyraBackdropWashHeight.toPx() }
    val fade = remember(listState, fadeDistance) { listState?.let { LevyraBackdropScrollFade(it, fadeDistance) } }
    Box(
        modifier = modifier
            .fillMaxSize()
            .clearAndSetSemantics { }
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                alpha = fade?.visibility() ?: 1f
            }
            .drawWithCache {
                val wash = Brush.verticalGradient(
                    0f to tint.copy(alpha = 0.30f * strength),
                    0.5f to tint.copy(alpha = 0.10f * strength),
                    1f to Color.Transparent,
                    endY = washHeight
                )
                val light = Brush.radialGradient(
                    listOf(tint.copy(alpha = 0.34f * strength), Color.Transparent),
                    center = if (layoutDirection == LayoutDirection.Rtl) Offset(size.width, 0f) else Offset.Zero,
                    radius = washHeight * 1.1f
                )
                onDrawBehind {
                    drawRect(wash, size = size.copy(height = washHeight))
                    drawRect(light)
                }
            }
    )
}

private const val LevyraBackdropMute = 0.45f
private val LevyraBackdropWashHeight = 380.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LevyraUtilityHeader(
    title: String,
    subtitle: String,
    icon: ImageVector?,
    onBack: () -> Unit,
    backDescription: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    medallionColor: Color = levyraUtilityColors().softContainer,
    medallionContentColor: Color = levyraUtilityColors().accent
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LevyraBackButton(
                onClick = onBack,
                contentDescription = backDescription,
                size = 48.dp,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = scheme.onSurface
                )
            )
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(if (compact) LevyraUtilityCompactMedallionSize else LevyraUtilityMedallionSize)
                        .background(medallionColor, MaterialShapes.Cookie9Sided.toShape())
                        .clearAndSetSemantics { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = medallionContentColor, modifier = Modifier.size(if (compact) 28.dp else 34.dp))
                }
            }
        }
        Spacer(Modifier.height(if (compact) 12.dp else 20.dp))
        Text(
            title,
            color = scheme.onBackground,
            style = if (compact) MaterialTheme.typography.headlineLargeEmphasized else MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() }
        )
        if (subtitle.isNotBlank()) {
            Text(
                subtitle,
                color = scheme.onSurfaceVariant,
                style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge
            )
        }
    }
}

private val LevyraUtilityMedallionSize = 76.dp

private class LevyraBackdropScrollFade(private val state: LazyListState, private val distance: Float) {
    private val leadingSizes = IntArray(LevyraBackdropTrackedItems)

    fun visibility(): Float {
        state.layoutInfo.visibleItemsInfo.forEach { item ->
            if (item.index < leadingSizes.size) leadingSizes[item.index] = item.size
        }
        val index = state.firstVisibleItemIndex
        if (index >= leadingSizes.size) return 0f
        var scrolled = state.firstVisibleItemScrollOffset.toFloat()
        for (i in 0 until index) {
            if (leadingSizes[i] == 0) return 0f
            scrolled += leadingSizes[i]
        }
        val progress = (scrolled / distance.coerceAtLeast(1f)).coerceIn(0f, 1f)
        return 1f - progress * progress * (3f - 2f * progress)
    }
}

private const val LevyraBackdropTrackedItems = 12
private val LevyraBackdropFadeDistance = 420.dp
private val LevyraUtilityCompactMedallionSize = 60.dp
