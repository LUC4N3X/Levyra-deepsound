from pathlib import Path
import re


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


def regex_once(text: str, pattern: str, replacement: str, label: str) -> str:
    updated, count = re.subn(pattern, replacement, text, count=1, flags=re.DOTALL)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one regex match, found {count}")
    return updated


# UI state: expose a dedicated Samples session flag so the standard mini-player
# can disappear without removing the bottom navigation.
state_path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraUiState.kt")
state = state_path.read_text(encoding="utf-8")
state = replace_once(
    state,
    "    val isVideoMode: Boolean = false,\n    val showSettings: Boolean = false,\n",
    "    val isVideoMode: Boolean = false,\n    val isSamplesOpen: Boolean = false,\n    val showSettings: Boolean = false,\n",
    "add Samples UI state",
)
state_path.write_text(state, encoding="utf-8")


# Root view model: source only verified Shorts, force video mode for the immersive
# session, and restore the user's previous playback state when leaving it.
vm_path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")
vm = vm_path.read_text(encoding="utf-8")
vm = replace_once(
    vm,
    "import com.luc4n3x.levyra.data.YoutubeMusicRepository\n",
    "import com.luc4n3x.levyra.data.YoutubeMusicRepository\n"
    "import com.luc4n3x.levyra.data.YoutubeShortsRepository\n"
    "import com.luc4n3x.levyra.data.isYoutubeShortTrack\n",
    "import Shorts repository",
)
vm = replace_once(
    vm,
    "private const val ARTIST_INITIAL_BIOGRAPHY_WAIT_MS = 4_500L\n",
    "private const val ARTIST_INITIAL_BIOGRAPHY_WAIT_MS = 4_500L\n"
    "private const val EXPLORE_SHORTS_FEED_LIMIT = 24\n",
    "add Shorts feed limit",
)
vm = replace_once(
    vm,
    "    private val repository = YoutubeMusicRepository(application.applicationContext)\n",
    "    private val repository = YoutubeMusicRepository(application.applicationContext)\n"
    "    private val shortsRepository = YoutubeShortsRepository(application.applicationContext)\n",
    "initialize Shorts repository",
)
vm = regex_once(
    vm,
    r"    private fun ensureMusicVideosLoaded\(\) \{.*?\n    \}\n\n    fun ensureExplore",
    '''    private fun ensureMusicVideosLoaded() {
        val snapshot = _state.value
        val languageCode = snapshot.languageCode
        if (musicVideosJob?.isActive == true) return

        val hasVerifiedShorts = snapshot.exploreVideos.isNotEmpty() &&
            snapshot.exploreVideos.all(::isYoutubeShortTrack)
        if (musicVideosLoadedLanguage == languageCode && hasVerifiedShorts) return
        if (!hasVerifiedShorts && snapshot.exploreVideos.isNotEmpty()) {
            _state.update { current -> current.copy(exploreVideos = emptyList()) }
        }

        musicVideosJob = viewModelScope.launch {
            val seedSnapshot = _state.value
            val seeds = buildList {
                addAll(seedSnapshot.exploreTracks)
                addAll(seedSnapshot.charts)
                seedSnapshot.homeSections.forEach { section -> addAll(section.tracks) }
                addAll(seedSnapshot.tracks)
            }
                .distinctBy { track -> track.id }
                .take(32)

            val shorts = withContext(Dispatchers.IO) {
                runCatching {
                    shortsRepository.feed(
                        seeds = seeds,
                        languageCode = languageCode,
                        limit = EXPLORE_SHORTS_FEED_LIMIT
                    )
                }.getOrDefault(emptyList())
            }
            if (_state.value.languageCode != languageCode) return@launch

            musicVideosLoadedLanguage = languageCode.takeIf { shorts.isNotEmpty() }.orEmpty()
            _state.update { current ->
                if (current.languageCode == languageCode) current.copy(exploreVideos = shorts) else current
            }
        }
    }

    fun ensureExplore''',
    "replace music-video shelf with verified Shorts feed",
)
vm = replace_once(
    vm,
    "    fun playSample(list: List<Track>, track: Track) {\n",
    "    fun beginSamplesPlayback() {\n"
    "        _state.update { current ->\n"
    "            if (current.isSamplesOpen) current else current.copy(isSamplesOpen = true)\n"
    "        }\n"
    "    }\n\n"
    "    fun playSample(list: List<Track>, track: Track) {\n",
    "add Samples session entry point",
)
vm = replace_once(
    vm,
    "        val selected = list.firstOrNull { candidate -> samePlayableTrack(candidate, track) } ?: track\n",
    "        val selected = list.firstOrNull { candidate -> samePlayableTrack(candidate, track) } ?: track\n"
    "        if (!isYoutubeShortTrack(selected)) return\n"
    "        beginSamplesPlayback()\n",
    "reject non-Short sample playback",
)
vm = replace_once(
    vm,
    "        _state.update { current -> if (current.isVideoMode) current else current.copy(isVideoMode = true) }\n",
    "        _state.update { current ->\n"
    "            current.copy(isVideoMode = true, isSamplesOpen = true)\n"
    "        }\n",
    "force video mode for Samples",
)
vm = replace_once(
    vm,
    "    fun endSamplesPlayback() {\n        val session = samplesPlaybackSession ?: return\n        samplesPlaybackSession = null\n",
    "    fun endSamplesPlayback() {\n"
    "        val session = samplesPlaybackSession\n"
    "        _state.update { current ->\n"
    "            if (current.isSamplesOpen) current.copy(isSamplesOpen = false) else current\n"
    "        }\n"
    "        if (session == null) return\n"
    "        samplesPlaybackSession = null\n",
    "close Samples even before first item resolves",
)
vm_path.write_text(vm, encoding="utf-8")


