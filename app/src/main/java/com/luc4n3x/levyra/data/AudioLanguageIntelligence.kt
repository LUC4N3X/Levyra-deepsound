package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.PlaybackStreamKind
import com.luc4n3x.levyra.domain.ResolvedPlaybackManifest
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

    private const val LANGUAGE_EXACT_MATCH_BONUS = 2
    private const val LANGUAGE_BASE_MATCH_BONUS = 1

    fun normalizeLanguage(code: String?): String {
        if (code.isNullOrBlank()) return ""
        val trimmed = code.trim().lowercase(Locale.ROOT)
        if (trimmed == "auto" || trimmed == "original" || trimmed == "default") return ""
        val parts = trimmed
            .replace('_', '-')
            .split('-')
            .map(String::trim)
            .filter(String::isNotBlank)
        if (parts.isEmpty()) return ""
        val canonicalBase = when (parts.first()) {
            "iw" -> "he"
            "in" -> "id"
            "tl" -> "fil"
            else -> parts.first()
        }
        return (listOf(canonicalBase) + parts.drop(1)).joinToString("-")
    }

    private fun preferredLanguageMatchBonus(language: String, preferredLanguage: String): Int {
        if (language.isBlank() || preferredLanguage.isBlank()) return 0
        if (language == preferredLanguage) return LANGUAGE_EXACT_MATCH_BONUS
        return if (language.substringBefore('-') == preferredLanguage.substringBefore('-')) {
            LANGUAGE_BASE_MATCH_BONUS
        } else {
            0
        }
    }

    internal fun canReuseResolvedLanguage(language: String?, preferredLanguage: String?): Boolean {
        val normalizedPreferred = normalizeLanguage(preferredLanguage)
        if (normalizedPreferred.isBlank()) return true
        val normalizedLanguage = normalizeLanguage(language)
        if (normalizedLanguage.isBlank()) return false
        if (normalizedLanguage == normalizedPreferred) return true
        if ('-' in normalizedPreferred) return false
        return normalizedLanguage.substringBefore('-') == normalizedPreferred
    }

    internal fun isLanguageBlindFallback(manifest: ResolvedPlaybackManifest): Boolean =
        manifest.isMuxed ||
            manifest.streams.any { descriptor ->
                descriptor.selected && descriptor.kind == PlaybackStreamKind.HLS
            }

    internal fun canReuseProvidedPlayback(
        manifest: ResolvedPlaybackManifest?,
        streamUrl: String,
        preferredLanguage: String,
        languageRevision: Long
    ): Boolean {
        if (manifest?.alternativeSource != null) return true
        if (preferredLanguage.isNotBlank() && manifest?.let(::isLanguageBlindFallback) == true) return false
        manifest?.provenance?.preferredAudioLanguage?.let { return it == preferredLanguage }
        if (preferredLanguage.isBlank()) return languageRevision == 0L
        val streamLanguage = extractXtag(extractXtagsFromUrl(streamUrl), "lang")
        return canReuseResolvedLanguage(streamLanguage, preferredLanguage)
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
        XTAGS_REGEX.find(url)?.groupValues?.getOrNull(1)?.let { return it }
        val decodedUrl = runCatching {
            URLDecoder.decode(url, StandardCharsets.UTF_8.name())
        }.getOrDefault(url)
        return XTAGS_REGEX.find(decodedUrl)?.groupValues?.getOrNull(1).orEmpty()
    }

    private fun isOriginalDisplayName(displayName: String): Boolean {
        if (displayName.isBlank()) return false
        val lower = displayName.lowercase(Locale.ROOT)
        return lower.contains("original") || lower.contains("originale")
    }

    private fun extractBaseLanguageCode(rawId: String): String {
        val dotIndex = rawId.indexOf('.')
        return if (dotIndex >= 0) rawId.substring(0, dotIndex) else rawId
    }

    private data class TrackClassification(
        val kind: AudioTrackKind,
        val tier: Int,
        val tieBreakerBonus: Int = 0
    )

    private fun resolvePrimaryClassification(
        isAutoDubbed: Boolean,
        isDescriptive: Boolean,
        isExplicitOriginal: Boolean,
        preferredMatchBonus: Int
    ): TrackClassification? {
        val matchesPreferred = preferredMatchBonus > 0
        return when {
            isAutoDubbed -> {
                TrackClassification(AudioTrackKind.AUTO_DUB, TIER_AUTO_DUB, preferredMatchBonus)
            }
            isDescriptive -> {
                TrackClassification(AudioTrackKind.DESCRIPTIVE, TIER_DESCRIPTIVE)
            }
            isExplicitOriginal && matchesPreferred -> {
                TrackClassification(AudioTrackKind.ORIGINAL, TIER_ORIGINAL_PREFERRED, preferredMatchBonus)
            }
            matchesPreferred -> {
                TrackClassification(AudioTrackKind.PREFERRED_HUMAN, TIER_PREFERRED_HUMAN, preferredMatchBonus)
            }
            isExplicitOriginal -> {
                TrackClassification(AudioTrackKind.ORIGINAL, TIER_ORIGINAL)
            }
            else -> null
        }
    }

    private fun classifyFormatFallback(
        audioIsDefault: Boolean,
        acont: String,
        hasAudioTrack: Boolean
    ): TrackClassification {
        return when {
            audioIsDefault && !acont.startsWith("dub") -> {
                TrackClassification(AudioTrackKind.DEFAULT_AUDIO, TIER_DEFAULT_AUDIO)
            }
            !hasAudioTrack && acont.isBlank() -> {
                TrackClassification(AudioTrackKind.UNSPECIFIED, TIER_UNSPECIFIED)
            }
            acont == "secondary" -> {
                TrackClassification(AudioTrackKind.SECONDARY, TIER_SECONDARY)
            }
            acont == "dubbed" -> {
                TrackClassification(AudioTrackKind.HUMAN_DUB, TIER_HUMAN_DUB)
            }
            audioIsDefault -> {
                TrackClassification(AudioTrackKind.DEFAULT_AUDIO, TIER_DEFAULT_AUDIO)
            }
            else -> {
                TrackClassification(AudioTrackKind.HUMAN_DUB, TIER_HUMAN_DUB)
            }
        }
    }

    private fun classifyExtractorFallback(
        trackType: AudioTrackType?,
        acont: String,
        hasXtags: Boolean
    ): TrackClassification {
        return when {
            trackType == null && !hasXtags -> {
                TrackClassification(AudioTrackKind.UNSPECIFIED, TIER_UNSPECIFIED)
            }
            trackType == AudioTrackType.SECONDARY || acont == "secondary" -> {
                TrackClassification(AudioTrackKind.SECONDARY, TIER_SECONDARY)
            }
            trackType == AudioTrackType.DUBBED || acont == "dubbed" -> {
                TrackClassification(AudioTrackKind.HUMAN_DUB, TIER_HUMAN_DUB)
            }
            else -> {
                TrackClassification(AudioTrackKind.UNSPECIFIED, TIER_UNSPECIFIED)
            }
        }
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
        val audioIsDefault = audioTrack?.optBoolean("audioIsDefault", false) == true
        val isAutoDubbedJson = audioTrack?.optBoolean("isAutoDubbed", false) == true

        val isAutoDubbed = isAutoDubbedJson || acont == "dubbed-auto"
        val isDescriptive = acont == "descriptive"
        val isExplicitOriginal = acont == "original" || isOriginalDisplayName(displayName)

        val rawLang = langXtag.ifBlank { extractBaseLanguageCode(audioTrackId) }
        val normalizedLang = normalizeLanguage(rawLang)
        val normalizedPref = normalizeLanguage(preferredLanguage)
        val preferredMatchBonus = preferredLanguageMatchBonus(normalizedLang, normalizedPref)

        val clenFromUrl = audioContentLengthFromUrl(rawUrl)
        val declaredClen = format.optString("contentLength").toLongOrNull() ?: 0L
        val contentLength = if (clenFromUrl > 0L) clenFromUrl else declaredClen

        val classification = resolvePrimaryClassification(
            isAutoDubbed = isAutoDubbed,
            isDescriptive = isDescriptive,
            isExplicitOriginal = isExplicitOriginal,
            preferredMatchBonus = preferredMatchBonus
        ) ?: classifyFormatFallback(
            audioIsDefault = audioIsDefault,
            acont = acont,
            hasAudioTrack = audioTrack != null
        )

        return LevyraAudioTrackMetadata(
            kind = classification.kind,
            language = normalizedLang,
            rawLanguage = rawLang,
            displayName = displayName,
            isDefault = audioIsDefault,
            isAutoDubbed = isAutoDubbed,
            isDescriptive = isDescriptive,
            isExplicitOriginal = isExplicitOriginal,
            contentLength = contentLength,
            xtags = rawXtags,
            tier = classification.tier,
            tieBreakerBonus = classification.tieBreakerBonus
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
                extractBaseLanguageCode(stream.audioTrackId.orEmpty())
            }
        }
        val normalizedLang = normalizeLanguage(rawLang)
        val normalizedPref = normalizeLanguage(preferredLanguage)
        val preferredMatchBonus = preferredLanguageMatchBonus(normalizedLang, normalizedPref)

        val clenFromUrl = audioContentLengthFromUrl(content)
        val contentLength = if (clenFromUrl > 0L) clenFromUrl else 0L

        val classification = resolvePrimaryClassification(
            isAutoDubbed = isAutoGenerated,
            isDescriptive = isDescriptive,
            isExplicitOriginal = isExplicitOriginal,
            preferredMatchBonus = preferredMatchBonus
        ) ?: classifyExtractorFallback(
            trackType = trackType,
            acont = acontFromXtags,
            hasXtags = rawXtags.isNotBlank()
        )

        return LevyraAudioTrackMetadata(
            kind = classification.kind,
            language = normalizedLang,
            rawLanguage = rawLang,
            displayName = stream.audioTrackName.orEmpty(),
            isDefault = false,
            isAutoDubbed = isAutoGenerated,
            isDescriptive = isDescriptive,
            isExplicitOriginal = isExplicitOriginal,
            contentLength = contentLength,
            xtags = rawXtags,
            tier = classification.tier,
            tieBreakerBonus = classification.tieBreakerBonus
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
        val preferredMatchBonus = preferredLanguageMatchBonus(normalizedLang, normalizedPref)

        val classification = resolvePrimaryClassification(
            isAutoDubbed = isAutoDubbed,
            isDescriptive = isDescriptive,
            isExplicitOriginal = isExplicitOriginal,
            preferredMatchBonus = preferredMatchBonus
        ) ?: classifyExtractorFallback(
            trackType = candidate.audioTrackType,
            acont = acont,
            hasXtags = rawXtags.isNotBlank()
        )

        return LevyraAudioTrackMetadata(
            kind = classification.kind,
            language = normalizedLang,
            rawLanguage = rawLang,
            displayName = "",
            isDefault = false,
            isAutoDubbed = isAutoDubbed,
            isDescriptive = isDescriptive,
            isExplicitOriginal = isExplicitOriginal,
            contentLength = candidate.contentLength,
            xtags = rawXtags,
            tier = classification.tier,
            tieBreakerBonus = classification.tieBreakerBonus
        )
    }
}
