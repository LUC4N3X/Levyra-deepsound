package com.luc4n3x.levyra.feature.recognition

import android.content.Context

object LevyraRecognitionCenter {
    private val lock = Any()
    private val provider: RecognitionProvider = NoOpRecognitionProvider

    @Volatile
    private var controller: MusicRecognitionController? = null

    val isAvailable: Boolean
        get() = provider !== NoOpRecognitionProvider

    fun get(context: Context): MusicRecognitionController = controller ?: synchronized(lock) {
        controller ?: MusicRecognitionController(
            audioCapture = MicrophoneCapture(context.applicationContext),
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
