package com.luc4n3x.levyra.domain

data class LevyraContentLocale(
    val languageCode: String,
    val hl: String,
    val gl: String,
    val chartRegionId: String,
    val chartCountry: String,
    val quickSectionTitle: String,
    val localSectionTitle: String,
    val energySectionTitle: String,
    val homeQueries: List<String>,
    val tasteQueries: Map<String, String>,
    val moodQueries: Map<String, String>
) {
    fun queryForTaste(id: String): String = tasteQueries[id] ?: homeQueries.firstOrNull().orEmpty()
    fun queryForMood(id: String): String = moodQueries[id] ?: homeQueries.firstOrNull().orEmpty()
}

object LevyraContentLocales {

    fun quickSearches(code: String): List<String> {
        return when (forLanguage(code).languageCode) {
            "it" -> listOf("Sfera Ebbasta", "Lazza", "Geolier", "Marracash", "top hits Italia", "rap italiano", "night drive", "gym bass")
            "es" -> listOf("Bad Bunny", "Rosalía", "Quevedo", "Aitana", "éxitos España", "reggaeton latino", "conducir de noche", "gym bass")
            "fr" -> listOf("Gazo", "Aya Nakamura", "Damso", "Ninho", "top hits France", "rap français", "conduite de nuit", "gym bass")
            "de" -> listOf("Apache 207", "RAF Camora", "Luciano", "Ayliva", "top hits Deutschland", "deutschrap", "night drive", "gym bass")
            "pt" -> listOf("Dillaz", "Bárbara Bandeira", "Diogo Piçarra", "BISPO", "hits Portugal", "rap português", "condução noturna", "música de treino")
            "nl" -> listOf("Frenna", "Suzan & Freek", "Antoon", "Boef", "Nederlandse hits", "nederlandse rap", "night drive", "gym bass")
            "pl" -> listOf("sanah", "Taco Hemingway", "Dawid Podsiadło", "Quebonafide", "polskie hity", "polski rap", "night drive", "gym bass")
            "ro" -> listOf("Inna", "The Motans", "Delia", "Carla's Dreams", "hituri România", "rap românesc", "night drive", "gym bass")
            "el" -> listOf("Konstantinos Argiros", "Eleni Foureira", "Snik", "Helena Paparizou", "ελληνικά hits", "ελληνικό rap", "night drive", "gym bass")
            "sv" -> listOf("Veronica Maggio", "Zara Larsson", "Hov1", "Miriam Bryant", "svenska hits", "svensk rap", "night drive", "gym bass")
            "da" -> listOf("Gilli", "Tobias Rahim", "MØ", "Medina", "danske hits", "dansk rap", "night drive", "gym bass")
            "cs" -> listOf("Calin", "Ewa Farna", "Ben Cristovao", "Viktor Sheen", "české hity", "český rap", "night drive", "gym bass")
            "uk" -> listOf("alyona alyona", "KALUSH", "Jerry Heil", "The Hardkiss", "українські хіти", "український реп", "нічна поїздка", "бас для тренувань")
            "ru" -> listOf("MiyaGi & Andy Panda", "Zivert", "Баста", "Клава Кока", "русские хиты", "русский рэп", "ночная поездка", "бас для тренировки")
            "tr" -> listOf("Tarkan", "Sefo", "Mabel Matiz", "Simge", "Türkçe hitler", "Türkçe rap", "gece sürüşü", "spor bas")
            "ar" -> listOf("عمرو دياب", "نانسي عجرم", "ويجز", "مروان بابلو", "أغاني عربية 2026", "راب عربي", "موسيقى قيادة ليلية", "موسيقى حماس للتمرين")
            "zh" -> listOf("周杰伦", "邓紫棋", "薛之谦", "林俊杰", "2026 华语热歌", "中文说唱", "夜间驾驶歌单", "健身音乐")
            "ja" -> listOf("YOASOBI", "Ado", "Official髭男dism", "Mrs. GREEN APPLE", "2026 邦楽ヒット", "日本語ラップ", "夜のドライブ", "ワークアウト音楽")
            "ko" -> listOf("BTS", "BLACKPINK", "NewJeans", "IVE", "2026 국내 인기곡", "한국 힙합", "야간 드라이브", "운동 음악")
            "hi" -> listOf("Arijit Singh", "Shreya Ghoshal", "Diljit Dosanjh", "A.R. Rahman", "2026 भारतीय हिट", "हिंदी रैप", "नाइट ड्राइव", "वर्कआउट संगीत")
            "id" -> listOf("Tulus", "Mahalini", "Hindia", "NIKI", "lagu Indonesia 2026", "rap Indonesia", "musik berkendara malam", "musik olahraga")
            "vi" -> listOf("Sơn Tùng M-TP", "Mỹ Tâm", "Đen Vâu", "HIEUTHUHAI", "nhạc Việt 2026", "rap Việt", "nhạc lái xe ban đêm", "nhạc tập luyện")
            "th" -> listOf("พีพี กฤษฏ์", "บิวกิ้น", "Tilly Birds", "Three Man Down", "เพลงไทย 2026", "แรปไทย", "เพลงขับรถกลางคืน", "เพลงออกกำลังกาย")
            "fil" -> listOf("Cup of Joe", "BINI", "SB19", "Ben&Ben", "OPM hits 2026", "Pinoy rap", "kantang pang-night drive", "musikang pang-workout")
            "he" -> listOf("עומר אדם", "נועה קירל", "אושר כהן", "עדן חסון", "להיטים ישראליים 2026", "היפ הופ ישראלי", "מוזיקה לנסיעה בלילה", "מוזיקה לאימון")
            else -> listOf("The Weeknd", "Drake", "Taylor Swift", "Billie Eilish", "top hits", "rap hits", "night drive", "gym bass")
        }
    }

