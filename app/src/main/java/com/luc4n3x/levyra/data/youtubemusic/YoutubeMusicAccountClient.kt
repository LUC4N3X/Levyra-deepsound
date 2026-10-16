package com.luc4n3x.levyra.data.youtubemusic

import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.SecureRandom
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class YoutubeMusicAccountHttpException(val status: Int) : IOException("YouTube Music HTTP $status")

internal class YoutubeMusicAccountClient {
    suspend fun accounts(cookie: String): List<YoutubeMusicAccount> {
        val seed = YoutubeMusicSession(YoutubeMusicAccount("", "", "", "", null, 0), cookie)
        return YoutubeMusicAccountParser.accounts(request(seed, "getAccountSwitcherEndpoint", null))
    }

    suspend fun verify(session: YoutubeMusicSession, language: String): YoutubeMusicAccount =
        YoutubeMusicAccountParser.activeAccount(request(session, "youtubei/v1/account/account_menu", payload(language)), session.account)

    suspend fun browse(session: YoutubeMusicSession, browseId: String, language: String, continuation: String = ""): JSONObject {
        val body = payload(language)
        if (continuation.isNotBlank()) body.put("continuation", continuation) else body.put("browseId", browseId)
        return request(session, "youtubei/v1/browse", body)
    }

    suspend fun registerPlayback(session: YoutubeMusicSession, videoId: String) {
        val player = request(session, "youtubei/v1/player", payload("en").put("videoId", videoId))
        val trackingUrl = player.optJSONObject("playbackTracking")
            ?.optJSONObject("videostatsPlaybackUrl")
            ?.optString("baseUrl")
            ?.toHttpUrlOrNull()
            ?.takeIf { it.isHttps && (it.host == TRACKING_DOMAIN || it.host.endsWith(".$TRACKING_DOMAIN")) }
            ?: throw YoutubeMusicProtocolException()
        val url = trackingUrl.newBuilder()
            .setQueryParameter("ver", "2")
            .setQueryParameter("c", "WEB_REMIX")
            .setQueryParameter("cpn", clientPlaybackNonce())
            .build()
        val call = LevyraHttpClientFactory.externalIntegrations().newCall(authorized(session, url.toString()).build())
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
                override fun onResponse(call: Call, response: Response) {
                    val status = response.use { it.code }
                    if (!continuation.isActive) return
                    when {
                        status == 401 || status == 403 -> continuation.resumeWithException(YoutubeMusicSessionExpired())
                        status !in 200..299 -> continuation.resumeWithException(YoutubeMusicAccountHttpException(status))
                        else -> continuation.resume(Unit)
                    }
                }
            })
        }
    }

    private fun clientPlaybackNonce(): String {
        val random = SecureRandom()
        return String(CharArray(CPN_LENGTH) { CPN_ALPHABET[random.nextInt(CPN_ALPHABET.length)] })
    }

    private fun payload(language: String) = JSONObject().put("context", JSONObject()
        .put("client", JSONObject().put("clientName", "WEB_REMIX").put("clientVersion", "1.20260423.01.00")
            .put("hl", language).put("gl", "US"))
        .put("user", JSONObject().put("lockedSafetyMode", false)))

    private fun authorized(session: YoutubeMusicSession, url: String): Request.Builder {
        val account = session.account
        val authorization = YoutubeMusicAuth.authorization(session.cookie, System.currentTimeMillis() / 1000)
        val request = Request.Builder().url(url)
            .header("Cookie", session.cookie).header("Authorization", authorization)
            .header("Origin", YoutubeMusicAuth.ORIGIN).header("x-origin", YoutubeMusicAuth.ORIGIN)
            .header("Referer", "${YoutubeMusicAuth.ORIGIN}/").header("X-Goog-Authuser", account.authUser.toString())
            .header("X-YouTube-Client-Name", "67").header("X-YouTube-Client-Version", "1.20260423.01.00")
            .header("X-Goog-Api-Format-Version", "1")
            .header("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
        account.pageId?.let { request.header("X-Goog-Pageid", it) }
        return request
    }

    private suspend fun request(session: YoutubeMusicSession, path: String, body: JSONObject?): JSONObject {
        val request = authorized(session, "${YoutubeMusicAuth.ORIGIN}/$path?prettyPrint=false")
        if (body != null) request.post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
        val call = LevyraHttpClientFactory.externalIntegrations().newCall(request.build())
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val parsed = response.use {
                            if (it.code == 401 || it.code == 403) throw YoutubeMusicSessionExpired()
                            if (!it.isSuccessful) throw YoutubeMusicAccountHttpException(it.code)
                            val responseBody = it.body
                            if (responseBody.contentLength() > MAX_RESPONSE_BYTES) throw YoutubeMusicProtocolException()
                            val raw = responseBody.byteStream().use { input ->
                                val output = ByteArrayOutputStream()
                                val buffer = ByteArray(8192)
                                var total = 0
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read < 0) break
                                    total += read
                                    if (total > MAX_RESPONSE_BYTES) throw YoutubeMusicProtocolException()
                                    output.write(buffer, 0, read)
                                }
                                output.toString("UTF-8")
                            }
                            val root = JSONObject(raw.removePrefix(")]}'\n"))
                            if (root.optJSONObject("responseContext")?.optBoolean("loggedOut") == true) throw YoutubeMusicSessionExpired()
                            if (root.has("error")) throw YoutubeMusicProtocolException()
                            root
                        }
                        if (continuation.isActive) continuation.resume(parsed)
                    } catch (error: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
                }
            })
        }
    }

    private companion object {
        const val MAX_RESPONSE_BYTES = 8 * 1024 * 1024
        const val TRACKING_DOMAIN = "youtube.com"
        const val CPN_LENGTH = 16
        const val CPN_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    }
}
