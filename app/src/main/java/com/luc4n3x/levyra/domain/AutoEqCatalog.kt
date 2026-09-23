package com.luc4n3x.levyra.domain

import java.net.URLDecoder
import java.text.Normalizer

data class AutoEqCatalogEntry(
    val name: String,
    val source: String,
    val variant: String,
    val graphicEqPath: String,
    val parametricEqPath: String
) {
    val key: String get() = graphicEqPath
}

class AutoEqCatalog private constructor(
    private val names: Array<String>,
    private val groups: ShortArray,
    private val groupPaths: Array<String>,
    private val groupSources: Array<String>,
    private val groupVariants: Array<String>,
    private val searchKeys: String,
    private val keyStarts: IntArray
) {
    val size: Int get() = names.size

    fun search(query: String, limit: Int = DEFAULT_RESULT_LIMIT): List<AutoEqCatalogEntry> {
        val tokens = query.take(MAX_QUERY_CHARS)
            .split(' ', '\t', '-', '/', '_', '.', ',')
            .map(::foldSearchText)
            .filter { it.isNotEmpty() }
            .distinct()
            .sortedByDescending { it.length }
        if (tokens.isEmpty() || limit <= 0) return emptyList()
        val joinedQuery = foldSearchText(query.take(MAX_QUERY_CHARS))
        val anchor = tokens.first()
        val matches = ArrayList<Match>()
        var position = searchKeys.indexOf(anchor)
        while (position >= 0) {
            val entry = entryAt(position)
            if (tokens.all { containsWithinKey(entry, it) }) {
                matches += Match(
                    index = entry,
                    rank = rank(entry, joinedQuery, tokens),
                    sourcePriority = sourcePriority(groupSources[groups[entry].toInt()]),
                    keyLength = keyEnd(entry) - keyStarts[entry]
                )
            }
            position = searchKeys.indexOf(anchor, keyEnd(entry) + 1)
        }
        return matches
            .sortedWith(MATCH_ORDER.thenBy(String.CASE_INSENSITIVE_ORDER) { names[it.index] })
            .take(limit)
            .map { entry(it.index) }
    }

    private class Match(val index: Int, val rank: Int, val sourcePriority: Int, val keyLength: Int)

    private fun entry(index: Int): AutoEqCatalogEntry {
        val group = groups[index].toInt()
        val encodedName = encodePathSegment(names[index])
        val encodedFile = encodePathSegment("${names[index]}$GRAPHIC_EQ_SUFFIX")
        val encodedParametricFile = encodePathSegment("${names[index]}$PARAMETRIC_EQ_SUFFIX")
        return AutoEqCatalogEntry(
            name = names[index],
            source = groupSources[group],
            variant = groupVariants[group],
            graphicEqPath = "$RESULTS_ROOT/${groupPaths[group]}/$encodedName/$encodedFile",
            parametricEqPath = "$RESULTS_ROOT/${groupPaths[group]}/$encodedName/$encodedParametricFile"
        )
    }

    private fun rank(index: Int, joinedQuery: String, tokens: List<String>): Int {
        val start = keyStarts[index]
        val length = keyEnd(index) - start
        return when {
            length == joinedQuery.length && searchKeys.regionMatches(start, joinedQuery, 0, length) -> 0
            searchKeys.regionMatches(start, joinedQuery, 0, joinedQuery.length) -> 1
            tokens.any { searchKeys.regionMatches(start, it, 0, it.length) } -> 2
            else -> 3
        }
    }

    private fun containsWithinKey(index: Int, token: String): Boolean {
        val start = keyStarts[index]
        val lastStart = keyEnd(index) - token.length
        for (offset in start..lastStart) {
            if (searchKeys.regionMatches(offset, token, 0, token.length)) return true
        }
        return false
    }

    private fun keyEnd(index: Int): Int =
        if (index + 1 < keyStarts.size) keyStarts[index + 1] - 1 else searchKeys.length

    private fun entryAt(position: Int): Int {
        var low = 0
        var high = keyStarts.size - 1
        while (low < high) {
            val middle = (low + high + 1) / 2
            if (keyStarts[middle] <= position) low = middle else high = middle - 1
        }
        return low
    }

    companion object {
        const val DEFAULT_RESULT_LIMIT = 60
        const val MAX_ENTRIES = 40_000
        const val MAX_QUERY_CHARS = 80
        private const val MAX_NAME_CHARS = 160
        private const val MAX_GROUPS = Short.MAX_VALUE.toInt()
        private const val ENTRY_PREFIX = "- ["
        private const val LINK_OPEN = "](./"
        private const val LINK_CLOSE = ") by "
        private const val RIG_SEPARATOR = " on "
        private const val RESULTS_ROOT = "results"
        private const val GRAPHIC_EQ_SUFFIX = " GraphicEQ.txt"
        private const val PARAMETRIC_EQ_SUFFIX = " ParametricEQ.txt"
        private const val KEY_SEPARATOR = '|'
        private const val UNRESERVED = "-._~"
        private const val ALLOWED_ENCODED_PATH = "!$%&'()+,-.0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        private val PREFERRED_SOURCES = listOf("oratory1990", "crinacle", "Rtings", "Innerfidelity", "Headphone.com Legacy")
        private val HEX = "0123456789ABCDEF".toCharArray()
        private val MATCH_ORDER = compareBy<Match>({ it.rank }, { it.sourcePriority }, { it.keyLength })
        private val COMBINING_MARKS = Regex("\\p{Mn}+")

        fun parseIndex(markdown: String): AutoEqCatalog {
            val names = ArrayList<String>()
            val groups = ArrayList<Short>()
            val groupIds = HashMap<String, Int>()
            val groupPaths = ArrayList<String>()
            val groupSources = ArrayList<String>()
            val groupVariants = ArrayList<String>()
            val keys = StringBuilder()
            val keyStarts = ArrayList<Int>()
            val seen = HashSet<String>()
            for (line in markdown.lineSequence()) {
                if (names.size >= MAX_ENTRIES) break
                val parsed = parseLine(line.trim()) ?: continue
                val groupPath = parsed.groupPath
                if (!seen.add("$groupPath/${parsed.name}")) continue
                val group = groupIds[groupPath] ?: run {
                    if (groupPaths.size >= MAX_GROUPS) return@run null
                    groupPaths += groupPath
                    groupSources += parsed.source
                    groupVariants += parsed.variant
                    (groupPaths.size - 1).also { groupIds[groupPath] = it }
                } ?: continue
                val key = foldSearchText(parsed.name)
                if (key.isEmpty()) continue
                if (keyStarts.isNotEmpty()) keys.append(KEY_SEPARATOR)
                keyStarts += keys.length
                keys.append(key)
                names += parsed.name
                groups += group.toShort()
            }
            return AutoEqCatalog(
                names = names.toTypedArray(),
                groups = groups.toShortArray(),
                groupPaths = groupPaths.toTypedArray(),
                groupSources = groupSources.toTypedArray(),
                groupVariants = groupVariants.toTypedArray(),
                searchKeys = keys.toString(),
                keyStarts = keyStarts.toIntArray()
            )
        }

        internal fun foldSearchText(value: String): String {
            val decomposed = Normalizer.normalize(value, Normalizer.Form.NFD).replace(COMBINING_MARKS, "")
            val folded = StringBuilder(decomposed.length)
            for (char in decomposed) {
                if (char.isLetterOrDigit()) folded.append(char.lowercaseChar())
            }
            return folded.toString()
        }

        internal fun encodePathSegment(value: String): String {
            val encoded = StringBuilder(value.length + 16)
            for (byte in value.toByteArray(Charsets.UTF_8)) {
                val unsigned = byte.toInt() and 0xFF
                val char = unsigned.toChar()
                if (unsigned < 0x80 && (char.isLetterOrDigit() || char in UNRESERVED)) {
                    encoded.append(char)
                } else {
                    encoded.append('%').append(HEX[unsigned ushr 4]).append(HEX[unsigned and 0x0F])
                }
            }
            return encoded.toString()
        }

        private fun sourcePriority(source: String): Int =
            PREFERRED_SOURCES.indexOf(source).takeIf { it >= 0 } ?: PREFERRED_SOURCES.size

        private fun parseLine(line: String): ParsedLine? {
            if (!line.startsWith(ENTRY_PREFIX)) return null
            val linkOpen = line.indexOf(LINK_OPEN, ENTRY_PREFIX.length)
            if (linkOpen <= ENTRY_PREFIX.length) return null
            val linkClose = line.indexOf(LINK_CLOSE, linkOpen + LINK_OPEN.length)
            if (linkClose < 0) return null
            val name = line.substring(ENTRY_PREFIX.length, linkOpen).trim()
            if (name.isEmpty() || name.length > MAX_NAME_CHARS || name.any { it.isISOControl() }) return null
            val path = line.substring(linkOpen + LINK_OPEN.length, linkClose)
            if (path.any { it !in ALLOWED_ENCODED_PATH && it != '/' }) return null
            val segments = path.split('/')
            if (segments.size != 3 || segments.any { it.isEmpty() || it == "." || it == ".." }) return null
            val decodedSegments = segments.map { decodeSegment(it) ?: return null }
            if (decodedSegments.any { it.contains('/') || it.contains('\\') || it.any(Char::isISOControl) }) return null
            if (decodedSegments[2] != name) return null
            val attribution = line.substring(linkClose + LINK_CLOSE.length)
            val source = attribution.substringBefore(RIG_SEPARATOR).trim()
            if (source != decodedSegments[0]) return null
            return ParsedLine(
                name = name,
                source = source,
                variant = decodedSegments[1],
                groupPath = "${segments[0]}/${segments[1]}"
            )
        }

        private fun decodeSegment(segment: String): String? =
            runCatching { URLDecoder.decode(segment.replace("+", "%2B"), Charsets.UTF_8.name()) }.getOrNull()

        private class ParsedLine(val name: String, val source: String, val variant: String, val groupPath: String)
    }
}
