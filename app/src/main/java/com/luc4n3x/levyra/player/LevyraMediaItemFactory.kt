package com.luc4n3x.levyra.player

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import com.luc4n3x.levyra.data.collections.isPersonalCollectionTrack
import com.luc4n3x.levyra.data.locallibrary.mediaSessionArtworkUri
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.radio.isLiveRadio
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object LevyraMediaItemFactory {
    fun metadataOnly(track: Track): MediaItem {
        return MediaItem.Builder()
            .setMediaId(mediaId(track))
            .setMediaMetadata(metadata(track, false))
            .build()
    }

    fun build(track: Track, videoMode: Boolean = false): MediaItem {
        val streamUrl = track.streamUrl
        val liveRadio = track.isLiveRadio()
        val personalCollection = isPersonalCollectionStream(track)
        val cacheReadSpec = if (personalCollection) null else playbackCacheReadSpec(streamUrl)
        val customCacheKey = cacheKey(track, videoMode, liveRadio, personalCollection, cacheReadSpec)
        val streamMimeType = streamMimeType(streamUrl, videoMode, liveRadio, personalCollection, cacheReadSpec)
        val builder = MediaItem.Builder()
            .setUri(streamUrl)
            .setMediaId(mediaId(track))
            .setMediaMetadata(metadata(track, videoMode))
        if (liveRadio) {
            builder.setLiveConfiguration(MediaItem.LiveConfiguration.Builder().build())
        }
        customCacheKey?.let(builder::setCustomCacheKey)
        samplesClipping(track, videoMode)?.let(builder::setClippingConfiguration)
        streamMimeType?.let { builder.setMimeType(it) }
        subtitleConfigurations(track, videoMode).takeIf { it.isNotEmpty() }?.let(builder::setSubtitleConfigurations)
        val mediaItem = builder.build()
        rememberCacheHint(track, videoMode, liveRadio, personalCollection, cacheReadSpec, customCacheKey, streamMimeType)
        return mediaItem
    }

    internal fun samplesClipping(track: Track, videoMode: Boolean): MediaItem.ClippingConfiguration? {
        if (!videoMode || track.album != "YouTube Music Samples" || track.videoType != "SHORTS") return null
        val url = track.videoUrl.toHttpUrlOrNull() ?: return null
        val startMs = url.queryParameter("clipStartMs")?.toLongOrNull() ?: return null
        val endMs = url.queryParameter("clipEndMs")?.toLongOrNull() ?: return null
        if (startMs < 0 || endMs <= startMs || endMs - startMs > 180_000L) return null
        return MediaItem.ClippingConfiguration.Builder().setStartPositionMs(startMs).setEndPositionMs(endMs).build()
    }

    private fun cacheKey(
        track: Track,
        videoMode: Boolean,
        liveRadio: Boolean,
        personalCollection: Boolean,
        cacheReadSpec: PlaybackCacheReadSpec?
    ): String? = when {
        liveRadio -> null
        personalCollection -> LevyraPlaybackCacheKey.personalCollection(track)
        cacheReadSpec != null -> cacheReadSpec.cacheKey
        videoMode && track.videoStreamUrl.isBlank() -> LevyraPlaybackCacheKey.video(track)
        else -> LevyraPlaybackCacheKey.stream(track)
    }

    private fun streamMimeType(
        streamUrl: String,
        videoMode: Boolean,
        liveRadio: Boolean,
        personalCollection: Boolean,
        cacheReadSpec: PlaybackCacheReadSpec?
    ): String? = when {
        liveRadio -> liveRadioMimeTypeFor(streamUrl)
        personalCollection -> personalCollectionMimeTypeFor(streamUrl)
        cacheReadSpec != null -> cacheReadSpec.mimeType.takeIf(String::isNotBlank)
        else -> mimeTypeFor(streamUrl, videoMode)
    }

    private fun subtitleConfigurations(track: Track, videoMode: Boolean): List<MediaItem.SubtitleConfiguration> {
        if (!videoMode) return emptyList()
        return track.videoSubtitleTracks.map { subtitle ->
            MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitle.vttUrl))
                .setId(subtitle.id)
                .setMimeType(MimeTypes.TEXT_VTT)
                .setLanguage(subtitle.languageCode)
                .setLabel(subtitle.label)
                .build()
        }
    }

    private fun rememberCacheHint(
        track: Track,
        videoMode: Boolean,
        liveRadio: Boolean,
        personalCollection: Boolean,
        cacheReadSpec: PlaybackCacheReadSpec?,
        cacheKey: String?,
        mimeType: String?
    ) {
        if (cacheReadSpec != null || liveRadio || personalCollection) return
        if (!shouldRememberPlaybackCacheHint(track.streamUrl, mimeType, videoMode)) return
        cacheKey?.let { PlaybackCacheHintStore.record(track, it, mimeType.orEmpty()) }
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

    internal fun isPersonalCollectionStream(track: Track): Boolean {
        if (!track.isPersonalCollectionTrack()) return false
        val scheme = track.streamUrl.substringBefore(':').lowercase()
        return scheme == "https" || scheme == "http"
    }

    internal fun personalCollectionMimeTypeFor(url: String): String? {
        val path = url.substringBefore('#').substringBefore('?').lowercase()
        return when (path.substringAfterLast('/').substringAfterLast('.', "")) {
            "m3u8" -> MimeTypes.APPLICATION_M3U8
            "mpd" -> MimeTypes.APPLICATION_MPD
            "mp3" -> MimeTypes.AUDIO_MPEG
            "m4a", "m4b", "mp4" -> MimeTypes.AUDIO_MP4
            "flac" -> MimeTypes.AUDIO_FLAC
            "ogg", "oga", "opus" -> MimeTypes.AUDIO_OGG
            "wav" -> MimeTypes.AUDIO_WAV
            "webm", "weba" -> MimeTypes.AUDIO_WEBM
            else -> null
        }
    }

    internal fun liveRadioMimeTypeFor(url: String): String? = mimeTypeFor(url, false)
        ?.takeIf { it == MimeTypes.APPLICATION_M3U8 || it == MimeTypes.APPLICATION_MPD }

    private fun metadata(track: Track, videoMode: Boolean): MediaMetadata {
        val art = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }
        val extras = Bundle().apply {
            putString("levyra.title", track.title)
            putString("levyra.artist", track.artist)
            putString("levyra.album", track.album)
            putLong("levyra.durationMs", track.durationMs.coerceAtLeast(0L))
            putString("levyra.source", track.source)
            putBoolean(PlaybackService.EXTRA_VIDEO_MODE, videoMode)
            putBoolean(PlaybackService.EXTRA_LIVE_RADIO, track.isLiveRadio())
            putBoolean(PlaybackService.EXTRA_PERSONAL_COLLECTION, isPersonalCollectionStream(track))
            track.youtubeLoudnessDb?.let { putFloat(PlaybackService.EXTRA_YOUTUBE_LOUDNESS_DB, it) }
            track.youtubePerceptualLoudnessDb?.let { putFloat(PlaybackService.EXTRA_YOUTUBE_PERCEPTUAL_LOUDNESS_DB, it) }
            track.replayGainTrackDb?.let { putFloat(PlaybackService.EXTRA_REPLAY_GAIN_TRACK_DB, it) }
            track.replayGainAlbumDb?.let { putFloat(PlaybackService.EXTRA_REPLAY_GAIN_ALBUM_DB, it) }
            track.replayGainTrackPeak?.let { putFloat(PlaybackService.EXTRA_REPLAY_GAIN_TRACK_PEAK, it) }
            track.replayGainAlbumPeak?.let { putFloat(PlaybackService.EXTRA_REPLAY_GAIN_ALBUM_PEAK, it) }
            if (videoMode && track.videoStreamUrl.isNotBlank()) {
                putString(PlaybackService.EXTRA_VIDEO_URL, track.videoStreamUrl)
                putString(PlaybackService.EXTRA_VIDEO_CACHE_KEY, LevyraPlaybackCacheKey.video(track))
                mimeTypeFor(track.videoStreamUrl, true)?.let { putString(PlaybackService.EXTRA_VIDEO_MIME_TYPE, it) }
            }
        }
        val builder = MediaMetadata.Builder()
            .setTitle(track.title)
            .setDisplayTitle(track.title)
            .setArtist(track.artist)
            .setSubtitle(track.artist)
            .setAlbumTitle(track.album.ifBlank { "Levyra" })
            .apply { mediaSessionArtworkUri(art)?.let(::setArtworkUri) }
            .setExtras(extras)
        if (track.isLiveRadio()) {
            builder.setIsPlayable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
        }
        return builder.build()
    }

    fun mediaId(track: Track): String {
        return track.id.ifBlank { track.videoUrl.ifBlank { "${track.artist}-${track.title}" } }
    }
}
