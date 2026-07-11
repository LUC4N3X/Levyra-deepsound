package com.luc4n3x.levyra.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
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
        val toggle = LevyraWidgetBridge.onToggle
        if (toggle != null) {
            toggle()
            return
        }
        val player = PlaybackService.activePlayer
        if (player != null && player.mediaItemCount > 0) {
            val playing = player.isPlaying
            if (playing) player.pause() else player.play()
            LevyraWidgetCenter.setPlaying(context, !playing)
        } else {
            openApp(context)
        }
    }

    private fun handleSkip(context: Context, action: (() -> Unit)?, forward: Boolean) {
        if (action != null) {
            action()
            return
        }
        val handled = if (forward) PlaybackService.requestQueueNext() else PlaybackService.requestQueuePrevious()
        if (!handled) openApp(context)
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
