package com.luc4n3x.levyra.ui.i18n

internal data class CuratedCollectionsCopy(
    val decadesTitle: String,
    val decadesSubtitle: String,
    val everydayTitle: String,
    val everydaySubtitle: String
)

private val curatedCollectionsCopies = mapOf(
    "en" to CuratedCollectionsCopy("Decades", "Music through the decades", "For every moment", "Playlists for your day"),
    "it" to CuratedCollectionsCopy("Decenni", "La musica dei decenni", "Per ogni momento", "Playlist per la giornata"),
    "es" to CuratedCollectionsCopy("Décadas", "Música de cada década", "Para cada momento", "Playlists para tu día"),
    "fr" to CuratedCollectionsCopy("Décennies", "La musique des décennies", "Pour chaque moment", "Playlists pour ta journée"),
    "de" to CuratedCollectionsCopy("Jahrzehnte", "Musik der Jahrzehnte", "Für jeden Moment", "Playlists für deinen Tag"),
    "pt" to CuratedCollectionsCopy("Décadas", "Música de várias décadas", "Para cada momento", "Playlists para o seu dia"),
    "pt-BR" to CuratedCollectionsCopy("Décadas", "Música de cada década", "Pra cada momento", "Playlists pro seu dia"),
    "nl" to CuratedCollectionsCopy("Decennia", "Muziek door de decennia", "Voor elk moment", "Playlists voor je dag"),
    "pl" to CuratedCollectionsCopy("Dekady", "Muzyka różnych dekad", "Na każdą chwilę", "Playlisty na cały dzień"),
    "ro" to CuratedCollectionsCopy("Decenii", "Muzica deceniilor", "Pentru orice moment", "Playlisturi pentru ziua ta"),
    "el" to CuratedCollectionsCopy("Δεκαετίες", "Μουσική των δεκαετιών", "Για κάθε στιγμή", "Playlist για τη μέρα σου"),
    "sv" to CuratedCollectionsCopy("Decennier", "Musik genom decennierna", "För varje stund", "Spellistor för din dag"),
    "da" to CuratedCollectionsCopy("Årtier", "Musik gennem årtierne", "Til hvert øjeblik", "Playlister til din dag"),
    "cs" to CuratedCollectionsCopy("Dekády", "Hudba napříč dekádami", "Pro každou chvíli", "Playlisty na celý den"),
    "uk" to CuratedCollectionsCopy("Десятиліття", "Музика різних десятиліть", "Для будь-якої миті", "Плейлисти на весь день"),
    "ru" to CuratedCollectionsCopy("Десятилетия", "Музыка разных десятилетий", "На каждый момент", "Плейлисты на весь день"),
    "nb" to CuratedCollectionsCopy("Tiår", "Musikk gjennom tiårene", "For hvert øyeblikk", "Spillelister for dagen"),
    "ca" to CuratedCollectionsCopy("Dècades", "Música de cada dècada", "Per a cada moment", "Llistes per al teu dia"),
    "kk" to CuratedCollectionsCopy("Онжылдықтар", "Әр онжылдықтың музыкасы", "Әр сәтке", "Күніңізге арналған плейлистер"),
    "tr" to CuratedCollectionsCopy("On yıllar", "On yılların müziği", "Her an için", "Gününüz için listeler"),
    "ar" to CuratedCollectionsCopy("العقود", "موسيقى عبر العقود", "لكل لحظة", "قوائم تشغيل ليومك"),
    "fa" to CuratedCollectionsCopy("دهه‌ها", "موسیقی دهه‌ها", "برای هر لحظه", "فهرست‌های پخش روزانه"),
    "zh" to CuratedCollectionsCopy("年代", "穿越年代的音乐", "每个时刻", "陪你度过每一天"),
    "zh-Hant" to CuratedCollectionsCopy("年代", "穿越年代的音樂", "每個時刻", "陪你度過每一天"),
    "ja" to CuratedCollectionsCopy("年代別", "年代をめぐる音楽", "あらゆる瞬間に", "毎日のプレイリスト"),
    "ko" to CuratedCollectionsCopy("시대별", "시대를 아우르는 음악", "모든 순간을 위해", "하루를 위한 플레이리스트"),
    "hi" to CuratedCollectionsCopy("दशक", "दशकों का संगीत", "हर पल के लिए", "आपके दिन की प्लेलिस्ट"),
    "bn" to CuratedCollectionsCopy("দশক", "দশকের পর দশকের গান", "প্রতিটি মুহূর্তের জন্য", "আপনার দিনের প্লেলিস্ট"),
    "id" to CuratedCollectionsCopy("Dekade", "Musik lintas dekade", "Untuk setiap momen", "Playlist untuk harimu"),
    "ms" to CuratedCollectionsCopy("Dekad", "Muzik merentas dekad", "Untuk setiap detik", "Senarai main harian"),
    "vi" to CuratedCollectionsCopy("Thập niên", "Âm nhạc qua các thập niên", "Cho mọi khoảnh khắc", "Playlist cho ngày của bạn"),
    "th" to CuratedCollectionsCopy("ยุคสมัย", "เพลงข้ามทศวรรษ", "ทุกช่วงเวลา", "เพลย์ลิสต์สำหรับวันของคุณ"),
    "fil" to CuratedCollectionsCopy("Mga dekada", "Musika ng bawat dekada", "Sa bawat sandali", "Mga playlist sa araw mo"),
    "he" to CuratedCollectionsCopy("עשורים", "מוזיקה לאורך העשורים", "לכל רגע", "פלייליסטים ליום שלך"),
    "hu" to CuratedCollectionsCopy("Évtizedek", "Az évtizedek zenéje", "Minden pillanatra", "Listák a napodhoz"),
    "fi" to CuratedCollectionsCopy("Vuosikymmenet", "Musiikkia vuosikymmeniltä", "Joka hetkeen", "Soittolistat päivääsi"),
    "bg" to CuratedCollectionsCopy("Десетилетия", "Музика през десетилетията", "За всеки момент", "Плейлисти за деня ти"),
    "hr" to CuratedCollectionsCopy("Desetljeća", "Glazba kroz desetljeća", "Za svaki trenutak", "Playliste za tvoj dan"),
    "sr" to CuratedCollectionsCopy("Деценије", "Музика кроз деценије", "За сваки тренутак", "Плејлисте за твој дан"),
    "sk" to CuratedCollectionsCopy("Dekády", "Hudba naprieč dekádami", "Na každú chvíľu", "Playlisty na celý deň"),
    "et" to CuratedCollectionsCopy("Kümnendid", "Muusika läbi kümnendite", "Igaks hetkeks", "Esitusloendid päevaks"),
    "sq" to CuratedCollectionsCopy("Dekadat", "Muzika ndër dekada", "Për çdo moment", "Playlist për ditën tënde")
)

internal fun curatedCollectionsCopy(code: String): CuratedCollectionsCopy = localizedValueOrEnglish(curatedCollectionsCopies, code)

internal fun curatedCollectionsLocalizationCodes(): Set<String> = curatedCollectionsCopies.keys
