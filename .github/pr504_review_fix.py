from pathlib import Path
import re

path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
text = path.read_text(encoding="utf-8")

old_open = """                        onOpen = {
                            stableSpotlightId = heroTrack.id
                            viewModel.playFrom(spotlightTracks, heroTrack)
                        }
"""
new_open = """                        onOpen = {
                            stableSpotlightId = heroTrack.id
                            when {
                                heroTrack.id != state.currentTrack?.id ->
                                    viewModel.playFrom(spotlightTracks, heroTrack)
                                state.isResolving -> Unit
                                else -> viewModel.togglePlay()
                            }
                        }
"""
if old_open not in text:
    raise SystemExit("Home spotlight onOpen block not found")
text = text.replace(old_open, new_open, 1)

# Release-oriented spotlight helpers are dead after the soundtrack redesign.
text, count = re.subn(
    r"private fun homeSpotlightBadge\(.*?\n(?=private fun homeSoundtrackPrimaryArtist\()",
    "",
    text,
    count=1,
    flags=re.S,
)
if count != 1:
    raise SystemExit(f"Expected to remove old spotlight helpers once, removed {count}")

# Artist artwork is now resolved directly from soundtrackArtistPool in HomeScreen.
text, count = re.subn(
    r"private fun homeSoundtrackArtistArtwork\(.*?\n}\n\n(?=private fun homeSoundtrackTitle\()",
    "",
    text,
    count=1,
    flags=re.S,
)
if count != 1:
    raise SystemExit(f"Expected to remove unused artist artwork helper once, removed {count}")

signature_old = """    isResolving: Boolean,
    animationsEnabled: Boolean,
    onPaletteChanged: (Color, Color) -> Unit,
"""
signature_new = """    isResolving: Boolean,
    onPaletteChanged: (Color, Color) -> Unit,
"""
if signature_old not in text:
    raise SystemExit("HomeEditorialSpotlight animation parameter not found")
text = text.replace(signature_old, signature_new, 1)

call_old = """                        isResolving = state.isResolving && heroTrack.id == state.currentTrack?.id,
                        animationsEnabled = state.animationsEnabled,
                        onPaletteChanged = { start, end ->
"""
call_new = """                        isResolving = state.isResolving && heroTrack.id == state.currentTrack?.id,
                        onPaletteChanged = { start, end ->
"""
if call_old not in text:
    raise SystemExit("HomeEditorialSpotlight call animation argument not found")
text = text.replace(call_old, call_new, 1)

hero_start = text.index("private fun HomeEditorialSpotlight(")
hero_end = text.index("\nprivate fun homeCollectionTitle(", hero_start)
hero = text[hero_start:hero_end]
unused_animation = "    val effectiveAnimationsEnabled = animationsEnabled && LocalAnimationsEnabled.current\n"
if unused_animation not in hero:
    raise SystemExit("Unused HomeEditorialSpotlight animation local not found")
hero = hero.replace(unused_animation, "", 1)

pause_old = """                isCurrent && isPlaying -> Icon(
                    imageVector = Icons.Rounded.Pause,
                    contentDescription = strings.playing,
"""
pause_new = """                isCurrent && isPlaying -> Icon(
                    imageVector = Icons.Rounded.Pause,
                    contentDescription = strings.pause,
"""
if pause_old not in hero:
    raise SystemExit("Home hero pause semantics block not found")
hero = hero.replace(pause_old, pause_new, 1)
text = text[:hero_start] + hero + text[hero_end:]

path.write_text(text, encoding="utf-8")
