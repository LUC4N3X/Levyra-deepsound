package com.luc4n3x.levyra.desktop.core.stream

import com.luc4n3x.levyra.desktop.core.catalog.CatalogMapper
import com.luc4n3x.levyra.desktop.core.model.AudioQuality
import com.luc4n3x.levyra.desktop.core.model.PreferredCodec
import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.desktop.core.model.videoId
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo

class YoutubeStreamResolver(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val nowMillis: () -> Long = System::currentTimeMillis
) : StreamResolver {

    private val cache = ConcurrentHashMap<String, ResolvedAudio>()
    private val locks = ConcurrentHashMap<String, Mutex>()
    private val rejectedUrls = ConcurrentHashMap<String, Long>()

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
            throw StreamResolutionException("Impossibile leggere il brano da YouTube", error)
        }
        val candidates = info.audioStreams.orEmpty()
            .mapNotNull(::toCandidate)
            .filterNot { isRejected(it.url) }
        val selected = AudioStreamSelector.select(candidates, quality, codec)
            ?: throw StreamResolutionException("Nessuno stream audio disponibile per ${track.title}")
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

        fun cacheKey(track: Track, quality: AudioQuality, codec: PreferredCodec): String =
            "${track.videoId}|${quality.name}|${codec.name}"

        fun expiryOf(url: String): Long {
            val raw = url.substringAfter("expire=", "").takeWhile { it.isDigit() }
            val seconds = raw.toLongOrNull() ?: return 0L
            return seconds * 1000L
        }
    }
}
