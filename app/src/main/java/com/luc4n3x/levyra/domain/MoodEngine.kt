package com.luc4n3x.levyra.domain

class MoodEngine {
    val moods: List<Mood> = moodsForLanguage("it")
    val tastes: List<Taste> = tastesForLanguage("it")
    val defaultHomeQueries: List<String> = LevyraContentLocales.forLanguage("it").homeQueries

    fun moodsForLanguage(languageCode: String): List<Mood> {
        val texts = moodTexts(languageCode)
        return listOf(
            Mood(
                id = "hits",
                title = texts.getValue("hits").first,
                subtitle = texts.getValue("hits").second,
                icon = "🔥",
                energyTarget = 78,
                tags = setOf("hit", "pop", "new"),
                accentStart = 0xFFFF512F.toInt(),
                accentEnd = 0xFFDD2476.toInt()
            ),
            Mood(
                id = "gym",
                title = texts.getValue("gym").first,
                subtitle = texts.getValue("gym").second,
                icon = "🏋️",
                energyTarget = 92,
                tags = setOf("gym", "bass", "rap", "energy", "trap"),
                accentStart = 0xFF1B5CFF.toInt(),
                accentEnd = 0xFF00E5FF.toInt()
            ),
            Mood(
                id = "chill",
                title = texts.getValue("chill").first,
                subtitle = texts.getValue("chill").second,
                icon = "😌",
                energyTarget = 42,
                tags = setOf("chill", "rnb", "ambient", "night"),
                accentStart = 0xFF11998E.toInt(),
                accentEnd = 0xFF38EF7D.toInt()
            ),
            Mood(
                id = "focus",
                title = texts.getValue("focus").first,
                subtitle = texts.getValue("focus").second,
                icon = "🎧",
                energyTarget = 48,
                tags = setOf("focus", "electronic", "deep", "ambient"),
                accentStart = 0xFF6A11CB.toInt(),
                accentEnd = 0xFF2575FC.toInt()
            ),
            Mood(
                id = "italia",
                title = texts.getValue("local").first,
                subtitle = texts.getValue("local").second,
                icon = localIcon(languageCode),
                energyTarget = 70,
                tags = setOf("local", "pop", "hit"),
                accentStart = 0xFF00D4A6.toInt(),
                accentEnd = 0xFFFF3B5C.toInt()
            ),
            Mood(
                id = "party",
                title = texts.getValue("party").first,
                subtitle = texts.getValue("party").second,
                icon = "🎉",
                energyTarget = 95,
                tags = setOf("party", "dance", "hit", "energy"),
                accentStart = 0xFFFFB000.toInt(),
                accentEnd = 0xFFFF4FD8.toInt()
            ),
            Mood(
                id = "drive",
                title = texts.getValue("drive").first,
                subtitle = texts.getValue("drive").second,
                icon = "🚗",
                energyTarget = 64,
                tags = setOf("night", "chill", "pop", "rap"),
                accentStart = 0xFF8E2DE2.toInt(),
                accentEnd = 0xFF4A00E0.toInt()
            ),
            Mood(
                id = "sad",
                title = texts.getValue("sad").first,
                subtitle = texts.getValue("sad").second,
                icon = "💔",
                energyTarget = 34,
                tags = setOf("sad", "rnb", "chill"),
                accentStart = 0xFF355C7D.toInt(),
                accentEnd = 0xFFC06C84.toInt()
            )
        )
    }

    fun tastesForLanguage(languageCode: String): List<Taste> {
        val locale = LevyraContentLocales.forLanguage(languageCode)
        val labels = tasteLabels(languageCode)
        return listOf(
            Taste("hits", labels.getValue("hits"), "🔥", locale.queryForTaste("hits")),
            Taste("rap", labels.getValue("rap"), "🎤", locale.queryForTaste("rap")),
            Taste("italiana", labels.getValue("local"), localIcon(languageCode), locale.queryForTaste("italiana")),
            Taste("pop", labels.getValue("pop"), "✨", locale.queryForTaste("pop")),
            Taste("gym", labels.getValue("gym"), "🏋️", locale.queryForTaste("gym")),
            Taste("chill", labels.getValue("chill"), "😌", locale.queryForTaste("chill")),
            Taste("focus", labels.getValue("focus"), "🎧", locale.queryForTaste("focus")),
            Taste("sad", labels.getValue("sad"), "💔", locale.queryForTaste("sad")),
            Taste("party", labels.getValue("party"), "🎉", locale.queryForTaste("party")),
            Taste("rock", labels.getValue("rock"), "🎸", locale.queryForTaste("rock")),
            Taste("electro", labels.getValue("electro"), "🎛️", locale.queryForTaste("electro")),
            Taste("rnb", labels.getValue("rnb"), "🕺", locale.queryForTaste("rnb"))
        )
    }

