package com.luc4n3x.levyra.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import com.luc4n3x.levyra.domain.OrbitEqualizer
import com.luc4n3x.levyra.domain.OrbitPosition
import com.luc4n3x.levyra.domain.ParametricBiquad
import com.luc4n3x.levyra.domain.ParametricEqProfile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrbitEqualizerDspTest {
    private val rate = 48_000

    @Test
    fun `processed audio follows the designed orbit response`() {
        val profile = OrbitEqualizer.shape(OrbitPosition(0.85f, 0.3f), "x").profile
        val probes = floatArrayOf(80f, 1_000f, 10_000f)
        val designed = FloatArray(probes.size).also { ParametricBiquad.responseDb(profile, probes, rate, it) }

        probes.forEachIndexed { index, frequency ->
            val measured = measuredGainDb(profile, frequency)
            println("orbit-dsp response ${frequency.toInt()} Hz designed=${"%.2f".format(designed[index])} measured=${"%.2f".format(measured)}")
            assertEquals(designed[index], measured, 0.1f)
        }
    }

    @Test
    fun `center position is transparent`() {
        val processor = processor(OrbitEqualizer.shape(OrbitPosition.Center, "x").profile, channels = 1)
        val input = FloatArray(4_800) { (0.8 * sin(2 * PI * 997 * it / rate)).toFloat() }
        val output = run(processor, input)
        val maxError = input.indices.maxOf { abs(input[it] - output[it]) }
        println("orbit-dsp center max error=${"%.3e".format(maxError)}")
        assertTrue(maxError < 1e-6f)
    }

    @Test
    fun `boosted positions never clip a near full scale signal`() {
        listOf(OrbitPosition(-1f, 0f), OrbitPosition(1f, 0f), OrbitPosition(0f, 1f), OrbitPosition(-0.7f, -0.7f)).forEach { position ->
            val processor = processor(OrbitEqualizer.shape(position, "x").profile, channels = 1)
            val tones = doubleArrayOf(60.0, 250.0, 1_150.0, 3_300.0, 11_000.0)
            val raw = DoubleArray(rate) { n -> tones.sumOf { sin(2 * PI * it * n / rate) } }
            val scale = 0.99 / raw.maxOf { abs(it) }
            val output = run(processor, FloatArray(raw.size) { (raw[it] * scale).toFloat() })
            val peak = output.drop(rate / 10).maxOf { abs(it) }
            println("orbit-dsp clip check $position peak=${"%.3f".format(peak)}")
            assertTrue("$position peak=$peak", peak <= 1f)
        }
    }

    @Test
    fun `boosted position adds no measurable harmonic distortion`() {
        val processor = processor(OrbitEqualizer.shape(OrbitPosition(0f, 1f), "x").profile, channels = 1)
        val input = FloatArray(rate) { (0.89 * sin(2 * PI * 1_000 * it / rate)).toFloat() }
        val steady = run(processor, input).drop(rate / 2).toFloatArray()
        val fundamental = goertzel(steady, 1_000.0)
        val harmonics = sqrt((2..10).sumOf { goertzel(steady, 1_000.0 * it).let { level -> level * level } })
        val thdDb = 20 * log10(harmonics / fundamental)
        println("orbit-dsp THD 1 kHz forward pole=${"%.1f".format(thdDb)} dB")
        assertTrue(thdDb < -100)
    }

    @Test
    fun `moving the orbit while playing produces no clicks`() {
        val processor = processor(OrbitEqualizer.shape(OrbitPosition.Center, "x").profile, channels = 1)
        val block = 480
        var phase = 0
        var previous = 0f
        var maxStep = 0f
        repeat(200) { index ->
            if (index % 10 == 5) {
                val angle = index / 200.0 * 2 * PI
                processor.setConfiguration(true, OrbitEqualizer.shape(OrbitPosition(cos(angle).toFloat(), sin(angle).toFloat()), "x").profile)
            }
            val input = FloatArray(block) { (0.5 * sin(2 * PI * 440 * (phase + it) / rate)).toFloat() }
            phase += block
            run(processor, input).forEach { sample ->
                maxStep = maxOf(maxStep, abs(sample - previous))
                previous = sample
            }
        }
        val naturalStep = (0.5 * 2 * PI * 440 / rate).toFloat()
        println("orbit-dsp max step=${"%.4f".format(maxStep)} natural=${"%.4f".format(naturalStep)}")
        assertTrue(maxStep < naturalStep * 2f)
    }

    @Test
    fun `stereo processing is far faster than real time`() {
        val processor = processor(OrbitEqualizer.shape(OrbitPosition(0.6f, -0.4f), "x").profile, channels = 2)
        val input = FloatArray(rate * 2) { (0.3 * sin(it * 0.01)).toFloat() }
        repeat(3) { run(processor, input) }
        val seconds = 10
        val start = System.nanoTime()
        repeat(seconds) { run(processor, input) }
        val elapsed = (System.nanoTime() - start) / 1e9
        val realtimeFactor = seconds / elapsed
        println("orbit-dsp jvm realtime factor=${"%.0f".format(realtimeFactor)}x ns/frame=${"%.1f".format(elapsed * 1e9 / (seconds * rate))}")
        assertTrue(realtimeFactor > 10)
    }

    private fun measuredGainDb(profile: ParametricEqProfile, frequency: Float): Float {
        val processor = processor(profile, channels = 1)
        val input = FloatArray(rate) { (0.25 * sin(2 * PI * frequency * it / rate)).toFloat() }
        val steady = run(processor, input).drop(rate / 2).toFloatArray()
        val reference = input.drop(rate / 2).toFloatArray()
        return (20 * log10(goertzel(steady, frequency.toDouble()) / goertzel(reference, frequency.toDouble()))).toFloat()
    }

    private fun processor(profile: ParametricEqProfile, channels: Int) =
        LevyraParametricEqualizerAudioProcessor().apply {
            setConfiguration(true, profile)
            configure(AudioFormat(rate, channels, C.ENCODING_PCM_FLOAT))
            flush(AudioProcessor.StreamMetadata.DEFAULT)
        }

    private fun run(processor: LevyraParametricEqualizerAudioProcessor, samples: FloatArray): FloatArray {
        val input = ByteBuffer.allocateDirect(samples.size * 4).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach(input::putFloat)
        input.flip()
        processor.queueInput(input)
        val output = processor.output.order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(output.remaining() / 4) { output.float }
    }

    private fun goertzel(samples: FloatArray, frequency: Double): Double {
        val omega = 2 * PI * frequency / rate
        val coefficient = 2 * cos(omega)
        var s1 = 0.0
        var s2 = 0.0
        samples.forEachIndexed { index, sample ->
            val window = 0.5 - 0.5 * cos(2 * PI * index / (samples.size - 1))
            val s0 = sample * window + coefficient * s1 - s2
            s2 = s1
            s1 = s0
        }
        return sqrt(s1 * s1 + s2 * s2 - coefficient * s1 * s2)
    }
}
