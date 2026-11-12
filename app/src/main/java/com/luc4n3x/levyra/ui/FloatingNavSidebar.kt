package com.luc4n3x.levyra.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material.icons.rounded.ViewSidebar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.FloatingNavSide
import com.luc4n3x.levyra.domain.LevyraFloatingNavSettings
import com.luc4n3x.levyra.domain.LevyraInterfaceSettings
import com.luc4n3x.levyra.domain.LevyraNavigationStudioSettings
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.domain.NavStyle
import com.luc4n3x.levyra.ui.components.levyraMarquee
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.floatingNav
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val SidebarRailWidth = 84.dp
private val SidebarHandleWidth = 56.dp
private val SidebarItemHeight = 60.dp
private val SidebarItemGap = 4.dp
private val SidebarItemInset = 6.dp
private val SidebarEdgeGap = 12.dp
private val SidebarBandMargin = 12.dp
private val SidebarGripWidth = 20.dp
private val SidebarGripHeight = 4.dp
private val SidebarTouchTarget = 48.dp
private val SidebarCurrentBadge = 40.dp
private val SidebarIndicatorCorner = 20.dp
private val SidebarRestElevation = 10.dp
private val SidebarLiftElevation = 22.dp
private const val SidebarSurfaceAlpha = 0.97f
private const val SidebarLiftScale = 1.04f
private const val SidebarAccessibilityStep = 0.12f

private class SidebarGeometry {
    var width = 0f
    var leftX = 0f
    var rightX = 0f
    var panelWidth = 0f
    var travel = 0f

    fun baseX(side: FloatingNavSide): Float = if (side == FloatingNavSide.Left) leftX else rightX
}

