package com.luc4n3x.levyra.diagnostics

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.ui.i18n.supportCopy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CrashReportsScreen(
    state: CrashReportsState,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    onCopy: (CrashReport) -> Unit,
    onShare: (CrashReport) -> Unit,
    onGithub: (CrashReport) -> Unit,
    onClose: () -> Unit
) {
    val words = supportCopy(state.language)
    BackHandler(onBack = onClose)
    CompositionLocalProvider(LocalLayoutDirection provides if (LevyraLanguageCatalog.isRtl(state.language)) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text(words.reports) }, actions = {
                    state.selected?.let { report ->
                        IconButton(onClick = { onDelete(report.id) }) { Icon(Icons.Rounded.DeleteOutline, words.delete) }
                    }
                    IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, words.close) }
                })
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(24.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.Description, null, tint = MaterialTheme.colorScheme.primary)
                        Text(if (state.reports.isEmpty()) words.reportsSubtitle else words.unexpectedClose, style = MaterialTheme.typography.headlineSmall)
                        Text(words.privacy, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (state.loading) item { CircularProgressIndicator() }
                if (state.failed) item { Text(words.failed) }
                if (!state.loading && state.reports.isEmpty()) item { Text(words.empty) }
                items(state.reports, key = { it.id }) { report ->
                    TextButton(onClick = { onSelect(report.id) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(report.title, color = if (state.selectedId == report.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            report.id.substringBefore('-').toLongOrNull()?.let { timestamp ->
                                Text(java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT,
                                    java.util.Locale.forLanguageTag(state.language)).format(java.util.Date(timestamp)), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                state.selected?.let { report ->
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onGithub(report) }, modifier = Modifier.fillMaxWidth()) { Text(words.github) }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(onClick = { onCopy(report) }, modifier = Modifier.weight(1f)) { Text(words.copy) }
                                FilledTonalButton(onClick = { onShare(report) }, modifier = Modifier.weight(1f)) { Text(words.share) }
                            }
                        }
                    }
                    items(state.preview) { chunk ->
                        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                SelectionContainer {
                                    Text(chunk, modifier = Modifier.fillMaxWidth().padding(16.dp), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                                }
                            }
                        }
                    }
                }
                item { TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text(words.close) } }
            }
        }
    }
}
