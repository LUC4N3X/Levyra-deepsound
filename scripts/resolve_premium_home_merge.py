from pathlib import Path

PATH = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
text = PATH.read_text(encoding="utf-8")

start_marker = "@Composable\nprivate fun PersonalListeningShelf("
end_marker = "private fun trackAlbumHit(track: Track): AlbumHit = AlbumHit("

start = text.find(start_marker)
end = text.find(end_marker, start)
if start < 0 or end < 0:
    raise SystemExit("PersonalListeningShelf boundaries not found; refusing to modify LevyraApp.kt")

current = text[start:end]
required_old_fragments = [
    "HorizontalPager(",
    "PersonalOrbitSpeedDialCard(",
    "LevyraHomeDesign.SPEED_DIAL_PAGE_SIZE",
]
missing = [fragment for fragment in required_old_fragments if fragment not in current]
if missing:
    raise SystemExit(f"Unexpected upstream Orbit block; missing {missing}. Refusing unsafe merge.")

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
}


'''

merged = text[:start] + replacement + text[end:]

checks = {
    "premium Orbit helper": "homePersonalOrbitColumns(tracks)",
    "premium Orbit LazyRow": "items = columns",
    "main compact-landscape dock behavior": "LevyraLandscapeDockMiniWeight",
    "main responsive player-pane behavior": "resolvePlayerPane(widthDp, heightDp)",
}
missing_after = [name for name, fragment in checks.items() if fragment not in merged]
if missing_after:
    raise SystemExit(f"Merge would drop required behavior: {missing_after}")

if "private fun PersonalOrbitSpeedDialCard(" in merged:
    raise SystemExit("Legacy PersonalOrbitSpeedDialCard still present after merge")

PATH.write_text(merged, encoding="utf-8")
print("Safely reapplied premium Orbit on top of current responsive-landscape main LevyraApp.kt")
