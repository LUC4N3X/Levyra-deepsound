package com.luc4n3x.levyra.nexus.playlistimport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class PlaylistTextParsersTest {
    private fun parse(format: PlaylistTextFormat, text: String) = PlaylistTextParsers.parse(format, text, "Fallback")

    @Test
    fun parsesExtendedM3u() {
        val parsed = parse(
            PlaylistTextFormat.M3U,
            """
            #EXTM3U
            #PLAYLIST:Road Trip
            #EXTINF:200,The Weeknd - Blinding Lights
            /music/blinding.mp3
            #EXTINF:-1,Queen - Bohemian Rhapsody
            https://www.youtube.com/watch?v=fJ9rUzIMcZQ
            /music/03 - Daft Punk - One More Time.flac
            """.trimIndent()
        )
        assertEquals("Road Trip", parsed.descriptor.title)
        assertEquals(3, parsed.tracks.size)
        assertEquals("Blinding Lights", parsed.tracks[0].title)
        assertEquals(listOf("The Weeknd"), parsed.tracks[0].artists)
        assertEquals(200_000L, parsed.tracks[0].durationMs)
        assertEquals("/music/blinding.mp3", parsed.tracks[0].localReference)
        assertEquals("fJ9rUzIMcZQ", parsed.tracks[1].directCatalogId)
        assertEquals("One More Time", parsed.tracks[2].title)
        assertEquals(listOf(0, 1, 2), parsed.tracks.map { it.position })
    }

    @Test
    fun parsesPls() {
        val parsed = parse(
            PlaylistTextFormat.PLS,
            "[playlist]\nFile1=/a/song.mp3\nTitle1=Artist One - Song One\nLength1=180\nFile2=/b/Artist Two - Song Two.mp3\nNumberOfEntries=2\n"
        )
        assertEquals(listOf("Song One", "Song Two"), parsed.tracks.map { it.title })
        assertEquals(180_000L, parsed.tracks[0].durationMs)
    }

    @Test
    fun parsesXspfAndRejectsDtd() {
        val parsed = parse(
            PlaylistTextFormat.XSPF,
            """<?xml version="1.0"?><playlist version="1" xmlns="http://xspf.org/ns/0/"><title>Mix &amp; Match</title><trackList>
            <track><title>Café del Mar</title><creator>Energy 52</creator><album>Café</album><duration>240000</duration></track>
            </trackList></playlist>"""
        )
        assertEquals("Mix & Match", parsed.descriptor.title)
        assertEquals("Café del Mar", parsed.tracks.single().title)
        assertEquals(240_000L, parsed.tracks.single().durationMs)
        try {
            parse(PlaylistTextFormat.XSPF, "<!DOCTYPE x [<!ENTITY a \"b\">]><playlist><trackList><track><title>&a;</title></track></trackList></playlist>")
            fail("DTD accepted")
        } catch (error: PlaylistParseException) {
            assertEquals(PlaylistParseFailure.MALFORMED, error.failure)
        }
    }

    @Test
    fun parsesExportifyCsv() {
        val csv = "﻿\"Track URI\",\"Track Name\",\"Artist Name(s)\",\"Album Name\",\"Release Date\",\"Duration (ms)\",\"Explicit\",\"ISRC\"\n" +
            "\"spotify:track:0VjIjW4GlUZAMYd2vXMi3b\",\"Blinding Lights\",\"The Weeknd\",\"After Hours\",\"2020-03-20\",\"200040\",\"false\",\"USUG11904206\"\n" +
            "\"spotify:track:7MXVkk9YMctZqd1Srtv4MB\",\"Starboy\",\"The Weeknd,Daft Punk\",\"Starboy\",\"2016-11-25\",\"230453\",\"true\",\"USUG11600976\"\n"
        val parsed = parse(PlaylistTextFormat.CSV, csv)
        assertEquals(PlaylistImportSource.EXPORTIFY, parsed.descriptor.source)
        assertEquals(2, parsed.tracks.size)
        val second = parsed.tracks[1]
        assertEquals(listOf("The Weeknd", "Daft Punk"), second.artists)
        assertEquals("USUG11600976", second.isrc)
        assertEquals(true, second.explicit)
        assertEquals(2016, second.releaseYear)
        assertEquals("7MXVkk9YMctZqd1Srtv4MB", second.sourceTrackId)
    }

    @Test
    fun parsesTuneMyMusicKreateAndTsv() {
        val tmm = parse(PlaylistTextFormat.CSV, "Track name,Artist name,Album,Playlist name,Type,ISRC\nSong A,Artist A,Album A,Chill,Playlist,\n")
        assertEquals(PlaylistImportSource.TUNEMYMUSIC, tmm.descriptor.source)
        assertEquals("Chill", tmm.descriptor.title)
        val kreate = parse(
            PlaylistTextFormat.CSV,
            "PlaylistBrowseId,PlaylistName,MediaId,Title,Artists,Duration,ThumbnailUrl\n,Mine,dQw4w9WgXcQ,Never Gonna Give You Up,Rick Astley,3:33,\n"
        )
        assertEquals(PlaylistImportSource.KREATE, kreate.descriptor.source)
        assertEquals("dQw4w9WgXcQ", kreate.tracks.single().directCatalogId)
        assertEquals(213_000L, kreate.tracks.single().durationMs)
        val tsv = parse(PlaylistTextFormat.TSV, "Title\tArtist\tDuration\nSong\tSinger\t245\n")
        assertEquals(245_000L, tsv.tracks.single().durationMs)
        val semicolon = parse(PlaylistTextFormat.CSV, "Titel;Title;Artist\nx;Song;Singer\n")
        assertEquals("Song", semicolon.tracks.single().title)
    }

    @Test
    fun csvFormulaCellsStayLiteralText() {
        val parsed = parse(PlaylistTextFormat.CSV, "Title,Artist\n\"=HYPERLINK(\"\"http://x\"\",\"\"a\"\")\",Singer\n")
        assertEquals("=HYPERLINK(\"http://x\",\"a\")", parsed.tracks.single().title)
    }

    @Test
    fun parsesJsonShapes() {
        val levyra = parse(PlaylistTextFormat.JSON, """{"name":"Mine","tracks":[{"videoId":"dQw4w9WgXcQ","title":"Never","artist":"Rick","durationMs":213000}]}""")
        assertEquals("Mine", levyra.descriptor.title)
        assertEquals("dQw4w9WgXcQ", levyra.tracks.single().directCatalogId)
        val spotifyApi = parse(
            PlaylistTextFormat.JSON,
            """{"items":[{"track":{"name":"Song","artists":[{"name":"A"},{"name":"B"}],"album":{"name":"LP"},"duration_ms":1000,"external_ids":{"isrc":"usabc1234567"},"explicit":true}}]}"""
        )
        val track = spotifyApi.tracks.single()
        assertEquals(listOf("A", "B"), track.artists)
        assertEquals("USABC1234567", track.isrc)
        assertEquals("LP", track.album)
        val array = parse(PlaylistTextFormat.JSON, """[{"title":"Ünïcødé ☕","artists":["Ñandú"],"duration":200}]""")
        assertEquals("Ünïcødé ☕", array.tracks.single().title)
        assertEquals(200_000L, array.tracks.single().durationMs)
    }

    @Test
    fun parsesTextLists() {
        val parsed = parse(PlaylistTextFormat.TEXT, "1. Adele - Hello\nBohemian Rhapsody by Queen\n# comment\nhttps://youtu.be/dQw4w9WgXcQ\nhttps://example.com/unknown\n")
        assertEquals(3, parsed.tracks.size)
        assertEquals("Hello", parsed.tracks[0].title)
        assertEquals(listOf("Queen"), parsed.tracks[1].artists)
        assertEquals("dQw4w9WgXcQ", parsed.tracks[2].directCatalogId)
        assertEquals(1, parsed.skippedRows)
    }

    @Test
    fun rejectsMalformedAndOversizedInput() {
        expectFailure(PlaylistParseFailure.MALFORMED) { parse(PlaylistTextFormat.JSON, "{\"tracks\": [") }
        expectFailure(PlaylistParseFailure.NO_TRACKS) { parse(PlaylistTextFormat.TEXT, "# only comments") }
        val rows = buildString {
            append("Title,Artist\n")
            repeat(MAX_PLAYLIST_IMPORT_TRACKS + 5) { append("Song $it,Artist\n") }
        }
        expectFailure(PlaylistParseFailure.TOO_LARGE) { parse(PlaylistTextFormat.CSV, rows) }
        val deep = "[".repeat(200) + "]".repeat(200)
        expectFailure(PlaylistParseFailure.MALFORMED) { parse(PlaylistTextFormat.JSON, deep) }
    }

    @Test
    fun largePlaylistParsesInOrder() {
        val csv = buildString {
            append("Title,Artist,Duration (ms)\n")
            repeat(3_000) { append("Song $it,Artist ${it % 7},${180_000 + it}\n") }
        }
        val parsed = parse(PlaylistTextFormat.CSV, csv)
        assertEquals(3_000, parsed.tracks.size)
        assertEquals("Song 2999", parsed.tracks.last().title)
        assertEquals(2_999, parsed.tracks.last().position)
    }

    @Test
    fun durationParsing() {
        assertEquals(213_000L, PlaylistTextParsers.durationFrom("3:33", false))
        assertEquals(3_723_000L, PlaylistTextParsers.durationFrom("1:02:03", false))
        assertEquals(200_040L, PlaylistTextParsers.durationFrom("200040", true))
        assertEquals(0L, PlaylistTextParsers.durationFrom("abc", false))
    }

    private fun expectFailure(expected: PlaylistParseFailure, block: () -> Unit) {
        try {
            block()
            fail("Expected $expected")
        } catch (error: PlaylistParseException) {
            assertEquals(expected, error.failure)
        }
    }
}

