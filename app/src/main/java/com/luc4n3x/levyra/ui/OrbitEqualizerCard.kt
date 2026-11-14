package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.rounded.FilterCenterFocus
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.OrbitEqualizer
import com.luc4n3x.levyra.domain.OrbitLayout
import com.luc4n3x.levyra.domain.OrbitPosition
import com.luc4n3x.levyra.domain.ParametricEqProfile
import com.luc4n3x.levyra.domain.ParametricProfiles
import com.luc4n3x.levyra.ui.i18n.OrbitEqCopy
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

private const val CENTER_SNAP = 0.06f
private const val POLE_REACH = 0.75f
private const val NUDGE = 0.1f
private const val LABEL_GAP_DEGREES = 24f
private val GRAB_RADIUS = 40.dp
private val FIELD_INSET = 36.dp

@Composable
internal fun OrbitEqualizerCard(
    enabled: Boolean,
    activeProfile: ParametricEqProfile?,
    layout: OrbitLayout?,
    customProfiles: List<ParametricEqProfile>,
    copy: OrbitEqCopy,
    savedTitle: String,
    actions: ParametricProfileActions,
    onSave: (String, ParametricEqProfile) -> Boolean
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val latestApply by rememberUpdatedState(actions.onApplyOrbit)
    val committed = remember(activeProfile, layout) { OrbitEqualizer.restore(activeProfile, layout) ?: OrbitLayout() }
    val orbit = remember { OrbitEqualizerState(committed) }
    fun commit(position: OrbitPosition) {
        val angles = orbit.poleAngles
        latestApply(OrbitEqualizer.shape(position, copy.title, angles).profile, OrbitLayout(angles, position))
    }
    fun glideTo(position: OrbitPosition) {
        scope.launch { commit(orbit.glideTo(position)) }
    }
    OrbitAuditionEffects(orbit, committed, copy.title, actions.onAudition)

    val dragging = orbit.dragging
    val comparing = orbit.comparing
    val live = orbit.positionNow()
    val poleAngles = orbit.poleAngles
    val shape = remember(live, poleAngles) { OrbitEqualizer.shape(live, copy.title, poleAngles) }
    val accent = lerp(LevyraPink, LevyraCyan, ((live.tilt + 1f) / 2f).coerceIn(0f, 1f))

    Surface(
        color = AudioCardSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, LevyraAdaptiveHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(copy.title, color = LevyraText, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text(copy.subtitle, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { checked ->
                        if (checked) commit(orbit.positionNow()) else actions.onEnabled(false)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = LevyraBlack,
                        checkedTrackColor = LevyraCyan,
                        uncheckedThumbColor = LevyraMuted,
                        uncheckedTrackColor = LevyraAdaptiveTrack
                    )
                )
            }

            OrbitField(
                position = live,
                poleAngles = poleAngles,
                accent = accent,
                active = enabled || dragging,
                dragging = dragging,
                copy = copy,
                onPole = { glideTo(it) },
                onDragStart = {
                    orbit.beginDrag()
                    scope.launch { orbit.stopGlide() }
                },
                onDrag = { target ->
                    if (orbit.dragTo(target)) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                onDragEnd = {
                    val final = orbit.positionNow()
                    commit(final)
                    scope.launch { orbit.endDrag(final) }
                },
                onNudge = { dx, dy -> glideTo(OrbitPosition(live.tilt + dx, live.contour + dy)) },
                onPoleDragStart = { pole ->
                    orbit.beginPoleDrag(pole)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                onPoleDrag = orbit::movePole,
                onPoleDragEnd = {
                    orbit.endPoleDrag()
                    commit(orbit.positionNow())
                },
                onResetPoles = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    orbit.resetPoles()
                    commit(orbit.positionNow())
                }
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = copy.headroom,
                    color = LevyraMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = String.format(Locale.ROOT, "%.1f dB", shape.profile.preampDb).replace("-", "−"),
                    color = LevyraText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
            ParametricResponseCurve(
                profile = shape.profile,
                description = copy.padDescription,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .alpha(if (enabled || dragging) 1f else 0.45f)
            )

            OrbitActions(
                copy = copy,
                shaped = live != OrbitPosition.Center,
                enabled = enabled,
                comparing = comparing,
                onCenter = { glideTo(OrbitPosition.Center) },
                onComparing = { orbit.comparing = it },
                onSave = {
                    val name = ParametricProfiles.availableName(copy.title, copy.title, customProfiles) { stem, number -> "$stem $number" }
                    val saved = OrbitLayout(poleAngles, live).takeIf { poleAngles != OrbitEqualizer.DEFAULT_POLE_ANGLES }
                    onSave(name, shape.profile.copy(orbitLayout = saved))
                }
            )

            OrbitSavedProfiles(
                title = savedTitle,
                customProfiles = customProfiles,
                activeId = activeProfile?.id.takeIf { enabled },
                onActivate = actions.onActivateCustom
            )
        }
    }
}

@Stable
private class OrbitEqualizerState(initial: OrbitLayout) {
    private val satellite = Animatable(Offset(initial.puck.tilt, initial.puck.contour), Offset.VectorConverter)
    private var dragTarget by mutableStateOf<OrbitPosition?>(null)
    private var draggingPole by mutableStateOf(false)
    var poleAngles by mutableStateOf(initial.poleAngles)
        private set
    var dragging by mutableStateOf(false)
        private set
    var comparing by mutableStateOf(false)
    var auditioning = false

    val moving: Boolean get() = dragging || draggingPole || satellite.isRunning

    fun positionNow(): OrbitPosition =
        (dragTarget ?: OrbitPosition(satellite.value.x, satellite.value.y)).clamped().quantized()

    fun layoutNow(): OrbitLayout = OrbitLayout(poleAngles, positionNow())

    fun beginPoleDrag(pole: Int) {
        if (pole in poleAngles.indices) draggingPole = true
    }

    fun movePole(pole: Int, degrees: Float) {
        if (pole !in poleAngles.indices) return
        poleAngles = poleAngles.toMutableList().also { it[pole] = OrbitEqualizer.normalizedAngle(degrees.roundToInt().toFloat()) }
    }

    fun endPoleDrag() {
        draggingPole = false
    }

    fun resetPoles() {
        poleAngles = OrbitEqualizer.DEFAULT_POLE_ANGLES
    }

    suspend fun glideTo(position: OrbitPosition): OrbitPosition {
        val target = position.clamped()
        satellite.animateTo(Offset(target.tilt, target.contour), spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow))
        return target.quantized()
    }

    suspend fun settle(layout: OrbitLayout) {
        if (moving) return
        poleAngles = layout.poleAngles
        satellite.snapTo(Offset(layout.puck.tilt, layout.puck.contour))
    }

    fun beginDrag() {
        dragging = true
    }

    suspend fun stopGlide() {
        satellite.stop()
    }

    fun dragTo(target: OrbitPosition): Boolean {
        val previous = dragTarget ?: OrbitPosition(satellite.value.x, satellite.value.y)
        val snapped = if (target.radius < CENTER_SNAP) OrbitPosition.Center else target
        dragTarget = snapped
        return crossesHapticBoundary(previous, snapped)
    }

    suspend fun endDrag(final: OrbitPosition) {
        satellite.snapTo(Offset(final.tilt, final.contour))
        dragTarget = null
        dragging = false
    }
}

