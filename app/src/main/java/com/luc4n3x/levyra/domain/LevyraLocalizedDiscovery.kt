package com.luc4n3x.levyra.domain

object LevyraLocalizedDiscovery {
    fun suggestions(query: String, languageCode: String, remote: List<String>): List<String> {
        val clean = query.trim().lowercase()
        if (clean.isBlank()) return remote.distinctClean().take(8)
        val locale = LevyraContentLocales.forLanguage(languageCode)
        val localPool = buildList {
            addAll(LevyraContentLocales.quickSearches(locale.languageCode))
            addAll(LevyraContentLocales.artistSuggestions(locale.languageCode))
            addAll(locale.homeQueries)
            addAll(locale.tasteQueries.values)
            addAll(locale.moodQueries.values)
            addAll(contextualQueries(clean, locale.languageCode))
        }
        val localMatches = localPool.filter { it.lowercase().contains(clean) || clean.contains(it.lowercase()) }
        return (remote + localMatches + contextualQueries(clean, locale.languageCode)).distinctClean().take(8)
    }

    fun homeBoostQueries(languageCode: String, tasteIds: Set<String>): List<String> {
        val locale = LevyraContentLocales.forLanguage(languageCode)
        val tasteQueries = tasteIds.mapNotNull { id -> locale.tasteQueries[id] }
        return (tasteQueries + locale.homeQueries + contextualQueries("", locale.languageCode)).distinctClean().take(10)
    }

    private fun contextualQueries(query: String, languageCode: String): List<String> {
        return when (languageCode) {
            "it" -> listOf("rap italiano nuovo 2026", "hit italiane estate 2026", "musica italiana palestra", "playlist viaggio notte italiana", "pop italiano nuovo")
            "es" -> listOf("reggaeton nuevo 2026", "hits España 2026", "música para gimnasio", "playlist conducir de noche", "pop español nuevo")
            "fr" -> listOf("rap français nouveau 2026", "hits France 2026", "musique sport", "playlist conduite nuit", "pop française nouvelle")
            "de" -> listOf("deutschrap neu 2026", "hits Deutschland 2026", "gym deutschrap", "nacht fahren playlist", "deutsche pop neu")
            "pt" -> listOf("funk brasileiro novo 2026", "hits Brasil 2026", "música academia", "dirigir à noite playlist", "pop brasileiro novo")
            "nl" -> listOf("nederlandse hits 2026", "nederlandse rap nieuw", "gym muziek", "night drive playlist", "nederlandse pop nieuw")
            "pl" -> listOf("polskie hity 2026", "polski rap nowy", "muzyka na siłownię", "night drive playlist", "polski pop nowy")
            "ro" -> listOf("hituri România 2026", "rap românesc nou", "muzică sală", "night drive playlist", "pop românesc nou")
            "el" -> listOf("ελληνικά hits 2026", "ελληνικό rap νέο", "μουσική γυμναστήριο", "night drive playlist", "ελληνική pop νέα")
            "sv" -> listOf("svenska hits 2026", "svensk rap ny", "träningsmusik", "night drive playlist", "svensk pop ny")
            "da" -> listOf("danske hits 2026", "dansk rap ny", "træningsmusik", "night drive playlist", "dansk pop ny")
            "cs" -> listOf("české hity 2026", "český rap nový", "hudba do posilovny", "night drive playlist", "český pop nový")
            "uk" -> listOf("українські хіти 2026", "український реп новий", "музика для тренувань", "night drive playlist", "українська поп музика")
            "ar" -> listOf("أغاني عربية جديدة 2026", "راب عربي جديد", "موسيقى للتمرين", "قائمة قيادة ليلية عربية", "بوب عربي جديد")
            "zh" -> listOf("2026 华语新歌", "中文说唱新歌", "健身音乐", "夜间驾驶歌单", "华语流行新歌")
            "ja" -> listOf("2026 邦楽新曲", "日本語ラップ新曲", "ワークアウト音楽", "夜のドライブプレイリスト", "最新 J-POP")
            "ko" -> listOf("2026 국내 신곡", "한국 힙합 신곡", "운동 음악", "야간 드라이브 플레이리스트", "최신 K-POP")
            "hi" -> listOf("2026 नए हिंदी गाने", "नया हिंदी रैप", "वर्कआउट संगीत", "नाइट ड्राइव प्लेलिस्ट", "नया हिंदी पॉप")
            "id" -> listOf("lagu Indonesia terbaru 2026", "rap Indonesia terbaru", "musik olahraga", "playlist berkendara malam", "pop Indonesia terbaru")
            "vi" -> listOf("nhạc Việt mới 2026", "rap Việt mới", "nhạc tập luyện", "playlist lái xe ban đêm", "V-pop mới")
            "th" -> listOf("เพลงไทยใหม่ 2026", "แรปไทยใหม่", "เพลงออกกำลังกาย", "เพลย์ลิสต์ขับรถกลางคืน", "เพลงป๊อปไทยใหม่")
            "fil" -> listOf("bagong OPM 2026", "bagong Pinoy rap", "musikang pang-workout", "playlist para sa night drive", "bagong P-pop")
            "he" -> listOf("שירים ישראליים חדשים 2026", "ראפ ישראלי חדש", "מוזיקה לאימון", "פלייליסט לנסיעה בלילה", "פופ ישראלי חדש")
            else -> listOf("new music 2026", "top hits 2026", "gym music", "night drive playlist", "fresh pop hits")
        }.let { list ->
            if (query.isBlank()) list else list.map { item -> if (item.lowercase().contains(query)) item else "$query $item" }
        }
    }

    private fun List<String>.distinctClean(): List<String> {
        val seen = LinkedHashSet<String>()
        return map { it.trim().replace(Regex("\\s+"), " ") }
            .filter { it.length >= 2 }
            .filter { seen.add(it.lowercase()) }
    }
}
