package com.luc4n3x.levyra.ui.library

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Button
import androidx.compose.ui.semantics.Role
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
import com.luc4n3x.levyra.ui.theme.LevyraType

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.automationCopy
import com.luc4n3x.levyra.ui.i18n.playlistImportCopy
import com.luc4n3x.levyra.ui.i18n.playlistImportDismissMessage
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm

@Composable
internal fun LibraryImportPlaylistCard(
    onClick: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    val strings = LocalLevyraStrings.current
    val copy = strings.playlistImportCopy()
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surfaceContainer,
        shape = LevyraCardDesign.SurfaceShape,
        modifier = Modifier.fillMaxWidth().clip(LevyraCardDesign.SurfaceShape)
            .levyraPressable(onClick = onClick, role = Role.Button)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = colors.primaryContainer, shape = LevyraCardDesign.ThumbShape) {
                    Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null, tint = colors.onPrimaryContainer, modifier = Modifier.padding(12.dp).size(24.dp))
                }
                Text(copy.title, style = LevyraType.contentTitle, color = colors.onSurface, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Close, playlistImportDismissMessage(strings.code), tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
            Text(copy.subtitle, color = colors.onSurfaceVariant, style = LevyraType.metadata)
        }
    }
}

@Composable
internal fun LibraryPlaylistEmpty(onCreate: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surfaceContainerLow, shape = LevyraCardDesign.SurfaceShape, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(108.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(80.dp).offset(x = 12.dp, y = (-8).dp).clip(LevyraCardDesign.ArtworkShape).background(colors.secondaryContainer))
                Surface(color = colors.primaryContainer, shape = LevyraCardDesign.ArtworkShape, modifier = Modifier.size(80.dp).offset(x = (-8).dp, y = 8.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null, tint = colors.onPrimaryContainer, modifier = Modifier.size(32.dp))
                    }
                }
            }
            Text(strings.createFirstPlaylist, style = LevyraType.screenTitle, color = colors.onSurface)
            Text(strings.createFirstPlaylistSubtitle, style = LevyraType.metadata, color = colors.onSurfaceVariant)
            Button(onClick = onCreate, shape = LevyraCardDesign.ArtworkShape, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(strings.newPlaylist, style = LevyraType.cardTitle)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LibraryImportPlaylistCompactAction(
    onClick: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = strings.playlistImportCopy()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = LevyraCardDesign.ArtworkShape,
            modifier = Modifier
                .sizeIn(minHeight = 48.dp)
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(onClick = onClick)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = null,
                    tint = LevyraCyan,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = copy.action,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = LevyraType.cardTitle
                )
            }
        }
    }
}
