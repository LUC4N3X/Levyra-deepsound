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

    val requests: SharedFlow<String> = requestFlow.asSharedFlow()

    init {
        writePort(server.localPort)
        scope.launch {
            while (isActive && !server.isClosed) {
                val socket = runCatching { server.accept() }.getOrNull() ?: break
                socket.use { client ->
                    val line = runCatching {
                        client.getInputStream().bufferedReader(StandardCharsets.UTF_8).readLine()
                    }.getOrNull().orEmpty()
                    if (line.startsWith(MESSAGE_PREFIX)) {
                        requestFlow.tryEmit(line.removePrefix(MESSAGE_PREFIX))
                    }
                }
            }
        }
    }

    override fun close() {
        scope.cancel()
        runCatching { server.close() }
        runCatching { lock.release() }
        runCatching { lockChannel.close() }
        runCatching { Files.deleteIfExists(lockPath) }
    }

    private fun writePort(port: Int) {
        val bytes = "$port\n".toByteArray(StandardCharsets.UTF_8)
        lockChannel.truncate(0L)
        lockChannel.position(0L)
        lockChannel.write(ByteBuffer.wrap(bytes))
        lockChannel.force(true)
    }

    companion object {
        fun acquire(payload: String = ""): DesktopInstanceManager? {
            val root = AppPaths.defaultRoot()
            Files.createDirectories(root)
            val lockPath = root.resolve(LOCK_FILE)
            val channel = FileChannel.open(
                lockPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE
            )
            val lock = runCatching { channel.tryLock() }.getOrNull()
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
    }
}
