package com.luc4n3x.levyra.domain

import java.util.Locale

internal data class OnboardingArtist(val name: String) {
    val id: String get() = "artist:$name"
}

internal data class OnboardingArtistSection(val id: String, val label: String, val artists: List<OnboardingArtist>)

internal object OnboardingArtists {
    const val InitialSectionSize = 6
    private const val Prefix = "artist:"
    private val international = linkedMapOf(
        "pop" to listOf("Beyoncé", "Taylor Swift", "Bruno Mars", "Ariana Grande", "Justin Bieber", "The Weeknd", "Sabrina Carpenter", "Ed Sheeran", "Rihanna", "Charli xcx", "Dua Lipa", "Billie Eilish", "Adele", "Olivia Rodrigo", "Lady Gaga", "Harry Styles", "Lana Del Rey", "Miley Cyrus"),
        "rap" to listOf("Kendrick Lamar", "Drake", "A\$AP Rocky", "Travis Scott", "J. Cole", "Cardi B", "Eminem", "JAY-Z", "Future", "Gunna", "Lil Wayne", "Nicki Minaj"),
        "rock" to listOf("Arctic Monkeys", "Radiohead", "Foo Fighters", "AC/DC", "Nirvana", "The Strokes", "Oasis", "Muse", "Pearl Jam", "The Cure", "The Beatles", "Fleetwood Mac"),
        "electro" to listOf("Daft Punk", "Fred again..", "Skrillex", "David Guetta", "Calvin Harris", "Avicii", "Kraftwerk", "Four Tet", "The Chemical Brothers", "deadmau5", "Martin Garrix", "Disclosure"),
        "rnb" to listOf("SZA", "Frank Ocean", "Alicia Keys", "Usher", "Erykah Badu", "Daniel Caesar", "Jorja Smith", "H.E.R.", "Summer Walker", "Solange", "D'Angelo", "Anderson .Paak")
    )

    fun sections(languageCode: String): List<OnboardingArtistSection> {
        val labels = MoodEngine().tastesForLanguage(languageCode).associate { it.id to it.label }
        val local = LevyraContentLocales.artistSuggestions(languageCode).let { artists ->
            if (LevyraContentLocales.forLanguage(languageCode).contentLanguageCode == "it") {
                listOf("Sfera Ebbasta", "Lazza", "Marracash", "Annalisa", "Geolier", "Madame", "Ghali", "Tedua", "Ultimo", "Capo Plaza", "Pinguini Tattici Nucleari", "Elodie", "Salmo", "Tiziano Ferro", "Måneskin", "BLANCO", "Coez", "Guè")
            } else artists
        }
        val seen = mutableSetOf<String>()
        return (listOf("italiana" to local) + international.entries.map { it.key to it.value }).map { (id, names) ->
            OnboardingArtistSection(id, labels.getValue(id), names.filter { seen.add(it.lowercase(Locale.ROOT)) }.map(::OnboardingArtist))
        }.filter { it.artists.isNotEmpty() }
    }

    fun selectedNames(ids: Set<String>): List<String> = ids.asSequence()
        .filter { it.startsWith(Prefix) }
        .map { it.removePrefix(Prefix).trim() }
        .filter { it.isNotBlank() && it.length <= 120 }
        .distinctBy { it.lowercase(Locale.ROOT) }
        .toList()
}
