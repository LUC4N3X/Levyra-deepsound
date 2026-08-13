package com.luc4n3x.levyra.data

enum class HomeSectionPresentation {
    ArtworkRow,
    ArtworkGrid,
    TrackGrid
}

internal object HomeSectionLayoutPolicy {
    const val ARTWORK_GRID_CAPACITY = 12

    private const val TRACK_GRID_MINIMUM_TRACKS = 8
    private const val ARTWORK_GRID_MINIMUM_TRACKS = 6
    private const val ROTATION = 4

    fun presentationFor(position: Int, trackCount: Int): HomeSectionPresentation {
        if (position < 0) return HomeSectionPresentation.ArtworkRow
        return when (position % ROTATION) {
            1, 3 -> if (trackCount >= TRACK_GRID_MINIMUM_TRACKS) {
                HomeSectionPresentation.TrackGrid
            } else {
                HomeSectionPresentation.ArtworkRow
            }
            2 -> if (trackCount in ARTWORK_GRID_MINIMUM_TRACKS..ARTWORK_GRID_CAPACITY) {
                HomeSectionPresentation.ArtworkGrid
            } else {
                HomeSectionPresentation.ArtworkRow
            }
            else -> HomeSectionPresentation.ArtworkRow
        }
    }
}
