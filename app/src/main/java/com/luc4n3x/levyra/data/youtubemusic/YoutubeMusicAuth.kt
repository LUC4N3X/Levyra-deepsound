package com.luc4n3x.levyra.data.youtubemusic

import java.io.IOException
import java.net.URI
import java.security.MessageDigest

internal class YoutubeMusicAccountStorageException : IOException("YouTube Music session storage is unavailable")
internal class YoutubeMusicSessionExpired : IOException("YouTube Music session requires sign-in")
internal class YoutubeMusicProtocolException : IOException("YouTube Music returned an unsupported response")
internal class YoutubeMusicLibraryLimit : IOException("YouTube Music library exceeds the import limit")

internal object YoutubeMusicAuth {
    const val ORIGIN = "https://music.youtube.com"
    const val LOGIN_URL = "https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&uilel=3&passive=true&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26app%3Ddesktop%26hl%3Den%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F%26feature%3D__FEATURE__&hl=en"

    fun cookies(value: String): Map<String, String> = value.split(';').mapNotNull {
        val parts = it.trim().split('=', limit = 2)
        if (parts.size == 2 && parts[0].isNotBlank()) parts[0] to parts[1] else null
    }.toMap()

    fun authorization(cookie: String, epochSeconds: Long): String {
        val values = cookies(cookie)
        val sapisid = values["SAPISID"]?.takeIf(String::isNotBlank)
            ?: values["__Secure-3PAPISID"]?.takeIf(String::isNotBlank)
            ?: throw YoutubeMusicSessionExpired()
        val digest = MessageDigest.getInstance("SHA-1")
            .digest("$epochSeconds $sapisid $ORIGIN".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return "SAPISIDHASH ${epochSeconds}_$digest"
    }

    fun isMusicUrl(url: String): Boolean = runCatching {
        val uri = URI(url)
        uri.scheme == "https" && uri.host == "music.youtube.com" &&
            uri.rawUserInfo == null && uri.port in listOf(-1, 443)
    }.getOrDefault(false)

    fun isSecureWebUrl(url: String): Boolean = runCatching {
        val uri = URI(url)
        uri.scheme == "https" && uri.host != null &&
            uri.rawUserInfo == null && uri.port in listOf(-1, 443)
    }.getOrDefault(false)
}
