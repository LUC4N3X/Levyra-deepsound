package com.luc4n3x.levyra.data.locallibrary

import com.luc4n3x.levyra.data.local.LocalMediaMatchRow
import com.luc4n3x.levyra.domain.Track
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs

internal data class LocalTitleIdentity(val base: String, val versions: Set<String>)

internal class LocalAudioMatchIndex(rows: List<LocalMediaMatchRow>) {
    private val byTitle: Map<String, List<LocalAudioCandidate>>
    private val byLevyraTrackId: Map<String, List<LocalMediaMatchRow>>

    init {
        val candidates = rows.mapNotNull(::localAudioCandidate)
        byTitle = candidates.groupBy { it.title.base }
        byLevyraTrackId = rows
            .filter { it.levyraTrackId.isNotBlank() && it.contentUri.isNotBlank() }
            .groupBy { it.levyraTrackId }
    }

    fun find(track: Track): List<LocalMediaMatchRow> {
        byLevyraTrackId[track.id.trim()]?.let { identified ->
            return identified.sortedWith(compareByDescending<LocalMediaMatchRow> { it.bitrate }.thenBy { it.id })
        }
        val query = onlineAudioQuery(track) ?: return emptyList()
        return byTitle[query.title.base].orEmpty()
            .mapNotNull { candidate -> localAudioMatch(query, candidate) }
            .sortedWith(
                compareByDescending<LocalAudioMatch> { it.albumMatches }
                    .thenBy { it.durationDeltaMs ?: Long.MAX_VALUE }
                    .thenByDescending { it.row.bitrate }
                    .thenBy { it.row.id }
            )
            .map { it.row }
    }
}

internal fun findLocalAudioMatch(track: Track, rows: List<LocalMediaMatchRow>): LocalMediaMatchRow? =
    LocalAudioMatchIndex(rows).find(track).firstOrNull()

private class LocalAudioCandidate(
    val row: LocalMediaMatchRow,
    val title: LocalTitleIdentity,
    val artists: Set<String>,
    val primaryArtist: String,
    val album: String
)

private class OnlineAudioQuery(
    val title: LocalTitleIdentity,
    val artists: Set<String>,
    val primaryArtist: String,
    val album: String,
    val durationMs: Long
)

private class LocalAudioMatch(
    val row: LocalMediaMatchRow,
    val albumMatches: Boolean,
    val durationDeltaMs: Long?
)

private fun localAudioCandidate(row: LocalMediaMatchRow): LocalAudioCandidate? {
    if (row.contentUri.isBlank()) return null
    val taggedArtists = matchArtistNames(row.artist)
    val albumArtists = matchArtistNames(row.albumArtist)
    var rawTitle = row.title
    var derivedArtists = emptyList<String>()
    if (taggedArtists.isEmpty() && albumArtists.isEmpty()) {
        rawTitle = rawTitle.replace(LEADING_TRACK_NUMBER, "")
        FILENAME_ARTIST_TITLE.matchEntire(rawTitle.trim())?.let { parts ->
            derivedArtists = matchArtistNames(parts.groupValues[1])
            rawTitle = parts.groupValues[2]
        }
    }
    val title = localTitleIdentity(rawTitle)
    if (title.base.isEmpty()) return null
    val primary = (taggedArtists.firstOrNull() ?: derivedArtists.firstOrNull() ?: albumArtists.firstOrNull()).orEmpty()
    return LocalAudioCandidate(
        row = row,
        title = title,
        artists = taggedArtists.ifEmpty { derivedArtists + albumArtists }.toSet(),
        primaryArtist = primary,
        album = localAlbumIdentity(row.album)
    )
}

