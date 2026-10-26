package com.luc4n3x.levyra.ui.i18n

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog

internal class AppleSearchCopy(
    val hint: String,
    val inCatalog: String,
    val topResults: String,
    val song: String,
    val artist: String,
    val album: String,
    val playlist: String,
    val musicVideo: String
)

internal object LevyraAppleMusicSearchStrings {
    private val copies = mapOf(
        "en" to AppleSearchCopy("Artists, songs, albums and more", "in Levyra", "Top Results", "Song", "Artist", "Album", "Playlist", "Music Video"),
        "it" to AppleSearchCopy("Artisti, brani, album e molto altro", "in Levyra", "Migliori risultati", "Brano", "Artista", "Album", "Playlist", "Video musicale"),
        "es" to AppleSearchCopy("Artistas, canciones, álbumes y más", "en Levyra", "Mejores resultados", "Canción", "Artista", "Álbum", "Playlist", "Videoclip"),
        "fr" to AppleSearchCopy("Artistes, titres, albums et plus", "dans Levyra", "Meilleurs résultats", "Morceau", "Artiste", "Album", "Playlist", "Vidéoclip"),
        "de" to AppleSearchCopy("Künstler, Titel, Alben und mehr", "in Levyra", "Top-Treffer", "Titel", "Künstler", "Album", "Playlist", "Musikvideo"),
        "pt" to AppleSearchCopy("Artistas, músicas, álbuns e mais", "no Levyra", "Melhores resultados", "Música", "Artista", "Álbum", "Playlist", "Videoclipe"),
        "nl" to AppleSearchCopy("Artiesten, nummers, albums en meer", "in Levyra", "Topresultaten", "Nummer", "Artiest", "Album", "Playlist", "Videoclip"),
        "pl" to AppleSearchCopy("Wykonawcy, utwory, albumy i więcej", "w Levyra", "Najlepsze wyniki", "Utwór", "Wykonawca", "Album", "Playlista", "Teledysk"),
        "ru" to AppleSearchCopy("Артисты, песни, альбомы и другое", "в Levyra", "Лучшие результаты", "Песня", "Артист", "Альбом", "Плейлист", "Клип"),
        "uk" to AppleSearchCopy("Виконавці, пісні, альбоми та інше", "у Levyra", "Найкращі результати", "Пісня", "Виконавець", "Альбом", "Плейлист", "Кліп"),
        "ja" to AppleSearchCopy("アーティスト、曲、アルバムなど", "Levyra内", "トップ結果", "曲", "アーティスト", "アルバム", "プレイリスト", "ミュージックビデオ"),
        "ko" to AppleSearchCopy("아티스트, 노래, 앨범 등", "Levyra에서", "상위 결과", "노래", "아티스트", "앨범", "플레이리스트", "뮤직비디오"),
        "zh" to AppleSearchCopy("艺人、歌曲、专辑等", "在 Levyra 中", "最佳结果", "歌曲", "艺人", "专辑", "歌单", "音乐视频"),
        "ar" to AppleSearchCopy("الفنانون والأغاني والألبومات والمزيد", "في Levyra", "أفضل النتائج", "أغنية", "فنان", "ألبوم", "قائمة تشغيل", "فيديو موسيقي"),
        "ro" to AppleSearchCopy("Artiști, melodii, albume și altele", "în Levyra", "Rezultate de top", "Melodie", "Artist", "Album", "Playlist", "Videoclip"),
        "sq" to AppleSearchCopy("Artistë, këngë, albume dhe më shumë", "në Levyra", "Rezultatet kryesore", "Këngë", "Artist", "Album", "Listë luajtjeje", "Videoklip"),
        "bg" to AppleSearchCopy("Изпълнители, песни, албуми и още", "в Levyra", "Най-добри резултати", "Песен", "Изпълнител", "Албум", "Плейлист", "Видеоклип"),
        "el" to AppleSearchCopy("Καλλιτέχνες, τραγούδια, άλμπουμ και άλλα", "στο Levyra", "Κορυφαία αποτελέσματα", "Τραγούδι", "Καλλιτέχνης", "Άλμπουμ", "Playlist", "Βίντεο κλιπ"),
        "sv" to AppleSearchCopy("Artister, låtar, album och mer", "i Levyra", "Toppresultat", "Låt", "Artist", "Album", "Spellista", "Musikvideo"),
        "da" to AppleSearchCopy("Kunstnere, sange, albums og mere", "i Levyra", "Topresultater", "Sang", "Kunstner", "Album", "Playliste", "Musikvideo"),
        "cs" to AppleSearchCopy("Interpreti, skladby, alba a další", "v Levyra", "Nejlepší výsledky", "Skladba", "Interpret", "Album", "Playlist", "Videoklip"),
        "sk" to AppleSearchCopy("Interpreti, skladby, albumy a ďalšie", "v Levyra", "Najlepšie výsledky", "Skladba", "Interpret", "Album", "Playlist", "Videoklip"),
        "bn" to AppleSearchCopy("শিল্পী, গান, অ্যালবাম এবং আরও", "Levyra-তে", "সেরা ফলাফল", "গান", "শিল্পী", "অ্যালবাম", "প্লেলিস্ট", "মিউজিক ভিডিও"),
        "kk" to AppleSearchCopy("Орындаушылар, әндер, альбомдар және т.б.", "Levyra ішінде", "Үздік нәтижелер", "Ән", "Орындаушы", "Альбом", "Плейлист", "Клип"),
        "sr" to AppleSearchCopy("Izvođači, pesme, albumi i još", "u Levyra", "Najbolji rezultati", "Pesma", "Izvođač", "Album", "Plejlista", "Spot"),
        "hr" to AppleSearchCopy("Izvođači, pjesme, albumi i više", "u Levyra", "Najbolji rezultati", "Pjesma", "Izvođač", "Album", "Popis pjesama", "Spot"),
        "hu" to AppleSearchCopy("Előadók, dalok, albumok és egyebek", "a Levyrában", "Legjobb találatok", "Dal", "Előadó", "Album", "Lejátszási lista", "Videoklip"),
        "fi" to AppleSearchCopy("Artistit, kappaleet, albumit ja muuta", "Levyrassa", "Parhaat tulokset", "Kappale", "Artisti", "Albumi", "Soittolista", "Musiikkivideo"),
        "et" to AppleSearchCopy("Esitajad, lood, albumid ja muu", "Levyras", "Parimad tulemused", "Lugu", "Esitaja", "Album", "Esitusloend", "Muusikavideo"),
        "nb" to AppleSearchCopy("Artister, sanger, album og mer", "i Levyra", "Toppresultater", "Sang", "Artist", "Album", "Spilleliste", "Musikkvideo"),
        "ca" to AppleSearchCopy("Artistes, cançons, àlbums i més", "a Levyra", "Millors resultats", "Cançó", "Artista", "Àlbum", "Llista", "Videoclip"),
        "tr" to AppleSearchCopy("Sanatçılar, şarkılar, albümler ve fazlası", "Levyra'da", "En iyi sonuçlar", "Şarkı", "Sanatçı", "Albüm", "Çalma listesi", "Müzik videosu"),
        "fa" to AppleSearchCopy("هنرمندان، آهنگ‌ها، آلبوم‌ها و بیشتر", "در Levyra", "برترین نتایج", "آهنگ", "هنرمند", "آلبوم", "پلی‌لیست", "موزیک ویدیو"),
        "hi" to AppleSearchCopy("कलाकार, गाने, एल्बम और बहुत कुछ", "Levyra में", "शीर्ष परिणाम", "गाना", "कलाकार", "एल्बम", "प्लेलिस्ट", "म्यूज़िक वीडियो"),
        "id" to AppleSearchCopy("Artis, lagu, album, dan lainnya", "di Levyra", "Hasil teratas", "Lagu", "Artis", "Album", "Playlist", "Video musik"),
        "ms" to AppleSearchCopy("Artis, lagu, album dan banyak lagi", "dalam Levyra", "Hasil teratas", "Lagu", "Artis", "Album", "Senarai main", "Video muzik"),
        "vi" to AppleSearchCopy("Nghệ sĩ, bài hát, album và hơn thế", "trong Levyra", "Kết quả hàng đầu", "Bài hát", "Nghệ sĩ", "Album", "Danh sách phát", "Video âm nhạc"),
        "th" to AppleSearchCopy("ศิลปิน เพลง อัลบั้ม และอื่นๆ", "ใน Levyra", "ผลลัพธ์ยอดนิยม", "เพลง", "ศิลปิน", "อัลบั้ม", "เพลย์ลิสต์", "มิวสิควิดีโอ"),
        "fil" to AppleSearchCopy("Mga artist, kanta, album at iba pa", "sa Levyra", "Mga nangungunang resulta", "Kanta", "Artist", "Album", "Playlist", "Music video"),
        "he" to AppleSearchCopy("אמנים, שירים, אלבומים ועוד", "ב-Levyra", "התוצאות המובילות", "שיר", "אמן", "אלבום", "פלייליסט", "קליפ")
    )

    fun forCode(code: String): AppleSearchCopy = copies[LevyraLanguageCatalog.normalize(code)] ?: copies.getValue("en")
}