class PlaylistInputDetectorTest {
    private fun remote(input: String) = PlaylistInputDetector.detect(input) as? DetectedPlaylistInput.RemotePlaylist

    @Test
    fun detectsProviderPlaylists() {
        assertEquals(PlaylistImportSource.SPOTIFY, remote("https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M?si=abc")?.source)
        assertEquals("37i9dQZF1DXcBWIGoYBM5M", remote("https://open.spotify.com/intl-it/playlist/37i9dQZF1DXcBWIGoYBM5M")?.id)
        assertEquals("37i9dQZF1DXcBWIGoYBM5M", remote("spotify:playlist:37i9dQZF1DXcBWIGoYBM5M")?.id)
        assertEquals(PlaylistImportSource.YOUTUBE_MUSIC, remote("https://music.youtube.com/playlist?list=PLabc123")?.source)
        assertEquals("PLabc123", remote("Check this https://www.youtube.com/playlist?list=PLabc123")?.id)
        assertEquals("pl.f4d106fed2bd41149aaacabb233eb5eb", remote("https://music.apple.com/us/playlist/todays-hits/pl.f4d106fed2bd41149aaacabb233eb5eb")?.id)
        assertEquals("908622995", remote("https://www.deezer.com/it/playlist/908622995")?.id)
        assertEquals(PlaylistImportSource.DEEZER, remote("https://link.deezer.com/s/31abc")?.source)
        assertEquals(PlaylistImportSource.JIOSAAVN, remote("https://www.jiosaavn.com/featured/hindi-india-superhits-top-50/zlJfJYVuyjpxWb5,FqsjKg__")?.source)
        assertEquals(PlaylistImportSource.BANDCAMP, remote("https://radiohead.bandcamp.com/album/in-rainbows")?.source)
    }

