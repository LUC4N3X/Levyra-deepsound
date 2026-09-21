package com.luc4n3x.levyra.ui.i18n

internal data class TechnicalAudioInfoCopy(
    val title: String,
    val subtitle: String,
    val runtime: String,
    val source: String,
    val outputAndDsp: String,
    val codec: String,
    val bitrate: String,
    val sampleRate: String,
    val channels: String,
    val mime: String,
    val container: String,
    val provider: String,
    val delivery: String,
    val quality: String,
    val bitDepth: String,
    val streamId: String,
    val loudness: String,
    val output: String,
    val route: String,
    val volume: String,
    val engine: String,
    val path: String,
    val processing: String,
    val audioSession: String,
    val verifiedSource: String,
    val confidence: String,
    val unavailable: String,
    val none: String,
    val requested: String,
    val fallback: String,
    val normalization: String,
    val equalizer: String,
    val limiter: String,
    val virtualizer: String,
    val preamp: String
)

internal fun LevyraStrings.technicalAudioInfoCopy(): TechnicalAudioInfoCopy = when (code) {
    "it" -> TechnicalAudioInfoCopy(
        "Info audio tecniche", "Formato reale, sorgente e percorso del segnale in tempo reale",
        "IN RIPRODUZIONE", "SORGENTE", "USCITA & DSP",
        "Codec", "Bitrate", "Frequenza", "Canali", "MIME", "Contenitore", "Provider",
        "Trasporto", "Qualità", "Profondità", "Stream", "Loudness", "Uscita", "Percorso",
        "Volume", "Motore", "Pipeline", "Elaborazione", "Sessione audio", "Sorgente verificata",
        "Affidabilità", "Non disponibile", "Nessuna", "Richiesto", "Fallback",
        "Normalizzazione", "Equalizzatore", "Limiter", "Virtualizer", "Preamp"
    )
    "es" -> TechnicalAudioInfoCopy(
        "Información técnica de audio", "Formato real, fuente y ruta de señal en tiempo real",
        "EN REPRODUCCIÓN", "FUENTE", "SALIDA Y DSP",
        "Códec", "Bitrate", "Frecuencia", "Canales", "MIME", "Contenedor", "Proveedor",
        "Transporte", "Calidad", "Profundidad", "Stream", "Loudness", "Salida", "Ruta",
        "Volumen", "Motor", "Pipeline", "Procesamiento", "Sesión de audio", "Fuente verificada",
        "Confianza", "No disponible", "Ninguno", "Solicitado", "Fallback",
        "Normalización", "Ecualizador", "Limiter", "Virtualizador", "Preamp"
    )
    "fr" -> TechnicalAudioInfoCopy(
        "Infos audio techniques", "Format réel, source et chemin du signal en temps réel",
        "LECTURE EN COURS", "SOURCE", "SORTIE & DSP",
        "Codec", "Débit", "Fréquence", "Canaux", "MIME", "Conteneur", "Fournisseur",
        "Transport", "Qualité", "Profondeur", "Flux", "Loudness", "Sortie", "Route",
        "Volume", "Moteur", "Pipeline", "Traitement", "Session audio", "Source vérifiée",
        "Confiance", "Indisponible", "Aucun", "Demandé", "Fallback",
        "Normalisation", "Égaliseur", "Limiteur", "Virtualizer", "Préampli"
    )
    "de" -> TechnicalAudioInfoCopy(
        "Technische Audio-Infos", "Reales Format, Quelle und Signalweg in Echtzeit",
        "AKTUELLE WIEDERGABE", "QUELLE", "AUSGABE & DSP",
        "Codec", "Bitrate", "Abtastrate", "Kanäle", "MIME", "Container", "Provider",
        "Übertragung", "Qualität", "Bittiefe", "Stream", "Loudness", "Ausgabe", "Route",
        "Lautstärke", "Engine", "Pipeline", "Verarbeitung", "Audio-Session", "Verifizierte Quelle",
        "Vertrauen", "Nicht verfügbar", "Keine", "Angefordert", "Fallback",
        "Normalisierung", "Equalizer", "Limiter", "Virtualizer", "Preamp"
    )
    "pt" -> TechnicalAudioInfoCopy(
        "Informação técnica de áudio", "Formato real, fonte e percurso do sinal em tempo real",
        "A REPRODUZIR", "FONTE", "SAÍDA & DSP",
        "Codec", "Bitrate", "Frequência", "Canais", "MIME", "Contentor", "Provider",
        "Transporte", "Qualidade", "Profundidade", "Stream", "Loudness", "Saída", "Rota",
        "Volume", "Motor", "Pipeline", "Processamento", "Sessão de áudio", "Fonte verificada",
        "Confiança", "Indisponível", "Nenhum", "Pedido", "Fallback",
        "Normalização", "Equalizador", "Limiter", "Virtualizer", "Preamp"
    )
    "ja" -> TechnicalAudioInfoCopy(
        "技術オーディオ情報", "実際の再生フォーマット、ソース、信号経路をリアルタイム表示",
        "再生中", "ソース", "出力 & DSP",
        "コーデック", "ビットレート", "サンプルレート", "チャンネル", "MIME", "コンテナ", "プロバイダー",
        "配信方式", "品質", "ビット深度", "ストリーム", "ラウドネス", "出力", "ルート",
        "音量", "エンジン", "パイプライン", "処理", "オーディオセッション", "検証済みソース",
        "信頼度", "利用不可", "なし", "要求済み", "フォールバック",
        "ノーマライズ", "イコライザー", "リミッター", "バーチャライザー", "プリアンプ"
    )
    else -> TechnicalAudioInfoCopy(
        "Technical audio info", "Live playback format, source and signal path",
        "NOW PLAYING", "SOURCE", "OUTPUT & DSP",
        "Codec", "Bitrate", "Sample rate", "Channels", "MIME", "Container", "Provider",
        "Delivery", "Quality", "Bit depth", "Stream", "Loudness", "Output", "Route",
        "Volume", "Engine", "Pipeline", "Processing", "Audio session", "Verified source",
        "Confidence", "Not reported", "None", "Requested", "Fallback",
        "Normalization", "Equalizer", "Limiter", "Virtualizer", "Preamp"
    )
}
