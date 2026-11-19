package com.luc4n3x.levyra.ui.i18n

import java.util.Locale

internal data class DownloadProgressCopy(
    private val downloadedOfPattern: String,
    private val queuedPattern: String,
    private val totalPattern: String,
    val openDownloads: String
) {
    fun downloadedOf(done: Int, total: Int): String = String.format(Locale.ROOT, downloadedOfPattern, done, total)

    fun queued(count: Int): String = String.format(Locale.ROOT, queuedPattern, count)

    fun total(percent: Int): String = String.format(Locale.ROOT, totalPattern, percent)
}

private val downloadProgressCopies = mapOf(
    "en" to DownloadProgressCopy("%1\$d of %2\$d downloaded", "In queue: %1\$d", "Total %1\$d%%", "Open downloads"),
    "it" to DownloadProgressCopy("Scaricati %1\$d di %2\$d", "In coda: %1\$d", "Totale %1\$d%%", "Apri i download"),
    "es" to DownloadProgressCopy("Descargadas %1\$d de %2\$d", "En cola: %1\$d", "Total %1\$d%%", "Abrir descargas"),
    "fr" to DownloadProgressCopy("%1\$d sur %2\$d téléchargés", "En file : %1\$d", "Total %1\$d %%", "Ouvrir les téléchargements"),
    "de" to DownloadProgressCopy("%1\$d von %2\$d geladen", "In der Warteschlange: %1\$d", "Gesamt %1\$d %%", "Downloads öffnen"),
    "pt" to DownloadProgressCopy("%1\$d de %2\$d transferidas", "Em fila: %1\$d", "Total %1\$d%%", "Abrir transferências"),
    "pt-BR" to DownloadProgressCopy("%1\$d de %2\$d baixadas", "Na fila: %1\$d", "Total %1\$d%%", "Abrir downloads"),
    "nl" to DownloadProgressCopy("%1\$d van %2\$d gedownload", "In wachtrij: %1\$d", "Totaal %1\$d%%", "Downloads openen"),
    "pl" to DownloadProgressCopy("Pobrano %1\$d z %2\$d", "W kolejce: %1\$d", "Łącznie %1\$d%%", "Otwórz pobrane"),
    "ro" to DownloadProgressCopy("Descărcate %1\$d din %2\$d", "În coadă: %1\$d", "Total %1\$d%%", "Deschide descărcările"),
    "el" to DownloadProgressCopy("Λήφθηκαν %1\$d από %2\$d", "Σε αναμονή: %1\$d", "Σύνολο %1\$d%%", "Άνοιγμα λήψεων"),
    "sv" to DownloadProgressCopy("%1\$d av %2\$d nedladdade", "I kö: %1\$d", "Totalt %1\$d %%", "Öppna nedladdningar"),
    "da" to DownloadProgressCopy("%1\$d af %2\$d downloadet", "I kø: %1\$d", "I alt %1\$d %%", "Åbn downloads"),
    "cs" to DownloadProgressCopy("Staženo %1\$d z %2\$d", "Ve frontě: %1\$d", "Celkem %1\$d %%", "Otevřít stažené"),
    "sk" to DownloadProgressCopy("Stiahnuté %1\$d z %2\$d", "V poradí: %1\$d", "Spolu %1\$d %%", "Otvoriť stiahnuté"),
    "hr" to DownloadProgressCopy("Preuzeto %1\$d od %2\$d", "U redu čekanja: %1\$d", "Ukupno %1\$d %%", "Otvori preuzimanja"),
    "sr" to DownloadProgressCopy("Преузето %1\$d од %2\$d", "На чекању: %1\$d", "Укупно %1\$d%%", "Отвори преузимања"),
    "sq" to DownloadProgressCopy("Shkarkuar %1\$d nga %2\$d", "Në radhë: %1\$d", "Gjithsej %1\$d%%", "Hap shkarkimet"),
    "bg" to DownloadProgressCopy("Изтеглени %1\$d от %2\$d", "На опашка: %1\$d", "Общо %1\$d%%", "Отвори изтеглянията"),
    "hu" to DownloadProgressCopy("%1\$d / %2\$d letöltve", "Sorban: %1\$d", "Összesen %1\$d%%", "Letöltések megnyitása"),
    "fi" to DownloadProgressCopy("Ladattu %1\$d/%2\$d", "Jonossa: %1\$d", "Yhteensä %1\$d %%", "Avaa lataukset"),
    "et" to DownloadProgressCopy("Alla laaditud %1\$d/%2\$d", "Järjekorras: %1\$d", "Kokku %1\$d%%", "Ava allalaadimised"),
    "nb" to DownloadProgressCopy("%1\$d av %2\$d lastet ned", "I kø: %1\$d", "Totalt %1\$d %%", "Åpne nedlastinger"),
    "ca" to DownloadProgressCopy("Baixades %1\$d de %2\$d", "A la cua: %1\$d", "Total %1\$d %%", "Obre les baixades"),
    "uk" to DownloadProgressCopy("Завантажено %1\$d з %2\$d", "У черзі: %1\$d", "Загалом %1\$d%%", "Відкрити завантаження"),
    "ru" to DownloadProgressCopy("Загружено %1\$d из %2\$d", "В очереди: %1\$d", "Всего %1\$d%%", "Открыть загрузки"),
    "kk" to DownloadProgressCopy("Жүктелді: %1\$d / %2\$d", "Кезекте: %1\$d", "Барлығы %1\$d%%", "Жүктеулерді ашу"),
    "tr" to DownloadProgressCopy("%2\$d şarkıdan %1\$d indirildi", "Sırada: %1\$d", "Toplam %%%1\$d", "İndirilenleri aç"),
    "ar" to DownloadProgressCopy("تم تنزيل %1\$d من %2\$d", "في قائمة الانتظار: %1\$d", "الإجمالي %1\$d٪", "فتح التنزيلات"),
    "fa" to DownloadProgressCopy("%1\$d از %2\$d بارگیری شد", "در صف: %1\$d", "مجموع %1\$d٪", "باز کردن بارگیری‌ها"),
    "zh" to DownloadProgressCopy("已下载 %1\$d/%2\$d", "排队中：%1\$d", "总进度 %1\$d%%", "打开下载"),
    "zh-Hant" to DownloadProgressCopy("已下載 %1\$d/%2\$d", "佇列中：%1\$d", "總進度 %1\$d%%", "開啟下載"),
    "ja" to DownloadProgressCopy("%2\$d曲中%1\$d曲をダウンロード済み", "待機中：%1\$d", "全体 %1\$d%%", "ダウンロードを開く"),
    "ko" to DownloadProgressCopy("%2\$d곡 중 %1\$d곡 다운로드됨", "대기 중: %1\$d", "전체 %1\$d%%", "다운로드 열기"),
    "hi" to DownloadProgressCopy("%2\$d में से %1\$d डाउनलोड हुए", "कतार में: %1\$d", "कुल %1\$d%%", "डाउनलोड खोलें"),
    "bn" to DownloadProgressCopy("%2\$d-এর মধ্যে %1\$d ডাউনলোড হয়েছে", "সারিতে: %1\$d", "মোট %1\$d%%", "ডাউনলোড খুলুন"),
    "id" to DownloadProgressCopy("%1\$d dari %2\$d diunduh", "Dalam antrean: %1\$d", "Total %1\$d%%", "Buka unduhan"),
    "ms" to DownloadProgressCopy("%1\$d daripada %2\$d dimuat turun", "Dalam baris gilir: %1\$d", "Jumlah %1\$d%%", "Buka muat turun"),
    "vi" to DownloadProgressCopy("Đã tải %1\$d/%2\$d", "Đang chờ: %1\$d", "Tổng %1\$d%%", "Mở mục tải xuống"),
    "th" to DownloadProgressCopy("ดาวน์โหลดแล้ว %1\$d จาก %2\$d", "ในคิว: %1\$d", "ทั้งหมด %1\$d%%", "เปิดรายการดาวน์โหลด"),
    "fil" to DownloadProgressCopy("Na-download ang %1\$d sa %2\$d", "Nasa pila: %1\$d", "Kabuuan %1\$d%%", "Buksan ang mga download"),
    "he" to DownloadProgressCopy("הורדו %1\$d מתוך %2\$d", "בתור: %1\$d", "סה״כ %1\$d%%", "פתיחת ההורדות")
)

internal fun downloadProgressCopy(code: String): DownloadProgressCopy = localizedValueOrEnglish(downloadProgressCopies, code)

internal fun downloadProgressLocalizationCodes(): Set<String> = downloadProgressCopies.keys
