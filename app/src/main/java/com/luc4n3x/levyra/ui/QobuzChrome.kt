package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.ui.i18n.LevyraQobuzSkinStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.QobuzDesign

private val QobuzTabOrder = listOf(LevyraTab.Home, LevyraTab.Explore, LevyraTab.Library, LevyraTab.Search)

private fun qobuzTabIcon(tab: LevyraTab, active: Boolean, entry: LevyraTabEntry): ImageVector = when (tab) {
    LevyraTab.Home -> if (active) Icons.Filled.Explore else Icons.Outlined.Explore
    LevyraTab.Explore -> if (active) Icons.AutoMirrored.Filled.MenuBook else Icons.AutoMirrored.Outlined.MenuBook
    LevyraTab.Library -> if (active) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic
    LevyraTab.Search -> if (active) Icons.Filled.Search else Icons.Outlined.Search
    else -> if (active) entry.selectedIcon else entry.unselectedIcon
}

@Composable
internal fun QobuzBottomTabs(
    entries: List<LevyraTabEntry>, selected: LevyraTab, applySystemInset: Boolean, modifier: Modifier,
    onSelect: (LevyraTabEntry, Boolean) -> Unit
) {
    val code = LocalLevyraStrings.current.code
    val copy = remember(code) { LevyraQobuzSkinStrings.forCode(code) }
    val ordered = remember(entries) {
        QobuzTabOrder.mapNotNull { tab -> entries.firstOrNull { it.tab == tab } } + entries.filter { it.tab !in QobuzTabOrder }
    }
    Column(modifier.fillMaxWidth().background(QobuzDesign.navBar)) {
        Row(
            Modifier.fillMaxWidth().height(QobuzDesign.navHeight).selectableGroup(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ordered.forEach { entry ->
                val active = entry.tab == selected
                val label = when (entry.tab) {
                    LevyraTab.Home -> copy.discover
                    LevyraTab.Explore -> copy.magazine
                    else -> entry.label
                }
                val tint = if (active) QobuzDesign.textPrimary else QobuzDesign.textSecondary
                Column(
                    Modifier.weight(1f).fillMaxHeight().selectable(active, role = Role.Tab, onClick = { onSelect(entry, active) }),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    Icon(qobuzTabIcon(entry.tab, active, entry), null, tint = tint, modifier = Modifier.size(QobuzDesign.navIcon))
                    Spacer(Modifier.height(2.dp))
                    Text(
                        label, color = tint, fontSize = 14.sp, lineHeight = 18.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (applySystemInset) Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
internal fun QobuzMiniControls(playing: Boolean, resolving: Boolean, favorite: Boolean, onFavorite: () -> Unit, onToggle: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(48.dp).clip(CircleShape)
                .levyraPressable(onClick = onFavorite, role = Role.Switch, haptic = LevyraHapticAction.Confirm)
                .semantics {
                    contentDescription = if (favorite) strings.removeFromFavorites else strings.addToFavorites
                    toggleableState = ToggleableState(favorite)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null, tint = QobuzDesign.iconPrimary, modifier = Modifier.size(26.dp))
        }
        Box(
            Modifier.size(48.dp).clip(CircleShape)
                .levyraPressable(onClick = onToggle, enabled = !resolving, onClickLabel = if (playing) strings.pause else strings.play, role = Role.Button),
            contentAlignment = Alignment.Center
        ) {
            if (resolving) LevyraLoadingIndicator(color = QobuzDesign.iconPrimary, modifier = Modifier.size(22.dp))
            else Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (playing) strings.pause else strings.play,
                tint = QobuzDesign.iconPrimary, modifier = Modifier.size(34.dp))
        }
    }
}
