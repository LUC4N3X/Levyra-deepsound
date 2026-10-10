package com.luc4n3x.levyra.data.network.byedpi

import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import io.github.dovecoteescapee.byedpi.core.ByeDpiProxy
import io.github.dovecoteescapee.byedpi.core.ByeDpiProxyUIPreferences
import io.github.dovecoteescapee.byedpi.core.DesyncMethod
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
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
    private val LOOPBACK: InetAddress = InetAddress.getByAddress(byteArrayOf(127, 0, 0, 1))
    private const val PROBE_TIMEOUT_MS = 1500
    private const val STARTUP_WAIT_MS = 2000L

    private val lock = ReentrantLock()
    private val stateChanged = lock.newCondition()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var runnerJob: Job? = null
    private var proxyInstance: ByeDpiProxy? = null
    private var generation = 0L

    @Volatile
    private var breaker = ByeDpiCircuitBreaker()

    @Volatile
    private var currentState: ByeDpiState = ByeDpiState.STOPPED

    @Volatile
    private var activePort: Int = 0

    @Volatile
    private var failureMessage: String? = null

    val state: ByeDpiState get() = currentState
    val port: Int get() = activePort
    val lastError: String? get() = failureMessage

    fun isAvailable(): Boolean = ByeDpiProxy.isAvailable()

    fun isRunning(): Boolean = currentState == ByeDpiState.RUNNING && activePort > 0

    fun isEngaged(): Boolean = currentState == ByeDpiState.STARTING || currentState == ByeDpiState.RUNNING

    fun tunnelAddress(): InetSocketAddress? {
        val runningPort = activePort
        if (currentState != ByeDpiState.RUNNING || runningPort <= 0) return null
        return InetSocketAddress(LOOPBACK, runningPort)
    }

    fun acquireTunnel(): InetSocketAddress? {
        if (!awaitRunning() || !breaker.tryAcquire()) return null
        return tunnelAddress()
    }

    fun awaitRunning(timeoutMs: Long = STARTUP_WAIT_MS): Boolean {
        if (currentState != ByeDpiState.STARTING) return currentState == ByeDpiState.RUNNING
        return lock.withLock {
            var remainingNanos = TimeUnit.MILLISECONDS.toNanos(timeoutMs)
            try {
                while (currentState == ByeDpiState.STARTING && remainingNanos > 0L) {
                    remainingNanos = stateChanged.awaitNanos(remainingNanos)
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
            currentState == ByeDpiState.RUNNING
        }
    }

    fun start(preferredPort: Int = DEFAULT_PORT) {
        lock.withLock {
            if (isEngaged()) return
            val previous = proxyInstance
            if (previous != null && previous.stopProxy() != 0) {
                failLocked("Previous ByeDPI instance did not stop")
                return
            }
            proxyInstance = null
            if (!isAvailable()) {
                failLocked("Native byedpi library is not available on this platform")
                return
            }
            val selectedPort = findAvailablePort(preferredPort)
            if (selectedPort <= 0) {
                failLocked("Could not find an available local port for ByeDPI")
                return
            }

            val instance = ByeDpiProxy()
            val startGeneration = ++generation
            proxyInstance = instance
            activePort = selectedPort
            failureMessage = null
            breaker.recordSuccess()
            currentState = ByeDpiState.STARTING
            runnerJob?.cancel()
            runnerJob = scope.launch { runProxy(instance, selectedPort, startGeneration) }
        }
    }

    fun stop() {
        lock.withLock {
            if (currentState == ByeDpiState.STOPPED) return
            generation++
            currentState = ByeDpiState.STOPPED
            val instance = proxyInstance
            proxyInstance = null
            activePort = 0
            failureMessage = null
            breaker.recordSuccess()
            runCatching { instance?.stopProxy() }
            runnerJob?.cancel()
            runnerJob = null
            stateChanged.signalAll()
        }
        Timber.i("ByeDPI stopped")
    }

    fun recordConnectionSuccess() = breaker.recordSuccess()

    fun recordConnectionFailure() {
        breaker.recordFailure()
        if (breaker.isOpen()) Timber.w("ByeDPI reached failure limit; routing suspended until the next probe")
    }

    fun isTemporarilyDegraded(): Boolean = breaker.isOpen()

    internal fun setRunningForTesting(testPort: Int = DEFAULT_PORT) {
        lock.withLock {
            generation++
            currentState = ByeDpiState.RUNNING
            activePort = testPort
            breaker.recordSuccess()
            stateChanged.signalAll()
        }
    }

    internal fun setStartingForTesting(testPort: Int = DEFAULT_PORT) {
        lock.withLock {
            generation++
            currentState = ByeDpiState.STARTING
            activePort = testPort
            breaker.recordSuccess()
        }
    }

    internal fun finishStartingForTesting(running: Boolean) {
        lock.withLock {
            currentState = if (running) ByeDpiState.RUNNING else ByeDpiState.FAILED
            if (!running) activePort = 0
            stateChanged.signalAll()
        }
    }

    internal fun resetForTesting(clockMs: (() -> Long)? = null) {
        lock.withLock {
            generation++
            currentState = ByeDpiState.STOPPED
            activePort = 0
            failureMessage = null
            proxyInstance = null
            runnerJob?.cancel()
            runnerJob = null
            breaker = clockMs?.let { ByeDpiCircuitBreaker(clockMs = it) } ?: ByeDpiCircuitBreaker()
            stateChanged.signalAll()
        }
    }

    private suspend fun runProxy(instance: ByeDpiProxy, port: Int, startGeneration: Long) {
        val preferences = ByeDpiProxyUIPreferences(
            ip = LOCALHOST,
            port = port,
            desyncMethod = DesyncMethod.Disorder,
            splitPosition = 1,
            splitAtHost = true,
            desyncHttp = true,
            desyncHttps = true
        )
        try {
            coroutineScope {
                launch {
                    val exitCode = instance.startProxy(preferences)
                    markFailed(startGeneration, "ByeDPI exited with code $exitCode")
                }
                if (probeSocksPort(LOCALHOST, port, PROBE_TIMEOUT_MS)) {
                    markRunning(startGeneration, port)
                } else {
                    markFailed(startGeneration, "ByeDPI failed local port probe on port $port")
                    instance.stopProxy()
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: Throwable) {
            markFailed(startGeneration, t.message ?: "Unknown ByeDPI startup failure")
        }
    }

    private fun markRunning(startGeneration: Long, port: Int) {
        val promoted = lock.withLock {
            val current = startGeneration == generation && currentState == ByeDpiState.STARTING
            if (current) {
                currentState = ByeDpiState.RUNNING
                stateChanged.signalAll()
            }
            current
        }
        if (promoted) {
            Timber.i("ByeDPI active and verified on %s:%d", LOCALHOST, port)
            LevyraHttpClientFactory.evictIdleConnections()
        }
    }

    private fun markFailed(startGeneration: Long, message: String) {
        lock.withLock {
            if (startGeneration == generation && isEngaged()) failLocked(message)
        }
    }

    private fun failLocked(message: String) {
        currentState = ByeDpiState.FAILED
        failureMessage = message
        activePort = 0
        Timber.w(message)
        stateChanged.signalAll()
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
                if (socksHandshakeSucceeds(host, port)) return@withContext true
                delay(50)
            }
            false
        }

    private fun socksHandshakeSucceeds(host: String, port: Int): Boolean =
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 200)
                socket.getOutputStream().write(byteArrayOf(0x05, 0x01, 0x00))
                val response = ByteArray(2)
                socket.setSoTimeout(300)
                socket.getInputStream().read(response) == 2 && response[0] == 0x05.toByte()
            }
        } catch (_: IOException) {
            false
        }
}
