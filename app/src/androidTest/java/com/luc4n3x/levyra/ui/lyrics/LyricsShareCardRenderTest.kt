package com.luc4n3x.levyra.ui.lyrics

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LyricsShareCardRenderTest {
    @Test
    fun rendererProducesExactSquareAndStoryBitmapsWithUnicodeArtwork() {
        val cover = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(30, 150, 210))
        }
        val track = Track(
            id = "lyrics-card",
            title = "ليلة Levyra",
            artist = "Test Artist",
            album = "",
            durationMs = 0L,
            streamUrl = "",
            videoUrl = "",
            thumbnailUrl = "",
            largeThumbnailUrl = "",
            source = "test",
            moodTags = emptySet(),
            energy = 0,
            vocal = 0,
            replayScore = 0,
            cacheScore = 0,
            accentStart = Color.rgb(20, 120, 180),
            accentEnd = Color.rgb(80, 40, 160)
        )

        val square = LyricsShareCard.render(track, "مرحبا بالعالم 🎵", cover, LyricsShareFormat.SQUARE)
        val story = LyricsShareCard.render(track, "שלום עולם\nمرحبا بالعالم", cover, LyricsShareFormat.STORY)

        assertEquals(1080, square.width)
        assertEquals(1080, square.height)
        assertEquals(1080, story.width)
        assertEquals(1920, story.height)

        square.recycle()
        story.recycle()
        cover.recycle()
    }
}
