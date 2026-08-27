package com.luc4n3x.levyra.runtime

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.luc4n3x.levyra.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun InternalDiagnosticsOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 96.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        InternalDiagnosticsEntry()
    }
}

@Composable
internal fun InternalDiagnosticsEntry() {
    var open by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { open = true },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Rounded.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(stringResource(R.string.internal_diagnostics_title), fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.internal_diagnostics_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    if (open) InternalDiagnosticsDialog { open = false }
}

@Composable
private fun InternalDiagnosticsDialog(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf(RuntimeDiagnostics.snapshot()) }
    var preflightRunning by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            scope.launch {
                val result = runCatching { RuntimeDiagnosticsExporter.export(context, uri) }
                Toast.makeText(
                    context,
                    if (result.isSuccess) R.string.internal_diagnostics_exported else R.string.internal_diagnostics_export_failed,
                    Toast.LENGTH_LONG
                ).show()
                snapshot = RuntimeDiagnostics.snapshot()
            }
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            snapshot = RuntimeDiagnostics.snapshot()
            delay(2_000L)
        }
    }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.internal_diagnostics_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        IconButton(onClick = onClose) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.internal_diagnostics_close))
                        }
                    }
                }
                item {
                    DiagnosticsCard(
                        listOf(
                            stringResource(R.string.internal_diagnostics_status) to stringResource(
                                if (snapshot.active) R.string.internal_diagnostics_active else R.string.internal_diagnostics_stopped
                            ),
                            stringResource(R.string.internal_diagnostics_events) to snapshot.eventCount.toString(),
                            stringResource(R.string.internal_diagnostics_anomalies) to snapshot.anomalies.size.toString(),
                            stringResource(R.string.internal_diagnostics_current_memory) to snapshot.currentMemory?.pssKb?.let(::formatKb).orEmpty(),
                            stringResource(R.string.internal_diagnostics_peak_memory) to snapshot.currentMemory?.peakPssKb?.let(::formatKb).orEmpty(),
                            stringResource(R.string.internal_diagnostics_player_state) to playerStateLabel(snapshot.playerState),
                            stringResource(R.string.internal_diagnostics_resolver_state) to resolverLabel(snapshot.resolverState),
                            stringResource(R.string.internal_diagnostics_preflight_status) to (
                                snapshot.preflight?.status?.let { preflightStatusLabel(it) }
                                    ?: stringResource(R.string.internal_diagnostics_not_run)
                            )
                        )
                    )
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            modifier = Modifier.weight(1f),
                            enabled = !preflightRunning,
                            onClick = {
                                preflightRunning = true
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) { RuntimeDiagnostics.runPreflight(context) }
                                        snapshot = RuntimeDiagnostics.snapshot()
                                    } finally {
                                        preflightRunning = false
                                    }
                                }
                            }
                        ) {
                            Text(stringResource(R.string.internal_diagnostics_run_preflight))
                        }
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = { exportLauncher.launch("levyra-diagnostics-${System.currentTimeMillis()}.zip") }
                        ) {
                            Text(stringResource(R.string.internal_diagnostics_export))
                        }
                    }
                }
                item {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            RuntimeDiagnostics.reset()
                            snapshot = RuntimeDiagnostics.snapshot()
                        }
                    ) {
                        Text(stringResource(R.string.internal_diagnostics_reset))
                    }
                }
                if (snapshot.preflight != null) {
                    item { Text(stringResource(R.string.internal_diagnostics_preflight), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                    items(snapshot.preflight!!.results, key = { it.checkId }) { result ->
                        DiagnosticsCard(
                            listOf(
                                result.checkId to preflightStatusLabel(result.status),
                                result.component to result.message,
                                stringResource(R.string.internal_diagnostics_details) to result.details.orEmpty()
                            )
                        )
                    }
                }
                item { Text(stringResource(R.string.internal_diagnostics_recent_anomalies), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                if (snapshot.anomalies.isEmpty()) {
                    item { Text(stringResource(R.string.internal_diagnostics_no_anomalies), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(snapshot.anomalies.takeLast(12).reversed(), key = { "${it.timestampMs}:${it.type}:${it.operation}" }) { anomaly ->
                        DiagnosticsCard(
                            listOf(
                                anomalyTypeLabel(anomaly.type) to anomalySeverityLabel(anomaly.severity),
                                stringResource(R.string.internal_diagnostics_occurrences) to anomaly.occurrenceCount.toString(),
                                stringResource(R.string.internal_diagnostics_window_ms) to anomaly.windowMs.toString()
                            )
                        )
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun DiagnosticsCard(rows: List<Pair<String, String>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        rows.filter { it.second.isNotBlank() }.forEach { (label, value) ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            }
        }
    }
}

private fun formatKb(value: Long): String = "${value / 1024L} MiB"

@Composable
private fun playerStateLabel(value: DiagnosticPlayerState): String = stringResource(
    when (value) {
        DiagnosticPlayerState.IDLE -> R.string.internal_diagnostics_state_idle
        DiagnosticPlayerState.BUFFERING -> R.string.internal_diagnostics_state_buffering
        DiagnosticPlayerState.READY -> R.string.internal_diagnostics_state_ready
        DiagnosticPlayerState.ENDED -> R.string.internal_diagnostics_state_ended
        DiagnosticPlayerState.UNKNOWN -> R.string.internal_diagnostics_state_unknown
    }
)

@Composable
private fun preflightStatusLabel(value: PreflightStatus): String = stringResource(
    when (value) {
        PreflightStatus.PASS -> R.string.internal_diagnostics_status_pass
        PreflightStatus.WARNING -> R.string.internal_diagnostics_status_warning
        PreflightStatus.FAIL -> R.string.internal_diagnostics_status_fail
    }
)

@Composable
private fun anomalySeverityLabel(value: AnomalySeverity): String = stringResource(
    when (value) {
        AnomalySeverity.INFO -> R.string.internal_diagnostics_severity_info
        AnomalySeverity.WARNING -> R.string.internal_diagnostics_severity_warning
        AnomalySeverity.CRITICAL -> R.string.internal_diagnostics_severity_critical
    }
)

@Composable
private fun anomalyTypeLabel(value: AnomalyType): String = stringResource(
    when (value) {
        AnomalyType.MEMORY_GROWTH -> R.string.internal_diagnostics_anomaly_memory_growth
        AnomalyType.PLAYER_RECREATION_STORM -> R.string.internal_diagnostics_anomaly_player_recreation
        AnomalyType.PREPARE_LOOP -> R.string.internal_diagnostics_anomaly_prepare_loop
        AnomalyType.BUFFERING_LOOP -> R.string.internal_diagnostics_anomaly_buffering_loop
        AnomalyType.RESOLVER_RETRY_STORM -> R.string.internal_diagnostics_anomaly_resolver_retry
        AnomalyType.FALLBACK_LOOP -> R.string.internal_diagnostics_anomaly_fallback_loop
        AnomalyType.NETWORK_RETRY_STORM -> R.string.internal_diagnostics_anomaly_network_retry
        AnomalyType.CACHE_CHURN -> R.string.internal_diagnostics_anomaly_cache_churn
        AnomalyType.DSP_RECREATE_STORM -> R.string.internal_diagnostics_anomaly_dsp_recreate
        AnomalyType.CANVAS_RESTART_LOOP -> R.string.internal_diagnostics_anomaly_canvas_restart
        AnomalyType.HOT_OPERATION_STORM -> R.string.internal_diagnostics_anomaly_hot_operation
    }
)

@Composable
private fun resolverLabel(event: ResolverEvent?): String {
    if (event == null) return ""
    val mode = stringResource(
        when (event.mode) {
            PlaybackMode.AUDIO -> R.string.internal_diagnostics_mode_audio
            PlaybackMode.VIDEO -> R.string.internal_diagnostics_mode_video
            PlaybackMode.DECORATIVE -> R.string.internal_diagnostics_mode_decorative
            PlaybackMode.UNKNOWN -> R.string.internal_diagnostics_state_unknown
        }
    )
    val strategy = stringResource(
        when (event.strategy) {
            ResolverStrategy.REEL_MUXED -> R.string.internal_diagnostics_strategy_reel_muxed
            ResolverStrategy.REEL_AUDIO -> R.string.internal_diagnostics_strategy_reel_audio
            ResolverStrategy.PERSISTED -> R.string.internal_diagnostics_strategy_persisted
            ResolverStrategy.DIRECT -> R.string.internal_diagnostics_strategy_direct
            ResolverStrategy.SEARCH -> R.string.internal_diagnostics_strategy_search
            ResolverStrategy.STANDARD_VIDEO -> R.string.internal_diagnostics_strategy_standard_video
            ResolverStrategy.REEL_VIDEO -> R.string.internal_diagnostics_strategy_reel_video
            ResolverStrategy.UNKNOWN -> R.string.internal_diagnostics_state_unknown
        }
    )
    val outcome = stringResource(
        when (event.outcome) {
            DiagnosticOutcome.SUCCESS -> R.string.internal_diagnostics_outcome_success
            DiagnosticOutcome.FAILURE -> R.string.internal_diagnostics_outcome_failure
            DiagnosticOutcome.TIMEOUT -> R.string.internal_diagnostics_outcome_timeout
            DiagnosticOutcome.CANCELLED -> R.string.internal_diagnostics_outcome_cancelled
        }
    )
    return stringResource(R.string.internal_diagnostics_resolver_format, mode, strategy, outcome)
}
