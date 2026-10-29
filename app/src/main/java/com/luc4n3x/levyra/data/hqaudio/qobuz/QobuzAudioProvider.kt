package com.luc4n3x.levyra.data.hqaudio.qobuz

import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackCandidate
import com.luc4n3x.levyra.data.hqaudio.AlternativeTrackFingerprint
import com.luc4n3x.levyra.data.hqaudio.FlacStreamInfo
import com.luc4n3x.levyra.data.hqaudio.HighQualityAudioProvider
import com.luc4n3x.levyra.data.hqaudio.ProviderBackendHealth
import com.luc4n3x.levyra.data.hqaudio.ProviderCircuitBreaker
import com.luc4n3x.levyra.data.hqaudio.ProviderFailure
import com.luc4n3x.levyra.data.hqaudio.ProviderLookupOutcome
import com.luc4n3x.levyra.data.hqaudio.ProviderSearchOutcome
import com.luc4n3x.levyra.data.hqaudio.ProviderStreamOutcome
import com.luc4n3x.levyra.data.hqaudio.ProviderStreamValidator
import com.luc4n3x.levyra.data.hqaudio.ResolvedHighQualityStream
import com.luc4n3x.levyra.data.hqaudio.StreamRejection
import com.luc4n3x.levyra.data.hqaudio.StreamValidation
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject

