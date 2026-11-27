package com.luc4n3x.levyra.ui.i18n

internal data class YoutubeMusicCopy(
    val connect: String, val introduction: String, val library: String, val accounts: String,
    val addAccount: String, val logout: String, val logoutAll: String, val selected: String,
    val playlists: String, val likes: String, val albums: String, val artists: String,
    val refresh: String, val more: String, val import: String, val importDescription: String,
    val imported: String, val empty: String, val networkError: String, val signInError: String,
    val protocolError: String, val storageError: String, val limitError: String,
    val retry: String, val back: String, val login: String, val checking: String, val play: String,
    val importSong: String, val disconnectTitle: String, val disconnectMessage: String,
    val cancel: String, val optional: String, val reconnect: String,
    val upToDate: String, val playlistOnly: String
)

internal fun youtubeMusicCopy(code: String): YoutubeMusicCopy = when (code.substringBefore('-')) {
    "it" -> YoutubeMusicCopy(
        "Collega YouTube Music", "Ritrova le tue playlist e la musica che hai salvato.", "Libreria YouTube Music", "Account",
        "Aggiungi account", "Scollega", "Scollega tutti", "In uso",
        "Playlist", "Mi piace", "Album", "Artisti", "Aggiorna", "Carica altri", "Importa e aggiorna",
        "Aggiunge i brani mancanti in Levyra senza rimuovere la tua musica. Gli aggiornamenti si avviano da qui.",
        "Musica importata in Levyra", "Non ci sono contenuti da mostrare", "Connessione non riuscita. Riprova tra poco.",
        "Accedi di nuovo per collegare questo account.", "YouTube Music ha restituito dati non leggibili. Riprova.",
        "Non è stato possibile salvare o leggere la sessione.", "La libreria supera il limite di 10.000 elementi. Importa una playlist alla volta.",
        "Riprova", "Indietro", "Accedi a YouTube Music", "Verifica dell’account…", "Riproduci", "Importa brano",
        "Scollegare l’account?", "La sessione verrà rimossa da Levyra. La musica già importata resterà nella tua libreria.",
        "Annulla", "Collegamento facoltativo", "Accedi di nuovo",
        "La playlist è già aggiornata", "Playlist salvata. Non è stato possibile aggiornare i preferiti: riprova l’importazione."
    )
    "ru" -> YoutubeMusicCopy(
        "Подключить YouTube Music", "Ваши плейлисты и сохранённая музыка.", "Библиотека YouTube Music", "Аккаунты",
        "Добавить аккаунт", "Отключить", "Отключить все", "Выбран",
        "Плейлисты", "Понравившиеся", "Альбомы", "Исполнители", "Обновить", "Загрузить ещё", "Импорт и обновление",
        "Добавляет недостающие треки в Levyra, не удаляя вашу музыку. Обновление запускается здесь.",
        "Музыка импортирована в Levyra", "Здесь пока ничего нет", "Не удалось подключиться. Попробуйте позже.",
        "Войдите снова, чтобы подключить аккаунт.", "Не удалось прочитать ответ YouTube Music. Попробуйте снова.",
        "Не удалось сохранить или прочитать сессию.", "Библиотека превышает лимит в 10 000 элементов. Импортируйте плейлисты отдельно.",
        "Повторить", "Назад", "Вход в YouTube Music", "Проверка аккаунта…", "Воспроизвести", "Импорт трека",
        "Отключить аккаунт?", "Сессия будет удалена из Levyra. Импортированная музыка останется в библиотеке.",
        "Отмена", "Подключение необязательно", "Войти снова",
        "Плейлист уже обновлён", "Плейлист сохранён. Не удалось обновить избранное: повторите импорт."
    )
    "ur" -> YoutubeMusicCopy(
        "YouTube Music جڑیں۔", "اپنی پلے لسٹس اور اپنی محفوظ کردہ موسیقی تلاش کریں۔", "YouTube Music لائبریری", "اکاؤنٹس",
        "اکاؤنٹ شامل کریں۔", "منقطع کرنا", "سب کو منقطع کریں۔", "منتخب",
        "پلے لسٹس", "پسندیدہ گانے", "البمز", "فنکار", "ریفریش کریں۔", "مزید لوڈ کریں۔", "درآمد کریں اور اپ ڈیٹ کریں۔",
        "آپ کی موسیقی کو ہٹائے بغیر غائب گانوں کو Levyra میں شامل کرتا ہے۔ یہاں اپ ڈیٹس شروع کریں۔",
        "موسیقی Levyra میں درآمد کی گئی۔", "ابھی تک یہاں کچھ نہیں ہے۔", "رابطہ نہیں ہو سکا۔ تھوڑی دیر میں دوبارہ کوشش کریں۔",
        "اس اکاؤنٹ سے منسلک ہونے کے لیے دوبارہ سائن ان کریں۔", "YouTube Music کا جواب نہیں پڑھ سکا۔ دوبارہ کوشش کریں۔",
        "آپ کا سیشن محفوظ یا پڑھا نہیں جا سکا۔", "لائبریری 10,000 آئٹم کی حد سے زیادہ ہے۔ پلے لسٹس کو الگ سے درآمد کریں۔",
        "دوبارہ کوشش کریں۔", "واپس", "YouTube Music میں سائن ان کریں۔", "آپ کا اکاؤنٹ چیک کیا جا رہا ہے…", "چلائیں", "گانا درآمد کریں۔",
        "اکاؤنٹ منقطع کریں؟", "آپ کا سیشن Levyra سے ہٹا دیا جائے گا۔ درآمد شدہ موسیقی آپ کی لائبریری میں رہے گی۔",
        "منسوخ کریں۔", "اختیاری کنکشن", "دوبارہ سائن ان کریں۔",
        "پلے لسٹ پہلے سے ہی اپ ٹو ڈیٹ ہے۔", "پلے لسٹ محفوظ ہو گئی۔ پسندیدہ کو اپ ڈیٹ نہیں کیا جا سکا: دوبارہ درآمد کرنے کی کوشش کریں۔"
    )
    "ta" -> YoutubeMusicCopy(
        "YouTube Music ஐ இணைக்கவும்", "உங்கள் பிளேலிஸ்ட்களையும் நீங்கள் சேமித்த இசையையும் கண்டறியவும்.", "YouTube Music நூலகம்", "கணக்குகள்",
        "கணக்கைச் சேர்க்கவும்", "துண்டிக்கவும்", "அனைத்தையும் துண்டிக்கவும்", "தேர்ந்தெடுக்கப்பட்டது",
        "பிளேலிஸ்ட்கள்", "பிடித்த பாடல்கள்", "ஆல்பங்கள்", "கலைஞர்கள்", "புதுப்பிக்கவும்", "மேலும் ஏற்றவும்", "இறக்குமதி செய்து புதுப்பிக்கவும்",
        "உங்கள் இசையை அகற்றாமலேயே விடுபட்ட பாடல்களை Levyra இல் சேர்க்கிறது. புதுப்பிப்புகளை இங்கே தொடங்கவும்.",
        "Levyra இல் இசை இறக்குமதி செய்யப்பட்டது", "இன்னும் இங்கே எதுவும் இல்லை", "இணைக்க முடியவில்லை. சிறிது நேரத்தில் மீண்டும் முயற்சிக்கவும்.",
        "இந்தக் கணக்கை இணைக்க மீண்டும் உள்நுழையவும்.", "YouTube Music இன் பதிலைப் படிக்க முடியவில்லை. மீண்டும் முயற்சிக்கவும்.",
        "உங்கள் அமர்வைச் சேமிக்கவோ படிக்கவோ முடியவில்லை.", "நூலகம் 10,000 உருப்படிகளின் வரம்பை மீறுகிறது. பிளேலிஸ்ட்களை தனித்தனியாக இறக்குமதி செய்யவும்.",
        "மீண்டும் முயற்சிக்கவும்", "பின்செல்", "YouTube Music இல் உள்நுழையவும்", "உங்கள் கணக்கைச் சரிபார்க்கிறது…", "இயக்கவும்", "பாடல் இறக்குமதி",
        "கணக்கை துண்டிக்கவா?", "உங்கள் அமர்வு Levyra இலிருந்து அகற்றப்படும். இறக்குமதி செய்யப்பட்ட இசை உங்கள் நூலகத்தில் இருக்கும்.",
        "ரத்து செய்", "விருப்ப இணைப்பு", "மீண்டும் உள்நுழையவும்",
        "பிளேலிஸ்ட் ஏற்கனவே புதுப்பித்த நிலையில் உள்ளது", "பிளேலிஸ்ட் சேமிக்கப்பட்டது. பிடித்தவற்றை புதுப்பிக்க முடியவில்லை: இறக்குமதியை மீண்டும் முயற்சிக்கவும்."
    )
    "te" -> YoutubeMusicCopy(
        "YouTube Musicని కనెక్ట్ చేయండి", "మీ ప్లేజాబితాలు మరియు మీరు సేవ్ చేసిన సంగీతాన్ని కనుగొనండి.", "YouTube Music లైబ్రరీ", "ఖాతాలు",
        "ఖాతాను జోడించండి", "డిస్‌కనెక్ట్ చేయండి", "అన్నింటినీ డిస్‌కనెక్ట్ చేయండి", "ఎంపిక చేయబడింది",
        "ప్లేజాబితాలు", "నచ్చిన పాటలు", "ఆల్బమ్‌లు", "కళాకారులు", "రిఫ్రెష్ చేయండి", "మరింత లోడ్ చేయండి", "దిగుమతి చేయండి మరియు నవీకరించండి",
        "మీ సంగీతాన్ని తీసివేయకుండానే తప్పిపోయిన పాటలను Levyraకి జోడిస్తుంది. నవీకరణలను ఇక్కడ ప్రారంభించండి.",
        "సంగీతం Levyraకి దిగుమతి చేయబడింది", "ఇక్కడ ఇంకా ఏమీ లేదు", "కనెక్ట్ చేయడం సాధ్యపడలేదు. కాసేపట్లో మళ్లీ ప్రయత్నించండి.",
        "ఈ ఖాతాను కనెక్ట్ చేయడానికి మళ్లీ సైన్ ఇన్ చేయండి.", "YouTube Music ప్రతిస్పందనను చదవడం సాధ్యపడలేదు. మళ్లీ ప్రయత్నించండి.",
        "మీ సెషన్‌ను సేవ్ చేయడం లేదా చదవడం సాధ్యం కాలేదు.", "లైబ్రరీ 10,000 అంశాల పరిమితిని మించిపోయింది. ప్లేజాబితాలను విడిగా దిగుమతి చేయండి.",
        "మళ్లీ ప్రయత్నించండి", "వెనక్కి", "YouTube Musicకి సైన్ ఇన్ చేయండి", "మీ ఖాతాను తనిఖీ చేస్తోంది...", "ప్లే చేయండి", "పాటను దిగుమతి చేయండి",
        "ఖాతాను డిస్‌కనెక్ట్ చేయాలా?", "మీ సెషన్ Levyra నుండి తీసివేయబడుతుంది. దిగుమతి చేసుకున్న సంగీతం మీ లైబ్రరీలో ఉంటుంది.",
        "రద్దు చేయి", "ఐచ్ఛిక కనెక్షన్", "మళ్లీ సైన్ ఇన్ చేయండి",
        "ప్లేజాబితా ఇప్పటికే తాజాగా ఉంది", "ప్లేజాబితా సేవ్ చేయబడింది. ఇష్టమైనవి నవీకరించబడలేదు: దిగుమతిని మళ్లీ ప్రయత్నించండి."
    )
    "mr" -> YoutubeMusicCopy(
        "YouTube Music कनेक्ट करा", "तुमच्या प्लेलिस्ट आणि तुम्ही सेव्ह केलेले संगीत शोधा.", "YouTube Music लायब्ररी", "खाती",
        "खाते जोडा", "डिस्कनेक्ट करा", "सर्व डिस्कनेक्ट करा", "निवडले",
        "प्लेलिस्ट", "आवडलेली गाणी", "अल्बम", "कलाकार", "रिफ्रेश करा", "अधिक लोड करा", "आयात आणि अद्यतन",
        "तुमचे संगीत न काढता Levyra मध्ये गहाळ गाणी जोडते. येथे अद्यतने सुरू करा.",
        "संगीत Levyra मध्ये आयात केले", "येथे अद्याप काहीही नाही", "कनेक्ट करता आले नाही. थोड्या वेळाने पुन्हा प्रयत्न करा.",
        "हे खाते कनेक्ट करण्यासाठी पुन्हा साइन इन करा.", "YouTube Music चा प्रतिसाद वाचता आला नाही. पुन्हा प्रयत्न करा.",
        "तुमचे सत्र वाचू किंवा वाचू शकलो नाही.", "लायब्ररीने 10,000 आयटम मर्यादा ओलांडली आहे. प्लेलिस्ट स्वतंत्रपणे आयात करा.",
        "पुन्हा प्रयत्न करा", "मागे", "YouTube Music मध्ये साइन इन करा", "तुमचे खाते तपासत आहे...", "वाजवा", "गाणे आयात करा",
        "खाते डिस्कनेक्ट करायचे?", "तुमचे सत्र Levyra वरून काढले जाईल. आयात केलेले संगीत तुमच्या लायब्ररीमध्ये राहील.",
        "रद्द करा", "पर्यायी कनेक्शन", "पुन्हा साइन इन करा",
        "प्लेलिस्ट आधीच अद्ययावत आहे", "प्लेलिस्ट सेव्ह केली. आवडते अपडेट करता आले नाहीत: आयात करण्याचा पुन्हा प्रयत्न करा."
    )
    "gu" -> YoutubeMusicCopy(
        "YouTube Music કનેક્ટ કરો", "તમારી પ્લેલિસ્ટ અને તમે સાચવેલ સંગીત શોધો.", "YouTube Music પુસ્તકાલય", "એકાઉન્ટ્સ",
        "એકાઉન્ટ ઉમેરો", "ડિસ્કનેક્ટ કરો", "બધાને ડિસ્કનેક્ટ કરો", "પસંદ કરેલ",
        "પ્લેલિસ્ટ્સ", "ગમતાં ગીતો", "આલ્બમ્સ", "કલાકારો", "તાજું કરો", "વધુ લોડ કરો", "આયાત કરો અને અપડેટ કરો",
        "તમારા સંગીતને દૂર કર્યા વિના Levyra માં ખૂટતા ગીતો ઉમેરે છે. અહીં અપડેટ્સ શરૂ કરો.",
        "સંગીત Levyra માં આયાત કરેલું", "હજુ સુધી અહીં કંઈ નથી", "કનેક્ટ કરી શકાયું નથી. ટૂંક સમયમાં ફરી પ્રયાસ કરો.",
        "આ એકાઉન્ટને કનેક્ટ કરવા માટે ફરીથી સાઇન ઇન કરો.", "YouTube Music નો પ્રતિભાવ વાંચી શકાયો નથી. ફરી પ્રયાસ કરો.",
        "તમારું સત્ર સાચવી કે વાંચી શકાયું નથી.", "લાઇબ્રેરી 10,000 આઇટમની મર્યાદાને ઓળંગે છે. પ્લેલિસ્ટ અલગથી આયાત કરો.",
        "ફરી પ્રયાસ કરો", "પાછા", "YouTube Music માં સાઇન ઇન કરો", "તમારું એકાઉન્ટ તપાસી રહ્યું છે...", "વગાડો", "ગીત આયાત કરો",
        "એકાઉન્ટ ડિસ્કનેક્ટ કરીએ?", "તમારું સત્ર Levyra માંથી દૂર કરવામાં આવશે. આયાત કરેલ સંગીત તમારી લાઇબ્રેરીમાં રહેશે.",
        "રદ કરો", "વૈકલ્પિક જોડાણ", "ફરીથી સાઇન ઇન કરો",
        "પ્લેલિસ્ટ પહેલેથી જ અદ્યતન છે", "પ્લેલિસ્ટ સાચવ્યું. મનપસંદ અપડેટ કરી શકાયા નથી: આયાત કરવાનો ફરી પ્રયાસ કરો."
    )
    else -> YoutubeMusicCopy(
        "Connect YouTube Music", "Find your playlists and the music you saved.", "YouTube Music library", "Accounts",
        "Add account", "Disconnect", "Disconnect all", "Selected",
        "Playlists", "Liked songs", "Albums", "Artists", "Refresh", "Load more", "Import and update",
        "Adds missing songs to Levyra without removing your music. Start updates here.",
        "Music imported into Levyra", "There is nothing here yet", "Could not connect. Try again shortly.",
        "Sign in again to connect this account.", "Could not read YouTube Music’s response. Try again.",
        "Could not save or read your session.", "The library exceeds the 10,000 item limit. Import playlists separately.",
        "Retry", "Back", "Sign in to YouTube Music", "Checking your account…", "Play", "Import song",
        "Disconnect account?", "Your session will be removed from Levyra. Imported music will stay in your library.",
        "Cancel", "Optional connection", "Sign in again",
        "The playlist is already up to date", "Playlist saved. Favorites could not be updated: retry the import."
    )
}
