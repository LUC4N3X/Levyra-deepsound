package com.luc4n3x.levyra.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.ContentCopy
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.util.UnstableApi
import com.luc4n3x.levyra.R
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.player.PlaybackDiagnosticSnapshot
import com.luc4n3x.levyra.player.PlaybackDiagnosticStatus
import com.luc4n3x.levyra.player.PlaybackDiagnosticsReader
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

@UnstableApi
@Composable
internal fun PlaybackDiagnosticsDialog(
    track: Track,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val reader = remember(context.applicationContext) {
        PlaybackDiagnosticsReader(context.applicationContext)
    }
    var snapshot by remember(track.id) { mutableStateOf(reader.capture(track)) }
    val copiedMessage = stringResource(R.string.playback_diagnostics_copied)

    LaunchedEffect(track.id) {
        while (isActive) {
            snapshot = reader.capture(track)
            delay(1_000L)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BugReport,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.playback_diagnostics),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = snapshot.status.statusLabel(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.playback_diagnostics_close)
                            )
                        }
                    }
                }

                item {
                    DiagnosticSection(
                        title = stringResource(R.string.playback_diagnostics_track),
                        rows = listOf(
                            stringResource(R.string.playback_diagnostics_id) to snapshot.trackId,
                            "Title" to snapshot.title,
                            "Artist" to snapshot.artist,
                            stringResource(R.string.playback_diagnostics_source) to snapshot.source,
                            stringResource(R.string.playback_diagnostics_mode) to if (snapshot.videoMode) "video" else "audio"
                        )
                    )
                }

                item {
                    DiagnosticSection(
                        title = stringResource(R.string.playback_diagnostics_player),
                        rows = buildList {
                            add(stringResource(R.string.playback_diagnostics_state) to snapshot.playerState)
                            add("Playing" to snapshot.isPlaying.toString())
                            add(stringResource(R.string.playback_diagnostics_position) to formatDuration(snapshot.positionMs))
                            add(stringResource(R.string.playback_diagnostics_buffered) to formatDuration(snapshot.bufferedPositionMs))
                            add(stringResource(R.string.playback_diagnostics_duration) to formatDuration(snapshot.durationMs))
                            add(
                                stringResource(R.string.playback_diagnostics_speed) to
                                    String.format(Locale.ROOT, "%.2fx", snapshot.playbackSpeed)
                            )
                            snapshot.audioSessionId?.let { add("Audio session" to it.toString()) }
                            if (snapshot.playerErrorCode.isNotBlank()) {
                                add(stringResource(R.string.playback_diagnostics_error_code) to snapshot.playerErrorCode)
                            }
                        }
                    )
                }

                item {
                    DiagnosticSection(
                        title = stringResource(R.string.playback_diagnostics_formats),
                        rows = listOf(
                            stringResource(R.string.playback_diagnostics_audio) to
                                snapshot.audioFormat?.summary().orEmpty().ifBlank { "-" },
                            stringResource(R.string.playback_diagnostics_video) to
                                snapshot.videoFormat?.summary().orEmpty().ifBlank { "-" }
                        )
                    )
                }

                item {
                    DiagnosticSection(
                        title = stringResource(R.string.playback_diagnostics_network),
                        rows = listOf(
                            stringResource(R.string.playback_diagnostics_cache) to formatBytes(snapshot.cacheBytes),
                            stringResource(R.string.playback_diagnostics_transport) to snapshot.networkTransport,
                            stringResource(R.string.playback_diagnostics_validated) to snapshot.networkValidated.toString(),
                            stringResource(R.string.playback_diagnostics_metered) to snapshot.networkMetered.toString()
                        )
                    )
                }

                if (snapshot.strategies.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.playback_diagnostics_resolver),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(snapshot.strategies, key = { it.name }) { strategy ->
                        DiagnosticSection(
                            title = strategy.name,
                            rows = buildList {
                                add("Success / failure" to "${strategy.successes} / ${strategy.failures}")
                                add("Failure streak" to strategy.consecutiveFailures.toString())
                                add("Circuit" to strategy.circuit.name)
                                strategy.averageLatencyMs?.let { add("Average latency" to "${it} ms") }
                                if (strategy.lastFailure.isNotBlank()) add("Last failure" to strategy.lastFailure)
                            }
                        )
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.playback_diagnostics_security),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(
                                    ClipData.newPlainText("Levyra playback diagnostics", snapshot.safeReport())
                                )
                                Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                            Text(
                                text = stringResource(R.string.playback_diagnostics_copy),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        OutlinedButton(
                            modifier = Modifier.weight(0.62f),
                            onClick = onDismiss
                        ) {
                            Text(stringResource(R.string.playback_diagnostics_close))
                        }
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun PlaybackDiagnosticStatus.statusLabel(): String = stringResource(
    when (this) {
        PlaybackDiagnosticStatus.HEALTHY -> R.string.playback_diagnostics_status_healthy
        PlaybackDiagnosticStatus.FALLBACK_HISTORY -> R.string.playback_diagnostics_status_fallback
        PlaybackDiagnosticStatus.ERROR -> R.string.playback_diagnostics_status_error
        PlaybackDiagnosticStatus.IDLE -> R.string.playback_diagnostics_status_idle
    }
)

@Composable
private fun DiagnosticSection(
    title: String,
    rows: List<Pair<String, String>>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        rows.filter { it.second.isNotBlank() }.forEach { (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.42f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(0.58f)
                )
            }
        }
    }
}

private fun formatDuration(valueMs: Long): String {
    val totalSeconds = valueMs.coerceAtLeast(0L) / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(Locale.ROOT, minutes, seconds)
}

private fun formatBytes(value: Long): String {
    val mib = value.coerceAtLeast(0L).toDouble() / (1024.0 * 1024.0)
    return String.format(Locale.ROOT, "%.1f MiB", mib)
}
