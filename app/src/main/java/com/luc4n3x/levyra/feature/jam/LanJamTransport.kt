package com.luc4n3x.levyra.feature.jam

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

internal const val JAM_MAX_LINE_CHARS = JamProtocol.MAX_MESSAGE_BYTES
private const val JAM_CONNECT_TIMEOUT_MS = 6_000
private const val JAM_HANDSHAKE_TIMEOUT_MS = 5_000L
private const val JAM_SOCKET_TIMEOUT_MS = 45_000
private const val JAM_MAX_PENDING_HANDSHAKES = 8
private const val JAM_EVENT_BUFFER = 64

internal fun BufferedReader.readBoundedLine(maxChars: Int = JAM_MAX_LINE_CHARS): String? {
    val builder = StringBuilder()
    while (true) {
        val value = read()
        if (value < 0) return if (builder.isEmpty()) null else builder.toString()
        val character = value.toChar()
        if (character == '\n') return builder.toString()
        if (character == '\r') continue
        if (builder.length >= maxChars) return null
        builder.append(character)
    }
}

internal fun localJamAddress(): String? = runCatching {
    NetworkInterface.getNetworkInterfaces()
        ?.toList()
        ?.asSequence()
        ?.filter { it.isUp && !it.isLoopback }
        ?.flatMap { it.inetAddresses.toList().asSequence() }
        ?.mapNotNull { address -> address.hostAddress?.substringBefore('%') }
        ?.firstOrNull(JamSessionCode::isPrivateIpv4)
}.getOrNull()

class LanJamHostTransport : JamHostTransport {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _events = MutableSharedFlow<JamHostEvent>(extraBufferCapacity = JAM_EVENT_BUFFER)
    override val events: Flow<JamHostEvent> = _events.asSharedFlow()

    private val clients = ConcurrentHashMap<String, JamClientConnection>()
    private val pendingSockets = ConcurrentHashMap.newKeySet<Socket>()
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null

    override suspend fun start(secret: String): JamSessionCode? = withContext(Dispatchers.IO) {
        stop()
        val address = localJamAddress() ?: return@withContext null
        val socket = runCatching { ServerSocket() }.getOrNull() ?: return@withContext null
        val bound = runCatching {
            socket.reuseAddress = true
            socket.bind(InetSocketAddress(InetAddress.getByName(address), 0))
            socket
        }.getOrElse {
            runCatching { socket.close() }
            Timber.w(it, "Jam host could not bind a local port")
            return@withContext null
        }
        if (bound.localPort !in JamSessionCode.MIN_PORT..JamSessionCode.MAX_PORT) {
            runCatching { bound.close() }
            return@withContext null
        }
        serverSocket = bound
        acceptJob = scope.launch { acceptLoop(bound, secret) }
        JamSessionCode(hostAddress = address, port = bound.localPort, secret = secret)
    }

    override suspend fun broadcast(message: JamMessage) {
        val payload = JamProtocol.encode(message)
        coroutineScope { clients.values.map { client -> async { client.write(payload) } }.awaitAll() }
    }

    override suspend fun send(participantId: String, message: JamMessage) {
        clients[participantId]?.write(JamProtocol.encode(message))
    }

    override suspend fun disconnect(participantId: String) {
        clients.remove(participantId)?.close()
        _events.tryEmit(JamHostEvent.GuestLeft(participantId))
    }

    override fun stop() {
        acceptJob?.cancel()
        acceptJob = null
        runCatching { serverSocket?.close() }
        serverSocket = null
        clients.values.forEach(JamClientConnection::close)
        clients.clear()
        pendingSockets.forEach { socket -> runCatching { socket.close() } }
        pendingSockets.clear()
    }

    fun release() {
        stop()
        scope.cancel()
    }

    private suspend fun acceptLoop(socket: ServerSocket, secret: String) {
        while (currentScopeActive()) {
            val client = try {
                withContext(Dispatchers.IO) { socket.accept() }
            } catch (error: CancellationException) {
                throw error
            } catch (_: IOException) {
                return
            }
            if (clients.size >= JamSessionState.MAX_PARTICIPANTS - 1 || pendingSockets.size >= JAM_MAX_PENDING_HANDSHAKES) {
                runCatching { client.close() }
                continue
            }
            pendingSockets.add(client)
            scope.launch { handshake(client, secret) }
        }
    }

