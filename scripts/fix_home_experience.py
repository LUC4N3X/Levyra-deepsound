from pathlib import Path

path = Path("app/src/main/java/com/luc4n3x/levyra/ui/HomeExperience.kt")
text = path.read_text(encoding="utf-8")


def replace_once(old: str, new: str, label: str) -> None:
    global text
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    text = text.replace(old, new, 1)


replace_once(
    "import androidx.compose.animation.core.animateColorAsState",
    "import androidx.compose.animation.animateColorAsState",
    "Compose colour animation import",
)
replace_once(
    "import androidx.compose.foundation.layout.fillMaxHeight\n",
    "",
    "Unused fillMaxHeight import",
)
replace_once(
    "import androidx.compose.foundation.shape.CircleShape\n",
    "",
    "Unused CircleShape import",
)
replace_once(
    """                active = currentTrack != null && isPlaying,
                resolving = currentTrack != null && isResolving,
""",
    """                active = currentTrack != null,
                playing = currentTrack != null && isPlaying,
                resolving = currentTrack != null && isResolving,
""",
    "Continue tile playback state",
)
replace_once(
    """    val active: Boolean = false,
    val resolving: Boolean = false,
""",
    """    val active: Boolean = false,
    val playing: Boolean = false,
    val resolving: Boolean = false,
""",
    "Quick access item playback field",
)
replace_once(
    """                    .width(LevyraHomeDesign.ArtworkSize)
                    .fillMaxHeight()
""",
    """                    .size(LevyraHomeDesign.ArtworkSize)
""",
    "Square quick-access artwork",
)
replace_once(
    """                    item.active -> Icon(
                        imageVector = Icons.Rounded.GraphicEq,
""",
    """                    item.playing -> Icon(
                        imageVector = Icons.Rounded.GraphicEq,
""",
    "Playing artwork state",
)
replace_once(
    "imageVector = if (isPlayingIcon(item)) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow,",
    "imageVector = if (item.playing) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow,",
    "Trailing playback icon",
)
replace_once(
    "\nprivate fun isPlayingIcon(item: HomeQuickAccessItem): Boolean = item.active\n",
    "\n",
    "Redundant playback helper",
)

path.write_text(text, encoding="utf-8")
print("HomeExperience compile and geometry fixes applied")
