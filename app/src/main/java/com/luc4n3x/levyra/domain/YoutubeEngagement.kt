package com.luc4n3x.levyra.domain

import androidx.compose.runtime.Immutable

@Immutable
data class YoutubeComment(
    val id: String,
    val text: String,
    val author: String,
    val authorAvatarUrl: String = "",
    val authorUrl: String = "",
    val publishedText: String = "",
    val likeCountText: String = "",
    val pinned: Boolean = false,
    val heartedByUploader: Boolean = false,
    val verifiedAuthor: Boolean = false,
    val streamPositionSeconds: Int = -1,
    val replyCount: Int = 0,
    val replyToken: String = "",
    val replies: List<YoutubeComment> = emptyList(),
    val repliesNextToken: String = "",
    val repliesExpanded: Boolean = false,
    val repliesLoading: Boolean = false,
    val repliesError: String? = null
)

@Immutable
data class YoutubeCommentsState(
    val videoId: String = "",
    val visible: Boolean = false,
    val loaded: Boolean = false,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val disabled: Boolean = false,
    val countText: String = "",
    val items: List<YoutubeComment> = emptyList(),
    val nextToken: String = "",
    val error: String? = null
)

@Immutable
data class YoutubeEngagementState(
    val videoId: String = "",
    val estimatedDislikeCount: Long = -1L,
    val dislikeEstimateLoading: Boolean = false,
    val dislikeEstimateAvailable: Boolean = false,
    val dislikeEstimateError: String? = null,
    val comments: YoutubeCommentsState = YoutubeCommentsState()
)
