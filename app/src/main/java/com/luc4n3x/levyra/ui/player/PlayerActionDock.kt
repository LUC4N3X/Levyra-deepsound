package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraExpressiveToggleCorner
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign

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
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        actions.forEach { action ->
            key(action.key) {
                PlayerDockControl(
                    action = action,
                    surfaces = surfaces,
                    animated = animated
                )
            }
        }
    }
}

@Composable
private fun RowScope.PlayerDockControl(
    action: PlayerDockAction,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        PlayerToggleControl(
            icon = action.icon,
            label = action.label,
            active = action.active,
            toggle = action.toggle,
            surfaces = surfaces,
            animated = animated,
            glyph = LevyraPlayerDesign.DockGlyph,
            enabled = action.enabled,
            busy = action.busy,
            stateDescription = action.stateDescription,
            onClick = action.onClick
        )
    }
}

@Composable
internal fun PlayerToggleControl(
    icon: ImageVector,
    label: String,
    active: Boolean,
    toggle: Boolean,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean,
    glyph: Dp,
    onClick: () -> Unit,
    enabled: Boolean = true,
    busy: Boolean = false,
    stateDescription: String? = null
) {
    val tint by animateColorAsState(
        targetValue = surfaces.tintFor(active),
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.standardTween(200)),
        label = "player-toggle-tint"
    )
    val fill by animateColorAsState(
        targetValue = if (active) surfaces.active else Color.Transparent,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.standardTween(200)),
        label = "player-toggle-fill"
    )
    val corner = levyraExpressiveToggleCorner(
        checked = active,
        unchecked = LevyraPlayerDesign.MinimumTouchTarget / 2,
        checkedCorner = ToggleCheckedCorner,
        label = "player-toggle-corner"
    )
    val shape = RoundedCornerShape(corner)
    Box(
        modifier = Modifier
            .size(LevyraPlayerDesign.MinimumTouchTarget)
            .graphicsLayer { alpha = if (enabled) 1f else DisabledDockAlpha }
            .clip(shape)
            .background(fill)
            .levyraPressable(
                onClick = { if (!busy) onClick() },
                enabled = enabled,
                pressedScale = LevyraPressScale.Control,
                role = Role.Button
            )
            .semantics {
                contentDescription = label
                segmentToggleState(toggle, active)?.let { toggleableState = it }
                stateDescription?.let { this.stateDescription = it }
            },
        contentAlignment = Alignment.Center
    ) {
        PlayerSegmentGlyph(icon, tint, busy, glyph)
    }
}

private val ToggleCheckedCorner: Dp = 14.dp
private const val DisabledDockAlpha = 0.42f

private fun dockHeight(compact: Boolean) =
    if (compact) LevyraPlayerDesign.DockHeightCompact else LevyraPlayerDesign.DockHeight
