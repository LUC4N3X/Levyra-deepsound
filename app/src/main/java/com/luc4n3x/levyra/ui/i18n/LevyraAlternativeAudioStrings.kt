package com.luc4n3x.levyra.ui.i18n

private fun alternativeAudioStrings(
    title: String,
    subtitle: String,
    off: String,
    automatic: String,
    prefer320: String,
    preferLossless: String
): Map<String, String> = mapOf(
    "alternativeAudioTitle" to title,
    "alternativeAudioSubtitle" to subtitle,
    "alternativeAudioOff" to off,
    "alternativeAudioAutomatic" to automatic,
    "alternativeAudioPrefer320" to prefer320,
    "alternativeAudioPreferLossless" to preferLossless
)

private val alternativeAudioBundles: Map<String, Map<String, String>> = mapOf(
    "en" to alternativeAudioStrings(
        "High-quality alternative audio",
        "Use a higher-quality external audio stream when Levyra can verify that it is the exact same track.",
        "Off",
        "Automatic",
        "Prefer 320 kbps",
        "Prefer FLAC lossless"
    ),
    "it" to alternativeAudioStrings(
        "Audio alternativo ad alta qualità",
        "Usa uno stream audio esterno di qualità superiore quando Levyra può verificare che si tratti esattamente dello stesso brano.",
        "Disattivato",
        "Automatico",
        "Preferisci 320 kbps",
        "Preferisci FLAC lossless"
    ),
    "es" to alternativeAudioStrings(
        "Audio alternativo de alta calidad",
        "Usa una transmisión de audio externa de mayor calidad cuando Levyra puede verificar que es exactamente la misma canción.",
        "Desactivado",
        "Automático",
        "Preferir 320 kbps",
        "Preferir FLAC sin pérdida"
    ),
    "fr" to alternativeAudioStrings(
        "Audio alternatif haute qualité",
        "Utilise un flux audio externe de meilleure qualité lorsque Levyra peut vérifier qu’il s’agit exactement du même titre.",
        "Désactivé",
        "Automatique",
        "Privilégier 320 kbps",
        "Privilégier FLAC sans perte"
    ),
    "de" to alternativeAudioStrings(
        "Alternatives Audio in hoher Qualität",
        "Verwendet einen externen Audiostream in höherer Qualität, wenn Levyra bestätigen kann, dass es genau derselbe Titel ist.",
        "Aus",
        "Automatisch",
        "320 kbps bevorzugen",
        "FLAC verlustfrei bevorzugen"
    ),
    "pt" to alternativeAudioStrings(
        "Áudio alternativo de alta qualidade",
        "Usa um stream de áudio externo de maior qualidade quando o Levyra consegue verificar que é exatamente a mesma faixa.",
        "Desativado",
        "Automático",
        "Preferir 320 kbps",
        "Preferir FLAC sem perdas"
    ),
    "nl" to alternativeAudioStrings(
        "Alternatieve audio van hoge kwaliteit",
        "Gebruik een externe audiostream van hogere kwaliteit wanneer Levyra kan verifiëren dat het exact hetzelfde nummer is.",
        "Uit",
        "Automatisch",
        "Voorkeur voor 320 kbps",
        "Voorkeur voor FLAC lossless"
    ),
    "pl" to alternativeAudioStrings(
        "Alternatywny dźwięk wysokiej jakości",
        "Używaj zewnętrznego strumienia audio wyższej jakości, gdy Levyra może potwierdzić, że to dokładnie ten sam utwór.",
        "Wyłączone",
        "Automatycznie",
        "Preferuj 320 kb/s",
        "Preferuj bezstratny FLAC"
    ),
    "ro" to alternativeAudioStrings(
        "Audio alternativ de înaltă calitate",
        "Folosește un flux audio extern de calitate superioară când Levyra poate verifica faptul că este exact aceeași piesă.",
        "Dezactivat",
        "Automat",
        "Preferă 320 kbps",
        "Preferă FLAC fără pierderi"
    ),
    "el" to alternativeAudioStrings(
        "Εναλλακτικός ήχος υψηλής ποιότητας",
        "Χρησιμοποιεί εξωτερική ροή ήχου υψηλότερης ποιότητας όταν το Levyra μπορεί να επιβεβαιώσει ότι είναι ακριβώς το ίδιο κομμάτι.",
        "Ανενεργό",
        "Αυτόματα",
        "Προτίμηση 320 kbps",
        "Προτίμηση FLAC χωρίς απώλειες"
    ),
    "sv" to alternativeAudioStrings(
        "Alternativt ljud i hög kvalitet",
        "Använd en extern ljudström med högre kvalitet när Levyra kan verifiera att det är exakt samma låt.",
        "Av",
        "Automatiskt",
        "Föredra 320 kbps",
        "Föredra förlustfri FLAC"
    ),
    "da" to alternativeAudioStrings(
        "Alternativ lyd i høj kvalitet",
        "Brug en ekstern lydstream i højere kvalitet, når Levyra kan bekræfte, at det er præcis det samme nummer.",
        "Fra",
        "Automatisk",
        "Foretræk 320 kbps",
        "Foretræk tabsfri FLAC"
    ),
    "cs" to alternativeAudioStrings(
        "Alternativní zvuk ve vysoké kvalitě",
        "Použije externí zvukový stream vyšší kvality, když Levyra ověří, že jde přesně o stejnou skladbu.",
        "Vypnuto",
        "Automaticky",
        "Upřednostnit 320 kb/s",
        "Upřednostnit bezztrátový FLAC"
    ),
    "uk" to alternativeAudioStrings(
        "Альтернативне аудіо високої якості",
        "Використовувати зовнішній аудіопотік вищої якості, коли Levyra може підтвердити, що це точно той самий трек.",
        "Вимкнено",
        "Автоматично",
        "Надавати перевагу 320 кбіт/с",
        "Надавати перевагу FLAC без втрат"
    ),
    "ru" to alternativeAudioStrings(
        "Альтернативный звук высокого качества",
        "Использовать внешний аудиопоток более высокого качества, когда Levyra может подтвердить, что это точно тот же трек.",
        "Выкл.",
        "Автоматически",
        "Предпочитать 320 кбит/с",
        "Предпочитать FLAC без потерь"
    ),
    "tr" to alternativeAudioStrings(
        "Yüksek kaliteli alternatif ses",
        "Levyra tam olarak aynı parça olduğunu doğrulayabildiğinde daha yüksek kaliteli harici bir ses akışı kullanır.",
        "Kapalı",
        "Otomatik",
        "320 kbps tercih et",
        "Kayıpsız FLAC tercih et"
    ),
    "ar" to alternativeAudioStrings(
        "صوت بديل عالي الجودة",
        "استخدم بث صوت خارجيًا بجودة أعلى عندما يتمكن Levyra من التحقق من أنه المقطع نفسه تمامًا.",
        "إيقاف",
        "تلقائي",
        "تفضيل 320 كيلوبت/ث",
        "تفضيل FLAC بدون فقدان"
    ),
    "zh" to alternativeAudioStrings(
        "高音质备用音频",
        "当 Levyra 能确认是完全相同的曲目时，使用更高音质的外部音频流。",
        "关闭",
        "自动",
        "优先 320 kbps",
        "优先 FLAC 无损"
    ),
    "ja" to alternativeAudioStrings(
        "高音質の代替オーディオ",
        "Levyra がまったく同じ曲であることを確認できた場合に、より高音質な外部オーディオストリームを使用します。",
        "オフ",
        "自動",
        "320 kbps を優先",
        "FLAC ロスレスを優先"
    ),
    "ko" to alternativeAudioStrings(
        "고음질 대체 오디오",
        "Levyra가 정확히 같은 곡임을 확인할 수 있을 때 더 높은 음질의 외부 오디오 스트림을 사용합니다.",
        "끄기",
        "자동",
        "320kbps 우선",
        "FLAC 무손실 우선"
    ),
    "hi" to alternativeAudioStrings(
        "उच्च गुणवत्ता वाला वैकल्पिक ऑडियो",
        "जब Levyra पुष्टि कर सके कि यह बिल्कुल वही ट्रैक है, तब उच्च गुणवत्ता वाली बाहरी ऑडियो स्ट्रीम का उपयोग करें।",
        "बंद",
        "स्वचालित",
        "320 kbps को प्राथमिकता दें",
        "FLAC लॉसलेस को प्राथमिकता दें"
    ),
    "id" to alternativeAudioStrings(
        "Audio alternatif berkualitas tinggi",
        "Gunakan stream audio eksternal berkualitas lebih tinggi saat Levyra dapat memastikan bahwa lagunya benar-benar sama.",
        "Nonaktif",
        "Otomatis",
        "Utamakan 320 kbps",
        "Utamakan FLAC lossless"
    ),
    "vi" to alternativeAudioStrings(
        "Âm thanh thay thế chất lượng cao",
        "Dùng luồng âm thanh bên ngoài chất lượng cao hơn khi Levyra xác minh được đó chính xác là cùng một bài hát.",
        "Tắt",
        "Tự động",
        "Ưu tiên 320 kbps",
        "Ưu tiên FLAC lossless"
    ),
    "th" to alternativeAudioStrings(
        "เสียงทางเลือกคุณภาพสูง",
        "ใช้สตรีมเสียงภายนอกที่มีคุณภาพสูงกว่าเมื่อ Levyra ยืนยันได้ว่าเป็นเพลงเดียวกันทุกประการ",
        "ปิด",
        "อัตโนมัติ",
        "เลือก 320 kbps ก่อน",
        "เลือก FLAC แบบไม่สูญเสียก่อน"
    ),
    "fil" to alternativeAudioStrings(
        "Mataas na kalidad na alternatibong audio",
        "Gumamit ng mas mataas na kalidad na panlabas na audio stream kapag natiyak ng Levyra na eksaktong parehong track ito.",
        "Naka-off",
        "Awtomatiko",
        "Mas gusto ang 320 kbps",
        "Mas gusto ang FLAC lossless"
    ),
    "he" to alternativeAudioStrings(
        "שמע חלופי באיכות גבוהה",
        "שימוש בזרם שמע חיצוני באיכות גבוהה יותר כש-Levyra יכולה לאמת שזה בדיוק אותו שיר.",
        "כבוי",
        "אוטומטי",
        "העדפת 320 kbps",
        "העדפת FLAC ללא אובדן"
    ),
    "fi" to alternativeAudioStrings(
        "Korkealaatuinen vaihtoehtoinen ääni",
        "Käytä korkealaatuisempaa ulkoista äänilähdettä, kun Levyra voi varmistaa sen olevan täsmälleen sama kappale.",
        "Pois",
        "Automaattinen",
        "Suosi 320 kbps",
        "Suosi häviötöntä FLACia"
    ),
    "et" to alternativeAudioStrings(
        "Kõrge kvaliteediga alternatiivne heli",
        "Kasuta kvaliteetsemat välist helivoogu, kui Levyra saab kinnitada, et tegemist on täpselt sama looga.",
        "Väljas",
        "Automaatne",
        "Eelista 320 kbps",
        "Eelista kadudeta FLAC-i"
    )
)

