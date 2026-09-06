from pathlib import Path
import sys

repo = Path(sys.argv[1]).resolve()
now_playing = repo / "app/src/main/java/com/luc4n3x/levyra/ui/player/LevyraNowPlaying.kt"
text = now_playing.read_text(encoding="utf-8")


def replace_once(old: str, new: str, label: str) -> None:
    global text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, found {count}")
    text = text.replace(old, new, 1)


replace_once(
    "import android.media.AudioManager\n",
    "import android.media.AudioManager\nimport androidx.activity.compose.BackHandler\n",
    "BackHandler import",
)
replace_once(
    "import androidx.compose.material.icons.rounded.CloseFullscreen\n",
    "import androidx.compose.material.icons.rounded.CloseFullscreen\nimport androidx.compose.material.icons.rounded.Fullscreen\n",
    "Fullscreen import",
)
replace_once(
    "gestureLayerContent: (@Composable (Track, PlayerGestureConfig, PlayerGestureMediaActions, PlayerGestureUiActions, Modifier) -> Unit)? = null,",
    "gestureLayerContent: (@Composable (Track, PlayerGestureConfig, PlayerGestureMediaActions, PlayerGestureUiActions, androidx.compose.runtime.MutableState<PlayerVideoTransform>?, Modifier) -> Unit)? = null,",
    "gesture callback signature",
)
replace_once(
    """        val playerHorizontalPadding = if (state.isVideoMode) {
            LevyraPlayerDesign.SpaceSm
        } else {
            levyraFoldAwareGutterDp(layoutMode, compactPlayer).dp
        }
""",
    """        val playerHorizontalPadding = if (state.isVideoMode) {
            LevyraPlayerDesign.SpaceXs
        } else {
            levyraFoldAwareGutterDp(layoutMode, compactPlayer).dp
        }
""",
    "video gutter",
)
replace_once(
    """        val artworkPreviewAvailable = !state.isVideoMode && artworkUrl.isNotBlank() && visualMode == PlayerVisualMode.Artwork
        var showArtworkPreview by remember(track?.id, state.isVideoMode) { mutableStateOf(false) }
        var optionsExpanded by remember(track?.id) { mutableStateOf(false) }
""",
    """        val artworkPreviewAvailable = !state.isVideoMode && artworkUrl.isNotBlank() && visualMode == PlayerVisualMode.Artwork
        var showArtworkPreview by remember(track?.id, state.isVideoMode) { mutableStateOf(false) }
        var optionsExpanded by remember(track?.id) { mutableStateOf(false) }
        var videoFullscreen by remember(track?.id, state.isVideoMode) { mutableStateOf(false) }
        val videoTransform = remember(track?.id, state.isVideoMode) {
            mutableStateOf(PlayerVideoTransform.None)
        }

        BackHandler(enabled = videoFullscreen) {
            videoFullscreen = false
        }
""",
    "video state",
)
replace_once(
    """                        PlayerGlassIconButton(
                            icon = Icons.Rounded.PictureInPictureAlt,
                            contentDescription = strings.pictureInPicture,
                            size = headerButtonSize,
                            iconSize = 19.dp,
                            borderTop = primary.copy(alpha = 0.48f),
                            borderBottom = primary.copy(alpha = 0.14f),
                            onClick = { LevyraPipBridge.enter() }
                        )
""",
    """                        PlayerGlassIconButton(
                            icon = Icons.Rounded.PictureInPictureAlt,
                            contentDescription = strings.pictureInPicture,
                            size = headerButtonSize,
                            iconSize = 19.dp,
                            borderTop = primary.copy(alpha = 0.48f),
                            borderBottom = primary.copy(alpha = 0.14f),
                            onClick = { LevyraPipBridge.enter() }
                        )
                        PlayerGlassIconButton(
                            icon = Icons.Rounded.Fullscreen,
                            contentDescription = strings.enterImmersive,
                            size = headerButtonSize,
                            iconSize = 20.dp,
                            borderTop = primary.copy(alpha = 0.48f),
                            borderBottom = primary.copy(alpha = 0.14f),
                            onClick = { videoFullscreen = true }
                        )
""",
    "fullscreen button",
)
replace_once(
    """            Box(
                modifier = if (playerPane == LevyraPlayerPane.SideBySide) {
                    Modifier
                        .size(width = artworkSize, height = artworkSize)
                        .padding(vertical = if (compactPlayer) 1.dp else 2.dp)
                } else {
                    Modifier
                        .size(width = artworkSize, height = artworkSize)
                        .padding(vertical = if (compactPlayer) 1.dp else 2.dp)
                },
                contentAlignment = Alignment.Center
            ) {
""",
    """            Box(
                modifier = when {
                    state.isVideoMode -> Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .padding(vertical = if (compactPlayer) 1.dp else 2.dp)
                    playerPane == LevyraPlayerPane.SideBySide -> Modifier
                        .size(width = artworkSize, height = artworkSize)
                        .padding(vertical = if (compactPlayer) 1.dp else 2.dp)
                    else -> Modifier
                        .size(width = artworkSize, height = artworkSize)
                        .padding(vertical = if (compactPlayer) 1.dp else 2.dp)
                },
                contentAlignment = Alignment.Center
            ) {
""",
    "16:9 video frame",
)
replace_once(
    """                if (state.isVideoMode && activeTrack.videoUrl.isNotBlank() && videoSurfaceContent != null) {
                    videoSurfaceContent(
                        activeTrack,
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = artScale
                                scaleY = artScale
                                translationY = artOffset.toPx()
                                shadowElevation = artShadow
                                shape = RoundedCornerShape(artCorner)
                                clip = true
                            }
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(artCorner)
                            )
                    )
                } else {
""",
    """                if (state.isVideoMode && activeTrack.videoUrl.isNotBlank() && videoSurfaceContent != null) {
                    val zoom = videoTransform.value
                    if (!videoFullscreen) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = artScale
                                    scaleY = artScale
                                    translationY = artOffset.toPx()
                                    shadowElevation = artShadow
                                    shape = RoundedCornerShape(artCorner)
                                    clip = true
                                }
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(artCorner)
                                )
                        ) {
                            videoSurfaceContent(
                                activeTrack,
                                Modifier
                                    .matchParentSize()
                                    .graphicsLayer {
                                        scaleX = zoom.scale
                                        scaleY = zoom.scale
                                        translationX = zoom.offsetX
                                        translationY = zoom.offsetY
                                    }
                            )
                        }
                    }
                } else {
""",
    "video zoom surface",
)
replace_once(
    """                if (state.interfaceSettings.playerGesturesEnabled && gestureLayerContent != null) {
                    gestureLayerContent(
""",
    """                val videoGesturesEnabled = state.isVideoMode && activeTrack.videoUrl.isNotBlank()
                if ((state.interfaceSettings.playerGesturesEnabled || videoGesturesEnabled) &&
                    gestureLayerContent != null &&
                    !videoFullscreen
                ) {
                    gestureLayerContent(
""",
    "video gesture condition",
)
replace_once(
    """                        PlayerGestureUiActions(
                            feedback = { message ->
                                gestureFeedback = message
                                gestureFeedbackEvent += 1
                            },
                            haptic = {
                                hapticFeedback.perform(LevyraHapticAction.TrackSwipe)
                            },
                            collapse = collapseActions,
                            artworkPreview = if (artworkPreviewAvailable) {
                                { showArtworkPreview = true }
                            } else {
                                null
                            }
                        ),
                        Modifier
                            .matchParentSize()
                            .zIndex(20f)
                    )
""",
    """                        PlayerGestureUiActions(
                            feedback = { message ->
                                gestureFeedback = message
                                gestureFeedbackEvent += 1
                            },
                            haptic = {
                                hapticFeedback.perform(LevyraHapticAction.TrackSwipe)
                            },
                            collapse = collapseActions,
                            artworkPreview = if (artworkPreviewAvailable) {
                                { showArtworkPreview = true }
                            } else {
                                null
                            }
                        ),
                        videoTransform.takeIf { videoGesturesEnabled },
                        Modifier
                            .matchParentSize()
                            .zIndex(if (videoGesturesEnabled) 21f else 20f)
                    )
""",
    "video gesture transform",
)
replace_once(
    """        playlistDialogContent?.invoke()
""",
    """        if (videoFullscreen &&
            state.isVideoMode &&
            track != null &&
            track.videoUrl.isNotBlank() &&
            videoSurfaceContent != null
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .zIndex(100f)
            ) {
                val zoom = videoTransform.value
                videoSurfaceContent(
                    track,
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = zoom.scale
                            scaleY = zoom.scale
                            translationX = zoom.offsetX
                            translationY = zoom.offsetY
                        }
                )
                if (gestureLayerContent != null) {
                    gestureLayerContent(
                        track,
                        PlayerGestureConfig(
                            trackId = track.id,
                            settings = state.interfaceSettings,
                            playbackSpeed = state.playbackSpeed,
                            environment = PlayerGestureEnvironment(
                                activity = playerActivity,
                                audioManager = audioManager,
                                brightnessLabel = strings.brightness,
                                volumeLabel = strings.volume,
                                rightToLeft = rightToLeft
                            )
                        ),
                        PlayerGestureMediaActions(
                            seekBy = { delta ->
                                viewModel.seekBy(delta)
                                hapticFeedback.perform(LevyraHapticAction.TrackSwipe)
                                mediaSeekFeedbackMs = delta
                                mediaSeekFeedbackEvent += 1
                            },
                            next = viewModel::next,
                            previous = viewModel::previous,
                            swipeOffset = { swipeOffsetPx = it },
                            temporarySpeed = viewModel::setTemporaryPlaybackSpeed
                        ),
                        PlayerGestureUiActions(
                            feedback = { message ->
                                gestureFeedback = message
                                gestureFeedbackEvent += 1
                            },
                            haptic = {
                                hapticFeedback.perform(LevyraHapticAction.TrackSwipe)
                            },
                            collapse = collapseActions
                        ),
                        videoTransform,
                        Modifier
                            .matchParentSize()
                            .zIndex(101f)
                    )
                }
                PlayerGlassIconButton(
                    icon = Icons.Rounded.CloseFullscreen,
                    contentDescription = strings.exitImmersive,
                    size = LevyraPlayerDesign.HeaderButton,
                    iconSize = 22.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = LevyraPlayerDesign.SpaceSm, end = LevyraPlayerDesign.SpaceSm)
                        .zIndex(102f),
                    onClick = { videoFullscreen = false }
                )
            }
        }

        playlistDialogContent?.invoke()
""",
    "fullscreen overlay",
)

now_playing.write_text(text, encoding="utf-8")

app = repo / "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt"
app_text = app.read_text(encoding="utf-8")
old = """        gestureLayerContent = { activeTrack, config, mediaActions, uiActions, modifier ->
            PlayerGestureLayer(
                config = config,
                mediaActions = mediaActions,
                uiActions = uiActions,
                modifier = modifier
            )
        },
"""
new = """        gestureLayerContent = { activeTrack, config, mediaActions, uiActions, videoTransform, modifier ->
            PlayerGestureLayer(
                config = config,
                mediaActions = mediaActions,
                uiActions = uiActions,
                videoTransform = videoTransform,
                modifier = modifier
            )
        },
"""
count = app_text.count(old)
if count != 1:
    raise SystemExit(f"LevyraApp gesture bridge: expected one match, found {count}")
app.write_text(app_text.replace(old, new, 1), encoding="utf-8")

required = [
    repo / "app/src/main/java/com/luc4n3x/levyra/ui/player/PlayerVideoZoom.kt",
    repo / "app/src/main/res/layout/levyra_video_player_view_zoomable.xml",
]
missing = [str(path.relative_to(repo)) for path in required if not path.exists()]
if missing:
    raise SystemExit("Missing native-video files from main: " + ", ".join(missing))
