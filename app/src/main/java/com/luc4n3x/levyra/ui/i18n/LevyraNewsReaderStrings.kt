package com.luc4n3x.levyra.ui.i18n

internal data class NewsReaderCopy(
    val readOn: String,
    val unavailable: String,
    val close: String,
    val byline: String,
    val readingTime: String
)

private val newsReaderCopies = mapOf(
    "en" to NewsReaderCopy("Read on {source}", "The full text of this story isn't available here yet.", "Close", "By {author}", "{count} min read"),
    "it" to NewsReaderCopy("Leggi su {source}", "Il testo completo di questa notizia non è ancora disponibile qui.", "Chiudi", "Di {author}", "{count} min di lettura"),
    "es" to NewsReaderCopy("Leer en {source}", "El texto completo de esta noticia aún no está disponible aquí.", "Cerrar", "Por {author}", "{count} min de lectura"),
    "fr" to NewsReaderCopy("Lire sur {source}", "Le texte intégral de cet article n'est pas encore disponible ici.", "Fermer", "Par {author}", "{count} min de lecture"),
    "de" to NewsReaderCopy("Auf {source} lesen", "Der vollständige Text dieser Meldung ist hier noch nicht verfügbar.", "Schließen", "Von {author}", "{count} Min. Lesezeit"),
    "pt" to NewsReaderCopy("Ler em {source}", "O texto completo desta notícia ainda não está disponível aqui.", "Fechar", "Por {author}", "{count} min de leitura"),
    "pt-BR" to NewsReaderCopy("Ler no {source}", "O texto completo desta notícia ainda não está disponível aqui.", "Fechar", "Por {author}", "{count} min de leitura"),
    "nl" to NewsReaderCopy("Lees op {source}", "De volledige tekst van dit bericht is hier nog niet beschikbaar.", "Sluiten", "Door {author}", "{count} min lezen"),
    "pl" to NewsReaderCopy("Czytaj w {source}", "Pełna treść tej wiadomości nie jest tu jeszcze dostępna.", "Zamknij", "Autor: {author}", "{count} min czytania"),
    "ro" to NewsReaderCopy("Citește pe {source}", "Textul complet al acestei știri nu este încă disponibil aici.", "Închide", "De {author}", "{count} min de citit"),
    "el" to NewsReaderCopy("Διάβασε στο {source}", "Το πλήρες κείμενο αυτής της είδησης δεν είναι ακόμη διαθέσιμο εδώ.", "Κλείσιμο", "Από {author}", "{count} λεπτά ανάγνωσης"),
    "sv" to NewsReaderCopy("Läs på {source}", "Hela texten till den här nyheten finns inte här än.", "Stäng", "Av {author}", "{count} min läsning"),
    "da" to NewsReaderCopy("Læs på {source}", "Den fulde tekst til denne nyhed er ikke tilgængelig her endnu.", "Luk", "Af {author}", "{count} min. læsning"),
    "cs" to NewsReaderCopy("Číst na {source}", "Celý text této zprávy tu zatím není k dispozici.", "Zavřít", "Autor: {author}", "{count} min čtení"),
    "sk" to NewsReaderCopy("Čítať na {source}", "Celý text tejto správy tu zatiaľ nie je k dispozícii.", "Zavrieť", "Autor: {author}", "{count} min čítania"),
    "hr" to NewsReaderCopy("Čitaj na {source}", "Cijeli tekst ove vijesti ovdje još nije dostupan.", "Zatvori", "Autor: {author}", "{count} min čitanja"),
    "sr" to NewsReaderCopy("Читај на {source}", "Цео текст ове вести овде још није доступан.", "Затвори", "Аутор: {author}", "{count} мин читања"),
    "sq" to NewsReaderCopy("Lexo në {source}", "Teksti i plotë i këtij lajmi nuk është ende i disponueshëm këtu.", "Mbyll", "Nga {author}", "{count} min lexim"),
    "bg" to NewsReaderCopy("Прочети в {source}", "Пълният текст на тази новина все още не е наличен тук.", "Затвори", "От {author}", "{count} мин четене"),
    "hu" to NewsReaderCopy("Olvasd el itt: {source}", "A hír teljes szövege itt még nem érhető el.", "Bezárás", "Szerző: {author}", "{count} perc olvasás"),
    "fi" to NewsReaderCopy("Lue palvelussa {source}", "Uutisen koko teksti ei ole vielä saatavilla täällä.", "Sulje", "Kirjoittanut {author}", "{count} min lukuaika"),
    "et" to NewsReaderCopy("Loe saidil {source}", "Selle uudise täisteksti pole siin veel saadaval.", "Sulge", "Autor: {author}", "{count} min lugemist"),
    "nb" to NewsReaderCopy("Les på {source}", "Hele teksten til denne nyheten er ikke tilgjengelig her ennå.", "Lukk", "Av {author}", "{count} min lesing"),
    "ca" to NewsReaderCopy("Llegeix a {source}", "El text complet d'aquesta notícia encara no està disponible aquí.", "Tanca", "Per {author}", "{count} min de lectura"),
    "uk" to NewsReaderCopy("Читати на {source}", "Повний текст цієї новини тут ще недоступний.", "Закрити", "Автор: {author}", "{count} хв читання"),
    "ru" to NewsReaderCopy("Читать на {source}", "Полный текст этой новости здесь пока недоступен.", "Закрыть", "Автор: {author}", "{count} мин чтения"),
    "kk" to NewsReaderCopy("{source} сайтында оқу", "Бұл жаңалықтың толық мәтіні мұнда әлі қолжетімді емес.", "Жабу", "Авторы: {author}", "{count} мин оқу"),
    "tr" to NewsReaderCopy("{source} üzerinde oku", "Bu haberin tam metni burada henüz mevcut değil.", "Kapat", "Yazan: {author}", "{count} dk okuma"),
    "ar" to NewsReaderCopy("اقرأ على {source}", "النص الكامل لهذا الخبر غير متاح هنا بعد.", "إغلاق", "بقلم {author}", "قراءة في {count} د"),
    "fa" to NewsReaderCopy("خواندن در {source}", "متن کامل این خبر هنوز اینجا در دسترس نیست.", "بستن", "نوشته {author}", "{count} دقیقه مطالعه"),
    "zh" to NewsReaderCopy("在 {source} 阅读", "这篇新闻的全文暂时无法在这里查看。", "关闭", "作者：{author}", "阅读约 {count} 分钟"),
    "zh-Hant" to NewsReaderCopy("在 {source} 閱讀", "這則新聞的全文暫時無法在這裡查看。", "關閉", "作者：{author}", "閱讀約 {count} 分鐘"),
    "ja" to NewsReaderCopy("{source}で読む", "このニュースの全文はまだここでは読めません。", "閉じる", "文：{author}", "{count}分で読めます"),
    "ko" to NewsReaderCopy("{source}에서 읽기", "이 소식의 전체 내용은 아직 여기에서 볼 수 없습니다.", "닫기", "글: {author}", "{count}분 분량"),
    "hi" to NewsReaderCopy("{source} पर पढ़ें", "इस खबर का पूरा पाठ अभी यहाँ उपलब्ध नहीं है।", "बंद करें", "लेखक: {author}", "{count} मिनट का पाठ"),
    "bn" to NewsReaderCopy("{source}-এ পড়ুন", "এই খবরের পুরো লেখা এখনও এখানে পাওয়া যাচ্ছে না।", "বন্ধ করুন", "লেখক: {author}", "{count} মিনিটের পড়া"),
    "id" to NewsReaderCopy("Baca di {source}", "Teks lengkap berita ini belum tersedia di sini.", "Tutup", "Oleh {author}", "{count} mnt baca"),
    "ms" to NewsReaderCopy("Baca di {source}", "Teks penuh berita ini belum tersedia di sini.", "Tutup", "Oleh {author}", "{count} min bacaan"),
    "vi" to NewsReaderCopy("Đọc trên {source}", "Toàn văn bản tin này chưa có ở đây.", "Đóng", "Bởi {author}", "{count} phút đọc"),
    "th" to NewsReaderCopy("อ่านบน {source}", "ข้อความฉบับเต็มของข่าวนี้ยังไม่พร้อมใช้งานที่นี่", "ปิด", "โดย {author}", "อ่าน {count} นาที"),
    "he" to NewsReaderCopy("לקריאה ב-{source}", "הטקסט המלא של הידיעה הזו עדיין לא זמין כאן.", "סגירה", "מאת {author}", "{count} דק׳ קריאה")
)

internal fun newsReaderCopy(code: String): NewsReaderCopy = localizedValueOrEnglish(newsReaderCopies, code)