@Composable
private fun OrbitAuditionEffects(
    orbit: OrbitEqualizerState,
    committed: OrbitLayout,
    title: String,
    onAudition: (ParametricEqProfile?) -> Unit
) {
    val latestAudition by rememberUpdatedState(onAudition)
    LaunchedEffect(committed) { orbit.settle(committed) }
    LaunchedEffect(orbit) {
        snapshotFlow { if (orbit.moving) orbit.layoutNow() else null }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { layout ->
                orbit.auditioning = true
                latestAudition(OrbitEqualizer.shape(layout.puck, title, layout.poleAngles).profile)
            }
    }
    DisposableEffect(orbit) {
        onDispose { if (orbit.auditioning) latestAudition(null) }
    }
    LaunchedEffect(orbit.comparing) {
        if (orbit.comparing) {
            latestAudition(OrbitEqualizer.comparisonProfile(OrbitEqualizer.shape(orbit.positionNow(), title, orbit.poleAngles)))
            orbit.auditioning = true
        } else if (orbit.auditioning) {
            latestAudition(null)
            orbit.auditioning = false
        }
    }
}

@Composable
private fun OrbitActions(
    copy: OrbitEqCopy,
    shaped: Boolean,
    enabled: Boolean,
    comparing: Boolean,
    onCenter: () -> Unit,
    onComparing: (Boolean) -> Unit,
    onSave: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ParametricAction(
                label = copy.center,
                icon = Icons.Rounded.FilterCenterFocus,
                enabled = shaped,
                modifier = Modifier.weight(1f),
                onClick = onCenter
            )
            OrbitCompareAction(
                label = copy.compare,
                enabled = enabled && shaped,
                comparing = comparing,
                modifier = Modifier.weight(1f),
                onComparing = onComparing
            )
        }
        ParametricAction(
            label = copy.save,
            icon = Icons.Rounded.BookmarkAdd,
            enabled = shaped,
            modifier = Modifier.fillMaxWidth(),
            onClick = onSave
        )
    }
}

