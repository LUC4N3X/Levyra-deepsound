package com.luc4n3x.levyra.data.hqaudio

import com.luc4n3x.levyra.domain.PlaybackDeliveryMethod
import com.luc4n3x.levyra.domain.PlaybackStreamDescriptor
import com.luc4n3x.levyra.domain.PlaybackStreamKind
import com.luc4n3x.levyra.domain.ResolvedPlaybackManifest
import com.luc4n3x.levyra.domain.Track
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import org.json.JSONArray
import org.json.JSONObject

internal const val YOUTUBE_ID = "4NRXx6U8ABQ"
internal const val YOUTUBE_AUDIO_URL = "https://rr1---sn.googlevideo.com/videoplayback?expire=9999999999&itag=140&mime=audio%2Fmp4"

internal fun query(
    title: String = "Blinding Lights",
    artist: String = "The Weeknd",
    album: String = "After Hours",
    durationMs: Long = 200_000L,
    explicit: Boolean? = null,
    isrc: String = ""
) = AlternativeTrackQuery(title, artist, album, durationMs, explicit, isrc)

internal fun candidate(
    id: String = "pW-kkdqr",
    providerId: String = "jiosaavn",
    title: String = "Blinding Lights",
    primary: List<String> = listOf("The Weeknd"),
    featured: List<String> = emptyList(),
    album: String = "After Hours",
    duration: Int = 200,
    explicit: Boolean? = false,
    offers320: Boolean = true,
    isrc: String = ""
) = AlternativeTrackCandidate(
    providerId = providerId,
    providerTrackId = id,
    title = title,
    primaryArtists = primary,
    featuredArtists = featured,
    album = album,
    durationSeconds = duration,
    explicit = explicit,
    isrc = isrc,
    offers320 = offers320,
    mediaToken = "token-$id"
)

internal fun resolvedStream(
    candidate: AlternativeTrackCandidate,
    tier: AudioQualityTier = AudioQualityTier.KBPS_320,
    expiresAtMs: Long = System.currentTimeMillis() + 3_600_000L
) = ResolvedHighQualityStream(
    providerId = candidate.providerId,
    providerTrackId = candidate.providerTrackId,
    url = "https://aac.saavncdn.com/820/${candidate.providerTrackId}_${tier.kbps}.mp4",
    quality = HighQualityStreamQuality.lossy(HighQualityCodec.AAC, tier.kbps, tier.kbps),
    mimeType = "audio/mp4",
    container = "mp4",
    codec = "mp4a",
    contentLength = bytesFor(tier.kbps, candidate.durationSeconds),
    expiresAtMs = expiresAtMs
)

internal fun losslessStream(
    candidate: AlternativeTrackCandidate,
    bitDepth: Int = 24,
    sampleRateHz: Int = 96_000,
    expiresAtMs: Long = System.currentTimeMillis() + 3_600_000L
) = ResolvedHighQualityStream(
    providerId = candidate.providerId,
    providerTrackId = candidate.providerTrackId,
    url = "https://streaming-qobuz-std.akamaized.net/file?uid=${candidate.providerTrackId}&fmt=7",
    quality = HighQualityStreamQuality(
        codec = HighQualityCodec.FLAC,
        nominalKbps = null,
        estimatedKbps = 2_400,
        sampleRateHz = sampleRateHz,
        bitDepth = bitDepth,
        channels = 2
    ),
    mimeType = "audio/flac",
    container = "flac",
    codec = "flac",
    contentLength = 60_000_000L,
    expiresAtMs = expiresAtMs
)

internal fun bytesFor(kbps: Int, seconds: Int): Long = kbps * 1_000L * seconds / 8L

internal fun mp4ProbeBody(): ByteArray =
    byteArrayOf(0, 0, 0, 0x1c) +
        "ftypisom".toByteArray(Charsets.US_ASCII) +
        ByteArray(16) +
        "mp4a".toByteArray(Charsets.US_ASCII) +
        ByteArray(64)

internal fun probeResponse(
    totalBytes: Long,
    contentType: String = "audio/mp4",
    code: Int = 206,
    body: ByteArray = mp4ProbeBody()
) = ProviderHttpResponse(
    code = code,
    headers = mapOf("Content-Type" to contentType, "Content-Range" to "bytes 0-8191/$totalBytes"),
    body = body
)

