package com.luc4n3x.levyra.ui.moodjourney

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.MoodCurve
import com.luc4n3x.levyra.domain.MoodCurvePreset
import com.luc4n3x.levyra.ui.atelierUppercase
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.SmartMusicCopy
import com.luc4n3x.levyra.ui.i18n.SmartMusicText
import com.luc4n3x.levyra.ui.i18n.smartMusicCopy
import com.luc4n3x.levyra.ui.theme.AtelierDesign
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign
import com.luc4n3x.levyra.ui.theme.LevyraIsAtelier
import com.luc4n3x.levyra.ui.theme.LevyraIsDefaultSkin
import com.luc4n3x.levyra.ui.theme.LevyraIsQobuz
import com.luc4n3x.levyra.ui.theme.LevyraSkinManagesLayout
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import com.luc4n3x.levyra.ui.theme.QobuzDesign

private val JourneyCoverSize = 148.dp
private const val JourneySamples = 33

private val JourneyPresets = listOf(MoodCurvePreset.Sunrise, MoodCurvePreset.WindDown, MoodCurvePreset.Summit, MoodCurvePreset.Wave)

@Composable
internal fun MoodJourneyEntryCard(onOpen: (MoodCurvePreset?) -> Unit, modifier: Modifier = Modifier) {
    val copy = smartMusicCopy(LocalLevyraStrings.current.code)
    val haptics = LocalLevyraHaptics.current
    val open: (MoodCurvePreset?) -> Unit = { preset ->
        haptics.perform(LevyraHapticAction.Confirm)
        onOpen(preset)
    }
    when {
        LevyraIsAtelier -> AtelierMoodJourneyEntry(copy, open, modifier)
        LevyraIsDefaultSkin -> DefaultMoodJourneyEntry(copy, open, modifier)
        else -> SkinMoodJourneyEntry(copy, open, modifier)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DefaultMoodJourneyEntry(copy: SmartMusicCopy, open: (MoodCurvePreset?) -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            JourneyHeading(copy, Modifier.weight(1f))
            Box(
                Modifier.size(48.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(scheme.surfaceContainerHigh)
                    .clickable(role = Role.Button, onClickLabel = copy[SmartMusicText.MoodTitle]) { open(null) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = scheme.onSurface, modifier = Modifier.size(24.dp))
            }
        }
        JourneyCoverRail(copy, journeyCoverStyle(), open)
    }
}

@Composable
private fun SkinMoodJourneyEntry(copy: SmartMusicCopy, open: (MoodCurvePreset?) -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(role = Role.Button) { open(null) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            JourneyHeading(copy, Modifier.weight(1f))
            Box(Modifier.size(48.dp).clip(LevyraHomeDesign.HeroShape).background(scheme.surfaceContainerHigh), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = scheme.onSurface, modifier = Modifier.size(22.dp))
            }
        }
        JourneyCoverRail(copy, journeyCoverStyle(), open)
    }
}

@Composable
private fun JourneyHeading(copy: SmartMusicCopy, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            copy[SmartMusicText.MoodTitle],
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )
        Text(copy[SmartMusicText.MoodSubtitle], style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
    }
}

@Composable
private fun AtelierMoodJourneyEntry(copy: SmartMusicCopy, open: (MoodCurvePreset?) -> Unit, modifier: Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(AtelierDesign.hairline))
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(role = Role.Button) { open(null) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                atelierUppercase(copy[SmartMusicText.MoodTitle]),
                style = MaterialTheme.typography.labelMedium,
                color = AtelierDesign.textSecondary,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = AtelierDesign.textSecondary, modifier = Modifier.size(18.dp))
        }
        Text(
            copy[SmartMusicText.MoodSubtitle],
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Light),
            color = AtelierDesign.text,
            modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { open(null) }.padding(bottom = 18.dp)
        )
        JourneyCoverRail(copy, journeyCoverStyle(), open)
        Box(Modifier.padding(top = 18.dp).fillMaxWidth().height(1.dp).background(AtelierDesign.hairline))
    }
}

