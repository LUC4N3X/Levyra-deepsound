package com.luc4n3x.levyra.domain

import java.util.Locale

object RadioMixer {

    const val ARTIST_WINDOW: Int = 10
    const val ARTIST_WINDOW_CAP: Int = 2

    private const val UNIQUE_TITLE_MIN_TOKENS = 3
    private const val UNIQUE_TITLE_MIN_CHARS = 12
    private const val PARTIAL_TITLE_MIN_TOKENS = 2
    private const val PARTIAL_TITLE_MIN_CHARS = 10
    private const val PARTIAL_TITLE_MIN_RATIO = 0.6

    fun mix(
        candidates: List<Track>,
        seed: Track,
        queued: List<Track>,
        recentContext: List<Track>,
        recentlyPlayed: List<Track> = emptyList(),
        limit: Int
    ): List<Track> {
        if (limit <= 0 || candidates.isEmpty()) return emptyList()
        val vocabulary = ArtistVocabulary()
        vocabulary.addAll(candidates)
        vocabulary.add(seed)
        vocabulary.addAll(queued)
        vocabulary.addAll(recentContext)

        val seedKey = vocabulary.key(seed)
        val present = ArrayList<RadioKey>(queued.size + limit + 1)
        present += seedKey
        queued.mapTo(present, vocabulary::key)
        val queuedIds = HashSet<String>(queued.size + 1)
        queued.mapNotNullTo(queuedIds) { it.id.takeIf(String::isNotBlank) }
        seed.id.takeIf(String::isNotBlank)?.let(queuedIds::add)
        val recentKeys = recentlyPlayed.map(vocabulary::key)
        val window = ArrayDeque<Set<String>>(ARTIST_WINDOW)
        recentContext.takeLast(ARTIST_WINDOW).forEach { window.addLast(vocabulary.key(it).artists) }
        val leadingArtists = window.lastOrNull().orEmpty()

        val pending = candidates
            .asSequence()
            .filter { it.id.isNotBlank() && it.title.isNotBlank() && it.id !in queuedIds }
            .distinctBy(Track::id)
            .map { it to vocabulary.key(it) }
            .toMutableList()
        val selected = ArrayList<Pair<Track, Set<String>>>(minOf(limit, pending.size))
        val versionedWorks = HashSet<RadioKey>()

        for (phase in RadioPhase.entries) {
            while (selected.size < limit && pending.isNotEmpty()) {
                val index = pending.indexOfFirst { (_, key) ->
                    eligible(key, phase, seedKey, present, recentKeys, window, versionedWorks)
                }
                if (index < 0) break
                val (track, key) = pending.removeAt(index)
                if (seedKey.sharesTitleWith(key) || present.any { it.isVersionOf(key) }) versionedWorks += key
                selected += track to key.artists
                present += key
                if (window.size == ARTIST_WINDOW) window.removeFirst()
                window.addLast(key.artists)
            }
            if (selected.size >= limit) break
        }
        return spreadArtistRuns(selected, leadingArtists)
    }

    private fun spreadArtistRuns(
        selected: List<Pair<Track, Set<String>>>,
        leadingArtists: Set<String>
    ): List<Track> {
        val result = ArrayList<Pair<Track, Set<String>>>(selected.size)
        fun clashes(index: Int, artists: Set<String>): Boolean {
            if (artists.isEmpty()) return false
            val before = if (index == 0) leadingArtists else result[index - 1].second
            val after = result.getOrNull(index)?.second.orEmpty()
            return before.any { it in artists } || after.any { it in artists }
        }
        for (entry in selected) {
            if (!clashes(result.size, entry.second)) {
                result += entry
                continue
            }
            val slot = (1 until result.size).firstOrNull { !clashes(it, entry.second) }
            if (slot != null) result.add(slot, entry) else result += entry
        }
        return result.map { it.first }
    }

    internal fun sameRecording(first: Track, second: Track): Boolean {
        val vocabulary = ArtistVocabulary()
        vocabulary.add(first)
        vocabulary.add(second)
        return vocabulary.key(first).isSameRecordingAs(vocabulary.key(second))
    }

