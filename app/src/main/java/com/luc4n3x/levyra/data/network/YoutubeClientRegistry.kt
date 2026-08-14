package com.luc4n3x.levyra.data.network

internal data class YoutubeClientProfile(
    val id: String,
    val clientName: String,
    val clientVersion: String,
    val clientHeaderName: String,
    val label: String,
    val userAgent: String,
    val host: String,
    val origin: String,
    val platform: String,
    val tier: Int,
    val delayMs: Long = 0L,
    val osName: String = "",
    val osVersion: String = "",
    val deviceMake: String = "",
    val deviceModel: String = "",
    val androidSdkVersion: Int = 0,
    val requiresPoToken: Boolean = false,
    val useSignatureTimestamp: Boolean = false,
    val deciphersStreamUrls: Boolean = false,
    val isEmbedded: Boolean = false,
    val useApiFormatVersion2: Boolean = false,
    val supportsLive: Boolean = true,
    val supportsUserUploads: Boolean = true,
    val supportsAgeRestricted: Boolean = false
)

internal object YoutubeClientRegistry {
    const val WEB_REMIX_VERSION = "1.20260423.01.00"
    const val WEB_VERSION = "2.20260630.01.00"
    const val ANDROID_VERSION = "19.44.38"
    const val ANDROID_MUSIC_VERSION = "8.10.52"
    const val ANDROID_VR_VERSION = "1.65.10"
    const val IOS_VERSION = "20.10.4"

    const val CLIENT_ID_WEB = "1"
    const val CLIENT_ID_ANDROID = "3"
    const val CLIENT_ID_IOS = "5"
    const val CLIENT_ID_ANDROID_MUSIC = "21"
    const val CLIENT_ID_ANDROID_VR = "28"
    const val CLIENT_ID_WEB_EMBEDDED = "56"
    const val CLIENT_ID_WEB_REMIX = "67"

    const val WEB_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36"

    private const val HOST_YOUTUBE = "https://www.youtube.com"
    private const val HOST_YOUTUBE_MUSIC = "https://music.youtube.com"
    private const val HOST_YOUTUBEI = "https://youtubei.googleapis.com"

    /**
     * Serves adaptive audio without a PoToken but refuses live streams and user uploads.
     */
    val ANDROID_VR = YoutubeClientProfile(
        id = "android-vr",
        clientName = "ANDROID_VR",
        clientVersion = ANDROID_VR_VERSION,
        clientHeaderName = CLIENT_ID_ANDROID_VR,
        label = "Android VR",
        userAgent = "com.google.android.apps.youtube.vr.oculus/$ANDROID_VR_VERSION " +
            "(Linux; U; Android 12L; Quest 3 Build/SQ3A.220605.009.A1) gzip",
        host = HOST_YOUTUBEI,
        origin = "",
        platform = "MOBILE",
        tier = 0,
        osName = "Android",
        osVersion = "12L",
        deviceMake = "Oculus",
        deviceModel = "Quest 3",
        androidSdkVersion = 32,
        useApiFormatVersion2 = true,
        supportsLive = false,
        supportsUserUploads = false
    )

    val ANDROID_MUSIC = YoutubeClientProfile(
        id = "android-music",
        clientName = "ANDROID_MUSIC",
        clientVersion = ANDROID_MUSIC_VERSION,
        clientHeaderName = CLIENT_ID_ANDROID_MUSIC,
        label = "Android Music",
        userAgent = "com.google.android.apps.youtube.music/$ANDROID_MUSIC_VERSION " +
            "(Linux; U; Android 15; Pixel 8 Pro Build/AP3A.241105.007) gzip",
        host = HOST_YOUTUBEI,
        origin = "",
        platform = "MOBILE",
        tier = 1,
        osName = "Android",
        osVersion = "15",
        androidSdkVersion = 35,
        useApiFormatVersion2 = true,
        supportsUserUploads = false
    )

