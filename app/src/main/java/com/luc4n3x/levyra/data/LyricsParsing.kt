package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricVocalRole
import com.luc4n3x.levyra.domain.LyricWord
import java.io.StringReader
import java.text.Normalizer
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.absoluteValue
import kotlin.math.max
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource

data class LyricsRequest(
    val title: String,
    val artist: String,
    val durationSec: Long
)

data class LyricsCandidate(
    val result: LyricsRepository.LyricsResult,
    val title: String,
    val artist: String,
    val durationSec: Long
)

enum class LyricsFormat {
    TTML,
    YRC,
    QRC,
    KRC,
    LRC,
    PLAIN
}

object LyricsProviderSelector {
    fun select(
        native: LyricsCandidate?,
        lrcLib: List<LyricsCandidate>,
        request: LyricsRequest
    ): LyricsRepository.LyricsResult? {
        native?.result?.takeIf { it.synced && it.lines.isNotEmpty() }?.let { return it }
        LyricsResultRanker.best(lrcLib.filter { it.result.synced }, request)?.let { return it }
        native?.result?.takeIf { !it.synced && it.lines.isNotEmpty() }?.let { return it }
        return LyricsResultRanker.best(lrcLib.filterNot { it.result.synced }, request)
    }
}

object UnifiedLyricsParser {
    fun detect(text: String): LyricsFormat {
        val source = text.trimStart()
        return when {
            source.startsWith("<tt", ignoreCase = true) || source.contains("<tt ", ignoreCase = true) -> LyricsFormat.TTML
            Regex("(?m)^\\[\\d+,\\d+]\\(\\d+,\\d+,\\d+\\)").containsMatchIn(source) -> LyricsFormat.YRC
            source.contains("[language:", ignoreCase = true) && Regex("(?m)^\\[\\d+,\\d+]<(?:\\d+,){2}\\d+>").containsMatchIn(source) -> LyricsFormat.KRC
            Regex("(?m)^\\[\\d+,\\d+]<(?:\\d+,){2}\\d+>").containsMatchIn(source) -> LyricsFormat.QRC
            Regex("(?m)^\\[\\d{1,3}:\\d{2}(?:[.:]\\d{1,3})?]").containsMatchIn(source) -> LyricsFormat.LRC
            else -> LyricsFormat.PLAIN
        }
    }

    fun parse(text: String, format: LyricsFormat = detect(text)): List<LyricLine> {
        val parsed = when (format) {
            LyricsFormat.TTML -> TtmlLyricsParser.parse(text)
            LyricsFormat.YRC -> YrcLyricsParser.parse(text)
            LyricsFormat.QRC -> QrcLyricsParser.parse(text)
            LyricsFormat.KRC -> KrcLyricsParser.parse(text)
            LyricsFormat.LRC -> LrcLyricsParser.parse(text)
            LyricsFormat.PLAIN -> parsePlain(text)
        }
        return LyricsCleaner.clean(parsed)
    }

    fun parsePlain(text: String): List<LyricLine> {
        return text.lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .mapIndexed { index, line ->
                LyricLine(
                    startMs = index * 4_200L,
                    endMs = (index + 1) * 4_200L,
                    text = line,
                    translated = "",
                    role = LyricRoleClassifier.roleOf(line)
                )
            }
            .toList()
    }
}

object LrcLyricsParser {
    private val timestampRegex = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")
    private val enhancedTimestampRegex = Regex("<(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?>")
    private val offsetRegex = Regex("(?im)^\\[offset:([+-]?\\d+)]\\s*$")

    fun parse(lrc: String): List<LyricLine> {
        val offsetMs = offsetRegex.find(lrc)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
        val raw = mutableListOf<RawLrcLine>()
        lrc.lineSequence().forEach { sourceLine ->
            val matches = timestampRegex.findAll(sourceLine).toList()
            if (matches.isEmpty()) return@forEach
            val content = sourceLine.substring(matches.last().range.last + 1).trim()
            if (content.isBlank()) return@forEach
            matches.forEach { match ->
                val start = (timestampMs(match.groupValues) + offsetMs).coerceAtLeast(0L)
                raw += RawLrcLine(start, content)
            }
        }

        val sorted = raw.sortedWith(compareBy<RawLrcLine> { it.startMs }.thenBy { it.content })
        return sorted.mapIndexedNotNull { index, item ->
            val nextStart = sorted.getOrNull(index + 1)?.startMs
            val fallbackEnd = nextStart?.minus(80L)?.coerceAtLeast(item.startMs + 350L) ?: (item.startMs + 6_000L)
            val words = parseEnhancedWords(item.content, item.startMs, fallbackEnd, offsetMs)
            val visibleText = if (words.isNotEmpty()) {
                words.joinToString("") { it.text }.replace(Regex("\\s+"), " ").trim()
            } else {
                enhancedTimestampRegex.replace(item.content, "").trim()
            }
            if (visibleText.isBlank()) {
                null
            } else {
                LyricLine(
                    startMs = item.startMs,
                    endMs = max(fallbackEnd, words.lastOrNull()?.endMs ?: fallbackEnd),
                    text = visibleText,
                    translated = "",
                    words = words,
                    role = LyricRoleClassifier.roleOf(visibleText)
                )
            }
        }
    }

