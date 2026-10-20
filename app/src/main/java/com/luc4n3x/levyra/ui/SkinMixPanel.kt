package com.luc4n3x.levyra.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.LevyraMixKind
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.*

@Composable
internal fun SkinMixPanel(
    familiarity: Float, loading: Boolean, accent: Color, modifier: Modifier,
    onFamiliarityChange: (Float) -> Unit, onStartMix: (LevyraMixKind) -> Unit,
    onOpenYourSound: () -> Unit, onOpenMixLab: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val corner = if (LevyraIsNocturne) Nocturne78Design.panelCorner else PrismDesign.surfaceCorner
    val shape = RoundedCornerShape(corner)
    Column(
        modifier.fillMaxWidth().clip(shape).background(LevyraPanel)
            .border(1.dp, LevyraActivePalette.outline.copy(alpha = 0.6f), shape).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    strings.mixForYou, color = LevyraText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = if (LevyraIsTorque) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                SkinMixReadout(familiarity, accent)
            }
            val playShape = if (LevyraIsNocturne) RoundedCornerShape(8.dp) else PrismDesign.cut(24.dp)
            Box(
                Modifier.size(64.dp).clip(playShape).background(accent)
                    .levyraPressable(onClick = { onStartMix(LevyraMixKind.Personalized) }, enabled = !loading,
                        onClickLabel = strings.mixForYou, role = Role.Button)
                    .semantics { contentDescription = strings.mixForYou },
                contentAlignment = Alignment.Center
            ) {
                val content = MaterialTheme.colorScheme.onPrimary
                if (loading) DiscoveryLoadingIndicator(Modifier.size(26.dp), color = content)
                else Icon(Icons.Rounded.PlayArrow, null, tint = content, modifier = Modifier.size(32.dp))
            }
        }
        MixBalanceSlider(familiarity, !loading, accent, strings.mixFamiliarLabel, strings.mixDiscoveryLabel, onFamiliarityChange)
        LevyraSecondaryToolsRow(loading, onStartMix = onStartMix, onOpenYourSound = onOpenYourSound, onOpenMixLab = onOpenMixLab)
    }
}

@Composable
private fun SkinMixReadout(familiarity: Float, accent: Color) {
    if (LevyraIsNocturne) {
        Text(
            "${(familiarity.coerceIn(0f, 1f) * 100).toInt()} / ${((1f - familiarity.coerceIn(0f, 1f)) * 100).toInt()}",
            color = accent, fontFamily = FontFamily.Monospace, fontSize = 20.sp, letterSpacing = 3.sp
        )
        return
    }
    val torque = LevyraIsTorque
    Canvas(Modifier.fillMaxWidth().height(28.dp)) {
        repeat(24) { index ->
            val x = size.width * (index + 0.5f) / 24
            val distance = kotlin.math.abs(index - familiarity.coerceIn(0f, 1f) * 23)
            val height = size.height * (1f - distance / 26).coerceIn(0.15f, 1f)
            drawLine(accent.copy(alpha = (1f - distance / 28).coerceIn(0.2f, 1f)),
                Offset(x, (size.height - height) / 2), Offset(x, (size.height + height) / 2),
                strokeWidth = if (torque) 3.dp.toPx() else 2.dp.toPx())
        }
    }
}