private data class JourneyCoverStyle(
    val corner: Dp,
    val playShape: Shape?,
    val base: Color,
    val vividness: Float,
    val muted: Boolean,
    val labelColor: Color,
    val uppercaseLabel: Boolean
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun journeyCoverStyle(): JourneyCoverStyle {
    val scheme = MaterialTheme.colorScheme
    return when {
        LevyraIsAtelier -> JourneyCoverStyle(
            corner = AtelierDesign.cardCorner,
            playShape = null,
            base = scheme.surface,
            vividness = 0.6f,
            muted = true,
            labelColor = AtelierDesign.text,
            uppercaseLabel = true
        )
        LevyraIsDefaultSkin -> JourneyCoverStyle(
            corner = 20.dp,
            playShape = MaterialShapes.Cookie9Sided.toShape(),
            base = scheme.surfaceContainer,
            vividness = 0.74f,
            muted = false,
            labelColor = scheme.onSurface,
            uppercaseLabel = false
        )
        else -> JourneyCoverStyle(
            corner = when {
                LevyraIsQobuz -> QobuzDesign.cardCorner
                LevyraSkinManagesLayout -> LevyraHomeDesign.HeroCorner.coerceAtMost(16.dp)
                else -> 20.dp
            },
            playShape = CircleShape,
            base = scheme.surfaceContainer,
            vividness = 0.68f,
            muted = false,
            labelColor = scheme.onSurface,
            uppercaseLabel = false
        )
    }
}

@Composable
private fun JourneyCoverRail(copy: SmartMusicCopy, style: JourneyCoverStyle, open: (MoodCurvePreset?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(JourneyPresets, key = { it.name }) { preset ->
            val label = copy[presetLabel(preset)]
            JourneyCoverCard(preset, if (style.uppercaseLabel) atelierUppercase(label) else label, style) { open(preset) }
        }
    }
}

private fun journeyHues(preset: MoodCurvePreset): Pair<Color, Color> = when (preset) {
    MoodCurvePreset.Sunrise -> Color(0xFFFFB36B) to Color(0xFFE2557C)
    MoodCurvePreset.WindDown -> Color(0xFF8C7BFF) to Color(0xFF283A8F)
    MoodCurvePreset.Summit -> Color(0xFF45D6B5) to Color(0xFF1C6488)
    else -> Color(0xFFC77DFF) to Color(0xFF3B6FE8)
}

private fun Color.mutedTowardGray(): Color {
    val gray = luminance().let { Color(it, it, it) }
    return lerp(gray, this, 0.35f)
}

@Composable
private fun JourneyCoverCard(preset: MoodCurvePreset, label: String, style: JourneyCoverStyle, onClick: () -> Unit) {
    val energies = remember(preset) {
        val curve = MoodCurve.preset(preset)
        FloatArray(JourneySamples) { curve.sample(it / (JourneySamples - 1f)).energy }
    }
    val (hueTop, hueBottom) = journeyHues(preset).let { (top, bottom) ->
        if (style.muted) top.mutedTowardGray() to bottom.mutedTowardGray() else top to bottom
    }
    val skyTop = lerp(style.base, hueTop, style.vividness)
    val skyBottom = lerp(style.base, hueBottom, style.vividness)
    val shape = RoundedCornerShape(style.corner)
    Column(
        Modifier.width(JourneyCoverSize).clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier.size(JourneyCoverSize).clip(shape)
                .drawWithCache {
                    val last = JourneySamples - 1
                    val horizon = size.height * 0.40f
                    val relief = size.height - horizon
                    fun front(i: Int) = Offset(i / last.toFloat() * size.width, horizon + (1f - energies[i]) * relief * 0.72f)
                    val frontEdge = Path().apply {
                        for (i in 0..last) front(i).let { if (i == 0) moveTo(it.x, it.y) else lineTo(it.x, it.y) }
                    }
                    val frontLand = Path().apply { addPath(frontEdge); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
                    val sky = Brush.verticalGradient(listOf(skyTop, skyBottom))
                    val shade = Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.20f), Color.Black.copy(alpha = 0.42f)),
                        startY = horizon,
                        endY = size.height
                    )
                    val peak = energies.indices.maxBy { energies[it] }
                    val sunRadius = size.width * 0.052f
                    val sunCenter = front(peak).let { Offset(it.x.coerceIn(sunRadius * 2.5f, size.width - sunRadius * 2.5f), (it.y - sunRadius * 3f).coerceAtLeast(sunRadius * 2.5f)) }
                    val glow = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.26f), Color.Transparent),
                        center = sunCenter,
                        radius = sunRadius * 3.6f
                    )
                    val stroke = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round)
                    onDrawBehind {
                        drawRect(sky)
                        drawCircle(glow, sunRadius * 3.6f, sunCenter)
                        drawCircle(Color.White.copy(alpha = 0.78f), sunRadius, sunCenter)
                        drawPath(frontLand, shade)
                        drawPath(frontEdge, Color.White.copy(alpha = 0.88f), style = stroke)
                    }
                }
        ) {
            if (style.playShape != null) {
                Box(
                    Modifier.align(Alignment.BottomEnd).padding(10.dp).size(40.dp)
                        .clip(style.playShape).background(Color.White.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = lerp(skyBottom, Color.Black, 0.35f), modifier = Modifier.size(22.dp))
                }
            }
        }
        Text(
            label,
            style = if (style.uppercaseLabel) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = style.labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
