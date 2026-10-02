package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class MusicVideoTitleTest {

    @Test
    fun `plain song titles are left untouched`() {
        assertEquals(MusicVideoRecording("Её парень", "Shluzov"), MusicVideoTitle.recording("Её парень", "Shluzov"))
        assertEquals(
            MusicVideoRecording("Blinding Lights", "The Weeknd"),
            MusicVideoTitle.recording("Blinding Lights", "The Weeknd")
        )
    }

    @Test
    fun `version suffix after a dash stays part of the title`() {
        assertEquals(
            MusicVideoRecording("Bohemian Rhapsody - Remastered 2011", "Queen"),
            MusicVideoTitle.recording("Bohemian Rhapsody - Remastered 2011", "Queen")
        )
        assertEquals(
            MusicVideoRecording("Blinding Lights - Live at the Kia Forum", "The Weeknd"),
            MusicVideoTitle.recording("Blinding Lights - Live at the Kia Forum", "The Weeknd")
        )
    }

    @Test
    fun `music video title is reduced to the credited artists and song`() {
        assertEquals(
            MusicVideoRecording("ЗНАК", "Toxi$, Дора"),
            MusicVideoTitle.recording("Toxi$, Дора – ЗНАК | Official Audio | 2026", "Toxi$")
        )
        assertEquals(
            MusicVideoRecording("Dark Paradise", "NovaFeel"),
            MusicVideoTitle.recording("NovaFeel - Dark Paradise", "NovaFeel")
        )
        assertEquals(
            MusicVideoRecording("one way ticket", "ONDA ANDAR"),
            MusicVideoTitle.recording("ONDA ANDAR -- one way ticket", "ONDA ANDAR")
        )
        assertEquals(
            MusicVideoRecording("Peshta", "Xamdam Sobirov"),
            MusicVideoTitle.recording("Xamdam Sobirov - Peshta / Хамдам Собиров - Пешта / Video Klip", "Xamdam Sobirov")
        )
    }

    @Test
    fun `label channel upload takes the artists from the video title`() {
        assertEquals(
            MusicVideoRecording("Базовый минимум", "Sabi, MIA BOYKA"),
            MusicVideoTitle.recording("Sabi, MIA BOYKA - Базовый минимум (Премьера клипа)", "Sputnik Records")
        )
    }

    @Test
    fun `premiere and video notes are dropped while featured credits remain`() {
        assertEquals(
            MusicVideoRecording("Я ТЕБЯ МОГНУ (feat. LIZOGUB, Давид Туров)", "ЦУЕФА"),
            MusicVideoTitle.recording("ЦУЕФА - Я ТЕБЯ МОГНУ (ПРЕМЬЕРА) (feat. LIZOGUB, Давид Туров)", "ЦУЕФА")
        )
        assertEquals(
            MusicVideoRecording("Бременские Музыканты", "MORGENSHTERN"),
            MusicVideoTitle.recording(
                "MORGENSHTERN - Бременские Музыканты (неофициальное видео, помогите получить права)",
                "MORGENSHTERN"
            )
        )
    }
}

class ChartRecordingTest {

    @Test
    fun `chart music video shows the song and its artists instead of the raw upload title`() {
        val upload = chartTrack(
            title = "Sabi, MIA BOYKA - Базовый минимум (Премьера клипа)",
            artist = "Sputnik Records",
            videoType = "MUSIC_VIDEO_TYPE_OMV",
            browseIds = listOf("UCsputnik")
        )

        val shown = chartRecordingOf(upload)

        assertEquals("Базовый минимум", shown.title)
        assertEquals("Sabi, MIA BOYKA", shown.artist)
        assertEquals(emptyList<String>(), shown.artistBrowseIds)
        assertEquals(upload.id, shown.id)
    }

    @Test
    fun `chart video by the credited artist keeps the artist channel`() {
        val upload = chartTrack(
            title = "NovaFeel - Dark Paradise",
            artist = "NovaFeel",
            videoType = "MUSIC_VIDEO_TYPE_UGC",
            browseIds = listOf("UCnovafeel")
        )

        val shown = chartRecordingOf(upload)

        assertEquals("Dark Paradise", shown.title)
        assertEquals(listOf("UCnovafeel"), shown.artistBrowseIds)
    }

    @Test
    fun `official audio tracks and untyped rows are never rewritten`() {
        val audio = chartTrack(title = "Pt. 1 - Prologue", artist = "Band", videoType = "MUSIC_VIDEO_TYPE_ATV")
        val untyped = chartTrack(title = "Pt. 1 - Prologue", artist = "Band", videoType = "")

        assertEquals(audio, chartRecordingOf(audio))
        assertEquals(untyped, chartRecordingOf(untyped))
    }

    private fun chartTrack(title: String, artist: String, videoType: String, browseIds: List<String> = emptyList()) =
        Track(
            id = "video-1",
            title = title,
            artist = artist,
            album = "",
            durationMs = 180_000L,
            streamUrl = "",
            videoUrl = "https://www.youtube.com/watch?v=video-1",
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
            videoType = videoType,
            artistBrowseIds = browseIds
        )
}
