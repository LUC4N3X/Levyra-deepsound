package com.luc4n3x.levyra.ui

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.LevyraMixKind
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraType
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import com.luc4n3x.levyra.ui.components.levyraExpressiveCorner
import com.luc4n3x.levyra.ui.theme.LevyraPlayerShapes
import com.luc4n3x.levyra.ui.theme.LevyraSegment
import com.luc4n3x.levyra.ui.components.levyraExpressiveToggleCorner
import androidx.compose.ui.semantics.selected
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

private const val ReactorBarCount = 17
private val ReactorBasePhases = floatArrayOf(
    0.30f, 0.45f, 0.65f, 0.82f, 0.95f, 0.88f, 0.72f, 0.55f,
    0.42f, 0.58f, 0.76f, 0.92f, 0.98f, 0.80f, 0.62f, 0.40f, 0.28f
)

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
    val colors = MaterialTheme.colorScheme
    val panelShape = RoundedCornerShape(32.dp)
    val wash = remember(accent, colors.surfaceContainerHigh) {
        Brush.verticalGradient(
            0f to accent.copy(alpha = 0.24f),
            0.7f to colors.surfaceContainerHigh.copy(alpha = 0f)
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(panelShape)
            .background(colors.surfaceContainerHigh)
            .background(wash)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SonicReactorSpectrum(
                familiarity = familiarity,
                accent = accent,
                active = loading,
                modifier = Modifier.weight(1f)
            )
            MixPlayButton(
                loading = loading,
                accent = accent,
                contentDescription = strings.mixForYou,
                onClick = { onStartMix(LevyraMixKind.Personalized) }
            )
        }

        MixBalanceSlider(
            familiarity = familiarity,
            enabled = !loading,
            accent = accent,
            familiarLabel = strings.mixFamiliarLabel,
            discoveryLabel = strings.mixDiscoveryLabel,
            onFamiliarityChange = onFamiliarityChange
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MixPlayButton(
    loading: Boolean,
    accent: Color,
    contentDescription: String,
    onClick: () -> Unit
) {
    val content = if (accent.luminance() > 0.5f) Color.Black.copy(alpha = 0.87f) else Color.White
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(MaterialShapes.Cookie9Sided.toShape())
            .background(accent)
            .levyraPressable(
                onClick = onClick,
                enabled = !loading,
                role = Role.Button,
                pressedScale = LevyraPressScale.Control
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            DiscoveryLoadingIndicator(modifier = Modifier.size(26.dp), color = content)
        } else {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun SonicReactorSpectrum(
    familiarity: Float,
    accent: Color,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val phase = rememberReactorPhase(active && animationsEnabled)
    val colors = MaterialTheme.colorScheme
    val discoveryFactor = (1f - familiarity).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surfaceContainer.copy(alpha = 0.72f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val slot = size.width / ReactorBarCount
            val barWidth = (slot * 0.32f).coerceIn(1.5f, 3.5f)
            val cornerRad = CornerRadius(barWidth / 2f, barWidth / 2f)
            val drift = if (animationsEnabled && active) phase.value else 0f
            val baseEnergy = if (active) 1.1f else 0.8f

            for (index in 0 until ReactorBarCount) {
                val base = ReactorBasePhases[index]
                val waveFreq = 1.0f + discoveryFactor * 1.5f
                val dynamicWave = if (animationsEnabled && active) {
                    val angle = (drift * waveFreq + index * 0.36f) * 3.14159f
                    abs(sin(angle))
                } else {
                    0.15f
                }
                val heightMultiplier = (base + (1f - base) * dynamicWave * baseEnergy)
                    .coerceIn(0.14f, 1.0f)
                val barHeight = size.height * heightMultiplier
                val x = index * slot + (slot - barWidth) / 2f
                val y = (size.height - barHeight) / 2f
                val barColor = if (active) accent else colors.onSurfaceVariant

                drawRoundRect(
                    color = barColor.copy(alpha = if (active) 0.58f else 0.35f),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = cornerRad
                )
            }
        }
    }
}

@Composable
private fun rememberReactorPhase(active: Boolean): State<Float> {
    if (!active) return remember { mutableFloatStateOf(0f) }
    val transition = rememberInfiniteTransition(label = "reactor-spectrum")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_200),
            repeatMode = RepeatMode.Restart
        ),
        label = "reactor-spectrum-phase"
    )
}

