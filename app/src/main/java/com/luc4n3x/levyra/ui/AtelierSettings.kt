package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.selected
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
private val AtelierSettingsKeyFill = Brush.radialGradient(listOf(Color(0xFF34343A), Color(0xFF111114)))
internal val AtelierSettingsPanelFill = Color(0xFF111114)
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
        drawPath(fill, AtelierSettingsPanelFill)
        drawPath(edge, AtelierBrass.copy(alpha = 0.32f), style = Stroke(stroke))
    }
}

@Composable
internal fun AtelierSettingsHeader(onClose: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Column(Modifier.fillMaxWidth()) {
        AtelierIconTarget(Icons.AutoMirrored.Rounded.ArrowBack, strings.close, AtelierDesign.text, onClose)
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier.fillMaxWidth()
                .graphicsLayer {
                    rotationZ = -0.8f
                    shadowElevation = 16.dp.toPx()
                }
                .background(AtelierPaper)
                .padding(8.dp)
                .border(1.dp, AtelierInk.copy(alpha = 0.7f))
                .padding(3.dp)
                .border(0.6.dp, AtelierInk.copy(alpha = 0.45f))
                .padding(horizontal = 18.dp, vertical = 22.dp)
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "— ✦ —", fontFamily = AtelierSerif, fontSize = 14.sp, color = AtelierInk.copy(alpha = 0.55f),
                    modifier = Modifier.clearAndSetSemantics { }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    strings.settings, fontFamily = AtelierSerif, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 40.sp,
                    color = AtelierInk, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() }
                )
                Spacer(Modifier.height(8.dp))
                Box(Modifier.width(64.dp).height(1.dp).background(AtelierInk.copy(alpha = 0.5f)))
                Spacer(Modifier.height(8.dp))
                Text(
                    strings.settingsSubtitle, fontFamily = AtelierSerif, fontStyle = FontStyle.Italic, fontSize = 15.sp,
                    color = AtelierInk.copy(alpha = 0.72f), textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
internal fun AtelierSettingsGroupLabel(text: String) {
    Column(Modifier.fillMaxWidth().padding(top = AtelierDesign.sectionGap - 8.dp)) {
        AtelierSettingsSectionLabel(text, Modifier.padding(bottom = 12.dp))
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
                Text(title, fontFamily = AtelierSerif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = AtelierDesign.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (summary.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(summary, fontFamily = AtelierSerif, fontStyle = FontStyle.Italic, fontSize = 13.sp, color = AtelierDesign.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.width(12.dp))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = AtelierBrass.copy(alpha = 0.8f), modifier = Modifier.size(22.dp))
        }
        if (last) {
            Spacer(Modifier.height(4.dp))
        } else {
            Box(
                Modifier.padding(start = AtelierSettingsPanelPadding + AtelierSettingsKeySize + 16.dp, end = AtelierSettingsPanelPadding)
                    .fillMaxWidth().height(1.dp).background(AtelierBrass.copy(alpha = 0.14f))
            )
        }
    }
}

@Composable
internal fun AtelierSettingsKey(icon: ImageVector) {
    Box(
        Modifier.size(AtelierSettingsKeySize).clip(CircleShape).background(AtelierSettingsKeyFill)
            .border(1.5.dp, AtelierBrass.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = AtelierBrass, modifier = Modifier.size(19.dp))
    }
}

@Composable
internal fun AtelierSettingsSearchField(query: String, placeholder: String, clearLabel: String, onQuery: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 18.dp).heightIn(min = 54.dp)
            .clip(AtelierDesign.cardShape)
            .background(AtelierSettingsPanelFill)
            .border(1.dp, AtelierBrass.copy(alpha = 0.45f), AtelierDesign.cardShape)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, null, tint = AtelierBrass, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    placeholder, fontFamily = AtelierSerif, fontStyle = FontStyle.Italic, fontSize = 16.sp, color = AtelierDesign.textFaint,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.clearAndSetSemantics { }
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = TextStyle(color = AtelierDesign.text, fontFamily = AtelierSerif, fontSize = 16.sp),
                cursorBrush = SolidColor(AtelierBrass),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = placeholder }
            )
        }
        if (query.isNotEmpty()) AtelierIconTarget(Icons.Rounded.Close, clearLabel, AtelierDesign.textSecondary, { onQuery("") })
    }
}

