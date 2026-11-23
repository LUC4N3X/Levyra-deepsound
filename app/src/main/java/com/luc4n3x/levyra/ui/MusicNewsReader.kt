package com.luc4n3x.levyra.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.data.news.MusicNewsArticle
import com.luc4n3x.levyra.data.news.MusicNewsBlock
import com.luc4n3x.levyra.data.news.MusicNewsReaderState
import com.luc4n3x.levyra.data.news.MusicNewsStory
import com.luc4n3x.levyra.ui.i18n.NewsReaderCopy
import com.luc4n3x.levyra.ui.i18n.exploreEditorialCopy
import com.luc4n3x.levyra.ui.i18n.newsReaderCopy
import com.luc4n3x.levyra.viewmodel.MusicNewsReaderSession
import kotlin.math.roundToInt

@Composable
internal fun MusicNewsReaderOverlay(languageCode: String, contentReady: Boolean, inPictureInPicture: Boolean) {
    if (!contentReady || inPictureInPicture) return
    val state by MusicNewsReaderSession.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    MusicNewsReaderHost(
        state = state,
        languageCode = languageCode,
        onClose = MusicNewsReaderSession::close,
        onOpenOriginal = { story -> openNewsInBrowser(context, story.url) }
    )
}

private fun openNewsInBrowser(context: Context, url: String) {
    val uri = Uri.parse(url)
    if (!uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank()) return
    val intent = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
    if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
    }
}

@Composable
internal fun MusicNewsReaderHost(
    state: MusicNewsReaderState?,
    languageCode: String,
    onClose: () -> Unit,
    onOpenOriginal: (MusicNewsStory) -> Unit
) {
    var shown by remember { mutableStateOf<MusicNewsReaderState?>(null) }
    if (state != null && state != shown) shown = state
    AnimatedVisibility(
        visible = state != null,
        enter = slideInVertically(tween(340)) { it / 10 } + fadeIn(tween(260)),
        exit = slideOutVertically(tween(220)) { it / 10 } + fadeOut(tween(180))
    ) {
        shown?.let { current ->
            key(current.story.url) { MusicNewsReader(current, languageCode, onClose, onOpenOriginal) }
        }
    }
}

@Composable
internal fun MusicNewsReader(
    state: MusicNewsReaderState,
    languageCode: String,
    onClose: () -> Unit,
    onOpenOriginal: (MusicNewsStory) -> Unit
) {
    BackHandler(onBack = onClose)
    val copy = remember(languageCode) { newsReaderCopy(languageCode) }
    val editorial = remember(languageCode) { exploreEditorialCopy(languageCode) }
    val colors = MaterialTheme.colorScheme
    val story = state.story
    val article = state.article
    val title = article?.title ?: story.title
    val hero = article?.heroImageUrl?.takeIf(String::isNotBlank) ?: story.imageUrl
    val age = remember(story.publishedAtMs, editorial) {
        exploreNewsAgeLabel(editorial, System.currentTimeMillis(), story.publishedAtMs)
    }
    val readingMinutes = remember(article) { article?.let(::readingMinutes) }
    val leadIndex = remember(article) { article?.blocks?.indexOfFirst { it is MusicNewsBlock.Paragraph } ?: -1 }
    val host = remember(story.url) { Uri.parse(story.url).host.orEmpty().removePrefix("www.") }
    val listState = rememberLazyListState()
    val collapsed by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val barAlpha by animateFloatAsState(if (collapsed) 1f else 0f, tween(220), label = "news-reader-bar")
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(Modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset + 32.dp)
        ) {
            item(key = "hero", contentType = "hero") {
                if (hero.isBlank()) {
                    Spacer(Modifier.statusBarsPadding().height(80.dp))
                } else {
                    NewsReaderHero(hero) {
                        if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset * 0.4f else 0f
                    }
                }
            }
            item(key = "header", contentType = "header") {
                NewsReaderHeader(
                    source = story.source,
                    age = age,
                    title = title,
                    author = article?.author.orEmpty(),
                    readingTime = readingMinutes?.let { copy.readingTime.replace("{count}", it.toString()) },
                    copy = copy,
                    modifier = if (hero.isBlank()) Modifier else Modifier.pullUp(HERO_OVERLAP)
                )
            }
            when {
                article != null -> items(
                    count = article.blocks.size,
                    key = { "block-$it" },
                    contentType = { article.blocks[it]::class.simpleName }
                ) { index -> NewsReaderBlock(article.blocks[index], lead = index == leadIndex) }
                state.loading -> item(key = "loading", contentType = "loading") { NewsReaderSkeleton() }
                else -> item(key = "unavailable", contentType = "unavailable") { NewsReaderUnavailable(copy.unavailable) }
            }
            if (!state.loading) {
                item(key = "original", contentType = "original") {
                    NewsReaderSourceCard(story.source, host, copy.readOn.replace("{source}", story.source)) {
                        onOpenOriginal(story)
                    }
                }
            }
        }
        NewsReaderTopBar(
            title = title,
            close = copy.close,
            barAlpha = { barAlpha },
            progress = { readingProgress(listState) },
            onClose = onClose
        )
    }
}

