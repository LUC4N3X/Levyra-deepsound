package com.luc4n3x.levyra.data

import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Inflater
import kotlin.math.abs
import org.json.JSONObject

internal data class KugouLyricsCandidate(
    val id: String,
    val accessKey: String,
    val title: String,
    val artist: String,
    val durationSec: Long
)

private const val KUGOU_DURATION_TOLERANCE_SEC = 4L
private const val KUGOU_MAX_LOOKUPS = 2
private const val KUGOU_WORD_CONFIDENCE = 95
private const val KUGOU_LINE_CONFIDENCE = 86
private const val KUGOU_MAX_LYRICS_BYTES = 1_048_576
private val KUGOU_KRC_KEY = byteArrayOf(64, 71, 97, 119, 94, 50, 116, 71, 81, 54, 49, 45, -50, -46, 110, 105)

internal fun parseKugouCandidates(body: String): List<KugouLyricsCandidate> {
    val candidates = JSONObject(body).optJSONArray("candidates") ?: return emptyList()
    return (0 until candidates.length()).mapNotNull { index ->
        val item = candidates.optJSONObject(index) ?: return@mapNotNull null
        val id = item.optString("id").trim()
        val accessKey = item.optString("accesskey").trim()
        if (id.isBlank() || accessKey.isBlank()) return@mapNotNull null
        KugouLyricsCandidate(
            id = id,
            accessKey = accessKey,
            title = item.optString("song").trim(),
            artist = item.optString("singer").trim(),
            durationSec = item.optLong("duration") / 1_000L
        )
    }
}

internal fun kugouShortlist(candidates: List<KugouLyricsCandidate>, durationSec: Long): List<KugouLyricsCandidate> {
    if (durationSec <= 0L) return candidates.take(1)
    return candidates
        .filter { it.durationSec <= 0L || abs(it.durationSec - durationSec) <= KUGOU_DURATION_TOLERANCE_SEC }
        .take(KUGOU_MAX_LOOKUPS)
}

internal fun parseKugouDownload(body: String, keepTranslation: Boolean = false): LyricsRepository.LyricsResult? {
    val content = JSONObject(body).optString("content").trim()
    if (content.isBlank()) return null
    val krc = decryptKugouKrc(Base64.getDecoder().decode(content)) ?: return null
    val parsed = UnifiedLyricsParser.parse(krc)
    val lines = if (keepTranslation) parsed else parsed.map { line -> if (line.translated.isBlank()) line else line.copy(translated = "") }
    if (lines.none { it.text.isNotBlank() }) return null
    val wordSynced = lines.any { it.words.isNotEmpty() }
    return LyricsRepository.LyricsResult(
        synced = true,
        lines = lines,
        provider = "Kugou",
        confidence = if (wordSynced) KUGOU_WORD_CONFIDENCE else KUGOU_LINE_CONFIDENCE,
        cached = false
    )
}

internal fun decryptKugouKrc(payload: ByteArray): String? {
    if (payload.size <= 4 || String(payload, 0, 4, Charsets.US_ASCII) != "krc1") return null
    val compressed = ByteArray(payload.size - 4) { index ->
        (payload[index + 4].toInt() xor KUGOU_KRC_KEY[index % KUGOU_KRC_KEY.size].toInt()).toByte()
    }
    val inflater = Inflater()
    return try {
        inflater.setInput(compressed)
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        while (!inflater.finished()) {
            val read = inflater.inflate(buffer)
            if (read == 0 && (inflater.needsInput() || inflater.needsDictionary())) return null
            output.write(buffer, 0, read)
            if (output.size() > KUGOU_MAX_LYRICS_BYTES) return null
        }
        output.toString(Charsets.UTF_8.name()).removePrefix("\uFEFF")
    } catch (_: java.util.zip.DataFormatException) {
        null
    } finally {
        inflater.end()
    }
}
