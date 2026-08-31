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


old_import = "import com.luc4n3x.levyra.data.albumRecommendationDeduplicationKey"
new_import = "import com.luc4n3x.levyra.data.buildPersonalizedHomeAlbumShelf"
if old_import not in text:
    raise RuntimeError("Album shelf import marker drifted")
text = text.replace(old_import, new_import, 1)

old_albums = '''    val homeAlbums = remember(state.homeAlbums) {
        state.homeAlbums
            .filter { album -> album.title.isNotBlank() && album.artist.isNotBlank() }
            .distinctBy(::albumRecommendationDeduplicationKey)
    }
'''
new_albums = '''    val homeAlbums = remember(
        state.homeAlbums,
        state.personalOrbitTracks,
        state.recentListens,
        state.favorites,
        quickPicks?.tracks,
        newReleases?.tracks,
        state.homeSections,
        state.charts,
        state.tracks,
        state.languageCode
    ) {
        buildPersonalizedHomeAlbumShelf(
            primaryAlbums = state.homeAlbums,
            personalTracks = state.personalOrbitTracks,
            recentTracks = state.recentListens,
            favoriteTracks = state.favorites,
            quickPickTracks = quickPicks?.tracks.orEmpty(),
            localizedReleaseTracks = newReleases?.tracks.orEmpty(),
            localizedSections = state.homeSections,
            chartTracks = state.charts,
            fallbackTracks = state.tracks
        )
    }
'''
if old_albums not in text:
    raise RuntimeError("Home album shelf block drifted")
text = text.replace(old_albums, new_albums, 1)

compact_header = r'''@Composable
private fun HomeCompactPlayAllHeader(
    title: String,
    onPlayAll: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
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
            modifier = Modifier
                .height(48.dp)
                .pressable(onClick = onPlayAll),
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
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = LevyraText.copy(alpha = 0.86f),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = strings.playAll,
                        color = LevyraText,
                        fontSize = 11.5.sp,
                        lineHeight = LevyraTypeRhythm.lineHeight(11.5.sp),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}'''

if "private fun HomeCompactPlayAllHeader(" not in text:
    marker = "@Composable\nprivate fun PersonalListeningShelf("
    index = text.find(marker)
    if index < 0:
        raise RuntimeError("Personal Listening insertion marker not found")
    text = text[:index] + compact_header + "\n\n" + text[index:]

personal_shelf = r'''@Composable
private fun PersonalListeningShelf(
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: (Track) -> Unit,
    onPlayAll: () -> Unit,
    onTrackActions: (Track) -> Unit
) {
    val haptics = LocalLevyraHaptics.current
    val shelfTracks = remember(tracks) {
        LevyraPersonalOrbit.distinctRecordings(tracks)
            .take(LevyraPersonalOrbit.DISPLAY_LIMIT)
    }
    val pages = remember(shelfTracks) { shelfTracks.chunked(4) }
    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HomeSectionInset {
            HomeCompactPlayAllHeader(
                title = LocalLevyraStrings.current.personalOrbitTitle,
                onPlayAll = onPlayAll
            )
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
                                    accentStart.copy(alpha = if (LevyraIsLight) 0.10f else 0.14f),
                                    accentEnd.copy(alpha = if (LevyraIsLight) 0.05f else 0.08f),
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
                            onLongClickLabel = LocalLevyraStrings.current.songOptions
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
                                    onLongClickLabel = LocalLevyraStrings.current.songOptions,
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
text = replace_function(text, "PersonalListeningShelf", personal_shelf)

old_quick_header = '''        HomeSectionInset {
            SectionHeaderAction(title, onPlayAll)
        }
'''
new_quick_header = '''        HomeSectionInset {
            HomeCompactPlayAllHeader(title = title, onPlayAll = onPlayAll)
        }
