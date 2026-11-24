package com.luc4n3x.levyra.ui.i18n

internal data class LastFmCopy(
    val title: String,
    val subtitle: String,
    val refresh: String,
    val import: String,
    val empty: String,
    val loved: String,
    val topWeek: String,
    val topMonth: String,
    val topAllTime: String,
    val kindSite: String,
    val kindLoved: String,
    val kindTop: String,
    val nothingToImport: String
)

internal fun lastFmCopy(code: String): LastFmCopy = when (code.substringBefore('-')) {
    "it" -> LastFmCopy(
        title = "Le tue playlist Last.fm",
        subtitle = "Le playlist del tuo profilo Last.fm e i tuoi ascolti. Tocca per importarle in Levyra.",
        refresh = "Aggiorna",
        import = "Importa in Levyra",
        empty = "Ancora niente qui: ascolta o aggiungi brani ai preferiti su Last.fm.",
        loved = "Brani amati",
        topWeek = "I più ascoltati della settimana",
        topMonth = "I più ascoltati del mese",
        topAllTime = "I più ascoltati di sempre",
        kindSite = "La tua playlist su Last.fm",
        kindLoved = "I tuoi preferiti su Last.fm",
        kindTop = "Dai tuoi scrobble su Last.fm",
        nothingToImport = "Questa playlist Last.fm non contiene brani da importare"
    )
    "ru" -> LastFmCopy(
        title = "Ваши плейлисты Last.fm",
        subtitle = "Из вашего профиля Last.fm. Нажмите, чтобы импортировать в Levyra.",
        refresh = "Обновить",
        import = "Импортировать в Levyra",
        empty = "Пока пусто: слушайте музыку или отмечайте любимые треки на Last.fm.",
        loved = "Любимые треки",
        topWeek = "Самое прослушиваемое за неделю",
        topMonth = "Самое прослушиваемое за месяц",
        topAllTime = "Самое прослушиваемое за всё время",
        kindSite = "Ваш плейлист на Last.fm",
        kindLoved = "Ваши любимые треки на Last.fm",
        kindTop = "По вашим скробблам на Last.fm",
        nothingToImport = "В этом плейлисте Last.fm нет треков для импорта"
    )
    else -> LastFmCopy(
        title = "Your Last.fm playlists",
        subtitle = "From your Last.fm profile. Tap one to import it into Levyra.",
        refresh = "Refresh",
        import = "Import into Levyra",
        empty = "Nothing here yet: listen or love tracks on Last.fm.",
        loved = "Loved tracks",
        topWeek = "Most played this week",
        topMonth = "Most played this month",
        topAllTime = "Most played of all time",
        kindSite = "Your Last.fm playlist",
        kindLoved = "Your loved tracks on Last.fm",
        kindTop = "From your Last.fm scrobbles",
        nothingToImport = "This Last.fm playlist has no tracks to import"
    )
}
