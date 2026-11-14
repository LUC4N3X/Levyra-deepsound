package com.luc4n3x.levyra.ui.moodjourney

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
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
import com.luc4n3x.levyra.domain.MoodStyle
import com.luc4n3x.levyra.domain.MoodTone
import com.luc4n3x.levyra.ui.components.LevyraBackButton
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.MoodStyleCopy
import com.luc4n3x.levyra.ui.i18n.SmartMusicCopy
import com.luc4n3x.levyra.ui.i18n.SmartMusicText
import com.luc4n3x.levyra.ui.i18n.moodStyleCopy
import com.luc4n3x.levyra.ui.i18n.smartMusicCopy
import com.luc4n3x.levyra.ui.smartmusic.rememberPinMorph
import com.luc4n3x.levyra.ui.smartmusic.rememberSelectionMorph
import com.luc4n3x.levyra.ui.theme.AtelierDesign
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
import com.luc4n3x.levyra.ui.components.LevyraSlider

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
    onAddToQueue: () -> Unit,
    backEnabled: Boolean = true,
    onPlayFrom: (String) -> Unit = {}
) {
    BackHandler(enabled = backEnabled, onBack = onClose)
    val atelier = activeMoodArcSkin() == MoodArcSkin.Atelier
    val baseScheme = MaterialTheme.colorScheme
    val screenScheme = if (atelier) atelierMoodScheme(baseScheme, AtelierDesign.accent) else baseScheme
    MaterialTheme(colorScheme = screenScheme, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes) {
        MoodJourneyContent(session, controller, animated, onClose, onPlay, onAddToQueue, onPlayFrom, atelier)
    }
}

@Composable
private fun atelierMoodScheme(base: ColorScheme, accent: Color): ColorScheme = remember(base, accent) {
    base.copy(
        primary = accent,
        onPrimary = AtelierDesign.onAccent,
        primaryContainer = accent.copy(alpha = 0.2f).compositeOver(AtelierDesign.panel),
        onPrimaryContainer = AtelierDesign.accentBright,
        secondary = AtelierDesign.accentBright,
        secondaryContainer = AtelierDesign.panelRaised,
        onSecondaryContainer = AtelierDesign.text,
        tertiary = AtelierDesign.accentBright,
        tertiaryContainer = AtelierDesign.cabinet,
        onTertiaryContainer = AtelierDesign.text,
        background = AtelierDesign.room,
        surface = AtelierDesign.room,
        onSurface = AtelierDesign.text,
        onSurfaceVariant = AtelierDesign.textSecondary,
        surfaceContainerLowest = AtelierDesign.room,
        surfaceContainerLow = AtelierDesign.cabinet,
        surfaceContainer = AtelierDesign.panel,
        surfaceContainerHigh = AtelierDesign.panelRaised,
        surfaceContainerHighest = AtelierDesign.panelRaised.copy(alpha = 1f).compositeOver(AtelierDesign.hairlineStrong),
        outline = AtelierDesign.textFaint,
        outlineVariant = AtelierDesign.hairlineStrong
    )
}

