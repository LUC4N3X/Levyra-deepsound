package com.luc4n3x.levyra.ui.i18n

internal val jamQrKeys = setOf("jamScanQr", "jamScanToJoin", "jamCopyInvite", "jamInviteCopied", "jamScannerUnavailable", "jamQrUnavailable")

private fun jamQr(scan: String, join: String, copy: String, copied: String, unavailable: String, qrUnavailable: String) =
    listOf(scan, join, copy, copied, unavailable, qrUnavailable).let { jamQrKeys.zip(it).toMap() }

private val jamQrBundles = mapOf(
    "en" to jamQr("Scan QR code", "Scan to join", "Copy invite", "Invite copied", "Camera unavailable. Allow camera access in Settings or paste an invitation to join.", "QR code unavailable. Copy or share the invitation instead."),
    "it" to jamQr("Scansiona codice QR", "Scansiona per partecipare", "Copia invito", "Invito copiato", "Fotocamera non disponibile. Consenti l’accesso nelle Impostazioni o incolla un invito per partecipare.", "Codice QR non disponibile. Copia o condividi l’invito."),
    "es" to jamQr("Escanear código QR", "Escanea para unirte", "Copiar invitación", "Invitación copiada", "Cámara no disponible. Permite el acceso en Ajustes o pega una invitación.", "Código QR no disponible. Copia o comparte la invitación."),
    "fr" to jamQr("Scanner le code QR", "Scannez pour rejoindre", "Copier l’invitation", "Invitation copiée", "Caméra indisponible. Autorisez l’accès dans les paramètres ou collez une invitation.", "Code QR indisponible. Copiez ou partagez l’invitation."),
    "de" to jamQr("QR-Code scannen", "Scannen und beitreten", "Einladung kopieren", "Einladung kopiert", "Kamera nicht verfügbar. Erlaube den Zugriff in den Einstellungen oder füge eine Einladung ein.", "QR-Code nicht verfügbar. Kopiere oder teile die Einladung.")
)

internal fun jamQrLocalizationEntries(code: String): Map<String, String> = localizedBundleOrEnglish(jamQrBundles, code)

internal val LevyraStrings.jamScanQr: String get() = value("jamScanQr")
internal val LevyraStrings.jamScanToJoin: String get() = value("jamScanToJoin")
internal val LevyraStrings.jamCopyInvite: String get() = value("jamCopyInvite")
internal val LevyraStrings.jamInviteCopied: String get() = value("jamInviteCopied")
internal val LevyraStrings.jamScannerUnavailable: String get() = value("jamScannerUnavailable")
internal val LevyraStrings.jamQrUnavailable: String get() = value("jamQrUnavailable")
