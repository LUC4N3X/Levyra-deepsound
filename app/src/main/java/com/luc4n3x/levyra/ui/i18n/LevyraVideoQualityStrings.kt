package com.luc4n3x.levyra.ui.i18n

internal val videoQualityKeys = setOf(
    "videoQuality",
    "videoQualitySubtitle",
    "videoQualityAuto",
    "videoQualityAutoSubtitle"
)

private fun videoQuality(
    quality: String,
    subtitle: String,
    auto: String,
    autoSubtitle: String
): Map<String, String> = mapOf(
    "videoQuality" to quality,
    "videoQualitySubtitle" to subtitle,
    "videoQualityAuto" to auto,
    "videoQualityAutoSubtitle" to autoSubtitle
)

private val videoQualityBundles: Map<String, Map<String, String>> = mapOf(
    "en" to videoQuality(
        "Video quality",
        "Default resolution for native video playback",
        "Auto",
        "Adjusts to your connection"
    ),
    "it" to videoQuality(
        "Qualità video",
        "Risoluzione predefinita per la riproduzione video nativa",
        "Automatica",
        "Si adatta alla tua connessione"
    ),
    "es" to videoQuality(
        "Calidad de vídeo",
        "Resolución predeterminada para la reproducción de vídeo nativa",
        "Automática",
        "Se adapta a tu conexión"
    ),
    "fr" to videoQuality(
        "Qualité vidéo",
        "Résolution par défaut pour la lecture vidéo native",
        "Automatique",
        "S'adapte à votre connexion"
    ),
    "de" to videoQuality(
        "Videoqualität",
        "Standardauflösung für die native Videowiedergabe",
        "Automatisch",
        "Passt sich deiner Verbindung an"
    ),
    "pt" to videoQuality(
        "Qualidade do vídeo",
        "Resolução padrão para reprodução de vídeo nativa",
        "Automática",
        "Ajusta-se à sua ligação"
    ),
    "ru" to videoQuality(
        "Качество видео",
        "Разрешение по умолчанию для нативного воспроизведения видео",
        "Авто",
        "Подстраивается под ваше соединение"
    ),
    "ja" to videoQuality(
        "動画品質",
        "ネイティブ動画再生のデフォルト解像度",
        "自動",
        "接続状況に合わせて調整"
    )
)

internal fun videoQualityLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(videoQualityBundles, code)
