from pathlib import Path
import re

ROOT = Path('.')


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding='utf-8')


def write(path: str, text: str) -> None:
    (ROOT / path).write_text(text, encoding='utf-8')


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected one match, found {count}')
    return text.replace(old, new, 1)


def regex_once(text: str, pattern: str, replacement, label: str, flags: int = 0) -> str:
    updated, count = re.subn(pattern, replacement, text, count=1, flags=flags)
    if count != 1:
        raise SystemExit(f'{label}: expected one match, found {count}')
    return updated


# ---------------------------------------------------------------------------
# Playback security exposes a strict token path for extractor-required flows.
# ---------------------------------------------------------------------------
path = 'app/src/main/java/com/luc4n3x/levyra/data/YoutubePlaybackSecurity.kt'
text = read(path)
old = '''    suspend fun poTokens(videoId: String, session: YoutubeGuestSession): YoutubePoTokens? {
        if (videoId.isBlank() || session.visitorData.isBlank()) return null
        return try {
            tokenGenerator.generate(videoId, session.visitorData, session.generation)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.w(error, "PO Token generation failed")
            null
        }
    }
'''
new = '''    suspend fun poTokensRequired(
        videoId: String,
        session: YoutubeGuestSession
    ): YoutubePoTokens {
        require(videoId.isNotBlank() && session.visitorData.isNotBlank()) {
            "PO Token binding and visitor identity are required"
        }
        return tokenGenerator.generate(videoId, session.visitorData, session.generation)
    }

    suspend fun poTokens(videoId: String, session: YoutubeGuestSession): YoutubePoTokens? {
        if (videoId.isBlank() || session.visitorData.isBlank()) return null
        return try {
            poTokensRequired(videoId, session)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Timber.w(error, "PO Token generation failed")
            null
        }
    }
'''
text = replace_once(text, old, new, 'strict PO token path')
write(path, text)


# ---------------------------------------------------------------------------
# Android provider: bounded blocking, cancellation propagation, TV variants.
# ---------------------------------------------------------------------------
path = 'app/src/main/java/com/luc4n3x/levyra/data/LevyraYoutubeSessionPoTokenProvider.kt'
text = read(path)
text = replace_once(
    text,
    '''import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
''',
    '''import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
''',
    'provider coroutine imports',
)
text = replace_once(
    text,
    '''import timber.log.Timber
''',
    '''import timber.log.Timber
import java.io.IOException
''',
    'provider IOException import',
)
text = replace_once(
    text,
    '''        if (clientName.equals("TVHTML5", ignoreCase = true)) return null
        if (Looper.myLooper() == Looper.getMainLooper()) return null

        return try {
            runBlocking(Dispatchers.IO) {
                val session = security.currentSession()
                val tokens = security.poTokens(session.visitorData, session) ?: return@runBlocking null
                tokens.playerToken
                    .takeIf(String::isNotBlank)
                    ?.let { YoutubeSessionPoToken(session.visitorData, it) }
            }
        } catch (error: Exception) {
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
''',
    '''        if (clientName.startsWith("TVHTML5", ignoreCase = true)) return null
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
        const val PROVIDER_TIMEOUT_MS = 15_000L
    }
}
''',
    'provider bounded token flow',
)
write(path, text)


