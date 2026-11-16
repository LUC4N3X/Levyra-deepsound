package com.luc4n3x.levyra.data.spotify

import com.luc4n3x.levyra.data.apple.AppleCatalogTracks
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogClassicalMatchReplayTest {

    @Test
    fun composerPrefixedYoutubeTitleMatchesTheSameRecording() {
        val catalog = apple("The Seal Lullaby", "Eric Whitacre Singers", 253_000L)
        val candidates = listOf(
            youtube("sealvoces01", "Whitacre: The Seal Lullaby", "VOCES8, Eric Whitacre, Christopher Glynn", 275_000L),
            youtube("sealwhita01", "Whitacre: The Seal Lullaby", "Eric Whitacre", 254_000L),
            youtube("sealarntz01", "Seal's Lullaby", "Holly Arntzen and the Saltwater Singers", 228_000L)
        )

        assertEquals("sealwhita01", SpotifyYouTubeMatcher.findBestMatch(catalog, candidates)?.candidate?.id)
    }

    @Test
    fun catalogNumberSpacingDoesNotHideTheSameWork() {
        val catalog = apple("Berceuse, H. 8", "Louise Williams & David Owen Norris", 156_000L)
        val candidates = listOf(
            youtube("bridgegon01", "Bridge: Gondoliera, H80", "Louise Williams, David Owen Norris", 266_000L),
            youtube("bridgeber01", "Bridge: Berceuse, H8", "Louise Williams, David Owen Norris", 157_000L),
            youtube("hickoxber01", "Berceuse, H. 8", "Richard Hickox, BBC National Orchestra of Wales", 206_000L)
        )

        assertEquals("bridgeber01", SpotifyYouTubeMatcher.findBestMatch(catalog, candidates)?.candidate?.id)
    }

    @Test
    fun cyrillicStorefrontArtistMatchesLatinYoutubeCredit() {
        val catalog = apple("Prélude in C-Sharp Minor, Op. 45", "Алексей Любимов", 338_000L)
        val candidates = listOf(
            youtube("lubimov0001", "Fryderyk Chopin: Prelude in C-Sharp Minor, Op. 45", "Alexei Lubimov", 288_000L),
            youtube("lubimov0002", "Chopin: Prélude in C-Sharp Minor, Op. 45", "Alexei Lubimov, Frédéric Chopin", 339_000L)
        )

        assertEquals("lubimov0002", SpotifyYouTubeMatcher.findBestMatch(catalog, candidates)?.candidate?.id)
    }

    @Test
    fun cyrillicArtistDoesNotMatchAnUnrelatedLatinPerformer() {
        val catalog = apple("Spiegel im Spiegel: 1", "Владимир Спиваков & Sergej Bezrodny", 636_000L)
        val candidate = youtube("spiegelbat1", "Pärt: Spiegel im Spiegel", "Lisa Batiashvili, Hélène Grimaud, Arvo Pärt", 624_000L)

        assertNull(SpotifyYouTubeMatcher.findBestMatch(catalog, listOf(candidate)))
    }

    @Test
    fun samePerformersWithAnotherCatalogLabelPlayInsteadOfFailing() {
        val catalog = apple("Wiegenlied, TrV 59", "James Ehnes & Andrew Armstrong", 262_000L)
        val candidates = listOf(
            youtube("ehnessona01", "Violin Sonata No. 3 in D Minor, Op. 108: II. Adagio", "James Ehnes, Andrew Armstrong, Johannes Brahms", 271_000L),
            youtube("straussw001", "Wiegenlied (Cradle Song), TrV 59", "Richard Strauss", 245_000L),
            youtube("ehneswieg01", "Wiegenlied, AV 41", "James Ehnes, Andrew Armstrong", 263_000L)
        )

        assertEquals("ehneswieg01", SpotifyYouTubeMatcher.findPlayableMatch(catalog, candidates)?.candidate?.id)
    }

    @Test
    fun missingRecordingFallsBackToTheClosestPerformanceOfTheSameMovement() {
        val catalog = apple(
            "Symphony No. 3 in F Major, Op. 90: III. Poco allegretto (Live)",
            "Philharmonisches Staatsorchester Hamburg & Kent Nagano",
            388_000L
        )
        val candidates = listOf(
            youtube("brahmsvpo01", "Symphony No. 3 in F Major, Op. 90: III. Poco allegretto (Live)", "Wiener Philharmoniker, Johannes Brahms", 418_000L),
            youtube("brahmsbpo01", "Symphony No. 3 in F Major, Op. 90: III. Poco allegretto (Live)", "Berlin Philharmonic, Wilhelm Furtwängler, Johannes Brahms", 382_000L),
            youtube("brahmscgo01", "Symphony No. 3 in F Major, Op. 90: III. Poco allegretto (Live)", "Concertgebouworkest, Dimitri Mitropoulos", 374_000L),
            youtube("naganosym02", "Symphony No. 2 in D Major: I. Allegro non troppo (Live)", "Philharmonisches Staatsorchester Hamburg, Kent Nagano, Johannes Brahms", 1_240_000L)
        )

        assertNull(SpotifyYouTubeMatcher.findBestMatch(catalog, candidates))
        assertEquals("brahmsbpo01", SpotifyYouTubeMatcher.findPlayableMatch(catalog, candidates)?.candidate?.id)
    }

    @Test
    fun descriptiveSubtitleStillFindsTheSamePerformers() {
        val catalog = apple("Berceuse romantique (Caprice)", "Joshua Bell & Paul Coker", 270_000L)
        val candidates = listOf(
            youtube("kreislerca1", "Kreisler: Caprice viennois, Op. 2", "Joshua Bell, Paul Coker", 280_000L),
            youtube("kreislerbe1", "Kreisler: Berceuse romantique, Op. 9", "Joshua Bell, Paul Coker, Fritz Kreisler", 271_000L),
            youtube("kreislerbe2", "Kreisler: Berceuse romantique, Op. 9", "Joshua Bell, Paul Coker", 274_000L)
        )

        assertEquals("kreislerbe1", SpotifyYouTubeMatcher.findPlayableMatch(catalog, candidates)?.candidate?.id)
    }

    @Test
    fun otherWorkByTheSamePerformersIsNeverAFallback() {
        val catalog = apple("Wiegenlied, TrV 59", "James Ehnes & Andrew Armstrong", 262_000L)
        val candidates = listOf(
            youtube("ehnessona01", "Violin Sonata No. 3 in D Minor, Op. 108: II. Adagio", "James Ehnes, Andrew Armstrong, Johannes Brahms", 263_000L),
            youtube("ehnesrhap01", "Violin Rhapsody No. 1, BB 94a, Sz. 87: I. Lassu. Moderato", "James Ehnes, Andrew Armstrong", 262_000L)
        )

        assertNull(SpotifyYouTubeMatcher.findPlayableMatch(catalog, candidates))
    }

    @Test
    fun sameWorkFallbackRejectsAPerformanceOfVeryDifferentLength() {
        val catalog = apple("Spiegel im Spiegel: 1", "Владимир Спиваков & Sergej Bezrodny", 636_000L)
        val candidates = listOf(
            youtube("spiegelvio1", "Spiegel im Spiegel, for Viola & Piano", "Benjamin Hudson, Jürgen Kruse, Arvo Pärt", 420_000L)
        )

        assertNull(SpotifyYouTubeMatcher.findPlayableMatch(catalog, candidates))
    }

    @Test
    fun popCoverWithAnotherTitleIsNotAcceptedAsTheSameWork() {
        val catalog = apple("Blinding Lights", "The Weeknd", 200_000L)
        val candidates = listOf(
            youtube("weekndsave1", "Save Your Tears", "The Weeknd", 201_000L),
            youtube("karaoke0001", "Blinding Lights (Karaoke Version)", "Sing King", 200_000L)
        )

        assertNull(SpotifyYouTubeMatcher.findPlayableMatch(catalog, candidates))
    }

    @Test
    fun sameTitleByAnotherArtistIsNotASameWorkFallbackForSongs() {
        val catalog = apple("Home", "ABBA", 180_000L)
        val candidate = youtube("queenhome01", "Home", "Queen", 180_000L)

        assertNull(SpotifyYouTubeMatcher.findPlayableMatch(catalog, listOf(candidate)))
    }

    @Test
    fun distinctWorkPrefixesAreNotStrippedIntoTheSameTitle() {
        val catalog = apple("Carmen: Prelude", "London Symphony Orchestra", 140_000L)
        val candidate = youtube("traviatapr1", "La Traviata: Prelude", "London Symphony Orchestra", 140_000L)

        assertNull(SpotifyYouTubeMatcher.findBestMatch(catalog, listOf(candidate)))
    }

    @Test
    fun anotherNumberOfTheSameOpusIsNeverAccepted() {
        val catalog = apple("Prelude, Op. 28: No. 4", "Martha Argerich", 130_000L)
        val candidates = listOf(
            youtube("argerichp06", "Prelude, Op. 28: No. 6", "Martha Argerich", 131_000L),
            youtube("pollinip006", "Prelude, Op. 28: No. 6", "Maurizio Pollini", 138_000L)
        )

        assertNull(SpotifyYouTubeMatcher.findBestMatch(catalog, candidates))
        assertNull(SpotifyYouTubeMatcher.findPlayableMatch(catalog, candidates))
    }

    @Test
    fun anotherMovementOfTheSameWorkIsNeverAccepted() {
        val catalog = apple("Symphony No. 3 in F Major, Op. 90: III. Poco allegretto", "Kent Nagano", 388_000L)
        val candidate = youtube("brahmsmov02", "Symphony No. 3 in F Major, Op. 90: II. Andante", "Kent Nagano", 389_000L)

        assertNull(SpotifyYouTubeMatcher.findPlayableMatch(catalog, listOf(candidate)))
    }

    @Test
    fun strictMatchStillWinsOverSameWorkFallback() {
        val catalog = apple("Consolations, S. 172: No. 3, Lento placido", "Khatia Buniatishvili", 312_000L)
        val candidates = listOf(
            youtube("lisztother1", "Consolations, S. 172: No. 3, Lento placido", "Daniel Barenboim, Franz Liszt", 311_000L),
            youtube("khatiacons1", "Consolations, S. 172: No. 3, Lento placido", "Khatia Buniatishvili, Franz Liszt", 312_000L)
        )

        val match = SpotifyYouTubeMatcher.findPlayableMatch(catalog, candidates)
        assertNotNull(match)
        assertEquals("khatiacons1", match?.candidate?.id)
    }

    private fun apple(title: String, artist: String, durationMs: Long): Track =
        requireNotNull(AppleCatalogTracks.track(title, artist, durationMs = durationMs))

    private fun youtube(id: String, title: String, artist: String, durationMs: Long): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = "",
        durationMs = durationMs,
        streamUrl = "",
        videoUrl = "https://www.youtube.com/watch?v=$id",
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "YouTube Music",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 70,
        cacheScore = 70,
        accentStart = 0,
        accentEnd = 0,
        videoType = "MUSIC_VIDEO_TYPE_ATV"
    )
}
