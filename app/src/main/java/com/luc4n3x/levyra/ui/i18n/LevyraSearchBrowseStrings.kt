package com.luc4n3x.levyra.ui.i18n

internal data class SearchBrowseCopy(
    val browseTitle: String,
    val newReleasesDetail: String,
    val moodsAndGenres: String,
    val levyraPicks: String
)

private val searchBrowseCopies = mapOf(
    "en" to SearchBrowseCopy("Browse", "The latest albums and singles", "Moods & genres", "Levyra picks"),
    "it" to SearchBrowseCopy("Sfoglia", "Gli ultimi album e singoli", "Mood e generi", "Scelti da Levyra"),
    "es" to SearchBrowseCopy("Explorar", "Los últimos álbumes y sencillos", "Estados de ánimo y géneros", "Selección de Levyra"),
    "fr" to SearchBrowseCopy("Parcourir", "Les derniers albums et singles", "Ambiances et genres", "La sélection Levyra"),
    "de" to SearchBrowseCopy("Stöbern", "Die neuesten Alben und Singles", "Stimmungen & Genres", "Levyra-Auswahl"),
    "pt" to SearchBrowseCopy("Explorar", "Os álbuns e singles mais recentes", "Estados de espírito e géneros", "Escolhas Levyra"),
    "pt-BR" to SearchBrowseCopy("Explorar", "Os álbuns e singles mais recentes", "Climas e gêneros", "Escolhas da Levyra"),
    "nl" to SearchBrowseCopy("Bladeren", "De nieuwste albums en singles", "Stemmingen & genres", "Levyra-selectie"),
    "pl" to SearchBrowseCopy("Przeglądaj", "Najnowsze albumy i single", "Nastroje i gatunki", "Wybór Levyra"),
    "ro" to SearchBrowseCopy("Răsfoiește", "Cele mai noi albume și single-uri", "Stări și genuri", "Selecția Levyra"),
    "el" to SearchBrowseCopy("Περιήγηση", "Τα πιο πρόσφατα άλμπουμ και σινγκλ", "Διαθέσεις και είδη", "Επιλογές Levyra"),
    "sv" to SearchBrowseCopy("Bläddra", "De senaste albumen och singlarna", "Stämningar och genrer", "Levyras urval"),
    "da" to SearchBrowseCopy("Gennemse", "De nyeste album og singler", "Stemninger og genrer", "Levyras udvalg"),
    "cs" to SearchBrowseCopy("Procházet", "Nejnovější alba a singly", "Nálady a žánry", "Výběr Levyra"),
    "sk" to SearchBrowseCopy("Prehliadať", "Najnovšie albumy a single", "Nálady a žánre", "Výber Levyra"),
    "hr" to SearchBrowseCopy("Pregledaj", "Najnoviji albumi i singlovi", "Raspoloženja i žanrovi", "Levyra izdvaja"),
    "sr" to SearchBrowseCopy("Прегледај", "Најновији албуми и синглови", "Расположења и жанрови", "Levyra издваја"),
    "sq" to SearchBrowseCopy("Shfleto", "Albumet dhe këngët më të reja", "Gjendje dhe zhanre", "Zgjedhjet e Levyra"),
    "bg" to SearchBrowseCopy("Разгледай", "Най-новите албуми и сингли", "Настроения и жанрове", "Избрано от Levyra"),
    "hu" to SearchBrowseCopy("Böngészés", "A legújabb albumok és kislemezek", "Hangulatok és műfajok", "Levyra válogatás"),
    "fi" to SearchBrowseCopy("Selaa", "Uusimmat albumit ja singlet", "Tunnelmat ja tyylilajit", "Levyran valinnat"),
    "et" to SearchBrowseCopy("Sirvi", "Uusimad albumid ja singlid", "Meeleolud ja žanrid", "Levyra valik"),
    "nb" to SearchBrowseCopy("Bla gjennom", "De nyeste albumene og singlene", "Stemninger og sjangre", "Levyras utvalg"),
    "ca" to SearchBrowseCopy("Explora", "Els últims àlbums i senzills", "Estats d'ànim i gèneres", "Selecció de Levyra"),
    "uk" to SearchBrowseCopy("Огляд", "Найновіші альбоми й сингли", "Настрої та жанри", "Вибір Levyra"),
    "ru" to SearchBrowseCopy("Обзор", "Свежие альбомы и синглы", "Настроения и жанры", "Выбор Levyra"),
    "kk" to SearchBrowseCopy("Шолу", "Ең жаңа альбомдар мен синглдар", "Көңіл-күй және жанрлар", "Levyra таңдауы"),
    "tr" to SearchBrowseCopy("Göz at", "En yeni albümler ve single'lar", "Ruh halleri ve türler", "Levyra seçkisi"),
    "ar" to SearchBrowseCopy("تصفّح", "أحدث الألبومات والأغاني المنفردة", "الأمزجة والأنواع", "اختيارات Levyra"),
    "fa" to SearchBrowseCopy("مرور", "تازه‌ترین آلبوم‌ها و تک‌آهنگ‌ها", "حال‌وهوا و سبک‌ها", "انتخاب‌های Levyra"),
    "zh" to SearchBrowseCopy("浏览", "最新专辑与单曲", "心情与流派", "Levyra 精选"),
    "zh-Hant" to SearchBrowseCopy("瀏覽", "最新專輯與單曲", "心情與曲風", "Levyra 精選"),
    "ja" to SearchBrowseCopy("ブラウズ", "最新のアルバムとシングル", "ムードとジャンル", "Levyra のおすすめ"),
    "ko" to SearchBrowseCopy("둘러보기", "최신 앨범과 싱글", "무드 및 장르", "Levyra 추천"),
    "hi" to SearchBrowseCopy("ब्राउज़ करें", "नए एल्बम और सिंगल", "मूड और शैलियाँ", "Levyra की पसंद"),
    "bn" to SearchBrowseCopy("ব্রাউজ করুন", "নতুন অ্যালবাম ও সিঙ্গেল", "মুড ও ঘরানা", "Levyra-র বাছাই"),
    "id" to SearchBrowseCopy("Jelajahi", "Album dan single terbaru", "Suasana & genre", "Pilihan Levyra"),
    "ms" to SearchBrowseCopy("Semak imbas", "Album dan single terkini", "Suasana & genre", "Pilihan Levyra"),
    "vi" to SearchBrowseCopy("Duyệt", "Album và đĩa đơn mới nhất", "Tâm trạng và thể loại", "Levyra chọn lọc"),
    "th" to SearchBrowseCopy("เรียกดู", "อัลบั้มและซิงเกิลล่าสุด", "อารมณ์และแนวเพลง", "Levyra คัดสรร"),
    "fil" to SearchBrowseCopy("Mag-browse", "Mga pinakabagong album at single", "Mga mood at genre", "Mga pili ng Levyra"),
    "he" to SearchBrowseCopy("עיון", "האלבומים והסינגלים החדשים ביותר", "מצבי רוח וז'אנרים", "הבחירות של Levyra")
)

internal fun searchBrowseCopy(code: String): SearchBrowseCopy = localizedValueOrEnglish(searchBrowseCopies, code)

internal fun searchBrowseLocalizationCodes(): Set<String> = searchBrowseCopies.keys
