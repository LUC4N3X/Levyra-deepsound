from pathlib import Path


def replace_exact(path: str, old: str, new: str, count: int = 1) -> None:
    file = Path(path)
    text = file.read_text(encoding="utf-8")
    actual = text.count(old)
    if actual != count:
        raise SystemExit(f"{path}: expected {count} occurrence(s), found {actual}: {old[:80]!r}")
    file.write_text(text.replace(old, new), encoding="utf-8")


def replace_many(path: str, replacements: list[tuple[str, str]]) -> None:
    for old, new in replacements:
        replace_exact(path, old, new)


resolver = "app/src/main/java/com/luc4n3x/levyra/data/PlaybackResolver.kt"
replace_many(
    resolver,
    [
        ("import kotlinx.coroutines.CoroutineStart\n", ""),
        ("import kotlinx.coroutines.Deferred\n", ""),
        ("import kotlinx.coroutines.async\n", ""),
        (
            "import kotlinx.coroutines.coroutineScope\n",
            "import kotlinx.coroutines.coroutineScope\nimport kotlinx.coroutines.currentCoroutineContext\n",
        ),
        (
            "import kotlinx.coroutines.delay\n",
            "import kotlinx.coroutines.delay\nimport kotlinx.coroutines.ensureActive\n",
        ),
        (
            "    private val streamCache = ConcurrentHashMap<String, CachedStream>()\n"
            "    private val inFlight = ConcurrentHashMap<String, Deferred<Track>>()\n",
            "    private val streamCache = ConcurrentHashMap<String, CachedStream>()\n"
            "    private val resolveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)\n"
            "    private val singleFlight = PlaybackSingleFlight<String, Track>(resolveScope)\n",
        ),
        (
            "        return withContext(Dispatchers.IO) {\n"
            "            runCatching {\n"
            "                NewPipeRuntime.ensure()\n"
            "                val info = StreamInfo.getInfo(ServiceList.YouTube, videoUrl)\n"
            "                cacheYoutubeEngagement(videoId, info.likeCount, info.viewCount)\n"
            "                track.withYoutubeEngagement(info.likeCount, info.viewCount)\n"
            "            }.getOrElse { error ->\n"
            "                Timber.d(error, \"youtube engagement unavailable for %s\", videoId)\n"
            "                cacheYoutubeEngagement(videoId, -1L, -1L)\n"
            "                track\n"
            "            }\n"
            "        }\n",
            "        return withContext(Dispatchers.IO) {\n"
            "            runCatchingPreservingCancellation {\n"
            "                NewPipeRuntime.ensure()\n"
            "                StreamInfo.getInfo(ServiceList.YouTube, videoUrl)\n"
            "            }.fold(\n"
            "                onSuccess = { info ->\n"
            "                    cacheYoutubeEngagement(videoId, info.likeCount, info.viewCount)\n"
            "                    track.withYoutubeEngagement(info.likeCount, info.viewCount)\n"
            "                },\n"
            "                onFailure = { error ->\n"
            "                    Timber.d(error, \"youtube engagement unavailable for %s\", videoId)\n"
            "                    cacheYoutubeEngagement(videoId, -1L, -1L)\n"
            "                    track\n"
            "                }\n"
            "            )\n"
            "        }\n",
        ),
        (
            "        sourceMatchScope.launch {\n"
            "            runCatching {\n"
            "                sourceMatchStore.recordFailure(track, isVideoMode, selectedAudioQuality, sourceMatchQuarantineMs)\n"
            "            }.onFailure { error ->\n"
            "                Timber.w(error, \"persistent source match failure update failed\")\n"
            "            }\n"
            "        }\n",
            "        sourceMatchScope.launch {\n"
            "            runCatchingPreservingCancellation {\n"
            "                sourceMatchStore.recordFailure(track, isVideoMode, selectedAudioQuality, sourceMatchQuarantineMs)\n"
            "            }.onFailure { error ->\n"
            "                Timber.w(error, \"persistent source match failure update failed\")\n"
            "            }\n"
            "        }\n",
        ),
        (
            "        val key = \"${cacheKey(track, isVideoMode, audioQuality)}_$requestKind\"\n"
            "        Timber.d(\"resolver start kind=%s mode=%s id=%s quality=%s\", requestKind, if (isVideoMode) \"video\" else \"audio\", track.id, audioQuality)\n"
            "        val deferred = async(Dispatchers.IO, start = CoroutineStart.LAZY) {\n"
            "            try {\n"
            "                withTimeout(timeoutMs) {\n"
            "                    resolveUncached(track.copy(streamUrl = \"\", videoStreamUrl = \"\"), isVideoMode, preferMp4Audio, audioQuality)\n"
            "                }\n"
            "            } catch (error: TimeoutCancellationException) {\n"
            "                val label = if (requestKind == \"offline\") \"Download\" else \"YouTube\"\n"
            "                throw PlaybackBlockedException(\"$label lento: sto aspettando lo stream più del previsto, riprova tra qualche secondo\")\n"
            "            }\n"
            "        }\n"
            "        val previous = inFlight.putIfAbsent(key, deferred)\n"
            "        if (previous != null) {\n"
            "            deferred.cancel()\n"
            "            Timber.d(\"resolver in-flight join kind=%s mode=%s id=%s\", requestKind, if (isVideoMode) \"video\" else \"audio\", track.id)\n"
            "            return@coroutineScope previous.await()\n"
            "        }\n\n"
            "        try {\n"
            "            deferred.start()\n"
            "            return@coroutineScope deferred.await()\n"
            "        } finally {\n"
            "            inFlight.remove(key, deferred)\n"
            "        }\n",
            "        val key = \"${cacheKey(track, isVideoMode, audioQuality)}_$requestKind\"\n"
            "        Timber.d(\"resolver start kind=%s mode=%s id=%s quality=%s\", requestKind, if (isVideoMode) \"video\" else \"audio\", track.id, audioQuality)\n"
            "        return@coroutineScope singleFlight.run(key) {\n"
            "            try {\n"
            "                withTimeout(timeoutMs) {\n"
            "                    resolveUncached(track.copy(streamUrl = \"\", videoStreamUrl = \"\"), isVideoMode, preferMp4Audio, audioQuality)\n"
            "                }\n"
            "            } catch (error: TimeoutCancellationException) {\n"
            "                val label = if (requestKind == \"offline\") \"Download\" else \"YouTube\"\n"
            "                throw PlaybackBlockedException(\"$label lento: sto aspettando lo stream più del previsto, riprova tra qualche secondo\")\n"
            "            }\n"
            "        }\n",
        ),
        (
            "        return runCatching { resolve(track, isVideoMode) }.getOrNull()\n",
            "        return runCatchingPreservingCancellation { resolve(track, isVideoMode) }.getOrNull()\n",
        ),
        (
            "                    val result = runCatching { resolveVideoWithLevyraExtractor(track, audioQuality) }\n",
            "                    val result = runCatchingPreservingCancellation { resolveVideoWithLevyraExtractor(track, audioQuality) }\n",
        ),
        (
            "                    val stream = runCatching { hedgedInnerTube(track, errors, true, audioQuality) }.getOrNull()\n",
            "                    val stream = runCatchingPreservingCancellation { hedgedInnerTube(track, errors, true, audioQuality) }.getOrNull()\n",
        ),
        (
            "                val resolved = runCatching { resolveWithLevyraExtractor(track, false, audioQuality) }\n",
            "                val resolved = runCatchingPreservingCancellation { resolveWithLevyraExtractor(track, false, audioQuality) }\n",
        ),
        (
            "                val stream = runCatching { hedgedInnerTube(track, errors, false, audioQuality) }.getOrNull()\n",
            "                val stream = runCatchingPreservingCancellation { hedgedInnerTube(track, errors, false, audioQuality) }.getOrNull()\n",
        ),
        (
            "            val resolved = runCatching { resolveWithLevyraExtractor(track, true, audioQuality) }\n",
            "            val resolved = runCatchingPreservingCancellation { resolveWithLevyraExtractor(track, true, audioQuality) }\n",
        ),
        (
            "            val stream = runCatching { raceInnerTube(track, errors, false, true, audioQuality) }.getOrNull()\n",
            "            val stream = runCatchingPreservingCancellation { raceInnerTube(track, errors, false, true, audioQuality) }.getOrNull()\n",
        ),
        (
            "            val resolved = runCatching { resolveAudioFast(candidate, localErrors, preferMp4Audio, audioQuality) }.getOrNull()\n",
            "            val resolved = runCatchingPreservingCancellation { resolveAudioFast(candidate, localErrors, preferMp4Audio, audioQuality) }.getOrNull()\n",
        ),
        (
            "                runCatching { repository.search(query, 6, userPreferences.languageCode()) }\n",
            "                runCatchingPreservingCancellation { repository.search(query, 6, userPreferences.languageCode()) }\n",
        ),
        (
            "                    runCatching { resolveWithInnerTube(track, profile, isVideoMode, false, audioQuality) }\n",
            "                    runCatchingPreservingCancellation { resolveWithInnerTube(track, profile, isVideoMode, false, audioQuality) }\n",
        ),
        (
            "                val attempt = runCatching { resolveWithInnerTube(track, profile, isVideoMode, preferMp4Audio, audioQuality) }\n",
            "                val attempt = runCatchingPreservingCancellation { resolveWithInnerTube(track, profile, isVideoMode, preferMp4Audio, audioQuality) }\n",
        ),
        (
            "            val firstAttempt = runCatching {\n"
            "                resolveWithInnerTubeOnce(track, profile, isVideoMode, preferMp4Audio, audioQuality)\n"
            "            }\n",
            "            val firstAttempt = runCatchingPreservingCancellation {\n"
            "                resolveWithInnerTubeOnce(track, profile, isVideoMode, preferMp4Audio, audioQuality)\n"
            "            }\n",
        ),
        (
            "        } catch (error: Throwable) {\n"
            "            val latency = elapsedMs(startedAt)\n"
            "            if (hasValidatedInternet()) {\n"
            "                recordClientFailure(profile, latency, error)\n"
            "                resilienceEngine.recordFailure(profile.label, mode, latency, error)\n"
            "            } else {\n"
            "                clearTransientClientPenalties()\n"
            "                Timber.d(error, \"resolver skipped client penalty while offline\")\n"
            "            }\n"
            "            throw error\n"
            "        }\n",
            "        } catch (error: CancellationException) {\n"
            "            throw error\n"
            "        } catch (error: Throwable) {\n"
            "            currentCoroutineContext().ensureActive()\n"
            "            val latency = elapsedMs(startedAt)\n"
            "            if (hasValidatedInternet()) {\n"
            "                recordClientFailure(profile, latency, error)\n"
            "                resilienceEngine.recordFailure(profile.label, mode, latency, error)\n"
            "            } else {\n"
            "                clearTransientClientPenalties()\n"
            "                Timber.d(error, \"resolver skipped client penalty while offline\")\n"
            "            }\n"
            "            throw error\n"
            "        }\n",
        ),
    ],
)

