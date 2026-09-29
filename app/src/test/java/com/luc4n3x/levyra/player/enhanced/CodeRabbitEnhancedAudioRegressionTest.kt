package com.luc4n3x.levyra.player.enhanced

import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeRabbitEnhancedAudioRegressionTest {
    @Test
    fun silentSideFramesAdvanceSideFilterStateBeforeSideReturns() {
        val sampleRate = 44_100
        val metrics = EnhancedAudioMetrics(
            deficitConfidence = 0.9f,
            adaptiveResidualGain = 0.45f,
            stereoCoherence = 0.8f,
            bypassed = false
        )
        val staleCandidate = DspRestorationEngine().apply {
            configure(sampleRate, 2, EnhancedAudioConfig())
        }
        val cleanReference = DspRestorationEngine().apply {
            configure(sampleRate, 2, EnhancedAudioConfig())
        }
        val firstSide = antiPhaseSine(sampleRate, frames = 768, frequencyHz = 9_000.0)
        val silence = FloatArray(8_192 * 2)
        val returningSide = antiPhaseSine(sampleRate, frames = 768, frequencyHz = 13_000.0)
        val scratch = FloatArray(maxOf(firstSide.size, silence.size, returningSide.size))

        assertTrue(staleCandidate.process(firstSide, scratch, 0, 768, metrics))
        assertTrue(staleCandidate.process(silence, scratch, 0, 8_192, metrics))
        assertTrue(cleanReference.process(silence, scratch, 0, 8_192, metrics))

        val afterGap = FloatArray(returningSide.size)
        val reference = FloatArray(returningSide.size)
        assertTrue(staleCandidate.process(returningSide, afterGap, 0, 768, metrics))
        assertTrue(cleanReference.process(returningSide, reference, 0, 768, metrics))

        val maxDifference = afterGap.indices.maxOf { index -> abs(afterGap[index] - reference[index]).toDouble() }
        assertTrue("Side filter history survived a long silent gap: maxDifference=$maxDifference", maxDifference < 5e-4)
    }

    @Test
    fun stateSetterDoesNotMutateAudioThreadMetricsScratchObject() {
        val source = sequenceOf(
            Path.of("app/src/main/java/com/luc4n3x/levyra/player/enhanced/EnhancedAudioProcessor.kt"),
            Path.of("src/main/java/com/luc4n3x/levyra/player/enhanced/EnhancedAudioProcessor.kt")
        ).firstOrNull(Files::exists) ?: error("EnhancedAudioProcessor.kt not found")
        val content = Files.readString(source)
        val updateState = content.substringAfter("private fun updateState()")
            .substringBefore("private fun maybeEmitMetrics")

        assertFalse("Main-thread state updates must not mutate the audio-thread scratch metrics", updateState.contains("mutableMetrics"))
    }

    private fun antiPhaseSine(sampleRate: Int, frames: Int, frequencyHz: Double): FloatArray {
        val output = FloatArray(frames * 2)
        for (frame in 0 until frames) {
            val sample = sin(2.0 * PI * frequencyHz * frame / sampleRate).toFloat() * 0.45f
            output[frame * 2] = sample
            output[frame * 2 + 1] = -sample
        }
        return output
    }
}
