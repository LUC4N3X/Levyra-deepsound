package com.luc4n3x.levyra.feature.jam

import kotlinx.coroutines.flow.Flow

sealed interface JamHostEvent {
    data class GuestPending(
        val participantId: String,
        val guestId: String,
        val name: String,
        val hostProof: String
    ) : JamHostEvent

    data class GuestLeft(val participantId: String) : JamHostEvent
    data class ActionReceived(val participantId: String, val action: JamAction) : JamHostEvent
    data class Failed(val failure: JamFailure) : JamHostEvent
}

sealed interface JamGuestEvent {
    data class Connected(val sessionId: String, val participantId: String) : JamGuestEvent
    data object AwaitingApproval : JamGuestEvent
    data class StateReceived(val message: JamMessage.State) : JamGuestEvent
    data class Failed(val failure: JamFailure) : JamGuestEvent
    data object Disconnected : JamGuestEvent
}

interface JamHostTransport {
    val events: Flow<JamHostEvent>

    suspend fun start(secret: String): JamSessionCode?

    suspend fun notifyPending(participantId: String, message: JamMessage.Pending)

    suspend fun admit(participantId: String, welcome: JamMessage.Welcome): Boolean

    suspend fun reject(participantId: String, failure: JamFailure)

    suspend fun broadcast(message: JamMessage)

    suspend fun send(participantId: String, message: JamMessage)

    suspend fun disconnect(participantId: String)

    fun stop()
}

interface JamGuestTransport {
    val events: Flow<JamGuestEvent>

    suspend fun connect(code: JamSessionCode, name: String, guestId: String): Boolean

    suspend fun send(message: JamMessage)

    fun stop()
}

interface JamTransportFactory {
    fun host(): JamHostTransport

    fun guest(): JamGuestTransport
}