private val HERO_OVERLAP = 64.dp
private const val CHARACTERS_PER_MINUTE = 1100

private fun Modifier.pullUp(amount: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val shift = amount.roundToPx().coerceAtMost(placeable.height)
    layout(placeable.width, placeable.height - shift) { placeable.place(0, -shift) }
}

private fun readingProgress(listState: LazyListState): Float {
    val info = listState.layoutInfo
    val total = info.totalItemsCount
    if (total <= 1) return 0f
    val last = info.visibleItemsInfo.lastOrNull() ?: return 0f
    val visibleFraction = ((info.viewportEndOffset - last.offset).toFloat() / last.size.coerceAtLeast(1)).coerceIn(0f, 1f)
    return ((last.index + visibleFraction) / total).coerceIn(0f, 1f)
}

private fun readingMinutes(article: MusicNewsArticle): Int {
    val characters = article.blocks.sumOf { block ->
        when (block) {
            is MusicNewsBlock.Paragraph -> block.html.length
            is MusicNewsBlock.Quote -> block.html.length
            is MusicNewsBlock.Bullet -> block.html.length
            is MusicNewsBlock.Heading -> block.text.length
            is MusicNewsBlock.Image -> 0
        }
    }
    return (characters / CHARACTERS_PER_MINUTE.toFloat()).roundToInt().coerceAtLeast(1)
}

@Composable
private fun NewsReaderTopBar(
    title: String,
    close: String,
    barAlpha: () -> Float,
    progress: () -> Float,
    onClose: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth()) {
        Box(Modifier.matchParentSize().graphicsLayer { alpha = barAlpha() }.background(colors.surfaceContainer))
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().height(64.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onClose,
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.38f),
                    contentColor = Color.White,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = close, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 16.dp).graphicsLayer { alpha = barAlpha() }
                )
            }
            Box(
                Modifier.fillMaxWidth().height(2.dp).graphicsLayer {
                    alpha = barAlpha()
                    scaleX = progress()
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }.background(colors.primary)
            )
        }
    }
}

@Composable
private fun NewsReaderHero(url: String, parallax: () -> Float) {
    val background = MaterialTheme.colorScheme.background
    Box(Modifier.fillMaxWidth().aspectRatio(0.86f)) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer { translationY = parallax() }
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.42f),
                    0.2f to Color.Transparent,
                    0.55f to Color.Transparent,
                    0.86f to background.copy(alpha = 0.82f),
                    1f to background
                )
            )
        )
    }
}

