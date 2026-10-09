package com.luc4n3x.levyra.ui.i18n

data class LevyraSmartOfflineCopy(
    val title: String,
    val subtitle: String,
    val enabled: String,
    val enabledSubtitle: String,
    val storageLimit: String,
    val storageLimitSubtitle: String,
    val custom: String,
    val customStorage: String,
    val customStorageSubtitle: String,
    val megabytes: String,
    val wifiOnly: String,
    val wifiOnlySubtitle: String,
    val chargingOnly: String,
    val chargingOnlySubtitle: String,
    val preferFavorites: String,
    val preferFavoritesSubtitle: String,
    val excludedArtists: String,
    val excludedArtistsSubtitle: String,
    val excludedPlaylists: String,
    val excludedPlaylistsSubtitle: String,
    val commaSeparatedHint: String,
    val save: String,
    val refresh: String,
    val refreshSubtitle: String,
    val updatedToday: String,
    val neverUpdated: String,
    val protectedManual: String
)

fun LevyraStrings.smartOfflineCopy(): LevyraSmartOfflineCopy = smartOffline
