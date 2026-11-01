package com.luc4n3x.levyra.ui.i18n

data class PlaylistAlbumCopy(
    val addAlbumToPlaylist: String,
    val albumLabel: String,
    val openAlbum: String,
    val playAlbum: String,
    val removeAlbum: String,
    val alreadyInPlaylist: String,
    val addFailed: String,
    val albumUnavailable: String,
    val inPlaylist: String,
    private val addedToTemplate: String,
    private val oneAlbum: String,
    private val manyAlbums: String
) {
    fun addedTo(playlistName: String): String = addedToTemplate.replace("%s", playlistName.ifBlank { "Playlist" })

    fun albumCount(count: Int): String = if (count == 1) oneAlbum else manyAlbums.replace("%d", count.toString())
}

fun LevyraStrings.playlistAlbumCopy(): PlaylistAlbumCopy = playlistAlbumCopy(code)

fun playlistAlbumCopy(code: String): PlaylistAlbumCopy = when (translationKey(code)) {
    "it" -> PlaylistAlbumCopy(
        addAlbumToPlaylist = "Aggiungi album alla playlist",
        albumLabel = "Album",
        openAlbum = "Apri album",
        playAlbum = "Riproduci album",
        removeAlbum = "Rimuovi album dalla playlist",
        alreadyInPlaylist = "L'album è già in questa playlist",
        addFailed = "Impossibile aggiungere l'album",
        albumUnavailable = "Un album non è al momento disponibile",
        inPlaylist = "Già presente",
        addedToTemplate = "Album aggiunto a %s",
        oneAlbum = "1 album",
        manyAlbums = "%d album"
    )
    "es" -> PlaylistAlbumCopy(
        addAlbumToPlaylist = "Añadir álbum a la playlist",
        albumLabel = "Álbum",
        openAlbum = "Abrir álbum",
        playAlbum = "Reproducir álbum",
        removeAlbum = "Quitar álbum de la playlist",
        alreadyInPlaylist = "El álbum ya está en esta playlist",
        addFailed = "No se pudo añadir el álbum",
        albumUnavailable = "Un álbum no está disponible ahora",
        inPlaylist = "Ya añadido",
        addedToTemplate = "Álbum añadido a %s",
        oneAlbum = "1 álbum",
        manyAlbums = "%d álbumes"
    )
    "fr" -> PlaylistAlbumCopy(
        addAlbumToPlaylist = "Ajouter l'album à la playlist",
        albumLabel = "Album",
        openAlbum = "Ouvrir l'album",
        playAlbum = "Lire l'album",
        removeAlbum = "Retirer l'album de la playlist",
        alreadyInPlaylist = "L'album est déjà dans cette playlist",
        addFailed = "Impossible d'ajouter l'album",
        albumUnavailable = "Un album est momentanément indisponible",
        inPlaylist = "Déjà ajouté",
        addedToTemplate = "Album ajouté à %s",
        oneAlbum = "1 album",
        manyAlbums = "%d albums"
    )
    "de" -> PlaylistAlbumCopy(
        addAlbumToPlaylist = "Album zur Playlist hinzufügen",
        albumLabel = "Album",
        openAlbum = "Album öffnen",
        playAlbum = "Album abspielen",
        removeAlbum = "Album aus Playlist entfernen",
        alreadyInPlaylist = "Das Album ist bereits in dieser Playlist",
        addFailed = "Album konnte nicht hinzugefügt werden",
        albumUnavailable = "Ein Album ist gerade nicht verfügbar",
        inPlaylist = "Bereits enthalten",
        addedToTemplate = "Album zu %s hinzugefügt",
        oneAlbum = "1 Album",
        manyAlbums = "%d Alben"
    )
    "pt" -> PlaylistAlbumCopy(
        addAlbumToPlaylist = "Adicionar álbum à playlist",
        albumLabel = "Álbum",
        openAlbum = "Abrir álbum",
        playAlbum = "Reproduzir álbum",
        removeAlbum = "Remover álbum da playlist",
        alreadyInPlaylist = "O álbum já está nesta playlist",
        addFailed = "Não foi possível adicionar o álbum",
        albumUnavailable = "Um álbum não está disponível agora",
        inPlaylist = "Já adicionado",
        addedToTemplate = "Álbum adicionado a %s",
        oneAlbum = "1 álbum",
        manyAlbums = "%d álbuns"
    )
    "ru" -> PlaylistAlbumCopy(
        addAlbumToPlaylist = "Добавить альбом в плейлист",
        albumLabel = "Альбом",
        openAlbum = "Открыть альбом",
        playAlbum = "Слушать альбом",
        removeAlbum = "Удалить альбом из плейлиста",
        alreadyInPlaylist = "Альбом уже есть в этом плейлисте",
        addFailed = "Не удалось добавить альбом",
        albumUnavailable = "Один из альбомов сейчас недоступен",
        inPlaylist = "Уже добавлен",
        addedToTemplate = "Альбом добавлен в %s",
        oneAlbum = "1 альбом",
        manyAlbums = "Альбомов: %d"
    )
    else -> PlaylistAlbumCopy(
        addAlbumToPlaylist = "Add album to playlist",
        albumLabel = "Album",
        openAlbum = "Open album",
        playAlbum = "Play album",
        removeAlbum = "Remove album from playlist",
        alreadyInPlaylist = "This album is already in the playlist",
        addFailed = "Couldn't add the album",
        albumUnavailable = "An album is currently unavailable",
        inPlaylist = "Already added",
        addedToTemplate = "Album added to %s",
        oneAlbum = "1 album",
        manyAlbums = "%d albums"
    )
}
