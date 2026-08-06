from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


app_path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
app = app_path.read_text(encoding="utf-8")

app = replace_once(
    app,
    '''    val onShortcut: (ExploreShortcut) -> Unit = { shortcut ->
        if (shortcut == ExploreShortcut.Samples) {
            viewModel.beginSamplesPlayback()
            samplesStartIndex = 0
        } else {
''',
    '''    val onShortcut: (ExploreShortcut) -> Unit = { shortcut ->
        if (shortcut == ExploreShortcut.Samples) {
            samplesStartIndex = 0
            if (samples.isNotEmpty()) {
                viewModel.beginSamplesPlayback()
            } else {
                viewModel.refreshSamples()
            }
        } else {
''',
    "make Samples shortcut retry an empty feed",
)

app = replace_once(
    app,
    '''        if (ExploreAnchor.Samples in availableAnchors) {
            ExploreShortcutCard(
                icon = Icons.Rounded.SlowMotionVideo,
                label = strings.exploreSamples,
                onClick = { onSelect(ExploreShortcut.Samples) }
            )
        }
''',
    '''        ExploreShortcutCard(
            icon = Icons.Rounded.SlowMotionVideo,
            label = strings.exploreSamples,
            onClick = { onSelect(ExploreShortcut.Samples) }
        )
''',
    "keep Samples shortcut visible",
)

app_path.write_text(app, encoding="utf-8")

screen_vm_path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraScreenViewModels.kt")
screen_vm = screen_vm_path.read_text(encoding="utf-8")
screen_vm = replace_once(
    screen_vm,
    '''    fun ensureExplore(strings: LevyraStrings) = root.ensureExplore(strings)
    fun beginSamplesPlayback() = root.beginSamplesPlayback()
''',
    '''    fun ensureExplore(strings: LevyraStrings) = root.ensureExplore(strings)
    fun refreshSamples() = root.refreshExploreSamples()
    fun beginSamplesPlayback() = root.beginSamplesPlayback()
''',
    "expose explicit Samples refresh",
)
screen_vm_path.write_text(screen_vm, encoding="utf-8")

root_vm_path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")
root_vm = root_vm_path.read_text(encoding="utf-8")
root_vm = replace_once(
    root_vm,
    '''    fun ensureExplore(strings: LevyraStrings) {
        if (_state.value.exploreZoneId == null) {
            selectExploreZone(ExploreCatalog.getZones(strings).first())
        }
        ensureMusicVideosLoaded()
    }

    fun selectExploreZone(zone: ExploreZone) {
''',
    '''    fun ensureExplore(strings: LevyraStrings) {
        if (_state.value.exploreZoneId == null) {
            selectExploreZone(ExploreCatalog.getZones(strings).first())
        }
        ensureMusicVideosLoaded()
    }

    fun refreshExploreSamples() {
        if (musicVideosJob?.isActive == true) return
        musicVideosLoadedLanguage = ""
        musicVideosRetryLanguage = ""
        musicVideosRetryAfterMs = 0L
        musicVideosFailureCount = 0
        ensureMusicVideosLoaded()
    }

    fun selectExploreZone(zone: ExploreZone) {
''',
    "add manual Samples refresh",
)
root_vm_path.write_text(root_vm, encoding="utf-8")

assert "viewModel.refreshSamples()" in app
assert "fun refreshSamples() = root.refreshExploreSamples()" in screen_vm
assert "fun refreshExploreSamples()" in root_vm
print("Samples shortcut is now persistent and retries the Shorts feed when empty")
