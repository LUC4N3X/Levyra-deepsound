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

internal fun newLocaleCoreOverrides(code: String): Map<String, String> = when (code) {
    "zh-Hant" -> coreCopy("開始吧。", "你的語言？", "你叫什麼名字？", "選擇 3 個或更多你喜歡的音樂類型。", "略過並繼續", "開始聆聽", "設定", "自訂 LEVYRA", "語言", "變更應用程式語言", "首頁", "搜尋", "音樂庫", "播放器", "播放佇列", "歌詞", "播放清單", "離線下載", "收藏", "快速精選", "播放", "搜尋歌曲、藝人等…", "返回", "選擇語言", "重新啟動 LEVYRA？", "語言已儲存。立即重新啟動應用程式，讓所有畫面完整套用新語言。", "立即重新啟動", "稍後")
    "hu" -> coreCopy("Kezdjük.", "Milyen nyelvet használsz?", "Hogy hívnak?", "Válassz legalább 3 kedvenc műfajt.", "Kihagyás és folytatás", "Hallgatás indítása", "Beállítások", "A LEVYRA személyre szabása", "Nyelv", "Az alkalmazás nyelvének módosítása", "Kezdőlap", "Keresés", "Könyvtár", "Lejátszó", "Lejátszási sor", "Dalszöveg", "Lejátszási listák", "Offline letöltések", "Kedvencek", "Gyors választások", "Lejátszás", "Keresés dalok, előadók között…", "Vissza", "Válassz nyelvet", "Újraindítod a LEVYRA-t?", "A nyelvet mentettük. Indítsd újra az alkalmazást, hogy minden képernyő az új nyelven töltődjön be.", "Újraindítás most", "Később")
    "bg" -> coreCopy("Да започваме.", "Кой е твоят език?", "Как се казваш?", "Избери поне 3 любими жанра.", "Пропусни и продължи", "Започни да слушаш", "Настройки", "Персонализирай LEVYRA", "Език", "Промени езика на приложението", "Начало", "Търсене", "Библиотека", "Плейър", "Опашка", "Текстове", "Плейлисти", "Офлайн изтегляния", "Любими", "Бързи избори", "Пусни", "Търси песни, изпълнители и още…", "Назад", "Избери език", "Рестартиране на LEVYRA?", "Езикът е запазен. Рестартирай приложението, за да се заредят всички екрани на новия език.", "Рестартирай сега", "По-късно")
    "nb" -> coreCopy("La oss begynne.", "Hvilket språk bruker du?", "Hva heter du?", "Velg minst 3 sjangre du liker.", "Hopp over og fortsett", "Begynn å lytte", "Innstillinger", "Tilpass LEVYRA", "Språk", "Endre språk i appen", "Hjem", "Søk", "Bibliotek", "Spiller", "Kø", "Sangtekster", "Spillelister", "Nedlastinger uten nett", "Favoritter", "Hurtigvalg", "Spill av", "Søk etter låter, artister og mer…", "Tilbake", "Velg språk", "Starte LEVYRA på nytt?", "Språket er lagret. Start appen på nytt for å laste alle skjermer med det nye språket.", "Start på nytt nå", "Senere")
    "ca" -> coreCopy("Comencem.", "Quin és el teu idioma?", "Com et dius?", "Tria 3 gèneres o més que t'agradin.", "Omet i continua", "Comença a escoltar", "Configuració", "Personalitza LEVYRA", "Idioma", "Canvia l'idioma de l'aplicació", "Inici", "Cerca", "Biblioteca", "Reproductor", "Cua", "Lletres", "Llistes de reproducció", "Baixades sense connexió", "Preferits", "Seleccions ràpides", "Reprodueix", "Cerca cançons, artistes i més…", "Enrere", "Selecciona un idioma", "Reiniciar LEVYRA?", "L'idioma s'ha desat. Reinicia l'aplicació per carregar totes les pantalles amb el nou idioma.", "Reinicia ara", "Més tard")
    "hr" -> coreCopy("Krenimo.", "Koji je tvoj jezik?", "Kako se zoveš?", "Odaberi najmanje 3 žanra koja voliš.", "Preskoči i nastavi", "Počni slušati", "Postavke", "Prilagodi LEVYRA-u", "Jezik", "Promijeni jezik aplikacije", "Početna", "Pretraži", "Medijateka", "Reproduktor", "Red čekanja", "Tekstovi pjesama", "Popisi za reprodukciju", "Offline preuzimanja", "Favoriti", "Brzi odabiri", "Reproduciraj", "Pretraži pjesme, izvođače i još…", "Natrag", "Odaberi jezik", "Ponovno pokrenuti LEVYRA-u?", "Jezik je spremljen. Ponovno pokreni aplikaciju kako bi se svi zasloni učitali na novom jeziku.", "Ponovno pokreni", "Kasnije")
    "sk" -> coreCopy("Začnime.", "Aký jazyk používaš?", "Ako sa voláš?", "Vyber aspoň 3 žánre, ktoré máš rád.", "Preskočiť a pokračovať", "Začať počúvať", "Nastavenia", "Prispôsobiť LEVYRA", "Jazyk", "Zmeniť jazyk aplikácie", "Domov", "Hľadať", "Knižnica", "Prehrávač", "Poradie", "Texty skladieb", "Playlisty", "Offline stiahnuté súbory", "Obľúbené", "Rýchle výbery", "Prehrať", "Hľadať skladby, interpretov a ďalšie…", "Späť", "Vyber jazyk", "Reštartovať LEVYRA?", "Jazyk bol uložený. Reštartuj aplikáciu, aby sa všetky obrazovky načítali v novom jazyku.", "Reštartovať teraz", "Neskôr")
    "ms" -> coreCopy("Mari mulakan.", "Apakah bahasa anda?", "Siapa nama anda?", "Pilih sekurang-kurangnya 3 genre yang anda suka.", "Langkau dan teruskan", "Mula mendengar", "Tetapan", "Sesuaikan LEVYRA", "Bahasa", "Tukar bahasa aplikasi", "Laman utama", "Cari", "Pustaka", "Pemain", "Baris gilir", "Lirik", "Senarai main", "Muat turun luar talian", "Kegemaran", "Pilihan pantas", "Main", "Cari lagu, artis dan banyak lagi…", "Kembali", "Pilih bahasa", "Mulakan semula LEVYRA?", "Bahasa telah disimpan. Mulakan semula aplikasi untuk memuatkan semua skrin dalam bahasa baharu.", "Mulakan semula sekarang", "Kemudian")
    "fa" -> coreCopy("شروع کنیم.", "زبان شما چیست؟", "نام شما چیست؟", "۳ سبک یا بیشتر را که دوست دارید انتخاب کنید.", "رد کردن و ادامه", "شروع گوش دادن", "تنظیمات", "شخصی‌سازی LEVYRA", "زبان", "تغییر زبان برنامه", "خانه", "جستجو", "کتابخانه", "پخش‌کننده", "صف پخش", "متن ترانه", "فهرست‌های پخش", "دانلودهای آفلاین", "علاقه‌مندی‌ها", "انتخاب‌های سریع", "پخش", "جستجوی آهنگ، هنرمند و بیشتر…", "بازگشت", "انتخاب زبان", "LEVYRA دوباره راه‌اندازی شود؟", "زبان ذخیره شد. برای بارگذاری همه صفحه‌ها با زبان جدید، برنامه را دوباره راه‌اندازی کنید.", "اکنون دوباره راه‌اندازی کن", "بعداً")
    else -> emptyMap()
}
