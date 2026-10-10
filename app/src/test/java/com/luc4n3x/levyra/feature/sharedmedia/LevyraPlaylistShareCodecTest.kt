package com.luc4n3x.levyra.feature.sharedmedia

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraPlaylistShareCodecTest {

    private fun track(id: String, title: String = "Title $id", artist: String = "Artist $id") =
        LevyraSharedTrack(id = id, title = title, artist = artist)

    private fun decoded(encoded: String): LevyraSharedPlaylist {
        val result = LevyraPlaylistShareCodec.decode(encoded)
        assertTrue("expected success but was $result", result is LevyraPlaylistDecodeResult.Success)
        return (result as LevyraPlaylistDecodeResult.Success).playlist
    }

    private fun failure(encoded: String): LevyraPlaylistDecodeError {
        val result = LevyraPlaylistShareCodec.decode(encoded)
        assertTrue("expected failure but was $result", result is LevyraPlaylistDecodeResult.Failure)
        return (result as LevyraPlaylistDecodeResult.Failure).error
    }

    @Test
    fun `round trip preserves title order and entries`() {
        val tracks = listOf(track("dQw4w9WgXcQ"), track("aBcDeFgHiJk"), track("_1234567890"))
        val encoded = LevyraPlaylistShareCodec.encode("Late night mix", tracks)
        assertNotNull(encoded)
        val playlist = decoded(encoded!!)
        assertEquals(LevyraPlaylistShareCodec.SCHEMA_VERSION, playlist.schemaVersion)
        assertEquals("Late night mix", playlist.title)
        assertEquals(tracks.map { it.id }, playlist.tracks.map { it.id })
        assertEquals(tracks.map { it.title }, playlist.tracks.map { it.title })
        assertEquals(tracks.map { it.artist }, playlist.tracks.map { it.artist })
    }

    @Test
    fun `payload stays compact for a large playlist`() {
        val tracks = (0 until 200).map { track("id%08d".format(it)) }
        val encoded = LevyraPlaylistShareCodec.encode("Big list", tracks)
        assertNotNull(encoded)
        assertTrue(encoded!!.length < LevyraPlaylistShareCodec.MAX_ENCODED_CHARS)
    }

    @Test
    fun `link round trip extracts the payload`() {
        val link = LevyraPlaylistShareCodec.encodeLink("Shared", listOf(track("dQw4w9WgXcQ")))
        assertNotNull(link)
        assertTrue(link!!.startsWith("levyra://playlist?"))
        val payload = LevyraPlaylistShareCodec.extractPayload(link)
        assertNotNull(payload)
        assertEquals("Shared", decoded(payload!!).title)
    }

    @Test
    fun `share sheet text round trip extracts embedded playlist link`() {
        val link = LevyraPlaylistShareCodec.encodeLink("Shared", listOf(track("dQw4w9WgXcQ")))!!
        val payload = LevyraPlaylistShareCodec.extractPayload("Shared playlist\n$link")
        assertNotNull(payload)
        assertEquals("Shared", decoded(payload!!).title)
    }

    @Test
    fun `embedded link tolerates trailing punctuation`() {
        val link = LevyraPlaylistShareCodec.encodeLink("Shared", listOf(track("dQw4w9WgXcQ")))!!
        val payload = LevyraPlaylistShareCodec.extractPayload("Playlist: ($link).")
        assertNotNull(payload)
        assertEquals("Shared", decoded(payload!!).title)
    }

    @Test
    fun `non levyra text yields no payload`() {
        assertNull(LevyraPlaylistShareCodec.extractPayload("https://music.youtube.com/playlist?list=abc"))
        assertNull(LevyraPlaylistShareCodec.extractPayload("levyra://playlist"))
        assertNull(LevyraPlaylistShareCodec.extractPayload(""))
    }

    @Test
    fun `duplicate ids are collapsed while keeping the first entry`() {
        val encoded = LevyraPlaylistShareCodec.encode(
            "Dupes",
            listOf(track("dQw4w9WgXcQ", title = "First"), track("dQw4w9WgXcQ", title = "Second"))
        )
        val playlist = decoded(encoded!!)
        assertEquals(1, playlist.tracks.size)
        assertEquals("First", playlist.tracks.first().title)
    }

    @Test
    fun `invalid ids are dropped`() {
        val encoded = LevyraPlaylistShareCodec.encode(
            "Mixed",
            listOf(track("dQw4w9WgXcQ"), track("bad id!"), track(""), track("x"))
        )
        val playlist = decoded(encoded!!)
        assertEquals(listOf("dQw4w9WgXcQ"), playlist.tracks.map { it.id })
    }

    @Test
    fun `an empty playlist cannot be encoded`() {
        assertNull(LevyraPlaylistShareCodec.encode("Nothing", emptyList()))
        assertNull(LevyraPlaylistShareCodec.encode("Nothing", listOf(track("nope!"))))
    }

    @Test
    fun `track count is capped at the supported maximum`() {
        val tracks = (0 until LevyraPlaylistShareCodec.MAX_TRACKS + 40).map {
            track("t%010d".format(it))
        }
        val playlist = decoded(LevyraPlaylistShareCodec.encode("Huge", tracks)!!)
        assertEquals(LevyraPlaylistShareCodec.MAX_TRACKS, playlist.tracks.size)
    }

    @Test
    fun `title and text fields are bounded`() {
        val playlist = decoded(
            LevyraPlaylistShareCodec.encode(
                "A".repeat(400),
                listOf(track("dQw4w9WgXcQ", title = "B".repeat(400), artist = "C".repeat(400)))
            )!!
        )
        assertEquals(LevyraPlaylistShareCodec.MAX_TITLE_CHARS, playlist.title.length)
        assertTrue(playlist.tracks.first().title.length <= 90)
        assertTrue(playlist.tracks.first().artist.length <= 90)
    }

    @Test
    fun `blank payload is reported as empty`() {
        assertEquals(LevyraPlaylistDecodeError.Empty, failure("   "))
    }

    @Test
    fun `garbage payload is reported as malformed`() {
        assertEquals(LevyraPlaylistDecodeError.Malformed, failure("not-a-real-payload"))
    }

    @Test
    fun `oversized payload is rejected before decompression`() {
        val oversized = "A".repeat(LevyraPlaylistShareCodec.MAX_ENCODED_CHARS + 1)
        assertEquals(LevyraPlaylistDecodeError.TooLarge, failure(oversized))
    }

    @Test
    fun `highly compressible payload is rejected by decompression bound`() {
        val raw = ByteArray(4 * 1024 * 1024) { 'A'.code.toByte() }
        val deflater = java.util.zip.Deflater(java.util.zip.Deflater.BEST_COMPRESSION)
        val compressed = try {
            deflater.setInput(raw)
            deflater.finish()
            val out = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(4_096)
            while (!deflater.finished()) {
                val produced = deflater.deflate(buffer)
                if (produced > 0) out.write(buffer, 0, produced)
            }
            out.toByteArray()
        } finally {
            deflater.end()
        }
        val encoded = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(compressed)
        assertTrue(encoded.length <= LevyraPlaylistShareCodec.MAX_ENCODED_CHARS)
        assertEquals(LevyraPlaylistDecodeError.Malformed, failure(encoded))
    }

    @Test
    fun `a corrupted payload fails instead of importing partial data`() {
        val encoded = LevyraPlaylistShareCodec.encode("Corrupt me", listOf(track("dQw4w9WgXcQ")))!!
        val corrupted = encoded.dropLast(4) + "AAAA"
        val error = failure(corrupted)
        assertTrue(
            "unexpected error $error",
            error == LevyraPlaylistDecodeError.Malformed ||
                error == LevyraPlaylistDecodeError.ChecksumMismatch ||
                error == LevyraPlaylistDecodeError.NotLevyraPayload ||
                error == LevyraPlaylistDecodeError.Empty
        )
    }

    @Test
    fun `shared links carry no whitespace and only sanitized entries`() {
        val link = LevyraPlaylistShareCodec.encodeLink(
            "Safe",
            listOf(track("dQw4w9WgXcQ", title = "https://example.invalid/x", artist = "token=abc"))
        )!!
        assertTrue(link.none { it == ' ' })
        val playlist = decoded(LevyraPlaylistShareCodec.extractPayload(link)!!)
        assertEquals(1, playlist.tracks.size)
        assertEquals("dQw4w9WgXcQ", playlist.tracks.first().id)
    }
}
