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
    val preamp: String,
    val codecId: String = "Codec ID",
    val remotePlayback: String = "Remote playback",
    val receiverManaged: String = "Managed by receiver"
)

internal fun LevyraStrings.technicalAudioInfoCopy(): TechnicalAudioInfoCopy = when (code) {
    "it" -> TechnicalAudioInfoCopy(
        "Info audio tecniche", "Formato reale, sorgente e percorso del segnale in tempo reale",
        "IN RIPRODUZIONE", "SORGENTE", "USCITA & DSP",
        "Codec", "Bitrate", "Frequenza", "Canali", "MIME", "Contenitore", "Provider",
        "Trasporto", "Qualità", "Profondità", "Stream", "Loudness", "Uscita", "Percorso",
        "Volume", "Motore", "Pipeline", "Elaborazione", "Sessione audio", "Sorgente verificata",
        "Affidabilità", "Non disponibile", "Nessuna", "Richiesto", "Fallback",
        "Normalizzazione", "Equalizzatore", "Limiter", "Virtualizer", "Preamp",
        codecId = "ID codec", remotePlayback = "Riproduzione remota", receiverManaged = "Gestito dal ricevitore"
    )
    "es" -> TechnicalAudioInfoCopy(
        "Información técnica de audio", "Formato real, fuente y ruta de señal en tiempo real",
        "EN REPRODUCCIÓN", "FUENTE", "SALIDA Y DSP",
        "Códec", "Bitrate", "Frecuencia", "Canales", "MIME", "Contenedor", "Proveedor",
        "Transporte", "Calidad", "Profundidad", "Stream", "Loudness", "Salida", "Ruta",
        "Volumen", "Motor", "Pipeline", "Procesamiento", "Sesión de audio", "Fuente verificada",
        "Confianza", "No disponible", "Ninguno", "Solicitado", "Fallback",
        "Normalización", "Ecualizador", "Limiter", "Virtualizador", "Preamp",
        codecId = "ID de códec", remotePlayback = "Reproducción remota", receiverManaged = "Gestionado por el receptor"
    )
    "fr" -> TechnicalAudioInfoCopy(
        "Infos audio techniques", "Format réel, source et chemin du signal en temps réel",
        "LECTURE EN COURS", "SOURCE", "SORTIE & DSP",
        "Codec", "Débit", "Fréquence", "Canaux", "MIME", "Conteneur", "Fournisseur",
        "Transport", "Qualité", "Profondeur", "Flux", "Loudness", "Sortie", "Route",
        "Volume", "Moteur", "Pipeline", "Traitement", "Session audio", "Source vérifiée",
        "Confiance", "Indisponible", "Aucun", "Demandé", "Fallback",
        "Normalisation", "Égaliseur", "Limiteur", "Virtualizer", "Préampli",
        codecId = "ID codec", remotePlayback = "Lecture à distance", receiverManaged = "Géré par le récepteur"
    )
    "de" -> TechnicalAudioInfoCopy(
        "Technische Audio-Infos", "Reales Format, Quelle und Signalweg in Echtzeit",
        "AKTUELLE WIEDERGABE", "QUELLE", "AUSGABE & DSP",
        "Codec", "Bitrate", "Abtastrate", "Kanäle", "MIME", "Container", "Provider",
        "Übertragung", "Qualität", "Bittiefe", "Stream", "Loudness", "Ausgabe", "Route",
        "Lautstärke", "Engine", "Pipeline", "Verarbeitung", "Audio-Session", "Verifizierte Quelle",
        "Vertrauen", "Nicht verfügbar", "Keine", "Angefordert", "Fallback",
        "Normalisierung", "Equalizer", "Limiter", "Virtualizer", "Preamp",
        codecId = "Codec-ID", remotePlayback = "Remote-Wiedergabe", receiverManaged = "Vom Empfänger verwaltet"
    )
    "pt" -> TechnicalAudioInfoCopy(
        "Informação técnica de áudio", "Formato real, fonte e percurso do sinal em tempo real",
        "A REPRODUZIR", "FONTE", "SAÍDA & DSP",
        "Codec", "Bitrate", "Frequência", "Canais", "MIME", "Contentor", "Provider",
        "Transporte", "Qualidade", "Profundidade", "Stream", "Loudness", "Saída", "Rota",
        "Volume", "Motor", "Pipeline", "Processamento", "Sessão de áudio", "Fonte verificada",
        "Confiança", "Indisponível", "Nenhum", "Pedido", "Fallback",
        "Normalização", "Equalizador", "Limiter", "Virtualizer", "Preamp",
        codecId = "ID do codec", remotePlayback = "Reprodução remota", receiverManaged = "Gerido pelo recetor"
    )
    "ja" -> TechnicalAudioInfoCopy(
        "技術オーディオ情報", "実際の再生フォーマット、ソース、信号経路をリアルタイム表示",
        "再生中", "ソース", "出力 & DSP",
        "コーデック", "ビットレート", "サンプルレート", "チャンネル", "MIME", "コンテナ", "プロバイダー",
        "配信方式", "品質", "ビット深度", "ストリーム", "ラウドネス", "出力", "ルート",
        "音量", "エンジン", "パイプライン", "処理", "オーディオセッション", "検証済みソース",
        "信頼度", "利用不可", "なし", "要求済み", "フォールバック",
        "ノーマライズ", "イコライザー", "リミッター", "バーチャライザー", "プリアンプ",
        codecId = "コーデック ID", remotePlayback = "リモート再生", receiverManaged = "受信機側で管理"
    )
    "fi" -> TechnicalAudioInfoCopy(
        "Tekniset äänitiedot", "Toiston todellinen muoto, lähde ja signaalipolku",
        "NYT TOISTETAAN", "LÄHDE", "LÄHTÖ & DSP",
        "Codec", "Bitrate", "Näytteenottotaajuus", "Kanavat", "MIME", "Säiliö", "Tarjoaja",
        "Toimitus", "Laatu", "Bittisyvyys", "Stream", "Loudness", "Lähtö", "Reitti",
        "Äänenvoimakkuus", "Moottori", "Putki", "Käsittely", "Ääni-istunto", "Varmennettu lähde",
        "Luotettavuus", "Ei ilmoitettu", "Ei mitään", "Pyydetty", "Varatoiminto",
        "Normalisointi", "Taajuuskorjain", "Rajoitin", "Virtualisoija", "Esivahvistin"
    )
    "et" -> TechnicalAudioInfoCopy(
        "Tehnilised heliandmed", "Reaalajas taasesituse vorming, allikas ja signaalitee",
        "HETKEL MÄNGIB", "ALLIKAS", "VÄLJUND JA DSP",
        "Codec", "Bitikiirus", "Diskreetimissagedus", "Kanalid", "MIME", "Konteiner", "Pakkuja",
        "Edastus", "Kvaliteet", "Bitisügavus", "Voog", "Helitugevus (LUFS)", "Väljund", "Tee",
        "Helitugevus", "Mootor", "Konveier", "Töötlus", "Heliseanss", "Kinnitatud allikas",
        "Usaldusväärsus", "Pole teatatud", "Puudub", "Taotletud", "Varulahendus",
        "Normaliseerimine", "Ekvalaiser", "Piiraja", "Virtualiseerija", "Eelvõimendi"
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
