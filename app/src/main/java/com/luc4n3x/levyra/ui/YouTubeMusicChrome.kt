package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.LevyraTab
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import com.luc4n3x.levyra.ui.theme.YouTubeMusicDesign

private val YouTubeMusicTabOrder = listOf(LevyraTab.Home, LevyraTab.Explore, LevyraTab.Library, LevyraTab.Search)

private fun youTubeMusicTabIcon(tab: LevyraTab, active: Boolean, entry: LevyraTabEntry): ImageVector = when (tab) {
    LevyraTab.Home -> if (active) Icons.Filled.Home else Icons.Outlined.Home
    LevyraTab.Explore -> if (active) Icons.Filled.Explore else Icons.Outlined.Explore
    LevyraTab.Library -> if (active) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder
    LevyraTab.Search -> if (active) Icons.Filled.Search else Icons.Outlined.Search
    else -> if (active) entry.selectedIcon else entry.unselectedIcon
}

@Composable
internal fun YouTubeMusicBottomTabs(
    entries: List<LevyraTabEntry>, selected: LevyraTab, applySystemInset: Boolean, modifier: Modifier,
    onSelect: (LevyraTabEntry, Boolean) -> Unit
) {
    val ordered = remember(entries) {
        YouTubeMusicTabOrder.mapNotNull { tab -> entries.firstOrNull { it.tab == tab } } + entries.filter { it.tab !in YouTubeMusicTabOrder }
    }
    Column(modifier.fillMaxWidth().background(YouTubeMusicDesign.navBar)) {
        Row(
            Modifier.fillMaxWidth().height(YouTubeMusicDesign.navHeight).selectableGroup(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ordered.forEach { entry ->
                val active = entry.tab == selected
                Column(
                    Modifier.weight(1f).fillMaxHeight()
                        .selectable(active, role = Role.Tab, onClick = { onSelect(entry, active) }),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        youTubeMusicTabIcon(entry.tab, active, entry), null,
                        tint = YouTubeMusicDesign.text, modifier = Modifier.size(YouTubeMusicDesign.navIcon)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        entry.label, color = YouTubeMusicDesign.text, fontSize = 12.sp, lineHeight = 14.sp,
                        fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (applySystemInset) Spacer(Modifier.navigationBarsPadding())
    }
}

@Composable
internal fun YouTubeMusicDetailDock(
    track: com.luc4n3x.levyra.domain.Track,
    isPlaying: Boolean,
    isResolving: Boolean,
    progress: Float,
    onToggle: () -> Unit,
    onOpenPlayer: () -> Unit,
    modifier: Modifier
): Boolean {
    if (!com.luc4n3x.levyra.ui.theme.LevyraIsYouTubeMusic) return false
    val navInset = WindowInsets.navigationBars.getBottom(androidx.compose.ui.platform.LocalDensity.current)
    Column(
        modifier.layout { measurable, constraints ->
            val side = DetailDockSideInset.roundToPx()
            val spill = DetailDockBottomInset.roundToPx() + navInset
            val width = constraints.maxWidth + side * 2
            val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
            layout(constraints.maxWidth, (placeable.height - spill).coerceAtLeast(0)) { placeable.place(-side, 0) }
        }.background(YouTubeMusicDesign.miniPlayer).clickable(onClick = onOpenPlayer)
    ) {
        Row(
            Modifier.fillMaxWidth().height(YouTubeMusicDesign.miniHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(16.dp))
            Box(Modifier.size(YouTubeMusicDesign.miniThumb).background(YouTubeMusicDesign.placeholder)) {
                CoverImage(track, Modifier.fillMaxSize())
            }
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(track.title, color = YouTubeMusicDesign.text, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = YouTubeMusicDesign.textSecondary, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.size(56.dp).clickable(onClick = onToggle), contentAlignment = Alignment.Center) {
                if (isResolving) com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Icon(
                    if (isPlaying) androidx.compose.material.icons.Icons.Rounded.Pause else androidx.compose.material.icons.Icons.Rounded.PlayArrow,
                    null, tint = Color.White, modifier = Modifier.size(YouTubeMusicDesign.miniIcon)
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(YouTubeMusicDesign.divider)) {
            Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().background(Color.White))
        }
        Box(Modifier.fillMaxWidth().height(DetailDockBottomInset).background(YouTubeMusicDesign.navBar))
        Box(Modifier.fillMaxWidth().background(YouTubeMusicDesign.navBar).navigationBarsPadding())
    }
    return true
}

private val DetailDockSideInset = 18.dp
private val DetailDockBottomInset = 14.dp
