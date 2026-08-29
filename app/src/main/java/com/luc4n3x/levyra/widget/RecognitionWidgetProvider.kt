package com.luc4n3x.levyra.widget

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.luc4n3x.levyra.LevyraLaunchActions
import com.luc4n3x.levyra.MainActivity
import com.luc4n3x.levyra.feature.recognition.LevyraRecognitionCenter
import com.luc4n3x.levyra.feature.recognition.MusicRecognitionService
import com.luc4n3x.levyra.feature.recognition.RecognitionState

class RecognitionWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        RecognitionWidgetCenter.render(context, LevyraRecognitionCenter.get(context).state.value)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: android.os.Bundle) {
        RecognitionWidgetCenter.render(context, LevyraRecognitionCenter.get(context).state.value)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_TOGGLE) return
        when (LevyraRecognitionCenter.get(context).state.value) {
            RecognitionState.Listening, RecognitionState.Identifying ->
                context.startService(MusicRecognitionService.cancelIntent(context))
            else -> if (hasPermission(context)) {
                ContextCompat.startForegroundService(context, MusicRecognitionService.microphoneIntent(context))
            } else {
                context.startActivity(permissionIntent(context))
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.luc4n3x.levyra.widget.RECOGNIZE"

        fun toggleIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            ACTION_TOGGLE.hashCode(),
            Intent(context, RecognitionWidgetProvider::class.java).setAction(ACTION_TOGGLE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        private fun hasPermission(context: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

        private fun permissionIntent(context: Context) = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(LevyraLaunchActions.EXTRA_SHORTCUT, LevyraLaunchActions.SHORTCUT_RECOGNITION)
            putExtra(LevyraLaunchActions.EXTRA_RECOGNITION_PERMISSION_REQUEST, true)
        }
    }
}
