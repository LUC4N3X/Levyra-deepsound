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
            else -> mapOf("hits" to "Hits", "rap" to "Rap & Trap", "local" to "Local", "pop" to "Pop", "gym" to "Gym", "chill" to "Chill", "focus" to "Focus", "sad" to "Melancholy", "party" to "Party", "rock" to "Rock", "electro" to "Electronic", "rnb" to "R&B")
        }
    }
}
