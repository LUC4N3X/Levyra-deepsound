package com.luc4n3x.levyra.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.CoverImage
import com.luc4n3x.levyra.ui.i18n.LevyraQobuzSkinStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.rememberSpotifyCoverTone
import com.luc4n3x.levyra.ui.theme.QobuzDesign
import com.luc4n3x.levyra.viewmodel.LevyraUiState

@Composable
internal fun QobuzQueueScreen(
    state: LevyraUiState,
    onPlay: (Track) -> Unit,
    onToggleRadio: () -> Unit,
    onClose: () -> Unit
) {
    val onTrackActions: (Track) -> Unit = {}
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraQobuzSkinStrings.forCode(strings.code) }
    val current = state.currentTrack
    val toneTrack = current ?: state.queue.firstOrNull()
    val field = if (toneTrack != null) rememberSpotifyCoverTone(toneTrack, ::qobuzPlayerField).value else Color(0xFF3D3D3D)
    var history by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = history) { history = false }
    val currentIndex = state.queue.indexOfFirst { it.id == current?.id }
    val previous = if (currentIndex > 0) state.queue.take(currentIndex) else emptyList()
    val upcoming = if (currentIndex >= 0) state.queue.drop(currentIndex + 1) else state.queue
    Column(Modifier.fillMaxSize().background(field).statusBarsPadding().navigationBarsPadding()) {
        Box(
            Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = strings.close, onClick = onClose).padding(top = 12.dp, bottom = 12.dp),
            contentAlignment = Alignment.Center
        ) { Box(Modifier.width(40.dp).height(5.dp).clip(RoundedCornerShape(50)).background(QobuzDesign.playerContentTertiary)) }
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (history) copy.history else copy.queueTitle, style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp, fontWeight = FontWeight.Bold),
                color = Color.White, modifier = Modifier.weight(1f).semantics { heading() })
            Row(
                Modifier.clip(RoundedCornerShape(6.dp)).background(if (history) Color.White else QobuzDesign.playerFill)
                    .clickable(role = Role.Switch) { history = !history }.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tint = if (history) Color.Black else Color.White
                Icon(Icons.Rounded.History, null, tint = tint, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(copy.history, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp), color = tint)
            }
        }
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 120.dp)) {
            if (history) {
                itemsIndexed(state.recentListens, key = { index, track -> "h-$index-${track.id}" }) { _, track ->
                    QobuzQueueRow(track, dimmed = false, copy.explicit, { onPlay(track) }, { onTrackActions(track) })
                }
                return@LazyColumn
            }
            if (previous.isNotEmpty()) {
                item(key = "previous-title") { QobuzQueueTitle(copy.previous) }
                itemsIndexed(previous, key = { index, track -> "p-$index-${track.id}" }) { _, track ->
                    QobuzQueueRow(track, dimmed = true, copy.explicit, { onPlay(track) }, { onTrackActions(track) })
                }
            }
            if (current != null) {
                item(key = "current-title") { QobuzQueueTitle(copy.current) }
                item(key = "current") { QobuzQueueRow(current, dimmed = false, copy.explicit, {}, { onTrackActions(current) }) }
            }
            item(key = "autoplay") {
                Row(Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(copy.autoplay, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold), color = Color.White,
                        modifier = Modifier.weight(1f).semantics { heading() })
                    QobuzQueueSwitch(state.radioEnabled, copy.autoplay, onToggleRadio)
                }
            }
            itemsIndexed(upcoming, key = { index, track -> "u-$index-${track.id}" }) { _, track ->
                QobuzQueueRow(track, dimmed = false, copy.explicit, { onPlay(track) }, { onTrackActions(track) })
            }
        }
    }
}

@Composable
private fun QobuzQueueTitle(text: String) {
    Text(text, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold), color = Color.White,
        modifier = Modifier.padding(top = 32.dp, bottom = 8.dp).semantics { heading() })
}

@Composable
private fun QobuzQueueRow(track: Track, dimmed: Boolean, explicitLabel: String, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 80.dp).alpha(if (dimmed) 0.55f else 1f).combinedClickable(onClick = onClick, onLongClick = onLongClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(60.dp).clip(RoundedCornerShape(4.dp)).background(QobuzDesign.playerFill)) { CoverImage(track, Modifier.fillMaxSize()) }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp, fontWeight = FontWeight.Medium), color = Color.White,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (track.explicit) {
                    Box(Modifier.size(14.dp).clip(RoundedCornerShape(2.dp)).background(QobuzDesign.playerContentTertiary), contentAlignment = Alignment.Center) {
                        Text("E", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Text(track.artist, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp), color = QobuzDesign.playerContentSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun QobuzQueueSwitch(checked: Boolean, label: String, onToggle: () -> Unit) {
    Box(
        Modifier.size(width = 64.dp, height = 40.dp).clip(RoundedCornerShape(50))
            .background(if (checked) QobuzDesign.playerFillStrong else QobuzDesign.playerFill)
            .toggleable(value = checked, role = Role.Switch, onValueChange = { onToggle() }).semantics { },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(Modifier.padding(5.dp).size(30.dp).clip(CircleShape).background(if (checked) Color.White else QobuzDesign.playerContentSecondary))
    }
}