private fun onlineAudioQuery(track: Track): OnlineAudioQuery? {
    val artists = matchArtistNames(track.artist)
    val primary = artists.firstOrNull() ?: return null
    val parsed = localTitleIdentity(track.title)
    if (parsed.base.isEmpty()) return null
    val versions = if (track.explicit) parsed.versions + VERSION_EXPLICIT else parsed.versions
    return OnlineAudioQuery(
        title = parsed.copy(versions = versions),
        artists = (artists + matchArtistNames(track.albumArtist)).toSet(),
        primaryArtist = primary,
        album = localAlbumIdentity(track.album),
        durationMs = track.durationMs
    )
}

private fun localAudioMatch(query: OnlineAudioQuery, candidate: LocalAudioCandidate): LocalAudioMatch? {
    if (!localVersionsCompatible(query.title.versions, candidate.title.versions)) return null
    val albumMatches = query.album.isNotEmpty() && query.album == candidate.album
    val albumConflicts = query.album.isNotEmpty() && candidate.album.isNotEmpty() && !albumMatches
    val delta = if (query.durationMs > 0L && candidate.row.durationMs > 0L) {
        abs(query.durationMs - candidate.row.durationMs)
    } else {
        null
    }
    val accepted = if (candidate.artists.isEmpty()) {
        albumMatches && delta != null && delta <= UNKNOWN_ARTIST_TOLERANCE_MS
    } else {
        val artistMatches = query.primaryArtist in candidate.artists || candidate.primaryArtist in query.artists
        artistMatches && when {
            delta == null -> albumMatches
            query.title.base in GENERIC_TITLES -> albumMatches && delta <= SAME_ALBUM_TOLERANCE_MS
            albumMatches -> delta <= SAME_ALBUM_TOLERANCE_MS
            albumConflicts -> delta <= OTHER_ALBUM_TOLERANCE_MS
            else -> delta <= UNKNOWN_ALBUM_TOLERANCE_MS
        }
    }
    return if (accepted) LocalAudioMatch(candidate.row, albumMatches, delta) else null
}

internal fun localVersionsCompatible(online: Set<String>, local: Set<String>): Boolean {
    if (online - VERSION_EXPLICIT != local - VERSION_EXPLICIT) return false
    return VERSION_EXPLICIT !in local || VERSION_EXPLICIT in online
}

internal fun localTitleIdentity(raw: String): LocalTitleIdentity {
    val versions = sortedSetOf<String>()
    val compatible = Normalizer.normalize(raw, Normalizer.Form.NFKC)
    val outside = BRACKETED_SEGMENT.replace(compatible) { segment ->
        if (absorbTitleQualifier(segment.groupValues[1], versions)) " " else " ${segment.groupValues[1]} "
    }
    val parts = TITLE_DASH_SEPARATOR.split(outside)
    val kept = StringBuilder(parts.first().replace(INLINE_FEATURING, " "))
    parts.drop(1).forEach { part ->
        if (!absorbTitleQualifier(part, versions)) kept.append(' ').append(part)
    }
    return LocalTitleIdentity(normalizeMatchText(kept.toString()), versions)
}

internal fun localAlbumIdentity(raw: String): String {
    val compatible = Normalizer.normalize(raw, Normalizer.Form.NFKC)
    val outside = BRACKETED_SEGMENT.replace(compatible) { segment ->
        if (isAlbumEditionText(segment.groupValues[1])) " " else " ${segment.groupValues[1]} "
    }
    val parts = TITLE_DASH_SEPARATOR.split(outside)
    val kept = (listOf(parts.first()) + parts.drop(1).filterNot(::isAlbumEditionText)).joinToString(" ")
    val album = normalizeMatchText(kept)
    return if (album in UNKNOWN_ALBUMS) "" else album
}

private fun isAlbumEditionText(raw: String): Boolean {
    val text = normalizeMatchText(raw)
    return text.isEmpty() || text in ALBUM_RELEASE_SUFFIXES ||
        text.split(' ').any { it in ALBUM_EDITION_WORDS || it.startsWith("remaster") }
}

