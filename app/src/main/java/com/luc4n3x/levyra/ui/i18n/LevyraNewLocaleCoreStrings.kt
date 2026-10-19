package com.luc4n3x.levyra.ui.i18n

private fun coreCopy(
    welcomeTitle: String,
    languageQuestion: String,
    nameQuestion: String,
    tasteQuestion: String,
    skipAndContinue: String,
    startListening: String,
    settings: String,
    settingsSubtitle: String,
    language: String,
    languageSubtitle: String,
    home: String,
    search: String,
    library: String,
    player: String,
    queue: String,
    lyrics: String,
    playlists: String,
    downloads: String,
    favorites: String,
    quickPicks: String,
    play: String,
    searchPlaceholder: String,
    back: String,
    selectLanguagePrompt: String,
    restartTitle: String,
    restartBody: String,
    restartNow: String,
    later: String
): Map<String, String> = mapOf(
    "welcomeTitle" to welcomeTitle,
    "languageQuestion" to languageQuestion,
    "nameQuestion" to nameQuestion,
    "tasteQuestion" to tasteQuestion,
    "skipAndContinue" to skipAndContinue,
    "startListening" to startListening,
    "settings" to settings,
    "settingsSubtitle" to settingsSubtitle,
    "language" to language,
    "languageSubtitle" to languageSubtitle,
    "home" to home,
    "search" to search,
    "library" to library,
    "player" to player,
    "queue" to queue,
    "lyrics" to lyrics,
    "playlists" to playlists,
    "downloads" to downloads,
    "favorites" to favorites,
    "quickPicks" to quickPicks,
    "play" to play,
    "searchPlaceholder" to searchPlaceholder,
    "back" to back,
    "selectLanguagePrompt" to selectLanguagePrompt,
    "restartRequiredTitle" to restartTitle,
    "restartRequiredBody" to restartBody,
    "restartNow" to restartNow,
    "later" to later
)

private fun setupCopy(welcomeBadge: String, namePlaceholder: String, next: String): Map<String, String> = mapOf(
    "welcomeBadge" to welcomeBadge,
    "namePlaceholder" to namePlaceholder,
    "next" to next
)

private fun newLocaleSetupOverrides(code: String): Map<String, String> = when (code) {
    "zh-Hant" -> setupCopy("歡迎", "你的名字…", "下一步")
    "hu" -> setupCopy("Üdvözlünk", "A neved…", "Tovább")
    "bg" -> setupCopy("Добре дошли", "Твоето име…", "Напред")
    "nb" -> setupCopy("Velkommen", "Navnet ditt…", "Neste")
    "ca" -> setupCopy("Benvingut", "El teu nom…", "Següent")
    "hr" -> setupCopy("Dobro došli", "Tvoje ime…", "Dalje")
    "sk" -> setupCopy("Vitaj", "Tvoje meno…", "Ďalej")
    "ms" -> setupCopy("Selamat datang", "Nama anda…", "Seterusnya")
    "fa" -> setupCopy("خوش آمدید", "نام شما…", "بعدی")
    "bn" -> setupCopy("স্বাগতম", "আপনার নাম…", "পরবর্তী")
    "kk" -> setupCopy("Қош келдіңіз", "Атыңыз…", "Келесі")
    "sr" -> setupCopy("Добро дошли", "Твоје име…", "Даље")
    "sq" -> setupCopy("Mirë se vjen", "Emri yt…", "Tjetër")
    else -> emptyMap()
}

private fun trendingCopy(
    albumsForYou: String,
    albumsForYouSetting: String,
    showRecommendedAlbums: String,
    trendingArtists: String
): Map<String, String> = mapOf(
    "albumsForYou" to albumsForYou,
    "albumsForYouSetting" to albumsForYouSetting,
    "showRecommendedAlbums" to showRecommendedAlbums,
    "trendingArtists" to trendingArtists
)

