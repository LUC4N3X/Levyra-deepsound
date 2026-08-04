package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.Track
import java.text.Normalizer
import java.util.Locale

internal data class HomeSectionMergeResult(
    val visible: List<HomeSection>,
    val deferredStructural: List<HomeSection>?,
    val changed: Boolean
)

internal object HomeRefreshStability {
    private const val MIN_SECTION_TRACKS = 3
    private const val MAX_SECTIONS = 10
    private const val MAX_TRACKS_PER_SECTION = 20
    private const val MIN_SECTION_OVERLAP = 2
    private const val MIN_SECTION_OVERLAP_RATIO = 0.35

    fun sanitizeSections(sections: List<HomeSection>): List<HomeSection> {
        val occurrences = HashMap<String, Int>()
        return sections
            .asSequence()
            .mapNotNull { section ->
                val title = section.title.trim()
                if (title.length < 2) return@mapNotNull null
                val tracks = sanitizeTracks(section.tracks)
                if (tracks.size < MIN_SECTION_TRACKS) return@mapNotNull null
                HomeSection(title = title, tracks = tracks)
            }
            .filter { section ->
                val base = sectionTitleIdentity(section.title)
                val occurrence = occurrences.getOrDefault(base, 0)
                occurrences[base] = occurrence + 1
                occurrence < 2
            }
            .take(MAX_SECTIONS)
            .toList()
    }

    fun mergeSections(
        previous: List<HomeSection>,
        incoming: List<HomeSection>,
        allowStructuralChanges: Boolean
    ): HomeSectionMergeResult {
        val oldSections = previous
        val newSections = sanitizeSections(incoming)
        if (newSections.isEmpty()) {
            return HomeSectionMergeResult(previous, null, false)
        }
        if (oldSections.isEmpty()) {
            return HomeSectionMergeResult(newSections, null, newSections != previous)
        }

        val oldEntries = keyedSections(oldSections)
        val newEntries = keyedSections(newSections, oldEntries)
        val oldKeys = oldEntries.map { it.first }
        val newKeys = newEntries.map { it.first }
        val structuralChange = oldKeys != newKeys
        val oldByKey = oldEntries.toMap()
        val newByKey = newEntries.toMap()

        if (allowStructuralChanges && structuralChange) {
            val structurallyMerged = newEntries.map { (key, section) ->
                mergeSection(oldByKey[key], section)
            }
            return HomeSectionMergeResult(
                visible = structurallyMerged,
                deferredStructural = null,
                changed = !sameSections(previous, structurallyMerged)
            )
        }

        val stableVisible = oldEntries.map { (key, oldSection) ->
            val replacement = newByKey[key]
            if (replacement == null) oldSection else mergeSection(oldSection, replacement)
        }
        return HomeSectionMergeResult(
            visible = stableVisible,
            deferredStructural = newSections.takeIf { structuralChange },
            changed = !sameSections(previous, stableVisible)
        )
    }

    fun sameSections(first: List<HomeSection>, second: List<HomeSection>): Boolean {
        if (first.size != second.size) return false
        return first.indices.all { index -> sameSection(first[index], second[index]) }
    }

