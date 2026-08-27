package com.luc4n3x.levyra.runtime

import org.junit.Assert.assertEquals
import org.junit.Test

class BoundedFlightRecorderTest {
    @Test
    fun recorderRetainsOnlyTheNewestEvents() {
        val recorder = BoundedFlightRecorder(capacity = 3)

        repeat(5) { index ->
            recorder.record(
                HotOperationEvent(
                    timestampMs = index.toLong(),
                    uptimeMs = index.toLong(),
                    operation = DiagnosticOperation.RESOLVER_ATTEMPT
                )
            )
        }

        assertEquals(listOf(2L, 3L, 4L), recorder.snapshot().map { it.timestampMs })
    }
}
