from pathlib import Path


FILE = Path("app/src/main/java/com/luc4n3x/levyra/data/PlaybackResolver.kt")


def replace_exact(text: str, old: str, new: str, count: int = 1) -> str:
    actual = text.count(old)
    if actual != count:
        raise SystemExit(
            f"PlaybackResolver.kt: expected {count} occurrence(s), found {actual}: {old[:100]!r}"
        )
    return text.replace(old, new)


text = FILE.read_text(encoding="utf-8")

for old in (
    "import kotlinx.coroutines.CoroutineStart\n",
    "import kotlinx.coroutines.Deferred\n",
    "import kotlinx.coroutines.async\n",
):
    text = replace_exact(text, old, "")

text = replace_exact(
    text,
    "import kotlinx.coroutines.coroutineScope\n",
    "import kotlinx.coroutines.coroutineScope\n"
    "import kotlinx.coroutines.currentCoroutineContext\n",
)
text = replace_exact(
    text,
    "import kotlinx.coroutines.delay\n",
    "import kotlinx.coroutines.delay\n"
    "import kotlinx.coroutines.ensureActive\n",
)

text = replace_exact(
    text,
    "    private val streamCache = ConcurrentHashMap<String, CachedStream>()\n"
    "    private val inFlight = ConcurrentHashMap<String, Deferred<Track>>()\n",
    "    private val streamCache = ConcurrentHashMap<String, CachedStream>()\n"
    "    private val resolveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)\n"
    "    private val singleFlight = PlaybackSingleFlight<String, Track>(resolveScope)\n",
)

text = replace_exact(
    text,
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
)

text = replace_exact(
    text,
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
)

text = replace_exact(
    text,
    "    private fun searchYouTubeWebCandidates(track: Track, query: String): List<Track> {\n",
    "    private suspend fun searchYouTubeWebCandidates(track: Track, query: String): List<Track> {\n",
)
text = replace_exact(
    text,
    "        return runCatching {\n"
    "            searchFallbackClient.newCall(request).execute().use { response ->\n",
    "        val result = runCatching {\n"
    "            searchFallbackClient.newCall(request).execute().use { response ->\n",
)
text = replace_exact(
    text,
    "        }.getOrDefault(emptyList())\n"
    "    }\n\n"
    "    private fun sameVideoIdentity(left: Track, right: Track): Boolean {\n",
    "        }.getOrDefault(emptyList())\n"
    "        currentCoroutineContext().ensureActive()\n"
    "        return result\n"
    "    }\n\n"
    "    private fun sameVideoIdentity(left: Track, right: Track): Boolean {\n",
)

text = replace_exact(
    text,
    "    private fun acceptResolvedStream(stream: DirectStream, isVideoMode: Boolean, label: String, errors: MutableList<String>): Boolean {\n",
    "    private suspend fun acceptResolvedStream(stream: DirectStream, isVideoMode: Boolean, label: String, errors: MutableList<String>): Boolean {\n",
)
text = replace_exact(
    text,
    "    private fun verifyDirectAudioUrlFast(url: String): Boolean {\n",
    "    private suspend fun verifyDirectAudioUrlFast(url: String): Boolean {\n",
)
text = replace_exact(
    text,
    "        return runCatching {\n"
    "            streamProbeClient.newCall(request).execute().use { response ->\n",
    "        val result = runCatching {\n"
    "            streamProbeClient.newCall(request).execute().use { response ->\n",
)
text = replace_exact(
    text,
    "        }.getOrDefault(false)\n"
    "    }\n\n"
    "    private fun isTrustedGoogleVideoUrl(url: String): Boolean {\n",
    "        }.getOrDefault(false)\n"
    "        currentCoroutineContext().ensureActive()\n"
    "        return result\n"
    "    }\n\n"
    "    private fun isTrustedGoogleVideoUrl(url: String): Boolean {\n",
)

text = replace_exact(
    text,
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
)

text = replace_exact(
    text,
    "    private fun isVerifiedHlsManifest(url: String): Boolean {\n",
    "    private suspend fun isVerifiedHlsManifest(url: String): Boolean {\n",
)
text = replace_exact(
    text,
    "        return runCatching {\n"
    "            youtubeHttpClient.newCall(request).execute().use { response ->\n",
    "        val result = runCatching {\n"
    "            youtubeHttpClient.newCall(request).execute().use { response ->\n",
)
text = replace_exact(
    text,
    "        }.getOrDefault(false)\n"
    "    }\n\n"
    "    private fun resolveWithLevyraExtractor(\n",
    "        }.getOrDefault(false)\n"
    "        currentCoroutineContext().ensureActive()\n"
    "        return result\n"
    "    }\n\n"
    "    private suspend fun resolveWithLevyraExtractor(\n",
)
text = replace_exact(
    text,
    "    private fun resolveVideoWithLevyraExtractor(\n",
    "    private suspend fun resolveVideoWithLevyraExtractor(\n",
)

FILE.write_text(text, encoding="utf-8")
print("PlaybackResolver stabilization patch applied cleanly")
