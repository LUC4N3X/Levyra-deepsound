package com.luc4n3x.levyra.domain

class MoodEngine {
    val moods: List<Mood> = listOf(
        Mood(
            id = "hits",
            title = "Hit del momento",
            subtitle = "le piu ascoltate ora",
            icon = "🔥",
            energyTarget = 78,
            tags = setOf("hit", "pop", "new"),
            accentStart = 0xFFFF512F.toInt(),
            accentEnd = 0xFFDD2476.toInt()
        ),
        Mood(
            id = "gym",
            title = "Palestra",
            subtitle = "carica e potenza",
            icon = "🏋️",
            energyTarget = 92,
            tags = setOf("gym", "bass", "rap", "energy", "trap"),
            accentStart = 0xFF1B5CFF.toInt(),
            accentEnd = 0xFF00E5FF.toInt()
        ),
        Mood(
            id = "chill",
            title = "Relax",
            subtitle = "calmo e morbido",
            icon = "😌",
            energyTarget = 42,
            tags = setOf("chill", "rnb", "ambient", "night"),
            accentStart = 0xFF11998E.toInt(),
            accentEnd = 0xFF38EF7D.toInt()
        ),
        Mood(
            id = "focus",
            title = "Focus",
            subtitle = "studio e concentrazione",
            icon = "🎧",
            energyTarget = 48,
            tags = setOf("focus", "electronic", "deep", "ambient"),
            accentStart = 0xFF6A11CB.toInt(),
            accentEnd = 0xFF2575FC.toInt()
        ),
        Mood(
            id = "italia",
            title = "Italia",
            subtitle = "il meglio italiano",
            icon = "🇮🇹",
            energyTarget = 70,
            tags = setOf("italian", "pop", "hit"),
            accentStart = 0xFF00D4A6.toInt(),
            accentEnd = 0xFFFF3B5C.toInt()
        ),
        Mood(
            id = "party",
            title = "Festa",
            subtitle = "alza il volume",
            icon = "🎉",
            energyTarget = 95,
            tags = setOf("party", "dance", "hit", "energy"),
            accentStart = 0xFFFFB000.toInt(),
            accentEnd = 0xFFFF4FD8.toInt()
        ),
        Mood(
            id = "drive",
            title = "In auto",
            subtitle = "musica da viaggio",
            icon = "🚗",
            energyTarget = 64,
            tags = setOf("night", "chill", "pop", "rap"),
            accentStart = 0xFF8E2DE2.toInt(),
            accentEnd = 0xFF4A00E0.toInt()
        ),
        Mood(
            id = "sad",
            title = "Malinconia",
            subtitle = "emozioni lente",
            icon = "💔",
            energyTarget = 34,
            tags = setOf("sad", "rnb", "chill"),
            accentStart = 0xFF355C7D.toInt(),
            accentEnd = 0xFFC06C84.toInt()
        )
    )

    /** Genres offered in the first launch questionnaire. */
    val tastes: List<Taste> = listOf(
        Taste("hits", "Hit", "🔥", "top hits 2026"),
        Taste("rap", "Rap & Trap", "🎤", "rap italiano 2026"),
        Taste("italiana", "Italiana", "🇮🇹", "canzoni italiane 2026"),
        Taste("pop", "Pop", "✨", "pop hits 2026"),
        Taste("gym", "Palestra", "🏋️", "gym workout hype music"),
        Taste("chill", "Relax", "😌", "chill relax music"),
        Taste("focus", "Focus", "🎧", "focus deep concentration music"),
        Taste("sad", "Malinconia", "💔", "canzoni tristi malinconia"),
        Taste("party", "Festa", "🎉", "party dance hits 2026"),
        Taste("rock", "Rock", "🎸", "rock hits"),
        Taste("electro", "Elettronica", "🎛️", "electronic edm music"),
        Taste("rnb", "R&B", "🕺", "rnb soul hits")
    )

    val defaultHomeQueries: List<String> = listOf(
        "top hits italia 2026",
        "global top hits 2026",
        "rap italiano 2026",
        "pop hits 2026"
    )

    fun queriesForTastes(ids: Set<String>): List<String> {
        val selected = tastes.filter { it.id in ids }
        if (selected.isEmpty()) return defaultHomeQueries
        return selected.map { it.query }
    }

    fun buildQueue(mood: Mood?, tracks: List<Track>): List<Track> {
        return tracks.sortedWith(
            compareByDescending<Track> { it.smartWeightFor(mood) }
                .thenByDescending { it.replayScore }
                .thenBy { it.title.lowercase() }
        )
    }

    fun tagQueryFor(mood: Mood): String {
        return when (mood.id) {
            "hits" -> "top hits 2026"
            "gym" -> "gym workout hype rap"
            "chill" -> "chill relax music"
            "focus" -> "focus deep concentration music"
            "italia" -> "canzoni italiane 2026"
            "party" -> "party dance hits 2026"
            "drive" -> "night drive playlist"
            "sad" -> "canzoni tristi malinconia"
            else -> mood.title
        }
    }
}
