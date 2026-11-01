package com.luc4n3x.levyra.domain

import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class MoodPoint(val energy: Float, val valence: Float) {
    fun clamped(): MoodPoint = MoodPoint(energy.finiteOr(0.5f).coerceIn(0f, 1f), valence.finiteOr(0.5f).coerceIn(0f, 1f))
}

private fun Float.finiteOr(fallback: Float): Float = if (isFinite()) this else fallback

enum class MoodCurvePreset { Sunrise, WindDown, Summit, Wave, Steady }

data class MoodCurve(val points: List<MoodPoint>) {
    val normalized: List<MoodPoint> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val clean = points.map(MoodPoint::clamped)
        when {
            clean.isEmpty() -> List(2) { MoodPoint(0.5f, 0.5f) }
            clean.size == 1 -> List(2) { clean.first() }
            clean.size > MAX_POINTS -> List(MAX_POINTS) { index -> clean[index * (clean.size - 1) / (MAX_POINTS - 1)] }
            else -> clean
        }
    }

    fun sample(position: Float): MoodPoint {
        val nodes = normalized
        val t = position.finiteOr(0f).coerceIn(0f, 1f)
        return MoodPoint(
            MonotoneCurve.evaluate(nodes.map { it.energy }, t),
            MonotoneCurve.evaluate(nodes.map { it.valence }, t)
        )
    }

    fun withPoint(index: Int, point: MoodPoint): MoodCurve {
        if (index !in points.indices) return this
        return MoodCurve(points.toMutableList().also { it[index] = point.clamped() })
    }

    companion object {
        const val MIN_POINTS = 2
        const val MAX_POINTS = 9
        const val DEFAULT_POINTS = 6

        fun preset(kind: MoodCurvePreset, count: Int = DEFAULT_POINTS): MoodCurve {
            val size = count.coerceIn(MIN_POINTS, MAX_POINTS)
            return MoodCurve(List(size) { index ->
                val t = if (size == 1) 0f else index / (size - 1f)
                when (kind) {
                    MoodCurvePreset.Sunrise -> MoodPoint(0.18f + 0.7f * t, 0.4f + 0.45f * t)
                    MoodCurvePreset.WindDown -> MoodPoint(0.82f - 0.68f * t, 0.65f - 0.25f * t)
                    MoodCurvePreset.Summit -> MoodPoint(0.25f + 0.65f * (1f - abs(2f * t - 1f)), 0.5f + 0.3f * (1f - abs(2f * t - 1f)))
                    MoodCurvePreset.Wave -> MoodPoint(0.5f + 0.32f * kotlin.math.sin(t * 2f * Math.PI.toFloat()), 0.55f)
                    MoodCurvePreset.Steady -> MoodPoint(0.55f, 0.6f)
                }.clamped()
            })
        }
    }
}

internal object MonotoneCurve {
    fun evaluate(values: List<Float>, t: Float): Float {
        val n = values.size
        if (n == 0) return 0.5f
        if (n == 1) return values[0]
        val segments = n - 1
        val scaled = t * segments
        val index = min(scaled.toInt(), segments - 1)
        val local = scaled - index
        val tangents = tangents(values)
        val y0 = values[index]
        val y1 = values[index + 1]
        val m0 = tangents[index]
        val m1 = tangents[index + 1]
        val t2 = local * local
        val t3 = t2 * local
        val value = (2 * t3 - 3 * t2 + 1) * y0 + (t3 - 2 * t2 + local) * m0 + (-2 * t3 + 3 * t2) * y1 + (t3 - t2) * m1
        return value.coerceIn(min(y0, y1), max(y0, y1))
    }

    private fun tangents(values: List<Float>): FloatArray {
        val n = values.size
        val slopes = FloatArray(n - 1) { values[it + 1] - values[it] }
        val tangents = FloatArray(n)
        tangents[0] = slopes[0]
        tangents[n - 1] = slopes[n - 2]
        for (i in 1 until n - 1) {
            tangents[i] = if (slopes[i - 1] * slopes[i] <= 0f) 0f else (slopes[i - 1] + slopes[i]) / 2f
        }
        for (i in 0 until n - 1) {
            val slope = slopes[i]
            if (slope == 0f) {
                tangents[i] = 0f
                tangents[i + 1] = 0f
                continue
            }
            val a = tangents[i] / slope
            val b = tangents[i + 1] / slope
            val magnitude = a * a + b * b
            if (magnitude > 9f) {
                val scale = 3f / sqrt(magnitude)
                tangents[i] = scale * a * slope
                tangents[i + 1] = scale * b * slope
            }
        }
        return tangents
    }
}

