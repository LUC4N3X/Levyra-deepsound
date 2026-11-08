package com.luc4n3x.levyra.ui.jam

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.feature.jam.JamGuestPermission
import com.luc4n3x.levyra.feature.jam.JamParticipant
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.MonoOverline
import com.luc4n3x.levyra.ui.MonoWaveLine
import com.luc4n3x.levyra.ui.components.LevyraBackButton
import com.luc4n3x.levyra.ui.i18n.LevyraMonoSettingsStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.monoPanel
import com.luc4n3x.levyra.ui.theme.MonoDesign
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val MonoJamRingFractions = floatArrayOf(0.17f, 0.31f, 0.45f)
private val MonoJamRingCapacity = intArrayOf(0, 3, 6)
private val MonoJamHostAvatar: Dp = 56.dp
private val MonoJamGuestAvatar: Dp = 38.dp
private const val MonoJamRingSteps = 144

@Composable
internal fun MonoJamHeader(title: String, subtitle: String, onClose: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val copy = LevyraMonoSettingsStrings.forCode(strings.code)
    Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        LevyraBackButton(
            onClick = onClose,
            contentDescription = strings.back,
            size = 48.dp,
            colors = IconButtonDefaults.iconButtonColors(containerColor = MonoDesign.surface, contentColor = MonoDesign.text),
            modifier = Modifier.border(1.dp, MonoDesign.hairline, CircleShape)
        )
        Spacer(Modifier.height(24.dp))
        MonoOverline(title, color = MonoDesign.accent)
        Spacer(Modifier.height(8.dp))
        Text(
            copy.jamHeadline, color = MonoDesign.text, style = MaterialTheme.typography.displaySmall,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(6.dp))
        Text(subtitle, color = MonoDesign.textSecondary, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(18.dp))
        MonoWaveLine(
            playing = false, modifier = Modifier.fillMaxWidth().height(12.dp).clearAndSetSemantics { },
            color = MonoDesign.hairlineStrong, cycles = 16f, strokeWidth = 1.dp
        )
    }
}

@Composable
internal fun MonoJamSessionHero(
    participants: List<JamParticipant>,
    isHost: Boolean,
    connected: Boolean,
    connectionLabel: String,
    locked: Boolean,
    code: String,
    onShare: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = LevyraMonoSettingsStrings.forCode(strings.code)
    val led by animateColorAsState(if (connected) MonoDesign.accent else MonoDesign.textFaint, label = "mono-jam-led")
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MonoDesign.itemGap)) {
        Column(Modifier.fillMaxWidth().monoPanel(MonoDesign.panelShape)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier.size(8.dp).drawBehind {
                        drawCircle(led.copy(alpha = 0.24f), radius = size.minDimension * 1.1f)
                        drawCircle(led)
                    }.clearAndSetSemantics { }
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    MonoOverline(copy.jamLiveSession, color = MonoDesign.text)
                    Text(
                        connectionLabel, color = if (connected) MonoDesign.accent else MonoDesign.textSecondary,
                        style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                if (locked) MonoJamTag(strings.jamLocked, icon = Icons.Rounded.Lock)
                MonoJamTag(if (isHost) strings.jamRoleHost else strings.jamRoleGuest, accent = isHost)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(MonoDesign.hairline))
            Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                MonoJamRadar(
                    participants = participants,
                    live = connected,
                    description = copy.jamRadar.format(participants.size),
                    modifier = Modifier.widthIn(max = 340.dp)
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(MonoDesign.hairline))
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MonoOverline(strings.jamParticipants, modifier = Modifier.weight(1f))
                Text(
                    participants.size.toString().padStart(2, '0'),
                    color = MonoDesign.text,
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = MaterialTheme.typography.labelMedium.fontFamily)
                )
            }
        }
        if (isHost && code.isNotBlank()) MonoJamInvitePanel(code, copy.jamInvite, copy.jamInviteHint, onShare)
    }
}

@Composable
private fun MonoJamTag(label: String, accent: Boolean = false, icon: ImageVector? = null) {
    Row(
        Modifier
            .clip(MonoDesign.pillShape)
            .background(if (accent) MonoDesign.accentGlow else Color.Transparent)
            .border(1.dp, if (accent) MonoDesign.accentDim else MonoDesign.hairlineStrong, MonoDesign.pillShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) Icon(icon, null, tint = MonoDesign.textSecondary, modifier = Modifier.size(12.dp))
        Text(
            label.uppercase(), style = MaterialTheme.typography.labelSmall,
            color = if (accent) MonoDesign.accent else MonoDesign.textSecondary, maxLines = 1
        )
    }
}

