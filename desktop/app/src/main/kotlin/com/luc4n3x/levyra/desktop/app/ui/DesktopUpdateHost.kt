package com.luc4n3x.levyra.desktop.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.generated.resources.Res
import com.luc4n3x.levyra.desktop.app.generated.resources.onboarding_levyra_brand
import com.luc4n3x.levyra.desktop.app.state.DesktopUpdateController
import com.luc4n3x.levyra.desktop.app.state.DesktopUpdatePhase
import com.luc4n3x.levyra.desktop.app.state.DesktopUpdateUiState
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.ui.icons.OfflineIcons
import com.luc4n3x.levyra.desktop.app.ui.theme.LevyraTheme
import com.luc4n3x.levyra.desktop.core.model.AppLanguage
import com.luc4n3x.levyra.desktop.core.model.ThemeMode
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.updateReleaseHighlights
import com.luc4n3x.levyra.ui.update.LevyraReleaseContent
import com.luc4n3x.levyra.ui.update.ReleasePalette
import com.luc4n3x.levyra.ui.update.updateMetaLine
import com.luc4n3x.levyra.update.formatUpdateTransferLine
import com.luc4n3x.levyra.update.releaseNoteHighlights
import com.luc4n3x.levyra.update.updateUnitsFor
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun DesktopUpdateHost(
    controller: DesktopUpdateController,
    language: AppLanguage,
    themeMode: ThemeMode,
    enabled: Boolean,
    onInstallReady: () -> Unit,
    content: @Composable () -> Unit
) {
    val state by controller.state.collectAsState()
    LaunchedEffect(controller, enabled) {
        if (enabled) controller.check()
    }

    if (enabled && state.release != null && state.phase !in setOf(DesktopUpdatePhase.IDLE, DesktopUpdatePhase.CHECKING)) {
        LevyraTheme(themeMode) {
            DesktopUpdateScreen(
                state,
                language,
                onUpdate = { controller.install(onInstallReady) },
                onLater = controller::dismiss
            )
        }
    } else {
        content()
    }
}

@Composable
internal fun DesktopUpdateScreen(
    state: DesktopUpdateUiState,
    language: AppLanguage,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    animated: Boolean = true
) {
    val release = state.release ?: return
    val strings = remember(language) { LevyraStrings.forCode(language.tag) }
    val notes = remember(strings, release) {
        strings.updateReleaseHighlights(release.version, releaseNoteHighlights(release.notes)).take(5)
    }
    val meta = remember(release, language) { updateMetaLine(release.publishedAtEpochMs, release.installer.size, language.tag) }
    val colors = MaterialTheme.colorScheme
    val footer: (@Composable () -> Unit)? = if (state.phase == DesktopUpdatePhase.AVAILABLE) {
        null
    } else {
        { DesktopUpdateStatus(state, strings, onUpdate, onLater) }
    }

    LevyraReleaseContent(
        version = release.version,
        notes = notes,
        meta = meta,
        strings = strings,
        animated = animated,
        palette = ReleasePalette(
            background = colors.surfaceDim,
            accent = colors.primary,
            onAccent = colors.onPrimary,
            text = colors.onSurface,
            muted = colors.onSurfaceVariant,
            panel = colors.surfaceContainerHigh,
            border = colors.outlineVariant
        ),
        protectionDetail = "GitHub · SHA-256",
        onUpdate = onUpdate,
        onLater = onLater,
        brandArt = {
            Image(
                painterResource(Res.drawable.onboarding_levyra_brand),
                contentDescription = "Levyra Deepsound",
                modifier = it,
                contentScale = ContentScale.Fit
            )
        },
        backButton = {
            FilledTonalIconButton(onClick = onLater, modifier = it) {
                Icon(LevyraIcons.ChevronLeft, contentDescription = strings.later)
            }
        },
        updateIcon = { Icon(OfflineIcons.Download, contentDescription = null, modifier = it) },
        protectionIcon = { Icon(OfflineIcons.Check, contentDescription = null, tint = colors.primary, modifier = it) },
        maxContentWidth = 680.dp,
        statusFooter = footer
    )
}

@Composable
private fun DesktopUpdateStatus(
    state: DesktopUpdateUiState,
    strings: LevyraStrings,
    onUpdate: () -> Unit,
    onLater: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (state.phase) {
            DesktopUpdatePhase.DOWNLOADING -> {
                Text(strings.updateDownloading, color = colors.onSurface, style = MaterialTheme.typography.titleMedium)
                if (state.totalBytes > 0L) {
                    LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                Text(
                    formatUpdateTransferLine(state.bytesDownloaded, state.totalBytes, null, updateUnitsFor(strings.code)),
                    color = colors.onSurfaceVariant
                )
            }
            DesktopUpdatePhase.INSTALLING -> {
                Text(strings.updateInstalling, color = colors.onSurface, style = MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            DesktopUpdatePhase.FAILED -> {
                Text(strings.updateFailed, color = colors.error, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = state.error.ifBlank { strings.updateLinkUnavailable },
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onUpdate) { Text(strings.updateRetry) }
                    TextButton(onClick = onLater) { Text(strings.later) }
                }
            }
            else -> Unit
        }
    }
}
