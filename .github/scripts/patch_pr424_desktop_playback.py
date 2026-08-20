from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, found {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


vlc = ROOT / "desktop/player/src/main/kotlin/com/luc4n3x/levyra/desktop/player/VlcAudioPlayer.kt"
resolver = ROOT / "desktop/core/src/main/kotlin/com/luc4n3x/levyra/desktop/core/stream/YoutubeStreamResolver.kt"

replace_once(
    vlc,
    '''import java.nio.file.Path
import java.util.Locale
''',
    '''import java.net.URI
import java.nio.file.Path
import java.util.Locale
''',
    "add URI import"
)

replace_once(
    vlc,
    '''import uk.co.caprica.vlcj.factory.MediaPlayerFactory
''',
    '''import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
''',
    "add YoutubeParsingHelper import"
)

replace_once(
    vlc,
    '''class VlcAudioPlayer private constructor(
''',
    '''internal data class YoutubePlaybackHttpOptions(
    val userAgent: String,
    val referrer: String?
)

internal fun youtubePlaybackHttpOptions(url: String): YoutubePlaybackHttpOptions? {
    val host = runCatching { URI(url).host.orEmpty().lowercase(Locale.ROOT) }.getOrDefault("")
    val youtubeMedia = host.endsWith("googlevideo.com") ||
        host.endsWith("youtube.com") ||
        host.endsWith("youtube-nocookie.com") ||
        host.endsWith("ytimg.com")
    if (!youtubeMedia) return null

    val userAgent = when {
        runCatching { YoutubeParsingHelper.isIosStreamingUrl(url) }.getOrDefault(false) ->
            YoutubeParsingHelper.getIosUserAgent(null)
        runCatching { YoutubeParsingHelper.isAndroidStreamingUrl(url) }.getOrDefault(false) ->
            YoutubeParsingHelper.getAndroidUserAgent(null)
        else -> YOUTUBE_MOBILE_WEB_USER_AGENT
    }
    val web = runCatching { YoutubeParsingHelper.isWebStreamingUrl(url) }.getOrDefault(false)
    val embedded = runCatching {
        YoutubeParsingHelper.isTvHtml5SimplyEmbeddedPlayerStreamingUrl(url)
    }.getOrDefault(false)
    val referrer = when {
        embedded -> "https://www.youtube.com/embed/"
        web -> "https://www.youtube.com/"
        else -> null
    }
    return YoutubePlaybackHttpOptions(userAgent, referrer)
}

private const val YOUTUBE_MOBILE_WEB_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Mobile Safari/537.36"

class VlcAudioPlayer private constructor(
''',
    "add Android-equivalent YouTube header selection"
)

for label, old, new in [
    (
        "play media options",
        "mediaPlayer.media().play(url, *mediaOptions(startAtMs))",
        "mediaPlayer.media().play(url, *mediaOptions(url, startAtMs))"
    ),
    (
        "prepare media options",
        "mediaPlayer.media().startPaused(url, *mediaOptions(startAtMs))",
        "mediaPlayer.media().startPaused(url, *mediaOptions(url, startAtMs))"
    ),
]:
    text = vlc.read_text(encoding="utf-8")
    count = text.count(old)
    if count < 1:
        raise SystemExit(f"{label}: expected at least one match, found {count}")
    vlc.write_text(text.replace(old, new), encoding="utf-8")

replace_once(
    vlc,
    '''    private fun mediaOptions(startAtMs: Long): Array<String> = buildList {
        add(":http-user-agent=${ExtractorHttp.DESKTOP_USER_AGENT}")
        add(":http-referrer=https://www.youtube.com/")
        add(":no-video")
        if (startAtMs > 0L) {
            add(":start-time=%.3f".format(Locale.ROOT, startAtMs / 1000.0))
        }
    }.toTypedArray()
''',
    '''    private fun mediaOptions(url: String, startAtMs: Long): Array<String> = buildList {
        if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
            val youtube = youtubePlaybackHttpOptions(url)
            add(":http-user-agent=${youtube?.userAgent ?: ExtractorHttp.DESKTOP_USER_AGENT}")
            youtube?.referrer?.let { add(":http-referrer=$it") }
        }
        add(":no-video")
        if (startAtMs > 0L) {
            add(":start-time=%.3f".format(Locale.ROOT, startAtMs / 1000.0))
        }
    }.toTypedArray()
''',
    "replace fixed VLC headers"
)

replace_once(
    resolver,
    '''    private val cache = ConcurrentHashMap<String, ResolvedAudio>()
    private val locks = ConcurrentHashMap<String, Mutex>()
''',
    '''    private val cache = ConcurrentHashMap<String, ResolvedAudio>()
    private val locks = ConcurrentHashMap<String, Mutex>()
    private val rejectedUrls = ConcurrentHashMap<String, Long>()
''',
    "add rejected URL quarantine"
)

replace_once(
    resolver,
    '''    override fun invalidate(track: Track) {
        val prefix = "${track.videoId}|"
        cache.keys.removeIf { it.startsWith(prefix) }
        locks.keys.removeIf { it.startsWith(prefix) }
    }
''',
    '''    override fun invalidate(track: Track) {
        val prefix = "${track.videoId}|"
        val now = nowMillis()
        cache.entries.forEach { (key, value) ->
            if (key.startsWith(prefix)) {
                if (value.url.startsWith("http://", true) || value.url.startsWith("https://", true)) {
                    rejectedUrls[value.url] = now + REJECTED_URL_TTL_MS
                }
                cache.remove(key, value)
            }
        }
        locks.keys.removeIf { it.startsWith(prefix) }
        rejectedUrls.entries.removeIf { it.value <= now }
    }
''',
    "quarantine failed playback URL"
)

replace_once(
    resolver,
    '''        val candidates = info.audioStreams.orEmpty().mapNotNull(::toCandidate)
        val selected = AudioStreamSelector.select(candidates, quality, codec)
''',
    '''        val candidates = info.audioStreams.orEmpty()
            .mapNotNull(::toCandidate)
            .filterNot { isRejected(it.url) }
        val selected = AudioStreamSelector.select(candidates, quality, codec)
''',
    "avoid rejected streams on VLC retry"
)

replace_once(
    resolver,
    '''    private fun toCandidate(stream: AudioStream): AudioCandidate? {
''',
    '''    private fun isRejected(url: String): Boolean {
        val until = rejectedUrls[url] ?: return false
        val now = nowMillis()
        if (until > now) return true
        rejectedUrls.remove(url, until)
        return false
    }

    private fun toCandidate(stream: AudioStream): AudioCandidate? {
''',
    "add rejected URL lookup"
)

replace_once(
    resolver,
    '''        const val FRESHNESS_MARGIN_MS = 120_000L
''',
    '''        const val FRESHNESS_MARGIN_MS = 120_000L
        const val REJECTED_URL_TTL_MS = 90_000L
''',
    "add quarantine TTL"
)

print("Desktop playback parity patch applied")
