package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.data.youtubemusic.PlaylistSyncErrorKind
import com.luc4n3x.levyra.data.youtubemusic.PlaylistSyncSummary
import java.text.DateFormat
import java.util.Date
import java.util.Locale

internal data class PlaylistSyncCopy(
    val action: String,
    val syncWithLevyra: String,
    val syncNow: String,
    val syncing: String,
    val syncedBadge: String,
    val sheetTitle: String,
    val subtitle: (tracks: Int, ready: Int) -> String,
    val emptySubtitle: String,
    val twoWayNote: String,
    val signedOutTitle: String,
    val signedOutMessage: String,
    val signInButton: String,
    val accountConnected: (String) -> String,
    val createPrivateTitle: String,
    val createPrivateSubtitle: String,
    val existingSection: String,
    val loadingRemote: String,
    val emptyRemote: String,
    val retry: String,
    val autoSyncTitle: String,
    val autoSyncSubtitle: String,
    val linkedHeader: String,
    val unmatchedNote: (Int) -> String,
    val openRemote: String,
    val unlinkButton: String,
    val unlinkNote: String,
    val errorTitle: String,
    val errorMessage: (PlaylistSyncErrorKind) -> String,
    val summaryText: (PlaylistSyncSummary) -> String,
    val lastSyncedLabel: (Long) -> String,
    val done: String
)

