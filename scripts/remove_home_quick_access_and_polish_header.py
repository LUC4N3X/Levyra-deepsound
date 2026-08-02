from pathlib import Path

path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
text = path.read_text(encoding="utf-8")

import_anchor = "import com.luc4n3x.levyra.ui.theme.LevyraMuted\n"
import_line = "import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign\n"
if import_line not in text:
    if import_anchor not in text:
        raise SystemExit("theme import anchor not found")
    text = text.replace(import_anchor, import_anchor + import_line, 1)

tracks_block = '''    val homeMixTracks = remember(quickPicks, visiblePersonalTracks, resonanceTracks) {
        quickPicks?.tracks?.takeIf { it.isNotEmpty() }
            ?: visiblePersonalTracks.takeIf { it.isNotEmpty() }
            ?: resonanceTracks
    }
    val homeReleaseTracks = remember(newReleases) { newReleases?.tracks.orEmpty() }
'''
if tracks_block not in text:
    raise SystemExit("quick access track block not found")
text = text.replace(tracks_block, "", 1)

quick_access_block = '''        item(key = "home-quick-access", contentType = "home-quick-access") {
            HomeSectionInset {
                LevyraHomeQuickAccessGrid(
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
            }
        }
'''
if quick_access_block not in text:
    raise SystemExit("quick access UI block not found")
text = text.replace(quick_access_block, "", 1)

start_marker = "@Composable\nprivate fun GreetingBar(userName: String, isResolving: Boolean, onSettings: () -> Unit) {"
end_marker = "\n@Composable\nprivate fun MetroHeroDeck("
start = text.find(start_marker)
end = text.find(end_marker, start)
if start < 0 or end < 0:
    raise SystemExit("GreetingBar boundaries not found")

new_greeting = '''@Composable
private fun GreetingBar(userName: String, isResolving: Boolean, onSettings: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val greetingHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = remember(userName, strings, greetingHour) {
        strings.formatGreeting(userName, greetingHour)
    }
    val isLight = LevyraIsLight
    val headerSurface = if (isLight) LevyraHomeDesign.HeaderSurfaceLight else LevyraHomeDesign.HeaderSurfaceDark
    val headerBorder = if (isLight) LevyraHomeDesign.HeaderBorderLight else LevyraHomeDesign.HeaderBorderDark
    val primaryText = if (isLight) LevyraText else Color.White
    val secondaryText = if (isLight) LevyraMuted else Color.White.copy(alpha = 0.62f)
    val settingsSurface = if (isLight) Color.White.copy(alpha = 0.82f) else Color.White.copy(alpha = 0.065f)
    val settingsBorder = if (isLight) Color(0x1C11131F) else Color.White.copy(alpha = 0.10f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isLight) 8.dp else 18.dp,
                shape = LevyraHomeDesign.HeaderShape,
                clip = false,
                ambientColor = LevyraCyan.copy(alpha = if (isLight) 0.12f else 0.20f),
                spotColor = LevyraViolet.copy(alpha = if (isLight) 0.10f else 0.24f)
            )
            .background(headerSurface, LevyraHomeDesign.HeaderShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        LevyraCyan.copy(alpha = if (isLight) 0.11f else 0.17f),
                        Color.Transparent,
                        LevyraViolet.copy(alpha = if (isLight) 0.07f else 0.13f)
                    )
                ),
                LevyraHomeDesign.HeaderShape
            )
            .border(BorderStroke(1.dp, headerBorder), LevyraHomeDesign.HeaderShape)
            .padding(LevyraHomeDesign.HeaderPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LevyraLogoMark(size = 36.dp)
                    LevyraWordmark(fontSize = 27.sp, dotSize = 4.dp)
                }
                Text(
                    text = greeting,
                    color = primaryText,
                    fontSize = 24.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.55).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(3.dp)
                            .background(
                                Brush.horizontalGradient(listOf(LevyraCyan, LevyraViolet)),
                                RoundedCornerShape(999.dp)
                            )
                    )
                    Text(
                        text = "Your sound, your space",
                        color = secondaryText,
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                modifier = Modifier
                    .height(LevyraHomeDesign.SettingsControlHeight)
                    .clip(LevyraHomeDesign.SettingsShape)
                    .background(settingsSurface)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                LevyraCyan.copy(alpha = if (isLight) 0.07f else 0.12f),
                                Color.Transparent,
                                LevyraViolet.copy(alpha = if (isLight) 0.05f else 0.09f)
                            )
                        )
                    )
                    .border(BorderStroke(1.dp, settingsBorder), LevyraHomeDesign.SettingsShape)
                    .pressable(onClick = onSettings)
                    .padding(horizontal = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isResolving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = LevyraCyan
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null,
                        tint = primaryText.copy(alpha = 0.90f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = strings.settings,
                    color = primaryText.copy(alpha = 0.88f),
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}
'''

text = text[:start] + new_greeting + text[end:]
path.write_text(text, encoding="utf-8")
