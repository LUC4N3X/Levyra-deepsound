package com.luc4n3x.levyra.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.feature.recognition.LevyraRecognitionCenter
import com.luc4n3x.levyra.feature.recognition.MusicRecognitionService
import com.luc4n3x.levyra.feature.recognition.RecognitionCatalogMatcher
import com.luc4n3x.levyra.feature.recognition.RecognitionErrorKind
import com.luc4n3x.levyra.feature.recognition.RecognitionHistoryEntry
import com.luc4n3x.levyra.feature.recognition.RecognitionProjectionActivity
import com.luc4n3x.levyra.feature.recognition.RecognitionResult
import com.luc4n3x.levyra.feature.recognition.RecognitionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

internal class MusicRecognitionUiController(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<LevyraUiState>,
    private val context: Context,
    private val repository: YoutubeMusicRepository
) {
    private val recognizer get() = LevyraRecognitionCenter.get(context)
    private val catalogMatcher by lazy { RecognitionCatalogMatcher(repository) }
    private var collectorJob: Job? = null
    private var historyJob: Job? = null
    private var matchJob: Job? = null

    fun startMicrophone() {
        ensureCollector()
        uiState.update { it.copy(showRecognition = true, recognitionMatch = null) }
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            uiState.update { it.copy(recognitionState = RecognitionState.Error(RecognitionErrorKind.PermissionDenied)) }
            return
        }
        runCatching {
            ContextCompat.startForegroundService(
                context,
                MusicRecognitionService.microphoneIntent(context)
            )
        }.onFailure { Timber.w(it, "Microphone recognition service could not start") }
    }

    fun startDevice() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        ensureCollector()
        uiState.update { it.copy(showRecognition = true, recognitionMatch = null) }
        runCatching { context.startActivity(RecognitionProjectionActivity.intent(context)) }
            .onFailure { Timber.w(it, "Device playback recognition could not start") }
    }

    fun open() {
        ensureCollector()
        uiState.update { it.copy(showRecognition = true) }
    }

    fun close() {
        uiState.update { it.copy(showRecognition = false) }
    }

    fun deleteEntry(id: String) {
        scope.launch { LevyraRecognitionCenter.history(context).delete(id) }
    }

    fun clearHistory() {
        scope.launch { LevyraRecognitionCenter.history(context).clear() }
    }

    fun openResult(entry: RecognitionHistoryEntry) {
        uiState.update {
            it.copy(
                showRecognition = true,
                recognitionState = RecognitionState.Result(entry.result),
                recognitionMatch = null
            )
        }
        matchResult(entry.result)
    }

    private fun ensureCollector() {
        if (collectorJob != null) return
        collectorJob = scope.launch {
            recognizer.state.collect { recognitionState ->
                uiState.update { it.copy(recognitionState = recognitionState) }
                if (recognitionState is RecognitionState.Result) {
                    matchResult(recognitionState.result)
                }
            }
        }
    }

    fun observeHistory() {
        historyJob?.cancel()
        historyJob = scope.launch {
            LevyraRecognitionCenter.observeHistory(context).collect { entries ->
                uiState.update { it.copy(recognitionHistory = entries) }
            }
        }
    }

    private fun matchResult(result: RecognitionResult) {
        matchJob?.cancel()
        uiState.update { it.copy(recognitionMatching = true, recognitionMatch = null) }
        matchJob = scope.launch(Dispatchers.IO) {
            val match = runCatching {
                catalogMatcher.match(result, uiState.value.languageCode)
            }.getOrNull()
            uiState.update { it.copy(recognitionMatching = false, recognitionMatch = match) }
        }
    }

    fun resetCollector() {
        collectorJob?.cancel()
        collectorJob = null
        matchJob?.cancel()
        matchJob = null
        uiState.update {
            it.copy(
                recognitionState = RecognitionState.Idle,
                recognitionMatch = null,
                recognitionMatching = false
            )
        }
    }

    fun cancel() {
        recognizer.cancel()
        matchJob?.cancel()
        matchJob = null
        uiState.update {
            it.copy(
                recognitionState = RecognitionState.Idle,
                recognitionMatch = null,
                recognitionMatching = false
            )
        }
    }

    fun cancelRecognizer() {
        recognizer.cancel()
    }

    fun release() {
        if (collectorJob != null) {
            collectorJob?.cancel()
            recognizer.cancel()
        }
        historyJob?.cancel()
        matchJob?.cancel()
    }
}
