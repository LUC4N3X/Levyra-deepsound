package com.luc4n3x.levyra.ui.player

internal enum class PlayerAudioRoutePlacement {
    Overflow,
    Hidden
}

internal data class PlayerHeaderPolicy(
    val leadingCount: Int,
    val trailingCount: Int,
    val centerLocked: Boolean
)

internal fun playerAudioRoutePlacement(
    isVideoMode: Boolean,
    hasTrack: Boolean
): PlayerAudioRoutePlacement =
    if (!isVideoMode && hasTrack) PlayerAudioRoutePlacement.Overflow else PlayerAudioRoutePlacement.Hidden

internal fun playerHeaderPolicy(
    isVideoMode: Boolean,
    isLiveRadio: Boolean,
    hasTrack: Boolean,
    hasSubtitles: Boolean,
    videoQualityAvailable: Boolean,
    slotWidthDp: Float,
    spacingDp: Float,
    sideReserveDp: Float
): PlayerHeaderPolicy {
    val leadingCount = 1
    val trailingCount = listOf(
        !isVideoMode && !isLiveRadio,
        isVideoMode,
        isVideoMode && hasSubtitles,
        isVideoMode && videoQualityAvailable,
        hasTrack
    ).count { it }

    fun occupiedWidth(count: Int): Float =
        slotWidthDp * count + spacingDp * (count - 1).coerceAtLeast(0)

    return PlayerHeaderPolicy(
        leadingCount = leadingCount,
        trailingCount = trailingCount,
        centerLocked = maxOf(occupiedWidth(leadingCount), occupiedWidth(trailingCount)) <= sideReserveDp
    )
}
