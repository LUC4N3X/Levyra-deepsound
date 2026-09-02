from pathlib import Path

path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
text = path.read_text()


def replace_once(old: str, new: str, label: str) -> None:
    global text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, found {count}")
    text = text.replace(old, new, 1)


replace_once(
    '''            spotlightCandidate?.let { candidate ->
                val heroTrack = candidate.track
                item(key = "home-editorial-spotlight", contentType = "home-spotlight") {
''',
    '''            spotlightCandidate?.let { candidate ->
                val heroTrack = candidate.track
                val soundtrackArtists = homeSoundtrackArtists(spotlightTracks, heroTrack)
                val heroArtistArtworkUrl = state.followedArtists
                    .firstOrNull { followed ->
                        followed.thumbnailUrl.isNotBlank() &&
                            homeSoundtrackMatchesArtist(heroTrack, followed.name, followed.browseId)
                    }
                    ?.thumbnailUrl
                    .orEmpty()
                    .ifBlank {
                        homeSoundtrackArtistArtwork(
                            track = heroTrack,
                            artists = state.homeArtists + state.similarArtists
                        )
                    }
                item(key = "home-editorial-spotlight", contentType = "home-spotlight") {
''',
    "spotlight call header",
)

replace_once(
    '''                            HomeEditorialSpotlight(
                                candidate = candidate,
                                isCurrent = heroTrack.id == state.currentTrack?.id,
''',
    '''                            HomeEditorialSpotlight(
                                candidate = candidate,
                                artistArtworkUrl = heroArtistArtworkUrl,
                                soundtrackArtists = soundtrackArtists,
                                isCurrent = heroTrack.id == state.currentTrack?.id,
''',
    "spotlight call args",
)

marker = '''@Composable
private fun HomeEditorialSpotlight(
'''
helpers = '''private fun homeSoundtrackPrimaryArtist(rawArtist: String): String {
    var value = rawArtist.trim()
    if (value.isBlank()) return value
    val separators = listOf(",", " & ", " feat. ", " feat ", " featuring ", " x ")
    separators.forEach { separator ->
        val index = value.indexOf(separator, ignoreCase = true)
        if (index > 0) value = value.substring(0, index).trim()
    }
    return value
}

private fun homeSoundtrackArtists(tracks: List<Track>, heroTrack: Track): List<String> {
    return sequenceOf(heroTrack)
        .plus(tracks.asSequence())
        .map { homeSoundtrackPrimaryArtist(it.artist) }
        .filter(String::isNotBlank)
        .distinctBy { it.lowercase(Locale.ROOT) }
        .take(3)
        .toList()
}

private fun homeSoundtrackMatchesArtist(track: Track, artistName: String, browseId: String): Boolean {
    if (artistName.isBlank()) return false
    if (browseId.isNotBlank() && track.artistBrowseIds.any { it.equals(browseId, ignoreCase = true) }) return true
    val primary = homeSoundtrackPrimaryArtist(track.artist)
    return primary.equals(artistName.trim(), ignoreCase = true)
}

private fun homeSoundtrackArtistArtwork(track: Track, artists: List<ArtistHit>): String {
    return artists
        .asSequence()
        .filter { it.thumbnailUrl.isNotBlank() }
        .firstOrNull { homeSoundtrackMatchesArtist(track, it.name, it.browseId) }
        ?.thumbnailUrl
        .orEmpty()
}

private fun homeSoundtrackTitle(strings: LevyraStrings): String = when (strings.code.lowercase(Locale.ROOT)) {
    "it" -> "La mia colonna sonora"
    "es" -> "Mi banda sonora"
    "fr" -> "Ma bande-son"
    "de" -> "Mein Soundtrack"
    "pt" -> "Minha trilha sonora"
    else -> "My soundtrack"
}

private fun homeSoundtrackLead(strings: LevyraStrings, artists: List<String>): String {
    val cleaned = artists.map(String::trim).filter(String::isNotBlank).take(3)
    val joined = when (cleaned.size) {
        0 -> return when (strings.code.lowercase(Locale.ROOT)) {
            "it" -> "La musica scelta per te"
            "es" -> "Música elegida para ti"
            "fr" -> "De la musique choisie pour vous"
            "de" -> "Musik, die für dich ausgewählt wurde"
            "pt" -> "Música escolhida para você"
            else -> "Music picked for you"
        }
        1 -> cleaned[0]
        2 -> when (strings.code.lowercase(Locale.ROOT)) {
            "it", "es", "pt" -> "${cleaned[0]} e ${cleaned[1]}"
            "fr" -> "${cleaned[0]} et ${cleaned[1]}"
            "de" -> "${cleaned[0]} und ${cleaned[1]}"
            else -> "${cleaned[0]} and ${cleaned[1]}"
        }
        else -> when (strings.code.lowercase(Locale.ROOT)) {
            "it", "es", "pt" -> "${cleaned[0]}, ${cleaned[1]} e ${cleaned[2]}"
            "fr" -> "${cleaned[0]}, ${cleaned[1]} et ${cleaned[2]}"
            "de" -> "${cleaned[0]}, ${cleaned[1]} und ${cleaned[2]}"
            else -> "${cleaned[0]}, ${cleaned[1]} and ${cleaned[2]}"
        }
    }
    return when (strings.code.lowercase(Locale.ROOT)) {
        "it" -> "Inizia con $joined"
        "es" -> "Empieza con $joined"
        "fr" -> "Commence avec $joined"
        "de" -> "Startet mit $joined"
        "pt" -> "Começa com $joined"
        else -> "Starts with $joined"
    }
}

'''
if text.count(marker) != 1:
    raise SystemExit(f"spotlight marker: expected exactly one match, found {text.count(marker)}")
