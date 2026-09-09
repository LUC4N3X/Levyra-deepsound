package com.luc4n3x.levyra.ui.i18n

internal val offlineHomeKeys = setOf(
    "offlineHomeTitle",
    "offlineHomeMessage",
    "offlineHomeRetry",
    "offlineHomeDownloads",
    "offlineHomePlaylists",
    "offlineHomeFavorites",
    "offlineHomeRecent"
)

private fun offlineHome(
    title: String,
    message: String,
    retry: String,
    downloads: String,
    playlists: String,
    favorites: String,
    recent: String
): Map<String, String> = mapOf(
    "offlineHomeTitle" to title,
    "offlineHomeMessage" to message,
    "offlineHomeRetry" to retry,
    "offlineHomeDownloads" to downloads,
    "offlineHomePlaylists" to playlists,
    "offlineHomeFavorites" to favorites,
    "offlineHomeRecent" to recent
)

private val offlineHomeBundles: Map<String, Map<String, String>> = mapOf(
    "en" to offlineHome("You're offline", "Levyra is showing the music saved on this device.", "Retry", "Downloads", "Offline playlists", "Favorites", "Recently played"),
    "it" to offlineHome("Sei offline", "Levyra mostra la musica salvata su questo dispositivo.", "Riprova", "Download", "Playlist offline", "Preferiti", "Ascoltati di recente"),
    "es" to offlineHome("Estás sin conexión", "Levyra muestra la música guardada en este dispositivo.", "Reintentar", "Descargas", "Listas sin conexión", "Favoritos", "Reproducidas hace poco"),
    "fr" to offlineHome("Vous êtes hors ligne", "Levyra affiche la musique enregistrée sur cet appareil.", "Réessayer", "Téléchargements", "Playlists hors ligne", "Favoris", "Écoutés récemment"),
    "de" to offlineHome("Du bist offline", "Levyra zeigt die auf diesem Gerät gespeicherte Musik.", "Erneut versuchen", "Downloads", "Offline-Playlists", "Favoriten", "Zuletzt gehört"),
    "pt" to offlineHome("Você está offline", "O Levyra mostra a música salva neste dispositivo.", "Tentar de novo", "Downloads", "Playlists offline", "Favoritos", "Ouvidas recentemente"),
    "nl" to offlineHome("Je bent offline", "Levyra toont de muziek die op dit apparaat is opgeslagen.", "Opnieuw proberen", "Downloads", "Offline afspeellijsten", "Favorieten", "Onlangs afgespeeld"),
    "pl" to offlineHome("Jesteś offline", "Levyra pokazuje muzykę zapisaną na tym urządzeniu.", "Spróbuj ponownie", "Pobrane", "Playlisty offline", "Ulubione", "Ostatnio odtwarzane"),
    "ro" to offlineHome("Ești offline", "Levyra afișează muzica salvată pe acest dispozitiv.", "Reîncearcă", "Descărcări", "Playlisturi offline", "Favorite", "Ascultate recent"),
    "el" to offlineHome("Είσαι εκτός σύνδεσης", "Το Levyra δείχνει τη μουσική που είναι αποθηκευμένη σε αυτή τη συσκευή.", "Δοκίμασε ξανά", "Λήψεις", "Λίστες εκτός σύνδεσης", "Αγαπημένα", "Πρόσφατες ακροάσεις"),
    "sv" to offlineHome("Du är offline", "Levyra visar musiken som är sparad på den här enheten.", "Försök igen", "Nedladdningar", "Offlinespellistor", "Favoriter", "Nyligen spelade"),
    "da" to offlineHome("Du er offline", "Levyra viser musikken, der er gemt på denne enhed.", "Prøv igen", "Downloads", "Offlineplaylister", "Favoritter", "Senest afspillet"),
    "cs" to offlineHome("Jsi offline", "Levyra zobrazuje hudbu uloženou v tomto zařízení.", "Zkusit znovu", "Stažené", "Offline playlisty", "Oblíbené", "Nedávno přehrané"),
    "uk" to offlineHome("Ви офлайн", "Levyra показує музику, збережену на цьому пристрої.", "Повторити", "Завантаження", "Офлайн-плейлисти", "Улюблені", "Нещодавно прослухані"),
    "ru" to offlineHome("Вы офлайн", "Levyra показывает музыку, сохранённую на этом устройстве.", "Повторить", "Загрузки", "Офлайн-плейлисты", "Избранное", "Недавно прослушанные"),
    "tr" to offlineHome("Çevrimdışısın", "Levyra bu cihazda kayıtlı müzikleri gösteriyor.", "Yeniden dene", "İndirilenler", "Çevrimdışı çalma listeleri", "Favoriler", "Son çalınanlar"),
    "ar" to offlineHome("أنت غير متصل", "يعرض Levyra الموسيقى المحفوظة على هذا الجهاز.", "إعادة المحاولة", "التنزيلات", "قوائم التشغيل دون اتصال", "المفضلة", "المُشغَّلة مؤخرًا"),
    "zh" to offlineHome("你处于离线状态", "Levyra 正在显示保存在此设备上的音乐。", "重试", "下载内容", "离线播放列表", "收藏", "最近播放"),
    "ja" to offlineHome("オフラインです", "Levyra はこの端末に保存された音楽を表示しています。", "再試行", "ダウンロード", "オフラインのプレイリスト", "お気に入り", "最近再生した曲"),
    "ko" to offlineHome("오프라인 상태입니다", "Levyra가 이 기기에 저장된 음악을 표시하고 있습니다.", "다시 시도", "다운로드", "오프라인 재생목록", "즐겨찾기", "최근 재생"),
    "hi" to offlineHome("आप ऑफ़लाइन हैं", "Levyra इस डिवाइस पर सहेजा गया संगीत दिखा रहा है।", "फिर कोशिश करें", "डाउनलोड", "ऑफ़लाइन प्लेलिस्ट", "पसंदीदा", "हाल में सुने गए"),
    "id" to offlineHome("Kamu sedang offline", "Levyra menampilkan musik yang tersimpan di perangkat ini.", "Coba lagi", "Unduhan", "Playlist offline", "Favorit", "Baru diputar"),
    "vi" to offlineHome("Bạn đang ngoại tuyến", "Levyra đang hiển thị nhạc đã lưu trên thiết bị này.", "Thử lại", "Bản tải xuống", "Danh sách phát ngoại tuyến", "Yêu thích", "Nghe gần đây"),
    "th" to offlineHome("คุณออฟไลน์อยู่", "Levyra กำลังแสดงเพลงที่บันทึกไว้ในอุปกรณ์นี้", "ลองใหม่", "ดาวน์โหลด", "เพลย์ลิสต์ออฟไลน์", "รายการโปรด", "เล่นล่าสุด"),
    "fil" to offlineHome("Offline ka ngayon", "Ipinapakita ng Levyra ang musikang naka-save sa device na ito.", "Subukan ulit", "Mga download", "Mga offline na playlist", "Mga paborito", "Kamakailang pinatugtog"),
    "he" to offlineHome("אתה במצב לא מקוון", "Levyra מציגה את המוזיקה השמורה במכשיר הזה.", "נסה שוב", "הורדות", "פלייליסטים במצב לא מקוון", "מועדפים", "הושמעו לאחרונה")
)

internal fun offlineHomeLocalizationEntries(code: String): Map<String, String> =
    offlineHomeBundles.getValue(code)

internal fun offlineHomeLocalizationCodes(): Set<String> = offlineHomeBundles.keys