enum class MoodCategoryKind(val energy: Float, val valence: Float) {
    ENERGIZE(0.85f, 0.72f),
    WORKOUT(0.95f, 0.6f),
    PARTY(0.9f, 0.82f),
    FEEL_GOOD(0.66f, 0.88f),
    ROMANCE(0.38f, 0.7f),
    CHILL(0.3f, 0.6f),
    FOCUS(0.35f, 0.5f),
    SLEEP(0.1f, 0.48f),
    SAD(0.28f, 0.16f),
    COMMUTE(0.55f, 0.6f);

    companion object {
        private val matchers: List<Pair<MoodCategoryKind, List<String>>> = listOf(
            WORKOUT to listOf("workout", "gym", "fitness", "training"),
            ENERGIZE to listOf("energ", "boost", "pump"),
            PARTY to listOf("party", "dance", "club"),
            FEEL_GOOD to listOf("feel good", "happy", "good vibes", "summer"),
            ROMANCE to listOf("romance", "romantic", "love"),
            SLEEP to listOf("sleep", "bedtime"),
            CHILL to listOf("chill", "relax", "calm"),
            FOCUS to listOf("focus", "study", "concentrat"),
            SAD to listOf("sad", "heartbreak", "melanchol"),
            COMMUTE to listOf("commute", "drive", "driving", "travel")
        )

        fun fromTitle(title: String): MoodCategoryKind? {
            val text = title.lowercase(Locale.ROOT)
            return matchers.firstOrNull { (_, needles) -> needles.any { text.contains(it) } }?.first
        }
    }
}

data class MoodEstimate(val energy: Float, val valence: Float, val confidence: Float) {
    companion object {
        val Unknown = MoodEstimate(0.5f, 0.5f, 0f)
    }
}

object MoodEvidence {
    const val EDITORIAL_WEIGHT = 1.2f
    const val TAG_WEIGHT = 0.35f
    const val TITLE_WEIGHT = 0.25f
    const val MIN_CONFIDENCE = 0.2f

    private class Cue(val energy: Float?, val valence: Float?)

    private val tagCues: Map<String, Cue> = mapOf(
        "gym" to Cue(0.9f, null), "workout" to Cue(0.92f, null), "energy" to Cue(0.85f, null), "bass" to Cue(0.78f, null),
        "party" to Cue(0.88f, 0.8f), "dance" to Cue(0.82f, 0.75f), "trap" to Cue(0.7f, 0.42f), "rap" to Cue(0.68f, null),
        "night" to Cue(0.4f, null), "chill" to Cue(0.3f, 0.6f), "ambient" to Cue(0.15f, 0.5f), "focus" to Cue(0.35f, 0.5f),
        "deep" to Cue(0.4f, null), "sad" to Cue(0.25f, 0.15f), "rnb" to Cue(0.45f, 0.6f), "acoustic" to Cue(0.25f, null),
        "sleep" to Cue(0.08f, 0.45f), "rock" to Cue(0.75f, null), "metal" to Cue(0.92f, 0.3f), "classical" to Cue(0.25f, 0.55f),
        "jazz" to Cue(0.35f, 0.62f), "lofi" to Cue(0.22f, 0.55f), "lo-fi" to Cue(0.22f, 0.55f), "romance" to Cue(0.35f, 0.72f)
    )

