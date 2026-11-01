package com.luc4n3x.levyra.player

import android.content.Context
import android.net.Uri
import com.luc4n3x.levyra.domain.Track
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal object GentleWakeToneGenerator {
    const val BUILT_IN_TONE_TRACK_ID = "levyra-gentle-wake-aurora"
    private const val FILE_NAME = "gentle_wake_aurora_loop_v1.wav"
    private const val SAMPLE_RATE = 24_000
    private const val CHANNELS = 2
    private const val DURATION_SECONDS = 16

    fun ensureToneTrack(
        context: Context,
        title: String,
        subtitle: String
    ): Track {
        val file = ensureWavFile(context.applicationContext)
        val uriString = Uri.fromFile(file).toString()
        return Track(
            id = BUILT_IN_TONE_TRACK_ID,
            title = title.ifBlank { "Aurora Harmonics" },
            artist = subtitle.ifBlank { "Levyra Gentle Wake" },
            album = "Gentle Wake",
            durationMs = DURATION_SECONDS * 1_000L,
            streamUrl = uriString,
            videoUrl = "",
            thumbnailUrl = "",
            largeThumbnailUrl = "",
            source = "GentleWakeBuiltIn",
            moodTags = setOf("ambient", "calm", "morning"),
            energy = 24,
            vocal = 10,
            replayScore = 95,
            cacheScore = 100,
            accentStart = 0xFFFFB77C.toInt(),
            accentEnd = 0xFF7B61FF.toInt()
        )
    }

    @Synchronized
    fun ensureWavFile(context: Context): File {
        val dir = File(context.cacheDir, "gentle_wake").apply { mkdirs() }
        val target = File(dir, FILE_NAME)
        val totalFrames = SAMPLE_RATE * DURATION_SECONDS
        val expectedBytes = 44L + totalFrames.toLong() * CHANNELS * 2L
        if (target.exists() && target.length() == expectedBytes) {
            return target
        }

        val pcmBytes = totalFrames * CHANNELS * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(36 + pcmBytes)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(1)
            putShort(CHANNELS.toShort())
            putInt(SAMPLE_RATE)
            putInt(SAMPLE_RATE * CHANNELS * 2)
            putShort((CHANNELS * 2).toShort())
            putShort(16)
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(pcmBytes)
        }.array()

        val chunkFrames = 1_024
        val chunkBuffer = ByteBuffer.allocate(chunkFrames * CHANNELS * 2).order(ByteOrder.LITTLE_ENDIAN)
        val twoPi = 2.0 * PI

        FileOutputStream(target).use { out ->
            out.write(header)
            var frame = 0
            while (frame < totalFrames) {
                chunkBuffer.clear()
                val limit = minOf(totalFrames, frame + chunkFrames)
                while (frame < limit) {
                    val t = frame.toDouble() / SAMPLE_RATE.toDouble()
                    val loopWindow = 0.5 - 0.5 * cos(twoPi * t / DURATION_SECONDS.toDouble())
                    val breath = 0.72 + 0.28 * sin(twoPi * 0.125 * t)
                    val shimmer = 0.5 + 0.5 * sin(twoPi * 0.25 * t)

                    val fRoot = 261.63
                    val fThird = 329.63
                    val fFifth = 392.00
                    val fMajorSeventh = 493.88
                    val fNinth = 587.33

                    val padL = 0.34 * sin(twoPi * fRoot * t) +
                        0.24 * sin(twoPi * fThird * t + 0.3) +
                        0.20 * sin(twoPi * fFifth * t) +
                        0.12 * shimmer * sin(twoPi * fMajorSeventh * t) +
                        0.10 * (1.0 - shimmer) * sin(twoPi * fNinth * t)

                    val padR = 0.34 * sin(twoPi * fRoot * t) +
                        0.24 * sin(twoPi * fThird * t - 0.3) +
                        0.20 * sin(twoPi * fFifth * t + 0.2) +
                        0.12 * (1.0 - shimmer) * sin(twoPi * fMajorSeventh * t) +
                        0.10 * shimmer * sin(twoPi * fNinth * t)

                    val envelope = (0.25 + 0.75 * loopWindow) * breath * 0.55
                    val sampleL = (padL * envelope * 32767.0).toInt().coerceIn(-32767, 32767).toShort()
                    val sampleR = (padR * envelope * 32767.0).toInt().coerceIn(-32767, 32767).toShort()
                    chunkBuffer.putShort(sampleL)
                    chunkBuffer.putShort(sampleR)
                    frame++
                }
                out.write(chunkBuffer.array(), 0, chunkBuffer.position())
            }
        }
        return target
    }
}