# ---------------------------------------------------------------------------
# NewPipe runtime: real OkHttp streaming and per-call timeout enforcement.
# Also block redirect leakage of sensitive pot query parameters.
# ---------------------------------------------------------------------------
path = 'app/src/main/java/com/luc4n3x/levyra/data/NewPipeRuntime.kt'
text = read(path)
text = replace_once(
    text,
    '''import org.schabi.newpipe.extractor.downloader.Response
''',
    '''import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.downloader.StreamingResponse
''',
    'streaming response import',
)
text = replace_once(
    text,
    '''import java.io.IOException
''',
    '''import java.io.ByteArrayInputStream
import java.io.IOException
''',
    'streaming byte input import',
)
text = replace_once(
    text,
    '''        .callTimeout(35, TimeUnit.SECONDS)
        .build()
''',
    '''        .callTimeout(35, TimeUnit.SECONDS)
        .addNetworkInterceptor { chain ->
            validateSensitiveTokenTarget(chain.request())
            chain.proceed(chain.request())
        }
        .build()
''',
    'sensitive redirect interceptor',
)
insert = '''
    override fun supportsStreamingResponses(): Boolean = true

    override fun getStreaming(
        url: String,
        headers: Map<String, List<String>>?,
        localization: Localization?
    ): StreamingResponse = executeStreaming(
        Request.newBuilder()
            .get(url)
            .headers(headers)
            .localization(localization)
            .build(),
        client
    )

    override fun getStreaming(
        url: String,
        headers: Map<String, List<String>>?,
        localization: Localization?,
        timeoutMs: Long
    ): StreamingResponse {
        require(timeoutMs > 0L) { "timeoutMs must be positive" }
        val timeoutClient = client.newBuilder()
            .callTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .build()
        return executeStreaming(
            Request.newBuilder()
                .get(url)
                .headers(headers)
                .localization(localization)
                .build(),
            timeoutClient
        )
    }

    override fun postStreaming(
        url: String,
        headers: Map<String, List<String>>?,
        dataToSend: ByteArray?,
        localization: Localization?
    ): StreamingResponse = executeStreaming(
        Request.newBuilder()
            .post(url, dataToSend)
            .headers(headers)
            .localization(localization)
            .build(),
        client
    )

'''
text = replace_once(
    text,
    '''    override fun execute(request: Request): Response {
''',
    insert + '''    override fun execute(request: Request): Response {
''',
    'streaming downloader overrides',
)
helper_marker = '''    private fun toOkHttpRequest(request: Request): okhttp3.Request {
'''
helper = '''    private fun executeStreaming(
        request: Request,
        httpClient: okhttp3.OkHttpClient
    ): StreamingResponse {
        val response = httpClient.newCall(toOkHttpRequest(request)).execute()
        if (response.code == 429) {
            response.close()
            throw IOException("YouTube ha limitato temporaneamente le richieste")
        }
        val responseBody = response.body
        if (responseBody == null) {
            val headers = response.headers.toMultimap()
            val code = response.code
            response.close()
            return StreamingResponse(code, headers, ByteArrayInputStream(ByteArray(0)))
        }
        return object : StreamingResponse(
            response.code,
            response.headers.toMultimap(),
            responseBody.byteStream()
        ) {
            override fun close() {
                response.close()
            }
        }
    }

    private fun validateSensitiveTokenTarget(request: okhttp3.Request) {
        if (request.url.queryParameter("pot").isNullOrBlank()) return
        val host = request.url.host.lowercase()
        if (!request.url.isHttps ||
            !(host == "googlevideo.com" || host.endsWith(".googlevideo.com"))
        ) {
            throw IOException("Blocked sensitive YouTube token redirect outside GoogleVideo")
        }
    }

'''
text = replace_once(text, helper_marker, helper + helper_marker, 'streaming downloader helpers')
write(path, text)


# ---------------------------------------------------------------------------
# Install NewPipe/provider synchronously during Application startup.
# ---------------------------------------------------------------------------
path = 'app/src/main/java/com/luc4n3x/levyra/LevyraApplication.kt'
text = read(path)
text = replace_once(
    text,
    '''        YoutubeLocalDecoder.install(this)
        PlaybackNetworkStack.initialize(this)
        warmPlaybackPipeline()
''',
    '''        YoutubeLocalDecoder.install(this)
        PlaybackNetworkStack.initialize(this)
        runCatching { NewPipeRuntime.ensure(this) }
            .onFailure { Timber.w(it, "Extractor initialization failed") }
        warmPlaybackPipeline()
''',
    'synchronous extractor startup',
)
text = replace_once(
    text,
    '''    private fun warmPlaybackPipeline() {
        startupScope.launch {
            runCatching { NewPipeRuntime.ensure(this@LevyraApplication) }
                .onFailure { Timber.w(it, "Extractor warmup failed") }
        }
        startupScope.launch {
''',
    '''    private fun warmPlaybackPipeline() {
        startupScope.launch {
''',
    'remove asynchronous extractor race',
)
write(path, text)


