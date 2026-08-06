from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")
text = path.read_text(encoding="utf-8")

text = replace_once(
    text,
    "import com.luc4n3x.levyra.data.isYoutubeShortTrack\n",
    "import com.luc4n3x.levyra.data.isYoutubeShortTrack\n"
    "import com.luc4n3x.levyra.data.youtubeShortsRetryDelayMs\n",
    "import Shorts retry policy",
)

text = replace_once(
    text,
    '''    private val exploreCache = ConcurrentHashMap<String, List<Track>>()
    private var musicVideosLoadedLanguage = ""
    private var musicVideosJob: Job? = null
    private var exploreJob: Job? = null
''',
    '''    private val exploreCache = ConcurrentHashMap<String, List<Track>>()
    private var musicVideosLoadedLanguage = ""
    private var musicVideosRetryLanguage = ""
    private var musicVideosRetryAfterMs = 0L
    private var musicVideosFailureCount = 0
    private var musicVideosJob: Job? = null
    private var exploreJob: Job? = null
''',
    "add Shorts feed retry state",
)

old_function = '''    private fun ensureMusicVideosLoaded() {
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
'''

new_function = '''    private fun ensureMusicVideosLoaded() {
        val snapshot = _state.value
        val languageCode = snapshot.languageCode
        if (musicVideosJob?.isActive == true) return
        if (musicVideosLoadedLanguage == languageCode) return

        val now = System.currentTimeMillis()
        if (musicVideosRetryLanguage != languageCode) {
            musicVideosRetryLanguage = languageCode
            musicVideosRetryAfterMs = 0L
            musicVideosFailureCount = 0
        }
        if (now < musicVideosRetryAfterMs) return

        val hasVerifiedShorts = snapshot.exploreVideos.isNotEmpty() &&
            snapshot.exploreVideos.all(::isYoutubeShortTrack)
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

            val feedResult = try {
                shortsRepository.feed(
                    seeds = seeds,
                    languageCode = languageCode,
                    limit = EXPLORE_SHORTS_FEED_LIMIT
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Timber.w(error, "Shorts feed failed for %s", languageCode)
                null
            }
            if (_state.value.languageCode != languageCode) return@launch

            if (feedResult == null || !feedResult.isConclusive) {
                registerShortsFeedFailure(languageCode)
                return@launch
            }

            musicVideosLoadedLanguage = languageCode
            musicVideosRetryLanguage = ""
            musicVideosRetryAfterMs = 0L
            musicVideosFailureCount = 0
            _state.update { current ->
                if (current.languageCode == languageCode) {
                    current.copy(exploreVideos = feedResult.tracks)
                } else {
                    current
                }
            }
        }
    }

    private fun registerShortsFeedFailure(languageCode: String) {
        if (musicVideosRetryLanguage != languageCode) {
            musicVideosRetryLanguage = languageCode
            musicVideosFailureCount = 0
        }
        musicVideosFailureCount = (musicVideosFailureCount + 1).coerceAtMost(5)
        musicVideosRetryAfterMs = System.currentTimeMillis() +
            youtubeShortsRetryDelayMs(musicVideosFailureCount)
    }
'''

text = replace_once(text, old_function, new_function, "replace Shorts loading flow")
path.write_text(text, encoding="utf-8")

assert "runCatching {\n                    shortsRepository.feed" not in text
assert "musicVideosRetryAfterMs" in text
assert "catch (error: CancellationException)" in text
print("Applied cancellation-safe Shorts loading and bounded retry backoff")
