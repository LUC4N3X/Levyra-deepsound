package com.luc4n3x.levyra.ui.i18n

internal val lyricsShareKeys = setOf(
    "shareLyrics",
    "shareLyricsContinue",
    "shareLyricsPreviewTitle",
    "shareLyricsPreviewDescription",
    "shareLyricsStyleArtwork",
    "shareLyricsStyleGradient",
    "shareLyricsStyleMinimal",
    "shareLyricsTextOriginal",
    "shareLyricsTextTranslation",
    "shareLyricsShareImage",
    "shareLyricsPreparing",
    "shareLyricsFailed",
    "shareLyricsLineSelected",
    "shareLyricsLineNotSelected"
)

private fun lyricsShare(
    v0: String,
    v1: String,
    v2: String,
    v3: String,
    v4: String,
    v5: String,
    v6: String,
    v7: String,
    v8: String,
    v9: String,
    v10: String,
    v11: String,
    v12: String,
    v13: String
): Map<String, String> = mapOf(
    "shareLyrics" to v0,
    "shareLyricsContinue" to v1,
    "shareLyricsPreviewTitle" to v2,
    "shareLyricsPreviewDescription" to v3,
    "shareLyricsStyleArtwork" to v4,
    "shareLyricsStyleGradient" to v5,
    "shareLyricsStyleMinimal" to v6,
    "shareLyricsTextOriginal" to v7,
    "shareLyricsTextTranslation" to v8,
    "shareLyricsShareImage" to v9,
    "shareLyricsPreparing" to v10,
    "shareLyricsFailed" to v11,
    "shareLyricsLineSelected" to v12,
    "shareLyricsLineNotSelected" to v13
)

private val lyricsShareBundles: Map<String, Map<String, String>> = mapOf(
    "en" to lyricsShare(
        "Share lyrics",
        "Preview",
        "Lyric card",
        "Lyric card preview",
        "Artwork",
        "Gradient",
        "Minimal",
        "Original",
        "Translation",
        "Share image",
        "Preparing…",
        "Couldn't create the image. Try again.",
        "Selected",
        "Not selected"
    ),
    "it" to lyricsShare(
        "Condividi testo",
        "Anteprima",
        "Card del testo",
        "Anteprima della card del testo",
        "Copertina",
        "Gradiente",
        "Minimale",
        "Originale",
        "Traduzione",
        "Condividi immagine",
        "Preparazione…",
        "Impossibile creare l'immagine. Riprova.",
        "Selezionato",
        "Non selezionato"
    ),
    "es" to lyricsShare(
        "Compartir letra",
        "Vista previa",
        "Tarjeta de letra",
        "Vista previa de la tarjeta de letra",
        "Portada",
        "Degradado",
        "Minimalista",
        "Original",
        "Traducción",
        "Compartir imagen",
        "Preparando…",
        "No se pudo crear la imagen. Inténtalo de nuevo.",
        "Seleccionado",
        "No seleccionado"
    ),
    "fr" to lyricsShare(
        "Partager les paroles",
        "Aperçu",
        "Carte de paroles",
        "Aperçu de la carte de paroles",
        "Pochette",
        "Dégradé",
        "Minimal",
        "Original",
        "Traduction",
        "Partager l'image",
        "Préparation…",
        "Impossible de créer l'image. Réessayez.",
        "Sélectionné",
        "Non sélectionné"
    ),
    "de" to lyricsShare(
        "Liedtext teilen",
        "Vorschau",
        "Liedtext-Karte",
        "Vorschau der Liedtext-Karte",
        "Cover",
        "Verlauf",
        "Minimal",
        "Original",
        "Übersetzung",
        "Bild teilen",
        "Wird vorbereitet…",
        "Das Bild konnte nicht erstellt werden. Versuche es erneut.",
        "Ausgewählt",
        "Nicht ausgewählt"
    ),
    "pt" to lyricsShare(
        "Compartilhar letra",
        "Pré-visualizar",
        "Cartão da letra",
        "Pré-visualização do cartão da letra",
        "Capa",
        "Gradiente",
        "Minimalista",
        "Original",
        "Tradução",
        "Compartilhar imagem",
        "Preparando…",
        "Não foi possível criar a imagem. Tente novamente.",
        "Selecionado",
        "Não selecionado"
    )
)

internal fun lyricsShareLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(lyricsShareBundles, code)
