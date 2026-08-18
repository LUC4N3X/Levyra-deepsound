package com.luc4n3x.levyra.feature.dearrow

object DeArrowSelectionPolicy {
    const val MAX_TITLE_LENGTH = 150

    fun selectTitle(titles: List<DeArrowTitle>): String? {
        val candidate = bestCandidate(titles.filterNot { it.original }) ?: return null
        val cleaned = stripFormattingMarker(candidate.title).trim()
        if (cleaned.isBlank() || cleaned.length > MAX_TITLE_LENGTH) return null
        return cleaned
    }

    fun selectThumbnailTimestamp(thumbnails: List<DeArrowThumbnail>): Double? {
        val eligible = thumbnails.filterNot { it.original }.filter { thumbnail ->
            val timestamp = thumbnail.timestamp
            timestamp != null && timestamp >= 0.0
        }
        return bestCandidate(eligible)?.timestamp
    }

    private fun <T : DeArrowVotable> bestCandidate(candidates: List<T>): T? {
        val eligible = candidates.filter { it.votes >= 0 }
        if (eligible.isEmpty()) return null
        val locked = eligible.filter { it.locked }
        val pool = locked.ifEmpty { eligible }
        return pool.maxByOrNull { it.votes }
    }

    private fun stripFormattingMarker(title: String): String =
        title.replace(Regex("(^|\\s)>"), "$1")
}