private val additionalLosslessLabels = mapOf(
    "sk" to "Uprednostniť bezstratový FLAC",
    "hr" to "Prednost FLAC-u bez gubitaka",
    "sr" to "Предност FLAC-у без губитака",
    "sq" to "Prefero FLAC pa humbje",
    "bg" to "Предпочитане на FLAC без загуби",
    "hu" to "Veszteségmentes FLAC előnyben",
    "nb" to "Foretrekk tapsfri FLAC",
    "ca" to "Prefereix FLAC sense pèrdua",
    "kk" to "Жоғалтусыз FLAC пішімін таңдау",
    "fa" to "ترجیح FLAC بدون افت کیفیت",
    "zh-Hant" to "優先使用 FLAC 無損音訊",
    "bn" to "ক্ষতিহীন FLAC অগ্রাধিকার দিন",
    "ms" to "Utamakan FLAC tanpa kehilangan kualiti"
)

private val qobuzSubtitles = mapOf(
    "en" to "Optional FLAC source. Availability and quality are verified for each track.",
    "it" to "Sorgente FLAC opzionale. Disponibilità e qualità verificate per ogni brano.",
    "es" to "Fuente FLAC opcional. Disponibilidad y calidad verificadas para cada canción.",
    "fr" to "Source FLAC facultative. Disponibilité et qualité vérifiées pour chaque titre.",
    "de" to "Optionale FLAC-Quelle. Verfügbarkeit und Qualität werden für jeden Titel geprüft.",
    "pt" to "Fonte FLAC opcional. Disponibilidade e qualidade verificadas para cada faixa.",
    "nl" to "Optionele FLAC-bron. Beschikbaarheid en kwaliteit worden per nummer gecontroleerd.",
    "pl" to "Opcjonalne źródło FLAC. Dostępność i jakość są sprawdzane dla każdego utworu.",
    "ro" to "Sursă FLAC opțională. Disponibilitatea și calitatea sunt verificate pentru fiecare piesă.",
    "el" to "Προαιρετική πηγή FLAC. Η διαθεσιμότητα και η ποιότητα ελέγχονται για κάθε κομμάτι.",
    "sv" to "Valfri FLAC-källa. Tillgänglighet och kvalitet verifieras för varje låt.",
    "da" to "Valgfri FLAC-kilde. Tilgængelighed og kvalitet kontrolleres for hvert nummer.",
    "cs" to "Volitelný zdroj FLAC. Dostupnost a kvalita se ověřují u každé skladby.",
    "uk" to "Додаткове джерело FLAC. Доступність і якість перевіряються для кожного треку.",
    "ru" to "Дополнительный источник FLAC. Доступность и качество проверяются для каждого трека.",
    "tr" to "İsteğe bağlı FLAC kaynağı. Her parça için kullanılabilirlik ve kalite doğrulanır.",
    "ar" to "مصدر FLAC اختياري. يتم التحقق من التوفر والجودة لكل مقطع.",
    "zh" to "可选 FLAC 音源。逐曲验证可用性与音质。",
    "ja" to "任意の FLAC 音源。曲ごとに利用可否と音質を検証します。",
    "ko" to "선택적 FLAC 음원. 각 곡의 사용 가능 여부와 음질을 확인합니다.",
    "hi" to "वैकल्पिक FLAC स्रोत। हर ट्रैक की उपलब्धता और गुणवत्ता सत्यापित की जाती है।",
    "id" to "Sumber FLAC opsional. Ketersediaan dan kualitas diverifikasi untuk setiap lagu.",
    "vi" to "Nguồn FLAC tùy chọn. Xác minh khả dụng và chất lượng cho từng bài hát.",
    "th" to "แหล่ง FLAC เสริม ตรวจสอบความพร้อมและคุณภาพของแต่ละเพลง",
    "fil" to "Opsyonal na FLAC source. Sinusuri ang availability at kalidad ng bawat track.",
    "he" to "מקור FLAC אופציונלי. הזמינות והאיכות נבדקות לכל שיר.",
    "fi" to "Valinnainen FLAC-lähde. Saatavuus ja laatu tarkistetaan jokaiselle kappaleelle.",
    "et" to "Valikuline FLAC-allikas. Saadavust ja kvaliteeti kontrollitakse iga loo puhul.",
    "sk" to "Voliteľný zdroj FLAC. Dostupnosť a kvalita sa overujú pri každej skladbe.",
    "hr" to "Neobavezni FLAC izvor. Dostupnost i kvaliteta provjeravaju se za svaku pjesmu.",
    "sr" to "Опциони FLAC извор. Доступност и квалитет се проверавају за сваку песму.",
    "sq" to "Burim FLAC opsional. Disponueshmëria dhe cilësia verifikohen për çdo këngë.",
    "bg" to "Допълнителен FLAC източник. Наличността и качеството се проверяват за всяка песен.",
    "hu" to "Opcionális FLAC-forrás. Az elérhetőséget és a minőséget minden számnál ellenőrizzük.",
    "nb" to "Valgfri FLAC-kilde. Tilgjengelighet og kvalitet kontrolleres for hvert spor.",
    "ca" to "Font FLAC opcional. Disponibilitat i qualitat verificades per a cada cançó.",
    "kk" to "Қосымша FLAC көзі. Әр тректің қолжетімділігі мен сапасы тексеріледі.",
    "fa" to "منبع اختیاری FLAC. دسترسی و کیفیت برای هر قطعه بررسی می‌شود.",
    "zh-Hant" to "可選 FLAC 音源。逐曲驗證可用性與音質。",
    "bn" to "ঐচ্ছিক FLAC উৎস। প্রতিটি ট্র্যাকের উপলব্ধতা ও মান যাচাই করা হয়।",
    "ms" to "Sumber FLAC pilihan. Ketersediaan dan kualiti disahkan untuk setiap lagu."
)

internal fun alternativeAudioLocalizationEntries(code: String): Map<String, String> {
    val bundle = localizedBundleOrEnglish(alternativeAudioBundles, code) +
        ("qobuzAudioSubtitle" to (qobuzSubtitles[code] ?: qobuzSubtitles.getValue("en")))
    val lossless = additionalLosslessLabels[code] ?: return bundle
    return bundle + ("alternativeAudioPreferLossless" to lossless)
}

internal fun alternativeAudioLocalizationCodes(): Set<String> =
    alternativeAudioBundles.keys + additionalLosslessLabels.keys

internal val alternativeAudioKeys: Set<String> = setOf(
    "alternativeAudioTitle",
    "alternativeAudioSubtitle",
    "alternativeAudioOff",
    "alternativeAudioAutomatic",
    "alternativeAudioPrefer320",
    "alternativeAudioPreferLossless",
    "qobuzAudioSubtitle"
)
