package com.luc4n3x.levyra.diagnostics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luc4n3x.levyra.domain.LevyraFontPreset
import com.luc4n3x.levyra.player.redactDiagnosticText
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal data class CrashReportsState(
    val loading: Boolean = true,
    val reports: List<CrashReport> = emptyList(),
    val selectedId: String? = null,
    val preview: List<String> = emptyList(),
    val presentation: CrashPresentation = CrashPresentation(),
    val failed: Boolean = false
) {
    val selected: CrashReport? get() = reports.firstOrNull { it.id == selectedId }
    val language: String get() = presentation.language
    val fontPreset: LevyraFontPreset get() = LevyraFontPreset.entries.firstOrNull { it.name == presentation.font } ?: LevyraFontPreset.Outfit
    val animationsEnabled: Boolean get() = presentation.animations
}

internal class CrashReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val storage = CrashReporting.store(application)
    private val mutableState = MutableStateFlow(CrashReportsState())
    val state = mutableState.asStateFlow()
    private var presentationOverride: CrashPresentation? = null
    private var loadGeneration = 0L
    private val exportMutex = Mutex()

    fun load(preferredId: String? = null, presentation: CrashPresentation? = null) {
        if (presentation != null) presentationOverride = presentation
        val generation = ++loadGeneration
        val presentationSnapshot = presentationOverride
        viewModelScope.launch {
            try {
                val next = withContext(Dispatchers.IO) {
                    val reports = storage.reports().map { it.copy(title = redactDiagnosticText(it.title), body = redactDiagnosticText(it.body)) }
                    val selected = reports.firstOrNull { it.id == preferredId } ?: reports.firstOrNull()
                    CrashReportsState(
                        loading = false, reports = reports, selectedId = selected?.id,
                        preview = selected?.body?.chunked(4_000).orEmpty(),
                        presentation = presentationSnapshot ?: selected?.presentation ?: CrashPresentation()
                    )
                }
                if (generation == loadGeneration) mutableState.value = next
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (generation == loadGeneration) mutableState.value = mutableState.value.copy(loading = false, failed = true)
            }
        }
    }

    fun select(id: String) {
        mutableState.value = mutableState.value.copy(selectedId = id)
        viewModelScope.launch {
            val selected = mutableState.value.reports.firstOrNull { it.id == id } ?: return@launch
            val preview = withContext(Dispatchers.Default) { selected.body.chunked(4_000) }
            if (mutableState.value.selectedId == id) mutableState.value = mutableState.value.copy(preview = preview)
        }
    }

    fun delete(id: String) {
        if (mutableState.value.reports.none { it.id == id }) return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    storage.delete(id)
                    File(getApplication<Application>().cacheDir, "share/diagnostics/$id.txt").delete()
                }
                load()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(failed = true)
            }
        }
    }

    suspend fun export(report: CrashReport): File = withContext(Dispatchers.IO) {
        exportMutex.withLock {
            val current = storage.read(report.id) ?: error("Report unavailable")
            val directory = File(getApplication<Application>().cacheDir, "share/diagnostics")
            check(directory.isDirectory || directory.mkdirs())
            File(directory, "${report.id}.txt").apply {
                writeDiagnosticBytes(this, redactDiagnosticText(current.body).toByteArray(Charsets.UTF_8))
                directory.listFiles().orEmpty().filter { it.extension == "txt" }
                    .sortedByDescending { it.lastModified() }.drop(CrashReportStore.MAX_REPORTS).forEach { it.delete() }
            }
        }
    }
}
