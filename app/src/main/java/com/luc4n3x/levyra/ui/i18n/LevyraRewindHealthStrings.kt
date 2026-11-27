package com.luc4n3x.levyra.ui.i18n

enum class MusicExperienceText {
    RewindSubtitle, Chapters, Coverage, Private, HealthTitle, HealthSubtitle,
    Scan, Issues, Repair, RepairBody, Missing, Empty, Unreadable, Incomplete,
    Permission, Unavailable, ScanHint, ActiveDays
}

internal fun experienceCopy(vararg values: String): Map<MusicExperienceText, String> {
    require(values.size == MusicExperienceText.entries.size)
    return MusicExperienceText.entries.zip(values).toMap()
}

internal val musicExperienceCopies = mapOf(
    "en" to experienceCopy(
        "A year that sounds like you.", "Chapters", "Based on saved listening: %s", "On your device. Just for you.",
        "Download Health Center", "Keep your music ready to go.", "Check my downloads", "Needs attention", "Repair",
        "Download a fresh copy using your download settings. The existing file stays untouched. Check again when the download finishes.",
        "File missing", "Empty file", "Audio cannot be read", "Audio may be incomplete", "Restore folder access", "Could not check this file",
        "Checks saved files and audio tracks locally. This is not a full audio decode or a checksum verification.", "Days with music"
    ),
    "it" to experienceCopy(
        "Un anno che suona come te.", "Capitoli", "Basato sugli ascolti salvati: %s", "Sul tuo dispositivo. Solo per te.",
        "Download Health Center", "La tua musica, pronta a partire.", "Controlla i miei download", "Da sistemare", "Ripara",
        "Scarica una nuova copia con le tue impostazioni di download. Il file esistente resta intatto. Ricontrolla al termine del download.",
        "File mancante", "File vuoto", "Audio non leggibile", "L'audio potrebbe essere incompleto", "Ripristina l'accesso alla cartella", "Impossibile verificare il file",
        "Verifica file salvati e tracce audio sul dispositivo. Non esegue una decodifica completa né un controllo checksum.", "Giorni in musica"
    ),
    "ur" to experienceCopy(
        "ایک سال جو آپ کی طرح لگتا ہے۔", "ابواب", "محفوظ کردہ سننے کی بنیاد پر: %s", "آپ کے آلے پر۔ صرف آپ کے لیے۔",
        "ہیلتھ سنٹر ڈاؤن لوڈ کریں۔", "اپنی موسیقی کو جانے کے لیے تیار رکھیں۔", "میرے ڈاؤن لوڈز کو چیک کریں۔", "توجہ کی ضرورت ہے۔", "مرمت",
        "اپنی ڈاؤن لوڈ کی ترتیبات کا استعمال کرتے ہوئے ایک تازہ کاپی ڈاؤن لوڈ کریں۔ موجودہ فائل اچھوتی رہتی ہے۔ ڈاؤن لوڈ مکمل ہونے پر دوبارہ چیک کریں۔",
        "فائل غائب ہے۔", "خالی فائل", "آڈیو پڑھا نہیں جا سکتا", "آڈیو نامکمل ہو سکتا ہے۔", "فولڈر تک رسائی بحال کریں۔", "اس فائل کو چیک نہیں کیا جا سکا",
        "محفوظ شدہ فائلوں اور آڈیو ٹریکس کو مقامی طور پر چیک کرتا ہے۔ یہ مکمل آڈیو ڈی کوڈ یا چیکسم تصدیق نہیں ہے۔", "موسیقی کے ساتھ دن"
    ),
    "ta" to experienceCopy(
        "உங்களைப் போலவே ஒரு வருடம்.", "அத்தியாயங்கள்", "சேமித்த கேட்பதன் அடிப்படையில்: %s", "உங்கள் சாதனத்தில். உங்களுக்காகத்தான்.",
        "சுகாதார மையத்தைப் பதிவிறக்கவும்", "உங்கள் இசையைத் தயாராக வைத்திருங்கள்.", "எனது பதிவிறக்கங்களைச் சரிபார்க்கவும்", "கவனம் தேவை", "பழுது",
        "உங்கள் பதிவிறக்க அமைப்புகளைப் பயன்படுத்தி புதிய நகலைப் பதிவிறக்கவும். ஏற்கனவே உள்ள கோப்பு தொடப்படாமல் உள்ளது. பதிவிறக்கம் முடிந்ததும் மீண்டும் சரிபார்க்கவும்.",
        "கோப்பு காணவில்லை", "வெற்று கோப்பு", "ஆடியோவைப் படிக்க முடியாது", "ஆடியோ முழுமையடையாமல் இருக்கலாம்", "கோப்புறை அணுகலை மீட்டமை", "இந்தக் கோப்பைச் சரிபார்க்க முடியவில்லை",
        "சேமிக்கப்பட்ட கோப்புகள் மற்றும் ஆடியோ டிராக்குகளை உள்நாட்டில் சரிபார்க்கிறது. இது முழு ஆடியோ டிகோட் அல்லது செக்சம் சரிபார்ப்பு அல்ல.", "இசையுடன் நாட்கள்"
    ),
    "te" to experienceCopy(
        "మీలాగే ఒక సంవత్సరం.", "అధ్యాయాలు", "సేవ్ చేసిన లిజనింగ్ ఆధారంగా: %s", "మీ పరికరంలో. మీ కోసమే.",
        "ఆరోగ్య కేంద్రాన్ని డౌన్‌లోడ్ చేయండి", "మీ సంగీతాన్ని సిద్ధంగా ఉంచుకోండి.", "నా డౌన్‌లోడ్‌లను తనిఖీ చేయండి", "శ్రద్ధ అవసరం", "మరమ్మత్తు",
        "మీ డౌన్‌లోడ్ సెట్టింగ్‌లను ఉపయోగించి తాజా కాపీని డౌన్‌లోడ్ చేయండి. ఇప్పటికే ఉన్న ఫైల్ తాకబడదు. డౌన్‌లోడ్ పూర్తయినప్పుడు మళ్లీ తనిఖీ చేయండి.",
        "ఫైల్ లేదు", "ఖాళీ ఫైల్", "ఆడియో చదవడం సాధ్యం కాదు", "ఆడియో అసంపూర్ణంగా ఉండవచ్చు", "ఫోల్డర్ యాక్సెస్‌ని పునరుద్ధరించండి", "ఈ ఫైల్‌ని తనిఖీ చేయడం సాధ్యపడలేదు",
        "సేవ్ చేసిన ఫైల్‌లు మరియు ఆడియో ట్రాక్‌లను స్థానికంగా తనిఖీ చేస్తుంది. ఇది పూర్తి ఆడియో డీకోడ్ లేదా చెక్‌సమ్ ధృవీకరణ కాదు.", "సంగీతంతో రోజులు"
    ),
    "mr" to experienceCopy(
        "तुझ्यासारखे वाटणारे वर्ष.", "अध्याय", "जतन केलेल्या ऐकण्यावर आधारित: %s", "तुमच्या डिव्हाइसवर. फक्त तुझ्यासाठी.",
        "आरोग्य केंद्र डाउनलोड करा", "तुमचे संगीत जाण्यासाठी तयार ठेवा.", "माझे डाउनलोड तपासा", "लक्ष देण्याची गरज आहे", "दुरुस्ती",
        "तुमची डाउनलोड सेटिंग्ज वापरून एक नवीन प्रत डाउनलोड करा. विद्यमान फाइल अस्पर्शित राहते. डाउनलोड पूर्ण झाल्यावर पुन्हा तपासा.",
        "फाइल गहाळ आहे", "रिकामी फाइल", "ऑडिओ वाचता येत नाही", "ऑडिओ अपूर्ण असू शकतो", "फोल्डर प्रवेश पुनर्संचयित करा", "ही फाइल तपासता आली नाही",
        "जतन केलेल्या फायली आणि ऑडिओ ट्रॅक स्थानिक पातळीवर तपासते. हे पूर्ण ऑडिओ डीकोड किंवा चेकसम सत्यापन नाही.", "संगीतासह दिवस"
    ),
    "gu" to experienceCopy(
        "એક વર્ષ જે તમારા જેવું લાગે છે.", "પ્રકરણો", "સાચવેલા સાંભળવાના આધારે: %s", "તમારા ઉપકરણ પર. ફક્ત તમારા માટે.",
        "આરોગ્ય કેન્દ્ર ડાઉનલોડ કરો", "તમારા સંગીતને જવા માટે તૈયાર રાખો.", "મારા ડાઉનલોડ્સ તપાસો", "ધ્યાનની જરૂર છે", "સમારકામ",
        "તમારી ડાઉનલોડ સેટિંગ્સનો ઉપયોગ કરીને નવી નકલ ડાઉનલોડ કરો. હાલની ફાઇલ અસ્પૃશ્ય રહે છે. ડાઉનલોડ સમાપ્ત થાય ત્યારે ફરીથી તપાસો.",
        "ફાઇલ ખૂટે છે", "ખાલી ફાઇલ", "ઑડિયો વાંચી શકાતો નથી", "ઑડિયો અપૂર્ણ હોઈ શકે છે", "ફોલ્ડર ઍક્સેસ પુનઃસ્થાપિત કરો", "આ ફાઇલ તપાસી શકાઈ નથી",
        "સાચવેલી ફાઇલો અને ઑડિયો ટ્રૅક્સ સ્થાનિક રીતે તપાસે છે. આ સંપૂર્ણ ઓડિયો ડીકોડ અથવા ચેકસમ ચકાસણી નથી.", "સંગીત સાથેના દિવસો"
    ),
    "pt-BR" to experienceCopy(
        "Um ano que parece com você.", "Capítulos", "Com base na escuta salva: %s", "No seu dispositivo. Só para você.",
        "Baixar Centro de Saúde", "Mantenha sua música pronta para uso.", "Verifique meus downloads", "Precisa de atenção", "Reparar",
        "Baixe uma nova cópia usando suas configurações de download. O arquivo existente permanece intacto. Verifique novamente quando o download terminar.",
        "Arquivo ausente", "Arquivo vazio", "O áudio não pode ser lido", "O áudio pode estar incompleto", "Restaurar o acesso à pasta", "Não foi possível verificar este arquivo",
        "Verifica arquivos salvos e faixas de áudio localmente. Esta não é uma decodificação de áudio completa ou uma verificação de soma de verificação.", "Dias com música"
    )
) + translatedMusicExperienceCopies

fun LevyraStrings.musicExperience(key: MusicExperienceText): String =
    localizedValueOrEnglish(musicExperienceCopies, code).getValue(key)