    private fun parseEnhancedWords(content: String, lineStartMs: Long, lineEndMs: Long, offsetMs: Long): List<LyricWord> {
        val matches = enhancedTimestampRegex.findAll(content).toList()
        if (matches.isEmpty()) return emptyList()
        val words = ArrayList<LyricWord>(matches.size)
        matches.forEachIndexed { index, match ->
            val start = (timestampMs(match.groupValues) + offsetMs).coerceAtLeast(lineStartMs)
            val nextMarkerStart = matches.getOrNull(index + 1)?.let { timestampMs(it.groupValues) + offsetMs }
            val rawTextStart = match.range.last + 1
            val rawTextEnd = matches.getOrNull(index + 1)?.range?.first ?: content.length
            val wordText = content.substring(rawTextStart, rawTextEnd)
            if (wordText.isBlank()) return@forEachIndexed
            val end = nextMarkerStart?.minus(20L)?.coerceAtLeast(start + 40L)
                ?: lineEndMs.coerceAtLeast(start + 80L)
            words += LyricWord(startMs = start, endMs = end, text = wordText)
        }
        return words
    }

    private fun timestampMs(groups: List<String>): Long {
        val min = groups[1].toLongOrNull() ?: 0L
        val sec = groups[2].toLongOrNull() ?: 0L
        val frac = groups.getOrElse(3) { "" }
        val ms = when (frac.length) {
            1 -> frac.toLongOrNull()?.times(100L) ?: 0L
            2 -> frac.toLongOrNull()?.times(10L) ?: 0L
            3 -> frac.toLongOrNull() ?: 0L
            else -> 0L
        }
        return min * 60_000L + sec * 1_000L + ms
    }

    private data class RawLrcLine(val startMs: Long, val content: String)
}

object YrcLyricsParser {
    private val lineRegex = Regex("^\\[(\\d+),(\\d+)](.*)$")
    private val wordRegex = Regex("\\((\\d+),(\\d+),\\d+\\)([^()]*)")

    fun parse(text: String): List<LyricLine> {
        return parseSyllableLines(text, lineRegex, wordRegex, absoluteWordTiming = true)
    }
}

object QrcLyricsParser {
    private val lineRegex = Regex("^\\[(\\d+),(\\d+)](.*)$")
    private val wordRegex = Regex("<(\\d+),(\\d+),\\d+>([^<]*)")

    fun parse(text: String): List<LyricLine> {
        return parseSyllableLines(text, lineRegex, wordRegex, absoluteWordTiming = true)
    }
}

object KrcLyricsParser {
    private val lineRegex = Regex("^\\[(\\d+),(\\d+)](.*)$")
    private val wordRegex = Regex("<(\\d+),(\\d+),\\d+>([^<]*)")

    fun parse(text: String): List<LyricLine> {
        return parseSyllableLines(text, lineRegex, wordRegex, absoluteWordTiming = false)
    }
}

