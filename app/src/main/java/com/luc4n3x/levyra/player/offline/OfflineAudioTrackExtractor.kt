@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
package com.luc4n3x.levyra.player.offline

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object OfflineAudioTrackExtractor {
    suspend fun extractAudioTrack(context: Context, input: File, output: File) {
        try {
            runExtraction(context, input, output)
        } catch (error: Throwable) {
            runCatching { output.delete() }
            throw error
        }
    }

    private suspend fun runExtraction(context: Context, input: File, output: File) {
        val failure = withContext(Dispatchers.Main) {
            val completion = CompletableDeferred<ExportException?>()
            val transformer = Transformer.Builder(context)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .addListener(
                    object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            completion.complete(null)
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            completion.complete(exportException)
                        }
                    }
                )
                .build()
            val editedItem = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(input)))
                .setRemoveVideo(true)
                .build()
            try {
                transformer.start(editedItem, output.absolutePath)
                completion.await()
            } finally {
                transformer.cancel()
            }
        }
        if (failure != null) {
            throw IOException("Offline audio extraction failed: ${failure.message.orEmpty()}", failure)
        }
        if (!output.isFile || output.length() <= 0L) {
            throw IOException("Offline audio extraction produced an empty file")
        }
    }
}
