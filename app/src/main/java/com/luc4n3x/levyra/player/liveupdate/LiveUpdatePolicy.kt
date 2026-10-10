package com.luc4n3x.levyra.player.liveupdate

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

internal object LiveUpdatePolicy {
    const val MIN_SDK: Int = Build.VERSION_CODES.BAKLAVA

    @ChecksSdkIntAtLeast(api = MIN_SDK)
    fun isSupported(): Boolean = isSupported(Build.VERSION.SDK_INT)

    fun isSupported(sdkInt: Int): Boolean = sdkInt >= MIN_SDK
}

internal data class DownloadBatchProgress(
    val title: String,
    val completed: Int,
    val total: Int
)

internal data class DownloadLiveUpdateContent(
    val shortCriticalText: String,
    val subText: String?,
    val requestPromotion: Boolean
)

internal object DownloadLiveUpdateMapper {
    fun map(
        progress: Int,
        batch: DownloadBatchProgress?,
        promotionAllowed: Boolean
    ): DownloadLiveUpdateContent {
        val percent = progress.coerceIn(0, 100)
        val subText = batch
            ?.takeIf { it.total > 1 }
            ?.let { current ->
                val counter = "${current.completed.coerceIn(0, current.total)} / ${current.total}"
                val title = current.title.trim()
                if (title.isEmpty()) counter else "$title · $counter"
            }
        return DownloadLiveUpdateContent(
            shortCriticalText = "$percent%",
            subText = subText,
            requestPromotion = promotionAllowed && percent < 100
        )
    }
}

internal object DownloadLiveUpdateSlot {
    private var owner: String? = null

    @Synchronized
    fun claim(taskKey: String): Boolean {
        val current = owner
        if (current == null || current == taskKey) {
            owner = taskKey
            return true
        }
        return false
    }

    @Synchronized
    fun release(taskKey: String) {
        if (owner == taskKey) owner = null
    }
}
