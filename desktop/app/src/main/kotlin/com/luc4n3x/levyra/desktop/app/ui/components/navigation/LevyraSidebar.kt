package com.luc4n3x.levyra.desktop.app.ui.components.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.state.Destination
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.ui.icons.OfflineIcons
import com.luc4n3x.levyra.desktop.app.ui.theme.LocalAccentColor
import com.luc4n3x.levyra.desktop.app.ui.theme.LevyraBrand
import com.luc4n3x.levyra.desktop.app.ui.components.Artwork

@Composable
fun LevyraSidebar(
    destination: Destination,
    hasActiveTrack: Boolean,
    isPlaying: Boolean,
    onNavigate: (Destination) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onToggleCompact: () -> Unit = {},
    favoritesLabel: String = "",
    favoritesCount: Int = 0,
    playlists: List<com.luc4n3x.levyra.desktop.core.storage.LocalPlaylist> = emptyList(),
    onOpenPlaylist: (String) -> Unit = {}
) {
    val strings = LocalStrings.current
    val radioLabel = androidx.compose.runtime.remember(strings.languageCode) {
        com.luc4n3x.levyra.ui.i18n.LevyraLiveRadioCatalog.forCode(strings.languageCode).title
    }
    val width by animateDpAsState(if (compact) CompactWidth else ExpandedWidth, tween(180))
    Column(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        NavItem(LevyraIcons.Menu, strings.appName, selected = false, compact = true, onClick = onToggleCompact)
        Spacer(Modifier.height(6.dp))
        NavItem(LevyraIcons.Home, strings.navHome, destination == Destination.HOME, compact) { onNavigate(Destination.HOME) }
        NavItem(LevyraIcons.Search, strings.navSearch, destination == Destination.SEARCH || destination == Destination.COLLECTION, compact) {
            onNavigate(Destination.SEARCH)
        }
        NavItem(LevyraIcons.Chart, strings.navDiscover, destination == Destination.DISCOVER, compact) { onNavigate(Destination.DISCOVER) }
        NavItem(LevyraIcons.Radio, radioLabel, destination == Destination.RADIO, compact) { onNavigate(Destination.RADIO) }
        NavDivider()
        NavItem(OfflineIcons.Library, strings.navLibrary, destination == Destination.LIBRARY || destination == Destination.PLAYLIST, compact) {
            onNavigate(Destination.LIBRARY)
        }
        NavItem(OfflineIcons.Folder, strings.localMusic, destination == Destination.LOCAL_MUSIC, compact) { onNavigate(Destination.LOCAL_MUSIC) }
        if (hasActiveTrack) {
            NavItem(
                icon = LevyraIcons.Disc,
                label = strings.navNowPlaying.sentenceCase(),
                selected = destination == Destination.NOW_PLAYING,
                compact = compact,
                trailing = { PlayingGlyph(active = isPlaying) },
                onClick = { onNavigate(Destination.NOW_PLAYING) }
            )
        }
        if (!compact && (favoritesCount > 0 || playlists.isNotEmpty())) {
            NavDivider()
            androidx.compose.foundation.lazy.LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (favoritesCount > 0) {
                    item(key = "favorites") {
                        LibraryEntry(null, favoritesLabel, favoritesCount, isFavorites = true) { onNavigate(Destination.LIBRARY) }
                    }
                }
                items(playlists.size, key = { playlists[it].id }) { index ->
                    val playlist = playlists[index]
                    LibraryEntry(playlist.artworkUrl, playlist.name, playlist.tracks.size, isFavorites = false) { onOpenPlaylist(playlist.id) }
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
        NavItem(LevyraIcons.Settings, strings.navSettings, destination == Destination.SETTINGS, compact) { onNavigate(Destination.SETTINGS) }
    }
}

@Composable
private fun NavDivider() {
    Box(
        Modifier
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    compact: Boolean,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val focused by interaction.collectIsFocusedAsState()
    val pressed by interaction.collectIsPressedAsState()
    val onSurface = MaterialTheme.colorScheme.onSurface
    val fill by animateColorAsState(
        when {
            selected -> LevyraBrand.soft.copy(alpha = 0.16f)
            pressed -> onSurface.copy(alpha = 0.04f)
            hovered || focused -> onSurface.copy(alpha = 0.06f)
            else -> Color.Transparent
        },
        tween(140)
    )
    val item: @Composable () -> Unit = {
        Box(
            Modifier
                .fillMaxWidth()
                .height(ItemHeight)
                .clip(ItemShape)
                .background(fill)
                .hoverable(interaction)
                .clickable(interactionSource = interaction, indication = null, role = Role.Tab, onClick = onClick)
                .semantics { this.selected = selected }
        ) {
            Row(
                Modifier.fillMaxHeight().padding(start = 16.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) LevyraBrand.soft else onSurface.copy(alpha = 0.86f),
                    modifier = Modifier.size(21.dp)
                )
                if (!compact) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected) LevyraBrand.soft else onSurface.copy(alpha = 0.86f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    trailing?.invoke()
                }
            }
        }
    }
    if (compact) {
        TooltipArea(tooltip = { NavTooltip(label) }, delayMillis = 450) { item() }
    } else {
        item()
    }
}

@Composable
private fun NavTooltip(label: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shadowElevation = 6.dp
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
    }
}

@Composable
private fun PlayingGlyph(active: Boolean) {
    val color = if (active) LocalAccentColor.current else MaterialTheme.colorScheme.onSurfaceVariant
    Row(Modifier.height(12.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        listOf(7.dp, 12.dp, 9.dp).forEach { height ->
            Box(Modifier.width(2.5.dp).height(if (active) height else 4.dp).clip(RoundedCornerShape(1.dp)).background(color))
        }
    }
}

private fun String.sentenceCase(): String =
    if (any(Char::isLowerCase)) this else lowercase().replaceFirstChar(Char::titlecase)

private val ExpandedWidth = 272.dp
private val CompactWidth = 60.dp
private val ItemHeight = 48.dp
private val ItemShape = RoundedCornerShape(24.dp)

@Composable
private fun LibraryEntry(artworkUrl: String?, title: String, count: Int, isFavorites: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isFavorites) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF3A1C2C)), contentAlignment = Alignment.Center) {
                Icon(LevyraIcons.HeartFilled, contentDescription = null, tint = Color(0xFFF2A4C4), modifier = Modifier.size(22.dp))
            }
        } else {
            Artwork(artworkUrl.orEmpty(), Modifier.size(48.dp), cornerRadius = 12.dp, iconSize = 18.dp)
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$count ${LocalStrings.current.playlistTracks}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
