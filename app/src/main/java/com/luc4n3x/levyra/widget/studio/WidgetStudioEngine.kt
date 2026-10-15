package com.luc4n3x.levyra.widget.studio

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import timber.log.Timber
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

object WidgetStudioEngine {
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "levyra-widget-studio").apply { isDaemon = true }
    }
    private val renderQueued = AtomicBoolean(false)
    private val progressQueued = AtomicBoolean(false)
    private val waiting = ConcurrentLinkedQueue<BroadcastReceiver.PendingResult>()

    fun requestRender(context: Context, pending: BroadcastReceiver.PendingResult? = null) {
        val appContext = context.applicationContext
        pending?.let(waiting::add)
        if (!renderQueued.compareAndSet(false, true)) return
        executor.execute {
            renderQueued.set(false)
            try {
                renderAll(appContext)
            } catch (error: Exception) {
                Timber.w(error, "Studio widget render failed")
            } finally {
                while (true) {
                    val result = waiting.poll() ?: break
                    result.finish()
                }
            }
        }
    }

    fun requestProgress(context: Context) {
        val appContext = context.applicationContext
        if (!progressQueued.compareAndSet(false, true)) return
        executor.execute {
            progressQueued.set(false)
            runCatching { renderProgress(appContext) }
                .onFailure { Timber.w(it, "Studio widget progress update failed") }
        }
    }

    fun hasProgressWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context) ?: return false
        return StudioWidgetFamily.entries.any { family ->
            widgetIds(context, manager, family).any { id ->
                StudioControl.Progress in WidgetStudioStore.config(context, id, family).controls
            }
        }
    }

    fun hasWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context) ?: return false
        return StudioWidgetFamily.entries.any { widgetIds(context, manager, it).isNotEmpty() }
    }

    fun placedWidgets(context: Context): List<Pair<Int, StudioWidgetFamily>> {
        val manager = AppWidgetManager.getInstance(context) ?: return emptyList()
        return StudioWidgetFamily.entries.flatMap { family ->
            widgetIds(context, manager, family).map { it to family }
        }
    }

    fun familyOf(context: Context, widgetId: Int): StudioWidgetFamily? {
        val manager = AppWidgetManager.getInstance(context) ?: return null
        val provider = manager.getAppWidgetInfo(widgetId)?.provider ?: return null
        if (provider.packageName != context.packageName) return null
        return StudioWidgetFamily.entries.firstOrNull { it.providerClass.name == provider.className }
    }

    private fun widgetIds(context: Context, manager: AppWidgetManager, family: StudioWidgetFamily): IntArray =
        manager.getAppWidgetIds(ComponentName(context, family.providerClass)) ?: IntArray(0)

    private fun renderAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val placed = StudioWidgetFamily.entries.map { it to widgetIds(context, manager, it) }
        if (placed.all { it.second.isEmpty() }) return
        val state = WidgetStudioStore.playbackState(context)
        if (state.hasTrack && state.artworkUrl.isNotBlank() && WidgetStudioArtwork.source(state.artworkUrl) == null) {
            WidgetStudioArtwork.request(state.artworkUrl) { requestRender(context) }
        }
        for ((family, ids) in placed) {
            for (id in ids) {
                runCatching {
                    val config = WidgetStudioStore.config(context, id, family)
                    val size = WidgetStudioRenderer.sizeOf(context, manager.getAppWidgetOptions(id), family)
                    manager.updateAppWidget(id, WidgetStudioRenderer.build(context, family, config, state, size, interactive = true))
                }.onFailure { Timber.w(it, "Studio widget %d render failed", id) }
            }
        }
    }

    private fun renderProgress(context: Context) {
        val manager = AppWidgetManager.getInstance(context) ?: return
        val state = WidgetStudioStore.playbackState(context)
        for (family in StudioWidgetFamily.entries) {
            for (id in widgetIds(context, manager, family)) {
                runCatching {
                    val config = WidgetStudioStore.config(context, id, family)
                    val size = WidgetStudioRenderer.sizeOf(context, manager.getAppWidgetOptions(id), family)
                    WidgetStudioRenderer.buildProgress(context, family, config, state, size)
                        ?.let { manager.partiallyUpdateAppWidget(id, it) }
                }.onFailure { Timber.w(it, "Studio widget %d progress failed", id) }
            }
        }
    }
}