    val ANDROID = YoutubeClientProfile(
        id = "android",
        clientName = "ANDROID",
        clientVersion = ANDROID_VERSION,
        clientHeaderName = CLIENT_ID_ANDROID,
        label = "Android",
        userAgent = "com.google.android.youtube/$ANDROID_VERSION " +
            "(Linux; U; Android 15; Pixel 8 Pro Build/AP3A.241105.007) gzip",
        host = HOST_YOUTUBEI,
        origin = "",
        platform = "MOBILE",
        tier = 2,
        osName = "Android",
        osVersion = "15",
        androidSdkVersion = 35,
        useApiFormatVersion2 = true
    )

    val IOS = YoutubeClientProfile(
        id = "ios",
        clientName = "IOS",
        clientVersion = IOS_VERSION,
        clientHeaderName = CLIENT_ID_IOS,
        label = "iOS",
        userAgent = "com.google.ios.youtube/$IOS_VERSION (iPhone16,2; U; CPU iOS 18_3 like Mac OS X)",
        host = HOST_YOUTUBEI,
        origin = "",
        platform = "MOBILE",
        tier = 3,
        osName = "iPhone",
        osVersion = "18.3",
        deviceMake = "Apple",
        deviceModel = "iPhone16,2"
    )

    val WEB_REMIX = YoutubeClientProfile(
        id = "web-remix",
        clientName = "WEB_REMIX",
        clientVersion = WEB_REMIX_VERSION,
        clientHeaderName = CLIENT_ID_WEB_REMIX,
        label = "YouTube Music Web",
        userAgent = WEB_USER_AGENT,
        host = HOST_YOUTUBE_MUSIC,
        origin = HOST_YOUTUBE_MUSIC,
        platform = "DESKTOP",
        tier = 4,
        requiresPoToken = true,
        useSignatureTimestamp = true,
        deciphersStreamUrls = true
    )

    val WEB = YoutubeClientProfile(
        id = "web",
        clientName = "WEB",
        clientVersion = WEB_VERSION,
        clientHeaderName = CLIENT_ID_WEB,
        label = "YouTube Web",
        userAgent = WEB_USER_AGENT,
        host = HOST_YOUTUBE,
        origin = HOST_YOUTUBE,
        platform = "DESKTOP",
        tier = 5,
        requiresPoToken = true,
        useSignatureTimestamp = true,
        deciphersStreamUrls = true
    )

    /**
     * Embedded surface kept last: it is the only Levyra client able to negotiate
     * age-restricted playback when every other candidate is denied.
     */
    val WEB_EMBEDDED_PLAYER = YoutubeClientProfile(
        id = "web-embedded",
        clientName = "WEB_EMBEDDED_PLAYER",
        clientVersion = WEB_REMIX_VERSION,
        clientHeaderName = CLIENT_ID_WEB_EMBEDDED,
        label = "Embedded Player",
        userAgent = WEB_USER_AGENT,
        host = HOST_YOUTUBE,
        origin = HOST_YOUTUBE,
        platform = "DESKTOP",
        tier = 6,
        isEmbedded = true,
        useSignatureTimestamp = true,
        deciphersStreamUrls = true,
        supportsAgeRestricted = true
    )

    val playbackProfiles: List<YoutubeClientProfile> = listOf(
        ANDROID_VR,
        ANDROID_MUSIC,
        ANDROID,
        IOS,
        WEB_REMIX,
        WEB,
        WEB_EMBEDDED_PLAYER
    )

    fun browseProfiles(webRemixVersion: String): List<YoutubeClientProfile> = listOf(
        WEB_REMIX.copy(clientVersion = webRemixVersion.ifBlank { WEB_REMIX_VERSION }, tier = 0),
        ANDROID_MUSIC.copy(tier = 1),
        ANDROID.copy(tier = 2),
        IOS.copy(tier = 3, osName = "iOS"),
        WEB.copy(tier = 4)
    )
}
