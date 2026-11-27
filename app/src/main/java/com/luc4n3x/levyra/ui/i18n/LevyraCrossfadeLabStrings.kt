package com.luc4n3x.levyra.ui.i18n

internal data class CrossfadeLabCopy(
    val title: String,
    val outgoing: String,
    val incoming: String,
    val equalPower: String,
    val off: String,
    val fixedDuration: String,
    val adaptiveDuration: String,
    val gaplessRequired: String,
    val sameReleasePolicy: String
)

internal fun crossfadeLabCopy(code: String): CrossfadeLabCopy = when (translationKey(code, regional = true)) {
    "it" -> CrossfadeLabCopy(
        title = "Curve Lab",
        outgoing = "In uscita",
        incoming = "In entrata",
        equalPower = "Potenza costante",
        off = "Crossfade disattivato",
        fixedDuration = "Durata fissa",
        adaptiveDuration = "AutoMix adatta la durata",
        gaplessRequired = "Attiva la riproduzione gapless per usare il crossfade",
        sameReleasePolicy = "I passaggi consecutivi dello stesso album restano gapless"
    )
    "es" -> CrossfadeLabCopy(
        title = "Laboratorio de curvas",
        outgoing = "Salida",
        incoming = "Entrada",
        equalPower = "Potencia constante",
        off = "Crossfade desactivado",
        fixedDuration = "Duración fija",
        adaptiveDuration = "AutoMix adapta la duración",
        gaplessRequired = "Activa la reproducción sin pausas para usar el crossfade",
        sameReleasePolicy = "Las pistas consecutivas del mismo álbum siguen sin pausas"
    )
    "fr" -> CrossfadeLabCopy(
        title = "Laboratoire de courbes",
        outgoing = "Sortie",
        incoming = "Entrée",
        equalPower = "Puissance constante",
        off = "Fondu enchaîné désactivé",
        fixedDuration = "Durée fixe",
        adaptiveDuration = "AutoMix adapte la durée",
        gaplessRequired = "Activez la lecture sans blanc pour utiliser le fondu enchaîné",
        sameReleasePolicy = "Les pistes consécutives d’un même album restent sans blanc"
    )
    "de" -> CrossfadeLabCopy(
        title = "Kurvenlabor",
        outgoing = "Ausgehend",
        incoming = "Eingehend",
        equalPower = "Konstante Leistung",
        off = "Crossfade deaktiviert",
        fixedDuration = "Feste Dauer",
        adaptiveDuration = "AutoMix passt die Dauer an",
        gaplessRequired = "Aktiviere Gapless-Wiedergabe, um Crossfade zu verwenden",
        sameReleasePolicy = "Aufeinanderfolgende Titel desselben Albums bleiben gapless"
    )
    "pt" -> CrossfadeLabCopy(
        title = "Laboratório de curvas",
        outgoing = "Saída",
        incoming = "Entrada",
        equalPower = "Potência constante",
        off = "Crossfade desativado",
        fixedDuration = "Duração fixa",
        adaptiveDuration = "O AutoMix adapta a duração",
        gaplessRequired = "Ativa a reprodução sem pausas para usar o crossfade",
        sameReleasePolicy = "Faixas consecutivas do mesmo álbum continuam sem pausas"
    )
    "ur" -> CrossfadeLabCopy(
        title = "وکر لیب",
        outgoing = "باہر جانے والا",
        incoming = "آنے والا",
        equalPower = "مساوی طاقت",
        off = "کراس فیڈ آف",
        fixedDuration = "مقررہ مدت",
        adaptiveDuration = "AutoMix مدت کو اپناتا ہے۔",
        gaplessRequired = "کراس فیڈ استعمال کرنے کے لیے گیپلیس پلے بیک کو فعال کریں۔",
        sameReleasePolicy = "ایک ہی البم کے لگاتار ٹریکس خالی رہتے ہیں۔"
    )
    "ta" -> CrossfadeLabCopy(
        title = "வளைவு ஆய்வகம்",
        outgoing = "வெளிச்செல்லும்",
        incoming = "உள்வரும்",
        equalPower = "சம சக்தி",
        off = "கிராஸ்ஃபேட் ஆஃப்",
        fixedDuration = "நிலையான காலம்",
        adaptiveDuration = "ஆட்டோமிக்ஸ் கால அளவை மாற்றியமைக்கிறது",
        gaplessRequired = "கிராஸ்ஃபேடைப் பயன்படுத்த, இடைவெளியற்ற பின்னணியை இயக்கவும்",
        sameReleasePolicy = "ஒரே ஆல்பத்தின் தொடர்ச்சியான டிராக்குகள் இடைவெளியின்றி இருக்கும்"
    )
    "te" -> CrossfadeLabCopy(
        title = "కర్వ్ ల్యాబ్",
        outgoing = "అవుట్గోయింగ్",
        incoming = "ఇన్కమింగ్",
        equalPower = "సమాన శక్తి",
        off = "క్రాస్‌ఫేడ్ ఆఫ్",
        fixedDuration = "స్థిర వ్యవధి",
        adaptiveDuration = "AutoMix వ్యవధిని సర్దుబాటు చేస్తుంది",
        gaplessRequired = "క్రాస్‌ఫేడ్‌ని ఉపయోగించడానికి గ్యాప్‌లెస్ ప్లేబ్యాక్‌ని ప్రారంభించండి",
        sameReleasePolicy = "ఒకే ఆల్బమ్ నుండి వరుస ట్రాక్‌లు ఖాళీ లేకుండా ఉంటాయి"
    )
    "mr" -> CrossfadeLabCopy(
        title = "वक्र प्रयोगशाळा",
        outgoing = "आउटगोइंग",
        incoming = "येणारे",
        equalPower = "समान शक्ती",
        off = "क्रॉसफेड बंद",
        fixedDuration = "निश्चित कालावधी",
        adaptiveDuration = "ऑटोमिक्स कालावधी अनुकूल करते",
        gaplessRequired = "क्रॉसफेड वापरण्यासाठी गॅपलेस प्लेबॅक सक्षम करा",
        sameReleasePolicy = "एकाच अल्बममधील सलग ट्रॅक गॅपलेस राहतात"
    )
    "gu" -> CrossfadeLabCopy(
        title = "કર્વ લેબ",
        outgoing = "આઉટગોઇંગ",
        incoming = "ઇનકમિંગ",
        equalPower = "સમાન શક્તિ",
        off = "ક્રોસફેડ બંધ",
        fixedDuration = "નિશ્ચિત સમયગાળો",
        adaptiveDuration = "ઑટોમિક્સ અવધિને અનુકૂળ કરે છે",
        gaplessRequired = "ક્રોસફેડનો ઉપયોગ કરવા માટે ગેપલેસ પ્લેબેકને સક્ષમ કરો",
        sameReleasePolicy = "એક જ આલ્બમના સળંગ ટ્રેક્સ ખાલી રહે છે"
    )
    "pt-BR" -> CrossfadeLabCopy(
        title = "Laboratório de curvas",
        outgoing = "Saída",
        incoming = "Entrada",
        equalPower = "Poder igual",
        off = "Crossfade desativado",
        fixedDuration = "Duração fixa",
        adaptiveDuration = "AutoMix adapta a duração",
        gaplessRequired = "Ative a reprodução contínua para usar crossfade",
        sameReleasePolicy = "Faixas consecutivas do mesmo álbum permanecem sem intervalos"
    )
    else -> CrossfadeLabCopy(
        title = "Curve lab",
        outgoing = "Outgoing",
        incoming = "Incoming",
        equalPower = "Equal power",
        off = "Crossfade off",
        fixedDuration = "Fixed duration",
        adaptiveDuration = "AutoMix adapts the duration",
        gaplessRequired = "Enable gapless playback to use crossfade",
        sameReleasePolicy = "Consecutive tracks from the same album remain gapless"
    )
}.forBrazil(code) { brazilian() }

private fun CrossfadeLabCopy.brazilian(): CrossfadeLabCopy = copy(
    title = LevyraBrazilianPortuguese.adapt(title),
    outgoing = LevyraBrazilianPortuguese.adapt(outgoing),
    incoming = LevyraBrazilianPortuguese.adapt(incoming),
    equalPower = LevyraBrazilianPortuguese.adapt(equalPower),
    off = LevyraBrazilianPortuguese.adapt(off),
    fixedDuration = LevyraBrazilianPortuguese.adapt(fixedDuration),
    adaptiveDuration = LevyraBrazilianPortuguese.adapt(adaptiveDuration),
    gaplessRequired = LevyraBrazilianPortuguese.adapt(gaplessRequired),
    sameReleasePolicy = LevyraBrazilianPortuguese.adapt(sameReleasePolicy)
)