# ---------------------------------------------------------------------------
# Player request token preparation: identity preservation, request-country binding,
# cancellation propagation, TVHTML5 family bypass and library logging.
# ---------------------------------------------------------------------------
path = 'third_party/LevyraExtractor/extractor/src/main/java/org/schabi/newpipe/extractor/services/youtube/YoutubeParsingHelper.java'
text = read(path)
text = replace_once(
    text,
    '''import java.util.concurrent.TimeUnit;
''',
    '''import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
''',
    'YoutubeParsingHelper logging imports',
)
text = replace_once(
    text,
    '''public final class YoutubeParsingHelper {
''',
    '''public final class YoutubeParsingHelper {
    private static final Logger LOGGER = Logger.getLogger(YoutubeParsingHelper.class.getName());
    private static final String PLAYER_ENDPOINT = "player";
''',
    'YoutubeParsingHelper logger constants',
)
text = text.replace('"player".equals(endpoint)', 'PLAYER_ENDPOINT.equals(endpoint)')
method_pattern = r'''    @Nonnull
    public static YoutubePlayerRequest prepareSessionPoTokenPlayerRequest\(
            @Nonnull final byte\[] body,
            @Nonnull final Localization localization,
            @Nonnull final ContentCountry contentCountry\) \{.*?
    \}

    @Nullable
    public static YoutubeSessionPoToken getSessionPoToken\('''
method_replacement = '''    @Nonnull
    public static YoutubePlayerRequest prepareSessionPoTokenPlayerRequest(
            @Nonnull final byte[] body,
            @Nonnull final Localization localization,
            @Nonnull final ContentCountry contentCountry) {
        String originalVisitorData = null;
        String clientVersion = null;
        try {
            final JsonObject request = JsonUtils.toJsonObject(new String(body,
                    StandardCharsets.UTF_8));
            final JsonObject context = request.getObject("context");
            final JsonObject client = context == null ? null : context.getObject("client");
            if (client == null) {
                return new YoutubePlayerRequest(body, null, null);
            }
            originalVisitorData = client.getString("visitorData");
            final String clientName = client.getString("clientName", "");
            clientVersion = client.getString("clientVersion", "");
            final String userAgent = client.getString("userAgent");
            if (clientName.regionMatches(true, 0, "TVHTML5", 0, "TVHTML5".length())) {
                return new YoutubePlayerRequest(body, originalVisitorData, clientVersion);
            }
            final JsonObject existingIntegrity = request.getObject("serviceIntegrityDimensions");
            final String existingPoToken = existingIntegrity == null
                    ? null : existingIntegrity.getString("poToken");
            if (existingIntegrity != null && !isNullOrEmpty(existingPoToken)) {
                return new YoutubePlayerRequest(body, originalVisitorData, clientVersion);
            }
            if (isNullOrEmpty(clientName) || isNullOrEmpty(clientVersion)) {
                return new YoutubePlayerRequest(body, originalVisitorData, clientVersion);
            }

            ContentCountry requestCountry = contentCountry;
            final String requestCountryCode = client.getString("gl");
            if (!isNullOrEmpty(requestCountryCode)) {
                requestCountry = new ContentCountry(requestCountryCode);
            }
            final YoutubeSessionPoToken result = getSessionPoToken(clientName, clientVersion,
                    userAgent, localization, requestCountry);
            if (result == null || isNullOrEmpty(result.getVisitorData())
                    || isNullOrEmpty(result.getPoToken())) {
                return new YoutubePlayerRequest(body, originalVisitorData, clientVersion);
            }

            client.put("visitorData", result.getVisitorData());
            final JsonObject integrity = existingIntegrity == null
                    ? new JsonObject() : existingIntegrity;
            integrity.put("poToken", result.getPoToken());
            request.put("serviceIntegrityDimensions", integrity);
            return new YoutubePlayerRequest(
                    JsonWriter.string(request).getBytes(StandardCharsets.UTF_8),
                    result.getVisitorData(), clientVersion);
        } catch (final CancellationException error) {
            throw error;
        } catch (final Exception error) {
            LOGGER.log(Level.FINE, "Could not add visitor-bound YouTube PO token", error);
            return new YoutubePlayerRequest(body, originalVisitorData, clientVersion);
        }
    }

    @Nullable
    public static YoutubeSessionPoToken getSessionPoToken('''
text = regex_once(text, method_pattern, method_replacement,
                  'rewrite player request token preparation', flags=re.S)
