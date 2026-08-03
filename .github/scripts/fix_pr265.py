from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
changed = []


def replace_once(path: str, old: str, new: str) -> None:
    file = ROOT / path
    text = file.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected one match, found {count}: {old[:100]!r}")
    file.write_text(text.replace(old, new, 1), encoding="utf-8")
    if path not in changed:
        changed.append(path)


def create_file(path: str, content: str) -> None:
    file = ROOT / path
    if file.exists():
        raise RuntimeError(f"{path}: file already exists")
    file.parent.mkdir(parents=True, exist_ok=True)
    file.write_text(content, encoding="utf-8")
    changed.append(path)


# PlayerBar: fix the broken layout scope, seed download state, avoid nullable dereferences,
# and keep sleep timer labels/selections aligned with the active flag.
player = "desktop/app/src/main/kotlin/com/luc4n3x/levyra/desktop/app/ui/player/PlayerBar.kt"
replace_once(
    player,
    "import kotlinx.coroutines.flow.map\nimport kotlinx.coroutines.flow.distinctUntilChanged\n",
    "import kotlinx.coroutines.flow.distinctUntilChanged\nimport kotlinx.coroutines.flow.flowOf\nimport kotlinx.coroutines.flow.map\n",
)
replace_once(
    player,
    '''    val downloadRecord by remember(downloadActions, track?.id) {
        if (track != null) {
            downloadActions?.stateFlow?.map { it[track.id] }?.distinctUntilChanged() ?: kotlinx.coroutines.flow.flowOf(null)
        } else {
            kotlinx.coroutines.flow.flowOf(null)
        }
    }.collectAsState(initial = null)
''',
    '''    val recordFlow = remember(downloadActions, track?.id) {
        if (track != null && downloadActions != null) {
            downloadActions.stateFlow.map { it[track.id] }.distinctUntilChanged()
        } else {
            flowOf(null)
        }
    }
    val initialRecord = track?.let { downloadActions?.stateFlow?.value?.get(it.id) }
    val downloadRecord by recordFlow.collectAsState(initial = initialRecord)
''',
)
replace_once(
    player,
    '''                            onClick = {
                                when (downloadRecord?.status) {
                                    DownloadStatus.QUEUED,
                                    DownloadStatus.RESOLVING,
                                    DownloadStatus.DOWNLOADING -> downloadActions.onCancel(downloadRecord.id)
                                    DownloadStatus.FAILED,
                                    DownloadStatus.CANCELLED -> downloadActions.onRetry(downloadRecord.id)
                                    DownloadStatus.COMPLETED -> Unit
                                    null -> downloadActions.onDownload(track)
                                }
                            },
''',
    '''                            onClick = {
                                val record = downloadRecord
                                when (record?.status) {
                                    DownloadStatus.QUEUED,
                                    DownloadStatus.RESOLVING,
                                    DownloadStatus.DOWNLOADING -> downloadActions.onCancel(record.id)
                                    DownloadStatus.FAILED,
                                    DownloadStatus.CANCELLED -> downloadActions.onRetry(record.id)
                                    DownloadStatus.COMPLETED -> Unit
                                    null -> downloadActions.onDownload(track)
                                }
                            },
''',
)
replace_once(
    player,
    '''                        ) {
                            when (downloadRecord?.status) {
                                DownloadStatus.QUEUED,
                                DownloadStatus.RESOLVING,
                                DownloadStatus.DOWNLOADING -> CircularProgressIndicator(
                                    progress = { downloadRecord.progress },
''',
    '''                        ) {
                            val record = downloadRecord
                            when (record?.status) {
                                DownloadStatus.QUEUED,
                                DownloadStatus.RESOLVING,
                                DownloadStatus.DOWNLOADING -> CircularProgressIndicator(
                                    progress = { record.progress },
''',
)
replace_once(
    player,
    '''                    PlayerProgressBar(
                        playbackStateFlow = playbackStateFlow,
                        onSeek = onSeek,
                        accent = accent,
                        trackId = track?.id
                    )

                Row(
''',
    '''                    PlayerProgressBar(
                        playbackStateFlow = playbackStateFlow,
                        onSeek = onSeek,
                        accent = accent,
                        trackId = track?.id
                    )
                }

                Row(
''',
)
replace_once(
    player,
    '''        state.sleepTimer.mode == SleepTimerMode.DURATION -> Format.duration(state.sleepRemainingMs)
        state.sleepTimer.mode == SleepTimerMode.END_OF_TRACK -> strings.sleepTimerEndOfTrack
''',
    '''        state.sleepTimer.active && state.sleepTimer.mode == SleepTimerMode.DURATION ->
            Format.duration(state.sleepRemainingMs)
        state.sleepTimer.active && state.sleepTimer.mode == SleepTimerMode.END_OF_TRACK ->
            strings.sleepTimerEndOfTrack
''',
)
replace_once(
    player,
    "            selected = timer.mode == SleepTimerMode.DURATION && timer.requestedMinutes == minutes,\n",
    "            selected = timer.active && timer.mode == SleepTimerMode.DURATION && timer.requestedMinutes == minutes,\n",
)
replace_once(
    player,
    "        selected = timer.mode == SleepTimerMode.END_OF_TRACK,\n",
    "        selected = timer.active && timer.mode == SleepTimerMode.END_OF_TRACK,\n",
)

