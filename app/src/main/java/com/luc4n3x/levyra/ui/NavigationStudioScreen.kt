package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import android.os.Build
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.luc4n3x.levyra.domain.LevyraNavigationStudioSettings
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.domain.NavBackground
import com.luc4n3x.levyra.domain.NavColorSource
import com.luc4n3x.levyra.domain.NavDestinations
import com.luc4n3x.levyra.domain.NavIndicator
import com.luc4n3x.levyra.domain.NavIndicatorTone
import com.luc4n3x.levyra.domain.NavLabelMode
import com.luc4n3x.levyra.domain.NavMotion
import com.luc4n3x.levyra.domain.NavStyle
import com.luc4n3x.levyra.domain.NavStyleSpec
import com.luc4n3x.levyra.domain.NavSurfaceTone
import com.luc4n3x.levyra.domain.NavTuning
import com.luc4n3x.levyra.ui.components.LevyraAdaptiveDockSurface
import com.luc4n3x.levyra.ui.components.LevyraBackButton
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.NavStudioText
import com.luc4n3x.levyra.ui.i18n.NavigationStudioCopy
import com.luc4n3x.levyra.ui.i18n.homeStudioCopy
import com.luc4n3x.levyra.ui.i18n.miniPlayerStudioCopy
import com.luc4n3x.levyra.ui.i18n.navigationStudioCopy
import com.luc4n3x.levyra.ui.i18n.speedDialCopy
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics

private val NavStudioCardShape = RoundedCornerShape(24.dp)
private val NavStudioStageShape = RoundedCornerShape(32.dp)

internal fun navigationStyleName(style: NavStyle): String = when (style) {
    NavStyle.Original -> "Levyra Original"
    NavStyle.SuperExpressive -> "Super Expressive"
    NavStyle.FloatingDock -> "Floating Dock"
    NavStyle.Minimal -> "Minimal"
    NavStyle.ExpressiveCompact -> "Expressive Compact"
    NavStyle.LiquidGlass -> "Liquid Glass"
    NavStyle.YtMusic -> "YT Music"
    NavStyle.SplitDock -> "Split Floating Dock"
    NavStyle.ConnectedDock -> "Connected Dock"
}

private fun navigationStyleDescription(style: NavStyle, copy: NavigationStudioCopy): String = when (style) {
    NavStyle.Original -> copy[NavStudioText.DescOriginal]
    NavStyle.SuperExpressive -> copy[NavStudioText.DescSuperExpressive]
    NavStyle.FloatingDock -> copy[NavStudioText.DescFloating]
    NavStyle.Minimal -> copy[NavStudioText.DescMinimal]
    NavStyle.ExpressiveCompact -> copy[NavStudioText.DescCompact]
    NavStyle.LiquidGlass -> copy[NavStudioText.DescLiquidGlass]
    NavStyle.YtMusic -> copy[NavStudioText.DescYtMusic]
    NavStyle.SplitDock -> copy[NavStudioText.DescSplitDock]
    NavStyle.ConnectedDock -> copy[NavStudioText.DescConnectedDock]
}

internal fun navReorderStep(
    order: List<LevyraTab>,
    tab: LevyraTab,
    offset: Float,
    heights: Map<LevyraTab, Int>
): Pair<List<LevyraTab>, Float>? {
    val position = order.indexOf(tab)
    if (position < 0) return null
    val nextHeight = order.getOrNull(position + 1)?.let(heights::get)?.toFloat()
    val previousHeight = order.getOrNull(position - 1)?.let(heights::get)?.toFloat()
    return when {
        nextHeight != null && offset > nextHeight / 2f ->
            order.toMutableList().apply { add(position + 1, removeAt(position)) } to offset - nextHeight
        previousHeight != null && offset < -previousHeight / 2f ->
            order.toMutableList().apply { add(position - 1, removeAt(position)) } to offset + previousHeight
        else -> null
    }
}

