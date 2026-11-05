package com.luc4n3x.levyra.ui.i18n

internal data class ExploreForYouCopy(val title: String, val subtitle: String)

private val exploreForYouCopies = mapOf(
    "en" to ExploreForYouCopy("For you", "Mixes made from your listening"),
    "it" to ExploreForYouCopy("Per te", "Mix creati sui tuoi ascolti"),
    "es" to ExploreForYouCopy("Para ti", "Mezclas creadas con lo que escuchas"),
    "fr" to ExploreForYouCopy("Pour toi", "Des mix créés à partir de tes écoutes"),
    "de" to ExploreForYouCopy("Für dich", "Mixe aus deinem Hörverhalten"),
    "pt" to ExploreForYouCopy("Para ti", "Misturas criadas a partir do que ouves"),
    "pt-BR" to ExploreForYouCopy("Para você", "Mixes criados a partir do que você ouve"),
    "nl" to ExploreForYouCopy("Voor jou", "Mixen op basis van wat je luistert"),
    "pl" to ExploreForYouCopy("Dla ciebie", "Miksy tworzone na podstawie twoich odsłuchań"),
    "ro" to ExploreForYouCopy("Pentru tine", "Mixuri create din ce asculți"),
    "el" to ExploreForYouCopy("Για σένα", "Μίξεις από όσα ακούς"),
    "sv" to ExploreForYouCopy("För dig", "Mixar skapade av ditt lyssnande"),
    "da" to ExploreForYouCopy("Til dig", "Mix skabt ud fra din lytning"),
    "cs" to ExploreForYouCopy("Pro tebe", "Mixy podle toho, co posloucháš"),
    "sk" to ExploreForYouCopy("Pre teba", "Mixy podľa toho, čo počúvaš"),
    "hr" to ExploreForYouCopy("Za tebe", "Mixevi prema onome što slušaš"),
    "sr" to ExploreForYouCopy("За тебе", "Миксеви према ономе што слушаш"),
    "sq" to ExploreForYouCopy("Për ty", "Mikse nga ajo që dëgjon"),
    "bg" to ExploreForYouCopy("За теб", "Миксове според това, което слушаш"),
    "hu" to ExploreForYouCopy("Neked", "Mixek a hallgatási szokásaid alapján"),
    "fi" to ExploreForYouCopy("Sinulle", "Kuunteluistasi tehdyt miksaukset"),
    "et" to ExploreForYouCopy("Sulle", "Sinu kuulamisest loodud miksid"),
    "nb" to ExploreForYouCopy("For deg", "Mikser laget ut fra lyttingen din"),
    "ca" to ExploreForYouCopy("Per a tu", "Mescles creades amb el que escoltes"),
    "uk" to ExploreForYouCopy("Для тебе", "Мікси на основі того, що ти слухаєш"),
    "ru" to ExploreForYouCopy("Для тебя", "Миксы на основе того, что ты слушаешь"),
    "kk" to ExploreForYouCopy("Саған арналған", "Тыңдағаныңнан жасалған микстер"),
    "tr" to ExploreForYouCopy("Senin için", "Dinlediklerinden oluşturulan mixler"),
    "ar" to ExploreForYouCopy("لك", "مزيجات مبنية على ما تستمع إليه"),
    "fa" to ExploreForYouCopy("برای تو", "میکس‌هایی بر اساس آنچه گوش می‌دهی"),
    "zh" to ExploreForYouCopy("为你推荐", "根据你的聆听创建的合辑"),
    "zh-Hant" to ExploreForYouCopy("為你推薦", "根據你的聆聽建立的合輯"),
    "ja" to ExploreForYouCopy("あなたへ", "再生履歴から作ったミックス"),
    "ko" to ExploreForYouCopy("당신을 위한", "청취 기록으로 만든 믹스"),
    "hi" to ExploreForYouCopy("आपके लिए", "आपकी सुनने की आदतों से बने मिक्स"),
    "bn" to ExploreForYouCopy("আপনার জন্য", "আপনার শোনা গান থেকে তৈরি মিক্স"),
    "id" to ExploreForYouCopy("Untukmu", "Mix dari kebiasaan dengarmu"),
    "ms" to ExploreForYouCopy("Untuk anda", "Mix daripada apa yang anda dengar"),
    "vi" to ExploreForYouCopy("Dành cho bạn", "Các mix tạo từ thói quen nghe của bạn"),
    "th" to ExploreForYouCopy("สำหรับคุณ", "มิกซ์ที่สร้างจากการฟังของคุณ"),
    "fil" to ExploreForYouCopy("Para sa iyo", "Mga mix mula sa pinakikinggan mo"),
    "he" to ExploreForYouCopy("בשבילך", "מיקסים שנוצרו מההאזנות שלך")
)

internal fun exploreForYouCopy(code: String): ExploreForYouCopy = localizedValueOrEnglish(exploreForYouCopies, code)

internal fun exploreForYouLocalizationCodes(): Set<String> = exploreForYouCopies.keys