private fun parseSyllableLines(
    text: String,
    lineRegex: Regex,
    wordRegex: Regex,
    absoluteWordTiming: Boolean
): List<LyricLine> {
    val lines = ArrayList<LyricLine>()
    text.lineSequence().forEach { source ->
        val match = lineRegex.matchEntire(source.trim()) ?: return@forEach
        val lineStart = match.groupValues[1].toLongOrNull() ?: return@forEach
        val duration = match.groupValues[2].toLongOrNull()?.coerceAtLeast(1L) ?: return@forEach
        val payload = match.groupValues[3]
        val words = wordRegex.findAll(payload).mapNotNull { wordMatch ->
            val timing = wordMatch.groupValues[1].toLongOrNull() ?: return@mapNotNull null
            val wordDuration = wordMatch.groupValues[2].toLongOrNull()?.coerceAtLeast(1L) ?: return@mapNotNull null
            val wordText = wordMatch.groupValues[3]
            if (wordText.isBlank()) return@mapNotNull null
            val start = if (absoluteWordTiming) timing else lineStart + timing
            LyricWord(startMs = start, endMs = start + wordDuration, text = wordText)
        }.toList()
        val visibleText = if (words.isNotEmpty()) {
            words.joinToString("") { it.text }.replace(Regex("\\s+"), " ").trim()
        } else {
            wordRegex.replace(payload, "\$3").trim()
        }
        if (visibleText.isBlank()) return@forEach
        lines += LyricLine(
            startMs = lineStart,
            endMs = lineStart + duration,
            text = visibleText,
            translated = "",
            words = words,
            role = LyricRoleClassifier.roleOf(visibleText)
        )
    }
    return lines.sortedBy { it.startMs }
}

