from pathlib import Path

APP = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
SCREEN_VM = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraScreenViewModels.kt")
TEST = Path("app/src/test/java/com/luc4n3x/levyra/viewmodel/HomeRenderSnapshotTest.kt")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected 1 match, found {count}")
    return text.replace(old, new, 1)


app = APP.read_text()
start_marker = "@Composable\nprivate fun ContinueListeningCard("
end_marker = "\n@Composable\nprivate fun HomeShortcutRow("
start = app.index(start_marker)
end = app.index(end_marker, start)
card = app[start:end]

replacements = [
    ("val accentStart = Color(track.accentStart)", "val accentStart = if (track.accentStart == 0) LevyraCyan else Color(track.accentStart)"),
    ("val accentEnd = Color(track.accentEnd)", "val accentEnd = if (track.accentEnd == 0) LevyraViolet else Color(track.accentEnd)"),
    ("shape = RoundedCornerShape(18.dp)", "shape = RoundedCornerShape(22.dp)"),
    (".heightIn(min = 70.dp)", ".heightIn(min = 86.dp)"),
    ("cinematicGlassBrush(accentStart, accentEnd, 0.24f)", "cinematicGlassBrush(accentStart, accentEnd, 0.66f)"),
    (".size(104.dp)\n                    .align(Alignment.CenterEnd)\n                    .offset(x = 44.dp)", ".size(158.dp)\n                    .align(Alignment.CenterEnd)\n                    .offset(x = 58.dp)"),
    ("accentEnd.copy(alpha = 0.16f)", "accentEnd.copy(alpha = 0.25f)"),
    (".padding(horizontal = 9.dp, vertical = 7.dp)", ".padding(horizontal = 11.dp, vertical = 10.dp)"),
    ("horizontalArrangement = Arrangement.spacedBy(8.dp)", "horizontalArrangement = Arrangement.spacedBy(11.dp)"),
    (".size(44.dp)\n                        .clip(RoundedCornerShape(11.dp))\n                        .border(Dp.Hairline, Color.White.copy(alpha = 0.13f), RoundedCornerShape(11.dp))", ".size(56.dp)\n                        .clip(RoundedCornerShape(15.dp))\n                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(15.dp))"),
    ("fontSize = 9.8.sp,\n                            lineHeight = 11.5.sp", "fontSize = 10.2.sp,\n                            lineHeight = 12.sp"),
    ("fontSize = 14.sp,\n                        lineHeight = 16.sp", "fontSize = 15.5.sp,\n                        lineHeight = 18.sp"),
    ("fontSize = 10.8.sp,\n                        lineHeight = 12.5.sp", "fontSize = 11.5.sp,\n                        lineHeight = 13.5.sp"),
    ("modifier = Modifier.size(32.dp)", "modifier = Modifier.size(42.dp)"),
    ("modifier = Modifier.size(18.dp)", "modifier = Modifier.size(22.dp)"),
    (".height(3.dp)\n                    .clip(RoundedCornerShape(topEnd = 3.dp))", ".height(4.dp)\n                    .clip(RoundedCornerShape(topEnd = 4.dp))"),
]
for old, new in replacements:
    card = replace_once(card, old, new, old[:45])

# Add a subtle leading glow so the card feels attached to the artwork instead of floating as a plain row.
needle = """        Box(\n            modifier = Modifier\n                .fillMaxWidth()\n                .background(cinematicGlassBrush(accentStart, accentEnd, 0.66f))\n        ) {\n"""
insert = """        Box(\n            modifier = Modifier\n                .fillMaxWidth()\n                .background(cinematicGlassBrush(accentStart, accentEnd, 0.66f))\n        ) {\n            Box(\n                modifier = Modifier\n                    .size(132.dp)\n                    .align(Alignment.CenterStart)\n                    .offset(x = (-42).dp)\n                    .background(\n                        Brush.radialGradient(\n                            listOf(accentStart.copy(alpha = 0.22f), Color.Transparent)\n                        )\n                    )\n            )\n"""
card = replace_once(card, needle, insert, "resume leading glow")
app = app[:start] + card + app[end:]
APP.write_text(app)

screen_vm = SCREEN_VM.read_text()
screen_vm = replace_once(
    screen_vm,
    "        personalOrbitTracks = previous.personalOrbitTracks,\n",
    "        personalOrbitTracks = personalOrbitTracks,\n",
    "live personal orbit while home frozen",
)
SCREEN_VM.write_text(screen_vm)

tests = TEST.read_text()
anchor = "    @Test\n    fun publishesLatestStructuralHomeContentAfterScrollingSettles() {"
new_test = '''    @Test
    fun publishesPersonalOrbitRemovalImmediatelyWhileHomeStructureIsFrozen() {
        val keptTrack = track("aaaaaaaaaaa")
        val removedTrack = track("bbbbbbbbbbb")
        val initialState = LevyraUiState(
            tracks = listOf(keptTrack, removedTrack),
            personalOrbitTracks = listOf(keptTrack, removedTrack),
            homeSections = listOf(HomeSection("Initial", listOf(keptTrack, removedTrack)))
        )
        val previous = buildHomeRenderSnapshot(initialState)
        val updatedState = initialState.copy(
            tracks = listOf(removedTrack),
            personalOrbitTracks = listOf(keptTrack),
            homeSections = listOf(HomeSection("Refreshed", listOf(removedTrack)))
        )

        val frozen = buildStableHomeRenderSnapshot(updatedState, previous, freezeContent = true)

        assertEquals(listOf(keptTrack), frozen.state.personalOrbitTracks)
        assertSame(previous.state.tracks, frozen.state.tracks)
        assertSame(previous.state.homeSections, frozen.state.homeSections)
    }

'''
tests = replace_once(tests, anchor, new_test + anchor, "HomeRenderSnapshot test anchor")
TEST.write_text(tests)
