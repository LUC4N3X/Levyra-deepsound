package com.luc4n3x.levyra.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.domain.NavBackground
import com.luc4n3x.levyra.domain.NavIndicator
import com.luc4n3x.levyra.domain.NavLabelMode
import com.luc4n3x.levyra.domain.NavStyle
import com.luc4n3x.levyra.domain.NavTuning
import com.luc4n3x.levyra.ui.components.dockClipHeight
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import kotlin.math.abs

private val StyledNavCompactShrink = 18.dp
private val StyledNavMinHeight = 48.dp
private val StyledNavIndicatorHeight = 32.dp
private val StyledNavFloatingGutter = 16.dp
private val StyledNavFloatingBottom = 8.dp

@Composable
internal fun StyledDockSurface(
    style: NavStyle,
    tuning: NavTuning,
    glass: GlassBackdropState?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    if (style == NavStyle.FloatingDock) {
        Column(modifier = modifier, content = content)
        return
    }
    val scheme = MaterialTheme.colorScheme
    val shape = remember {
        RoundedCornerShape(topStart = LevyraPlayerDesign.DockTrayCorner, topEnd = LevyraPlayerDesign.DockTrayCorner)
    }
    Column(
        modifier = modifier
            .clip(shape)
            .navBackground(tuning.background, scheme.surfaceContainer, scheme.surfaceContainerHigh, glass)
            .border(LevyraPlayerDesign.Hairline, scheme.outlineVariant.copy(alpha = 0.35f), shape),
        content = content
    )
}

private fun Modifier.navBackground(
    background: NavBackground,
    base: Color,
    raised: Color,
    glass: GlassBackdropState?
): Modifier = when (background) {
    NavBackground.Solid -> background(base)
    NavBackground.Gradient -> background(Brush.verticalGradient(listOf(base.copy(alpha = 0.82f), raised)))
    NavBackground.Translucent -> if (glass != null) {
        glassFrost(
            state = glass,
            blurRadius = 24.dp,
            onDrawFrosted = { drawRect(base.copy(alpha = 0.62f)) },
            onDrawFallback = { drawRect(base.copy(alpha = 0.94f)) }
        )
    } else {
        background(base.copy(alpha = 0.80f))
    }
}

