package com.luc4n3x.levyra.domain

import java.text.Normalizer
import java.util.Locale

// Precompiled: artist identity/segment helpers run per candidate in ranking and dedup loops.
private val ARTIST_COMBINING_MARKS = Regex("""\p{M}+""")
private val ARTIST_NON_ALPHANUMERIC = Regex("""[^\p{L}\p{N}]+""")
private val ARTIST_WHITESPACE = Regex("""\s+""")
private val ARTIST_EXPLICIT_SEPARATOR =
    Regex("""(?i)\s+(?:feat(?:uring)?\.?|ft\.?|with|con|vs\.?)\s+|,\s*|;\s*|\s+[x×/]\s+""")
private val ARTIST_JOINED_SEPARATOR = Regex("""(?i)\s+(?:&|and|e|y|et|und)\s+""")
private val ARTIST_YEAR_TOKEN = Regex("""\b(?:19|20)\d{2}\b""")

internal fun artistIdentityKey(value: String): String {
    return Normalizer.normalize(value, Normalizer.Form.NFKD)
        .replace(ARTIST_COMBINING_MARKS, "")
        .lowercase(Locale.ROOT)
        .replace("&", " and ")
        .replace(ARTIST_NON_ALPHANUMERIC, " ")
        .replace(ARTIST_WHITESPACE, " ")
        .trim()
}

internal fun primaryArtistSegment(value: String): String {
    val clean = value.trim()
    if (clean.isBlank()) return ""
    val explicit = ARTIST_EXPLICIT_SEPARATOR
        .split(clean)
        .firstOrNull()
        .orEmpty()
        .trim()
    if (explicit.isNotBlank() && !explicit.equals(clean, ignoreCase = true)) return explicit
    val joinedParts = ARTIST_JOINED_SEPARATOR.split(clean)
    return if (joinedParts.size == 2 && joinedParts.all { it.trim().split(ARTIST_WHITESPACE).size <= 4 }) {
        joinedParts.first().trim()
    } else {
        clean
    }
}

internal fun isArtistShelfNameEligible(value: String): Boolean {
    val primary = primaryArtistSegment(value).ifBlank { value.trim() }
    val key = artistIdentityKey(primary)
    if (key.length < 2) return false
    val blockedPhrases = listOf(
        "youtube music",
        "various artists",
        "artisti vari",
        "playlist",
        "official playlist",
        "music playlist",
        "musica italiana",
        "music italiana",
        "canzoni sanremo",
        "canzoni italiane",
        "hit canzoni",
        "top hits",
        "top 50",
        "top 100",
        "classifica",
        "compilation",
        "karaoke",
        "subscribe",
        "radio station"
    )
    if (blockedPhrases.any(key::contains)) return false
    if (key.startsWith("topsify ")) return false
    if (key.endsWith(" mix") || key.endsWith(" playlist") || key.endsWith(" chart") || key.endsWith(" charts")) return false
    if (ARTIST_YEAR_TOKEN.containsMatchIn(key) && listOf("hit", "canzoni", "mix", "top", "classifica").any(key::contains)) return false
    return true
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
