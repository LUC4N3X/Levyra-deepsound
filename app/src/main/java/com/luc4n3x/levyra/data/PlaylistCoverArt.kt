package com.luc4n3x.levyra.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.util.LruCache
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import com.luc4n3x.levyra.domain.PlaylistCoverPlan
import com.luc4n3x.levyra.domain.PlaylistCoverStyle
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import timber.log.Timber

internal const val PLAYLIST_COVER_RENDER_PX = 1024
internal const val PLAYLIST_COVER_PREVIEW_PX = 512
private const val COVER_BASE_COLOR = 0xFF0B0B0F.toInt()
private const val COVER_TITLE_MAX_LINES = 3

internal class PlaylistCoverArtist(context: Context) {
    private val appContext = context.applicationContext

    suspend fun render(plan: PlaylistCoverPlan, sizePx: Int): Bitmap {
        val artworks = loadArtworks(plan.artworkUrls, sizePx)
        val photo = plan.photo?.let { loadPhoto(it.uri, sizePx) }
        return withContext(Dispatchers.Default) {
            renderPlaylistCover(plan, artworks, photo, sizePx)
        }
    }

    private suspend fun loadArtworks(urls: List<String>, sizePx: Int): List<Bitmap> {
        if (urls.isEmpty()) return emptyList()
        val tileSize = if (urls.size > 1) sizePx / 2 else sizePx
        return coroutineScope {
            urls.map { url -> async(Dispatchers.IO) { loadBitmap(LevyraArtworkCache.large(url), tileSize) } }
                .awaitAll()
                .filterNotNull()
        }
    }

    private suspend fun loadPhoto(uri: String, sizePx: Int): Bitmap? =
        withContext(Dispatchers.IO) { loadBitmap(Uri.parse(uri), sizePx * 2) }

    private suspend fun loadBitmap(data: Any, sizePx: Int): Bitmap? {
        val request = ImageRequest.Builder(appContext)
            .data(data)
            .size(Size(sizePx, sizePx))
            .allowHardware(false)
            .build()
        return try {
            (SingletonImageLoader.get(appContext).execute(request) as? SuccessResult)?.image?.toBitmap()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "Playlist cover artwork unavailable")
            null
        }
    }
}

internal object PlaylistCoverPreviewCache {
    private const val MAX_BYTES = 8 * 1024 * 1024
    private val cache = object : LruCache<PlaylistCoverPlan, Bitmap>(MAX_BYTES) {
        override fun sizeOf(key: PlaylistCoverPlan, value: Bitmap): Int = value.allocationByteCount
    }

    fun get(plan: PlaylistCoverPlan): Bitmap? = cache.get(plan)

    fun put(plan: PlaylistCoverPlan, bitmap: Bitmap) {
        cache.put(plan, bitmap)
    }
}

internal fun renderPlaylistCover(
    plan: PlaylistCoverPlan,
    artworks: List<Bitmap>,
    photo: Bitmap?,
    sizePx: Int
): Bitmap {
    val size = sizePx.coerceAtLeast(64)
    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    canvas.drawColor(COVER_BASE_COLOR)
    val bounds = RectF(0f, 0f, size.toFloat(), size.toFloat())
    when (plan.style) {
        PlaylistCoverStyle.Artwork -> {
            val artwork = artworks.firstOrNull()
            if (artwork != null) drawCropped(canvas, artwork, bounds) else drawSignal(canvas, plan, bounds, withTitle = true)
        }
        PlaylistCoverStyle.Mosaic -> {
            if (artworks.isEmpty()) drawSignal(canvas, plan, bounds, withTitle = true) else drawMosaic(canvas, artworks, bounds)
        }
        PlaylistCoverStyle.Spotlight -> drawSpotlight(canvas, plan, artworks.firstOrNull(), bounds)
        PlaylistCoverStyle.Signal -> drawSignal(canvas, plan, bounds, withTitle = true)
        PlaylistCoverStyle.Photo -> {
            val source = photo
            val crop = plan.photo
            if (source != null && crop != null) {
                val rect = playlistCoverCropRect(
                    source.width,
                    source.height,
                    PlaylistCoverCrop(crop.viewportSizePx, crop.zoom, crop.offsetX, crop.offsetY)
                )
                canvas.drawBitmap(
                    source,
                    Rect(rect.left, rect.top, rect.left + rect.size, rect.top + rect.size),
                    bounds,
                    Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
                )
            } else {
                drawSignal(canvas, plan, bounds, withTitle = false)
            }
        }
        PlaylistCoverStyle.Current,
        PlaylistCoverStyle.Automatic -> drawSignal(canvas, plan, bounds, withTitle = false)
    }
    return output
}

