from pathlib import Path
import sys

repo = Path(sys.argv[1]).resolve()
path = repo / "app/src/main/java/com/luc4n3x/levyra/ui/player/LevyraNowPlaying.kt"
text = path.read_text(encoding="utf-8")

header_button = """                        PlayerGlassIconButton(
                            icon = Icons.Rounded.Fullscreen,
                            contentDescription = strings.enterImmersive,
                            size = headerButtonSize,
                            iconSize = 20.dp,
                            borderTop = primary.copy(alpha = 0.48f),
                            borderBottom = primary.copy(alpha = 0.14f),
                            onClick = { videoFullscreen = true }
                        )
"""
if text.count(header_button) != 1:
    raise SystemExit("fullscreen header button anchor changed")
text = text.replace(header_button, "", 1)

anchor = """                if (showArtworkPreview && artworkPreviewAvailable) {
"""
overlay_button = """                if (videoGesturesEnabled && !videoFullscreen) {
                    PlayerGlassIconButton(
                        icon = Icons.Rounded.Fullscreen,
                        contentDescription = strings.enterImmersive,
                        size = LevyraPlayerDesign.MinimumTouchTarget,
                        iconSize = 21.dp,
                        borderTop = primary.copy(alpha = 0.48f),
                        borderBottom = primary.copy(alpha = 0.14f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = LevyraPlayerDesign.SpaceSm, bottom = LevyraPlayerDesign.SpaceSm)
                            .zIndex(23f),
                        onClick = { videoFullscreen = true }
                    )
                }

"""
if text.count(anchor) != 1:
    raise SystemExit("video overlay anchor changed")
text = text.replace(anchor, overlay_button + anchor, 1)

old_exit = """                    size = LevyraPlayerDesign.HeaderButton,
                    iconSize = 22.dp,
"""
new_exit = """                    size = LevyraPlayerDesign.MinimumTouchTarget,
                    iconSize = 22.dp,
"""
if text.count(old_exit) != 1:
    raise SystemExit("fullscreen exit size anchor changed")
text = text.replace(old_exit, new_exit, 1)

path.write_text(text, encoding="utf-8")
