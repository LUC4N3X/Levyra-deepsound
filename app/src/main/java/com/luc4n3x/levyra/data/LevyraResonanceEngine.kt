package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.SmartMusicProfile
import com.luc4n3x.levyra.domain.SmartMusicTasteSeed
import com.luc4n3x.levyra.domain.Track
import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.roundToInt

internal data class ResonanceNode(
    var weight: Double = 0.0,
    var halfLifeDays: Double = 30.0,
    var positiveSignals: Int = 0,
    var negativeSignals: Int = 0,
    var updatedAt: Long = 0L
)

internal data class ResolverArmState(
    var successes: Int = 0,
    var failures: Int = 0,
    var averageLatencyMs: Double = 0.0,
    var updatedAt: Long = 0L
)

internal data class SessionDna(
    var explorationPressure: Double = 0.5,
    var energyDirection: Double = 0.0,
    var familiarityNeed: Double = 0.35,
    var repetitionTolerance: Double = 0.55,
    var averageCompletion: Double = 0.5,
    var skipMomentum: Double = 0.0,
    var consecutiveSkips: Int = 0,
    var lastEnergy: Int = -1,
    var startedAt: Long = 0L,
    var lastEventAt: Long = 0L
)

internal data class LevyraResonanceState(
    var plays: Int = 0,
    var completedPlays: Int = 0,
    var favoriteSignals: Int = 0,
    var downloadSignals: Int = 0,
    var albumOpenSignals: Int = 0,
    var skipSignals: Int = 0,
    val artists: MutableMap<String, ResonanceNode> = LinkedHashMap(),
    val albums: MutableMap<String, ResonanceNode> = LinkedHashMap(),
    val moods: MutableMap<String, ResonanceNode> = LinkedHashMap(),
    val resolverArms: MutableMap<String, ResolverArmState> = LinkedHashMap(),
    val recentTrackIds: MutableList<String> = ArrayList(),
    val recentArtists: MutableList<String> = ArrayList(),
    var session: SessionDna = SessionDna(),
    var lastUpdated: Long = 0L
)