    @Test
    fun trackLinksAreNotPlaylists() {
        assertTrue(PlaylistInputDetector.detect("https://open.spotify.com/track/0VjIjW4GlUZAMYd2vXMi3b") is DetectedPlaylistInput.NotAPlaylist)
        assertTrue(PlaylistInputDetector.detect("https://www.youtube.com/watch?v=dQw4w9WgXcQ") is DetectedPlaylistInput.NotAPlaylist)
        assertTrue(PlaylistInputDetector.detect("https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=RDdQw4w9WgXcQ") is DetectedPlaylistInput.NotAPlaylist)
        assertTrue(PlaylistInputDetector.detect("https://music.apple.com/us/album/x/123") is DetectedPlaylistInput.NotAPlaylist)
    }

    @Test
    fun unsupportedProvidersAreHonest() {
        val tidal = PlaylistInputDetector.detect("https://tidal.com/browse/playlist/0a1b2c3d-1111-2222-3333-444455556666")
        assertEquals(DetectedPlaylistInput.UnsupportedRemote(PlaylistImportSource.TIDAL, UnsupportedPlaylistReason.AUTHENTICATION_REQUIRED), tidal)
        assertTrue(PlaylistInputDetector.detect("https://soundcloud.com/user/sets/my-set") is DetectedPlaylistInput.UnsupportedRemote)
        assertTrue(PlaylistInputDetector.detect("https://music.amazon.com/playlists/B0ABC") is DetectedPlaylistInput.UnsupportedRemote)
    }

