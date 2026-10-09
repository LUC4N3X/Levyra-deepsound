package com.luc4n3x.levyra.ui.i18n

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

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
    val songs: String,
    val updatedPrefix: String,
    val protectedManual: String
)

fun LevyraStrings.smartOfflineCopy(): LevyraSmartOfflineCopy = when (code) {
    "it" -> LevyraSmartOfflineCopy(
        title = "Smart Offline",
        subtitle = "Mantiene offline la musica più utile in base ai tuoi ascolti",
        enabled = "Attiva Smart Offline",
        enabledSubtitle = "Aggiorna automaticamente una raccolta separata dai download manuali",
        storageLimit = "Limite di archiviazione",
        storageLimitSubtitle = "Si applica solo ai brani gestiti da Smart Offline",
        custom = "Personalizzato",
        customStorage = "Limite personalizzato",
        customStorageSubtitle = "Inserisci lo spazio massimo riservato a Smart Offline",
        megabytes = "MB",
        wifiOnly = "Solo Wi‑Fi",
        wifiOnlySubtitle = "Avvia gli aggiornamenti solo su reti non a consumo",
        chargingOnly = "Solo durante la ricarica",
        chargingOnlySubtitle = "Rimanda il lavoro finché il dispositivo non è in carica",
        preferFavorites = "Dai priorità ai preferiti",
        preferFavoritesSubtitle = "I brani preferiti ricevono un forte bonus nel ranking",
        excludedArtists = "Artisti esclusi",
        excludedArtistsSubtitle = "Smart Offline ignorerà questi artisti",
        excludedPlaylists = "Playlist escluse",
        excludedPlaylistsSubtitle = "Usa nomi o identificativi delle playlist",
        commaSeparatedHint = "Separati da virgole",
        save = "Salva",
        refresh = "Aggiorna Smart Offline",
        refreshSubtitle = "Ricalcola ora la raccolta usando i dati locali",
        updatedToday = "Aggiornato oggi",
        neverUpdated = "Non ancora aggiornato",
        songs = "brani",
        updatedPrefix = "Aggiornato",
        protectedManual = "Protetto come download manuale"
    )
    else -> LevyraSmartOfflineCopy(
        title = "Smart Offline",
        subtitle = "Keeps useful music offline based on your listening",
        enabled = "Enable Smart Offline",
        enabledSubtitle = "Automatically updates a collection kept separate from manual downloads",
        storageLimit = "Storage limit",
        storageLimitSubtitle = "Applies only to Smart Offline-managed tracks",
        custom = "Custom",
        customStorage = "Custom limit",
        customStorageSubtitle = "Enter the maximum space reserved for Smart Offline",
        megabytes = "MB",
        wifiOnly = "Wi‑Fi only",
        wifiOnlySubtitle = "Runs updates only on unmetered networks",
        chargingOnly = "Only while charging",
        chargingOnlySubtitle = "Defers work until the device is charging",
        preferFavorites = "Prefer favorites",
        preferFavoritesSubtitle = "Favorite tracks receive a strong ranking bonus",
        excludedArtists = "Excluded artists",
        excludedArtistsSubtitle = "Smart Offline will ignore these artists",
        excludedPlaylists = "Excluded playlists",
        excludedPlaylistsSubtitle = "Use playlist names or identifiers",
        commaSeparatedHint = "Comma separated",
        save = "Save",
        refresh = "Refresh Smart Offline",
        refreshSubtitle = "Recalculate the collection now from local data",
        updatedToday = "Updated today",
        neverUpdated = "Not updated yet",
        songs = "songs",
        updatedPrefix = "Updated",
        protectedManual = "Protected as a manual download"
    )
}

fun LevyraSmartOfflineCopy.updatedLabel(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    if (timestamp <= 0L) return neverUpdated
    val zone = ZoneId.systemDefault()
    val day = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    if (day == today) return updatedToday
    return "$updatedPrefix ${day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))}"
}
