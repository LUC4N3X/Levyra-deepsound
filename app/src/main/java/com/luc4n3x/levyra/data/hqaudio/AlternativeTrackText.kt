package com.luc4n3x.levyra.data.hqaudio

import java.text.Normalizer
import java.util.Locale

enum class TrackVersionMarker {
    LIVE,
    REMIX,
    ACOUSTIC,
    INSTRUMENTAL,
    KARAOKE,
    COVER,
    SPED_UP,
    SLOWED,
    REVERB,
    RADIO_EDIT,
    EXTENDED,
    DEMO,
    REMASTER,
    MONO,
    STEREO,
    EDIT,
    VERSION
}

enum class AlbumEdition {
    DELUXE,
    EXPANDED,
    ANNIVERSARY,
    REMASTERED,
    SINGLE,
    EP,
    SOUNDTRACK,
    OTHER_EDITION
}

data class TitleIdentity(
    val core: String,
    val fullNormalized: String,
    val markers: Set<TrackVersionMarker>,
    val versionLabels: Set<String>,
    val featuredArtists: Set<String>,
    val explicitHint: Boolean?
) {
    val versionSignature: Set<String>
        get() = markers.map { it.name }.toSet() + versionLabels.map { "label:$it" }
}

data class ArtistCredit(
    val primary: String,
    val names: Set<String>
)

data class AlbumIdentity(
    val core: String,
    val editions: Set<AlbumEdition>
) {
    val isBlank: Boolean
        get() = core.isBlank()
}

internal object AlternativeTrackText {
    private val htmlEntity = Regex("&(#[xX][0-9a-fA-F]{1,6}|#[0-9]{1,7}|[a-zA-Z]{2,8});")
    private val combiningMarks = Regex("\\p{Mn}+")
    private val apostrophes = Regex("['`´ʼ’‘]")
    private val nonWord = Regex("[^\\p{L}\\p{N}]+")
    private val whitespace = Regex("\\s+")
    private val bracketed = Regex("[(\\[{]([^)\\]}]*)[)\\]}]")
    private val dashSeparator = Regex("\\s[-–—]\\s")
    private val inlineFeaturing = Regex("\\s(?:feat\\.?|ft\\.?|featuring)\\s", RegexOption.IGNORE_CASE)
    private val featuringLead = Regex("^\\s*(?:feat\\.?|ft\\.?|featuring|with)\\s+", RegexOption.IGNORE_CASE)
    private val artistSeparators = Regex(
        "\\s*(?:,|&|\\+|/|;|\\sx\\s|\\sand\\s|\\se\\s|\\sy\\s|\\svs\\.?\\s|\\sfeat\\.?\\s|\\sft\\.?\\s|\\sfeaturing\\s|\\swith\\s)\\s*",
        RegexOption.IGNORE_CASE
    )
    private val leadingArticle = Regex("^the\\s+")
    private val yearToken = Regex("\\b(?:19|20)\\d{2}\\b")
    private val versionWord = Regex("(?:^|\\s)version(?:\\s|$)")

    private val namedEntities = mapOf(
        "amp" to "&",
        "quot" to "\"",
        "apos" to "'",
        "lt" to "<",
        "gt" to ">",
        "nbsp" to " ",
        "ndash" to "–",
        "mdash" to "—",
        "lsquo" to "‘",
        "rsquo" to "’",
        "ldquo" to "“",
        "rdquo" to "”",
        "hellip" to "…"
    )

    private class MarkerRule(pattern: String, val markers: Set<TrackVersionMarker>) {
        val phrase = Regex("(?:^|\\s)(?:$pattern)(?:\\s|$)")
    }

    private val standaloneMarkerRules = listOf(
        MarkerRule("sped up|speed up|spedup|nightcore", setOf(TrackVersionMarker.SPED_UP)),
        MarkerRule("slowed|slowed down", setOf(TrackVersionMarker.SLOWED)),
        MarkerRule("reverb|reverbed", setOf(TrackVersionMarker.REVERB)),
        MarkerRule("karaoke|in the style of|originally performed by", setOf(TrackVersionMarker.KARAOKE)),
        MarkerRule("instrumental|backing track", setOf(TrackVersionMarker.INSTRUMENTAL)),
        MarkerRule("remix|remixed|mashup|bootleg|rework|lofi|lo fi", setOf(TrackVersionMarker.REMIX)),
        MarkerRule("8d|8d audio", setOf(TrackVersionMarker.VERSION))
    )

