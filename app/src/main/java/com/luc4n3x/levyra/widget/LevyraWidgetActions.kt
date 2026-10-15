package com.luc4n3x.levyra.widget

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.luc4n3x.levyra.LevyraLaunchActions
import com.luc4n3x.levyra.MainActivity
import com.luc4n3x.levyra.player.PlaybackService

internal object LevyraWidgetActions {

    fun toggle(receiver: BroadcastReceiver, context: Context) {
        LevyraWidgetBridge.onToggle?.let { toggle ->
            toggle()
            return
        }
        withSessionPlayer(receiver, context) { controller ->
            if (controller.mediaItemCount <= 0) return@withSessionPlayer false
            if (controller.isPlaying) controller.pause() else controller.play()
            true
        }
    }

    fun skip(receiver: BroadcastReceiver, context: Context, forward: Boolean) {
        val action = if (forward) LevyraWidgetBridge.onNext else LevyraWidgetBridge.onPrevious
        if (action != null) {
            action()
            return
        }
        withSessionPlayer(receiver, context) { controller ->
            if (controller.mediaItemCount <= 0) return@withSessionPlayer false
            val command = if (forward) Player.COMMAND_SEEK_TO_NEXT else Player.COMMAND_SEEK_TO_PREVIOUS
            if (!controller.isCommandAvailable(command)) return@withSessionPlayer false
            if (forward) controller.seekToNext() else controller.seekToPrevious()
            true
        }
    }

    fun withController(
        receiver: BroadcastReceiver,
        context: Context,
        command: (MediaController?, finish: () -> Unit) -> Unit
    ) {
        val appContext = context.applicationContext
        val pending = receiver.goAsync()
        val future = MediaController.Builder(
            appContext,
            SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        ).buildAsync()
        future.addListener({
            val controller = runCatching { future.get() }.getOrNull()
            var finished = false
            val finish = {
                if (!finished) {
                    finished = true
                    controller?.release()
                    pending.finish()
                }
            }
            runCatching { command(controller, finish) }.onFailure { finish() }
        }, ContextCompat.getMainExecutor(appContext))
    }

    private fun withSessionPlayer(receiver: BroadcastReceiver, context: Context, command: (MediaController) -> Boolean) {
        val appContext = context.applicationContext
        val pending = receiver.goAsync()
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

    fun openApp(context: Context, shortcut: String? = null) {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (shortcut != null) putExtra(LevyraLaunchActions.EXTRA_SHORTCUT, shortcut)
        }
        context.startActivity(intent)
    }
}
