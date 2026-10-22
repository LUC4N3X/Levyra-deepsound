package com.luc4n3x.levyra.feature.radio

import java.util.Locale

internal data class RadioSearchRequest(
    val field: String,
    val value: String,
    val offset: Int = 0,
    val tokenFallback: Boolean = false
)

internal data class RadioStationPage(val stations: List<RadioStation>, val rawCount: Int)

internal data class RadioSearchPage(
    val stations: List<RadioStation>,
    val next: List<RadioSearchRequest>,
    val partialFailure: Boolean = false
)

internal fun radioSearchRequests(query: String): List<RadioSearchRequest> = buildList {
    listOf("name", "country", "language", "tag").forEach { add(RadioSearchRequest(it, query)) }
    val normalized = normalizeRadioName(query)
    if (normalized.isNotBlank() && !normalized.equals(query, true)) add(RadioSearchRequest("name", normalized))
    radioSearchRequestTokens(query).take(4).forEach { token ->
        add(RadioSearchRequest("name", token, tokenFallback = true))
    }
}.distinctBy { it.field to it.value.lowercase(Locale.ROOT) }
