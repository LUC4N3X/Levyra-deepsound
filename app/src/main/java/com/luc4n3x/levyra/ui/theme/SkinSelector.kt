package com.luc4n3x.levyra.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.LevyraSkin
import com.luc4n3x.levyra.ui.i18n.LevyraSkinStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings

@Composable
internal fun SkinSelector(selected: LevyraSkin, onSelect: (LevyraSkin) -> Unit) {
    val copy = LevyraSkinStrings.forCode(LocalLevyraStrings.current.code)
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(copy.title, style = MaterialTheme.typography.titleLarge, color = LevyraText, modifier = Modifier.semantics { heading() })
        LevyraSkin.entries.forEach { skin ->
            val nocturne = skin == LevyraSkin.Nocturne78
            val title = if (nocturne) Nocturne78Palette.label else "Levyra Default"
            val active = selected == skin
            Surface(
                modifier = Modifier.fillMaxWidth().selectable(selected = active, role = Role.RadioButton, onClick = { onSelect(skin) }),
                shape = MaterialTheme.shapes.medium,
                color = LevyraPanel,
                border = BorderStroke(if (active) 2.dp else 1.dp, if (active) LevyraCyan else LevyraGlassBorder)
            ) {
                Column(Modifier.padding(PaddingValues(16.dp)), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkinPreview(nocturne)
                    Text(title, style = MaterialTheme.typography.titleMedium, color = LevyraText)
                    Text(if (nocturne) copy.nocturneBody else copy.defaultBody, style = MaterialTheme.typography.bodyMedium, color = LevyraMuted)
                }
            }
        }
        if (selected == LevyraSkin.Nocturne78) {
            SkinManagedNotice { onSelect(LevyraSkin.Default) }
        }
    }
}

@Composable
internal fun SkinManagedNotice(onUseDefault: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            LevyraSkinStrings.managedForCode(LocalLevyraStrings.current.code),
            style = MaterialTheme.typography.bodyMedium,
            color = LevyraMuted
        )
        if (onUseDefault != null) {
            TextButton(onClick = onUseDefault) { Text("Levyra Default") }
        }
    }
}

@Composable
private fun SkinPreview(nocturne: Boolean) {
    val palette = if (nocturne) Nocturne78Palette else LevyraThemes.levyraAura
    Canvas(Modifier.fillMaxWidth().height(76.dp)) {
        val radius = if (nocturne) 6.dp.toPx() else 18.dp.toPx()
        drawRoundRect(palette.black, cornerRadius = CornerRadius(radius))
        val artSize = size.height - 20.dp.toPx()
        val inset = 10.dp.toPx()
        drawRoundRect(palette.panelSoft, Offset(inset, inset), Size(artSize, artSize), CornerRadius(radius))
        drawCircle(palette.muted, artSize * 0.24f, Offset(inset + artSize / 2, size.height / 2), style = Stroke(1.dp.toPx()))
        drawCircle(palette.cyan, 2.dp.toPx(), Offset(inset + artSize / 2, size.height / 2))
        val left = artSize + inset * 2
        val right = size.width - 52.dp.toPx()
        drawLine(palette.text, Offset(left, inset * 2), Offset(left + (right - left) * 0.8f, inset * 2), 3.dp.toPx())
        drawLine(palette.muted, Offset(left, inset * 3.2f), Offset(left + (right - left) * 0.5f, inset * 3.2f), 2.dp.toPx())
        drawLine(palette.panelSoft, Offset(left, size.height - inset * 2), Offset(right, size.height - inset * 2), 2.dp.toPx())
        drawLine(palette.cyan, Offset(left, size.height - inset * 2), Offset(left + (right - left) * 0.4f, size.height - inset * 2), 2.dp.toPx())
        val center = Offset(size.width - 28.dp.toPx(), size.height / 2)
        drawRoundRect(palette.panelSoft, center - Offset(17.dp.toPx(), 20.dp.toPx()), Size(34.dp.toPx(), 40.dp.toPx()), CornerRadius(radius))
        val glyph = Path().apply {
            moveTo(center.x - 4.dp.toPx(), center.y - 7.dp.toPx())
            lineTo(center.x + 6.dp.toPx(), center.y)
            lineTo(center.x - 4.dp.toPx(), center.y + 7.dp.toPx())
            close()
        }
        drawPath(glyph, palette.cyan, style = if (nocturne) Stroke(1.5.dp.toPx()) else androidx.compose.ui.graphics.drawscope.Fill)
    }
}