internal fun jsonResponse(body: String, code: Int = 200) =
    ProviderHttpResponse(code, mapOf("Content-Type" to "application/json"), body.toByteArray(Charsets.UTF_8))

internal fun htmlResponse(code: Int) =
    ProviderHttpResponse(code, mapOf("Content-Type" to "text/html"), "<HTML>Access Denied</HTML>".toByteArray())

internal fun saavnSong(
    id: String,
    title: String,
    primary: List<String>,
    album: String,
    duration: Int,
    featured: List<String> = emptyList(),
    explicit: String = "0",
    offers320: String = "true",
    token: String = "enc-$id"
): JSONObject = JSONObject()
    .put("id", id)
    .put("title", title)
    .put("type", "song")
    .put("explicit_content", explicit)
    .put(
        "more_info",
        JSONObject()
            .put("album", album)
            .put("duration", duration.toString())
            .put("320kbps", offers320)
            .put("encrypted_media_url", token)
            .put(
                "artistMap",
                JSONObject()
                    .put("primary_artists", JSONArray(primary.map { JSONObject().put("name", it) }))
                    .put("featured_artists", JSONArray(featured.map { JSONObject().put("name", it) }))
            )
    )

internal fun searchBody(vararg songs: JSONObject): String = JSONObject()
    .put("total", songs.size)
    .put("start", 1)
    .put("results", JSONArray(songs.toList()))
    .toString()

internal fun playbackTrack(
    streamUrl: String = "",
    manifest: ResolvedPlaybackManifest? = null
) = Track(
    id = YOUTUBE_ID,
    title = "Blinding Lights",
    artist = "The Weeknd",
    album = "After Hours",
    durationMs = 200_000L,
    streamUrl = streamUrl,
    videoUrl = "https://www.youtube.com/watch?v=$YOUTUBE_ID",
    thumbnailUrl = "https://lh3.googleusercontent.com/thumb",
    largeThumbnailUrl = "https://lh3.googleusercontent.com/large",
    source = "YouTube",
    moodTags = emptySet(),
    energy = 60,
    vocal = 60,
    replayScore = 0,
    cacheScore = 0,
    accentStart = 0,
    accentEnd = 0,
    playbackManifest = manifest
)

internal fun normalManifest(averageBitrate: Int = 128_000): ResolvedPlaybackManifest {
    val now = System.currentTimeMillis()
    return ResolvedPlaybackManifest(
        sourceVideoId = YOUTUBE_ID,
        provider = "LevyraExtractor",
        resolvedAtMs = now,
        expiresAtMs = now + 3_600_000L,
        durationMs = 200_000L,
        selectedAudioUrl = YOUTUBE_AUDIO_URL,
        selectedVideoUrl = "",
        streams = listOf(
            PlaybackStreamDescriptor(
                url = YOUTUBE_AUDIO_URL,
                kind = PlaybackStreamKind.AUDIO,
                deliveryMethod = PlaybackDeliveryMethod.PROGRESSIVE,
                container = "m4a",
                mimeType = "audio/mp4",
                codec = "mp4a.40.2",
                bitrate = averageBitrate + 2_000,
                averageBitrate = averageBitrate,
                itag = 140,
                expiresAtMs = now + 3_600_000L,
                selected = true
            )
        )
    )
}

internal fun normalTrack(averageBitrate: Int = 128_000): Track =
    playbackTrack(streamUrl = YOUTUBE_AUDIO_URL, manifest = normalManifest(averageBitrate))

internal class InMemoryMappingStorage : HighQualityMappingStorage {
    val values = ConcurrentHashMap<String, String>()

    override fun read(key: String): String? = values[key]

    override fun write(key: String, value: String) {
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }

    override fun keys(): Set<String> = values.keys.toSet()
}

