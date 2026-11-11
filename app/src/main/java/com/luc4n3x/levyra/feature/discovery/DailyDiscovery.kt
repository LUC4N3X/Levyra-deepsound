package com.luc4n3x.levyra.feature.discovery

internal const val DAILY_DISCOVERY_LIMIT = 12

internal fun dailyDiscoveryCards(
    cards: List<DiscoveryCard>,
    hiddenKeys: Set<String>,
    familiarArtistKeys: Set<String> = emptySet(),
    limit: Int = DAILY_DISCOVERY_LIMIT
): List<DiscoveryCard> = cards.asSequence()
    .filter { it.isDailyDiscovery() && it.key !in hiddenKeys }
    .filter { card ->
        val artist = discoveryPrimaryArtistKey(card.track.artist)
        artist !in familiarArtistKeys && artist != discoveryPrimaryArtistKey(card.reason.seedArtist)
    }
    .distinctBy { discoveryPrimaryArtistKey(it.track.artist) }
    .take(limit)
    .toList()

private fun DiscoveryCard.isDailyDiscovery(): Boolean =
    reason.kind == DiscoveryReasonKind.SimilarToTrack && reason.seedTitle.isNotBlank() && reason.seedArtist.isNotBlank()
