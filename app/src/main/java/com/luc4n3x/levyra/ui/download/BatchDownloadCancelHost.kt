package com.luc4n3x.levyra.ui.download

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.artistCatalogCopy
import com.luc4n3x.levyra.viewmodel.LevyraViewModel

@Composable
internal fun BatchDownloadCancelHost(viewModel: LevyraViewModel) {
    val pending by viewModel.pendingBatchCancel.collectAsStateWithLifecycle()
    val batch = pending ?: return
    val strings = LocalLevyraStrings.current
    val copy = remember(strings) { strings.artistCatalogCopy() }
    AlertDialog(
        onDismissRequest = viewModel::dismissBatchCancel,
        icon = { Icon(Icons.Rounded.DownloadForOffline, contentDescription = null) },
        title = { Text(copy.cancelDownloadTitle) },
        text = { Text("${batch.title} · ${batch.completed}/${batch.total}\n\n${copy.cancelDownloadBody}") },
        confirmButton = {
            TextButton(onClick = viewModel::confirmBatchCancel) { Text(copy.cancelDownload) }
        },
        dismissButton = {
            TextButton(onClick = viewModel::dismissBatchCancel) { Text(copy.keepDownloading) }
        }
    )
}
