package com.luc4n3x.levyra.feature.recognition

import android.content.Context

internal class RecognitionProviderUnavailableException : IllegalStateException("Recognition provider is unavailable")

object LevyraRecognitionCenter {
    private val lock = Any()
    private val provider: RecognitionProvider = NoOpRecognitionProvider
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

    fun cancel(context: Context) {
        get(context).cancel()
    }
}