    private val descriptorMarkerRules = standaloneMarkerRules + listOf(
        MarkerRule("live|live at|live from|in concert|live session|session|sessions", setOf(TrackVersionMarker.LIVE)),
        MarkerRule("unplugged", setOf(TrackVersionMarker.LIVE, TrackVersionMarker.ACOUSTIC)),
        MarkerRule("acoustic", setOf(TrackVersionMarker.ACOUSTIC)),
        MarkerRule("cover|tribute", setOf(TrackVersionMarker.COVER)),
        MarkerRule("radio edit|radio version|single edit|radio mix", setOf(TrackVersionMarker.RADIO_EDIT, TrackVersionMarker.EDIT)),
        MarkerRule("edit|edited", setOf(TrackVersionMarker.EDIT)),
        MarkerRule("extended|club mix", setOf(TrackVersionMarker.EXTENDED)),
        MarkerRule("demo", setOf(TrackVersionMarker.DEMO)),
        MarkerRule("remaster|remastered|remastering", setOf(TrackVersionMarker.REMASTER)),
        MarkerRule("mono", setOf(TrackVersionMarker.MONO)),
        MarkerRule("stereo", setOf(TrackVersionMarker.STEREO)),
        MarkerRule("mix|dub|vip", setOf(TrackVersionMarker.REMIX))
    )

    private val neutralDescriptorPhrases = setOf(
        "original",
        "original mix",
        "original version",
        "album version",
        "official",
        "official audio",
        "official video",
        "official music video",
        "audio",
        "video",
        "lyrics",
        "lyric video",
        "visualizer",
        "visualiser",
        "explicit",
        "explicit version",
        "clean",
        "clean version"
    )

    private val featuringPrefixes = listOf("feat ", "ft ", "featuring ", "with ")
    private val neutralPrefixes = listOf("from ", "prod ", "produced by ")

    private class EditionRule(pattern: String, val edition: AlbumEdition) {
        val phrase = Regex("(?:^|\\s)(?:$pattern)(?:\\s|$)")
    }

    private val editionRules = listOf(
        EditionRule("super deluxe|deluxe", AlbumEdition.DELUXE),
        EditionRule("expanded|bonus track version|bonus tracks", AlbumEdition.EXPANDED),
        EditionRule("anniversary", AlbumEdition.ANNIVERSARY),
        EditionRule("remaster|remastered", AlbumEdition.REMASTERED),
        EditionRule("single", AlbumEdition.SINGLE),
        EditionRule("ep", AlbumEdition.EP),
        EditionRule("soundtrack|ost|motion picture", AlbumEdition.SOUNDTRACK),
        EditionRule("edition|version", AlbumEdition.OTHER_EDITION)
    )

    private sealed interface Descriptor {
        data class Featuring(val names: List<String>) : Descriptor
        data class Neutral(val explicitHint: Boolean?) : Descriptor
        data class Version(val markers: Set<TrackVersionMarker>, val label: String?) : Descriptor
        data object Core : Descriptor
    }

    fun decodeHtmlEntities(value: String): String {
        if (!value.contains('&')) return value
        var current = value
        repeat(2) {
            current = htmlEntity.replace(current) { match ->
                val entity = match.groupValues[1]
                when {
                    entity.startsWith("#x") || entity.startsWith("#X") ->
                        entity.substring(2).toIntOrNull(16)?.let(::codePointText) ?: match.value
                    entity.startsWith("#") ->
                        entity.substring(1).toIntOrNull()?.let(::codePointText) ?: match.value
                    else -> namedEntities[entity.lowercase(Locale.ROOT)] ?: match.value
                }
            }
        }
        return current
    }

    private fun codePointText(codePoint: Int): String? =
        if (Character.isValidCodePoint(codePoint)) String(Character.toChars(codePoint)) else null

    private fun prepare(value: String): String =
        Normalizer.normalize(decodeHtmlEntities(value), Normalizer.Form.NFKC).trim()

    fun normalize(value: String): String {
        val lowered = prepare(value).lowercase(Locale.ROOT)
        val stripped = Normalizer.normalize(lowered, Normalizer.Form.NFD).replace(combiningMarks, "")
        return stripped
            .replace(apostrophes, "")
            .replace("&", " and ")
            .replace(nonWord, " ")
            .replace(whitespace, " ")
            .trim()
    }

    fun normalizeArtist(value: String): String = normalize(value).replace(leadingArticle, "")

    fun artistNames(raw: String): List<String> =
        decodeHtmlEntities(raw)
            .split(artistSeparators)
            .map(::normalizeArtist)
            .filter { it.isNotBlank() }
            .distinct()

    fun artistCredit(raw: String): ArtistCredit {
        val names = artistNames(raw)
        return ArtistCredit(primary = names.firstOrNull().orEmpty(), names = names.toSet())
    }

