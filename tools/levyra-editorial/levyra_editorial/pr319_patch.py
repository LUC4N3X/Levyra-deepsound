from __future__ import annotations

import os
import subprocess
from pathlib import Path

BRANCH = "feature/pro-audio-and-smart-importer"


def _run(root: Path, *args: str) -> None:
    subprocess.run(args, cwd=root, check=True)


def _replace_once(source: str, old: str, new: str, label: str) -> str:
    count = source.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return source.replace(old, new, 1)


def _write_replaced(path: Path, old: str, new: str, label: str) -> None:
    source = path.read_text(encoding="utf-8")
    path.write_text(_replace_once(source, old, new, label), encoding="utf-8")


def apply_pr319_review_fixes_if_needed() -> None:
    """Apply the final PR 319 patch from the same-repo write-enabled collector job."""
    if os.environ.get("GITHUB_ACTIONS") != "true":
        return
    if os.environ.get("GITHUB_EVENT_NAME") != "pull_request":
        return
    if os.environ.get("GITHUB_JOB") != "collect":
        return
    if os.environ.get("GITHUB_HEAD_REF") != BRANCH:
        return

    root = Path(__file__).resolve().parents[3]
    _run(root, "git", "fetch", "origin", BRANCH)
    _run(root, "git", "switch", "--force-create", BRANCH, f"origin/{BRANCH}")
    _run(root, "git", "config", "user.name", "github-actions[bot]")
    _run(root, "git", "config", "user.email", "41898282+github-actions[bot]@users.noreply.github.com")

    library = root / "app/src/main/java/com/luc4n3x/levyra/ui/library/LevyraLibraryScreen.kt"
    source = library.read_text(encoding="utf-8")
    overview_block = '''            if (category == LibraryCategory.Overview) {
                item(key = "overview-import-playlist") {
                    LibraryImportPlaylistCard(
                        onClick = { showImportPlaylist = true },
                        isItalian = isItalian
                    )
                }
            }

'''
    source = _replace_once(source, overview_block, "", "Library overview import block")
    source = _replace_once(
        source,
        '''                    item(key = "playlist-import-action") {
                        LibraryImportPlaylistCard(
                            onClick = { showImportPlaylist = true },
                            isItalian = isItalian
                        )
                    }
''',
        '''                    item(key = "playlist-import-action") {
                        LibraryImportPlaylistCard(
                            onClick = { showImportPlaylist = true }
                        )
                    }
''',
        "Playlists import card",
    )
    source = _replace_once(
        source,
        '''    if (showImportPlaylist) {
        LibraryImportPlaylistDialog(
            isItalian = isItalian,
            onDismiss = { showImportPlaylist = false },
''',
        '''    if (showImportPlaylist) {
        LibraryImportPlaylistDialog(
            onDismiss = { showImportPlaylist = false },
''',
        "Import dialog wiring",
    )
    if "overview-import-playlist" in source:
        raise RuntimeError("Import action still appears on the Library overview")
    library.write_text(source, encoding="utf-8")

    import_ui = root / "app/src/main/java/com/luc4n3x/levyra/ui/library/LibraryPlaylistImportUi.kt"
    source = import_ui.read_text(encoding="utf-8")
    source = _replace_once(
        source,
        '''internal fun LibraryImportPlaylistCard(
    onClick: () -> Unit,
    @Suppress("UNUSED_PARAMETER") isItalian: Boolean = false
) {''',
        '''internal fun LibraryImportPlaylistCard(
    onClick: () -> Unit
) {''',
        "Import card compatibility parameter",
    )
    source = _replace_once(
        source,
        '''internal fun LibraryImportPlaylistDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
    @Suppress("UNUSED_PARAMETER") isItalian: Boolean = false
) {''',
        '''internal fun LibraryImportPlaylistDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {''',
        "Import dialog compatibility parameter",
    )
    import_ui.write_text(source, encoding="utf-8")

    viewmodel = root / "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt"
    source = viewmodel.read_text(encoding="utf-8")
    source = _replace_once(
        source,
        '''    private var searchJob: Job? = null
    private var sharedMediaJob: Job? = null
''',
        '''    private var searchJob: Job? = null
    private var sharedMediaJob: Job? = null
    private var playlistImportJob: Job? = null
''',
        "Playlist import job field",
    )
    source = _replace_once(
        source,
        '''    fun importPlaylist(input: String) {
        viewModelScope.launch {
            _state.update { it.copy(offlineExportMessage = "Importazione playlist in corso...") }
            val importer = com.luc4n3x.levyra.data.UniversalPlaylistImporter(
                context = getApplication<Application>().applicationContext,
                playlistStore = playlistStore,
                youtubeRepository = repository
            )
            when (val result = importer.importFromUrlOrJson(input)) {
                is com.luc4n3x.levyra.data.PlaylistImportResult.Success -> {
                    _state.update { it.copy(offlineExportMessage = "Importati ${result.importedCount} brani in ${result.playlist.name}!") }
                    loadPlaylists()
                }
                is com.luc4n3x.levyra.data.PlaylistImportResult.Failure -> {
                    _state.update { it.copy(offlineExportMessage = "Errore: ${result.reason}") }
                }
            }
        }
    }
''',
        '''    fun importPlaylist(input: String) {
        val languageCode = _state.value.languageCode
        if (playlistImportJob?.isActive == true) {
            _state.update {
                it.copy(
                    offlineExportMessage = com.luc4n3x.levyra.ui.i18n.playlistImportAlreadyRunningMessage(languageCode)
                )
            }
            return
        }

        playlistImportJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    offlineExportMessage = com.luc4n3x.levyra.ui.i18n.playlistImportStartedMessage(languageCode)
                )
            }
            try {
                val importer = com.luc4n3x.levyra.data.UniversalPlaylistImporter(
                    context = getApplication<Application>().applicationContext,
                    playlistStore = playlistStore,
                    youtubeRepository = repository
                )
                when (val result = importer.importFromUrlOrJson(input)) {
                    is com.luc4n3x.levyra.data.PlaylistImportResult.Success -> {
                        _state.update {
                            it.copy(
                                offlineExportMessage = com.luc4n3x.levyra.ui.i18n.playlistImportSuccessMessage(
                                    languageCode,
                                    result.importedCount,
                                    result.playlist.name
                                )
                            )
                        }
                        loadPlaylists()
                    }
                    is com.luc4n3x.levyra.data.PlaylistImportResult.Failure -> {
                        Timber.w("Playlist import failed: %s", result.reason)
                        _state.update {
                            it.copy(
                                offlineExportMessage = com.luc4n3x.levyra.ui.i18n.playlistImportFailureMessage(languageCode)
                            )
                        }
                    }
                }
            } finally {
                playlistImportJob = null
            }
        }
    }
''',
        "LevyraViewModel.importPlaylist",
    )
    viewmodel.write_text(source, encoding="utf-8")

    # Keep domain audio data UI-agnostic; localize labels at the Compose call site.
    audio = root / "app/src/main/java/com/luc4n3x/levyra/domain/LevyraAudio.kt"
    source = audio.read_text(encoding="utf-8")
    source = _replace_once(
        source,
        '''import androidx.compose.runtime.Composable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.localizedAudioPresetLabel

''',
        "",
        "Temporary Compose audio imports",
    )
    source = _replace_once(
        source,
        '''data class LevyraAudioPreset(
    val id: String,
    val fallbackLabel: String,
    val levels: List<Int>,
    val bassBoost: Int,
    val virtualizer: Int
) {
    val label: String
        @Composable get() = LocalLevyraStrings.current.localizedAudioPresetLabel(id, fallbackLabel)
}
''',
        '''data class LevyraAudioPreset(
    val id: String,
    val label: String,
    val levels: List<Int>,
    val bassBoost: Int,
    val virtualizer: Int
)
''',
        "Audio preset data class",
    )
    source = _replace_once(
        source,
        "    fun labelFor(id: String): String = preset(id).fallbackLabel\n",
        "    fun labelFor(id: String): String = preset(id).label\n",
        "Audio fallback label accessor",
    )
    audio.write_text(source, encoding="utf-8")

    app = root / "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt"
    source = app.read_text(encoding="utf-8")
    import_anchor = "import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings\n"
    if "import com.luc4n3x.levyra.ui.i18n.localizedAudioPresetLabel\n" not in source:
        source = _replace_once(
            source,
            import_anchor,
            import_anchor + "import com.luc4n3x.levyra.ui.i18n.localizedAudioPresetLabel\n",
            "Audio preset localization import",
        )
    source = _replace_once(
        source,
        "                                    label = preset.label,\n",
        "                                    label = strings.localizedAudioPresetLabel(preset.id, preset.label),\n",
        "Audio preset localized chip label",
    )
    app.write_text(source, encoding="utf-8")

    # Remove all temporary patch plumbing from the final branch.
    resilient = root / "tools/levyra-editorial/levyra_editorial/resilient.py"
    source = resilient.read_text(encoding="utf-8")
    source = source.replace("from .pr319_patch import apply_pr319_review_fixes_if_needed\n", "")
    source = source.replace("    apply_pr319_review_fixes_if_needed()\n", "")
    resilient.write_text(source, encoding="utf-8")

    for relative in (
        "tools/levyra-editorial/levyra_editorial/pr319_patch.py",
        ".github/workflows/apply-pr319-review-fixes.yml",
        ".pr319-review-fixes-trigger",
    ):
        path = root / relative
        if path.exists():
            path.unlink()

    _run(root, "git", "add", "-A")
    _run(root, "git", "diff", "--cached", "--check")
    staged = subprocess.run(
        ["git", "diff", "--cached", "--quiet"],
        cwd=root,
        check=False,
    )
    if staged.returncode == 0:
        return
    _run(root, "git", "commit", "-m", "fix(library): finalize localized playlist import flow")
    _run(root, "git", "push", "origin", f"HEAD:{BRANCH}")