    private val titleCues: Map<String, Cue> = mapOf(
        "acoustic" to Cue(0.22f, null), "unplugged" to Cue(0.25f, null), "piano" to Cue(0.2f, null), "lullaby" to Cue(0.06f, 0.55f),
        "ballad" to Cue(0.25f, null), "ballata" to Cue(0.25f, null), "balada" to Cue(0.25f, null), "interlude" to Cue(0.3f, null),
        "club" to Cue(0.88f, 0.7f), "rave" to Cue(0.92f, 0.65f), "workout" to Cue(0.92f, null), "party" to Cue(0.86f, 0.8f),
        "festa" to Cue(0.8f, 0.82f), "fiesta" to Cue(0.82f, 0.82f), "dance" to Cue(0.8f, 0.75f),
        "sad" to Cue(null, 0.15f), "triste" to Cue(null, 0.15f), "traurig" to Cue(null, 0.15f), "tears" to Cue(null, 0.15f),
        "lacrime" to Cue(null, 0.15f), "lágrimas" to Cue(null, 0.15f), "alone" to Cue(null, 0.22f), "lonely" to Cue(null, 0.2f),
        "solo" to Cue(null, 0.35f), "heartbreak" to Cue(null, 0.15f), "broken" to Cue(null, 0.22f), "goodbye" to Cue(null, 0.25f),
        "addio" to Cue(null, 0.25f), "adiós" to Cue(null, 0.25f), "happy" to Cue(0.62f, 0.88f), "felice" to Cue(0.6f, 0.86f),
        "feliz" to Cue(0.6f, 0.86f), "sunshine" to Cue(0.62f, 0.86f), "summer" to Cue(0.66f, 0.8f), "estate" to Cue(0.66f, 0.8f),
        "verano" to Cue(0.66f, 0.8f), "love" to Cue(null, 0.72f), "amore" to Cue(null, 0.72f), "amor" to Cue(null, 0.72f),
        "amour" to Cue(null, 0.72f), "liebe" to Cue(null, 0.72f), "sleep" to Cue(0.08f, 0.45f), "dream" to Cue(0.3f, 0.6f),
        "night" to Cue(0.42f, null), "notte" to Cue(0.42f, null), "noche" to Cue(0.42f, null), "fire" to Cue(0.78f, null),
        "fuego" to Cue(0.8f, null), "fuoco" to Cue(0.8f, null), "run" to Cue(0.75f, null), "rain" to Cue(0.32f, 0.38f),
        "pioggia" to Cue(0.32f, 0.38f), "lluvia" to Cue(0.32f, 0.38f)
    )

    private val wordSplit = Regex("""[^\p{L}\p{N}'-]+""")

    fun estimate(track: Track, editorial: Collection<MoodCategoryKind>): MoodEstimate {
        var energySum = 0f
        var energyWeight = 0f
        var valenceSum = 0f
        var valenceWeight = 0f
        fun add(cue: Cue, weight: Float) {
            cue.energy?.let { energySum += it * weight; energyWeight += weight }
            cue.valence?.let { valenceSum += it * weight; valenceWeight += weight }
        }
        editorial.forEach { kind -> add(Cue(kind.energy, kind.valence), EDITORIAL_WEIGHT) }
        track.moodTags.forEach { raw -> tagCues[raw.trim().lowercase(Locale.ROOT)]?.let { add(it, TAG_WEIGHT) } }
        val words = "${track.title} ${track.album}".lowercase(Locale.ROOT).split(wordSplit)
        val seen = HashSet<String>()
        for (word in words) {
            if (word.length < 3 || !seen.add(word)) continue
            titleCues[word]?.let { add(it, TITLE_WEIGHT) }
        }
        if (energyWeight == 0f && valenceWeight == 0f) return MoodEstimate.Unknown
        val energy = if (energyWeight > 0f) energySum / energyWeight else 0.5f
        val valence = if (valenceWeight > 0f) valenceSum / valenceWeight else 0.5f
        val evidence = energyWeight + 0.5f * valenceWeight
        val titleOnly = editorial.isEmpty() && track.moodTags.none { tagCues.containsKey(it.trim().lowercase(Locale.ROOT)) }
        val confidence = (1f - exp(-evidence)).coerceIn(0f, 1f) * (if (energyWeight > 0f) 1f else 0.45f) * if (titleOnly) 0.5f else 1f
        return MoodEstimate(energy.coerceIn(0f, 1f), valence.coerceIn(0f, 1f), confidence)
    }
}

data class MoodJourneyCandidate(
    val track: Track,
    val mood: MoodEstimate,
    val affinity: Float = 0f
)

