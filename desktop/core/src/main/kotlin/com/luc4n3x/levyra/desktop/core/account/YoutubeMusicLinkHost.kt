package com.luc4n3x.levyra.desktop.core.account

import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicDeviceLink
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicLinkChannel
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicLinkClient
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicLinkDevice
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicLinkException
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicLinkOffer
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicLinkPayload
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicLinkResult
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicLinkStatus
import java.io.File
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.security.KeyPair
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible

data class LinkAddress(val interfaceName: String, val ip: String)

enum class LinkRejection { InvalidCode, SignInFailed }

sealed interface YoutubeMusicLinkState {
    data object Idle : YoutubeMusicLinkState
    data object Starting : YoutubeMusicLinkState
    data object NoNetwork : YoutubeMusicLinkState
    data class Waiting(val code: String, val addresses: List<LinkAddress>, val expiresAtMs: Long) : YoutubeMusicLinkState
    data object PhoneConnected : YoutubeMusicLinkState
    data object SigningIn : YoutubeMusicLinkState
    data class Done(val accounts: List<String>) : YoutubeMusicLinkState
    data class Rejected(val reason: LinkRejection) : YoutubeMusicLinkState
    data object Expired : YoutubeMusicLinkState
    data object Failed : YoutubeMusicLinkState
}

