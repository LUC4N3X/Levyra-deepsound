package com.luc4n3x.levyra.desktop.app.ui.components

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.desktop.core.storage.DownloadRecord

import kotlinx.coroutines.flow.StateFlow

@Immutable
data class DownloadActions(
    val stateFlow: StateFlow<Map<String, DownloadRecord>>,
    val onDownload: (Track) -> Unit,
    val onCancel: (String) -> Unit,
    val onRetry: (String) -> Unit,
    val onDelete: (String) -> Unit
)

val LocalDownloadActions = staticCompositionLocalOf<DownloadActions?> { null }
