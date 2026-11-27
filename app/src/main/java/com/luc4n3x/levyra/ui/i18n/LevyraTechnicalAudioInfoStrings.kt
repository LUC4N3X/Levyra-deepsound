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
    val receiverManaged: String = "Managed by receiver",
    val lossless: String = "Lossless",
    val enhancedAudio: String = "LEVYRA ENHANCED AUDIO",
    val status: String = "Status",
    val active: String = "Active",
    val bypassed: String = "Bypassed",
    val off: String = "Off",
    val bypassReason: String = "Bypass reason",
    val yes: String = "Yes",
    val no: String = "No"
)

internal fun LevyraStrings.technicalAudioInfoCopy(): TechnicalAudioInfoCopy = when (translationKey(code, regional = true)) {
    "it" -> TechnicalAudioInfoCopy(
        "Info audio tecniche", "Formato reale, sorgente e percorso del segnale in tempo reale",
        "IN RIPRODUZIONE", "SORGENTE", "USCITA & DSP",
        "Codec", "Bitrate", "Frequenza", "Canali", "MIME", "Contenitore", "Provider",
        "Trasporto", "Qualità", "Profondità", "Stream", "Loudness", "Uscita", "Percorso",
        "Volume", "Motore", "Pipeline", "Elaborazione", "Sessione audio", "Sorgente verificata",
        "Affidabilità", "Non disponibile", "Nessuna", "Richiesto", "Fallback",
        "Normalizzazione", "Equalizzatore", "Limiter", "Virtualizer", "Preamp",
        codecId = "ID codec", remotePlayback = "Riproduzione remota", receiverManaged = "Gestito dal ricevitore",
        lossless = "Lossless"
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
        "Normalisointi", "Taajuuskorjain", "Rajoitin", "Virtualisoija", "Esivahvistin",
        codecId = "Codec-tunnus", remotePlayback = "Etätoisto", receiverManaged = "Vastaanottimen hallinnassa"
    )
    "et" -> TechnicalAudioInfoCopy(
        "Tehnilised heliandmed", "Reaalajas taasesituse vorming, allikas ja signaalitee",
        "HETKEL MÄNGIB", "ALLIKAS", "VÄLJUND JA DSP",
        "Codec", "Bitikiirus", "Diskreetimissagedus", "Kanalid", "MIME", "Konteiner", "Pakkuja",
        "Edastus", "Kvaliteet", "Bitisügavus", "Voog", "Helitugevus (LUFS)", "Väljund", "Tee",
        "Helitugevus", "Mootor", "Konveier", "Töötlus", "Heliseanss", "Kinnitatud allikas",
        "Usaldusväärsus", "Pole teatatud", "Puudub", "Taotletud", "Varulahendus",
        "Normaliseerimine", "Ekvalaiser", "Piiraja", "Virtualiseerija", "Eelvõimendi",
        codecId = "Koodeki ID", remotePlayback = "Kaugesitus", receiverManaged = "Vastuvõtja haldab"
    )
    "ur" -> TechnicalAudioInfoCopy(
        "تکنیکی آڈیو معلومات", "لائیو پلے بیک فارمیٹ، سورس اور سگنل پاتھ",
        "ابھی چل رہا ہے", "ذریعہ", "آؤٹ پٹ اور ڈی ایس پی",
        "کوڈیک", "بٹریٹ", "نمونہ کی شرح", "چینلز", "MIME", "کنٹینر", "فراہم کرنے والا",
        "ڈیلیوری", "معیار", "بٹ گہرائی", "سلسلہ", "اونچ نیچ", "آؤٹ پٹ", "راستہ",
        "آواز کی سطح", "انجن", "پائپ لائن", "پروسیسنگ", "آڈیو سیشن", "تصدیق شدہ ذریعہ",
        "اعتماد", "اطلاع نہیں دی گئی۔", "کوئی نہیں۔", "درخواست کی۔", "فال بیک",
        "نارملائزیشن", "برابر کرنے والا", "حد", "ورچوئلائزر", "پریمپ"
    )
    "ta" -> TechnicalAudioInfoCopy(
        "தொழில்நுட்ப ஆடியோ தகவல்", "நேரடி பின்னணி வடிவம், மூல மற்றும் சமிக்ஞை பாதை",
        "இப்போது இயங்குகிறது", "ஆதாரம்", "வெளியீடு & டிஎஸ்பி",
        "கோடெக்", "பிட்ரேட்", "மாதிரி விகிதம்", "சேனல்கள்", "MIME", "கொள்கலன்", "வழங்குபவர்",
        "டெலிவரி", "தரம்", "பிட் ஆழம்", "ஸ்ட்ரீம்", "சத்தம்", "வெளியீடு", "பாதை",
        "ஒலி அளவு", "இயந்திரம்", "குழாய்", "செயலாக்கம்", "ஆடியோ அமர்வு", "சரிபார்க்கப்பட்ட ஆதாரம்",
        "நம்பிக்கை", "தெரிவிக்கப்படவில்லை", "இல்லை", "கோரப்பட்டது", "வீழ்ச்சி",
        "இயல்பாக்குதல்", "சமநிலைப்படுத்தி", "வரம்பு", "மெய்நிகராக்கி", "பிரேம்ப்"
    )
    "te" -> TechnicalAudioInfoCopy(
        "సాంకేతిక ఆడియో సమాచారం", "ప్రత్యక్ష ప్లేబ్యాక్ ఫార్మాట్, మూలం మరియు సిగ్నల్ మార్గం",
        "ఇప్పుడు ప్లే అవుతోంది", "మూలం", "అవుట్‌పుట్ & DSP",
        "కోడెక్", "బిట్రేట్", "నమూనా రేటు", "ఛానెల్‌లు", "MIME", "కంటైనర్", "ప్రొవైడర్",
        "డెలివరీ", "నాణ్యత", "బిట్ లోతు", "స్ట్రీమ్", "శబ్దము", "అవుట్‌పుట్", "మార్గం",
        "వాల్యూమ్", "ఇంజిన్", "పైప్లైన్", "ప్రాసెసింగ్", "ఆడియో సెషన్", "ధృవీకరించబడిన మూలం",
        "విశ్వాసం", "నివేదించబడలేదు", "ఏదీ లేదు", "అభ్యర్థించారు", "ఫాల్ బ్యాక్",
        "సాధారణీకరణ", "ఈక్వలైజర్", "పరిమితి", "వర్చువలైజర్", "ప్రీయాంప్"
    )
    "mr" -> TechnicalAudioInfoCopy(
        "तांत्रिक ऑडिओ माहिती", "थेट प्लेबॅक स्वरूप, स्त्रोत आणि सिग्नल पथ",
        "आता वाजत आहे", "स्रोत", "आउटपुट आणि डीएसपी",
        "कोडेक", "बिटरेट", "नमुना दर", "चॅनेल", "MIME", "कंटेनर", "प्रदाता",
        "डिलिव्हरी", "गुणवत्ता", "थोडी खोली", "प्रवाह", "जोरात", "आउटपुट", "मार्ग",
        "आवाजाची पातळी", "इंजिन", "पाइपलाइन", "प्रक्रिया करत आहे", "ऑडिओ सत्र", "सत्यापित स्रोत",
        "आत्मविश्वास", "कळवले नाही", "काहीही नाही", "विनंती केली", "फॉलबॅक",
        "सामान्यीकरण", "तुल्यकारक", "लिमिटर", "व्हर्च्युअलायझर", "Preamp"
    )
    "gu" -> TechnicalAudioInfoCopy(
        "તકનીકી ઑડિઓ માહિતી", "લાઇવ પ્લેબેક ફોર્મેટ, સ્ત્રોત અને સિગ્નલ પાથ",
        "હમણાં વાગી રહ્યું છે", "સ્ત્રોત", "આઉટપુટ અને ડીએસપી",
        "કોડેક", "બિટરેટ", "નમૂના દર", "ચેનલો", "MIME", "કન્ટેનર", "પ્રદાતા",
        "ડિલિવરી", "ગુણવત્તા", "બીટ ઊંડાઈ", "પ્રવાહ", "લાઉડનેસ", "આઉટપુટ", "રૂટ",
        "અવાજનું સ્તર", "એન્જીન", "પાઇપલાઇન", "પ્રોસેસિંગ", "ઓડિયો સત્ર", "ચકાસાયેલ સ્ત્રોત",
        "આત્મવિશ્વાસ", "જાણ કરી નથી", "કોઈ નહિ", "વિનંતી કરી", "ફોલબેક",
        "સામાન્યીકરણ", "બરાબરી", "લિમિટર", "વર્ચ્યુઅલાઈઝર", "પ્રીમ્પ"
    )
    "pt-BR" -> TechnicalAudioInfoCopy(
        "Informações técnicas de áudio", "Formato de reprodução ao vivo, fonte e caminho do sinal",
        "AGORA JOGANDO", "FONTE", "SAÍDA E DSP",
        "Codec", "Taxa de bits", "Taxa de amostragem", "Canais", "MIME", "Recipiente", "Provedor",
        "Entrega", "Qualidade", "Profundidade de bits", "Transmitir", "Volume", "Saída", "Rota",
        "Volume", "Motor", "Gasoduto", "Processamento", "Sessão de áudio", "Fonte verificada",
        "Confiança", "Não relatado", "Nenhum", "Solicitado", "Alternativa",
        "Normalização", "Equalizador", "Limitador", "Virtualizador", "Pré-amplificador"
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
}.forBrazil(code) { brazilian() }

private fun TechnicalAudioInfoCopy.brazilian(): TechnicalAudioInfoCopy = copy(
    title = LevyraBrazilianPortuguese.adapt(title),
    subtitle = LevyraBrazilianPortuguese.adapt(subtitle),
    runtime = LevyraBrazilianPortuguese.adapt(runtime),
    source = LevyraBrazilianPortuguese.adapt(source),
    outputAndDsp = LevyraBrazilianPortuguese.adapt(outputAndDsp),
    codec = LevyraBrazilianPortuguese.adapt(codec),
    bitrate = LevyraBrazilianPortuguese.adapt(bitrate),
    sampleRate = LevyraBrazilianPortuguese.adapt(sampleRate),
    channels = LevyraBrazilianPortuguese.adapt(channels),
    mime = LevyraBrazilianPortuguese.adapt(mime),
    container = LevyraBrazilianPortuguese.adapt(container),
    provider = LevyraBrazilianPortuguese.adapt(provider),
    delivery = LevyraBrazilianPortuguese.adapt(delivery),
    quality = LevyraBrazilianPortuguese.adapt(quality),
    bitDepth = LevyraBrazilianPortuguese.adapt(bitDepth),
    streamId = LevyraBrazilianPortuguese.adapt(streamId),
    loudness = LevyraBrazilianPortuguese.adapt(loudness),
    output = LevyraBrazilianPortuguese.adapt(output),
    route = LevyraBrazilianPortuguese.adapt(route),
    volume = LevyraBrazilianPortuguese.adapt(volume),
    engine = LevyraBrazilianPortuguese.adapt(engine),
    path = LevyraBrazilianPortuguese.adapt(path),
    processing = LevyraBrazilianPortuguese.adapt(processing),
    audioSession = LevyraBrazilianPortuguese.adapt(audioSession),
    verifiedSource = LevyraBrazilianPortuguese.adapt(verifiedSource),
    confidence = LevyraBrazilianPortuguese.adapt(confidence),
    unavailable = LevyraBrazilianPortuguese.adapt(unavailable),
    none = LevyraBrazilianPortuguese.adapt(none),
    requested = LevyraBrazilianPortuguese.adapt(requested),
    fallback = LevyraBrazilianPortuguese.adapt(fallback),
    normalization = LevyraBrazilianPortuguese.adapt(normalization),
    equalizer = LevyraBrazilianPortuguese.adapt(equalizer),
    limiter = LevyraBrazilianPortuguese.adapt(limiter),
    virtualizer = LevyraBrazilianPortuguese.adapt(virtualizer),
    preamp = LevyraBrazilianPortuguese.adapt(preamp),
    codecId = LevyraBrazilianPortuguese.adapt(codecId),
    remotePlayback = LevyraBrazilianPortuguese.adapt(remotePlayback),
    receiverManaged = LevyraBrazilianPortuguese.adapt(receiverManaged),
    lossless = LevyraBrazilianPortuguese.adapt(lossless),
    enhancedAudio = LevyraBrazilianPortuguese.adapt(enhancedAudio),
    status = LevyraBrazilianPortuguese.adapt(status),
    active = LevyraBrazilianPortuguese.adapt(active),
    bypassed = LevyraBrazilianPortuguese.adapt(bypassed),
    off = LevyraBrazilianPortuguese.adapt(off),
    bypassReason = LevyraBrazilianPortuguese.adapt(bypassReason),
    yes = LevyraBrazilianPortuguese.adapt(yes),
    no = LevyraBrazilianPortuguese.adapt(no)
)
