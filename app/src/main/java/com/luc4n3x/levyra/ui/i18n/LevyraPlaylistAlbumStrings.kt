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

fun playlistAlbumCopy(code: String): PlaylistAlbumCopy = when (translationKey(code, regional = true)) {
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
    "ur" -> PlaylistAlbumCopy(
        addAlbumToPlaylist = "پلے لسٹ میں البم شامل کریں۔",
        albumLabel = "البم",
        openAlbum = "البم کھولیں۔",
        playAlbum = "البم چلائیں۔",
        removeAlbum = "پلے لسٹ سے البم کو ہٹا دیں۔",
        alreadyInPlaylist = "یہ البم پہلے سے ہی پلے لسٹ میں ہے۔",
        addFailed = "البم شامل نہیں کیا جا سکا",
        albumUnavailable = "ایک البم فی الحال دستیاب نہیں ہے۔",
        inPlaylist = "پہلے ہی شامل کیا گیا ہے۔",
        addedToTemplate = "البم %s میں شامل کر دیا گیا۔",
        oneAlbum = "1 البم",
        manyAlbums = "%d البمز"
    )
    "ta" -> PlaylistAlbumCopy(
        addAlbumToPlaylist = "பிளேலிஸ்ட்டில் ஆல்பத்தைச் சேர்க்கவும்",
        albumLabel = "ஆல்பம்",
        openAlbum = "ஆல்பத்தைத் திற",
        playAlbum = "ஆல்பத்தை இயக்கு",
        removeAlbum = "பிளேலிஸ்ட்டில் இருந்து ஆல்பத்தை அகற்று",
        alreadyInPlaylist = "இந்த ஆல்பம் ஏற்கனவே பிளேலிஸ்ட்டில் உள்ளது",
        addFailed = "ஆல்பத்தைச் சேர்க்க முடியவில்லை",
        albumUnavailable = "ஒரு ஆல்பம் தற்போது கிடைக்கவில்லை",
        inPlaylist = "ஏற்கனவே சேர்க்கப்பட்டது",
        addedToTemplate = "%s இல் ஆல்பம் சேர்க்கப்பட்டது",
        oneAlbum = "1 ஆல்பம்",
        manyAlbums = "%d ஆல்பங்கள்"
    )
    "te" -> PlaylistAlbumCopy(
        addAlbumToPlaylist = "ప్లేజాబితాకు ఆల్బమ్‌ని జోడించండి",
        albumLabel = "ఆల్బమ్",
        openAlbum = "ఆల్బమ్‌ని తెరవండి",
        playAlbum = "ఆల్బమ్ ప్లే చేయండి",
        removeAlbum = "ప్లేజాబితా నుండి ఆల్బమ్‌ను తీసివేయండి",
        alreadyInPlaylist = "ఈ ఆల్బమ్ ఇప్పటికే ప్లేజాబితాలో ఉంది",
        addFailed = "ఆల్బమ్‌ని జోడించడం సాధ్యపడలేదు",
        albumUnavailable = "ఆల్బమ్ ప్రస్తుతం అందుబాటులో లేదు",
        inPlaylist = "ఇప్పటికే జోడించబడింది",
        addedToTemplate = "ఆల్బమ్ %sకి జోడించబడింది",
        oneAlbum = "1 ఆల్బమ్",
        manyAlbums = "%d ఆల్బమ్‌లు"
    )
    "mr" -> PlaylistAlbumCopy(
        addAlbumToPlaylist = "प्लेलिस्टमध्ये अल्बम जोडा",
        albumLabel = "अल्बम",
        openAlbum = "अल्बम उघडा",
        playAlbum = "अल्बम प्ले करा",
        removeAlbum = "प्लेलिस्टमधून अल्बम काढा",
        alreadyInPlaylist = "हा अल्बम आधीपासून प्लेलिस्टमध्ये आहे",
        addFailed = "अल्बम जोडू शकलो नाही",
        albumUnavailable = "अल्बम सध्या अनुपलब्ध आहे",
        inPlaylist = "आधीच जोडले आहे",
        addedToTemplate = "%s मध्ये अल्बम जोडला",
        oneAlbum = "1 अल्बम",
        manyAlbums = "%d अल्बम"
    )
    "gu" -> PlaylistAlbumCopy(
        addAlbumToPlaylist = "પ્લેલિસ્ટમાં આલ્બમ ઉમેરો",
        albumLabel = "આલ્બમ",
        openAlbum = "આલ્બમ ખોલો",
        playAlbum = "આલ્બમ ચલાવો",
        removeAlbum = "પ્લેલિસ્ટમાંથી આલ્બમ દૂર કરો",
        alreadyInPlaylist = "આ આલ્બમ પહેલેથી જ પ્લેલિસ્ટમાં છે",
        addFailed = "આલ્બમ ઉમેરી શકાયું નથી",
        albumUnavailable = "આલ્બમ હાલમાં અનુપલબ્ધ છે",
        inPlaylist = "પહેલેથી જ ઉમેરાયેલ છે",
        addedToTemplate = "%s માં આલ્બમ ઉમેર્યું",
        oneAlbum = "1 આલ્બમ",
        manyAlbums = "%d આલ્બમ્સ"
    )
    "pt-BR" -> PlaylistAlbumCopy(
        addAlbumToPlaylist = "Adicionar álbum à playlist",
        albumLabel = "Álbum",
        openAlbum = "Abrir álbum",
        playAlbum = "Reproduzir álbum",
        removeAlbum = "Remover álbum da playlist",
        alreadyInPlaylist = "Este álbum já está na playlist",
        addFailed = "Não foi possível adicionar o álbum",
        albumUnavailable = "Um álbum está indisponível no momento",
        inPlaylist = "Já adicionado",
        addedToTemplate = "Álbum adicionado a %s",
        oneAlbum = "1 álbum",
        manyAlbums = "Álbuns %d"
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
