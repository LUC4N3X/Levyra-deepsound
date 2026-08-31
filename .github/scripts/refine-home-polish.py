from pathlib import Path

path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
text = path.read_text(encoding="utf-8")


def replace_exact(old: str, new: str, label: str) -> None:
    global text
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    text = text.replace(old, new, 1)


def replace_between(start_marker: str, end_marker: str, replacement: str, label: str) -> None:
    global text
    start = text.find(start_marker)
    end = text.find(end_marker, start + len(start_marker))
    if start < 0 or end < 0 or end <= start:
        raise RuntimeError(f"{label}: markers not found")
    text = text[:start] + replacement.rstrip() + "\n\n" + text[end:]

replace_exact(
    "import androidx.compose.foundation.text.BasicTextField\n",
    "import androidx.compose.foundation.text.BasicTextField\nimport androidx.compose.foundation.text.TextAutoSize\n",
    "TextAutoSize import"
)
replace_exact(
    "    var addTarget by remember { mutableStateOf<Track?>(null) }\n",
    "    var addTarget by remember { mutableStateOf<Track?>(null) }\n    var selectedHomeCollection by remember { mutableStateOf<HomeEditorialCollection?>(null) }\n",
    "selected collection state"
)
replace_exact(
    "                            onOpen = { collection -> viewModel.playAll(collection.tracks) }\n",
    "                            onOpen = { collection -> selectedHomeCollection = collection }\n",
    "collection open behavior"
)
replace_exact(
    "            .height(LevyraHomeDesign.HeroHeight)\n",
    "            .heightIn(min = LevyraHomeDesign.HeroHeight)\n",
    "hero scalable height"
)

old_home_end = '''    addTarget?.let { track ->
        AddToPlaylistDialog(
            track = track,
            playlists = state.playlists,
            onDismiss = { addTarget = null },
            onAddTo = { playlistId ->
                viewModel.addToPlaylist(playlistId, track)
                addTarget = null
            },
            onCreateWith = { name ->
                viewModel.createPlaylist(name, track)
                addTarget = null
            }
        )
    }
}
'''
new_home_end = '''    addTarget?.let { track ->
        AddToPlaylistDialog(
            track = track,
            playlists = state.playlists,
            onDismiss = { addTarget = null },
            onAddTo = { playlistId ->
                viewModel.addToPlaylist(playlistId, track)
                addTarget = null
            },
            onCreateWith = { name ->
                viewModel.createPlaylist(name, track)
                addTarget = null
            }
        )
    }

    selectedHomeCollection?.let { collection ->
        HomeEditorialCollectionDialog(
            collection = collection,
            currentId = state.currentTrack?.id,
            isPlaying = state.isPlaying,
            isResolving = state.isResolving,
            onDismiss = { selectedHomeCollection = null },
            onPlay = { track -> viewModel.playFrom(collection.tracks, track) },
            onPlayAll = { viewModel.playAll(collection.tracks) },
            onTrackActions = onTrackActions
        )
    }
}
'''
replace_exact(old_home_end, new_home_end, "home collection dialog attachment")

replace_exact(
    '''        HomeSectionInset {
            HomeCompactPlayAllHeader(
                title = LocalLevyraStrings.current.personalOrbitTitle,
                onPlayAll = onPlayAll
            )
        }
''',
    '''        HomeSectionInset {
            HomeOrbitHeader(onPlayAll = onPlayAll)
        }
''',
    "orbit header"
)

