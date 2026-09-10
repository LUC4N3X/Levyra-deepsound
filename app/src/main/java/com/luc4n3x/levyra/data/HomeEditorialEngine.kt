package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.HomeCollectionKind
import com.luc4n3x.levyra.domain.HomeCollectionSource
import com.luc4n3x.levyra.domain.HomeEditorialCollection
import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.HomeSpotlightCandidate
import com.luc4n3x.levyra.domain.HomeSpotlightKind
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.Track
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale
import kotlin.math.absoluteValue

object HomeEditorialEngine {
    private const val collectionTrackLimit = 18
    private const val minimumCollectionSize = 4
    private const val targetCollectionCount = 6
    private const val stableCollectionSlots = 3

    private val localReleaseDateFormatters = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE.withResolverStyle(ResolverStyle.STRICT),
        DateTimeFormatter.ofPattern("uuuu/MM/dd", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT),
        DateTimeFormatter.ofPattern("uuuu.MM.dd", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT),
        DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT),
        DateTimeFormatter.ofPattern("MM/dd/uuuu", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT)
    )
    private val artworkSizeSuffixPattern = Regex(
        "=(?:w\\d+-h\\d+|s\\d+)(?:-[a-z0-9]+)*$",
        RegexOption.IGNORE_CASE
    )

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
        preferenceScore: (Track) -> Int = { 0 },
        nowMillis: Long = System.currentTimeMillis()
    ): List<HomeSpotlightCandidate> {
        val sourcePriority = LinkedHashMap<String, Int>()
        val candidates = LinkedHashMap<String, Track>()

        fun append(tracks: List<Track>, priority: Int) {
            tracks.forEach { track ->
                if (!isReliableCandidate(track)) return@forEach
                val key = identityKey(track)
                candidates.putIfAbsent(key, track)
                sourcePriority[key] = maxOf(sourcePriority[key] ?: Int.MIN_VALUE, priority)
            }
        }

        if (showNewReleases) append(newReleaseTracks, 1_400)
        if (showPersonalOrbit) append(personalTracks, 1_250)
        if (showResonance) append(resonanceTracks, 1_100)
        append(quickPickTracks, 950)
        fallbackSections.forEachIndexed { index, tracks -> append(tracks, 780 - index.coerceAtMost(5) * 35) }
        append(chartTracks, 1_300)

        val newReleaseKeys = if (showNewReleases) {
            newReleaseTracks.asSequence().map(::identityKey).toHashSet()
        } else {
            emptySet()
        }
        val chartKeys = chartTracks.asSequence().map(::identityKey).toHashSet()
        val today = localDate(nowMillis)
        val daySeed = localDayKey(nowMillis)

        val rankedCandidates = candidates.mapNotNull { (key, track) ->
            val inNewReleases = key in newReleaseKeys
            val inCharts = key in chartKeys
            val releaseDate = if (inNewReleases) parseReleaseDate(track.releaseDate)?.let(::localDate) else null
            val ageDays = releaseDate?.let { calendarDayAge(it, today) }?.takeIf { it >= 0 }
            val isFreshRelease = inNewReleases && ageDays != null && ageDays <= 7
            val kind = when {
                isFreshRelease && ageDays == 0 -> HomeSpotlightKind.ReleasedToday
                isFreshRelease -> HomeSpotlightKind.JustReleased
                inCharts -> HomeSpotlightKind.ChartTrending
                else -> HomeSpotlightKind.LevyraSelect
            }
            val freshnessScore = when {
                isFreshRelease && ageDays == 0 -> 12_000
                isFreshRelease -> 10_000 - ageDays!! * 220
                inCharts -> 6_000
                else -> 0
            }
            val artworkScore = when {
                track.largeThumbnailUrl.isNotBlank() -> 420
                track.thumbnailUrl.isNotBlank() -> 260
                else -> 0
            }
            val metadataScore = track.metadataConfidence.coerceIn(0, 100) * 2
            val engagementScore = (track.replayScore + track.cacheScore + track.energy / 2).coerceIn(0, 260)
            val preferenceBoost = preferenceScore(track).coerceIn(0, 6_000)
            val stableDailyJitter = stableHash("$daySeed|${track.id}|${track.title}|${track.artist}") % 97
            HomeSpotlightCandidate(
                track = track,
                kind = kind,
                score = freshnessScore + sourcePriority.getValue(key) + artworkScore + metadataScore + engagementScore + preferenceBoost + stableDailyJitter,
                releaseAgeDays = ageDays
            )
        }.sortedWith(
            compareByDescending<HomeSpotlightCandidate> { it.score }
                .thenBy { stableHash("$daySeed|${it.track.id}|${it.track.title}") }
        )

        val freshCandidates = rankedCandidates.filter { candidate ->
            candidate.kind == HomeSpotlightKind.ReleasedToday ||
                candidate.kind == HomeSpotlightKind.JustReleased
        }
        return freshCandidates.ifEmpty { rankedCandidates }
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
        includeFresh: Boolean = true,
        nowMillis: Long = System.currentTimeMillis()
    ): List<HomeEditorialCollection> {
        val visibleReleaseTracks = if (includeFresh) newReleaseTracks else emptyList()
        val sectionTracks = homeSections.flatMap { it.tracks }
        val pool = (
            visibleReleaseTracks +
                personalTracks +
                resonanceTracks +
                quickPickTracks +
                chartTracks +
                sectionTracks +
                favorites +
                libraryTracks
            )
            .asSequence()
            .filter(::isReliableCandidate)
            .distinctBy(::identityKey)
            .toList()

        if (pool.size < minimumCollectionSize) return emptyList()

        val today = localDate(nowMillis)
        val daySeed = localDayKey(nowMillis)
        val chartKeys = chartTracks.asSequence().map(::identityKey).toHashSet()
        val personalKeys = (personalTracks + favorites + libraryTracks)
            .asSequence()
            .map(::identityKey)
            .toHashSet()
        val editorialKeys = sectionTracks.asSequence().map(::identityKey).toHashSet()
        val resonanceKeys = resonanceTracks.asSequence().map(::identityKey).toHashSet()
        val quickPickKeys = quickPickTracks.asSequence().map(::identityKey).toHashSet()
        val newReleaseKeys = visibleReleaseTracks.asSequence().map(::identityKey).toHashSet()

        val local = pool.filter { "local" in normalizedTags(it) }
        val workout = pool.filter {
            val tags = normalizedTags(it)
            it.energy >= 86 || tags.any { tag -> tag in workoutTags }
        }
        val chill = pool.filter {
            val tags = normalizedTags(it)
            it.energy <= 70 || tags.any { tag -> tag in chillTags }
        }
        val focus = pool.filter {
            val tags = normalizedTags(it)
            tags.any { tag -> tag in focusTags } || it.energy in 65..82 && it.vocal <= 72
        }
        val party = pool.filter {
            val tags = normalizedTags(it)
            it.energy >= 84 && tags.any { tag -> tag in partyTags }
        }
        val rap = pool.filter { track -> normalizedTags(track).any { it in rapTags } }
        val pop = pool.filter { track -> normalizedTags(track).any { it in popTags } }
        val discovery = (chartTracks + sectionTracks + personalTracks + quickPickTracks + pool)
            .asSequence()
            .filter(::isReliableCandidate)
            .distinctBy(::identityKey)
            .toList()
        val fresh = if (includeFresh) {
            pool.filter { track ->
                identityKey(track) in newReleaseKeys &&
                    parseReleaseDate(track.releaseDate)
                        ?.let(::localDate)
                        ?.let { releaseDate -> calendarDayAge(releaseDate, today) in 0..7 } == true
            }
        } else {
            emptyList()
        }

        val result = mutableListOf<HomeEditorialCollection>()

        fun sourceBoost(track: Track): Int {
            val key = identityKey(track)
            var score = 0
            if (key in personalKeys) score += 1_050
            if (key in chartKeys) score += 900
            if (key in editorialKeys) score += 800
            if (key in newReleaseKeys) score += 720
            if (key in resonanceKeys) score += 560
            if (key in quickPickKeys) score += 480
            return score
        }

        fun addCollection(
            id: String,
            kind: HomeCollectionKind,
            tracks: List<Track>,
            source: HomeCollectionSource,
            titleOverride: String = "",
            updatedToday: Boolean = false,
            allowSmartFill: Boolean = true
        ) {
            val primaryKeys = tracks.asSequence().map(::identityKey).toHashSet()
            val candidatePool = if (allowSmartFill) {
                (tracks + pool).asSequence().distinctBy(::identityKey).toList()
            } else {
                tracks.asSequence().distinctBy(::identityKey).toList()
            }
            val selected = candidatePool
                .asSequence()
                .filter(::isReliableCandidate)
                .sortedWith(
                    compareByDescending<Track> { track ->
                        val primaryBoost = if (identityKey(track) in primaryKeys) 3_200 else 0
                        primaryBoost +
                            sourceBoost(track) +
                            kindAffinity(track, kind) +
                            track.metadataConfidence.coerceIn(0, 100) * 3 +
                            track.replayScore.coerceIn(0, 100) * 2 +
                            track.cacheScore.coerceIn(0, 100) +
                            stableHash("$daySeed|$id|${identityKey(track)}") % 211
                    }.thenBy { track -> stableHash("$id|${identityKey(track)}") }
                )
                .take(collectionTrackLimit)
                .toList()
            if (selected.size < minimumCollectionSize) return
            val accentSeed = selected.take(4)
            val accentStart = accentSeed.firstOrNull()?.accentStart ?: 0xFF00E5FF.toInt()
            val accentEnd = accentSeed.getOrNull(1)?.accentEnd
                ?: accentSeed.firstOrNull()?.accentEnd
                ?: 0xFF7B42FF.toInt()
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

        if (includeFresh && fresh.isNotEmpty()) {
            addCollection(
                id = "fresh",
                kind = HomeCollectionKind.Fresh,
                tracks = fresh,
                source = HomeCollectionSource.Editorial,
                updatedToday = fresh.any { track ->
                    parseReleaseDate(track.releaseDate)?.let(::localDate) == today
                },
                allowSmartFill = false
            )
        }

        fun addThemedCollection(
            id: String,
            kind: HomeCollectionKind,
            tracks: List<Track>,
            source: HomeCollectionSource
        ) {
            if (tracks.isNotEmpty()) {
                addCollection(id, kind, tracks, source)
            }
        }

        addThemedCollection("local", HomeCollectionKind.Local, local, HomeCollectionSource.Editorial)
        addThemedCollection("workout", HomeCollectionKind.Workout, workout, HomeCollectionSource.Levyra)
        addThemedCollection("chill", HomeCollectionKind.Chill, chill, HomeCollectionSource.Levyra)
        addThemedCollection("rap", HomeCollectionKind.Rap, rap, HomeCollectionSource.Editorial)
        addThemedCollection("party", HomeCollectionKind.Party, party, HomeCollectionSource.Levyra)
        addThemedCollection("focus", HomeCollectionKind.Focus, focus, HomeCollectionSource.Levyra)
        addThemedCollection("pop", HomeCollectionKind.Pop, pop, HomeCollectionSource.Editorial)
        if (discovery.isNotEmpty()) {
            addCollection(
                id = "discovery",
                kind = HomeCollectionKind.Discovery,
                tracks = discovery,
                source = if (chartKeys.isNotEmpty()) HomeCollectionSource.Charts else HomeCollectionSource.Levyra
            )
        }

        homeSections
            .asSequence()
            .filter { it.title.isNotBlank() }
            .forEachIndexed { index, section ->
                addCollection(
                    id = "editorial-${stableHash(section.title)}-$index",
                    kind = HomeCollectionKind.Editorial,
                    tracks = section.tracks,
                    source = HomeCollectionSource.Editorial,
                    titleOverride = section.title,
                    allowSmartFill = true
                )
            }

        val distinct = result
            .sortedWith(
                compareByDescending<HomeEditorialCollection> { collectionQuality(it) }
                    .thenBy { stableHash("dedupe|$daySeed|${it.id}") }
            )
            .distinctBy(::collectionFingerprint)
            .toMutableList()
        if (distinct.size < targetCollectionCount) {
            val fallbackKinds = listOf(
                HomeCollectionKind.Discovery,
                HomeCollectionKind.Pop,
                HomeCollectionKind.Chill,
                HomeCollectionKind.Focus,
                HomeCollectionKind.Workout,
                HomeCollectionKind.Party,
                HomeCollectionKind.Rap,
                HomeCollectionKind.Local
            ).sortedWith(
                compareByDescending<HomeCollectionKind> { kind ->
                    pool.sumOf { track -> kindAffinity(track, kind) } +
                        when (kind) {
                            HomeCollectionKind.Discovery -> chartKeys.size * 120
                            HomeCollectionKind.Local -> editorialKeys.size * 80
                            else -> 0
                        }
                }.thenBy { kind -> stableHash("$daySeed|${kind.name}") }
            )
            var fallbackIndex = 0
            while (distinct.size < targetCollectionCount && fallbackIndex < fallbackKinds.size * 3) {
                val kind = fallbackKinds[fallbackIndex % fallbackKinds.size]
                if (distinct.any { it.kind == kind }) {
                    fallbackIndex++
                    continue
                }
                val id = "smart-${kind.name.lowercase(Locale.ROOT)}-$fallbackIndex"
                val before = result.size
                val fallbackSeedTracks = pool
                    .sortedBy { track -> stableHash("$daySeed|$id|${identityKey(track)}") }
                    .take((minimumCollectionSize + 4).coerceAtMost(pool.size))
                addCollection(
                    id = id,
                    kind = kind,
                    tracks = fallbackSeedTracks,
                    source = if (chartKeys.isNotEmpty()) HomeCollectionSource.Charts else HomeCollectionSource.Levyra,
                    allowSmartFill = true
                )
                if (result.size > before) {
                    val candidate = result.last()
                    if (distinct.none { collectionFingerprint(it) == collectionFingerprint(candidate) }) {
                        distinct += candidate
                    }
                }
                fallbackIndex++
            }
        }

        if (distinct.size < targetCollectionCount) {
            val selectedIds = distinct.asSequence().map { it.id }.toHashSet()
            val selectedKinds = distinct.asSequence().map { it.kind }.toHashSet()
            val selectedFingerprints = distinct.asSequence().map(::collectionFingerprint).toHashSet()
            val emergencyCandidates = result
                .asSequence()
                .filter { it.id !in selectedIds }
                .sortedWith(
                    compareByDescending<HomeEditorialCollection> { collectionQuality(it) }
                        .thenBy { stableHash("emergency|$daySeed|${it.id}") }
                )
                .toList()

            emergencyCandidates
                .asSequence()
                .filter { it.kind !in selectedKinds }
                .forEach { candidate ->
                    val fingerprint = collectionFingerprint(candidate)
                    if (distinct.size < targetCollectionCount && fingerprint !in selectedFingerprints) {
                        distinct += candidate
                        selectedIds += candidate.id
                        selectedKinds += candidate.kind
                        selectedFingerprints += fingerprint
                    }
                }

            emergencyCandidates
                .asSequence()
                .filter { it.id !in selectedIds }
                .forEach { candidate ->
                    val fingerprint = collectionFingerprint(candidate)
                    if (distinct.size < targetCollectionCount && fingerprint !in selectedFingerprints) {
                        distinct += candidate
                        selectedIds += candidate.id
                        selectedFingerprints += fingerprint
                    }
                }
        }

        return withDistinctPrimaryArtwork(
            collections = selectCollectionsForDay(distinct, nowMillis),
            fallbackPool = pool,
            daySeed = daySeed
        )
    }

    private fun selectCollectionsForDay(
        collections: List<HomeEditorialCollection>,
        nowMillis: Long
    ): List<HomeEditorialCollection> {
        if (collections.isEmpty()) return emptyList()
        val daySeed = localDayKey(nowMillis)
        val ranked = collections.sortedWith(
            compareByDescending<HomeEditorialCollection> { collectionQuality(it) }
                .thenBy { stableHash("stable|${it.id}") }
        )
        if (ranked.size <= targetCollectionCount) return ranked.take(targetCollectionCount)

        val stable = ranked.take(stableCollectionSlots)
        val stableIds = stable.asSequence().map { it.id }.toHashSet()
        val rotating = ranked
            .asSequence()
            .filterNot { it.id in stableIds }
            .sortedWith(
                compareBy<HomeEditorialCollection> { stableHash("$daySeed|${it.id}") }
                    .thenByDescending(::collectionQuality)
            )
            .toList()
        return (stable + rotating)
            .distinctBy { it.id }
            .take(targetCollectionCount)
    }

    private fun withDistinctPrimaryArtwork(
        collections: List<HomeEditorialCollection>,
        fallbackPool: List<Track>,
        daySeed: Int
    ): List<HomeEditorialCollection> {
        if (collections.size < 2) return collections
        val candidatesByCollection = collections.map { collection ->
            val existingKeys = collection.tracks.asSequence().map(::identityKey).toHashSet()
            val fallbackTracks = if (collection.kind == HomeCollectionKind.Fresh) {
                emptyList()
            } else {
                fallbackPool
                    .asSequence()
                    .filterNot { identityKey(it) in existingKeys }
                    .sortedWith(
                        compareByDescending<Track> { track ->
                            kindAffinity(track, collection.kind) +
                                track.metadataConfidence.coerceIn(0, 100) * 3 +
                                track.replayScore.coerceIn(0, 100) * 2 +
                                track.cacheScore.coerceIn(0, 100)
                        }.thenBy { track ->
                            stableHash("cover|$daySeed|${collection.id}|${identityKey(track)}")
                        }
                    )
                    .toList()
            }
            (collection.tracks + fallbackTracks)
                .asSequence()
                .distinctBy(::identityKey)
                .filter { artworkIdentity(it).isNotBlank() }
                .toList()
        }
        val ownerByArtwork = HashMap<String, Int>()

        fun assign(collectionIndex: Int, visitedArtwork: MutableSet<String>): Boolean {
            candidatesByCollection[collectionIndex].forEach { track ->
                val artwork = artworkIdentity(track)
                if (!visitedArtwork.add(artwork)) return@forEach
                val currentOwner = ownerByArtwork[artwork]
                if (currentOwner == null || assign(currentOwner, visitedArtwork)) {
                    ownerByArtwork[artwork] = collectionIndex
                    return true
                }
            }
            return false
        }

        collections.indices.forEach { index ->
            if (!assign(index, HashSet())) return collections
        }

        val artworkByCollection = arrayOfNulls<String>(collections.size)
        ownerByArtwork.forEach { (artwork, collectionIndex) ->
            artworkByCollection[collectionIndex] = artwork
        }
        return collections.mapIndexed { index, collection ->
            val assignedArtwork = artworkByCollection[index] ?: return@mapIndexed collection
            val primary = candidatesByCollection[index]
                .firstOrNull { artworkIdentity(it) == assignedArtwork }
                ?: return@mapIndexed collection
            val primaryKey = identityKey(primary)
            if (collection.tracks.firstOrNull()?.let(::identityKey) == primaryKey) return@mapIndexed collection
            collection.copy(
                tracks = buildList(collection.tracks.size.coerceAtLeast(minimumCollectionSize)) {
                    add(primary)
                    collection.tracks.forEach { track ->
                        if (identityKey(track) != primaryKey && size < collectionTrackLimit) add(track)
                    }
                }
            )
        }
    }

    private fun collectionQuality(collection: HomeEditorialCollection): Int {
        val tracks = collection.tracks
        if (tracks.isEmpty()) return 0
        val averageMetadata = tracks.sumOf { it.metadataConfidence.coerceIn(0, 100) } / tracks.size
        val artworkCount = tracks.count { it.largeThumbnailUrl.isNotBlank() || it.thumbnailUrl.isNotBlank() }
        val uniqueArtists = tracks
            .map { it.artist.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotBlank() }
            .distinct()
            .size
        val sourceBoost = when (collection.source) {
            HomeCollectionSource.Editorial -> 260
            HomeCollectionSource.Charts -> 240
            HomeCollectionSource.Levyra -> 180
        }
        val freshnessBoost = if (collection.updatedToday) 300 else 0
        val personalizationBoost = if (
            collection.kind == HomeCollectionKind.Editorial && collection.titleOverride.isNotBlank()
        ) {
            720
        } else {
            0
        }
        return averageMetadata * 4 +
            artworkCount * 24 +
            uniqueArtists * 18 +
            tracks.size.coerceAtMost(collectionTrackLimit) * 8 +
            sourceBoost +
            freshnessBoost +
            personalizationBoost
    }

    private fun kindAffinity(track: Track, kind: HomeCollectionKind): Int {
        val tags = normalizedTags(track)
        return when (kind) {
            HomeCollectionKind.Fresh -> if (track.releaseDate.isNotBlank()) 1_100 else 0
            HomeCollectionKind.Local -> if ("local" in tags) 1_350 else 0
            HomeCollectionKind.Workout -> {
                val tagBoost = if (tags.any { it in workoutTags }) 1_350 else 0
                tagBoost + ((track.energy - 72).coerceAtLeast(0) * 18)
            }
            HomeCollectionKind.Chill -> {
                val tagBoost = if (tags.any { it in chillTags }) 1_350 else 0
                tagBoost + ((76 - track.energy).coerceAtLeast(0) * 14)
            }
            HomeCollectionKind.Focus -> {
                val tagBoost = if (tags.any { it in focusTags }) 1_350 else 0
                val energyBoost = if (track.energy in 58..84) 260 else 0
                val vocalBoost = (75 - track.vocal).coerceAtLeast(0) * 8
                tagBoost + energyBoost + vocalBoost
            }
            HomeCollectionKind.Party -> {
                val tagBoost = if (tags.any { it in partyTags }) 1_350 else 0
                tagBoost + ((track.energy - 74).coerceAtLeast(0) * 16)
            }
            HomeCollectionKind.Rap -> if (tags.any { it in rapTags }) 1_500 else 0
            HomeCollectionKind.Pop -> if (tags.any { it in popTags }) 1_450 else 0
            HomeCollectionKind.Discovery -> {
                track.replayScore.coerceIn(0, 100) * 5 + track.energy.coerceIn(0, 100) * 2
            }
            HomeCollectionKind.Editorial -> track.metadataConfidence.coerceIn(0, 100) * 4
        }
    }

    private fun collectionFingerprint(collection: HomeEditorialCollection): String {
        return collection.tracks.take(6).joinToString("|") { identityKey(it) }
    }

    private fun artworkIdentity(track: Track): String {
        val rawArtwork = track.thumbnailUrl
            .trim()
            .ifBlank { track.largeThumbnailUrl.trim() }
            .ifBlank { LevyraPersonalOrbit.youtubeFallbackArtwork(track).orEmpty() }
        if (rawArtwork.isBlank()) return ""
        return rawArtwork
            .substringBefore('?')
            .replace(artworkSizeSuffixPattern, "")
            .trimEnd('/')
            .lowercase(Locale.ROOT)
    }

    private fun parseReleaseDate(value: String): Long? {
        val clean = value.trim()
        if (clean.isBlank()) return null

        runCatching { Instant.parse(clean).toEpochMilli() }.getOrNull()?.let { return it }
        runCatching {
            OffsetDateTime.parse(clean, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .toInstant()
                .toEpochMilli()
        }.getOrNull()?.let { return it }
        runCatching {
            LocalDateTime.parse(clean, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrNull()?.let { return it }
        localReleaseDateFormatters.forEach { formatter ->
            runCatching {
                LocalDate.parse(clean, formatter)
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            }.getOrNull()?.let { return it }
        }
        return null
    }

    private fun localDate(value: Long, zoneId: ZoneId = ZoneId.systemDefault()): LocalDate {
        return Instant.ofEpochMilli(value).atZone(zoneId).toLocalDate()
    }

    private fun calendarDayAge(releaseDate: LocalDate, today: LocalDate): Int {
        return ChronoUnit.DAYS.between(releaseDate, today).toInt()
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

    private val workoutTags = setOf("gym", "workout", "energy", "training", "running")
    private val chillTags = setOf("chill", "relax", "lofi", "calm", "sleep", "mood", "sad")
    private val focusTags = setOf("focus", "study", "drive", "electronic", "instrumental")
    private val partyTags = setOf("party", "dance", "latin", "club", "funk", "edm")
    private val rapTags = setOf("rap", "hiphop", "hip-hop", "drill", "trap")
    private val popTags = setOf("pop", "hits", "hit", "anthem")
}