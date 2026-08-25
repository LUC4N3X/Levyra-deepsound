package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.LevyraMixKind
import com.luc4n3x.levyra.ui.components.LevyraConnectedDefaults
import com.luc4n3x.levyra.ui.components.LevyraConnectedPosition
import com.luc4n3x.levyra.ui.components.LevyraConnectedStyle
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraConnectedRowSurface
import com.luc4n3x.levyra.ui.components.levyraConnectedSurface
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraOnAccent
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm

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
    onOpenYourSound: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val style = LevyraConnectedDefaults.style(accent = accent)
    val headerWash = remember(accent) {
        Brush.linearGradient(
            listOf(
                accent.copy(alpha = 0.22f),
                accent.copy(alpha = 0.06f),
                Color.Transparent
            )
        )
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(style.gap)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .levyraConnectedSurface(LevyraConnectedPosition.Top, style)
                .background(headerWash)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceMd)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MixCrest(accent = accent, active = loading)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.levyraMix,
                        color = LevyraText,
                        fontSize = 17.sp,
                        letterSpacing = (-0.3).sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = strings.mixCreate,
                        color = LevyraMuted,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
        MixPrimaryAction(
            label = strings.mixForYou,
            accent = accent,
            enabled = !loading,
            style = style,
            onClick = { onStartMix(LevyraMixKind.Personalized) }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(style.gap)) {
            MixSecondaryTile(
                icon = Icons.Rounded.Casino,
                label = strings.surpriseMe,
                accent = accent,
                enabled = !loading,
                position = LevyraConnectedPosition.Top,
                style = style,
                onClick = { onStartMix(LevyraMixKind.SurpriseMe) }
            )
            MixSecondaryTile(
                icon = Icons.Rounded.GraphicEq,
                label = strings.yourSound,
                accent = accent,
                enabled = true,
                position = LevyraConnectedPosition.Bottom,
                style = style,
                onClick = onOpenYourSound
            )
        }
    }
}

@Composable
private fun MixCrest(accent: Color, active: Boolean) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val phase = rememberCrestPhase(active && animationsEnabled)
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(LevyraPlayerDesign.ShapeXs)
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

@Composable
private fun MixBalanceSlider(
    familiarity: Float,
    enabled: Boolean,
    accent: Color,
    familiarLabel: String,
    discoveryLabel: String,
    onFamiliarityChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Slider(
            value = 1f - familiarity,
            onValueChange = { onFamiliarityChange(1f - it) },
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = LevyraPlayerDesign.TrackInactive,
                disabledThumbColor = LevyraMuted,
                disabledActiveTrackColor = LevyraMuted
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "$familiarLabel / $discoveryLabel" }
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = familiarLabel,
                color = if (familiarity >= 0.5f) accent else LevyraMuted,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = discoveryLabel,
                color = if (familiarity < 0.5f) accent else LevyraMuted,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MixPrimaryAction(
    label: String,
    accent: Color,
    enabled: Boolean,
    style: LevyraConnectedStyle,
    onClick: () -> Unit
) {
    val fill = remember(accent, enabled) {
        if (enabled) {
            Brush.horizontalGradient(
                listOf(accent, accent.copy(alpha = 0.72f))
            )
        } else {
            Brush.horizontalGradient(listOf(accent.copy(alpha = 0.24f), accent.copy(alpha = 0.16f)))
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .levyraConnectedSurface(LevyraConnectedPosition.Middle, style, bordered = false)
            .background(fill)
            .levyraPressable(
                onClick = onClick,
                enabled = enabled,
                pressedScale = LevyraPressScale.Surface,
                role = Role.Button,
                onClickLabel = label,
                haptic = LevyraHapticAction.Confirm
            )
            .padding(horizontal = 18.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = LevyraOnAccent,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = LevyraOnAccent,
            fontSize = 15.sp,
            letterSpacing = (-0.2).sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RowScope.MixSecondaryTile(
    icon: ImageVector,
    label: String,
    accent: Color,
    enabled: Boolean,
    position: LevyraConnectedPosition,
    style: LevyraConnectedStyle,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .levyraConnectedRowSurface(position, style, enabled = enabled)
            .levyraPressable(
                onClick = onClick,
                enabled = enabled,
                pressedScale = LevyraPressScale.Tile,
                role = Role.Button,
                onClickLabel = label
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(LevyraPlayerDesign.ShapeXxs)
                .background(accent.copy(alpha = if (enabled) 0.18f else 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) accent else LevyraMuted,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = label,
            color = if (enabled) LevyraText else LevyraMuted,
            fontSize = 13.sp,
            lineHeight = LevyraTypeRhythm.lineHeight(13.sp),
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
