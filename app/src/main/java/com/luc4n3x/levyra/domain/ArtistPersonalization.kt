package com.luc4n3x.levyra.domain

import java.util.Locale
import java.util.concurrent.TimeUnit

data class PersonalizedArtistCandidate(
    val browseId: String,
    val name: String,
    val score: Long,
    val lastListenedAt: Long
)

internal fun rankPersonalizedArtistCandidates(
    events: List<ListenEvent>,
    nowMs: Long = System.currentTimeMillis(),
    limit: Int = 16
): List<PersonalizedArtistCandidate> {
    if (limit <= 0 || events.isEmpty()) return emptyList()
    return events
        .asSequence()
        .filter { event ->
            event.startedAt in 1..nowMs &&
                event.listenedMs >= ListeningPulseEngine.MIN_LISTEN_MS &&
                event.artistBrowseIds.firstOrNull().orEmpty().isNotBlank()
        }
        .mapNotNull { event ->
            val browseId = event.artistBrowseIds.firstOrNull().orEmpty().trim()
            val name = primaryArtistSegment(event.artist).ifBlank { event.artist.trim() }
            if (browseId.isBlank() || name.length < 2 || !isArtistShelfNameEligible(name)) null
            else RankedArtistEvent(browseId, name, event)
        }
        .groupBy { it.browseId.lowercase(Locale.ROOT) }
        .mapNotNull { (_, group) ->
            val newest = group.maxByOrNull { it.event.startedAt } ?: return@mapNotNull null
            val eventScore = group.sumOf { ranked ->
                val ageMs = (nowMs - ranked.event.startedAt).coerceAtLeast(0L)
                val ageDays = TimeUnit.MILLISECONDS.toDays(ageMs)
                val recency = when {
                    ageDays <= 2L -> 8L
                    ageDays <= 7L -> 6L
                    ageDays <= 30L -> 4L
                    ageDays <= 90L -> 2L
                    else -> 1L
                }
                val listenedSeconds = TimeUnit.MILLISECONDS.toSeconds(ranked.event.listenedMs)
                    .coerceIn(5L, MAX_LISTEN_SECONDS_PER_EVENT)
                listenedSeconds * recency + if (ranked.event.completed) COMPLETION_BONUS * recency else 0L
            }
            val repeatBonus = (group.size - 1).coerceIn(0, MAX_REPEAT_BONUS_PLAYS) * REPEAT_BONUS
            PersonalizedArtistCandidate(
                browseId = newest.browseId,
                name = newest.name,
                score = eventScore + repeatBonus,
                lastListenedAt = newest.event.startedAt
            )
        }
        .sortedWith(
            compareByDescending<PersonalizedArtistCandidate> { it.score }
                .thenByDescending { it.lastListenedAt }
                .thenBy { it.name.lowercase(Locale.ROOT) }
        )
        .take(limit)
        .toList()
}

private data class RankedArtistEvent(
    val browseId: String,
    val name: String,
    val event: ListenEvent
)

private const val MAX_LISTEN_SECONDS_PER_EVENT = 1_200L
private const val COMPLETION_BONUS = 180L
private const val REPEAT_BONUS = 240L
private const val MAX_REPEAT_BONUS_PLAYS = 12
