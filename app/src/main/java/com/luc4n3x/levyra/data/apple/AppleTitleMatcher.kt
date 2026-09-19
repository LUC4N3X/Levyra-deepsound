package com.luc4n3x.levyra.data.apple

internal object AppleTitleMatcher {
    fun score(referenceCore: String, candidateCore: String): Pair<Int, String?> {
        if (referenceCore.isBlank() || candidateCore.isBlank()) return 0 to "title_core_blank"
        val coverage = tokenCoverage(referenceCore, candidateCore)
        if (referenceCore != candidateCore && coverage < 0.60) return 0 to "title_mismatch"
        return when {
            referenceCore == candidateCore -> 28 to null
            coverage >= 0.85 -> 20 to null
            else -> (coverage * 15).toInt() to null
        }
    }

    private fun tokenCoverage(target: String, candidate: String): Double {
        val targetTokens = target.split(' ').filter { it.isNotBlank() }.toSet()
        if (targetTokens.isEmpty()) return 0.0
        val candidateTokens = candidate.split(' ').filter { it.isNotBlank() }.toSet()
        if (candidateTokens.isEmpty()) return 0.0
        return targetTokens.count { it in candidateTokens }.toDouble() / targetTokens.size.toDouble()
    }
}
