package com.luc4n3x.levyra.domain

import com.luc4n3x.levyra.viewmodel.MoodJourneyCurveCodec
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodJourneyTest {

    private fun song(title: String, artist: String, album: String = "$artist album", tags: Set<String> = emptySet(), durationMs: Long = 210_000L) = Track(
        id = "$artist-$title",
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = tags,
        energy = 50,
        vocal = 50,
        replayScore = 50,
        cacheScore = 50,
        accentStart = 0,
        accentEnd = 0
    )

    private fun library(seed: Int, size: Int, artists: Int = size / 3): List<MoodJourneyCandidate> {
        val random = Random(seed)
        return List(size) { index ->
            MoodJourneyCandidate(
                track = song("Track $index", "Artist ${index % artists}", durationMs = 150_000L + random.nextLong(150_000L)),
                mood = MoodEstimate(random.nextFloat(), random.nextFloat(), 0.6f + random.nextFloat() * 0.4f)
            )
        }
    }

    @Test
    fun curveInterpolationNeverOvershootsItsPoints() {
        val curve = MoodCurve(listOf(MoodPoint(0.1f, 0.5f), MoodPoint(0.9f, 0.5f), MoodPoint(0.85f, 0.5f), MoodPoint(0.2f, 0.5f)))
        for (i in 0..300) {
            val value = curve.sample(i / 300f).energy
            assertTrue(value in 0.1f..0.9f)
        }
        assertEquals(0.1f, curve.sample(0f).energy, 0.0001f)
        assertEquals(0.2f, curve.sample(1f).energy, 0.0001f)
        val rising = MoodCurve.preset(MoodCurvePreset.Sunrise)
        for (i in 0 until 100) assertTrue(rising.sample((i + 1) / 100f).energy >= rising.sample(i / 100f).energy - 1e-4f)
    }

    @Test
    fun moodToneCoversTheEnergyValenceGrid() {
        assertEquals(MoodTone.Melancholy, MoodTone.of(0.1f, 0.1f))
        assertEquals(MoodTone.Calm, MoodTone.of(0.2f, 0.5f))
        assertEquals(MoodTone.Tender, MoodTone.of(0.3f, 0.8f))
        assertEquals(MoodTone.Flowing, MoodTone.of(0.5f, 0.5f))
        assertEquals(MoodTone.Tense, MoodTone.of(0.9f, 0.2f))
        assertEquals(MoodTone.Euphoric, MoodTone.of(0.95f, 0.9f))
        assertEquals(MoodTone.Flowing, MoodTone.of(Float.NaN, Float.NaN))
    }

    @Test
    fun peakIndexPointsAtTheMostEnergeticStep() {
        val steps = listOf(0.2f, 0.9f, 0.4f).mapIndexed { index, energy ->
            val track = song("Peak $index", "Artist $index")
            MoodJourneyStep(track, "k$index", MoodEstimate(energy, 0.5f, 1f), MoodPoint(energy, 0.5f), index / 3f, false)
        }
        val metrics = MoodJourneyEngine.measure(steps, 3, 0L)
        assertEquals(1, MoodJourneyResult(steps, metrics, 0).peakIndex)
        assertEquals(-1, MoodJourneyResult(emptyList(), metrics, 0).peakIndex)
    }

    @Test
    fun curveNormalizesInvalidInput() {
        val curve = MoodCurve(listOf(MoodPoint(Float.NaN, 2f)))
        assertEquals(2, curve.normalized.size)
        assertEquals(0.5f, curve.normalized.first().energy)
        assertEquals(1f, curve.normalized.first().valence)
    }

    @Test
    fun evidenceUsesOnlyVerifiableSignals() {
        val unknown = MoodEvidence.estimate(song("Untitled", "Someone"), emptySet())
        assertEquals(0f, unknown.confidence)
        val workout = MoodEvidence.estimate(song("Untitled", "Someone"), setOf(MoodCategoryKind.WORKOUT))
        assertTrue(workout.energy > 0.85f && workout.confidence > 0.6f)
        val sadTitle = MoodEvidence.estimate(song("Tears in the rain", "Someone"), emptySet())
        assertTrue(sadTitle.valence < 0.3f)
        assertTrue(sadTitle.confidence < MoodEvidence.MIN_CONFIDENCE)
        val tagged = MoodEvidence.estimate(song("Untitled", "Someone", tags = setOf("chill")), emptySet())
        assertTrue(tagged.energy < 0.4f && tagged.confidence >= MoodEvidence.MIN_CONFIDENCE)
        assertEquals(MoodCategoryKind.SLEEP, MoodCategoryKind.fromTitle("Sleep"))
        assertEquals(MoodCategoryKind.FEEL_GOOD, MoodCategoryKind.fromTitle("Feel Good"))
    }

    @Test
    fun beamSearchMatchesTheCurveWithSmootherTransitionsThanGreedy() {
        var beamError = 0f
        var greedyError = 0f
        var beamRough = 0f
        var greedyRough = 0f
        var beamAdjacent = 0
        var greedyAdjacent = 0
        repeat(30) { seed ->
            val pool = library(seed, 400)
            val request = MoodJourneyRequest(MoodCurve.preset(MoodCurvePreset.values()[seed % 5]), trackCount = 20)
            val beam = MoodJourneyEngine.build(pool, request).metrics
            val greedy = MoodJourneyEngine.buildGreedyBaseline(pool, request).metrics
            beamError += beam.meanEnergyError
            greedyError += greedy.meanEnergyError
            beamRough += beam.roughness
            greedyRough += greedy.roughness
            beamAdjacent += beam.repeatedArtistsAdjacent
            greedyAdjacent += greedy.repeatedArtistsAdjacent
        }
        println(
            "Mood Journey over 30 libraries: energy error beam=%.4f greedy=%.4f, roughness beam=%.4f greedy=%.4f, adjacent same artist beam=%d greedy=%d"
                .format(beamError / 30, greedyError / 30, beamRough / 30, greedyRough / 30, beamAdjacent, greedyAdjacent)
        )
        assertTrue(beamRough <= greedyRough)
        assertTrue(beamError / 30 <= greedyError / 30 + 0.01f)
        assertEquals(0, beamAdjacent)
    }

    @Test
    fun resultHasNoRepeatsAndSpacesArtists() {
        val pool = library(3, 300, artists = 40) + library(3, 300, artists = 40)
        val result = MoodJourneyEngine.build(pool, MoodJourneyRequest(MoodCurve.preset(MoodCurvePreset.Wave), trackCount = 30))
        assertEquals(30, result.steps.size)
        assertEquals(0, result.metrics.duplicateRecordings)
        val artists = result.steps.map { it.track.artist }
        artists.indices.forEach { index ->
            val window = artists.subList(maxOf(0, index - 2), index)
            assertFalse("artist repeated too soon at $index", artists[index] in window)
        }
        assertTrue(artists.groupingBy { it }.eachCount().values.all { it <= 6 })
    }

    @Test
    fun pinnedSongsAreKeptAndExclusionsRespected() {
        val pool = library(5, 200)
        val calm = MoodJourneyCandidate(song("Night lullaby", "Pinned Artist"), MoodEstimate(0.05f, 0.5f, 1f))
        val banned = pool.first()
        val request = MoodJourneyRequest(
            curve = MoodCurve.preset(MoodCurvePreset.WindDown),
            trackCount = 16,
            pinnedKeys = listOf(MoodJourneyEngine.keyOf(calm.track)),
            excludedKeys = setOf(MoodJourneyEngine.keyOf(banned.track)),
            exclusions = ArtistExclusions(nameKeys = artistIdentityKeys("Artist 1").toSet())
        )
        val result = MoodJourneyEngine.build(pool + calm, request)
        val pinned = result.steps.firstOrNull { it.pinned }
        assertNotNull(pinned)
        assertTrue("calm song placed at ${result.steps.indexOf(pinned)}", result.steps.indexOf(pinned) >= result.steps.size / 2)
        assertTrue(result.steps.none { it.key == MoodJourneyEngine.keyOf(banned.track) })
        assertTrue(result.steps.none { it.track.artist == "Artist 1" })
    }

    @Test
    fun regenerationAvoidsThePreviousSelection() {
        val pool = library(9, 500)
        val first = MoodJourneyEngine.build(pool, MoodJourneyRequest(MoodCurve.preset(MoodCurvePreset.Summit), trackCount = 20))
        val second = MoodJourneyEngine.build(
            pool,
            MoodJourneyRequest(MoodCurve.preset(MoodCurvePreset.Summit), trackCount = 20, avoidKeys = first.steps.map { it.key }.toSet(), seed = 1)
        )
        val overlap = first.steps.map { it.key }.intersect(second.steps.map { it.key }.toSet()).size
        assertTrue("overlap $overlap", overlap <= 6)
        assertTrue(second.metrics.meanEnergyError < 0.1f)
    }

    @Test
    fun flatCurveStillFillsLongJourneys() {
        val result = MoodJourneyEngine.build(library(13, 400), MoodJourneyRequest(MoodCurve.preset(MoodCurvePreset.Steady), trackCount = 60))
        assertEquals(60, result.steps.size)
        assertEquals(0, result.shortfall)
        assertEquals(0, result.metrics.duplicateRecordings)
    }

    @Test
    fun artistDominatedPoolRelaxesSpacingAndKeepsLaterPinnedSongs() {
        val pool = List(80) { MoodJourneyCandidate(song("Song $it", if (it < 75) "Solo Artist" else "Guest $it"), MoodEstimate(0.5f, 0.5f, 1f)) }
        val late = MoodJourneyCandidate(song("Finale", "Closing Guest"), MoodEstimate(0.95f, 0.5f, 1f))
        val result = MoodJourneyEngine.build(
            pool + late,
            MoodJourneyRequest(MoodCurve.preset(MoodCurvePreset.Sunrise), trackCount = 40, pinnedKeys = listOf(MoodJourneyEngine.keyOf(late.track)))
        )
        assertEquals(40, result.steps.size)
        assertEquals(0, result.shortfall)
        assertTrue(result.steps.any { it.pinned })
        assertEquals(0, result.metrics.duplicateRecordings)
    }

    @Test
    fun lowConfidenceTracksAreNotGuessedIntoTheJourney() {
        val pool = List(50) { MoodJourneyCandidate(song("Song $it", "Artist $it"), MoodEstimate(0.5f, 0.5f, 0.05f)) }
        val result = MoodJourneyEngine.build(pool, MoodJourneyRequest(MoodCurve.preset(MoodCurvePreset.Steady)))
        assertTrue(result.steps.isEmpty())
        assertEquals(MoodJourneyRequest.DEFAULT_TRACKS, result.shortfall)
    }

    @Test
    fun largeLibrariesStayFast() {
        val pool = library(11, 2_000, artists = 600)
        MoodJourneyEngine.build(pool, MoodJourneyRequest(MoodCurve.preset(MoodCurvePreset.Wave), trackCount = 60))
        val started = System.nanoTime()
        val result = MoodJourneyEngine.build(pool, MoodJourneyRequest(MoodCurve.preset(MoodCurvePreset.Wave), trackCount = 60))
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        println("Mood Journey 60 songs from ${result.metrics.candidateCount} candidates in $elapsedMs ms")
        assertEquals(60, result.steps.size)
        assertTrue(elapsedMs < 2_000)
    }

    @Test
    fun curveCodecRoundTrips() {
        val curve = MoodCurve.preset(MoodCurvePreset.Summit, 7)
        val decoded = MoodJourneyCurveCodec.decode(MoodJourneyCurveCodec.encode(curve, 24, setOf(MoodStyle.DISCO, MoodStyle.ROCK)))
        assertNotNull(decoded)
        assertEquals(24, decoded!!.trackCount)
        assertEquals(setOf(MoodStyle.ROCK, MoodStyle.DISCO), decoded.styles)
        decoded.curve.points.zip(curve.normalized).forEach { (a, b) -> assertEquals(b.energy, a.energy, 1e-4f) }
        assertEquals(emptySet<MoodStyle>(), MoodJourneyCurveCodec.decode("{\"count\":12,\"points\":[[0.2,0.3],[0.4,0.5],[0.6,0.7]],\"styles\":[\"gone\"]}")!!.styles)
        assertEquals(12, MoodJourneyCurveCodec.decode("{\"count\":12,\"points\":[[0.2,0.3],[0.4,0.5],[0.6,0.7]]}")!!.trackCount)
        assertEquals(null, MoodJourneyCurveCodec.decode("{\"points\":[[0.2,0.3]]}"))
        assertEquals(null, MoodJourneyCurveCodec.decode("not json"))
    }

    @Test
    fun selectedStylesKeepOnlyMatchingSongsExceptPinned() {
        val rock = List(30) { MoodJourneyCandidate(song("Rock $it", "Band $it"), MoodEstimate(it / 30f, 0.5f, 0.8f), styles = setOf(MoodStyle.ROCK)) }
        val disco = List(30) { MoodJourneyCandidate(song("Disco $it", "Diva $it"), MoodEstimate(it / 30f, 0.8f, 0.8f), styles = setOf(MoodStyle.DISCO)) }
        val plain = List(30) { MoodJourneyCandidate(song("Plain $it", "Singer $it"), MoodEstimate(it / 30f, 0.5f, 0.8f)) }
        val pinned = plain.first()
        val request = MoodJourneyRequest(
            MoodCurve.preset(MoodCurvePreset.Sunrise),
            trackCount = 20,
            styles = setOf(MoodStyle.ROCK, MoodStyle.DISCO),
            pinnedKeys = listOf(MoodJourneyEngine.keyOf(pinned.track))
        )
        val result = MoodJourneyEngine.build(rock + disco + plain, request)
        assertEquals(20, result.steps.size)
        val chosen = result.steps.map { it.track.title }
        assertTrue(chosen.all { it.startsWith("Rock") || it.startsWith("Disco") || it == pinned.track.title })
        assertTrue(pinned.track.title in chosen)
        val mixed = MoodJourneyEngine.build(rock + disco + plain, request.copy(styles = emptySet(), pinnedKeys = emptyList()))
        assertEquals(61, result.metrics.candidateCount)
        assertEquals(90, mixed.metrics.candidateCount)
    }

    @Test
    fun styleCueGivesGenreSongsEnoughConfidenceAndBlendsWithMood() {
        val unlabelled = song("Untitled", "Band")
        assertTrue(MoodEvidence.estimate(unlabelled, emptyList()).confidence < MoodEvidence.MIN_CONFIDENCE)
        val rock = MoodEvidence.estimate(unlabelled, emptyList(), listOf(MoodStyle.ROCK))
        assertTrue(rock.confidence >= MoodEvidence.MIN_CONFIDENCE)
        assertEquals(MoodStyle.ROCK.energy, rock.energy, 1e-4f)
        val chillRock = MoodEvidence.estimate(unlabelled, listOf(MoodCategoryKind.CHILL), listOf(MoodStyle.ROCK))
        assertTrue(chillRock.energy < rock.energy && chillRock.energy > MoodCategoryKind.CHILL.energy)
    }

    @Test
    fun playlistTitlesMapToMoodsOnWholeWords() {
        assertEquals(MoodCategoryKind.WORKOUT, MoodCategoryKind.fromPlaylistTitle("Classic Rock Workout"))
        assertEquals(MoodCategoryKind.FEEL_GOOD, MoodCategoryKind.fromPlaylistTitle("Feel-Good Rock Hits"))
        assertEquals(MoodCategoryKind.CHILL, MoodCategoryKind.fromPlaylistTitle("Reggae Relaxation"))
        assertEquals(MoodCategoryKind.SAD, MoodCategoryKind.fromPlaylistTitle("Country Breakup"))
        assertEquals(MoodCategoryKind.ROMANCE, MoodCategoryKind.fromPlaylistTitle("Country Love Songs"))
        assertEquals(MoodCategoryKind.SLEEP, MoodCategoryKind.fromPlaylistTitle("New Age Meditation"))
        assertEquals(null, MoodCategoryKind.fromPlaylistTitle("Brunch Classics"))
        assertEquals(null, MoodCategoryKind.fromPlaylistTitle("'90s Rock"))
    }

    @Test
    fun stylesMatchYouTubeMusicGenreTitlesExactly() {
        assertTrue(MoodStyle.ROCK.matchesCategory("Rock"))
        assertTrue(MoodStyle.COUNTRY.matchesCategory("Country & Americana"))
        assertTrue(MoodStyle.REGGAE.matchesCategory("Reggae & caribbean"))
        assertFalse(MoodStyle.POP.matchesCategory("K-Pop"))
        assertFalse(MoodStyle.POP.matchesCategory("J-Pop"))
        assertTrue(MoodStyle.DISCO.categoryTitles.isEmpty() && MoodStyle.DISCO.searchQueries.isNotEmpty())
        assertTrue(MoodStyle.NEW_AGE.categoryTitles.isEmpty() && MoodStyle.NEW_AGE.searchQueries.isNotEmpty())
        MoodStyle.entries.forEach { assertEquals(it, MoodStyle.from(it.storageId)) }
    }

    @Test
    fun `energy shift moves only the chosen points by one clamped delta`() {
        val curve = MoodCurve(listOf(MoodPoint(0.2f, 0.3f), MoodPoint(0.5f, 0.6f), MoodPoint(0.8f, 0.4f), MoodPoint(0.4f, 0.9f)))

        val raised = curve.withEnergyShift(setOf(0, 1), 0.25f)
        assertEquals(0.45f, raised.points[0].energy, 1e-5f)
        assertEquals(0.75f, raised.points[1].energy, 1e-5f)
        assertEquals(curve.points[2], raised.points[2])
        assertEquals(curve.points[3], raised.points[3])
        assertEquals(0.3f, raised.points[0].valence, 0f)

        val capped = curve.withEnergyShift(setOf(1, 2), 0.5f)
        assertEquals(0.7f, capped.points[1].energy, 1e-5f)
        assertEquals(1f, capped.points[2].energy, 1e-5f)

        val floored = curve.withEnergyShift(setOf(0, 2), -0.9f)
        assertEquals(0f, floored.points[0].energy, 1e-5f)
        assertEquals(0.6f, floored.points[2].energy, 1e-5f)

        assertEquals(curve, curve.withEnergyShift(emptySet(), 0.3f))
        assertEquals(curve, curve.withEnergyShift(setOf(7), 0.3f))
        assertEquals(curve, curve.withEnergyShift(setOf(1), Float.NaN))
    }

    @Test
    fun `six newer styles sit beside the original ones without breaking stored ids`() {
        val legacy = listOf("pop", "rock", "hip_hop", "electronic", "rnb", "jazz", "classical", "reggae", "folk", "country", "disco", "new_age", "latin", "metal", "blues")
        legacy.forEachIndexed { index, id -> assertEquals(MoodStyle.entries[index], MoodStyle.from(id)) }
        val added = listOf(MoodStyle.K_POP, MoodStyle.AFROBEATS, MoodStyle.INDIE, MoodStyle.PUNK, MoodStyle.SOUL_FUNK, MoodStyle.AMBIENT)
        assertEquals(added, MoodStyle.entries.drop(legacy.size))
        assertEquals(MoodStyle.entries.size, MoodStyle.entries.map { it.storageId }.toSet().size)
        assertTrue(MoodStyle.K_POP.matchesCategory("K-Pop"))
        MoodStyle.entries.forEach { style ->
            assertTrue(style.name, style.searchQueries.isNotEmpty() && style.appleGenres.isNotEmpty())
            assertTrue(style.name, style.energy in 0f..1f && style.valence in 0f..1f)
        }
        val decoded = MoodJourneyCurveCodec.decode(MoodJourneyCurveCodec.encode(MoodCurve.preset(MoodCurvePreset.Wave), 20, setOf(MoodStyle.BLUES, MoodStyle.AMBIENT)))
        assertEquals(setOf(MoodStyle.BLUES, MoodStyle.AMBIENT), decoded?.styles)
    }
}
