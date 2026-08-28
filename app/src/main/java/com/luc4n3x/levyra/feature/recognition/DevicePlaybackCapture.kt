package com.luc4n3x.levyra.feature.recognition

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.os.Build
import androidx.annotation.RequiresApi
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class DevicePlaybackCaptureUnsupportedException :
    Exception("Device playback capture requires Android 10 or newer")

@RequiresApi(Build.VERSION_CODES.Q)
class DevicePlaybackCapture(private val projection: MediaProjection) : AudioCapture {

    @SuppressLint("MissingPermission")
    override suspend fun capture(durationMs: Long): CapturedAudio = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) throw DevicePlaybackCaptureUnsupportedException()
        if (durationMs !in 1L..MicrophoneCapture.MAX_CAPTURE_DURATION_MS) {
            throw MicrophoneCaptureException(
                "Capture duration must be between 1 and ${MicrophoneCapture.MAX_CAPTURE_DURATION_MS} ms"
            )
        }

        val minimumBufferBytes = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minimumBufferBytes <= 0) {
            throw MicrophoneCaptureException("AudioRecord reported an invalid buffer size")
        }
        val bufferBytes = max(minimumBufferBytes, READ_CHUNK_SAMPLE_COUNT * BYTES_PER_SAMPLE) *
            RECORDING_BUFFER_MULTIPLIER

        val configuration = AudioPlaybackCaptureConfiguration.Builder(projection)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()
        val format = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE_HZ)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .build()

        val record = runCatching {
            AudioRecord.Builder()
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferBytes)
                .setAudioPlaybackCaptureConfig(configuration)
                .build()
        }.getOrNull()
        if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
            record?.release()
            throw MicrophoneCaptureException("Unable to initialize device playback capture")
        }

        val targetSampleCount = ((durationMs * SAMPLE_RATE_HZ) / 1000L).toInt().coerceAtLeast(1)
        val output = ShortArray(targetSampleCount)
        val chunk = ShortArray(READ_CHUNK_SAMPLE_COUNT)
        var captured = 0

        try {
            record.startRecording()
            if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                throw MicrophoneCaptureException("Device playback capture failed to start")
            }
            while (captured < targetSampleCount) {
                currentCoroutineContext().ensureActive()
                val toRead = minOf(chunk.size, targetSampleCount - captured)
                val read = record.read(chunk, 0, toRead, AudioRecord.READ_BLOCKING)
                if (read < 0) {
                    throw MicrophoneCaptureException("Device playback capture read failed with code $read")
                }
                if (read == 0) continue
                System.arraycopy(chunk, 0, output, captured, read)
                captured += read
            }
        } finally {
            runCatching { record.stop() }
            record.release()
        }

        CapturedAudio(
            samples = if (captured == output.size) output else output.copyOf(captured),
            sampleRateHz = SAMPLE_RATE_HZ,
            channelCount = CHANNEL_COUNT
        )
    }

    private companion object {
        const val SAMPLE_RATE_HZ = ShazamSignatureGenerator.SAMPLE_RATE_HZ
        const val CHANNEL_COUNT = 1
        const val READ_CHUNK_SAMPLE_COUNT = 4_096
        const val BYTES_PER_SAMPLE = 2
        const val RECORDING_BUFFER_MULTIPLIER = 2
    }
}