private fun crossesHapticBoundary(previous: OrbitPosition, next: OrbitPosition): Boolean {
    val enteredCenter = previous.radius >= CENTER_SNAP != next.radius >= CENTER_SNAP
    val reachedRim = previous.radius < 0.999f && next.radius >= 0.999f
    return enteredCenter || reachedRim
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrbitSavedProfiles(
    title: String,
    customProfiles: List<ParametricEqProfile>,
    activeId: String?,
    onActivate: (String) -> Unit
) {
    val saved = remember(customProfiles) { customProfiles.filter { OrbitEqualizer.restore(it, it.orbitLayout) != null } }
    if (saved.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = LevyraMuted, fontSize = 11.sp, fontWeight = FontWeight.Black)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            saved.forEach { profile ->
                OrbitSavedChip(
                    label = profile.name,
                    selected = activeId == profile.id,
                    onClick = { onActivate(profile.id) }
                )
            }
        }
    }
}

@Composable
private fun OrbitField(
    position: OrbitPosition,
    poleAngles: List<Float>,
    accent: Color,
    active: Boolean,
    dragging: Boolean,
    copy: OrbitEqCopy,
    onPole: (OrbitPosition) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (OrbitPosition) -> Unit,
    onDragEnd: () -> Unit,
    onNudge: (Float, Float) -> Unit,
    onPoleDragStart: (Int) -> Unit,
    onPoleDrag: (Int, Float) -> Unit,
    onPoleDragEnd: () -> Unit,
    onResetPoles: () -> Unit
) {
    val halo by animateFloatAsState(if (dragging) 1.35f else 1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium), label = "orbit-halo")
    val hairline = LevyraAdaptiveHairline
    val muted = LevyraMuted
    val surface = AudioCardSurface
    val latestStart by rememberUpdatedState(onDragStart)
    val latestDrag by rememberUpdatedState(onDrag)
    val latestEnd by rememberUpdatedState(onDragEnd)
    val latestTap by rememberUpdatedState(onPole)
    val latestPosition by rememberUpdatedState(position)
    val latestAngles by rememberUpdatedState(poleAngles)
    val labels = listOf(copy.warm, copy.bright, copy.forward, copy.open)
    val state = poleState(position, poleAngles, labels)
    fun poleTarget(angle: Float): OrbitPosition {
        val direction = OrbitEqualizer.poleDirection(angle)
        return OrbitPosition(direction.tilt * POLE_REACH, direction.contour * POLE_REACH)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = 380.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .orbitPadGestures(
                position = { latestPosition },
                onTap = { latestTap(it) },
                onStart = { latestStart() },
                onDrag = { latestDrag(it) },
                onEnd = { latestEnd() }
            )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(FIELD_INSET)
                .alpha(if (active) 1f else 0.55f)
                .semantics {
                    contentDescription = copy.padDescription
                    stateDescription = state
                    customActions = labels.indices.map { pole ->
                        val direction = OrbitEqualizer.poleDirection(poleAngles[pole])
                        CustomAccessibilityAction(labels[pole]) { onNudge(direction.tilt * NUDGE, direction.contour * NUDGE); true }
                    } + listOf(
                        CustomAccessibilityAction(copy.center) { onPole(OrbitPosition.Center); true },
                        CustomAccessibilityAction(copy.resetPoles) { onResetPoles(); true }
                    )
                }
        ) {
            drawOrbitField(position, poleAngles, accent, halo, hairline, muted, surface)
        }
        labels.forEachIndexed { pole, label ->
            val vertical = abs(OrbitEqualizer.poleDirection(poleAngles[pole]).contour) > 0.7f
            OrbitPoleLabel(
                label = label,
                angle = { latestAngles[pole] },
                stack = { latestAngles.take(pole).count { angularGap(it, latestAngles[pole]) < LABEL_GAP_DEGREES } },
                maxWidth = if (vertical) 168.dp else 96.dp,
                resetLabel = copy.resetPoles,
                onTap = { onPole(poleTarget(poleAngles[pole])) },
                onReset = onResetPoles,
                onDragStart = { onPoleDragStart(pole) },
                onDrag = { degrees -> onPoleDrag(pole, degrees) },
                onDragEnd = onPoleDragEnd
            )
        }
    }
}