text = text.replace(marker, helpers + marker, 1)

replace_once(
    '''private fun HomeEditorialSpotlight(
    candidate: HomeSpotlightCandidate,
    isCurrent: Boolean,
''',
    '''private fun HomeEditorialSpotlight(
    candidate: HomeSpotlightCandidate,
    artistArtworkUrl: String,
    soundtrackArtists: List<String>,
    isCurrent: Boolean,
''',
    "spotlight signature",
)

replace_once(
    '''    val badge = homeSpotlightBadge(strings, candidate)
    val detail = homeSpotlightDetail(strings, candidate)
''',
    '''    val soundtrackTitle = homeSoundtrackTitle(strings)
    val soundtrackLead = homeSoundtrackLead(strings, soundtrackArtists)
''',
    "spotlight copy",
)

replace_once(
    '''    modifier = Modifier
        .fillMaxWidth()
        .height(276.dp)
''',
    '''    modifier = Modifier
        .fillMaxWidth()
        .height(408.dp)
''',
    "spotlight height",
)

replace_once(
    '''    CoverImage(
        track = track,
        modifier = Modifier.fillMaxSize(),
        highRes = true,
        zoom = 1f,
        onImageLoaded = { image ->
            if (!paletteExtractionStarted && loadedArtworkBitmap == null) {
                loadedArtworkBitmap = image.toBitmap()
            }
        }
    )
''',
    '''    if (artistArtworkUrl.isNotBlank()) {
        StableRemoteArtwork(
            url = artistArtworkUrl,
            contentDescription = soundtrackTitle,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            highRes = true
        )
    } else {
        CoverImage(
            track = track,
            modifier = Modifier.fillMaxSize(),
            highRes = true,
            zoom = 1f,
            onImageLoaded = { image ->
                if (!paletteExtractionStarted && loadedArtworkBitmap == null) {
                    loadedArtworkBitmap = image.toBitmap()
                }
            }
        )
    }
''',
    "spotlight artwork",
)

replace_once(
    '''                        0f to Color.Black.copy(alpha = 0.10f),
                        0.28f to Color.Black.copy(alpha = 0.04f),
                        0.58f to Color.Black.copy(alpha = 0.28f),
                        0.78f to Color.Black.copy(alpha = 0.68f),
                        1f to Color.Black.copy(alpha = 0.94f)
''',
    '''                        0f to Color.Black.copy(alpha = 0.03f),
                        0.42f to Color.Black.copy(alpha = 0.02f),
                        0.62f to Color.Black.copy(alpha = 0.24f),
                        0.80f to Color.Black.copy(alpha = 0.72f),
                        1f to Color.Black.copy(alpha = 0.97f)
''',
    "spotlight vertical scrim",
)

replace_once(
    '''    Column(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth(0.78f)
            .padding(start = 22.dp, end = 10.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = badge.uppercase(Locale.ROOT),
            color = accentStart
                .copy(alpha = 0.98f)
                ,
            fontSize = 11.sp,
            lineHeight = LevyraTypeRhythm.lineHeight(11.sp),
            fontWeight = FontWeight.Black,
            letterSpacing = 1.45.sp,
            maxLines = 1
        )
        Text(
            text = track.title,
            color = Color.White,
            fontSize = 31.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.05).sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 16.5.sp,
            lineHeight = LevyraTypeRhythm.lineHeight(16.5.sp),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = detail,
            color = Color.White.copy(alpha = 0.68f),
            fontSize = 13.sp,
            lineHeight = LevyraTypeRhythm.lineHeight(13.sp),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
''',
    '''    Column(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth(0.80f)
            .padding(start = 22.dp, end = 12.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "RADIO",
            color = Color.White.copy(alpha = 0.94f),
            fontSize = 12.sp,
            lineHeight = LevyraTypeRhythm.lineHeight(12.sp),
            fontWeight = FontWeight.Black,
            letterSpacing = 0.45.sp,
            maxLines = 1
        )
        Text(
            text = soundtrackTitle,
            color = Color.White,
            fontSize = 38.sp,
            lineHeight = 42.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.25).sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = soundtrackLead,
            color = Color.White.copy(alpha = 0.96f),
            fontSize = 18.5.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
''',
    "spotlight text",
)

replace_once(
    '''    Surface(
        color = Color.White,
        shape = CircleShape,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.42f)),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 18.dp, bottom = 20.dp)
            .size(60.dp)
''',
    '''    Surface(
        color = LevyraCyan,
        shape = CircleShape,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 20.dp, bottom = 30.dp)
            .size(68.dp)
''',
    "spotlight play button",
)

button_anchor = text.index('    Surface(\n        color = LevyraCyan,')
spinner_old = '                    color = accentStart\n'
spinner_pos = text.find(spinner_old, button_anchor)
if spinner_pos < 0:
    raise SystemExit("spotlight spinner color not found")
text = text[:spinner_pos] + '                    color = Color(0xFF07080C)\n' + text[spinner_pos + len(spinner_old):]

path.write_text(text)