internal fun playlistSyncCopy(code: String): PlaylistSyncCopy {
    val language = code.substringBefore('-')
    val locale = Locale.forLanguageTag(code.ifBlank { "en" })
    return when (language) {
        "it" -> PlaylistSyncCopy(
            action = "Sincronizza con YouTube Music",
            syncWithLevyra = "Sincronizza con Levyra",
            syncNow = "Sincronizza ora",
            syncing = "Sincronizzazione…",
            syncedBadge = "Sincronizzata",
            sheetTitle = "Sincronizzazione YouTube Music",
            subtitle = { tracks, ready -> "$tracks brani · $ready pronti su YouTube Music" },
            emptySubtitle = "Anche le playlist vuote possono sincronizzarsi in entrambe le direzioni",
            twoWayNote = "La sincronizzazione bidirezionale allinea aggiunte, rimozioni e cambi di nome tra Levyra e YouTube Music.",
            signedOutTitle = "YouTube Music non è collegato",
            signedOutMessage = "Accedi a YouTube Music per collegare questa playlist e mantenerla sincronizzata in entrambe le direzioni.",
            signInButton = "Accedi a YouTube Music",
            accountConnected = { "Account attivo: $it" },
            createPrivateTitle = "Crea playlist privata su YouTube Music",
            createPrivateSubtitle = "Crea una nuova playlist privata sul tuo account e mantienila sincronizzata",
            existingSection = "Collega playlist esistente",
            loadingRemote = "Caricamento delle tue playlist YouTube Music…",
            emptyRemote = "Nessuna playlist modificabile trovata su questo account YouTube Music.",
            retry = "Riprova",
            autoSyncTitle = "Sincronizzazione automatica sulle modifiche",
            autoSyncSubtitle = "Invia automaticamente a YouTube Music le modifiche fatte in Levyra",
            linkedHeader = "Playlist YouTube Music collegata",
            unmatchedNote = { "$it brani locali o senza ID YouTube restano in Levyra e non vengono rimossi." },
            openRemote = "Apri in YouTube Music",
            unlinkButton = "Scollega playlist",
            unlinkNote = "Lo scollegamento interrompe solo la sincronizzazione: entrambe le copie restano intatte.",
            errorTitle = "Sincronizzazione non riuscita",
            errorMessage = { kind ->
                when (kind) {
                    PlaylistSyncErrorKind.SIGN_IN -> "Sessione YouTube Music scaduta. Accedi di nuovo."
                    PlaylistSyncErrorKind.ACCOUNT_MISMATCH -> "Questa playlist è collegata a un altro account YouTube Music."
                    PlaylistSyncErrorKind.NOT_FOUND -> "Playlist remota non trovata su YouTube Music. Puoi scollegarla e ricollegarla."
                    PlaylistSyncErrorKind.NETWORK -> "Connessione non riuscita. Controlla la rete e riprova."
                    PlaylistSyncErrorKind.SERVICE -> "YouTube Music non ha confermato la sincronizzazione. Riprova."
                }
            },
            summaryText = { summary ->
                val parts = buildList {
                    if (summary.addedLocally > 0) add("+${summary.addedLocally} in Levyra")
                    if (summary.removedLocally > 0) add("-${summary.removedLocally} in Levyra")
                    if (summary.addedRemotely > 0) add("+${summary.addedRemotely} su YouTube Music")
                    if (summary.removedRemotely > 0) add("-${summary.removedRemotely} su YouTube Music")
                    if (summary.renamedLocally) add("Titolo aggiornato in Levyra")
                    if (summary.renamedRemotely) add("Titolo aggiornato su YouTube Music")
                }
                if (parts.isEmpty()) "Già allineata" else parts.joinToString(" · ")
            },
            lastSyncedLabel = { timestampMs ->
                if (timestampMs <= 0L) "Mai sincronizzata"
                else "Ultima sincronizzazione: ${formatSyncTimestamp(timestampMs, locale)}"
            },
            done = "Fine"
        )

        "ru" -> PlaylistSyncCopy(
            action = "Синхронизация с YouTube Music",
            syncWithLevyra = "Синхронизировать с Levyra",
            syncNow = "Синхронизировать",
            syncing = "Синхронизация…",
            syncedBadge = "Синхронизировано",
            sheetTitle = "Синхронизация YouTube Music",
            subtitle = { tracks, ready -> "Треков: $tracks · готовы для YouTube Music: $ready" },
            emptySubtitle = "Пустые плейлисты тоже поддерживают двустороннюю синхронизацию",
            twoWayNote = "Двусторонняя синхронизация переносит добавления, удаления и переименования между Levyra и YouTube Music.",
            signedOutTitle = "YouTube Music не подключён",
            signedOutMessage = "Войдите в YouTube Music, чтобы связать этот плейлист и синхронизировать изменения в обе стороны.",
            signInButton = "Войти в YouTube Music",
            accountConnected = { "Аккаунт: $it" },
            createPrivateTitle = "Создать приватный плейлист в YouTube Music",
            createPrivateSubtitle = "Создать новый приватный плейлист в аккаунте и синхронизировать его",
            existingSection = "Связать существующий плейлист",
            loadingRemote = "Загрузка ваших плейлистов YouTube Music…",
            emptyRemote = "На этом аккаунте не найдено собственных плейлистов YouTube Music.",
            retry = "Повторить",
            autoSyncTitle = "Автосинхронизация при изменениях",
            autoSyncSubtitle = "Автоматически отправлять изменения из Levyra в YouTube Music",
            linkedHeader = "Связанный плейлист YouTube Music",
            unmatchedNote = { "Локальные треки без идентификатора YouTube ($it) остаются в Levyra и не удаляются." },
            openRemote = "Открыть в YouTube Music",
            unlinkButton = "Отвязать плейлист",
            unlinkNote = "Отвязка отключает только синхронизацию; оба плейлиста сохраняются.",
            errorTitle = "Не удалось синхронизировать",
            errorMessage = { kind ->
                when (kind) {
                    PlaylistSyncErrorKind.SIGN_IN -> "Сессия YouTube Music истекла. Войдите снова."
                    PlaylistSyncErrorKind.ACCOUNT_MISMATCH -> "Плейлист связан с другим аккаунтом YouTube Music."
                    PlaylistSyncErrorKind.NOT_FOUND -> "Плейлист не найден в YouTube Music. Отвяжите и свяжите его снова."
                    PlaylistSyncErrorKind.NETWORK -> "Ошибка сети. Проверьте подключение и повторите."
                    PlaylistSyncErrorKind.SERVICE -> "YouTube Music не подтвердил синхронизацию. Повторите попытку."
                }
            },
            summaryText = { summary ->
                val parts = buildList {
                    if (summary.addedLocally > 0) add("+${summary.addedLocally} в Levyra")
                    if (summary.removedLocally > 0) add("-${summary.removedLocally} в Levyra")
                    if (summary.addedRemotely > 0) add("+${summary.addedRemotely} в YouTube Music")
                    if (summary.removedRemotely > 0) add("-${summary.removedRemotely} в YouTube Music")
                    if (summary.renamedLocally) add("Название обновлено в Levyra")
                    if (summary.renamedRemotely) add("Название обновлено в YouTube Music")
                }
                if (parts.isEmpty()) "Уже синхронизирован" else parts.joinToString(" · ")
            },
            lastSyncedLabel = { timestampMs ->
                if (timestampMs <= 0L) "Ещё не синхронизирован"
                else "Последняя синхронизация: ${formatSyncTimestamp(timestampMs, locale)}"
            },
            done = "Готово"
        )

        else -> PlaylistSyncCopy(
            action = "YouTube Music sync",
            syncWithLevyra = "Sync with Levyra",
            syncNow = "Sync now",
            syncing = "Syncing…",
            syncedBadge = "Synced",
            sheetTitle = "YouTube Music sync",
            subtitle = { tracks, ready -> "$tracks tracks · $ready ready for YouTube Music" },
            emptySubtitle = "Empty playlists can still sync in both directions",
            twoWayNote = "Two-way sync keeps track additions, removals, and renames aligned between Levyra and YouTube Music.",
            signedOutTitle = "YouTube Music isn’t connected",
            signedOutMessage = "Sign in to YouTube Music to link this playlist and keep changes synced in both directions.",
            signInButton = "Sign in to YouTube Music",
            accountConnected = { "Account: $it" },
            createPrivateTitle = "Create private YouTube Music playlist",
            createPrivateSubtitle = "Create a new private playlist on your account and keep it in sync",
            existingSection = "Link existing playlist",
            loadingRemote = "Loading your YouTube Music playlists…",
            emptyRemote = "No owned YouTube Music playlists found on this account.",
            retry = "Retry",
            autoSyncTitle = "Auto-sync on local edits",
            autoSyncSubtitle = "Automatically push edits made in Levyra to YouTube Music",
            linkedHeader = "Linked YouTube Music playlist",
            unmatchedNote = { "$it local-only or unmatched tracks stay in Levyra and are never removed during sync." },
            openRemote = "Open in YouTube Music",
            unlinkButton = "Unlink playlist",
            unlinkNote = "Unlinking only stops sync; both playlists stay intact.",
            errorTitle = "Sync couldn’t finish",
            errorMessage = { kind ->
                when (kind) {
                    PlaylistSyncErrorKind.SIGN_IN -> "Your YouTube Music session expired. Sign in again."
                    PlaylistSyncErrorKind.ACCOUNT_MISMATCH -> "This playlist is linked to a different YouTube Music account."
                    PlaylistSyncErrorKind.NOT_FOUND -> "The remote playlist wasn’t found on YouTube Music. You can unlink and relink it."
                    PlaylistSyncErrorKind.NETWORK -> "Couldn’t connect. Check your network and try again."
                    PlaylistSyncErrorKind.SERVICE -> "YouTube Music didn’t confirm the sync. Try again."
                }
            },
            summaryText = { summary ->
                val parts = buildList {
                    if (summary.addedLocally > 0) add("+${summary.addedLocally} in Levyra")
                    if (summary.removedLocally > 0) add("-${summary.removedLocally} in Levyra")
                    if (summary.addedRemotely > 0) add("+${summary.addedRemotely} on YouTube Music")
                    if (summary.removedRemotely > 0) add("-${summary.removedRemotely} on YouTube Music")
                    if (summary.renamedLocally) add("Renamed in Levyra")
                    if (summary.renamedRemotely) add("Renamed on YouTube Music")
                }
                if (parts.isEmpty()) "Up to date" else parts.joinToString(" · ")
            },
            lastSyncedLabel = { timestampMs ->
                if (timestampMs <= 0L) "Never synced yet"
                else "Last synced: ${formatSyncTimestamp(timestampMs, locale)}"
            },
            done = "Done"
        )
    }
}

private fun formatSyncTimestamp(timestampMs: Long, locale: Locale): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale).format(Date(timestampMs))
