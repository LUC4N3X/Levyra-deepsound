package com.luc4n3x.levyra.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent

class LevyraWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        LevyraWidgetCenter.render(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_TOGGLE -> LevyraWidgetActions.toggle(this, context)
            ACTION_NEXT -> LevyraWidgetActions.skip(this, context, forward = true)
            ACTION_PREVIOUS -> LevyraWidgetActions.skip(this, context, forward = false)
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.luc4n3x.levyra.widget.TOGGLE"
        const val ACTION_NEXT = "com.luc4n3x.levyra.widget.NEXT"
        const val ACTION_PREVIOUS = "com.luc4n3x.levyra.widget.PREVIOUS"
    }
}
