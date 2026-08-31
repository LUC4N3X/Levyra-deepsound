from pathlib import Path

path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
text = path.read_text(encoding="utf-8")


def function_bounds(source: str, name: str) -> tuple[int, int]:
    marker = f"private fun {name}("
    fn_start = source.find(marker)
    if fn_start < 0:
        raise RuntimeError(f"Function not found: {name}")
    annotation = source.rfind("@Composable", 0, fn_start)
    start = annotation if annotation >= 0 and source[annotation:fn_start].strip() == "@Composable" else fn_start
    brace = source.find("{", fn_start)
    if brace < 0:
        raise RuntimeError(f"Opening brace not found: {name}")
    depth = 0
    for index in range(brace, len(source)):
        if source[index] == "{":
            depth += 1
        elif source[index] == "}":
            depth -= 1
            if depth == 0:
                return start, index + 1
    raise RuntimeError(f"Closing brace not found: {name}")


def replace_function(source: str, name: str, replacement: str) -> str:
    start, end = function_bounds(source, name)
    return source[:start] + replacement.rstrip() + "\n\n" + source[end:].lstrip("\n")


shelf = r'''@Composable
private fun PersonalListeningShelf(
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: (Track) -> Unit,
    onPlayAll: () -> Unit,
    onTrackActions: (Track) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val haptics = LocalLevyraHaptics.current
    val shelfTracks = remember(tracks) {
        LevyraPersonalOrbit.distinctRecordings(tracks)
            .take(LevyraPersonalOrbit.DISPLAY_LIMIT)
    }
    val pages = remember(shelfTracks) { shelfTracks.chunked(4) }
    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HomeHorizontalInset),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = strings.personalOrbitTitle,
                    color = LevyraText,
                    fontSize = 23.sp,
                    lineHeight = LevyraTypeRhythm.lineHeight(23.sp),
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.45).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = strings.personalOrbitSubtitle,
                    color = LevyraMuted,
                    fontSize = 13.sp,
                    lineHeight = LevyraTypeRhythm.lineHeight(13.sp),
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                color = Color.Transparent,
                border = BorderStroke(Dp.Hairline, LevyraAdaptiveSoftHairline),
                shape = CircleShape,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .pressable(onClick = onPlayAll)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = LevyraText.copy(alpha = 0.88f),
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = strings.playAll,
                        color = LevyraText,
                        fontSize = 12.5.sp,
                        lineHeight = LevyraTypeRhythm.lineHeight(12.5.sp),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(246.dp),
            contentPadding = PaddingValues(horizontal = HomeHorizontalInset),
            pageSpacing = 12.dp
        ) { pageIndex ->
            val pageTracks = pages.getOrElse(pageIndex) { emptyList() }
            val featured = pageTracks.firstOrNull()
            val satellites = pageTracks.drop(1).take(3)
            val accentStart = remember(featured?.id, featured?.accentStart) {
                featured?.let { Color(it.accentStart) } ?: LevyraCyan
            }
            val accentEnd = remember(featured?.id, featured?.accentEnd) {
                featured?.let { Color(it.accentEnd) } ?: LevyraViolet
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    accentStart.copy(alpha = if (LevyraIsLight) 0.12f else 0.16f),
                                    accentEnd.copy(alpha = if (LevyraIsLight) 0.06f else 0.09f),
                                    Color.Transparent
                                ),
                                radius = 520f
                            )
                        )
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    if (featured != null) {
                        PersonalListeningCard(
                            track = featured,
                            active = featured.id == currentId,
                            playing = isPlaying && featured.id == currentId,
                            resolving = isResolving && featured.id == currentId,
                            onClick = { onPlay(featured) },
                            modifier = Modifier
                                .weight(1.76f)
                                .fillMaxHeight(),
                            onLongClick = {
                                haptics.perform(LevyraHapticAction.TrackSwipe)
                                onTrackActions(featured)
                            },
                            onLongClickLabel = strings.songOptions
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1.76f).fillMaxHeight())
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(3) { index ->
                            val track = satellites.getOrNull(index)
                            if (track != null) {
                                PersonalOrbitSatelliteCard(
                                    track = track,
                                    active = track.id == currentId,
                                    playing = isPlaying && track.id == currentId,
                                    resolving = isResolving && track.id == currentId,
                                    onClick = { onPlay(track) },
                                    onLongClick = {
                                        haptics.perform(LevyraHapticAction.TrackSwipe)
                                        onTrackActions(track)
                                    },
                                    onLongClickLabel = strings.songOptions,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f).fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }

        if (pages.size > 1) {
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.indices.forEach { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .width(if (selected) 18.dp else 6.dp)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) LevyraText.copy(alpha = 0.92f)
                                else LevyraMuted.copy(alpha = 0.30f)
                            )
                    )
                }
            }
        }
    }
}'''

