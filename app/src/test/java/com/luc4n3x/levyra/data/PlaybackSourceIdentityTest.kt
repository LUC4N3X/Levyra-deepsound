package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PlaybackSourceIdentityTest {
    @Test
    fun canonicalKeyIgnoresResolvedStreamState() {
        val original = track(
            id = "catalog-recording-1",
            title = "Song Title (Official Audio)",
            artist = "Artist Name",
            videoUrl = "https://www.youtube.com/watch?v=abcdefghijk"
        )
        val resolved = original.copy(
            streamUrl = "https://r1.googlevideo.com/videoplayback?expire=9999999999",
            videoStreamUrl = "https://r1.googlevideo.com/videoplayback?expire=9999999999&video=1",
            videoUrl = "https://www.youtube.com/watch?v=ZYXWVUTSRQP",
            source = "fallback"
        )

        assertEquals(PlaybackSourceIdentity.canonicalKey(original), PlaybackSourceIdentity.canonicalKey(resolved))
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

    private fun track(
        id: String = "track-id",
        title: String = "Song Title",
        artist: String = "Artist Name",
        videoUrl: String = "",
        durationMs: Long = 180_000L,
        explicit: Boolean = false
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
        explicit = explicit
    )
}
