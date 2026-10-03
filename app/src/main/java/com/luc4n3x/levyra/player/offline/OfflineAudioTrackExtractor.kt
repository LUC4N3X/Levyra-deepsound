@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
package com.luc4n3x.levyra.player.offline

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import timber.log.Timber

internal enum class OfflineAudioExtractionStrategy(val label: String) {
    DIRECT_MP4_REMUX("media_muxer_remux"),
    TRANSFORMER_AAC_FALLBACK("media3_transformer_aac"),
    NO_AUDIO_TRACK("no_audio_track")
}

internal fun isDirectMp4RemuxAudioMime(mimeType: String): Boolean {
    val normalized = mimeType.substringBefore(';').trim().lowercase(Locale.US)
    return normalized == MimeTypes.AUDIO_AAC ||
        normalized == "audio/mp4" ||
        normalized == "audio/m4a" ||
        normalized == "audio/x-m4a"
}

internal fun selectPreferredAudioTrackIndex(trackMimeTypes: List<String>): Int {
    val directIndex = trackMimeTypes.indexOfFirst(::isDirectMp4RemuxAudioMime)
    if (directIndex >= 0) return directIndex
    return trackMimeTypes.indexOfFirst { it.trim().startsWith("audio/", ignoreCase = true) }
}

internal fun selectOfflineAudioExtractionStrategy(trackMimeTypes: List<String>): OfflineAudioExtractionStrategy {
    val index = selectPreferredAudioTrackIndex(trackMimeTypes)
    if (index < 0) return OfflineAudioExtractionStrategy.NO_AUDIO_TRACK
    return if (isDirectMp4RemuxAudioMime(trackMimeTypes[index])) {
        OfflineAudioExtractionStrategy.DIRECT_MP4_REMUX
    } else {
        OfflineAudioExtractionStrategy.TRANSFORMER_AAC_FALLBACK
    }
}

internal fun resolveExtractorSampleBufferSize(maxInputSizeHint: Int, defaultBufferSize: Int = 512 * 1024): Int {
    return maxOf(defaultBufferSize, maxInputSizeHint.coerceAtLeast(0))
}

internal fun normalizeExtractorPresentationTimeUs(
    sampleTimeUs: Long,
    firstPresentationTimeUs: Long,
    lastAdjustedTimeUs: Long
): Long {
    if (sampleTimeUs < 0L) return (lastAdjustedTimeUs + 1L).coerceAtLeast(0L)
    val base = if (firstPresentationTimeUs == Long.MIN_VALUE) sampleTimeUs else firstPresentationTimeUs
    val shifted = (sampleTimeUs - base).coerceAtLeast(0L)
    return if (lastAdjustedTimeUs >= 0L && shifted < lastAdjustedTimeUs) {
        lastAdjustedTimeUs
    } else {
        shifted
    }
}

