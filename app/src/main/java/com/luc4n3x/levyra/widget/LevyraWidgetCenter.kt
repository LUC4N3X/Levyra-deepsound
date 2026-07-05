package com.luc4n3x.levyra.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import com.luc4n3x.levyra.LevyraLaunchActions
import com.luc4n3x.levyra.MainActivity
import com.luc4n3x.levyra.R
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

object LevyraWidgetCenter {
    private const val PREFS_NAME = "levyra_widget"
    private const val KEY_TITLE = "title"
    private const val KEY_ARTIST = "artist"
    private const val KEY_ARTWORK = "artwork"
    private const val KEY_PLAYING = "playing"

    @Volatile
    private var cachedArtworkUrl: String = ""

    @Volatile
    private var cachedArtwork: Bitmap? = null

    private val fetching = AtomicBoolean(false)

    fun update(context: Context, title: String?, artist: String?, artworkUrl: String?, isPlaying: Boolean) {
        val appContext = context.applicationContext
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_TITLE, title.orEmpty())
            .putString(KEY_ARTIST, artist.orEmpty())
            .putString(KEY_ARTWORK, artworkUrl.orEmpty())
            .putBoolean(KEY_PLAYING, isPlaying)
            .apply()
        render(appContext)
    }

    fun setPlaying(context: Context, isPlaying: Boolean) {
        val appContext = context.applicationContext
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_PLAYING, isPlaying)
            .apply()
        render(appContext)
    }

    fun render(context: Context) {
        val appContext = context.applicationContext
        runCatching { renderInternal(appContext) }
            .onFailure { Timber.w(it, "Widget render failed") }
    }

    private fun renderInternal(context: Context) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val ids = manager.getAppWidgetIds(ComponentName(context, LevyraWidgetProvider::class.java))
        if (ids == null || ids.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val title = prefs.getString(KEY_TITLE, "").orEmpty()
        val artist = prefs.getString(KEY_ARTIST, "").orEmpty()
        val artworkUrl = prefs.getString(KEY_ARTWORK, "").orEmpty()
        val playing = prefs.getBoolean(KEY_PLAYING, false)
        val views = RemoteViews(context.packageName, R.layout.levyra_widget)
        views.setTextViewText(R.id.widget_title, title.ifBlank { context.getString(R.string.widget_idle_title) })
        views.setTextViewText(R.id.widget_artist, artist.ifBlank { context.getString(R.string.widget_idle_subtitle) })
        views.setImageViewResource(R.id.widget_toggle, if (playing) R.drawable.ic_widget_pause else R.drawable.ic_widget_play)
        val artwork = cachedArtwork?.takeIf { artworkUrl.isNotBlank() && cachedArtworkUrl == artworkUrl }
        if (artwork != null) {
            views.setImageViewBitmap(R.id.widget_artwork, artwork)
        } else {
            views.setImageViewResource(R.id.widget_artwork, R.drawable.ic_widget_note)
        }
        views.setOnClickPendingIntent(R.id.widget_root, openApp(context, null))
        views.setOnClickPendingIntent(R.id.widget_toggle, broadcast(context, LevyraWidgetProvider.ACTION_TOGGLE))
        views.setOnClickPendingIntent(R.id.widget_next, broadcast(context, LevyraWidgetProvider.ACTION_NEXT))
        views.setOnClickPendingIntent(R.id.widget_previous, broadcast(context, LevyraWidgetProvider.ACTION_PREVIOUS))
        views.setOnClickPendingIntent(R.id.widget_favorites, openApp(context, LevyraLaunchActions.SHORTCUT_FAVORITES))
        views.setOnClickPendingIntent(R.id.widget_flow, openApp(context, LevyraLaunchActions.SHORTCUT_FLOW))
        views.setOnClickPendingIntent(R.id.widget_offline, openApp(context, LevyraLaunchActions.SHORTCUT_OFFLINE))
        views.setOnClickPendingIntent(R.id.widget_lyrics, openApp(context, LevyraLaunchActions.SHORTCUT_LYRICS))
        manager.updateAppWidget(ids, views)
        if (artwork == null && artworkUrl.isNotBlank()) {
            fetchArtwork(context, artworkUrl)
        }
    }

    private fun openApp(context: Context, shortcut: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (shortcut != null) putExtra(LevyraLaunchActions.EXTRA_SHORTCUT, shortcut)
        }
        val requestCode = shortcut?.hashCode() ?: 0
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun broadcast(context: Context, action: String): PendingIntent {
        val intent = Intent(context, LevyraWidgetProvider::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun fetchArtwork(context: Context, url: String) {
        if (!fetching.compareAndSet(false, true)) return
        Thread {
            var connection: HttpURLConnection? = null
            try {
                connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                val bytes = connection.inputStream.use { it.readBytes() }
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= 256 && bounds.outHeight / (sample * 2) >= 256) {
                    sample *= 2
                }
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                if (bitmap != null) {
                    cachedArtwork = bitmap
                    cachedArtworkUrl = url
                    render(context)
                }
            } catch (error: Exception) {
                Timber.w(error, "Widget artwork fetch failed")
            } finally {
                connection?.disconnect()
                fetching.set(false)
            }
        }.start()
    }
}
