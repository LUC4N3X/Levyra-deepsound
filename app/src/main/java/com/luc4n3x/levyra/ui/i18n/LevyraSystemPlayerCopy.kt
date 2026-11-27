package com.luc4n3x.levyra.ui.i18n

data class LevyraSystemPlayerCopy(
    val outputTitle: String,
    val outputSubtitle: String,
    val currentOutput: String,
    val chooseOutput: String,
    val systemManaged: String,
    val speaker: String,
    val bluetooth: String,
    val wired: String,
    val external: String,
    val streamQuality: String,
    val dspActive: String,
    val dspOff: String,
    val sleepChooseDuration: String,
    val sleepEndsAt: String,
    val sleepActive: String,
    val sleepAddFifteen: String,
    val sleepMinuteUnit: String,
    val sleepFadeActive: String,
    val releaseReady: String,
    val releaseFrom: String,
    val releaseTo: String,
    val releaseHighlights: String,
    val releaseProtection: String,
    val releaseProtectionDetail: String,
    val availableOutputs: String,
    val switchingOutput: String,
    val outputChangeFailed: String,
    val sampleRates: String,
    val channels: String,
    val openSystemOutput: String,
    val noOutputs: String
)

fun LevyraStrings.systemPlayerCopy(): LevyraSystemPlayerCopy = when (translationKey(code, regional = true)) {
    "it" -> LevyraSystemPlayerCopy(
        "Uscita audio", "Il percorso audio reale usato da Levyra", "IN RIPRODUZIONE SU", "Scegli uscita",
        "Gestito da Android", "Altoparlante", "Bluetooth", "Cablata", "Esterna", "Qualità stream",
        "DSP attivo", "DSP non attivo", "Scegli quando fermare la musica", "Termina alle", "TIMER ATTIVO",
        "+15 min", "min", "Dissolvenza finale attiva", "Aggiornamento pronto", "Da", "A", "Novità principali",
        "Aggiornamento protetto", "APK ufficiale GitHub · pacchetto e firma verificati prima dell’installazione",
        "USCITE DISPONIBILI", "Cambio uscita…", "Impossibile cambiare uscita", "Frequenze", "Canali",
        "Apri selettore di sistema", "Nessuna uscita disponibile"
    )
    "es" -> LevyraSystemPlayerCopy(
        "Salida de audio", "La ruta de audio real que usa Levyra", "REPRODUCIENDO EN", "Elegir salida",
        "Gestionado por Android", "Altavoz", "Bluetooth", "Cable", "Externa", "Calidad del stream",
        "DSP activo", "DSP inactivo", "Elige cuándo detener la música", "Termina a las", "TEMPORIZADOR ACTIVO",
        "+15 min", "min", "Fundido final activo", "Actualización lista", "De", "A", "Novedades principales",
        "Actualización protegida", "APK oficial de GitHub · paquete y firma verificados antes de instalar",
        "SALIDAS DISPONIBLES", "Cambiando salida…", "No se pudo cambiar la salida", "Frecuencias", "Canales",
        "Abrir selector del sistema", "No hay salidas disponibles"
    )
    "fr" -> LevyraSystemPlayerCopy(
        "Sortie audio", "Le chemin audio réellement utilisé par Levyra", "LECTURE SUR", "Choisir la sortie",
        "Géré par Android", "Haut-parleur", "Bluetooth", "Filaire", "Externe", "Qualité du flux",
        "DSP actif", "DSP inactif", "Choisissez quand arrêter la musique", "Se termine à", "MINUTERIE ACTIVE",
        "+15 min", "min", "Fondu final actif", "Mise à jour prête", "De", "À", "Principales nouveautés",
        "Mise à jour protégée", "APK GitHub officiel · paquet et signature vérifiés avant installation",
        "SORTIES DISPONIBLES", "Changement de sortie…", "Impossible de changer la sortie", "Fréquences", "Canaux",
        "Ouvrir le sélecteur système", "Aucune sortie disponible"
    )
    "de" -> LevyraSystemPlayerCopy(
        "Audioausgabe", "Der tatsächlich von Levyra verwendete Audioweg", "WIEDERGABE ÜBER", "Ausgabe wählen",
        "Von Android verwaltet", "Lautsprecher", "Bluetooth", "Kabel", "Extern", "Stream-Qualität",
        "DSP aktiv", "DSP inaktiv", "Wähle, wann die Musik stoppen soll", "Endet um", "TIMER AKTIV",
        "+15 Min.", "Min.", "Ausblenden aktiv", "Update bereit", "Von", "Auf", "Wichtigste Neuerungen",
        "Geschütztes Update", "Offizielle GitHub-APK · Paket und Signatur werden vor Installation geprüft",
        "VERFÜGBARE AUSGÄNGE", "Ausgabe wird gewechselt…", "Ausgabe konnte nicht gewechselt werden", "Frequenzen", "Kanäle",
        "Systemauswahl öffnen", "Keine Ausgänge verfügbar"
    )
    "pt" -> LevyraSystemPlayerCopy(
        "Saída de áudio", "O caminho de áudio realmente usado pelo Levyra", "A REPRODUZIR EM", "Escolher saída",
        "Gerido pelo Android", "Altifalante", "Bluetooth", "Com fios", "Externa", "Qualidade do stream",
        "DSP ativo", "DSP inativo", "Escolha quando parar a música", "Termina às", "TEMPORIZADOR ATIVO",
        "+15 min", "min", "Fade final ativo", "Atualização pronta", "De", "Para", "Principais novidades",
        "Atualização protegida", "APK oficial do GitHub · pacote e assinatura verificados antes da instalação",
        "SAÍDAS DISPONÍVEIS", "A mudar a saída…", "Não foi possível mudar a saída", "Frequências", "Canais",
        "Abrir seletor do sistema", "Nenhuma saída disponível"
    )
    "ja" -> LevyraSystemPlayerCopy(
        "オーディオ出力", "Levyra が実際に使用している出力先", "再生先", "出力先を選択",
        "Android が管理", "スピーカー", "Bluetooth", "有線", "外部", "ストリーム品質",
        "DSP 有効", "DSP 無効", "音楽を停止するタイミングを選択", "終了時刻", "タイマー作動中",
        "+15分", "分", "終了時フェード有効", "アップデートの準備完了", "現在", "更新後", "主な変更",
        "保護されたアップデート", "公式 GitHub APK · インストール前にパッケージと署名を検証",
        "利用可能な出力先", "出力先を切り替えています…", "出力先を変更できませんでした", "周波数", "チャンネル",
        "システム選択画面を開く", "利用可能な出力先がありません"
    )
    "fi" -> LevyraSystemPlayerCopy(
        "Äänilähtö", "Todellinen äänipolku, jota Levyra käyttää", "TOISTETAAN LAITTEESSA", "Valitse lähtö",
        "Androidin hallitsema", "Kaiutin", "Bluetooth", "Langallinen", "Ulkoinen", "Suoratoiston laatu",
        "DSP aktiivinen", "DSP ei käytössä", "Valitse milloin musiikin tulisi pysähtyä", "Päättyy klo", "AJASTIN AKTIIVINEN",
        "+15 min", "min", "Loppuhäivytys aktiivinen", "Päivitys valmis", "Alkaen", "Päättyen", "Tärkeimmät muutokset",
        "Suojattu päivitys", "Virallinen GitHub APK · paketti ja allekirjoitus varmistettu ennen asennusta",
        "KÄYTETTÄVISSÄ OLEVAT LÄHDÖT", "Vaihdetaan lähtöä…", "Lähtöä ei voitu vaihtaa", "Taajuudet", "Kanavat",
        "Avaa järjestelmän valitsin", "Lähtöjä ei ole käytettävissä"
    )
    "et" -> LevyraSystemPlayerCopy(
        "Heliväljund", "Tegelik helitee, mida Levyra kasutab", "ESITATAKSE SEADMES", "Vali väljund",
        "Androidi hallatav", "Kõlar", "Bluetooth", "Juhtmega", "Väline", "Voo kvaliteet",
        "DSP aktiivne", "DSP passiivne", "Vali, millal muusika peaks peatuma", "Lõpeb kell", "TAIMER AKTIIVNE",
        "+15 min", "min", "Lõpu hajumine aktiivne", "Värskendus valmis", "Alates", "Kuni", "Peamised muudatused",
        "Kaitstud värskendus", "Ametlik GitHubi APK · pakett ja allkirjastuse identiteet kinnitatud enne paigaldamist",
        "SAADAOLEVAD VÄLJUNDID", "Väljundi vahetamine…", "Väljundit ei saanud vahetada", "Sagedused", "Kanalid",
        "Ava süsteemivalik", "Väljundeid pole saadaval"
    )
    "ur" -> LevyraSystemPlayerCopy(
        "آڈیو آؤٹ پٹ", "اصلی آڈیو پاتھ Levyra استعمال کر رہا ہے۔", "چل رہا ہے۔", "آؤٹ پٹ کا انتخاب کریں۔",
        "Android کے زیر انتظام", "سپیکر", "Bluetooth", "وائرڈ", "بیرونی", "سلسلہ کا معیار",
        "ڈی ایس پی ایکٹو", "ڈی ایس پی غیر فعال", "منتخب کریں کہ موسیقی کب بند ہونی چاہیے۔", "پر ختم ہوتا ہے۔", "ٹائمر ایکٹو",
        "+15 منٹ", "منٹ", "ختم دھندلا فعال", "اپ ڈیٹ تیار ہے۔", "سے", "کو", "کلیدی تبدیلیاں",
        "محفوظ اپ ڈیٹ", "آفیشل GitHub APK · پیکیج اور دستخطی شناخت کی تصدیق انسٹال کرنے سے پہلے",
        "دستیاب آؤٹ پٹ", "آؤٹ پٹ سوئچ کر رہا ہے…", "آؤٹ پٹ کو تبدیل نہیں کیا جا سکا", "نمونے کے نرخ", "چینلز",
        "سسٹم آؤٹ پٹ چنندہ کھولیں۔", "کوئی آؤٹ پٹ دستیاب نہیں ہے۔"
    )
    "ta" -> LevyraSystemPlayerCopy(
        "ஆடியோ வெளியீடு", "உண்மையான ஆடியோ பாதை Levyra பயன்படுத்துகிறது", "விளையாடுகிறது", "வெளியீட்டைத் தேர்ந்தெடுக்கவும்",
        "Android ஆல் நிர்வகிக்கப்படுகிறது", "பேச்சாளர்", "Bluetooth", "வயர்டு", "வெளி", "ஸ்ட்ரீம் தரம்",
        "டிஎஸ்பி செயலில்", "டிஎஸ்பி செயலற்றவர்", "இசை எப்போது நிறுத்தப்பட வேண்டும் என்பதைத் தேர்ந்தெடுக்கவும்", "மணிக்கு முடிவடைகிறது", "டைமர் ஆக்டிவ்",
        "+15 நிமிடம்", "நிமிடம்", "முடிவு மங்கல் செயலில்", "புதுப்பிப்பு தயார்", "இருந்து", "செய்ய", "முக்கிய மாற்றங்கள்",
        "பாதுகாக்கப்பட்ட புதுப்பிப்பு", "அதிகாரப்பூர்வ GitHub APK · தொகுப்பு மற்றும் கையொப்பமிடுதல் அடையாளம் நிறுவும் முன் சரிபார்க்கப்பட்டது",
        "கிடைக்கும் வெளியீடுகள்", "வெளியீட்டை மாற்றுகிறது…", "வெளியீட்டை மாற்ற முடியவில்லை", "மாதிரி விகிதங்கள்", "சேனல்கள்",
        "சிஸ்டம் அவுட்புட் பிக்கரைத் திறக்கவும்", "வெளியீடுகள் இல்லை"
    )
    "te" -> LevyraSystemPlayerCopy(
        "ఆడియో అవుట్‌పుట్", "Levyra నిజమైన ఆడియో పాత్‌ని ఉపయోగిస్తున్నారు", "ప్లే అవుతోంది", "అవుట్‌పుట్‌ని ఎంచుకోండి",
        "Android ద్వారా నిర్వహించబడుతుంది", "స్పీకర్", "Bluetooth", "వైర్డు", "బాహ్య", "స్ట్రీమ్ నాణ్యత",
        "డీఎస్పీ యాక్టివ్", "DSP నిష్క్రియ", "సంగీతం ఎప్పుడు ఆగిపోవాలో ఎంచుకోండి", "వద్ద ముగుస్తుంది", "టైమర్ యాక్టివ్",
        "+15 నిమి", "నిమి", "ఎండ్ ఫేడ్ యాక్టివ్", "అప్‌డేట్ సిద్ధంగా ఉంది", "నుండి", "కు", "కీలక మార్పులు",
        "రక్షిత నవీకరణ", "అధికారిక GitHub APK · ఇన్‌స్టాల్ చేయడానికి ముందు ప్యాకేజీ మరియు సంతకం గుర్తింపు ధృవీకరించబడ్డాయి",
        "అందుబాటులో ఉన్న అవుట్‌పుట్‌లు", "అవుట్‌పుట్ మారుతోంది…", "అవుట్‌పుట్‌ని మార్చడం సాధ్యపడలేదు", "నమూనా రేట్లు", "ఛానెల్‌లు",
        "సిస్టమ్ అవుట్‌పుట్ పికర్‌ని తెరవండి", "అవుట్‌పుట్‌లు అందుబాటులో లేవు"
    )
    "mr" -> LevyraSystemPlayerCopy(
        "ऑडिओ आउटपुट", "वास्तविक ऑडिओ पथ Levyra वापरत आहे", "खेळत आहे", "आउटपुट निवडा",
        "Android द्वारे व्यवस्थापित", "वक्ता", "Bluetooth", "वायर्ड", "बाह्य", "प्रवाह गुणवत्ता",
        "डीएसपी सक्रिय", "डीएसपी निष्क्रिय", "संगीत कधी थांबले पाहिजे ते निवडा", "वाजता संपेल", "टाइमर सक्रिय",
        "+१५ मि", "मि", "समाप्त फिकट सक्रिय", "अपडेट तयार आहे", "पासून", "ला", "मुख्य बदल",
        "संरक्षित अद्यतन", "अधिकृत GitHub APK · पॅकेज आणि स्वाक्षरी ओळख स्थापित करण्यापूर्वी सत्यापित",
        "उपलब्ध आउटपुट", "आउटपुट स्विच करत आहे...", "आउटपुट बदलू शकलो नाही", "नमुना दर", "चॅनेल",
        "सिस्टम आउटपुट पिकर उघडा", "कोणतेही आउटपुट उपलब्ध नाहीत"
    )
    "gu" -> LevyraSystemPlayerCopy(
        "ઓડિયો આઉટપુટ", "વાસ્તવિક ઑડિઓ પાથ Levyra નો ઉપયોગ કરી રહ્યો છે", "ચાલી રહ્યું છે", "આઉટપુટ પસંદ કરો",
        "Android દ્વારા સંચાલિત", "વક્તા", "Bluetooth", "વાયર્ડ", "બાહ્ય", "સ્ટ્રીમ ગુણવત્તા",
        "ડીએસપી સક્રિય", "ડીએસપી નિષ્ક્રિય", "સંગીત ક્યારે બંધ થવું જોઈએ તે પસંદ કરો", "વાગ્યે સમાપ્ત થાય છે", "ટાઈમર સક્રિય",
        "+15 મિનિટ", "મિનિટ", "સમાપ્ત ફેડ સક્રિય", "અપડેટ તૈયાર છે", "થી", "થી", "મુખ્ય ફેરફારો",
        "સંરક્ષિત અપડેટ", "અધિકૃત GitHub APK · પેકેજ અને હસ્તાક્ષર ઓળખ સ્થાપિત કરતા પહેલા ચકાસાયેલ છે",
        "ઉપલબ્ધ આઉટપુટ", "આઉટપુટ સ્વિચ કરી રહ્યું છે...", "આઉટપુટ બદલી શકાયું નથી", "નમૂના દરો", "ચેનલો",
        "સિસ્ટમ આઉટપુટ પીકર ખોલો", "કોઈ આઉટપુટ ઉપલબ્ધ નથી"
    )
    "pt-BR" -> LevyraSystemPlayerCopy(
        "Saída de áudio", "O caminho de áudio real que Levyra está usando", "JOGANDO", "Escolha a saída",
        "Gerenciado por Android", "Palestrante", "Bluetooth", "Com fio", "Externo", "Qualidade de transmissão",
        "DSP ativo", "DSP inativo", "Escolha quando a música deve parar", "Termina em", "TEMPORIZADOR ATIVO",
        "+15 minutos", "min", "Fim fade ativo", "Atualização pronta", "De", "Para", "Principais mudanças",
        "Atualização protegida", "APK GitHub oficial · pacote e identidade de assinatura verificada antes da instalação",
        "SAÍDAS DISPONÍVEIS", "Trocando saída…", "Não foi possível alterar a saída", "Taxas de amostragem", "Canais",
        "Seletor de saída do sistema aberto", "Nenhuma saída disponível"
    )
    else -> LevyraSystemPlayerCopy(
        "Audio output", "The real audio path Levyra is using", "PLAYING ON", "Choose output",
        "Managed by Android", "Speaker", "Bluetooth", "Wired", "External", "Stream quality",
        "DSP active", "DSP inactive", "Choose when the music should stop", "Ends at", "TIMER ACTIVE",
        "+15 min", "min", "End fade active", "Update ready", "From", "To", "Key changes",
        "Protected update", "Official GitHub APK · package and signing identity verified before install",
        "AVAILABLE OUTPUTS", "Switching output…", "Could not change output", "Sample rates", "Channels",
        "Open system output picker", "No outputs available"
    )
}.forBrazil(code) { brazilian() }.withSleepTimer(sleepTimerCopy(code))

