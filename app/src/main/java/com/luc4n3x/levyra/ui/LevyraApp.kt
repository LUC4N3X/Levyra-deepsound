@file:OptIn(androidx.media3.common.util.UnstableApi::class)
package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.R
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Subject
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.draw.scale
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.luc4n3x.levyra.data.LevyraArtworkCache
import com.luc4n3x.levyra.player.LevyraPipBridge
import com.luc4n3x.levyra.player.PlaybackService
import com.luc4n3x.levyra.domain.AppUpdateInfo
import com.luc4n3x.levyra.domain.ArtistProfile
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.ArtistRelease
import com.luc4n3x.levyra.domain.DownloadedTrack
import com.luc4n3x.levyra.domain.FollowedArtist
import com.luc4n3x.levyra.domain.SearchFilter
import com.luc4n3x.levyra.domain.SmartMusicProfile
import com.luc4n3x.levyra.domain.LevyraContentLocales
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.LevyraAudioPresets
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.ListeningPulse
import com.luc4n3x.levyra.domain.PulseArtist
import com.luc4n3x.levyra.domain.ExploreCatalog
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.Mood
import com.luc4n3x.levyra.domain.ReleaseRadarEntry
import com.luc4n3x.levyra.domain.Taste
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.LevyraLaunchActions
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraOrange
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import com.luc4n3x.levyra.ui.theme.LevyraPanelSoft
import com.luc4n3x.levyra.ui.theme.LevyraPalette
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraThemeController
import com.luc4n3x.levyra.ui.theme.LevyraThemes
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import androidx.compose.ui.window.DialogProperties

import com.luc4n3x.levyra.ui.theme.glassmorphism
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LevyraViewModel
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.delay
import java.time.format.TextStyle as DayTextStyle
import java.util.Locale

private val LocalAnimationsEnabled = compositionLocalOf { true }
private const val PLAYER_MEDIA_SEEK_STEP_MS = 5_000L
private val CinematicPlum = Color(0xFF2A1738)
private val CinematicGold = Color(0xFFFFC46B)
private val CinematicGlass = Color(0xFF151321)
private val CinematicGlassDeep = Color(0xFF0B0A14)
private val CinematicHairline = Color.White.copy(alpha = 0.105f)
private val HomeHorizontalInset = 18.dp
private val HomeHorizontalShelfEndPadding = 30.dp

private val LevyraIsLight: Boolean get() = LevyraActivePalette.isLight
private val LevyraReadableOnArtwork: Color get() = Color.White
private val LevyraReadableMutedOnArtwork: Color get() = Color.White.copy(alpha = 0.78f)
private val LevyraAdaptiveHairline: Color get() = if (LevyraIsLight) Color(0x26101322) else Color.White.copy(alpha = 0.105f)
private val LevyraAdaptiveSoftHairline: Color get() = if (LevyraIsLight) Color(0x16101322) else Color.White.copy(alpha = 0.08f)
private val LevyraAdaptiveCard: Color get() = if (LevyraIsLight) Color.White.copy(alpha = 0.74f) else CinematicGlass.copy(alpha = 0.66f)
private val LevyraAdaptiveCardDeep: Color get() = if (LevyraIsLight) Color.White.copy(alpha = 0.86f) else CinematicGlassDeep.copy(alpha = 0.74f)
private val LevyraAdaptiveChip: Color get() = if (LevyraIsLight) Color.White.copy(alpha = 0.82f) else Color.White.copy(alpha = 0.06f)
private val LevyraAdaptiveChipSelected: Color get() = if (LevyraIsLight) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.12f)
private val LevyraAdaptiveTrack: Color get() = if (LevyraIsLight) Color(0x1A11131F) else Color.White.copy(alpha = 0.08f)

@Composable
private fun HomeSectionInset(content: @Composable () -> Unit) {
    Box(modifier = Modifier.padding(horizontal = HomeHorizontalInset)) {
        content()
    }
}

private fun cinematicGlassBrush(
    accentStart: Color = LevyraCyan,
    accentEnd: Color = LevyraViolet,
    intensity: Float = 1f
): Brush {
    return if (LevyraIsLight) {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.96f),
                Color(0xFFF7F9FF).copy(alpha = 0.94f),
                accentStart.copy(alpha = 0.08f * intensity),
                accentEnd.copy(alpha = 0.06f * intensity),
                Color(0xFFF2F5FF).copy(alpha = 0.92f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                accentStart.copy(alpha = 0.13f * intensity),
                CinematicGlass.copy(alpha = 0.96f),
                CinematicGlassDeep.copy(alpha = 0.98f),
                accentEnd.copy(alpha = 0.10f * intensity)
            )
        )
    }
}

private fun cinematicTextBrush(): Brush {
    return if (LevyraIsLight) {
        Brush.linearGradient(
            listOf(
                LevyraText,
                LevyraCyan.copy(alpha = 0.95f),
                LevyraViolet.copy(alpha = 0.90f),
                Color(0xFF283044)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White,
                LevyraCyan.copy(alpha = 0.92f),
                LevyraViolet.copy(alpha = 0.86f),
                CinematicGold.copy(alpha = 0.82f)
            )
        )
    }
}

@Composable
private fun RowScope.TabButton(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.8f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "tab-press-scale"
    )
    val pressGlow by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(if (pressed) 100 else 300),
        label = "tab-press-glow"
    )

    val offsetY by animateDpAsState(
        targetValue = if (selected && LocalAnimationsEnabled.current) (-6).dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "tab-offset-y"
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else if (pressed) 0.7f else 0.38f,
        animationSpec = tween(260),
        label = "tab-text-alpha"
    )
    val selectedAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(260, easing = FastOutSlowInEasing),
        label = "tab-selected-alpha"
    )
    val tabActiveTint = if (LevyraIsLight) LevyraCyan else Color(0xFF93DCFF)
    val tabPressedTint = if (LevyraIsLight) LevyraText else Color(0xFFBDEBFF)
    val tabInactiveTint = if (LevyraIsLight) LevyraMuted else Color(0xFF737373)
    val iconTint by animateColorAsState(
        targetValue = if (selected) tabActiveTint else if (pressed) tabPressedTint else tabInactiveTint,
        animationSpec = tween(260),
        label = "tab-icon-tint"
    )
    val labelTint by animateColorAsState(
        targetValue = if (selected) if (LevyraIsLight) LevyraText else Color(0xFFE0F4FF) else if (LevyraIsLight) LevyraMuted else Color.White,
        animationSpec = tween(260),
        label = "tab-label-tint"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "tab-icon-scale"
    )
    val underlineWidth by animateDpAsState(
        targetValue = if (selected) 20.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "tab-underline-width"
    )

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .pressable(
                interactionSource = interactionSource,
                pressedScale = 0.94f,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .offset(y = offsetY)
                .padding(horizontal = 2.dp, vertical = 1.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .graphicsLayer {
                        scaleX = iconScale * pressScale
                        scaleY = iconScale * pressScale
                    }
                    .drawBehind {
                        if (pressGlow > 0f) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF5BD6FF).copy(alpha = 0.35f * pressGlow),
                                        LevyraViolet.copy(alpha = 0.15f * pressGlow),
                                        Color.Transparent
                                    )
                                ),
                                radius = size.minDimension * 1.5f * pressGlow
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.15f * pressGlow),
                                radius = size.minDimension * 0.45f * pressGlow
                            )
                        }

                        drawCircle(
                            color = LevyraCyan.copy(alpha = if (LevyraIsLight) 0.18f * selectedAlpha else 0.13f * selectedAlpha),
                            radius = size.minDimension * 0.46f
                        )
                        drawCircle(
                            color = LevyraCyan.copy(alpha = if (LevyraIsLight) 0.08f * selectedAlpha else 0.07f * selectedAlpha),
                            radius = size.minDimension * 0.27f
                        )
                        drawCircle(
                            color = LevyraCyan.copy(alpha = if (LevyraIsLight) 0.32f * selectedAlpha else 0.28f * selectedAlpha),
                            radius = size.minDimension * 0.43f,
                            style = Stroke(width = 1.dp.toPx())
                        )
                        drawCircle(
                            color = LevyraCyan.copy(alpha = if (LevyraIsLight) 0.62f * selectedAlpha else 0.55f * selectedAlpha),
                            radius = 1.35.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.78f, size.height * 0.24f)
                        )
                        drawCircle(
                            color = LevyraViolet.copy(alpha = 0.42f * selectedAlpha),
                            radius = 1.1.dp.toPx(),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.25f, size.height * 0.78f)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = label,
                color = labelTint.copy(alpha = textAlpha),
                fontSize = 11.sp,
                lineHeight = 12.sp,
                fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .width(underlineWidth)
                    .height(1.5.dp)
                    .graphicsLayer { alpha = selectedAlpha }
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                LevyraCyan.copy(alpha = 0f),
                                LevyraCyan.copy(alpha = if (LevyraIsLight) 0.90f else 0.86f),
                                LevyraViolet.copy(alpha = if (LevyraIsLight) 0.48f else 0.55f),
                                LevyraCyan.copy(alpha = 0f)
                            )
                        ),
                        shape = RoundedCornerShape(99.dp)
                    )
            )
        }
    }
}
@Composable
private fun ActiveTrackEqualizer(
    modifier: Modifier = Modifier,
    color: Color = LevyraCyan,
    isPlaying: Boolean = true,
    width: Dp = 18.dp,
    height: Dp = 14.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer-bars")
    
    val height1 by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 550, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "eq-bar-1"
        )
    } else {
        remember { mutableStateOf(0.4f) }
    }

    val height2 by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 380, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "eq-bar-2"
        )
    } else {
        remember { mutableStateOf(0.6f) }
    }

    val height3 by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0.15f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 460, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "eq-bar-3"
        )
    } else {
        remember { mutableStateOf(0.3f) }
    }

    val height4 by if (isPlaying) {
        infiniteTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 620, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "eq-bar-4"
        )
    } else {
        remember { mutableStateOf(0.5f) }
    }

    Row(
        modifier = modifier.size(width = width, height = height),
        horizontalArrangement = Arrangement.spacedBy(1.5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val bars = listOf(height1, height2, height3, height4)
        bars.forEach { barHeight ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(barHeight)
                    .background(color, RoundedCornerShape(topStart = 1.dp, topEnd = 1.dp))
            )
        }
    }
}
@Composable
private fun SectionTitle(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(22.dp)
                .background(Brush.verticalGradient(listOf(LevyraCyan, LevyraViolet)), RoundedCornerShape(99.dp))
        )
        Text(title, color = LevyraText, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
@Composable
private fun CoverImage(track: Track, modifier: Modifier, highRes: Boolean = false, zoom: Float = 1f) {
    val context = LocalContext.current
    val raw = if (highRes) track.largeThumbnailUrl.ifBlank { track.thumbnailUrl } else track.thumbnailUrl.ifBlank { track.largeThumbnailUrl }
    val model = remember(context, track.id, track.title, track.artist, raw, highRes) {
        LevyraArtworkCache.model(context, track, highRes)
    }
    val background = Brush.linearGradient(listOf(Color(track.accentStart), Color(track.accentEnd)))
    Box(modifier = modifier.background(background), contentAlignment = Alignment.Center) {
        InstantArtworkPlaceholder(track = track, modifier = Modifier.fillMaxSize())
        if (model != null) {
            val crossfadeMs = if (LocalAnimationsEnabled.current && highRes) 120 else 0
            val request = remember(context, model, crossfadeMs) {
                ImageRequest.Builder(context)
                    .data(model)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .crossfade(crossfadeMs)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().scale(zoom)
            )
        }
    }
}

@Composable
private fun InstantArtworkPlaceholder(track: Track, modifier: Modifier) {
    val initials = remember(track.title, track.artist) {
        listOf(track.title, track.artist)
            .mapNotNull { value -> value.trim().firstOrNull()?.uppercaseChar()?.toString() }
            .take(2)
            .joinToString("")
            .ifBlank { "♪" }
    }
    Box(
        modifier = modifier
            .background(
                Brush.radialGradient(
                    listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color(track.accentStart).copy(alpha = 0.46f),
                        Color(track.accentEnd).copy(alpha = 0.64f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = LevyraText.copy(alpha = 0.88f),
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}
@Composable
private fun EmptyCover(modifier: Modifier) {
    Box(
        modifier = modifier.background(Brush.linearGradient(listOf(LevyraCyan.copy(alpha = 0.72f), LevyraViolet.copy(alpha = 0.72f)))),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.MusicNote, null, tint = LevyraBlack, modifier = Modifier.size(30.dp))
    }
}
@Composable
private fun EmptyState(text: String) {
    Surface(
        color = CinematicGlass.copy(alpha = 0.66f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🎵", fontSize = 28.sp)
            Text(text, color = LevyraMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}
@Composable
private fun Modifier.pressable(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    pressedScale: Float = 0.96f,
    onClick: () -> Unit
): Modifier {
    if (!LocalAnimationsEnabled.current) {
        return this.clickable(enabled = enabled, onClick = onClick)
    }
    val interaction = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f, 
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "press"
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
}

@Composable
fun LevyraApp(viewModel: LevyraViewModel, isInPictureInPicture: Boolean = false) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.isVideoMode, state.isPlaying) {
        val currentPipState = LevyraPipBridge.current()
        LevyraPipBridge.updatePlayback(
            videoMode = state.isVideoMode,
            playing = state.isPlaying,
            aspectRatio = currentPipState.aspectRatio
        )
    }
    if (isInPictureInPicture) {
        LevyraPictureInPictureSurface(state)
        return
    }
    val toastContext = LocalContext.current
    val activity = toastContext as? Activity
    var showLanguageRestartDialog by remember { mutableStateOf(false) }
    var showDownloadsFolder by remember { mutableStateOf(false) }
    val accent = if (state.dynamicColor) state.currentTrack ?: state.tracks.firstOrNull() else null
    val overlayEnter = if (state.animationsEnabled) fadeIn(animationSpec = tween(180, easing = LinearOutSlowInEasing)) else EnterTransition.None
    val overlayExit = if (state.animationsEnabled) fadeOut(animationSpec = tween(140, easing = FastOutSlowInEasing)) else ExitTransition.None
    val miniEnter = if (state.animationsEnabled) {
        slideInVertically(animationSpec = tween(260, easing = FastOutSlowInEasing), initialOffsetY = { it / 2 }) + fadeIn(animationSpec = tween(180, easing = LinearOutSlowInEasing))
    } else {
        EnterTransition.None
    }
    val miniExit = if (state.animationsEnabled) {
        slideOutVertically(animationSpec = tween(180, easing = FastOutSlowInEasing), targetOffsetY = { it / 3 }) + fadeOut(animationSpec = tween(140, easing = FastOutSlowInEasing))
    } else {
        ExitTransition.None
    }
    val accentTrack = state.currentTrack ?: state.tracks.firstOrNull()
    LaunchedEffect(state.themePreset, accentTrack?.accentStart, accentTrack?.accentEnd, state.selectedMood?.id) {
        LevyraThemeController.apply(
            state.themePreset,
            accentTrack?.accentStart,
            accentTrack?.accentEnd,
            state.selectedMood?.accentStart,
            state.selectedMood?.accentEnd
        )
    }
    val rootView = LocalView.current
    LaunchedEffect(state.themePreset) {
        val palette = LevyraThemes.byId(state.themePreset)
        (rootView.context as? Activity)?.window?.let { window ->
            window.decorView.setBackgroundColor(palette.black.toArgb())
            WindowCompat.getInsetsController(window, rootView).apply {
                isAppearanceLightStatusBars = palette.isLight
                isAppearanceLightNavigationBars = palette.isLight
            }
        }
    }
    val pendingShortcut by LevyraLaunchActions.pendingShortcut
    LaunchedEffect(pendingShortcut) {
        when (pendingShortcut) {
            LevyraLaunchActions.SHORTCUT_FAVORITES -> viewModel.selectTab(LevyraTab.Library)
            LevyraLaunchActions.SHORTCUT_OFFLINE -> viewModel.selectTab(LevyraTab.Library)
            LevyraLaunchActions.SHORTCUT_FLOW -> viewModel.playDailyFlow()
            LevyraLaunchActions.SHORTCUT_LYRICS -> {
                viewModel.selectTab(LevyraTab.Player)
                viewModel.openLyrics()
            }
        }
        if (pendingShortcut != null) LevyraLaunchActions.pendingShortcut.value = null
    }
    val pendingArtist by LevyraLaunchActions.pendingArtist
    LaunchedEffect(pendingArtist) {
        pendingArtist?.let { artistName ->
            viewModel.openArtistByName(artistName)
            LevyraLaunchActions.pendingArtist.value = null
        }
    }
    LaunchedEffect(state.offlineExportMessage) {
        state.offlineExportMessage?.let { message ->
            Toast.makeText(toastContext, message, Toast.LENGTH_LONG).show()
            viewModel.clearOfflineExportMessage()
        }
    }
    LaunchedEffect(state.updateMessage) {
        state.updateMessage?.let { message ->
            Toast.makeText(toastContext, message, Toast.LENGTH_LONG).show()
            viewModel.clearUpdateMessage()
        }
    }
    BackHandler(enabled = showLanguageRestartDialog || showDownloadsFolder || state.openPlaylist != null || state.showUpdatePrompt || state.showAlbum || state.showArtist || state.showQueue || state.showLyrics || state.showSettings || state.showAudioQualityPanel || state.selectedTab != LevyraTab.Home) {
        if (showLanguageRestartDialog) {
            showLanguageRestartDialog = false
        } else if (showDownloadsFolder) {
            showDownloadsFolder = false
        } else if (state.showAudioQualityPanel) {
            viewModel.closeAudioQualityPanel()
        } else if (state.openPlaylist != null) {
            viewModel.closePlaylist()
        } else if (!viewModel.navigateBack()) {
            activity?.finish()
        }
    }
    CompositionLocalProvider(
        LocalAnimationsEnabled provides state.animationsEnabled,
        LocalLevyraStrings provides LevyraStrings.forCode(state.languageCode)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LevyraBlack)
        ) {
            LevyraBackground(accent?.accentStart, accent?.accentEnd)

            AnimatedContent(
                targetState = state.selectedTab,
                transitionSpec = {
                    if (!state.animationsEnabled) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                        val enter = slideInHorizontally(
                            animationSpec = tween(320, easing = FastOutSlowInEasing),
                            initialOffsetX = { it * direction }
                        ) + fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing))
                        val exit = slideOutHorizontally(
                            animationSpec = tween(240, easing = FastOutSlowInEasing),
                            targetOffsetX = { -it * direction / 3 }
                        ) + fadeOut(animationSpec = tween(160, easing = FastOutSlowInEasing))
                        enter togetherWith exit
                    }
                },
                label = "levyra-page-transition"
            ) { tab ->
                Box(modifier = Modifier.fillMaxSize()) {
                    when (tab) {
                        LevyraTab.Home -> HomeScreen(viewModel, state)
                        LevyraTab.Search -> SearchScreen(viewModel, state)
                        LevyraTab.Explore -> ExploreScreen(viewModel, state)
                        LevyraTab.Library -> LibraryScreen(viewModel, state, onOpenDownloads = { showDownloadsFolder = true })
                        LevyraTab.Player -> PlayerScreen(viewModel, state)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                AnimatedVisibility(
                    visible = state.selectedTab != LevyraTab.Player && state.currentTrack != null,
                    enter = miniEnter,
                    exit = miniExit
                ) {
                    state.currentTrack?.let { track ->
                        MiniPlayer(
                            track = track,
                            isPlaying = state.isPlaying,
                            isResolving = state.isResolving,
                            progress = progressOf(state.positionMs, state.durationMs),
                            onOpen = { viewModel.selectTab(LevyraTab.Player) },
                            onToggle = viewModel::togglePlay,
                            onNext = viewModel::next,
                            onClose = viewModel::closePlayer
                        )
                    }
                }
                AnimatedVisibility(
                    visible = state.selectedTab != LevyraTab.Player,
                    enter = miniEnter,
                    exit = miniExit
                ) {
                    BottomTabs(
                        selected = state.selectedTab,
                        flatTop = state.currentTrack != null,
                        onSelect = viewModel::selectTab
                    )
                }
            }

            AnimatedVisibility(
                visible = state.downloadingTrackIds.isNotEmpty(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(18f)
                    .padding(horizontal = 22.dp)
                    .navigationBarsPadding()
                    .padding(bottom = downloadHudBottomPadding(state)),
                enter = miniEnter,
                exit = miniExit
            ) {
                DownloadProgressHud(state = state)
            }

            AnimatedVisibility(visible = state.showOnboarding, enter = overlayEnter, exit = overlayExit) {
                if (state.showOnboarding) {
                    OnboardingOverlay(tastes = state.tastes, selectedLanguageCode = state.languageCode, onDone = viewModel::completeOnboarding)
                }
            }

            AnimatedVisibility(visible = state.showSettings, enter = overlayEnter, exit = overlayExit) {
                SettingsOverlay(
                    animationsEnabled = state.animationsEnabled,
                    dynamicColor = state.dynamicColor,
                    sponsorBlock = state.sponsorBlockEnabled,
                    skipSilence = state.skipSilence,
                    updateInfo = state.updateInfo,
                    isCheckingUpdates = state.isCheckingUpdates,
                    currentLanguageCode = state.languageCode,
                    themePreset = state.themePreset,
                    onThemePreset = viewModel::setThemePreset,
                    onAnimations = viewModel::setAnimationsEnabled,
                    onDynamicColor = viewModel::setDynamicColor,
                    onSponsorBlock = viewModel::setSponsorBlock,
                    onSkipSilence = viewModel::setSkipSilence,
                    onLanguage = { code ->
                        val normalized = LevyraLanguageCatalog.normalize(code)
                        if (normalized != state.languageCode) {
                            viewModel.setLanguage(normalized)
                            showLanguageRestartDialog = true
                        }
                    },
                    onCheckUpdates = { viewModel.checkForUpdates(silent = false) },
                    onDownloadUpdate = { state.updateInfo?.let { openExternalUrl(toastContext, it.downloadUrl) } },
                    onRedoQuestionnaire = viewModel::restartOnboarding,
                    onClose = viewModel::closeSettings
                )
            }

            AnimatedVisibility(visible = state.showLyrics, enter = overlayEnter, exit = overlayExit) {
                LyricsOverlay(state = state, onTranslation = viewModel::setLyricsTranslationEnabled, onClose = viewModel::closeLyrics)
            }

            AnimatedVisibility(visible = state.showAudioQualityPanel, enter = miniEnter, exit = miniExit) {
                AudioQualityPanel(
                    selected = state.audioQuality,
                    volumePercent = 33,
                    audioSettings = state.audioSettings,
                    onSelect = viewModel::setAudioQuality,
                    onEqualizerEnabled = viewModel::setEqualizerEnabled,
                    onPreset = viewModel::setEqualizerPreset,
                    onBassBoost = viewModel::setBassBoost,
                    onVirtualizer = viewModel::setVirtualizer,
                    onCrossfade = viewModel::setCrossfadeSeconds,
                    onDjSoft = viewModel::setDjSoftMode,
                    onReplayGain = viewModel::setReplayGainEnabled,
                    onTempo = viewModel::setPlaybackSpeed,
                    onPitch = viewModel::setPitch,
                    onGapless = viewModel::setGaplessEnabled,
                    onClose = viewModel::closeAudioQualityPanel
                )
            }

            AnimatedVisibility(visible = state.showUpdatePrompt && state.updateInfo?.isNewer == true, enter = overlayEnter, exit = overlayExit) {
                state.updateInfo?.let { update ->
                    UpdateAvailableOverlay(
                        update = update,
                        onDownload = {
                            openExternalUrl(toastContext, update.downloadUrl)
                            viewModel.dismissUpdatePrompt()
                        },
                        onLater = viewModel::dismissUpdatePrompt
                    )
                }
            }

            AnimatedVisibility(visible = state.showQueue, enter = overlayEnter, exit = overlayExit) {
                QueueOverlay(
                    state = state,
                    onPlay = viewModel::playQueueTrack,
                    onPlayNext = viewModel::playNext,
                    onRemove = viewModel::removeFromQueue,
                    onMove = viewModel::moveQueueItem,
                    onUndo = viewModel::undoQueueRemoval,
                    onToggleRadio = viewModel::toggleContinuousRadio,
                    onClose = viewModel::closeQueue
                )
            }

            AnimatedVisibility(visible = state.showAlbum, modifier = Modifier.zIndex(40f), enter = overlayEnter, exit = overlayExit) {
                AlbumOverlay(
                    state = state,
                    onPlayAll = viewModel::playCurrentAlbum,
                    onPlay = viewModel::playAlbumSong,
                    onTogglePlayback = viewModel::togglePlay,
                    onFavorite = viewModel::toggleFavorite,
                    onDownload = viewModel::exportTrack,
                    onDownloadAlbum = viewModel::exportCurrentAlbum,

                    onAddToPlaylist = { playlistId, track -> viewModel.addToPlaylist(playlistId, track) },
                    onCreatePlaylistWithTrack = { name, track -> viewModel.createPlaylist(name, track) },
                    onOpenAlbumArtist = viewModel::openArtistFromAlbum,
                    onOpenTrackArtist = viewModel::openArtist,
                    onOpenPlayer = viewModel::openPlayerScreen,
                    onClose = viewModel::closeAlbum
                )
            }

            AnimatedVisibility(visible = state.showArtist, enter = overlayEnter, exit = overlayExit) {
                ArtistOverlay(
                    state = state,
                    onPlay = viewModel::playArtistSong,
                    onFavorite = viewModel::toggleFavorite,
                    onDownload = viewModel::exportTrack,
                    onToggleFollow = viewModel::toggleFollowArtist,
                    onOpenArtist = viewModel::openArtistFromHit,
                    onOpenRelease = viewModel::openArtistRelease,
                    onClose = viewModel::closeArtist
                )
            }

            AnimatedVisibility(visible = state.openPlaylist != null, enter = overlayEnter, exit = overlayExit) {
                PlaylistDetailOverlay(viewModel = viewModel, state = state)
            }

            AnimatedVisibility(visible = showDownloadsFolder, enter = overlayEnter, exit = overlayExit) {
                DownloadsFolderOverlay(state = state, viewModel = viewModel, onClose = { showDownloadsFolder = false })
            }

            if (showLanguageRestartDialog) {
                LanguageRestartDialog(
                    onRestart = {
                        showLanguageRestartDialog = false
                        restartLevyra(activity)
                    },
                    onLater = { showLanguageRestartDialog = false }
                )
            }

        }
    }
}

private data class DownloadHudItem(
    val progress: Int,
    val title: String,
    val count: Int
)

private fun LevyraUiState.activeDownloadHudItem(): DownloadHudItem? {
    val ids = downloadingTrackIds.toList()
    if (ids.isEmpty()) return null
    val primaryId = ids.maxByOrNull { downloadProgressByTrackId[it] ?: 1 } ?: return null
    val progress = (downloadProgressByTrackId[primaryId] ?: 1).coerceIn(1, 99)
    val rawTitle = downloadTitleByTrackId[primaryId].orEmpty().ifBlank { "brano" }
    val title = if (ids.size > 1) "$rawTitle +${ids.size - 1}" else rawTitle
    return DownloadHudItem(progress = progress, title = title, count = ids.size)
}

private fun downloadHudBottomPadding(state: LevyraUiState): Dp {
    return when {
        state.selectedTab == LevyraTab.Player -> 24.dp
        state.currentTrack != null -> 154.dp
        else -> 96.dp
    }
}

@Composable
private fun DownloadProgressHud(state: LevyraUiState) {
    val item = state.activeDownloadHudItem() ?: return
    val progressFraction = item.progress / 100f
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(24.dp, RoundedCornerShape(999.dp), clip = false)
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            LevyraPanelSoft.copy(alpha = 0.98f),
                            LevyraInk.copy(alpha = 0.96f),
                            LevyraPanel.copy(alpha = 0.94f)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                LevyraCyan.copy(alpha = 0.24f),
                                LevyraViolet.copy(alpha = 0.16f),
                                Color.Transparent
                            )
                        )
                    )
                    .drawBehind {
                        val strokeWidth = 3.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2f
                        drawCircle(
                            color = Color.White.copy(alpha = 0.16f),
                            radius = radius,
                            style = Stroke(width = strokeWidth)
                        )
                        drawArc(
                            color = LevyraCyan,
                            startAngle = -90f,
                            sweepAngle = 360f * progressFraction.coerceIn(0f, 1f),
                            useCenter = false,
                            style = Stroke(width = strokeWidth)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${item.progress}%",
                    color = LevyraCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (item.count > 1) "Download multipli in corso" else "Download in corso",
                    color = LevyraMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.title,
                    color = LevyraText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Rounded.Download, contentDescription = null, tint = LevyraCyan, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun LanguageRestartDialog(onRestart: () -> Unit, onLater: () -> Unit) {
    val strings = LocalLevyraStrings.current
    AlertDialog(
        onDismissRequest = onLater,
        containerColor = Color(0xFF11131C),
        title = { Text(strings.restartRequiredTitle, color = LevyraText, fontWeight = FontWeight.Black) },
        text = { Text(strings.restartRequiredBody, color = LevyraMuted, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp) },
        confirmButton = { TextButton(onClick = onRestart) { Text(strings.restartNow, color = LevyraCyan, fontWeight = FontWeight.Black) } },
        dismissButton = { TextButton(onClick = onLater) { Text(strings.later, color = LevyraMuted, fontWeight = FontWeight.Bold) } }
    )
}

private fun restartLevyra(activity: Activity?) {
    if (activity == null) return
    val intent = activity.packageManager.getLaunchIntentForPackage(activity.packageName) ?: activity.intent
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
    activity.startActivity(intent)
    activity.finish()
}


@Composable
private fun AlbumOverlay(
    state: LevyraUiState,
    onPlayAll: () -> Unit,
    onPlay: (Track) -> Unit,
    onTogglePlayback: () -> Unit,
    onFavorite: (Track) -> Unit,
    onDownload: (Track) -> Unit,
    onDownloadAlbum: () -> Unit,

    onAddToPlaylist: (String, Track) -> Unit,
    onCreatePlaylistWithTrack: (String, Track) -> Unit,
    onOpenAlbumArtist: () -> Unit,
    onOpenTrackArtist: (Track) -> Unit,
    onOpenPlayer: () -> Unit,
    onClose: () -> Unit
) {
    val blocker = remember { MutableInteractionSource() }
    val detail = state.albumDetail
    val album = detail?.album
    val tracks = detail?.tracks.orEmpty()
    val description = detail?.description.orEmpty()
    val cover = album?.thumbnailUrl.orEmpty()
    val accentTrack = tracks.firstOrNull() ?: state.currentTrack
    val accentStart = accentTrack?.let { Color(it.accentStart) } ?: LevyraCyan
    val accentEnd = accentTrack?.let { Color(it.accentEnd) } ?: LevyraViolet
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val strings = LocalLevyraStrings.current
    var addTarget by remember { mutableStateOf<Track?>(null) }
    val albumCurrentTrack = remember(tracks, state.currentTrack) {
        tracks.firstOrNull { candidate -> uiTrackMatches(state.currentTrack, candidate) }
    }
    val albumIsActive = albumCurrentTrack != null
    val albumIsPlaying = albumIsActive && state.isPlaying
    val albumIsResolving = albumIsActive && state.isResolving
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LevyraBlack)
            .clickable(interactionSource = blocker, indication = null) {}
    ) {
        LevyraBackground(accentTrack?.accentStart, accentTrack?.accentEnd)
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.08f),
                            Color.Black.copy(alpha = 0.34f),
                            LevyraBlack.copy(alpha = 0.96f)
                        )
                    )
                )
        )
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = if (state.currentTrack != null) 232.dp else 112.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "album-topbar") {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color.White.copy(alpha = 0.06f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp).pressable(onClick = onClose)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = strings.back, tint = LevyraText)
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        color = Color.White.copy(alpha = 0.06f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp).pressable(onClick = {
                            val query = listOf(album?.title.orEmpty(), album?.artist.orEmpty()).filter { it.isNotBlank() }.joinToString(" ")
                            if (query.isNotBlank()) openExternalUrl(context, "https://music.youtube.com/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}")
                        })
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = LevyraText)
                        }
                    }
                }
            }
            when {
                state.albumLoading && (album == null || tracks.isEmpty()) -> {
                    item(key = "album-loading") {
                        AlbumLoadingCard()
                    }
                }
                album == null -> {
                    item(key = "album-empty") {
                        GlassMessage("Album non disponibile", LevyraOrange)
                    }
                }
                else -> {
                    item(key = "album-hero") {
                        AlbumHeroCard(
                            album = album,
                            cover = cover,
                            description = description,
                            trackCount = tracks.size,
                            isPlaying = albumIsPlaying,
                            isResolving = albumIsResolving,
                            accentStart = accentStart,
                            accentEnd = accentEnd,
                            onPlayAll = {
                                if (albumIsActive) onTogglePlayback() else onPlayAll()
                            },
                            onDownload = onDownloadAlbum,
                            onOpenArtist = onOpenAlbumArtist,
                            onShare = {
                                val shareText = buildString {
                                    append(album.title)
                                    if (album.artist.isNotBlank()) append(" - ").append(album.artist)
                                    if (album.browseId.isNotBlank()) append("\nhttps://music.youtube.com/browse/").append(album.browseId)
                                }
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Condividi album"))
                            }
                        )
                    }
                    if (tracks.isNotEmpty()) {
                        item(key = "album-track-count") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .height(22.dp)
                                        .width(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Brush.verticalGradient(listOf(LevyraCyan, LevyraViolet)))
                                )
                                Text("${tracks.size} brani", color = LevyraText, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp)
                            }
                        }
                        itemsIndexed(
                            items = tracks,
                            key = { index, track -> "album-track-$index-${track.id}" },
                            contentType = { _, _ -> "album-track" }
                        ) { index, track ->
                            AlbumTrackItem(
                                index = index,
                                track = track,
                                isCurrent = uiTrackMatches(state.currentTrack, track),
                                isPlaying = state.isPlaying,
                                isFavorite = track.id in state.favoriteIds,
                                isDownloading = track.id in state.downloadingTrackIds,
                                isDownloaded = track.id in state.downloadedTrackIds,
                                downloadProgress = state.downloadProgressByTrackId[track.id],
                                onPlay = {
                                    if (uiTrackMatches(state.currentTrack, track)) onOpenPlayer() else onPlay(track)
                                },
                                onFavorite = { onFavorite(track) },
                                onDownload = { onDownload(track) },
                                onAddToPlaylist = { addTarget = track },
                                onArtist = { onOpenTrackArtist(track) }
                            )
                        }
                    } else if (!state.albumLoading) {
                        item(key = "album-no-tracks") {
                            GlassMessage(state.albumError ?: "Tracce album non disponibili", LevyraOrange)
                        }
                    }
                }
            }
        }
        if (state.albumLoading && album != null && tracks.isNotEmpty()) {
            LinearMiniLoading(modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 58.dp))
        }
        state.currentTrack?.let { current ->
            AlbumNowPlayingDock(
                track = current,
                isPlaying = state.isPlaying,
                isResolving = state.isResolving,
                progress = progressOf(state.positionMs, state.durationMs),
                onToggle = onTogglePlayback,
                onOpenPlayer = onOpenPlayer,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 18.dp, end = 18.dp, bottom = 14.dp)
            )
        }

        addTarget?.let { track ->
            AddToPlaylistDialog(
                track = track,
                playlists = state.playlists,
                onDismiss = { addTarget = null },
                onAddTo = { playlistId ->
                    onAddToPlaylist(playlistId, track)
                    addTarget = null
                },
                onCreateWith = { name ->
                    onCreatePlaylistWithTrack(name, track)
                    addTarget = null
                }
            )
        }    }
}@Composable
private fun AlbumLoadingCard() {
    Surface(
        color = Color.White.copy(alpha = 0.055f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 34.dp)
            .shimmer()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.size(226.dp).clip(RoundedCornerShape(28.dp)).background(Color.White.copy(alpha = 0.08f)))
            Box(modifier = Modifier.height(34.dp).fillMaxWidth(0.70f).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.08f)))
            Box(modifier = Modifier.height(18.dp).fillMaxWidth(0.48f).clip(RoundedCornerShape(9.dp)).background(Color.White.copy(alpha = 0.06f)))
            Box(modifier = Modifier.height(78.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.055f)))
        }
    }
}

