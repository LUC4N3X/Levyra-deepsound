from pathlib import Path

path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
text = path.read_text(encoding="utf-8")

old = '''    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(220)) + slideInVertically(initialOffsetY = { it / 3 }),
        exit = fadeOut(animationSpec = tween(140))
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (compact) 7.dp else 9.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 7.dp)
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.085f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.105f)),
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.height(if (compact) 38.dp else 40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.padding(
                            start = if (compact) 12.dp else 13.dp,
                            end = if (compact) 10.dp else 11.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ThumbUp,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = if (hasLikes) 0.94f else 0.48f),
                            modifier = Modifier.size(if (compact) 18.dp else 19.dp)
                        )
                        if (hasLikes) {
                            Text(
                                text = compactYoutubeCount(track.youtubeLikeCount),
                                color = Color.White.copy(alpha = 0.94f),
                                fontSize = if (compact) 12.sp else 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(if (compact) 23.dp else 24.dp)
                            .background(Color.White.copy(alpha = 0.14f))
                    )
                    Row(
                        modifier = Modifier.padding(
                            start = if (compact) 10.dp else 11.dp,
                            end = if (compact) 12.dp else 13.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ThumbDown,
                            contentDescription = null,
                            tint = if (hasDislikeEstimate) {
                                secondary.playerMix(Color.White, 0.58f)
                            } else {
                                Color.White.copy(alpha = 0.48f)
                            },
                            modifier = Modifier.size(if (compact) 18.dp else 19.dp)
                        )
                        when {
                            engagement.dislikeEstimateLoading -> CircularProgressIndicator(
                                modifier = Modifier.size(if (compact) 12.dp else 13.dp),
                                strokeWidth = 1.8.dp,
                                color = secondary.playerMix(Color.White, 0.58f)
                            )
                            hasDislikeEstimate -> Text(
                                text = "~${compactYoutubeCount(engagement.estimatedDislikeCount)}",
                                color = Color.White.copy(alpha = 0.90f),
                                fontSize = if (compact) 12.sp else 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Box(
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
        }
    }
'''

new = '''    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(220)) + slideInVertically(initialOffsetY = { it / 3 }),
        exit = fadeOut(animationSpec = tween(140))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (compact) 7.dp else 9.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.085f),
                border = BorderStroke(
                    1.dp,
                    if (comments.visible) primary.copy(alpha = 0.46f) else Color.White.copy(alpha = 0.105f)
                ),
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.padding(
                            start = if (compact) 12.dp else 13.dp,
                            end = if (compact) 10.dp else 11.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ThumbUp,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = if (hasLikes) 0.94f else 0.48f),
                            modifier = Modifier.size(if (compact) 18.dp else 19.dp)
                        )
                        if (hasLikes) {
                            Text(
                                text = compactYoutubeCount(track.youtubeLikeCount),
                                color = Color.White.copy(alpha = 0.94f),
                                fontSize = if (compact) 12.sp else 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(Color.White.copy(alpha = 0.14f))
                    )
                    Row(
                        modifier = Modifier.padding(
                            start = if (compact) 10.dp else 11.dp,
                            end = if (compact) 10.dp else 11.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ThumbDown,
                            contentDescription = null,
                            tint = if (hasDislikeEstimate) {
                                secondary.playerMix(Color.White, 0.58f)
                            } else {
                                Color.White.copy(alpha = 0.48f)
                            },
                            modifier = Modifier.size(if (compact) 18.dp else 19.dp)
                        )
                        when {
                            engagement.dislikeEstimateLoading -> CircularProgressIndicator(
                                modifier = Modifier.size(if (compact) 12.dp else 13.dp),
                                strokeWidth = 1.8.dp,
                                color = secondary.playerMix(Color.White, 0.58f)
                            )
                            hasDislikeEstimate -> Text(
                                text = "~${compactYoutubeCount(engagement.estimatedDislikeCount)}",
                                color = Color.White.copy(alpha = 0.90f),
                                fontSize = if (compact) 12.sp else 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(Color.White.copy(alpha = 0.14f))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .sizeIn(minWidth = 48.dp)
                            .pressable(enabled = canOpenComments, onClick = onComments)
                            .padding(horizontal = if (compact) 10.dp else 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChatBubbleOutline,
                                contentDescription = null,
                                tint = if (canOpenComments) {
                                    primary.playerMix(Color.White, 0.58f)
                                } else {
                                    Color.White.copy(alpha = 0.42f)
                                },
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
                }
            }
        }
    }
'''

count = text.count(old)
if count != 1:
    raise SystemExit(f"expected exactly one engagement block, found {count}")

path.write_text(text.replace(old, new, 1), encoding="utf-8")