    fun sectionTitleIdentity(title: String): String {
        val compatibilityNormalized = Normalizer.normalize(title.trim(), Normalizer.Form.NFKC)
        val latinFolded = Normalizer.normalize(compatibilityNormalized, Normalizer.Form.NFD)
            .replace(Regex("(\\p{IsLatin})\\p{M}+")) { match -> match.groupValues[1] }
        val normalized = Normalizer.normalize(latinFolded, Normalizer.Form.NFC)
            .lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{M}\\p{N}]+"), "-")
            .trim('-')
        return normalized.ifBlank { "section" }
    }

    fun sectionIdentity(section: HomeSection): String {
        val trackKeys = sectionTrackIdentities(section).sorted()
        return if (trackKeys.isEmpty()) {
            "section"
        } else {
            "tracks:${trackKeys.joinToString("\u001F")}" 
        }
    }

    private fun keyedSections(
        sections: List<HomeSection>,
        previousEntries: List<Pair<String, HomeSection>> = emptyList()
    ): List<Pair<String, HomeSection>> {
        val occurrences = HashMap<String, Int>()
        val directEntries = sections.map { section ->
            val base = sectionIdentity(section)
            val occurrence = occurrences.getOrDefault(base, 0)
            occurrences[base] = occurrence + 1
            "$base#$occurrence" to section
        }
        if (previousEntries.isEmpty()) return directEntries

        val unmatchedPrevious = previousEntries.toMutableList()
        return directEntries.map { directEntry ->
            val exactIndex = unmatchedPrevious.indexOfFirst { it.first == directEntry.first }
            val matchedIndex = if (exactIndex >= 0) {
                exactIndex
            } else {
                bestOverlapIndex(unmatchedPrevious, directEntry.second)
            }
            if (matchedIndex < 0) {
                directEntry
            } else {
                val stableKey = unmatchedPrevious.removeAt(matchedIndex).first
                stableKey to directEntry.second
            }
        }
    }

    private fun bestOverlapIndex(
        candidates: List<Pair<String, HomeSection>>,
        incoming: HomeSection
    ): Int {
        val incomingKeys = sectionTrackIdentities(incoming)
        if (incomingKeys.isEmpty()) return -1
        var bestIndex = -1
        var bestOverlap = 0
        var bestRatio = 0.0
        candidates.forEachIndexed { index, (_, candidate) ->
            val candidateKeys = sectionTrackIdentities(candidate)
            if (candidateKeys.isEmpty()) return@forEachIndexed
            val overlap = incomingKeys.count(candidateKeys::contains)
            val ratio = overlap.toDouble() / minOf(incomingKeys.size, candidateKeys.size).toDouble()
            if (overlap > bestOverlap || (overlap == bestOverlap && ratio > bestRatio)) {
                bestIndex = index
                bestOverlap = overlap
                bestRatio = ratio
            }
        }
        return bestIndex.takeIf {
            bestOverlap >= MIN_SECTION_OVERLAP && bestRatio >= MIN_SECTION_OVERLAP_RATIO
        } ?: -1
    }

    private fun sectionTrackIdentities(section: HomeSection): Set<String> {
        return section.tracks
            .asSequence()
            .map(::trackIdentity)
            .filter(String::isNotBlank)
            .toCollection(LinkedHashSet())
    }

    private fun mergeSection(previous: HomeSection?, incoming: HomeSection): HomeSection {
        if (previous == null) return incoming
        if (sameSection(previous, incoming)) return previous
        val previousByIdentity = previous.tracks.associateBy(::trackIdentity)
        val tracks = incoming.tracks.map { track ->
            val oldTrack = previousByIdentity[trackIdentity(track)]
            when {
                oldTrack == null -> track
                oldTrack == track -> oldTrack
                else -> mergeTrack(oldTrack, track)
            }
        }
        return HomeSection(title = incoming.title, tracks = tracks)
    }

    private fun mergeTrack(previous: Track, incoming: Track): Track {
        val artworkMerged = LevyraPersonalOrbit.preferAlbumArtwork(incoming, previous)
        val preferPreviousMetadata = previous.metadataConfidence > incoming.metadataConfidence
        return artworkMerged.copy(
            album = artworkMerged.album.ifBlank { previous.album },
            durationMs = artworkMerged.durationMs.takeIf { it > 0L } ?: previous.durationMs,
            streamUrl = artworkMerged.streamUrl.ifBlank { previous.streamUrl },
            videoUrl = artworkMerged.videoUrl.ifBlank { previous.videoUrl },
            source = artworkMerged.source.ifBlank { previous.source },
            moodTags = artworkMerged.moodTags.ifEmpty { previous.moodTags },
            videoStreamUrl = artworkMerged.videoStreamUrl.ifBlank { previous.videoStreamUrl },
            sponsorSegments = artworkMerged.sponsorSegments.ifEmpty { previous.sponsorSegments },
            youtubeLoudnessDb = artworkMerged.youtubeLoudnessDb ?: previous.youtubeLoudnessDb,
            youtubePerceptualLoudnessDb = artworkMerged.youtubePerceptualLoudnessDb
                ?: previous.youtubePerceptualLoudnessDb,
            isrc = artworkMerged.isrc.ifBlank { previous.isrc },
            upc = artworkMerged.upc.ifBlank { previous.upc },
            releaseDate = artworkMerged.releaseDate.ifBlank { previous.releaseDate },
            year = artworkMerged.year.ifBlank { previous.year },
            trackNumber = artworkMerged.trackNumber.takeIf { it > 0 } ?: previous.trackNumber,
            discNumber = artworkMerged.discNumber.takeIf { it > 0 } ?: previous.discNumber,
            explicit = artworkMerged.explicit || previous.explicit,
            albumBrowseId = artworkMerged.albumBrowseId.ifBlank { previous.albumBrowseId },
            artistBrowseIds = artworkMerged.artistBrowseIds.ifEmpty { previous.artistBrowseIds },
            counterpartVideoId = artworkMerged.counterpartVideoId.ifBlank { previous.counterpartVideoId },
            videoType = artworkMerged.videoType.ifBlank { previous.videoType },
            metadataProvider = if (preferPreviousMetadata) {
                previous.metadataProvider.ifBlank { artworkMerged.metadataProvider }
            } else {
                artworkMerged.metadataProvider.ifBlank { previous.metadataProvider }
            },
            metadataConfidence = maxOf(previous.metadataConfidence, artworkMerged.metadataConfidence),
            canonicalAlbumUrl = artworkMerged.canonicalAlbumUrl.ifBlank { previous.canonicalAlbumUrl }
        )
    }

    private fun sanitizeTracks(tracks: List<Track>): List<Track> {
        val unique = LinkedHashMap<String, Track>()
        tracks.forEach { track ->
            if (track.title.isBlank() || track.artist.isBlank()) return@forEach
            val key = trackIdentity(track)
            if (key.isNotBlank()) unique.putIfAbsent(key, track)
        }
        return unique.values.take(MAX_TRACKS_PER_SECTION)
    }

    private fun sameSection(first: HomeSection, second: HomeSection): Boolean {
        return first.title == second.title && first.tracks == second.tracks
    }

    private fun trackIdentity(track: Track): String {
        return track.id.trim().ifBlank {
            track.videoUrl.trim().ifBlank {
                "${track.title.trim().lowercase(Locale.ROOT)}|${track.artist.trim().lowercase(Locale.ROOT)}"
            }
        }
    }
}
