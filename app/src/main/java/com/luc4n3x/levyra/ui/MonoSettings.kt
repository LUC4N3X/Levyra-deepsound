package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.MonoDesign

private val MonoSettingsRowHeight: Dp = 56.dp
private val MonoSettingsTile: Dp = 36.dp
private val MonoSettingsTileShape = RoundedCornerShape(10.dp)
private val MonoSettingsInset: Dp = 14.dp

@Composable
internal fun MonoSettingsHeader(onClose: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(top = 6.dp)) {
                Text(
                    strings.settings, style = MaterialTheme.typography.headlineLarge, color = MonoDesign.text,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.semantics { heading() }
                )
                Spacer(Modifier.height(4.dp))
                Text(strings.settingsSubtitle, style = MaterialTheme.typography.bodyMedium, color = MonoDesign.textSecondary)
            }
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MonoDesign.surface)
                    .border(1.dp, MonoDesign.hairline, CircleShape)
                    .clickable(role = Role.Button, onClickLabel = strings.close, onClick = onClose),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.Close, strings.close, tint = MonoDesign.text, modifier = Modifier.size(22.dp)) }
        }
        Spacer(Modifier.height(18.dp))
        Box(Modifier.width(28.dp).height(2.dp).background(MonoDesign.accent, MonoDesign.pillShape))
    }
}

@Composable
internal fun MonoSettingsGroupLabel(text: String) {
    MonoOverline(
        text,
        modifier = Modifier.padding(start = 4.dp, top = 28.dp, bottom = 10.dp).semantics { heading() }
    )
}

private fun Modifier.monoPanelSegment(top: Boolean, bottom: Boolean): Modifier = drawBehind {
    val radius = MonoDesign.panelCorner.toPx()
    val stroke = 1.dp.toPx()
    val half = stroke / 2f
    val reach = radius * 2f
    val y = if (top) 0f else -reach
    val height = (if (bottom) size.height else size.height + reach) - y
    clipRect {
        drawRoundRect(MonoDesign.surface, Offset(0f, y), Size(size.width, height), CornerRadius(radius))
        drawRoundRect(
            MonoDesign.hairline, Offset(half, y + half), Size(size.width - stroke, height - stroke),
            CornerRadius(radius - half), style = Stroke(stroke)
        )
    }
}

@Composable
internal fun MonoSettingsRow(title: String, summary: String, icon: ImageVector, first: Boolean, last: Boolean, onClick: () -> Unit) {
    val corner = MonoDesign.panelCorner
    val shape = RoundedCornerShape(
        topStart = if (first) corner else 0.dp, topEnd = if (first) corner else 0.dp,
        bottomStart = if (last) corner else 0.dp, bottomEnd = if (last) corner else 0.dp
    )
    Column(Modifier.fillMaxWidth().monoPanelSegment(first, last)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .clickable(role = Role.Button, onClick = onClick)
                .heightIn(min = MonoSettingsRowHeight)
                .padding(horizontal = MonoSettingsInset, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(MonoSettingsTile)
                    .clip(MonoSettingsTileShape)
                    .background(MonoDesign.raised)
                    .border(1.dp, MonoDesign.hairline, MonoSettingsTileShape),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = MonoDesign.text, modifier = Modifier.size(19.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MonoDesign.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (summary.isNotBlank()) {
                    Text(summary, style = MaterialTheme.typography.bodySmall, color = MonoDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.width(10.dp))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MonoDesign.textFaint, modifier = Modifier.size(20.dp))
        }
        if (!last) {
            Box(
                Modifier.padding(start = MonoSettingsInset + MonoSettingsTile + 14.dp, end = MonoSettingsInset)
                    .fillMaxWidth().height(1.dp).background(MonoDesign.hairline)
            )
        }
    }
}

@Composable
internal fun MonoSettingsSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val travel = animateFloatAsState(
        if (checked) 1f else 0f,
        if (LocalAnimationsEnabled.current) spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium) else snap(),
        label = "mono-switch"
    )
    Canvas(modifier.size(width = 46.dp, height = 26.dp)) {
        val t = travel.value
        val stroke = 1.dp.toPx()
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(MonoDesign.accent.copy(alpha = t), cornerRadius = radius)
        drawRoundRect(
            lerp(MonoDesign.hairlineStrong, MonoDesign.accent, t), Offset(stroke / 2f, stroke / 2f),
            Size(size.width - stroke, size.height - stroke), CornerRadius(size.height / 2f - stroke / 2f), style = Stroke(stroke)
        )
        val thumb = size.height / 2f - 4.dp.toPx() + 1.dp.toPx() * t
        val travelSpan = size.width - size.height
        val x = if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Rtl) size.width - size.height / 2f - travelSpan * t else size.height / 2f + travelSpan * t
        drawCircle(lerp(MonoDesign.textSecondary, MonoDesign.onAccent, t), thumb, Offset(x, size.height / 2f))
    }
}
