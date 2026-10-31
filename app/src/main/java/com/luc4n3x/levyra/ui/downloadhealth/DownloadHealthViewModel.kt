package com.luc4n3x.levyra.ui.downloadhealth

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luc4n3x.levyra.data.DownloadHealthRepository
import com.luc4n3x.levyra.data.DownloadHealthResult
import com.luc4n3x.levyra.data.isSameLegacyDownloadTree
import com.luc4n3x.levyra.data.local.OfflineDownloadTaskSummaryRow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DownloadHealthState(
    val results: List<DownloadHealthResult> = emptyList(),
    val tasks: List<OfflineDownloadTaskSummaryRow> = emptyList(),
    val scanning: Boolean = false,
    val scanned: Int = 0,
    val total: Int = 0,
    val finished: Boolean = false,
    val failed: Boolean = false,
    val repairing: Boolean = false,
    val folderAccessMismatch: Boolean = false
)

class DownloadHealthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DownloadHealthRepository(application)
    private val mutableState = MutableStateFlow(DownloadHealthState())
    val state = mutableState.asStateFlow()
    private var scanJob: Job? = null

    init {
        viewModelScope.launch {
            repository.tasks.collect { tasks -> mutableState.update { it.copy(tasks = tasks) } }
        }
    }

    fun scan() {
        scanJob?.cancel()
        mutableState.update { it.copy(scanning = true, scanned = 0, total = 0, failed = false) }
        scanJob = viewModelScope.launch {
            try {
                val results = repository.scan { scanned, total -> mutableState.update { it.copy(scanned = scanned, total = total) } }
                mutableState.update { it.copy(results = results, scanning = false, finished = true) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(scanning = false, failed = true) }
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        mutableState.update { it.copy(scanning = false) }
    }

    fun repair(results: List<DownloadHealthResult>) {
        if (mutableState.value.repairing) return
        mutableState.update { it.copy(repairing = true, failed = false) }
        viewModelScope.launch {
            try {
                results.filter { it.repairable }.forEach { repository.repair(it) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(failed = true) }
            } finally {
                mutableState.update { it.copy(repairing = false) }
            }
        }
    }

    fun restoreFolderAccess(pickedTreeUri: Uri, expectedTreeUri: String) {
        if (!isSameLegacyDownloadTree(pickedTreeUri.toString(), expectedTreeUri)) {
            mutableState.update { it.copy(folderAccessMismatch = true) }
            return
        }
        viewModelScope.launch {
            val granted = try {
                repository.restoreLegacyFolderAccess(pickedTreeUri, expectedTreeUri)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            }
            if (granted) scan() else mutableState.update { it.copy(failed = true) }
        }
    }

    fun consumeFolderAccessMismatch() = mutableState.update { it.copy(folderAccessMismatch = false) }

    fun cancel(taskKey: String) = taskAction { repository.cancel(taskKey) }
    fun retry(taskKey: String) = taskAction { repository.retry(taskKey) }

    private fun taskAction(action: suspend () -> Unit) {
        viewModelScope.launch {
            try { action() } catch (cancelled: CancellationException) { throw cancelled } catch (_: Exception) {
                mutableState.update { it.copy(failed = true) }
            }
        }
    }
}
