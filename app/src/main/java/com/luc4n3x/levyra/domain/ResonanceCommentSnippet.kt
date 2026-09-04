package com.luc4n3x.levyra.domain

import androidx.compose.runtime.Immutable

/**
 * Real YouTube comment snippet displayed in the "Voci che risuonano" (most discussed tracks)
 * home shelf. Holds authentic comment text, count, and author metadata without invented content.
 */
@Immutable
data class ResonanceCommentSnippet(
    val videoId: String,
    val countText: String = "",
    val author: String = "",
    val authorAvatarUrl: String = "",
    val text: String = "",
    val likeCountText: String = "",
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val disabled: Boolean = false
) {
    val hasComment: Boolean
        get() = text.isNotBlank()
}
