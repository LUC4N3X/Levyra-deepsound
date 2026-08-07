from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly 1 match, found {count}")
    return text.replace(old, new, 1)


vm_path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")
vm = vm_path.read_text()

vm = replace_once(
    vm,
    "    private var pauseAfterNextPlaybackStart: Boolean = false\n",
    "",
    "remove global pause flag",
)

policy_marker = "internal fun prioritizeNewReleasesForUser("
policy = (
    "internal data class PlaybackResolveRequest(\n"
    "    val id: Long,\n"
    "    val startPaused: Boolean = false\n"
    ")\n\n"
    "internal fun shouldStartPlaybackPaused(\n"
    "    request: PlaybackResolveRequest,\n"
    "    activeRequestId: Long\n"
    "): Boolean = request.id == activeRequestId && request.startPaused\n\n"
)
if "internal data class PlaybackResolveRequest" not in vm:
    vm = replace_once(vm, policy_marker, policy + policy_marker, "insert request policy")

vm = replace_once(
    vm,
    "            pendingSeekMs = resumeMs\n"
    "            pauseAfterNextPlaybackStart = !session.wasPlaying\n"
    "            startResolve(restoredTrack)\n",
    "            pendingSeekMs = resumeMs\n"
    "            startResolve(restoredTrack, startPaused = !session.wasPlaying)\n",
    "scope Samples paused restore",
)

vm = replace_once(
    vm,
    "    private fun startResolve(track: Track, preserveCrossfade: Boolean = false, autoRetryWhenOffline: Boolean = false) {\n",
    "    private fun startResolve(track: Track, preserveCrossfade: Boolean = false, autoRetryWhenOffline: Boolean = false, startPaused: Boolean = false) {\n",
    "startResolve signature",
)

start_resolve_marker = "        val requestId = ++playRequestId\n"
start_resolve_pos = vm.index("    private fun startResolve(")
request_pos = vm.index(start_resolve_marker, start_resolve_pos)
insertion_at = request_pos + len(start_resolve_marker)
vm = vm[:insertion_at] + "        val request = PlaybackResolveRequest(requestId, startPaused)\n" + vm[insertion_at:]

vm = replace_once(
    vm,
    "            resolveAndStartPlayback(track, requestId, autoRetryWhenOffline)\n",
    "            resolveAndStartPlayback(track, request, autoRetryWhenOffline)\n",
    "resolve coroutine request",
)

vm = replace_once(
    vm,
    "    private suspend fun CoroutineScope.resolveAndStartPlayback(\n"
    "        track: Track,\n"
    "        requestId: Long,\n"
    "        autoRetryWhenOffline: Boolean\n"
    "    ) {\n",
    "    private suspend fun CoroutineScope.resolveAndStartPlayback(\n"
    "        track: Track,\n"
    "        request: PlaybackResolveRequest,\n"
    "        autoRetryWhenOffline: Boolean\n"
    "    ) {\n"
    "        val requestId = request.id\n",
    "resolveAndStartPlayback signature",
)

vm = replace_once(
    vm,
    "            startPlayback(preserveEditorialArtwork(track, instant))\n",
    "            startPlayback(preserveEditorialArtwork(track, instant), request)\n",
    "instant playback request",
)
vm = replace_once(
    vm,
    "            startPlayback(playable)\n",
    "            startPlayback(playable, request)\n",
    "resolved playback request",
)

vm = replace_once(
    vm,
    "        pauseAfterNextPlaybackStart = false\n",
    "",
    "remove failure reset",
)

vm = replace_once(
    vm,
    "    private fun startPlayback(playable: Track) {\n"
    "        val selectedIndex = queueEngine.state.value.currentIndex\n"
    "        if (selectedIndex >= 0) queueEngine.updateTrackAt(selectedIndex, playable)\n"
    "        repository.replace(playable)\n"
    "        val startPaused = pauseAfterNextPlaybackStart\n"
    "        pauseAfterNextPlaybackStart = false\n",
    "    private fun startPlayback(playable: Track, request: PlaybackResolveRequest) {\n"
    "        if (request.id != playRequestId) return\n"
    "        val startPaused = shouldStartPlaybackPaused(request, playRequestId)\n"
    "        val selectedIndex = queueEngine.state.value.currentIndex\n"
    "        if (selectedIndex >= 0) queueEngine.updateTrackAt(selectedIndex, playable)\n"
    "        repository.replace(playable)\n",
    "startPlayback request policy",
)

if "pauseAfterNextPlaybackStart" in vm:
    raise RuntimeError("stale pauseAfterNextPlaybackStart reference remains")
vm_path.write_text(vm)


app_path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
app = app_path.read_text()
start = app.index("private fun RowScope.ExploreMoodCard(")
end = app.index("\n@Composable\nprivate fun ExploreSamplesRow(", start)
block = app[start:end]
block = replace_once(block, ".height(58.dp)", ".heightIn(min = 58.dp)", "ExploreMoodCard height")
block = replace_once(block, "            maxLines = 1,\n", "            maxLines = 2,\n", "ExploreMoodCard maxLines")
app_path.write_text(app[:start] + block + app[end:])