orbit_header = r'''@Composable
private fun HomeOrbitHeader(onPlayAll: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = strings.personalOrbitTitle,
                color = LevyraText,
                fontSize = 23.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(23.sp),
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.45).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier.height(48.dp).pressable(onClick = onPlayAll),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = LevyraPanelSoft.copy(alpha = if (LevyraIsLight) 0.72f else 0.48f),
                    border = BorderStroke(Dp.Hairline, LevyraAdaptiveSoftHairline),
                    shape = CircleShape,
                    modifier = Modifier.height(38.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = LevyraText.copy(alpha = 0.86f), modifier = Modifier.size(15.dp))
                        Text(strings.playAll, color = LevyraText, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
        Text(
            text = strings.personalOrbitSubtitle,
            color = LevyraMuted,
            style = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.Medium),
            autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 13.5.sp, stepSize = 0.5.sp),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier.fillMaxWidth()
        )
    }
}'''
marker = "@Composable\nprivate fun PersonalListeningShelf("
index = text.find(marker)
if index < 0:
    raise RuntimeError("HomeOrbitHeader insertion marker not found")
text = text[:index] + orbit_header + "\n\n" + text[index:]

palette_helper = r'''private fun homeCollectionSpotifyPalette(collection: HomeEditorialCollection): Pair<Color, Color> {
    val palettes = listOf(
        Color(0xFFE13300) to Color(0xFF8A1D00),
        Color(0xFF8D67AB) to Color(0xFF50305F),
        Color(0xFF1E3264) to Color(0xFF315C98),
        Color(0xFF148A08) to Color(0xFF0B4F08),
        Color(0xFFE8115B) to Color(0xFF8D123A),
        Color(0xFF006450) to Color(0xFF0B8D74),
        Color(0xFFB49BC8) to Color(0xFF6E4C82),
        Color(0xFFD84000) to Color(0xFF8D2D00),
        Color(0xFF503750) to Color(0xFF826182),
        Color(0xFF7358FF) to Color(0xFF3E2CB0)
    )
    val key = "${collection.kind}:${collection.id}:${collection.titleOverride}"
    return palettes[(key.hashCode() and Int.MAX_VALUE) % palettes.size]
}'''
marker = "@Composable\nprivate fun HomeEditorialCollectionCard("
index = text.find(marker)
if index < 0:
    raise RuntimeError("collection palette insertion marker not found")
text = text[:index] + palette_helper + "\n\n" + text[index:]

collection_card = r'''@Composable
private fun HomeEditorialCollectionCard(
    collection: HomeEditorialCollection,
    animationsEnabled: Boolean,
    onOpen: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val effectiveAnimationsEnabled = animationsEnabled && LocalAnimationsEnabled.current
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && effectiveAnimationsEnabled) 0.982f else 1f,
        animationSpec = tween(140, easing = FastOutSlowInEasing),
        label = "homeCollectionScale-${collection.id}"
    )
    val palette = remember(collection.id, collection.kind, collection.titleOverride) {
        homeCollectionSpotifyPalette(collection)
    }
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(122.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(8.dp, shape, clip = false)
            .clip(shape)
            .background(Brush.linearGradient(listOf(palette.first, palette.first.copy(alpha = 0.96f), palette.second)))
            .clickable(interactionSource = interaction, indication = null, onClick = onOpen)
    ) {
        Text(
            text = homeCollectionTitle(strings, collection),
            color = Color.White,
            fontSize = 18.5.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(164.dp).padding(start = 15.dp, top = 15.dp)
        )
        HomeEditorialArtworkStack(
            tracks = collection.tracks,
            modifier = Modifier.align(Alignment.BottomEnd).width(112.dp).height(112.dp)
        )
    }
}'''
replace_between(
    "@Composable\nprivate fun HomeEditorialCollectionCard(",
    "@Composable\nprivate fun HomeEditorialArtworkStack(",
    collection_card,
    "spotify collection card"
)