internal class QobuzAudioProvider(
    private val api: QobuzApi = QobuzApi(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val formatId: Int = 27
) : HighQualityAudioProvider {
    init { require(formatId in setOf(6, 7, 27)) }
    override val id = PROVIDER_ID
    override val displayName = "Qobuz"
    override val lossless = true
    private val circuit = ProviderCircuitBreaker(id, clock, failureThreshold = 1, baseOpenMs = 30_000)
    private val gate = Semaphore(2)
    private data class Cached<T>(val value: T, val until: Long)
    private val searches = LinkedHashMap<String, Cached<ProviderSearchOutcome.Found>>()
    private val tracks = LinkedHashMap<String, Cached<AlternativeTrackCandidate>>()
    private val streams = LinkedHashMap<String, Cached<ResolvedHighQualityStream>>()

    override suspend fun search(query: String, page: Int): ProviderSearchOutcome {
        if (page !in 1..3) return ProviderSearchOutcome.Found(emptyList())
        return attempt({ ProviderSearchOutcome.Failed(it) }) {
            val key = "$page:$query"
            cached(searches, key)?.let { return@attempt it }
            val json = api.get("track/search", mapOf("query" to query, "limit" to "20", "offset" to ((page - 1) * 20).toString()))
            val rows = json.getJSONObject("tracks")
            val items = rows.getJSONArray("items")
            val candidates = (0 until items.length()).mapNotNull { QobuzPayload.candidate(items.getJSONObject(it)) }
            candidates.forEach { remember(tracks, it.providerTrackId, it, clock() + METADATA_TTL_MS) }
            ProviderSearchOutcome.Found(candidates, page * 20 < rows.optInt("total")).also {
                remember(searches, key, it, clock() + METADATA_TTL_MS)
            }
        }
    }

    override suspend fun lookup(providerTrackId: String): ProviderLookupOutcome {
        if (!QobuzPayload.isTrackId(providerTrackId)) return ProviderLookupOutcome.Missing
        return attempt({ if (it == ProviderFailure.NOT_FOUND) ProviderLookupOutcome.Missing else ProviderLookupOutcome.Failed(it) }) {
            val candidate = cached(tracks, providerTrackId) ?: QobuzPayload.candidate(api.get("track/get", mapOf("track_id" to providerTrackId)))
            if (candidate == null || candidate.providerTrackId != providerTrackId) ProviderLookupOutcome.Missing else {
                remember(tracks, providerTrackId, candidate, clock() + METADATA_TTL_MS)
                ProviderLookupOutcome.Found(candidate)
            }
        }
    }

    override suspend fun resolveStream(candidate: AlternativeTrackCandidate): ProviderStreamOutcome {
        if (candidate.providerId != id || !QobuzPayload.isTrackId(candidate.providerTrackId)) {
            return ProviderStreamOutcome.Unavailable(listOf(StreamRejection.NO_MEDIA))
        }
        return attempt({ ProviderStreamOutcome.Failed(it) }) {
            val key = AlternativeTrackFingerprint.of(candidate)
            cached(streams, key)?.takeIf { it.isFresh(clock(), 90_000) }?.let { return@attempt ProviderStreamOutcome.Resolved(it) }
            val json = api.get("track/getFileUrl", mapOf("track_id" to candidate.providerTrackId, "format_id" to formatId.toString(), "intent" to "stream"))
            if (json.optString("track_id") != candidate.providerTrackId || json.optInt("format_id") !in setOf(6, 7, 27)) {
                return@attempt ProviderStreamOutcome.Unavailable(listOf(StreamRejection.NO_MEDIA))
            }
            val url = json.optString("url").toHttpUrlOrNull()?.takeIf(QobuzNetwork::allows)
                ?: return@attempt ProviderStreamOutcome.Unavailable(listOf(StreamRejection.TRANSPORT))
            val response = api.probe(url)
            val info = FlacStreamInfo.parse(response.body)
            if (info == null || info.totalSamples <= 0 || info.bitsPerSample !in setOf(16, 24)) {
                return@attempt ProviderStreamOutcome.Unavailable(listOf(StreamRejection.UNSUPPORTED_CONTAINER))
            }
            when (val validation = ProviderStreamValidator.validateLossless(response, candidate.durationSeconds)) {
                is StreamValidation.Invalid -> ProviderStreamOutcome.Unavailable(listOf(validation.rejection))
                is StreamValidation.Valid -> {
                    val expiry = url.queryParameter("etsp")?.toLongOrNull()?.takeIf { it in 1..Long.MAX_VALUE / 1_000 }?.times(1_000L)
                    val stream = ResolvedHighQualityStream(
                        providerId = id, providerTrackId = candidate.providerTrackId, url = url.toString(),
                        tier = validation.tier, mimeType = validation.mimeType, container = validation.container, codec = validation.codec,
                        contentLength = validation.contentLength, estimatedKbps = validation.estimatedKbps,
                        expiresAtMs = minOf(expiry ?: Long.MAX_VALUE, clock() + STREAM_TTL_MS),
                        bitDepth = validation.bitDepth, sampleRateHz = validation.sampleRateHz
                    )
                    if (!stream.isFresh(clock(), 90_000)) ProviderStreamOutcome.Unavailable(listOf(StreamRejection.NO_MEDIA)) else {
                        remember(streams, key, stream, stream.expiresAtMs)
                        ProviderStreamOutcome.Resolved(stream)
                    }
                }
            }
        }
    }

    override fun reportLookupTimeout() {
        val permit = circuit.acquire()
        if (permit != ProviderCircuitBreaker.Permit.REJECTED) circuit.onFailure(permit, "LOOKUP_TIMEOUT")
        circuit.release(permit)
    }
    override fun health(): List<ProviderBackendHealth> = listOf(circuit.snapshot(id))

    private suspend fun <T> attempt(failed: (ProviderFailure) -> T, block: suspend () -> T): T = withContext(Dispatchers.IO) {
        gate.withPermit {
            val permit = circuit.acquire()
            if (permit == ProviderCircuitBreaker.Permit.REJECTED) return@withPermit failed(ProviderFailure.CIRCUIT_OPEN)
            val started = clock()
            try {
                block().also { circuit.onSuccess(permit, clock() - started) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                val failure = when (error) {
                    is QobuzFailure -> error.failure
                    is SocketTimeoutException -> ProviderFailure.TIMEOUT
                    is IOException -> ProviderFailure.NETWORK
                    else -> ProviderFailure.MALFORMED_RESPONSE
                }
                if (failure == ProviderFailure.RATE_LIMITED) circuit.onRateLimitedAdaptive((error as? QobuzFailure)?.retryAfterMs)
                else if (failure != ProviderFailure.NOT_FOUND) circuit.onFailure(permit, failure.name)
                else circuit.onSuccess(permit, clock() - started)
                failed(failure)
            } finally {
                circuit.release(permit)
            }
        }
    }

    private fun <T> cached(cache: LinkedHashMap<String, Cached<T>>, key: String): T? = synchronized(cache) {
        cache[key]?.takeIf { clock() < it.until }?.value
    }

    private fun <T> remember(cache: LinkedHashMap<String, Cached<T>>, key: String, value: T, until: Long) = synchronized(cache) {
        cache.entries.removeIf { clock() >= it.value.until }
        cache.remove(key)
        if (cache.size >= 64) cache.remove(cache.keys.first())
        cache[key] = Cached(value, until)
    }

    companion object {
        const val PROVIDER_ID = "qobuz"
        const val LOOKUP_BUDGET_MS = 4_000L
        private const val METADATA_TTL_MS = 5L * 60 * 1_000
        private const val STREAM_TTL_MS = 5L * 60 * 1_000
    }
}

internal object QobuzPayload {
    private val featuredSeparator = Regex("\\s+(?:feat\\.?|ft\\.?|featuring)\\s+", RegexOption.IGNORE_CASE)

    fun isTrackId(value: String) = value.length in 1..24 && value.all { it in '0'..'9' }

    fun candidate(json: JSONObject): AlternativeTrackCandidate? {
        if (!json.optBoolean("streamable", false)) return null
        val id = json.optString("id").takeIf(::isTrackId) ?: return null
        val title = json.text("title").takeIf(String::isNotEmpty) ?: return null
        val duration = json.optInt("duration").takeIf { it > 0 } ?: return null
        val artist = json.optJSONObject("performer")?.text("name").orEmpty()
        if (artist.isBlank()) return null
        val names = featuredSeparator.split(artist, limit = 2)
        val album = json.optJSONObject("album")
        val version = json.text("version")
        return AlternativeTrackCandidate(
            providerId = QobuzAudioProvider.PROVIDER_ID, providerTrackId = id,
            title = if (version.isNotBlank() && !title.contains(version, true)) "$title ($version)" else title,
            primaryArtists = listOf(names.first()), featuredArtists = names.drop(1),
            album = album?.text("title").orEmpty(), durationSeconds = duration,
            explicit = if (!json.isNull("parental_warning")) json.optBoolean("parental_warning") else null,
            isrc = json.text("isrc"), releaseYear = album?.text("release_date_original")?.take(4)?.toIntOrNull() ?: 0
        )
    }

    private fun JSONObject.text(key: String): String = if (isNull(key)) "" else optString(key).trim()
}