@Composable
private fun MoodJourneyContent(
    session: MoodJourneySession,
    controller: MoodJourneyController,
    animated: Boolean,
    onClose: () -> Unit,
    onPlay: () -> Unit,
    onAddToQueue: () -> Unit,
    onPlayFrom: (String) -> Unit,
    atelier: Boolean
) {
    val strings = LocalLevyraStrings.current
    val copy = smartMusicCopy(strings.code)
    val styleCopy = moodStyleCopy(strings.code)
    val scheme = MaterialTheme.colorScheme
    val style = rememberMoodArcStyle()
    var selectedPoint by rememberSaveable { mutableIntStateOf(0) }
    var saveOpen by remember { mutableStateOf(false) }
    LaunchedEffect(session.savedPlaylistId) { if (session.savedPlaylistId != null) saveOpen = false }
    val mood = averageMood(session.curve)
    val wash = lerp(scheme.secondaryContainer, scheme.tertiaryContainer, mood.energy)
    val glow = scheme.primary
    Surface(modifier = Modifier.fillMaxSize(), color = scheme.surface) {
        Box(
            Modifier.fillMaxSize().then(
                if (atelier) {
                    Modifier.drawBehind {
                        val center = Offset(size.width * (0.2f + 0.6f * mood.energy), 0f)
                        drawRect(Brush.radialGradient(listOf(glow.copy(alpha = 0.14f + 0.1f * mood.energy), Color.Transparent), center = center, radius = size.width * 1.1f))
                    }
                } else {
                    Modifier.background(Brush.verticalGradient(0f to wash.copy(alpha = 0.55f), 0.45f to scheme.surface))
                }
            )
        ) {
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
                                style = if (atelier) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineSmall,
                                fontWeight = if (atelier) FontWeight.Light else FontWeight.Black,
                                color = scheme.onSurface,
                                modifier = Modifier.semantics { heading() }
                            )
                            Text(copy[SmartMusicText.MoodSubtitle], style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                    }
                    if (atelier) Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().height(1.dp).background(AtelierDesign.hairline))
                }
                item(key = "curve") {
                    CurveEditor(
                        session = session,
                        selected = selectedPoint.coerceIn(0, session.curve.points.lastIndex),
                        animated = animated,
                        copy = copy,
                        style = style,
                        onSelect = { selectedPoint = it },
                        onMove = controller::movePoint,
                        onCommit = controller::commitCurve,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                item(key = "controls") {
                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PresetRow(animated, copy, controller::applyPreset)
                        StyleSelector(session.styles, styleCopy, style, controller::selectStyle)
                        ToneControl(session, selectedPoint.coerceIn(0, session.curve.points.lastIndex), styleCopy, controller)
                        LengthStepper(session.trackCount, copy, controller::setTrackCount)
                        JourneyStatus(session, copy, styleCopy, animated)
                    }
                }
                val steps = session.result?.steps.orEmpty()
                val peak = session.result?.peakIndex ?: -1
                itemsIndexed(steps, key = { _, step -> step.key }) { index, step ->
                    JourneyStepRow(
                        index = index,
                        step = step,
                        peak = index == peak,
                        animated = animated,
                        copy = copy,
                        style = style,
                        onPin = { controller.togglePin(step.key) },
                        onExclude = { controller.exclude(step.key) },
                        onPlay = { onPlayFrom(step.key) },
                        cueSheet = atelier,
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
                atelier = atelier,
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
    style: MoodArcStyle,
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
    val styleCopy = moodStyleCopy(LocalLevyraStrings.current.code)
    var group by remember(curve.points.size) { mutableStateOf(emptySet<Int>()) }
    val latestGroup by rememberUpdatedState(group)
    val setGroup by rememberUpdatedState<(Set<Int>) -> Unit>({ group = it })
    val shift: (Int, Float) -> Unit = { index, delta ->
        val members = if (index in group) group else setOf(index)
        val shifted = curve.withEnergyShift(members, delta)
        members.forEach { onMove(it, shifted.points[it]) }
        onCommit()
    }
    val panel = scheme.surfaceContainerLow
    val steps = session.result?.steps.orEmpty()
    val peak = session.result?.peakIndex ?: -1
    val stepColors = remember(steps, style) { steps.map { style.toneColor(it.mood.energy, it.mood.valence) } }
    val reveal = rememberArcReveal(steps, animated)
    val atelierPanel = style.skin == MoodArcSkin.Atelier
    Surface(
        color = panel,
        shape = style.panelShape,
        border = if (atelierPanel) BorderStroke(1.dp, AtelierDesign.hairlineStrong) else null,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .height(CurveHeight)
                    .semantics {
                        contentDescription = copy[SmartMusicText.MoodCurveDescription]
                        if (group.isNotEmpty()) {
                            customActions = listOf(CustomAccessibilityAction(styleCopy.groupClear) { group = emptySet(); true })
                        }
                    }
                    .curveGestures(
                        curve = { curve },
                        group = { latestGroup },
                        padPx = with(density) { CurvePadding.toPx() },
                        onHaptic = { haptics.perform(it) },
                        onSelect = { select(it) },
                        onGroup = { setGroup(it) },
                        onMove = { index, point -> move(index, point) },
                        onCommit = { commit() }
                    )
            ) {
                val widthPx = with(density) { maxWidth.toPx() }
                val heightPx = with(density) { maxHeight.toPx() }
                val pad = with(density) { CurvePadding.toPx() }
                val outline = scheme.outlineVariant
                val muted = scheme.onSurfaceVariant
                val pinRing = scheme.primary
                Canvas(Modifier.fillMaxSize()) {
                    val frame = ArcFrame(pad, size.width - pad * 2, size.height - pad * 2)
                    for (band in 1..3) {
                        val gy = pad + frame.usableH * band / 4f
                        drawLine(outline.copy(alpha = 0.28f), Offset(pad, gy), Offset(size.width - pad, gy), 1.dp.toPx())
                    }
                    if (atelierPanel) drawAtelierRuler(frame, outline, size.width)
                    val target = targetPath(curve, frame::x, frame::y)
                    if (steps.isEmpty()) {
                        drawTargetOnly(curve, target, frame, style)
                    } else {
                        drawPath(
                            target,
                            muted.copy(alpha = 0.5f),
                            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 6.dp.toPx())))
                        )
                        drawSongArc(steps, stepColors, peak, frame, style, panel, pinRing, reveal.value)
                    }
                }
                Text(style.label(copy[SmartMusicText.MoodIntense]), style = MaterialTheme.typography.labelSmall, fontFamily = style.numerals, color = muted, modifier = Modifier.align(Alignment.TopStart).padding(start = 14.dp, top = 4.dp))
                Text(style.label(copy[SmartMusicText.MoodCalm]), style = MaterialTheme.typography.labelSmall, fontFamily = style.numerals, color = muted, modifier = Modifier.align(Alignment.BottomStart).padding(start = 14.dp, bottom = 4.dp))
                steps.getOrNull(peak)?.let { peakStep ->
                    PeakLabel(peakStep, ArcFrame(pad, widthPx - pad * 2, heightPx - pad * 2), widthPx, style, copy, reveal)
                }
                curve.points.forEachIndexed { index, point ->
                    CurveHandle(
                        index = index,
                        point = point.clamped(),
                        count = curve.points.size,
                        frame = ArcFrame(pad, widthPx - pad * 2, heightPx - pad * 2),
                        active = index == selected,
                        grouped = index in group,
                        dimmed = steps.isNotEmpty(),
                        animated = animated,
                        copy = copy,
                        styleCopy = styleCopy,
                        style = style,
                        onShift = shift,
                        onToggleGroup = { group = if (index in group) group - index else group + index }
                    )
                }
            }
            if (steps.isNotEmpty()) ArcLegend(copy, style, stepColors)
        }
    }
}

