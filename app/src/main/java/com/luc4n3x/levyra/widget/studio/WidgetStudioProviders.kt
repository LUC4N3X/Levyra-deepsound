package com.luc4n3x.levyra.widget.studio

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.luc4n3x.levyra.player.SystemMediaCommand
import com.luc4n3x.levyra.widget.LevyraWidgetActions

abstract class StudioWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetStudioEngine.requestRender(context, goAsync())
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        WidgetStudioEngine.requestRender(context, goAsync())
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        WidgetStudioStore.deleteConfigs(context, appWidgetIds)
    }
}

class LevyraMiniWidgetProvider : StudioWidgetProvider()
class LevyraSlimWidgetProvider : StudioWidgetProvider()
class LevyraExpressiveWidgetProvider : StudioWidgetProvider()
class LevyraHeroWidgetProvider : StudioWidgetProvider()
class LevyraImmersiveWidgetProvider : StudioWidgetProvider()
class LevyraVinylWidgetProvider : StudioWidgetProvider()
class LevyraQuickActionsWidgetProvider : StudioWidgetProvider()

val StudioWidgetFamily.providerClass: Class<out StudioWidgetProvider>
    get() = when (this) {
        StudioWidgetFamily.Mini -> LevyraMiniWidgetProvider::class.java
        StudioWidgetFamily.Slim -> LevyraSlimWidgetProvider::class.java
        StudioWidgetFamily.Expressive -> LevyraExpressiveWidgetProvider::class.java
        StudioWidgetFamily.Hero -> LevyraHeroWidgetProvider::class.java
        StudioWidgetFamily.Immersive -> LevyraImmersiveWidgetProvider::class.java
        StudioWidgetFamily.Vinyl -> LevyraVinylWidgetProvider::class.java
        StudioWidgetFamily.QuickActions -> LevyraQuickActionsWidgetProvider::class.java
    }

class WidgetStudioActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE -> LevyraWidgetActions.toggle(this, context)
            ACTION_NEXT -> LevyraWidgetActions.skip(this, context, forward = true)
            ACTION_PREVIOUS -> LevyraWidgetActions.skip(this, context, forward = false)
            ACTION_LIKE, ACTION_SHUFFLE, ACTION_REPEAT -> sendSystemCommand(context, intent.action.orEmpty())
        }
    }

    private fun sendSystemCommand(context: Context, action: String) {
        val appContext = context.applicationContext
        val state = WidgetStudioStore.playbackState(appContext)
        val target = targetCommand(action, state)
        if (target == null) {
            WidgetStudioEngine.requestRender(appContext)
            return
        }
        LevyraWidgetActions.withController(this, appContext) { controller, finish ->
            val command = SessionCommand(target.customAction, Bundle.EMPTY)
            if (controller == null || !controller.isSessionCommandAvailable(command)) {
                WidgetStudioEngine.requestRender(appContext)
                finish()
                return@withController
            }
            Futures.addCallback(
                controller.sendCustomCommand(command, Bundle.EMPTY),
                object : FutureCallback<SessionResult> {
                    override fun onSuccess(result: SessionResult) {
                        if (result.resultCode == SessionResult.RESULT_SUCCESS) {
                            applyOptimistic(appContext, state, target)
                        }
                        WidgetStudioEngine.requestRender(appContext)
                        finish()
                    }

                    override fun onFailure(error: Throwable) {
                        WidgetStudioEngine.requestRender(appContext)
                        finish()
                    }
                },
                ContextCompat.getMainExecutor(appContext)
            )
        }
    }

    private fun targetCommand(action: String, state: StudioPlaybackState): SystemMediaCommand? = when (action) {
        ACTION_LIKE -> when (state.favorite) {
            StudioFavoriteState.Favorite -> SystemMediaCommand.RemoveFavorite
            StudioFavoriteState.NotFavorite -> SystemMediaCommand.AddFavorite
            StudioFavoriteState.Unavailable -> null
        }
        ACTION_SHUFFLE -> state.shuffle?.let { if (it) SystemMediaCommand.DisableShuffle else SystemMediaCommand.EnableShuffle }
        ACTION_REPEAT -> when (state.repeat) {
            StudioRepeatState.Off -> SystemMediaCommand.SetRepeatAll
            StudioRepeatState.All -> SystemMediaCommand.SetRepeatOne
            StudioRepeatState.One -> SystemMediaCommand.SetRepeatOff
            StudioRepeatState.Unavailable -> null
        }
        else -> null
    }

    private fun applyOptimistic(context: Context, state: StudioPlaybackState, command: SystemMediaCommand) {
        val favorite = when (command) {
            SystemMediaCommand.AddFavorite -> StudioFavoriteState.Favorite
            SystemMediaCommand.RemoveFavorite -> StudioFavoriteState.NotFavorite
            else -> state.favorite
        }
        val shuffle = when (command) {
            SystemMediaCommand.EnableShuffle -> true
            SystemMediaCommand.DisableShuffle -> false
            else -> state.shuffle
        }
        val repeat = when (command) {
            SystemMediaCommand.SetRepeatOff -> StudioRepeatState.Off
            SystemMediaCommand.SetRepeatAll -> StudioRepeatState.All
            SystemMediaCommand.SetRepeatOne -> StudioRepeatState.One
            else -> state.repeat
        }
        WidgetStudioStore.writeActions(context, favorite, shuffle, repeat)
    }

    companion object {
        const val ACTION_TOGGLE = "com.luc4n3x.levyra.widget.studio.TOGGLE"
        const val ACTION_NEXT = "com.luc4n3x.levyra.widget.studio.NEXT"
        const val ACTION_PREVIOUS = "com.luc4n3x.levyra.widget.studio.PREVIOUS"
        const val ACTION_LIKE = "com.luc4n3x.levyra.widget.studio.LIKE"
        const val ACTION_SHUFFLE = "com.luc4n3x.levyra.widget.studio.SHUFFLE"
        const val ACTION_REPEAT = "com.luc4n3x.levyra.widget.studio.REPEAT"
    }
}
