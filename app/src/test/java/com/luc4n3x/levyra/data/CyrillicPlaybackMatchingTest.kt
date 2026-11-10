package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.data.playlistimport.withEquivalentPlaybackSource
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.viewmodel.isPlaybackCandidateCompatible
import com.luc4n3x.levyra.viewmodel.playbackCandidateScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CyrillicPlaybackMatchingTest {
    private fun track(
        id: String,
        title: String,
        artist: String,
        durationMs: Long = 0L,
        source: String = "YouTube Music",
        videoType: String = "MUSIC_VIDEO_TYPE_ATV"
    ) = Track(
        id = id, title = title, artist = artist, album = "", durationMs = durationMs, streamUrl = "",
        videoUrl = "https://www.youtube.com/watch?v=$id", thumbnailUrl = "", largeThumbnailUrl = "", source = source,
        moodTags = emptySet(), energy = 0, vocal = 0, replayScore = 0, cacheScore = 0, accentStart = 0, accentEnd = 0,
        videoType = videoType
    )

    @Test
    fun ukrainianAndRussianTitlesAreComparableInsteadOfErasedToBlankKeys() {
        val ukrainian = track("ua000000001", "Обійми", "Океан Ельзи", 231_000)
        val russian = track("ru000000001", "Группа крови", "Кино", 285_000)
        assertTrue(isPlaybackCandidateCompatible(ukrainian, track("ua000000002", "Обійми", "Океан Ельзи", 232_000)))
        assertTrue(isPlaybackCandidateCompatible(russian, track("ru000000002", "Группа крови (Remastered)", "Кино")))
        assertFalse(isPlaybackCandidateCompatible(ukrainian, track("ua000000003", "Без бою", "Океан Ельзи")))
        assertFalse(isPlaybackCandidateCompatible(ukrainian, track("ua000000004", "Обійми", "Другая Река")))
        assertFalse(isPlaybackCandidateCompatible(russian, track("ru000000003", "Звезда по имени Солнце", "Кино")))
    }

    @Test
    fun transliteratedPlaylistMetadataFindsTheCyrillicRecording() {
        val latin = track("ua000000010", "Obiymy", "Okean Elzy", 231_000)
        val cyrillic = track("ua000000011", "Обійми", "Океан Ельзи", 231_500)
        assertTrue(isPlaybackCandidateCompatible(latin, cyrillic))
        assertTrue(isPlaybackCandidateCompatible(cyrillic, latin))
        assertTrue(isPlaybackCandidateCompatible(track("ru000000010", "Gruppa krovi", "Kino"), track("ru000000011", "Группа крови", "Кино")))
        assertTrue(
            isPlaybackCandidateCompatible(
                track("ua000000012", "Стефанія", "Kalush Orchestra"),
                track("ua000000013", "Stefania", "KALUSH Orchestra")
            )
        )
        assertFalse(isPlaybackCandidateCompatible(latin, track("ua000000014", "Без бою", "Океан Ельзи")))
    }

    @Test
    fun ukrainianSpellingVariantsStillIdentifyTheSameSong() {
        val target = track("ua000000020", "Пам'ять", "Океан Ельзи", 200_000)
        assertTrue(isPlaybackCandidateCompatible(target, track("ua000000021", "Памʼять", "Океан Ельзи", 200_000)))
        assertTrue(isPlaybackCandidateCompatible(target, track("ua000000022", "Пам’ять", "Океан Ельзи", 200_000)))
        val latinI = track("ua000000023", "Обiйми", "Океан Ельзи")
        assertTrue(isPlaybackCandidateCompatible(track("ua000000024", "Обійми", "Океан Ельзи"), latinI))
        assertTrue(isPlaybackCandidateCompatible(track("ua000000025", "Обійми", "Океан Эльзы"), track("ua000000026", "Обійми", "Океан Ельзи")))
    }

    @Test
    fun studioRecordingOutranksCyrillicCoverKaraokeAndLiveUploads() {
        val target = track("ua000000030", "Обійми", "Океан Ельзи", 231_000)
        val studio = track("ua000000031", "Обійми", "Океан Ельзи", 231_000)
        val translit = track("ua000000032", "Obiymy", "Okean Elzy", 231_000)
        val variants = listOf(
            track("ua000000033", "Обійми (караоке)", "Океан Ельзи", 231_000),
            track("ua000000034", "Обійми (кавер)", "Океан Ельзи", 231_000),
            track("ua000000035", "Обійми (наживо)", "Океан Ельзи", 231_000)
        )
        val studioScore = playbackCandidateScore(target, studio)
        assertTrue(studioScore > playbackCandidateScore(target, translit))
        variants.forEach { variant -> assertTrue(variant.title, studioScore > playbackCandidateScore(target, variant)) }
        assertTrue(playbackCandidateScore(target, translit) > playbackCandidateScore(target, variants.first()))
    }

    @Test
    fun resolverFallbackAcceptsTimedTransliterationsAndRejectsOtherRecordings() {
        val original = track("dead0000001", "Обійми", "Океан Ельзи", 231_000, source = "Playlist")
        val candidates = listOf(
            track("ua000000040", "Obiymy", "Okean Elzy", 231_800),
            track("ua000000041", "Обійми (Live)", "Океан Ельзи", 260_000),
            track("ua000000042", "Обійми (Кавер)", "Океан Ельзи", 231_000),
            track("ua000000043", "Обійми", "Другая Река", 231_000),
            track("ua000000044", "Обійми", "Океан Ельзи", 300_000),
            track("ua000000045", "Памʼять", "Океан Ельзи", 231_000)
        )
        assertEquals(listOf("ua000000040"), trustedPlaybackFallbackCandidates(original, candidates).map { it.id })
        val untimed = original.copy(durationMs = 0L)
        assertTrue(trustedPlaybackFallbackCandidates(untimed, candidates.take(1)).isEmpty())
        val apostrophe = original.copy(title = "Пам'ять")
        assertEquals(listOf("ua000000045"), trustedPlaybackFallbackCandidates(apostrophe, candidates).map { it.id })
    }

    @Test
    fun equivalentReplacementKeepsPlaylistIdentityAndMetadata() {
        val original = track("broken00001", "Обійми", "Океан Ельзи", 231_000).copy(
            album = "Без меж",
            thumbnailUrl = "https://example.com/original.jpg",
            playlistEntryId = "entry-1"
        )
        val replacement = track("working0001", "Obiymy", "Okean Elzy", 231_400).copy(thumbnailUrl = "https://example.com/other.jpg")
        val kept = requireNotNull(original.withEquivalentPlaybackSource(replacement))
        assertEquals("working0001", kept.id)
        assertEquals("Обійми", kept.title)
        assertEquals("Океан Ельзи", kept.artist)
        assertEquals("Без меж", kept.album)
        assertEquals("https://example.com/original.jpg", kept.thumbnailUrl)
        assertEquals("entry-1", kept.playlistEntryId)
        assertEquals(231_400L, kept.durationMs)
        assertEquals("working0001", PlaybackSourceIdentity.sourceVideoId(kept))
        assertNull(original.withEquivalentPlaybackSource(track("other000001", "Без бою", "Океан Ельзи", 231_000)))
    }
}
