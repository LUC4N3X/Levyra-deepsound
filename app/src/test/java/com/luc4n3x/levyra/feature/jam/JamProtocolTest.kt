package com.luc4n3x.levyra.feature.jam

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JamProtocolTest {

    @Test
    fun stateRoundTripsWithBoundedFields() {
        val state = sampleState()
        val message = JamMessage.State(
            sessionId = state.sessionId,
            revision = state.revision,
            source = state.hostId,
            timestamp = 123_456L,
            state = state
        )

        assertEquals(message, JamProtocol.decode(JamProtocol.encode(message)))
    }

    @Test
    fun everyActionRoundTrips() {
        val actions = listOf(
            JamAction.AddTrack(sampleTrack()),
            JamAction.RemoveTrack("track-1"),
            JamAction.SelectIndex(0),
            JamAction.SetPlayWhenReady(true),
            JamAction.Seek(12_345L),
            JamAction.Next,
            JamAction.Previous
        )

        actions.forEach { action ->
            val message = JamMessage.Action("session-1", "guest-1", action)
            assertEquals(message, JamProtocol.decode(JamProtocol.encode(message)))
        }
    }

    @Test
    fun malformedOversizedAndUnknownVersionPayloadsAreRejected() {
        assertNull(JamProtocol.decode("not-json"))
        assertNull(JamProtocol.decode("x".repeat(JamProtocol.MAX_MESSAGE_BYTES + 1)))
        assertNull(JamProtocol.decode("""{"v":${JamProtocol.VERSION + 1},"t":"bye"}"""))
        assertNull(JamProtocol.decode("""{"v":${JamProtocol.VERSION},"t":"error","code":"FutureFailure"}"""))
    }

    @Test
    fun oversizedQueueAndParticipantsAreRejected() {
        val encoded = JSONObject(JamProtocol.encode(stateMessage()))
        val payload = encoded.getJSONObject("payload")
        val queue = JSONArray()
        repeat(JamSessionState.MAX_QUEUE_SIZE + 1) { queue.put(trackJson("track-$it")) }
        payload.put("queue", queue)
        assertNull(JamProtocol.decode(encoded.toString()))

        val participants = JSONArray()
        repeat(JamSessionState.MAX_PARTICIPANTS + 1) {
            participants.put(JSONObject().put("id", "p-$it").put("name", "Guest").put("isHost", it == 0))
        }
        payload.put("queue", JSONArray().put(trackJson("track-1")))
        payload.put("participants", participants)
        assertNull(JamProtocol.decode(encoded.toString()))
    }

    @Test
    fun namesAndThumbnailUrlsAreSanitized() {
        val auth = JamProtocol.decode(
            """{"v":1,"t":"auth","guestNonce":"1234567890abcdef","name":"  A\u0000lice\n  ","proof":"abcdef123456"}"""
        ) as JamMessage.Authenticate
        assertEquals("Alice", auth.name)

        val encoded = JSONObject(JamProtocol.encode(stateMessage()))
        encoded.getJSONObject("payload").getJSONArray("queue").getJSONObject(0)
            .put("thumbnailUrl", "https://")
        val decoded = JamProtocol.decode(encoded.toString()) as JamMessage.State
        assertTrue(decoded.state.queue.single().thumbnailUrl.isEmpty())
    }

    @Test
    fun invalidRevisionIsRejected() {
        val encoded = JSONObject(JamProtocol.encode(stateMessage())).put("revision", -1L)

        assertNull(JamProtocol.decode(encoded.toString()))
        assertTrue(JamPlaybackSync.isStaleRevision(4L, 4L))
        assertTrue(JamPlaybackSync.isStaleRevision(3L, 4L))
    }

    @Test
    fun inconsistentHostQueueAndMediaIdentityAreRejected() {
        val missingHost = JSONObject(JamProtocol.encode(stateMessage()))
        missingHost.getJSONObject("payload").put("hostId", "missing")
        assertNull(JamProtocol.decode(missingHost.toString()))

        val duplicateTrack = JSONObject(JamProtocol.encode(stateMessage()))
        duplicateTrack.getJSONObject("payload").getJSONArray("queue").put(trackJson("track-1"))
        assertNull(JamProtocol.decode(duplicateTrack.toString()))

        val mismatchedMedia = JSONObject(JamProtocol.encode(stateMessage()))
        mismatchedMedia.getJSONObject("payload").put("currentMediaId", "other")
        assertNull(JamProtocol.decode(mismatchedMedia.toString()))
    }

    private fun stateMessage(): JamMessage.State {
        val state = sampleState()
        return JamMessage.State(state.sessionId, state.revision, state.hostId, 123_456L, state)
    }

    private fun sampleState() = JamSessionState(
        sessionId = "session-1",
        hostId = "host-1",
        revision = 7L,
        createdAt = 100L,
        participants = listOf(
            JamParticipant("host-1", "Host", true),
            JamParticipant("guest-1", "Guest", false)
        ),
        queue = listOf(sampleTrack()),
        currentIndex = 0,
        currentMediaId = "track-1",
        positionMs = 4_000L,
        playWhenReady = true,
        shuffle = false,
        repeatMode = 0,
        permission = JamGuestPermission.Collaborative,
        updatedAtElapsedMs = 0L
    )

    private fun sampleTrack() = JamTrack("track-1", "Song", "Artist", 180_000L, "https://example.com/a.jpg")

    private fun trackJson(id: String) = JSONObject()
        .put("id", id)
        .put("title", "Song")
        .put("artist", "Artist")
        .put("durationMs", 180_000L)
        .put("thumbnailUrl", "https://example.com/a.jpg")
}
