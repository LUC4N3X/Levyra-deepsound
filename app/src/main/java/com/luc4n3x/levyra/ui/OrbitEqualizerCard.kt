package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.OrbitEqualizer
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
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

private const val CENTER_SNAP = 0.06f
private const val POLE_REACH = 0.75f
private const val NUDGE = 0.1f
private val GRAB_RADIUS = 40.dp

@Composable
internal fun OrbitEqualizerCard(
    enabled: Boolean,
    activeProfile: ParametricEqProfile?,
    customProfiles: List<ParametricEqProfile>,
    copy: OrbitEqCopy,
    savedTitle: String,
    actions: ParametricProfileActions,
    onApply: (ParametricEqProfile) -> Unit,
    onSave: (String, ParametricEqProfile) -> Boolean
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val latestApply by rememberUpdatedState(onApply)
    val committed = remember(activeProfile) { OrbitEqualizer.positionOf(activeProfile) ?: OrbitPosition.Center }
    val orbit = remember { OrbitEqualizerState(committed) }
    fun commit(position: OrbitPosition) = latestApply(OrbitEqualizer.shape(position, copy.title).profile)
    fun glideTo(position: OrbitPosition) {
        scope.launch { commit(orbit.glideTo(position)) }
    }
    OrbitAuditionEffects(orbit, committed, copy.title, actions.onAudition)

    val dragging = orbit.dragging
    val comparing = orbit.comparing
    val live = orbit.positionNow()
    val shape = remember(live) { OrbitEqualizer.shape(live, copy.title) }
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
                onNudge = { dx, dy -> glideTo(OrbitPosition(live.tilt + dx, live.contour + dy)) }
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
                    onSave(name, shape.profile)
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
private class OrbitEqualizerState(initial: OrbitPosition) {
    private val satellite = Animatable(Offset(initial.tilt, initial.contour), Offset.VectorConverter)
    private var dragTarget by mutableStateOf<OrbitPosition?>(null)
    var dragging by mutableStateOf(false)
        private set
    var comparing by mutableStateOf(false)
    var auditioning = false

    val moving: Boolean get() = dragging || satellite.isRunning

    fun positionNow(): OrbitPosition =
        (dragTarget ?: OrbitPosition(satellite.value.x, satellite.value.y)).clamped().quantized()

    suspend fun glideTo(position: OrbitPosition): OrbitPosition {
        val target = position.clamped()
        satellite.animateTo(Offset(target.tilt, target.contour), spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow))
        return target.quantized()
    }

    suspend fun settle(position: OrbitPosition) {
        if (!moving) satellite.snapTo(Offset(position.tilt, position.contour))
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
    committed: OrbitPosition,
    title: String,
    onAudition: (ParametricEqProfile?) -> Unit
) {
    val latestAudition by rememberUpdatedState(onAudition)
    LaunchedEffect(committed) { orbit.settle(committed) }
    LaunchedEffect(orbit) {
        snapshotFlow { if (orbit.moving) orbit.positionNow() else null }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { position ->
                orbit.auditioning = true
                latestAudition(OrbitEqualizer.shape(position, title).profile)
            }
    }
    DisposableEffect(orbit) {
        onDispose { if (orbit.auditioning) latestAudition(null) }
    }
    LaunchedEffect(orbit.comparing) {
        if (orbit.comparing) {
            latestAudition(OrbitEqualizer.comparisonProfile(OrbitEqualizer.shape(orbit.positionNow(), title)))
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
    val saved = remember(customProfiles) { customProfiles.filter { OrbitEqualizer.positionOf(it) != null } }
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
    accent: Color,
    active: Boolean,
    dragging: Boolean,
    copy: OrbitEqCopy,
    onPole: (OrbitPosition) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (OrbitPosition) -> Unit,
    onDragEnd: () -> Unit,
    onNudge: (Float, Float) -> Unit
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
    val state = poleState(position, copy)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = 380.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(36.dp)
                .alpha(if (active) 1f else 0.55f)
                .semantics {
                    contentDescription = copy.padDescription
                    stateDescription = state
                    customActions = listOf(
                        CustomAccessibilityAction(copy.warm) { onNudge(-NUDGE, 0f); true },
                        CustomAccessibilityAction(copy.bright) { onNudge(NUDGE, 0f); true },
                        CustomAccessibilityAction(copy.forward) { onNudge(0f, NUDGE); true },
                        CustomAccessibilityAction(copy.open) { onNudge(0f, -NUDGE); true },
                        CustomAccessibilityAction(copy.center) { onPole(OrbitPosition.Center); true }
                    )
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val radius = size.width / 2f
                        fun unit(point: Offset) = OrbitPosition(
                            (point.x - radius) / radius,
                            (radius - point.y) / radius
                        ).clamped()
                        val current = latestPosition
                        val handle = Offset(radius + current.tilt * radius, radius - current.contour * radius)
                        if ((down.position - handle).getDistance() > GRAB_RADIUS.toPx()) {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                                if (change.isConsumed || (change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                    return@awaitEachGesture
                                }
                                if (!change.pressed) {
                                    change.consume()
                                    latestTap(unit(down.position))
                                    return@awaitEachGesture
                                }
                            }
                        }
                        down.consume()
                        latestStart()
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            change.consume()
                            latestDrag(unit(change.position))
                        }
                        latestEnd()
                    }
                }
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
        OrbitPoleLabel(copy.forward, Modifier.align(Alignment.TopCenter), maxWidth = 168.dp) { onPole(OrbitPosition(0f, POLE_REACH)) }
        OrbitPoleLabel(copy.open, Modifier.align(Alignment.BottomCenter), maxWidth = 168.dp) { onPole(OrbitPosition(0f, -POLE_REACH)) }
        OrbitPoleLabel(copy.warm, Modifier.align(AbsoluteAlignment.CenterLeft)) { onPole(OrbitPosition(-POLE_REACH, 0f)) }
        OrbitPoleLabel(copy.bright, Modifier.align(AbsoluteAlignment.CenterRight)) { onPole(OrbitPosition(POLE_REACH, 0f)) }
    }
}

private fun poleState(position: OrbitPosition, copy: OrbitEqCopy): String {
    fun percent(value: Float) = (abs(value) * 100).roundToInt()
    val tilt = if (position.tilt < 0f) copy.warm else copy.bright
    val contour = if (position.contour < 0f) copy.open else copy.forward
    return "$tilt ${percent(position.tilt)}%, $contour ${percent(position.contour)}%"
}

@Composable
private fun OrbitPoleLabel(label: String, modifier: Modifier, maxWidth: Dp = 96.dp, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp, maxWidth = maxWidth)
            .clickable(role = Role.Button, onClick = onClick)
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