    fun artistSuggestions(code: String): List<String> {
        return when (forLanguage(code).languageCode) {
            "it" -> listOf("Sfera Ebbasta", "Lazza", "Geolier", "Marracash", "Ultimo", "Annalisa", "Tedua", "Ghali", "Madame", "Capo Plaza")
            "es" -> listOf("Bad Bunny", "Rosalía", "Quevedo", "Aitana", "Feid", "Karol G", "Rauw Alejandro", "Myke Towers", "Mora", "Bizarrap")
            "fr" -> listOf("Gazo", "Aya Nakamura", "Damso", "Ninho", "Tiakola", "SDM", "Zola", "Dadju", "SCH", "Jul")
            "de" -> listOf("Apache 207", "RAF Camora", "Luciano", "Ayliva", "Ufo361", "Shirin David", "Kontra K", "Nina Chuba", "Ski Aggu", "Bonez MC")
            "pt" -> listOf("Dillaz", "Bárbara Bandeira", "Diogo Piçarra", "BISPO", "Carolina Deslandes", "Slow J", "Nena", "Plutónio", "Mizzy Miles", "Wet Bed Gang")
            "nl" -> listOf("Frenna", "Suzan & Freek", "Antoon", "Boef", "Roxy Dekker", "Maan", "Ronnie Flex", "Broederliefde", "S10", "Snelle")
            "pl" -> listOf("sanah", "Taco Hemingway", "Dawid Podsiadło", "Quebonafide", "PRO8L3M", "Mata", "Daria Zawiałow", "Kizo", "Oki", "Bambi")
            "ro" -> listOf("Inna", "The Motans", "Delia", "Carla's Dreams", "Irina Rimes", "Smiley", "Andra", "M.G.L.", "Theo Rose", "Ian")
            "el" -> listOf("Konstantinos Argiros", "Eleni Foureira", "Snik", "Helena Paparizou", "Sakis Rouvas", "Josephine", "Light", "Mad Clip", "Rack", "Melisses")
            "sv" -> listOf("Veronica Maggio", "Zara Larsson", "Hov1", "Miriam Bryant", "Einár", "Victor Leksell", "Benjamin Ingrosso", "Molly Sandén", "Miss Li", "Darin")
            "da" -> listOf("Gilli", "Tobias Rahim", "MØ", "Medina", "KESI", "Lamin", "Andreas Odbjerg", "Christopher", "Artigeardit", "Burhan G")
            "cs" -> listOf("Calin", "Ewa Farna", "Ben Cristovao", "Viktor Sheen", "Yzomandias", "Mirai", "Kryštof", "Pam Rabbit", "Separ", "Rytmus")
            "uk" -> listOf("alyona alyona", "KALUSH", "Jerry Heil", "The Hardkiss", "Monatik", "Dorofeeva", "Okean Elzy", "Wellboy", "Artem Pivovarov", "Kazka")
            "ru" -> listOf("MiyaGi & Andy Panda", "Zivert", "Баста", "Клава Кока", "JONY", "Мот", "ANNA ASTI", "MACAN", "Егор Крид", "Artik & Asti")
            "tr" -> listOf("Tarkan", "Sefo", "Mabel Matiz", "Simge", "Ezhel", "UZI", "Semicenk", "Edis", "Hadise", "Güneş")
            "ar" -> listOf("عمرو دياب", "نانسي عجرم", "ويجز", "مروان بابلو", "شيرين", "تامر حسني", "إليسا", "بلقيس", "Saint Levant", "DYSTINCT")
            "zh" -> listOf("周杰伦", "邓紫棋", "薛之谦", "林俊杰", "陈奕迅", "王菲", "毛不易", "张杰", "蔡依林", "告五人")
            "ja" -> listOf("YOASOBI", "Ado", "Official髭男dism", "Mrs. GREEN APPLE", "Vaundy", "米津玄師", "King Gnu", "藤井風", "back number", "あいみょん")
            "ko" -> listOf("BTS", "BLACKPINK", "NewJeans", "IVE", "aespa", "Stray Kids", "SEVENTEEN", "IU", "LE SSERAFIM", "(G)I-DLE")
            "hi" -> listOf("Arijit Singh", "Shreya Ghoshal", "A.R. Rahman", "Pritam", "Diljit Dosanjh", "Badshah", "Neha Kakkar", "Jubin Nautiyal", "Vishal-Shekhar", "AP Dhillon")
            "id" -> listOf("Tulus", "Mahalini", "Hindia", "NIKI", "Tiara Andini", "Bernadya", "Juicy Luicy", "Denny Caknan", "Pamungkas", "Lyodra")
            "vi" -> listOf("Sơn Tùng M-TP", "Mỹ Tâm", "Đen Vâu", "HIEUTHUHAI", "Hoàng Thùy Linh", "MONO", "Bích Phương", "tlinh", "Vũ.", "SOOBIN")
            "th" -> listOf("พีพี กฤษฏ์", "บิวกิ้น", "Tilly Birds", "Three Man Down", "4EVE", "MILLI", "Jeff Satur", "NONT TANONT", "Ink Waruntorn", "Slot Machine")
            "fil" -> listOf("Cup of Joe", "BINI", "SB19", "Ben&Ben", "Arthur Nery", "Dionela", "TJ Monterde", "December Avenue", "fitterkarma", "Earl Agustin")
            "he" -> listOf("עומר אדם", "נועה קירל", "אושר כהן", "עדן חסון", "נס וסטילה", "טונה", "רביב כנר", "ישי ריבו", "אנה זק", "פאר טסי")
            else -> listOf("The Weeknd", "Drake", "Taylor Swift", "Billie Eilish", "SZA", "Travis Scott", "Dua Lipa", "Post Malone", "Ariana Grande", "Kendrick Lamar")
        }
    }

