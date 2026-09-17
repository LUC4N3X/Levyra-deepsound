from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected 1 match, found {count}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8")


replace_once(
    "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraUiState.kt",
    "    val pendingQueueAddTrack: Track? = null,\n",
    "    val pendingQueueAddTracks: List<Track> = emptyList(),\n",
)

replace_once(
    "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt",
    """            state.pendingQueueAddTrack?.let {\n                QueueSpaceDestinationDialog(\n                    spaces = state.queueSpaces,\n                    activeSpaceId = state.activeQueueSpaceId,\n                    onDismiss = viewModel::dismissQueueDestinationPicker,\n                    onSelect = viewModel::addPendingTrackToQueueSpace\n                )\n            }\n""",
    """            if (state.pendingQueueAddTracks.isNotEmpty()) {\n                QueueSpaceDestinationDialog(\n                    spaces = state.queueSpaces,\n                    activeSpaceId = state.activeQueueSpaceId,\n                    onDismiss = viewModel::dismissQueueDestinationPicker,\n                    onSelect = viewModel::addPendingTrackToQueueSpace\n                )\n            }\n""",
)

replace_once(
    "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt",
    "import com.luc4n3x.levyra.player.queue.shouldPromptForQueueDestination\n",
    "import com.luc4n3x.levyra.player.queue.shouldPromptForQueueDestination\nimport com.luc4n3x.levyra.player.queue.mergePendingQueueDestinationTracks\n",
)

replace_once(
    "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt",
    """        LevyraTypographyController.apply(snapshot.interfaceSettings.fontPreset)\n""",
    """        localLibrarySortFlow.value =\n            snapshot.interfaceSettings.librarySort to snapshot.interfaceSettings.librarySortDirection\n        LevyraTypographyController.apply(snapshot.interfaceSettings.fontPreset)\n""",
)

replace_once(
    "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt",
    """        if (shouldPromptForQueueDestination(_state.value.queueSpaces)) {\n            _state.update { it.copy(pendingQueueAddTrack = track) }\n            return\n        }\n""",
    """        if (shouldPromptForQueueDestination(_state.value.queueSpaces)) {\n            _state.update { current ->\n                current.copy(\n                    pendingQueueAddTracks = mergePendingQueueDestinationTracks(\n                        current.pendingQueueAddTracks,\n                        listOf(track)\n                    )\n                )\n            }\n            return\n        }\n""",
)

replace_once(
    "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt",
    """    fun dismissQueueDestinationPicker() {\n        if (_state.value.pendingQueueAddTrack == null) return\n        _state.update { it.copy(pendingQueueAddTrack = null) }\n    }\n\n    fun addPendingTrackToQueueSpace(spaceId: String) {\n        val pending = _state.value.pendingQueueAddTrack ?: return\n        _state.update { it.copy(pendingQueueAddTrack = null) }\n        if (spaceId == _state.value.activeQueueSpaceId) {\n            addToQueueLocal(pending)\n            return\n        }\n        addTracksToQueueSpace(spaceId, listOf(pending))\n    }\n""",
    """    fun dismissQueueDestinationPicker() {\n        if (_state.value.pendingQueueAddTracks.isEmpty()) return\n        _state.update { it.copy(pendingQueueAddTracks = emptyList()) }\n    }\n\n    fun addPendingTrackToQueueSpace(spaceId: String) {\n        val pending = _state.value.pendingQueueAddTracks\n        if (pending.isEmpty()) return\n        _state.update { it.copy(pendingQueueAddTracks = emptyList()) }\n        if (spaceId == _state.value.activeQueueSpaceId) {\n            pending.forEach(::addToQueueLocal)\n            return\n        }\n        addTracksToQueueSpace(spaceId, pending)\n    }\n""",
)

policy_path = Path("app/src/main/java/com/luc4n3x/levyra/player/queue/QueueDestinationPolicy.kt")
policy = policy_path.read_text(encoding="utf-8")
if "mergePendingQueueDestinationTracks" in policy:
    raise SystemExit("QueueDestinationPolicy.kt already contains merge helper")
policy_path.write_text(
    policy.replace(
        "package com.luc4n3x.levyra.player.queue\n\n",
        "package com.luc4n3x.levyra.player.queue\n\nimport com.luc4n3x.levyra.domain.Track\n\n",
        1,
    )
    + """\ninternal fun mergePendingQueueDestinationTracks(\n    current: List<Track>,\n    additions: List<Track>\n): List<Track> = (current + additions).distinctBy { track ->\n    track.id.ifBlank { "${track.title}|${track.artist}" }\n}\n""",
    encoding="utf-8",
)

test_path = Path("app/src/test/java/com/luc4n3x/levyra/player/queue/QueueDestinationPolicyTest.kt")
test = test_path.read_text(encoding="utf-8")
if "pending destination keeps every unique track" in test:
    raise SystemExit("QueueDestinationPolicyTest already contains regression test")
test = test.replace(
    "package com.luc4n3x.levyra.player.queue\n\n",
    "package com.luc4n3x.levyra.player.queue\n\nimport com.luc4n3x.levyra.domain.Track\nimport org.junit.Assert.assertEquals\n",
    1,
)
needle = """    private fun space(id: String) = QueueSpaceSummary(\n"""
addition = """    @Test\n    fun `pending destination keeps every unique track across repeated adds`() {\n        val first = track("1")\n        val second = track("2")\n\n        val pending = mergePendingQueueDestinationTracks(\n            mergePendingQueueDestinationTracks(emptyList(), listOf(first)),\n            listOf(second, first)\n        )\n\n        assertEquals(listOf("1", "2"), pending.map { it.id })\n    }\n\n    private fun track(id: String) = Track(\n        id = id,\n        title = "Title $id",\n        artist = "Artist",\n        album = "",\n        durationMs = 180_000L,\n        streamUrl = "",\n        videoUrl = "",\n        thumbnailUrl = "",\n        largeThumbnailUrl = "",\n        source = "test",\n        moodTags = emptySet(),\n        energy = 0,\n        vocal = 0,\n        replayScore = 0,\n        cacheScore = 0,\n        accentStart = 0,\n        accentEnd = 0\n    )\n\n"""
if test.count(needle) != 1:
    raise SystemExit("QueueDestinationPolicyTest.kt: insertion point mismatch")
test_path.write_text(test.replace(needle, addition + needle, 1), encoding="utf-8")

print("Targeted CodeRabbit patch applied")
