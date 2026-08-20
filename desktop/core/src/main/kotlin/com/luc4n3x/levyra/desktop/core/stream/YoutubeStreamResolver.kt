package com.luc4n3x.levyra.desktop.core.stream

import com.luc4n3x.levyra.desktop.core.catalog.CatalogMapper
import com.luc4n3x.levyra.desktop.core.extractor.ExtractorHttp
import com.luc4n3x.levyra.desktop.core.model.AudioQuality
import com.luc4n3x.levyra.desktop.core.model.PreferredCodec
import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.desktop.core.model.videoId
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo

internal enum class CandidateResolutionFailure {
    RESOLVE_FAILED,
    STREAM_EXPIRED,
    STREAM_PROBE_FAILED
}

internal data class CandidateResolution(
    val candidate: AudioCandidate? = null,
    val failure: CandidateResolutionFailure? = null
)

internal fun selectVerifiedCandidate(
    rankedCandidates: List<AudioCandidate>,
    isRejected: (String) -> Boolean,
    isFresh: (String) -> Boolean,
    verify: (String) -> Boolean
): CandidateResolution {
    val eligible = rankedCandidates.filterNot { isRejected(it.url) }
    if (eligible.isEmpty()) return CandidateResolution(failure = CandidateResolutionFailure.RESOLVE_FAILED)
    val fresh = eligible.filter { isFresh(it.url) }
    if (fresh.isEmpty()) return CandidateResolution(failure = CandidateResolutionFailure.STREAM_EXPIRED)
    val selected = fresh.firstOrNull { verify(it.url) }
        ?: return CandidateResolution(failure = CandidateResolutionFailure.STREAM_PROBE_FAILED)
    return CandidateResolution(candidate = selected)
}