object TtmlLyricsParser {
    fun parse(ttml: String): List<LyricLine> {
        val document = runCatching {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = false
                runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
                runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
                runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
                runCatching { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) }
                isXIncludeAware = false
                isExpandEntityReferences = false
            }
            factory.newDocumentBuilder().parse(InputSource(StringReader(ttml)))
        }.getOrNull() ?: return emptyList()

        val nodes = document.getElementsByTagName("p")
        val agents = buildSet {
            for (index in 0 until nodes.length) {
                val element = nodes.item(index) as? Element ?: continue
                element.getAttribute("ttm:agent")
                    .ifBlank { element.getAttribute("agent") }
                    .trim()
                    .takeIf { it.isNotBlank() }
                    ?.let(::add)
            }
        }
        val multipleAgents = agents.size > 1
        val lines = ArrayList<LyricLine>()
        for (index in 0 until nodes.length) {
            val element = nodes.item(index) as? Element ?: continue
            val start = element.timeAttribute("begin") ?: continue
            val end = element.timeAttribute("end")
                ?: element.timeAttribute("dur")?.let { start + it }
                ?: (start + 6_000L)
            val translation = element.specialSpanText("x-translation")
            val romanized = element.specialSpanText("x-roman")
            val role = element.lyricRole(multipleAgents)
            val words = element.wordSpans()
            val text = if (words.isNotEmpty()) {
                words.joinToString("") { it.text }.replace(Regex("\\s+"), " ").trim()
            } else {
                element.baseText().replace(Regex("\\s+"), " ").trim()
            }
            if (text.isNotBlank()) {
                val lineEnd = max(end, words.lastOrNull()?.endMs ?: end).coerceAtLeast(start + 120L)
                lines += LyricLine(
                    startMs = start,
                    endMs = lineEnd,
                    text = text,
                    translated = translation,
                    words = words,
                    romanized = romanized,
                    role = role
                )
            }
            lines += element.backgroundLines(start, end)
        }
        return lines.sortedWith(compareBy<LyricLine> { it.startMs }.thenBy { it.role.ordinal })
    }

    private fun Element.wordSpans(): List<LyricWord> {
        val out = ArrayList<LyricWord>()
        val spans = getElementsByTagName("span")
        for (index in 0 until spans.length) {
            val span = spans.item(index) as? Element ?: continue
            val role = span.roleAttribute()
            if (role in SPECIAL_ROLES || span.hasAncestorWithSpecialRole(this)) continue
            val start = span.timeAttribute("begin") ?: continue
            val end = span.timeAttribute("end")
                ?: span.timeAttribute("dur")?.let { start + it }
                ?: continue
            val text = span.directText()
            if (text.isNotBlank()) {
                out += LyricWord(startMs = start, endMs = end.coerceAtLeast(start + 20L), text = text)
            }
        }
        return out
    }

    private fun Element.backgroundLines(parentStart: Long, parentEnd: Long): List<LyricLine> {
        val out = ArrayList<LyricLine>()
        val spans = getElementsByTagName("span")
        for (index in 0 until spans.length) {
            val span = spans.item(index) as? Element ?: continue
            if (span.roleAttribute() != "x-bg") continue
            val start = span.timeAttribute("begin") ?: parentStart
            val end = span.timeAttribute("end")
                ?: span.timeAttribute("dur")?.let { start + it }
                ?: parentEnd
            val words = span.wordSpans()
            val text = if (words.isNotEmpty()) {
                words.joinToString("") { it.text }.replace(Regex("\\s+"), " ").trim()
            } else {
                span.baseText().replace(Regex("\\s+"), " ").trim()
            }
            if (text.isBlank()) continue
            out += LyricLine(
                startMs = start,
                endMs = end.coerceAtLeast(start + 120L),
                text = text,
                translated = span.specialSpanText("x-translation"),
                words = words,
                romanized = span.specialSpanText("x-roman"),
                role = LyricVocalRole.BACKGROUND
            )
        }
        return out
    }

    private fun Element.specialSpanText(expectedRole: String): String {
        val spans = getElementsByTagName("span")
        val values = ArrayList<String>()
        for (index in 0 until spans.length) {
            val span = spans.item(index) as? Element ?: continue
            if (span.roleAttribute() != expectedRole || span.hasAncestorWithSpecialRole(this)) continue
            val value = span.textContent.orEmpty().replace(Regex("\\s+"), " ").trim()
            if (value.isNotBlank()) values += value
        }
        return values.distinct().joinToString(" ")
    }

    private fun Element.lyricRole(multipleAgents: Boolean): LyricVocalRole {
        val role = roleAttribute()
        if (role == "x-bg") return LyricVocalRole.BACKGROUND
        val agent = getAttribute("ttm:agent").ifBlank { getAttribute("agent") }.lowercase(Locale.ROOT)
        val explicit = getAttribute("data-duet").ifBlank { getAttribute("duet") }.lowercase(Locale.ROOT)
        return when {
            explicit == "right" || agent in setOf("v2", "voice2", "agent2", "singer2") -> LyricVocalRole.DUET_RIGHT
            explicit == "left" || multipleAgents && agent in setOf("v1", "voice1", "agent1", "singer1") -> LyricVocalRole.DUET_LEFT
            else -> LyricVocalRole.MAIN
        }
    }

    private fun Element.roleAttribute(): String = getAttribute("ttm:role")
        .ifBlank { getAttribute("role") }
        .lowercase(Locale.ROOT)

    private fun Element.hasAncestorWithSpecialRole(root: Element): Boolean {
        var current = parentNode
        while (current is Element && current !== root) {
            if (current.roleAttribute() in SPECIAL_ROLES) return true
            current = current.parentNode
        }
        return false
    }

    private fun Element.baseText(): String = buildString {
        fun appendNode(node: Node) {
            when (node.nodeType) {
                Node.TEXT_NODE, Node.CDATA_SECTION_NODE -> append(node.nodeValue.orEmpty())
                Node.ELEMENT_NODE -> {
                    val element = node as Element
                    if (element.roleAttribute() in SPECIAL_ROLES) return
                    val children = element.childNodes
                    for (childIndex in 0 until children.length) appendNode(children.item(childIndex))
                }
            }
        }
        val children = childNodes
        for (index in 0 until children.length) appendNode(children.item(index))
    }

    private fun Element.directText(): String = buildString {
        val children = childNodes
        for (index in 0 until children.length) {
            val child = children.item(index)
            if (child.nodeType == Node.TEXT_NODE || child.nodeType == Node.CDATA_SECTION_NODE) append(child.nodeValue.orEmpty())
        }
    }

    private fun Element.timeAttribute(name: String): Long? = getAttribute(name).takeIf(String::isNotBlank)?.let(::parseTimeMs)

    private fun parseTimeMs(raw: String): Long? {
        val value = raw.trim()
        if (value.endsWith("ms")) return value.removeSuffix("ms").toDoubleOrNull()?.toLong()
        if (value.endsWith("s")) return value.removeSuffix("s").toDoubleOrNull()?.times(1_000L)?.toLong()
        if (value.endsWith("m")) return value.removeSuffix("m").toDoubleOrNull()?.times(60_000L)?.toLong()
        val parts = value.split(":")
        return when (parts.size) {
            2 -> {
                val minutes = parts[0].toLongOrNull() ?: return null
                val seconds = parts[1].toDoubleOrNull() ?: return null
                minutes * 60_000L + (seconds * 1_000L).toLong()
            }
            3 -> {
                val hours = parts[0].toLongOrNull() ?: return null
                val minutes = parts[1].toLongOrNull() ?: return null
                val seconds = parts[2].toDoubleOrNull() ?: return null
                hours * 3_600_000L + minutes * 60_000L + (seconds * 1_000L).toLong()
            }
            else -> value.toDoubleOrNull()?.times(1_000L)?.toLong()
        }
    }

    private val SPECIAL_ROLES = setOf("x-translation", "x-roman", "x-bg")
}