data class MoodJourneyRequest(
    val curve: MoodCurve,
    val trackCount: Int = DEFAULT_TRACKS,
    val pinnedKeys: List<String> = emptyList(),
    val excludedKeys: Set<String> = emptySet(),
    val exclusions: ArtistExclusions = ArtistExclusions.Empty,
    val avoidKeys: Set<String> = emptySet(),
    val seed: Long = 0L
) {
    val boundedTrackCount: Int get() = trackCount.coerceIn(MIN_TRACKS, MAX_TRACKS)

    companion object {
        const val MIN_TRACKS = 6
        const val MAX_TRACKS = 60
        const val DEFAULT_TRACKS = 18
    }
}

data class MoodJourneyStep(
    val track: Track,
    val key: String,
    val mood: MoodEstimate,
    val target: MoodPoint,
    val position: Float,
    val pinned: Boolean
)

data class MoodJourneyMetrics(
    val meanEnergyError: Float,
    val meanValenceError: Float,
    val maxEnergyJump: Float,
    val roughness: Float,
    val repeatedArtistsAdjacent: Int,
    val duplicateRecordings: Int,
    val lowConfidenceShare: Float,
    val candidateCount: Int,
    val elapsedMs: Long
) {
    val coherence: Float get() = (1f - (meanEnergyError * 0.7f + meanValenceError * 0.3f) * 2f).coerceIn(0f, 1f)
}

data class MoodJourneyResult(
    val steps: List<MoodJourneyStep>,
    val metrics: MoodJourneyMetrics,
    val shortfall: Int
) {
    val tracks: List<Track> get() = steps.map { it.track }
}

object MoodJourneyEngine {
    private const val BEAM_WIDTH = 40
    private const val CANDIDATES_PER_SLOT = 48
    private const val ENERGY_WEIGHT = 1f
    private const val VALENCE_WEIGHT = 0.45f
    private const val UNCERTAINTY_WEIGHT = 0.004f
    private const val JUMP_TOLERANCE = 0.08f
    private const val ENERGY_JUMP_WEIGHT = 2.2f
    private const val VALENCE_JUMP_WEIGHT = 0.9f
    private const val ADJACENT_ALBUM_PENALTY = 0.08f
    private const val RELAXED_ARTIST_PENALTY = 0.05f
    private const val AVOID_PENALTY = 0.01f
    private const val AFFINITY_BONUS = 0.002f
    private const val ARTIST_SPACING = 2
    private const val MAX_POOL = 900

    fun keyOf(track: Track): String = LevyraPersonalOrbit.identityKey(track)

    fun build(
        pool: List<MoodJourneyCandidate>,
        request: MoodJourneyRequest,
        clock: () -> Long = System::nanoTime
    ): MoodJourneyResult {
        val started = clock()
        val pinnedSet = request.pinnedKeys.toSet()
        val eligible = prepare(pool, request, pinnedSet)
        val count = min(request.boundedTrackCount, eligible.size)
        if (count == 0) {
            return MoodJourneyResult(emptyList(), emptyMetrics(eligible.size, (clock() - started) / 1_000_000L), request.boundedTrackCount)
        }
        val artistCap = max(2, (count + 4) / 5)
        var positions = FloatArray(count) { (it + 0.5f) / count }
        var order = search(eligible, request, pinnedSet, positions, artistCap)
        val durations = order.map { eligible[it].track.durationMs }
        if (durations.isNotEmpty() && durations.all { it > 0L }) {
            val total = durations.sum().toFloat()
            var elapsed = 0f
            positions = FloatArray(durations.size) { index ->
                val center = (elapsed + durations[index] / 2f) / total
                elapsed += durations[index]
                center
            }
            order = search(eligible, request, pinnedSet, positions, artistCap)
        }
        val steps = order.mapIndexed { index, candidateIndex ->
            val candidate = eligible[candidateIndex]
            MoodJourneyStep(
                track = candidate.track,
                key = keyOf(candidate.track),
                mood = candidate.mood,
                target = request.curve.sample(positions[index]),
                position = positions[index],
                pinned = keyOf(candidate.track) in pinnedSet
            )
        }
        val elapsedMs = (clock() - started) / 1_000_000L
        return MoodJourneyResult(steps, measure(steps, eligible.size, elapsedMs), request.boundedTrackCount - steps.size)
    }

