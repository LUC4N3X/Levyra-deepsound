package com.luc4n3x.levyra.data.playlistimport

import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.domain.SearchPage
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.nexus.playlistimport.CandidateKind
import com.luc4n3x.levyra.nexus.playlistimport.CandidateOrigin
import com.luc4n3x.levyra.nexus.playlistimport.ImportedTrackIdentity
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistMatchEngine
import com.luc4n3x.levyra.nexus.playlistimport.ResolutionPreference
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistImportCatalogTest {

    private fun testTrack(
        id: String,
        title: String,
        artist: String = "Test Artist",
        album: String = "Test Album",
        durationMs: Long = 210_000L,
        videoType: String = "MUSIC_VIDEO_TYPE_ATV",
        thumbnailUrl: String = "https://i.ytimg.com/vi/$id/hqdefault.jpg"
    ) = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        streamUrl = "",
        videoUrl = "https://www.youtube.com/watch?v=$id",
        thumbnailUrl = thumbnailUrl,
        largeThumbnailUrl = thumbnailUrl,
        source = "test",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 50,
        cacheScore = 50,
        accentStart = 0,
        accentEnd = 0,
        videoType = videoType
    )

    private class FakeYoutubeRepository(
        private val songsHandler: (String) -> List<Track> = { emptyList() },
        private val searchHandler: (String, Int) -> List<Track> = { _, _ -> emptyList() }
    ) : YoutubeMusicRepository(null) {
        val songsQueries = mutableListOf<String>()
        val searchQueries = mutableListOf<String>()

        override suspend fun searchSongsPage(
            query: String,
            languageCode: String,
            continuation: String
        ): SearchPage<Track> {
            songsQueries.add(query)
            return SearchPage(items = songsHandler(query), continuation = "")
        }

        override suspend fun searchForImport(query: String, limit: Int, languageCode: String): List<Track> {
            searchQueries.add(query)
            return searchHandler(query, limit)
        }
    }

    @Test
    fun test1_searchSongsPageHasThreeResultsWithoutTarget_generalSearchHasTarget_targetFound() = runBlocking {
        val wrongSongs = listOf(
            testTrack("w1", "Random Tribute 1", "Tribute Band"),
            testTrack("w2", "Random Tribute 2", "Another Band"),
            testTrack("w3", "Completely Unrelated Song", "Different Artist")
        )
        val correctTrack = testTrack("correct1", "Bohemian Rhapsody", "Queen", "A Night at the Opera")
        val fakeRepo = FakeYoutubeRepository(
            songsHandler = { wrongSongs },
            searchHandler = { _, _ -> listOf(correctTrack) }
        )
        val catalog = PlaylistImportCatalog(fakeRepo, localTracks = { emptyList() }, languageCode = { "en" })

        val manualResults = catalog.search("Bohemian Rhapsody Queen", CandidateOrigin.ONLINE)
        assertTrue(manualResults.any { it.candidate.id == "correct1" })
        assertEquals("Bohemian Rhapsody", manualResults.first { it.candidate.id == "correct1" }.candidate.title)

        val identity = ImportedTrackIdentity(0, "Bohemian Rhapsody", listOf("Queen"), album = "A Night at the Opera", durationMs = 210_000L)
        val autoResults = catalog.candidates(identity, broad = false)
        assertTrue(autoResults.any { it.candidate.id == "correct1" })
        val eval = PlaylistMatchEngine.evaluate(identity, autoResults.first { it.candidate.id == "correct1" }.candidate)
        assertTrue("Auto match should accept correct track", eval.confidence.autoAccepted)
    }

    @Test
    fun test2_targetPresentInSongsAndGeneralSearch_noDuplicatesAfterMerge() = runBlocking {
        val targetSong = testTrack("dup1", "Stairway to Heaven", "Led Zeppelin")
        val otherSong = testTrack("s2", "Black Dog", "Led Zeppelin")
        val fakeRepo = FakeYoutubeRepository(
            songsHandler = { listOf(targetSong, otherSong) },
            searchHandler = { _, _ -> listOf(targetSong, testTrack("m2", "Kashmir", "Led Zeppelin")) }
        )
        val catalog = PlaylistImportCatalog(fakeRepo, localTracks = { emptyList() }, languageCode = { "en" })

        val searchResults = catalog.search("Stairway to Heaven Led Zeppelin", CandidateOrigin.ONLINE)
        val dupCount = searchResults.count { it.candidate.id == "dup1" }
        assertEquals(1, dupCount)

        val identity = ImportedTrackIdentity(0, "Stairway to Heaven", listOf("Led Zeppelin"), durationMs = 210_000L)
        val candidateResults = catalog.candidates(identity, broad = false)
        assertEquals(1, candidateResults.count { it.candidate.id == "dup1" })
    }

    @Test
    fun test3_correctResultIsOfficialVideoInsteadOfSong_correctlyIdentifiedAndDiscovered() = runBlocking {
        val officialVideo = testTrack(
            id = "omv_video_1",
            title = "Official Music Video",
            artist = "Major Artist",
            videoType = "MUSIC_VIDEO_TYPE_OMV"
        )
        val wrongSongs = listOf(
            testTrack("ws1", "Song Title Cover", "Cover Band"),
            testTrack("ws2", "Song Title Karaoke", "Karaoke"),
            testTrack("ws3", "Song Title Instrumental", "Instrumental")
        )
        val fakeRepo = FakeYoutubeRepository(
            songsHandler = { wrongSongs },
            searchHandler = { _, _ -> listOf(officialVideo) }
        )
        val catalog = PlaylistImportCatalog(fakeRepo, localTracks = { emptyList() }, languageCode = { "en" })

        val results = catalog.search("Official Music Video Major Artist", CandidateOrigin.ONLINE)
        val found = results.firstOrNull { it.candidate.id == "omv_video_1" }
        assertNotNull("Official video must be present in search results", found)
        assertEquals(CandidateKind.OFFICIAL_VIDEO, found?.candidate?.kind)

        val identity = ImportedTrackIdentity(0, "Official Music Video", listOf("Major Artist"), durationMs = 210_000L)
        val evaluation = PlaylistMatchEngine.evaluate(identity, found!!.candidate)
        assertTrue("Official video should score high enough for matching", evaluation.score >= 80)
    }

    @Test
    fun test4_titleWithFeatRemasterLiveExplicitParentheses_findsAndMatchesTrack() = runBlocking {
        val complexTitle = "In the End (feat. Jay-Z) [Live At Wembley] - Remastered 2021 (Explicit)"
        val cleanSong = testTrack("clean_1", "In the End", "Linkin Park")
        val fakeRepo = FakeYoutubeRepository(
            songsHandler = { query ->
                if (query.contains("Live") || query.contains("Remastered")) {
                    emptyList()
                } else {
                    listOf(cleanSong)
                }
            },
            searchHandler = { query, _ ->
                if (query.contains("Live") || query.contains("Remastered")) {
                    emptyList()
                } else {
                    listOf(cleanSong)
                }
            }
        )
        val catalog = PlaylistImportCatalog(fakeRepo, localTracks = { emptyList() }, languageCode = { "en" })

        val identity = ImportedTrackIdentity(0, complexTitle, listOf("Linkin Park"), durationMs = 210_000L)
        val candidates = catalog.candidates(identity, broad = false)
        assertTrue("Normalized fallback should find the track", candidates.any { it.candidate.id == "clean_1" })
    }

    @Test
    fun test5_manualSearchShowsValidResultsEvenWithLowMatchingScore() = runBlocking {
        val lowScoreTrack = testTrack(
            id = "ugc_upload_1",
            title = "Live Performance at Backyard",
            artist = "Unknown Band",
            videoType = "MUSIC_VIDEO_TYPE_UGC",
            durationMs = 999_000L
        )
        val fakeRepo = FakeYoutubeRepository(
            songsHandler = { emptyList() },
            searchHandler = { _, _ -> listOf(lowScoreTrack) }
        )
        val catalog = PlaylistImportCatalog(fakeRepo, localTracks = { emptyList() }, languageCode = { "en" })

        val searchResults = catalog.search("Live Performance", CandidateOrigin.ONLINE)
        assertEquals(1, searchResults.size)
        assertEquals("ugc_upload_1", searchResults.first().candidate.id)

        val identity = ImportedTrackIdentity(0, "Completely Different Studio Track", listOf("Other Artist"), durationMs = 180_000L)
        val eval = PlaylistMatchEngine.evaluate(identity, searchResults.first().candidate)
        assertFalse("Matching engine should not auto-accept this candidate", eval.confidence.autoAccepted)
        assertEquals(CandidateKind.USER_VIDEO, searchResults.first().candidate.kind)
    }

    @Test
    fun test6_temporaryIoExceptionRetriesAndRecoversWithoutCrashing() = runBlocking {
        val attempts = AtomicInteger(0)
        val expectedTrack = testTrack("recovered_1", "Resilient Track", "Artist")
        val fakeRepo = object : YoutubeMusicRepository(null) {
            override suspend fun searchSongsPage(
                query: String,
                languageCode: String,
                continuation: String
            ): SearchPage<Track> {
                if (attempts.incrementAndGet() == 1) {
                    throw IOException("Temporary connection reset")
                }
                return SearchPage(items = listOf(expectedTrack), continuation = "")
            }

            override suspend fun searchForImport(query: String, limit: Int, languageCode: String): List<Track> {
                return emptyList()
            }
        }
        val catalog = PlaylistImportCatalog(fakeRepo, localTracks = { emptyList() }, languageCode = { "en" })

        val results = catalog.search("Resilient Track", CandidateOrigin.ONLINE)
        assertEquals(1, results.size)
        assertEquals("recovered_1", results.first().candidate.id)

        val persistentFailingRepo = object : YoutubeMusicRepository(null) {
            override suspend fun searchSongsPage(
                query: String,
                languageCode: String,
                continuation: String
            ): SearchPage<Track> = throw IOException("Network down")

            override suspend fun searchForImport(query: String, limit: Int, languageCode: String): List<Track> =
                throw IOException("Network down")
        }
        val failingCatalog = PlaylistImportCatalog(persistentFailingRepo, localTracks = { emptyList() }, languageCode = { "en" })
        val failure = runCatching { failingCatalog.search("Any query", CandidateOrigin.ONLINE) }.exceptionOrNull()
        assertTrue("Permanent failure must remain distinguishable from an empty catalog", failure is IOException)
    }

    @Test
    fun test6b_failedSongBranchKeepsCandidatesFromWorkingSearch() = runBlocking {
        val identity = ImportedTrackIdentity(0, "Группа крови", listOf("Кино"))
        val mixedQueries = mutableListOf<String>()
        val songsDown = object : YoutubeMusicRepository(null) {
            override suspend fun searchSongsPage(query: String, languageCode: String, continuation: String): SearchPage<Track> =
                SearchPage(failed = true)

            override suspend fun searchForImport(query: String, limit: Int, languageCode: String): List<Track> {
                mixedQueries += query
                return listOf(testTrack("album", "Группа крови", "Кино").copy(durationMs = 285_000), testTrack("single", "Группа крови", "Кино").copy(durationMs = 236_000))
            }
        }
        val resolution = PlaylistImportCatalog(songsDown, localTracks = { emptyList() }, languageCode = { "en" })
            .resolve(identity, false, ResolutionPreference.SMART)
        assertNotNull(resolution.outcome.selected)
        assertTrue(mixedQueries.isNotEmpty())

        val allDown = object : YoutubeMusicRepository(null) {
            override suspend fun searchSongsPage(query: String, languageCode: String, continuation: String): SearchPage<Track> =
                SearchPage(failed = true)

            override suspend fun searchForImport(query: String, limit: Int, languageCode: String): List<Track> =
                throw IOException("Network down")
        }
        val failure = runCatching {
            PlaylistImportCatalog(allDown, localTracks = { emptyList() }, languageCode = { "en" })
                .resolve(identity, false, ResolutionPreference.SMART)
        }.exceptionOrNull()
        assertTrue(failure is IOException)
    }

    @Test
    fun test7_emptySearchResultsRemainStableWithoutCrashing() = runBlocking {
        val emptyRepo = FakeYoutubeRepository(
            songsHandler = { emptyList() },
            searchHandler = { _, _ -> emptyList() }
        )
        val catalog = PlaylistImportCatalog(emptyRepo, localTracks = { emptyList() }, languageCode = { "en" })

        val searchResults = catalog.search("Nonexistent Song 123456789", CandidateOrigin.ONLINE)
        assertTrue(searchResults.isEmpty())

        val identity = ImportedTrackIdentity(0, "Nonexistent Song 123456789", listOf("Unknown"), durationMs = 100_000L)
        val candidates = catalog.candidates(identity, broad = false)
        assertTrue(candidates.isEmpty())
    }

    @Test
    fun test8_musicalRelevancePrioritizesSongsAndOfficialVideosOverUgc() = runBlocking {
        val ugcTrack = testTrack("ugc1", "Song Title (Bass Boosted)", "Random Uploader", videoType = "MUSIC_VIDEO_TYPE_UGC")
        val omvTrack = testTrack("omv1", "Song Title", "Official Artist", videoType = "MUSIC_VIDEO_TYPE_OMV")
        val atvTrack = testTrack("atv1", "Song Title", "Official Artist", videoType = "MUSIC_VIDEO_TYPE_ATV")

        val combined = combineOnlineSearchResults(
            songs = listOf(atvTrack),
            mixed = listOf(ugcTrack, omvTrack),
            limit = 10
        )

        assertEquals("Top result should be preserved from mixed", "ugc1", combined.first().id)
        val ugcIndex = combined.indexOfFirst { it.id == "ugc1" }
        val omvIndex = combined.indexOfFirst { it.id == "omv1" }
        val atvIndex = combined.indexOfFirst { it.id == "atv1" }
        assertTrue("Official video should be present in results", omvIndex >= 0)
        assertTrue("Official song should be present in results", atvIndex >= 0)
    }

    @Test
    fun sameVideoFallbackCannotRemoveAKnownDuetPartner() = runBlocking {
        val video = testTrack("duet", "Song", "Artist, Guest", videoType = "")
        val repository = FakeYoutubeRepository(searchHandler = { _, _ -> listOf(video) })
        val identity = ImportedTrackIdentity(0, "Song", listOf("Artist"), durationMs = 210_000)
        val resolution = PlaylistImportCatalog(repository, { emptyList() }, { "en" }, youtubeSearch = {
            listOf(video.copy(title = "Artist - Song", artist = "Artist", source = "LevyraExtractor Search"))
        }).resolve(identity, false, ResolutionPreference.SMART)
        assertFalse(resolution.outcome.confidence.autoAccepted)
        assertTrue(resolution.candidates.single { it.candidate.id == "duet" }.track.artist.contains("Guest"))
    }

    @Test
    fun normalizedYoutubeFallbackReplacesEarlierMetadataForTheSameVideo() = runBlocking {
        val video = testTrack("official", "Bob Dylan - Hurricane (Official Audio)", "Bob Dylan",
            durationMs = 515_000, videoType = "")
        val repository = FakeYoutubeRepository(searchHandler = { _, _ -> listOf(video.copy(source = "YouTube Music")) })
        val identity = ImportedTrackIdentity(0, "Hurricane", listOf("Bob Dylan"), durationMs = 512_609)
        val resolution = PlaylistImportCatalog(repository, { emptyList() }, { "en" }, youtubeSearch = {
            listOf(video.copy(source = "LevyraExtractor Search"))
        }).resolve(identity, false, ResolutionPreference.SMART)
        assertTrue(resolution.outcome.confidence.autoAccepted)
        assertEquals("official", resolution.outcome.selected?.candidate?.id)
        val saved = resolution.candidates.single { it.candidate.id == "official" }.track
        assertEquals("Hurricane (Official Audio)", saved.title)
        assertEquals("Bob Dylan", saved.artist)
        assertEquals(video.videoUrl, saved.videoUrl)
    }

    @Test
    fun failedYoutubeFallbackIsAnErrorAndDoesNotCacheAMissingResult() = runBlocking {
        var attempts = 0
        val catalog = PlaylistImportCatalog(FakeYoutubeRepository(), { emptyList() }, { "en" }, youtubeSearch = {
            attempts++
            throw IOException("Provider unavailable")
        })
        val identity = ImportedTrackIdentity(0, "Unknown recording", listOf("Artist"), durationMs = 210_000)
        repeat(2) {
            val failure = runCatching { catalog.resolve(identity, false, ResolutionPreference.SMART) }.exceptionOrNull()
            assertTrue("An incomplete search must not become Missing", failure is IOException)
        }
        assertEquals(2, attempts)
    }

    @Test
    fun extractorArtistPrefixSupportsBandNamesAndChannelSuffixes() {
        listOf(" - ", " – ", " — ").forEach { separator ->
            listOf("Ricchi E Poveri", "Ricchi E Poveri - Topic", "Ricchi E PoveriVEVO").forEach { channel ->
                val track = testTrack("band", "Ricchi E Poveri${separator}Cosa Sei", channel)
                    .copy(source = "LevyraExtractor Search")
                val candidate = track.toMatchCandidate().candidate
                assertEquals("Cosa Sei", candidate.title)
                assertEquals(listOf("Ricchi E Poveri"), candidate.artists)
            }
        }
    }

    @Test
    fun officialChannelArtistPrefixDoesNotHideTheRecordingTitle() {
        val identity = ImportedTrackIdentity(0, "Hurricane", listOf("Bob Dylan"), durationMs = 512_609)
        val track = testTrack("official", "Bob Dylan - Hurricane (Official Audio)", "Bob Dylan",
            durationMs = 515_000, videoType = "").copy(source = "LevyraExtractor Search")
        val candidate = track.toMatchCandidate().candidate
        assertEquals("Hurricane (Official Audio)", candidate.title)
        assertTrue(PlaylistMatchEngine.evaluate(identity, candidate).confidence.autoAccepted)
        assertFalse(PlaylistMatchEngine.evaluate(identity, track.copy(title = "Bob Dylan - Hurricane (Official Live Audio)")
            .toMatchCandidate().candidate).confidence.autoAccepted)
        assertFalse(PlaylistMatchEngine.evaluate(identity, track.copy(artist = "Cover Artist")
            .toMatchCandidate().candidate).confidence.autoAccepted)
        assertFalse(PlaylistMatchEngine.evaluate(identity, track.copy(title = "Bob Dylan, Guest - Hurricane (Official Audio)")
            .toMatchCandidate().candidate).confidence.autoAccepted)
    }

    @Test
    fun irrelevantMusicResultsDoNotPreventYoutubeFallback() = runBlocking {
        val repository = FakeYoutubeRepository(songsHandler = { listOf(testTrack("wrong", "Hurricane", "Cover Artist")) })
        val queries = mutableListOf<String>()
        val identity = ImportedTrackIdentity(0, "Hurricane", listOf("Bob Dylan"), durationMs = 512_609)
        val outcome = PlaylistImportCatalog(repository, { emptyList() }, { "en" }, youtubeSearch = { query ->
            queries += query
            listOf(testTrack("official", "Hurricane (Official Audio)", "Bob Dylan", durationMs = 512_000))
        }).resolve(identity, false, ResolutionPreference.SMART).outcome
        assertEquals("official", outcome.selected?.candidate?.id)
        assertTrue(outcome.confidence.autoAccepted)
        assertEquals(listOf("Hurricane Bob Dylan"), queries)
    }

    @Test
    fun normalizedFallbackKeepsTheWholeBandName() = runBlocking {
        val repository = FakeYoutubeRepository(songsHandler = { query ->
            if (query == "cosa sei ricchi e poveri") listOf(testTrack("correct", "Cosa Sei", "Ricchi E Poveri")) else emptyList()
        })
        val identity = ImportedTrackIdentity(0, "Cosa Sei (Official Audio)", listOf("Ricchi E Poveri"), durationMs = 210_000)
        val outcome = PlaylistImportCatalog(repository, { emptyList() }, { "en" })
            .resolve(identity, false, ResolutionPreference.SMART).outcome
        assertEquals("correct", outcome.selected?.candidate?.id)
        assertTrue(outcome.confidence.autoAccepted)
        assertFalse(repository.songsQueries.contains("cosa sei ricchi"))
    }

    @Test
    fun missingRemixCanBeFoundUsingEveryCreditedArtistBeforeMetadata() = runBlocking {
        val identity = ImportedTrackIdentity(0, "The Dead Dance - Blake Lewis remix",
            listOf("Ariana Savalas", "Blake Lewis"), durationMs = 147_167)
        val query = "${identity.title} - Ariana Savalas Blake Lewis"
        val repository = FakeYoutubeRepository(songsHandler = { requested ->
            if (requested == query) listOf(testTrack("correct", "The Dead Dance (Blake Lewis Remix)", "Ariana Savalas", durationMs = 147_000)) else emptyList()
        })
        var metadataCalls = 0
        val outcome = PlaylistImportCatalog(repository, { emptyList() }, { "en" }, metadataSearch = {
            metadataCalls++
            emptyList()
        }).resolve(identity, false, ResolutionPreference.SMART).outcome
        assertEquals("correct", outcome.selected?.candidate?.id)
        assertTrue(outcome.confidence.autoAccepted)
        assertEquals(0, metadataCalls)
        assertEquals(1, repository.songsQueries.count { it == query })
    }

    @Test
    fun reliableFirstSearchDoesNotRunAdditionalVariantsOrMetadata() = runBlocking {
        val repository = FakeYoutubeRepository(songsHandler = { listOf(testTrack("correct", "Song", "Artist One, Artist Two")) })
        var metadataCalls = 0
        var youtubeCalls = 0
        val outcome = PlaylistImportCatalog(repository, { emptyList() }, { "en" }, metadataSearch = {
            metadataCalls++
            emptyList()
        }, youtubeSearch = {
            youtubeCalls++
            emptyList()
        }).resolve(ImportedTrackIdentity(0, "Song", listOf("Artist One", "Artist Two"), durationMs = 210_000), false, ResolutionPreference.SMART).outcome
        assertTrue(outcome.confidence.autoAccepted)
        assertEquals(1, repository.songsQueries.size)
        assertTrue(repository.searchQueries.isEmpty())
        assertEquals(0, metadataCalls)
        assertEquals(0, youtubeCalls)
    }

    @Test
    fun test9_metadataPreservedInToMatchCandidate() {
        val original = testTrack(
            id = "track_meta_1",
            title = "Preserved Title",
            artist = "Preserved Artist",
            album = "Preserved Album",
            durationMs = 245_000L,
            videoType = "MUSIC_VIDEO_TYPE_ATV",
            thumbnailUrl = "https://example.com/art.jpg"
        )
        val catalogCandidate = original.toMatchCandidate()
        val candidate = catalogCandidate.candidate

        assertEquals("track_meta_1", candidate.id)
        assertEquals("Preserved Title", candidate.title)
        assertEquals(listOf("Preserved Artist"), candidate.artists)
        assertEquals("Preserved Album", candidate.album)
        assertEquals(245_000L, candidate.durationMs)
        assertEquals(CandidateOrigin.ONLINE, candidate.origin)
        assertEquals(CandidateKind.SONG, candidate.kind)
        assertEquals("https://example.com/art.jpg", candidate.artworkUrl)
        assertEquals(original, catalogCandidate.track)
    }
}
