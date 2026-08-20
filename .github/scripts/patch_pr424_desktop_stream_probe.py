from pathlib import Path

path = Path("desktop/core/src/main/kotlin/com/luc4n3x/levyra/desktop/core/stream/YoutubeStreamResolver.kt")
text = path.read_text(encoding="utf-8")


def replace_once(old: str, new: str, label: str) -> None:
    global text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, found {count}")
    text = text.replace(old, new, 1)

replace_once(
    '''import com.luc4n3x.levyra.desktop.core.catalog.CatalogMapper
''',
    '''import com.luc4n3x.levyra.desktop.core.catalog.CatalogMapper
import com.luc4n3x.levyra.desktop.core.extractor.ExtractorHttp
''',
    "ExtractorHttp import",
)

replace_once(
    '''import java.util.concurrent.ConcurrentHashMap
''',
    '''import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
''',
    "TimeUnit import",
)

replace_once(
    '''import org.schabi.newpipe.extractor.ServiceList
''',
    '''import okhttp3.OkHttpClient
import okhttp3.Request
import org.schabi.newpipe.extractor.ServiceList
''',
    "OkHttp imports",
)

replace_once(
    '''    private val cache = ConcurrentHashMap<String, ResolvedAudio>()
    private val locks = ConcurrentHashMap<String, Mutex>()
    private val rejectedUrls = ConcurrentHashMap<String, Long>()
''',
    '''    private val cache = ConcurrentHashMap<String, ResolvedAudio>()
    private val locks = ConcurrentHashMap<String, Mutex>()
    private val rejectedUrls = ConcurrentHashMap<String, Long>()
    private val streamProbeClient: OkHttpClient = ExtractorHttp.client.newBuilder()
        .connectTimeout(450, TimeUnit.MILLISECONDS)
        .readTimeout(800, TimeUnit.MILLISECONDS)
        .writeTimeout(350, TimeUnit.MILLISECONDS)
        .callTimeout(950, TimeUnit.MILLISECONDS)
        .build()
''',
    "stream probe client",
)

replace_once(
    '''        val candidates = info.audioStreams.orEmpty()
            .mapNotNull(::toCandidate)
            .filterNot { isRejected(it.url) }
        val selected = AudioStreamSelector.select(candidates, quality, codec)
            ?: throw StreamResolutionException("Nessuno stream audio disponibile per ${track.title}")
''',
    '''        val candidates = info.audioStreams.orEmpty()
            .mapNotNull(::toCandidate)
            .filterNot { isRejected(it.url) }
            .filter(AudioStreamSelector::isPlayable)
            .sortedByDescending { candidate -> AudioStreamSelector.score(candidate, quality, codec) }
        val selected = candidates.firstOrNull { candidate -> verifyDirectAudioUrlFast(candidate.url) }
            ?: throw StreamResolutionException("Nessuno stream audio verificato disponibile per ${track.title}")
''',
    "verified candidate selection",
)

replace_once(
    '''    private fun isRejected(url: String): Boolean {
''',
    '''    private fun verifyDirectAudioUrlFast(url: String): Boolean {
        if (url.isBlank() || !isFreshPlaybackUrl(url)) return false
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Range", "bytes=0-8191")
            .header("Accept", "*/*")
            .header("Accept-Encoding", "identity")
            .header("User-Agent", STREAM_PROBE_USER_AGENT)
            .build()
        return runCatching {
            streamProbeClient.newCall(request).execute().use { response ->
                if (response.code in PROBE_REJECT_CODES) return@use false
                if (response.code !in 200..299 && response.code != 206) return@use false
                val contentType = response.header("Content-Type").orEmpty().lowercase()
                if (contentType.contains("text/html") || contentType.contains("application/json")) return@use false
                response.peekBody(32L).bytes().isNotEmpty()
            }
        }.getOrDefault(false)
    }

    private fun isFreshPlaybackUrl(url: String): Boolean {
        val expiresAt = expiryOf(url)
        return expiresAt <= 0L || nowMillis() + FRESHNESS_MARGIN_MS < expiresAt
    }

    private fun isRejected(url: String): Boolean {
''',
    "direct stream verifier",
)

replace_once(
    '''        const val FRESHNESS_MARGIN_MS = 120_000L
        const val REJECTED_URL_TTL_MS = 90_000L
''',
    '''        const val FRESHNESS_MARGIN_MS = 120_000L
        const val REJECTED_URL_TTL_MS = 90_000L
        const val STREAM_PROBE_USER_AGENT =
            "com.google.visionos.youtube/1.02(RealityDevice14,1; U; CPU visionOS 25_6_0 like Mac OS X; US)"
        val PROBE_REJECT_CODES = setOf(403, 404, 410, 416, 429)
''',
    "probe constants",
)

path.write_text(text, encoding="utf-8")
print("desktop direct stream probe patch applied")
