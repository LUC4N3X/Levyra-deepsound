package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.theme.*
import com.luc4n3x.levyra.ui.theme.nocturneMiniSettings
import com.luc4n3x.levyra.ui.theme.nocturneNavSettings
import com.luc4n3x.levyra.ui.components.levyraMarquee
import com.luc4n3x.levyra.ui.components.LevyraBackButton
import com.luc4n3x.levyra.ui.components.levyraShimmerSweep
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.luc4n3x.levyra.ui.components.rememberLevyraShimmerProgress
import com.luc4n3x.levyra.ui.components.LevyraPlayAllButton
import com.luc4n3x.levyra.ui.components.LevyraPlayAllIconButton
import com.luc4n3x.levyra.ui.components.LevyraNowPlayingBadge
import com.luc4n3x.levyra.ui.components.rememberLevyraPlayMorphShape
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.graphics.shapes.RoundedPolygon
import com.luc4n3x.levyra.ui.components.levyraCookieLandingShape
import com.luc4n3x.levyra.ui.components.levyraGroupedGridShape
import com.luc4n3x.levyra.ui.components.levyraGroupedListShape
import com.luc4n3x.levyra.ui.components.rememberLevyraAvatarPressShape
import androidx.compose.foundation.text.BasicText
import com.luc4n3x.levyra.ui.components.LevyraFilterPill
import com.luc4n3x.levyra.ui.components.LevyraSectionAction
import com.luc4n3x.levyra.ui.components.LevyraSectionHeader
import com.luc4n3x.levyra.ui.components.LevyraSectionPlayAll
import com.luc4n3x.levyra.ui.components.LevyraSectionShowAll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontFamily
import com.luc4n3x.levyra.ui.components.levyraExpressiveToggleCorner
import com.luc4n3x.levyra.ui.components.levyraExpressiveCorner
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.CircularWavyProgressIndicator
import com.luc4n3x.levyra.domain.ChartMarketDirectory
import com.luc4n3x.levyra.domain.ExploreCategory
import com.luc4n3x.levyra.domain.SpeedDial
import com.luc4n3x.levyra.domain.RecommendationFeedbackKind
import com.luc4n3x.levyra.domain.isExcludableArtist
import androidx.compose.runtime.key
import com.luc4n3x.levyra.ui.components.LevyraPlayPauseGlyph
import com.luc4n3x.levyra.ui.artwork.ArtworkBackdropWash
import com.luc4n3x.levyra.ui.components.LevyraAdaptiveDockSurface
import com.luc4n3x.levyra.ui.components.animatedCompaction
import com.luc4n3x.levyra.ui.components.dockClipHeight
import com.luc4n3x.levyra.ui.components.dockFade
import com.luc4n3x.levyra.ui.components.dockFoldHeight
import com.luc4n3x.levyra.ui.components.dockFoldWidth
import com.luc4n3x.levyra.ui.components.dockLerpHeight
import com.luc4n3x.levyra.ui.components.dockLerpSize
import com.luc4n3x.levyra.ui.components.rememberLevyraDockState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import com.luc4n3x.levyra.ui.components.LevyraIonicons
import com.luc4n3x.levyra.ui.support.SupportLevyraSettingsLink
import com.luc4n3x.levyra.ui.components.PlaybackDiagnosticsDialog
import com.luc4n3x.levyra.ui.player.PlayerVideoTransform
import com.luc4n3x.levyra.ui.player.applyPlayerVideoGesture
import com.luc4n3x.levyra.ui.player.playerVideoZoomGestures
import com.luc4n3x.levyra.ui.components.formatSeekbarMillis
import com.luc4n3x.levyra.ui.components.PlayerControlLabels
import com.luc4n3x.levyra.ui.components.PlayerGlassIconButton
import com.luc4n3x.levyra.ui.components.PlayerIcon
import com.luc4n3x.levyra.feature.settings.SettingsSearchIndex
import com.luc4n3x.levyra.feature.settings.SettingsSearchResult
import com.luc4n3x.levyra.feature.cast.CastRouteButton
import com.luc4n3x.levyra.ui.components.PremiumSeekbar
import com.luc4n3x.levyra.ui.selection.TrackSelectionActions
import com.luc4n3x.levyra.ui.selection.TrackSelectionBar
import com.luc4n3x.levyra.ui.selection.TrackSelectionState
import com.luc4n3x.levyra.ui.selection.resolveSelected
import com.luc4n3x.levyra.ui.selection.rememberTrackSelectionState
import com.luc4n3x.levyra.ui.selection.trackSelectionKey
import com.luc4n3x.levyra.ui.components.SpringIconButton
import com.luc4n3x.levyra.ui.components.playerGlass
import com.luc4n3x.levyra.ui.artwork.ArtworkPreviewOverlay
import com.luc4n3x.levyra.ui.artwork.LivingArtworkColors
import com.luc4n3x.levyra.ui.artwork.livingArtworkColors
import com.luc4n3x.levyra.ui.artwork.SeamlessArtworkImage
import com.luc4n3x.levyra.ui.artwork.rememberArtworkPalette
import com.luc4n3x.levyra.ui.album.AlbumNeutralPaletteEnd
import com.luc4n3x.levyra.ui.album.AlbumNeutralPaletteStart
import com.luc4n3x.levyra.ui.album.AlbumStageColors
import com.luc4n3x.levyra.ui.album.albumContentGutter
import com.luc4n3x.levyra.ui.album.albumStageColors
import com.luc4n3x.levyra.ui.components.LevyraCardCaption
import com.luc4n3x.levyra.ui.components.LevyraDockProgress
import com.luc4n3x.levyra.ui.components.LevyraRowText
import com.luc4n3x.levyra.ui.components.levyraDockSurface
import com.luc4n3x.levyra.ui.media.ImmersiveMediaActionRow
import com.luc4n3x.levyra.ui.media.ImmersiveMediaHero
import com.luc4n3x.levyra.ui.media.ImmersiveMediaPrimaryAction
import com.luc4n3x.levyra.ui.media.ImmersiveMediaQuietAction
import com.luc4n3x.levyra.ui.media.ImmersiveTopBarButtonFill
import com.luc4n3x.levyra.ui.media.ImmersiveTopBarButtonSize
import com.luc4n3x.levyra.ui.media.ImmersiveMediaTopBar
import com.luc4n3x.levyra.ui.media.animatedImmersiveMediaColors
import com.luc4n3x.levyra.ui.media.immersiveHeroHeight
import com.luc4n3x.levyra.ui.theme.LevyraOnAccent
import com.luc4n3x.levyra.ui.lyrics.LYRICS_SHARE_MAX_LINES
import com.luc4n3x.levyra.ui.lyrics.lyricsProviderDisplayName
import com.luc4n3x.levyra.ui.lyrics.lyricsShouldFollowActiveLine
import com.luc4n3x.levyra.ui.lyrics.LyricsShareSheet
import com.luc4n3x.levyra.ui.lyrics.LyricsShareSnapshot
import com.luc4n3x.levyra.ui.lyrics.isLyricsShareSelectable
import com.luc4n3x.levyra.ui.lyrics.lyricsShareSelectedLines
import com.luc4n3x.levyra.ui.lyrics.lyricsShareSnapshot
import com.luc4n3x.levyra.ui.lyrics.toggleLyricsShareSelection
import com.luc4n3x.levyra.ui.lyrics.rememberLyricsAudioOutputRoute
import com.luc4n3x.levyra.ui.lyrics.KARAOKE_VISUAL_LEAD_MS
import com.luc4n3x.levyra.ui.lyrics.LYRICS_INSTRUMENTAL_DOT_COUNT
import com.luc4n3x.levyra.ui.lyrics.LyricsInstrumentalGap
import com.luc4n3x.levyra.ui.lyrics.LyricsPlaybackClock
import com.luc4n3x.levyra.ui.lyrics.TimedLyricText
import com.luc4n3x.levyra.ui.lyrics.activeLyricsInstrumentalGap
import com.luc4n3x.levyra.ui.lyrics.activeLyricIndex
import com.luc4n3x.levyra.ui.lyrics.LyricsSyncQuickBar
import com.luc4n3x.levyra.ui.lyrics.LyricsSyncSheet
import com.luc4n3x.levyra.ui.lyrics.calibratedLyricsTrackOffset
import com.luc4n3x.levyra.ui.lyrics.lyricsOffsetIsolated
import com.luc4n3x.levyra.ui.i18n.lyricsSyncCopy
import com.luc4n3x.levyra.ui.lyrics.adaptiveLyricFontSizeSp
import com.luc4n3x.levyra.ui.i18n.homeVideoShelfTitle
import com.luc4n3x.levyra.ui.i18n.homeStudioCopy
import com.luc4n3x.levyra.ui.lyrics.lyricLineScaleSafeWidthPx
import com.luc4n3x.levyra.ui.lyrics.buildTimedLyricText
import com.luc4n3x.levyra.ui.lyrics.karaokeCharacterProgress
import com.luc4n3x.levyra.ui.lyrics.lyricsOffsetPosition
import com.luc4n3x.levyra.ui.lyrics.lyricsInstrumentalDotIntensity
import com.luc4n3x.levyra.ui.lyrics.lyricsInstrumentalGaps
import com.luc4n3x.levyra.ui.lyrics.lyricsInstrumentalProgress
import com.luc4n3x.levyra.ui.lyrics.LocalLivePlaybackPosition
import com.luc4n3x.levyra.ui.lyrics.LyricsAmbientBackdrop
import com.luc4n3x.levyra.ui.lyrics.rememberLyricsPlaybackClock
import com.luc4n3x.levyra.ui.lyrics.LYRICS_LANDSCAPE_CHROME_HEIGHT_DP
import com.luc4n3x.levyra.ui.lyrics.LYRICS_LANDSCAPE_PANE_GAP_DP
import com.luc4n3x.levyra.ui.lyrics.LyricsImmersiveSystemBars
import com.luc4n3x.levyra.ui.lyrics.LyricsLandscapeArtworkPane
import com.luc4n3x.levyra.ui.lyrics.LyricsLandscapeBackdrop
import com.luc4n3x.levyra.ui.lyrics.LyricsLandscapeChromeScrim
import com.luc4n3x.levyra.ui.lyrics.lyricsLandscapeLayoutActive
import com.luc4n3x.levyra.ui.lyrics.lyricsLandscapeMetrics
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraPlayerShapes
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign
import com.luc4n3x.levyra.ui.player.*
import com.luc4n3x.levyra.domain.PlayerVisualMode
import com.luc4n3x.levyra.domain.PlayerBackgroundMode
import com.luc4n3x.levyra.domain.ReactiveAuraPreset
import com.luc4n3x.levyra.domain.PlayerColorMode
import com.luc4n3x.levyra.domain.PlayerDoubleTapAction
import com.luc4n3x.levyra.domain.PlayerLongPressAction
import com.luc4n3x.levyra.domain.PlayerVerticalSwipeAction
import com.luc4n3x.levyra.domain.ResonanceCommentSnippet
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.Spring
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import com.luc4n3x.levyra.ui.components.carouselDepthEnabled
import com.luc4n3x.levyra.ui.components.levyraCarouselDepth
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import android.app.Activity
import android.content.ClipData
import android.media.AudioManager
import android.content.Context
import android.content.Intent
import com.luc4n3x.levyra.update.AppUpdateContract
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.provider.DocumentsContract
import android.widget.Toast
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.luc4n3x.levyra.BuildConfig
import com.luc4n3x.levyra.R
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.animation.core.Easing
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material.icons.rounded.Dashboard
import com.luc4n3x.levyra.domain.HomeDensity
import com.luc4n3x.levyra.domain.HomeMotion
import com.luc4n3x.levyra.domain.HomeSectionId
import com.luc4n3x.levyra.domain.HomeSectionLayout
import com.luc4n3x.levyra.domain.HomeStyle
import com.luc4n3x.levyra.domain.LevyraHomeStudioSettings
import com.luc4n3x.levyra.domain.MiniPlayerStyle
import com.luc4n3x.levyra.domain.NavColorSource
import com.luc4n3x.levyra.domain.NavStyle
import com.luc4n3x.levyra.domain.LevyraSkin
import com.luc4n3x.levyra.ui.theme.LevyraIsNocturne
import com.luc4n3x.levyra.ui.theme.Nocturne78Palette
import com.luc4n3x.levyra.ui.theme.Nocturne78Design
import com.luc4n3x.levyra.ui.theme.SkinSelector
import com.luc4n3x.levyra.ui.i18n.NavStudioText
import com.luc4n3x.levyra.ui.i18n.navigationStudioCopy
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Groups
import com.luc4n3x.levyra.ui.i18n.settingsHubCopy
import com.luc4n3x.levyra.ui.i18n.SettingsHubText
import androidx.compose.material.icons.rounded.Widgets
import com.luc4n3x.levyra.ui.i18n.WidgetStudioText
import com.luc4n3x.levyra.ui.i18n.widgetStudioCopy
import com.luc4n3x.levyra.widget.studio.WidgetStudioActivity
import com.luc4n3x.levyra.ui.i18n.MiniStudioText
import com.luc4n3x.levyra.ui.i18n.miniPlayerStudioCopy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Explicit
import androidx.compose.material.icons.rounded.Explore
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
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
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.PlaylistRemove
import androidx.compose.material3.IconButtonDefaults
import com.luc4n3x.levyra.ui.components.LevyraExpressiveIconButton
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.Source
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.NetworkCell
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.SwipeVertical
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.Recommend
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.automirrored.rounded.Subject
import androidx.compose.material.icons.rounded.ViewCompact
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.ui.semantics.setProgress
import androidx.core.view.WindowCompat
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.Image as CoilImage
import coil3.compose.AsyncImage
import coil3.toBitmap
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.bitmapConfig
import coil3.request.crossfade
import com.luc4n3x.levyra.data.ArtworkPalette
import com.luc4n3x.levyra.data.DownloadFolderAccess
import com.luc4n3x.levyra.data.ArtworkPaletteCache
import com.luc4n3x.levyra.data.ArtworkRequestSource
import com.luc4n3x.levyra.data.areAllFavoriteTracks
import com.luc4n3x.levyra.data.HomeLoadingPolicy
import com.luc4n3x.levyra.data.HomeOfflinePolicy
import com.luc4n3x.levyra.data.HomeEditorialEngine
import com.luc4n3x.levyra.data.HomeSectionLayoutPolicy
import com.luc4n3x.levyra.data.HomeSectionPresentation
import com.luc4n3x.levyra.data.LevyraArtworkCache
import com.luc4n3x.levyra.data.LevyraArtworkStartupMetrics
import com.luc4n3x.levyra.data.AppleArtistArtworkRepository
import com.luc4n3x.levyra.data.SpotifyArtistArtworkRepository
import com.luc4n3x.levyra.data.PlaybackSourceIdentity
import com.luc4n3x.levyra.domain.YoutubeMusicVideoType
import com.luc4n3x.levyra.data.buildPersonalizedHomeAlbumShelf
import com.luc4n3x.levyra.data.filterSearchSongsExcludingTopResult
import com.luc4n3x.levyra.data.findVerifiedTopResultArtist
import com.luc4n3x.levyra.data.deduplicateSearchSongs
import com.luc4n3x.levyra.data.selectSearchTopResultTracks
import com.luc4n3x.levyra.player.LevyraPipBridge
import com.luc4n3x.levyra.player.NativeAudioIntegration
import com.luc4n3x.levyra.player.PlaybackService
import com.luc4n3x.levyra.domain.ADVANCED_QUEUE_PREFETCH_OFF
import com.luc4n3x.levyra.domain.ADVANCED_QUEUE_PREFETCH_OPTIONS
import com.luc4n3x.levyra.domain.AppUpdateInfo
import com.luc4n3x.levyra.domain.ArtistBiography
import com.luc4n3x.levyra.domain.artistBiographyEditorial
import com.luc4n3x.levyra.domain.ArtistProfile
import com.luc4n3x.levyra.domain.ChartsCatalog
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.data.apple.AppleArtistPage
import com.luc4n3x.levyra.data.apple.AppleArtistShelfStyle
import com.luc4n3x.levyra.data.apple.AppleCatalogTracks
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.ArtistRelease
import com.luc4n3x.levyra.domain.DownloadOwnership
import com.luc4n3x.levyra.domain.DownloadedTrack
import com.luc4n3x.levyra.domain.FollowedArtist
import com.luc4n3x.levyra.domain.PlaylistHit
import com.luc4n3x.levyra.domain.SearchFilter
import com.luc4n3x.levyra.domain.SearchAlbumKind
import com.luc4n3x.levyra.domain.SearchResults
import com.luc4n3x.levyra.domain.availableSearchAlbumKinds
import com.luc4n3x.levyra.ui.youtubemusic.entryFor
import com.luc4n3x.levyra.domain.SmartMusicProfile
import com.luc4n3x.levyra.domain.LevyraCanvasQuality
import com.luc4n3x.levyra.domain.LevyraVisualPerformance
import com.luc4n3x.levyra.domain.LevyraCanvasSource
import com.luc4n3x.levyra.domain.LevyraContentLocales
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.LevyraDownloadFolderMode
import com.luc4n3x.levyra.domain.LevyraDownloadPreset
import com.luc4n3x.levyra.domain.LevyraDownloadSettings
import com.luc4n3x.levyra.domain.LevyraSmartOfflineSettings
import com.luc4n3x.levyra.domain.LevyraBackupFrequency
import com.luc4n3x.levyra.domain.LevyraBackupSettings
import com.luc4n3x.levyra.domain.LevyraVaultStatus
import com.luc4n3x.levyra.data.levyraVaultFileName
import com.luc4n3x.levyra.data.VaultPreview
import com.luc4n3x.levyra.domain.LevyraFontPreset
import com.luc4n3x.levyra.domain.OfflineDownloadTask
import com.luc4n3x.levyra.domain.LevyraAudioPresets
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.ListeningPulse
import com.luc4n3x.levyra.ui.pulse.ListeningPulseProCard
import com.luc4n3x.levyra.ui.recap.LevyraListeningRecapOverlay
import com.luc4n3x.levyra.ui.insights.ListeningInsightsScreen
import com.luc4n3x.levyra.ui.rewind.RewindScreen
import com.luc4n3x.levyra.ui.rewind.RewindEntry
import com.luc4n3x.levyra.ui.downloadhealth.DownloadHealthScreen
import com.luc4n3x.levyra.ui.downloadhealth.DownloadHealthEntry
import com.luc4n3x.levyra.ui.experiences.LocalMusicExperiences
import com.luc4n3x.levyra.ui.experiences.MusicExperienceNavigation
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricSection
import com.luc4n3x.levyra.domain.LyricSectionType
import com.luc4n3x.levyra.domain.LyricVocalRole
import com.luc4n3x.levyra.domain.LyricsProviderOrdering
import com.luc4n3x.levyra.domain.LyricsTranslationState
import com.luc4n3x.levyra.domain.VideoQualityTarget
import com.luc4n3x.levyra.domain.PulseArtist
import com.luc4n3x.levyra.domain.ExploreCatalog
import com.luc4n3x.levyra.domain.ExploreZone
import com.luc4n3x.levyra.domain.Mood
import com.luc4n3x.levyra.domain.MoodEngine
import com.luc4n3x.levyra.domain.ReleaseRadarEntry
import com.luc4n3x.levyra.domain.Taste
import com.luc4n3x.levyra.domain.HomeCollectionKind
import com.luc4n3x.levyra.domain.HomeEditorialCollection
import com.luc4n3x.levyra.domain.HomeCollectionSource
import com.luc4n3x.levyra.domain.HomeSpotlightCandidate
import com.luc4n3x.levyra.domain.HomeSpotlightKind
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.domain.YoutubeComment
import com.luc4n3x.levyra.domain.YoutubeCommentsState
import com.luc4n3x.levyra.domain.YoutubeEngagementState
import com.luc4n3x.levyra.LevyraLaunchActions
import com.luc4n3x.levyra.domain.LevyraNetworkSettings
import com.luc4n3x.levyra.domain.LevyraNetworkSettingsError
import com.luc4n3x.levyra.domain.LevyraNetworkTestOutcome
import com.luc4n3x.levyra.feature.recognition.RecognitionState
import com.luc4n3x.levyra.feature.search.buildPersonalizedSearchSnapshot
import com.luc4n3x.levyra.feature.search.buildSearchPlaceholderCycle
import com.luc4n3x.levyra.feature.search.buildSearchTasteHints
import com.luc4n3x.levyra.feature.search.rankPersonalizedSearchArtists
import com.luc4n3x.levyra.ui.jam.LevyraJamOverlay
import com.luc4n3x.levyra.ui.recognition.LevyraRecognitionOverlay
import com.luc4n3x.levyra.ui.settings.NetworkSettingsPanel
import com.luc4n3x.levyra.ui.settings.DeviceIntegrationSettingsPanel
import com.luc4n3x.levyra.ui.settings.SETTINGS_CATEGORY_AUDIO
import com.luc4n3x.levyra.ui.settings.settingsCategoryTitle
import com.luc4n3x.levyra.ui.settings.settingsSearchEntries
import com.luc4n3x.levyra.feature.sharedmedia.BulkLinkCaptureSummary
import com.luc4n3x.levyra.feature.sharedmedia.SharedMediaKind
import com.luc4n3x.levyra.feature.sharedmedia.SharedMediaPreview
import com.luc4n3x.levyra.ui.components.LevyraArtistAvatarSize
import com.luc4n3x.levyra.ui.components.LevyraArtistItemWidth
import com.luc4n3x.levyra.ui.components.LevyraArtistNameSpacing
import com.luc4n3x.levyra.ui.components.LevyraArtistShelfItem
import com.luc4n3x.levyra.ui.components.LevyraArtistShelfSpacing
import com.luc4n3x.levyra.ui.components.LevyraTrackActionSheet
import com.luc4n3x.levyra.ui.components.LevyraConnectedDefaults
import com.luc4n3x.levyra.ui.components.LevyraConnectedPosition
import com.luc4n3x.levyra.ui.components.LevyraConnectedStyle
import com.luc4n3x.levyra.ui.components.LevyraPlayingIndicator
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.LevyraSkeletonBlock
import com.luc4n3x.levyra.ui.components.levyraConnectedSurface
import com.luc4n3x.levyra.domain.LevyraMixKind
import com.luc4n3x.levyra.ui.components.rememberNowPlayingAccent
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.components.levyraShimmer
import com.luc4n3x.levyra.ui.components.levyraArtistAccent
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraBlue
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraOrange
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import com.luc4n3x.levyra.ui.theme.LevyraPanelSoft
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraIsPureBlack
import com.luc4n3x.levyra.ui.theme.LevyraThemeController
import com.luc4n3x.levyra.ui.theme.LevyraHaptics
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraThemeStudioOverlay
import com.luc4n3x.levyra.ui.lyrics.studio.LyricsStudioOverlay
import com.luc4n3x.levyra.ui.theme.levyraFontFamilyFor
import com.luc4n3x.levyra.domain.LevyraLyricsStudioSettings
import com.luc4n3x.levyra.domain.LyricsActiveColorMode
import com.luc4n3x.levyra.domain.LyricsAlignment
import com.luc4n3x.levyra.domain.LyricsLineSpacing
import com.luc4n3x.levyra.domain.LyricsBackgroundMode
import com.luc4n3x.levyra.domain.LyricsFontWeightPreset
import com.luc4n3x.levyra.domain.LyricsGlowEffect
import com.luc4n3x.levyra.ui.theme.LevyraThemes
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import com.luc4n3x.levyra.ui.theme.rememberLevyraHaptics
import com.luc4n3x.levyra.ui.theme.LocalLevyraVisualCapabilities
import com.luc4n3x.levyra.ui.theme.rememberLevyraVisualCapabilities
import com.luc4n3x.levyra.ui.i18n.LevyraSpotifySkinStrings
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.luc4n3x.levyra.ui.theme.glassmorphism
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.appleArtistInfoCopy
import com.luc4n3x.levyra.ui.i18n.searchDiscographyCopy
import com.luc4n3x.levyra.ui.i18n.BulkLinkCaptureCopy
import com.luc4n3x.levyra.ui.i18n.bulkLinkCaptureCopy
import com.luc4n3x.levyra.ui.i18n.speedDialCopy
import com.luc4n3x.levyra.ui.i18n.formatLibraryDuration
import com.luc4n3x.levyra.ui.i18n.personalizedSearchCopy
import com.luc4n3x.levyra.ui.i18n.personalizedSearchPromptText
import com.luc4n3x.levyra.ui.i18n.queueSectionCopy
import com.luc4n3x.levyra.feature.radio.isLiveRadio
import com.luc4n3x.levyra.ui.i18n.advancedPrefetchCopy
import com.luc4n3x.levyra.ui.i18n.advancedPrefetchMobileDataCopy
import com.luc4n3x.levyra.ui.i18n.automationCopy
import com.luc4n3x.levyra.ui.i18n.smartOfflineCopy
import com.luc4n3x.levyra.ui.i18n.formatLibraryBytes
import com.luc4n3x.levyra.ui.i18n.localizedAudioPresetLabel
import com.luc4n3x.levyra.ui.ambient.LevyraAmbientOverlay
import com.luc4n3x.levyra.ui.library.AddTracksToPlaylistDialog
import com.luc4n3x.levyra.ui.library.LibrarySelectionAction
import com.luc4n3x.levyra.ui.library.LevyraLibraryScreen
import com.luc4n3x.levyra.ui.library.LevyraPlaylistDetailScreen
import com.luc4n3x.levyra.ui.library.PlaylistStudioScreen
import com.luc4n3x.levyra.ui.mixlab.MixLabScreen
import com.luc4n3x.levyra.ui.components.rememberLastNonNull
import com.luc4n3x.levyra.viewmodel.completedScanSongs
import com.luc4n3x.levyra.viewmodel.ExploreViewModel
import com.luc4n3x.levyra.viewmodel.HomeRenderSnapshot
import com.luc4n3x.levyra.viewmodel.buildHomeRenderSnapshot
import com.luc4n3x.levyra.viewmodel.HomeViewModel
import com.luc4n3x.levyra.viewmodel.LevyraScreenViewModelFactory
import com.luc4n3x.levyra.ui.discovery.discoverySection
import com.luc4n3x.levyra.ui.discovery.DiscoveryHomeHost
import com.luc4n3x.levyra.ui.discovery.LocalHomeDiscovery
import com.luc4n3x.levyra.ui.discovery.DiscoveryHomeHandle
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import com.luc4n3x.levyra.viewmodel.LevyraViewModel
import com.luc4n3x.levyra.viewmodel.youtubePlayableTrack
import com.luc4n3x.levyra.ui.artwork.VideoFrameFit
import com.luc4n3x.levyra.ui.artwork.VideoFrameFitCache
import com.luc4n3x.levyra.ui.artwork.detectVideoFrameFit
import com.luc4n3x.levyra.viewmodel.LibraryViewModel
import com.luc4n3x.levyra.viewmodel.PlayerViewModel
import com.luc4n3x.levyra.viewmodel.SearchViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.luc4n3x.levyra.domain.ExcludedArtist
import com.luc4n3x.levyra.domain.LevyraAmbientMode
import com.luc4n3x.levyra.domain.LevyraAmbientSettings
import com.luc4n3x.levyra.domain.LevyraAutomationSettings
import com.luc4n3x.levyra.domain.LevyraInterfaceSettings
import com.luc4n3x.levyra.ui.player.PlayerDragEvent
import com.luc4n3x.levyra.ui.player.PlayerGestureZone
import com.luc4n3x.levyra.ui.player.PlayerMorphAnchors
import com.luc4n3x.levyra.ui.player.PlayerMorphLanding
import com.luc4n3x.levyra.ui.player.flowLandingShape
import com.luc4n3x.levyra.ui.player.PlayerExpansionExpanded
import com.luc4n3x.levyra.ui.player.PlayerPrewarmExpansion
import com.luc4n3x.levyra.ui.player.PlayerMorphSlot
import com.luc4n3x.levyra.ui.player.PlayerSwipeResult
import com.luc4n3x.levyra.ui.player.PlayerVerticalResult
import com.luc4n3x.levyra.ui.player.morphCornerRadius
import com.luc4n3x.levyra.ui.player.playerBackgroundScale
import com.luc4n3x.levyra.ui.player.playerChromeAlpha
import com.luc4n3x.levyra.ui.player.playerExpansionFromDrag
import com.luc4n3x.levyra.ui.player.playerAxisDragGestures
import com.luc4n3x.levyra.ui.player.playerMorphActive
import com.luc4n3x.levyra.ui.player.playerPredictiveBackExpansion
import com.luc4n3x.levyra.ui.player.playerMorphAnchor
import com.luc4n3x.levyra.ui.player.playerMorphContainer
import com.luc4n3x.levyra.ui.player.playerMorphFraction
import com.luc4n3x.levyra.ui.player.playerSeekDeltaMs
import com.luc4n3x.levyra.ui.player.playerSurfaceAlpha
import com.luc4n3x.levyra.ui.player.playerSwipeContentAlpha
import com.luc4n3x.levyra.ui.player.playerTapSide
import com.luc4n3x.levyra.ui.player.rememberPlayerMorphAnchors
import com.luc4n3x.levyra.ui.player.PlayerSwipeMotion
import com.luc4n3x.levyra.ui.player.rememberPlayerSwipeMotion
import com.luc4n3x.levyra.ui.player.rememberTrackStepDirection
import com.luc4n3x.levyra.ui.player.resolveMiniPlayerDismiss
import com.luc4n3x.levyra.ui.player.resolvePlayerExpansionTarget
import com.luc4n3x.levyra.ui.player.playerContainerBounds
import com.luc4n3x.levyra.ui.player.playerContainerCornerPx
import com.luc4n3x.levyra.ui.player.PlayerContainerShape
import java.io.File
import java.time.format.TextStyle as DayTextStyle
import java.util.Locale
import kotlin.math.roundToInt
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
import com.luc4n3x.levyra.ui.theme.LevyraType

