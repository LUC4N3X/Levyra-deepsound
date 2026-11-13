package com.luc4n3x.levyra.ui.i18n

internal data class LevyraSupportCopy(
    val reports: String,
    val reportsSubtitle: String,
    val unexpectedClose: String,
    val privacy: String,
    val github: String,
    val copy: String,
    val share: String,
    val close: String,
    val delete: String,
    val empty: String,
    val failed: String,
    val copied: String,
    val pasteOnGithub: String,
    val githubReady: String,
    val keepLogs: String,
    val focus: String,
    val exitFocus: String,
    val focusSubtitle: String
)

private val englishSupportCopy = LevyraSupportCopy(
    reports = "Crash reports",
    reportsSubtitle = "Review, share or delete reports saved on this device",
    unexpectedClose = "Levyra closed unexpectedly. We prepared a report that can help resolve the problem.",
    privacy = "Nothing is sent automatically. Review the report before sharing. Up to five reports are saved on this device.",
    github = "Report on GitHub",
    copy = "Copy report",
    share = "Share logs",
    close = "Close",
    delete = "Delete report",
    empty = "No saved crash reports",
    failed = "The report could not be opened or shared. Please try again.",
    copied = "Report copied",
    pasteOnGithub = "This preview is shortened to fit the GitHub link. Replace the issue description with the complete copied report before submitting. DO NOT DELETE THE LOGS.",
    githubReady = "Crash logs are included in the GitHub report. DO NOT DELETE THEM. Review the report before submitting.",
    keepLogs = "DO NOT DELETE THESE LOGS. They are needed to diagnose the crash. Remove any remaining personal information before publishing.",
    focus = "Player Focus",
    exitFocus = "Exit Player Focus",
    focusSubtitle = "Just your music and the essential controls"
)

internal fun supportCopy(code: String): LevyraSupportCopy = when (code.substringBefore('-')) {
    "it" -> LevyraSupportCopy(
        reports = "Report dei crash",
        reportsSubtitle = "Consulta, condividi o elimina i report salvati su questo dispositivo",
        unexpectedClose = "Levyra si è chiusa inaspettatamente. Abbiamo preparato un report che può aiutare a risolvere il problema.",
        privacy = "Nessun invio automatico. Controlla il report prima di condividerlo. Sul dispositivo vengono conservati al massimo cinque report.",
        github = "Report on GitHub",
        copy = "Copia report",
        share = "Condividi log",
        close = "Chiudi",
        delete = "Elimina report",
        empty = "Nessun report di crash salvato",
        failed = "Impossibile aprire o condividere il report. Riprova.",
        copied = "Report copiato",
        pasteOnGithub = "Anteprima abbreviata per il limite del link GitHub. Prima di pubblicare, sostituisci la descrizione con il report completo copiato. NON CANCELLARE I LOG.",
        githubReady = "I log del crash sono inclusi nel report GitHub. NON CANCELLARLI. Controlla il report prima di pubblicare.",
        keepLogs = "Log necessari per diagnosticare il crash: NON CANCELLARLI. Rimuovi eventuali dati personali rimasti prima di pubblicare.",
        focus = "Player Focus",
        exitFocus = "Esci da Player Focus",
        focusSubtitle = "Solo la tua musica e i controlli essenziali"
    )
    else -> englishSupportCopy
}
