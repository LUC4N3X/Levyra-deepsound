package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.PlaybackDeliveryMethod
import com.luc4n3x.levyra.domain.PlaybackStreamDescriptor
import com.luc4n3x.levyra.domain.PlaybackStreamKind
import com.luc4n3x.levyra.domain.ResolvedPlaybackManifest
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun pairedUgcVideoIsNotAcceptedAsOfficial() {
        val sourceId = "audio123456"
        val ugc = watchTrack(
            videoId = "video123456",
            title = "Song",
            videoType = "MUSIC_VIDEO_TYPE_UGC"
        )
        val audio = watchTrack(
            videoId = sourceId,
            title = "Song",
            videoType = "MUSIC_VIDEO_TYPE_ATV",
            counterpart = ugc
        )

        assertNull(selectYoutubeMusicOfficialCounterpart(sourceId, listOf(audio)))
    }

    @Test
    fun malformedOmvCounterpartIsRejected() {
        val sourceId = "audio123456"
        val malformed = watchTrack(
            videoId = "short",
            title = "Song",
            videoType = "MUSIC_VIDEO_TYPE_OMV"
        )
        val audio = watchTrack(
            videoId = sourceId,
            title = "Song",
            videoType = "MUSIC_VIDEO_TYPE_ATV",
            counterpart = malformed
        )

        assertNull(selectYoutubeMusicOfficialCounterpart(sourceId, listOf(audio)))
    }

    @Test
    fun originalTrackIdWinsOverStaleVideoSearchSelection() {
        val track = track(
            id = "audio123456",
            videoUrl = "https://www.youtube.com/watch?v=wrong123456"
        )

        assertEquals("audio123456", youtubeMusicAudioSourceId(track))
    }

    @Test
    fun explicitAudioVideoIdWinsOverTrackAndVideoSelection() {
        val track = track(
            id = "other123456",
            videoUrl = "https://www.youtube.com/watch?v=wrong123456",
            audioVideoId = "audio123456"
        )

        assertEquals("audio123456", youtubeMusicAudioSourceId(track))
    }

    @Test
    fun audioPlaybackSeedDropsVideoRuntimePayload() {
        val dirtyVideo = track(
            id = "audio123456",
            videoUrl = "https://www.youtube.com/watch?v=video123456",
            audioVideoId = "audio123456"
        ).copy(
            streamUrl = "https://rr.example/audio",
            videoStreamUrl = "https://rr.example/video",
            playbackManifest = videoManifest(),
            counterpartVideoId = "video123456",
            videoType = "MUSIC_VIDEO_TYPE_OMV"
        )

        val audio = youtubeMusicAudioPlaybackSeed(dirtyVideo)!!

        assertEquals("", audio.streamUrl)
        assertEquals("", audio.videoStreamUrl)
        assertNull(audio.playbackManifest)
        assertEquals("audio123456", audio.audioVideoId)
        assertEquals("https://www.youtube.com/watch?v=audio123456", audio.videoUrl)
        assertEquals("", audio.counterpartVideoId)
        assertEquals("MUSIC_VIDEO_TYPE_ATV", audio.videoType)
    }

    @Test
    fun pairingRequestUsesOnlyTheOriginalYoutubeMusicVideoEndpoint() {
        val payload = buildYoutubeMusicPairingPayload(
            sourceVideoId = "audio123456",
            hl = "it",
            gl = "IT"
        )

        assertEquals("audio123456", payload.getString("videoId"))
        assertFalse(payload.has("playlistId"))
        assertFalse(payload.has("isAudioOnly"))
        assertFalse(payload.has("watchEndpointMusicSupportedConfigs"))
        assertFalse(payload.has("enablePersistentPlaylistPanel"))
        assertEquals(
            "WEB_REMIX",
            payload.getJSONObject("context")
                .getJSONObject("client")
                .getString("clientName")
        )
    }

    private fun videoManifest() = ResolvedPlaybackManifest(
        sourceVideoId = "video123456",
        provider = "test",
        resolvedAtMs = 1L,
        expiresAtMs = Long.MAX_VALUE,
        durationMs = 180_000L,
        selectedAudioUrl = "https://rr.example/audio",
        selectedVideoUrl = "https://rr.example/video",
        streams = listOf(
            PlaybackStreamDescriptor(
                url = "https://rr.example/audio",
                kind = PlaybackStreamKind.AUDIO,
                deliveryMethod = PlaybackDeliveryMethod.PROGRESSIVE,
                selected = true
            ),
            PlaybackStreamDescriptor(
                url = "https://rr.example/video",
                kind = PlaybackStreamKind.VIDEO,
                deliveryMethod = PlaybackDeliveryMethod.PROGRESSIVE,
                selected = true
            )
        )
    )

    private fun track(
        id: String,
        videoUrl: String,
        audioVideoId: String = ""
    ) = Track(
        id = id,
        title = "Title",
        artist = "Artist",
        album = "Album",
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = videoUrl,
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "YouTube Music",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0,
        audioVideoId = audioVideoId
    )

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
