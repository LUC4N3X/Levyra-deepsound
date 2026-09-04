from pathlib import Path
import re

app_path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
app_text = app_path.read_text(encoding="utf-8")
viewmodel_path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraScreenViewModels.kt")
viewmodel_text = viewmodel_path.read_text(encoding="utf-8")


def block_end(source: str, start: int) -> int:
    brace = source.index("{", start)
    depth = 0
    quote = None
    escape = False
    i = brace
    while i < len(source):
        ch = source[i]
        if quote is not None:
            if escape:
                escape = False
            elif ch == "\\":
                escape = True
            elif ch == quote:
                quote = None
            i += 1
            continue
        if ch in ('"', "'"):
            quote = ch
            i += 1
            continue
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                return i + 1
        i += 1
    raise RuntimeError("unterminated Kotlin block")


def replace_block(source: str, marker: str, replacement: str) -> str:
    count = source.count(marker)
    if count != 1:
        raise RuntimeError(f"{marker}: expected one match, found {count}")
    start = source.index(marker)
    end = block_end(source, start)
    return source[:start] + replacement.rstrip() + source[end:]


def replace_composable(source: str, marker: str, replacement: str) -> str:
    count = source.count(marker)
    if count != 1:
        raise RuntimeError(f"{marker}: expected one match, found {count}")
    start = source.index(marker)
    end = block_end(source, start)
    return source[:start] + replacement.rstrip() + "\n\n" + source[end:].lstrip("\n")


app_text = replace_block(
    app_text,
    "    val visibleEditorialCollections = remember(editorialCollections, spotlightCandidate?.track?.id) {",
    """    val visibleEditorialCollections = remember(editorialCollections, spotlightCandidate?.track?.id) {
        editorialCollections
            .map { collection ->
                val filteredTracks = collection.tracks.filterNot { it.id == spotlightCandidate?.track?.id }
                collection.copy(tracks = filteredTracks)
            }
            .filter { it.tracks.isNotEmpty() }
    }""",
)

app_text, width_count = re.subn(
    r"private val HOME_COLLECTION_COLUMN_WIDTH = \d+\.dp",
    "private val HOME_COLLECTION_COLUMN_WIDTH = 306.dp",
    app_text,
    count=1,
)
if width_count != 1:
    raise RuntimeError(f"collection width: expected one match, found {width_count}")

app_text = replace_composable(
    app_text,
    "@Composable\nprivate fun HomeEditorialCollectionCard(",
    '''@Composable
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
    val palette = remember(visualIndex) { homeCollectionTilePalette(visualIndex) }
    val shape = RoundedCornerShape(20.dp)
    val cardBrush = remember(palette) {
        Brush.linearGradient(
            colorStops = arrayOf(
                0f to palette.first,
                0.62f to palette.first.copy(alpha = 0.96f),
                1f to palette.second
            )
        )
    }
    val primaryTrack = collection.tracks.firstOrNull()
    val secondaryTrack = collection.tracks.getOrNull(1)
    val artistLine = collection.tracks
        .asSequence()
        .map { it.artist.trim() }
        .filter(String::isNotBlank)
        .distinct()
        .take(2)
        .joinToString(" · ")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(cardBrush)
            .border(Dp.Hairline, Color.White.copy(alpha = 0.18f), shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onOpen)
    ) {
        if (secondaryTrack != null) {
            CoverImage(
                track = secondaryTrack,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 16.dp, y = 18.dp)
                    .size(78.dp)
                    .graphicsLayer { rotationZ = 10f }
                    .clip(RoundedCornerShape(11.dp)),
                highRes = false,
                zoom = 1.02f
            )
        }
        if (primaryTrack != null) {
            CoverImage(
                track = primaryTrack,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 8.dp, y = 10.dp)
                    .size(106.dp)
                    .graphicsLayer { rotationZ = 4f }
                    .clip(RoundedCornerShape(12.dp))
                    .border(Dp.Hairline, Color.White.copy(alpha = 0.20f), RoundedCornerShape(12.dp)),
                highRes = false,
                zoom = 1.02f
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.18f),
                            0.58f to Color.Black.copy(alpha = 0.04f),
                            1f to Color.Transparent
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(196.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (collection.updatedToday) {
                    strings.collectionUpdatedToday.uppercase(Locale.ROOT)
                } else {
                    strings.collectionsTitle.uppercase(Locale.ROOT)
                },
                color = Color.White.copy(alpha = 0.76f),
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.75.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = homeCollectionTitle(strings, collection),
                    color = Color.White,
                    fontSize = 19.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (artistLine.isNotBlank()) {
                    Text(
                        text = artistLine,
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}''',
)

