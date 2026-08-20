from pathlib import Path

root = Path(__file__).resolve().parents[2]
vlc = root / "desktop/player/src/main/kotlin/com/luc4n3x/levyra/desktop/player/VlcAudioPlayer.kt"
test = root / "desktop/player/src/test/kotlin/com/luc4n3x/levyra/desktop/player/YoutubePlaybackUrlTest.kt"
text = vlc.read_text(encoding="utf-8")


def replace_once(old: str, new: str, label: str) -> None:
    global text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, found {count}")
    text = text.replace(old, new, 1)

replace_once(
    "import java.util.concurrent.atomic.AtomicInteger\n",
    "import java.util.concurrent.atomic.AtomicInteger\nimport java.util.concurrent.atomic.AtomicLong\n",
    "AtomicLong import",
)

replace_once(
    '''private const val YOUTUBE_MOBILE_WEB_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Mobile Safari/537.36"
''',
    '''private const val YOUTUBE_MOBILE_WEB_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Mobile Safari/537.36"

private val youtubeRequestNumber = AtomicLong(1L)

internal fun youtubePlaybackUrl(url: String): String {
    val uri = runCatching { URI(url) }.getOrNull() ?: return url
    val host = uri.host.orEmpty().lowercase(Locale.ROOT)
    val path = uri.path.orEmpty().lowercase(Locale.ROOT)
    if (!host.endsWith("googlevideo.com") || !path.contains("videoplayback")) return url
    if (path.endsWith(".m3u8") || path.endsWith(".mpd")) return url
    if (Regex("(?:^|[?&])sq=", RegexOption.IGNORE_CASE).containsMatchIn(url)) return url
    if (Regex("(?:^|[?&])rn=", RegexOption.IGNORE_CASE).containsMatchIn(url)) return url

    val fragmentIndex = url.indexOf('#')
    val base = if (fragmentIndex >= 0) url.substring(0, fragmentIndex) else url
    val fragment = if (fragmentIndex >= 0) url.substring(fragmentIndex) else ""
    val separator = if ('?' in base) '&' else '?'
    return "$base${separator}rn=${youtubeRequestNumber.getAndIncrement()}$fragment"
}
''',
    "googlevideo rn adapter",
)

replace_once(
    '''    override fun play(url: String, startAtMs: Long) {
        if (released.get()) return
        loadedUrl = url
        requestedPaused = false
        resetTimeThrottle(startAtMs)
        pushOutputDevice()
        mediaPlayer.media().play(url, *mediaOptions(url, startAtMs))
    }
''',
    '''    override fun play(url: String, startAtMs: Long) {
        if (released.get()) return
        val playbackUrl = youtubePlaybackUrl(url)
        loadedUrl = playbackUrl
        requestedPaused = false
        resetTimeThrottle(startAtMs)
        pushOutputDevice()
        mediaPlayer.media().play(playbackUrl, *mediaOptions(playbackUrl, startAtMs))
    }
''',
    "play adapted URL",
)

replace_once(
    '''    override fun prepare(url: String, startAtMs: Long): Boolean {
        if (released.get()) return false
        loadedUrl = url
        requestedPaused = true
        resetTimeThrottle(startAtMs)
        pushOutputDevice()
        val started = runCatching {
            mediaPlayer.media().startPaused(url, *mediaOptions(url, startAtMs))
        }.getOrDefault(false)
''',
    '''    override fun prepare(url: String, startAtMs: Long): Boolean {
        if (released.get()) return false
        val playbackUrl = youtubePlaybackUrl(url)
        loadedUrl = playbackUrl
        requestedPaused = true
        resetTimeThrottle(startAtMs)
        pushOutputDevice()
        val started = runCatching {
            mediaPlayer.media().startPaused(playbackUrl, *mediaOptions(playbackUrl, startAtMs))
        }.getOrDefault(false)
''',
    "prepare adapted URL",
)

replace_once(
    '''            val youtube = youtubePlaybackHttpOptions(url)
            add(":http-user-agent=${youtube?.userAgent ?: ExtractorHttp.DESKTOP_USER_AGENT}")
            youtube?.referrer?.let { add(":http-referrer=$it") }
''',
    '''            val youtube = youtubePlaybackHttpOptions(url)
            add(":http-user-agent=${youtube?.userAgent ?: ExtractorHttp.DESKTOP_USER_AGENT}")
            youtube?.referrer?.let { add(":http-referrer=$it") }
            if (youtube != null) {
                add(":http-reconnect")
                add(":http-continuous")
            }
''',
    "VLC YouTube reconnect options",
)

vlc.write_text(text, encoding="utf-8")

test.write_text(
    '''package com.luc4n3x.levyra.desktop.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubePlaybackUrlTest {
    @Test
    fun addsRequestNumberToProgressiveGoogleVideo() {
        val input = "https://rr1---sn.example.googlevideo.com/videoplayback?expire=2000000000&id=abc"
        val output = youtubePlaybackUrl(input)

        assertTrue(output.startsWith(input + "&rn="))
    }

    @Test
    fun preservesExistingRequestNumber() {
        val input = "https://rr1---sn.example.googlevideo.com/videoplayback?expire=2000000000&rn=77&id=abc"

        assertEquals(input, youtubePlaybackUrl(input))
    }

    @Test
    fun segmentedGoogleVideoIsNotModified() {
        val input = "https://rr1---sn.example.googlevideo.com/videoplayback?expire=2000000000&sq=5&id=abc"

        assertEquals(input, youtubePlaybackUrl(input))
    }

    @Test
    fun nonYoutubeUrlIsNotModified() {
        val input = "https://example.com/audio.m4a?x=1"

        assertEquals(input, youtubePlaybackUrl(input))
    }
}
''',
    encoding="utf-8",
)

print("desktop googlevideo parity patch applied")