    fun buildGreedyBaseline(pool: List<MoodJourneyCandidate>, request: MoodJourneyRequest): MoodJourneyResult {
        val eligible = prepare(pool, request, request.pinnedKeys.toSet())
        val count = min(request.boundedTrackCount, eligible.size)
        val used = BooleanArray(eligible.size)
        val steps = ArrayList<MoodJourneyStep>(count)
        for (index in 0 until count) {
            val position = (index + 0.5f) / count
            val target = request.curve.sample(position)
            var best = -1
            var bestCost = Float.MAX_VALUE
            eligible.forEachIndexed { candidateIndex, candidate ->
                if (used[candidateIndex]) return@forEachIndexed
                val cost = fitCost(candidate.mood, target)
                if (cost < bestCost) {
                    bestCost = cost
                    best = candidateIndex
                }
            }
            if (best < 0) break
            used[best] = true
            val candidate = eligible[best]
            steps += MoodJourneyStep(candidate.track, keyOf(candidate.track), candidate.mood, target, position, false)
        }
        return MoodJourneyResult(steps, measure(steps, eligible.size, 0L), request.boundedTrackCount - steps.size)
    }

    private fun prepare(pool: List<MoodJourneyCandidate>, request: MoodJourneyRequest, pinned: Set<String>): List<MoodJourneyCandidate> {
        val byKey = LinkedHashMap<String, MoodJourneyCandidate>()
        val accepted = ArrayList<MoodJourneyCandidate>()
        val byTitle = HashMap<String, MutableList<Track>>()
        for (candidate in pool) {
            if (accepted.size >= MAX_POOL) break
            val track = candidate.track
            val key = keyOf(track)
            if (key.isEmpty() || key in request.excludedKeys) continue
            val isPinned = key in pinned
            if (!isPinned) {
                if (!LevyraPersonalOrbit.isReliableMusicCandidate(track)) continue
                if (request.exclusions.excludesTrack(track)) continue
                if (candidate.mood.confidence < MoodEvidence.MIN_CONFIDENCE) continue
            }
            val existing = byKey[key]
            if (existing != null) {
                if (candidate.mood.confidence > existing.mood.confidence) {
                    byKey[key] = candidate
                    accepted[accepted.indexOf(existing)] = candidate
                }
                continue
            }
            val titleBucket = byTitle.getOrPut(LevyraPersonalOrbit.musicTitleKey(track)) { ArrayList(2) }
            if (titleBucket.any { LevyraPersonalOrbit.sameRecording(it, track) }) continue
            titleBucket += track
            byKey[key] = candidate
            accepted += candidate
        }
        return accepted
    }

    private class Expansion(val parent: Int, val candidate: Int, val cost: Float)

    private class BeamState(
        val sequence: IntArray,
        val length: Int,
        val used: LongArray,
        val cost: Float
    )

    private fun search(
        eligible: List<MoodJourneyCandidate>,
        request: MoodJourneyRequest,
        pinnedSet: Set<String>,
        positions: FloatArray,
        artistCap: Int
    ): List<Int> {
        val count = positions.size
        val targets = Array(count) { request.curve.sample(positions[it]) }
        val keys = eligible.map { keyOf(it.track) }
        val artists = eligible.map { primaryArtist(it.track) }
        val albums = eligible.map { it.track.album.trim().lowercase(Locale.ROOT) }
        val pinnedSlots = assignPinned(eligible, keys, pinnedSet, targets)
        val pinnedCandidates = pinnedSlots.values.toSet()
        val slotCandidates = Array(count) { slot ->
            pinnedSlots[slot]?.let { intArrayOf(it) } ?: nearest(eligible, targets[slot], pinnedCandidates, max(CANDIDATES_PER_SLOT, count * 3))
        }
        val context = SearchContext(eligible, request, targets, keys, artists, albums, pinnedSlots, artistCap)
        val words = (eligible.size + 63) / 64
        var beam = listOf(BeamState(IntArray(count), 0, LongArray(words), 0f))
        for (slot in 0 until count) {
            val expansions = ArrayList<Expansion>(beam.size * slotCandidates[slot].size)
            expandSlot(context, beam, slot, slotCandidates[slot], relaxed = false, out = expansions)
            if (expansions.isEmpty()) expandSlot(context, beam, slot, relaxedCandidates(eligible.size, pinnedCandidates), relaxed = true, out = expansions)
            if (expansions.isEmpty()) break
            expansions.sortBy { it.cost }
            beam = expansions.take(BEAM_WIDTH).map { advance(beam[it.parent], it) }
        }
        val best = beam.minByOrNull { it.cost } ?: return emptyList()
        return best.sequence.copyOf(best.length).toList()
    }