# Track rows: seed the StateFlow value, render focus, and bind nullable records locally.
track_row = "desktop/app/src/main/kotlin/com/luc4n3x/levyra/desktop/app/ui/components/TrackRow.kt"
replace_once(
    track_row,
    "import androidx.compose.foundation.interaction.MutableInteractionSource\nimport androidx.compose.foundation.interaction.collectIsHoveredAsState\n",
    "import androidx.compose.foundation.interaction.MutableInteractionSource\nimport androidx.compose.foundation.interaction.collectIsFocusedAsState\nimport androidx.compose.foundation.interaction.collectIsHoveredAsState\n",
)
replace_once(
    track_row,
    "import kotlinx.coroutines.flow.map\nimport kotlinx.coroutines.flow.distinctUntilChanged\n",
    "import kotlinx.coroutines.flow.distinctUntilChanged\nimport kotlinx.coroutines.flow.flowOf\nimport kotlinx.coroutines.flow.map\n",
)
replace_once(
    track_row,
    '''    val downloadRecord by remember(downloadActions, track.id) {
        downloadActions?.stateFlow?.map { it[track.id] }?.distinctUntilChanged() ?: kotlinx.coroutines.flow.flowOf(null)
    }.collectAsState(initial = null)
    val interactionSource = remember(track.id) { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
''',
    '''    val recordFlow = remember(downloadActions, track.id) {
        downloadActions?.stateFlow?.map { it[track.id] }?.distinctUntilChanged() ?: flowOf(null)
    }
    val downloadRecord by recordFlow.collectAsState(
        initial = downloadActions?.stateFlow?.value?.get(track.id)
    )
    val interactionSource = remember(track.id) { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val focused by interactionSource.collectIsFocusedAsState()
''',
)
replace_once(
    track_row,
    "        hovered -> MaterialTheme.colorScheme.surfaceContainerHigh\n",
    "        hovered || focused -> MaterialTheme.colorScheme.surfaceContainerHigh\n",
)
replace_once(
    track_row,
    '''        when (downloadRecord?.status) {
            DownloadStatus.QUEUED,
            DownloadStatus.RESOLVING,
            DownloadStatus.DOWNLOADING -> CircularProgressIndicator(
                progress = { downloadRecord.progress },
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp
            )

            DownloadStatus.COMPLETED -> Icon(
                imageVector = OfflineIcons.Check,
                contentDescription = strings.downloadOfflineBadge,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )

            else -> Unit
        }
''',
    '''        downloadRecord?.let { record ->
            when (record.status) {
                DownloadStatus.QUEUED,
                DownloadStatus.RESOLVING,
                DownloadStatus.DOWNLOADING -> CircularProgressIndicator(
                    progress = { record.progress },
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )

                DownloadStatus.COMPLETED -> Icon(
                    imageVector = OfflineIcons.Check,
                    contentDescription = strings.downloadOfflineBadge,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )

                else -> Unit
            }
        }
''',
)
replace_once(
    track_row,
    '''                if (downloadActions != null) {
                    when (downloadRecord?.status) {
''',
    '''                if (downloadActions != null) {
                    val record = downloadRecord
                    when (record?.status) {
''',
)
replace_once(track_row, "downloadActions.onCancel(downloadRecord.id)", "downloadActions.onCancel(record.id)")
replace_once(track_row, "downloadActions.onDelete(downloadRecord.id)", "downloadActions.onDelete(record.id)")
replace_once(track_row, "downloadActions.onRetry(downloadRecord.id)", "downloadActions.onRetry(record.id)")

