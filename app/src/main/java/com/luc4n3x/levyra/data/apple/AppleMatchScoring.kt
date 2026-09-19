package com.luc4n3x.levyra.data.apple

internal object AppleMatchScoring {
    fun tokenCoverage(target: String, candidate: String): Double {
        val targetTokens = target.split(' ').filter { it.isNotBlank() }.toSet()
        if (targetTokens.isEmpty()) return 0.0
        val candidateTokens = candidate.split(' ').filter { it.isNotBlank() }.toSet()
        if (candidateTokens.isEmpty()) return 0.0
        return targetTokens.count { it in candidateTokens }.toDouble() / targetTokens.size.toDouble()
    }
}
