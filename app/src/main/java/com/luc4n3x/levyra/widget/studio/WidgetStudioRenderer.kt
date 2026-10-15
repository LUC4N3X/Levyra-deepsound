package com.luc4n3x.levyra.widget.studio

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import com.luc4n3x.levyra.LevyraLaunchActions
import com.luc4n3x.levyra.MainActivity
import com.luc4n3x.levyra.R
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class StudioWidgetSize(val widthDp: Int, val heightDp: Int)

internal enum class StudioLayout(
    @LayoutRes val res: Int,
    val slotOrder: List<Int>,
    val balancedPairs: List<Pair<Int, Int>>,
    val progress: StudioProgressKind,
    val playShape: StudioPlayShape
) {
    Mini(R.layout.studio_widget_mini, emptyList(), emptyList(), StudioProgressKind.None, StudioPlayShape.Morph),
    Slim(R.layout.studio_widget_slim, listOf(1), emptyList(), StudioProgressKind.None, StudioPlayShape.Morph),
    Expressive(R.layout.studio_widget_expressive, listOf(1, 2), listOf(1 to 2), StudioProgressKind.Bar, StudioPlayShape.Pill),
    HeroFocus(R.layout.studio_widget_hero_focus, listOf(3, 1, 2, 4, 5, 6, 7, 8), listOf(1 to 2), StudioProgressKind.Row, StudioPlayShape.Morph),
    HeroSplit(R.layout.studio_widget_hero_split, listOf(1, 2, 3, 4, 5, 6, 7, 8), listOf(2 to 3), StudioProgressKind.Row, StudioPlayShape.Morph),
    ImmersiveCompact(R.layout.studio_widget_immersive_compact, listOf(3, 1, 2, 4, 5, 6, 7, 8), listOf(1 to 2), StudioProgressKind.Row, StudioPlayShape.Morph),
    Immersive(R.layout.studio_widget_immersive, listOf(1, 4, 5, 6, 7, 8, 2), listOf(1 to 2), StudioProgressKind.Row, StudioPlayShape.Morph),
    VinylSmall(R.layout.studio_widget_vinyl_small, emptyList(), emptyList(), StudioProgressKind.None, StudioPlayShape.Circle),
    VinylLarge(R.layout.studio_widget_vinyl_large, listOf(1), listOf(1 to 2), StudioProgressKind.None, StudioPlayShape.Circle),
    Quick(R.layout.studio_widget_quick, emptyList(), emptyList(), StudioProgressKind.None, StudioPlayShape.Circle),
    QuickCompact(R.layout.studio_widget_quick_compact, emptyList(), emptyList(), StudioProgressKind.None, StudioPlayShape.Circle)
}

internal enum class StudioProgressKind { None, Bar, Row }

internal enum class StudioPlayShape { Morph, Pill, Circle }

internal object WidgetStudioRenderer {
    private const val REQUEST_BASE = 0x4C57_0000
    private val ROW_SLOTS = setOf(4, 5, 6, 7, 8)
    private val SLOT_ROW_LAYOUTS = setOf(
        StudioLayout.HeroSplit, StudioLayout.Immersive, StudioLayout.HeroFocus, StudioLayout.ImmersiveCompact
    )

    private val slotRoots = intArrayOf(
        0, R.id.studio_slot1, R.id.studio_slot2, R.id.studio_slot3, R.id.studio_slot4,
        R.id.studio_slot5, R.id.studio_slot6, R.id.studio_slot7, R.id.studio_slot8
    )
    private val slotBgs = intArrayOf(
        0, R.id.studio_slot1_bg, R.id.studio_slot2_bg, R.id.studio_slot3_bg, R.id.studio_slot4_bg,
        R.id.studio_slot5_bg, R.id.studio_slot6_bg, R.id.studio_slot7_bg, R.id.studio_slot8_bg
    )
    private val slotIcons = intArrayOf(
        0, R.id.studio_slot1_icon, R.id.studio_slot2_icon, R.id.studio_slot3_icon, R.id.studio_slot4_icon,
        R.id.studio_slot5_icon, R.id.studio_slot6_icon, R.id.studio_slot7_icon, R.id.studio_slot8_icon
    )

    fun sizeOf(context: Context, options: Bundle?, family: StudioWidgetFamily): StudioWidgetSize {
        val fallback = defaultSize(family)
        if (options == null) return fallback
        val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val width = options.getInt(
            if (landscape) AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
        )
        val height = options.getInt(
            if (landscape) AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
        )
        if (width <= 0 || height <= 0) return fallback
        return StudioWidgetSize(width, height)
    }