class YoutubeMusicLinkHost internal constructor(
    private val signIn: suspend (YoutubeMusicLinkPayload) -> YoutubeMusicLinkResult,
    private val addresses: () -> List<LinkAddress> = ::reachableLinkAddresses,
    private val device: () -> YoutubeMusicLinkDevice = ::localLinkDevice,
    private val clock: () -> Long = System::currentTimeMillis,
    private val sessionMs: Long = SESSION_MS,
    private val confirmMs: Int = CONFIRM_MS
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<YoutubeMusicLinkState>(YoutubeMusicLinkState.Idle)
    val state: StateFlow<YoutubeMusicLinkState> = _state.asStateFlow()

    @Volatile
    private var session: Session? = null

    private class Session(
        val server: ServerSocket,
        val keys: KeyPair,
        val secret: ByteArray,
        val waiting: YoutubeMusicLinkState.Waiting,
        val busy: AtomicBoolean = AtomicBoolean(false),
        val spent: AtomicBoolean = AtomicBoolean(false)
    ) {
        var acceptJob: Job? = null
        var expiryJob: Job? = null

        @Volatile
        var serveJob: Job? = null

        @Volatile
        var client: Socket? = null
    }

    @Synchronized
    fun start() {
        close()
        _state.value = YoutubeMusicLinkState.Starting
        val found = addresses().take(MAX_ADDRESSES)
        if (found.isEmpty()) {
            _state.value = YoutubeMusicLinkState.NoNetwork
            return
        }
        val server = try {
            ServerSocket(0, BACKLOG)
        } catch (_: IOException) {
            _state.value = YoutubeMusicLinkState.Failed
            return
        }
        val keys = YoutubeMusicDeviceLink.newHostKeys()
        val secret = YoutubeMusicDeviceLink.newSession()
        val offer = YoutubeMusicLinkOffer(found.map { it.ip }, server.localPort, keys.public.encoded, secret)
        val waiting = YoutubeMusicLinkState.Waiting(offer.uri(), found, clock() + sessionMs)
        val current = Session(server, keys, secret, waiting)
        session = current
        _state.value = waiting
        current.acceptJob = scope.launch { acceptLoop(current) }
        current.expiryJob = scope.launch {
            delay(sessionMs)
            synchronized(this@YoutubeMusicLinkHost) {
                if (session === current && !current.spent.get()) {
                    close(current)
                    _state.value = YoutubeMusicLinkState.Expired
                }
            }
        }
    }

    @Synchronized
    fun stop() {
        close()
        _state.value = YoutubeMusicLinkState.Idle
    }

    private fun close(target: Session? = session) {
        target ?: return
        if (session === target) session = null
        target.expiryJob?.cancel()
        target.acceptJob?.cancel()
        target.serveJob?.cancel()
        runCatching { target.client?.close() }
        runCatching { target.server.close() }
        target.secret.fill(0)
    }

    private suspend fun acceptLoop(current: Session) {
        while (scope.isActive && session === current) {
            val socket = try {
                runInterruptible { current.server.accept() }
            } catch (error: CancellationException) {
                throw error
            } catch (_: IOException) {
                return
            }
            if (!current.busy.compareAndSet(false, true)) {
                refuse(socket, YoutubeMusicLinkStatus.Closed)
                continue
            }
            current.client = socket
            current.serveJob = scope.launch {
                try {
                    if (session === current) serve(current, socket)
                } finally {
                    current.busy.set(false)
                    current.client = null
                    runCatching { socket.close() }
                }
            }
        }
    }

    private suspend fun serve(current: Session, socket: Socket) {
        socket.soTimeout = HELLO_TIMEOUT_MS
        socket.tcpNoDelay = true
        val input = socket.getInputStream()
        val output = socket.getOutputStream()
        val channel = try {
            val hello = YoutubeMusicDeviceLink.readMessage(input)
            val phoneKey = YoutubeMusicDeviceLink.phoneKeyOf(hello)
            YoutubeMusicDeviceLink.channel(current.keys.private, phoneKey, current.secret).also { channel ->
                try {
                    channel.open(YoutubeMusicDeviceLink.HELLO, false, YoutubeMusicDeviceLink.sealedOf(hello, YoutubeMusicDeviceLink.HELLO))
                } catch (error: Exception) {
                    channel.close()
                    throw error
                }
            }
        } catch (_: Exception) {
            refuse(socket, YoutubeMusicLinkStatus.Rejected)
            return
        }
        try {
            if (session !== current) return refuse(socket, YoutubeMusicLinkStatus.Closed)
            YoutubeMusicDeviceLink.writeMessage(
                output,
                YoutubeMusicDeviceLink.message(
                    YoutubeMusicDeviceLink.HELLO,
                    channel.seal(YoutubeMusicDeviceLink.HELLO, true, YoutubeMusicDeviceLink.encodeDevice(device()))
                )
            )
            publish(current, YoutubeMusicLinkState.PhoneConnected)
            socket.soTimeout = confirmMs
            val message = try {
                YoutubeMusicDeviceLink.readMessage(input)
            } catch (_: SocketTimeoutException) {
                return backToWaiting(current)
            } catch (_: IOException) {
                return backToWaiting(current)
            }
            when (message.optString("t")) {
                YoutubeMusicDeviceLink.CANCEL -> {
                    runCatching { channel.open(YoutubeMusicDeviceLink.CANCEL, false, YoutubeMusicDeviceLink.sealedOf(message, YoutubeMusicDeviceLink.CANCEL)) }
                    backToWaiting(current)
                }
                YoutubeMusicDeviceLink.LOGINS -> signInFromPhone(current, output, channel, message)
                else -> backToWaiting(current)
            }
        } finally {
            channel.close()
        }
    }

    private suspend fun signInFromPhone(current: Session, output: java.io.OutputStream, channel: YoutubeMusicLinkChannel, message: org.json.JSONObject) {
        val payload = try {
            val plain = channel.open(YoutubeMusicDeviceLink.LOGINS, false, YoutubeMusicDeviceLink.sealedOf(message, YoutubeMusicDeviceLink.LOGINS))
            try {
                YoutubeMusicDeviceLink.decodePayload(plain)
            } finally {
                plain.fill(0)
            }
        } catch (_: YoutubeMusicLinkException) {
            reply(output, channel, YoutubeMusicLinkResult(YoutubeMusicLinkStatus.Rejected, emptyList()))
            publish(current, YoutubeMusicLinkState.Rejected(LinkRejection.InvalidCode))
            return
        }
        val claimed = synchronized(this) { session === current && current.spent.compareAndSet(false, true) }
        if (!claimed) {
            runCatching { reply(output, channel, YoutubeMusicLinkResult(YoutubeMusicLinkStatus.Closed, emptyList())) }
            return
        }
        publish(current, YoutubeMusicLinkState.SigningIn)
        val result = try {
            signIn(payload)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            YoutubeMusicLinkResult(YoutubeMusicLinkStatus.SignInFailed, emptyList())
        }
        runCatching { reply(output, channel, result) }
        finishSignIn(current, result)
    }

    private fun finishSignIn(current: Session, result: YoutubeMusicLinkResult) = synchronized(this) {
        if (session !== current) return@synchronized
        if (result.status == YoutubeMusicLinkStatus.Accepted) {
            close(current)
            _state.value = YoutubeMusicLinkState.Done(result.accounts)
        } else if (clock() >= current.waiting.expiresAtMs) {
            close(current)
            _state.value = YoutubeMusicLinkState.Expired
        } else {
            current.spent.set(false)
            _state.value = YoutubeMusicLinkState.Rejected(
                if (result.status == YoutubeMusicLinkStatus.Rejected) LinkRejection.InvalidCode else LinkRejection.SignInFailed
            )
        }
    }

    private fun backToWaiting(current: Session) {
        publish(current, current.waiting)
    }

    private fun publish(current: Session, next: YoutubeMusicLinkState) {
        synchronized(this) { if (session === current) _state.value = next }
    }

    private fun reply(output: java.io.OutputStream, channel: YoutubeMusicLinkChannel, result: YoutubeMusicLinkResult) {
        YoutubeMusicDeviceLink.writeMessage(
            output,
            YoutubeMusicDeviceLink.message(YoutubeMusicDeviceLink.LOGINS, channel.seal(YoutubeMusicDeviceLink.LOGINS, true, YoutubeMusicDeviceLink.encodeResult(result)))
        )
    }

    private fun refuse(socket: Socket, status: YoutubeMusicLinkStatus) {
        runCatching {
            YoutubeMusicDeviceLink.writeMessage(
                socket.getOutputStream(),
                org.json.JSONObject().put("t", YoutubeMusicLinkClient.STATUS).put("s", status.code)
            )
        }
        runCatching { socket.close() }
    }

    fun dispose() {
        stop()
        scope.cancel()
    }

    companion object {
        const val SESSION_MS = 5L * 60L * 1_000L
        private const val CONFIRM_MS = 2 * 60 * 1_000
        private const val HELLO_TIMEOUT_MS = 8_000
        private const val BACKLOG = 4
        private const val MAX_ADDRESSES = 6
    }
}

