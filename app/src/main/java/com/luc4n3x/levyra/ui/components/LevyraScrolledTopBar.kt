package com.luc4n3x.levyra.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.GlassBackdropState
import com.luc4n3x.levyra.ui.glassFrost

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LevyraScrolledTopBar(
    glass: GlassBackdropState,
    listState: LazyListState,
    title: String,
    icon: ImageVector? = null,
    seed: Color = LevyraUtilitySeed,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val threshold = with(LocalDensity.current) { LevyraScrolledTopBarThreshold.toPx() }
    val collapsed by remember(listState, threshold) {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > threshold }
    }
    val visibility by animateFloatAsState(
        if (collapsed) 1f else 0f,
        animationSpec = if (com.luc4n3x.levyra.ui.LocalAnimationsEnabled.current) androidx.compose.animation.core.spring() else androidx.compose.animation.core.snap(),
        label = "levyra-scrolled-top-bar"
    )
    androidx.compose.runtime.SideEffect { glass.sampling = collapsed || visibility > 0f }
    val colors = MaterialTheme.colorScheme
    val tone = levyraUtilityColors(seed)
    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = visibility
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.verticalGradient(0f to Color.Black, 0.78f to Color.Black, 1f to Color.Transparent),
                    blendMode = BlendMode.DstIn
                )
            }
            .glassFrost(
                state = glass,
                blurRadius = 24.dp,
                onDrawFrosted = {
                    drawRect(colors.surfaceContainer.copy(alpha = 0.72f))
                    drawRect(Brush.horizontalGradient(listOf(tone.accent.copy(alpha = 0.10f), Color.Transparent)))
                },
                onDrawFallback = { drawRect(colors.surfaceContainer) }
            )
            .statusBarsPadding()
            .padding(bottom = LevyraScrolledTopBarFade)
            .then(if (collapsed) Modifier else Modifier.clearAndSetSemantics { })
    ) {
        Row(
            Modifier.fillMaxWidth().height(LevyraScrolledTopBarHeight).padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (icon != null) {
                Box(
                    Modifier.size(34.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(tone.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = tone.onAccent, modifier = Modifier.size(18.dp))
                }
            }
            Text(
                title,
                color = colors.onSurface,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            if (collapsed) actions() else Spacer(Modifier.size(0.dp))
        }
    }
}

private val LevyraScrolledTopBarHeight = 60.dp
private val LevyraScrolledTopBarFade = 14.dp
private val LevyraScrolledTopBarThreshold = 72.dp
