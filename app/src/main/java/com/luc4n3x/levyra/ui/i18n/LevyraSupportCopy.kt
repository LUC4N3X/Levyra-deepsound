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
    pasteOnGithub = "The complete report was copied. Paste it into Logs or error details on GitHub, complete the required fields and review before submitting.",
    githubReady = "Report copied. Review the GitHub form and complete the required fields before submitting.",
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
        pasteOnGithub = "Il report completo è stato copiato. Incollalo in Logs or error details su GitHub, completa i campi obbligatori e controlla prima di pubblicare.",
        githubReady = "Report copiato. Controlla il modulo GitHub e completa i campi obbligatori prima di pubblicare.",
        focus = "Player Focus",
        exitFocus = "Esci da Player Focus",
        focusSubtitle = "Solo la tua musica e i controlli essenziali"
    )
    else -> englishSupportCopy
}
