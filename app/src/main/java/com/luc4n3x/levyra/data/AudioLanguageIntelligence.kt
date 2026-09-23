package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.player.offline.audioContentLengthFromUrl
import org.json.JSONObject
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.AudioTrackType
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale

enum class AudioTrackKind {
    ORIGINAL,
    PREFERRED_HUMAN,
    DEFAULT_AUDIO,
    UNSPECIFIED,
    SECONDARY,
    HUMAN_DUB,
    DESCRIPTIVE,
    AUTO_DUB
}

data class LevyraAudioTrackMetadata(
    val kind: AudioTrackKind,
    val language: String,
    val rawLanguage: String,
    val displayName: String,
    val isDefault: Boolean,
    val isAutoDubbed: Boolean,
    val isDescriptive: Boolean,
    val isExplicitOriginal: Boolean,
    val contentLength: Long,
    val xtags: String,
    val tier: Int,
    val tieBreakerBonus: Int
)

object AudioLanguageIntelligence {

    const val TIER_ORIGINAL_PREFERRED = 8
    const val TIER_PREFERRED_HUMAN = 7
    const val TIER_ORIGINAL = 6
    const val TIER_DEFAULT_AUDIO = 5
    const val TIER_UNSPECIFIED = 4
    const val TIER_SECONDARY = 3
    const val TIER_HUMAN_DUB = 2
    const val TIER_AUTO_DUB = 1
    const val TIER_DESCRIPTIVE = 0

    private const val PREFERRED_AUTO_DUB_BONUS = 50_000

    fun normalizeLanguage(code: String?): String {
        if (code.isNullOrBlank()) return ""
        val trimmed = code.trim().lowercase(Locale.ROOT)
        if (trimmed == "auto" || trimmed == "original" || trimmed == "default") return ""
        return trimmed.substringBefore('-').substringBefore('_').trim()
    }

    fun extractXtag(xtags: String?, key: String): String? {
        if (xtags.isNullOrBlank()) return null
        val helperVal = runCatching { YoutubeParsingHelper.extractXtagsValue(xtags, key) }.getOrNull()
        if (!helperVal.isNullOrBlank()) return helperVal

        val decoded = runCatching { URLDecoder.decode(xtags, StandardCharsets.UTF_8.name()) }.getOrDefault(xtags)
            .replace("%3D", "=", ignoreCase = true)
            .replace("%3A", ":", ignoreCase = true)
            .replace("%3B", ";", ignoreCase = true)
            .replace("%26", "&", ignoreCase = true)
        for (part in decoded.split(':', '&', ';')) {
            val sep = part.indexOf('=')
            if (sep > 0 && part.substring(0, sep).trim().equals(key, ignoreCase = true)) {
                return part.substring(sep + 1).trim()
            }
        }
        return null
    }

    private val XTAGS_REGEX = Regex("(?:[?&])xtags=([^&]+)")

    fun extractXtagsFromUrl(url: String): String {
        if (url.isBlank()) return ""
        return XTAGS_REGEX.find(url)?.groupValues?.getOrNull(1).orEmpty()
    }

    private fun isOriginalDisplayName(displayName: String): Boolean {
        if (displayName.isBlank()) return false
        val lower = displayName.lowercase(Locale.ROOT)
        return lower.contains("original") || lower.contains("originale")
    }

