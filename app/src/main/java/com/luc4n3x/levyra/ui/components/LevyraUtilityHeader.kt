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
import androidx.compose.ui.draw.drawBehind
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
internal fun levyraUtilityColors(seed: Color = LevyraUtilitySeed): LevyraUtilityColors {
    val light = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    return remember(seed, light) {
        val accent = levyraSoftAccent(seed, light)
        val role = levyraTonalRole(seed, light, LevyraUtilityContainerChroma)
        LevyraUtilityColors(
            accent = accent,
            onAccent = levyraOnSoftAccent(seed, light),
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
    scrollProgress: (() -> Float)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clearAndSetSemantics { }
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                alpha = 1f - (scrollProgress?.invoke() ?: 0f)
            }
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.34f * strength), accent.copy(alpha = 0.10f * strength), Color.Transparent),
                        center = Offset(size.width * 0.12f, 0f),
                        radius = size.width * 1.3f
                    )
                )
            }
    )
}

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

internal fun LazyListState.utilityScrollProgress(): Float {
    if (firstVisibleItemIndex > 0) return 1f
    val height = layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: return 0f
    return (firstVisibleItemScrollOffset.toFloat() / height.coerceAtLeast(1)).coerceIn(0f, 1f)
}
private val LevyraUtilityCompactMedallionSize = 60.dp