private fun Modifier.orbitPadGestures(
    position: () -> OrbitPosition,
    onTap: (OrbitPosition) -> Unit,
    onStart: () -> Unit,
    onDrag: (OrbitPosition) -> Unit,
    onEnd: () -> Unit
): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown()
        val inset = FIELD_INSET.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.width / 2f - inset
        fun unit(point: Offset) = OrbitPosition(
            (point.x - center.x) / radius,
            (center.y - point.y) / radius
        ).clamped()
        val current = position()
        val handle = Offset(center.x + current.tilt * radius, center.y - current.contour * radius)
        if ((down.position - handle).getDistance() > GRAB_RADIUS.toPx()) {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                if (change.isConsumed || (change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                    return@awaitEachGesture
                }
                if (!change.pressed) {
                    change.consume()
                    if ((down.position - center).getDistance() <= radius + viewConfiguration.touchSlop) onTap(unit(down.position))
                    return@awaitEachGesture
                }
            }
        }
        down.consume()
        onStart()
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) break
            change.consume()
            onDrag(unit(change.position))
        }
        onEnd()
    }
}

private fun DrawScope.drawOrbitField(
    position: OrbitPosition,
    poleAngles: List<Float>,
    accent: Color,
    halo: Float,
    hairline: Color,
    muted: Color,
    surface: Color
) {
    val radius = size.minDimension / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    val point = Offset(center.x + position.tilt * radius, center.y - position.contour * radius)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = 0.10f + 0.22f * position.radius), Color.Transparent),
            center = point,
            radius = radius * 1.25f
        ),
        radius = radius,
        center = center
    )
    listOf(1f / 3f, 2f / 3f).forEach { fraction ->
        drawCircle(hairline, radius = radius * fraction, center = center, style = Stroke(1.dp.toPx()))
    }
    drawCircle(accent.copy(alpha = 0.55f), radius = radius, center = center, style = Stroke(1.5.dp.toPx()))
    drawLine(hairline, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), 1.dp.toPx())
    drawLine(hairline, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), 1.dp.toPx())
    poleAngles.forEach { angle ->
        val direction = OrbitEqualizer.poleDirection(angle)
        val anchor = Offset(center.x + direction.tilt * radius, center.y - direction.contour * radius)
        drawCircle(surface, radius = 5.dp.toPx(), center = anchor)
        drawCircle(accent.copy(alpha = 0.7f), radius = 3.5.dp.toPx(), center = anchor)
    }
    drawLine(
        brush = Brush.linearGradient(listOf(accent.copy(alpha = 0.05f), accent.copy(alpha = 0.75f)), center, point),
        start = center,
        end = point,
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawCircle(muted, radius = 2.5.dp.toPx(), center = center)
    drawCircle(accent.copy(alpha = 0.2f), radius = 20.dp.toPx() * halo, center = point)
    drawCircle(surface, radius = 11.dp.toPx(), center = point)
    drawCircle(accent, radius = 8.dp.toPx(), center = point)
}

private fun poleState(position: OrbitPosition, poleAngles: List<Float>, labels: List<String>): String {
    val weights = OrbitEqualizer.poleWeights(position, poleAngles)
    return labels.indices.joinToString(", ") { pole -> "${labels[pole]} ${(weights[pole] * 100).roundToInt()}%" }
}

private class RingSlot {
    var x = 0
    var y = 0
    var fieldWidth = 0
    var fieldHeight = 0

    fun fieldOffset(local: Offset): Offset = Offset(x + local.x - fieldWidth / 2f, fieldHeight / 2f - (y + local.y))
}

internal fun draggedPoleAngle(startPole: Float, startFinger: Offset, finger: Offset): Float =
    OrbitEqualizer.normalizedAngle(startPole + fingerDegrees(finger) - fingerDegrees(startFinger))