internal fun mapExtractorSampleFlagsToCodecFlags(sampleFlags: Int): Int {
    var codecFlags = 0
    if ((sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
        codecFlags = codecFlags or MediaCodec.BUFFER_FLAG_KEY_FRAME
    }
    return codecFlags
}

internal object OfflineAudioTrackExtractor {
    private const val DEFAULT_BUFFER_SIZE = 512 * 1024

    suspend fun extractAudioTrack(context: Context, input: File, output: File) {
        if (!input.isFile || input.length() <= 0L) {
            throw IOException("Offline audio extraction input is missing or empty")
        }
        Timber.i(
            "Audio extraction started: api=%d input=%s (%d bytes)",
            Build.VERSION.SDK_INT,
            input.name,
            input.length()
        )
        try {
            runExtraction(context, input, output)
        } catch (error: Throwable) {
            runCatching { output.delete() }
            throw error
        }
    }

    private suspend fun runExtraction(context: Context, input: File, output: File) {
        val extractor = MediaExtractor()
        var trackMimeSummary = "unreadable"
        var selectedStrategy = OfflineAudioExtractionStrategy.DIRECT_MP4_REMUX
        try {
            try {
                extractor.setDataSource(input.absolutePath)
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                throw IOException(
                    "Offline audio extraction failed to open container: api=${Build.VERSION.SDK_INT} reason=${error.message.orEmpty()}",
                    error
                )
            }
            val trackCount = extractor.trackCount
            val formats = (0 until trackCount).map(extractor::getTrackFormat)
            val mimes = formats.map { it.getString(MediaFormat.KEY_MIME).orEmpty() }
            trackMimeSummary = if (mimes.isEmpty()) "none" else mimes.joinToString(",")
            selectedStrategy = selectOfflineAudioExtractionStrategy(mimes)
            val selectedIndex = selectPreferredAudioTrackIndex(mimes)
            Timber.i(
                "Audio extraction tracks: api=%d tracks=%d mimes=[%s] strategy=%s",
                Build.VERSION.SDK_INT,
                trackCount,
                trackMimeSummary,
                selectedStrategy.label
            )
            if (selectedIndex < 0 || selectedStrategy == OfflineAudioExtractionStrategy.NO_AUDIO_TRACK) {
                throw IOException(
                    "Offline audio extraction failed: no audio track found (api=${Build.VERSION.SDK_INT}, tracks=$trackCount, mimes=[$trackMimeSummary])"
                )
            }
            if (selectedStrategy == OfflineAudioExtractionStrategy.DIRECT_MP4_REMUX) {
                try {
                    remuxSelectedTrack(extractor, selectedIndex, formats[selectedIndex], output)
                    verifyNonEmptyOutput(output, selectedStrategy, trackMimeSummary)
                    Timber.i(
                        "Audio extraction completed: strategy=%s bytes=%d",
                        selectedStrategy.label,
                        output.length()
                    )
                    return
                } catch (remuxError: CancellationException) {
                    throw remuxError
                } catch (remuxError: Throwable) {
                    runCatching { output.delete() }
                    Timber.w(
                        remuxError,
                        "Direct MP4 audio remux failed; attempting Transformer fallback (api=%d mimes=[%s])",
                        Build.VERSION.SDK_INT,
                        trackMimeSummary
                    )
                }
            }
        } finally {
            runCatching { extractor.release() }
        }

        currentCoroutineContext().ensureActive()
        runTransformerFallback(
            context = context,
            input = input,
            output = output,
            trackMimeSummary = trackMimeSummary
        )
        verifyNonEmptyOutput(output, OfflineAudioExtractionStrategy.TRANSFORMER_AAC_FALLBACK, trackMimeSummary)
        Timber.i(
            "Audio extraction completed: strategy=%s bytes=%d",
            OfflineAudioExtractionStrategy.TRANSFORMER_AAC_FALLBACK.label,
            output.length()
        )
    }

    private suspend fun remuxSelectedTrack(
        extractor: MediaExtractor,
        trackIndex: Int,
        trackFormat: MediaFormat,
        output: File
    ) {
        runCatching { output.delete() }
        extractor.selectTrack(trackIndex)
        val maxInputSize = runCatching {
            if (trackFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                trackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            } else {
                0
            }
        }.getOrDefault(0)
        val buffer = ByteBuffer.allocate(resolveExtractorSampleBufferSize(maxInputSize, DEFAULT_BUFFER_SIZE))
        val bufferInfo = MediaCodec.BufferInfo()
        val muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxerStarted = false
        try {
            val muxerTrack = muxer.addTrack(trackFormat)
            muxer.start()
            muxerStarted = true

            var firstPresentationTimeUs = Long.MIN_VALUE
            var lastAdjustedTimeUs = -1L
            var samplesWritten = 0L

            while (true) {
                currentCoroutineContext().ensureActive()
                buffer.clear()
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                if (sampleSize == 0) {
                    if (!extractor.advance()) break
                    continue
                }

                val sampleTimeUs = extractor.sampleTime
                if (firstPresentationTimeUs == Long.MIN_VALUE && sampleTimeUs >= 0L) {
                    firstPresentationTimeUs = sampleTimeUs
                }
                val adjustedTimeUs = normalizeExtractorPresentationTimeUs(
                    sampleTimeUs = sampleTimeUs,
                    firstPresentationTimeUs = firstPresentationTimeUs,
                    lastAdjustedTimeUs = lastAdjustedTimeUs
                )
                lastAdjustedTimeUs = adjustedTimeUs

                bufferInfo.set(
                    0,
                    sampleSize,
                    adjustedTimeUs,
                    mapExtractorSampleFlagsToCodecFlags(extractor.sampleFlags)
                )
                muxer.writeSampleData(muxerTrack, buffer, bufferInfo)
                samplesWritten++
                if (!extractor.advance()) break
            }

            if (samplesWritten <= 0L) {
                throw IOException("Offline audio extraction produced zero audio samples")
            }
            muxer.stop()
            muxerStarted = false
            Timber.d("Audio extraction remux wrote %d samples", samplesWritten)
        } finally {
            if (muxerStarted) {
                runCatching { muxer.stop() }
            }
            runCatching { muxer.release() }
        }
    }

    private suspend fun runTransformerFallback(
        context: Context,
        input: File,
        output: File,
        trackMimeSummary: String
    ) {
        runCatching { output.delete() }
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
            val diagnostics = "api=${Build.VERSION.SDK_INT} strategy=${OfflineAudioExtractionStrategy.TRANSFORMER_AAC_FALLBACK.label} mimes=[$trackMimeSummary] errorCode=${failure.errorCode} (${failure.errorCodeName}) message=${failure.message.orEmpty()}"
            Timber.e(failure, "Offline audio extraction fallback failed: %s", diagnostics)
            throw IOException("Offline audio extraction failed ($diagnostics): ${failure.message.orEmpty()}", failure)
        }
    }

    private fun verifyNonEmptyOutput(
        output: File,
        strategy: OfflineAudioExtractionStrategy,
        trackMimeSummary: String
    ) {
        if (!output.isFile || output.length() <= 0L) {
            throw IOException(
                "Offline audio extraction produced an empty file (api=${Build.VERSION.SDK_INT}, strategy=${strategy.label}, mimes=[$trackMimeSummary])"
            )
        }
    }
}