private fun newLocaleTrendingOverrides(code: String): Map<String, String> = when (code) {
    "zh-Hant" -> trendingCopy("熱門專輯", "熱門專輯", "在首頁顯示熱門專輯", "熱門歌手")
    "hu" -> trendingCopy("Felkapott albumok", "Felkapott albumok", "Felkapott albumok megjelenítése a Kezdőlapon", "Felkapott előadók")
    "bg" -> trendingCopy("Популярни албуми", "Популярни албуми", "Показвай популярни албуми в Начало", "Популярни изпълнители")
    "nb" -> trendingCopy("Populære album", "Populære album", "Vis populære album på Hjem", "Populære artister")
    "ca" -> trendingCopy("Àlbums del moment", "Àlbums del moment", "Mostra els àlbums del moment a l'Inici", "Artistes del moment")
    "bn" -> trendingCopy("জনপ্রিয় অ্যালবাম", "জনপ্রিয় অ্যালবাম", "হোমে জনপ্রিয় অ্যালবাম দেখান", "জনপ্রিয় শিল্পী")
    "kk" -> trendingCopy("Танымал альбомдар", "Танымал альбомдар", "Басты бетте танымал альбомдарды көрсету", "Танымал орындаушылар")
    "sr" -> trendingCopy("Популарни албуми", "Популарни албуми", "Прикажи популарне албуме на Почетној", "Популарни извођачи")
    "sq" -> trendingCopy("Albumet e momentit", "Albumet e momentit", "Shfaq albumet e momentit në Kreu", "Artistët e momentit")
    "hr" -> trendingCopy("Popularni albumi", "Popularni albumi", "Prikaži popularne albume na Početnoj", "Popularni izvođači")
    "sk" -> trendingCopy("Populárne albumy", "Populárne albumy", "Zobraziť populárne albumy na Domov", "Populárni interpreti")
    "ms" -> trendingCopy("Album trending", "Album trending", "Tunjukkan album trending di Laman Utama", "Artis trending")
    "fa" -> trendingCopy("آلبوم‌های برتر", "آلبوم‌های برتر", "نمایش آلبوم‌های برتر در خانه", "هنرمندان برتر")
    else -> emptyMap()
}

internal fun newLocaleCoreOverrides(code: String): Map<String, String> =
    newLocaleCoreCopy(code) + newLocaleSetupOverrides(code) + newLocaleTrendingOverrides(code)

