package com.luc4n3x.levyra.data.hqaudio.tidal

import android.net.Uri
import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackCandidate
import com.luc4n3x.levyra.data.hqaudio.AudioQualityTier
import com.luc4n3x.levyra.data.hqaudio.FlacStreamInfo
import com.luc4n3x.levyra.data.hqaudio.HighQualityAudioProvider
import com.luc4n3x.levyra.data.hqaudio.ProviderFailure
import com.luc4n3x.levyra.data.hqaudio.ProviderLookupOutcome
import com.luc4n3x.levyra.data.hqaudio.ProviderSearchOutcome
import com.luc4n3x.levyra.data.hqaudio.ProviderStreamOutcome
import com.luc4n3x.levyra.data.hqaudio.ResolvedHighQualityStream
import com.luc4n3x.levyra.data.hqaudio.StreamRejection
import com.luc4n3x.levyra.data.runCatchingPreservingCancellation
import com.luc4n3x.levyra.domain.PlaybackDeliveryMethod
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal class TidalAudioProvider(
    private val manifestDirectory: File,
    private val api: TidalApi = TidalApi(),
    private val clock: () -> Long = System::currentTimeMillis
) : HighQualityAudioProvider {
    override val id = PROVIDER_ID
    override val displayName = "TIDAL"
    override val lossless = true
    @Volatile private var unavailableUntil = 0L
    private val recent = object : LinkedHashMap<String, Pair<AlternativeTrackCandidate, Long>>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<AlternativeTrackCandidate, Long>>) = size > RECENT_LIMIT
    }

    override suspend fun search(query: String, page: Int): ProviderSearchOutcome {
        if (page !in 1..2) return ProviderSearchOutcome.Found(emptyList())
        return attempt<ProviderSearchOutcome> {
            val json = coroutineScope {
                launch { api.warmStream() }
                api.get("search/tracks", mapOf("query" to query, "limit" to "$PAGE_SIZE", "offset" to "${(page - 1) * PAGE_SIZE}"))
            }
            val items = json.getJSONArray("items")
            val candidates = (0 until items.length()).mapNotNull { TidalPayload.candidate(items.getJSONObject(it)) }
            synchronized(recent) { candidates.forEach { recent[it.providerTrackId] = it to clock() + RECENT_TTL_MS } }
            ProviderSearchOutcome.Found(candidates, items.length() == PAGE_SIZE)
        }.getOrElse { ProviderSearchOutcome.Failed(failure(it)) }
    }

    override suspend fun prewarm() {
        coroutineScope {
            launch { api.warmStream() }
            api.warmCatalog()
        }
    }

    override suspend fun lookup(providerTrackId: String): ProviderLookupOutcome {
        if (!TidalPayload.isTrackId(providerTrackId)) return ProviderLookupOutcome.Missing
        synchronized(recent) { recent[providerTrackId]?.takeIf { clock() < it.second }?.first }?.let { return ProviderLookupOutcome.Found(it) }
        return attempt<ProviderLookupOutcome> {
            TidalPayload.candidate(api.get("tracks/$providerTrackId"))?.let(ProviderLookupOutcome::Found) ?: ProviderLookupOutcome.Missing
        }.getOrElse { if ((it as? TidalFailure)?.failure == ProviderFailure.NOT_FOUND) ProviderLookupOutcome.Missing else ProviderLookupOutcome.Failed(failure(it)) }
    }

    override suspend fun resolveStream(candidate: AlternativeTrackCandidate): ProviderStreamOutcome {
        if (!TidalPayload.isTrackId(candidate.providerTrackId)) return ProviderStreamOutcome.Unavailable(listOf(StreamRejection.NO_MEDIA))
        return attempt<ProviderStreamOutcome> {
            val quality = if (candidate.mediaToken == HI_RES) HI_RES else LOSSLESS
            val description = coroutineScope {
                val proxy = async { api.mediaProxy() }
                TidalDashManifest.parse(api.manifest(candidate.providerTrackId, quality)).proxiedThrough(proxy.await())
            }
            val info = description.declaredStreamInfo() ?: probeStreamInfo(description)
            if (!info.durationMatches(candidate.durationSeconds) || info.sampleRateHz != description.sampleRateHz) {
                return@attempt ProviderStreamOutcome.Unavailable(listOf(StreamRejection.DURATION_MISMATCH))
            }
            ProviderStreamOutcome.Resolved(resolvedStream(candidate, description, info))
        }.getOrElse { ProviderStreamOutcome.Failed(failure(it)) }
    }

    private fun TidalDashManifest.declaredStreamInfo(): FlacStreamInfo? = bitDepth.takeIf { it > 0 }?.let {
        FlacStreamInfo(sampleRateHz, channels, it, durationMs * sampleRateHz / 1_000L)
    }

    private suspend fun probeStreamInfo(description: TidalDashManifest): FlacStreamInfo {
        val response = api.probe(description.initialization, INIT_PROBE_BYTES)
        if (response.code !in setOf(200, 206)) throw TidalFailure(ProviderFailure.HTTP_ERROR)
        val type = response.header("Content-Type").orEmpty().substringBefore(';').lowercase()
        if (type !in setOf("audio/mp4", "video/mp4", "application/mp4", "application/octet-stream", "binary/octet-stream")) throw TidalFailure(ProviderFailure.MALFORMED_RESPONSE)
        return TidalPayload.streamInfo(response.body)
    }

    private suspend fun resolvedStream(
        candidate: AlternativeTrackCandidate,
        description: TidalDashManifest,
        info: FlacStreamInfo
    ): ResolvedHighQualityStream {
        val file = withContext(Dispatchers.IO) { TidalManifestFiles.write(manifestDirectory, description.toMpd(), clock()) }
        val tier = if (info.bitsPerSample > 16 || info.sampleRateHz > 48_000) AudioQualityTier.LOSSLESS_HI_RES else AudioQualityTier.LOSSLESS
        return ResolvedHighQualityStream(
            providerId = id, providerTrackId = candidate.providerTrackId, url = Uri.fromFile(file).toString(),
            tier = tier, mimeType = MIME_TYPE, container = "mp4", codec = "flac",
            contentLength = 0L, estimatedKbps = description.bandwidth / 1_000, expiresAtMs = clock() + STREAM_TTL_MS,
            bitDepth = info.bitsPerSample, sampleRateHz = info.sampleRateHz, deliveryMethod = PlaybackDeliveryMethod.DASH
        )
    }

    private suspend fun <T> attempt(block: suspend () -> T): Result<T> = runCatchingPreservingCancellation {
        if (clock() < unavailableUntil) throw TidalFailure(ProviderFailure.CIRCUIT_OPEN)
        block()
    }

    private fun failure(error: Throwable): ProviderFailure {
        val failure = when (error) {
            is TidalFailure -> error.failure
            is SocketTimeoutException -> ProviderFailure.TIMEOUT
            is IOException -> ProviderFailure.NETWORK
            else -> ProviderFailure.MALFORMED_RESPONSE
        }
        if (failure == ProviderFailure.FORBIDDEN) unavailableUntil = clock() + 60_000L
        if (failure == ProviderFailure.RATE_LIMITED) unavailableUntil = clock() + 30_000L
        return failure
    }

    companion object {
        const val PROVIDER_ID = "tidal"
        const val MIME_TYPE = "application/dash+xml"
        private const val HI_RES = "HI_RES_LOSSLESS"
        private const val LOSSLESS = "LOSSLESS"
        private const val PAGE_SIZE = 20
        private const val INIT_PROBE_BYTES = 65_536
        private const val STREAM_TTL_MS = 120_000L
        private const val RECENT_TTL_MS = 5L * 60L * 1_000L
        private const val RECENT_LIMIT = 256
    }
}

