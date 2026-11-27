package com.luc4n3x.levyra.ui.i18n

internal data class MonoPlayerCopy(
    val upNext: String,
    val lyricsTab: String,
    val similar: String,
    val lyricsPreview: String,
    val showLyrics: String,
    val aboutArtist: String,
    val credits: String,
    val mainArtist: String,
    val moveUp: String,
    val moveDown: String,
    val explicit: String,
    val syncedLyrics: String
)

internal object LevyraMonoPlayerStrings {
    private val copies = mapOf(
        "en" to MonoPlayerCopy(
            upNext = "Up next",
            lyricsTab = "Lyrics",
            similar = "Similar",
            lyricsPreview = "Lyrics preview",
            showLyrics = "Show lyrics",
            aboutArtist = "About the artist",
            credits = "Credits",
            mainArtist = "Main artist",
            moveUp = "Move up",
            moveDown = "Move down",
            explicit = "Explicit",
            syncedLyrics = "Synced"
        ),
        "it" to MonoPlayerCopy(
            upNext = "A seguire",
            lyricsTab = "Testo",
            similar = "Simili",
            lyricsPreview = "Anteprima testo",
            showLyrics = "Mostra testo",
            aboutArtist = "L'artista",
            credits = "Crediti",
            mainArtist = "Artista principale",
            moveUp = "Sposta su",
            moveDown = "Sposta giù",
            explicit = "Esplicito",
            syncedLyrics = "Sincronizzato"
        ),
        "ur" to MonoPlayerCopy(
            upNext = "اگلا",
            lyricsTab = "گانے کے بول",
            similar = "اسی طرح",
            lyricsPreview = "دھن کا پیش نظارہ",
            showLyrics = "دھن دکھائیں۔",
            aboutArtist = "فنکار کے بارے میں",
            credits = "کریڈٹس",
            mainArtist = "مرکزی فنکار",
            moveUp = "اوپر بڑھیں۔",
            moveDown = "نیچے کی طرف بڑھیں۔",
            explicit = "واضح",
            syncedLyrics = "مطابقت پذیر"
        ),
        "ta" to MonoPlayerCopy(
            upNext = "அடுத்தது",
            lyricsTab = "பாடல் வரிகள்",
            similar = "ஒத்த",
            lyricsPreview = "பாடல் வரிகள் முன்னோட்டம்",
            showLyrics = "பாடல் வரிகளைக் காட்டு",
            aboutArtist = "கலைஞர் பற்றி",
            credits = "கடன்கள்",
            mainArtist = "முக்கிய கலைஞர்",
            moveUp = "மேலே செல்லவும்",
            moveDown = "கீழே நகர்த்தவும்",
            explicit = "வெளிப்படையானது",
            syncedLyrics = "ஒத்திசைக்கப்பட்டது"
        ),
        "te" to MonoPlayerCopy(
            upNext = "తదుపరి",
            lyricsTab = "పాట సాహిత్యం",
            similar = "ఇలాంటి",
            lyricsPreview = "సాహిత్యం ప్రివ్యూ",
            showLyrics = "సాహిత్యాన్ని చూపించు",
            aboutArtist = "కళాకారుడి గురించి",
            credits = "క్రెడిట్స్",
            mainArtist = "ప్రధాన కళాకారుడు",
            moveUp = "పైకి కదలండి",
            moveDown = "క్రిందికి తరలించు",
            explicit = "స్పష్టమైన",
            syncedLyrics = "సమకాలీకరించబడింది"
        ),
        "mr" to MonoPlayerCopy(
            upNext = "पुढील वर",
            lyricsTab = "गाण्याचे बोल",
            similar = "तत्सम",
            lyricsPreview = "गीताचे पूर्वावलोकन",
            showLyrics = "बोल दाखवा",
            aboutArtist = "कलाकाराबद्दल",
            credits = "श्रेय",
            mainArtist = "मुख्य कलाकार",
            moveUp = "वर हलवा",
            moveDown = "खाली हलवा",
            explicit = "स्पष्ट",
            syncedLyrics = "समक्रमित"
        ),
        "gu" to MonoPlayerCopy(
            upNext = "આગળ",
            lyricsTab = "ગીતના બોલ",
            similar = "સમાન",
            lyricsPreview = "ગીતોનું પૂર્વાવલોકન",
            showLyrics = "ગીતો બતાવો",
            aboutArtist = "કલાકાર વિશે",
            credits = "શ્રેય",
            mainArtist = "મુખ્ય કલાકાર",
            moveUp = "ઉપર ખસેડો",
            moveDown = "નીચે ખસેડો",
            explicit = "સ્પષ્ટ",
            syncedLyrics = "સમન્વયિત"
        ),
        "pt-BR" to MonoPlayerCopy(
            upNext = "A seguir",
            lyricsTab = "Letra",
            similar = "Semelhante",
            lyricsPreview = "Visualização da letra",
            showLyrics = "Mostrar letras",
            aboutArtist = "Sobre o artista",
            credits = "Créditos",
            mainArtist = "Artista principal",
            moveUp = "Subir",
            moveDown = "Mover para baixo",
            explicit = "Explícito",
            syncedLyrics = "Sincronizado"
        )
    )

    fun forCode(code: String): MonoPlayerCopy = localizedValueOrEnglish(copies, code)
}