# Screen view model: pass the explicit session start into Explore and include the
# flag in its projection so lifecycle changes are never filtered out.
svm_path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraScreenViewModels.kt")
svm = svm_path.read_text(encoding="utf-8")
svm = replace_once(
    svm,
    "    fun ensureExplore(strings: LevyraStrings) = root.ensureExplore(strings)\n    fun endSamplesPlayback() = root.endSamplesPlayback()\n",
    "    fun ensureExplore(strings: LevyraStrings) = root.ensureExplore(strings)\n"
    "    fun beginSamplesPlayback() = root.beginSamplesPlayback()\n"
    "    fun endSamplesPlayback() = root.endSamplesPlayback()\n",
    "expose Samples session start",
)
svm = regex_once(
    svm,
    r"private data class ExploreProjection\((.*?)val isPlaying: Boolean\n\)",
    lambda match: "private data class ExploreProjection(" + match.group(1) +
        "val isPlaying: Boolean,\n    val isSamplesOpen: Boolean\n)",
    "extend Explore projection data",
)
svm = replace_once(
    svm,
    "    isPlaying = state.isPlaying\n)\n",
    "    isPlaying = state.isPlaying,\n    isSamplesOpen = state.isSamplesOpen\n)\n",
    "map Samples projection state",
)
svm_path.write_text(svm, encoding="utf-8")


# Pure Explore model: ordinary music videos must never leak into Samples.
layout_path = Path("app/src/main/java/com/luc4n3x/levyra/ui/ExploreLayout.kt")
layout = layout_path.read_text(encoding="utf-8")
layout = replace_once(
    layout,
    "import com.luc4n3x.levyra.domain.Track\n",
    "import com.luc4n3x.levyra.domain.Track\nimport com.luc4n3x.levyra.data.isYoutubeShortTrack\n",
    "import short classifier in layout",
)
layout = replace_once(
    layout,
    "    return videos.asSequence()\n        .filter { track -> track.thumbnailUrl.isNotBlank() || track.largeThumbnailUrl.isNotBlank() }\n",
    "    return videos.asSequence()\n"
    "        .filter(::isYoutubeShortTrack)\n"
    "        .filter { track -> track.thumbnailUrl.isNotBlank() || track.largeThumbnailUrl.isNotBlank() }\n",
    "filter Samples to actual Shorts",
)
layout_path.write_text(layout, encoding="utf-8")


