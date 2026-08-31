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


artist_shelf = r'''@Composable
private fun TrendingArtistsShelf(
    artists: List<ArtistHit>,
    loadingSlots: Int,
    onArtistClick: (ArtistHit) -> Unit
) {
    val strings = LocalLevyraStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeSectionHeader(
            title = strings.artists,
            modifier = Modifier.padding(horizontal = HomeHorizontalInset)
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(start = HomeHorizontalInset, end = 38.dp)
        ) {
            itemsIndexed(
                items = artists,
                key = { index, artist ->
                    "trending-artist-$index-${artist.browseId.ifBlank { artist.name.trim().lowercase(Locale.ROOT) }}"
                },
                contentType = { index, _ ->
                    if (index == 0) "trending-artist-featured" else "trending-artist-portrait"
                }
            ) { index, artist ->
                ArtistHitShelfItem(
                    artist = artist,
                    featured = index == 0,
                    onClick = { onArtistClick(artist) }
                )
            }
            items(
                count = loadingSlots,
                key = { index -> "artist-loading-${artists.size + index}" },
                contentType = { index ->
                    if (artists.isEmpty() && index == 0) "trending-artist-loading-featured"
                    else "trending-artist-loading"
                }
            ) { index ->
                TrendingArtistLoadingItem(featured = artists.isEmpty() && index == 0)
            }
        }
    }
}'''
replace_between(
    "@Composable\nprivate fun TrendingArtistsShelf(",
    "@Composable\nprivate fun ArtistHitShelfItem(",
    artist_shelf,
    "artist discovery shelf"
)

artist_item = r'''@Composable
private fun ArtistHitShelfItem(
    artist: ArtistHit,
    featured: Boolean,
    onClick: () -> Unit
) {
    val accent = remember(artist.browseId, artist.name, artist.accentStart, artist.accentEnd) {
        levyraArtistAccent(
            key = artist.browseId.ifBlank { artist.name },
            accentStart = artist.accentStart,
            accentEnd = artist.accentEnd
        )
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val animationsEnabled = LocalAnimationsEnabled.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && animationsEnabled) 0.982f else 1f,
        animationSpec = tween(130, easing = FastOutSlowInEasing),
        label = "homeArtistScale-${artist.browseId.ifBlank { artist.name }}"
    )
    val width = if (featured) 184.dp else 154.dp
    val height = if (featured) 210.dp else 186.dp
    val shape = RoundedCornerShape(if (featured) 28.dp else 22.dp)

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (featured) 12.dp else 8.dp,
                shape = shape,
                clip = false,
                ambientColor = accent.first.copy(alpha = 0.16f),
                spotColor = accent.second.copy(alpha = 0.18f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.first.copy(alpha = if (LevyraIsLight) 0.22f else 0.34f),
                        LevyraPanelSoft,
                        accent.second.copy(alpha = if (LevyraIsLight) 0.12f else 0.20f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        accent.first.copy(alpha = 0.42f),
                        Color.White.copy(alpha = 0.10f),
                        accent.second.copy(alpha = 0.34f)
                    )
                ),
                shape = shape
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
    ) {
        if (artist.thumbnailUrl.isNotBlank()) {
            StableRemoteArtwork(
                url = artist.thumbnailUrl,
                contentDescription = artist.name,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                highRes = featured
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                accent.first.copy(alpha = 0.50f),
                                LevyraPanelSoft,
                                accent.second.copy(alpha = 0.42f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = artist.name,
                    tint = LevyraText.copy(alpha = 0.84f),
                    modifier = Modifier.size(if (featured) 72.dp else 58.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to accent.first.copy(alpha = 0.08f),
                            0.48f to Color.Transparent,
                            0.68f to Color.Black.copy(alpha = 0.16f),
                            1f to Color.Black.copy(alpha = 0.90f)
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
                            accent.first.copy(alpha = 0.08f),
                            Color.Transparent,
                            accent.second.copy(alpha = 0.05f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = if (featured) 15.dp else 12.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = artist.name,
                color = Color.White,
                fontSize = if (featured) 18.sp else 15.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(if (featured) 18.sp else 15.sp),
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.25).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .width(if (featured) 38.dp else 28.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(accent.first, accent.second)))
            )
        }
    }
}'''
replace_between(
    "@Composable\nprivate fun ArtistHitShelfItem(",
    "@Composable\nprivate fun TrendingArtistLoadingItem(",
    artist_item,
    "artist portrait card"
)

loading_item = r'''@Composable
private fun TrendingArtistLoadingItem(featured: Boolean) {
    val width = if (featured) 184.dp else 154.dp
    val height = if (featured) 210.dp else 186.dp
    val shape = RoundedCornerShape(if (featured) 28.dp else 22.dp)
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(shape)
            .levyraShimmer()
            .background(CinematicGlassDeep)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 13.dp, vertical = 15.dp)
                .width(if (featured) 108.dp else 88.dp)
                .height(16.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
        )
    }
}'''
replace_between(
    "@Composable\nprivate fun TrendingArtistLoadingItem(",
    "@Composable\nprivate fun ResonanceShelf(",
    loading_item,
    "artist loading portrait"
)

replace_exact(
    '''            TextButton(onClick = onPlayAll) {
                Icon(Icons.Rounded.PlayArrow, null, tint = LevyraCyan, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text(strings.playAll, color = LevyraCyan, fontWeight = FontWeight.Black)
            }''',
    '''            TextButton(onClick = {
                onPlayAll()
                onDismiss()
            }) {
                Icon(Icons.Rounded.PlayArrow, null, tint = LevyraCyan, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text(strings.playAll, color = LevyraCyan, fontWeight = FontWeight.Black)
            }''',
    "dismiss collection after play all"
)

path.write_text(text, encoding="utf-8")
