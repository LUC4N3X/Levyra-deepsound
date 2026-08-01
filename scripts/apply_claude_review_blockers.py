from pathlib import Path


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"expected exactly one match in {path}, found {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


def replace_between(path: Path, start_marker: str, end_marker: str, replacement: str) -> None:
    text = path.read_text(encoding="utf-8")
    start = text.find(start_marker)
    if start < 0:
        raise SystemExit(f"missing start marker in {path}: {start_marker!r}")
    end = text.find(end_marker, start)
    if end < 0:
        raise SystemExit(f"missing end marker in {path}: {end_marker!r}")
    path.write_text(text[:start] + replacement + text[end:], encoding="utf-8")


provider = Path("app/src/main/java/com/luc4n3x/levyra/feature/motion/CommunityCanvasProvider.kt")
replace_once(
    provider,
    """                is CommunityCanvasIndexLookup.Available -> indexed.entries
                CommunityCanvasIndexLookup.Unavailable -> catalog()
""",
    """                is CommunityCanvasIndexLookup.Available -> indexed.entries.ifEmpty { catalog() }
                CommunityCanvasIndexLookup.Unavailable -> catalog()
""",
)
replace_once(
    provider,
    """        val rows = parseCommunityCanvasIndexShard(payload)
        if (rows.isEmpty()) throw CommunityCanvasException("Community canvas index shard is empty")
""",
    """        val rows = parseCommunityCanvasIndexShardOrNull(payload)
            ?: throw CommunityCanvasException("Community canvas index shard is invalid")
""",
)

index_file = Path("app/src/main/java/com/luc4n3x/levyra/feature/motion/CommunityCanvasIndex.kt")
replace_between(
    index_file,
    "internal fun parseCommunityCanvasIndexShard(payload: String): List<CommunityCanvasIndexedEntry> {",
    "\nprivate const val COMMUNITY_GENERATION_PREFIX_CHARS",
    """internal fun parseCommunityCanvasIndexShard(payload: String): List<CommunityCanvasIndexedEntry> =
    parseCommunityCanvasIndexShardOrNull(payload).orEmpty()

internal fun parseCommunityCanvasIndexShardOrNull(
    payload: String
): List<CommunityCanvasIndexedEntry>? {
    val root = runCatching { JSONObject(payload) }.getOrNull() ?: return null
    if (root.optInt("version") != COMMUNITY_CANVAS_INDEX_VERSION) return null
    val items = root.optJSONArray("items") ?: return null
    return buildList {
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            val lookupHash = item.optString("h").trim()
            if (!COMMUNITY_LOOKUP_HASH_PATTERN.matches(lookupHash)) continue
            val rawUrl = item.optString("u").trim()
            if (communityCanvasMediaUrl(rawUrl) == null) continue
            val scope = when (item.optString("s").trim().lowercase(Locale.ROOT)) {
                "a" -> MotionArtworkScope.ALBUM
                "t" -> MotionArtworkScope.TRACK
                else -> continue
            }
            add(
                CommunityCanvasIndexedEntry(
                    lookupHash = lookupHash,
                    url = rawUrl,
                    scope = scope,
                    isrc = communityCanvasIsrc(item.optString("i")),
                    width = item.optInt("w").takeIf { it > 0 },
                    height = item.optInt("g").takeIf { it > 0 }
                )
            )
        }
    }
}
""",
)

motion = Path("app/src/main/java/com/luc4n3x/levyra/ui/MotionArtworkLayer.kt")
replace_once(
    motion,
    "import androidx.compose.animation.core.FastOutSlowInEasing\n",
    "import androidx.compose.animation.core.Animatable\nimport androidx.compose.animation.core.FastOutSlowInEasing\n",
)
for obsolete in (
    "import androidx.compose.animation.core.RepeatMode\n",
    "import androidx.compose.animation.core.animateFloat\n",
    "import androidx.compose.animation.core.infiniteRepeatable\n",
    "import androidx.compose.animation.core.rememberInfiniteTransition\n",
):
    replace_once(motion, obsolete, "")
