from pathlib import Path
import re

path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
text = path.read_text(encoding="utf-8")


def replace_once(old: str, new: str, label: str) -> None:
    global text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"patch target count for {label}: {count}")
    text = text.replace(old, new, 1)


replace_once(
    '''    val spotlightCandidate = remember(spotlightCandidates, stableSpotlightId) {
        spotlightCandidates.firstOrNull { it.track.id == stableSpotlightId }
            ?: spotlightCandidates.firstOrNull { it.track.id != state.currentTrack?.id }
            ?: spotlightCandidates.firstOrNull()
    }
    LaunchedEffect(spotlightDayKey, spotlightCandidate?.track?.id, spotlightCandidates) {
        val selectedStillAvailable = stableSpotlightId != null && spotlightCandidates.any { it.track.id == stableSpotlightId }
        if (!selectedStillAvailable && spotlightCandidate != null) {
            stableSpotlightId = spotlightCandidate.track.id
        }
    }
    val spotlightTracks = remember(spotlightCandidates) { spotlightCandidates.map { it.track } }
''',
    '''    val soundtrackArtistPool = remember(
        state.followedArtists,
        state.homeArtists,
        state.similarArtists
    ) {
        (state.followedArtists + state.homeArtists + state.similarArtists)
            .filter { it.thumbnailUrl.isNotBlank() }
            .distinctBy { artist ->
                artist.browseId.ifBlank { artist.name.trim().lowercase(Locale.ROOT) }
            }
    }
    val spotlightCandidate = remember(
        spotlightCandidates,
        stableSpotlightId,
        soundtrackArtistPool,
        state.currentTrack?.id
    ) {
        fun hasArtistPortrait(candidate: HomeSpotlightCandidate): Boolean {
            return soundtrackArtistPool.any { artist ->
                homeSoundtrackMatchesArtist(candidate.track, artist.name, artist.browseId)
            }
        }
        spotlightCandidates.firstOrNull {
            it.track.id == stableSpotlightId && hasArtistPortrait(it)
        }
            ?: spotlightCandidates.firstOrNull {
                it.track.id != state.currentTrack?.id && hasArtistPortrait(it)
            }
            ?: spotlightCandidates.firstOrNull(::hasArtistPortrait)
            ?: spotlightCandidates.firstOrNull { it.track.id == stableSpotlightId }
            ?: spotlightCandidates.firstOrNull { it.track.id != state.currentTrack?.id }
            ?: spotlightCandidates.firstOrNull()
    }
    LaunchedEffect(spotlightDayKey, spotlightCandidate?.track?.id, spotlightCandidates) {
        if (spotlightCandidate != null && stableSpotlightId != spotlightCandidate.track.id) {
            stableSpotlightId = spotlightCandidate.track.id
        }
    }
    val spotlightTracks = remember(spotlightCandidates) { spotlightCandidates.map { it.track } }
''',
    "artist-first spotlight selection",
)

replace_once(
    '''                val soundtrackArtists = homeSoundtrackArtists(spotlightTracks, heroTrack)
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
                    HomeSectionLead(compactHome) {
                        HomeSectionInset {
                            HomeEditorialSpotlight(
                                candidate = candidate,
                                artistArtworkUrl = heroArtistArtworkUrl,
                                soundtrackArtists = soundtrackArtists,
                                isCurrent = heroTrack.id == state.currentTrack?.id,
                                isPlaying = state.isPlaying && heroTrack.id == state.currentTrack?.id,
                                isResolving = state.isResolving && heroTrack.id == state.currentTrack?.id,
                                animationsEnabled = state.animationsEnabled,
                                onPaletteChanged = { start, end ->
                                    homeAccentStart = start
                                    homeAccentEnd = end
                                },
                                onOpen = {
                                    stableSpotlightId = heroTrack.id
                                    viewModel.playFrom(spotlightTracks, heroTrack)
                                }
                            )
                        }
                    }
                }
''',
    '''                val baseSoundtrackArtists = homeSoundtrackArtists(spotlightTracks, heroTrack)
                val heroPortraitArtist = soundtrackArtistPool.firstOrNull { artist ->
                    homeSoundtrackMatchesArtist(heroTrack, artist.name, artist.browseId)
                } ?: soundtrackArtistPool.firstOrNull { artist ->
                    spotlightTracks.any { track ->
                        homeSoundtrackMatchesArtist(track, artist.name, artist.browseId)
                    }
                }
                val soundtrackArtists = buildList {
                    heroPortraitArtist?.name
                        ?.let(::homeSoundtrackPrimaryArtist)
                        ?.takeIf { it.isNotBlank() }
                        ?.let(::add)
                    baseSoundtrackArtists.forEach { artist ->
                        if (none { it.equals(artist, ignoreCase = true) }) add(artist)
                    }
                }.take(3)
                val heroArtistArtworkUrl = heroPortraitArtist?.thumbnailUrl.orEmpty()
                    .ifBlank {
                        soundtrackArtists.asSequence()
                            .mapNotNull { artistName ->
                                soundtrackArtistPool.firstOrNull { artist ->
                                    homeSoundtrackPrimaryArtist(artist.name)
                                        .equals(artistName, ignoreCase = true)
                                }?.thumbnailUrl
                            }
                            .firstOrNull()
                            .orEmpty()
                    }
                item(key = "home-editorial-spotlight", contentType = "home-spotlight") {
                    HomeEditorialSpotlight(
                        candidate = candidate,
                        artistArtworkUrl = heroArtistArtworkUrl,
                        soundtrackArtists = soundtrackArtists,
                        isCurrent = heroTrack.id == state.currentTrack?.id,
                        isPlaying = state.isPlaying && heroTrack.id == state.currentTrack?.id,
                        isResolving = state.isResolving && heroTrack.id == state.currentTrack?.id,
                        animationsEnabled = state.animationsEnabled,
                        onPaletteChanged = { start, end ->
                            homeAccentStart = start
                            homeAccentEnd = end
                        },
                        onOpen = {
                            stableSpotlightId = heroTrack.id
                            viewModel.playFrom(spotlightTracks, heroTrack)
                        }
                    )
                }
''',
    "artist portrait hero and remove inset wrappers",
)

