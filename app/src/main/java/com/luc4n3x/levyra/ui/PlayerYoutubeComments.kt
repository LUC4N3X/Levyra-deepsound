@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
package com.luc4n3x.levyra.ui

import androidx.compose.runtime.getValue
import com.luc4n3x.levyra.ui.theme.*
import com.luc4n3x.levyra.ui.components.levyraGroupedListShape
import androidx.compose.runtime.key
import com.luc4n3x.levyra.ui.player.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.YoutubeComment
import com.luc4n3x.levyra.domain.YoutubeCommentsState
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged




@Composable
internal fun PlayerYoutubeCommentsSheet(
    comments: YoutubeCommentsState,
    primary: Color,
    secondary: Color,
    strings: LevyraStrings,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit,
    onToggleReplies: (String) -> Unit,
    onLoadMoreReplies: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val sheetInteraction = remember { MutableInteractionSource() }

    LaunchedEffect(comments.nextToken, comments.loadingMore, comments.items.size, comments.error) {
        if (comments.error != null || comments.nextToken.isBlank()) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (
                    comments.nextToken.isNotBlank() &&
                    !comments.loadingMore &&
                    comments.items.isNotEmpty() &&
                    lastVisible >= comments.items.lastIndex - 2
                ) {
                    onLoadMore()
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.62f))
                .clickable(onClick = onDismiss)
        )

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .widthIn(max = 620.dp)
                .fillMaxHeight(0.82f)
                .navigationBarsPadding()
                .clickable(
                    interactionSource = sheetInteraction,
                    indication = null,
                    onClick = {}
                ),
            color = Color(0xFF101114),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .align(Alignment.CenterHorizontally)
                        .width(42.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.24f))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.totalComments.uppercase(),
                            color = primary.playerMix(Color.White, 0.55f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.4.sp
                        )
                        val commentCount = youtubeCommentCountBadge(comments.countText)
                        Text(
                            text = commentCount.ifBlank { "—" },
                            color = Color.White,
                            fontSize = 30.sp,
                            lineHeight = 34.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.8).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    PlayerRoundIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = strings.close,
                        size = 38.dp,
                        iconSize = 20.dp,
                        tint = Color.White.copy(alpha = 0.82f),
                        background = Color.White.copy(alpha = 0.08f),
                        borderColor = Color.Transparent,
                        onClick = onDismiss
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    primary.copy(alpha = 0.34f),
                                    secondary.copy(alpha = 0.28f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                when {
                    comments.loading && comments.items.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = primary.playerMix(Color.White, 0.55f),
                                strokeWidth = 3.dp
                            )
                        }
                    }
                    comments.disabled -> {
                        YoutubeCommentsEmptyState(
                            icon = Icons.Rounded.ChatBubbleOutline,
                            label = "${strings.totalComments}: —",
                            primary = primary
                        )
                    }
                    comments.error != null && comments.items.isEmpty() -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChatBubbleOutline,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.38f),
                                modifier = Modifier.size(38.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                color = primary.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, primary.copy(alpha = 0.34f)),
                                shape = CircleShape,
                                modifier = Modifier.pressable(onClick = onRetry)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        text = strings.check,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    comments.items.isEmpty() -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            YoutubeCommentsEmptyState(
                                icon = Icons.Rounded.ChatBubbleOutline,
                                label = if (comments.nextToken.isBlank()) {
                                    "${strings.totalComments}: 0"
                                } else {
                                    strings.totalComments
                                },
                                primary = primary,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (comments.nextToken.isNotBlank()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                TextButton(onClick = onLoadMore) {
                                    Text(
                                        text = strings.more,
                                        color = primary.playerMix(Color.White, 0.52f),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            itemsIndexed(
                                items = comments.items,
                                key = { _, comment -> comment.id },
                                contentType = { _, _ -> "youtube-comment" }
                            ) { index, comment ->
                                YoutubeCommentCard(
                                    comment = comment,
                                    shape = levyraGroupedListShape(index, comments.items.size),
                                    primary = primary,
                                    secondary = secondary,
                                    moreLabel = strings.more,
                                    onToggleReplies = { onToggleReplies(comment.id) },
                                    onLoadMoreReplies = { onLoadMoreReplies(comment.id) }
                                )
                            }
                            if (comments.loadingMore) {
                                item(key = "comments-loading-more") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.4.dp,
                                            color = primary.playerMix(Color.White, 0.55f)
                                        )
                                    }
                                }
                            } else if (comments.error != null && comments.nextToken.isNotBlank()) {
                                item(key = "comments-retry-more") {
                                    TextButton(
                                        onClick = onRetryLoadMore,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = null,
                                            tint = primary.playerMix(Color.White, 0.52f),
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(7.dp))
                                        Text(
                                            text = strings.check,
                                            color = primary.playerMix(Color.White, 0.52f),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else if (comments.nextToken.isNotBlank()) {
                                item(key = "comments-more") {
                                    TextButton(
                                        onClick = onLoadMore,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = strings.more,
                                            color = primary.playerMix(Color.White, 0.52f),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YoutubeCommentsEmptyState(
    icon: ImageVector,
    label: String,
    primary: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            color = primary.copy(alpha = 0.10f),
            shape = CircleShape
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.44f),
                modifier = Modifier.padding(16.dp).size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.52f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun YoutubeCommentCard(
    comment: YoutubeComment,
    shape: Shape,
    primary: Color,
    secondary: Color,
    moreLabel: String,
    onToggleReplies: () -> Unit,
    onLoadMoreReplies: () -> Unit
) {
    Surface(
        color = if (comment.pinned) primary.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f),
        shape = shape
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            YoutubeCommentHeader(comment, primary)

            Text(
                text = comment.text,
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal
            )

            YoutubeCommentActions(comment, secondary, onToggleReplies)

            YoutubeCommentReplies(comment, primary, moreLabel, onToggleReplies, onLoadMoreReplies)
        }
    }
}

@Composable
private fun YoutubeCommentHeader(comment: YoutubeComment, primary: Color) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        YoutubeCommentAvatar(
            url = comment.authorAvatarUrl,
            author = comment.author,
            tint = primary,
            modifier = Modifier.size(38.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = comment.author,
                    color = Color.White.copy(alpha = 0.94f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (comment.verifiedAuthor) {
                    Icon(
                        imageVector = Icons.Rounded.Verified,
                        contentDescription = null,
                        tint = primary.playerMix(Color.White, 0.44f),
                        modifier = Modifier.size(13.dp)
                    )
                }
                if (comment.pinned) {
                    Icon(
                        imageVector = Icons.Rounded.PushPin,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.46f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            if (comment.publishedText.isNotBlank()) {
                Text(
                    text = comment.publishedText,
                    color = Color.White.copy(alpha = 0.46f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        if (comment.heartedByUploader) {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = Color(0xFFFF5B72),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun YoutubeCommentActions(comment: YoutubeComment, secondary: Color, onToggleReplies: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (comment.likeCountText.isNotBlank()) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.07f))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ThumbUp,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.62f),
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = comment.likeCountText,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (comment.replyCount > 0 && comment.replyToken.isNotBlank()) {
            val replyPressSource = remember { MutableInteractionSource() }
            val replyPressed by replyPressSource.collectIsPressedAsState()
            val replyCorner by animateDpAsState(
                targetValue = if (replyPressed || comment.repliesExpanded) 10.dp else 18.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "comment-reply-corner"
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(replyCorner))
                    .background(secondary.copy(alpha = if (comment.repliesExpanded) 0.34f else 0.20f))
                    .clickable(
                        interactionSource = replyPressSource,
                        indication = LocalIndication.current,
                        onClick = onToggleReplies
                    )
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Reply,
                    contentDescription = null,
                    tint = secondary.playerMix(Color.White, 0.66f),
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = comment.replyCount.toString(),
                    color = secondary.playerMix(Color.White, 0.66f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun YoutubeCommentReplies(
    comment: YoutubeComment,
    primary: Color,
    moreLabel: String,
    onToggleReplies: () -> Unit,
    onLoadMoreReplies: () -> Unit
) {
    AnimatedVisibility(
        visible = comment.repliesExpanded,
        enter = fadeIn(tween(160)) + slideInVertically(initialOffsetY = { -it / 5 }),
        exit = fadeOut(tween(120))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 2.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.22f))
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            comment.replies.forEach { reply ->
                YoutubeCommentReply(reply = reply, primary = primary)
            }
            if (comment.repliesLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = primary.playerMix(Color.White, 0.52f)
                    )
                }
            } else if (comment.repliesError != null && comment.replies.isEmpty()) {
                TextButton(onClick = onToggleReplies, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = primary.playerMix(Color.White, 0.52f),
                        modifier = Modifier.size(15.dp)
                    )
                }
            } else if (comment.repliesNextToken.isNotBlank()) {
                TextButton(onClick = onLoadMoreReplies, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = moreLabel,
                        color = primary.playerMix(Color.White, 0.52f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun YoutubeCommentReply(
    reply: YoutubeComment,
    primary: Color
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        YoutubeCommentAvatar(
            url = reply.authorAvatarUrl,
            author = reply.author,
            modifier = Modifier.size(28.dp)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = reply.author,
                    color = Color.White.copy(alpha = 0.86f),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (reply.verifiedAuthor) {
                    Icon(
                        imageVector = Icons.Rounded.Verified,
                        contentDescription = null,
                        tint = primary.playerMix(Color.White, 0.44f),
                        modifier = Modifier.size(11.dp)
                    )
                }
                if (reply.heartedByUploader) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFFF5B72),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Text(
                text = reply.text,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (reply.publishedText.isNotBlank()) {
                    Text(
                        text = reply.publishedText,
                        color = Color.White.copy(alpha = 0.36f),
                        fontSize = 9.5.sp
                    )
                }
                if (reply.likeCountText.isNotBlank()) {
                    Text(
                        text = "👍 ${reply.likeCountText}",
                        color = Color.White.copy(alpha = 0.36f),
                        fontSize = 9.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun YoutubeCommentAvatar(
    url: String,
    author: String,
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = tint.copy(alpha = 0.24f)
    ) {
        if (url.isNotBlank()) {
            AsyncImage(
                model = url,
                contentDescription = author,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = author.trim().firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString().orEmpty(),
                    color = tint.playerMix(Color.White, 0.7f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
