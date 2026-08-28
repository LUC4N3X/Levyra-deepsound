package com.luc4n3x.levyra.feature.recognition

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

sealed interface RecognitionState {
    data object Idle : RecognitionState
    data object Listening : RecognitionState
    data object Identifying : RecognitionState
    data class Result(val result: RecognitionResult) : RecognitionState
    data object NoMatch : RecognitionState
    data class Error(val kind: RecognitionErrorKind) : RecognitionState
}

class MusicRecognitionController(
    private val audioCapture: AudioCapture,
    private val provider: RecognitionProvider = NoOpRecognitionProvider,
    private val captureDurationMs: Long = MicrophoneCapture.RELIABLE_FINGERPRINT_CAPTURE_DURATION_MS,
    private val captureTimeoutMs: Long = captureDurationMs + DEFAULT_CAPTURE_TIMEOUT_GRACE_MS,
    private val targetSampleRateHz: Int = DEFAULT_TARGET_SAMPLE_RATE_HZ,
    private val identifyTimeoutMs: Long = DEFAULT_IDENTIFY_TIMEOUT_MS,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val startLock = Any()
    private val _state = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val state: StateFlow<RecognitionState> = _state.asStateFlow()

    @Volatile
    private var activeJob: Job? = null
    private var runGeneration: Long = 0L
    private var activeRunGeneration: Long = 0L

    fun start(capture: AudioCapture = audioCapture) {
        synchronized(startLock) {
            if (activeJob?.isActive == true) return
            val generation = ++runGeneration
            activeRunGeneration = generation
            _state.value = RecognitionState.Listening
            val job = scope.launch(start = CoroutineStart.LAZY) {
                try {
                    runRecognition(generation, capture)
                } catch (error: CancellationException) {
                    publishTerminalIfActive(generation, RecognitionState.Error(RecognitionErrorKind.Cancelled))
                    throw error
                }
            }
            activeJob = job
            job.invokeOnCompletion {
                synchronized(startLock) {
                    if (activeRunGeneration == generation && activeJob === job) {
                        activeJob = null
                    }
                }
            }
            job.start()
        }
    }

    fun reset() {
        synchronized(startLock) {
            if (activeJob?.isActive == true) return
            activeRunGeneration = 0L
            _state.value = RecognitionState.Idle
        }
    }

    fun cancel() {
        val job = synchronized(startLock) {
            val current = activeJob ?: return@synchronized null
            activeRunGeneration = 0L
            activeJob = null
            _state.value = RecognitionState.Error(RecognitionErrorKind.Cancelled)
            current
        }
        job?.cancel()
    }

    fun close() {
        cancel()
        scope.cancel()
    }

    private suspend fun runRecognition(generation: Long, capture: AudioCapture) {
        if (!publishIfActive(generation, RecognitionState.Listening)) return
        val captured = try {
            withTimeout(captureTimeoutMs) { capture.capture(captureDurationMs) }
        } catch (_: TimeoutCancellationException) {
            publishTerminalIfActive(generation, RecognitionState.Error(RecognitionErrorKind.Timeout))
            return
        } catch (error: CancellationException) {
            throw error
        } catch (_: RecognitionProviderUnavailableException) {
            publishTerminalIfActive(generation, RecognitionState.Error(RecognitionErrorKind.Unavailable))
            return
        } catch (_: MicrophonePermissionDeniedException) {
            publishTerminalIfActive(generation, RecognitionState.Error(RecognitionErrorKind.PermissionDenied))
            return
        } catch (_: DevicePlaybackCaptureUnsupportedException) {
            publishTerminalIfActive(generation, RecognitionState.Error(RecognitionErrorKind.Unavailable))
            return
        } catch (_: Throwable) {
            publishTerminalIfActive(generation, RecognitionState.Error(RecognitionErrorKind.Fingerprint))
            return
        }

        val fingerprint = try {
            buildFingerprint(captured)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            publishTerminalIfActive(generation, RecognitionState.Error(RecognitionErrorKind.Fingerprint))
            return
        }

        if (!publishIfActive(generation, RecognitionState.Identifying)) return
        val outcome = try {
            withTimeout(identifyTimeoutMs) { provider.identify(fingerprint) }
        } catch (_: TimeoutCancellationException) {
            publishTerminalIfActive(generation, RecognitionState.Error(RecognitionErrorKind.Timeout))
            return
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            publishTerminalIfActive(generation, RecognitionState.Error(RecognitionErrorKind.Network))
            return
        }

        publishTerminalIfActive(
            generation,
            when (outcome) {
                is RecognitionOutcome.Match -> RecognitionState.Result(outcome.result)
                RecognitionOutcome.NoMatch -> RecognitionState.NoMatch
                is RecognitionOutcome.Error -> RecognitionState.Error(outcome.kind)
            }
        )
    }

    private fun publishIfActive(generation: Long, state: RecognitionState): Boolean = synchronized(startLock) {
        if (activeRunGeneration != generation) {
            false
        } else {
            _state.value = state
            true
        }
    }

    private fun publishTerminalIfActive(generation: Long, state: RecognitionState): Boolean = synchronized(startLock) {
        if (activeRunGeneration != generation) {
            false
        } else {
            activeRunGeneration = 0L
            activeJob = null
            _state.value = state
            true
        }
    }

    private fun buildFingerprint(captured: CapturedAudio): AudioFingerprint {
        val mono = if (captured.channelCount == 2) {
            AudioPreprocessor.downmixStereoToMono(captured.samples)
        } else {
            captured.samples
        }
        val resampled = AudioPreprocessor.resampleMono(mono, captured.sampleRateHz, targetSampleRateHz)
        val normalized = AudioPreprocessor.normalizeAmplitude(resampled)
        val durationMs = if (targetSampleRateHz > 0) {
            (normalized.size.toLong() * 1000L) / targetSampleRateHz
        } else {
            0L
        }
        return AudioFingerprint(normalized, targetSampleRateHz, durationMs)
    }

    companion object {
        const val DEFAULT_TARGET_SAMPLE_RATE_HZ = 16_000
        const val DEFAULT_IDENTIFY_TIMEOUT_MS = 12_000L
        const val DEFAULT_CAPTURE_TIMEOUT_GRACE_MS = 5_000L
    }
}
