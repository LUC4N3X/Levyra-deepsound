package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.ReactiveAuraPreset
import com.luc4n3x.levyra.ui.components.levyraExpressiveToggleCorner
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm

private const val IntensitySteps = 3
private const val IntensityMax = 100f

internal fun reactiveAuraPresetLabel(preset: ReactiveAuraPreset, strings: LevyraStrings): String = when (preset) {
    ReactiveAuraPreset.Calm -> strings.reactiveAuraPresetCalm
    ReactiveAuraPreset.Flow -> strings.reactiveAuraPresetFlow
    ReactiveAuraPreset.Pulse -> strings.reactiveAuraPresetPulse
}

@Composable
internal fun ReactiveAuraControls(
    preset: ReactiveAuraPreset,
    intensity: Int,
    accent: Color,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean,
    onPreset: (ReactiveAuraPreset) -> Unit,
    onIntensity: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LevyraPlayerDesign.Gutter),
        verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)
    ) {
        Text(
            text = strings.playerBackgroundReactiveAura,
            color = surfaces.content,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReactiveAuraPreset.entries.forEach { entry ->
                ReactiveAuraPresetTile(
                    label = reactiveAuraPresetLabel(entry, strings),
                    recommended = entry == ReactiveAuraPreset.Recommended,
                    recommendedLabel = strings.reactiveAuraRecommended,
                    selected = entry == preset,
                    accent = accent,
                    surfaces = surfaces,
                    animated = animated,
                    onClick = { onPreset(entry) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        val intensityLabel = strings.reactiveAuraIntensity
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = intensityLabel,
                color = surfaces.contentMuted,
                fontSize = 13.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(13.sp),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$intensity%",
                color = surfaces.content,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = intensity.toFloat(),
            onValueChange = { onIntensity(it.toInt()) },
            valueRange = 0f..IntensityMax,
            steps = IntensitySteps,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = surfaces.contentFaint.copy(alpha = 0.35f),
                activeTickColor = surfaces.heroContent.copy(alpha = 0.6f),
                inactiveTickColor = surfaces.contentFaint.copy(alpha = 0.6f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "$intensityLabel $intensity%" }
        )
    }
}

@Composable
private fun ReactiveAuraPresetTile(
    label: String,
    recommended: Boolean,
    recommendedLabel: String,
    selected: Boolean,
    accent: Color,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val corner = levyraExpressiveToggleCorner(
        checked = selected,
        unchecked = 16.dp,
        checkedCorner = 28.dp,
        label = "player-aura-preset-corner"
    )
    val shape = RoundedCornerShape(corner)
    val fill by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.24f) else surfaces.controlQuiet,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.standardTween(220)),
        label = "player-aura-preset-fill"
    )
    Column(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(fill)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent else surfaces.contentFaint.copy(alpha = 0.25f),
                shape = shape
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            color = if (selected) surfaces.content else surfaces.contentMuted,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        if (recommended) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.32f))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = recommendedLabel,
                    color = surfaces.content,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}