app_text = replace_composable(
    app_text,
    "@Composable\nprivate fun HomeMusicVideoShelf(",
    '''@Composable
private fun HomeMusicVideoShelf(
    title: String,
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: (Track) -> Unit,
    onToggleCurrent: () -> Unit
) {
    val videos = remember(tracks) {
        LevyraPersonalOrbit.distinctRecordings(tracks)
            .take(10)
            .map(::homeMusicVideoPreviewTrack)
    }
    if (videos.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeSectionInset { HomeSectionHeader(title) }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(start = HomeHorizontalInset, end = HomeHorizontalShelfEndPadding)
        ) {
            itemsIndexed(
                items = videos,
                key = { index, track -> "home-video-$index-${LevyraPersonalOrbit.identityKey(track)}" },
                contentType = { _, _ -> "home-video-card" }
            ) { _, track ->
                val active = currentId != null && track.id == currentId
                val shape = RoundedCornerShape(17.dp)
                Column(
                    modifier = Modifier
                        .width(252.dp)
                        .pressable(onClick = { if (active && !isResolving) onToggleCurrent() else onPlay(track) }),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(shape)
                            .border(
                                if (active) 1.5.dp else Dp.Hairline,
                                if (active) LevyraCyan.copy(alpha = 0.86f) else LevyraAdaptiveSoftHairline,
                                shape
                            )
                    ) {
                        CoverImage(
                            track = track,
                            modifier = Modifier.fillMaxSize(),
                            highRes = true,
                            zoom = 1f
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0f to Color.Black.copy(alpha = 0.04f),
                                            0.56f to Color.Transparent,
                                            1f to Color.Black.copy(alpha = 0.58f)
                                        )
                                    )
                                )
                        )
                        Surface(
                            color = Color.Black.copy(alpha = 0.70f),
                            border = BorderStroke(Dp.Hairline, Color.White.copy(alpha = 0.18f)),
                            shape = CircleShape,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(9.dp)
                                .size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                when {
                                    active && isResolving -> CircularProgressIndicator(
                                        modifier = Modifier.size(17.dp),
                                        strokeWidth = 2.dp,
                                        color = LevyraCyan
                                    )
                                    active && isPlaying -> ActiveTrackEqualizer(
                                        color = LevyraCyan,
                                        isPlaying = true,
                                        width = 16.dp,
                                        height = 12.dp
                                    )
                                    else -> Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = track.title,
                        color = LevyraText,
                        fontSize = 15.sp,
                        lineHeight = LevyraTypeRhythm.lineHeight(15.sp),
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        color = LevyraMuted,
                        fontSize = 12.sp,
                        lineHeight = LevyraTypeRhythm.lineHeight(12.sp),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}''',
)

