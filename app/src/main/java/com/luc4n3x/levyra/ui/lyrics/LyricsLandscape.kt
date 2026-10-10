package com.luc4n3x.levyra.ui.lyrics

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.luc4n3x.levyra.domain.LevyraCanvasQuality
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.motion.MotionArtwork
import com.luc4n3x.levyra.ui.InstantArtworkPlaceholder
import com.luc4n3x.levyra.ui.MotionArtworkLayer
import com.luc4n3x.levyra.ui.MotionArtworkPresentation
import com.luc4n3x.levyra.ui.artwork.SeamlessArtworkImage
import com.luc4n3x.levyra.ui.components.LevyraPlayPauseGlyph
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.preferredPlayerArtworkUrl

internal const val LYRICS_LANDSCAPE_MIN_WIDTH_DP = 480
internal const val LYRICS_LANDSCAPE_CHROME_HEIGHT_DP = 62
internal const val LYRICS_LANDSCAPE_PANE_GAP_DP = 12

private const val LYRICS_LANDSCAPE_MIN_LYRICS_WIDTH_DP = 360f
private const val LYRICS_LANDSCAPE_ARTWORK_MAX_DP = 420f
private const val LYRICS_LANDSCAPE_ARTWORK_MIN_DP = 96f
private const val LYRICS_LANDSCAPE_PANE_HORIZONTAL_PADDING_DP = 28f
private const val LYRICS_LANDSCAPE_PANE_VERTICAL_PADDING_DP = 20f
private const val LYRICS_LANDSCAPE_METADATA_RESERVE_DP = 128f
private const val LYRICS_LANDSCAPE_BACKDROP_DECODE_PX = 96
private val LyricsLandscapeBase = Color(0xFF07080C)

internal data class LyricsLandscapeMetrics(
    val artworkPaneWidthDp: Float,
    val artworkSizeDp: Float,
    val lyricsPaneWidthDp: Float
)

internal fun lyricsLandscapeLayoutActive(orientation: Int, widthDp: Int, heightDp: Int): Boolean =
    orientation == Configuration.ORIENTATION_LANDSCAPE &&
        widthDp >= LYRICS_LANDSCAPE_MIN_WIDTH_DP &&
        widthDp > heightDp

internal fun lyricsLandscapeLayoutActive(configuration: Configuration): Boolean =
    lyricsLandscapeLayoutActive(
        orientation = configuration.orientation,
        widthDp = configuration.screenWidthDp,
        heightDp = configuration.screenHeightDp
    )

internal fun lyricsLandscapeMetrics(widthDp: Float, heightDp: Float): LyricsLandscapeMetrics {
    val safeWidth = widthDp.coerceAtLeast(0f)
    val safeHeight = heightDp.coerceAtLeast(0f)
    val fraction = when {
        safeWidth < 720f -> 0.40f
        safeWidth < 1000f -> 0.42f
        else -> 0.45f
    }
    val paneWidth = (safeWidth * fraction)
        .coerceAtMost(safeWidth - LYRICS_LANDSCAPE_MIN_LYRICS_WIDTH_DP)
        .coerceAtLeast(safeWidth * 0.32f)
    val artworkByWidth = paneWidth - LYRICS_LANDSCAPE_PANE_HORIZONTAL_PADDING_DP * 2f
    val artworkByHeight = safeHeight -
        LYRICS_LANDSCAPE_PANE_VERTICAL_PADDING_DP * 2f -
        LYRICS_LANDSCAPE_METADATA_RESERVE_DP
    val artwork = minOf(artworkByWidth, artworkByHeight, LYRICS_LANDSCAPE_ARTWORK_MAX_DP)
        .coerceAtLeast(LYRICS_LANDSCAPE_ARTWORK_MIN_DP)
    return LyricsLandscapeMetrics(
        artworkPaneWidthDp = paneWidth,
        artworkSizeDp = artwork,
        lyricsPaneWidthDp = safeWidth - paneWidth
    )
}