# Explore UI: opening any Samples entry starts the immersive session, while the
# mini-player is hidden and the normal bottom navigation remains available.
app_path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
app = app_path.read_text(encoding="utf-8")
app = replace_once(
    app,
    "                        visible = state.currentTrack != null,\n                        enter = miniEnter,\n",
    "                        visible = state.currentTrack != null && !state.isSamplesOpen,\n                        enter = miniEnter,\n",
    "hide standard mini-player in Samples",
)
app = replace_once(
    app,
    "        if (shortcut == ExploreShortcut.Samples) {\n            samplesStartIndex = 0\n",
    "        if (shortcut == ExploreShortcut.Samples) {\n"
    "            viewModel.beginSamplesPlayback()\n"
    "            samplesStartIndex = 0\n",
    "start Samples from shortcut",
)
app = replace_once(
    app,
    "    val onPlaySamples: (() -> Unit)? = if (samples.isNotEmpty()) {\n        { samplesStartIndex = 0 }\n",
    "    val onPlaySamples: (() -> Unit)? = if (samples.isNotEmpty()) {\n"
    "        {\n"
    "            viewModel.beginSamplesPlayback()\n"
    "            samplesStartIndex = 0\n"
    "        }\n",
    "start Samples from section action",
)
app = replace_once(
    app,
    "                        onOpen = { track ->\n                            samplesStartIndex = samples.indexOfFirst { candidate -> candidate.id == track.id }\n",
    "                        onOpen = { track ->\n"
    "                            viewModel.beginSamplesPlayback()\n"
    "                            samplesStartIndex = samples.indexOfFirst { candidate -> candidate.id == track.id }\n",
    "start Samples from preview card",
)
app = replace_once(
    app,
    "            .heightIn(min = 50.dp)\n            .clip(shape)\n",
    "            .height(58.dp)\n            .clip(shape)\n",
    "give mood cards a fixed YT Music height",
)
app = replace_once(
    app,
    "                .fillMaxHeight()\n                .width(7.dp)\n                .background(edgeBrush)\n",
    "                .fillMaxHeight()\n"
    "                .width(6.dp)\n"
    "                .background(edgeBrush)\n",
    "make mood accent rail visible",
)
app_path.write_text(app, encoding="utf-8")


# Tests: default fixtures are now real Shorts; add an explicit ordinary-video
# rejection to guard against reintroducing the original bug.
test_path = Path("app/src/test/java/com/luc4n3x/levyra/ui/ExploreLayoutTest.kt")
test = test_path.read_text(encoding="utf-8")
test = replace_once(
    test,
    "    @Test\n    fun samplesDefaultToPreviewBoundAndKeepTheImmersiveFeedBounded() {\n",
    "    @Test\n"
    "    fun samplesRejectOrdinaryMusicVideos() {\n"
    "        val ordinary = track(\"ordinary\").copy(\n"
    "            source = \"YouTube Music\",\n"
    "            videoUrl = \"https://www.youtube.com/watch?v=abcdefghijk\",\n"
    "            videoType = \"\"\n"
    "        )\n\n"
    "        assertTrue(exploreSampleTracks(listOf(ordinary)).isEmpty())\n"
    "    }\n\n"
    "    @Test\n"
    "    fun samplesDefaultToPreviewBoundAndKeepTheImmersiveFeedBounded() {\n",
    "add ordinary video regression test",
)
test = replace_once(
    test,
    "        videoUrl = \"\",\n        thumbnailUrl = thumbnailUrl,\n        largeThumbnailUrl = largeThumbnailUrl,\n        source = \"YouTube Music\",\n",
    "        videoUrl = \"https://www.youtube.com/shorts/$id\",\n"
    "        thumbnailUrl = thumbnailUrl,\n"
    "        largeThumbnailUrl = largeThumbnailUrl,\n"
    "        source = \"YouTube Shorts\",\n",
    "make Explore fixtures short-form",
)
test = replace_once(
    test,
    "        accentStart = 0xFF00E5FF.toInt(),\n        accentEnd = 0xFF2979FF.toInt()\n",
    "        accentStart = 0xFF00E5FF.toInt(),\n"
    "        accentEnd = 0xFF2979FF.toInt(),\n"
    "        videoType = \"SHORTS\"\n",
    "tag Explore fixtures as Shorts",
)
test_path.write_text(test, encoding="utf-8")


for path in (state_path, vm_path, svm_path, layout_path, app_path, test_path):
    if not path.read_text(encoding="utf-8").strip():
        raise RuntimeError(f"empty output: {path}")

print("Applied verified Shorts feed, immersive Samples session, and visible mood accent rail")