internal fun matchArtistNames(raw: String): List<String> =
    ARTIST_SEPARATOR.split(Normalizer.normalize(raw.replace(TOPIC_SUFFIX, ""), Normalizer.Form.NFKC))
        .map(::normalizeMatchText)
        .filter { it.isNotEmpty() && it !in UNKNOWN_ARTISTS }
        .distinct()

internal fun normalizeMatchText(raw: String): String {
    val compatible = Normalizer.normalize(raw, Normalizer.Form.NFKC).lowercase(Locale.ROOT)
    val decomposed = Normalizer.normalize(compatible, Normalizer.Form.NFD)
    val withoutLatinAccents = StringBuilder(decomposed.length)
    var previousScript: Character.UnicodeScript? = null
    var index = 0
    while (index < decomposed.length) {
        val codePoint = decomposed.codePointAt(index)
        val isMark = Character.getType(codePoint) == Character.NON_SPACING_MARK.toInt()
        if (!isMark) previousScript = Character.UnicodeScript.of(codePoint)
        if (!isMark || previousScript != Character.UnicodeScript.LATIN) {
            withoutLatinAccents.appendCodePoint(codePoint)
        }
        index += Character.charCount(codePoint)
    }
    val folded = StringBuilder(withoutLatinAccents.length)
    Normalizer.normalize(withoutLatinAccents, Normalizer.Form.NFC).forEach { character ->
        folded.append(LATIN_LETTER_FOLDS[character] ?: character.toString())
    }
    return folded.toString()
        .replace(APOSTROPHES, "")
        .replace(AMPERSAND, " and ")
        .replace(NON_WORD, " ")
        .trim()
        .replace(WHITESPACE, " ")
}

private fun absorbTitleQualifier(segment: String, versions: MutableSet<String>): Boolean {
    val text = normalizeMatchText(segment)
    if (text.isEmpty()) return true
    val words = text.split(' ')
    if (words.first() in FEATURING_WORDS) {
        TITLE_DASH_SEPARATOR.split(segment).drop(1).forEach { absorbTitleQualifier(it, versions) }
        return true
    }
    if (text in NEUTRAL_VERSIONS) return true
    val tags = titleVersionTags(text)
    if (tags.isNotEmpty()) {
        versions += tags
        return true
    }
    return words.all { it in TITLE_NOISE_WORDS }
}

private fun titleVersionTags(text: String): Set<String> {
    val tags = VERSION_RULES.filter { (pattern, _) -> pattern.containsMatchIn(text) }.mapTo(sortedSetOf()) { it.second }
    if (tags.isEmpty() && GENERIC_VERSION.containsMatchIn(text)) tags += "version:$text"
    return tags
}

internal const val VERSION_EXPLICIT = "explicit"
private const val SAME_ALBUM_TOLERANCE_MS = 3_000L
private const val UNKNOWN_ALBUM_TOLERANCE_MS = 2_000L
private const val OTHER_ALBUM_TOLERANCE_MS = 1_500L
private const val UNKNOWN_ARTIST_TOLERANCE_MS = 2_000L

private val BRACKETED_SEGMENT = Regex("[(\\[{【〔]([^)\\]}】〕]*)[)\\]}】〕]")
private val INLINE_FEATURING = Regex("\\s(?:feat\\.?|ft\\.|featuring)\\s.*$", RegexOption.IGNORE_CASE)
private val TITLE_DASH_SEPARATOR = Regex("\\s+[-–—]\\s+")
private val ARTIST_SEPARATOR = Regex(
    "\\s*(?:[,;/&+、，•]|\\s(?:x|×|and|feat\\.?|ft\\.?|featuring|with|vs\\.?)\\s)\\s*",
    RegexOption.IGNORE_CASE
)
private val TOPIC_SUFFIX = Regex("\\s+-\\s+topic\\s*$", RegexOption.IGNORE_CASE)
private val LEADING_TRACK_NUMBER = Regex("^\\s*\\d{1,3}\\s*[-._)]\\s*")
private val FILENAME_ARTIST_TITLE = Regex("^(.+?)\\s+[-–—]\\s+(.+)$")
private val LATIN_LETTER_FOLDS = mapOf(
    'ß' to "ss", 'ı' to "i", 'ł' to "l", 'ø' to "o", 'đ' to "d", 'ð' to "d", 'þ' to "th", 'æ' to "ae", 'œ' to "oe"
)
private val APOSTROPHES = Regex("['’‘`´ʼ]")
private val AMPERSAND = Regex("&")
private val NON_WORD = Regex("[^\\p{L}\\p{N}\\p{M}]+")
private val WHITESPACE = Regex("\\s+")
private val GENERIC_VERSION = Regex("\\b(version|versione|versao|ver)\\b")

