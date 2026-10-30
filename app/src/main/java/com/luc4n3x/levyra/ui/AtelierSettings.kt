package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.AtelierDesign

private val AtelierSettingsKeySize: Dp = 40.dp
private val AtelierSettingsKeyShape = RoundedCornerShape(10.dp)
private val AtelierSettingsKeyFill = Brush.verticalGradient(listOf(Color(0xFF2C2C31), Color(0xFF141417)))
private val AtelierSettingsKeyHighlight = Brush.horizontalGradient(listOf(Color(0x00FFFFFF), Color(0x40FFFFFF), Color(0x00FFFFFF)))
private val AtelierSettingsKeyEdge = Color(0x66000000)
private val AtelierSettingsRowHeight: Dp = 72.dp
private val AtelierSettingsPanelPadding: Dp = 16.dp
private val AtelierSettingsCapHeight: Dp = AtelierDesign.panelCorner

@Composable
internal fun atelierUppercaseLabel(text: String): String =
    text.uppercase(java.util.Locale.forLanguageTag(com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings.current.code))

@Composable
internal fun AtelierIconTarget(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier.size(48.dp).clip(AtelierDesign.cardShape)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, label, tint = tint, modifier = Modifier.size(24.dp)) }
}

private fun Modifier.atelierPanelSegment(top: Boolean, bottom: Boolean): Modifier = drawWithCache {
    val radius = AtelierDesign.panelCorner.toPx().coerceAtMost(size.height)
    val stroke = 1.dp.toPx()
    val half = stroke / 2f
    val w = size.width
    val h = size.height
    val topCorner = if (top) CornerRadius(radius) else CornerRadius.Zero
    val bottomCorner = if (bottom) CornerRadius(radius) else CornerRadius.Zero
    val fill = Path().apply {
        addRoundRect(RoundRect(Rect(0f, 0f, w, h), topCorner, topCorner, bottomCorner, bottomCorner))
    }
    val edge = Path().apply {
        if (top) {
            moveTo(half, h)
            lineTo(half, radius)
            arcTo(Rect(half, half, half + radius * 2f, half + radius * 2f), 180f, 90f, false)
            lineTo(w - half - radius, half)
            arcTo(Rect(w - half - radius * 2f, half, w - half, half + radius * 2f), 270f, 90f, false)
            lineTo(w - half, h)
        }
        if (bottom) {
            moveTo(half, 0f)
            lineTo(half, h - radius)
            arcTo(Rect(half, h - half - radius * 2f, half + radius * 2f, h - half), 180f, -90f, false)
            lineTo(w - half - radius, h - half)
            arcTo(Rect(w - half - radius * 2f, h - half - radius * 2f, w - half, h - half), 90f, -90f, false)
            lineTo(w - half, 0f)
        }
        if (!top && !bottom) {
            moveTo(half, 0f)
            lineTo(half, h)
            moveTo(w - half, 0f)
            lineTo(w - half, h)
        }
    }
    onDrawBehind {
        drawPath(fill, AtelierDesign.cabinet)
        drawPath(edge, AtelierDesign.hairline, style = Stroke(stroke))
    }
}

@Composable
internal fun AtelierSettingsHeader(onClose: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Column(Modifier.fillMaxWidth()) {
        AtelierIconTarget(Icons.AutoMirrored.Rounded.ArrowBack, strings.close, AtelierDesign.text, onClose)
        Spacer(Modifier.height(14.dp))
        Text(
            strings.settings, style = MaterialTheme.typography.displaySmall, color = AtelierDesign.text,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(6.dp))
        Text(strings.settingsSubtitle, style = MaterialTheme.typography.bodyMedium, color = AtelierDesign.textSecondary)
        Spacer(Modifier.height(18.dp))
        Box(Modifier.width(32.dp).height(1.dp).background(AtelierDesign.accent))
    }
}

@Composable
internal fun AtelierSettingsGroupLabel(text: String) {
    Column(Modifier.fillMaxWidth().padding(top = AtelierDesign.sectionGap - 8.dp)) {
        Text(
            atelierUppercaseLabel(text),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.6.sp),
            color = AtelierDesign.accent,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp).semantics { heading() }
        )
        Box(Modifier.fillMaxWidth().height(AtelierSettingsCapHeight).atelierPanelSegment(top = true, bottom = false))
    }
}

@Composable
internal fun AtelierSettingsRow(title: String, summary: String, icon: ImageVector, last: Boolean, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().atelierPanelSegment(top = false, bottom = last)) {
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)
                .heightIn(min = AtelierSettingsRowHeight)
                .padding(horizontal = AtelierSettingsPanelPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AtelierSettingsKey(icon)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = AtelierDesign.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (summary.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(summary, style = MaterialTheme.typography.bodySmall, color = AtelierDesign.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.width(12.dp))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = AtelierDesign.textFaint, modifier = Modifier.size(22.dp))
        }
        if (last) {
            Spacer(Modifier.height(4.dp))
        } else {
            Box(
                Modifier.padding(start = AtelierSettingsPanelPadding + AtelierSettingsKeySize + 16.dp, end = AtelierSettingsPanelPadding)
                    .fillMaxWidth().height(1.dp).background(AtelierDesign.hairline)
            )
        }
    }
}

@Composable
private fun AtelierSettingsKey(icon: ImageVector) {
    Box(
        Modifier.size(AtelierSettingsKeySize)
            .clip(AtelierSettingsKeyShape)
            .background(AtelierSettingsKeyFill)
            .drawWithCache {
                val stroke = 1.dp.toPx()
                val inset = 10.dp.toPx() * 0.5f
                val edgeRadius = CornerRadius(10.dp.toPx())
                onDrawWithContent {
                    drawContent()
                    drawRoundRect(AtelierSettingsKeyEdge, cornerRadius = edgeRadius, style = Stroke(stroke))
                    drawLine(AtelierSettingsKeyHighlight, Offset(inset, stroke / 2f), Offset(size.width - inset, stroke / 2f), stroke)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = AtelierDesign.accent, modifier = Modifier.size(20.dp))
    }
}