    fun parseFromFormat(
        format: JSONObject,
        resolvedUrl: String = "",
        preferredLanguage: String? = null
    ): LevyraAudioTrackMetadata {
        val audioTrack = format.optJSONObject("audioTrack")
        val rawUrl = resolvedUrl.ifBlank { format.optString("url") }
        val rawXtags = format.optString("xtags").ifBlank { extractXtagsFromUrl(rawUrl) }
        val acont = extractXtag(rawXtags, "acont").orEmpty().lowercase(Locale.ROOT)
        val langXtag = extractXtag(rawXtags, "lang").orEmpty()
        val audioTrackId = audioTrack?.optString("id").orEmpty()
        val displayName = audioTrack?.optString("displayName").orEmpty()
        val audioIsDefault = audioTrack?.optBoolean("audioIsDefault", false) ?: false
        val isAutoDubbedJson = audioTrack?.optBoolean("isAutoDubbed", false) ?: false

        val isAutoDubbed = isAutoDubbedJson || acont == "dubbed-auto"
        val isDescriptive = acont == "descriptive"
        val isExplicitOriginal = acont == "original" || isOriginalDisplayName(displayName)

        val rawLang = langXtag.ifBlank {
            if (audioTrackId.contains('.')) audioTrackId.substringBefore('.') else audioTrackId
        }
        val normalizedLang = normalizeLanguage(rawLang)
        val normalizedPref = normalizeLanguage(preferredLanguage)
        val matchesPreferred = normalizedPref.isNotEmpty() && normalizedLang == normalizedPref

        val clenFromUrl = audioContentLengthFromUrl(rawUrl)
        val declaredClen = format.optString("contentLength").toLongOrNull() ?: 0L
        val contentLength = if (clenFromUrl > 0L) clenFromUrl else declaredClen

        val kind: AudioTrackKind
        val tier: Int
        var tieBreaker = 0

        when {
            isAutoDubbed -> {
                kind = AudioTrackKind.AUTO_DUB
                tier = TIER_AUTO_DUB
                if (matchesPreferred) {
                    tieBreaker = PREFERRED_AUTO_DUB_BONUS
                }
            }
            isDescriptive -> {
                kind = AudioTrackKind.DESCRIPTIVE
                tier = TIER_DESCRIPTIVE
            }
            isExplicitOriginal && matchesPreferred -> {
                kind = AudioTrackKind.ORIGINAL
                tier = TIER_ORIGINAL_PREFERRED
            }
            matchesPreferred && !isAutoDubbed && !isDescriptive -> {
                kind = AudioTrackKind.PREFERRED_HUMAN
                tier = TIER_PREFERRED_HUMAN
            }
            isExplicitOriginal -> {
                kind = AudioTrackKind.ORIGINAL
                tier = TIER_ORIGINAL
            }
            audioIsDefault && !acont.startsWith("dub") -> {
                kind = AudioTrackKind.DEFAULT_AUDIO
                tier = TIER_DEFAULT_AUDIO
            }
            audioTrack == null && rawXtags.isBlank() -> {
                kind = AudioTrackKind.UNSPECIFIED
                tier = TIER_UNSPECIFIED
            }
            acont == "secondary" -> {
                kind = AudioTrackKind.SECONDARY
                tier = TIER_SECONDARY
            }
            acont == "dubbed" -> {
                kind = AudioTrackKind.HUMAN_DUB
                tier = TIER_HUMAN_DUB
            }
            else -> {
                kind = if (audioIsDefault) AudioTrackKind.DEFAULT_AUDIO else AudioTrackKind.HUMAN_DUB
                tier = if (audioIsDefault) TIER_DEFAULT_AUDIO else TIER_HUMAN_DUB
            }
        }

        return LevyraAudioTrackMetadata(
            kind = kind,
            language = normalizedLang,
            rawLanguage = rawLang,
            displayName = displayName,
            isDefault = audioIsDefault,
            isAutoDubbed = isAutoDubbed,
            isDescriptive = isDescriptive,
            isExplicitOriginal = isExplicitOriginal,
            contentLength = contentLength,
            xtags = rawXtags,
            tier = tier,
            tieBreakerBonus = tieBreaker
        )
    }

    fun parseFromExtractor(
        stream: AudioStream,
        preferredLanguage: String? = null
    ): LevyraAudioTrackMetadata {
        val content = stream.content.orEmpty()
        val rawXtags = extractXtagsFromUrl(content)
        val acontFromXtags = extractXtag(rawXtags, "acont").orEmpty().lowercase(Locale.ROOT)
        val langFromXtags = extractXtag(rawXtags, "lang").orEmpty()

        val isAutoGenerated = stream.itagItem?.isAutoGenerated == true || acontFromXtags == "dubbed-auto"
        val trackType = stream.audioTrackType
        val isDescriptive = trackType == AudioTrackType.DESCRIPTIVE || acontFromXtags == "descriptive"
        val isExplicitOriginal = trackType == AudioTrackType.ORIGINAL || acontFromXtags == "original" ||
            isOriginalDisplayName(stream.audioTrackName.orEmpty())

        val rawLang = langFromXtags.ifBlank {
            stream.audioLocale.orEmpty().ifBlank {
                val trackId = stream.audioTrackId.orEmpty()
                if (trackId.contains('.')) trackId.substringBefore('.') else trackId
            }
        }
        val normalizedLang = normalizeLanguage(rawLang)
        val normalizedPref = normalizeLanguage(preferredLanguage)
        val matchesPreferred = normalizedPref.isNotEmpty() && normalizedLang == normalizedPref

        val clenFromUrl = audioContentLengthFromUrl(content)
        val contentLength = if (clenFromUrl > 0L) clenFromUrl else 0L

        val kind: AudioTrackKind
        val tier: Int
        var tieBreaker = 0

        when {
            isAutoGenerated -> {
                kind = AudioTrackKind.AUTO_DUB
                tier = TIER_AUTO_DUB
                if (matchesPreferred) {
                    tieBreaker = PREFERRED_AUTO_DUB_BONUS
                }
            }
            isDescriptive -> {
                kind = AudioTrackKind.DESCRIPTIVE
                tier = TIER_DESCRIPTIVE
            }
            isExplicitOriginal && matchesPreferred -> {
                kind = AudioTrackKind.ORIGINAL
                tier = TIER_ORIGINAL_PREFERRED
            }
            matchesPreferred && !isAutoGenerated && !isDescriptive -> {
                kind = AudioTrackKind.PREFERRED_HUMAN
                tier = TIER_PREFERRED_HUMAN
            }
            isExplicitOriginal -> {
                kind = AudioTrackKind.ORIGINAL
                tier = TIER_ORIGINAL
            }
            trackType == null && rawXtags.isBlank() -> {
                kind = AudioTrackKind.UNSPECIFIED
                tier = TIER_UNSPECIFIED
            }
            trackType == AudioTrackType.SECONDARY || acontFromXtags == "secondary" -> {
                kind = AudioTrackKind.SECONDARY
                tier = TIER_SECONDARY
            }
            trackType == AudioTrackType.DUBBED || acontFromXtags == "dubbed" -> {
                kind = AudioTrackKind.HUMAN_DUB
                tier = TIER_HUMAN_DUB
            }
            else -> {
                kind = AudioTrackKind.UNSPECIFIED
                tier = TIER_UNSPECIFIED
            }
        }

        return LevyraAudioTrackMetadata(
            kind = kind,
            language = normalizedLang,
            rawLanguage = rawLang,
            displayName = stream.audioTrackName.orEmpty(),
            isDefault = false,
            isAutoDubbed = isAutoGenerated,
            isDescriptive = isDescriptive,
            isExplicitOriginal = isExplicitOriginal,
            contentLength = contentLength,
            xtags = rawXtags,
            tier = tier,
            tieBreakerBonus = tieBreaker
        )
    }

