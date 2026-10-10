package com.luc4n3x.levyra.feature.recognition

import android.content.Context
import android.media.projection.MediaProjection
import android.os.Build
import com.luc4n3x.levyra.data.security.AndroidKeystoreCredentialStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

internal class RecognitionProviderUnavailableException :
    IllegalStateException("Recognition provider is unavailable")

object LevyraRecognitionCenter {
    private val lock = Any()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var fallbackProvider: RecognitionProvider? = null

    @Volatile
    private var controller: MusicRecognitionController? = null

    @Volatile
    private var historyStore: RecognitionHistoryStore? = null

    @Volatile
    private var historyJob: Job? = null

    private val unsupportedDevicePlaybackCapture = AudioCapture {
        throw DevicePlaybackCaptureUnsupportedException()
    }

    const val isAvailable: Boolean = true

    val isFallbackConfigured: Boolean
        get() = fallbackProvider != null

    fun get(context: Context): MusicRecognitionController = controller ?: synchronized(lock) {
        controller ?: createControllerLocked(context).also { controller = it }
    }

    fun history(context: Context): RecognitionHistoryStore = historyStore ?: synchronized(lock) {
        historyStore ?: RecognitionHistoryStore.from(context).also { historyStore = it }
    }

    fun observeHistory(context: Context): Flow<List<RecognitionHistoryEntry>> = history(context).observe()

    fun start(context: Context) = startMicrophone(context)

    fun startMicrophone(context: Context) {
        val appContext = context.applicationContext
        get(appContext).start(MicrophoneCapture(appContext))
    }

    fun startDevicePlayback(context: Context, projection: MediaProjection) {
        val appContext = context.applicationContext
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            get(appContext).start(unsupportedDevicePlaybackCapture)
            return
        }
        get(appContext).start(DevicePlaybackCapture(projection))
    }

    fun cancel(context: Context) {
        get(context).cancel()
    }

    fun reset(context: Context) {
        get(context).reset()
    }

    fun configureAudD(context: Context, token: String) {
        val appContext = context.applicationContext
        synchronized(lock) {
            val provider = AudDRecognitionProvider(AndroidKeystoreCredentialStore(appContext))
            provider.saveToken(token)
            fallbackProvider = provider.takeIf { it.isConfigured() }
            rebuildControllerLocked(appContext)
        }
    }

    fun restoreAudD(context: Context) {
        val appContext = context.applicationContext
        synchronized(lock) {
            val provider = AudDRecognitionProvider(AndroidKeystoreCredentialStore(appContext))
            fallbackProvider = provider.takeIf { it.isConfigured() }
            rebuildControllerLocked(appContext)
        }
    }

    fun clearAudD(context: Context) {
        val appContext = context.applicationContext
        synchronized(lock) {
            AudDRecognitionProvider(AndroidKeystoreCredentialStore(appContext)).clear()
            fallbackProvider = null
            rebuildControllerLocked(appContext)
        }
    }

    private fun rebuildControllerLocked(context: Context) {
        controller?.close()
        controller = createControllerLocked(context)
    }

    private fun createControllerLocked(context: Context): MusicRecognitionController {
        val appContext = context.applicationContext
        val created = MusicRecognitionController(
            audioCapture = MicrophoneCapture(appContext),
            provider = LayeredRecognitionProvider(ShazamRecognitionProvider(), fallbackProvider)
        )
        historyJob?.cancel()
        historyJob = scope.launch {
            val store = history(appContext)
            created.state.collect { state ->
                if (state is RecognitionState.Result) store.record(state.result)
            }
        }
        return created
    }
}
