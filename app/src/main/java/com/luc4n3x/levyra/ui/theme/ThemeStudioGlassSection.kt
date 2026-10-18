package com.luc4n3x.levyra.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.InvertColors
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.toShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.LevyraGlassStyle
import com.luc4n3x.levyra.ui.i18n.GlassStudioCopy
import kotlin.math.roundToInt

private val GlassTileGap = 3.dp
private val GlassTileOuterCorner = 28.dp
private val GlassTileInnerCorner = 8.dp
private val GlassTileFirstShape = RoundedCornerShape(
    topStart = GlassTileOuterCorner,
    topEnd = GlassTileOuterCorner,
    bottomStart = GlassTileInnerCorner,
    bottomEnd = GlassTileInnerCorner
)
private val GlassTileMiddleShape = RoundedCornerShape(GlassTileInnerCorner)
private val GlassTileLastShape = RoundedCornerShape(
    topStart = GlassTileInnerCorner,
    topEnd = GlassTileInnerCorner,
    bottomStart = GlassTileOuterCorner,
    bottomEnd = GlassTileOuterCorner
)
private val GlassSliderTrackHeight = 16.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ThemeStudioGlassSection(
    copy: GlassStudioCopy,
    enabled: Boolean,
    style: LevyraGlassStyle,
    onStyleChange: (LevyraGlassStyle) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceMd)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = LevyraPlayerDesign.SpaceXs)
            ) {
                Text(
                    copy.title,
                    color = LevyraMuted,
                    fontSize = 12.sp,
                    letterSpacing = 0.4.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    copy.subtitle,
                    color = LevyraMuted,
                    fontSize = 13.sp,
                    lineHeight = LevyraTypeRhythm.lineHeight(13f),
                    fontWeight = FontWeight.Medium
                )
            }
            FilledTonalButton(
                onClick = { onStyleChange(LevyraGlassStyle()) },
                enabled = enabled && style != LevyraGlassStyle(),
                shapes = ButtonDefaults.shapes()
            ) {
                Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(copy.reset, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(GlassTileGap)) {
            GlassLevelTile(
                label = copy.blur,
                icon = Icons.Rounded.BlurOn,
                value = style.blur,
                enabled = enabled,
                shape = GlassTileFirstShape
            ) { onStyleChange(style.copy(blur = it)) }
            GlassLevelTile(
                label = copy.transparency,
                icon = Icons.Rounded.Opacity,
                value = style.transparency,
                enabled = enabled,
                shape = GlassTileMiddleShape
            ) { onStyleChange(style.copy(transparency = it)) }
            GlassLevelTile(
                label = copy.tint,
                icon = Icons.Rounded.InvertColors,
                value = style.tint,
                enabled = enabled,
                shape = GlassTileMiddleShape
            ) { onStyleChange(style.copy(tint = it)) }
            GlassRefractionTile(
                title = copy.refraction,
                subtitle = copy.refractionSubtitle,
                checked = style.refraction,
                enabled = enabled,
                onCheckedChange = { onStyleChange(style.copy(refraction = it)) }
            )
        }
        Text(
            if (enabled) copy.performanceNote else copy.disabledNote,
            color = LevyraMuted,
            fontSize = 12.sp,
            lineHeight = LevyraTypeRhythm.lineHeight(12f),
            modifier = Modifier.padding(horizontal = LevyraPlayerDesign.SpaceXs)
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun GlassTileIcon(icon: ImageVector, enabled: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(MaterialShapes.Cookie9Sided.toShape())
            .background(if (enabled) scheme.secondaryContainer else scheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlassLevelTile(
    label: String,
    icon: ImageVector,
    value: Int,
    enabled: Boolean,
    shape: Shape,
    onCommit: (Int) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    var draft by remember(value) { mutableFloatStateOf(value.toFloat()) }
    val percent = "${draft.roundToInt()}%"
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = scheme.surfaceContainerHigh,
        contentColor = scheme.onSurface
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassTileIcon(icon = icon, enabled = enabled)
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = CircleShape,
                    color = if (enabled) scheme.primaryContainer else scheme.surfaceContainerHighest,
                    contentColor = if (enabled) scheme.onPrimaryContainer else scheme.onSurfaceVariant
                ) {
                    Text(
                        percent,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
            val sliderState = remember { SliderState(draft, 0, 0f..100f) }
            sliderState.value = draft
            Slider(
                state = sliderState,
                onValueChange = { draft = it },
                onValueChangeFinished = {
                    val committed = draft.roundToInt().coerceIn(0, 100)
                    if (committed != value) onCommit(committed)
                },
                enabled = enabled,
                interactionSource = interactionSource,
                thumb = {
                    SliderDefaults.Thumb(interactionSource = interactionSource, enabled = enabled)
                },
                track = { sliderState: SliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        enabled = enabled,
                        modifier = Modifier.height(GlassSliderTrackHeight)
                    )
                },
                modifier = Modifier.semantics {
                    contentDescription = label
                    stateDescription = percent
                }
            )
        }
    }
}

@Composable
private fun GlassRefractionTile(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = GlassTileLastShape,
        color = scheme.surfaceContainerHigh,
        contentColor = scheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Switch,
                    onValueChange = onCheckedChange
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassTileIcon(icon = Icons.Rounded.AutoAwesome, enabled = enabled)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                thumbContent = {
                    Icon(
                        imageVector = if (checked) Icons.Rounded.Check else Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            )
        }
    }
}
