package com.luc4n3x.levyra.feature.jam

import android.os.SystemClock
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class JamUiState(
    val role: JamRole? = null,
    val connection: JamConnectionState = JamConnectionState.Idle,
    val code: String = "",
    val deepLink: String = "",
    val session: JamSessionState? = null,
    val selfParticipantId: String = "",
    val permission: JamGuestPermission = JamGuestPermission.HostOnly,
    val failure: JamFailure? = null
) {
    val isActive: Boolean get() = role != null
    val isHost: Boolean get() = role == JamRole.Host
    val canControlPlayback: Boolean get() = isHost || permission.canControlPlayback
    val canAddTracks: Boolean get() = isHost || permission.canAddTracks
}

class JamController(
    private val scope: CoroutineScope,
    private val bridge: JamPlayerBridge,
    private val transports: JamTransportFactory = LanJamTransportFactory
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow(JamUiState())
    val state: StateFlow<JamUiState> = _state.asStateFlow()

    private var hostTransport: JamHostTransport? = null
    private var guestTransport: JamGuestTransport? = null
    private var hostEventsJob: Job? = null
    private var guestEventsJob: Job? = null
    private var broadcastJob: Job? = null
    private var reconnectJob: Job? = null

    private var sessionId: String = ""
    private var hostParticipantId: String = ""
    private var revision: Long = 0L
    private var createdAt: Long = 0L
    private var permission: JamGuestPermission = JamGuestPermission.HostOnly
    private var participants: MutableList<JamParticipant> = mutableListOf()
    private var lastAppliedRevision: Long = -1L
    private var joinedCode: JamSessionCode? = null
    private var joinedName: String = ""
    private var leaving = false

    suspend fun createJam(displayName: String, guestPermission: JamGuestPermission) {
        mutex.withLock {
            releaseLocked()
            _state.value = JamUiState(role = JamRole.Host, connection = JamConnectionState.Connecting)
            val transport = transports.host()
            hostTransport = transport
            val code = transport.start(JamSessionCode.newSecret())
            if (code == null) {
                releaseLocked()
                _state.value = JamUiState(failure = JamFailure.ConnectionFailed)
                return
            }
            sessionId = UUID.randomUUID().toString()
            hostParticipantId = UUID.randomUUID().toString()
            revision = 0L
            createdAt = System.currentTimeMillis()
            permission = guestPermission
            participants = mutableListOf(
                JamParticipant(hostParticipantId, JamProtocol.sanitizeName(displayName), isHost = true)
            )
            _state.value = JamUiState(
                role = JamRole.Host,
                connection = JamConnectionState.Connected,
                code = code.formatted(),
                deepLink = code.deepLink(),
                session = buildHostState(),
                selfParticipantId = hostParticipantId,
                permission = guestPermission
            )
            observeHost(transport)
            startBroadcastLoop()
        }
    }

    suspend fun joinJam(rawCode: String, displayName: String) {
        val code = JamSessionCode.parse(rawCode)
        if (code == null) {
            _state.value = JamUiState(failure = JamFailure.InvalidCode)
            return
        }
        mutex.withLock {
            releaseLocked()
            leaving = false
            joinedCode = code
            joinedName = JamProtocol.sanitizeName(displayName)
            _state.value = JamUiState(role = JamRole.Guest, connection = JamConnectionState.Connecting)
            val transport = transports.guest()
            guestTransport = transport
            observeGuest(transport)
            lastAppliedRevision = -1L
            val connected = transport.connect(code, displayName)
            if (!connected) {
                val failure = _state.value.failure ?: JamFailure.ConnectionFailed
                releaseLocked()
                _state.value = JamUiState(failure = failure)
            }
        }
    }

    suspend fun leave() {
        mutex.withLock {
            leaving = true
            guestTransport?.let { transport -> runCatching { transport.send(JamMessage.Bye("leave")) } }
            releaseLocked()
            _state.value = JamUiState()
        }
    }

    suspend fun endJam() {
        mutex.withLock {
            hostTransport?.let { transport -> runCatching { transport.broadcast(JamMessage.Bye("host_ended")) } }
            releaseLocked()
            _state.value = JamUiState()
        }
    }

    suspend fun setGuestPermission(value: JamGuestPermission) {
        mutex.withLock {
            if (_state.value.role != JamRole.Host) return
            permission = value
            publishHostState()
        }
    }

    suspend fun removeParticipant(participantId: String) {
        mutex.withLock {
            if (_state.value.role != JamRole.Host) return
            if (participantId == hostParticipantId) return
            val transport = hostTransport ?: return
            transport.send(participantId, JamMessage.Failure(JamFailure.NotAuthorized))
            transport.disconnect(participantId)
        }
    }

    suspend fun requestAction(action: JamAction) {
        mutex.withLock {
            val current = _state.value
            if (action is JamAction.AddTrack &&
                (bridge.snapshot().queue.size >= JamSessionState.MAX_QUEUE_SIZE ||
                    (current.session?.queue?.size ?: 0) >= JamSessionState.MAX_QUEUE_SIZE)
            ) {
                _state.update { it.copy(failure = JamFailure.NotAuthorized) }
                return
            }
            when (current.role) {
                JamRole.Host -> {
                    bridge.applyAction(action)
                    publishHostState()
                }
                JamRole.Guest -> {
                    if (!JamAuthorization.allows(current.permission, action)) {
                        _state.update { it.copy(failure = JamFailure.NotAuthorized) }
                        return
                    }
                    guestTransport?.send(JamMessage.Action(sessionId, current.selfParticipantId, action))
                }
                null -> Unit
            }
        }
    }

    fun clearFailure() {
        _state.update { it.copy(failure = null) }
    }

    fun rejectGuestLocalMutation(): Boolean {
        if (_state.value.role != JamRole.Guest) return false
        _state.update { it.copy(failure = JamFailure.NotAuthorized) }
        return true
    }

    suspend fun release() {
        mutex.withLock {
            releaseLocked()
            _state.value = JamUiState()
        }
    }

    fun close() {
        leaving = true
        releaseLocked()
        _state.value = JamUiState()
    }

    private fun releaseLocked() {
        reconnectJob?.cancel()
        reconnectJob = null
        broadcastJob?.cancel()
        broadcastJob = null
        hostEventsJob?.cancel()
        hostEventsJob = null
        guestEventsJob?.cancel()
        guestEventsJob = null
        hostTransport?.let { transport ->
            if (transport is LanJamHostTransport) transport.release() else transport.stop()
        }
        hostTransport = null
        guestTransport?.let { transport ->
            if (transport is LanJamGuestTransport) transport.release() else transport.stop()
        }
        guestTransport = null
        participants = mutableListOf()
        sessionId = ""
        hostParticipantId = ""
        revision = 0L
        createdAt = 0L
        lastAppliedRevision = -1L
        joinedCode = null
        joinedName = ""
    }

    private fun observeHost(transport: JamHostTransport) {
        hostEventsJob?.cancel()
        hostEventsJob = scope.launch {
            transport.events.collect { event -> handleHostEvent(transport, event) }
        }
    }

    private suspend fun handleHostEvent(transport: JamHostTransport, event: JamHostEvent) {
        mutex.withLock {
            if (_state.value.role != JamRole.Host || hostTransport !== transport) return
            when (event) {
                is JamHostEvent.GuestJoined -> {
                    if (participants.size >= JamSessionState.MAX_PARTICIPANTS) {
                        transport.send(event.participantId, JamMessage.Failure(JamFailure.NotAuthorized))
                        transport.disconnect(event.participantId)
                        return
                    }
                    participants.removeAll { it.id == event.participantId }
                    participants.add(JamParticipant(event.participantId, event.name, isHost = false))
                    transport.send(event.participantId, JamMessage.Welcome(sessionId, event.participantId, event.hostProof))
                    publishHostState()
                }
                is JamHostEvent.GuestLeft -> {
                    if (participants.removeAll { it.id == event.participantId }) publishHostState()
                }
                is JamHostEvent.ActionReceived -> {
                    val sender = participants.firstOrNull { it.id == event.participantId }
                    if (sender == null || sender.isHost) return
                    if (!JamAuthorization.allows(permission, event.action)) return
                    if (event.action is JamAction.AddTrack &&
                        bridge.snapshot().queue.size >= JamSessionState.MAX_QUEUE_SIZE
                    ) return
                    bridge.applyAction(event.action)
                    publishHostState()
                }
                is JamHostEvent.Failed -> _state.update { it.copy(failure = event.failure) }
            }
        }
    }

    private fun observeGuest(transport: JamGuestTransport) {
        guestEventsJob?.cancel()
        guestEventsJob = scope.launch {
            transport.events.collect { event -> handleGuestEvent(event) }
        }
    }

    private suspend fun handleGuestEvent(event: JamGuestEvent) {
        when (event) {
            is JamGuestEvent.Connected -> {
                sessionId = event.sessionId
                _state.update {
                    it.copy(
                        role = JamRole.Guest,
                        connection = JamConnectionState.Connected,
                        selfParticipantId = event.participantId,
                        failure = null
                    )
                }
            }
            is JamGuestEvent.StateReceived -> applyRemoteState(event.message)
            is JamGuestEvent.Failed -> _state.update {
                it.copy(connection = JamConnectionState.Disconnected, failure = event.failure)
            }
            JamGuestEvent.Disconnected -> {
                _state.update { it.copy(connection = JamConnectionState.Disconnected) }
                scheduleReconnect()
            }
        }
    }

    private suspend fun applyRemoteState(message: JamMessage.State) {
        if (sessionId.isNotBlank() && message.sessionId != sessionId) return
        if (JamPlaybackSync.isStaleRevision(message.revision, lastAppliedRevision)) return
        lastAppliedRevision = message.revision
        val received = message.state.copy(updatedAtElapsedMs = SystemClock.elapsedRealtime())
        bridge.applyRemoteState(received)
        _state.update {
            it.copy(
                connection = JamConnectionState.Connected,
                session = received,
                permission = received.permission
            )
        }
    }

    private fun scheduleReconnect() {
        if (
            leaving || joinedCode == null ||
            _state.value.failure in setOf(
                JamFailure.HostEnded,
                JamFailure.NotAuthorized,
                JamFailure.ProtocolError,
                JamFailure.InvalidCode
            )
        ) return
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            repeat(RECONNECT_ATTEMPTS) { attempt ->
                delay(RECONNECT_DELAYS_MS[attempt])
                val connected = mutex.withLock {
                    if (leaving || _state.value.role != JamRole.Guest) return@withLock true
                    val code = joinedCode ?: return@withLock true
                    guestEventsJob?.cancel()
                    guestTransport?.stop()
                    val transport = transports.guest()
                    guestTransport = transport
                    observeGuest(transport)
                    _state.update { it.copy(connection = JamConnectionState.Connecting) }
                    transport.connect(code, joinedName)
                }
                if (connected) return@launch
            }
            _state.update { it.copy(connection = JamConnectionState.Disconnected, failure = JamFailure.ConnectionFailed) }
        }
    }

    private fun startBroadcastLoop() {
        broadcastJob?.cancel()
        broadcastJob = scope.launch {
            while (true) {
                delay(JamPlaybackSync.BROADCAST_INTERVAL_MS)
                mutex.withLock {
                    if (_state.value.role != JamRole.Host) return@withLock
                    publishHostState()
                }
            }
        }
    }

    private suspend fun publishHostState() {
        val transport = hostTransport ?: return
        revision++
        val hostState = buildHostState()
        _state.update { it.copy(session = hostState, permission = permission) }
        transport.broadcast(
            JamMessage.State(
                sessionId = sessionId,
                revision = revision,
                source = hostParticipantId,
                timestamp = System.currentTimeMillis(),
                state = hostState
            )
        )
    }

    private fun buildHostState(): JamSessionState {
        val snapshot = bridge.snapshot()
        return JamSessionState(
            sessionId = sessionId,
            hostId = hostParticipantId,
            revision = revision,
            createdAt = createdAt,
            participants = participants.toList(),
            queue = snapshot.queue.take(JamSessionState.MAX_QUEUE_SIZE),
            currentIndex = snapshot.currentIndex,
            currentMediaId = snapshot.currentMediaId,
            positionMs = snapshot.positionMs,
            playWhenReady = snapshot.playWhenReady,
            shuffle = snapshot.shuffle,
            repeatMode = snapshot.repeatMode,
            permission = permission,
            updatedAtElapsedMs = SystemClock.elapsedRealtime()
        )
    }

    companion object {
        private const val RECONNECT_ATTEMPTS = 3
        private val RECONNECT_DELAYS_MS = longArrayOf(1_000L, 2_000L, 4_000L)
    }
}