@Composable
internal fun LyricsShareBar(
    selectedCount: Int,
    maxCount: Int,
    canContinue: Boolean,
    accent: Color,
    onCopy: () -> Unit,
    onCancel: () -> Unit,
    onContinue: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    Surface(
        color = Color.Black.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
        shape = RoundedCornerShape(26.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics { liveRegion = LiveRegionMode.Polite }
            ) {
                if (selectedCount == 0) {
                    Text(
                        text = strings.shareLyricsHint,
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "$selectedCount/$maxCount",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = strings.linesLabel,
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (selectedCount > 0) {
                CircleIconButton(
                    icon = Icons.Rounded.ContentCopy,
                    tint = Color.White,
                    background = Color.White.copy(alpha = 0.13f),
                    onClick = onCopy,
                    contentDescription = strings.copyVerses
                )
            }
            CircleIconButton(
                icon = Icons.Rounded.Close,
                tint = Color.White,
                background = Color.White.copy(alpha = 0.13f),
                onClick = onCancel,
                contentDescription = strings.cancel
            )
            Button(
                onClick = onContinue,
                enabled = canContinue,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LevyraCyan,
                    contentColor = LevyraOnAccent,
                    disabledContainerColor = Color.White.copy(alpha = 0.10f),
                    disabledContentColor = Color.White.copy(alpha = 0.40f)
                ),
                shape = CircleShape,
                modifier = Modifier.heightIn(min = 46.dp)
            ) {
                Text(strings.shareLyricsContinue, fontWeight = FontWeight.Black, maxLines = 1)
            }
        }
    }
}

@Composable
internal fun LyricsVersionDialog(
    versions: List<com.luc4n3x.levyra.data.LyricsRepository.LyricsVersion>,
    loading: Boolean,
    strings: LevyraStrings,
    onAutomatic: () -> Unit,
    onSelect: (com.luc4n3x.levyra.data.LyricsRepository.LyricsVersion) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF11131C),
            shape = RoundedCornerShape(30.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.82f)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.lyricsVersions, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
                        Text(strings.changeLyrics, color = Color.White.copy(alpha = 0.58f), fontSize = 12.sp)
                    }
                    CircleIconButton(
                        icon = Icons.Rounded.Close,
                        tint = Color.White,
                        background = Color.White.copy(alpha = 0.10f),
                        onClick = onDismiss,
                        contentDescription = strings.close
                    )
                }
                Surface(
                    color = LevyraCyan.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.28f)),
                    modifier = Modifier.fillMaxWidth().pressable(onClick = onAutomatic)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.Refresh, null, tint = LevyraCyan)
                        Text(strings.automaticLyrics, color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
                when {
                    loading && versions.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = LevyraCyan)
                    }
                    versions.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(strings.lyricsUnavailable, color = Color.White.copy(alpha = 0.64f))
                    }
                    else -> LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(versions, key = { it.id }) { version ->
                            val preview = version.result.lines
                                .asSequence()
                                .filterNot { it.isMetadata || it.isInstrumental }
                                .take(2)
                                .joinToString(" · ") { it.text }
                            Surface(
                                color = if (version.selected) LevyraViolet.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (version.selected) LevyraViolet.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.07f)
                                ),
                                modifier = Modifier.fillMaxWidth().pressable { onSelect(version) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            version.result.provider,
                                            color = if (version.selected) LevyraViolet else Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            buildString {
                                                append(if (version.result.synced) strings.synced else strings.lyrics)
                                                append(" · ").append(version.result.confidence).append('%')
                                            },
                                            color = Color.White.copy(alpha = 0.56f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        "${version.title} — ${version.artist}",
                                        color = Color.White.copy(alpha = 0.76f),
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (preview.isNotBlank()) {
                                        Text(
                                            preview,
                                            color = Color.White.copy(alpha = 0.48f),
                                            fontSize = 11.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
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
internal fun LyricsIntelligenceDialog(
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
