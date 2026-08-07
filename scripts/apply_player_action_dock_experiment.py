#!/usr/bin/env python3
"""Apply the draft persistent player action dock before Android compilation.

This is intentionally scoped to PR #317 so the UI can be tested on-device
without touching playback behavior. The patch is idempotent for repeated Gradle
invocations in the same workspace.
"""

from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
PLAYER = ROOT / "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt"


def main() -> None:
    text = PLAYER.read_text(encoding="utf-8")

    # Already transformed in this workspace.
    if "sleepMinutes: Int," in text and "isFavorite: Boolean," in text and "val advancedControlsExpanded = true" in text:
        return

    text, count = re.subn(
        r"        var advancedControlsExpanded by remember\(track\?\.id\) \{\n            mutableStateOf\(false\)\n        \}",
        "        val advancedControlsExpanded = true",
        text,
        count=1,
    )
    if count != 1:
        raise RuntimeError(f"advancedControlsExpanded replacement failed: {count}")

    # The controls are now persistent, so the old reveal handle is not rendered.
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

    replacement = '''@Composable
private fun PlayerUtilityDock(
    activeColor: Color,
    secondaryColor: Color,
    lyricsAvailable: Boolean,
    isExporting: Boolean,
    isDownloaded: Boolean,
    sleepMinutes: Int,
    isFavorite: Boolean,
    compact: Boolean,
    onDownload: () -> Unit,
    onSleep: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onQueue: () -> Unit,
    onLyrics: () -> Unit,
    onFavorite: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .playerGlass(
                shape = LevyraPlayerDesign.ShapeLg,
                fill = LevyraPlayerDesign.GlassFillSunken
            )
            .padding(horizontal = if (compact) 4.dp else 6.dp, vertical = if (compact) 4.dp else 5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerDockAction(
                icon = if (isDownloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                label = when {
                    isExporting -> strings.downloadInProgress
                    isDownloaded -> strings.downloaded
                    else -> strings.download
                },
                tint = if (isExporting || isDownloaded) secondaryColor else Color.White.copy(alpha = 0.76f),
                active = isDownloaded,
                isBusy = isExporting,
                enabled = !isExporting,
                compact = compact,
                onClick = onDownload
            )
            PlayerDockAction(
                icon = Icons.Rounded.Bedtime,
                label = if (sleepMinutes > 0) "${sleepMinutes}m" else strings.timer,
                tint = if (sleepMinutes > 0) secondaryColor else Color.White.copy(alpha = 0.76f),
                active = sleepMinutes > 0,
                compact = compact,
                onClick = onSleep
            )
            PlayerDockAction(
                icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                label = strings.addToPlaylist,
                tint = Color.White.copy(alpha = 0.82f),
                compact = compact,
                onClick = onAddToPlaylist
            )
            PlayerDockAction(
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                label = strings.queue,
                tint = Color.White.copy(alpha = 0.82f),
                compact = compact,
                onClick = onQueue
            )
            PlayerDockAction(
                icon = Icons.AutoMirrored.Rounded.Subject,
                label = strings.lyrics,
                tint = if (lyricsAvailable) activeColor else Color.White.copy(alpha = 0.72f),
                active = lyricsAvailable,
                compact = compact,
                onClick = onLyrics
            )
            PlayerDockAction(
                icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                label = strings.favoritesPlain,
                tint = if (isFavorite) activeColor else Color.White.copy(alpha = 0.76f),
                active = isFavorite,
                compact = compact,
                onClick = onFavorite
            )
        }
    }
}

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
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(if (compact) 46.dp else 50.dp)
            .pressable(enabled = enabled, pressedScale = 0.92f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 36.dp else 40.dp)
                .background(
                    if (active) tint.copy(alpha = 0.14f) else Color.Transparent,
                    RoundedCornerShape(if (compact) 12.dp else 14.dp)
                )
                .border(
                    BorderStroke(1.dp, if (active) tint.copy(alpha = 0.24f) else Color.Transparent),
                    RoundedCornerShape(if (compact) 12.dp else 14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(if (compact) 18.dp else 20.dp),
                    strokeWidth = 2.2.dp,
                    color = tint
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(if (compact) 20.dp else 22.dp)
                )
            }
        }
    }
}

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
                isFavorite = track.id in state.favoriteIds,
                compact = compact,
                onDownload = viewModel::exportCurrentTrack,
                onSleep = viewModel::cycleSleepTimer,
                onAddToPlaylist = onAddToPlaylist,
                onQueue = viewModel::openQueue,
                onLyrics = viewModel::openLyrics,
                onFavorite = { viewModel.toggleFavorite(track) }
            )'''
    if old_call not in text:
        raise RuntimeError("PlayerUtilityDock call not found")
    text = text.replace(old_call, new_call, 1)

    PLAYER.write_text(text, encoding="utf-8")


if __name__ == "__main__":
    main()
