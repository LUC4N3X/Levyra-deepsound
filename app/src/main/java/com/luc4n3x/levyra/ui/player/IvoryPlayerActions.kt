package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.PlayerIcon
import com.luc4n3x.levyra.ui.components.levyraExpressiveCorner
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.theme.IvoryDesign
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import kotlinx.coroutines.delay

@Composable
internal fun IvorySheetGrid(
    actions: List<PlayerSheetAction>,
    animated: Boolean,
    onPerform: (PlayerSheetAction) -> Unit
) {
    val rows = remember(actions) { actions.chunked(IvorySheetColumns) }
    Column(verticalArrangement = Arrangement.spacedBy(IvorySheetGap)) {
        rows.forEachIndexed { rowIndex, rowActions ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(IvorySheetGap)
            ) {
                rowActions.forEachIndexed { columnIndex, action ->
                    key(action.key) {
                        IvorySheetTile(
                            action = action,
                            order = rowIndex * IvorySheetColumns + columnIndex,
                            animated = animated,
                            onPerform = onPerform
                        )
                    }
                }
                repeat(IvorySheetColumns - rowActions.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.IvorySheetTile(
    action: PlayerSheetAction,
    order: Int,
    animated: Boolean,
    onPerform: (PlayerSheetAction) -> Unit
) {
    val palette = LevyraActivePalette
    val reveal = remember { Animatable(if (animated) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (!animated) return@LaunchedEffect
        delay(IvoryRevealStepMs * order.coerceAtMost(IvoryRevealMaxSteps))
        reveal.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
    }
    val interaction = remember { MutableInteractionSource() }
    val corner = levyraExpressiveCorner(
        interactionSource = interaction,
        rest = IvoryDesign.tileCorner,
        pressed = IvoryPressedCorner,
        label = "ivory-sheet-tile-corner"
    )
    val fill by animateColorAsState(if (action.active) IvoryDesign.ink else palette.ink, label = "ivory-sheet-tile-fill")
    val ink by animateColorAsState(if (action.active) IvoryDesign.onInk else IvoryDesign.ink, label = "ivory-sheet-tile-ink")
    val badge by animateColorAsState(if (action.active) IvoryDesign.rose else palette.panelSoft, label = "ivory-sheet-tile-badge")
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .heightIn(min = IvoryTileHeight)
            .graphicsLayer {
                val shown = reveal.value
                alpha = shown.coerceIn(0f, 1f) * if (action.enabled) 1f else IvoryDisabledAlpha
                translationY = (1f - shown) * IvoryRevealRise.toPx()
                val scale = 0.92f + 0.08f * shown
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(corner))
            .background(fill)
            .levyraPressable(
                onClick = { if (!action.busy) onPerform(action) },
                enabled = action.enabled,
                interactionSource = interaction,
                pressedScale = LevyraPressScale.Control,
                role = Role.Button
            )
            .semantics {
                contentDescription = listOfNotNull(action.label, action.value).joinToString(", ")
                if (action.toggle) toggleableState = ToggleableState(action.active)
            }
            .padding(horizontal = LevyraPlayerDesign.SpaceSm, vertical = LevyraPlayerDesign.SpaceMd),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(IvoryBadgeSize)
                .clip(CircleShape)
                .background(badge),
            contentAlignment = Alignment.Center
        ) {
            if (action.busy) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ink)
            } else {
                PlayerIcon(action.icon, if (action.active) IvoryDesign.onInk else IvoryDesign.ink, Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(LevyraPlayerDesign.SpaceSm))
        Text(
            text = action.label,
            color = ink,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Start,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        action.value?.let { value ->
            Text(
                text = value,
                color = if (action.active) IvoryDesign.rose else palette.muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private const val IvorySheetColumns = 3
private val IvorySheetGap = 8.dp
private val IvoryTileHeight = 92.dp
private val IvoryPressedCorner = 28.dp
private val IvoryBadgeSize = 34.dp
private val IvoryRevealRise = 18.dp
private const val IvoryRevealStepMs = 28L
private const val IvoryRevealMaxSteps = 12
private const val IvoryDisabledAlpha = 0.42f