private fun DrawScope.drawAtelierRuler(frame: ArcFrame, color: Color, width: Float) {
    val minor = 4.dp.toPx()
    val major = 9.dp.toPx()
    val stroke = 1.dp.toPx()
    for (tick in 0..20) {
        val y = frame.pad + frame.usableH * tick / 20f
        val length = if (tick % 5 == 0) major else minor
        drawLine(color.copy(alpha = if (tick % 5 == 0) 0.7f else 0.35f), Offset(width - length, y), Offset(width, y), stroke)
    }
    for (tick in 0..24) {
        val x = frame.pad + frame.usableW * tick / 24f
        val length = if (tick % 6 == 0) major else minor
        drawLine(color.copy(alpha = if (tick % 6 == 0) 0.7f else 0.35f), Offset(x, size.height - length), Offset(x, size.height), stroke)
    }
}

private class ArcFrame(val pad: Float, val usableW: Float, val usableH: Float) {
    val floor: Float get() = pad + usableH
    fun x(t: Float): Float = pad + t * usableW
    fun y(energy: Float): Float = pad + (1f - energy) * usableH
}

@Composable
private fun rememberArcReveal(steps: List<MoodJourneyStep>, animated: Boolean): Animatable<Float, *> {
    val sequence = remember(steps) { steps.map { it.key }.hashCode() }
    var revealed by rememberSaveable { mutableIntStateOf(0) }
    val reveal = remember { Animatable(if (animated && steps.isNotEmpty() && sequence != revealed) 0f else 1f) }
    LaunchedEffect(sequence, animated) {
        if (animated && steps.isNotEmpty() && sequence != revealed) {
            reveal.snapTo(0f)
            reveal.animateTo(1f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
        } else {
            reveal.snapTo(1f)
        }
        revealed = sequence
    }
    return reveal
}

private fun Modifier.curveGestures(
    curve: () -> MoodCurve,
    group: () -> Set<Int>,
    padPx: Float,
    onHaptic: (LevyraHapticAction) -> Unit,
    onSelect: (Int) -> Unit,
    onGroup: (Set<Int>) -> Unit,
    onMove: (Int, MoodPoint) -> Unit,
    onCommit: () -> Unit
): Modifier = this
    .pointerInput(Unit) {
        detectTapGestures(
            onLongPress = { offset ->
                val index = nearestPoint(curve(), offset.x, size.width.toFloat(), padPx)
                val current = group()
                onHaptic(LevyraHapticAction.Reorder)
                onGroup(if (index in current) current - index else current + index)
                onSelect(index)
            }
        ) { offset ->
            onHaptic(LevyraHapticAction.Confirm)
            if (group().isNotEmpty()) onGroup(emptySet())
            onSelect(nearestPoint(curve(), offset.x, size.width.toFloat(), padPx))
        }
    }
    .pointerInput(Unit) {
        var active = 0
        var lastBand = 0
        var start = curve()
        var members = emptySet<Int>()
        detectDragGestures(
            onDragStart = { offset ->
                active = nearestPoint(curve(), offset.x, size.width.toFloat(), padPx)
                start = curve()
                members = group().takeIf { active in it }.orEmpty()
                lastBand = (curve().points[active].energy * 4).toInt()
                onHaptic(LevyraHapticAction.Confirm)
                onSelect(active)
            },
            onDragEnd = onCommit,
            onDragCancel = onCommit
        ) { change, _ ->
            change.consume()
            val usable = (size.height - padPx * 2).coerceAtLeast(1f)
            val energy = (1f - (change.position.y - padPx) / usable).coerceIn(0f, 1f)
            val band = (energy * 4).toInt()
            if (band != lastBand) {
                lastBand = band
                onHaptic(LevyraHapticAction.SeekSnap)
            }
            if (members.isNotEmpty()) {
                val anchor = start.points.getOrNull(active)?.clamped() ?: return@detectDragGestures
                val shifted = start.withEnergyShift(members, energy - anchor.energy)
                members.forEach { onMove(it, shifted.points[it]) }
                return@detectDragGestures
            }
            val current = curve().points.getOrNull(active) ?: return@detectDragGestures
            onMove(active, MoodPoint(energy, current.valence))
        }
    }

private fun DrawScope.drawTargetOnly(curve: MoodCurve, target: Path, frame: ArcFrame, style: MoodArcStyle) {
    val area = Path().apply {
        addPath(target)
        lineTo(frame.x(1f), frame.floor)
        lineTo(frame.x(0f), frame.floor)
        close()
    }
    val stops = curve.normalized.mapIndexed { index, point ->
        index / (curve.normalized.size - 1f) to style.toneColor(point.energy, point.valence)
    }.toTypedArray()
    drawPath(area, Brush.verticalGradient(listOf(style.toneColor(1f, 0.5f).copy(alpha = 0.24f), Color.Transparent), startY = frame.pad, endY = frame.floor))
    drawPath(target, Brush.horizontalGradient(*stops, startX = frame.pad, endX = frame.x(1f)), style = Stroke(width = style.stroke.toPx(), cap = StrokeCap.Round))
}

private fun DrawScope.drawSongArc(
    steps: List<MoodJourneyStep>,
    colors: List<Color>,
    peak: Int,
    frame: ArcFrame,
    style: MoodArcStyle,
    panel: Color,
    pinRing: Color,
    reveal: Float
) {
    val points = steps.map { Offset(frame.x(it.position), frame.y(it.mood.energy)) }
    val line = smoothThrough(points)
    val area = Path().apply {
        addPath(line)
        lineTo(points.last().x, frame.floor)
        lineTo(points.first().x, frame.floor)
        close()
    }
    val span = (points.last().x - points.first().x).coerceAtLeast(1f)
    val brush = if (points.size < 2) SolidColor(colors.first()) else Brush.horizontalGradient(
        *points.indices.map { ((points[it].x - points.first().x) / span).coerceIn(0f, 1f) to colors[it] }.toTypedArray(),
        startX = points.first().x,
        endX = points.last().x
    )
    val peakColor = colors.getOrElse(peak) { colors.first() }
    val dot = style.dot.toPx() * if (steps.size > 14) 0.8f else 1f
    clipRect(right = frame.pad + frame.usableW * reveal + dot * 3f) {
        drawPath(area, Brush.verticalGradient(listOf(peakColor.copy(alpha = 0.26f), peakColor.copy(alpha = 0f)), startY = frame.pad, endY = frame.floor))
        points.forEachIndexed { index, point ->
            drawLine(colors[index].copy(alpha = 0.18f), Offset(point.x, point.y + dot * 2f), Offset(point.x, frame.floor), 1.dp.toPx())
        }
        drawPath(line, brush, style = Stroke(width = style.stroke.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        points.forEachIndexed { index, point ->
            drawStepMark(steps[index], colors[index], point, dot, index == peak, style.squareDots, panel, pinRing)
        }
    }
}

private fun DrawScope.drawStepMark(step: MoodJourneyStep, color: Color, point: Offset, dot: Float, peak: Boolean, square: Boolean, panel: Color, pinRing: Color) {
    if (peak) drawMark(square, color.copy(alpha = 0.45f), point, dot * 2.6f, Stroke(1.5.dp.toPx()))
    drawMark(square, panel, point, dot + 2.dp.toPx(), Fill)
    if (step.mood.confidence < 0.5f) {
        drawMark(square, color, point, dot - 0.75.dp.toPx(), Stroke(1.5.dp.toPx()))
    } else {
        drawMark(square, color, point, dot, Fill)
    }
    if (step.pinned) drawMark(square, pinRing, point, dot + 3.5.dp.toPx(), Stroke(1.5.dp.toPx()))
}

@Composable
private fun PeakLabel(peakStep: MoodJourneyStep, frame: ArcFrame, widthPx: Float, style: MoodArcStyle, copy: SmartMusicCopy, reveal: Animatable<Float, *>) {
    val density = LocalDensity.current
    val anchorX = frame.x(peakStep.position)
    val anchorY = frame.y(peakStep.mood.energy) - with(density) { (style.dot * 2.6f + 4.dp).toPx() }
    val fadeSpan = (1f - peakStep.position).coerceIn(0.01f, 0.25f)
    Text(
        style.label(copy[SmartMusicText.ArcPeak]),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        fontFamily = style.numerals,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .graphicsLayer { alpha = ((reveal.value - peakStep.position) / fadeSpan).coerceIn(0f, 1f) }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                layout(placeable.width, placeable.height) {
                    val x = (anchorX - placeable.width / 2f).coerceIn(0f, (widthPx - placeable.width).coerceAtLeast(0f))
                    val y = (anchorY - placeable.height).coerceAtLeast(0f)
                    placeable.place(x.roundToInt(), y.roundToInt())
                }
            }
    )
}

@Composable
private fun CurveHandle(
    index: Int,
    point: MoodPoint,
    count: Int,
    frame: ArcFrame,
    active: Boolean,
    grouped: Boolean,
    dimmed: Boolean,
    animated: Boolean,
    copy: SmartMusicCopy,
    styleCopy: MoodStyleCopy,
    style: MoodArcStyle,
    onShift: (Int, Float) -> Unit,
    onToggleGroup: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val pointPx = with(LocalDensity.current) { PointSize.toPx() }
    val targetX = frame.x(index / (count - 1f)) - pointPx / 2f
    val targetY = frame.y(point.energy) - pointPx / 2f
    val y by animateFloatAsState(targetY, if (animated) spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh) else snap(), label = "mood-point-y")
    val tone = style.toneColor(point.energy, point.valence)
    val description = styleCopy.pointDescription
        .replace("{n}", (index + 1).toString())
        .replace("{e}", (point.energy * 100).roundToInt().toString())
        .replace("{b}", (point.valence * 100).roundToInt().toString())
    Box(
        Modifier
            .offset { IntOffset(targetX.roundToInt(), y.roundToInt()) }
            .size(PointSize)
            .clip(rememberSelectionMorph(active || grouped, animated, rotation = point.valence * 40f))
            .background(if (active || grouped) tone else scheme.surfaceContainerHighest.copy(alpha = if (dimmed) 0.72f else 1f))
            .semantics {
                contentDescription = description
                this.selected = active || grouped
                customActions = listOf(
                    CustomAccessibilityAction(copy[SmartMusicText.MoodRaise]) { onShift(index, 0.1f); true },
                    CustomAccessibilityAction(copy[SmartMusicText.MoodLower]) { onShift(index, -0.1f); true },
                    CustomAccessibilityAction(styleCopy.groupToggle) { onToggleGroup(); true }
                )
            }
    ) {
        val dot = when {
            grouped -> scheme.primary
            active -> scheme.surface
            else -> tone
        }
        Box(Modifier.align(Alignment.Center).size(10.dp).clip(CircleShape).background(dot))
    }
}

private fun targetPath(curve: MoodCurve, px: (Float) -> Float, py: (Float) -> Float): Path {
    val path = Path()
    val samples = 72
    for (i in 0..samples) {
        val t = i / samples.toFloat()
        val x = px(t)
        val y = py(curve.sample(t).energy)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    return path
}

private fun smoothThrough(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    for (i in 1 until points.size) {
        val from = points[i - 1]
        val to = points[i]
        val midX = (from.x + to.x) / 2f
        path.cubicTo(midX, from.y, midX, to.y, to.x, to.y)
    }
    return path
}

private fun DrawScope.drawMark(square: Boolean, color: Color, center: Offset, radius: Float, style: DrawStyle) {
    if (square) {
        drawRect(color, topLeft = Offset(center.x - radius, center.y - radius), size = Size(radius * 2f, radius * 2f), style = style)
    } else {
        drawCircle(color, radius, center, style = style)
    }
}

@Composable
private fun ArcLegend(copy: SmartMusicCopy, style: MoodArcStyle, stepColors: List<Color>) {
    val scheme = MaterialTheme.colorScheme
    val muted = scheme.onSurfaceVariant
    val accent = stepColors.maxByOrNull { it.luminance() } ?: scheme.primary
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 14.dp)
    ) {
        LegendItem(copy[SmartMusicText.ArcTarget], style) {
            drawLine(muted, Offset(0f, center.y), Offset(size.width, center.y), 1.5.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())))
        }
        LegendItem(copy[SmartMusicText.ArcSongs], style) {
            val brush = if (stepColors.size < 2) SolidColor(accent) else Brush.horizontalGradient(stepColors)
            drawLine(brush, Offset(0f, center.y), Offset(size.width, center.y), 3.dp.toPx(), cap = StrokeCap.Round)
            drawMark(style.squareDots, accent, center, 3.5.dp.toPx(), Fill)
        }
        LegendItem(copy[SmartMusicText.ArcPeak], style) {
            drawMark(style.squareDots, accent.copy(alpha = 0.45f), center, 6.dp.toPx(), Stroke(1.5.dp.toPx()))
            drawMark(style.squareDots, accent, center, 3.dp.toPx(), Fill)
        }
    }
}

