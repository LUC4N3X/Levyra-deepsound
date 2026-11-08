package com.luc4n3x.levyra.diagnostics

import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Description
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.luc4n3x.levyra.ui.SettingsButton
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.supportCopy
import com.luc4n3x.levyra.viewmodel.LevyraUiState

@Composable
internal fun CrashReportsSettingsEntry() {
    val words = supportCopy(LocalLevyraStrings.current.code)
    val context = LocalContext.current
    SettingsButton(Icons.Rounded.Description, words.reports, words.reportsSubtitle) {
        context.startActivity(Intent(context, CrashReportsActivity::class.java)
            .putExtra(CrashReportsActivity.EXTRA_PRESENTATION, CrashReporting.presentation.encode()))
    }
}

@Composable
internal fun DiagnosticContextEffect(state: LevyraUiState) {
    val screen = when {
        state.showSettings -> "Settings"
        state.showLyrics -> "Lyrics"
        state.showQueue -> "Queue"
        state.showArtist -> "Artist"
        state.showAlbum -> "Album"
        else -> state.selectedTab.name
    }
    val skin = state.interfaceSettings.skin.name
    val palette = com.luc4n3x.levyra.ui.theme.LevyraActivePalette
    val presentation = remember(state.languageCode, state.interfaceSettings.fontPreset, state.animationsEnabled, palette) {
        CrashPresentation(state.languageCode, state.interfaceSettings.fontPreset.name, state.animationsEnabled, palette.isLight,
            listOf(palette.black, palette.ink, palette.panel, palette.panelSoft, palette.cyan, palette.blue, palette.violet,
                palette.pink, palette.orange, palette.text, palette.muted, palette.outline).map { it.toArgb() })
    }
    LaunchedEffect(screen, skin, presentation) { CrashReporting.updateContext(screen, skin, presentation) }
}
