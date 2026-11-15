package com.luc4n3x.levyra.ui.i18n

internal data class CuratedCollectionsCopy(
    val decadesTitle: String,
    val decadesSubtitle: String,
    val everydayTitle: String,
    val everydaySubtitle: String,
    val browseTitle: String,
    val browseSubtitle: String
)

private val curatedCollectionsCopies = mapOf(
    "en" to CuratedCollectionsCopy("Decades", "Music through the decades", "For every moment", "Playlists for your day", "More playlists", "By mood, genre and decade"),
    "it" to CuratedCollectionsCopy("Decenni", "La musica dei decenni", "Per ogni momento", "Playlist per la giornata", "Altre playlist", "Per mood, genere e decennio"),
    "es" to CuratedCollectionsCopy("Décadas", "Música de cada década", "Para cada momento", "Playlists para tu día", "Más playlists", "Por ánimo, género y década"),
    "fr" to CuratedCollectionsCopy("Décennies", "La musique des décennies", "Pour chaque moment", "Playlists pour ta journée", "Plus de playlists", "Par humeur, genre et décennie"),
    "de" to CuratedCollectionsCopy("Jahrzehnte", "Musik der Jahrzehnte", "Für jeden Moment", "Playlists für deinen Tag", "Mehr Playlists", "Nach Stimmung, Genre, Jahrzehnt"),
    "pt" to CuratedCollectionsCopy("Décadas", "Música de várias décadas", "Para cada momento", "Playlists para o seu dia", "Mais playlists", "Por humor, género e década"),
    "pt-BR" to CuratedCollectionsCopy("Décadas", "Música de cada década", "Pra cada momento", "Playlists pro seu dia", "Mais playlists", "Por humor, gênero e década"),
    "nl" to CuratedCollectionsCopy("Decennia", "Muziek door de decennia", "Voor elk moment", "Playlists voor je dag", "Meer playlists", "Op stemming, genre en decennium"),
    "pl" to CuratedCollectionsCopy("Dekady", "Muzyka różnych dekad", "Na każdą chwilę", "Playlisty na cały dzień", "Więcej playlist", "Wg nastroju, gatunku i dekady"),
    "ro" to CuratedCollectionsCopy("Decenii", "Muzica deceniilor", "Pentru orice moment", "Playlisturi pentru ziua ta", "Mai multe playlisturi", "După stare, gen și deceniu"),
    "el" to CuratedCollectionsCopy("Δεκαετίες", "Μουσική των δεκαετιών", "Για κάθε στιγμή", "Playlist για τη μέρα σου", "Περισσότερες λίστες", "Ανά διάθεση, είδος, δεκαετία"),
    "sv" to CuratedCollectionsCopy("Decennier", "Musik genom decennierna", "För varje stund", "Spellistor för din dag", "Fler spellistor", "Efter stämning, genre, decennium"),
    "da" to CuratedCollectionsCopy("Årtier", "Musik gennem årtierne", "Til hvert øjeblik", "Playlister til din dag", "Flere playlister", "Efter stemning, genre og årti"),
    "cs" to CuratedCollectionsCopy("Dekády", "Hudba napříč dekádami", "Pro každou chvíli", "Playlisty na celý den", "Další playlisty", "Podle nálady, žánru a dekády"),
    "uk" to CuratedCollectionsCopy("Десятиліття", "Музика різних десятиліть", "Для будь-якої миті", "Плейлисти на весь день", "Більше плейлистів", "За настроєм, жанром і епохою"),
    "ru" to CuratedCollectionsCopy("Десятилетия", "Музыка разных десятилетий", "На каждый момент", "Плейлисты на весь день", "Больше плейлистов", "По настроению, жанру и эпохе"),
    "nb" to CuratedCollectionsCopy("Tiår", "Musikk gjennom tiårene", "For hvert øyeblikk", "Spillelister for dagen", "Flere spillelister", "Etter stemning, sjanger og tiår"),
    "ca" to CuratedCollectionsCopy("Dècades", "Música de cada dècada", "Per a cada moment", "Llistes per al teu dia", "Més llistes", "Per estat, gènere i dècada"),
    "kk" to CuratedCollectionsCopy("Онжылдықтар", "Әр онжылдықтың музыкасы", "Әр сәтке", "Күніңізге арналған плейлистер", "Көбірек плейлист", "Көңіл күй, жанр және онжылдық"),
    "tr" to CuratedCollectionsCopy("On yıllar", "On yılların müziği", "Her an için", "Gününüz için listeler", "Daha fazla çalma listesi", "Ruh hâli, tür ve onyıla göre"),
    "ar" to CuratedCollectionsCopy("العقود", "موسيقى عبر العقود", "لكل لحظة", "قوائم تشغيل ليومك", "المزيد من القوائم", "حسب المزاج والنوع والعقد"),
    "fa" to CuratedCollectionsCopy("دهه‌ها", "موسیقی دهه‌ها", "برای هر لحظه", "فهرست‌های پخش روزانه", "فهرست‌های بیشتر", "بر اساس حال‌وهوا، سبک و دهه"),
    "zh" to CuratedCollectionsCopy("年代", "穿越年代的音乐", "每个时刻", "陪你度过每一天", "更多歌单", "按心情、流派和年代"),
    "zh-Hant" to CuratedCollectionsCopy("年代", "穿越年代的音樂", "每個時刻", "陪你度過每一天", "更多播放清單", "依心情、曲風和年代"),
    "ja" to CuratedCollectionsCopy("年代別", "年代をめぐる音楽", "あらゆる瞬間に", "毎日のプレイリスト", "その他のプレイリスト", "ムード・ジャンル・年代別"),
    "ko" to CuratedCollectionsCopy("시대별", "시대를 아우르는 음악", "모든 순간을 위해", "하루를 위한 플레이리스트", "플레이리스트 더 보기", "분위기, 장르, 시대별"),
    "hi" to CuratedCollectionsCopy("दशक", "दशकों का संगीत", "हर पल के लिए", "आपके दिन की प्लेलिस्ट", "और प्लेलिस्ट", "मूड, शैली और दशक के अनुसार"),
    "bn" to CuratedCollectionsCopy("দশক", "দশকের পর দশকের গান", "প্রতিটি মুহূর্তের জন্য", "আপনার দিনের প্লেলিস্ট", "আরও প্লেলিস্ট", "মুড, ধারা ও দশক অনুযায়ী"),
    "id" to CuratedCollectionsCopy("Dekade", "Musik lintas dekade", "Untuk setiap momen", "Playlist untuk harimu", "Playlist lainnya", "Menurut suasana, genre, dekade"),
    "ms" to CuratedCollectionsCopy("Dekad", "Muzik merentas dekad", "Untuk setiap detik", "Senarai main harian", "Lagi senarai main", "Ikut suasana, genre dan dekad"),
    "vi" to CuratedCollectionsCopy("Thập niên", "Âm nhạc qua các thập niên", "Cho mọi khoảnh khắc", "Playlist cho ngày của bạn", "Thêm danh sách phát", "Tâm trạng, thể loại và thập niên"),
    "th" to CuratedCollectionsCopy("ยุคสมัย", "เพลงข้ามทศวรรษ", "ทุกช่วงเวลา", "เพลย์ลิสต์สำหรับวันของคุณ", "เพลย์ลิสต์เพิ่มเติม", "ตามอารมณ์ แนวเพลง และยุค"),
    "fil" to CuratedCollectionsCopy("Mga dekada", "Musika ng bawat dekada", "Sa bawat sandali", "Mga playlist sa araw mo", "Iba pang playlist", "Ayon sa mood, genre at dekada"),
    "he" to CuratedCollectionsCopy("עשורים", "מוזיקה לאורך העשורים", "לכל רגע", "פלייליסטים ליום שלך", "עוד פלייליסטים", "לפי מצב רוח, ז'אנר ועשור"),
    "hu" to CuratedCollectionsCopy("Évtizedek", "Az évtizedek zenéje", "Minden pillanatra", "Listák a napodhoz", "További lejátszási listák", "Hangulat, műfaj, évtized szerint"),
    "fi" to CuratedCollectionsCopy("Vuosikymmenet", "Musiikkia vuosikymmeniltä", "Joka hetkeen", "Soittolistat päivääsi", "Lisää soittolistoja", "Tunnelman, tyylin ja ajan mukaan"),
    "bg" to CuratedCollectionsCopy("Десетилетия", "Музика през десетилетията", "За всеки момент", "Плейлисти за деня ти", "Още плейлисти", "По настроение, жанр, десетилетие"),
    "hr" to CuratedCollectionsCopy("Desetljeća", "Glazba kroz desetljeća", "Za svaki trenutak", "Playliste za tvoj dan", "Još playlista", "Po ugođaju, žanru i desetljeću"),
    "sr" to CuratedCollectionsCopy("Деценије", "Музика кроз деценије", "За сваки тренутак", "Плејлисте за твој дан", "Још плејлиста", "По расположењу, жанру и деценији"),
    "sk" to CuratedCollectionsCopy("Dekády", "Hudba naprieč dekádami", "Na každú chvíľu", "Playlisty na celý deň", "Ďalšie playlisty", "Podľa nálady, žánru a dekády"),
    "et" to CuratedCollectionsCopy("Kümnendid", "Muusika läbi kümnendite", "Igaks hetkeks", "Esitusloendid päevaks", "Veel esitusloendeid", "Meeleolu, žanri, kümnendi järgi"),
    "sq" to CuratedCollectionsCopy("Dekadat", "Muzika ndër dekada", "Për çdo moment", "Playlist për ditën tënde", "Më shumë playlista", "Sipas humorit, zhanrit, dekadës")
)

internal fun curatedCollectionsCopy(code: String): CuratedCollectionsCopy = localizedValueOrEnglish(curatedCollectionsCopies, code)

internal fun curatedCollectionsLocalizationCodes(): Set<String> = curatedCollectionsCopies.keys
