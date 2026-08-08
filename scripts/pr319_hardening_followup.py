from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
MODE = sys.argv[1] if len(sys.argv) > 1 else "post"


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"missing marker in {path}: {old[:120]!r}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


if MODE == "pre":
    patcher = ROOT / "scripts/pr319_hardening_once.py"
    replace_once(
        patcher,
        '''if library.count(old) != 1:\n    raise SystemExit("LevyraLibraryScreen: header marker mismatch")\nlibrary = library.replace(old, new, 1)''',
        '''if old not in library:\n    raise SystemExit("LevyraLibraryScreen: header marker missing")\nlibrary = library.replace(old, new, 1)'''
    )
    raise SystemExit(0)

# Keep the dismissal state local/saveable instead of performing blocking DataStore reads from Compose.
prefs = ROOT / "app/src/main/java/com/luc4n3x/levyra/data/LevyraPreferences.kt"
replace_once(
    prefs,
    '''    fun playlistImportCardDismissed(): Boolean = read(false) { it[KEY_PLAYLIST_IMPORT_CARD_DISMISSED] ?: false }\n\n    fun setPlaylistImportCardDismissed(value: Boolean) {\n        write { it[KEY_PLAYLIST_IMPORT_CARD_DISMISSED] = value }\n    }\n\n''',
    ''
)
replace_once(
    prefs,
    '        val KEY_PLAYLIST_IMPORT_CARD_DISMISSED = booleanPreferencesKey("playlist_import_card_dismissed")\n',
    ''
)

library = ROOT / "app/src/main/java/com/luc4n3x/levyra/ui/library/LevyraLibraryScreen.kt"
replace_once(
    library,
    '''    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext\n    val libraryPreferences = remember(appContext) { com.luc4n3x.levyra.data.LevyraPreferences(appContext) }\n''',
    ''
)
replace_once(
    library,
    '    var showImportPlaylistCard by remember { mutableStateOf(!libraryPreferences.playlistImportCardDismissed()) }',
    '    var showImportPlaylistCard by rememberSaveable { mutableStateOf(true) }'
)
replace_once(
    library,
    '''                                onDismiss = {\n                                    showImportPlaylistCard = false\n                                    libraryPreferences.setPlaylistImportCardDismissed(true)\n                                }\n''',
    '''                                onDismiss = { showImportPlaylistCard = false }\n'''
)

# Make import-job cleanup identity-safe so an older cancelled job cannot clear a newer one.
vm = ROOT / "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt"
text = vm.read_text(encoding="utf-8")
start = text.index("    fun importPlaylist(input: String) {")
end = text.index("    fun renamePlaylist(", start)
block = text[start:end]
block = block.replace(
    '''        playlistImportJob = viewModelScope.launch {\n''',
    '''        var job: Job? = null\n        job = viewModelScope.launch {\n''',
    1
)
block = block.replace(
    '''            } finally {\n                playlistImportJob = null\n            }\n        }\n    }\n\n''',
    '''            } finally {\n                if (playlistImportJob === job) playlistImportJob = null\n            }\n        }\n        playlistImportJob = job\n    }\n\n''',
    1
)
if "playlistImportJob === job" not in block:
    raise SystemExit("ViewModel identity guard was not applied")
vm.write_text(text[:start] + block + text[end:], encoding="utf-8")

# Add byte-boundary coverage, including multi-byte UTF-8, directly against readUtf8Bounded.
test = ROOT / "app/src/test/java/com/luc4n3x/levyra/data/SponsorBlockRepositoryTest.kt"
text = test.read_text(encoding="utf-8")
needle = '''class SponsorBlockRepositoryTest {\n\n'''
addition = '''class SponsorBlockRepositoryTest {\n\n    @Test\n    fun boundedReaderAcceptsBodyExactlyAtByteLimit() {\n        val body = "éé"\n        val bytes = body.toByteArray(Charsets.UTF_8)\n\n        assertEquals(body, readUtf8Bounded(ByteArrayInputStream(bytes), bytes.size.toLong()))\n    }\n\n    @Test\n    fun boundedReaderRejectsBodyOneByteOverLimit() {\n        val body = "ééx"\n        val bytes = body.toByteArray(Charsets.UTF_8)\n\n        assertNull(readUtf8Bounded(ByteArrayInputStream(bytes), (bytes.size - 1).toLong()))\n    }\n\n'''
if needle not in text:
    raise SystemExit("SponsorBlock test insertion marker missing")
text = text.replace(needle, addition, 1)
text = text.replace('import org.junit.Assert.assertTrue\n', 'import org.junit.Assert.assertTrue\nimport org.junit.Assert.assertNull\n', 1)
test.write_text(text, encoding="utf-8")

print("PR #319 follow-up applied")
