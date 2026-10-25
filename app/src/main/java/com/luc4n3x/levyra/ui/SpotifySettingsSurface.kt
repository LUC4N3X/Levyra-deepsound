package com.luc4n3x.levyra.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.components.LevyraBackButton
import com.luc4n3x.levyra.ui.components.LevyraSectionPlayAll
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraSpotifySkinStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.SpotifyDesign

@Composable
internal fun SpotifySettingsHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        LevyraBackButton(onClick = onBack, contentDescription = LocalLevyraStrings.current.back)
        Text(title, style = MaterialTheme.typography.titleLarge, color = SpotifyDesign.text,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp).semantics { heading() })
    }
}

@Composable
internal fun SpotifySettingsCategoryRow(title: String, summary: String, icon: ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp)
        .levyraPressable(onClick = onClick, onClickLabel = title, role = Role.Button)
        .padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(icon, null, tint = SpotifyDesign.text, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = SpotifyDesign.text)
            if (summary.isNotBlank()) Text(summary, style = MaterialTheme.typography.bodySmall, color = SpotifyDesign.subdued)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = SpotifyDesign.subdued, modifier = Modifier.size(24.dp))
    }
}

@Composable
internal fun SpotifyOrbitHeader(onPlayAll: (() -> Unit)?) {
    SpotifySectionTitle(LevyraSpotifySkinStrings.orbitTitle, 0.dp) {
        if (onPlayAll != null) LevyraSectionPlayAll(onClick = onPlayAll)
    }
}

@Composable
internal fun SpotifySettingsStudioShelf(title: String, entries: List<SettingsStudioEntry>) {
    Column(Modifier.fillMaxWidth()) {
        SpotifySettingsHeading(title)
        entries.forEach { entry ->
            SpotifySettingsCategoryRow(entry.title, "", entry.icon, entry.onClick)
        }
    }
}