    fun defaultHomeQueries(languageCode: String): List<String> = LevyraContentLocales.forLanguage(languageCode).homeQueries

    fun queriesForTastes(ids: Set<String>, languageCode: String): List<String> {
        val locale = LevyraContentLocales.forLanguage(languageCode)
        val selected = ids.mapNotNull { id -> locale.tasteQueries[id] }.distinct()
        return if (selected.isEmpty()) locale.homeQueries else selected
    }

    fun buildQueue(mood: Mood?, tracks: List<Track>): List<Track> {
        return tracks.sortedWith(
            compareByDescending<Track> { it.smartWeightFor(mood) }
                .thenByDescending { it.replayScore }
                .thenBy { it.title.lowercase() }
        )
    }

    fun tagQueryFor(mood: Mood, languageCode: String): String = LevyraContentLocales.forLanguage(languageCode).queryForMood(mood.id).ifBlank { mood.title }

    private fun localIcon(languageCode: String): String {
        return when (LevyraLanguageCatalog.normalize(languageCode)) {
            "it" -> "🇮🇹"
            "es" -> "🇪🇸"
            "fr" -> "🇫🇷"
            "de" -> "🇩🇪"
            "pt" -> "🇧🇷"
            "nl" -> "🇳🇱"
            "pl" -> "🇵🇱"
            "ro" -> "🇷🇴"
            "el" -> "🇬🇷"
            "sv" -> "🇸🇪"
            "da" -> "🇩🇰"
            "cs" -> "🇨🇿"
            "uk" -> "🇺🇦"
            "ru" -> "🇷🇺"
            "tr" -> "🇹🇷"
            "ar" -> "🇸🇦"
            "zh" -> "🇨🇳"
            "ja" -> "🇯🇵"
            "ko" -> "🇰🇷"
            "hi" -> "🇮🇳"
            "id" -> "🇮🇩"
            "vi" -> "🇻🇳"
            "th" -> "🇹🇭"
            "fil" -> "🇵🇭"
            "he" -> "🇮🇱"
            else -> "🇬🇧"
        }
    }

