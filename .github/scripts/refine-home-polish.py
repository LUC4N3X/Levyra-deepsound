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
    "autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 13.5.sp, stepSize = 0.5.sp),",
    "autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = 13.5.sp, stepSize = 0.5.sp),",
    "single-line Orbit subtitle scaling"
)

replace_exact(
    '''    var homeAccentStart by remember { mutableStateOf(Color(0xFF071019)) }
    var homeAccentEnd by remember { mutableStateOf(Color(0xFF160E24)) }
''',
    '''    var homeAccentStart by remember(spotlightCandidate?.track?.id) {
        mutableStateOf(Color(spotlightCandidate?.track?.accentStart ?: 0xFF071019))
    }
    var homeAccentEnd by remember(spotlightCandidate?.track?.id) {
        mutableStateOf(Color(spotlightCandidate?.track?.accentEnd ?: 0xFF160E24))
    }
    LaunchedEffect(
        spotlightCandidate?.track?.id,
        spotlightCandidate?.track?.thumbnailUrl,
        spotlightCandidate?.track?.largeThumbnailUrl
    ) {
        val track = spotlightCandidate?.track ?: return@LaunchedEffect
        val paletteKey = ArtworkPaletteCache.key(
            trackId = track.id,
            thumbnailUrl = track.thumbnailUrl,
            largeThumbnailUrl = track.largeThumbnailUrl
        )
        val cachedPalette = ArtworkPaletteCache.peek(paletteKey)
            ?: ArtworkPaletteCache.load(context, paletteKey)
        val palette = cachedPalette ?: ArtworkPalette(track.accentStart, track.accentEnd)
        homeAccentStart = Color(palette.start)
        homeAccentEnd = Color(palette.end)
    }
''',
    "eager Spotlight atmosphere palette"
)

collections_shelf = r'''@Composable
private fun HomeEditorialCollectionsShelf(
    collections: List<HomeEditorialCollection>,
    animationsEnabled: Boolean,
    onOpen: (HomeEditorialCollection) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val indexedColumns = remember(collections) {
        collections.mapIndexed { index, collection -> index to collection }.chunked(2)
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeSectionHeader(
            title = strings.collectionsTitle,
            subtitle = strings.collectionsSubtitle,
            modifier = Modifier.padding(horizontal = HomeHorizontalInset)
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(start = HomeHorizontalInset, end = 42.dp)
        ) {
            itemsIndexed(
                items = indexedColumns,
                key = { _, column -> column.joinToString(prefix = "home-collection-column-") { it.second.id } },
                contentType = { _, _ -> "home-collection-column" }
            ) { _, column ->
                Column(
                    modifier = Modifier.width(286.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    column.forEach { (visualIndex, collection) ->
                        HomeEditorialCollectionCard(
                            collection = collection,
                            visualIndex = visualIndex,
                            animationsEnabled = animationsEnabled,
                            onOpen = { onOpen(collection) }
                        )
                    }
                }
            }
        }
    }
}'''
replace_between(
    "@Composable\nprivate fun HomeEditorialCollectionsShelf(",
    "private fun homeCollectionSpotifyPalette(",
    collections_shelf,
    "spotify collection shelf indexing"
)

palette_and_card = r'''private fun homeCollectionSpotifyPalette(visualIndex: Int): Pair<Color, Color> {
    val palettes = listOf(
        Color(0xFFE13300) to Color(0xFF8A1D00),
        Color(0xFF8D67AB) to Color(0xFF553869),
        Color(0xFF1E3264) to Color(0xFF315C98),
        Color(0xFFE8115B) to Color(0xFF8D123A),
        Color(0xFF006450) to Color(0xFF0B8D74),
        Color(0xFF7358FF) to Color(0xFF3E2CB0),
        Color(0xFFBA5D07) to Color(0xFF7A3502),
        Color(0xFFB02897) to Color(0xFF6F175F),
        Color(0xFF0D73EC) to Color(0xFF12488F),
        Color(0xFFA56752) to Color(0xFF62392D),
        Color(0xFF477D95) to Color(0xFF294A59),
        Color(0xFFC2552D) to Color(0xFF75311A)
    )
    return palettes[visualIndex.mod(palettes.size)]
}

@Composable
private fun HomeEditorialCollectionCard(
    collection: HomeEditorialCollection,
    visualIndex: Int,
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
    val palette = remember(visualIndex) { homeCollectionSpotifyPalette(visualIndex) }
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(122.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(8.dp, shape, clip = false)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to palette.first,
                        0.68f to palette.first.copy(alpha = 0.96f),
                        1f to palette.second
                    )
                )
            )
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
    "private fun homeCollectionSpotifyPalette(",
    "@Composable\nprivate fun HomeEditorialArtworkStack(",
    palette_and_card,
    "spotify collection palette and card"
)

artist_item = r'''@Composable
private fun ArtistHitShelfItem(
    artist: ArtistHit,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.width(132.dp).pressable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        StableRemoteArtwork(
            url = artist.thumbnailUrl,
            contentDescription = artist.name,
            modifier = Modifier.size(124.dp).clip(CircleShape),
            contentScale = ContentScale.Crop,
            highRes = true
        )
        Text(
            text = artist.name,
            color = LevyraText,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.SemiBold,
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
    "spotify artist shelf item"
)

loading_item = r'''@Composable
private fun TrendingArtistLoadingItem() {
    Column(
        modifier = Modifier.width(132.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            modifier = Modifier.size(124.dp).clip(CircleShape).levyraShimmer().background(CinematicGlassDeep)
        )
        Box(
            modifier = Modifier.fillMaxWidth(0.72f).height(14.dp).clip(RoundedCornerShape(99.dp)).levyraShimmer().background(CinematicGlassDeep)
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