@Composable
internal fun LyricsImmersiveSystemBars(active: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, active) {
        val window = view.context.findActivity()?.window
        if (!active || window == null) return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, view)
        val previousBehavior = controller.systemBarsBehavior
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = previousBehavior
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
internal fun LyricsLandscapeBackdrop(
    track: Track?,
    accentStart: Color,
    accentEnd: Color,
    artworkAlpha: Float,
    modifier: Modifier = Modifier
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LyricsLandscapeBase)
    ) {
        if (track != null && artworkAlpha > 0f) {
            val context = LocalContext.current
            val request = remember(context, track.id, track.largeThumbnailUrl, track.thumbnailUrl) {
                ImageRequest.Builder(context)
                    .data(track.largeThumbnailUrl.ifBlank { track.thumbnailUrl })
                    .size(LYRICS_LANDSCAPE_BACKDROP_DECODE_PX, LYRICS_LANDSCAPE_BACKDROP_DECODE_PX)
                    .crossfade(false)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = artworkAlpha
                        scaleX = 1.3f
                        scaleY = 1.3f
                    }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val artworkSideX = if (rtl) size.width * 0.78f else size.width * 0.22f
                    val glow = Brush.radialGradient(
                        colors = listOf(accentStart.copy(alpha = 0.20f), accentEnd.copy(alpha = 0.06f), Color.Transparent),
                        center = Offset(artworkSideX, size.height * 0.46f),
                        radius = size.maxDimension * 0.55f
                    )
                    val readingScrim = Brush.horizontalGradient(
                        colors = if (rtl) {
                            listOf(Color.Black.copy(alpha = 0.72f), Color.Black.copy(alpha = 0.60f), Color.Black.copy(alpha = 0.22f))
                        } else {
                            listOf(Color.Black.copy(alpha = 0.22f), Color.Black.copy(alpha = 0.60f), Color.Black.copy(alpha = 0.72f))
                        }
                    )
                    val vignette = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.maxDimension * 0.75f
                    )
                    onDrawBehind {
                        drawRect(glow)
                        drawRect(readingScrim)
                        drawRect(vignette)
                    }
                }
        )
    }
}

@Composable
internal fun LyricsLandscapeArtworkPane(
    track: Track?,
    fallbackTitle: String,
    artworkSize: Dp,
    motionArtwork: MotionArtwork?,
    motionEnabled: Boolean,
    isPlaying: Boolean,
    canvasQuality: LevyraCanvasQuality,
    accent: Color,
    animationsEnabled: Boolean,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    val artworkShape = RoundedCornerShape(22.dp)
    val artworkUrl = track?.let(::preferredPlayerArtworkUrl).orEmpty()
    val entrance = remember { Animatable(if (animationsEnabled) 0f else 1f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, tween(durationMillis = 320))
    }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = LYRICS_LANDSCAPE_PANE_HORIZONTAL_PADDING_DP.dp,
                vertical = LYRICS_LANDSCAPE_PANE_VERTICAL_PADDING_DP.dp
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(artworkSize)
                .graphicsLayer {
                    val progress = entrance.value
                    alpha = progress
                    val scale = 0.97f + 0.03f * progress
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(
                    elevation = 24.dp,
                    shape = artworkShape,
                    clip = false,
                    ambientColor = accent.copy(alpha = 0.40f),
                    spotColor = Color.Black.copy(alpha = 0.80f)
                )
                .clip(artworkShape)
                .background(Color.Black.copy(alpha = 0.24f), artworkShape)
                .border(1.dp, Color.White.copy(alpha = 0.10f), artworkShape)
        ) {
            if (track != null) {
                MotionArtworkLayer(
                    artwork = motionArtwork,
                    enabled = motionEnabled,
                    isPlaying = isPlaying,
                    cornerRadius = 22.dp,
                    presentation = MotionArtworkPresentation.Card,
                    quality = canvasQuality,
                    modifier = Modifier.fillMaxSize()
                ) {
                    SeamlessArtworkImage(url = artworkUrl, modifier = Modifier.fillMaxSize()) {
                        InstantArtworkPlaceholder(track = track, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier.width(artworkSize),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = track?.title ?: fallbackTitle,
                color = Color.White,
                fontSize = 20.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            track?.artist?.takeIf(String::isNotBlank)?.let { artist ->
                Text(
                    text = artist,
                    color = Color.White.copy(alpha = 0.68f),
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (track != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.width(artworkSize),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LyricsLandscapeControl(
                    icon = Icons.Rounded.SkipPrevious,
                    contentDescription = strings.previous,
                    onClick = onPrevious
                )
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color.White.copy(alpha = 0.14f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                        .clip(CircleShape)
                        .levyraPressable(onClick = onTogglePlay, pressedScale = LevyraPressScale.Control, role = Role.Button),
                    contentAlignment = Alignment.Center
                ) {
                    LevyraPlayPauseGlyph(
                        playing = isPlaying,
                        color = Color.White,
                        contentDescription = if (isPlaying) strings.pause else strings.play,
                        modifier = Modifier.size(26.dp)
                    )
                }
                LyricsLandscapeControl(
                    icon = Icons.Rounded.SkipNext,
                    contentDescription = strings.next,
                    onClick = onNext
                )
            }
        }
    }
}

@Composable
private fun LyricsLandscapeControl(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .levyraPressable(onClick = onClick, pressedScale = LevyraPressScale.Control, role = Role.Button),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = Color.White.copy(alpha = 0.88f), modifier = Modifier.size(26.dp))
    }
}

@Composable
internal fun LyricsLandscapeChromeScrim(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                listOf(LyricsLandscapeBase.copy(alpha = 0.92f), LyricsLandscapeBase.copy(alpha = 0.55f), Color.Transparent)
            )
        )
    )
}