    fun isArtistSuggestionForLanguage(artistName: String, code: String): Boolean {
        val clean = artistName.trim()
        val fullIdentity = artistIdentityKey(clean)
        val primaryIdentity = artistIdentityKey(primaryArtistSegment(clean).ifBlank { clean })
        if (fullIdentity.isBlank() && primaryIdentity.isBlank()) return false
        return artistSuggestions(code).any { suggestion ->
            val suggestionIdentity = artistIdentityKey(suggestion)
            suggestionIdentity == fullIdentity || suggestionIdentity == primaryIdentity
        }
    }

    fun artistSuggestionsTitle(code: String): String {
        return when (forLanguage(code).languageCode) {
            "it" -> "Esplora artisti"
            "es" -> "Explora artistas"
            "fr" -> "Explore les artistes"
            "de" -> "Künstler entdecken"
            "pt" -> "Explorar artistas"
            "nl" -> "Ontdek artiesten"
            "pl" -> "Odkrywaj artystów"
            "ro" -> "Explorează artiști"
            "el" -> "Εξερεύνηση καλλιτεχνών"
            "sv" -> "Utforska artister"
            "da" -> "Udforsk kunstnere"
            "cs" -> "Objevuj interprety"
            "uk" -> "Досліджуй артистів"
            "ru" -> "Открой для себя исполнителей"
            "tr" -> "Sanatçıları keşfet"
            "ar" -> "استكشف الفنانين"
            "zh" -> "探索歌手"
            "ja" -> "アーティストを探す"
            "ko" -> "아티스트 둘러보기"
            "hi" -> "कलाकार खोजें"
            "id" -> "Jelajahi artis"
            "vi" -> "Khám phá nghệ sĩ"
            "th" -> "สำรวจศิลปิน"
            "fil" -> "Tuklasin ang mga artist"
            "he" -> "גילוי אמנים"
            else -> "Explore artists"
        }
    }

