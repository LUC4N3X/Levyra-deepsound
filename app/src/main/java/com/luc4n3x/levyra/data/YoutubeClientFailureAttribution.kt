package com.luc4n3x.levyra.data

import org.json.JSONObject
import java.util.Locale

internal enum class YoutubeClientFailureScope {
    CLIENT,
    TRANSIENT,
    NOT_ATTRIBUTABLE
}

internal object YoutubeClientFailureAttribution {
    private val botMarkers = listOf(
        "not a bot",
        "automated traffic",
        "unusual traffic",
        "non essere un bot"
    )
    private val contentMarkers = listOf(
        "video is private",
        "video privato",
        "confirm your age",
        "inappropriate for some users",
        "age-restrict",
        "age restrict",
        "conferma la tua età",
        "members-only",
        "join this channel",
        "requires payment",
        "music premium",
        "available in your country",
        "not available in your region",
        "non disponibile nel tuo paese",
        "geo_restricted",
        "has been removed",
        "no longer available",
        "has been terminated",
        "live event will begin",
        "premieres in"
    )
    private val policyMarkers = listOf(
        "non abilitato alla capability",
        "identità video youtube assente"
    )
    private val contentStatuses = setOf("LIVE_STREAM_OFFLINE")
    private val transientKinds = setOf(
        PlaybackFailureKind.Network,
        PlaybackFailureKind.Timeout,
        PlaybackFailureKind.Truncated,
        PlaybackFailureKind.ServerError
    )

    fun playabilityScope(playability: JSONObject?): YoutubeClientFailureScope {
        val status = playability?.optString("status").orEmpty()
        val messages = playability?.optJSONArray("messages")
        val text = buildString {
            append(playability?.optString("reason").orEmpty())
            if (messages != null) {
                for (index in 0 until messages.length()) {
                    append(' ').append(messages.opt(index) as? String ?: "")
                }
            }
        }
        return playabilityScope(status, text)
    }

    fun playabilityScope(status: String, text: String): YoutubeClientFailureScope {
        val normalized = text.lowercase(Locale.ROOT)
        if (botMarkers.any(normalized::contains)) return YoutubeClientFailureScope.CLIENT
        if (status.uppercase(Locale.ROOT) in contentStatuses) return YoutubeClientFailureScope.NOT_ATTRIBUTABLE
        if (contentMarkers.any(normalized::contains)) return YoutubeClientFailureScope.NOT_ATTRIBUTABLE
        return YoutubeClientFailureScope.CLIENT
    }

    fun scope(error: Throwable): YoutubeClientFailureScope {
        if (YoutubePlaybackSecurity.isLocalRuntimeFailure(error)) return YoutubeClientFailureScope.NOT_ATTRIBUTABLE
        val chain = generateSequence(error) { it.cause }.take(MAX_CAUSE_DEPTH).toList()
        val request = chain.firstNotNullOfOrNull { it as? YoutubePlayerRequestException }
        request?.playabilityScope?.let { return it }
        val code = request?.httpCode
        if (code != null) {
            return if (code == 408 || code >= 500) {
                YoutubeClientFailureScope.TRANSIENT
            } else {
                YoutubeClientFailureScope.CLIENT
            }
        }
        val text = chain.joinToString(" ") { it.message.orEmpty() }.lowercase(Locale.ROOT)
        if (botMarkers.any(text::contains)) return YoutubeClientFailureScope.CLIENT
        if (policyMarkers.any(text::contains) || contentMarkers.any(text::contains)) {
            return YoutubeClientFailureScope.NOT_ATTRIBUTABLE
        }
        return if (classifyPlaybackFailureReason(text) in transientKinds) {
            YoutubeClientFailureScope.TRANSIENT
        } else {
            YoutubeClientFailureScope.CLIENT
        }
    }

    private const val MAX_CAUSE_DEPTH = 8
}
