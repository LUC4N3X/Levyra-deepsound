package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.desktop.app.ui.components.SectionTints
import com.luc4n3x.levyra.desktop.app.ui.components.SunnyShape
import com.luc4n3x.levyra.desktop.app.ui.components.levyraIconPainter
import com.luc4n3x.levyra.desktop.app.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.ui.icons.OfflineIcons
import com.luc4n3x.levyra.ui.i18n.SettingsHubText
import com.luc4n3x.levyra.ui.i18n.settingsHubCopy
import com.luc4n3x.levyra.ui.i18n.LevyraStrings as SharedStrings

internal enum class SettingsPage(val group: SettingsHubText, val icon: ImageVector, val tint: Color) {
    INTERFACE(SettingsHubText.Appearance, LevyraIcons.Settings, SectionTints.Lavender),
    PLAYBACK(SettingsHubText.Listening, LevyraIcons.Play, SectionTints.Rose),
    AUDIO(SettingsHubText.Listening, LevyraIcons.Tune, SectionTints.Periwinkle),
    PROFILE(SettingsHubText.Library, LevyraIcons.Artist, SectionTints.Sky),
    STORAGE(SettingsHubText.Library, OfflineIcons.Folder, SectionTints.Mint),
    VLC(SettingsHubText.System, LevyraIcons.Disc, SectionTints.Amber),
    SHORTCUTS(SettingsHubText.System, LevyraIcons.Menu, SectionTints.Lavender),
    ABOUT(SettingsHubText.System, LevyraIcons.Radio, SectionTints.Amber);

    fun title(strings: LevyraStrings): String = when (this) {
        INTERFACE -> strings.settingsInterface
        PLAYBACK -> strings.settingsPlayback
        AUDIO -> strings.settingsAudio
        PROFILE -> strings.settingsProfile
        STORAGE -> strings.settingsStorage
        VLC -> strings.settingsVlc
        SHORTCUTS -> strings.settingsShortcuts
        ABOUT -> strings.settingsAbout
    }

    fun subtitle(strings: LevyraStrings): String = when (this) {
        INTERFACE -> strings.settingsTheme
        PLAYBACK -> listOf(strings.settingsSpeed, strings.crossfade).joinToString(" · ")
        AUDIO -> listOf(strings.settingsEqualizer, strings.settingsAudioQuality).joinToString(" · ")
        PROFILE -> strings.settingsCountry
        STORAGE -> strings.settingsOpenFolder
        VLC -> strings.settingsVlc
        SHORTCUTS -> strings.settingsShortcuts
        ABOUT -> strings.settingsAbout
    }
}

@Composable
internal fun SettingsHubList(
    strings: LevyraStrings,
    selected: SettingsPage,
    appVersion: String,
    onSelect: (SettingsPage) -> Unit,
    modifier: Modifier = Modifier
) {
    val hub = SharedStrings.forCode(strings.languageCode).settingsHubCopy
    Column(
        modifier = modifier
            .width(400.dp)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(start = 28.dp, end = 12.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(strings.navSettings, style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.SemiBold))
        SettingsPage.entries.groupBy { it.group }.forEach { (group, pages) ->
            Text(
                hub[group],
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, top = 14.dp, bottom = 2.dp)
            )
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                pages.forEach { page -> HubRow(page, strings, page == selected) { onSelect(page) } }
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.size(56.dp).clip(SunnyShape).background(Color(0xFF1C1C22)), contentAlignment = Alignment.Center) {
                Image(levyraIconPainter(), contentDescription = null, modifier = Modifier.size(44.dp))
            }
            Text(
                "LEVYRA $appVersion",
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HubRow(page: SettingsPage, strings: LevyraStrings, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f) else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(page.tint.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
            Icon(page.icon, contentDescription = null, tint = page.tint, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(page.title(strings), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                page.subtitle(strings),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            LevyraIcons.ChevronLeft,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = 180f }
        )
    }
    Box(Modifier.fillMaxWidth().height(1.dp).padding(start = 66.dp).background(MaterialTheme.colorScheme.outlineVariant))
}
