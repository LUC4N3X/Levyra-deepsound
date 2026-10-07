package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
import com.luc4n3x.levyra.ui.components.LevyraPressScale
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
    val tint by animateColorAsState(
        targetValue = surfaces.tintFor(action.active),
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.standardTween(200)),
        label = "player-dock-tint"
    )
    val indicator by animateFloatAsState(
        targetValue = if (action.active) 1f else 0f,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.expressiveSpring()),
        label = "player-dock-indicator"
    )
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(LevyraPlayerDesign.MinimumTouchTarget)
                .graphicsLayer { alpha = if (action.enabled) 1f else DisabledDockAlpha }
                .clip(CircleShape)
                .levyraPressable(
                    onClick = { if (!action.busy) action.onClick() },
                    enabled = action.enabled,
                    pressedScale = LevyraPressScale.Control,
                    role = Role.Button
                )
                .semantics {
                    contentDescription = action.label
                    segmentToggleState(action.toggle, action.active)?.let { toggleableState = it }
                    action.stateDescription?.let { stateDescription = it }
                },
            contentAlignment = Alignment.Center
        ) {
            PlayerSegmentGlyph(action.icon, tint, action.busy, LevyraPlayerDesign.DockGlyph)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = LevyraPlayerDesign.SpaceXxs)
                    .size(LevyraPlayerDesign.ModeIndicator)
                    .graphicsLayer {
                        alpha = indicator
                        scaleX = indicator
                        scaleY = indicator
                    }
                    .background(tint, CircleShape)
            )
        }
    }
}

private const val DisabledDockAlpha = 0.42f

private fun dockHeight(compact: Boolean) =
    if (compact) LevyraPlayerDesign.DockHeightCompact else LevyraPlayerDesign.DockHeight