private val VERSION_RULES: List<Pair<Regex, String>> = listOf(
    Regex("\\b(live|vivo|directo|direct|concert|concerto|konzert)\\b") to "live",
    Regex("\\b(remix|remixed|rmx|mix|bootleg|rework|flip|vip)\\b") to "remix",
    Regex("\\b(re|ri)master") to "remaster",
    Regex("\\b(acoustic|acustico|acustica|acoustique|akustik|unplugged)\\b") to "acoustic",
    Regex("\\binstrumental") to "instrumental",
    Regex("\\b(karaoke|backing track)\\b") to "karaoke",
    Regex("\\ba ?cap+el+a\\b") to "acapella",
    Regex("\\bdemo\\b") to "demo",
    Regex("\\bedit\\b") to "edit",
    Regex("\\bextended\\b") to "extended",
    Regex("\\b(clean|censored)\\b") to "clean",
    Regex("\\b(explicit|dirty)\\b") to VERSION_EXPLICIT,
    Regex("\\bcover\\b") to "cover",
    Regex("\\bslowed\\b") to "slowed",
    Regex("\\b(sped|speed) ?up\\b") to "sped-up",
    Regex("\\bnightcore\\b") to "nightcore",
    Regex("\\breverb\\b") to "reverb",
    Regex("\\b8d\\b") to "8d",
    Regex("\\bbass boost") to "bass-boost",
    Regex("\\b(orchestral|symphonic|orchestra)\\b") to "orchestral",
    Regex("\\bmono\\b") to "mono",
    Regex("\\breprise\\b") to "reprise",
    Regex("\\b(intro|outro|interlude|skit)\\b") to "segment",
    Regex("\\bsessions?\\b") to "session",
    Regex("\\brehearsal\\b") to "rehearsal",
    Regex("\\bre ?record") to "re-recorded"
)

private val NEUTRAL_VERSIONS = setOf("original mix", "album version", "original version", "original")
private val GENERIC_TITLES = setOf(
    "intro", "outro", "interlude", "prelude", "skit", "untitled", "bonus track", "hidden track", "reprise"
)
private val FEATURING_WORDS = setOf("feat", "ft", "featuring", "with", "prod", "produced")
private val TITLE_NOISE_WORDS = setOf(
    "official", "oficial", "officiel", "ufficiale", "offizielles", "audio", "video", "videoclip", "clip",
    "music", "musica", "lyric", "lyrics", "letra", "testo", "visualizer", "visualiser", "hd", "hq", "4k", "mv"
)
private val ALBUM_EDITION_WORDS = setOf(
    "deluxe", "edition", "edicion", "edizione", "edicao", "expanded", "anniversary", "bonus", "special",
    "collector", "collectors", "reissue", "explicit", "clean", "version", "remastered", "remaster", "mono", "stereo"
)
private val ALBUM_RELEASE_SUFFIXES = setOf("single", "ep")
private val UNKNOWN_ALBUMS = setOf("unknown", "unknown album", "album sconosciuto", "album desconocido", "album inconnu")
private val UNKNOWN_ARTISTS = setOf(
    "unknown", "unknown artist", "artista sconosciuto", "artista desconocido", "artiste inconnu",
    "unbekannter kunstler", "various artists", "various", "va", "artisti vari", "varios artistas", "artistes divers"
)
