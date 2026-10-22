package com.luc4n3x.levyra.feature.discovery

import androidx.compose.runtime.Immutable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters

enum class DiscoveryDecision { Saved, Dismissed }

@Immutable
data class DiscoveryStep(
    val card: DiscoveryCard,
    val decision: DiscoveryDecision,
    val playlistId: String = "",
    val savedTrackId: String = ""
)

@Immutable
data class DiscoveryDeck(
    val cards: List<DiscoveryCard> = emptyList(),
    val index: Int = 0,
    val history: List<DiscoveryStep> = emptyList()
) {
    val current: DiscoveryCard? get() = cards.getOrNull(index)
    val upcoming: List<DiscoveryCard> get() = cards.drop(index + 1).take(UPCOMING)
    val remaining: Int get() = (cards.size - index).coerceAtLeast(0)
    val canUndo: Boolean get() = history.isNotEmpty()

    fun decide(decision: DiscoveryDecision, playlistId: String = "", savedTrackId: String = ""): DiscoveryDeck {
        val card = current ?: return this
        return copy(index = index + 1, history = (history + DiscoveryStep(card, decision, playlistId, savedTrackId)).takeLast(MAX_HISTORY))
    }

    fun undo(): Pair<DiscoveryDeck, DiscoveryStep>? {
        val step = history.lastOrNull() ?: return null
        val restoredIndex = cards.indexOfFirst { it.key == step.card.key }.takeIf { it >= 0 } ?: return null
        return copy(index = restoredIndex, history = history.dropLast(1)) to step
    }

    fun appended(more: List<DiscoveryCard>): DiscoveryDeck {
        val known = cards.mapTo(HashSet()) { it.key }
        val fresh = more.filter { known.add(it.key) }
        return if (fresh.isEmpty()) this else copy(cards = cards + fresh)
    }

    companion object {
        const val UPCOMING = 3
        const val MAX_HISTORY = 30
    }
}

data class DiscoveryWeek(val id: String, val monday: LocalDate) {
    companion object {
        fun of(date: LocalDate): DiscoveryWeek {
            val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val year = date.get(IsoFields.WEEK_BASED_YEAR)
            val week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
            return DiscoveryWeek("levyra-discovery-$year-w${week.toString().padStart(2, '0')}", monday)
        }
    }
}
