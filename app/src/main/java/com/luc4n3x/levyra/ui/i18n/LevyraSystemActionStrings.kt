package com.luc4n3x.levyra.ui.i18n

private fun systemActionStrings(
    sleepTimer: String,
    sleepTimerEndOfTrack: String,
    sleepTimerCancel: String,
    sleepTimerCancelled: String,
    sleepTimerRemaining: String,
    recognitionListening: String,
    recognitionProcessing: String,
    recognitionTapToListen: String
): Map<String, String> = mapOf(
    "sleepTimer" to sleepTimer,
    "sleepTimerEndOfTrack" to sleepTimerEndOfTrack,
    "sleepTimerCancel" to sleepTimerCancel,
    "sleepTimerCancelled" to sleepTimerCancelled,
    "sleepTimerRemaining" to sleepTimerRemaining,
    "recognitionListening" to recognitionListening,
    "recognitionProcessing" to recognitionProcessing,
    "recognitionTapToListen" to recognitionTapToListen
)

private val systemActionBundles: Map<String, Map<String, String>> = mapOf(
    "en" to systemActionStrings("Sleep timer", "End of track", "Cancel timer", "Timer cancelled", "Remaining", "Listening…", "Analyzing…", "Tap to listen"),
    "it" to systemActionStrings("Timer di spegnimento", "Fine del brano", "Annulla timer", "Timer annullato", "Tempo rimanente", "In ascolto…", "Analisi…", "Tocca per ascoltare"),
    "es" to systemActionStrings("Temporizador de apagado", "Fin de la canción", "Cancelar temporizador", "Temporizador cancelado", "Restante", "Escuchando…", "Analizando…", "Toca para escuchar"),
    "fr" to systemActionStrings("Minuterie d'arrêt", "Fin du titre", "Annuler la minuterie", "Minuterie annulée", "Restant", "À l'écoute…", "Analyse…", "Touchez pour écouter"),
    "de" to systemActionStrings("Einschlaftimer", "Ende des Titels", "Timer abbrechen", "Timer abgebrochen", "Verbleibend", "Hört zu…", "Analyse…", "Zum Zuhören tippen"),
    "pt" to systemActionStrings("Temporizador de sono", "Fim da faixa", "Cancelar temporizador", "Temporizador cancelado", "Restante", "A ouvir…", "A analisar…", "Toque para ouvir"),
    "nl" to systemActionStrings("Slaaptimer", "Einde van nummer", "Timer annuleren", "Timer geannuleerd", "Resterend", "Aan het luisteren…", "Analyseren…", "Tik om te luisteren"),
    "pl" to systemActionStrings("Wyłącznik czasowy", "Koniec utworu", "Anuluj wyłącznik", "Wyłącznik anulowany", "Pozostało", "Słuchanie…", "Analizowanie…", "Dotknij, aby słuchać"),
    "ro" to systemActionStrings("Temporizator de oprire", "Sfârșitul piesei", "Anulează temporizatorul", "Temporizator anulat", "Rămas", "Se ascultă…", "Se analizează…", "Atinge pentru a asculta"),
    "el" to systemActionStrings("Χρονοδιακόπτης ύπνου", "Τέλος κομματιού", "Ακύρωση χρονοδιακόπτη", "Ο χρονοδιακόπτης ακυρώθηκε", "Απομένουν", "Ακρόαση…", "Ανάλυση…", "Πατήστε για ακρόαση"),
    "sv" to systemActionStrings("Insomningstimer", "Slutet av låten", "Avbryt timer", "Timer avbruten", "Återstår", "Lyssnar…", "Analyserar…", "Tryck för att lyssna"),
    "da" to systemActionStrings("Søvntimer", "Slut på nummer", "Annuller timer", "Timer annulleret", "Resterende", "Lytter…", "Analyserer…", "Tryk for at lytte"),
    "cs" to systemActionStrings("Časovač vypnutí", "Konec skladby", "Zrušit časovač", "Časovač zrušen", "Zbývá", "Poslouchám…", "Analyzuji…", "Klepnutím poslouchat"),
    "uk" to systemActionStrings("Таймер сну", "Кінець треку", "Скасувати таймер", "Таймер скасовано", "Залишилось", "Слухаю…", "Аналіз…", "Торкніться, щоб слухати"),
    "ru" to systemActionStrings("Таймер сна", "Конец трека", "Отменить таймер", "Таймер отменён", "Осталось", "Слушаю…", "Анализ…", "Нажмите, чтобы слушать"),
    "tr" to systemActionStrings("Uyku zamanlayıcı", "Parça sonu", "Zamanlayıcıyı iptal et", "Zamanlayıcı iptal edildi", "Kalan", "Dinleniyor…", "Analiz ediliyor…", "Dinlemek için dokunun"),
    "ar" to systemActionStrings("مؤقّت النوم", "نهاية المقطع", "إلغاء المؤقّت", "تم إلغاء المؤقّت", "المتبقّي", "جارٍ الاستماع…", "جارٍ التحليل…", "اضغط للاستماع"),
    "zh" to systemActionStrings("睡眠定时", "本曲结束", "取消定时", "定时已取消", "剩余", "正在聆听…", "正在分析…", "点按以聆听"),
    "ja" to systemActionStrings("スリープタイマー", "曲の終わり", "タイマーをキャンセル", "タイマーをキャンセルしました", "残り", "聴き取り中…", "解析中…", "タップして聴き取る"),
    "ko" to systemActionStrings("취침 타이머", "곡 종료 시", "타이머 취소", "타이머가 취소되었습니다", "남은 시간", "듣는 중…", "분석 중…", "탭하여 듣기"),
    "hi" to systemActionStrings("स्लीप टाइमर", "गाना खत्म होने पर", "टाइमर रद्द करें", "टाइमर रद्द हुआ", "शेष", "सुन रहे हैं…", "विश्लेषण हो रहा है…", "सुनने के लिए टैप करें"),
    "id" to systemActionStrings("Timer tidur", "Akhir lagu", "Batalkan timer", "Timer dibatalkan", "Tersisa", "Mendengarkan…", "Menganalisis…", "Ketuk untuk mendengarkan"),
    "vi" to systemActionStrings("Hẹn giờ ngủ", "Kết thúc bài hát", "Hủy hẹn giờ", "Đã hủy hẹn giờ", "Còn lại", "Đang nghe…", "Đang phân tích…", "Chạm để nghe"),
    "th" to systemActionStrings("ตั้งเวลาปิด", "จบเพลง", "ยกเลิกตัวตั้งเวลา", "ยกเลิกตัวตั้งเวลาแล้ว", "คงเหลือ", "กำลังฟัง…", "กำลังวิเคราะห์…", "แตะเพื่อฟัง"),
    "fil" to systemActionStrings("Sleep timer", "Pagkatapos ng kanta", "Kanselahin ang timer", "Nakansela ang timer", "Natitira", "Nakikinig…", "Sinusuri…", "I-tap para makinig"),
    "he" to systemActionStrings("טיימר שינה", "סוף השיר", "בטל טיימר", "הטיימר בוטל", "נותר", "מאזין…", "מנתח…", "הקש כדי להאזין")
)

internal fun systemActionLocalizationEntries(code: String): Map<String, String> = systemActionBundles.getValue(code)