artwork_stack = r'''@Composable
private fun HomeEditorialArtworkStack(
    tracks: List<Track>,
    modifier: Modifier = Modifier
) {
    val artwork = tracks.firstOrNull()
    Box(modifier = modifier, contentAlignment = Alignment.BottomEnd) {
        if (artwork != null) {
            CoverImage(
                track = artwork,
                modifier = Modifier
                    .size(92.dp)
                    .offset(x = 14.dp, y = 12.dp)
                    .graphicsLayer { rotationZ = 12f }
                    .shadow(10.dp, RoundedCornerShape(8.dp), clip = false)
                    .clip(RoundedCornerShape(8.dp)),
                highRes = false,
                zoom = 1.02f
            )
        }
    }
}'''
replace_between(
    "@Composable\nprivate fun HomeEditorialArtworkStack(",
    "@Composable\nprivate fun CollectionArtworkMosaic(",
    artwork_stack,
    "spotify collection artwork"
)

collection_dialog = r'''@Composable
private fun HomeEditorialCollectionDialog(
    collection: HomeEditorialCollection,
    currentId: String?,
    isPlaying: Boolean,
    isResolving: Boolean,
    onDismiss: () -> Unit,
    onPlay: (Track) -> Unit,
    onPlayAll: () -> Unit,
    onTrackActions: (Track) -> Unit
) {
    val strings = LocalLevyraStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (LevyraIsLight) Color(0xFFF6F6F6) else Color(0xFF121212),
        title = {
            Text(
                text = homeCollectionTitle(strings, collection),
                color = LevyraText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)) {
                itemsIndexed(collection.tracks, key = { index, track -> "collection-${collection.id}-$index-${LevyraPersonalOrbit.identityKey(track)}" }) { _, track ->
                    val active = track.id == currentId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 62.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .then(if (active) Modifier.background(LevyraCyan.copy(alpha = 0.07f)) else Modifier)
                            .pressable(onClick = { onPlay(track) })
                            .padding(start = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp)
                    ) {
                        Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                            CoverImage(track = track, modifier = Modifier.fillMaxSize(), highRes = false)
                            if (active) {
                                Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                                    if (isResolving) CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp, color = LevyraCyan)
                                    else ActiveTrackEqualizer(LevyraCyan, isPlaying, width = 16.dp, height = 11.dp)
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(track.title, color = if (active) LevyraCyan else LevyraText, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, color = LevyraMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { onTrackActions(track) }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = strings.songOptions, tint = LevyraMuted)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onPlayAll) {
                Icon(Icons.Rounded.PlayArrow, null, tint = LevyraCyan, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text(strings.playAll, color = LevyraCyan, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.close, color = LevyraMuted, fontWeight = FontWeight.Bold) } }
    )
}'''
marker = "@Composable\nprivate fun CollectionArtworkMosaic("
index = text.find(marker)
if index < 0:
    raise RuntimeError("collection dialog insertion marker not found")
text = text[:index] + collection_dialog + "\n\n" + text[index:]

artist_item = r'''@Composable
private fun ArtistHitShelfItem(
    artist: ArtistHit,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(164.dp).pressable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StableRemoteArtwork(
            url = artist.thumbnailUrl,
            contentDescription = artist.name,
            modifier = Modifier.size(154.dp).shadow(7.dp, CircleShape, clip = false).clip(CircleShape),
            contentScale = ContentScale.Crop,
            highRes = true
        )
        Text(
            text = artist.name,
            color = LevyraText,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}'''
replace_between(
    "@Composable\nprivate fun ArtistHitShelfItem(",
    "@Composable\nprivate fun TrendingArtistLoadingItem(",
    artist_item,
    "spotify artist item"
)

loading_item = r'''@Composable
private fun TrendingArtistLoadingItem() {
    Column(
        modifier = Modifier.width(164.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(154.dp).clip(CircleShape).levyraShimmer().background(CinematicGlassDeep)
        )
        Box(
            modifier = Modifier.fillMaxWidth(0.74f).height(15.dp).clip(RoundedCornerShape(99.dp)).levyraShimmer().background(CinematicGlassDeep)
        )
    }
}'''
replace_between(
    "@Composable\nprivate fun TrendingArtistLoadingItem(",
    "@Composable\nprivate fun ResonanceShelf(",
    loading_item,
    "spotify artist loading item"
)

path.write_text(text, encoding="utf-8")
