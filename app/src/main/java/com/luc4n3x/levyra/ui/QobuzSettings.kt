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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.QobuzDesign

private val QobuzSettingsCorner = 16.dp

@Composable
internal fun QobuzSettingsHeader(onClose: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = strings.close, onClick = onClose),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, strings.close, tint = QobuzDesign.iconPrimary, modifier = Modifier.size(26.dp)) }
        }
        Box(Modifier.size(80.dp).clip(CircleShape).background(QobuzDesign.surfaceSecondary), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Tune, null, tint = QobuzDesign.textPrimary, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(strings.settings, style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold), color = QobuzDesign.textPrimary,
            textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
        Text(strings.settingsSubtitle, style = MaterialTheme.typography.bodyMedium, color = QobuzDesign.textSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
internal fun QobuzSettingsGroupLabel(text: String) {
    Text(
        text.uppercase(), style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp),
        color = QobuzDesign.textSecondary,
        modifier = Modifier.padding(top = 32.dp).fillMaxWidth()
            .clip(RoundedCornerShape(topStart = QobuzSettingsCorner, topEnd = QobuzSettingsCorner))
            .background(QobuzDesign.surfaceSecondary).padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)
            .semantics { heading() }
    )
}

@Composable
internal fun QobuzSettingsRow(title: String, icon: ImageVector, last: Boolean, onClick: () -> Unit) {
    val shape = if (last) RoundedCornerShape(bottomStart = QobuzSettingsCorner, bottomEnd = QobuzSettingsCorner) else RoundedCornerShape(0.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(QobuzDesign.surfaceSecondary).clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 60.dp).padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = if (last) 16.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = QobuzDesign.iconPrimary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(24.dp))
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium), color = QobuzDesign.textPrimary,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = QobuzDesign.iconSecondary, modifier = Modifier.size(22.dp))
    }
}
