package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.theme.LevyraIsNocturne
import com.luc4n3x.levyra.ui.theme.Nocturne78Design

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
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
import androidx.compose.ui.graphics.Brush
import com.luc4n3x.levyra.ui.components.levyraExpressiveCorner
import com.luc4n3x.levyra.ui.theme.LevyraPlayerShapes
import com.luc4n3x.levyra.ui.theme.LevyraSegment
import com.luc4n3x.levyra.ui.components.levyraExpressiveToggleCorner
import androidx.compose.ui.semantics.selected
import kotlin.math.roundToInt

@Composable
internal fun LevyraMixLauncherPanel(
    familiarity: Float,
    loading: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onFamiliarityChange: (Float) -> Unit,
    onStartMix: (LevyraMixKind) -> Unit,
    onOpenYourSound: () -> Unit,
    onOpenMixLab: () -> Unit,
    coverSources: LevyraMixCoverSources = LevyraMixCoverSources()
) {
    if (com.luc4n3x.levyra.ui.theme.LevyraSkinManagesLayout) {
        SkinMixPanel(familiarity, loading, accent, modifier, onFamiliarityChange, onStartMix, onOpenYourSound, onOpenMixLab)
        return
    }
    if (com.luc4n3x.levyra.ui.theme.LevyraIsDefaultSkin) {
        ForYouMixPanel(familiarity, loading, accent, coverSources, modifier, onFamiliarityChange, onStartMix, onOpenYourSound, onOpenMixLab)
        return
    }
    val colors = MaterialTheme.colorScheme
    val nocturne = LevyraIsNocturne
    val panelShape = RoundedCornerShape(if (nocturne) Nocturne78Design.panelCorner else 28.dp)
    val wash = remember(accent, colors.surfaceContainerHigh, nocturne) {
        Brush.linearGradient(
            0f to accent.copy(alpha = if (nocturne) 0f else 0.34f),
            0.55f to accent.copy(alpha = if (nocturne) 0f else 0.10f),
            1f to colors.surfaceContainerHigh.copy(alpha = 0f)
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(panelShape)
            .background(colors.surfaceContainer)
            .background(wash)
            .border(1.dp, accent.copy(alpha = 0.22f), panelShape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val strings = LocalLevyraStrings.current
        LevyraMixCoverRail(
            sources = coverSources,
            loading = loading,
            accent = accent,
            mixForYou = strings.mixForYou,
            onRepeat = strings.collectionRepeat,
            throwback = strings.collectionThrowback,
            surprise = strings.surpriseMe,
            onStartMix = onStartMix
        )
        MixBalanceSlider(
            familiarity = familiarity,
            enabled = !loading,
            accent = accent,
            familiarLabel = strings.mixFamiliarLabel,
            discoveryLabel = strings.mixDiscoveryLabel,
            onFamiliarityChange = onFamiliarityChange
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
internal fun MixBalanceSlider(
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
