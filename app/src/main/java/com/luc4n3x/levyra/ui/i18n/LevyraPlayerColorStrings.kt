package com.luc4n3x.levyra.ui.i18n

internal val playerColorKeys = setOf(
    "playerColors",
    "playerColorsSubtitle",
    "playerColorsDynamic",
    "playerColorsTheme",
    "playerColorsCustom",
    "playerColorsDynamicHint",
    "playerColorsThemeHint",
    "playerColorsCustomHint"
)

private fun playerColors(
    title: String,
    subtitle: String,
    dynamic: String,
    theme: String,
    custom: String,
    dynamicHint: String,
    themeHint: String,
    customHint: String
): Map<String, String> = mapOf(
    "playerColors" to title,
    "playerColorsSubtitle" to subtitle,
    "playerColorsDynamic" to dynamic,
    "playerColorsTheme" to theme,
    "playerColorsCustom" to custom,
    "playerColorsDynamicHint" to dynamicHint,
    "playerColorsThemeHint" to themeHint,
    "playerColorsCustomHint" to customHint
)

private val playerColorBundles = mapOf(
    "en" to playerColors("Player colors", "Follow the artwork or keep fixed colors", "Dynamic", "Theme colors", "Custom", "Colors change with the artwork of every song. This is the default.", "The player keeps your Theme Studio colors on every song.", "The player keeps a palette built from the color you pick."),
    "it" to playerColors("Colori del player", "Segui la copertina o mantieni colori fissi", "Dinamico", "Colori del tema", "Personalizzato", "I colori cambiano con la copertina di ogni brano. È l'impostazione predefinita.", "Il player mantiene i colori di Theme Studio su ogni brano.", "Il player mantiene una palette creata dal colore che scegli."),
    "es" to playerColors("Colores del reproductor", "Sigue la portada o mantén colores fijos", "Dinámico", "Colores del tema", "Personalizado", "Los colores cambian con la portada de cada canción. Es la opción predeterminada.", "El reproductor mantiene los colores de Theme Studio en cada canción.", "El reproductor mantiene una paleta creada a partir del color que elijas."),
    "fr" to playerColors("Couleurs du lecteur", "Suivre la pochette ou garder des couleurs fixes", "Dynamique", "Couleurs du thème", "Personnalisé", "Les couleurs changent avec la pochette de chaque titre. C'est le réglage par défaut.", "Le lecteur garde les couleurs de Theme Studio sur chaque titre.", "Le lecteur garde une palette créée à partir de la couleur choisie."),
    "de" to playerColors("Player-Farben", "Dem Cover folgen oder feste Farben behalten", "Dynamisch", "Designfarben", "Eigene", "Die Farben wechseln mit dem Cover jedes Titels. Das ist die Standardeinstellung.", "Der Player behält bei jedem Titel die Farben aus Theme Studio.", "Der Player behält eine Palette aus der von dir gewählten Farbe."),
    "pt" to playerColors("Cores do leitor", "Seguir a capa ou manter cores fixas", "Dinâmico", "Cores do tema", "Personalizado", "As cores mudam com a capa de cada música. É a opção predefinida.", "O leitor mantém as cores do Theme Studio em todas as músicas.", "O leitor mantém uma paleta criada a partir da cor que escolheres."),
    "nl" to playerColors("Spelerkleuren", "Volg de hoes of houd vaste kleuren", "Dynamisch", "Themakleuren", "Aangepast", "De kleuren veranderen met de hoes van elk nummer. Dit is de standaard.", "De speler houdt bij elk nummer je Theme Studio-kleuren.", "De speler houdt een palet op basis van de kleur die je kiest."),
    "pl" to playerColors("Kolory odtwarzacza", "Dopasuj do okładki lub zachowaj stałe kolory", "Dynamiczne", "Kolory motywu", "Własne", "Kolory zmieniają się wraz z okładką każdego utworu. To ustawienie domyślne.", "Odtwarzacz zachowuje kolory z Theme Studio dla każdego utworu.", "Odtwarzacz zachowuje paletę utworzoną z wybranego koloru."),
    "ro" to playerColors("Culorile playerului", "Urmează coperta sau păstrează culori fixe", "Dinamic", "Culorile temei", "Personalizat", "Culorile se schimbă odată cu coperta fiecărei melodii. Aceasta este setarea implicită.", "Playerul păstrează culorile din Theme Studio la fiecare melodie.", "Playerul păstrează o paletă creată din culoarea aleasă."),
    "el" to playerColors("Χρώματα αναπαραγωγής", "Ακολούθησε το εξώφυλλο ή κράτα σταθερά χρώματα", "Δυναμικά", "Χρώματα θέματος", "Προσαρμογή", "Τα χρώματα αλλάζουν με το εξώφυλλο κάθε τραγουδιού. Είναι η προεπιλογή.", "Η αναπαραγωγή κρατά τα χρώματα του Theme Studio σε κάθε τραγούδι.", "Η αναπαραγωγή κρατά μια παλέτα από το χρώμα που επιλέγεις."),
    "sv" to playerColors("Spelarfärger", "Följ omslaget eller behåll fasta färger", "Dynamiska", "Temafärger", "Anpassade", "Färgerna ändras med omslaget för varje låt. Detta är standard.", "Spelaren behåller dina Theme Studio-färger för varje låt.", "Spelaren behåller en palett skapad från färgen du väljer."),
    "da" to playerColors("Afspillerfarver", "Følg coveret eller behold faste farver", "Dynamiske", "Temafarver", "Tilpasset", "Farverne skifter med coveret på hvert nummer. Dette er standard.", "Afspilleren beholder dine Theme Studio-farver på hvert nummer.", "Afspilleren beholder en palet lavet ud fra den farve, du vælger."),
    "cs" to playerColors("Barvy přehrávače", "Řídit se obalem, nebo ponechat pevné barvy", "Dynamické", "Barvy motivu", "Vlastní", "Barvy se mění podle obalu každé skladby. Toto je výchozí nastavení.", "Přehrávač si u každé skladby ponechá barvy z Theme Studia.", "Přehrávač si ponechá paletu vytvořenou ze zvolené barvy."),
    "sk" to playerColors("Farby prehrávača", "Riadiť sa obalom alebo ponechať pevné farby", "Dynamické", "Farby motívu", "Vlastné", "Farby sa menia podľa obalu každej skladby. Toto je predvolené nastavenie.", "Prehrávač si pri každej skladbe ponechá farby z Theme Studia.", "Prehrávač si ponechá paletu vytvorenú zo zvolenej farby."),
    "hr" to playerColors("Boje reproduktora", "Prati naslovnicu ili zadrži fiksne boje", "Dinamičke", "Boje teme", "Prilagođeno", "Boje se mijenjaju s naslovnicom svake pjesme. Ovo je zadana postavka.", "Reproduktor zadržava boje iz Theme Studija na svakoj pjesmi.", "Reproduktor zadržava paletu izrađenu iz odabrane boje."),
    "sr" to playerColors("Боје плејера", "Прати омот или задржи сталне боје", "Динамичке", "Боје теме", "Прилагођено", "Боје се мењају са омотом сваке песме. Ово је подразумевано подешавање.", "Плејер задржава боје из Theme Studio-а на свакој песми.", "Плејер задржава палету направљену од изабране боје."),
    "sq" to playerColors("Ngjyrat e luajtësit", "Ndiq kopertinën ose mbaj ngjyra fikse", "Dinamike", "Ngjyrat e temës", "E personalizuar", "Ngjyrat ndryshojnë sipas kopertinës së çdo kënge. Ky është cilësimi i parazgjedhur.", "Luajtësi ruan ngjyrat e Theme Studio në çdo këngë.", "Luajtësi ruan një paletë të krijuar nga ngjyra që zgjedh."),
    "bg" to playerColors("Цветове на плеъра", "Следвай обложката или запази постоянни цветове", "Динамични", "Цветове на темата", "Персонализирани", "Цветовете се променят с обложката на всяка песен. Това е стандартната настройка.", "Плеърът запазва цветовете от Theme Studio за всяка песен.", "Плеърът запазва палитра, създадена от избрания цвят."),
    "hu" to playerColors("Lejátszó színei", "Kövesse a borítót vagy tartson fix színeket", "Dinamikus", "Téma színei", "Egyéni", "A színek minden dal borítójával változnak. Ez az alapértelmezett.", "A lejátszó minden dalnál megtartja a Theme Studio színeit.", "A lejátszó a kiválasztott színből készített palettát tartja meg."),
    "fi" to playerColors("Soittimen värit", "Seuraa kansikuvaa tai pidä värit kiinteinä", "Dynaaminen", "Teeman värit", "Oma", "Värit vaihtuvat jokaisen kappaleen kansikuvan mukaan. Tämä on oletus.", "Soitin pitää Theme Studion värit jokaisessa kappaleessa.", "Soitin pitää valitsemastasi väristä luodun paletin."),
    "et" to playerColors("Mängija värvid", "Järgi kaanepilti või hoia värvid püsivana", "Dünaamiline", "Teema värvid", "Kohandatud", "Värvid muutuvad iga loo kaanepildi järgi. See on vaikeseade.", "Mängija hoiab igas loos Theme Studio värve.", "Mängija hoiab sinu valitud värvist loodud paletti."),
    "nb" to playerColors("Spillerfarger", "Følg omslaget eller behold faste farger", "Dynamiske", "Temafarger", "Tilpasset", "Fargene endres med omslaget på hver sang. Dette er standard.", "Spilleren beholder fargene fra Theme Studio på hver sang.", "Spilleren beholder en palett laget ut fra fargen du velger."),
    "ca" to playerColors("Colors del reproductor", "Segueix la caràtula o mantén colors fixos", "Dinàmics", "Colors del tema", "Personalitzat", "Els colors canvien amb la caràtula de cada cançó. És l'opció per defecte.", "El reproductor manté els colors del Theme Studio a cada cançó.", "El reproductor manté una paleta creada a partir del color que triïs."),
    "uk" to playerColors("Кольори плеєра", "Слідувати обкладинці або зберігати сталі кольори", "Динамічні", "Кольори теми", "Власні", "Кольори змінюються з обкладинкою кожної пісні. Це типове налаштування.", "Плеєр зберігає кольори Theme Studio для кожної пісні.", "Плеєр зберігає палітру, створену з вибраного кольору."),
    "ru" to playerColors("Цвета плеера", "Следовать обложке или сохранять постоянные цвета", "Динамические", "Цвета темы", "Свои", "Цвета меняются вместе с обложкой каждого трека. Это настройка по умолчанию.", "Плеер сохраняет цвета Theme Studio для каждого трека.", "Плеер сохраняет палитру, созданную из выбранного цвета."),
    "kk" to playerColors("Ойнатқыш түстері", "Мұқабаға ілесу немесе тұрақты түстерді сақтау", "Динамикалық", "Тақырып түстері", "Арнайы", "Түстер әр әннің мұқабасымен бірге өзгереді. Бұл әдепкі баптау.", "Ойнатқыш әр әнде Theme Studio түстерін сақтайды.", "Ойнатқыш таңдалған түстен құралған палитраны сақтайды."),
    "tr" to playerColors("Oynatıcı renkleri", "Kapağı izle veya sabit renkleri koru", "Dinamik", "Tema renkleri", "Özel", "Renkler her şarkının kapağıyla değişir. Varsayılan ayar budur.", "Oynatıcı her şarkıda Theme Studio renklerini korur.", "Oynatıcı seçtiğin renkten oluşturulan paleti korur."),
    "ar" to playerColors("ألوان المشغل", "اتبع الغلاف أو احتفظ بألوان ثابتة", "ديناميكية", "ألوان السمة", "مخصصة", "تتغير الألوان مع غلاف كل أغنية. هذا هو الإعداد الافتراضي.", "يحتفظ المشغل بألوان Theme Studio في كل أغنية.", "يحتفظ المشغل بلوحة ألوان مبنية على اللون الذي تختاره."),
    "fa" to playerColors("رنگ‌های پخش‌کننده", "همراهی با کاور یا حفظ رنگ‌های ثابت", "پویا", "رنگ‌های پوسته", "سفارشی", "رنگ‌ها با کاور هر آهنگ تغییر می‌کنند. این حالت پیش‌فرض است.", "پخش‌کننده در هر آهنگ رنگ‌های Theme Studio را حفظ می‌کند.", "پخش‌کننده پالت ساخته‌شده از رنگ انتخابی شما را حفظ می‌کند."),
    "zh" to playerColors("播放器颜色", "跟随封面或保持固定颜色", "动态", "主题颜色", "自定义", "颜色会随每首歌曲的封面变化。这是默认设置。", "播放器在每首歌曲中都保持 Theme Studio 的颜色。", "播放器保持由你选择的颜色生成的配色。"),
    "zh-Hant" to playerColors("播放器顏色", "跟隨封面或保持固定顏色", "動態", "主題顏色", "自訂", "顏色會隨每首歌曲的封面變化。這是預設設定。", "播放器在每首歌曲中都保持 Theme Studio 的顏色。", "播放器保持由你選擇的顏色產生的配色。"),
    "ja" to playerColors("プレーヤーの色", "ジャケットに合わせるか、色を固定するか", "ダイナミック", "テーマの色", "カスタム", "曲ごとのジャケットに合わせて色が変わります。これが既定です。", "どの曲でも Theme Studio の色を保ちます。", "選んだ色から作ったパレットを保ちます。"),
    "ko" to playerColors("플레이어 색상", "커버를 따르거나 고정 색상 유지", "다이내믹", "테마 색상", "사용자 지정", "곡마다 커버에 맞춰 색상이 바뀝니다. 기본 설정입니다.", "모든 곡에서 Theme Studio 색상을 유지합니다.", "선택한 색상으로 만든 팔레트를 유지합니다."),
    "hi" to playerColors("प्लेयर के रंग", "कवर का अनुसरण करें या रंग स्थिर रखें", "डायनेमिक", "थीम के रंग", "कस्टम", "हर गाने के कवर के साथ रंग बदलते हैं। यह डिफ़ॉल्ट है।", "प्लेयर हर गाने पर Theme Studio के रंग बनाए रखता है।", "प्लेयर आपके चुने रंग से बनी पैलेट बनाए रखता है।"),
    "bn" to playerColors("প্লেয়ারের রঙ", "কভার অনুসরণ করুন বা স্থির রঙ রাখুন", "ডায়নামিক", "থিমের রঙ", "কাস্টম", "প্রতিটি গানের কভারের সাথে রঙ বদলায়। এটি ডিফল্ট সেটিং।", "প্লেয়ার প্রতিটি গানে Theme Studio-র রঙ ধরে রাখে।", "প্লেয়ার আপনার বেছে নেওয়া রঙ থেকে তৈরি প্যালেট ধরে রাখে।"),
    "id" to playerColors("Warna pemutar", "Ikuti sampul atau pertahankan warna tetap", "Dinamis", "Warna tema", "Kustom", "Warna berubah mengikuti sampul setiap lagu. Ini pengaturan bawaan.", "Pemutar mempertahankan warna Theme Studio di setiap lagu.", "Pemutar mempertahankan palet dari warna yang kamu pilih."),
    "ms" to playerColors("Warna pemain", "Ikut kulit album atau kekalkan warna tetap", "Dinamik", "Warna tema", "Tersuai", "Warna berubah mengikut kulit setiap lagu. Ini ialah tetapan lalai.", "Pemain mengekalkan warna Theme Studio pada setiap lagu.", "Pemain mengekalkan palet yang dibina daripada warna pilihan anda."),
    "vi" to playerColors("Màu trình phát", "Theo ảnh bìa hoặc giữ màu cố định", "Động", "Màu chủ đề", "Tùy chỉnh", "Màu thay đổi theo ảnh bìa của từng bài hát. Đây là mặc định.", "Trình phát giữ màu Theme Studio cho mọi bài hát.", "Trình phát giữ bảng màu tạo từ màu bạn chọn."),
    "th" to playerColors("สีของโปรแกรมเล่น", "ตามปกเพลงหรือคงสีไว้", "ไดนามิก", "สีของธีม", "กำหนดเอง", "สีจะเปลี่ยนตามปกของแต่ละเพลง นี่คือค่าเริ่มต้น", "โปรแกรมเล่นจะคงสีจาก Theme Studio ในทุกเพลง", "โปรแกรมเล่นจะคงชุดสีที่สร้างจากสีที่คุณเลือก"),
    "fil" to playerColors("Mga kulay ng player", "Sundan ang cover o panatilihin ang kulay", "Dynamic", "Mga kulay ng tema", "Custom", "Nagbabago ang kulay ayon sa cover ng bawat kanta. Ito ang default.", "Pinapanatili ng player ang mga kulay ng Theme Studio sa bawat kanta.", "Pinapanatili ng player ang palette mula sa kulay na pinili mo."),
    "he" to playerColors("צבעי הנגן", "לעקוב אחר העטיפה או לשמור צבעים קבועים", "דינמי", "צבעי ערכת הנושא", "מותאם אישית", "הצבעים משתנים עם העטיפה של כל שיר. זו ברירת המחדל.", "הנגן שומר על צבעי Theme Studio בכל שיר.", "הנגן שומר על פלטה שנבנתה מהצבע שבחרת.")
)

internal fun playerColorLocalizationCodes(): Set<String> = playerColorBundles.keys

internal fun playerColorLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(playerColorBundles, code)
