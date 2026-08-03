from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def replace_once(path: str, old: str, new: str) -> None:
    file = ROOT / path
    text = file.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected one match, found {count}: {old!r}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")


download_actions = "desktop/app/src/main/kotlin/com/luc4n3x/levyra/desktop/app/ui/components/DownloadActions.kt"
replace_once(
    download_actions,
    "import com.luc4n3x.levyra.desktop.core.storage.DownloadRecord\n",
    "import com.luc4n3x.levyra.desktop.core.storage.DownloadData\n",
)
replace_once(
    download_actions,
    "    val stateFlow: StateFlow<Map<String, DownloadRecord>>,\n",
    "    val stateFlow: StateFlow<DownloadData>,\n",
)

player = "desktop/app/src/main/kotlin/com/luc4n3x/levyra/desktop/app/ui/player/PlayerBar.kt"
replace_once(
    player,
    "            downloadActions.stateFlow.map { it[track.id] }.distinctUntilChanged()\n",
    "            downloadActions.stateFlow.map { data -> data.records.firstOrNull { it.id == track.id } }.distinctUntilChanged()\n",
)
replace_once(
    player,
    "    val initialRecord = track?.let { downloadActions?.stateFlow?.value?.get(it.id) }\n",
    "    val initialRecord = track?.let { currentTrack ->\n        downloadActions?.stateFlow?.value?.records?.firstOrNull { it.id == currentTrack.id }\n    }\n",
)

track_row = "desktop/app/src/main/kotlin/com/luc4n3x/levyra/desktop/app/ui/components/TrackRow.kt"
replace_once(
    track_row,
    "        downloadActions?.stateFlow?.map { it[track.id] }?.distinctUntilChanged() ?: flowOf(null)\n",
    "        downloadActions?.stateFlow?.map { data -> data.records.firstOrNull { it.id == track.id } }\n            ?.distinctUntilChanged() ?: flowOf(null)\n",
)
replace_once(
    track_row,
    "        initial = downloadActions?.stateFlow?.value?.get(track.id)\n",
    "        initial = downloadActions?.stateFlow?.value?.records?.firstOrNull { it.id == track.id }\n",
)

print("Applied PR 265 round-two download state fixes")
