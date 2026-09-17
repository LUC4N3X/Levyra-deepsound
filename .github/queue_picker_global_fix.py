from pathlib import Path
import sys

ROOT = Path('.')


def replace_once(path: str, old: str, new: str) -> None:
    target = ROOT / path
    text = target.read_text(encoding='utf-8')
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'Expected exactly one match in {path}, found {count}')
    target.write_text(text.replace(old, new, 1), encoding='utf-8')


def write_test() -> None:
    path = ROOT / 'app/src/test/java/com/luc4n3x/levyra/player/queue/QueueDestinationPolicyTest.kt'
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text('''package com.luc4n3x.levyra.player.queue\n\nimport org.junit.Assert.assertFalse\nimport org.junit.Assert.assertTrue\nimport org.junit.Test\n\nclass QueueDestinationPolicyTest {\n    @Test\n    fun `one queue adds directly but multiple queues require destination selection`() {\n        assertFalse(shouldPromptForQueueDestination(emptyList()))\n        assertFalse(shouldPromptForQueueDestination(listOf(space("current"))))\n        assertTrue(shouldPromptForQueueDestination(listOf(space("current"), space("gym"))))\n    }\n\n    private fun space(id: String) = QueueSpaceSummary(\n        id = id,\n        name = id,\n        createdAt = 0L,\n        updatedAt = 0L,\n        lastActiveAt = 0L,\n        isActive = id == "current",\n        trackCount = 0,\n        durationMs = 0L,\n        artworkUrls = emptyList()\n    )\n}\n''', encoding='utf-8')


