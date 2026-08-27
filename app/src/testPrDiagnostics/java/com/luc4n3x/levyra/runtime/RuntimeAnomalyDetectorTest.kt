package com.luc4n3x.levyra.runtime

import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeAnomalyDetectorTest {
    @Test
    fun detectsMonotonicMemoryGrowth() {
        val harness = DetectorHarness()

        repeat(5) { index ->
            harness.record(
                MemorySampleEvent(
                    timestampMs = index * 30_000L,
                    uptimeMs = index * 30_000L,
                    pssKb = 100_000L + index * 16_384L,
                    javaHeapKb = 20_000L,
                    nativeHeapKb = 30_000L,
                    rssKb = null,
                    threadCount = 20,
                    peakPssKb = 100_000L + index * 16_384L
                )
            )
        }

        assertTrue(harness.anomalies().any { it.type == AnomalyType.MEMORY_GROWTH })
    }

    @Test
    fun detectsResolverRetryStorm() {
        val harness = DetectorHarness()

        repeat(8) { index ->
            harness.record(
                ResolverEvent(
                    timestampMs = index * 5_000L,
                    uptimeMs = index * 5_000L,
                    mode = PlaybackMode.AUDIO,
                    strategy = ResolverStrategy.DIRECT,
                    client = ResolverClient.ANDROID_VR,
                    attempt = 1,
                    latencyMs = 100L,
                    outcome = DiagnosticOutcome.FAILURE,
                    failure = FailureCategory.NETWORK
                )
            )
        }

        assertTrue(harness.anomalies().any { it.type == AnomalyType.RESOLVER_RETRY_STORM })
    }

    @Test
    fun detectsGenericHotOperationStorm() {
        val harness = DetectorHarness()

        repeat(20) { index ->
            harness.record(
                HotOperationEvent(
                    timestampMs = index * 1_000L,
                    uptimeMs = index * 1_000L,
                    operation = DiagnosticOperation.PLAYER_PREPARE
                )
            )
        }

        assertTrue(harness.anomalies().any { it.type == AnomalyType.HOT_OPERATION_STORM })
    }

    private class DetectorHarness {
        private val recorder = BoundedFlightRecorder()
        private val detector = RuntimeAnomalyDetector()

        fun record(event: DiagnosticEvent) {
            recorder.record(event)
            detector.observe(event)
        }

        fun anomalies(): List<DiagnosticAnomaly> = detector.snapshot()
    }
}
