package com.luc4n3x.levyra.ui.i18n

internal val videoQualityKeys = setOf(
    "videoQuality",
    "videoQualitySubtitle",
    "videoQualityAuto",
    "videoQualityAutoSubtitle",
    "videoSource",
    "videoSourceSubtitle",
    "videoSourceOfficial",
    "videoSourceEmpty",
    "videoOptions"
)

private fun videoQuality(
    quality: String,
    subtitle: String,
    auto: String,
    autoSubtitle: String,
    source: String,
    sourceSubtitle: String,
    sourceOfficial: String,
    sourceEmpty: String,
    options: String
): Map<String, String> = mapOf(
    "videoQuality" to quality,
    "videoQualitySubtitle" to subtitle,
    "videoQualityAuto" to auto,
    "videoQualityAutoSubtitle" to autoSubtitle,
    "videoSource" to source,
    "videoSourceSubtitle" to sourceSubtitle,
    "videoSourceOfficial" to sourceOfficial,
    "videoSourceEmpty" to sourceEmpty,
    "videoOptions" to options
)

private val videoQualityBundles: Map<String, Map<String, String>> = mapOf(
    "en" to videoQuality(
        "Video quality",
        "Default resolution for native video playback",
        "Auto",
        "Adjusts to your connection",
        "Video source",
        "Choose which video plays with this song",
        "Official",
        "No other matching videos found",
        "Video options"
    ),
    "it" to videoQuality(
        "Qualità video",
        "Risoluzione predefinita per la riproduzione video nativa",
        "Automatica",
        "Si adatta alla tua connessione",
        "Sorgente video",
        "Scegli quale video accompagna questo brano",
        "Ufficiale",
        "Nessun altro video corrispondente",
        "Opzioni video"
    ),
    "es" to videoQuality(
        "Calidad de vídeo",
        "Resolución predeterminada para la reproducción de vídeo nativa",
        "Automática",
        "Se adapta a tu conexión",
        "Fuente de vídeo",
        "Elige qué vídeo acompaña esta canción",
        "Oficial",
        "No hay otros vídeos que coincidan",
        "Opciones de vídeo"
    ),
    "fr" to videoQuality(
        "Qualité vidéo",
        "Résolution par défaut pour la lecture vidéo native",
        "Automatique",
        "S'adapte à votre connexion",
        "Source vidéo",
        "Choisis la vidéo associée à ce titre",
        "Officielle",
        "Aucune autre vidéo correspondante",
        "Options vidéo"
    ),
    "de" to videoQuality(
        "Videoqualität",
        "Standardauflösung für die native Videowiedergabe",
        "Automatisch",
        "Passt sich deiner Verbindung an",
        "Videoquelle",
        "Wähle, welches Video zu diesem Song läuft",
        "Offiziell",
        "Keine weiteren passenden Videos",
        "Videooptionen"
    ),
    "pt" to videoQuality(
        "Qualidade do vídeo",
        "Resolução padrão para reprodução de vídeo nativa",
        "Automática",
        "Ajusta-se à sua ligação",
        "Fonte do vídeo",
        "Escolhe o vídeo que acompanha esta música",
        "Oficial",
        "Não há outros vídeos correspondentes",
        "Opções de vídeo"
    ),
    "ru" to videoQuality(
        "Качество видео",
        "Разрешение по умолчанию для нативного воспроизведения видео",
        "Авто",
        "Подстраивается под ваше соединение",
        "Источник видео",
        "Выберите видео для этого трека",
        "Официальное",
        "Других подходящих видео нет",
        "Параметры видео"
    ),
    "ja" to videoQuality(
        "動画品質",
        "ネイティブ動画再生のデフォルト解像度",
        "自動",
        "接続状況に合わせて調整",
        "動画ソース",
        "この曲で再生する動画を選択",
        "公式",
        "他に一致する動画はありません",
        "動画オプション"
    ),
    "ur" to videoQuality(
        "ویڈیو کا معیار",
        "مقامی ویڈیو پلے بیک کے لیے ڈیفالٹ ریزولوشن",
        "آٹو",
        "آپ کے کنکشن کو ایڈجسٹ کرتا ہے۔",
        "ویڈیو ماخذ",
        "منتخب کریں کہ اس گانے کے ساتھ کون سی ویڈیو چلتی ہے۔",
        "سرکاری",
        "کوئی اور مماثل ویڈیوز نہیں ملے",
        "ویڈیو کے اختیارات"
    ),
    "ta" to videoQuality(
        "வீடியோ தரம்",
        "நேட்டிவ் வீடியோ பிளேபேக்கிற்கான இயல்புநிலை தெளிவுத்திறன்",
        "ஆட்டோ",
        "உங்கள் இணைப்பில் சரிசெய்கிறது",
        "வீடியோ ஆதாரம்",
        "இந்தப் பாடலுடன் எந்த வீடியோ இயங்க வேண்டும் என்பதைத் தேர்வுசெய்யவும்",
        "அதிகாரி",
        "பொருந்தும் வீடியோக்கள் எதுவும் இல்லை",
        "வீடியோ விருப்பங்கள்"
    ),
    "te" to videoQuality(
        "వీడియో నాణ్యత",
        "స్థానిక వీడియో ప్లేబ్యాక్ కోసం డిఫాల్ట్ రిజల్యూషన్",
        "ఆటో",
        "మీ కనెక్షన్‌కి సర్దుబాటు చేస్తుంది",
        "వీడియో మూలం",
        "ఈ పాటతో ఏ వీడియో ప్లే అవుతుందో ఎంచుకోండి",
        "అధికారిక",
        "సరిపోలే ఇతర వీడియోలు ఏవీ కనుగొనబడలేదు",
        "వీడియో ఎంపికలు"
    ),
    "mr" to videoQuality(
        "व्हिडिओ गुणवत्ता",
        "मूळ व्हिडिओ प्लेबॅकसाठी डीफॉल्ट रिझोल्यूशन",
        "ऑटो",
        "तुमच्या कनेक्शनशी जुळवून घेते",
        "व्हिडिओ स्रोत",
        "या गाण्यासोबत कोणता व्हिडिओ प्ले होतो ते निवडा",
        "अधिकृत",
        "इतर कोणतेही जुळणारे व्हिडिओ आढळले नाहीत",
        "व्हिडिओ पर्याय"
    ),
    "gu" to videoQuality(
        "વિડિઓ ગુણવત્તા",
        "મૂળ વિડિયો પ્લેબેક માટે ડિફોલ્ટ રિઝોલ્યુશન",
        "ઓટો",
        "તમારા કનેક્શનને સમાયોજિત કરે છે",
        "વિડિઓ સ્ત્રોત",
        "આ ગીત સાથે કયો વીડિયો ચાલે છે તે પસંદ કરો",
        "સત્તાવાર",
        "અન્ય કોઈ મેળ ખાતા વીડિયો મળ્યા નથી",
        "વિડિઓ વિકલ્પો"
    ),
    "pt-BR" to videoQuality(
        "Qualidade de vídeo",
        "Resolução padrão para reprodução de vídeo nativo",
        "Automático",
        "Ajusta-se à sua conexão",
        "Fonte de vídeo",
        "Escolha qual vídeo reproduz esta música",
        "Oficial",
        "Nenhum outro vídeo correspondente encontrado",
        "Opções de vídeo"
    )
)

internal fun videoQualityLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(videoQualityBundles, code)
