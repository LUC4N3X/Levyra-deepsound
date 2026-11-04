package com.luc4n3x.levyra.ui.i18n

internal data class TasteCardCopy(
    val yourSound: String,
    val nightOwl: String,
    val earlyRiser: String,
    val daytime: String,
    val evening: String,
    val explorer: String,
    val loyal: String
)

private val tasteCardCopies = mapOf(
    "en" to TasteCardCopy("Your sound", "Night owl", "Early riser", "Daytime listener", "Evening listener", "Always chasing new artists.", "Faithful to the artists you love."),
    "it" to TasteCardCopy("Il tuo suono", "Nottambulo", "Mattiniero", "Ascolto diurno", "Ascolto serale", "Sempre a caccia di nuovi artisti.", "Fedele agli artisti che ami."),
    "es" to TasteCardCopy("Tu sonido", "Ave nocturna", "Madrugador", "Oyente de día", "Oyente de tarde", "Siempre buscando nuevos artistas.", "Fiel a los artistas que amas."),
    "fr" to TasteCardCopy("Ton son", "Oiseau de nuit", "Lève-tôt", "Écoute de jour", "Écoute du soir", "Toujours à la recherche de nouveaux artistes.", "Fidèle aux artistes que tu aimes."),
    "de" to TasteCardCopy("Dein Sound", "Nachteule", "Frühaufsteher", "Tageshörer", "Abendhörer", "Immer auf der Suche nach neuen Artists.", "Treu zu den Artists, die du liebst."),
    "pt" to TasteCardCopy("O teu som", "Ave noturna", "Madrugador", "Ouvinte de dia", "Ouvinte ao fim do dia", "Sempre à procura de novos artistas.", "Fiel aos artistas que adoras."),
    "pt-BR" to TasteCardCopy("Seu som", "Coruja da noite", "Madrugador", "Ouvinte diurno", "Ouvinte do entardecer", "Sempre em busca de novos artistas.", "Fiel aos artistas que você ama."),
    "nl" to TasteCardCopy("Jouw sound", "Nachtuil", "Vroege vogel", "Dagluisteraar", "Avondluisteraar", "Altijd op zoek naar nieuwe artiesten.", "Trouw aan de artiesten van wie je houdt."),
    "pl" to TasteCardCopy("Twoje brzmienie", "Nocny marek", "Ranny ptaszek", "Słuchacz dzienny", "Słuchacz wieczorny", "Wciąż szukasz nowych artystów.", "Wierny artystom, których kochasz."),
    "ro" to TasteCardCopy("Sunetul tău", "Pasăre de noapte", "Matinal", "Ascultător de zi", "Ascultător de seară", "Mereu în căutarea unor artiști noi.", "Fidel artiștilor pe care îi iubești."),
    "el" to TasteCardCopy("Ο ήχος σου", "Νυχτοπούλι", "Πρωινός τύπος", "Ακροατής της ημέρας", "Ακροατής του βραδιού", "Πάντα σε αναζήτηση νέων καλλιτεχνών.", "Πιστός στους καλλιτέχνες που αγαπάς."),
    "sv" to TasteCardCopy("Ditt sound", "Nattuggla", "Morgonpigg", "Daglyssnare", "Kvällslyssnare", "Alltid på jakt efter nya artister.", "Trogen artisterna du älskar."),
    "da" to TasteCardCopy("Din lyd", "Natteravn", "Morgenfrisk", "Daglytter", "Aftenlytter", "Altid på jagt efter nye kunstnere.", "Tro mod de kunstnere, du elsker."),
    "cs" to TasteCardCopy("Tvůj zvuk", "Noční sova", "Ranní ptáče", "Denní posluchač", "Večerní posluchač", "Pořád hledáš nové interprety.", "Věrný interpretům, které miluješ."),
    "sk" to TasteCardCopy("Tvoj zvuk", "Nočná sova", "Ranné vtáča", "Denný poslucháč", "Večerný poslucháč", "Stále hľadáš nových interpretov.", "Verný interpretom, ktorých miluješ."),
    "hr" to TasteCardCopy("Tvoj zvuk", "Noćna ptica", "Ranoranilac", "Dnevni slušatelj", "Večernji slušatelj", "Uvijek u potrazi za novim izvođačima.", "Vjeran izvođačima koje voliš."),
    "sr" to TasteCardCopy("Твој звук", "Ноћна птица", "Раноранилац", "Дневни слушалац", "Вечерњи слушалац", "Увек у потрази за новим извођачима.", "Веран извођачима које волиш."),
    "sq" to TasteCardCopy("Tingulli yt", "Zog nate", "Mëngjesor", "Dëgjues ditor", "Dëgjues i mbrëmjes", "Gjithmonë në kërkim të artistëve të rinj.", "Besnik ndaj artistëve që do."),
    "bg" to TasteCardCopy("Твоят звук", "Нощна птица", "Ранобудник", "Дневен слушател", "Вечерен слушател", "Винаги търсиш нови изпълнители.", "Верен на изпълнителите, които обичаш."),
    "hu" to TasteCardCopy("A te hangzásod", "Éjszakai bagoly", "Korán kelő", "Nappali hallgató", "Esti hallgató", "Mindig új előadókat keresel.", "Hű maradsz a kedvenc előadóidhoz."),
    "fi" to TasteCardCopy("Sinun soundisi", "Yökyöpeli", "Aamuvirkku", "Päiväkuuntelija", "Iltakuuntelija", "Aina uusien artistien perässä.", "Uskollinen rakastamillesi artisteille."),
    "et" to TasteCardCopy("Sinu kõla", "Öökull", "Varajane ärkaja", "Päevane kuulaja", "Õhtune kuulaja", "Alati uute artistide jahil.", "Truu artistidele, keda armastad."),
    "nb" to TasteCardCopy("Din lyd", "Nattugle", "Morgenfugl", "Daglytter", "Kveldslytter", "Alltid på jakt etter nye artister.", "Trofast mot artistene du elsker."),
    "ca" to TasteCardCopy("El teu so", "Mussol nocturn", "Matiner", "Oient de dia", "Oient de vespre", "Sempre a la recerca de nous artistes.", "Fidel als artistes que estimes."),
    "uk" to TasteCardCopy("Твій звук", "Нічна сова", "Ранній птах", "Денний слухач", "Вечірній слухач", "Завжди в пошуку нових виконавців.", "Вірний виконавцям, яких любиш."),
    "ru" to TasteCardCopy("Твой звук", "Сова", "Жаворонок", "Дневной слушатель", "Вечерний слушатель", "Всегда в поиске новых исполнителей.", "Верен исполнителям, которых любишь."),
    "kk" to TasteCardCopy("Сенің дыбысың", "Түнгі үкі", "Ерте тұратын", "Күндізгі тыңдаушы", "Кешкі тыңдаушы", "Әрдайым жаңа орындаушыларды іздейсің.", "Сүйікті орындаушыларыңа адалсың."),
    "tr" to TasteCardCopy("Senin sesin", "Gece kuşu", "Erkenci", "Gündüz dinleyicisi", "Akşam dinleyicisi", "Her zaman yeni sanatçıların peşinde.", "Sevdiğin sanatçılara sadık."),
    "ar" to TasteCardCopy("صوتك", "كائن ليلي", "مبكر الاستيقاظ", "مستمع نهاري", "مستمع مسائي", "تبحث دائمًا عن فنانين جدد.", "وفيّ للفنانين الذين تحبهم."),
    "fa" to TasteCardCopy("صدای تو", "شب‌زنده‌دار", "سحرخیز", "شنونده روز", "شنونده عصر", "همیشه در جست‌وجوی هنرمندان تازه.", "وفادار به هنرمندانی که دوستشان داری."),
    "zh" to TasteCardCopy("你的声音", "夜猫子", "早起鸟", "白天聆听者", "傍晚聆听者", "总在寻找新艺人。", "忠于你喜爱的艺人。"),
    "zh-Hant" to TasteCardCopy("你的聲音", "夜貓子", "早起鳥", "白天聆聽者", "傍晚聆聽者", "總在尋找新藝人。", "忠於你喜愛的藝人。"),
    "ja" to TasteCardCopy("あなたのサウンド", "夜型リスナー", "朝型リスナー", "昼のリスナー", "夕方のリスナー", "いつも新しいアーティストを探しています。", "好きなアーティストに一途です。"),
    "ko" to TasteCardCopy("나의 사운드", "올빼미형", "아침형", "낮 리스너", "저녁 리스너", "늘 새로운 아티스트를 찾고 있어요.", "좋아하는 아티스트에게 한결같아요."),
    "hi" to TasteCardCopy("आपका साउंड", "रात का पंछी", "सुबह का पंछी", "दिन के श्रोता", "शाम के श्रोता", "हमेशा नए कलाकारों की तलाश में।", "अपने पसंदीदा कलाकारों के प्रति वफ़ादार।"),
    "bn" to TasteCardCopy("আপনার সাউন্ড", "রাতজাগা পাখি", "ভোরের পাখি", "দিনের শ্রোতা", "সন্ধ্যার শ্রোতা", "সবসময় নতুন শিল্পীর খোঁজে।", "প্রিয় শিল্পীদের প্রতি বিশ্বস্ত।"),
    "id" to TasteCardCopy("Suaramu", "Si burung hantu", "Si bangun pagi", "Pendengar siang", "Pendengar sore", "Selalu mencari artis baru.", "Setia pada artis yang kamu cintai."),
    "ms" to TasteCardCopy("Bunyi anda", "Burung hantu malam", "Si bangun awal", "Pendengar siang", "Pendengar petang", "Sentiasa mencari artis baharu.", "Setia kepada artis yang anda sayangi."),
    "vi" to TasteCardCopy("Âm thanh của bạn", "Cú đêm", "Người dậy sớm", "Người nghe ban ngày", "Người nghe buổi tối", "Luôn tìm kiếm nghệ sĩ mới.", "Trung thành với nghệ sĩ bạn yêu."),
    "th" to TasteCardCopy("เสียงของคุณ", "นกฮูกราตรี", "คนตื่นเช้า", "นักฟังยามกลางวัน", "นักฟังยามเย็น", "ตามหาศิลปินใหม่อยู่เสมอ", "ภักดีต่อศิลปินที่คุณรัก"),
    "fil" to TasteCardCopy("Ang tunog mo", "Kuwago ng gabi", "Maagang gumising", "Tagapakinig sa araw", "Tagapakinig sa hapon", "Laging naghahanap ng bagong artist.", "Tapat sa mga artist na mahal mo."),
    "he" to TasteCardCopy("הסאונד שלך", "ינשוף לילה", "משכים קום", "מאזין יום", "מאזין ערב", "תמיד בחיפוש אחרי אמנים חדשים.", "נאמן לאמנים שאתה אוהב.")
)

internal fun tasteCardCopy(code: String): TasteCardCopy = localizedValueOrEnglish(tasteCardCopies, code)

internal fun tasteCardLocalizationCodes(): Set<String> = tasteCardCopies.keys
