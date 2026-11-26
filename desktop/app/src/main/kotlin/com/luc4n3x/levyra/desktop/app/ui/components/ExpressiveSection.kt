package com.luc4n3x.levyra.desktop.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

fun scallopShape(lobes: Int, depth: Float): Shape = GenericShape { size, _ ->
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    val radius = min(size.width, size.height) / 2f
    val steps = lobes * 24
    for (step in 0..steps) {
        val angle = 2.0 * PI * step / steps - PI / 2
        val r = radius * (1f - depth + depth * cos(lobes * angle).toFloat())
        val x = centerX + r * cos(angle).toFloat()
        val y = centerY + r * sin(angle).toFloat()
        if (step == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

val CookieShape: Shape = scallopShape(lobes = 9, depth = 0.07f)
val SunnyShape: Shape = scallopShape(lobes = 8, depth = 0.045f)

object SectionTints {
    val Lavender = Color(0xFFB9A2F5)
    val Amber = Color(0xFFF4C27A)
    val Sky = Color(0xFF9CCDE8)
    val Rose = Color(0xFFF2A4C4)
    val Periwinkle = Color(0xFF9FB2FF)
    val Mint = Color(0xFF8FD9B6)
}

@Composable
fun ExpressiveSectionHeader(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier.size(44.dp).clip(SunnyShape).background(tint),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF1B1530), modifier = Modifier.size(21.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun SectionPlayButton(description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(LevyraIcons.Play, contentDescription = description, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun SectionShowAll(label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 14.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        Icon(
            LevyraIcons.ChevronLeft,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = 180f }
        )
    }
}
