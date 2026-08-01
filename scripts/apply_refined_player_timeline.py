#!/usr/bin/env python3
"""Apply the final understated Now Playing spacing and timeline polish."""

from pathlib import Path

PATH = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
text = PATH.read_text(encoding="utf-8")


def replace_between(source: str, start_marker: str, end_marker: str, replacement: str, label: str) -> str:
    start_count = source.count(start_marker)
    end_count = source.count(end_marker)
    if start_count != 1 or end_count != 1:
        raise SystemExit(
            f"Expected one {label} boundary pair, found start={start_count}, end={end_count}"
        )
    start = source.index(start_marker)
    end = source.index(end_marker, start)
    return source[:start] + replacement.rstrip() + "\n\n" + source[end:]


new_timeline = r'''@Composable
private fun PlayerTimeline(
    positionMs: Long,
    durationMs: Long,
    activeColor: Color,
    secondaryColor: Color,
    compact: Boolean,
    onSeek: (Float) -> Unit
) {
    var dragFraction by remember { mutableFloatStateOf(-1f) }
    val isDragging = dragFraction >= 0f
    val fraction = (if (isDragging) dragFraction else progressOf(positionMs, durationMs)).coerceIn(0f, 1f)
    val railStroke by animateDpAsState(
        targetValue = if (isDragging) 3.1.dp else 2.35.dp,
        animationSpec = spring(dampingRatio = 0.84f, stiffness = Spring.StiffnessMedium),
        label = "timeline-rail-stroke"
    )
    val markerHalfSize by animateDpAsState(
        targetValue = if (isDragging) 5.7.dp else 4.55.dp,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMedium),
        label = "timeline-marker-size"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = if (compact) 0.dp else 1.dp,
                bottom = if (compact) 2.dp else 3.dp
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 31.dp else 33.dp)
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = fraction,
                        range = 0f..1f,
                        steps = 0
                    )
                    setProgress { targetValue ->
                        if (durationMs <= 0L) {
                            false
                        } else {
                            onSeek(targetValue.coerceIn(0f, 1f))
                            true
                        }
                    }
                }
                .pointerInput(durationMs) {
                    if (durationMs > 0L) {
                        detectTapGestures { offset ->
                            val inset = 7.dp.toPx()
                            val usable = (size.width - inset * 2f).coerceAtLeast(1f)
                            onSeek(((offset.x - inset) / usable).coerceIn(0f, 1f))
                        }
                    }
                }
                .pointerInput(durationMs) {
                    if (durationMs > 0L) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                val inset = 7.dp.toPx()
                                val usable = (size.width - inset * 2f).coerceAtLeast(1f)
                                dragFraction = ((offset.x - inset) / usable).coerceIn(0f, 1f)
                            },
                            onDragEnd = {
                                if (dragFraction >= 0f) onSeek(dragFraction)
                                dragFraction = -1f
                            },
                            onDragCancel = { dragFraction = -1f },
                            onHorizontalDrag = { change, _ ->
                                change.consume()
                                val inset = 7.dp.toPx()
                                val usable = (size.width - inset * 2f).coerceAtLeast(1f)
                                dragFraction = ((change.position.x - inset) / usable).coerceIn(0f, 1f)
                            }
                        )
                    }
                }
                .drawBehind {
                    val centerY = size.height / 2f
                    val inset = 7.dp.toPx()
                    val usable = (size.width - inset * 2f).coerceAtLeast(1f)
                    val playedX = inset + usable * fraction
                    val endX = inset + usable
                    val marker = markerHalfSize.toPx()
                    val rail = railStroke.toPx()
                    val start = androidx.compose.ui.geometry.Offset(inset, centerY)
                    val end = androidx.compose.ui.geometry.Offset(endX, centerY)
                    val playedEnd = androidx.compose.ui.geometry.Offset(playedX, centerY)

                    drawLine(
                        color = Color.White.copy(alpha = 0.13f),
                        start = start,
                        end = end,
                        strokeWidth = 1.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.045f),
                        start = androidx.compose.ui.geometry.Offset(inset, centerY - 2.1.dp.toPx()),
                        end = androidx.compose.ui.geometry.Offset(endX, centerY - 2.1.dp.toPx()),
                        strokeWidth = 0.7.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    if (playedX > inset + 0.5f) {
                        val playedBrush = Brush.horizontalGradient(
                            colors = listOf(
                                activeColor.playerMix(Color.White, 0.38f),
                                activeColor.playerMix(secondaryColor, 0.22f),
                                secondaryColor.playerMix(Color.White, 0.18f)
                            ),
                            startX = inset,
                            endX = playedX.coerceAtLeast(inset + 1f)
                        )
                        val glowBrush = Brush.horizontalGradient(
                            colors = listOf(
                                activeColor.copy(alpha = 0.06f),
                                activeColor.playerMix(secondaryColor, 0.35f).copy(alpha = 0.13f),
                                secondaryColor.copy(alpha = 0.19f)
                            ),
                            startX = inset,
                            endX = playedX.coerceAtLeast(inset + 1f)
                        )
                        drawLine(
                            brush = glowBrush,
                            start = start,
                            end = playedEnd,
                            strokeWidth = if (isDragging) 8.dp.toPx() else 6.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            brush = playedBrush,
                            start = start,
                            end = playedEnd,
                            strokeWidth = rail,
                            cap = StrokeCap.Round
                        )

                        val whisperPath = Path().apply {
                            moveTo(inset, centerY)
                            val width = playedX - inset
                            val segments = kotlin.math.ceil(width / 58.dp.toPx())
                                .toInt()
                                .coerceIn(1, 6)
                            val segmentWidth = width / segments
                            repeat(segments) { index ->
                                val x0 = inset + segmentWidth * index
                                val x1 = if (index == segments - 1) playedX else x0 + segmentWidth
                                val direction = if (index % 2 == 0) -1f else 1f
                                val amplitude = (if (isDragging) 0.78.dp else 0.58.dp).toPx() * direction
                                cubicTo(
                                    x0 + (x1 - x0) * 0.30f,
                                    centerY + amplitude,
                                    x0 + (x1 - x0) * 0.70f,
                                    centerY - amplitude,
                                    x1,
                                    centerY
                                )
                            }
                        }
                        drawPath(
                            path = whisperPath,
                            color = Color.White.copy(alpha = if (isDragging) 0.46f else 0.31f),
                            style = Stroke(width = 0.75.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    if (isDragging) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.18f),
                            start = androidx.compose.ui.geometry.Offset(playedX, centerY - 11.dp.toPx()),
                            end = androidx.compose.ui.geometry.Offset(playedX, centerY + 11.dp.toPx()),
                            strokeWidth = 0.8.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    fun diamondPath(halfSize: Float): Path = Path().apply {
                        moveTo(playedX, centerY - halfSize)
                        lineTo(playedX + halfSize, centerY)
                        lineTo(playedX, centerY + halfSize)
                        lineTo(playedX - halfSize, centerY)
                        close()
                    }

                    drawCircle(
                        color = activeColor.playerMix(secondaryColor, 0.44f)
                            .copy(alpha = if (isDragging) 0.18f else 0.11f),
                        radius = marker + 5.2.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(playedX, centerY)
                    )
                    drawPath(
                        path = diamondPath(marker + 1.8.dp.toPx()),
                        color = Color.Black.copy(alpha = 0.38f)
                    )
                    drawPath(
                        path = diamondPath(marker + 0.7.dp.toPx()),
                        brush = Brush.linearGradient(
                            colors = listOf(
                                activeColor.playerMix(Color.White, 0.30f),
                                secondaryColor.playerMix(Color.White, 0.16f)
                            ),
                            start = androidx.compose.ui.geometry.Offset(playedX - marker, centerY - marker),
                            end = androidx.compose.ui.geometry.Offset(playedX + marker, centerY + marker)
                        )
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.94f),
                        radius = if (isDragging) 1.9.dp.toPx() else 1.55.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(playedX, centerY)
                    )
                }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(if (isDragging) (durationMs * fraction).toLong() else positionMs),
                color = if (isDragging) Color.White else Color.White.copy(alpha = 0.68f),
                fontSize = if (compact) 10.5.sp else 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = formatDuration(durationMs),
                color = Color.White.copy(alpha = 0.50f),
                fontSize = if (compact) 10.5.sp else 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }
    }
}'''

