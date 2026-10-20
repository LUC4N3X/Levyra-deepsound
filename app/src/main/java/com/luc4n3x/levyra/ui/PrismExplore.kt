package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.theme.*
import com.luc4n3x.levyra.domain.Track

@Composable
internal fun RowScope.PrismMoodTile(label: String, portrait: String, active: Boolean, onClick: () -> Unit, onMix: (() -> Unit)?, mixLabel: String) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 24.dp, bottomEnd = 10.dp)
    Column(
        Modifier.weight(1f).clip(shape).background(colors.surfaceContainer)
            .border(1.dp, if (active) colors.primary else colors.outlineVariant, shape)
            .semantics { selected = active }
            .levyraPressable(onClick = onClick, onClickLabel = label, role = Role.Button, onLongClick = onMix, onLongClickLabel = if (onMix != null) mixLabel else null)
            .padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(88.dp).clip(RoundedCornerShape(16.dp)).background(
            Brush.horizontalGradient(listOf(colors.primaryContainer, colors.tertiaryContainer))
        )) {
            if (portrait.isNotBlank()) AsyncImage(portrait, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.align(Alignment.CenterEnd).width(88.dp).fillMaxHeight().clip(RoundedCornerShape(topStart = 36.dp, bottomStart = 36.dp)))
            Box(Modifier.align(Alignment.CenterStart).padding(start = 12.dp).width(3.dp).height(32.dp).background(colors.onPrimaryContainer, RoundedCornerShape(2.dp)))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, color = colors.onSurface, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 3, overflow = TextOverflow.Ellipsis)
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = colors.primary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
internal fun PrismFreshRelease(track: Track, title: String, subtitle: String, label: String, enabled: Boolean, onOpen: () -> Unit, actions: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(colors.surfaceContainer)
            .levyraPressable(onClick = onOpen, enabled = enabled, onClickLabel = label, role = Role.Button).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp))) { CoverImage(track, Modifier.fillMaxSize(), highRes = true) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = colors.onSurface, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            actions()
        }
    }
}
