package com.luc4n3x.levyra.ui.i18n

private fun alternativeAudioStrings(
    title: String,
    subtitle: String,
    off: String,
    automatic: String,
    prefer320: String
): Map<String, String> = mapOf(
    "alternativeAudioTitle" to title,
    "alternativeAudioSubtitle" to subtitle,
    "alternativeAudioOff" to off,
    "alternativeAudioAutomatic" to automatic,
    "alternativeAudioPrefer320" to prefer320
)

private val alternativeAudioBundles: Map<String, Map<String, String>> = mapOf(
    "en" to alternativeAudioStrings(
        "High-quality alternative audio",
        "Use a higher-quality external audio stream when Levyra can verify that it is the exact same track.",
        "Off",
        "Automatic",
        "Prefer maximum quality"
    ),
    "it" to alternativeAudioStrings(
        "Audio alternativo ad alta qualità",
        "Usa uno stream audio esterno di qualità superiore quando Levyra può verificare che si tratti esattamente dello stesso brano.",
        "Disattivato",
        "Automatico",
        "Preferisci la massima qualità"
    ),
    "es" to alternativeAudioStrings(
        "Audio alternativo de alta calidad",
        "Usa una transmisión de audio externa de mayor calidad cuando Levyra puede verificar que es exactamente la misma canción.",
        "Desactivado",
        "Automático",
        "Preferir la máxima calidad"
    ),
    "fr" to alternativeAudioStrings(
        "Audio alternatif haute qualité",
        "Utilise un flux audio externe de meilleure qualité lorsque Levyra peut vérifier qu’il s’agit exactement du même titre.",
        "Désactivé",
        "Automatique",
        "Privilégier la qualité maximale"
    ),
    "de" to alternativeAudioStrings(
        "Alternatives Audio in hoher Qualität",
        "Verwendet einen externen Audiostream in höherer Qualität, wenn Levyra bestätigen kann, dass es genau derselbe Titel ist.",
        "Aus",
        "Automatisch",
        "Maximale Qualität bevorzugen"
    ),
    "pt" to alternativeAudioStrings(
        "Áudio alternativo de alta qualidade",
        "Usa um stream de áudio externo de maior qualidade quando o Levyra consegue verificar que é exatamente a mesma faixa.",
        "Desativado",
        "Automático",
        "Preferir a qualidade máxima"
    ),
    "nl" to alternativeAudioStrings(
        "Alternatieve audio van hoge kwaliteit",
        "Gebruik een externe audiostream van hogere kwaliteit wanneer Levyra kan verifiëren dat het exact hetzelfde nummer is.",
        "Uit",
        "Automatisch",
        "Voorkeur voor maximale kwaliteit"
    ),
    "pl" to alternativeAudioStrings(
        "Alternatywny dźwięk wysokiej jakości",
        "Używaj zewnętrznego strumienia audio wyższej jakości, gdy Levyra może potwierdzić, że to dokładnie ten sam utwór.",
        "Wyłączone",
        "Automatycznie",
        "Preferuj maksymalną jakość"
    ),
    "ro" to alternativeAudioStrings(
        "Audio alternativ de înaltă calitate",
        "Folosește un flux audio extern de calitate superioară când Levyra poate verifica faptul că este exact aceeași piesă.",
        "Dezactivat",
        "Automat",
        "Preferă calitatea maximă"
    ),
    "el" to alternativeAudioStrings(
        "Εναλλακτικός ήχος υψηλής ποιότητας",
        "Χρησιμοποιεί εξωτερική ροή ήχου υψηλότερης ποιότητας όταν το Levyra μπορεί να επιβεβαιώσει ότι είναι ακριβώς το ίδιο κομμάτι.",
        "Ανενεργό",
        "Αυτόματα",
        "Προτίμηση μέγιστης ποιότητας"
    ),
    "sv" to alternativeAudioStrings(
        "Alternativt ljud i hög kvalitet",
        "Använd en extern ljudström med högre kvalitet när Levyra kan verifiera att det är exakt samma låt.",
        "Av",
        "Automatiskt",
        "Föredra högsta kvalitet"
    ),
    "da" to alternativeAudioStrings(
        "Alternativ lyd i høj kvalitet",
        "Brug en ekstern lydstream i højere kvalitet, når Levyra kan bekræfte, at det er præcis det samme nummer.",
        "Fra",
        "Automatisk",
        "Foretræk højeste kvalitet"
    ),
    "cs" to alternativeAudioStrings(
        "Alternativní zvuk ve vysoké kvalitě",
        "Použije externí zvukový stream vyšší kvality, když Levyra ověří, že jde přesně o stejnou skladbu.",
        "Vypnuto",
        "Automaticky",
        "Upřednostnit nejvyšší kvalitu"
    ),
    "uk" to alternativeAudioStrings(
        "Альтернативне аудіо високої якості",
        "Використовувати зовнішній аудіопотік вищої якості, коли Levyra може підтвердити, що це точно той самий трек.",
        "Вимкнено",
        "Автоматично",
        "Надавати перевагу максимальній якості"
    ),
    "ru" to alternativeAudioStrings(
        "Альтернативный звук высокого качества",
        "Использовать внешний аудиопоток более высокого качества, когда Levyra может подтвердить, что это точно тот же трек.",
        "Выкл.",
        "Автоматически",
        "Предпочитать максимальное качество"
    ),
    "tr" to alternativeAudioStrings(
        "Yüksek kaliteli alternatif ses",
        "Levyra tam olarak aynı parça olduğunu doğrulayabildiğinde daha yüksek kaliteli harici bir ses akışı kullanır.",
        "Kapalı",
        "Otomatik",
        "En yüksek kaliteyi tercih et"
    ),
    "ar" to alternativeAudioStrings(
        "صوت بديل عالي الجودة",
        "استخدم بث صوت خارجيًا بجودة أعلى عندما يتمكن Levyra من التحقق من أنه المقطع نفسه تمامًا.",
        "إيقاف",
        "تلقائي",
        "تفضيل أعلى جودة"
    ),
    "zh" to alternativeAudioStrings(
        "高音质备用音频",
        "当 Levyra 能确认是完全相同的曲目时，使用更高音质的外部音频流。",
        "关闭",
        "自动",
        "优先最高音质"
    ),
    "ja" to alternativeAudioStrings(
        "高音質の代替オーディオ",
        "Levyra がまったく同じ曲であることを確認できた場合に、より高音質な外部オーディオストリームを使用します。",
        "オフ",
        "自動",
        "最高音質を優先"
    ),
    "ko" to alternativeAudioStrings(
        "고음질 대체 오디오",
        "Levyra가 정확히 같은 곡임을 확인할 수 있을 때 더 높은 음질의 외부 오디오 스트림을 사용합니다.",
        "끄기",
        "자동",
        "최고 음질 우선"
    ),
    "hi" to alternativeAudioStrings(
        "उच्च गुणवत्ता वाला वैकल्पिक ऑडियो",
        "जब Levyra पुष्टि कर सके कि यह बिल्कुल वही ट्रैक है, तब उच्च गुणवत्ता वाली बाहरी ऑडियो स्ट्रीम का उपयोग करें।",
        "बंद",
        "स्वचालित",
        "अधिकतम गुणवत्ता को प्राथमिकता दें"
    ),
    "id" to alternativeAudioStrings(
        "Audio alternatif berkualitas tinggi",
        "Gunakan stream audio eksternal berkualitas lebih tinggi saat Levyra dapat memastikan bahwa lagunya benar-benar sama.",
        "Nonaktif",
        "Otomatis",
        "Utamakan kualitas maksimum"
    ),
    "vi" to alternativeAudioStrings(
        "Âm thanh thay thế chất lượng cao",
        "Dùng luồng âm thanh bên ngoài chất lượng cao hơn khi Levyra xác minh được đó chính xác là cùng một bài hát.",
        "Tắt",
        "Tự động",
        "Ưu tiên chất lượng cao nhất"
    ),
    "th" to alternativeAudioStrings(
        "เสียงทางเลือกคุณภาพสูง",
        "ใช้สตรีมเสียงภายนอกที่มีคุณภาพสูงกว่าเมื่อ Levyra ยืนยันได้ว่าเป็นเพลงเดียวกันทุกประการ",
        "ปิด",
        "อัตโนมัติ",
        "เลือกคุณภาพสูงสุดก่อน"
    ),
    "fil" to alternativeAudioStrings(
        "Mataas na kalidad na alternatibong audio",
        "Gumamit ng mas mataas na kalidad na panlabas na audio stream kapag natiyak ng Levyra na eksaktong parehong track ito.",
        "Naka-off",
        "Awtomatiko",
        "Mas gusto ang pinakamataas na kalidad"
    ),
    "he" to alternativeAudioStrings(
        "שמע חלופי באיכות גבוהה",
        "שימוש בזרם שמע חיצוני באיכות גבוהה יותר כש-Levyra יכולה לאמת שזה בדיוק אותו שיר.",
        "כבוי",
        "אוטומטי",
        "העדפת איכות מרבית"
    )
)

internal fun alternativeAudioLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(alternativeAudioBundles, code)

internal fun alternativeAudioLocalizationCodes(): Set<String> = supportedLocalizationCodes()

internal val alternativeAudioKeys: Set<String> = setOf(
    "alternativeAudioTitle",
    "alternativeAudioSubtitle",
    "alternativeAudioOff",
    "alternativeAudioAutomatic",
    "alternativeAudioPrefer320"
)
