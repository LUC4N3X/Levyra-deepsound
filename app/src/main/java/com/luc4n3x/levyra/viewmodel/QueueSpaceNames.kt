package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.player.queue.QueueSpaceSummary

internal fun queueSpaceDisplayName(space: QueueSpaceSummary?, strings: LevyraStrings): String =
    space?.name?.trim()?.takeIf { it.isNotEmpty() } ?: strings.queueSpaceDefaultName

internal fun duplicatedQueueSpaceName(name: String): String = "$name 2"
