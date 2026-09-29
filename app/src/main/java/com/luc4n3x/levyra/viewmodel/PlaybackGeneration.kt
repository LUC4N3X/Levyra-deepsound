package com.luc4n3x.levyra.viewmodel

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

internal data class PlaybackTicket(
    val generation: Long,
    val identity: String
)

internal class PlaybackGenerationGuard {
    private val counter = AtomicLong(0L)
    private val active = AtomicReference(PlaybackTicket(generation = 0L, identity = ""))

    val currentGeneration: Long
        get() = active.get().generation

    fun begin(identity: String): PlaybackTicket =
        active.updateAndGet { PlaybackTicket(counter.incrementAndGet(), identity) }

    fun current(): PlaybackTicket = active.get()

    fun isCurrent(ticket: PlaybackTicket): Boolean = active.get().generation == ticket.generation

    fun accepts(ticket: PlaybackTicket, resultIdentity: String): Boolean {
        val latest = active.get()
        return latest.generation == ticket.generation &&
            (ticket.identity.isEmpty() || ticket.identity == resultIdentity)
    }
}
