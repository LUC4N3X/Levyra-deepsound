package com.luc4n3x.levyra.ui.i18n

private val mixLabKeyOrder = listOf(
    "mixLab",
    "mixLabSubtitle",
    "mixLabConfigureTitle",
    "mixLabFamiliarity",
    "mixLabFamiliarityFavorites",
    "mixLabFamiliarityDiscovery",
    "mixLabRecency",
    "mixLabRecencyClassics",
    "mixLabRecencyNew",
    "mixLabDuration",
    "mixLabDurationShort",
    "mixLabDurationMedium",
    "mixLabDurationLong",
    "mixLabDurationAny",
    "mixLabTrackCount",
    "mixLabGenres",
    "mixLabGenresHint",
    "mixLabArtists",
    "mixLabArtistsHint",
    "mixLabMood",
    "mixLabMoodHint",
    "mixLabPresets",
    "mixLabPresetChill",
    "mixLabPresetWorkout",
    "mixLabPresetLateNight",
    "mixLabPresetRediscover",
    "mixLabPresetFreshFinds",
    "mixLabGenerate",
    "mixLabRegenerate",
    "mixLabTuneParameters",
    "mixLabGenerating",
    "mixLabPreviewTitle",
    "mixLabPreviewTracks",
    "mixLabPreviewArtists",
    "mixLabPreviewFamiliarShare",
    "mixLabPreviewDiscoveryShare",
    "mixLabPlay",
    "mixLabShuffle",
    "mixLabAddToQueue",
    "mixLabSaveAsPlaylist",
    "mixLabSaveDialogTitle",
    "mixLabSaveDialogNameHint",
    "mixLabSaveConfirm",
    "mixLabSaveCancel",
    "mixLabSaveSuccess",
    "mixLabSaveFailed",
    "mixLabSaveRetry",
    "mixLabErrorTitle",
    "mixLabErrorBody",
    "mixLabErrorRetry",
    "mixLabEmptyArtists"
)

internal val mixLabKeys: Set<String> = mixLabKeyOrder.toSet()

private fun mixlab(vararg values: String): Map<String, String> {
    require(values.size == mixLabKeyOrder.size) { "Mix Lab bundle has ${values.size} values" }
    return mixLabKeyOrder.zip(values).toMap()
}

private val mixLabBundles: Map<String, Map<String, String>> = mapOf(
    "en" to mixlab(
        "Mix Lab", "Build a mix from what you actually listen to", "Tune your mix",
        "Familiarity", "Favorites", "Discovery",
        "Recency", "Classics", "New",
        "Duration", "Short", "Medium", "Long", "Any",
        "Number of tracks",
        "Genres", "Only from the genres you pick",
        "Artists", "Only from the artists you pick",
        "Mood", "Based on your real genre and mood tags",
        "Presets", "Chill", "Workout", "Late Night", "Rediscover", "Fresh Finds",
        "Generate", "Regenerate", "Tune parameters",
        "Building your mix…",
        "Preview", "{count} tracks", "{count} artists", "{percent}% familiar", "{percent}% discovery",
        "Play", "Shuffle", "Add to queue", "Save as playlist",
        "Save mix as playlist", "Playlist name", "Save", "Cancel",
        "Saved to your playlists", "Couldn't save the playlist", "Retry",
        "Not enough songs for this mix", "Try widening your genres, artists or mood, or lower the track count.", "Retry",
        "Pick fewer artists, or none, to get enough songs"
    ),
    "it" to mixlab(
        "Mix Lab", "Crea un mix da quello che ascolti davvero", "Personalizza il tuo mix",
        "Familiarità", "Preferiti", "Scoperta",
        "Recency", "Classici", "Novità",
        "Durata", "Brevi", "Medi", "Lunghi", "Qualsiasi",
        "Numero di brani",
        "Generi", "Solo dai generi che scegli",
        "Artisti", "Solo dagli artisti che scegli",
        "Mood", "Basato sui tuoi generi e tag reali",
        "Preset", "Relax", "Allenamento", "Notte", "Riscoperta", "Nuove scoperte",
        "Genera", "Rigenera", "Modifica parametri",
        "Creazione del mix…",
        "Anteprima", "{count} brani", "{count} artisti", "{percent}% familiari", "{percent}% scoperta",
        "Riproduci", "Mescola", "Aggiungi alla coda", "Salva come playlist",
        "Salva il mix come playlist", "Nome playlist", "Salva", "Annulla",
        "Salvata tra le tue playlist", "Impossibile salvare la playlist", "Riprova",
        "Non ci sono abbastanza brani per questo mix", "Prova ad ampliare generi, artisti o mood, oppure riduci il numero di brani.", "Riprova",
        "Scegli meno artisti, o nessuno, per avere brani a sufficienza"
    )
)

internal fun mixLabLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(mixLabBundles, code)
