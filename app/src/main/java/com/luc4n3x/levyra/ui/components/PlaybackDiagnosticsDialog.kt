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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.util.UnstableApi
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.player.PlaybackDiagnosticSnapshot
import com.luc4n3x.levyra.player.PlaybackDiagnosticStatus
import com.luc4n3x.levyra.player.PlaybackDiagnosticsReader
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.RecommendationDiagnosticsCopy
import com.luc4n3x.levyra.ui.i18n.recommendationDiagnosticsCopy
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
    val copy = LocalLevyraStrings.current.recommendationDiagnosticsCopy()
    val reader = remember(context.applicationContext) {
        PlaybackDiagnosticsReader(context.applicationContext)
    }
    var snapshot by remember(track.id) { mutableStateOf(reader.capture(track)) }

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
                    DiagnosticsHeader(
                        title = copy.diagnosticsTitle,
                        status = snapshot.status.label(copy),
                        closeLabel = copy.close,
                        onDismiss = onDismiss
                    )
                }

                item {
                    DiagnosticSection(
                        title = copy.track,
                        rows = listOf(
                            copy.id to snapshot.trackId,
                            copy.title to snapshot.title,
                            copy.artist to snapshot.artist,
                            copy.source to snapshot.source,
                            copy.mode to if (snapshot.videoMode) copy.video else copy.audio
                        )
                    )
                }

                item {
                    val playerRows = listOfNotNull(
                        copy.state to snapshot.playerState,
                        copy.playing to snapshot.isPlaying.toString(),
                        copy.position to formatDuration(snapshot.positionMs),
                        copy.buffered to formatDuration(snapshot.bufferedPositionMs),
                        copy.duration to formatDuration(snapshot.durationMs),
                        copy.speed to String.format(Locale.ROOT, "%.2fx", snapshot.playbackSpeed),
                        snapshot.audioSessionId?.let { copy.audioSession to it.toString() },
                        snapshot.playerErrorCode.takeIf { it.isNotBlank() }?.let { copy.errorCode to it }
                    )
                    DiagnosticSection(title = copy.player, rows = playerRows)
                }

                item {
                    DiagnosticSection(
                        title = copy.formats,
                        rows = listOf(
                            copy.audio to snapshot.audioFormat?.summary().orEmpty().ifBlank { "-" },
                            copy.video to snapshot.videoFormat?.summary().orEmpty().ifBlank { "-" }
                        )
                    )
                }

                item {
                    DiagnosticSection(
                        title = copy.network,
                        rows = listOf(
                            copy.cache to formatBytes(snapshot.cacheBytes),
                            copy.transport to snapshot.networkTransport,
                            copy.validated to snapshot.networkValidated.toString(),
                            copy.metered to snapshot.networkMetered.toString()
                        )
                    )
                }

                if (snapshot.strategies.isNotEmpty()) {
                    item {
                        Text(
                            text = copy.resolver,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(snapshot.strategies, key = { it.name }) { strategy ->
                        DiagnosticSection(
                            title = strategy.name,
                            rows = buildList {
                                add(copy.successFailure to "${strategy.successes} / ${strategy.failures}")
                                add(copy.failureStreak to strategy.consecutiveFailures.toString())
                                add(copy.circuit to strategy.circuit.name)
                                strategy.averageLatencyMs?.let { add(copy.averageLatency to "${it} ms") }
                                if (strategy.lastFailure.isNotBlank()) add(copy.lastFailure to strategy.lastFailure)
                            }
                        )
                    }
                }

                item {
                    Text(
                        text = copy.security,
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
                                Toast.makeText(context, copy.copied, Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                            Text(
                                text = copy.copyReport,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        OutlinedButton(
                            modifier = Modifier.weight(0.62f),
                            onClick = onDismiss
                        ) {
                            Text(copy.close)
                        }
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

@Composable
private fun DiagnosticsHeader(
    title: String,
    status: String,
    closeLabel: String,
    onDismiss: () -> Unit
) {
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
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onDismiss) {
            Icon(Icons.Rounded.Close, contentDescription = closeLabel)
        }
    }
}

private fun PlaybackDiagnosticStatus.label(copy: RecommendationDiagnosticsCopy): String = when (this) {
    PlaybackDiagnosticStatus.HEALTHY -> copy.statusHealthy
    PlaybackDiagnosticStatus.FALLBACK_HISTORY -> copy.statusFallback
    PlaybackDiagnosticStatus.ERROR -> copy.statusError
    PlaybackDiagnosticStatus.IDLE -> copy.statusIdle
}

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
