package com.luc4n3x.levyra.feature.dearrow

import kotlinx.coroutines.CancellationException

data class VideoMetadata(
    val videoId: String,
    val title: String,
    val thumbnailUrl: String
)

class VideoMetadataEnhancer(
    private val repository: DeArrowRepository
) {
    suspend fun enhance(video: VideoMetadata): VideoMetadata {
        val branding = try {
            repository.branding(video.videoId)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            null
        } ?: return video

        val title = branding.title?.takeIf { it.isNotBlank() } ?: video.title
        val thumbnailUrl = branding.thumbnailUrl?.takeIf { it.isNotBlank() } ?: video.thumbnailUrl
        if (title == video.title && thumbnailUrl == video.thumbnailUrl) return video
        return video.copy(title = title, thumbnailUrl = thumbnailUrl)
    }
}
