#!/usr/bin/env python3
"""Apply the PR #317 Pixel-inspired no-scroll player experiment.

The experiment is presentation-only: playback commands and state ownership stay in
Levyra's existing ViewModel/player stack. Gradle runs this patch before Android
compilation so the draft APK can be evaluated on-device before source integration.

The generated player keeps playlist/favorite beside metadata, keeps the compact
YouTube engagement capsule for comments, and replaces the expandable lower area
with one Material You action shelf: download, timer, queue, lyrics, speed and
normalization. Artwork sizing and spacing become height-aware so common phone
sizes can expose the complete primary player without requiring a scroll.
"""

from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
PLAYER = ROOT / "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt"
MARKER = "PIXEL_PLAYER_ACTION_SHELF_PR317"


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


def main() -> None:
    text = PLAYER.read_text(encoding="utf-8")
    if MARKER in text:
        return

    text, count = re.subn(
        r"        var advancedControlsExpanded by remember\(track\?\.id\) \{\n            mutableStateOf\(false\)\n        \}",
        "        val advancedControlsExpanded = true",
        text,
        count=1,
    )
    if count != 1:
        raise RuntimeError(f"advancedControlsExpanded replacement failed: {count}")

    text = replace_once(
        text,
        "        val compactPlayer = layoutMode == LevyraLayoutMode.Compact && (maxWidth < 380.dp || maxHeight < 700.dp)",
        "        val compactPlayer = layoutMode == LevyraLayoutMode.Compact && (maxWidth < 400.dp || maxHeight < 860.dp)",
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
        "            (maxHeight - if (compactPlayer) 480.dp else 300.dp).coerceAtLeast(180.dp)",
        "artwork height budget",
    )
    text = replace_once(
        text,
        "                    bottom = if (compactPlayer) 28.dp else 34.dp",
        "                    bottom = if (compactPlayer) 16.dp else 28.dp",
        "stacked player bottom padding",
    )

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
                fill = Color.Black.copy(alpha = 0.24f),
                borderTop = Color.White.copy(alpha = 0.13f),
                borderBottom = Color.White.copy(alpha = 0.055f)
            )
            .padding(horizontal = if (compact) 5.dp else 7.dp, vertical = if (compact) 4.dp else 5.dp)
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
                tint = if (isExporting || isDownloaded) secondaryColor else LevyraPlayerDesign.TextSecondary,
                active = isDownloaded,
                isBusy = isExporting,
                enabled = !isExporting,
                compact = compact,
                onClick = onDownload
            )
            PlayerDockAction(
                icon = Icons.Rounded.Bedtime,
                label = if (sleepMinutes > 0) "${{sleepMinutes}}m" else strings.timer,
                tint = if (sleepMinutes > 0) secondaryColor else LevyraPlayerDesign.TextSecondary,
                active = sleepMinutes > 0,
                compact = compact,
                onClick = onSleep
            )
            PlayerDockAction(
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                label = strings.queue,
                tint = LevyraPlayerDesign.TextSecondary,
                compact = compact,
                onClick = onQueue
            )
            PlayerDockAction(
                icon = Icons.AutoMirrored.Rounded.Subject,
                label = strings.lyrics,
                tint = if (lyricsAvailable) activeColor else LevyraPlayerDesign.TextSecondary,
                active = lyricsAvailable,
                compact = compact,
                onClick = onLyrics
            )
            PlayerDockAction(
                icon = Icons.Rounded.Speed,
                label = "${{speed}}×",
                tint = if (speed != 1f) activeColor else LevyraPlayerDesign.TextSecondary,
                active = speed != 1f,
                compact = compact,
                onClick = onSpeed
            )
            PlayerDockAction(
                icon = Icons.Rounded.GraphicEq,
                label = strings.options,
                tint = if (audioNormalization) secondaryColor else LevyraPlayerDesign.TextSecondary,
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
            .height(if (compact) 48.dp else 52.dp)
            .pressable(enabled = enabled, pressedScale = 0.90f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {{
        Box(
            modifier = Modifier
                .size(if (compact) 39.dp else 43.dp)
                .background(
                    if (active) tint.copy(alpha = 0.16f) else Color.Transparent,
                    RoundedCornerShape(if (compact) 14.dp else 16.dp)
                )
                .border(
                    BorderStroke(
                        LevyraPlayerDesign.Hairline,
                        if (active) tint.copy(alpha = 0.28f) else Color.Transparent
                    ),
                    RoundedCornerShape(if (compact) 14.dp else 16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {{
            if (isBusy) {{
                CircularProgressIndicator(
                    modifier = Modifier.size(if (compact) 18.dp else 20.dp),
                    strokeWidth = 2.2.dp,
                    color = tint
                )
            }} else {{
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(if (compact) 20.dp else 22.dp)
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
                activeColor = primaryContent,
                secondaryColor = secondaryContent,
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
        ".padding(top = if (compact) 7.dp else 9.dp)",
        ".padding(top = if (compact) 2.dp else 5.dp)",
        "engagement top spacing",
    )
    text = replace_once(
        text,
        "modifier = Modifier.height(48.dp)",
        "modifier = Modifier.height(if (compact) 40.dp else 44.dp)",
        "engagement capsule height",
    )

    PLAYER.write_text(text, encoding="utf-8")


if __name__ == "__main__":
    main()
