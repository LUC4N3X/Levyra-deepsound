from pathlib import Path


def replace_once(path: Path, old: str, new: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"expected exactly one match in {path}, found {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


levyra = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")

replace_once(
    levyra,
    '''            Surface(
                color = Color.White.copy(alpha = 0.085f),
                border = BorderStroke(
                    1.dp,
                    if (comments.visible) primary.copy(alpha = 0.52f) else Color.White.copy(alpha = 0.105f)
                ),
                shape = CircleShape,
                modifier = Modifier
                    .sizeIn(minHeight = 48.dp)
                    .pressable(enabled = canOpenComments, onClick = onComments)
            ) {
                Row(
                    modifier = Modifier
                        .height(if (compact) 38.dp else 40.dp)
                        .padding(horizontal = if (compact) 12.dp else 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChatBubbleOutline,
                        contentDescription = null,
                        tint = if (canOpenComments) Color.White.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.42f),
                        modifier = Modifier.size(if (compact) 18.dp else 19.dp)
                    )
                    when {
                        comments.loading && !comments.loaded -> CircularProgressIndicator(
                            modifier = Modifier.size(if (compact) 12.dp else 13.dp),
                            strokeWidth = 1.8.dp,
                            color = primary.playerMix(Color.White, 0.52f)
                        )
                        commentBadge.isNotBlank() -> Text(
                            text = commentBadge,
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = if (compact) 12.sp else 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                }
            }
''',
    '''            Box(
                modifier = Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .pressable(enabled = canOpenComments, onClick = onComments),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color.White.copy(alpha = 0.085f),
                    border = BorderStroke(
                        1.dp,
                        if (comments.visible) primary.copy(alpha = 0.52f) else Color.White.copy(alpha = 0.105f)
                    ),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier
                            .height(if (compact) 38.dp else 40.dp)
                            .padding(horizontal = if (compact) 10.dp else 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ChatBubbleOutline,
                            contentDescription = null,
                            tint = if (canOpenComments) Color.White.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.42f),
                            modifier = Modifier.size(if (compact) 17.dp else 18.dp)
                        )
                        when {
                            comments.loading && !comments.loaded -> CircularProgressIndicator(
                                modifier = Modifier.size(if (compact) 12.dp else 13.dp),
                                strokeWidth = 1.8.dp,
                                color = primary.playerMix(Color.White, 0.52f)
                            )
                            commentBadge.isNotBlank() -> Text(
                                text = commentBadge,
                                color = Color.White.copy(alpha = 0.90f),
                                fontSize = if (compact) 11.5.sp else 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
''',
)

replace_once(
    levyra,
    '''        val playCorner by animateDpAsState(
            targetValue = if (isPlaying) 24.dp else 34.dp,
            animationSpec = spring(dampingRatio = 0.67f, stiffness = Spring.StiffnessMediumLow),
            label = "play-corner"
        )
        val playShape = RoundedCornerShape(playCorner)
''',
    '''        val playShape = RoundedCornerShape(30.dp)
''',
)

replace_once(
    levyra,
    '''                AnimatedContent(
                    targetState = isPlaying,
                    transitionSpec = {
                        (fadeIn(tween(150)) + scaleIn(initialScale = 0.72f, animationSpec = tween(150))) togetherWith
                            (fadeOut(tween(110)) + scaleOut(targetScale = 0.72f, animationSpec = tween(110)))
                    },
                    label = "play-icon"
                ) { playing ->
                    Icon(
                        imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playing) LocalLevyraStrings.current.pause else LocalLevyraStrings.current.play,
                        tint = playGradient.content,
                        modifier = Modifier.size(if (compact) 37.dp else 39.dp)
                    )
                }
''',
    '''                AnimatedContent(
                    targetState = isPlaying,
                    transitionSpec = {
                        fadeIn(tween(120, easing = FastOutSlowInEasing)) togetherWith
                            fadeOut(tween(90, easing = FastOutSlowInEasing))
                    },
                    label = "play-icon"
                ) { playing ->
                    Box(
                        modifier = Modifier.size(if (compact) 39.dp else 41.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (playing) LocalLevyraStrings.current.pause else LocalLevyraStrings.current.play,
                            tint = playGradient.content,
                            modifier = Modifier
                                .size(if (compact) 35.dp else 36.dp)
                                .offset(x = if (playing) 0.dp else 1.dp)
                        )
                    }
                }
''',
)

motion = Path("app/src/main/java/com/luc4n3x/levyra/ui/MotionArtworkLayer.kt")

replace_once(
    motion,
    '''                    val amount = motionAmount
                    val scale = 1f + amount * (0.045f + zoomPhase.value * 0.020f)
                    scaleX = scale
                    scaleY = scale
                    translationX = artworkSize.width * 0.014f * horizontalDrift.value * amount
                    translationY = artworkSize.height * 0.012f * verticalDrift.value * amount
                    rotationZ = 0.12f * tiltPhase.value * amount
''',
    '''                    val amount = motionAmount
                    val scale = 1f + amount * (0.058f + zoomPhase.value * 0.026f)
                    scaleX = scale
                    scaleY = scale
                    translationX = artworkSize.width * 0.021f * horizontalDrift.value * amount
                    translationY = artworkSize.height * 0.016f * verticalDrift.value * amount
                    rotationZ = 0.18f * tiltPhase.value * amount
''',
)

replace_once(
    motion,
    '''private const val STATIC_ARTWORK_ZOOM_DURATION_MS = 12_000
private const val STATIC_ARTWORK_HORIZONTAL_DURATION_MS = 15_000
private const val STATIC_ARTWORK_VERTICAL_DURATION_MS = 18_000
private const val STATIC_ARTWORK_TILT_DURATION_MS = 21_000
private const val STATIC_ARTWORK_MOTION_ENTER_MS = 480
''',
    '''private const val STATIC_ARTWORK_ZOOM_DURATION_MS = 9_000
private const val STATIC_ARTWORK_HORIZONTAL_DURATION_MS = 11_500
private const val STATIC_ARTWORK_VERTICAL_DURATION_MS = 13_500
private const val STATIC_ARTWORK_TILT_DURATION_MS = 17_000
private const val STATIC_ARTWORK_MOTION_ENTER_MS = 360
''',
)
