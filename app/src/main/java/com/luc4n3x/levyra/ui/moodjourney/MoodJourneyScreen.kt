package com.luc4n3x.levyra.ui.moodjourney

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.MoodCurve
import com.luc4n3x.levyra.domain.MoodCurvePreset
import com.luc4n3x.levyra.domain.MoodJourneyRequest
import com.luc4n3x.levyra.domain.MoodJourneyStep
import com.luc4n3x.levyra.domain.MoodPoint
import com.luc4n3x.levyra.ui.components.LevyraBackButton
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.SmartMusicCopy
import com.luc4n3x.levyra.ui.i18n.SmartMusicText
import com.luc4n3x.levyra.ui.i18n.smartMusicCopy
import com.luc4n3x.levyra.ui.smartmusic.rememberPinMorph
import com.luc4n3x.levyra.ui.smartmusic.rememberSelectionMorph
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import com.luc4n3x.levyra.viewmodel.MoodJourneyController
import com.luc4n3x.levyra.viewmodel.MoodJourneySession
import com.luc4n3x.levyra.viewmodel.MoodJourneyStage
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt

private val CurveHeight = 248.dp
private val CurvePadding = 22.dp
private val PointSize = 30.dp

@Composable
internal fun MoodJourneyScreen(
    session: MoodJourneySession,
    controller: MoodJourneyController,
    animated: Boolean,
    onClose: () -> Unit,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit
) {
    BackHandler(onBack = onClose)
    val strings = LocalLevyraStrings.current
    val copy = smartMusicCopy(strings.code)
    val scheme = MaterialTheme.colorScheme
    var selectedPoint by rememberSaveable { mutableIntStateOf(0) }
    var saveOpen by remember { mutableStateOf(false) }
    LaunchedEffect(session.savedPlaylistId) { if (session.savedPlaylistId != null) saveOpen = false }
    val mood = averageMood(session.curve)
    val wash = lerp(scheme.secondaryContainer, scheme.tertiaryContainer, mood.energy)
    Surface(modifier = Modifier.fillMaxSize(), color = scheme.surface) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to wash.copy(alpha = 0.55f), 0.45f to scheme.surface))) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().statusBarsPadding(),
                contentPadding = PaddingValues(bottom = 132.dp)
            ) {
                item(key = "header") {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        LevyraBackButton(onClick = onClose, contentDescription = copy[SmartMusicText.MoodClose])
                        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                            Text(
                                copy[SmartMusicText.MoodTitle],
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = scheme.onSurface,
                                modifier = Modifier.semantics { heading() }
                            )
                            Text(copy[SmartMusicText.MoodSubtitle], style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                    }
                }
                item(key = "curve") {
                    CurveEditor(
                        session = session,
                        selected = selectedPoint.coerceIn(0, session.curve.points.lastIndex),
                        animated = animated,
                        copy = copy,
                        onSelect = { selectedPoint = it },
                        onMove = controller::movePoint,
                        onCommit = controller::commitCurve,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                item(key = "controls") {
                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PresetRow(animated, copy, controller::applyPreset)
                        BrightnessControl(session, selectedPoint.coerceIn(0, session.curve.points.lastIndex), copy, controller)
                        LengthStepper(session.trackCount, copy, controller::setTrackCount)
                        JourneyStatus(session, copy, animated)
                    }
                }
                val steps = session.result?.steps.orEmpty()
                itemsIndexed(steps, key = { _, step -> step.key }) { index, step ->
                    JourneyStepRow(
                        index = index,
                        step = step,
                        animated = animated,
                        copy = copy,
                        onPin = { controller.togglePin(step.key) },
                        onExclude = { controller.exclude(step.key) },
                        modifier = Modifier
                            .then(if (animated) Modifier.animateItem() else Modifier)
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
            JourneyActions(
                session = session,
                copy = copy,
                onPlay = onPlay,
                onAddToQueue = onAddToQueue,
                onSave = { saveOpen = true },
                onRegenerate = controller::regenerate,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
    if (saveOpen) {
        val defaultName = remember(strings.code) {
            val date = runCatching {
                LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.forLanguageTag(strings.code)))
            }.getOrDefault(LocalDate.now().toString())
            "${copy[SmartMusicText.MoodTitle]} · $date"
        }
        SaveJourneyDialog(defaultName, session, copy, onDismiss = { saveOpen = false }, onConfirm = controller::saveAsPlaylist)
    }
}

private fun averageMood(curve: MoodCurve): MoodPoint {
    val points = curve.normalized
    return MoodPoint(points.map { it.energy }.average().toFloat(), points.map { it.valence }.average().toFloat())
}

@Composable
private fun CurveEditor(
    session: MoodJourneySession,
    selected: Int,
    animated: Boolean,
    copy: SmartMusicCopy,
    onSelect: (Int) -> Unit,
    onMove: (Int, MoodPoint) -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalLevyraHaptics.current
    val density = LocalDensity.current
    val curve by rememberUpdatedState(session.curve)
    val select by rememberUpdatedState(onSelect)
    val move by rememberUpdatedState(onMove)
    val commit by rememberUpdatedState(onCommit)
    val calm = scheme.secondary
    val bright = scheme.tertiary
    Surface(color = scheme.surfaceContainerLow, shape = RoundedCornerShape(topStart = 40.dp, topEnd = 20.dp, bottomEnd = 40.dp, bottomStart = 20.dp), modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(CurveHeight)
                .semantics { contentDescription = copy[SmartMusicText.MoodCurveDescription] }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val nearest = nearestPoint(curve, offset.x, size.width.toFloat(), with(density) { CurvePadding.toPx() })
                        haptics.perform(LevyraHapticAction.Confirm)
                        select(nearest)
                    }
                }
                .pointerInput(Unit) {
                    var active = 0
                    var lastBand = 0
                    val pad = with(density) { CurvePadding.toPx() }
                    detectDragGestures(
                        onDragStart = { offset ->
                            active = nearestPoint(curve, offset.x, size.width.toFloat(), pad)
                            lastBand = (curve.points[active].energy * 4).toInt()
                            haptics.perform(LevyraHapticAction.Confirm)
                            select(active)
                        },
                        onDragEnd = { commit() },
                        onDragCancel = { commit() }
                    ) { change, _ ->
                        change.consume()
                        val usable = (size.height - pad * 2).coerceAtLeast(1f)
                        val energy = (1f - (change.position.y - pad) / usable).coerceIn(0f, 1f)
                        val band = (energy * 4).toInt()
                        if (band != lastBand) {
                            lastBand = band
                            haptics.perform(LevyraHapticAction.SeekSnap)
                        }
                        val current = curve.points.getOrNull(active) ?: return@detectDragGestures
                        move(active, MoodPoint(energy, current.valence))
                    }
                }
        ) {
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }
            val pad = with(density) { CurvePadding.toPx() }
            val steps = session.result?.steps.orEmpty()
            Canvas(Modifier.fillMaxSize()) {
                val usableW = size.width - pad * 2
                val usableH = size.height - pad * 2
                fun px(t: Float) = pad + t * usableW
                fun py(e: Float) = pad + (1f - e) * usableH
                for (band in 1..3) {
                    val gy = pad + usableH * band / 4f
                    drawLine(scheme.outlineVariant.copy(alpha = 0.28f), Offset(pad, gy), Offset(size.width - pad, gy), 1.dp.toPx())
                }
                val samples = 72
                val stroke = Path()
                val area = Path()
                for (i in 0..samples) {
                    val t = i / samples.toFloat()
                    val point = curve.sample(t)
                    val x = px(t)
                    val y = py(point.energy)
                    if (i == 0) {
                        stroke.moveTo(x, y)
                        area.moveTo(x, size.height - pad)
                        area.lineTo(x, y)
                    } else {
                        stroke.lineTo(x, y)
                        area.lineTo(x, y)
                    }
                }
                area.lineTo(px(1f), size.height - pad)
                area.close()
                val stops = curve.normalized.mapIndexed { index, point ->
                    (index / (curve.normalized.size - 1f)) to lerp(calm, bright, point.valence)
                }.toTypedArray()
                drawPath(area, Brush.verticalGradient(listOf(bright.copy(alpha = 0.28f), calm.copy(alpha = 0.02f)), startY = pad, endY = size.height - pad))
                drawPath(stroke, Brush.horizontalGradient(*stops, startX = pad, endX = size.width - pad), style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round))
                steps.forEach { step ->
                    val x = px(step.position)
                    val target = py(step.target.energy)
                    val actual = py(step.mood.energy)
                    drawLine(scheme.onSurfaceVariant.copy(alpha = 0.25f), Offset(x, target), Offset(x, actual), 1.5.dp.toPx())
                    drawCircle(if (step.pinned) scheme.primary else scheme.onSurface.copy(alpha = 0.75f), 3.5.dp.toPx(), Offset(x, actual))
                }
            }
            Text(copy[SmartMusicText.MoodIntense], style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant, modifier = Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 4.dp))
            Text(copy[SmartMusicText.MoodCalm], style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant, modifier = Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 4.dp))
            val pointPx = with(density) { PointSize.toPx() }
            curve.points.forEachIndexed { index, point ->
                val clean = point.clamped()
                val count = curve.points.size
                val targetX = pad + (index / (count - 1f)) * (widthPx - pad * 2) - pointPx / 2f
                val targetY = pad + (1f - clean.energy) * (heightPx - pad * 2) - pointPx / 2f
                val y by animateFloatAsState(targetY, if (animated) spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh) else snap(), label = "mood-point-y")
                val active = index == selected
                val description = copy[SmartMusicText.MoodPointDescription]
                    .replace("{n}", (index + 1).toString())
                    .replace("{e}", (clean.energy * 100).roundToInt().toString())
                    .replace("{b}", (clean.valence * 100).roundToInt().toString())
                Box(
                    Modifier
                        .offset { IntOffset(targetX.roundToInt(), y.roundToInt()) }
                        .size(PointSize)
                        .clip(rememberSelectionMorph(active, animated, rotation = clean.valence * 40f))
                        .background(if (active) lerp(calm, bright, clean.valence) else scheme.surfaceContainerHighest)
                        .semantics {
                            contentDescription = description
                            this.selected = active
                            customActions = listOf(
                                CustomAccessibilityAction(copy[SmartMusicText.MoodRaise]) {
                                    onMove(index, MoodPoint((clean.energy + 0.1f).coerceAtMost(1f), clean.valence)); onCommit(); true
                                },
                                CustomAccessibilityAction(copy[SmartMusicText.MoodLower]) {
                                    onMove(index, MoodPoint((clean.energy - 0.1f).coerceAtLeast(0f), clean.valence)); onCommit(); true
                                }
                            )
                        }
                ) {
                    Box(Modifier.align(Alignment.Center).size(10.dp).clip(CircleShape).background(if (active) scheme.surface else lerp(calm, bright, clean.valence)))
                }
            }
        }
    }
}

