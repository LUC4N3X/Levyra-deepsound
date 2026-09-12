package com.luc4n3x.levyra.feature.radio

import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject

internal class RadioBrowserApi(
    private val client: OkHttpClient = LevyraHttpClientFactory.externalIntegrations(),
    private val serverPool: RadioBrowserServerPool = RadioBrowserServerPool()
) {
    suspend fun stations(filter: RadioFilter): List<RadioStation> {
        val params = linkedMapOf<String, String>()
        filter.countryCode?.takeIf(String::isNotBlank)?.let { params["countrycode"] = it.uppercase(Locale.ROOT) }
        filter.language?.takeIf(String::isNotBlank)?.let { params["language"] = it }
        filter.category.apiTag?.let { params["tag"] = it }
        params["order"] = if (filter.category == RadioCategory.Worldwide) "clickcount" else "votes"
        params["reverse"] = "true"
        params["hidebroken"] = "true"
        params["offset"] = filter.offset.coerceAtLeast(0).toString()
        params["limit"] = filter.limit.coerceIn(1, MAX_STATION_LIMIT).toString()
        return stationRequest("json/stations/search", params)
    }

    suspend fun search(query: String, limit: Int = 24): List<RadioStation> {
        val clean = query.trim().take(MAX_QUERY_LENGTH)
        if (clean.length < 2) return emptyList()
        val boundedLimit = limit.coerceIn(1, MAX_STATION_LIMIT)
        return coroutineScope {
            val deferreds = listOf("name", "country", "language", "tag").map { field ->
                async {
                    try {
                        Result.success(
                            stationRequest(
                                path = "json/stations/search",
                                params = mapOf(
                                    field to clean,
                                    "order" to "votes",
                                    "reverse" to "true",
                                    "hidebroken" to "true",
                                    "limit" to boundedLimit.toString()
                                )
                            )
                        )
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Throwable) {
                        Result.failure(error)
                    }
                }
            }
            val results = deferreds.awaitAll()
            val (successes, failures) = results.partition { it.isSuccess }
            if (successes.isEmpty() && failures.isNotEmpty()) {
                throw failures.first().exceptionOrNull() ?: IllegalStateException("Radio Browser search failed")
            }
            successes.flatMap { it.getOrDefault(emptyList()) }
        }
    }

    suspend fun countries(): List<RadioDirectoryEntry> = directoryRequest("json/countries") { json ->
        RadioDirectoryEntry(
            name = json.optString("name").trim(),
            code = json.optString("iso_3166_1").trim().uppercase(Locale.ROOT),
            stationCount = json.optInt("stationcount", 0)
        )
    }

    suspend fun languages(): List<RadioDirectoryEntry> = directoryRequest("json/languages") { json ->
        RadioDirectoryEntry(
            name = json.optString("name").trim(),
            stationCount = json.optInt("stationcount", 0)
        )
    }

    suspend fun recordClick(stationUuid: String) {
        if (!SAFE_UUID.matches(stationUuid)) return
        try {
            execute("json/url/$stationUuid", emptyMap(), MAX_CLICK_BYTES)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
        }
    }

    private suspend fun stationRequest(path: String, params: Map<String, String>): List<RadioStation> {
        val body = execute(path, params, MAX_RESPONSE_BYTES)
        val array = JSONArray(body)
        return buildList(array.length().coerceAtMost(MAX_PARSED_STATIONS)) {
            for (index in 0 until minOf(array.length(), MAX_PARSED_STATIONS)) {
                array.optJSONObject(index)?.toRadioStation()?.let(::add)
            }
        }
    }

    private suspend fun directoryRequest(
        path: String,
        transform: (JSONObject) -> RadioDirectoryEntry
    ): List<RadioDirectoryEntry> {
        val body = execute(
            path,
            mapOf("hidebroken" to "true", "order" to "stationcount", "reverse" to "true", "limit" to "500"),
            MAX_DIRECTORY_BYTES
        )
        val array = JSONArray(body)
        return buildList(array.length().coerceAtMost(500)) {
            for (index in 0 until minOf(array.length(), 500)) {
                array.optJSONObject(index)?.let(transform)?.takeIf { it.name.isNotBlank() && it.stationCount > 0 }?.let(::add)
            }
        }
    }

    private suspend fun execute(path: String, params: Map<String, String>, maxBytes: Int): String {
        var lastFailure: Throwable? = null
        val servers = serverPool.servers()
        val rotation = nextServer.getAndIncrement()
        repeat(minOf(servers.size, MAX_SERVER_ATTEMPTS)) { attempt ->
            val server = servers[Math.floorMod(rotation + attempt, servers.size)]
            val url = buildUrl(server, path, params)
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .header("User-Agent", USER_AGENT)
                    .build()
                return client.await(request).use { response ->
                    if (!response.isSuccessful) error("Radio Browser HTTP ${response.code}")
                    response.body.readBoundedUtf8(maxBytes)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                lastFailure = error
            }
        }
        throw lastFailure ?: IllegalStateException("Radio Browser unavailable")
    }

    private fun buildUrl(server: String, path: String, params: Map<String, String>): HttpUrl {
        val builder = "https://$server/".toHttpUrl().newBuilder()
        path.split('/').filter(String::isNotBlank).forEach(builder::addPathSegment)
        params.forEach { (key, value) -> builder.addQueryParameter(key, value) }
        return builder.build()
    }

    private fun JSONObject.toRadioStation(): RadioStation? {
        val uuid = optString("stationuuid").trim()
        val name = optString("name").trim()
        if (uuid.isBlank() || name.isBlank()) return null
        return RadioStation(
            uuid = uuid,
            name = name,
            streamUrl = optString("url").trim(),
            resolvedStreamUrl = optString("url_resolved").trim(),
            faviconUrl = optString("favicon").trim(),
            homepageUrl = optString("homepage").trim(),
            country = optString("country").trim(),
            countryCode = optString("countrycode").trim().uppercase(Locale.ROOT),
            language = optString("language").trim(),
            tags = optString("tags").split(',').map(String::trim).filter(String::isNotBlank).distinct().take(24),
            codec = optString("codec").trim(),
            bitrateKbps = optInt("bitrate", 0).coerceAtLeast(0),
            votes = optInt("votes", 0).coerceAtLeast(0),
            clickCount = optInt("clickcount", 0).coerceAtLeast(0),
            lastCheckOk = optInt("lastcheckok", 0) == 1 || optBoolean("lastcheckok", false)
        )
    }

    private companion object {
        val USER_AGENT = "Levyra/${BuildConfig.VERSION_NAME} (Android; Live Radio)"
        const val MAX_QUERY_LENGTH = 80
        const val MAX_STATION_LIMIT = 64
        const val MAX_PARSED_STATIONS = 256
        const val MAX_SERVER_ATTEMPTS = 3
        const val MAX_RESPONSE_BYTES = 2_000_000
        const val MAX_DIRECTORY_BYTES = 400_000
        const val MAX_CLICK_BYTES = 16_000
        val SAFE_UUID = Regex("^[a-fA-F0-9-]{8,64}$")
        val nextServer = AtomicInteger(0)
    }
}

