from pathlib import Path

APP = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
DESIGN = Path("app/src/main/java/com/luc4n3x/levyra/ui/theme/HomeDesign.kt")


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


def pop_balanced_block(source: str, marker: str) -> tuple[str, str]:
    start = source.find(marker)
    if start < 0:
        raise RuntimeError(f"Block not found: {marker}")
    brace = source.find("{", start)
    if brace < 0:
        raise RuntimeError(f"Opening brace not found for block: {marker}")
    depth = 0
    end = -1
    for index in range(brace, len(source)):
        if source[index] == "{":
            depth += 1
        elif source[index] == "}":
            depth -= 1
            if depth == 0:
                end = index + 1
                break
    if end < 0:
        raise RuntimeError(f"Closing brace not found for block: {marker}")
    while end < len(source) and source[end] in " \t":
        end += 1
    if source[end:end + 2] == "\n\n":
        end += 2
    elif source[end:end + 1] == "\n":
        end += 1
    return source[:start] + source[end:], source[start:end].rstrip()


text = APP.read_text(encoding="utf-8")

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
    val strings = LocalLevyraStrings.current
    val haptics = LocalLevyraHaptics.current
    val shelfTracks = remember(tracks) {
        LevyraPersonalOrbit.distinctRecordings(tracks)
            .take(LevyraPersonalOrbit.DISPLAY_LIMIT)
    }
    val pages = remember(shelfTracks) { shelfTracks.chunked(9) }
    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeSectionHeader(
            title = strings.personalOrbitTitle,
            subtitle = strings.personalOrbitSubtitle,
            onPlayAll = onPlayAll,
            modifier = Modifier.padding(horizontal = HomeHorizontalInset)
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = HomeHorizontalInset),
            pageSpacing = 12.dp
        ) { pageIndex ->
            val pageTracks = pages.getOrElse(pageIndex) { emptyList() }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { rowIndex ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(3) { columnIndex ->
                            val track = pageTracks.getOrNull(rowIndex * 3 + columnIndex)
                            if (track != null) {
                                PersonalListeningCard(
                                    track = track,
                                    active = track.id == currentId,
                                    playing = isPlaying && track.id == currentId,
                                    resolving = isResolving && track.id == currentId,
                                    onClick = { onPlay(track) },
                                    modifier = Modifier.weight(1f),
                                    onLongClick = {
                                        haptics.perform(LevyraHapticAction.TrackSwipe)
                                        onTrackActions(track)
                                    },
                                    onLongClickLabel = strings.songOptions
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
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
                    Box(
                        modifier = Modifier
                            .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                            .background(
                                if (pagerState.currentPage == index) LevyraText else LevyraMuted.copy(alpha = 0.38f),
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}'''
text = replace_function(text, "PersonalListeningShelf", personal_shelf)

personal_card = r'''@Composable
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
    val artworkShape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(artworkShape)
            .border(
                width = if (active) 1.5.dp else Dp.Hairline,
                color = if (active) LevyraCyan.copy(alpha = 0.90f) else LevyraAdaptiveSoftHairline,
                shape = artworkShape
            )
            .pressable(
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = onLongClickLabel
            )
    ) {
        CoverImage(
            track = track,
            modifier = Modifier.fillMaxSize(),
            highRes = false
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.52f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.84f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = track.title,
                color = Color.White,
                fontSize = 12.5.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(12.5.sp),
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 10.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(10.sp),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (active) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(7.dp)
                    .size(26.dp)
                    .background(Color.Black.copy(alpha = 0.72f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (resolving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = LevyraCyan
                    )
                } else {
                    ActiveTrackEqualizer(
                        color = LevyraCyan,
                        isPlaying = playing,
                        width = 12.dp,
                        height = 9.dp
                    )
                }
            }
        }
    }
}'''
text = replace_function(text, "PersonalListeningCard", personal_card)

quick_shelf = r'''@Composable
private fun HomeQuickPicksShelf(
    title: String,
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: (Track) -> Unit,
    onPlayAll: () -> Unit,
    onTrackActions: ((Track) -> Unit)? = null
) {
    val columns = remember(tracks) {
        tracks
            .distinctBy(LevyraPersonalOrbit::identityKey)
            .chunked(4)
    }
    val density = LocalDensity.current
    val containerWidthPx = LocalWindowInfo.current.containerSize.width
    val columnWidth = remember(containerWidthPx, density) {
        val availableWidth = with(density) { containerWidthPx.toDp() }
        (availableWidth - HomeHorizontalInset - 34.dp)
            .coerceIn(286.dp, 338.dp)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HomeSectionInset {
            SectionHeaderAction(title, onPlayAll)
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(start = HomeHorizontalInset, end = 38.dp)
        ) {
            itemsIndexed(
                items = columns,
                key = { _, column ->
                    column.joinToString(prefix = "quick-picks-", separator = "|") {
                        LevyraPersonalOrbit.identityKey(it)
                    }
                },
                contentType = { _, _ -> "quick-picks-column" }
            ) { _, column ->
                Column(
                    modifier = Modifier.width(columnWidth),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    column.forEach { track ->
                        HomeQuickPickRow(
                            track = track,
                            isCurrent = track.id == currentId,
                            isPlaying = isPlaying && track.id == currentId,
                            isResolving = isResolving && track.id == currentId,
                            onPlay = { onPlay(track) },
                            onActions = onTrackActions?.let { actions -> { actions(track) } }
                        )
                    }
                }
            }
        }
    }
}'''
text = replace_function(text, "HomeQuickPicksShelf", quick_shelf)

quick_row = r'''@Composable
private fun HomeQuickPickRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: () -> Unit,
    onActions: (() -> Unit)? = null
) {
    val strings = LocalLevyraStrings.current
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(shape)
            .then(
                if (isCurrent) Modifier.background(LevyraCyan.copy(alpha = 0.07f)) else Modifier
            )
            .pressable(onClick = onPlay)
            .padding(start = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            CoverImage(
                track = track,
                modifier = Modifier.fillMaxSize(),
                highRes = false
            )
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isResolving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = LevyraCyan
                        )
                    } else {
                        ActiveTrackEqualizer(
                            color = LevyraCyan,
                            isPlaying = isPlaying,
                            width = 18.dp,
                            height = 13.dp
                        )
                    }
                }
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = track.title,
                color = if (isCurrent) LevyraCyan else LevyraText,
                fontSize = 15.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(15.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                color = LevyraMuted,
                fontSize = 12.5.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(12.5.sp),
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (onActions != null) {
            IconButton(
                onClick = onActions,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = strings.songOptions,
                    tint = LevyraMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}'''
text = replace_function(text, "HomeQuickPickRow", quick_row)

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
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(start = HomeHorizontalInset, end = 34.dp)
        ) {
            itemsIndexed(
                items = columns,
                key = { _, column -> column.joinToString(prefix = "home-collection-column-") { it.id } },
                contentType = { _, _ -> "home-collection-column" }
            ) { _, column ->
                Column(
                    modifier = Modifier.width(256.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
    val accentStart = remember(collection.accentStart) { Color(collection.accentStart) }
    val accentEnd = remember(collection.accentEnd) { Color(collection.accentEnd) }
    val artwork = collection.tracks.firstOrNull()
    val shape = RoundedCornerShape(15.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(106.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        accentStart.copy(alpha = if (LevyraIsLight) 0.24f else 0.38f),
                        accentEnd.copy(alpha = if (LevyraIsLight) 0.15f else 0.22f),
                        LevyraPanelSoft
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
        if (artwork != null) {
            CoverImage(
                track = artwork,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(106.dp),
                highRes = false,
                zoom = 1.03f
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(108.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                LevyraPanelSoft,
                                LevyraPanelSoft.copy(alpha = 0.36f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(164.dp)
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = homeCollectionTitle(strings, collection),
                color = LevyraText,
                fontSize = 16.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(16.sp),
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (collection.updatedToday) strings.collectionUpdatedToday else strings.collectionEditorial,
                color = LevyraMuted,
                fontSize = 10.5.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(10.5.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}'''
text = replace_function(text, "HomeEditorialCollectionCard", collection_card)

text, spotlight_block = pop_balanced_block(text, "            spotlightCandidate?.let { candidate ->")
text, quick_block = pop_balanced_block(text, "            if (showDeferredHomeSections && quickPicks != null && quickPicks.tracks.isNotEmpty()) {")
text, personal_block = pop_balanced_block(text, "            if (state.interfaceSettings.showPersonalOrbit && visiblePersonalTracks.isNotEmpty()) {")

quick_old = "                            onPlayAll = { viewModel.playAll(quickPicks.tracks) }\n"
quick_new = "                            onPlayAll = { viewModel.playAll(quickPicks.tracks) },\n                            onTrackActions = onTrackActions\n"
if quick_old not in quick_block:
    raise RuntimeError("Quick-picks call site drifted")
quick_block = quick_block.replace(quick_old, quick_new, 1)

insert_marker = "            if (\n                showDeferredHomeSections && state.interfaceSettings.showNewReleases &&\n"
insert_at = text.find(insert_marker)
if insert_at < 0:
    raise RuntimeError("New releases insertion point not found")
reordered = personal_block + "\n\n" + quick_block + "\n\n" + spotlight_block + "\n\n"
text = text[:insert_at] + reordered + text[insert_at:]

old_height = ".heightIn(min = LevyraHomeDesign.HeroHeight)"
if old_height not in text:
    raise RuntimeError("Home spotlight height marker not found")
text = text.replace(old_height, ".height(LevyraHomeDesign.HeroHeight)", 1)
text = text.replace("        val artworkWidth = maxWidth * 0.64f", "        val artworkWidth = maxWidth * 0.58f", 1)
text = text.replace("                .width(maxWidth * 0.66f)", "                .width(maxWidth * 0.70f)", 1)
text = text.replace("                fontSize = 28.sp,\n                lineHeight = LevyraTypeRhythm.lineHeight(28.sp),", "                fontSize = 24.sp,\n                lineHeight = LevyraTypeRhythm.lineHeight(24.sp),", 1)

APP.write_text(text, encoding="utf-8")

design = DESIGN.read_text(encoding="utf-8")
replacements = {
    "val SectionGap: Dp = 10.dp": "val SectionGap: Dp = 8.dp",
    "val SectionGapCompact: Dp = 8.dp": "val SectionGapCompact: Dp = 6.dp",
    "val SectionStride: Dp = 30.dp": "val SectionStride: Dp = 26.dp",
    "val SectionStrideCompact: Dp = 22.dp": "val SectionStrideCompact: Dp = 20.dp",
    "val HeroCorner: Dp = 26.dp": "val HeroCorner: Dp = 20.dp",
    "val HeroHeight: Dp = 260.dp": "val HeroHeight: Dp = 220.dp",
    "val ShelfCorner: Dp = 16.dp": "val ShelfCorner: Dp = 12.dp",
    "val ArtworkCorner: Dp = 18.dp": "val ArtworkCorner: Dp = 14.dp",
    "val ShelfItemGap: Dp = 14.dp": "val ShelfItemGap: Dp = 12.dp",
}
for old, new in replacements.items():
    if old not in design:
        raise RuntimeError(f"HomeDesign marker not found: {old}")
    design = design.replace(old, new, 1)
DESIGN.write_text(design, encoding="utf-8")
