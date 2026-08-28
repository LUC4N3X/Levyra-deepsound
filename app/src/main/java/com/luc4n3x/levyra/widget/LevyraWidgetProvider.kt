package com.luc4n3x.levyra.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.luc4n3x.levyra.LevyraLaunchActions
import com.luc4n3x.levyra.MainActivity
import com.luc4n3x.levyra.player.PlaybackService

class LevyraWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        LevyraWidgetCenter.render(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE -> handleToggle(context)
            ACTION_NEXT -> handleSkip(context, LevyraWidgetBridge.onNext, true)
            ACTION_PREVIOUS -> handleSkip(context, LevyraWidgetBridge.onPrevious, false)
        }
    }

    private fun handleToggle(context: Context) {
        LevyraWidgetBridge.onToggle?.let { toggle ->
            toggle()
            return
        }
        withSessionPlayer(context) { controller ->
            if (controller.mediaItemCount <= 0) return@withSessionPlayer false
            if (controller.isPlaying) controller.pause() else controller.play()
            true
        }
    }

    private fun handleSkip(context: Context, action: (() -> Unit)?, forward: Boolean) {
        if (action != null) {
            action()
            return
        }
        withSessionPlayer(context) { controller ->
            if (controller.mediaItemCount <= 0) return@withSessionPlayer false
            if (forward) controller.seekToNext() else controller.seekToPrevious()
            true
        }
    }

    private fun withSessionPlayer(context: Context, command: (MediaController) -> Boolean) {
        val appContext = context.applicationContext
        val pending = goAsync()
        val future = MediaController.Builder(
            appContext,
            SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        ).buildAsync()
        future.addListener({
            val controller = runCatching { future.get() }.getOrNull()
            val handled = controller?.let { runCatching { command(it) }.getOrDefault(false) } == true
            controller?.release()
            if (!handled) openApp(appContext)
            pending.finish()
        }, ContextCompat.getMainExecutor(appContext))
    }

    private fun openApp(context: Context, shortcut: String? = null) {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (shortcut != null) putExtra(LevyraLaunchActions.EXTRA_SHORTCUT, shortcut)
        }
        context.startActivity(intent)
    }

    companion object {
        const val ACTION_TOGGLE = "com.luc4n3x.levyra.widget.TOGGLE"
        const val ACTION_NEXT = "com.luc4n3x.levyra.widget.NEXT"
        const val ACTION_PREVIOUS = "com.luc4n3x.levyra.widget.PREVIOUS"
    }
}
