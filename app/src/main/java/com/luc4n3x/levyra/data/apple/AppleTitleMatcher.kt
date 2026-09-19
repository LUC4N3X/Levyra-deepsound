package com.luc4n3x.levyra.data.apple

internal object AppleTitleMatcher {
    fun score(referenceCore: String, candidateCore: String): Pair<Int, String?> {
        if (referenceCore.isBlank() || candidateCore.isBlank()) return 0 to "title_core_blank"
        val coverage = AppleMatchScoring.tokenCoverage(referenceCore, candidateCore)
        if (referenceCore != candidateCore && coverage < 0.60) return 0 to "title_mismatch"
        return when {
            referenceCore == candidateCore -> 28 to null
            coverage >= 0.85 -> 20 to null
            else -> (coverage * 15).toInt() to null
        }
    }

}
