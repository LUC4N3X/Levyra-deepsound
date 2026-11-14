package com.luc4n3x.levyra.desktop.app.state

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SerialDownloadRangeTest {
    @Test fun resumeRequiresExactRemainingRepresentation() {
        assertEquals(100L, serialDownloadLength(206, "bytes 40-99/100", 60L, 40L))
        assertEquals(100L, serialDownloadLength(206, "bytes 40-99/100", -1L, 40L))
        for (header in listOf(null, "bytes 0-59/100", "bytes 40-89/100", "bytes 40-100/100", "bytes 40-99/*")) {
            assertThrows(IOException::class.java) { serialDownloadLength(206, header, 60L, 40L) }
        }
        assertThrows(IOException::class.java) { serialDownloadLength(206, "bytes 40-99/100", 59L, 40L) }
    }

    @Test fun ignoredRangeRestartsWholeFileAndRejectsNonMediaStatuses() {
        assertEquals(100L, serialDownloadLength(200, null, 100L, 40L))
        assertThrows(IOException::class.java) { serialDownloadLength(204, null, 0L, 40L) }
        assertThrows(IOException::class.java) { serialDownloadLength(416, "bytes */100", 0L, 40L) }
    }
}
