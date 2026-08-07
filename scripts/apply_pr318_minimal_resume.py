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
        val shape = RoundedCornerShape(18.dp)

        Surface(
            color = Color.Transparent,
            shape = shape,
            border = BorderStroke(Dp.Hairline, LevyraAdaptiveHairline),
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .pressable(onClick = onResume)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.045f),
                                accentStart.copy(alpha = 0.035f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                // A quiet orbit motif gives the resume card its own signature without adding visual noise.
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = 34.dp)
                        .border(1.dp, accentEnd.copy(alpha = 0.10f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = 12.dp)
                        .border(1.dp, accentStart.copy(alpha = 0.08f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-34).dp, y = 12.dp)
                        .background(accentStart.copy(alpha = 0.42f), CircleShape)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CoverImage(
                        track = track,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                Dp.Hairline,
                                Color.White.copy(alpha = 0.12f),
                                RoundedCornerShape(12.dp)
                            ),
                        highRes = false
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .height(2.dp)
                                    .background(
                                        Brush.horizontalGradient(listOf(accentStart, accentEnd)),
                                        RoundedCornerShape(99.dp)
                                    )
                            )
                            Text(
                                text = LocalLevyraStrings.current.continueListening,
                                color = LevyraMuted.copy(alpha = 0.92f),
                                fontSize = 9.2.sp,
                                lineHeight = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = track.title,
                            color = LevyraText,
                            fontSize = 14.2.sp,
                            lineHeight = 15.8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            color = LevyraMuted,
                            fontSize = 10.4.sp,
                            lineHeight = 11.8.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.055f),
                        shape = CircleShape,
                        border = BorderStroke(Dp.Hairline, Color.White.copy(alpha = 0.11f)),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = LevyraText,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 68.dp, end = 52.dp, bottom = 5.dp)
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(safeProgress)
                            .height(1.5.dp)
                            .background(accentStart.copy(alpha = 0.68f))
                    )
                }
            }
        }
    }
    '''
).strip()

path.write_text(source[:start] + replacement + source[end:])
