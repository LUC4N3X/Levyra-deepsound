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
    val pending: List<JamPendingParticipant> = emptyList(),
    val bannedCount: Int = 0,
    val failure: JamFailure? = null
) {
    val isActive: Boolean get() = role != null
    val isHost: Boolean get() = role == JamRole.Host
    val locked: Boolean get() = session?.locked == true
    val requireApproval: Boolean get() = session?.requireApproval == true
    val canControlPlayback: Boolean get() = isHost || permission.canControlPlayback
    val canAddTracks: Boolean get() = isHost || permission.canAddTracks
    val supportsBatchAddTracks: Boolean
        get() = isHost || JamCapabilities.BATCH_ADD_TRACKS in session?.capabilities.orEmpty()
}

private fun jamActionBatchValid(action: JamAction): Boolean = when (action) {
    is JamAction.AddTracks -> action.tracks.size in 1..JamSessionState.MAX_QUEUE_SIZE
    is JamAction.PlayNextTracks -> action.tracks.size in 1..JamSessionState.MAX_QUEUE_SIZE
    else -> true
}

private fun jamActionAddedTracks(action: JamAction): List<JamTrack> = when (action) {
    is JamAction.AddTrack -> listOf(action.track)
    is JamAction.AddTracks -> action.tracks
    is JamAction.PlayNextTracks -> action.tracks
    else -> emptyList()
}

private fun jamActionExceedsQueueLimit(action: JamAction, queue: List<JamTrack>): Boolean {
    val requested = jamActionAddedTracks(action)
    if (requested.isEmpty()) return false
    val knownIds = queue.mapTo(hashSetOf()) { it.id }
    val added = requested.asSequence()
        .distinctBy { it.id }
        .count { knownIds.add(it.id) }
    return queue.size + added > JamSessionState.MAX_QUEUE_SIZE
}