    internal fun isVersionOf(first: Track, second: Track): Boolean {
        val vocabulary = ArtistVocabulary()
        vocabulary.add(first)
        vocabulary.add(second)
        return vocabulary.key(first).isVersionOf(vocabulary.key(second))
    }

    private fun eligible(
        key: RadioKey,
        phase: RadioPhase,
        seedKey: RadioKey,
        present: List<RadioKey>,
        recentKeys: List<RadioKey>,
        window: ArrayDeque<Set<String>>,
        versionedWorks: Set<RadioKey>
    ): Boolean {
        if (present.any { it.isSameRecordingAs(key) }) return false
        if (seedKey.sharesTitleWith(key) || present.any { it.isVersionOf(key) }) {
            if (!phase.allowVersions) return false
            if (versionedWorks.any { it.isSameWorkAs(key) || it.sharesTitleWith(key) }) return false
        }
        if (phase.avoidRecent && recentKeys.any { it.isSameWorkAs(key) }) return false
        if (key.artists.isEmpty()) return true
        if (phase.avoidConsecutive && window.lastOrNull()?.any { it in key.artists } == true) return false
        if (phase.capArtists) {
            val crowded = key.artists.any { artist -> window.count { artist in it } >= ARTIST_WINDOW_CAP }
            if (crowded) return false
        }
        return true
    }

    private enum class RadioPhase(
        val capArtists: Boolean,
        val avoidConsecutive: Boolean,
        val avoidRecent: Boolean,
        val allowVersions: Boolean
    ) {
        Balanced(capArtists = true, avoidConsecutive = true, avoidRecent = true, allowVersions = false),
        Spaced(capArtists = false, avoidConsecutive = true, avoidRecent = true, allowVersions = false),
        Fresh(capArtists = false, avoidConsecutive = false, avoidRecent = true, allowVersions = false),
        Fill(capArtists = false, avoidConsecutive = false, avoidRecent = false, allowVersions = true)
    }

    private class RadioKey(
        val titleTokens: Set<String>,
        val titleChars: Int,
        val rawTokens: Set<String>,
        val artists: Set<String>,
        val artistNameTokens: List<Set<String>>,
        val variants: Set<String>
    ) {
        private val creditTokens: Set<String> = artistNameTokens.flatMapTo(HashSet()) { it }

        private fun creditsAppearIn(other: RadioKey): Boolean = artistNameTokens.any { name ->
            name.isNotEmpty() && name.all { it in other.creditTokens || it in other.rawTokens }
        }

        fun isSameWorkAs(other: RadioKey): Boolean {
            if (titleTokens.isEmpty() || other.titleTokens.isEmpty()) return false
            val sharesArtist = artists.any { it in other.artists } || creditsAppearIn(other) || other.creditsAppearIn(this)
            if (titleTokens == other.titleTokens) {
                return sharesArtist ||
                    variants.isNotEmpty() ||
                    other.variants.isNotEmpty() ||
                    (titleTokens.size >= UNIQUE_TITLE_MIN_TOKENS && titleChars >= UNIQUE_TITLE_MIN_CHARS)
            }
            val (small, large) = if (titleTokens.size <= other.titleTokens.size) {
                this to other
            } else {
                other to this
            }
            if (small.titleTokens.size < PARTIAL_TITLE_MIN_TOKENS) return false
            if (!large.titleTokens.containsAll(small.titleTokens)) return false
            val ratio = small.titleTokens.size.toDouble() / large.titleTokens.size.toDouble()
            if (ratio < PARTIAL_TITLE_MIN_RATIO) return false
            return sharesArtist || small.titleChars >= PARTIAL_TITLE_MIN_CHARS
        }

        fun sharesTitleWith(other: RadioKey): Boolean = titleTokens.isNotEmpty() && titleTokens == other.titleTokens

        fun isSameRecordingAs(other: RadioKey): Boolean = variants == other.variants && isSameWorkAs(other)

        fun isVersionOf(other: RadioKey): Boolean = variants != other.variants && isSameWorkAs(other)
    }

    private class ArtistVocabulary {
        private val names = HashSet<String>()
        private val tokens = HashSet<String>()

