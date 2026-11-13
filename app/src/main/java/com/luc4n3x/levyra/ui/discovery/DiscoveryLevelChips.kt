package com.luc4n3x.levyra.ui.discovery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luc4n3x.levyra.feature.discovery.BubbleLevel
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.smartMusicCopy
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics

internal class DiscoveryLevelChipStyle(
    val shape: Shape,
    val textStyle: TextStyle,
    val selectedContainer: Color,
    val selectedContent: Color,
    val container: Color,
    val content: Color,
    val border: BorderStroke? = null,
    val uppercase: Boolean = false
)

@Composable
internal fun HomeDiscoveryLevelChips(style: DiscoveryLevelChipStyle, modifier: Modifier = Modifier) {
    val handle = LocalHomeDiscovery.current ?: return
    val state = handle.state.collectAsStateWithLifecycle()
    val level by remember(state) { derivedStateOf { state.value.request.level } }
    DiscoveryLevelChips(level, style, handle.onOpenBubble, modifier)
}

@Composable
internal fun DiscoveryLevelChips(
    current: BubbleLevel,
    style: DiscoveryLevelChipStyle,
    onPick: (BubbleLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { smartMusicCopy(strings.code) }
    val haptics = LocalLevyraHaptics.current
    FlowRow(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        BubbleLevel.entries.forEach { level ->
            val selected = level == current
            val label = copy[bubbleLevelName(level)].let { if (style.uppercase) it.uppercase() else it }
            val tint = if (selected) style.selectedContent else style.content
            val border = style.border
            Row(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .heightIn(min = 36.dp)
                    .clip(style.shape)
                    .background(if (selected) style.selectedContainer else style.container)
                    .then(if (border != null && !selected) Modifier.border(border, style.shape) else Modifier)
                    .selectable(selected = selected, role = Role.RadioButton) {
                        haptics.perform(LevyraHapticAction.Confirm)
                        onPick(level)
                    }
                    .padding(start = 10.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(bubbleLevelIcon(level), contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                Text(label, style = style.textStyle, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
