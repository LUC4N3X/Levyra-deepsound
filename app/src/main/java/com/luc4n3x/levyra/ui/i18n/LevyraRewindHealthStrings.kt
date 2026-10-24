package com.luc4n3x.levyra.ui.i18n

enum class MusicExperienceText {
    RewindSubtitle, Chapters, Coverage, Private, HealthTitle, HealthSubtitle,
    Scan, Issues, Repair, RepairBody, Missing, Empty, Unreadable, Incomplete,
    Permission, Unavailable, ScanHint, ActiveDays
}

internal fun experienceCopy(vararg values: String): Map<MusicExperienceText, String> {
    require(values.size == MusicExperienceText.entries.size)
    return MusicExperienceText.entries.zip(values).toMap()
}

internal val musicExperienceCopies = mapOf(
    "en" to experienceCopy(
        "A year that sounds like you.", "Chapters", "Based on saved listening: %s", "On your device. Just for you.",
        "Download Health Center", "Keep your music ready to go.", "Check my downloads", "Needs attention", "Repair",
        "Download a fresh copy using your download settings. The existing file stays untouched. Check again when the download finishes.",
        "File missing", "Empty file", "Audio cannot be read", "Audio may be incomplete", "Restore folder access", "Could not check this file",
        "Checks saved files and audio tracks locally. This is not a full audio decode or a checksum verification.", "Days with music"
    ),
    "it" to experienceCopy(
        "Un anno che suona come te.", "Capitoli", "Basato sugli ascolti salvati: %s", "Sul tuo dispositivo. Solo per te.",
        "Download Health Center", "La tua musica, pronta a partire.", "Controlla i miei download", "Da sistemare", "Ripara",
        "Scarica una nuova copia con le tue impostazioni di download. Il file esistente resta intatto. Ricontrolla al termine del download.",
        "File mancante", "File vuoto", "Audio non leggibile", "L'audio potrebbe essere incompleto", "Ripristina l'accesso alla cartella", "Impossibile verificare il file",
        "Verifica file salvati e tracce audio sul dispositivo. Non esegue una decodifica completa né un controllo checksum.", "Giorni in musica"
    )
) + translatedMusicExperienceCopies

fun LevyraStrings.musicExperience(key: MusicExperienceText): String =
    localizedValueOrEnglish(musicExperienceCopies, code).getValue(key)