replace_once(
    motion,
    "import kotlinx.coroutines.delay\n",
    "import kotlinx.coroutines.coroutineScope\nimport kotlinx.coroutines.delay\nimport kotlinx.coroutines.isActive\nimport kotlinx.coroutines.launch\n",
)
replace_once(
    motion,
    """    var videoReady by remember(artwork?.identityKey, artwork?.url, artwork?.mimeType) {
        mutableStateOf(false)
    }
    val videoArtwork = artwork?.takeIf {
""",
    """    var videoReady by remember(artwork?.identityKey, artwork?.url, artwork?.mimeType) {
        mutableStateOf(false)
    }
    var videoRetryCount by remember(artwork?.identityKey, artwork?.url, artwork?.mimeType) {
        mutableStateOf(0)
    }
    val videoArtwork = artwork?.takeIf {
""",
)
replace_once(
    motion,
    """    val animateStatic = enabled &&
        lifecycleActive &&
        environment.localAllowed &&
        isPlaying &&
        !videoReady
""",
    """    LaunchedEffect(videoArtwork) {
        if (videoArtwork == null) videoReady = false
    }
    LaunchedEffect(
        videoUnavailable,
        enabled,
        lifecycleActive,
        environment.remoteAllowed,
        isPlaying,
    ) {
        if (
            !videoUnavailable ||
            !enabled ||
            !lifecycleActive ||
            !environment.remoteAllowed ||
            !isPlaying ||
            videoRetryCount >= MAX_VIDEO_RETRIES
        ) {
            return@LaunchedEffect
        }
        delay(VIDEO_RETRY_DELAY_MS)
        if (
            enabled &&
            lifecycleActive &&
            environment.remoteAllowed &&
            isPlaying &&
            videoRetryCount < MAX_VIDEO_RETRIES
        ) {
            videoRetryCount += 1
            videoUnavailable = false
        }
    }
    val animateStatic = enabled &&
        lifecycleActive &&
        environment.localAllowed &&
        isPlaying &&
        !videoReady
""",
)
replace_once(
    motion,
    """                onFirstFrame = { videoReady = true },
""",
    """                onFirstFrame = {
                    videoReady = true
                    videoRetryCount = 0
                },
""",
)
replace_between(
    motion,
    "@Composable\nprivate fun MotionArtworkStaticFallback(",
    "\n@Composable\nprivate fun MotionArtworkVideo(",
    """@Composable
private fun MotionArtworkStaticFallback(
    animated: Boolean,
    cornerRadius: Dp,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    var artworkSize by remember { mutableStateOf(IntSize.Zero) }
    val zoomPhase = remember { Animatable(0f) }
    val horizontalDrift = remember { Animatable(0f) }
    val verticalDrift = remember { Animatable(0f) }
    val tiltPhase = remember { Animatable(0f) }
    val motionAmount by animateFloatAsState(
        targetValue = if (animated) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (animated) STATIC_ARTWORK_MOTION_ENTER_MS else STATIC_ARTWORK_MOTION_EXIT_MS,
            easing = FastOutSlowInEasing
        ),
        label = "static-artwork-motion-amount"
    )

    LaunchedEffect(animated) {
        if (!animated) {
            coroutineScope {
                launch {
                    zoomPhase.animateTo(
                        0f,
                        tween(STATIC_ARTWORK_MOTION_EXIT_MS, easing = FastOutSlowInEasing)
                    )
                }
                launch {
                    horizontalDrift.animateTo(
                        0f,
                        tween(STATIC_ARTWORK_MOTION_EXIT_MS, easing = FastOutSlowInEasing)
                    )
                }
                launch {
                    verticalDrift.animateTo(
                        0f,
                        tween(STATIC_ARTWORK_MOTION_EXIT_MS, easing = FastOutSlowInEasing)
                    )
                }
                launch {
                    tiltPhase.animateTo(
                        0f,
                        tween(STATIC_ARTWORK_MOTION_EXIT_MS, easing = FastOutSlowInEasing)
                    )
                }
            }
            return@LaunchedEffect
        }
        coroutineScope {
            launch {
                while (isActive) {
                    zoomPhase.animateTo(
                        1f,
                        tween(STATIC_ARTWORK_ZOOM_DURATION_MS, easing = FastOutSlowInEasing)
                    )
                    zoomPhase.animateTo(
                        0f,
                        tween(STATIC_ARTWORK_ZOOM_DURATION_MS, easing = FastOutSlowInEasing)
                    )
                }
            }
            launch {
                while (isActive) {
                    horizontalDrift.animateTo(
                        1f,
                        tween(STATIC_ARTWORK_HORIZONTAL_DURATION_MS, easing = FastOutSlowInEasing)
                    )
                    horizontalDrift.animateTo(
                        -1f,
                        tween(STATIC_ARTWORK_HORIZONTAL_DURATION_MS, easing = FastOutSlowInEasing)
                    )
                }
            }
            launch {
                while (isActive) {
                    verticalDrift.animateTo(
                        -1f,
                        tween(STATIC_ARTWORK_VERTICAL_DURATION_MS, easing = FastOutSlowInEasing)
                    )
                    verticalDrift.animateTo(
                        1f,
                        tween(STATIC_ARTWORK_VERTICAL_DURATION_MS, easing = FastOutSlowInEasing)
                    )
                }
            }
            launch {
                while (isActive) {
                    tiltPhase.animateTo(
                        1f,
                        tween(STATIC_ARTWORK_TILT_DURATION_MS, easing = FastOutSlowInEasing)
                    )
                    tiltPhase.animateTo(
                        -1f,
                        tween(STATIC_ARTWORK_TILT_DURATION_MS, easing = FastOutSlowInEasing)
                    )
                }
            }
        }
    }

    Box(modifier = modifier.clip(shape)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { artworkSize = it }
                .graphicsLayer {
                    val amount = motionAmount
                    val scale = 1f + amount * (0.045f + zoomPhase.value * 0.020f)
                    scaleX = scale
                    scaleY = scale
                    translationX = artworkSize.width * 0.014f * horizontalDrift.value * amount
                    translationY = artworkSize.height * 0.012f * verticalDrift.value * amount
                    rotationZ = 0.12f * tiltPhase.value * amount
                }
        ) {
            content()
        }
    }
}
""",
)
replace_once(
    motion,
    "private const val VIDEO_FIRST_FRAME_TIMEOUT_MS = 6_000L\n",
    """private const val VIDEO_FIRST_FRAME_TIMEOUT_MS = 9_000L
private const val VIDEO_RETRY_DELAY_MS = 4_000L
private const val MAX_VIDEO_RETRIES = 1
""",
)

