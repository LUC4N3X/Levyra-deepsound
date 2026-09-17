package com.luc4n3x.levyra.player.queue

internal fun shouldPromptForQueueDestination(queueSpaces: List<QueueSpaceSummary>): Boolean =
    queueSpaces.size > 1
