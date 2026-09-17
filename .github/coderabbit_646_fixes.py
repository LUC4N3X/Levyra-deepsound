from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    target = Path(path)
    text = target.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one match in {path}, found {count}")
    target.write_text(text.replace(old, new, 1), encoding="utf-8")


replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/locallibrary/LocalLibraryRepository.kt",
    """    private var activeJob: Job? = null
    private var pendingMode: LocalScanMode? = null
""",
    """    private var activeJob: Job? = null
    private var pendingMode: LocalScanMode? = null
    private var loopRunning: Boolean = false
""",
)

replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/locallibrary/LocalLibraryRepository.kt",
    """    fun requestScan(mode: LocalScanMode, force: Boolean = false): Job = synchronized(requestLock) {
        val running = activeJob
        if (running != null && running.isActive) {
            pendingMode = strongerLocalScanMode(pendingMode, mode)
            return running
        }
        pendingMode = null
        scope.launch { runScanLoop(mode, force) }.also { activeJob = it }
    }
""",
    """    fun requestScan(mode: LocalScanMode, force: Boolean = false): Job = synchronized(requestLock) {
        val running = activeJob
        if (running != null && running.isActive && loopRunning) {
            pendingMode = strongerLocalScanMode(pendingMode, mode)
            return running
        }
        pendingMode = null
        loopRunning = true
        scope.launch { runScanLoop(mode, force) }.also { activeJob = it }
    }
""",
)

replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/locallibrary/LocalLibraryRepository.kt",
    """            mode = synchronized(requestLock) {
                val next = pendingMode
                pendingMode = null
                next
            } ?: return
""",
    """            mode = synchronized(requestLock) {
                val next = pendingMode
                pendingMode = null
                if (next == null) loopRunning = false
                next
            } ?: return
""",
)

replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/locallibrary/LocalLibraryRepository.kt",
    """        val merged = reconcileDownloadedTracks(current, identified.map(::toDownloadEntity), ::contentReadable)
""",
    """        val merged = reconcileDownloadedTracks(
            current,
            identified.filter { it.levyraTrackId.isNotBlank() }.map(::toDownloadEntity),
            ::contentReadable
        )
""",
)

replace_once(
    "app/src/main/java/com/luc4n3x/levyra/player/AndroidAutoLibrary.kt",
    """            .setMediaId(trackMediaId(track))
""",
    """            .setMediaId(LevyraMediaItemFactory.mediaId(track))
""",
)

replace_once(
    "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt",
    """            queueEngine.state.collectLatest { queueSnapshot ->
                refreshLocalQueueAvailability(queueSnapshot.tracks)
                val previousIndex = queueIndex
""",
    """            launch {
                queueEngine.state
                    .map { it.tracks }
                    .distinctUntilChanged()
                    .collectLatest { tracks -> refreshLocalQueueAvailability(tracks) }
            }
            queueEngine.state.collect { queueSnapshot ->
                val previousIndex = queueIndex
""",
)

replace_once(
    "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt",
    """            result.failed -> strings.localScanning
""",
    """            result.failed -> strings.localScanFailed
""",
)

replace_once(
    "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt",
    """        _state.update { it.copy(queueSwitching = true) }
        playJob?.cancel()
        playRequestId++
        streamTransitionId++
        cancelResolutionSideJobs()
        player.pause()
        val loaded = withContext(Dispatchers.IO) { queueEngine.switchSpace(spaceId, outgoingPositionMs) }
        _state.update { it.copy(queueSwitching = false) }
""",
    """        _state.update { it.copy(queueSwitching = true) }
        val loaded = try {
            playJob?.cancel()
            playRequestId++
            streamTransitionId++
            cancelResolutionSideJobs()
            player.pause()
            withContext(Dispatchers.IO) { queueEngine.switchSpace(spaceId, outgoingPositionMs) }
        } finally {
            _state.update { it.copy(queueSwitching = false) }
        }
""",
)

replace_once(
    "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt",
    """        queueSpaceJob?.cancel()
        queueSpaceJob = viewModelScope.launch {
            if (spaceId == _state.value.activeQueueSpaceId) {
                val fallback = spaces.firstOrNull { it.id != spaceId } ?: return@launch
                performQueueSpaceSwitch(fallback.id)
                if (queueEngine.state.value.spaceId == spaceId) return@launch
            }
            queueEngine.deleteSpace(spaceId)
        }
""",
    """        if (spaceId != _state.value.activeQueueSpaceId) {
            viewModelScope.launch { queueEngine.deleteSpace(spaceId) }
            return
        }
        queueSpaceJob?.cancel()
        queueSpaceJob = viewModelScope.launch {
            val fallback = spaces.firstOrNull { it.id != spaceId } ?: return@launch
            performQueueSpaceSwitch(fallback.id)
            if (queueEngine.state.value.spaceId == spaceId) return@launch
            queueEngine.deleteSpace(spaceId)
        }
""",
)

