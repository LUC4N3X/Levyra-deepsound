package com.luc4n3x.levyra.domain

enum class HomeSpotlightKind {
    ReleasedToday,
    JustReleased,
    ChartTrending,
    LevyraSelect
}

data class HomeSpotlightCandidate(
    val track: Track,
    val kind: HomeSpotlightKind,
    val score: Int,
    val releaseAgeDays: Int? = null
)

enum class HomeCollectionKind {
    Fresh,
    Local,
    Workout,
    Chill,
    Focus,
    Party,
    Rap,
    Pop,
    Discovery,
    Editorial
}

enum class HomeCollectionSource {
    Levyra,
    Editorial,
    Charts
}

data class HomeEditorialCollection(
    val id: String,
    val kind: HomeCollectionKind,
    val titleOverride: String,
    val tracks: List<Track>,
    val source: HomeCollectionSource,
    val updatedToday: Boolean,
    val accentStart: Int,
    val accentEnd: Int
)
