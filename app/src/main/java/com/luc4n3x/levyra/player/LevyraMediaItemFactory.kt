package com.luc4n3x.levyra.player

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.luc4n3x.levyra.domain.Track

object LevyraMediaItemFactory {
    fun metadataOnly(track: Track): MediaItem {
        return MediaItem.Builder()
            .setMediaId(track.id.ifBlank { track.videoUrl.ifBlank { "${track.artist}-${track.title}" } })
            .setMediaMetadata(metadata(track))
            .build()
    }

    fun build(track: Track): MediaItem {
        return MediaItem.Builder()
            .setUri(track.streamUrl)
            .setMimeType(mimeTypeFor(track.streamUrl))
            .setCustomCacheKey(LevyraPlaybackCacheKey.stream(track))
            .setMediaId(track.id.ifBlank { track.videoUrl.ifBlank { "${track.artist}-${track.title}" } })
            .setMediaMetadata(metadata(track))
            .build()
    }

    private fun metadata(track: Track): MediaMetadata {
        val art = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }
        val extras = Bundle().apply {
            putString("levyra.title", track.title)
            putString("levyra.artist", track.artist)
            putString("levyra.album", track.album)
            putLong("levyra.durationMs", track.durationMs.coerceAtLeast(0L))
            putString("levyra.source", track.source)
            if (track.videoStreamUrl.isNotBlank()) {
                putString(PlaybackService.EXTRA_VIDEO_URL, track.videoStreamUrl)
                putString(PlaybackService.EXTRA_VIDEO_CACHE_KEY, LevyraPlaybackCacheKey.video(track))
            }
        }
        return MediaMetadata.Builder()
            .setTitle(track.title)
            .setDisplayTitle(track.title)
            .setArtist(track.artist)
            .setSubtitle(track.artist)
            .setAlbumTitle(track.album.ifBlank { "Levyra" })
            .apply { if (art.isNotBlank()) setArtworkUri(Uri.parse(art)) }
            .setExtras(extras)
            .build()
    }

    private fun mimeTypeFor(url: String): String {
        val clean = url.substringBefore('#').lowercase()
        val path = clean.substringBefore('?')
        return when {
            path.endsWith(".m3u8") || path.contains("/hls_playlist") || path.contains("/manifest/hls") || clean.contains("mime=application%2fx-mpegurl") -> "application/x-mpegURL"
            path.endsWith(".mpd") -> "application/dash+xml"
            path.endsWith(".webm") || clean.contains("mime=audio%2fwebm") || clean.contains("mime=audio/webm") -> "audio/webm"
            path.endsWith(".mp3") || clean.contains("mime=audio%2fmpeg") || clean.contains("mime=audio/mpeg") -> "audio/mpeg"
            path.endsWith(".m4a") || path.endsWith(".mp4") || clean.contains("mime=audio%2fmp4") || clean.contains("mime=audio/mp4") -> "audio/mp4"
            else -> "audio/mp4"
        }
    }
}
