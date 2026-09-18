package com.luc4n3x.levyra.ui.i18n

internal val localTagKeys = setOf(
    "localFullTagSearchHint",
    "localEditTags",
    "localTagEditorTitle",
    "localTagEditorSubtitle",
    "localTagTitle",
    "localTagArtist",
    "localTagAlbum",
    "localTagAlbumArtist",
    "localTagGenre",
    "localTagYear",
    "localTagTrack",
    "localTagDisc",
    "localTagComposer",
    "localTagLyricist",
    "localTagComment",
    "localTagCopyright",
    "localTagCredits",
    "localTagSave",
    "localTagSaving",
    "localTagSaved",
    "localTagWriteFailed",
    "localTagUnsupported",
    "localTagTooLarge",
    "localTagPermissionDenied"
)

private fun localTagBundle(
    searchHint: String,
    editTags: String,
    editorTitle: String,
    editorSubtitle: String,
    title: String,
    artist: String,
    album: String,
    albumArtist: String,
    genre: String,
    year: String,
    track: String,
    disc: String,
    composer: String,
    lyricist: String,
    comment: String,
    copyright: String,
    credits: String,
    save: String,
    saving: String,
    saved: String,
    writeFailed: String,
    unsupported: String,
    tooLarge: String,
    permissionDenied: String
): Map<String, String> = mapOf(
    "localFullTagSearchHint" to searchHint,
    "localEditTags" to editTags,
    "localTagEditorTitle" to editorTitle,
    "localTagEditorSubtitle" to editorSubtitle,
    "localTagTitle" to title,
    "localTagArtist" to artist,
    "localTagAlbum" to album,
    "localTagAlbumArtist" to albumArtist,
    "localTagGenre" to genre,
    "localTagYear" to year,
    "localTagTrack" to track,
    "localTagDisc" to disc,
    "localTagComposer" to composer,
    "localTagLyricist" to lyricist,
    "localTagComment" to comment,
    "localTagCopyright" to copyright,
    "localTagCredits" to credits,
    "localTagSave" to save,
    "localTagSaving" to saving,
    "localTagSaved" to saved,
    "localTagWriteFailed" to writeFailed,
    "localTagUnsupported" to unsupported,
    "localTagTooLarge" to tooLarge,
    "localTagPermissionDenied" to permissionDenied
)

private val localTagBundles = mapOf(
    "en" to localTagBundle(
        "Search title, artist, album, composer, lyricist, comments or tags",
        "Edit file tags",
        "File tags",
        "Changes are written into the audio file itself.",
        "Title", "Artist", "Album", "Album artist", "Genre", "Year", "Track", "Disc",
        "Composer", "Lyricist", "Comment", "Copyright", "Credits", "Save changes", "Saving…",
        "Tags saved", "Couldn't write these tags.", "Safe tag editing is not available for this file format.",
        "This file is too large for Levyra's safe tag writer.", "Write access wasn't granted."
    ),
    "it" to localTagBundle(
        "Cerca titolo, artista, album, compositore, autore, commenti o tag",
        "Modifica tag file",
        "Tag del file",
        "Le modifiche vengono scritte direttamente nel file audio.",
        "Titolo", "Artista", "Album", "Artista album", "Genere", "Anno", "Traccia", "Disco",
        "Compositore", "Autore testo", "Commento", "Copyright", "Crediti", "Salva modifiche", "Salvataggio…",
        "Tag salvati", "Impossibile scrivere questi tag.", "La modifica sicura dei tag non è disponibile per questo formato.",
        "Il file è troppo grande per il writer sicuro di Levyra.", "L'accesso in scrittura non è stato concesso."
    ),
    "es" to localTagBundle(
        "Buscar título, artista, álbum, compositor, letrista, comentarios o etiquetas",
        "Editar etiquetas del archivo",
        "Etiquetas del archivo",
        "Los cambios se escriben directamente en el archivo de audio.",
        "Título", "Artista", "Álbum", "Artista del álbum", "Género", "Año", "Pista", "Disco",
        "Compositor", "Letrista", "Comentario", "Copyright", "Créditos", "Guardar cambios", "Guardando…",
        "Etiquetas guardadas", "No se pudieron escribir las etiquetas.", "La edición segura no está disponible para este formato.",
        "El archivo es demasiado grande para el editor seguro de Levyra.", "No se concedió permiso de escritura."
    ),
    "fr" to localTagBundle(
        "Rechercher titre, artiste, album, compositeur, parolier, commentaires ou tags",
        "Modifier les tags du fichier",
        "Tags du fichier",
        "Les modifications sont écrites directement dans le fichier audio.",
        "Titre", "Artiste", "Album", "Artiste de l'album", "Genre", "Année", "Piste", "Disque",
        "Compositeur", "Parolier", "Commentaire", "Copyright", "Crédits", "Enregistrer", "Enregistrement…",
        "Tags enregistrés", "Impossible d'écrire ces tags.", "L'édition sûre n'est pas disponible pour ce format.",
        "Ce fichier est trop volumineux pour l'éditeur sûr de Levyra.", "L'autorisation d'écriture n'a pas été accordée."
    ),
    "de" to localTagBundle(
        "Titel, Interpret, Album, Komponist, Texter, Kommentare oder Tags suchen",
        "Datei-Tags bearbeiten",
        "Datei-Tags",
        "Änderungen werden direkt in die Audiodatei geschrieben.",
        "Titel", "Interpret", "Album", "Albuminterpret", "Genre", "Jahr", "Titelnummer", "Disc",
        "Komponist", "Texter", "Kommentar", "Copyright", "Credits", "Änderungen speichern", "Speichern…",
        "Tags gespeichert", "Diese Tags konnten nicht geschrieben werden.", "Sicheres Tag-Bearbeiten ist für dieses Format nicht verfügbar.",
        "Die Datei ist für Levyras sicheren Tag-Writer zu groß.", "Schreibzugriff wurde nicht erteilt."
    ),
    "pt" to localTagBundle(
        "Pesquisar título, artista, álbum, compositor, letrista, comentários ou etiquetas",
        "Editar etiquetas do ficheiro",
        "Etiquetas do ficheiro",
        "As alterações são gravadas diretamente no ficheiro de áudio.",
        "Título", "Artista", "Álbum", "Artista do álbum", "Género", "Ano", "Faixa", "Disco",
        "Compositor", "Letrista", "Comentário", "Copyright", "Créditos", "Guardar alterações", "A guardar…",
        "Etiquetas guardadas", "Não foi possível gravar estas etiquetas.", "A edição segura não está disponível para este formato.",
        "Este ficheiro é demasiado grande para o editor seguro do Levyra.", "A permissão de escrita não foi concedida."
    )
)

internal fun localTagLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(localTagBundles, code)