@Composable
internal fun NavigationStudioOverlay(
    settings: LevyraNavigationStudioSettings,
    onUpdate: (LevyraNavigationStudioSettings) -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = strings.navigationStudioCopy
    val haptics = LocalLevyraHaptics.current
    val latest by rememberUpdatedState(settings)
    var draft by remember { mutableStateOf<NavTuning?>(null) }
    var confirmRestore by remember { mutableStateOf(false) }
    var previewTab by remember { mutableStateOf(LevyraTab.Home) }
    val style = settings.style
    val spec = LevyraNavigationStudioSettings.spec(style)
    val tuning = draft ?: settings.activeTuning()
    val editable = style != NavStyle.Original
    val commit: (NavTuning) -> Unit = { next ->
        val current = latest
        onUpdate(current.updateTuning(current.style) { next })
        draft = null
    }
    val preview: (NavTuning) -> Unit = { next -> draft = next.normalized(style) }

    var dragging by remember { mutableStateOf<LevyraTab?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var workingOrder by remember(style, tuning.order) { mutableStateOf(tuning.order) }
    val rowHeights = remember { mutableStateMapOf<LevyraTab, Int>() }
    val labels = rememberLevyraTabEntries().associate { it.tab to it.label }

    fun moveBy(tab: LevyraTab, delta: Int): Boolean {
        val index = workingOrder.indexOf(tab)
        val target = index + delta
        if (index < 0 || target !in workingOrder.indices) return false
        val next = workingOrder.toMutableList().apply { add(target, removeAt(index)) }
        workingOrder = next
        commit(tuning.copy(order = next))
        haptics.perform(LevyraHapticAction.Reorder)
        return true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LevyraInk)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LevyraBackButton(onClick = onClose, contentDescription = strings.back)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                ) {
                    Text(
                        text = copy[NavStudioText.Title],
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        color = LevyraText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = copy[NavStudioText.Subtitle],
                        style = MaterialTheme.typography.bodyMedium,
                        color = LevyraMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            NavStudioStage(
                style = style,
                tuning = tuning,
                selected = previewTab,
                onSelect = { previewTab = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "nav-style-label", contentType = "label") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) { MiniStudioLabel(copy[NavStudioText.StyleSection]) }
                        TextButton(
                            onClick = { confirmRestore = true },
                            enabled = settings != LevyraNavigationStudioSettings(),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(copy[NavStudioText.RestoreDefault], maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                item(key = "nav-styles", contentType = "styles") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(NavStyle.entries, key = { it.storageId }) { option ->
                            NavStyleCard(
                                style = option,
                                selected = option == style,
                                copy = copy,
                                tuning = settings.tuningFor(option),
                                onClick = {
                                    if (option != latest.style) {
                                        draft = null
                                        onUpdate(latest.withStyle(option))
                                        haptics.perform(LevyraHapticAction.Confirm)
                                    }
                                }
                            )
                        }
                    }
                }
                item(key = "nav-style-description", contentType = "text") {
                    Text(
                        text = "${navigationStyleName(style)} · ${navigationStyleDescription(style, copy)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LevyraMuted
                    )
                }
                if (!editable) {
                    item(key = "nav-locked", contentType = "info") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(NavStudioCardShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(copy[NavStudioText.OriginalLocked], style = MaterialTheme.typography.bodyMedium, color = LevyraText)
                        }
                    }
                    return@LazyColumn
                }
                item(key = "nav-destinations-label", contentType = "label") {
                    Column {
                        MiniStudioLabel(copy[NavStudioText.Destinations])
                        Text(copy[NavStudioText.DestinationsHint], style = MaterialTheme.typography.bodySmall, color = LevyraMuted)
                    }
                }
                items(workingOrder, key = { "nav-${NavDestinations.storageId(it)}" }, contentType = { "nav-row" }) { tab ->
                    val isDragging = dragging == tab
                    val index = workingOrder.indexOf(tab)
                    val title = labels[tab].orEmpty()
                    val finishDrag: () -> Unit = {
                        dragging = null
                        dragOffset = 0f
                        val current = latest.activeTuning()
                        if (workingOrder != current.order) commit(current.copy(order = workingOrder))
                    }
                    NavDestinationRow(
                        tab = tab,
                        title = title,
                        dragging = isDragging,
                        dragOffset = { dragOffset },
                        visible = tab !in tuning.hidden,
                        copy = copy,
                        moveActions = listOfNotNull(
                            CustomAccessibilityAction(strings.speedDialCopy().moveEarlier) { moveBy(tab, -1) }
                                .takeIf { index > 0 },
                            CustomAccessibilityAction(strings.speedDialCopy().moveLater) { moveBy(tab, 1) }
                                .takeIf { index < workingOrder.lastIndex }
                        ),
                        onMeasured = { rowHeights[tab] = it },
                        onDragStart = {
                            dragging = tab
                            dragOffset = 0f
                            haptics.perform(LevyraHapticAction.Confirm)
                        },
                        onDragEnd = finishDrag,
                        onDrag = { delta ->
                            dragOffset += delta
                            navReorderStep(workingOrder, tab, dragOffset, rowHeights)?.let { (order, offset) ->
                                workingOrder = order
                                dragOffset = offset
                                haptics.perform(LevyraHapticAction.Reorder)
                            }
                        },
                        onVisible = { shown -> commit(tuning.withVisibility(tab, shown)) }
                    )
                }
                navOptionItems(tuning, spec, copy, preview, commit)
                item(key = "nav-reset-style", contentType = "actions") {
                    OutlinedButton(
                        onClick = {
                            draft = null
                            onUpdate(latest.resetStyle(latest.style))
                        },
                        enabled = style in settings.normalized().tunings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(copy[NavStudioText.ResetStyle])
                    }
                }
            }
        }
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text(copy[NavStudioText.RestoreDefault]) },
            text = { Text(copy[NavStudioText.RestoreConfirm]) },
            confirmButton = {
                TextButton(onClick = {
                    draft = null
                    onUpdate(LevyraNavigationStudioSettings())
                    haptics.perform(LevyraHapticAction.Confirm)
                    confirmRestore = false
                }) { Text(strings.homeStudioCopy.reset) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = false }) { Text(strings.cancel) }
            }
        )
    }
}

