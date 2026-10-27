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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.components.LocalDockMorph
import com.luc4n3x.levyra.ui.components.dockMorphTray
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.domain.NavTuning
import com.luc4n3x.levyra.ui.theme.*

@Composable
internal fun PrismBottomTabs(tuning: NavTuning, selected: LevyraTab, onSelect: (LevyraTab) -> Unit, applySystemInset: Boolean) {
    val all = rememberLevyraTabEntries()
    val entries = remember(all, tuning.order, tuning.hidden) {
        tuning.visibleOrder().mapNotNull { tab -> all.firstOrNull { it.tab == tab } }
    }
    val haptics = LocalLevyraHaptics.current
    val measuredHeight = LocalNocturneNavigationHeight.current
    val density = LocalDensity.current
    val torque = LevyraIsTorque
    val skinModifier = Modifier.onSizeChanged { size -> measuredHeight?.value = with(density) { size.height.toDp() } }
    val skinSelect: (LevyraTabEntry, Boolean) -> Unit = { entry, active ->
        if (!active && tuning.haptics) haptics.perform(LevyraHapticAction.SeekSnap)
        onSelect(entry.tab)
    }
    if (SkinBottomTabs(entries, selected, applySystemInset, skinModifier, skinSelect)) return
    Column(Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth().onSizeChanged { size ->
                measuredHeight?.value = with(density) { size.height.toDp() }
            },
            shape = if (torque) RectangleShape else RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), color = prismGlassColor(),
            border = if (LevyraActivePalette.isLight) BorderStroke(PrismDesign.hairline, PrismAccent.Hairline) else null
        ) {
            Column {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup().padding(horizontal = 6.dp, vertical = 4.dp).dockMorphTray(LocalDockMorph.current, entries.size, entries.indexOfFirst { it.tab == selected }, leading = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                entries.forEach { entry ->
                    val active = entry.tab == selected
                    PrismNavigationTab(entry, active) {
                        if (!active && tuning.haptics) haptics.perform(LevyraHapticAction.SeekSnap)
                        onSelect(entry.tab)
                    }
                }
            }
            if (applySystemInset) Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
private fun SkinBottomTabs(
    entries: List<LevyraTabEntry>, selected: LevyraTab, applySystemInset: Boolean, modifier: Modifier,
    onSelect: (LevyraTabEntry, Boolean) -> Unit
): Boolean {
    when {
        LevyraIsSpotify -> SpotifyBottomTabs(entries, selected, applySystemInset, modifier, onSelect)
        LevyraIsAppleMusic -> AppleMusicBottomTabs(entries, selected, applySystemInset, modifier, onSelect)
        LevyraIsYouTubeMusic -> YouTubeMusicBottomTabs(entries, selected, applySystemInset, modifier, onSelect)
        LevyraIsIvory -> IvoryBottomTabs(entries, selected, applySystemInset, modifier, onSelect)
        else -> return false
    }
    return true
}

@Composable
private fun RowScope.PrismNavigationTab(entry: LevyraTabEntry, active: Boolean, onClick: () -> Unit) {
    val torque = LevyraIsTorque
    val tint = if (active) LevyraCyan else LevyraMuted
    Column(
        Modifier.weight(1f).fillMaxHeight().heightIn(min = 60.dp).clip(RoundedCornerShape(PrismDesign.controlCorner))
            .selectable(active, role = Role.Tab, onClick = onClick).padding(horizontal = 2.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.width(52.dp).height(30.dp).clip(RoundedCornerShape(if (torque) 2.dp else 15.dp))
                .background(if (active && !torque) PrismAccent.Tint else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(if (active) entry.selectedIcon else entry.unselectedIcon, null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(entry.label, color = if (active) LevyraText else LevyraMuted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (torque) Box(Modifier.padding(top = 4.dp).width(20.dp).height(3.dp).background(if (active) LevyraActivePalette.blue else Color.Transparent))
    }
}

@Composable
private fun IvoryBottomTabs(
    entries: List<LevyraTabEntry>, selected: LevyraTab, applySystemInset: Boolean, modifier: Modifier,
    onSelect: (LevyraTabEntry, Boolean) -> Unit
) {
    Column(modifier.fillMaxWidth().background(LevyraActivePalette.ink)) {
        Box(Modifier.fillMaxWidth().height(PrismDesign.hairline).background(LevyraActivePalette.outline))
        Row(Modifier.fillMaxWidth().height(66.dp).selectableGroup().padding(horizontal = 4.dp).dockMorphTray(LocalDockMorph.current, entries.size, entries.indexOfFirst { it.tab == selected }, leading = 4.dp), verticalAlignment = Alignment.Bottom) {
            entries.forEach { entry ->
                val active = entry.tab == selected
                Box(
                    Modifier.weight(1f).fillMaxHeight().selectable(active, role = Role.Tab, onClick = { onSelect(entry, active) }),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (active) Box(Modifier.fillMaxWidth().fillMaxHeight(0.92f).clip(IvoryNotchShape).background(IvoryDesign.ink))
                    Column(
                        Modifier.fillMaxSize().padding(top = 10.dp, bottom = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            if (active) entry.selectedIcon else entry.unselectedIcon, null,
                            tint = if (active) IvoryDesign.onInk else LevyraText, modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            entry.label, color = if (active) IvoryDesign.onInk else LevyraMuted, style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 10.dp)
                        )
                    }
                }
            }
        }
        if (applySystemInset) Spacer(Modifier.navigationBarsPadding())
    }
}
