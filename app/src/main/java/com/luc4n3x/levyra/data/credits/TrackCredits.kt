package com.luc4n3x.levyra.data.credits

import com.luc4n3x.levyra.domain.Track
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

enum class TrackCreditGroup { Artists, Performance, Composition, Production, Other }

data class TrackCreditPerson(
    val name: String,
    val roles: List<String> = emptyList(),
    val linkedArtistIndex: Int? = null
)

data class TrackCreditSection(
    val group: TrackCreditGroup,
    val title: String,
    val people: List<TrackCreditPerson>
)

data class TrackReleaseCredits(
    val album: String = "",
    val releaseDate: String = "",
    val label: String = "",
    val phonographicCopyright: String = "",
    val copyright: String = "",
    val isrc: String = "",
    val upc: String = ""
) {
    val isEmpty: Boolean
        get() = album.isBlank() && releaseDate.isBlank() && label.isBlank() &&
            phonographicCopyright.isBlank() && copyright.isBlank() && isrc.isBlank() && upc.isBlank()
}

data class TrackCredits(
    val sections: List<TrackCreditSection> = emptyList(),
    val release: TrackReleaseCredits = TrackReleaseCredits(),
    val provider: String = "",
    val remoteResolved: Boolean = false
) {
    val isEmpty: Boolean get() = sections.none { it.people.isNotEmpty() } && release.isEmpty
}

internal object TrackCreditsParser {
    private val composerSeparators = Regex("\\s*(?:,|&|/|;|\\s+and\\s+)\\s*", RegexOption.IGNORE_CASE)
    private val primaryArtistRoles = setOf("primary artist", "main artist", "artist")

    fun parseApple(song: JSONObject?, credits: JSONObject?, releaseMatch: Boolean): TrackCredits {
        val songData = song?.optJSONArray("data")?.optJSONObject(0)
        val songAttributes = songData?.optJSONObject("attributes")
        val albumAttributes = songData?.optJSONObject("relationships")
            ?.optJSONObject("albums")?.optJSONArray("data")?.optJSONObject(0)?.optJSONObject("attributes")

        val sections = parseCreditSections(credits).toMutableList()
        val composerName = songAttributes?.optString("composerName").orEmpty().trim()
        if (sections.none { it.group == TrackCreditGroup.Composition } && composerName.isNotBlank()) {
            composerSection(composerName)?.let(sections::add)
        }

        val copyrightLine = if (releaseMatch) albumAttributes?.optString("copyright").orEmpty().trim() else ""
        val release = TrackReleaseCredits(
            album = if (releaseMatch) albumAttributes?.optString("name").orEmpty().trim()
                .ifBlank { songAttributes?.optString("albumName").orEmpty().trim() } else "",
            releaseDate = if (releaseMatch) songAttributes?.optString("releaseDate").orEmpty().trim()
                .ifBlank { albumAttributes?.optString("releaseDate").orEmpty().trim() } else "",
            label = if (releaseMatch) albumAttributes?.optString("recordLabel").orEmpty().trim() else "",
            phonographicCopyright = copyrightLine.takeIf(::isPhonographic).orEmpty(),
            copyright = copyrightLine.takeUnless(::isPhonographic).orEmpty(),
            isrc = songAttributes?.optString("isrc").orEmpty().trim(),
            upc = if (releaseMatch) albumAttributes?.optString("upc").orEmpty().trim() else ""
        )
        return TrackCredits(
            sections = sections.sortedBy { it.group.ordinal },
            release = release,
            provider = APPLE_PROVIDER,
            remoteResolved = true
        )
    }

    fun localFallback(track: Track): TrackCredits {
        val sections = listOfNotNull(composerSection(track.composer.trim()))
        val copyright = track.copyright.trim()
        return TrackCredits(
            sections = sections,
            release = TrackReleaseCredits(
                album = track.album.trim(),
                releaseDate = track.releaseDate.trim().ifBlank { track.year.trim() },
                phonographicCopyright = copyright.takeIf(::isPhonographic).orEmpty(),
                copyright = copyright.takeUnless(::isPhonographic).orEmpty(),
                isrc = track.isrc.trim(),
                upc = track.upc.trim()
            )
        )
    }