text = replace_once(
    text,
    '''        try {
            return provider.getSessionPoToken(clientName, clientVersion, userAgent,
                    localization, contentCountry, ServiceList.YouTube.hasTokens());
        } catch (final Exception error) {
            System.err.println("Could not obtain visitor-bound YouTube PO token: "
                    + error.getClass().getSimpleName() + ": " + error.getMessage());
            return null;
        }
''',
    '''        try {
            return provider.getSessionPoToken(clientName, clientVersion, userAgent,
                    localization, contentCountry, ServiceList.YouTube.hasTokens());
        } catch (final CancellationException error) {
            throw error;
        } catch (final Exception error) {
            LOGGER.log(Level.FINE, "Could not obtain visitor-bound YouTube PO token", error);
            return null;
        }
''',
    'provider cancellation and logging',
)
text = replace_once(
    text,
    '''        Localization localization = new Localization("en");
        final byte[] body = JsonWriter.string(
                        prepareDesktopJsonBuilder(localization, ContentCountry.DEFAULT)
''',
    '''        final Localization localization = new Localization("en");
        final ContentCountry contentCountry = NewPipe.getPreferredContentCountry();
        final byte[] body = JsonWriter.string(
                        prepareDesktopJsonBuilder(localization, contentCountry)
''',
    'sync web request country',
)
text = replace_once(
    text,
    '''                addSessionPoTokenToPlayerBody(body, localization, ContentCountry.DEFAULT),
''',
    '''                addSessionPoTokenToPlayerBody(body, localization, contentCountry),
''',
    'sync web token country',
)
write(path, text)


