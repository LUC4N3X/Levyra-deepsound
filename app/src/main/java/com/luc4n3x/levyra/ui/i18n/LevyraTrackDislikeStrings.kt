package com.luc4n3x.levyra.ui.i18n

internal data class TrackDislikeCopy(
    val dislike: String,
    val removeDislike: String,
    val skipped: String,
    val undo: String
)

private val trackDislikeCopies = mapOf(
    "en" to TrackDislikeCopy("Don't like", "Remove dislike", "Skipped in autoplay", "Undo"),
    "it" to TrackDislikeCopy("Non mi piace", "Rimuovi Non mi piace", "Saltato nella riproduzione automatica", "Annulla"),
    "es" to TrackDislikeCopy("No me gusta", "Quitar No me gusta", "Se omite en la reproducción automática", "Deshacer"),
    "fr" to TrackDislikeCopy("Je n'aime pas", "Retirer Je n'aime pas", "Ignoré en lecture automatique", "Annuler"),
    "de" to TrackDislikeCopy("Gefällt mir nicht", "Gefällt mir nicht entfernen", "Bei automatischer Wiedergabe übersprungen", "Rückgängig"),
    "pt" to TrackDislikeCopy("Não gosto", "Remover Não gosto", "Ignorada na reprodução automática", "Desfazer"),
    "nl" to TrackDislikeCopy("Niet leuk", "Niet leuk verwijderen", "Overgeslagen bij automatisch afspelen", "Ongedaan maken"),
    "pl" to TrackDislikeCopy("Nie podoba mi się", "Usuń Nie podoba mi się", "Pomijany w automatycznym odtwarzaniu", "Cofnij"),
    "ro" to TrackDislikeCopy("Nu-mi place", "Elimină Nu-mi place", "Omisă la redarea automată", "Anulează"),
    "el" to TrackDislikeCopy("Δεν μου αρέσει", "Αφαίρεση Δεν μου αρέσει", "Παραλείπεται στην αυτόματη αναπαραγωγή", "Αναίρεση"),
    "sv" to TrackDislikeCopy("Gillar inte", "Ta bort Gillar inte", "Hoppas över vid automatisk uppspelning", "Ångra"),
    "da" to TrackDislikeCopy("Kan ikke lide", "Fjern Kan ikke lide", "Springes over ved automatisk afspilning", "Fortryd"),
    "cs" to TrackDislikeCopy("Nelíbí se mi", "Odebrat Nelíbí se mi", "Přeskočeno při automatickém přehrávání", "Zpět"),
    "sk" to TrackDislikeCopy("Nepáči sa mi", "Odstrániť Nepáči sa mi", "Preskočené pri automatickom prehrávaní", "Späť"),
    "bn" to TrackDislikeCopy("পছন্দ নয়", "অপছন্দ সরান", "অটোপ্লেতে বাদ দেওয়া হয়", "পূর্বাবস্থায় ফেরান"),
    "kk" to TrackDislikeCopy("Ұнамайды", "Ұнамайды белгісін алу", "Автоойнатуда өткізіледі", "Болдырмау"),
    "sr" to TrackDislikeCopy("Не свиђа ми се", "Уклони Не свиђа ми се", "Прескаче се у аутоматској репродукцији", "Опозови"),
    "sq" to TrackDislikeCopy("Nuk më pëlqen", "Hiq Nuk më pëlqen", "Kapërcehet në luajtjen automatike", "Zhbëj"),
    "hr" to TrackDislikeCopy("Ne sviđa mi se", "Ukloni Ne sviđa mi se", "Preskače se u automatskoj reprodukciji", "Poništi"),
    "bg" to TrackDislikeCopy("Не ми харесва", "Премахни Не ми харесва", "Пропуска се при автоматично възпроизвеждане", "Отмяна"),
    "hu" to TrackDislikeCopy("Nem tetszik", "Nem tetszik eltávolítása", "Automatikus lejátszáskor kihagyva", "Visszavonás"),
    "fi" to TrackDislikeCopy("En pidä", "Poista En pidä", "Ohitetaan automaattisessa toistossa", "Kumoa"),
    "et" to TrackDislikeCopy("Ei meeldi", "Eemalda Ei meeldi", "Automaatesitusel vahele jäetud", "Võta tagasi"),
    "nb" to TrackDislikeCopy("Liker ikke", "Fjern Liker ikke", "Hoppes over ved automatisk avspilling", "Angre"),
    "ca" to TrackDislikeCopy("No m'agrada", "Treu No m'agrada", "S'omet a la reproducció automàtica", "Desfés"),
    "uk" to TrackDislikeCopy("Не подобається", "Прибрати Не подобається", "Пропускається під час автовідтворення", "Скасувати"),
    "ru" to TrackDislikeCopy("Не нравится", "Убрать Не нравится", "Пропускается при автовоспроизведении", "Отменить"),
    "tr" to TrackDislikeCopy("Beğenmedim", "Beğenmedim işaretini kaldır", "Otomatik oynatmada atlanır", "Geri al"),
    "ar" to TrackDislikeCopy("لا يعجبني", "إزالة عدم الإعجاب", "يتم تخطيه في التشغيل التلقائي", "تراجع"),
    "fa" to TrackDislikeCopy("نمی‌پسندم", "حذف نپسندیدن", "در پخش خودکار رد می‌شود", "واگرد"),
    "zh" to TrackDislikeCopy("不喜欢", "取消不喜欢", "自动播放时跳过", "撤销"),
    "zh-Hant" to TrackDislikeCopy("不喜歡", "取消不喜歡", "自動播放時略過", "復原"),
    "ja" to TrackDislikeCopy("好きじゃない", "「好きじゃない」を解除", "自動再生でスキップ", "元に戻す"),
    "ko" to TrackDislikeCopy("싫어요", "싫어요 취소", "자동 재생에서 건너뜀", "실행 취소"),
    "hi" to TrackDislikeCopy("पसंद नहीं", "नापसंद हटाएं", "ऑटोप्ले में छोड़ा जाता है", "पहले जैसा करें"),
    "id" to TrackDislikeCopy("Tidak suka", "Hapus Tidak suka", "Dilewati saat putar otomatis", "Urungkan"),
    "ms" to TrackDislikeCopy("Tidak suka", "Alih keluar Tidak suka", "Dilangkau semasa main automatik", "Buat asal"),
    "vi" to TrackDislikeCopy("Không thích", "Bỏ Không thích", "Bị bỏ qua khi tự động phát", "Hoàn tác"),
    "th" to TrackDislikeCopy("ไม่ชอบ", "เลิกไม่ชอบ", "ข้ามเมื่อเล่นอัตโนมัติ", "เลิกทำ"),
    "fil" to TrackDislikeCopy("Hindi gusto", "Alisin ang Hindi gusto", "Nilalaktawan sa autoplay", "I-undo"),
    "he" to TrackDislikeCopy("לא אוהב", "הסרת לא אוהב", "מדולג בהפעלה אוטומטית", "ביטול")
)

internal fun trackDislikeCopy(code: String): TrackDislikeCopy =
    localizedValueOrEnglish(trackDislikeCopies, code)

internal fun trackDislikeLocalizationCodes(): Set<String> = trackDislikeCopies.keys
