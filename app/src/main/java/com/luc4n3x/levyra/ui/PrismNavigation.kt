package com.luc4n3x.levyra.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.domain.NavTuning
import com.luc4n3x.levyra.ui.theme.*

@Composable
internal fun PrismBottomTabs(tuning: NavTuning, selected: LevyraTab, onSelect: (LevyraTab) -> Unit, applySystemInset: Boolean) {
    val all = rememberLevyraTabEntries()
    val entries = remember(all, tuning.order, tuning.hidden) {
        tuning.visibleOrder().mapNotNull { tab -> all.firstOrNull { it.tab == tab } }
    }
    val colors = MaterialTheme.colorScheme
    val haptics = LocalLevyraHaptics.current
    val measuredHeight = LocalNocturneNavigationHeight.current
    val density = LocalDensity.current
    Column(Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth().onSizeChanged { size ->
                measuredHeight?.value = with(density) { size.height.toDp() }
            }.padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(28.dp), color = prismGlassColor(),
            border = BorderStroke(1.dp, colors.outlineVariant)
        ) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup().padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                entries.forEach { entry ->
                    val active = entry.tab == selected
                    Column(
                        Modifier.weight(1f).fillMaxHeight().heightIn(min = 56.dp)
                            .background(if (active) colors.primaryContainer else Color.Transparent, RoundedCornerShape(22.dp))
                            .selectable(active, role = Role.Tab, onClick = {
                                if (!active && tuning.haptics) haptics.perform(LevyraHapticAction.SeekSnap)
                                onSelect(entry.tab)
                            }).padding(horizontal = 4.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                    ) {
                        val tint = if (active) colors.onPrimaryContainer else colors.onSurfaceVariant
                        Icon(entry.unselectedIcon, null, tint = tint, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.height(4.dp))
                        Text(entry.label, color = tint, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                    }
                }
            }
        }
        if (applySystemInset) Spacer(Modifier.navigationBarsPadding())
    }
}
