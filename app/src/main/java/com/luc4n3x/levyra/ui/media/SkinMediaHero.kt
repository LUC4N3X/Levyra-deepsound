package com.luc4n3x.levyra.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.theme.*

private val TorqueMediaBand = GenericShape { size, _ ->
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height * 0.62f)
    lineTo(0f, size.height)
    close()
}

internal fun skinMediaColors(): ImmersiveMediaColors? {
    if (!LevyraIsPrism) return null
    val p = LevyraActivePalette
    return ImmersiveMediaColors(
        fieldTop = p.black, fieldMid = p.black, base = p.black,
        content = p.text, contentMuted = p.muted, accent = p.cyan,
        actionStart = p.blue, actionEnd = if (LevyraIsTorque) p.blue else p.violet, actionContent = Color.White,
        secondaryFill = p.panel, hairline = p.outline
    )
}

@Composable
private fun MediaSubtitle(text: String, color: Color, onClick: (() -> Unit)?, align: TextAlign = TextAlign.Start, uppercase: Boolean = false) {
    if (text.isBlank()) return
    Text(
        if (uppercase) text.uppercase() else text, color = color, style = MaterialTheme.typography.titleMedium, textAlign = align,
        letterSpacing = if (uppercase) 1.2.sp else MaterialTheme.typography.titleMedium.letterSpacing,
        maxLines = 2, overflow = TextOverflow.Ellipsis,
        modifier = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick).padding(vertical = 4.dp) else Modifier
    )
}

@Composable
internal fun PrismMediaHero(
    title: String, overline: String, subtitle: String, metadata: String, colors: ImmersiveMediaColors,
    viewportWidth: Dp, topBarHeight: Dp, onSubtitleClick: (() -> Unit)?, actions: @Composable () -> Unit,
    artwork: @Composable BoxScope.() -> Unit, modifier: Modifier
) {
    Column(modifier.fillMaxWidth().padding(top = topBarHeight, start = 24.dp, end = 24.dp, bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val art = min(viewportWidth * 0.72f, 300.dp)
        Box(Modifier.size(art).clip(RoundedCornerShape(PrismDesign.artworkCorner))) { artwork() }
        Box(Modifier.padding(top = 6.dp).width(art * 0.6f).height(2.dp).background(PrismAccent.gradient(), RoundedCornerShape(1.dp)))
        Spacer(Modifier.height(20.dp))
        if (overline.isNotBlank()) Text(overline.uppercase(), color = colors.accent, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.6.sp, maxLines = 1)
        Text(title, color = colors.content, fontSize = 30.sp, lineHeight = 34.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.8).sp, textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis)
        MediaSubtitle(subtitle, colors.content.copy(alpha = 0.86f), onSubtitleClick, align = TextAlign.Center)
        if (metadata.isNotBlank()) Text(metadata, color = colors.contentMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(18.dp))
        actions()
    }
}

@Composable
internal fun TorqueMediaHero(
    title: String, overline: String, subtitle: String, metadata: String, colors: ImmersiveMediaColors,
    viewportWidth: Dp, onSubtitleClick: (() -> Unit)?, actions: @Composable () -> Unit,
    artwork: @Composable BoxScope.() -> Unit, modifier: Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(min(viewportWidth, 420.dp))) { artwork() }
        Box(Modifier.fillMaxWidth().height(150.dp)) {
            Box(Modifier.matchParentSize().clip(TorqueMediaBand).background(LevyraActivePalette.blue))
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (overline.isNotBlank()) Text(overline.uppercase(), color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp, maxLines = 1)
                Text(title.uppercase(), color = Color.White, fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                MediaSubtitle(subtitle, Color.White.copy(alpha = 0.92f), onSubtitleClick, align = TextAlign.Center)
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (metadata.isNotBlank()) Text(metadata.uppercase(), color = colors.contentMuted, fontSize = 12.sp, letterSpacing = 1.4.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            actions()
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
internal fun NocturneMediaHero(
    title: String, overline: String, subtitle: String, metadata: String, colors: ImmersiveMediaColors,
    topBarHeight: Dp, onSubtitleClick: (() -> Unit)?, actions: @Composable () -> Unit,
    artwork: @Composable BoxScope.() -> Unit, modifier: Modifier
) {
    val amber = colors.accent
    val frame = RoundedCornerShape(Nocturne78Design.artworkCorner + 4.dp)
    val readout = RoundedCornerShape(Nocturne78Design.controlCorner)
    Column(modifier.fillMaxWidth().padding(top = topBarHeight, start = 20.dp, end = 20.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).border(1.dp, amber.copy(alpha = 0.45f), frame)
                .padding(10.dp).clip(RoundedCornerShape(Nocturne78Design.artworkCorner))
        ) { artwork() }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (overline.isNotBlank()) Text(overline.uppercase(), color = amber, fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 2.4.sp, maxLines = 1)
            Text(title, color = colors.content, fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Medium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            MediaSubtitle(subtitle, colors.contentMuted, onSubtitleClick, uppercase = true)
        }
        if (metadata.isNotBlank()) {
            Box(
                Modifier.fillMaxWidth().clip(readout).background(Brush.verticalGradient(listOf(LevyraPanelSoft, LevyraPanel)))
                    .border(1.dp, colors.hairline, readout).padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(metadata.uppercase(), color = amber, fontFamily = FontFamily.Monospace, fontSize = 12.sp, letterSpacing = 1.2.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        actions()
    }
}
