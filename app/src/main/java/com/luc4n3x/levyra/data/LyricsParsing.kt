package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricWord
import java.io.StringReader
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.absoluteValue
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

object LrcLyricsParser {
    private val timestampRegex = Regex("\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?]")

    fun parse(lrc: String): List<LyricLine> {
        val raw = mutableListOf<Pair<Long, String>>()
        lrc.lineSequence().forEach { line ->
            val matches = timestampRegex.findAll(line).toList()
            if (matches.isEmpty()) return@forEach
            val text = line.substring(matches.last().range.last + 1).trim()
            if (text.isBlank()) return@forEach
            matches.forEach { match ->
                raw += timestampMs(match.groupValues) to text
            }
        }

        val sorted = raw.sortedBy { it.first }
        return sorted.mapIndexed { index, (start, text) ->
            val end = sorted.getOrNull(index + 1)?.first?.minus(80L)?.coerceAtLeast(start + 800L) ?: (start + 6_000L)
            LyricLine(startMs = start, endMs = end, text = text, translated = "")
        }
    }

    private fun timestampMs(groups: List<String>): Long {
        val min = groups[1].toLongOrNull() ?: 0L
        val sec = groups[2].toLongOrNull() ?: 0L
        val frac = groups[3]
        val ms = when (frac.length) {
            1 -> frac.toLongOrNull()?.times(100L) ?: 0L
            2 -> frac.toLongOrNull()?.times(10L) ?: 0L
            3 -> frac.toLongOrNull() ?: 0L
            else -> 0L
        }
        return min * 60_000L + sec * 1_000L + ms
    }
}

object TtmlLyricsParser {
    fun parse(ttml: String): List<LyricLine> {
        val document = runCatching {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = false
                runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
                runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
                runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            }
            factory.newDocumentBuilder().parse(InputSource(StringReader(ttml)))
        }.getOrNull() ?: return emptyList()

        val nodes = document.getElementsByTagName("p")
        val lines = ArrayList<LyricLine>()
        for (i in 0 until nodes.length) {
            val element = nodes.item(i) as? Element ?: continue
            val start = element.timeAttribute("begin") ?: continue
            val words = element.wordSpans()
            val text = if (words.isNotEmpty()) {
                words.joinToString(" ") { it.text }.replace(Regex("\\s+"), " ").trim()
            } else {
                element.textContent.orEmpty().replace(Regex("\\s+"), " ").trim()
            }
            if (text.isBlank()) continue
            val end = element.timeAttribute("end")
                ?: element.timeAttribute("dur")?.let { start + it }
                ?: words.lastOrNull()?.endMs
                ?: (start + 6_000L)
            lines += LyricLine(startMs = start, endMs = end.coerceAtLeast(start + 500L), text = text, translated = "", words = words)
        }
        return lines.sortedBy { it.startMs }
    }

    private fun Element.wordSpans(): List<LyricWord> {
        val out = ArrayList<LyricWord>()
        val spans = getElementsByTagName("span")
        for (i in 0 until spans.length) {
            val span = spans.item(i) as? Element ?: continue
            val role = span.getAttribute("ttm:role").ifBlank { span.getAttribute("role") }
            if (role == "x-translation" || role == "x-roman" || role == "x-bg") continue
            val start = span.timeAttribute("begin") ?: continue
            val end = span.timeAttribute("end") ?: continue
            val text = span.directText().trim()
            if (text.isNotBlank()) {
                out += LyricWord(startMs = start, endMs = end.coerceAtLeast(start), text = text)
            }
        }
        return out
    }

    private fun Element.directText(): String = buildString {
        val children = childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child.nodeType == Node.TEXT_NODE || child.nodeType == Node.CDATA_SECTION_NODE) {
                append(child.nodeValue)
            }
        }
    }.replace(Regex("\\s+"), " ")

    private fun Element.timeAttribute(name: String): Long? = getAttribute(name).takeIf { it.isNotBlank() }?.let(::parseTimeMs)

    private fun parseTimeMs(raw: String): Long? {
        val value = raw.trim()
        if (value.endsWith("ms")) return value.removeSuffix("ms").toDoubleOrNull()?.toLong()
        if (value.endsWith("s")) return value.removeSuffix("s").toDoubleOrNull()?.times(1_000L)?.toLong()
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
}

object LyricsResultRanker {
    fun best(candidates: List<LyricsCandidate>, request: LyricsRequest): LyricsRepository.LyricsResult? {
        return candidates
            .map { candidate -> candidate.result.copy(confidence = score(candidate, request)) }
            .filter { it.lines.isNotEmpty() && it.confidence >= 42 }
            .sortedWith(
                compareByDescending<LyricsRepository.LyricsResult> { it.synced }
                    .thenByDescending { it.lines.any { line -> line.words.isNotEmpty() } }
                    .thenByDescending { it.confidence }
                    .thenByDescending { it.lines.size }
            )
            .firstOrNull()
    }

    fun score(candidate: LyricsCandidate, request: LyricsRequest): Int {
        val titleScore = similarity(candidate.title.cleanComparable(), request.title.cleanComparable())
        val artistScore = similarity(candidate.artist.cleanComparable(), request.artist.cleanComparable())
        val durationScore = when {
            request.durationSec <= 0L || candidate.durationSec <= 0L -> 12
            (candidate.durationSec - request.durationSec).absoluteValue <= 3L -> 18
            (candidate.durationSec - request.durationSec).absoluteValue <= 8L -> 12
            (candidate.durationSec - request.durationSec).absoluteValue <= 16L -> 6
            else -> -12
        }
        val syncScore = if (candidate.result.synced) 18 else 4
        val wordScore = if (candidate.result.lines.any { it.words.isNotEmpty() }) 8 else 0
        val sizeScore = candidate.result.lines.size.coerceAtMost(40) / 2
        return (titleScore * 36 / 100 + artistScore * 26 / 100 + durationScore + syncScore + wordScore + sizeScore).coerceIn(0, 100)
    }

    private fun similarity(left: String, right: String): Int {
        if (left.isBlank() || right.isBlank()) return 0
        if (left == right) return 100
        if (left.contains(right) || right.contains(left)) return 84
        val a = left.split(" ").filter { it.isNotBlank() }.toSet()
        val b = right.split(" ").filter { it.isNotBlank() }.toSet()
        if (a.isEmpty() || b.isEmpty()) return 0
        val intersection = a.intersect(b).size
        val union = a.union(b).size.coerceAtLeast(1)
        return (intersection * 100 / union).coerceIn(0, 100)
    }

    private fun String.cleanComparable(): String = lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N} ]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
