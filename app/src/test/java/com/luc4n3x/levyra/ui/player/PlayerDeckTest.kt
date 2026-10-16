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
        assertEquals(PlayerVisualMode.CanvasCard, PlayerDeckOrder.first())
    }

    @Test
    fun `existing modes keep the levyra layout and new styles get their own`() {
        assertEquals(PlayerDeckLayout.Poster, PlayerVisualMode.Artwork.deckLayout())
        assertEquals(PlayerDeckLayout.Levyra, PlayerVisualMode.CanvasCard.deckLayout())
        assertEquals(PlayerDeckLayout.Aura, PlayerVisualMode.CanvasImmersive.deckLayout())
        assertEquals(PlayerDeckLayout.Expressive, PlayerVisualMode.Expressive.deckLayout())
        assertEquals(PlayerDeckLayout.Pulse, PlayerVisualMode.Pulse.deckLayout())
    }

    @Test
    fun `lyrics flip is offered only by decks that render an upright lyrics face`() {
        assertFalse(PlayerDeckLayout.Aura.showsLyricsFace())
        assertFalse(PlayerDeckLayout.Pulse.showsLyricsFace())
        PlayerDeckLayout.entries
            .filterNot { it == PlayerDeckLayout.Aura || it == PlayerDeckLayout.Pulse }
            .forEach { assertTrue(it.showsLyricsFace()) }
    }

    @Test
    fun `new styles fall back to the levyra layout where they are not designed to run`() {
        listOf(PlayerVisualMode.CanvasImmersive, PlayerVisualMode.Expressive, PlayerVisualMode.Artwork, PlayerVisualMode.Pulse).forEach { mode ->
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
            resolvePlayerDeckVisualMode(PlayerVisualMode.Expressive, PlayerDeckLayout.Levyra)
        )
        assertEquals(
            PlayerVisualMode.CanvasCard,
            resolvePlayerDeckVisualMode(PlayerVisualMode.Pulse, PlayerDeckLayout.Levyra)
        )
        assertEquals(
            PlayerVisualMode.Artwork,
            resolvePlayerDeckVisualMode(PlayerVisualMode.Artwork, PlayerDeckLayout.Levyra)
        )
        PlayerDeckOrder.forEach { mode ->
            assertEquals(mode, resolvePlayerDeckVisualMode(mode, mode.deckLayout()))
        }
    }

    @Test
    fun `levyra layouts resolve identically regardless of playback context`() {
        listOf(PlayerVisualMode.CanvasCard).forEach { mode ->
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
    fun `expressive type scales down for long titles and compact screens`() {
        val short = expressiveTitleSize("Halo", compact = false).value
        val medium = expressiveTitleSize("Midnight City Lights", compact = false).value
        val long = expressiveTitleSize("A Very Long Title That Keeps Going Forever And Ever", compact = false).value
        assertTrue(short > medium)
        assertTrue(medium > long)
        assertTrue(expressiveTitleSize("Halo", compact = true).value < short)
    }

    @Test
    fun `stored editorial choice migrates to the expressive deck`() {
        assertEquals(PlayerVisualMode.Expressive, PlayerVisualMode.from("Editorial"))
        assertEquals(PlayerVisualMode.Expressive, PlayerVisualMode.from("Expressive"))
    }

    @Test
    fun `audio spec labels describe the selected format compactly`() {
        assertEquals("OPUS", playerAudioCodecLabel("audio/opus", null))
        assertEquals("AAC", playerAudioCodecLabel("audio/mp4a-latm", "mp4a.40.2"))
        assertEquals("AAC", playerAudioCodecLabel(null, "mp4a.40.5"))
        assertEquals("MP3", playerAudioCodecLabel("audio/mpeg", null))
        assertEquals("MP2", playerAudioCodecLabel("audio/mpeg-l2", null))
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
                assertTrue("${strings.code} $mode short label", playerDeckLabel(mode, strings).isNotBlank())
                assertTrue("${strings.code} $mode short label length", playerDeckLabel(mode, strings).length <= 16)
                assertTrue("${strings.code} $mode hint", playerDeckHint(mode, strings).isNotBlank())
            }
            assertTrue(strings.playerDeck.isNotBlank())
            assertTrue(strings.playerDeckSubtitle.isNotBlank())
            assertTrue(strings.playerDeckLandscapeNote.isNotBlank())
        }
        val italian = LevyraStrings.forCode("it")
        assertEquals("Bloom", visualModeStateDescription(PlayerVisualMode.Expressive, italian))
        assertEquals("Late Night", visualModeStateDescription(PlayerVisualMode.Pulse, italian))
        assertEquals("Velvet", playerDeckLabel(PlayerVisualMode.CanvasImmersive, italian))
        assertEquals("Studio", playerDeckLabel(PlayerVisualMode.CanvasCard, italian))
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
