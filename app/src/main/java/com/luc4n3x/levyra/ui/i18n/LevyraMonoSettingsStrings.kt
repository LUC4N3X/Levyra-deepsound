package com.luc4n3x.levyra.ui.i18n

internal data class MonoSettingsCopy(
    val jamHeadline: String,
    val jamLiveSession: String,
    val jamInvite: String,
    val jamInviteHint: String,
    val jamRadar: String
)

internal object LevyraMonoSettingsStrings {
    private val copies = mapOf(
        "en" to MonoSettingsCopy(
            jamHeadline = "Listen together",
            jamLiveSession = "Live session",
            jamInvite = "Invite",
            jamInviteHint = "Share this code with anyone on the same network.",
            jamRadar = "%1\$d people in the session"
        ),
        "it" to MonoSettingsCopy(
            jamHeadline = "Ascolta insieme",
            jamLiveSession = "Sessione live",
            jamInvite = "Invito",
            jamInviteHint = "Condividi questo codice con chi è sulla stessa rete.",
            jamRadar = "%1\$d persone nella sessione"
        )
    )

    fun forCode(code: String): MonoSettingsCopy = localizedValueOrEnglish(copies, code)
}