# Collection cards and sidebar items need visible keyboard focus states.
collection_card = "desktop/app/src/main/kotlin/com/luc4n3x/levyra/desktop/app/ui/components/CollectionCard.kt"
replace_once(
    collection_card,
    "import androidx.compose.foundation.hoverable\n",
    "import androidx.compose.foundation.hoverable\nimport androidx.compose.foundation.interaction.collectIsFocusedAsState\n",
)
replace_once(
    collection_card,
    "    val (interactionSource, hovered) = rememberHoverState(ref.id)\n",
    "    val (interactionSource, hovered) = rememberHoverState(ref.id)\n    val focused by interactionSource.collectIsFocusedAsState()\n",
)
replace_once(collection_card, "    val targetBackground = if (hovered) {\n", "    val targetBackground = if (hovered || focused) {\n")
replace_once(collection_card, "            if (hovered) {\n", "            if (hovered || focused) {\n")

root = "desktop/app/src/main/kotlin/com/luc4n3x/levyra/desktop/app/ui/LevyraRoot.kt"
replace_once(
    root,
    "import androidx.compose.foundation.interaction.MutableInteractionSource\nimport androidx.compose.foundation.interaction.collectIsHoveredAsState\n",
    "import androidx.compose.foundation.interaction.MutableInteractionSource\nimport androidx.compose.foundation.interaction.collectIsFocusedAsState\nimport androidx.compose.foundation.interaction.collectIsHoveredAsState\n",
)
replace_once(
    root,
    "    val hovered by interactionSource.collectIsHoveredAsState()\n    val targetBackground = when {\n",
    "    val hovered by interactionSource.collectIsHoveredAsState()\n    val focused by interactionSource.collectIsFocusedAsState()\n    val targetBackground = when {\n",
)
replace_once(
    root,
    "        hovered -> Color.White.copy(alpha = LevyraMotion.HOVER_ALPHA)\n",
    "        hovered || focused -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = LevyraMotion.HOVER_ALPHA)\n",
)
replace_once(
    root,
    "        hovered -> MaterialTheme.colorScheme.onSurface\n",
    "        hovered || focused -> MaterialTheme.colorScheme.onSurface\n",
)

# Parallel/serial downloads: cleanup after child completion, bound reads, and block private targets including redirects.
parallel = "desktop/app/src/main/kotlin/com/luc4n3x/levyra/desktop/app/state/DesktopParallelDownloader.kt"
replace_once(
    parallel,
    "    private val client = baseClient.newBuilder()\n",
    "    private val client = baseClient.newBuilder()\n        .dns(PublicAddressDns(baseClient.dns))\n        .addNetworkInterceptor(PublicDownloadUrlInterceptor)\n",
)
replace_once(parallel, "    ) = coroutineScope {\n", "    ) {\n")
replace_once(
    parallel,
    '''        try {
            ranges.map { range ->
                async(Dispatchers.IO) {
                    limiter.withPermit {
                        downloadRangeWithRetry(url, range, output)
                        onProgress(downloadedBytes.addAndGet(range.length).coerceAtMost(totalBytes))
                    }
                }
            }.awaitAll()
            if (downloadedBytes.get() != totalBytes || Files.size(output) != totalBytes) {
                throw IOException("Download parallelo incompleto: ${downloadedBytes.get()}/$totalBytes byte")
            }
        } catch (cancellation: CancellationException) {
            Files.deleteIfExists(output)
            throw cancellation
        } catch (error: Throwable) {
            Files.deleteIfExists(output)
            throw error
        }
''',
    '''        try {
            coroutineScope {
                ranges.map { range ->
                    async(Dispatchers.IO) {
                        limiter.withPermit {
                            downloadRangeWithRetry(url, range, output)
                            onProgress(downloadedBytes.addAndGet(range.length).coerceAtMost(totalBytes))
                        }
                    }
                }.awaitAll()
            }
            if (downloadedBytes.get() != totalBytes || Files.size(output) != totalBytes) {
                throw IOException("Download parallelo incompleto: ${downloadedBytes.get()}/$totalBytes byte")
            }
        } catch (error: Throwable) {
            runCatching { Files.deleteIfExists(output) }
            throw error
        }
''',
)
replace_once(
    parallel,
    '''        val rangeUrl = desktopRangeUrl(normalizedUrl, range)
        val rangeParamApplied = rangeUrl != normalizedUrl
        val request = Request.Builder()
            .url(rangeUrl)
''',
    '''        val rangeUrl = desktopRangeUrl(normalizedUrl, range)
        val rangeParamApplied = rangeUrl != normalizedUrl
        val safeRangeUrl = requirePublicDownloadUrl(rangeUrl)
        val request = Request.Builder()
            .url(safeRangeUrl)
''',
)

