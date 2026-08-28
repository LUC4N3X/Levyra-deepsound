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
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicLong

object RecognitionWidgetCenter {
    @Volatile private var artworkUrl = ""
    @Volatile private var artwork: Bitmap? = null
    @Volatile private var latestState: RecognitionState = RecognitionState.Idle
    @Volatile private var desiredArtworkUrl = ""
    @Volatile private var fetchingArtworkUrl = ""
    private val artworkGeneration = AtomicLong(0L)
    private val fetchLock = Any()

    fun render(context: Context, state: RecognitionState) {
        val appContext = context.applicationContext
        latestState = state
        val resultArtworkUrl = (state as? RecognitionState.Result)?.result?.artworkUrl.orEmpty()
        if (resultArtworkUrl != desiredArtworkUrl) {
            desiredArtworkUrl = resultArtworkUrl
            artworkGeneration.incrementAndGet()
            // The old tiny bitmap is no longer useful once the requested result changes. Drop the
            // strong reference immediately instead of retaining two artworks during the next fetch.
            artwork = null
            artworkUrl = ""
        }

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
            val cached = artwork?.takeIf { resultArtworkUrl.isNotBlank() && artworkUrl == resultArtworkUrl }
            if (cached != null) views.setImageViewBitmap(R.id.recognition_widget_action, cached)
            views.setOnClickPendingIntent(R.id.recognition_widget_root, RecognitionWidgetProvider.toggleIntent(appContext))
            views.setOnClickPendingIntent(R.id.recognition_widget_action, RecognitionWidgetProvider.toggleIntent(appContext))
            manager.updateAppWidget(id, views)
        }
        if (artwork?.takeIf { artworkUrl == resultArtworkUrl } == null && resultArtworkUrl.isNotBlank()) {
            fetchArtwork(appContext, resultArtworkUrl, artworkGeneration.get())
        }
    }

    private fun fetchArtwork(context: Context, rawUrl: String, generation: Long) {
        val safeUrl = SafeImageUrlPolicy.sanitize(rawUrl)
        if (safeUrl.isEmpty()) return
        synchronized(fetchLock) {
            if (fetchingArtworkUrl.isNotBlank()) return
            fetchingArtworkUrl = rawUrl
        }
        Thread {
            var connection: HttpURLConnection? = null
            var published = false
            try {
                connection = URL(safeUrl).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 5_000
                connection.readTimeout = 5_000
                if (connection.responseCode != HttpURLConnection.HTTP_OK ||
                    !SafeImageUrlPolicy.isAllowedImageMimeType(connection.contentType)
                ) return@Thread

                val declaredLength = connection.contentLengthLong
                if (declaredLength > MAX_WIDGET_ARTWORK_BYTES) return@Thread

                // A home-screen widget only needs a 128px result. Keep one fixed bounded input
                // buffer and decode directly from it, avoiding ByteArrayOutputStream + toByteArray()
                // which previously could retain two multi-megabyte copies of the same response.
                val bytes = ByteArray(MAX_WIDGET_ARTWORK_BYTES + 1)
                var total = 0
                connection.inputStream.use { input ->
                    while (total < bytes.size) {
                        val read = input.read(bytes, total, bytes.size - total)
                        if (read < 0) break
                        total += read
                    }
                }
                if (total <= 0 || total > MAX_WIDGET_ARTWORK_BYTES) return@Thread

                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, total, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@Thread

                var sample = 1
                while (bounds.outWidth / sample > ARTWORK_TARGET_PX || bounds.outHeight / sample > ARTWORK_TARGET_PX) {
                    sample *= 2
                }
                val decoded = BitmapFactory.decodeByteArray(
                    bytes,
                    0,
                    total,
                    BitmapFactory.Options().apply {
                        inSampleSize = sample
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                )
                if (decoded != null && generation == artworkGeneration.get() && desiredArtworkUrl == rawUrl) {
                    artwork = decoded
                    artworkUrl = rawUrl
                    published = true
                }
            } catch (_: Exception) {
                // Artwork is optional; keep the recognition state usable without it.
            } finally {
                connection?.disconnect()
                val superseded = generation != artworkGeneration.get() || desiredArtworkUrl != rawUrl
                synchronized(fetchLock) {
                    if (fetchingArtworkUrl == rawUrl) fetchingArtworkUrl = ""
                }
                if (published || superseded) render(context, latestState)
            }
        }.apply {
            name = "Levyra-recognition-widget-art"
            isDaemon = true
        }.start()
    }

    private const val ARTWORK_TARGET_PX = 128
    private const val MAX_WIDGET_ARTWORK_BYTES = 1024 * 1024
}
