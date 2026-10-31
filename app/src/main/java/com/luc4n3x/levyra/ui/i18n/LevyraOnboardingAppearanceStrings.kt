package com.luc4n3x.levyra.ui.i18n


internal data class OnboardingAppearanceCopy(
    val title: String,
    val body: String,
    val dark: String,
    val darkBody: String,
    val light: String,
    val lightBody: String,
    val footnote: String
)

internal object LevyraOnboardingAppearanceStrings {
    private val copies: Map<String, OnboardingAppearanceCopy> = mapOf(
        "en" to OnboardingAppearanceCopy("Pick your look", "Choose how Levyra feels. You'll see it right away.", "Dark", "Deep and immersive, made for the night", "Light", "Bright and crisp, made for the day", "You can change it anytime in Theme Studio."),
        "it" to OnboardingAppearanceCopy("Scegli il tuo stile", "Decidi come vuoi vivere Levyra. Lo vedi subito.", "Scuro", "Profondo e immersivo, nato per la sera", "Chiaro", "Luminoso e nitido, nato per il giorno", "Puoi cambiarlo quando vuoi in Theme Studio."),
        "es" to OnboardingAppearanceCopy("Elige tu estilo", "Decide cómo quieres sentir Levyra. Lo verás al instante.", "Oscuro", "Profundo e inmersivo, pensado para la noche", "Claro", "Luminoso y nítido, pensado para el día", "Puedes cambiarlo cuando quieras en Theme Studio."),
        "fr" to OnboardingAppearanceCopy("Choisissez votre style", "Décidez de l'ambiance de Levyra. L'aperçu est immédiat.", "Sombre", "Profond et immersif, pensé pour le soir", "Clair", "Lumineux et net, pensé pour la journée", "Vous pourrez le changer à tout moment dans Theme Studio."),
        "de" to OnboardingAppearanceCopy("Wähle deinen Look", "Entscheide, wie sich Levyra anfühlt. Du siehst es sofort.", "Dunkel", "Tief und immersiv, gemacht für den Abend", "Hell", "Hell und klar, gemacht für den Tag", "Du kannst es jederzeit in Theme Studio ändern."),
        "pt" to OnboardingAppearanceCopy("Escolhe o teu estilo", "Decide como queres sentir o Levyra. Vês logo o resultado.", "Escuro", "Profundo e imersivo, feito para a noite", "Claro", "Luminoso e nítido, feito para o dia", "Podes mudar quando quiseres no Theme Studio."),
        "nl" to OnboardingAppearanceCopy("Kies je stijl", "Bepaal hoe Levyra voelt. Je ziet het meteen.", "Donker", "Diep en meeslepend, gemaakt voor de avond", "Licht", "Helder en fris, gemaakt voor overdag", "Je kunt het altijd wijzigen in Theme Studio."),
        "pl" to OnboardingAppearanceCopy("Wybierz swój styl", "Zdecyduj, jak ma wyglądać Levyra. Zobaczysz to od razu.", "Ciemny", "Głęboki i wciągający, stworzony na wieczór", "Jasny", "Jasny i wyraźny, stworzony na dzień", "Możesz to zmienić w każdej chwili w Theme Studio."),
        "ro" to OnboardingAppearanceCopy("Alege-ți stilul", "Decide cum vrei să arate Levyra. Vezi imediat.", "Întunecat", "Profund și captivant, pentru seară", "Luminos", "Clar și proaspăt, pentru zi", "Îl poți schimba oricând din Theme Studio."),
        "el" to OnboardingAppearanceCopy("Διάλεξε το στυλ σου", "Αποφάσισε πώς θα νιώθεις το Levyra. Το βλέπεις αμέσως.", "Σκούρο", "Βαθύ και καθηλωτικό, για το βράδυ", "Ανοιχτό", "Φωτεινό και καθαρό, για τη μέρα", "Μπορείς να το αλλάξεις όποτε θέλεις στο Theme Studio."),
        "sv" to OnboardingAppearanceCopy("Välj din stil", "Bestäm hur Levyra ska kännas. Du ser det direkt.", "Mörkt", "Djupt och uppslukande, gjort för kvällen", "Ljust", "Ljust och skarpt, gjort för dagen", "Du kan ändra det när som helst i Theme Studio."),
        "da" to OnboardingAppearanceCopy("Vælg din stil", "Bestem, hvordan Levyra skal føles. Du ser det med det samme.", "Mørkt", "Dybt og medrivende, skabt til aftenen", "Lyst", "Lyst og skarpt, skabt til dagen", "Du kan altid ændre det i Theme Studio."),
        "cs" to OnboardingAppearanceCopy("Vyber si styl", "Rozhodni, jak má Levyra působit. Uvidíš to hned.", "Tmavý", "Hluboký a pohlcující, pro večer", "Světlý", "Jasný a čistý, pro den", "Kdykoli ho změníš v Theme Studiu."),
        "sk" to OnboardingAppearanceCopy("Vyber si štýl", "Rozhodni, ako má Levyra pôsobiť. Uvidíš to hneď.", "Tmavý", "Hlboký a pohlcujúci, na večer", "Svetlý", "Jasný a čistý, na deň", "Kedykoľvek ho zmeníš v Theme Studiu."),
        "bn" to OnboardingAppearanceCopy("তোমার স্টাইল বেছে নাও", "Levyra কেমন লাগবে তা ঠিক করো। সঙ্গে সঙ্গে দেখতে পাবে।", "ডার্ক", "গভীর ও নিমগ্ন, রাতের জন্য", "লাইট", "উজ্জ্বল ও স্পষ্ট, দিনের জন্য", "Theme Studio-তে যেকোনো সময় বদলাতে পারবে।"),
        "kk" to OnboardingAppearanceCopy("Стиліңді таңда", "Levyra қалай көрінетінін шеш. Бірден көресің.", "Қараңғы", "Терең әрі әсерлі, кешке арналған", "Жарық", "Жарық әрі анық, күндізге арналған", "Оны кез келген уақытта Theme Studio-да өзгерте аласың."),
        "sr" to OnboardingAppearanceCopy("Изабери свој стил", "Одлучи какав ће бити Levyra. Видиш одмах.", "Тамно", "Дубоко и упечатљиво, за вече", "Светло", "Светло и јасно, за дан", "Можеш да га промениш било када у Theme Studio."),
        "sq" to OnboardingAppearanceCopy("Zgjidh stilin tënd", "Vendos si do të ndihet Levyra. E sheh menjëherë.", "I errët", "I thellë dhe zhytës, për mbrëmjen", "I çelët", "I ndritshëm dhe i qartë, për ditën", "Mund ta ndryshosh kur të duash te Theme Studio."),
        "hr" to OnboardingAppearanceCopy("Odaberi svoj stil", "Odluči kakav će biti Levyra. Vidiš odmah.", "Tamno", "Duboko i upečatljivo, za večer", "Svijetlo", "Svijetlo i jasno, za dan", "Možeš ga promijeniti bilo kada u Theme Studiju."),
        "bg" to OnboardingAppearanceCopy("Избери своя стил", "Реши как да изглежда Levyra. Виждаш го веднага.", "Тъмно", "Дълбоко и завладяващо, за вечерта", "Светло", "Светло и ясно, за деня", "Можеш да го смениш по всяко време в Theme Studio."),
        "hu" to OnboardingAppearanceCopy("Válaszd ki a stílusod", "Döntsd el, milyen legyen a Levyra. Azonnal látod.", "Sötét", "Mély és magával ragadó, estére", "Világos", "Világos és tiszta, nappalra", "Bármikor módosíthatod a Theme Studióban."),
        "fi" to OnboardingAppearanceCopy("Valitse tyylisi", "Päätä, miltä Levyra tuntuu. Näet sen heti.", "Tumma", "Syvä ja mukaansatempaava, illoille", "Vaalea", "Kirkas ja selkeä, päiville", "Voit vaihtaa sen milloin tahansa Theme Studiossa."),
        "et" to OnboardingAppearanceCopy("Vali oma stiil", "Otsusta, milline Levyra on. Näed seda kohe.", "Tume", "Sügav ja kaasahaarav, õhtuks", "Hele", "Hele ja selge, päevaks", "Saad seda igal ajal Theme Studios muuta."),
        "nb" to OnboardingAppearanceCopy("Velg din stil", "Bestem hvordan Levyra skal føles. Du ser det med en gang.", "Mørk", "Dyp og oppslukende, laget for kvelden", "Lys", "Lys og skarp, laget for dagen", "Du kan endre det når som helst i Theme Studio."),
        "ca" to OnboardingAppearanceCopy("Tria el teu estil", "Decideix com vols sentir Levyra. Ho veuràs a l'instant.", "Fosc", "Profund i immersiu, pensat per al vespre", "Clar", "Lluminós i nítid, pensat per al dia", "El pots canviar quan vulguis a Theme Studio."),
        "uk" to OnboardingAppearanceCopy("Обери свій стиль", "Виріши, яким буде Levyra. Побачиш одразу.", "Темний", "Глибокий і захопливий, для вечора", "Світлий", "Яскравий і чіткий, для дня", "Змінити можна будь-коли в Theme Studio."),
        "ru" to OnboardingAppearanceCopy("Выбери свой стиль", "Реши, каким будет Levyra. Увидишь сразу.", "Тёмная", "Глубокая и атмосферная, для вечера", "Светлая", "Яркая и чёткая, для дня", "Изменить можно в любой момент в Theme Studio."),
        "tr" to OnboardingAppearanceCopy("Tarzını seç", "Levyra'nın nasıl görüneceğine karar ver. Hemen göreceksin.", "Koyu", "Derin ve sürükleyici, akşamlar için", "Açık", "Parlak ve net, gündüzler için", "Bunu istediğin zaman Theme Studio'dan değiştirebilirsin."),
        "ar" to OnboardingAppearanceCopy("اختر أسلوبك", "قرّر كيف تريد أن يبدو Levyra. سترى النتيجة فورًا.", "داكن", "عميق وغامر، مصمم للمساء", "فاتح", "مشرق وواضح، مصمم للنهار", "يمكنك تغييره في أي وقت من Theme Studio."),
        "fa" to OnboardingAppearanceCopy("سبک خودت را انتخاب کن", "تصمیم بگیر Levyra چه حسی داشته باشد. فوراً می‌بینی.", "تیره", "عمیق و فراگیر، برای شب", "روشن", "روشن و شفاف، برای روز", "هر زمان بخواهی می‌توانی آن را در Theme Studio تغییر دهی."),
        "zh" to OnboardingAppearanceCopy("选择你的风格", "决定 Levyra 的外观，立即预览。", "深色", "沉浸深邃，适合夜晚", "浅色", "明亮清爽，适合白天", "随时可在 Theme Studio 中更改。"),
        "zh-Hant" to OnboardingAppearanceCopy("選擇你的風格", "決定 Levyra 的外觀，立即預覽。", "深色", "沉浸深邃，適合夜晚", "淺色", "明亮清爽，適合白天", "隨時可在 Theme Studio 中變更。"),
        "ja" to OnboardingAppearanceCopy("スタイルを選ぶ", "Levyra の見た目を決めましょう。すぐに反映されます。", "ダーク", "深く没入できる、夜のための表示", "ライト", "明るくくっきり、昼のための表示", "Theme Studio でいつでも変更できます。"),
        "ko" to OnboardingAppearanceCopy("스타일 선택", "Levyra의 분위기를 정하세요. 바로 확인할 수 있어요.", "다크", "깊고 몰입감 있는, 밤을 위한 모드", "라이트", "밝고 선명한, 낮을 위한 모드", "Theme Studio에서 언제든지 바꿀 수 있어요."),
        "hi" to OnboardingAppearanceCopy("अपना स्टाइल चुनें", "तय करें कि Levyra कैसा दिखे। तुरंत देखें।", "डार्क", "गहरा और डूब जाने वाला, रात के लिए", "लाइट", "उजला और साफ़, दिन के लिए", "इसे कभी भी Theme Studio में बदल सकते हैं।"),
        "id" to OnboardingAppearanceCopy("Pilih gayamu", "Tentukan tampilan Levyra. Langsung terlihat.", "Gelap", "Dalam dan imersif, untuk malam hari", "Terang", "Cerah dan tajam, untuk siang hari", "Kamu bisa mengubahnya kapan saja di Theme Studio."),
        "ms" to OnboardingAppearanceCopy("Pilih gaya anda", "Tentukan rupa Levyra. Anda akan melihatnya serta-merta.", "Gelap", "Mendalam dan mengasyikkan, untuk waktu malam", "Cerah", "Terang dan jelas, untuk waktu siang", "Anda boleh menukarnya bila-bila masa dalam Theme Studio."),
        "vi" to OnboardingAppearanceCopy("Chọn phong cách", "Quyết định giao diện của Levyra. Thấy ngay lập tức.", "Tối", "Sâu lắng và cuốn hút, dành cho buổi tối", "Sáng", "Tươi sáng và sắc nét, dành cho ban ngày", "Bạn có thể đổi bất cứ lúc nào trong Theme Studio."),
        "th" to OnboardingAppearanceCopy("เลือกสไตล์ของคุณ", "เลือกหน้าตาของ Levyra แล้วดูผลได้ทันที", "มืด", "ลึกและดื่มด่ำ เหมาะกับยามค่ำ", "สว่าง", "สว่างและคมชัด เหมาะกับกลางวัน", "เปลี่ยนได้ทุกเมื่อใน Theme Studio"),
        "fil" to OnboardingAppearanceCopy("Piliin ang istilo mo", "Pagpasyahan ang itsura ng Levyra. Makikita mo agad.", "Madilim", "Malalim at nakakalubog, para sa gabi", "Maliwanag", "Maliwanag at malinaw, para sa araw", "Mapapalitan mo ito anumang oras sa Theme Studio."),
        "he" to OnboardingAppearanceCopy("בחר את הסגנון שלך", "החלט איך Levyra ירגיש. תראה את זה מיד.", "כהה", "עמוק וסוחף, לשעות הערב", "בהיר", "בהיר וחד, לשעות היום", "אפשר לשנות בכל עת ב-Theme Studio.")
    )

    fun forCode(code: String): OnboardingAppearanceCopy =
        localizedValueOrEnglish(copies, code).forBrazil(code) { brazilian() }
}

private fun OnboardingAppearanceCopy.brazilian(): OnboardingAppearanceCopy = copy(
    title = LevyraBrazilianPortuguese.adapt(title),
    body = LevyraBrazilianPortuguese.adapt(body),
    dark = LevyraBrazilianPortuguese.adapt(dark),
    darkBody = LevyraBrazilianPortuguese.adapt(darkBody),
    light = LevyraBrazilianPortuguese.adapt(light),
    lightBody = LevyraBrazilianPortuguese.adapt(lightBody),
    footnote = LevyraBrazilianPortuguese.adapt(footnote)
)
