from pathlib import Path

PATH = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
text = PATH.read_text(encoding="utf-8")


def function_bounds(source: str, name: str) -> tuple[int, int]:
    marker = f"private fun {name}("
    fn_start = source.find(marker)
    if fn_start < 0:
        raise RuntimeError(f"Function not found: {name}")

    annotation = source.rfind("@Composable", 0, fn_start)
    if annotation >= 0 and source[annotation:fn_start].strip() == "@Composable":
        start = annotation
    else:
        start = fn_start

    brace = source.find("{", fn_start)
    if brace < 0:
        raise RuntimeError(f"Opening brace not found: {name}")

    depth = 0
    for index in range(brace, len(source)):
        char = source[index]
        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                return start, index + 1
    raise RuntimeError(f"Closing brace not found: {name}")


def replace_function(source: str, name: str, replacement: str) -> str:
    start, end = function_bounds(source, name)
    return source[:start] + replacement.rstrip() + "\n\n" + source[end:].lstrip("\n")


# Thread a real play-all action into the artist overlay instead of rendering a decorative CTA.
overlay_start = text.find("private fun ArtistOverlay(")
overlay_brace = text.find("{", overlay_start)
if overlay_start < 0 or overlay_brace < 0:
    raise RuntimeError("ArtistOverlay signature not found")
overlay_signature = text[overlay_start:overlay_brace]
old_signature_part = "    onPlay: (Track) -> Unit,\n    onToggleFollow: () -> Unit,"
new_signature_part = "    onPlay: (Track) -> Unit,\n    onPlayAll: (List<Track>) -> Unit,\n    onToggleFollow: () -> Unit,"
if old_signature_part not in overlay_signature:
    raise RuntimeError("ArtistOverlay onPlay signature drifted")
overlay_signature = overlay_signature.replace(old_signature_part, new_signature_part, 1)
text = text[:overlay_start] + overlay_signature + text[overlay_brace:]

call_old = "                    onPlay = viewModel::playArtistSong,\n                    onToggleFollow = viewModel::toggleFollowArtist,"
call_new = "                    onPlay = viewModel::playArtistSong,\n                    onPlayAll = { tracks -> viewModel.playAll(tracks) },\n                    onToggleFollow = viewModel::toggleFollowArtist,"
if call_old not in text:
    raise RuntimeError("ArtistOverlay call site not found")
text = text.replace(call_old, call_new)

old_parent = '''                    if (artist.topSongs.isNotEmpty()) {
                        item { Box(modifier = Modifier.padding(horizontal = 20.dp)) { ArtistSectionTitle(strings.popularTracks) } }
                        item {
                            ArtistPopularTracksShelf(
                                tracks = artist.topSongs,
                                currentId = state.currentTrack?.id,
                                isPlaying = state.isPlaying,
                                isResolving = state.isResolving,
                                onPlay = onPlay
                            )
                        }
                    }
'''
new_parent = '''                    if (artist.topSongs.isNotEmpty()) {
                        item(
                            key = "artist-popular-tracks",
                            contentType = "artist-popular-tracks"
                        ) {
                            ArtistPopularTracksShelf(
                                tracks = artist.topSongs,
                                currentId = state.currentTrack?.id,
                                isPlaying = state.isPlaying,
                                isResolving = state.isResolving,
                                onPlay = onPlay,
                                onPlayAll = onPlayAll
                            )
                        }
                    }
'''
if old_parent not in text:
    raise RuntimeError("Artist popular-tracks parent block drifted")
text = text.replace(old_parent, new_parent, 1)

# The oversized #1 card is intentionally removed. The section now follows the compact,
# horizontally paged four-row rhythm used by mature music apps.
start, end = function_bounds(text, "ArtistTopTrackCard")
text = text[:start] + text[end:].lstrip("\n")

shelf = r'''@Composable
private fun ArtistPopularTracksShelf(
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: (Track) -> Unit,
    onPlayAll: (List<Track>) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val distinctTracks = remember(tracks) {
        tracks.distinctBy { track ->
            track.id.ifBlank { "${track.artist}|${track.title}" }
        }
    }
    if (distinctTracks.isEmpty()) return

    val columns = remember(distinctTracks) { distinctTracks.chunked(4) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = strings.popularTracks,
                color = LevyraText,
                fontSize = 22.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(22.sp),
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.45).sp,
                modifier = Modifier.weight(1f)
            )
            Surface(
                color = Color.White.copy(alpha = 0.04f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                shape = CircleShape,
                modifier = Modifier
                    .height(48.dp)
                    .pressable { onPlayAll(distinctTracks) }
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.playAll,
                        color = LevyraText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(
                items = columns,
                key = { _, columnTracks ->
                    columnTracks.joinToString(
                        prefix = "artist-popular-col-",
                        separator = "|"
                    ) { track ->
                        track.id.ifBlank { "${track.artist}:${track.title}" }
                    }
                },
                contentType = { _, _ -> "artist-popular-column" }
            ) { _, columnTracks ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.width(306.dp)
                ) {
                    columnTracks.forEach { track ->
                        val isCurrent = track.id == currentId
                        ArtistPopularTrackRow(
                            track = track,
                            isCurrent = isCurrent,
                            isPlaying = isPlaying && isCurrent,
                            isResolving = isResolving && isCurrent,
                            onPlay = { onPlay(track) }
                        )
                    }
                }
            }
        }
    }
}'''
text = replace_function(text, "ArtistPopularTracksShelf", shelf)

row = r'''@Composable
private fun ArtistPopularTrackRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (isCurrent) Modifier.background(LevyraCyan.copy(alpha = 0.08f))
                else Modifier
            )
            .heightIn(min = 64.dp)
            .pressable(onClick = onPlay)
            .padding(vertical = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(LevyraPanelSoft),
            contentAlignment = Alignment.Center
        ) {
            CoverImage(
                track = track,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = track.title,
                color = if (isCurrent) LevyraCyan else LevyraText,
                fontSize = 15.5.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(15.5.sp),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.25).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val duration = formatDuration(track.durationMs)
                .takeIf { it != "--:--" }
                .orEmpty()
            Text(
                text = listOf(track.artist, duration)
                    .filter { it.isNotBlank() }
                    .joinToString("  ·  "),
                color = LevyraMuted,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isResolving -> CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = LevyraCyan
                )
                isCurrent -> LevyraPlayingIndicator(
                    playing = isPlaying,
                    color = LevyraCyan,
                    size = 18.dp,
                    contentDescription = strings.playing
                )
                else -> Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = LevyraMuted.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}'''
text = replace_function(text, "ArtistPopularTrackRow", row)

PATH.write_text(text, encoding="utf-8")