@Composable
internal fun FloatingNavSidebar(
    settings: LevyraFloatingNavSettings,
    navigation: LevyraNavigationStudioSettings,
    selected: LevyraTab,
    visible: Boolean,
    bottomClearancePx: () -> Int,
    onSelect: (LevyraTab) -> Unit,
    onSettingsChange: (LevyraFloatingNavSettings) -> Unit
) {
    val tuning = navigation.activeTuning()
    val entries = rememberLevyraBarEntries(tuning.visibleOrder())
    if (entries.isEmpty()) return
    val colors = rememberNavColors(NavStyle.FloatingDock, tuning)
    val backdrop = MaterialTheme.colorScheme.background
    val surface = remember(colors.surface, backdrop) {
        colors.surface.compositeOver(backdrop.copy(alpha = 1f)).copy(alpha = SidebarSurfaceAlpha)
    }
    val copy = LocalLevyraStrings.current.floatingNav()
    val animated = LocalAnimationsEnabled.current
    val haptics = LocalLevyraHaptics.current
    val gestureAnimated by rememberUpdatedState(animated)
    val gestureHaptics by rememberUpdatedState(haptics)
    val scope = rememberCoroutineScope()
    val persist by rememberUpdatedState(onSettingsChange)
    val latestSettings by rememberUpdatedState(settings)

    var expanded by rememberSaveable { mutableStateOf(false) }
    var side by remember { mutableStateOf(settings.side) }
    val anchor = remember { mutableFloatStateOf(settings.anchor) }
    val offsetX = remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val geometry = remember { SidebarGeometry() }

    LaunchedEffect(settings.side) { side = settings.side }
    LaunchedEffect(settings.anchor) { if (!dragging) anchor.floatValue = settings.anchor }
    BackHandler(enabled = visible && expanded) { expanded = false }

    fun save() = persist(latestSettings.copy(side = side, anchor = anchor.floatValue))

    fun nudge(step: Float) {
        anchor.floatValue = (anchor.floatValue + step).coerceIn(0f, 1f)
        save()
    }

    fun switchSide() {
        side = side.opposite
        haptics.perform(LevyraHapticAction.Reorder)
        save()
    }

    fun settle() {
        if (!dragging) return
        dragging = false
        val from = geometry.baseX(side)
        val center = from + offsetX.floatValue + geometry.panelWidth / 2f
        val target = if (center < geometry.width / 2f) FloatingNavSide.Left else FloatingNavSide.Right
        if (target != side) {
            offsetX.floatValue = from + offsetX.floatValue - geometry.baseX(target)
            side = target
            gestureHaptics.perform(LevyraHapticAction.Reorder)
        }
        save()
        val start = offsetX.floatValue
        settleJob = scope.launch {
            animate(
                initialValue = start,
                targetValue = 0f,
                animationSpec = if (gestureAnimated) spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow) else snap()
            ) { value, _ -> offsetX.floatValue = value }
        }
    }

    LaunchedEffect(visible) {
        if (!visible) {
            expanded = false
            settle()
        }
    }

    val lift by animateFloatAsState(
        targetValue = if (dragging) 1f else 0f,
        animationSpec = if (animated) spring(stiffness = Spring.StiffnessMedium) else snap(),
        label = "floating-nav-lift"
    )
    val selectedEntry = entries.firstOrNull { it.tab == selected }
    val selectedIndex = entries.indexOfFirst { it.tab == selected }
    val shape = RoundedCornerShape(percent = 50)
    val accessibilityActions = listOf(
        CustomAccessibilityAction(copy.moveUp) { nudge(-SidebarAccessibilityStep); true },
        CustomAccessibilityAction(copy.moveDown) { nudge(SidebarAccessibilityStep); true },
        CustomAccessibilityAction(copy.switchSide) { switchSide(); true }
    )

    val safeHorizontal = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
    val safeTop = WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
    val bottomSystem = WindowInsets.ime.union(WindowInsets.navigationBars).only(WindowInsetsSides.Bottom)

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.layout { measurable, constraints ->
                val width = constraints.maxWidth
                val height = constraints.maxHeight
                val margin = SidebarBandMargin.roundToPx()
                val gap = SidebarEdgeGap.roundToPx()
                val top = safeTop.getTop(this) + margin
                val bottom = height - maxOf(bottomClearancePx(), bottomSystem.getBottom(this)) - margin
                val band = (bottom - top).coerceAtLeast(SidebarTouchTarget.roundToPx())
                val placeable = measurable.measure(Constraints(maxWidth = width, maxHeight = band))
                val left = safeHorizontal.getLeft(this, layoutDirection) + gap
                val right = width - safeHorizontal.getRight(this, layoutDirection) - gap - placeable.width
                geometry.width = width.toFloat()
                geometry.leftX = left.toFloat()
                geometry.rightX = right.toFloat()
                geometry.panelWidth = placeable.width.toFloat()
                geometry.travel = (band - placeable.height).toFloat()
                layout(width, height) {
                    val x = (geometry.baseX(side) + offsetX.floatValue).roundToInt()
                    val y = top + (anchor.floatValue * (band - placeable.height).coerceAtLeast(0)).roundToInt()
                    placeable.place(x, y)
                }
            },
            enter = fadeIn(if (animated) tween(220) else snap()) +
                scaleIn(animationSpec = if (animated) tween(220) else snap(), initialScale = 0.86f),
            exit = fadeOut(if (animated) tween(160) else snap()) +
                scaleOut(animationSpec = if (animated) tween(160) else snap(), targetScale = 0.86f)
        ) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        val scale = 1f + (SidebarLiftScale - 1f) * lift
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(if (side == FloatingNavSide.Left) 0f else 1f, 0.5f)
                        shadowElevation = (SidebarRestElevation + (SidebarLiftElevation - SidebarRestElevation) * lift).toPx()
                        this.shape = shape
                        clip = true
                        ambientShadowColor = Color.Black.copy(alpha = 0.28f)
                        spotShadowColor = Color.Black.copy(alpha = 0.36f)
                    }
                    .background(surface)
                    .systemGestureExclusion()
                    .border(1.dp, colors.rim, shape)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                settleJob?.cancel()
                                dragging = true
                                gestureHaptics.perform(LevyraHapticAction.SeekSnap)
                            },
                            onDragEnd = { settle() },
                            onDragCancel = { settle() },
                            onDrag = { change, amount ->
                                change.consume()
                                if (geometry.travel > 0f) {
                                    anchor.floatValue = (anchor.floatValue + amount.y / geometry.travel).coerceIn(0f, 1f)
                                }
                                val minimum = geometry.leftX - geometry.baseX(side)
                                val maximum = geometry.rightX - geometry.baseX(side)
                                offsetX.floatValue = (offsetX.floatValue + amount.x).coerceIn(minimum, maximum)
                            }
                        )
                    }
            ) {
                AnimatedContent(
                    targetState = expanded,
                    transitionSpec = {
                        val sizeSpec = if (animated) {
                            spring<IntSize>(dampingRatio = 0.8f, stiffness = 420f)
                        } else {
                            snap()
                        }
                        (fadeIn(tween(if (animated) 200 else 0, delayMillis = if (animated) 70 else 0)) +
                            scaleIn(initialScale = 0.92f, animationSpec = if (animated) tween(240) else snap()))
                            .togetherWith(fadeOut(tween(if (animated) 90 else 0)))
                            .using(SizeTransform(clip = true) { _, _ -> sizeSpec })
                    },
                    contentAlignment = Alignment.TopCenter,
                    label = "floating-nav-morph"
                ) { open ->
                    if (open) {
                        SidebarPanel(
                            entries = entries,
                            selected = selected,
                            selectedIndex = selectedIndex,
                            colors = colors,
                            collapseLabel = copy.collapse,
                            paneLabel = copy.handle,
                            accessibilityActions = accessibilityActions,
                            animated = animated,
                            onCollapse = { expanded = false },
                            onSelect = { tab ->
                                expanded = false
                                if (tab != selected) haptics.perform(LevyraHapticAction.Confirm)
                                onSelect(tab)
                            }
                        )
                    } else {
                        SidebarHandle(
                            entry = selectedEntry,
                            colors = colors,
                            label = selectedEntry?.let { "${copy.handle}, ${it.label}" } ?: copy.handle,
                            expandLabel = copy.expand,
                            accessibilityActions = accessibilityActions,
                            onExpand = { expanded = true }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarGrip(colors: NavColors) {
    Box(
        modifier = Modifier
            .size(SidebarGripWidth, SidebarGripHeight)
            .clip(CircleShape)
            .background(colors.inactive.copy(alpha = colors.inactive.alpha * 0.45f))
    )
}

@Composable
private fun SidebarHandle(
    entry: LevyraTabEntry?,
    colors: NavColors,
    label: String,
    expandLabel: String,
    accessibilityActions: List<CustomAccessibilityAction>,
    onExpand: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(SidebarHandleWidth)
            .levyraPressable(onClick = onExpand, role = Role.Button, onClickLabel = expandLabel)
            .semantics(mergeDescendants = true) {
                contentDescription = label
                customActions = accessibilityActions
            }
            .padding(top = 10.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SidebarGrip(colors)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(SidebarCurrentBadge)
                .clip(CircleShape)
                .background(colors.indicator),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = entry?.selectedIcon ?: Icons.Rounded.ViewSidebar,
                contentDescription = null,
                tint = colors.onIndicator,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun SidebarPanel(
    entries: List<LevyraTabEntry>,
    selected: LevyraTab,
    selectedIndex: Int,
    colors: NavColors,
    collapseLabel: String,
    paneLabel: String,
    accessibilityActions: List<CustomAccessibilityAction>,
    animated: Boolean,
    onCollapse: () -> Unit,
    onSelect: (LevyraTab) -> Unit
) {
    val indicator by animateFloatAsState(
        targetValue = selectedIndex.coerceAtLeast(0).toFloat(),
        animationSpec = if (animated) spring(dampingRatio = 0.74f, stiffness = Spring.StiffnessMediumLow) else snap(),
        label = "floating-nav-indicator"
    )
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    LaunchedEffect(Unit) {
        if (selectedIndex > 0) {
            scroll.scrollTo(with(density) { ((SidebarItemHeight + SidebarItemGap) * selectedIndex).roundToPx() })
        }
    }
    Column(
        modifier = Modifier
            .width(SidebarRailWidth)
            .semantics {
                paneTitle = paneLabel
                customActions = accessibilityActions
            }
            .padding(top = 10.dp, bottom = SidebarItemInset),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SidebarGrip(colors)
        Box(
            modifier = Modifier
                .size(SidebarTouchTarget)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onCollapse),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.UnfoldLess,
                contentDescription = collapseLabel,
                tint = colors.inactive,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(scroll)
                .padding(horizontal = SidebarItemInset)
                .selectableGroup()
                .drawBehind {
                    if (selectedIndex < 0) return@drawBehind
                    val step = (SidebarItemHeight + SidebarItemGap).toPx()
                    drawRoundRect(
                        color = colors.indicator,
                        topLeft = Offset(0f, indicator * step),
                        size = Size(size.width, SidebarItemHeight.toPx()),
                        cornerRadius = CornerRadius(SidebarIndicatorCorner.toPx())
                    )
                },
            verticalArrangement = Arrangement.spacedBy(SidebarItemGap)
        ) {
            entries.forEach { entry ->
                SidebarItem(
                    entry = entry,
                    selected = entry.tab == selected,
                    colors = colors,
                    animated = animated,
                    onClick = { onSelect(entry.tab) }
                )
            }
        }
    }
}

@Composable
private fun SidebarItem(
    entry: LevyraTabEntry,
    selected: Boolean,
    colors: NavColors,
    animated: Boolean,
    onClick: () -> Unit
) {
    val tint by animateColorAsState(
        targetValue = if (selected) colors.onIndicator else colors.inactive,
        animationSpec = if (animated) tween(200) else snap(),
        label = "floating-nav-item-tint"
    )
    val textTint by animateColorAsState(
        targetValue = if (selected) colors.onIndicator else colors.inactiveText,
        animationSpec = if (animated) tween(200) else snap(),
        label = "floating-nav-item-text"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(SidebarItemHeight)
            .clip(RoundedCornerShape(SidebarIndicatorCorner))
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) entry.selectedIcon else entry.unselectedIcon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = entry.label,
            color = textTint,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 2.dp).levyraMarquee()
        )
    }
}

internal fun SettingsGroupCollector.floatingNavSettingsItems(
    interfaceSettings: LevyraInterfaceSettings,
    strings: LevyraStrings,
    onInterfaceSettings: (LevyraInterfaceSettings) -> Unit
) {
    val floating = interfaceSettings.floatingNav
    val copy = strings.floatingNav()
    fun update(value: LevyraFloatingNavSettings) = onInterfaceSettings(interfaceSettings.copy(floatingNav = value))
    item {
        SettingsToggle(
            icon = Icons.Rounded.ViewSidebar,
            title = copy.title,
            subtitle = copy.subtitle,
            checked = floating.enabled,
            onCheckedChange = { update(floating.copy(enabled = it)) }
        )
    }
    if (floating.enabled) {
        item {
            SettingsChoiceRow(
                icon = Icons.Rounded.SwapHoriz,
                title = copy.side,
                subtitle = copy.sideSubtitle,
                options = listOf(
                    FloatingNavSide.Left.storageId to copy.left,
                    FloatingNavSide.Right.storageId to copy.right
                ),
                selected = floating.side.storageId,
                onSelect = { update(floating.copy(side = FloatingNavSide.from(it))) }
            )
        }
    }
}
