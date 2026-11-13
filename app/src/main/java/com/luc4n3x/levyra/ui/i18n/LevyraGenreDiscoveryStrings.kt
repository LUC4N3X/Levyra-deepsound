package com.luc4n3x.levyra.ui.i18n

internal data class GenreDiscoveryCopy(
    val aboutTitle: String,
    val readMore: String,
    val readLess: String,
    val aboutCredit: String,
    val artistsTitle: String,
    val albumsTitle: String,
    val appleCredit: String
)

private val genreDiscoveryCopies = mapOf(
    "en" to GenreDiscoveryCopy("The genre in brief", "Read more", "Show less", "Text: Last.fm, CC BY-SA", "Artists to discover", "Albums fans love", "Editorial selections: Apple Music"),
    "it" to GenreDiscoveryCopy("Il genere in breve", "Leggi tutto", "Mostra meno", "Testo: Last.fm, CC BY-SA", "Artisti da scoprire", "Gli album più amati", "Selezioni editoriali: Apple Music"),
    "es" to GenreDiscoveryCopy("El género en breve", "Leer más", "Ver menos", "Texto: Last.fm, CC BY-SA", "Artistas por descubrir", "Los álbumes más queridos", "Selecciones editoriales: Apple Music"),
    "fr" to GenreDiscoveryCopy("Le genre en bref", "Lire la suite", "Voir moins", "Texte : Last.fm, CC BY-SA", "Artistes à découvrir", "Les albums préférés du public", "Sélections éditoriales : Apple Music"),
    "de" to GenreDiscoveryCopy("Das Genre im Überblick", "Weiterlesen", "Weniger anzeigen", "Text: Last.fm, CC BY-SA", "Künstler zum Entdecken", "Die beliebtesten Alben", "Redaktionelle Auswahl: Apple Music"),
    "pt" to GenreDiscoveryCopy("O género em resumo", "Ler mais", "Ver menos", "Texto: Last.fm, CC BY-SA", "Artistas a descobrir", "Os álbuns mais adorados", "Seleções editoriais: Apple Music"),
    "pt-BR" to GenreDiscoveryCopy("O gênero em resumo", "Ler mais", "Ver menos", "Texto: Last.fm, CC BY-SA", "Artistas para descobrir", "Os álbuns mais amados", "Seleções editoriais: Apple Music"),
    "nl" to GenreDiscoveryCopy("Het genre in het kort", "Lees meer", "Minder tonen", "Tekst: Last.fm, CC BY-SA", "Artiesten om te ontdekken", "De meest geliefde albums", "Redactionele selecties: Apple Music"),
    "pl" to GenreDiscoveryCopy("Gatunek w skrócie", "Czytaj dalej", "Pokaż mniej", "Tekst: Last.fm, CC BY-SA", "Artyści do odkrycia", "Najbardziej lubiane albumy", "Wybór redakcji: Apple Music"),
    "ro" to GenreDiscoveryCopy("Genul pe scurt", "Citește mai mult", "Arată mai puțin", "Text: Last.fm, CC BY-SA", "Artiști de descoperit", "Cele mai iubite albume", "Selecții editoriale: Apple Music"),
    "el" to GenreDiscoveryCopy("Το είδος με λίγα λόγια", "Διαβάστε περισσότερα", "Λιγότερα", "Κείμενο: Last.fm, CC BY-SA", "Καλλιτέχνες για να ανακαλύψετε", "Τα πιο αγαπημένα άλμπουμ", "Επιλογές συντακτών: Apple Music"),
    "sv" to GenreDiscoveryCopy("Genren i korthet", "Läs mer", "Visa mindre", "Text: Last.fm, CC BY-SA", "Artister att upptäcka", "De mest älskade albumen", "Redaktionella urval: Apple Music"),
    "da" to GenreDiscoveryCopy("Genren kort fortalt", "Læs mere", "Vis mindre", "Tekst: Last.fm, CC BY-SA", "Kunstnere at opdage", "De mest elskede album", "Redaktionelle udvalg: Apple Music"),
    "cs" to GenreDiscoveryCopy("Žánr v kostce", "Číst dál", "Zobrazit méně", "Text: Last.fm, CC BY-SA", "Interpreti k objevení", "Nejoblíbenější alba", "Výběr redakce: Apple Music"),
    "uk" to GenreDiscoveryCopy("Жанр коротко", "Читати далі", "Показати менше", "Текст: Last.fm, CC BY-SA", "Виконавці, яких варто відкрити", "Найулюбленіші альбоми", "Добірки редакції: Apple Music"),
    "ru" to GenreDiscoveryCopy("Жанр вкратце", "Читать далее", "Свернуть", "Текст: Last.fm, CC BY-SA", "Исполнители, которых стоит открыть", "Самые любимые альбомы", "Подборки редакции: Apple Music"),
    "nb" to GenreDiscoveryCopy("Sjangeren kort fortalt", "Les mer", "Vis mindre", "Tekst: Last.fm, CC BY-SA", "Artister å oppdage", "De mest elskede albumene", "Redaksjonelle utvalg: Apple Music"),
    "ca" to GenreDiscoveryCopy("El gènere en breu", "Llegeix-ne més", "Mostra'n menys", "Text: Last.fm, CC BY-SA", "Artistes per descobrir", "Els àlbums més estimats", "Seleccions editorials: Apple Music"),
    "kk" to GenreDiscoveryCopy("Жанр қысқаша", "Толығырақ оқу", "Азырақ көрсету", "Мәтін: Last.fm, CC BY-SA", "Танысуға тұрарлық орындаушылар", "Ең сүйікті альбомдар", "Редакция таңдауы: Apple Music"),
    "tr" to GenreDiscoveryCopy("Kısaca bu tür", "Devamını oku", "Daha az göster", "Metin: Last.fm, CC BY-SA", "Keşfedilecek sanatçılar", "En sevilen albümler", "Editör seçkileri: Apple Music"),
    "ar" to GenreDiscoveryCopy("النوع باختصار", "اقرأ المزيد", "عرض أقل", "النص: Last.fm، CC BY-SA", "فنانون جديرون بالاكتشاف", "ألبومات يعشقها المستمعون", "اختيارات تحريرية: Apple Music"),
    "fa" to GenreDiscoveryCopy("این سبک به‌اختصار", "بیشتر بخوانید", "نمایش کمتر", "متن: Last.fm، CC BY-SA", "هنرمندانی برای کشف", "محبوب‌ترین آلبوم‌ها", "گزیده‌های سردبیری: Apple Music"),
    "zh" to GenreDiscoveryCopy("曲风速览", "阅读更多", "收起", "文字：Last.fm，CC BY-SA", "值得发现的艺人", "最受喜爱的专辑", "编辑精选：Apple Music"),
    "zh-Hant" to GenreDiscoveryCopy("曲風速覽", "閱讀更多", "收起", "文字：Last.fm，CC BY-SA", "值得探索的藝人", "最受喜愛的專輯", "編輯精選：Apple Music"),
    "ja" to GenreDiscoveryCopy("ジャンル紹介", "続きを読む", "閉じる", "テキスト: Last.fm、CC BY-SA", "注目のアーティスト", "愛されているアルバム", "編集部のセレクション: Apple Music"),
    "ko" to GenreDiscoveryCopy("장르 한눈에 보기", "더 읽기", "간단히 보기", "글: Last.fm, CC BY-SA", "새로 만날 아티스트", "가장 사랑받는 앨범", "에디터 추천: Apple Music"),
    "hi" to GenreDiscoveryCopy("शैली एक नज़र में", "और पढ़ें", "कम दिखाएँ", "पाठ: Last.fm, CC BY-SA", "खोजने लायक कलाकार", "सबसे पसंदीदा एल्बम", "संपादकीय चयन: Apple Music"),
    "bn" to GenreDiscoveryCopy("এক নজরে ঘরানা", "আরও পড়ুন", "কম দেখান", "লেখা: Last.fm, CC BY-SA", "আবিষ্কার করার মতো শিল্পী", "সবচেয়ে প্রিয় অ্যালবাম", "সম্পাদকীয় বাছাই: Apple Music"),
    "id" to GenreDiscoveryCopy("Sekilas tentang genre", "Baca selengkapnya", "Tampilkan lebih sedikit", "Teks: Last.fm, CC BY-SA", "Artis untuk ditemukan", "Album paling disukai", "Pilihan editorial: Apple Music"),
    "ms" to GenreDiscoveryCopy("Genre secara ringkas", "Baca lagi", "Tunjukkan kurang", "Teks: Last.fm, CC BY-SA", "Artis untuk diterokai", "Album paling digemari", "Pilihan editorial: Apple Music"),
    "vi" to GenreDiscoveryCopy("Tóm tắt về thể loại", "Đọc thêm", "Thu gọn", "Văn bản: Last.fm, CC BY-SA", "Nghệ sĩ để khám phá", "Những album được yêu thích nhất", "Tuyển chọn của biên tập viên: Apple Music"),
    "th" to GenreDiscoveryCopy("แนวเพลงโดยสังเขป", "อ่านเพิ่มเติม", "แสดงน้อยลง", "ข้อความ: Last.fm, CC BY-SA", "ศิลปินที่น่าค้นพบ", "อัลบั้มที่คนรักมากที่สุด", "คัดสรรโดยบรรณาธิการ: Apple Music"),
    "fil" to GenreDiscoveryCopy("Ang genre sa madaling salita", "Magbasa pa", "Ipakita nang mas kaunti", "Teksto: Last.fm, CC BY-SA", "Mga artist na dapat tuklasin", "Mga pinakamamahal na album", "Mga piling editoryal: Apple Music"),
    "he" to GenreDiscoveryCopy("הז׳אנר בקצרה", "קראו עוד", "הצג פחות", "טקסט: Last.fm, CC BY-SA", "אמנים שכדאי לגלות", "האלבומים האהובים ביותר", "בחירות העורכים: Apple Music"),
    "hu" to GenreDiscoveryCopy("A műfaj röviden", "Tovább olvasom", "Kevesebb", "Szöveg: Last.fm, CC BY-SA", "Felfedezendő előadók", "A legkedveltebb albumok", "Szerkesztői válogatások: Apple Music"),
    "fi" to GenreDiscoveryCopy("Tyylilaji lyhyesti", "Lue lisää", "Näytä vähemmän", "Teksti: Last.fm, CC BY-SA", "Löydettäviä artisteja", "Rakastetuimmat albumit", "Toimituksen valinnat: Apple Music"),
    "bg" to GenreDiscoveryCopy("Жанрът накратко", "Прочетете още", "Покажи по-малко", "Текст: Last.fm, CC BY-SA", "Изпълнители за откриване", "Най-обичаните албуми", "Редакционни подборки: Apple Music"),
    "hr" to GenreDiscoveryCopy("Žanr ukratko", "Pročitaj više", "Prikaži manje", "Tekst: Last.fm, CC BY-SA", "Izvođači za otkrivanje", "Najomiljeniji albumi", "Urednički odabiri: Apple Music"),
    "sr" to GenreDiscoveryCopy("Жанр укратко", "Прочитај више", "Прикажи мање", "Текст: Last.fm, CC BY-SA", "Извођачи за откривање", "Најомиљенији албуми", "Уреднички избор: Apple Music"),
    "sk" to GenreDiscoveryCopy("Žáner v skratke", "Čítať ďalej", "Zobraziť menej", "Text: Last.fm, CC BY-SA", "Interpreti na objavenie", "Najobľúbenejšie albumy", "Výber redakcie: Apple Music"),
    "et" to GenreDiscoveryCopy("Žanr lühidalt", "Loe edasi", "Näita vähem", "Tekst: Last.fm, CC BY-SA", "Avastamist väärt artistid", "Kõige armastatumad albumid", "Toimetuse valikud: Apple Music"),
    "sq" to GenreDiscoveryCopy("Zhanri shkurt", "Lexo më shumë", "Shfaq më pak", "Teksti: Last.fm, CC BY-SA", "Artistë për të zbuluar", "Albumet më të dashura", "Përzgjedhje editoriale: Apple Music")
)

internal fun genreDiscoveryCopy(code: String): GenreDiscoveryCopy = localizedValueOrEnglish(genreDiscoveryCopies, code)

internal fun genreDiscoveryLocalizationCodes(): Set<String> = genreDiscoveryCopies.keys