    fun searchSuggestionsTitle(code: String): String {
        return when (forLanguage(code).languageCode) {
            "it" -> "Suggerimenti"
            "es" -> "Sugerencias"
            "fr" -> "Suggestions"
            "de" -> "Vorschläge"
            "pt" -> "Sugestões"
            "nl" -> "Suggesties"
            "pl" -> "Sugestie"
            "ro" -> "Sugestii"
            "el" -> "Προτάσεις"
            "sv" -> "Förslag"
            "da" -> "Forslag"
            "cs" -> "Návrhy"
            "uk" -> "Пропозиції"
            "ru" -> "Рекомендации"
            "tr" -> "Öneriler"
            "ar" -> "اقتراحات"
            "zh" -> "推荐"
            "ja" -> "候補"
            "ko" -> "추천"
            "hi" -> "सुझाव"
            "id" -> "Saran"
            "vi" -> "Gợi ý"
            "th" -> "คำแนะนำ"
            "fil" -> "Mga mungkahi"
            "he" -> "הצעות"
            else -> "Suggestions"
        }
    }

    fun forLanguage(code: String): LevyraContentLocale {
        return when (LevyraLanguageCatalog.normalize(code)) {
            "it" -> locale(
                languageCode = "it", hl = "it", gl = "IT", chartRegionId = "it", chartCountry = "it",
                quick = "Scelte rapide", local = "Italia nella tua orbita", energy = "Energia immediata",
                homeQueries = listOf("top hits italia 2026", "canzoni italiane 2026", "rap italiano 2026", "pop italiano 2026"),
                tasteQueries = mapOf("hits" to "top hits italia 2026", "rap" to "rap trap italiano 2026", "italiana" to "canzoni italiane 2026", "pop" to "pop italiano 2026", "gym" to "musica palestra rap workout 2026", "chill" to "musica chill italiana relax", "focus" to "musica focus concentrazione deep", "sad" to "canzoni tristi malinconia italiane", "party" to "hit festa dance italia 2026", "rock" to "rock italiano hits", "electro" to "musica elettronica edm 2026", "rnb" to "rnb soul hits 2026"),
                moodQueries = mapOf("hits" to "top hits italia 2026", "gym" to "palestra workout rap trap hype", "chill" to "musica chill relax italiana", "focus" to "focus deep concentration music", "italia" to "canzoni italiane 2026", "party" to "party dance hits italia 2026", "drive" to "musica da viaggio in auto notte", "sad" to "canzoni tristi malinconia italiane")
            )
            "es" -> locale(
                languageCode = "es", hl = "es", gl = "ES", chartRegionId = "es", chartCountry = "es",
                quick = "Selecciones rápidas", local = "España en tu órbita", energy = "Energía inmediata",
                homeQueries = listOf("éxitos España 2026", "pop español 2026", "reggaeton latino 2026", "música latina nueva 2026"),
                tasteQueries = mapOf("hits" to "éxitos España 2026", "rap" to "rap trap español 2026", "italiana" to "música española popular 2026", "pop" to "pop español 2026", "gym" to "música para gimnasio reggaeton workout", "chill" to "música chill española relax", "focus" to "música para concentrarse focus", "sad" to "canciones tristes españolas", "party" to "fiesta reggaeton dance hits 2026", "rock" to "rock español hits", "electro" to "música electrónica edm 2026", "rnb" to "rnb latino soul hits"),
                moodQueries = mapOf("hits" to "éxitos España 2026", "gym" to "música gimnasio reggaeton workout hype", "chill" to "música chill española relax", "focus" to "música focus concentración", "italia" to "música española popular 2026", "party" to "fiesta reggaeton dance hits 2026", "drive" to "música para conducir de noche español", "sad" to "canciones tristes españolas")
            )
            "fr" -> locale(
                languageCode = "fr", hl = "fr", gl = "FR", chartRegionId = "fr", chartCountry = "fr",
                quick = "Sélections rapides", local = "France dans ton orbite", energy = "Énergie immédiate",
                homeQueries = listOf("top hits France 2026", "rap français 2026", "pop française 2026", "chansons françaises 2026"),
                tasteQueries = mapOf("hits" to "top hits France 2026", "rap" to "rap français 2026", "italiana" to "chansons françaises 2026", "pop" to "pop française 2026", "gym" to "musique sport workout rap français", "chill" to "musique chill française relax", "focus" to "musique concentration focus", "sad" to "chansons tristes françaises", "party" to "soirée dance hits France 2026", "rock" to "rock français hits", "electro" to "musique électronique edm 2026", "rnb" to "rnb soul français hits"),
                moodQueries = mapOf("hits" to "top hits France 2026", "gym" to "musique sport workout rap hype", "chill" to "musique chill française relax", "focus" to "musique concentration focus", "italia" to "chansons françaises 2026", "party" to "soirée dance hits France 2026", "drive" to "musique pour conduire nuit français", "sad" to "chansons tristes françaises")
            )
            "de" -> locale(
                languageCode = "de", hl = "de", gl = "DE", chartRegionId = "de", chartCountry = "de",
                quick = "Schnellauswahl", local = "Deutschland in deiner Umlaufbahn", energy = "Sofortige Energie",
                homeQueries = listOf("top hits Deutschland 2026", "deutschrap 2026", "deutsche pop hits 2026", "german top songs 2026"),
                tasteQueries = mapOf("hits" to "top hits Deutschland 2026", "rap" to "deutschrap trap 2026", "italiana" to "deutsche musik hits 2026", "pop" to "deutsche pop hits 2026", "gym" to "gym workout deutschrap hype", "chill" to "chill deutsche musik relax", "focus" to "musik zum konzentrieren focus", "sad" to "traurige deutsche lieder", "party" to "party dance hits Deutschland 2026", "rock" to "deutscher rock hits", "electro" to "elektronische musik edm 2026", "rnb" to "rnb soul hits Deutschland"),
                moodQueries = mapOf("hits" to "top hits Deutschland 2026", "gym" to "gym workout deutschrap hype", "chill" to "chill deutsche musik relax", "focus" to "musik zum konzentrieren focus", "italia" to "deutsche musik hits 2026", "party" to "party dance hits Deutschland 2026", "drive" to "musik zum autofahren nacht deutsch", "sad" to "traurige deutsche lieder")
            )
            "pt" -> locale(
                languageCode = "pt", hl = "pt-PT", gl = "PT", chartRegionId = "pt", chartCountry = "pt",
                quick = "Escolhas rápidas", local = "Portugal na tua órbita", energy = "Energia imediata",
                homeQueries = listOf("hits Portugal 2026", "rap português 2026", "pop português 2026", "música portuguesa 2026"),
                tasteQueries = mapOf("hits" to "hits Portugal 2026", "rap" to "rap trap português 2026", "italiana" to "música portuguesa 2026", "pop" to "pop português 2026", "gym" to "música treino rap português", "chill" to "música portuguesa chill relax", "focus" to "música para foco concentração", "sad" to "músicas portuguesas tristes", "party" to "festa dance hits Portugal 2026", "rock" to "rock português hits", "electro" to "música eletrónica edm 2026", "rnb" to "rnb soul Portugal hits"),
                moodQueries = mapOf("hits" to "hits Portugal 2026", "gym" to "música treino rap português hype", "chill" to "música portuguesa chill relax", "focus" to "música foco concentração", "italia" to "música portuguesa 2026", "party" to "festa dance hits Portugal 2026", "drive" to "música para conduzir à noite Portugal", "sad" to "músicas portuguesas tristes")
            )
            "nl" -> compact("nl", "nl", "NL", "nl", "nl", "Snelle keuzes", "Nederland in je orbit", "Directe energie", "Nederlandse muziek hits 2026", "nederlandse rap 2026", "nederlandse pop hits 2026", "party dance hits Nederland 2026", "verdrietige nederlandse liedjes")
            "pl" -> compact("pl", "pl", "PL", "pl", "pl", "Szybkie wybory", "Polska w twojej orbicie", "Natychmiastowa energia", "polskie hity 2026", "polski rap 2026", "polski pop 2026", "impreza dance hity Polska 2026", "smutne polskie piosenki")
            "ro" -> compact("ro", "ro", "RO", "ro", "ro", "Alegeri rapide", "România în orbita ta", "Energie instantanee", "hituri România 2026", "rap trap românesc 2026", "pop românesc 2026", "petrecere dance hituri România 2026", "melodii triste românești")
            "el" -> compact("el", "el", "GR", "gr", "gr", "Γρήγορες επιλογές", "Η Ελλάδα στην τροχιά σου", "Άμεση ενέργεια", "ελληνικά hits 2026", "ελληνικό rap trap 2026", "ελληνική pop 2026", "party dance hits Ελλάδα 2026", "λυπημένα ελληνικά τραγούδια")
            "sv" -> compact("sv", "sv", "SE", "se", "se", "Snabba val", "Sverige i din omloppsbana", "Direkt energi", "svenska hits 2026", "svensk rap 2026", "svensk pop 2026", "party dance hits Sverige 2026", "sorgliga svenska låtar")
            "da" -> compact("da", "da", "DK", "dk", "dk", "Hurtige valg", "Danmark i din bane", "Øjeblikkelig energi", "danske hits 2026", "dansk rap 2026", "dansk pop 2026", "fest dance hits Danmark 2026", "triste danske sange")
            "cs" -> compact("cs", "cs", "CZ", "cz", "cz", "Rychlé volby", "Česko ve tvé orbitě", "Okamžitá energie", "české hity 2026", "český rap 2026", "český pop 2026", "party dance hity Česko 2026", "smutné české písně")
            "uk" -> compact("uk", "uk", "UA", "ua", "ua", "Швидкий вибір", "Україна у твоїй орбіті", "Миттєва енергія", "українські хіти 2026", "український реп 2026", "українська поп музика 2026", "вечірка dance hits Україна 2026", "сумні українські пісні")
            "ru" -> compact("ru", "ru", "RU", "ru", "ru", "Быстрый выбор", "Россия в твоей орбите", "Мгновенная энергия", "русские хиты 2026", "русский рэп 2026", "русская поп-музыка 2026", "танцевальные хиты Россия 2026", "грустные русские песни")
            "tr" -> compact("tr", "tr", "TR", "tr", "tr", "Hızlı seçimler", "Türkiye yörüngende", "Anında enerji", "Türkçe hitler 2026", "Türkçe rap 2026", "Türkçe pop 2026", "Türkiye parti ve dans hitleri 2026", "hüzünlü Türkçe şarkılar")
            "ar" -> compact("ar", "ar", "SA", "sa", "sa", "اختيارات سريعة", "العالم العربي في مدارك", "طاقة فورية", "أغاني عربية جديدة 2026", "راب عربي جديد 2026", "بوب عربي 2026", "أغاني حفلات عربية 2026", "أغاني عربية حزينة")
            "zh" -> compact("zh", "zh-CN", "CN", "cn", "cn", "快捷精选", "华语音乐进入你的星轨", "即刻能量", "2026 华语热歌", "2026 中文说唱", "2026 华语流行", "2026 华语派对舞曲", "华语伤感歌曲")
            "ja" -> compact("ja", "ja", "JP", "jp", "jp", "クイックピック", "あなたのオービットにある日本の音楽", "即効エネルギー", "2026 邦楽ヒット", "2026 日本語ラップ", "2026 J-POP ヒット", "2026 日本のパーティーソング", "日本の切ない曲")
            "ko" -> compact("ko", "ko", "KR", "kr", "kr", "빠른 추천", "나의 오빗 속 한국 음악", "즉시 충전", "2026 국내 인기곡", "2026 한국 힙합", "2026 K-POP 인기곡", "2026 한국 파티 음악", "한국 발라드 슬픈 노래")
            "hi" -> compact("hi", "hi", "IN", "in", "in", "त्वरित सुझाव", "आपकी ऑर्बिट में भारत", "तुरंत ऊर्जा", "2026 भारतीय हिट गाने", "2026 हिंदी रैप", "2026 हिंदी पॉप", "2026 भारतीय पार्टी गाने", "दुख भरे हिंदी गाने")
            "id" -> compact("id", "id", "ID", "id", "id", "Pilihan cepat", "Indonesia di orbit Anda", "Energi instan", "lagu Indonesia terbaru 2026", "rap Indonesia 2026", "pop Indonesia 2026", "lagu pesta Indonesia 2026", "lagu Indonesia sedih")
            "vi" -> compact("vi", "vi", "VN", "vn", "vn", "Gợi ý nhanh", "Việt Nam trong quỹ đạo của bạn", "Năng lượng tức thì", "nhạc Việt mới 2026", "rap Việt 2026", "V-pop 2026", "nhạc tiệc Việt Nam 2026", "nhạc Việt buồn")
            "th" -> compact("th", "th", "TH", "th", "th", "ตัวเลือกด่วน", "ประเทศไทยในวงโคจรของคุณ", "พลังงานทันที", "เพลงไทยใหม่ 2026", "แรปไทย 2026", "เพลงป๊อปไทย 2026", "เพลงปาร์ตี้ไทย 2026", "เพลงไทยเศร้า")
            "fil" -> compact("fil", "fil", "PH", "ph", "ph", "Mabilis na pili", "Pilipinas sa orbit mo", "Agarang enerhiya", "mga bagong OPM hit 2026", "Pinoy rap 2026", "P-pop at OPM pop 2026", "mga kantang Pinoy para sa party 2026", "mga malungkot na kantang OPM")
            "he" -> compact("he", "he", "IL", "il", "il", "בחירות מהירות", "ישראל במסלול שלך", "אנרגיה מיידית", "להיטים ישראליים חדשים 2026", "ראפ והיפ הופ ישראלי 2026", "פופ ישראלי 2026", "מוזיקה ישראלית למסיבה 2026", "שירים ישראליים עצובים")
            else -> locale(
                languageCode = "en", hl = "en", gl = "US", chartRegionId = "us", chartCountry = "us",
                quick = "Quick picks", local = "English hits in your orbit", energy = "Instant energy",
                homeQueries = listOf("top hits 2026", "global top songs 2026", "us pop hits 2026", "new english songs 2026"),
                tasteQueries = mapOf("hits" to "top hits 2026", "rap" to "rap trap hits 2026", "italiana" to "english pop hits 2026", "pop" to "pop hits 2026", "gym" to "gym workout hype music", "chill" to "chill relax music", "focus" to "focus deep concentration music", "sad" to "sad songs 2026", "party" to "party dance hits 2026", "rock" to "rock hits 2026", "electro" to "electronic edm music 2026", "rnb" to "rnb soul hits 2026"),
                moodQueries = mapOf("hits" to "top hits 2026", "gym" to "gym workout hype rap", "chill" to "chill relax music", "focus" to "focus deep concentration music", "italia" to "english pop hits 2026", "party" to "party dance hits 2026", "drive" to "night drive playlist", "sad" to "sad songs 2026")
            )
        }
    }

