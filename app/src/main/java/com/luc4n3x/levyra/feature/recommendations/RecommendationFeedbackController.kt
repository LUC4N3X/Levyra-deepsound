package com.luc4n3x.levyra.feature.recommendations

import android.content.Context
import com.luc4n3x.levyra.data.RecommendationFeedbackStore
import com.luc4n3x.levyra.domain.Track

internal class RecommendationFeedbackController(context: Context) {
    private val store = RecommendationFeedbackStore(context.applicationContext)

    suspend fun moreLike(track: Track) = store.moreLike(track)

    suspend fun lessLike(track: Track) = store.lessLike(track)

    suspend fun blockArtist(track: Track) = store.blockArtist(track)

    suspend fun unblockArtist(track: Track) = store.unblockArtist(track)

    suspend fun isArtistBlocked(track: Track): Boolean = store.isArtistBlocked(track)
}