@Composable
private fun AlbumHeroCard(
    album: AlbumHit,
    cover: String,
    description: String,
    trackCount: Int,
    isPlaying: Boolean,
    isResolving: Boolean,
    accentStart: Color,
    accentEnd: Color,
    onPlayAll: () -> Unit,
    onDownload: () -> Unit,
    onOpenArtist: () -> Unit,
    onShare: () -> Unit
) {
    val context = LocalContext.current
    var descriptionExpanded by remember { mutableStateOf(false) }
    // Apple/Vercel hero: calm surface, one signature drop-shadow under the artwork,
    // restrained hairline borders, tight negative-tracking display type, and a
    // single primary action (Play) that owns the visual weight.
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .shadow(30.dp, RoundedCornerShape(28.dp), clip = false, ambientColor = accentStart.copy(alpha = 0.55f), spotColor = accentEnd.copy(alpha = 0.55f))
                .clip(RoundedCornerShape(28.dp))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)), RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (cover.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(LevyraArtworkCache.large(cover))
                        .crossfade(180)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = album.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(accentStart, accentEnd))), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Album, null, tint = LevyraText, modifier = Modifier.size(72.dp))
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                album.title,
                color = LevyraText,
                fontSize = 32.sp,
                lineHeight = 35.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                letterSpacing = (-1.0).sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                listOf(album.artist, album.year).filter { it.isNotBlank() }.joinToString("  ·  "),
                color = LevyraMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.2).sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Primary action: full-width Play, the way Apple Music anchors an album.
        AlbumPrimaryPlayButton(
            enabled = trackCount > 0,
            isPlaying = isPlaying,
            isResolving = isResolving,
            accentStart = accentStart,
            accentEnd = accentEnd,
            onClick = onPlayAll
        )

        // Secondary actions: quiet, evenly weighted, hairline chips.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AlbumSecondaryAction(icon = Icons.Rounded.Download, label = "Offline", enabled = trackCount > 0, modifier = Modifier.weight(1f), onClick = onDownload)
            AlbumSecondaryAction(icon = Icons.Rounded.Person, label = "Artista", enabled = album.artist.isNotBlank(), modifier = Modifier.weight(1f), onClick = onOpenArtist)
            AlbumSecondaryAction(icon = Icons.Rounded.Share, label = "Condividi", enabled = true, modifier = Modifier.weight(1f), onClick = onShare)
        }

        if (description.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)), RoundedCornerShape(20.dp))
                    .clickable { descriptionExpanded = !descriptionExpanded }
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    description,
                    color = LevyraMuted,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = (-0.2).sp,
                    maxLines = if (descriptionExpanded) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (descriptionExpanded) "Mostra meno" else "Leggi tutto",
                    color = accentStart,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp
                )
            }
        }
    }
}

@Composable
private fun AlbumPrimaryPlayButton(
    enabled: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    accentStart: Color,
    accentEnd: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(if (enabled) 16.dp else 0.dp, RoundedCornerShape(16.dp), clip = false, spotColor = accentStart.copy(alpha = 0.6f))
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (enabled) Brush.horizontalGradient(listOf(accentStart, accentEnd))
                else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.08f)))
            )
            .pressable(enabled = enabled && !isResolving, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (isResolving) {
                CircularProgressIndicator(color = LevyraBlack, strokeWidth = 2.5.dp, modifier = Modifier.size(22.dp))
            } else {
                Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = if (enabled) LevyraBlack else LevyraMuted, modifier = Modifier.size(26.dp))
            }
            Text(
                if (isPlaying) "In riproduzione" else "Riproduci",
                color = if (enabled) LevyraBlack else LevyraMuted,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
        }
    }
}

@Composable
private fun AlbumSecondaryAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = if (enabled) 0.06f else 0.03f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = if (enabled) 0.08f else 0.04f)), RoundedCornerShape(14.dp))
            .pressable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = if (enabled) LevyraText else LevyraMuted.copy(alpha = 0.45f), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            label,
            color = if (enabled) LevyraText else LevyraMuted.copy(alpha = 0.45f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun uiTrackMatches(current: Track?, candidate: Track): Boolean {
    if (current == null) return false
    if (current.id.isNotBlank() && candidate.id.isNotBlank() && current.id == candidate.id) return true
    val currentTitle = current.title.trim().lowercase()
    val candidateTitle = candidate.title.trim().lowercase()
    val currentArtist = current.artist.trim().lowercase()
    val candidateArtist = candidate.artist.trim().lowercase()
    return currentTitle.isNotBlank() && currentTitle == candidateTitle && currentArtist == candidateArtist
}

@Composable
private fun AlbumNowPlayingDock(
    track: Track,
    isPlaying: Boolean,
    isResolving: Boolean,
    progress: Float,
    onToggle: () -> Unit,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentStart = Color(track.accentStart)
    val accentEnd = Color(track.accentEnd)
    // Consistent with the global MiniPlayer: accent-tinted glass pill, rounded on
    // all corners, hairline border, one gradient play button, thin progress rail.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(20.dp, RoundedCornerShape(22.dp), clip = false, spotColor = accentStart.copy(alpha = 0.4f))
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        accentStart.copy(alpha = 0.20f),
                        Color(0xFF141414),
                        Color(0xFF0E0E0E),
                        accentEnd.copy(alpha = 0.14f)
                    )
                )
            )
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(22.dp))
            .clickable(onClick = onOpenPlayer)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(start = 10.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)), RoundedCornerShape(13.dp))
                ) {
                    CoverImage(track, Modifier.fillMaxSize())
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(track.title, color = LevyraText, fontSize = 14.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.2).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                MiniPlayerToggleButton(
                    isPlaying = isPlaying,
                    isResolving = isResolving,
                    onToggle = onToggle
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.06f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(LevyraCyan, LevyraViolet)))
                )
            }
        }
    }
}

@Composable
private fun LinearMiniLoading(modifier: Modifier = Modifier) {
    Surface(color = Color.White.copy(alpha = 0.08f), shape = CircleShape, modifier = modifier.width(92.dp).height(4.dp)) {
        Box(modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(LevyraCyan.copy(alpha = 0.2f), LevyraCyan, LevyraViolet, LevyraCyan.copy(alpha = 0.2f)))))
    }
}

@Composable
private fun AlbumTrackItem(
    index: Int,
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    isDownloading: Boolean,
    isDownloaded: Boolean,
    downloadProgress: Int?,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onDownload: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onArtist: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Surface(
        color = if (isCurrent) LevyraCyan.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, if (isCurrent) LevyraCyan.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onPlay)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                if (isCurrent) {
                    ActiveTrackEqualizer(color = LevyraCyan, isPlaying = isPlaying, width = 18.dp, height = 14.dp)
                } else {
                    Text("${index + 1}", color = LevyraMuted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp)
                }
            }
            Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(LevyraPanelSoft), contentAlignment = Alignment.Center) {
                val thumb = track.thumbnailUrl.ifBlank { track.largeThumbnailUrl }
                if (thumb.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(LevyraArtworkCache.small(thumb)).crossfade(120).diskCachePolicy(CachePolicy.ENABLED).memoryCachePolicy(CachePolicy.ENABLED).build(),
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                } else {
                    Icon(Icons.Rounded.MusicNote, null, tint = LevyraMuted, modifier = Modifier.size(24.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(track.title, color = if (isCurrent) LevyraCyan else LevyraText, fontSize = 15.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val duration = formatDuration(track.durationMs).takeIf { it != "--:--" }.orEmpty()
                val status = when {
                    isDownloaded -> "Offline"
                    isDownloading -> "Download ${downloadProgress ?: 1}%"
                    else -> duration
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = track.artist,
                        color = LevyraMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.2).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (status.isNotBlank()) {
                        Text(
                            text = "  ·  ${status}",
                            color = LevyraMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.2).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Rounded.MoreVert, null, tint = LevyraMuted)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text(if (isFavorite) "Rimuovi dai preferiti" else "Aggiungi ai preferiti") }, leadingIcon = { Icon(if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null) }, onClick = { expanded = false; onFavorite() })
                    DropdownMenuItem(text = { Text("Aggiungi a playlist") }, leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, null) }, onClick = { expanded = false; onAddToPlaylist() })
                    DropdownMenuItem(text = { Text(if (isDownloaded) "Già offline" else "Scarica") }, leadingIcon = { Icon(if (isDownloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download, null) }, onClick = { expanded = false; if (!isDownloaded) onDownload() })
                    DropdownMenuItem(text = { Text("Apri artista") }, leadingIcon = { Icon(Icons.Rounded.Person, null) }, onClick = { expanded = false; onArtist() })
                    DropdownMenuItem(text = { Text("Condividi") }, leadingIcon = { Icon(Icons.Rounded.Share, null) }, onClick = {
                        expanded = false
                        val shareText = buildString {
                            append(track.title)
                            if (track.artist.isNotBlank()) append(" - ").append(track.artist)
                            val link = track.videoUrl.ifBlank { track.streamUrl }
                            if (link.isNotBlank()) append("\n").append(link)
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Condividi brano"))
                    })
                }
            }
        }
    }
}

