package com.luc4n3x.levyra.player.offline.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OfflineDownloadCancelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CANCEL_DOWNLOAD) return
        val taskKey = intent.getStringExtra(EXTRA_TASK_KEY).orEmpty()
        if (taskKey.isBlank()) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                OfflineExportWorker.cancel(context.applicationContext, taskKey)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_CANCEL_DOWNLOAD = "com.luc4n3x.levyra.action.CANCEL_OFFLINE_DOWNLOAD"
        const val EXTRA_TASK_KEY = "levyra.download.task_key"
    }
}
