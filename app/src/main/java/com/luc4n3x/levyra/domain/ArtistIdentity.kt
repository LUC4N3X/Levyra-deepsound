package com.luc4n3x.levyra.domain

import java.text.Normalizer
import java.util.Locale

internal fun artistIdentityKey(value: String): String {
    return Normalizer.normalize(value, Normalizer.Form.NFKD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
        .replace("&", " and ")
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

internal fun primaryArtistSegment(value: String): String {
    val clean = value.trim()
    if (clean.isBlank()) return ""
    val explicit = Regex("(?i)\\s+(?:feat(?:uring)?\\.?|ft\\.?|with|con|vs\\.?)\\s+|,\\s*|;\\s*|\\s+[x×/]\\s+")
        .split(clean)
        .firstOrNull()
        .orEmpty()
        .trim()
    if (explicit.isNotBlank() && !explicit.equals(clean, ignoreCase = true)) return explicit
    val joinedParts = Regex("(?i)\\s+(?:&|and|e|y|et|und)\\s+").split(clean)
    return if (joinedParts.size == 2 && joinedParts.all { it.trim().split(Regex("\\s+")).size <= 4 }) {
        joinedParts.first().trim()
    } else {
        clean
    }
}

internal fun artistSearchMatchScore(query: String, candidate: String): Int {
    val queryKey = artistIdentityKey(query)
    val candidateKey = artistIdentityKey(candidate)
    if (queryKey.isBlank() || candidateKey.isBlank()) return Int.MIN_VALUE
    if (queryKey == candidateKey) return 10_000

    val primaryKey = artistIdentityKey(primaryArtistSegment(query))
    var score = 0
    if (primaryKey.isNotBlank() && primaryKey == candidateKey) score += 900
    if (queryKey.startsWith("$candidateKey ")) score += 760
    if (candidateKey.startsWith("$queryKey ")) score += 620
    if (queryKey.contains(candidateKey)) score += 460
    if (candidateKey.contains(queryKey)) score += 380

    val queryTokens = queryKey.split(' ').filter { it.length >= 2 }.toSet()
    val candidateTokens = candidateKey.split(' ').filter { it.length >= 2 }.toSet()
    if (queryTokens.isNotEmpty() && candidateTokens.isNotEmpty()) {
        val shared = queryTokens.intersect(candidateTokens).size
        score += shared * 80
        if (shared == candidateTokens.size) score += 120
        if (shared == queryTokens.size) score += 100
    }
    score -= kotlin.math.abs(queryKey.length - candidateKey.length).coerceAtMost(80)
    return score
}