@Suppress("ComplexMethod", "CognitiveComplexMethod")
@Composable
private fun MonoJamRadar(participants: List<JamParticipant>, live: Boolean, description: String, modifier: Modifier = Modifier) {
    val host = remember(participants) { participants.firstOrNull { it.isHost } }
    val guests = remember(participants) { participants.filterNot { it.isHost } }
    val animate = LocalAnimationsEnabled.current
    val energy = animateFloatAsState(
        if (live) 0.4f + 0.1f * participants.size.coerceAtMost(6) else 0.12f,
        if (animate) spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessVeryLow) else snap(),
        label = "mono-jam-energy"
    )
    val phase: State<Float>? = if (live && animate) {
        rememberInfiniteTransition(label = "mono-jam-radar").animateFloat(
            0f, 2f * PI.toFloat(),
            infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
            label = "mono-jam-radar-phase"
        )
    } else null
    val paths = remember { Array(MonoJamRingFractions.size) { Path() } }
    BoxWithConstraints(
        modifier.fillMaxWidth().aspectRatio(1f).semantics { contentDescription = description }
    ) {
        val side = maxWidth
        Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
            drawMonoJamRadar(paths, phase?.value ?: 0f, energy.value)
        }
        host?.let { MonoJamAvatar(it.name, true, MonoJamHostAvatar, Modifier.monoRadarSlot(side, 0f, 0f, MonoJamHostAvatar)) }
        val shown = guests.size.coerceAtMost(MonoJamRingCapacity.sum())
        for (index in 0 until shown) {
            val ring = if (index < MonoJamRingCapacity[1]) 1 else 2
            val slot = if (ring == 1) index else index - MonoJamRingCapacity[1]
            val ringCount = if (ring == 1) shown.coerceAtMost(MonoJamRingCapacity[1]) else shown - MonoJamRingCapacity[1]
            val angle = (-PI / 2 + ring * 0.6 + 2 * PI * slot / ringCount).toFloat()
            val overflow = guests.size - shown
            val label = if (overflow > 0 && index == shown - 1) "+${overflow + 1}" else guests[index].name
            MonoJamAvatar(
                label, false, MonoJamGuestAvatar,
                Modifier.monoRadarSlot(side, MonoJamRingFractions[ring], angle, MonoJamGuestAvatar),
                initialOnly = !label.startsWith("+")
            )
        }
    }
}

private fun Modifier.monoRadarSlot(side: Dp, fraction: Float, angle: Float, avatar: Dp): Modifier {
    val radius = side * fraction
    return absoluteOffset(
        x = side / 2 + radius * cos(angle) - avatar / 2,
        y = side / 2 + radius * sin(angle) - avatar / 2
    )
}

private fun DrawScope.drawMonoJamRadar(paths: Array<Path>, phase: Float, energy: Float) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val side = size.minDimension
    val hair = 1.dp.toPx()
    val outer = side * 0.49f
    drawLine(MonoDesign.hairline, Offset(center.x - outer, center.y), Offset(center.x + outer, center.y), hair)
    drawLine(MonoDesign.hairline, Offset(center.x, center.y - outer), Offset(center.x, center.y + outer), hair)
    drawCircle(MonoDesign.hairline, outer, center, style = Stroke(hair))
    MonoJamRingFractions.forEachIndexed { ring, fraction ->
        val radius = side * fraction
        drawCircle(MonoDesign.hairline, radius, center, style = Stroke(hair))
        val wobble = 4.dp.toPx() * energy * (1f + ring * 0.45f)
        val lobes = 6 + ring * 3
        val direction = if (ring % 2 == 0) 1f else -1f
        val path = paths[ring]
        path.rewind()
        for (i in 0..MonoJamRingSteps) {
            val t = 2f * PI.toFloat() * i / MonoJamRingSteps
            val envelope = 0.45f + 0.55f * sin(t * 2f + phase * direction + ring)
            val r = radius + wobble * envelope * sin(t * lobes + phase * direction * (1f + ring * 0.35f) + ring * 1.7f)
            val x = center.x + r * cos(t)
            val y = center.y + r * sin(t)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        val color = if (ring == 0) MonoDesign.accent else MonoDesign.text.copy(alpha = 0.55f - ring * 0.15f)
        drawPath(path, color, style = Stroke(if (ring == 0) 1.6.dp.toPx() else 1.2.dp.toPx()))
    }
    drawCircle(MonoDesign.accentGlow, side * 0.11f * (0.9f + energy * 0.2f), center)
}