@Composable
internal fun StyledBottomTabs(
    style: NavStyle,
    tuning: NavTuning,
    selected: LevyraTab,
    compaction: () -> Float,
    onSelect: (LevyraTab) -> Unit,
    glass: GlassBackdropState?,
    applySystemInset: Boolean = true
) {
    val allEntries = rememberLevyraTabEntries()
    val entries = remember(allEntries, tuning.order, tuning.hidden) {
        tuning.visibleOrder().mapNotNull { tab -> allEntries.firstOrNull { it.tab == tab } }
    }
    if (entries.isEmpty()) return
    val scheme = MaterialTheme.colorScheme
    val motion = LocalAnimationsEnabled.current && tuning.expressiveMotion
    val haptics = LocalLevyraHaptics.current
    val selectedIndex = entries.indexOfFirst { it.tab == selected }
    val indicatorPosition by animateFloatAsState(
        targetValue = selectedIndex.coerceAtLeast(0).toFloat(),
        animationSpec = if (LocalAnimationsEnabled.current) {
            if (motion) {
                spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)
            } else {
                spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
            }
        } else {
            snap()
        },
        label = "styled-nav-indicator"
    )
    val height = tuning.heightDp.dp
    val compactHeight = (height - StyledNavCompactShrink).coerceAtLeast(StyledNavMinHeight)
    val floating = style == NavStyle.FloatingDock
    val capsuleShape = RoundedCornerShape(height / 2)
    val indicatorColor = scheme.secondaryContainer
    val stretchFactor = tuning.intensity / 100f
    val labelVisible = tuning.labels != NavLabelMode.Never
    val labelRoom = if (labelVisible) 16.dp else 0.dp
    val iconTop = ((height - StyledNavIndicatorHeight - labelRoom) / 2).coerceAtLeast(4.dp)

    Column(modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .then(
                    if (floating) {
                        Modifier.padding(
                            start = StyledNavFloatingGutter,
                            end = StyledNavFloatingGutter,
                            bottom = StyledNavFloatingBottom
                        )
                    } else {
                        Modifier
                    }
                )
                .dockClipHeight(compaction, height - compactHeight)
                .fillMaxWidth()
                .height(height)
                .then(
                    if (floating) {
                        Modifier
                            .shadow(
                                elevation = (6f + 14f * stretchFactor).dp,
                                shape = capsuleShape,
                                ambientColor = Color.Black.copy(alpha = 0.22f),
                                spotColor = Color.Black.copy(alpha = 0.34f)
                            )
                            .clip(capsuleShape)
                            .navBackground(tuning.background, scheme.surfaceContainerHigh, scheme.surfaceContainerHighest, glass)
                            .border(LevyraPlayerDesign.Hairline, scheme.outlineVariant.copy(alpha = 0.45f), capsuleShape)
                    } else {
                        Modifier
                    }
                )
        ) {
            val slotWidth = maxWidth / entries.size
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        if (selectedIndex < 0) return@drawBehind
                        drawNavIndicator(
                            indicator = tuning.indicator,
                            slotPx = slotWidth.toPx(),
                            position = indicatorPosition,
                            selectedIndex = selectedIndex,
                            lastIndex = entries.lastIndex,
                            iconTopPx = iconTop.toPx(),
                            labelRoomPx = labelRoom.toPx(),
                            stretchFactor = stretchFactor,
                            pillColor = indicatorColor,
                            markColor = scheme.primary
                        )
                    }
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .selectableGroup(),
                verticalAlignment = Alignment.Top
            ) {
                entries.forEach { entry ->
                    val isSelected = entry.tab == selected
                    StyledTabButton(
                        entry = entry,
                        isSelected = isSelected,
                        tuning = tuning,
                        iconTop = iconTop,
                        motion = motion,
                        onIndicator = tuning.indicator == NavIndicator.Pill || tuning.indicator == NavIndicator.Morph,
                        onClick = {
                            if (!isSelected) {
                                haptics.perform(if (motion) LevyraHapticAction.Confirm else LevyraHapticAction.SeekSnap)
                            }
                            onSelect(entry.tab)
                        }
                    )
                }
            }
        }
        if (applySystemInset) {
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

private fun DrawScope.drawNavIndicator(
    indicator: NavIndicator,
    slotPx: Float,
    position: Float,
    selectedIndex: Int,
    lastIndex: Int,
    iconTopPx: Float,
    labelRoomPx: Float,
    stretchFactor: Float,
    pillColor: Color,
    markColor: Color
) {
    val travel = abs(position - selectedIndex).coerceAtMost(1f)
    val centerSlot = if (layoutDirection == LayoutDirection.Rtl) lastIndex - position else position
    val centerX = slotPx * centerSlot + slotPx / 2f
    val pillHeight = StyledNavIndicatorHeight.toPx()
    when (indicator) {
        NavIndicator.Pill, NavIndicator.Morph -> {
            val base = (slotPx - 20.dp.toPx()).coerceIn(48.dp.toPx(), 64.dp.toPx())
            val stretch = if (indicator == NavIndicator.Morph) slotPx * 0.55f * travel * stretchFactor else 0f
            val width = base + stretch
            drawRoundRect(
                color = pillColor,
                topLeft = Offset(centerX - width / 2f, iconTopPx),
                size = Size(width, pillHeight),
                cornerRadius = CornerRadius(pillHeight / 2f)
            )
        }
        NavIndicator.Dot -> drawCircle(
            color = markColor,
            radius = 2.5.dp.toPx(),
            center = Offset(centerX, iconTopPx + pillHeight + labelRoomPx + 5.dp.toPx())
        )
        NavIndicator.Line -> {
            val width = 22.dp.toPx() + 18.dp.toPx() * travel * stretchFactor
            val lineHeight = 3.dp.toPx()
            drawRoundRect(
                color = markColor,
                topLeft = Offset(centerX - width / 2f, size.height - lineHeight - 6.dp.toPx()),
                size = Size(width, lineHeight),
                cornerRadius = CornerRadius(lineHeight / 2f)
            )
        }
    }
}

@Composable
private fun RowScope.StyledTabButton(
    entry: LevyraTabEntry,
    isSelected: Boolean,
    tuning: NavTuning,
    iconTop: Dp,
    motion: Boolean,
    onIndicator: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val selectedTint = if (onIndicator) scheme.onSecondaryContainer else scheme.primary
    val idleTint = scheme.onSurfaceVariant
    val tint by animateColorAsState(
        targetValue = if (isSelected) selectedTint else idleTint,
        animationSpec = if (LocalAnimationsEnabled.current) tween(200) else snap(),
        label = "styled-tab-tint"
    )
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.86f
            isSelected && motion -> 1.08f
            else -> 1f
        },
        animationSpec = if (motion) {
            spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)
        } else {
            snap()
        },
        label = "styled-tab-scale"
    )
    val showLabel = when (tuning.labels) {
        NavLabelMode.Always -> true
        NavLabelMode.ActiveOnly -> isSelected
        NavLabelMode.Never -> false
    }
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                selected = isSelected
                contentDescription = entry.label
            }
            .levyraPressable(
                onClick = onClick,
                interactionSource = interactionSource,
                pressedScale = 0.94f
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.padding(top = iconTop),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(
                modifier = Modifier.height(StyledNavIndicatorHeight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) entry.selectedIcon else entry.unselectedIcon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier
                        .size(tuning.iconDp.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                )
            }
            if (tuning.labels != NavLabelMode.Never) {
                Text(
                    text = entry.label,
                    color = tint,
                    fontSize = 11.sp,
                    lineHeight = LevyraTypeRhythm.lineHeight(11.sp),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .height(14.dp)
                        .graphicsLayer { alpha = if (showLabel) 1f else 0f }
                )
            }
        }
    }
}