@Composable
internal fun AtelierStudioShelf(title: String, subtitle: String, entries: List<SettingsStudioEntry>) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Text(
            title, fontFamily = AtelierSerif, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = AtelierDesign.text,
            modifier = Modifier.padding(start = 4.dp).semantics { heading() }
        )
        Text(
            subtitle, fontFamily = AtelierSerif, fontStyle = FontStyle.Italic, fontSize = 14.sp, color = AtelierDesign.textSecondary,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 14.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
                Column(
                    Modifier.width(140.dp).heightIn(min = 132.dp)
                        .graphicsLayer {
                            rotationZ = if (index % 2 == 0) -1.2f else 1f
                            shadowElevation = 8.dp.toPx()
                        }
                        .background(AtelierPaper)
                        .clickable(role = Role.Button, onClick = entry.onClick)
                        .padding(6.dp)
                        .border(0.8.dp, AtelierInk.copy(alpha = 0.5f))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(AtelierSettingsKeyFill)
                            .border(1.5.dp, AtelierBrass, CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Icon(entry.icon, null, tint = AtelierBrass, modifier = Modifier.size(20.dp)) }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        entry.title, fontFamily = AtelierSerif, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 17.sp,
                        color = AtelierInk, textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
internal fun AtelierSettingsSectionLabel(text: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("§", fontFamily = AtelierSerif, fontSize = 20.sp, color = AtelierBrass, modifier = Modifier.clearAndSetSemantics { })
        Spacer(Modifier.width(10.dp))
        Text(
            atelierUppercaseLabel(text),
            fontFamily = AtelierSerif, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 2.4.sp,
            color = AtelierBrass, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f).height(1.dp).background(AtelierBrass.copy(alpha = 0.35f)))
    }
}

@Composable
internal fun AtelierSettingsDetailHeader(title: String, summary: String, onBack: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Column(Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
        AtelierIconTarget(Icons.AutoMirrored.Rounded.ArrowBack, strings.back, AtelierDesign.text, onBack)
        Spacer(Modifier.height(10.dp))
        Text(
            title, fontFamily = AtelierSerif, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 38.sp,
            color = AtelierDesign.text, modifier = Modifier.semantics { heading() }
        )
        if (summary.isNotBlank()) {
            Text(
                summary, fontFamily = AtelierSerif, fontStyle = FontStyle.Italic, fontSize = 15.sp,
                color = AtelierDesign.textSecondary, modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(36.dp).height(1.dp).background(AtelierBrass))
            Text(" ✦ ", fontFamily = AtelierSerif, fontSize = 12.sp, color = AtelierBrass, modifier = Modifier.clearAndSetSemantics { })
            Box(Modifier.width(36.dp).height(1.dp).background(AtelierBrass))
        }
    }
}

@Composable
internal fun AtelierSettingsSwitch(checked: Boolean) {
    val travel by animateFloatAsState(if (checked) 1f else 0f, label = "atelier-settings-switch")
    Box(
        Modifier.size(width = 56.dp, height = 30.dp).clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color(0xFF0B0B0D), Color(0xFF232328))))
            .border(1.5.dp, AtelierBrass.copy(alpha = if (checked) 0.9f else 0.45f), CircleShape)
            .clearAndSetSemantics { }
    ) {
        Box(
            Modifier.align(Alignment.CenterEnd).padding(end = 9.dp).size(6.dp).clip(CircleShape)
                .background(Color(0xFFD2452F).copy(alpha = 0.2f + 0.8f * travel))
        )
        Box(
            Modifier.align(Alignment.CenterStart)
                .graphicsLayer { translationX = (4.dp + 26.dp * travel).toPx() }
                .size(22.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFFEDEDF2), AtelierBrass, Color(0xFF2A2A30))))
                .border(1.dp, Color.Black.copy(alpha = 0.35f), CircleShape)
        )
    }
}

@Composable
internal fun AtelierSettingsChip(label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.heightIn(min = 48.dp)
            .graphicsLayer { translationY = if (active) 2.dp.toPx() else 0f }
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) Color(0xFFCFC2A6) else AtelierPaper)
            .border(1.dp, AtelierInk.copy(alpha = if (active) 0.6f else 0.25f), RoundedCornerShape(6.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { selected = active }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.width(14.dp).height(2.dp).clip(RoundedCornerShape(1.dp))
                .background(if (active) Color(0xFFD2452F) else AtelierInk.copy(alpha = 0.15f))
        )
        Spacer(Modifier.height(4.dp))
        Text(label, fontFamily = AtelierSerif, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AtelierInk)
    }
}
