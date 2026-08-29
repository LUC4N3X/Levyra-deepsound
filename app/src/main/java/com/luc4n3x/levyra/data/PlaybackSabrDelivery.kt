package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.PlaybackDeliveryMethod
import com.luc4n3x.levyra.domain.PlaybackStreamDescriptor
import com.luc4n3x.levyra.domain.PlaybackStreamKind
import com.luc4n3x.levyra.player.sabr.SabrEndpoint
import com.luc4n3x.levyra.player.sabr.SabrFormatId
import com.luc4n3x.levyra.player.sabr.SabrStreamSpec
import java.util.Base64

/** One adaptive format as the player response describes it, reduced to what SABR delivery needs. */
internal data class SabrFormatCandidate(
    val itag: Int,
    val lastModified: Long,
    val mimeType: String,
    val contentLength: Long,
    val bitrate: Int = 0,
    val averageBitrate: Int = 0,
    val sampleRate: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val fps: Int = 0,
    val qualityLabel: String = ""
) {
    val isAudio: Boolean get() = mimeType.startsWith("audio/", ignoreCase = true)
    val isVideo: Boolean get() = mimeType.startsWith("video/", ignoreCase = true)

    fun formatId(): SabrFormatId = SabrFormatId(itag, lastModified)

    fun isUsable(): Boolean =
        itag > 0 && lastModified > 0L && contentLength > 0L && (isAudio || isVideo)
}

/**
 * Builds the descriptor for one SABR-delivered format.
 *
 * The descriptive query on the returned URL is not sent anywhere: it mirrors the shape of a direct
 * Googlevideo URL so the resolver's existing freshness, MIME and playability checks read a SABR
 * stream exactly the way they read a progressive one.
 */
internal fun buildSabrStreamDescriptor(
    endpointUrl: String,
    ustreamerConfig: ByteArray,
    candidate: SabrFormatCandidate,
    companionAudio: SabrFormatCandidate?,
    durationMs: Long,
    clientName: Int,
    clientVersion: String,
    userAgent: String,
    expiresAtMs: Long
): PlaybackStreamDescriptor? {
    if (!SabrEndpoint.isAllowed(endpointUrl)) return null
    if (ustreamerConfig.isEmpty() || durationMs <= 0L || !candidate.isUsable()) return null
    if (companionAudio != null && (!companionAudio.isUsable() || !companionAudio.isAudio)) return null
    if (candidate.isVideo && companionAudio == null) return null

    val spec = SabrStreamSpec(
        endpointUrl = endpointUrl,
        ustreamerConfig = ustreamerConfig,
        format = candidate.formatId(),
        companionAudioFormat = companionAudio?.formatId(),
        contentLength = candidate.contentLength,
        durationMs = durationMs,
        videoTrack = candidate.isVideo,
        clientName = clientName,
        clientVersion = clientVersion,
        userAgent = userAgent
    )
    val mimeType = candidate.mimeType.substringBefore(';').trim()
    val url = buildString {
        append(spec.toUri())
        append("?itag=").append(candidate.itag)
        append("&mime=").append(mimeType.replace("/", "%2F"))
        expiresAtMs.takeIf { it > 0L }?.let { append("&expire=").append(it / 1000L) }
    }
    return PlaybackStreamDescriptor(
        url = url,
        kind = if (candidate.isVideo) PlaybackStreamKind.VIDEO else PlaybackStreamKind.AUDIO,
        deliveryMethod = PlaybackDeliveryMethod.SABR,
        container = mimeType.substringAfter('/', ""),
        mimeType = mimeType,
        codec = candidate.mimeType.substringAfter("codecs=", "").trim('"', ' '),
        bitrate = candidate.bitrate,
        averageBitrate = candidate.averageBitrate,
        sampleRate = candidate.sampleRate,
        width = candidate.width,
        height = candidate.height,
        fps = candidate.fps,
        itag = candidate.itag,
        qualityLabel = candidate.qualityLabel,
        expiresAtMs = expiresAtMs,
        selected = false
    )
}

/**
 * SABR audio candidates ordered the way Levyra would pick a direct audio stream, so the fallback
 * inherits the user's quality preference instead of introducing a second selector.
 */
internal fun orderSabrAudioCandidates(
    candidates: List<SabrFormatCandidate>,
    preferHighestBitrate: Boolean
): List<SabrFormatCandidate> {
    val usable = candidates.filter { it.isUsable() && it.isAudio }
    val byBitrate = compareBy<SabrFormatCandidate> { it.averageBitrate.coerceAtLeast(it.bitrate) }
    return usable.sortedWith(if (preferHighestBitrate) byBitrate.reversed() else byBitrate)
}

/**
 * SABR video candidates capped at [maxHeight] so server-driven delivery can never hand the device a
 * resolution Levyra already decided it should not decode.
 */
internal fun orderSabrVideoCandidates(
    candidates: List<SabrFormatCandidate>,
    maxHeight: Int
): List<SabrFormatCandidate> = candidates
    .filter { it.isUsable() && it.isVideo && (maxHeight <= 0 || it.height <= maxHeight) }
    .sortedWith(
        compareByDescending<SabrFormatCandidate> { it.height }
            .thenByDescending { it.averageBitrate.coerceAtLeast(it.bitrate) }
    )

/** The player response encodes the ustreamer config as URL-safe base64 with optional padding. */
internal fun decodeSabrUstreamerConfig(value: String): ByteArray? {
    val trimmed = value.trim()
    if (trimmed.isEmpty() || trimmed.length > MAX_USTREAMER_CONFIG_CHARS) return null
    val normalized = trimmed.replace('-', '+').replace('_', '/').trimEnd('=')
    val padding = (4 - normalized.length % 4) % 4
    return runCatching {
        Base64.getDecoder().decode(normalized + "=".repeat(padding))
    }.getOrNull()?.takeIf { it.isNotEmpty() }
}

private const val MAX_USTREAMER_CONFIG_CHARS = 16 * 1024