        fun add(track: Track) {
            artistNames(track.artist).forEach { name ->
                names += name
                tokens += tokenize(name)
            }
        }

        fun addAll(tracks: List<Track>) = tracks.forEach(::add)

        fun key(track: Track): RadioKey {
            val lowered = track.title.lowercase(Locale.ROOT)
            val credited = artistNames(track.artist)
            val variants = HashSet<String>()
            val titleTokens = LinkedHashSet<String>()
            val rawTokens = HashSet<String>()
            val prefixArtists = HashSet<String>()

            BRACKET_PATTERN.findAll(lowered).forEach { match ->
                tokenize(match.value).filterTo(variants) { it in VARIANT_TOKENS }
            }
            val segments = lowered.replace(BRACKET_PATTERN, " ").split(SEGMENT_PATTERN)
            segments.forEachIndexed { index, segment ->
                val segmentTokens = tokenize(segment)
                rawTokens += segmentTokens
                val normalizedSegment = segmentTokens.joinToString(" ")
                if (index == 0 && segments.size > 1 && normalizedSegment in names) {
                    prefixArtists += normalizedSegment
                    return@forEachIndexed
                }
                val segmentVariants = segmentTokens.filter { it in VARIANT_TOKENS }
                if (index > 0 && segmentVariants.isNotEmpty()) {
                    variants += segmentVariants
                    return@forEachIndexed
                }
                segmentTokens.filterTo(titleTokens) { it !in NOISE_TOKENS }
            }
            rawTokens += tokenize(lowered)

            val withoutArtists = titleTokens.filterTo(LinkedHashSet()) { it !in tokens }
            val finalTokens = withoutArtists.ifEmpty { titleTokens }
            return RadioKey(
                titleTokens = finalTokens,
                titleChars = finalTokens.sumOf(String::length),
                rawTokens = rawTokens,
                artists = credited.toHashSet().apply { addAll(prefixArtists) },
                artistNameTokens = credited.map { tokenize(it).toSet() },
                variants = variants.mapTo(HashSet()) { VARIANT_ALIASES[it] ?: it }
            )
        }
    }

    private fun artistNames(artist: String): List<String> =
        ListeningSignalEngine.splitArtists(artist)
            .map { tokenize(ListenIdentity.artistKey(it)).joinToString(" ") }
            .filter { it.isNotBlank() && it !in IGNORED_ARTISTS }

    private fun tokenize(value: String): List<String> =
        TOKEN_PATTERN.findAll(value.lowercase(Locale.ROOT)).map { it.value }.toList()

    private val TOKEN_PATTERN = Regex("""[\p{L}\p{M}\p{N}]+""")
    private val BRACKET_PATTERN = Regex("""[(\[【「『][^)\]】」』]*[)\]】」』]""")
    private val SEGMENT_PATTERN = Regex("""\s+[-–—|~:@]\s+|\s*[|｜]\s*""")

    private val NOISE_TOKENS = setOf(
        "official", "music", "video", "audio", "lyrics", "lyric", "visualizer", "visualiser",
        "hd", "hq", "4k", "mv", "m", "v", "ost", "soundtrack", "score", "the", "a", "an", "of",
        "by", "from", "feat", "ft", "featuring", "with", "full", "original", "topic", "vevo"
    )
    private val VARIANT_TOKENS = setOf(
        "live", "remix", "rmx", "edit", "acoustic", "instrumental", "cover", "piano", "orchestral",
        "orchestra", "epic", "extended", "extra", "remaster", "remastered", "unplugged", "demo",
        "version", "ver", "cinematic", "slowed", "reverb", "nightcore", "8d", "loop", "hour", "hours",
        "karaoke", "strings", "violin", "cello", "guitar", "lofi", "bootleg", "mashup", "rework", "vip"
    )
    private val VARIANT_ALIASES = mapOf(
        "rmx" to "remix",
        "remastered" to "remaster",
        "orchestra" to "orchestral",
        "hours" to "hour",
        "ver" to "version"
    )
    private val IGNORED_ARTISTS = setOf("various artists", "unknown artist")
}
