package com.luc4n3x.levyra.player

import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackMemoryDiagnosticsTest {
    @Test
    fun parsesProcStatusAndSmapsRollup() {
        val memory = parsePlaybackProcessMemory(
            status = """
                VmRSS:    456789 kB
                RssAnon:  345678 kB
                Threads:  123
            """.trimIndent(),
            smapsRollup = """
                Pss_Anon:       234567 kB
                Private_Dirty:  223344 kB
                Swap:              4096 kB
                SwapPss:          2048 kB
            """.trimIndent(),
            fileDescriptors = 47
        )

        assertEquals(456789L, memory.rssKb)
        assertEquals(345678L, memory.rssAnonKb)
        assertEquals(234567L, memory.pssAnonKb)
        assertEquals(223344L, memory.privateDirtyKb)
        assertEquals(4096L, memory.swapKb)
        assertEquals(2048L, memory.swapPssKb)
        assertEquals(123, memory.threads)
        assertEquals(47, memory.fileDescriptors)
    }

    @Test
    fun writesBoundedCsvWithSafeMediaIdentity() {
        val directory = createTempDirectory("playback-memory-diagnostics").toFile()
        val output = File(directory, PlaybackMemoryDiagnosticLog.FILE_NAME)
        val log = PlaybackMemoryDiagnosticLog(output, directory)
        log.prepare()

        val appended = log.append(
            PlaybackMemoryDiagnosticSample(
                timestampMs = 1L,
                elapsedRealtimeMs = 2L,
                processMemory = PlaybackProcessMemory(3L, 4L, 5L, 6L, 7L, 8L, 9, 10),
                nativeHeapBytes = 11L,
                javaHeapBytes = 12L,
                playbackState = 3,
                playWhenReady = true,
                isPlaying = true,
                audioSessionId = 13,
                positionBucket30s = 14L,
                mediaId = "track,\"quoted\"\nnext",
                transitionCount = 15L,
                activeResolverJobs = 1,
                prefetchActiveCount = 1,
                recoveryActive = false,
                transitionActive = false,
                memoryGuardHighSamples = 2
            )
        )

        val lines = output.readLines()
        assertTrue(appended)
        assertEquals(PlaybackMemoryDiagnosticLog.HEADER, lines.first())
        assertFalse(lines.last().contains("quoted"))
        assertTrue(lines.last().contains("\"8018624c658700dd\""))
        assertTrue(lines.last().endsWith(",1,false,false,2,-1,-1"))

        log.prepare()
        assertEquals(lines, output.readLines())

        val fullLog = output.readBytes() +
            ByteArray((PlaybackMemoryDiagnosticLog.MAX_FILE_BYTES - output.length()).toInt())
        output.writeBytes(fullLog)
        log.prepare()
        assertEquals(PlaybackMemoryDiagnosticLog.MAX_FILE_BYTES, output.length())
        assertFalse(log.append(sampleForLimitTest()))
        assertEquals(PlaybackMemoryDiagnosticLog.MAX_FILE_BYTES, output.length())
        directory.deleteRecursively()
    }

    @Test
    fun hashesEveryMediaIdentity() {
        assertEquals("5f6b0b4e201f2a7e", diagnosticMediaIdentity("dQw4w9WgXcQ"))
        assertEquals("none", diagnosticMediaIdentity(""))
        assertFalse(diagnosticMediaIdentity("Artist - private local title").contains("private"))
    }

    private fun sampleForLimitTest() = PlaybackMemoryDiagnosticSample(
        timestampMs = 1L,
        elapsedRealtimeMs = 1L,
        processMemory = PlaybackProcessMemory(-1L, -1L, -1L, -1L, -1L, -1L, -1, -1),
        nativeHeapBytes = -1L,
        javaHeapBytes = -1L,
        playbackState = 1,
        playWhenReady = false,
        isPlaying = false,
        audioSessionId = -1,
        positionBucket30s = 0L,
        mediaId = "",
        transitionCount = 0L,
        activeResolverJobs = 0,
        prefetchActiveCount = 0,
        recoveryActive = false,
        transitionActive = false,
        memoryGuardHighSamples = 0
    )
}
