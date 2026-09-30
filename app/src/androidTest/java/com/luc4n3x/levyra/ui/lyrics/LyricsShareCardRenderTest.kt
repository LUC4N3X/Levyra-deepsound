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
    fun rendererProducesExactPortraitBitmapsForEveryStyleWithUnicodeLyrics() {
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

        val accents = LyricsShareCard.resolveAccents(track, cover)
        val content = LyricsShareCardContent(
            title = track.title,
            artist = track.artist,
            lyrics = listOf("مرحبا بالعالم 🎵", "שלום עולם")
        )

        LyricsShareCardStyle.entries.forEach { style ->
            val exported = LyricsShareCard.render(content, cover, style, accents, LyricsShareCard.EXPORT_WIDTH_PX)!!
            val preview = LyricsShareCard.render(content, cover, style, accents, LyricsShareCard.PREVIEW_WIDTH_PX)!!
            assertEquals(1080, exported.width)
            assertEquals(1350, exported.height)
            assertEquals(540, preview.width)
            assertEquals(675, preview.height)
            exported.recycle()
            preview.recycle()
        }
        cover.recycle()
    }
}
