@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
package com.luc4n3x.levyra.ui

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import android.app.Activity
import android.media.AudioManager
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.material.icons.rounded.Explicit
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.automirrored.rounded.Subject
import androidx.compose.material.icons.rounded.ViewCompact
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
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.core.view.WindowCompat
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.luc4n3x.levyra.data.ArtworkRequestSource
import com.luc4n3x.levyra.data.HomeLoadingPolicy
import com.luc4n3x.levyra.data.LevyraArtworkCache
import com.luc4n3x.levyra.data.LevyraArtworkStartupMetrics
import com.luc4n3x.levyra.data.albumRecommendationDeduplicationKey
import com.luc4n3x.levyra.player.LevyraPipBridge
import com.luc4n3x.levyra.player.PlaybackService
import com.luc4n3x.levyra.domain.AppUpdateInfo
import com.luc4n3x.levyra.domain.ArtistBiography
import com.luc4n3x.levyra.domain.artistBiographyEditorial
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
import com.luc4n3x.levyra.domain.LevyraDownloadFolderMode
import com.luc4n3x.levyra.domain.LevyraDownloadPreset
import com.luc4n3x.levyra.domain.LevyraDownloadSettings
import com.luc4n3x.levyra.domain.LevyraInterfaceSettings
import com.luc4n3x.levyra.domain.OfflineDownloadTask
import com.luc4n3x.levyra.domain.LevyraAudioPresets
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.ListeningPulse
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricSection
import com.luc4n3x.levyra.domain.LyricSectionType
import com.luc4n3x.levyra.domain.LyricVocalRole
import com.luc4n3x.levyra.domain.PulseArtist
import com.luc4n3x.levyra.domain.ExploreCatalog
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.Mood
import com.luc4n3x.levyra.domain.MoodEngine
import com.luc4n3x.levyra.domain.ReleaseRadarEntry
import com.luc4n3x.levyra.domain.Taste
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.YoutubeComment
import com.luc4n3x.levyra.domain.YoutubeCommentsState
import com.luc4n3x.levyra.domain.YoutubeEngagementState
import com.luc4n3x.levyra.LevyraLaunchActions
import com.luc4n3x.levyra.feature.sharedmedia.SharedMediaPreview
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
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

import com.luc4n3x.levyra.ui.theme.glassmorphism
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.library.LevyraLibraryScreen
import com.luc4n3x.levyra.ui.library.LevyraPlaylistDetailScreen
import com.luc4n3x.levyra.viewmodel.ExploreViewModel
import com.luc4n3x.levyra.viewmodel.HomeRenderSnapshot
import com.luc4n3x.levyra.viewmodel.HomeViewModel
import com.luc4n3x.levyra.viewmodel.LevyraScreenViewModelFactory
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LevyraViewModel
import com.luc4n3x.levyra.viewmodel.LibraryViewModel
import com.luc4n3x.levyra.viewmodel.PlayerViewModel
import com.luc4n3x.levyra.viewmodel.SearchViewModel
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.io.File
import java.time.format.TextStyle as DayTextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val LocalAnimationsEnabled = compositionLocalOf { true }
private val CinematicPlum = Color(0xFF2A1738)
private val CinematicGold = Color(0xFFFFC46B)
private val CinematicGlass = Color(0xFF151321)
private val CinematicGlassDeep = Color(0xFF0B0A14)
private val CinematicHairline = Color.White.copy(alpha = 0.105f)
private val HomeHorizontalInset = 18.dp
private val HomeHorizontalShelfEndPadding = 30.dp
private const val HOME_ARTIST_SHELF_SIZE = 13
private val LevyraNavigationBlue = Color(0xFF0A84FF)
private val LevyraNavigationBlueDeep = Color(0xFF0066E6)
private val LevyraSignalNodes = listOf(
    0.08f to 0.16f,
    0.20f to 0.09f,
    0.34f to 0.20f,
    0.52f to 0.12f,
    0.69f to 0.23f,
    0.86f to 0.14f,
    0.14f to 0.38f,
    0.42f to 0.33f,
    0.76f to 0.41f,
    0.93f to 0.31f,
    0.27f to 0.58f,
    0.61f to 0.53f,
    0.84f to 0.66f
)

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
    val selectedProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "tab-selected-progress"
    )
    val pillWidth by animateDpAsState(
        targetValue = if (selected) 56.dp else 34.dp,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "tab-pill-width"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (pressed) 0.86f else if (selected) 1.04f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium),
        label = "tab-icon-scale"
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) Color.White else if (LevyraIsLight) LevyraMuted else Color(0xFF85858D),
        animationSpec = tween(220),
        label = "tab-icon-tint"
    )
    val labelTint by animateColorAsState(
        targetValue = when {
            selected && LevyraIsLight -> LevyraNavigationBlueDeep
            selected -> LevyraNavigationBlue
            LevyraIsLight -> LevyraMuted
            else -> Color(0xFF8B8B94)
        },
        animationSpec = tween(220),
        label = "tab-label-tint"
    )
    val pillShape = RoundedCornerShape(18.dp)

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .pressable(
                interactionSource = interactionSource,
                pressedScale = 0.96f,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(pillWidth)
                    .height(34.dp)
                    .clip(pillShape)
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                LevyraNavigationBlue.copy(alpha = 0.92f * selectedProgress),
                                LevyraNavigationBlueDeep.copy(alpha = 0.78f * selectedProgress)
                            )
                        ),
                        shape = pillShape
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFF79BDFF).copy(alpha = 0.48f * selectedProgress),
                        shape = pillShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selectedProgress > 0.01f) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer { alpha = selectedProgress }
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.20f),
                                        Color.Transparent
                                    )
                                ),
                                pillShape
                            )
                    )
                }
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier
                        .size(23.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                )
            }
            Text(
                text = label,
                color = labelTint,
                fontSize = 11.sp,
                lineHeight = 12.sp,
                letterSpacing = 0.05.sp,
                fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .width(18.dp * selectedProgress)
                    .height(2.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(LevyraNavigationBlue.copy(alpha = selectedProgress))
            )
        }
    }
}
@Composable
private fun rememberEqualizerBar(
    isPlaying: Boolean,
    idleValue: Float,
    minimumValue: Float,
    maximumValue: Float,
    durationMillis: Int
): Animatable<Float, AnimationVector1D> {
    val animationsEnabled = LocalAnimationsEnabled.current
    val bar = remember { Animatable(idleValue) }
    LaunchedEffect(animationsEnabled, isPlaying, idleValue, minimumValue, maximumValue, durationMillis) {
        if (!animationsEnabled || !isPlaying) {
            bar.snapTo(idleValue)
            return@LaunchedEffect
        }
        bar.snapTo(minimumValue)
        while (true) {
            bar.animateTo(
                targetValue = maximumValue,
                animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing)
            )
            bar.animateTo(
                targetValue = minimumValue,
                animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing)
            )
        }
    }
    return bar
}

@Composable
private fun ActiveTrackEqualizer(
    modifier: Modifier = Modifier,
    color: Color = LevyraCyan,
    isPlaying: Boolean = true,
    width: Dp = 18.dp,
    height: Dp = 14.dp
) {
    val bar1 = rememberEqualizerBar(isPlaying, 0.4f, 0.2f, 1f, 550)
    val bar2 = rememberEqualizerBar(isPlaying, 0.6f, 0.3f, 0.9f, 380)
    val bar3 = rememberEqualizerBar(isPlaying, 0.3f, 0.15f, 0.95f, 460)
    val bar4 = rememberEqualizerBar(isPlaying, 0.5f, 0.25f, 0.85f, 620)
    val bars = remember(bar1, bar2, bar3, bar4) { listOf(bar1, bar2, bar3, bar4) }

    Canvas(modifier = modifier.size(width = width, height = height)) {
        val gap = 1.5.dp.toPx()
        val barWidth = (size.width - gap * (bars.size - 1)) / bars.size
        val cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx(), 1.dp.toPx())
        bars.forEachIndexed { index, bar ->
            val barHeight = size.height * bar.value.coerceIn(0f, 1f)
            drawRoundRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(index * (barWidth + gap), size.height - barHeight),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = cornerRadius
            )
        }
    }
}
@Composable
private fun SectionAccentBar(height: Dp = 22.dp, width: Dp = 4.dp) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .background(Brush.verticalGradient(listOf(LevyraCyan, LevyraViolet)), RoundedCornerShape(99.dp))
    )
}

@Composable
private fun HomePlayAllButton(onClick: () -> Unit, size: Dp = 36.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        LevyraCyan.copy(alpha = 0.15f),
                        LevyraViolet.copy(alpha = 0.11f)
                    )
                ),
                CircleShape
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        LevyraCyan.copy(alpha = 0.45f),
                        LevyraViolet.copy(alpha = 0.32f)
                    )
                ),
                CircleShape
            )
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = LocalLevyraStrings.current.play,
            tint = LevyraCyan,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        SectionAccentBar()
        Text(title, color = LevyraText, fontSize = 21.sp, lineHeight = 23.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.55).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
@Composable
private fun CoverImage(track: Track, modifier: Modifier, highRes: Boolean = false, zoom: Float = 1f) {
    val context = LocalContext.current
    val models = remember(
        context,
        track.id,
        track.title,
        track.artist,
        track.videoUrl,
        track.thumbnailUrl,
        track.largeThumbnailUrl,
        highRes
    ) {
        LevyraArtworkCache.models(context, track, highRes)
    }
    var modelIndex by remember(models) { mutableStateOf(0) }
    val model = models.getOrNull(modelIndex)
    val artworkKey = remember(track.id, highRes) { "${track.id}:${if (highRes) "large" else "small"}" }
    val modelIdentity = remember(model) {
        when (model) {
            is File -> "file:${model.absolutePath}"
            null -> "missing"
            else -> "remote:${model}"
        }
    }
    val requestSource = remember(model) {
        when (model) {
            is File -> ArtworkRequestSource.PersistentFile
            null -> ArtworkRequestSource.Missing
            else -> ArtworkRequestSource.Remote
        }
    }
    if (BuildConfig.DEBUG) {
        LaunchedEffect(artworkKey, modelIdentity, requestSource) {
            LevyraArtworkStartupMetrics.recordArtworkRequest(artworkKey, modelIdentity, requestSource)
            if (model == null) LevyraArtworkStartupMetrics.recordArtworkLoading(artworkKey)
        }
    }
    val background = remember(track.accentStart, track.accentEnd) {
        Brush.linearGradient(listOf(Color(track.accentStart), Color(track.accentEnd)))
    }
    var artworkLoaded by remember(models) { mutableStateOf(false) }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (!artworkLoaded) {
            InstantArtworkPlaceholder(
                track = track,
                modifier = Modifier.fillMaxSize().background(background)
            )
        }
        if (model != null) {
            val crossfadeMs = if (LocalAnimationsEnabled.current && highRes && model !is File) 120 else 0
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
                onLoading = { LevyraArtworkStartupMetrics.recordArtworkLoading(artworkKey) },
                onSuccess = {
                    LevyraArtworkStartupMetrics.recordArtworkDisplayed(artworkKey)
                    artworkLoaded = true
                },
                onError = {
                    LevyraArtworkStartupMetrics.recordArtworkFailure(artworkKey)
                    if (modelIndex < models.lastIndex) modelIndex += 1
                },
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
    val animationsEnabled = LocalAnimationsEnabled.current
    val interaction = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && animationsEnabled) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "press"
    )
    return this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(
            interactionSource = interaction,
            indication = if (animationsEnabled) null else LocalIndication.current,
            enabled = enabled,
            onClick = onClick
        )
}

@Composable
fun LevyraApp(viewModel: LevyraViewModel, isInPictureInPicture: Boolean = false) {
    val screenViewModelFactory = remember(viewModel) { LevyraScreenViewModelFactory(viewModel) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentStrings = LevyraStrings.forCode(state.languageCode)
    val layoutDirection = if (LevyraLanguageCatalog.isRtl(currentStrings.code)) LayoutDirection.Rtl else LayoutDirection.Ltr
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
    val createBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let(viewModel::createBackup)
    }
    val restoreBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::restoreBackup)
    }
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
    val pendingSharedMedia by LevyraLaunchActions.pendingSharedMedia
    LaunchedEffect(pendingSharedMedia?.key) {
        pendingSharedMedia?.let { request ->
            viewModel.handleSharedMedia(request)
            LevyraLaunchActions.pendingSharedMedia.value = null
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
    LaunchedEffect(state.backupMessage) {
        state.backupMessage?.let { message ->
            Toast.makeText(toastContext, message, Toast.LENGTH_LONG).show()
            viewModel.clearBackupMessage()
        }
    }
    BackHandler(enabled = showLanguageRestartDialog || state.sharedMediaPreview != null || showDownloadsFolder || state.openPlaylist != null || state.showUpdatePrompt || state.showAlbum || state.showArtist || state.showQueue || state.showLyrics || state.showSettings || state.showAudioQualityPanel || state.selectedTab != LevyraTab.Home) {
        if (showLanguageRestartDialog) {
            showLanguageRestartDialog = false
        } else if (state.sharedMediaPreview != null) {
            viewModel.dismissSharedMedia()
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
        LocalLevyraStrings provides currentStrings,
        LocalLayoutDirection provides layoutDirection
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LevyraBlack)
        ) {
            LevyraBackground(accent?.accentStart, accent?.accentEnd)

            val homeListState = rememberLazyListState()

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
                        LevyraTab.Home -> {
                            val homeViewModel: HomeViewModel = composeViewModel(key = "levyra-home", factory = screenViewModelFactory)
                            val renderSnapshot by homeViewModel.renderState.collectAsStateWithLifecycle()
                            HomeScreen(homeViewModel, renderSnapshot, homeListState)
                        }
                        LevyraTab.Search -> {
                            val searchViewModel: SearchViewModel = composeViewModel(key = "levyra-search", factory = screenViewModelFactory)
                            val screenState by searchViewModel.state.collectAsStateWithLifecycle()
                            SearchScreen(searchViewModel, screenState)
                        }
                        LevyraTab.Explore -> {
                            val exploreViewModel: ExploreViewModel = composeViewModel(key = "levyra-explore", factory = screenViewModelFactory)
                            val screenState by exploreViewModel.state.collectAsStateWithLifecycle()
                            ExploreScreen(exploreViewModel, screenState)
                        }
                        LevyraTab.Library -> {
                            val libraryViewModel: LibraryViewModel = composeViewModel(key = "levyra-library", factory = screenViewModelFactory)
                            val screenState by libraryViewModel.state.collectAsStateWithLifecycle()
                            LevyraLibraryScreen(libraryViewModel, screenState, onOpenDownloads = { showDownloadsFolder = true })
                        }
                        LevyraTab.Player -> {
                            val playerViewModel: PlayerViewModel = composeViewModel(key = "levyra-player", factory = screenViewModelFactory)
                            val screenState by playerViewModel.state.collectAsStateWithLifecycle()
                            PlayerScreen(playerViewModel, screenState)
                        }
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
                DownloadProgressHud(state = state, onCancel = viewModel::cancelDownload)
            }

            AnimatedVisibility(visible = state.showOnboarding, enter = overlayEnter, exit = overlayExit) {
                if (state.showOnboarding) {
                    OnboardingOverlay(selectedLanguageCode = state.languageCode, onDone = viewModel::completeOnboarding)
                }
            }

            state.sharedMediaPreview?.let { preview ->
                SharedMediaPreviewDialog(
                    preview = preview,
                    languageCode = state.languageCode,
                    onPlay = viewModel::playSharedMedia,
                    onPlayNext = viewModel::playNextSharedMedia,
                    onQueue = viewModel::queueSharedMedia,
                    onDownload = viewModel::downloadSharedMedia,
                    onDismiss = viewModel::dismissSharedMedia
                )
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
                    interfaceSettings = state.interfaceSettings,
                    downloadSettings = state.downloadSettings,
                    downloadQueue = state.downloadQueue,
                    playbackDiagnostics = state.playbackDiagnostics,
                    onThemePreset = viewModel::setThemePreset,
                    onInterfaceSettings = viewModel::setInterfaceSettings,
                    onDownloadSettings = viewModel::setDownloadSettings,
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
                    onDownloadUpdate = { state.updateInfo?.let { openExternalUrl(toastContext, it.downloadUrl, currentStrings) } },
                    onCreateBackup = { createBackupLauncher.launch("levyra-backup-${System.currentTimeMillis()}.zip") },
                    onRestoreBackup = { restoreBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
                    onPauseDownload = viewModel::pauseDownload,
                    onResumeDownload = viewModel::resumeDownload,
                    onCancelDownload = viewModel::cancelDownload,
                    onShareDiagnostics = {
                        val diagnostics = viewModel.refreshPlaybackDiagnostics()
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "LEVYRA playback diagnostics")
                            putExtra(Intent.EXTRA_TEXT, diagnostics)
                        }
                        toastContext.startActivity(Intent.createChooser(intent, currentStrings.shareDiagnostics))
                    },
                    onRedoQuestionnaire = viewModel::restartOnboarding,
                    onClose = viewModel::closeSettings
                )
            }

            AnimatedVisibility(visible = state.showLyrics, enter = overlayEnter, exit = overlayExit) {
                LyricsOverlay(
                    state = state,
                    onTranslation = viewModel::setLyricsTranslationEnabled,
                    onSeekToMs = { positionMs ->
                        viewModel.seekTo(progressOf(positionMs, state.durationMs))
                    },
                    onClose = viewModel::closeLyrics
                )
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
                            openExternalUrl(toastContext, update.downloadUrl, currentStrings)
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
                    onToggleFollow = viewModel::toggleFollowArtist,
                    onOpenArtist = viewModel::openArtistFromHit,
                    onOpenRelease = viewModel::openArtistRelease,
                    onClose = viewModel::closeArtist
                )
            }

            AnimatedVisibility(visible = state.openPlaylist != null, enter = overlayEnter, exit = overlayExit) {
                LevyraPlaylistDetailScreen(viewModel = viewModel, state = state)
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

@Composable
private fun SharedMediaPreviewDialog(
    preview: SharedMediaPreview,
    languageCode: String,
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onQueue: () -> Unit,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    val italian = languageCode.equals("it", ignoreCase = true)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = LevyraInk.copy(alpha = 0.98f),
            shape = RoundedCornerShape(30.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (preview.thumbnailUrl.isNotBlank()) {
                        AsyncImage(
                            model = preview.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(78.dp)
                                .clip(RoundedCornerShape(18.dp))
                        )
                    } else {
                        EmptyCover(
                            Modifier
                                .size(78.dp)
                                .clip(RoundedCornerShape(18.dp))
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = if (italian) "Condiviso con Levyra" else "Shared with Levyra",
                            color = LevyraCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = preview.title,
                            color = LevyraText,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = preview.subtitle,
                            color = LevyraMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    CircleIconButton(
                        icon = Icons.Rounded.Close,
                        tint = LevyraText,
                        background = Color.White.copy(alpha = 0.08f),
                        onClick = onDismiss
                    )
                }
                when {
                    preview.loading -> {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp, color = LevyraCyan)
                        }
                    }
                    preview.error.isNotBlank() -> {
                        Text(
                            text = preview.error,
                            color = LevyraOrange,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    preview.playable -> {
                        Text(
                            text = if (italian) "${preview.tracks.size} ${if (preview.tracks.size == 1) "brano pronto" else "brani pronti"}" else "${preview.tracks.size} ${if (preview.tracks.size == 1) "track ready" else "tracks ready"}",
                            color = LevyraMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SharedMediaAction(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.PlayArrow,
                                title = if (italian) "Riproduci" else "Play",
                                primary = true,
                                onClick = onPlay
                            )
                            SharedMediaAction(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.Download,
                                title = if (italian) "Scarica" else "Download",
                                primary = false,
                                onClick = onDownload
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SharedMediaAction(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.SkipNext,
                                title = if (italian) "Dopo" else "Play next",
                                primary = false,
                                onClick = onPlayNext
                            )
                            SharedMediaAction(
                                modifier = Modifier.weight(1f),
                                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                                title = if (italian) "In coda" else "Queue",
                                primary = false,
                                onClick = onQueue
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedMediaAction(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    primary: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (primary) LevyraCyan else Color.White.copy(alpha = 0.08f),
        contentColor = if (primary) LevyraBlack else LevyraText,
        shape = RoundedCornerShape(16.dp),
        border = if (primary) null else BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
        modifier = modifier.pressable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

private data class DownloadHudItem(
    val taskKey: String,
    val progress: Int,
    val title: String,
    val count: Int
)

private fun LevyraUiState.activeDownloadHudItem(strings: LevyraStrings): DownloadHudItem? {
    val ids = downloadingTrackIds.toList()
    if (ids.isEmpty()) return null
    val primaryId = ids.maxByOrNull { downloadProgressByTrackId[it] ?: 1 } ?: return null
    val progress = (downloadProgressByTrackId[primaryId] ?: 1).coerceIn(1, 99)
    val rawTitle = downloadTitleByTrackId[primaryId].orEmpty().ifBlank { strings.song }
    val title = if (ids.size > 1) "$rawTitle +${ids.size - 1}" else rawTitle
    return DownloadHudItem(taskKey = primaryId, progress = progress, title = title, count = ids.size)
}

private fun downloadHudBottomPadding(state: LevyraUiState): Dp {
    return when {
        state.selectedTab == LevyraTab.Player -> 24.dp
        state.currentTrack != null -> 154.dp
        else -> 96.dp
    }
}

@Composable
private fun DownloadProgressHud(state: LevyraUiState, onCancel: (String) -> Unit) {
    val item = state.activeDownloadHudItem(LocalLevyraStrings.current) ?: return
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
                    text = if (item.count > 1) LocalLevyraStrings.current.downloadsInProgress else LocalLevyraStrings.current.downloadInProgress,
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
            IconButton(
                onClick = { onCancel(item.taskKey) },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = LocalLevyraStrings.current.cancelDownload,
                    tint = LevyraMuted,
                    modifier = Modifier.size(21.dp)
                )
            }
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
    val strings = LocalLevyraStrings.current
    val listState = rememberLazyListState()
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
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.back, tint = LevyraText)
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        color = Color.White.copy(alpha = 0.06f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp).pressable(onClick = {
                            val query = listOf(album?.title.orEmpty(), album?.artist.orEmpty()).filter { it.isNotBlank() }.joinToString(" ")
                            if (query.isNotBlank()) openExternalUrl(context, "https://music.youtube.com/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}", strings)
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
                        GlassMessage(strings.albumUnavailable, LevyraOrange)
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
                                context.startActivity(Intent.createChooser(intent, strings.share))
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
                                Text(strings.formatTrackCount(tracks.size), color = LevyraText, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp)
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
                            GlassMessage(state.albumError ?: strings.albumTracksUnavailable, LevyraOrange)
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
            AlbumSecondaryAction(icon = Icons.Rounded.Download, label = LocalLevyraStrings.current.offline, enabled = trackCount > 0, modifier = Modifier.weight(1f), onClick = onDownload)
            AlbumSecondaryAction(icon = Icons.Rounded.Person, label = LocalLevyraStrings.current.artistLabel, enabled = album.artist.isNotBlank(), modifier = Modifier.weight(1f), onClick = onOpenArtist)
            AlbumSecondaryAction(icon = Icons.Rounded.Share, label = LocalLevyraStrings.current.share, enabled = true, modifier = Modifier.weight(1f), onClick = onShare)
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
                    if (descriptionExpanded) LocalLevyraStrings.current.showLess else LocalLevyraStrings.current.readAll,
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
    val isAppleStyle = LevyraActivePalette.id == com.luc4n3x.levyra.ui.theme.LevyraThemes.APPLE_MUSIC
    val cornerRadius = if (isAppleStyle) 12.dp else 16.dp
    val buttonShape = RoundedCornerShape(cornerRadius)
    val safeGradient = remember(accentStart, accentEnd) {
        playerContrastGradient(accentStart, accentEnd, PlayerMinimumContrast)
    }
    val enabledContent = safeGradient.content
    val disabledContent = LevyraMuted
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(if (enabled) 16.dp else 0.dp, buttonShape, clip = false, spotColor = safeGradient.start.copy(alpha = 0.6f))
            .clip(buttonShape)
            .background(
                if (enabled) Brush.horizontalGradient(listOf(safeGradient.start, safeGradient.end))
                else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.08f)))
            )
            .pressable(enabled = enabled && !isResolving, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (isResolving) {
                CircularProgressIndicator(
                    color = if (enabled) enabledContent else disabledContent,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Icon(
                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    null,
                    tint = if (enabled) enabledContent else disabledContent,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                if (isPlaying) LocalLevyraStrings.current.playing else LocalLevyraStrings.current.play,
                color = if (enabled) enabledContent else disabledContent,
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
    val isAppleStyle = LevyraActivePalette.id == com.luc4n3x.levyra.ui.theme.LevyraThemes.APPLE_MUSIC
    val cornerRadius = if (isAppleStyle) 12.dp else 14.dp
    val buttonShape = RoundedCornerShape(cornerRadius)
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(buttonShape)
            .background(Color.White.copy(alpha = if (enabled) 0.06f else 0.03f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = if (enabled) 0.08f else 0.04f)), buttonShape)
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
                    buttonColor = if (LevyraIsLight) LevyraBlack else Color.White,
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
    val strings = LocalLevyraStrings.current
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
                    isDownloaded -> strings.offline
                    isDownloading -> strings.formatDownloadProgress(downloadProgress ?: 1)
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
                    DropdownMenuItem(text = { Text(if (isFavorite) strings.removeFromFavorites else strings.addToFavorites) }, leadingIcon = { Icon(if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null) }, onClick = { expanded = false; onFavorite() })
                    DropdownMenuItem(text = { Text(strings.addToPlaylist) }, leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) }, onClick = { expanded = false; onAddToPlaylist() })
                    DropdownMenuItem(text = { Text(if (isDownloaded) strings.alreadyOffline else strings.download) }, leadingIcon = { Icon(if (isDownloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download, null) }, onClick = { expanded = false; if (!isDownloaded) onDownload() })
                    DropdownMenuItem(text = { Text(strings.openArtist) }, leadingIcon = { Icon(Icons.Rounded.Person, null) }, onClick = { expanded = false; onArtist() })
                    DropdownMenuItem(text = { Text(strings.share) }, leadingIcon = { Icon(Icons.Rounded.Share, null) }, onClick = {
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
                        context.startActivity(Intent.createChooser(intent, strings.shareSong))
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
    val strings = LocalLevyraStrings.current
    val showArtistError = shouldShowArtistError(
        hasError = state.artistError != null,
        hasProfile = profile != null
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LevyraBlack)
            .clickable(interactionSource = blocker, indication = null) {}
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (state.currentTrack != null) 200.dp else 110.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            when {
                state.artistLoading && profile == null -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlayerRoundIconButton(
                                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = strings.back,
                                background = Color.White.copy(alpha = 0.07f),
                                onClick = onClose
                            )
                        }
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 150.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = LevyraCyan)
                        }
                    }
                }
                showArtistError -> {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(80.dp)
                        ) {
                            PlayerRoundIconButton(
                                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = strings.back,
                                background = Color.White.copy(alpha = 0.07f),
                                onClick = onClose
                            )
                            Text(strings.artistProfileUnavailable, color = LevyraMuted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                else -> {
                    val artist = requireNotNull(profile) { "Artist profile required outside loading and error states" }
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            ArtistHeader(
                                profile = artist,
                                isFollowed = isFollowed,
                                accentStart = accentStart,
                                accentEnd = accentEnd,
                                onPlay = artist.topSongs.firstOrNull()?.let { track -> { onPlay(track) } },
                                onToggleFollow = onToggleFollow,
                                onClose = onClose
                            )
                            if (artist.hasBio) {
                                ArtistBio(
                                    biography = requireNotNull(artist.biography),
                                    accentStart = accentStart,
                                    accentEnd = accentEnd,
                                    modifier = Modifier
                                        .padding(start = 14.dp, end = 14.dp, bottom = 6.dp)
                                        .offset(y = (-10).dp)
                                )
                            }
                        }
                    }
                    if (artist.topSongs.isNotEmpty()) {
                        item { Box(modifier = Modifier.padding(horizontal = 20.dp)) { ArtistSectionTitle(strings.popularTracks) } }
                        item {
                            ArtistPopularTracksShelf(
                                tracks = artist.topSongs,
                                currentId = state.currentTrack?.id,
                                isPlaying = state.isPlaying,
                                isResolving = state.isResolving,
                                onPlay = onPlay
                            )
                        }
                    }
                    if (artist.albums.isNotEmpty()) {
                        item { Box(modifier = Modifier.padding(horizontal = 20.dp)) { ArtistSectionTitle(strings.albumsPlain) } }
                        item { Box(modifier = Modifier.padding(start = 20.dp)) { ArtistReleaseRow(artist.albums, artist.name, onOpenRelease) } }
                    }
                    if (artist.singles.isNotEmpty()) {
                        item { Box(modifier = Modifier.padding(horizontal = 20.dp)) { ArtistSectionTitle(strings.singlesAndEps) } }
                        item { Box(modifier = Modifier.padding(start = 20.dp)) { ArtistReleaseRow(artist.singles, artist.name, onOpenRelease) } }
                    }
                    if (artist.videos.isNotEmpty()) {
                        item { Box(modifier = Modifier.padding(horizontal = 20.dp)) { ArtistSectionTitle(strings.video) } }
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(start = 20.dp, end = 20.dp)
                            ) {
                                items(artist.videos.take(20), key = { "artist-video-${it.id}" }) { track ->
                                    VideoGlassCard(
                                        track = track,
                                        isCurrent = track.id == state.currentTrack?.id,
                                        isPlaying = state.isPlaying && track.id == state.currentTrack?.id,
                                        onClick = { onPlay(track) }
                                    )
                                }
                            }
                        }
                    }
                    if (artist.relatedArtists.isNotEmpty()) {
                        item { Box(modifier = Modifier.padding(horizontal = 20.dp)) { ArtistSectionTitle(strings.similarArtists) } }
                        item { Box(modifier = Modifier.padding(start = 20.dp)) { ArtistHitRow(artist.relatedArtists, onClick = onOpenArtist) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistPopularTracksShelf(
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: (Track) -> Unit
) {
    val shelfTracks = remember(tracks) {
        tracks.distinctBy { it.id.ifBlank { "${it.artist}|${it.title}" } }.take(18)
    }
    val pages = remember(shelfTracks) { shelfTracks.chunked(6) }
    val pagerState = rememberPagerState(pageCount = { pages.size.coerceAtLeast(1) })

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            pageSpacing = 14.dp
        ) { pageIndex ->
            val pageTracks = pages.getOrElse(pageIndex) { emptyList() }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) { rowIndex ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Spacer(modifier = Modifier.fillMaxWidth().aspectRatio(1f))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Spacer(modifier = Modifier.height(34.dp))
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
                        label = "artist-popular-page-indicator-$index"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(5.dp)
                            .width(indicatorWidth)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index) {
                                    Brush.horizontalGradient(listOf(LevyraCyan, LevyraViolet))
                                } else {
                                    Brush.horizontalGradient(
                                        listOf(
                                            LevyraMuted.copy(alpha = 0.28f),
                                            LevyraMuted.copy(alpha = 0.18f)
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
private fun ArtistSectionTitle(title: String) {
    Text(
        text = title,
        color = LevyraText,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.55).sp
    )
}

@Composable
private fun ArtistFollowButton(isFollowed: Boolean, onClick: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val buttonShape = CircleShape
    Surface(
        color = if (isFollowed) Color.White.copy(alpha = 0.10f) else Color.White,
        shape = buttonShape,
        border = if (isFollowed) BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)) else null,
        modifier = Modifier.height(50.dp).pressable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isFollowed) Icons.Rounded.Check else Icons.Rounded.PersonAdd,
                contentDescription = null,
                tint = if (isFollowed) Color.White else Color.Black,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = if (isFollowed) strings.followingArtist else strings.followArtist,
                color = if (isFollowed) Color.White else Color.Black,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun ArtistPlayButton(onClick: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Surface(
        color = LevyraCyan,
        shape = CircleShape,
        modifier = Modifier.height(50.dp).pressable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = LevyraBlack, modifier = Modifier.size(23.dp))
            Text(strings.play, color = LevyraBlack, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ArtistHeader(
    profile: ArtistProfile,
    isFollowed: Boolean,
    accentStart: Color,
    accentEnd: Color,
    onPlay: (() -> Unit)?,
    onToggleFollow: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    Box(modifier = Modifier.fillMaxWidth().height(472.dp).background(Brush.linearGradient(listOf(accentStart, accentEnd)))) {
        val heroArtwork = profile.thumbnailUrl.ifBlank { profile.bannerUrl }
        if (heroArtwork.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(heroArtwork).crossfade(true).build(),
                contentDescription = profile.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        } else {
            Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.White.copy(alpha = 0.74f), modifier = Modifier.align(Alignment.Center).size(94.dp))
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.20f),
                            0.42f to Color.Transparent,
                            0.70f to Color.Black.copy(alpha = 0.56f),
                            1f to LevyraBlack
                        )
                    )
                )
        )
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerRoundIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = strings.back,
                tint = Color.White,
                background = Color.Black.copy(alpha = 0.34f),
                borderColor = Color.White.copy(alpha = 0.14f),
                onClick = onClose
            )
        }
        Column(
            modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    profile.name,
                    color = Color.White,
                    fontSize = 40.sp,
                    lineHeight = 43.sp,
                    letterSpacing = (-1.25).sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Icon(Icons.Rounded.Verified, contentDescription = null, tint = LevyraCyan, modifier = Modifier.size(24.dp))
            }
            if (profile.monthlyListeners.isNotBlank()) {
                Text(
                    text = profile.monthlyListeners,
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (profile.subscribers.isNotBlank()) {
                Text(
                    text = profile.subscribers,
                    color = Color.White.copy(alpha = 0.66f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onPlay != null) {
                    ArtistPlayButton(onClick = onPlay)
                }
                ArtistFollowButton(isFollowed = isFollowed, onClick = onToggleFollow)
            }
        }
    }
}

private fun normalizeBiographyPreview(
    text: String,
    paragraphs: List<String>,
    summary: String
): String {
    val source = text.takeIf { it.isNotBlank() }
        ?: paragraphs.joinToString(" ").takeIf { it.isNotBlank() }
        ?: summary
    return source.replace(Regex("""\s+"""), " ").trim()
}

private data class ArtistBiographyPresentation(
    val previewText: String,
    val dialogParagraphs: List<String>,
    val sourceText: String,
    val dialogSourceText: String,
    val showSource: Boolean
)

private data class ArtistBiographyAccent(
    val start: Color,
    val end: Color
)

private fun buildArtistBiographyPresentation(
    biography: ArtistBiography,
    paragraphs: List<String>,
    summary: String
): ArtistBiographyPresentation {
    val previewText = normalizeBiographyPreview(
        text = biography.text,
        paragraphs = paragraphs,
        summary = summary
    )
    val sourceLabel = biography.sourceLabel.trim()
    val wikipediaSource = sourceLabel.equals("Wikipedia", ignoreCase = true)
    return ArtistBiographyPresentation(
        previewText = previewText,
        dialogParagraphs = paragraphs.ifEmpty { listOf(previewText) },
        sourceText = if (wikipediaSource) "Wikipedia" else sourceLabel,
        dialogSourceText = if (wikipediaSource) "Wikipedia · CC BY-SA 4.0" else sourceLabel,
        showSource = sourceLabel.isNotBlank() && !sourceLabel.startsWith("YouTube", ignoreCase = true)
    )
}

@Composable
private fun ArtistBio(
    biography: ArtistBiography,
    accentStart: Color,
    accentEnd: Color,
    modifier: Modifier = Modifier
) {
    val editorial = remember(biography.text, biography.description, biography.languageCode) {
        artistBiographyEditorial(
            text = biography.text,
            description = biography.description,
            languageCode = biography.languageCode
        )
    }
    val presentation = remember(
        biography.text,
        biography.sourceLabel,
        editorial.paragraphs,
        editorial.summary
    ) {
        buildArtistBiographyPresentation(
            biography = biography,
            paragraphs = editorial.paragraphs,
            summary = editorial.summary
        )
    }
    var showFullBiography by rememberSaveable(biography.pageId, biography.languageCode, biography.text) {
        mutableStateOf(false)
    }
    var previewOverflow by remember(presentation.previewText) { mutableStateOf(false) }
    val showReadMore = previewOverflow ||
        presentation.dialogParagraphs.size > 1 ||
        presentation.previewText.length > 360

    ArtistBiographyCard(
        biography = biography,
        presentation = presentation,
        accent = ArtistBiographyAccent(accentStart, accentEnd),
        showReadMore = showReadMore,
        onPreviewOverflow = { previewOverflow = it },
        onOpen = { showFullBiography = true },
        modifier = modifier
    )

    if (showFullBiography) {
        ArtistBiographyDialog(
            biography = biography,
            paragraphs = presentation.dialogParagraphs,
            sourceText = presentation.dialogSourceText,
            onDismiss = { showFullBiography = false }
        )
    }
}

@Composable
private fun ArtistBiographyCard(
    biography: ArtistBiography,
    presentation: ArtistBiographyPresentation,
    accent: ArtistBiographyAccent,
    showReadMore: Boolean,
    onPreviewOverflow: (Boolean) -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(26.dp)
    val surfaceBrush = remember(accent) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF111216),
                accent.start.copy(alpha = 0.025f),
                Color(0xFF0A0B0E),
                accent.end.copy(alpha = 0.018f)
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, shape, clip = false)
            .background(surfaceBrush, shape)
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
            .clip(shape)
            .clickable(onClick = onOpen)
    ) {
        BiographyCardHighlight()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 17.dp, end = 20.dp, bottom = 15.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            BiographyCardHeader(
                biography = biography,
                sourceText = presentation.sourceText,
                showSource = presentation.showSource
            )
            BiographyCardPreview(
                text = presentation.previewText,
                onOverflowChange = onPreviewOverflow
            )
            if (showReadMore) {
                BiographyReadMore(onClick = onOpen)
            }
        }
    }
}

@Composable
private fun BiographyCardHighlight() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    )
                )
        )
    }
}

