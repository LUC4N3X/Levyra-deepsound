package com.luc4n3x.levyra.ui.discovery

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.ChartsCatalog
import com.luc4n3x.levyra.domain.ExploreCategory
import com.luc4n3x.levyra.feature.discovery.BubbleLevel
import com.luc4n3x.levyra.feature.discovery.DiscoveryLens
import com.luc4n3x.levyra.feature.discovery.DiscoveryRequest
import com.luc4n3x.levyra.ui.i18n.SmartMusicCopy
import com.luc4n3x.levyra.ui.i18n.SmartMusicText
import com.luc4n3x.levyra.ui.smartmusic.rememberOrbitMorph
import com.luc4n3x.levyra.ui.smartmusic.rememberSelectionMorph
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val Decades = listOf(1950, 1960, 1970, 1980, 1990, 2000, 2010, 2020)

internal fun bubbleLevelName(level: BubbleLevel): SmartMusicText = when (level) {
    BubbleLevel.Near -> SmartMusicText.BubbleNear
    BubbleLevel.Adventurous -> SmartMusicText.BubbleAdventurous
    BubbleLevel.OutOfOrbit -> SmartMusicText.BubbleOut
}

private fun bubbleLevelHint(level: BubbleLevel): SmartMusicText = when (level) {
    BubbleLevel.Near -> SmartMusicText.BubbleNearHint
    BubbleLevel.Adventurous -> SmartMusicText.BubbleAdventurousHint
    BubbleLevel.OutOfOrbit -> SmartMusicText.BubbleOutHint
}

@Composable
internal fun BubbleOrbitSelector(
    request: DiscoveryRequest,
    scenes: List<ExploreCategory>,
    animated: Boolean,
    copy: SmartMusicCopy,
    ink: Color,
    onLevel: (BubbleLevel) -> Unit,
    onLens: (DiscoveryLens) -> Unit,
    onLoadScenes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalLevyraHaptics.current
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OrbitDiagram(request.level, animated, Modifier.size(44.dp))
            BubbleLevel.entries.forEach { level ->
                val active = level == request.level
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(if (active) 24.dp else 12.dp))
                        .background(if (active) scheme.primaryContainer else scheme.surfaceContainerHigh.copy(alpha = 0.8f))
                        .selectable(selected = active, role = Role.RadioButton) {
                            haptics.perform(LevyraHapticAction.Confirm)
                            onLevel(level)
                        }
                        .padding(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    if (active) Box(Modifier.size(10.dp).clip(rememberSelectionMorph(true, animated)).background(scheme.primary))
                    Text(
                        copy[bubbleLevelName(level)],
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        AnimatedContent(
            targetState = request.level,
            transitionSpec = {
                if (animated) (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
                else fadeIn(snap()) togetherWith fadeOut(snap())
            },
            label = "bubble-level-hint"
        ) { level ->
            Text(copy[bubbleLevelHint(level)], style = MaterialTheme.typography.bodySmall, color = ink.copy(alpha = 0.72f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        LensRow(request.lens, scenes, copy, onLens, onLoadScenes)
    }
}

@Composable
private fun OrbitDiagram(level: BubbleLevel, animated: Boolean, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val ring by animateFloatAsState(
        targetValue = level.ordinal.toFloat(),
        animationSpec = if (animated) spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow) else snap(),
        label = "bubble-orbit-ring"
    )
    val angle = if (animated) {
        rememberInfiniteTransition(label = "bubble-orbit").animateFloat(
            0f, 360f, infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Restart), label = "bubble-orbit-angle"
        ).value
    } else 45f
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val maxRadius = size.minDimension / 2f - 4.dp.toPx()
            for (index in 0..2) {
                val radius = maxRadius * (0.42f + 0.29f * index)
                drawCircle(scheme.outlineVariant.copy(alpha = if (index.toFloat() <= ring + 0.01f) 0.75f else 0.3f), radius, style = Stroke(1.5.dp.toPx()))
            }
            val radius = maxRadius * (0.42f + 0.29f * ring)
            val radians = Math.toRadians(angle.toDouble())
            val satellite = Offset(center.x + radius * cos(radians).toFloat(), center.y + radius * sin(radians).toFloat())
            drawCircle(scheme.tertiary, 5.dp.toPx(), satellite)
        }
        Box(Modifier.size(20.dp).clip(rememberOrbitMorph(level != BubbleLevel.Near, animated)).background(scheme.primary))
    }
}

@Composable
private fun LensRow(
    lens: DiscoveryLens,
    scenes: List<ExploreCategory>,
    copy: SmartMusicCopy,
    onLens: (DiscoveryLens) -> Unit,
    onLoadScenes: () -> Unit
) {
    var countryOpen by remember { mutableStateOf(false) }
    var sceneOpen by remember { mutableStateOf(false) }
    var decadeOpen by remember { mutableStateOf(false) }
    var cityOpen by rememberSaveable { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(copy[SmartMusicText.BubbleExplore], style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LensChip(copy[SmartMusicText.BubbleAnywhere], lens == DiscoveryLens.Open, dropdown = false) { onLens(DiscoveryLens.Open) }
        Box {
            LensChip((lens as? DiscoveryLens.Country)?.label ?: copy[SmartMusicText.BubbleCountry], lens is DiscoveryLens.Country, dropdown = true) { countryOpen = true }
            DropdownMenu(expanded = countryOpen, onDismissRequest = { countryOpen = false }) {
                ChartsCatalog.regions.forEach { region ->
                    DropdownMenuItem(
                        text = { Text("${region.emoji}  ${region.label}") },
                        onClick = {
                            countryOpen = false
                            onLens(DiscoveryLens.Country(region.id, region.label))
                        }
                    )
                }
            }
        }
        Box {
            LensChip((lens as? DiscoveryLens.Scene)?.title ?: copy[SmartMusicText.BubbleScene], lens is DiscoveryLens.Scene, dropdown = true) {
                onLoadScenes()
                sceneOpen = true
            }
            DropdownMenu(expanded = sceneOpen && scenes.isNotEmpty(), onDismissRequest = { sceneOpen = false }) {
                scenes.forEach { scene ->
                    DropdownMenuItem(
                        text = { Text(scene.title) },
                        onClick = {
                            sceneOpen = false
                            onLens(DiscoveryLens.Scene(scene.params, scene.title))
                        }
                    )
                }
            }
        }
        Box {
            LensChip((lens as? DiscoveryLens.Decade)?.let { "${it.startYear}s" } ?: copy[SmartMusicText.BubbleDecade], lens is DiscoveryLens.Decade, dropdown = true) { decadeOpen = true }
            DropdownMenu(expanded = decadeOpen, onDismissRequest = { decadeOpen = false }) {
                Decades.forEach { decade ->
                    DropdownMenuItem(
                        text = { Text("${decade}s") },
                        onClick = {
                            decadeOpen = false
                            onLens(DiscoveryLens.Decade(decade))
                        }
                    )
                }
            }
        }
        LensChip((lens as? DiscoveryLens.City)?.name ?: copy[SmartMusicText.BubbleCity], lens is DiscoveryLens.City, dropdown = false) { cityOpen = true }
    }
    if (cityOpen) {
        CityDialog(copy, onDismiss = { cityOpen = false }) { city ->
            cityOpen = false
            onLens(DiscoveryLens.City(city))
        }
    }
}

@Composable
private fun LensChip(label: String, selected: Boolean, dropdown: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        color = if (selected) scheme.secondaryContainer else scheme.surfaceContainerHigh,
        contentColor = if (selected) scheme.onSecondaryContainer else scheme.onSurface,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 6.dp, bottomEnd = 14.dp, bottomStart = 6.dp),
        modifier = Modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (dropdown) Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CityDialog(copy: SmartMusicCopy, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var city by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(copy[SmartMusicText.BubbleCity]) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it.take(60) },
                    singleLine = true,
                    placeholder = { Text(copy[SmartMusicText.BubbleCityHint]) },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(copy[SmartMusicText.BubbleAttribution], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(city.trim()) }, enabled = city.trim().length >= 2) { Text(copy[SmartMusicText.BubbleCityGo]) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(copy[SmartMusicText.MoodClose]) } }
    )
}