offline = "desktop/app/src/main/kotlin/com/luc4n3x/levyra/desktop/app/state/OfflineDownloadController.kt"
replace_once(
    offline,
    '''    private val client = baseClient.newBuilder()
        .callTimeout(0L, TimeUnit.MILLISECONDS)
        .readTimeout(0L, TimeUnit.MILLISECONDS)
        .build()
''',
    '''    private val client = baseClient.newBuilder()
        .dns(PublicAddressDns(baseClient.dns))
        .addNetworkInterceptor(PublicDownloadUrlInterceptor)
        .callTimeout(0L, TimeUnit.MILLISECONDS)
        .readTimeout(60L, TimeUnit.SECONDS)
        .build()
''',
)
replace_once(
    offline,
    '''        var resumeOffset = resumedBytes
        val requestBuilder = Request.Builder()
            .url(stripDesktopAudioRangeParameter(url))
''',
    '''        var resumeOffset = resumedBytes
        val safeUrl = requirePublicDownloadUrl(stripDesktopAudioRangeParameter(url))
        val requestBuilder = Request.Builder()
            .url(safeUrl)
''',
)

create_file(
    "desktop/app/src/main/kotlin/com/luc4n3x/levyra/desktop/app/state/PublicDownloadUrlPolicy.kt",
    '''package com.luc4n3x.levyra.desktop.app.state

import java.io.IOException
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.Locale
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

internal fun requirePublicDownloadUrl(url: String): String {
    val parsed = url.toHttpUrlOrNull() ?: throw IOException("URL di download non valida")
    validatePublicDownloadUrl(parsed)
    return parsed.toString()
}

internal class PublicAddressDns(private val delegate: Dns) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val addresses = delegate.lookup(hostname)
        if (addresses.isEmpty() || addresses.any(::isBlockedAddress)) {
            throw UnknownHostException("Destinazione di download non consentita: $hostname")
        }
        return addresses
    }
}

internal object PublicDownloadUrlInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        validatePublicDownloadUrl(chain.request().url)
        return chain.proceed(chain.request())
    }
}

private fun validatePublicDownloadUrl(url: HttpUrl) {
    if (url.scheme != "http" && url.scheme != "https") {
        throw IOException("Protocollo di download non consentito")
    }
    val host = url.host.lowercase(Locale.ROOT)
    if (host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local")) {
        throw IOException("Destinazione di download locale non consentita")
    }
    if (isIpLiteral(host)) {
        val address = runCatching { InetAddress.getByName(host) }.getOrNull()
            ?: throw IOException("Indirizzo di download non valido")
        if (isBlockedAddress(address)) {
            throw IOException("Destinazione di download privata non consentita")
        }
    }
}

private fun isIpLiteral(host: String): Boolean =
    host.contains(':') || host.all { it.isDigit() || it == '.' }

private fun isBlockedAddress(address: InetAddress): Boolean {
    if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress ||
        address.isSiteLocalAddress || address.isMulticastAddress
    ) {
        return true
    }
    return when (address) {
        is Inet4Address -> {
            val bytes = address.address.map { it.toInt() and 0xff }
            (bytes[0] == 100 && bytes[1] in 64..127) ||
                (bytes[0] == 198 && bytes[1] in 18..19) ||
                bytes[0] == 0 || bytes[0] >= 224
        }
        is Inet6Address -> {
            val first = address.address[0].toInt() and 0xff
            (first and 0xfe) == 0xfc || first == 0xff
        }
        else -> true
    }
}
''',
)