private fun fingerDegrees(point: Offset): Float = Math.toDegrees(atan2(point.y.toDouble(), point.x.toDouble())).toFloat()

private fun angularGap(first: Float, second: Float): Float = abs(((first - second) % 360f + 540f) % 360f - 180f)

private fun Modifier.ringAnchor(angle: () -> Float, stack: () -> Int, slot: RingSlot): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    val width = constraints.maxWidth
    val height = constraints.maxHeight
    layout(width, height) {
        val direction = OrbitEqualizer.poleDirection(angle())
        val distance = width / 2f - FIELD_INSET.toPx() / 2f
        val x = (width / 2f + direction.tilt * distance - placeable.width / 2f).roundToInt()
        val lift = stack() * placeable.height * if (direction.contour >= 0f) 1 else -1
        val y = (height / 2f - direction.contour * distance - placeable.height / 2f).roundToInt() + lift
        slot.x = x.coerceIn(0, (width - placeable.width).coerceAtLeast(0))
        slot.y = y.coerceIn(0, (height - placeable.height).coerceAtLeast(0))
        slot.fieldWidth = width
        slot.fieldHeight = height
        placeable.place(slot.x, slot.y)
    }
}

@Composable
private fun OrbitPoleLabel(
    label: String,
    angle: () -> Float,
    stack: () -> Int,
    maxWidth: Dp,
    resetLabel: String,
    onTap: () -> Unit,
    onReset: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val slot = remember { RingSlot() }
    val latestDragStart by rememberUpdatedState(onDragStart)
    val latestDrag by rememberUpdatedState(onDrag)
    val latestDragEnd by rememberUpdatedState(onDragEnd)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .ringAnchor(angle, stack, slot)
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp, maxWidth = maxWidth)
            .pointerInput(Unit) {
                var startPole = 0f
                var startFinger = Offset.Zero
                detectDragGestures(
                    onDragStart = { start ->
                        startPole = angle()
                        startFinger = slot.fieldOffset(start)
                        latestDragStart()
                    },
                    onDragEnd = { latestDragEnd() },
                    onDragCancel = { latestDragEnd() }
                ) { change, _ ->
                    change.consume()
                    latestDrag(draggedPoleAngle(startPole, startFinger, slot.fieldOffset(change.position)))
                }
            }
            .combinedClickable(role = Role.Button, onLongClickLabel = resetLabel, onLongClick = onReset, onClick = onTap)
            .semantics {
                val degrees = angle()
                progressBarRangeInfo = ProgressBarRangeInfo(degrees, 0f..360f)
                stateDescription = "${degrees.roundToInt()}°"
                setProgress { target ->
                    latestDragStart()
                    latestDrag(target)
                    latestDragEnd()
                    true
                }
            }
    ) {
        BasicText(
            text = label,
            style = TextStyle(color = LevyraMuted, fontWeight = FontWeight.Black),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(minFontSize = 7.sp, maxFontSize = 11.sp)
        )
    }
}

@Composable
private fun OrbitCompareAction(
    label: String,
    enabled: Boolean,
    comparing: Boolean,
    modifier: Modifier,
    onComparing: (Boolean) -> Unit
) {
    val latest by rememberUpdatedState(onComparing)
    Surface(
        color = if (comparing) LevyraCyan.copy(alpha = 0.16f) else LevyraAdaptiveChip,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (comparing) LevyraCyan.copy(alpha = 0.55f) else LevyraAdaptiveHairline),
        modifier = modifier
            .heightIn(min = 48.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .semantics {
                role = Role.Button
                selected = comparing
                if (enabled) onClick { latest(!comparing); true }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(onPress = {
                    latest(true)
                    tryAwaitRelease()
                    latest(false)
                })
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.AutoMirrored.Rounded.CompareArrows, contentDescription = null, tint = LevyraCyan, modifier = Modifier.size(18.dp))
            Text(
                label,
                color = LevyraText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 7.dp)
            )
        }
    }
}

@Composable
private fun OrbitSavedChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) LevyraCyan.copy(alpha = 0.16f) else LevyraAdaptiveChip,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (selected) LevyraCyan.copy(alpha = 0.55f) else LevyraAdaptiveHairline),
        modifier = Modifier
            .heightIn(min = 44.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                label,
                color = if (selected) LevyraCyan else LevyraText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