service = "app/src/main/java/com/luc4n3x/levyra/player/PlaybackService.kt"
replace_many(
    service,
    [
        (
            "import com.luc4n3x.levyra.data.PlaybackResolver\n",
            "import com.luc4n3x.levyra.data.PlaybackResolver\nimport com.luc4n3x.levyra.data.runCatchingPreservingCancellation\n",
        ),
        (
            "        val additions = runCatching {\n"
            "            musicRepository.radio(seed, LevyraPreferences(this).languageCode(), 20)\n"
            "        }.onFailure { Timber.w(it, \"Background radio expansion failed\") }\n",
            "        val additions = runCatchingPreservingCancellation {\n"
            "            musicRepository.radio(seed, LevyraPreferences(this).languageCode(), 20)\n"
            "        }.onFailure { Timber.w(it, \"Background radio expansion failed\") }\n",
        ),
        (
            "        runCatching { resolveQueueTrack(target) }\n"
            "            .onFailure { Timber.w(it, \"Background queue resolution failed\") }\n",
            "        runCatchingPreservingCancellation { resolveQueueTrack(target) }\n"
            "            .onFailure { Timber.w(it, \"Background queue resolution failed\") }\n",
        ),
        (
            "                runCatching { resolveQueueTrack(target) }\n"
            "                    .onFailure { Timber.d(it, \"Service queue prefetch skipped\") }\n",
            "                runCatchingPreservingCancellation { resolveQueueTrack(target) }\n"
            "                    .onFailure { Timber.d(it, \"Service queue prefetch skipped\") }\n",
        ),
        (
            "            withContext(Dispatchers.IO) { runCatching { playbackWarmup.prime(resolved, 256L * 1024L) } }\n",
            "            withContext(Dispatchers.IO) { runCatchingPreservingCancellation { playbackWarmup.prime(resolved, 256L * 1024L) } }\n",
        ),
        (
            "            val resolved = runCatching { resolveQueueTrack(track) }\n"
            "                .onFailure { Timber.w(it, \"Background queue resolution failed\") }\n",
            "            val resolved = runCatchingPreservingCancellation { resolveQueueTrack(track) }\n"
            "                .onFailure { Timber.w(it, \"Background queue resolution failed\") }\n",
        ),
        (
            "                    runCatching { resolveQueueTrack(queueTrack) }\n"
            "                        .onFailure { Timber.w(it, \"Fresh background stream resolution failed\") }\n",
            "                    runCatchingPreservingCancellation { resolveQueueTrack(queueTrack) }\n"
            "                        .onFailure { Timber.w(it, \"Fresh background stream resolution failed\") }\n",
        ),
    ],
)