    fun title(raw: String): TitleIdentity {
        val decoded = prepare(raw)
        val markers = linkedSetOf<TrackVersionMarker>()
        val labels = linkedSetOf<String>()
        val featured = linkedSetOf<String>()
        var explicitHint: Boolean? = null

        fun absorb(text: String): Boolean = when (val descriptor = classifyTitleDescriptor(text)) {
            is Descriptor.Featuring -> {
                featured += descriptor.names
                true
            }
            is Descriptor.Neutral -> {
                descriptor.explicitHint?.let { explicitHint = it }
                true
            }
            is Descriptor.Version -> {
                markers += descriptor.markers
                descriptor.label?.let(labels::add)
                true
            }
            Descriptor.Core -> false
        }

        var remaining = decoded
        bracketed.findAll(decoded).forEach { match ->
            if (absorb(match.groupValues[1])) remaining = remaining.replaceFirst(match.value, " ")
        }
        val segments = remaining.split(dashSeparator)
        val coreSegments = mutableListOf(segments.first())
        segments.drop(1).forEach { segment -> if (!absorb(segment)) coreSegments += segment }
        var coreText = coreSegments.joinToString(" ")
        inlineFeaturing.find(coreText)?.let { match ->
            featured += artistNames(coreText.substring(match.range.last + 1))
            coreText = coreText.substring(0, match.range.first)
        }
        val core = normalize(coreText)
        standaloneMarkerRules.forEach { rule ->
            if (rule.phrase.containsMatchIn(core)) markers += rule.markers
        }
        return TitleIdentity(
            core = core,
            fullNormalized = normalize(decoded),
            markers = markers,
            versionLabels = labels,
            featuredArtists = featured,
            explicitHint = explicitHint
        )
    }

    private fun classifyTitleDescriptor(text: String): Descriptor {
        val normalized = normalize(text)
        if (normalized.isBlank()) return Descriptor.Neutral(null)
        if (featuringPrefixes.any(normalized::startsWith)) {
            return Descriptor.Featuring(artistNames(text.replace(featuringLead, "")))
        }
        if (neutralPrefixes.any(normalized::startsWith)) return Descriptor.Neutral(null)
        if (normalized in neutralDescriptorPhrases) return Descriptor.Neutral(explicitHintOf(normalized))
        val reduced = normalized.replace(yearToken, " ").replace(whitespace, " ").trim()
        val markers = linkedSetOf<TrackVersionMarker>()
        descriptorMarkerRules.forEach { rule ->
            if (rule.phrase.containsMatchIn(reduced)) markers += rule.markers
        }
        if (markers.isNotEmpty()) return Descriptor.Version(markers, null)
        if (versionWord.containsMatchIn(reduced)) return Descriptor.Version(setOf(TrackVersionMarker.VERSION), reduced)
        return Descriptor.Core
    }

    private fun explicitHintOf(normalized: String): Boolean? = when {
        normalized.contains("explicit") -> true
        normalized.contains("clean") -> false
        else -> null
    }

    fun album(raw: String): AlbumIdentity {
        val decoded = prepare(raw)
        val editions = linkedSetOf<AlbumEdition>()

        fun absorb(text: String): Boolean {
            val found = albumDescriptorEditions(text) ?: return false
            editions += found
            return true
        }

        var remaining = decoded
        bracketed.findAll(decoded).forEach { match ->
            if (absorb(match.groupValues[1])) remaining = remaining.replaceFirst(match.value, " ")
        }
        val segments = remaining.split(dashSeparator)
        val kept = mutableListOf(segments.first())
        segments.drop(1).forEach { segment -> if (!absorb(segment)) kept += segment }
        var core = normalize(kept.joinToString(" "))
        listOf(" ep" to AlbumEdition.EP, " single" to AlbumEdition.SINGLE).forEach { (suffix, edition) ->
            if (core.endsWith(suffix)) {
                editions += edition
                core = core.removeSuffix(suffix).trim()
            }
        }
        return AlbumIdentity(core = core, editions = editions)
    }

    private fun albumDescriptorEditions(text: String): Set<AlbumEdition>? {
        val normalized = normalize(text)
        if (normalized.isBlank()) return emptySet()
        val reduced = normalized.replace(yearToken, " ").replace(whitespace, " ").trim()
        val matched = editionRules.filter { it.phrase.containsMatchIn(reduced) }.map { it.edition }
        if (matched.isNotEmpty()) {
            val specific = matched.filter { it != AlbumEdition.OTHER_EDITION }
            return specific.ifEmpty { matched }.toSet()
        }
        if (featuringPrefixes.any(normalized::startsWith) || neutralPrefixes.any(normalized::startsWith)) return emptySet()
        return null
    }
}