private fun LazyListScope.navOptionItems(
    tuning: NavTuning,
    spec: NavStyleSpec,
    copy: NavigationStudioCopy,
    preview: (NavTuning) -> Unit,
    commit: (NavTuning) -> Unit
) {
    item(key = "nav-indicator", contentType = "group") {
        MiniStudioGroup(copy[NavStudioText.Indicator]) {
            NavChoiceChips(
                options = spec.indicators,
                selected = tuning.indicator,
                label = { navIndicatorLabel(it, copy) },
                onSelect = { commit(tuning.copy(indicator = it)) }
            )
            MiniStudioCaption(copy[NavStudioText.IndicatorColor])
            MiniStudioChoice(
                options = NavIndicatorTone.entries,
                selected = tuning.indicatorTone,
                label = { tone ->
                    when (tone) {
                        NavIndicatorTone.Tonal -> copy[NavStudioText.ToneTonal]
                        NavIndicatorTone.Vivid -> copy[NavStudioText.ToneVivid]
                        NavIndicatorTone.Neutral -> copy[NavStudioText.ToneNeutral]
                    }
                },
                onSelect = { commit(tuning.copy(indicatorTone = it)) }
            )
            MiniStudioSlider(
                title = copy[NavStudioText.IndicatorSize],
                value = tuning.indicatorScale,
                range = NavTuning.INDICATOR_SCALE_RANGE,
                unit = "%",
                onPreview = { preview(tuning.copy(indicatorScale = it)) },
                onCommit = { commit(tuning.copy(indicatorScale = it)) }
            )
        }
    }
    item(key = "nav-icons-labels", contentType = "group") {
        MiniStudioGroup(copy[NavStudioText.IconsLabels]) {
            MiniStudioCaption(copy[NavStudioText.Labels])
            NavChoiceChips(
                options = NavLabelMode.entries,
                selected = tuning.labels,
                label = { mode ->
                    when (mode) {
                        NavLabelMode.Always -> copy[NavStudioText.LabelsAlways]
                        NavLabelMode.ActiveOnly -> copy[NavStudioText.LabelsActive]
                        NavLabelMode.Compact -> copy[NavStudioText.LabelsCompact]
                        NavLabelMode.Never -> copy[NavStudioText.LabelsNever]
                        NavLabelMode.Expanding -> copy[NavStudioText.LabelsExpanding]
                    }
                },
                onSelect = { commit(tuning.copy(labels = it)) }
            )
            MiniStudioSlider(
                title = copy[NavStudioText.IconSize],
                value = tuning.iconDp,
                range = NavTuning.ICON_RANGE,
                unit = "dp",
                onPreview = { preview(tuning.copy(iconDp = it)) },
                onCommit = { commit(tuning.copy(iconDp = it)) }
            )
            MiniStudioSlider(
                title = copy[NavStudioText.InactiveIcons],
                value = tuning.inactiveAlpha,
                range = NavTuning.INACTIVE_RANGE,
                unit = "%",
                onPreview = { preview(tuning.copy(inactiveAlpha = it)) },
                onCommit = { commit(tuning.copy(inactiveAlpha = it)) }
            )
        }
    }
    item(key = "nav-colors", contentType = "group") {
        val strings = LocalLevyraStrings.current
        MiniStudioGroup(copy[NavStudioText.Colors]) {
            MiniStudioCaption(copy[NavStudioText.ColorSource])
            NavChoiceChips(
                options = NavColorSourceOrder,
                selected = tuning.colorSource,
                label = { source ->
                    when (source) {
                        NavColorSource.Theme -> copy[NavStudioText.ColorTheme]
                        NavColorSource.Dynamic -> copy[NavStudioText.ColorDynamic]
                        NavColorSource.Artwork -> copy[NavStudioText.ColorArtwork]
                        NavColorSource.Custom -> copy[NavStudioText.ColorCustom]
                    }
                },
                onSelect = { commit(tuning.copy(colorSource = it)) }
            )
            if (tuning.colorSource == NavColorSource.Dynamic && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                Text(copy[NavStudioText.DynamicUnavailable], style = MaterialTheme.typography.bodySmall, color = LevyraMuted)
            }
            if (tuning.colorSource == NavColorSource.Artwork) {
                Text(copy[NavStudioText.ArtworkHint], style = MaterialTheme.typography.bodySmall, color = LevyraMuted)
            }
            if (tuning.colorSource == NavColorSource.Custom) {
                MiniColorEditor(
                    color = tuning.accent,
                    copy = strings.miniPlayerStudioCopy,
                    onPreviewColor = { preview(tuning.copy(accent = it)) },
                    onCancelPreview = { preview(tuning) },
                    onColor = { commit(tuning.copy(accent = it)) }
                )
            }
            MiniStudioCaption(copy[NavStudioText.Surface])
            NavChoiceChips(
                options = NavSurfaceTone.entries,
                selected = tuning.surfaceTone,
                label = { tone ->
                    when (tone) {
                        NavSurfaceTone.Auto -> copy[NavStudioText.SurfaceAuto]
                        NavSurfaceTone.Light -> copy[NavStudioText.SurfaceLight]
                        NavSurfaceTone.Dark -> copy[NavStudioText.SurfaceDark]
                        NavSurfaceTone.Amoled -> copy[NavStudioText.SurfaceAmoled]
                    }
                },
                onSelect = { commit(tuning.copy(surfaceTone = it)) }
            )
            if (spec.backgrounds.size > 1) {
                MiniStudioCaption(copy[NavStudioText.Background])
                MiniStudioChoice(
                    options = spec.backgrounds,
                    selected = tuning.background,
                    label = { background ->
                        when (background) {
                            NavBackground.Solid -> copy[NavStudioText.BgSolid]
                            NavBackground.Gradient -> copy[NavStudioText.BgGradient]
                            NavBackground.Translucent -> copy[NavStudioText.BgTranslucent]
                        }
                    },
                    onSelect = { commit(tuning.copy(background = it)) }
                )
            }
            if (tuning.background == NavBackground.Translucent) {
                MiniStudioSlider(
                    title = copy[NavStudioText.Opacity],
                    value = tuning.opacity,
                    range = NavTuning.OPACITY_RANGE,
                    unit = "%",
                    onPreview = { preview(tuning.copy(opacity = it)) },
                    onCommit = { commit(tuning.copy(opacity = it)) }
                )
            }
            Text(copy[NavStudioText.ContrastHint], style = MaterialTheme.typography.bodySmall, color = LevyraMuted)
        }
    }
    item(key = "nav-size", contentType = "group") {
        MiniStudioGroup(copy[NavStudioText.Size]) {
            MiniStudioSlider(
                title = copy[NavStudioText.Height],
                value = tuning.heightDp,
                range = spec.height,
                unit = "dp",
                onPreview = { preview(tuning.copy(heightDp = it)) },
                onCommit = { commit(tuning.copy(heightDp = it)) }
            )
            if (spec.corner.last > spec.corner.first) {
                MiniStudioSlider(
                    title = copy[NavStudioText.Corners],
                    value = tuning.cornerDp,
                    range = spec.corner,
                    unit = "dp",
                    onPreview = { preview(tuning.copy(cornerDp = it)) },
                    onCommit = { commit(tuning.copy(cornerDp = it)) }
                )
            }
            MiniStudioSlider(
                title = copy[NavStudioText.Spacing],
                value = tuning.spacingDp,
                range = NavTuning.SPACING_RANGE,
                unit = "dp",
                onPreview = { preview(tuning.copy(spacingDp = it)) },
                onCommit = { commit(tuning.copy(spacingDp = it)) }
            )
            MiniStudioSlider(
                title = copy[NavStudioText.Padding],
                value = tuning.paddingDp,
                range = NavTuning.PADDING_RANGE,
                unit = "dp",
                onPreview = { preview(tuning.copy(paddingDp = it)) },
                onCommit = { commit(tuning.copy(paddingDp = it)) }
            )
        }
    }
    item(key = "nav-motion", contentType = "group") {
        MiniStudioGroup(copy[NavStudioText.Motion]) {
            NavChoiceChips(
                options = NavMotion.entries,
                selected = tuning.motion,
                label = { motion ->
                    when (motion) {
                        NavMotion.Standard -> copy[NavStudioText.MotionStandard]
                        NavMotion.Expressive -> copy[NavStudioText.MotionExpressive]
                        NavMotion.Spring -> copy[NavStudioText.MotionSpring]
                        NavMotion.Minimal -> copy[NavStudioText.MotionMinimal]
                        NavMotion.Off -> copy[NavStudioText.MotionOff]
                    }
                },
                onSelect = { commit(tuning.copy(motion = it)) }
            )
            if (tuning.motion != NavMotion.Off && tuning.motion != NavMotion.Minimal) {
                MiniStudioSlider(
                    title = copy[NavStudioText.Intensity],
                    value = tuning.intensity,
                    range = 0..100,
                    unit = "%",
                    onPreview = { preview(tuning.copy(intensity = it)) },
                    onCommit = { commit(tuning.copy(intensity = it)) }
                )
            }
        }
    }
    item(key = "nav-advanced", contentType = "group") {
        MiniStudioGroup(copy[NavStudioText.Advanced]) {
            MiniStudioSwitch(
                title = copy[NavStudioText.Haptics],
                checked = tuning.haptics,
                onCheckedChange = { commit(tuning.copy(haptics = it)) }
            )
        }
    }
}

