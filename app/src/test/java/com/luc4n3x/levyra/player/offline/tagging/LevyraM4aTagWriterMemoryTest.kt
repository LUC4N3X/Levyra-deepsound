package com.luc4n3x.levyra.player.offline.tagging

import java.io.DataOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraM4aTagWriterMemoryTest {
    @Test
    fun tagsLargeAudioWithinConstrainedHeapWithoutChangingPayload() {
        val directory = Files.createTempDirectory("levyra-tag-memory").toFile()
        try {
            for (moovFirst in listOf(true, false)) {
                val input = File(directory, "input-$moovFirst.m4a")
                val output = File(directory, "output-$moovFirst.m4a")
                val log = File(directory, "worker-$moovFirst.log")
                val payloadSize = 24 * 1024 * 1024
                DataOutputStream(input.outputStream().buffered()).use { stream ->
                    stream.writeInt(12)
                    stream.writeBytes("ftypM4A ")
                    if (moovFirst) {
                        stream.writeInt(8)
                        stream.writeBytes("moov")
                    }
                    stream.writeInt(payloadSize + 8)
                    stream.writeBytes("mdat")
                    val chunk = ByteArray(8192) { (it * 31).toByte() }
                    repeat(payloadSize / chunk.size) { stream.write(chunk) }
                    if (!moovFirst) {
                        stream.writeInt(8)
                        stream.writeBytes("moov")
                    }
                }
                val classpath = listOf(
                    M4aTagMemoryWorker::class.java,
                    LevyraM4aTagWriter::class.java,
                    Unit::class.java
                ).map { File(requireNotNull(requireNotNull(it.protectionDomain).codeSource).location.toURI()).path }
                    .distinct().joinToString(File.pathSeparator)
                val javaExecutable = File(System.getProperty("java.home"), "bin/java").path
                val process = ProcessBuilder(
                    javaExecutable, "-Xmx64m", "-cp", classpath,
                    M4aTagMemoryWorker::class.java.name, input.path, output.path
                ).redirectErrorStream(true).redirectOutput(log).start()
                try {
                    assertTrue("Tagging worker timed out", process.waitFor(30, TimeUnit.SECONDS))
                    assertEquals(log.readText(), 0, process.exitValue())
                } finally {
                    if (process.isAlive) process.destroyForcibly()
                }
                assertArrayEquals(payloadDigest(input), payloadDigest(output))
                assertTrue(output.length() > input.length())
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun payloadDigest(file: File): ByteArray = RandomAccessFile(file, "r").use { source ->
        while (source.filePointer < source.length()) {
            val start = source.filePointer
            val size = source.readInt().toLong() and 0xFFFF_FFFFL
            val type = source.readInt()
            require(size >= 8 && start + size <= source.length())
            if (type == 0x6D646174) {
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(8192)
                var remaining = size - 8
                while (remaining > 0) {
                    val count = minOf(remaining, buffer.size.toLong()).toInt()
                    source.readFully(buffer, 0, count)
                    digest.update(buffer, 0, count)
                    remaining -= count
                }
                return@use digest.digest()
            }
            source.seek(start + size)
        }
        error("Missing mdat")
    }
}

object M4aTagMemoryWorker {
    @JvmStatic
    fun main(args: Array<String>) {
        val result = LevyraM4aTagWriter.write(
            File(args[0]), File(args[1]),
            LevyraM4aMetadata(title = "Memory regression", artist = "Artist", album = "Album")
        )
        check(result.success) { result.reason }
    }
}
