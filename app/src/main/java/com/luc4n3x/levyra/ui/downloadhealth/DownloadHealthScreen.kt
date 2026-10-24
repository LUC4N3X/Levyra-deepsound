@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.luc4n3x.levyra.ui.downloadhealth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luc4n3x.levyra.data.DownloadHealthResult
import com.luc4n3x.levyra.data.local.OfflineDownloadTaskSummaryRow
import com.luc4n3x.levyra.domain.DownloadHealth
import com.luc4n3x.levyra.ui.experiences.LocalMusicExperiences
import com.luc4n3x.levyra.ui.experiences.MusicExperienceStyle
import com.luc4n3x.levyra.ui.experiences.musicExperienceStyle
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.MusicExperienceText
import com.luc4n3x.levyra.ui.i18n.musicExperience
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun DownloadHealthEntry() {
    val navigation = LocalMusicExperiences.current
    val strings = LocalLevyraStrings.current
    val style = musicExperienceStyle()
    Surface(onClick = navigation.health, color = style.surface, contentColor = style.text,
        shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().testTag("open-download-health")) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.HealthAndSafety, null, tint = style.accent)
            Column(Modifier.weight(1f)) {
                Text(strings.musicExperience(MusicExperienceText.HealthTitle), style = MaterialTheme.typography.titleMedium)
                Text(strings.musicExperience(MusicExperienceText.HealthSubtitle), style = MaterialTheme.typography.bodyMedium, color = style.muted)
            }
        }
    }
}

@Composable
internal fun DownloadHealthScreen(onClose: () -> Unit, onSettings: () -> Unit, viewModel: DownloadHealthViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DisposableEffect(viewModel) { onDispose { viewModel.cancelScan() } }
    DownloadHealthPresentation(state, onClose, onSettings,
        HealthActions(viewModel::scan, viewModel::cancelScan, viewModel::repair, { viewModel.cancel(it) }, { viewModel.retry(it) }))
}

internal data class HealthActions(
    val scan: () -> Unit,
    val cancelScan: () -> Unit,
    val repair: (List<DownloadHealthResult>) -> Unit,
    val cancel: (String) -> Unit,
    val retry: (String) -> Unit
)

@Composable
internal fun DownloadHealthPresentation(state: DownloadHealthState, onClose: () -> Unit, onSettings: () -> Unit, actions: HealthActions) {
    val strings = LocalLevyraStrings.current
    val style = musicExperienceStyle()
    val number = remember(strings.code) { NumberFormat.getIntegerInstance(Locale.forLanguageTag(strings.code)) }
    var issuesOnly by rememberSaveable { mutableStateOf(true) }
    var repairIds by rememberSaveable { mutableStateOf<List<Long>>(emptyList()) }
    val healthy = state.results.count { it.health == DownloadHealth.Healthy }
    val issues = state.results.size - healthy
    val visible = remember(state.results, issuesOnly) { state.results.filter { !issuesOnly || it.health != DownloadHealth.Healthy } }
    BackHandler(onBack = onClose)
    Surface(color = style.background, contentColor = style.text, modifier = Modifier.fillMaxSize().testTag("download-health-screen")) {
        LazyColumn(contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp),
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item(key = "header") {
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, strings.back) }
                    Text(strings.downloads, style = MaterialTheme.typography.titleMedium, color = style.muted)
                }
                Text(strings.musicExperience(MusicExperienceText.HealthTitle), style = MaterialTheme.typography.displaySmallEmphasized, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(8.dp))
                Text(strings.musicExperience(MusicExperienceText.HealthSubtitle), style = MaterialTheme.typography.titleLarge, color = style.muted)
            }
            item(key = "health-orbit") {
                HealthOverview(state, healthy, style, number, strings)
            }
            item(key = "actions") { HealthScanControls(state, style, number, strings, actions) }
            if (state.tasks.isNotEmpty()) {
                item(key = "tasks-heading") { Text(strings.downloadsInProgress, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() }) }
                items(state.tasks, key = { "task:${it.taskKey}" }) { task -> HealthTaskRow(task, style, strings, actions) }
            }
            if (state.finished) {
                item(key = "filters") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = issuesOnly, onClick = { issuesOnly = true }, label = { Text("${strings.musicExperience(MusicExperienceText.Issues)} · ${number.format(issues)}") })
                        FilterChip(selected = !issuesOnly, onClick = { issuesOnly = false }, label = { Text("${strings.downloads} · ${number.format(state.results.size)}") })
                    }
                    val repairable = state.results.filter { it.repairable }
                    if (repairable.isNotEmpty()) FilledTonalButton(onClick = { repairIds = repairable.map { it.download.id } }, enabled = !state.scanning && !state.repairing) {
                        Text("${strings.musicExperience(MusicExperienceText.Repair)} · ${number.format(repairable.size)}")
                    }
                }
                if (state.results.isEmpty()) item(key = "empty") {
                    Text(strings.noOfflineDownloads, style = MaterialTheme.typography.titleLarge)
                }
                items(visible, key = { "download:${it.download.id}" }) { result ->
                    HealthResultRow(result, style, strings, enabled = !state.scanning && !state.repairing,
                        onRepair = { repairIds = listOf(result.download.id) }, onSettings = onSettings)
                }
            }
        }
    }
    if (repairIds.isNotEmpty()) {
        HealthRepairDialog(strings, onDismiss = { repairIds = emptyList() },
            onConfirm = { actions.repair(state.results.filter { it.download.id in repairIds }); repairIds = emptyList() })
    }
}