@Composable
private fun LegendItem(text: String, style: MoodArcStyle, glyph: DrawScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.size(width = 22.dp, height = 14.dp), onDraw = glyph)
        Text(style.label(text), style = MaterialTheme.typography.labelMedium, fontFamily = style.numerals, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

private fun nearestPoint(curve: MoodCurve, x: Float, width: Float, pad: Float): Int {
    val count = curve.points.size
    if (count <= 1) return 0
    val t = ((x - pad) / (width - pad * 2)).coerceIn(0f, 1f)
    return (t * (count - 1)).roundToInt().coerceIn(0, count - 1)
}

internal fun presetLabel(preset: MoodCurvePreset): SmartMusicText = when (preset) {
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
private fun StyleSelector(selected: Set<MoodStyle>, copy: MoodStyleCopy, style: MoodArcStyle, onSelect: (MoodStyle?) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalLevyraHaptics.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(style.label(copy.title), style = MaterialTheme.typography.labelLarge, color = scheme.onSurface, modifier = Modifier.semantics { heading() })
        Text(copy.hint, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StyleChip(copy.mixed, selected.isEmpty(), style) {
                haptics.perform(LevyraHapticAction.Confirm)
                onSelect(null)
            }
            MoodStyle.entries.forEach { entry ->
                StyleChip(copy.name(entry), entry in selected, style) {
                    haptics.perform(LevyraHapticAction.Confirm)
                    onSelect(entry)
                }
            }
        }
    }
}

@Composable
private fun StyleChip(label: String, selected: Boolean, style: MoodArcStyle, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = if (selected) style.rowShapes.second else style.rowShapes.first
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(if (selected) scheme.secondaryContainer else scheme.surfaceContainerHigh)
            .border(1.dp, if (selected) scheme.secondary.copy(alpha = 0.6f) else Color.Transparent, shape)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = scheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) scheme.onSecondaryContainer else scheme.onSurface, maxLines = 1)
    }
}