fun reachableLinkAddresses(): List<LinkAddress> =
    runCatching { NetworkInterface.getNetworkInterfaces()?.toList().orEmpty() }
        .getOrDefault(emptyList())
        .asSequence()
        .filter { runCatching { it.isUp && !it.isLoopback && !it.isVirtual }.getOrDefault(false) }
        .filterNot { nic -> HOST_ONLY_PREFIXES.any { nic.name.startsWith(it) } }
        .sortedBy { nic -> linkPriority(nic.name, nic.displayName.orEmpty()) }
        .flatMap { nic ->
            nic.inetAddresses.asSequence()
                .filterIsInstance<Inet4Address>()
                .mapNotNull { address -> address.hostAddress?.let { LinkAddress(nic.displayName ?: nic.name, it) } }
        }
        .filter { YoutubeMusicDeviceLink.isUsableIpv4(it.ip) }
        .distinctBy { it.ip }
        .toList()

internal fun linkPriority(name: String, displayName: String): Int {
    val label = "$name $displayName".lowercase()
    return when {
        HOST_ONLY_PREFIXES.any { label.contains(it) } || label.contains("virtual") || label.contains("hyper-v") -> 3
        label.contains("wi-fi") || label.contains("wlan") || label.contains("wireless") || label.startsWith("wl") -> 0
        label.contains("ethernet") || label.startsWith("eth") || label.startsWith("en") -> 1
        else -> 2
    }
}

internal fun localLinkDevice(): YoutubeMusicLinkDevice {
    val osName = System.getProperty("os.name").orEmpty()
    val name = when {
        osName.startsWith("Windows") -> System.getenv("COMPUTERNAME")
        else -> System.getenv("HOSTNAME") ?: runCatching { File("/etc/hostname").readText().trim() }.getOrNull()
    }?.takeIf { it.isNotBlank() }
        ?: runCatching { InetAddress.getLocalHost().hostName }.getOrNull()
        ?: "Levyra Desktop"
    return YoutubeMusicLinkDevice(name.take(48), osName.take(48))
}

private val HOST_ONLY_PREFIXES = listOf("docker", "br-", "veth", "virbr", "vboxnet", "vmnet", "podman", "cni", "vethernet")