spotify_path = Path("tools/levyra-editorial/levyra_editorial/spotify.py")
spotify = spotify_path.read_text()
spotify = replace_once(
    spotify,
    "        self._track_metadata: dict[str, Mapping[str, Any]] = {}\n",
    "        self._track_metadata: dict[str, Mapping[str, Any]] = {}\n"
    "        self._track_metadata_rate_limited = False\n",
    "metadata rate-limit state",
)
spotify = replace_once(
    spotify,
    "    def enrich_track_metadata(self, items: list[dict[str, Any]]) -> list[dict[str, Any]]:\n"
    "        \"\"\"Best-effort ISRC and release metadata without weakening Pathfinder reads.\"\"\"\n"
    "        if self._access_token is None:\n",
    "    def enrich_track_metadata(self, items: list[dict[str, Any]]) -> list[dict[str, Any]]:\n"
    "        \"\"\"Best-effort ISRC and release metadata without weakening Pathfinder reads.\"\"\"\n"
    "        if self._track_metadata_rate_limited:\n"
    "            return items\n"
    "        if self._access_token is None:\n",
    "metadata early return",
)
spotify = replace_once(
    spotify,
    "                if response.status_code == 429:\n"
    "                    delay = _bounded_retry_after(response.headers.get(\"Retry-After\"))\n"
    "                    LOGGER.warning(\n"
    "                        \"Spotify track metadata rate-limited; retrying once in %d second(s).\",\n"
    "                        delay,\n"
    "                    )\n"
    "                    if delay > 0:\n"
    "                        time.sleep(delay)\n"
    "                    response = request_batch()\n"
    "                    if response.status_code == 401:\n"
    "                        self.authenticate()\n"
    "                        response = request_batch()\n",
    "                if response.status_code == 429:\n"
    "                    self._track_metadata_rate_limited = True\n"
    "                    LOGGER.warning(\n"
    "                        \"Spotify track metadata rate-limited; disabling best-effort metadata \"\n"
    "                        \"enrichment for the remainder of this run.\"\n"
    "                    )\n"
    "                    break\n",
    "metadata 429 handling",
)
spotify_path.write_text(spotify)


pathfinder_path = Path("tools/levyra-editorial/tests/test_pathfinder.py")
test = pathfinder_path.read_text()
test = test.replace("import levyra_editorial.spotify as spotify_module\n", "", 1)
old_start = test.index("def test_track_metadata_enrichment_retries_one_rate_limited_batch(")
old_end = test.index("\n\ndef test_playlist_limit_stops_paging_after_the_first_page()", old_start)
replacement = (
    "def test_track_metadata_enrichment_disables_after_first_rate_limit() -> None:\n"
    "    session = SequencedSession(\n"
    "        [FakeResponse({}, status_code=429, headers={\"Retry-After\": \"5\"})]\n"
    "    )\n"
    "    client = authenticated_client(session)\n"
    "    first = [{\"track\": {\"id\": \"track12345\", \"album\": {}}}]\n"
    "    second = [{\"track\": {\"id\": \"track67890\", \"album\": {}}}]\n\n"
    "    assert client.enrich_track_metadata(first) == first\n"
    "    assert client.enrich_track_metadata(second) == second\n"
    "    assert [url for url, _ in session.calls] == [f\"{API_BASE_URL}/tracks\"]\n"
)
pathfinder_path.write_text(test[:old_start] + replacement + test[old_end:])

restore_test = Path("app/src/test/java/com/luc4n3x/levyra/viewmodel/SamplesPlaybackRestorePolicyTest.kt")
restore_test.write_text(
    "package com.luc4n3x.levyra.viewmodel\n\n"
    "import org.junit.Assert.assertFalse\n"
    "import org.junit.Assert.assertTrue\n"
    "import org.junit.Test\n\n"
    "class SamplesPlaybackRestorePolicyTest {\n"
    "    @Test\n"
    "    fun pausedRestoreStartsPausedOnlyForItsOwnRequest() {\n"
    "        val request = PlaybackResolveRequest(id = 10L, startPaused = true)\n"
    "        assertTrue(shouldStartPlaybackPaused(request, activeRequestId = 10L))\n"
    "    }\n\n"
    "    @Test\n"
    "    fun supersededRestoreCannotPauseTheReplacementRequest() {\n"
    "        val staleRestore = PlaybackResolveRequest(id = 10L, startPaused = true)\n"
    "        assertFalse(shouldStartPlaybackPaused(staleRestore, activeRequestId = 11L))\n"
    "    }\n\n"
    "    @Test\n"
    "    fun normalRequestAfterFailedRestoreDoesNotInheritPause() {\n"
    "        val failedRestore = PlaybackResolveRequest(id = 10L, startPaused = true)\n"
    "        val normalReplacement = PlaybackResolveRequest(id = 11L)\n"
    "        assertFalse(shouldStartPlaybackPaused(failedRestore, activeRequestId = 11L))\n"
    "        assertFalse(shouldStartPlaybackPaused(normalReplacement, activeRequestId = 11L))\n"
    "    }\n"
    "}\n"
)

print("PR313_TARGETED_PATCH_OK")
