package com.luc4n3x.levyra.player

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.luc4n3x.levyra.domain.Track

object LevyraMediaItemFactory {
    fun metadataOnly(track: Track): MediaItem {
        return MediaItem.Builder()
            .setMediaId(mediaId(track))
            .setMediaMetadata(metadata(track, false))
            .build()
    }

    fun build(track: Track, videoMode: Boolean = false): MediaItem {
        val streamUrl = track.streamUrl
        val builder = MediaItem.Builder()
            .setUri(streamUrl)
            .setCustomCacheKey(
                if (videoMode && track.videoStreamUrl.isBlank()) {
                    LevyraPlaybackCacheKey.video(track)
                } else {
                    LevyraPlaybackCacheKey.stream(track)
                }
            )
            .setMediaId(mediaId(track))
            .setMediaMetadata(metadata(track, videoMode))
        mimeTypeFor(streamUrl, videoMode)?.let { builder.setMimeType(it) }
        return builder.build()
    }

    internal fun mimeTypeFor(url: String, videoMode: Boolean): String? {
        val clean = url.substringBefore('#').lowercase()
        val path = clean.substringBefore('?')
        val isContentUri = clean.startsWith("content://")
        val isExtensionlessFileUri = clean.startsWith("file://") && !path.substringAfterLast('/').contains('.')
        return when {
            isContentUri || isExtensionlessFileUri -> null
            path.endsWith(".m3u8") || path.contains("/hls_playlist") || path.contains("/manifest/hls") || clean.contains("mime=application%2fx-mpegurl") || clean.contains("mime=application/vnd.apple.mpegurl") || clean.contains("type=application%2fx-mpegurl") -> "application/x-mpegURL"
            path.endsWith(".mpd") || clean.contains("mime=application%2fdash+xml") || clean.contains("mime=application/dash+xml") -> "application/dash+xml"
            clean.contains("mime=video%2fwebm") || clean.contains("mime=video/webm") -> "video/webm"
            clean.contains("mime=video%2fmp4") || clean.contains("mime=video/mp4") -> "video/mp4"
            clean.contains("mime=audio%2fwebm") || clean.contains("mime=audio/webm") -> "audio/webm"
            clean.contains("mime=audio%2fmpeg") || clean.contains("mime=audio/mpeg") -> "audio/mpeg"
            clean.contains("mime=audio%2fmp4") || clean.contains("mime=audio/mp4") -> "audio/mp4"
            path.endsWith(".webm") -> if (videoMode) "video/webm" else "audio/webm"
            path.endsWith(".mp3") -> "audio/mpeg"
            path.endsWith(".m4a") -> "audio/mp4"
            path.endsWith(".mp4") -> if (videoMode) "video/mp4" else "audio/mp4"
            videoMode -> "video/mp4"
            else -> "audio/mp4"
        }
    }

    private fun metadata(track: Track, videoMode: Boolean): MediaMetadata {
        val art = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }
        val extras = Bundle().apply {
            putString("levyra.title", track.title)
            putString("levyra.artist", track.artist)
            putString("levyra.album", track.album)
            putLong("levyra.durationMs", track.durationMs.coerceAtLeast(0L))
            putString("levyra.source", track.source)
            putBoolean(PlaybackService.EXTRA_VIDEO_MODE, videoMode)
            track.youtubeLoudnessDb?.let { putFloat(PlaybackService.EXTRA_YOUTUBE_LOUDNESS_DB, it) }
            track.youtubePerceptualLoudnessDb?.let { putFloat(PlaybackService.EXTRA_YOUTUBE_PERCEPTUAL_LOUDNESS_DB, it) }
            if (videoMode && track.videoStreamUrl.isNotBlank()) {
                putString(PlaybackService.EXTRA_VIDEO_URL, track.videoStreamUrl)
                putString(PlaybackService.EXTRA_VIDEO_CACHE_KEY, LevyraPlaybackCacheKey.video(track))
                mimeTypeFor(track.videoStreamUrl, true)?.let { putString(PlaybackService.EXTRA_VIDEO_MIME_TYPE, it) }
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

    private fun mediaId(track: Track): String {
        return track.id.ifBlank { track.videoUrl.ifBlank { "${track.artist}-${track.title}" } }
    }
}