# Cancellation-aware artist fallbacks.
catalog = "desktop/core/src/main/kotlin/com/luc4n3x/levyra/desktop/core/catalog/CatalogRepository.kt"
replace_once(
    catalog,
    "import kotlinx.coroutines.CoroutineDispatcher\n",
    "import kotlinx.coroutines.CancellationException\nimport kotlinx.coroutines.CoroutineDispatcher\n",
)
replace_once(
    catalog,
    '''    private suspend fun fallbackArtistTracks(name: String): List<Track> {
        val page = runCatching { search(name, SearchFilter.SONGS) }.getOrDefault(CatalogPage())
        val matching = page.tracks.filter { artistLabelMatches(it.artist, name) }
        return matching.ifEmpty { page.tracks }.take(MAX_ARTIST_TRACKS)
    }

    private suspend fun fallbackArtistAlbums(name: String): List<CollectionRef> {
        val page = runCatching { search(name, SearchFilter.ALBUMS) }.getOrDefault(CatalogPage())
        val matching = page.collections.filter { artistLabelMatches(it.subtitle, name) }
        return matching.ifEmpty { page.collections }
            .map { it.copy(kind = CollectionKind.ALBUM) }
            .take(MAX_ARTIST_ALBUMS)
    }
''',
    '''    private suspend fun fallbackArtistTracks(name: String): List<Track> {
        val page = searchOrEmpty(name, SearchFilter.SONGS)
        val matching = page.tracks.filter { artistLabelMatches(it.artist, name) }
        return matching.ifEmpty { page.tracks }.take(MAX_ARTIST_TRACKS)
    }

    private suspend fun fallbackArtistAlbums(name: String): List<CollectionRef> {
        val page = searchOrEmpty(name, SearchFilter.ALBUMS)
        val matching = page.collections.filter { artistLabelMatches(it.subtitle, name) }
        return matching.ifEmpty { page.collections }
            .map { it.copy(kind = CollectionKind.ALBUM) }
            .take(MAX_ARTIST_ALBUMS)
    }

    private suspend fun searchOrEmpty(name: String, filter: SearchFilter): CatalogPage = try {
        search(name, filter)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        CatalogPage()
    }
''',
)

# VLC: report transcoder timeouts as failures, close the file before validation, and avoid sout persistence.
vlc = "desktop/player/src/main/kotlin/com/luc4n3x/levyra/desktop/player/VlcMp3Transcoder.kt"
replace_once(vlc, "import kotlinx.coroutines.withTimeout\n", "import kotlinx.coroutines.withTimeoutOrNull\n")
replace_once(
    vlc,
    "            val succeeded = AtomicBoolean(false)\n",
    "            val succeeded = AtomicBoolean(false)\n            val released = AtomicBoolean(false)\n",
)
replace_once(
    vlc,
    '''            player.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
                override fun finished(mediaPlayer: MediaPlayer) {
                    completion.complete(Unit)
                }

                override fun error(mediaPlayer: MediaPlayer) {
                    completion.completeExceptionally(IOException("Conversione MP3 interrotta da VLC"))
                }
            })

            try {
''',
    '''            player.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
                override fun finished(mediaPlayer: MediaPlayer) {
                    completion.complete(Unit)
                }

                override fun error(mediaPlayer: MediaPlayer) {
                    completion.completeExceptionally(IOException("Conversione MP3 interrotta da VLC"))
                }
            })

            fun releaseResources() {
                if (released.compareAndSet(false, true)) {
                    runCatching { player.controls().stop() }
                    runCatching { player.release() }
                    runCatching { factory.release() }
                }
            }

            try {
''',
)
replace_once(
    vlc,
    '''                withTimeout(TimeUnit.MINUTES.toMillis(TRANSCODE_TIMEOUT_MINUTES)) {
                    completion.await()
                }
                if (!Files.isRegularFile(target) || Files.size(target) <= 0L) {
''',
    '''                val finished = withTimeoutOrNull(TimeUnit.MINUTES.toMillis(TRANSCODE_TIMEOUT_MINUTES)) {
                    completion.await()
                    true
                } ?: false
                if (!finished) {
                    throw IOException("Conversione MP3 scaduta dopo $TRANSCODE_TIMEOUT_MINUTES minuti")
                }
                releaseResources()
                if (!Files.isRegularFile(target) || Files.size(target) <= 0L) {
''',
)
replace_once(
    vlc,
    '''            } finally {
                runCatching { player.controls().stop() }
                runCatching { player.release() }
                runCatching { factory.release() }
                if (!succeeded.get()) {
''',
    '''            } finally {
                releaseResources()
                if (!succeeded.get()) {
''',
)
replace_once(
    vlc,
    '''        ":no-video",
        ":sout=#transcode{vcodec=none,acodec=mp3,ab=$safeBitrate,channels=2,samplerate=44100}:std{access=file,mux=raw,dst='$destination'}",
        ":sout-keep"
''',
    '''        ":no-video",
        ":sout=#transcode{vcodec=none,acodec=mp3,ab=$safeBitrate,channels=2,samplerate=44100}:std{access=file,mux=raw,dst='$destination'}"
''',
)

