package com.luc4n3x.levyra.viewmodel

import android.os.SystemClock
import com.luc4n3x.levyra.data.YoutubeCommentsRepository
import com.luc4n3x.levyra.data.YoutubeCommentsResult
import com.luc4n3x.levyra.domain.YoutubeComment
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class YoutubeCommentsController(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<LevyraUiState>,
    private val engagementGeneration: AtomicLong,
    private val repository: YoutubeCommentsRepository
) {
    private var commentsJob: Job? = null
    private var pageJob: Job? = null
    private val replyJobs = ConcurrentHashMap<String, Job>()
    private val continuationHistory = linkedSetOf<String>()
    private var lastLiveChatPageAtMs = 0L

    fun resetForNewVideo() {
        continuationHistory.clear()
        lastLiveChatPageAtMs = 0L
    }

    fun cancelRequests() {
        commentsJob?.cancel()
        pageJob?.cancel()
        replyJobs.values.forEach { it.cancel() }
        replyJobs.clear()
        continuationHistory.clear()
    }

    private fun isRequestCurrent(videoId: String, generation: Long): Boolean {
        return isYoutubeCommentsRequestCurrent(
            videoId = videoId,
            generation = generation,
            currentGeneration = engagementGeneration.get(),
            state = uiState.value
        )
    }

    fun refreshSummary(videoId: String, generation: Long, force: Boolean = false) {
        val comments = uiState.value.youtubeEngagement.comments
        if (
            comments.videoId == videoId &&
            !force &&
            (comments.loading || comments.loaded || comments.disabled)
        ) return

        commentsJob?.cancel()
        uiState.update { state ->
            if (state.youtubeEngagement.videoId != videoId) state else state.copy(
                youtubeEngagement = state.youtubeEngagement.copy(
                    comments = state.youtubeEngagement.comments.copy(
                        videoId = videoId,
                        loading = true,
                        error = null
                    )
                )
            )
        }
        commentsJob = scope.launch {
            val result = repository.initial(videoId, forceRefresh = force)
            if (!isActive || !isRequestCurrent(videoId, generation)) return@launch
            if (result !is YoutubeCommentsResult.Failed) continuationHistory.clear()
            if (result is YoutubeCommentsResult.Available && result.page.liveChat) {
                lastLiveChatPageAtMs = SystemClock.elapsedRealtime()
            }
            uiState.update { state ->
                state.withYoutubeCommentsResultIfCurrent(
                    videoId = videoId,
                    generation = generation,
                    currentGeneration = engagementGeneration.get(),
                    result = result
                )
            }
        }
    }

    fun open(videoId: String, generation: Long) {
        uiState.update { state ->
            state.copy(
                youtubeEngagement = state.youtubeEngagement.copy(
                    videoId = videoId,
                    comments = state.youtubeEngagement.comments.copy(
                        videoId = videoId,
                        visible = true
                    )
                )
            )
        }
        val comments = uiState.value.youtubeEngagement.comments
        if (!comments.loaded && !comments.loading && !comments.disabled) {
            refreshSummary(videoId, generation, force = true)
        }
    }

    fun close() {
        uiState.update { state ->
            state.copy(
                youtubeEngagement = state.youtubeEngagement.copy(
                    comments = state.youtubeEngagement.comments.copy(visible = false)
                )
            )
        }
    }

    private fun rememberContinuation(token: String) {
        continuationHistory += token
        while (continuationHistory.size > MAX_YOUTUBE_COMMENT_CONTINUATIONS) {
            val oldest = continuationHistory.firstOrNull() ?: break
            continuationHistory.remove(oldest)
        }
    }

    fun loadMore(forceRefresh: Boolean) {
        val engagement = uiState.value.youtubeEngagement
        val comments = engagement.comments
        val videoId = engagement.videoId
        val pageToken = comments.nextToken
        if (
            videoId.isBlank() ||
            pageToken.isBlank() ||
            comments.loadingMore ||
            (!forceRefresh && comments.error != null)
        ) return
        val generation = engagementGeneration.get()
        pageJob?.cancel()
        uiState.update { state ->
            if (state.youtubeEngagement.videoId != videoId) state else state.copy(
                youtubeEngagement = state.youtubeEngagement.copy(
                    comments = state.youtubeEngagement.comments.copy(loadingMore = true, error = null)
                )
            )
        }
        pageJob = scope.launch {
            if (comments.liveChat) {
                val pollIntervalMs = youtubeLiveChatPollIntervalMs(comments.nextPollDelayMs)
                val elapsed = SystemClock.elapsedRealtime() - lastLiveChatPageAtMs
                if (elapsed < pollIntervalMs) {
                    delay(pollIntervalMs - elapsed)
                }
            }
            val result = repository.more(
                videoId = videoId,
                continuationToken = pageToken,
                pageUrl = comments.nextPageUrl,
                forceRefresh = forceRefresh
            )
            if (!isActive || !isRequestCurrent(videoId, generation)) return@launch
            if (comments.liveChat) {
                lastLiveChatPageAtMs = SystemClock.elapsedRealtime()
            }
            uiState.update { state ->
                if (!isYoutubeCommentsRequestCurrent(
                        videoId,
                        generation,
                        engagementGeneration.get(),
                        state
                    )
                ) return@update state
                val currentComments = state.youtubeEngagement.comments
                val updated = when (result) {
                    is YoutubeCommentsResult.Available -> {
                        rememberContinuation(pageToken)
                        val combined = (currentComments.items + result.page.items)
                            .distinctBy(YoutubeComment::id)
                        val merged = if (result.page.liveChat) {
                            combined.takeLast(MAX_LIVE_CHAT_ITEMS)
                        } else {
                            combined
                        }
                        val nextToken = nextYoutubeCommentsToken(
                            requestedToken = pageToken,
                            candidateToken = result.page.nextToken,
                            successfulTokens = continuationHistory
                        )
                        currentComments.copy(
                            loaded = true,
                            loadingMore = false,
                            items = merged,
                            nextToken = nextToken,
                            nextPageUrl = result.page.nextPageUrl,
                            nextPollDelayMs = result.page.nextPollDelayMs,
                            liveChat = result.page.liveChat,
                            error = null
                        )
                    }
                    YoutubeCommentsResult.Disabled -> currentComments.copy(
                        loadingMore = false,
                        disabled = true,
                        nextToken = ""
                    )
                    is YoutubeCommentsResult.Failed -> currentComments.copy(
                        loadingMore = false,
                        error = "unavailable"
                    )
                }
                state.copy(
                    youtubeEngagement = state.youtubeEngagement.copy(comments = updated)
                )
            }
        }
    }

    fun toggleReplies(commentId: String) {
        val engagement = uiState.value.youtubeEngagement
        val comment = engagement.comments.items.firstOrNull { it.id == commentId } ?: return
        if (comment.repliesExpanded) {
            updateComment(commentId) { it.copy(repliesExpanded = false) }
            return
        }
        if (comment.replies.isNotEmpty()) {
            updateComment(commentId) { it.copy(repliesExpanded = true) }
            return
        }
        loadReplies(commentId, append = false)
    }

    fun loadMoreReplies(commentId: String) {
        loadReplies(commentId, append = true)
    }

    private fun loadReplies(commentId: String, append: Boolean) {
        val engagement = uiState.value.youtubeEngagement
        val comment = engagement.comments.items.firstOrNull { it.id == commentId } ?: return
        val token = if (append) comment.repliesNextToken else comment.replyToken
        if (engagement.videoId.isBlank() || token.isBlank() || comment.repliesLoading) return
        val videoId = engagement.videoId
        val generation = engagementGeneration.get()
        replyJobs.remove(commentId)?.cancel()
        updateComment(commentId) {
            it.copy(repliesExpanded = true, repliesLoading = true, repliesError = null)
        }
        val job = scope.launch {
            val result = repository.replies(
                videoId = videoId,
                continuationToken = token,
                forceRefresh = comment.repliesError != null
            )
            if (!isActive || !isRequestCurrent(videoId, generation)) return@launch
            when (result) {
                is YoutubeCommentsResult.Available -> updateComment(commentId) { current ->
                    val base = if (append) current.replies else emptyList()
                    current.copy(
                        replies = (base + result.page.items).distinctBy(YoutubeComment::id),
                        repliesNextToken = result.page.nextToken.takeUnless { it == token }.orEmpty(),
                        repliesExpanded = true,
                        repliesLoading = false,
                        repliesError = null
                    )
                }
                YoutubeCommentsResult.Disabled -> updateComment(commentId) {
                    it.copy(repliesLoading = false, repliesNextToken = "")
                }
                is YoutubeCommentsResult.Failed -> updateComment(commentId) {
                    it.copy(repliesLoading = false, repliesError = "unavailable")
                }
            }
        }
        replyJobs[commentId] = job
        job.invokeOnCompletion { replyJobs.remove(commentId, job) }
    }

    private fun updateComment(
        commentId: String,
        transform: (YoutubeComment) -> YoutubeComment
    ) {
        uiState.update { state ->
            val comments = state.youtubeEngagement.comments
            val updatedItems = comments.items.map { comment ->
                if (comment.id == commentId) transform(comment) else comment
            }
            state.copy(
                youtubeEngagement = state.youtubeEngagement.copy(
                    comments = comments.copy(items = updatedItems)
                )
            )
        }
    }
}