builder = Path("scripts/build_community_canvas_index.py")
replace_between(
    builder,
    "def partition_rows(\n",
    "\n\ndef build_index(\n",
    """def partition_rows(
    rows: list[dict[str, Any]],
    target_bytes: int,
    max_bytes: int,
    requested_prefix_chars: int | None,
) -> tuple[int, dict[str, list[dict[str, Any]]], int]:
    candidates = (
        [requested_prefix_chars]
        if requested_prefix_chars is not None
        else list(range(MIN_PREFIX_CHARS, MAX_PREFIX_CHARS + 1))
    )
    largest_valid: tuple[int, dict[str, list[dict[str, Any]]], int] | None = None
    for prefix_chars in candidates:
        if prefix_chars is None or prefix_chars not in range(MIN_PREFIX_CHARS, MAX_PREFIX_CHARS + 1):
            raise IndexBuildError(
                f"prefix chars must be between {MIN_PREFIX_CHARS} and {MAX_PREFIX_CHARS}"
            )
        shards: dict[str, list[dict[str, Any]]] = defaultdict(list)
        for row in rows:
            prefix = hash_key(str(row["_key"]))[:prefix_chars]
            shards[prefix].append(
                {key: value for key, value in row.items() if key != "_key"}
            )
        largest = max(
            (len(serialized_shard(shard_rows)) for shard_rows in shards.values()),
            default=0,
        )
        if largest <= max_bytes:
            largest_valid = (prefix_chars, dict(shards), largest)
            if requested_prefix_chars is not None or largest <= target_bytes:
                return largest_valid
    if largest_valid is not None:
        return largest_valid
    raise IndexBuildError(
        f"unable to keep every shard below {max_bytes} bytes with at most {MAX_PREFIX_CHARS} prefix chars"
    )
""",
)
replace_once(
    builder,
    """    if row_shape != (2, 1, 1):
        raise IndexBuildError(
            f"lookup row generation changed: expected (2 track, 1 ISRC, 1 album), got {row_shape}"
        )
""",
    """    if row_shape != (2, 1, 1):
        raise IndexBuildError(
            f"lookup row generation changed: expected (2 track, 1 ISRC, 1 album), got {row_shape}"
        )

    fallback_prefix, _, fallback_largest = partition_rows(
        rows=[
            {
                "_key": f"test-key-{index}",
                "h": "A" * 43,
                "u": "https://vivimusicanvas.mkmdevilmi.workers.dev/Song/test.mp4",
                "s": "t",
            }
            for index in range(8)
        ],
        target_bytes=1,
        max_bytes=4096,
        requested_prefix_chars=None,
    )
    if fallback_prefix != MAX_PREFIX_CHARS or fallback_largest > 4096:
        raise IndexBuildError("valid shard fallback no longer reaches the maximum prefix depth")
""",
)

