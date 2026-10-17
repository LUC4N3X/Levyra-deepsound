package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioMixerTest {

    @Test
    fun manySameArtistCandidatesAreSpacedAndCapped() {
        val seed = track("seed", "Dynamite", "BTS")
        val candidates = listOf(
            track("b1", "Butter", "BTS"),
            track("b2", "NORMAL", "BTS"),
            track("b3", "Hooligan", "BTS"),
            track("b4", "Boy With Luv (feat. Halsey)", "BTS"),
            track("b5", "Permission to Dance", "BTS"),
            track("s1", "Chk Chk Boom", "Stray Kids"),
            track("l1", "Made My Night", "LE SSERAFIM"),
            track("e1", "Bite Me", "ENHYPEN"),
            track("k1", "CHOOM", "BABYMONSTER"),
            track("p1", "How You Like That", "BLACKPINK"),
            track("v1", "FRI(END)S", "V")
        )

        val mixed = RadioMixer.mix(candidates, seed, listOf(seed), listOf(seed), limit = 8)

        assertEquals(8, mixed.size)
        assertTrue(mixed.first().artist != "BTS")
        assertTrue(mixed.count { it.artist == "BTS" } <= RadioMixer.ARTIST_WINDOW_CAP)
        mixed.zipWithNext().forEach { (first, second) ->
            assertFalse("${first.title} then ${second.title}", first.artist == "BTS" && second.artist == "BTS")
        }
    }

    @Test
    fun fillPhaseSpreadsTheRemainingSameArtistTracks() {
        val seed = track("seed", "Dynamite", "BTS")
        val candidates = listOf(
            track("o1", "Chk Chk Boom", "Stray Kids"),
            track("b1", "Butter", "BTS"),
            track("o2", "Bite Me", "ENHYPEN"),
            track("o3", "CHOOM", "BABYMONSTER"),
            track("b2", "NORMAL", "BTS"),
            track("b3", "Hooligan", "BTS")
        )

        val mixed = RadioMixer.mix(candidates, seed, listOf(seed), listOf(seed), limit = 6)

        assertEquals(6, mixed.size)
        assertTrue(mixed.first().artist != "BTS")
        mixed.zipWithNext().forEach { (first, second) ->
            assertFalse("${first.id} then ${second.id}", first.artist == "BTS" && second.artist == "BTS")
        }
    }

    @Test
    fun differentRelatedArtistsKeepYoutubeOrder() {
        val seed = track("seed", "Levitating", "Dua Lipa")
        val candidates = listOf(
            track("c1", "My Oh My", "Camila Cabello"),
            track("c2", "Espresso", "Sabrina Carpenter"),
            track("c3", "Shape of You", "Ed Sheeran"),
            track("c4", "Save Your Tears", "The Weeknd")
        )

        val mixed = RadioMixer.mix(candidates, seed, listOf(seed), listOf(seed), limit = 4)

        assertEquals(listOf("c1", "c2", "c3", "c4"), mixed.map(Track::id))
    }

    @Test
    fun sameRecordingUploadedWithDifferentIdsIsKeptOnce() {
        val seed = track("seed", "Interstellar Main Theme - Extra Extended - Soundtrack by Hans Zimmer", "Cinémavore")
        val candidates = listOf(
            track("u1", "Interstellar Main Theme - Extra Extended - Soundtrack by Hans Zimmer", "Billion Dynasty"),
            track("u2", "Interstellar Theme | EPIC Version", "EpicTrailerMusicUK"),
            track("h1", "First Step", "Hans Zimmer"),
            track("r1", "The White Book", "Ramin Djawadi"),
            track("a1", "Atiye Dizi Müzikleri - Interstellar Main Theme (Extra Extended)", "FILMFOLIO")
        )

        val mixed = RadioMixer.mix(candidates, seed, listOf(seed), listOf(seed), limit = 2)

        assertEquals(listOf("h1", "r1"), mixed.map(Track::id))
    }

    @Test
    fun soundtrackTitleCarryingTheComposerMatchesTheOfficialRecording() {
        val upload = track("u1", "Hans Zimmer - Time (Inception)", "Crisyta")
        val official = track("o1", "Time", "Hans Zimmer")
        val unrelated = track("p1", "Time", "Pink Floyd")

        assertTrue(RadioMixer.sameRecording(upload, official))
        assertFalse(RadioMixer.sameRecording(official, unrelated))
    }

    @Test
    fun soundtrackRadioDoesNotStackVersionsOfTheSameTheme() {
        val seed = track("seed", "Time", "Hans Zimmer")
        val candidates = listOf(
            track("v1", "Time (Piano Version)", "Hans Zimmer"),
            track("v2", "Hans Zimmer - Time (Inception)", "Crisyta"),
            track("c1", "Cornfield Chase (From Interstellar)", "Hans Zimmer"),
            track("o1", "Light of the Seven", "Ramin Djawadi"),
            track("o2", "Arrival of the Birds", "The Cinematic Orchestra")
        )

        val mixed = RadioMixer.mix(candidates, seed, listOf(seed), listOf(seed), limit = 3)

        assertEquals(setOf("c1", "o1", "o2"), mixed.map(Track::id).toSet())
        assertTrue(mixed.first().id != "c1")
    }

    @Test
    fun remixOfANewSongIsValidButRemixOfAQueuedSongWaitsForFill() {
        val seed = track("seed", "One More Time", "Daft Punk")
        val queued = listOf(seed, track("q1", "Blinding Lights", "The Weeknd"))
        val candidates = listOf(
            track("r1", "Blinding Lights (Chromatics Remix)", "The Weeknd"),
            track("r2", "Midnight City (Eric Prydz Remix)", "M83"),
            track("l1", "One More Time (Live)", "Daft Punk"),
            track("n1", "Lady (Hear Me Tonight)", "Modjo")
        )

        val balanced = RadioMixer.mix(candidates, seed, queued, queued, limit = 2)
        val filled = RadioMixer.mix(candidates, seed, queued, queued, limit = 4)

        assertEquals(listOf("r2", "n1"), balanced.map(Track::id))
        assertEquals(4, filled.size)
        assertTrue(RadioMixer.isVersionOf(candidates[0], queued[1]))
        assertFalse(RadioMixer.sameRecording(candidates[0], queued[1]))
    }

    @Test
    fun featuredArtistsCountTowardsSpacing() {
        val seed = track("seed", "Shape of You", "Ed Sheeran")
        val candidates = listOf(
            track("f1", "I Don't Care", "Ed Sheeran & Justin Bieber"),
            track("f2", "Peaches (feat. Daniel Caesar)", "Justin Bieber, Daniel Caesar"),
            track("o1", "Señorita", "Shawn Mendes, Camila Cabello"),
            track("o2", "Let Me Love You", "DJ Snake")
        )

        val mixed = RadioMixer.mix(candidates, seed, listOf(seed), listOf(seed), limit = 4)

        assertEquals("f2", mixed.first().id)
        mixed.zipWithNext().forEach { (first, second) ->
            assertFalse(setOf(first.id, second.id) == setOf("f1", "f2"))
        }
    }

    @Test
    fun unicodeArtistsAndTitlesAreDistinguishedAndDeduplicated() {
        val seed = track("seed", "봄날", "방탄소년단")
        val candidates = listOf(
            track("k1", "봄날 (Spring Day)", "방탄소년단"),
            track("k2", "피땀눈물", "방탄소년단"),
            track("j1", "夜に駆ける", "YOASOBI"),
            track("j2", "アイドル", "YOASOBI"),
            track("c1", "晴天", "周杰倫"),
            track("r1", "Кукла колдуна", "Король и Шут")
        )

        val mixed = RadioMixer.mix(candidates, seed, listOf(seed), listOf(seed), limit = 4)

        assertFalse(mixed.any { it.id == "k1" })
        assertEquals(listOf("j1", "k2", "j2", "c1"), mixed.map(Track::id))
    }

    @Test
    fun insufficientDiversityStillFillsTheQueue() {
        val seed = track("seed", "Song 0", "Solo Artist")
        val candidates = (1..6).map { track("s$it", "Song number $it", "Solo Artist") }

        val mixed = RadioMixer.mix(candidates, seed, listOf(seed), listOf(seed), limit = 5)

        assertEquals(5, mixed.size)
    }

    @Test
    fun recentlyPlayedTracksAreUsedOnlyWhenNothingElseIsLeft() {
        val seed = track("seed", "Seed Song", "Seed Artist")
        val played = track("old", "Old Favourite", "Other Artist")
        val candidates = listOf(
            track("again", "Old Favourite", "Other Artist"),
            track("fresh", "Fresh Song", "Third Artist")
        )

        val first = RadioMixer.mix(candidates, seed, listOf(seed), listOf(seed), listOf(played), limit = 1)
        val both = RadioMixer.mix(candidates, seed, listOf(seed), listOf(seed), listOf(played), limit = 2)

        assertEquals(listOf("fresh"), first.map(Track::id))
        assertEquals(listOf("fresh", "again"), both.map(Track::id))
    }

    @Test
    fun queuedTracksAndSeedAreNeverRepeated() {
        val seed = track("seed", "Seed Song", "Seed Artist")
        val queued = listOf(seed, track("q1", "Queued Song", "Another"))
        val candidates = listOf(
            track("seed", "Seed Song", "Seed Artist"),
            track("q1", "Queued Song", "Another"),
            track("q1-video", "Queued Song (Official Video)", "Another"),
            track("n1", "New Song", "Newcomer")
        )

        val mixed = RadioMixer.mix(candidates, seed, queued, queued, limit = 5)

        assertEquals(listOf("n1"), mixed.map(Track::id))
    }

    @Test
    fun officialVideoSeedRejectsLiveSessionsReuploadsAndRemasters() {
        val seed = track("seed", "BTS (방탄소년단) 'Dynamite' Official MV", "HYBE LABELS")
        val candidates = listOf(
            track("live", "BTS (방탄소년단) 'Dynamite' @ BBC Radio 1 Live Lounge", "BTS HYBE LABELS"),
            track("clip", "[TinyTAN | CLIP] - Dynamite", "BTS"),
            track("cover", "Dynamite (2023 Remastered)", "Inessa Lee"),
            track("butter", "Butter", "BTS"),
            track("magic", "Magic", "TOMORROW X TOGETHER")
        )

        val mixed = RadioMixer.mix(candidates, seed, listOf(seed), listOf(seed), limit = 2)

        assertEquals(setOf("butter", "magic"), mixed.map(Track::id).toSet())
    }

    @Test
    fun coversNamedLikeTheSeedWaitUntilNothingElseIsLeft() {
        val seed = track("seed", "Levitating", "Dua Lipa")
        val candidates = listOf(
            track("pmj", "Levitating", "Postmodern Jukebox"),
            track("tony", "Levitating", "Tony Production"),
            track("closer", "Closer (feat. Halsey)", "The Chainsmokers"),
            track("hymn", "Hymn For The Weekend", "Coldplay")
        )

        val balanced = RadioMixer.mix(candidates, seed, listOf(seed), listOf(seed), limit = 2)
        val filled = RadioMixer.mix(candidates, seed, listOf(seed), listOf(seed), limit = 4)

        assertEquals(listOf("closer", "hymn"), balanced.map(Track::id))
        assertEquals(3, filled.size)
    }

    @Test
    fun shortCommonTitlesFromDifferentArtistsAreNotMerged() {
        assertFalse(RadioMixer.sameRecording(track("a", "My Love", "Westlife"), track("b", "My Love Is Gone", "Other")))
        assertFalse(RadioMixer.sameRecording(track("a", "Intro", "The xx"), track("b", "Intro", "M83")))
    }

    private fun track(id: String, title: String, artist: String) = Track(
        id = id,
        title = title,
        artist = artist,
        album = "",
        durationMs = 0L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0
    )
}
