package com.luc4n3x.levyra.data.youtubemusic

import kotlinx.coroutines.CancellationException

internal enum class YoutubeMusicImportOutcome { Imported, Empty, UpToDate, PlaylistOnly }

internal suspend fun <T> importYoutubeMusicItems(
    items: List<T>,
    importPlaylist: suspend (List<T>) -> Int,
    importFavorites: (suspend (List<T>) -> Unit)? = null
): YoutubeMusicImportOutcome {
    if (items.isEmpty()) return YoutubeMusicImportOutcome.Empty
    val added = importPlaylist(items)
    if (importFavorites == null) return if (added > 0) YoutubeMusicImportOutcome.Imported else YoutubeMusicImportOutcome.UpToDate
    return try {
        importFavorites(items)
        YoutubeMusicImportOutcome.Imported
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        YoutubeMusicImportOutcome.PlaylistOnly
    }
}
