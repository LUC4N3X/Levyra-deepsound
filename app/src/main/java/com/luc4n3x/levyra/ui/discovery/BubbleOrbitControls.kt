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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.border
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.TripOrigin
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.toShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlin.math.abs
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

private fun bubbleLevelIcon(level: BubbleLevel): ImageVector = when (level) {
    BubbleLevel.Near -> Icons.Rounded.TripOrigin
    BubbleLevel.Adventurous -> Icons.Rounded.Explore
    BubbleLevel.OutOfOrbit -> Icons.Rounded.RocketLaunch
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun BubbleOrbitSelector(
    request: DiscoveryRequest,
    scenes: List<ExploreCategory>,
    animated: Boolean,
    copy: SmartMusicCopy,
    onLevel: (BubbleLevel) -> Unit,
    onLens: (DiscoveryLens) -> Unit,
    onLoadScenes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalLevyraHaptics.current
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OrbitDiagram(request.level, animated, Modifier.size(48.dp))
            Row(
                modifier = Modifier.weight(1f).height(IntrinsicSize.Min).selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                BubbleLevel.entries.forEachIndexed { index, level ->
                    val checked = level == request.level
                    val label = copy[bubbleLevelName(level)]
                    val weight by animateFloatAsState(
                        targetValue = if (checked) 2.6f else 1f,
                        animationSpec = if (animated) spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow) else snap(),
                        label = "bubble-level-weight"
                    )
                    ToggleButton(
                        checked = checked,
                        onCheckedChange = {
                            if (!checked) {
                                haptics.perform(LevyraHapticAction.Confirm)
                                onLevel(level)
                            }
                        },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            BubbleLevel.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        },
                        colors = ToggleButtonDefaults.colors(
                            containerColor = scheme.surfaceContainerHigh,
                            contentColor = scheme.onSurface
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxHeight()
                            .heightIn(min = 48.dp)
                            .semantics {
                                role = Role.RadioButton
                                contentDescription = label
                            }
                    ) {
                        Icon(bubbleLevelIcon(level), contentDescription = null, modifier = Modifier.size(18.dp))
                        AnimatedVisibility(visible = checked) {
                            Row {
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 14.sp)
                                )
                            }
                        }
                    }
                }
            }
        }
        AnimatedContent(
            targetState = request.level,
            transitionSpec = {
                if (animated) {
                    slideInVertically { it / 2 } + fadeIn() togetherWith slideOutVertically { -it / 2 } + fadeOut()
                } else {
                    fadeIn(snap()) togetherWith fadeOut(snap())
                }
            },
            label = "bubble-level-hint",
            modifier = Modifier.padding(horizontal = 20.dp)
        ) { level ->
            Text(
                copy[bubbleLevelHint(level)],
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        LensRow(request.lens, scenes, animated, copy, onLens, onLoadScenes)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
            0f, 360f, infiniteRepeatable(tween(16_000, easing = LinearEasing), RepeatMode.Restart), label = "bubble-orbit-angle"
        )
    } else null
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val maxRadius = size.minDimension / 2f - 4.dp.toPx()
            drawCircle(Brush.radialGradient(listOf(scheme.primary.copy(alpha = 0.22f), Color.Transparent), center, maxRadius), maxRadius)
            for (index in 0..2) {
                val radius = maxRadius * (0.42f + 0.29f * index)
                val active = abs(index - ring) < 0.5f
                drawCircle(
                    color = if (active) scheme.tertiary else scheme.outlineVariant.copy(alpha = 0.45f),
                    radius = radius,
                    style = Stroke(
                        width = if (active) 2.5.dp.toPx() else 1.dp.toPx(),
                        pathEffect = if (index == 2 && !active) PathEffect.dashPathEffect(floatArrayOf(6f, 6f)) else null
                    )
                )
            }
            val radius = maxRadius * (0.42f + 0.29f * ring)
            val radians = Math.toRadians((angle?.value ?: 45f).toDouble())
            val satellite = Offset(center.x + radius * cos(radians).toFloat(), center.y + radius * sin(radians).toFloat())
            drawCircle(scheme.tertiary.copy(alpha = 0.3f), 8.dp.toPx(), satellite)
            drawCircle(scheme.tertiary, 4.5.dp.toPx(), satellite)
        }
        Box(Modifier.fillMaxSize(0.32f).clip(rememberOrbitMorph(level != BubbleLevel.Near, animated)).background(scheme.primary))
    }
}