replace_once(
    '''    BoxWithConstraints(
    modifier = Modifier
        .fillMaxWidth()
        .height(408.dp)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clip(LevyraHomeDesign.HeroShape)
        .background(Color(0xFF050609))
        .border(
            Dp.Hairline,
            Color.White.copy(alpha = 0.11f),
            LevyraHomeDesign.HeroShape
        )
        .clickable(
            interactionSource = interaction,
            indication = null,
            onClick = onOpen
        )
) {
''',
    '''    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(520.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onOpen
            )
    ) {
''',
    "full-bleed hero container",
)

replace_once(
    '''                    colorStops = arrayOf(
                        0f to Color.Black.copy(alpha = 0.03f),
                        0.42f to Color.Black.copy(alpha = 0.02f),
                        0.62f to Color.Black.copy(alpha = 0.24f),
                        0.80f to Color.Black.copy(alpha = 0.72f),
                        1f to Color.Black.copy(alpha = 0.97f)
                    )
''',
    '''                    colorStops = arrayOf(
                        0f to Color.Black.copy(alpha = 0.30f),
                        0.12f to Color.Black.copy(alpha = 0.10f),
                        0.48f to Color.Transparent,
                        0.67f to Color.Black.copy(alpha = 0.18f),
                        0.82f to Color.Black.copy(alpha = 0.72f),
                        1f to Color.Black
                    )
''',
    "vertical background blend",
)

replace_once(
    '''                    colorStops = arrayOf(
                        0f to Color.Black.copy(alpha = 0.50f),
                        0.42f to Color.Black.copy(alpha = 0.16f),
                        0.78f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.10f)
                    )
''',
    '''                    colorStops = arrayOf(
                        0f to Color.Black.copy(alpha = 0.42f),
                        0.38f to Color.Black.copy(alpha = 0.12f),
                        0.76f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.06f)
                    )
''',
    "horizontal background blend",
)

replace_once(
    '''            .fillMaxWidth(0.80f)
            .padding(start = 22.dp, end = 12.dp, bottom = 30.dp),
''',
    '''            .fillMaxWidth(0.78f)
            .padding(start = 22.dp, end = 12.dp, bottom = 38.dp),
''',
    "hero copy placement",
)

replace_once(
    '''            fontSize = 38.sp,
            lineHeight = 42.sp,
''',
    '''            fontSize = 40.sp,
            lineHeight = 44.sp,
''',
    "hero title sizing",
)

replace_once(
    '''            .padding(end = 20.dp, bottom = 30.dp)
            .size(68.dp)
''',
    '''            .padding(end = 22.dp, bottom = 42.dp)
            .size(76.dp)
''',
    "hero play control",
)

# Remove the old pressed-card scale because the soundtrack is now a seamless page hero.
text = re.sub(
    r'''    val isPressed by interaction\.collectIsPressedAsState\(\)\n    val scale by animateFloatAsState\(\n.*?        label = "homeSpotlightScale"\n    \)\n''',
    "",
    text,
    count=1,
    flags=re.S,
)

path.write_text(text, encoding="utf-8")