@Composable
private fun MonoJamAvatar(name: String, host: Boolean, size: Dp, modifier: Modifier = Modifier, initialOnly: Boolean = true) {
    val label = remember(name, initialOnly) { if (initialOnly) name.trim().firstOrNull()?.uppercase() ?: "?" else name }
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(if (host) MonoDesign.accent else MonoDesign.raised)
            .border(1.dp, if (host) MonoDesign.accent else MonoDesign.hairlineStrong, CircleShape)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (host) MonoDesign.onAccent else MonoDesign.text,
            style = if (host) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun MonoJamInvitePanel(code: String, title: String, hint: String, onShare: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Column(
        Modifier.fillMaxWidth().monoPanel(MonoDesign.panelShape).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        MonoOverline(title, color = MonoDesign.accent, modifier = Modifier.semantics { heading() })
        JamInvitationCode(code)
        Text(hint, color = MonoDesign.textSecondary, style = MaterialTheme.typography.bodySmall)
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(MonoDesign.pillShape)
                .background(MonoDesign.accent)
                .clickable(role = Role.Button, onClick = onShare)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            Icon(Icons.Rounded.IosShare, null, tint = MonoDesign.onAccent, modifier = Modifier.size(18.dp))
            Text(strings.jamShareInvite, color = MonoDesign.onAccent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun DrawScope.drawMonoViewfinder(color: Color) {
    val inset = 10.dp.toPx()
    val arm = 16.dp.toPx()
    val stroke = 1.5.dp.toPx()
    val l = inset
    val t = inset
    val r = size.width - inset
    val b = size.height - inset
    drawLine(color, Offset(l, t), Offset(l + arm, t), stroke)
    drawLine(color, Offset(l, t), Offset(l, t + arm), stroke)
    drawLine(color, Offset(r, t), Offset(r - arm, t), stroke)
    drawLine(color, Offset(r, t), Offset(r, t + arm), stroke)
    drawLine(color, Offset(l, b), Offset(l + arm, b), stroke)
    drawLine(color, Offset(l, b), Offset(l, b - arm), stroke)
    drawLine(color, Offset(r, b), Offset(r - arm, b), stroke)
    drawLine(color, Offset(r, b), Offset(r, b - arm), stroke)
}

@Composable
internal fun MonoJamPermissionPicker(
    options: List<JamGuestPermission>,
    selected: JamGuestPermission,
    label: (JamGuestPermission) -> String,
    onSelect: (JamGuestPermission) -> Unit
) {
    val index = options.indexOf(selected).coerceAtLeast(0)
    val position = animateFloatAsState(
        index.toFloat(),
        if (LocalAnimationsEnabled.current) spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow) else snap(),
        label = "mono-jam-permission"
    )
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val count = options.size
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(MonoDesign.pillShape)
            .border(1.dp, MonoDesign.hairlineStrong, MonoDesign.pillShape)
            .drawBehind {
                val inset = 4.dp.toPx()
                val segment = (size.width - inset * 2f) / count
                val height = size.height - inset * 2f
                val x = if (rtl) size.width - inset - segment * (position.value + 1f) else inset + segment * position.value
                drawRoundRect(MonoDesign.accent, Offset(x, inset), Size(segment, height), CornerRadius(height / 2f))
            }
            .padding(4.dp)
            .selectableGroup()
    ) {
        options.forEach { option ->
            val active = option == selected
            val color by animateColorAsState(if (active) MonoDesign.onAccent else MonoDesign.textSecondary, label = "mono-jam-permission-text")
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .heightIn(min = 48.dp)
                    .clip(MonoDesign.pillShape)
                    .selectable(selected = active, role = Role.RadioButton) { onSelect(option) }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label(option), color = color, style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