    fun defaultSize(family: StudioWidgetFamily): StudioWidgetSize = when (family) {
        StudioWidgetFamily.Mini -> StudioWidgetSize(170, 170)
        StudioWidgetFamily.Slim -> StudioWidgetSize(330, 72)
        StudioWidgetFamily.Expressive -> StudioWidgetSize(330, 160)
        StudioWidgetFamily.Hero -> StudioWidgetSize(330, 330)
        StudioWidgetFamily.Immersive -> StudioWidgetSize(400, 320)
        StudioWidgetFamily.Vinyl -> StudioWidgetSize(170, 170)
        StudioWidgetFamily.QuickActions -> StudioWidgetSize(330, 200)
    }

    fun layoutFor(family: StudioWidgetFamily, config: StudioWidgetConfig, size: StudioWidgetSize): StudioLayout {
        val w = size.widthDp
        val h = size.heightDp
        return when (family) {
            StudioWidgetFamily.Mini -> StudioLayout.Mini
            StudioWidgetFamily.Slim -> StudioLayout.Slim
            StudioWidgetFamily.Expressive -> if (h < 104) StudioLayout.Slim else StudioLayout.Expressive
            StudioWidgetFamily.Hero -> compactFallback(w, h) ?: heroLayout(config)
            StudioWidgetFamily.Immersive -> compactFallback(w, h) ?: immersiveLayout(w, h)
            StudioWidgetFamily.Vinyl -> vinylLayout(w, h)
            StudioWidgetFamily.QuickActions -> if (h >= 250) StudioLayout.Quick else StudioLayout.QuickCompact
        }
    }

    private fun heroLayout(config: StudioWidgetConfig): StudioLayout =
        if (config.style == StudioWidgetStyle.ArtworkControls) StudioLayout.HeroSplit else StudioLayout.HeroFocus

    private fun immersiveLayout(w: Int, h: Int): StudioLayout =
        if (w >= 440 && w >= h * 1.12f) StudioLayout.Immersive else StudioLayout.ImmersiveCompact

    private fun vinylLayout(w: Int, h: Int): StudioLayout =
        if (w >= 220 && h >= 290) StudioLayout.VinylLarge else StudioLayout.VinylSmall

    private fun compactFallback(w: Int, h: Int): StudioLayout? = when {
        h < 104 -> StudioLayout.Slim
        h < 230 && w >= 250 -> StudioLayout.Expressive
        w < 210 || h < 230 -> StudioLayout.Mini
        else -> null
    }

    fun build(
        context: Context,
        family: StudioWidgetFamily,
        config: StudioWidgetConfig,
        state: StudioPlaybackState,
        size: StudioWidgetSize,
        interactive: Boolean,
        pixelScale: Float = 1f
    ): RemoteViews {
        val layout = layoutFor(family, config, size)
        val density = context.resources.displayMetrics.density
        val source = if (state.hasTrack) WidgetStudioArtwork.source(state.artworkUrl) else null
        val seed = WidgetStudioArtwork.seed(state.artworkUrl)?.takeIf { source != null }
            ?: state.accentSeed.takeIf { it != 0 && state.hasTrack }
        val scheme = WidgetStudioColors.scheme(context, config.palette, config.customColor, seed)
        val views = RemoteViews(context.packageName, layout.res)
        val scope = BindScope(context, views, layout, family, config, state, size, scheme, density * pixelScale, interactive, source, pixelScale)
        when (layout) {
            StudioLayout.Quick, StudioLayout.QuickCompact -> scope.bindQuick()
            else -> scope.bindPlayer()
        }
        return views
    }

    fun buildProgress(
        context: Context,
        family: StudioWidgetFamily,
        config: StudioWidgetConfig,
        state: StudioPlaybackState,
        size: StudioWidgetSize
    ): RemoteViews? {
        val layout = layoutFor(family, config, size)
        if (layout.progress == StudioProgressKind.None || !showsProgress(config, state)) return null
        val source = if (state.hasTrack) WidgetStudioArtwork.source(state.artworkUrl) else null
        val seed = WidgetStudioArtwork.seed(state.artworkUrl)?.takeIf { source != null }
            ?: state.accentSeed.takeIf { it != 0 && state.hasTrack }
        val scheme = WidgetStudioColors.scheme(context, config.palette, config.customColor, seed)
        val views = RemoteViews(context.packageName, layout.res)
        BindScope(
            context, views, layout, family, config, state, size, scheme,
            context.resources.displayMetrics.density, interactive = false, source = source, outputScale = 1f
        ).bindProgressOnly()
        return views
    }

    fun showsProgress(config: StudioWidgetConfig, state: StudioPlaybackState): Boolean =
        StudioControl.Progress in config.controls && state.hasTrack && state.durationMs > 0L

