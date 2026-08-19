package com.luc4n3x.levyra.desktop.core.localmusic

import com.luc4n3x.levyra.desktop.core.model.Track
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uPlaylistTest {

    @Test
    fun parsesExtendedEntriesAndIgnoresComments() {
        val content = buildString {
            appendLine("#EXTM3U")
            appendLine("#PLAYLIST:Evening")
            appendLine("#EXTINF:214,Kiasmos - Blurred")
            appendLine("music/01.flac")
            appendLine("")
            appendLine("#EXTINF:-1,Untitled")
            appendLine("https://example.org/stream.mp3")
            appendLine("plain/without/extinf.mp3")
        }

        val entries = M3uPlaylist.parse(content)

        assertEquals(3, entries.size)
        assertEquals("Kiasmos", entries[0].artist)
        assertEquals("Blurred", entries[0].title)
        assertEquals(214_000L, entries[0].durationMs)
        assertTrue(entries[1].isRemote)
        assertEquals(0L, entries[1].durationMs)
        assertEquals("", entries[2].title)
    }

    @Test
    fun resolvesRelativeEntriesAgainstThePlaylistDirectory() {
        val base = Path.of("C:/Music/Sets")
        val relative = M3uPlaylist.resolve(M3uEntry(location = "album/01.flac"), base)
        val absolute = M3uPlaylist.resolve(M3uEntry(location = "C:/Music/other.flac"), base)
        val remote = M3uPlaylist.resolve(M3uEntry(location = "https://example.org/a.mp3"), base)

        assertEquals(Path.of("C:/Music/Sets/album/01.flac").normalize(), relative)
        assertEquals(Path.of("C:/Music/other.flac").normalize(), absolute)
        assertNull(remote)
    }

    @Test
    fun keepsWindowsAbsolutePathsAbsoluteOnNonWindowsTestHosts() {
        val base = Path.of("/tmp/playlists")
        val drivePath = M3uPlaylist.resolve(M3uEntry(location = "D:\\Music\\track.flac"), base)
        val uncPath = M3uPlaylist.resolve(M3uEntry(location = "\\\\server\\share\\track.flac"), base)

        assertEquals(Path.of("D:\\Music\\track.flac").normalize(), drivePath)
        assertEquals(Path.of("\\\\server\\share\\track.flac").normalize(), uncPath)
    }

    @Test
    fun rendersLocalPathsAndRemoteUrlsWithExtinfLabels() {
        val rendered = M3uPlaylist.render(
            name = "Evening",
            tracks = listOf(
                Track(
                    id = "local:1",
                    title = "Blurred",
                    artist = "Kiasmos",
                    videoUrl = "",
                    durationMs = 214_000L,
                    offlinePath = "C:/Music/01.flac"
                ),
                Track(
                    id = "yt",
                    title = "Remote",
                    artist = "",
                    videoUrl = "https://www.youtube.com/watch?v=abc",
                    durationMs = 60_000L
                ),
                Track(id = "empty", title = "Nowhere", videoUrl = "")
            )
        )

        val lines = rendered.trim().lines()
        assertEquals("#EXTM3U", lines[0])
        assertEquals("#PLAYLIST:Evening", lines[1])
        assertEquals("#EXTINF:214,Kiasmos - Blurred", lines[2])
        assertEquals("C:/Music/01.flac", lines[3])
        assertEquals("#EXTINF:60,Remote", lines[4])
        assertEquals("https://www.youtube.com/watch?v=abc", lines[5])
        assertEquals(6, lines.size)
    }

    @Test
    fun renderedPlaylistParsesBackIntoTheSameEntries() {
        val rendered = M3uPlaylist.render(
            name = "Evening",
            tracks = listOf(
                Track(
                    id = "local:1",
                    title = "Blurred",
                    artist = "Kiasmos",
                    videoUrl = "",
                    durationMs = 214_000L,
                    offlinePath = "C:/Music/01.flac"
                )
            )
        )

        val entries = M3uPlaylist.parse(rendered)

        assertEquals(1, entries.size)
        assertEquals("C:/Music/01.flac", entries[0].location)
        assertEquals("Kiasmos", entries[0].artist)
        assertEquals("Blurred", entries[0].title)
    }
}
