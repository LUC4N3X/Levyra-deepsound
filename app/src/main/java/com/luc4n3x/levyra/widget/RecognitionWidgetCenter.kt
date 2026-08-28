package com.luc4n3x.levyra.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import android.widget.RemoteViews
import com.luc4n3x.levyra.R
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.data.security.SafeImageUrlPolicy
import com.luc4n3x.levyra.feature.recognition.RecognitionState
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

object RecognitionWidgetCenter {
    @Volatile private var artworkUrl = ""
    @Volatile private var artwork: Bitmap? = null
    private val fetching = AtomicBoolean(false)

    fun render(context: Context, state: RecognitionState) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val ids = manager.getAppWidgetIds(ComponentName(appContext, RecognitionWidgetProvider::class.java))
        val strings = LevyraStrings.forCode(LevyraPreferences(appContext).snapshot().languageCode)
        ids.forEach { id ->
            val width = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val views = RemoteViews(appContext.packageName, R.layout.recognition_widget)
            val title = when (state) {
                RecognitionState.Idle -> strings.recognitionTitle
                RecognitionState.Listening -> strings.recognitionListening
                RecognitionState.Identifying -> strings.recognitionProcessing
                RecognitionState.NoMatch -> strings.recognitionNoMatch
                is RecognitionState.Error -> strings.recognitionUnavailable
                is RecognitionState.Result -> state.result.title
            }
            val subtitle = when (state) {
                is RecognitionState.Result -> state.result.artist
                RecognitionState.Idle -> strings.recognitionTapToListen
                else -> ""
            }
            views.setTextViewText(R.id.recognition_widget_title, title)
            views.setTextViewText(R.id.recognition_widget_subtitle, subtitle)
            views.setViewVisibility(R.id.recognition_widget_text, if (width < 110) View.GONE else View.VISIBLE)
            views.setViewVisibility(R.id.recognition_widget_subtitle, if (width < 220 || subtitle.isBlank()) View.GONE else View.VISIBLE)
            views.setImageViewResource(
                R.id.recognition_widget_action,
                if (state is RecognitionState.Listening || state is RecognitionState.Identifying) R.drawable.ic_widget_pause
                else R.drawable.ic_qs_recognize
            )
            val resultArtworkUrl = (state as? RecognitionState.Result)?.result?.artworkUrl.orEmpty()
            val cached = artwork?.takeIf { resultArtworkUrl.isNotBlank() && artworkUrl == resultArtworkUrl }
            if (cached != null) views.setImageViewBitmap(R.id.recognition_widget_action, cached)
            views.setOnClickPendingIntent(R.id.recognition_widget_root, RecognitionWidgetProvider.toggleIntent(appContext))
            views.setOnClickPendingIntent(R.id.recognition_widget_action, RecognitionWidgetProvider.toggleIntent(appContext))
            manager.updateAppWidget(id, views)
            if (cached == null && resultArtworkUrl.isNotBlank()) fetchArtwork(appContext, resultArtworkUrl, state)
        }
    }

    private fun fetchArtwork(context: Context, rawUrl: String, state: RecognitionState) {
        val safeUrl = SafeImageUrlPolicy.sanitize(rawUrl)
        if (safeUrl.isEmpty() || !fetching.compareAndSet(false, true)) return
        Thread {
            var connection: HttpURLConnection? = null
            try {
                connection = URL(safeUrl).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 5_000
                connection.readTimeout = 5_000
                if (connection.responseCode != HttpURLConnection.HTTP_OK ||
                    !SafeImageUrlPolicy.isAllowedImageMimeType(connection.contentType)
                ) return@Thread
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8_192)
                var total = 0
                connection.inputStream.use { input ->
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > SafeImageUrlPolicy.MAX_IMAGE_PAYLOAD_BYTES) return@Thread
                        output.write(buffer, 0, read)
                    }
                }
                val bytes = output.toByteArray()
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sample = 1
                while (bounds.outWidth / sample > 128 || bounds.outHeight / sample > 128) sample *= 2
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
                    ?.let { decoded ->
                        artwork = decoded
                        artworkUrl = rawUrl
                        render(context, state)
                    }
            } catch (_: Exception) {
                Unit
            } finally {
                connection?.disconnect()
                fetching.set(false)
            }
        }.start()
    }
}
