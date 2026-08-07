#!/usr/bin/env python3
"""Apply the PR #317 Pixel-inspired no-scroll player experiment.

Presentation-only experiment for the draft APK. Playback commands, queue state,
Media3 ownership, song/video mode and persistence remain untouched.

The generated layout deliberately keeps only the essential visual hierarchy on
the primary screen: artwork, metadata, timeline, transport and one tonal action
shelf. YouTube engagement leaves the vertical flow; comments stay reachable from
a small metadata action. Secondary playback options are folded into the shelf.
"""

from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
PLAYER = ROOT / "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt"
MARKER = "PIXEL_PLAYER_MINIMAL_PR317_V2"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


def main() -> None:
    text = PLAYER.read_text(encoding="utf-8")
    if MARKER in text:
        return

    # Always expose the compact secondary actions; the old reveal chevron is gone.
    text, count = re.subn(
        r"        var advancedControlsExpanded by remember\(track\?\.id\) \{\n            mutableStateOf\(false\)\n        \}",
        "        val advancedControlsExpanded = true",
        text,
        count=1,
    )
    if count != 1:
        raise RuntimeError(f"advancedControlsExpanded replacement failed: {count}")

    # Height-first layout: reduce artwork before allowing the core player to fall below the fold.
    text = replace_once(
        text,
        "        val compactPlayer = layoutMode == LevyraLayoutMode.Compact && (maxWidth < 380.dp || maxHeight < 700.dp)",
        "        val compactPlayer = layoutMode == LevyraLayoutMode.Compact && (maxWidth < 410.dp || maxHeight < 900.dp)",
        "compact player breakpoint",
    )
    text = replace_once(
        text,
        "        val playerItemSpacing = if (compactPlayer) LevyraPlayerDesign.SpaceSm else LevyraPlayerDesign.SpaceMd",
        "        val playerItemSpacing = if (compactPlayer) LevyraPlayerDesign.SpaceXs else LevyraPlayerDesign.SpaceMd",
        "compact player spacing",
    )
    text = replace_once(
        text,
        "            (maxHeight - 220.dp).coerceAtLeast(180.dp)",
        "            (maxHeight - if (compactPlayer) 560.dp else 330.dp).coerceAtLeast(176.dp)",
        "artwork height budget",
    )
    text = replace_once(
        text,
        "                    bottom = if (compactPlayer) 28.dp else 34.dp",
        "                    bottom = if (compactPlayer) 10.dp else 24.dp",
        "stacked player bottom padding",
    )

    # Flatten the header controls: Pixel-like touch targets, without decorative circles.
    collapse_old = '''                PlayerGlassIconButton(
                    icon = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = strings.collapsePlayer,
                    size = headerButtonSize,
                    iconSize = if (compactPlayer) 25.dp else 26.dp,
                    onClick = collapseActions.collapse
                )'''
    collapse_new = '''                PlayerGlassIconButton(
                    icon = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = strings.collapsePlayer,
                    size = if (compactPlayer) 38.dp else 40.dp,
                    iconSize = if (compactPlayer) 24.dp else 25.dp,
                    fill = Color.Transparent,
                    borderTop = Color.Transparent,
                    borderBottom = Color.Transparent,
                    onClick = collapseActions.collapse
                )'''
    text = replace_once(text, collapse_old, collapse_new, "collapse button")

    more_old = '''                PlayerGlassIconButton(
                    icon = Icons.Rounded.MoreVert,
                    contentDescription = strings.options,
                    size = headerButtonSize,
                    iconSize = if (compactPlayer) 21.dp else 22.dp,
                    onClick = { viewModel.openAudioQualityPanel() }
                )'''
    more_new = '''                PlayerGlassIconButton(
                    icon = Icons.Rounded.MoreVert,
                    contentDescription = strings.options,
                    size = if (compactPlayer) 38.dp else 40.dp,
                    iconSize = if (compactPlayer) 20.dp else 21.dp,
                    fill = Color.Transparent,
                    borderTop = Color.Transparent,
                    borderBottom = Color.Transparent,
                    onClick = { viewModel.openAudioQualityPanel() }
                )'''
    text = replace_once(text, more_old, more_new, "options button")

    mode_old = '''            .playerGlass(
                shape = LevyraPlayerDesign.ShapePill,
                fill = LevyraPlayerDesign.GlassFillSunken
            )
            .padding(3.dp)'''
    mode_new = '''            .playerGlass(
                shape = LevyraPlayerDesign.ShapePill,
                fill = Color.Black.copy(alpha = 0.16f),
                borderTop = Color.White.copy(alpha = 0.055f),
                borderBottom = Color.Transparent
            )
            .padding(2.dp)'''
    text = replace_once(text, mode_old, mode_new, "mode switch surface")
    text = replace_once(
        text,
        "targetValue = if (selected) Color.White.copy(alpha = 0.14f) else Color.Transparent",
        "targetValue = if (selected) Color.White.copy(alpha = 0.105f) else Color.Transparent",
        "mode selected fill",
    )

    # Metadata is intentionally calmer and slightly denser.
    text = replace_once(
        text,
        "                                fontSize = if (compactPlayer) 24.sp else 26.sp,\n                                lineHeight = if (compactPlayer) 26.sp else 28.sp,",
        "                                fontSize = if (compactPlayer) 22.sp else 25.sp,\n                                lineHeight = if (compactPlayer) 24.sp else 27.sp,",
        "title typography",
    )
    text, count = re.subn(
        r"(text = activeTrack\.artist,\n\s+color = LevyraPlayerDesign\.TextSecondary,\n\s+)fontSize = if \(compactPlayer\) 14\.sp else 15\.sp,",
        r"\1fontSize = if (compactPlayer) 13.sp else 14.5.sp,",
        text,
        count=1,
    )
    if count != 1:
        raise RuntimeError(f"artist typography: expected one metadata match, found {count}")
    text = replace_once(
        text,
        '''            val actionSize = if (compactPlayer) {
                LevyraPlayerDesign.UtilityButtonCompact
            } else {
                LevyraPlayerDesign.UtilityButton
            }''',
        '''            val actionSize = if (compactPlayer) 38.dp else 40.dp''',
        "metadata action size",
    )

    # Keep comments reachable without dedicating a whole vertical engagement row.
    action_row_old = '''                    Row(horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceXs)) {
                        PlayerGlassIconButton(
                            icon = Icons.AutoMirrored.Rounded.PlaylistAdd,'''
    action_row_new = '''                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (state.youtubeEngagement.videoId.isNotBlank()) {
                            PlayerGlassIconButton(
                                icon = Icons.Rounded.ChatBubbleOutline,
                                contentDescription = strings.totalComments,
                                size = actionSize,
                                iconSize = if (compactPlayer) 19.dp else 20.dp,
                                tint = LevyraPlayerDesign.TextTertiary,
                                fill = Color.Transparent,
                                borderTop = Color.Transparent,
                                borderBottom = Color.Transparent,
                                onClick = viewModel::openYoutubeComments
                            )
                        }
                        PlayerGlassIconButton(
                            icon = Icons.AutoMirrored.Rounded.PlaylistAdd,'''
    text = replace_once(text, action_row_old, action_row_new, "metadata action row")

    playlist_tail_old = '''                            tint = LevyraPlayerDesign.TextSecondary,
                            onClick = { playlistTarget = activeTrack }
                        )'''
    playlist_tail_new = '''                            tint = LevyraPlayerDesign.TextSecondary,
                            fill = Color.Transparent,
                            borderTop = Color.Transparent,
                            borderBottom = Color.Transparent,
                            onClick = { playlistTarget = activeTrack }
                        )'''
    text = replace_once(text, playlist_tail_old, playlist_tail_new, "playlist action styling")

    text = replace_once(
        text,
        "                            fill = if (isFavorite) favoriteFill else LevyraPlayerDesign.GlassFill,",
        "                            fill = if (isFavorite) favoriteFill.copy(alpha = 0.26f) else Color.Transparent,",
        "favorite fill",
    )
    text = replace_once(
        text,
        '''                            borderTop = if (isFavorite) {
                                primary.playerMix(Color.White, 0.3f).copy(alpha = 0.7f)
                            } else {
                                LevyraPlayerDesign.GlassBorderTop
                            },''',
        '''                            borderTop = if (isFavorite) {
                                primary.playerMix(Color.White, 0.3f).copy(alpha = 0.32f)
                            } else {
                                Color.Transparent
                            },''',
        "favorite top border",
    )
    text = replace_once(
        text,
        '''                            borderBottom = if (isFavorite) {
                                primary.copy(alpha = 0.2f)
                            } else {
                                LevyraPlayerDesign.GlassBorderBottom
                            },''',
        '''                            borderBottom = Color.Transparent,''',
        "favorite bottom border",
    )

    engagement_call = '''                PlayerYoutubeEngagementRow(
                    track = activeTrack,
                    engagement = state.youtubeEngagement,
                    primary = primary,
                    secondary = secondary,
                    compact = compactPlayer,
                    onComments = viewModel::openYoutubeComments
                )
'''
    text = replace_once(text, engagement_call, "", "engagement row removal")

    # Remove the old expandable reveal handle from PlayerScreen.
    text, count = re.subn(
        r"\n        val pulseBlock: @Composable \(\) -> Unit = \{.*?\n        \}\n\n        val advancedBlock:",
        "\n        val advancedBlock:",
        text,
        count=1,
        flags=re.S,
    )
    if count != 1:
        raise RuntimeError(f"pulseBlock removal failed: {count}")
    text = text.replace("                        pulseBlock()\n", "", 1)
    text = text.replace("                    item { pulseBlock() }\n", "", 1)

    # One low-profile Material You shelf. No labels are rendered, but semantics keep them accessible.
    replacement = f'''@Composable
private fun PlayerUtilityDock(
    activeColor: Color,
    secondaryColor: Color,
    lyricsAvailable: Boolean,
    isExporting: Boolean,
    isDownloaded: Boolean,
    sleepMinutes: Int,
    speed: Float,
    audioNormalization: Boolean,
    compact: Boolean,
    onDownload: () -> Unit,
    onSleep: () -> Unit,
    onQueue: () -> Unit,
    onLyrics: () -> Unit,
    onSpeed: () -> Unit,
    onNormalization: () -> Unit
) {{
    // {MARKER}
    val strings = LocalLevyraStrings.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .playerGlass(
                shape = LevyraPlayerDesign.ShapeXl,
                fill = Color.White.copy(alpha = 0.035f),
                borderTop = Color.White.copy(alpha = 0.065f),
                borderBottom = Color.Transparent
            )
            .padding(horizontal = if (compact) 3.dp else 5.dp, vertical = 2.dp)
    ) {{
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {{
            PlayerDockAction(
                icon = if (isDownloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                label = when {{
                    isExporting -> strings.downloadInProgress
                    isDownloaded -> strings.downloaded
                    else -> strings.download
                }},
                tint = if (isExporting || isDownloaded) secondaryColor else LevyraPlayerDesign.IconIdle,
                active = isDownloaded,
                isBusy = isExporting,
                enabled = !isExporting,
                compact = compact,
                onClick = onDownload
            )
            PlayerDockAction(
                icon = Icons.Rounded.Bedtime,
                label = if (sleepMinutes > 0) "${{sleepMinutes}}m" else strings.timer,
                tint = if (sleepMinutes > 0) secondaryColor else LevyraPlayerDesign.IconIdle,
                active = sleepMinutes > 0,
                compact = compact,
                onClick = onSleep
            )
            PlayerDockAction(
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                label = strings.queue,
                tint = LevyraPlayerDesign.IconIdle,
                compact = compact,
                onClick = onQueue
            )
            PlayerDockAction(
                icon = Icons.AutoMirrored.Rounded.Subject,
                label = strings.lyrics,
                tint = if (lyricsAvailable) activeColor else LevyraPlayerDesign.IconIdle,
                active = lyricsAvailable,
                compact = compact,
                onClick = onLyrics
            )
            PlayerDockAction(
                icon = Icons.Rounded.Speed,
                label = "${{speed}}×",
                tint = if (speed != 1f) activeColor else LevyraPlayerDesign.IconIdle,
                active = speed != 1f,
                compact = compact,
                onClick = onSpeed
            )
            PlayerDockAction(
                icon = Icons.Rounded.GraphicEq,
                label = strings.options,
                tint = if (audioNormalization) secondaryColor else LevyraPlayerDesign.IconIdle,
                active = audioNormalization,
                compact = compact,
                onClick = onNormalization
            )
        }}
    }}
}}

@Composable
private fun RowScope.PlayerDockAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    active: Boolean = false,
    isBusy: Boolean = false,
    enabled: Boolean = true,
    compact: Boolean,
    onClick: () -> Unit
) {{
    Box(
        modifier = Modifier
            .weight(1f)
            .height(if (compact) 42.dp else 46.dp)
            .pressable(enabled = enabled, pressedScale = 0.90f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {{
        Box(
            modifier = Modifier
                .size(if (compact) 33.dp else 36.dp)
                .background(
                    if (active) tint.copy(alpha = 0.12f) else Color.Transparent,
                    RoundedCornerShape(if (compact) 12.dp else 13.dp)
                ),
            contentAlignment = Alignment.Center
        ) {{
            if (isBusy) {{
                CircularProgressIndicator(
                    modifier = Modifier.size(if (compact) 17.dp else 18.dp),
                    strokeWidth = 2.dp,
                    color = tint
                )
            }} else {{
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(if (compact) 19.dp else 20.dp)
                )
            }}
        }}
    }}
}}

private fun compactYoutubeCount'''

    text, count = re.subn(
        r"@Composable\nprivate fun PlayerUtilityDock\(.*?\n\}\n\n@Composable\nprivate fun RowScope\.PlayerDockAction\(.*?\n\}\n\nprivate fun compactYoutubeCount",
        replacement,
        text,
        count=1,
        flags=re.S,
    )
    if count != 1:
        raise RuntimeError(f"PlayerUtilityDock replacement failed: {count}")

    old_call = '''            PlayerUtilityDock(
                activeColor = primaryContent,
                secondaryColor = secondaryContent,
                lyricsAvailable = state.lyrics.isNotEmpty(),
                isExporting = state.isOfflineExporting,
                isDownloaded = track.id in state.downloadedTrackIds,
                compact = compact,
                onQueue = viewModel::openQueue,
                onLyrics = viewModel::openLyrics,
                onAddToPlaylist = onAddToPlaylist,
                onDownload = viewModel::exportCurrentTrack
            )'''
    new_call = '''            PlayerUtilityDock(
                activeColor = primary.playerMix(Color.White, 0.28f),
                secondaryColor = secondary.playerMix(Color.White, 0.28f),
                lyricsAvailable = state.lyrics.isNotEmpty(),
                isExporting = state.isOfflineExporting,
                isDownloaded = track.id in state.downloadedTrackIds,
                sleepMinutes = state.sleepTimerMinutes,
                speed = state.playbackSpeed,
                audioNormalization = state.audioNormalization,
                compact = compact,
                onDownload = viewModel::exportCurrentTrack,
                onSleep = viewModel::cycleSleepTimer,
                onQueue = viewModel::openQueue,
                onLyrics = viewModel::openLyrics,
                onSpeed = viewModel::cycleSpeed,
                onNormalization = viewModel::toggleAudioNormalization
            )'''
    text = replace_once(text, old_call, new_call, "PlayerUtilityDock wiring")

    # The shelf replaces both the duplicated utility row and the inline lyrics card.
    extra_controls = '''            PlayerOptionsRow(
                speed = state.playbackSpeed,
                sleepMinutes = state.sleepTimerMinutes,
                audioNormalization = state.audioNormalization,
                activeColor = primary,
                secondaryColor = secondary,
                compact = compact,
                onSpeed = viewModel::cycleSpeed,
                onSleep = viewModel::cycleSleepTimer,
                onNormalization = viewModel::toggleAudioNormalization
            )
            PlayerInlineLyricsSection(
                trackId = track.id,
                lyrics = state.lyrics,
                lyricsLoading = state.lyricsLoading,
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                primaryContent = primaryContent,
                compact = compact,
                strings = strings,
                onSeek = viewModel::seekTo
            )
'''
    text = replace_once(text, extra_controls, "", "secondary utility rows")

    text = replace_once(
        text,
        "                modifier = Modifier.padding(vertical = LevyraPlayerDesign.SpaceXs)",
        "                modifier = Modifier.padding(vertical = if (compactPlayer) 0.dp else 2.dp)",
        "transport vertical padding",
    )

    PLAYER.write_text(text, encoding="utf-8")


if __name__ == "__main__":
    main()
