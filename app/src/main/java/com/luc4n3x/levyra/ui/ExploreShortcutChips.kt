package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.unit.sp
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign

private fun shortcutHue(key: String): Color = when (key) {
    "releases" -> Color(0xFFFFB45C)
    "samples" -> Color(0xFFFF7A9C)
    "moods" -> Color(0xFF4FD1B8)
    "radio" -> Color(0xFFFF8A65)
    "podcasts" -> Color(0xFFB18CFF)
    else -> Color(0xFF7CD67A)
}

@Composable
internal fun ExploreShortcutChipRow(entries: List<ExploreShortcutEntry>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = LevyraHomeDesign.HorizontalInset),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { entry ->
                    key(entry.key) { ExploreShortcutChip(entry, Modifier.weight(1f)) }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExploreShortcutChip(entry: ExploreShortcutEntry, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val hue = shortcutHue(entry.key)
    val badge = lerp(colors.surfaceContainerHighest, hue, if (LevyraIsLight) 0.30f else 0.26f)
    val glyph = if (LevyraIsLight) lerp(hue, Color.Black, 0.45f) else hue
    Row(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(CircleShape)
            .background(colors.surfaceContainerHigh)
            .levyraPressable(
                onClick = entry.onClick,
                onClickLabel = entry.label,
                role = Role.Button,
                pressedScale = LevyraPressScale.Control
            )
            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(entry.polygon.toShape()).background(badge),
            contentAlignment = Alignment.Center
        ) {
            Icon(entry.icon, contentDescription = null, tint = glyph, modifier = Modifier.size(20.dp))
        }
        BasicText(
            text = entry.label,
            style = MaterialTheme.typography.titleSmall.copy(color = colors.onSurface, fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 14.sp),
            modifier = Modifier.weight(1f)
        )
    }
}