class YoutubeStreamResolver(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val nowMillis: () -> Long = System::currentTimeMillis
) : StreamResolver {

    private val cache = ConcurrentHashMap<String, ResolvedAudio>()
    private val locks = ConcurrentHashMap<String, Mutex>()
    private val rejectedUrls = ConcurrentHashMap<String, Long>()
    private val streamProbeClient: OkHttpClient = ExtractorHttp.client.newBuilder()
        .connectTimeout(450, TimeUnit.MILLISECONDS)
        .readTimeout(800, TimeUnit.MILLISECONDS)
        .writeTimeout(350, TimeUnit.MILLISECONDS)
        .callTimeout(950, TimeUnit.MILLISECONDS)
        .build()

    override suspend fun resolve(
        track: Track,
        quality: AudioQuality,
        codec: PreferredCodec
    ): ResolvedAudio {
        resolveOffline(track)?.let { return it }
        val key = cacheKey(track, quality, codec)
        cache[key]?.takeIf { it.isFresh(nowMillis() + FRESHNESS_MARGIN_MS) }?.let { return it }
        val mutex = locks.computeIfAbsent(key) { Mutex() }
        return mutex.withLock {
            cache[key]?.takeIf { it.isFresh(nowMillis() + FRESHNESS_MARGIN_MS) }?.let { return@withLock it }
            val resolved = withContext(dispatcher) { fetch(track, quality, codec) }
            cache[key] = resolved
            cache.entries.removeIf { !it.value.isFresh(nowMillis()) }
            resolved
        }
    }

    override fun invalidate(track: Track) {
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

    private fun resolveOffline(track: Track): ResolvedAudio? {
        if (track.offlinePath.isBlank()) return null
        val file = runCatching { Path.of(track.offlinePath) }.getOrNull()
            ?: throw StreamResolutionException("Percorso offline non valido")
        if (!Files.isRegularFile(file)) {
            throw StreamResolutionException("File offline non disponibile")
        }
        return ResolvedAudio(
            url = file.toUri().toString(),
            label = track.offlineMediaLabel.ifBlank { "Offline" },
            expiresAtMillis = 0L,
            durationMs = track.durationMs,
            artworkUrl = track.artworkUrl,
            title = track.title,
            artist = track.artist
        )
    }

    private fun fetch(track: Track, quality: AudioQuality, codec: PreferredCodec): ResolvedAudio {
        val info = try {
            StreamInfo.getInfo(ServiceList.YouTube, track.videoUrl)
        } catch (error: Exception) {
            throw StreamResolutionException("RESOLVE_FAILED: Impossibile leggere il brano da YouTube", error)
        }
        val rankedCandidates = info.audioStreams.orEmpty()
            .mapNotNull(::toCandidate)
            .filter(AudioStreamSelector::isPlayable)
            .sortedByDescending { candidate -> AudioStreamSelector.score(candidate, quality, codec) }
        val resolution = selectVerifiedCandidate(
            rankedCandidates = rankedCandidates,
            isRejected = ::isRejected,
            isFresh = ::isFreshPlaybackUrl,
            verify = ::verifyDirectAudioUrlFast
        )
        val selected = resolution.candidate ?: when (resolution.failure) {
            CandidateResolutionFailure.RESOLVE_FAILED -> throw StreamResolutionException(
                "RESOLVE_FAILED: Nessuno stream audio disponibile per ${track.title}"
            )

            CandidateResolutionFailure.STREAM_EXPIRED -> throw StreamResolutionException(
                "STREAM_EXPIRED: Gli stream disponibili sono scaduti"
            )

            CandidateResolutionFailure.STREAM_PROBE_FAILED, null -> throw StreamResolutionException(
                "STREAM_PROBE_FAILED: Nessuno stream audio verificato disponibile per ${track.title}"
            )
        }
        val artwork = info.thumbnails
            .orEmpty()
            .maxByOrNull { it.width.coerceAtLeast(0) * it.height.coerceAtLeast(0) }
            ?.url
            .orEmpty()
        return ResolvedAudio(
            url = selected.url,
            label = selected.label,
            expiresAtMillis = expiryOf(selected.url),
            durationMs = info.duration.coerceAtLeast(0L) * 1000L,
            artworkUrl = artwork.ifBlank { track.artworkUrl },
            title = info.name.orEmpty().ifBlank { track.title },
            artist = CatalogMapper.cleanArtist(info.uploaderName.orEmpty()).ifBlank { track.artist }
        )
    }

    private fun verifyDirectAudioUrlFast(url: String): Boolean {
        if (url.isBlank() || !isFreshPlaybackUrl(url)) return false
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Range", "bytes=0-8191")
            .header("Accept", "*/*")
            .header("Accept-Encoding", "identity")
            .header("User-Agent", ExtractorHttp.YOUTUBE_STREAM_USER_AGENT)
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
        val until = rejectedUrls[url] ?: return false
        val now = nowMillis()
        if (until > now) return true
        rejectedUrls.remove(url, until)
        return false
    }

    private fun toCandidate(stream: AudioStream): AudioCandidate? {
        if (!stream.isUrl) return null
        val url = stream.content.orEmpty()
        if (url.isBlank()) return null
        val format = stream.format
        return AudioCandidate(
            url = url,
            mimeType = format?.mimeType.orEmpty(),
            suffix = format?.suffix.orEmpty(),
            itag = stream.itag,
            averageBitrate = stream.averageBitrate
        )
    }

    private companion object {
        const val FRESHNESS_MARGIN_MS = 120_000L
        const val REJECTED_URL_TTL_MS = 90_000L
        val PROBE_REJECT_CODES = setOf(403, 404, 410, 416, 429)

        fun cacheKey(track: Track, quality: AudioQuality, codec: PreferredCodec): String =
            "${track.videoId}|${quality.name}|${codec.name}"

        fun expiryOf(url: String): Long {
            val raw = url.substringAfter("expire=", "").takeWhile { it.isDigit() }
            val seconds = raw.toLongOrNull() ?: return 0L
            return seconds * 1000L
        }
    }
}