    private fun compact(
        languageCode: String,
        hl: String,
        gl: String,
        chartRegionId: String,
        chartCountry: String,
        quick: String,
        local: String,
        energy: String,
        localMusic: String,
        rap: String,
        pop: String,
        party: String,
        sad: String
    ): LevyraContentLocale = locale(
        languageCode = languageCode,
        hl = hl,
        gl = gl,
        chartRegionId = chartRegionId,
        chartCountry = chartCountry,
        quick = quick,
        local = local,
        energy = energy,
        homeQueries = listOf(localMusic, rap, pop, "global top hits 2026"),
        tasteQueries = mapOf(
            "hits" to localMusic,
            "rap" to rap,
            "italiana" to localMusic,
            "pop" to pop,
            "gym" to "$localMusic gym workout hype",
            "chill" to "$localMusic chill relax",
            "focus" to "focus deep concentration music",
            "sad" to sad,
            "party" to party,
            "rock" to "$localMusic rock hits",
            "electro" to "electronic edm music 2026",
            "rnb" to "$localMusic rnb soul hits"
        ),
        moodQueries = mapOf(
            "hits" to localMusic,
            "gym" to "$localMusic gym workout hype",
            "chill" to "$localMusic chill relax",
            "focus" to "focus deep concentration music",
            "italia" to localMusic,
            "party" to party,
            "drive" to "$localMusic night drive playlist",
            "sad" to sad
        )
    )

    private fun locale(
        languageCode: String,
        hl: String,
        gl: String,
        chartRegionId: String,
        chartCountry: String,
        quick: String,
        local: String,
        energy: String,
        homeQueries: List<String>,
        tasteQueries: Map<String, String>,
        moodQueries: Map<String, String>
    ): LevyraContentLocale = LevyraContentLocale(
        languageCode = languageCode,
        hl = hl,
        gl = gl,
        chartRegionId = chartRegionId,
        chartCountry = chartCountry,
        quickSectionTitle = quick,
        localSectionTitle = local,
        energySectionTitle = energy,
        homeQueries = homeQueries,
        tasteQueries = tasteQueries,
        moodQueries = moodQueries
    )
}
