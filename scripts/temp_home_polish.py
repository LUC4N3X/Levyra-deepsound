from pathlib import Path
import re

path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
text = path.read_text(encoding="utf-8")


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


def replace_block(marker: str, replacement: str) -> None:
    global text
    count = text.count(marker)
    if count != 1:
        raise RuntimeError(f"{marker}: expected one match, found {count}")
    start = text.index(marker)
    end = block_end(text, start)
    text = text[:start] + replacement.rstrip() + text[end:]


def replace_composable(marker: str, replacement: str) -> None:
    global text
    count = text.count(marker)
    if count != 1:
        raise RuntimeError(f"{marker}: expected one match, found {count}")
    start = text.index(marker)
    end = block_end(text, start)
    text = text[:start] + replacement.rstrip() + "\n\n" + text[end:].lstrip("\n")


visible_marker = "    val visibleEditorialCollections = remember(editorialCollections, spotlightCandidate?.track?.id) {"
replace_block(
    visible_marker,
    """    val visibleEditorialCollections = remember(editorialCollections, spotlightCandidate?.track?.id) {
        editorialCollections.map { collection ->
            val filteredTracks = collection.tracks.filterNot { it.id == spotlightCandidate?.track?.id }
            if (filteredTracks.size >= 4) collection.copy(tracks = filteredTracks) else collection
        }
    }""",
)

collection_marker = "            if (showDeferredHomeSections && visibleEditorialCollections.isNotEmpty()) {"
if text.count(collection_marker) != 1:
    raise RuntimeError(f"collection block: expected one match, found {text.count(collection_marker)}")
collection_start = text.index(collection_marker)
collection_end = block_end(text, collection_start)
collection_block = text[collection_start:collection_end]
text = text[:collection_start] + text[collection_end:]
video_anchor = "            if (showDeferredHomeSections && homeVideoTracks.isNotEmpty()) {"
if text.count(video_anchor) != 1:
    raise RuntimeError(f"video anchor: expected one match, found {text.count(video_anchor)}")
video_index = text.index(video_anchor)
text = text[:video_index] + collection_block + "\n\n" + text[video_index:]

text, width_count = re.subn(
    r"private val HOME_COLLECTION_COLUMN_WIDTH = \d+\.dp",
    "private val HOME_COLLECTION_COLUMN_WIDTH = 306.dp",
    text,
    count=1,
)
if width_count != 1:
    raise RuntimeError(f"collection width: expected one match, found {width_count}")

replace_composable(
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
    if (tracks.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeSectionInset {
            HomeSectionHeader(title)
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                start = HomeHorizontalInset,
                end = HomeHorizontalShelfEndPadding
            )
        ) {
            items(
                items = tracks.take(10),
                key = { "home-video-${it.id}" },
                contentType = { "home-video-card" }
            ) { track ->
                val isCurrent = track.id == currentId
                val artwork = trackArtworkUrl(track)
                val shape = RoundedCornerShape(16.dp)
                Column(
                    modifier = Modifier.width(256.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(shape)
                            .background(LevyraSurface)
                            .border(
                                1.dp,
                                if (isCurrent) LevyraOrange.copy(alpha = 0.66f)
                                else LevyraGlassStroke.copy(alpha = 0.16f),
                                shape
                            )
                            .clickable {
                                if (isCurrent && !isResolving) onToggleCurrent() else onPlay(track)
                            }
                    ) {
                        AsyncImage(
                            model = artwork,
                            contentDescription = track.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0f to Color.Transparent,
                                            0.64f to Color.Transparent,
                                            1f to Color.Black.copy(alpha = 0.48f)
                                        )
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(9.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.74f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCurrent && isResolving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    if (isCurrent && isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = track.title,
                        color = LevyraText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        lineHeight = 17.sp,
                        maxLines = 2,
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
            }
        }
    }
}''',
)

replace_composable(
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
    if (tracks.isEmpty()) return
    val columns = remember(tracks) { tracks.take(8).chunked(2) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeSectionInset {
            SectionHeaderAction(LevyraLocalStrings.current.voicesTitle, onPlayAll = onPlayAll)
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                start = HomeHorizontalInset,
                end = HomeHorizontalShelfEndPadding
            )
        ) {
            itemsIndexed(
                items = columns,
                key = { index, chunk -> "res-column-$index-${chunk.joinToString("-") { it.id }}" },
                contentType = { _, _ -> "home-resonance-column" }
            ) { _, chunk ->
                Column(
                    modifier = Modifier.width(312.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chunk.forEach { track ->
                        val isCurrent = track.id == currentId
                        val shape = RoundedCornerShape(12.dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp)
                                .clip(shape)
                                .background(
                                    LevyraSurface.copy(
                                        alpha = if (LevyraIsLight) 0.82f else if (isCurrent) 0.9f else 0.68f
                                    )
                                )
                                .border(
                                    1.dp,
                                    if (isCurrent) LevyraOrange.copy(alpha = 0.62f)
                                    else LevyraGlassStroke.copy(alpha = 0.16f),
                                    shape
                                )
                                .clickable { onPlay(track) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = trackArtworkUrl(track),
                                contentDescription = track.title,
                                modifier = Modifier.size(68.dp),
                                contentScale = ContentScale.Crop
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 11.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = track.title,
                                    color = LevyraText,
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
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCurrent) LevyraPlayerControl.copy(alpha = 0.92f)
                                        else Color.Black.copy(alpha = if (LevyraIsLight) 0.08f else 0.24f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCurrent && isResolving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = LevyraOnPlayerSurface
                                    )
                                } else {
                                    Icon(
                                        if (isCurrent && isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = if (isCurrent) LevyraOnPlayerSurface else LevyraText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}''',
)

path.write_text(text, encoding="utf-8")
