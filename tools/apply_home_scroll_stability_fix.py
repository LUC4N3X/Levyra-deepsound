from __future__ import annotations

from pathlib import Path

path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
content = path.read_text(encoding="utf-8")


def replace_once(old: str, new: str) -> None:
    global content
    count = content.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one match, found {count}: {old[:100]!r}")
    content = content.replace(old, new, 1)


# Keep the Home list state across player-tab transitions and Activity state restoration.
replace_once(
    "            val homeListState = rememberLazyListState()\n",
    "            val homeListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }\n",
)

# Starting or closing playback must not select a different hero and mutate the list above
# the user's current viewport.
replace_once(
    """    val spotlightCandidate = remember(spotlightCandidates, stableSpotlightId, state.currentTrack?.id) {
        spotlightCandidates.firstOrNull { it.track.id == stableSpotlightId }
            ?: spotlightCandidates.firstOrNull { it.track.id != state.currentTrack?.id }
            ?: spotlightCandidates.firstOrNull()
    }
""",
    """    val spotlightCandidate = remember(spotlightCandidates, stableSpotlightId) {
        spotlightCandidates.firstOrNull { it.track.id == stableSpotlightId }
            ?: spotlightCandidates.firstOrNull()
    }
""",
)

# The mini player is an overlay. Reserve its maximum inset permanently so appearing or
# disappearing never changes LazyColumn geometry and never re-anchors the Top 50 viewport.
replace_once(
    "        contentPadding = PaddingValues(top = 8.dp, bottom = if (state.currentTrack != null) 188.dp else 104.dp),\n",
    """        // Keep Home geometry immutable while the mini player appears, resolves, pauses or closes.
        contentPadding = PaddingValues(top = 8.dp, bottom = 188.dp),
""",
)

path.write_text(content, encoding="utf-8")
print("Home scroll stability patch applied")
# Ready for the one-shot workflow trigger.
