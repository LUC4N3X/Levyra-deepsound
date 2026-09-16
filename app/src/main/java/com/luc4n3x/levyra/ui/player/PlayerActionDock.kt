package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
    val enabled: Boolean = true,
    val stateDescription: String? = null
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
    Row(
        modifier = modifier
            .widthIn(max = LevyraPlayerDesign.DockMaxWidth)
            .fillMaxWidth()
            .height(dockHeight(compact)),
        horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.DockGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        actions.forEachIndexed { index, action ->
            key(action.key) {
                PlayerDockSegment(
                    action = action,
                    position = segmentPosition(index, actions.size),
                    surfaces = surfaces,
                    animated = animated
                )
            }
        }
    }
}

@Composable
private fun RowScope.PlayerDockSegment(
    action: PlayerDockAction,
    position: LevyraSegment,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean
) {
    val tint by animateColorAsState(
        targetValue = surfaces.tintFor(action.active),
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.standardTween(200)),
        label = "player-dock-tint"
    )
    PlayerSegmentButton(
        position = position,
        weight = 1f,
        container = surfaces.fillFor(action.active),
        innerCorner = LevyraPlayerDesign.DockInnerCorner,
        contentDescription = action.label,
        animated = animated,
        enabled = action.enabled,
        toggleState = segmentToggleState(action.toggle, action.active),
        stateDescription = action.stateDescription,
        outline = surfaces.segmentOutline,
        onClick = { if (!action.busy) action.onClick() }
    ) {
        PlayerSegmentGlyph(action.icon, tint, action.busy, LevyraPlayerDesign.DockGlyph)
    }
}

private fun dockHeight(compact: Boolean) =
    if (compact) LevyraPlayerDesign.DockHeightCompact else LevyraPlayerDesign.DockHeight
