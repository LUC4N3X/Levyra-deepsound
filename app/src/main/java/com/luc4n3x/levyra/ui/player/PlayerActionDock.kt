package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.components.PlayerIcon
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraSegment

@Immutable
internal data class PlayerDockAction(
    val key: String,
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val active: Boolean = false,
    val toggle: Boolean = false,
    val busy: Boolean = false,
    val enabled: Boolean = true
)

@Composable
internal fun PlayerActionDock(
    actions: List<PlayerDockAction>,
    surfaces: PlayerSurfaceTokens,
    compact: Boolean,
    animated: Boolean,
    modifier: Modifier = Modifier
) {
    if (actions.isEmpty()) return
    val outline = if (surfaces.amoled) surfaces.outline else Color.Transparent
    Row(
        modifier = modifier
            .widthIn(max = LevyraPlayerDesign.DockMaxWidth)
            .fillMaxWidth()
            .height(if (compact) LevyraPlayerDesign.DockHeightCompact else LevyraPlayerDesign.DockHeight),
        horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.DockGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        actions.forEachIndexed { index, action ->
            key(action.key) {
                val position = when {
                    actions.size == 1 -> LevyraSegment.Single
                    index == 0 -> LevyraSegment.Leading
                    index == actions.lastIndex -> LevyraSegment.Trailing
                    else -> LevyraSegment.Middle
                }
                val tint by animateColorAsState(
                    targetValue = if (action.active) surfaces.activeContent else surfaces.contentMuted,
                    animationSpec = if (animated) LevyraPlayerDesign.standardTween(200) else snap(),
                    label = "player-dock-tint"
                )
                PlayerSegmentButton(
                    position = position,
                    weight = 1f,
                    container = if (action.active) surfaces.active else surfaces.controlQuiet,
                    innerCorner = LevyraPlayerDesign.DockInnerCorner,
                    contentDescription = action.label,
                    animated = animated,
                    enabled = action.enabled,
                    toggleState = if (action.toggle) ToggleableState(action.active) else null,
                    outline = outline,
                    onClick = { if (!action.busy) action.onClick() }
                ) {
                    if (action.busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = tint
                        )
                    } else {
                        PlayerIcon(action.icon, tint, Modifier.size(LevyraPlayerDesign.DockGlyph))
                    }
                }
            }
        }
    }
}
