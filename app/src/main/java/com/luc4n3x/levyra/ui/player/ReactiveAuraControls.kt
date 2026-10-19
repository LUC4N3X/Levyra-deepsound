package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.ReactiveAuraPreset
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
import kotlin.math.roundToInt

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
    var previewIntensity by remember(intensity) { mutableFloatStateOf(intensity.toFloat()) }
    val largeText = LocalDensity.current.fontScale >= 1.6f
    Column(
        modifier = modifier
            .padding(horizontal = LevyraPlayerDesign.Gutter)
            .fillMaxWidth()
            .clip(LevyraPlayerDesign.ShapeLg)
            .background(surfaces.controlQuiet)
            .padding(LevyraPlayerDesign.SpaceLg),
        verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceLg)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceXs)) {
            Text(
                text = strings.playerBackgroundReactiveAura,
                color = surfaces.content,
                fontSize = 20.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(20.sp),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = "${strings.reactiveAuraRecommended} · ${reactiveAuraPresetLabel(ReactiveAuraPreset.Recommended, strings)}",
                color = surfaces.contentMuted,
                fontSize = 12.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(12.sp)
            )
        }
        if (largeText) {
            Column(
                modifier = Modifier.fillMaxWidth().selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)
            ) {
                ReactiveAuraPreset.entries.forEach { entry ->
                    ReactiveAuraPresetButton(entry, entry == preset, accent, surfaces, animated,
                        onClick = { onPreset(entry) }, modifier = Modifier.fillMaxWidth(), compact = true)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)
            ) {
                ReactiveAuraPreset.entries.forEach { entry ->
                    ReactiveAuraPresetButton(entry, entry == preset, accent, surfaces, animated,
                        onClick = { onPreset(entry) }, modifier = Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceXs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)
            ) {
                Text(
                    text = strings.reactiveAuraIntensity,
                    color = surfaces.contentMuted,
                    fontSize = 14.sp,
                    lineHeight = LevyraTypeRhythm.lineHeight(14.sp),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${previewIntensity.roundToInt()}%",
                    color = surfaces.content,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = previewIntensity,
                onValueChange = { previewIntensity = it },
                onValueChangeFinished = { onIntensity(previewIntensity.roundToInt()) },
                valueRange = 0f..IntensityMax,
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = surfaces.contentFaint.copy(alpha = 0.22f)
                ),
                modifier = Modifier.fillMaxWidth().semantics {
                    contentDescription = strings.reactiveAuraIntensity
                }
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("0%", color = surfaces.contentMuted, fontSize = 11.sp)
                Text("100%", color = surfaces.contentMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ReactiveAuraPresetButton(
    preset: ReactiveAuraPreset,
    selected: Boolean,
    accent: Color,
    surfaces: PlayerSurfaceTokens,
    animated: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val corner by animateDpAsState(
        targetValue = if (selected) LevyraPlayerDesign.CornerLg else LevyraPlayerDesign.CornerSm,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.expressiveSpring()),
        label = "player-aura-preset-corner"
    )
    val fill by animateColorAsState(
        targetValue = if (selected) accent else surfaces.controlQuiet,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.standardTween()),
        label = "player-aura-preset-fill"
    )
    val ink = if (selected) surfaces.heroContent else surfaces.content
    val icon = if (selected) Icons.Rounded.Check else when (preset) {
        ReactiveAuraPreset.Calm -> Icons.Rounded.Air
        ReactiveAuraPreset.Flow -> Icons.Rounded.Waves
        ReactiveAuraPreset.Pulse -> Icons.Rounded.GraphicEq
    }
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = if (compact) 56.dp else 88.dp).semantics { this.selected = selected },
        shape = RoundedCornerShape(corner),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = fill, contentColor = ink),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(LevyraPlayerDesign.SpaceSm)
    ) {
        if (compact) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceMd)) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                Text(reactiveAuraPresetLabel(preset, LocalLevyraStrings.current), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                Text(reactiveAuraPresetLabel(preset, LocalLevyraStrings.current), fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
}