# ---------------------------------------------------------------------------
# Android Auto search: shared Deferred, bounded LRU cache, lifecycle and matching.
# ---------------------------------------------------------------------------
path = 'app/src/main/java/com/luc4n3x/levyra/player/AndroidAutoLibrary.kt'
text = read(path)
text = replace_once(
    text,
    '''import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
''',
    '''import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext
''',
    'Android Auto coroutine imports',
)
text = replace_once(
    text,
    '''import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
''',
    '''import java.util.LinkedHashMap
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
''',
    'Android Auto cache imports',
)
text = replace_once(
    text,
    '''    private val searchCache = ConcurrentHashMap<String, TimedTracks>()
    private val searchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val searchInFlight = ConcurrentHashMap<String, Job>()
''',
    '''    private val searchCacheLock = Any()
    private val searchCache = object : LinkedHashMap<String, TimedTracks>(
        MAX_SEARCH_CACHE_ENTRIES,
        0.75f,
        true
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, TimedTracks>?
        ): Boolean = size > MAX_SEARCH_CACHE_ENTRIES
    }
    private val searchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val searchInFlight = ConcurrentHashMap<String, Deferred<List<Track>>>()
''',
    'Android Auto bounded search fields',
)
old = '''    fun preloadSearch(query: String) {
        val clean = query.voiceSearchQuery()
        if (clean.isBlank()) return
        val now = System.currentTimeMillis()
        if (searchCache[clean]?.let { now - it.createdAt < SEARCH_TTL_MS } == true) return

        val job = searchScope.launch(start = CoroutineStart.LAZY) {
            try {
                searchTracks(clean)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.d(error, "Android Auto search preload failed")
            } finally {
                searchInFlight.remove(clean)
            }
        }
        if (searchInFlight.putIfAbsent(clean, job) == null) {
            job.start()
        } else {
            job.cancel()
        }
    }

    suspend fun search(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val clean = query.voiceSearchQuery()
        if (clean.isBlank()) return@withContext flowTracks().map { trackItem(it) }
        searchTracks(clean).map { trackItem(it) }
    }
'''
new = '''    fun preloadSearch(query: String) {
        val clean = query.voiceSearchQuery()
        if (clean.isBlank() || cachedSearch(clean) != null) return
        startOrJoinSearch(clean).invokeOnCompletion { error ->
            if (error != null && error !is CancellationException) {
                Timber.d(error, "Android Auto search preload failed")
            }
        }
    }

    suspend fun search(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        val clean = query.voiceSearchQuery()
        if (clean.isBlank()) return@withContext flowTracks().map { trackItem(it) }
        val tracks = cachedSearch(clean) ?: startOrJoinSearch(clean).await()
        tracks.map { trackItem(it) }
    }

    fun close() {
        searchInFlight.values.forEach { it.cancel() }
        searchInFlight.clear()
        searchScope.cancel()
        synchronized(searchCacheLock) { searchCache.clear() }
    }

    private fun startOrJoinSearch(query: String): Deferred<List<Track>> {
        searchInFlight[query]?.let { return it }
        val deferred = searchScope.async(start = CoroutineStart.LAZY) {
            searchTracks(query)
        }
        deferred.invokeOnCompletion {
            searchInFlight.remove(query, deferred)
        }
        val existing = searchInFlight.putIfAbsent(query, deferred)
        if (existing != null) {
            deferred.cancel()
            return existing
        }
        deferred.start()
        return deferred
    }
'''
text = replace_once(text, old, new, 'Android Auto shared search flow')
old = '''    private suspend fun searchTracks(query: String): List<Track> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        searchCache[query]?.takeIf { now - it.createdAt < SEARCH_TTL_MS }?.tracks?.let { return@withContext it }
        val local = searchLocal(query)
        val languageCode = contentLanguage()
        val remote = runCatching { musicRepository.search(query, 30, languageCode) }.getOrDefault(emptyList())
        val result = (local + remote).distinctTracks().take(MAX_FOLDER_TRACKS)
        searchCache[query] = TimedTracks(result, now)
        result
    }
'''
new = '''    private suspend fun searchTracks(query: String): List<Track> = withContext(Dispatchers.IO) {
        cachedSearch(query)?.let { return@withContext it }
        val local = searchLocal(query)
        val languageCode = contentLanguage()
        val remote = try {
            musicRepository.search(query, 30, languageCode)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Timber.w(error, "Android Auto remote search failed")
            return@withContext local.distinctTracks().take(MAX_FOLDER_TRACKS)
        }
        val result = (local + remote).distinctTracks().take(MAX_FOLDER_TRACKS)
        cacheSearch(query, result)
        result
    }

    private fun cachedSearch(query: String): List<Track>? = synchronized(searchCacheLock) {
        val now = System.currentTimeMillis()
        val iterator = searchCache.entries.iterator()
        while (iterator.hasNext()) {
            if (now - iterator.next().value.createdAt >= SEARCH_TTL_MS) iterator.remove()
        }
        searchCache[query]?.tracks
    }

    private fun cacheSearch(query: String, tracks: List<Track>) {
        synchronized(searchCacheLock) {
            searchCache[query] = TimedTracks(tracks, System.currentTimeMillis())
        }
    }
'''
text = replace_once(text, old, new, 'Android Auto cache success semantics')
text = replace_once(
    text,
    '''            val searchable = listOf(track.title, track.artist, track.album, track.source)
''',
    '''            val searchable = listOf(track.title, track.artist, track.album)
''',
    'Android Auto exclude source from search',
)
text = replace_once(
    text,
    '''                artist == needle -> 3
                title.contains(needle) -> 2
                else -> 1
''',
    '''                artist == needle -> 3
                title.contains(needle) -> 2
                artist.contains(needle) -> 2
                track.album.lowercase(Locale.ROOT).contains(needle) -> 1
                else -> 0
''',
    'Android Auto ranking buckets',
)
old = '''    private fun String.voiceSearchQuery(): String {
        var value = cleanQuery()
        VOICE_COMMAND_PREFIXES.firstOrNull { value.startsWith(it, ignoreCase = true) }
            ?.let { prefix -> value = value.drop(prefix.length).trim() }
        val lowered = value.lowercase(Locale.ROOT)
        VOICE_APP_SUFFIXES.firstOrNull(lowered::endsWith)
            ?.let { suffix -> value = value.dropLast(suffix.length).trim() }
        return value.cleanQuery()
    }
'''
new = '''    private fun String.voiceSearchQuery(): String {
        var value = cleanQuery()
        while (true) {
            val prefix = VOICE_COMMAND_PREFIXES.firstOrNull {
                value.startsWith(it, ignoreCase = true)
            } ?: break
            value = value.drop(prefix.length).trim()
        }
        while (true) {
            val lowered = value.lowercase(Locale.ROOT)
            val suffix = VOICE_APP_SUFFIXES.firstOrNull(lowered::endsWith) ?: break
            value = value.dropLast(suffix.length).trim()
        }
        return value.cleanQuery()
    }
'''
text = replace_once(text, old, new, 'Android Auto repeated voice normalization')
text = replace_once(
    text,
    '''        .filter { it.length >= 2 && it !in SEARCH_STOP_WORDS }
''',
    '''        .filter { it.isNotEmpty() && it !in SEARCH_STOP_WORDS }
''',
    'Android Auto one-character artist tokens',
)
text = replace_once(
    text,
    '''        private const val SEARCH_TTL_MS = 3L * 60L * 1000L
        private val VOICE_COMMAND_PREFIXES = listOf(
            "riproduci ", "suona ", "metti ", "ascolta ", "cerca ", "play "
        )
        private val VOICE_APP_SUFFIXES = listOf(" su levyra", " in levyra")
''',
    '''        private const val SEARCH_TTL_MS = 3L * 60L * 1000L
        private const val MAX_SEARCH_CACHE_ENTRIES = 64
        private val VOICE_COMMAND_PREFIXES = listOf(
            "riproduci ", "suona ", "metti ", "ascolta ", "cerca ",
            "play ", "reproduce ", "toca ", "escucha ", "écoute ", "joue ",
            "spiele ", "suche ", "ouvir ", "toque "
        )
        private val VOICE_APP_SUFFIXES = listOf(
            " su levyra", " in levyra", " en levyra", " dans levyra", " mit levyra"
        )
''',
    'Android Auto cache and voice constants',
)
write(path, text)


