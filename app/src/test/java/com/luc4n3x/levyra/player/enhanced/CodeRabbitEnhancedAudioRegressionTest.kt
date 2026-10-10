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
}