internal class RadioBrowserServerPool(
    private val lookup: suspend (String) -> Array<InetAddress> = { host ->
        withContext(Dispatchers.IO) { InetAddress.getAllByName(host) }
    },
    private val now: () -> Long = System::currentTimeMillis
) {
    @Volatile private var cached = emptyList<String>()
    @Volatile private var cachedAt = 0L
    @Volatile private var isFallback = false

    suspend fun servers(): List<String> {
        val current = cached
        val ttl = if (isFallback) FALLBACK_CACHE_MS else SERVER_CACHE_MS
        if (current.isNotEmpty() && now() - cachedAt < ttl) return current
        val discovered = try {
            lookup(DISCOVERY_HOST).mapNotNull { address ->
                address.canonicalHostName
                    .trimEnd('.')
                    .lowercase(Locale.ROOT)
                    .takeIf { it != DISCOVERY_HOST && it.endsWith(".api.radio-browser.info") }
            }.distinct()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            emptyList()
        }
        val fallback = discovered.isEmpty()
        val result = discovered.ifEmpty { listOf(DISCOVERY_HOST) }
        cached = result
        cachedAt = now()
        isFallback = fallback
        return result
    }

    private companion object {
        const val DISCOVERY_HOST = "all.api.radio-browser.info"
        const val SERVER_CACHE_MS = 24 * 60 * 60 * 1_000L
        const val FALLBACK_CACHE_MS = 5 * 60 * 1_000L
    }
}

private suspend fun OkHttpClient.await(request: Request): Response = suspendCancellableCoroutine { continuation ->
    val call = newCall(request)
    continuation.invokeOnCancellation { call.cancel() }
    call.enqueue(object : Callback {
        override fun onFailure(call: Call, e: java.io.IOException) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            if (continuation.isActive) continuation.resume(response) else response.close()
        }
    })
}

private fun okhttp3.ResponseBody.readBoundedUtf8(maxBytes: Int): String {
    if (contentLength() > maxBytes) error("Radio Browser response too large")
    byteStream().use { input ->
        val output = ByteArrayOutputStream(minOf(maxBytes, 64 * 1024))
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > maxBytes) error("Radio Browser response too large")
            output.write(buffer, 0, count)
        }
        return output.toString(Charsets.UTF_8.name())
    }
}
