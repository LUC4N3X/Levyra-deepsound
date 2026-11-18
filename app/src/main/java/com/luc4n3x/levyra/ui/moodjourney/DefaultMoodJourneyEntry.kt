package com.luc4n3x.levyra.ui.moodjourney

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.MoodCurve
import com.luc4n3x.levyra.domain.MoodCurvePreset
import com.luc4n3x.levyra.ui.components.LevyraGroupedInnerCorner
import com.luc4n3x.levyra.ui.components.LevyraGroupedOuterCorner
import com.luc4n3x.levyra.ui.components.LevyraPastelAmber
import com.luc4n3x.levyra.ui.components.LevyraPastelLavender
import com.luc4n3x.levyra.ui.components.LevyraPastelPeriwinkle
import com.luc4n3x.levyra.ui.components.LevyraPastelRose
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraGroupedGridShape
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.components.levyraUtilityColors
import com.luc4n3x.levyra.ui.i18n.SmartMusicCopy
import com.luc4n3x.levyra.ui.i18n.SmartMusicText

private val DefaultJourneyGap = 3.dp
private val DefaultJourneyTileHeight = 104.dp
private const val DefaultJourneySamples = 24

private fun defaultJourneySeed(preset: MoodCurvePreset): Color = when (preset) {
    MoodCurvePreset.Sunrise -> LevyraPastelAmber
    MoodCurvePreset.WindDown -> LevyraPastelLavender
    MoodCurvePreset.Summit -> LevyraPastelPeriwinkle
    else -> LevyraPastelRose
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DefaultMoodJourneyEntry(
    copy: SmartMusicCopy,
    presets: List<MoodCurvePreset>,
    open: (MoodCurvePreset?) -> Unit,
    modifier: Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val tone = levyraUtilityColors(LevyraPastelLavender)
    val title = copy[SmartMusicText.MoodTitle]
    val rows = presets.chunked(2)
    Column(modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(DefaultJourneyGap)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        topStart = LevyraGroupedOuterCorner,
                        topEnd = LevyraGroupedOuterCorner,
                        bottomStart = LevyraGroupedInnerCorner,
                        bottomEnd = LevyraGroupedInnerCorner
                    )
                )
                .background(scheme.surfaceContainer)
                .padding(start = 16.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(44.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(tone.accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Timeline, contentDescription = null, tint = tone.onAccent, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                Text(copy[SmartMusicText.MoodSubtitle], style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
            }
            FilledTonalIconButton(
                onClick = { open(null) },
                shape = MaterialShapes.Cookie4Sided.toShape(),
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = tone.container, contentColor = tone.onContainer),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = title, modifier = Modifier.size(24.dp))
            }
        }
        rows.forEachIndexed { rowIndex, row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(DefaultJourneyGap)) {
                row.forEachIndexed { columnIndex, preset ->
                    DefaultJourneyPresetTile(
                        preset = preset,
                        label = copy[presetLabel(preset)],
                        shape = levyraGroupedGridShape(rowIndex + 1, columnIndex, rows.size + 1, row.size),
                        onClick = { open(preset) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DefaultJourneyPresetTile(
    preset: MoodCurvePreset,
    label: String,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val tone = levyraUtilityColors(defaultJourneySeed(preset))
    val energies = remember(preset) {
        val curve = MoodCurve.preset(preset)
        FloatArray(DefaultJourneySamples) { curve.sample(it / (DefaultJourneySamples - 1f)).energy }
    }
    Box(
        modifier
            .heightIn(min = DefaultJourneyTileHeight)
            .clip(shape)
            .background(scheme.surfaceContainer)
            .drawWithCache {
                val last = DefaultJourneySamples - 1
                val top = size.height * 0.46f
                val band = size.height - top - 14.dp.toPx()
                fun point(i: Int) = Offset(i / last.toFloat() * size.width, top + (1f - energies[i]) * band)
                val edge = Path().apply {
                    for (i in 0..last) point(i).let { if (i == 0) moveTo(it.x, it.y) else lineTo(it.x, it.y) }
                }
                val area = Path().apply { addPath(edge); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
                val fill = Brush.verticalGradient(listOf(tone.accent.copy(alpha = 0.32f), tone.accent.copy(alpha = 0.04f)), startY = top, endY = size.height)
                val stroke = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                onDrawBehind {
                    drawPath(area, fill)
                    drawPath(edge, tone.accent, style = stroke)
                }
            }
            .semantics(mergeDescendants = true) {}
            .levyraPressable(onClick = onClick, role = Role.Button, onClickLabel = label, pressedScale = LevyraPressScale.Tile)
            .padding(16.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMediumEmphasized,
            color = scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