@Composable
private fun NewsReaderHeader(
    source: String,
    age: String?,
    title: String,
    author: String,
    readingTime: String?,
    copy: NewsReaderCopy,
    modifier: Modifier
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = colors.primaryContainer, contentColor = colors.onPrimaryContainer) {
                Text(
                    source.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            if (!age.isNullOrBlank()) {
                Spacer(Modifier.width(10.dp))
                Text(age, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium.copy(lineHeight = 38.sp),
            fontWeight = FontWeight.Bold,
            color = colors.onBackground,
            modifier = Modifier.semantics { heading() }
        )
        if (author.isNotBlank() || readingTime != null) {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (author.isNotBlank()) {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(colors.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            author.trim().first().uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSecondaryContainer
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    listOfNotNull(author.takeIf(String::isNotBlank)?.let { copy.byline.replace("{author}", it) }, readingTime)
                        .joinToString("  ·  "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.6f))
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun NewsReaderBlock(block: MusicNewsBlock, lead: Boolean) {
    val colors = MaterialTheme.colorScheme
    val bodyStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 28.sp)
    when (block) {
        is MusicNewsBlock.Paragraph -> Text(
            rememberNewsHtml(block.html),
            style = if (lead) bodyStyle.copy(fontSize = 19.sp, lineHeight = 31.sp, fontWeight = FontWeight.Medium) else bodyStyle,
            color = if (lead) colors.onBackground else colors.onBackground.copy(alpha = 0.88f),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 9.dp)
        )
        is MusicNewsBlock.Heading -> Text(
            block.text,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground,
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 2.dp).semantics { heading() }
        )
        is MusicNewsBlock.Quote -> Surface(
            shape = RoundedCornerShape(24.dp),
            color = colors.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(Modifier.height(IntrinsicSize.Min).padding(20.dp)) {
                Box(Modifier.width(4.dp).fillMaxHeight().clip(CircleShape).background(colors.primary))
                Spacer(Modifier.width(16.dp))
                Text(
                    rememberNewsHtml(block.html),
                    style = MaterialTheme.typography.titleMedium.copy(lineHeight = 27.sp),
                    fontStyle = FontStyle.Italic,
                    color = colors.onSurface
                )
            }
        }
        is MusicNewsBlock.Image -> Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            AsyncImage(
                model = block.url,
                contentDescription = block.caption.ifBlank { null },
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(colors.surfaceContainerHigh)
            )
            if (block.caption.isNotBlank()) {
                Text(
                    block.caption,
                    style = MaterialTheme.typography.labelMedium.copy(lineHeight = 18.sp),
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp)
                )
            }
        }
        is MusicNewsBlock.Bullet -> Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 5.dp)) {
            Box(Modifier.padding(top = 11.dp).size(6.dp).clip(CircleShape).background(colors.primary))
            Spacer(Modifier.width(14.dp))
            Text(rememberNewsHtml(block.html), style = bodyStyle, color = colors.onBackground.copy(alpha = 0.88f))
        }
    }
}

@Composable
private fun rememberNewsHtml(html: String): AnnotatedString {
    val link = MaterialTheme.colorScheme.primary
    return remember(html, link) {
        AnnotatedString.fromHtml(
            html,
            linkStyles = TextLinkStyles(
                SpanStyle(color = link, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)
            )
        )
    }
}

@Composable
private fun NewsReaderSkeleton() {
    val pulse by rememberInfiniteTransition(label = "news-reader-pulse").animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "news-reader-pulse-alpha"
    )
    val tone = MaterialTheme.colorScheme.onSurface
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        listOf(1f, 0.94f, 0.98f, 0.62f, 1f, 0.88f, 0.4f).forEach { fraction ->
            Box(
                Modifier.fillMaxWidth(fraction).height(14.dp).clip(CircleShape)
                    .graphicsLayer { alpha = pulse }
                    .background(tone.copy(alpha = 0.12f))
            )
        }
    }
}

@Composable
private fun NewsReaderUnavailable(message: String) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp)
        )
    }
}

@Composable
private fun NewsReaderSourceCard(source: String, host: String, label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = colors.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(colors.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    source.trim().firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "♪",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onPrimaryContainer
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                if (host.isNotBlank()) {
                    Text(host, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
            Icon(
                Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.padding(start = 8.dp).size(20.dp)
            )
        }
    }
}
