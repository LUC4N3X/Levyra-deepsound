package com.luc4n3x.levyra.ui.i18n

data class LikeAllCopy(
    val action: String,
    val alreadyLiked: String,
    val failed: String,
    private val likedOne: String,
    private val likedFormat: String
) {
    fun liked(count: Int): String = if (count == 1) likedOne else likedFormat.replace("{count}", count.toString())
}

fun LevyraStrings.likeAllCopy(): LikeAllCopy = when (translationKey(code)) {
    "it" -> LikeAllCopy(
        "Metti Mi piace a tutti",
        "Tutti i brani hanno già Mi piace",
        "Impossibile aggiungere Mi piace ai brani",
        "Mi piace aggiunto a 1 brano",
        "Mi piace aggiunto a {count} brani"
    )
    "es" -> LikeAllCopy(
        "Marcar todas con Me gusta",
        "Todas las canciones ya tienen Me gusta",
        "No se pudo añadir Me gusta a las canciones",
        "Me gusta añadido a 1 canción",
        "Me gusta añadido a {count} canciones"
    )
    "fr" -> LikeAllCopy(
        "Tout aimer",
        "Tous les titres sont déjà aimés",
        "Impossible d’aimer les titres",
        "1 titre ajouté aux titres aimés",
        "{count} titres ajoutés aux titres aimés"
    )
    "de" -> LikeAllCopy(
        "Alle liken",
        "Alle Titel sind bereits geliked",
        "Titel konnten nicht geliked werden",
        "1 Titel geliked",
        "{count} Titel geliked"
    )
    "pt" -> LikeAllCopy(
        "Curtir todas",
        "Todas as faixas já foram curtidas",
        "Não foi possível curtir as faixas",
        "1 faixa curtida",
        "{count} faixas curtidas"
    )
    else -> LikeAllCopy(
        "Like all",
        "All tracks are already liked",
        "Couldn’t like the tracks",
        "Liked 1 track",
        "Liked {count} tracks"
    )
}
