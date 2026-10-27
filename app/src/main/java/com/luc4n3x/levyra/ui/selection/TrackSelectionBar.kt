package com.luc4n3x.levyra.ui.selection

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.levyraTabContentBottomPadding
import com.luc4n3x.levyra.ui.theme.LevyraIsNocturne
import com.luc4n3x.levyra.ui.theme.LevyraIsPrism
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.components.LevyraExpressiveIconButton
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraGlass
import com.luc4n3x.levyra.ui.theme.LevyraGlassBorder
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraText

internal data class TrackSelectionActions(
    val onPlayNext: (() -> Unit)? = null,
    val onAddToQueue: (() -> Unit)? = null,
    val onAddToPlaylist: (() -> Unit)? = null,
    val onFavorite: (() -> Unit)? = null,
    val onDownload: (() -> Unit)? = null
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TrackSelectionBar(
    state: TrackSelectionState,
    allVisibleIds: List<String>,
    actions: TrackSelectionActions,
    modifier: Modifier = Modifier
) {
    if (!state.isActive) return
    if (!LevyraIsPrism && !LevyraIsNocturne) {
        ExpressiveTrackSelectionBar(state, allVisibleIds, actions, modifier)
        return
    }
    val strings = LocalLevyraStrings.current

    SelectionBarEntrance(modifier) {
        Surface(
            color = LevyraGlass,
            border = BorderStroke(1.dp, LevyraGlassBorder),
            shape = MaterialTheme.shapes.large,
            shadowElevation = 14.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SelectionAction(
                        label = strings.close,
                        icon = { Icon(Icons.Rounded.Close, contentDescription = null) },
                        onClick = state::exit
                    )
                    Text(
                        text = strings.formatTrackCount(state.count),
                        color = LevyraText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    )
                    SelectionAction(
                        label = strings.selectAll,
                        icon = { Icon(Icons.Rounded.SelectAll, contentDescription = null) },
                        onClick = { state.toggleSelectAll(allVisibleIds) }
                    )
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    actions.onPlayNext?.let {
                        SelectionAction(
                            label = strings.playNext,
                            icon = { Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = null) },
                            onClick = it
                        )
                    }
                    actions.onAddToQueue?.let {
                        SelectionAction(
                            label = strings.addToQueue,
                            icon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null) },
                            onClick = it
                        )
                    }
                    actions.onAddToPlaylist?.let {
                        SelectionAction(
                            label = strings.addToPlaylist,
                            icon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null) },
                            onClick = it
                        )
                    }
                    actions.onFavorite?.let {
                        SelectionAction(
                            label = strings.favorite,
                            icon = { Icon(Icons.Rounded.Favorite, contentDescription = null) },
                            onClick = it
                        )
                    }
                    actions.onDownload?.let {
                        SelectionAction(
                            label = strings.download,
                            icon = { Icon(Icons.Rounded.Download, contentDescription = null) },
                            onClick = it
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionAction(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    LevyraExpressiveIconButton(
        onClick = onClick,
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = label },
        colors = IconButtonDefaults.iconButtonColors(contentColor = LevyraMuted)
    ) {
        icon()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpressiveTrackSelectionBar(
    state: TrackSelectionState,
    allVisibleIds: List<String>,
    actions: TrackSelectionActions,
    modifier: Modifier
) {
    val strings = LocalLevyraStrings.current
    val animated = LocalAnimationsEnabled.current
    val scheme = MaterialTheme.colorScheme
    val selectableIds = remember(allVisibleIds) {
        allVisibleIds.mapNotNullTo(LinkedHashSet()) { id -> id.trim().takeIf(String::isNotBlank) }
    }
    val allSelected = selectableIds.isNotEmpty() && state.selectedIds.containsAll(selectableIds)

    SelectionBarEntrance(modifier) {
        Surface(
            color = scheme.surfaceContainerHigh,
            contentColor = scheme.onSurface,
            shape = RoundedCornerShape(ExpressiveSelectionCorner),
            tonalElevation = 6.dp,
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 8.dp)
                    .then(if (animated) Modifier.animateContentSize() else Modifier),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LevyraExpressiveIconButton(
                        onClick = state::exit,
                        modifier = Modifier
                            .size(40.dp)
                            .semantics { contentDescription = strings.close },
                        colors = IconButtonDefaults.filledTonalIconButtonColors()
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = null)
                    }
                    AnimatedContent(
                        targetState = state.count,
                        transitionSpec = {
                            val rising = targetState > initialState
                            if (animated) {
                                slideInVertically { height -> if (rising) height else -height } + fadeIn() togetherWith
                                    slideOutVertically { height -> if (rising) -height else height } + fadeOut()
                            } else {
                                EnterTransition.None togetherWith ExitTransition.None
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp),
                        label = "selection-count"
                    ) { count ->
                        Text(
                            text = strings.formatTrackCount(count),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    AnimatedVisibility(
                        visible = !allSelected,
                        enter = if (animated) {
                            fadeIn(tween(SelectionEnterMillis)) + scaleIn(tween(SelectionEnterMillis), initialScale = 0.92f) +
                                expandHorizontally(tween(SelectionEnterMillis, easing = FastOutSlowInEasing))
                        } else {
                            EnterTransition.None
                        },
                        exit = if (animated) {
                            fadeOut(tween(SelectionExitMillis)) + scaleOut(tween(SelectionExitMillis), targetScale = 0.92f) +
                                shrinkHorizontally(tween(SelectionExitMillis, easing = FastOutSlowInEasing))
                        } else {
                            ExitTransition.None
                        }
                    ) {
                        Button(
                            onClick = { state.selectAll(selectableIds) },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = scheme.primary,
                                contentColor = scheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Rounded.SelectAll, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strings.selectAll, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    actions.onPlayNext?.let {
                        ExpressiveSelectionAction(strings.playNext, Icons.AutoMirrored.Rounded.PlaylistPlay, it)
                    }
                    actions.onAddToQueue?.let {
                        ExpressiveSelectionAction(strings.addToQueue, Icons.AutoMirrored.Rounded.QueueMusic, it)
                    }
                    actions.onAddToPlaylist?.let {
                        ExpressiveSelectionAction(strings.addToPlaylist, Icons.AutoMirrored.Rounded.PlaylistAdd, it)
                    }
                    actions.onFavorite?.let {
                        ExpressiveSelectionAction(strings.favorite, Icons.Rounded.Favorite, it, emphasized = true)
                    }
                    actions.onDownload?.let {
                        ExpressiveSelectionAction(strings.download, Icons.Rounded.Download, it)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpressiveSelectionAction(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    emphasized: Boolean = false
) {
    Column(
        modifier = Modifier.width(ExpressiveSelectionActionWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        LevyraExpressiveIconButton(
            onClick = onClick,
            modifier = Modifier
                .size(width = 52.dp, height = 36.dp)
                .semantics { contentDescription = label },
            colors = if (emphasized) {
                IconButtonDefaults.filledIconButtonColors()
            } else {
                IconButtonDefaults.filledTonalIconButtonColors()
            }
        ) {
            Icon(icon, contentDescription = null)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private val ExpressiveSelectionCorner = 32.dp
private val ExpressiveSelectionActionWidth = 64.dp

@Composable
private fun SelectionBarEntrance(
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    val animated = LocalAnimationsEnabled.current
    val entrance = remember { MutableTransitionState(!animated).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = entrance,
        enter = fadeIn(tween(SelectionEnterMillis, easing = LinearOutSlowInEasing)) +
            slideInVertically(tween(SelectionEnterMillis, easing = FastOutSlowInEasing)) { height -> height / 4 } +
            scaleIn(
                animationSpec = tween(SelectionEnterMillis, easing = FastOutSlowInEasing),
                initialScale = 0.96f,
                transformOrigin = TransformOrigin(0.5f, 1f)
            ),
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        content()
    }
}

private const val SelectionEnterMillis = 320
private const val SelectionExitMillis = 180

@Composable
internal fun trackSelectionBarTabClearance(miniPlayerVisible: Boolean): Dp {
    val navigationInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return (levyraTabContentBottomPadding(miniPlayerVisible) - navigationInset - SelectionBarTabOverlap)
        .coerceAtLeast(12.dp)
}

internal val TrackSelectionBarListClearance = 140.dp
private val SelectionBarTabOverlap = 24.dp
