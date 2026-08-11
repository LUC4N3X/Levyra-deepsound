package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSourceIdentityTest {
    @Test
    fun canonicalKeyIgnoresTransientResolvedStreamStateWhenSourceIsUnchanged() {
        val original = track(
            id = "catalog-recording-1",
            title = "Song Title (Official Audio)",
            artist = "Artist Name",
            videoUrl = "https://www.youtube.com/watch?v=abcdefghijk"
        )
        val resolved = original.copy(
            streamUrl = "https://r1.googlevideo.com/videoplayback?expire=9999999999",
            videoStreamUrl = "https://r1.googlevideo.com/videoplayback?expire=9999999999&video=1",
            source = "fallback"
        )

        assertEquals(PlaybackSourceIdentity.canonicalKey(original), PlaybackSourceIdentity.canonicalKey(resolved))
    }

    @Test
    fun canonicalKeySeparatesArtTrackAndOfficialVideoWithAndWithoutIsrc() {
        val audio = track(
            id = "lFQdcPTTzSg",
            title = "Dai Dai",
            artist = "Shakira, Burna Boy",
            videoUrl = "https://www.youtube.com/watch?v=lFQdcPTTzSg"
        )
        val officialVideo = audio.copy(
            videoUrl = "https://www.youtube.com/watch?v=fcnDmrtj6Sk",
            counterpartVideoId = "fcnDmrtj6Sk",
            audioVideoId = "lFQdcPTTzSg"
        )

        assertNotEquals(
            PlaybackSourceIdentity.canonicalKey(audio),
            PlaybackSourceIdentity.canonicalKey(officialVideo)
        )
        assertNotEquals(
            PlaybackSourceIdentity.matchKey(audio, videoMode = true, audioQuality = "High"),
            PlaybackSourceIdentity.matchKey(officialVideo, videoMode = true, audioQuality = "High")
        )

        val audioWithIsrc = audio.copy(isrc = "USQX92601234")
        val officialVideoWithIsrc = officialVideo.copy(isrc = "USQX92601234")
        assertNotEquals(
            PlaybackSourceIdentity.canonicalKey(audioWithIsrc),
            PlaybackSourceIdentity.canonicalKey(officialVideoWithIsrc)
        )
        assertTrue(PlaybackSourceIdentity.canonicalKey(officialVideoWithIsrc).contains("youtube-video-v2"))
    }

    @Test
    fun youtubeIdsRemainCaseSensitiveInCanonicalAndMatchKeys() {
        val upper = track(
            id = "recording-1",
            videoUrl = "https://www.youtube.com/watch?v=AbCdEfGhIjK"
        )
        val lower = upper.copy(
            videoUrl = "https://www.youtube.com/watch?v=abCdEfGhIjK"
        )

        assertNotEquals(
            PlaybackSourceIdentity.canonicalKey(upper),
            PlaybackSourceIdentity.canonicalKey(lower)
        )
        assertNotEquals(
            PlaybackSourceIdentity.matchKey(upper, videoMode = true, audioQuality = "High"),
            PlaybackSourceIdentity.matchKey(lower, videoMode = true, audioQuality = "High")
        )
    }

    @Test
    fun videoPersistentMatchUsesStrictPairingNamespace() {
        val key = PlaybackSourceIdentity.matchKey(track(), videoMode = true, audioQuality = "High")
        assertTrue(key.contains("|video-v2|high"))
    }

    @Test
    fun canonicalKeySeparatesDifferentDurations() {
        val short = track(durationMs = 180_000L)
        val long = track(durationMs = 240_000L)

        assertNotEquals(PlaybackSourceIdentity.canonicalKey(short), PlaybackSourceIdentity.canonicalKey(long))
    }

    @Test
    fun canonicalKeySeparatesExplicitAndCleanRecordings() {
        val clean = track(id = "recording-1", explicit = false)
        val explicit = track(id = "recording-1", explicit = true)

        assertNotEquals(PlaybackSourceIdentity.canonicalKey(clean), PlaybackSourceIdentity.canonicalKey(explicit))
    }

    @Test
    fun canonicalKeySeparatesDifferentStableRecordingsWithEqualMetadata() {
        val first = track(
            id = "recording-1",
            videoUrl = "https://www.youtube.com/watch?v=abcdefghijk"
        )
        val second = track(
            id = "recording-2",
            videoUrl = "https://www.youtube.com/watch?v=ZYXWVUTSRQP"
        )

        assertNotEquals(PlaybackSourceIdentity.canonicalKey(first), PlaybackSourceIdentity.canonicalKey(second))
    }

    @Test
    fun offlineMp4MatchesUseASeparatePersistentKey() {
        val track = track()

        val playback = PlaybackSourceIdentity.matchKey(track, videoMode = false, audioQuality = "High")
        val offline = PlaybackSourceIdentity.matchKey(track, videoMode = false, audioQuality = "High", preferMp4Audio = true)

        assertNotEquals(playback, offline)
    }

    @Test
    fun extractsWatchShortLiveAndYoutuBeIds() {
        assertEquals("abcdefghijk", PlaybackSourceIdentity.extractYoutubeVideoId("https://www.youtube.com/watch?v=abcdefghijk"))
        assertEquals("abcdefghijk", PlaybackSourceIdentity.extractYoutubeVideoId("https://youtube.com/shorts/abcdefghijk"))
        assertEquals("abcdefghijk", PlaybackSourceIdentity.extractYoutubeVideoId("https://youtube.com/live/abcdefghijk"))
        assertEquals("abcdefghijk", PlaybackSourceIdentity.extractYoutubeVideoId("https://youtu.be/abcdefghijk"))
    }

    @Test
    fun sourceVideoIdUsesSelectedOfficialUrlWithoutReplacingCanonicalId() {
        val selected = track(
            id = "catalog-recording-1",
            videoUrl = "https://www.youtube.com/watch?v=fcnDmrtj6Sk"
        ).copy(audioVideoId = "lFQdcPTTzSg")

        assertEquals("fcnDmrtj6Sk", PlaybackSourceIdentity.sourceVideoId(selected))
        assertEquals("catalog-recording-1", selected.id)
    }

    @Test
    fun sourceVideoIdUsesAuthoritativeAudioIdentityWhenNoVideoIsSelected() {
        val audio = track(
            id = "catalog-recording-1",
            videoUrl = ""
        ).copy(audioVideoId = "lFQdcPTTzSg")

        assertEquals("lFQdcPTTzSg", PlaybackSourceIdentity.sourceVideoId(audio))
        assertEquals("catalog-recording-1", audio.id)
    }

    private fun track(
        id: String = "track-id",
        title: String = "Song Title",
        artist: String = "Artist Name",
        videoUrl: String = "",
        durationMs: Long = 180_000L,
        explicit: Boolean = false,
        isrc: String = ""
    ) = Track(
        id = id,
        title = title,
        artist = artist,
        album = "Album",
        durationMs = durationMs,
        streamUrl = "",
        videoUrl = videoUrl,
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0,
        explicit = explicit,
        isrc = isrc
    )
}