private val NavColorSourceOrder = listOf(NavColorSource.Theme, NavColorSource.Dynamic, NavColorSource.Artwork, NavColorSource.Custom)

private fun navIndicatorLabel(indicator: NavIndicator, copy: NavigationStudioCopy): String = when (indicator) {
    NavIndicator.Pill -> copy[NavStudioText.IndicatorPill]
    NavIndicator.Morph -> copy[NavStudioText.IndicatorMorph]
    NavIndicator.Dot -> copy[NavStudioText.IndicatorDot]
    NavIndicator.Line -> copy[NavStudioText.IndicatorLine]
    NavIndicator.Circle -> copy[NavStudioText.IndicatorCircle]
    NavIndicator.Floating -> copy[NavStudioText.IndicatorFloating]
    NavIndicator.None -> copy[NavStudioText.IndicatorNone]
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> NavChoiceChips(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val active = option == selected
            FilterChip(
                selected = active,
                onClick = { onSelect(option) },
                label = { Text(label(option), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = if (active) {
                    { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                } else {
                    null
                },
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
private fun LazyItemScope.NavDestinationRow(
    tab: LevyraTab,
    title: String,
    dragging: Boolean,
    dragOffset: () -> Float,
    visible: Boolean,
    copy: NavigationStudioCopy,
    moveActions: List<CustomAccessibilityAction>,
    onMeasured: (Int) -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onDrag: (Float) -> Unit,
    onVisible: (Boolean) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val placement = if (dragging) {
        Modifier
    } else {
        Modifier.animateItem(
            placementSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
        )
    }
    Row(
        modifier = Modifier
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer {
                val lifted = if (dragging) 1.02f else 1f
                translationY = if (dragging) dragOffset() else 0f
                scaleX = lifted
                scaleY = lifted
            }
            .then(placement)
            .onSizeChanged { onMeasured(it.height) }
            .semantics { customActions = moveActions }
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (dragging) scheme.secondaryContainer else scheme.surfaceContainerHigh)
            .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .semantics { contentDescription = copy[NavStudioText.DragHandle] }
                .pointerInput(tab) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { onDragStart() },
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragEnd,
                        onDrag = { change, amount ->
                            change.consume()
                            onDrag(amount.y)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.DragIndicator, contentDescription = null, tint = LevyraMuted)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = LevyraText,
            modifier = Modifier.weight(1f)
        )
        NavVisibilityControl(
            essential = tab in NavDestinations.essential,
            visible = visible,
            description = "${copy[NavStudioText.Show]}: $title",
            onVisible = onVisible
        )
    }
}

@Composable
private fun NavVisibilityControl(
    essential: Boolean,
    visible: Boolean,
    description: String,
    onVisible: (Boolean) -> Unit
) {
    if (essential) {
        Icon(
            Icons.Rounded.Lock,
            contentDescription = null,
            tint = LevyraMuted,
            modifier = Modifier
                .padding(end = 14.dp)
                .size(18.dp)
        )
    } else {
        Switch(
            checked = visible,
            onCheckedChange = onVisible,
            modifier = Modifier.semantics { contentDescription = description }
        )
    }
}

@Composable
private fun NavStudioStage(
    style: NavStyle,
    tuning: NavTuning,
    selected: LevyraTab,
    onSelect: (LevyraTab) -> Unit,
    modifier: Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val glass = rememberGlassBackdropState(enabled = false)
    Column(
        modifier = modifier
            .clip(NavStudioStageShape)
            .background(Brush.verticalGradient(listOf(scheme.surfaceContainerHigh, scheme.surfaceContainerLow)))
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.35f), NavStudioStageShape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(scheme.primary.copy(alpha = 0.22f - index * 0.05f))
                )
            }
        }
        NavStylePreview(style = style, tuning = tuning, selected = selected, onSelect = onSelect, glass = glass)
    }
}

