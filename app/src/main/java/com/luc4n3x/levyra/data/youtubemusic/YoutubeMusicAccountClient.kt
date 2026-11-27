package com.luc4n3x.levyra.data.youtubemusic

import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.SecureRandom
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class YoutubeMusicAccountHttpException(val status: Int) : IOException("YouTube Music HTTP $status")

internal data class RemotePlaylistRemoval(val videoId: String, val setVideoId: String)

internal class YoutubeMusicAccountClient {
    suspend fun accounts(cookie: String): List<YoutubeMusicAccount> {
        val seed = YoutubeMusicSession(YoutubeMusicAccount("", "", "", "", null, 0), cookie)
        return YoutubeMusicAccountParser.accounts(request(seed, "getAccountSwitcherEndpoint", null))
    }

    suspend fun verify(session: YoutubeMusicSession, language: String): YoutubeMusicAccount =
        YoutubeMusicAccountParser.activeAccount(request(session, "youtubei/v1/account/account_menu", payload(language)), session.account)

    suspend fun browse(session: YoutubeMusicSession, browseId: String, language: String, continuation: String = "", params: String = ""): JSONObject {
        val body = payload(language)
        if (continuation.isNotBlank()) body.put("continuation", continuation) else body.put("browseId", browseId)
        if (continuation.isBlank() && params.isNotBlank()) body.put("params", params)
        return request(session, "youtubei/v1/browse", body)
    }

    suspend fun createPlaylist(session: YoutubeMusicSession, title: String): String {
        val body = payload("en").put("title", title).put("description", "").put("privacyStatus", "PRIVATE")
        val root = request(session, "youtubei/v1/playlist/create", body)
        return extractCreatedPlaylistId(root) ?: throw YoutubeMusicProtocolException()
    }

    suspend fun addToPlaylist(session: YoutubeMusicSession, playlistId: String, videoIds: List<String>) {
        val valid = videoIds.map(String::trim).filter(String::isNotBlank)
        if (valid.isEmpty()) return
        valid.chunked(EDIT_BATCH_SIZE).forEach { batch ->
            addVideoBatchWithFallback(session, playlistId, batch)
        }
    }

