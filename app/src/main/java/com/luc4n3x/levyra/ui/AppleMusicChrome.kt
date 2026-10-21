package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.ui.theme.AppleMusicDesign

private val AppleMusicTabOrder = listOf(LevyraTab.Home, LevyraTab.Explore, LevyraTab.Library, LevyraTab.Search)

private fun appleMusicTabIcon(tab: LevyraTab, fallback: ImageVector): ImageVector = when (tab) {
    LevyraTab.Home -> Icons.Rounded.Home
    LevyraTab.Explore -> Icons.Rounded.GridView
    LevyraTab.Library -> Icons.Rounded.LibraryMusic
    LevyraTab.Search -> Icons.Rounded.Search
    else -> fallback
}

@Composable
internal fun AppleMusicBottomTabs(
    entries: List<LevyraTabEntry>, selected: LevyraTab, applySystemInset: Boolean, modifier: Modifier,
    onSelect: (LevyraTabEntry, Boolean) -> Unit
) {
    val ordered = remember(entries) {
        AppleMusicTabOrder.mapNotNull { tab -> entries.firstOrNull { it.tab == tab } } + entries.filter { it.tab !in AppleMusicTabOrder }
    }
    val pill = CircleShape
    Box(
        modifier.fillMaxWidth()
            .then(if (applySystemInset) Modifier.navigationBarsPadding() else Modifier)
            .padding(start = AppleMusicDesign.gutter, end = AppleMusicDesign.gutter, bottom = 6.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            Modifier.widthIn(max = 630.dp).fillMaxWidth().height(AppleMusicDesign.navHeight)
                .shadow(18.dp, pill, ambientColor = Color.Black.copy(alpha = 0.10f), spotColor = Color.Black.copy(alpha = 0.14f))
                .clip(pill)
                .background(AppleMusicDesign.chrome)
                .border(0.5.dp, AppleMusicDesign.chromeStroke, pill)
                .selectableGroup()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ordered.forEach { entry ->
                val active = entry.tab == selected
                val tint = if (active) AppleMusicDesign.accent else AppleMusicDesign.label
                Column(
                    Modifier.weight(1f).fillMaxHeight().clip(pill)
                        .selectable(active, role = Role.Tab, onClick = { onSelect(entry, active) }),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                ) {
                    Icon(appleMusicTabIcon(entry.tab, entry.selectedIcon), null, tint = tint, modifier = Modifier.size(AppleMusicDesign.navIcon - 2.dp))
                    Spacer(Modifier.height(1.dp))
                    Text(
                        entry.label, color = tint, fontSize = 11.sp, lineHeight = 13.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
