package com.luc4n3x.levyra.feature.dearrow

sealed interface DeArrowVotable {
    val locked: Boolean
    val votes: Int
    val original: Boolean
}

data class DeArrowTitle(
    val title: String,
    override val locked: Boolean,
    override val votes: Int,
    override val original: Boolean
) : DeArrowVotable

data class DeArrowThumbnail(
    val timestamp: Double?,
    override val locked: Boolean,
    override val votes: Int,
    override val original: Boolean
) : DeArrowVotable

data class DeArrowBranding(
    val titles: List<DeArrowTitle>,
    val thumbnails: List<DeArrowThumbnail>
)

sealed interface DeArrowBrandingOutcome {
    data class Resolved(val branding: DeArrowBranding?) : DeArrowBrandingOutcome
    data object Inconclusive : DeArrowBrandingOutcome
}

data class DeArrowResult(
    val title: String?,
    val thumbnailUrl: String?
)