# ---------------------------------------------------------------------------
# Cancel Android Auto search work with PlaybackService lifecycle.
# ---------------------------------------------------------------------------
path = 'app/src/main/java/com/luc4n3x/levyra/player/PlaybackService.kt'
text = read(path)
text = replace_once(
    text,
    '''        if (activeService === this) activeService = null
        serviceScope.cancel()
''',
    '''        if (activeService === this) activeService = null
        if (::autoLibrary.isInitialized) autoLibrary.close()
        serviceScope.cancel()
''',
    'PlaybackService closes Android Auto library',
)
write(path, text)


# ---------------------------------------------------------------------------
# Token-preparation regression tests.
# ---------------------------------------------------------------------------
path = 'third_party/LevyraExtractor/extractor/src/test/java/org/schabi/newpipe/extractor/services/youtube/YoutubeSessionPoTokenTest.java'
text = read(path)
text = replace_once(
    text,
    '''import java.util.concurrent.atomic.AtomicBoolean;
''',
    '''import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
''',
    'token test cancellation import',
)
text = replace_once(
    text,
    '''import static org.junit.jupiter.api.Assertions.assertTrue;
''',
    '''import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
''',
    'token test assertThrows import',
)
insert_marker = '''    @Test
    void preparedPlayerRequestRetainsTheExactProviderVisitor() throws Exception {
'''
new_tests = '''    @Test
    void providerCancellationPropagates() {
        NewPipe.setYoutubeSessionPoTokenProvider((clientName, clientVersion, userAgent,
                                                   localization, contentCountry, login) -> {
            throw new CancellationException("cancelled");
        });

        assertThrows(CancellationException.class,
                () -> YoutubeParsingHelper.addSessionPoTokenToPlayerBody(
                        playerBody("WEB"), LOCALIZATION, CONTENT_COUNTRY));
    }

    @Test
    void tokenProviderUsesCountryFromRequestClientContext() throws Exception {
        final AtomicBoolean matched = new AtomicBoolean(false);
        NewPipe.setYoutubeSessionPoTokenProvider((clientName, clientVersion, userAgent,
                                                   localization, contentCountry, login) -> {
            matched.set("DE".equals(contentCountry.getCountryCode()));
            return new YoutubeSessionPoToken("visitor", "token");
        });
        final byte[] body = ("{\\"context\\":{\\"client\\":{\\"clientName\\":\\"WEB\\","
                + "\\"clientVersion\\":\\"2.test\\",\\"gl\\":\\"DE\\"}},"
                + "\\"videoId\\":\\"video\\"}").getBytes(StandardCharsets.UTF_8);

        YoutubeParsingHelper.addSessionPoTokenToPlayerBody(body, LOCALIZATION, CONTENT_COUNTRY);

        assertTrue(matched.get());
    }

    @Test
    void allTvHtml5VariantsRemainTokenFree() {
        final AtomicInteger calls = new AtomicInteger();
        NewPipe.setYoutubeSessionPoTokenProvider((clientName, clientVersion, userAgent,
                                                   localization, contentCountry, login) -> {
            calls.incrementAndGet();
            return new YoutubeSessionPoToken("visitor", "token");
        });

        final byte[] body = playerBody("TVHTML5_SIMPLY_EMBEDDED_PLAYER");
        assertArrayEquals(body, YoutubeParsingHelper.addSessionPoTokenToPlayerBody(
                body, LOCALIZATION, CONTENT_COUNTRY));
        assertEquals(0, calls.get());
    }

'''
text = replace_once(text, insert_marker, new_tests + insert_marker,
                    'token preparation regression tests')
write(path, text)

print('Application and extractor bridge patch staged successfully')
