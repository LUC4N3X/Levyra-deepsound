package com.luc4n3x.levyra.ui.i18n

internal data class QueueSectionCopy(
    val played: String,
    val upNext: String
)

internal fun LevyraStrings.queueSectionCopy(): QueueSectionCopy = when (code) {
    "it" -> QueueSectionCopy("Riprodotti", "Prossimi")
    "es" -> QueueSectionCopy("Reproducidos", "A continuación")
    "fr" -> QueueSectionCopy("Déjà lus", "À suivre")
    "de" -> QueueSectionCopy("Gespielt", "Als Nächstes")
    "pt" -> QueueSectionCopy("Reproduzidas", "A seguir")
    "nl" -> QueueSectionCopy("Afgespeeld", "Hierna")
    "pl" -> QueueSectionCopy("Odtworzone", "Następne")
    "ro" -> QueueSectionCopy("Redate", "Urmează")
    "el" -> QueueSectionCopy("Αναπαράχθηκαν", "Επόμενα")
    "sv" -> QueueSectionCopy("Spelade", "Nästa")
    "da" -> QueueSectionCopy("Afspillet", "Næste")
    "cs" -> QueueSectionCopy("Přehráno", "Následuje")
    "sk" -> QueueSectionCopy("Prehrané", "Nasleduje")
    "hr" -> QueueSectionCopy("Reproducirano", "Sljedeće")
    "bg" -> QueueSectionCopy("Прослушани", "Следва")
    "hu" -> QueueSectionCopy("Lejátszva", "Következik")
    "fi" -> QueueSectionCopy("Toistetut", "Seuraavaksi")
        "et" -> QueueSectionCopy("Esitatud", "Järgmisena")
    "nb" -> QueueSectionCopy("Spilt", "Neste")
    "ca" -> QueueSectionCopy("Reproduïts", "A continuació")
    "uk" -> QueueSectionCopy("Відтворено", "Далі")
    "ru" -> QueueSectionCopy("Прослушано", "Далее")
    "tr" -> QueueSectionCopy("Çalınanlar", "Sıradaki")
    "ar" -> QueueSectionCopy("تم تشغيلها", "التالي")
    "fa" -> QueueSectionCopy("پخش‌شده", "بعدی")
    "zh" -> QueueSectionCopy("已播放", "接下来")
    "zh-Hant" -> QueueSectionCopy("已播放", "接下來")
    "ja" -> QueueSectionCopy("再生済み", "次に再生")
    "ko" -> QueueSectionCopy("재생됨", "다음")
    "hi" -> QueueSectionCopy("चल चुके", "अगला")
    "id" -> QueueSectionCopy("Sudah diputar", "Berikutnya")
    "ms" -> QueueSectionCopy("Telah dimainkan", "Seterusnya")
    "vi" -> QueueSectionCopy("Đã phát", "Tiếp theo")
    "th" -> QueueSectionCopy("เล่นแล้ว", "ถัดไป")
    "fil" -> QueueSectionCopy("Napatugtog", "Susunod")
    "he" -> QueueSectionCopy("נוגנו", "הבאים")
    else -> QueueSectionCopy("Played", "Up next")
}

internal fun LevyraStrings.localLibraryRecentFilterLabel(): String = when (code) {
    "it" -> "RECENTI"
    "es" -> "RECIENTES"
    "fr" -> "RÉCENTS"
    "de" -> "NEU"
    "pt" -> "RECENTES"
    "nl" -> "RECENT"
    "pl" -> "OSTATNIE"
    "ro" -> "RECENTE"
    "el" -> "ΠΡΟΣΦΑΤΑ"
    "sv" -> "SENASTE"
    "da" -> "SENESTE"
    "cs" -> "NEDÁVNÉ"
    "sk" -> "NEDÁVNE"
    "hr" -> "NEDAVNO"
    "bg" -> "СКОРОШНИ"
    "hu" -> "LEGUTÓBBI"
    "fi" -> "VIIMEISIMMÄT"
        "et" -> "VIIMASED"
    "nb" -> "NYLIGE"
    "ca" -> "RECENTS"
    "uk" -> "НЕДАВНІ"
    "ru" -> "НЕДАВНИЕ"
    "tr" -> "YENİ"
    "ar" -> "الأحدث"
    "fa" -> "اخیر"
    "zh", "zh-Hant" -> "最近"
    "ja" -> "最近"
    "ko" -> "최근"
    "hi" -> "हाल के"
    "id" -> "TERBARU"
    "ms" -> "TERKINI"
    "vi" -> "GẦN ĐÂY"
    "th" -> "ล่าสุด"
    "fil" -> "BAGO"
    "he" -> "אחרונים"
    else -> "RECENT"
}
