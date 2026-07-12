package com.luc4n3x.levyra.data

import android.app.ActivityManager
import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.PowerManager
import kotlin.math.abs
import kotlin.math.max

internal data class LevyraVideoCandidate(
    val url: String,
    val mimeType: String,
    val codec: String,
    val width: Int,
    val height: Int,
    val fps: Int,
    val bitrate: Int,
    val itag: Int,
    val muxed: Boolean,
    val label: String
)

internal data class LevyraVideoSelection(
    val candidate: LevyraVideoCandidate,
    val targetHeight: Int,
    val hardwareDecoded: Boolean,
    val reason: String
)

internal class LevyraVideoStreamSelector(context: Context) {
    private val appContext = context.applicationContext
    private val activityManager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val decoderCapabilities by lazy { readDecoderCapabilities() }

    fun select(
        muxedCandidates: List<LevyraVideoCandidate>,
        videoOnlyCandidates: List<LevyraVideoCandidate>,
        hasSeparateAudio: Boolean,
        blocked: (String) -> Boolean
    ): LevyraVideoSelection? {
        val targetHeight = targetHeight()
        val rawMuxed = muxedCandidates.filter { it.url.isNotBlank() && !blocked(it.url) }
        val rawVideoOnly = if (hasSeparateAudio) {
            videoOnlyCandidates.filter { it.url.isNotBlank() && !blocked(it.url) }
        } else {
            emptyList()
        }
        val usableMuxed = compatibleCandidates(rawMuxed)
        val usableVideoOnly = compatibleCandidates(rawVideoOnly)
        val bestMuxed = usableMuxed.maxByOrNull { score(it, targetHeight) }
        val bestVideoOnly = usableVideoOnly.maxByOrNull { score(it, targetHeight) }
        val chosen = when {
            bestVideoOnly == null -> bestMuxed
            bestMuxed == null -> bestVideoOnly
            shouldPreferSeparated(bestMuxed, bestVideoOnly, targetHeight) -> bestVideoOnly
            else -> bestMuxed
        } ?: return null
        val hardware = decoderSupport(chosen).hardware
        val reason = buildString {
            append(chosen.height.takeIf { it > 0 }?.let { "${it}p" } ?: "auto")
            append(" · ")
            append(codecFamily(chosen))
            append(if (hardware) " HW" else " compat")
            append(if (chosen.muxed) " · avvio rapido" else " · qualità separata")
        }
        return LevyraVideoSelection(chosen, targetHeight, hardware, reason)
    }

    fun targetHeight(): Int {
        val lowRam = activityManager.isLowRamDevice
        val powerSave = powerManager.isPowerSaveMode
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let(connectivityManager::getNetworkCapabilities)
        val unmetered = !connectivityManager.isActiveNetworkMetered
        val fastTransport = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
        val metrics = appContext.resources.displayMetrics
        val displayShortSide = minOf(metrics.widthPixels, metrics.heightPixels)
        return when {
            lowRam || powerSave -> 720
            !unmetered || !fastTransport -> 720
            displayShortSide >= 1800 && decoderCapabilities.any { it.hardware && it.family == CodecFamily.AV1 } -> 2160
            displayShortSide >= 1200 -> 1440
            displayShortSide >= 900 -> 1080
            else -> 720
        }
    }

    private fun compatibleCandidates(candidates: List<LevyraVideoCandidate>): List<LevyraVideoCandidate> {
        val compatible = candidates.filter { candidate ->
            val support = decoderSupport(candidate)
            support.family == CodecFamily.OTHER || support.supported
        }
        return compatible.ifEmpty { candidates }
    }

    private fun shouldPreferSeparated(
        muxed: LevyraVideoCandidate,
        videoOnly: LevyraVideoCandidate,
        targetHeight: Int
    ): Boolean {
        val lowRam = activityManager.isLowRamDevice
        val constrained = powerManager.isPowerSaveMode || connectivityManager.isActiveNetworkMetered
        if (lowRam || constrained) return false
        val muxedHeight = muxed.height.coerceAtLeast(0)
        val videoHeight = videoOnly.height.coerceAtLeast(0)
        if (videoHeight <= muxedHeight) return false
        if (videoHeight > targetHeight && muxedHeight >= targetHeight) return false
        return videoHeight - muxedHeight >= 240 || videoHeight >= 1080 && muxedHeight < 1080
    }

