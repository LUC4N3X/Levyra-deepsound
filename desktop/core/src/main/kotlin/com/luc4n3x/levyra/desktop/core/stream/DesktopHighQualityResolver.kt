package com.luc4n3x.levyra.desktop.core.stream

import com.luc4n3x.levyra.data.hqaudio.*
import com.luc4n3x.levyra.data.hqaudio.jiosaavn.JioSaavnAudioProvider
import com.luc4n3x.levyra.data.hqaudio.lossless.KuwoAudioProvider
import com.luc4n3x.levyra.data.hqaudio.tidal.TidalAudioProvider
import com.luc4n3x.levyra.desktop.core.model.AudioQuality
import com.luc4n3x.levyra.desktop.core.model.PreferredCodec
import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.domain.HighQualityAudioMode
import com.luc4n3x.levyra.domain.PlaybackDeliveryMethod
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope

class DesktopHighQualityResolver private constructor(
    private val original: StreamResolver,
    private val resolver: HighQualityAudioResolver,
    private val mode: () -> HighQualityAudioMode,
    private val forDownload: Boolean,
    private val clock: () -> Long
) : StreamResolver {
    private val selected = ConcurrentHashMap<String, ResolvedHighQualityStream>()

    override suspend fun resolve(track: Track, quality: AudioQuality, codec: PreferredCodec): ResolvedAudio {
        if (track.offlinePath.isNotBlank()) return original.resolve(track, quality, codec)
        resolver.mode = mode()
        if (!resolver.mode.enabled || quality == AudioQuality.LOW || track.title.isBlank() || track.artist.isBlank()) {
            return original.resolve(track, quality, codec)
        }
        val started = clock()
        val query = AlternativeTrackQuery(track.title, track.artist, track.album, track.durationMs,
            track.explicit, track.isrc, musicVideo = track.musicVideo)
        val pending = resolver.begin(track.id, query)
        val early = resolver.await(pending, 1_200L)
        choose(track, early)?.let { return it }
        val normal = try {
            Result.success(original.resolve(track, quality, codec))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
        val budget = when {
            forDownload && resolver.mode.lossless -> 35_000L
            resolver.mode == HighQualityAudioMode.AUTOMATIC -> 2_500L
            else -> 8_500L
        }
        val result = resolver.await(pending, (budget - (clock() - started)).coerceAtLeast(0L))
        choose(track, result)?.let { return it }
        return normal.getOrThrow()
    }

    private fun choose(track: Track, result: HighQualityResolution): ResolvedAudio? {
        val stream = (result as? HighQualityResolution.Selected)?.stream ?: return null
        if (!mode().enabled || stream.tier.lossless && !mode().lossless) return null
        if (!stream.tier.lossless && stream.tier != AudioQualityTier.KBPS_320) return null
        if (forDownload && (!stream.offlineAllowed || stream.deliveryMethod !in setOf(PlaybackDeliveryMethod.PROGRESSIVE, PlaybackDeliveryMethod.DASH))) return null
        if (selected.size >= 192) selected.keys.firstOrNull()?.let(selected::remove)
        selected[track.id] = stream
        return ResolvedAudio(
            url = stream.url,
            label = "${resolver.providerName(stream.providerId)} · ${stream.qualityLabel}",
            expiresAtMillis = stream.expiresAtMs,
            durationMs = track.durationMs,
            artworkUrl = track.artworkUrl,
            title = track.title,
            artist = track.artist,
            contentLength = stream.contentLength,
            container = stream.container,
            codec = stream.codec,
            offlineAllowed = stream.offlineAllowed,
            sourceIdentity = "${stream.providerId}:${stream.providerTrackId}",
            deliveryMethod = stream.deliveryMethod
        )
    }

    override fun invalidate(track: Track) {
        selected.remove(track.id)?.let { stream ->
            resolver.reportPlaybackFailure(track.id, stream.providerTrackId, "playback failed", stream.providerId)
        }
        original.invalidate(track)
    }

    companion object {
        fun create(scope: CoroutineScope, original: StreamResolver, cache: Path,
                   mode: () -> HighQualityAudioMode, forDownload: Boolean = false): DesktopHighQualityResolver {
            val exchange = OkHttpProviderExchange(if (forDownload) HighQualityProviderHttpClient::downloadClient else HighQualityProviderHttpClient::client)
            val providers = buildList {
                add(TidalAudioProvider(cache.resolve("tidal-manifests").toFile()))
                add(KuwoAudioProvider(exchange))
                add(JioSaavnAudioProvider(exchange))
            }
            val mappings = object : HighQualityMappingStorage {
                private val entries = ConcurrentHashMap<String, String>()
                override fun read(key: String) = entries[key]
                override fun write(key: String, value: String) { entries[key] = value }
                override fun remove(key: String) { entries.remove(key) }
                override fun keys(): Set<String> = entries.keys.toSet()
            }
            return DesktopHighQualityResolver(original, HighQualityAudioResolver(providers, HighQualityMappingStore(mappings), scope,
                lookupBudgetMs = if (forDownload) 35_000L else 8_000L,
                losslessLaneBudgetMs = if (forDownload) 30_000L else 4_000L), mode, forDownload, System::currentTimeMillis)
        }
    }
}