@Composable
private fun ArtistOverlay(
    state: LevyraUiState,
    onPlay: (Track) -> Unit,
    onFavorite: (Track) -> Unit,
    onDownload: (Track) -> Unit,
    onToggleFollow: () -> Unit,
    onOpenArtist: (ArtistHit) -> Unit,
    onOpenRelease: (ArtistRelease, String) -> Unit,
    onClose: () -> Unit
) {
    val blocker = remember { MutableInteractionSource() }
    val profile = state.artistProfile
    val isFollowed = profile != null && (
        (profile.browseId.isNotBlank() && profile.browseId in state.followedArtistKeys) ||
            profile.name.trim().lowercase() in state.followedArtistKeys
        )
    val accentStart = profile?.let { Color(it.accentStart) } ?: LevyraCyan
    val accentEnd = profile?.let { Color(it.accentEnd) } ?: LevyraViolet
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LevyraBlack)
            .clickable(interactionSource = blocker, indication = null) {}
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(440.dp)
                .background(Brush.verticalGradient(listOf(accentStart.copy(alpha = 0.75f), accentEnd.copy(alpha = 0.35f), LevyraBlack, LevyraBlack)))
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = if (state.currentTrack != null) 200.dp else 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = LocalLevyraStrings.current.back, tint = LevyraText)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = LevyraText.copy(alpha = 0.7f))
                }
            }
            when {
                state.artistLoading && profile == null -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = LevyraCyan)
                        }
                    }
                }
                profile == null -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                            Text(state.artistError ?: "Profilo artista non disponibile", color = LevyraMuted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                else -> {
                    item { ArtistHeader(profile, accentStart, accentEnd) }
                    item { ArtistFollowButton(isFollowed = isFollowed, accentStart = accentStart, accentEnd = accentEnd, onClick = onToggleFollow) }
                    if (profile.hasBio) {
                        item { ArtistBio(profile.bio) }
                    }
                    if (profile.topSongs.isNotEmpty()) {
                        item { SectionTitle("🔥 Brani popolari") }
                        items(profile.topSongs, key = { "artist-song-${it.id}" }) { track ->
                            TrackRow(
                                track = track,
                                isCurrent = track.id == state.currentTrack?.id,
                                isPlaying = state.isPlaying && track.id == state.currentTrack?.id,
                                isResolving = state.isResolving && track.id == state.currentTrack?.id,
                                isFavorite = track.id in state.favoriteIds,
                                onClick = { onPlay(track) },
                                onFavorite = { onFavorite(track) },
                                isDownloading = track.id in state.downloadingTrackIds,
                                isDownloaded = track.id in state.downloadedTrackIds,
                                downloadProgress = state.downloadProgressByTrackId[track.id],
                                onDownload = { onDownload(track) }
                            )
                        }
                    }
                    if (profile.albums.isNotEmpty()) {
                        item { SectionTitle("💿 Album") }
                        item { ArtistReleaseRow(profile.albums, profile.name, onOpenRelease) }
                    }
                    if (profile.singles.isNotEmpty()) {
                        item { SectionTitle("🎵 Singoli ed EP") }
                        item { ArtistReleaseRow(profile.singles, profile.name, onOpenRelease) }
                    }
                    if (profile.relatedArtists.isNotEmpty()) {
                        item { SectionTitle("✨ ${LocalLevyraStrings.current.similarArtists}") }
                        item { ArtistHitRow(profile.relatedArtists, onClick = onOpenArtist) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistFollowButton(isFollowed: Boolean, accentStart: Color, accentEnd: Color, onClick: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(50.dp),
        border = if (isFollowed) BorderStroke(1.dp, LevyraText.copy(alpha = 0.35f)) else null,
        modifier = Modifier.pressable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .then(
                    if (isFollowed) Modifier
                    else Modifier.background(Brush.linearGradient(listOf(accentStart, accentEnd)), RoundedCornerShape(50.dp))
                )
                .padding(horizontal = 22.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isFollowed) Icons.Rounded.Check else Icons.Rounded.PersonAdd,
                contentDescription = null,
                tint = if (isFollowed) LevyraText else Color.White,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = if (isFollowed) strings.followingArtist else strings.followArtist,
                color = if (isFollowed) LevyraText else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun ArtistHeader(profile: ArtistProfile, accentStart: Color, accentEnd: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            modifier = Modifier
                .size(164.dp)
                .clip(RoundedCornerShape(42.dp))
                .background(Brush.linearGradient(listOf(accentStart, accentEnd))),
            contentAlignment = Alignment.Center
        ) {
            if (profile.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(profile.thumbnailUrl).crossfade(true).build(),
                    contentDescription = profile.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize().clip(RoundedCornerShape(42.dp))
                )
            } else {
                Icon(Icons.Rounded.Person, null, tint = LevyraText, modifier = Modifier.size(64.dp))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(profile.name, color = LevyraText, fontSize = 38.sp, lineHeight = 42.sp, letterSpacing = (-1.2).sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Rounded.Verified, null, tint = LevyraCyan, modifier = Modifier.size(26.dp))
        }
        val meta = listOf(profile.subscribers, profile.monthlyListeners).filter { it.isNotBlank() }.joinToString(" · ")
        if (meta.isNotBlank()) {
            Text(meta, color = LevyraMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ArtistBio(bio: String) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        color = LevyraPanelSoft.copy(alpha = 0.55f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Biografia", color = LevyraCyan, fontSize = 13.sp, fontWeight = FontWeight.Black)
            Text(
                bio,
                color = LevyraText.copy(alpha = 0.86f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 21.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (expanded) "Mostra meno" else "Mostra tutto",
                color = LevyraCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ArtistReleaseRow(releases: List<ArtistRelease>, artistName: String, onOpen: (ArtistRelease, String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(releases, key = { "rel-${it.browseId.ifBlank { it.title }}" }) { release ->
            Column(
                modifier = Modifier.width(140.dp).pressable { onOpen(release, artistName) },
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(LevyraPanelSoft),
                    contentAlignment = Alignment.Center
                ) {
                    if (release.thumbnailUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(release.thumbnailUrl).crossfade(true).build(),
                            contentDescription = release.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        Icon(Icons.Rounded.Album, null, tint = LevyraMuted, modifier = Modifier.size(40.dp))
                    }
                }
                Text(release.title, color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(release.year.ifBlank { release.subtitle }, color = LevyraMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ReleaseRadarRow(
    entries: List<ReleaseRadarEntry>,
    onOpen: (ReleaseRadarEntry) -> Unit,
    onArtist: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = HomeHorizontalInset, end = HomeHorizontalShelfEndPadding)
    ) {
        items(
            items = entries,
            key = { "radar-${it.artistName}-${it.release.browseId.ifBlank { it.release.title }}" },
            contentType = { "release-radar-card" }
        ) { entry ->
            Column(
                modifier = Modifier.width(148.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(148.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(LevyraPanelSoft)
                        .pressable { onOpen(entry) },
                    contentAlignment = Alignment.Center
                ) {
                    if (entry.release.thumbnailUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(entry.release.thumbnailUrl).crossfade(false).build(),
                            contentDescription = entry.release.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        Icon(Icons.Rounded.Album, null, tint = LevyraMuted, modifier = Modifier.size(42.dp))
                    }
                    if (entry.isFresh) {
                        Text(
                            "NEW",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .background(LevyraPink.copy(alpha = 0.92f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(entry.release.title, color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    entry.artistName,
                    color = LevyraCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { onArtist(entry.artistName) }
                )
                Text(
                    entry.release.year.ifBlank { entry.release.subtitle },
                    color = LevyraMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun UpdateAvailableOverlay(
    update: AppUpdateInfo,
    onDownload: () -> Unit,
    onLater: () -> Unit
) {
    val blocker = remember { MutableInteractionSource() }
    val notes = remember(update.releaseNotes, update.latestVersionName) {
        cleanedUpdateNotes(update.releaseNotes, update.latestVersionName)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.86f))
            .clickable(interactionSource = blocker, indication = null) {}
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF0A1020),
            shape = RoundedCornerShape(30.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .shadow(28.dp, RoundedCornerShape(30.dp), clip = false)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(116.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        LevyraCyan.copy(alpha = 0.72f),
                                        Color(0xFF6B7CFF).copy(alpha = 0.62f),
                                        LevyraViolet.copy(alpha = 0.72f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(170.dp)
                                .offset(x = (-72).dp, y = (-48).dp)
                                .blur(42.dp)
                                .background(Color.White.copy(alpha = 0.16f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .offset(x = 96.dp, y = 44.dp)
                                .blur(48.dp)
                                .background(LevyraCyan.copy(alpha = 0.22f), CircleShape)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("LEVYRA", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Black, letterSpacing = 2.5.sp)
                            Surface(color = Color.Black.copy(alpha = 0.28f), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))) {
                                Text("NUOVO AGGIORNAMENTO", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp), letterSpacing = 1.4.sp)
                            }
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Versione ${update.latestVersionName}",
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(color = LevyraCyan.copy(alpha = 0.16f), shape = RoundedCornerShape(999.dp), border = BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.42f))) {
                                    Text("APK", color = LevyraCyan, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), letterSpacing = 0.8.sp)
                                }
                            }
                            Text(
                                text = update.releaseTitle.ifBlank { "Miglioramenti generali e risoluzione di bug." },
                                color = LevyraMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 17.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            color = Color.White.copy(alpha = 0.045f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(modifier = Modifier.size(8.dp).background(LevyraCyan, CircleShape))
                                Text("Schermata più compatta, changelog leggibile e contenuto scrollabile su tutti gli schermi.", color = LevyraText.copy(alpha = 0.86f), fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Novità", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                            if (notes.isEmpty()) {
                                UpdateNoteCard("Miglioramenti alle prestazioni e stabilità dell'app.")
                            } else {
                                notes.forEach { note ->
                                    UpdateNoteCard(note)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.04f),
                                shape = RoundedCornerShape(17.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.13f)),
                                modifier = Modifier.weight(1f).height(48.dp).pressable(onClick = onLater)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("Più tardi", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                color = LevyraCyan,
                                shape = RoundedCornerShape(17.dp),
                                modifier = Modifier.weight(1f).height(48.dp).pressable(onClick = onDownload)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("Aggiorna", color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Black)
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
private fun UpdateNoteCard(note: String) {
    Surface(
        color = Color.White.copy(alpha = 0.055f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.055f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.size(6.dp).background(LevyraCyan, CircleShape))
            Text(note, color = LevyraMuted, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun cleanedUpdateNotes(notes: String, version: String): List<String> {
    val versionKey = version.trim().lowercase()
    return notes
        .lineSequence()
        .map { it.trim() }
        .map { raw ->
            raw
                .replace(Regex("^#{1,6}\\s*"), "")
                .replace(Regex("^[-*•]+\\s*"), "")
                .replace(Regex("\\*\\*([^*]+)\\*\\*")) { match -> match.groupValues[1] }
                .replace(Regex("`([^`]+)`")) { match -> match.groupValues[1] }
                .replace(Regex("\\[([^\\]]+)]\\(([^)]+)\\)")) { match -> match.groupValues[1] }
                .replace("__", "")
                .replace("**", "")
                .trim()
        }
        .filter { line -> line.isNotBlank() && line != "---" && line.any { it.isLetterOrDigit() } }
        .filterNot { line ->
            val lower = line.lowercase()
            lower == "novità" ||
                lower == "changelog" ||
                lower.startsWith("levyra v$versionKey") ||
                lower.startsWith("levyra $versionKey") ||
                lower.startsWith("versione $versionKey")
        }
        .distinct()
        .take(12)
        .toList()
}

private fun compactReleaseNotes(notes: String): String {
    val clean = notes
        .lineSequence()
        .map { it.trim().trimStart('-', '*', '•').trim() }
        .filter { it.isNotBlank() }
        .take(3)
        .joinToString(" · ")
    return clean.ifBlank { "Correzioni, rifiniture e miglioramenti generali." }
}

private fun openExternalUrl(context: android.content.Context, url: String) {
    if (url.isBlank()) {
        Toast.makeText(context, "Link aggiornamento non disponibile", Toast.LENGTH_LONG).show()
        return
    }
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, "Impossibile aprire il download", Toast.LENGTH_LONG).show()
    }
}

@Composable
private fun QueueOverlay(
    state: LevyraUiState,
    onPlay: (Track) -> Unit,
    onPlayNext: (Track) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onUndo: () -> Unit,
    onToggleRadio: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val blocker = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(LevyraInk, LevyraBlack)))
            .clickable(interactionSource = blocker, indication = null) {}
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(strings.queue, color = LevyraText, fontSize = 26.sp, fontWeight = FontWeight.Black)
                        Text(
                            "${state.queue.size} brani · ${state.queueHistoryCount} nella cronologia",
                            color = LevyraMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    CircleIconButton(
                        icon = Icons.Rounded.Close,
                        tint = LevyraText,
                        background = Color.White.copy(alpha = 0.1f),
                        onClick = onClose
                    )
                }
            }
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.Bolt, null, tint = if (state.radioEnabled) LevyraCyan else LevyraMuted, modifier = Modifier.size(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Radio continua", color = LevyraText, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            Text("Aggiunge brani coerenti quando la coda sta finendo", color = LevyraMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Switch(checked = state.radioEnabled, onCheckedChange = { onToggleRadio() })
                        if (state.queueUndoAvailable) {
                            IconButton(onClick = onUndo) {
                                Icon(Icons.Rounded.Undo, "Annulla rimozione", tint = LevyraCyan)
                            }
                        }
                    }
                }
            }
            if (state.queue.isEmpty()) {
                item { Text(strings.queueEmpty, color = LevyraMuted, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
            } else {
                itemsIndexed(state.queue, key = { _, track -> "q-${System.identityHashCode(track)}-${track.id}-${track.videoUrl}" }) { index, track ->
                    val isCurrent = index == state.queueCurrentIndex
                    var dragDistance by remember(track) { mutableFloatStateOf(0f) }
                    val latestQueue by rememberUpdatedState(state.queue)
                    val latestMove by rememberUpdatedState(onMove)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = if (isCurrent) LevyraCyan.copy(alpha = 0.09f) else Color.White.copy(alpha = 0.035f),
                        border = BorderStroke(1.dp, if (isCurrent) LevyraCyan.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.055f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pressable(onClick = { onPlay(track) })
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Rounded.DragHandle,
                                "Trascina per riordinare",
                                tint = LevyraMuted,
                                modifier = Modifier
                                    .size(24.dp)
                                    .pointerInput(track) {
                                        var dragIndex = index
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                dragDistance = 0f
                                                dragIndex = latestQueue.indexOfFirst { it === track }
                                                    .takeIf { it >= 0 }
                                                    ?: latestQueue.indexOf(track).takeIf { it >= 0 }
                                                    ?: index
                                            },
                                            onDragCancel = { dragDistance = 0f },
                                            onDragEnd = { dragDistance = 0f },
                                            onDrag = { change, amount ->
                                                change.consume()
                                                dragDistance += amount.y
                                                val threshold = 46.dp.toPx()
                                                val lastIndex = latestQueue.lastIndex
                                                when {
                                                    dragDistance > threshold && dragIndex < lastIndex -> {
                                                        latestMove(dragIndex, dragIndex + 1)
                                                        dragIndex += 1
                                                        dragDistance = 0f
                                                    }
                                                    dragDistance < -threshold && dragIndex > 0 -> {
                                                        latestMove(dragIndex, dragIndex - 1)
                                                        dragIndex -= 1
                                                        dragDistance = 0f
                                                    }
                                                }
                                            }
                                        )
                                    }
                            )
                            CoverImage(track, Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(track.title, color = if (isCurrent) LevyraCyan else LevyraText, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(track.artist, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (isCurrent) {
                                Icon(Icons.Rounded.Equalizer, null, tint = LevyraCyan, modifier = Modifier.size(20.dp))
                            } else {
                                IconButton(onClick = { onPlayNext(track) }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Rounded.SkipNext, "Riproduci dopo", tint = LevyraText, modifier = Modifier.size(19.dp))
                                }
                                IconButton(onClick = { onRemove(index) }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Rounded.Delete, "Rimuovi", tint = LevyraMuted, modifier = Modifier.size(19.dp))
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
private fun SmartMusicProfileCard(profile: SmartMusicProfile, onPlayFlow: () -> Unit) {
    val artists = profile.topArtists.take(3).joinToString(" • ") { it.label }
    val albums = profile.topAlbums.take(2).joinToString(" • ") { it.label.substringBefore(" • ") }
    Surface(
        color = Color.White.copy(alpha = 0.055f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Brush.linearGradient(listOf(LevyraCyan.copy(alpha = 0.16f), LevyraViolet.copy(alpha = 0.10f), Color.Transparent)))
            )
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(LevyraCyan, LevyraViolet))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.GraphicEq, null, tint = Color.White, modifier = Modifier.size(23.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Smart Music Profile", color = LevyraText, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (profile.isWarm) "Personalizzazione attiva" else "Sto imparando dai tuoi ascolti",
                            color = LevyraMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        color = LevyraCyan.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.24f)),
                        shape = CircleShape,
                        modifier = Modifier.pressable(onClick = onPlayFlow)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Rounded.PlayArrow, null, tint = LevyraCyan, modifier = Modifier.size(18.dp))
                            Text("Flow", color = LevyraCyan, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                if (artists.isNotBlank()) {
                    Text("Artisti: $artists", color = LevyraText.copy(alpha = 0.88f), fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (albums.isNotBlank()) {
                    Text("Album mood: $albums", color = LevyraMuted, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun LyricsOverlay(state: LevyraUiState, onTranslation: (Boolean) -> Unit, onClose: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val track = state.currentTrack
    val accentStart = if (track != null) Color(track.accentStart) else LevyraCyan
    val accentEnd = if (track != null) Color(track.accentEnd) else LevyraViolet
    val blocker = remember { MutableInteractionSource() }
    val listState = rememberLazyListState()
    val active = state.activeLyric
    val activeIndex = if (state.lyricsSynced && active != null) state.lyrics.indexOf(active) else -1

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            runCatching { listState.animateScrollToItem(activeIndex.coerceAtLeast(0)) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(interactionSource = blocker, indication = null) {}
    ) {
        LevyraBackground(accentStart = track?.accentStart, accentEnd = track?.accentEnd)
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f)))
        
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track?.title ?: strings.lyrics,
                            color = LevyraText,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(track?.artist ?: "", color = LevyraMuted, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    CircleIconButton(
                        icon = Icons.Rounded.Close,
                        tint = LevyraText,
                        background = Color.White.copy(alpha = 0.15f),
                        onClick = onClose
                    )
                }
            }
            item {
                LyricsProStatusRow(
                    provider = state.lyricsProvider,
                    synced = state.lyricsSynced,
                    cached = state.lyricsCached,
                    confidence = state.lyricsConfidence,
                    syncedLabel = strings.synced
                )
            }
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White.copy(alpha = 0.055f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.Translate, null, tint = if (state.lyricsTranslationEnabled) LevyraCyan else LevyraMuted)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Traduzione automatica", color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            Text("Usa le lingue disponibili nei transcript YouTube", color = LevyraMuted, fontSize = 11.sp)
                        }
                        Switch(
                            checked = state.lyricsTranslationEnabled,
                            onCheckedChange = onTranslation
                        )
                    }
                }
            }
            if (state.lyrics.isEmpty()) {
                item { Text(strings.lyricsUnavailable, color = LevyraMuted, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            } else {
                itemsIndexed(state.lyrics) { index, line ->
                    val isActive = state.lyricsSynced && index == activeIndex
                    val lineColor = when {
                        isActive -> Color.White
                        state.lyricsSynced -> Color.White.copy(alpha = 0.5f)
                        else -> Color.White.copy(alpha = 0.85f)
                    }
                    val annotatedLine = buildAnnotatedString {
                        if (isActive && line.words.isNotEmpty()) {
                            line.words.forEachIndexed { wordIndex, word ->
                                val wordActive = state.positionMs >= word.startMs && state.positionMs <= word.endMs
                                withStyle(
                                    SpanStyle(
                                        color = if (wordActive) accentStart else Color.White.copy(alpha = 0.56f),
                                        fontWeight = if (wordActive) FontWeight.Black else FontWeight.Bold
                                    )
                                ) {
                                    append(word.text)
                                }
                                if (wordIndex < line.words.lastIndex) append(" ")
                            }
                        } else {
                            append(line.text)
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = annotatedLine,
                            color = lineColor,
                            fontSize = if (isActive) 28.sp else 22.sp,
                            lineHeight = if (isActive) 34.sp else 28.sp,
                            fontWeight = if (isActive) FontWeight.Black else FontWeight.Bold,
                            modifier = Modifier.graphicsLayer {
                                scaleX = if (isActive) 1f else 0.95f
                                scaleY = if (isActive) 1f else 0.95f
                                transformOrigin = TransformOrigin(0f, 0.5f)
                            }
                        )
                        if (line.translated.isNotBlank()) {
                            Text(
                                text = line.translated,
                                color = if (isActive) accentEnd.copy(alpha = 0.95f) else LevyraMuted,
                                fontSize = if (isActive) 17.sp else 15.sp,
                                lineHeight = 21.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsProStatusRow(provider: String, synced: Boolean, cached: Boolean, confidence: Int, syncedLabel: String) {
    val label = buildString {
        append(if (synced) syncedLabel else "Lyrics Pro")
        if (provider.isNotBlank()) append(" • ").append(provider)
        if (cached) append(" • cache")
        if (confidence > 0) append(" • ").append(confidence).append("%")
    }
    Surface(
        color = if (synced) LevyraCyan.copy(alpha = 0.18f) else LevyraViolet.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, if (synced) LevyraCyan.copy(alpha = 0.28f) else LevyraViolet.copy(alpha = 0.26f)),
        shape = CircleShape
    ) {
        Text(label, color = if (synced) LevyraCyan else LevyraText, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

@Composable
private fun LevyraBackground(accentStart: Int?, accentEnd: Int?) {
    val auroraBlue = Color(0xFF0B5F86)
    val electricCyan = Color(0xFF52D9FF)
    val emberOrange = Color(0xFFFF6A1A)
    val deepViolet = Color(0xFF4B2A8C)
    val softMagenta = Color(0xFFB7477A)

    val sourceStart = accentStart?.let { Color(it) }
    val sourceEnd = accentEnd?.let { Color(it) }

    val warmAccent by animateColorAsState(
        targetValue = if (LevyraIsLight) LevyraOrange else emberOrange,
        animationSpec = tween(1800, easing = LinearOutSlowInEasing),
        label = "levyra-warm-background-accent"
    )
    val coolAccent by animateColorAsState(
        targetValue = sourceStart?.let { source ->
            val red = source.red
            val green = source.green
            val blue = source.blue
            if (green > red * 1.08f && green > blue * 0.90f) auroraBlue else source
        } ?: if (LevyraIsLight) LevyraCyan else auroraBlue,
        animationSpec = tween(1800, easing = LinearOutSlowInEasing),
        label = "levyra-cool-background-accent"
    )
    val secondAccent by animateColorAsState(
        targetValue = sourceEnd?.let { source ->
            val red = source.red
            val green = source.green
            val blue = source.blue
            if (green > red * 1.08f && green > blue * 0.90f) deepViolet else source
        } ?: if (LevyraIsLight) LevyraViolet else deepViolet,
        animationSpec = tween(1800, easing = LinearOutSlowInEasing),
        label = "levyra-secondary-background-accent"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "levyra-background-breath")
    val driftY by infiniteTransition.animateFloat(
        initialValue = -18f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "levyra-background-drift-y"
    )
    val driftX by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "levyra-background-drift-x"
    )

    if (LevyraIsLight) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFF8FAFF),
                            LevyraBlack,
                            Color(0xFFEFF3FB)
                        )
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(560.dp)
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        translationX = driftX
                        translationY = driftY
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                coolAccent.copy(alpha = 0.13f),
                                LevyraCyan.copy(alpha = 0.075f),
                                Color.Transparent
                            ),
                            radius = 1050f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(540.dp)
                    .align(Alignment.TopStart)
                    .offset(x = (-210).dp, y = 90.dp)
                    .graphicsLayer {
                        translationX = driftX * -0.55f
                        translationY = driftY * 0.35f
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                secondAccent.copy(alpha = 0.10f),
                                LevyraViolet.copy(alpha = 0.055f),
                                Color.Transparent
                            ),
                            radius = 760f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(610.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 245.dp, y = 18.dp)
                    .graphicsLayer {
                        translationX = driftX * 0.65f
                        translationY = driftY * -0.45f
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                warmAccent.copy(alpha = 0.075f),
                                Color.Transparent
                            ),
                            radius = 860f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.64f),
                                LevyraBlack.copy(alpha = 0.96f)
                            )
                        )
                    )
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF030508),
                            Color(0xFF05070E),
                            Color(0xFF06030A),
                            Color(0xFF010103)
                        )
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(620.dp)
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        translationX = driftX
                        translationY = driftY
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                coolAccent.copy(alpha = 0.20f),
                                electricCyan.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            radius = 1120f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(560.dp)
                    .align(Alignment.TopStart)
                    .offset(x = (-190).dp, y = 72.dp)
                    .graphicsLayer {
                        translationX = driftX * -0.55f
                        translationY = driftY * 0.35f
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                secondAccent.copy(alpha = 0.16f),
                                softMagenta.copy(alpha = 0.07f),
                                Color.Transparent
                            ),
                            radius = 720f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(620.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 240.dp, y = 20.dp)
                    .graphicsLayer {
                        translationX = driftX * 0.65f
                        translationY = driftY * -0.45f
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                warmAccent.copy(alpha = 0.18f),
                                Color(0xFFFF9A3D).copy(alpha = 0.055f),
                                Color.Transparent
                            ),
                            radius = 820f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0xFF020103).copy(alpha = 0.86f),
                                Color.Black
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun HomeScreen(viewModel: LevyraViewModel, state: LevyraUiState) {
    val strings = LocalLevyraStrings.current
    val context = LocalContext.current
    var addTarget by remember { mutableStateOf<Track?>(null) }
    val trendingArtists = remember(state.tracks, state.homeSections, state.charts, state.favorites) {
        buildTrendingArtists(state)
    }
    val personalTracks = remember(state.personalOrbitTracks) {
        state.personalOrbitTracks.take(LevyraPersonalOrbit.DISPLAY_LIMIT)
    }
    val resonanceTracks = remember(state.currentTrack, state.recentSearches, state.favorites, state.tracks, state.homeSections, state.charts) {
        buildResonanceTracks(state)
    }
    val newReleases = remember(state.homeSections) {
        state.homeSections.firstOrNull { isVerifiedReleaseSectionTitle(it.title) }
    }
    val otherSections = remember(state.homeSections) {
        state.homeSections.filter { !isVerifiedReleaseSectionTitle(it.title) && !isQuickPicksSectionTitle(it.title) }
    }
    val secondaryPreloadTracks = remember(resonanceTracks, newReleases) {
        (resonanceTracks + (newReleases?.tracks ?: emptyList())).distinctBy { it.id }.take(10)
    }
    val chartChunks = remember(state.charts) { state.charts.chunked(4) }
    val homeListState = rememberLazyListState()
    LaunchedEffect(personalTracks, secondaryPreloadTracks) {
        delay(500L)
        while (homeListState.isScrollInProgress) delay(120L)
        if (personalTracks.isNotEmpty()) {
            LevyraArtworkCache.preloadPriority(context, personalTracks, LevyraPersonalOrbit.DISPLAY_LIMIT)
        }
        LevyraArtworkCache.preloadHome(context, secondaryPreloadTracks, 10)
    }
    LazyColumn(
        state = homeListState,
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(top = 10.dp, bottom = if (state.currentTrack != null) 188.dp else 104.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item(key = "home-top", contentType = "home-header") {
            HomeSectionInset {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    GreetingBar(state.userName, state.isResolving, onSettings = viewModel::openSettings)
                    MoodRow(moods = state.moods, selectedId = state.selectedMood?.id, onSelect = viewModel::selectMood)
                }
            }
        }
        if (personalTracks.isNotEmpty()) {
            item(key = "home-personal", contentType = "home-shelf") {
                PersonalListeningShelf(
                    tracks = personalTracks,
                    currentId = state.currentTrack?.id,
                    isPlaying = state.isPlaying,
                    isResolving = state.isResolving,
                    onPlay = { track -> viewModel.playFrom(personalTracks, track) },
                    onPlayAll = { viewModel.playAll(personalTracks) }
                )
            }
        }
        if (resonanceTracks.isNotEmpty()) {
            item(key = "home-resonance", contentType = "home-shelf") {
                ResonanceShelf(
                    tracks = resonanceTracks,
                    currentId = state.currentTrack?.id,
                    isPlaying = state.isPlaying,
                    isResolving = state.isResolving,
                    onPlay = { track -> viewModel.playFrom(resonanceTracks, track) },
                    onPlayAll = { viewModel.playAll(resonanceTracks) }
                )
            }
        }
        if (state.currentTrack != null) {
            item(key = "home-continue", contentType = "home-card") {
                HomeSectionInset {
                    ContinueListeningCard(
                        track = state.currentTrack,
                        isPlaying = state.isPlaying,
                        isResolving = state.isResolving,
                        progress = progressOf(state.positionMs, state.durationMs),
                        onResume = viewModel::togglePlay
                    )
                }
            }
        }

        if (state.releaseRadar.isNotEmpty()) {
            item(key = "sec-release-radar-header", contentType = "home-section-header") {
                HomeSectionInset { SectionTitle("📡 ${strings.releaseRadar}") }
            }
            item(key = "sec-release-radar-row", contentType = "home-horizontal-row") {
                ReleaseRadarRow(
                    entries = state.releaseRadar,
                    onOpen = { entry -> viewModel.searchNow("${entry.release.title} ${entry.artistName}") },
                    onArtist = viewModel::openArtistByName
                )
            }
        }
        if (state.similarArtists.isNotEmpty()) {
            item(key = "sec-similar-artists-header", contentType = "home-section-header") {
                HomeSectionInset { SectionTitle("✨ ${strings.similarToFollowed}") }
            }
            item(key = "sec-similar-artists-row", contentType = "home-horizontal-row") {
                ArtistHitRow(
                    artists = state.similarArtists,
                    contentPadding = PaddingValues(start = HomeHorizontalInset, end = HomeHorizontalShelfEndPadding),
                    onClick = viewModel::openArtistFromHit
                )
            }
        }
        if (newReleases != null && newReleases.tracks.isNotEmpty()) {
            item(key = "sec-new-releases-header", contentType = "home-section-header") {
                HomeSectionInset { SectionHeaderAction(strings.newReleases, onPlayAll = { viewModel.playAll(newReleases.tracks) }) }
            }
            item(key = "sec-new-releases-row", contentType = "home-horizontal-row") {
                AlbumCardRow(
                    tracks = newReleases.tracks,
                    currentId = state.currentTrack?.id,
                    animationsEnabled = state.animationsEnabled,
                    onPlay = { viewModel.playFrom(newReleases.tracks, it) }
                )
            }
        }
        if (state.homeAlbums.isNotEmpty() || state.homeAlbumsLoading) {
            item(key = "sec-home-albums-header", contentType = "home-section-header") {
                HomeSectionInset { SectionHeaderAction(strings.albumsForYou, onPlayAll = { viewModel.playAlbumRecommendations(state.homeAlbums) }) }
            }
            item(key = "sec-home-albums-row", contentType = "home-horizontal-row") {
                if (state.homeAlbums.isNotEmpty()) {
                    HomeAlbumHitRow(
                        albums = state.homeAlbums,
                        animationsEnabled = state.animationsEnabled,
                        onOpen = viewModel::openAlbum
                    )
                } else {
                    HomeAlbumLoadingRow()
                }
            }
        }
        if (trendingArtists.isNotEmpty()) {
            item(key = "home-trending-artists", contentType = "home-shelf") {
                TrendingArtistsShelf(
                    artists = trendingArtists,
                    onArtistClick = viewModel::openArtistByName
                )
            }
        }
        otherSections.forEachIndexed { index, section ->
            if (section.tracks.isNotEmpty()) {
                item(key = "sec-other-${index}-header", contentType = "home-section-header") {
                    HomeSectionInset { SectionHeaderAction(section.title, onPlayAll = { viewModel.playAll(section.tracks) }) }
                }
                item(key = "sec-other-${index}-row", contentType = "home-horizontal-row") {
                    AlbumCardRow(
                        tracks = section.tracks,
                        currentId = state.currentTrack?.id,
                        animationsEnabled = state.animationsEnabled,
                        onPlay = { viewModel.playFrom(section.tracks, it) }
                    )
                }
            }
        }
        item(key = "home-chart-title", contentType = "home-section-header") {
            val region = state.chartRegions.firstOrNull { it.id == state.selectedChartId }
            HomeSectionInset {
                SectionHeaderAction("Top 50 ${region?.label ?: "Global"} ${region?.emoji ?: ""}", onPlayAll = { viewModel.playAll(state.charts) })
            }
        }
        item(key = "home-chart-regions", contentType = "home-horizontal-row") {
            HomeSectionInset {
                ChartRegionRow(regions = state.chartRegions, selectedId = state.selectedChartId, loading = state.isLoadingCharts, onSelect = viewModel::selectChart)
            }
        }
        if (state.charts.isEmpty()) {
            item(key = "home-chart-empty", contentType = "home-card") {
                HomeSectionInset {
                    if (state.isLoadingCharts) {
                        ChartLoadingSkeleton()
                    } else {
                        GlassMessage(strings.top50Unavailable, LevyraOrange)
                    }
                }
            }
        }
        if (state.charts.isNotEmpty()) {
            item(key = "home-chart-row", contentType = "home-horizontal-row") {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(start = HomeHorizontalInset, end = HomeHorizontalShelfEndPadding)
                ) {
                    itemsIndexed(
                        items = chartChunks,
                        key = { chunkIndex, chunk -> "chart-column-$chunkIndex-${chunk.joinToString("-") { it.id }}" },
                        contentType = { _, _ -> "chart-column" }
                    ) { chunkIndex, chunk ->
                        Column(modifier = Modifier.width(320.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            chunk.forEachIndexed { itemIndex, track ->
                                val rank = chunkIndex * 4 + itemIndex + 1
                                ChartRow(
                                    rank = rank,
                                    track = track,
                                    isCurrent = track.id == state.currentTrack?.id,
                                    isPlaying = state.isPlaying && track.id == state.currentTrack?.id,
                                    isResolving = state.isResolving && track.id == state.currentTrack?.id,
                                    isFavorite = track.id in state.favoriteIds,
                                    onClick = { viewModel.playFrom(state.charts, track) },
                                    onFavorite = { viewModel.toggleFavorite(track) },
                                    onAddToPlaylist = { addTarget = track },
                                    onAddToQueue = { viewModel.addToQueue(track) }
                                )
                            }
                        }
                    }
                }
            }
        }
        item(key = "home-status", contentType = "home-card") {
            HomeSectionInset { StatusBlock(state) }
        }
    }

    addTarget?.let { track ->
        AddToPlaylistDialog(
            track = track,
            playlists = state.playlists,
            onDismiss = { addTarget = null },
            onAddTo = { playlistId ->
                viewModel.addToPlaylist(playlistId, track)
                addTarget = null
            },
            onCreateWith = { name ->
                viewModel.createPlaylist(name, track)
                addTarget = null
            }
        )
    }
}

private data class HomeHeroUpdate(
    val track: Track,
    val sourceTitle: String,
    val verifiedRelease: Boolean
)

private fun pickHeroUpdate(state: LevyraUiState): HomeHeroUpdate? {
    val currentId = state.currentTrack?.id
    val verifiedReleases = state.homeSections
        .asSequence()
        .filter { isVerifiedReleaseSectionTitle(it.title) }
        .flatMap { section ->
            section.tracks.asSequence()
                .filter { isReliableMusicUpdateCandidate(it) }
                .map { track -> HomeHeroUpdate(track, section.title, true) }
        }
    val trustedEditorial = state.homeSections
        .asSequence()
        .filterNot { isQuickPicksSectionTitle(it.title) }
        .filterNot { isVerifiedReleaseSectionTitle(it.title) }
        .flatMap { section ->
            section.tracks.asSequence()
                .filter { isReliableMusicUpdateCandidate(it) }
                .map { track -> HomeHeroUpdate(track, section.title, false) }
        }
    val chartUpdates = state.charts
        .asSequence()
        .filter { isReliableMusicUpdateCandidate(it) }
        .map { track -> HomeHeroUpdate(track, "YouTube Charts Italia", false) }
    val libraryUpdates = sequenceOf(
        state.tracks.asSequence(),
        state.favorites.asSequence(),
        state.currentTrack?.let { sequenceOf(it) } ?: emptySequence()
    )
        .flatten()
        .filter { isReliableMusicUpdateCandidate(it) }
        .map { track -> HomeHeroUpdate(track, track.source.ifBlank { "YouTube Music" }, false) }
    return sequenceOf(verifiedReleases, trustedEditorial, chartUpdates, libraryUpdates)
        .flatten()
        .distinctBy { it.track.id }
        .firstOrNull { it.track.id != currentId }
        ?: state.currentTrack?.let { HomeHeroUpdate(it, it.source.ifBlank { "YouTube Music" }, false) }
}

private data class TrendingArtist(val name: String, val imageUrl: String)

private fun buildTrendingArtists(state: LevyraUiState): List<TrendingArtist> {
    val allTracks = buildList {
        addAll(state.charts)
        addAll(state.homeSections.flatMap { it.tracks })
        addAll(state.tracks)
        addAll(state.favorites)
    }
    return allTracks
        .filter { it.artist.isNotBlank() && it.thumbnailUrl.isNotBlank() && !it.artist.contains("Unknown", ignoreCase = true) }
        .distinctBy { it.artist }
        .take(10)
        .map { TrendingArtist(name = it.artist, imageUrl = it.thumbnailUrl) }
}

@Composable
private fun TrendingArtistsShelf(
    artists: List<TrendingArtist>,
    onArtistClick: (String) -> Unit
) {
    val strings = LocalLevyraStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = HomeHorizontalInset),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(22.dp)
                    .width(4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Brush.verticalGradient(listOf(LevyraCyan, LevyraViolet)))
            )
            Text(
                text = strings.artists,
                color = LevyraText,
                fontSize = 21.sp,
                lineHeight = 23.sp,
                letterSpacing = (-0.55).sp,
                fontWeight = FontWeight.Black
            )
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(start = HomeHorizontalInset, end = HomeHorizontalShelfEndPadding)
        ) {
            items(
                items = artists,
                key = { it.name },
                contentType = { "trending-artist" }
            ) { artist ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(86.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onArtistClick(artist.name) }
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        LevyraCyan.copy(alpha = 0.62f),
                                        LevyraViolet.copy(alpha = 0.56f)
                                    )
                                )
                            )
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = artist.imageUrl,
                            contentDescription = artist.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(CinematicGlassDeep)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = artist.name,
                        color = LevyraText,
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private fun buildResonanceTracks(state: LevyraUiState): List<Track> {
    return buildList {
        addAll(state.charts)
        addAll(state.homeSections.flatMap { it.tracks })
        addAll(state.favorites)
        addAll(state.tracks)
        state.currentTrack?.let { add(it) }
    }
        .asSequence()
        .filter { it.id.length == 11 && isReliableMusicUpdateCandidate(it) }
        .filter { it.title.isNotBlank() && it.artist.isNotBlank() }
        .distinctBy { it.id }
        .sortedWith(compareByDescending<Track> { it.replayScore + it.vocal + it.cacheScore / 2 }.thenBy { it.title })
        .take(8)
        .toList()
}

@Composable
private fun ResonanceShelf(
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: (Track) -> Unit,
    onPlayAll: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HomeHorizontalInset),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        LevyraViolet.copy(alpha = 0.72f),
                                        LevyraCyan.copy(alpha = 0.52f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Text(
                        text = strings.voicesTitle,
                        color = LevyraText,
                        fontSize = 22.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.45).sp
                    )
                }
                Text(
                    text = strings.voicesSubtitle,
                    color = LevyraMuted,
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                color = LevyraAdaptiveChip,
                border = BorderStroke(Dp.Hairline, LevyraAdaptiveHairline),
                shape = CircleShape,
                modifier = Modifier.pressable(onClick = onPlayAll)
            ) {
                Box(
                    modifier = Modifier.size(38.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = strings.play,
                        tint = LevyraViolet,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(start = HomeHorizontalInset, end = HomeHorizontalShelfEndPadding)
        ) {
            itemsIndexed(
                items = tracks,
                key = { _, track -> track.id },
                contentType = { _, _ -> "resonance-card" }
            ) { index, track ->
                ResonanceCard(
                    track = track,
                    index = index,
                    active = track.id == currentId,
                    playing = isPlaying && track.id == currentId,
                    resolving = isResolving && track.id == currentId,
                    onClick = { onPlay(track) }
                )
            }
        }
    }
}

@Composable
private fun ResonanceCard(
    track: Track,
    index: Int,
    active: Boolean,
    playing: Boolean,
    resolving: Boolean,
    onClick: () -> Unit
) {
    val accentStart = Color(track.accentStart)
    val accentEnd = Color(track.accentEnd)
    val score = (track.replayScore + track.vocal + track.cacheScore / 2 + index * 7).coerceAtLeast(24)
    val comments = 520 + (score * 31) % 4200
    val engagement = minOf(99, score % 100)
    val pulseWidth = ((score % 72) + 24) / 100f
    Box(
        modifier = Modifier
            .width(280.dp)
            .height(112.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = if (active) 1.5.dp else Dp.Hairline,
                color = if (active) LevyraCyan.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp)
            )
            .pressable(onClick = onClick)
    ) {
        CoverImage(
            track = track,
            modifier = Modifier
                .fillMaxSize()
                .scale(1.4f)
                .blur(36.dp),
            highRes = false
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.65f)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(cinematicGlassBrush(accentStart, accentEnd, 0.45f))
        )
        
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(Dp.Hairline, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            ) {
                CoverImage(
                    track = track,
                    modifier = Modifier.fillMaxSize(),
                    highRes = false
                )
                if (active) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        ActiveTrackEqualizer(
                            color = LevyraCyan,
                            isPlaying = playing,
                            width = 20.dp,
                            height = 16.dp
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = track.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = LevyraOrange, modifier = Modifier.size(13.dp))
                            Text(
                                text = "$engagement%",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        
                        Text(
                            text = formatCompactNumber(comments),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(pulseWidth.coerceIn(0.1f, 1f))
                                .height(3.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(Brush.horizontalGradient(listOf(accentStart, accentEnd)))
                        )
                    }
                }
            }
        }
    }
}

private fun formatCompactNumber(value: Int): String {
    return if (value >= 1000) {
        val major = value / 1000
        val minor = (value % 1000) / 100
        if (minor == 0) "$major K" else "$major,$minor K"
    } else {
        value.toString()
    }
}

@Composable
private fun PersonalListeningShelf(
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: (Track) -> Unit,
    onPlayAll: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val shelfTracks = remember(tracks) { tracks.distinctBy { it.id }.take(LevyraPersonalOrbit.DISPLAY_LIMIT) }
    val pages = remember(shelfTracks) { shelfTracks.chunked(6) }
    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HomeHorizontalInset),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        LevyraCyan.copy(alpha = 0.30f),
                                        LevyraViolet.copy(alpha = 0.42f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = if (LevyraIsLight) LevyraViolet else Color(0xFFB7A7FF),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = strings.personalOrbitTitle,
                        color = LevyraText,
                        fontSize = 25.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.6).sp
                    )
                }
                Text(
                    text = strings.personalOrbitSubtitle,
                    color = LevyraMuted,
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                color = LevyraAdaptiveChip,
                border = BorderStroke(Dp.Hairline, LevyraAdaptiveHairline),
                shape = CircleShape,
                modifier = Modifier.pressable(onClick = onPlayAll)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = LevyraCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = strings.play,
                        color = LevyraText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            LevyraViolet.copy(alpha = if (LevyraIsLight) 0.05f else 0.10f),
                            LevyraCyan.copy(alpha = if (LevyraIsLight) 0.025f else 0.045f),
                            Color.Transparent
                        ),
                        radius = 900f
                    )
                )
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = HomeHorizontalInset),
                pageSpacing = 14.dp
            ) { pageIndex ->
                val pageTracks = pages.getOrElse(pageIndex) { emptyList() }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(2) { rowIndex ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            repeat(3) { columnIndex ->
                                val track = pageTracks.getOrNull(rowIndex * 3 + columnIndex)
                                if (track != null) {
                                    PersonalListeningCard(
                                        track = track,
                                        active = track.id == currentId,
                                        playing = isPlaying && track.id == currentId,
                                        resolving = isResolving && track.id == currentId,
                                        onClick = { onPlay(track) },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (pages.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                pages.indices.forEach { index ->
                    val indicatorWidth by animateDpAsState(
                        targetValue = if (pagerState.currentPage == index) 20.dp else 6.dp,
                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                        label = "orbit-page-indicator-$index"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            .width(indicatorWidth)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index) {
                                    Brush.horizontalGradient(listOf(LevyraCyan, LevyraViolet))
                                } else {
                                    Brush.horizontalGradient(
                                        listOf(
                                            LevyraMuted.copy(alpha = 0.34f),
                                            LevyraMuted.copy(alpha = 0.22f)
                                        )
                                    )
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonalListeningCard(
    track: Track,
    active: Boolean,
    playing: Boolean,
    resolving: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(17.dp)
    Surface(
        color = CinematicGlassDeep,
        border = BorderStroke(
            width = if (active) 1.5.dp else Dp.Hairline,
            color = if (active) LevyraCyan.copy(alpha = 0.86f) else LevyraAdaptiveHairline
        ),
        shape = cardShape,
        modifier = modifier
            .aspectRatio(1f)
            .pressable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            CoverImage(
                track = track,
                modifier = Modifier.fillMaxSize(),
                highRes = false
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.46f to Color.Transparent,
                            0.72f to Color.Black.copy(alpha = 0.35f),
                            1.0f to Color.Black.copy(alpha = 0.90f)
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 34.dp, bottom = 9.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = track.title,
                    color = LevyraReadableOnArtwork,
                    fontSize = 11.5.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    color = LevyraReadableMutedOnArtwork,
                    fontSize = 9.5.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                color = Color.Black.copy(alpha = 0.58f),
                border = BorderStroke(Dp.Hairline, Color.White.copy(alpha = 0.18f)),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(25.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    when {
                        resolving -> CircularProgressIndicator(
                            modifier = Modifier.size(11.dp),
                            strokeWidth = 1.4.dp,
                            color = LevyraCyan
                        )
                        active -> ActiveTrackEqualizer(
                            color = LevyraCyan,
                            isPlaying = playing,
                            width = 11.dp,
                            height = 8.dp
                        )
                        else -> Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            if (active) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .height(5.dp)
                        .width(28.dp)
                        .clip(CircleShape)
                        .background(Brush.horizontalGradient(listOf(LevyraCyan, LevyraViolet)))
                )
            }
        }
    }
}

private fun isQuickPicksSectionTitle(title: String): Boolean {
    val normalized = title.lowercase()
    return normalized.contains("scelte rapide") ||
        normalized.contains("quick picks") ||
        normalized.contains("quick pick") ||
        normalized.contains("scelte per te")
}

private fun isVerifiedReleaseSectionTitle(title: String): Boolean {
    val normalized = title.lowercase()
    return normalized.contains("novità") ||
        normalized.contains("nuove uscite") ||
        normalized.contains("appena usciti") ||
        normalized.contains("ultime uscite") ||
        normalized.contains("nuovi album") ||
        normalized.contains("nuovi singoli") ||
        normalized.contains("new releases") ||
        normalized.contains("new release") ||
        normalized.contains("latest releases") ||
        normalized.contains("latest release") ||
        normalized.contains("new albums") ||
        normalized.contains("new singles")
}

private fun releaseKindFromSource(title: String, track: Track): String {
    val normalized = title.lowercase()
    val album = track.album.trim()
    return when {
        normalized.contains("album") -> "album"
        normalized.contains("single") || normalized.contains("singol") -> "singolo"
        album.isNotBlank() &&
            !album.equals("YouTube Music", ignoreCase = true) &&
            !album.equals(track.title, ignoreCase = true) -> "album"
        else -> "uscita"
    }
}

/**
 * True only when the track carries a genuine square album cover.
 *
 * YouTube Music serves album/song art from Google's image CDN as square,
 * resizable URLs (`=w544-h544...` or `=s...`). "Songs" that are really music
 * videos instead come back with a 16:9 video frame from `i.ytimg.com/vi/...`
 * (hqdefault/mqdefault/…). Those framegrabs look wrong in a cover grid, so the
 * personal orbit shelf keeps only tracks backed by real artwork.
 */
private val SQUARE_ART_WIDTH_HEIGHT_PATTERN = Regex("=w\\d+-h\\d+")
private val SQUARE_ART_SIZE_PATTERN = Regex("=s\\d+")

private fun hasSquareAlbumArtwork(track: Track): Boolean {
    val url = track.thumbnailUrl.ifBlank { track.largeThumbnailUrl }.trim()
    if (url.isBlank()) return false
    val lower = url.lowercase()
    val looksLikeVideoFrame = lower.contains("/vi/") ||
        lower.contains("/vi_webp/") ||
        lower.contains("ytimg.com/an_webp") ||
        lower.contains("hqdefault") ||
        lower.contains("mqdefault") ||
        lower.contains("sddefault") ||
        lower.contains("maxresdefault") ||
        lower.contains("hq720") ||
        lower.endsWith("default.jpg") ||
        lower.endsWith("default.webp")
    if (looksLikeVideoFrame) return false
    return lower.contains("googleusercontent.com") ||
        lower.contains("ggpht.com") ||
        SQUARE_ART_WIDTH_HEIGHT_PATTERN.containsMatchIn(url) ||
        SQUARE_ART_SIZE_PATTERN.containsMatchIn(url)
}

private fun isReliableMusicUpdateCandidate(track: Track): Boolean {
    val title = track.title.trim()
    val artist = track.artist.trim()
    if (title.length < 2 || artist.length < 2) return false
    if (artist.equals("YouTube Music", ignoreCase = true) || artist.equals("YouTube", ignoreCase = true)) return false
    return !isLikelyPlaylistOrCompilation(track)
}

private fun isLikelyPlaylistOrCompilation(track: Track): Boolean {
    val combined = listOf(track.title, track.artist, track.album).joinToString(" ").lowercase()
    val markers = listOf(
        "playlist",
        "mix",
        "top hit",
        "top hits",
        "hit italiane",
        "canzoni italiane",
        "musica italiana",
        "estate mix",
        "summer mix",
        "best of",
        "compilation",
        "classifica",
        "radio edit",
        "sped up",
        "slowed",
        "nightcore"
    )
    return markers.any { combined.contains(it) }
}

private data class HomeUpdateCopy(
    val badge: String,
    val headline: String,
    val detail: String,
    val caption: String,
    val sourceLabel: String,
    val primaryAction: String,
    val icon: ImageVector
)

private fun buildHomeUpdateCopy(update: HomeHeroUpdate): HomeUpdateCopy {
    val track = update.track
    val artist = track.artist.ifBlank { "Artista" }
    val title = track.title.ifBlank { "Brano" }
    val source = track.source.ifBlank { "YouTube Music" }
    val sourceTitle = update.sourceTitle.trim().ifBlank { source }
    val sourceLabel = buildProfessionalSourceLabel(source, sourceTitle)
    if (!update.verifiedRelease) {
        val chartDriven = isChartDrivenSource(sourceTitle) || isChartDrivenSource(source)
        return HomeUpdateCopy(
            badge = if (chartDriven) "TREND ITALIA" else "RADAR MUSICALE",
            headline = title,
            detail = artist,
            caption = if (chartDriven) "In evidenza nelle classifiche italiane." else "Selezionato oggi dal tuo flusso musicale.",
            sourceLabel = sourceLabel,
            primaryAction = "Ascolta",
            icon = if (chartDriven) Icons.Rounded.Equalizer else Icons.Rounded.GraphicEq
        )
    }
    val kind = releaseKindFromSource(sourceTitle, track)
    val album = track.album.trim().takeIf {
        it.isNotBlank() &&
            !it.equals("YouTube Music", ignoreCase = true) &&
            !it.equals(title, ignoreCase = true)
    }
    return when (kind) {
        "album" -> HomeUpdateCopy(
            badge = "NUOVO ALBUM",
            headline = album ?: title,
            detail = artist,
            caption = if (album != null) "Include anche “$title”." else "Disponibile nelle nuove uscite.",
            sourceLabel = sourceLabel,
            primaryAction = "Apri",
            icon = Icons.Rounded.Album
        )
        "singolo" -> HomeUpdateCopy(
            badge = "NUOVO SINGOLO",
            headline = title,
            detail = artist,
            caption = "Disponibile ora nelle nuove uscite.",
            sourceLabel = sourceLabel,
            primaryAction = "Apri",
            icon = Icons.Rounded.MusicNote
        )
        else -> HomeUpdateCopy(
            badge = "NUOVA USCITA",
            headline = title,
            detail = artist,
            caption = "Una novità appena entrata nel radar.",
            sourceLabel = sourceLabel,
            primaryAction = "Apri",
            icon = Icons.Rounded.MusicNote
        )
    }
}

private fun buildProfessionalSourceLabel(source: String, sourceTitle: String): String {
    return listOf(source, sourceTitle)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .map { it.removePrefix("Fonte:").trim() }
        .distinctBy { it.lowercase() }
        .joinToString(" · ")
        .ifBlank { "YouTube Music" }
}

private fun isChartDrivenSource(source: String): Boolean {
    val normalized = source.lowercase()
    return normalized.contains("chart") ||
        normalized.contains("classifica") ||
        normalized.contains("trend") ||
        normalized.contains("top")
}

@Composable
private fun HomeDiscoveryHero(
    update: HomeHeroUpdate,
    isFavorite: Boolean,
    isDownloading: Boolean,
    isDownloaded: Boolean,
    downloadProgress: Int?,
    onPlay: () -> Unit,
    onSave: () -> Unit
) {
    val track = update.track
    val accentStart = Color(track.accentStart)
    val accentEnd = Color(track.accentEnd)
    val copy = remember(track.id, track.title, track.artist, track.album, update.sourceTitle, update.verifiedRelease) {
        buildHomeUpdateCopy(update)
    }
    Surface(
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            accentStart.copy(alpha = 0.30f),
                            Color(0xFF07111F),
                            accentEnd.copy(alpha = 0.26f)
                        )
                    )
                )
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(13.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Icon(
                                    imageVector = copy.icon,
                                    contentDescription = null,
                                    tint = LevyraCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = copy.badge,
                                    color = LevyraText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Text(
                            text = copy.headline,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 19.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(brush = Brush.horizontalGradient(listOf(LevyraCyan, LevyraViolet)))
                        )
                        Text(
                            text = copy.detail,
                            color = LevyraText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 17.sp
                        )
                        Text(
                            text = copy.caption,
                            color = LevyraMuted,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = copy.sourceLabel,
                            color = LevyraCyan.copy(alpha = 0.88f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(contentAlignment = Alignment.BottomEnd) {
                        CoverImage(
                            track = track,
                            modifier = Modifier
                                .size(86.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            highRes = true
                        )
                        Surface(
                            color = Color.Black.copy(alpha = 0.42f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier
                                .padding(6.dp)
                                .size(30.dp)
                                .pressable(onClick = onPlay)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = LevyraText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .pressable(onClick = onPlay)
                    ) {
                        Box(
                            modifier = Modifier.background(Brush.horizontalGradient(listOf(LevyraCyan, LevyraViolet))),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = copy.primaryAction,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.045f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
                        modifier = Modifier
                            .weight(0.68f)
                            .height(40.dp)
                            .pressable(onClick = onSave)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (isFavorite) LevyraPink else LevyraText,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isFavorite) "Salvato" else "Salva",
                                    color = LevyraText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueListeningCard(
    track: Track,
    isPlaying: Boolean,
    isResolving: Boolean,
    progress: Float,
    onResume: () -> Unit
) {
    val accentStart = Color(track.accentStart)
    val accentEnd = Color(track.accentEnd)
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(Dp.Hairline, LevyraAdaptiveHairline),
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .pressable(onClick = onResume)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(cinematicGlassBrush(accentStart, accentEnd, 0.24f))
        ) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 62.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(accentEnd.copy(alpha = 0.16f), Color.Transparent)
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CoverImage(
                    track = track,
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(Dp.Hairline, Color.White.copy(alpha = 0.13f), RoundedCornerShape(14.dp)),
                    highRes = false
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Headphones,
                            contentDescription = null,
                            tint = LevyraCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Continua ad ascoltare",
                            color = LevyraCyan,
                            fontSize = 10.8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = track.title,
                        color = LevyraText,
                        fontSize = 15.5.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        color = LevyraMuted,
                        fontSize = 11.8.sp,
                        lineHeight = 14.sp,
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
                        when {
                            isResolving -> CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 1.8.dp,
                                color = LevyraCyan
                            )
                            isPlaying -> ActiveTrackEqualizer(
                                color = LevyraCyan,
                                isPlaying = true,
                                width = 15.dp,
                                height = 11.dp
                            )
                            else -> Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = LevyraText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(3.dp)
                    .background(Brush.horizontalGradient(listOf(accentStart, accentEnd)))
            )
        }
    }
}

@Composable
private fun HomeShortcutRow(
    hasTracks: Boolean,
    onShuffle: () -> Unit,
    onFavorites: () -> Unit,
    onNewReleases: () -> Unit,
    onGenres: () -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            QuickAction(
                icon = Icons.Rounded.Shuffle,
                label = "Mix per te",
                accent = LevyraCyan,
                enabled = hasTracks,
                modifier = Modifier.width(176.dp),
                onClick = onShuffle
            )
        }
        item {
            QuickAction(
                icon = Icons.Rounded.Favorite,
                label = "Preferiti",
                accent = LevyraPink,
                enabled = true,
                modifier = Modifier.width(176.dp),
                onClick = onFavorites
            )
        }
        item {
            QuickAction(
                icon = Icons.Rounded.Bolt,
                label = "Nuove uscite",
                accent = LevyraViolet,
                enabled = true,
                modifier = Modifier.width(176.dp),
                onClick = onNewReleases
            )
        }
        item {
            QuickAction(
                icon = Icons.Rounded.MusicNote,
                label = "Generi",
                accent = Color(0xFFB7C7FF),
                enabled = true,
                modifier = Modifier.width(176.dp),
                onClick = onGenres
            )
        }
    }
}

@Composable
private fun QuickSectionHeader(title: String, actionLabel: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            color = LevyraText,
            fontSize = 23.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.weight(1f)
        )
        Surface(
            color = Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            modifier = Modifier.pressable(onClick = onAction)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = LevyraCyan,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = actionLabel,
                    color = LevyraText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun QuickSongList(
    tracks: List<Track>,
    currentId: String?,
    favoriteIds: Set<String>,
    downloadingIds: Set<String>,
    downloadedIds: Set<String>,
    downloadProgressByTrackId: Map<String, Int>,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: (Track) -> Unit,
    onFavorite: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onOpenPlayer: (Track) -> Unit,
    onOffline: (Track) -> Unit,
    onArtist: (Track) -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        tracks.take(4).forEach { track ->
            QuickSongRow(
                track = track,
                isCurrent = track.id == currentId,
                isPlaying = isPlaying && track.id == currentId,
                isResolving = isResolving && track.id == currentId,
                isFavorite = track.id in favoriteIds,
                isDownloading = track.id in downloadingIds,
                isDownloaded = track.id in downloadedIds,
                downloadProgress = downloadProgressByTrackId[track.id],
                onPlay = { onPlay(track) },
                onFavorite = { onFavorite(track) },
                onAddToQueue = { onAddToQueue(track) },
                onOpenPlayer = { onOpenPlayer(track) },
                onOffline = { onOffline(track) },
                onArtist = { onArtist(track) }
            )
        }
    }
}

@Composable
private fun QuickSongRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    isFavorite: Boolean,
    isDownloading: Boolean,
    isDownloaded: Boolean,
    downloadProgress: Int?,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onAddToQueue: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOffline: () -> Unit,
    onArtist: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .pressable(onClick = onPlay)
            .padding(horizontal = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(58.dp),
            contentAlignment = Alignment.Center
        ) {
            CoverImage(
                track = track,
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            if (isCurrent) {
                Surface(
                    color = Color.Black.copy(alpha = 0.46f),
                    shape = CircleShape,
                    modifier = Modifier.size(25.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        when {
                            isResolving -> CircularProgressIndicator(
                                modifier = Modifier.size(13.dp),
                                strokeWidth = 2.dp,
                                color = LevyraCyan
                            )
                            isPlaying -> Icon(
                                imageVector = Icons.Rounded.Equalizer,
                                contentDescription = null,
                                tint = LevyraCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            else -> Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = track.title,
                color = if (isCurrent) LevyraCyan else LevyraText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = track.artist,
                color = LevyraMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onArtist() }
            )
        }
        DownloadButton(isDownloading = isDownloading, isDownloaded = isDownloaded, progress = downloadProgress, onDownload = onOffline)
        IconButton(onClick = onFavorite, modifier = Modifier.size(38.dp)) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "Preferito",
                tint = if (isFavorite) LevyraPink else LevyraMuted,
                modifier = Modifier.size(23.dp)
            )
        }
        TrackOverflowMenu(
            track = track,
            iconSize = 21.dp,
            buttonSize = 38.dp,
            onAddToQueue = onAddToQueue,
            onOpenPlayer = onOpenPlayer,
            onOffline = onOffline,
            onArtist = onArtist
        )
    }
}

@Composable
private fun TrackOverflowMenu(
    track: Track,
    iconSize: androidx.compose.ui.unit.Dp = 23.dp,
    buttonSize: androidx.compose.ui.unit.Dp = 48.dp,
    onAddToQueue: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOffline: () -> Unit,
    onArtist: () -> Unit = {}
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(buttonSize)
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Azioni",
                tint = LevyraMuted,
                modifier = Modifier.size(iconSize)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Aggiungi alla coda") },
                leadingIcon = { Icon(Icons.Rounded.QueueMusic, null) },
                onClick = {
                    expanded = false
                    onAddToQueue()
                }
            )
            DropdownMenuItem(
                text = { Text("Condividi") },
                leadingIcon = { Icon(Icons.Rounded.Share, null) },
                onClick = {
                    expanded = false
                    val shareText = buildString {
                        append(track.title)
                        if (track.artist.isNotBlank()) append(" - ").append(track.artist)
                        val link = track.videoUrl.ifBlank { track.streamUrl }
                        if (link.isNotBlank()) append("\n").append(link)
                    }
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, "Condividi brano"))
                }
            )
            DropdownMenuItem(
                text = { Text("Vai al player") },
                leadingIcon = { Icon(Icons.Rounded.PlayArrow, null) },
                onClick = {
                    expanded = false
                    onOpenPlayer()
                }
            )
            DropdownMenuItem(
                text = { Text("Apri artista") },
                leadingIcon = { Icon(Icons.Rounded.Person, null) },
                onClick = {
                    expanded = false
                    onArtist()
                }
            )
            DropdownMenuItem(
                text = { Text("Salva offline") },
                leadingIcon = { Icon(Icons.Rounded.LibraryMusic, null) },
                onClick = {
                    expanded = false
                    onOffline()
                }
            )
        }
    }
}

@Composable
private fun ChartRegionRow(regions: List<com.luc4n3x.levyra.domain.ChartRegion>, selectedId: String, loading: Boolean, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = LevyraCyan)
        }
        regions.forEach { region ->
            val selected = region.id == selectedId
            Surface(
                color = if (selected) LevyraCyan.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.07f),
                border = BorderStroke(1.dp, if (selected) LevyraCyan else Color.White.copy(alpha = 0.1f)),
                shape = CircleShape,
                modifier = Modifier.pressable(onClick = { onSelect(region.id) })
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(region.emoji, fontSize = 14.sp)
                    Text(region.label, color = if (selected) LevyraCyan else LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun SearchScreen(viewModel: LevyraViewModel, state: LevyraUiState) {
    val strings = LocalLevyraStrings.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var addTarget by remember { mutableStateOf<Track?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SearchHeader(
            query = state.query,
            isSearching = state.isSearching,
            onQuery = viewModel::setQuery,
            onSearch = { query ->
                focusManager.clearFocus()
                keyboardController?.hide()
                viewModel.searchNow(query)
            },
            onClear = {
                viewModel.setQuery("")
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (state.currentTrack != null) 188.dp else 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val queryClean = state.query.trim()

            if (queryClean.isEmpty()) {
                if (state.recentSearches.isNotEmpty()) {
                    item {
                        RecentSearchesRow(
                            tracks = state.recentSearches,
                            favoriteIds = state.favoriteIds,
                            downloadedTrackIds = state.downloadedTrackIds,
                            onTrackClick = { track ->
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                viewModel.play(track)
                            },
                            onRemove = viewModel::removeRecentSearch,
                            onFavorite = viewModel::toggleFavorite,
                            onAddToPlaylist = { addTarget = it },
                            onPlayNext = viewModel::playNext,
                            onAddToQueue = viewModel::addToQueue,
                            onDownload = viewModel::exportTrack,
                            onArtist = viewModel::openArtist
                        )
                    }
                }

                item {
                    val fallbackSuggestions = LevyraContentLocales.artistSuggestions(state.languageCode)
                    SuggestionsList(
                        title = LevyraContentLocales.artistSuggestionsTitle(state.languageCode),
                        suggestions = fallbackSuggestions,
                        onSuggestionClick = { query ->
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.setQuery(query)
                            viewModel.searchNow(query)
                        }
                    )
                }
            } else if (state.searchSuggestions.isNotEmpty() && !state.isSearching && state.searchResults.isEmpty() && state.searchError == null) {
                item {
                    SuggestionsList(
                        title = LevyraContentLocales.searchSuggestionsTitle(state.languageCode),
                        suggestions = state.searchSuggestions,
                        onSuggestionClick = { suggestion ->
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.setQuery(suggestion)
                            viewModel.searchNow(suggestion)
                        }
                    )
                }
            } else if (state.searchResults.isEmpty() && !state.isSearching && state.searchError == null) {
                item {
                    QuickChips(
                        languageCode = state.languageCode,
                        onClick = { query ->
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            viewModel.setQuery(query)
                            viewModel.searchNow(query)
                        }
                    )
                }
            }

            if (queryClean.isNotEmpty()) {
                when {
                    state.isSearching -> item {
                        SearchLoadingSkeleton()
                    }
                    state.searchError != null -> item { GlassMessage(state.searchError, LevyraOrange) }
                    !state.searchData.isEmpty -> {
                        val data = state.searchData
                        val filter = state.searchFilter
                        item {
                            SearchFilterChips(
                                selected = filter,
                                hasArtists = data.artists.isNotEmpty(),
                                hasAlbums = data.albums.isNotEmpty(),
                                onSelect = viewModel::setSearchFilter
                            )
                        }
                        if (filter == SearchFilter.All && data.topTrack != null) {
                            item {
                                TopResultCard(
                                    track = data.topTrack,
                                    isCurrent = data.topTrack.id == state.currentTrack?.id,
                                    isPlaying = state.isPlaying && data.topTrack.id == state.currentTrack?.id,
                                    isResolving = state.isResolving && data.topTrack.id == state.currentTrack?.id,
                                    isFavorite = data.topTrack.id in state.favoriteIds,
                                    onPlay = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        viewModel.playFrom(data.songs, data.topTrack)
                                    },
                                    onFavorite = { viewModel.toggleFavorite(data.topTrack) },
                                    onAddToPlaylist = { addTarget = data.topTrack },
                                    onArtist = { viewModel.openArtist(data.topTrack) }
                                )
                            }
                        }
                        if ((filter == SearchFilter.All || filter == SearchFilter.Artists) && data.artists.isNotEmpty()) {
                            item { SectionTitle(strings.artists) }
                            item {
                                ArtistHitRow(
                                    artists = data.artists,
                                    onClick = { hit ->
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        viewModel.openArtistFromHit(hit)
                                    }
                                )
                            }
                        }
                        if ((filter == SearchFilter.All || filter == SearchFilter.Albums) && data.albums.isNotEmpty()) {
                            item { SectionTitle(strings.albumsAndSingles) }
                            item {
                                AlbumHitRow(
                                    albums = data.albums,
                                    onClick = { album ->
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        viewModel.openAlbum(album)
                                    }
                                )
                            }
                        }
                        if (filter == SearchFilter.All || filter == SearchFilter.Songs) {
                            val songs = if (filter == SearchFilter.All) data.songs.drop(if (data.topTrack != null) 1 else 0) else data.songs
                            if (songs.isNotEmpty()) {
                                item { SectionTitle(strings.songs) }
                                items(songs, key = { "search-song-${it.id}" }) { track ->
                                    SearchTrackCard(
                                        track = track,
                                        isCurrent = track.id == state.currentTrack?.id,
                                        isPlaying = state.isPlaying && track.id == state.currentTrack?.id,
                                        isResolving = state.isResolving && track.id == state.currentTrack?.id,
                                        isFavorite = track.id in state.favoriteIds,
                                        isDownloading = track.id in state.downloadingTrackIds,
                                        isDownloaded = track.id in state.downloadedTrackIds,
                                        downloadProgress = state.downloadProgressByTrackId[track.id],
                                        onClick = {
                                            focusManager.clearFocus()
                                            keyboardController?.hide()
                                            viewModel.playFrom(data.songs, track)
                                        },
                                        onFavorite = { viewModel.toggleFavorite(track) },
                                        onAddToPlaylist = { addTarget = track },
                                        onDownload = { viewModel.exportTrack(track) },
                                        onArtist = { viewModel.openArtist(track) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    addTarget?.let { track ->
        AddToPlaylistDialog(
            track = track,
            playlists = state.playlists,
            onDismiss = { addTarget = null },
            onAddTo = { playlistId ->
                viewModel.addToPlaylist(playlistId, track)
                addTarget = null
            },
            onCreateWith = { name ->
                viewModel.createPlaylist(name, track)
                addTarget = null
            }
        )
    }
}

@Composable
private fun SearchHeader(
    query: String,
    isSearching: Boolean,
    onQuery: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClear: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val spokenText = matches[0]
                onQuery(spokenText)
                onSearch(spokenText)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = {
                onClear()
                focusManager.clearFocus()
                keyboardController?.hide()
            },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = LocalLevyraStrings.current.back,
                tint = LevyraText
            )
        }

        Surface(
            color = CinematicGlass.copy(alpha = 0.78f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            shape = CircleShape,
            shadowElevation = 10.dp,
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = onQuery,
                    modifier = Modifier.weight(1f).padding(vertical = 10.dp),
                    singleLine = true,
                    textStyle = TextStyle(color = LevyraText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                    cursorBrush = SolidColor(LevyraCyan),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(LocalLevyraStrings.current.searchPlaceholder, color = LevyraMuted, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        }
                        innerTextField()
                    }
                )

                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = LocalLevyraStrings.current.clear,
                            tint = LevyraMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        IconButton(
            onClick = { 
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Ascoltando...")
                }
                try {
                    speechRecognizerLauncher.launch(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Ricerca vocale non supportata", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.05f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Rounded.Mic,
                contentDescription = LocalLevyraStrings.current.voice,
                tint = LevyraText,
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(
            onClick = { Toast.makeText(context, "Filtri musicali in arrivo!", Toast.LENGTH_SHORT).show() },
            modifier = Modifier
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.05f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Rounded.Equalizer,
                contentDescription = "Soundwave",
                tint = LevyraText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun RecentSearchesRow(
    tracks: List<Track>,
    favoriteIds: Set<String>,
    downloadedTrackIds: Set<String>,
    onTrackClick: (Track) -> Unit,
    onRemove: (Track) -> Unit,
    onFavorite: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onPlayNext: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onDownload: (Track) -> Unit,
    onArtist: (Track) -> Unit
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Ricerche recenti",
            color = LevyraText,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(tracks, key = { "recent-${it.id}" }) { track ->
                var menuExpanded by remember(track.id) { mutableStateOf(false) }
                val isFavorite = track.id in favoriteIds
                val isDownloaded = track.id in downloadedTrackIds

                Column(
                    modifier = Modifier
                        .width(140.dp)
                        .clickable { onTrackClick(track) },
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.5f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                    ) {
                        CoverImage(
                            track = track,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Box(modifier = Modifier.align(Alignment.TopEnd)) {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(32.dp)
                                    .background(Color.Black.copy(alpha = 0.58f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "Azioni",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (isFavorite) "Rimuovi dai preferiti" else "Aggiungi ai preferiti") },
                                    leadingIcon = { Icon(if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null) },
                                    onClick = {
                                        menuExpanded = false
                                        onFavorite(track)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Riproduci dopo") },
                                    leadingIcon = { Icon(Icons.Rounded.PlaylistPlay, null) },
                                    onClick = {
                                        menuExpanded = false
                                        onPlayNext(track)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Aggiungi alla coda") },
                                    leadingIcon = { Icon(Icons.Rounded.QueueMusic, null) },
                                    onClick = {
                                        menuExpanded = false
                                        onAddToQueue(track)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Aggiungi a playlist") },
                                    leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, null) },
                                    onClick = {
                                        menuExpanded = false
                                        onAddToPlaylist(track)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isDownloaded) "Già offline" else "Scarica") },
                                    leadingIcon = { Icon(if (isDownloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download, null) },
                                    onClick = {
                                        menuExpanded = false
                                        if (!isDownloaded) onDownload(track)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Apri artista") },
                                    leadingIcon = { Icon(Icons.Rounded.Person, null) },
                                    onClick = {
                                        menuExpanded = false
                                        onArtist(track)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Condividi") },
                                    leadingIcon = { Icon(Icons.Rounded.Share, null) },
                                    onClick = {
                                        menuExpanded = false
                                        val shareText = buildString {
                                            append(track.title)
                                            if (track.artist.isNotBlank()) append(" - ").append(track.artist)
                                            val link = track.videoUrl.ifBlank { track.streamUrl }
                                            if (link.isNotBlank()) append("\n").append(link)
                                        }
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Condividi brano"))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Rimuovi dalle recenti") },
                                    leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                                    onClick = {
                                        menuExpanded = false
                                        onRemove(track)
                                    }
                                )
                            }
                        }
                    }
                    Text(
                        text = track.title,
                        color = LevyraText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        color = LevyraMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionsList(
    title: String,
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (title.isNotEmpty()) {
            Text(
                text = title,
                color = LevyraText,
                fontSize = 24.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.018f),
                            LevyraCyan.copy(alpha = 0.018f),
                            Color.Transparent
                        )
                    ),
                    RoundedCornerShape(24.dp)
                )
                .padding(vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            suggestions.forEachIndexed { index, suggestion ->
                val accent = if (index % 2 == 0) LevyraCyan else LevyraViolet
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onSuggestionClick(suggestion) }
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.012f), RoundedCornerShape(18.dp))
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            LevyraCyan.copy(alpha = 0.18f),
                                            LevyraViolet.copy(alpha = 0.14f)
                                        )
                                    ),
                                    CircleShape
                                )
                                .border(1.dp, accent.copy(alpha = 0.34f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = suggestion,
                            color = LevyraText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Completa",
                            tint = accent.copy(alpha = 0.78f),
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = 45f }
                        )
                    }
                }
                if (index != suggestions.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 64.dp, end = 14.dp)
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.06f),
                                        Color.White.copy(alpha = 0.018f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadsFolderCard(count: Int, onClick: () -> Unit) {
    Surface(
        color = Color.White.copy(alpha = 0.045f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.DownloadDone,
                    contentDescription = null,
                    tint = LevyraCyan,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Cartella download",
                    color = LevyraText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (count == 1) "1 brano scaricato" else "$count brani scaricati",
                    color = LevyraMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            }
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = LevyraMuted,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun DownloadsFolderOverlay(
    state: LevyraUiState,
    viewModel: LevyraViewModel,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LevyraInk)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = if (state.currentTrack != null) 194.dp else 108.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Indietro", tint = LevyraText)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Download offline",
                            color = LevyraText,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = if (state.downloads.size == 1) "1 brano salvato" else "${state.downloads.size} brani salvati",
                            color = LevyraMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            if (state.downloads.isEmpty()) {
                item {
                    EmptyState("Nessun download salvato offline.")
                }
            } else {
                items(state.downloads, key = { "dlfolder-${it.id}" }) { download ->
                    DownloadRow(
                        download = download,
                        isCurrent = download.trackId == state.currentTrack?.id,
                        onPlay = { viewModel.playDownloaded(download) },
                        onDelete = { viewModel.deleteDownload(download) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    viewModel: LevyraViewModel,
    state: LevyraUiState,
    onOpenDownloads: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    var addTarget by remember { mutableStateOf<Track?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = if (state.currentTrack != null) 194.dp else 108.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                LibraryHeader(
                    title = strings.libraryTitle,
                    subtitle = strings.librarySubtitle,
                    playlistCount = state.playlists.size,
                    downloadCount = state.downloads.size,
                    favoriteCount = state.favorites.size
                )
            }

            item {
                LibrarySectionHeader(
                    title = cleanLibraryLabel(strings.playlists),
                    detail = "Playlist personali",
                    count = state.playlists.size,
                    icon = Icons.Rounded.QueueMusic,
                    accent = LevyraViolet,
                    actionLabel = strings.newItem,
                    onAction = { showCreate = true }
                )
            }
            if (state.playlists.isEmpty()) {
                item {
                    LibraryEmptyState(
                        icon = Icons.Rounded.QueueMusic,
                        title = "Crea la tua prima playlist",
                        detail = "Raccogli i brani che vuoi ritrovare subito.",
                        accent = LevyraViolet,
                        actionLabel = strings.newItem,
                        onAction = { showCreate = true }
                    )
                }
            } else {
                items(state.playlists, key = { "pl-${it.id}" }) { playlist ->
                    PlaylistRow(
                        playlist = playlist,
                        onOpen = { viewModel.openPlaylist(playlist.id) },
                        onPlay = { viewModel.playPlaylist(playlist.id) },
                        onDelete = { viewModel.deletePlaylist(playlist.id) }
                    )
                }
            }

            item {
                LibrarySectionHeader(
                    title = cleanLibraryLabel(strings.downloads),
                    detail = "Disponibili senza rete",
                    count = state.downloads.size,
                    icon = Icons.Rounded.DownloadDone,
                    accent = LevyraCyan
                )
            }
            if (state.downloads.isEmpty()) {
                item {
                    LibraryEmptyState(
                        icon = Icons.Rounded.Download,
                        title = "Nessun download offline",
                        detail = "Tocca download su un brano per salvarlo in Music/Levyra.",
                        accent = LevyraCyan
                    )
                }
            } else {
                item {
                    DownloadsFolderCard(
                        count = state.downloads.size,
                        onClick = onOpenDownloads
                    )
                }
            }

            item {
                LibrarySectionHeader(
                    title = cleanLibraryLabel(strings.favorites),
                    detail = "Brani salvati",
                    count = state.favorites.size,
                    icon = Icons.Rounded.Favorite,
                    accent = LevyraPink
                )
            }
            if (state.favorites.isEmpty()) {
                item {
                    LibraryEmptyState(
                        icon = Icons.Rounded.FavoriteBorder,
                        title = "Preferiti ancora vuoti",
                        detail = "Tocca il cuore su un brano per metterlo qui.",
                        accent = LevyraPink
                    )
                }
            } else {
                item {
                    RowCarousel(
                        tracks = state.favorites,
                        currentId = state.currentTrack?.id,
                        isPlaying = state.isPlaying,
                        isResolving = state.isResolving,
                        favoriteIds = state.favoriteIds,
                        onPlay = { viewModel.playFrom(state.favorites, it) },
                        onFavorite = { viewModel.toggleFavorite(it) },
                        onAddToPlaylist = { addTarget = it },
                        onAddToQueue = { viewModel.addToQueue(it) },
                        onDownload = { viewModel.exportTrack(it) }
                    )
                }
            }

            if (state.followedArtists.isNotEmpty()) {
                item {
                    LibrarySectionHeader(
                        title = strings.followedArtistsTitle,
                        detail = strings.followedArtistsSubtitle,
                        count = state.followedArtists.size,
                        icon = Icons.Rounded.Person,
                        accent = CinematicGold
                    )
                }
                item {
                    FollowedArtistsRow(
                        artists = state.followedArtists,
                        onOpen = { viewModel.openArtistByName(it.name) }
                    )
                }
            }

            item {
                LibrarySectionDivider(label = strings.pulseSectionBand)
            }

            item {
                LibrarySectionHeader(
                    title = strings.pulseTitle,
                    detail = strings.pulseSubtitle,
                    count = state.listeningPulse.plays,
                    icon = Icons.Rounded.Insights,
                    accent = LevyraCyan
                )
            }
            item {
                ListeningPulseCard(pulse = state.listeningPulse, strings = strings)
            }

            item {
                LibrarySectionHeader(
                    title = strings.listeningHistory,
                    detail = strings.listeningHistorySubtitle,
                    count = state.recentListens.size,
                    icon = Icons.Rounded.History,
                    accent = LevyraViolet
                )
            }
            if (state.recentListens.isEmpty()) {
                item {
                    LibraryEmptyState(
                        icon = Icons.Rounded.History,
                        title = strings.listeningHistoryEmptyTitle,
                        detail = strings.listeningHistoryEmptyDetail,
                        accent = LevyraViolet
                    )
                }
            } else {
                items(state.recentListens, key = { "hist-${it.id}" }) { track ->
                    TrackRow(
                        track = track,
                        isCurrent = track.id == state.currentTrack?.id,
                        isPlaying = state.isPlaying && track.id == state.currentTrack?.id,
                        isResolving = state.isResolving && track.id == state.currentTrack?.id,
                        isFavorite = track.id in state.favoriteIds,
                        onClick = { viewModel.playFrom(state.recentListens, track) },
                        onFavorite = { viewModel.toggleFavorite(track) },
                        isDownloading = track.id in state.downloadingTrackIds,
                        isDownloaded = track.id in state.downloadedTrackIds,
                        downloadProgress = state.downloadProgressByTrackId[track.id],
                        onDownload = { viewModel.exportTrack(track) },
                        onArtist = { viewModel.openArtist(track) },
                        onAddToPlaylist = { addTarget = track }
                    )
                }
            }
        }

        if (showCreate) {
            PlaylistCreateDialog(
                onDismiss = { showCreate = false },
                onConfirm = { name ->
                    viewModel.createPlaylist(name)
                    showCreate = false
                }
            )
        }

        addTarget?.let { track ->
            AddToPlaylistDialog(
                track = track,
                playlists = state.playlists,
                onDismiss = { addTarget = null },
                onAddTo = { playlistId ->
                    viewModel.addToPlaylist(playlistId, track)
                    addTarget = null
                },
                onCreateWith = { name ->
                    viewModel.createPlaylist(name, track)
                    addTarget = null
                }
            )
        }
    }
}

@Composable
private fun LibraryHeader(
    title: String,
    subtitle: String,
    playlistCount: Int,
    downloadCount: Int,
    favoriteCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    color = LevyraText,
                    fontSize = 35.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.7).sp
                )
                Text(
                    text = subtitle,
                    color = LevyraMuted.copy(alpha = 0.84f),
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(LevyraCyan.copy(alpha = 0.24f), LevyraViolet.copy(alpha = 0.20f))))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.LibraryMusic, null, tint = LevyraCyan, modifier = Modifier.size(25.dp))
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LibraryStatPill(Icons.Rounded.QueueMusic, playlistCount.toString(), "playlist", LevyraViolet)
            LibraryStatPill(Icons.Rounded.DownloadDone, downloadCount.toString(), "offline", LevyraCyan)
            LibraryStatPill(Icons.Rounded.Favorite, favoriteCount.toString(), "preferiti", LevyraPink)
        }
    }
}

@Composable
private fun LibraryStatPill(icon: ImageVector, value: String, label: String, accent: Color) {
    Surface(
        color = CinematicGlass.copy(alpha = 0.54f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.085f)),
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
            Text(value, color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Black)
            Text(label, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ListeningPulseCard(pulse: ListeningPulse, strings: LevyraStrings) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CinematicGlass.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!pulse.hasSignal) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Rounded.Insights, null, tint = LevyraCyan, modifier = Modifier.size(22.dp))
                    Text(
                        text = strings.pulseEmpty,
                        color = LevyraMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                return@Column
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PulseStat(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Headphones,
                    value = pulse.totalMinutes.toString(),
                    label = strings.pulseMinutes,
                    accent = LevyraCyan
                )
                PulseStat(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.PlayArrow,
                    value = pulse.plays.toString(),
                    label = strings.pulsePlays,
                    accent = LevyraViolet
                )
                PulseStat(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.LocalFireDepartment,
                    value = pulse.streakDays.toString(),
                    label = strings.pulseStreak,
                    accent = LevyraOrange
                )
                PulseStat(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.TaskAlt,
                    value = "${pulse.completionRate}%",
                    label = strings.pulseCompletion,
                    accent = LevyraPink
                )
            }
            PulseWeekChart(pulse = pulse, label = strings.pulseWeek, languageCode = strings.code)
            if (pulse.topArtists.isNotEmpty()) {
                PulseArtistsRow(artists = pulse.topArtists, label = strings.pulseTopArtists, minuteShort = strings.pulseMinuteShort)
            }
            if (pulse.peakHour >= 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Schedule, null, tint = LevyraMuted, modifier = Modifier.size(15.dp))
                    Text(
                        text = "${strings.pulsePeakHour} · ${pulse.peakHour.toString().padStart(2, '0')}:00",
                        color = LevyraMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PulseStat(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    accent: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(accent.copy(alpha = 0.10f))
            .border(1.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(17.dp))
        Text(value, color = LevyraText, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Text(
            text = label,
            color = LevyraMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PulseWeekChart(pulse: ListeningPulse, label: String, languageCode: String) {
    val peak = pulse.weekPeakMs
    val locale = remember(languageCode) { Locale.forLanguageTag(languageCode) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            pulse.week.forEach { day ->
                val fraction = if (peak > 0L) (day.listenedMs.toFloat() / peak.toFloat()).coerceIn(0.06f, 1f) else 0.06f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(fraction)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    LevyraCyan.copy(alpha = if (day.listenedMs > 0L) 0.9f else 0.16f),
                                    LevyraViolet.copy(alpha = if (day.listenedMs > 0L) 0.75f else 0.12f)
                                )
                            )
                        )
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            pulse.week.forEach { day ->
                val dayLabel = day.date.dayOfWeek
                    .getDisplayName(DayTextStyle.SHORT_STANDALONE, locale)
                    .replace(".", "")
                    .take(3)
                Text(
                    text = dayLabel,
                    color = LevyraMuted.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PulseArtistsRow(artists: List<PulseArtist>, label: String, minuteShort: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            artists.forEach { artist ->
                Surface(
                    color = LevyraViolet.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, LevyraViolet.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = artist.name,
                            color = LevyraText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "${artist.listenedMs / 60_000L} $minuteShort",
                            color = LevyraMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowedArtistsRow(artists: List<FollowedArtist>, onOpen: (FollowedArtist) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(artists, key = { "followed-${it.key}" }) { artist ->
            Column(
                modifier = Modifier.width(96.dp).clickable { onOpen(artist) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(CinematicGold.copy(alpha = 0.3f), LevyraViolet.copy(alpha = 0.28f))))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (artist.thumbnailUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(artist.thumbnailUrl).crossfade(true).build(),
                            contentDescription = artist.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize().clip(CircleShape)
                        )
                    } else {
                        Icon(Icons.Rounded.Person, null, tint = LevyraText, modifier = Modifier.size(38.dp))
                    }
                }
                Text(
                    text = artist.name,
                    color = LevyraText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun LibrarySectionDivider(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, LevyraCyan.copy(alpha = 0.35f))
                    )
                )
        )
        Text(
            text = label.uppercase(),
            color = LevyraMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.6.sp
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(LevyraViolet.copy(alpha = 0.35f), Color.Transparent)
                    )
                )
        )
    }
}

@Composable
private fun LibrarySectionHeader(
    title: String,
    detail: String,
    count: Int,
    icon: ImageVector,
    accent: Color,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.16f))
                .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, color = LevyraText, fontSize = 22.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Surface(color = Color.White.copy(alpha = 0.07f), shape = RoundedCornerShape(999.dp)) {
                    Text(count.toString(), color = LevyraMuted, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                }
            }
            Text(detail, color = LevyraMuted.copy(alpha = 0.72f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (actionLabel != null && onAction != null) {
            Surface(
                color = accent.copy(alpha = 0.13f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.pressable(onClick = onAction)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(Icons.Rounded.Add, null, tint = accent, modifier = Modifier.size(16.dp))
                    Text(cleanLibraryLabel(actionLabel), color = LevyraText, fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun LibraryEmptyState(
    icon: ImageVector,
    title: String,
    detail: String,
    accent: Color,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Surface(
        color = CinematicGlass.copy(alpha = 0.50f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(detail, color = LevyraMuted.copy(alpha = 0.76f), fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (actionLabel != null && onAction != null) {
                Surface(
                    color = accent.copy(alpha = 0.14f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.24f)),
                    modifier = Modifier
                        .size(42.dp)
                        .pressable(onClick = onAction)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Add, null, tint = accent, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

private fun cleanLibraryLabel(value: String): String {
    return value.dropWhile { !it.isLetterOrDigit() }.trim()
}

@Composable
private fun DownloadRow(download: DownloadedTrack, isCurrent: Boolean, onPlay: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cinematicGlassBrush(intensity = if (isCurrent) 0.82f else 0.48f))
            .border(
                1.dp,
                if (isCurrent) LevyraCyan.copy(alpha = 0.34f) else Color.White.copy(alpha = 0.07f),
                RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onPlay)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(listOf(LevyraCyan.copy(alpha = 0.32f), LevyraViolet.copy(alpha = 0.32f)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.DownloadDone, null, tint = LevyraCyan, modifier = Modifier.size(24.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(download.title, color = if (isCurrent) LevyraCyan else LevyraText, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(download.artist, color = LevyraMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (download.embeddedMetadata) "Music/Levyra · cover e tag" else "Music/Levyra",
                color = LevyraMuted.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.Delete, contentDescription = "Rimuovi", tint = LevyraMuted, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun PlaylistRow(
    playlist: com.luc4n3x.levyra.domain.Playlist,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cinematicGlassBrush(intensity = 0.50f))
            .border(1.dp, Color.White.copy(alpha = 0.075f), RoundedCornerShape(18.dp))
            .clickable { onOpen() }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(LevyraCyan.copy(alpha = 0.32f), LevyraViolet.copy(alpha = 0.32f)))),
            contentAlignment = Alignment.Center
        ) {
            if (playlist.coverUrl.isNotBlank()) {
                AsyncImage(
                    model = playlist.coverUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Rounded.QueueMusic, null, tint = LevyraCyan, modifier = Modifier.size(26.dp))
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(playlist.name, color = LevyraText, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (playlist.size == 1) "1 brano" else "${playlist.size} brani",
                color = LevyraMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium
            )
        }
        IconButton(onClick = onPlay) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = "Riproduci", tint = LevyraCyan, modifier = Modifier.size(26.dp))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.Delete, contentDescription = "Elimina", tint = LevyraMuted, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun PlaylistCreateDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LevyraPanel,
        title = { Text("Nuova playlist", color = LevyraText, fontWeight = FontWeight.Black) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text("Nome della playlist", color = LevyraMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = LevyraText,
                    unfocusedTextColor = LevyraText,
                    focusedBorderColor = LevyraCyan,
                    unfocusedBorderColor = LevyraMuted.copy(alpha = 0.4f),
                    cursorColor = LevyraCyan
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name) }) {
                Text("Crea", color = LevyraCyan, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla", color = LevyraMuted) }
        }
    )
}

@Composable
private fun AddToPlaylistDialog(
    track: Track,
    playlists: List<com.luc4n3x.levyra.domain.Playlist>,
    onDismiss: () -> Unit,
    onAddTo: (String) -> Unit,
    onCreateWith: (String) -> Unit
) {
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LevyraPanel,
        title = { Text("Aggiungi a playlist", color = LevyraText, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (creating) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        placeholder = { Text("Nome nuova playlist", color = LevyraMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = LevyraText,
                            unfocusedTextColor = LevyraText,
                            focusedBorderColor = LevyraCyan,
                            unfocusedBorderColor = LevyraMuted.copy(alpha = 0.4f),
                            cursorColor = LevyraCyan
                        )
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { creating = true }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.Add, null, tint = LevyraCyan)
                        Text("Crea nuova playlist", color = LevyraCyan, fontWeight = FontWeight.Bold)
                    }
                    playlists.forEach { pl ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onAddTo(pl.id) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.QueueMusic, null, tint = LevyraMuted)
                            Text(pl.name, color = LevyraText, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (creating) {
                TextButton(onClick = { if (name.isNotBlank()) onCreateWith(name) }) {
                    Text("Crea e aggiungi", color = LevyraCyan, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Chiudi", color = LevyraMuted) }
        }
    )
}

@Composable
private fun PlaylistDetailOverlay(viewModel: LevyraViewModel, state: LevyraUiState) {
    val playlist = state.openPlaylist ?: return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LevyraInk)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = if (state.currentTrack != null) 220.dp else 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.closePlaylist() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = LocalLevyraStrings.current.back, tint = LevyraText)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(playlist.name, color = LevyraText, fontSize = 26.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(if (playlist.size == 1) "1 brano" else "${playlist.size} brani", color = LevyraMuted, fontSize = 14.sp)
                    }
                    if (playlist.tracks.isNotEmpty()) {
                        IconButton(onClick = { viewModel.exportOpenPlaylist() }) {
                            Icon(Icons.Rounded.DownloadDone, contentDescription = "Scarica playlist", tint = LevyraViolet, modifier = Modifier.size(27.dp))
                        }
                        IconButton(onClick = { viewModel.playPlaylist(playlist.id) }) {
                            Icon(Icons.Rounded.PlaylistPlay, contentDescription = "Riproduci tutto", tint = LevyraCyan, modifier = Modifier.size(30.dp))
                        }
                    }
                }
            }
            if (playlist.tracks.isEmpty()) {
                item { EmptyState("Playlist vuota. Aggiungi brani dai tre puntini su un brano.") }
            } else {
                items(playlist.tracks, key = { "pldetail-${it.id}" }) { track ->
                    TrackRow(
                        track = track,
                        isCurrent = track.id == state.currentTrack?.id,
                        isPlaying = state.isPlaying && track.id == state.currentTrack?.id,
                        isResolving = state.isResolving && track.id == state.currentTrack?.id,
                        isFavorite = track.id in state.favoriteIds,
                        onClick = { viewModel.playPlaylist(playlist.id, track.id) },
                        onFavorite = { viewModel.toggleFavorite(track) },
                        isDownloading = track.id in state.downloadingTrackIds,
                        isDownloaded = track.id in state.downloadedTrackIds,
                        downloadProgress = state.downloadProgressByTrackId[track.id],
                        onDownload = { viewModel.exportTrack(track) },
                        onArtist = { viewModel.openArtist(track) },
                        onRemove = { viewModel.removeFromPlaylist(playlist.id, track.id) }
                    )
                }
            }
        }
        state.currentTrack?.let { current ->
            AlbumNowPlayingDock(
                track = current,
                isPlaying = state.isPlaying,
                isResolving = state.isResolving,
                progress = progressOf(state.positionMs, state.durationMs),
                onToggle = viewModel::togglePlay,
                onOpenPlayer = viewModel::openPlayerScreen,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
            )
        }
    }
}

@Composable
private fun PlayerScreen(viewModel: LevyraViewModel, state: LevyraUiState) {
    val strings = LocalLevyraStrings.current
    val track = state.currentTrack
    val bgStart = track?.let { Color(it.accentStart) } ?: LevyraCyan
    val artworkUrl = track?.largeThumbnailUrl?.ifBlank { track.thumbnailUrl }.orEmpty()
    var mediaSeekFeedbackMs by remember(track?.id) { mutableStateOf(0L) }
    var mediaSeekFeedbackEvent by remember(track?.id) { mutableStateOf(0) }

    LaunchedEffect(mediaSeekFeedbackEvent) {
        if (mediaSeekFeedbackEvent > 0) {
            delay(650L)
            mediaSeekFeedbackMs = 0L
        }
    }

    val artScale by animateFloatAsState(
        targetValue = if (state.isPlaying) 1.02f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "artwork-scale"
    )
    val artCorner by animateDpAsState(
        targetValue = if (state.isPlaying) 24.dp else 12.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "artwork-corner"
    )
    val artShadow by animateFloatAsState(
        targetValue = if (state.isPlaying) 28f else 10f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "artwork-shadow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LevyraBlack)
    ) {
        LevyraBackground(accentStart = track?.accentStart, accentEnd = track?.accentEnd)
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))) // Subtle darkening

        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 540.dp)
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PlayerRoundIconButton(
                        icon = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = strings.back,
                        size = 42.dp,
                        iconSize = 28.dp,
                        tint = LevyraText,
                        background = Color.White.copy(alpha = 0.075f),
                        onClick = { viewModel.selectTab(LevyraTab.Home) }
                    )
                    if (track != null && track.videoUrl.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(500.dp))
                                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)), RoundedCornerShape(500.dp))
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier
                                .background(if (!state.isVideoMode) Color.White.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(500.dp))
                                .clickable { if (state.isVideoMode) viewModel.toggleVideoMode() }
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(strings.song, color = if (!state.isVideoMode) LevyraText else LevyraMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(modifier = Modifier
                                .background(if (state.isVideoMode) Color.White.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(500.dp))
                                .clickable { if (!state.isVideoMode) viewModel.toggleVideoMode() }
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(strings.video, color = if (state.isVideoMode) LevyraText else LevyraMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("RIPRODUZIONE DA ${track?.source ?: "LEVYRA"}", color = LevyraMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.isVideoMode) {
                            PlayerRoundIconButton(
                                icon = Icons.Rounded.PictureInPictureAlt,
                                contentDescription = "Picture in Picture",
                                size = 42.dp,
                                iconSize = 21.dp,
                                tint = LevyraText,
                                background = Color.White.copy(alpha = 0.075f),
                                borderColor = LevyraCyan.copy(alpha = 0.18f),
                                onClick = { LevyraPipBridge.enter() }
                            )
                        }
                        PlayerRoundIconButton(
                            icon = Icons.Rounded.Close,
                            contentDescription = "Chiudi player",
                            size = 42.dp,
                            iconSize = 22.dp,
                            tint = Color.White,
                            background = Color.White.copy(alpha = 0.075f),
                            borderColor = LevyraCyan.copy(alpha = 0.18f),
                            onClick = {
                                viewModel.closePlayer()
                                viewModel.selectTab(LevyraTab.Home)
                            }
                        )
                        PlayerRoundIconButton(
                            icon = Icons.Rounded.MoreVert,
                            contentDescription = "Opzioni",
                            size = 42.dp,
                            iconSize = 24.dp,
                            tint = LevyraText,
                            background = Color.White.copy(alpha = 0.055f),
                            onClick = { viewModel.openAudioQualityPanel() }
                        )
                    }
                }
            }
            if (track == null) {
                item { EmptyState(strings.emptyPlayer) }
            } else {
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (state.isVideoMode && track.videoUrl.isNotBlank()) {
                            LevyraVideoSurface(
                                state = state,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                                    .graphicsLayer {
                                        scaleX = artScale
                                        scaleY = artScale
                                        shadowElevation = artShadow
                                        shape = RoundedCornerShape(artCorner)
                                        clip = true
                                    }
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .padding(vertical = 16.dp)
                                    .graphicsLayer {
                                        scaleX = artScale
                                        scaleY = artScale
                                        shadowElevation = artShadow
                                        shape = RoundedCornerShape(artCorner)
                                        clip = true
                                    }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(artworkUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    Color.White.copy(alpha = 0.09f),
                                                    Color.White.copy(alpha = 0.04f),
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.16f)
                                                )
                                            )
                                        )
                                )
                            }
                        }

                        val leftSeekInteraction = remember(track.id) { MutableInteractionSource() }
                        val rightSeekInteraction = remember(track.id) { MutableInteractionSource() }
                        Row(
                            modifier = Modifier
                                .matchParentSize()
                                .zIndex(20f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .combinedClickable(
                                        interactionSource = leftSeekInteraction,
                                        indication = null,
                                        onClick = {},
                                        onDoubleClick = {
                                            viewModel.seekBy(-PLAYER_MEDIA_SEEK_STEP_MS)
                                            mediaSeekFeedbackMs = -PLAYER_MEDIA_SEEK_STEP_MS
                                            mediaSeekFeedbackEvent += 1
                                        }
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .combinedClickable(
                                        interactionSource = rightSeekInteraction,
                                        indication = null,
                                        onClick = {},
                                        onDoubleClick = {
                                            viewModel.seekBy(PLAYER_MEDIA_SEEK_STEP_MS)
                                            mediaSeekFeedbackMs = PLAYER_MEDIA_SEEK_STEP_MS
                                            mediaSeekFeedbackEvent += 1
                                        }
                                    )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .align(
                                    if (mediaSeekFeedbackMs < 0L) {
                                        Alignment.CenterStart
                                    } else {
                                        Alignment.CenterEnd
                                    }
                                )
                                .padding(horizontal = 30.dp)
                        ) {
                            AnimatedVisibility(
                                visible = mediaSeekFeedbackMs != 0L,
                                enter = fadeIn(animationSpec = tween(110)),
                                exit = fadeOut(animationSpec = tween(180))
                            ) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.62f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = if (mediaSeekFeedbackMs < 0L) "−5 s" else "+5 s",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                color = LevyraText,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = track.artist,
                                color = LevyraMuted,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable { viewModel.openArtist(track) }
                            )
                        }
                        IconButton(onClick = { viewModel.toggleFavorite(track) }) {
                            Icon(
                                imageVector = if (track.id in state.favoriteIds) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Preferiti",
                                tint = if (track.id in state.favoriteIds) LevyraPink else LevyraMuted,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
                item {
                    WaveformVisualizer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(vertical = 4.dp),
                        color = bgStart
                    )
                }
                item {
                    PlayerTimeline(
                        positionMs = state.positionMs,
                        durationMs = state.durationMs,
                        onSeek = viewModel::seekTo
                    )
                }
                item {
                    MainPlayerControls(
                        isPlaying = state.isPlaying,
                        isResolving = state.isResolving,
                        shuffleOn = state.shuffleEnabled,
                        repeatMode = state.repeatMode,
                        onShuffle = viewModel::toggleShuffle,
                        onPrevious = viewModel::previous,
                        onToggle = viewModel::togglePlay,
                        onNext = viewModel::next,
                        onRepeat = viewModel::toggleRepeat
                    )
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.openQueue() }) {
                            Icon(Icons.Rounded.QueueMusic, "Coda", tint = LevyraMuted, modifier = Modifier.size(28.dp))
                        }
                        IconButton(onClick = { viewModel.openLyrics() }) {
                            Icon(Icons.Rounded.Subject, "Testo", tint = LevyraMuted, modifier = Modifier.size(28.dp))
                        }
                        IconButton(onClick = { viewModel.exportCurrentTrack() }) {
                            Icon(Icons.Rounded.Download, "Download", tint = if (state.isOfflineExporting) LevyraCyan else LevyraMuted, modifier = Modifier.size(28.dp))
                        }
                    }
                }
                item {
                    PlayerOptionsRow(
                        speed = state.playbackSpeed,
                        sleepMinutes = state.sleepTimerMinutes,
                        audioNormalization = state.audioNormalization,
                        onSpeed = viewModel::cycleSpeed,
                        onSleep = viewModel::cycleSleepTimer,
                        onNormalization = viewModel::toggleAudioNormalization
                    )
                }
                item {
                    if (state.lyrics.isNotEmpty()) {
                        var showInlineLyrics by remember { mutableStateOf(false) }
                        if (!showInlineLyrics) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.clickable { showInlineLyrics = true }
                                ) {
                                    Text("Mostra testo brano", color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(400.dp)
                                    .padding(vertical = 16.dp)
                                    .glassmorphism(shape = RoundedCornerShape(24.dp))
                            ) {
                                val listState = rememberLazyListState()
                                val activeIndex = state.lyrics.indexOfFirst { state.positionMs in it.startMs..it.endMs }
                                
                                LaunchedEffect(activeIndex) {
                                    if (activeIndex >= 0) {
                                        listState.animateScrollToItem(maxOf(0, activeIndex - 1))
                                    }
                                }
                                
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(24.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    itemsIndexed(state.lyrics) { index, line ->
                                        val isActive = index == activeIndex
                                        Text(
                                            text = line.text,
                                            color = if (isActive) Color.White else Color.White.copy(alpha = 0.35f),
                                            fontSize = if (isActive) 24.sp else 20.sp,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                            modifier = Modifier.clickable { viewModel.seekTo(progressOf(line.startMs, state.durationMs)) }
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { showInlineLyrics = false },
                                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                                ) {
                                    Icon(Icons.Rounded.Close, "Chiudi", tint = Color.White.copy(alpha = 0.5f))
                                }
                            }
                        }
                    } else if (state.lyricsLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .padding(vertical = 16.dp)
                                .glassmorphism(shape = RoundedCornerShape(24.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = LevyraCyan, strokeWidth = 3.dp)
                        }
                    }
                }
                item { PlayerError(state.playerError) }
            }
        }
    }
}

@Composable
private fun PlayerTimeline(positionMs: Long, durationMs: Long, onSeek: (Float) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Slider(
            value = progressOf(positionMs, durationMs),
            onValueChange = onSeek,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = LevyraCyan,
                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
        )
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(positionMs), color = LevyraMuted, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Medium)
            Text(formatDuration(durationMs), color = LevyraMuted, fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun MainPlayerControls(
    isPlaying: Boolean,
    isResolving: Boolean,
    shuffleOn: Boolean,
    repeatMode: com.luc4n3x.levyra.domain.RepeatMode,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        PlayerRoundIconButton(
            icon = Icons.Rounded.Shuffle,
            contentDescription = "Shuffle",
            tint = if (shuffleOn) LevyraCyan else LevyraMuted,
            background = Color.White.copy(alpha = if (shuffleOn) 0.12f else 0.055f),
            borderColor = if (shuffleOn) LevyraCyan.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.08f),
            onClick = onShuffle
        )
        PlayerRoundIconButton(
            icon = Icons.Rounded.SkipPrevious,
            contentDescription = "Precedente",
            size = 52.dp,
            iconSize = 32.dp,
            tint = LevyraText,
            background = Color.White.copy(alpha = 0.065f),
            onClick = onPrevious
        )
        Box(
            modifier = Modifier
                .size(72.dp)
                .shadow(22.dp, CircleShape, clip = false)
                .background(Brush.radialGradient(listOf(Color.White, Color(0xFF67E8FF).copy(alpha = 0.94f), Color(0xFFA78BFA).copy(alpha = 0.88f))), CircleShape)
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)), CircleShape)
                .pressable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            if (isResolving) CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp, color = LevyraBlack)
            else Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = LevyraBlack, modifier = Modifier.size(38.dp))
        }
        PlayerRoundIconButton(
            icon = Icons.Rounded.SkipNext,
            contentDescription = "Successivo",
            size = 52.dp,
            iconSize = 32.dp,
            tint = LevyraText,
            background = Color.White.copy(alpha = 0.065f),
            onClick = onNext
        )
        val repeatIcon = if (repeatMode == com.luc4n3x.levyra.domain.RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat
        PlayerRoundIconButton(
            icon = repeatIcon,
            contentDescription = "Repeat",
            tint = if (repeatMode != com.luc4n3x.levyra.domain.RepeatMode.Off) LevyraCyan else LevyraMuted,
            background = Color.White.copy(alpha = if (repeatMode != com.luc4n3x.levyra.domain.RepeatMode.Off) 0.12f else 0.055f),
            borderColor = if (repeatMode != com.luc4n3x.levyra.domain.RepeatMode.Off) LevyraCyan.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.08f),
            onClick = onRepeat
        )
    }
}

@Composable
private fun PlayerOptionsRow(
    speed: Float,
    sleepMinutes: Int,
    audioNormalization: Boolean,
    onSpeed: () -> Unit,
    onSleep: () -> Unit,
    onNormalization: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OptionChip(
            icon = Icons.Rounded.GraphicEq,
            label = "Norm",
            active = audioNormalization,
            modifier = Modifier.weight(1f),
            onClick = onNormalization
        )
        OptionChip(
            icon = Icons.Rounded.Speed,
            label = "${trimSpeed(speed)}x",
            active = speed != 1f,
            modifier = Modifier.weight(1f),
            onClick = onSpeed
        )
        OptionChip(
            icon = Icons.Rounded.Bedtime,
            label = if (sleepMinutes > 0) "${sleepMinutes}m" else "Timer",
            active = sleepMinutes > 0,
            modifier = Modifier.weight(1f),
            onClick = onSleep
        )
    }
}

@Composable
private fun OptionChip(icon: ImageVector, label: String, active: Boolean, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val alpha by animateFloatAsState(targetValue = if (enabled) 1f else 0.5f, animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f))
    val bgColor = if (active) LevyraCyan.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f)
    val textColor = if (active) LevyraCyan else LevyraText
    val borderColor = if (active) LevyraCyan.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.08f)

    Surface(
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(500.dp),
        modifier = modifier
            .height(40.dp)
            .graphicsLayer { this.alpha = alpha }
            .pressable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = textColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ShareOptionChip(track: Track, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Surface(
        color = Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        shape = CircleShape,
        modifier = modifier
            .size(48.dp)
            .pressable {
                val link = track.videoUrl.ifBlank { "https://music.youtube.com/watch?v=${track.id}" }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "${track.title} — ${track.artist}")
                    putExtra(Intent.EXTRA_TEXT, "${track.title} — ${track.artist}\n$link")
                }
                context.startActivity(Intent.createChooser(intent, "Condividi brano"))
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Share, null, tint = LevyraText, modifier = Modifier.size(20.dp))
        }
    }
}

private fun trimSpeed(speed: Float): String {
    return if (speed % 1f == 0f) speed.toInt().toString() else speed.toString().trimEnd('0').trimEnd('.')
}

@Composable
private fun LanguageSelector(selectedCode: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(LevyraLanguageCatalog.languages, key = { it.code }) { language ->
            val selected = language.code == LevyraLanguageCatalog.normalize(selectedCode)
            val scale by animateFloatAsState(
                targetValue = if (selected) 1.04f else 1f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f)
            )
            val bgAlpha by animateFloatAsState(targetValue = if (selected) 0.18f else 0.04f)
            val borderAlpha by animateFloatAsState(targetValue = if (selected) 0.6f else 0.06f)

            Surface(
                color = LevyraCyan.copy(alpha = bgAlpha),
                border = BorderStroke(1.dp, LevyraCyan.copy(alpha = borderAlpha)),
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier
                    .width(130.dp)
                    .height(130.dp)
                    .graphicsLayer {
                        this.scaleX = scale
                        this.scaleY = scale
                    }
                    .pressable(onClick = { onSelect(language.code) })
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = language.flag,
                        fontSize = 42.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = language.englishName,
                        color = LevyraText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = language.nativeName,
                        color = if (selected) LevyraCyan else LevyraMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingOverlay(tastes: List<Taste>, selectedLanguageCode: String, onDone: (String, Set<String>, String) -> Unit) {
    var selected by remember { mutableStateOf(setOf<String>()) }
    var name by remember { mutableStateOf("") }
    var languageCode by remember(selectedLanguageCode) { mutableStateOf(LevyraLanguageCatalog.normalize(selectedLanguageCode)) }
    val strings = LevyraStrings.forCode(languageCode)
    val blocker = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(interactionSource = blocker, indication = null) {}
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 40.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(strings.welcomeBadge, color = LevyraCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(strings.welcomeTitle, color = LevyraText, fontSize = 46.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp)
                Spacer(modifier = Modifier.height(28.dp))
                Text(strings.languageQuestion, color = LevyraText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                LanguageSelector(selectedCode = languageCode, onSelect = { languageCode = it })
                Spacer(modifier = Modifier.height(28.dp))
                Text(strings.nameQuestion, color = LevyraText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                BasicTextField(
                    value = name,
                    onValueChange = { newName ->
                        name = newName.replaceFirstChar { char ->
                            if (char.isLowerCase()) char.titlecase(java.util.Locale.getDefault()) else char.toString()
                        }
                    },
                    textStyle = TextStyle(color = LevyraText, fontSize = 24.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 32.dp)
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(20.dp),
                    cursorBrush = SolidColor(LevyraCyan),
                    decorationBox = { innerTextField ->
                        if (name.isEmpty()) {
                            Text(strings.namePlaceholder, color = LevyraMuted, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        innerTextField()
                    }
                )
                Text(strings.tasteQuestion, color = LevyraText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(tastes.chunked(2)) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { taste ->
                        TasteCard(
                            taste = taste,
                            selected = taste.id in selected,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                selected = if (taste.id in selected) selected - taste.id else selected + taste.id
                            }
                        )
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black, Color.Black)))
                .padding(bottom = 24.dp, top = 32.dp, start = 24.dp, end = 24.dp)
                .navigationBarsPadding()
        ) {
            val isActive = selected.size >= 3 || name.isNotBlank()
            Surface(
                color = if (isActive) LevyraCyan else Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().height(56.dp).pressable(onClick = { onDone(name, selected, languageCode) })
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (selected.isEmpty() && name.isBlank()) strings.skipAndContinue else strings.startListening,
                        color = if (isActive) Color.Black else LevyraMuted,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TasteCard(taste: Taste, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        color = if (selected) LevyraCyan else Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(percent = 50),
        modifier = modifier
            .pressable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(taste.emoji, fontSize = 22.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                taste.label,
                color = if (selected) Color.Black else LevyraText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingsOverlay(
    animationsEnabled: Boolean,
    dynamicColor: Boolean,
    sponsorBlock: Boolean,
    skipSilence: Boolean,
    updateInfo: AppUpdateInfo?,
    isCheckingUpdates: Boolean,
    currentLanguageCode: String,
    themePreset: String,
    onThemePreset: (String) -> Unit,
    onAnimations: (Boolean) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onSponsorBlock: (Boolean) -> Unit,
    onSkipSilence: (Boolean) -> Unit,
    onLanguage: (String) -> Unit,
    onCheckUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onRedoQuestionnaire: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    var languageExpanded by remember { mutableStateOf(false) }
    val blocker = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(LevyraInk, LevyraBlack)))
            .clickable(interactionSource = blocker, indication = null) {}
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(strings.settings, color = LevyraText, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text(strings.settingsSubtitle, color = LevyraMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    CircleIconButton(
                        icon = Icons.Rounded.Close,
                        tint = LevyraText,
                        background = Color.White.copy(alpha = 0.08f),
                        onClick = onClose
                    )
                }
            }
            item { SettingsSectionLabel(strings.design) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Rounded.Palette, null, tint = LevyraCyan, modifier = Modifier.size(20.dp))
                        Column {
                            Text(strings.theme, color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            Text(strings.themeSubtitle, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    ThemeSelector(selectedId = themePreset, onSelect = onThemePreset)
                }
            }
            item {
                SettingsToggle(
                    icon = Icons.Rounded.Bolt,
                    title = strings.animations,
                    subtitle = strings.animationsSubtitle,
                    checked = animationsEnabled,
                    onCheckedChange = onAnimations
                )
            }
            item {
                SettingsToggle(
                    icon = Icons.Rounded.Album,
                    title = strings.dynamicColor,
                    subtitle = strings.dynamicColorSubtitle,
                    checked = dynamicColor,
                    onCheckedChange = onDynamicColor
                )
            }
            item { SettingsSectionLabel(strings.playback) }
            item {
                SettingsToggle(
                    icon = Icons.Rounded.SkipNext,
                    title = strings.sponsorBlock,
                    subtitle = strings.sponsorBlockSubtitle,
                    checked = sponsorBlock,
                    onCheckedChange = onSponsorBlock
                )
            }
            item {
                SettingsToggle(
                    icon = Icons.Rounded.Bedtime,
                    title = strings.skipSilence,
                    subtitle = strings.skipSilenceSubtitle,
                    checked = skipSilence,
                    onCheckedChange = onSkipSilence
                )
            }
            item { SettingsSectionLabel(strings.preferences) }
            item {
                SettingsButton(
                    icon = Icons.Rounded.Settings,
                    title = strings.redoQuestionnaire,
                    subtitle = strings.redoQuestionnaireSubtitle,
                    onClick = onRedoQuestionnaire
                )
            }
            item {
                SettingsButton(
                    icon = Icons.Rounded.Settings,
                    title = strings.language,
                    subtitle = "${strings.languageSubtitle}: ${LevyraLanguageCatalog.displayName(currentLanguageCode)}",
                    onClick = { languageExpanded = !languageExpanded }
                )
            }
            if (languageExpanded) {
                item { LanguageSelector(selectedCode = currentLanguageCode, onSelect = onLanguage, modifier = Modifier.padding(bottom = 4.dp)) }
            }
            item { SettingsSectionLabel(strings.app) }
            item {
                SettingsUpdateCard(
                    updateInfo = updateInfo,
                    isChecking = isCheckingUpdates,
                    onCheck = onCheckUpdates,
                    onDownload = onDownloadUpdate
                )
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "LEVYRA ${BuildConfig.VERSION_NAME}",
                        color = LevyraMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Made with ❤️ by ",
                            color = LevyraMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .padding(horizontal = 6.dp)
                                .drawBehind {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                LevyraCyan.copy(alpha = 0.5f),
                                                LevyraViolet.copy(alpha = 0.2f),
                                                Color.Transparent
                                            ),
                                            radius = size.height * 1.5f
                                        )
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = "https://github.com/LUC4N3X.png",
                                contentDescription = "LUC4N3X",
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, LevyraCyan.copy(alpha = 0.3f), CircleShape)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "LUC4N3X",
                            color = LevyraText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(text, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.4.sp, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun ThemeSelector(selectedId: String, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(LevyraThemes.presets, key = { "theme-${it.id}" }) { preset ->
            ThemePresetCard(
                preset = preset,
                selected = preset.id == selectedId,
                onClick = { onSelect(preset.id) }
            )
        }
    }
}

@Composable
private fun ThemePresetCard(preset: LevyraPalette, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = preset.black,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) LevyraCyan else if (preset.isLight) Color(0x2211131F) else Color.White.copy(alpha = 0.14f)),
        modifier = Modifier.pressable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .width(112.dp)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(preset.emoji, fontSize = 20.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(preset.cyan))
                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(preset.violet))
                Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(preset.pink))
            }
            Text(
                preset.label,
                color = preset.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingsToggle(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        color = LevyraAdaptiveCard,
        border = BorderStroke(1.dp, LevyraAdaptiveHairline),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(LevyraCyan.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = LevyraCyan, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = LevyraBlack,
                    checkedTrackColor = LevyraCyan,
                    uncheckedThumbColor = LevyraMuted,
                    uncheckedTrackColor = LevyraAdaptiveTrack
                )
            )
        }
    }
}

@Composable
private fun SettingsButton(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        color = LevyraAdaptiveCard,
        border = BorderStroke(1.dp, LevyraAdaptiveHairline),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(LevyraPink.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = LevyraPink, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SettingsUpdateCard(
    updateInfo: AppUpdateInfo?,
    isChecking: Boolean,
    onCheck: () -> Unit,
    onDownload: () -> Unit
) {
    val hasUpdate = updateInfo?.isNewer == true
    Surface(
        color = Color.Transparent,
        border = BorderStroke(1.dp, if (hasUpdate) LevyraCyan.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            if (hasUpdate) LevyraCyan.copy(alpha = 0.17f) else Color.White.copy(alpha = 0.055f),
                            Color.White.copy(alpha = 0.035f),
                            if (hasUpdate) LevyraViolet.copy(alpha = 0.13f) else Color.White.copy(alpha = 0.04f)
                        )
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(LevyraCyan.copy(alpha = 0.16f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = LevyraCyan)
                    } else {
                        Icon(Icons.Rounded.Bolt, null, tint = LevyraCyan, modifier = Modifier.size(21.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = if (hasUpdate) "Aggiornamento disponibile" else "Aggiornamenti",
                        color = LevyraText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = when {
                            isChecking -> "Controllo ultima versione…"
                            hasUpdate -> "LEVYRA ${updateInfo?.latestVersionName.orEmpty()} pronta al download"
                            updateInfo != null -> "Installata la versione più recente"
                            else -> "Verifica nuove versioni pubblicate"
                        },
                        color = LevyraMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (hasUpdate && updateInfo != null) {
                Surface(
                    color = Color.Black.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(updateInfo.releaseTitle.ifBlank { "LEVYRA ${updateInfo.latestVersionName}" }, color = LevyraText, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = if (updateInfo.directApk) "APK firmato pronto da installare" else "Pagina release pronta da aprire",
                            color = LevyraMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SettingsMiniButton(
                    label = if (isChecking) "Controllo" else "Check",
                    accent = LevyraCyan,
                    enabled = !isChecking,
                    modifier = Modifier.weight(1f),
                    onClick = onCheck
                )
                if (hasUpdate) {
                    SettingsMiniButton(
                        label = "Scarica",
                        accent = LevyraViolet,
                        enabled = !isChecking,
                        modifier = Modifier.weight(1f),
                        onClick = onDownload
                    )
                }
            }
            Text(
                text = "Versione installata: ${BuildConfig.VERSION_NAME}",
                color = LevyraMuted.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SettingsMiniButton(
    label: String,
    accent: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = if (enabled) accent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.045f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (enabled) accent.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.07f)),
        modifier = modifier
            .height(42.dp)
            .pressable(onClick = { if (enabled) onClick() })
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (enabled) LevyraText else LevyraMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun LevyraLogoMark(size: Dp = 58.dp) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size * 1.22f)
                .blur(18.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            LevyraCyan.copy(alpha = 0.24f),
                            LevyraViolet.copy(alpha = 0.16f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFF1A1B22))
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            LevyraCyan.copy(alpha = 0.9f),
                            LevyraViolet.copy(alpha = 0.5f),
                            Color.Transparent,
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.levyra_logo),
                contentDescription = "Logo Levyra",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun LevyraWordmark(fontSize: TextUnit = 30.sp, dotSize: Dp = 5.dp) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = "LEVYRA",
            style = TextStyle(
                brush = Brush.linearGradient(
                    listOf(Color.White, Color(0xFFE7E1FF), LevyraCyan.copy(alpha = 0.92f))
                ),
                fontSize = fontSize,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.5).sp
            )
        )
        Box(
            modifier = Modifier
                .padding(start = 3.dp, bottom = 4.dp)
                .size(dotSize)
                .background(
                    Brush.linearGradient(listOf(LevyraCyan, LevyraViolet)),
                    CircleShape
                )
        )
    }
}

@Composable
private fun GreetingBar(userName: String, isResolving: Boolean, onSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            LevyraLogoMark(size = 56.dp)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                LevyraWordmark(fontSize = 27.sp, dotSize = 5.dp)
                Text(
                    text = greeting(userName),
                    color = LevyraMuted,
                    fontSize = 12.5.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Surface(
            color = LevyraAdaptiveChip,
            border = BorderStroke(Dp.Hairline, LevyraAdaptiveHairline),
            shape = CircleShape,
            modifier = Modifier
                .size(48.dp)
                .pressable(onClick = onSettings)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isResolving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = LevyraCyan
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = LocalLevyraStrings.current.settings,
                        tint = LevyraText,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetroHeroDeck(
    tracks: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    isResolving: Boolean,
    favoritesCount: Int,
    queueCount: Int,
    onPrimary: (Track) -> Unit,
    onPlayAll: () -> Unit,
    onOpenLibrary: () -> Unit
) {
    val hero = currentTrack ?: tracks.firstOrNull() ?: return
    val accentStart = Color(hero.accentStart)
    val accentEnd = Color(hero.accentEnd)
    Surface(
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(30.dp),
        shadowElevation = 18.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            accentStart.copy(alpha = 0.58f),
                            Color(0xFF11172A),
                            accentEnd.copy(alpha = 0.44f)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(150.dp)
                    .background(Color.White.copy(alpha = 0.06f), CircleShape)
            )
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(color = Color.Black.copy(alpha = 0.18f), shape = CircleShape) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.Headphones, null, tint = LevyraCyan, modifier = Modifier.size(15.dp))
                            Text("DISCOVERY FLOW", color = LevyraText, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                        }
                    }
                    Surface(color = Color.White.copy(alpha = 0.1f), shape = CircleShape, modifier = Modifier.pressable(onClick = onPlayAll)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, tint = LevyraText, modifier = Modifier.size(15.dp))
                            Text("Play", color = LevyraText, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(modifier = Modifier.size(118.dp)) {
                        tracks.drop(1).take(2).forEachIndexed { index, track ->
                            CoverImage(
                                track = track,
                                modifier = Modifier
                                    .align(if (index == 0) Alignment.TopEnd else Alignment.BottomStart)
                                    .size(72.dp)
                                    .graphicsLayer {
                                        alpha = 0.58f
                                        rotationZ = if (index == 0) 8f else -8f
                                    }
                                    .clip(RoundedCornerShape(18.dp))
                            )
                        }
                        CoverImage(
                            track = hero,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(96.dp)
                                .border(2.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
                                .clip(RoundedCornerShape(24.dp)),
                            highRes = true
                        )
                        if (isPlaying || isResolving) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.54f),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(96.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (isResolving) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp, color = LevyraCyan)
                                    else Icon(Icons.Rounded.GraphicEq, null, tint = LevyraCyan, modifier = Modifier.size(28.dp))
                                }
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(hero.title, color = LevyraText, fontSize = 24.sp, lineHeight = 27.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(hero.artist, color = LevyraText.copy(alpha = 0.78f), fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetroStatPill(Icons.Rounded.QueueMusic, queueCount.coerceAtLeast(tracks.size).toString(), "queue")
                            MetroStatPill(Icons.Rounded.Favorite, favoritesCount.toString(), "saved")
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MetroActionButton(
                        icon = Icons.Rounded.PlayArrow,
                        text = if (currentTrack?.id == hero.id && isPlaying) "Apri player" else "Ascolta ora",
                        accent = LevyraCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { onPrimary(hero) }
                    )
                    MetroActionButton(
                        icon = Icons.Rounded.LibraryMusic,
                        text = "Libreria",
                        accent = LevyraPink,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenLibrary
                    )
                }
            }
        }
    }
}

@Composable
private fun MetroStatPill(icon: ImageVector, value: String, label: String) {
    Surface(color = Color.Black.copy(alpha = 0.18f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), shape = CircleShape) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, null, tint = LevyraCyan, modifier = Modifier.size(13.dp))
            Text(value, color = LevyraText, fontSize = 11.sp, fontWeight = FontWeight.Black)
            Text(label, color = LevyraText.copy(alpha = 0.62f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MetroActionButton(icon: ImageVector, text: String, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.26f)),
        shape = RoundedCornerShape(17.dp),
        modifier = modifier
            .height(50.dp)
            .pressable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun FloatingArtwork(track: Track, isPlaying: Boolean, isResolving: Boolean, modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying && !isResolving) 1.01f else 1.0f,
        label = "BreathingShadow"
    )

    Box(
        modifier = modifier.aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        CoverImage(
            track = track,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .shadow(26.dp, RoundedCornerShape(26.dp), spotColor = Color.Black, ambientColor = Color.Black)
                .clip(RoundedCornerShape(26.dp))
                .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(26.dp)),
            highRes = true
        )
    }
}

@Composable
private fun QuickStart(
    track: Track?,
    isPlaying: Boolean,
    isResolving: Boolean,
    progress: Float,
    hasSuggestions: Boolean,
    onResume: () -> Unit,
    onShuffle: () -> Unit,
    onOpenFavorites: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (track != null) {
            Surface(
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .pressable(onClick = onResume)
            ) {
                Box {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CoverImage(track, Modifier.size(54.dp).clip(RoundedCornerShape(10.dp)))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Continua ad ascoltare", color = LevyraCyan, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            Text(track.title, color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (isResolving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp).padding(end = 8.dp), strokeWidth = 2.dp, color = LevyraCyan)
                        } else if (isPlaying) {
                            Icon(Icons.Rounded.Equalizer, null, tint = LevyraCyan, modifier = Modifier.size(20.dp).padding(end = 8.dp))
                        } else {
                            Icon(Icons.Rounded.PlayArrow, null, tint = LevyraText, modifier = Modifier.size(24.dp).padding(end = 8.dp))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(2.dp)
                            .background(LevyraCyan)
                            .align(Alignment.BottomStart)
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            QuickAction(
                icon = Icons.Rounded.Shuffle,
                label = "Mix per te",
                accent = LevyraCyan,
                enabled = hasSuggestions,
                modifier = Modifier.weight(1f),
                onClick = onShuffle
            )
            QuickAction(
                icon = Icons.Rounded.Favorite,
                label = "Preferiti",
                accent = LevyraPink,
                enabled = true,
                modifier = Modifier.weight(1f),
                onClick = onOpenFavorites
            )
        }
    }
}

@Composable
private fun QuickAction(icon: ImageVector, label: String, accent: Color, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        color = Color.White.copy(alpha = if (enabled) 0.07f else 0.03f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .height(56.dp)
            .pressable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(accent.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(19.dp))
            }
            Text(label, color = LevyraText, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SearchDock(query: String, isSearching: Boolean, onQuery: (String) -> Unit, onSearch: () -> Unit, onFocus: () -> Unit) {
    Surface(
        color = Color.White.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onFocus)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Rounded.Search, null, tint = LevyraCyan, modifier = Modifier.size(20.dp))
                Text(
                    text = if (query.isEmpty()) "Cerca brani, artisti..." else query,
                    color = if (query.isEmpty()) LevyraMuted else LevyraText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Surface(
                color = Color.White.copy(alpha = 0.06f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "⌘K",
                    color = LevyraMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun MoodRow(moods: List<Mood>, selectedId: String?, onSelect: (Mood) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        items(
            items = moods,
            key = { it.id },
            contentType = { "home-mood" }
        ) { mood ->
            val selected = mood.id == selectedId
            Surface(
                color = if (selected) {
                    Color(mood.accentStart).copy(alpha = if (LevyraIsLight) 0.15f else 0.18f)
                } else {
                    LevyraAdaptiveChip
                },
                border = BorderStroke(
                    width = if (selected) 1.dp else Dp.Hairline,
                    color = if (selected) {
                        Color(mood.accentStart).copy(alpha = 0.58f)
                    } else {
                        LevyraAdaptiveHairline
                    }
                ),
                shape = CircleShape,
                modifier = Modifier.pressable(onClick = { onSelect(mood) })
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Text(text = mood.icon, fontSize = 15.sp)
                    Text(
                        text = mood.title,
                        color = LevyraText,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeaderAction(title: String, onPlayAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .height(22.dp)
                    .width(4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Brush.verticalGradient(listOf(LevyraCyan, LevyraViolet)))
            )
            Text(
                text = title,
                color = LevyraText,
                fontSize = 21.sp,
                lineHeight = 23.sp,
                letterSpacing = (-0.55).sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Surface(
            color = LevyraAdaptiveChip,
            border = BorderStroke(Dp.Hairline, LevyraAdaptiveHairline),
            shape = CircleShape,
            modifier = Modifier
                .size(36.dp)
                .pressable(onClick = onPlayAll)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = LocalLevyraStrings.current.play,
                    tint = LevyraCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeAlbumLoadingRow() {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(start = HomeHorizontalInset, end = HomeHorizontalShelfEndPadding)
    ) {
        items(4, key = { "home-album-loading-$it" }) {
            Column(
                modifier = Modifier.width(148.dp).shimmer(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.075f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color.White.copy(alpha = 0.07f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                )
            }
        }
    }
}

@Composable
private fun HomeAlbumHitRow(albums: List<AlbumHit>, animationsEnabled: Boolean, onOpen: (AlbumHit) -> Unit) {
    if (albums.isEmpty()) return
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(start = HomeHorizontalInset, end = HomeHorizontalShelfEndPadding)
    ) {
        itemsIndexed(
            items = albums,
            key = { index, album -> "home-album-card-$index-${album.title}-${album.artist}" },
            contentType = { _, _ -> "home-album-card" }
        ) { index, album ->
            var isPressed by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue = if (isPressed && animationsEnabled) 0.95f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "homeAlbumScale"
            )
            val modifier = if (animationsEnabled) {
                Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
            } else Modifier
            Column(
                modifier = modifier
                    .width(148.dp)
                    .pointerInput(album.title, album.artist) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                            },
                            onTap = { onOpen(album) }
                        )
                    },
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = CinematicGlass.copy(alpha = 0.3f),
                    border = BorderStroke(Dp.Hairline, Color.White.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(18.dp),
                    shadowElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    ) {
                        if (album.thumbnailUrl.isNotBlank()) {
                            AsyncImage(
                                model = album.thumbnailUrl,
                                contentDescription = album.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(18.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Brush.linearGradient(listOf(LevyraPanel, LevyraInk))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Album, null, tint = LevyraCyan, modifier = Modifier.size(42.dp))
                            }
                        }
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.42f), Color.Black.copy(alpha = 0.76f))))
                        )
                        Surface(
                            color = CinematicGlassDeep.copy(alpha = 0.52f),
                            border = BorderStroke(Dp.Hairline, Color.White.copy(alpha = 0.15f)),
                            shape = CircleShape,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Rounded.Album, null, tint = LevyraCyan, modifier = Modifier.size(12.dp))
                                Text("${index + 1}", color = LevyraText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Column(modifier = Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(album.title, color = LevyraText, fontSize = 13.5.sp, lineHeight = 15.5.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val subtitle = listOf("Album", album.artist, album.year).filter { it.isNotBlank() }.joinToString(" • ")
                    Text(subtitle, color = LevyraMuted, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun AlbumCardRow(tracks: List<Track>, currentId: String?, animationsEnabled: Boolean, onPlay: (Track) -> Unit) {
    if (tracks.isEmpty()) return
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(start = HomeHorizontalInset, end = HomeHorizontalShelfEndPadding)
    ) {
        itemsIndexed(
            items = tracks,
            key = { index, track -> "album-card-$index-${track.id}" },
            contentType = { _, _ -> "album-card" }
        ) { index, track ->
            val isCurrent = track.id == currentId
            var isPressed by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue = if (isPressed && animationsEnabled) 0.95f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "scale"
            )
            val modifier = if (animationsEnabled) {
                Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
            } else Modifier
            Column(
                modifier = modifier
                    .width(148.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                            },
                            onTap = { onPlay(track) }
                        )
                    },
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = CinematicGlass.copy(alpha = 0.3f),
                    border = BorderStroke(if (isCurrent) 1.dp else Dp.Hairline, if (isCurrent) LevyraCyan.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(18.dp),
                    shadowElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    ) {
                        CoverImage(
                            track = track,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(18.dp)),
                            highRes = false
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f), Color.Black.copy(alpha = 0.75f))))
                        )
                        Surface(
                            color = CinematicGlassDeep.copy(alpha = 0.5f),
                            border = BorderStroke(Dp.Hairline, Color.White.copy(alpha = 0.15f)),
                            shape = CircleShape,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(if (isCurrent) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow, null, tint = LevyraCyan, modifier = Modifier.size(12.dp))
                                Text(if (isCurrent) "ON" else "${index + 1}", color = LevyraText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Column(modifier = Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(track.title, color = if (isCurrent) LevyraCyan else LevyraText, fontSize = 13.5.sp, lineHeight = 15.5.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val kind = if (track.album.isNotBlank() && track.album != track.title && track.album != "YouTube Music") "Album" else "Single"
                    Text("$kind • ${track.artist}", color = LevyraMuted, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun RowCarousel(
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    isResolving: Boolean,
    favoriteIds: Set<String>,
    onPlay: (Track) -> Unit,
    onFavorite: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onDownload: (Track) -> Unit
) {
    if (tracks.isEmpty()) return
    val perPage = 4
    val pages = (tracks.size + perPage - 1) / perPage
    val pagerState = rememberPagerState(pageCount = { pages })
    HorizontalPager(
        state = pagerState,
        pageSpacing = 16.dp,
        contentPadding = PaddingValues(end = if (pages > 1) 48.dp else 0.dp)
    ) { page ->
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            tracks.drop(page * perPage).take(perPage).forEach { track ->
                CompactRow(
                    track = track,
                    isCurrent = track.id == currentId,
                    isPlaying = isPlaying && track.id == currentId,
                    isResolving = isResolving && track.id == currentId,
                    isFavorite = track.id in favoriteIds,
                    onClick = { onPlay(track) },
                    onFavorite = { onFavorite(track) },
                    onAddToPlaylist = { onAddToPlaylist(track) },
                    onAddToQueue = { onAddToQueue(track) },
                    onDownload = { onDownload(track) }
                )
            }
        }
    }
}

@Composable
private fun CompactRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onAddToQueue: () -> Unit,
    onDownload: () -> Unit
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box {
            CoverImage(track, Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)))
            if (isPlaying || isResolving) {
                Surface(color = Color.Black.copy(alpha = 0.45f), shape = RoundedCornerShape(10.dp), modifier = Modifier.matchParentSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isResolving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = LevyraCyan)
                        else Icon(Icons.Rounded.Equalizer, null, tint = LevyraCyan, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                track.title,
                color = if (isCurrent) LevyraCyan else LevyraText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(track.artist, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Opzioni brano",
                    tint = LevyraMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(if (isFavorite) "Rimuovi dai preferiti" else "Aggiungi ai preferiti") },
                    leadingIcon = { Icon(if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null) },
                    onClick = {
                        expanded = false
                        onFavorite()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Aggiungi a playlist") },
                    leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, null) },
                    onClick = {
                        expanded = false
                        onAddToPlaylist()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Aggiungi alla coda") },
                    leadingIcon = { Icon(Icons.Rounded.QueueMusic, null) },
                    onClick = {
                        expanded = false
                        onAddToQueue()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Scarica") },
                    leadingIcon = { Icon(Icons.Rounded.Download, null) },
                    onClick = {
                        expanded = false
                        onDownload()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Condividi") },
                    leadingIcon = { Icon(Icons.Rounded.Share, null) },
                    onClick = {
                        expanded = false
                        val shareText = buildString {
                            append(track.title)
                            if (track.artist.isNotBlank()) append(" - ").append(track.artist)
                            val link = track.videoUrl.ifBlank { track.streamUrl }
                            if (link.isNotBlank()) append("\n").append(link)
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "Condividi brano"))
                    }
                )
            }
        }
    }
}
@Composable
private fun ChartRow(
    rank: Int,
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onAddToQueue: () -> Unit
) {
    Surface(
        color = if (isCurrent) LevyraCyan.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, if (isCurrent) LevyraCyan.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.07f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.width(26.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = rank.toString(),
                    color = if (rank <= 3) LevyraCyan else LevyraMuted,
                    fontSize = if (rank <= 3) 20.sp else 16.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Box {
                CoverImage(track, Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)), zoom = 1.35f)
                if (isPlaying || isResolving) {
                    Surface(color = Color.Black.copy(alpha = 0.45f), shape = RoundedCornerShape(12.dp), modifier = Modifier.matchParentSize()) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isResolving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = LevyraCyan)
                            else Icon(Icons.Rounded.Equalizer, null, tint = LevyraCyan, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(track.title, color = LevyraText, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            var expanded by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "Opzioni",
                        tint = LevyraMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isFavorite) "Rimuovi dai preferiti" else "Aggiungi ai preferiti") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFavorite) LevyraPink else LocalContentColor.current
                            )
                        },
                        onClick = {
                            expanded = false
                            onFavorite()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Aggiungi alla coda") },
                        leadingIcon = { Icon(Icons.Rounded.QueueMusic, null) },
                        onClick = {
                            expanded = false
                            onAddToQueue()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Aggiungi a playlist") },
                        leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, null) },
                        onClick = {
                            expanded = false
                            onAddToPlaylist()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedTrackCard(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = LevyraPanelSoft,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box {
                CoverImage(track, Modifier.size(110.dp).clip(CircleShape))
                if (isPlaying || isResolving) {
                    Surface(color = Color.Black.copy(alpha = 0.5f), shape = CircleShape, modifier = Modifier.matchParentSize()) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isResolving) CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp, color = LevyraCyan)
                            else Icon(Icons.Rounded.Equalizer, null, tint = LevyraCyan, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(track.title, color = LevyraText, fontSize = 24.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = LevyraMuted, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Surface(
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(track.source, color = LevyraText, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun SearchResultsHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 2.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                LevyraCyan.copy(alpha = 0.28f),
                                LevyraViolet.copy(alpha = 0.22f),
                                LevyraPink.copy(alpha = 0.18f)
                            )
                        ),
                        CircleShape
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.13f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.MusicNote, null, tint = LevyraCyan, modifier = Modifier.size(20.dp))
            }
            Text(
                text = "Potrebbe piacerti anche",
                color = LevyraText,
                fontSize = 23.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            LevyraCyan.copy(alpha = 0.95f),
                            LevyraViolet.copy(alpha = 0.72f),
                            Color.Transparent
                        )
                    ),
                    RoundedCornerShape(99.dp)
                )
        )
    }
}

@Composable
private fun SearchSuggestionTrackCard(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(track.accentStart).copy(alpha = if (isCurrent) 0.24f else 0.12f),
                        Color(track.accentEnd).copy(alpha = if (isCurrent) 0.16f else 0.07f),
                        Color.White.copy(alpha = 0.035f)
                    )
                ),
                RoundedCornerShape(22.dp)
            )
            .border(
                1.dp,
                if (isCurrent) LevyraCyan.copy(alpha = 0.50f) else Color.White.copy(alpha = 0.075f),
                RoundedCornerShape(22.dp)
            )
            .pressable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(48.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(track.accentStart).copy(alpha = 0.95f),
                                Color(track.accentEnd).copy(alpha = 0.78f)
                            )
                        ),
                        RoundedCornerShape(99.dp)
                    )
            )
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        if (isCurrent) LevyraCyan.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.065f),
                        CircleShape
                    )
                    .border(
                        1.dp,
                        if (isCurrent) LevyraCyan.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.08f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isResolving -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = LevyraCyan)
                    isPlaying -> Icon(Icons.Rounded.GraphicEq, null, tint = LevyraCyan, modifier = Modifier.size(22.dp))
                    else -> Icon(Icons.Rounded.PlayArrow, null, tint = if (isCurrent) LevyraCyan else LevyraText, modifier = Modifier.size(24.dp))
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = track.title,
                    color = if (isCurrent) LevyraCyan else LevyraText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    color = LevyraMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onFavorite, modifier = Modifier.size(38.dp)) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Preferito",
                    tint = if (isFavorite) LevyraPink else LevyraMuted,
                    modifier = Modifier.size(21.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchFilterChips(
    selected: SearchFilter,
    hasArtists: Boolean,
    hasAlbums: Boolean,
    onSelect: (SearchFilter) -> Unit
) {
    val chips = buildList {
        add(SearchFilter.All to "Tutti")
        add(SearchFilter.Songs to "Brani")
        if (hasArtists) add(SearchFilter.Artists to "Artisti")
        if (hasAlbums) add(SearchFilter.Albums to "Album")
    }
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.forEach { (filter, label) ->
            val active = filter == selected
            Surface(
                color = if (active) LevyraText else Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(99.dp),
                border = if (active) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier.clickable { onSelect(filter) }
            ) {
                Text(
                    text = label,
                    color = if (active) LevyraBlack else LevyraText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun TopResultCard(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onArtist: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Risultato principale", color = LevyraCyan, fontSize = 13.sp, fontWeight = FontWeight.Black)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(track.accentStart).copy(alpha = 0.30f),
                            Color(track.accentEnd).copy(alpha = 0.14f),
                            Color.White.copy(alpha = 0.04f)
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                .pressable(onClick = onPlay)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    CoverImage(track, Modifier.size(76.dp).clip(RoundedCornerShape(14.dp)), highRes = true)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(track.title, color = LevyraText, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            track.artist,
                            color = LevyraMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { onArtist() }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = LevyraText,
                        shape = RoundedCornerShape(99.dp),
                        modifier = Modifier.weight(1f).pressable(onClick = onPlay)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isResolving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = LevyraBlack)
                            else Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = LevyraBlack, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isPlaying) "In riproduzione" else "Riproduci", color = LevyraBlack, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Surface(color = Color.White.copy(alpha = 0.08f), shape = CircleShape, modifier = Modifier.size(46.dp).clickable { onAddToPlaylist() }) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.PlaylistAdd, null, tint = LevyraText, modifier = Modifier.size(22.dp))
                        }
                    }
                    Surface(color = Color.White.copy(alpha = 0.08f), shape = CircleShape, modifier = Modifier.size(46.dp).clickable { onFavorite() }) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                null,
                                tint = if (isFavorite) LevyraPink else LevyraText,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchTrackCard(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    isFavorite: Boolean,
    isDownloading: Boolean,
    isDownloaded: Boolean,
    downloadProgress: Int?,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDownload: () -> Unit,
    onArtist: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isCurrent) {
                    cinematicGlassBrush(Color(track.accentStart), Color(track.accentEnd), 0.82f)
                } else {
                    SolidColor(Color.Transparent)
                }
            )
            .border(
                1.dp,
                if (isCurrent) LevyraCyan.copy(alpha = 0.30f) else Color.Transparent,
                RoundedCornerShape(18.dp)
            )
            .pressable(onClick = onClick)
            .padding(horizontal = if (isCurrent) 9.dp else 0.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Box {
            CoverImage(track, Modifier.size(52.dp).clip(RoundedCornerShape(9.dp)))
            if (isPlaying || isResolving) {
                Surface(color = Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(9.dp), modifier = Modifier.matchParentSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isResolving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = LevyraCyan)
                        else Icon(Icons.Rounded.Equalizer, null, tint = LevyraCyan, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(track.title, color = if (isCurrent) LevyraCyan else LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                track.artist,
                color = LevyraMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onArtist() }
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            DownloadButton(isDownloading = isDownloading, isDownloaded = isDownloaded, progress = downloadProgress, onDownload = onDownload)
            IconButton(onClick = onAddToPlaylist, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.PlaylistAdd, contentDescription = "Aggiungi a playlist", tint = LevyraMuted, modifier = Modifier.size(24.dp))
            }
            IconButton(onClick = onFavorite, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Preferito",
                    tint = if (isFavorite) LevyraPink else LevyraMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ArtistHitRow(
    artists: List<ArtistHit>,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onClick: (ArtistHit) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = contentPadding
    ) {
        items(artists, key = { "artist-hit-${it.name}" }) { hit ->
            Column(
                modifier = Modifier.width(104.dp).clickable { onClick(hit) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(hit.accentStart), Color(hit.accentEnd)))),
                    contentAlignment = Alignment.Center
                ) {
                    if (hit.thumbnailUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(hit.thumbnailUrl).crossfade(true).build(),
                            contentDescription = hit.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize().clip(CircleShape)
                        )
                    } else {
                        Icon(Icons.Rounded.Person, null, tint = LevyraText, modifier = Modifier.size(40.dp))
                    }
                }
                Text(hit.name, color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Artista", color = LevyraMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AlbumHitRow(albums: List<AlbumHit>, onClick: (AlbumHit) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(albums, key = { "album-hit-${it.title}-${it.artist}" }) { album ->
            Column(
                modifier = Modifier.width(150.dp).clickable { onClick(album) },
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(LevyraPanelSoft),
                    contentAlignment = Alignment.Center
                ) {
                    if (album.thumbnailUrl.isNotBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current).data(album.thumbnailUrl).crossfade(true).build(),
                            contentDescription = album.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        Icon(Icons.Rounded.Album, null, tint = LevyraMuted, modifier = Modifier.size(40.dp))
                    }
                }
                Text(album.title, color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(album.year, album.artist).filter { it.isNotBlank() }.joinToString(" · "),
                    color = LevyraMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    isDownloading: Boolean = false,
    isDownloaded: Boolean = false,
    downloadProgress: Int? = null,
    onDownload: (() -> Unit)? = null,
    onArtist: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isCurrent) {
                    cinematicGlassBrush(Color(track.accentStart), Color(track.accentEnd), 0.82f)
                } else {
                    SolidColor(Color.Transparent)
                }
            )
            .border(
                1.dp,
                if (isCurrent) LevyraCyan.copy(alpha = 0.30f) else Color.Transparent,
                RoundedCornerShape(18.dp)
            )
            .pressable(onClick = onClick)
            .padding(horizontal = if (isCurrent) 9.dp else 0.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box {
            CoverImage(track, Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)))
            if (isPlaying || isResolving) {
                Surface(color = Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp), modifier = Modifier.matchParentSize()) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isResolving) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = LevyraCyan)
                        else Icon(Icons.Rounded.Equalizer, null, tint = LevyraCyan, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(track.title, color = if (isCurrent) LevyraCyan else LevyraText, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    track.artist,
                    color = LevyraMuted,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (onArtist != null) Modifier.clickable { onArtist() } else Modifier
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            if (onDownload != null) {
                DownloadButton(isDownloading = isDownloading, isDownloaded = isDownloaded, progress = downloadProgress, onDownload = onDownload)
            }
            if (onAddToPlaylist != null) {
                IconButton(onClick = onAddToPlaylist, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.PlaylistAdd, contentDescription = "Aggiungi a playlist", tint = LevyraMuted, modifier = Modifier.size(24.dp))
                }
            }
            IconButton(onClick = onFavorite, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Preferito",
                    tint = if (isFavorite) LevyraPink else LevyraMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
            if (onRemove != null) {
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Rimuovi", tint = LevyraMuted, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

@Composable
private fun DownloadButton(isDownloading: Boolean, isDownloaded: Boolean, progress: Int? = null, onDownload: () -> Unit) {
    IconButton(onClick = { if (!isDownloading && !isDownloaded) onDownload() }) {
        when {
            isDownloading -> {
                val label = progress?.coerceIn(1, 99)?.let { "$it%" } ?: "..."
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(34.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp, color = LevyraCyan)
                    Text(label, color = LevyraCyan, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)
                }
            }
            isDownloaded -> Icon(Icons.Rounded.DownloadDone, contentDescription = "Scaricato", tint = LevyraCyan, modifier = Modifier.size(23.dp))
            else -> Icon(Icons.Rounded.Download, contentDescription = "Scarica", tint = LevyraMuted, modifier = Modifier.size(23.dp))
        }
    }
}

@Composable
private fun MiniPlayer(
    track: Track,
    isPlaying: Boolean,
    isResolving: Boolean,
    progress: Float,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    val accentStart = Color(track.accentStart)
    val accentEnd = Color(track.accentEnd)
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.075f)),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(18.dp, RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp), clip = false)
    ) {
        Column(
            modifier = Modifier.background(
                Brush.linearGradient(
                    listOf(
                        accentStart.copy(alpha = 0.18f),
                        Color(0xFF171717),
                        Color(0xFF101010),
                        accentEnd.copy(alpha = 0.12f)
                    )
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(start = 14.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .pressable(onClick = onOpen)
                ) {
                    CoverImage(track, Modifier.fillMaxSize())
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(12.dp))
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .pressable(pressedScale = 0.985f, onClick = onOpen),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = track.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = track.artist,
                        color = LevyraMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                MiniPlayerToggleButton(
                    isPlaying = isPlaying,
                    isResolving = isResolving,
                    onToggle = onToggle
                )
                PlayerRoundIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "Chiudi player",
                    size = 38.dp,
                    iconSize = 19.dp,
                    tint = Color.White.copy(alpha = 0.90f),
                    background = Color.White.copy(alpha = 0.070f),
                    borderColor = Color.White.copy(alpha = 0.085f),
                    onClick = onClose
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.055f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(LevyraCyan, LevyraViolet)))
                )
            }
        }
    }
}

@Composable
private fun MiniPlayerToggleButton(
    isPlaying: Boolean,
    isResolving: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(Brush.radialGradient(listOf(Color.White, Color(0xFF67E8FF).copy(alpha = 0.96f), Color(0xFFA78BFA).copy(alpha = 0.74f))), CircleShape)
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)), CircleShape)
            .pressable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        if (isResolving) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = LevyraBlack
            )
        } else {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pausa" else "Riproduci",
                tint = LevyraBlack,
                modifier = Modifier.size(25.dp)
            )
        }
    }
}

