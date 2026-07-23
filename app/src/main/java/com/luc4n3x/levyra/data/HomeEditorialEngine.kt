package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.HomeCollectionKind
import com.luc4n3x.levyra.domain.HomeCollectionSource
import com.luc4n3x.levyra.domain.HomeEditorialCollection
import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.HomeSpotlightCandidate
import com.luc4n3x.levyra.domain.HomeSpotlightKind
import com.luc4n3x.levyra.domain.Track
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.absoluteValue

object HomeEditorialEngine {
    private const val collectionTrackLimit = 18
    private const val minimumCollectionSize = 4
    private const val millisecondsPerDay = 86_400_000L

    fun localDayKey(nowMillis: Long = System.currentTimeMillis()): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return calendar.get(Calendar.YEAR) * 400 + calendar.get(Calendar.DAY_OF_YEAR)
    }

    fun buildSpotlightCandidates(
        showNewReleases: Boolean,
        newReleaseTracks: List<Track>,
        showPersonalOrbit: Boolean,
        personalTracks: List<Track>,
        showResonance: Boolean,
        resonanceTracks: List<Track>,
        quickPickTracks: List<Track>,
        fallbackSections: List<List<Track>>,
        chartTracks: List<Track>,
        currentTrackId: String?,
        nowMillis: Long = System.currentTimeMillis()
    ): List<HomeSpotlightCandidate> {
        val sourcePriority = LinkedHashMap<String, Int>()
        val candidates = LinkedHashMap<String, Track>()

        fun append(tracks: List<Track>, priority: Int) {
            tracks.forEach { track ->
                val key = identityKey(track)
                if (key.isBlank() || !isReliableCandidate(track) || track.id == currentTrackId) return@forEach
                if (key !in candidates) candidates[key] = track
                val previous = sourcePriority[key] ?: Int.MIN_VALUE
                if (priority > previous) sourcePriority[key] = priority
            }
        }

        if (showNewReleases) append(newReleaseTracks, 7_000)
        append(chartTracks, 5_500)
        if (showPersonalOrbit) append(personalTracks, 4_500)
        if (showResonance) append(resonanceTracks, 4_000)
        append(quickPickTracks, 3_500)
        fallbackSections.forEachIndexed { index, tracks -> append(tracks, 3_000 - index.coerceAtMost(10) * 50) }

        val todayStart = startOfLocalDay(nowMillis)
        val newReleaseKeys = newReleaseTracks.asSequence().map(::identityKey).toHashSet()
        val chartKeys = chartTracks.asSequence().map(::identityKey).toHashSet()
        val daySeed = localDayKey(nowMillis)

        return candidates.map { (key, track) ->
            val releaseMillis = parseReleaseDate(track.releaseDate, nowMillis)
            val ageDays = releaseMillis?.let { ((todayStart - startOfLocalDay(it)) / millisecondsPerDay).toInt() }
                ?.takeIf { it >= 0 }
            val inNewReleases = key in newReleaseKeys
            val inCharts = key in chartKeys
            val formatKind = inferReleaseKind(track)
            val kind = when {
                ageDays == 0 -> HomeSpotlightKind.ReleasedToday
                ageDays != null && ageDays <= 7 && formatKind == HomeSpotlightKind.NewAlbum -> HomeSpotlightKind.NewAlbum
                ageDays != null && ageDays <= 7 && formatKind == HomeSpotlightKind.NewSingle -> HomeSpotlightKind.NewSingle
                inNewReleases -> HomeSpotlightKind.JustReleased
                inCharts -> HomeSpotlightKind.TrendingToday
                else -> HomeSpotlightKind.LevyraSelect
            }
            val freshnessScore = when {
                ageDays == 0 -> 12_000
                ageDays != null && ageDays <= 7 -> 10_000 - ageDays * 220
                inNewReleases -> 8_000
                inCharts -> 6_000
                else -> 0
            }
            val artworkScore = when {
                track.largeThumbnailUrl.isNotBlank() -> 180
                track.thumbnailUrl.isNotBlank() -> 90
                else -> 0
            }
            val metadataScore = track.metadataConfidence.coerceIn(0, 100) * 2
            val engagementScore = (track.replayScore + track.cacheScore + track.energy / 2).coerceIn(0, 260)
            val stableDailyJitter = stableHash("$daySeed|${track.id}|${track.title}|${track.artist}") % 97
            HomeSpotlightCandidate(
                track = track,
                kind = kind,
                score = freshnessScore + sourcePriority.getValue(key) + artworkScore + metadataScore + engagementScore + stableDailyJitter,
                releaseAgeDays = ageDays
            )
        }
            .sortedWith(
                compareByDescending<HomeSpotlightCandidate> { it.score }
                    .thenBy { stableHash("$daySeed|${it.track.id}|${it.track.title}") }
            )
    }

    fun buildCollections(
        homeSections: List<HomeSection>,
        newReleaseTracks: List<Track>,
        personalTracks: List<Track>,
        resonanceTracks: List<Track>,
        quickPickTracks: List<Track>,
        chartTracks: List<Track>,
        favorites: List<Track>,
        libraryTracks: List<Track>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<HomeEditorialCollection> {
        val pool = buildList {
            addAll(newReleaseTracks)
            addAll(personalTracks)
            addAll(resonanceTracks)
            addAll(quickPickTracks)
            addAll(chartTracks)
            homeSections.forEach { addAll(it.tracks) }
            addAll(favorites)
            addAll(libraryTracks)
        }
            .asSequence()
            .filter(::isReliableCandidate)
            .distinctBy(::identityKey)
            .toList()

        if (pool.size < minimumCollectionSize) return emptyList()

        val todayStart = startOfLocalDay(nowMillis)
        val fresh = pool.filter { track ->
            val releaseMillis = parseReleaseDate(track.releaseDate, nowMillis) ?: return@filter false
            val ageDays = ((todayStart - startOfLocalDay(releaseMillis)) / millisecondsPerDay).toInt()
            ageDays in 0..14
        }
        val newReleaseKeys = newReleaseTracks.asSequence().map(::identityKey).toHashSet()
        val local = pool.filter { "local" in normalizedTags(it) }
        val workout = pool.filter {
            val tags = normalizedTags(it)
            it.energy >= 86 || tags.any { tag -> tag in setOf("gym", "workout", "energy", "training", "running") }
        }
        val chill = pool.filter {
            val tags = normalizedTags(it)
            it.energy <= 70 || tags.any { tag -> tag in setOf("chill", "relax", "lofi", "calm", "sleep", "mood") }
        }
        val focus = pool.filter {
            val tags = normalizedTags(it)
            tags.any { tag -> tag in setOf("focus", "study", "drive", "electronic", "instrumental") } ||
                it.energy in 65..82 && it.vocal <= 72
        }
        val party = pool.filter {
            val tags = normalizedTags(it)
            it.energy >= 84 && tags.any { tag -> tag in setOf("party", "dance", "latin", "club", "funk") }
        }
        val rap = pool.filter {
            val tags = normalizedTags(it)
            tags.any { tag -> tag in setOf("rap", "hiphop", "hip-hop", "drill", "trap") }
        }
        val pop = pool.filter {
            val tags = normalizedTags(it)
            tags.any { tag -> tag in setOf("pop", "hit", "anthem") }
        }
        val discovery = pool.sortedWith(
            compareByDescending<Track> { it.metadataConfidence + it.replayScore + it.cacheScore }
                .thenByDescending { it.largeThumbnailUrl.isNotBlank() }
                .thenBy { stableHash(identityKey(it)) }
        )

        val result = ArrayList<HomeEditorialCollection>(8)
        val usedCollectionIds = HashSet<String>()

        fun addCollection(
            id: String,
            kind: HomeCollectionKind,
            titleOverride: String = "",
            tracks: List<Track>,
            source: HomeCollectionSource,
            updatedToday: Boolean = false
        ) {
            if (!usedCollectionIds.add(id)) return
            val selected = tracks
                .asSequence()
                .filter(::isReliableCandidate)
                .distinctBy(::identityKey)
                .sortedWith(
                    compareByDescending<Track> { track ->
                        val key = identityKey(track)
                        val freshBoost = if (key in newReleaseKeys) 400 else 0
                        freshBoost + track.metadataConfidence * 2 + track.replayScore + track.cacheScore + track.energy / 3
                    }.thenBy { stableHash("$id|${identityKey(it)}") }
                )
                .take(collectionTrackLimit)
                .toList()
            if (selected.size < minimumCollectionSize) return
            val accentSeed = selected.take(4)
            val accentStart = accentSeed.firstOrNull()?.accentStart ?: 0xFF00E5FF.toInt()
            val accentEnd = accentSeed.getOrNull(1)?.accentEnd ?: accentSeed.firstOrNull()?.accentEnd ?: 0xFF7B42FF.toInt()
            result += HomeEditorialCollection(
                id = id,
                kind = kind,
                titleOverride = titleOverride,
                tracks = selected,
                source = source,
                updatedToday = updatedToday,
                accentStart = accentStart,
                accentEnd = accentEnd
            )
        }

        addCollection(
            id = "fresh",
            kind = HomeCollectionKind.Fresh,
            tracks = (fresh + newReleaseTracks),
            source = HomeCollectionSource.Editorial,
            updatedToday = fresh.any { track ->
                parseReleaseDate(track.releaseDate, nowMillis)?.let { startOfLocalDay(it) == todayStart } == true
            }
        )
        addCollection("local", HomeCollectionKind.Local, tracks = local, source = HomeCollectionSource.Editorial)
        addCollection("workout", HomeCollectionKind.Workout, tracks = workout, source = HomeCollectionSource.Levyra)
        addCollection("chill", HomeCollectionKind.Chill, tracks = chill, source = HomeCollectionSource.Levyra)
        addCollection("rap", HomeCollectionKind.Rap, tracks = rap, source = HomeCollectionSource.Editorial)
        addCollection("party", HomeCollectionKind.Party, tracks = party, source = HomeCollectionSource.Levyra)
        addCollection("focus", HomeCollectionKind.Focus, tracks = focus, source = HomeCollectionSource.Levyra)
        addCollection("pop", HomeCollectionKind.Pop, tracks = pop, source = HomeCollectionSource.Editorial)

        homeSections
            .asSequence()
            .filter { section -> section.title.isNotBlank() && section.tracks.size >= minimumCollectionSize }
            .take(2)
            .forEachIndexed { index, section ->
                addCollection(
                    id = "editorial-${stableHash(section.title)}-$index",
                    kind = HomeCollectionKind.Editorial,
                    titleOverride = section.title,
                    tracks = section.tracks,
                    source = HomeCollectionSource.Editorial
                )
            }

        addCollection("discovery", HomeCollectionKind.Discovery, tracks = discovery, source = HomeCollectionSource.Levyra)

        return result
            .distinctBy { collection -> collection.tracks.take(6).joinToString("|") { identityKey(it) } }
            .take(7)
    }

    private fun inferReleaseKind(track: Track): HomeSpotlightKind {
        val albumName = track.album.trim()
        val albumLike = track.albumBrowseId.isNotBlank() || track.trackNumber > 0 ||
            albumName.isNotBlank() && !albumName.equals(track.title, ignoreCase = true) &&
            !albumName.equals("YouTube Music", ignoreCase = true)
        return if (albumLike) HomeSpotlightKind.NewAlbum else HomeSpotlightKind.NewSingle
    }

    private fun parseReleaseDate(value: String, nowMillis: Long): Long? {
        val clean = value.trim()
        if (clean.isBlank()) return null
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "yyyy.MM.dd",
            "dd/MM/yyyy",
            "MM/dd/yyyy"
        )
        formats.forEach { pattern ->
            val parser = SimpleDateFormat(pattern, Locale.ROOT).apply {
                isLenient = false
                timeZone = TimeZone.getDefault()
            }
            val position = ParsePosition(0)
            val parsed = parser.parse(clean, position)
            if (parsed != null && position.index == clean.length) return parsed.time
        }
        if (clean.matches(Regex("^(19|20)\\d{2}$"))) {
            val year = clean.toIntOrNull() ?: return null
            val currentYear = Calendar.getInstance().apply { timeInMillis = nowMillis }.get(Calendar.YEAR)
            if (year == currentYear) {
                return Calendar.getInstance().apply {
                    clear()
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, Calendar.JANUARY)
                    set(Calendar.DAY_OF_MONTH, 1)
                }.timeInMillis
            }
        }
        return null
    }

    private fun startOfLocalDay(value: Long): Long {
        return Calendar.getInstance().apply {
            time = Date(value)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun normalizedTags(track: Track): Set<String> {
        return track.moodTags.mapTo(LinkedHashSet()) { it.trim().lowercase(Locale.ROOT) }
    }

    private fun isReliableCandidate(track: Track): Boolean {
        val title = track.title.trim()
        val artist = track.artist.trim()
        if (title.length < 2 || artist.length < 2) return false
        if (artist.equals("YouTube Music", ignoreCase = true) || artist.equals("YouTube", ignoreCase = true)) return false
        val combined = "$title $artist ${track.album}".lowercase(Locale.ROOT)
        return listOf(
            "playlist",
            "compilation",
            "best of",
            "sped up",
            "slowed",
            "nightcore"
        ).none(combined::contains)
    }

    private fun identityKey(track: Track): String {
        if (track.id.isNotBlank()) return track.id
        return "${track.title.trim().lowercase(Locale.ROOT)}|${track.artist.trim().lowercase(Locale.ROOT)}"
    }

    private fun stableHash(value: String): Int {
        var hash = 17
        value.forEach { char -> hash = hash * 31 + char.code }
        return hash.absoluteValue
    }
}