replace_once(
    "app/src/main/java/com/luc4n3x/levyra/ui/i18n/LevyraStrings.kt",
    """    val localScanning: String get() = value(\"localScanning\")
    val localScanUpToDate: String get() = value(\"localScanUpToDate\")
""",
    """    val localScanning: String get() = value(\"localScanning\")
    val localScanFailed: String get() = localScanFailedLocalization(code)
    val localScanUpToDate: String get() = value(\"localScanUpToDate\")
""",
)

replace_once(
    "app/src/main/java/com/luc4n3x/levyra/ui/i18n/LevyraLocalLibraryStrings.kt",
    """internal fun localLibraryLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(localLibraryBundles, code)
""",
    """private val localScanFailedMessages = mapOf(
    \"en\" to \"Couldn't scan your music.\",
    \"it\" to \"Impossibile analizzare la musica.\",
    \"es\" to \"No se pudo analizar tu música.\",
    \"fr\" to \"Impossible d’analyser votre musique.\",
    \"de\" to \"Deine Musik konnte nicht gescannt werden.\",
    \"pt\" to \"Não foi possível analisar a tua música.\",
    \"nl\" to \"Je muziek kon niet worden gescand.\",
    \"pl\" to \"Nie udało się przeskanować muzyki.\",
    \"ro\" to \"Muzica nu a putut fi scanată.\",
    \"el\" to \"Δεν ήταν δυνατή η σάρωση της μουσικής σου.\",
    \"sv\" to \"Det gick inte att skanna din musik.\",
    \"da\" to \"Din musik kunne ikke scannes.\",
    \"cs\" to \"Hudbu se nepodařilo prohledat.\",
    \"sk\" to \"Hudbu sa nepodarilo prehľadať.\",
    \"hr\" to \"Skeniranje glazbe nije uspjelo.\",
    \"bg\" to \"Музиката не можа да бъде сканирана.\",
    \"hu\" to \"A zene beolvasása nem sikerült.\",
    \"fi\" to \"Musiikin skannaus epäonnistui.\",
    \"nb\" to \"Kunne ikke skanne musikken din.\",
    \"ca\" to \"No s'ha pogut analitzar la música.\",
    \"uk\" to \"Не вдалося просканувати музику.\",
    \"ru\" to \"Не удалось просканировать музыку.\",
    \"tr\" to \"Müziğin taranamadı.\",
    \"ar\" to \"تعذّر فحص الموسيقى.\",
    \"fa\" to \"اسکن موسیقی انجام نشد.\",
    \"zh\" to \"无法扫描你的音乐。\",
    \"zh-Hant\" to \"無法掃描你的音樂。\",
    \"ja\" to \"音楽をスキャンできませんでした。\",
    \"ko\" to \"음악을 검색하지 못했습니다.\",
    \"hi\" to \"आपके संगीत को स्कैन नहीं किया जा सका।\",
    \"id\" to \"Musikmu tidak dapat dipindai.\",
    \"ms\" to \"Muzik anda tidak dapat diimbas.\",
    \"vi\" to \"Không thể quét nhạc của bạn.\",
    \"th\" to \"ไม่สามารถสแกนเพลงของคุณได้\",
    \"fil\" to \"Hindi ma-scan ang musika mo.\",
    \"he\" to \"לא ניתן היה לסרוק את המוזיקה שלך.\"
)

internal fun localScanFailedLocalization(code: String): String =
    localScanFailedMessages[code] ?: localScanFailedMessages.getValue(\"en\")

internal fun localLibraryLocalizationEntries(code: String): Map<String, String> =
    localizedBundleOrEnglish(localLibraryBundles, code)
""",
)

replace_once(
    "app/src/test/java/com/luc4n3x/levyra/ui/i18n/LevyraNewFeatureLocalizationTest.kt",
    """            val summary = strings.formatLocalScanSummary(3, 2, 1)
            assertTrue(\"$code switch message lost the queue name\", switched.contains(\"Gym\"))
            assertTrue(\"$code scan summary lost its counters\", listOf(\"3\", \"2\", \"1\").all(summary::contains))
""",
    """            val summary = strings.formatLocalScanSummary(3, 2, 1)
            assertTrue(\"$code switch message lost the queue name\", switched.contains(\"Gym\"))
            assertTrue(\"$code scan summary lost its counters\", listOf(\"3\", \"2\", \"1\").all(summary::contains))
            assertTrue(\"$code scan failure message is blank\", strings.localScanFailed.isNotBlank())
""",
)