    private fun moodTexts(languageCode: String): Map<String, Pair<String, String>> {
        return when (LevyraLanguageCatalog.normalize(languageCode)) {
            "it" -> mapOf(
                "hits" to ("Hit del momento" to "le più ascoltate ora"),
                "gym" to ("Palestra" to "carica e potenza"),
                "chill" to ("Relax" to "calmo e morbido"),
                "focus" to ("Focus" to "studio e concentrazione"),
                "local" to ("Italia" to "il meglio italiano"),
                "party" to ("Festa" to "alza il volume"),
                "drive" to ("In auto" to "musica da viaggio"),
                "sad" to ("Malinconia" to "emozioni lente")
            )
            "es" -> mapOf(
                "hits" to ("Éxitos" to "lo más escuchado ahora"),
                "gym" to ("Gimnasio" to "fuerza y potencia"),
                "chill" to ("Relax" to "suave y tranquilo"),
                "focus" to ("Focus" to "estudio y concentración"),
                "local" to ("España" to "lo mejor en español"),
                "party" to ("Fiesta" to "sube el volumen"),
                "drive" to ("Conduciendo" to "música para viajar"),
                "sad" to ("Melancolía" to "emociones lentas")
            )
            "fr" -> mapOf(
                "hits" to ("Hits du moment" to "les plus écoutés"),
                "gym" to ("Sport" to "énergie et puissance"),
                "chill" to ("Relax" to "calme et doux"),
                "focus" to ("Focus" to "étude et concentration"),
                "local" to ("France" to "le meilleur en français"),
                "party" to ("Fête" to "monte le volume"),
                "drive" to ("En voiture" to "musique de route"),
                "sad" to ("Mélancolie" to "émotions lentes")
            )
            "de" -> mapOf(
                "hits" to ("Aktuelle Hits" to "jetzt meistgehört"),
                "gym" to ("Gym" to "kraft und energie"),
                "chill" to ("Relax" to "ruhig und weich"),
                "focus" to ("Focus" to "lernen und konzentrieren"),
                "local" to ("Deutschland" to "das beste auf Deutsch"),
                "party" to ("Party" to "lauter machen"),
                "drive" to ("Auto" to "musik für unterwegs"),
                "sad" to ("Melancholie" to "langsame gefühle")
            )
            "pt" -> mapOf(
                "hits" to ("Hits do momento" to "as mais ouvidas agora"),
                "gym" to ("Academia" to "energia e força"),
                "chill" to ("Relax" to "calmo e suave"),
                "focus" to ("Foco" to "estudo e concentração"),
                "local" to ("Brasil" to "o melhor em português"),
                "party" to ("Festa" to "aumenta o volume"),
                "drive" to ("No carro" to "música para viagem"),
                "sad" to ("Melancolia" to "emoções lentas")
            )
            "ar" -> mapOf(
                "hits" to ("الأكثر رواجًا" to "الأكثر استماعًا الآن"),
                "gym" to ("التمرين" to "طاقة وقوة"),
                "chill" to ("استرخاء" to "هادئ وناعم"),
                "focus" to ("تركيز" to "دراسة وتركيز"),
                "local" to ("العالم العربي" to "أفضل الموسيقى العربية"),
                "party" to ("حفلة" to "ارفع مستوى الصوت"),
                "drive" to ("قيادة" to "موسيقى للطريق"),
                "sad" to ("مشاعر" to "أغانٍ هادئة وحزينة")
            )
            "zh" -> mapOf(
                "hits" to ("热门歌曲" to "此刻播放最多"),
                "gym" to ("健身" to "能量与力量"),
                "chill" to ("放松" to "轻柔而平静"),
                "focus" to ("专注" to "学习与集中注意力"),
                "local" to ("华语" to "精选华语音乐"),
                "party" to ("派对" to "把音量调高"),
                "drive" to ("驾车" to "公路旅程音乐"),
                "sad" to ("伤感" to "缓慢流淌的情绪")
            )
            "ja" -> mapOf(
                "hits" to ("話題のヒット" to "今いちばん聴かれている曲"),
                "gym" to ("ワークアウト" to "パワーとエネルギー"),
                "chill" to ("チル" to "穏やかで心地よい音"),
                "focus" to ("集中" to "勉強と作業のために"),
                "local" to ("邦楽" to "選りすぐりの日本の音楽"),
                "party" to ("パーティー" to "音量を上げよう"),
                "drive" to ("ドライブ" to "旅に合う音楽"),
                "sad" to ("切なさ" to "ゆっくり流れる感情")
            )
            "ko" -> mapOf(
                "hits" to ("인기 히트곡" to "지금 가장 많이 듣는 음악"),
                "gym" to ("운동" to "힘과 에너지"),
                "chill" to ("칠" to "부드럽고 편안하게"),
                "focus" to ("집중" to "공부와 몰입을 위한 음악"),
                "local" to ("한국 음악" to "엄선한 국내 음악"),
                "party" to ("파티" to "볼륨을 높여 보세요"),
                "drive" to ("드라이브" to "도로 위의 음악"),
                "sad" to ("감성" to "천천히 흐르는 감정")
            )
            "hi" -> mapOf(
                "hits" to ("ट्रेंडिंग हिट्स" to "अभी सबसे ज़्यादा सुने जा रहे"),
                "gym" to ("वर्कआउट" to "ताकत और ऊर्जा"),
                "chill" to ("सुकून" to "नरम और शांत"),
                "focus" to ("फ़ोकस" to "पढ़ाई और एकाग्रता"),
                "local" to ("भारतीय" to "बेहतरीन भारतीय संगीत"),
                "party" to ("पार्टी" to "आवाज़ बढ़ाएँ"),
                "drive" to ("ड्राइव" to "सफ़र का संगीत"),
                "sad" to ("जज़्बात" to "धीमी और भावुक धुनें")
            )
            "id" -> mapOf(
                "hits" to ("Hit populer" to "yang paling banyak diputar sekarang"),
                "gym" to ("Olahraga" to "tenaga dan energi"),
                "chill" to ("Santai" to "lembut dan tenang"),
                "focus" to ("Fokus" to "belajar dan berkonsentrasi"),
                "local" to ("Indonesia" to "pilihan musik Indonesia"),
                "party" to ("Pesta" to "naikkan volumenya"),
                "drive" to ("Berkendara" to "musik untuk perjalanan"),
                "sad" to ("Melankolis" to "emosi yang mengalun pelan")
            )
            "vi" -> mapOf(
                "hits" to ("Hit thịnh hành" to "được nghe nhiều nhất lúc này"),
                "gym" to ("Tập luyện" to "sức mạnh và năng lượng"),
                "chill" to ("Thư giãn" to "nhẹ nhàng và bình yên"),
                "focus" to ("Tập trung" to "học tập và làm việc"),
                "local" to ("Nhạc Việt" to "tuyển chọn âm nhạc Việt Nam"),
                "party" to ("Tiệc" to "tăng âm lượng lên"),
                "drive" to ("Lái xe" to "âm nhạc cho hành trình"),
                "sad" to ("Tâm trạng" to "cảm xúc chậm rãi")
            )
            "th" -> mapOf(
                "hits" to ("เพลงฮิตมาแรง" to "เพลงที่มีคนฟังมากที่สุดตอนนี้"),
                "gym" to ("ออกกำลังกาย" to "พลังและความแข็งแรง"),
                "chill" to ("ชิลล์" to "นุ่มนวลและสงบ"),
                "focus" to ("มีสมาธิ" to "สำหรับเรียนและทำงาน"),
                "local" to ("เพลงไทย" to "คัดสรรเพลงไทยยอดนิยม"),
                "party" to ("ปาร์ตี้" to "เพิ่มระดับเสียงให้สุด"),
                "drive" to ("ขับรถ" to "เพลงสำหรับการเดินทาง"),
                "sad" to ("อารมณ์เศร้า" to "ความรู้สึกที่ค่อย ๆ ไหล")
            )
            "fil" -> mapOf(
                "hits" to ("Mga hit ngayon" to "pinakamadalas pakinggan ngayon"),
                "gym" to ("Workout" to "lakas at enerhiya"),
                "chill" to ("Chill" to "banayad at kalmado"),
                "focus" to ("Pokus" to "para sa pag-aaral at trabaho"),
                "local" to ("OPM" to "piniling musikang Pilipino"),
                "party" to ("Party" to "lakasan ang volume"),
                "drive" to ("Biyahe" to "musika para sa daan"),
                "sad" to ("Hugot" to "mabagal at malalim na damdamin")
            )
            "he" -> mapOf(
                "hits" to ("להיטים עכשיו" to "השירים המושמעים ביותר כרגע"),
                "gym" to ("אימון" to "כוח ואנרגיה"),
                "chill" to ("צ'יל" to "רך ורגוע"),
                "focus" to ("ריכוז" to "ללימודים ולעבודה"),
                "local" to ("ישראלי" to "מוזיקה ישראלית נבחרת"),
                "party" to ("מסיבה" to "מגבירים את הווליום"),
                "drive" to ("נסיעה" to "מוזיקה לדרך"),
                "sad" to ("מלנכוליה" to "רגשות שזורמים לאט")
            )
            else -> mapOf(
                "hits" to ("Trending hits" to "most played now"),
                "gym" to ("Gym" to "power and energy"),
                "chill" to ("Chill" to "soft and calm"),
                "focus" to ("Focus" to "study and concentration"),
                "local" to ("Local" to "your language picks"),
                "party" to ("Party" to "turn it up"),
                "drive" to ("Driving" to "road trip music"),
                "sad" to ("Melancholy" to "slow emotions")
            )
        }
    }

