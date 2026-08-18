package com.luc4n3x.levyra.feature.recognition

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class MicrophonePermissionDeniedException : Exception("RECORD_AUDIO permission is not granted")

class MicrophoneCaptureException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class CapturedAudio(
    val samples: ShortArray,
    val sampleRateHz: Int,
    val channelCount: Int
)

fun interface AudioCapture {
    suspend fun capture(durationMs: Long): CapturedAudio
}

class MicrophoneCapture(private val context: Context) : AudioCapture {

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override suspend fun capture(durationMs: Long): CapturedAudio = withContext(Dispatchers.IO) {
        if (durationMs !in 1L..MAX_CAPTURE_DURATION_MS) {
            throw MicrophoneCaptureException("Capture duration must be between 1 and $MAX_CAPTURE_DURATION_MS ms")
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            throw MicrophonePermissionDeniedException()
        }

        val sampleRate = TARGET_SAMPLE_RATE_HZ
        val minBufferBytes = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufferBytes <= 0) {
            throw MicrophoneCaptureException("AudioRecord reported an invalid buffer size")
        }

        val recordingBufferBytes = max(minBufferBytes, READ_CHUNK_SAMPLE_COUNT * BYTES_PER_SAMPLE) *
            RECORDING_BUFFER_MULTIPLIER
        val record = openAudioRecord(sampleRate, recordingBufferBytes)
            ?: throw MicrophoneCaptureException("Unable to initialize AudioRecord for capture")

        val targetFrameCount = ((durationMs * sampleRate) / 1000L).toInt().coerceAtLeast(1)
        val targetSampleCount = targetFrameCount * CHANNEL_COUNT
        val output = ShortArray(targetSampleCount)
        val chunk = ShortArray(READ_CHUNK_SAMPLE_COUNT)
        var samplesCaptured = 0

        try {
            record.startRecording()
            if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                throw MicrophoneCaptureException("AudioRecord failed to enter recording state")
            }
            while (samplesCaptured < targetSampleCount) {
                currentCoroutineContext().ensureActive()
                val samplesToRead = minOf(chunk.size, targetSampleCount - samplesCaptured)
                val read = record.read(chunk, 0, samplesToRead, AudioRecord.READ_NON_BLOCKING)
                if (read < 0) {
                    throw MicrophoneCaptureException("AudioRecord read failed with error code $read")
                }
                if (read == 0) {
                    delay(READ_RETRY_DELAY_MS)
                    continue
                }
                System.arraycopy(chunk, 0, output, samplesCaptured, read)
                samplesCaptured += read
            }
        } finally {
            runCatching { record.stop() }
            record.release()
        }

        val capturedSamples = if (samplesCaptured == output.size) output else output.copyOf(samplesCaptured)
        CapturedAudio(
            samples = capturedSamples,
            sampleRateHz = sampleRate,
            channelCount = CHANNEL_COUNT
        )
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun openAudioRecord(sampleRate: Int, bufferBytes: Int): AudioRecord? {
        return createAudioRecord(MediaRecorder.AudioSource.UNPROCESSED, sampleRate, bufferBytes)
            ?: createAudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, bufferBytes)
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun createAudioRecord(source: Int, sampleRate: Int, bufferBytes: Int): AudioRecord? {
        val record = runCatching {
            AudioRecord(
                source,
                sampleRate,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferBytes
            )
        }.getOrNull()
        if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
            record?.release()
            return null
        }
        return record
    }

    companion object {
        const val RELIABLE_FINGERPRINT_CAPTURE_DURATION_MS = 7_000L
        internal const val MAX_CAPTURE_DURATION_MS = 15_000L
        private const val READ_RETRY_DELAY_MS = 5L
        private const val TARGET_SAMPLE_RATE_HZ = 44_100
        private const val CHANNEL_COUNT = 2
        private const val READ_CHUNK_SAMPLE_COUNT = 4_096
        private const val BYTES_PER_SAMPLE = 2
        private const val RECORDING_BUFFER_MULTIPLIER = 4
    }
}
