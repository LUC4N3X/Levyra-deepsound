package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.LevyraContentLocales
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.artistIdentityKey
import com.luc4n3x.levyra.domain.artistIdentityMatches
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

internal fun musicPreviewArtists(preferredArtists: List<String>, languageCode: String): List<String> {
    val local = LevyraContentLocales.artistSuggestions(languageCode)
    val global = LevyraContentLocales.artistSuggestions("en")
    return buildList {
        repeat(maxOf(preferredArtists.size, local.size, global.size)) { index ->
            preferredArtists.getOrNull(index)?.let(::add)
            global.getOrNull(index)?.let(::add)
            local.getOrNull(index)?.let(::add)
        }
    }.filter(String::isNotBlank).distinctBy(::artistIdentityKey).take(24)
}

internal suspend fun discoverMusicPreviews(
    repository: YoutubeMusicRepository,
    preferredArtists: List<String>,
    languageCode: String
): List<Track> = coroutineScope {
    val gate = Semaphore(4)
    musicPreviewArtists(preferredArtists, languageCode).map { artist ->
        async {
            gate.withPermit {
                try {
                    withTimeoutOrNull(12_000L) {
                        repository.searchMusicVideos(artist, limit = 4, languageCode = languageCode)
                            .firstOrNull { track ->
                                track.durationMs in 45_000L..600_000L &&
                                    !track.videoType.contains("ATV") &&
                                    artistIdentityMatches(track.artist.removeSuffix("VEVO").removeSuffix(" - Topic"), artist)
                            }?.let(::musicVideoPreview)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    Timber.w(error, "Music preview discovery failed")
                    null
                }
            }
        }
    }.awaitAll().filterNotNull()
}

internal fun musicVideoPreview(track: Track): Track {
    val startMs = minOf(45_000L, track.durationMs / 4)
    val endMs = minOf(track.durationMs, startMs + 30_000L)
    return track.copy(
        album = "YouTube Music previews",
        durationMs = endMs - startMs,
        streamUrl = "",
        videoStreamUrl = "",
        videoUrl = "https://www.youtube.com/shorts/${track.id}?clipStartMs=$startMs&clipEndMs=$endMs",
        source = YOUTUBE_SHORTS_SOURCE,
        videoType = "SHORTS",
        counterpartVideoId = track.id
    )
}
