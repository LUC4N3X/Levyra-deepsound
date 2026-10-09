package com.luc4n3x.levyra.ui.i18n

private val exploreKeys = listOf(
    "exploreMoods",
    "exploreMoodSection",
    "exploreSamples",
    "exploreSamplesSubtitle",
    "exploreSamplesError",
    "exploreSamplesRetry",
    "exploreAfrobeats"
)

private fun explore(vararg values: String): Map<String, String> {
    require(values.size == exploreKeys.size) {
        "Expected ${exploreKeys.size} explore strings, received ${values.size}"
    }
    return exploreKeys.zip(values.asList()).toMap()
}

private val exploreBundles: Map<String, Map<String, String>> = mapOf(
    "en" to explore("Moods & genres", "Moods", "Samples", "Vertical clips from the videos of the moment", "Samples are unavailable right now. Try again shortly.", "Retry", "Afrobeats"),
    "it" to explore("Mood e generi", "Mood", "Samples", "Clip verticali dai video del momento", "Samples non disponibili al momento. Riprova tra poco.", "Riprova", "Afrobeats"),
    "es" to explore("Estados de ánimo y géneros", "Estados de ánimo", "Samples", "Clips verticales de los vídeos del momento", "Los Samples no están disponibles ahora. Inténtalo de nuevo en breve.", "Reintentar", "Afrobeats"),
    "fr" to explore("Ambiances et genres", "Ambiances", "Samples", "Clips verticaux tirés des vidéos du moment", "Les Samples sont indisponibles pour le moment. Réessayez dans un instant.", "Réessayer", "Afrobeats"),
    "de" to explore("Stimmungen & Genres", "Stimmungen", "Samples", "Vertikale Clips aus den Videos der Stunde", "Samples sind gerade nicht verfügbar. Versuche es gleich noch einmal.", "Erneut versuchen", "Afrobeats"),
    "pt" to explore("Ambientes e géneros", "Ambientes", "Samples", "Clipes verticais dos vídeos do momento", "Os Samples não estão disponíveis agora. Tenta novamente em breve.", "Tentar novamente", "Afrobeats"),
    "nl" to explore("Stemmingen en genres", "Stemmingen", "Samples", "Verticale clips uit de video's van het moment", "Samples zijn nu niet beschikbaar. Probeer het zo opnieuw.", "Opnieuw proberen", "Afrobeats"),
    "pl" to explore("Nastroje i gatunki", "Nastroje", "Samples", "Pionowe klipy z najnowszych teledysków", "Samples są teraz niedostępne. Spróbuj ponownie za chwilę.", "Spróbuj ponownie", "Afrobeats"),
    "ro" to explore("Stări și genuri", "Stări", "Samples", "Clipuri verticale din videoclipurile momentului", "Samples nu sunt disponibile momentan. Încearcă din nou în curând.", "Încearcă din nou", "Afrobeats"),
    "el" to explore("Διαθέσεις και είδη", "Διαθέσεις", "Δείγματα", "Κάθετα κλιπ από τα βίντεο της στιγμής", "Τα Samples δεν είναι διαθέσιμα αυτή τη στιγμή. Δοκιμάστε ξανά σε λίγο.", "Δοκιμή ξανά", "Afrobeats"),
    "sv" to explore("Stämningar och genrer", "Stämningar", "Samples", "Vertikala klipp från stundens videor", "Samples är inte tillgängliga just nu. Försök igen om en stund.", "Försök igen", "Afrobeats"),
    "da" to explore("Stemninger og genrer", "Stemninger", "Samples", "Lodrette klip fra øjeblikkets videoer", "Samples er ikke tilgængelige lige nu. Prøv igen om lidt.", "Prøv igen", "Afrobeats"),
    "cs" to explore("Nálady a žánry", "Nálady", "Ukázky", "Svislé klipy z aktuálních videoklipů", "Samples teď nejsou dostupné. Zkuste to za chvíli znovu.", "Zkusit znovu", "Afrobeats"),
    "sk" to explore("Nálady a žánre", "Nálady", "Ukážky", "Zvislé klipy z aktuálnych videí", "Ukážky momentálne nie sú dostupné. Skúste to znova o chvíľu.", "Skúsiť znova", "Afrobeats"),
    "hr" to explore("Raspoloženja i žanrovi", "Raspoloženja", "Isječci", "Okomiti isječci iz aktualnih videozapisa", "Isječci trenutačno nisu dostupni. Pokušajte ponovno uskoro.", "Pokušaj ponovno", "Afrobeats"),
    "bg" to explore("Настроения и жанрове", "Настроения", "Откъси", "Вертикални откъси от актуалните видеоклипове", "Откъсите в момента не са налични. Опитайте отново след малко.", "Опитайте отново", "Afrobeats"),
    "hu" to explore("Hangulatok és műfajok", "Hangulatok", "Részletek", "Álló klipek az éppen népszerű videókból", "A részletek jelenleg nem érhetők el. Próbáld újra hamarosan.", "Újra", "Afrobeats"),
    "uk" to explore("Настрої та жанри", "Настрої", "Семпли", "Вертикальні кліпи з актуальних відео", "Семпли зараз недоступні. Спробуйте ще раз трохи пізніше.", "Спробувати знову", "Afrobeats"),
    "ru" to explore("Настроения и жанры", "Настроения", "Сэмплы", "Вертикальные клипы из актуальных видео", "Сэмплы сейчас недоступны. Попробуйте ещё раз чуть позже.", "Повторить", "Afrobeats"),
    "tr" to explore("Ruh halleri ve türler", "Ruh halleri", "Örnekler", "Anın videolarından dikey klipler", "Örnekler şu anda kullanılamıyor. Kısa süre sonra tekrar deneyin.", "Tekrar dene", "Afrobeats"),
    "ar" to explore("الأجواء والأنواع", "الأجواء", "مقتطفات", "مقاطع عمودية من فيديوهات اللحظة", "المقتطفات غير متاحة الآن. أعد المحاولة بعد قليل.", "إعادة المحاولة", "Afrobeats"),
    "fa" to explore("حال‌وهوا و ژانرها", "حال‌وهوا", "نمونه‌ها", "کلیپ‌های عمودی از ویدیوهای محبوب این لحظه", "نمونه‌ها فعلاً در دسترس نیستند. کمی بعد دوباره تلاش کنید.", "تلاش دوباره", "Afrobeats"),
    "zh" to explore("心情与流派", "心情", "音乐短片", "来自当下热门视频的竖屏短片", "音乐短片暂时不可用，请稍后重试。", "重试", "Afrobeats"),
    "zh-Hant" to explore("心情與曲風", "心情", "音樂短片", "來自當下熱門影片的直式短片", "音樂短片目前無法使用，請稍後再試。", "重試", "Afrobeats"),
    "ja" to explore("ムードとジャンル", "ムード", "サンプル", "話題のビデオから生まれた縦型クリップ", "サンプルは現在利用できません。しばらくしてからもう一度お試しください。", "再試行", "Afrobeats"),
    "ko" to explore("무드 및 장르", "무드", "샘플", "지금 뜨는 영상에서 뽑은 세로형 클립", "샘플을 지금 사용할 수 없습니다. 잠시 후 다시 시도하세요.", "다시 시도", "Afrobeats"),
    "hi" to explore("मूड और शैलियाँ", "मूड", "सैंपल", "इस पल के वीडियो से वर्टिकल क्लिप", "सैंपल अभी उपलब्ध नहीं हैं। थोड़ी देर बाद फिर कोशिश करें।", "फिर कोशिश करें", "Afrobeats"),
    "id" to explore("Suasana dan genre", "Suasana", "Sampel", "Klip vertikal dari video terkini", "Sampel belum tersedia saat ini. Coba lagi sebentar lagi.", "Coba lagi", "Afrobeats"),
    "ms" to explore("Suasana dan genre", "Suasana", "Sampel", "Klip menegak daripada video popular semasa", "Sampel tidak tersedia buat masa ini. Cuba lagi sebentar lagi.", "Cuba lagi", "Afrobeats"),
    "vi" to explore("Tâm trạng và thể loại", "Tâm trạng", "Mẫu nhạc", "Clip dọc từ những video đang hot", "Mẫu nhạc hiện chưa khả dụng. Hãy thử lại sau ít phút.", "Thử lại", "Afrobeats"),
    "th" to explore("อารมณ์และแนวเพลง", "อารมณ์", "ตัวอย่างเพลง", "คลิปแนวตั้งจากวิดีโอที่กำลังมาแรง", "ตัวอย่างเพลงยังไม่พร้อมใช้งานในขณะนี้ โปรดลองอีกครั้งในอีกสักครู่", "ลองอีกครั้ง", "Afrobeats"),
    "fil" to explore("Mood at genre", "Mood", "Samples", "Mga vertical na clip mula sa mga video ngayon", "Hindi available ang Samples ngayon. Subukan ulit maya-maya.", "Subukan ulit", "Afrobeats"),
    "he" to explore("מצבי רוח וז'אנרים", "מצבי רוח", "דגימות", "קליפים אנכיים מתוך הסרטונים של הרגע", "הדגימות אינן זמינות כרגע. נסו שוב בעוד רגע.", "נסו שוב", "Afrobeats"),
    "fi" to explore("Tunnelmat ja tyylilajit", "Tunnelmat", "Näytteet", "Pystyvideoleikkeet hetken videoista", "Näytteet eivät ole juuri nyt saatavilla. Yritä hetken kuluttua uudelleen.", "Yritä uudelleen", "Afrobeats"),
    "nb" to explore("Stemninger og sjangre", "Stemninger", "Klipp", "Vertikale klipp fra videoene som er populære nå", "Klipp er ikke tilgjengelige akkurat nå. Prøv igjen om litt.", "Prøv igjen", "Afrobeats"),
    "ca" to explore("Estats d'ànim i gèneres", "Estats d'ànim", "Mostres", "Clips verticals dels vídeos del moment", "Les mostres no estan disponibles ara mateix. Torna-ho a provar d'aquí a poc.", "Torna-ho a provar", "Afrobeats"),
    "et" to explore("Meeleolud ja žanrid", "Meeleolud", "Näidised", "Vertikaalsed klipid hetke videotest", "Näidised pole praegu saadaval. Proovi varsti uuesti.", "Proovi uuesti", "Afrobeats"),
)

internal fun exploreLocalizationEntries(code: String): Map<String, String> = localizedBundleOrEnglish(exploreBundles, code)

internal fun exploreLocalizationCodes(): Set<String> = exploreBundles.keys
