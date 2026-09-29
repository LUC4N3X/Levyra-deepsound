package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricVocalRole
import com.luc4n3x.levyra.domain.LyricWord
import com.luc4n3x.levyra.domain.LyricsProviderId
import com.luc4n3x.levyra.domain.LyricsProviderOrdering
import java.io.StringReader
import java.text.Normalizer
import java.util.Base64
import java.util.Locale
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import timber.log.Timber

data class LyricsRequest(
    val title: String,
    val artist: String,
    val durationSec: Long,
    val album: String = "",
    val recordingId: String = ""
)

data class LyricsCandidate(
    val result: LyricsRepository.LyricsResult,
    val title: String,
    val artist: String,
    val durationSec: Long,
    val album: String = "",
    val recordingId: String = ""
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
    const val MAX_SOURCE_CHARS = 2_000_000

    fun detect(text: String): LyricsFormat {
        val source = text.trimStart()
        return when {
            source.startsWith("<tt", ignoreCase = true) || LYRICS_TT_ELEMENT_MARKER.containsMatchIn(source) -> LyricsFormat.TTML
            source.contains("LyricContent=", ignoreCase = true) -> LyricsFormat.QRC
            LYRICS_YRC_MARKER.containsMatchIn(source) -> LyricsFormat.YRC
            source.contains("[language:", ignoreCase = true) && LYRICS_WORD_TIMED_MARKER.containsMatchIn(source) -> LyricsFormat.KRC
            LYRICS_WORD_TIMED_MARKER.containsMatchIn(source) -> LyricsFormat.QRC
            LYRICS_QRC_SUFFIX_MARKER.containsMatchIn(source) -> LyricsFormat.QRC
            LYRICS_LRC_MARKER.containsMatchIn(source) -> LyricsFormat.LRC
            else -> LyricsFormat.PLAIN
        }
    }

    fun parse(text: String, format: LyricsFormat? = null): List<LyricLine> {
        if (text.isBlank() || text.length > MAX_SOURCE_CHARS) return emptyList()
        val resolvedFormat = format ?: detect(text)
        val parsed = try {
            parseFormat(text, resolvedFormat)
        } catch (error: IllegalArgumentException) {
            parseFailed(error, resolvedFormat)
        } catch (error: IllegalStateException) {
            parseFailed(error, resolvedFormat)
        }
        return LyricsCleaner.clean(parsed.take(MAX_LYRIC_LINES))
    }

    private fun parseFormat(text: String, format: LyricsFormat): List<LyricLine> = when (format) {
        LyricsFormat.TTML -> TtmlLyricsParser.parse(text)
        LyricsFormat.YRC -> YrcLyricsParser.parse(text)
        LyricsFormat.QRC -> QrcLyricsParser.parse(text)
        LyricsFormat.KRC -> KrcLyricsParser.parse(text)
        LyricsFormat.LRC -> LrcLyricsParser.parse(text)
        LyricsFormat.PLAIN -> parsePlain(text)
    }

    private fun parseFailed(error: RuntimeException, format: LyricsFormat): List<LyricLine> {
        Timber.w(error, "Lyrics parsing failed for %s", format)
        return emptyList()
    }

    fun parsePlain(text: String): List<LyricLine> {
        return text.lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .take(MAX_LYRIC_LINES)
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
    private val offsetRegex = Regex("(?im)^\\[offset:\\s*([+-]?\\d+)\\s*]\\s*$")

    fun parse(lrc: String): List<LyricLine> {
        val offsetMs = (offsetRegex.find(lrc)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L)
            .coerceIn(-MAX_LYRIC_OFFSET_MS, MAX_LYRIC_OFFSET_MS)
        val raw = ArrayList<RawLrcLine>()
        val breaks = ArrayList<Long>()
        for (sourceLine in lrc.lineSequence()) {
            if (raw.size >= MAX_LYRIC_LINES) break
            val timestamps = leadingTimestamps(sourceLine) ?: continue
            val content = sourceLine.substring(timestamps.contentStart).trim()
            timestamps.startsMs.forEach { timestamp ->
                val start = (timestamp - offsetMs).coerceAtLeast(0L)
                if (content.isBlank()) breaks += start else raw += RawLrcLine(start, content)
            }
        }
        breaks.sort()

        val merged = mergeSameTimestampTranslations(raw.sortedBy { it.startMs })
        return merged.mapIndexedNotNull { index, item ->
            val fallbackEnd = fallbackEndMs(item.startMs, merged.getOrNull(index + 1)?.startMs, firstGreaterThan(breaks, item.startMs))
            buildLine(item, fallbackEnd, offsetMs)
        }
    }

    private fun fallbackEndMs(startMs: Long, nextStart: Long?, nextBreak: Long?): Long =
        if (nextBreak != null && (nextStart == null || nextBreak < nextStart)) {
            nextBreak.coerceAtLeast(startMs + 350L)
        } else {
            nextStart?.minus(80L)?.coerceAtLeast(startMs + 350L) ?: startMs + 6_000L
        }

    private fun buildLine(item: RawLrcLine, fallbackEnd: Long, offsetMs: Long): LyricLine? {
        val words = parseEnhancedWords(item.content, item.startMs, fallbackEnd, offsetMs)
        val visibleText = if (words.isNotEmpty()) {
            words.joinToString("") { it.text }.replace(LYRICS_WHITESPACE, " ").trim()
        } else {
            enhancedTimestampRegex.replace(item.content, "").trim()
        }
        if (visibleText.isBlank()) return null
        return LyricLine(
            startMs = item.startMs,
            endMs = max(fallbackEnd, words.lastOrNull()?.endMs ?: fallbackEnd),
            text = visibleText,
            translated = item.translation,
            words = words,
            role = LyricRoleClassifier.roleOf(visibleText)
        )
    }

    private fun leadingTimestamps(sourceLine: String): LeadingTimestamps? {
        var cursor = 0
        while (cursor < sourceLine.length && sourceLine[cursor].isWhitespace()) cursor++
        val starts = ArrayList<Long>(2)
        while (true) {
            val match = timestampRegex.matchAt(sourceLine, cursor) ?: break
            starts += timestampMs(match.groupValues)
            cursor = match.range.last + 1
        }
        return if (starts.isEmpty()) null else LeadingTimestamps(starts, cursor)
    }

    private fun mergeSameTimestampTranslations(sorted: List<RawLrcLine>): List<RawLrcLine> {
        val groups = sameTimestampGroups(sorted)
        val pairCount = groups.count(::isTranslationPair)
        val translatedDocument = pairCount >= MIN_TRANSLATION_PAIRS &&
            pairCount * 100 >= groups.size * MIN_TRANSLATION_PAIR_PERCENT
        if (!translatedDocument) return sorted
        return groups.flatMap { group ->
            if (isTranslationPair(group)) listOf(group[0].copy(translation = group[1].content.trim())) else group
        }
    }

    private fun sameTimestampGroups(sorted: List<RawLrcLine>): List<List<RawLrcLine>> {
        val groups = ArrayList<List<RawLrcLine>>()
        var index = 0
        while (index < sorted.size) {
            var groupEnd = index + 1
            while (groupEnd < sorted.size && sorted[groupEnd].startMs == sorted[index].startMs) groupEnd++
            groups += sorted.subList(index, groupEnd)
            index = groupEnd
        }
        return groups
    }

    private fun isTranslationPair(group: List<RawLrcLine>): Boolean {
        if (group.size != 2) return false
        val translation = group[1].content
        if (enhancedTimestampRegex.containsMatchIn(translation)) return false
        return !plainText(group[0].content).equals(translation.trim(), ignoreCase = true)
    }

    private fun plainText(content: String): String = enhancedTimestampRegex.replace(content, "").trim()

    private fun firstGreaterThan(sorted: List<Long>, value: Long): Long? {
        var low = 0
        var high = sorted.size
        while (low < high) {
            val middle = low + (high - low) / 2
            if (sorted[middle] <= value) low = middle + 1 else high = middle
        }
        return sorted.getOrNull(low)
    }

    private fun parseEnhancedWords(content: String, lineStartMs: Long, lineEndMs: Long, offsetMs: Long): List<LyricWord> {
        val matches = enhancedTimestampRegex.findAll(content).toList()
        if (matches.isEmpty()) return emptyList()
        val words = ArrayList<LyricWord>(matches.size + 1)
        val markerStarts = matches.map { (timestampMs(it.groupValues) - offsetMs).coerceAtLeast(lineStartMs) }
        val leadingText = content.substring(0, matches.first().range.first)
        if (leadingText.isNotBlank()) {
            val end = (markerStarts.first() - 20L).coerceAtLeast(lineStartMs + 40L)
            words += LyricWord(startMs = lineStartMs, endMs = end, text = leadingText)
        }
        matches.forEachIndexed { index, match ->
            val start = markerStarts[index]
            val nextMarkerStart = markerStarts.getOrNull(index + 1)
            val rawTextStart = match.range.last + 1
            val rawTextEnd = matches.getOrNull(index + 1)?.range?.first ?: content.length
            val wordText = content.substring(rawTextStart, rawTextEnd)
            if (wordText.isBlank()) {
                if (words.isNotEmpty() && wordText.isNotEmpty() && !words.last().text.last().isWhitespace()) {
                    words[words.lastIndex] = words.last().let { it.copy(text = it.text + " ") }
                }
                return@forEachIndexed
            }
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

    private data class RawLrcLine(val startMs: Long, val content: String, val translation: String = "")

    private const val MIN_TRANSLATION_PAIRS = 2
    private const val MIN_TRANSLATION_PAIR_PERCENT = 60

    private class LeadingTimestamps(val startsMs: List<Long>, val contentStart: Int)
}

object YrcLyricsParser {
    private val markerRegex = Regex("\\((\\d+),(\\d+),\\d+\\)")

    fun parse(text: String): List<LyricLine> =
        parseSyllableLines(text, markerRegex, textFollowsMarker = true, relativeWhenAmbiguous = false)
            .map { it.line }
            .sortedBy { it.startMs }
}

object QrcLyricsParser {
    private val prefixMarkerRegex = Regex("<(\\d+),(\\d+),\\d+>")
    private val suffixMarkerRegex = Regex("\\((\\d+),(\\d+)\\)")
    private val lyricContentRegex = Regex("LyricContent\\s*=\\s*\"([^\"]*)\"", RegexOption.IGNORE_CASE)

    fun parse(text: String): List<LyricLine> {
        val content = lyricContentRegex.find(text)?.groupValues?.get(1)?.let(::unescapeXmlText) ?: text
        val prefixed = LYRICS_WORD_TIMED_MARKER.containsMatchIn(content)
        val lines = if (prefixed) {
            parseSyllableLines(content, prefixMarkerRegex, textFollowsMarker = true, relativeWhenAmbiguous = false)
        } else {
            parseSyllableLines(content, suffixMarkerRegex, textFollowsMarker = false, relativeWhenAmbiguous = false)
        }
        return lines.map { it.line }.sortedBy { it.startMs }
    }
}

object KrcLyricsParser {
    private val markerRegex = Regex("<(\\d+),(\\d+),\\d+>")
    private val languageRegex = Regex("(?im)^\\[language:\\s*([A-Za-z0-9+/=_-]+)\\s*]\\s*$")

    fun parse(text: String): List<LyricLine> {
        val lines = parseSyllableLines(text, markerRegex, textFollowsMarker = true, relativeWhenAmbiguous = true)
        val language = languageRegex.find(text)?.groupValues?.get(1)?.let(::decodeLanguage)
        val enriched = if (language == null) lines.map { it.line } else applyLanguage(lines, language)
        return enriched.sortedBy { it.startMs }
    }

    private fun decodeLanguage(encoded: String): KrcLanguage? {
        if (encoded.length > MAX_KRC_LANGUAGE_CHARS) return null
        val json = try {
            String(Base64.getDecoder().decode(encoded), Charsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            return null
        }
        val content = runCatching { JSONObject(json) }.getOrNull()?.optJSONArray("content") ?: return null
        val language = KrcLanguage(languageContent(content, KRC_TYPE_TRANSLATION), languageContent(content, KRC_TYPE_ROMANIZATION))
        return language.takeIf { it.translation != null || it.romanization != null }
    }

    private fun languageContent(content: JSONArray, type: Int): JSONArray? {
        for (index in 0 until content.length()) {
            val entry = content.optJSONObject(index)
            if (entry?.optInt("type", -1) == type) entry.optJSONArray("lyricContent")?.let { return it }
        }
        return null
    }

    private fun applyLanguage(lines: List<SyllableLine>, language: KrcLanguage): List<LyricLine> =
        lines.mapIndexed { index, parsed ->
            val line = parsed.line
            val translated = language.translation?.optJSONArray(index)?.joinedText(" ").orEmpty()
            val romanizedParts = language.romanization?.optJSONArray(index)
            val romanizedWords = if (romanizedParts != null && romanizedParts.length() == line.words.size) {
                line.words.mapIndexed { wordIndex, word ->
                    word.copy(romanized = romanizedParts.optString(wordIndex).trim())
                }
            } else {
                line.words
            }
            line.copy(
                translated = translated.ifBlank { line.translated },
                romanized = romanizedParts?.joinedText(" ").orEmpty().ifBlank { line.romanized },
                words = romanizedWords
            )
        }

    private fun JSONArray.joinedText(separator: String): String {
        val parts = ArrayList<String>(length())
        for (index in 0 until length()) {
            optString(index).trim().takeIf(String::isNotEmpty)?.let(parts::add)
        }
        return parts.joinToString(separator).replace(LYRICS_WHITESPACE, " ").trim()
    }

    private class KrcLanguage(val translation: JSONArray?, val romanization: JSONArray?)

    private const val KRC_TYPE_ROMANIZATION = 0
    private const val KRC_TYPE_TRANSLATION = 1
    private const val MAX_KRC_LANGUAGE_CHARS = 1_000_000
}

private fun LyricLine.duplicates(other: LyricLine): Boolean =
    startMs == other.startMs && role == other.role && text.equals(other.text, ignoreCase = true)

private fun resolvedEnd(line: LyricLine, nextMainStart: Long?): Long {
    val naturalEnd = when {
        line.endMs > line.startMs -> line.endMs
        nextMainStart != null -> nextMainStart - 60L
        else -> line.startMs + 6_000L
    }
    val lastWordEnd = line.words.maxOfOrNull { it.endMs } ?: naturalEnd
    val end = when {
        lastWordEnd <= naturalEnd -> naturalEnd
        nextMainStart == null -> lastWordEnd
        else -> min(lastWordEnd, max(naturalEnd, nextMainStart - 60L))
    }
    return end.coerceAtLeast(line.startMs + 120L)
}

private class SyllableLine(val line: LyricLine)

private class RawSyllable(val timing: Long, val durationMs: Long, val text: String)

private class RawSyllableLine(val startMs: Long, val durationMs: Long, val payload: String, val syllables: List<RawSyllable>)

private fun parseSyllableLines(
    text: String,
    markerRegex: Regex,
    textFollowsMarker: Boolean,
    relativeWhenAmbiguous: Boolean
): List<SyllableLine> {
    val rawLines = ArrayList<RawSyllableLine>()
    for (source in text.lineSequence()) {
        if (rawLines.size >= MAX_LYRIC_LINES) break
        val match = LYRICS_SYLLABLE_LINE.matchEntire(source.trim()) ?: continue
        val lineStart = match.groupValues[1].toLongOrNull() ?: continue
        val duration = match.groupValues[2].toLongOrNull()?.coerceAtLeast(1L) ?: continue
        val payload = match.groupValues[3]
        rawLines += RawSyllableLine(lineStart, duration, payload, syllables(payload, markerRegex, textFollowsMarker))
    }
    val relative = syllableTimingIsRelative(rawLines, relativeWhenAmbiguous)
    return rawLines.mapNotNull { raw ->
        val words = raw.syllables.map { syllable ->
            val start = if (relative) raw.startMs + syllable.timing else syllable.timing
            LyricWord(startMs = start, endMs = start + syllable.durationMs, text = syllable.text)
        }
        val visibleText = if (words.isNotEmpty()) {
            words.joinToString("") { it.text }.replace(LYRICS_WHITESPACE, " ").trim()
        } else {
            markerRegex.replace(raw.payload, "").trim()
        }
        if (visibleText.isBlank()) return@mapNotNull null
        SyllableLine(
            LyricLine(
                startMs = raw.startMs,
                endMs = raw.startMs + raw.durationMs,
                text = visibleText,
                translated = "",
                words = words.filter { it.text.isNotBlank() },
                role = LyricRoleClassifier.roleOf(visibleText)
            )
        )
    }
}

private fun syllables(payload: String, markerRegex: Regex, textFollowsMarker: Boolean): List<RawSyllable> {
    val markers = markerRegex.findAll(payload).take(MAX_WORDS_PER_LINE).toList()
    if (markers.isEmpty()) return emptyList()
    val out = ArrayList<RawSyllable>(markers.size)
    markers.forEachIndexed { index, marker ->
        val textStart: Int
        val textEnd: Int
        if (textFollowsMarker) {
            textStart = marker.range.last + 1
            textEnd = markers.getOrNull(index + 1)?.range?.first ?: payload.length
        } else {
            textStart = markers.getOrNull(index - 1)?.range?.last?.plus(1) ?: 0
            textEnd = marker.range.first
        }
        val value = payload.substring(textStart, textEnd)
        val timing = marker.groupValues[1].toLongOrNull() ?: return@forEachIndexed
        val duration = marker.groupValues[2].toLongOrNull()?.coerceAtLeast(0L) ?: return@forEachIndexed
        if (value.isEmpty()) return@forEachIndexed
        if (value.isBlank()) {
            out.lastOrNull()?.takeUnless { it.text.last().isWhitespace() }?.let { previous ->
                out[out.lastIndex] = RawSyllable(previous.timing, previous.durationMs, previous.text + " ")
            }
            return@forEachIndexed
        }
        out += RawSyllable(timing, duration, value)
    }
    return out
}

private fun syllableTimingIsRelative(lines: List<RawSyllableLine>, relativeWhenAmbiguous: Boolean): Boolean {
    var relativeVotes = 0
    var absoluteVotes = 0
    for (line in lines) {
        val first = line.syllables.firstOrNull() ?: continue
        if (line.startMs < SYLLABLE_TIMING_AMBIGUITY_MS) continue
        if (first.timing + SYLLABLE_TIMING_TOLERANCE_MS < line.startMs) relativeVotes++ else absoluteVotes++
    }
    return if (relativeVotes == absoluteVotes) relativeWhenAmbiguous else relativeVotes > absoluteVotes
}

private fun unescapeXmlText(value: String): String {
    if ('&' !in value) return value
    return XML_ENTITY_REGEX.replace(value) { match ->
        val entity = match.groupValues[1]
        when {
            entity.equals("quot", ignoreCase = true) -> "\""
            entity.equals("apos", ignoreCase = true) -> "'"
            entity.equals("lt", ignoreCase = true) -> "<"
            entity.equals("gt", ignoreCase = true) -> ">"
            entity.equals("amp", ignoreCase = true) -> "&"
            entity.startsWith("#x", ignoreCase = true) -> entity.drop(2).toIntOrNull(16)?.xmlCodePoint() ?: match.value
            entity.startsWith("#") -> entity.drop(1).toIntOrNull()?.xmlCodePoint() ?: match.value
            else -> match.value
        }
    }
}

private fun Int.xmlCodePoint(): String? =
    takeIf { Character.isValidCodePoint(it) }?.let { String(Character.toChars(it)) }

object TtmlLyricsParser {
    fun parse(ttml: String): List<LyricLine> {
        val document = runCatching {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = false
                runCatching { setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true) }
                runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
                runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
                runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
                runCatching { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) }
                isXIncludeAware = false
                isExpandEntityReferences = false
            }
            factory.newDocumentBuilder().parse(InputSource(StringReader(ttml)))
        }.getOrNull() ?: return emptyList()

        val root = document.documentElement ?: return emptyList()
        val offsetMs = root.lyricOffsetMs()
        val nodes = root.descendantsNamed("p").take(MAX_LYRIC_LINES)
        val agents = TtmlAgents.from(root, nodes)
        val lines = ArrayList<LyricLine>()
        for (element in nodes) {
            val start = element.timeAttribute("begin") ?: element.firstChildSpanBeginMs() ?: continue
            val end = element.timeAttribute("end")
                ?: element.timeAttribute("dur")?.let { start + it }
                ?: (start + 6_000L)
            element.mainLine(start, end, agents)?.let(lines::add)
            lines += element.backgroundLines(start, end)
        }
        val ordered = lines.sortedWith(compareBy<LyricLine> { it.startMs }.thenBy { it.role.ordinal })
        return if (offsetMs == 0L) ordered else ordered.map { it.shiftedBy(offsetMs) }
    }

    private fun Element.mainLine(start: Long, end: Long, agents: TtmlAgents): LyricLine? {
        val words = wordSpans()
        val text = if (words.isNotEmpty()) {
            words.joinToString("") { it.text }.replace(LYRICS_WHITESPACE, " ").trim()
        } else {
            baseText().replace(LYRICS_WHITESPACE, " ").trim()
        }
        if (text.isBlank()) return null
        return LyricLine(
            startMs = start,
            endMs = max(end, words.lastOrNull()?.endMs ?: end).coerceAtLeast(start + 120L),
            text = text,
            translated = specialSpanText("x-translation"),
            words = words,
            romanized = specialSpanText("x-roman"),
            role = lyricRole(agents)
        )
    }

    private fun LyricLine.shiftedBy(offsetMs: Long): LyricLine {
        val earliestMs = minOf(startMs, words.minOfOrNull { it.startMs } ?: startMs)
        val appliedOffset = offsetMs.coerceAtLeast(-earliestMs)
        return copy(
            startMs = startMs + appliedOffset,
            endMs = endMs + appliedOffset,
            words = words.map {
                it.copy(startMs = it.startMs + appliedOffset, endMs = it.endMs + appliedOffset)
            }
        )
    }

    private fun Element.lyricOffsetMs(): Long {
        val audio = childElement("head")?.childElement("metadata")?.childElement("audio") ?: return 0L
        val raw = audio.namedAttribute("lyricOffset").takeIf(String::isNotBlank) ?: return 0L
        val seconds = raw.trim().toDoubleOrNull()?.takeIf(Double::isFinite) ?: return 0L
        return (seconds * 1_000L).toLong().coerceIn(-MAX_LYRIC_OFFSET_MS, MAX_LYRIC_OFFSET_MS)
    }

    private fun Element.childElement(localName: String): Element? {
        val children = childNodes
        for (index in 0 until children.length) {
            val child = children.item(index) as? Element ?: continue
            if (child.localNodeName().equals(localName, ignoreCase = true)) return child
        }
        return null
    }

    private fun Element.descendantsNamed(localName: String): List<Element> {
        val all = getElementsByTagName("*")
        val out = ArrayList<Element>()
        for (index in 0 until all.length) {
            val element = all.item(index) as? Element ?: continue
            if (element.localNodeName().equals(localName, ignoreCase = true)) out += element
        }
        return out
    }

    private fun Element.firstChildSpanBeginMs(): Long? {
        val children = childNodes
        var earliest: Long? = null
        for (index in 0 until children.length) {
            val child = children.item(index) as? Element ?: continue
            if (!child.localNodeName().equals("span", ignoreCase = true)) continue
            if (child.roleAttribute() in SPECIAL_ROLES) continue
            val begin = child.timeAttribute("begin") ?: continue
            if (earliest == null || begin < earliest) earliest = begin
        }
        return earliest
    }

    private fun Element.localNodeName(): String = nodeName.orEmpty().substringAfterLast(':')

    private fun Element.namedAttribute(localName: String): String {
        val direct = getAttribute(localName)
        if (direct.isNotBlank()) return direct
        val attributes = attributes ?: return ""
        for (index in 0 until attributes.length) {
            val item = attributes.item(index) ?: continue
            val qualified = item.nodeName.orEmpty()
            if (qualified == localName) continue
            if (!qualified.substringAfterLast(':').equals(localName, ignoreCase = true)) continue
            val value = item.nodeValue.orEmpty()
            if (value.isNotBlank()) return value
        }
        return ""
    }

    private fun Element.wordSpans(): List<LyricWord> {
        val out = descendantsNamed("span")
            .asSequence()
            .filter { span -> span.roleAttribute() !in SPECIAL_ROLES && !span.hasAncestorWithSpecialRole(this) }
            .mapNotNull { span -> span.timedWord(this) }
            .take(MAX_WORDS_PER_LINE)
            .toMutableList()
        out.lastOrNull()?.let { last -> out[out.lastIndex] = last.copy(text = last.text.trimEnd()) }
        return out
    }

    private fun Element.timedWord(line: Element): LyricWord? {
        val start = timeAttribute("begin") ?: return null
        val end = timeAttribute("end") ?: timeAttribute("dur")?.let { start + it } ?: return null
        val text = directText().takeIf(String::isNotBlank) ?: return null
        val spaced = if (!text.last().isWhitespace() && followedByWhitespace(line)) "$text " else text
        return LyricWord(startMs = start, endMs = end.coerceAtLeast(start + 20L), text = spaced)
    }

    private fun Element.followedByWhitespace(root: Element): Boolean {
        var current: Node = this
        while (current !== root) {
            val sibling = current.nextSibling
            if (sibling != null) {
                val isText = sibling.nodeType == Node.TEXT_NODE || sibling.nodeType == Node.CDATA_SECTION_NODE
                return isText && sibling.nodeValue.orEmpty().firstOrNull()?.isWhitespace() == true
            }
            current = current.parentNode ?: return false
        }
        return false
    }

    private fun Element.backgroundLines(parentStart: Long, parentEnd: Long): List<LyricLine> {
        val out = ArrayList<LyricLine>()
        for (span in descendantsNamed("span")) {
            if (span.roleAttribute() != "x-bg") continue
            val start = span.timeAttribute("begin") ?: parentStart
            val end = span.timeAttribute("end")
                ?: span.timeAttribute("dur")?.let { start + it }
                ?: parentEnd
            val words = span.wordSpans()
            val text = if (words.isNotEmpty()) {
                words.joinToString("") { it.text }.replace(LYRICS_WHITESPACE, " ").trim()
            } else {
                span.baseText().replace(LYRICS_WHITESPACE, " ").trim()
            }
            if (text.isBlank()) continue
            out += LyricLine(
                startMs = start,
                endMs = max(end, words.lastOrNull()?.endMs ?: end).coerceAtLeast(start + 120L),
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
        val values = ArrayList<String>()
        for (span in descendantsNamed("span")) {
            if (span.roleAttribute() != expectedRole || span.hasAncestorWithSpecialRole(this)) continue
            val value = span.textContent.orEmpty().replace(LYRICS_WHITESPACE, " ").trim()
            if (value.isNotBlank()) values += value
        }
        return values.distinct().joinToString(" ")
    }

    private fun Element.lyricRole(agents: TtmlAgents): LyricVocalRole {
        if (roleAttribute() == "x-bg") return LyricVocalRole.BACKGROUND
        val explicit = getAttribute("data-duet").ifBlank { namedAttribute("duet") }.lowercase(Locale.ROOT)
        return when (explicit) {
            "right" -> LyricVocalRole.DUET_RIGHT
            "left" -> LyricVocalRole.DUET_LEFT
            else -> agents.roleOf(namedAttribute("agent"))
        }
    }

    private class TtmlAgents(
        private val groupAgents: Set<String>,
        private val personOrder: List<String>
    ) {
        fun roleOf(rawAgent: String): LyricVocalRole {
            val agent = rawAgent.trim().lowercase(Locale.ROOT)
            if (agent.isBlank() || agent in groupAgents) return LyricVocalRole.MAIN
            if (agent in RIGHT_AGENT_IDS) return LyricVocalRole.DUET_RIGHT
            if (personOrder.size < 2) return LyricVocalRole.MAIN
            if (agent in LEFT_AGENT_IDS) return LyricVocalRole.DUET_LEFT
            val index = personOrder.indexOf(agent)
            return when {
                index < 0 -> LyricVocalRole.MAIN
                index % 2 == 0 -> LyricVocalRole.DUET_LEFT
                else -> LyricVocalRole.DUET_RIGHT
            }
        }

        companion object {
            fun from(root: Element, lines: List<Element>): TtmlAgents {
                val groups = HashSet<String>()
                for (agent in root.descendantsNamed("agent")) {
                    val id = agent.namedAttribute("id").trim().lowercase(Locale.ROOT)
                    if (id.isNotBlank() && agent.namedAttribute("type").equals("group", ignoreCase = true)) groups += id
                }
                val order = LinkedHashSet<String>()
                for (line in lines) {
                    val agent = line.namedAttribute("agent").trim().lowercase(Locale.ROOT)
                    if (agent.isNotBlank() && agent !in groups) order += agent
                }
                return TtmlAgents(groups, order.toList())
            }
        }
    }

    private fun Element.roleAttribute(): String = namedAttribute("role").lowercase(Locale.ROOT)

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

    private fun Element.timeAttribute(name: String): Long? = namedAttribute(name).takeIf(String::isNotBlank)?.let(::parseTimeMs)

    private fun parseTimeMs(raw: String): Long? {
        val value = raw.trim()
        val millis = when {
            value.endsWith("ms") -> value.removeSuffix("ms").toDoubleOrNull()
            value.endsWith("s") -> value.removeSuffix("s").toDoubleOrNull()?.times(1_000.0)
            value.endsWith("m") -> value.removeSuffix("m").toDoubleOrNull()?.times(60_000.0)
            value.endsWith("h") -> value.removeSuffix("h").toDoubleOrNull()?.times(3_600_000.0)
            else -> clockTimeMs(value)
        }
        return millis?.takeIf { it.isFinite() && it >= 0.0 && it <= MAX_TTML_TIME_MS }?.toLong()
    }

    private fun clockTimeMs(value: String): Double? {
        val parts = value.split(":")
        return when (parts.size) {
            2 -> {
                val minutes = parts[0].toLongOrNull() ?: return null
                val seconds = parts[1].toDoubleOrNull() ?: return null
                minutes * 60_000.0 + seconds * 1_000.0
            }
            3 -> {
                val hours = parts[0].toLongOrNull() ?: return null
                val minutes = parts[1].toLongOrNull() ?: return null
                val seconds = parts[2].toDoubleOrNull() ?: return null
                hours * 3_600_000.0 + minutes * 60_000.0 + seconds * 1_000.0
            }
            else -> value.toDoubleOrNull()?.times(1_000.0)
        }
    }

    private const val MAX_TTML_TIME_MS = 24.0 * 3_600_000.0

    private val SPECIAL_ROLES = setOf("x-translation", "x-roman", "x-bg")
    private val RIGHT_AGENT_IDS = setOf("v2", "voice2", "agent2", "singer2")
    private val LEFT_AGENT_IDS = setOf("v1", "voice1", "agent1", "singer1")
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
            if (previous != null && previous.duplicates(line)) {
                deduplicated[deduplicated.lastIndex] = mergeDuplicate(previous, line)
            } else {
                deduplicated += line
            }
        }
        return withResolvedEnds(deduplicated)
    }

    private fun withResolvedEnds(lines: List<LyricLine>): List<LyricLine> {
        val nextMainStarts = arrayOfNulls<Long>(lines.size)
        var upcomingMainStart: Long? = null
        for (index in lines.indices.reversed()) {
            nextMainStarts[index] = upcomingMainStart
            if (lines[index].role != LyricVocalRole.BACKGROUND) upcomingMainStart = lines[index].startMs
        }
        return lines.mapIndexed { index, line -> line.copy(endMs = resolvedEnd(line, nextMainStarts[index])) }
    }

    private fun cleanTimedWords(words: List<LyricWord>): List<LyricWord> {
        var floorMs = 0L
        val normalized = words.mapNotNull { word ->
            val value = word.text.replace(LYRICS_WHITESPACE, " ")
            if (value.isBlank()) return@mapNotNull null
            val start = max(word.startMs, floorMs)
            floorMs = start
            word.copy(text = value, startMs = start, endMs = max(word.endMs, start))
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
            val trailing = if (word.text.last().isWhitespace()) " " else ""
            if (value.isBlank()) null else word.copy(text = value + trailing)
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
        .replace(LYRICS_WHITESPACE, " ")
        .replace(LYRICS_PUNCTUATION_SPACING, "$1")
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
    fun best(
        candidates: List<LyricsCandidate>,
        request: LyricsRequest,
        ordering: LyricsProviderOrdering? = null
    ): LyricsRepository.LyricsResult? {
        val ranked = scoreAndSort(candidates, request, ordering, keepRejected = false)
        val primary = ranked.firstOrNull() ?: return null
        return LyricsCandidateFusion.enrich(primary, ranked.drop(1), request).result
    }

    fun rankedCandidates(
        candidates: List<LyricsCandidate>,
        request: LyricsRequest,
        ordering: LyricsProviderOrdering? = null
    ): List<LyricsCandidate> {
        return scoreAndSort(candidates, request, ordering, keepRejected = true).distinctBy(::duplicateKey)
    }

    private fun scoreAndSort(
        candidates: List<LyricsCandidate>,
        request: LyricsRequest,
        ordering: LyricsProviderOrdering?,
        keepRejected: Boolean
    ): List<LyricsCandidate> {
        val providerOrder = ordering?.enabledIds ?: LyricsProviderId.DEFAULT_ORDER
        return candidates
            .mapNotNull { candidate ->
                val strength = LyricsMatcher.matchStrength(candidate, request)
                if (strength == LyricsMatchStrength.REJECTED && !keepRejected) return@mapNotNull null
                val scored = score(candidate, request, ordering)
                val confidence = when (strength) {
                    LyricsMatchStrength.STRONG -> scored
                    LyricsMatchStrength.PLAUSIBLE -> min(scored, PLAUSIBLE_MATCH_CONFIDENCE_CAP)
                    LyricsMatchStrength.REJECTED -> min(scored, REJECTED_MATCH_CONFIDENCE_CAP)
                }
                candidate.copy(result = candidate.result.copy(confidence = confidence))
            }
            .filter { it.result.lines.isNotEmpty() && it.result.confidence >= 42 }
            .sortedWith(
                compareByDescending<LyricsCandidate> { it.result.confidence }
                    .thenByDescending { it.result.lines.any { line -> line.words.isNotEmpty() } }
                    .thenByDescending { it.result.synced }
                    .thenByDescending { it.result.lines.size }
                    .thenBy { candidate -> providerRank(candidate.result.provider, providerOrder) }
                    .thenBy { it.result.provider }
            )
    }

    private fun providerRank(provider: String, order: List<LyricsProviderId>): Int {
        val index = LyricsProviderId.of(provider)?.let(order::indexOf) ?: -1
        return if (index < 0) Int.MAX_VALUE else index
    }

    fun score(
        candidate: LyricsCandidate,
        request: LyricsRequest,
        ordering: LyricsProviderOrdering? = null
    ): Int {
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
        val recordingIdentityScore = when {
            request.recordingId.isBlank() || candidate.recordingId.isBlank() -> 0
            request.recordingId == candidate.recordingId -> 14
            else -> -24
        }
        val albumScore = when {
            request.album.isBlank() || candidate.album.isBlank() -> 0
            LyricsMatcher.similarity(candidate.album, request.album) >= 92 -> 5
            LyricsMatcher.similarity(candidate.album, request.album) >= 75 -> 2
            else -> -4
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
        val providerScore = ordering?.providerScoreFor(result.provider)
            ?: when {
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
            recordingIdentityScore +
            albumScore +
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

    private fun duplicateKey(candidate: LyricsCandidate): String {
        val result = candidate.result
        val timing = result.lines.joinToString("|") { line ->
            "${line.startMs}:${line.endMs}:${line.role}:${LyricsMatcher.normalize(line.text)}"
        }
        return "${result.synced}:${result.lines.any { it.words.isNotEmpty() }}:$timing"
    }
}

object LyricsCandidateFusion {
    fun enrich(
        primary: LyricsCandidate,
        alternatives: List<LyricsCandidate>,
        request: LyricsRequest
    ): LyricsCandidate {
        var result = primary.result
        val providers = linkedSetOf(result.provider)
        alternatives.forEach { secondary ->
            if (!sameRecording(primary, secondary, request)) return@forEach
            val merged = mergeSupplemental(result.lines, secondary.result.lines) ?: return@forEach
            if (merged == result.lines) return@forEach
            result = result.copy(lines = merged)
            providers += secondary.result.provider
        }
        return primary.copy(result = result.copy(provider = providers.filter(String::isNotBlank).joinToString(" + ")))
    }

    fun sameRecording(
        primary: LyricsCandidate,
        secondary: LyricsCandidate,
        request: LyricsRequest
    ): Boolean {
        if (primary.recordingId.isNotBlank() && secondary.recordingId.isNotBlank()) {
            if (primary.recordingId != secondary.recordingId) return false
        } else {
            if (LyricsMatcher.similarity(primary.title, secondary.title) < 94) return false
            if (LyricsMatcher.similarity(primary.artist, secondary.artist) < 88) return false
            if (LyricsMatcher.versionMismatchPenalty(primary.title, secondary.title) != 0) return false
            if (LyricsMatcher.versionMismatchPenalty(secondary.title, primary.title) != 0) return false
            if (primary.durationSec > 0L && secondary.durationSec > 0L &&
                (primary.durationSec - secondary.durationSec).absoluteValue > 5L
            ) {
                return false
            }
            if (primary.album.isNotBlank() && secondary.album.isNotBlank() &&
                LyricsMatcher.similarity(primary.album, secondary.album) < 85
            ) {
                return false
            }
        }
        if (request.recordingId.isNotBlank()) {
            val explicitIds = listOf(primary.recordingId, secondary.recordingId).filter(String::isNotBlank)
            if (explicitIds.any { it != request.recordingId }) return false
        }
        return lineAlignmentIsSafe(primary.result.lines, secondary.result.lines)
    }

    private fun lineAlignmentIsSafe(primary: List<LyricLine>, secondary: List<LyricLine>): Boolean {
        if (primary.isEmpty() || primary.size != secondary.size) return false
        return primary.indices.all { index ->
            val left = primary[index]
            val right = secondary[index]
            left.role == right.role &&
                left.isInstrumental == right.isInstrumental &&
                left.isMetadata == right.isMetadata &&
                LyricsMatcher.similarity(left.text, right.text) >= 94
        }
    }

    private fun mergeSupplemental(primary: List<LyricLine>, secondary: List<LyricLine>): List<LyricLine>? {
        if (!lineAlignmentIsSafe(primary, secondary)) return null
        return primary.indices.map { index ->
            val authoritative = primary[index]
            val supplemental = secondary[index]
            authoritative.copy(
                translated = authoritative.translated.ifBlank { supplemental.translated },
                romanized = authoritative.romanized.ifBlank { supplemental.romanized },
                words = mergeWordRomanization(authoritative.words, supplemental.words)
            )
        }
    }

    private fun mergeWordRomanization(primary: List<LyricWord>, secondary: List<LyricWord>): List<LyricWord> {
        if (primary.isEmpty() || primary.size != secondary.size) return primary
        if (primary.indices.any { index ->
                LyricsMatcher.similarity(primary[index].text, secondary[index].text) < 98
            }
        ) {
            return primary
        }
        return primary.indices.map { index ->
            primary[index].copy(romanized = primary[index].romanized.ifBlank { secondary[index].romanized })
        }
    }
}

enum class LyricsMatchStrength {
    STRONG,
    PLAUSIBLE,
    REJECTED
}

object LyricsMatcher {
    private val versionTerms = setOf(
        "live", "remix", "acoustic", "instrumental", "karaoke", "sped", "slowed", "nightcore", "remaster", "demo", "edit", "version", "cover"
    )
    private val featuringSuffixRegex = Regex("(?i)\\b(feat(?:uring)?|ft\\.?|con|with)\\b.*$")
    private val officialMetadataRegex = Regex("(?i)[(\\[][^)\\]]*(official|video|audio|lyrics?|visuali[sz]er|mv)[^)\\]]*[)\\]]")
    private val combiningMarksRegex = Regex("\\p{M}+")
    private val nonWordRegex = Regex("[^\\p{L}\\p{N} ]+")
    private val whitespaceRegex = Regex("\\s+")

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

    fun matchStrength(candidate: LyricsCandidate, request: LyricsRequest): LyricsMatchStrength {
        val identity = recordingIdentityStrength(candidate, request)
        if (identity != null) return identity
        val evidence = MatchEvidence(
            titleScore = knownSimilarity(candidate.title, request.title),
            artistScore = knownSimilarity(candidate.artist, request.artist),
            durationDifference = durationDifferenceSec(candidate.durationSec, request.durationSec),
            versionMismatch = versionMismatchPenalty(candidate.title, request.title) != 0
        )
        return when {
            evidence.rejected() -> LyricsMatchStrength.REJECTED
            evidence.strong() -> LyricsMatchStrength.STRONG
            else -> LyricsMatchStrength.PLAUSIBLE
        }
    }

    private fun recordingIdentityStrength(candidate: LyricsCandidate, request: LyricsRequest): LyricsMatchStrength? {
        if (request.recordingId.isBlank() || candidate.recordingId.isBlank()) return null
        return if (request.recordingId == candidate.recordingId) LyricsMatchStrength.STRONG else LyricsMatchStrength.REJECTED
    }

    private fun knownSimilarity(candidate: String, requested: String): Int =
        if (candidate.isBlank() || requested.isBlank()) UNKNOWN_SCORE else similarity(candidate, requested)

    private fun durationDifferenceSec(candidateSec: Long, requestedSec: Long): Long? =
        if (candidateSec > 0L && requestedSec > 0L) (candidateSec - requestedSec).absoluteValue else null

    private class MatchEvidence(
        val titleScore: Int,
        val artistScore: Int,
        val durationDifference: Long?,
        val versionMismatch: Boolean
    ) {
        private val titleKnown = titleScore != UNKNOWN_SCORE
        private val artistKnown = artistScore != UNKNOWN_SCORE
        private val durationClose = durationDifference != null && durationDifference <= MATCH_CLOSE_DURATION_SEC

        fun rejected(): Boolean {
            if (durationDifference != null && durationDifference > MATCH_REJECT_DURATION_SEC) return true
            if (durationClose) return false
            val wrongTitle = titleKnown && titleScore < MATCH_REJECT_TITLE
            val wrongArtist = artistKnown && artistScore < MATCH_REJECT_ARTIST && titleScore < MATCH_STRONG_TITLE
            return wrongTitle || wrongArtist
        }

        fun strong(): Boolean {
            val artistMatches = !artistKnown || artistScore >= MATCH_STRONG_ARTIST
            val durationMatches = durationDifference != null && durationDifference <= MATCH_STRONG_DURATION_SEC
            return titleScore >= MATCH_STRONG_TITLE && artistMatches && durationMatches && !versionMismatch
        }
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
            .replace(featuringSuffixRegex, " ")
            .replace(officialMetadataRegex, " ")
        return Normalizer.normalize(withoutFeaturing, Normalizer.Form.NFD)
            .replace(combiningMarksRegex, "")
            .lowercase(Locale.ROOT)
            .replace("&", " and ")
            .replace(nonWordRegex, " ")
            .replace(whitespaceRegex, " ")
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

private const val PLAUSIBLE_MATCH_CONFIDENCE_CAP = 79
private const val REJECTED_MATCH_CONFIDENCE_CAP = 42
private const val MATCH_STRONG_TITLE = 85
private const val MATCH_STRONG_ARTIST = 70
private const val MATCH_STRONG_DURATION_SEC = 5L
private const val MATCH_CLOSE_DURATION_SEC = 3L
private const val MATCH_REJECT_TITLE = 40
private const val MATCH_REJECT_ARTIST = 30
private const val MATCH_REJECT_DURATION_SEC = 60L
private const val UNKNOWN_SCORE = -1
private val LYRICS_WHITESPACE = Regex("""\s+""")
private val LYRICS_PUNCTUATION_SPACING = Regex("""\s+([,.;:!?])""")
private val LYRICS_YRC_MARKER = Regex("""(?m)^\[\d+,\d+]\(\d+,\d+,\d+\)""")
private val LYRICS_WORD_TIMED_MARKER = Regex("""(?m)^\[\d+,\d+]<(?:\d+,){2}\d+>""")
private val LYRICS_LRC_MARKER = Regex("""(?m)^\[\d{1,3}:\d{2}(?:[.:]\d{1,3})?]""")
private val LYRICS_QRC_SUFFIX_MARKER = Regex("""(?m)^\[\d+,\d+][^\n(]+\(\d+,\d+\)""")
private val LYRICS_TT_ELEMENT_MARKER = Regex("""<(?:[A-Za-z][\w.-]*:)?tt[\s>/]""", RegexOption.IGNORE_CASE)
private val LYRICS_SYLLABLE_LINE = Regex("""^\[(\d+),(\d+)](.*)$""")
private val XML_ENTITY_REGEX = Regex("""&(#x[0-9A-Fa-f]{1,6}|#\d{1,7}|[A-Za-z]{2,4});""")
private const val MAX_LYRIC_OFFSET_MS = 600_000L
private const val MAX_LYRIC_LINES = 10_000
private const val MAX_WORDS_PER_LINE = 512
private const val SYLLABLE_TIMING_AMBIGUITY_MS = 1_000L
private const val SYLLABLE_TIMING_TOLERANCE_MS = 250L