# Accessibility/localization and disabled hover feedback.
artist = "desktop/app/src/main/kotlin/com/luc4n3x/levyra/desktop/app/ui/screens/ArtistScreen.kt"
replace_once(artist, "                        contentDescription = null,\n                        modifier = Modifier.size(21.dp)\n", "                        contentDescription = strings.onboardingBack,\n                        modifier = Modifier.size(21.dp)\n")
replace_once(
    artist,
    "                            text = formatArtistCount(artist.subscriberCount),\n",
    "                            text = \"${formatArtistCount(artist.subscriberCount)} ${strings.artistSubscribers}\",\n",
)
replace_once(
    artist,
    '''private fun ArtistBackRow(title: String, onBack: () -> Unit) {
    Row(
''',
    '''private fun ArtistBackRow(title: String, onBack: () -> Unit) {
    val strings = LocalStrings.current
    Row(
''',
)
replace_once(artist, "                contentDescription = null,\n                modifier = Modifier.size(20.dp)\n", "                contentDescription = strings.onboardingBack,\n                modifier = Modifier.size(20.dp)\n")

strings = "desktop/app/src/main/kotlin/com/luc4n3x/levyra/desktop/app/ui/i18n/Strings.kt"
replace_once(strings, "    val miniPlayer: String,\n    val cancel: String,\n", "    val miniPlayer: String,\n    val artistSubscribers: String,\n    val cancel: String,\n")
replace_once(strings, "        miniPlayer = extras.miniPlayer,\n        cancel = shared.cancel,\n", "        miniPlayer = extras.miniPlayer,\n        artistSubscribers = localizedSubscriberLabel(language.tag),\n        cancel = shared.cancel,\n")
replace_once(
    strings,
    "}\n\nval LocalStrings = staticCompositionLocalOf { stringsFor(AppLanguage.ENGLISH) }\n",
    '''}

private fun localizedSubscriberLabel(tag: String): String = when (tag.substringBefore('-').lowercase()) {
    "it" -> "iscritti"
    "es" -> "suscriptores"
    "fr" -> "abonnés"
    "de" -> "Abonnenten"
    "pt" -> "inscritos"
    "ru" -> "подписчиков"
    "tr" -> "abone"
    "ar" -> "مشترك"
    "hi" -> "सब्सक्राइबर"
    "ja" -> "登録者"
    "ko" -> "구독자"
    "zh" -> "订阅者"
    else -> "subscribers"
}

val LocalStrings = staticCompositionLocalOf { stringsFor(AppLanguage.ENGLISH) }
''',
)

home = "desktop/app/src/main/kotlin/com/luc4n3x/levyra/desktop/app/ui/screens/HomeScreen.kt"
replace_once(home, "            .hoverScale()\n            .height(118.dp)\n", "            .then(if (enabled) Modifier.hoverScale() else Modifier)\n            .height(118.dp)\n")

print("Patched files:")
for path in changed:
    print(f"- {path}")
