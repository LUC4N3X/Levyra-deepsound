package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.feature.scrobbling.ListenBrainzPlaylistKind

internal data class ListenBrainzCopy(
    val title: String,
    val subtitle: String,
    val refresh: String,
    val import: String,
    val empty: String,
    val topWeek: String,
    val topMonth: String,
    val kindTop: String,
    val kindCreatedFor: String,
    val kindOwn: String,
    val nothingToImport: String
) {
    fun kind(kind: ListenBrainzPlaylistKind): String = when (kind) {
        ListenBrainzPlaylistKind.TopWeek, ListenBrainzPlaylistKind.TopMonth -> kindTop
        ListenBrainzPlaylistKind.CreatedFor -> kindCreatedFor
        ListenBrainzPlaylistKind.Own -> kindOwn
    }
}

internal fun listenBrainzCopy(code: String): ListenBrainzCopy = when (code.substringBefore('-')) {
    "it" -> ListenBrainzCopy(
        title = "Le tue playlist ListenBrainz",
        subtitle = "Generate dai tuoi ascolti. Tocca per importarle in Levyra.",
        refresh = "Aggiorna",
        import = "Importa in Levyra",
        empty = "Ancora nessuna playlist: continua ad ascoltare e ListenBrainz le genererà.",
        topWeek = "I più ascoltati della settimana",
        topMonth = "I più ascoltati del mese",
        kindTop = "Dai tuoi ascolti su ListenBrainz",
        kindCreatedFor = "Creata per te da ListenBrainz",
        kindOwn = "La tua playlist ListenBrainz",
        nothingToImport = "Questa playlist ListenBrainz non contiene brani da importare"
    )
    "ru" -> ListenBrainzCopy(
        title = "Ваши плейлисты ListenBrainz",
        subtitle = "Созданы по вашим прослушиваниям. Нажмите, чтобы импортировать в Levyra.",
        refresh = "Обновить",
        import = "Импортировать в Levyra",
        empty = "Плейлистов пока нет: слушайте дальше, и ListenBrainz их создаст.",
        topWeek = "Самое прослушиваемое за неделю",
        topMonth = "Самое прослушиваемое за месяц",
        kindTop = "По вашим прослушиваниям в ListenBrainz",
        kindCreatedFor = "Создан для вас ListenBrainz",
        kindOwn = "Ваш плейлист ListenBrainz",
        nothingToImport = "В этом плейлисте ListenBrainz нет треков для импорта"
    )
    else -> ListenBrainzCopy(
        title = "Your ListenBrainz playlists",
        subtitle = "Generated from your listening. Tap one to import it into Levyra.",
        refresh = "Refresh",
        import = "Import into Levyra",
        empty = "No playlists yet: keep listening and ListenBrainz will generate them.",
        topWeek = "Most played this week",
        topMonth = "Most played this month",
        kindTop = "From your ListenBrainz listening",
        kindCreatedFor = "Made for you by ListenBrainz",
        kindOwn = "Your ListenBrainz playlist",
        nothingToImport = "This ListenBrainz playlist has no tracks to import"
    )
}
