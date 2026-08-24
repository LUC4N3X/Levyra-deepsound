package com.luc4n3x.levyra.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.LevyraMixKind
import com.luc4n3x.levyra.ui.components.LevyraConnectedDefaults
import com.luc4n3x.levyra.ui.components.LevyraConnectedPosition
import com.luc4n3x.levyra.ui.components.LevyraConnectedStyle
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraConnectedSurface
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraText

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
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(style.gap)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .levyraConnectedSurface(LevyraConnectedPosition.Top, style)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)
        ) {
            Text(
                text = strings.levyraMix,
                color = LevyraText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(strings.mixFamiliarLabel, color = LevyraMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(strings.mixDiscoveryLabel, color = LevyraMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Slider(
                value = 1f - familiarity,
                onValueChange = { onFamiliarityChange(1f - it) },
                enabled = !loading,
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    inactiveTrackColor = LevyraPlayerDesign.TrackInactive
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        LevyraMixActionRow(
            icon = Icons.Rounded.AutoAwesome,
            label = strings.mixForYou,
            accent = accent,
            enabled = !loading,
            position = LevyraConnectedPosition.Middle,
            style = style,
            onClick = { onStartMix(LevyraMixKind.Personalized) }
        )
        LevyraMixActionRow(
            icon = Icons.Rounded.Casino,
            label = strings.surpriseMe,
            accent = accent,
            enabled = !loading,
            position = LevyraConnectedPosition.Middle,
            style = style,
            onClick = { onStartMix(LevyraMixKind.SurpriseMe) }
        )
        LevyraMixActionRow(
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

@Composable
private fun LevyraMixActionRow(
    icon: ImageVector,
    label: String,
    accent: Color,
    enabled: Boolean,
    position: LevyraConnectedPosition,
    style: LevyraConnectedStyle,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .levyraConnectedSurface(position, style, enabled = enabled)
            .levyraPressable(
                onClick = onClick,
                enabled = enabled,
                pressedScale = LevyraPressScale.Row,
                role = Role.Button,
                onClickLabel = label,
                haptic = LevyraHapticAction.Confirm
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) accent else LevyraMuted,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = label,
            color = if (enabled) LevyraText else LevyraMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
