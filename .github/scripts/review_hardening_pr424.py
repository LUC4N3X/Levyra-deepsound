from pathlib import Path
import re


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text(encoding="utf-8")
    if old not in text:
        raise SystemExit(f"{label}: expected block not found in {path}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def patch_android_player() -> None:
    path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
    text = path.read_text(encoding="utf-8")

    import_anchor = "import coil3.request.CachePolicy\nimport coil3.request.ImageRequest\nimport coil3.request.crossfade\n"
    import_replacement = (
        "import coil3.request.CachePolicy\n"
        "import coil3.request.ImageRequest\n"
        "import coil3.request.allowHardware\n"
        "import coil3.request.bitmapConfig\n"
        "import coil3.request.crossfade\n"
    )
    if import_anchor not in text:
        raise SystemExit("android player: Coil import anchor missing")
    text = text.replace(import_anchor, import_replacement, 1)

    old_palette = '''            val request = ImageRequest.Builder(playerContext)
                .data(LevyraArtworkCache.small(artUrl))
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build()
            val bitmap = runCatching {
                imageLoader.execute(request).image?.toBitmap()
            }.getOrNull()
            if (bitmap != null) {
                val extracted = withContext(Dispatchers.Default) {
                    val sample = if (bitmap.width > 96 || bitmap.height > 96) {
                        android.graphics.Bitmap.createScaledBitmap(bitmap, 96, 96, true)
                    } else bitmap
                    val paletteBitmap = if (sample.config == android.graphics.Bitmap.Config.ARGB_8888) {
                        sample
                    } else {
                        sample.copy(android.graphics.Bitmap.Config.ARGB_8888, false) ?: sample
                    }
                    try {
                        ArtworkPaletteCache.extract(
                            bitmap = paletteBitmap,
                            fallbackStart = fallbackPalette.start,
                            fallbackEnd = fallbackPalette.end
                        )
                    } finally {
                        if (paletteBitmap !== sample) paletteBitmap.recycle()
                        if (sample !== bitmap) sample.recycle()
                        bitmap.recycle()
                    }
                }
                artworkPaletteState.value = extracted
                ArtworkPaletteCache.store(playerContext, paletteKey, extracted)
            }
'''
    new_palette = '''            val request = ImageRequest.Builder(playerContext)
                .data(LevyraArtworkCache.small(artUrl))
                .size(96, 96)
                .allowHardware(false)
                .bitmapConfig(android.graphics.Bitmap.Config.ARGB_8888)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .build()
            val bitmap = runCatching {
                imageLoader.execute(request).image?.toBitmap()
            }.getOrNull()
            if (bitmap != null) {
                // Coil can return the BitmapImage's backing bitmap from toBitmap().
                // Treat the result as borrowed: never recycle a bitmap that the loader/cache may own.
                val extracted = withContext(Dispatchers.Default) {
                    ArtworkPaletteCache.extract(
                        bitmap = bitmap,
                        fallbackStart = fallbackPalette.start,
                        fallbackEnd = fallbackPalette.end
                    )
                }
                artworkPaletteState.value = extracted
                ArtworkPaletteCache.store(playerContext, paletteKey, extracted)
            }
'''
    if old_palette not in text:
        raise SystemExit("android player: palette extraction block missing")
    text = text.replace(old_palette, new_palette, 1)

    old_gate = '''        val immersiveArtworkEnabled = state.motionArtworkEnabled &&
            state.animationsEnabled &&
            playerPane == LevyraPlayerPane.Stacked &&
            !state.isVideoMode &&
            track != null
'''
    new_gate = '''        val immersiveArtworkEnabled = state.motionArtworkEnabled &&
            state.animationsEnabled &&
            playerPane == LevyraPlayerPane.Stacked &&
            !state.isVideoMode &&
            track != null &&
            state.motionArtwork != null
'''
    if old_gate not in text:
        raise SystemExit("android player: immersive gate block missing")
    text = text.replace(old_gate, new_gate, 1)

    backdrop_pattern = re.compile(
        r"@Composable\nprivate fun PlayerImmersiveBackdrop\(.*?\n}\n\n(?=@Composable\nprivate fun PlayerArtworkCanvas\()",
        re.S,
    )
    new_backdrop = r'''@Composable
private fun PlayerImmersiveBackdrop(
    artworkUrl: String,
    ambience: PlayerAmbience,
    isPlaying: Boolean,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val primary = animateColorAsState(
        targetValue = ambience.primary,
        animationSpec = if (animationsEnabled) tween(900, easing = LinearOutSlowInEasing) else snap(),
        label = "player-backdrop-primary"
    )
    val secondary = animateColorAsState(
        targetValue = ambience.secondary,
        animationSpec = if (animationsEnabled) tween(900, easing = LinearOutSlowInEasing) else snap(),
        label = "player-backdrop-secondary"
    )
    val base = animateColorAsState(
        targetValue = ambience.base,
        animationSpec = if (animationsEnabled) tween(900, easing = LinearOutSlowInEasing) else snap(),
        label = "player-backdrop-base"
    )
    val imageAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 0.72f else 0.58f,
        animationSpec = if (animationsEnabled) tween(500, easing = FastOutSlowInEasing) else snap(),
        label = "player-backdrop-image-alpha"
    )
    val ambientMatrix = remember { createPlayerAmbientColorMatrix() }
    val ambientColorFilter = remember(ambientMatrix) { ColorFilter.colorMatrix(ambientMatrix) }

    Box(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize().background(base.value))
        if (artworkUrl.isNotBlank()) {
            AnimatedContent(
                targetState = artworkUrl,
                transitionSpec = {
                    if (animationsEnabled) {
                        fadeIn(tween(440, easing = LinearOutSlowInEasing)) togetherWith
                            fadeOut(tween(300, easing = FastOutSlowInEasing))
                    } else {
                        EnterTransition.None togetherWith ExitTransition.None
                    }
                },
                label = "player-backdrop-artwork"
            ) { url ->
                val ambientImageRequest = remember(context, url) {
                    ImageRequest.Builder(context)
                        .data(LevyraArtworkCache.large(url))
                        .size(512, 512)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .crossfade(false)
                        .build()
                }
                AsyncImage(
                    model = ambientImageRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = ambientColorFilter,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(52.dp)
                        .graphicsLayer {
                            scaleX = 1.12f
                            scaleY = 1.12f
                            alpha = imageAlpha
                        }
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val animatedPrimary = primary.value
                    val animatedSecondary = secondary.value
                    val animatedBase = base.value
                    drawRect(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to animatedBase.copy(alpha = 0.90f),
                                0.18f to animatedPrimary.copy(alpha = 0.26f),
                                0.54f to Color.Transparent,
                                0.78f to animatedSecondary.copy(alpha = 0.22f),
                                1.00f to Color.Black.copy(alpha = 0.88f)
                            )
                        )
                    )
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(
                                animatedPrimary.copy(alpha = 0.20f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.20f, size.height * 0.16f),
                            radius = size.maxDimension * 0.82f
                        )
                    )
                }
        )
    }
}
'''
    text, count = backdrop_pattern.subn(new_backdrop + "\n", text, count=1)
    if count != 1:
        raise SystemExit(f"android player: expected one backdrop block, found {count}")

    path.write_text(text, encoding="utf-8")


def patch_lyrics_share() -> None:
    path = Path("app/src/main/java/com/luc4n3x/levyra/ui/lyrics/LyricsShareCard.kt")
    text = path.read_text(encoding="utf-8")
    old = '''        val cover = LevyraArtworkCache.localFile(context, track, highRes = true)
            ?.takeIf(File::isFile)
            ?.let { file -> runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull() }
        val file = File(directory, "lyrics-${System.currentTimeMillis()}.png")
        val bitmap = render(track, text, cover)
        cover?.recycle()
        val written = runCatching {
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
        }.getOrDefault(false)
        bitmap.recycle()
        if (!written || !file.isFile || file.length() <= 0L) {
            runCatching { file.delete() }
            return@withContext null
        }
'''
    new = '''        val cover = LevyraArtworkCache.localFile(context, track, highRes = true)
            ?.takeIf(File::isFile)
            ?.let { file -> runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull() }
        val file = File(directory, "lyrics-${System.currentTimeMillis()}.png")
        var bitmap: Bitmap? = null
        val written = try {
            bitmap = render(track, text, cover)
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
        } catch (_: Exception) {
            false
        } finally {
            cover?.recycle()
            bitmap?.recycle()
        }
        if (!written || !file.isFile || file.length() <= 0L) {
            runCatching { file.delete() }
            return@withContext null
        }
'''
    if old not in text:
        raise SystemExit("lyrics share: render/write block missing")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def patch_playback_controller() -> None:
    path = Path("desktop/app/src/main/kotlin/com/luc4n3x/levyra/desktop/app/state/PlaybackController.kt")
    text = path.read_text(encoding="utf-8")

    old_imports = '''import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
'''
    new_imports = '''import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
'''
    if old_imports not in text:
        raise SystemExit("playback: import anchor missing")
    text = text.replace(old_imports, new_imports, 1)

    text = text.replace(
        "    private var transitionJob: Job? = null\n    private var prepareJob: Job? = null\n",
        "    private var transitionJob: Job? = null\n    private var transitionEventJob: Job? = null\n    private var prepareJob: Job? = null\n",
        1,
    )

    old_speed = '''                .map { DesktopSettings.normalizeSpeed(it.playbackSpeed) }
                .distinctUntilChanged()
                .collect { speed -> applySpeed(speed) }
'''
    new_speed = '''                .map { DesktopSettings.normalizeSpeed(it.playbackSpeed) }
                .distinctUntilChanged()
                .collect { speed ->
                    applySpeed(speed)
                    companionPlayer?.setSpeed(speed)
                }
'''
    if old_speed not in text:
        raise SystemExit("playback: speed collector missing")
    text = text.replace(old_speed, new_speed, 1)

    old_output = '''                .map { it.audioOutputDeviceId }
                .distinctUntilChanged()
                .collect { deviceId ->
                    outputDeviceMissingState.value = false
                    player?.applyOutputDevice(deviceId)
                    companionPlayer?.applyOutputDevice(deviceId)
                }
'''
    new_output = '''                .map { it.audioOutputDeviceId }
                .distinctUntilChanged()
                .collect { deviceId ->
                    cancelTransition()
                    outputDeviceMissingState.value = false
                    player?.applyOutputDevice(deviceId)
                    companionPlayer?.applyOutputDevice(deviceId)
                }
'''
    if old_output not in text:
        raise SystemExit("playback: output collector missing")
    text = text.replace(old_output, new_output, 1)

    prepare_pattern = re.compile(
        r"    private suspend fun prepareCompanion\(next: Track, transitionMs: Long\) \{.*?\n    }\n\n    private suspend fun resolveHandoffTrack",
        re.S,
    )
    new_prepare = r'''    private suspend fun prepareCompanion(next: Track, transitionMs: Long) {
        val ownerJob = currentCoroutineContext()[Job]
        try {
            val companion = ensureCompanion() ?: return
            val settings = settingsStore.current
            val playable = resolveHandoffTrack(next) ?: return
            val resolved = try {
                resolver.resolve(playable, settings.audioQuality, settings.preferredCodec)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                DesktopDiagnostics.background("handoff resolve of ${next.title}", error)
                return
            }
            if (!companion.prepare(resolved.url, 0L)) return
            currentCoroutineContext().ensureActive()
            companion.setVolume(0)
            companion.setMuted(internalState.value.muted)
            val equalizer = settings.equalizer
            companion.applyEqualizer(equalizer.enabled, equalizer.preamp, equalizer.amps)
            companion.applyOutputDevice(effectiveOutputDeviceId(settings.audioOutputDeviceId))
            companion.setSpeed(DesktopSettings.normalizeSpeed(settings.playbackSpeed))
            val enriched = playable.copy(
                title = resolved.title.ifBlank { playable.title },
                artist = resolved.artist.ifBlank { playable.artist },
                artworkUrl = resolved.artworkUrl.ifBlank { playable.artworkUrl },
                durationMs = if (resolved.durationMs > 0L) resolved.durationMs else playable.durationMs
            )
            val published = synchronized(transitionLock) {
                if (
                    prepareJob !== ownerJob ||
                    transitionActive ||
                    PrefetchPlanner.handoffTrack(internalState.value.queue)?.id != next.id
                ) {
                    false
                } else {
                    preparedTrackId = next.id
                    preparedTransitionMs = transitionMs
                    preparedStreamLabel = resolved.label
                    true
                }
            }
            if (!published) {
                runCatching { companion.stop() }
                return
            }
            updateTrackMetadata(enriched)
        } finally {
            synchronized(transitionLock) {
                if (prepareJob === ownerJob) prepareJob = null
            }
        }
    }

    private suspend fun resolveHandoffTrack'''
    text, count = prepare_pattern.subn(new_prepare, text, count=1)
    if count != 1:
        raise SystemExit(f"playback: expected one prepareCompanion block, found {count}")

    transition_pattern = re.compile(
        r"    private suspend fun runTransition\(transitionMs: Long\) \{.*?\n    }\n\n    private fun abortTransition",
        re.S,
    )
    new_transition = r'''    private suspend fun runTransition(transitionMs: Long) {
        val companion = companionPlayer
        val outgoing = player
        if (companion == null) {
            synchronized(transitionLock) { transitionActive = false }
            return
        }
        val incomingFailure = AtomicReference<String?>(null)
        val incomingFinished = AtomicBoolean(false)
        transitionEventJob?.cancel()
        transitionEventJob = playerScope.launch(start = CoroutineStart.UNDISPATCHED) {
            companion.events.collect { event ->
                when (event) {
                    is PlayerEvent.Failed -> incomingFailure.compareAndSet(null, event.reason)
                    is PlayerEvent.Finished -> incomingFinished.set(true)
                    else -> Unit
                }
            }
        }
        companion.setMuted(internalState.value.muted)
        companion.setVolume(if (transitionMs > 0L) 0 else internalState.value.volume)
        if (!companion.startPrepared()) {
            transitionEventJob?.cancel()
            transitionEventJob = null
            abortTransition()
            if (transitionMs <= 0L) {
                next(automatic = true)
            }
            return
        }
        if (transitionMs > 0L) {
            val steps = (transitionMs / CrossfadePlanner.STEP_MS).toInt().coerceAtLeast(1)
            for (step in 1..steps) {
                val failureReason = incomingFailure.get()
                if (failureReason != null || incomingFinished.get()) {
                    transitionEventJob?.cancel()
                    transitionEventJob = null
                    abortTransition()
                    messageFlow.tryEmit(failureReason ?: "Il brano successivo si è chiuso durante la transizione")
                    return
                }
                val fraction = step.toFloat() / steps.toFloat()
                val base = internalState.value.volume
                outgoing?.setVolume(
                    CrossfadePlanner.volumeFor(base, CrossfadePlanner.outgoingGain(fraction))
                )
                companion.setVolume(
                    CrossfadePlanner.volumeFor(base, CrossfadePlanner.incomingGain(fraction))
                )
                delay(CrossfadePlanner.STEP_MS)
            }
        }
        val failureReason = incomingFailure.get()
        if (failureReason != null || incomingFinished.get()) {
            transitionEventJob?.cancel()
            transitionEventJob = null
            abortTransition()
            messageFlow.tryEmit(failureReason ?: "Il brano successivo si è chiuso durante la transizione")
            return
        }
        completeTransition()
    }

    private fun abortTransition'''
    text, count = transition_pattern.subn(new_transition, text, count=1)
    if count != 1:
        raise SystemExit(f"playback: expected one runTransition block, found {count}")

    old_observe = '''    private fun observeEvents(target: AudioPlayer) {
        eventJob?.cancel()
        eventJob = playerScope.launch {
            target.events.collect { event -> handleEvent(event) }
        }
    }
'''
    new_observe = '''    private fun observeEvents(target: AudioPlayer) {
        eventJob?.cancel()
        eventJob = playerScope.launch(start = CoroutineStart.UNDISPATCHED) {
            target.events.collect { event -> handleEvent(event) }
        }
    }
'''
    if old_observe not in text:
        raise SystemExit("playback: observeEvents block missing")
    text = text.replace(old_observe, new_observe, 1)

    old_complete = '''        runCatching { outgoing?.stop() }
        outgoing?.setVolume(internalState.value.volume)
        observeEvents(incoming)
        incoming.setVolume(internalState.value.volume)
'''
    new_complete = '''        runCatching { outgoing?.stop() }
        outgoing?.setVolume(internalState.value.volume)
        observeEvents(incoming)
        transitionEventJob?.cancel()
        transitionEventJob = null
        incoming.setVolume(internalState.value.volume)
'''
    if old_complete not in text:
        raise SystemExit("playback: complete transition observer block missing")
    text = text.replace(old_complete, new_complete, 1)

    old_cancel = '''    private fun cancelTransition() {
        val companion: AudioPlayer?
        val wasActive: Boolean
        synchronized(transitionLock) {
            wasActive = transitionActive
            transitionActive = false
            preparedTrackId = ""
            preparedTransitionMs = 0L
            preparedStreamLabel = ""
            handoffAttemptedTrackId = ""
            companion = companionPlayer
        }
        transitionJob?.cancel()
        prepareJob?.cancel()
        runCatching { companion?.stop() }
        if (wasActive) {
            player?.setVolume(internalState.value.volume)
        }
    }
'''
    new_cancel = '''    private fun cancelTransition() {
        val companion: AudioPlayer?
        val pendingPrepare: Job?
        val wasActive: Boolean
        synchronized(transitionLock) {
            wasActive = transitionActive
            transitionActive = false
            preparedTrackId = ""
            preparedTransitionMs = 0L
            preparedStreamLabel = ""
            handoffAttemptedTrackId = ""
            companion = companionPlayer
            pendingPrepare = prepareJob
            prepareJob = null
        }
        transitionJob?.cancel()
        transitionEventJob?.cancel()
        transitionEventJob = null
        pendingPrepare?.cancel()
        runCatching { companion?.stop() }
        if (wasActive) {
            player?.setVolume(internalState.value.volume)
        }
    }
'''
    if old_cancel not in text:
        raise SystemExit("playback: cancelTransition block missing")
    text = text.replace(old_cancel, new_cancel, 1)

    old_shutdown = '''        transitionJob?.cancel()
        prepareJob?.cancel()
        persistJob?.cancel()
'''
    new_shutdown = '''        transitionJob?.cancel()
        transitionEventJob?.cancel()
        prepareJob?.cancel()
        persistJob?.cancel()
'''
    if old_shutdown not in text:
        raise SystemExit("playback: shutdown job block missing")
    text = text.replace(old_shutdown, new_shutdown, 1)

    path.write_text(text, encoding="utf-8")


def patch_vlc_player() -> None:
    path = Path("desktop/player/src/main/kotlin/com/luc4n3x/levyra/desktop/player/VlcAudioPlayer.kt")
    text = path.read_text(encoding="utf-8")
    old = '''    private var lastPublishedTimeMs: Long = Long.MIN_VALUE
    private var lastTimePublishNanos: Long = 0L
    private var loadedUrl: String = ""
    private var requestedPaused: Boolean = false
'''
    new = '''    private var lastPublishedTimeMs: Long = Long.MIN_VALUE
    private var lastTimePublishNanos: Long = 0L

    @Volatile
    private var loadedUrl: String = ""

    @Volatile
    private var requestedPaused: Boolean = false
'''
    if old not in text:
        raise SystemExit("vlc player: state block missing")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def patch_local_store() -> None:
    path = Path("desktop/core/src/main/kotlin/com/luc4n3x/levyra/desktop/core/localmusic/LocalLibraryStore.kt")
    text = path.read_text(encoding="utf-8")
    old = '''    fun removeFolder(folderId: String) = mutate { data ->
        val folder = data.folders.firstOrNull { it.id == folderId } ?: return@mutate data
        data.copy(
            folders = data.folders.filterNot { it.id == folderId },
            tracks = data.tracks.filterNot { LocalMusicIdentity.isWithin(it.path, folder.path) }
        )
    }
'''
    new = '''    fun removeFolder(folderId: String) = mutate { data ->
        val folder = data.folders.firstOrNull { it.id == folderId } ?: return@mutate data
        val remainingFolders = data.folders.filterNot { it.id == folderId }
        data.copy(
            folders = remainingFolders,
            tracks = data.tracks.filter { track ->
                !LocalMusicIdentity.isWithin(track.path, folder.path) ||
                    remainingFolders.any { remaining -> LocalMusicIdentity.isWithin(track.path, remaining.path) }
            }
        )
    }
'''
    if old not in text:
        raise SystemExit("local store: removeFolder block missing")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def patch_local_controller() -> None:
    path = Path("desktop/app/src/main/kotlin/com/luc4n3x/levyra/desktop/app/state/LocalMusicController.kt")
    text = path.read_text(encoding="utf-8")

    old_remote = '''            M3uPlaylist.parse(content).forEach { entry ->
                val resolved = M3uPlaylist.resolve(entry, baseDirectory)
                if (resolved == null || !Files.isRegularFile(resolved)) {
                    skipped += 1
                    return@forEach
                }
'''
    new_remote = '''            M3uPlaylist.parse(content).forEach { entry ->
                if (entry.isRemote) {
                    val videoId = M3uPlaylist.youtubeVideoId(entry)
                    if (videoId.isBlank()) {
                        skipped += 1
                    } else {
                        tracks.add(
                            Track(
                                id = videoId,
                                title = entry.title.ifBlank { videoId },
                                artist = entry.artist,
                                videoUrl = Track.watchUrlOf(videoId),
                                durationMs = entry.durationMs
                            )
                        )
                    }
                    return@forEach
                }
                val resolved = M3uPlaylist.resolve(entry, baseDirectory)
                if (resolved == null || !Files.isRegularFile(resolved)) {
                    skipped += 1
                    return@forEach
                }
'''
    if old_remote not in text:
        raise SystemExit("local controller: playlist loop block missing")
    text = text.replace(old_remote, new_remote, 1)

    old_invalid = '''                    val valid = key.reset()
                    if (!valid && parent != null) {
                        watched.remove(LocalMusicIdentity.normalizePathKey(parent.toString()))
                    }
'''
    new_invalid = '''                    val valid = key.reset()
                    if (!valid && parent != null) {
                        val removed = watched.remove(LocalMusicIdentity.normalizePathKey(parent.toString()))
                        if (removed) registered = (registered - 1).coerceAtLeast(0)
                    }
'''
    if old_invalid not in text:
        raise SystemExit("local controller: watcher invalidation block missing")
    text = text.replace(old_invalid, new_invalid, 1)
    text = text.replace("        const val MAX_WATCH_DEPTH = 8\n", "        const val MAX_WATCH_DEPTH = 12\n", 1)

    path.write_text(text, encoding="utf-8")


def patch_m3u() -> None:
    path = Path("desktop/core/src/main/kotlin/com/luc4n3x/levyra/desktop/core/localmusic/M3uPlaylist.kt")
    text = path.read_text(encoding="utf-8")
    text = text.replace("import java.nio.file.Path\n", "import java.net.URI\nimport java.nio.file.Path\n", 1)
    anchor = '''    private fun isWindowsAbsolute(location: String): Boolean =
        location.startsWith("//") ||
            (location.length >= 3 && location[0].isLetter() && location[1] == ':' && location[2] == '/')

    fun render(name: String, tracks: List<Track>): String = buildString {
'''
    replacement = '''    private fun isWindowsAbsolute(location: String): Boolean =
        location.startsWith("//") ||
            (location.length >= 3 && location[0].isLetter() && location[1] == ':' && location[2] == '/')

    fun youtubeVideoId(entry: M3uEntry): String {
        if (!entry.isRemote) return ""
        val uri = runCatching { URI(entry.location.trim()) }.getOrNull() ?: return ""
        if (!uri.scheme.equals("https", ignoreCase = true) && !uri.scheme.equals("http", ignoreCase = true)) {
            return ""
        }
        val host = uri.host.orEmpty().lowercase()
        if (host !in YOUTUBE_HOSTS) return ""
        return Track.videoIdOf(entry.location).takeIf { YOUTUBE_VIDEO_ID.matches(it) }.orEmpty()
    }

    fun render(name: String, tracks: List<Track>): String = buildString {
'''
    if anchor not in text:
        raise SystemExit("m3u: render anchor missing")
    text = text.replace(anchor, replacement, 1)
    old_tail = '''    private const val LINE_SEPARATOR = "\\n"
}
'''
    new_tail = '''    private const val LINE_SEPARATOR = "\\n"
    private val YOUTUBE_HOSTS = setOf(
        "youtube.com",
        "www.youtube.com",
        "m.youtube.com",
        "music.youtube.com",
        "youtu.be",
        "youtube-nocookie.com",
        "www.youtube-nocookie.com"
    )
    private val YOUTUBE_VIDEO_ID = Regex("[A-Za-z0-9_-]{11}")
}
'''
    if old_tail not in text:
        raise SystemExit("m3u: tail anchor missing")
    path.write_text(text.replace(old_tail, new_tail, 1), encoding="utf-8")


def patch_tests() -> None:
    m3u = Path("desktop/core/src/test/kotlin/com/luc4n3x/levyra/desktop/core/localmusic/M3uPlaylistTest.kt")
    text = m3u.read_text(encoding="utf-8")
    insert_before = '''    @Test
    fun rendersLocalPathsAndRemoteUrlsWithExtinfLabels() {
'''
    test_block = '''    @Test
    fun acceptsOnlyYouTubeRemoteEntriesForRoundTrip() {
        assertEquals(
            "dQw4w9WgXcQ",
            M3uPlaylist.youtubeVideoId(
                M3uEntry(location = "https://www.youtube.com/watch?v=dQw4w9WgXcQ")
            )
        )
        assertEquals(
            "dQw4w9WgXcQ",
            M3uPlaylist.youtubeVideoId(M3uEntry(location = "https://youtu.be/dQw4w9WgXcQ"))
        )
        assertEquals("", M3uPlaylist.youtubeVideoId(M3uEntry(location = "https://example.org/watch?v=dQw4w9WgXcQ")))
    }

'''
    if insert_before not in text:
        raise SystemExit("m3u test insertion point missing")
    text = text.replace(insert_before, test_block + insert_before, 1)
    m3u.write_text(text, encoding="utf-8")

    store_test = Path("desktop/core/src/test/kotlin/com/luc4n3x/levyra/desktop/core/localmusic/LocalLibraryStoreTest.kt")
    if not store_test.exists():
        store_test.write_text('''package com.luc4n3x.levyra.desktop.core.localmusic

import com.luc4n3x.levyra.desktop.core.storage.JsonFileStore
import java.nio.file.Files
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalLibraryStoreTest {

    @Test
    fun removingParentFolderPreservesTracksCoveredByRemainingChildFolder() {
        val root = Files.createTempDirectory("levyra-local-store")
        val parent = root.resolve("Music")
        val child = parent.resolve("Albums")
        Files.createDirectories(child)
        val backing = JsonFileStore(
            file = root.resolve("localmusic.json"),
            serializer = LocalLibraryData.serializer(),
            defaultValue = { LocalLibraryData() },
            json = JsonFileStore.DEFAULT_JSON
        )
        val store = LocalLibraryStore(backing, nowMillis = { 1_000L })
        val parentFolder = requireNotNull(store.addFolder(parent.toString()))
        val childFolder = requireNotNull(store.addFolder(child.toString()))
        store.replaceTracks(
            listOf(
                LocalTrack(id = "parent", path = parent.resolve("loose.flac").toString(), folderId = parentFolder.id),
                LocalTrack(id = "child", path = child.resolve("album.flac").toString(), folderId = childFolder.id)
            )
        )

        store.removeFolder(parentFolder.id)

        assertEquals(listOf(childFolder.id), store.current.folders.map { it.id })
        assertEquals(listOf("child"), store.current.tracks.map { it.id })
        assertTrue(LocalMusicIdentity.isWithin(store.current.tracks.single().path, childFolder.path))
    }
}
''', encoding="utf-8")


patch_android_player()
patch_lyrics_share()
patch_playback_controller()
patch_vlc_player()
patch_local_store()
patch_local_controller()
patch_m3u()
patch_tests()