@Composable
private fun PlayerRoundIconButton(
    icon: ImageVector,
    contentDescription: String?,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    tint: Color = LevyraText,
    background: Color = Color.White.copy(alpha = 0.065f),
    borderColor: Color = Color.White.copy(alpha = 0.085f),
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(background, CircleShape)
            .border(BorderStroke(1.dp, borderColor), CircleShape)
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
private fun GradientPlayButton(isPlaying: Boolean, isResolving: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .background(Brush.linearGradient(listOf(LevyraCyan, LevyraViolet)), CircleShape)
            .pressable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isResolving) CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp, color = LevyraBlack)
        else Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = LevyraBlack, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun GradientButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Brush.linearGradient(listOf(LevyraCyan, LevyraViolet)), RoundedCornerShape(18.dp))
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = LevyraBlack, fontSize = 16.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun CircleIconButton(icon: ImageVector, tint: Color, background: Color, onClick: () -> Unit) {
    val resolvedBackground = if (LevyraIsLight && background.alpha < 0.2f) LevyraAdaptiveChip else background
    val resolvedBorder = if (LevyraIsLight && background.alpha < 0.2f) LevyraAdaptiveHairline else Color.Transparent
    Box(
        modifier = Modifier
            .size(46.dp)
            .background(resolvedBackground, CircleShape)
            .border(Dp.Hairline, resolvedBorder, CircleShape)
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun ExploreScreen(viewModel: LevyraViewModel, state: LevyraUiState) {
    val strings = LocalLevyraStrings.current
    LaunchedEffect(Unit) { viewModel.ensureExplore(strings) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 42.dp, bottom = 190.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            item {
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    SectionTitle(strings.exploreFresh)
                }
            }
            when {
                state.isExploreLoading -> item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = LevyraCyan, strokeWidth = 3.dp, modifier = Modifier.size(30.dp))
                    }
                }
                state.exploreTracks.isEmpty() -> item {
                    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                        EmptyState(strings.exploreEmpty)
                    }
                }
                else -> item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.exploreTracks, key = { "ex-track-${it.id}" }) { track ->
                            TrackGlassCard(
                                track = track,
                                isCurrent = track.id == state.currentTrack?.id,
                                isPlaying = state.isPlaying && track.id == state.currentTrack?.id,
                                isFavorite = track.id in state.favoriteIds,
                                onClick = { viewModel.playFrom(state.exploreTracks, track) },
                                onFavorite = { viewModel.toggleFavorite(track) },
                                onShare = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, track.title)
                                        putExtra(Intent.EXTRA_TEXT, "${track.title} - ${track.artist}\n${track.streamUrl}")
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Condividi via"))
                                },
                                onAddToPlaylist = {}
                            )
                        }
                    }
                }
            }

            item {
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    SectionTitle(strings.exploreZones)
                }
            }
            items(ExploreCatalog.getZones(strings).chunked(2), key = { row -> "zone-${row.first().id}" }) { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { zone ->
                        ZoneCard(
                            zone = zone,
                            selected = zone.id == state.exploreZoneId,
                            onClick = { viewModel.selectExploreZone(zone) }
                        )
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }

            if (state.exploreVideos.isNotEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                        SectionTitle(strings.exploreNewVideos)
                    }
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.exploreVideos, key = { "ex-video-${it.id}" }) { track ->
                            VideoGlassCard(
                                track = track,
                                isCurrent = track.id == state.currentTrack?.id,
                                isPlaying = state.isPlaying && track.id == state.currentTrack?.id,
                                onClick = { viewModel.playFrom(state.exploreVideos, track) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.ZoneCard(zone: ExploreZone, selected: Boolean, onClick: () -> Unit) {
    val start = Color(zone.accentStart)
    Row(
        modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .clip(CircleShape)
            .background(if (selected) Color(0xFF1F1F24) else Color(0xFF0F0F12))
            .border(
                BorderStroke(Dp.Hairline, if (selected) start.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.15f)),
                CircleShape
            )
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(start = 16.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(start)
        )
        Text(
            zone.label,
            modifier = Modifier.padding(start = 10.dp, end = 16.dp),
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TrackGlassCard(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    onAddToPlaylist: () -> Unit
) {
    val scale by animateFloatAsState(if (isCurrent) 1.02f else 1f, label = "scale")
    var menuExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .width(140.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(Dp.Hairline, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(track.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (isCurrent) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlaying) {
                        Icon(Icons.Rounded.Pause, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    } else {
                        Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "More", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(CinematicGlass)
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isFavorite) "Rimuovi dai preferiti" else "Aggiungi a preferiti", color = Color.White) },
                        onClick = {
                            onFavorite()
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Condividi", color = Color.White) },
                        onClick = {
                            onShare()
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Aggiungi a playlist", color = Color.White) },
                        onClick = {
                            onAddToPlaylist()
                            menuExpanded = false
                        }
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                track.title,
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                track.artist,
                color = LevyraMuted.copy(alpha = 0.76f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun VideoGlassCard(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (isCurrent) 1.02f else 1f, label = "scale")
    Column(
        modifier = Modifier
            .width(280.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(6.dp))
                .border(Dp.Hairline, Color.White.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(track.largeThumbnailUrl.ifEmpty { track.thumbnailUrl })
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (isCurrent) 0.4f else 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrent && isPlaying) {
                    Icon(Icons.Rounded.Pause, null, tint = Color.White, modifier = Modifier.size(42.dp))
                } else {
                    Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(42.dp))
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                track.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                track.artist,
                color = LevyraMuted.copy(alpha = 0.76f),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BottomTabs(selected: LevyraTab, flatTop: Boolean, onSelect: (LevyraTab) -> Unit) {
    val strings = LocalLevyraStrings.current
    val surfaceColor = if (LevyraIsLight) Color.White.copy(alpha = 0.94f) else Color(0xF208080B)
    val linePrimary = if (LevyraIsLight) Color(0x1F11131F) else Color.White.copy(alpha = 0.08f)
    val lineSecondary = if (LevyraIsLight) Color.White.copy(alpha = 0.62f) else Color.White.copy(alpha = 0.03f)
    Surface(
        color = surfaceColor,
        shape = RoundedCornerShape(0.dp),
        shadowElevation = if (LevyraIsLight) 8.dp else 12.dp,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = linePrimary,
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = lineSecondary,
                    start = androidx.compose.ui.geometry.Offset(0f, 1.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(size.width, 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(start = 10.dp, end = 10.dp, top = 9.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TabButton(Icons.Rounded.Home, strings.home, selected == LevyraTab.Home) { onSelect(LevyraTab.Home) }
                TabButton(Icons.Rounded.Search, strings.search, selected == LevyraTab.Search) { onSelect(LevyraTab.Search) }
                TabButton(Icons.Rounded.Explore, strings.explore, selected == LevyraTab.Explore) { onSelect(LevyraTab.Explore) }
                TabButton(Icons.Rounded.LibraryMusic, strings.library, selected == LevyraTab.Library) { onSelect(LevyraTab.Library) }
                TabButton(Icons.Rounded.Album, strings.player, selected == LevyraTab.Player) { onSelect(LevyraTab.Player) }
            }
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}


@Composable
private fun PageHeader(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cinematicGlassBrush(intensity = 0.62f), RoundedCornerShape(28.dp))
            .border(1.dp, LevyraAdaptiveHairline, RoundedCornerShape(28.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            title,
            color = LevyraText,
            fontSize = 34.sp,
            lineHeight = 37.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1).sp,
            style = TextStyle(brush = cinematicTextBrush())
        )
        Text(subtitle, color = LevyraMuted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 19.sp)
    }
}


@Composable
private fun QuickChips(languageCode: String, onClick: (String) -> Unit) {
    val chips = LevyraContentLocales.quickSearches(languageCode)
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        chips.forEach { chip ->
            Surface(
                color = CinematicGlass.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                shape = CircleShape,
                modifier = Modifier.pressable(onClick = { onClick(chip) })
            ) {
                Text(chip, color = LevyraText, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp))
            }
        }
    }
}

@Composable
private fun SearchSummary(state: LevyraUiState) {
    when {
        state.isSearching -> GlassMessage("Sto cercando su YouTube Music…", LevyraCyan)
        state.searchError != null -> GlassMessage(state.searchError, LevyraOrange)
        state.searchResults.isNotEmpty() -> GlassMessage("${state.searchResults.size} risultati", LevyraCyan)
        else -> GlassMessage("Scrivi il nome di un brano e cerca", LevyraMuted)
    }
}

@Composable
private fun StatusBlock(state: LevyraUiState) {
    if (state.searchError != null || state.playerError != null) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.searchError?.let { GlassMessage(it, LevyraOrange) }
            state.playerError?.let { GlassMessage(it, LevyraOrange) }
        }
    }
}

@Composable
private fun PlayerError(error: String?) {
    if (error != null) GlassMessage(error, LevyraOrange)
}

@Composable
private fun SearchLoadingSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shimmer()
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(4) {
            LoadingLine(height = 72.dp, radius = 18.dp)
        }
    }
}

@Composable
private fun ChartLoadingSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shimmer(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(2) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(4) { LoadingLine(height = 56.dp, radius = 16.dp) }
            }
        }
    }
}

@Composable
private fun LevyraPictureInPictureSurface(state: LevyraUiState) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        LevyraVideoSurface(
            state = state,
            modifier = Modifier.fillMaxSize(),
            pictureInPicture = true
        )
    }
}

@Composable
private fun LevyraVideoSurface(
    state: LevyraUiState,
    modifier: Modifier,
    pictureInPicture: Boolean = false
) {
    val player = PlaybackService.activePlayer
    var aspectRatio by remember(player, state.currentTrack?.id) {
        mutableFloatStateOf(
            player?.videoSize?.let { size ->
                if (size.width > 0 && size.height > 0) size.width.toFloat() / size.height.toFloat() else 16f / 9f
            } ?: 16f / 9f
        )
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    aspectRatio = (videoSize.width.toFloat() / videoSize.height.toFloat()).coerceIn(0.42f, 2.39f)
                }
            }
        }
        player?.addListener(listener)
        onDispose { player?.removeListener(listener) }
    }
    LaunchedEffect(state.isVideoMode, state.isPlaying, aspectRatio) {
        LevyraPipBridge.updatePlayback(
            videoMode = state.isVideoMode,
            playing = state.isPlaying,
            aspectRatio = aspectRatio
        )
    }
    val surfaceModifier = if (pictureInPicture) {
        modifier
    } else {
        modifier.aspectRatio(aspectRatio.coerceIn(0.56f, 2.1f))
    }
    Box(
        modifier = surfaceModifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (player != null) {
            AndroidView(
                factory = { context ->
                    androidx.media3.ui.PlayerView(context).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_ALWAYS)
                        setBackgroundColor(android.graphics.Color.BLACK)
                        keepScreenOn = true
                        this.player = player
                    }
                },
                update = { view ->
                    val active = PlaybackService.activePlayer
                    if (view.player !== active) view.player = active
                    view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    view.keepScreenOn = state.isPlaying
                },
                onRelease = { view ->
                    view.player = null
                    view.keepScreenOn = false
                },
                modifier = Modifier.fillMaxSize()
            )
        } else if (!pictureInPicture) {
            VideoLoadingSkeleton()
        }
    }
}

