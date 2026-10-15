package com.luc4n3x.levyra.widget.studio

import android.content.Context
import android.os.SystemClock
import com.luc4n3x.levyra.data.LevyraHomeSnapshotCache
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.domain.Track
import java.util.Locale

internal object WidgetStudioDemo {
    private const val FALLBACK_DURATION_MS = 200_000L

    fun states(context: Context, count: Int): List<StudioPlaybackState> {
        val snapshot = LevyraHomeSnapshotCache(context).load(LevyraPreferences(context).languageCode()) ?: return emptyList()
        val candidates = snapshot.charts + snapshot.homeSections.flatMap { it.tracks } + snapshot.personalOrbit
        return candidates
            .filter { it.title.isNotBlank() && artworkOf(it).isNotBlank() }
            .distinctBy { it.artist.lowercase(Locale.ROOT).substringBefore(',').trim() }
            .take(count)
            .map(::stateOf)
    }

    private fun artworkOf(track: Track): String = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }

    private fun stateOf(track: Track): StudioPlaybackState {
        val duration = track.durationMs.takeIf { it > 0L } ?: FALLBACK_DURATION_MS
        return StudioPlaybackState(
            title = track.title,
            artist = track.artist,
            album = track.album,
            artworkUrl = artworkOf(track),
            playing = true,
            hasTrack = true,
            accentSeed = 0,
            favorite = StudioFavoriteState.NotFavorite,
            shuffle = false,
            repeat = StudioRepeatState.Off,
            positionMs = duration * 2 / 5,
            durationMs = duration,
            snapshotRealtimeMs = SystemClock.elapsedRealtime()
        )
    }
}