    private fun tasteLabels(languageCode: String): Map<String, String> {
        return when (LevyraLanguageCatalog.normalize(languageCode)) {
            "it" -> mapOf("hits" to "Hit", "rap" to "Rap & Trap", "local" to "Italiana", "pop" to "Pop", "gym" to "Palestra", "chill" to "Relax", "focus" to "Focus", "sad" to "Malinconia", "party" to "Festa", "rock" to "Rock", "electro" to "Elettronica", "rnb" to "R&B")
            "es" -> mapOf("hits" to "Éxitos", "rap" to "Rap & Trap", "local" to "Española", "pop" to "Pop", "gym" to "Gimnasio", "chill" to "Relax", "focus" to "Focus", "sad" to "Melancolía", "party" to "Fiesta", "rock" to "Rock", "electro" to "Electrónica", "rnb" to "R&B")
            "fr" -> mapOf("hits" to "Hits", "rap" to "Rap & Trap", "local" to "Française", "pop" to "Pop", "gym" to "Sport", "chill" to "Relax", "focus" to "Focus", "sad" to "Mélancolie", "party" to "Fête", "rock" to "Rock", "electro" to "Électronique", "rnb" to "R&B")
            "de" -> mapOf("hits" to "Hits", "rap" to "Rap & Trap", "local" to "Deutsch", "pop" to "Pop", "gym" to "Gym", "chill" to "Relax", "focus" to "Focus", "sad" to "Melancholie", "party" to "Party", "rock" to "Rock", "electro" to "Elektronisch", "rnb" to "R&B")
            "pt" -> mapOf("hits" to "Hits", "rap" to "Rap & Trap", "local" to "Brasileira", "pop" to "Pop", "gym" to "Academia", "chill" to "Relax", "focus" to "Foco", "sad" to "Melancolia", "party" to "Festa", "rock" to "Rock", "electro" to "Eletrônica", "rnb" to "R&B")
            "ar" -> mapOf("hits" to "الأكثر رواجًا", "rap" to "راب وتراب", "local" to "عربية", "pop" to "بوب", "gym" to "تمرين", "chill" to "استرخاء", "focus" to "تركيز", "sad" to "حزينة", "party" to "حفلة", "rock" to "روك", "electro" to "إلكترونية", "rnb" to "R&B")
            "zh" -> mapOf("hits" to "热门", "rap" to "说唱与 Trap", "local" to "华语", "pop" to "流行", "gym" to "健身", "chill" to "放松", "focus" to "专注", "sad" to "伤感", "party" to "派对", "rock" to "摇滚", "electro" to "电子", "rnb" to "R&B")
            "ja" -> mapOf("hits" to "ヒット", "rap" to "ラップ＆トラップ", "local" to "邦楽", "pop" to "ポップ", "gym" to "ワークアウト", "chill" to "チル", "focus" to "集中", "sad" to "切なさ", "party" to "パーティー", "rock" to "ロック", "electro" to "エレクトロニック", "rnb" to "R&B")
            "ko" -> mapOf("hits" to "히트곡", "rap" to "랩 & 트랩", "local" to "한국 음악", "pop" to "팝", "gym" to "운동", "chill" to "칠", "focus" to "집중", "sad" to "감성", "party" to "파티", "rock" to "록", "electro" to "일렉트로닉", "rnb" to "R&B")
            "hi" -> mapOf("hits" to "हिट्स", "rap" to "रैप और ट्रैप", "local" to "भारतीय", "pop" to "पॉप", "gym" to "वर्कआउट", "chill" to "सुकून", "focus" to "फ़ोकस", "sad" to "जज़्बात", "party" to "पार्टी", "rock" to "रॉक", "electro" to "इलेक्ट्रॉनिक", "rnb" to "R&B")
            "id" -> mapOf("hits" to "Hit", "rap" to "Rap & Trap", "local" to "Indonesia", "pop" to "Pop", "gym" to "Olahraga", "chill" to "Santai", "focus" to "Fokus", "sad" to "Melankolis", "party" to "Pesta", "rock" to "Rock", "electro" to "Elektronik", "rnb" to "R&B")
            "vi" -> mapOf("hits" to "Hit", "rap" to "Rap & Trap", "local" to "Nhạc Việt", "pop" to "Pop", "gym" to "Tập luyện", "chill" to "Thư giãn", "focus" to "Tập trung", "sad" to "Tâm trạng", "party" to "Tiệc", "rock" to "Rock", "electro" to "Điện tử", "rnb" to "R&B")
            "th" -> mapOf("hits" to "เพลงฮิต", "rap" to "แรปและแทรป", "local" to "เพลงไทย", "pop" to "ป๊อป", "gym" to "ออกกำลังกาย", "chill" to "ชิลล์", "focus" to "มีสมาธิ", "sad" to "เพลงเศร้า", "party" to "ปาร์ตี้", "rock" to "ร็อก", "electro" to "อิเล็กทรอนิกส์", "rnb" to "R&B")
            "fil" -> mapOf("hits" to "Mga hit", "rap" to "Rap at Trap", "local" to "OPM", "pop" to "Pop", "gym" to "Workout", "chill" to "Chill", "focus" to "Pokus", "sad" to "Hugot", "party" to "Party", "rock" to "Rock", "electro" to "Electronic", "rnb" to "R&B")
            "he" -> mapOf("hits" to "להיטים", "rap" to "ראפ וטראפ", "local" to "ישראלי", "pop" to "פופ", "gym" to "אימון", "chill" to "צ'יל", "focus" to "ריכוז", "sad" to "מלנכוליה", "party" to "מסיבה", "rock" to "רוק", "electro" to "אלקטרוני", "rnb" to "R&B")
            else -> mapOf("hits" to "Hits", "rap" to "Rap & Trap", "local" to "Local", "pop" to "Pop", "gym" to "Gym", "chill" to "Chill", "focus" to "Focus", "sad" to "Melancholy", "party" to "Party", "rock" to "Rock", "electro" to "Electronic", "rnb" to "R&B")
        }
    }
}
