package com.luc4n3x.levyra.data

import android.content.Context
import android.os.Looper
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.services.youtube.YoutubeSessionPoToken
import org.schabi.newpipe.extractor.services.youtube.YoutubeSessionPoTokenProvider
import timber.log.Timber
import java.io.IOException

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
        if (clientName.startsWith("TVHTML5", ignoreCase = true)) return null
        if (Looper.myLooper() == Looper.getMainLooper()) return null

        return try {
            runBlocking {
                withTimeout(PROVIDER_TIMEOUT_MS) {
                    val session = security.currentSession()
                    val tokens = security.poTokensRequired(session.visitorData, session)
                    tokens.playerToken
                        .takeIf(String::isNotBlank)
                        ?.let { YoutubeSessionPoToken(session.visitorData, it) }
                }
            }
        } catch (error: TimeoutCancellationException) {
            throw IOException("Extractor session PoToken timed out", error)
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            throw error
        } catch (error: Exception) {
            Timber.w(
                error,
                "Extractor session PoToken failed for %s/%s",
                clientName,
                clientVersion
            )
            throw IOException("Extractor session PoToken failed", error)
        }
    }

    private companion object {
        // Covers the 15s visitor request plus the 20s runtime initialization and 12s mint budget.
        const val PROVIDER_TIMEOUT_MS = 55_000L
    }
}