object LyricsCleaner {
    private val metadataRegex = Regex(
        "(?i)^(lyrics?|written|composed|produced|arranged|performed|music|words|paroles|testo|traduzione|作词|作曲|編曲|编曲|詞|曲|가사|작사|작곡)\\s*(by)?\\s*[:：-]"
    )
    private val noiseRegex = Regex("(?i)^(embed|you might also like|contributors?\\s*\\d*|translations?|romanized)$")
    private val instrumentalRegex = Regex("(?i)^\\[?(instrumental|music|interlude|intro|outro|break)\\]?$|^♪+$")

    fun clean(lines: List<LyricLine>): List<LyricLine> {
        val prepared = lines.asSequence()
            .map { line ->
                val normalizedText = normalizeText(line.text)
                val role = if (line.role == LyricVocalRole.MAIN) LyricRoleClassifier.roleOf(normalizedText) else line.role
                val text = LyricRoleClassifier.stripPrefix(normalizedText)
                val translated = normalizeText(line.translated)
                val romanized = normalizeText(line.romanized)
                val metadata = line.isMetadata || metadataRegex.containsMatchIn(text) || noiseRegex.matches(text)
                val instrumental = line.isInstrumental || instrumentalRegex.matches(text)
                line.copy(
                    text = if (instrumental && text.isBlank()) "♪" else text,
                    translated = translated,
                    romanized = romanized,
                    words = cleanTimedWords(line.words),
                    role = role,
                    isInstrumental = instrumental,
                    isMetadata = metadata
                )
            }
            .filter { it.text.isNotBlank() }
            .filterNot { it.isMetadata }
            .sortedWith(compareBy<LyricLine> { it.startMs }.thenBy { it.role.ordinal })
            .toList()

        if (prepared.isEmpty()) return emptyList()
        val deduplicated = ArrayList<LyricLine>(prepared.size)
        prepared.forEach { line ->
            val previous = deduplicated.lastOrNull()
            if (previous != null && previous.startMs == line.startMs && previous.text.equals(line.text, ignoreCase = true) && previous.role == line.role) {
                deduplicated[deduplicated.lastIndex] = mergeDuplicate(previous, line)
            } else {
                deduplicated += line
            }
        }
        return deduplicated.mapIndexed { index, line ->
            val nextMainStart = deduplicated.drop(index + 1)
                .firstOrNull { it.role != LyricVocalRole.BACKGROUND }
                ?.startMs
            val end = when {
                line.endMs > line.startMs -> line.endMs
                nextMainStart != null -> nextMainStart - 60L
                else -> line.startMs + 6_000L
            }.coerceAtLeast(line.startMs + 120L)
            line.copy(endMs = end)
        }
    }

    private fun cleanTimedWords(words: List<LyricWord>): List<LyricWord> {
        val normalized = words.mapNotNull { word ->
            val value = word.text.replace(Regex("\\s+"), " ")
            if (value.isBlank()) null else word.copy(text = value)
        }
        if (normalized.isEmpty()) return emptyList()

        val rendered = StringBuilder()
        val ranges = ArrayList<WordTextRange>(normalized.size)
        var previousEndedWithWhitespace = true
        normalized.forEachIndexed { index, word ->
            val value = word.text.trim()
            if (index > 0 && !previousEndedWithWhitespace && !value.first().isPunctuationWithoutLeadingSpace()) {
                rendered.append(' ')
            }
            val start = rendered.length
            rendered.append(value)
            ranges += WordTextRange(start, rendered.length)
            previousEndedWithWhitespace = word.text.lastOrNull()?.isWhitespace() == true
        }

        val prefixLength = LyricRoleClassifier.prefixLength(rendered.toString())
        if (prefixLength <= 0) return normalized

        return normalized.mapIndexedNotNull { index, word ->
            val range = ranges[index]
            val removed = (prefixLength - range.start).coerceIn(0, range.endExclusive - range.start)
            val value = word.text.trim().drop(removed).trimStart()
            if (value.isBlank()) null else word.copy(text = value)
        }
    }

    private fun mergeDuplicate(previous: LyricLine, current: LyricLine): LyricLine {
        return previous.copy(
            endMs = max(previous.endMs, current.endMs),
            translated = richerText(previous.translated, current.translated),
            words = mergeWords(previous.words, current.words),
            romanized = richerText(previous.romanized, current.romanized),
            isInstrumental = previous.isInstrumental || current.isInstrumental,
            isMetadata = previous.isMetadata || current.isMetadata
        )
    }

