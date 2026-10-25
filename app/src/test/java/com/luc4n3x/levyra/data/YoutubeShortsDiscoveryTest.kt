package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.player.LevyraMediaItemFactory
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class YoutubeShortsDiscoveryTest {
    @Test
    fun previewDiscoveryHasEnoughDistinctArtistsAndKeepsLanguageAndTaste() {
        val artists = musicPreviewArtists(listOf("Ernia", "Drake"), "it")
        assertTrue(artists.size >= 10)
        assertEquals(artists.size, artists.distinct().size)
        assertTrue(artists.contains("Drake"))
        assertTrue(artists.contains("Geolier"))
        val french = musicPreviewArtists(emptyList(), "fr")
        assertTrue(french.contains("Aya Nakamura"))
        assertTrue(french.none { it == "Geolier" || it == "Ernia" })
    }

    @Test
    fun musicPreviewsFillTenCardsWithDifferentArtists() {
        val candidates = List(12) { index -> musicVideoPreview(sample("clip$index", "Artist $index").copy(durationMs = 180_000L)) }
        val result = rankYoutubeShortDiscovery(emptyList(), candidates, emptyList(), emptyList(), 12)
        assertEquals(12, result.size)
        assertEquals(12, result.map { it.artist }.distinct().size)
        assertEquals(30_000L, result.first().durationMs)
        assertEquals(45_000L, LevyraMediaItemFactory.samplesClipping(result.first(), true)!!.startPositionMs)
    }

    @Test
    fun genreMetadataFollowsTheArtistAndCollaborationDoesNotRepeatIt() {
        val seed = sample("seed", "Drake").copy(moodTags = setOf("rap"))
        val clips = listOf(sample("solo", "Drake"), sample("collab", "Drake, 21 Savage"))
        val result = rankYoutubeShortDiscovery(clips, emptyList(), listOf(seed), listOf("Drake"), 8)

        assertEquals(1, result.size)
        assertEquals("rap", youtubeShortGenreTag(result.single().moodTags))
        assertEquals("pop", youtubeShortGenreTag(setOf("pop", "shorts")))
    }

    @Test
    fun preferredMusicReplacesUnrelatedAnonymousSamples() {
        val generic = sample("generic", "Unrelated")
        val drake = sample("drake", "DrakeVEVO")
        val result = rankYoutubeShortDiscovery(listOf(generic), listOf(drake), emptyList(), listOf("Drake"), 8)

        assertEquals(listOf(drake), result)
    }

    @Test
    fun seedsAdaptSelectionAndArtistRepetitionIsBounded() {
        val candidates = listOf(sample("one", "Ernia"), sample("two", "Ernia"), sample("three", "Ernia"), sample("four", "Drake"))
        val result = rankYoutubeShortDiscovery(candidates, emptyList(), listOf(sample("seed", "Ernia")), listOf("Drake"), 8)

        assertEquals("Drake", result.first().artist)
        assertEquals(1, result.count { it.artist == "Ernia" })
    }

    @Test
    fun changingTasteChangesRecommendationsWithoutHardcodedArtists() {
        val candidates = listOf(sample("one", "Drake"), sample("two", "Stromae"))

        assertEquals("Drake", rankYoutubeShortDiscovery(candidates, emptyList(), emptyList(), listOf("Drake"), 8).single().artist)
        assertEquals("Stromae", rankYoutubeShortDiscovery(candidates, emptyList(), emptyList(), listOf("Stromae"), 8).single().artist)
        assertTrue(rankYoutubeShortDiscovery(candidates, emptyList(), emptyList(), listOf("Drake"), 0).isEmpty())
    }

    @Test
    fun anonymousFallbackKeepsMusicSamplesAndRejectsGenericShorts() {
        val music = sample("music", "Artist")
        val unrelatedShort = sample("short", "Blogger").copy(album = "YouTube Shorts")

        assertEquals(listOf(music), rankYoutubeShortDiscovery(listOf(music), listOf(unrelatedShort), emptyList(), emptyList(), 8))
    }

    @Test
    fun parsesNestedMusicSampleMetadataAndContinuation() {
        val root = JSONObject("""{
            "contents":[{"musicImmersiveVideoRenderer":{
                "inlinePlaybackCommand":{"inlinePlaybackCommand":{"watchEndpoint":{"watchEndpoint":{
                    "videoId":"abcdefghijk","startTimeSeconds":59,"endTimeSeconds":88
                }}}},
                "overlay":{"elementRenderer":{"trackMetadataOverlayData":{"trackTitle":"Song","artistName":"Drake"}}},
                "thumbnail":{"thumbnails":[{"url":"https://i.ytimg.com/vi/abcdefghijk/frame.jpg"}]}
            }}],
            "continuations":[{"nextContinuationData":{"continuation":"next-page"}}]
        }""")
        val result = parseYoutubeMusicSamples(root).single()

        assertEquals("Song", result.title)
        assertEquals("Drake", result.artist)
        assertEquals(29_000L, result.durationMs)
        assertEquals("next-page", youtubeMusicSamplesContinuation(root))
        val clip = LevyraMediaItemFactory.samplesClipping(result, videoMode = true)!!
        assertEquals(59_000L, clip.startPositionMs)
        assertEquals(88_000L, clip.endPositionMs)
        assertNull(LevyraMediaItemFactory.samplesClipping(result, videoMode = false))
        assertNull(LevyraMediaItemFactory.samplesClipping(result.copy(album = "Album"), videoMode = true))
        assertNull(LevyraMediaItemFactory.samplesClipping(result.copy(videoUrl = "https://www.youtube.com/shorts/abcdefghijk?clipStartMs=88000&clipEndMs=59000"), videoMode = true))
    }

    private fun sample(id: String, artist: String) = Track(
        id = id, title = "Song $id", artist = artist, album = "YouTube Music Samples", durationMs = 30_000L,
        streamUrl = "", videoUrl = "https://www.youtube.com/shorts/$id", thumbnailUrl = "", largeThumbnailUrl = "",
        source = YOUTUBE_SHORTS_SOURCE, moodTags = emptySet(), energy = 50, vocal = 50,
        replayScore = 50, cacheScore = 50, accentStart = 0, accentEnd = 0, videoType = "SHORTS"
    )
}