    private class BindScope(
        val context: Context,
        val views: RemoteViews,
        val layout: StudioLayout,
        val family: StudioWidgetFamily,
        val config: StudioWidgetConfig,
        val state: StudioPlaybackState,
        val size: StudioWidgetSize,
        val scheme: StudioColorScheme,
        val density: Float,
        val interactive: Boolean,
        val source: android.graphics.Bitmap?,
        val outputScale: Float
    ) {
        private val minimal = config.style == StudioWidgetStyle.Minimal
        private val w = size.widthDp
        private val h = size.heightDp
        private fun px(dp: Float): Int = (dp * density).roundToInt().coerceAtLeast(1)

        fun bindPlayer() {
            bindSurface()
            bindArtwork()
            bindTexts()
            bindPlay()
            bindSkips()
            bindSlots()
            bindProgress()
            if (interactive) {
                views.setOnClickPendingIntent(R.id.studio_root, rootIntent())
            }
        }

        private fun rootIntent(): PendingIntent =
            if (state.hasTrack) openApp(LevyraLaunchActions.SHORTCUT_PLAYER, 0) else openApp(null, 1)

        fun bindProgressOnly() {
            bindProgress()
        }

        private fun tint(viewId: Int, color: Int) {
            views.setInt(viewId, "setColorFilter", color or 0xFF000000.toInt())
            views.setInt(viewId, "setImageAlpha", android.graphics.Color.alpha(color))
        }

        private fun bindSurface() {
            if (layout in setOf(StudioLayout.Mini, StudioLayout.HeroFocus, StudioLayout.ImmersiveCompact, StudioLayout.VinylSmall)) return
            views.setInt(R.id.studio_bg, "setColorFilter", if (minimal) scheme.surface else scheme.surfaceContainer)
            if (layout == StudioLayout.Slim) views.setImageViewResource(R.id.studio_bg, R.drawable.studio_shape_pill)
        }

        private fun bindArtwork() {
            when (layout) {
                StudioLayout.Mini -> bindMiniArtwork()
                StudioLayout.Slim -> bindRoundedArt((h - 16).coerceIn(36, 56).coerceAtMost(w / 5).toFloat(), 14f)
                StudioLayout.Expressive -> bindRoundedArt(expressiveArtDp(), 20f)
                StudioLayout.ImmersiveCompact -> bindScrimmedArt(immersiveCompactBottomDp(), 0.15f, 0.28f, 0.04f, 0f)
                StudioLayout.HeroFocus -> bindScrimmedArt(
                    heroFocusBottomDp(), 0.2f, 0.3f, 0.02f, if (scheme.dark) 0.5f else 0.62f
                )
                StudioLayout.HeroSplit -> bindHeroSplitArt()
                StudioLayout.Immersive -> bindImmersiveArt()
                StudioLayout.VinylSmall -> bindVinyl(min(w, h).toFloat() - 8f)
                StudioLayout.VinylLarge -> bindVinyl(min(w - 32, h - 32 - 128).coerceAtLeast(80).toFloat())
                StudioLayout.Quick, StudioLayout.QuickCompact -> Unit
            }
        }

        private val artRadius: Float
            get() = 28f * density

        private fun fullBleedArt(scrim: WidgetStudioPainter.Scrim?, circular: Boolean = false) =
            WidgetStudioPainter.fullBleed(context, source, px(w.toFloat()), px(h.toFloat()), artRadius, scheme, scrim, circular)

        private fun bindMiniArtwork() {
            val circular = config.artwork == StudioArtworkMode.Circular
            val titleVisible = !minimal && !circular && state.hasTrack && w >= 150
            val scrim = if (titleVisible) {
                WidgetStudioPainter.Scrim(fadeStart = 0.42f, solidAt = 0.86f, solidAlpha = 0.82f, topAlpha = 0f)
            } else {
                null
            }
            views.setImageViewBitmap(R.id.studio_art_full, fullBleedArt(scrim, circular))
        }

        private fun bindRoundedArt(side: Float, radiusDp: Float) {
            views.setImageViewBitmap(
                R.id.studio_art,
                WidgetStudioPainter.rounded(
                    context, source, px(side), px(side), radiusDp * density,
                    config.artwork == StudioArtworkMode.Circular, scheme, outputScale
                )
            )
        }

        private fun bindScrimmedArt(bottomDp: Float, minTextTop: Float, fadeOffset: Float, solidOffset: Float, topAlpha: Float) {
            val textTop = (1f - bottomDp / h).coerceIn(minTextTop, 0.75f)
            val scrim = WidgetStudioPainter.Scrim(
                fadeStart = (textTop - fadeOffset).coerceAtLeast(0.02f),
                solidAt = (textTop + solidOffset).coerceAtMost(0.85f),
                solidAlpha = if (scheme.dark) 0.93f else 0.94f,
                topAlpha = topAlpha
            )
            views.setImageViewBitmap(R.id.studio_art_full, fullBleedArt(scrim))
        }

        private fun bindHeroSplitArt() {
            val artWidth = (w - 24).coerceAtLeast(40).toFloat()
            val artHeight = (h - 24 - heroSplitBottomDp()).coerceAtLeast(60).toFloat()
            views.setImageViewBitmap(
                R.id.studio_art,
                WidgetStudioPainter.rounded(context, source, px(artWidth), px(artHeight), 22f * density, false, scheme, outputScale)
            )
        }

        private fun bindImmersiveArt() {
            val side = immersiveArtDp()
            val art = WidgetStudioPainter.rounded(context, source, px(side), px(side), 24f * density, false, scheme, outputScale)
            views.setImageViewBitmap(R.id.studio_art, art)
            if (config.artwork != StudioArtworkMode.FullBleed) {
                views.setViewVisibility(R.id.studio_art_full, View.GONE)
                views.setViewVisibility(R.id.studio_art, View.VISIBLE)
                return
            }
            val artFraction = ((side + 14f) / w).coerceIn(0.2f, 0.6f)
            val scrim = WidgetStudioPainter.Scrim(
                fadeStart = artFraction * 0.35f,
                solidAt = (artFraction + 0.12f).coerceAtMost(0.85f),
                solidAlpha = 0.9f,
                topAlpha = 0f,
                horizontal = true
            )
            views.setImageViewBitmap(R.id.studio_art_full, fullBleedArt(scrim))
            views.setViewVisibility(R.id.studio_art_full, View.VISIBLE)
            views.setViewVisibility(R.id.studio_art, View.INVISIBLE)
            views.setViewVisibility(R.id.studio_bg, View.GONE)
        }

        private fun bindVinyl(side: Float) {
            views.setImageViewBitmap(R.id.studio_art, WidgetStudioPainter.vinyl(context, source, px(side), scheme))
        }

        private fun expressiveArtDp(): Float = min(h - 24f, (w - 24f) * 0.42f).coerceAtLeast(40f)

        private fun immersiveArtDp(): Float = min(h - 28f, (w - 28f) * 0.42f).coerceAtLeast(60f)

        private fun heroFocusBottomDp(): Float {
            val progress = if (showsProgress(config, state)) 28f else 0f
            val row = if (rowVisible()) 54f else 0f
            return 14f + 76f + 10f + progress + 14f + 21f + 6f + 58f + row
        }

        private fun immersiveCompactBottomDp(): Float {
            val progress = if (showsProgress(config, state)) 30f else 0f
            val row = if (rowVisible()) 52f else 0f
            return 14f + 46f + progress + 6f + 60f + row + 14f
        }

        private fun heroSplitBottomDp(): Int {
            val album = if (state.album.isNotBlank() && h >= 380) 18 else 0
            val progress = if (showsProgress(config, state)) 24 else 0
            val row = if (rowVisible()) 52 else 0
            return 14 + 44 + album + progress + 6 + 68 + row
        }

        private fun rowVisible(): Boolean = when (layout) {
            StudioLayout.HeroSplit -> family == StudioWidgetFamily.Immersive && h >= 400 && assignedSlots().keys.any { it in ROW_SLOTS }
            StudioLayout.Immersive -> h >= 270 && assignedSlots().keys.any { it in ROW_SLOTS }
            StudioLayout.HeroFocus -> family == StudioWidgetFamily.Immersive && assignedSlots().keys.any { it in ROW_SLOTS }
            StudioLayout.ImmersiveCompact -> assignedSlots().keys.any { it in ROW_SLOTS }
            else -> false
        }

        private val onArtwork: Boolean
            get() = layout == StudioLayout.Mini || layout == StudioLayout.HeroFocus || layout == StudioLayout.ImmersiveCompact ||
                (layout == StudioLayout.Immersive && config.artwork == StudioArtworkMode.FullBleed)

        private fun bindTexts() {
            val title = state.title.ifBlank { context.getString(R.string.widget_idle_title) }
            val artist = if (state.hasTrack) state.artist else context.getString(R.string.widget_idle_subtitle)
            when (layout) {
                StudioLayout.Mini -> {
                    bindMiniTitle(title)
                    describeRoot(title, artist)
                    return
                }
                StudioLayout.VinylSmall -> {
                    describeRoot(title, artist)
                    return
                }
                else -> Unit
            }
            views.setTextViewText(R.id.studio_title, title)
            views.setTextColor(R.id.studio_title, scheme.onSurface)
            views.setTextViewText(R.id.studio_artist, artist)
            views.setTextColor(R.id.studio_artist, scheme.onSurfaceVariant)
            val hideArtist = artist.isBlank() || (layout == StudioLayout.Slim && h < 52)
            views.setViewVisibility(R.id.studio_artist, if (hideArtist) View.GONE else View.VISIBLE)
            if (layout == StudioLayout.HeroSplit || layout == StudioLayout.Immersive) bindAlbum()
            if (layout == StudioLayout.HeroFocus || layout == StudioLayout.Immersive) bindNowPlayingLabel()
        }

        private fun bindMiniTitle(title: String) {
            val visible = !minimal && config.artwork != StudioArtworkMode.Circular && state.hasTrack && w >= 150
            views.setViewVisibility(R.id.studio_title, if (visible) View.VISIBLE else View.GONE)
            views.setTextViewText(R.id.studio_title, title)
            views.setTextColor(R.id.studio_title, scheme.onSurface)
        }

        private fun describeRoot(title: String, artist: String) {
            views.setContentDescription(R.id.studio_root, listOf(title, artist).filter { it.isNotBlank() }.joinToString(", "))
        }

        private fun bindAlbum() {
            val album = state.album.takeIf { it.isNotBlank() && it != state.title }
            val showAlbum = album != null && (layout == StudioLayout.Immersive || h >= 380)
            views.setTextViewText(R.id.studio_album, album.orEmpty())
            views.setTextColor(R.id.studio_album, WidgetStudioColors.withAlpha(scheme.onSurfaceVariant, 0.8f))
            views.setViewVisibility(R.id.studio_album, if (showAlbum) View.VISIBLE else View.GONE)
        }

        private fun bindNowPlayingLabel() {
            val label = if (state.playing) context.getString(R.string.car_now_playing) else context.getString(R.string.app_name)
            views.setTextViewText(R.id.studio_label, label.uppercase(Locale.getDefault()))
            views.setTextColor(R.id.studio_label, scheme.onSurfaceVariant)
            views.setViewVisibility(R.id.studio_badge, if (state.playing) View.VISIBLE else View.GONE)
            views.setInt(R.id.studio_badge, "setColorFilter", scheme.primary)
        }

        private fun bindPlay() {
            val shape = when (layout.playShape) {
                StudioPlayShape.Pill -> R.drawable.studio_shape_pill
                StudioPlayShape.Circle -> R.drawable.studio_shape_circle
                StudioPlayShape.Morph -> if (state.playing) R.drawable.studio_shape_squircle else R.drawable.studio_shape_circle
            }
            views.setImageViewResource(R.id.studio_play_bg, shape)
            val (background, foreground) = when {
                !minimal -> scheme.primary to scheme.onPrimary
                onArtwork -> WidgetStudioColors.withAlpha(scheme.surface, 0.86f) to scheme.onSurface
                else -> scheme.surfaceContainerHigh to scheme.onSurface
            }
            tint(R.id.studio_play_bg, background)
            views.setImageViewResource(R.id.studio_play_icon, if (state.playing) R.drawable.ic_studio_pause else R.drawable.ic_studio_play)
            views.setInt(R.id.studio_play_icon, "setColorFilter", foreground)
            views.setContentDescription(R.id.studio_play, context.getString(R.string.widget_toggle))
            if (interactive) views.setOnClickPendingIntent(R.id.studio_play, broadcast(WidgetStudioActionReceiver.ACTION_TOGGLE, 1))
        }

        private fun bindSkips() {
            if (layout == StudioLayout.Mini || layout == StudioLayout.VinylSmall) return
            val tint = scheme.onSurface
            val alpha = if (state.hasTrack) 255 else 97
            views.setInt(R.id.studio_next, "setColorFilter", tint)
            views.setInt(R.id.studio_previous, "setColorFilter", tint)
            views.setInt(R.id.studio_next, "setImageAlpha", alpha)
            views.setInt(R.id.studio_previous, "setImageAlpha", alpha)
            if (layout == StudioLayout.Slim) {
                val showPrevious = StudioControl.Previous in config.controls && w >= 300
                views.setViewVisibility(R.id.studio_previous, if (showPrevious) View.VISIBLE else View.GONE)
                views.setViewVisibility(R.id.studio_next, if (w >= 180) View.VISIBLE else View.GONE)
            }
            if (interactive) {
                views.setOnClickPendingIntent(R.id.studio_next, broadcast(WidgetStudioActionReceiver.ACTION_NEXT, 2))
                views.setOnClickPendingIntent(R.id.studio_previous, broadcast(WidgetStudioActionReceiver.ACTION_PREVIOUS, 3))
            }
        }

        private fun availableSlots(): List<Int> {
            val order = layout.slotOrder
            return when (layout) {
                StudioLayout.Slim -> if (w >= 250) order else emptyList()
                StudioLayout.Expressive -> order.take(if (w >= 300) 2 else if (w >= 250) 1 else 0)
                StudioLayout.ImmersiveCompact -> if (w >= 280) order else order.filter { it == 3 || it in ROW_SLOTS }
                StudioLayout.HeroFocus -> heroFocusSlots(order)
                StudioLayout.HeroSplit -> heroSplitSlots()
                StudioLayout.Immersive -> immersiveSlots(order)
                else -> order
            }
        }

        private fun heroFocusSlots(order: List<Int>): List<Int> {
            val immersive = family == StudioWidgetFamily.Immersive
            return when {
                immersive && w >= 280 -> order
                immersive -> order.filter { it == 3 || it in ROW_SLOTS }
                w >= 280 -> order.filter { it !in ROW_SLOTS }
                else -> order.take(1)
            }
        }

        private fun heroSplitSlots(): List<Int> {
            val base = if (w >= 260) listOf(1, 2, 3) else listOf(1)
            return if (family == StudioWidgetFamily.Immersive && h >= 400) base + ROW_SLOTS.sorted() else base
        }

        private fun immersiveSlots(order: List<Int>): List<Int> {
            val compactColumn = w - 28f - immersiveArtDp() - 18f < 300f
            return when {
                compactColumn && h >= 270 -> order.filter { it in ROW_SLOTS }
                compactColumn -> emptyList()
                h >= 270 -> order
                else -> order.filter { it !in ROW_SLOTS }
            }
        }

        private fun assignedSlots(): Map<Int, StudioControl> {
            val controls = family.controls.filter {
                it in config.controls && it != StudioControl.Progress && it != StudioControl.Previous
            }
            return availableSlots().zip(controls).toMap()
        }

        private fun bindSlots() {
            if (layout.slotOrder.isEmpty()) return
            val assigned = assignedSlots()
            val allSlots = layout.slotOrder + layout.balancedPairs.flatMap { listOf(it.first, it.second) }
            for (slot in allSlots.distinct()) {
                val control = assigned[slot]
                if (control != null) {
                    bindSlot(slot, control)
                } else {
                    val keepSpace = partnerOf(slot)?.let(assigned::containsKey) == true
                    views.setViewVisibility(slotRoots[slot], if (keepSpace) View.INVISIBLE else View.GONE)
                }
            }
            if (layout in SLOT_ROW_LAYOUTS) {
                views.setViewVisibility(R.id.studio_slot_row, if (rowVisible()) View.VISIBLE else View.GONE)
            }
        }

        private fun partnerOf(slot: Int): Int? = layout.balancedPairs.firstNotNullOfOrNull { (a, b) ->
            when (slot) {
                a -> b
                b -> a
                else -> null
            }
        }

        private fun bindSlot(slot: Int, control: StudioControl) {
            val spec = slotSpec(control)
            val rowSlot = slot in ROW_SLOTS
            val activeBg = if (minimal) scheme.surfaceContainerHigh else scheme.secondaryContainer
            val activeFg = if (minimal) scheme.onSurface else scheme.onSecondaryContainer
            val (bg, fg) = when {
                spec.active -> activeBg to activeFg
                rowSlot && onArtwork -> WidgetStudioColors.withAlpha(scheme.surfaceContainerHigh, 0.62f) to scheme.onSurface
                rowSlot -> scheme.surfaceContainerHigh to scheme.onSurfaceVariant
                onArtwork -> WidgetStudioColors.withAlpha(scheme.surface, 0.42f) to scheme.onSurface
                else -> 0 to scheme.onSurfaceVariant
            }
            views.setViewVisibility(slotRoots[slot], View.VISIBLE)
            tint(slotBgs[slot], bg)
            views.setImageViewResource(slotIcons[slot], spec.icon)
            views.setInt(slotIcons[slot], "setColorFilter", fg)
            views.setInt(slotIcons[slot], "setImageAlpha", if (spec.enabled) 255 else 97)
            views.setContentDescription(slotRoots[slot], spec.label)
            if (interactive) views.setOnClickPendingIntent(slotRoots[slot], spec.intent)
        }

        private fun bindProgress() {
            if (layout.progress == StudioProgressKind.None) return
            val visible = showsProgress(config, state) && (layout != StudioLayout.Expressive || h >= 120)
            val progressView = if (layout.progress == StudioProgressKind.Bar) R.id.studio_progress else R.id.studio_progress_row
            views.setViewVisibility(progressView, if (visible) View.VISIBLE else View.GONE)
            if (!visible) return
            val now = SystemClock.elapsedRealtime()
            val position = state.positionAt(now)
            val fraction = position.toFloat() / state.durationMs.toFloat()
            val barWidthDp = progressWidthDp()
            val barHeightDp = if (layout == StudioLayout.HeroSplit || layout == StudioLayout.Expressive || layout == StudioLayout.ImmersiveCompact) 12f else 14f
            views.setImageViewBitmap(
                R.id.studio_progress,
                WidgetStudioPainter.progress(
                    px(barWidthDp), px(barHeightDp), fraction,
                    active = scheme.primary,
                    track = WidgetStudioColors.withAlpha(scheme.onSurface, if (onArtwork) 0.28f else 0.18f),
                    wavy = state.playing && !minimal,
                    density = density,
                    rtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
                )
            )
            if (layout.progress == StudioProgressKind.Row) {
                views.setChronometer(R.id.studio_elapsed, now - position, null, state.playing)
                views.setTextColor(R.id.studio_elapsed, scheme.onSurfaceVariant)
                views.setTextViewText(R.id.studio_duration, formatDuration(state.durationMs))
                views.setTextColor(R.id.studio_duration, scheme.onSurfaceVariant)
            }
        }

        private fun progressWidthDp(): Float = when (layout) {
            StudioLayout.Expressive -> w - 24f - expressiveArtDp() - 14f
            StudioLayout.HeroFocus -> (w - 36 - 92).toFloat()
            StudioLayout.ImmersiveCompact -> (w - 32 - 92).toFloat()
            StudioLayout.HeroSplit -> (w - 36 - 92).toFloat()
            StudioLayout.Immersive -> w - 28f - immersiveArtDp() - 18f - 92f
            else -> w.toFloat()
        }.coerceAtLeast(40f)

        fun bindQuick() {
            views.setInt(R.id.studio_bg, "setColorFilter", if (minimal) scheme.surface else scheme.surfaceContainer)
            bindQuickResume()
            val tones = listOf(
                scheme.tertiaryContainer to scheme.onTertiaryContainer,
                scheme.primaryContainer to scheme.onPrimaryContainer,
                scheme.secondaryContainer to scheme.onSecondaryContainer,
                scheme.surfaceContainerHigh to scheme.onSurface,
                scheme.tertiaryContainer to scheme.onTertiaryContainer,
                scheme.primaryContainer to scheme.onPrimaryContainer
            )
            QUICK_TILES.forEachIndexed { index, tile ->
                val (bg, fg) = if (minimal) scheme.surfaceContainerHigh to scheme.onSurface else tones[index]
                views.setInt(tile.bg, "setColorFilter", bg)
                views.setInt(tile.icon, "setColorFilter", fg)
                if (layout == StudioLayout.Quick) views.setTextColor(tile.label, scheme.onSurface)
                if (interactive) views.setOnClickPendingIntent(tile.root, openApp(tile.shortcut, 20 + index))
            }
            if (interactive) {
                val resumeShortcut = if (state.playing) LevyraLaunchActions.SHORTCUT_PLAYER else LevyraLaunchActions.SHORTCUT_RESUME
                views.setOnClickPendingIntent(R.id.studio_quick_resume, openApp(resumeShortcut, if (state.playing) 0 else 30))
                views.setOnClickPendingIntent(R.id.studio_root, rootIntent())
            }
        }

        private fun bindQuickResume() {
            val resumeVisible = layout == StudioLayout.Quick || h >= 150
            views.setViewVisibility(R.id.studio_quick_resume, if (resumeVisible) View.VISIBLE else View.GONE)
            val tones = if (minimal) {
                intArrayOf(scheme.surfaceContainerHigh, scheme.onSurface, scheme.surface, scheme.onSurface)
            } else {
                intArrayOf(scheme.primaryContainer, scheme.primary, scheme.onPrimary, scheme.onPrimaryContainer)
            }
            views.setInt(R.id.studio_quick_resume_bg, "setColorFilter", tones[0])
            views.setInt(R.id.studio_quick_resume_disc, "setColorFilter", tones[1])
            views.setInt(R.id.studio_quick_resume_icon, "setColorFilter", tones[2])
            bindQuickResumeTexts(tones[3])
        }

        private fun bindQuickResumeTexts(foreground: Int) {
            views.setImageViewResource(
                R.id.studio_quick_resume_icon,
                if (state.playing) R.drawable.ic_widget_playing else R.drawable.ic_studio_play
            )
            views.setTextViewText(
                R.id.studio_quick_resume_label,
                context.getString(if (state.playing) R.string.car_now_playing else R.string.shortcut_resume)
            )
            views.setTextColor(R.id.studio_quick_resume_label, foreground)
            views.setTextViewText(
                R.id.studio_title,
                if (state.hasTrack) listOf(state.title, state.artist).filter { it.isNotBlank() }.joinToString(" · ")
                else context.getString(R.string.widget_idle_subtitle)
            )
            views.setTextColor(R.id.studio_title, WidgetStudioColors.withAlpha(foreground, 0.78f))
        }

        private fun likeSpec(): SlotSpec {
            val favorite = state.favorite == StudioFavoriteState.Favorite
            return SlotSpec(
                icon = if (favorite) R.drawable.ic_studio_favorite else R.drawable.ic_studio_favorite_border,
                active = favorite,
                enabled = state.hasTrack && state.favorite != StudioFavoriteState.Unavailable,
                label = context.getString(if (favorite) R.string.notification_favorite_remove else R.string.notification_favorite_add),
                intent = broadcast(WidgetStudioActionReceiver.ACTION_LIKE, 4)
            )
        }

        private fun shuffleSpec(): SlotSpec {
            val enabled = state.shuffle == true
            return SlotSpec(
                icon = R.drawable.ic_studio_shuffle,
                active = enabled,
                enabled = state.shuffle != null,
                label = context.getString(if (enabled) R.string.notification_shuffle_disable else R.string.notification_shuffle_enable),
                intent = broadcast(WidgetStudioActionReceiver.ACTION_SHUFFLE, 5)
            )
        }

        private fun repeatSpec(): SlotSpec = SlotSpec(
            icon = if (state.repeat == StudioRepeatState.One) R.drawable.ic_studio_repeat_one else R.drawable.ic_studio_repeat,
            active = state.repeat == StudioRepeatState.All || state.repeat == StudioRepeatState.One,
            enabled = state.repeat != StudioRepeatState.Unavailable,
            label = context.getString(
                when (state.repeat) {
                    StudioRepeatState.All -> R.string.notification_repeat_all
                    StudioRepeatState.One -> R.string.notification_repeat_one
                    else -> R.string.notification_repeat_off
                }
            ),
            intent = broadcast(WidgetStudioActionReceiver.ACTION_REPEAT, 6)
        )

        private fun slotSpec(control: StudioControl): SlotSpec = when (control) {
            StudioControl.Like -> likeSpec()
            StudioControl.Shuffle -> shuffleSpec()
            StudioControl.Repeat -> repeatSpec()
            StudioControl.Lyrics -> SlotSpec(
                R.drawable.ic_studio_lyrics, false, state.hasTrack, context.getString(R.string.widget_lyrics),
                openApp(LevyraLaunchActions.SHORTCUT_LYRICS, 10)
            )
            StudioControl.Queue -> SlotSpec(
                R.drawable.ic_studio_queue, false, state.hasTrack, context.getString(R.string.car_queue),
                openApp(LevyraLaunchActions.SHORTCUT_QUEUE, 11)
            )
            StudioControl.Flow -> SlotSpec(
                R.drawable.ic_studio_flow, false, true, context.getString(R.string.widget_flow),
                openApp(LevyraLaunchActions.SHORTCUT_FLOW, 12)
            )
            StudioControl.Downloads -> SlotSpec(
                R.drawable.ic_studio_download, false, true, context.getString(R.string.car_downloads),
                openApp(LevyraLaunchActions.SHORTCUT_OFFLINE, 13)
            )
            StudioControl.Previous, StudioControl.Progress -> SlotSpec(
                R.drawable.ic_studio_previous, false, true, "", broadcast(WidgetStudioActionReceiver.ACTION_PREVIOUS, 3)
            )
        }

        private fun broadcast(action: String, code: Int): PendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_BASE + code,
            Intent(context, WidgetStudioActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        private fun openApp(shortcut: String?, code: Int): PendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_BASE + 0x100 + code,
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                if (shortcut != null) putExtra(LevyraLaunchActions.EXTRA_SHORTCUT, shortcut)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private class SlotSpec(
        @DrawableRes val icon: Int,
        val active: Boolean,
        val enabled: Boolean,
        val label: String,
        val intent: PendingIntent
    )

    private class QuickTile(val root: Int, val bg: Int, val icon: Int, val label: Int, val shortcut: String)

    private val QUICK_TILES = listOf(
        QuickTile(R.id.studio_quick_flow, R.id.studio_quick_flow_bg, R.id.studio_quick_flow_icon, R.id.studio_quick_flow_label, LevyraLaunchActions.SHORTCUT_FLOW),
        QuickTile(R.id.studio_quick_favorites, R.id.studio_quick_favorites_bg, R.id.studio_quick_favorites_icon, R.id.studio_quick_favorites_label, LevyraLaunchActions.SHORTCUT_FAVORITES),
        QuickTile(R.id.studio_quick_downloads, R.id.studio_quick_downloads_bg, R.id.studio_quick_downloads_icon, R.id.studio_quick_downloads_label, LevyraLaunchActions.SHORTCUT_OFFLINE),
        QuickTile(R.id.studio_quick_search, R.id.studio_quick_search_bg, R.id.studio_quick_search_icon, R.id.studio_quick_search_label, LevyraLaunchActions.SHORTCUT_SEARCH),
        QuickTile(R.id.studio_quick_recognize, R.id.studio_quick_recognize_bg, R.id.studio_quick_recognize_icon, R.id.studio_quick_recognize_label, LevyraLaunchActions.SHORTCUT_RECOGNITION),
        QuickTile(R.id.studio_quick_lyrics, R.id.studio_quick_lyrics_bg, R.id.studio_quick_lyrics_icon, R.id.studio_quick_lyrics_label, LevyraLaunchActions.SHORTCUT_LYRICS)
    )

    private fun formatDuration(ms: Long): String {
        val totalSeconds = max(0L, ms / 1000L)
        val hours = totalSeconds / 3600L
        val minutes = totalSeconds % 3600L / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0) {
            String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
        }
    }
}