@Composable
private fun HealthScanControls(state: DownloadHealthState, style: MusicExperienceStyle, number: NumberFormat, strings: LevyraStrings, actions: HealthActions) {
    Column {
        if (state.scanning) {
            LinearWavyProgressIndicator(progress = { if (state.total > 0) state.scanned.toFloat() / state.total else 0f }, modifier = Modifier.fillMaxWidth(), color = style.accent)
            Text("${number.format(state.scanned)} / ${number.format(state.total)}", color = style.muted)
            TextButton(onClick = actions.cancelScan) { Text(strings.cancel) }
        } else {
            Button(onClick = actions.scan, shapes = if (style.expressive) ButtonDefaults.shapes() else ButtonDefaults.shapes(shape = style.shape, pressedShape = style.shape), modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = style.accent, contentColor = style.onAccent)) {
                Text(strings.musicExperience(MusicExperienceText.Scan))
            }
        }
        if (state.failed) Text(strings.operationFailed, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        Text(strings.musicExperience(MusicExperienceText.ScanHint), style = MaterialTheme.typography.bodySmall, color = style.muted, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun HealthTaskRow(task: OfflineDownloadTaskSummaryRow, style: MusicExperienceStyle, strings: LevyraStrings, actions: HealthActions) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(task.title, style = MaterialTheme.typography.titleMedium)
        Text(if (task.state == "FAILED") strings.updateFailed else strings.downloadInProgress, color = style.muted)
        LinearProgressIndicator(progress = { task.progress / 100f }, modifier = Modifier.fillMaxWidth(), color = style.accent)
        Row {
            if (task.state in setOf("FAILED", "PAUSED")) TextButton(onClick = { actions.retry(task.taskKey) }) { Text(strings.resumeDownload) }
            TextButton(onClick = { actions.cancel(task.taskKey) }) { Text(strings.cancelDownload) }
        }
    }
}

@Composable
private fun HealthRepairDialog(strings: LevyraStrings, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(strings.musicExperience(MusicExperienceText.Repair)) },
        text = { Text(strings.musicExperience(MusicExperienceText.RepairBody)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(strings.musicExperience(MusicExperienceText.Repair)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } })
}

@Composable
private fun HealthOverview(state: DownloadHealthState, healthy: Int, style: MusicExperienceStyle, number: NumberFormat, strings: LevyraStrings) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Surface(color = style.accent, contentColor = style.onAccent,
            shape = if (style.expressive) MaterialShapes.Clover4Leaf.toShape() else style.shape,
            modifier = Modifier.size(maxWidth.coerceAtMost(280.dp))) {
            Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                if (state.scanning) LoadingIndicator(color = style.onAccent, modifier = Modifier.size(64.dp))
                else Icon(if (state.finished && healthy == state.results.size && healthy > 0) Icons.Rounded.Check else Icons.Rounded.DownloadDone, null, modifier = Modifier.size(40.dp))
                Text(if (state.finished) number.format(healthy) else "—", style = MaterialTheme.typography.displayLargeEmphasized, fontSize = 72.sp)
                Text(strings.diagnosticsStatusHealthy, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun HealthResultRow(result: DownloadHealthResult, style: MusicExperienceStyle, strings: LevyraStrings, enabled: Boolean, onRepair: () -> Unit, onSettings: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(12.dp).background(if (result.health == DownloadHealth.Healthy) style.accent else style.muted, CircleShape))
            Column(Modifier.weight(1f)) {
                Text(result.download.title, style = MaterialTheme.typography.titleMedium)
                Text(result.download.artist, color = style.muted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text(if (result.health == DownloadHealth.Healthy) strings.diagnosticsStatusHealthy else strings.musicExperience(when (result.health) {
            DownloadHealth.Missing -> MusicExperienceText.Missing
            DownloadHealth.Empty -> MusicExperienceText.Empty
            DownloadHealth.Unreadable -> MusicExperienceText.Unreadable
            DownloadHealth.Incomplete -> MusicExperienceText.Incomplete
            DownloadHealth.AccessDenied -> MusicExperienceText.Permission
            else -> MusicExperienceText.Unavailable
        }), color = style.muted, style = MaterialTheme.typography.bodyMedium)
        if (result.repairable) TextButton(onClick = onRepair, enabled = enabled) { Text(strings.musicExperience(MusicExperienceText.Repair)) }
        if (result.health == DownloadHealth.AccessDenied) TextButton(onClick = onSettings) { Text(strings.settings) }
        HorizontalDivider(color = style.text.copy(alpha = 0.12f))
    }
}
