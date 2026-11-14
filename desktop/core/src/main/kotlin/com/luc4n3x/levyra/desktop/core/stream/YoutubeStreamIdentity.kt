package com.luc4n3x.levyra.desktop.core.stream

import com.luc4n3x.levyra.desktop.core.extractor.ExtractorHttp
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper
import com.luc4n3x.levyra.desktop.core.extractor.ExtractorRuntime

data class YoutubeStreamIdentity(val userAgent: String, val referrer: String?) {
    companion object {
        fun forUrl(url: String): YoutubeStreamIdentity? {
            val parsed = url.toHttpUrlOrNull() ?: return null
            if (listOf("googlevideo.com", "youtube.com", "youtube-nocookie.com", "ytimg.com").none {
                parsed.host == it || parsed.host.endsWith(".$it")
            }) return null
            val client = parsed.queryParameter("c").orEmpty()
            val userAgent = when (client) {
                "ANDROID_VR" -> YoutubeParsingHelper.getAndroidVRUserAgent(null)
                "ANDROID" -> YoutubeParsingHelper.getAndroidUserAgent(null)
                "IOS" -> YoutubeParsingHelper.getIosUserAgent(null)
                "VISIONOS" -> visionOsUserAgent(ExtractorRuntime.contentCountry().countryCode)
                else -> ExtractorHttp.DESKTOP_USER_AGENT
            }
            val referrer = when {
                client.contains("EMBEDDED") -> "https://www.youtube.com/embed/"
                client in setOf("WEB", "MWEB", "WEB_EMBEDDED_PLAYER") -> "https://www.youtube.com/"
                else -> null
            }
            return YoutubeStreamIdentity(userAgent, referrer)
        }
    }
}
