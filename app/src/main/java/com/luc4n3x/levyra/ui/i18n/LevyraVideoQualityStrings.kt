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
    )
)

internal fun videoQualityLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(videoQualityBundles, code)