    fun merge(remote: TrackCredits, local: TrackCredits): TrackCredits {
        val sections = remote.sections.ifEmpty { local.sections }
        val r = remote.release
        val l = local.release
        return remote.copy(
            sections = sections,
            release = TrackReleaseCredits(
                album = r.album.ifBlank { l.album },
                releaseDate = r.releaseDate.ifBlank { l.releaseDate },
                label = r.label.ifBlank { l.label },
                phonographicCopyright = r.phonographicCopyright.ifBlank { l.phonographicCopyright },
                copyright = r.copyright.ifBlank { l.copyright },
                isrc = r.isrc.ifBlank { l.isrc },
                upc = r.upc.ifBlank { l.upc }
            )
        )
    }

    fun withArtists(credits: TrackCredits, artistNames: List<String>): TrackCredits {
        val artists = artistNames.map(String::trim).filter(String::isNotBlank).distinctBy(::normalizeName)
        if (artists.isEmpty()) return credits
        val artistIndex = artists.withIndex().associate { normalizeName(it.value) to it.index }
        val sections = credits.sections.mapNotNull { section ->
            val people = section.people
                .filterNot { person ->
                    section.group == TrackCreditGroup.Performance &&
                        normalizeName(person.name) in artistIndex &&
                        person.roles.all { it.trim().lowercase(Locale.ROOT) in primaryArtistRoles }
                }
                .map { person -> person.copy(linkedArtistIndex = artistIndex[normalizeName(person.name)]) }
            if (people.isEmpty()) null else section.copy(people = people)
        }
        val artistSection = TrackCreditSection(
            group = TrackCreditGroup.Artists,
            title = "",
            people = artists.mapIndexed { index, name -> TrackCreditPerson(name = name, linkedArtistIndex = index) }
        )
        return credits.copy(sections = listOf(artistSection) + sections)
    }

    fun normalizeName(name: String): String =
        name.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")

    private fun parseCreditSections(root: JSONObject?): List<TrackCreditSection> {
        val data = root?.optJSONArray("data") ?: return emptyList()
        val sections = LinkedHashMap<Pair<TrackCreditGroup, String>, MutableList<TrackCreditPerson>>()
        for (index in 0 until data.length()) {
            val section = data.optJSONObject(index) ?: continue
            val attributes = section.optJSONObject("attributes")
            val title = attributes?.optString("title").orEmpty().trim()
            val group = groupOf(
                listOf(section.optString("id"), attributes?.optString("kind").orEmpty(), title).joinToString(" ")
            )
            val items = section.optJSONObject("relationships")
                ?.optJSONObject("credit-artists")
                ?.optJSONArray("data")
                ?: continue
            val people = sections.getOrPut(group to if (group == TrackCreditGroup.Other) title else "") { mutableListOf() }
            for (itemIndex in 0 until items.length()) {
                val itemAttributes = items.optJSONObject(itemIndex)?.optJSONObject("attributes") ?: continue
                val name = itemAttributes.optString("name").trim()
                if (name.isBlank()) continue
                val roles = itemAttributes.optJSONArray("roleNames").toTrimmedList()
                val existing = people.indexOfFirst { normalizeName(it.name) == normalizeName(name) }
                if (existing >= 0) {
                    val merged = (people[existing].roles + roles).distinctBy { it.lowercase(Locale.ROOT) }
                    people[existing] = people[existing].copy(roles = merged)
                } else {
                    people += TrackCreditPerson(name = name, roles = roles.distinctBy { it.lowercase(Locale.ROOT) })
                }
            }
        }
        return sections.mapNotNull { (key, people) ->
            if (people.isEmpty()) null else TrackCreditSection(group = key.first, title = key.second, people = people)
        }
    }

    private fun composerSection(composerName: String): TrackCreditSection? {
        val people = composerName.split(composerSeparators)
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinctBy(::normalizeName)
            .map { TrackCreditPerson(name = it) }
        if (people.isEmpty()) return null
        return TrackCreditSection(group = TrackCreditGroup.Composition, title = "", people = people)
    }

    private fun groupOf(descriptor: String): TrackCreditGroup {
        val text = descriptor.lowercase(Locale.ROOT)
        return when {
            "compos" in text || "lyric" in text || "writ" in text -> TrackCreditGroup.Composition
            "produc" in text || "engineer" in text || "mix" in text || "master" in text -> TrackCreditGroup.Production
            "perform" in text -> TrackCreditGroup.Performance
            else -> TrackCreditGroup.Other
        }
    }

    private fun isPhonographic(line: String): Boolean {
        val trimmed = line.trimStart()
        return trimmed.startsWith("℗") || trimmed.startsWith("(P)", ignoreCase = true)
    }

    private fun JSONArray?.toTrimmedList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                optString(index).trim().takeIf(String::isNotBlank)?.let(::add)
            }
        }
    }

    const val APPLE_PROVIDER = "Apple Music"
}
