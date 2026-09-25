package com.luc4n3x.levyra.ui.i18n

internal data class BulkLinkCaptureCopy(
    val title: String,
    val detected: String,
    val found: String,
    val duplicates: String,
    val unrecognized: String,
    val resolving: String,
    val noTracks: String,
    val savePlaylist: String,
    val playlistName: String
) {
    fun detected(count: Int): String = detected.withCount(count)
    fun found(count: Int): String = found.withCount(count)
    fun duplicates(count: Int): String = duplicates.withCount(count)
    fun unrecognized(count: Int): String = unrecognized.withCount(count)
    fun resolving(count: Int): String = resolving.withCount(count)

    private fun String.withCount(count: Int): String = replace("{n}", count.toString())
}

private val bulkLinkCaptureCopies = mapOf(
    "en" to BulkLinkCaptureCopy("Multiple links", "Links detected: {n}", "Tracks found: {n}", "Duplicates: {n}", "Not recognized: {n}", "Links to check: {n}", "No track was recognized in these links", "Save playlist", "Captured links"),
    "it" to BulkLinkCaptureCopy("Link multipli", "Link rilevati: {n}", "Brani trovati: {n}", "Duplicati: {n}", "Non riconosciuti: {n}", "Link da analizzare: {n}", "Nessun brano riconosciuto in questi link", "Salva playlist", "Link importati"),
    "es" to BulkLinkCaptureCopy("Varios enlaces", "Enlaces detectados: {n}", "Canciones encontradas: {n}", "Duplicados: {n}", "No reconocidos: {n}", "Enlaces por revisar: {n}", "No se reconoció ninguna canción en estos enlaces", "Guardar playlist", "Enlaces capturados"),
    "fr" to BulkLinkCaptureCopy("Plusieurs liens", "Liens détectés : {n}", "Titres trouvés : {n}", "Doublons : {n}", "Non reconnus : {n}", "Liens à analyser : {n}", "Aucun titre reconnu dans ces liens", "Enregistrer la playlist", "Liens capturés"),
    "de" to BulkLinkCaptureCopy("Mehrere Links", "Erkannte Links: {n}", "Gefundene Titel: {n}", "Duplikate: {n}", "Nicht erkannt: {n}", "Zu prüfende Links: {n}", "In diesen Links wurde kein Titel erkannt", "Playlist speichern", "Erfasste Links"),
    "pt" to BulkLinkCaptureCopy("Vários links", "Links detectados: {n}", "Faixas encontradas: {n}", "Duplicados: {n}", "Não reconhecidos: {n}", "Links a verificar: {n}", "Nenhuma faixa reconhecida nesses links", "Salvar playlist", "Links capturados"),
    "nl" to BulkLinkCaptureCopy("Meerdere links", "Gevonden links: {n}", "Gevonden nummers: {n}", "Dubbel: {n}", "Niet herkend: {n}", "Te controleren links: {n}", "Geen nummer herkend in deze links", "Playlist opslaan", "Vastgelegde links"),
    "pl" to BulkLinkCaptureCopy("Wiele linków", "Wykryte linki: {n}", "Znalezione utwory: {n}", "Duplikaty: {n}", "Nierozpoznane: {n}", "Linki do sprawdzenia: {n}", "W tych linkach nie rozpoznano żadnego utworu", "Zapisz playlistę", "Zapisane linki"),
    "ro" to BulkLinkCaptureCopy("Mai multe linkuri", "Linkuri detectate: {n}", "Piese găsite: {n}", "Duplicate: {n}", "Nerecunoscute: {n}", "Linkuri de verificat: {n}", "Nicio piesă recunoscută în aceste linkuri", "Salvează playlistul", "Linkuri capturate"),
    "el" to BulkLinkCaptureCopy("Πολλαπλοί σύνδεσμοι", "Σύνδεσμοι που εντοπίστηκαν: {n}", "Κομμάτια που βρέθηκαν: {n}", "Διπλότυπα: {n}", "Μη αναγνωρισμένα: {n}", "Σύνδεσμοι για έλεγχο: {n}", "Δεν αναγνωρίστηκε κανένα κομμάτι σε αυτούς τους συνδέσμους", "Αποθήκευση λίστας", "Καταγεγραμμένοι σύνδεσμοι"),
    "sv" to BulkLinkCaptureCopy("Flera länkar", "Hittade länkar: {n}", "Hittade låtar: {n}", "Dubbletter: {n}", "Känns inte igen: {n}", "Länkar att kontrollera: {n}", "Ingen låt kändes igen i länkarna", "Spara spellista", "Fångade länkar"),
    "da" to BulkLinkCaptureCopy("Flere links", "Fundne links: {n}", "Fundne numre: {n}", "Dubletter: {n}", "Ikke genkendt: {n}", "Links til kontrol: {n}", "Intet nummer blev genkendt i disse links", "Gem playliste", "Fangede links"),
    "cs" to BulkLinkCaptureCopy("Více odkazů", "Zjištěné odkazy: {n}", "Nalezené skladby: {n}", "Duplicity: {n}", "Nerozpoznané: {n}", "Odkazy ke kontrole: {n}", "V těchto odkazech nebyla rozpoznána žádná skladba", "Uložit playlist", "Zachycené odkazy"),
    "sk" to BulkLinkCaptureCopy("Viac odkazov", "Zistené odkazy: {n}", "Nájdené skladby: {n}", "Duplicity: {n}", "Nerozpoznané: {n}", "Odkazy na kontrolu: {n}", "V týchto odkazoch sa nerozpoznala žiadna skladba", "Uložiť playlist", "Zachytené odkazy"),
    "hr" to BulkLinkCaptureCopy("Više poveznica", "Otkrivene poveznice: {n}", "Pronađene pjesme: {n}", "Duplikati: {n}", "Neprepoznato: {n}", "Poveznice za provjeru: {n}", "U ovim poveznicama nije prepoznata nijedna pjesma", "Spremi popis za reprodukciju", "Prikupljene poveznice"),
    "bg" to BulkLinkCaptureCopy("Няколко връзки", "Открити връзки: {n}", "Намерени песни: {n}", "Дубликати: {n}", "Неразпознати: {n}", "Връзки за проверка: {n}", "В тези връзки не е разпозната песен", "Запази плейлиста", "Събрани връзки"),
    "hu" to BulkLinkCaptureCopy("Több link", "Észlelt linkek: {n}", "Talált dalok: {n}", "Ismétlődések: {n}", "Nem felismert: {n}", "Ellenőrizendő linkek: {n}", "Ezekben a linkekben nem ismerhető fel dal", "Lejátszási lista mentése", "Rögzített linkek"),
    "fi" to BulkLinkCaptureCopy("Useita linkkejä", "Tunnistetut linkit: {n}", "Löydetyt kappaleet: {n}", "Kaksoiskappaleet: {n}", "Tunnistamattomat: {n}", "Tarkistettavat linkit: {n}", "Linkeistä ei tunnistettu kappaleita", "Tallenna soittolista", "Kerätyt linkit"),
    "et" to BulkLinkCaptureCopy("Mitu linki", "Tuvastatud lingid: {n}", "Leitud lood: {n}", "Duplikaadid: {n}", "Tundmatud: {n}", "Kontrollitavad lingid: {n}", "Nendest linkidest ei tuvastatud ühtegi lugu", "Salvesta esitusloend", "Kogutud lingid"),
    "nb" to BulkLinkCaptureCopy("Flere lenker", "Funnet lenker: {n}", "Funnet låter: {n}", "Duplikater: {n}", "Ikke gjenkjent: {n}", "Lenker som sjekkes: {n}", "Ingen låt ble gjenkjent i disse lenkene", "Lagre spilleliste", "Innsamlede lenker"),
    "ca" to BulkLinkCaptureCopy("Diversos enllaços", "Enllaços detectats: {n}", "Cançons trobades: {n}", "Duplicats: {n}", "No reconeguts: {n}", "Enllaços per revisar: {n}", "No s'ha reconegut cap cançó en aquests enllaços", "Desa la llista", "Enllaços capturats"),
    "uk" to BulkLinkCaptureCopy("Кілька посилань", "Виявлено посилань: {n}", "Знайдено треків: {n}", "Дублікатів: {n}", "Не розпізнано: {n}", "Посилань на перевірку: {n}", "У цих посиланнях не розпізнано жодного треку", "Зберегти плейлист", "Захоплені посилання"),
    "ru" to BulkLinkCaptureCopy("Несколько ссылок", "Найдено ссылок: {n}", "Найдено треков: {n}", "Дубликатов: {n}", "Не распознано: {n}", "Ссылок на проверку: {n}", "В этих ссылках не распознано ни одного трека", "Сохранить плейлист", "Собранные ссылки"),
    "tr" to BulkLinkCaptureCopy("Birden fazla bağlantı", "Algılanan bağlantılar: {n}", "Bulunan parçalar: {n}", "Yinelenenler: {n}", "Tanınmayanlar: {n}", "Kontrol edilecek bağlantılar: {n}", "Bu bağlantılarda parça tanınmadı", "Çalma listesini kaydet", "Yakalanan bağlantılar"),
    "ar" to BulkLinkCaptureCopy("روابط متعددة", "الروابط المكتشفة: {n}", "المقاطع التي تم العثور عليها: {n}", "المكررة: {n}", "غير المعروفة: {n}", "روابط قيد الفحص: {n}", "لم يتم التعرف على أي مقطع في هذه الروابط", "حفظ قائمة التشغيل", "روابط ملتقطة"),
    "fa" to BulkLinkCaptureCopy("چند پیوند", "پیوندهای شناسایی‌شده: {n}", "آهنگ‌های پیداشده: {n}", "تکراری: {n}", "ناشناخته: {n}", "پیوندهای در حال بررسی: {n}", "هیچ آهنگی در این پیوندها شناسایی نشد", "ذخیره فهرست پخش", "پیوندهای ثبت‌شده"),
    "zh" to BulkLinkCaptureCopy("多个链接", "检测到的链接：{n}", "找到的歌曲：{n}", "重复：{n}", "无法识别：{n}", "待检查链接：{n}", "这些链接中没有识别到歌曲", "保存歌单", "已捕获的链接"),
    "zh-Hant" to BulkLinkCaptureCopy("多個連結", "偵測到的連結：{n}", "找到的歌曲：{n}", "重複：{n}", "無法辨識：{n}", "待檢查連結：{n}", "這些連結中沒有辨識到歌曲", "儲存播放清單", "已擷取的連結"),
    "ja" to BulkLinkCaptureCopy("複数のリンク", "検出したリンク：{n}", "見つかった曲：{n}", "重複：{n}", "認識できないリンク：{n}", "確認するリンク：{n}", "これらのリンクから曲を認識できませんでした", "プレイリストを保存", "取り込んだリンク"),
    "ko" to BulkLinkCaptureCopy("여러 링크", "감지된 링크: {n}", "찾은 곡: {n}", "중복: {n}", "인식 불가: {n}", "확인할 링크: {n}", "이 링크에서 곡을 인식하지 못했습니다", "플레이리스트 저장", "가져온 링크"),
    "hi" to BulkLinkCaptureCopy("कई लिंक", "मिले लिंक: {n}", "मिले गाने: {n}", "डुप्लिकेट: {n}", "पहचाने नहीं गए: {n}", "जाँचे जाने वाले लिंक: {n}", "इन लिंक में कोई गाना नहीं पहचाना गया", "प्लेलिस्ट सहेजें", "कैप्चर किए गए लिंक"),
    "id" to BulkLinkCaptureCopy("Beberapa tautan", "Tautan terdeteksi: {n}", "Lagu ditemukan: {n}", "Duplikat: {n}", "Tidak dikenali: {n}", "Tautan diperiksa: {n}", "Tidak ada lagu yang dikenali dari tautan ini", "Simpan playlist", "Tautan yang ditangkap"),
    "ms" to BulkLinkCaptureCopy("Beberapa pautan", "Pautan dikesan: {n}", "Lagu ditemui: {n}", "Pendua: {n}", "Tidak dikenali: {n}", "Pautan disemak: {n}", "Tiada lagu dikenali daripada pautan ini", "Simpan senarai main", "Pautan yang ditangkap"),
    "vi" to BulkLinkCaptureCopy("Nhiều liên kết", "Liên kết phát hiện: {n}", "Bài hát tìm thấy: {n}", "Trùng lặp: {n}", "Không nhận dạng được: {n}", "Liên kết cần kiểm tra: {n}", "Không nhận dạng được bài hát nào từ các liên kết này", "Lưu danh sách phát", "Liên kết đã thu thập"),
    "th" to BulkLinkCaptureCopy("หลายลิงก์", "ลิงก์ที่พบ: {n}", "เพลงที่พบ: {n}", "รายการซ้ำ: {n}", "ไม่รู้จัก: {n}", "ลิงก์ที่ต้องตรวจสอบ: {n}", "ไม่พบเพลงที่รู้จักในลิงก์เหล่านี้", "บันทึกเพลย์ลิสต์", "ลิงก์ที่บันทึกไว้"),
    "fil" to BulkLinkCaptureCopy("Maraming link", "Mga link na nakita: {n}", "Mga kantang nahanap: {n}", "Mga duplicate: {n}", "Hindi nakilala: {n}", "Mga link na susuriin: {n}", "Walang nakilalang kanta sa mga link na ito", "I-save ang playlist", "Mga nakuhang link"),
    "he" to BulkLinkCaptureCopy("קישורים מרובים", "קישורים שזוהו: {n}", "שירים שנמצאו: {n}", "כפולים: {n}", "לא זוהו: {n}", "קישורים לבדיקה: {n}", "לא זוהה שיר בקישורים האלה", "שמירת פלייליסט", "קישורים שנאספו")
)

internal fun bulkLinkCaptureCopy(code: String): BulkLinkCaptureCopy =
    localizedValueOrEnglish(bulkLinkCaptureCopies, code)

internal fun bulkLinkCaptureLocalizationCodes(): Set<String> = bulkLinkCaptureCopies.keys