internal object TidalPayload {
    fun isTrackId(id: String) = id.length in 1..24 && id.all { it in '0'..'9' }

    fun candidate(json: JSONObject): AlternativeTrackCandidate? {
        if (!json.optBoolean("allowStreaming", true) || !json.optBoolean("streamReady", true)) return null
        val id = json.optString("id").takeIf(::isTrackId) ?: return null
        val title = json.optString("title").trim().takeIf { it.isNotEmpty() } ?: return null
        val duration = json.optInt("duration").takeIf { it > 0 } ?: return null
        val artists = json.optJSONArray("artists") ?: return null
        val names = (0 until artists.length()).mapNotNull { artists.optJSONObject(it)?.optString("name")?.trim()?.takeIf(String::isNotBlank) }
        if (names.isEmpty()) return null
        val tags = json.optJSONObject("mediaMetadata")?.optJSONArray("tags") ?: JSONArray()
        val version = json.optString("version").trim().takeIf { it.isNotBlank() && !it.equals("null", true) }
        return AlternativeTrackCandidate(
            providerId = TidalAudioProvider.PROVIDER_ID, providerTrackId = id,
            title = if (version != null && !title.contains(version, true)) "$title ($version)" else title,
            primaryArtists = names.take(1), featuredArtists = names.drop(1),
            album = json.optJSONObject("album")?.optString("title").orEmpty(), durationSeconds = duration,
            explicit = if (json.has("explicit")) json.optBoolean("explicit") else null,
            isrc = json.optString("isrc").takeUnless { it.equals("null", true) }.orEmpty().uppercase(),
            mediaToken = if ((0 until tags.length()).any { tags.optString(it) == "HIRES_LOSSLESS" }) "HI_RES_LOSSLESS" else "LOSSLESS"
        )
    }

    fun streamInfo(init: ByteArray): FlacStreamInfo {
        val marker = byteArrayOf(0x64, 0x66, 0x4C, 0x61)
        val offset = (4 until init.size - 46).firstOrNull { index -> marker.indices.all { init[index + it] == marker[it] } }
            ?: throw TidalFailure(ProviderFailure.MALFORMED_RESPONSE)
        val size = ((init[offset - 4].toInt() and 255).toLong() shl 24) or
            ((init[offset - 3].toInt() and 255).toLong() shl 16) or ((init[offset - 2].toInt() and 255).toLong() shl 8) or (init[offset - 1].toInt() and 255).toLong()
        if (size < 50 || offset - 4 + size > init.size) throw TidalFailure(ProviderFailure.MALFORMED_RESPONSE)
        return FlacStreamInfo.parse(byteArrayOf(0x66, 0x4C, 0x61, 0x43) + init.copyOfRange(offset + 8, offset + 46))
            ?: throw TidalFailure(ProviderFailure.MALFORMED_RESPONSE)
    }
}

private object TidalManifestFiles {
    @Synchronized
    fun write(directory: File, bytes: ByteArray, now: Long): File {
        check(directory.isDirectory || directory.mkdirs())
        directory.listFiles()?.filter { it.isFile }?.sortedByDescending(File::lastModified)?.forEachIndexed { index, file ->
            if (index >= 31 || now - file.lastModified() > 600_000L) file.delete()
        }
        return File(directory, "${UUID.randomUUID()}.mpd").apply { writeBytes(bytes) }
    }
}
