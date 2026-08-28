package com.luc4n3x.levyra.recognition

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AcousticFingerprintEngineTest {
    @Test
    fun silentAudioDoesNotProduceFingerprint() {
        val samples = ShortArray(AcousticFingerprintEngine.SAMPLE_RATE_HZ * 8)
        assertNull(AcousticFingerprintEngine().fingerprint(samples))
    }

    @Test
    fun fingerprintIsDeterministicAndWireCompatible() {
        val samples = compositeTone(seconds = 4)
        val engine = AcousticFingerprintEngine()
        val first = engine.fingerprint(samples)
        val second = engine.fingerprint(samples)

        assertNotNull(first)
        assertEquals(first, second)
        requireNotNull(first)
        assertTrue(first.peakCount > 0)
        assertTrue(first.sampleDurationMs > 0)

        val header = ByteBuffer.wrap(first.payload).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(0xCAFE2580.toInt(), header.int)
        val storedCrc = header.int
        header.int
        assertEquals(0x94119C00.toInt(), header.int)
        val computedCrc = CRC32().apply {
            update(first.payload, 8, first.payload.size - 8)
        }.value.toInt()
        assertEquals(computedCrc, storedCrc)
        assertTrue(first.toDataUri().startsWith(RecognitionSignature.DATA_URI_PREFIX))
    }

    @Test
    fun sparseFingerprintCanContinuePastDurationTarget() {
        val signature = AcousticFingerprintEngine(
            FingerprintProfile(
                durationTargetSeconds = 0.1,
                peakTarget = Int.MAX_VALUE,
                hardMaxDurationSeconds = 2.0,
                hardPeakLimit = Int.MAX_VALUE
            )
        ).fingerprint(compositeTone(seconds = 2))

        assertNotNull(signature)
        assertTrue(signature!!.sampleDurationMs >= 1_900L)
    }

    @Test
    fun hardDurationBoundPreventsUnboundedAnalysis() {
        val signature = AcousticFingerprintEngine(
            FingerprintProfile(
                durationTargetSeconds = 0.1,
                peakTarget = Int.MAX_VALUE,
                hardMaxDurationSeconds = 1.0,
                hardPeakLimit = Int.MAX_VALUE
            )
        ).fingerprint(compositeTone(seconds = 4))

        assertNotNull(signature)
        assertTrue(signature!!.sampleDurationMs <= 1_000L)
    }

    @Test
    fun shortInputReturnsNull() {
        assertNull(AcousticFingerprintEngine().fingerprint(ShortArray(127)))
    }

    private fun compositeTone(seconds: Int): ShortArray =
        ShortArray(AcousticFingerprintEngine.SAMPLE_RATE_HZ * seconds) { index ->
            val time = index.toDouble() / AcousticFingerprintEngine.SAMPLE_RATE_HZ
            (
                (
                    sin(2.0 * PI * 440.0 * time) +
                        sin(2.0 * PI * 880.0 * time) +
                        0.5 * sin(2.0 * PI * 1_760.0 * time)
                    ) * 12_000.0
                ).toInt().toShort()
        }
}
