package com.luc4n3x.levyra.ui

internal object HomeSoundtrackPortraits {
    private val byLanguage: Map<String, List<String>> = mapOf(
        "it" to listOf("Annalisa", "Lazza", "Tananai", "Marracash", "Sfera Ebbasta", "Elodie", "Angelina Mango"),
        "en" to listOf("Billie Eilish", "The Weeknd", "Dua Lipa", "Sabrina Carpenter", "Kendrick Lamar", "Olivia Rodrigo", "Harry Styles"),
        "es" to listOf("Rosalía", "Bad Bunny", "Karol G", "Quevedo", "Peso Pluma", "Aitana", "Feid"),
        "pt" to listOf("Anitta", "Luísa Sonza", "Matuê", "Ludmilla", "Jão", "Liniker", "Pabllo Vittar"),
        "fr" to listOf("Aya Nakamura", "Angèle", "Stromae", "Damso", "Theodora", "Tiakola", "Ninho"),
        "de" to listOf("Apache 207", "Nina Chuba", "Shirin David", "Luciano", "Ayliva", "Cro", "Kontra K"),
        "ru" to listOf("Zivert", "Miyagi & Andy Panda", "Macan", "Monetochka", "Instasamka", "Jony", "Oxxxymiron"),
        "ja" to listOf("Fujii Kaze", "Ado", "YOASOBI", "Kenshi Yonezu", "Aimyon", "Mrs. GREEN APPLE", "Vaundy"),
        "ko" to listOf("IU", "NewJeans", "aespa", "IVE", "BLACKPINK", "Stray Kids", "BTS")
    )

    private val qobuzPortraitPages: Map<String, String> = mapOf(
        "karol g" to "https://www.qobuz.com/it-it/interpreter/karol-g/2587039"
    )

    fun qobuzPortraitPage(artistName: String): String? = qobuzPortraitPages[artistName.trim().lowercase()]

    fun artistForDay(languageCode: String, epochDay: Long): String {
        val pool = byLanguage[languageCode.substringBefore('-').lowercase()] ?: byLanguage.getValue("en")
        return pool[Math.floorMod(epochDay, pool.size.toLong()).toInt()]
    }
}