'''
if old_quick_header not in text:
    raise RuntimeError("Quick Picks header marker drifted")
text = text.replace(old_quick_header, new_quick_header, 1)

collections_shelf = r'''@Composable
private fun HomeEditorialCollectionsShelf(
    collections: List<HomeEditorialCollection>,
    animationsEnabled: Boolean,
    onOpen: (HomeEditorialCollection) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val columns = remember(collections) { collections.chunked(2) }
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
                items = columns,
                key = { _, column -> column.joinToString(prefix = "home-collection-column-") { it.id } },
                contentType = { _, _ -> "home-collection-column" }
            ) { _, column ->
                Column(
                    modifier = Modifier.width(286.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    column.forEach { collection ->
                        HomeEditorialCollectionCard(
                            collection = collection,
                            animationsEnabled = animationsEnabled,
                            onOpen = { onOpen(collection) }
                        )
                    }
                }
            }
        }
    }
}'''
text = replace_function(text, "HomeEditorialCollectionsShelf", collections_shelf)

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
        targetValue = if (isPressed && effectiveAnimationsEnabled) 0.985f else 1f,
        animationSpec = tween(140, easing = FastOutSlowInEasing),
        label = "homeCollectionScale-${collection.id}"
    )
    val primaryTrack = collection.tracks.firstOrNull()
    val secondaryTrack = collection.tracks.getOrNull(1)
    val accentStart = remember(primaryTrack?.id, primaryTrack?.accentStart, collection.accentStart) {
        Color(primaryTrack?.accentStart ?: collection.accentStart)
    }
    val accentEnd = remember(secondaryTrack?.id, secondaryTrack?.accentEnd, primaryTrack?.accentEnd, collection.accentEnd) {
        Color(secondaryTrack?.accentEnd ?: primaryTrack?.accentEnd ?: collection.accentEnd)
    }
    val shape = RoundedCornerShape(22.dp)
    val baseColor = if (LevyraIsLight) Color(0xFFF2F3F6) else Color(0xFF121216)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(baseColor)
            .background(
                Brush.linearGradient(
                    colorStops = arrayOf(
                        0f to accentStart.copy(alpha = if (LevyraIsLight) 0.34f else 0.46f),
                        0.58f to accentEnd.copy(alpha = if (LevyraIsLight) 0.20f else 0.28f),
                        1f to Color.Transparent
                    )
                )
            )
            .border(Dp.Hairline, LevyraAdaptiveSoftHairline, shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onOpen
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(178.dp)
                .padding(start = 16.dp, top = 15.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "LEVYRA",
                color = LevyraText.copy(alpha = 0.58f),
                fontSize = 9.5.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(9.5.sp),
                fontWeight = FontWeight.Black,
                letterSpacing = 1.4.sp,
                maxLines = 1
            )
            Text(
                text = homeCollectionTitle(strings, collection),
                color = LevyraText,
                fontSize = 19.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(19.sp),
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.30).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (collection.updatedToday) strings.collectionUpdatedToday else strings.collectionEditorial,
                color = LevyraText.copy(alpha = 0.58f),
                fontSize = 10.5.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(10.5.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        HomeEditorialArtworkStack(
            tracks = collection.tracks,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(122.dp)
                .fillMaxHeight()
        )
    }
}'''
text = replace_function(text, "HomeEditorialCollectionCard", collection_card)

artwork_stack = r'''@Composable
private fun HomeEditorialArtworkStack(
    tracks: List<Track>,
    modifier: Modifier = Modifier
) {
    val primary = tracks.firstOrNull()
    val secondary = tracks.getOrNull(1)
    Box(modifier = modifier, contentAlignment = Alignment.CenterEnd) {
        if (secondary != null) {
            CoverImage(
                track = secondary,
                modifier = Modifier
                    .size(82.dp)
                    .offset(x = 18.dp, y = (-10).dp)
                    .graphicsLayer {
                        rotationZ = 9f
                        alpha = 0.72f
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .border(Dp.Hairline, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp)),
                highRes = false,
                zoom = 1.02f
            )
        }
        if (primary != null) {
            CoverImage(
                track = primary,
                modifier = Modifier
                    .size(96.dp)
                    .offset(x = 10.dp, y = 13.dp)
                    .graphicsLayer { rotationZ = (-6f) }
                    .clip(RoundedCornerShape(18.dp))
                    .border(Dp.Hairline, Color.White.copy(alpha = 0.14f), RoundedCornerShape(18.dp)),
                highRes = false,
                zoom = 1.02f
            )
        }
    }
}'''

if "private fun HomeEditorialArtworkStack(" not in text:
    marker = "@Composable\nprivate fun CollectionArtworkMosaic("
    index = text.find(marker)
    if index < 0:
        raise RuntimeError("Collection artwork insertion marker not found")
    text = text[:index] + artwork_stack + "\n\n" + text[index:]

path.write_text(text, encoding="utf-8")
