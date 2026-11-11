package com.luc4n3x.levyra.feature.discovery

internal const val DAILY_DISCOVERY_LIMIT = 12

internal fun dailyDiscoveryCards(
    cards: List<DiscoveryCard>,
    hiddenKeys: Set<String>,
    familiarArtistKeys: Set<String> = emptySet(),
    limit: Int = DAILY_DISCOVERY_LIMIT
): List<DiscoveryCard> {
    val bySeed = cards.asSequence()
        .filter { it.isDailyDiscovery() && it.key !in hiddenKeys }
        .filter { card ->
            val artist = discoveryPrimaryArtistKey(card.track.artist)
            artist !in familiarArtistKeys && artist != discoveryPrimaryArtistKey(card.reason.seedArtist)
        }
        .distinctBy { discoveryPrimaryArtistKey(it.track.artist) }
        .groupBy { it.reason.seedTitle to it.reason.seedArtist }
        .values
    val rounds = bySeed.maxOfOrNull { it.size } ?: 0
    return (0 until rounds).flatMap { round -> bySeed.mapNotNull { it.getOrNull(round) } }.take(limit)
}

private fun DiscoveryCard.isDailyDiscovery(): Boolean =
    reason.kind == DiscoveryReasonKind.SimilarToTrack && reason.seedTitle.isNotBlank() && reason.seedArtist.isNotBlank()
