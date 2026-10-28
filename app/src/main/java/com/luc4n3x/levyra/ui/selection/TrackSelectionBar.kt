package com.luc4n3x.levyra.ui.selection

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.AppleMusicDesign
import com.luc4n3x.levyra.ui.theme.IvoryDesign
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraGlassBorder
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraIsAppleMusic
import com.luc4n3x.levyra.ui.theme.LevyraIsIvory
import com.luc4n3x.levyra.ui.theme.LevyraIsNocturne
import com.luc4n3x.levyra.ui.theme.LevyraIsPrism
import com.luc4n3x.levyra.ui.theme.LevyraIsSpotify
import com.luc4n3x.levyra.ui.theme.LevyraIsTorque
import com.luc4n3x.levyra.ui.theme.LevyraIsYouTubeMusic
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPanelSoft
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import com.luc4n3x.levyra.ui.theme.Nocturne78Design
import com.luc4n3x.levyra.ui.theme.PrismDesign
import com.luc4n3x.levyra.ui.theme.SkinChrome
import com.luc4n3x.levyra.ui.theme.SpotifyDesign
import com.luc4n3x.levyra.ui.theme.YouTubeMusicDesign

internal data class TrackSelectionActions(
    val onPlayNext: (() -> Unit)? = null,
    val onAddToQueue: (() -> Unit)? = null,
    val onAddToPlaylist: (() -> Unit)? = null,
    val onFavorite: (() -> Unit)? = null,
    val onDownload: (() -> Unit)? = null
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun TrackSelectionBar(
    state: TrackSelectionState,
    allVisibleIds: List<String>,
    actions: TrackSelectionActions,
    modifier: Modifier = Modifier,
    aboveRootDock: Boolean = false
) {
    val animated = LocalAnimationsEnabled.current
    val motion = MaterialTheme.motionScheme
    val bottomInset = trackSelectionBottomInset(aboveRootDock)
    var lastCount by remember { mutableIntStateOf(state.count) }
    SideEffect { if (state.isActive) lastCount = state.count }
    AnimatedVisibility(
        visible = state.isActive,
        enter = if (animated) fadeIn(motion.fastEffectsSpec()) +
            slideInVertically(motion.defaultSpatialSpec()) { it / 5 } else EnterTransition.None,
        exit = if (animated) fadeOut(motion.fastEffectsSpec()) +
            slideOutVertically(motion.fastSpatialSpec()) { it / 5 } else ExitTransition.None,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .onSizeChanged { state.dockHeightPx = it.height }
            .padding(bottom = bottomInset + SelectionDockGap)
    ) {
        SelectionDock(state, allVisibleIds, actions, if (state.isActive) state.count else lastCount)
    }
}

private data class SelectionDockStyle(
    val surface: Color,
    val content: Color,
    val accent: Color,
    val actionSurface: Color,
    val onActionSurface: Color,
    val shape: Shape,
    val actionShape: Shape,
    val countStyle: TextStyle,
    val border: BorderStroke? = null
)

@Composable
private fun selectionDockStyle(): SelectionDockStyle {
    val scheme = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    if (!LevyraIsPrism && !LevyraIsNocturne) {
        return SelectionDockStyle(
            scheme.surfaceContainerHigh, scheme.onSurface, scheme.primary, scheme.secondaryContainer, scheme.onSecondaryContainer,
            MaterialTheme.shapes.extraLarge, MaterialTheme.shapes.large, type.titleMedium
        )
    }
    val shape = when {
        LevyraIsNocturne -> RoundedCornerShape(Nocturne78Design.cardCorner)
        LevyraIsIvory -> RoundedCornerShape(IvoryDesign.surfaceCorner)
        LevyraIsSpotify -> RoundedCornerShape(SpotifyDesign.cornerLarge)
        LevyraIsAppleMusic -> RoundedCornerShape(AppleMusicDesign.cornerL)
        LevyraIsYouTubeMusic -> RoundedCornerShape(YouTubeMusicDesign.cardCorner)
        else -> PrismDesign.cut(PrismDesign.surfaceCorner)
    }
    val countStyle = when {
        LevyraIsNocturne -> type.titleMedium.copy(fontFamily = type.titleLarge.fontFamily)
        LevyraIsTorque -> type.titleSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        else -> type.titleMedium
    }
    return SelectionDockStyle(
        surface = LevyraPanel,
        content = LevyraText,
        accent = LevyraCyan,
        actionSurface = LevyraPanelSoft,
        onActionSurface = LevyraText,
        shape = shape,
        actionShape = when {
            LevyraIsNocturne -> RoundedCornerShape(Nocturne78Design.controlCorner)
            LevyraIsIvory -> RoundedCornerShape(IvoryDesign.controlCorner)
            LevyraIsAppleMusic -> RoundedCornerShape(AppleMusicDesign.cornerL)
            LevyraIsTorque -> RoundedCornerShape(PrismDesign.controlCorner)
            else -> SkinChrome.chipShape
        },
        countStyle = countStyle,
        border = if (LevyraIsNocturne || (LevyraIsPrism && !LevyraIsSpotify && !LevyraIsAppleMusic && !LevyraIsYouTubeMusic)) {
            BorderStroke(1.dp, LevyraGlassBorder)
        } else null
    )
}

private data class SelectionDockAction(val label: String, val icon: ImageVector, val invoke: () -> Unit)

@Composable
private fun TrackSelectionActions.dockActions(): List<SelectionDockAction> {
    val strings = LocalLevyraStrings.current
    return buildList {
        onPlayNext?.let { add(SelectionDockAction(strings.playNext, Icons.AutoMirrored.Rounded.PlaylistPlay, it)) }
        onAddToQueue?.let { add(SelectionDockAction(strings.addToQueue, Icons.AutoMirrored.Rounded.QueueMusic, it)) }
        onAddToPlaylist?.let { add(SelectionDockAction(strings.addToPlaylist, Icons.AutoMirrored.Rounded.PlaylistAdd, it)) }
        onFavorite?.let { add(SelectionDockAction(strings.favorite, Icons.Rounded.Favorite, it)) }
        onDownload?.let { add(SelectionDockAction(strings.download, Icons.Rounded.Download, it)) }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SelectionDock(
    state: TrackSelectionState,
    allVisibleIds: List<String>,
    actions: TrackSelectionActions,
    count: Int
) {
    val style = selectionDockStyle()
    val animated = LocalAnimationsEnabled.current
    val motion = MaterialTheme.motionScheme
    val selectableIds = remember(allVisibleIds) {
        allVisibleIds.mapNotNullTo(linkedSetOf()) { it.trim().takeIf(String::isNotBlank) }
    }
    val availableActions = actions.dockActions()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val readableWidth = maxWidth.value / LocalDensity.current.fontScale
        Surface(
            shape = style.shape,
            color = style.surface,
            contentColor = style.content,
            border = style.border,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight)
                .semantics { isTraversalGroup = true }
        ) {
            Column(
                modifier = Modifier
                    .then(if (animated) Modifier.animateContentSize(motion.fastSpatialSpec()) else Modifier)
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SelectionDockHeader(state, selectableIds, count, style, readableWidth, maxWidth)
                SelectionDockActions(availableActions, state.isActive, style, readableWidth)
            }
        }
    }
}

@Composable
private fun SelectionDockHeader(
    state: TrackSelectionState,
    selectableIds: Set<String>,
    count: Int,
    style: SelectionDockStyle,
    readableWidth: Float,
    maxWidth: Dp
) {
    val strings = LocalLevyraStrings.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = state::exit, enabled = state.isActive, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = strings.close)
        }
        SelectionDockCount(count, style)
        SelectionDockSelectAll(state, selectableIds, style, readableWidth, maxWidth)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RowScope.SelectionDockCount(count: Int, style: SelectionDockStyle) {
    val strings = LocalLevyraStrings.current
    val animated = LocalAnimationsEnabled.current
    val motion = MaterialTheme.motionScheme
    AnimatedContent(
        targetState = count,
        transitionSpec = {
            (if (animated) fadeIn(motion.fastEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec())
            else EnterTransition.None togetherWith ExitTransition.None).using(null)
        },
        label = "selection-count",
        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            .clearAndSetSemantics {
                contentDescription = strings.formatTrackCount(count)
                liveRegion = LiveRegionMode.Polite
            }
    ) {
        Text(strings.formatTrackCount(it), style = style.countStyle, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SelectionDockSelectAll(
    state: TrackSelectionState,
    selectableIds: Set<String>,
    style: SelectionDockStyle,
    readableWidth: Float,
    maxWidth: Dp
) {
    val strings = LocalLevyraStrings.current
    val haptics = LocalLevyraHaptics.current
    val allSelected = selectableIds.isNotEmpty() && state.selectedIds.containsAll(selectableIds)
    val selectAll: () -> Unit = {
        haptics.perform(LevyraHapticAction.Confirm)
        if (allSelected) state.exit() else state.selectAll(selectableIds)
    }
    if (readableWidth >= 280f) {
        TextButton(
            onClick = selectAll,
            enabled = state.isActive && selectableIds.isNotEmpty(),
            colors = ButtonDefaults.textButtonColors(contentColor = style.accent),
            modifier = Modifier.widthIn(max = maxWidth / 2).heightIn(min = 48.dp).semantics { selected = allSelected }
        ) {
            Icon(if (allSelected) Icons.Rounded.Check else Icons.Rounded.SelectAll, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(strings.selectAll, style = MaterialTheme.typography.labelMedium)
        }
    } else {
        IconButton(
            onClick = selectAll,
            enabled = state.isActive && selectableIds.isNotEmpty(),
            colors = IconButtonDefaults.iconButtonColors(contentColor = style.accent),
            modifier = Modifier.size(48.dp).semantics { selected = allSelected }
        ) {
            Icon(if (allSelected) Icons.Rounded.Check else Icons.Rounded.SelectAll, strings.selectAll)
        }
    }
}

@Composable
private fun SelectionDockActions(
    availableActions: List<SelectionDockAction>,
    enabled: Boolean,
    style: SelectionDockStyle,
    readableWidth: Float
) {
    val haptics = LocalLevyraHaptics.current
    val inlineCount = when {
        readableWidth >= 600f -> availableActions.size
        readableWidth >= 280f -> minOf(2, availableActions.size)
        readableWidth >= 220f -> minOf(1, availableActions.size)
        else -> 0
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        availableActions.take(inlineCount).forEachIndexed { index, action ->
            FilledTonalButton(
                onClick = {
                    haptics.perform(LevyraHapticAction.Confirm)
                    action.invoke()
                },
                enabled = enabled,
                shape = style.actionShape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (index == 0) style.actionSurface else Color.Transparent,
                    contentColor = if (index == 0) style.onActionSurface else style.content
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
                Icon(action.icon, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(action.label, style = MaterialTheme.typography.labelMedium)
            }
        }
        if (inlineCount < availableActions.size) {
            SelectionDockOverflow(availableActions.drop(inlineCount), enabled, style)
        }
    }
}

@Composable
private fun SelectionDockOverflow(actions: List<SelectionDockAction>, enabled: Boolean, style: SelectionDockStyle) {
    val strings = LocalLevyraStrings.current
    val haptics = LocalLevyraHaptics.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.size(48.dp)
        ) { Icon(Icons.Rounded.MoreHoriz, strings.more) }
        DropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false },
            containerColor = style.surface,
            shape = style.shape
        ) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label, color = style.content) },
                    leadingIcon = { Icon(action.icon, null, tint = style.content) },
                    onClick = {
                        expanded = false
                        haptics.perform(LevyraHapticAction.Confirm)
                        action.invoke()
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                )
            }
        }
    }
}
