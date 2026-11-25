package com.luc4n3x.levyra

import android.os.Process

internal object ForegroundUiPriority {
    private const val FOREGROUND_UI_THREAD_PRIORITY = -20

    private var restorePriority: Int? = null

    fun boost() {
        val current = runCatching { Process.getThreadPriority(Process.myTid()) }.getOrNull() ?: return
        if (current <= FOREGROUND_UI_THREAD_PRIORITY) return
        runCatching { Process.setThreadPriority(FOREGROUND_UI_THREAD_PRIORITY) }
            .onSuccess { if (restorePriority == null) restorePriority = current }
    }

    fun restore() {
        val previous = restorePriority ?: return
        restorePriority = null
        runCatching { Process.setThreadPriority(previous) }
    }
}
