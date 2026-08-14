package com.luc4n3x.levyra.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory

@UnstableApi
object PlaybackNetworkStack {
    private const val USER_AGENT = "LevyraPlayer/1.13 Android Music"

    fun initialize(context: Context) {
        LevyraHttpClientFactory.media(context.applicationContext)
    }

    fun playbackFactory(context: Context): HttpDataSource.Factory {
        return createOkHttpFactory(context)
    }

    fun warmupFactory(context: Context): HttpDataSource.Factory {
        return createOkHttpFactory(context)
    }

    private fun createOkHttpFactory(context: Context): HttpDataSource.Factory {
        return OkHttpDataSource.Factory(LevyraHttpClientFactory.media(context.applicationContext))
            .setUserAgent(USER_AGENT)
    }
}
