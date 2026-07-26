package com.luc4n3x.levyra.desktop.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.state.DesktopUpdateController
import com.luc4n3x.levyra.desktop.app.state.DesktopUpdatePhase
import com.luc4n3x.levyra.desktop.core.model.AppLanguage
import com.luc4n3x.levyra.ui.i18n.LevyraStrings as SharedLevyraStrings

@Composable
internal fun DesktopUpdateDialogHost(
    controller: DesktopUpdateController,
    language: AppLanguage,
    enabled: Boolean,
    onInstallReady: () -> Unit
) {
    val state by controller.state.collectAsState()
    val strings = SharedLevyraStrings.forCode(language.tag)

    LaunchedEffect(controller, enabled) {
        if (enabled) controller.check()
    }

    when (state.phase) {
        DesktopUpdatePhase.AVAILABLE -> {
            val release = state.release ?: return
            AlertDialog(
                onDismissRequest = controller::dismiss,
                title = { Text("${strings.newUpdate} · ${release.version}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(strings.updateDescription)
                        Text(
                            text = release.name,
                            style = MaterialTheme.typography.titleSmall
                        )
                        if (release.notes.isNotBlank()) {
                            Text(
                                text = strings.whatsNew,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = release.notes.take(MAX_RELEASE_NOTES),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 10,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { controller.install(onInstallReady) }) {
                        Text(strings.update)
                    }
                },
                dismissButton = {
                    TextButton(onClick = controller::dismiss) {
                        Text(strings.later)
                    }
                }
            )
        }

        DesktopUpdatePhase.DOWNLOADING -> {
            val release = state.release ?: return
            AlertDialog(
                onDismissRequest = {},
                title = { Text("${strings.download} · ${release.version}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (state.totalBytes > 0L) {
                            LinearProgressIndicator(
                                progress = { state.progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("${(state.progress * 100f).toInt()}%")
                        } else {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                },
                confirmButton = {}
            )
        }

        DesktopUpdatePhase.INSTALLING -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(strings.update) },
                text = { Text(strings.restartNow) },
                confirmButton = {}
            )
        }

        DesktopUpdatePhase.FAILED -> {
            AlertDialog(
                onDismissRequest = controller::dismiss,
                title = { Text(strings.cannotOpenDownload) },
                text = {
                    Text(
                        text = state.error.ifBlank { strings.updateLinkUnavailable },
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                confirmButton = {
                    Button(onClick = { controller.install(onInstallReady) }) {
                        Text(strings.update)
                    }
                },
                dismissButton = {
                    TextButton(onClick = controller::dismiss) {
                        Text(strings.later)
                    }
                }
            )
        }

        DesktopUpdatePhase.IDLE,
        DesktopUpdatePhase.CHECKING -> Unit
    }
}

private const val MAX_RELEASE_NOTES = 1_500
