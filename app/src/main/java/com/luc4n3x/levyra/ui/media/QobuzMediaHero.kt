package com.luc4n3x.levyra.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.player.qobuzPlayerField
import com.luc4n3x.levyra.ui.theme.QobuzDesign

internal fun qobuzMediaColors(cover: Color): ImmersiveMediaColors {
    val field = if (cover.isSpecified) qobuzPlayerField(cover) else Color(0xFF3D3D3D)
    return ImmersiveMediaColors(
        fieldTop = field, fieldMid = field, base = QobuzDesign.surfacePrimary,
        content = Color.White, contentMuted = Color.White.copy(alpha = 0.72f), accent = Color.White,
        actionStart = Color.White, actionEnd = Color.White, actionContent = Color.Black,
        secondaryFill = Color.White.copy(alpha = 0.16f), hairline = QobuzDesign.separator
    )
}

@Composable
internal fun QobuzMediaHero(
    title: String, overline: String, subtitle: String, metadata: String, colors: ImmersiveMediaColors,
    viewportWidth: Dp, topBarHeight: Dp, onSubtitleClick: (() -> Unit)?, actions: @Composable () -> Unit,
    artwork: @Composable BoxScope.() -> Unit, modifier: Modifier
) {
    Column(
        modifier.fillMaxWidth().background(colors.fieldTop).padding(top = topBarHeight, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val art = min(viewportWidth - 96.dp, 250.dp)
        Box(Modifier.padding(top = 16.dp).size(art).clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.2f))) { artwork() }
        Spacer(Modifier.height(32.dp))
        if (overline.isNotBlank()) {
            Text(overline, color = colors.contentMuted, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.height(8.dp))
        }
        Text(
            title, color = colors.content, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center, maxLines = 3, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp).semantics { heading() }
        )
        if (subtitle.isNotBlank()) {
            Text(
                subtitle, color = colors.content, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium),
                textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp)
                    .then(if (onSubtitleClick != null) Modifier.levyraPressable(onClick = onSubtitleClick, role = Role.Button) else Modifier)
            )
        }
        if (metadata.isNotBlank()) {
            Text(metadata, color = colors.contentMuted, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 6.dp))
        }
        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { actions() }
    }
}
