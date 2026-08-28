package com.luc4n3x.levyra.feature.jam

import com.luc4n3x.levyra.data.security.SafeImageUrlPolicy
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import org.json.JSONArray
import org.json.JSONObject

sealed interface JamMessage {
    data class Challenge(val hostNonce: String) : JamMessage
    data class Authenticate(val guestNonce: String, val name: String, val proof: String) : JamMessage
    data class Welcome(val sessionId: String, val participantId: String, val hostProof: String) : JamMessage
    data class State(
        val sessionId: String,
        val revision: Long,
        val source: String,
        val timestamp: Long,
        val state: JamSessionState
    ) : JamMessage
    data class Action(
        val sessionId: String,
        val participantId: String,
        val action: JamAction
    ) : JamMessage
    data class Bye(val reason: String) : JamMessage
    data class Failure(val failure: JamFailure) : JamMessage
}

object JamAuth {
    private const val GUEST_AUTH_CONTEXT = "levyra-jam-guest-auth-v1"
    private const val HOST_WELCOME_CONTEXT = "levyra-jam-host-welcome-v1"

    fun generateNonce(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun computeGuestProof(secret: String, hostNonce: String, guestNonce: String): String =
        hmacSha256(secret, "$GUEST_AUTH_CONTEXT:$hostNonce:$guestNonce")

    fun computeHostProof(secret: String, hostNonce: String, guestNonce: String): String =
        hmacSha256(secret, "$HOST_WELCOME_CONTEXT:$hostNonce:$guestNonce")

    fun verifyProof(expected: String, actual: String): Boolean {
        if (expected.isEmpty() || actual.isEmpty()) return false
        val expectedBytes = expected.toByteArray(Charsets.UTF_8)
        val actualBytes = actual.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(expectedBytes, actualBytes)
    }

    private fun hmacSha256(key: String, message: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKey)
        val bytes = mac.doFinal(message.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

object JamProtocol {
    const val VERSION = 1
    const val MAX_MESSAGE_BYTES = 512 * 1024

    private const val TYPE = "t"
    private const val VERSION_FIELD = "v"

    fun encode(message: JamMessage): String = when (message) {
        is JamMessage.Challenge -> base("challenge").apply {
            put("hostNonce", message.hostNonce)
        }
        is JamMessage.Authenticate -> base("auth").apply {
            put("guestNonce", message.guestNonce)
            put("name", message.name.take(JamSessionState.MAX_NAME_LENGTH))
            put("proof", message.proof)
        }
        is JamMessage.Welcome -> base("welcome").apply {
            put("sessionId", message.sessionId)
            put("participantId", message.participantId)
            put("hostProof", message.hostProof)
        }
        is JamMessage.State -> base("state").apply {
            put("sessionId", message.sessionId)
            put("revision", message.revision)
            put("source", message.source)
            put("timestamp", message.timestamp)
            put("payload", encodeState(message.state))
        }
        is JamMessage.Action -> base("action").apply {
            put("sessionId", message.sessionId)
            put("participantId", message.participantId)
            put("action", encodeAction(message.action))
        }
        is JamMessage.Bye -> base("bye").apply {
            put("reason", message.reason.take(JamSessionState.MAX_TEXT_LENGTH))
        }
        is JamMessage.Failure -> base("error").apply { put("code", message.failure.name) }
    }.toString()

    fun decode(raw: String): JamMessage? {
        if (raw.isBlank() || raw.toByteArray(Charsets.UTF_8).size > MAX_MESSAGE_BYTES) return null
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        if (root.optInt(VERSION_FIELD, -1) != VERSION) return null
        return when (root.optString(TYPE)) {
            "challenge" -> JamMessage.Challenge(
                hostNonce = root.optString("hostNonce").trim().take(JamSessionState.MAX_TEXT_LENGTH)
            ).takeIf { it.hostNonce.isNotBlank() }
            "auth" -> JamMessage.Authenticate(
                guestNonce = root.optString("guestNonce").trim().take(JamSessionState.MAX_TEXT_LENGTH),
                name = sanitizeName(root.optString("name")),
                proof = root.optString("proof").trim().take(JamSessionState.MAX_TEXT_LENGTH)
            ).takeIf { it.guestNonce.isNotBlank() && it.proof.isNotBlank() }
            "welcome" -> JamMessage.Welcome(
                sessionId = root.optString("sessionId").trim().take(JamSessionState.MAX_TEXT_LENGTH),
                participantId = root.optString("participantId").trim().take(JamSessionState.MAX_TEXT_LENGTH),
                hostProof = root.optString("hostProof").trim().take(JamSessionState.MAX_TEXT_LENGTH)
            ).takeIf { it.sessionId.isNotBlank() && it.participantId.isNotBlank() && it.hostProof.isNotBlank() }
            "state" -> decodeState(root)
            "action" -> decodeAction(root)
            "bye" -> JamMessage.Bye(root.optString("reason").take(JamSessionState.MAX_TEXT_LENGTH))
            "error" -> runCatching { JamFailure.valueOf(root.optString("code")) }.getOrNull()?.let(JamMessage::Failure)
            else -> null
        }
    }

    internal fun sanitizeName(value: String): String =
        value.filterNot { it.isISOControl() }.trim().take(JamSessionState.MAX_NAME_LENGTH)

    private fun base(type: String): JSONObject = JSONObject()
        .put(VERSION_FIELD, VERSION)
        .put(TYPE, type)

    private fun encodeState(state: JamSessionState): JSONObject = JSONObject().apply {
        put("hostId", state.hostId)
        put("createdAt", state.createdAt)
        put("currentIndex", state.currentIndex)
        put("currentMediaId", state.currentMediaId)
        put("positionMs", state.positionMs)
        put("playWhenReady", state.playWhenReady)
        put("shuffle", state.shuffle)
        put("repeatMode", state.repeatMode)
        put("permission", state.permission.id)
        put(
            "participants",
            JSONArray().apply {
                state.participants.take(JamSessionState.MAX_PARTICIPANTS).forEach { participant ->
                    put(
                        JSONObject()
                            .put("id", participant.id)
                            .put("name", participant.name)
                            .put("isHost", participant.isHost)
                    )
                }
            }
        )
        put(
            "queue",
            JSONArray().apply {
                state.queue.take(JamSessionState.MAX_QUEUE_SIZE).forEach { track -> put(encodeTrack(track)) }
            }
        )
    }

    private fun decodeState(root: JSONObject): JamMessage.State? {
        val sessionId = boundedIdentifier(root.optString("sessionId")) ?: return null
        val revision = root.optLong("revision", -1L)
        if (revision < 0L) return null
        val source = boundedIdentifier(root.optString("source")) ?: return null
        val timestamp = root.optLong("timestamp", -1L)
        if (timestamp < 0L) return null
        val payload = root.optJSONObject("payload") ?: return null
        val hostId = boundedIdentifier(payload.optString("hostId")) ?: return null
        val decodedQueue = decodeTracks(payload.optJSONArray("queue")) ?: return null
        val decodedParticipants = decodeParticipants(payload.optJSONArray("participants")) ?: return null
        if (decodedParticipants.count { it.isHost } != 1 ||
            decodedParticipants.none { it.id == hostId && it.isHost }
        ) return null
        val currentIndex = payload.optInt("currentIndex", -2)
        if (decodedQueue.isEmpty() && currentIndex != -1) return null
        if (decodedQueue.isNotEmpty() && currentIndex !in decodedQueue.indices) return null
        val currentMediaId = payload.optString("currentMediaId").trim()
        if (currentMediaId.length > JamSessionState.MAX_TEXT_LENGTH) return null
        if (decodedQueue.getOrNull(currentIndex)?.id.orEmpty() != currentMediaId) return null
        val state = JamSessionState(
            sessionId = sessionId,
            hostId = hostId,
            revision = revision,
            createdAt = payload.optLong("createdAt", 0L).coerceAtLeast(0L),
            participants = decodedParticipants,
            queue = decodedQueue,
            currentIndex = currentIndex,
            currentMediaId = currentMediaId,
            positionMs = payload.optLong("positionMs", 0L).coerceAtLeast(0L),
            playWhenReady = payload.optBoolean("playWhenReady", false),
            shuffle = payload.optBoolean("shuffle", false),
            repeatMode = payload.optInt("repeatMode", 0).coerceIn(0, 2),
            permission = JamGuestPermission.fromId(payload.optString("permission")),
            updatedAtElapsedMs = 0L
        )
        return JamMessage.State(
            sessionId = sessionId,
            revision = revision,
            source = source,
            timestamp = timestamp,
            state = state
        )
    }

    private fun decodeParticipants(array: JSONArray?): List<JamParticipant>? {
        array ?: return emptyList()
        if (array.length() > JamSessionState.MAX_PARTICIPANTS) return null
        val participants = mutableListOf<JamParticipant>()
        for (index in 0 until array.length()) {
            val entry = array.optJSONObject(index) ?: return null
            val id = boundedIdentifier(entry.optString("id")) ?: return null
            if (participants.any { it.id == id }) return null
            participants += JamParticipant(
                id = id,
                name = sanitizeName(entry.optString("name")),
                isHost = entry.optBoolean("isHost", false)
            )
        }
        return participants
    }

    private fun encodeTrack(track: JamTrack): JSONObject = JSONObject()
        .put("id", track.id)
        .put("title", track.title.take(JamSessionState.MAX_TEXT_LENGTH))
        .put("artist", track.artist.take(JamSessionState.MAX_TEXT_LENGTH))
        .put("durationMs", track.durationMs)
        .put("thumbnailUrl", track.thumbnailUrl.take(JamSessionState.MAX_TEXT_LENGTH))

    private fun decodeTrack(entry: JSONObject?): JamTrack? {
        entry ?: return null
        val id = boundedIdentifier(entry.optString("id")) ?: return null
        return JamTrack(
            id = id,
            title = entry.optString("title").filterNot { it.isISOControl() }.take(JamSessionState.MAX_TEXT_LENGTH),
            artist = entry.optString("artist").filterNot { it.isISOControl() }.take(JamSessionState.MAX_TEXT_LENGTH),
            durationMs = entry.optLong("durationMs", 0L).coerceAtLeast(0L),
            thumbnailUrl = sanitizedHttpsUrl(entry.optString("thumbnailUrl"))
        )
    }

    private fun decodeTracks(array: JSONArray?): List<JamTrack>? {
        array ?: return emptyList()
        if (array.length() > JamSessionState.MAX_QUEUE_SIZE) return null
        val tracks = mutableListOf<JamTrack>()
        for (index in 0 until array.length()) {
            val track = decodeTrack(array.optJSONObject(index)) ?: return null
            if (tracks.any { it.id == track.id }) return null
            tracks += track
        }
        return tracks
    }

    private fun sanitizedHttpsUrl(value: String): String =
        SafeImageUrlPolicy.sanitize(value)

    private fun encodeAction(action: JamAction): JSONObject = when (action) {
        is JamAction.AddTrack -> JSONObject().put("kind", "add").put("track", encodeTrack(action.track))
        is JamAction.RemoveTrack -> JSONObject().put("kind", "remove").put("trackId", action.trackId)
        is JamAction.SelectIndex -> JSONObject().put("kind", "select").put("index", action.index)
        is JamAction.SetPlayWhenReady -> JSONObject().put("kind", "play").put("playWhenReady", action.playWhenReady)
        is JamAction.Seek -> JSONObject().put("kind", "seek").put("positionMs", action.positionMs)
        JamAction.Next -> JSONObject().put("kind", "next")
        JamAction.Previous -> JSONObject().put("kind", "previous")
    }

    private fun decodeAction(root: JSONObject): JamMessage.Action? {
        val sessionId = boundedIdentifier(root.optString("sessionId")) ?: return null
        val participantId = boundedIdentifier(root.optString("participantId")) ?: return null
        val payload = root.optJSONObject("action") ?: return null
        val action = when (payload.optString("kind")) {
            "add" -> decodeTrack(payload.optJSONObject("track"))?.let(JamAction::AddTrack)
            "remove" -> payload.optString("trackId").trim().take(JamSessionState.MAX_TEXT_LENGTH)
                .takeIf { it.isNotBlank() }
                ?.let(JamAction::RemoveTrack)
            "select" -> payload.optInt("index", -1)
                .takeIf { it in 0 until JamSessionState.MAX_QUEUE_SIZE }
                ?.let(JamAction::SelectIndex)
            "play" -> JamAction.SetPlayWhenReady(payload.optBoolean("playWhenReady", false))
            "seek" -> payload.optLong("positionMs", -1L)
                .takeIf { it >= 0L }
                ?.let(JamAction::Seek)
            "next" -> JamAction.Next
            "previous" -> JamAction.Previous
            else -> null
        } ?: return null
        return JamMessage.Action(sessionId, participantId, action)
    }

    private fun boundedIdentifier(value: String): String? {
        val normalized = value.trim()
        return normalized.takeIf { it.isNotBlank() && it.length <= JamSessionState.MAX_TEXT_LENGTH }
    }
}
