package com.luc4n3x.levyra.feature.jam

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JamModerationTest {

    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val host = FakeHostTransport()
    private val guest = FakeGuestTransport()
    private val bridge = FakeBridge()
    private val controller = JamController(
        scope = scope,
        bridge = bridge,
        transports = object : JamTransportFactory {
            override fun host(): JamHostTransport = host
            override fun guest(): JamGuestTransport = guest
        },
        elapsedMs = { 1_000L },
        wallClockMs = { 2_000L }
    )

    @After
    fun tearDown() {
        controller.close()
        scope.cancel()
    }

    @Test
    fun pendingGuestIsHeldUntilTheHostApproves() = runBlocking {
        createHostSession(approvalRequired = true)

        host.emit(pending("guest-1", identity(1), "Ada"))

        assertEquals(listOf("guest-1"), host.pendingNotices)
        assertTrue(host.admitted.isEmpty())
        assertEquals(1, controller.state.value.pending.size)
        assertEquals("Ada", controller.state.value.pending.single().name)

        controller.approveParticipant("guest-1")

        assertEquals(listOf("guest-1"), host.admitted)
        assertTrue(controller.state.value.pending.isEmpty())
        assertEquals(2, controller.state.value.session?.participants?.size)
    }

    @Test
    fun pendingGuestIsRemovedWhenTransportReportsDisconnect() = runBlocking {
        createHostSession(approvalRequired = true)
        host.emit(pending("guest-1", identity(1), "Ada"))
        assertEquals(1, controller.state.value.pending.size)

        host.emit(JamHostEvent.GuestLeft("guest-1"))

        assertTrue(controller.state.value.pending.isEmpty())
        assertEquals(1, controller.state.value.session?.participants?.size)
    }

    @Test
    fun rejectedGuestNeverEntersTheParticipantList() = runBlocking {
        createHostSession(approvalRequired = true)
        host.emit(pending("guest-1", identity(1), "Ada"))

        controller.rejectParticipant("guest-1")

        assertEquals(listOf("guest-1" to JamFailure.Rejected), host.rejected)
        assertTrue(host.admitted.isEmpty())
        assertTrue(controller.state.value.pending.isEmpty())
        assertEquals(1, controller.state.value.session?.participants?.size)
    }

    @Test
    fun bannedIdentityIsRefusedOnReconnect() = runBlocking {
        createHostSession(approvalRequired = false)
        host.emit(pending("guest-1", identity(1), "Ada"))
        assertEquals(listOf("guest-1"), host.admitted)

        controller.removeParticipant("guest-1", ban = true)
        assertTrue(host.rejected.contains("guest-1" to JamFailure.Banned))
        assertEquals(1, controller.state.value.bannedCount)

        host.emit(pending("guest-2", identity(1), "Ada"))

        assertTrue(host.rejected.contains("guest-2" to JamFailure.Banned))
        assertEquals(listOf("guest-1"), host.admitted)
        assertEquals(1, controller.state.value.session?.participants?.size)
    }

    @Test
    fun removedGuestWithoutBanCanRequestToJoinAgain() = runBlocking {
        createHostSession(approvalRequired = true)
        host.emit(pending("guest-1", identity(1), "Ada"))
        controller.approveParticipant("guest-1")

        controller.removeParticipant("guest-1", ban = false)
        assertTrue(host.rejected.contains("guest-1" to JamFailure.Removed))
        assertEquals(0, controller.state.value.bannedCount)

        host.emit(pending("guest-2", identity(1), "Ada"))

        assertEquals(1, controller.state.value.pending.size)
        assertTrue(host.rejected.none { it.first == "guest-2" })
    }

    @Test
    fun lockedSessionRefusesEveryNewGuest() = runBlocking {
        createHostSession(approvalRequired = true)
        controller.setSessionLocked(true)

        host.emit(pending("guest-1", identity(1), "Ada"))

        assertEquals(listOf("guest-1" to JamFailure.SessionLocked), host.rejected)
        assertTrue(controller.state.value.pending.isEmpty())
        assertTrue(controller.state.value.locked)
    }

    @Test
    fun sessionRefusesGuestsPastTheParticipantLimit() = runBlocking {
        createHostSession(approvalRequired = false)
        repeat(JamSessionState.MAX_PARTICIPANTS - 1) { index ->
            host.emit(pending("guest-$index", identity(index + 10), "Guest $index"))
        }
        assertEquals(JamSessionState.MAX_PARTICIPANTS, controller.state.value.session?.participants?.size)

        host.emit(pending("overflow", identity(99), "Late"))

        assertTrue(host.rejected.contains("overflow" to JamFailure.SessionFull))
    }

    @Test
    fun disablingApprovalAdmitsEveryWaitingGuest() = runBlocking {
        createHostSession(approvalRequired = true)
        host.emit(pending("guest-1", identity(1), "Ada"))
        host.emit(pending("guest-2", identity(2), "Bob"))
        assertEquals(2, controller.state.value.pending.size)

        controller.setApprovalRequired(false)

        assertEquals(setOf("guest-1", "guest-2"), host.admitted.toSet())
        assertTrue(controller.state.value.pending.isEmpty())
        assertEquals(3, controller.state.value.session?.participants?.size)
    }

    @Test
    fun guestActionIsIgnoredWhenThePermissionDoesNotAllowIt() = runBlocking {
        createHostSession(approvalRequired = false, permission = JamGuestPermission.HostOnly)
        host.emit(pending("guest-1", identity(1), "Ada"))

        host.emit(JamHostEvent.ActionReceived("guest-1", JamAction.AddTrack(track("song-1"))))

        assertTrue(bridge.applied.isEmpty())
    }

    @Test
    fun actionFromAnUnknownParticipantIsIgnored() = runBlocking {
        createHostSession(approvalRequired = true, permission = JamGuestPermission.Collaborative)
        host.emit(pending("guest-1", identity(1), "Ada"))

        host.emit(JamHostEvent.ActionReceived("guest-1", JamAction.AddTrack(track("song-1"))))

        assertTrue(bridge.applied.isEmpty())
    }

    @Test
    fun acceptedGuestTrackKeepsContributorAttribution() = runBlocking {
        createHostSession(approvalRequired = false, permission = JamGuestPermission.AddSongs)
        host.emit(pending("guest-1", identity(1), "Ada"))

        host.emit(JamHostEvent.ActionReceived("guest-1", JamAction.AddTrack(track("song-1"))))

        assertEquals(1, bridge.applied.size)
        val queue = controller.state.value.session?.queue.orEmpty()
        assertEquals("song-1", queue.single().id)
        assertEquals("guest-1", queue.single().addedBy)
    }

    @Test
    fun guestWithoutAStableIdentityIsRemovedButNeverBanned() = runBlocking {
        createHostSession(approvalRequired = false)
        host.emit(pending("guest-1", "not-a-valid-identity", "Ada"))
        assertEquals(listOf("guest-1"), host.admitted)

        controller.removeParticipant("guest-1", ban = true)

        assertTrue(host.rejected.contains("guest-1" to JamFailure.Banned))
        assertEquals(0, controller.state.value.bannedCount)
        assertEquals(1, controller.state.value.session?.participants?.size)
    }

    @Test
    fun clearingBansRemovesEveryBlockedIdentity() = runBlocking {
        createHostSession(approvalRequired = false)
        host.emit(pending("guest-1", identity(1), "Ada"))
        controller.removeParticipant("guest-1", ban = true)
        assertEquals(1, controller.state.value.bannedCount)

        controller.clearBans()
        assertEquals(0, controller.state.value.bannedCount)

        host.emit(pending("guest-2", identity(1), "Ada"))
        assertTrue(host.admitted.contains("guest-2"))
    }

    @Test
    fun endingTheSessionClearsModerationState() = runBlocking {
        createHostSession(approvalRequired = true)
        host.emit(pending("guest-1", identity(1), "Ada"))

        controller.endJam()

        val state = controller.state.value
        assertNull(state.role)
        assertTrue(state.pending.isEmpty())
        assertEquals(0, state.bannedCount)
    }

    private suspend fun createHostSession(
        approvalRequired: Boolean,
        permission: JamGuestPermission = JamGuestPermission.AddSongs
    ) {
        controller.createJam("Host", permission, approvalRequired)
        assertEquals(JamRole.Host, controller.state.value.role)
    }

    private fun pending(participantId: String, guestId: String, name: String) =
        JamHostEvent.GuestPending(participantId, guestId, name, "proof-$participantId")

    private fun identity(seed: Int): String = String.format("%032x", seed)

    private fun track(id: String) = JamTrack(id, "Song $id", "Artist", 180_000L, "")

    private class FakeBridge : JamPlayerBridge {
        val applied = mutableListOf<JamAction>()
        private val queue = mutableListOf<JamTrack>()

        override fun snapshot(): JamPlaybackSnapshot = JamPlaybackSnapshot(
            queue = queue.toList(),
            currentIndex = if (queue.isEmpty()) -1 else 0,
            currentMediaId = queue.firstOrNull()?.id.orEmpty()
        )

        override suspend fun applyRemoteState(state: JamSessionState) = Unit

        override suspend fun applyAction(action: JamAction) {
            applied += action
            when (action) {
                is JamAction.AddTrack -> queue += action.track
                is JamAction.AddTracks -> queue += action.tracks
                is JamAction.PlayNextTracks -> queue.addAll(0, action.tracks)
                is JamAction.RemoveTrack -> queue.removeAll { it.id == action.trackId }
                else -> Unit
            }
        }
    }

    private class FakeHostTransport : JamHostTransport {
        private val eventFlow = MutableSharedFlow<JamHostEvent>(extraBufferCapacity = 64)
        override val events: Flow<JamHostEvent> = eventFlow.asSharedFlow()

        val admitted = mutableListOf<String>()
        val rejected = mutableListOf<Pair<String, JamFailure>>()
        val pendingNotices = mutableListOf<String>()
        val disconnected = mutableListOf<String>()
        val broadcasts = mutableListOf<JamMessage>()

        suspend fun emit(event: JamHostEvent) {
            eventFlow.emit(event)
        }

        override suspend fun start(secret: String): JamSessionCode =
            JamSessionCode("192.168.1.10", 40_000, secret)

        override suspend fun notifyPending(participantId: String, message: JamMessage.Pending) {
            pendingNotices += participantId
        }

        override suspend fun admit(participantId: String, welcome: JamMessage.Welcome): Boolean {
            admitted += participantId
            return true
        }

        override suspend fun reject(participantId: String, failure: JamFailure) {
            rejected += participantId to failure
        }

        override suspend fun broadcast(message: JamMessage) {
            broadcasts += message
        }

        override suspend fun send(participantId: String, message: JamMessage) = Unit

        override suspend fun disconnect(participantId: String) {
            disconnected += participantId
        }

        override fun stop() = Unit
    }

    private class FakeGuestTransport : JamGuestTransport {
        private val eventFlow = MutableSharedFlow<JamGuestEvent>(extraBufferCapacity = 64)
        override val events: Flow<JamGuestEvent> = eventFlow.asSharedFlow()

        val sent = mutableListOf<JamMessage>()

        suspend fun emit(event: JamGuestEvent) {
            eventFlow.emit(event)
        }

        override suspend fun connect(code: JamSessionCode, name: String, guestId: String): Boolean = true

        override suspend fun send(message: JamMessage) {
            sent += message
        }

        override fun stop() = Unit
    }
}