    @Test
    fun rejectsUnsafeOrUnknownUrls() {
        assertEquals(DetectedPlaylistInput.Unrecognized, PlaylistInputDetector.detect("https://user:pass@open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M"))
        assertEquals(DetectedPlaylistInput.Unrecognized, PlaylistInputDetector.detect("https://open.spotify.com:8443/playlist/37i9dQZF1DXcBWIGoYBM5M"))
        assertEquals(DetectedPlaylistInput.Unrecognized, PlaylistInputDetector.detect("https://evil.example/playlist?list=PL1"))
        assertEquals(DetectedPlaylistInput.Unrecognized, PlaylistInputDetector.detect("file:///etc/passwd"))
        assertEquals(DetectedPlaylistInput.Unrecognized, PlaylistInputDetector.detect("hello"))
    }

    @Test
    fun sniffsTextFormats() {
        fun format(text: String, name: String? = null) = (PlaylistInputDetector.detect(text, name) as? DetectedPlaylistInput.StructuredText)?.format
        assertEquals(PlaylistTextFormat.M3U, format("#EXTM3U\n#EXTINF:1,a - b\nx.mp3"))
        assertEquals(PlaylistTextFormat.PLS, format("[playlist]\nFile1=a"))
        assertEquals(PlaylistTextFormat.XSPF, format("<?xml?><playlist xmlns=\"http://xspf.org/ns/0/\">"))
        assertEquals(PlaylistTextFormat.JSON, format("{\"tracks\":[]}"))
        assertEquals(PlaylistTextFormat.CSV, format("Track Name,Artist Name(s)\na,b"))
        assertEquals(PlaylistTextFormat.TSV, format("Title\tArtist\na\tb"))
        assertEquals(PlaylistTextFormat.TEXT, format("Adele - Hello\nQueen - Innuendo"))
        assertEquals(PlaylistTextFormat.M3U, format("anything", "list.m3u8"))
        assertNull(PlaylistInputDetector.sniffFormat("just some words"))
    }
}

class PlaylistImportHealerTest {
    private fun identity(position: Int, title: String, artist: String = "Artist", sourceId: String = "") =
        ImportedTrackIdentity(position, title, listOf(artist), durationMs = 200_000L, sourceTrackId = sourceId)

    private fun candidate(id: String, title: String, artist: String = "Artist", durationMs: Long = 200_000L) =
        MatchCandidate(id, title, listOf(artist), durationMs = durationMs, kind = CandidateKind.SONG)

    private fun resolve(identity: ImportedTrackIdentity, candidates: List<MatchCandidate>): ImportEntry =
        PlaylistImportReview.applyOutcome(ImportEntry(identity), PlaylistMatchEngine.select(identity, candidates))

    @Test
    fun intentionalDuplicatesArePreservedAndFlagged() {
        val a = resolve(identity(0, "Song", sourceId = "s1"), listOf(candidate("x", "Song")))
        val b = resolve(identity(1, "Song", sourceId = "s1"), listOf(candidate("x", "Song")))
        val healed = PlaylistImportHealer.heal(listOf(a, b)).entries
        assertEquals(2, healed.size)
        assertTrue(healed.all { ImportFlag.DUPLICATE_SOURCE in it.flags })
        assertTrue(healed.none { ImportFlag.COLLISION in it.flags })
        assertEquals(ImportEntryStatus.MATCHED, healed[1].status)
        val counts = PlaylistImportReview.counts(healed)
        assertEquals(2, counts.matched)
        assertEquals(2, counts.ready)
        assertEquals(0, counts.mergedRepeats)
    }