class JamController(
    private val scope: CoroutineScope,
    private val bridge: JamPlayerBridge,
    private val transports: JamTransportFactory = LanJamTransportFactory,
    private val elapsedMs: () -> Long = { SystemClock.elapsedRealtime() },
    private val wallClockMs: () -> Long = { System.currentTimeMillis() }
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
    private var requireApproval: Boolean = true
    private var locked: Boolean = false
    private var participants: MutableList<JamParticipant> = mutableListOf()
    private val pending: MutableList<JamPendingParticipant> = mutableListOf()
    private val pendingProofs: MutableMap<String, String> = linkedMapOf()
    private val participantIdentities: MutableMap<String, String> = linkedMapOf()
    private val banned: MutableSet<String> = linkedSetOf()
    private val trackOwners: MutableMap<String, String> = linkedMapOf()
    private var lastAppliedRevision: Long = -1L
    private var joinedCode: JamSessionCode? = null
    private var joinedName: String = ""
    private var joinedIdentity: String = ""
    private var leaving = false

    suspend fun createJam(
        displayName: String,
        guestPermission: JamGuestPermission,
        approvalRequired: Boolean = true
    ) {
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
            createdAt = wallClockMs()
            permission = guestPermission
            requireApproval = approvalRequired
            locked = false
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

    suspend fun joinJam(rawCode: String, displayName: String, guestIdentity: String) {
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
            joinedIdentity = JamIdentity.sanitize(guestIdentity)
            _state.value = JamUiState(role = JamRole.Guest, connection = JamConnectionState.Connecting)
            val transport = transports.guest()
            guestTransport = transport
            observeGuest(transport)
            lastAppliedRevision = -1L
            val connected = transport.connect(code, joinedName, joinedIdentity)
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

    suspend fun setSessionLocked(value: Boolean) {
        mutex.withLock {
            if (_state.value.role != JamRole.Host) return
            if (locked == value) return
            locked = value
            publishHostState()
        }
    }

    suspend fun setApprovalRequired(value: Boolean) {
        mutex.withLock {
            if (_state.value.role != JamRole.Host) return
            if (requireApproval == value) return
            requireApproval = value
            if (!value) {
                val transport = hostTransport
                val waiting = pending.toList()
                pending.clear()
                if (transport != null) {
                    waiting.forEach { entry ->
                        if (participants.size >= JamSessionState.MAX_PARTICIPANTS) {
                            rejectLocked(transport, entry.participantId, JamFailure.SessionFull)
                        } else {
                            admitLocked(transport, entry)
                        }
                    }
                }
            }
            publishHostState()
        }
    }

    suspend fun approveParticipant(participantId: String) {
        mutex.withLock {
            if (_state.value.role != JamRole.Host) return
            val transport = hostTransport ?: return
            val entry = pending.firstOrNull { it.participantId == participantId } ?: return
            pending.remove(entry)
            if (participants.size >= JamSessionState.MAX_PARTICIPANTS) {
                rejectLocked(transport, participantId, JamFailure.SessionFull)
                publishHostState()
                return
            }
            admitLocked(transport, entry)
            publishHostState()
        }
    }

    suspend fun rejectParticipant(participantId: String) {
        mutex.withLock {
            if (_state.value.role != JamRole.Host) return
            val transport = hostTransport ?: return
            if (pending.removeAll { it.participantId == participantId }) {
                rejectLocked(transport, participantId, JamFailure.Rejected)
                publishHostState()
            }
        }
    }

    suspend fun removeParticipant(participantId: String, ban: Boolean = false) {
        mutex.withLock {
            if (_state.value.role != JamRole.Host) return
            if (participantId == hostParticipantId) return
            val transport = hostTransport ?: return
            if (ban) {
                participantIdentities[participantId]
                    ?.takeIf { it.isNotBlank() }
                    ?.let(::banIdentityLocked)
            }
            participants.removeAll { it.id == participantId }
            participantIdentities.remove(participantId)
            pending.removeAll { it.participantId == participantId }
            rejectLocked(transport, participantId, if (ban) JamFailure.Banned else JamFailure.Removed)
            publishHostState()
        }
    }

    suspend fun clearBans() {
        mutex.withLock {
            if (_state.value.role != JamRole.Host) return
            if (banned.isEmpty()) return
            banned.clear()
            _state.update { it.copy(bannedCount = 0) }
        }
    }

    suspend fun requestAction(action: JamAction) {
        mutex.withLock {
            val current = _state.value
            if (!jamActionBatchValid(action)) {
                _state.update { it.copy(failure = JamFailure.NotAuthorized) }
                return
            }
            if (jamActionExceedsQueueLimit(action, bridge.snapshot().queue) ||
                current.session?.queue?.let { jamActionExceedsQueueLimit(action, it) } == true
            ) {
                _state.update { it.copy(failure = JamFailure.NotAuthorized) }
                return
            }
            when (current.role) {
                JamRole.Host -> {
                    rememberOwnersLocked(action, hostParticipantId)
                    bridge.applyAction(action)
                    publishHostState()
                }
                JamRole.Guest -> {
                    if (current.connection != JamConnectionState.Connected) {
                        _state.update { it.copy(failure = JamFailure.NotAuthorized) }
                        return
                    }
                    if (action is JamAction.AddTracks &&
                        JamCapabilities.BATCH_ADD_TRACKS !in current.session?.capabilities.orEmpty()
                    ) {
                        _state.update { it.copy(failure = JamFailure.NotAuthorized) }
                        return
                    }
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
        pending.clear()
        pendingProofs.clear()
        participantIdentities.clear()
        banned.clear()
        trackOwners.clear()
        sessionId = ""
        hostParticipantId = ""
        revision = 0L
        createdAt = 0L
        requireApproval = true
        locked = false
        lastAppliedRevision = -1L
        joinedCode = null
        joinedName = ""
        joinedIdentity = ""
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
                is JamHostEvent.GuestPending -> handleGuestPendingLocked(transport, event)
                is JamHostEvent.GuestLeft -> {
                    val removedParticipant = participants.removeAll { it.id == event.participantId }
                    val removedPending = pending.removeAll { it.participantId == event.participantId }
                    pendingProofs.remove(event.participantId)
                    participantIdentities.remove(event.participantId)
                    if (removedParticipant) publishHostState() else if (removedPending) publishPendingLocked()
                }
                is JamHostEvent.ActionReceived -> {
                    val sender = participants.firstOrNull { it.id == event.participantId }
                    if (sender == null || sender.isHost) return
                    if (!JamAuthorization.allows(permission, event.action)) return
                    if (jamActionExceedsQueueLimit(event.action, bridge.snapshot().queue)) return
                    rememberOwnersLocked(event.action, event.participantId)
                    bridge.applyAction(event.action)
                    publishHostState()
                }
                is JamHostEvent.Failed -> _state.update { it.copy(failure = event.failure) }
            }
        }
    }

    private suspend fun handleGuestPendingLocked(
        transport: JamHostTransport,
        event: JamHostEvent.GuestPending
    ) {
        val identity = JamIdentity.sanitize(event.guestId)
        // Session bans are best-effort moderation based on a resettable local guest identity.
        if (identity.isNotBlank() && identity in banned) {
            rejectLocked(transport, event.participantId, JamFailure.Banned)
            return
        }
        if (locked) {
            rejectLocked(transport, event.participantId, JamFailure.SessionLocked)
            return
        }
        if (identity.isNotBlank()) {
            val replaced = participantIdentities.entries
                .filter { it.value == identity }
                .map { it.key }
            replaced.forEach { staleId ->
                participants.removeAll { it.id == staleId }
                participantIdentities.remove(staleId)
                trackOwners.entries.forEach { owner ->
                    if (owner.value == staleId) owner.setValue(event.participantId)
                }
                transport.disconnect(staleId)
            }
            val stalePending = pending.filter { it.guestId == identity }
            stalePending.forEach { stale ->
                pending.remove(stale)
                rejectLocked(transport, stale.participantId, JamFailure.Removed)
            }
        }
        if (participants.size >= JamSessionState.MAX_PARTICIPANTS) {
            rejectLocked(transport, event.participantId, JamFailure.SessionFull)
            return
        }

        val entry = JamPendingParticipant(
            participantId = event.participantId,
            guestId = identity,
            name = JamProtocol.sanitizeName(event.name),
            requestedAtElapsedMs = elapsedMs()
        )
        pendingProofs[event.participantId] = event.hostProof

        if (!requireApproval) {
            admitLocked(transport, entry)
            publishHostState()
            return
        }
        if (pending.size >= JamSessionState.MAX_PENDING) {
            rejectLocked(transport, event.participantId, JamFailure.SessionFull)
            return
        }
        pending += entry
        transport.notifyPending(event.participantId, JamMessage.Pending(event.hostProof))
        publishPendingLocked()
    }

    private suspend fun admitLocked(transport: JamHostTransport, entry: JamPendingParticipant) {
        val proof = pendingProofs.remove(entry.participantId) ?: return
        participants.removeAll { it.id == entry.participantId }
        participants += JamParticipant(entry.participantId, entry.name, isHost = false)
        if (entry.guestId.isNotBlank()) participantIdentities[entry.participantId] = entry.guestId
        val admitted = transport.admit(
            entry.participantId,
            JamMessage.Welcome(sessionId, entry.participantId, proof)
        )
        if (!admitted) {
            participants.removeAll { it.id == entry.participantId }
            participantIdentities.remove(entry.participantId)
        }
    }

    private suspend fun rejectLocked(
        transport: JamHostTransport,
        participantId: String,
        failure: JamFailure
    ) {
        pendingProofs.remove(participantId)
        transport.reject(participantId, failure)
    }

    private fun banIdentityLocked(identity: String) {
        banned += identity
        while (banned.size > JamSessionState.MAX_BANNED) {
            val oldest = banned.firstOrNull() ?: break
            banned.remove(oldest)
        }
    }

    private fun rememberOwnersLocked(action: JamAction, participantId: String) {
        val tracks = jamActionAddedTracks(action)
        if (tracks.isEmpty()) return
        tracks.forEach { track -> trackOwners[track.id] = participantId }
        while (trackOwners.size > JamSessionState.MAX_QUEUE_SIZE) {
            val oldest = trackOwners.keys.firstOrNull() ?: break
            trackOwners.remove(oldest)
        }
    }

    private fun publishPendingLocked() {
        _state.update { it.copy(pending = pending.toList(), bannedCount = banned.size) }
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
            JamGuestEvent.AwaitingApproval -> _state.update {
                it.copy(
                    role = JamRole.Guest,
                    connection = JamConnectionState.AwaitingApproval,
                    failure = null
                )
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
        val received = message.state.copy(updatedAtElapsedMs = elapsedMs())
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
            _state.value.failure in NON_RECOVERABLE_FAILURES
        ) return
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            repeat(RECONNECT_ATTEMPTS) { attempt ->
                delay(RECONNECT_DELAYS_MS[attempt])
                val connected = mutex.withLock {
                    if (leaving || _state.value.role != JamRole.Guest) return@withLock true
                    if (_state.value.failure in NON_RECOVERABLE_FAILURES) return@withLock true
                    val code = joinedCode ?: return@withLock true
                    guestEventsJob?.cancel()
                    guestTransport?.stop()
                    val transport = transports.guest()
                    guestTransport = transport
                    observeGuest(transport)
                    _state.update { it.copy(connection = JamConnectionState.Connecting) }
                    transport.connect(code, joinedName, joinedIdentity)
                }
                if (connected) return@launch
            }
            _state.update {
                it.copy(connection = JamConnectionState.Disconnected, failure = JamFailure.ConnectionFailed)
            }
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
        _state.update {
            it.copy(
                session = hostState,
                permission = permission,
                pending = pending.toList(),
                bannedCount = banned.size
            )
        }
        transport.broadcast(
            JamMessage.State(
                sessionId = sessionId,
                revision = revision,
                source = hostParticipantId,
                timestamp = wallClockMs(),
                state = hostState
            )
        )
    }

    private fun buildHostState(): JamSessionState {
        val snapshot = bridge.snapshot()
        val queue = snapshot.queue.take(JamSessionState.MAX_QUEUE_SIZE).map { track ->
            val owner = trackOwners[track.id]
            if (owner == null) track else track.copy(addedBy = owner)
        }
        val liveIds = queue.mapTo(hashSetOf()) { it.id }
        trackOwners.keys.retainAll(liveIds)
        return JamSessionState(
            sessionId = sessionId,
            hostId = hostParticipantId,
            revision = revision,
            createdAt = createdAt,
            participants = participants.toList(),
            queue = queue,
            currentIndex = snapshot.currentIndex,
            currentMediaId = snapshot.currentMediaId,
            positionMs = snapshot.positionMs,
            playWhenReady = snapshot.playWhenReady,
            shuffle = snapshot.shuffle,
            repeatMode = snapshot.repeatMode,
            permission = permission,
            updatedAtElapsedMs = elapsedMs(),
            capabilities = JamCapabilities.current,
            locked = locked,
            requireApproval = requireApproval
        )
    }

    companion object {
        private const val RECONNECT_ATTEMPTS = 3
        private val RECONNECT_DELAYS_MS = longArrayOf(1_000L, 2_000L, 4_000L)
        private val NON_RECOVERABLE_FAILURES = setOf(
            JamFailure.HostEnded,
            JamFailure.NotAuthorized,
            JamFailure.ProtocolError,
            JamFailure.InvalidCode,
            JamFailure.Rejected,
            JamFailure.Banned,
            JamFailure.SessionLocked,
            JamFailure.SessionFull,
            JamFailure.Removed
        )
    }
}
