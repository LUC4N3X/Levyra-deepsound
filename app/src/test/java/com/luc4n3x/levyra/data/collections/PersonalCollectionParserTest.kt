package com.luc4n3x.levyra.data.collections

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonalCollectionParserTest {

    private fun parse(text: String, base: String? = "https://music.example.com/lists/main.m3u8"): CollectionParseResult =
        PersonalCollectionParser.parse(text.toByteArray(StandardCharsets.UTF_8), base, "Fallback")

    private fun success(text: String, base: String? = "https://music.example.com/lists/main.m3u8"): ParsedCollection {
        val result = parse(text, base)
        assertTrue("expected success but was $result", result is CollectionParseResult.Success)
        return (result as CollectionParseResult.Success).collection
    }

    private fun failure(text: String, base: String? = null): CollectionParseError {
        val result = parse(text, base)
        assertTrue("expected failure but was $result", result is CollectionParseResult.Failure)
        return (result as CollectionParseResult.Failure).error
    }

    @Test
    fun extendedM3uReadsMetadataArtworkAndPlaylistName() {
        val collection = success(
            """
            #EXTM3U
            #PLAYLIST:Road Trip
            #EXTINF:215.5 tvg-logo="covers/a.jpg" group-title="Rock",Artist One - First Song
            https://cdn.example.com/a.mp3
            #EXTALB:Second Album
            #EXTINF:-1,Untitled Without Artist
            ../songs/b.flac
            """.trimIndent()
        )
        assertEquals(PersonalCollectionFormat.M3U, collection.format)
        assertEquals("Road Trip", collection.name)
        assertEquals(2, collection.entries.size)
        val first = collection.entries[0]
        assertEquals("First Song", first.title)
        assertEquals("Artist One", first.artist)
        assertEquals(215_500L, first.durationMs)
        assertEquals("https://music.example.com/lists/covers/a.jpg", first.artworkUrl)
        val second = collection.entries[1]
        assertEquals("Untitled Without Artist", second.title)
        assertEquals("", second.artist)
        assertEquals("Second Album", second.album)
        assertEquals(0L, second.durationMs)
        assertEquals("https://music.example.com/songs/b.flac", second.audioUrl)
    }

    @Test
    fun metadataDoesNotLeakIntoTheFollowingEntry() {
        val collection = success(
            """
            #EXTM3U
            #EXTINF:100 logo="x.png",Band - Song
            a.mp3
            b.mp3
            """.trimIndent()
        )
        assertEquals("Song", collection.entries[0].title)
        assertEquals("b", collection.entries[1].title)
        assertEquals("", collection.entries[1].artist)
        assertEquals("", collection.entries[1].artworkUrl)
        assertEquals(0L, collection.entries[1].durationMs)
    }

    @Test
    fun commaInsideQuotedAttributeDoesNotSplitTheTitle() {
        val collection = success("#EXTINF:10 artwork=\"a,b.png\",Real Title\nsong.mp3")
        assertEquals("Real Title", collection.entries.single().title)
        assertEquals("https://music.example.com/lists/a,b.png", collection.entries.single().artworkUrl)
    }

    @Test
    fun plainM3uWithoutHeaderUsesFileNamesAsTitles() {
        val collection = success("https://cdn.example.com/My%20Track.ogg\r\nhttps://cdn.example.com/other.opus\r\n")
        assertEquals(listOf("My Track", "other"), collection.entries.map { it.title })
        assertEquals("Fallback", collection.name)
    }

    @Test
    fun hlsMediaManifestIsRejectedInsteadOfImportingSegments() {
        val error = failure(
            """
            #EXTM3U
            #EXT-X-VERSION:3
            #EXT-X-TARGETDURATION:10
            #EXTINF:9.97,
            segment0.ts
            #EXTINF:9.97,
            segment1.ts
            #EXT-X-ENDLIST
            """.trimIndent(),
            "https://cdn.example.com/stream/index.m3u8"
        )
        assertEquals(CollectionParseError.HlsManifest, error)
    }

    @Test
    fun hlsMasterPlaylistIsRejected() {
        val error = failure("#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=128000\naudio/index.m3u8", "https://cdn.example.com/master.m3u8")
        assertEquals(CollectionParseError.HlsManifest, error)
    }

    @Test
    fun hlsStreamAsSingleEntryInsideAPlaylistIsKept() {
        val collection = success("#EXTM3U\n#EXTINF:-1,Live Mix\nhttps://cdn.example.com/live/index.m3u8")
        assertEquals("https://cdn.example.com/live/index.m3u8", collection.entries.single().audioUrl)
    }

    @Test
    fun relativeEntriesWithoutABaseAreReportedNotGuessed() {
        val collection = success("songs/a.mp3\nhttps://cdn.example.com/b.mp3", base = null)
        assertEquals(1, collection.entries.size)
        assertEquals(1, collection.issueCount(CollectionIssueKind.RelativeWithoutBase))
    }

    @Test
    fun dangerousSchemesAreRejected() {
        val collection = success(
            """
            file:///sdcard/Music/a.mp3
            content://media/external/audio/media/1
            javascript:alert(1)
            data:audio/mp3;base64,AAAA
            C:\Music\local.mp3
            ftp://server/a.mp3
            https://cdn.example.com/ok.mp3
            """.trimIndent()
        )
        assertEquals(listOf("https://cdn.example.com/ok.mp3"), collection.entries.map { it.audioUrl })
        assertEquals(6, collection.issueCount(CollectionIssueKind.UnsupportedScheme))
    }

    @Test
    fun credentialsAreStrippedFromStoredUrlsAndCollectedPerOrigin() {
        val collection = success("https://alice:s3cret@music.example.com/a.mp3\nhttps://alice:s3cret@music.example.com/b.mp3")
        assertTrue(collection.entries.all { !it.audioUrl.contains("alice") && !it.audioUrl.contains("s3cret") })
        val credential = collection.credentials.single()
        assertEquals("https://music.example.com:443", credential.origin)
        assertEquals("alice", credential.username)
        assertEquals("s3cret", credential.password)
        assertEquals(2, collection.issueCount(CollectionIssueKind.CredentialsRemoved))
        assertEquals(0, collection.skippedCount)
    }

    @Test
    fun duplicateUrlsAreMergedAndCounted() {
        val collection = success(
            "https://cdn.example.com/a.mp3\nhttps://CDN.example.com/a.mp3#t=10\nhttps://cdn.example.com/b.mp3"
        )
        assertEquals(2, collection.entries.size)
        assertEquals(1, collection.issueCount(CollectionIssueKind.Duplicate))
    }

    @Test
    fun entryKeysAreStableAcrossReorderAndMetadataChanges() {
        val first = success("#EXTINF:1,A - One\nhttps://cdn.example.com/a.mp3\nhttps://cdn.example.com/b.mp3")
        val second = success("https://cdn.example.com/b.mp3\n#EXTINF:1,A - Renamed\nhttps://cdn.example.com/a.mp3")
        assertEquals(first.entries[0].entryKey, second.entries[1].entryKey)
        assertEquals(first.entries[1].entryKey, second.entries[0].entryKey)
        assertNotEquals(first.entries[0].entryKey, first.entries[1].entryKey)
    }

    @Test
    fun jsonDocumentResolvesBaseUrlAndReadsAllFields() {
        val collection = success(
            """
            {
              "name": "Vinyl Rips",
              "baseUrl": "https://files.example.org/music/",
              "artwork": "cover.png",
              "tracks": [
                {"id": "track-1", "title": "Intro", "artist": "Band", "album": "LP", "albumArtist": "Band",
                 "duration": 61.5, "url": "lp/01.flac", "artwork": "lp/front.jpg", "videoUrl": "lp/01.mp4",
                 "trackNumber": 1, "discNumber": 2, "year": 1999},
                {"title": "Outro", "artists": ["A", "B"], "durationMs": 120000, "audioUrl": "https://other.example.org/02.mp3"}
              ]
            }
            """.trimIndent(),
            base = null
        )
        assertEquals(PersonalCollectionFormat.JSON, collection.format)
        assertEquals("Vinyl Rips", collection.name)
        assertEquals("https://files.example.org/music/cover.png", collection.artworkUrl)
        val intro = collection.entries[0]
        assertEquals("https://files.example.org/music/lp/01.flac", intro.audioUrl)
        assertEquals("https://files.example.org/music/lp/front.jpg", intro.artworkUrl)
        assertEquals("https://files.example.org/music/lp/01.mp4", intro.videoUrl)
        assertEquals(61_500L, intro.durationMs)
        assertEquals(1, intro.trackNumber)
        assertEquals(2, intro.discNumber)
        assertEquals(1999, intro.year)
        assertTrue(intro.entryKey.startsWith("i"))
        val outro = collection.entries[1]
        assertEquals("A, B", outro.artist)
        assertEquals(120_000L, outro.durationMs)
        assertTrue(outro.entryKey.startsWith("u"))
    }

    @Test
    fun jsonBaseUrlCredentialsAreStoredSeparatelyFromResolvedTracks() {
        val collection = success(
            """{"baseUrl":"https://alice:secret@files.example.org/music/","tracks":[{"url":"one.mp3"}]}""",
            base = null
        )

        assertEquals("https://files.example.org/music/one.mp3", collection.entries.single().audioUrl)
        assertEquals(CollectionCredential("https://files.example.org:443", "alice", "secret"), collection.credentials.single())
        assertEquals(1, collection.issueCount(CollectionIssueKind.CredentialsRemoved))
    }

    @Test
    fun jsonDeclaredIdKeepsIdentityWhenTheUrlMoves() {
        val before = success("""[{"id":"x","url":"https://a.example.com/one.mp3"}]""")
        val after = success("""[{"id":"x","url":"https://b.example.com/moved.mp3"}]""")
        assertEquals(before.entries.single().entryKey, after.entries.single().entryKey)
    }

    @Test
    fun jsonArrayOfPlainUrlsIsAccepted() {
        val collection = success("""["https://cdn.example.com/a.mp3", 42, {"title":"no url"}]""")
        assertEquals(1, collection.entries.size)
        assertEquals(2, collection.issueCount(CollectionIssueKind.MissingUrl))
    }

    @Test
    fun malformedAndEmptyInputsAreFailuresNotEmptyCollections() {
        assertEquals(CollectionParseError.Malformed, failure("{\"tracks\": [ {\"url\": "))
        assertEquals(CollectionParseError.Malformed, failure("{\"name\": \"only metadata\"}"))
        assertEquals(CollectionParseError.Empty, failure("   \n  "))
        assertEquals(CollectionParseError.NoPlayableEntries, failure("#EXTM3U\n#EXTINF:1,Nothing here"))
        assertEquals(CollectionParseError.NoPlayableEntries, failure("""{"tracks": []}"""))
        assertEquals(CollectionParseError.NoPlayableEntries, failure("<html><body>Login</body></html>"))
    }

    @Test
    fun oversizedPayloadIsRejectedBeforeParsing() {
        val bytes = ByteArray(PersonalCollectionParser.MAX_PLAYLIST_BYTES + 1) { 'a'.code.toByte() }
        val result = PersonalCollectionParser.parse(bytes, null, "x")
        assertEquals(CollectionParseResult.Failure(CollectionParseError.TooLarge), result)
    }

    @Test
    fun largePlaylistIsCappedAndReportsTruncation() {
        val total = PersonalCollectionParser.MAX_ENTRIES + 25
        val text = buildString {
            append("#EXTM3U\n")
            repeat(total) { index -> append("#EXTINF:180,Artist - Song ").append(index).append("\nhttps://cdn.example.com/").append(index).append(".mp3\n") }
        }
        val started = System.nanoTime()
        val collection = success(text)
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        assertEquals(PersonalCollectionParser.MAX_ENTRIES, collection.entries.size)
        assertEquals(1, collection.issueCount(CollectionIssueKind.Truncated))
        assertTrue("parsing took ${elapsedMs}ms", elapsedMs < 10_000)
    }

    @Test
    fun latin1PlaylistFallsBackWhenNotValidUtf8() {
        val latin1 = Charset.forName("windows-1252")
        val bytes = "#EXTINF:10,Beyoncé - Café Olé\nhttps://cdn.example.com/a.mp3".toByteArray(latin1)
        val collection = (PersonalCollectionParser.parse(bytes, null, "x") as CollectionParseResult.Success).collection
        assertEquals("Café Olé", collection.entries.single().title)
        assertEquals("Beyoncé", collection.entries.single().artist)
    }

    @Test
    fun byteOrderMarksAreHonoured() {
        val utf8 = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "#EXTM3U\n#EXTINF:1,日本 - 曲\nhttps://cdn.example.com/a.mp3".toByteArray()
        val utf16 = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + "#EXTINF:1,Ünï - Çødé\nhttps://cdn.example.com/b.mp3".toByteArray(StandardCharsets.UTF_16LE)
        val first = (PersonalCollectionParser.parse(utf8, null, "x") as CollectionParseResult.Success).collection
        val second = (PersonalCollectionParser.parse(utf16, null, "x") as CollectionParseResult.Success).collection
        assertEquals("曲", first.entries.single().title)
        assertEquals("Çødé", second.entries.single().title)
    }

    @Test
    fun controlCharactersAndHugeTitlesAreSanitized() {
        val longTitle = "x".repeat(2_000)
        val collection = success("#EXTINF:1,Band\u0000\u0007 - $longTitle\nhttps://cdn.example.com/a.mp3")
        assertEquals("Band", collection.entries.single().artist)
        assertEquals(PersonalCollectionParser.MAX_TEXT_LENGTH, collection.entries.single().title.length)
    }

    @Test
    fun specialCharactersInPathsAreEncodedSafely() {
        val collection = success("Sigur Rós/Ágætis byrjun/01 Intro (live) [1].mp3")
        val url = collection.entries.single().audioUrl
        assertTrue(url, url.startsWith("https://music.example.com/lists/Sigur%20R"))
        assertTrue(url, !url.contains(' '))
    }
}
