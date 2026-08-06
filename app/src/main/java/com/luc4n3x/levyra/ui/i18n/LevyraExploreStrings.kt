package com.luc4n3x.levyra.ui.i18n

private val exploreKeys = listOf(
    "exploreMoods",
    "exploreSamples",
    "exploreSamplesSubtitle"
)

private fun explore(vararg values: String): Map<String, String> {
    require(values.size == exploreKeys.size) {
        "Expected ${exploreKeys.size} explore strings, received ${values.size}"
    }
    return exploreKeys.zip(values.asList()).toMap()
}

private val exploreBundles: Map<String, Map<String, String>> = mapOf(
    "en" to explore("Moods & genres", "Samples", "Vertical clips from the videos of the moment"),
    "it" to explore("Mood e generi", "Samples", "Clip verticali dai video del momento"),
    "es" to explore("Estados de ánimo y géneros", "Samples", "Clips verticales de los vídeos del momento"),
    "fr" to explore("Ambiances et genres", "Samples", "Clips verticaux tirés des vidéos du moment"),
    "de" to explore("Stimmungen & Genres", "Samples", "Vertikale Clips aus den Videos der Stunde"),
    "pt" to explore("Ambientes e géneros", "Samples", "Clipes verticais dos vídeos do momento"),
    "nl" to explore("Stemmingen en genres", "Samples", "Verticale clips uit de video's van het moment"),
    "pl" to explore("Nastroje i gatunki", "Samples", "Pionowe klipy z najnowszych teledysków"),
    "ro" to explore("Stări și genuri", "Samples", "Clipuri verticale din videoclipurile momentului"),
    "el" to explore("Διαθέσεις και είδη", "Δείγματα", "Κάθετα κλιπ από τα βίντεο της στιγμής"),
    "sv" to explore("Stämningar och genrer", "Samples", "Vertikala klipp från stundens videor"),
    "da" to explore("Stemninger og genrer", "Samples", "Lodrette klip fra øjeblikkets videoer"),
    "cs" to explore("Nálady a žánry", "Ukázky", "Svislé klipy z aktuálních videoklipů"),
    "uk" to explore("Настрої та жанри", "Семпли", "Вертикальні кліпи з актуальних відео"),
    "ru" to explore("Настроения и жанры", "Сэмплы", "Вертикальные клипы из актуальных видео"),
    "tr" to explore("Ruh halleri ve türler", "Örnekler", "Anın videolarından dikey klipler"),
    "ar" to explore("الأجواء والأنواع", "مقتطفات", "مقاطع عمودية من فيديوهات اللحظة"),
    "zh" to explore("心情与流派", "音乐短片", "来自当下热门视频的竖屏短片"),
    "ja" to explore("ムードとジャンル", "サンプル", "話題のビデオから生まれた縦型クリップ"),
    "ko" to explore("무드 및 장르", "샘플", "지금 뜨는 영상에서 뽑은 세로형 클립"),
    "hi" to explore("मूड और शैलियाँ", "सैंपल", "इस पल के वीडियो से वर्टिकल क्लिप"),
    "id" to explore("Suasana dan genre", "Sampel", "Klip vertikal dari video terkini"),
    "vi" to explore("Tâm trạng và thể loại", "Mẫu nhạc", "Clip dọc từ những video đang hot"),
    "th" to explore("อารมณ์และแนวเพลง", "ตัวอย่างเพลง", "คลิปแนวตั้งจากวิดีโอที่กำลังมาแรง"),
    "fil" to explore("Mood at genre", "Samples", "Mga vertical na clip mula sa mga video ngayon"),
    "he" to explore("מצבי רוח וז'אנרים", "דגימות", "קליפים אנכיים מתוך הסרטונים של הרגע")
)

internal fun exploreLocalizationEntries(code: String): Map<String, String> = exploreBundles.getValue(code)

internal fun exploreLocalizationCodes(): Set<String> = exploreBundles.keys