    private fun mergeWords(previous: List<LyricWord>, current: List<LyricWord>): List<LyricWord> {
        if (previous.isEmpty()) return current
        if (current.isEmpty()) return previous
        val previousScore = wordListScore(previous)
        val currentScore = wordListScore(current)
        val primary = if (currentScore > previousScore) current else previous
        val secondary = if (primary === current) previous else current
        return primary.mapIndexed { index, word ->
            val indexed = secondary.getOrNull(index)
            val counterpart = indexed?.takeIf {
                it.text.equals(word.text, ignoreCase = true) || (it.startMs - word.startMs).absoluteValue <= 350L
            } ?: secondary.minByOrNull { candidate ->
                val textPenalty = if (candidate.text.equals(word.text, ignoreCase = true)) 0L else 2_000L
                (candidate.startMs - word.startMs).absoluteValue + textPenalty
            }?.takeIf { candidate ->
                candidate.text.equals(word.text, ignoreCase = true) || (candidate.startMs - word.startMs).absoluteValue <= 350L
            }
            word.copy(romanized = richerText(word.romanized, counterpart?.romanized.orEmpty()))
        }
    }

    private fun wordListScore(words: List<LyricWord>): Int {
        val validTimings = words.count { it.endMs > it.startMs }
        val romanized = words.count { it.romanized.isNotBlank() }
        return words.size * 100 + validTimings * 10 + romanized
    }

    private fun richerText(previous: String, current: String): String = when {
        previous.isBlank() -> current
        current.isBlank() -> previous
        current.length > previous.length -> current
        else -> previous
    }

    private fun normalizeText(value: String): String = value
        .replace('\n', ' ')
        .replace(Regex("\\s+"), " ")
        .replace(Regex("\\s+([,.;:!?])"), "$1")
        .trim()

    private fun Char.isPunctuationWithoutLeadingSpace(): Boolean = this in charArrayOf(',', '.', ';', ':', '!', '?', ')', ']', '}', '’', '\'', '…')

    private data class WordTextRange(val start: Int, val endExclusive: Int)
}

object LyricRoleClassifier {
    private val backgroundRegex = Regex("(?i)^(?:\\(|\\[)?(?:bg|background|backing|choir|chorus)[:：]\\s*")
    private val rightRegex = Regex("(?i)^(?:\\(|\\[)?(?:v2|voice 2|singer 2|right)[:：]\\s*")
    private val leftRegex = Regex("(?i)^(?:\\(|\\[)?(?:v1|voice 1|singer 1|left)[:：]\\s*")
    private val prefixRegexes = listOf(backgroundRegex, rightRegex, leftRegex)

    fun roleOf(text: String): LyricVocalRole = when {
        backgroundRegex.containsMatchIn(text) -> LyricVocalRole.BACKGROUND
        rightRegex.containsMatchIn(text) -> LyricVocalRole.DUET_RIGHT
        leftRegex.containsMatchIn(text) -> LyricVocalRole.DUET_LEFT
        else -> LyricVocalRole.MAIN
    }

    fun stripPrefix(text: String): String = text
        .replace(backgroundRegex, "")
        .replace(rightRegex, "")
        .replace(leftRegex, "")
        .trim()

    fun prefixLength(text: String): Int = prefixRegexes
        .asSequence()
        .mapNotNull { it.find(text) }
        .maxOfOrNull { it.range.last + 1 }
        ?: 0
}

object LyricsResultRanker {
    fun best(candidates: List<LyricsCandidate>, request: LyricsRequest): LyricsRepository.LyricsResult? {
        return candidates
            .map { candidate -> candidate.result.copy(confidence = score(candidate, request)) }
            .filter { it.lines.isNotEmpty() && it.confidence >= 42 }
            .sortedWith(
                compareByDescending<LyricsRepository.LyricsResult> { it.confidence }
                    .thenByDescending { it.lines.any { line -> line.words.isNotEmpty() } }
                    .thenByDescending { it.synced }
                    .thenByDescending { it.lines.size }
            )
            .firstOrNull()
    }