def apply_production() -> None:
    policy = ROOT / 'app/src/main/java/com/luc4n3x/levyra/player/queue/QueueDestinationPolicy.kt'
    policy.write_text('''package com.luc4n3x.levyra.player.queue\n\ninternal fun shouldPromptForQueueDestination(queueSpaces: List<QueueSpaceSummary>): Boolean =\n    queueSpaces.size > 1\n''', encoding='utf-8')

    replace_once(
        'app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraUiState.kt',
        '''    val queueSpaces: List<QueueSpaceSummary> = emptyList(),\n    val activeQueueSpaceId: String = DEFAULT_QUEUE_SPACE_ID,\n    val queueSwitching: Boolean = false,\n''',
        '''    val queueSpaces: List<QueueSpaceSummary> = emptyList(),\n    val activeQueueSpaceId: String = DEFAULT_QUEUE_SPACE_ID,\n    val pendingQueueAddTrack: Track? = null,\n    val queueSwitching: Boolean = false,\n'''
    )

    replace_once(
        'app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt',
        '''import com.luc4n3x.levyra.player.queue.QueueSpaceSummary\n''',
        '''import com.luc4n3x.levyra.player.queue.QueueSpaceSummary\nimport com.luc4n3x.levyra.player.queue.shouldPromptForQueueDestination\n'''
    )

    replace_once(
        'app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt',
        '''    fun addToQueue(track: Track) {\n        if (routeJamAction(JamAction.AddTrack(toJamTrack(track)))) return\n        addToQueueLocal(track)\n    }\n''',
        '''    fun addToQueue(track: Track) {\n        if (routeJamAction(JamAction.AddTrack(toJamTrack(track)))) return\n        if (shouldPromptForQueueDestination(_state.value.queueSpaces)) {\n            _state.update { it.copy(pendingQueueAddTrack = track) }\n            return\n        }\n        addToQueueLocal(track)\n    }\n\n    fun dismissQueueDestinationPicker() {\n        if (_state.value.pendingQueueAddTrack == null) return\n        _state.update { it.copy(pendingQueueAddTrack = null) }\n    }\n\n    fun addPendingTrackToQueueSpace(spaceId: String) {\n        val pending = _state.value.pendingQueueAddTrack ?: return\n        _state.update { it.copy(pendingQueueAddTrack = null) }\n        if (spaceId == _state.value.activeQueueSpaceId) {\n            addToQueueLocal(pending)\n            return\n        }\n        addTracksToQueueSpace(spaceId, listOf(pending))\n    }\n'''
    )

    replace_once(
        'app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt',
        '''            trackActionTarget?.let { target ->\n''',
        '''            state.pendingQueueAddTrack?.let {\n                QueueSpaceDestinationDialog(\n                    spaces = state.queueSpaces,\n                    activeSpaceId = state.activeQueueSpaceId,\n                    onDismiss = viewModel::dismissQueueDestinationPicker,\n                    onSelect = viewModel::addPendingTrackToQueueSpace\n                )\n            }\n\n            trackActionTarget?.let { target ->\n'''
    )

    replace_once(
        'app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt',
        '''                    onAddToQueue = { viewModel.addToQueue(target) },\n                    queueSpaces = state.queueSpaces,\n''',
        '''                    onAddToQueue = {\n                        viewModel.addTracksToQueueSpace(state.activeQueueSpaceId, listOf(target))\n                    },\n                    queueSpaces = state.queueSpaces,\n'''
    )

    replace_once(
        'app/src/main/java/com/luc4n3x/levyra/ui/player/QueueSpacesPanel.kt',
        '''import androidx.compose.material3.CircularProgressIndicator\n''',
        '''import androidx.compose.material3.AlertDialog\nimport androidx.compose.material3.CircularProgressIndicator\n'''
    )
    replace_once(
        'app/src/main/java/com/luc4n3x/levyra/ui/player/QueueSpacesPanel.kt',
        '''import androidx.compose.material3.Text\n''',
        '''import androidx.compose.material3.Text\nimport androidx.compose.material3.TextButton\n'''
    )
    replace_once(
        'app/src/main/java/com/luc4n3x/levyra/ui/player/QueueSpacesPanel.kt',
        '''internal fun queueSpaceLabel(space: QueueSpaceSummary, strings: LevyraStrings): String =\n    space.name.trim().ifEmpty { strings.queueSpaceDefaultName }\n\n''',
        '''internal fun queueSpaceLabel(space: QueueSpaceSummary, strings: LevyraStrings): String =\n    space.name.trim().ifEmpty { strings.queueSpaceDefaultName }\n\n@Composable\ninternal fun QueueSpaceDestinationDialog(\n    spaces: List<QueueSpaceSummary>,\n    activeSpaceId: String,\n    onDismiss: () -> Unit,\n    onSelect: (String) -> Unit\n) {\n    val strings = LocalLevyraStrings.current\n    val orderedSpaces = remember(spaces, activeSpaceId) {\n        spaces.sortedBy { it.id != activeSpaceId }\n    }\n    AlertDialog(\n        onDismissRequest = onDismiss,\n        title = {\n            Text(\n                text = strings.addToQueue,\n                color = LevyraText,\n                fontWeight = FontWeight.Black\n            )\n        },\n        text = {\n            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {\n                orderedSpaces.forEach { space ->\n                    TextButton(\n                        onClick = { onSelect(space.id) },\n                        modifier = Modifier.fillMaxWidth()\n                    ) {\n                        Row(\n                            modifier = Modifier.fillMaxWidth(),\n                            verticalAlignment = Alignment.CenterVertically,\n                            horizontalArrangement = Arrangement.spacedBy(10.dp)\n                        ) {\n                            Icon(\n                                imageVector = if (space.id == activeSpaceId) {\n                                    Icons.Rounded.Check\n                                } else {\n                                    Icons.AutoMirrored.Rounded.QueueMusic\n                                },\n                                contentDescription = null,\n                                tint = if (space.id == activeSpaceId) LevyraCyan else LevyraMuted,\n                                modifier = Modifier.size(20.dp)\n                            )\n                            Column(modifier = Modifier.weight(1f)) {\n                                Text(\n                                    text = queueSpaceLabel(space, strings),\n                                    color = LevyraText,\n                                    fontWeight = FontWeight.Bold\n                                )\n                                Text(\n                                    text = if (space.trackCount == 0) {\n                                        strings.queueSpaceEmpty\n                                    } else {\n                                        strings.formatTrackCount(space.trackCount)\n                                    },\n                                    color = LevyraMuted,\n                                    fontSize = 11.sp\n                                )\n                            }\n                        }\n                    }\n                }\n            }\n        },\n        confirmButton = {},\n        dismissButton = {\n            TextButton(onClick = onDismiss) {\n                Text(strings.cancel)\n            }\n        }\n    )\n}\n\n'''
    )


phase = sys.argv[1] if len(sys.argv) > 1 else ''
if phase == 'test':
    write_test()
elif phase == 'prod':
    apply_production()
else:
    raise SystemExit('usage: queue_picker_global_fix.py [test|prod]')