@Composable
internal fun BubbleDistanceBadge(distance: Float, copy: SmartMusicCopy, container: Color, content: Color, modifier: Modifier = Modifier) {
    val percent = (distance.coerceIn(0f, 1f) * 100).roundToInt()
    val label = copy[SmartMusicText.BubbleDistance].replace("{n}", percent.toString())
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(10.dp), modifier = modifier.semantics { contentDescription = label }) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Canvas(Modifier.size(14.dp)) {
                val maxRadius = size.minDimension / 2f
                drawCircle(content.copy(alpha = 0.35f), maxRadius, style = Stroke(1.dp.toPx()))
                drawCircle(content, 2.dp.toPx(), center)
                val radius = maxRadius * (0.25f + 0.75f * distance.coerceIn(0f, 1f))
                drawCircle(content, 2.dp.toPx(), Offset(center.x + radius * 0.7071f, center.y - radius * 0.7071f))
            }
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
internal fun BubbleOrbitEntry(copy: SmartMusicCopy, animated: Boolean, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalLevyraHaptics.current
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 12.dp, bottomEnd = 28.dp, bottomStart = 12.dp))
            .background(scheme.surfaceContainerHigh)
            .clickable {
                haptics.perform(LevyraHapticAction.Confirm)
                onOpen()
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OrbitDiagram(BubbleLevel.Adventurous, animated, Modifier.size(44.dp))
        Column(Modifier.weight(1f)) {
            Text(copy[SmartMusicText.BubbleTitle], style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = scheme.onSurface)
            Text(copy[SmartMusicText.BubbleEntry], style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