    fun score(candidate: LyricsCandidate, request: LyricsRequest): Int {
        val result = candidate.result
        val titleScore = LyricsMatcher.similarity(candidate.title, request.title)
        val artistScore = LyricsMatcher.similarity(candidate.artist, request.artist)
        val durationDifference = if (request.durationSec > 0L && candidate.durationSec > 0L) {
            (candidate.durationSec - request.durationSec).absoluteValue
        } else {
            -1L
        }
        val durationScore = when {
            durationDifference < 0L -> 3
            durationDifference <= 2L -> 10
            durationDifference <= 5L -> 8
            durationDifference <= 10L -> 5
            durationDifference <= 18L -> 1
            durationDifference <= 30L -> -6
            else -> -14
        }
        val visibleLines = result.lines.filterNot { it.isMetadata || it.text.isBlank() }
        val primaryLines = visibleLines.filter { it.role != LyricVocalRole.BACKGROUND }
        val syncedLines = primaryLines.count { it.startMs >= 0L && it.endMs > it.startMs }
        val syncCoverage = if (primaryLines.isEmpty()) 0 else syncedLines * 100 / primaryLines.size
        val totalCharacters = primaryLines.sumOf { it.text.length.coerceAtLeast(1) }
        val timedCharacters = primaryLines.sumOf { line ->
            if (line.words.isEmpty()) 0 else line.words.sumOf { it.text.trim().length.coerceAtLeast(1) }
        }
        val wordCoverage = if (totalCharacters == 0) 0 else (timedCharacters * 100 / totalCharacters).coerceIn(0, 100)
        val uniqueLines = primaryLines.map { LyricsMatcher.normalize(it.text) }.filter(String::isNotBlank).distinct().size
        val duplicateRatio = if (primaryLines.isEmpty()) 0 else ((primaryLines.size - uniqueLines) * 100 / primaryLines.size).coerceAtLeast(0)
        val malformedWords = primaryLines.sumOf { line -> line.words.count { it.endMs <= it.startMs || it.startMs < line.startMs - 250L || it.endMs > line.endMs + 500L } }
        val totalWords = primaryLines.sumOf { it.words.size }
        val malformedRatio = if (totalWords == 0) 0 else malformedWords * 100 / totalWords
        val lastEndMs = primaryLines.maxOfOrNull { it.endMs } ?: 0L
        val expectedDurationMs = request.durationSec.coerceAtLeast(0L) * 1_000L
        val timelineScore = when {
            !result.synced || primaryLines.size < 4 || expectedDurationMs <= 0L -> 2
            lastEndMs in expectedDurationMs * 55L / 100L..expectedDurationMs * 108L / 100L -> 6
            lastEndMs in expectedDurationMs * 35L / 100L..expectedDurationMs * 120L / 100L -> 2
            else -> -6
        }
        val providerScore = when {
            result.provider.startsWith("Binimum · Word", ignoreCase = true) -> 8
            result.provider.startsWith("LyricsPlus", ignoreCase = true) && result.lines.any { it.words.isNotEmpty() } -> 7
            result.provider.startsWith("YouTube Music", ignoreCase = true) -> 6
            result.provider.startsWith("Binimum", ignoreCase = true) -> 6
            result.provider.startsWith("LyricsPlus", ignoreCase = true) -> 5
            result.provider.startsWith("LRCLIB Exact", ignoreCase = true) -> 5
            result.provider.startsWith("LRCLIB Search", ignoreCase = true) -> 4
            result.provider.startsWith("YouTube Transcript Auto", ignoreCase = true) -> 0
            result.provider.startsWith("YouTube Transcript", ignoreCase = true) -> 3
            result.provider.startsWith("Lyrics.ovh", ignoreCase = true) -> -3
            else -> 0
        }
        val syncScore = when {
            !result.synced -> 0
            syncCoverage >= 95 -> 8
            syncCoverage >= 75 -> 6
            else -> 2
        }
        val wordScore = when {
            wordCoverage >= 85 -> 12
            wordCoverage >= 55 -> 9
            wordCoverage >= 20 -> 4
            else -> 0
        }
        val contentScore = when {
            primaryLines.size >= 18 -> 4
            primaryLines.size >= 8 -> 3
            primaryLines.size >= 3 -> 1
            else -> 0
        }
        val enrichmentScore = listOf(
            visibleLines.any { it.translated.isNotBlank() },
            visibleLines.any { it.romanized.isNotBlank() },
            visibleLines.any { it.role != LyricVocalRole.MAIN }
        ).count { it }
        val providerConfidenceScore = result.confidence.coerceIn(0, 100) * 5 / 100
        val duplicatePenalty = when {
            duplicateRatio >= 45 -> 12
            duplicateRatio >= 30 -> 7
            duplicateRatio >= 18 -> 3
            else -> 0
        }
        val malformedPenalty = when {
            malformedRatio >= 30 -> 14
            malformedRatio >= 15 -> 8
            malformedRatio > 0 -> 3
            else -> 0
        }
        val mismatchPenalty = LyricsMatcher.versionMismatchPenalty(candidate.title, request.title)
        val total = 6 +
            titleScore * 24 / 100 +
            artistScore * 16 / 100 +
            durationScore +
            syncScore +
            wordScore +
            timelineScore +
            providerScore +
            contentScore +
            enrichmentScore +
            providerConfidenceScore -
            duplicatePenalty -
            malformedPenalty -
            mismatchPenalty
        return total.coerceIn(0, 100)
    }
}

