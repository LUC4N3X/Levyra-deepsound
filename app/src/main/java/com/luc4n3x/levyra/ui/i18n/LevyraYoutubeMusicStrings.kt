package com.luc4n3x.levyra.ui.i18n

internal data class YoutubeMusicCopy(
    val connect: String, val introduction: String, val library: String, val accounts: String,
    val addAccount: String, val logout: String, val logoutAll: String, val selected: String,
    val playlists: String, val likes: String, val albums: String, val artists: String,
    val refresh: String, val more: String, val import: String, val importDescription: String,
    val imported: String, val empty: String, val networkError: String, val signInError: String,
    val protocolError: String, val storageError: String, val limitError: String,
    val retry: String, val back: String, val login: String, val checking: String, val play: String,
    val importSong: String, val disconnectTitle: String, val disconnectMessage: String,
    val cancel: String, val optional: String, val reconnect: String,
    val upToDate: String, val playlistOnly: String
)

internal fun youtubeMusicCopy(code: String): YoutubeMusicCopy = when (code.substringBefore('-')) {
    "it" -> YoutubeMusicCopy(
        "Collega YouTube Music", "Ritrova le tue playlist e la musica che hai salvato.", "Libreria YouTube Music", "Account",
        "Aggiungi account", "Scollega", "Scollega tutti", "In uso",
        "Playlist", "Mi piace", "Album", "Artisti", "Aggiorna", "Carica altri", "Importa e aggiorna",
        "Aggiunge i brani mancanti in Levyra senza rimuovere la tua musica. Gli aggiornamenti si avviano da qui.",
        "Musica importata in Levyra", "Non ci sono contenuti da mostrare", "Connessione non riuscita. Riprova tra poco.",
        "Accedi di nuovo per collegare questo account.", "YouTube Music ha restituito dati non leggibili. Riprova.",
        "Non è stato possibile salvare o leggere la sessione.", "La libreria supera il limite di 10.000 elementi. Importa una playlist alla volta.",
        "Riprova", "Indietro", "Accedi a YouTube Music", "Verifica dell’account…", "Riproduci", "Importa brano",
        "Scollegare l’account?", "La sessione verrà rimossa da Levyra. La musica già importata resterà nella tua libreria.",
        "Annulla", "Collegamento facoltativo", "Accedi di nuovo",
        "La playlist è già aggiornata", "Playlist salvata. Non è stato possibile aggiornare i preferiti: riprova l’importazione."
    )
    "ru" -> YoutubeMusicCopy(
        "Подключить YouTube Music", "Ваши плейлисты и сохранённая музыка.", "Библиотека YouTube Music", "Аккаунты",
        "Добавить аккаунт", "Отключить", "Отключить все", "Выбран",
        "Плейлисты", "Понравившиеся", "Альбомы", "Исполнители", "Обновить", "Загрузить ещё", "Импорт и обновление",
        "Добавляет недостающие треки в Levyra, не удаляя вашу музыку. Обновление запускается здесь.",
        "Музыка импортирована в Levyra", "Здесь пока ничего нет", "Не удалось подключиться. Попробуйте позже.",
        "Войдите снова, чтобы подключить аккаунт.", "Не удалось прочитать ответ YouTube Music. Попробуйте снова.",
        "Не удалось сохранить или прочитать сессию.", "Библиотека превышает лимит в 10 000 элементов. Импортируйте плейлисты отдельно.",
        "Повторить", "Назад", "Вход в YouTube Music", "Проверка аккаунта…", "Воспроизвести", "Импорт трека",
        "Отключить аккаунт?", "Сессия будет удалена из Levyra. Импортированная музыка останется в библиотеке.",
        "Отмена", "Подключение необязательно", "Войти снова",
        "Плейлист уже обновлён", "Плейлист сохранён. Не удалось обновить избранное: повторите импорт."
    )
    else -> YoutubeMusicCopy(
        "Connect YouTube Music", "Find your playlists and the music you saved.", "YouTube Music library", "Accounts",
        "Add account", "Disconnect", "Disconnect all", "Selected",
        "Playlists", "Liked songs", "Albums", "Artists", "Refresh", "Load more", "Import and update",
        "Adds missing songs to Levyra without removing your music. Start updates here.",
        "Music imported into Levyra", "There is nothing here yet", "Could not connect. Try again shortly.",
        "Sign in again to connect this account.", "Could not read YouTube Music’s response. Try again.",
        "Could not save or read your session.", "The library exceeds the 10,000 item limit. Import playlists separately.",
        "Retry", "Back", "Sign in to YouTube Music", "Checking your account…", "Play", "Import song",
        "Disconnect account?", "Your session will be removed from Levyra. Imported music will stay in your library.",
        "Cancel", "Optional connection", "Sign in again",
        "The playlist is already up to date", "Playlist saved. Favorites could not be updated: retry the import."
    )
}
