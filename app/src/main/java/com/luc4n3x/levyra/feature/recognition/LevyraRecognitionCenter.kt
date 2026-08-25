package com.luc4n3x.levyra.feature.recognition

import android.content.Context

object LevyraRecognitionCenter {
    private val lock = Any()

    @Volatile
    private var controller: MusicRecognitionController? = null

    fun get(context: Context): MusicRecognitionController = controller ?: synchronized(lock) {
        controller ?: MusicRecognitionController(
            audioCapture = MicrophoneCapture(context.applicationContext),
            provider = NoOpRecognitionProvider
        ).also { controller = it }
    }

    fun start(context: Context) {
        get(context).start()
    }

    fun cancel(context: Context) {
        get(context).cancel()
    }
}
