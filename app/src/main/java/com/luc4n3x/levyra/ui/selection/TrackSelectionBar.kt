package com.luc4n3x.levyra.ui.selection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraGlass
import com.luc4n3x.levyra.ui.theme.LevyraGlassBorder
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraText

@Composable
internal fun TrackSelectionBar(
    state: TrackSelectionState,
    allVisibleIds: List<String>,
    onPlayNext: (() -> Unit)?,
    onAddToQueue: (() -> Unit)?,
    onAddToPlaylist: (() -> Unit)?,
    onFavorite: (() -> Unit)?,
    onDownload: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    if (!state.isActive) return
    val strings = LocalLevyraStrings.current
    val allSelected = allVisibleIds.isNotEmpty() &&
        state.selectedIds.size == allVisibleIds.distinct().size &&
        allVisibleIds.all(state.selectedIds::contains)

    Surface(
        color = LevyraGlass,
        border = BorderStroke(1.dp, LevyraGlassBorder),
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 14.dp,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            SelectionAction(
                label = strings.selectAll,
                icon = { Icon(Icons.Rounded.SelectAll, contentDescription = null) },
                onClick = {
                    if (allSelected) state.deselectAll() else state.selectAll(allVisibleIds)
                }
            )
            Spacer(Modifier.size(2.dp))
            onPlayNext?.let {
                SelectionAction(
                    label = strings.playNext,
                    icon = { Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = null) },
                    onClick = it
                )
            }
            onAddToQueue?.let {
                SelectionAction(
                    label = strings.addToQueue,
                    icon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null) },
                    onClick = it
                )
            }
            onAddToPlaylist?.let {
                SelectionAction(
                    label = strings.addToPlaylist,
                    icon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null) },
                    onClick = it
                )
            }
            onFavorite?.let {
                SelectionAction(
                    label = strings.favorite,
                    icon = { Icon(Icons.Rounded.Favorite, contentDescription = null) },
                    onClick = it
                )
            }
            onDownload?.let {
                SelectionAction(
                    label = strings.download,
                    icon = { Icon(Icons.Rounded.Download, contentDescription = null) },
                    onClick = it
                )
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
    Surface(
        color = Color.Transparent,
        shape = CircleShape,
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = label }
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            androidx.compose.material3.ProvideTextStyle(
                androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(color = LevyraMuted)
            ) {
                icon()
            }
        }
    }
}