@Composable
private fun NavStylePreview(
    style: NavStyle,
    tuning: NavTuning,
    selected: LevyraTab,
    onSelect: (LevyraTab) -> Unit,
    glass: GlassBackdropState,
    modifier: Modifier = Modifier
) {
    val artworkSeed = LocalNavArtworkSeed.current ?: NavArtworkDemoSeed
    CompositionLocalProvider(LocalNavArtworkSeed provides artworkSeed) {
        NavStylePreviewBar(style, tuning, selected, onSelect, glass, modifier)
    }
}

@Composable
private fun NavStylePreviewBar(
    style: NavStyle,
    tuning: NavTuning,
    selected: LevyraTab,
    onSelect: (LevyraTab) -> Unit,
    glass: GlassBackdropState,
    modifier: Modifier
) {
    Box(modifier = modifier) {
        if (style == NavStyle.Original) {
            LevyraAdaptiveDockSurface(glass = glass, modifier = Modifier.fillMaxWidth()) {
                BottomTabs(selected = selected, compaction = { 0f }, onSelect = onSelect)
            }
        } else {
            val previewLink = remember(style, tuning.cornerDp, tuning.heightDp) {
                if (style == NavStyle.ConnectedDock) {
                    DockLink(attach = { 1f }, fuse = { 0f }, outerCorner = minOf(tuning.cornerDp, tuning.heightDp / 2).dp)
                } else {
                    null
                }
            }
            StyledDockSurface(style = style, tuning = tuning, glass = null, modifier = Modifier.fillMaxWidth()) {
                if (previewLink != null) ConnectedDockPreviewSlab(tuning)
                StyledBottomTabs(
                    style = style,
                    tuning = tuning,
                    selected = selected,
                    compaction = { 0f },
                    onSelect = onSelect,
                    glass = null,
                    applySystemInset = false,
                    link = previewLink
                )
            }
        }
    }
}

