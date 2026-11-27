package com.luc4n3x.levyra.ui.i18n

internal data class SupportDonationCopy(
    val title: String,
    val subtitle: String,
    val body: String,
    val recurring: String,
    val oneTime: String
)

private val supportDonationCopies = mapOf(
    "en" to SupportDonationCopy(
        "Support Levyra",
        "Free, no ads, no tracking. A donation keeps it that way.",
        "Levyra is built by one developer, without ads or tracking. If it makes your music better, a small donation helps keep it going.",
        "Monthly or one-time",
        "One-time donation"
    ),
    "it" to SupportDonationCopy(
        "Sostieni Levyra",
        "Gratis, senza pubblicità né tracciamento. Una donazione la mantiene così.",
        "Levyra è sviluppata da una sola persona, senza pubblicità né tracciamento. Se rende migliore la tua musica, una piccola donazione aiuta a farla crescere.",
        "Mensile o una tantum",
        "Donazione singola"
    ),
    "es" to SupportDonationCopy(
        "Apoya Levyra",
        "Gratis, sin anuncios ni rastreo. Una donación la mantiene así.",
        "Levyra la desarrolla una sola persona, sin anuncios ni rastreo. Si mejora tu música, una pequeña donación ayuda a que siga adelante.",
        "Mensual o única",
        "Donación única"
    ),
    "fr" to SupportDonationCopy(
        "Soutenir Levyra",
        "Gratuit, sans publicité ni pistage. Un don permet de le garder ainsi.",
        "Levyra est développé par une seule personne, sans publicité ni pistage. S’il améliore votre musique, un petit don l’aide à continuer.",
        "Mensuel ou ponctuel",
        "Don ponctuel"
    ),
    "de" to SupportDonationCopy(
        "Levyra unterstützen",
        "Kostenlos, ohne Werbung und Tracking. Eine Spende hält es so.",
        "Levyra wird von einer einzigen Person entwickelt, ohne Werbung und Tracking. Wenn es deine Musik besser macht, hilft eine kleine Spende, dass es weitergeht.",
        "Monatlich oder einmalig",
        "Einmalige Spende"
    ),
    "pt" to SupportDonationCopy(
        "Apoiar o Levyra",
        "Gratuito, sem anúncios nem rastreio. Um donativo mantém-no assim.",
        "O Levyra é feito por um único programador, sem anúncios nem rastreio. Se torna a sua música melhor, um pequeno donativo ajuda a mantê-lo vivo.",
        "Mensal ou pontual",
        "Donativo único"
    ),
    "nl" to SupportDonationCopy(
        "Steun Levyra",
        "Gratis, zonder advertenties of tracking. Een donatie houdt dat zo.",
        "Levyra wordt door één ontwikkelaar gemaakt, zonder advertenties of tracking. Maakt het je muziek beter, dan helpt een kleine donatie om door te gaan.",
        "Maandelijks of eenmalig",
        "Eenmalige donatie"
    ),
    "pl" to SupportDonationCopy(
        "Wesprzyj Levyra",
        "Za darmo, bez reklam i śledzenia. Darowizna pomaga to utrzymać.",
        "Levyra tworzy jedna osoba, bez reklam i śledzenia. Jeśli sprawia, że Twoja muzyka jest lepsza, niewielka darowizna pomaga ją rozwijać.",
        "Co miesiąc lub jednorazowo",
        "Jednorazowa darowizna"
    ),
    "ro" to SupportDonationCopy(
        "Susține Levyra",
        "Gratuit, fără reclame și fără urmărire. O donație o păstrează așa.",
        "Levyra este creată de un singur dezvoltator, fără reclame și fără urmărire. Dacă îți face muzica mai bună, o mică donație o ajută să continue.",
        "Lunar sau o singură dată",
        "Donație unică"
    ),
    "el" to SupportDonationCopy(
        "Υποστηρίξτε το Levyra",
        "Δωρεάν, χωρίς διαφημίσεις και παρακολούθηση. Μια δωρεά το κρατά έτσι.",
        "Το Levyra το φτιάχνει ένας μόνο προγραμματιστής, χωρίς διαφημίσεις και παρακολούθηση. Αν κάνει τη μουσική σας καλύτερη, μια μικρή δωρεά βοηθά να συνεχίσει.",
        "Μηνιαία ή εφάπαξ",
        "Εφάπαξ δωρεά"
    ),
    "sv" to SupportDonationCopy(
        "Stöd Levyra",
        "Gratis, utan reklam och spårning. En donation håller det så.",
        "Levyra byggs av en enda utvecklare, utan reklam och spårning. Om det gör din musik bättre hjälper en liten donation att hålla det igång.",
        "Månadsvis eller engångs",
        "Engångsdonation"
    ),
    "da" to SupportDonationCopy(
        "Støt Levyra",
        "Gratis, uden reklamer og sporing. En donation holder det sådan.",
        "Levyra bliver lavet af én udvikler, uden reklamer og sporing. Hvis det gør din musik bedre, hjælper en lille donation med at holde det i gang.",
        "Månedlig eller engangs",
        "Engangsdonation"
    ),
    "cs" to SupportDonationCopy(
        "Podpořte Levyra",
        "Zdarma, bez reklam a sledování. Dar pomáhá, aby to tak zůstalo.",
        "Levyra vyvíjí jediný člověk, bez reklam a sledování. Pokud vám zlepšuje hudbu, malý dar pomáhá v jejím vývoji.",
        "Měsíčně nebo jednorázově",
        "Jednorázový dar"
    ),
    "sk" to SupportDonationCopy(
        "Podporte Levyra",
        "Zadarmo, bez reklám a sledovania. Dar pomáha, aby to tak zostalo.",
        "Levyra vyvíja jediný človek, bez reklám a sledovania. Ak vám zlepšuje hudbu, malý dar pomáha v jej vývoji.",
        "Mesačne alebo jednorazovo",
        "Jednorazový dar"
    ),
    "bn" to SupportDonationCopy(
        "Levyra-কে সমর্থন করুন",
        "ফ্রি, বিজ্ঞাপন ও ট্র্যাকিং ছাড়া। একটি অনুদান এটিকে এভাবেই রাখে।",
        "Levyra একজন ডেভেলপার তৈরি করেন, বিজ্ঞাপন ও ট্র্যাকিং ছাড়া। এটি আপনার গান শোনা ভালো করলে, ছোট একটি অনুদান এটিকে চালিয়ে যেতে সাহায্য করে।",
        "মাসিক বা একবার",
        "একবারের অনুদান"
    ),
    "kk" to SupportDonationCopy(
        "Levyra-ны қолдау",
        "Тегін, жарнамасыз және бақылаусыз. Қайырымдылық оны солай сақтайды.",
        "Levyra-ны бір әзірлеуші жасайды, жарнамасыз және бақылаусыз. Егер ол музыкаңызды жақсартса, шағын қайырымдылық оның дамуына көмектеседі.",
        "Ай сайын немесе бір рет",
        "Бір реттік қайырымдылық"
    ),
    "sr" to SupportDonationCopy(
        "Подржите Levyra",
        "Бесплатно, без реклама и праћења. Донација помаже да тако остане.",
        "Levyra прави један програмер, без реклама и праћења. Ако вам побољшава музику, мала донација помаже да се настави.",
        "Месечно или једнократно",
        "Једнократна донација"
    ),
    "sq" to SupportDonationCopy(
        "Mbështet Levyra",
        "Falas, pa reklama dhe pa gjurmim. Një dhurim e mban kështu.",
        "Levyra ndërtohet nga një zhvillues i vetëm, pa reklama dhe pa gjurmim. Nëse e bën muzikën tënde më të mirë, një dhurim i vogël e ndihmon të vazhdojë.",
        "Mujore ose një herë",
        "Dhurim një herë"
    ),
    "hr" to SupportDonationCopy(
        "Podržite Levyra",
        "Besplatno, bez oglasa i praćenja. Donacija pomaže da tako ostane.",
        "Levyra izrađuje jedan programer, bez oglasa i praćenja. Ako vam poboljšava glazbu, mala donacija pomaže da se nastavi.",
        "Mjesečno ili jednokratno",
        "Jednokratna donacija"
    ),
    "bg" to SupportDonationCopy(
        "Подкрепете Levyra",
        "Безплатно, без реклами и проследяване. Дарението го запазва такова.",
        "Levyra се създава от един разработчик, без реклами и проследяване. Ако прави музиката ви по-добра, малко дарение помага да продължи.",
        "Месечно или еднократно",
        "Еднократно дарение"
    ),
    "hu" to SupportDonationCopy(
        "Támogasd a Levyrát",
        "Ingyenes, reklámok és követés nélkül. Egy adomány segít, hogy így maradjon.",
        "A Levyrát egyetlen fejlesztő készíti, reklámok és követés nélkül. Ha jobbá teszi a zenehallgatásodat, egy kis adomány segíti a folytatást.",
        "Havonta vagy egyszeri",
        "Egyszeri adomány"
    ),
    "fi" to SupportDonationCopy(
        "Tue Levyraa",
        "Ilmainen, ilman mainoksia ja seurantaa. Lahjoitus pitää sen sellaisena.",
        "Levyraa tekee yksi kehittäjä, ilman mainoksia ja seurantaa. Jos se tekee musiikistasi parempaa, pieni lahjoitus auttaa jatkamaan.",
        "Kuukausittain tai kerran",
        "Kertalahjoitus"
    ),
    "et" to SupportDonationCopy(
        "Toeta Levyrat",
        "Tasuta, ilma reklaami ja jälgimiseta. Annetus hoiab selle nii.",
        "Levyrat teeb üks arendaja, ilma reklaami ja jälgimiseta. Kui see teeb su muusika paremaks, aitab väike annetus sel jätkata.",
        "Igakuine või ühekordne",
        "Ühekordne annetus"
    ),
    "nb" to SupportDonationCopy(
        "Støtt Levyra",
        "Gratis, uten reklame og sporing. En donasjon holder det slik.",
        "Levyra lages av én utvikler, uten reklame og sporing. Hvis det gjør musikken din bedre, hjelper en liten donasjon med å holde det i gang.",
        "Månedlig eller engangs",
        "Engangsdonasjon"
    ),
    "ca" to SupportDonationCopy(
        "Dona suport a Levyra",
        "Gratuït, sense anuncis ni seguiment. Una donació el manté així.",
        "Levyra el fa una sola persona, sense anuncis ni seguiment. Si millora la teva música, una petita donació ajuda que continuï.",
        "Mensual o puntual",
        "Donació puntual"
    ),
    "uk" to SupportDonationCopy(
        "Підтримати Levyra",
        "Безкоштовно, без реклами та стеження. Донат допомагає так і залишатися.",
        "Levyra створює один розробник, без реклами та стеження. Якщо застосунок робить вашу музику кращою, невеликий донат допомагає йому розвиватися.",
        "Щомісяця або одноразово",
        "Одноразовий донат"
    ),
    "ru" to SupportDonationCopy(
        "Поддержать Levyra",
        "Бесплатно, без рекламы и слежки. Донат помогает так и оставаться.",
        "Levyra делает один разработчик, без рекламы и слежки. Если приложение делает вашу музыку лучше, небольшой донат помогает ему развиваться.",
        "Ежемесячно или разово",
        "Разовый донат"
    ),
    "tr" to SupportDonationCopy(
        "Levyra’yı destekle",
        "Ücretsiz, reklamsız ve takipsiz. Bir bağış bunu böyle tutar.",
        "Levyra tek bir geliştirici tarafından, reklam ve takip olmadan yapılıyor. Müziğini daha iyi hale getiriyorsa küçük bir bağış devam etmesine yardım eder.",
        "Aylık veya tek seferlik",
        "Tek seferlik bağış"
    ),
    "ar" to SupportDonationCopy(
        "ادعم Levyra",
        "مجاني، بلا إعلانات ولا تتبع. التبرع يبقيه كذلك.",
        "يطوّر Levyra مطوّر واحد، بلا إعلانات ولا تتبع. إذا جعل موسيقاك أفضل، فإن تبرعًا صغيرًا يساعده على الاستمرار.",
        "شهري أو لمرة واحدة",
        "تبرع لمرة واحدة"
    ),
    "fa" to SupportDonationCopy(
        "حمایت از Levyra",
        "رایگان، بدون تبلیغ و ردیابی. یک کمک مالی آن را همین‌طور نگه می‌دارد.",
        "Levyra را یک توسعه‌دهنده می‌سازد، بدون تبلیغ و ردیابی. اگر موسیقی شما را بهتر می‌کند، یک کمک کوچک به ادامهٔ آن کمک می‌کند.",
        "ماهانه یا یک‌باره",
        "کمک یک‌باره"
    ),
    "zh" to SupportDonationCopy(
        "支持 Levyra",
        "免费、无广告、无追踪。你的捐助让它保持如此。",
        "Levyra 由一位开发者独立打造，没有广告，也没有追踪。如果它让你的音乐体验更好，一笔小小的捐助能帮助它持续发展。",
        "按月或一次性",
        "一次性捐助"
    ),
    "zh-Hant" to SupportDonationCopy(
        "支持 Levyra",
        "免費、無廣告、無追蹤。你的捐助讓它保持如此。",
        "Levyra 由一位開發者獨立打造，沒有廣告，也沒有追蹤。如果它讓你的音樂體驗更好，一筆小小的捐助能幫助它持續發展。",
        "按月或一次性",
        "一次性捐助"
    ),
    "ja" to SupportDonationCopy(
        "Levyra を支援",
        "無料・広告なし・トラッキングなし。寄付がこの形を支えます。",
        "Levyra は一人の開発者が、広告もトラッキングもなしで作っています。音楽体験が良くなったと感じたら、少額の寄付が開発の継続を支えます。",
        "毎月または1回",
        "1回限りの寄付"
    ),
    "ko" to SupportDonationCopy(
        "Levyra 후원하기",
        "무료, 광고 없음, 추적 없음. 후원이 이를 지켜 줍니다.",
        "Levyra는 한 명의 개발자가 광고와 추적 없이 만들고 있습니다. 음악 생활이 더 좋아졌다면, 작은 후원이 계속 개발하는 데 힘이 됩니다.",
        "매월 또는 일회성",
        "일회성 후원"
    ),
    "hi" to SupportDonationCopy(
        "Levyra को सपोर्ट करें",
        "मुफ़्त, बिना विज्ञापन और ट्रैकिंग के। एक दान इसे ऐसा ही बनाए रखता है।",
        "Levyra एक ही डेवलपर बनाता है, बिना विज्ञापन और ट्रैकिंग के। अगर यह आपके संगीत को बेहतर बनाता है, तो एक छोटा दान इसे आगे बढ़ाने में मदद करता है।",
        "मासिक या एक बार",
        "एक बार का दान"
    ),
    "id" to SupportDonationCopy(
        "Dukung Levyra",
        "Gratis, tanpa iklan, tanpa pelacakan. Donasi menjaganya tetap begitu.",
        "Levyra dibuat oleh satu pengembang, tanpa iklan dan pelacakan. Jika membuat musikmu lebih baik, donasi kecil membantunya terus berjalan.",
        "Bulanan atau sekali",
        "Donasi sekali"
    ),
    "ms" to SupportDonationCopy(
        "Sokong Levyra",
        "Percuma, tanpa iklan dan penjejakan. Derma mengekalkannya begitu.",
        "Levyra dibina oleh seorang pembangun, tanpa iklan dan penjejakan. Jika ia menjadikan muzik anda lebih baik, derma kecil membantu ia terus berjalan.",
        "Bulanan atau sekali",
        "Derma sekali"
    ),
    "vi" to SupportDonationCopy(
        "Ủng hộ Levyra",
        "Miễn phí, không quảng cáo, không theo dõi. Một khoản ủng hộ giúp giữ nguyên như vậy.",
        "Levyra do một nhà phát triển duy nhất làm ra, không quảng cáo và không theo dõi. Nếu ứng dụng giúp bạn nghe nhạc tốt hơn, một khoản ủng hộ nhỏ sẽ giúp nó tiếp tục.",
        "Hằng tháng hoặc một lần",
        "Ủng hộ một lần"
    ),
    "th" to SupportDonationCopy(
        "สนับสนุน Levyra",
        "ฟรี ไม่มีโฆษณา ไม่มีการติดตาม การบริจาคช่วยให้เป็นแบบนี้ต่อไป",
        "Levyra สร้างโดยนักพัฒนาเพียงคนเดียว ไม่มีโฆษณาและไม่มีการติดตาม หากช่วยให้การฟังเพลงของคุณดีขึ้น การบริจาคเล็กน้อยช่วยให้พัฒนาต่อได้",
        "รายเดือนหรือครั้งเดียว",
        "บริจาคครั้งเดียว"
    ),
    "fil" to SupportDonationCopy(
        "Suportahan ang Levyra",
        "Libre, walang ads at walang tracking. Pinananatili itong ganito ng donasyon.",
        "Ang Levyra ay gawa ng iisang developer, walang ads at walang tracking. Kung pinapaganda nito ang musika mo, nakakatulong ang maliit na donasyon para magpatuloy ito.",
        "Buwanan o isang beses",
        "Isang beses na donasyon"
    ),
    "he" to SupportDonationCopy(
        "תמכו ב-Levyra",
        "חינם, בלי פרסומות ובלי מעקב. תרומה שומרת על זה כך.",
        "את Levyra בונה מפתח יחיד, בלי פרסומות ובלי מעקב. אם היא משפרת את המוזיקה שלכם, תרומה קטנה עוזרת לה להמשיך.",
        "חודשי או חד־פעמי",
        "תרומה חד־פעמית"
    ),
    "ur" to SupportDonationCopy(
        "Levyra کی حمایت کریں۔",
        "مفت، کوئی اشتہار نہیں، کوئی ٹریکنگ نہیں۔ ایک عطیہ اسے اسی طرح رکھتا ہے۔",
        "Levyra اشتہارات یا ٹریکنگ کے بغیر ایک ڈویلپر نے بنایا ہے۔ اگر یہ آپ کی موسیقی کو بہتر بناتا ہے، تو ایک چھوٹا سا عطیہ اسے جاری رکھنے میں مدد کرتا ہے۔",
        "ماہانہ یا ایک بار",
        "ایک بار کا عطیہ"
    ),
    "ta" to SupportDonationCopy(
        "ஆதரவு Levyra",
        "இலவசம், விளம்பரங்கள் இல்லை, கண்காணிப்பு இல்லை. ஒரு நன்கொடை அதை அப்படியே வைத்திருக்கும்.",
        "Levyra விளம்பரங்கள் அல்லது கண்காணிப்பு இல்லாமல் ஒரு டெவலப்பரால் உருவாக்கப்பட்டது. இது உங்கள் இசையை சிறப்பாக்கினால், ஒரு சிறிய நன்கொடை அதைத் தொடர உதவும்.",
        "மாதாந்திர அல்லது ஒரு முறை",
        "ஒருமுறை நன்கொடை"
    ),
    "te" to SupportDonationCopy(
        "మద్దతు Levyra",
        "ఉచితం, ప్రకటనలు లేవు, ట్రాకింగ్ లేదు. విరాళం దానిని అలాగే ఉంచుతుంది.",
        "Levyra ప్రకటనలు లేదా ట్రాకింగ్ లేకుండా ఒక డెవలపర్ ద్వారా నిర్మించబడింది. ఇది మీ సంగీతాన్ని మెరుగుపరచినట్లయితే, చిన్న విరాళం దానిని కొనసాగించడంలో సహాయపడుతుంది.",
        "నెలవారీ లేదా ఒక సారి",
        "ఒక్కసారి విరాళం"
    ),
    "mr" to SupportDonationCopy(
        "Levyra चे समर्थन करा",
        "विनामूल्य, जाहिराती नाहीत, ट्रॅकिंग नाही. देणगी तशीच ठेवते.",
        "Levyra जाहिराती किंवा ट्रॅकिंगशिवाय एका विकसकाने तयार केले आहे. जर ते तुमचे संगीत चांगले बनवते, तर एक लहान देणगी ते चालू ठेवण्यास मदत करते.",
        "मासिक किंवा एकदाच",
        "एकवेळ दान"
    ),
    "gu" to SupportDonationCopy(
        "આધાર Levyra",
        "મફત, કોઈ જાહેરાતો, કોઈ ટ્રેકિંગ નહીં. દાન તે રીતે રાખે છે.",
        "Levyra જાહેરાતો અથવા ટ્રેકિંગ વિના, એક વિકાસકર્તા દ્વારા બનાવવામાં આવ્યું છે. જો તે તમારા સંગીતને વધુ સારું બનાવે છે, તો એક નાનું દાન તેને ચાલુ રાખવામાં મદદ કરે છે.",
        "માસિક અથવા એક વખત",
        "એક વખતનું દાન"
    ),
    "pt-BR" to SupportDonationCopy(
        "Apoie o Levyra",
        "Grátis, sem anúncios e sem rastreamento. Uma doação ajuda a manter assim.",
        "O Levyra é feito por um único desenvolvedor, sem anúncios e sem rastreamento. Se ele deixa sua música melhor, uma pequena doação ajuda a manter o projeto vivo.",
        "Mensal ou avulsa",
        "Doação avulsa"
    )
).also { copies ->
    require(copies.keys == supportedLocalizationCodes()) {
        "Invalid support donation localization coverage: missing=${supportedLocalizationCodes() - copies.keys}, extra=${copies.keys - supportedLocalizationCodes()}"
    }
}

internal fun supportDonationCopy(code: String): SupportDonationCopy =
    localizedValueOrEnglish(supportDonationCopies, code)