@Composable
private fun ToneControl(session: MoodJourneySession, selected: Int, copy: MoodStyleCopy, controller: MoodJourneyController) {
    val scheme = MaterialTheme.colorScheme
    val point = session.curve.points.getOrNull(selected)?.clamped() ?: return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(copy.tone, style = MaterialTheme.typography.labelLarge, color = scheme.onSurface, modifier = Modifier.weight(1f))
            Text("${selected + 1}/${session.curve.points.size}", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(copy.toneLow, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            LevyraSlider(
                value = point.valence,
                onValueChange = { controller.movePoint(selected, MoodPoint(point.energy, it)) },
                onValueChangeFinished = controller::commitCurve,
                modifier = Modifier.weight(1f).semantics { contentDescription = copy.tone }
            )
            Text(copy.toneHigh, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        }
        Text(copy.toneHint, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
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
private fun JourneyStatus(session: MoodJourneySession, copy: SmartMusicCopy, styleCopy: MoodStyleCopy, animated: Boolean) {
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
                MoodJourneyStage.Empty -> Text(
                    if (session.styles.isEmpty()) copy[SmartMusicText.MoodEmpty] else styleCopy.empty,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface
                )
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
            val playlists = session.editorialPlaylists + session.stylePlaylists
            val evidence = if (playlists > 0) copy[SmartMusicText.MoodEvidence].replace("{n}", playlists.toString())
            else copy[SmartMusicText.MoodEvidenceNone]
            if (stage != MoodJourneyStage.Loading) Text(evidence, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            if (session.stylesLoading) Text(styleCopy.loading, style = MaterialTheme.typography.bodySmall, color = scheme.primary)
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
    peak: Boolean,
    animated: Boolean,
    copy: SmartMusicCopy,
    style: MoodArcStyle,
    onPin: () -> Unit,
    onExclude: () -> Unit,
    onPlay: () -> Unit,
    cueSheet: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalLevyraHaptics.current
    val tone = style.toneColor(step.mood.energy, step.mood.valence)
    val shape = if (index % 2 == 0) style.rowShapes.first else style.rowShapes.second
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (cueSheet) {
                    Modifier.drawBehind {
                        if (step.pinned) drawRect(scheme.primary, size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height))
                        drawLine(AtelierDesign.hairline, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1.dp.toPx())
                    }
                } else {
                    Modifier.background(if (step.pinned) scheme.primaryContainer.copy(alpha = 0.55f) else scheme.surfaceContainer)
                }
            )
            .clickable(role = Role.Button, onClickLabel = step.track.title) {
                haptics.perform(LevyraHapticAction.Confirm)
                onPlay()
            }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (cueSheet) {
            Text(
                (index + 1).toString().padStart(2, '0'),
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                color = if (step.pinned) scheme.primary else scheme.onSurfaceVariant,
                softWrap = false, modifier = Modifier.widthIn(min = 28.dp)
            )
        }
        StepCover(index, step, tone, style, showIndex = !cueSheet)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(step.track.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(step.track.artist, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            StepTags(step, peak, animated, copy, style, tone)
        }
        PinButton(step.pinned, animated, copy) { haptics.perform(LevyraHapticAction.Favorite); onPin() }
        IconButton(onClick = onExclude) {
            Icon(Icons.Rounded.Close, contentDescription = copy[SmartMusicText.MoodExclude], tint = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StepCover(index: Int, step: MoodJourneyStep, tone: Color, style: MoodArcStyle, showIndex: Boolean = true) {
    Box(Modifier.size(52.dp)) {
        AsyncImage(
            model = step.track.largeThumbnailUrl.ifBlank { step.track.thumbnailUrl },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(52.dp).clip(style.coverShape).background(MaterialTheme.colorScheme.surfaceContainerHighest)
        )
        if (showIndex) Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 4.dp, y = 4.dp)
                .size(20.dp)
                .clip(if (style.squareDots) RoundedCornerShape(4.dp) else CircleShape)
                .background(tone),
            contentAlignment = Alignment.Center
        ) {
            Text((index + 1).toString(), style = MaterialTheme.typography.labelSmall, fontFamily = style.numerals, color = if (tone.luminance() > 0.45f) Color.Black else Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StepTags(step: MoodJourneyStep, peak: Boolean, animated: Boolean, copy: SmartMusicCopy, style: MoodArcStyle, tone: Color) {
    val scheme = MaterialTheme.colorScheme
    val estimated = step.mood.confidence < 0.5f
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        ToneTag(copy[moodToneLabel(MoodTone.of(step.mood.energy, step.mood.valence))], tone, style, estimated)
        EnergyGauge(step, animated)
        if (peak) Badge(style.label(copy[SmartMusicText.ArcPeak]), scheme.onSurface)
        if (step.pinned) Badge(copy[SmartMusicText.MoodKept], scheme.primary)
        if (estimated) Badge(copy[SmartMusicText.MoodEstimated], scheme.outline)
    }
}

@Composable
private fun PinButton(pinned: Boolean, animated: Boolean, copy: SmartMusicCopy, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    IconButton(onClick = onClick) {
        Box(
            Modifier.size(36.dp).clip(rememberPinMorph(pinned, animated)).background(if (pinned) scheme.primary else scheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.PushPin,
                contentDescription = copy[if (pinned) SmartMusicText.MoodUnpin else SmartMusicText.MoodPin],
                tint = if (pinned) scheme.onPrimary else scheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ToneTag(text: String, tone: Color, style: MoodArcStyle, estimated: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Canvas(Modifier.size(8.dp)) {
            val radius = size.minDimension / 2f
            if (estimated) drawMark(style.squareDots, tone, center, radius - 0.75.dp.toPx(), Stroke(1.5.dp.toPx()))
            else drawMark(style.squareDots, tone, center, radius, Fill)
        }
        Text(style.label(text), style = MaterialTheme.typography.labelMedium, fontFamily = style.numerals, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
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
    atelier: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val ready = session.stage == MoodJourneyStage.Ready && !session.result?.steps.isNullOrEmpty()
    Surface(
        color = if (atelier) AtelierDesign.cabinet else scheme.surfaceContainerHigh,
        shape = if (atelier) RectangleShape else RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        shadowElevation = if (atelier) 0.dp else 6.dp,
        border = if (atelier) BorderStroke(1.dp, AtelierDesign.hairlineStrong) else null,
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