viewmodel = "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt"
replace_many(
    viewmodel,
    [
        (
            "import com.luc4n3x.levyra.data.PlaybackResolver\n",
            "import com.luc4n3x.levyra.data.PlaybackResolver\nimport com.luc4n3x.levyra.data.runCatchingPreservingCancellation\n",
        ),
        (
            "                runCatching { playbackWarmup.primeVideo(resolved) }\n",
            "                runCatchingPreservingCancellation { playbackWarmup.primeVideo(resolved) }\n",
        ),
        (
            "                runCatching { playbackWarmup.prime(resolved) }\n",
            "                runCatchingPreservingCancellation { playbackWarmup.prime(resolved) }\n",
        ),
        (
            "            val result = runCatching { appUpdateRepository.latest() }\n",
            "            val result = runCatchingPreservingCancellation { appUpdateRepository.latest() }\n",
        ),
        (
            "            val message = runCatching { backupManager.exportTo(uri).message }\n",
            "            val message = runCatchingPreservingCancellation { backupManager.exportTo(uri).message }\n",
        ),
        (
            "            val message = runCatching {\n"
            "                backupManager.restoreFrom(uri)\n",
            "            val message = runCatchingPreservingCancellation {\n"
            "                backupManager.restoreFrom(uri)\n",
        ),
        (
            "                runCatching { artistRepository.profile(normalizedBrowseId, clean) }.getOrNull()\n",
            "                runCatchingPreservingCancellation { artistRepository.profile(normalizedBrowseId, clean) }.getOrNull()\n",
        ),
        (
            "                        runCatching { artistRepository.profileFor(clean) }.getOrNull()\n",
            "                        runCatchingPreservingCancellation { artistRepository.profileFor(clean) }.getOrNull()\n",
        ),
        (
            "                    runCatching { artistRepository.biographyFor(clean, normalizedBrowseId) }.getOrNull()\n",
            "                    runCatchingPreservingCancellation { artistRepository.biographyFor(clean, normalizedBrowseId) }.getOrNull()\n",
        ),
        (
            "            runCatching { biographyDeferred.await() }.getOrNull()?.let { biography ->\n",
            "            runCatchingPreservingCancellation { biographyDeferred.await() }.getOrNull()?.let { biography ->\n",
        ),
        (
            "            val detail = runCatching { repository.albumDetail(album, languageCode) }.getOrNull()\n",
            "            val detail = runCatchingPreservingCancellation { repository.albumDetail(album, languageCode) }.getOrNull()\n",
        ),
        (
            "                val description = runCatching {\n"
            "                    repository.resolveAlbumDescription(detail, languageCode)\n",
            "                val description = runCatchingPreservingCancellation {\n"
            "                    repository.resolveAlbumDescription(detail, languageCode)\n",
        ),
        (
            "            val result = runCatching {\n"
            "                sharedMediaResolver.resolve(request, _state.value.languageCode)\n",
            "            val result = runCatchingPreservingCancellation {\n"
            "                sharedMediaResolver.resolve(request, _state.value.languageCode)\n",
        ),
        (
            "                runCatching { repository.searchSuggestions(clean, _state.value.languageCode) }.getOrDefault(emptyList()).take(6)\n",
            "                runCatchingPreservingCancellation { repository.searchSuggestions(clean, _state.value.languageCode) }.getOrDefault(emptyList()).take(6)\n",
        ),
        (
            "        val result = runCatching {\n"
            "            coroutineScope {\n"
            "                val rawSearch = async {\n",
            "        val result = runCatchingPreservingCancellation {\n"
            "            coroutineScope {\n"
            "                val rawSearch = async {\n",
        ),
        (
            "                    runCatching { artistRepository.artistHitFor(clean) }.getOrNull()\n",
            "                    runCatchingPreservingCancellation { artistRepository.artistHitFor(clean) }.getOrNull()\n",
        ),
        (
            "            val results = runCatching {\n"
            "                repository.exploreZone(zone.id, zone.query, languageCode, 24)\n",
            "            val results = runCatchingPreservingCancellation {\n"
            "                repository.exploreZone(zone.id, zone.query, languageCode, 24)\n",
        ),
        (
            "            val result = runCatching { sponsorBlockRepository.segments(track.id) }.getOrDefault(emptyList())\n",
            "            val result = runCatchingPreservingCancellation { sponsorBlockRepository.segments(track.id) }.getOrDefault(emptyList())\n",
        ),
        (
            "                runCatching {\n"
            "                    lyricsRepository.prefetch(\n",
            "                runCatchingPreservingCancellation {\n"
            "                    lyricsRepository.prefetch(\n",
        ),
        (
            "                                val match = runCatching {\n"
            "                                    repository.searchOne(\"${track.title} ${track.artist}\", _state.value.languageCode)\n",
            "                                val match = runCatchingPreservingCancellation {\n"
            "                                    repository.searchOne(\"${track.title} ${track.artist}\", _state.value.languageCode)\n",
        ),
        (
            "                                    runCatching { playbackWarmup.primeVideo(resolved) }\n",
            "                                    runCatchingPreservingCancellation { playbackWarmup.primeVideo(resolved) }\n",
        ),
        (
            "                                    runCatching { playbackWarmup.prime(resolved, plan.primeBytes) }\n",
            "                                    runCatchingPreservingCancellation { playbackWarmup.prime(resolved, plan.primeBytes) }\n",
        ),
        (
            "        val radioTracks = runCatching {\n"
            "            repository.radio(request.seed, _state.value.languageCode, 20)\n",
            "        val radioTracks = runCatchingPreservingCancellation {\n"
            "            repository.radio(request.seed, _state.value.languageCode, 20)\n",
        ),
        (
            "            val resolved = runCatching { resolvePlayableTrack(carried) }\n",
            "            val resolved = runCatchingPreservingCancellation { resolvePlayableTrack(carried) }\n",
        ),
    ],
)

print("Playback stabilization patch applied cleanly")