private fun LevyraSystemPlayerCopy.withSleepTimer(sleep: SleepTimerCopy): LevyraSystemPlayerCopy = copy(
    sleepChooseDuration = sleep.chooseDuration,
    sleepEndsAt = sleep.endsAt,
    sleepActive = sleep.active,
    sleepAddFifteen = sleep.addFifteen,
    sleepMinuteUnit = sleep.minuteUnit,
    sleepFadeActive = sleep.fadeActive
)

private fun LevyraSystemPlayerCopy.brazilian(): LevyraSystemPlayerCopy = copy(
    outputTitle = LevyraBrazilianPortuguese.adapt(outputTitle),
    outputSubtitle = LevyraBrazilianPortuguese.adapt(outputSubtitle),
    currentOutput = LevyraBrazilianPortuguese.adapt(currentOutput),
    chooseOutput = LevyraBrazilianPortuguese.adapt(chooseOutput),
    systemManaged = LevyraBrazilianPortuguese.adapt(systemManaged),
    speaker = LevyraBrazilianPortuguese.adapt(speaker),
    bluetooth = LevyraBrazilianPortuguese.adapt(bluetooth),
    wired = LevyraBrazilianPortuguese.adapt(wired),
    external = LevyraBrazilianPortuguese.adapt(external),
    streamQuality = LevyraBrazilianPortuguese.adapt(streamQuality),
    dspActive = LevyraBrazilianPortuguese.adapt(dspActive),
    dspOff = LevyraBrazilianPortuguese.adapt(dspOff),
    sleepChooseDuration = LevyraBrazilianPortuguese.adapt(sleepChooseDuration),
    sleepEndsAt = LevyraBrazilianPortuguese.adapt(sleepEndsAt),
    sleepActive = LevyraBrazilianPortuguese.adapt(sleepActive),
    sleepAddFifteen = LevyraBrazilianPortuguese.adapt(sleepAddFifteen),
    sleepMinuteUnit = LevyraBrazilianPortuguese.adapt(sleepMinuteUnit),
    sleepFadeActive = LevyraBrazilianPortuguese.adapt(sleepFadeActive),
    releaseReady = LevyraBrazilianPortuguese.adapt(releaseReady),
    releaseFrom = LevyraBrazilianPortuguese.adapt(releaseFrom),
    releaseTo = LevyraBrazilianPortuguese.adapt(releaseTo),
    releaseHighlights = LevyraBrazilianPortuguese.adapt(releaseHighlights),
    releaseProtection = LevyraBrazilianPortuguese.adapt(releaseProtection),
    releaseProtectionDetail = LevyraBrazilianPortuguese.adapt(releaseProtectionDetail),
    availableOutputs = LevyraBrazilianPortuguese.adapt(availableOutputs),
    switchingOutput = LevyraBrazilianPortuguese.adapt(switchingOutput),
    outputChangeFailed = LevyraBrazilianPortuguese.adapt(outputChangeFailed),
    sampleRates = LevyraBrazilianPortuguese.adapt(sampleRates),
    channels = LevyraBrazilianPortuguese.adapt(channels),
    openSystemOutput = LevyraBrazilianPortuguese.adapt(openSystemOutput),
    noOutputs = LevyraBrazilianPortuguese.adapt(noOutputs)
)
