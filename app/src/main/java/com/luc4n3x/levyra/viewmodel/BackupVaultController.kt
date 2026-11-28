package com.luc4n3x.levyra.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import com.luc4n3x.levyra.data.AutomaticBackupScheduler
import com.luc4n3x.levyra.data.LevyraBackupManager
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.domain.LevyraBackupSettings
import com.luc4n3x.levyra.domain.LevyraVaultStatus
import com.luc4n3x.levyra.domain.VaultSyncFileState
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.vaultSyncFileCopy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

private const val VAULT_SYNC_FILE_ACCESS = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

internal class BackupVaultController(
    private val scope: CoroutineScope,
    private val context: Context,
    private val preferences: LevyraPreferences,
    private val backupManager: LevyraBackupManager,
    private val uiState: MutableStateFlow<LevyraUiState>,
    private val cleanUserError: (Throwable) -> String,
    private val afterRestore: suspend () -> Unit
) {
    private val _syncFile = MutableStateFlow(VaultSyncFileState())
    val syncFile: StateFlow<VaultSyncFileState> = _syncFile.asStateFlow()

    fun setBackupSettings(value: LevyraBackupSettings) {
        val normalized = value.normalized()
        preferences.setBackupSettings(normalized)
        AutomaticBackupScheduler.schedule(context, normalized)
        uiState.update { it.copy(backupSettings = normalized) }
    }

    fun createBackup(uri: Uri) {
        scope.launch {
            if (uiState.value.vaultStatus == LevyraVaultStatus.Running) return@launch
            uiState.update { it.copy(vaultStatus = LevyraVaultStatus.Running) }
            try {
                val result = backupManager.exportTo(uri)
                uiState.update {
                    it.copy(
                        backupMessage = result.message,
                        vaultStatus = LevyraVaultStatus.Completed,
                        lastBackupAtMs = System.currentTimeMillis()
                    )
                }
            } catch (cancelled: CancellationException) {
                uiState.update { it.copy(vaultStatus = LevyraVaultStatus.Idle) }
                throw cancelled
            } catch (error: Throwable) {
                uiState.update {
                    it.copy(
                        backupMessage = "Backup non riuscito: ${cleanUserError(error)}",
                        vaultStatus = LevyraVaultStatus.Error
                    )
                }
            }
        }
    }

    fun backupNow() {
        scope.launch {
            if (uiState.value.vaultStatus == LevyraVaultStatus.Running) return@launch
            uiState.update { it.copy(vaultStatus = LevyraVaultStatus.Running) }
            try {
                val result = backupManager.exportAutomatic(uiState.value.backupSettings.retentionCount)
                val syncFailure = try {
                    val synced = backupManager.exportToSyncFile()
                    val expected = preferences.vaultSyncFileUri().isNotBlank() && preferences.vaultSyncFileWritesEnabled()
                    if (!synced && expected) vaultSyncCopy().backupSavedNotSynced else null
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Throwable) {
                    Timber.w(error, "Vault sync file not updated")
                    vaultSyncCopy().backupSavedNotSynced
                }
                refreshSyncFile()
                uiState.update {
                    it.copy(
                        backupMessage = syncFailure ?: result.message,
                        vaultStatus = LevyraVaultStatus.Completed,
                        lastBackupAtMs = System.currentTimeMillis()
                    )
                }
            } catch (cancelled: CancellationException) {
                uiState.update { it.copy(vaultStatus = LevyraVaultStatus.Idle) }
                throw cancelled
            } catch (error: Throwable) {
                uiState.update {
                    it.copy(
                        backupMessage = "Backup non riuscito: ${cleanUserError(error)}",
                        vaultStatus = LevyraVaultStatus.Error
                    )
                }
            }
        }
    }

    fun previewRestore(uri: Uri) {
        scope.launch {
            try {
                val preview = backupManager.inspect(uri)
                uiState.update { it.copy(backupPreview = preview, pendingRestoreUri = uri) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                uiState.update { it.copy(backupMessage = "Backup non valido: ${cleanUserError(error)}") }
            }
        }
    }

    fun dismissRestorePreview() {
        uiState.update { it.copy(backupPreview = null, pendingRestoreUri = null) }
    }

    fun confirmRestore() {
        val uri = uiState.value.pendingRestoreUri ?: return
        val preview = uiState.value.backupPreview
        if (preview != null && !preview.compatible) {
            uiState.update {
                it.copy(backupMessage = "Backup non compatibile con questa versione di Levyra", backupPreview = null, pendingRestoreUri = null)
            }
            return
        }
        scope.launch {
            if (uiState.value.vaultStatus == LevyraVaultStatus.Running) return@launch
            uiState.update { it.copy(vaultStatus = LevyraVaultStatus.Running, backupPreview = null, pendingRestoreUri = null) }
            try {
                val result = backupManager.restoreFrom(uri)
                if (uri.toString() == preferences.vaultSyncFileUri()) {
                    preferences.setVaultSyncFileWritesEnabled(true)
                    refreshSyncFile()
                }
                afterRestore()
                uiState.update {
                    it.copy(
                        backupMessage = result.message,
                        vaultStatus = LevyraVaultStatus.Completed
                    )
                }
            } catch (cancelled: CancellationException) {
                uiState.update { it.copy(vaultStatus = LevyraVaultStatus.Idle) }
                throw cancelled
            } catch (error: Throwable) {
                uiState.update {
                    it.copy(
                        backupMessage = "Ripristino non riuscito: ${cleanUserError(error)}",
                        vaultStatus = LevyraVaultStatus.Error
                    )
                }
            }
        }
    }

    fun clearBackupMessage() {
        uiState.update { it.copy(backupMessage = null) }
    }

    fun setBackupLocation(uri: Uri) {
        scope.launch(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                val newUri = uri.toString()
                val previous = preferences.backupTreeUri().takeIf { it.isNotBlank() }
                resolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                try {
                    preferences.persistBackupTreeUri(newUri)
                } catch (persistError: Throwable) {
                    if (persistError is CancellationException) throw persistError
                    if (previous != newUri) {
                        runCatching {
                            resolver.releasePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            )
                        }.onFailure { releaseError ->
                            if (releaseError is CancellationException) throw releaseError
                            Timber.w(releaseError, "Unable to release backup location permission after persistence failure")
                        }
                    }
                    uiState.update { it.copy(backupMessage = "Posizione backup non disponibile") }
                    return@launch
                }
                uiState.update { it.copy(backupLocationUri = newUri) }
                if (previous != null && previous != newUri) {
                    runCatching {
                        resolver.releasePersistableUriPermission(
                            Uri.parse(previous),
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    }.onFailure { releaseError ->
                        if (releaseError is CancellationException) throw releaseError
                        Timber.w(releaseError, "Unable to release previous backup location permission")
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                uiState.update { it.copy(backupMessage = "Posizione backup non disponibile") }
            }
        }
    }

    fun clearBackupLocation() {
        scope.launch(Dispatchers.IO) {
            val raw = uiState.value.backupLocationUri ?: return@launch
            try {
                val resolver = context.contentResolver
                try {
                    resolver.releasePersistableUriPermission(
                        Uri.parse(raw),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (releaseError: Throwable) {
                    if (releaseError is CancellationException) throw releaseError
                    Timber.w(releaseError, "Unable to release backup location permission")
                }
                preferences.setBackupTreeUri("")
                uiState.update { it.copy(backupLocationUri = null) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                Timber.w(error, "Unable to clear backup location")
            }
        }
    }

    private fun vaultSyncCopy() = LevyraStrings.forCode(uiState.value.languageCode).vaultSyncFileCopy()

    fun loadSyncFile() {
        scope.launch { refreshSyncFile() }
    }

    private suspend fun refreshSyncFile() = withContext(Dispatchers.IO) {
        val raw = preferences.vaultSyncFileUri().takeIf { it.isNotBlank() }
        if (raw == null) {
            _syncFile.value = VaultSyncFileState()
            return@withContext
        }
        val uri = Uri.parse(raw)
        val accessLost = !backupManager.hasPersistedAccess(uri)
        val displayName = if (accessLost) {
            ""
        } else {
            runCatching {
                context.contentResolver
                    .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            }.getOrNull().orEmpty()
        }
        if (preferences.vaultSyncFileUri() != raw) return@withContext
        _syncFile.value = VaultSyncFileState(
            uri = raw,
            displayName = displayName,
            lastWriteAtMs = preferences.vaultSyncFileLastWriteAt(),
            writesEnabled = preferences.vaultSyncFileWritesEnabled(),
            accessLost = accessLost
        )
    }

    fun createSyncFile(uri: Uri) = linkSyncFile(uri, writesEnabled = true)

    fun useExistingSyncFile(uri: Uri) = linkSyncFile(uri, writesEnabled = false)

    private fun linkSyncFile(uri: Uri, writesEnabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            val copy = vaultSyncCopy()
            val newUri = uri.toString()
            val previous = preferences.vaultSyncFileUri().takeIf { it.isNotBlank() }
            try {
                context.contentResolver.takePersistableUriPermission(uri, VAULT_SYNC_FILE_ACCESS)
            } catch (refused: SecurityException) {
                Timber.w(refused, "Sync file location refused persistable access")
                uiState.update { it.copy(backupMessage = copy.accessRefused) }
                return@launch
            }
            val relinked = previous == newUri
            try {
                preferences.persistVaultSyncFile(newUri, writesEnabled || (relinked && preferences.vaultSyncFileWritesEnabled()))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (persistError: Throwable) {
                Timber.w(persistError, "Unable to persist the sync file link")
                if (!relinked) releaseSyncFileAccess(uri)
                uiState.update { it.copy(backupMessage = copy.linkFailed) }
                return@launch
            }
            if (previous != null && !relinked) releaseSyncFileAccess(Uri.parse(previous))
            refreshSyncFile()
            when {
                writesEnabled -> updateSyncFile()
                !relinked -> previewRestore(uri)
            }
        }
    }

    private fun releaseSyncFileAccess(uri: Uri) {
        runCatching {
            context.contentResolver.releasePersistableUriPermission(uri, VAULT_SYNC_FILE_ACCESS)
        }.onFailure { error -> Timber.w(error, "Unable to release sync file permission") }
    }

    fun updateSyncFile() {
        scope.launch {
            if (uiState.value.vaultStatus == LevyraVaultStatus.Running) {
                uiState.update { it.copy(backupMessage = vaultSyncCopy().updateFailed) }
                return@launch
            }
            uiState.update { it.copy(vaultStatus = LevyraVaultStatus.Running) }
            val copy = vaultSyncCopy()
            val written = try {
                preferences.setVaultSyncFileWritesEnabled(true)
                backupManager.exportToSyncFile()
            } catch (cancelled: CancellationException) {
                uiState.update { it.copy(vaultStatus = LevyraVaultStatus.Idle) }
                throw cancelled
            } catch (error: Throwable) {
                Timber.w(error, "Vault sync file not updated")
                null
            }
            refreshSyncFile()
            uiState.update {
                it.copy(
                    backupMessage = when (written) {
                        true -> copy.updateDone
                        false -> copy.accessLost
                        null -> copy.updateFailed
                    },
                    vaultStatus = if (written == true) LevyraVaultStatus.Completed else LevyraVaultStatus.Error
                )
            }
        }
    }

    fun restoreSyncFile() {
        _syncFile.value.uri?.let { previewRestore(Uri.parse(it)) }
    }

    fun unlinkSyncFile() {
        scope.launch(Dispatchers.IO) {
            val raw = preferences.vaultSyncFileUri().takeIf { it.isNotBlank() } ?: return@launch
            try {
                preferences.persistVaultSyncFile("", writesEnabled = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                Timber.w(error, "Unable to unlink the sync file")
                return@launch
            }
            releaseSyncFileAccess(Uri.parse(raw))
            refreshSyncFile()
        }
    }

    fun setPreUpdateBackupFailed(value: Boolean) {
        uiState.update { it.copy(preUpdateBackupFailed = value) }
    }
}
