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
            "pt" -> listOf("Anitta", "Matuê", "Luísa Sonza", "Veigh", "top hits Brasil", "funk brasileiro", "dirigir à noite", "gym bass")
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
            "pt" -> listOf("Anitta", "Matuê", "Luísa Sonza", "Veigh", "Luan Santana", "Henrique & Juliano", "MC Ryan SP", "WIU", "Marília Mendonça", "Jorge & Mateus")
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
                languageCode = "it",
                hl = "it",
                gl = "IT",
                chartRegionId = "it",
                chartCountry = "it",
                quick = "Scelte rapide",
                local = "Italia nella tua orbita",
                energy = "Energia immediata",
                homeQueries = listOf("top hits italia 2026", "canzoni italiane 2026", "rap italiano 2026", "pop italiano 2026"),
                tasteQueries = mapOf(
                    "hits" to "top hits italia 2026",
                    "rap" to "rap trap italiano 2026",
                    "italiana" to "canzoni italiane 2026",
                    "pop" to "pop italiano 2026",
                    "gym" to "musica palestra rap workout 2026",
                    "chill" to "musica chill italiana relax",
                    "focus" to "musica focus concentrazione deep",
                    "sad" to "canzoni tristi malinconia italiane",
                    "party" to "hit festa dance italia 2026",
                    "rock" to "rock italiano hits",
                    "electro" to "musica elettronica edm 2026",
                    "rnb" to "rnb soul hits 2026"
                ),
                moodQueries = mapOf(
                    "hits" to "top hits italia 2026",
                    "gym" to "palestra workout rap trap hype",
                    "chill" to "musica chill relax italiana",
                    "focus" to "focus deep concentration music",
                    "italia" to "canzoni italiane 2026",
                    "party" to "party dance hits italia 2026",
                    "drive" to "musica da viaggio in auto notte",
                    "sad" to "canzoni tristi malinconia italiane"
                )
            )
            "es" -> locale(
                languageCode = "es",
                hl = "es",
                gl = "ES",
                chartRegionId = "es",
                chartCountry = "es",
                quick = "Selecciones rápidas",
                local = "España en tu órbita",
                energy = "Energía inmediata",
                homeQueries = listOf("éxitos España 2026", "pop español 2026", "reggaeton latino 2026", "música latina nueva 2026"),
                tasteQueries = mapOf(
                    "hits" to "éxitos España 2026",
                    "rap" to "rap trap español 2026",
                    "italiana" to "música española popular 2026",
                    "pop" to "pop español 2026",
                    "gym" to "música para gimnasio reggaeton workout",
                    "chill" to "música chill española relax",
                    "focus" to "música para concentrarse focus",
                    "sad" to "canciones tristes españolas",
                    "party" to "fiesta reggaeton dance hits 2026",
                    "rock" to "rock español hits",
                    "electro" to "música electrónica edm 2026",
                    "rnb" to "rnb latino soul hits"
                ),
                moodQueries = mapOf(
                    "hits" to "éxitos España 2026",
                    "gym" to "música gimnasio reggaeton workout hype",
                    "chill" to "música chill española relax",
                    "focus" to "música focus concentración",
                    "italia" to "música española popular 2026",
                    "party" to "fiesta reggaeton dance hits 2026",
                    "drive" to "música para conducir de noche español",
                    "sad" to "canciones tristes españolas"
                )
            )
            "fr" -> locale(
                languageCode = "fr",
                hl = "fr",
                gl = "FR",
                chartRegionId = "fr",
                chartCountry = "fr",
                quick = "Sélections rapides",
                local = "France dans ton orbite",
                energy = "Énergie immédiate",
                homeQueries = listOf("top hits France 2026", "rap français 2026", "pop française 2026", "chansons françaises 2026"),
                tasteQueries = mapOf(
                    "hits" to "top hits France 2026",
                    "rap" to "rap français 2026",
                    "italiana" to "chansons françaises 2026",
                    "pop" to "pop française 2026",
                    "gym" to "musique sport workout rap français",
                    "chill" to "musique chill française relax",
                    "focus" to "musique concentration focus",
                    "sad" to "chansons tristes françaises",
                    "party" to "soirée dance hits France 2026",
                    "rock" to "rock français hits",
                    "electro" to "musique électronique edm 2026",
                    "rnb" to "rnb soul français hits"
                ),
                moodQueries = mapOf(
                    "hits" to "top hits France 2026",
                    "gym" to "musique sport workout rap hype",
                    "chill" to "musique chill française relax",
                    "focus" to "musique concentration focus",
                    "italia" to "chansons françaises 2026",
                    "party" to "soirée dance hits France 2026",
                    "drive" to "musique pour conduire nuit français",
                    "sad" to "chansons tristes françaises"
                )
            )
            "de" -> locale(
                languageCode = "de",
                hl = "de",
                gl = "DE",
                chartRegionId = "de",
                chartCountry = "de",
                quick = "Schnellauswahl",
                local = "Deutschland in deiner Umlaufbahn",
                energy = "Sofortige Energie",
                homeQueries = listOf("top hits Deutschland 2026", "deutschrap 2026", "deutsche pop hits 2026", "german top songs 2026"),
                tasteQueries = mapOf(
                    "hits" to "top hits Deutschland 2026",
                    "rap" to "deutschrap trap 2026",
                    "italiana" to "deutsche musik hits 2026",
                    "pop" to "deutsche pop hits 2026",
                    "gym" to "gym workout deutschrap hype",
                    "chill" to "chill deutsche musik relax",
                    "focus" to "musik zum konzentrieren focus",
                    "sad" to "traurige deutsche lieder",
                    "party" to "party dance hits Deutschland 2026",
                    "rock" to "deutscher rock hits",
                    "electro" to "elektronische musik edm 2026",
                    "rnb" to "rnb soul hits Deutschland"
                ),
                moodQueries = mapOf(
                    "hits" to "top hits Deutschland 2026",
                    "gym" to "gym workout deutschrap hype",
                    "chill" to "chill deutsche musik relax",
                    "focus" to "musik zum konzentrieren focus",
                    "italia" to "deutsche musik hits 2026",
                    "party" to "party dance hits Deutschland 2026",
                    "drive" to "musik zum autofahren nacht deutsch",
                    "sad" to "traurige deutsche lieder"
                )
            )
            "pt" -> locale(
                languageCode = "pt",
                hl = "pt",
                gl = "BR",
                chartRegionId = "br",
                chartCountry = "br",
                quick = "Escolhas rápidas",
                local = "Brasil na sua órbita",
                energy = "Energia imediata",
                homeQueries = listOf("top hits Brasil 2026", "funk brasileiro 2026", "sertanejo 2026", "pop brasileiro 2026"),
                tasteQueries = mapOf(
                    "hits" to "top hits Brasil 2026",
                    "rap" to "rap trap brasileiro 2026",
                    "italiana" to "música brasileira 2026",
                    "pop" to "pop brasileiro 2026",
                    "gym" to "música academia funk workout",
                    "chill" to "música chill brasileira relax",
                    "focus" to "música para foco concentração",
                    "sad" to "músicas tristes brasileiras",
                    "party" to "festa funk dance hits 2026",
                    "rock" to "rock brasileiro hits",
                    "electro" to "música eletrônica edm 2026",
                    "rnb" to "rnb soul brasil hits"
                ),
                moodQueries = mapOf(
                    "hits" to "top hits Brasil 2026",
                    "gym" to "música academia funk workout hype",
                    "chill" to "música chill brasileira relax",
                    "focus" to "música foco concentração",
                    "italia" to "música brasileira 2026",
                    "party" to "festa funk dance hits 2026",
                    "drive" to "música para dirigir à noite brasil",
                    "sad" to "músicas tristes brasileiras"
                )
            )
            "nl" -> compact(
                languageCode = "nl",
                hl = "nl",
                gl = "NL",
                chartRegionId = "nl",
                chartCountry = "nl",
                quick = "Snelle keuzes",
                local = "Nederland in je orbit",
                energy = "Directe energie",
                localMusic = "Nederlandse muziek hits 2026",
                rap = "nederlandse rap 2026",
                pop = "nederlandse pop hits 2026",
                party = "party dance hits Nederland 2026",
                sad = "verdrietige nederlandse liedjes"
            )
            "pl" -> compact(
                languageCode = "pl",
                hl = "pl",
                gl = "PL",
                chartRegionId = "pl",
                chartCountry = "pl",
                quick = "Szybkie wybory",
                local = "Polska w twojej orbicie",
                energy = "Natychmiastowa energia",
                localMusic = "polskie hity 2026",
                rap = "polski rap 2026",
                pop = "polski pop 2026",
                party = "impreza dance hity Polska 2026",
                sad = "smutne polskie piosenki"
            )
            "ro" -> compact(
                languageCode = "ro",
                hl = "ro",
                gl = "RO",
                chartRegionId = "ro",
                chartCountry = "ro",
                quick = "Alegeri rapide",
                local = "România în orbita ta",
                energy = "Energie instantanee",
                localMusic = "hituri România 2026",
                rap = "rap trap românesc 2026",
                pop = "pop românesc 2026",
                party = "petrecere dance hituri România 2026",
                sad = "melodii triste românești"
            )
            "el" -> compact(
                languageCode = "el",
                hl = "el",
                gl = "GR",
                chartRegionId = "gr",
                chartCountry = "gr",
                quick = "Γρήγορες επιλογές",
                local = "Η Ελλάδα στην τροχιά σου",
                energy = "Άμεση ενέργεια",
                localMusic = "ελληνικά hits 2026",
                rap = "ελληνικό rap trap 2026",
                pop = "ελληνική pop 2026",
                party = "party dance hits Ελλάδα 2026",
                sad = "λυπημένα ελληνικά τραγούδια"
            )
            "sv" -> compact(
                languageCode = "sv",
                hl = "sv",
                gl = "SE",
                chartRegionId = "se",
                chartCountry = "se",
                quick = "Snabba val",
                local = "Sverige i din omloppsbana",
                energy = "Direkt energi",
                localMusic = "svenska hits 2026",
                rap = "svensk rap 2026",
                pop = "svensk pop 2026",
                party = "party dance hits Sverige 2026",
                sad = "sorgliga svenska låtar"
            )
            "da" -> compact(
                languageCode = "da",
                hl = "da",
                gl = "DK",
                chartRegionId = "dk",
                chartCountry = "dk",
                quick = "Hurtige valg",
                local = "Danmark i din bane",
                energy = "Øjeblikkelig energi",
                localMusic = "danske hits 2026",
                rap = "dansk rap 2026",
                pop = "dansk pop 2026",
                party = "fest dance hits Danmark 2026",
                sad = "triste danske sange"
            )
            "cs" -> compact(
                languageCode = "cs",
                hl = "cs",
                gl = "CZ",
                chartRegionId = "cz",
                chartCountry = "cz",
                quick = "Rychlé volby",
                local = "Česko ve tvé orbitě",
                energy = "Okamžitá energie",
                localMusic = "české hity 2026",
                rap = "český rap 2026",
                pop = "český pop 2026",
                party = "party dance hity Česko 2026",
                sad = "smutné české písně"
            )
            "uk" -> compact(
                languageCode = "uk",
                hl = "uk",
                gl = "UA",
                chartRegionId = "ua",
                chartCountry = "ua",
                quick = "Швидкий вибір",
                local = "Україна у твоїй орбіті",
                energy = "Миттєва енергія",
                localMusic = "українські хіти 2026",
                rap = "український реп 2026",
                pop = "українська поп музика 2026",
                party = "вечірка dance hits Україна 2026",
                sad = "сумні українські пісні"
            )
            "ru" -> compact(
                languageCode = "ru",
                hl = "ru",
                gl = "RU",
                chartRegionId = "ru",
                chartCountry = "ru",
                quick = "Быстрый выбор",
                local = "Россия в твоей орбите",
                energy = "Мгновенная энергия",
                localMusic = "русские хиты 2026",
                rap = "русский рэп 2026",
                pop = "русская поп-музыка 2026",
                party = "танцевальные хиты Россия 2026",
                sad = "грустные русские песни"
            )
            "tr" -> compact(
                languageCode = "tr",
                hl = "tr",
                gl = "TR",
                chartRegionId = "tr",
                chartCountry = "tr",
                quick = "Hızlı seçimler",
                local = "Türkiye yörüngende",
                energy = "Anında enerji",
                localMusic = "Türkçe hitler 2026",
                rap = "Türkçe rap 2026",
                pop = "Türkçe pop 2026",
                party = "Türkiye parti ve dans hitleri 2026",
                sad = "hüzünlü Türkçe şarkılar"
            )
            "ar" -> compact(
                languageCode = "ar",
                hl = "ar",
                gl = "SA",
                chartRegionId = "sa",
                chartCountry = "sa",
                quick = "اختيارات سريعة",
                local = "العالم العربي في مدارك",
                energy = "طاقة فورية",
                localMusic = "أغاني عربية جديدة 2026",
                rap = "راب عربي جديد 2026",
                pop = "بوب عربي 2026",
                party = "أغاني حفلات عربية 2026",
                sad = "أغاني عربية حزينة"
            )
            "zh" -> compact(
                languageCode = "zh",
                hl = "zh-CN",
                gl = "CN",
                chartRegionId = "cn",
                chartCountry = "cn",
                quick = "快捷精选",
                local = "华语音乐进入你的星轨",
                energy = "即刻能量",
                localMusic = "2026 华语热歌",
                rap = "2026 中文说唱",
                pop = "2026 华语流行",
                party = "2026 华语派对舞曲",
                sad = "华语伤感歌曲"
            )
            "ja" -> compact(
                languageCode = "ja",
                hl = "ja",
                gl = "JP",
                chartRegionId = "jp",
                chartCountry = "jp",
                quick = "クイックピック",
                local = "あなたのオービットにある日本の音楽",
                energy = "即効エネルギー",
                localMusic = "2026 邦楽ヒット",
                rap = "2026 日本語ラップ",
                pop = "2026 J-POP ヒット",
                party = "2026 日本のパーティーソング",
                sad = "日本の切ない曲"
            )
            "ko" -> compact(
                languageCode = "ko",
                hl = "ko",
                gl = "KR",
                chartRegionId = "kr",
                chartCountry = "kr",
                quick = "빠른 추천",
                local = "나의 오빗 속 한국 음악",
                energy = "즉시 충전",
                localMusic = "2026 국내 인기곡",
                rap = "2026 한국 힙합",
                pop = "2026 K-POP 인기곡",
                party = "2026 한국 파티 음악",
                sad = "한국 발라드 슬픈 노래"
            )
            "hi" -> compact(
                languageCode = "hi",
                hl = "hi",
                gl = "IN",
                chartRegionId = "in",
                chartCountry = "in",
                quick = "त्वरित सुझाव",
                local = "आपकी ऑर्बिट में भारत",
                energy = "तुरंत ऊर्जा",
                localMusic = "2026 भारतीय हिट गाने",
                rap = "2026 हिंदी रैप",
                pop = "2026 हिंदी पॉप",
                party = "2026 भारतीय पार्टी गाने",
                sad = "दुख भरे हिंदी गाने"
            )
            "id" -> compact(
                languageCode = "id",
                hl = "id",
                gl = "ID",
                chartRegionId = "id",
                chartCountry = "id",
                quick = "Pilihan cepat",
                local = "Indonesia di orbit Anda",
                energy = "Energi instan",
                localMusic = "lagu Indonesia terbaru 2026",
                rap = "rap Indonesia 2026",
                pop = "pop Indonesia 2026",
                party = "lagu pesta Indonesia 2026",
                sad = "lagu Indonesia sedih"
            )
            "vi" -> compact(
                languageCode = "vi",
                hl = "vi",
                gl = "VN",
                chartRegionId = "vn",
                chartCountry = "vn",
                quick = "Gợi ý nhanh",
                local = "Việt Nam trong quỹ đạo của bạn",
                energy = "Năng lượng tức thì",
                localMusic = "nhạc Việt mới 2026",
                rap = "rap Việt 2026",
                pop = "V-pop 2026",
                party = "nhạc tiệc Việt Nam 2026",
                sad = "nhạc Việt buồn"
            )
            "th" -> compact(
                languageCode = "th",
                hl = "th",
                gl = "TH",
                chartRegionId = "th",
                chartCountry = "th",
                quick = "ตัวเลือกด่วน",
                local = "ประเทศไทยในวงโคจรของคุณ",
                energy = "พลังงานทันที",
                localMusic = "เพลงไทยใหม่ 2026",
                rap = "แรปไทย 2026",
                pop = "เพลงป๊อปไทย 2026",
                party = "เพลงปาร์ตี้ไทย 2026",
                sad = "เพลงไทยเศร้า"
            )
            "fil" -> compact(
                languageCode = "fil",
                hl = "fil",
                gl = "PH",
                chartRegionId = "ph",
                chartCountry = "ph",
                quick = "Mabilis na pili",
                local = "Pilipinas sa orbit mo",
                energy = "Agarang enerhiya",
                localMusic = "mga bagong OPM hit 2026",
                rap = "Pinoy rap 2026",
                pop = "P-pop at OPM pop 2026",
                party = "mga kantang Pinoy para sa party 2026",
                sad = "mga malungkot na kantang OPM"
            )
            "he" -> compact(
                languageCode = "he",
                hl = "he",
                gl = "IL",
                chartRegionId = "il",
                chartCountry = "il",
                quick = "בחירות מהירות",
                local = "ישראל במסלול שלך",
                energy = "אנרגיה מיידית",
                localMusic = "להיטים ישראליים חדשים 2026",
                rap = "ראפ והיפ הופ ישראלי 2026",
                pop = "פופ ישראלי 2026",
                party = "מוזיקה ישראלית למסיבה 2026",
                sad = "שירים ישראליים עצובים"
            )
            else -> locale(
                languageCode = "en",
                hl = "en",
                gl = "US",
                chartRegionId = "us",
                chartCountry = "us",
                quick = "Quick picks",
                local = "English hits in your orbit",
                energy = "Instant energy",
                homeQueries = listOf("top hits 2026", "global top songs 2026", "us pop hits 2026", "new english songs 2026"),
                tasteQueries = mapOf(
                    "hits" to "top hits 2026",
                    "rap" to "rap trap hits 2026",
                    "italiana" to "english pop hits 2026",
                    "pop" to "pop hits 2026",
                    "gym" to "gym workout hype music",
                    "chill" to "chill relax music",
                    "focus" to "focus deep concentration music",
                    "sad" to "sad songs 2026",
                    "party" to "party dance hits 2026",
                    "rock" to "rock hits 2026",
                    "electro" to "electronic edm music 2026",
                    "rnb" to "rnb soul hits 2026"
                ),
                moodQueries = mapOf(
                    "hits" to "top hits 2026",
                    "gym" to "gym workout hype rap",
                    "chill" to "chill relax music",
                    "focus" to "focus deep concentration music",
                    "italia" to "english pop hits 2026",
                    "party" to "party dance hits 2026",
                    "drive" to "night drive playlist",
                    "sad" to "sad songs 2026"
                )
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
    ): LevyraContentLocale {
        return locale(
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
    }

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
    ): LevyraContentLocale {
        return LevyraContentLocale(
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
}