featured_card = r'''@Composable
private fun PersonalListeningCard(
    track: Track,
    active: Boolean,
    playing: Boolean,
    resolving: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null
) {
    val artworkShape = RoundedCornerShape(20.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val effectiveAnimationsEnabled = LocalAnimationsEnabled.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && effectiveAnimationsEnabled) 0.985f else 1f,
        animationSpec = tween(130, easing = FastOutSlowInEasing),
        label = "personalOrbitFeaturedScale-${track.id}"
    )
    val accentStart = remember(track.id, track.accentStart) { Color(track.accentStart) }
    val accentEnd = remember(track.id, track.accentEnd) { Color(track.accentEnd) }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(artworkShape)
            .border(
                width = if (active) 1.5.dp else Dp.Hairline,
                color = if (active) LevyraCyan.copy(alpha = 0.88f) else LevyraAdaptiveSoftHairline,
                shape = artworkShape
            )
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = onLongClickLabel
            )
    ) {
        CoverImage(
            track = track,
            modifier = Modifier.fillMaxSize(),
            highRes = false,
            zoom = 1.02f
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to accentStart.copy(alpha = 0.08f),
                            0.46f to Color.Transparent,
                            0.70f to Color.Black.copy(alpha = 0.18f),
                            1f to Color.Black.copy(alpha = 0.88f)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            accentStart.copy(alpha = 0.08f),
                            Color.Transparent,
                            accentEnd.copy(alpha = 0.04f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = track.title,
                color = Color.White,
                fontSize = 18.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(18.sp),
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.25).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 12.5.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(12.5.sp),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (active) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(11.dp)
                    .size(34.dp)
                    .background(Color.Black.copy(alpha = 0.64f), CircleShape)
                    .border(Dp.Hairline, Color.White.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (resolving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(15.dp),
                        strokeWidth = 1.8.dp,
                        color = LevyraCyan
                    )
                } else {
                    ActiveTrackEqualizer(
                        color = LevyraCyan,
                        isPlaying = playing,
                        width = 15.dp,
                        height = 11.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonalOrbitSatelliteCard(
    track: Track,
    active: Boolean,
    playing: Boolean,
    resolving: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    onLongClickLabel: String?,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val effectiveAnimationsEnabled = LocalAnimationsEnabled.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && effectiveAnimationsEnabled) 0.975f else 1f,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "personalOrbitSatelliteScale-${track.id}"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .border(
                width = if (active) 1.2.dp else Dp.Hairline,
                color = if (active) LevyraCyan.copy(alpha = 0.82f) else LevyraAdaptiveSoftHairline,
                shape = shape
            )
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = onLongClickLabel
            )
    ) {
        CoverImage(
            track = track,
            modifier = Modifier.fillMaxSize(),
            highRes = false,
            zoom = 1.04f
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.70f)
                        )
                    )
                )
        )
        Text(
            text = track.title,
            color = Color.White,
            fontSize = 11.5.sp,
            lineHeight = LevyraTypeRhythm.lineHeight(11.5.sp),
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 7.dp)
        )
        if (active) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(25.dp)
                    .background(Color.Black.copy(alpha = 0.68f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (resolving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(11.dp),
                        strokeWidth = 1.4.dp,
                        color = LevyraCyan
                    )
                } else {
                    ActiveTrackEqualizer(
                        color = LevyraCyan,
                        isPlaying = playing,
                        width = 11.dp,
                        height = 8.dp
                    )
                }
            }
        }
    }
}'''

text = replace_function(text, "PersonalListeningShelf", shelf)
text = replace_function(text, "PersonalListeningCard", featured_card)
path.write_text(text, encoding="utf-8")