app_text = replace_composable(
    app_text,
    "@Composable\nprivate fun ResonanceShelf(",
    '''@Composable
private fun ResonanceShelf(
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: (Track) -> Unit,
    onPlayAll: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val columns = remember(tracks) { tracks.take(8).chunked(2) }
    if (columns.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeSectionHeader(
            title = strings.voicesTitle,
            subtitle = strings.voicesSubtitle,
            onPlayAll = onPlayAll,
            modifier = Modifier.padding(horizontal = HomeHorizontalInset)
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(start = HomeHorizontalInset, end = HomeHorizontalShelfEndPadding)
        ) {
            itemsIndexed(
                items = columns,
                key = { index, chunk -> "res-column-$index-${chunk.joinToString("-") { it.id }}" },
                contentType = { _, _ -> "home-resonance-column" }
            ) { _, chunk ->
                Column(
                    modifier = Modifier.width(310.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chunk.forEach { track ->
                        val active = track.id == currentId
                        val shape = RoundedCornerShape(13.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp)
                                .clip(shape)
                                .background(
                                    if (active) LevyraCyan.copy(alpha = if (LevyraIsLight) 0.10f else 0.11f)
                                    else LevyraAdaptiveCard
                                )
                                .border(
                                    if (active) 1.2.dp else Dp.Hairline,
                                    if (active) LevyraCyan.copy(alpha = 0.72f) else LevyraAdaptiveSoftHairline,
                                    shape
                                )
                                .pressable(onClick = { onPlay(track) }),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(RoundedCornerShape(topStart = 13.dp, bottomStart = 13.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                CoverImage(track = track, modifier = Modifier.fillMaxSize(), highRes = false)
                                if (active) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .background(Color.Black.copy(alpha = 0.30f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isResolving) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(17.dp),
                                                strokeWidth = 2.dp,
                                                color = LevyraCyan
                                            )
                                        } else {
                                            ActiveTrackEqualizer(
                                                color = LevyraCyan,
                                                isPlaying = isPlaying,
                                                width = 16.dp,
                                                height = 11.dp
                                            )
                                        }
                                    }
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 11.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = track.title,
                                    color = if (active) LevyraCyan else LevyraText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist,
                                    color = LevyraMuted,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = if (active && isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = if (active) LevyraCyan else LevyraText.copy(alpha = 0.78f),
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}''',
)

old_mood_flow = "    private val moodSelectionExplicit = MutableStateFlow(false)"
new_mood_flow = "    private val explicitMoodSelection = MutableStateFlow<Mood?>(null)"
if viewmodel_text.count(old_mood_flow) != 1:
    raise RuntimeError("mood flow marker mismatch")
viewmodel_text = viewmodel_text.replace(old_mood_flow, new_mood_flow, 1)

old_combine = '''    internal val renderState: StateFlow<HomeRenderSnapshot> = combine(
        state,
        freezeHomeContent,
        moodSelectionExplicit
    ) { snapshot, freeze, explicitMoodSelection ->
        HomeRenderInput(
            state = if (explicitMoodSelection) snapshot else snapshot.copy(selectedMood = null),
            freezeContent = freeze
        )
    }'''
new_combine = '''    internal val renderState: StateFlow<HomeRenderSnapshot> = combine(
        state,
        freezeHomeContent,
        explicitMoodSelection
    ) { snapshot, freeze, selectedMood ->
        HomeRenderInput(
            state = snapshot.copy(selectedMood = selectedMood),
            freezeContent = freeze
        )
    }'''
if viewmodel_text.count(old_combine) != 1:
    raise RuntimeError("mood combine marker mismatch")
viewmodel_text = viewmodel_text.replace(old_combine, new_combine, 1)

old_select = '''    fun selectMood(mood: Mood) {
        root.selectMood(mood)
        moodSelectionExplicit.value = true
    }'''
new_select = '''    fun selectMood(mood: Mood) {
        explicitMoodSelection.value = mood
        root.selectMood(mood)
    }'''
if viewmodel_text.count(old_select) != 1:
    raise RuntimeError("mood select marker mismatch")
viewmodel_text = viewmodel_text.replace(old_select, new_select, 1)

app_path.write_text(app_text, encoding="utf-8")
viewmodel_path.write_text(viewmodel_text, encoding="utf-8")
