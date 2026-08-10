package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YoutubeMusicOfficialVideoResolverTest {
    @Test
    fun daiDaiUsesYoutubeMusicOfficialCounterpart() {
        val sourceId = "audio123456"
        val official = watchTrack(
            videoId = "fcnDmrtj6Sk",
            title = "Dai Dai",
            videoType = "MUSIC_VIDEO_TYPE_OMV"
        )
        val audio = watchTrack(
            videoId = sourceId,
            title = "Dai Dai",
            videoType = "MUSIC_VIDEO_TYPE_ATV",
            counterpart = official
        )

        assertEquals(
            "fcnDmrtj6Sk",
            selectYoutubeMusicOfficialCounterpart(sourceId, listOf(audio))?.videoId
        )
    }

    @Test
    fun daDioUsesYoutubeMusicOfficialCounterpart() {
        val sourceId = "audio654321"
        val official = watchTrack(
            videoId = "-ZwDJaZ2coY",
            title = "Da Dio",
            videoType = "MUSIC_VIDEO_TYPE_OMV"
        )
        val audio = watchTrack(
            videoId = sourceId,
            title = "Da Dio",
            videoType = "MUSIC_VIDEO_TYPE_ATV",
            counterpart = official
        )

        assertEquals(
            "-ZwDJaZ2coY",
            selectYoutubeMusicOfficialCounterpart(sourceId, listOf(audio))?.videoId
        )
    }

    @Test
    fun sourceCanAppearAsCounterpartOfOfficialPrimary() {
        val sourceId = "audio123456"
        val audio = watchTrack(
            videoId = sourceId,
            title = "Song",
            videoType = "MUSIC_VIDEO_TYPE_ATV"
        )
        val official = watchTrack(
            videoId = "video123456",
            title = "Song",
            videoType = "MUSIC_VIDEO_TYPE_OMV",
            counterpart = audio
        )

        assertEquals(
            "video123456",
            selectYoutubeMusicOfficialCounterpart(sourceId, listOf(official))?.videoId
        )
    }

    @Test
    fun unrelatedOfficialVideoIsNotUsedAsCounterpart() {
        val sourceId = "audio123456"
        val unrelated = watchTrack(
            videoId = "other123456",
            title = "Other",
            videoType = "MUSIC_VIDEO_TYPE_OMV"
        )

        assertNull(selectYoutubeMusicOfficialCounterpart(sourceId, listOf(unrelated)))
    }

    private fun watchTrack(
        videoId: String,
        title: String,
        videoType: String,
        counterpart: YoutubeMusicWatchTrack? = null
    ) = YoutubeMusicWatchTrack(
        videoId = videoId,
        title = title,
        artists = listOf(YoutubeMusicWatchArtist("Artist", "UC_ARTIST")),
        albumTitle = "Album",
        albumBrowseId = "MPRE_ALBUM",
        durationMs = 180_000L,
        thumbnailUrl = "https://example.com/thumb.jpg",
        videoType = videoType,
        explicit = false,
        counterpart = counterpart
    )
}
