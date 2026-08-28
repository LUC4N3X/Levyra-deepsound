package com.luc4n3x.levyra.feature.jam

import kotlinx.coroutines.flow.Flow

sealed interface JamHostEvent {
    data class GuestJoined(val participantId: String, val name: String, val hostProof: String) : JamHostEvent
    data class GuestLeft(val participantId: String) : JamHostEvent
    data class ActionReceived(val participantId: String, val action: JamAction) : JamHostEvent
    data class Failed(val failure: JamFailure) : JamHostEvent
}

sealed interface JamGuestEvent {
    data class Connected(val sessionId: String, val participantId: String) : JamGuestEvent
    data class StateReceived(val message: JamMessage.State) : JamGuestEvent
    data class Failed(val failure: JamFailure) : JamGuestEvent
    data object Disconnected : JamGuestEvent
}

interface JamHostTransport {
    val events: Flow<JamHostEvent>

    suspend fun start(secret: String): JamSessionCode?

    suspend fun broadcast(message: JamMessage)

    suspend fun send(participantId: String, message: JamMessage)

    suspend fun disconnect(participantId: String)

    fun stop()
}

interface JamGuestTransport {
    val events: Flow<JamGuestEvent>

    suspend fun connect(code: JamSessionCode, name: String): Boolean

    suspend fun send(message: JamMessage)

    fun stop()
}

interface JamTransportFactory {
    fun host(): JamHostTransport

    fun guest(): JamGuestTransport
}
