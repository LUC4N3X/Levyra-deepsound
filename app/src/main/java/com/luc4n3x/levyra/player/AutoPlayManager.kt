package com.luc4n3x.levyra.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.data.RecommendationFeedbackStore
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.data.rankRecommendationCandidates
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class AutoPlayManager(
    private val context: Context,
    private val player: ExoPlayer,
    private val scope: CoroutineScope
) {
    private val repository = YoutubeMusicRepository(context.applicationContext)
    private val preferences = LevyraPreferences(context.applicationContext)
    private val feedbackStore = RecommendationFeedbackStore(context.applicationContext)
    private var fetchJob: Job? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                checkQueue()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    checkQueue()
                }
            }
        })
    }

    private fun checkQueue() {
        val remaining = player.mediaItemCount - player.currentMediaItemIndex
        if (remaining > 2 || fetchJob?.isActive == true) return
        val currentItem = player.currentMediaItem ?: return
        val existingIds = buildSet {
            for (index in 0 until player.mediaItemCount) {
                player.getMediaItemAt(index).mediaId.trim()
                    .takeIf { it.isNotBlank() }
                    ?.let(::add)
            }
        }

        fetchJob = scope.launch(Dispatchers.IO) {
            try {
                val title = currentItem.mediaMetadata.title?.toString().orEmpty()
                val artist = currentItem.mediaMetadata.artist?.toString().orEmpty()
                val query = if (artist.isNotBlank()) "$artist $title" else title
                if (query.isBlank()) return@launch

                val results = repository.search(
                    query = query,
                    limit = 24,
                    languageCode = preferences.languageCode()
                )
                if (results.isEmpty()) return@launch

                val feedback = feedbackStore.snapshot()
                val newTracks = rankRecommendationCandidates(
                    candidates = results,
                    feedback = feedback,
                    excludedTrackIds = existingIds
                ).take(5)

                if (newTracks.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        val mediaItems = newTracks.map { track ->
                            MediaItem.Builder()
                                .setMediaId(track.id)
                                .setUri(track.videoUrl)
                                .setMediaMetadata(
                                    androidx.media3.common.MediaMetadata.Builder()
                                        .setTitle(track.title)
                                        .setArtist(track.artist)
                                        .setArtworkUri(android.net.Uri.parse(track.largeThumbnailUrl))
                                        .build()
                                )
                                .setRequestMetadata(
                                    MediaItem.RequestMetadata.Builder()
                                        .setMediaUri(android.net.Uri.parse(track.videoUrl))
                                        .build()
                                )
                                .build()
                        }
                        player.addMediaItems(mediaItems)
                        Timber.d(
                            "AutoPlayManager added ${mediaItems.size} preference-ranked tracks to the queue"
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "AutoPlayManager failed to fetch related tracks")
            }
        }
    }
}
