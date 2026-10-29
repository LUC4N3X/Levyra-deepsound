package com.luc4n3x.levyra.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.InstantArtworkPlaceholder
import com.luc4n3x.levyra.ui.artwork.SeamlessArtworkImage
import com.luc4n3x.levyra.ui.components.PlayerIcon
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
import com.luc4n3x.levyra.ui.theme.QobuzDesign

private val QobuzSheetArtwork = 56.dp
private val QobuzSheetRowMinHeight = 56.dp
private val QobuzSheetIcon = 24.dp

@Composable
internal fun QobuzPlayerActions(
    track: Track,
    artworkUrl: String,
    actions: List<PlayerSheetAction>,
    discoverContent: (@Composable () -> Unit)?,
    onPerform: (PlayerSheetAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp)
    ) {
        QobuzPlayerActionsHeader(track = track, artworkUrl = artworkUrl)
        HorizontalDivider(thickness = 1.dp, color = QobuzDesign.separator)
        actions.forEachIndexed { index, action ->
            key(action.key) {
                QobuzPlayerActionRow(action = action, onPerform = onPerform)
                if (index < actions.lastIndex) {
                    HorizontalDivider(thickness = 1.dp, color = QobuzDesign.separator)
                }
            }
        }
        discoverContent?.let {
            Spacer(modifier = Modifier.size(16.dp))
            Column(modifier = Modifier.padding(horizontal = 20.dp)) { it() }
        }
    }
}

@Composable
private fun QobuzPlayerActionsHeader(track: Track, artworkUrl: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SeamlessArtworkImage(
            url = artworkUrl,
            modifier = Modifier
                .size(QobuzSheetArtwork)
                .clip(RoundedCornerShape(4.dp))
        ) {
            InstantArtworkPlaceholder(track = track, modifier = Modifier.fillMaxSize())
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = QobuzDesign.textPrimary,
                fontSize = 17.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(17.sp),
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = track.artist,
                color = QobuzDesign.textSecondary,
                fontSize = 15.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(15.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QobuzPlayerActionRow(
    action: PlayerSheetAction,
    onPerform: (PlayerSheetAction) -> Unit
) {
    val labelColor = if (action.enabled) QobuzDesign.textPrimary else QobuzDesign.textDisabled
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = QobuzSheetRowMinHeight)
            .clickable(enabled = action.enabled && !action.busy, role = Role.Button) { onPerform(action) }
            .semantics {
                contentDescription = listOfNotNull(action.label, action.value).joinToString(", ")
                if (action.toggle) stateDescription = if (action.active) "On" else "Off"
            }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (action.busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(QobuzSheetIcon),
                strokeWidth = 2.dp,
                color = QobuzDesign.iconPrimary
            )
        } else {
            PlayerIcon(
                action.icon,
                if (action.enabled) QobuzDesign.iconPrimary else QobuzDesign.textDisabled,
                Modifier.size(QobuzSheetIcon)
            )
        }
        Spacer(modifier = Modifier.width(24.dp))
        Text(
            text = action.label,
            color = labelColor,
            fontSize = 17.sp,
            lineHeight = LevyraTypeRhythm.lineHeight(17.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        action.value?.let { value ->
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = value,
                color = QobuzDesign.textSecondary,
                fontSize = 15.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(15.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
