package com.luc4n3x.levyra.data.network.byedpi

import io.github.dovecoteescapee.byedpi.core.ByeDpiProxy
import io.github.dovecoteescapee.byedpi.core.ByeDpiProxyUIPreferences
import io.github.dovecoteescapee.byedpi.core.DesyncMethod
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

enum class ByeDpiState {
    STOPPED,
    STARTING,
    RUNNING,
    FAILED
}

object ByeDpiSupervisor {
    private const val DEFAULT_PORT = 1080
    private const val LOCALHOST = "127.0.0.1"
    private const val PROBE_TIMEOUT_MS = 1500
    private const val MAX_CONSECUTIVE_FAILURES = 3

    private val lock = Any()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var runnerJob: Job? = null
    private var proxyProcessJob: Job? = null
    private var proxyInstance: ByeDpiProxy? = null

    @Volatile
    private var currentState: ByeDpiState = ByeDpiState.STOPPED

    @Volatile
    private var activePort: Int = 0

    @Volatile
    private var failureMessage: String? = null

    private val consecutiveFailures = AtomicInteger(0)

    val state: ByeDpiState get() = currentState
    val port: Int get() = activePort
    val lastError: String? get() = failureMessage

    fun isAvailable(): Boolean = ByeDpiProxy.isAvailable()

    fun isRunning(): Boolean = currentState == ByeDpiState.RUNNING && activePort > 0

    fun proxy(): Proxy? {
        if (!isRunning()) return null
        return runCatching {
            Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved(LOCALHOST, activePort))
        }.getOrNull()
    }

    fun start(preferredPort: Int = DEFAULT_PORT) {
        synchronized(lock) {
            if (currentState == ByeDpiState.RUNNING || currentState == ByeDpiState.STARTING) {
                return
            }
            if (!isAvailable()) {
                currentState = ByeDpiState.FAILED
                failureMessage = "Native byedpi library is not available on this platform"
                Timber.w(failureMessage)
                return
            }

            currentState = ByeDpiState.STARTING
            failureMessage = null

            runnerJob?.cancel()
            runnerJob = null
            val instance = ByeDpiProxy()
            proxyInstance = instance

            val selectedPort = findAvailablePort(preferredPort)
            if (selectedPort <= 0) {
                currentState = ByeDpiState.FAILED
                failureMessage = "Could not find an available local port for ByeDPI"
                Timber.e(failureMessage)
                return
            }

            activePort = selectedPort

            val prefs = ByeDpiProxyUIPreferences(
                ip = LOCALHOST,
                port = selectedPort,
                desyncMethod = DesyncMethod.Disorder,
                splitPosition = 1,
                splitAtHost = true,
                desyncHttp = true,
                desyncHttps = true
            )

            runnerJob?.cancel()
            runnerJob = scope.launch {
                try {
                    proxyProcessJob?.cancel()
                    proxyProcessJob = launch {
                        val exitCode = instance.startProxy(prefs)
                        Timber.i("ByeDPI exited with code $exitCode")
                    }

                    val probeSuccess = probeSocksPort(LOCALHOST, selectedPort, PROBE_TIMEOUT_MS)
                    if (probeSuccess) {
                        currentState = ByeDpiState.RUNNING
                        consecutiveFailures.set(0)
                        Timber.i("ByeDPI active and verified on $LOCALHOST:$selectedPort")
                    } else {
                        currentState = ByeDpiState.FAILED
                        failureMessage = "ByeDPI failed local port probe on port $selectedPort"
                        Timber.w(failureMessage)
                        instance.stopProxy()
                        proxyProcessJob?.cancel()
                        proxyProcessJob = null
                    }
                } catch (t: Throwable) {
                    currentState = ByeDpiState.FAILED
                    failureMessage = t.message ?: "Unknown ByeDPI startup failure"
                    Timber.e(t, "ByeDPI failure: %s", failureMessage)
                }
            }
        }
    }

    fun stop() {
        synchronized(lock) {
            if (currentState == ByeDpiState.STOPPED) return
            currentState = ByeDpiState.STOPPED
            val instance = proxyInstance
            proxyInstance = null
            proxyProcessJob?.cancel()
            proxyProcessJob = null
            runnerJob?.cancel()
            runnerJob = null
            activePort = 0
            failureMessage = null
            consecutiveFailures.set(0)

            scope.launch {
                withContext(NonCancellable) {
                    runCatching { instance?.stopProxy() }
                    Timber.i("ByeDPI stopped")
                }
            }
        }
    }

    fun recordConnectionSuccess() {
        consecutiveFailures.set(0)
    }

    fun recordConnectionFailure() {
        val count = consecutiveFailures.incrementAndGet()
        if (count >= MAX_CONSECUTIVE_FAILURES) {
            Timber.w("ByeDPI reached failure limit ($count); temporary fail-safe bypass active")
        }
    }

    fun isTemporarilyDegraded(): Boolean = consecutiveFailures.get() >= MAX_CONSECUTIVE_FAILURES

    internal fun setRunningForTesting(testPort: Int = DEFAULT_PORT) {
        currentState = ByeDpiState.RUNNING
        activePort = testPort
        consecutiveFailures.set(0)
    }

    internal fun resetForTesting() {
        currentState = ByeDpiState.STOPPED
        activePort = 0
        consecutiveFailures.set(0)
    }

    private fun findAvailablePort(preferred: Int): Int {
        if (isPortFree(preferred)) return preferred
        return runCatching {
            ServerSocket(0).use { it.localPort }
        }.getOrDefault(0)
    }

    private fun isPortFree(port: Int): Boolean {
        if (port !in 1024..65535) return false
        return runCatching {
            ServerSocket(port).use { true }
        }.getOrDefault(false)
    }

    private suspend fun probeSocksPort(host: String, port: Int, timeoutMs: Int): Boolean =
        withContext(Dispatchers.IO) {
            val deadline = System.currentTimeMillis() + timeoutMs
            while (System.currentTimeMillis() < deadline) {
                try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(host, port), 200)
                        socket.getOutputStream().write(byteArrayOf(0x05, 0x01, 0x00))
                        val resp = ByteArray(2)
                        socket.setSoTimeout(300)
                        val read = socket.getInputStream().read(resp)
                        if (read == 2 && resp[0] == 0x05.toByte()) {
                            return@withContext true
                        }
                    }
                } catch (_: IOException) {
                    delay(50)
                }
            }
            false
        }
}
