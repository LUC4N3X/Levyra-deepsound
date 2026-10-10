package com.luc4n3x.levyra.feature.jam

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JamModerationProtocolTest {

    @Test
    fun pendingNoticeRoundTripsAndRequiresAHostProof() {
        val message = JamMessage.Pending("host-proof")

        assertEquals(message, JamProtocol.decode(JamProtocol.encode(message)))
        assertNull(JamProtocol.decode("""{"v":${JamProtocol.VERSION},"t":"pending","hostProof":"  "}"""))
    }

    @Test
    fun guestIdentityRoundTripsAndRejectsMalformedValues() {
        val identity = "a".repeat(JamIdentity.LENGTH)
        val message = JamMessage.Authenticate("nonce", "Ada", "proof", identity)

        assertEquals(message, JamProtocol.decode(JamProtocol.encode(message)))

        val decoded = JamProtocol.decode(
            """{"v":${JamProtocol.VERSION},"t":"auth","guestNonce":"n","proof":"p","guestId":"not-hex"}"""
        ) as JamMessage.Authenticate
        assertEquals("", decoded.guestId)

        val uppercase = JamProtocol.decode(
            """{"v":${JamProtocol.VERSION},"t":"auth","guestNonce":"n","proof":"p","guestId":"${identity.uppercase()}"}"""
        ) as JamMessage.Authenticate
        assertEquals(identity, uppercase.guestId)
    }

    @Test
    fun identityValidationRejectsWrongLengthAndNonHex() {
        assertTrue(JamIdentity.isValid("0".repeat(JamIdentity.LENGTH)))
        assertFalse(JamIdentity.isValid("0".repeat(JamIdentity.LENGTH - 1)))
        assertFalse(JamIdentity.isValid("0".repeat(JamIdentity.LENGTH + 1)))
        assertFalse(JamIdentity.isValid("g".repeat(JamIdentity.LENGTH)))
        assertEquals("", JamIdentity.sanitize(""))
    }

    @Test
    fun moderationFlagsRoundTripAndDefaultToOpenForLegacyPayloads() {
        val state = sampleState(locked = true, requireApproval = true)
        val encoded = JSONObject(JamProtocol.encode(stateMessage(state)))

        val decoded = JamProtocol.decode(encoded.toString()) as JamMessage.State
        assertTrue(decoded.state.locked)
        assertTrue(decoded.state.requireApproval)

        val payload = encoded.getJSONObject("payload")
        payload.remove("locked")
        payload.remove("requireApproval")
        val legacy = JamProtocol.decode(encoded.toString()) as JamMessage.State
        assertFalse(legacy.state.locked)
        assertFalse(legacy.state.requireApproval)
    }

    @Test
    fun contributorAttributionSurvivesEncodingAndStaysBounded() {
        val state = sampleState(addedBy = "guest-1")
        val decoded = JamProtocol.decode(JamProtocol.encode(stateMessage(state))) as JamMessage.State
        assertEquals("guest-1", decoded.state.queue.single().addedBy)

        val oversized = JSONObject(JamProtocol.encode(stateMessage(sampleState())))
        oversized.getJSONObject("payload").getJSONArray("queue").getJSONObject(0)
            .put("addedBy", "x".repeat(JamSessionState.MAX_TEXT_LENGTH * 3))
        val trimmed = JamProtocol.decode(oversized.toString()) as JamMessage.State
        assertEquals(JamSessionState.MAX_TEXT_LENGTH, trimmed.state.queue.single().addedBy.length)
    }

    @Test
    fun moderationFailureCodesRoundTrip() {
        listOf(
            JamFailure.Rejected,
            JamFailure.Banned,
            JamFailure.SessionLocked,
            JamFailure.SessionFull,
            JamFailure.Removed
        ).forEach { failure ->
            val message = JamMessage.Failure(failure)
            assertEquals(message, JamProtocol.decode(JamProtocol.encode(message)))
        }
    }

    @Test
    fun moderationCapabilityIsAdvertised() {
        assertTrue(JamCapabilities.MODERATION in JamCapabilities.current)
        val decoded = JamProtocol.decode(JamProtocol.encode(stateMessage(sampleState()))) as JamMessage.State
        assertTrue(JamCapabilities.MODERATION in decoded.state.capabilities)
    }

    @Test
    fun boundsCoverPendingAndBannedCollections() {
        assertTrue(JamSessionState.MAX_PENDING in 1..JamSessionState.MAX_PARTICIPANTS)
        assertTrue(JamSessionState.MAX_BANNED in 1..256)
    }

    private fun stateMessage(state: JamSessionState) =
        JamMessage.State(state.sessionId, state.revision, state.hostId, 123_456L, state)

    private fun sampleState(
        locked: Boolean = false,
        requireApproval: Boolean = false,
        addedBy: String = ""
    ) = JamSessionState(
        sessionId = "session-1",
        hostId = "host-1",
        revision = 7L,
        createdAt = 100L,
        participants = listOf(
            JamParticipant("host-1", "Host", true),
            JamParticipant("guest-1", "Guest", false)
        ),
        queue = listOf(JamTrack("track-1", "Song", "Artist", 180_000L, "", addedBy)),
        currentIndex = 0,
        currentMediaId = "track-1",
        positionMs = 4_000L,
        playWhenReady = true,
        shuffle = false,
        repeatMode = 0,
        permission = JamGuestPermission.Collaborative,
        updatedAtElapsedMs = 0L,
        capabilities = JamCapabilities.current,
        locked = locked,
        requireApproval = requireApproval
    )
}
