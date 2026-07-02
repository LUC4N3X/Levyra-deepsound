package com.luc4n3x.levyra.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

class AutoPlayManager(
    private val context: Context,
    private val player: ExoPlayer,
    private val scope: CoroutineScope
) {
    private val repository = YoutubeMusicRepository
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
        // If we are at the last or second to last item, fetch more
        val remaining = player.mediaItemCount - player.currentMediaItemIndex
        if (remaining <= 2 && fetchJob?.isActive != true) {
            val currentItem = player.currentMediaItem ?: return
            fetchJob = scope.launch(Dispatchers.IO) {
                try {
                    // Try to fetch related tracks using the current item's title/artist
                    val title = currentItem.mediaMetadata.title?.toString() ?: ""
                    val artist = currentItem.mediaMetadata.artist?.toString() ?: ""
                    
                    val query = if (artist.isNotBlank()) "$artist $title" else title
                    if (query.isBlank()) return@launch
                    
                    // Simple logic: search for related tracks based on the query
                    // In a real scenario you would use the "Next" endpoint of YouTube Music
                    val results = repository.search(query, limit = 10)
                    
                    // Filter out what's already in the queue to avoid duplicates
                    val existingIds = (0 until player.mediaItemCount).mapNotNull {
                        player.getMediaItemAt(it).mediaId
                    }
                    
                    val newTracks = results.filter { it.id !in existingIds }.shuffled().take(5)
                    
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
                                    // Set extras so LevyraMediaSourceFactory can resolve the actual stream
                                    .setRequestMetadata(
                                        MediaItem.RequestMetadata.Builder()
                                            .setMediaUri(android.net.Uri.parse(track.videoUrl))
                                            .build()
                                    )
                                    .build()
                            }
                            player.addMediaItems(mediaItems)
                            Timber.d("AutoPlayManager added ${mediaItems.size} tracks to the queue")
                        }
                    }
                } catch (e: Exception) {
                    Timber.w(e, "AutoPlayManager failed to fetch related tracks")
                }
            }
        }
    }
}
