package com.luc4n3x.levyra.ui.i18n

private val exploreKeys = listOf(
    "exploreMoods",
    "exploreSamples",
    "exploreSamplesSubtitle",
    "exploreSamplesError",
    "exploreSamplesRetry"
)

private fun explore(vararg values: String): Map<String, String> {
    require(values.size == exploreKeys.size) {
        "Expected ${exploreKeys.size} explore strings, received ${values.size}"
    }
    return exploreKeys.zip(values.asList()).toMap()
}

private val exploreBundles: Map<String, Map<String, String>> = mapOf(
    "en" to explore("Moods & genres", "Samples", "Vertical clips from the videos of the moment", "Samples are unavailable right now. Try again shortly.", "Retry"),
    "it" to explore("Mood e generi", "Samples", "Clip verticali dai video del momento", "Samples non disponibili al momento. Riprova tra poco.", "Riprova"),
    "es" to explore("Estados de ánimo y géneros", "Samples", "Clips verticales de los vídeos del momento", "Los Samples no están disponibles ahora. Inténtalo de nuevo en breve.", "Reintentar"),
    "fr" to explore("Ambiances et genres", "Samples", "Clips verticaux tirés des vidéos du moment", "Les Samples sont indisponibles pour le moment. Réessayez dans un instant.", "Réessayer"),
    "de" to explore("Stimmungen & Genres", "Samples", "Vertikale Clips aus den Videos der Stunde", "Samples sind gerade nicht verfügbar. Versuche es gleich noch einmal.", "Erneut versuchen"),
    "pt" to explore("Ambientes e géneros", "Samples", "Clipes verticais dos vídeos do momento", "Os Samples não estão disponíveis agora. Tenta novamente em breve.", "Tentar novamente"),
    "nl" to explore("Stemmingen en genres", "Samples", "Verticale clips uit de video's van het moment", "Samples zijn nu niet beschikbaar. Probeer het zo opnieuw.", "Opnieuw proberen"),
    "pl" to explore("Nastroje i gatunki", "Samples", "Pionowe klipy z najnowszych teledysków", "Samples są teraz niedostępne. Spróbuj ponownie za chwilę.", "Spróbuj ponownie"),
    "ro" to explore("Stări și genuri", "Samples", "Clipuri verticale din videoclipurile momentului", "Samples nu sunt disponibile momentan. Încearcă din nou în curând.", "Încearcă din nou"),
    "el" to explore("Διαθέσεις και είδη", "Δείγματα", "Κάθετα κλιπ από τα βίντεο της στιγμής", "Τα Samples δεν είναι διαθέσιμα αυτή τη στιγμή. Δοκιμάστε ξανά σε λίγο.", "Δοκιμή ξανά"),
    "sv" to explore("Stämningar och genrer", "Samples", "Vertikala klipp från stundens videor", "Samples är inte tillgängliga just nu. Försök igen om en stund.", "Försök igen"),
    "da" to explore("Stemninger og genrer", "Samples", "Lodrette klip fra øjeblikkets videoer", "Samples er ikke tilgængelige lige nu. Prøv igen om lidt.", "Prøv igen"),
    "cs" to explore("Nálady a žánry", "Ukázky", "Svislé klipy z aktuálních videoklipů", "Samples teď nejsou dostupné. Zkuste to za chvíli znovu.", "Zkusit znovu"),
    "uk" to explore("Настрої та жанри", "Семпли", "Вертикальні кліпи з актуальних відео", "Семпли зараз недоступні. Спробуйте ще раз трохи пізніше.", "Спробувати знову"),
    "ru" to explore("Настроения и жанры", "Сэмплы", "Вертикальные клипы из актуальных видео", "Сэмплы сейчас недоступны. Попробуйте ещё раз чуть позже.", "Повторить"),
    "tr" to explore("Ruh halleri ve türler", "Örnekler", "Anın videolarından dikey klipler", "Örnekler şu anda kullanılamıyor. Kısa süre sonra tekrar deneyin.", "Tekrar dene"),
    "ar" to explore("الأجواء والأنواع", "مقتطفات", "مقاطع عمودية من فيديوهات اللحظة", "المقتطفات غير متاحة الآن. أعد المحاولة بعد قليل.", "إعادة المحاولة"),
    "zh" to explore("心情与流派", "音乐短片", "来自当下热门视频的竖屏短片", "音乐短片暂时不可用，请稍后重试。", "重试"),
    "ja" to explore("ムードとジャンル", "サンプル", "話題のビデオから生まれた縦型クリップ", "サンプルは現在利用できません。しばらくしてからもう一度お試しください。", "再試行"),
    "ko" to explore("무드 및 장르", "샘플", "지금 뜨는 영상에서 뽑은 세로형 클립", "샘플을 지금 사용할 수 없습니다. 잠시 후 다시 시도하세요.", "다시 시도"),
    "hi" to explore("मूड और शैलियाँ", "सैंपल", "इस पल के वीडियो से वर्टिकल क्लिप", "सैंपल अभी उपलब्ध नहीं हैं। थोड़ी देर बाद फिर कोशिश करें।", "फिर कोशिश करें"),
    "id" to explore("Suasana dan genre", "Sampel", "Klip vertikal dari video terkini", "Sampel belum tersedia saat ini. Coba lagi sebentar lagi.", "Coba lagi"),
    "vi" to explore("Tâm trạng và thể loại", "Mẫu nhạc", "Clip dọc từ những video đang hot", "Mẫu nhạc hiện chưa khả dụng. Hãy thử lại sau ít phút.", "Thử lại"),
    "th" to explore("อารมณ์และแนวเพลง", "ตัวอย่างเพลง", "คลิปแนวตั้งจากวิดีโอที่กำลังมาแรง", "ตัวอย่างเพลงยังไม่พร้อมใช้งานในขณะนี้ โปรดลองอีกครั้งในอีกสักครู่", "ลองอีกครั้ง"),
    "fil" to explore("Mood at genre", "Samples", "Mga vertical na clip mula sa mga video ngayon", "Hindi available ang Samples ngayon. Subukan ulit maya-maya.", "Subukan ulit"),
    "he" to explore("מצבי רוח וז'אנרים", "דגימות", "קליפים אנכיים מתוך הסרטונים של הרגע", "הדגימות אינן זמינות כרגע. נסו שוב בעוד רגע.", "נסו שוב"),
)

internal fun exploreLocalizationEntries(code: String): Map<String, String> = exploreBundles.getValue(code)

internal fun exploreLocalizationCodes(): Set<String> = exploreBundles.keys
