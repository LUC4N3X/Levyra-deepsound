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

        val minBufferBytes = AudioRecord.getMinBufferSize(
            TARGET_SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufferBytes <= 0) {
            throw MicrophoneCaptureException("AudioRecord reported an invalid buffer size")
        }

        val recordingBufferBytes = max(
            minBufferBytes,
            READ_CHUNK_SAMPLE_COUNT * BYTES_PER_SAMPLE
        ) * RECORDING_BUFFER_MULTIPLIER
        val record = createAudioRecord(TARGET_SAMPLE_RATE_HZ, recordingBufferBytes)
            ?: throw MicrophoneCaptureException("Unable to initialize AudioRecord for capture")

        val targetSampleCount = ((durationMs * TARGET_SAMPLE_RATE_HZ) / 1000L)
            .toInt()
            .coerceAtLeast(1)
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
                val read = record.read(chunk, 0, samplesToRead, AudioRecord.READ_BLOCKING)
                if (read < 0) {
                    throw MicrophoneCaptureException("AudioRecord read failed with error code $read")
                }
                if (read == 0) continue
                System.arraycopy(chunk, 0, output, samplesCaptured, read)
                samplesCaptured += read
            }
        } finally {
            runCatching { record.stop() }
            record.release()
        }

        CapturedAudio(
            samples = if (samplesCaptured == output.size) output else output.copyOf(samplesCaptured),
            sampleRateHz = TARGET_SAMPLE_RATE_HZ,
            channelCount = CHANNEL_COUNT
        )
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun createAudioRecord(sampleRate: Int, bufferBytes: Int): AudioRecord? {
        val record = runCatching {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
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
        const val RELIABLE_FINGERPRINT_CAPTURE_DURATION_MS = 8_400L
        internal const val MAX_CAPTURE_DURATION_MS = 15_000L
        private const val TARGET_SAMPLE_RATE_HZ = 16_000
        private const val CHANNEL_COUNT = 1
        private const val READ_CHUNK_SAMPLE_COUNT = 4_096
        private const val BYTES_PER_SAMPLE = 2
        private const val RECORDING_BUFFER_MULTIPLIER = 2
    }
}
