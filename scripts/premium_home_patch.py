from pathlib import Path

path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
source = path.read_text(encoding="utf-8")


def function_span(text: str, marker: str) -> tuple[int, int]:
    start = text.find(marker)
    if start < 0:
        raise RuntimeError(f"marker not found: {marker}")
    body_start = text.find("{", start)
    if body_start < 0:
        raise RuntimeError(f"body not found: {marker}")
    depth = 0
    for index in range(body_start, len(text)):
        char = text[index]
        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                return start, index + 1
    raise RuntimeError(f"unbalanced function: {marker}")


replacement = '''@Composable
private fun PersonalListeningShelf(
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: (Track) -> Unit,
    onPlayAll: () -> Unit,
    onTrackActions: (Track) -> Unit
) {
    val columns = remember(tracks) { homePersonalOrbitColumns(tracks) }
    if (columns.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HomeSectionInset {
            HomeOrbitHeader(onPlayAll = onPlayAll)
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val columnWidth = (maxWidth - HOME_DENSE_SHELF_PEEK)
                .coerceIn(HOME_DENSE_SHELF_MIN_WIDTH, HOME_DENSE_SHELF_MAX_WIDTH)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = LevyraHomeDesign.HorizontalInset,
                    end = HOME_DENSE_SHELF_END_PADDING
                ),
                horizontalArrangement = Arrangement.spacedBy(LevyraHomeDesign.TrackColumnGap)
            ) {
                itemsIndexed(
                    items = columns,
                    key = { columnIndex, column ->
                        val identity = column.firstOrNull()
                            ?.let(LevyraPersonalOrbit::identityKey)
                            .orEmpty()
                        "orbit-column-$columnIndex-$identity"
                    },
                    contentType = { _, _ -> HOME_DENSE_SHELF_CONTENT_TYPE }
                ) { _, columnTracks ->
                    Column(
                        modifier = Modifier.width(columnWidth),
                        verticalArrangement = Arrangement.spacedBy(LevyraHomeDesign.TrackColumnGap)
                    ) {
                        columnTracks.forEach { track ->
                            key(LevyraPersonalOrbit.identityKey(track)) {
                                HomeTrackRow(
                                    track = track,
                                    isCurrent = track.id == currentId,
                                    isPlaying = isPlaying && track.id == currentId,
                                    isResolving = isResolving && track.id == currentId,
                                    onPlay = { onPlay(track) },
                                    onActions = { onTrackActions(track) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}'''

start, end = function_span(source, "@Composable\nprivate fun PersonalListeningShelf(")
source = source[:start] + replacement + source[end:]

old_card_marker = "@Composable\nprivate fun PersonalOrbitSpeedDialCard("
if old_card_marker in source:
    start, end = function_span(source, old_card_marker)
    source = source[:start] + source[end:]

if "homePersonalOrbitColumns(tracks)" not in source:
    raise RuntimeError("new Orbit layout was not inserted")
if "PersonalOrbitSpeedDialCard(" in source:
    raise RuntimeError("old Orbit speed-dial card still present")

path.write_text(source, encoding="utf-8")