internal class LevyraResonanceEngine(
    private val clock: () -> Long = System::currentTimeMillis
) {
    fun recordPlayback(state: LevyraResonanceState, track: Track) {
        val now = clock()
        ensureSession(state, now)
        state.plays += 1
        touchTrack(state, track, 4.0, 30.0, now)
        recordResolverResult(state, track.source, success = true, latencyMs = 0L, now = now)
        val artist = artistKey(track)
        val previousEnergy = state.session.lastEnergy
        if (previousEnergy >= 0 && track.energy in 0..100) {
            val direction = ((track.energy - previousEnergy).toDouble() / 100.0).coerceIn(-1.0, 1.0)
            state.session.energyDirection = ema(state.session.energyDirection, direction, 0.28)
        }
        if (track.energy in 0..100) state.session.lastEnergy = track.energy
        val recentArtistCount = state.recentArtists.count { it == artist }
        state.session.repetitionTolerance = when {
            recentArtistCount >= 3 -> (state.session.repetitionTolerance - 0.12).coerceIn(0.05, 0.95)
            recentArtistCount == 0 -> (state.session.repetitionTolerance + 0.025).coerceIn(0.05, 0.95)
            else -> state.session.repetitionTolerance
        }
        appendBounded(state.recentTrackIds, trackIdentity(track), 24)
        if (artist.isNotBlank()) appendBounded(state.recentArtists, artist, 18)
        state.session.explorationPressure = (
            0.42 +
                state.session.averageCompletion * 0.28 -
                state.session.skipMomentum * 0.34 +
                state.session.repetitionTolerance * 0.12
            ).coerceIn(0.05, 0.95)
        state.session.lastEventAt = now
        state.lastUpdated = now
    }

    fun recordCompletion(state: LevyraResonanceState, track: Track) {
        val now = clock()
        ensureSession(state, now)
        state.completedPlays += 1
        touchTrack(state, track, 12.0, 60.0, now)
        state.session.averageCompletion = ema(state.session.averageCompletion, 1.0, 0.22)
        state.session.skipMomentum = (state.session.skipMomentum * 0.48).coerceIn(0.0, 1.0)
        state.session.consecutiveSkips = 0
        state.session.familiarityNeed = (state.session.familiarityNeed - 0.05).coerceIn(0.05, 0.95)
        state.session.explorationPressure = (state.session.explorationPressure + 0.07).coerceIn(0.05, 0.95)
        state.session.lastEventAt = now
        state.lastUpdated = now
    }

    fun recordFavorite(state: LevyraResonanceState, track: Track, added: Boolean) {
        val now = clock()
        ensureSession(state, now)
        if (added) {
            state.favoriteSignals += 1
            touchTrack(state, track, 18.0, 120.0, now)
            state.session.skipMomentum = (state.session.skipMomentum * 0.55).coerceIn(0.0, 1.0)
            state.session.consecutiveSkips = 0
        } else {
            touchTrack(state, track, -8.0, 18.0, now)
        }
        state.session.lastEventAt = now
        state.lastUpdated = now
    }

    fun recordDownload(state: LevyraResonanceState, track: Track) {
        val now = clock()
        ensureSession(state, now)
        state.downloadSignals += 1
        touchTrack(state, track, 22.0, 180.0, now)
        state.session.familiarityNeed = (state.session.familiarityNeed + 0.035).coerceIn(0.05, 0.95)
        state.session.lastEventAt = now
        state.lastUpdated = now
    }

    fun recordAlbumOpen(state: LevyraResonanceState, album: AlbumHit) {
        val now = clock()
        ensureSession(state, now)
        state.albumOpenSignals += 1
        val artist = cleanSeed(album.artist)
        val title = cleanSeed(album.title)
        if (artist.isNotBlank()) touchNode(state.artists, artist, 6.0, 45.0, now)
        if (artist.isNotBlank() && title.isNotBlank()) touchNode(state.albums, "$title|$artist", 10.0, 55.0, now)
        state.session.lastEventAt = now
        state.lastUpdated = now
    }

    fun recordTransition(
        state: LevyraResonanceState,
        track: Track,
        positionMs: Long,
        durationMs: Long = track.durationMs
    ) {
        val safeDuration = durationMs.takeIf { it > 0L } ?: return
        val ratio = (positionMs.coerceAtLeast(0L).toDouble() / safeDuration.toDouble()).coerceIn(0.0, 1.0)
        if (ratio >= COMPLETION_TRANSITION_THRESHOLD) return
        val now = clock()
        ensureSession(state, now)
        val rejection = (1.0 - ratio).coerceIn(0.15, 1.0)
        state.skipSignals += 1
        touchTrack(state, track, -(4.0 + rejection * 10.0), 10.0, now)
        state.session.averageCompletion = ema(state.session.averageCompletion, ratio, 0.24)
        state.session.skipMomentum = ema(state.session.skipMomentum, rejection, 0.38)
        state.session.consecutiveSkips = (state.session.consecutiveSkips + 1).coerceAtMost(12)
        state.session.familiarityNeed = (state.session.familiarityNeed + rejection * 0.2).coerceIn(0.05, 0.95)
        state.session.explorationPressure = (state.session.explorationPressure - rejection * 0.16).coerceIn(0.05, 0.95)
        val artist = artistKey(track)
        if (artist.isNotBlank() && state.recentArtists.takeLast(4).count { it == artist } >= 2) {
            state.session.repetitionTolerance = (state.session.repetitionTolerance - 0.1).coerceIn(0.05, 0.95)
        }
        state.session.lastEventAt = now
        state.lastUpdated = now
    }

    fun recordReplay(state: LevyraResonanceState, track: Track) {
        val now = clock()
        ensureSession(state, now)
        touchTrack(state, track, 14.0, 90.0, now)
        state.session.averageCompletion = ema(state.session.averageCompletion, 1.0, 0.18)
        state.session.skipMomentum = (state.session.skipMomentum * 0.42).coerceIn(0.0, 1.0)
        state.session.consecutiveSkips = 0
        state.session.familiarityNeed = (state.session.familiarityNeed + 0.08).coerceIn(0.05, 0.95)
        state.session.lastEventAt = now
        state.lastUpdated = now
    }

    fun recordResolverResult(
        state: LevyraResonanceState,
        source: String,
        success: Boolean,
        latencyMs: Long
    ) {
        recordResolverResult(state, source, success, latencyMs, clock())
    }

    fun toSmartProfile(state: LevyraResonanceState): SmartMusicProfile {
        val now = clock()
        val artists = state.artists.toSeeds(now) { key, weight ->
            val label = titleSeed(key)
            SmartMusicTasteSeed(label, "$label album", weight)
        }
        val albums = state.albums.toSeeds(now) { key, weight ->
            val parts = key.split('|', limit = 2)
            val title = titleSeed(parts.getOrNull(0).orEmpty())
            val artist = titleSeed(parts.getOrNull(1).orEmpty())
            SmartMusicTasteSeed(
                label = listOf(title, artist).filter(String::isNotBlank).joinToString(" • "),
                query = listOf(title, artist, "album").filter(String::isNotBlank).joinToString(" "),
                weight = weight
            )
        }
        val moods = state.moods.toSeeds(now) { key, weight ->
            val label = titleSeed(key)
            SmartMusicTasteSeed(label, "$label music", weight)
        }
        return SmartMusicProfile(
            plays = state.plays,
            completedPlays = state.completedPlays,
            favoriteSignals = state.favoriteSignals,
            downloadSignals = state.downloadSignals,
            albumOpenSignals = state.albumOpenSignals,
            topArtists = artists,
            topAlbums = albums,
            topMoods = moods,
            lastUpdated = state.lastUpdated
        )
    }

    fun rankRadioCandidates(
        state: LevyraResonanceState,
        candidates: List<Track>,
        currentQueue: List<Track>,
        currentTrack: Track?,
        limit: Int = 20
    ): List<Track> {
        val distinct = candidates
            .asSequence()
            .filter { it.title.isNotBlank() }
            .distinctBy(::trackIdentity)
            .toList()
        if (distinct.size <= 1) return distinct.take(limit.coerceAtLeast(0))
        val now = clock()
        val maxima = GraphMaxima(
            artist = state.artists.maxEffective(now),
            album = state.albums.maxEffective(now),
            mood = state.moods.maxEffective(now)
        )
        val queueTail = currentQueue.takeLast(8)
        val recoveryMode = state.session.consecutiveSkips >= 3 || state.session.skipMomentum >= 0.64
        val scored = distinct.map { track ->
            val artist = artistKey(track)
            val album = albumKey(track)
            val identity = trackIdentity(track)
            val artistAffinity = normalizedNodeScore(state.artists, artist, maxima.artist, now)
            val albumAffinity = normalizedNodeScore(state.albums, album, maxima.album, now)
            val moodAffinity = track.moodTags
                .map(::cleanSeed)
                .filter(String::isNotBlank)
                .map { normalizedNodeScore(state.moods, it, maxima.mood, now) }
                .averageOrZero()
            val tasteAffinity = (artistAffinity * 0.55 + albumAffinity * 0.3 + moodAffinity * 0.15)
                .coerceIn(0.0, 1.0)
            val continuity = sessionContinuity(state.session, currentTrack, track)
            val completion = completionProbability(state, track, artist)
            val context = contextAffinity(currentTrack, track)
            val wasRecent = identity in state.recentTrackIds
            val discovery = ((1.0 - artistAffinity) * 0.7 + if (wasRecent) 0.0 else 0.3).coerceIn(0.0, 1.0)
            val offline = offlineAvailability(track)
            val resolver = resolverReliability(state, track.source, now)
            val freshness = if (wasRecent) 0.0 else 1.0
            val recentArtistPenalty = recentArtistPenalty(state, artist)
            val rejectionPenalty = rejectionPenalty(state, artist, now)
            val queueSimilarity = queueTail.maxOfOrNull { similarity(track, it) } ?: 0.0
            var score =
                tasteAffinity * 0.27 +
                    continuity * 0.19 +
                    completion * 0.16 +
                    context * 0.11 +
                    discovery * state.session.explorationPressure * 0.1 +
                    offline * 0.07 +
                    resolver * 0.06 +
                    freshness * 0.04 -
                    recentArtistPenalty * 0.18 -
                    rejectionPenalty * 0.24 -
                    queueSimilarity * 0.12
            if (recoveryMode) {
                score += tasteAffinity * 0.22 + completion * 0.15 + offline * 0.08
                score -= discovery * 0.18 + recentArtistPenalty * 0.1
            }
            RankedCandidate(
                track = track,
                identity = identity,
                artist = artist,
                baseScore = score,
                tasteAffinity = tasteAffinity,
                completionProbability = completion,
                offlineAvailability = offline,
                discoveryValue = discovery
            )
        }
        return diversify(scored, currentTrack, recoveryMode, limit.coerceIn(0, 20)).map(RankedCandidate::track)
    }

    private fun diversify(
        candidates: List<RankedCandidate>,
        currentTrack: Track?,
        recoveryMode: Boolean,
        limit: Int
    ): List<RankedCandidate> {
        if (limit <= 0) return emptyList()
        val remaining = candidates.toMutableList()
        val selected = ArrayList<RankedCandidate>(minOf(limit, remaining.size))
        if (recoveryMode && remaining.isNotEmpty()) {
            val safest = remaining.maxWithOrNull(
                compareBy<RankedCandidate> {
                    it.baseScore + it.tasteAffinity * 0.2 + it.completionProbability * 0.14 + it.offlineAvailability * 0.08
                }.thenByDescending { it.identity }
            )
            if (safest != null) {
                selected += safest
                remaining.remove(safest)
            }
        }
        while (remaining.isNotEmpty() && selected.size < limit) {
            val previous = selected.lastOrNull()?.track ?: currentTrack
            val chosen = remaining.maxWithOrNull(
                compareBy<RankedCandidate> { candidate ->
                    val selectedSimilarity = selected.maxOfOrNull { similarity(candidate.track, it.track) } ?: 0.0
                    val adjacentArtistPenalty = if (
                        previous != null &&
                        artistKey(previous).isNotBlank() &&
                        artistKey(previous) == candidate.artist &&
                        remaining.any { it.artist != candidate.artist }
                    ) 0.34 else 0.0
                    candidate.baseScore - selectedSimilarity * 0.22 - adjacentArtistPenalty
                }.thenByDescending { it.identity }
            ) ?: break
            selected += chosen
            remaining.remove(chosen)
        }
        return selected
    }

    private fun touchTrack(
        state: LevyraResonanceState,
        track: Track,
        delta: Double,
        halfLifeDays: Double,
        now: Long
    ) {
        val artist = artistKey(track)
        val album = albumKey(track)
        if (artist.isNotBlank()) touchNode(state.artists, artist, delta, halfLifeDays, now)
        if (album.isNotBlank() && album.substringBefore('|') != cleanSeed(track.title)) {
            touchNode(state.albums, album, delta, max(halfLifeDays, 40.0), now)
        }
        track.moodTags
            .asSequence()
            .map(::cleanSeed)
            .filter(String::isNotBlank)
            .take(5)
            .forEach { mood ->
                val moodDelta = if (delta >= 0.0) max(1.0, delta * 0.5) else delta * 0.45
                touchNode(state.moods, mood, moodDelta, max(halfLifeDays * 0.75, 8.0), now)
            }
    }

    private fun touchNode(
        target: MutableMap<String, ResonanceNode>,
        key: String,
        delta: Double,
        halfLifeDays: Double,
        now: Long
    ) {
        if (key.isBlank() || !delta.isFinite()) return
        val node = target.getOrPut(key) { ResonanceNode(updatedAt = now, halfLifeDays = halfLifeDays) }
        val decayed = effectiveWeight(node, now)
        node.weight = (decayed + delta).coerceIn(0.0, 9999.0)
        node.halfLifeDays = if (delta >= 0.0) max(node.halfLifeDays, halfLifeDays) else minOf(node.halfLifeDays, halfLifeDays)
        if (delta >= 0.0) node.positiveSignals = (node.positiveSignals + 1).coerceAtMost(100_000)
        else node.negativeSignals = (node.negativeSignals + 1).coerceAtMost(100_000)
        node.updatedAt = now
        if (node.weight < 0.05 && node.negativeSignals > node.positiveSignals) target.remove(key)
    }

    private fun recordResolverResult(
        state: LevyraResonanceState,
        source: String,
        success: Boolean,
        latencyMs: Long,
        now: Long
    ) {
        val key = sourceKey(source)
        if (key.isBlank()) return
        val arm = state.resolverArms.getOrPut(key) { ResolverArmState(updatedAt = now) }
        if (success) arm.successes = (arm.successes + 1).coerceAtMost(100_000)
        else arm.failures = (arm.failures + 1).coerceAtMost(100_000)
        if (latencyMs > 0L) {
            arm.averageLatencyMs = if (arm.averageLatencyMs <= 0.0) latencyMs.toDouble() else ema(arm.averageLatencyMs, latencyMs.toDouble(), 0.18)
        }
        arm.updatedAt = now
    }

    private fun ensureSession(state: LevyraResonanceState, now: Long) {
        val session = state.session
        if (session.startedAt <= 0L || session.lastEventAt <= 0L || now - session.lastEventAt > SESSION_BREAK_MS) {
            state.session = SessionDna(startedAt = now, lastEventAt = now)
            state.recentTrackIds.clear()
            state.recentArtists.clear()
        }
    }

    private fun sessionContinuity(session: SessionDna, current: Track?, candidate: Track): Double {
        if (candidate.energy !in 0..100) return 0.5
        val currentEnergy = current?.energy?.takeIf { it in 0..100 } ?: session.lastEnergy.takeIf { it in 0..100 }
        if (currentEnergy == null) return 0.5
        val target = (currentEnergy + session.energyDirection * 24.0).coerceIn(0.0, 100.0)
        return (1.0 - abs(candidate.energy - target) / 100.0).coerceIn(0.0, 1.0)
    }

    private fun completionProbability(state: LevyraResonanceState, track: Track, artist: String): Double {
        val node = state.artists[artist]
        val signalReliability = if (node == null) 0.5 else {
            (node.positiveSignals + 1.0) / (node.positiveSignals + node.negativeSignals + 2.0)
        }
        val replay = (track.replayScore.coerceIn(0, 100) / 100.0)
        return (signalReliability * 0.45 + state.session.averageCompletion * 0.35 + replay * 0.2).coerceIn(0.0, 1.0)
    }

    private fun contextAffinity(current: Track?, candidate: Track): Double {
        if (current == null) return 0.5
        val mood = jaccard(current.moodTags.map(::cleanSeed).toSet(), candidate.moodTags.map(::cleanSeed).toSet())
        val duration = if (current.durationMs > 0L && candidate.durationMs > 0L) {
            (1.0 - abs(current.durationMs - candidate.durationMs).toDouble() / max(current.durationMs, candidate.durationMs).toDouble())
                .coerceIn(0.0, 1.0)
        } else 0.5
        val vocal = if (current.vocal in 0..100 && candidate.vocal in 0..100) {
            (1.0 - abs(current.vocal - candidate.vocal) / 100.0).coerceIn(0.0, 1.0)
        } else 0.5
        return (mood * 0.5 + duration * 0.25 + vocal * 0.25).coerceIn(0.0, 1.0)
    }

    private fun recentArtistPenalty(state: LevyraResonanceState, artist: String): Double {
        if (artist.isBlank()) return 0.0
        val recent = state.recentArtists.takeLast(8)
        if (recent.isEmpty()) return 0.0
        return (recent.count { it == artist }.toDouble() / 4.0).coerceIn(0.0, 1.0)
    }

    private fun rejectionPenalty(state: LevyraResonanceState, artist: String, now: Long): Double {
        val node = state.artists[artist] ?: return 0.0
        val total = node.positiveSignals + node.negativeSignals
        if (total <= 0) return 0.0
        val negativeRatio = node.negativeSignals.toDouble() / total.toDouble()
        val recency = if (now - node.updatedAt <= 14L * DAY_MS) 1.0 else 0.45
        return (negativeRatio * state.session.skipMomentum * recency).coerceIn(0.0, 1.0)
    }

    private fun resolverReliability(state: LevyraResonanceState, source: String, now: Long): Double {
        val arm = state.resolverArms[sourceKey(source)] ?: return 0.58
        val successRate = (arm.successes + 2.0) / (arm.successes + arm.failures + 4.0)
        val latencyScore = if (arm.averageLatencyMs <= 0.0) 0.65 else 1.0 / (1.0 + arm.averageLatencyMs / 1_200.0)
        val staleFactor = if (now - arm.updatedAt > 30L * DAY_MS) 0.75 else 1.0
        return ((successRate * 0.72 + latencyScore * 0.28) * staleFactor).coerceIn(0.0, 1.0)
    }

    private fun offlineAvailability(track: Track): Double {
        if (track.source.contains("offline", ignoreCase = true)) return 1.0
        if (track.streamUrl.startsWith("content://", ignoreCase = true) || track.streamUrl.startsWith("file://", ignoreCase = true)) return 1.0
        return (track.cacheScore.coerceIn(0, 100) / 100.0 * 0.75).coerceIn(0.0, 0.75)
    }

    private fun similarity(first: Track, second: Track): Double {
        if (trackIdentity(first) == trackIdentity(second)) return 1.0
        val sameArtist = artistKey(first).isNotBlank() && artistKey(first) == artistKey(second)
        val sameAlbum = albumKey(first).isNotBlank() && albumKey(first) == albumKey(second)
        val moods = jaccard(first.moodTags.map(::cleanSeed).toSet(), second.moodTags.map(::cleanSeed).toSet())
        val energy = if (first.energy in 0..100 && second.energy in 0..100) {
            (1.0 - abs(first.energy - second.energy) / 100.0).coerceIn(0.0, 1.0)
        } else 0.0
        return (
            (if (sameArtist) 0.62 else 0.0) +
                (if (sameAlbum) 0.2 else 0.0) +
                moods * 0.12 +
                energy * 0.06
            ).coerceIn(0.0, 1.0)
    }

    private fun normalizedNodeScore(
        map: Map<String, ResonanceNode>,
        key: String,
        maximum: Double,
        now: Long
    ): Double {
        if (key.isBlank() || maximum <= 0.0) return 0.0
        return (effectiveWeight(map[key] ?: return 0.0, now) / maximum).coerceIn(0.0, 1.0)
    }

    private fun Map<String, ResonanceNode>.maxEffective(now: Long): Double =
        values.maxOfOrNull { effectiveWeight(it, now) }?.coerceAtLeast(1.0) ?: 1.0

    private fun Map<String, ResonanceNode>.toSeeds(
        now: Long,
        factory: (String, Int) -> SmartMusicTasteSeed
    ): List<SmartMusicTasteSeed> = entries
        .mapNotNull { (key, node) ->
            val effective = effectiveWeight(node, now)
            if (key.isBlank() || effective < 0.5) null else key to effective.roundToInt().coerceIn(1, 9999)
        }
        .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
        .take(16)
        .map { (key, weight) -> factory(key, weight) }

    private fun effectiveWeight(node: ResonanceNode, now: Long): Double {
        if (node.weight <= 0.0) return 0.0
        if (node.updatedAt <= 0L || now <= node.updatedAt) return node.weight
        val ageDays = (now - node.updatedAt).toDouble() / DAY_MS.toDouble()
        val halfLife = node.halfLifeDays.coerceAtLeast(1.0)
        return node.weight * exp(-LN_2 * ageDays / halfLife)
    }

    private fun trackIdentity(track: Track): String {
        val id = track.id.trim().lowercase(Locale.ROOT)
        if (id.isNotBlank()) return "id:$id"
        return "meta:${artistKey(track)}|${cleanSeed(track.title)}"
    }

    private fun artistKey(track: Track): String = cleanSeed(track.artist)

    private fun albumKey(track: Track): String {
        val artist = artistKey(track)
        val album = cleanSeed(track.album)
        return if (artist.isBlank() || album.isBlank()) "" else "$album|$artist"
    }

    private fun sourceKey(source: String): String = cleanSeed(source).take(48)

    private fun cleanSeed(value: String): String = value
        .trim()
        .replace(Regex("\\s+"), " ")
        .replace(Regex("(?i)\\s*[(\\[].*?(official|video|lyrics|audio|prod\\.|feat\\.).*?[)\\]]"), "")
        .trim(' ', '.', '-', '_', '•')
        .take(80)
        .lowercase(Locale.ROOT)

    private fun titleSeed(value: String): String = value
        .split(' ')
        .filter(String::isNotBlank)
        .joinToString(" ") { token ->
            token.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString() }
        }

    private fun jaccard(first: Set<String>, second: Set<String>): Double {
        if (first.isEmpty() || second.isEmpty()) return 0.0
        val union = first union second
        if (union.isEmpty()) return 0.0
        return (first intersect second).size.toDouble() / union.size.toDouble()
    }

    private fun List<Double>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()

    private fun ema(current: Double, incoming: Double, alpha: Double): Double =
        (current * (1.0 - alpha) + incoming * alpha)

    private fun <T> appendBounded(target: MutableList<T>, value: T, limit: Int) {
        target += value
        while (target.size > limit) target.removeAt(0)
    }

    private data class GraphMaxima(
        val artist: Double,
        val album: Double,
        val mood: Double
    )

    private data class RankedCandidate(
        val track: Track,
        val identity: String,
        val artist: String,
        val baseScore: Double,
        val tasteAffinity: Double,
        val completionProbability: Double,
        val offlineAvailability: Double,
        val discoveryValue: Double
    )

    private companion object {
        const val DAY_MS = 86_400_000L
        const val SESSION_BREAK_MS = 30L * 60L * 1_000L
        const val COMPLETION_TRANSITION_THRESHOLD = 0.82
        const val LN_2 = 0.6931471805599453
    }
}