    private class SearchContext(
        val eligible: List<MoodJourneyCandidate>,
        val request: MoodJourneyRequest,
        val targets: Array<MoodPoint>,
        val keys: List<String>,
        val artists: List<String>,
        val albums: List<String>,
        val pinnedSlots: Map<Int, Int>,
        val artistCap: Int
    )

    private fun expandSlot(
        context: SearchContext,
        beam: List<BeamState>,
        slot: Int,
        pool: IntArray,
        relaxed: Boolean,
        out: MutableList<Expansion>
    ) {
        beam.forEachIndexed { parentIndex, state ->
            for (candidateIndex in pool) {
                if (isUsed(state, candidateIndex)) continue
                val pinned = context.pinnedSlots[slot] == candidateIndex
                val violates = !pinned && violatesArtistRules(state, candidateIndex, context.artists, context.artistCap)
                if (violates && !relaxed) continue
                val penalty = if (violates) RELAXED_ARTIST_PENALTY else 0f
                out += Expansion(parentIndex, candidateIndex, state.cost + stepCost(context, state, slot, candidateIndex) + penalty)
            }
        }
    }

    private fun stepCost(context: SearchContext, state: BeamState, slot: Int, candidateIndex: Int): Float {
        val candidate = context.eligible[candidateIndex]
        val key = context.keys[candidateIndex]
        var step = fitCost(candidate.mood, context.targets[slot]) + (1f - candidate.mood.confidence) * UNCERTAINTY_WEIGHT
        step -= candidate.affinity.coerceIn(0f, 1f) * AFFINITY_BONUS
        if (key in context.request.avoidKeys) step += AVOID_PENALTY
        step += jitter(key, context.request.seed)
        if (state.length == 0) return step
        val previous = state.sequence[state.length - 1]
        step += transitionCost(context.eligible[previous].mood, candidate.mood, context.targets[slot - 1], context.targets[slot])
        val album = context.albums[previous]
        if (album.isNotEmpty() && album == context.albums[candidateIndex]) step += ADJACENT_ALBUM_PENALTY
        return step
    }

    private fun isUsed(state: BeamState, index: Int): Boolean = state.used[index ushr 6] and (1L shl (index and 63)) != 0L

    private fun advance(parent: BeamState, expansion: Expansion): BeamState {
        val sequence = parent.sequence.copyOf()
        sequence[parent.length] = expansion.candidate
        val used = parent.used.copyOf()
        used[expansion.candidate ushr 6] = used[expansion.candidate ushr 6] or (1L shl (expansion.candidate and 63))
        return BeamState(sequence, parent.length + 1, used, expansion.cost)
    }

    private fun assignPinned(
        eligible: List<MoodJourneyCandidate>,
        keys: List<String>,
        pinnedSet: Set<String>,
        targets: Array<MoodPoint>
    ): Map<Int, Int> {
        if (pinnedSet.isEmpty()) return emptyMap()
        val pinnedIndices = keys.indices.filter { keys[it] in pinnedSet }.take(targets.size)
        val free = targets.indices.toMutableSet()
        val assignment = HashMap<Int, Int>()
        val pairs = pinnedIndices.flatMap { candidate -> targets.indices.map { slot -> Triple(candidate, slot, fitCost(eligible[candidate].mood, targets[slot])) } }
            .sortedBy { it.third }
        val placed = HashSet<Int>()
        for ((candidate, slot, _) in pairs) {
            if (candidate in placed || slot !in free) continue
            assignment[slot] = candidate
            placed += candidate
            free -= slot
        }
        return assignment
    }

    private fun relaxedCandidates(size: Int, pinned: Set<Int>): IntArray =
        (0 until size).filterNot { it in pinned }.toIntArray()

