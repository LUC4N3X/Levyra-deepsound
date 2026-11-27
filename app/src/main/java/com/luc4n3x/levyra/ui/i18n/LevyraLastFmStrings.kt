package com.luc4n3x.levyra.ui.i18n

internal data class LastFmCopy(
    val title: String,
    val subtitle: String,
    val refresh: String,
    val import: String,
    val empty: String,
    val loved: String,
    val topWeek: String,
    val topMonth: String,
    val topAllTime: String,
    val kindSite: String,
    val kindLoved: String,
    val kindTop: String,
    val nothingToImport: String
)

internal fun lastFmCopy(code: String): LastFmCopy = when (code.substringBefore('-')) {
    "it" -> LastFmCopy(
        title = "Le tue playlist Last.fm",
        subtitle = "Le playlist del tuo profilo Last.fm e i tuoi ascolti. Tocca per importarle in Levyra.",
        refresh = "Aggiorna",
        import = "Importa in Levyra",
        empty = "Ancora niente qui: ascolta o aggiungi brani ai preferiti su Last.fm.",
        loved = "Brani amati",
        topWeek = "I più ascoltati della settimana",
        topMonth = "I più ascoltati del mese",
        topAllTime = "I più ascoltati di sempre",
        kindSite = "La tua playlist su Last.fm",
        kindLoved = "I tuoi preferiti su Last.fm",
        kindTop = "Dai tuoi scrobble su Last.fm",
        nothingToImport = "Questa playlist Last.fm non contiene brani da importare"
    )
    "ru" -> LastFmCopy(
        title = "Ваши плейлисты Last.fm",
        subtitle = "Из вашего профиля Last.fm. Нажмите, чтобы импортировать в Levyra.",
        refresh = "Обновить",
        import = "Импортировать в Levyra",
        empty = "Пока пусто: слушайте музыку или отмечайте любимые треки на Last.fm.",
        loved = "Любимые треки",
        topWeek = "Самое прослушиваемое за неделю",
        topMonth = "Самое прослушиваемое за месяц",
        topAllTime = "Самое прослушиваемое за всё время",
        kindSite = "Ваш плейлист на Last.fm",
        kindLoved = "Ваши любимые треки на Last.fm",
        kindTop = "По вашим скробблам на Last.fm",
        nothingToImport = "В этом плейлисте Last.fm нет треков для импорта"
    )
    "ur" -> LastFmCopy(
        title = "آپ کی Last.fm پلے لسٹس",
        subtitle = "آپ کے Last.fm پروفائل سے۔ اسے Levyra میں درآمد کرنے کے لیے ایک کو تھپتھپائیں۔",
        refresh = "ریفریش کریں۔",
        import = "Levyra میں درآمد کریں۔",
        empty = "ابھی تک یہاں کچھ نہیں: Last.fm پر ٹریکس سنیں یا پسند کریں۔",
        loved = "ٹریکس کو پسند کیا۔",
        topWeek = "اس ہفتے سب سے زیادہ کھیلا گیا۔",
        topMonth = "اس مہینے میں سب سے زیادہ کھیلا گیا۔",
        topAllTime = "اب تک کا سب سے زیادہ کھیلا گیا۔",
        kindSite = "آپ کی Last.fm پلے لسٹ",
        kindLoved = "Last.fm پر آپ کے پسندیدہ ٹریکس",
        kindTop = "آپ کے Last.fm سکروبلز سے",
        nothingToImport = "اس Last.fm پلے لسٹ میں درآمد کرنے کے لیے کوئی ٹریک نہیں ہے۔"
    )
    "ta" -> LastFmCopy(
        title = "உங்கள் Last.fm பிளேலிஸ்ட்கள்",
        subtitle = "உங்கள் Last.fm சுயவிவரத்திலிருந்து. Levyra இல் இறக்குமதி செய்ய ஒன்றைத் தட்டவும்.",
        refresh = "புதுப்பிக்கவும்",
        import = "Levyra இல் இறக்குமதி செய்யவும்",
        empty = "இதுவரை இங்கு எதுவும் இல்லை: Last.fm இல் பாடல்களைக் கேளுங்கள் அல்லது விரும்புங்கள்.",
        loved = "பிடித்த பாடல்கள்",
        topWeek = "இந்த வாரம் அதிகம் விளையாடியது",
        topMonth = "இந்த மாதம் அதிகம் விளையாடியது",
        topAllTime = "எல்லா நேரத்திலும் அதிகம் விளையாடியது",
        kindSite = "உங்கள் Last.fm பிளேலிஸ்ட்",
        kindLoved = "Last.fm இல் உங்களுக்குப் பிடித்த டிராக்குகள்",
        kindTop = "உங்கள் Last.fm ஸ்க்ரோபிள்களிலிருந்து",
        nothingToImport = "இந்த Last.fm பிளேலிஸ்ட்டில் இறக்குமதி செய்ய தடங்கள் இல்லை"
    )
    "te" -> LastFmCopy(
        title = "మీ Last.fm ప్లేజాబితాలు",
        subtitle = "మీ Last.fm ప్రొఫైల్ నుండి. దీన్ని Levyraకి దిగుమతి చేయడానికి ఒకదాన్ని నొక్కండి.",
        refresh = "రిఫ్రెష్ చేయండి",
        import = "Levyraలోకి దిగుమతి చేయండి",
        empty = "ఇంకా ఇక్కడ ఏదీ లేదు: Last.fmలో ట్రాక్‌లను వినండి లేదా ప్రేమించండి.",
        loved = "ట్రాక్‌లు నచ్చాయి",
        topWeek = "ఈ వారం ఎక్కువగా ఆడారు",
        topMonth = "ఈ నెలలో ఎక్కువగా ఆడారు",
        topAllTime = "అన్ని సమయాలలో ఎక్కువగా ఆడారు",
        kindSite = "మీ Last.fm ప్లేజాబితా",
        kindLoved = "Last.fmలో మీకు ఇష్టమైన ట్రాక్‌లు",
        kindTop = "మీ Last.fm స్క్రోబుల్స్ నుండి",
        nothingToImport = "ఈ Last.fm ప్లేజాబితాలో దిగుమతి చేయడానికి ట్రాక్‌లు లేవు"
    )
    "mr" -> LastFmCopy(
        title = "तुमच्या Last.fm प्लेलिस्ट",
        subtitle = "तुमच्या Last.fm प्रोफाइलवरून. Levyra मध्ये आयात करण्यासाठी एकावर टॅप करा.",
        refresh = "रिफ्रेश करा",
        import = "Levyra मध्ये आयात करा",
        empty = "येथे अद्याप काहीही नाही: Last.fm वरील ट्रॅक ऐका किंवा प्रेम करा.",
        loved = "ट्रॅक आवडले",
        topWeek = "या आठवड्यात सर्वाधिक खेळले गेले",
        topMonth = "या महिन्यात सर्वाधिक खेळले गेले",
        topAllTime = "आतापर्यंतचे सर्वाधिक खेळले गेले",
        kindSite = "तुमची Last.fm प्लेलिस्ट",
        kindLoved = "Last.fm वर तुमचे आवडते ट्रॅक",
        kindTop = "तुमच्या Last.fm स्क्रॉबल्समधून",
        nothingToImport = "या Last.fm प्लेलिस्टमध्ये आयात करण्यासाठी कोणतेही ट्रॅक नाहीत"
    )
    "gu" -> LastFmCopy(
        title = "તમારી Last.fm પ્લેલિસ્ટ્સ",
        subtitle = "તમારી Last.fm પ્રોફાઇલમાંથી. તેને Levyra માં આયાત કરવા માટે એકને ટેપ કરો.",
        refresh = "તાજું કરો",
        import = "Levyra માં આયાત કરો",
        empty = "હજી સુધી અહીં કંઈ નથી: Last.fm પર ગીતો સાંભળો અથવા પ્રેમ કરો.",
        loved = "ગમ્યું ટ્રેક",
        topWeek = "આ અઠવાડિયે સૌથી વધુ રમ્યા",
        topMonth = "આ મહિને સૌથી વધુ રમ્યા",
        topAllTime = "અત્યાર સુધીના સૌથી વધુ રમ્યા",
        kindSite = "તમારી Last.fm પ્લેલિસ્ટ",
        kindLoved = "Last.fm પર તમારા પ્રિય ટ્રેક",
        kindTop = "તમારા Last.fm સ્ક્રૉબલ્સમાંથી",
        nothingToImport = "આ Last.fm પ્લેલિસ્ટમાં આયાત કરવા માટે કોઈ ટ્રૅક નથી"
    )
    else -> LastFmCopy(
        title = "Your Last.fm playlists",
        subtitle = "From your Last.fm profile. Tap one to import it into Levyra.",
        refresh = "Refresh",
        import = "Import into Levyra",
        empty = "Nothing here yet: listen or love tracks on Last.fm.",
        loved = "Loved tracks",
        topWeek = "Most played this week",
        topMonth = "Most played this month",
        topAllTime = "Most played of all time",
        kindSite = "Your Last.fm playlist",
        kindLoved = "Your loved tracks on Last.fm",
        kindTop = "From your Last.fm scrobbles",
        nothingToImport = "This Last.fm playlist has no tracks to import"
    )
}
