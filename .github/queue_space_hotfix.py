from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    target = Path(path)
    text = target.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one match in {path}, found {count}")
    target.write_text(text.replace(old, new, 1), encoding="utf-8")


replace_once(
    "app/src/main/java/com/luc4n3x/levyra/player/queue/PersistentQueueEngine.kt",
    """        val snapshot = if (restored != null && restored.tracks.isNotEmpty()) {
            restored.toRuntimeSnapshot()
        } else {
            buildSnapshot(
                tracks = fallbackTracks,
                currentIndex = fallbackIndex,
                positionMs = fallbackPositionMs,
                repeatMode = fallbackRepeatMode,
                shuffleEnabled = fallbackShuffleEnabled,
                radioEnabled = fallbackRadioEnabled,
                generation = 1L
            ).copy(spaceId = spaceId)
        }
""",
    """        val snapshot = restored?.toRuntimeSnapshot() ?: buildSnapshot(
            tracks = fallbackTracks,
            currentIndex = fallbackIndex,
            positionMs = fallbackPositionMs,
            repeatMode = fallbackRepeatMode,
            shuffleEnabled = fallbackShuffleEnabled,
            radioEnabled = fallbackRadioEnabled,
            generation = 1L
        ).copy(spaceId = spaceId)
""",
)

replace_once(
    "app/src/test/java/com/luc4n3x/levyra/player/queue/QueueSpaceEngineTest.kt",
    """    @Test
    fun restoreOfAnEmptyActiveSpaceKeepsItsIdentityForTheFallbackQueue() = runBlocking {
        val storage = FakeQueueSpaceStorage()
        storage.put(persisted("drive", emptyList(), currentIndex = -1, positionMs = 0L))
        storage.activeId = "drive"
        val engine = PersistentQueueEngine(storage, ManualDispatcher())

        val restored = engine.restore(listOf(track("last")), 0, 12_000L)

        assertEquals("drive", restored.spaceId)
        assertEquals(listOf("last"), restored.tracks.map { it.id })
        assertEquals(12_000L, restored.positionMs)
    }
""",
    """    @Test
    fun restoreOfAnEmptyActiveSpaceDoesNotInheritFallbackQueue() = runBlocking {
        val storage = FakeQueueSpaceStorage()
        storage.put(persisted("drive", emptyList(), currentIndex = -1, positionMs = 0L))
        storage.activeId = "drive"
        val engine = PersistentQueueEngine(storage, ManualDispatcher())

        val restored = engine.restore(listOf(track("last")), 0, 12_000L)

        assertEquals("drive", restored.spaceId)
        assertTrue(restored.tracks.isEmpty())
        assertEquals(-1, restored.currentIndex)
        assertEquals(0L, restored.positionMs)
    }
""",
)

replace_once(
    "app/src/main/java/com/luc4n3x/levyra/ui/components/LevyraTrackActionSheet.kt",
    """    var queueSpacePickerVisible by remember { mutableStateOf(false) }
    val otherQueueSpaces = remember(queueSpaces, activeQueueSpaceId) {
        queueSpaces.filter { it.id != activeQueueSpaceId }
    }
""",
    """    var queueSpacePickerVisible by remember { mutableStateOf(false) }
    val queueSpaceChoices = remember(queueSpaces, activeQueueSpaceId) {
        queueSpaces.sortedBy { it.id != activeQueueSpaceId }
    }
""",
)

replace_once(
    "app/src/main/java/com/luc4n3x/levyra/ui/components/LevyraTrackActionSheet.kt",
    """                            TrackActionTile(
                                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                label = strings.addToQueue,
                                animationsEnabled = animationsEnabled,
                                modifier = Modifier.weight(1f),
                                onClick = { perform(onAddToQueue) }
                            )
""",
    """                            TrackActionTile(
                                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                label = strings.addToQueue,
                                animationsEnabled = animationsEnabled,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (queueSpaceChoices.size > 1) {
                                        queueSpacePickerVisible = !queueSpacePickerVisible
                                    } else {
                                        perform(onAddToQueue)
                                    }
                                }
                            )
""",
)

replace_once(
    "app/src/main/java/com/luc4n3x/levyra/ui/components/LevyraTrackActionSheet.kt",
    """                            if (otherQueueSpaces.isNotEmpty()) {
                                TrackActionRow(
                                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                    label = strings.queueSpaceAddTo,
                                    onClick = { queueSpacePickerVisible = !queueSpacePickerVisible }
                                )
                                if (queueSpacePickerVisible) {
                                    otherQueueSpaces.forEach { space ->
                                        TrackActionRow(
                                            icon = Icons.Rounded.Add,
                                            label = queueSpaceLabel(space, strings),
                                            tint = LevyraCyan,
                                            onClick = { perform { onAddToQueueSpace(space.id) } }
                                        )
                                    }
                                }
                            }
""",
    """                            if (queueSpacePickerVisible && queueSpaceChoices.size > 1) {
                                queueSpaceChoices.forEach { space ->
                                    TrackActionRow(
                                        icon = Icons.Rounded.Add,
                                        label = queueSpaceLabel(space, strings),
                                        tint = LevyraCyan,
                                        onClick = {
                                            perform {
                                                if (space.id == activeQueueSpaceId) {
                                                    onAddToQueue()
                                                } else {
                                                    onAddToQueueSpace(space.id)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
""",
)

replace_once(
    "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt",
    """    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(LevyraInk, LevyraBlack)))
            .consumeOverlayTouches()
    ) {
        LazyColumn(
""",
    """    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(LevyraInk, LevyraBlack)))
    ) {
        LazyColumn(
""",
)