    private fun nearest(eligible: List<MoodJourneyCandidate>, target: MoodPoint, excluded: Set<Int>, limit: Int): IntArray {
        val scored = ArrayList<Pair<Int, Float>>(eligible.size)
        eligible.forEachIndexed { index, candidate ->
            if (index !in excluded) scored += index to fitCost(candidate.mood, target)
        }
        scored.sortBy { it.second }
        return IntArray(min(limit, scored.size)) { scored[it].first }
    }

    private fun violatesArtistRules(state: BeamState, candidate: Int, artists: List<String>, cap: Int): Boolean {
        val artist = artists[candidate]
        if (artist.isEmpty()) return false
        var occurrences = 0
        for (index in 0 until state.length) {
            if (artists[state.sequence[index]] != artist) continue
            if (state.length - index <= ARTIST_SPACING) return true
            occurrences++
            if (occurrences >= cap) return true
        }
        return false
    }

    internal fun fitCost(mood: MoodEstimate, target: MoodPoint): Float {
        val energy = mood.energy - target.energy
        val valence = mood.valence - target.valence
        return ENERGY_WEIGHT * energy * energy + VALENCE_WEIGHT * valence * valence
    }

    internal fun transitionCost(previous: MoodEstimate, next: MoodEstimate, previousTarget: MoodPoint, nextTarget: MoodPoint): Float {
        val energyExcess = max(0f, abs(next.energy - previous.energy) - abs(nextTarget.energy - previousTarget.energy) - JUMP_TOLERANCE)
        val valenceExcess = max(0f, abs(next.valence - previous.valence) - abs(nextTarget.valence - previousTarget.valence) - JUMP_TOLERANCE)
        return ENERGY_JUMP_WEIGHT * energyExcess * energyExcess + VALENCE_JUMP_WEIGHT * valenceExcess * valenceExcess
    }

    private fun primaryArtist(track: Track): String =
        LevyraPersonalOrbit.artistKeys(track).minOrNull() ?: track.artist.trim().lowercase(Locale.ROOT)

    private fun jitter(key: String, seed: Long): Float {
        if (seed == 0L) return 0f
        var hash = seed * 1_000_003L
        for (char in key) hash = hash * 31L + char.code
        val mixed = hash xor (hash ushr 29)
        return ((mixed and 0xFFFF).toFloat() / 0xFFFF - 0.5f) * 0.004f
    }

    fun measure(steps: List<MoodJourneyStep>, candidateCount: Int, elapsedMs: Long): MoodJourneyMetrics {
        if (steps.isEmpty()) return emptyMetrics(candidateCount, elapsedMs)
        var energyError = 0f
        var valenceError = 0f
        var maxJump = 0f
        var roughness = 0f
        var adjacentArtists = 0
        steps.forEachIndexed { index, step ->
            energyError += abs(step.mood.energy - step.target.energy)
            valenceError += abs(step.mood.valence - step.target.valence)
            if (index > 0) {
                val previous = steps[index - 1]
                val jump = abs(step.mood.energy - previous.mood.energy)
                maxJump = max(maxJump, jump)
                roughness += max(0f, jump - abs(step.target.energy - previous.target.energy))
                if (primaryArtist(step.track) == primaryArtist(previous.track)) adjacentArtists++
            }
        }
        var duplicates = 0
        for (i in steps.indices) for (j in i + 1 until steps.size) {
            if (steps[i].key == steps[j].key || LevyraPersonalOrbit.sameRecording(steps[i].track, steps[j].track)) duplicates++
        }
        return MoodJourneyMetrics(
            meanEnergyError = energyError / steps.size,
            meanValenceError = valenceError / steps.size,
            maxEnergyJump = maxJump,
            roughness = if (steps.size > 1) roughness / (steps.size - 1) else 0f,
            repeatedArtistsAdjacent = adjacentArtists,
            duplicateRecordings = duplicates,
            lowConfidenceShare = steps.count { it.mood.confidence < 0.5f }.toFloat() / steps.size,
            candidateCount = candidateCount,
            elapsedMs = elapsedMs
        )
    }

    private fun emptyMetrics(candidates: Int, elapsed: Long) = MoodJourneyMetrics(0f, 0f, 0f, 0f, 0, 0, 0f, candidates, elapsed)
}