@Composable
private fun NavStyleCard(
    style: NavStyle,
    selected: Boolean,
    copy: NavigationStudioCopy,
    tuning: NavTuning,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val name = navigationStyleName(style)
    val isDefault = style == NavStyle.Original
    val selectedLabel = copy[NavStudioText.Selected]
    val defaultLabel = copy[NavStudioText.DefaultBadge]
    val glass = rememberGlassBackdropState(enabled = false)
    Column(
        modifier = Modifier
            .width(184.dp)
            .clip(NavStudioCardShape)
            .background(if (selected) scheme.secondaryContainer else scheme.surfaceContainerHigh)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) scheme.primary else scheme.outlineVariant.copy(alpha = 0.5f),
                shape = NavStudioCardShape
            )
            .levyraPressable(onClick = onClick, onClickLabel = name, role = Role.RadioButton)
            .semantics {
                this.selected = selected
                stateDescription = listOfNotNull(selectedLabel.takeIf { selected }, defaultLabel.takeIf { isDefault }).joinToString(", ")
            }
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.surfaceContainerLowest),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .requiredWidth(360.dp)
                    .graphicsLayer {
                        scaleX = 0.46f
                        scaleY = 0.46f
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
            ) {
                NavStylePreview(style = style, tuning = tuning, selected = LevyraTab.Home, onSelect = {}, glass = glass)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold),
                color = if (selected) scheme.onSecondaryContainer else LevyraText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(scheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(15.dp))
                }
            }
        }
        Box(modifier = Modifier.height(22.dp), contentAlignment = Alignment.CenterStart) {
            if (isDefault) MiniDefaultBadge(defaultLabel)
        }
    }
}
