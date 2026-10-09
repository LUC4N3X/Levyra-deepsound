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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

private const val CrestBarCount = 9
private val CrestPhases = floatArrayOf(0.35f, 0.72f, 0.44f, 0.95f, 0.58f, 0.86f, 0.40f, 0.68f, 0.30f)

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
    val panelShape = RoundedCornerShape(28.dp)
    val panelBorder = remember(colors.outlineVariant) {
        colors.outlineVariant.copy(alpha = 0.30f)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = panelShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.22f),
                spotColor = Color.Black.copy(alpha = 0.30f)
            )
            .clip(panelShape)
            .background(colors.surfaceContainer)
            .border(1.dp, panelBorder, panelShape)
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
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.4f))
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
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = strings.levyraMix,
                        style = LevyraType.screenTitle,
                        color = colors.onSurface,
                        modifier = Modifier.semantics { heading() }
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.surfaceContainerHigh)
                            .border(0.75.dp, colors.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PRO DSP",
                            style = LevyraType.overline.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                                fontSize = 8.5.sp
                            ),
                            color = colors.onSurfaceVariant
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MixCrest(accent = accent, active = loading)
                    Text(
                        text = strings.mixCreate,
                        style = LevyraType.metadata,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            FilledIconButton(
                onClick = { onStartMix(LevyraMixKind.Personalized) },
                enabled = !loading,
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = accent,
                    contentColor = colors.surface
                ),
                modifier = Modifier
                    .size(50.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.25f),
                        spotColor = Color.Black.copy(alpha = 0.35f)
                    )
                    .semantics { contentDescription = strings.mixForYou }
            ) {
                if (loading) {
                    DiscoveryLoadingIndicator(
                        modifier = Modifier.size(22.dp),
                        color = colors.surface
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        SonicReactorSpectrum(
            familiarity = familiarity,
            accent = accent,
            active = loading
        )

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

@Composable
private fun SonicReactorSpectrum(
    familiarity: Float,
    accent: Color,
    active: Boolean,
    modifier: Modifier = Modifier
) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val phase = rememberReactorPhase(animationsEnabled)
    val colors = MaterialTheme.colorScheme
    val discoveryFactor = (1f - familiarity).coerceIn(0f, 1f)
    val harmonicColor = remember(accent, colors.secondary, discoveryFactor) {
        lerp(accent, colors.secondary, discoveryFactor * 0.75f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceContainerLowest.copy(alpha = 0.85f))
            .border(
                0.75.dp,
                colors.outlineVariant.copy(alpha = 0.25f),
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val slot = size.width / ReactorBarCount
            val barWidth = (slot * 0.36f).coerceIn(2.5f, 5.5f)
            val cornerRad = CornerRadius(1.5f, 1.5f)
            val drift = if (animationsEnabled) phase.value else 0f
            val baseEnergy = if (active) 1.3f else 1.0f

            for (index in 0 until ReactorBarCount) {
                val base = ReactorBasePhases[index]
                val waveFreq = 1.0f + discoveryFactor * 1.5f
                val dynamicWave = if (animationsEnabled) {
                    val angle = (drift * waveFreq + index * 0.36f) * 3.14159f
                    abs(sin(angle))
                } else {
                    0.5f
                }
                val heightMultiplier = (base + (1f - base) * dynamicWave * baseEnergy)
                    .coerceIn(0.14f, 1.0f)
                val barHeight = size.height * heightMultiplier
                val x = index * slot + (slot - barWidth) / 2f
                val y = (size.height - barHeight) / 2f
                val barColor = if (index % 2 == 0) accent else harmonicColor

                drawRoundRect(
                    color = barColor.copy(alpha = (0.45f + heightMultiplier * 0.55f).coerceIn(0f, 1f)),
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
            MixToolAction(
                icon = Icons.Rounded.Casino,
                label = strings.surpriseMe,
                accent = accent,
                enabled = !loading,
                modifier = actionModifier
            ) { onStartMix(LevyraMixKind.SurpriseMe) }

            MixToolAction(
                icon = Icons.Rounded.GraphicEq,
                label = strings.yourSound,
                accent = accent,
                enabled = true,
                modifier = actionModifier,
                onClick = onOpenYourSound
            )

            MixToolAction(
                icon = Icons.Rounded.Tune,
                label = strings.mixLab,
                accent = accent,
                enabled = true,
                modifier = actionModifier,
                onClick = onOpenMixLab
            )
        }

        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                actions(Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                actions(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MixToolAction(
    icon: ImageVector,
    label: String,
    accent: Color,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)

    val actionModifier = modifier
        .heightIn(min = 60.dp)
        .clip(shape)
        .background(colors.surfaceContainerHigh.copy(alpha = 0.55f))
        .border(1.dp, colors.outlineVariant.copy(alpha = 0.22f), shape)
        .semantics(mergeDescendants = true) {}
        .levyraPressable(
            onClick = onClick,
            enabled = enabled,
            pressedScale = LevyraPressScale.Control,
            role = Role.Button
        )
        .padding(horizontal = 4.dp, vertical = 8.dp)

    Column(
        modifier = actionModifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (enabled) accent.copy(alpha = 0.12f) else colors.onSurface.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) accent else colors.onSurfaceVariant,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = label,
            style = LevyraType.caption.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.5.sp,
                letterSpacing = (-0.2).sp
            ),
            color = if (enabled) colors.onSurface else colors.onSurfaceVariant,
            softWrap = false,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MixCrest(accent: Color, active: Boolean) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val phase = rememberCrestPhase(active && animationsEnabled)
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
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
                    base + (1f - base) * abs(sin((drift + index * 0.35f) * 3.14159f))
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = familiarLabel,
                    style = LevyraType.caption.copy(fontWeight = FontWeight.Medium),
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceContainerHigh)
                    .border(0.75.dp, colors.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = when {
                        familiarity >= 0.65f -> "$percentFamiliar% $familiarLabel"
                        familiarity <= 0.35f -> "$percentDiscovery% $discoveryLabel"
                        else -> strings.mix
                    },
                    style = LevyraType.overline.copy(
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    maxLines = 1,
                    softWrap = false
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = discoveryLabel,
                    style = LevyraType.caption.copy(fontWeight = FontWeight.Medium),
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false
                )
            }
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
                        .size(width = 16.dp, height = 24.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(6.dp),
                            ambientColor = Color.Black.copy(alpha = 0.3f),
                            spotColor = Color.Black.copy(alpha = 0.4f)
                        )
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (enabled) accent else colors.onSurfaceVariant)
                        .border(1.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(10.dp)
                            .background(Color.White.copy(alpha = 0.85f), CircleShape)
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
                accent = accent,
                modifier = Modifier.weight(1f)
            ) {
                onFamiliarityChange(0.85f)
                sliderState.value = 0.15f
            }

            MixPresetChip(
                label = strings.mix,
                icon = Icons.Rounded.GraphicEq,
                isSelected = familiarity in 0.35f..0.69f,
                enabled = enabled,
                accent = accent,
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
                accent = accent,
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
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(10.dp)
    val container by animateColorAsState(
        targetValue = if (isSelected) accent.copy(alpha = 0.14f) else colors.surfaceContainerHigh.copy(alpha = 0.45f),
        label = "mixPresetContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) accent else colors.onSurfaceVariant,
        label = "mixPresetContent"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accent.copy(alpha = 0.45f) else colors.outlineVariant.copy(alpha = 0.22f),
        label = "mixPresetBorder"
    )

    Row(
        modifier = modifier
            .heightIn(min = 34.dp)
            .clip(shape)
            .background(container)
            .border(1.dp, borderColor, shape)
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
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = label,
            style = LevyraType.caption.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp,
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
