package com.luc4n3x.levyra.ui.components

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object LevyraRecentColors {
    private const val PREFERENCES = "levyra_color_picker"
    private const val KEY = "recent_colors"

    suspend fun load(context: Context): List<Int> = withContext(Dispatchers.IO) {
        decode(context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getString(KEY, "").orEmpty())
    }

    fun remember(context: Context, current: List<Int>, color: Int): List<Int> {
        val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val stored = decode(preferences.getString(KEY, "").orEmpty())
        val next = rememberRecentColor(stored.ifEmpty { current }, color)
        preferences.edit { putString(KEY, encode(next)) }
        return next
    }

    internal fun encode(colors: List<Int>): String = colors.joinToString(",") { formatLevyraHexColor(it) }

    internal fun decode(raw: String): List<Int> =
        raw.split(',').mapNotNull { parseLevyraHexColor(it) }.distinct().take(RecentColorLimit)
}