@Composable
private fun BiographyCardHeader(
    biography: ArtistBiography,
    sourceText: String,
    showSource: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = LocalLevyraStrings.current.biography,
            color = LevyraText.copy(alpha = 0.92f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.1.sp
        )
        if (showSource) {
            BiographySourceLink(
                sourceText = sourceText,
                sourceUrl = biography.sourceUrl
            )
        }
    }
}

@Composable
private fun BiographySourceLink(
    sourceText: String,
    sourceUrl: String
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(enabled = sourceUrl.isNotBlank()) {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(sourceUrl)))
                }
            }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = sourceText,
            color = LevyraMuted.copy(alpha = 0.72f),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (sourceUrl.isNotBlank()) {
            Icon(
                imageVector = Icons.Rounded.OpenInNew,
                contentDescription = sourceText,
                tint = LevyraMuted.copy(alpha = 0.66f),
                modifier = Modifier.size(11.dp)
            )
        }
    }
}

@Composable
private fun BiographyCardPreview(
    text: String,
    onOverflowChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "“",
            color = LevyraText.copy(alpha = 0.14f),
            fontSize = 54.sp,
            lineHeight = 44.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(30.dp)
        )
        Text(
            text = text,
            color = LevyraText.copy(alpha = 0.92f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 22.5.sp,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result -> onOverflowChange(result.hasVisualOverflow) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BiographyReadMore(onClick: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.075f))
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = strings.readAll,
            color = LevyraText.copy(alpha = 0.88f),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(Color.White.copy(alpha = 0.07f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = strings.readAll,
                tint = LevyraText.copy(alpha = 0.78f),
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
private fun BiographyDialogParagraph(
    paragraph: String,
    featured: Boolean
) {
    if (featured) {
        BiographyFeaturedParagraph(paragraph)
    } else {
        Text(
            text = paragraph,
            color = LevyraMuted.copy(alpha = 0.94f),
            fontSize = 15.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun BiographyFeaturedParagraph(paragraph: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "“",
            color = LevyraText.copy(alpha = 0.13f),
            fontSize = 50.sp,
            lineHeight = 42.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(28.dp)
        )
        Text(
            text = paragraph,
            color = LevyraText.copy(alpha = 0.95f),
            fontSize = 16.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ArtistBiographyDialog(
    biography: ArtistBiography,
    paragraphs: List<String>,
    sourceText: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.84f))
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            BiographyDialogSurface(
                biography = biography,
                paragraphs = paragraphs,
                sourceText = sourceText,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun BiographyDialogSurface(
    biography: ArtistBiography,
    paragraphs: List<String>,
    sourceText: String,
    onDismiss: () -> Unit
) {
    Surface(
        color = Color(0xFF0D0E11),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 620.dp)
            .fillMaxHeight(0.90f)
            .shadow(28.dp, RoundedCornerShape(28.dp), clip = false)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(19.dp)
        ) {
            item(key = "biography-dialog-header") {
                BiographyDialogHeader(
                    biography = biography,
                    onDismiss = onDismiss
                )
            }
            itemsIndexed(
                items = paragraphs,
                key = { index, paragraph -> "biography-paragraph-$index-${paragraph.hashCode()}" }
            ) { index, paragraph ->
                BiographyDialogParagraph(
                    paragraph = paragraph,
                    featured = index == 0
                )
            }
            item(key = "biography-dialog-source") {
                BiographyDialogSource(
                    sourceText = sourceText,
                    sourceUrl = biography.sourceUrl
                )
            }
        }
    }
}

@Composable
private fun BiographyDialogHeader(
    biography: ArtistBiography,
    onDismiss: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = strings.biography,
                color = LevyraMuted.copy(alpha = 0.86f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = biography.pageTitle.ifBlank { strings.biography },
                color = LevyraText,
                fontSize = 27.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.Black
            )
            if (biography.description.isNotBlank()) {
                Text(
                    text = biography.description,
                    color = LevyraMuted.copy(alpha = 0.82f),
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .size(38.dp)
                .background(Color.White.copy(alpha = 0.065f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = strings.close,
                tint = LevyraText,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
private fun BiographyDialogSource(
    sourceText: String,
    sourceUrl: String
) {
    if (sourceText.isBlank()) return
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.07f), CircleShape)
            .clickable(enabled = sourceUrl.isNotBlank()) {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(sourceUrl)))
                }
            }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = sourceText,
            color = LevyraMuted.copy(alpha = 0.80f),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (sourceUrl.isNotBlank()) {
            Icon(
                imageVector = Icons.Rounded.OpenInNew,
                contentDescription = sourceText,
                tint = LevyraMuted.copy(alpha = 0.72f),
                modifier = Modifier.size(12.dp)
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
                        StableRemoteArtwork(
                            url = entry.release.thumbnailUrl,
                            contentDescription = entry.release.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        Icon(Icons.Rounded.Album, null, tint = LevyraMuted, modifier = Modifier.size(42.dp))
                    }
                    if (entry.isFresh) {
                        Text(
                            LocalLevyraStrings.current.newBadge,
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
    val strings = LocalLevyraStrings.current
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
                                Text(strings.newUpdate, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp), letterSpacing = 1.4.sp)
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
                                    text = "${strings.versionLabel} ${update.latestVersionName}",
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
                                text = update.releaseTitle.ifBlank { strings.updateDescription },
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
                                Text(strings.updateDescription, color = LevyraText.copy(alpha = 0.86f), fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(strings.whatsNew, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                            if (notes.isEmpty()) {
                                UpdateNoteCard(strings.updateDescription)
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
                                    Text(strings.later, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                color = LevyraCyan,
                                shape = RoundedCornerShape(17.dp),
                                modifier = Modifier.weight(1f).height(48.dp).pressable(onClick = onDownload)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(strings.update, color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Black)
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
    return clean.ifBlank { "General improvements and bug fixes." }
}

private fun openExternalUrl(
    context: android.content.Context,
    url: String,
    strings: LevyraStrings = LevyraStrings.forCode("en")
) {
    if (url.isBlank()) {
        Toast.makeText(context, strings.externalLinkUnavailable, Toast.LENGTH_LONG).show()
        return
    }
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, strings.cannotOpenExternalLink, Toast.LENGTH_LONG).show()
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
                            strings.formatQueueSummary(state.queue.size, state.queueHistoryCount),
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
                            Text(strings.continuousRadio, color = LevyraText, fontSize = 14.sp, fontWeight = FontWeight.Black)
                            Text(strings.continuousRadioSubtitle, color = LevyraMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Switch(checked = state.radioEnabled, onCheckedChange = { onToggleRadio() })
                        if (state.queueUndoAvailable) {
                            IconButton(onClick = onUndo) {
                                Icon(Icons.AutoMirrored.Rounded.Undo, strings.undoRemoval, tint = LevyraCyan)
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
                                LocalLevyraStrings.current.dragToReorder,
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
                                    Icon(Icons.Rounded.SkipNext, strings.playNext, tint = LevyraText, modifier = Modifier.size(19.dp))
                                }
                                IconButton(onClick = { onRemove(index) }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Rounded.Delete, strings.remove, tint = LevyraMuted, modifier = Modifier.size(19.dp))
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
    val strings = LocalLevyraStrings.current
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
                        Text(LocalLevyraStrings.current.smartMusicProfile, color = LevyraText, fontSize = 18.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            if (profile.isWarm) strings.profileActive else strings.profileLearning,
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
                            Text(LocalLevyraStrings.current.flow, color = LevyraCyan, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                if (artists.isNotBlank()) {
                    Text(strings.formatArtists(artists), color = LevyraText.copy(alpha = 0.88f), fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (albums.isNotBlank()) {
                    Text(strings.formatAlbumMood(albums), color = LevyraMuted, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

private enum class LyricsViewMode {
    PAGE,
    CINEMA,
    COMPACT
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LyricsOverlay(
    state: LevyraUiState,
    onTranslation: (Boolean) -> Unit,
    onSeekToMs: (Long) -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val track = state.currentTrack
    val accentStart = if (track != null) Color(track.accentStart) else LevyraCyan
    val accentEnd = if (track != null) Color(track.accentEnd) else LevyraViolet
    val blocker = remember { MutableInteractionSource() }
    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    var viewMode by remember(track?.id) { mutableStateOf(LyricsViewMode.CINEMA) }
    var showRomanization by remember(track?.id) { mutableStateOf(true) }
    var showSecondaryVoices by remember(track?.id) { mutableStateOf(true) }
    var lyricsOffsetMs by remember(track?.id) { mutableStateOf(0L) }
    var autoScrollEnabled by remember(track?.id) { mutableStateOf(true) }
    var autoScrolling by remember { mutableStateOf(false) }
    var initialLyricsPositioned by remember(track?.id) { mutableStateOf(false) }
    val visibleLyrics = remember(state.lyrics, showSecondaryVoices) {
        if (showSecondaryVoices) {
            state.lyrics
        } else {
            state.lyrics.filter { line -> line.role == LyricVocalRole.MAIN }
        }
    }
    val effectivePositionMs = (state.positionMs - lyricsOffsetMs).coerceAtLeast(0L)
    val activeIndex = if (state.lyricsSynced) activeLyricIndex(effectivePositionMs, visibleLyrics) else -1
    val lyricsStartIndex = 4
    val hasRomanization = state.lyrics.any { it.romanized.isNotBlank() || it.words.any { word -> word.romanized.isNotBlank() } }
    val hasMultipleVoices = state.lyrics.any { it.role != LyricVocalRole.MAIN }
    val sectionStarts = remember(state.lyricsSections, visibleLyrics) {
        val mapped = LinkedHashMap<Int, LyricSection>()
        state.lyricsSections.forEach { section ->
            val exactIndex = visibleLyrics.indexOfFirst { line ->
                line.role != LyricVocalRole.BACKGROUND && line.startMs >= section.startMs
            }
            val fallbackIndex = visibleLyrics.indexOfLast { line ->
                line.role != LyricVocalRole.BACKGROUND && line.startMs <= section.startMs
            }
            val index = if (exactIndex >= 0) exactIndex else fallbackIndex
            if (index >= 0 && mapped[index] == null) mapped[index] = section
        }
        mapped
    }
    val activeSection = remember(state.lyricsSections, effectivePositionMs) {
        state.lyricsSections.lastOrNull { section -> effectivePositionMs >= section.startMs }
    }
    val chorusPhrase = state.intelligenceSummary.repeatedPhrases.firstOrNull()
    val chorusIndex = remember(visibleLyrics, chorusPhrase, state.lyricsSections) {
        val sectionStart = state.lyricsSections.firstOrNull { it.type == LyricSectionType.CHORUS }?.startMs
        if (sectionStart != null) {
            visibleLyrics.indexOfFirst { line -> line.role != LyricVocalRole.BACKGROUND && line.startMs >= sectionStart }
        } else if (chorusPhrase.isNullOrBlank()) {
            -1
        } else {
            visibleLyrics.indexOfFirst { line -> line.text.contains(chorusPhrase, ignoreCase = true) }
        }
    }
    var requestedLyricIndex by remember { mutableStateOf<Int?>(null) }
    var showIntelligenceDialog by remember(track?.id) { mutableStateOf(false) }
    val anchorFraction = when (viewMode) {
        LyricsViewMode.CINEMA -> 0.36f
        LyricsViewMode.PAGE -> 0.42f
        LyricsViewMode.COMPACT -> 0.50f
    }

    LaunchedEffect(listState.isScrollInProgress, autoScrolling) {
        if (listState.isScrollInProgress && !autoScrolling) {
            autoScrollEnabled = false
        } else if (!listState.isScrollInProgress && !autoScrolling && !autoScrollEnabled) {
            delay(3_500L)
            autoScrollEnabled = true
        }
    }

    LaunchedEffect(activeIndex, lyricsStartIndex, autoScrollEnabled, viewMode, visibleLyrics.size) {
        if (activeIndex >= 0 && autoScrollEnabled) {
            autoScrolling = true
            runCatching {
                val targetIndex = lyricsStartIndex + activeIndex
                val targetVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == targetIndex }
                centerLyricsItem(
                    listState = listState,
                    index = targetIndex,
                    animate = initialLyricsPositioned && targetVisible,
                    anchorFraction = anchorFraction
                )
                initialLyricsPositioned = true
            }
            autoScrolling = false
        }
    }

    LaunchedEffect(requestedLyricIndex, lyricsStartIndex, viewMode) {
        val requested = requestedLyricIndex ?: return@LaunchedEffect
        autoScrolling = true
        runCatching {
            centerLyricsItem(
                listState = listState,
                index = lyricsStartIndex + requested,
                animate = true,
                anchorFraction = anchorFraction
            )
        }
        autoScrolling = false
        requestedLyricIndex = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(interactionSource = blocker, indication = null) {}
    ) {
        LevyraBackground(accentStart = track?.accentStart, accentEnd = track?.accentEnd)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(
                        alpha = when (viewMode) {
                            LyricsViewMode.CINEMA -> 0.68f
                            LyricsViewMode.PAGE -> 0.76f
                            LyricsViewMode.COMPACT -> 0.86f
                        }
                    )
                )
        )
        if (viewMode == LyricsViewMode.CINEMA) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(accentStart.copy(alpha = 0.12f), Color.Transparent),
                            radius = 1_250f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.18f), Color.Transparent, Color.Black.copy(alpha = 0.55f))
                        )
                    )
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = if (viewMode == LyricsViewMode.COMPACT) 18.dp else 22.dp,
                end = if (viewMode == LyricsViewMode.COMPACT) 18.dp else 22.dp,
                top = if (viewMode == LyricsViewMode.COMPACT) 8.dp else 14.dp,
                bottom = if (viewMode == LyricsViewMode.COMPACT) 96.dp else 150.dp
            ),
            verticalArrangement = Arrangement.spacedBy(
                when (viewMode) {
                    LyricsViewMode.CINEMA -> 14.dp
                    LyricsViewMode.PAGE -> 11.dp
                    LyricsViewMode.COMPACT -> 6.dp
                }
            )
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (viewMode == LyricsViewMode.COMPACT) 9.dp else 12.dp)
                ) {
                    if (track != null) {
                        AsyncImage(
                            model = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl },
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(
                                    when (viewMode) {
                                        LyricsViewMode.CINEMA -> 62.dp
                                        LyricsViewMode.PAGE -> 52.dp
                                        LyricsViewMode.COMPACT -> 44.dp
                                    }
                                )
                                .clip(RoundedCornerShape(if (viewMode == LyricsViewMode.COMPACT) 12.dp else 16.dp))
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track?.title ?: strings.lyrics,
                            color = Color.White,
                            fontSize = when (viewMode) {
                                LyricsViewMode.CINEMA -> 23.sp
                                LyricsViewMode.PAGE -> 21.sp
                                LyricsViewMode.COMPACT -> 18.sp
                            },
                            fontWeight = FontWeight.Black,
                            maxLines = if (viewMode == LyricsViewMode.COMPACT) 1 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track?.artist.orEmpty(),
                            color = Color.White.copy(alpha = 0.67f),
                            fontSize = if (viewMode == LyricsViewMode.COMPACT) 12.sp else 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (state.intelligenceSummary.available && viewMode != LyricsViewMode.COMPACT) {
                        CircleIconButton(
                            icon = Icons.Rounded.Insights,
                            tint = LevyraViolet,
                            background = LevyraViolet.copy(alpha = 0.14f),
                            onClick = { showIntelligenceDialog = true },
                            contentDescription = strings.openLyricsAnalysis
                        )
                    }
                    CircleIconButton(
                        icon = Icons.Rounded.Close,
                        tint = Color.White,
                        background = Color.White.copy(alpha = 0.13f),
                        onClick = onClose,
                        contentDescription = strings.closeLyrics
                    )
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (visibleLyrics.isNotEmpty() && viewMode != LyricsViewMode.COMPACT) {
                        LyricsStatusRow(
                            provider = state.lyricsProvider,
                            synced = state.lyricsSynced,
                            cached = state.lyricsCached,
                            confidence = state.lyricsConfidence,
                            syncedLabel = strings.synced
                        )
                    } else if (activeSection != null) {
                        Text(
                            text = lyricSectionLabel(strings, activeSection),
                            color = accentEnd.copy(alpha = 0.90f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.7.sp,
                            maxLines = 1
                        )
                    }
                    if (hasMultipleVoices) {
                        LyricsControlChip(
                            label = strings.lyricsDuet,
                            selected = showSecondaryVoices,
                            icon = Icons.Rounded.GraphicEq,
                            onClick = { showSecondaryVoices = !showSecondaryVoices }
                        )
                    }
                }
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val modeLabel = when (viewMode) {
                            LyricsViewMode.CINEMA -> strings.lyricsCinema
                            LyricsViewMode.PAGE -> strings.lyricsPage
                            LyricsViewMode.COMPACT -> strings.lyricsCompact
                        }
                        val modeIcon = when (viewMode) {
                            LyricsViewMode.CINEMA -> Icons.Rounded.GraphicEq
                            LyricsViewMode.PAGE -> Icons.AutoMirrored.Rounded.Subject
                            LyricsViewMode.COMPACT -> Icons.Rounded.ViewCompact
                        }
                        LyricsControlChip(
                            label = modeLabel,
                            selected = true,
                            icon = modeIcon,
                            onClick = {
                                viewMode = when (viewMode) {
                                    LyricsViewMode.CINEMA -> LyricsViewMode.PAGE
                                    LyricsViewMode.PAGE -> LyricsViewMode.COMPACT
                                    LyricsViewMode.COMPACT -> LyricsViewMode.CINEMA
                                }
                                initialLyricsPositioned = false
                            }
                        )
                        LyricsControlChip(
                            label = strings.automaticTranslation,
                            selected = state.lyricsTranslationEnabled,
                            icon = Icons.Rounded.Translate,
                            onClick = { onTranslation(!state.lyricsTranslationEnabled) }
                        )
                        if (hasRomanization) {
                            LyricsControlChip(
                                label = strings.lyricsRomanization,
                                selected = showRomanization,
                                icon = Icons.Rounded.Language,
                                onClick = { showRomanization = !showRomanization }
                            )
                        }
                        if (lyricsOffsetMs != 0L) {
                            LyricsControlChip(
                                label = formatLyricsOffset(lyricsOffsetMs),
                                selected = true,
                                icon = Icons.Rounded.Schedule,
                                onClick = { lyricsOffsetMs = 0L }
                            )
                        }
                    }
                    if (state.lyricsSections.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            contentPadding = PaddingValues(end = 12.dp)
                        ) {
                            items(
                                items = state.lyricsSections,
                                key = { section -> "${section.type}-${section.ordinal}-${section.startMs}" }
                            ) { section ->
                                LyricsSectionChip(
                                    label = lyricSectionLabel(strings, section),
                                    selected = activeSection?.let { active ->
                                        active.type == section.type && active.ordinal == section.ordinal
                                    } == true,
                                    onClick = {
                                        val targetIndex = visibleLyrics.indexOfFirst { line ->
                                            line.role != LyricVocalRole.BACKGROUND && line.startMs >= section.startMs
                                        }.takeIf { it >= 0 } ?: visibleLyrics.indexOfLast { line -> line.startMs <= section.startMs }
                                        if (targetIndex >= 0) {
                                            requestedLyricIndex = targetIndex
                                            autoScrollEnabled = true
                                            if (state.lyricsSynced) {
                                                onSeekToMs((section.startMs + lyricsOffsetMs).coerceAtLeast(0L))
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            item {
                Spacer(
                    modifier = Modifier.height(
                        when (viewMode) {
                            LyricsViewMode.CINEMA -> 30.dp
                            LyricsViewMode.PAGE -> 6.dp
                            LyricsViewMode.COMPACT -> 2.dp
                        }
                    )
                )
            }
            if (visibleLyrics.isEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 28.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (state.lyricsLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White.copy(alpha = 0.82f)
                            )
                        }
                        Text(
                            text = if (state.lyricsLoading) strings.searchingLyrics else strings.lyricsUnavailable,
                            color = Color.White.copy(alpha = 0.68f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = visibleLyrics,
                    key = { index, line -> "${line.startMs}-${line.role.name}-$index" }
                ) { index, line ->
                    val timedActive = state.lyricsSynced && (
                        index == activeIndex ||
                            line.role == LyricVocalRole.BACKGROUND && effectivePositionMs in line.startMs..line.endMs
                        )
                    KaraokeLyricLine(
                        line = line,
                        positionMs = effectivePositionMs,
                        isActive = timedActive,
                        isPrimaryActive = index == activeIndex,
                        synced = state.lyricsSynced,
                        viewMode = viewMode,
                        distanceFromActive = if (activeIndex >= 0) kotlin.math.abs(index - activeIndex) else 0,
                        sectionLabel = sectionStarts[index]?.let { lyricSectionLabel(strings, it) },
                        showRomanization = showRomanization,
                        accentEnd = accentEnd,
                        onClick = {
                            if (state.lyricsSynced) {
                                onSeekToMs((line.startMs + lyricsOffsetMs).coerceAtLeast(0L))
                                autoScrollEnabled = true
                            }
                        },
                        onLongClick = {
                            if (state.lyricsSynced) {
                                lyricsOffsetMs = state.positionMs - line.startMs
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                autoScrollEnabled = true
                            }
                        }
                    )
                }
            }
        }
    }

    if (showIntelligenceDialog) {
        LyricsIntelligenceDialog(
            summary = state.intelligenceSummary,
            onDismiss = { showIntelligenceDialog = false },
            onJumpToChorus = if (chorusIndex >= 0) {
                {
                    showIntelligenceDialog = false
                    requestedLyricIndex = chorusIndex
                    val chorusStartMs = visibleLyrics.getOrNull(chorusIndex)?.startMs ?: 0L
                    if (state.lyricsSynced) onSeekToMs((chorusStartMs + lyricsOffsetMs).coerceAtLeast(0L))
                }
            } else {
                null
            }
        )
    }
}

private suspend fun centerLyricsItem(
    listState: LazyListState,
    index: Int,
    animate: Boolean,
    anchorFraction: Float
) {
    if (index < 0) return
    var itemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (itemInfo == null) {
        listState.scrollToItem(index)
        withFrameNanos { }
        itemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    }
    val item = itemInfo ?: return
    val layoutInfo = listState.layoutInfo
    val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
    if (viewportHeight <= 0) return
    val desiredCenter = layoutInfo.viewportStartOffset + viewportHeight * anchorFraction.coerceIn(0.25f, 0.70f)
    val currentCenter = item.offset + item.size / 2f
    val delta = currentCenter - desiredCenter
    val hysteresis = maxOf(8f, viewportHeight * 0.018f)
    if (kotlin.math.abs(delta) <= hysteresis) return
    if (animate && kotlin.math.abs(delta) <= viewportHeight * 0.55f) {
        listState.animateScrollBy(delta)
    } else {
        listState.scrollBy(delta)
    }
}

private fun lyricSectionLabel(strings: LevyraStrings, section: LyricSection): String {
    val base = when (section.type) {
        LyricSectionType.INTRO -> strings.lyricsSectionIntro
        LyricSectionType.VERSE -> strings.lyricsSectionVerse
        LyricSectionType.PRE_CHORUS -> strings.lyricsSectionPreChorus
        LyricSectionType.CHORUS -> strings.lyricsSectionChorus
        LyricSectionType.BRIDGE -> strings.lyricsSectionBridge
        LyricSectionType.INSTRUMENTAL -> strings.lyricsSectionInstrumental
        LyricSectionType.OUTRO -> strings.lyricsSectionOutro
    }
    return if (section.ordinal > 1 && section.type in setOf(LyricSectionType.VERSE, LyricSectionType.CHORUS, LyricSectionType.BRIDGE)) {
        "$base ${section.ordinal}"
    } else {
        base
    }
}

@Composable
private fun LyricsSectionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.055f),
        border = BorderStroke(1.dp, if (selected) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.07f)),
        shape = CircleShape,
        modifier = Modifier.pressable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.58f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun LyricsControlChip(
    label: String,
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) LevyraCyan.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, if (selected) LevyraCyan.copy(alpha = 0.32f) else Color.White.copy(alpha = 0.08f)),
        shape = CircleShape,
        modifier = Modifier.pressable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) LevyraCyan else Color.White.copy(alpha = 0.55f),
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.68f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun KaraokeLyricLine(
    line: LyricLine,
    positionMs: Long,
    isActive: Boolean,
    isPrimaryActive: Boolean,
    synced: Boolean,
    viewMode: LyricsViewMode,
    distanceFromActive: Int,
    sectionLabel: String?,
    showRomanization: Boolean,
    accentEnd: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val cinema = viewMode == LyricsViewMode.CINEMA
    val compact = viewMode == LyricsViewMode.COMPACT
    val strings = LocalLevyraStrings.current
    val sectionLocale = remember(strings.code) { Locale.forLanguageTag(strings.code.replace('_', '-')) }
    val alignment = when (line.role) {
        LyricVocalRole.DUET_RIGHT -> Alignment.End
        LyricVocalRole.BACKGROUND -> Alignment.CenterHorizontally
        else -> Alignment.Start
    }
    val textAlign = when (line.role) {
        LyricVocalRole.DUET_RIGHT -> TextAlign.End
        LyricVocalRole.BACKGROUND -> TextAlign.Center
        else -> TextAlign.Start
    }
    val roleScale = when (line.role) {
        LyricVocalRole.BACKGROUND -> if (compact) 0.86f else 0.80f
        LyricVocalRole.DUET_LEFT, LyricVocalRole.DUET_RIGHT -> 0.92f
        LyricVocalRole.MAIN -> 1f
    }
    val activeScale by animateFloatAsState(
        targetValue = if (isPrimaryActive) 1.008f else 1f,
        animationSpec = tween(durationMillis = 120, easing = LinearOutSlowInEasing),
        label = "lyrics-line-scale"
    )
    val targetAlpha = when {
        !compact || !synced -> 1f
        distanceFromActive == 0 -> 1f
        distanceFromActive == 1 -> 0.58f
        distanceFromActive == 2 -> 0.25f
        else -> 0.10f
    }
    val lineAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 110),
        label = "lyrics-line-alpha"
    )
    val mainFontSize = when {
        compact && isPrimaryActive -> 22.sp
        compact -> 17.sp
        cinema && isPrimaryActive -> 29.sp
        cinema -> 24.sp
        isPrimaryActive -> 26.sp
        else -> 21.sp
    }
    val resolvedFontSize = mainFontSize * roleScale
    val lineHeight = resolvedFontSize * if (compact) 1.14f else 1.18f
    val inactiveColor = when {
        isActive -> Color.White.copy(alpha = 0.76f)
        synced -> Color.White.copy(alpha = when {
            compact -> 0.62f
            cinema -> 0.43f
            else -> 0.52f
        })
        else -> Color.White.copy(alpha = 0.88f)
    }
    val horizontalPadding = when (line.role) {
        LyricVocalRole.BACKGROUND -> if (compact) 16.dp else 24.dp
        LyricVocalRole.DUET_LEFT, LyricVocalRole.DUET_RIGHT -> 10.dp
        LyricVocalRole.MAIN -> 0.dp
    }
    val resolvedRomanization = line.romanized.ifBlank {
        line.words.joinToString("") { word -> word.romanized }.trim()
    }
    val activeWordColor = Color.White
    val completedWordColor = Color.White
    val pendingWordColor = Color.White.copy(alpha = if (compact) 0.50f else 0.58f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = if (compact) 2.dp else 0.dp)
            .graphicsLayer {
                scaleX = activeScale
                scaleY = activeScale
                alpha = lineAlpha
                transformOrigin = when (line.role) {
                    LyricVocalRole.DUET_RIGHT -> TransformOrigin(1f, 0.5f)
                    LyricVocalRole.BACKGROUND -> TransformOrigin.Center
                    else -> TransformOrigin(0f, 0.5f)
                }
            }
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = alignment,
        verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp)
    ) {
        if (!sectionLabel.isNullOrBlank()) {
            Text(
                text = sectionLabel.uppercase(sectionLocale),
                color = accentEnd.copy(alpha = if (isPrimaryActive) 0.92f else 0.56f),
                fontSize = if (compact) 9.sp else 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.9.sp,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (line.words.isNotEmpty() && synced) {
            KaraokeWordTimedText(
                words = line.words,
                positionMs = positionMs,
                isActive = isActive,
                fontSize = resolvedFontSize,
                lineHeight = lineHeight,
                textAlign = textAlign,
                inactiveColor = inactiveColor,
                completedColor = completedWordColor,
                activeColor = activeWordColor,
                pendingColor = pendingWordColor,
                fontWeight = if (isPrimaryActive) FontWeight.ExtraBold else FontWeight.Bold
            )
        } else {
            Text(
                text = if (line.isInstrumental) "♪" else line.text,
                color = if (isActive || isPrimaryActive) Color.White else inactiveColor,
                fontSize = resolvedFontSize,
                lineHeight = lineHeight,
                fontWeight = if (isPrimaryActive) FontWeight.ExtraBold else FontWeight.Bold,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (showRomanization && resolvedRomanization.isNotBlank()) {
            Text(
                text = resolvedRomanization,
                color = if (isActive) Color.White.copy(alpha = 0.76f) else Color.White.copy(alpha = 0.40f),
                fontSize = when {
                    compact -> 11.sp
                    cinema && isPrimaryActive -> 15.sp
                    else -> 13.sp
                },
                lineHeight = if (compact) 15.sp else 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (line.translated.isNotBlank()) {
            Text(
                text = line.translated,
                color = if (isActive) accentEnd.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.44f),
                fontSize = when {
                    compact -> 12.sp
                    cinema && isPrimaryActive -> 16.sp
                    else -> 14.sp
                },
                lineHeight = if (compact) 16.sp else 20.sp,
                fontWeight = FontWeight.Medium,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun KaraokeWordTimedText(
    words: List<com.luc4n3x.levyra.domain.LyricWord>,
    positionMs: Long,
    isActive: Boolean,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    textAlign: TextAlign,
    inactiveColor: Color,
    completedColor: Color,
    activeColor: Color,
    pendingColor: Color,
    fontWeight: FontWeight
) {
    val timedText = remember(words) { buildTimedLyricText(words) }
    val karaokeEasing = remember { CubicBezierEasing(0.18f, 0f, 0.20f, 1f) }
    val targetCharacterProgress = remember(timedText, positionMs, isActive) {
        if (!isActive || timedText.text.isEmpty()) {
            0f
        } else {
            val visualPositionMs = positionMs + 55L
            var filledCharacters = 0f
            timedText.words.forEach { timedWord ->
                when {
                    visualPositionMs >= timedWord.endMs -> {
                        filledCharacters = maxOf(filledCharacters, timedWord.startIndex + timedWord.length.toFloat())
                    }
                    visualPositionMs > timedWord.startMs -> {
                        val raw = (visualPositionMs - timedWord.startMs).toFloat() /
                            (timedWord.endMs - timedWord.startMs).coerceAtLeast(1L).toFloat()
                        val eased = karaokeEasing.transform(raw.coerceIn(0f, 1f))
                        filledCharacters = maxOf(filledCharacters, timedWord.startIndex + timedWord.length * eased)
                    }
                }
            }
            filledCharacters.coerceIn(0f, timedText.text.length.toFloat())
        }
    }
    val characterProgress by animateFloatAsState(
        targetValue = targetCharacterProgress,
        animationSpec = tween(durationMillis = 65, easing = LinearEasing),
        label = "karaoke-word-progress"
    )
    val visualPositionMs = positionMs + 55L
    val fillColor = remember(words, visualPositionMs, activeColor, completedColor) {
        if (words.any { visualPositionMs in it.startMs until it.endMs }) activeColor else completedColor
    }
    val contentAlignment = when (textAlign) {
        TextAlign.Center -> Alignment.Center
        TextAlign.End, TextAlign.Right -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }
    var textLayoutResult by remember(timedText.text, fontSize, lineHeight, textAlign, fontWeight) {
        mutableStateOf<TextLayoutResult?>(null)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = contentAlignment
    ) {
        Box(modifier = Modifier.wrapContentWidth()) {
            Text(
                text = timedText.text,
                color = if (isActive) pendingColor else inactiveColor,
                fontSize = fontSize,
                lineHeight = lineHeight,
                fontWeight = fontWeight,
                textAlign = textAlign,
                onTextLayout = { textLayoutResult = it },
                modifier = Modifier.wrapContentWidth()
            )
            if (isActive && characterProgress > 0f) {
                Text(
                    text = timedText.text,
                    color = fillColor,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    fontWeight = fontWeight,
                    textAlign = textAlign,
                    modifier = Modifier
                        .matchParentSize()
                        .drawWithContent {
                            val layoutResult = textLayoutResult ?: return@drawWithContent
                            val path = buildKaraokeGlyphPath(
                                layoutResult = layoutResult,
                                textLength = timedText.text.length,
                                characterProgress = characterProgress
                            )
                            clipPath(path) {
                                this@drawWithContent.drawContent()
                            }
                        }
                )
            }
        }
    }
}

private fun buildKaraokeGlyphPath(
    layoutResult: TextLayoutResult,
    textLength: Int,
    characterProgress: Float
): Path {
    val path = Path()
    if (textLength == 0 || characterProgress <= 0f) return path
    val boundedProgress = characterProgress.coerceIn(0f, textLength.toFloat())
    val completedCharacters = boundedProgress.toInt().coerceIn(0, textLength)
    for (index in 0 until completedCharacters) {
        val bounds = layoutResult.getBoundingBox(index)
        if (bounds.width > 0f && bounds.height > 0f) path.addRect(bounds)
    }
    if (completedCharacters < textLength) {
        val fraction = boundedProgress - completedCharacters
        if (fraction > 0f) {
            val bounds = layoutResult.getBoundingBox(completedCharacters)
            if (bounds.width > 0f && bounds.height > 0f) {
                val partialBounds = if (layoutResult.getBidiRunDirection(completedCharacters) == ResolvedTextDirection.Rtl) {
                    Rect(
                        left = bounds.right - bounds.width * fraction,
                        top = bounds.top,
                        right = bounds.right,
                        bottom = bounds.bottom
                    )
                } else {
                    Rect(
                        left = bounds.left,
                        top = bounds.top,
                        right = bounds.left + bounds.width * fraction,
                        bottom = bounds.bottom
                    )
                }
                path.addRect(partialBounds)
            }
        }
    }
    return path
}

private fun buildTimedLyricText(words: List<com.luc4n3x.levyra.domain.LyricWord>): TimedLyricText {
    val text = StringBuilder()
    val timedWords = ArrayList<TimedLyricWord>(words.size)
    var previousEndedWithWhitespace = true
    words.forEachIndexed { index, word ->
        val value = word.text.trim()
        if (value.isNotBlank()) {
            if (index > 0 && !previousEndedWithWhitespace && !value.first().isPunctuationWithoutLeadingSpace()) {
                text.append(' ')
            }
            val startIndex = text.length
            text.append(value)
            timedWords += TimedLyricWord(
                startIndex = startIndex,
                length = value.length,
                startMs = word.startMs,
                endMs = word.endMs.coerceAtLeast(word.startMs + 1L)
            )
            previousEndedWithWhitespace = word.text.lastOrNull()?.isWhitespace() == true
        }
    }
    return TimedLyricText(text.toString(), timedWords)
}

private data class TimedLyricText(
    val text: String,
    val words: List<TimedLyricWord>
)

private data class TimedLyricWord(
    val startIndex: Int,
    val length: Int,
    val startMs: Long,
    val endMs: Long
)

private fun Char.isPunctuationWithoutLeadingSpace(): Boolean = this in charArrayOf(',', '.', ';', ':', '!', '?', ')', ']', '}', '’', '\'', '…')

private fun activeLyricIndex(positionMs: Long, lines: List<LyricLine>): Int {
    if (lines.isEmpty()) return -1
    var low = 0
    var high = lines.lastIndex
    var candidate = -1
    while (low <= high) {
        val middle = (low + high) ushr 1
        if (lines[middle].startMs <= positionMs) {
            candidate = middle
            low = middle + 1
        } else {
            high = middle - 1
        }
    }
    if (candidate < 0) return -1
    val searchStart = (candidate - 16).coerceAtLeast(0)
    for (index in candidate downTo searchStart) {
        val line = lines[index]
        if (line.role != LyricVocalRole.BACKGROUND && positionMs in line.startMs..line.endMs) return index
    }
    for (index in candidate downTo searchStart) {
        val line = lines[index]
        if (positionMs in line.startMs..line.endMs) return index
    }
    for (index in candidate downTo searchStart) {
        if (lines[index].role != LyricVocalRole.BACKGROUND) return index
    }
    return candidate
}

private fun formatLyricsOffset(offsetMs: Long): String {
    val sign = if (offsetMs >= 0L) "+" else "−"
    val absolute = kotlin.math.abs(offsetMs)
    return "$sign${absolute / 1_000}.${(absolute % 1_000) / 100}s"
}

@Composable
private fun LyricsIntelligenceDialog(
    summary: com.luc4n3x.levyra.domain.LevyraIntelligenceSummary,
    onDismiss: () -> Unit,
    onJumpToChorus: (() -> Unit)?
) {
    val strings = LocalLevyraStrings.current
    val repeatedPhrase = summary.repeatedPhrases.firstOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF11131C),
        icon = {
            Icon(
                imageVector = Icons.Rounded.Insights,
                contentDescription = null,
                tint = LevyraViolet,
                modifier = Modifier.size(24.dp)
            )
        },
        title = {
            Text(
                text = strings.openLyricsAnalysis,
                color = LevyraText,
                fontWeight = FontWeight.Black
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (summary.mood.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(strings.atmosphere, color = LevyraMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(summary.mood, color = LevyraText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (summary.themes.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(strings.themes, color = LevyraMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = summary.themes.take(4).joinToString(" · "),
                            color = LevyraText,
                            fontSize = 14.sp,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (!repeatedPhrase.isNullOrBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(strings.chorusDetected, color = LevyraMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "“$repeatedPhrase”",
                            color = LevyraText.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = strings.formatLyricsAnalysis(summary.lineCount, summary.wordCount),
                    color = LevyraMuted.copy(alpha = 0.82f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        confirmButton = {
            if (onJumpToChorus != null) {
                TextButton(onClick = onJumpToChorus) {
                    Text(strings.goToChorus, color = LevyraCyan, fontWeight = FontWeight.Black)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(strings.close, color = LevyraCyan, fontWeight = FontWeight.Black)
                }
            }
        },
        dismissButton = {
            if (onJumpToChorus != null) {
                TextButton(onClick = onDismiss) {
                    Text(strings.close, color = LevyraMuted, fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

@Composable
private fun LyricsStatusRow(provider: String, synced: Boolean, cached: Boolean, confidence: Int, syncedLabel: String) {
    val label = buildString {
        if (synced) {
            append(syncedLabel)
            if (provider.isNotBlank()) append(" • ").append(provider)
        } else {
            append(provider.ifBlank { "Lyrics" })
        }
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
    val sourceStart = accentStart?.let { Color(it) } ?: LevyraNavigationBlue
    val sourceEnd = accentEnd?.let { Color(it) } ?: LevyraNavigationBlueDeep
    val primaryAccent by animateColorAsState(
        targetValue = sourceStart,
        animationSpec = tween(900, easing = LinearOutSlowInEasing),
        label = "levyra-background-primary"
    )
    val secondaryAccent by animateColorAsState(
        targetValue = sourceEnd,
        animationSpec = tween(900, easing = LinearOutSlowInEasing),
        label = "levyra-background-secondary"
    )
    val isLight = LevyraIsLight

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithCache {
                if (size.minDimension <= 0f) {
                    onDrawBehind {}
                } else if (isLight) {
                    val width = size.width
                    val height = size.height
                    val backgroundBrush = Brush.verticalGradient(
                        listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFF8FAFF),
                            Color(0xFFF2F5FB)
                        )
                    )
                    val haloCenter = androidx.compose.ui.geometry.Offset(width * 0.18f, height * 0.04f)
                    val haloRadius = width * 0.92f
                    val haloBrush = Brush.radialGradient(
                        colors = listOf(
                            LevyraNavigationBlue.copy(alpha = 0.09f),
                            LevyraNavigationBlue.copy(alpha = 0.025f),
                            Color.Transparent
                        ),
                        center = haloCenter,
                        radius = haloRadius
                    )
                    val violetHaloCenter = androidx.compose.ui.geometry.Offset(width * 0.94f, height * 0.12f)
                    val violetHaloRadius = width * 0.64f
                    val violetHaloBrush = Brush.radialGradient(
                        colors = listOf(
                            LevyraViolet.copy(alpha = 0.05f),
                            LevyraViolet.copy(alpha = 0.015f),
                            Color.Transparent
                        ),
                        center = violetHaloCenter,
                        radius = violetHaloRadius
                    )
                    onDrawBehind {
                        drawRect(backgroundBrush)
                        drawCircle(
                            brush = haloBrush,
                            radius = haloRadius,
                            center = haloCenter
                        )
                        drawCircle(
                            brush = violetHaloBrush,
                            radius = violetHaloRadius,
                            center = violetHaloCenter
                        )
                    }
                } else {
                    val width = size.width
                    val height = size.height
                    val backgroundBrush = Brush.verticalGradient(
                        listOf(
                            Color.Black,
                            Color(0xFF01040A),
                            Color(0xFF02050B),
                            Color.Black
                        )
                    )
                    val topHalo = androidx.compose.ui.geometry.Offset(width * 0.18f, height * 0.02f)
                    val topHaloRadius = width * 0.88f
                    val topHaloBrush = Brush.radialGradient(
                        colors = listOf(
                            LevyraNavigationBlue.copy(alpha = 0.17f),
                            LevyraNavigationBlue.copy(alpha = 0.055f),
                            Color.Transparent
                        ),
                        center = topHalo,
                        radius = topHaloRadius
                    )
                    val rightHalo = androidx.compose.ui.geometry.Offset(width * 1.04f, height * 0.34f)
                    val rightHaloRadius = width * 0.76f
                    val rightHaloBrush = Brush.radialGradient(
                        colors = listOf(
                            LevyraNavigationBlueDeep.copy(alpha = 0.10f),
                            primaryAccent.copy(alpha = 0.025f),
                            Color.Transparent
                        ),
                        center = rightHalo,
                        radius = rightHaloRadius
                    )
                    val leftHalo = androidx.compose.ui.geometry.Offset(-width * 0.06f, height * 0.56f)
                    val leftHaloRadius = width * 0.62f
                    val leftHaloBrush = Brush.radialGradient(
                        colors = listOf(
                            primaryAccent.copy(alpha = 0.055f),
                            primaryAccent.copy(alpha = 0.018f),
                            Color.Transparent
                        ),
                        center = leftHalo,
                        radius = leftHaloRadius
                    )
                    val signalPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(-width * 0.10f, height * 0.27f)
                        cubicTo(
                            width * 0.18f,
                            height * 0.17f,
                            width * 0.34f,
                            height * 0.39f,
                            width * 0.56f,
                            height * 0.25f
                        )
                        cubicTo(
                            width * 0.72f,
                            height * 0.15f,
                            width * 0.88f,
                            height * 0.32f,
                            width * 1.10f,
                            height * 0.20f
                        )
                    }
                    val signalBrush = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            LevyraNavigationBlue.copy(alpha = 0.04f),
                            LevyraNavigationBlue.copy(alpha = 0.12f),
                            LevyraNavigationBlueDeep.copy(alpha = 0.055f),
                            Color.Transparent
                        )
                    )
                    val signalStroke = Stroke(width = 1.15.dp.toPx())
                    val echoPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(-width * 0.08f, height * 0.285f)
                        cubicTo(
                            width * 0.19f,
                            height * 0.20f,
                            width * 0.36f,
                            height * 0.42f,
                            width * 0.57f,
                            height * 0.28f
                        )
                        cubicTo(
                            width * 0.74f,
                            height * 0.18f,
                            width * 0.90f,
                            height * 0.35f,
                            width * 1.08f,
                            height * 0.24f
                        )
                    }
                    val echoStroke = Stroke(width = 0.75.dp.toPx())
                    val firstArcTopLeft = androidx.compose.ui.geometry.Offset(width * 0.45f, -height * 0.075f)
                    val firstArcSize = androidx.compose.ui.geometry.Size(width * 0.74f, height * 0.34f)
                    val firstArcStroke = Stroke(width = 0.9.dp.toPx())
                    val secondArcTopLeft = androidx.compose.ui.geometry.Offset(width * 0.38f, -height * 0.105f)
                    val secondArcSize = androidx.compose.ui.geometry.Size(width * 0.90f, height * 0.41f)
                    val secondArcStroke = Stroke(width = 0.65.dp.toPx())
                    val signalNodes = LevyraSignalNodes.mapIndexed { index, node ->
                        Triple(
                            index,
                            androidx.compose.ui.geometry.Offset(width * node.first, height * node.second),
                            when (index % 3) {
                                0 -> 0.13f
                                1 -> 0.085f
                                else -> 0.055f
                            }
                        )
                    }
                    val nodeRadiusLarge = 1.25.dp.toPx()
                    val nodeRadiusSmall = 0.75.dp.toPx()
                    val nodeHaloRadius = 8.dp.toPx()
                    val bottomFadeTop = height * 0.50f
                    val bottomFadeBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.24f),
                            Color.Black.copy(alpha = 0.88f),
                            Color.Black
                        ),
                        startY = bottomFadeTop,
                        endY = height
                    )
                    val bottomFadeSize = androidx.compose.ui.geometry.Size(width, height - bottomFadeTop)

                    onDrawBehind {
                        drawRect(backgroundBrush)
                        drawCircle(
                            brush = topHaloBrush,
                            radius = topHaloRadius,
                            center = topHalo
                        )
                        drawCircle(
                            brush = rightHaloBrush,
                            radius = rightHaloRadius,
                            center = rightHalo
                        )
                        drawCircle(
                            brush = leftHaloBrush,
                            radius = leftHaloRadius,
                            center = leftHalo
                        )
                        drawPath(
                            path = signalPath,
                            brush = signalBrush,
                            style = signalStroke
                        )
                        drawPath(
                            path = echoPath,
                            color = LevyraNavigationBlue.copy(alpha = 0.035f),
                            style = echoStroke
                        )
                        drawArc(
                            color = LevyraNavigationBlue.copy(alpha = 0.075f),
                            startAngle = 202f,
                            sweepAngle = 116f,
                            useCenter = false,
                            topLeft = firstArcTopLeft,
                            size = firstArcSize,
                            style = firstArcStroke
                        )
                        drawArc(
                            color = secondaryAccent.copy(alpha = 0.035f),
                            startAngle = 196f,
                            sweepAngle = 128f,
                            useCenter = false,
                            topLeft = secondArcTopLeft,
                            size = secondArcSize,
                            style = secondArcStroke
                        )
                        signalNodes.forEach { (index, center, pulse) ->
                            drawCircle(
                                color = LevyraNavigationBlue.copy(alpha = pulse),
                                radius = if (index % 4 == 0) nodeRadiusLarge else nodeRadiusSmall,
                                center = center
                            )
                            if (index % 4 == 0) {
                                drawCircle(
                                    color = LevyraNavigationBlue.copy(alpha = 0.025f),
                                    radius = nodeHaloRadius,
                                    center = center
                                )
                            }
                        }
                        drawRect(
                            brush = bottomFadeBrush,
                            topLeft = androidx.compose.ui.geometry.Offset(0f, bottomFadeTop),
                            size = bottomFadeSize
                        )
                    }
                }
            }
    )
}

internal fun homeSectionLazyKey(
    position: Int,
    title: String,
    trackIds: List<String>
): String {
    return "$position|${title.trim().lowercase(Locale.ROOT)}"
}

@Composable
private fun HomeScreen(viewModel: HomeViewModel, renderSnapshot: HomeRenderSnapshot, homeListState: LazyListState) {
    val state = renderSnapshot.state
    val homeDerivedState = renderSnapshot.derived
    val strings = LocalLevyraStrings.current
    val context = LocalContext.current
    var addTarget by remember { mutableStateOf<Track?>(null) }
    LaunchedEffect(homeDerivedState.artistRefreshFingerprint) {
        viewModel.refreshHomeArtists()
    }
    val personalTracks = remember(state.personalOrbitTracks) {
        state.personalOrbitTracks.take(LevyraPersonalOrbit.DISPLAY_LIMIT)
    }
    val resonanceTracks = homeDerivedState.resonanceTracks
    val quickPicks = homeDerivedState.quickPicks
    val newReleases = homeDerivedState.newReleases
    val homeAlbums = remember(state.homeAlbums) {
        state.homeAlbums
            .filter { album -> album.title.isNotBlank() && album.artist.isNotBlank() }
            .distinctBy(::albumRecommendationDeduplicationKey)
    }
    val otherSections = homeDerivedState.otherSections
    val chartChunks = homeDerivedState.chartChunks
    val homeContent = homeDerivedState.contentAvailability
    val homeFingerprint = homeDerivedState.contentFingerprint
    val showHomeAlbumShimmer = HomeLoadingPolicy.showAlbumShimmer(homeContent, state.homeAlbumsLoading)
    val showChartShimmer = HomeLoadingPolicy.showChartShimmer(homeContent, state.isLoadingCharts)
    LaunchedEffect(homeFingerprint) {
        LevyraArtworkStartupMetrics.recordHomeEmission(homeFingerprint, homeContent.hasUsableContent)
    }
    LaunchedEffect(showHomeAlbumShimmer, showChartShimmer) {
        if (showHomeAlbumShimmer) LevyraArtworkStartupMetrics.recordShimmer(homeContent.hasUsableContent)
        if (showChartShimmer) LevyraArtworkStartupMetrics.recordShimmer(homeContent.hasUsableContent)
    }
    LaunchedEffect(homeListState) {
        snapshotFlow {
            val atTop = homeListState.firstVisibleItemIndex == 0 && homeListState.firstVisibleItemScrollOffset == 0
            homeListState.isScrollInProgress to atTop
        }
            .distinctUntilChanged()
            .collect { (scrollInProgress, atTop) ->
                viewModel.setHomeViewport(scrollInProgress, atTop)
            }
    }
    DisposableEffect(viewModel, homeListState) {
        val atTop = homeListState.firstVisibleItemIndex == 0 && homeListState.firstVisibleItemScrollOffset == 0
        viewModel.onHomeEntered(atTop)
        onDispose { viewModel.onHomeLeft() }
    }
    if (BuildConfig.DEBUG) {
        LaunchedEffect(Unit) {
            delay(5_000L)
            withContext(Dispatchers.IO) {
                LevyraArtworkStartupMetrics.persistSnapshot(context)
            }
        }
    }
    LazyColumn(
        state = homeListState,
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(top = 8.dp, bottom = if (state.currentTrack != null) 188.dp else 104.dp),
        verticalArrangement = Arrangement.spacedBy(if (state.interfaceSettings.compactHome) 12.dp else 22.dp)
    ) {
        item(key = "home-top", contentType = "home-header") {
            HomeSectionInset {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    GreetingBar(state.userName, state.isResolving, onSettings = viewModel::openSettings)
                    MoodRow(moods = state.moods, selectedId = state.selectedMood?.id, onSelect = viewModel::selectMood)
                }
            }
        }
        if (state.interfaceSettings.showPersonalOrbit && personalTracks.isNotEmpty()) {
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
        if (state.currentTrack != null && !state.isPlaying && !state.isResolving) {
            item(key = "home-continue", contentType = "home-card") {
                HomeContinueListeningCard(
                    viewModel = viewModel,
                    track = state.currentTrack
                )
            }
        }
        if (state.interfaceSettings.showResonance && resonanceTracks.isNotEmpty()) {
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
        if (quickPicks != null && quickPicks.tracks.isNotEmpty()) {
            item(key = "home-quick-picks", contentType = "home-dense-shelf") {
                HomeQuickPicksShelf(
                    title = quickPicks.title.ifBlank { strings.quickPicks },
                    tracks = quickPicks.tracks,
                    currentId = state.currentTrack?.id,
                    isPlaying = state.isPlaying,
                    isResolving = state.isResolving,
                    onPlay = { track -> viewModel.playFrom(quickPicks.tracks, track) },
                    onPlayAll = { viewModel.playAll(quickPicks.tracks) }
                )
            }
        }

        if (state.interfaceSettings.showNewReleases && state.releaseRadar.isNotEmpty()) {
            item(key = "sec-release-radar-header", contentType = "home-section-header") {
                HomeSectionInset { SectionTitle(strings.releaseRadar) }
            }
            item(key = "sec-release-radar-row", contentType = "home-horizontal-row") {
                ReleaseRadarRow(
                    entries = state.releaseRadar,
                    onOpen = { entry -> viewModel.searchNow("${entry.release.title} ${entry.artistName}") },
                    onArtist = viewModel::openArtistByName
                )
            }
        }
        if (state.interfaceSettings.showTrendingArtists && state.similarArtists.isNotEmpty()) {
            item(key = "sec-similar-artists-header", contentType = "home-section-header") {
                HomeSectionInset { SectionTitle(strings.similarToFollowed) }
            }
            item(key = "sec-similar-artists-row", contentType = "home-horizontal-row") {
                ArtistHitRow(
                    artists = state.similarArtists,
                    contentPadding = PaddingValues(start = HomeHorizontalInset, end = HomeHorizontalShelfEndPadding),
                    onClick = viewModel::openArtistFromHit
                )
            }
        }
        if (state.interfaceSettings.showNewReleases && newReleases != null && newReleases.tracks.isNotEmpty()) {
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
        if (state.interfaceSettings.showAlbumsForYou && (homeAlbums.isNotEmpty() || showHomeAlbumShimmer)) {
            item(key = "sec-home-albums-header", contentType = "home-section-header") {
                HomeSectionInset { SectionHeaderAction(strings.albumsForYou, onPlayAll = { viewModel.playAlbumRecommendations(homeAlbums) }) }
            }
            item(key = "sec-home-albums-row", contentType = "home-horizontal-row") {
                if (homeAlbums.isNotEmpty()) {
                    HomeAlbumHitRow(
                        albums = homeAlbums,
                        animationsEnabled = state.animationsEnabled,
                        onOpen = viewModel::openAlbum
                    )
                } else if (showHomeAlbumShimmer) {
                    HomeAlbumLoadingRow()
                }
            }
        }
        if (
            state.interfaceSettings.showTrendingArtists &&
            (state.homeArtists.isNotEmpty() || state.homeArtistsLoading)
        ) {
            item(key = "home-trending-artists", contentType = "home-shelf") {
                TrendingArtistsShelf(
                    artists = state.homeArtists.take(HOME_ARTIST_SHELF_SIZE),
                    loadingSlots = if (state.homeArtists.isEmpty() && state.homeArtistsLoading) HOME_ARTIST_SHELF_SIZE else 0,
                    onArtistClick = viewModel::openArtistFromHit
                )
            }
        }
        otherSections.forEachIndexed { sectionIndex, section ->
            if (section.tracks.isNotEmpty()) {
                val sectionKey = homeSectionLazyKey(
                    position = sectionIndex,
                    title = section.title,
                    trackIds = section.tracks.take(3).map { it.id }
                )
                item(key = "sec-other-$sectionKey-header", contentType = "home-section-header") {
                    HomeSectionInset { SectionHeaderAction(section.title, onPlayAll = { viewModel.playAll(section.tracks) }) }
                }
                item(key = "sec-other-$sectionKey-row", contentType = "home-horizontal-row") {
                    AlbumCardRow(
                        tracks = section.tracks,
                        currentId = state.currentTrack?.id,
                        animationsEnabled = state.animationsEnabled,
                        onPlay = { viewModel.playFrom(section.tracks, it) }
                    )
                }
            }
        }
        if (state.interfaceSettings.showCharts) {
            item(key = "home-chart-title", contentType = "home-section-header") {
                val region = state.chartRegions.firstOrNull { it.id == state.selectedChartId }
                HomeSectionInset {
                    SectionHeaderAction("Top 50 ${region?.label ?: "Global"} ${region?.emoji ?: ""}", onPlayAll = { viewModel.playAll(state.charts) })
                }
            }
            item(key = "home-chart-regions", contentType = "home-horizontal-row") {
                ChartRegionRow(
                    regions = state.chartRegions,
                    selectedId = state.selectedChartId,
                    loading = state.isLoadingCharts,
                    onSelect = viewModel::selectChart
                )
            }
            if (state.charts.isEmpty() && (showChartShimmer || !state.isLoadingCharts)) {
                item(key = "home-chart-empty", contentType = "home-card") {
                    HomeSectionInset {
                        if (showChartShimmer) {
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
                            key = { chunkIndex, _ -> "chart-column-$chunkIndex" },
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

@Composable
private fun HomeQuickAccessGrid(
    hasMix: Boolean,
    hasFavorites: Boolean,
    hasNewReleases: Boolean,
    onMix: () -> Unit,
    onFavorites: () -> Unit,
    onNewReleases: () -> Unit,
    onSearch: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickAction(
                icon = Icons.Rounded.Shuffle,
                label = LocalLevyraStrings.current.mixForYou,
                accent = LevyraCyan,
                enabled = hasMix,
                modifier = Modifier.weight(1f),
                onClick = onMix
            )
            QuickAction(
                icon = Icons.Rounded.Favorite,
                label = LocalLevyraStrings.current.favoritesPlain,
                accent = LevyraPink,
                enabled = hasFavorites,
                modifier = Modifier.weight(1f),
                onClick = onFavorites
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickAction(
                icon = Icons.Rounded.Bolt,
                label = LocalLevyraStrings.current.newReleases,
                accent = LevyraViolet,
                enabled = hasNewReleases,
                modifier = Modifier.weight(1f),
                onClick = onNewReleases
            )
            QuickAction(
                icon = Icons.Rounded.Search,
                label = LocalLevyraStrings.current.search,
                accent = Color(0xFFB7C7FF),
                enabled = true,
                modifier = Modifier.weight(1f),
                onClick = onSearch
            )
        }
    }
}

@Composable
private fun HomeQuickPicksShelf(
    title: String,
    tracks: List<Track>,
    currentId: String?,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: (Track) -> Unit,
    onPlayAll: () -> Unit
) {
    val columns = remember(tracks) {
        tracks
            .distinctBy(LevyraPersonalOrbit::identityKey)
            .take(21)
            .chunked(3)
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HomeSectionInset {
            SectionHeaderAction(title, onPlayAll)
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(
                start = HomeHorizontalInset,
                end = HomeHorizontalShelfEndPadding
            )
        ) {
            itemsIndexed(
                items = columns,
                key = { columnIndex, column ->
                    "quick-picks-$columnIndex-${column.firstOrNull()?.id.orEmpty()}"
                },
                contentType = { _, _ -> "quick-picks-column" }
            ) { _, column ->
                Column(
                    modifier = Modifier.width(286.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    column.forEach { track ->
                        HomeQuickPickRow(
                            track = track,
                            isCurrent = track.id == currentId,
                            isPlaying = isPlaying && track.id == currentId,
                            isResolving = isResolving && track.id == currentId,
                            onPlay = { onPlay(track) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeQuickPickRow(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: () -> Unit
) {
    val shape = RoundedCornerShape(17.dp)
    val background = if (isCurrent) {
        Brush.horizontalGradient(
            listOf(
                LevyraCyan.copy(alpha = 0.16f),
                LevyraViolet.copy(alpha = 0.06f),
                Color.Transparent
            )
        )
    } else {
        cinematicGlassBrush(
            accentStart = Color(track.accentStart),
            accentEnd = Color(track.accentEnd),
            intensity = 0.6f
        )
    }
    val outlineColor = if (isCurrent) {
        LevyraCyan.copy(alpha = 0.24f)
    } else {
        LevyraAdaptiveSoftHairline
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clip(shape)
            .background(background)
            .border(Dp.Hairline, outlineColor, shape)
            .pressable(onClick = onPlay)
            .padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CoverImage(
            track = track,
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(13.dp))
                .border(
                    Dp.Hairline,
                    Color.White.copy(alpha = 0.08f),
                    RoundedCornerShape(13.dp)
                ),
            highRes = false
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = track.title,
                color = if (isCurrent) LevyraCyan else LevyraText,
                fontSize = 14.5.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                color = LevyraMuted,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    if (isCurrent) LevyraCyan.copy(alpha = 0.16f) else Color.Transparent,
                    CircleShape
                )
                .border(
                    Dp.Hairline,
                    if (isCurrent) LevyraCyan.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.06f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                isResolving -> CircularProgressIndicator(
                    modifier = Modifier.size(15.dp),
                    strokeWidth = 1.8.dp,
                    color = LevyraCyan
                )
                isCurrent && isPlaying -> ActiveTrackEqualizer(
                    color = LevyraCyan,
                    isPlaying = true,
                    width = 15.dp,
                    height = 11.dp
                )
                else -> Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = if (isCurrent) LevyraCyan else LevyraText.copy(alpha = 0.72f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeContinueListeningCard(
    viewModel: HomeViewModel,
    track: Track
) {
    val playbackProgress by viewModel.playbackProgress.collectAsStateWithLifecycle()
    HomeSectionInset {
        ContinueListeningCard(
            track = track,
            progress = progressOf(playbackProgress.positionMs, playbackProgress.durationMs),
            onResume = viewModel::togglePlay
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
}

@Composable
private fun StableRemoteArtwork(
    url: String,
    contentDescription: String,
    modifier: Modifier,
    contentScale: ContentScale
) {
    val context = LocalContext.current
    val resizedUrl = remember(url) { LevyraArtworkCache.small(url) }
    val request = remember(context, resizedUrl) {
        ImageRequest.Builder(context)
            .data(resizedUrl)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
    )
}

@Composable
private fun TrendingArtistsShelf(
    artists: List<ArtistHit>,
    loadingSlots: Int,
    onArtistClick: (ArtistHit) -> Unit
) {
    val strings = LocalLevyraStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = HomeHorizontalInset),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionAccentBar(height = 22.dp, width = 4.dp)
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
            itemsIndexed(
                items = artists,
                key = { index, artist -> "trending-artist-$index-${artist.browseId.ifBlank { artist.name.trim().lowercase(Locale.ROOT) }}" },
                contentType = { _, _ -> "trending-artist" }
            ) { _, artist ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(86.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onArtistClick(artist) }
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
                        if (artist.thumbnailUrl.isNotBlank()) {
                            StableRemoteArtwork(
                                url = artist.thumbnailUrl,
                                contentDescription = artist.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(CinematicGlassDeep)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = artist.name,
                                tint = LevyraText,
                                modifier = Modifier.size(34.dp)
                            )
                        }
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
            items(
                count = loadingSlots,
                key = { index -> "artist-loading-${artists.size + index}" },
                contentType = { "trending-artist-loading" }
            ) {
                TrendingArtistLoadingItem()
            }
        }
    }
}


@Composable
private fun TrendingArtistLoadingItem() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(86.dp)
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .shimmer()
                .background(CinematicGlassDeep)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(62.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(99.dp))
                .shimmer()
                .background(CinematicGlassDeep)
        )
    }
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
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionAccentBar(height = 32.dp, width = 4.dp)
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = strings.voicesTitle,
                            color = LevyraText,
                            fontSize = 24.sp,
                            lineHeight = 27.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.55).sp
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = LevyraMuted,
                            modifier = Modifier.size(22.dp)
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
            }
            HomePlayAllButton(onClick = onPlayAll)
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
    val shape = RoundedCornerShape(18.dp)
    Surface(
        color = if (LevyraIsLight) Color.White.copy(alpha = 0.92f) else Color(0xFF101114),
        border = BorderStroke(
            width = if (active) 1.5.dp else Dp.Hairline,
            color = if (active) LevyraCyan.copy(alpha = 0.80f) else LevyraAdaptiveSoftHairline
        ),
        shape = shape,
        modifier = Modifier
            .width(282.dp)
            .height(112.dp)
            .pressable(onClick = onClick)
    ) {
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
                    .border(Dp.Hairline, LevyraAdaptiveSoftHairline, RoundedCornerShape(14.dp))
            ) {
                CoverImage(
                    track = track,
                    modifier = Modifier.fillMaxSize(),
                    highRes = false
                )
                when {
                    resolving -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.38f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = LevyraCyan
                            )
                        }
                    }
                    active -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.30f)),
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
                        color = LevyraText,
                        fontSize = 15.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        color = LevyraMuted,
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocalFireDepartment,
                                contentDescription = null,
                                tint = LevyraOrange,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "$engagement%",
                                color = LevyraText,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = formatCompactNumber(comments),
                            color = LevyraMuted,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .clip(CircleShape)
                            .background(LevyraAdaptiveTrack)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(pulseWidth.coerceIn(0.1f, 1f))
                                .height(2.dp)
                                .clip(CircleShape)
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
    val shelfTracks = remember(tracks) { tracks.distinctBy { LevyraPersonalOrbit.identityKey(it) }.take(LevyraPersonalOrbit.DISPLAY_LIMIT) }
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
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionAccentBar(height = 34.dp, width = 4.dp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = strings.personalOrbitTitle,
                            color = LevyraText,
                            fontSize = 26.sp,
                            lineHeight = 29.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.65).sp
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = LevyraMuted,
                            modifier = Modifier.size(23.dp)
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
            }
            HomePlayAllButton(onClick = onPlayAll)
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = HomeHorizontalInset),
            pageSpacing = 14.dp
        ) { pageIndex ->
            val pageTracks = pages.getOrElse(pageIndex) { emptyList() }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(2) { rowIndex ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Spacer(modifier = Modifier.fillMaxWidth().aspectRatio(1f))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Spacer(modifier = Modifier.height(34.dp))
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
                            .height(5.dp)
                            .width(indicatorWidth)
                            .clip(CircleShape)
                            .background(
                                if (pagerState.currentPage == index) {
                                    Brush.horizontalGradient(listOf(LevyraCyan, LevyraViolet))
                                } else {
                                    Brush.horizontalGradient(
                                        listOf(
                                            LevyraMuted.copy(alpha = 0.28f),
                                            LevyraMuted.copy(alpha = 0.18f)
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
    val artworkShape = RoundedCornerShape(17.dp)
    Column(
        modifier = modifier.pressable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(artworkShape)
                .border(
                    width = if (active) 1.5.dp else Dp.Hairline,
                    color = if (active) LevyraCyan.copy(alpha = 0.88f) else LevyraAdaptiveSoftHairline,
                    shape = artworkShape
                )
        ) {
            CoverImage(
                track = track,
                modifier = Modifier.fillMaxSize(),
                highRes = false
            )
            when {
                resolving -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(27.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.72f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                            color = LevyraCyan
                        )
                    }
                }
                active -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(27.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.72f)),
                        contentAlignment = Alignment.Center
                    ) {
                        ActiveTrackEqualizer(
                            color = LevyraCyan,
                            isPlaying = playing,
                            width = 12.dp,
                            height = 9.dp
                        )
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = track.title,
                color = LevyraText,
                fontSize = 12.5.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                color = LevyraMuted,
                fontSize = 10.5.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


private fun isPersonalOrbitSectionTitle(title: String): Boolean {
    val normalized = title.lowercase(Locale.ROOT)
    return normalized.contains("nella tua orbita") ||
        normalized.contains("la tua orbita") ||
        normalized.contains("your orbit") ||
        normalized.contains("in your orbit") ||
        normalized.contains("tu órbita") ||
        normalized.contains("ton orbite") ||
        normalized.contains("deine umlaufbahn") ||
        normalized.contains("jouw baan") ||
        normalized.contains("twoja orbita")
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

private fun buildHomeUpdateCopy(update: HomeHeroUpdate, strings: LevyraStrings): HomeUpdateCopy {
    val track = update.track
    val artist = track.artist.ifBlank { strings.artistLabel }
    val title = track.title.ifBlank { strings.song }
    val source = track.source.ifBlank { "YouTube Music" }
    val sourceTitle = update.sourceTitle.trim().ifBlank { source }
    val sourceLabel = buildProfessionalSourceLabel(source, sourceTitle)
    if (!update.verifiedRelease) {
        val chartDriven = isChartDrivenSource(sourceTitle) || isChartDrivenSource(source)
        return HomeUpdateCopy(
            badge = if (chartDriven) strings.top50Charts.uppercase() else strings.releaseRadar.uppercase(),
            headline = title,
            detail = artist,
            caption = strings.discoveryFlow,
            sourceLabel = sourceLabel,
            primaryAction = strings.play,
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
            badge = strings.newAlbum,
            headline = album ?: title,
            detail = artist,
            caption = strings.newReleaseSubtitle,
            sourceLabel = sourceLabel,
            primaryAction = strings.open,
            icon = Icons.Rounded.Album
        )
        "singolo" -> HomeUpdateCopy(
            badge = strings.newSingle,
            headline = title,
            detail = artist,
            caption = strings.newReleaseSubtitle,
            sourceLabel = sourceLabel,
            primaryAction = strings.open,
            icon = Icons.Rounded.MusicNote
        )
        else -> HomeUpdateCopy(
            badge = strings.newRelease,
            headline = title,
            detail = artist,
            caption = strings.newReleaseSubtitle,
            sourceLabel = sourceLabel,
            primaryAction = strings.open,
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
    onPlay: () -> Unit,
    onSave: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val track = update.track
    val accentStart = Color(track.accentStart)
    val accentEnd = Color(track.accentEnd)
    val copy = remember(track.id, track.title, track.artist, track.album, update.sourceTitle, update.verifiedRelease, strings.code) {
        buildHomeUpdateCopy(update, strings)
    }
    val availabilityLabel = when {
        isDownloaded -> strings.downloaded
        isDownloading -> strings.localizeDownloadState("DOWNLOADING")
        else -> null
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
                        if (availabilityLabel != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDownloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                                    contentDescription = null,
                                    tint = LevyraMuted,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = availabilityLabel,
                                    color = LevyraMuted,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
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
                                    text = if (isFavorite) LocalLevyraStrings.current.saved else LocalLevyraStrings.current.save,
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
    progress: Float,
    onResume: () -> Unit
) {
    val accentStart = Color(track.accentStart)
    val accentEnd = Color(track.accentEnd)
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(Dp.Hairline, LevyraAdaptiveHairline),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 70.dp)
            .pressable(onClick = onResume)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cinematicGlassBrush(accentStart, accentEnd, 0.24f))
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 44.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(accentEnd.copy(alpha = 0.16f), Color.Transparent)
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 9.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CoverImage(
                    track = track,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .border(Dp.Hairline, Color.White.copy(alpha = 0.13f), RoundedCornerShape(11.dp)),
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
                        Icon(
                            imageVector = Icons.Rounded.Headphones,
                            contentDescription = null,
                            tint = LevyraCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = LocalLevyraStrings.current.continueListening,
                            color = LevyraCyan,
                            fontSize = 9.8.sp,
                            lineHeight = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = track.title,
                        color = LevyraText,
                        fontSize = 14.sp,
                        lineHeight = 16.sp,
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
                    modifier = Modifier.size(32.dp)
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
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(3.dp)
                    .clip(RoundedCornerShape(topEnd = 3.dp))
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
                label = LocalLevyraStrings.current.mixForYou,
                accent = LevyraCyan,
                enabled = hasTracks,
                modifier = Modifier.width(176.dp),
                onClick = onShuffle
            )
        }
        item {
            QuickAction(
                icon = Icons.Rounded.Favorite,
                label = LocalLevyraStrings.current.favoritesPlain,
                accent = LevyraPink,
                enabled = true,
                modifier = Modifier.width(176.dp),
                onClick = onFavorites
            )
        }
        item {
            QuickAction(
                icon = Icons.Rounded.Bolt,
                label = LocalLevyraStrings.current.newReleases,
                accent = LevyraViolet,
                enabled = true,
                modifier = Modifier.width(176.dp),
                onClick = onNewReleases
            )
        }
        item {
            QuickAction(
                icon = Icons.Rounded.MusicNote,
                label = LocalLevyraStrings.current.genres,
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
                contentDescription = LocalLevyraStrings.current.favorite,
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
    val strings = LocalLevyraStrings.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(buttonSize)
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = strings.actions,
                tint = LevyraMuted,
                modifier = Modifier.size(iconSize)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(strings.addToQueue) },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, null) },
                onClick = {
                    expanded = false
                    onAddToQueue()
                }
            )
            DropdownMenuItem(
                text = { Text(strings.share) },
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
                    context.startActivity(Intent.createChooser(intent, strings.shareSong))
                }
            )
            DropdownMenuItem(
                text = { Text(strings.goToPlayer) },
                leadingIcon = { Icon(Icons.Rounded.PlayArrow, null) },
                onClick = {
                    expanded = false
                    onOpenPlayer()
                }
            )
            DropdownMenuItem(
                text = { Text(strings.openArtist) },
                leadingIcon = { Icon(Icons.Rounded.Person, null) },
                onClick = {
                    expanded = false
                    onArtist()
                }
            )
            DropdownMenuItem(
                text = { Text(strings.saveOffline) },
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
private fun ChartRegionRow(
    regions: List<com.luc4n3x.levyra.domain.ChartRegion>,
    selectedId: String,
    loading: Boolean,
    onSelect: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = HomeHorizontalInset,
            end = HomeHorizontalShelfEndPadding
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (loading) {
            item(key = "chart-region-loading", contentType = "chart-region-loading") {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = LevyraCyan
                )
            }
        }
        items(
            items = regions,
            key = { region -> region.id },
            contentType = { "chart-region-chip" }
        ) { region ->
            val selected = region.id == selectedId
            Surface(
                color = if (selected) LevyraCyan.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.07f),
                border = BorderStroke(
                    1.dp,
                    if (selected) LevyraCyan else Color.White.copy(alpha = 0.1f)
                ),
                shape = CircleShape,
                modifier = Modifier.pressable(onClick = { onSelect(region.id) })
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(region.emoji, fontSize = 14.sp)
                    Text(
                        text = region.label,
                        color = if (selected) LevyraCyan else LevyraText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchScreen(viewModel: SearchViewModel, state: LevyraUiState) {
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
    val strings = LocalLevyraStrings.current

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
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
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
                            Text(strings.searchPlaceholder, color = LevyraMuted, fontWeight = FontWeight.Medium, fontSize = 15.sp)
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
                            contentDescription = strings.clear,
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
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.forLanguageTag(strings.code).toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.forLanguageTag(strings.code).toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, strings.listeningPrompt)
                }
                try {
                    speechRecognizerLauncher.launch(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, strings.voiceSearchUnsupported, Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.05f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Rounded.Mic,
                contentDescription = strings.voice,
                tint = LevyraText,
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(
            onClick = { Toast.makeText(context, strings.musicFiltersComingSoon, Toast.LENGTH_SHORT).show() },
            modifier = Modifier
                .size(40.dp)
                .background(Color.White.copy(alpha = 0.05f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Rounded.Equalizer,
                contentDescription = strings.audioEngine,
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
    val strings = LocalLevyraStrings.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = strings.recentSearches,
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
                                    contentDescription = strings.actions,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (isFavorite) strings.removeFromFavorites else strings.addToFavorites) },
                                    leadingIcon = { Icon(if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null) },
                                    onClick = {
                                        menuExpanded = false
                                        onFavorite(track)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.playNext) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, null) },
                                    onClick = {
                                        menuExpanded = false
                                        onPlayNext(track)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.addToQueue) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, null) },
                                    onClick = {
                                        menuExpanded = false
                                        onAddToQueue(track)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.addToPlaylist) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) },
                                    onClick = {
                                        menuExpanded = false
                                        onAddToPlaylist(track)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (isDownloaded) strings.alreadyOffline else strings.download) },
                                    leadingIcon = { Icon(if (isDownloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download, null) },
                                    onClick = {
                                        menuExpanded = false
                                        if (!isDownloaded) onDownload(track)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.openArtist) },
                                    leadingIcon = { Icon(Icons.Rounded.Person, null) },
                                    onClick = {
                                        menuExpanded = false
                                        onArtist(track)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.share) },
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
                                        context.startActivity(Intent.createChooser(intent, strings.shareSong))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.removeFromRecentSearches) },
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
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = LocalLevyraStrings.current.complete,
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
    val strings = LocalLevyraStrings.current
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
                    text = LocalLevyraStrings.current.downloadsFolder,
                    color = LevyraText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = strings.formatDownloadedTrackCount(count),
                    color = LevyraMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
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
    val strings = LocalLevyraStrings.current
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
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = LocalLevyraStrings.current.back, tint = LevyraText)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = LocalLevyraStrings.current.offlineDownloadsPlain,
                            color = LevyraText,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = strings.formatSavedTrackCount(state.downloads.size),
                            color = LevyraMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            if (state.downloads.isEmpty()) {
                item {
                    EmptyState(strings.noOfflineDownloads)
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
    viewModel: LibraryViewModel,
    state: LevyraUiState,
    listState: LazyListState,
    onOpenDownloads: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    var addTarget by remember { mutableStateOf<Track?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf(LibraryFilter.All) }
    val showPlaylists = filter == LibraryFilter.All || filter == LibraryFilter.Playlists
    val showDownloads = filter == LibraryFilter.All || filter == LibraryFilter.Downloads
    val showSongs = filter == LibraryFilter.All || filter == LibraryFilter.Songs
    val showArtists = filter == LibraryFilter.All || filter == LibraryFilter.Artists
    val showPulse = filter == LibraryFilter.All
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = if (state.currentTrack != null) 194.dp else 108.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item(key = "lib-header", contentType = "lib-header") {
                LibraryHeader(
                    title = strings.libraryTitle,
                    subtitle = strings.librarySubtitle,
                    playlistCount = state.playlists.size,
                    downloadCount = state.downloads.size,
                    favoriteCount = state.favorites.size
                )
            }

            item(key = "lib-filters", contentType = "lib-filters") {
                LibraryFilterChips(
                    selected = filter,
                    playlistCount = state.playlists.size,
                    songCount = state.favorites.size + state.recentListens.size,
                    artistCount = state.followedArtists.size,
                    downloadCount = state.downloads.size,
                    onSelect = { filter = it }
                )
            }

            if (showPlaylists) {
                item(key = "lib-playlists-header", contentType = "lib-section-header") {
                    LibrarySectionHeader(
                        title = cleanLibraryLabel(strings.playlists),
                        detail = strings.personalPlaylists,
                        count = state.playlists.size,
                        icon = Icons.AutoMirrored.Rounded.QueueMusic,
                        accent = LevyraViolet,
                        actionLabel = strings.newItem,
                        onAction = { showCreate = true }
                    )
                }
                if (state.playlists.isEmpty()) {
                    item(key = "lib-playlists-empty", contentType = "lib-empty") {
                        LibraryEmptyState(
                            icon = Icons.AutoMirrored.Rounded.QueueMusic,
                            title = strings.createFirstPlaylist,
                            detail = strings.createFirstPlaylistSubtitle,
                            accent = LevyraViolet,
                            actionLabel = strings.newItem,
                            onAction = { showCreate = true }
                        )
                    }
                } else {
                    items(state.playlists, key = { "pl-${it.id}" }, contentType = { "lib-playlist" }) { playlist ->
                        PlaylistRow(
                            playlist = playlist,
                            onOpen = { viewModel.openPlaylist(playlist.id) },
                            onPlay = { viewModel.playPlaylist(playlist.id) },
                            onDelete = { viewModel.deletePlaylist(playlist.id) }
                        )
                    }
                }
            }

            if (showDownloads) {
                item(key = "lib-downloads-header", contentType = "lib-section-header") {
                    LibrarySectionHeader(
                        title = cleanLibraryLabel(strings.downloads),
                        detail = strings.offline,
                        count = state.downloads.size,
                        icon = Icons.Rounded.DownloadDone,
                        accent = LevyraCyan
                    )
                }
                if (state.downloads.isEmpty()) {
                    item(key = "lib-downloads-empty", contentType = "lib-empty") {
                        LibraryEmptyState(
                            icon = Icons.Rounded.Download,
                            title = strings.noOfflineDownloads,
                            detail = strings.downloadTrackHint,
                            accent = LevyraCyan
                        )
                    }
                } else {
                    item(key = "lib-downloads-card", contentType = "lib-downloads-card") {
                        DownloadsFolderCard(
                            count = state.downloads.size,
                            onClick = onOpenDownloads
                        )
                    }
                }
            }

            if (showSongs) {
                item(key = "lib-favorites-header", contentType = "lib-section-header") {
                    LibrarySectionHeader(
                        title = cleanLibraryLabel(strings.favorites),
                        detail = strings.savedTracks,
                        count = state.favorites.size,
                        icon = Icons.Rounded.Favorite,
                        accent = LevyraPink
                    )
                }
                if (state.favorites.isEmpty()) {
                    item(key = "lib-favorites-empty", contentType = "lib-empty") {
                        LibraryEmptyState(
                            icon = Icons.Rounded.FavoriteBorder,
                            title = strings.favoritesEmpty,
                            detail = strings.tapHeartToAdd,
                            accent = LevyraPink
                        )
                    }
                } else {
                    item(key = "lib-favorites-carousel", contentType = "lib-carousel") {
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
            }

            if (showArtists && state.followedArtists.isNotEmpty()) {
                item(key = "lib-artists-header", contentType = "lib-section-header") {
                    LibrarySectionHeader(
                        title = strings.followedArtistsTitle,
                        detail = strings.followedArtistsSubtitle,
                        count = state.followedArtists.size,
                        icon = Icons.Rounded.Person,
                        accent = CinematicGold
                    )
                }
                item(key = "lib-artists-row", contentType = "lib-artists-row") {
                    FollowedArtistsRow(
                        artists = state.followedArtists,
                        onOpen = { viewModel.openArtistByName(it.name) }
                    )
                }
            }

            if (showArtists && state.followedArtists.isEmpty() && filter == LibraryFilter.Artists) {
                item(key = "lib-artists-empty", contentType = "lib-empty") {
                    LibraryEmptyState(
                        icon = Icons.Rounded.Person,
                        title = strings.followedArtistsTitle,
                        detail = strings.followedArtistsSubtitle,
                        accent = CinematicGold
                    )
                }
            }

            if (showPulse) {
                item(key = "lib-pulse-divider", contentType = "lib-divider") {
                    LibrarySectionDivider(label = strings.pulseSectionBand)
                }
                item(key = "lib-pulse-header", contentType = "lib-section-header") {
                    LibrarySectionHeader(
                        title = strings.pulseTitle,
                        detail = strings.pulseSubtitle,
                        count = state.listeningPulse.plays,
                        icon = Icons.Rounded.Insights,
                        accent = LevyraCyan
                    )
                }
                item(key = "lib-pulse-card", contentType = "lib-pulse-card") {
                    ListeningPulseCard(pulse = state.listeningPulse, strings = strings)
                }
            }

            if (showSongs) {
                item(key = "lib-history-header", contentType = "lib-section-header") {
                    LibrarySectionHeader(
                        title = strings.listeningHistory,
                        detail = strings.listeningHistorySubtitle,
                        count = state.recentListens.size,
                        icon = Icons.Rounded.History,
                        accent = LevyraViolet
                    )
                }
                if (state.recentListens.isEmpty()) {
                    item(key = "lib-history-empty", contentType = "lib-empty") {
                        LibraryEmptyState(
                            icon = Icons.Rounded.History,
                            title = strings.listeningHistoryEmptyTitle,
                            detail = strings.listeningHistoryEmptyDetail,
                            accent = LevyraViolet
                        )
                    }
                } else {
                    item(key = "lib-history-recap", contentType = "lib-history-recap") {
                        ListeningHistoryRecapCard(
                            tracks = state.recentListens,
                            currentTrackId = state.currentTrack?.id,
                            isPlaying = state.isPlaying,
                            isResolving = state.isResolving,
                            onPlay = { track -> viewModel.playFrom(state.recentListens, track) }
                        )
                    }
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
private fun ListeningHistoryRecapCard(
    tracks: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: (Track) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val recentTracks = remember(tracks) {
        tracks
            .distinctBy { track ->
                track.id.ifBlank { "${track.title.lowercase()}|${track.artist.lowercase()}" }
            }
            .take(6)
    }
    val latest = recentTracks.first()
    val visibleArtwork = recentTracks.take(5)
    val uniqueArtists = remember(tracks) {
        tracks.map { it.artist.trim().lowercase() }.filter { it.isNotBlank() }.distinct().size
    }
    val uniqueTracks = remember(tracks) {
        tracks.distinctBy { it.id.ifBlank { "${it.title.lowercase()}|${it.artist.lowercase()}" } }.size
    }
    val primary = remember(latest.accentStart) { Color(latest.accentStart) }
    val secondary = remember(latest.accentEnd) { Color(latest.accentEnd) }
    val stackWidth = ((visibleArtwork.size - 1).coerceAtLeast(0) * 34 + 62).dp
    val latestIsCurrent = latest.id == currentTrackId
    val playingLatest = latestIsCurrent && isPlaying
    val resolvingLatest = latestIsCurrent && isResolving

    Surface(
        color = LevyraPanel.copy(alpha = 0.84f),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
        modifier = Modifier
            .fillMaxWidth()
            .pressable(pressedScale = 0.985f) { onPlay(latest) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            primary.copy(alpha = 0.18f),
                            LevyraPanel.copy(alpha = 0.90f),
                            secondary.copy(alpha = 0.12f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 17.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = if (strings.code == "it") "LA TUA SCIA RECENTE" else "YOUR RECENT TRAIL",
                            color = primary.playerMix(Color.White, 0.62f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.15.sp
                        )
                        Text(
                            text = if (strings.code == "it") {
                                "${tracks.size} passaggi · $uniqueArtists artisti"
                            } else {
                                "${tracks.size} plays · $uniqueArtists artists"
                            },
                            color = Color.White.copy(alpha = 0.60f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Surface(
                        color = Color.Black.copy(alpha = 0.24f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                    ) {
                        Text(
                            text = if (strings.code == "it") "$uniqueTracks unici" else "$uniqueTracks unique",
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(stackWidth)
                            .height(66.dp)
                    ) {
                        visibleArtwork.forEachIndexed { index, track ->
                            val selected = track.id == currentTrackId
                            AsyncImage(
                                model = track.thumbnailUrl.ifBlank { track.largeThumbnailUrl },
                                contentDescription = track.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .offset(x = (index * 34).dp)
                                    .zIndex(index.toFloat())
                                    .size(62.dp)
                                    .shadow(8.dp, RoundedCornerShape(17.dp))
                                    .clip(RoundedCornerShape(17.dp))
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) {
                                            primary.playerMix(Color.White, 0.34f)
                                        } else {
                                            Color.White.copy(alpha = 0.16f)
                                        },
                                        shape = RoundedCornerShape(17.dp)
                                    )
                                    .pressable(pressedScale = 0.94f) { onPlay(track) }
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (strings.code == "it") "Ultimo ascolto" else "Last played",
                            color = Color.White.copy(alpha = 0.48f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = latest.title,
                            color = Color.White,
                            fontSize = 17.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = latest.artist,
                            color = Color.White.copy(alpha = 0.58f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(43.dp)
                            .background(
                                brush = Brush.linearGradient(listOf(primary, secondary)),
                                shape = CircleShape
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            resolvingLatest -> CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.4.dp,
                                color = Color.White
                            )
                            playingLatest -> Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(23.dp)
                            )
                            else -> Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(25.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ListeningHistoryMetric(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Headphones,
                        value = tracks.size.toString(),
                        label = if (strings.code == "it") "ascolti" else "plays",
                        accent = primary
                    )
                    ListeningHistoryMetric(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Person,
                        value = uniqueArtists.toString(),
                        label = if (strings.code == "it") "artisti" else "artists",
                        accent = secondary
                    )
                    ListeningHistoryMetric(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.MusicNote,
                        value = uniqueTracks.toString(),
                        label = if (strings.code == "it") "tracce" else "tracks",
                        accent = primary.playerMix(secondary, 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ListeningHistoryMetric(
    modifier: Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    accent: Color
) {
    Surface(
        color = Color.Black.copy(alpha = 0.20f),
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent.playerMix(Color.White, 0.42f),
                modifier = Modifier.size(16.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.46f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

private enum class LibraryFilter { All, Playlists, Songs, Artists, Downloads }

@Composable
private fun LibraryFilterChips(
    selected: LibraryFilter,
    playlistCount: Int,
    songCount: Int,
    artistCount: Int,
    downloadCount: Int,
    onSelect: (LibraryFilter) -> Unit
) {
    val strings = LocalLevyraStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        LibraryFilterChip(strings.all, Icons.Rounded.LibraryMusic, LevyraCyan, null, selected == LibraryFilter.All) { onSelect(LibraryFilter.All) }
        LibraryFilterChip(cleanLibraryLabel(strings.playlists), Icons.AutoMirrored.Rounded.QueueMusic, LevyraViolet, playlistCount, selected == LibraryFilter.Playlists) { onSelect(LibraryFilter.Playlists) }
        LibraryFilterChip(cleanLibraryLabel(strings.songs), Icons.Rounded.MusicNote, LevyraPink, songCount, selected == LibraryFilter.Songs) { onSelect(LibraryFilter.Songs) }
        LibraryFilterChip(cleanLibraryLabel(strings.artists), Icons.Rounded.Person, CinematicGold, artistCount, selected == LibraryFilter.Artists) { onSelect(LibraryFilter.Artists) }
        LibraryFilterChip(cleanLibraryLabel(strings.downloads), Icons.Rounded.DownloadDone, LevyraCyan, downloadCount, selected == LibraryFilter.Downloads) { onSelect(LibraryFilter.Downloads) }
    }
}

@Composable
private fun LibraryFilterChip(
    label: String,
    icon: ImageVector,
    accent: Color,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) accent.copy(alpha = 0.18f) else CinematicGlass.copy(alpha = 0.5f)
    val borderColor = if (selected) accent.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.08f)
    val contentColor = if (selected) LevyraText else LevyraMuted
    Surface(
        color = background,
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier.pressable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(icon, null, tint = if (selected) accent else LevyraMuted, modifier = Modifier.size(16.dp))
            Text(label, color = contentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            if (count != null && count > 0) {
                Text(count.toString(), color = if (selected) accent else LevyraMuted.copy(alpha = 0.75f), fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
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
        val strings = LocalLevyraStrings.current
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LibraryStatPill(Icons.AutoMirrored.Rounded.QueueMusic, playlistCount.toString(), strings.playlistsPlain, LevyraViolet)
            LibraryStatPill(Icons.Rounded.DownloadDone, downloadCount.toString(), strings.offline, LevyraCyan)
            LibraryStatPill(Icons.Rounded.Favorite, favoriteCount.toString(), strings.favoritesPlain, LevyraPink)
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
            .padding(top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(LevyraAdaptiveSoftHairline)
        )
        Text(
            text = label.uppercase(),
            color = LevyraMuted,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.8.sp
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(0.5.dp)
                .background(LevyraAdaptiveSoftHairline)
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
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = title,
                    color = LevyraText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "($count)",
                    color = LevyraMuted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = detail,
                color = LevyraMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (actionLabel != null && onAction != null) {
            IconButton(
                onClick = onAction,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.06f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = cleanLibraryLabel(actionLabel),
                    tint = LevyraText,
                    modifier = Modifier.size(20.dp)
                )
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
        color = Color.White.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        shape = RoundedCornerShape(16.dp),
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
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = LevyraText, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(detail, color = LevyraMuted, fontSize = 12.5.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (actionLabel != null && onAction != null) {
                IconButton(
                    onClick = onAction,
                    modifier = Modifier
                        .size(36.dp)
                        .background(accent.copy(alpha = 0.16f), CircleShape)
                        .border(BorderStroke(1.dp, accent.copy(alpha = 0.28f)), CircleShape)
                ) {
                    Icon(Icons.Rounded.Add, null, tint = accent, modifier = Modifier.size(18.dp))
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
                if (download.embeddedMetadata) "Music/Levyra · ${LocalLevyraStrings.current.coverAndTags}" else "Music/Levyra",
                color = LevyraMuted.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.Delete, contentDescription = LocalLevyraStrings.current.remove, tint = LevyraMuted, modifier = Modifier.size(22.dp))
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
            .clip(RoundedCornerShape(14.dp))
            .pressable(onClick = onOpen)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.05f)),
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
                Icon(Icons.AutoMirrored.Rounded.QueueMusic, null, tint = LevyraMuted, modifier = Modifier.size(26.dp))
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(playlist.name, color = LevyraText, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                LocalLevyraStrings.current.formatTrackCount(playlist.size),
                color = LevyraMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium
            )
        }
        IconButton(onClick = onPlay, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = LocalLevyraStrings.current.play, tint = LevyraMuted, modifier = Modifier.size(24.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.Delete, contentDescription = LocalLevyraStrings.current.delete, tint = LevyraMuted, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PlaylistCreateDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LevyraPanel,
        title = { Text(LocalLevyraStrings.current.newPlaylist, color = LevyraText, fontWeight = FontWeight.Black) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text(LocalLevyraStrings.current.playlistName, color = LevyraMuted) },
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
                Text(LocalLevyraStrings.current.create, color = LevyraCyan, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(LocalLevyraStrings.current.cancel, color = LevyraMuted) }
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
    val strings = LocalLevyraStrings.current
    var creating by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LevyraPanel,
        title = { Text(LocalLevyraStrings.current.addToPlaylist, color = LevyraText, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (creating) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        placeholder = { Text(LocalLevyraStrings.current.newPlaylistName, color = LevyraMuted) },
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
                        Text(LocalLevyraStrings.current.createNewPlaylist, color = LevyraCyan, fontWeight = FontWeight.Bold)
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
                            Icon(Icons.AutoMirrored.Rounded.QueueMusic, null, tint = LevyraMuted)
                            Text(pl.name, color = LevyraText, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (creating) {
                TextButton(onClick = { if (name.isNotBlank()) onCreateWith(name) }) {
                    Text(LocalLevyraStrings.current.createAndAdd, color = LevyraCyan, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.close, color = LevyraMuted) }
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
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = LocalLevyraStrings.current.back, tint = LevyraText)
                    }
                    Spacer(Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(playlist.name, color = LevyraText, fontSize = 26.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(LocalLevyraStrings.current.formatTrackCount(playlist.size), color = LevyraMuted, fontSize = 14.sp)
                    }
                    if (playlist.tracks.isNotEmpty()) {
                        IconButton(onClick = { viewModel.exportOpenPlaylist() }) {
                            Icon(Icons.Rounded.DownloadDone, contentDescription = LocalLevyraStrings.current.downloadPlaylist, tint = LevyraViolet, modifier = Modifier.size(27.dp))
                        }
                        IconButton(onClick = { viewModel.playPlaylist(playlist.id) }) {
                            Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = LocalLevyraStrings.current.playAll, tint = LevyraCyan, modifier = Modifier.size(30.dp))
                        }
                    }
                }
            }
            if (playlist.tracks.isEmpty()) {
                item { EmptyState(LocalLevyraStrings.current.playlistEmpty) }
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

private fun Color.playerMix(other: Color, amount: Float): Color {
    val fraction = amount.coerceIn(0f, 1f)
    return Color(
        red = red + (other.red - red) * fraction,
        green = green + (other.green - green) * fraction,
        blue = blue + (other.blue - blue) * fraction,
        alpha = alpha + (other.alpha - alpha) * fraction
    )
}

private val PlayerDarkSurface = Color(0xFF0B0B10)
private const val PlayerMinimumContrast = 4.5f
private const val PlayerStrongContrast = 7f

private data class PlayerContrastAdjustment(
    val color: Color,
    val amount: Float,
    val valid: Boolean
)

private data class PlayerContrastGradient(
    val start: Color,
    val end: Color,
    val content: Color
)

private fun Color.playerCompositeOver(background: Color): Color {
    val foregroundAlpha = alpha.coerceIn(0f, 1f)
    val backgroundAlpha = background.alpha.coerceIn(0f, 1f)
    val outputAlpha = foregroundAlpha + backgroundAlpha * (1f - foregroundAlpha)
    if (outputAlpha <= 0f) return Color.Transparent
    return Color(
        red = (red * foregroundAlpha + background.red * backgroundAlpha * (1f - foregroundAlpha)) / outputAlpha,
        green = (green * foregroundAlpha + background.green * backgroundAlpha * (1f - foregroundAlpha)) / outputAlpha,
        blue = (blue * foregroundAlpha + background.blue * backgroundAlpha * (1f - foregroundAlpha)) / outputAlpha,
        alpha = outputAlpha
    )
}

private fun playerContrastRatio(foreground: Color, background: Color): Float {
    val opaqueBackground = background.playerCompositeOver(Color.Black).copy(alpha = 1f)
    val opaqueForeground = foreground.playerCompositeOver(opaqueBackground).copy(alpha = 1f)
    val foregroundLuminance = opaqueForeground.luminance()
    val backgroundLuminance = opaqueBackground.luminance()
    val lighter = maxOf(foregroundLuminance, backgroundLuminance)
    val darker = minOf(foregroundLuminance, backgroundLuminance)
    return (lighter + 0.05f) / (darker + 0.05f)
}

private fun Color.playerAdjustForegroundToward(
    target: Color,
    backgrounds: List<Color>,
    minimumContrast: Float
): PlayerContrastAdjustment {
    val source = copy(alpha = 1f)
    val opaqueTarget = target.copy(alpha = 1f)
    if (backgrounds.all { playerContrastRatio(source, it) >= minimumContrast }) {
        return PlayerContrastAdjustment(source, 0f, true)
    }
    if (backgrounds.any { playerContrastRatio(opaqueTarget, it) < minimumContrast }) {
        return PlayerContrastAdjustment(opaqueTarget, 1f, false)
    }
    var low = 0f
    var high = 1f
    repeat(24) {
        val middle = (low + high) / 2f
        val candidate = source.playerMix(opaqueTarget, middle).copy(alpha = 1f)
        if (backgrounds.all { playerContrastRatio(candidate, it) >= minimumContrast }) {
            high = middle
        } else {
            low = middle
        }
    }
    return PlayerContrastAdjustment(source.playerMix(opaqueTarget, high).copy(alpha = 1f), high, true)
}

private fun Color.playerContentColor(
    backgrounds: List<Color>,
    minimumContrast: Float = PlayerMinimumContrast
): Color {
    val white = playerAdjustForegroundToward(Color.White, backgrounds, minimumContrast)
    val black = playerAdjustForegroundToward(Color.Black, backgrounds, minimumContrast)
    return when {
        white.valid && black.valid -> if (white.amount <= black.amount) white.color else black.color
        white.valid -> white.color
        black.valid -> black.color
        else -> if (backgrounds.sumOf { it.luminance().toDouble() } / backgrounds.size.coerceAtLeast(1) < 0.5) Color.White else Color.Black
    }
}

private fun Color.playerAdjustBackgroundFor(
    content: Color,
    minimumContrast: Float
): PlayerContrastAdjustment {
    val source = copy(alpha = 1f)
    if (playerContrastRatio(content, source) >= minimumContrast) {
        return PlayerContrastAdjustment(source, 0f, true)
    }
    val target = if (content.luminance() >= 0.5f) Color.Black else Color.White
    if (playerContrastRatio(content, target) < minimumContrast) {
        return PlayerContrastAdjustment(target, 1f, false)
    }
    var low = 0f
    var high = 1f
    repeat(24) {
        val middle = (low + high) / 2f
        val candidate = source.playerMix(target, middle).copy(alpha = 1f)
        if (playerContrastRatio(content, candidate) >= minimumContrast) {
            high = middle
        } else {
            low = middle
        }
    }
    return PlayerContrastAdjustment(source.playerMix(target, high).copy(alpha = 1f), high, true)
}

private fun playerContrastGradient(
    start: Color,
    end: Color,
    minimumContrast: Float = PlayerMinimumContrast
): PlayerContrastGradient {
    fun candidate(content: Color): Pair<PlayerContrastGradient, Float>? {
        val safeStart = start.playerAdjustBackgroundFor(content, minimumContrast)
        val safeEnd = end.playerAdjustBackgroundFor(content, minimumContrast)
        if (!safeStart.valid || !safeEnd.valid) return null
        return PlayerContrastGradient(safeStart.color, safeEnd.color, content) to safeStart.amount + safeEnd.amount
    }
    val white = candidate(Color.White)
    val black = candidate(Color.Black)
    return when {
        white != null && black != null -> if (white.second <= black.second) white.first else black.first
        white != null -> white.first
        black != null -> black.first
        else -> PlayerContrastGradient(Color.Black, Color.Black, Color.White)
    }
}

private fun Color.playerMutedContentColor(
    backgrounds: List<Color>,
    minimumContrast: Float = PlayerMinimumContrast
): Color {
    val source = copy(alpha = 1f)
    if (backgrounds.any { playerContrastRatio(source, it) < minimumContrast }) return source
    val averageBackground = backgrounds.fold(Color.Transparent) { accumulator, color ->
        if (accumulator == Color.Transparent) color.copy(alpha = 1f) else accumulator.playerMix(color.copy(alpha = 1f), 0.5f)
    }
    var low = 0f
    var high = 1f
    repeat(24) {
        val middle = (low + high) / 2f
        val candidate = source.playerMix(averageBackground, middle).copy(alpha = 1f)
        if (backgrounds.all { playerContrastRatio(candidate, it) >= minimumContrast }) {
            low = middle
        } else {
            high = middle
        }
    }
    return source.playerMix(averageBackground, low).copy(alpha = 1f)
}

@Composable
private fun PlayerImmersiveBackdrop(
    primaryTarget: Color,
    secondaryTarget: Color,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val primary by animateColorAsState(
        targetValue = primaryTarget,
        animationSpec = tween(850, easing = LinearOutSlowInEasing),
        label = "player-backdrop-primary"
    )
    val secondary by animateColorAsState(
        targetValue = secondaryTarget,
        animationSpec = tween(850, easing = LinearOutSlowInEasing),
        label = "player-backdrop-secondary"
    )
    val intensity by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.78f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "player-backdrop-intensity"
    )
    val primarySurface = remember(primary) {
        primary.playerAdjustBackgroundFor(Color.White, PlayerStrongContrast).color
    }
    val secondarySurface = remember(secondary) {
        secondary.playerAdjustBackgroundFor(Color.White, PlayerStrongContrast).color
    }
    val mixedSurface = remember(primary, secondary) {
        primary.playerMix(secondary, 0.5f).playerAdjustBackgroundFor(Color.White, PlayerStrongContrast).color
    }

    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        primarySurface.playerMix(Color.Black, 0.18f),
                        mixedSurface.playerMix(Color.Black, 0.32f),
                        secondarySurface.playerMix(Color.Black, 0.46f),
                        Color(0xFF050508),
                        Color.Black
                    )
                )
            )
            .drawBehind {
                if (size.minDimension <= 0f) return@drawBehind
                val topCenter = androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height * 0.08f)
                val sideCenter = androidx.compose.ui.geometry.Offset(size.width * 0.95f, size.height * 0.34f)
                val lowerCenter = androidx.compose.ui.geometry.Offset(size.width * 0.38f, size.height * 0.76f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primarySurface.playerMix(Color.White, 0.08f).copy(alpha = 0.42f * intensity),
                            primarySurface.copy(alpha = 0.16f * intensity),
                            Color.Transparent
                        ),
                        center = topCenter,
                        radius = size.width * 1.08f
                    ),
                    radius = size.width * 1.08f,
                    center = topCenter
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            secondarySurface.playerMix(Color.White, 0.06f).copy(alpha = 0.30f * intensity),
                            secondarySurface.copy(alpha = 0.10f * intensity),
                            Color.Transparent
                        ),
                        center = sideCenter,
                        radius = size.width * 0.88f
                    ),
                    radius = size.width * 0.88f,
                    center = sideCenter
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            mixedSurface.copy(alpha = 0.17f * intensity),
                            Color.Transparent
                        ),
                        center = lowerCenter,
                        radius = size.width * 0.86f
                    ),
                    radius = size.width * 0.86f,
                    center = lowerCenter
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.03f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.22f),
                            Color.Black.copy(alpha = 0.74f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun PlayerArtworkCanvas(
    track: Track,
    artworkUrl: String,
    isPlaying: Boolean,
    cornerRadius: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.965f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow),
        label = "player-artwork-stage-scale"
    )
    val haloScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.04f else 0.97f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow),
        label = "player-artwork-halo-scale"
    )
    val haloAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 0.92f else 0.64f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "player-artwork-halo-alpha"
    )
    val primary = Color(track.accentStart)
    val secondary = Color(track.accentEnd)
    val artworkShape = RoundedCornerShape(cornerRadius)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.98f)
                .graphicsLayer {
                    scaleX = haloScale
                    scaleY = haloScale
                    alpha = haloAlpha
                }
                .background(
                    Brush.radialGradient(
                        listOf(
                            primary.playerMix(Color.White, 0.18f).copy(alpha = 0.72f),
                            secondary.copy(alpha = 0.38f),
                            primary.playerMix(Color.Black, 0.58f).copy(alpha = 0.18f),
                            Color.Transparent
                        )
                    ),
                    RoundedCornerShape(cornerRadius + 30.dp)
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize(0.865f)
                .graphicsLayer {
                    scaleX = artworkScale
                    scaleY = artworkScale
                    shadowElevation = if (isPlaying) 34f else 18f
                    shape = artworkShape
                    clip = true
                }
                .background(primary.playerMix(Color.Black, 0.76f), artworkShape)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.20f),
                    shape = artworkShape
                )
        ) {
            if (artworkUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(artworkUrl)
                        .crossfade(true)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                InstantArtworkPlaceholder(track = track, modifier = Modifier.fillMaxSize())
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.13f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.22f)
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.22f))
            )
        }
    }
}

@Composable
private fun PlayerModeSwitch(
    isVideoMode: Boolean,
    activeColor: Color,
    onSong: () -> Unit,
    onVideo: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    Row(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.24f), RoundedCornerShape(500.dp))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(500.dp))
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val songBackground by animateColorAsState(
            targetValue = if (!isVideoMode) activeColor.copy(alpha = 0.42f) else Color.Transparent,
            animationSpec = tween(180),
            label = "player-mode-song"
        )
        val videoBackground by animateColorAsState(
            targetValue = if (isVideoMode) activeColor.copy(alpha = 0.42f) else Color.Transparent,
            animationSpec = tween(180),
            label = "player-mode-video"
        )
        val selectedContent = remember(activeColor) {
            Color.White.playerContentColor(
                listOf(activeColor.copy(alpha = 0.42f).playerCompositeOver(PlayerDarkSurface))
            )
        }
        Box(
            modifier = Modifier
                .background(songBackground, RoundedCornerShape(500.dp))
                .pressable(enabled = isVideoMode, onClick = onSong)
                .padding(horizontal = 13.dp, vertical = 6.dp)
        ) {
            Text(
                text = strings.song,
                color = if (!isVideoMode) selectedContent else Color.White.copy(alpha = 0.72f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier
                .background(videoBackground, RoundedCornerShape(500.dp))
                .pressable(enabled = !isVideoMode, onClick = onVideo)
                .padding(horizontal = 13.dp, vertical = 6.dp)
        ) {
            Text(
                text = strings.video,
                color = if (isVideoMode) selectedContent else Color.White.copy(alpha = 0.72f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LevyraControlPulseHandle(
    expanded: Boolean,
    compact: Boolean,
    activeColor: Color,
    secondaryColor: Color,
    hasActiveState: Boolean,
    onToggle: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val width = if (compact) 88.dp else 94.dp
    val height = if (compact) 29.dp else 31.dp
    val lineWidth = if (compact) 18.dp else 20.dp
    val iconBoxSize = if (compact) 21.dp else 23.dp
    val iconSize = if (compact) 17.dp else 18.dp
    val glow = if (expanded) 0.34f else 0.22f
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "player-shelf-chevron"
    )

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.24f),
            border = BorderStroke(
                1.dp,
                Color.White.copy(alpha = if (expanded) 0.17f else 0.10f)
            ),
            shape = RoundedCornerShape(500.dp),
            modifier = Modifier
                .width(width)
                .height(height)
                .shadow(3.dp, RoundedCornerShape(500.dp))
                .pressable(pressedScale = 0.98f, onClick = onToggle)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                activeColor.copy(alpha = 0.06f),
                                activeColor.copy(alpha = glow),
                                secondaryColor.copy(alpha = glow),
                                secondaryColor.copy(alpha = 0.06f)
                            )
                        ),
                        shape = RoundedCornerShape(500.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(lineWidth)
                            .height(2.dp)
                            .background(activeColor.copy(alpha = 0.68f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(iconBoxSize)
                            .graphicsLayer { rotationZ = rotation }
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(
                                        activeColor.copy(alpha = 0.46f),
                                        secondaryColor.copy(alpha = 0.46f)
                                    )
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = strings.options,
                            tint = Color.White,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(lineWidth)
                            .height(2.dp)
                            .background(secondaryColor.copy(alpha = 0.68f), CircleShape)
                    )
                }
                if (hasActiveState && !expanded) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 5.dp, end = 8.dp)
                            .size(5.dp)
                            .background(Color.White, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerUtilityDock(
    activeColor: Color,
    secondaryColor: Color,
    lyricsAvailable: Boolean,
    isExporting: Boolean,
    isDownloaded: Boolean,
    compact: Boolean,
    onQueue: () -> Unit,
    onLyrics: () -> Unit,
    onDownload: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    Surface(
        color = Color.Black.copy(alpha = 0.22f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.11f)),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = if (compact) 7.dp else 8.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerDockAction(
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                label = strings.queue,
                tint = Color.White.copy(alpha = 0.86f),
                compact = compact,
                onClick = onQueue
            )
            PlayerDockAction(
                icon = Icons.AutoMirrored.Rounded.Subject,
                label = strings.lyrics,
                tint = if (lyricsAvailable) activeColor else Color.White.copy(alpha = 0.70f),
                compact = compact,
                onClick = onLyrics
            )
            PlayerDockAction(
                icon = if (isDownloaded) Icons.Rounded.DownloadDone else Icons.Rounded.Download,
                label = when {
                    isExporting -> strings.downloadInProgress
                    isDownloaded -> strings.downloaded
                    else -> strings.download
                },
                tint = if (isExporting || isDownloaded) secondaryColor else Color.White.copy(alpha = 0.70f),
                isBusy = isExporting,
                enabled = !isExporting,
                compact = compact,
                onClick = onDownload
            )
        }
    }
}

@Composable
private fun RowScope.PlayerDockAction(
    icon: ImageVector,
    label: String,
    tint: Color,
    isBusy: Boolean = false,
    enabled: Boolean = true,
    compact: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .height(if (compact) 58.dp else 62.dp)
            .pressable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isBusy) {
            CircularProgressIndicator(
                modifier = Modifier.size(if (compact) 20.dp else 21.dp),
                strokeWidth = 2.4.dp,
                color = tint
            )
        } else {
            Icon(
                icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(if (compact) 22.dp else 23.dp)
            )
        }
        Spacer(modifier = Modifier.height(if (compact) 4.dp else 5.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.68f),
            fontSize = if (compact) 9.5.sp else 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun compactYoutubeCount(value: Long): String {
    val absolute = kotlin.math.abs(value.toDouble())
    val (scaled, suffix) = when {
        absolute >= 1_000_000_000.0 -> value / 1_000_000_000.0 to "B"
        absolute >= 1_000_000.0 -> value / 1_000_000.0 to "M"
        absolute >= 1_000.0 -> value / 1_000.0 to "K"
        else -> return value.toString()
    }
    val pattern = if (kotlin.math.abs(scaled) >= 100.0) "%.0f" else "%.1f"
    return String.format(Locale.getDefault(), pattern, scaled)
        .replace(Regex("[,.]0$"), "") + suffix
}

@Composable
private fun PlayerYoutubeEngagementRow(
    track: Track,
    engagement: YoutubeEngagementState,
    primary: Color,
    secondary: Color,
    compact: Boolean,
    onComments: () -> Unit
) {
    val hasLikes = track.youtubeLikeCount >= 0L
    val hasDislikeEstimate = engagement.dislikeEstimateAvailable && engagement.estimatedDislikeCount >= 0L
    val comments = engagement.comments
    val commentBadge = youtubeCommentCountBadge(comments.countText)
    val canOpenComments = engagement.videoId.isNotBlank()
    val visible = hasLikes || hasDislikeEstimate || engagement.dislikeEstimateLoading || canOpenComments

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(220)) + slideInVertically(initialOffsetY = { it / 3 }),
        exit = fadeOut(animationSpec = tween(140))
    ) {
        Column(
            modifier = Modifier.padding(top = if (compact) 9.dp else 11.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 6.dp)
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 9.dp),
                contentPadding = PaddingValues(end = 10.dp)
            ) {
                item(key = "youtube-votes") {
                    Surface(
                        color = Color.White.copy(alpha = 0.085f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.105f)),
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.height(if (compact) 40.dp else 42.dp),
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
                                    modifier = Modifier.size(if (compact) 19.dp else 20.dp)
                                )
                                if (hasLikes) {
                                    Text(
                                        text = compactYoutubeCount(track.youtubeLikeCount),
                                        color = Color.White.copy(alpha = 0.94f),
                                        fontSize = if (compact) 12.5.sp else 13.sp,
                                        fontWeight = FontWeight.ExtraBold
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
                                    modifier = Modifier.size(if (compact) 19.dp else 20.dp)
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
                                        fontSize = if (compact) 12.5.sp else 13.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "youtube-comments") {
                    Surface(
                        color = Color.White.copy(alpha = 0.085f),
                        border = BorderStroke(
                            1.dp,
                            if (comments.visible) primary.copy(alpha = 0.52f) else Color.White.copy(alpha = 0.105f)
                        ),
                        shape = CircleShape,
                        modifier = Modifier.pressable(enabled = canOpenComments, onClick = onComments)
                    ) {
                        Row(
                            modifier = Modifier
                                .height(if (compact) 40.dp else 42.dp)
                                .padding(horizontal = if (compact) 13.dp else 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChatBubbleOutline,
                                contentDescription = null,
                                tint = if (canOpenComments) Color.White.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.42f),
                                modifier = Modifier.size(if (compact) 19.dp else 20.dp)
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
                                    fontSize = if (compact) 12.5.sp else 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

        }
    }
}

private fun youtubeCommentCountBadge(value: String): String {
    val normalized = value.replace('\u00A0', ' ').trim()
    if (normalized.isBlank()) return ""
    return Regex("""\d[\d\s.,]*(?:[KMBkmb])?""")
        .find(normalized)
        ?.value
        ?.replace(" ", "")
        .orEmpty()
}

@Composable
private fun PlayerAdvancedControlsPanel(
    expanded: Boolean,
    track: Track,
    state: LevyraUiState,
    primary: Color,
    secondary: Color,
    primaryContent: Color,
    secondaryContent: Color,
    compact: Boolean,
    strings: LevyraStrings,
    viewModel: PlayerViewModel
) {
    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(animationSpec = tween(90)) + expandVertically(
            animationSpec = tween(170, easing = FastOutSlowInEasing),
            expandFrom = Alignment.Top
        ),
        exit = fadeOut(animationSpec = tween(70)) + shrinkVertically(
            animationSpec = tween(130, easing = FastOutSlowInEasing),
            shrinkTowards = Alignment.Top
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
        ) {
            PlayerUtilityDock(
                activeColor = primaryContent,
                secondaryColor = secondaryContent,
                lyricsAvailable = state.lyrics.isNotEmpty(),
                isExporting = state.isOfflineExporting,
                isDownloaded = track.id in state.downloadedTrackIds,
                compact = compact,
                onQueue = viewModel::openQueue,
                onLyrics = viewModel::openLyrics,
                onDownload = viewModel::exportCurrentTrack
            )
            PlayerOptionsRow(
                speed = state.playbackSpeed,
                sleepMinutes = state.sleepTimerMinutes,
                audioNormalization = state.audioNormalization,
                activeColor = primary,
                secondaryColor = secondary,
                compact = compact,
                onSpeed = viewModel::cycleSpeed,
                onSleep = viewModel::cycleSleepTimer,
                onNormalization = viewModel::toggleAudioNormalization
            )
            PlayerInlineLyricsSection(
                trackId = track.id,
                lyrics = state.lyrics,
                lyricsLoading = state.lyricsLoading,
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                primaryContent = primaryContent,
                compact = compact,
                strings = strings,
                onSeek = viewModel::seekTo
            )
        }
    }
}

@Composable
private fun PlayerInlineLyricsSection(
    trackId: String,
    lyrics: List<com.luc4n3x.levyra.domain.LyricLine>,
    lyricsLoading: Boolean,
    positionMs: Long,
    durationMs: Long,
    primaryContent: Color,
    compact: Boolean,
    strings: LevyraStrings,
    onSeek: (Float) -> Unit
) {
    var showInlineLyrics by remember(trackId) { mutableStateOf(false) }

    when {
        lyrics.isNotEmpty() && !showInlineLyrics -> {
            Surface(
                color = Color.Black.copy(alpha = 0.20f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .pressable { showInlineLyrics = true }
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 18.dp,
                        vertical = if (compact) 12.dp else 13.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Subject,
                            null,
                            tint = primaryContent,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(9.dp))
                        Text(
                            strings.showLyrics,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        null,
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        lyrics.isNotEmpty() -> {
            val listState = rememberLazyListState()
            val activeIndex = lyrics.indexOfFirst { positionMs in it.startMs..it.endMs }

            LaunchedEffect(activeIndex) {
                if (activeIndex >= 0) {
                    listState.animateScrollToItem(maxOf(0, activeIndex - 1))
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .padding(vertical = 8.dp)
                    .background(Color.Black.copy(alpha = 0.26f), RoundedCornerShape(28.dp))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.11f)), RoundedCornerShape(28.dp))
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 52.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(lyrics) { index, line ->
                        val isActive = index == activeIndex
                        Text(
                            text = line.text,
                            color = if (isActive) primaryContent else Color.White.copy(alpha = 0.48f),
                            fontSize = if (isActive) 24.sp else 19.sp,
                            lineHeight = if (isActive) 29.sp else 24.sp,
                            fontWeight = if (isActive) FontWeight.Black else FontWeight.Medium,
                            modifier = Modifier.clickable {
                                onSeek(progressOf(line.startMs, durationMs))
                            }
                        )
                    }
                }
                IconButton(
                    onClick = { showInlineLyrics = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        strings.close,
                        tint = Color.White.copy(alpha = 0.62f)
                    )
                }
            }
        }

        lyricsLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
                    .background(Color.Black.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = primaryContent,
                    strokeWidth = 3.dp
                )
            }
        }
    }
}

@Composable
private fun PlayerScreen(viewModel: PlayerViewModel, state: LevyraUiState) {
    val strings = LocalLevyraStrings.current
    val track = state.currentTrack
    val playerContext = LocalContext.current
    val playerActivity = playerContext as? Activity
    val audioManager = remember(playerContext) { playerContext.getSystemService(AudioManager::class.java) }
    val hapticFeedback = LocalHapticFeedback.current
    val seekStepMs = state.interfaceSettings.doubleTapSeekSeconds.toLong() * 1_000L
    val primaryTarget = track?.let { Color(it.accentStart) } ?: LevyraCyan
    val secondaryTarget = track?.let { Color(it.accentEnd) } ?: LevyraViolet
    val primary by animateColorAsState(
        targetValue = primaryTarget,
        animationSpec = tween(700, easing = LinearOutSlowInEasing),
        label = "player-primary-color"
    )
    val secondary by animateColorAsState(
        targetValue = secondaryTarget,
        animationSpec = tween(700, easing = LinearOutSlowInEasing),
        label = "player-secondary-color"
    )
    val primaryContent = remember(primary) {
        primary.playerContentColor(listOf(PlayerDarkSurface))
    }
    val secondaryContent = remember(secondary) {
        secondary.playerContentColor(listOf(PlayerDarkSurface))
    }
    val artworkUrl = track?.largeThumbnailUrl?.ifBlank { track.thumbnailUrl }.orEmpty()
    var mediaSeekFeedbackMs by remember(track?.id) { mutableStateOf(0L) }
    var mediaSeekFeedbackEvent by remember(track?.id) { mutableStateOf(0) }
    var gestureFeedback by remember(track?.id) { mutableStateOf("") }
    var gestureFeedbackEvent by remember(track?.id) { mutableStateOf(0) }

    BackHandler(enabled = state.youtubeEngagement.comments.visible) {
        viewModel.closeYoutubeComments()
    }

    LaunchedEffect(mediaSeekFeedbackEvent) {
        if (mediaSeekFeedbackEvent > 0) {
            delay(650L)
            mediaSeekFeedbackMs = 0L
        }
    }
    LaunchedEffect(gestureFeedbackEvent) {
        if (gestureFeedbackEvent > 0) {
            delay(700L)
            gestureFeedback = ""
        }
    }

    val artScale by animateFloatAsState(
        targetValue = if (state.isPlaying) 1f else 0.975f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "artwork-scale"
    )
    val artCorner by animateDpAsState(
        targetValue = if (state.isPlaying) 30.dp else 22.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "artwork-corner"
    )
    val artShadow by animateFloatAsState(
        targetValue = if (state.isPlaying) 30f else 16f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "artwork-shadow"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val compactPlayer = maxWidth < 380.dp || maxHeight < 700.dp
        var advancedControlsExpanded by remember(track?.id) {
            mutableStateOf(false)
        }
        val playerHorizontalPadding = if (compactPlayer) 18.dp else 20.dp
        val playerItemSpacing = if (compactPlayer) 9.dp else 12.dp
        val artworkSize = minOf(
            (maxWidth - playerHorizontalPadding * 2f).coerceAtLeast(180.dp),
            520.dp
        )

        PlayerImmersiveBackdrop(
            primaryTarget = primaryTarget,
            secondaryTarget = secondaryTarget,
            isPlaying = state.isPlaying,
            modifier = Modifier.fillMaxSize()
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(
                start = playerHorizontalPadding,
                end = playerHorizontalPadding,
                top = if (compactPlayer) 8.dp else 10.dp,
                bottom = if (compactPlayer) 28.dp else 34.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(playerItemSpacing)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (compactPlayer) 46.dp else 48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.width(86.dp), contentAlignment = Alignment.CenterStart) {
                        PlayerRoundIconButton(
                            icon = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = strings.back,
                            size = if (compactPlayer) 39.dp else 40.dp,
                            iconSize = if (compactPlayer) 26.dp else 27.dp,
                            tint = Color.White,
                            background = Color.Black.copy(alpha = 0.22f),
                            borderColor = Color.White.copy(alpha = 0.12f),
                            onClick = { viewModel.selectTab(LevyraTab.Home) }
                        )
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (track != null && track.videoUrl.isNotBlank()) {
                            PlayerModeSwitch(
                                isVideoMode = state.isVideoMode,
                                activeColor = primary,
                                onSong = viewModel::toggleVideoMode,
                                onVideo = viewModel::toggleVideoMode
                            )
                        } else {
                            Surface(
                                color = Color.Black.copy(alpha = 0.20f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = strings.formatPlayingFrom(track?.source ?: "LEVYRA"),
                                    color = Color.White.copy(alpha = 0.72f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.1.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.width(86.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.isVideoMode) {
                            PlayerRoundIconButton(
                                icon = Icons.Rounded.PictureInPictureAlt,
                                contentDescription = strings.pictureInPicture,
                                size = if (compactPlayer) 39.dp else 40.dp,
                                iconSize = if (compactPlayer) 20.dp else 20.dp,
                                tint = Color.White,
                                background = Color.Black.copy(alpha = 0.22f),
                                borderColor = primary.copy(alpha = 0.38f),
                                onClick = { LevyraPipBridge.enter() }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        PlayerRoundIconButton(
                            icon = Icons.Rounded.MoreVert,
                            contentDescription = strings.options,
                            size = if (compactPlayer) 39.dp else 40.dp,
                            iconSize = if (compactPlayer) 22.dp else 23.dp,
                            tint = Color.White,
                            background = Color.Black.copy(alpha = 0.22f),
                            borderColor = Color.White.copy(alpha = 0.12f),
                            onClick = { viewModel.openAudioQualityPanel() }
                        )
                    }
                }
            }
            if (track == null) {
                item { EmptyState(strings.emptyPlayer) }
            } else {
                item {
                    val mediaHeight = if (state.isVideoMode && track.videoUrl.isNotBlank()) {
                        artworkSize * 0.5625f
                    } else {
                        artworkSize
                    }
                    Box(
                        modifier = Modifier
                            .size(width = artworkSize, height = mediaHeight)
                            .padding(vertical = if (compactPlayer) 1.dp else 2.dp)
                    ) {
                        if (state.isVideoMode && track.videoUrl.isNotBlank()) {
                            LevyraVideoSurface(
                                state = state,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                                    .graphicsLayer {
                                        scaleX = artScale
                                        scaleY = artScale
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
                            MotionArtworkLayer(
                                artwork = state.motionArtwork,
                                enabled = state.animationsEnabled && !state.isVideoMode,
                                isPlaying = state.isPlaying,
                                cornerRadius = artCorner,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = artScale
                                        scaleY = artScale
                                    }
                            ) {
                                PlayerArtworkCanvas(
                                    track = track,
                                    artworkUrl = artworkUrl,
                                    isPlaying = state.isPlaying,
                                    cornerRadius = artCorner,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        if (state.interfaceSettings.playerGesturesEnabled) {
                            Row(
                                modifier = Modifier
                                    .matchParentSize()
                                    .zIndex(20f)
                            ) {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .pointerInput(track.id, state.interfaceSettings.doubleTapSeekSeconds, state.interfaceSettings.longPressSpeed) {
                                                detectTapGestures(
                                                    onPress = {
                                                        val originalSpeed = state.playbackSpeed
                                                        coroutineScope {
                                                            var boosted = false
                                                            val speedJob = launch {
                                                                delay(320L)
                                                                boosted = true
                                                                viewModel.setTemporaryPlaybackSpeed(state.interfaceSettings.longPressSpeed)
                                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                gestureFeedback = "${String.format(Locale.US, "%.1f", state.interfaceSettings.longPressSpeed)}×"
                                                                gestureFeedbackEvent += 1
                                                            }
                                                            try {
                                                                tryAwaitRelease()
                                                            } finally {
                                                                speedJob.cancel()
                                                                if (boosted) viewModel.setTemporaryPlaybackSpeed(originalSpeed)
                                                            }
                                                        }
                                                    },
                                                    onDoubleTap = {
                                                        viewModel.seekBy(-seekStepMs)
                                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        mediaSeekFeedbackMs = -seekStepMs
                                                        mediaSeekFeedbackEvent += 1
                                                    }
                                                )
                                            }
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .width(54.dp)
                                            .fillMaxHeight()
                                            .pointerInput(track.id, playerActivity) {
                                                detectVerticalDragGestures { change, dragAmount ->
                                                    change.consume()
                                                    val activity = playerActivity ?: return@detectVerticalDragGestures
                                                    val attributes = activity.window.attributes
                                                    val current = attributes.screenBrightness.takeIf { it >= 0f } ?: 0.5f
                                                    val updated = (current - dragAmount / size.height.coerceAtLeast(1)).coerceIn(0.05f, 1f)
                                                    attributes.screenBrightness = updated
                                                    activity.window.attributes = attributes
                                                    gestureFeedback = "${strings.brightness} ${(updated * 100f).roundToInt()}%"
                                                    gestureFeedbackEvent += 1
                                                }
                                            }
                                    )
                                }
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .pointerInput(track.id, state.interfaceSettings.doubleTapSeekSeconds, state.interfaceSettings.longPressSpeed) {
                                                detectTapGestures(
                                                    onPress = {
                                                        val originalSpeed = state.playbackSpeed
                                                        coroutineScope {
                                                            var boosted = false
                                                            val speedJob = launch {
                                                                delay(320L)
                                                                boosted = true
                                                                viewModel.setTemporaryPlaybackSpeed(state.interfaceSettings.longPressSpeed)
                                                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                gestureFeedback = "${String.format(Locale.US, "%.1f", state.interfaceSettings.longPressSpeed)}×"
                                                                gestureFeedbackEvent += 1
                                                            }
                                                            try {
                                                                tryAwaitRelease()
                                                            } finally {
                                                                speedJob.cancel()
                                                                if (boosted) viewModel.setTemporaryPlaybackSpeed(originalSpeed)
                                                            }
                                                        }
                                                    },
                                                    onDoubleTap = {
                                                        viewModel.seekBy(seekStepMs)
                                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        mediaSeekFeedbackMs = seekStepMs
                                                        mediaSeekFeedbackEvent += 1
                                                    }
                                                )
                                            }
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .width(54.dp)
                                            .fillMaxHeight()
                                            .pointerInput(track.id, audioManager) {
                                                var accumulated = 0f
                                                detectVerticalDragGestures(
                                                    onDragStart = { accumulated = 0f },
                                                    onVerticalDrag = { change, dragAmount ->
                                                        change.consume()
                                                        val manager = audioManager ?: return@detectVerticalDragGestures
                                                        val maximum = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                                        accumulated += -dragAmount / size.height.coerceAtLeast(1) * maximum
                                                        val steps = accumulated.roundToInt()
                                                        if (steps != 0) {
                                                            val current = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                                            val updated = (current + steps).coerceIn(0, maximum)
                                                            manager.setStreamVolume(AudioManager.STREAM_MUSIC, updated, 0)
                                                            accumulated -= steps.toFloat()
                                                            gestureFeedback = "${strings.volume} ${((updated.toFloat() / maximum.toFloat()) * 100f).roundToInt()}%"
                                                            gestureFeedbackEvent += 1
                                                        }
                                                    }
                                                )
                                            }
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = gestureFeedback.isNotBlank(),
                            modifier = Modifier.align(Alignment.Center).zIndex(22f),
                            enter = fadeIn(animationSpec = tween(110)),
                            exit = fadeOut(animationSpec = tween(180))
                        ) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.74f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                                shape = CircleShape
                            ) {
                                Text(
                                    text = gestureFeedback,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(if (mediaSeekFeedbackMs < 0L) Alignment.CenterStart else Alignment.CenterEnd)
                                .padding(horizontal = 30.dp)
                        ) {
                            AnimatedVisibility(
                                visible = mediaSeekFeedbackMs != 0L,
                                enter = fadeIn(animationSpec = tween(110)),
                                exit = fadeOut(animationSpec = tween(180))
                            ) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.72f),
                                    border = BorderStroke(1.dp, primary.copy(alpha = 0.34f)),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "${if (mediaSeekFeedbackMs < 0L) "−" else "+"}${kotlin.math.abs(mediaSeekFeedbackMs) / 1_000L} s",
                                        color = Color.White,
                                        fontSize = if (compactPlayer) 14.sp else 15.sp,
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
                            .padding(
                                horizontal = 4.dp,
                                vertical = if (compactPlayer) 1.dp else 2.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                color = Color.White,
                                fontSize = if (compactPlayer) 24.sp else 25.sp,
                                lineHeight = if (compactPlayer) 28.sp else 29.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(if (compactPlayer) 3.dp else 4.dp))
                            Text(
                                text = track.artist,
                                color = Color.White.copy(alpha = 0.68f),
                                fontSize = if (compactPlayer) 14.sp else 15.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable { viewModel.openArtist(track) }
                            )
                            PlayerYoutubeEngagementRow(
                                track = track,
                                engagement = state.youtubeEngagement,
                                primary = primary,
                                secondary = secondary,
                                compact = compactPlayer,
                                onComments = viewModel::openYoutubeComments
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        val isFavorite = track.id in state.favoriteIds
                        PlayerRoundIconButton(
                            icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = strings.favoritesPlain,
                            size = if (compactPlayer) 44.dp else 46.dp,
                            iconSize = if (compactPlayer) 23.dp else 24.dp,
                            tint = if (isFavorite) {
                                Color.White.playerContentColor(
                                    listOf(primary.copy(alpha = 0.46f).playerCompositeOver(PlayerDarkSurface))
                                )
                            } else {
                                Color.White.copy(alpha = 0.78f)
                            },
                            background = if (isFavorite) primary.copy(alpha = 0.46f) else Color.Black.copy(alpha = 0.20f),
                            borderColor = if (isFavorite) primary.playerMix(Color.White, 0.25f).copy(alpha = 0.62f) else Color.White.copy(alpha = 0.12f),
                            onClick = { viewModel.toggleFavorite(track) }
                        )
                    }
                }
                item {
                    PlayerTimeline(
                        positionMs = state.positionMs,
                        durationMs = state.durationMs,
                        activeColor = primary,
                        secondaryColor = secondary,
                        compact = compactPlayer,
                        onSeek = viewModel::seekTo
                    )
                }
                item {
                    MainPlayerControls(
                        isPlaying = state.isPlaying,
                        isResolving = state.isResolving,
                        shuffleOn = state.shuffleEnabled,
                        repeatMode = state.repeatMode,
                        activeColor = primary,
                        secondaryColor = secondary,
                        activeContentColor = primaryContent,
                        secondaryContentColor = secondaryContent,
                        compact = compactPlayer,
                        onShuffle = viewModel::toggleShuffle,
                        onPrevious = viewModel::previous,
                        onToggle = viewModel::togglePlay,
                        onNext = viewModel::next,
                        onRepeat = viewModel::toggleRepeat
                    )
                }
                item {
                    LevyraControlPulseHandle(
                        expanded = advancedControlsExpanded,
                        compact = compactPlayer,
                        activeColor = primary,
                        secondaryColor = secondary,
                        hasActiveState = state.playbackSpeed != 1f || state.sleepTimerMinutes > 0 || state.isOfflineExporting,
                        onToggle = { advancedControlsExpanded = !advancedControlsExpanded }
                    )
                }
                item(key = "player-advanced-controls") {
                    PlayerAdvancedControlsPanel(
                        expanded = advancedControlsExpanded,
                        track = track,
                        state = state,
                        primary = primary,
                        secondary = secondary,
                        primaryContent = primaryContent,
                        secondaryContent = secondaryContent,
                        compact = compactPlayer,
                        strings = strings,
                        viewModel = viewModel
                    )
                }
                item { PlayerError(state.playerError) }
            }
        }

        if (state.youtubeEngagement.comments.visible) {
            PlayerYoutubeCommentsSheet(
                comments = state.youtubeEngagement.comments,
                primary = primary,
                secondary = secondary,
                strings = strings,
                onDismiss = viewModel::closeYoutubeComments,
                onRetry = viewModel::retryYoutubeComments,
                onLoadMore = viewModel::loadMoreYoutubeComments,
                onRetryLoadMore = viewModel::retryYoutubeCommentsPage,
                onToggleReplies = viewModel::toggleYoutubeCommentReplies,
                onLoadMoreReplies = viewModel::loadMoreYoutubeCommentReplies
            )
        }
    }
}

@Composable
private fun PlayerYoutubeCommentsSheet(
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
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
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
                            text = strings.totalComments,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        if (comments.countText.isNotBlank()) {
                            Text(
                                text = comments.countText,
                                color = Color.White.copy(alpha = 0.52f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    PlayerRoundIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = strings.close,
                        size = 38.dp,
                        iconSize = 20.dp,
                        tint = Color.White.copy(alpha = 0.82f),
                        background = Color.White.copy(alpha = 0.07f),
                        borderColor = Color.White.copy(alpha = 0.10f),
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
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = comments.items,
                                key = YoutubeComment::id,
                                contentType = { "youtube-comment" }
                            ) { comment ->
                                YoutubeCommentCard(
                                    comment = comment,
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
    primary: Color,
    secondary: Color,
    moreLabel: String,
    onToggleReplies: () -> Unit,
    onLoadMoreReplies: () -> Unit
) {
    Surface(
        color = Color.White.copy(alpha = 0.055f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                YoutubeCommentAvatar(
                    url = comment.authorAvatarUrl,
                    author = comment.author,
                    modifier = Modifier.size(36.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = comment.author,
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
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
                            color = Color.White.copy(alpha = 0.40f),
                            fontSize = 10.sp,
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

            Text(
                text = comment.text,
                color = Color.White.copy(alpha = 0.86f),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (comment.likeCountText.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ThumbUp,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.46f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = comment.likeCountText,
                            color = Color.White.copy(alpha = 0.48f),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (comment.replyCount > 0 && comment.replyToken.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = onToggleReplies)
                            .padding(horizontal = 7.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Reply,
                            contentDescription = null,
                            tint = secondary.playerMix(Color.White, 0.58f),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = comment.replyCount.toString(),
                            color = secondary.playerMix(Color.White, 0.58f),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = comment.repliesExpanded,
                enter = fadeIn(tween(160)) + slideInVertically(initialOffsetY = { -it / 5 }),
                exit = fadeOut(tween(120))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 30.dp, top = 2.dp)
                        .border(
                            width = 1.dp,
                            color = primary.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 10.dp),
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
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
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
                    text = author.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty(),
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
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
    val ribbonAmplitude by animateDpAsState(
        targetValue = if (isDragging) 3.5.dp else 2.4.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium),
        label = "timeline-ribbon-amplitude"
    )
    val ribbonStroke by animateDpAsState(
        targetValue = if (isDragging) 3.7.dp else 2.9.dp,
        animationSpec = spring(dampingRatio = 0.84f, stiffness = Spring.StiffnessMedium),
        label = "timeline-ribbon-stroke"
    )
    val markerHalfSize by animateDpAsState(
        targetValue = if (isDragging) 5.6.dp else 4.2.dp,
        animationSpec = spring(dampingRatio = 0.76f, stiffness = Spring.StiffnessMedium),
        label = "timeline-marker-size"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = if (compact) 1.dp else 2.dp,
                bottom = if (compact) 3.dp else 4.dp
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 30.dp else 32.dp)
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
                            val inset = 8.dp.toPx()
                            val usable = (size.width - inset * 2f).coerceAtLeast(1f)
                            onSeek(((offset.x - inset) / usable).coerceIn(0f, 1f))
                        }
                    }
                }
                .pointerInput(durationMs) {
                    if (durationMs > 0L) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                val inset = 8.dp.toPx()
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
                                val inset = 8.dp.toPx()
                                val usable = (size.width - inset * 2f).coerceAtLeast(1f)
                                dragFraction = ((change.position.x - inset) / usable).coerceIn(0f, 1f)
                            }
                        )
                    }
                }
                .drawBehind {
                    val centerY = size.height / 2f
                    val inset = 8.dp.toPx()
                    val usable = (size.width - inset * 2f).coerceAtLeast(1f)
                    val playedX = inset + usable * fraction
                    val endX = inset + usable
                    val amplitude = ribbonAmplitude.toPx()
                    val stroke = ribbonStroke.toPx()
                    val marker = markerHalfSize.toPx()
                    val markerGap = marker + 3.5.dp.toPx()
                    val playedEnd = (playedX - markerGap).coerceAtLeast(inset)
                    val futureStart = (playedX + markerGap).coerceAtMost(endX)
                    val amplitudePattern = floatArrayOf(0.74f, 1f, 0.82f, 0.64f, 0.9f, 0.7f)

                    fun waveformPath(
                        startX: Float,
                        finishX: Float,
                        baselineY: Float,
                        amplitudeScale: Float,
                        phaseOffset: Int
                    ): Path {
                        val width = (finishX - startX).coerceAtLeast(0f)
                        val path = Path().apply { moveTo(startX, baselineY) }
                        if (width <= 0.5f) return path
                        val preferredSegmentWidth = 43.dp.toPx()
                        val segmentCount = kotlin.math.ceil(width / preferredSegmentWidth)
                            .toInt()
                            .coerceIn(1, 7)
                        val segmentWidth = width / segmentCount

                        repeat(segmentCount) { index ->
                            val segmentStartX = startX + segmentWidth * index
                            val segmentEndX = if (index == segmentCount - 1) finishX else segmentStartX + segmentWidth
                            val controlOneX = segmentStartX + (segmentEndX - segmentStartX) * 0.30f
                            val controlTwoX = segmentStartX + (segmentEndX - segmentStartX) * 0.70f
                            val patternIndex = (index + phaseOffset) % amplitudePattern.size
                            val direction = if ((index + phaseOffset) % 2 == 0) -1f else 1f
                            val segmentAmplitude = amplitude * amplitudePattern[patternIndex] * amplitudeScale

                            path.cubicTo(
                                controlOneX,
                                baselineY + segmentAmplitude * direction,
                                controlTwoX,
                                baselineY - segmentAmplitude * direction,
                                segmentEndX,
                                baselineY
                            )
                        }
                        return path
                    }

                    if (playedEnd > inset + 0.5f) {
                        val playedPath = waveformPath(
                            startX = inset,
                            finishX = playedEnd,
                            baselineY = centerY - 0.5.dp.toPx(),
                            amplitudeScale = 1f,
                            phaseOffset = 0
                        )
                        val playedEchoPath = waveformPath(
                            startX = inset,
                            finishX = playedEnd,
                            baselineY = centerY + 3.3.dp.toPx(),
                            amplitudeScale = 0.48f,
                            phaseOffset = 1
                        )

                        drawPath(
                            path = playedEchoPath,
                            color = secondaryColor.playerMix(activeColor, 0.42f).copy(alpha = 0.20f),
                            style = Stroke(width = 1.35.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawPath(
                            path = playedPath,
                            color = activeColor.playerMix(secondaryColor, 0.28f).copy(alpha = 0.22f),
                            style = Stroke(width = stroke + 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawPath(
                            path = playedPath,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.88f),
                                    Color.White,
                                    secondaryColor.playerMix(Color.White, 0.58f)
                                ),
                                startX = inset,
                                endX = playedEnd.coerceAtLeast(inset + 1f)
                            ),
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }

                    if (futureStart < endX - 0.5f) {
                        val futurePath = waveformPath(
                            startX = futureStart,
                            finishX = endX,
                            baselineY = centerY - 0.3.dp.toPx(),
                            amplitudeScale = 0.58f,
                            phaseOffset = 2
                        )
                        val futureEchoPath = waveformPath(
                            startX = futureStart,
                            finishX = endX,
                            baselineY = centerY + 3.dp.toPx(),
                            amplitudeScale = 0.34f,
                            phaseOffset = 3
                        )

                        drawPath(
                            path = futureEchoPath,
                            color = Color.White.copy(alpha = 0.075f),
                            style = Stroke(width = 1.1.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawPath(
                            path = futurePath,
                            color = Color.White.copy(alpha = 0.18f),
                            style = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    if (isDragging) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.32f),
                            start = androidx.compose.ui.geometry.Offset(playedX, centerY - 11.dp.toPx()),
                            end = androidx.compose.ui.geometry.Offset(playedX, centerY + 11.dp.toPx()),
                            strokeWidth = 1.dp.toPx(),
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

                    drawPath(
                        path = diamondPath(marker + 3.5.dp.toPx()),
                        color = activeColor.playerMix(secondaryColor, 0.46f).copy(alpha = if (isDragging) 0.24f else 0.16f)
                    )
                    drawPath(
                        path = diamondPath(marker + 1.25.dp.toPx()),
                        color = secondaryColor.playerMix(activeColor, 0.48f).copy(alpha = 0.76f)
                    )
                    drawPath(
                        path = diamondPath(marker),
                        color = Color.White
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
                color = if (isDragging) Color.White else Color.White.copy(alpha = 0.64f),
                fontSize = if (compact) 10.5.sp else 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatDuration(durationMs),
                color = Color.White.copy(alpha = 0.52f),
                fontSize = if (compact) 10.5.sp else 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun MainPlayerControls(
    isPlaying: Boolean,
    isResolving: Boolean,
    shuffleOn: Boolean,
    repeatMode: com.luc4n3x.levyra.domain.RepeatMode,
    activeColor: Color,
    secondaryColor: Color,
    activeContentColor: Color,
    secondaryContentColor: Color,
    compact: Boolean,
    onShuffle: () -> Unit,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 2.dp,
                vertical = if (compact) 3.dp else 4.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        PlayerRoundIconButton(
            icon = Icons.Rounded.Shuffle,
            contentDescription = LocalLevyraStrings.current.shuffle,
            size = if (compact) 38.dp else 40.dp,
            iconSize = if (compact) 21.dp else 22.dp,
            tint = if (shuffleOn) activeContentColor else Color.White.copy(alpha = 0.58f),
            background = Color.Transparent,
            borderColor = Color.Transparent,
            onClick = onShuffle
        )
        PlayerTransportButton(
            icon = Icons.Rounded.SkipPrevious,
            contentDescription = LocalLevyraStrings.current.previous,
            compact = compact,
            onClick = onPrevious
        )
        val playCorner by animateDpAsState(
            targetValue = if (isPlaying) 24.dp else 34.dp,
            animationSpec = spring(dampingRatio = 0.67f, stiffness = Spring.StiffnessMediumLow),
            label = "play-corner"
        )
        val playShape = RoundedCornerShape(playCorner)
        val playGradient = remember(activeColor, secondaryColor) {
            playerContrastGradient(
                start = activeColor.playerMix(Color.White, 0.16f),
                end = secondaryColor.playerMix(Color.White, 0.08f),
                minimumContrast = PlayerMinimumContrast
            )
        }
        Box(
            modifier = Modifier
                .size(
                    width = if (compact) 78.dp else 82.dp,
                    height = if (compact) 64.dp else 66.dp
                )
                .shadow(
                    elevation = if (compact) 16.dp else 18.dp,
                    shape = playShape,
                    clip = false,
                    ambientColor = activeColor.copy(alpha = 0.46f),
                    spotColor = secondaryColor.copy(alpha = 0.52f)
                )
                .background(
                    Brush.linearGradient(
                        listOf(playGradient.start, playGradient.end)
                    ),
                    playShape
                )
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)), playShape)
                .pressable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            if (isResolving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(if (compact) 28.dp else 29.dp),
                    strokeWidth = 3.2.dp,
                    color = playGradient.content
                )
            } else {
                AnimatedContent(
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
            }
        }
        PlayerTransportButton(
            icon = Icons.Rounded.SkipNext,
            contentDescription = LocalLevyraStrings.current.next,
            compact = compact,
            onClick = onNext
        )
        val repeatIcon = if (repeatMode == com.luc4n3x.levyra.domain.RepeatMode.One) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat
        PlayerRoundIconButton(
            icon = repeatIcon,
            contentDescription = LocalLevyraStrings.current.repeat,
            size = if (compact) 38.dp else 40.dp,
            iconSize = if (compact) 21.dp else 22.dp,
            tint = if (repeatMode != com.luc4n3x.levyra.domain.RepeatMode.Off) secondaryContentColor else Color.White.copy(alpha = 0.58f),
            background = Color.Transparent,
            borderColor = Color.Transparent,
            onClick = onRepeat
        )
    }
}

@Composable
private fun PlayerTransportButton(
    icon: ImageVector,
    contentDescription: String,
    compact: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(
                width = if (compact) 54.dp else 56.dp,
                height = if (compact) 52.dp else 54.dp
            )
            .background(Color.White.copy(alpha = 0.11f), RoundedCornerShape(20.dp))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), RoundedCornerShape(20.dp))
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(if (compact) 30.dp else 31.dp)
        )
    }
}

@Composable
private fun PlayerOptionsRow(
    speed: Float,
    sleepMinutes: Int,
    audioNormalization: Boolean,
    activeColor: Color,
    secondaryColor: Color,
    compact: Boolean,
    onSpeed: () -> Unit,
    onSleep: () -> Unit,
    onNormalization: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = if (compact) 1.dp else 2.dp,
                bottom = if (compact) 3.dp else 4.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OptionChip(
            icon = Icons.Rounded.GraphicEq,
            label = LocalLevyraStrings.current.normalizationShort,
            active = audioNormalization,
            activeColor = activeColor,
            modifier = Modifier.weight(1f),
            compact = compact,
            onClick = onNormalization
        )
        OptionChip(
            icon = Icons.Rounded.Speed,
            label = "${trimSpeed(speed)}x",
            active = speed != 1f,
            activeColor = activeColor.playerMix(secondaryColor, 0.35f),
            modifier = Modifier.weight(1f),
            compact = compact,
            onClick = onSpeed
        )
        OptionChip(
            icon = Icons.Rounded.Bedtime,
            label = if (sleepMinutes > 0) "${sleepMinutes}m" else LocalLevyraStrings.current.timer,
            active = sleepMinutes > 0,
            activeColor = secondaryColor,
            modifier = Modifier.weight(1f),
            compact = compact,
            onClick = onSleep
        )
    }
}

@Composable
private fun OptionChip(
    icon: ImageVector,
    label: String,
    active: Boolean,
    activeColor: Color,
    modifier: Modifier,
    compact: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.5f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "player-option-alpha"
    )
    val background = if (active) activeColor.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.20f)
    val effectiveBackground = background.playerCompositeOver(PlayerDarkSurface)
    val contentColor = if (active) {
        activeColor.playerContentColor(listOf(effectiveBackground))
    } else {
        Color.White.playerContentColor(listOf(effectiveBackground))
    }
    val borderColor = if (active) activeColor.copy(alpha = 0.58f) else Color.White.copy(alpha = 0.10f)

    Surface(
        color = background,
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .height(if (compact) 40.dp else 42.dp)
            .graphicsLayer { this.alpha = alpha }
            .pressable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                null,
                tint = contentColor,
                modifier = Modifier.size(if (compact) 16.dp else 16.dp)
            )
            Spacer(modifier = Modifier.width(if (compact) 6.dp else 6.dp))
            Text(
                text = label,
                color = contentColor,
                fontSize = if (compact) 11.5.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ShareOptionChip(track: Track, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val strings = LocalLevyraStrings.current
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
                context.startActivity(Intent.createChooser(intent, strings.shareSong))
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
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LevyraLanguageCatalog.languages.forEach { language ->
            val selected = language.code == LevyraLanguageCatalog.normalize(selectedCode)
            val scale by animateFloatAsState(
                targetValue = if (selected) 1.02f else 1f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
                label = "scale"
            )
            val rowColor by animateColorAsState(
                targetValue = if (selected) LevyraCyan else LevyraAdaptiveCardDeep,
                animationSpec = tween(200),
                label = "language-row"
            )
            
            Surface(
                color = rowColor,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (selected) LevyraCyan else LevyraAdaptiveHairline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(scale)
                    .pressable(onClick = { onSelect(language.code) })
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        color = if (selected) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                        shape = CircleShape,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = language.flag,
                                fontSize = 18.sp
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = language.nativeName,
                            color = if (selected) LevyraBlack else LevyraText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!language.englishName.equals(language.nativeName, ignoreCase = true)) {
                            Text(
                                text = language.englishName,
                                color = if (selected) LevyraBlack.copy(alpha = 0.6f) else LevyraMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    AnimatedVisibility(
                        visible = selected,
                        enter = scaleIn(spring(dampingRatio = 0.5f, stiffness = 400f)) + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        Surface(
                            color = LevyraBlack,
                            shape = CircleShape,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = LevyraCyan, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingOverlay(selectedLanguageCode: String, onDone: (String, Set<String>, String) -> Unit) {
    val currentLocale = LocalLocale.current.platformLocale
    var selected by remember { mutableStateOf(setOf<String>()) }
    var name by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(OnboardingStep.Language) }
    var languageCode by remember(selectedLanguageCode) { mutableStateOf(LevyraLanguageCatalog.normalize(selectedLanguageCode)) }
    val moodEngine = remember { MoodEngine() }
    val tastes = remember(languageCode) { moodEngine.tastesForLanguage(languageCode) }
    val strings = LevyraStrings.forCode(languageCode)
    val blocker = remember { MutableInteractionSource() }
    val primaryEnabled = onboardingPrimaryEnabled(step, selected.size)
    val layoutDirection = if (LevyraLanguageCatalog.isRtl(languageCode)) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF030304))
                .clickable(interactionSource = blocker, indication = null) {}
        ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 82.dp, y = (-70).dp)
                .size(300.dp)
                .blur(70.dp)
                .background(
                    Brush.radialGradient(listOf(LevyraViolet.copy(alpha = 0.22f), Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-120).dp, y = 90.dp)
                .size(360.dp)
                .blur(84.dp)
                .background(
                    Brush.radialGradient(listOf(LevyraCyan.copy(alpha = 0.17f), Color.Transparent)),
                    CircleShape
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            OnboardingTopBar(step = step, backLabel = strings.back, onBack = { step = step.previous() })
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val direction = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                    (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { it * direction } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally(tween(240, easing = FastOutSlowInEasing)) { -it * direction / 3 } + fadeOut(tween(160)))
                },
                modifier = Modifier.weight(1f),
                label = "onboarding-step"
            ) { activeStep ->
                when (activeStep) {
                    OnboardingStep.Language -> OnboardingLanguageStage(
                        strings = strings,
                        languageCode = languageCode,
                        onLanguage = { languageCode = it }
                    )
                    OnboardingStep.Profile -> OnboardingProfileStage(
                        strings = strings,
                        name = name,
                        locale = currentLocale,
                        onName = { name = it }
                    )
                    OnboardingStep.Taste -> OnboardingTasteStage(
                        strings = strings,
                        tastes = tastes,
                        selected = selected,
                        onToggle = { tasteId ->
                            selected = if (tasteId in selected) selected - tasteId else selected + tasteId
                        }
                    )
                }
            }
            OnboardingFooter(
                strings = strings,
                step = step,
                enabled = primaryEnabled,
                selectedTasteCount = selected.size,
                onPrimary = {
                    if (step == OnboardingStep.Taste) onDone(name, selected, languageCode)
                    else step = step.next()
                },
                onSkip = { onDone(name, selected, languageCode) }
            )
        }
    }
}
}

@Composable
private fun OnboardingTopBar(step: OnboardingStep, backLabel: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (step == OnboardingStep.Language) {
            LevyraLogoMark(size = 42.dp)
        } else {
            CircleIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                tint = LevyraText,
                background = Color.White.copy(alpha = 0.07f),
                contentDescription = backLabel,
                onClick = onBack
            )
        }
        LevyraWordmark(fontSize = 22.sp, dotSize = 4.dp)
        Spacer(modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OnboardingStep.entries.forEach { item ->
                val active = item.ordinal <= step.ordinal
                Box(
                    modifier = Modifier
                        .width(if (item == step) 24.dp else 7.dp)
                        .height(7.dp)
                        .background(if (active) LevyraCyan else Color.White.copy(alpha = 0.14f), CircleShape)
                )
            }
        }
    }
}

@Composable
private fun OnboardingLanguageStage(strings: LevyraStrings, languageCode: String, onLanguage: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.width(24.dp).height(2.dp).background(LevyraCyan, CircleShape))
                Text(
                    text = strings.welcomeBadge,
                    color = LevyraCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.8.sp
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(strings.welcomeTitle, color = LevyraText, fontSize = 40.sp, lineHeight = 43.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.25).sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(strings.languageQuestion, color = LevyraMuted, fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.language,
                    color = LevyraMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                LanguageSelector(selectedCode = languageCode, onSelect = onLanguage)
            }
        }
    }
}

@Composable
private fun OnboardingProfileStage(
    strings: LevyraStrings,
    name: String,
    locale: Locale,
    onName: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .shadow(28.dp, CircleShape, ambientColor = LevyraCyan.copy(alpha = 0.24f), spotColor = LevyraViolet.copy(alpha = 0.28f))
                    .background(Brush.linearGradient(listOf(LevyraCyan, LevyraViolet)), CircleShape)
                    .border(4.dp, Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val initial = name.trim().take(1).uppercase(locale)
                if (initial.isBlank()) Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                else Text(initial, color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black)
            }
        }
        item {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(strings.nameQuestion, color = LevyraText, fontSize = 36.sp, lineHeight = 40.sp, fontWeight = FontWeight.Black, letterSpacing = (-1.1).sp)
                BasicTextField(
                    value = name,
                    onValueChange = { newName ->
                        onName(preserveProfileNameInput(newName))
                    },
                    singleLine = true,
                    textStyle = TextStyle(color = LevyraText, fontSize = 22.sp, fontWeight = FontWeight.Bold),
                    cursorBrush = SolidColor(LevyraCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LevyraAdaptiveCardDeep, RoundedCornerShape(22.dp))
                        .border(1.dp, LevyraAdaptiveHairline, RoundedCornerShape(22.dp))
                        .padding(horizontal = 20.dp, vertical = 19.dp),
                    decorationBox = { innerTextField ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = LevyraCyan, modifier = Modifier.size(22.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (name.isBlank()) Text(strings.namePlaceholder, color = LevyraMuted, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                                innerTextField()
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun OnboardingTasteStage(
    strings: LevyraStrings,
    tastes: List<Taste>,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 26.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    strings.tasteQuestion,
                    color = LevyraText,
                    fontSize = 34.sp,
                    lineHeight = 39.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    modifier = Modifier.weight(1f)
                )
                Surface(color = LevyraCyan.copy(alpha = 0.14f), shape = CircleShape, border = BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.32f))) {
                    Text("${selected.size} / 3", color = LevyraCyan, fontSize = 13.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp))
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
        items(tastes.chunked(2), key = { row -> row.joinToString("-") { it.id } }) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { taste ->
                    TasteCard(
                        taste = taste,
                        selected = taste.id in selected,
                        modifier = Modifier.weight(1f),
                        onClick = { onToggle(taste.id) }
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun OnboardingFooter(
    strings: LevyraStrings,
    step: OnboardingStep,
    enabled: Boolean,
    selectedTasteCount: Int,
    onPrimary: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF030304), Color(0xFF030304))))
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            color = if (enabled) LevyraCyan else Color.White.copy(alpha = 0.10f),
            shape = CircleShape,
            modifier = Modifier.fillMaxWidth().height(58.dp).pressable(enabled = enabled, onClick = onPrimary)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (step == OnboardingStep.Taste) strings.startListening else strings.next,
                    color = if (enabled) LevyraBlack else LevyraMuted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = if (enabled) LevyraBlack else LevyraMuted, modifier = Modifier.size(21.dp))
            }
        }
        if (step == OnboardingStep.Taste && selectedTasteCount < 3) {
            TextButton(onClick = onSkip) {
                Text(strings.skipAndContinue, color = LevyraMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun TasteCard(taste: Taste, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        color = if (selected) LevyraCyan.copy(alpha = 0.17f) else LevyraAdaptiveCard,
        border = BorderStroke(1.dp, if (selected) LevyraCyan.copy(alpha = 0.72f) else LevyraAdaptiveHairline),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .height(62.dp)
            .pressable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(taste.emoji, fontSize = 21.sp)
            Text(
                taste.label,
                color = LevyraText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = LevyraCyan, modifier = Modifier.size(18.dp))
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
    interfaceSettings: LevyraInterfaceSettings,
    downloadSettings: LevyraDownloadSettings,
    downloadQueue: List<OfflineDownloadTask>,
    playbackDiagnostics: String,
    onThemePreset: (String) -> Unit,
    onInterfaceSettings: (LevyraInterfaceSettings) -> Unit,
    onDownloadSettings: (LevyraDownloadSettings) -> Unit,
    onAnimations: (Boolean) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onSponsorBlock: (Boolean) -> Unit,
    onSkipSilence: (Boolean) -> Unit,
    onLanguage: (String) -> Unit,
    onCheckUpdates: () -> Unit,
    onDownloadUpdate: () -> Unit,
    onCreateBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onPauseDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onShareDiagnostics: () -> Unit,
    onRedoQuestionnaire: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    var languageExpanded by remember { mutableStateOf(false) }
    val blocker = remember { MutableInteractionSource() }
    val batteryContext = LocalContext.current
    val batteryLifecycleOwner = LocalLifecycleOwner.current
    var batteryCheckToken by remember { mutableStateOf(0) }
    val batteryUnrestricted = remember(batteryCheckToken) {
        batteryContext.getSystemService(PowerManager::class.java)
            ?.isIgnoringBatteryOptimizations(batteryContext.packageName) == true
    }
    DisposableEffect(batteryLifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) batteryCheckToken++
        }
        batteryLifecycleOwner.lifecycle.addObserver(observer)
        onDispose { batteryLifecycleOwner.lifecycle.removeObserver(observer) }
    }
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
            item { SettingsSectionLabel(strings.homeInterfaceSection) }
            item {
                SettingsToggle(
                    icon = Icons.Rounded.Home,
                    title = strings.compactHome,
                    subtitle = strings.compactHomeSubtitle,
                    checked = interfaceSettings.compactHome,
                    onCheckedChange = { onInterfaceSettings(interfaceSettings.copy(compactHome = it)) }
                )
            }
            item {
                SettingsToggle(
                    icon = Icons.Rounded.MusicNote,
                    title = strings.yourOrbitSetting,
                    subtitle = strings.showPersonalListening,
                    checked = interfaceSettings.showPersonalOrbit,
                    onCheckedChange = { onInterfaceSettings(interfaceSettings.copy(showPersonalOrbit = it)) }
                )
            }
            item {
                SettingsToggle(
                    icon = Icons.Rounded.GraphicEq,
                    title = strings.voicesSetting,
                    subtitle = strings.voicesSettingSubtitle,
                    checked = interfaceSettings.showResonance,
                    onCheckedChange = { onInterfaceSettings(interfaceSettings.copy(showResonance = it)) }
                )
            }
            item {
                SettingsToggle(
                    icon = Icons.Rounded.Notifications,
                    title = strings.newReleasesSetting,
                    subtitle = strings.showRecentReleases,
                    checked = interfaceSettings.showNewReleases,
                    onCheckedChange = { onInterfaceSettings(interfaceSettings.copy(showNewReleases = it)) }
                )
            }
            item {
                SettingsToggle(
                    icon = Icons.Rounded.Album,
                    title = strings.albumsForYouSetting,
                    subtitle = strings.showRecommendedAlbums,
                    checked = interfaceSettings.showAlbumsForYou,
                    onCheckedChange = { onInterfaceSettings(interfaceSettings.copy(showAlbumsForYou = it)) }
                )
            }
            item {
                SettingsToggle(
                    icon = Icons.Rounded.Person,
                    title = strings.trendingArtists,
                    subtitle = strings.showDiscoveredArtists,
                    checked = interfaceSettings.showTrendingArtists,
                    onCheckedChange = { onInterfaceSettings(interfaceSettings.copy(showTrendingArtists = it)) }
                )
            }
            item {
                SettingsToggle(
                    icon = Icons.Rounded.LocalFireDepartment,
                    title = strings.top50Charts,
                    subtitle = strings.showChartsCountry,
                    checked = interfaceSettings.showCharts,
                    onCheckedChange = { onInterfaceSettings(interfaceSettings.copy(showCharts = it)) }
                )
            }
            item { SettingsSectionLabel(strings.mobilePlayerSection) }
            item {
                SettingsToggle(
                    icon = Icons.Rounded.Speed,
                    title = strings.advancedGestures,
                    subtitle = strings.advancedGesturesSubtitle,
                    checked = interfaceSettings.playerGesturesEnabled,
                    onCheckedChange = { onInterfaceSettings(interfaceSettings.copy(playerGesturesEnabled = it)) }
                )
            }
            if (interfaceSettings.playerGesturesEnabled) {
                item {
                    SettingsChoiceRow(
                        icon = Icons.Rounded.SkipNext,
                        title = strings.doubleTapSeek,
                        subtitle = strings.doubleTapSeekSubtitle,
                        options = listOf("5" to "5 s", "10" to "10 s", "15" to "15 s", "30" to "30 s"),
                        selected = interfaceSettings.doubleTapSeekSeconds.toString(),
                        onSelect = { value -> onInterfaceSettings(interfaceSettings.copy(doubleTapSeekSeconds = value.toInt())) }
                    )
                }
                item {
                    SettingsChoiceRow(
                        icon = Icons.Rounded.Speed,
                        title = strings.longPress,
                        subtitle = strings.longPressSubtitle,
                        options = listOf("1.5" to "1.5×", "2.0" to "2×", "2.5" to "2.5×", "3.0" to "3×"),
                        selected = String.format(Locale.US, "%.1f", interfaceSettings.longPressSpeed),
                        onSelect = { value -> onInterfaceSettings(interfaceSettings.copy(longPressSpeed = value.toFloat())) }
                    )
                }
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
            item { SettingsSectionLabel(strings.downloadEngineSection) }
            item {
                SettingsChoiceRow(
                    icon = Icons.Rounded.Speed,
                    title = if (strings.code == "it") "Preset qualità" else "Quality preset",
                    subtitle = if (strings.code == "it") "Bilancia qualità, velocità e consumo dati" else "Balance quality, speed and data usage",
                    options = listOf(
                        LevyraDownloadPreset.Automatic.name to if (strings.code == "it") "Automatico" else "Automatic",
                        LevyraDownloadPreset.HighQuality.name to if (strings.code == "it") "Alta qualità" else "High quality",
                        LevyraDownloadPreset.DataSaver.name to if (strings.code == "it") "Risparmio dati" else "Data saver"
                    ),
                    selected = downloadSettings.preset.name,
                    onSelect = { value -> onDownloadSettings(downloadSettings.copy(preset = LevyraDownloadPreset.valueOf(value))) }
                )
            }
            item {
                SettingsChoiceRow(
                    icon = Icons.Rounded.Album,
                    title = if (strings.code == "it") "Organizzazione cartelle" else "Folder organization",
                    subtitle = if (strings.code == "it") "Salva per artista e album senza duplicare i file" else "Save by artist and album without duplicating files",
                    options = listOf(
                        LevyraDownloadFolderMode.Flat.name to if (strings.code == "it") "Levyra" else "Levyra",
                        LevyraDownloadFolderMode.Artist.name to if (strings.code == "it") "Artista" else "Artist",
                        LevyraDownloadFolderMode.ArtistAlbum.name to if (strings.code == "it") "Artista / Album" else "Artist / Album"
                    ),
                    selected = downloadSettings.folderMode.name,
                    onSelect = { value -> onDownloadSettings(downloadSettings.copy(folderMode = LevyraDownloadFolderMode.valueOf(value))) }
                )
            }
            item {
                SettingsChoiceRow(
                    icon = Icons.Rounded.Speed,
                    title = if (strings.code == "it") "Limite velocità" else "Speed limit",
                    subtitle = if (strings.code == "it") "Riduce l'uso della rete durante i download" else "Limit network usage while downloading",
                    options = listOf(
                        "0" to if (strings.code == "it") "Illimitato" else "Unlimited",
                        "512" to "512 Kbps",
                        "1024" to "1 Mbps",
                        "2048" to "2 Mbps",
                        "4096" to "4 Mbps",
                        "8192" to "8 Mbps"
                    ),
                    selected = downloadSettings.maxRateKbps.toString(),
                    onSelect = { value -> onDownloadSettings(downloadSettings.copy(maxRateKbps = value.toInt())) }
                )
            }
            item {
                SettingsToggle(
                    icon = Icons.Rounded.Download,
                    title = strings.wifiOnly,
                    subtitle = strings.wifiOnlySubtitle,
                    checked = downloadSettings.wifiOnly,
                    onCheckedChange = { onDownloadSettings(downloadSettings.copy(wifiOnly = it)) }
                )
            }
            item {
                SettingsToggle(
                    icon = Icons.Rounded.Bolt,
                    title = strings.chargingOnly,
                    subtitle = strings.chargingOnlySubtitle,
                    checked = downloadSettings.chargingOnly,
                    onCheckedChange = { onDownloadSettings(downloadSettings.copy(chargingOnly = it)) }
                )
            }
            item {
                SettingsToggle(
                    icon = Icons.Rounded.History,
                    title = strings.automaticResume,
                    subtitle = strings.partialDownloadResume,
                    checked = downloadSettings.resumable,
                    onCheckedChange = { onDownloadSettings(downloadSettings.copy(resumable = it)) }
                )
            }
            item {
                SettingsChoiceRow(
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    title = strings.simultaneousDownloads,
                    subtitle = strings.simultaneousDownloadsSubtitle,
                    options = listOf("1" to "1", "2" to "2", "3" to "3", "4" to "4"),
                    selected = downloadSettings.maxConcurrentDownloads.toString(),
                    onSelect = { value -> onDownloadSettings(downloadSettings.copy(maxConcurrentDownloads = value.toInt())) }
                )
            }
            item {
                SettingsToggle(
                    icon = Icons.Rounded.Album,
                    title = if (strings.code == "it") "Metadati incorporati" else "Embedded metadata",
                    subtitle = if (strings.code == "it") "Scrive titolo, artista e album nel file" else "Write title, artist and album into the file",
                    checked = downloadSettings.embedMetadata,
                    onCheckedChange = { onDownloadSettings(downloadSettings.copy(embedMetadata = it)) }
                )
            }
            item {
                SettingsToggle(
                    icon = Icons.Rounded.Palette,
                    title = if (strings.code == "it") "Copertina incorporata" else "Embedded artwork",
                    subtitle = if (strings.code == "it") "Inserisce la copertina ufficiale nel brano" else "Embed the official artwork into the track",
                    checked = downloadSettings.embedArtwork,
                    onCheckedChange = { onDownloadSettings(downloadSettings.copy(embedArtwork = it)) }
                )
            }
            item {
                SettingsToggle(
                    icon = Icons.Rounded.Verified,
                    title = if (strings.code == "it") "Verifica file" else "File verification",
                    subtitle = if (strings.code == "it") "Controlla firma, dimensione e leggibilità prima di completare" else "Validate signature, size and readability before completion",
                    checked = downloadSettings.verifyFile,
                    onCheckedChange = { onDownloadSettings(downloadSettings.copy(verifyFile = it)) }
                )
            }
            item {
                SettingsToggle(
                    icon = Icons.Rounded.DownloadDone,
                    title = if (strings.code == "it") "Evita duplicati" else "Skip duplicates",
                    subtitle = if (strings.code == "it") "Riutilizza i download già presenti e validi" else "Reuse existing valid downloads",
                    checked = downloadSettings.skipExisting,
                    onCheckedChange = { onDownloadSettings(downloadSettings.copy(skipExisting = it)) }
                )
            }
            if (downloadQueue.isNotEmpty()) {
                item {
                    DownloadQueueSettingsCard(
                        tasks = downloadQueue,
                        onPause = onPauseDownload,
                        onResume = onResumeDownload,
                        onCancel = onCancelDownload
                    )
                }
            }
            item { SettingsSectionLabel(strings.lyricsAnalysisSection) }
            item {
                SettingsInfoCard(
                    icon = Icons.Rounded.Insights,
                    title = strings.lyricsAnalysisCompact,
                    subtitle = strings.lyricsAnalysisCompactSubtitle
                )
            }
            item { SettingsSectionLabel(strings.backupRestoreSection) }
            item {
                SettingsButton(
                    icon = Icons.Rounded.Download,
                    title = strings.createDataBackup,
                    subtitle = strings.createDataBackupSubtitle,
                    onClick = onCreateBackup
                )
            }
            item {
                SettingsButton(
                    icon = Icons.Rounded.History,
                    title = strings.restoreBackup,
                    subtitle = strings.restoreBackupSubtitle,
                    onClick = onRestoreBackup
                )
            }
            item { SettingsSectionLabel(strings.playbackResilienceSection) }
            item {
                SettingsButton(
                    icon = Icons.Rounded.Bolt,
                    title = strings.batteryUnrestricted,
                    subtitle = if (batteryUnrestricted) strings.batteryUnrestrictedActive else strings.batteryUnrestrictedSubtitle,
                    onClick = {
                        if (!batteryUnrestricted) {
                            val request = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                .setData(Uri.parse("package:${batteryContext.packageName}"))
                            runCatching { batteryContext.startActivity(request) }.onFailure {
                                runCatching {
                                    batteryContext.startActivity(
                                        Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    )
                                }
                            }
                        }
                    }
                )
            }
            item {
                SettingsButton(
                    icon = Icons.Rounded.Share,
                    title = strings.exportSafeDiagnostics,
                    subtitle = if (playbackDiagnostics.isBlank()) strings.generateResolverTrace else strings.safeDiagnosticsSubtitle,
                    onClick = onShareDiagnostics
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
            if (BuildConfig.UPSTREAM_UPDATES_ENABLED) {
                item {
                    SettingsUpdateCard(
                        updateInfo = updateInfo,
                        isChecking = isCheckingUpdates,
                        onCheck = onCheckUpdates,
                        onDownload = onDownloadUpdate
                    )
                }
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
                            "${strings.madeWithBy} ",
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
private fun SettingsChoiceRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Surface(
        color = LevyraAdaptiveCard,
        border = BorderStroke(1.dp, LevyraAdaptiveHairline),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(40.dp).background(LevyraViolet.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = LevyraViolet, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    Text(subtitle, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(options, key = { it.first }) { option ->
                    val active = option.first == selected
                    Surface(
                        color = if (active) LevyraCyan.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.055f),
                        border = BorderStroke(1.dp, if (active) LevyraCyan.copy(alpha = 0.42f) else LevyraAdaptiveHairline),
                        shape = CircleShape,
                        modifier = Modifier.pressable { onSelect(option.first) }
                    ) {
                        Text(
                            option.second,
                            color = if (active) LevyraCyan else LevyraText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsInfoCard(icon: ImageVector, title: String, subtitle: String) {
    Surface(
        color = LevyraViolet.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, LevyraViolet.copy(alpha = 0.22f)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(40.dp).background(LevyraViolet.copy(alpha = 0.16f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = LevyraViolet, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = LevyraMuted, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DownloadQueueSettingsCard(
    tasks: List<OfflineDownloadTask>,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit
) {
    Surface(
        color = LevyraAdaptiveCard,
        border = BorderStroke(1.dp, LevyraAdaptiveHairline),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(LocalLevyraStrings.current.persistentQueue, color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Black)
            tasks.forEach { task ->
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(task.title.ifBlank { LocalLevyraStrings.current.song }, color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${LocalLevyraStrings.current.localizeDownloadState(task.state)} • ${task.progress.coerceIn(0, 100)}%", color = LevyraMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        val canResume = task.state == "PAUSED" || task.state == "FAILED"
                        val actionDescription = if (canResume) LocalLevyraStrings.current.formatResumeDownload(task.title) else LocalLevyraStrings.current.formatPauseDownload(task.title)
                        IconButton(onClick = { if (canResume) onResume(task.taskKey) else onPause(task.taskKey) }) {
                            Icon(if (canResume) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, actionDescription, tint = LevyraCyan)
                        }
                        IconButton(onClick = { onCancel(task.taskKey) }) {
                            Icon(Icons.Rounded.Close, LocalLevyraStrings.current.formatCancelDownload(task.title), tint = LevyraPink)
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(5.dp).background(Color.White.copy(alpha = 0.08f), CircleShape)) {
                        Box(modifier = Modifier.fillMaxWidth(task.progress.coerceIn(0, 100) / 100f).fillMaxHeight().background(LevyraCyan, CircleShape))
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
    val availableUpdate = updateInfo?.takeIf { it.isNewer }
    val hasUpdate = availableUpdate != null
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
                        text = if (hasUpdate) LocalLevyraStrings.current.updateAvailable else LocalLevyraStrings.current.updates,
                        color = LevyraText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = when {
                            isChecking -> LocalLevyraStrings.current.checkingLatestVersion
                            availableUpdate != null -> LocalLevyraStrings.current.formatLatestVersionReady(availableUpdate.latestVersionName)
                            updateInfo != null -> LocalLevyraStrings.current.latestInstalled
                            else -> LocalLevyraStrings.current.checkNewVersions
                        },
                        color = LevyraMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            availableUpdate?.let { info ->
                Surface(
                    color = Color.Black.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(info.releaseTitle.ifBlank { "LEVYRA ${info.latestVersionName}" }, color = LevyraText, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = if (info.directApk) LocalLevyraStrings.current.signedApkReady else LocalLevyraStrings.current.releasePageReady,
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
                    label = if (isChecking) LocalLevyraStrings.current.checking else LocalLevyraStrings.current.check,
                    accent = LevyraCyan,
                    enabled = !isChecking,
                    modifier = Modifier.weight(1f),
                    onClick = onCheck
                )
                if (hasUpdate) {
                    SettingsMiniButton(
                        label = LocalLevyraStrings.current.download,
                        accent = LevyraViolet,
                        enabled = !isChecking,
                        modifier = Modifier.weight(1f),
                        onClick = onDownload
                    )
                }
            }
            Text(
                text = LocalLevyraStrings.current.formatInstalledVersion(BuildConfig.VERSION_NAME),
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
            color = LevyraText,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.2).sp
        )
        Box(
            modifier = Modifier
                .padding(start = 3.dp, bottom = 4.dp)
                .size(dotSize)
                .background(LevyraCyan, CircleShape)
        )
    }
}

@Composable
private fun GreetingBar(userName: String, isResolving: Boolean, onSettings: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
                modifier = Modifier.weight(1f)
            ) {
                LevyraLogoMark(size = 42.dp)
                LevyraWordmark(fontSize = 20.sp, dotSize = 4.dp)
            }
            Surface(
                color = if (LevyraIsLight) Color.White.copy(alpha = 0.90f) else Color(0xFF0C0D10),
                border = BorderStroke(Dp.Hairline, LevyraAdaptiveSoftHairline),
                shape = RoundedCornerShape(15.dp),
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
        Text(
            text = LocalLevyraStrings.current.formatGreeting(userName, java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)),
            color = LevyraText,
            fontSize = 24.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.6).sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
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
                            Text(LocalLevyraStrings.current.discoveryFlow, color = LevyraText, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                        }
                    }
                    Surface(color = Color.White.copy(alpha = 0.1f), shape = CircleShape, modifier = Modifier.pressable(onClick = onPlayAll)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, tint = LevyraText, modifier = Modifier.size(15.dp))
                            Text(LocalLevyraStrings.current.play, color = LevyraText, fontSize = 11.sp, fontWeight = FontWeight.Black)
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
                            MetroStatPill(Icons.AutoMirrored.Rounded.QueueMusic, queueCount.coerceAtLeast(tracks.size).toString(), LocalLevyraStrings.current.queue)
                            MetroStatPill(Icons.Rounded.Favorite, favoritesCount.toString(), LocalLevyraStrings.current.saved)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    MetroActionButton(
                        icon = Icons.Rounded.PlayArrow,
                        text = if (currentTrack?.id == hero.id && isPlaying) LocalLevyraStrings.current.openPlayer else LocalLevyraStrings.current.play,
                        accent = LevyraCyan,
                        modifier = Modifier.weight(1f),
                        onClick = { onPrimary(hero) }
                    )
                    MetroActionButton(
                        icon = Icons.Rounded.LibraryMusic,
                        text = LocalLevyraStrings.current.library,
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
                            Text(LocalLevyraStrings.current.continueListening, color = LevyraCyan, fontSize = 11.sp, fontWeight = FontWeight.Black)
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
                label = LocalLevyraStrings.current.mixForYou,
                accent = LevyraCyan,
                enabled = hasSuggestions,
                modifier = Modifier.weight(1f),
                onClick = onShuffle
            )
            QuickAction(
                icon = Icons.Rounded.Favorite,
                label = LocalLevyraStrings.current.favoritesPlain,
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
        color = if (LevyraIsLight) LevyraAdaptiveChip else Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, if (LevyraIsLight) LevyraAdaptiveHairline else Color.White.copy(alpha = 0.09f)),
        shape = CircleShape,
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onFocus)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Icon(Icons.Rounded.Search, null, tint = LevyraMuted, modifier = Modifier.size(20.dp))
            Text(
                text = if (query.isEmpty()) LocalLevyraStrings.current.searchSongsArtists else query,
                color = if (query.isEmpty()) LevyraMuted else LevyraText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Rounded.Mic, null, tint = LevyraMuted, modifier = Modifier.size(19.dp))
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
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (selected) {
                            Brush.linearGradient(listOf(LevyraCyan, LevyraViolet))
                        } else {
                            SolidColor(if (LevyraIsLight) Color.White.copy(alpha = 0.82f) else Color(0xFF0C0D10))
                        },
                        CircleShape
                    )
                    .then(
                        if (selected) Modifier
                        else Modifier.border(Dp.Hairline, LevyraAdaptiveSoftHairline, CircleShape)
                    )
                    .pressable(onClick = { onSelect(mood) })
            ) {
                Text(
                    text = mood.title,
                    color = if (selected) Color.White else LevyraText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 17.dp, vertical = 11.dp)
                )
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
            SectionAccentBar(height = 22.dp, width = 4.dp)
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
        HomePlayAllButton(onClick = onPlayAll)
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
                modifier = Modifier.width(156.dp).shimmer(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(166.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(134.dp)
                            .align(Alignment.TopCenter)
                            .offset(y = 6.dp)
                            .graphicsLayer { rotationZ = 5f }
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                    )
                    Box(
                        modifier = Modifier
                            .size(148.dp)
                            .align(Alignment.BottomCenter)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = 0.075f))
                    )
                }
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
    val effectiveAnimationsEnabled = animationsEnabled && LocalAnimationsEnabled.current
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(start = HomeHorizontalInset, end = HomeHorizontalShelfEndPadding)
    ) {
        itemsIndexed(
            items = albums,
            key = { index, album -> "home-album-$index-${album.browseId.ifBlank { "${album.title.trim().lowercase()}|${album.artist.trim().lowercase()}" }}" },
            contentType = { _, _ -> "home-album-card" }
        ) { _, album ->
            val interaction = remember { MutableInteractionSource() }
            val isPressed by interaction.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed && effectiveAnimationsEnabled) 0.95f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "homeAlbumScale"
            )
            Column(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .width(156.dp)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = { onOpen(album) }
                    ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(166.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(134.dp)
                            .align(Alignment.TopCenter)
                            .offset(y = 4.dp)
                            .graphicsLayer { rotationZ = -7f }
                            .clip(RoundedCornerShape(18.dp))
                            .background(cinematicGlassBrush(LevyraViolet, LevyraPink, 0.9f))
                            .border(Dp.Hairline, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                    )
                    Box(
                        modifier = Modifier
                            .size(134.dp)
                            .align(Alignment.TopCenter)
                            .offset(y = 9.dp)
                            .graphicsLayer { rotationZ = 5f }
                            .clip(RoundedCornerShape(18.dp))
                            .background(cinematicGlassBrush(LevyraCyan, LevyraViolet, 0.9f))
                            .border(Dp.Hairline, Color.White.copy(alpha = 0.10f), RoundedCornerShape(18.dp))
                    )
                    Surface(
                        color = CinematicGlass.copy(alpha = 0.3f),
                        border = BorderStroke(Dp.Hairline, Color.White.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(18.dp),
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .size(148.dp)
                            .align(Alignment.BottomCenter)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (album.thumbnailUrl.isNotBlank()) {
                                StableRemoteArtwork(
                                    url = album.thumbnailUrl,
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
                                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.34f))))
                            )
                            if (album.year.isNotBlank()) {
                                Surface(
                                    color = CinematicGlassDeep.copy(alpha = 0.55f),
                                    border = BorderStroke(Dp.Hairline, Color.White.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = album.year,
                                        color = Color.White,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            if (album.explicit) {
                                Surface(
                                    color = CinematicGlassDeep.copy(alpha = 0.55f),
                                    border = BorderStroke(Dp.Hairline, Color.White.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(7.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Explicit,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier
                                            .padding(3.dp)
                                            .size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Column(modifier = Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(album.title, color = LevyraText, fontSize = 13.5.sp, lineHeight = 15.5.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val subtitle = listOf(LocalLevyraStrings.current.albumPlain, album.artist).filter { it.isNotBlank() }.joinToString(" • ")
                    Text(subtitle, color = LevyraMuted, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun AlbumCardRow(tracks: List<Track>, currentId: String?, animationsEnabled: Boolean, onPlay: (Track) -> Unit) {
    if (tracks.isEmpty()) return
    val effectiveAnimationsEnabled = animationsEnabled && LocalAnimationsEnabled.current
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(start = HomeHorizontalInset, end = HomeHorizontalShelfEndPadding)
    ) {
        itemsIndexed(
            items = tracks,
            key = { index, track -> "album-card-$index-${track.id.ifBlank { "${track.title.trim().lowercase()}|${track.artist.trim().lowercase()}" }}" },
            contentType = { _, _ -> "album-card" }
        ) { index, track ->
            val isCurrent = track.id == currentId
            val interaction = remember { MutableInteractionSource() }
            val isPressed by interaction.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed && effectiveAnimationsEnabled) 0.95f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "scale"
            )
            Column(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .width(148.dp)
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = { onPlay(track) }
                    ),
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
                                Text(if (isCurrent) LocalLevyraStrings.current.activeIndicator else "${index + 1}", color = LevyraText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Column(modifier = Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(track.title, color = if (isCurrent) LevyraCyan else LevyraText, fontSize = 13.5.sp, lineHeight = 15.5.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val strings = LocalLevyraStrings.current
                    val kind = if (track.album.isNotBlank() && track.album != track.title && track.album != "YouTube Music") strings.albumPlain else strings.singlePlain
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
    val strings = LocalLevyraStrings.current
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
                    contentDescription = LocalLevyraStrings.current.songOptions,
                    tint = LevyraMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(if (isFavorite) LocalLevyraStrings.current.removeFromFavorites else LocalLevyraStrings.current.addToFavorites) },
                    leadingIcon = { Icon(if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null) },
                    onClick = {
                        expanded = false
                        onFavorite()
                    }
                )
                DropdownMenuItem(
                    text = { Text(LocalLevyraStrings.current.addToPlaylist) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) },
                    onClick = {
                        expanded = false
                        onAddToPlaylist()
                    }
                )
                DropdownMenuItem(
                    text = { Text(LocalLevyraStrings.current.addToQueue) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, null) },
                    onClick = {
                        expanded = false
                        onAddToQueue()
                    }
                )
                DropdownMenuItem(
                    text = { Text(LocalLevyraStrings.current.download) },
                    leadingIcon = { Icon(Icons.Rounded.Download, null) },
                    onClick = {
                        expanded = false
                        onDownload()
                    }
                )
                DropdownMenuItem(
                    text = { Text(LocalLevyraStrings.current.share) },
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
                        context.startActivity(Intent.createChooser(intent, strings.shareSong))
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
                if (rank <= 3) {
                    Text(
                        text = rank.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        style = TextStyle(brush = Brush.verticalGradient(listOf(LevyraCyan, LevyraViolet)))
                    )
                } else {
                    Text(
                        text = rank.toString(),
                        color = LevyraMuted,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }
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
                        contentDescription = LocalLevyraStrings.current.options,
                        tint = LevyraMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isFavorite) LocalLevyraStrings.current.removeFromFavorites else LocalLevyraStrings.current.addToFavorites) },
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
                        text = { Text(LocalLevyraStrings.current.addToQueue) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.QueueMusic, null) },
                        onClick = {
                            expanded = false
                            onAddToQueue()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(LocalLevyraStrings.current.addToPlaylist) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) },
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
                text = LocalLevyraStrings.current.youMightAlsoLike,
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
                    contentDescription = LocalLevyraStrings.current.favorite,
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
    val strings = LocalLevyraStrings.current
    val chips = buildList {
        add(SearchFilter.All to strings.all)
        add(SearchFilter.Songs to strings.songsPlain)
        if (hasArtists) add(SearchFilter.Artists to strings.artistsLabelPlural)
        if (hasAlbums) add(SearchFilter.Albums to strings.albumsPlain)
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
        Text(LocalLevyraStrings.current.topResult, color = LevyraCyan, fontSize = 13.sp, fontWeight = FontWeight.Black)
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
                            Text(if (isPlaying) LocalLevyraStrings.current.playing else LocalLevyraStrings.current.play, color = LevyraBlack, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Surface(color = Color.White.copy(alpha = 0.08f), shape = CircleShape, modifier = Modifier.size(46.dp).clickable { onAddToPlaylist() }) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null, tint = LevyraText, modifier = Modifier.size(22.dp))
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
                Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = LocalLevyraStrings.current.addToPlaylist, tint = LevyraMuted, modifier = Modifier.size(24.dp))
            }
            IconButton(onClick = onFavorite, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = LocalLevyraStrings.current.favorite,
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
        itemsIndexed(
            items = artists,
            key = { index, hit -> "artist-hit-$index-${hit.browseId.ifBlank { hit.name.trim().lowercase(Locale.ROOT) }}" },
            contentType = { _, _ -> "artist-hit" }
        ) { _, hit ->
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
                        StableRemoteArtwork(
                            url = hit.thumbnailUrl,
                            contentDescription = hit.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize().clip(CircleShape)
                        )
                    } else {
                        Icon(Icons.Rounded.Person, null, tint = LevyraText, modifier = Modifier.size(40.dp))
                    }
                }
                Text(hit.name, color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(LocalLevyraStrings.current.artistLabel, color = LevyraMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AlbumHitRow(albums: List<AlbumHit>, onClick: (AlbumHit) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        itemsIndexed(
            items = albums,
            key = { index, album -> "album-hit-$index-${album.browseId.ifBlank { "${album.title.trim().lowercase(Locale.ROOT)}-${album.artist.trim().lowercase(Locale.ROOT)}" }}" },
            contentType = { _, _ -> "album-hit" }
        ) { _, album ->
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
                    Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = LocalLevyraStrings.current.addToPlaylist, tint = LevyraMuted, modifier = Modifier.size(24.dp))
                }
            }
            IconButton(onClick = onFavorite, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = LocalLevyraStrings.current.favorite,
                    tint = if (isFavorite) LevyraPink else LevyraMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
            if (onRemove != null) {
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.Delete, contentDescription = LocalLevyraStrings.current.remove, tint = LevyraMuted, modifier = Modifier.size(22.dp))
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
            isDownloaded -> Icon(Icons.Rounded.DownloadDone, contentDescription = LocalLevyraStrings.current.downloaded, tint = LevyraCyan, modifier = Modifier.size(23.dp))
            else -> Icon(Icons.Rounded.Download, contentDescription = LocalLevyraStrings.current.download, tint = LevyraMuted, modifier = Modifier.size(23.dp))
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
    val accentCenter = accentStart.playerMix(accentEnd, 0.48f)
    val rawMiniStart = accentStart.playerMix(Color.Black, 0.34f)
    val rawMiniEnd = accentEnd.playerMix(Color.Black, 0.48f)
    val miniGradient = remember(accentStart, accentEnd) {
        PlayerContrastGradient(
            start = rawMiniStart.playerAdjustBackgroundFor(Color.White, PlayerStrongContrast).color,
            end = rawMiniEnd.playerAdjustBackgroundFor(Color.White, PlayerStrongContrast).color,
            content = Color.White
        )
    }
    val miniBackgrounds = remember(miniGradient) {
        listOf(miniGradient.start, Color(0xFF111114), Color(0xFF09090C), miniGradient.end)
    }
    val miniPrimaryContent = miniGradient.content
    val miniSecondaryContent = remember(miniGradient) {
        miniPrimaryContent.playerMutedContentColor(miniBackgrounds)
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(420, easing = LinearOutSlowInEasing),
        label = "mini-progress"
    )
    val containerShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    Surface(
        color = Color.Transparent,
        shape = containerShape,
        border = BorderStroke(1.dp, accentCenter.copy(alpha = 0.24f)),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = containerShape,
                clip = false,
                ambientColor = accentStart.copy(alpha = 0.16f),
                spotColor = Color.Black.copy(alpha = 0.82f)
            )
    ) {
        Column(
            modifier = Modifier.background(
                Brush.linearGradient(
                    listOf(
                        miniGradient.start,
                        Color(0xFF111114),
                        Color(0xFF09090C),
                        miniGradient.end
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
                        .shadow(8.dp, RoundedCornerShape(15.dp), clip = false)
                        .clip(RoundedCornerShape(15.dp))
                        .pressable(onClick = onOpen)
                ) {
                    CoverImage(track, Modifier.fillMaxSize())
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)), RoundedCornerShape(15.dp))
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
                        color = miniPrimaryContent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.1).sp,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE, repeatDelayMillis = 2400)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = track.artist,
                        color = miniSecondaryContent,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                MiniPlayerToggleButton(
                    isPlaying = isPlaying,
                    isResolving = isResolving,
                    buttonColor = miniPrimaryContent,
                    onToggle = onToggle
                )
                PlayerRoundIconButton(
                    icon = Icons.Rounded.SkipNext,
                    contentDescription = LocalLevyraStrings.current.next,
                    size = 38.dp,
                    iconSize = 21.dp,
                    tint = miniPrimaryContent,
                    background = miniPrimaryContent.copy(alpha = 0.08f),
                    borderColor = miniPrimaryContent.copy(alpha = 0.16f),
                    onClick = onNext
                )
                PlayerRoundIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = LocalLevyraStrings.current.closePlayer,
                    size = 38.dp,
                    iconSize = 19.dp,
                    tint = miniSecondaryContent,
                    background = miniPrimaryContent.copy(alpha = 0.06f),
                    borderColor = miniPrimaryContent.copy(alpha = 0.13f),
                    onClick = onClose
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(miniPrimaryContent.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(99.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    accentStart.playerMix(Color.White, 0.16f),
                                    accentEnd.playerMix(Color.White, 0.10f)
                                )
                            )
                        )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun MiniPlayerToggleButton(
    isPlaying: Boolean,
    isResolving: Boolean,
    buttonColor: Color,
    onToggle: () -> Unit
) {
    val playBg = buttonColor.copy(alpha = 1f)
    val playTint = Color.White.playerContentColor(listOf(playBg))
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(playBg, CircleShape)
            .pressable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        if (isResolving) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = playTint
            )
        } else {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    (fadeIn(tween(140)) + scaleIn(initialScale = 0.7f, animationSpec = tween(140))) togetherWith
                        (fadeOut(tween(100)) + scaleOut(targetScale = 0.7f, animationSpec = tween(100)))
                },
                label = "mini-play-icon"
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playing) LocalLevyraStrings.current.pause else LocalLevyraStrings.current.play,
                    tint = playTint,
                    modifier = Modifier.size(22.dp)
                )
            }
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
private fun CircleIconButton(
    icon: ImageVector,
    tint: Color,
    background: Color,
    onClick: () -> Unit,
    contentDescription: String? = null
) {
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
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun ExploreScreen(viewModel: ExploreViewModel, state: LevyraUiState) {
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
                                    context.startActivity(Intent.createChooser(intent, strings.shareVia))
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
                    Icon(Icons.Rounded.MoreVert, contentDescription = LocalLevyraStrings.current.more, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(CinematicGlass)
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isFavorite) LocalLevyraStrings.current.removeFromFavorites else LocalLevyraStrings.current.addToFavorites, color = Color.White) },
                        onClick = {
                            onFavorite()
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(LocalLevyraStrings.current.share, color = Color.White) },
                        onClick = {
                            onShare()
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(LocalLevyraStrings.current.addToPlaylist, color = Color.White) },
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
    val surfaceColor = if (LevyraIsLight) Color.White.copy(alpha = 0.96f) else Color(0xF708080B)
    val linePrimary = if (LevyraIsLight) Color(0x1A11131F) else Color.White.copy(alpha = 0.07f)
    Surface(
        color = surfaceColor,
        shape = RoundedCornerShape(0.dp),
        shadowElevation = if (LevyraIsLight) 6.dp else 10.dp,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = linePrimary,
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 7.dp),
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
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            title,
            color = LevyraText,
            fontSize = 32.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.8).sp
        )
        Text(subtitle, color = LevyraMuted, fontSize = 14.5.sp, fontWeight = FontWeight.Medium, lineHeight = 19.sp)
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
        state.isSearching -> GlassMessage(LocalLevyraStrings.current.searchingYouTubeMusic, LevyraCyan)
        state.searchError != null -> GlassMessage(state.searchError, LevyraOrange)
        state.searchResults.isNotEmpty() -> GlassMessage(LocalLevyraStrings.current.formatSearchResults(state.searchResults.size), LevyraCyan)
        else -> GlassMessage(LocalLevyraStrings.current.emptySearchPrompt, LevyraMuted)
    }
}

@Composable
private fun StatusBlock(state: LevyraUiState) {
    if (state.homeError != null || state.playerError != null) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.homeError?.let { GlassMessage(it, LevyraOrange) }
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
                    loading -> LocalLevyraStrings.current.searchingLyrics
                    available -> LocalLevyraStrings.current.showLyrics
                    else -> LocalLevyraStrings.current.lyricsUnavailable
                },
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
        }
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
