package com.luc4n3x.levyra.data.hqaudio.tidal

import android.net.Uri
import android.util.Base64
import android.util.Xml
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
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser

internal class TidalAudioProvider(
    private val manifestDirectory: File,
    private val api: TidalApi = TidalApi(),
    private val clock: () -> Long = System::currentTimeMillis
) : HighQualityAudioProvider {
    override val id = PROVIDER_ID
    override val displayName = "TIDAL"
    override val lossless = true
    @Volatile private var unavailableUntil = 0L

    override suspend fun search(query: String, page: Int): ProviderSearchOutcome {
        if (page !in 1..2) return ProviderSearchOutcome.Found(emptyList())
        return attempt<ProviderSearchOutcome> {
            val json = api.get("search", mapOf("query" to query, "limit" to "20", "offset" to ((page - 1) * 20).toString(), "types" to "TRACKS"))
            val items = json.getJSONObject("tracks").getJSONArray("items")
            ProviderSearchOutcome.Found((0 until items.length()).mapNotNull { TidalPayload.candidate(items.getJSONObject(it)) }, items.length() == 20)
        }.getOrElse { ProviderSearchOutcome.Failed(failure(it)) }
    }

    override suspend fun lookup(providerTrackId: String): ProviderLookupOutcome {
        if (!TidalPayload.isTrackId(providerTrackId)) return ProviderLookupOutcome.Missing
        return attempt<ProviderLookupOutcome> {
            TidalPayload.candidate(api.get("tracks/$providerTrackId"))?.let(ProviderLookupOutcome::Found) ?: ProviderLookupOutcome.Missing
        }.getOrElse { if ((it as? TidalFailure)?.failure == ProviderFailure.NOT_FOUND) ProviderLookupOutcome.Missing else ProviderLookupOutcome.Failed(failure(it)) }
    }

    override suspend fun resolveStream(candidate: AlternativeTrackCandidate): ProviderStreamOutcome {
        if (!TidalPayload.isTrackId(candidate.providerTrackId)) return ProviderStreamOutcome.Unavailable(listOf(StreamRejection.NO_MEDIA))
        return attempt<ProviderStreamOutcome> {
            val manifest = playbackManifest(candidate)
            val description = TidalPayload.manifest(manifest)
            val info = probeStreamInfo(description)
            if (!info.durationMatches(candidate.durationSeconds) || info.sampleRateHz != description.sampleRateHz) {
                return@attempt ProviderStreamOutcome.Unavailable(listOf(StreamRejection.DURATION_MISMATCH))
            }
            ProviderStreamOutcome.Resolved(resolvedStream(candidate, manifest, description, info))
        }.getOrElse { ProviderStreamOutcome.Failed(failure(it)) }
    }

    private suspend fun playbackManifest(candidate: AlternativeTrackCandidate): ByteArray {
        val hiRes = candidate.mediaToken == "HI_RES_LOSSLESS"
        val parameters = mapOf("playbackmode" to "STREAM", "assetpresentation" to "FULL", "prefetch" to "false", "audioquality" to if (hiRes) "HI_RES_LOSSLESS" else "LOSSLESS")
        val json = try {
            api.get("tracks/${candidate.providerTrackId}/playbackinfopostpaywall/v4", parameters, hiRes)
        } catch (failure: TidalFailure) {
            if (!hiRes || failure.failure !in setOf(ProviderFailure.FORBIDDEN, ProviderFailure.NOT_FOUND)) throw failure
            api.get("tracks/${candidate.providerTrackId}/playbackinfopostpaywall/v4", parameters + ("audioquality" to "LOSSLESS"))
        }
        if (json.optString("manifestMimeType") != "application/dash+xml") throw TidalFailure(ProviderFailure.MALFORMED_RESPONSE)
        val encoded = json.getString("manifest")
        if (encoded.length > 131_072) throw TidalFailure(ProviderFailure.MALFORMED_RESPONSE)
        return Base64.decode(encoded, Base64.DEFAULT)
    }

    private suspend fun probeStreamInfo(description: TidalPayload.Manifest): FlacStreamInfo {
        val response = api.probe(description.initialization, 65_536)
        if (response.code !in setOf(200, 206)) throw TidalFailure(ProviderFailure.HTTP_ERROR)
        val type = response.header("Content-Type").orEmpty().substringBefore(';').lowercase()
        if (type !in setOf("audio/mp4", "video/mp4", "application/mp4", "application/octet-stream", "binary/octet-stream")) throw TidalFailure(ProviderFailure.MALFORMED_RESPONSE)
        return TidalPayload.streamInfo(response.body)
    }

    private suspend fun resolvedStream(
        candidate: AlternativeTrackCandidate,
        manifest: ByteArray,
        description: TidalPayload.Manifest,
        info: FlacStreamInfo
    ): ResolvedHighQualityStream {
        val file = withContext(Dispatchers.IO) { TidalManifestFiles.write(manifestDirectory, manifest, clock()) }
        val tier = if (info.bitsPerSample > 16 || info.sampleRateHz > 48_000) AudioQualityTier.LOSSLESS_HI_RES else AudioQualityTier.LOSSLESS
        return ResolvedHighQualityStream(
            providerId = id, providerTrackId = candidate.providerTrackId, url = Uri.fromFile(file).toString(),
            tier = tier, mimeType = "application/dash+xml", container = "mp4", codec = "flac",
            contentLength = 0L, estimatedKbps = description.bitrate / 1_000, expiresAtMs = clock() + STREAM_TTL_MS,
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
        private const val STREAM_TTL_MS = 120_000L

        fun isManifestUri(value: String): Boolean = value.startsWith("file://", ignoreCase = true) &&
            value.contains("/tidal-manifests/") && value.substringBefore('?').endsWith(".mpd")
    }
}

internal object TidalPayload {
    data class Manifest(val initialization: HttpUrl, val sampleRateHz: Int, val bitrate: Int)

    fun isTrackId(id: String) = id.length in 1..24 && id.all { it in '0'..'9' }

    fun candidate(json: JSONObject): AlternativeTrackCandidate? {
        if (!json.optBoolean("allowStreaming", true)) return null
        val id = json.optString("id").takeIf(::isTrackId) ?: return null
        val title = json.optString("title").trim().takeIf { it.isNotEmpty() } ?: return null
        val duration = json.optInt("duration").takeIf { it > 0 } ?: return null
        val artists = json.optJSONArray("artists") ?: return null
        val names = (0 until artists.length()).mapNotNull { artists.optJSONObject(it)?.optString("name")?.takeIf(String::isNotBlank) }
        if (names.isEmpty()) return null
        val tags = json.optJSONObject("mediaMetadata")?.optJSONArray("tags") ?: JSONArray()
        val version = json.optString("version").takeIf { it.isNotBlank() && !it.equals("null", true) }
        return AlternativeTrackCandidate(
            providerId = TidalAudioProvider.PROVIDER_ID, providerTrackId = id,
            title = if (version != null && !title.contains(version, true)) "$title ($version)" else title,
            primaryArtists = names.take(1), featuredArtists = names.drop(1),
            album = json.optJSONObject("album")?.optString("title").orEmpty(), durationSeconds = duration,
            explicit = if (json.has("explicit")) json.optBoolean("explicit") else null,
            isrc = json.optString("isrc").takeUnless { it.equals("null", true) }.orEmpty(),
            mediaToken = if ((0 until tags.length()).any { tags.optString(it) == "HIRES_LOSSLESS" }) "HI_RES_LOSSLESS" else "LOSSLESS"
        )
    }

    fun manifest(bytes: ByteArray): Manifest {
        require(bytes.size in 1..65_536)
        val xml = bytes.toString(Charsets.UTF_8)
        require(!xml.contains("<!DOCTYPE", true) && !xml.contains("<!ENTITY", true))
        val parser = Xml.newPullParser().apply { setInput(xml.reader()) }
        var initialization: HttpUrl? = null
        var sampleRate = 0
        var bitrate = 0
        var representations = 0
        var templates = 0
        var elements = 0
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                require(++elements <= 512)
                require(parser.name !in setOf("ContentProtection", "pssh", "BaseURL", "Location", "UTCTiming"))
                if (parser.name == "Representation") {
                    require(++representations == 1 && parser.getAttributeValue(null, "codecs") == "flac")
                    sampleRate = parser.getAttributeValue(null, "audioSamplingRate").toInt()
                    bitrate = parser.getAttributeValue(null, "bandwidth").toInt()
                }
                if (parser.name == "SegmentTemplate") {
                    require(++templates == 1)
                    initialization = parser.getAttributeValue(null, "initialization").toHttpUrl()
                    require(TidalNetwork.allows(initialization))
                    val media = parser.getAttributeValue(null, "media").replace("$" + "Number$", "1").toHttpUrl()
                    require(TidalNetwork.allows(media))
                }
            }
            parser.next()
        }
        require(sampleRate in 8_000..384_000 && bitrate > 0)
        return Manifest(requireNotNull(initialization), sampleRate, bitrate)
    }

    fun streamInfo(init: ByteArray): FlacStreamInfo {
        val marker = byteArrayOf(0x64, 0x66, 0x4C, 0x61)
        val offset = (4 until init.size - 46).firstOrNull { index -> marker.indices.all { init[index + it] == marker[it] } }
            ?: throw TidalFailure(ProviderFailure.MALFORMED_RESPONSE)
        val size = ((init[offset - 4].toInt() and 255).toLong() shl 24) or
            ((init[offset - 3].toInt() and 255).toLong() shl 16) or ((init[offset - 2].toInt() and 255).toLong() shl 8) or (init[offset - 1].toInt() and 255).toLong()
        require(size >= 50 && offset - 4 + size <= init.size)
        return requireNotNull(FlacStreamInfo.parse(byteArrayOf(0x66, 0x4C, 0x61, 0x43) + init.copyOfRange(offset + 8, offset + 46)))
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