editorial_test = Path("app/src/test/java/com/luc4n3x/levyra/data/EditorialArtworkContinuityTest.kt")
replace_between(
    editorial_test,
    "    @Test\n    fun fallbackChartSourcesAlsoKeepTheArtworkTheUserOpened()",
    "    @Test\n    fun normalTracksKeepTheResolvedArtwork()",
    """    @Test
    fun chartMoodTagDoesNotLockNonEditorialArtwork() {
        val presented = track(
            source = "Search",
            thumbnail = "https://charts.example.test/red-cover.jpg",
            moodTags = setOf("chart"),
        )
        val resolved = track(
            source = "YouTube Music",
            thumbnail = "https://charts.example.test/white-cover.jpg",
        )

        assertEquals(resolved, preserveEditorialArtwork(presented, resolved))
    }

    @Test
    fun chartSourceNameDoesNotLockNonEditorialArtwork() {
        val presented = track(
            source = "Apple Music Charts",
            thumbnail = "https://charts.example.test/presented-cover.jpg",
        )
        val resolved = track(
            source = "YouTube Music",
            thumbnail = "https://charts.example.test/resolved-cover.jpg",
        )

        assertEquals(resolved, preserveEditorialArtwork(presented, resolved))
    }

""",
)

provider_test = Path("app/src/test/java/com/luc4n3x/levyra/feature/motion/CommunityCanvasProviderTest.kt")
replace_once(
    provider_test,
    """    @Test
    fun mirrorCatalogVersionIsExposedForUsabilityChecks() {
""",
    """    @Test
    fun fuzzyCatalogMatchingPreservesMetadataVariantCoverage() {
        val entries = listOf(
            CommunityCanvasEntry(
                song = "Flowers",
                artist = "Miley Cyrus",
                album = "Endless Summer Vacation",
                url = "https://vivimusicanvas.mkmdevilmi.workers.dev/Song/flowers.mp4",
                scope = MotionArtworkScope.TRACK,
            )
        )
        val identity = MotionTrackIdentity(
            title = "Flowers - Single Version",
            artists = listOf("Miley Cyrus"),
            album = "Endless Summer Vacation (Deluxe)",
            durationMs = 200_000L,
            isrc = "",
            upc = "",
            year = "",
            trackId = "flowers",
            albumId = "endless-summer-vacation",
        )

        assertTrue(communityCanvasCandidates(identity, entries, nowMs = 1_000L).isNotEmpty())
    }

    @Test
    fun mirrorCatalogVersionIsExposedForUsabilityChecks() {
""",
)

index_test = Path("app/src/test/java/com/luc4n3x/levyra/feature/motion/CommunityCanvasIndexTest.kt")
text = index_test.read_text(encoding="utf-8")
needle = "\n}\n"
if not text.endswith(needle):
    raise SystemExit("unexpected CommunityCanvasIndexTest ending")
addition = """

    @Test
    fun safeButUnsupportedShardRowsAreAValidCachedMiss() {
        val payload = """
            {
              "version": 2,
              "items": [
                {
                  "h": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                  "u": "https://example.com/not-allowed.mp4",
                  "s": "t"
                }
              ]
            }
        """.trimIndent()

        val parsed = parseCommunityCanvasIndexShardOrNull(payload)

        assertNotNull(parsed)
        assertTrue(parsed!!.isEmpty())
    }

    @Test
    fun malformedShardRemainsUnavailable() {
        assertNull(parseCommunityCanvasIndexShardOrNull("{ truncated"))
    }
"""
index_test.write_text(text[:-len(needle)] + addition + needle, encoding="utf-8")
