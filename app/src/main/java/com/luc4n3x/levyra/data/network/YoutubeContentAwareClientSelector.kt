package com.luc4n3x.levyra.data.network

import java.util.Locale

internal data class YoutubeContentHints(
    val isLive: Boolean? = null,
    val isUserUpload: Boolean? = null,
    val isAgeRestricted: Boolean? = null
) {
    fun mergedWith(other: YoutubeContentHints): YoutubeContentHints = YoutubeContentHints(
        isLive = other.isLive ?: isLive,
        isUserUpload = other.isUserUpload ?: isUserUpload,
        isAgeRestricted = other.isAgeRestricted ?: isAgeRestricted
    )

    companion object {
        val NONE = YoutubeContentHints()

        private val AGE_RESTRICTION_MARKERS = listOf(
            "confirm your age",
            "age-restricted",
            "age restricted",
            "inappropriate for some users"
        )

        fun fromMetadata(videoType: String): YoutubeContentHints = YoutubeContentHints(
            isUserUpload = true.takeIf { videoType.uppercase(Locale.ROOT).contains("UGC") }
        )

        fun fromPlayabilityReason(reason: String): YoutubeContentHints {
            val normalized = reason.lowercase(Locale.ROOT)
            return YoutubeContentHints(
                isAgeRestricted = true.takeIf { AGE_RESTRICTION_MARKERS.any(normalized::contains) }
            )
        }
    }
}

internal data class YoutubeClientRanking(
    val score: Double,
    val averageLatencyMs: Long,
    val consecutiveFailures: Int,
    val blockedUntilMs: Long
)

/**
 * Orders playback clients by content compatibility first and observed health second.
 *
 * A client is excluded only when it provably cannot serve the content, and only while at least one
 * compatible candidate remains, so a conservative hint can never strand playback. Age restriction is
 * handled as a promotion instead of an exclusion because the other clients may still succeed.
 */
internal object YoutubeContentAwareClientSelector {
    const val DEFAULT_SCORE = 50.0
    private const val VR_PRIMARY_SCORE_TOLERANCE = 12.0

    private fun supports(profile: YoutubeClientProfile, hints: YoutubeContentHints): Boolean {
        if (hints.isLive == true && !profile.supportsLive) return false
        if (hints.isUserUpload == true && !profile.supportsUserUploads) return false
        return true
    }

    fun order(
        profiles: List<YoutubeClientProfile>,
        hints: YoutubeContentHints,
        nowMs: Long,
        ranking: (YoutubeClientProfile) -> YoutubeClientRanking?
    ): List<YoutubeClientProfile> {
        if (profiles.isEmpty()) return emptyList()
        val compatible = profiles.filter { supports(it, hints) }.ifEmpty { profiles }
        val unblocked = compatible.filter { (ranking(it)?.blockedUntilMs ?: 0L) <= nowMs }
        val sorted = unblocked.ifEmpty { compatible }.sortedWith(
            compareByDescending<YoutubeClientProfile> { ranking(it)?.score ?: DEFAULT_SCORE }
                .thenBy { ranking(it)?.averageLatencyMs ?: Long.MAX_VALUE }
                .thenBy { it.tier }
        )
        if (hints.isAgeRestricted == true) {
            return promote(sorted) { it.supportsAgeRestricted }
        }
        if (!keepVrPrimary(sorted, ranking)) return sorted
        return promote(sorted) { it.clientName == YoutubeClientRegistry.ANDROID_VR.clientName }
    }

    private fun keepVrPrimary(
        sorted: List<YoutubeClientProfile>,
        ranking: (YoutubeClientProfile) -> YoutubeClientRanking?
    ): Boolean {
        val vr = sorted.firstOrNull { it.clientName == YoutubeClientRegistry.ANDROID_VR.clientName }
            ?: return false
        val vrRanking = ranking(vr)
        val bestScore = sorted.firstOrNull()?.let { ranking(it)?.score } ?: DEFAULT_SCORE
        return (vrRanking?.consecutiveFailures ?: 0) == 0 &&
            (vrRanking?.score ?: DEFAULT_SCORE) >= bestScore - VR_PRIMARY_SCORE_TOLERANCE
    }

    private fun promote(
        sorted: List<YoutubeClientProfile>,
        predicate: (YoutubeClientProfile) -> Boolean
    ): List<YoutubeClientProfile> {
        val (promoted, rest) = sorted.partition(predicate)
        return if (promoted.isEmpty()) sorted else promoted + rest
    }
}
