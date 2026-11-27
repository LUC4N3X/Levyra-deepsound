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
        ),
        "ur" to MonoSettingsCopy(
            jamHeadline = "ایک ساتھ سنیں۔",
            jamLiveSession = "لائیو سیشن",
            jamInvite = "دعوت دیں۔",
            jamInviteHint = "اسی نیٹ ورک پر کسی کے ساتھ بھی اس کوڈ کا اشتراک کریں۔",
            jamRadar = "سیشن میں %1\$d لوگ"
        ),
        "ta" to MonoSettingsCopy(
            jamHeadline = "ஒன்றாகக் கேளுங்கள்",
            jamLiveSession = "நேரடி அமர்வு",
            jamInvite = "அழைக்கவும்",
            jamInviteHint = "ஒரே நெட்வொர்க்கில் உள்ள எவருடனும் இந்தக் குறியீட்டைப் பகிரவும்.",
            jamRadar = "அமர்வில் %1\$d நபர்கள்"
        ),
        "te" to MonoSettingsCopy(
            jamHeadline = "కలిసి వినండి",
            jamLiveSession = "ప్రత్యక్ష సెషన్",
            jamInvite = "ఆహ్వానించండి",
            jamInviteHint = "ఈ కోడ్‌ని ఒకే నెట్‌వర్క్‌లో ఎవరితోనైనా షేర్ చేయండి.",
            jamRadar = "సెషన్‌లో %1\$d వ్యక్తులు"
        ),
        "mr" to MonoSettingsCopy(
            jamHeadline = "एकत्र ऐका",
            jamLiveSession = "थेट सत्र",
            jamInvite = "आमंत्रित करा",
            jamInviteHint = "हा कोड समान नेटवर्कवरील कोणाशीही शेअर करा.",
            jamRadar = "सत्रातील %1\$d लोक"
        ),
        "gu" to MonoSettingsCopy(
            jamHeadline = "સાથે સાંભળો",
            jamLiveSession = "જીવંત સત્ર",
            jamInvite = "આમંત્રણ આપો",
            jamInviteHint = "સમાન નેટવર્ક પર કોઈપણ સાથે આ કોડ શેર કરો.",
            jamRadar = "સત્રમાં %1\$d લોકો"
        ),
        "pt-BR" to MonoSettingsCopy(
            jamHeadline = "Ouçam juntos",
            jamLiveSession = "Sessão ao vivo",
            jamInvite = "Convidar",
            jamInviteHint = "Compartilhe este código com qualquer pessoa na mesma rede.",
            jamRadar = "%1\$d pessoas na sessão"
        )
    )

    fun forCode(code: String): MonoSettingsCopy = localizedValueOrEnglish(copies, code)
}