internal class FakeHighQualityProvider(
    override val id: String = "jiosaavn",
    override val displayName: String = "JioSaavn",
    override val losslessCapable: Boolean = false,
    override val supportsLookup: Boolean = true,
    var searchOutcome: suspend (String) -> ProviderSearchOutcome = { ProviderSearchOutcome.Found(emptyList()) },
    var lookupOutcome: suspend (String) -> ProviderLookupOutcome = { ProviderLookupOutcome.Missing },
    var streamOutcome: suspend (AlternativeTrackCandidate) -> ProviderStreamOutcome = {
        ProviderStreamOutcome.Resolved(resolvedStream(it))
    }
) : HighQualityAudioProvider {
    val searches = CopyOnWriteArrayList<String>()
    val lookups = CopyOnWriteArrayList<String>()
    val streamRequests = CopyOnWriteArrayList<String>()

    override suspend fun search(query: String): ProviderSearchOutcome {
        searches += query
        return searchOutcome(query)
    }

    override suspend fun lookup(providerTrackId: String): ProviderLookupOutcome {
        lookups += providerTrackId
        return lookupOutcome(providerTrackId)
    }

    val preferences = CopyOnWriteArrayList<HighQualityPreference>()

    override suspend fun resolveStream(
        candidate: AlternativeTrackCandidate,
        preference: HighQualityPreference
    ): ProviderStreamOutcome {
        streamRequests += candidate.providerTrackId
        preferences += preference
        return streamOutcome(candidate)
    }
}

internal fun flacProbeBody(
    sampleRateHz: Int = 96_000,
    bitDepth: Int = 24,
    channels: Int = 2,
    seconds: Int = 200
): ByteArray {
    val totalSamples = sampleRateHz.toLong() * seconds
    val packed = (sampleRateHz.toLong() shl 44) or
        ((channels - 1).toLong() shl 41) or
        ((bitDepth - 1).toLong() shl 36) or
        totalSamples
    val streamInfo = ByteArray(34)
    for (index in 0 until 8) {
        streamInfo[10 + index] = (packed ushr (56 - index * 8)).toByte()
    }
    return "fLaC".toByteArray(Charsets.US_ASCII) + byteArrayOf(0, 0, 0, 34) + streamInfo + ByteArray(64)
}

internal fun flacResponse(
    totalBytes: Long,
    body: ByteArray = flacProbeBody(),
    contentType: String = "audio/flac",
    code: Int = 206
) = ProviderHttpResponse(
    code = code,
    headers = mapOf("Content-Type" to contentType, "Content-Range" to "bytes 0-8191/$totalBytes"),
    body = body
)

internal fun mp3Response(totalBytes: Long, contentType: String = "audio/mpeg") = ProviderHttpResponse(
    code = 206,
    headers = mapOf("Content-Type" to contentType, "Content-Range" to "bytes 0-8191/$totalBytes"),
    body = byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte(), 0x64) + ByteArray(128)
)

internal fun qobuzTrack(
    id: Long,
    title: String = "Blinding Lights",
    version: String? = null,
    performer: String = "The Weeknd",
    album: String = "After Hours",
    duration: Int = 200,
    isrc: String = "USUG11904206",
    explicit: Boolean = false,
    bitDepth: Int = 24,
    samplingRateKhz: Double = 96.0,
    streamable: Boolean = true
): JSONObject = JSONObject()
    .put("id", id)
    .put("title", title)
    .put("version", version ?: JSONObject.NULL)
    .put("duration", duration)
    .put("isrc", isrc)
    .put("parental_warning", explicit)
    .put("streamable", streamable)
    .put("maximum_bit_depth", bitDepth)
    .put("maximum_sampling_rate", samplingRateKhz)
    .put("performer", JSONObject().put("name", performer))
    .put("album", JSONObject().put("title", album).put("artist", JSONObject().put("name", performer)))

internal fun qobuzSearchBody(vararg tracks: JSONObject): String = JSONObject()
    .put("success", true)
    .put("data", JSONObject().put("tracks", JSONObject().put("items", JSONArray(tracks.toList()))))
    .toString()

internal fun qobuzStreamBody(url: String, bitDepth: Int = 24, samplingRateKhz: Double = 96.0): String = JSONObject()
    .put("success", true)
    .put("data", JSONObject().put("url", url).put("bit_depth", bitDepth).put("sampling_rate", samplingRateKhz))
    .toString()

internal fun qobuzMediaUrl(format: Int, expiresAtSeconds: Long = System.currentTimeMillis() / 1_000L + 1_800L) =
    "https://streaming-qobuz-std.akamaized.net/file?uid=1&fmt=$format&etsp=$expiresAtSeconds&hmac=signature"
