package com.luc4n3x.levyra.data

import android.content.Context
import android.os.Looper
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

internal class LevyraYoutubeSessionPoTokenProvider(context: Context) : YoutubeSessionPoTokenProvider {
    private val security = YoutubePlaybackSecurity.getInstance(context)

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
                    val session = security.currentSessionRequired()
                    security.playerPoTokenRequired(session)
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
        const val PROVIDER_TIMEOUT_MS = 55_000L
    }
}
