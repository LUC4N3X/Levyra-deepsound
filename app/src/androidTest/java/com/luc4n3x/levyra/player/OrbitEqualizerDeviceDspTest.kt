package com.luc4n3x.levyra.player

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.luc4n3x.levyra.domain.OrbitEqualizer
import com.luc4n3x.levyra.domain.OrbitPosition
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrbitEqualizerDeviceDspTest {
    private val rate = 48_000
    private val block = 1_024

    @Test
    fun orbitStaysCheapCleanAndUnclippedOnDevice() {
        val processor = LevyraParametricEqualizerAudioProcessor().apply {
            setConfiguration(true, OrbitEqualizer.shape(OrbitPosition(-0.7f, 0.7f), "device").profile)
            configure(AudioFormat(rate, 2, C.ENCODING_PCM_FLOAT))
            flush(AudioProcessor.StreamMetadata.DEFAULT)
        }
        val input = ByteBuffer.allocateDirect(block * 2 * 4).order(ByteOrder.LITTLE_ENDIAN)
        var phase = 0L
        var peak = 0f
        var finite = true
        fun fill() {
            input.clear()
            repeat(block) {
                val t = (phase + it).toDouble() / rate
                val sample = (0.49 * sin(2 * PI * 80 * t) + 0.49 * sin(2 * PI * 3_000 * t)).toFloat()
                input.putFloat(sample)
                input.putFloat(sample)
            }
            phase += block
            input.flip()
        }
        val warmupBlocks = rate / block
        val measuredBlocks = rate * 20 / block
        var elapsed = 0L
        repeat(warmupBlocks + measuredBlocks) { index ->
            fill()
            val start = System.nanoTime()
            processor.queueInput(input)
            val output = processor.output
            if (index >= warmupBlocks) elapsed += System.nanoTime() - start
            while (output.remaining() >= 4) {
                val value = output.float
                if (!value.isFinite()) finite = false
                if (index >= warmupBlocks) peak = maxOf(peak, abs(value))
            }
        }
        val audioSeconds = measuredBlocks.toDouble() * block / rate
        val realtimeFactor = audioSeconds / (elapsed / 1e9)
        val cpuPercent = 100.0 / realtimeFactor
        Log.i("OrbitDeviceDsp", "realtime=${"%.0f".format(realtimeFactor)}x cpu=${"%.3f".format(cpuPercent)}% peak=${"%.3f".format(peak)}")
        assertTrue(finite)
        assertTrue("peak=$peak", peak <= 1f)
        assertTrue("realtime=$realtimeFactor", realtimeFactor > 5)
    }
}