text = replace_between(
    text,
    "@Composable\nprivate fun PlayerTimeline(",
    "@Composable\nprivate fun MainPlayerControls(",
    new_timeline,
    "PlayerTimeline",
)

old_spacing = "val playerItemSpacing = if (compactPlayer) 9.dp else 12.dp"
new_spacing = "val playerItemSpacing = if (compactPlayer) 8.dp else 10.dp"
if text.count(old_spacing) != 1:
    raise SystemExit(f"Expected one player item spacing value, found {text.count(old_spacing)}")
text = text.replace(old_spacing, new_spacing, 1)

old_metadata_padding = '''                            .padding(
                                horizontal = 4.dp,
                                vertical = if (compactPlayer) 1.dp else 2.dp
                            )'''
new_metadata_padding = '''                            .padding(horizontal = 4.dp)'''
if text.count(old_metadata_padding) != 1:
    raise SystemExit(f"Expected one player metadata padding block, found {text.count(old_metadata_padding)}")
text = text.replace(old_metadata_padding, new_metadata_padding, 1)

old_inner_shadow = '''    val artworkShadow by animateFloatAsState(
        targetValue = if (isPlaying) 27f else 17f,
        animationSpec = tween(190, easing = FastOutSlowInEasing),
        label = "player-artwork-shadow"
    )'''
