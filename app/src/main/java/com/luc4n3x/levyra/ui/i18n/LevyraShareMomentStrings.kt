package com.luc4n3x.levyra.ui.i18n

internal const val SHARE_MOMENT_TIME_TOKEN = "{time}"

internal data class ShareMomentCopy(
    val action: String,
    val fromTemplate: String
)

internal fun LevyraStrings.shareMomentCopy(): ShareMomentCopy = when (code) {
    "it" -> ShareMomentCopy("Condividi da qui", "da {time}")
    "es" -> ShareMomentCopy("Compartir desde aquí", "desde {time}")
    "fr" -> ShareMomentCopy("Partager à partir d'ici", "à partir de {time}")
    "de" -> ShareMomentCopy("Ab hier teilen", "ab {time}")
    "pt" -> ShareMomentCopy("Partilhar a partir daqui", "a partir de {time}")
    "nl" -> ShareMomentCopy("Delen vanaf hier", "vanaf {time}")
    "pl" -> ShareMomentCopy("Udostępnij od tego miejsca", "od {time}")
    "ro" -> ShareMomentCopy("Distribuie de aici", "de la {time}")
    "el" -> ShareMomentCopy("Κοινοποίηση από εδώ", "από {time}")
    "sv" -> ShareMomentCopy("Dela härifrån", "från {time}")
    "da" -> ShareMomentCopy("Del herfra", "fra {time}")
    "cs" -> ShareMomentCopy("Sdílet odsud", "od {time}")
    "sk" -> ShareMomentCopy("Zdieľať odtiaľto", "od {time}")
    "hr" -> ShareMomentCopy("Podijeli odavde", "od {time}")
    "sr" -> ShareMomentCopy("Подели одавде", "од {time}")
    "sq" -> ShareMomentCopy("Ndaj nga këtu", "nga {time}")
    "bg" -> ShareMomentCopy("Споделяне от тук", "от {time}")
    "hu" -> ShareMomentCopy("Megosztás innentől", "kezdés: {time}")
    "fi" -> ShareMomentCopy("Jaa tästä kohdasta", "kohdasta {time}")
    "et" -> ShareMomentCopy("Jaga siit alates", "alates {time}")
    "nb" -> ShareMomentCopy("Del herfra", "fra {time}")
    "ca" -> ShareMomentCopy("Comparteix des d'aquí", "des de {time}")
    "uk" -> ShareMomentCopy("Поділитися звідси", "з {time}")
    "ru" -> ShareMomentCopy("Поделиться с этого места", "с {time}")
    "kk" -> ShareMomentCopy("Осы жерден бөлісу", "басталуы: {time}")
    "tr" -> ShareMomentCopy("Buradan paylaş", "başlangıç: {time}")
    "ar" -> ShareMomentCopy("مشاركة من هنا", "من {time}")
    "fa" -> ShareMomentCopy("اشتراک‌گذاری از اینجا", "از {time}")
    "zh" -> ShareMomentCopy("从此处分享", "从 {time} 开始")
    "zh-Hant" -> ShareMomentCopy("從此處分享", "從 {time} 開始")
    "ja" -> ShareMomentCopy("ここから共有", "{time} から")
    "ko" -> ShareMomentCopy("여기서부터 공유", "{time}부터")
    "hi" -> ShareMomentCopy("यहाँ से साझा करें", "{time} से")
    "bn" -> ShareMomentCopy("এখান থেকে শেয়ার করুন", "{time} থেকে")
    "id" -> ShareMomentCopy("Bagikan dari sini", "dari {time}")
    "ms" -> ShareMomentCopy("Kongsi dari sini", "dari {time}")
    "vi" -> ShareMomentCopy("Chia sẻ từ đây", "từ {time}")
    "th" -> ShareMomentCopy("แชร์จากตรงนี้", "ตั้งแต่ {time}")
    "fil" -> ShareMomentCopy("Ibahagi mula rito", "mula {time}")
    "he" -> ShareMomentCopy("שיתוף מכאן", "החל מ-{time}")
    else -> ShareMomentCopy("Share from here", "from {time}")
}
