package com.luc4n3x.levyra.ui.player

import com.luc4n3x.levyra.data.backupInterfaceSettingsFromJson
import com.luc4n3x.levyra.data.backupInterfaceSettingsToJson
import com.luc4n3x.levyra.domain.LevyraInterfaceSettings
import com.luc4n3x.levyra.domain.PlayerVisualMode
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.LevyraPlayerPane
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.viewmodel.shouldRefreshMotionArtworkOwnership
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerDeckTest {

    @Test
    fun `deck order lists every visual mode exactly once`() {
        assertEquals(PlayerVisualMode.entries.toSet(), PlayerDeckOrder.toSet())
        assertEquals(PlayerVisualMode.entries.size, PlayerDeckOrder.size)
        assertEquals(PlayerVisualMode.CanvasImmersive, PlayerDeckOrder.first())
    }

    @Test
    fun `existing modes keep the levyra layout and new styles get their own`() {
        assertEquals(PlayerDeckLayout.Levyra, PlayerVisualMode.Artwork.deckLayout())
        assertEquals(PlayerDeckLayout.Levyra, PlayerVisualMode.CanvasCard.deckLayout())
        assertEquals(PlayerDeckLayout.Levyra, PlayerVisualMode.CanvasImmersive.deckLayout())
        assertEquals(PlayerDeckLayout.Editorial, PlayerVisualMode.Editorial.deckLayout())
        assertEquals(PlayerDeckLayout.Pulse, PlayerVisualMode.Pulse.deckLayout())
        assertTrue(PlayerVisualMode.CanvasImmersive.showsCinematicStage())
        PlayerVisualMode.entries.filter { it != PlayerVisualMode.CanvasImmersive }.forEach {
            assertTrue("$it must not draw the cinematic stage", !it.showsCinematicStage())
        }
    }

    @Test
    fun `new styles fall back to the levyra layout where they are not designed to run`() {
        listOf(PlayerVisualMode.Editorial, PlayerVisualMode.Pulse).forEach { mode ->
            val supported = resolvePlayerDeckLayout(mode, false, false, LevyraPlayerPane.Stacked, true)
            assertEquals(mode.deckLayout(), supported)
            assertEquals(PlayerDeckLayout.Levyra, resolvePlayerDeckLayout(mode, true, false, LevyraPlayerPane.Stacked, true))
            assertEquals(PlayerDeckLayout.Levyra, resolvePlayerDeckLayout(mode, false, true, LevyraPlayerPane.Stacked, true))
            assertEquals(PlayerDeckLayout.Levyra, resolvePlayerDeckLayout(mode, false, false, LevyraPlayerPane.SideBySide, true))
            assertEquals(PlayerDeckLayout.Levyra, resolvePlayerDeckLayout(mode, false, false, LevyraPlayerPane.Stacked, false))
        }
    }

    @Test
    fun `fallback presentation never lands on the immersive exit affordance`() {
        assertEquals(
            PlayerVisualMode.CanvasCard,
            resolvePlayerDeckVisualMode(PlayerVisualMode.Editorial, PlayerDeckLayout.Levyra)
        )
        assertEquals(
            PlayerVisualMode.CanvasCard,
            resolvePlayerDeckVisualMode(PlayerVisualMode.Pulse, PlayerDeckLayout.Levyra)
        )
        PlayerDeckOrder.forEach { mode ->
            assertEquals(mode, resolvePlayerDeckVisualMode(mode, mode.deckLayout()))
        }
    }

    @Test
    fun `levyra layouts resolve identically regardless of playback context`() {
        listOf(PlayerVisualMode.Artwork, PlayerVisualMode.CanvasCard, PlayerVisualMode.CanvasImmersive).forEach { mode ->
            listOf(true, false).forEach { video ->
                listOf(LevyraPlayerPane.Stacked, LevyraPlayerPane.SideBySide).forEach { pane ->
                    assertEquals(PlayerDeckLayout.Levyra, resolvePlayerDeckLayout(mode, video, !video, pane, true))
                }
            }
        }
    }

    @Test
    fun `deck choice survives persistence and invalid values fall back safely`() {
        PlayerVisualMode.entries.forEach { mode ->
            assertEquals(mode, PlayerVisualMode.from(mode.name))
            assertEquals(mode, PlayerVisualMode.from(mode.name.lowercase()))
            val restored = backupInterfaceSettingsFromJson(
                backupInterfaceSettingsToJson(LevyraInterfaceSettings(playerVisualMode = mode))
            )
            assertEquals(mode, restored.playerVisualMode)
        }
        assertEquals(PlayerVisualMode.Artwork, PlayerVisualMode.from("hologram"))
        assertEquals(PlayerVisualMode.Artwork, PlayerVisualMode.from(""))
    }

    @Test
    fun `switching style never restarts motion artwork or touches other settings`() {
        val playing = LevyraInterfaceSettings(playerVisualMode = PlayerVisualMode.CanvasImmersive, doubleTapSeekSeconds = 15)
        PlayerDeckOrder.forEach { from ->
            PlayerDeckOrder.forEach { to ->
                val previous = playing.copy(playerVisualMode = from)
                val next = previous.copy(playerVisualMode = to)
                assertFalse(shouldRefreshMotionArtworkOwnership(previous, next))
                assertEquals(previous, next.copy(playerVisualMode = from))
            }
        }
    }

    @Test
    fun `queue position only appears for real queues`() {
        val queue = listOf(track("a"), track("b"), track("c"))
        assertNull(playerDeckQueuePosition(emptyList(), "a"))
        assertNull(playerDeckQueuePosition(listOf(track("a")), "a"))
        assertNull(playerDeckQueuePosition(queue, "z"))
        assertNull(playerDeckQueuePosition(queue, ""))
        val position = playerDeckQueuePosition(queue, "b")
        assertEquals(PlayerDeckQueuePosition(1, 3), position)
        assertEquals("02", position?.indexLabel)
        assertEquals("03", position?.totalLabel)
    }

    @Test
    fun `editorial type scales down for long titles and compact screens`() {
        val short = editorialTitleSize("Halo", compact = false).value
        val medium = editorialTitleSize("Midnight City Lights", compact = false).value
        val long = editorialTitleSize("A Very Long Title That Keeps Going Forever And Ever", compact = false).value
        assertTrue(short > medium)
        assertTrue(medium > long)
        assertTrue(editorialTitleSize("Halo", compact = true).value < short)
    }

    @Test
    fun `editorial deckline joins album and year without blanks`() {
        assertEquals("Album  ·  2024", editorialDeckline(track("a", album = "Album", year = "2024")))
        assertEquals("Album  ·  1999", editorialDeckline(track("a", album = "Album", releaseDate = "1999-02-01")))
        assertEquals("", editorialDeckline(track("a")))
    }

    @Test
    fun `audio spec labels describe the selected format compactly`() {
        assertEquals("OPUS", playerAudioCodecLabel("audio/opus", null))
        assertEquals("AAC", playerAudioCodecLabel("audio/mp4a-latm", "mp4a.40.2"))
        assertEquals("AAC", playerAudioCodecLabel(null, "mp4a.40.5"))
        assertEquals("FLAC", playerAudioCodecLabel("audio/flac", null))
        assertEquals("", playerAudioCodecLabel(null, null))
        assertEquals(
            listOf("OPUS", "160 kbps", "48 kHz", "2.0"),
            playerAudioSpecLabels(PlayerAudioSpec("OPUS", 160, 48_000, 2))
        )
        assertEquals(listOf("AAC", "44.1 kHz", "5.1"), playerAudioSpecLabels(PlayerAudioSpec("AAC", null, 44_100, 6)))
        assertTrue(playerAudioSpecLabels(PlayerAudioSpec()).isEmpty())
        assertTrue(PlayerAudioSpec().isEmpty)
    }

    @Test
    fun `every language names and explains every deck style`() {
        LevyraStrings.all().forEach { strings ->
            PlayerDeckOrder.forEach { mode ->
                assertTrue("${strings.code} $mode label", visualModeStateDescription(mode, strings).isNotBlank())
                assertTrue("${strings.code} $mode hint", playerDeckHint(mode, strings).isNotBlank())
            }
            assertTrue(strings.playerDeck.isNotBlank())
            assertTrue(strings.playerDeckSubtitle.isNotBlank())
            assertTrue(strings.playerDeckLandscapeNote.isNotBlank())
        }
        val italian = LevyraStrings.forCode("it")
        assertEquals("Editoriale", visualModeStateDescription(PlayerVisualMode.Editorial, italian))
        assertEquals("Pulse", visualModeStateDescription(PlayerVisualMode.Pulse, italian))
    }

    private fun track(
        id: String,
        album: String = "",
        year: String = "",
        releaseDate: String = ""
    ) = Track(
        id = id,
        title = "Title $id",
        artist = "Artist",
        album = album,
        durationMs = 1_000L,
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
        accentStart = 0,
        accentEnd = 0,
        year = year,
        releaseDate = releaseDate
    )
}