object LyricsMatcher {
    private val versionTerms = setOf(
        "live", "remix", "acoustic", "instrumental", "karaoke", "sped", "slowed", "nightcore", "remaster", "demo", "edit", "version", "cover"
    )

    fun similarity(left: String, right: String): Int {
        val normalizedLeft = normalize(left)
        val normalizedRight = normalize(right)
        if (normalizedLeft.isBlank() || normalizedRight.isBlank()) return 0
        if (normalizedLeft == normalizedRight) return 100
        if (normalizedLeft.contains(normalizedRight) || normalizedRight.contains(normalizedLeft)) return 90
        val leftTokens = normalizedLeft.split(" ").filter(String::isNotBlank).toSet()
        val rightTokens = normalizedRight.split(" ").filter(String::isNotBlank).toSet()
        val tokenScore = if (leftTokens.isEmpty() || rightTokens.isEmpty()) 0 else {
            leftTokens.intersect(rightTokens).size * 100 / leftTokens.union(rightTokens).size.coerceAtLeast(1)
        }
        val diceScore = bigramDice(normalizedLeft, normalizedRight)
        val editScore = levenshteinSimilarity(normalizedLeft, normalizedRight)
        return (tokenScore * 45 + diceScore * 30 + editScore * 25) / 100
    }

    fun versionMismatchPenalty(candidate: String, request: String): Int {
        val candidateTerms = normalize(candidate).split(" ").filter { it in versionTerms }.toSet()
        val requestTerms = normalize(request).split(" ").filter { it in versionTerms }.toSet()
        return when {
            candidateTerms == requestTerms -> 0
            requestTerms.isEmpty() && candidateTerms.isNotEmpty() -> 16
            candidateTerms.isEmpty() && requestTerms.isNotEmpty() -> 10
            else -> 13
        }
    }

    fun normalize(value: String): String {
        val withoutFeaturing = value
            .replace(Regex("(?i)\\b(feat(?:uring)?|ft\\.?|con|with)\\b.*$"), " ")
            .replace(Regex("(?i)[(\\[][^)\\]]*(official|video|audio|lyrics?|visuali[sz]er|mv)[^)\\]]*[)\\]]"), " ")
        return Normalizer.normalize(withoutFeaturing, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
            .replace("&", " and ")
            .replace(Regex("[^\\p{L}\\p{N} ]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun bigramDice(left: String, right: String): Int {
        fun grams(value: String): List<String> {
            val compact = value.replace(" ", "")
            if (compact.length < 2) return listOf(compact)
            return (0 until compact.lastIndex).map { compact.substring(it, it + 2) }
        }
        val leftGrams = grams(left).toMutableList()
        val rightGrams = grams(right).toMutableList()
        if (leftGrams.isEmpty() || rightGrams.isEmpty()) return 0
        var matches = 0
        leftGrams.forEach { gram ->
            val index = rightGrams.indexOf(gram)
            if (index >= 0) {
                matches++
                rightGrams.removeAt(index)
            }
        }
        return matches * 200 / (leftGrams.size + grams(right).size).coerceAtLeast(1)
    }

    private fun levenshteinSimilarity(left: String, right: String): Int {
        if (left == right) return 100
        val previous = IntArray(right.length + 1) { it }
        val current = IntArray(right.length + 1)
        for (leftIndex in left.indices) {
            current[0] = leftIndex + 1
            for (rightIndex in right.indices) {
                val substitution = previous[rightIndex] + if (left[leftIndex] == right[rightIndex]) 0 else 1
                current[rightIndex + 1] = minOf(current[rightIndex] + 1, previous[rightIndex + 1] + 1, substitution)
            }
            current.copyInto(previous)
        }
        val distance = previous[right.length]
        return ((1.0 - distance.toDouble() / max(left.length, right.length).coerceAtLeast(1)) * 100.0).toInt().coerceIn(0, 100)
    }
}
