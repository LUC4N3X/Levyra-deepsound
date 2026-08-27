package com.luc4n3x.levyra.feature.recognition

import android.content.Context

internal class RecognitionProviderUnavailableException : IllegalStateException("Recognition provider is unavailable")

object LevyraRecognitionCenter {
    private val lock = Any()
    @Volatile
    private var provider: RecognitionProvider = NoOpRecognitionProvider
    private val unavailableCapture = AudioCapture { throw RecognitionProviderUnavailableException() }

    @Volatile
    private var controller: MusicRecognitionController? = null

    val isAvailable: Boolean
        get() = provider !== NoOpRecognitionProvider

    fun get(context: Context): MusicRecognitionController = controller ?: synchronized(lock) {
        controller ?: MusicRecognitionController(
            audioCapture = if (isAvailable) MicrophoneCapture(context.applicationContext) else unavailableCapture,
            provider = provider
        ).also { controller = it }
    }

    fun start(context: Context) {
        if (isAvailable) get(context).start()
    }

    fun configureAudD(context: Context, token: String) {
        synchronized(lock) {
            val credentials = com.luc4n3x.levyra.data.security.AndroidKeystoreCredentialStore(context.applicationContext)
            val audD = AudDRecognitionProvider(credentials)
            audD.saveToken(token)
            provider = if (audD.isConfigured()) audD else NoOpRecognitionProvider
            controller?.cancel()
            controller = null
        }
    }

    fun restoreAudD(context: Context) {
        synchronized(lock) {
            val audD = AudDRecognitionProvider(
                com.luc4n3x.levyra.data.security.AndroidKeystoreCredentialStore(context.applicationContext)
            )
            provider = if (audD.isConfigured()) audD else NoOpRecognitionProvider
            controller?.cancel()
            controller = null
        }
    }

    fun clearAudD(context: Context) {
        synchronized(lock) {
            AudDRecognitionProvider(
                com.luc4n3x.levyra.data.security.AndroidKeystoreCredentialStore(context.applicationContext)
            ).clear()
            provider = NoOpRecognitionProvider
            controller?.cancel()
            controller = null
        }
    }

    fun cancel(context: Context) {
        get(context).cancel()
    }
}
