package com.luc4n3x.levyra.ui.i18n

internal data class ExploreEditorialCopy(
    val newsTitle: String,
    val newsMore: String,
    val releasesTitle: String,
    val justNow: String,
    val minutesAgo: String,
    val hoursAgo: String,
    val daysAgo: String,
    val readStory: String
)

private val exploreEditorialCopies = mapOf(
    "en" to ExploreEditorialCopy("Music news", "More stories", "This week's releases", "Just now", "{count} min ago", "{count} h ago", "{count} d ago", "Read story"),
    "it" to ExploreEditorialCopy("Notizie musicali", "Altre notizie", "Le uscite della settimana", "Adesso", "{count} min fa", "{count} h fa", "{count} g fa", "Leggi l'articolo"),
    "es" to ExploreEditorialCopy("Noticias musicales", "Más noticias", "Lanzamientos de la semana", "Ahora", "hace {count} min", "hace {count} h", "hace {count} d", "Leer artículo"),
    "fr" to ExploreEditorialCopy("Actus musique", "Plus d'actus", "Les sorties de la semaine", "À l'instant", "il y a {count} min", "il y a {count} h", "il y a {count} j", "Lire l'article"),
    "de" to ExploreEditorialCopy("Musiknews", "Weitere Meldungen", "Neuheiten der Woche", "Gerade eben", "vor {count} Min.", "vor {count} Std.", "vor {count} T.", "Artikel lesen"),
    "pt" to ExploreEditorialCopy("Notícias de música", "Mais notícias", "Lançamentos da semana", "Agora mesmo", "há {count} min", "há {count} h", "há {count} d", "Ler artigo"),
    "pt-BR" to ExploreEditorialCopy("Notícias de música", "Mais notícias", "Lançamentos da semana", "Agora mesmo", "há {count} min", "há {count} h", "há {count} d", "Ler matéria"),
    "nl" to ExploreEditorialCopy("Muzieknieuws", "Meer verhalen", "Releases van de week", "Zojuist", "{count} min geleden", "{count} u geleden", "{count} d geleden", "Artikel lezen"),
    "pl" to ExploreEditorialCopy("Wiadomości muzyczne", "Więcej wiadomości", "Premiery tygodnia", "Przed chwilą", "{count} min temu", "{count} godz. temu", "{count} dn. temu", "Czytaj artykuł"),
    "ro" to ExploreEditorialCopy("Știri muzicale", "Mai multe știri", "Lansările săptămânii", "Chiar acum", "acum {count} min", "acum {count} h", "acum {count} z", "Citește articolul"),
    "el" to ExploreEditorialCopy("Μουσικά νέα", "Περισσότερα νέα", "Οι κυκλοφορίες της εβδομάδας", "Μόλις τώρα", "πριν από {count} λ.", "πριν από {count} ώ.", "πριν από {count} ημ.", "Διάβασε το άρθρο"),
    "sv" to ExploreEditorialCopy("Musiknyheter", "Fler nyheter", "Veckans släpp", "Just nu", "{count} min sedan", "{count} tim sedan", "{count} d sedan", "Läs artikeln"),
    "da" to ExploreEditorialCopy("Musiknyheder", "Flere nyheder", "Ugens udgivelser", "Lige nu", "for {count} min. siden", "for {count} t. siden", "for {count} d. siden", "Læs artiklen"),
    "cs" to ExploreEditorialCopy("Hudební novinky", "Další zprávy", "Novinky týdne", "Právě teď", "před {count} min", "před {count} h", "před {count} d", "Přečíst článek"),
    "sk" to ExploreEditorialCopy("Hudobné novinky", "Ďalšie správy", "Novinky týždňa", "Práve teraz", "pred {count} min", "pred {count} h", "pred {count} d", "Prečítať článok"),
    "hr" to ExploreEditorialCopy("Glazbene vijesti", "Još vijesti", "Izdanja tjedna", "Upravo sada", "prije {count} min", "prije {count} h", "prije {count} d", "Pročitaj članak"),
    "sr" to ExploreEditorialCopy("Музичке вести", "Још вести", "Издања недеље", "Управо сада", "пре {count} мин", "пре {count} ч", "пре {count} д", "Прочитај чланак"),
    "sq" to ExploreEditorialCopy("Lajme muzikore", "Më shumë lajme", "Publikimet e javës", "Tani", "{count} min më parë", "{count} orë më parë", "{count} ditë më parë", "Lexo artikullin"),
    "bg" to ExploreEditorialCopy("Музикални новини", "Още новини", "Издания на седмицата", "Току-що", "преди {count} мин", "преди {count} ч", "преди {count} д", "Прочети статията"),
    "hu" to ExploreEditorialCopy("Zenei hírek", "További hírek", "A hét megjelenései", "Épp most", "{count} perce", "{count} órája", "{count} napja", "Cikk elolvasása"),
    "fi" to ExploreEditorialCopy("Musiikkiuutiset", "Lisää uutisia", "Viikon julkaisut", "Juuri nyt", "{count} min sitten", "{count} t sitten", "{count} pv sitten", "Lue artikkeli"),
    "et" to ExploreEditorialCopy("Muusikauudised", "Veel uudiseid", "Nädala väljaanded", "Just praegu", "{count} min tagasi", "{count} h tagasi", "{count} p tagasi", "Loe artiklit"),
    "nb" to ExploreEditorialCopy("Musikknyheter", "Flere nyheter", "Ukens utgivelser", "Akkurat nå", "for {count} min siden", "for {count} t siden", "for {count} d siden", "Les artikkelen"),
    "ca" to ExploreEditorialCopy("Notícies musicals", "Més notícies", "Novetats de la setmana", "Ara mateix", "fa {count} min", "fa {count} h", "fa {count} d", "Llegeix l'article"),
    "uk" to ExploreEditorialCopy("Музичні новини", "Більше новин", "Релізи тижня", "Щойно", "{count} хв тому", "{count} год тому", "{count} дн тому", "Читати статтю"),
    "ru" to ExploreEditorialCopy("Музыкальные новости", "Ещё новости", "Релизы недели", "Только что", "{count} мин назад", "{count} ч назад", "{count} дн назад", "Читать статью"),
    "kk" to ExploreEditorialCopy("Музыка жаңалықтары", "Тағы жаңалықтар", "Аптаның релиздері", "Жаңа ғана", "{count} мин бұрын", "{count} сағ бұрын", "{count} күн бұрын", "Мақаланы оқу"),
    "tr" to ExploreEditorialCopy("Müzik haberleri", "Daha fazla haber", "Haftanın yayınları", "Az önce", "{count} dk önce", "{count} sa önce", "{count} g önce", "Haberi oku"),
    "ar" to ExploreEditorialCopy("أخبار الموسيقى", "المزيد من الأخبار", "إصدارات الأسبوع", "الآن", "قبل {count} د", "قبل {count} س", "قبل {count} ي", "اقرأ المقال"),
    "fa" to ExploreEditorialCopy("اخبار موسیقی", "خبرهای بیشتر", "انتشارهای این هفته", "همین حالا", "{count} دقیقه پیش", "{count} ساعت پیش", "{count} روز پیش", "خواندن مقاله"),
    "zh" to ExploreEditorialCopy("音乐新闻", "更多新闻", "本周新歌", "刚刚", "{count} 分钟前", "{count} 小时前", "{count} 天前", "阅读文章"),
    "zh-Hant" to ExploreEditorialCopy("音樂新聞", "更多新聞", "本週新歌", "剛剛", "{count} 分鐘前", "{count} 小時前", "{count} 天前", "閱讀文章"),
    "ja" to ExploreEditorialCopy("音楽ニュース", "その他のニュース", "今週のリリース", "たった今", "{count}分前", "{count}時間前", "{count}日前", "記事を読む"),
    "ko" to ExploreEditorialCopy("음악 뉴스", "뉴스 더 보기", "이번 주 발매작", "방금", "{count}분 전", "{count}시간 전", "{count}일 전", "기사 읽기"),
    "hi" to ExploreEditorialCopy("संगीत समाचार", "और खबरें", "इस हफ़्ते की रिलीज़", "अभी", "{count} मिनट पहले", "{count} घंटे पहले", "{count} दिन पहले", "लेख पढ़ें"),
    "bn" to ExploreEditorialCopy("সংগীতের খবর", "আরও খবর", "এই সপ্তাহের রিলিজ", "এইমাত্র", "{count} মিনিট আগে", "{count} ঘণ্টা আগে", "{count} দিন আগে", "নিবন্ধ পড়ুন"),
    "id" to ExploreEditorialCopy("Berita musik", "Berita lainnya", "Rilisan minggu ini", "Baru saja", "{count} mnt lalu", "{count} jam lalu", "{count} hr lalu", "Baca artikel"),
    "ms" to ExploreEditorialCopy("Berita muzik", "Lebih banyak berita", "Keluaran minggu ini", "Baru sahaja", "{count} min lalu", "{count} jam lalu", "{count} hari lalu", "Baca artikel"),
    "vi" to ExploreEditorialCopy("Tin âm nhạc", "Thêm tin", "Phát hành trong tuần", "Vừa xong", "{count} phút trước", "{count} giờ trước", "{count} ngày trước", "Đọc bài viết"),
    "th" to ExploreEditorialCopy("ข่าวเพลง", "ข่าวเพิ่มเติม", "เพลงใหม่ประจำสัปดาห์", "เมื่อสักครู่", "{count} นาทีที่แล้ว", "{count} ชม. ที่แล้ว", "{count} วันที่แล้ว", "อ่านบทความ"),
    "fil" to ExploreEditorialCopy("Balitang musika", "Iba pang balita", "Mga labas ngayong linggo", "Ngayon lang", "{count} min ang nakalipas", "{count} oras ang nakalipas", "{count} araw ang nakalipas", "Basahin ang artikulo"),
    "he" to ExploreEditorialCopy("חדשות מוזיקה", "עוד כתבות", "הוצאות השבוע", "הרגע", "לפני {count} דק׳", "לפני {count} שע׳", "לפני {count} ימים", "קריאת הכתבה")
)

internal fun exploreEditorialCopy(code: String): ExploreEditorialCopy = localizedValueOrEnglish(exploreEditorialCopies, code)

internal fun exploreEditorialLocalizationCodes(): Set<String> = exploreEditorialCopies.keys