private fun nearestPoint(curve: MoodCurve, x: Float, width: Float, pad: Float): Int {
    val count = curve.points.size
    if (count <= 1) return 0
    val t = ((x - pad) / (width - pad * 2)).coerceIn(0f, 1f)
    return (t * (count - 1)).roundToInt().coerceIn(0, count - 1)
}

private fun presetLabel(preset: MoodCurvePreset): SmartMusicText = when (preset) {
    MoodCurvePreset.Sunrise -> SmartMusicText.MoodPresetSunrise
    MoodCurvePreset.WindDown -> SmartMusicText.MoodPresetWindDown
    MoodCurvePreset.Summit -> SmartMusicText.MoodPresetSummit
    MoodCurvePreset.Wave -> SmartMusicText.MoodPresetWave
    MoodCurvePreset.Steady -> SmartMusicText.MoodPresetSteady
}

@Composable
private fun PresetRow(animated: Boolean, copy: SmartMusicCopy, onPreset: (MoodCurvePreset) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalLevyraHaptics.current
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MoodCurvePreset.entries.forEach { preset ->
            val curve = remember(preset) { MoodCurve.preset(preset) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 8.dp, bottomEnd = 20.dp, bottomStart = 8.dp))
                    .background(scheme.surfaceContainerHigh)
                    .clickable(role = Role.Button) {
                        haptics.perform(LevyraHapticAction.Confirm)
                        onPreset(preset)
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Canvas(Modifier.size(width = 34.dp, height = 20.dp)) {
                    val path = Path()
                    for (i in 0..24) {
                        val t = i / 24f
                        val x = t * size.width
                        val y = (1f - curve.sample(t).energy) * size.height
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, scheme.primary, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
                }
                Text(copy[presetLabel(preset)], style = MaterialTheme.typography.labelLarge, color = scheme.onSurface, maxLines = 1)
            }
        }
    }
}

