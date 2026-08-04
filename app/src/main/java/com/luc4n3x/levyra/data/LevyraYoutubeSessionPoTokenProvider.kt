package com.luc4n3x.levyra.data

import android.content.Context
import android.os.Looper
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.services.youtube.YoutubeSessionPoToken
import org.schabi.newpipe.extractor.services.youtube.YoutubeSessionPoTokenProvider
import timber.log.Timber

/**
 * Supplies the vendored extractor with the same visitor-bound player PoToken flow used by Levyra's
 * direct InnerTube resolver. The provider deliberately returns null on the main thread so extractor
 * compatibility fallback remains non-blocking.
 */
internal class LevyraYoutubeSessionPoTokenProvider(context: Context) : YoutubeSessionPoTokenProvider {
    private val appContext = context.applicationContext
    private val security = YoutubePlaybackSecurity(
        appContext,
        LevyraHttpClientFactory.youtubePlayer(),
        BuildConfig.YOUTUBE_INNERTUBE_API_KEY,
        LevyraPreferences(appContext)
    )

    override fun getSessionPoToken(
        clientName: String,
        clientVersion: String,
        userAgent: String?,
        localization: Localization,
        contentCountry: ContentCountry,
        loggedIn: Boolean
    ): YoutubeSessionPoToken? {
        if (clientName.equals("TVHTML5", ignoreCase = true)) return null
        if (Looper.myLooper() == Looper.getMainLooper()) return null

        return try {
            runBlocking(Dispatchers.IO) {
                val session = security.currentSession()
                val tokens = security.poTokens(session.visitorData, session) ?: return@runBlocking null
                tokens.playerToken
                    .takeIf(String::isNotBlank)
                    ?.let { YoutubeSessionPoToken(session.visitorData, it) }
            }
        } catch (error: Throwable) {
            Timber.w(
                error,
                "Extractor session PoToken unavailable for %s/%s",
                clientName,
                clientVersion
            )
            null
        }
    }
}
