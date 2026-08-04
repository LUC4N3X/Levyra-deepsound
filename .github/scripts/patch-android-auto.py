from pathlib import Path

path = Path("app/src/main/java/com/luc4n3x/levyra/player/AndroidAutoLibrary.kt")
text = path.read_text(encoding="utf-8")


def replace_once(old: str, new: str, label: str) -> None:
    global text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, found {count}")
    text = text.replace(old, new, 1)


replace_once(
    "import kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.withContext\n",
    "import kotlinx.coroutines.CoroutineScope\n"
    "import kotlinx.coroutines.Dispatchers\n"
    "import kotlinx.coroutines.Job\n"
    "import kotlinx.coroutines.SupervisorJob\n"
    "import kotlinx.coroutines.launch\n"
    "import kotlinx.coroutines.withContext\n",
    "Android Auto coroutine imports",
)
replace_once(
    "    private val searchCache = ConcurrentHashMap<String, TimedTracks>()\n",
    "    private val searchCache = ConcurrentHashMap<String, TimedTracks>()\n"
    "    private val searchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)\n"
    "    private val searchInFlight = ConcurrentHashMap<String, Job>()\n",
    "Android Auto search fields",
)
replace_once(
    """    fun preloadSearch(query: String) {
        if (query.cleanQuery().isBlank()) return
    }

    suspend fun search(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val clean = query.cleanQuery()
        if (clean.isBlank()) return@withContext flowTracks().map { trackItem(it) }
        searchTracks(clean).map { trackItem(it) }
    }
""",
    """    fun preloadSearch(query: String) {
        val clean = query.voiceSearchQuery()
        if (clean.isBlank()) return
        val now = System.currentTimeMillis()
        if (searchCache[clean]?.let { now - it.createdAt < SEARCH_TTL_MS } == true) return
        if (searchInFlight.containsKey(clean)) return

        val job = searchScope.launch {
            try {
                searchTracks(clean)
            } catch (error: Throwable) {
                Timber.d(error, "Android Auto search preload failed")
            } finally {
                searchInFlight.remove(clean)
            }
        }
        searchInFlight.putIfAbsent(clean, job)?.let { job.cancel() }
    }

    suspend fun search(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val clean = query.voiceSearchQuery()
        if (clean.isBlank()) return@withContext flowTracks().map { trackItem(it) }
        searchTracks(clean).map { trackItem(it) }
    }
""",
    "Android Auto preload and search",
)
replace_once(
    """    private suspend fun searchLocal(query: String): List<Track> = withContext(Dispatchers.IO) {
        val needle = query.lowercase(Locale.ROOT)
        buildList {
            addAll(favorites())
            addAll(recents())
            addAll(downloads())
            playlists().forEach { addAll(it.tracks) }
        }.filter { track ->
            track.title.lowercase(Locale.ROOT).contains(needle) ||
                track.artist.lowercase(Locale.ROOT).contains(needle) ||
                track.album.lowercase(Locale.ROOT).contains(needle)
        }.distinctTracks().take(20)
    }
""",
    """    private suspend fun searchLocal(query: String): List<Track> = withContext(Dispatchers.IO) {
        val needle = query.lowercase(Locale.ROOT)
        val tokens = query.searchTokens()
        buildList {
            addAll(favorites())
            addAll(recents())
            addAll(downloads())
            playlists().forEach { addAll(it.tracks) }
        }.filter { track ->
            val searchable = listOf(track.title, track.artist, track.album, track.source)
                .joinToString(" ")
                .lowercase(Locale.ROOT)
            searchable.contains(needle) || tokens.isNotEmpty() && tokens.all(searchable::contains)
        }.sortedByDescending { track ->
            val title = track.title.lowercase(Locale.ROOT)
            val artist = track.artist.lowercase(Locale.ROOT)
            when {
                title == needle -> 5
                title.startsWith(needle) -> 4
                artist == needle -> 3
                title.contains(needle) -> 2
                else -> 1
            }
        }.distinctTracks().take(24)
    }
""",
    "Android Auto local search",
)

helper_marker = "    private fun String.sha256(): String = MessageDigest.getInstance(\"SHA-256\")\n"
helpers = """    private fun String.voiceSearchQuery(): String {
        var value = cleanQuery()
        VOICE_COMMAND_PREFIXES.firstOrNull { value.startsWith(it, ignoreCase = true) }
            ?.let { prefix -> value = value.drop(prefix.length).trim() }
        val lowered = value.lowercase(Locale.ROOT)
        VOICE_APP_SUFFIXES.firstOrNull(lowered::endsWith)
            ?.let { suffix -> value = value.dropLast(suffix.length).trim() }
        return value.cleanQuery()
    }

    private fun String.searchTokens(): List<String> = cleanQuery()
        .lowercase(Locale.ROOT)
        .split(' ')
        .map(String::trim)
        .filter { it.length >= 2 && it !in SEARCH_STOP_WORDS }
        .distinct()

"""
replace_once(helper_marker, helpers + helper_marker, "Android Auto voice helpers")
replace_once(
    "        private const val SEARCH_TTL_MS = 3L * 60L * 1000L\n",
    "        private const val SEARCH_TTL_MS = 3L * 60L * 1000L\n"
    "        private val VOICE_COMMAND_PREFIXES = listOf(\n"
    "            \"riproduci \", \"suona \", \"metti \", \"ascolta \", \"cerca \", \"play \"\n"
    "        )\n"
    "        private val VOICE_APP_SUFFIXES = listOf(\" su levyra\", \" in levyra\")\n"
    "        private val SEARCH_STOP_WORDS = setOf(\n"
    "            \"di\", \"del\", \"della\", \"dei\", \"degli\", \"delle\", \"da\", \"by\", \"the\",\n"
    "            \"un\", \"una\", \"uno\", \"il\", \"lo\", \"la\", \"i\", \"gli\", \"le\"\n"
    "        )\n",
    "Android Auto voice constants",
)

path.write_text(text, encoding="utf-8")