@Composable
private fun LensRow(
    lens: DiscoveryLens,
    scenes: List<ExploreCategory>,
    animated: Boolean,
    copy: SmartMusicCopy,
    onLens: (DiscoveryLens) -> Unit,
    onLoadScenes: () -> Unit
) {
    var countryOpen by remember { mutableStateOf(false) }
    var sceneOpen by remember { mutableStateOf(false) }
    var decadeOpen by remember { mutableStateOf(false) }
    var cityOpen by rememberSaveable { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LensChip(Icons.Rounded.Public, copy[SmartMusicText.BubbleAnywhere], lens == DiscoveryLens.Open, dropdown = false, animated = animated) {
            onLens(DiscoveryLens.Open)
        }
        Box {
            LensChip(Icons.Rounded.Flag, (lens as? DiscoveryLens.Country)?.label ?: copy[SmartMusicText.BubbleCountry], lens is DiscoveryLens.Country, dropdown = true, animated = animated) {
                countryOpen = true
            }
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
            LensChip(Icons.Rounded.Category, (lens as? DiscoveryLens.Scene)?.title ?: copy[SmartMusicText.BubbleScene], lens is DiscoveryLens.Scene, dropdown = true, animated = animated) {
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
            LensChip(Icons.Rounded.History, (lens as? DiscoveryLens.Decade)?.let { "${it.startYear}s" } ?: copy[SmartMusicText.BubbleDecade], lens is DiscoveryLens.Decade, dropdown = true, animated = animated) {
                decadeOpen = true
            }
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
        LensChip(Icons.Rounded.LocationCity, (lens as? DiscoveryLens.City)?.name ?: copy[SmartMusicText.BubbleCity], lens is DiscoveryLens.City, dropdown = false, animated = animated) {
            cityOpen = true
        }
    }
    if (cityOpen) {
        CityDialog(copy, onDismiss = { cityOpen = false }) { city ->
            cityOpen = false
            onLens(DiscoveryLens.City(city))
        }
    }
}

@Composable
private fun LensChip(icon: ImageVector, label: String, selected: Boolean, dropdown: Boolean, animated: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val corner by animateDpAsState(
        targetValue = if (selected) 22.dp else 12.dp,
        animationSpec = if (animated) spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow) else snap(),
        label = "bubble-lens-corner"
    )
    val shape = RoundedCornerShape(corner)
    Row(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(if (selected) scheme.secondaryContainer else scheme.surfaceContainerHigh)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier.size(30.dp).clip(rememberSelectionMorph(selected, animated)).background(if (selected) scheme.secondary else scheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (selected) scheme.onSecondary else scheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) scheme.onSecondaryContainer else scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 180.dp)
        )
        if (dropdown) Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun BubbleOrbitEntry(
    copy: SmartMusicCopy,
    animated: Boolean,
    artworks: List<String>,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalLevyraHaptics.current
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(role = Role.Button) {
                haptics.perform(LevyraHapticAction.Confirm)
                onOpen()
            }
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ArtworkConstellation(artworks, animated)
        Column(Modifier.weight(1f)) {
            Text(
                copy[SmartMusicText.BubbleTitle],
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                copy[SmartMusicText.BubbleEntry],
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ArtworkConstellation(artworks: List<String>, animated: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val shapes = listOf(MaterialShapes.Cookie9Sided.toShape(), MaterialShapes.Clover4Leaf.toShape(), MaterialShapes.Sunny.toShape())
    val fallback = listOf(scheme.tertiaryContainer, scheme.secondaryContainer, scheme.primaryContainer)
    val spin = if (animated) {
        rememberInfiniteTransition(label = "bubble-constellation").animateFloat(
            0f, 360f, infiniteRepeatable(tween(40_000, easing = LinearEasing), RepeatMode.Restart), label = "bubble-constellation-spin"
        )
    } else null
    Box(Modifier.size(width = 84.dp, height = 44.dp)) {
        repeat(3) { index ->
            val url = artworks.getOrNull(index)
            Box(
                Modifier
                    .offset(x = (index * 20).dp)
                    .size(44.dp)
                    .graphicsLayer { rotationZ = (spin?.value ?: 0f) * if (index % 2 == 0) 1f else -1f }
                    .clip(shapes[index])
                    .background(fallback[index])
                    .border(2.dp, scheme.surface, shapes[index])
            ) {
                if (url != null) {
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize().graphicsLayer { rotationZ = -((spin?.value ?: 0f) * if (index % 2 == 0) 1f else -1f) }
                    )
                }
            }
        }
    }
}