    private fun score(candidate: LevyraVideoCandidate, targetHeight: Int): Int {
        val height = candidate.height.takeIf { it > 0 } ?: 360
        val distance = abs(targetHeight - height)
        val withinTarget = height <= targetHeight
        val qualityScore = if (withinTarget) {
            height * 18
        } else {
            targetHeight * 18 - distance * 22
        }
        val support = decoderSupport(candidate)
        val codecScore = when (support.family) {
            CodecFamily.AV1 -> if (support.hardware) 2_300 else -6_000
            CodecFamily.VP9 -> if (support.hardware) 1_850 else -3_000
            CodecFamily.AVC -> if (support.hardware) 1_700 else 900
            CodecFamily.HEVC -> if (support.hardware) 1_500 else -2_500
            CodecFamily.OTHER -> if (support.supported) 300 else -4_000
        }
        val containerScore = when {
            candidate.mimeType.contains("mp4", true) -> 900
            candidate.mimeType.contains("webm", true) -> 350
            else -> 0
        }
        val startScore = if (candidate.muxed) 2_600 else 1_300
        val fpsScore = when {
            candidate.fps <= 0 -> 0
            candidate.fps <= 30 -> 250
            support.hardware && !activityManager.isLowRamDevice -> 500
            else -> -800
        }
        val bitrateScore = (candidate.bitrate / 250_000).coerceIn(0, 900)
        return qualityScore + codecScore + containerScore + startScore + fpsScore + bitrateScore
    }

    private fun decoderSupport(candidate: LevyraVideoCandidate): DecoderSupport {
        val family = codecFamilyValue(candidate)
        val matching = decoderCapabilities.filter { it.family == family }
        return DecoderSupport(
            family = family,
            supported = matching.isNotEmpty(),
            hardware = matching.any { it.hardware }
        )
    }

    private fun codecFamily(candidate: LevyraVideoCandidate): String {
        return when (codecFamilyValue(candidate)) {
            CodecFamily.AV1 -> "AV1"
            CodecFamily.VP9 -> "VP9"
            CodecFamily.AVC -> "H.264"
            CodecFamily.HEVC -> "HEVC"
            CodecFamily.OTHER -> candidate.codec.ifBlank { candidate.mimeType.substringAfter('/') }.ifBlank { "video" }
        }
    }

    private fun codecFamilyValue(candidate: LevyraVideoCandidate): CodecFamily {
        val raw = "${candidate.codec} ${candidate.mimeType}".lowercase()
        return when {
            raw.contains("av01") || raw.contains("av1") -> CodecFamily.AV1
            raw.contains("vp09") || raw.contains("vp9") -> CodecFamily.VP9
            raw.contains("avc1") || raw.contains("avc") || raw.contains("h264") -> CodecFamily.AVC
            raw.contains("hev1") || raw.contains("hvc1") || raw.contains("hevc") || raw.contains("h265") -> CodecFamily.HEVC
            else -> CodecFamily.OTHER
        }
    }

    private fun readDecoderCapabilities(): List<CodecCapability> {
        return runCatching {
            MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
                .asSequence()
                .filterNot { info -> info.isEncoder }
                .flatMap { info ->
                    val hardware = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        info.isHardwareAccelerated
                    } else {
                        !info.name.contains("google", true) && !info.name.contains("software", true)
                    }
                    info.supportedTypes.asSequence().map { mime ->
                        CodecCapability(
                            family = when {
                                mime.equals("video/av01", true) -> CodecFamily.AV1
                                mime.equals("video/x-vnd.on2.vp9", true) -> CodecFamily.VP9
                                mime.equals("video/avc", true) -> CodecFamily.AVC
                                mime.equals("video/hevc", true) -> CodecFamily.HEVC
                                else -> CodecFamily.OTHER
                            },
                            hardware = hardware
                        )
                    }
                }
                .toList()
        }.getOrDefault(emptyList())
    }

    private data class DecoderSupport(
        val family: CodecFamily,
        val supported: Boolean,
        val hardware: Boolean
    )

    private data class CodecCapability(
        val family: CodecFamily,
        val hardware: Boolean
    )

    private enum class CodecFamily {
        AV1,
        VP9,
        AVC,
        HEVC,
        OTHER
    }
}
