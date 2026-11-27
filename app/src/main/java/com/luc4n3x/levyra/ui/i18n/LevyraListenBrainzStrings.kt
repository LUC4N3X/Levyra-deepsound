package com.luc4n3x.levyra.ui.i18n

internal data class ListenBrainzCopy(
    val title: String,
    val subtitle: String,
    val refresh: String,
    val import: String,
    val empty: String,
    val topWeek: String,
    val topMonth: String,
    val kindTop: String,
    val kindCreatedFor: String,
    val kindOwn: String,
    val nothingToImport: String
)

internal fun listenBrainzCopy(code: String): ListenBrainzCopy = when (code.substringBefore('-')) {
    "it" -> ListenBrainzCopy(
        title = "Le tue playlist ListenBrainz",
        subtitle = "Generate dai tuoi ascolti. Tocca per importarle in Levyra.",
        refresh = "Aggiorna",
        import = "Importa in Levyra",
        empty = "Ancora nessuna playlist: continua ad ascoltare e ListenBrainz le genererà.",
        topWeek = "I più ascoltati della settimana",
        topMonth = "I più ascoltati del mese",
        kindTop = "Dai tuoi ascolti su ListenBrainz",
        kindCreatedFor = "Creata per te da ListenBrainz",
        kindOwn = "La tua playlist ListenBrainz",
        nothingToImport = "Questa playlist ListenBrainz non contiene brani da importare"
    )
    "ru" -> ListenBrainzCopy(
        title = "Ваши плейлисты ListenBrainz",
        subtitle = "Созданы по вашим прослушиваниям. Нажмите, чтобы импортировать в Levyra.",
        refresh = "Обновить",
        import = "Импортировать в Levyra",
        empty = "Плейлистов пока нет: слушайте дальше, и ListenBrainz их создаст.",
        topWeek = "Самое прослушиваемое за неделю",
        topMonth = "Самое прослушиваемое за месяц",
        kindTop = "По вашим прослушиваниям в ListenBrainz",
        kindCreatedFor = "Создан для вас ListenBrainz",
        kindOwn = "Ваш плейлист ListenBrainz",
        nothingToImport = "В этом плейлисте ListenBrainz нет треков для импорта"
    )
    "ur" -> ListenBrainzCopy(
        title = "آپ کی ListenBrainz پلے لسٹس",
        subtitle = "آپ کے سننے سے پیدا ہوا۔ اسے Levyra میں درآمد کرنے کے لیے ایک کو تھپتھپائیں۔",
        refresh = "ریفریش کریں۔",
        import = "Levyra میں درآمد کریں۔",
        empty = "ابھی تک کوئی پلے لسٹ نہیں: سنتے رہیں اور ListenBrainz انہیں تیار کرے گا۔",
        topWeek = "اس ہفتے سب سے زیادہ کھیلا گیا۔",
        topMonth = "اس مہینے میں سب سے زیادہ کھیلا گیا۔",
        kindTop = "آپ کے ListenBrainz سننے سے",
        kindCreatedFor = "ListenBrainz کے ذریعہ آپ کے لیے بنایا گیا ہے۔",
        kindOwn = "آپ کی ListenBrainz پلے لسٹ",
        nothingToImport = "اس ListenBrainz پلے لسٹ میں درآمد کرنے کے لیے کوئی ٹریک نہیں ہے۔"
    )
    "ta" -> ListenBrainzCopy(
        title = "உங்கள் ListenBrainz பிளேலிஸ்ட்கள்",
        subtitle = "நீங்கள் கேட்டதிலிருந்து உருவாக்கப்பட்டது. Levyra இல் இறக்குமதி செய்ய ஒன்றைத் தட்டவும்.",
        refresh = "புதுப்பிக்கவும்",
        import = "Levyra இல் இறக்குமதி செய்யவும்",
        empty = "இதுவரை பிளேலிஸ்ட்கள் இல்லை: தொடர்ந்து கேட்டுக் கொண்டே இருங்கள், ListenBrainz அவற்றை உருவாக்கும்.",
        topWeek = "இந்த வாரம் அதிகம் விளையாடியது",
        topMonth = "இந்த மாதம் அதிகம் விளையாடியது",
        kindTop = "உங்கள் ListenBrainz கேட்பதிலிருந்து",
        kindCreatedFor = "ListenBrainz உங்களுக்காக உருவாக்கப்பட்டது",
        kindOwn = "உங்கள் ListenBrainz பிளேலிஸ்ட்",
        nothingToImport = "இந்த ListenBrainz பிளேலிஸ்ட்டில் இறக்குமதி செய்ய தடங்கள் இல்லை"
    )
    "te" -> ListenBrainzCopy(
        title = "మీ ListenBrainz ప్లేజాబితాలు",
        subtitle = "మీ వినడం నుండి రూపొందించబడింది. దీన్ని Levyraకి దిగుమతి చేయడానికి ఒకదాన్ని నొక్కండి.",
        refresh = "రిఫ్రెష్ చేయండి",
        import = "Levyraలోకి దిగుమతి చేయండి",
        empty = "ఇంకా ప్లేజాబితాలు లేవు: వింటూ ఉండండి మరియు ListenBrainz వాటిని రూపొందిస్తుంది.",
        topWeek = "ఈ వారం ఎక్కువగా ఆడారు",
        topMonth = "ఈ నెలలో ఎక్కువగా ఆడారు",
        kindTop = "మీ ListenBrainz వినడం నుండి",
        kindCreatedFor = "ListenBrainz ద్వారా మీ కోసం రూపొందించబడింది",
        kindOwn = "మీ ListenBrainz ప్లేజాబితా",
        nothingToImport = "ఈ ListenBrainz ప్లేజాబితాలో దిగుమతి చేయడానికి ట్రాక్‌లు లేవు"
    )
    "mr" -> ListenBrainzCopy(
        title = "तुमच्या ListenBrainz प्लेलिस्ट",
        subtitle = "तुमच्या ऐकण्यावरून निर्माण झाले. Levyra मध्ये आयात करण्यासाठी एक टॅप करा.",
        refresh = "रिफ्रेश करा",
        import = "Levyra मध्ये आयात करा",
        empty = "अद्याप कोणतीही प्लेलिस्ट नाही: ऐकत राहा आणि ListenBrainz त्यांना व्युत्पन्न करेल.",
        topWeek = "या आठवड्यात सर्वाधिक खेळले गेले",
        topMonth = "या महिन्यात सर्वाधिक खेळले गेले",
        kindTop = "तुमच्या ListenBrainz ऐकण्यावरून",
        kindCreatedFor = "ListenBrainz द्वारे तुमच्यासाठी बनवलेले",
        kindOwn = "तुमची ListenBrainz प्लेलिस्ट",
        nothingToImport = "या ListenBrainz प्लेलिस्टमध्ये आयात करण्यासाठी कोणतेही ट्रॅक नाहीत"
    )
    "gu" -> ListenBrainzCopy(
        title = "તમારી ListenBrainz પ્લેલિસ્ટ્સ",
        subtitle = "તમારા શ્રવણમાંથી જનરેટ. તેને Levyra માં આયાત કરવા માટે એકને ટેપ કરો.",
        refresh = "તાજું કરો",
        import = "Levyra માં આયાત કરો",
        empty = "હજુ સુધી કોઈ પ્લેલિસ્ટ નથી: સાંભળતા રહો અને ListenBrainz તેમને જનરેટ કરશે.",
        topWeek = "આ અઠવાડિયે સૌથી વધુ રમ્યા",
        topMonth = "આ મહિને સૌથી વધુ રમ્યા",
        kindTop = "તમારા ListenBrainz સાંભળીને",
        kindCreatedFor = "ListenBrainz દ્વારા તમારા માટે બનાવેલ",
        kindOwn = "તમારી ListenBrainz પ્લેલિસ્ટ",
        nothingToImport = "આ ListenBrainz પ્લેલિસ્ટ આયાત કરવા માટે કોઈ ટ્રૅક્સ નથી"
    )
    else -> ListenBrainzCopy(
        title = "Your ListenBrainz playlists",
        subtitle = "Generated from your listening. Tap one to import it into Levyra.",
        refresh = "Refresh",
        import = "Import into Levyra",
        empty = "No playlists yet: keep listening and ListenBrainz will generate them.",
        topWeek = "Most played this week",
        topMonth = "Most played this month",
        kindTop = "From your ListenBrainz listening",
        kindCreatedFor = "Made for you by ListenBrainz",
        kindOwn = "Your ListenBrainz playlist",
        nothingToImport = "This ListenBrainz playlist has no tracks to import"
    )
}
