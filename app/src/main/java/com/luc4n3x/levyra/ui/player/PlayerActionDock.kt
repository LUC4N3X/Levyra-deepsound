package com.luc4n3x.levyra.ui.player

import androidx.compose.ui.graphics.Shape
import androidx.compose.animation.animateColorAsState
import com.luc4n3x.levyra.ui.components.levyraExpressiveCorner
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
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
        horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.DockGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        actions.forEachIndexed { index, action ->
            key(action.key) {
                PlayerDockSegment(
                    action = action,
                    first = index == 0,
                    last = index == actions.lastIndex,
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
    first: Boolean,
    last: Boolean,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean
) {
    val interaction = remember { MutableInteractionSource() }
    val outer = dockHeight(false) / 2
    val inner = levyraExpressiveCorner(
        interactionSource = interaction,
        rest = if (action.active) outer else LevyraPlayerDesign.DockInnerCorner,
        pressed = outer,
        label = "player-dock-inner"
    )
    val fill by animateColorAsState(
        targetValue = if (action.active) surfaces.hero else surfaces.tonal,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.standardTween(220)),
        label = "player-dock-fill"
    )
    val tint by animateColorAsState(
        targetValue = if (action.active) surfaces.heroContent else surfaces.content,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.standardTween(220)),
        label = "player-dock-tint"
    )
    val shape = dockSegmentShape(first = first, last = last, outer = outer, inner = inner)
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .graphicsLayer { alpha = if (action.enabled) 1f else DisabledDockAlpha }
            .clip(shape)
            .background(fill)
            .levyraPressable(
                onClick = { if (!action.busy) action.onClick() },
                enabled = action.enabled,
                interactionSource = interaction,
                pressedScale = LevyraPressScale.Control,
                role = Role.Button
            )
            .dockActionSemantics(action),
        contentAlignment = Alignment.Center
    ) {
        PlayerSegmentGlyph(action.icon, tint, action.busy, LevyraPlayerDesign.DockGlyph)
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
        targetValue = if (active) surfaces.active else surfaces.controlQuiet,
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

private fun dockSegmentShape(first: Boolean, last: Boolean, outer: Dp, inner: Dp): Shape {
    val start = if (first) outer else inner
    val end = if (last) outer else inner
    return RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end)
}

private fun Modifier.dockActionSemantics(action: PlayerDockAction): Modifier = semantics {
    contentDescription = action.label
    segmentToggleState(action.toggle, action.active)?.let { toggleableState = it }
    action.stateDescription?.let { stateDescription = it }
}

private fun dockHeight(compact: Boolean) =
    if (compact) LevyraPlayerDesign.DockHeightCompact else LevyraPlayerDesign.DockHeight
