package com.luc4n3x.levyra.ui.theme

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
import androidx.compose.material3.TextButton
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

private val GlassStudioCardShape = RoundedCornerShape(28.dp)

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
            TextButton(
                onClick = { onStyleChange(LevyraGlassStyle()) },
                enabled = enabled && style != LevyraGlassStyle()
            ) {
                Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(copy.reset, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = GlassStudioCardShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                GlassLevelSlider(copy.blur, style.blur, enabled) { onStyleChange(style.copy(blur = it)) }
                GlassLevelSlider(copy.transparency, style.transparency, enabled) {
                    onStyleChange(style.copy(transparency = it))
                }
                GlassLevelSlider(copy.tint, style.tint, enabled) { onStyleChange(style.copy(tint = it)) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = style.refraction,
                            enabled = enabled,
                            role = Role.Switch,
                            onValueChange = { onStyleChange(style.copy(refraction = it)) }
                        )
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(copy.refraction, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            copy.refractionSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = style.refraction, onCheckedChange = null, enabled = enabled)
                }
            }
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
private fun GlassLevelSlider(
    label: String,
    value: Int,
    enabled: Boolean,
    onCommit: (Int) -> Unit
) {
    var draft by remember(value) { mutableFloatStateOf(value.toFloat()) }
    val percent = "${draft.roundToInt()}%"
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                percent,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = draft,
            onValueChange = { draft = it },
            onValueChangeFinished = {
                val committed = draft.roundToInt().coerceIn(0, 100)
                if (committed != value) onCommit(committed)
            },
            valueRange = 0f..100f,
            enabled = enabled,
            modifier = Modifier.semantics {
                contentDescription = label
                stateDescription = percent
            }
        )
    }
}