private fun newLocaleCoreCopy(code: String): Map<String, String> = when (code) {
    "zh-Hant" -> coreCopy("開始吧。", "你的語言？", "你叫什麼名字？", "選擇 3 個或更多你喜歡的音樂類型。", "略過並繼續", "開始聆聽", "設定", "自訂 LEVYRA", "語言", "變更應用程式語言", "首頁", "搜尋", "音樂庫", "播放器", "播放佇列", "歌詞", "播放清單", "離線下載", "收藏", "快速精選", "播放", "搜尋歌曲、藝人等…", "返回", "選擇語言", "重新啟動 LEVYRA？", "語言已儲存。立即重新啟動應用程式，讓所有畫面完整套用新語言。", "立即重新啟動", "稍後")
    "hu" -> coreCopy("Kezdjük.", "Milyen nyelvet használsz?", "Hogy hívnak?", "Válassz legalább 3 kedvenc műfajt.", "Kihagyás és folytatás", "Hallgatás indítása", "Beállítások", "A LEVYRA személyre szabása", "Nyelv", "Az alkalmazás nyelvének módosítása", "Kezdőlap", "Keresés", "Könyvtár", "Lejátszó", "Lejátszási sor", "Dalszöveg", "Lejátszási listák", "Offline letöltések", "Kedvencek", "Gyors választások", "Lejátszás", "Keresés dalok, előadók között…", "Vissza", "Válassz nyelvet", "Újraindítod a LEVYRA-t?", "A nyelvet mentettük. Indítsd újra az alkalmazást, hogy minden képernyő az új nyelven töltődjön be.", "Újraindítás most", "Később")
    "bg" -> coreCopy("Да започваме.", "Кой е твоят език?", "Как се казваш?", "Избери поне 3 любими жанра.", "Пропусни и продължи", "Започни да слушаш", "Настройки", "Персонализирай LEVYRA", "Език", "Промени езика на приложението", "Начало", "Търсене", "Библиотека", "Плейър", "Опашка", "Текстове", "Плейлисти", "Офлайн изтегляния", "Любими", "Бързи избори", "Пусни", "Търси песни, изпълнители и още…", "Назад", "Избери език", "Рестартиране на LEVYRA?", "Езикът е запазен. Рестартирай приложението, за да се заредят всички екрани на новия език.", "Рестартирай сега", "По-късно")
    "nb" -> coreCopy("La oss begynne.", "Hvilket språk bruker du?", "Hva heter du?", "Velg minst 3 sjangre du liker.", "Hopp over og fortsett", "Begynn å lytte", "Innstillinger", "Tilpass LEVYRA", "Språk", "Endre språk i appen", "Hjem", "Søk", "Bibliotek", "Spiller", "Kø", "Sangtekster", "Spillelister", "Nedlastinger uten nett", "Favoritter", "Hurtigvalg", "Spill av", "Søk etter låter, artister og mer…", "Tilbake", "Velg språk", "Starte LEVYRA på nytt?", "Språket er lagret. Start appen på nytt for å laste alle skjermer med det nye språket.", "Start på nytt nå", "Senere")
    "ca" -> coreCopy("Comencem.", "Quin és el teu idioma?", "Com et dius?", "Tria 3 gèneres o més que t'agradin.", "Omet i continua", "Comença a escoltar", "Configuració", "Personalitza LEVYRA", "Idioma", "Canvia l'idioma de l'aplicació", "Inici", "Cerca", "Biblioteca", "Reproductor", "Cua", "Lletres", "Llistes de reproducció", "Baixades sense connexió", "Preferits", "Seleccions ràpides", "Reprodueix", "Cerca cançons, artistes i més…", "Enrere", "Selecciona un idioma", "Reiniciar LEVYRA?", "L'idioma s'ha desat. Reinicia l'aplicació per carregar totes les pantalles amb el nou idioma.", "Reinicia ara", "Més tard")
    "bn" -> coreCopy("চলুন শুরু করি।", "আপনার ভাষা কী?", "আপনার নাম কী?", "আপনার পছন্দের অন্তত 3টি ধারা বেছে নিন।", "এড়িয়ে যান ও চালিয়ে যান", "শোনা শুরু করুন", "সেটিংস", "LEVYRA নিজের মতো সাজান", "ভাষা", "অ্যাপের ভাষা পরিবর্তন করুন", "হোম", "সার্চ", "লাইব্রেরি", "প্লেয়ার", "সারি", "গানের কথা", "প্লেলিস্ট", "অফলাইন ডাউনলোড", "প্রিয়", "দ্রুত বাছাই", "চালান", "গান, শিল্পী এবং আরও খুঁজুন…", "ফিরে যান", "ভাষা বেছে নিন", "LEVYRA আবার চালু করবেন?", "ভাষা সংরক্ষিত হয়েছে। সব স্ক্রিন নতুন ভাষায় লোড করতে অ্যাপটি আবার চালু করুন।", "আবার চালু করুন", "পরে")
    "kk" -> coreCopy("Бастайық.", "Тіліңіз қандай?", "Атыңыз кім?", "Ұнайтын кемінде 3 жанрды таңдаңыз.", "Өткізіп, жалғастыру", "Тыңдауды бастау", "Баптаулар", "LEVYRA-ны өзіңізге бейімдеңіз", "Тіл", "Қолданба тілін өзгерту", "Басты бет", "Іздеу", "Кітапхана", "Ойнатқыш", "Кезек", "Ән мәтіндері", "Ойнату тізімдері", "Офлайн жүктеп алулар", "Таңдаулылар", "Жылдам таңдау", "Ойнату", "Әндер, орындаушылар және т.б. іздеу…", "Артқа", "Тілді таңдаңыз", "LEVYRA қайта іске қосылсын ба?", "Тіл сақталды. Барлық экран жаңа тілде жүктелуі үшін қолданбаны қайта іске қосыңыз.", "Қайта іске қосу", "Кейінірек")
    "sr" -> coreCopy("Хајде да почнемо.", "Који је твој језик?", "Како се зовеш?", "Изабери најмање 3 жанра која волиш.", "Прескочи и настави", "Почни да слушаш", "Подешавања", "Прилагоди LEVYRA-у", "Језик", "Промени језик апликације", "Почетна", "Претрага", "Библиотека", "Плејер", "Ред", "Текстови песама", "Плејлисте", "Офлајн преузимања", "Омиљено", "Брзи избор", "Пусти", "Претражи песме, извођаче и још…", "Назад", "Изабери језик", "Поново покренути LEVYRA-у?", "Језик је сачуван. Поново покрени апликацију да би се сви екрани учитали на новом језику.", "Поново покрени", "Касније")
    "sq" -> coreCopy("Le të fillojmë.", "Cila është gjuha jote?", "Si e ke emrin?", "Zgjidh të paktën 3 zhanre që të pëlqejnë.", "Kapërce dhe vazhdo", "Fillo të dëgjosh", "Cilësimet", "Përshtate LEVYRA-n", "Gjuha", "Ndrysho gjuhën e aplikacionit", "Kreu", "Kërko", "Biblioteka", "Luajtësi", "Radha", "Tekstet e këngëve", "Listat e luajtjes", "Shkarkimet offline", "Të preferuarat", "Zgjedhje të shpejta", "Luaj", "Kërko këngë, artistë dhe më shumë…", "Mbrapa", "Zgjidh gjuhën", "Të riniset LEVYRA?", "Gjuha u ruajt. Rinise aplikacionin që të gjitha ekranet të ngarkohen në gjuhën e re.", "Rinis", "Më vonë")
    "hr" -> coreCopy("Krenimo.", "Koji je tvoj jezik?", "Kako se zoveš?", "Odaberi najmanje 3 žanra koja voliš.", "Preskoči i nastavi", "Počni slušati", "Postavke", "Prilagodi LEVYRA-u", "Jezik", "Promijeni jezik aplikacije", "Početna", "Pretraži", "Medijateka", "Reproduktor", "Red čekanja", "Tekstovi pjesama", "Popisi za reprodukciju", "Offline preuzimanja", "Favoriti", "Brzi odabiri", "Reproduciraj", "Pretraži pjesme, izvođače i još…", "Natrag", "Odaberi jezik", "Ponovno pokrenuti LEVYRA-u?", "Jezik je spremljen. Ponovno pokreni aplikaciju kako bi se svi zasloni učitali na novom jeziku.", "Ponovno pokreni", "Kasnije")
    "sk" -> coreCopy("Začnime.", "Aký jazyk používaš?", "Ako sa voláš?", "Vyber aspoň 3 žánre, ktoré máš rád.", "Preskočiť a pokračovať", "Začať počúvať", "Nastavenia", "Prispôsobiť LEVYRA", "Jazyk", "Zmeniť jazyk aplikácie", "Domov", "Hľadať", "Knižnica", "Prehrávač", "Poradie", "Texty skladieb", "Playlisty", "Offline stiahnuté súbory", "Obľúbené", "Rýchle výbery", "Prehrať", "Hľadať skladby, interpretov a ďalšie…", "Späť", "Vyber jazyk", "Reštartovať LEVYRA?", "Jazyk bol uložený. Reštartuj aplikáciu, aby sa všetky obrazovky načítali v novom jazyku.", "Reštartovať teraz", "Neskôr")
    "ms" -> coreCopy("Mari mulakan.", "Apakah bahasa anda?", "Siapa nama anda?", "Pilih sekurang-kurangnya 3 genre yang anda suka.", "Langkau dan teruskan", "Mula mendengar", "Tetapan", "Sesuaikan LEVYRA", "Bahasa", "Tukar bahasa aplikasi", "Laman utama", "Cari", "Pustaka", "Pemain", "Baris gilir", "Lirik", "Senarai main", "Muat turun luar talian", "Kegemaran", "Pilihan pantas", "Main", "Cari lagu, artis dan banyak lagi…", "Kembali", "Pilih bahasa", "Mulakan semula LEVYRA?", "Bahasa telah disimpan. Mulakan semula aplikasi untuk memuatkan semua skrin dalam bahasa baharu.", "Mulakan semula sekarang", "Kemudian")
    "fa" -> coreCopy("شروع کنیم.", "زبان شما چیست؟", "نام شما چیست؟", "۳ سبک یا بیشتر را که دوست دارید انتخاب کنید.", "رد کردن و ادامه", "شروع گوش دادن", "تنظیمات", "شخصی‌سازی LEVYRA", "زبان", "تغییر زبان برنامه", "خانه", "جستجو", "کتابخانه", "پخش‌کننده", "صف پخش", "متن ترانه", "فهرست‌های پخش", "دانلودهای آفلاین", "علاقه‌مندی‌ها", "انتخاب‌های سریع", "پخش", "جستجوی آهنگ، هنرمند و بیشتر…", "بازگشت", "انتخاب زبان", "LEVYRA دوباره راه‌اندازی شود؟", "زبان ذخیره شد. برای بارگذاری همه صفحه‌ها با زبان جدید، برنامه را دوباره راه‌اندازی کنید.", "اکنون دوباره راه‌اندازی کن", "بعداً")
    else -> emptyMap()
}