    internal fun parseFromSabr(
        candidate: SabrFormatCandidate,
        preferredLanguage: String? = null
    ): LevyraAudioTrackMetadata {
        val rawXtags = candidate.xtags.orEmpty()
        val acont = extractXtag(rawXtags, "acont").orEmpty().lowercase(Locale.ROOT)
        val langXtag = extractXtag(rawXtags, "lang").orEmpty()

        val isAutoDubbed = candidate.isAutoGeneratedAudio || acont == "dubbed-auto"
        val isDescriptive = candidate.audioTrackType == AudioTrackType.DESCRIPTIVE || acont == "descriptive"
        val isExplicitOriginal = candidate.audioTrackType == AudioTrackType.ORIGINAL || candidate.isOriginalAudio || acont == "original"

        val rawLang = langXtag.ifBlank { candidate.audioLocale.orEmpty() }
        val normalizedLang = normalizeLanguage(rawLang)
        val normalizedPref = normalizeLanguage(preferredLanguage)
        val matchesPreferred = normalizedPref.isNotEmpty() && normalizedLang == normalizedPref

        val kind: AudioTrackKind
        val tier: Int
        var tieBreaker = 0

        when {
            isAutoDubbed -> {
                kind = AudioTrackKind.AUTO_DUB
                tier = TIER_AUTO_DUB
                if (matchesPreferred) {
                    tieBreaker = PREFERRED_AUTO_DUB_BONUS
                }
            }
            isDescriptive -> {
                kind = AudioTrackKind.DESCRIPTIVE
                tier = TIER_DESCRIPTIVE
            }
            isExplicitOriginal && matchesPreferred -> {
                kind = AudioTrackKind.ORIGINAL
                tier = TIER_ORIGINAL_PREFERRED
            }
            matchesPreferred && !isAutoDubbed && !isDescriptive -> {
                kind = AudioTrackKind.PREFERRED_HUMAN
                tier = TIER_PREFERRED_HUMAN
            }
            isExplicitOriginal -> {
                kind = AudioTrackKind.ORIGINAL
                tier = TIER_ORIGINAL
            }
            candidate.audioTrackType == null && rawXtags.isBlank() -> {
                kind = AudioTrackKind.UNSPECIFIED
                tier = TIER_UNSPECIFIED
            }
            candidate.audioTrackType == AudioTrackType.SECONDARY || acont == "secondary" -> {
                kind = AudioTrackKind.SECONDARY
                tier = TIER_SECONDARY
            }
            candidate.audioTrackType == AudioTrackType.DUBBED || acont == "dubbed" -> {
                kind = AudioTrackKind.HUMAN_DUB
                tier = TIER_HUMAN_DUB
            }
            else -> {
                kind = AudioTrackKind.UNSPECIFIED
                tier = TIER_UNSPECIFIED
            }
        }

        return LevyraAudioTrackMetadata(
            kind = kind,
            language = normalizedLang,
            rawLanguage = rawLang,
            displayName = "",
            isDefault = false,
            isAutoDubbed = isAutoDubbed,
            isDescriptive = isDescriptive,
            isExplicitOriginal = isExplicitOriginal,
            contentLength = candidate.contentLength,
            xtags = rawXtags,
            tier = tier,
            tieBreakerBonus = tieBreaker
        )
    }
}
