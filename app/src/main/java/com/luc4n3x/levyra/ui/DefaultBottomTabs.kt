package com.luc4n3x.levyra.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.domain.NavDestinations
import com.luc4n3x.levyra.ui.components.LocalDockMorph
import com.luc4n3x.levyra.ui.components.dockClipHeight
import com.luc4n3x.levyra.ui.components.dockFade
import com.luc4n3x.levyra.ui.components.dockMorphTray
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics

private val NavItemTop = 12.dp
private val NavIndicatorHeight = 32.dp
private val NavIndicatorWidth = 56.dp
private val NavIconSize = 24.dp
private val NavIconLabelSpace = 4.dp
private const val NavIndicatorAlpha = 0.12f

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
    val selectedIndex = entries.indexOfFirst { it.tab == selected }
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .dockClipHeight(compaction, barHeight - compactHeight)
                .fillMaxWidth()
                .height(barHeight)
                .dockMorphTray(LocalDockMorph.current, entries.size, selectedIndex.coerceAtLeast(0))
        ) {
            Row(Modifier.fillMaxSize().selectableGroup(), verticalAlignment = Alignment.Top) {
                entries.forEach { entry ->
                    val isSelected = entry.tab == selected
                    DefaultTabItem(
                        entry = entry,
                        isSelected = isSelected,
                        indicatorColor = scheme.onSurface.copy(alpha = NavIndicatorAlpha),
                        activeIconColor = scheme.onSurface,
                        activeLabelColor = scheme.onSurface,
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
        Spacer(Modifier.navigationBarsPadding())
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
    val animationsEnabled = LocalAnimationsEnabled.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val motion = MaterialTheme.motionScheme
    val reveal by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = if (animationsEnabled) motion.fastSpatialSpec() else snap(),
        label = "nav-indicator-reveal"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = if (animationsEnabled) spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium) else snap(),
        label = "nav-item-press"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) activeIconColor else inactiveColor,
        animationSpec = if (animationsEnabled) motion.fastEffectsSpec() else snap(),
        label = "nav-icon-color"
    )
    val labelColor by animateColorAsState(
        targetValue = if (isSelected) activeLabelColor else inactiveColor,
        animationSpec = if (animationsEnabled) motion.fastEffectsSpec() else snap(),
        label = "nav-label-color"
    )
    val dockMorph = LocalDockMorph.current
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
                        .dockMorphAnchorBackground(if (isSelected) dockMorph else null, indicatorColor, CircleShape)
                )
                Box(Modifier.matchParentSize().clip(CircleShape).indication(interactionSource, ripple()))
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
            Text(
                text = entry.label,
                color = labelColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 3.dp).graphicsLayer { alpha = dockFade(compaction()) }
            )
        }
    }
}