@Composable
internal fun LevyraSecondaryToolsRow(
    loading: Boolean,
    modifier: Modifier = Modifier,
    onStartMix: (LevyraMixKind) -> Unit,
    onOpenYourSound: () -> Unit,
    onOpenMixLab: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val stacked = maxWidth < 280.dp || LocalDensity.current.fontScale > 1.3f
        val actions: @Composable (Modifier) -> Unit = { actionModifier ->
            MixToolAction(
                icon = Icons.Rounded.Casino,
                label = strings.surpriseMe,
                enabled = !loading,
                segment = if (stacked) LevyraSegment.Single else LevyraSegment.Leading,
                modifier = actionModifier
            ) { onStartMix(LevyraMixKind.SurpriseMe) }

            MixToolAction(
                icon = Icons.Rounded.GraphicEq,
                label = strings.yourSound,
                enabled = true,
                segment = if (stacked) LevyraSegment.Single else LevyraSegment.Middle,
                modifier = actionModifier,
                onClick = onOpenYourSound
            )

            MixToolAction(
                icon = Icons.Rounded.Tune,
                label = strings.mixLab,
                enabled = true,
                segment = if (stacked) LevyraSegment.Single else LevyraSegment.Trailing,
                modifier = actionModifier,
                onClick = onOpenMixLab
            )
        }

        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                actions(Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                actions(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MixToolAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    segment: LevyraSegment,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val innerCorner = levyraExpressiveCorner(
        interactionSource = interaction,
        rest = 10.dp,
        pressed = 24.dp,
        label = "mix-tool-corner"
    )
    val shape = LevyraPlayerShapes.segment(segment, innerCorner)

    val actionModifier = modifier
        .heightIn(min = 68.dp)
        .clip(shape)
        .background(colors.onSurface.copy(alpha = if (enabled) 0.08f else 0.04f))
        .semantics(mergeDescendants = true) {}
        .levyraPressable(
            onClick = onClick,
            enabled = enabled,
            pressedScale = LevyraPressScale.Control,
            role = Role.Button,
            interactionSource = interaction
        )
        .padding(horizontal = 4.dp, vertical = 10.dp)

    Column(
        modifier = actionModifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.onSurface,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = label,
            style = LevyraType.caption.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp,
                letterSpacing = (-0.2).sp
            ),
            color = colors.onSurface,
            softWrap = false,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
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
    val strings = LocalLevyraStrings.current
    val colors = MaterialTheme.colorScheme
    val sliderColors = SliderDefaults.colors(
        thumbColor = accent,
        activeTrackColor = accent,
        inactiveTrackColor = colors.outlineVariant.copy(alpha = 0.45f),
        disabledThumbColor = colors.onSurfaceVariant,
        disabledActiveTrackColor = colors.onSurfaceVariant
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

    val percentFamiliar = (familiarity * 100f).roundToInt().coerceIn(0, 100)
    val percentDiscovery = 100 - percentFamiliar

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = familiarLabel,
                style = LevyraType.caption.copy(fontWeight = FontWeight.Medium),
                color = colors.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$percentFamiliar% / $percentDiscovery%",
                style = LevyraType.overline.copy(fontWeight = FontWeight.Medium, fontSize = 10.sp),
                color = colors.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = discoveryLabel,
                style = LevyraType.caption.copy(fontWeight = FontWeight.Medium),
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
                Box(
                    modifier = Modifier
                        .size(width = 14.dp, height = 22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (enabled) accent else colors.onSurfaceVariant)
                        .border(0.75.dp, colors.onSurface.copy(alpha = 0.16f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(10.dp)
                            .background(colors.onSurface.copy(alpha = 0.50f), CircleShape)
                    )
                }
            },
            track = { currentSliderState ->
                SliderDefaults.Track(
                    sliderState = currentSliderState,
                    colors = sliderColors,
                    enabled = enabled,
                    modifier = Modifier.height(5.dp)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .semantics { contentDescription = "$familiarLabel / $discoveryLabel" }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MixPresetChip(
                label = familiarLabel,
                icon = Icons.Rounded.AutoAwesome,
                isSelected = familiarity >= 0.70f,
                enabled = enabled,
                modifier = Modifier.weight(1f)
            ) {
                onFamiliarityChange(0.85f)
                sliderState.value = 0.15f
            }

            MixPresetChip(
                label = strings.mix,
                icon = Icons.Rounded.GraphicEq,
                isSelected = familiarity >= 0.35f && familiarity < 0.70f,
                enabled = enabled,
                modifier = Modifier.weight(1f)
            ) {
                onFamiliarityChange(0.50f)
                sliderState.value = 0.50f
            }

            MixPresetChip(
                label = discoveryLabel,
                icon = Icons.Rounded.Casino,
                isSelected = familiarity < 0.35f,
                enabled = enabled,
                modifier = Modifier.weight(1f)
            ) {
                onFamiliarityChange(0.15f)
                sliderState.value = 0.85f
            }
        }
    }
}

@Composable
private fun MixPresetChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val corner = levyraExpressiveToggleCorner(
        checked = isSelected,
        unchecked = 12.dp,
        checkedCorner = 24.dp,
        label = "mix-preset-corner"
    )
    val shape = RoundedCornerShape(corner)
    val container by animateColorAsState(
        targetValue = if (isSelected) colors.primaryContainer else colors.surfaceContainer.copy(alpha = 0.72f),
        label = "mixPresetContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) colors.onPrimaryContainer else colors.onSurfaceVariant,
        label = "mixPresetContent"
    )

    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(container)
            .semantics { selected = isSelected }
            .levyraPressable(
                onClick = onClick,
                enabled = enabled,
                role = Role.Button,
                pressedScale = LevyraPressScale.Control
            )
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            style = LevyraType.caption.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp,
                letterSpacing = (-0.2).sp
            ),
            color = contentColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
