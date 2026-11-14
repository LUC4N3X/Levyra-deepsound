package com.luc4n3x.levyra.desktop.app

import com.luc4n3x.levyra.desktop.core.storage.AppPaths
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class DesktopInstanceManager private constructor(
    private val lockPath: Path,
    private val lockChannel: FileChannel,
    private val lock: FileLock,
    private val server: ServerSocket
) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val requestFlow = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 15)
    private val closed = AtomicBoolean()
    private val activeClient = AtomicReference<Socket?>()

    val requests: SharedFlow<String> = requestFlow.asSharedFlow()

    init {
        writePort(server.localPort)
        scope.launch {
            while (isActive && !server.isClosed) {
                val socket = runCatching { server.accept() }.getOrNull() ?: break
                activeClient.set(socket)
                socket.use { client ->
                    client.soTimeout = CLIENT_TIMEOUT_MS
                    val line = runCatching {
                        client.getInputStream().bufferedReader(StandardCharsets.UTF_8).let { reader ->
                            val message = StringBuilder()
                            while (message.length <= MAX_MESSAGE_CHARS) {
                                val next = reader.read()
                                if (next < 0 || next == '\n'.code) return@let message.toString().trimEnd('\r')
                                message.append(next.toChar())
                            }
                            ""
                        }
                    }.getOrNull().orEmpty()
                    if (line.startsWith(MESSAGE_PREFIX)) {
                        requestFlow.tryEmit(line.removePrefix(MESSAGE_PREFIX))
                    }
                }
                activeClient.compareAndSet(socket, null)
            }
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        scope.cancel()
        runCatching { server.close() }
        runCatching { activeClient.getAndSet(null)?.close() }
        runCatching { lock.release() }
        runCatching { lockChannel.close() }
    }

    private fun writePort(port: Int) {
        val bytes = "$port\n".toByteArray(StandardCharsets.UTF_8)
        lockChannel.truncate(0L)
        lockChannel.position(0L)
        lockChannel.write(ByteBuffer.wrap(bytes))
        lockChannel.force(true)
    }

    companion object {
        fun acquire(payload: String = "", root: Path = AppPaths.defaultRoot()): DesktopInstanceManager? {
            Files.createDirectories(root)
            val lockPath = root.resolve(LOCK_FILE)
            val channel = FileChannel.open(
                lockPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE
            )
            val lock = runCatching { channel.tryLock(LOCK_OFFSET, 1L, false) }.getOrNull()
            if (lock != null) {
                val server = ServerSocket().apply {
                    reuseAddress = false
                    bind(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 8)
                }
                return DesktopInstanceManager(lockPath, channel, lock, server)
            }

            runCatching { channel.close() }
            signalRunningInstance(lockPath, payload)
            return null
        }

        private fun signalRunningInstance(lockPath: Path, payload: String) {
            repeat(CONNECT_ATTEMPTS) {
                val port = runCatching {
                    Files.readString(lockPath, StandardCharsets.UTF_8)
                        .lineSequence()
                        .firstOrNull()
                        ?.trim()
                        ?.toIntOrNull()
                }.getOrNull()
                if (port != null && port in 1..65535) {
                    val sent = runCatching {
                        Socket().use { socket ->
                            socket.connect(
                                InetSocketAddress(InetAddress.getLoopbackAddress(), port),
                                CONNECT_TIMEOUT_MS
                            )
                            socket.getOutputStream().bufferedWriter(StandardCharsets.UTF_8).use { writer ->
                                writer.write(MESSAGE_PREFIX)
                                writer.write(payload.replace('\n', ' ').replace('\r', ' '))
                                writer.newLine()
                            }
                        }
                    }.isSuccess
                    if (sent) return
                }
                Thread.sleep(CONNECT_RETRY_MS)
            }
        }

        private const val LOCK_FILE = "desktop-instance.lock"
        private const val MESSAGE_PREFIX = "LEVYRA\t"
        private const val CONNECT_ATTEMPTS = 20
        private const val CONNECT_TIMEOUT_MS = 250
        private const val CONNECT_RETRY_MS = 100L
        private const val CLIENT_TIMEOUT_MS = 1_000
        private const val MAX_MESSAGE_CHARS = 16_384
        private const val LOCK_OFFSET = 4_096L
    }
}