private fun drawCropped(canvas: Canvas, bitmap: Bitmap, target: RectF) {
    val scale = max(target.width() / bitmap.width, target.height() / bitmap.height)
    val matrix = Matrix().apply {
        setScale(scale, scale)
        postTranslate(
            target.left + (target.width() - bitmap.width * scale) / 2f,
            target.top + (target.height() - bitmap.height * scale) / 2f
        )
    }
    canvas.save()
    canvas.clipRect(target)
    canvas.drawBitmap(bitmap, matrix, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
    canvas.restore()
}

internal data class PlaylistCoverTile(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

internal fun playlistMosaicTiles(count: Int, width: Float, height: Float): List<PlaylistCoverTile> {
    val halfW = width / 2f
    val halfH = height / 2f
    return when (count.coerceIn(0, 4)) {
        0 -> emptyList()
        1 -> listOf(PlaylistCoverTile(0f, 0f, width, height))
        2 -> listOf(PlaylistCoverTile(0f, 0f, halfW, height), PlaylistCoverTile(halfW, 0f, width, height))
        3 -> listOf(
            PlaylistCoverTile(0f, 0f, halfW, height),
            PlaylistCoverTile(halfW, 0f, width, halfH),
            PlaylistCoverTile(halfW, halfH, width, height)
        )
        else -> listOf(
            PlaylistCoverTile(0f, 0f, halfW, halfH),
            PlaylistCoverTile(halfW, 0f, width, halfH),
            PlaylistCoverTile(0f, halfH, halfW, height),
            PlaylistCoverTile(halfW, halfH, width, height)
        )
    }
}

private fun drawMosaic(canvas: Canvas, artworks: List<Bitmap>, bounds: RectF) {
    val tiles = playlistMosaicTiles(artworks.size, bounds.width(), bounds.height())
    tiles.forEachIndexed { index, tile ->
        drawCropped(canvas, artworks[index], RectF(tile.left, tile.top, tile.right, tile.bottom))
    }
    val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(70, 0, 0, 0)
        strokeWidth = max(1f, bounds.width() * 0.004f)
    }
    if (tiles.size >= 2) canvas.drawLine(bounds.centerX(), 0f, bounds.centerX(), bounds.height(), divider)
    if (tiles.size == 3) canvas.drawLine(bounds.centerX(), bounds.centerY(), bounds.width(), bounds.centerY(), divider)
    if (tiles.size >= 4) canvas.drawLine(0f, bounds.centerY(), bounds.width(), bounds.centerY(), divider)
}

private fun drawField(canvas: Canvas, plan: PlaylistCoverPlan, bounds: RectF) {
    val base = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f,
            0f,
            bounds.width(),
            bounds.height(),
            intArrayOf(blend(plan.primaryColor, COVER_BASE_COLOR, 0.34f), blend(plan.secondaryColor, COVER_BASE_COLOR, 0.72f), COVER_BASE_COLOR),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(bounds, base)
}

private fun drawSignal(canvas: Canvas, plan: PlaylistCoverPlan, bounds: RectF, withTitle: Boolean) {
    drawField(canvas, plan, bounds)
    val corner = Math.floorMod(plan.seed, 4L).toInt()
    val cx = if (corner == 0 || corner == 3) bounds.width() * 0.86f else bounds.width() * 0.14f
    val cy = if (corner < 2) bounds.height() * 0.18f else bounds.height() * 0.30f
    val rings = 5 + Math.floorMod(plan.seed ushr 8, 3L).toInt()
    val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            cx,
            cy,
            bounds.width() * 0.7f,
            intArrayOf(withAlpha(plan.primaryColor, 150), withAlpha(plan.secondaryColor, 40), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(bounds, glow)
    val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = bounds.width() * 0.006f
    }
    val step = bounds.width() * 0.105f
    for (index in 1..rings) {
        val fade = 1f - index / (rings + 1f)
        ring.color = withAlpha(blend(plan.primaryColor, Color.WHITE, 0.35f), (fade * 150).roundToInt())
        canvas.drawCircle(cx, cy, step * index, ring)
    }
    if (withTitle) drawTitle(canvas, plan.title, bounds)
}

private fun drawSpotlight(canvas: Canvas, plan: PlaylistCoverPlan, artwork: Bitmap?, bounds: RectF) {
    drawField(canvas, plan, bounds)
    if (artwork == null) {
        drawSignal(canvas, plan, bounds, withTitle = true)
        return
    }
    val side = bounds.width() * 0.58f
    val composition = Math.floorMod(plan.seed ushr 16, 3L).toInt()
    val left = when (composition) {
        0 -> bounds.width() * 0.08f
        1 -> bounds.width() - side - bounds.width() * 0.08f
        else -> (bounds.width() - side) / 2f
    }
    val top = bounds.height() * 0.08f
    val frame = RectF(left, top, left + side, top + side)
    val radius = side * 0.05f
    val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            frame.centerX(),
            frame.centerY() + side * 0.08f,
            side * 0.78f,
            intArrayOf(Color.argb(150, 0, 0, 0), Color.TRANSPARENT),
            null,
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawCircle(frame.centerX(), frame.centerY() + side * 0.08f, side * 0.78f, shadow)
    canvas.save()
    val clip = android.graphics.Path().apply { addRoundRect(frame, radius, radius, android.graphics.Path.Direction.CW) }
    canvas.clipPath(clip)
    drawCropped(canvas, artwork, frame)
    canvas.restore()
    drawTitle(canvas, plan.title, bounds)
}

private fun drawTitle(canvas: Canvas, title: String, bounds: RectF) {
    if (title.isBlank()) return
    val margin = bounds.width() * 0.08f
    val width = (bounds.width() - margin * 2).roundToInt().coerceAtLeast(1)
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textSize = titleTextSize(title, bounds.width())
        letterSpacing = -0.02f
        setShadowLayer(bounds.width() * 0.02f, 0f, bounds.width() * 0.004f, Color.argb(120, 0, 0, 0))
    }
    val layout = StaticLayout.Builder.obtain(title, 0, title.length, paint, width)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setMaxLines(COVER_TITLE_MAX_LINES)
        .setEllipsize(TextUtils.TruncateAt.END)
        .setLineSpacing(0f, 0.92f)
        .setIncludePad(false)
        .build()
    val scrim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f,
            bounds.height() * 0.55f,
            0f,
            bounds.height(),
            intArrayOf(Color.TRANSPARENT, Color.argb(150, 0, 0, 0)),
            null,
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, bounds.height() * 0.55f, bounds.width(), bounds.height(), scrim)
    canvas.save()
    canvas.translate(margin, bounds.height() - margin - layout.height)
    layout.draw(canvas)
    canvas.restore()
}

internal fun titleTextSize(title: String, width: Float): Float {
    val length = title.trim().length
    val ratio = when {
        length <= 10 -> 0.125f
        length <= 22 -> 0.10f
        length <= 40 -> 0.082f
        else -> 0.068f
    }
    return width * ratio
}

private fun blend(color: Int, other: Int, fraction: Float): Int {
    val f = fraction.coerceIn(0f, 1f)
    val inverse = 1f - f
    return Color.rgb(
        (Color.red(color) * inverse + Color.red(other) * f).roundToInt(),
        (Color.green(color) * inverse + Color.green(other) * f).roundToInt(),
        (Color.blue(color) * inverse + Color.blue(other) * f).roundToInt()
    )
}

private fun withAlpha(color: Int, alpha: Int): Int =
    Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
