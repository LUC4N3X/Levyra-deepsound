package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.data.LyricsPhraseHit

enum class LyricsPhraseSearchStatus { IDLE, TOO_SHORT, SEARCHING, RESULTS, RESULTS_UNVERIFIED, NO_MATCH, UNAVAILABLE }

data class LyricsPhraseSearchUiState(
    val visible: Boolean = false,
    val query: String = "",
    val status: LyricsPhraseSearchStatus = LyricsPhraseSearchStatus.IDLE,
    val hits: List<LyricsPhraseHit> = emptyList()
)

internal fun isCurrentLyricsRequest(
    requestGeneration: Long,
    currentGeneration: Long,
    requestedTrackIdentity: String,
    currentTrackIdentity: String?
): Boolean = requestGeneration == currentGeneration && requestedTrackIdentity == currentTrackIdentity