    private suspend fun addVideoBatchWithFallback(
        session: YoutubeMusicSession,
        playlistId: String,
        videoIds: List<String>
    ) {
        if (videoIds.isEmpty()) return
        try {
            editPlaylist(session, buildAddVideosPayload(playlistId, videoIds))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "Batch add failed for %d videos to playlist %s, falling back to sub-batches", videoIds.size, playlistId)
            if (videoIds.size > 1) {
                val half = videoIds.size / 2
                addVideoBatchWithFallback(session, playlistId, videoIds.subList(0, half))
                addVideoBatchWithFallback(session, playlistId, videoIds.subList(half, videoIds.size))
            } else {
                Timber.w(error, "Single video %s could not be added to playlist %s", videoIds.first(), playlistId)
                if (error is YoutubeMusicSessionExpired || error is YoutubeMusicPlaylistNotFoundException || error is IOException) {
                    throw error
                }
            }
        }
    }

    suspend fun removeFromPlaylist(
        session: YoutubeMusicSession,
        playlistId: String,
        items: List<RemotePlaylistRemoval>
    ) {
        val valid = items.filter { it.videoId.isNotBlank() && it.setVideoId.isNotBlank() }
        if (valid.isEmpty()) return
        valid.chunked(EDIT_BATCH_SIZE).forEach { batch ->
            removeVideoBatchWithFallback(session, playlistId, batch)
        }
    }

    private suspend fun removeVideoBatchWithFallback(
        session: YoutubeMusicSession,
        playlistId: String,
        items: List<RemotePlaylistRemoval>
    ) {
        if (items.isEmpty()) return
        try {
            editPlaylist(session, buildRemoveVideosPayload(playlistId, items))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "Batch removal failed for %d items from playlist %s, falling back to sub-batches", items.size, playlistId)
            if (items.size > 1) {
                val half = items.size / 2
                removeVideoBatchWithFallback(session, playlistId, items.subList(0, half))
                removeVideoBatchWithFallback(session, playlistId, items.subList(half, items.size))
            } else {
                Timber.w(error, "Single item %s could not be removed from playlist %s", items.first(), playlistId)
                if (error is YoutubeMusicSessionExpired || error is YoutubeMusicPlaylistNotFoundException || error is IOException) {
                    throw error
                }
            }
        }
    }

    suspend fun renamePlaylist(session: YoutubeMusicSession, playlistId: String, title: String) {
        val cleanTitle = title.trim().take(MAX_PLAYLIST_TITLE_LENGTH)
        if (cleanTitle.isEmpty()) return
        editPlaylist(session, buildRenamePlaylistPayload(playlistId, cleanTitle))
    }

    private suspend fun editPlaylist(session: YoutubeMusicSession, body: JSONObject) {
        val root = request(session, "youtubei/v1/browse/edit_playlist", body)
        if (!isEditPlaylistSuccessResponse(root)) {
            Timber.w("Unexpected YouTube Music edit_playlist response status=%s", root.optString("status"))
            throw YoutubeMusicProtocolException()
        }
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

    private fun payload(language: String) = youtubeMusicContextPayload(language)

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
                            if (it.code == 404) throw YoutubeMusicPlaylistNotFoundException()
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
                            val rawClean = raw.trim()
                                .removePrefix(")]}'\n")
                                .removePrefix(")]}'\r\n")
                                .removePrefix(")]}'")
                                .trim()
                            val root = JSONObject(rawClean)
                            if (root.optJSONObject("responseContext")?.optBoolean("loggedOut") == true) throw YoutubeMusicSessionExpired()
                            if (root.has("error")) {
                                val errObj = root.optJSONObject("error")
                                val code = errObj?.optInt("code") ?: 0
                                val errStatus = errObj?.optString("status").orEmpty()
                                if (code == 404 || errStatus == "NOT_FOUND") throw YoutubeMusicPlaylistNotFoundException()
                                if (code == 401 || code == 403 || errStatus == "UNAUTHENTICATED") throw YoutubeMusicSessionExpired()
                                throw YoutubeMusicProtocolException()
                            }
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

    internal companion object {
        const val MAX_RESPONSE_BYTES = 8 * 1024 * 1024
        const val TRACKING_DOMAIN = "youtube.com"
        const val CPN_LENGTH = 16
        const val CPN_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        const val MAX_PLAYLIST_TITLE_LENGTH = 150
        const val EDIT_BATCH_SIZE = 25

        fun extractCreatedPlaylistId(root: JSONObject): String? {
            val direct = root.optString("playlistId").trim().ifBlank {
                root.optJSONObject("playlist")?.optString("playlistId")?.trim()
            }
            if (!direct.isNullOrBlank()) return direct

            fun findId(element: Any): String? {
                when (element) {
                    is JSONObject -> {
                        val pid = element.optString("playlistId").trim()
                        if (pid.isNotBlank()) return pid
                        val browseId = element.optString("browseId").trim()
                        if (browseId.isNotBlank() && (browseId.startsWith("VLPL") || browseId.startsWith("PL") || browseId.startsWith("FEmusic_playlist_detail_"))) {
                            return browseId.removePrefix("VL").removePrefix("FEmusic_playlist_detail_")
                        }
                        val keys = element.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val found = findId(element.get(key))
                            if (found != null) return found
                        }
                    }
                    is JSONArray -> {
                        for (i in 0 until element.length()) {
                            val found = findId(element.get(i))
                            if (found != null) return found
                        }
                    }
                }
                return null
            }
            return findId(root)
        }

        fun buildAddVideosPayload(playlistId: String, videoIds: List<String>): JSONObject {
            val actions = JSONArray()
            videoIds.forEach { videoId ->
                actions.put(
                    JSONObject()
                        .put("action", "ACTION_ADD_VIDEO")
                        .put("addedVideoId", videoId)
                )
            }
            return youtubeMusicContextPayload("en")
                .put("playlistId", playlistId.trim().substringAfter("list=").removePrefix("VL").trim())
                .put("actions", actions)
        }

        fun buildRemoveVideosPayload(playlistId: String, items: List<RemotePlaylistRemoval>): JSONObject {
            val actions = JSONArray()
            items.forEach { item ->
                actions.put(
                    JSONObject()
                        .put("action", "ACTION_REMOVE_VIDEO")
                        .put("removedVideoId", item.videoId)
                        .put("setVideoId", item.setVideoId)
                )
            }
            return youtubeMusicContextPayload("en")
                .put("playlistId", playlistId.trim().substringAfter("list=").removePrefix("VL").trim())
                .put("actions", actions)
        }

        fun isEditPlaylistSuccessResponse(root: JSONObject): Boolean {
            val status = root.optString("status").trim()
            if (status.contains("SUCCEEDED", ignoreCase = true) ||
                status.contains("SUCCESS", ignoreCase = true) ||
                status.contains("OK", ignoreCase = true) ||
                status.contains("COMPLETED", ignoreCase = true)
            ) {
                return true
            }
            if (status.contains("FAIL", ignoreCase = true) ||
                status.contains("ERROR", ignoreCase = true) ||
                status.contains("INVALID", ignoreCase = true) ||
                status.contains("REJECTED", ignoreCase = true)
            ) {
                return false
            }
            val hasResults = root.has("playlistEditResults") ||
                (root.optJSONArray("actions")?.length() ?: 0) > 0 ||
                (root.has("responseContext") && !root.has("error"))
            return status.isBlank() && hasResults
        }

        fun buildRenamePlaylistPayload(playlistId: String, title: String): JSONObject {
            val actions = JSONArray().put(
                JSONObject()
                    .put("action", "ACTION_SET_PLAYLIST_NAME")
                    .put("playlistName", title.trim().take(MAX_PLAYLIST_TITLE_LENGTH))
            )
            return youtubeMusicContextPayload("en")
                .put("playlistId", playlistId.trim().substringAfter("list=").removePrefix("VL").trim())
                .put("actions", actions)
        }
    }
}

private fun youtubeMusicContextPayload(language: String): JSONObject = JSONObject().put(
    "context",
    JSONObject()
        .put(
            "client",
            JSONObject()
                .put("clientName", "WEB_REMIX")
                .put("clientVersion", "1.20260423.01.00")
                .put("hl", language)
                .put("gl", "US")
        )
        .put("user", JSONObject().put("lockedSafetyMode", false))
)
