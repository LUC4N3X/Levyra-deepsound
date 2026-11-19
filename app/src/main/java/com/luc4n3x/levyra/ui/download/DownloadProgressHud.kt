package com.luc4n3x.levyra.ui.download

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.OfflineDownloadTask
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.artistCatalogCopy
import com.luc4n3x.levyra.ui.i18n.downloadProgressCopy

private const val QUEUE_PREVIEW_ROWS = 5

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun DownloadProgressHud(
    queue: List<OfflineDownloadTask>,
    onOpenDownloads: () -> Unit,
    onCancelAll: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { downloadProgressCopy(strings.code) }
    val catalogCopy = remember(strings) { strings.artistCatalogCopy() }
    val colors = MaterialTheme.colorScheme
    val active = remember(queue) { activeHudTasks(queue) }
    var sessionKeys by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val session = remember(sessionKeys, active) { extendDownloadSession(sessionKeys.toSet(), active) }
    LaunchedEffect(session) { sessionKeys = session.toList() }
    val snapshot = remember(active, session) { downloadHudSnapshot(active, session) } ?: return
    var expanded by rememberSaveable { mutableStateOf(false) }
    var confirmCancel by rememberSaveable { mutableStateOf(false) }
    val currentProgress by animateFloatAsState(
        targetValue = snapshot.currentProgress / 100f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "downloadHudCurrent"
    )
    val overallProgress by animateFloatAsState(
        targetValue = snapshot.overallProgress / 100f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "downloadHudOverall"
    )
    val downloadedOf = copy.downloadedOf(snapshot.completed, snapshot.total)

    Surface(
        onClick = onOpenDownloads,
        shape = RoundedCornerShape(28.dp),
        color = colors.surfaceContainerHigh,
        contentColor = colors.onSurface,
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { onClick(label = copy.openDownloads) { onOpenDownloads(); true } }
    ) {
        Column(modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(colors.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CircularWavyProgressIndicator(
                        progress = { currentProgress },
                        modifier = Modifier.size(44.dp),
                        color = colors.primary,
                        trackColor = colors.primary.copy(alpha = 0.18f)
                    )
                    Text(
                        text = "${snapshot.currentProgress}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimaryContainer,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = snapshot.current.title.ifBlank { strings.song },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = listOf(snapshot.current.artist, downloadedOf).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (snapshot.queued.isNotEmpty()) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (expanded) strings.showLess else strings.showMore,
                            tint = colors.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = { confirmCancel = true }) {
                    Icon(Icons.Rounded.Close, contentDescription = catalogCopy.cancelDownload, tint = colors.onSurfaceVariant)
                }
            }
            Column(modifier = Modifier.padding(start = 2.dp, end = 10.dp, top = 10.dp)) {
                LinearWavyProgressIndicator(
                    progress = { overallProgress },
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.primary,
                    trackColor = colors.primary.copy(alpha = 0.18f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = copy.total(snapshot.overallProgress),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 1
                    )
                    if (snapshot.queued.isNotEmpty()) {
                        Text(
                            text = copy.queued(snapshot.queued.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = expanded && snapshot.queued.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(start = 2.dp, end = 10.dp, top = 8.dp)) {
                    snapshot.queued.take(QUEUE_PREVIEW_ROWS).forEach { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 36.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = colors.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = listOf(task.title, task.artist).filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = strings.localizeDownloadState(task.state),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                    val hidden = snapshot.queued.size - QUEUE_PREVIEW_ROWS
                    if (hidden > 0) {
                        Text(
                            text = "+$hidden",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primary,
                            modifier = Modifier.padding(start = 26.dp, top = 4.dp)
                        )
                    }
                }
            }
        }
    }

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            icon = { Icon(Icons.Rounded.DownloadForOffline, contentDescription = null) },
            title = { Text(catalogCopy.cancelDownloadTitle) },
            text = { Text("$downloadedOf\n\n${catalogCopy.cancelDownloadBody}") },
            confirmButton = {
                TextButton(onClick = {
                    confirmCancel = false
                    onCancelAll()
                }) { Text(catalogCopy.cancelDownload) }
            },
            dismissButton = {
                TextButton(onClick = { confirmCancel = false }) { Text(catalogCopy.keepDownloading) }
            }
        )
    }
}