    @Test
    fun accidentalCollisionIsHealedWithAlternative() {
        val shared = candidate("x", "Intro")
        val a = resolve(identity(0, "Intro", "Band A"), listOf(candidate("x", "Intro", "Band A")))
        val b = resolve(identity(1, "Intro", "Band A", sourceId = "other"), listOf(candidate("x", "Intro", "Band A"), candidate("y", "Intro", "Band A", 201_000L)))
        assertEquals(shared.id, b.selected?.candidate?.id)
        val result = PlaylistImportHealer.heal(listOf(a, b.copy(identity = b.identity.copy(title = "Intro (Reprise)"))))
        val second = result.entries[1]
        assertTrue(ImportFlag.COLLISION in second.flags || ImportFlag.HEALED in second.flags)
    }

    @Test
    fun collisionWithoutAlternativeGoesToReview() {
        val a = resolve(identity(0, "Song One"), listOf(candidate("x", "Song One")))
        val b = resolve(identity(1, "Song One", sourceId = "different"), listOf(candidate("x", "Song One")))
        val healed = PlaylistImportHealer.heal(listOf(a.copy(identity = a.identity.copy(sourceTrackId = "first")), b)).entries
        assertEquals(ImportEntryStatus.REVIEW, healed[1].status)
        assertTrue(ImportFlag.COLLISION in healed[1].flags)
    }

    @Test
    fun missingEntriesAreScheduledForBroaderSearch() {
        val missing = resolve(identity(0, "Unknown Song"), emptyList())
        val result = PlaylistImportHealer.heal(listOf(missing))
        assertEquals(setOf(0), result.reSearchPositions)
        assertEquals(ImportEntryStatus.MISSING, result.entries.single().status)
    }

    @Test
    fun commitIncludesOnlyConfirmedMatchesInOrder() {
        val matched = resolve(identity(0, "Song One"), listOf(candidate("a", "Song One")))
        val review = resolve(identity(1, "Song Two"), listOf(candidate("b", "Song Two (Radio Edit)")))
        val missing = resolve(identity(2, "Song Three"), emptyList())
        val entries = listOf(matched, review, missing)
        assertEquals(ImportEntryStatus.REVIEW, review.status)
        assertEquals(listOf("a"), PlaylistImportReview.commitSelection(entries).map { it.id })
        val accepted = PlaylistImportReview.acceptAllSuggestions(entries)
        assertEquals(listOf("a", "b"), PlaylistImportReview.commitSelection(accepted).map { it.id })
    }

    @Test
    fun manualChoiceSkipAndRestore() {
        val entry = resolve(identity(0, "Song One"), listOf(candidate("a", "Song One"), candidate("b", "Song One (Live)")))
        val chosen = PlaylistImportReview.choose(entry, "b")
        assertEquals("b", chosen.selected?.candidate?.id)
        assertEquals(ImportEntryStatus.MATCHED, chosen.status)
        val manual = PlaylistImportReview.chooseManual(entry, candidate("local:1", "Song One"))
        assertEquals("local:1", manual.selected?.candidate?.id)
        val skipped = PlaylistImportReview.skip(manual)
        assertEquals(ImportEntryStatus.SKIPPED, skipped.status)
        val restored = PlaylistImportReview.restoreAutomatic(skipped)
        assertEquals("a", restored.selected?.candidate?.id)
        assertEquals(ImportChoiceOrigin.AUTOMATIC, restored.origin)
        assertEquals(1, PlaylistImportReview.counts(listOf(manual)).manual)
    }
}

