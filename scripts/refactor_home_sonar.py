from pathlib import Path

path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
text = path.read_text(encoding="utf-8")

constant_anchor = "private const val HOME_DEFERRED_SECTION_REVEAL_MS = 180L\n"
constant_line = "private const val HOME_HORIZONTAL_ROW_CONTENT_TYPE = \"home-horizontal-row\"\n"
if constant_line not in text:
    if constant_anchor not in text:
        raise SystemExit("Home constant anchor not found")
    text = text.replace(constant_anchor, constant_anchor + constant_line, 1)

old_call = '''                LevyraHomeQuickAccessGrid(
                    currentTrack = state.currentTrack,
                    mixTrack = homeMixTracks.firstOrNull(),
                    favoriteTrack = state.favorites.firstOrNull(),
                    releaseTrack = homeReleaseTracks.firstOrNull(),
                    chartTrack = state.charts.firstOrNull(),
                    isPlaying = state.isPlaying,
                    isResolving = state.isResolving,
                    hasMix = homeMixTracks.isNotEmpty(),
                    hasFavorites = state.favorites.isNotEmpty(),
                    hasNewReleases = homeReleaseTracks.isNotEmpty(),
                    hasCharts = state.charts.isNotEmpty(),
                    isLight = LevyraIsLight,
                    onContinue = viewModel::togglePlay,
                    onMix = { viewModel.playAll(homeMixTracks) },
                    onFavorites = { viewModel.playAll(state.favorites) },
                    onNewReleases = { viewModel.playAll(homeReleaseTracks) },
                    onCharts = { viewModel.playAll(state.charts) },
                    onSearch = { viewModel.searchNow() }
                )
'''
new_call = '''                LevyraHomeQuickAccessGrid(
                    state = LevyraHomeQuickAccessState(
                        tracks = LevyraHomeQuickAccessTracks(
                            current = state.currentTrack,
                            mix = homeMixTracks.firstOrNull(),
                            favorite = state.favorites.firstOrNull(),
                            release = homeReleaseTracks.firstOrNull(),
                            chart = state.charts.firstOrNull()
                        ),
                        availability = LevyraHomeQuickAccessAvailability(
                            hasMix = homeMixTracks.isNotEmpty(),
                            hasFavorites = state.favorites.isNotEmpty(),
                            hasNewReleases = homeReleaseTracks.isNotEmpty(),
                            hasCharts = state.charts.isNotEmpty()
                        ),
                        playback = LevyraHomeQuickAccessPlayback(
                            isPlaying = state.isPlaying,
                            isResolving = state.isResolving
                        ),
                        isLight = LevyraIsLight
                    ),
                    actions = LevyraHomeQuickAccessActions(
                        onContinue = viewModel::togglePlay,
                        onMix = { viewModel.playAll(homeMixTracks) },
                        onFavorites = { viewModel.playAll(state.favorites) },
                        onNewReleases = { viewModel.playAll(homeReleaseTracks) },
                        onCharts = { viewModel.playAll(state.charts) },
                        onSearch = { viewModel.searchNow() }
                    )
                )
'''
if old_call not in text:
    raise SystemExit("Quick-access call block not found")
text = text.replace(old_call, new_call, 1)

literal = '"home-horizontal-row"'
literal_count = text.count(literal)
if literal_count < 2:
    raise SystemExit(f"Expected duplicated content type, found {literal_count}")
text = text.replace(literal, "HOME_HORIZONTAL_ROW_CONTENT_TYPE")

if text.count("LevyraHomeQuickAccessGrid(") != 1:
    raise SystemExit("Unexpected quick-access call count")
if literal in text:
    raise SystemExit("Horizontal row literal remains")

path.write_text(text, encoding="utf-8")
