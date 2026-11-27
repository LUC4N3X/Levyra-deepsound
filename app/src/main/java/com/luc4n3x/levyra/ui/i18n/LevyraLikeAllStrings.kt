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

fun LevyraStrings.likeAllCopy(): LikeAllCopy = when (translationKey(code, regional = true)) {
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
    "ur" -> LikeAllCopy(
        "سب کی طرح",
        "تمام ٹریک پہلے ہی پسند کیے گئے ہیں۔",
        "ٹریکس پسند نہیں کر سکے۔",
        "1 ٹریک پسند کیا۔",
        "{count} ٹریکس کو پسند کیا۔"
    )
    "ta" -> LikeAllCopy(
        "அனைத்தையும் போல",
        "அனைத்து பாடல்களும் ஏற்கனவே விரும்பப்பட்டன",
        "தடங்கள் பிடிக்க முடியவில்லை",
        "1 டிராக்கை விரும்பினேன்",
        "{count} டிராக்குகளை விரும்பினேன்"
    )
    "te" -> LikeAllCopy(
        "అందరిలాగే",
        "అన్ని ట్రాక్‌లు ఇప్పటికే లైక్ చేయబడ్డాయి",
        "ట్రాక్‌లు నచ్చలేదు",
        "1 ట్రాక్ ఇష్టపడ్డారు",
        "{count} ట్రాక్‌లను ఇష్టపడ్డారు"
    )
    "mr" -> LikeAllCopy(
        "सर्व आवडले",
        "सर्व ट्रॅक आधीच आवडले आहेत",
        "ट्रॅक आवडले नाहीत",
        "१ ट्रॅक आवडला",
        "{count} ट्रॅक आवडले"
    )
    "gu" -> LikeAllCopy(
        "બધાની જેમ",
        "બધા ટ્રેક પહેલેથી જ પસંદ કરવામાં આવ્યા છે",
        "ટ્રેક પસંદ કરી શક્યા નથી",
        "1 ટ્રેક ગમ્યો",
        "{count} ટ્રેક ગમ્યા"
    )
    "pt-BR" -> LikeAllCopy(
        "Curtir todas",
        "Todas as faixas já estão curtidas",
        "Não consegui gostar das faixas",
        "Gostei de 1 faixa",
        "Faixas favoritas de {count}"
    )
    else -> LikeAllCopy(
        "Like all",
        "All tracks are already liked",
        "Couldn’t like the tracks",
        "Liked 1 track",
        "Liked {count} tracks"
    )
}
