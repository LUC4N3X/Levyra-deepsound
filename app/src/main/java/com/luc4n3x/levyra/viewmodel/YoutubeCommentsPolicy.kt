package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.data.YoutubeCommentsResult
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.ResonanceCommentSnippet
import com.luc4n3x.levyra.domain.YoutubeComment
import com.luc4n3x.levyra.domain.YoutubeCommentsState
import com.luc4n3x.levyra.domain.resonanceCommentsForTracks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal const val MAX_LIVE_CHAT_ITEMS = 500
internal const val LIVE_CHAT_POLL_INTERVAL_MS = 2_000L
internal const val MAX_LIVE_CHAT_POLL_INTERVAL_MS = 60_000L
internal const val MAX_YOUTUBE_COMMENT_CONTINUATIONS = 512

internal fun youtubeLiveChatPollIntervalMs(serverDelayMs: Long): Long =
    serverDelayMs.coerceIn(LIVE_CHAT_POLL_INTERVAL_MS, MAX_LIVE_CHAT_POLL_INTERVAL_MS)

internal fun nextYoutubeCommentsToken(
    requestedToken: String,
    candidateToken: String,
    successfulTokens: Set<String>
): String {
    val requested = requestedToken.trim()
    val candidate = candidateToken.trim()
    if (candidate.isBlank() || candidate == requested || candidate in successfulTokens) return ""
    return candidate
}

internal fun isYoutubeCommentsRequestCurrent(
    videoId: String,
    generation: Long,
    currentGeneration: Long,
    state: LevyraUiState
): Boolean = generation == currentGeneration && state.youtubeEngagement.videoId == videoId

internal fun LevyraUiState.withYoutubeCommentsResultIfCurrent(
    videoId: String,
    generation: Long,
    currentGeneration: Long,
    result: YoutubeCommentsResult
): LevyraUiState {
    if (!isYoutubeCommentsRequestCurrent(videoId, generation, currentGeneration, this)) return this
    val visible = youtubeEngagement.comments.visible
    val comments = when (result) {
        is YoutubeCommentsResult.Available -> YoutubeCommentsState(
            videoId = videoId,
            visible = visible,
            loaded = true,
            loading = false,
            loadingMore = false,
            disabled = result.page.commentsDisabled,
            countText = result.page.countText,
            items = result.page.items.distinctBy(YoutubeComment::id),
            nextToken = result.page.nextToken,
            nextPageUrl = result.page.nextPageUrl,
            nextPollDelayMs = result.page.nextPollDelayMs,
            liveChat = result.page.liveChat,
            error = null
        )
        YoutubeCommentsResult.Disabled -> YoutubeCommentsState(
            videoId = videoId,
            visible = visible,
            loaded = true,
            loading = false,
            disabled = true
        )
        is YoutubeCommentsResult.Failed -> youtubeEngagement.comments.copy(
            videoId = videoId,
            loading = false,
            loaded = false,
            error = "unavailable"
        )
    }
    return copy(youtubeEngagement = youtubeEngagement.copy(comments = comments))
}

internal fun CoroutineScope.replaceHomeResonanceCommentsJob(
    currentJob: Job?,
    block: suspend CoroutineScope.() -> Unit
): Job {
    currentJob?.cancel()
    return launch(Dispatchers.IO, block = block)
}

internal fun homeResonanceCommentVideoIds(tracks: List<Track>): List<String> = tracks.asSequence()
    .map(Track::id)
    .filter { it.length == 11 }
    .distinct()
    .toList()

internal fun homeResonanceCommentsToRefresh(
    tracks: List<Track>,
    comments: Map<String, ResonanceCommentSnippet>,
    nowMs: Long,
    ttlMs: Long
): List<Track> = tracks.asSequence()
    .filter { it.id.length == 11 }
    .distinctBy(Track::id)
    .filter { track ->
        val updatedAtMs = comments[track.id]?.updatedAtMs ?: 0L
        val ageMs = nowMs - updatedAtMs
        updatedAtMs <= 0L || ageMs < 0L || ageMs >= ttlMs
    }
    .toList()

internal fun LevyraUiState.withHomeResonanceComment(
    requestVideoIds: List<String>,
    videoId: String,
    transform: (ResonanceCommentSnippet?) -> ResonanceCommentSnippet
): LevyraUiState {
    if (requestVideoIds != homeResonanceCommentVideoIds(homeResonanceTracks) || videoId !in requestVideoIds) {
        return this
    }
    val comments = LinkedHashMap(resonanceCommentsForTracks(homeResonanceTracks, homeResonanceComments))
    comments[videoId] = transform(comments[videoId])
    return copy(homeResonanceComments = resonanceCommentsForTracks(homeResonanceTracks, comments))
}
