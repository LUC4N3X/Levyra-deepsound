from pathlib import Path
import textwrap

path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
source = path.read_text()
start_marker = "@Composable\nprivate fun ContinueListeningCard("
end_marker = "\n@Composable\nprivate fun HomeShortcutRow("

if source.count(start_marker) != 1:
    raise SystemExit(f"Expected one ContinueListeningCard, found {source.count(start_marker)}")

start = source.index(start_marker)
end = source.index(end_marker, start)

replacement = textwrap.dedent(
    '''
    @Composable
    private fun ContinueListeningCard(
        track: Track,
        progress: Float,
        onResume: () -> Unit
    ) {
        val accentStart = if (track.accentStart == 0) LevyraCyan else Color(track.accentStart)
        val accentEnd = if (track.accentEnd == 0) LevyraViolet else Color(track.accentEnd)
        val safeProgress = progress.coerceIn(0f, 1f)
        val shape = RoundedCornerShape(19.dp)

        Surface(
            color = Color.Transparent,
            shape = shape,
            border = BorderStroke(Dp.Hairline, LevyraAdaptiveHairline),
            modifier = Modifier
                .wrapContentWidth()
                .widthIn(max = 318.dp)
                .heightIn(min = 76.dp)
                .pressable(onClick = onResume)
        ) {
            Box(
                modifier = Modifier.background(
                    cinematicGlassBrush(accentStart, accentEnd, 0.46f)
                )
            ) {
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .align(Alignment.CenterStart)
                        .offset(x = (-38).dp)
                        .background(
                            Brush.radialGradient(
                                listOf(accentStart.copy(alpha = 0.17f), Color.Transparent)
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .size(118.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = 44.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(accentEnd.copy(alpha = 0.18f), Color.Transparent)
                            )
                        )
                )
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    CoverImage(
                        track = track,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                1.dp,
                                Color.White.copy(alpha = 0.16f),
                                RoundedCornerShape(14.dp)
                            ),
                        highRes = false
                    )
                    Column(
                        modifier = Modifier.widthIn(max = 172.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Headphones,
                                contentDescription = null,
                                tint = LevyraCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = LocalLevyraStrings.current.continueListening,
                                color = LevyraCyan,
                                fontSize = 9.6.sp,
                                lineHeight = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = track.title,
                            color = LevyraText,
                            fontSize = 14.5.sp,
                            lineHeight = 16.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            color = LevyraMuted,
                            fontSize = 10.8.sp,
                            lineHeight = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        color = LevyraAdaptiveChip,
                        shape = CircleShape,
                        border = BorderStroke(Dp.Hairline, LevyraAdaptiveHairline),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = LevyraText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(safeProgress)
                        .height(3.dp)
                        .clip(RoundedCornerShape(topEnd = 3.dp))
                        .background(Brush.horizontalGradient(listOf(accentStart, accentEnd)))
                )
            }
        }
    }
    '''
).strip()

path.write_text(source[:start] + replacement + source[end:])
