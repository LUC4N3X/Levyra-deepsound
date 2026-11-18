package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.feature.radio.RadioCategory
import com.luc4n3x.levyra.ui.components.LevyraPastelAmber
import com.luc4n3x.levyra.ui.components.LevyraPastelLavender
import com.luc4n3x.levyra.ui.components.LevyraPastelPeriwinkle
import com.luc4n3x.levyra.ui.components.LevyraPastelRose
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.LevyraUtilitySeed
import com.luc4n3x.levyra.ui.components.levyraExpressiveCorner
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.components.levyraUtilityColors

private val DefaultRadioCategorySeeds = listOf(LevyraUtilitySeed, LevyraPastelRose, LevyraPastelAmber, LevyraPastelLavender, LevyraPastelPeriwinkle)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val DefaultRadioCategoryShapes = listOf(
    MaterialShapes.Cookie9Sided,
    MaterialShapes.Clover4Leaf,
    MaterialShapes.Sunny,
    MaterialShapes.Cookie6Sided,
    MaterialShapes.Pentagon,
    MaterialShapes.Cookie4Sided
)

private const val DefaultRadioCategoryColumns = 3

@Composable
internal fun DefaultRadioCategoryGrid(
    selected: RadioCategory,
    label: (RadioCategory) -> String,
    modifier: Modifier = Modifier,
    onSelect: (RadioCategory) -> Unit
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RadioCategory.entries.chunked(DefaultRadioCategoryColumns).forEachIndexed { row, categories ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEachIndexed { column, category ->
                    key(category.name) {
                        DefaultRadioCategoryTile(
                            category = category,
                            label = label(category),
                            index = row * DefaultRadioCategoryColumns + column,
                            selected = category == selected,
                            modifier = Modifier.weight(1f)
                        ) { onSelect(category) }
                    }
                }
                repeat(DefaultRadioCategoryColumns - categories.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private class DefaultRadioCategoryStyle(
    val corner: androidx.compose.ui.unit.Dp,
    val turn: Float,
    val container: Color,
    val badgeFill: Color,
    val badgeInk: Color,
    val label: Color,
    val weight: FontWeight
)

private fun defaultRadioCategoryStyle(selected: Boolean, tone: com.luc4n3x.levyra.ui.components.LevyraUtilityColors, idle: Color, ink: Color) =
    if (selected) {
        DefaultRadioCategoryStyle(32.dp, 30f, tone.container, tone.accent, tone.onAccent, tone.onContainer, FontWeight.Bold)
    } else {
        DefaultRadioCategoryStyle(20.dp, 0f, idle, tone.container, tone.onContainer, ink, FontWeight.SemiBold)
    }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DefaultRadioCategoryTile(
    category: RadioCategory,
    label: String,
    index: Int,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val tone = levyraUtilityColors(DefaultRadioCategorySeeds[index % DefaultRadioCategorySeeds.size])
    val style = defaultRadioCategoryStyle(selected, tone, colors.surfaceContainer, colors.onSurface)
    val interaction = remember { MutableInteractionSource() }
    val corner = levyraExpressiveCorner(interactionSource = interaction, rest = style.corner, pressed = 12.dp, label = "default-radio-category")
    val turn by animateFloatAsState(
        targetValue = style.turn,
        animationSpec = if (LocalAnimationsEnabled.current) androidx.compose.animation.core.spring() else androidx.compose.animation.core.snap(),
        label = "default-radio-category-turn"
    )
    val shape = if (selected) MaterialShapes.Cookie12Sided else DefaultRadioCategoryShapes[index % DefaultRadioCategoryShapes.size]
    Column(
        modifier
            .heightIn(min = 104.dp)
            .clip(RoundedCornerShape(corner))
            .background(style.container)
            .levyraPressable(onClick = onClick, interactionSource = interaction, role = Role.Button, pressedScale = LevyraPressScale.Tile)
            .semantics { this.selected = selected }
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        DefaultRadioCategoryBadge(category, shape, { turn }, style.badgeFill, style.badgeInk)
        Text(
            label,
            color = style.label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = style.weight),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DefaultRadioCategoryBadge(
    category: RadioCategory,
    shape: androidx.graphics.shapes.RoundedPolygon,
    turn: () -> Float,
    fill: Color,
    ink: Color
) {
    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { rotationZ = turn() }
                .background(fill, shape.toShape())
        )
        Icon(radioCategoryIcon(category), contentDescription = null, tint = ink, modifier = Modifier.size(22.dp))
    }
}

@Composable
internal fun DefaultRadioGroupedAction(
    icon: ImageVector,
    label: String,
    container: Color,
    content: Color,
    startCorner: Dp,
    endCorner: Dp,
    modifier: Modifier = Modifier,
    text: String? = null,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val start = levyraExpressiveCorner(interactionSource = interaction, rest = startCorner, pressed = 28.dp, label = "default-radio-action-start")
    val end = levyraExpressiveCorner(interactionSource = interaction, rest = endCorner, pressed = 28.dp, label = "default-radio-action-end")
    Row(
        modifier
            .height(56.dp)
            .clip(RoundedCornerShape(topStart = start, bottomStart = start, topEnd = end, bottomEnd = end))
            .background(container)
            .levyraPressable(onClick = onClick, interactionSource = interaction, role = Role.Button, pressedScale = LevyraPressScale.Control)
            .semantics { contentDescription = label }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(24.dp))
        if (text != null) {
            Text(text, color = content, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1)
        }
    }
}

internal fun defaultRadioSegmentShape(first: Boolean, last: Boolean): RoundedCornerShape {
    val outer = 24.dp
    val inner = 6.dp
    return RoundedCornerShape(
        topStart = if (first) outer else inner,
        topEnd = if (first) outer else inner,
        bottomStart = if (last) outer else inner,
        bottomEnd = if (last) outer else inner
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DefaultRadioHeaderAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    val tone = levyraUtilityColors(LevyraUtilitySeed)
    androidx.compose.material3.FilledTonalIconButton(
        onClick = onClick,
        shapes = androidx.compose.material3.IconButtonDefaults.shapes(
            shape = MaterialShapes.Cookie4Sided.toShape(),
            pressedShape = MaterialShapes.Cookie9Sided.toShape()
        ),
        colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = tone.container,
            contentColor = tone.onContainer
        ),
        modifier = Modifier.size(46.dp)
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
    }
}