new_inner_shadow = '''    val artworkShadow by animateFloatAsState(
        targetValue = if (isPlaying) 24f else 15f,
        animationSpec = tween(190, easing = FastOutSlowInEasing),
        label = "player-artwork-shadow"
    )'''
if text.count(old_inner_shadow) != 1:
    raise SystemExit(f"Expected one inner artwork shadow block, found {text.count(old_inner_shadow)}")
text = text.replace(old_inner_shadow, new_inner_shadow, 1)

old_card_shadow = '''    val artShadow by animateFloatAsState(
        targetValue = if (state.isPlaying) 27f else 17f,
        animationSpec = tween(190, easing = FastOutSlowInEasing),
        label = "artwork-shadow"
    )'''
new_card_shadow = '''    val artShadow by animateFloatAsState(
        targetValue = if (state.isPlaying) 24f else 15f,
        animationSpec = tween(190, easing = FastOutSlowInEasing),
        label = "artwork-shadow"
    )'''
if text.count(old_card_shadow) != 1:
    raise SystemExit(f"Expected one whole-card shadow block, found {text.count(old_card_shadow)}")
text = text.replace(old_card_shadow, new_card_shadow, 1)

engagement_start = "@OptIn(ExperimentalLayoutApi::class)\n@Composable\nprivate fun PlayerYoutubeEngagementRow("
engagement_end = "private fun youtubeCommentCountBadge("
if text.count(engagement_start) != 1 or text.count(engagement_end) != 1:
    raise SystemExit("Unable to isolate PlayerYoutubeEngagementRow")
engagement_from = text.index(engagement_start)
engagement_to = text.index(engagement_end, engagement_from)
engagement = text[engagement_from:engagement_to]

engagement_replacements = [
    (".padding(top = if (compact) 9.dp else 11.dp)", ".padding(top = if (compact) 7.dp else 9.dp)", 1),
    ("horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 9.dp)", "horizontalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 8.dp)", 1),
    ("verticalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 8.dp)", "verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 7.dp)", 1),
    (".height(if (compact) 40.dp else 42.dp)", ".height(if (compact) 38.dp else 40.dp)", 2),
    ("Modifier.size(if (compact) 19.dp else 20.dp)", "Modifier.size(if (compact) 18.dp else 19.dp)", 3),
    ("fontSize = if (compact) 12.5.sp else 13.sp", "fontSize = if (compact) 12.sp else 12.5.sp", 3),
    (".padding(horizontal = if (compact) 13.dp else 14.dp)", ".padding(horizontal = if (compact) 12.dp else 13.dp)", 1),
]
for old, new, expected in engagement_replacements:
    count = engagement.count(old)
    if count != expected:
        raise SystemExit(f"Engagement replacement mismatch for {old!r}: expected {expected}, found {count}")
    engagement = engagement.replace(old, new)

text = text[:engagement_from] + engagement + text[engagement_to:]

if text.count("private fun PlayerTimeline(") != 1:
    raise SystemExit("PlayerTimeline duplication detected")
if "timeline-ribbon-amplitude" in text or "waveformPath(" in text:
    raise SystemExit("Legacy timeline waveform was not fully removed")
if "timeline-rail-stroke" not in text or "whisperPath" not in text:
    raise SystemExit("Refined timeline markers are missing")

PATH.write_text(text, encoding="utf-8")
print("Applied refined player timeline, spacing, shadows, and engagement sizing")