    private suspend fun handshake(socket: Socket, secret: String) {
        var connection: JamClientConnection? = null
        try {
            connection = runCatching { JamClientConnection(socket) }.getOrElse {
                runCatching { socket.close() }
                return
            }
            val participantId = withTimeoutOrNull(JAM_HANDSHAKE_TIMEOUT_MS) {
                val hostNonce = JamAuth.generateNonce()
                connection.write(JamProtocol.encode(JamMessage.Challenge(hostNonce)))

                val line = withContext(Dispatchers.IO) { connection.readLine() } ?: return@withTimeoutOrNull null
                val auth = JamProtocol.decode(line) as? JamMessage.Authenticate ?: return@withTimeoutOrNull null

                val expectedProof = JamAuth.computeGuestProof(secret, hostNonce, auth.guestNonce)
                if (!JamAuth.verifyProof(expectedProof, auth.proof)) {
                    runCatching { connection.write(JamProtocol.encode(JamMessage.Failure(JamFailure.NotAuthorized))) }
                    return@withTimeoutOrNull null
                }

                val newParticipantId = UUID.randomUUID().toString()
                val hostProof = JamAuth.computeHostProof(secret, hostNonce, auth.guestNonce)
                clients[newParticipantId] = connection
                pendingSockets.remove(socket)
                connection.disableReadTimeout()
                _events.tryEmit(JamHostEvent.GuestJoined(newParticipantId, auth.name, hostProof))
                newParticipantId
            }

            if (participantId == null) {
                connection.close()
            } else {
                readLoop(participantId, connection)
            }
        } catch (error: CancellationException) {
            connection?.close()
            throw error
        } catch (_: Exception) {
            connection?.close()
        } finally {
            pendingSockets.remove(socket)
        }
    }

