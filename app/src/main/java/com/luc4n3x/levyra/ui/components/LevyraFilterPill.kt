package com.luc4n3x.levyra.ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Immutable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraType

private val FilterPillTouchHeight = 48.dp
private val FilterPillVisualHeight = 40.dp

@Immutable
private data class FilterPillColors(val container: Color, val content: Color, val outline: Color)

@Composable
private fun animatedFilterPillColors(selected: Boolean, animationsEnabled: Boolean): FilterPillColors {
    val colors = MaterialTheme.colorScheme
    val spec: AnimationSpec<Color> = if (animationsEnabled) LevyraMotion.standard() else snap()
    val container by animateColorAsState(
        targetValue = if (selected) colors.primaryContainer else colors.surfaceContainerHigh.copy(alpha = 0.45f),
        animationSpec = spec,
        label = "filter-pill-container"
    )
    val content by animateColorAsState(
        targetValue = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
        animationSpec = spec,
        label = "filter-pill-content"
    )
    val outline by animateColorAsState(
        targetValue = if (selected) colors.primary.copy(alpha = 0.55f) else colors.outlineVariant.copy(alpha = 0.35f),
        animationSpec = spec,
        label = "filter-pill-outline"
    )
    return FilterPillColors(container, content, outline)
}

@Composable
private fun FilterPillLeading(
    leadingIcon: ImageVector?,
    selected: Boolean,
    tint: Color,
    animationsEnabled: Boolean
) {
    if (leadingIcon != null) {
        Icon(
            imageVector = leadingIcon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.padding(end = 7.dp).size(16.dp)
        )
        return
    }
    AnimatedVisibility(
        visible = selected,
        enter = if (animationsEnabled) fadeIn() + expandHorizontally() else fadeIn(snap()),
        exit = if (animationsEnabled) fadeOut() + shrinkHorizontally() else fadeOut(snap())
    ) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.padding(end = 6.dp).size(16.dp)
        )
    }
}

@Composable
internal fun LevyraFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    role: Role = Role.Tab
) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val pillColors = animatedFilterPillColors(selected, animationsEnabled)
    Box(
        modifier = modifier
            .heightIn(min = FilterPillTouchHeight)
            .semantics { this.selected = selected }
            .levyraPressable(
                onClick = onClick,
                onClickLabel = label,
                role = role,
                pressedScale = LevyraPressScale.Control
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .height(FilterPillVisualHeight)
                .clip(CircleShape)
                .background(pillColors.container)
                .border(1.dp, pillColors.outline, CircleShape)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterPillLeading(leadingIcon, selected, pillColors.content, animationsEnabled)
            Text(
                text = label,
                style = LevyraType.cardTitle.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = pillColors.content,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
