package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraType

private const val HomeHeaderFadeShare = 0.55f
private const val HomeHeaderParallax = 0.35f

internal fun homeHeaderCollapse(scrollOffsetPx: Float, headerHeightPx: Float): Float {
    if (headerHeightPx <= 0f) return 0f
    return (scrollOffsetPx / (headerHeightPx * HomeHeaderFadeShare)).coerceIn(0f, 1f)
}

internal fun Modifier.homeHeaderParallax(state: LazyListState): Modifier = graphicsLayer {
    if (state.firstVisibleItemIndex != 0) {
        alpha = 0f
        return@graphicsLayer
    }
    val offset = state.firstVisibleItemScrollOffset.toFloat()
    val collapse = homeHeaderCollapse(offset, size.height)
    alpha = 1f - collapse
    translationY = offset * HomeHeaderParallax
    val scale = 1f - 0.04f * collapse
    scaleX = scale
    scaleY = scale
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun HomeCollapsedTopBar(
    listState: LazyListState,
    statusBarTop: Dp,
    userName: String,
    canvas: Color,
    onSearch: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val density = LocalDensity.current
    val animated = LocalAnimationsEnabled.current
    val greetingHour by rememberCurrentGreetingHour()
    val barHeight = LevyraHomeDesign.SettingsControlHeight + HOME_COLLAPSED_BAR_PADDING * 2
    val revealDistancePx = with(density) { barHeight.toPx() }
    val collapsed by remember(listState, revealDistancePx) {
        derivedStateOf {
            val first = listState.layoutInfo.visibleItemsInfo.firstOrNull() ?: return@derivedStateOf false
            val offset = listState.firstVisibleItemScrollOffset
            first.index > 0 || (offset > 0 && first.size - offset < revealDistancePx)
        }
    }
    val reveal by animateFloatAsState(
        targetValue = if (collapsed) 1f else 0f,
        animationSpec = if (animated) LevyraMotion.settle.spec() else snap(),
        label = "home-collapsed-bar"
    )
    if (reveal <= 0f) return
    val title = remember(strings, userName, greetingHour) { strings.formatGreeting(userName, greetingHour) }
    val solidShare = (statusBarTop + barHeight) / (statusBarTop + barHeight + HOME_COLLAPSED_BAR_FADE)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(statusBarTop + barHeight + HOME_COLLAPSED_BAR_FADE)
            .graphicsLayer { alpha = reveal }
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to canvas,
                        solidShare to canvas,
                        1f to canvas.copy(alpha = 0f)
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .padding(top = statusBarTop)
                .height(barHeight)
                .fillMaxWidth()
                .padding(horizontal = LevyraHomeDesign.HorizontalInset)
                .graphicsLayer { translationY = (1f - reveal) * -HOME_COLLAPSED_BAR_SLIDE.toPx() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeLogoBadge()
            Text(
                text = title,
                color = LevyraText,
                style = LevyraType.sectionTitle.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.4).sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() }
            )
            HomeHeaderIconButton(
                icon = Icons.Rounded.Search,
                contentDescription = strings.search,
                polygon = MaterialShapes.Cookie4Sided,
                onClick = onSearch
            )
        }
    }
}

private val HOME_COLLAPSED_BAR_PADDING = 8.dp
private val HOME_COLLAPSED_BAR_FADE = 20.dp
private val HOME_COLLAPSED_BAR_SLIDE = 12.dp
