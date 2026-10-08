package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.LevyraMixKind
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign
import com.luc4n3x.levyra.ui.theme.LevyraType

private const val CrestBarCount = 9
private val CrestPhases = floatArrayOf(0.35f, 0.72f, 0.44f, 0.95f, 0.58f, 0.86f, 0.40f, 0.68f, 0.30f)

@Composable
internal fun LevyraMixLauncherPanel(
    familiarity: Float,
    loading: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onFamiliarityChange: (Float) -> Unit,
    onStartMix: (LevyraMixKind) -> Unit,
    onOpenYourSound: () -> Unit,
    onOpenMixLab: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LevyraMixHeroCard(
            familiarity = familiarity,
            loading = loading,
            accent = accent,
            onFamiliarityChange = onFamiliarityChange,
            onStartMix = onStartMix
        )
        LevyraSecondaryToolsRow(
            loading = loading,
            accent = accent,
            onStartMix = onStartMix,
            onOpenYourSound = onOpenYourSound,
            onOpenMixLab = onOpenMixLab
        )
    }
}

@Composable
internal fun LevyraMixHeroCard(
    familiarity: Float,
    loading: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onFamiliarityChange: (Float) -> Unit,
    onStartMix: (LevyraMixKind) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val heroShape = LevyraCardDesign.SurfaceShape
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(heroShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF283153), Color(0xFF171D35), Color(0xFF101522))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.15f), heroShape)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(148.dp)
                .background(
                    Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.22f), Color.Transparent)
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MixCrest(accent = accent, active = loading)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        strings.levyraMix,
                        style = LevyraType.screenTitle,
                        color = Color.White,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        strings.mixCreate,
                        style = LevyraType.metadata,
                        color = Color.White.copy(alpha = 0.70f)
                    )
                }
                FilledIconButton(
                    onClick = { onStartMix(LevyraMixKind.Personalized) },
                    enabled = !loading,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(54.dp)
                        .semantics { contentDescription = strings.mixForYou }
                ) {
                    if (loading) {
                        DiscoveryLoadingIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(30.dp))
                    }
                }
            }
            MixBalanceSlider(
                familiarity,
                !loading,
                accent,
                strings.mixFamiliarLabel,
                strings.mixDiscoveryLabel,
                onFamiliarityChange
            )
        }
    }
}

@Composable
internal fun LevyraSecondaryToolsRow(
    loading: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onStartMix: (LevyraMixKind) -> Unit,
    onOpenYourSound: () -> Unit,
    onOpenMixLab: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val stacked = maxWidth < 280.dp || LocalDensity.current.fontScale > 1.3f
        val actions: @Composable (Modifier) -> Unit = { actionModifier ->
            MixToolAction(Icons.Rounded.Casino, strings.surpriseMe, accent, !loading, stacked, actionModifier) { onStartMix(LevyraMixKind.SurpriseMe) }
            MixToolAction(Icons.Rounded.GraphicEq, strings.yourSound, accent, true, stacked, actionModifier, onOpenYourSound)
            MixToolAction(Icons.Rounded.Tune, strings.mixLab, accent, true, stacked, actionModifier, onOpenMixLab)
        }
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { actions(Modifier.fillMaxWidth()) }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { actions(Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun MixToolAction(
    icon: ImageVector,
    label: String,
    accent: Color,
    enabled: Boolean,
    horizontal: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val actionModifier = modifier
        .heightIn(min = if (horizontal) 56.dp else 84.dp)
        .clip(LevyraCardDesign.ArtworkShape)
        .background(colors.surfaceContainerHigh)
        .border(1.dp, colors.outlineVariant.copy(alpha = 0.30f), LevyraCardDesign.ArtworkShape)
        .semantics(mergeDescendants = true) {}
        .levyraPressable(
            onClick = onClick,
            enabled = enabled,
            pressedScale = LevyraPressScale.Tile,
            role = Role.Button
        )
        .padding(horizontal = 10.dp, vertical = 10.dp)
    val content: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = if (enabled) 0.15f else 0.07f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) accent else colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            label,
            style = LevyraType.cardTitle,
            color = if (enabled) colors.onSurface else colors.onSurfaceVariant,
            softWrap = true,
            textAlign = if (horizontal) TextAlign.Start else TextAlign.Center
        )
    }
    if (horizontal) {
        Row(
            modifier = actionModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) { content() }
    } else {
        Column(
            modifier = actionModifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) { content() }
    }
}

@Composable
private fun MixCrest(accent: Color, active: Boolean) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val phase = rememberCrestPhase(active && animationsEnabled)
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(LevyraCardDesign.ThumbShape)
            .background(accent.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            val slot = size.width / CrestBarCount
            val barWidth = slot * 0.52f
            val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
            val drift = phase.value
            for (index in 0 until CrestBarCount) {
                val base = CrestPhases[index]
                val wave = if (drift > 0f) {
                    base + (1f - base) * kotlin.math.abs(kotlin.math.sin((drift + index * 0.35f) * 3.14159f))
                } else {
                    base
                }
                val barHeight = (size.height * wave.coerceIn(0.18f, 1f))
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(index * slot + (slot - barWidth) / 2f, (size.height - barHeight) / 2f),
                    size = Size(barWidth, barHeight),
                    cornerRadius = radius
                )
            }
        }
    }
}

@Composable
private fun rememberCrestPhase(active: Boolean): State<Float> {
    if (!active) return remember { mutableFloatStateOf(0f) }
    val transition = rememberInfiniteTransition(label = "levyra-mix-crest")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_600),
            repeatMode = RepeatMode.Restart
        ),
        label = "levyra-mix-crest-phase"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MixBalanceSlider(
    familiarity: Float,
    enabled: Boolean,
    accent: Color,
    familiarLabel: String,
    discoveryLabel: String,
    onFamiliarityChange: (Float) -> Unit
) {
    val sliderColors = SliderDefaults.colors(
        thumbColor = accent,
        activeTrackColor = accent,
        inactiveTrackColor = Color.White.copy(alpha = 0.24f),
        disabledThumbColor = Color.White.copy(alpha = 0.50f),
        disabledActiveTrackColor = Color.White.copy(alpha = 0.40f)
    )
    val sliderState = rememberSliderState(
        value = 1f - familiarity,
        trackRange = 0f..1f
    )
    LaunchedEffect(familiarity) {
        val target = 1f - familiarity
        if (sliderState.value != target) {
            sliderState.value = target
        }
    }
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(familiarLabel, modifier = Modifier.weight(1f), style = LevyraType.caption, color = Color.White.copy(alpha = 0.74f))
            Text(discoveryLabel, modifier = Modifier.weight(1f), style = LevyraType.caption, color = Color.White.copy(alpha = 0.74f), textAlign = TextAlign.End)
        }
        Slider(
            state = sliderState,
            onValueChange = { value ->
                sliderState.value = value
                onFamiliarityChange(1f - value)
            },
            enabled = enabled,
            colors = sliderColors,
            thumb = {
                Box(Modifier.size(width = 4.dp, height = 24.dp).background(if (enabled) accent else Color.White.copy(alpha = 0.50f), CircleShape))
            },
            track = { sliderState ->
                SliderDefaults.Track(sliderState = sliderState, colors = sliderColors, enabled = enabled, modifier = Modifier.height(6.dp))
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .semantics { contentDescription = "$familiarLabel / $discoveryLabel" }
        )
    }
}
