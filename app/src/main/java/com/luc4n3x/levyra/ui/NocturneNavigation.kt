package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.components.LocalDockMorph
import com.luc4n3x.levyra.ui.components.dockMorphTray
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.domain.NavTuning
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.Nocturne78Icons
import com.luc4n3x.levyra.ui.theme.Nocturne78Palette

internal val LocalNocturneNavigationHeight = compositionLocalOf<MutableState<Dp>?> { null }

@Composable
internal fun NocturneBottomTabs(tuning: NavTuning, selected: LevyraTab, onSelect: (LevyraTab) -> Unit, applySystemInset: Boolean) {
    val measuredHeight = LocalNocturneNavigationHeight.current
    val density = LocalDensity.current
    val haptics = LocalLevyraHaptics.current
    val all = rememberLevyraTabEntries()
    val entries = remember(all, tuning.order, tuning.hidden) {
        tuning.visibleOrder().mapNotNull { tab -> all.firstOrNull { it.tab == tab } }
    }
    val p = Nocturne78Palette
    Column(Modifier.fillMaxWidth().background(p.panel)) {
        HorizontalDivider(color = p.outline, thickness = 1.dp)
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
                .onSizeChanged { size -> measuredHeight?.value = with(density) { size.height.toDp() } }
                .selectableGroup()
                .dockMorphTray(LocalDockMorph.current, entries.size, entries.indexOfFirst { it.tab == selected }),
            verticalAlignment = Alignment.CenterVertically
        ) {
            entries.forEach { entry ->
                val active = entry.tab == selected
                val tint = if (active) p.cyan else p.muted
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 64.dp)
                        .selectable(selected = active, onClick = {
                            if (!active && tuning.haptics) haptics.perform(LevyraHapticAction.SeekSnap)
                            onSelect(entry.tab)
                        }, role = Role.Tab)
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(Modifier.width(20.dp).height(2.dp).background(if (active) p.cyan else Color.Transparent))
                    Spacer(Modifier.height(5.dp))
                    Icon(Nocturne78Icons.resolve(entry.unselectedIcon), null, tint = tint, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        entry.label, color = tint, fontSize = 11.sp, lineHeight = 15.sp,
                        fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (applySystemInset) Spacer(Modifier.navigationBarsPadding())
    }
}
