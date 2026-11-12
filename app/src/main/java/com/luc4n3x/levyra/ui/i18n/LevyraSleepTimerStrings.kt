package com.luc4n3x.levyra.ui.i18n

internal data class SleepTimerCopy(
    val chooseDuration: String,
    val endsAt: String,
    val active: String,
    val minuteUnit: String,
    val fadeActive: String
) {
    val addFifteen: String get() = "+15 $minuteUnit"
}

private val sleepTimerCopies: Map<String, SleepTimerCopy> = mapOf(
    "en" to SleepTimerCopy("Choose when the music should stop", "Ends at", "TIMER ACTIVE", "min", "End fade active"),
    "it" to SleepTimerCopy("Scegli quando fermare la musica", "Termina alle", "TIMER ATTIVO", "min", "Dissolvenza finale attiva"),
    "es" to SleepTimerCopy("Elige cuándo detener la música", "Termina a las", "TEMPORIZADOR ACTIVO", "min", "Fundido final activo"),
    "fr" to SleepTimerCopy("Choisissez quand arrêter la musique", "Se termine à", "MINUTEUR ACTIF", "min", "Fondu final actif"),
    "de" to SleepTimerCopy("Wähle, wann die Musik stoppen soll", "Endet um", "TIMER AKTIV", "Min.", "Ausblenden aktiv"),
    "pt" to SleepTimerCopy("Escolha quando parar a música", "Termina às", "TEMPORIZADOR ATIVO", "min", "Desvanecimento final ativo"),
    "pt-BR" to SleepTimerCopy("Escolha quando a música deve parar", "Termina às", "TIMER ATIVO", "min", "Fade final ativo"),
    "nl" to SleepTimerCopy("Kies wanneer de muziek moet stoppen", "Eindigt om", "TIMER ACTIEF", "min", "Uitfaden actief"),
    "pl" to SleepTimerCopy("Wybierz, kiedy muzyka ma się zatrzymać", "Koniec o", "WYŁĄCZNIK AKTYWNY", "min", "Wyciszanie na końcu aktywne"),
    "ro" to SleepTimerCopy("Alege când să se oprească muzica", "Se termină la", "TEMPORIZATOR ACTIV", "min", "Estompare finală activă"),
    "el" to SleepTimerCopy("Επιλέξτε πότε θα σταματήσει η μουσική", "Λήγει στις", "ΧΡΟΝΟΔΙΑΚΟΠΤΗΣ ΕΝΕΡΓΟΣ", "λεπ.", "Σταδιακό σβήσιμο ενεργό"),
    "sv" to SleepTimerCopy("Välj när musiken ska sluta", "Slutar kl.", "TIMER AKTIV", "min", "Uttoning aktiv"),
    "da" to SleepTimerCopy("Vælg, hvornår musikken skal stoppe", "Slutter kl.", "TIMER AKTIV", "min", "Udtoning aktiv"),
    "cs" to SleepTimerCopy("Vyberte, kdy se má hudba zastavit", "Končí v", "ČASOVAČ AKTIVNÍ", "min", "Postupné ztlumení aktivní"),
    "sk" to SleepTimerCopy("Vyberte, kedy sa má hudba zastaviť", "Končí o", "ČASOVAČ AKTÍVNY", "min", "Postupné stlmenie aktívne"),
    "bn" to SleepTimerCopy("কখন গান থামবে তা বেছে নিন", "শেষ হবে", "টাইমার চালু", "মিনিট", "ধীরে কমানো চালু"),
    "kk" to SleepTimerCopy("Музыка қашан тоқтайтынын таңдаңыз", "Аяқталады", "ТАЙМЕР ҚОСУЛЫ", "мин", "Соңғы бәсеңдеу қосулы"),
    "sr" to SleepTimerCopy("Изаберите када музика треба да стане", "Завршава се у", "ТАЈМЕР АКТИВАН", "мин", "Утишавање на крају активно"),
    "sq" to SleepTimerCopy("Zgjidh kur të ndalojë muzika", "Përfundon në", "KOHËMATËSI AKTIV", "min", "Shuarja graduale aktive"),
    "hr" to SleepTimerCopy("Odaberite kada glazba treba stati", "Završava u", "TIMER AKTIVAN", "min", "Postupno utišavanje aktivno"),
    "bg" to SleepTimerCopy("Изберете кога да спре музиката", "Приключва в", "ТАЙМЕРЪТ Е АКТИВЕН", "мин", "Затихването е активно"),
    "hu" to SleepTimerCopy("Válaszd ki, mikor álljon le a zene", "Vége ekkor:", "IDŐZÍTŐ AKTÍV", "perc", "Halkítás aktív"),
    "fi" to SleepTimerCopy("Valitse milloin musiikin tulisi pysähtyä", "Päättyy klo", "AJASTIN PÄÄLLÄ", "min", "Loppuhäivytys päällä"),
    "et" to SleepTimerCopy("Vali, millal muusika peaks peatuma", "Lõpeb kell", "TAIMER AKTIIVNE", "min", "Lõpu hajumine aktiivne"),
    "nb" to SleepTimerCopy("Velg når musikken skal stoppe", "Slutter kl.", "TIMER AKTIV", "min", "Uttoning aktiv"),
    "ca" to SleepTimerCopy("Tria quan s'ha d'aturar la música", "Acaba a les", "TEMPORITZADOR ACTIU", "min", "Esvaïment final actiu"),
    "uk" to SleepTimerCopy("Виберіть, коли музика має зупинитися", "Завершиться о", "ТАЙМЕР АКТИВНИЙ", "хв", "Згасання активне"),
    "ru" to SleepTimerCopy("Выберите, когда остановить музыку", "Закончится в", "ТАЙМЕР АКТИВЕН", "мин", "Затухание включено"),
    "tr" to SleepTimerCopy("Müziğin ne zaman duracağını seçin", "Bitiş", "ZAMANLAYICI ETKİN", "dk", "Sona doğru kısma etkin"),
    "ar" to SleepTimerCopy("اختر متى تتوقف الموسيقى", "ينتهي في", "المؤقت نشط", "د", "التلاشي النهائي نشط"),
    "fa" to SleepTimerCopy("انتخاب کنید موسیقی کی متوقف شود", "پایان در", "زمان‌سنج فعال", "دقیقه", "محو تدریجی فعال"),
    "zh" to SleepTimerCopy("选择音乐何时停止", "结束于", "定时器已开启", "分钟", "结尾淡出已开启"),
    "zh-Hant" to SleepTimerCopy("選擇音樂何時停止", "結束於", "計時器已啟用", "分鐘", "結尾淡出已啟用"),
    "ja" to SleepTimerCopy("音楽を停止するタイミングを選択", "終了時刻", "タイマー作動中", "分", "フェードアウト有効"),
    "ko" to SleepTimerCopy("음악을 멈출 시점을 선택하세요", "종료 시각", "타이머 작동 중", "분", "페이드아웃 사용 중"),
    "hi" to SleepTimerCopy("चुनें कि संगीत कब रुके", "समाप्ति", "टाइमर चालू", "मिनट", "धीरे-धीरे बंद होना चालू"),
    "id" to SleepTimerCopy("Pilih kapan musik berhenti", "Berakhir pukul", "TIMER AKTIF", "mnt", "Fade akhir aktif"),
    "ms" to SleepTimerCopy("Pilih bila muzik perlu berhenti", "Tamat pada", "PEMASA AKTIF", "min", "Pudar akhir aktif"),
    "vi" to SleepTimerCopy("Chọn thời điểm dừng nhạc", "Kết thúc lúc", "HẸN GIỜ ĐANG BẬT", "phút", "Giảm dần cuối bài đang bật"),
    "th" to SleepTimerCopy("เลือกเวลาที่จะหยุดเพลง", "สิ้นสุดเวลา", "ตัวตั้งเวลาทำงานอยู่", "นาที", "ค่อยๆ ลดเสียงตอนจบเปิดอยู่"),
    "fil" to SleepTimerCopy("Piliin kung kailan hihinto ang musika", "Matatapos nang", "AKTIBO ANG TIMER", "min", "Aktibo ang fade sa dulo"),
    "he" to SleepTimerCopy("בחרו מתי המוזיקה תיפסק", "מסתיים ב-", "הטיימר פעיל", "דק׳", "דעיכה בסיום פעילה")
)

internal fun sleepTimerCopy(code: String): SleepTimerCopy = localizedValueOrEnglish(sleepTimerCopies, code)

internal fun sleepTimerLocalizationCodes(): Set<String> = sleepTimerCopies.keys
