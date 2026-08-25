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
private val ARTIST_AUDIENCE_PATTERN =
    Regex("""(\d[\d.,\s\u00A0]*)\s*(k|m|b|mln|mld|mrd|mio|mila|tys)?""", RegexOption.IGNORE_CASE)
private val ARTIST_LEADING_ARTICLES = setOf(
    "the", "a", "an", "el", "la", "las", "los", "le", "les", "il", "lo", "gli", "der", "die", "das", "os", "as"
)

internal fun artistIdentityKey(value: String): String {
    return Normalizer.normalize(value, Normalizer.Form.NFKD)
        .replace(ARTIST_COMBINING_MARKS, "")
        .lowercase(Locale.ROOT)
        .replace("&", " and ")
        .replace(ARTIST_NON_ALPHANUMERIC, " ")
        .replace(ARTIST_WHITESPACE, " ")
        .trim()
}

/**
 * Identity keys a name can be matched by: the plain key plus, when the name starts with a common
 * leading article, the key without it. Lets "weeknd" match "The Weeknd" without dropping articles
 * from anything shown to the user.
 */
internal fun artistIdentityKeys(value: String): Set<String> {
    val key = artistIdentityKey(value)
    if (key.isBlank()) return emptySet()
    val article = key.substringBefore(' ')
    val remainder = key.substringAfter(' ', "")
    if (remainder.isBlank() || article !in ARTIST_LEADING_ARTICLES) return setOf(key)
    return setOf(key, remainder)
}

internal fun artistIdentityMatches(first: String, second: String): Boolean {
    val firstKeys = artistIdentityKeys(first)
    if (firstKeys.isEmpty()) return false
    return artistIdentityKeys(second).any(firstKeys::contains)
}

/**
 * Rough audience size parsed from a search subtitle ("274M monthly audience", "7 subscribers").
 * Ranks equally matching artists only; it never filters a candidate out.
 */
internal fun artistAudienceWeight(value: String): Long {
    val match = ARTIST_AUDIENCE_PATTERN.find(value) ?: return 0L
    val multiplier = when (match.groupValues[2].lowercase(Locale.ROOT)) {
        "k", "mila", "tys" -> 1_000.0
        "m", "mln", "mio" -> 1_000_000.0
        "b", "mrd", "mld" -> 1_000_000_000.0
        else -> 1.0
    }
    val digits = match.groupValues[1].filter { it.isDigit() || it == '.' || it == ',' }
    val amount = if (multiplier > 1.0) {
        digits.replace(',', '.').toDoubleOrNull()
    } else {
        digits.filter(Char::isDigit).toDoubleOrNull()
    } ?: return 0L
    return (amount * multiplier).toLong()
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
