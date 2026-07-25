package com.luc4n3x.levyra.desktop.app.util

import java.util.Locale

object Format {

    fun duration(milliseconds: Long): String {
        if (milliseconds <= 0L) return "--:--"
        val totalSeconds = milliseconds / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
        }
    }

    fun count(value: Long): String = when {
        value <= 0L -> ""
        value < 1_000L -> value.toString()
        value < 1_000_000L -> String.format(Locale.ROOT, "%.1fK", value / 1_000.0)
        else -> String.format(Locale.ROOT, "%.1fM", value / 1_000_000.0)
    }
}