    private suspend fun readLoop(participantId: String, connection: JamClientConnection) {
        try {
            while (true) {
                val line = withContext(Dispatchers.IO) { connection.readLine() } ?: break
                if (line.isBlank()) continue
                when (val message = JamProtocol.decode(line)) {
                    is JamMessage.Action -> _events.tryEmit(
                        JamHostEvent.ActionReceived(participantId, message.action)
                    )
                    is JamMessage.Bye -> break
                    null -> break
                    else -> Unit
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: IOException) {
            Timber.d("Jam guest connection closed")
        } finally {
            clients.remove(participantId)
            connection.close()
            _events.tryEmit(JamHostEvent.GuestLeft(participantId))
        }
    }

    private fun currentScopeActive(): Boolean = scope.coroutineContext[Job]?.isActive != false
}

class LanJamGuestTransport : JamGuestTransport {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _events = MutableSharedFlow<JamGuestEvent>(extraBufferCapacity = JAM_EVENT_BUFFER)
    override val events: Flow<JamGuestEvent> = _events.asSharedFlow()

    @Volatile
    private var connection: JamClientConnection? = null
    private var readJob: Job? = null

    override suspend fun connect(code: JamSessionCode, name: String): Boolean = withContext(Dispatchers.IO) {
        stop()
        val socket = Socket()
        val opened = runCatching {
            socket.connect(InetSocketAddress(code.hostAddress, code.port), JAM_CONNECT_TIMEOUT_MS)
            JamClientConnection(socket)
        }.getOrElse {
            runCatching { socket.close() }
            _events.tryEmit(JamGuestEvent.Failed(JamFailure.ConnectionFailed))
            return@withContext false
        }
        connection = opened

        val authSuccess = withTimeoutOrNull(JAM_HANDSHAKE_TIMEOUT_MS) {
            val challengeLine = runCatching { opened.readLine() }.getOrNull()
            val challenge = challengeLine?.let(JamProtocol::decode) as? JamMessage.Challenge
            if (challenge == null) {
                val failure = (challengeLine?.let(JamProtocol::decode) as? JamMessage.Failure)?.failure ?: JamFailure.ConnectionFailed
                _events.tryEmit(JamGuestEvent.Failed(failure))
                return@withTimeoutOrNull false
            }

            val guestNonce = JamAuth.generateNonce()
            val guestProof = JamAuth.computeGuestProof(code.secret, challenge.hostNonce, guestNonce)
            val authMessage = JamMessage.Authenticate(
                guestNonce = guestNonce,
                name = JamProtocol.sanitizeName(name),
                proof = guestProof
            )
            val sent = runCatching { opened.write(JamProtocol.encode(authMessage)) }.isSuccess
            if (!sent) {
                _events.tryEmit(JamGuestEvent.Failed(JamFailure.ConnectionFailed))
                return@withTimeoutOrNull false
            }

            val welcomeLine = runCatching { opened.readLine() }.getOrNull()
            val welcome = welcomeLine?.let(JamProtocol::decode)
            if (welcome !is JamMessage.Welcome) {
                val failure = (welcome as? JamMessage.Failure)?.failure ?: JamFailure.ConnectionFailed
                _events.tryEmit(JamGuestEvent.Failed(failure))
                return@withTimeoutOrNull false
            }

            val expectedHostProof = JamAuth.computeHostProof(code.secret, challenge.hostNonce, guestNonce)
            if (!JamAuth.verifyProof(expectedHostProof, welcome.hostProof)) {
                _events.tryEmit(JamGuestEvent.Failed(JamFailure.NotAuthorized))
                return@withTimeoutOrNull false
            }

            opened.disableReadTimeout()
            _events.tryEmit(JamGuestEvent.Connected(welcome.sessionId, welcome.participantId))
            readJob = scope.launch { readLoop(opened) }
            true
        } ?: false

        if (!authSuccess) {
            opened.close()
            connection = null
            return@withContext false
        }
        true
    }

    override suspend fun send(message: JamMessage) {
        connection?.write(JamProtocol.encode(message))
    }

    override fun stop() {
        readJob?.cancel()
        readJob = null
        connection?.close()
        connection = null
    }

    fun release() {
        stop()
        scope.cancel()
    }

    private suspend fun readLoop(active: JamClientConnection) {
        var failure: JamFailure? = null
        try {
            while (true) {
                val line = withContext(Dispatchers.IO) { active.readLine() } ?: break
                if (line.isBlank()) continue
                when (val message = JamProtocol.decode(line)) {
                    is JamMessage.State -> _events.tryEmit(JamGuestEvent.StateReceived(message))
                    is JamMessage.Failure -> {
                        failure = message.failure
                        break
                    }
                    is JamMessage.Bye -> {
                        failure = JamFailure.HostEnded
                        break
                    }
                    null -> {
                        failure = JamFailure.ProtocolError
                        break
                    }
                    else -> Unit
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: IOException) {
            failure = JamFailure.ConnectionFailed
        } finally {
            active.close()
            if (connection === active) connection = null
            failure?.let { _events.tryEmit(JamGuestEvent.Failed(it)) }
            _events.tryEmit(JamGuestEvent.Disconnected)
        }
    }
}

object LanJamTransportFactory : JamTransportFactory {
    override fun host(): JamHostTransport = LanJamHostTransport()

    override fun guest(): JamGuestTransport = LanJamGuestTransport()
}

internal class JamClientConnection(private val socket: Socket) {
    private val writeMutex = Mutex()
    private val reader: BufferedReader
    private val writer: BufferedWriter

    init {
        socket.tcpNoDelay = true
        socket.soTimeout = JAM_SOCKET_TIMEOUT_MS
        reader = socket.getInputStream().bufferedReader(Charsets.UTF_8)
        writer = socket.getOutputStream().bufferedWriter(Charsets.UTF_8)
    }

    fun readLine(): String? = reader.readBoundedLine()

    suspend fun write(payload: String) {
        writeMutex.withLock {
            withContext(Dispatchers.IO) {
                runCatching {
                    writer.write(payload)
                    writer.write("\n")
                    writer.flush()
                }.onFailure { close() }
            }
        }
    }

    fun disableReadTimeout() {
        socket.soTimeout = 0
    }

    fun close() {
        runCatching { socket.close() }
    }
}