@Composable
private fun BrightnessControl(session: MoodJourneySession, selected: Int, copy: SmartMusicCopy, controller: MoodJourneyController) {
    val scheme = MaterialTheme.colorScheme
    val point = session.curve.points.getOrNull(selected)?.clamped() ?: return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(copy[SmartMusicText.MoodBrightness], style = MaterialTheme.typography.labelLarge, color = scheme.onSurface, modifier = Modifier.weight(1f))
            Text("${selected + 1}/${session.curve.points.size}", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(copy[SmartMusicText.MoodDark], style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            Slider(
                value = point.valence,
                onValueChange = { controller.movePoint(selected, MoodPoint(point.energy, it)) },
                onValueChangeFinished = controller::commitCurve,
                modifier = Modifier.weight(1f)
            )
            Text(copy[SmartMusicText.MoodBright], style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LengthStepper(count: Int, copy: SmartMusicCopy, onCount: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalLevyraHaptics.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(copy[SmartMusicText.MoodSongs].replace("{n}", count.toString()), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = scheme.onSurface, modifier = Modifier.weight(1f))
        FilledTonalIconButton(
            onClick = { haptics.perform(LevyraHapticAction.SeekSnap); onCount(count - 2) },
            enabled = count > MoodJourneyRequest.MIN_TRACKS
        ) { Icon(Icons.Rounded.Remove, contentDescription = copy[SmartMusicText.MoodLower]) }
        Spacer(Modifier.width(8.dp))
        FilledTonalIconButton(
            onClick = { haptics.perform(LevyraHapticAction.SeekSnap); onCount(count + 2) },
            enabled = count < MoodJourneyRequest.MAX_TRACKS
        ) { Icon(Icons.Rounded.Add, contentDescription = copy[SmartMusicText.MoodRaise]) }
    }
}

@Composable
private fun JourneyStatus(session: MoodJourneySession, copy: SmartMusicCopy, animated: Boolean) {
    val scheme = MaterialTheme.colorScheme
    AnimatedContent(
        targetState = session.stage,
        transitionSpec = { if (animated) fadeIn() togetherWith fadeOut() else fadeIn(snap()) togetherWith fadeOut(snap()) },
        label = "mood-status"
    ) { stage ->
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            when (stage) {
                MoodJourneyStage.Loading -> Text(copy[SmartMusicText.MoodLoading], style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                MoodJourneyStage.Generating -> Text(copy[SmartMusicText.MoodGenerating], style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                MoodJourneyStage.Empty -> Text(copy[SmartMusicText.MoodEmpty], style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
                MoodJourneyStage.Error -> Text(copy[SmartMusicText.MoodError], style = MaterialTheme.typography.bodyMedium, color = scheme.error)
                MoodJourneyStage.Ready -> {
                    val metrics = session.result?.metrics
                    if (metrics != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricChip(copy[SmartMusicText.MoodFit].replace("{n}", (metrics.coherence * 100).roundToInt().toString()), scheme.primaryContainer, scheme.onPrimaryContainer)
                        }
                    }
                }
            }
            val evidence = if (session.editorialPlaylists > 0) copy[SmartMusicText.MoodEvidence].replace("{n}", session.editorialPlaylists.toString())
            else copy[SmartMusicText.MoodEvidenceNone]
            if (stage != MoodJourneyStage.Loading) Text(evidence, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            val shortfall = session.result?.shortfall ?: 0
            if (stage == MoodJourneyStage.Ready && shortfall > 0) {
                Text(copy[SmartMusicText.MoodShortfall].replace("{n}", session.result?.steps?.size?.toString().orEmpty()), style = MaterialTheme.typography.bodySmall, color = scheme.tertiary)
            }
            if (session.saveFailed) Text(copy[SmartMusicText.MoodSaveFailed], style = MaterialTheme.typography.bodySmall, color = scheme.error)
            if (session.savedPlaylistId != null) Text(copy[SmartMusicText.MoodSaved], style = MaterialTheme.typography.bodySmall, color = scheme.primary)
        }
    }
}

@Composable
private fun MetricChip(text: String, container: Color, content: Color) {
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(12.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

@Composable
private fun JourneyStepRow(
    index: Int,
    step: MoodJourneyStep,
    animated: Boolean,
    copy: SmartMusicCopy,
    onPin: () -> Unit,
    onExclude: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalLevyraHaptics.current
    val tone = lerp(scheme.secondary, scheme.tertiary, step.mood.valence)
    val shape = if (index % 2 == 0) {
        RoundedCornerShape(topStart = 26.dp, topEnd = 12.dp, bottomEnd = 26.dp, bottomStart = 12.dp)
    } else {
        RoundedCornerShape(topStart = 12.dp, topEnd = 26.dp, bottomEnd = 12.dp, bottomStart = 26.dp)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (step.pinned) scheme.primaryContainer.copy(alpha = 0.55f) else scheme.surfaceContainer)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(52.dp)) {
            AsyncImage(
                model = step.track.largeThumbnailUrl.ifBlank { step.track.thumbnailUrl },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(scheme.surfaceContainerHighest)
            )
            Box(
                Modifier.align(Alignment.BottomEnd).offset(x = 4.dp, y = 4.dp).size(20.dp).clip(CircleShape).background(tone),
                contentAlignment = Alignment.Center
            ) {
                Text((index + 1).toString(), style = MaterialTheme.typography.labelSmall, color = scheme.surface, fontWeight = FontWeight.Bold)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(step.track.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(step.track.artist, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EnergyGauge(step, animated)
                if (step.pinned) Badge(copy[SmartMusicText.MoodKept], scheme.primary)
                if (step.mood.confidence < 0.5f) Badge(copy[SmartMusicText.MoodEstimated], scheme.outline)
            }
        }
        IconButton(onClick = { haptics.perform(LevyraHapticAction.Favorite); onPin() }) {
            Box(
                Modifier.size(36.dp).clip(rememberPinMorph(step.pinned, animated)).background(if (step.pinned) scheme.primary else scheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.PushPin,
                    contentDescription = copy[if (step.pinned) SmartMusicText.MoodUnpin else SmartMusicText.MoodPin],
                    tint = if (step.pinned) scheme.onPrimary else scheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        IconButton(onClick = onExclude) {
            Icon(Icons.Rounded.Close, contentDescription = copy[SmartMusicText.MoodExclude], tint = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EnergyGauge(step: MoodJourneyStep, animated: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val actual by animateFloatAsState(step.mood.energy, if (animated) spring(Spring.DampingRatioMediumBouncy) else snap(), label = "mood-gauge")
    Canvas(Modifier.size(width = 56.dp, height = 8.dp)) {
        val radius = size.height / 2f
        drawRoundRect(scheme.surfaceContainerHighest, cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius))
        drawRoundRect(
            scheme.primary,
            size = androidx.compose.ui.geometry.Size(size.width * actual.coerceIn(0.05f, 1f), size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius)
        )
        val tx = size.width * step.target.energy
        drawLine(scheme.onSurface, Offset(tx, -2f), Offset(tx, size.height + 2f), 2.dp.toPx(), cap = StrokeCap.Round)
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 1.dp)
    )
}

@Composable
private fun JourneyActions(
    session: MoodJourneySession,
    copy: SmartMusicCopy,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
    onSave: () -> Unit,
    onRegenerate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val ready = session.stage == MoodJourneyStage.Ready && !session.result?.steps.isNullOrEmpty()
    Surface(
        color = scheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        shadowElevation = 6.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = onPlay, enabled = ready, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(copy[SmartMusicText.MoodPlay], maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            FilledTonalIconButton(onClick = onAddToQueue, enabled = ready, modifier = Modifier.size(52.dp)) {
                Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = copy[SmartMusicText.MoodQueue])
            }
            FilledTonalIconButton(onClick = onSave, enabled = ready && !session.saving, modifier = Modifier.size(52.dp)) {
                Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = copy[SmartMusicText.MoodSave])
            }
            FilledTonalIconButton(onClick = onRegenerate, enabled = ready, modifier = Modifier.size(52.dp)) {
                Icon(Icons.Rounded.Refresh, contentDescription = copy[SmartMusicText.MoodRegenerate])
            }
        }
    }
}

@Composable
private fun SaveJourneyDialog(
    defaultName: String,
    session: MoodJourneySession,
    copy: SmartMusicCopy,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(defaultName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(copy[SmartMusicText.MoodSave]) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it.take(80) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (session.saveFailed) Text(copy[SmartMusicText.MoodSaveFailed], color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank() && !session.saving) { Text(copy[SmartMusicText.MoodSave]) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(copy[SmartMusicText.MoodClose]) } }
    )
}

@Composable
internal fun MoodJourneyEntry(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val copy = smartMusicCopy(LocalLevyraStrings.current.code)
    val haptics = LocalLevyraHaptics.current
    val curve = remember { MoodCurve.preset(MoodCurvePreset.Summit) }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 30.dp, bottomEnd = 12.dp, bottomStart = 30.dp))
            .background(Brush.horizontalGradient(listOf(scheme.secondaryContainer, scheme.tertiaryContainer)))
            .clickable(role = Role.Button) {
                haptics.perform(LevyraHapticAction.Confirm)
                onOpen()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Canvas(Modifier.size(width = 64.dp, height = 40.dp)) {
            val path = Path()
            for (i in 0..32) {
                val t = i / 32f
                val x = t * size.width
                val y = (1f - curve.sample(t).energy) * size.height
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, scheme.onTertiaryContainer, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            drawCircle(scheme.onTertiaryContainer, 4.dp.toPx(), Offset(size.width / 2f, (1f - curve.sample(0.5f).energy) * size.height))
        }
        Column(Modifier.weight(1f)) {
            Text(copy[SmartMusicText.MoodTitle], style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = scheme.onSecondaryContainer)
            Text(copy[SmartMusicText.MoodEntry], style = MaterialTheme.typography.bodySmall, color = scheme.onSecondaryContainer.copy(alpha = 0.8f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
