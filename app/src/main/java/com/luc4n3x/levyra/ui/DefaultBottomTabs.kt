package com.luc4n3x.levyra.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.indication
import androidx.compose.material.icons.rounded.Search
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.domain.NavDestinations
import com.luc4n3x.levyra.ui.components.LocalDockMorph
import com.luc4n3x.levyra.ui.components.dockClipHeight
import com.luc4n3x.levyra.ui.components.dockFade
import com.luc4n3x.levyra.ui.components.dockMorphTray
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics

private val NavItemTop = 9.dp
private val NavIndicatorHeight = 32.dp
private val NavIndicatorWidth = 56.dp
private val NavIconSize = 24.dp
private val NavIconLabelSpace = 4.dp
private val NavLabelMinFontSize = 9.sp

@Composable
internal fun DefaultBottomTabs(
    selected: LevyraTab,
    compaction: () -> Float,
    onSelect: (LevyraTab) -> Unit,
    barHeight: Dp,
    compactHeight: Dp
) {
    val entries = rememberLevyraBarEntries(NavDestinations.defaultOrder)
    val haptics = LocalLevyraHaptics.current
    val scheme = MaterialTheme.colorScheme
    val tone = com.luc4n3x.levyra.ui.components.levyraUtilityColors(com.luc4n3x.levyra.ui.components.LevyraUtilitySeed)
    val pillHeight = barHeight - com.luc4n3x.levyra.ui.components.DefaultDockPillBottom
    val pillCompactHeight = compactHeight - com.luc4n3x.levyra.ui.components.DefaultDockPillBottom
    val selectedIndex = entries.indexOfFirst { it.tab == selected }
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .dockClipHeight(compaction, pillHeight - pillCompactHeight)
                .fillMaxWidth()
                .height(pillHeight)
                .dockMorphTray(LocalDockMorph.current, entries.size, selectedIndex.coerceAtLeast(0))
        ) {
            Row(Modifier.fillMaxSize().selectableGroup(), verticalAlignment = Alignment.Top) {
                entries.forEach { entry ->
                    val isSelected = entry.tab == selected
                    DefaultTabItem(
                        entry = entry,
                        isSelected = isSelected,
                        indicatorColor = scheme.surfaceContainerHighest,
                        activeIconColor = tone.accent,
                        activeLabelColor = tone.accent,
                        inactiveColor = scheme.onSurfaceVariant,
                        compaction = compaction,
                        onClick = {
                            if (!isSelected) haptics.perform(LevyraHapticAction.SeekSnap)
                            onSelect(entry.tab)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RowScope.DefaultTabItem(
    entry: LevyraTabEntry,
    isSelected: Boolean,
    indicatorColor: Color,
    activeIconColor: Color,
    activeLabelColor: Color,
    inactiveColor: Color,
    compaction: () -> Float,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reveal by navItemReveal(isSelected)
    val pressScale by navItemPressScale(pressed)
    val iconColor by navItemColor(isSelected, activeIconColor, inactiveColor, "nav-icon-color")
    val labelColor by navItemColor(isSelected, activeLabelColor, inactiveColor, "nav-label-color")
    val dockMorph = LocalDockMorph.current
    val indicatorCorner = com.luc4n3x.levyra.ui.components.levyraExpressiveCorner(
        interactionSource = interactionSource,
        rest = NavIndicatorHeight / 2,
        pressed = 10.dp,
        label = "nav-indicator-corner"
    )
    val indicatorShape = androidx.compose.foundation.shape.RoundedCornerShape(indicatorCorner)
    Box(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                selected = isSelected
            }
            .pressable(interactionSource = interactionSource, pressedScale = 0.97f, onClick = onClick),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            Modifier.padding(top = NavItemTop),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NavIconLabelSpace)
        ) {
            Box(Modifier.size(NavIndicatorWidth, NavIndicatorHeight), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            scaleX = reveal
                            alpha = reveal.coerceIn(0f, 1f)
                        }
                        .dockMorphAnchorBackground(if (isSelected) dockMorph else null, indicatorColor, indicatorShape)
                )
                Box(Modifier.matchParentSize().clip(indicatorShape).indication(interactionSource, ripple()))
                Icon(
                    imageVector = if (isSelected) entry.selectedIcon else entry.unselectedIcon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(NavIconSize).graphicsLayer {
                        scaleX = pressScale
                        scaleY = pressScale
                    }
                )
            }
            val labelStyle = MaterialTheme.typography.labelMedium
            androidx.compose.foundation.text.BasicText(
                text = entry.label,
                color = { labelColor },
                style = labelStyle.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, textAlign = TextAlign.Center),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(
                    minFontSize = NavLabelMinFontSize,
                    maxFontSize = labelStyle.fontSize,
                    stepSize = 0.5.sp
                ),
                modifier = Modifier.padding(horizontal = 1.dp).graphicsLayer { alpha = dockFade(compaction()) }
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun navItemReveal(selected: Boolean): State<Float> = animateFloatAsState(
    targetValue = if (selected) 1f else 0f,
    animationSpec = if (LocalAnimationsEnabled.current) MaterialTheme.motionScheme.fastSpatialSpec() else snap(),
    label = "nav-indicator-reveal"
)

@Composable
private fun navItemPressScale(pressed: Boolean): State<Float> = animateFloatAsState(
    targetValue = if (pressed) 0.9f else 1f,
    animationSpec = if (LocalAnimationsEnabled.current) spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium) else snap(),
    label = "nav-item-press"
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun navItemColor(selected: Boolean, active: Color, inactive: Color, label: String): State<Color> = animateColorAsState(
    targetValue = if (selected) active else inactive,
    animationSpec = if (LocalAnimationsEnabled.current) MaterialTheme.motionScheme.fastEffectsSpec() else snap(),
    label = label
)

internal fun defaultDockSearchSlot(
    selected: Boolean,
    compaction: () -> Float,
    onClick: () -> Unit
): (@Composable () -> Unit)? =
    if (com.luc4n3x.levyra.ui.theme.LevyraIsDefaultSkin) {
        { DefaultDockSearchButton(selected, compaction, onClick) }
    } else {
        null
    }

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DefaultDockSearchButton(selected: Boolean, compaction: () -> Float, onClick: () -> Unit) {
    val tone = com.luc4n3x.levyra.ui.components.levyraUtilityColors(com.luc4n3x.levyra.ui.components.LevyraUtilitySeed)
    val strings = com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings.current
    val haptics = LocalLevyraHaptics.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by navItemPressScale(pressed)
    val reveal by navItemReveal(selected)
    Box(
        Modifier
            .layout { measurable, _ ->
                val full = com.luc4n3x.levyra.ui.components.DefaultDockSearchSize.roundToPx()
                val compact = com.luc4n3x.levyra.ui.components.DefaultDockSearchCompactSize.roundToPx()
                val side = (full + (compact - full) * compaction().coerceIn(0f, 1f)).toInt()
                val placeable = measurable.measure(androidx.compose.ui.unit.Constraints.fixed(side, side))
                layout(side, side) { placeable.place(0, 0) }
            }
            .semantics {
                role = Role.Tab
                this.selected = selected
            }
            .pressable(interactionSource = interactionSource, pressedScale = 0.97f) {
                if (!selected) haptics.perform(LevyraHapticAction.SeekSnap)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        val searchShape = androidx.compose.material3.MaterialShapes.Cookie9Sided.toShape()
        Box(
            Modifier
                .fillMaxSize()
                .padding(8.dp)
                .graphicsLayer {
                    scaleX = reveal
                    scaleY = reveal
                    rotationZ = reveal * 40f
                    alpha = reveal.coerceIn(0f, 1f)
                }
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, searchShape)
        )
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Rounded.Search,
            contentDescription = strings.search,
            tint = if (selected) tone.accent else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(28.dp).graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
        )
    }
}