@Composable
private fun VideoLoadingSkeleton() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shimmer()
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.08f),
                        LevyraCyan.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = LevyraCyan, strokeWidth = 3.dp)
    }
}

@Composable
private fun LoadingLine(height: androidx.compose.ui.unit.Dp, radius: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(radius))
            .background(cinematicGlassBrush(intensity = 0.54f))
    )
}

@Composable
private fun GlassMessage(text: String, color: Color) {
    Surface(
        color = CinematicGlass.copy(alpha = 0.70f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.30f)),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(15.dp))
    }
}






@Composable
private fun AudioQualityPanel(
    selected: String,
    volumePercent: Int,
    audioSettings: LevyraAudioSettings,
    onSelect: (String) -> Unit,
    onEqualizerEnabled: (Boolean) -> Unit,
    onPreset: (String) -> Unit,
    onBassBoost: (Int) -> Unit,
    onVirtualizer: (Int) -> Unit,
    onCrossfade: (Int) -> Unit,
    onDjSoft: (Boolean) -> Unit,
    onReplayGain: (Boolean) -> Unit,
    onTempo: (Float) -> Unit,
    onPitch: (Float) -> Unit,
    onGapless: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val blocker = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.58f))
            .clickable(interactionSource = blocker, indication = null) { onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = Color(0xFF11131C),
            shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .navigationBarsPadding()
                .clickable(interactionSource = blocker, indication = null) {}
        ) {
            LazyColumn(
                contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(56.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(alpha = 0.38f))
                        )
                    }
                }
                item {
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF667AA9).copy(alpha = 0.95f),
                                        Color(0xFF536890).copy(alpha = 0.98f),
                                        LevyraViolet.copy(alpha = 0.72f)
                                    )
                                )
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 18.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color.White.copy(alpha = 0.10f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Equalizer, null, tint = LevyraText.copy(alpha = 0.9f), modifier = Modifier.size(30.dp))
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(strings.audioEngine, color = LevyraText, fontSize = 22.sp, fontWeight = FontWeight.Black)
                                    Text(strings.audioEngineSubtitle, color = LevyraText.copy(alpha = 0.72f), fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Rounded.GraphicEq, null, tint = LevyraText, modifier = Modifier.size(17.dp))
                                    Text("$volumePercent%", color = LevyraText, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(strings.audioQuality, color = LevyraText, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                strings.audioQualityAuto to "Auto",
                                strings.audioQualityHigh to "High",
                                strings.audioQualityLow to "Low"
                            ).forEach { (label, quality) ->
                                AudioQualityChoice(
                                    label = label,
                                    selected = selected.equals(quality, ignoreCase = true),
                                    modifier = Modifier.weight(1f),
                                    onClick = { onSelect(quality) }
                                )
                            }
                        }
                    }
                }
                item {
                    PremiumToggleRow(
                        title = strings.equalizer,
                        subtitle = strings.equalizerSubtitle,
                        checked = audioSettings.equalizerEnabled,
                        onChecked = onEqualizerEnabled
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(strings.preset, color = LevyraText, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 4.dp)) {
                            items(LevyraAudioPresets.presets, key = { it.id }) { preset ->
                                PremiumPresetChip(
                                    label = preset.label,
                                    selected = audioSettings.presetId == preset.id,
                                    onClick = { onPreset(preset.id) }
                                )
                            }
                        }
                    }
                }
                item { PremiumSliderRow(strings.bassBoost, "${audioSettings.bassBoost}%", audioSettings.bassBoost.toFloat(), 0f..100f, 0) { onBassBoost(it.toInt()) } }
                item { PremiumSliderRow(strings.virtualizer, "${audioSettings.virtualizer}%", audioSettings.virtualizer.toFloat(), 0f..100f, 0) { onVirtualizer(it.toInt()) } }
                item { PremiumSliderRow(strings.crossfade, "${audioSettings.crossfadeSeconds}s", audioSettings.crossfadeSeconds.toFloat(), 0f..12f, 11) { onCrossfade(it.toInt()) } }
                item { PremiumToggleRow(strings.djSoft, "${strings.crossfade} ${audioSettings.crossfadeSeconds}s", audioSettings.djSoftMode, onDjSoft) }
                item { PremiumToggleRow(strings.replayGain, strings.volume, audioSettings.replayGainEnabled, onReplayGain) }
                item { PremiumSliderRow(strings.tempo, "${trimSpeed(audioSettings.playbackSpeed)}x", audioSettings.playbackSpeed, 0.5f..2.0f, 14) { onTempo((it * 100f).toInt() / 100f) } }
                item { PremiumSliderRow(strings.pitch, "${trimSpeed(audioSettings.pitch)}x", audioSettings.pitch, 0.5f..2.0f, 14) { onPitch((it * 100f).toInt() / 100f) } }
                item { PremiumToggleRow(strings.gapless, strings.gapless, audioSettings.gaplessEnabled, onGapless) }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Surface(
                            color = Color(0xFFB8C8F4),
                            shape = RoundedCornerShape(22.dp),
                            modifier = Modifier
                                .width(132.dp)
                                .height(58.dp)
                                .pressable(onClick = onClose)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(strings.done, color = Color(0xFF263049), fontSize = 16.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumToggleRow(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Surface(
        color = Color.White.copy(alpha = 0.045f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = LevyraText, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Switch(
                checked = checked,
                onCheckedChange = onChecked,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = LevyraText,
                    checkedTrackColor = LevyraCyan.copy(alpha = 0.55f),
                    uncheckedThumbColor = LevyraMuted,
                    uncheckedTrackColor = Color.White.copy(alpha = 0.08f)
                )
            )
        }
    }
}

@Composable
private fun PremiumSliderRow(title: String, valueLabel: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int, onValue: (Float) -> Unit) {
    Surface(
        color = Color.White.copy(alpha = 0.045f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = LevyraText, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Surface(color = LevyraCyan.copy(alpha = 0.13f), shape = RoundedCornerShape(999.dp)) {
                    Text(valueLabel, color = LevyraCyan, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                }
            }
            Slider(
                value = value.coerceIn(range.start, range.endInclusive),
                onValueChange = onValue,
                valueRange = range,
                steps = steps,
                colors = SliderDefaults.colors(
                    thumbColor = LevyraText,
                    activeTrackColor = LevyraCyan,
                    inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                )
            )
        }
    }
}

@Composable
private fun PremiumPresetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) LevyraCyan.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.055f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, if (selected) LevyraCyan.copy(alpha = 0.42f) else Color.White.copy(alpha = 0.08f)),
        modifier = Modifier
            .height(42.dp)
            .pressable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
            Text(label, color = if (selected) LevyraText else LevyraMuted, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun AudioQualityChoice(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        color = if (selected) Color(0xFFB8C8F4) else Color.White.copy(alpha = 0.035f),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, if (selected) Color.Transparent else Color.White.copy(alpha = 0.05f)),
        modifier = modifier
            .height(64.dp)
            .pressable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (selected) Color(0xFF263049) else LevyraMuted,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun LyricsButton(loading: Boolean, available: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(LevyraViolet.copy(alpha = 0.55f), LevyraCyan.copy(alpha = 0.45f), LevyraPink.copy(alpha = 0.5f))
                )
            )
            .pressable(enabled = !loading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Icon(Icons.Rounded.MusicNote, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Text(
                text = when {
                    loading -> "Cerco il testo…"
                    available -> "Mostra il testo"
                    else -> "Testo non disponibile"
                },
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

private fun greeting(userName: String): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val name = if (userName.isNotBlank()) " $userName" else ""
    return when (hour) {
        in 5..11 -> "Buongiorno$name ☀️"
        in 12..17 -> "Buon pomeriggio$name 🎶"
        in 18..22 -> "Buonasera$name 🌙"
        else -> "Buonanotte$name 🌌"
    }
}

private fun progressOf(positionMs: Long, durationMs: Long): Float {
    if (durationMs <= 0L) return 0f
    return (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "--:--"
    val totalSeconds = ms / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private val LevyraUiState.offlineExportMessageCompat: String?
    get() = null

private val LevyraUiState.isOfflineExportingCompat: Boolean
    get() = false

private val LevyraUiState.embeddedMetadataWriterReadyCompat: Boolean
    get() = false

private val LevyraUiState.offlineExportMessage: String?
    get() = null

private val LevyraUiState.isOfflineExporting: Boolean
    get() = false

private val LevyraUiState.embeddedMetadataWriterReady: Boolean
    get() = false

private fun LevyraViewModel.clearOfflineExportMessage() = Unit

private fun LevyraViewModel.addToQueue(track: Track) {
    play(track)
}

private fun LevyraViewModel.exportTrack(track: Track) {
    play(track)
}

private fun LevyraViewModel.exportCurrentTrack() {
    selectTab(LevyraTab.Player)
}