class RemotePlaylistPayloadsTest {
    @Test
    fun parsesSpotifyEmbed() {
        val html = """<html><script id="__NEXT_DATA__" type="application/json">{"props":{"pageProps":{"state":{"data":{"entity":{"name":"Hits","subtitle":"Spotify","coverArt":{"sources":[{"url":"https://i.scdn.co/a"}]},"trackList":[{"uri":"spotify:track:70cHKK8bHAfJrOGVnfRG9J","title":"Nicole Kidman","subtitle":"ADÉLA, Guest","duration":181270,"isExplicit":true}]}}}}}}</script></html>"""
        val page = RemotePlaylistPayloads.spotifyEmbed(html)
        assertEquals("Hits", page.title)
        val track = page.tracks.single()
        assertEquals(listOf("ADÉLA", "Guest"), track.artists)
        assertEquals(181_270L, track.durationMs)
        assertEquals(true, track.explicit)
        assertEquals("70cHKK8bHAfJrOGVnfRG9J", track.sourceTrackId)
    }

    @Test
    fun spotifyLayoutChangeFailsSafely() {
        try {
            RemotePlaylistPayloads.spotifyEmbed("<html>new layout</html>")
            fail("accepted")
        } catch (error: PlaylistParseException) {
            assertEquals(PlaylistParseFailure.PROVIDER_CHANGED, error.failure)
        }
    }

    @Test
    fun parsesDeezerPages() {
        val header = RemotePlaylistPayloads.deezerPlaylist("""{"title":"En mode 60","nb_tracks":50,"creator":{"name":"Deezer"},"picture_xl":"https://cdn/x.jpg","tracks":{"data":[{"id":1,"title":"Hey Jude (Remastered 2015)","isrc":"GBUM71505902","duration":429,"explicit_lyrics":false,"artist":{"name":"The Beatles"},"album":{"title":"1"}}]}}""")
        assertEquals(50, header.total)
        assertEquals("GBUM71505902", header.tracks.single().isrc)
        assertEquals(429_000L, header.tracks.single().durationMs)
        try {
            RemotePlaylistPayloads.deezerTracks("""{"error":{"type":"Exception","message":"Quota limit exceeded","code":4}}""")
            fail("accepted")
        } catch (error: RemotePayloadException) {
            assertEquals(4L, error.code)
        }
    }

    @Test
    fun parsesAppleMusic() {
        val html = """<script type="application/ld+json">{"@type":"MusicPlaylist","name":"Today&#8217;s Hits","numTracks":120}</script>
            <script type="application/json" id="serialized-server-data">{"data":[{"data":{"sections":[{"items":[{"id":"t1","title":"Patient Zero","duration":225868,"subtitleLinks":[{"title":"Taylor Swift"}],"tertiaryLinks":[{"title":"Album X"}],"contentDescriptor":{"kind":"song","identifiers":{"storeAdamID":"6814997425"}}}]}]}}]}</script>"""
        val page = RemotePlaylistPayloads.appleMusic(html)
        assertEquals(120, page.total)
        assertEquals("Today’s Hits", page.title)
        assertEquals("Album X", page.tracks.single().album)
        val completeness = completenessOf(page.tracks.size, page.total, IncompleteReason.PROVIDER_PAGE_LIMIT)
        assertEquals(PlaylistImportCompleteness.Incomplete(1, 120, IncompleteReason.PROVIDER_PAGE_LIMIT), completeness)
    }

    @Test
    fun parsesJioSaavnAndBandcamp() {
        val saavn = RemotePlaylistPayloads.jioSaavn("""{"title":"Top &amp; Hits","list_count":"50","list":[{"id":"x","title":"Parvati","year":"2026","explicit_content":"0","more_info":{"album":"OST","duration":"284","artistMap":{"primary_artists":[{"name":"Sadhu Tiwari"}],"featured_artists":[{"name":"Priyanshi"}]}}}]}""")
        assertEquals("Top & Hits", saavn.title)
        assertEquals(50, saavn.total)
        assertEquals(listOf("Priyanshi"), saavn.tracks.single().featuredArtists)
        val bandcamp = RemotePlaylistPayloads.bandcampAlbum("""<script type="application/ld+json">{"@type":"MusicAlbum","name":"In Rainbows","numTracks":1,"byArtist":{"name":"Radiohead"},"track":{"itemListElement":[{"position":1,"item":{"name":"15 Step","duration":"P00H03M57S"}}]}}</script>""")
        assertEquals(237_000L, bandcamp.tracks.single().durationMs)
        assertEquals(listOf("Radiohead"), bandcamp.tracks.single().artists)
    }
}