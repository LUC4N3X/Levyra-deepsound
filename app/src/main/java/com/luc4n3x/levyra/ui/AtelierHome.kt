package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import androidx.compose.foundation.layout.widthIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.ui.geometry.center
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.ArtistHit
import com.luc4n3x.levyra.domain.HomeBlock
import com.luc4n3x.levyra.domain.LevyraPersonalOrbit
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.atelier.AtelierAmbientBackdrop
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.AtelierCopy
import com.luc4n3x.levyra.ui.i18n.LevyraAtelierStrings
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.AtelierDesign
import com.luc4n3x.levyra.viewmodel.HomeViewModel
import com.luc4n3x.levyra.viewmodel.LevyraUiState
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.delay

internal val AtelierRowPadding = PaddingValues(horizontal = AtelierDesign.gutter)

private val AtelierGridCellMin = 156.dp
private val AtelierLeadSleeve = 196.dp
internal val AtelierSmallSleeve = 104.dp
private val AtelierContinueTile = 148.dp
private val AtelierKeyShape = RoundedCornerShape(11.dp)
internal val AtelierArtistSizes = listOf(120.dp, 84.dp, 104.dp, 72.dp, 112.dp, 92.dp)
internal val AtelierArtistMax = 120.dp
internal const val AtelierCollapsedPicks = 5
internal const val AtelierPickPairCount = 2
internal const val AtelierCollapsedCharts = 10
private const val AtelierWallColumns = 5
private const val AtelierWallMax = 15
private const val AtelierFeatureMax = 5
private const val AtelierFanMax = 5

internal fun atelierAlbumKey(album: AlbumHit): String =
    album.browseId.ifBlank { "${album.title}|${album.artist}" }

internal fun Modifier.atelierBottomRule(): Modifier = drawBehind {
    val inset = AtelierDesign.gutter.toPx()
    val thickness = 1.dp.toPx()
    drawRect(
        color = AtelierDesign.hairline,
        topLeft = Offset(inset, size.height - thickness),
        size = Size(size.width - inset * 2, thickness)
    )
}

private fun Modifier.atelierShelfEdge(overhang: Dp): Modifier = drawBehind {
    val extra = overhang.toPx()
    val start = if (layoutDirection == LayoutDirection.Rtl) -extra else 0f
    drawRect(
        color = AtelierDesign.hairlineStrong,
        topLeft = Offset(start, size.height),
        size = Size(size.width + extra, 1.dp.toPx())
    )
}

private fun atelierArtistKey(track: Track): String = track.artist.lowercase(Locale.ROOT)
    .split(',', '&', '·', '/').first()
    .substringBefore(" feat").substringBefore(" ft.").substringBefore(" x ")
    .trim()

private fun atelierDuration(ms: Long): String {
    if (ms <= 0L) return ""
    val total = ms / 1000L
    val hours = total / 3600L
    val minutes = total % 3600L / 60L
    val seconds = (total % 60L).toString().padStart(2, '0')
    return if (hours > 0L) "$hours:${minutes.toString().padStart(2, '0')}:$seconds" else "$minutes:$seconds"
}

internal fun atelierCoverUrl(track: Track): String = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }

@Composable
internal fun atelierUppercase(text: String): String {
    val code = LocalLevyraStrings.current.code
    return remember(text, code) { text.uppercase(Locale.forLanguageTag(code)) }
}

@Composable
internal fun AtelierSectionLabel(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable RowScope.() -> Unit = {}
) {
    Column(modifier.fillMaxWidth().padding(top = AtelierDesign.sectionGap)) {
        Box(
            Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter).height(1.dp)
                .background(AtelierDesign.hairline)
        )
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = AtelierDesign.gutter, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = atelierUppercase(title),
                style = MaterialTheme.typography.labelMedium,
                color = AtelierDesign.accent,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).clearAndSetSemantics {
                    heading()
                    contentDescription = title
                }
            )
            action()
        }
    }
}

@Composable
private fun AtelierMagazineHeading(title: String, centered: Boolean) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter)
            .padding(top = AtelierDesign.sectionGap + 12.dp, bottom = 18.dp),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Box(Modifier.width(28.dp).height(2.dp).background(AtelierDesign.accent))
        Spacer(Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Light),
            color = AtelierDesign.text,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() }
        )
    }
}

@Composable
internal fun AtelierTextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, onClickLabel: String? = null) {
    Box(
        modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .levyraPressable(onClick = onClick, role = Role.Button, onClickLabel = onClickLabel, pressedScale = LevyraPressScale.Control)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = AtelierDesign.accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun AtelierIconAction(icon: ImageVector, label: String, onClick: () -> Unit, tint: Color = AtelierDesign.textSecondary) {
    Box(
        Modifier.size(48.dp).levyraPressable(onClick = onClick, role = Role.Button, pressedScale = LevyraPressScale.Control),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
internal fun AtelierHardwareKey(
    label: String,
    lit: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    busy: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    content: @Composable RowScope.() -> Unit
) {
    val animated = LocalAnimationsEnabled.current
    val playing = LocalLevyraStrings.current.playing
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val travel by animateFloatAsState(
        if (pressed && animated) 1f else 0f,
        spring(dampingRatio = 0.6f, stiffness = 900f),
        label = "atelier-hardware-key"
    )
    val led by animateFloatAsState(if (lit) 1f else 0f, if (animated) tween(260) else snap(), label = "atelier-hardware-led")
    Row(
        modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .graphicsLayer { translationY = travel * 1.5.dp.toPx() }
            .clip(AtelierKeyShape)
            .drawBehind {
                val top = lerp(Color(0xFF2C2C31), Color(0xFF1E1E22), travel)
                val bottom = lerp(Color(0xFF141417), Color(0xFF0E0E10), travel)
                drawRect(Brush.verticalGradient(listOf(top, bottom)))
                drawRect(Color.White.copy(alpha = 0.16f - travel * 0.1f), size = Size(size.width, 1.dp.toPx()))
                drawRect(Color.Black.copy(alpha = 0.5f), Offset(0f, size.height - 2.dp.toPx()), Size(size.width, 2.dp.toPx()))
                if (led > 0.01f) {
                    val width = 16.dp.toPx()
                    drawRoundRect(
                        AtelierDesign.accentBright.copy(alpha = led),
                        Offset((size.width - width) / 2f, 5.dp.toPx()),
                        Size(width, 2.dp.toPx()),
                        CornerRadius(1.dp.toPx())
                    )
                }
            }
            .clickable(interaction, indication = null, enabled = !busy, role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = label
                if (lit) stateDescription = playing
            }
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(20.dp), color = AtelierDesign.accentBright, strokeWidth = 2.dp)
        } else {
            content()
        }
    }
}

@Composable
internal fun AtelierPlayGlyph(playing: Boolean, size: Dp = 26.dp) {
    Icon(
        if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
        null,
        tint = if (playing) AtelierDesign.accentBright else AtelierDesign.text,
        modifier = Modifier.size(size)
    )
}

@Composable
internal fun AtelierSleeveFrame(modifier: Modifier, content: @Composable () -> Unit) {
    Box(
        modifier.clip(AtelierDesign.sleeveShape).background(AtelierDesign.panel)
            .border(1.dp, AtelierDesign.hairline, AtelierDesign.sleeveShape)
    ) { content() }
}

@Composable
internal fun AtelierArtwork(url: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (url.isNotBlank()) {
            AsyncImage(url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Rounded.MusicNote, null, tint = AtelierDesign.textFaint, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
internal fun AtelierPerspectiveSleeve(
    track: Track,
    side: Dp,
    turn: Float,
    elevation: Dp,
    highRes: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier.size(side)
            .graphicsLayer {
                rotationY = turn
                cameraDistance = 40f * density
                shadowElevation = elevation.toPx()
                shape = AtelierDesign.sleeveShape
                clip = true
                ambientShadowColor = Color.Black
                spotShadowColor = Color.Black
            }
            .background(AtelierDesign.panel)
            .drawWithCache {
                val sheen = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.14f), Color.Transparent, Color.Black.copy(alpha = 0.2f)),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
                val rim = Color.White.copy(alpha = 0.18f)
                onDrawWithContent {
                    drawContent()
                    drawRect(sheen)
                    drawRect(rim, size = Size(size.width, 1.dp.toPx()))
                }
            }
    ) {
        CoverImage(track, Modifier.fillMaxSize().clearAndSetSemantics {}, highRes = highRes)
    }
}

@Composable
internal fun AtelierVinyl(track: Track?, modifier: Modifier) {
    Box(
        modifier.drawWithCache {
            val radius = size.minDimension / 2f
            val body = Brush.radialGradient(
                listOf(Color(0xFF1B181C), Color(0xFF0E0D10), Color(0xFF060506)),
                center = size.center,
                radius = radius
            )
            val sheen = Brush.sweepGradient(
                listOf(
                    Color.Transparent, Color.White.copy(alpha = 0.10f), Color.Transparent, Color.Transparent,
                    Color.White.copy(alpha = 0.07f), Color.Transparent, Color.Transparent
                ),
                center = size.center
            )
            val groove = Stroke(1.dp.toPx())
            val faint = Color.White.copy(alpha = 0.035f)
            val band = Color.White.copy(alpha = 0.075f)
            onDrawBehind {
                drawCircle(body, radius)
                for (index in 0 until 16) {
                    drawCircle(if (index % 4 == 0) band else faint, radius * (0.96f - index * 0.036f), style = groove)
                }
                drawCircle(sheen, radius * 0.97f)
            }
        },
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.fillMaxSize(0.36f).clip(CircleShape).background(AtelierDesign.accent)) {
            if (track != null) CoverImage(track, Modifier.fillMaxSize().clearAndSetSemantics {})
        }
        Box(Modifier.size(6.dp).clip(CircleShape).background(AtelierDesign.room))
    }
}

@Composable
internal fun AtelierTileText(title: String, subtitle: String, highlighted: Boolean) {
    Spacer(Modifier.height(8.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = if (highlighted) AtelierDesign.accentBright else AtelierDesign.text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    if (subtitle.isNotBlank()) {
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = AtelierDesign.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun AtelierTrackTile(
    track: Track,
    width: Dp,
    isCurrent: Boolean,
    actionsLabel: String,
    onPlay: () -> Unit,
    onActions: () -> Unit
) {
    Column(
        Modifier.width(width).levyraPressable(
            onClick = onPlay,
            role = Role.Button,
            pressedScale = LevyraPressScale.Tile,
            onLongClick = onActions,
            onLongClickLabel = actionsLabel
        )
    ) {
        AtelierSleeveFrame(Modifier.size(width)) {
            CoverImage(track, Modifier.fillMaxSize().clearAndSetSemantics {})
        }
        AtelierTileText(track.title, track.artist, isCurrent)
    }
}

@Composable
internal fun AtelierAlbumTile(album: AlbumHit, modifier: Modifier, onOpen: () -> Unit) {
    Column(modifier.levyraPressable(onClick = onOpen, role = Role.Button, pressedScale = LevyraPressScale.Tile)) {
        AtelierSleeveFrame(Modifier.fillMaxWidth().aspectRatio(1f)) { AtelierArtwork(album.thumbnailUrl) }
        AtelierTileText(album.title, album.artist, highlighted = false)
    }
}

@Composable
internal fun AtelierTrackLine(
    track: Track,
    rank: Int?,
    isCurrent: Boolean,
    actionIcon: ImageVector,
    actionLabel: String,
    onPlay: () -> Unit,
    onAction: () -> Unit
) {
    val playing = LocalLevyraStrings.current.playing
    Row(
        Modifier.fillMaxWidth().heightIn(min = AtelierDesign.rowHeight).atelierBottomRule()
            .levyraPressable(onClick = onPlay, role = Role.Button, pressedScale = LevyraPressScale.Row)
            .semantics { if (isCurrent) stateDescription = playing }
            .padding(start = AtelierDesign.gutter, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (rank != null) {
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Light),
                color = if (isCurrent) AtelierDesign.accentBright else AtelierDesign.textSecondary,
                maxLines = 1,
                modifier = Modifier.defaultMinSize(minWidth = 44.dp)
            )
        }
        AtelierSleeveFrame(Modifier.size(AtelierDesign.rowArtwork)) {
            CoverImage(track, Modifier.fillMaxSize().clearAndSetSemantics {})
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isCurrent) AtelierDesign.accentBright else AtelierDesign.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = AtelierDesign.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        AtelierIconAction(actionIcon, actionLabel, onAction)
    }
}

internal fun LazyListScope.atelierTrackRows(
    key: String,
    tracks: List<Track>,
    visible: Int,
    ranked: Boolean,
    line: @Composable (Track, Int?) -> Unit
) {
    itemsIndexed(
        items = tracks.take(visible),
        key = { _, track -> "$key-${track.id}" },
        contentType = { _, _ -> "atelier-track-line" }
    ) { index, track -> line(track, if (ranked) index + 1 else null) }
}

internal fun LazyListScope.atelierExpandToggle(
    key: String,
    total: Int,
    collapsed: Int,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    if (total <= collapsed) return
    item(key = "$key-toggle", contentType = "atelier-toggle") {
        val strings = LocalLevyraStrings.current
        Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
            AtelierTextAction(if (expanded) strings.showLess else strings.showMore, onToggle)
        }
    }
}

@Composable
internal fun AtelierShelfSkeleton() {
    Row(
        Modifier.fillMaxWidth().padding(top = AtelierDesign.sectionGap).padding(AtelierRowPadding),
        horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap)
    ) {
        repeat(3) {
            Box(
                Modifier.size(AtelierDesign.albumCard).clip(AtelierDesign.sleeveShape).background(AtelierDesign.panel)
            )
        }
    }
}

@Composable
private fun AtelierStack(layers: Int, size: Dp, layer: @Composable (Int, Modifier) -> Unit) {
    val step = 12.dp
    Box(Modifier.size(width = size + step * (layers - 1), height = size)) {
        for (index in layers - 1 downTo 0) {
            layer(index, Modifier.offset(x = step * index).size(size).graphicsLayer { alpha = 1f - index * 0.3f })
        }
    }
}

@Composable
private fun AtelierHomeMasthead(name: String, onOpenSettings: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val title = name.ifBlank { strings.home }
    Row(
        Modifier.fillMaxWidth().padding(start = AtelierDesign.gutter, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = atelierUppercase(title),
            style = MaterialTheme.typography.labelMedium,
            color = AtelierDesign.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).clearAndSetSemantics {
                heading()
                contentDescription = title
            }
        )
        AtelierIconAction(Icons.Rounded.Settings, strings.settings, onOpenSettings, AtelierDesign.text)
    }
}

@Composable
private fun AtelierComposedPageTitle(statusTop: Dp, onOpenSettings: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Row(
        Modifier.fillMaxWidth().padding(top = statusTop + 8.dp).padding(start = AtelierDesign.gutter, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Box(Modifier.width(28.dp).height(2.dp).background(AtelierDesign.accent))
            Spacer(Modifier.height(14.dp))
            Text(
                text = strings.home,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Light),
                color = AtelierDesign.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
        }
        AtelierIconAction(Icons.Rounded.Settings, strings.settings, onOpenSettings, AtelierDesign.text)
    }
}

@Composable
private fun AtelierHomeHero(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isResolving: Boolean,
    statusTop: Dp,
    showMasthead: Boolean,
    listenerName: String,
    onOpenSettings: () -> Unit,
    onOpen: () -> Unit,
    onPlay: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraAtelierStrings.forCode(strings.code) }
    val animated = LocalAnimationsEnabled.current
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    val hasAlbum = track.album.isNotBlank()
    val cover = remember(track.largeThumbnailUrl, track.thumbnailUrl) { atelierCoverUrl(track) }
    var revealed by rememberSaveable { mutableStateOf(false) }
    val slide = remember { Animatable(if (revealed || !animated) 1f else 0f) }
    LaunchedEffect(animated) {
        if (animated && slide.value < 1f) {
            delay(240)
            slide.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
        } else {
            slide.snapTo(1f)
        }
        revealed = true
    }
    val active = isCurrent && isPlaying
    Box(Modifier.fillMaxWidth().clipToBounds()) {
        AtelierAmbientBackdrop(cover, Modifier.matchParentSize(), strength = 1.15f)
        Box(
            Modifier.matchParentSize().drawWithCache {
                val fade = Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.35f),
                    0.16f to Color.Transparent,
                    0.62f to Color.Transparent,
                    1f to AtelierDesign.room
                )
                onDrawBehind { drawRect(fade) }
            }
        )
        Column(Modifier.fillMaxWidth().padding(top = statusTop)) {
            if (showMasthead) AtelierHomeMasthead(listenerName, onOpenSettings)
            AtelierHomeHeroLabel(isCurrent, copy)
            AtelierHomeHeroSleeve(track, slide, direction, hasAlbum, onOpen)
            AtelierHomeHeroDetails(track, active, isCurrent, isResolving, hasAlbum, onOpen, onPlay)
        }
    }
}

@Composable
private fun AtelierHomeHeroLabel(isCurrent: Boolean, copy: AtelierCopy) {
    val strings = LocalLevyraStrings.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = atelierUppercase(if (isCurrent) strings.currentlyPlaying else strings.continueListening),
            style = MaterialTheme.typography.labelMedium,
            color = AtelierDesign.accentBright,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = copy.sideA,
            style = MaterialTheme.typography.labelSmall,
            color = AtelierDesign.textSecondary,
            maxLines = 1,
            modifier = Modifier.padding(start = 12.dp).clearAndSetSemantics {}
        )
    }
}

@Composable
private fun AtelierHomeHeroSleeve(
    track: Track,
    slide: Animatable<Float, AnimationVector1D>,
    direction: Float,
    hasAlbum: Boolean,
    onOpen: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 22.dp)) {
        val sleeve = (maxWidth * 0.78f).coerceAtMost(400.dp)
        val travel = with(LocalDensity.current) { (sleeve * 0.2f).toPx() }
        Box(Modifier.fillMaxWidth().height(sleeve).padding(start = AtelierDesign.gutter)) {
            AtelierVinyl(
                track,
                Modifier.align(Alignment.CenterStart).offset(x = sleeve * 0.03f).size(sleeve * 0.94f)
                    .graphicsLayer {
                        translationX = slide.value * travel * direction
                        rotationZ = slide.value * 64f * direction
                    }
            )
            AtelierPerspectiveSleeve(
                track = track,
                side = sleeve,
                turn = -9f * direction,
                elevation = 40.dp,
                highRes = true,
                modifier = Modifier.align(Alignment.CenterStart).levyraPressable(
                    onClick = onOpen,
                    role = Role.Button,
                    onClickLabel = if (hasAlbum) strings.openAlbum else strings.play,
                    pressedScale = LevyraPressScale.Surface
                )
            )
        }
    }
}

@Composable
private fun AtelierHomeHeroDetails(
    track: Track,
    active: Boolean,
    isCurrent: Boolean,
    isResolving: Boolean,
    hasAlbum: Boolean,
    onOpen: () -> Unit,
    onPlay: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    Column(Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter).padding(top = 26.dp)) {
        Text(
            text = track.album.ifBlank { track.title },
            style = MaterialTheme.typography.displayMedium,
            color = AtelierDesign.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
            color = AtelierDesign.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
        Row(Modifier.padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            AtelierHardwareKey(
                label = if (active) strings.pause else strings.play,
                lit = active,
                busy = isCurrent && isResolving,
                onClick = onPlay,
                modifier = Modifier.width(92.dp).height(58.dp)
            ) { AtelierPlayGlyph(active, 28.dp) }
            if (hasAlbum) {
                Spacer(Modifier.width(8.dp))
                AtelierTextAction(strings.openAlbum, onOpen)
            }
        }
    }
}

@Immutable
private class AtelierFeature(
    val key: String,
    val overline: String,
    val title: String,
    val subtitle: String,
    val lead: Track,
    val tracks: List<Track>,
    val external: (() -> Unit)?
)

private fun atelierFeatures(
    soundtrack: StudioHero?,
    orbit: List<Track>,
    orbitTitle: String,
    picks: List<Track>,
    picksTitle: String,
    collections: List<AppleMusicHomeCollection>,
    collectionsTitle: String,
    copy: AtelierCopy
): List<AtelierFeature> {
    val used = HashSet<String>()
    val cards = ArrayList<AtelierFeature>(AtelierFeatureMax)
    fun leadOf(tracks: List<Track>): Track? = tracks.firstOrNull { track ->
        val artist = atelierArtistKey(track)
        artist.isNotEmpty() && artist !in used
    }
    soundtrack?.let { hero ->
        used += atelierArtistKey(hero.track)
        cards += AtelierFeature("feature-soundtrack", hero.label, hero.lead, hero.track.title, hero.track, listOf(hero.track), hero.onOpen)
    }
    leadOf(orbit)?.let { lead ->
        used += atelierArtistKey(lead)
        cards += AtelierFeature("feature-orbit", orbitTitle, lead.artist, copy.tracksCount.format(orbit.size), lead, orbit, null)
    }
    leadOf(picks)?.let { lead ->
        used += atelierArtistKey(lead)
        cards += AtelierFeature("feature-picks", picksTitle, lead.title, lead.artist, lead, picks, null)
    }
    for (collection in collections) {
        if (cards.size >= AtelierFeatureMax) break
        val tracks = collection.tracks.distinctBy(Track::id)
        val lead = leadOf(tracks) ?: continue
        used += atelierArtistKey(lead)
        cards += AtelierFeature(
            "feature-collection-${collection.id}",
            collectionsTitle,
            collection.title,
            copy.tracksCount.format(tracks.size),
            lead,
            tracks,
            null
        )
    }
    return cards.take(AtelierFeatureMax)
}

@Composable
private fun AtelierFeatureRow(
    features: List<AtelierFeature>,
    cardWidth: Dp,
    currentId: String?,
    isPlaying: Boolean,
    isResolving: Boolean,
    onPlay: (AtelierFeature) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraAtelierStrings.forCode(strings.code) }
    val pager = rememberPagerState { features.size }
    if (features.isEmpty()) return
    val focused = features[pager.currentPage.coerceIn(0, features.lastIndex)]
    val cover = remember(focused.lead.largeThumbnailUrl, focused.lead.thumbnailUrl) { atelierCoverUrl(focused.lead) }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val sleeve = (maxWidth * 0.62f).coerceAtMost(320.dp)
        val sidePad = (maxWidth - sleeve) / 2
        AtelierAmbientBackdrop(cover, Modifier.matchParentSize(), strength = 1.25f)
        Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    atelierUppercase(copy.featureTitle), style = MaterialTheme.typography.labelMedium,
                    color = AtelierDesign.accentBright, modifier = Modifier.weight(1f)
                )
                Text(
                    "${pager.currentPage + 1} / ${features.size}", style = MaterialTheme.typography.labelMedium,
                    color = AtelierDesign.textFaint
                )
            }
            Spacer(Modifier.height(18.dp))
            HorizontalPager(
                state = pager,
                contentPadding = PaddingValues(horizontal = sidePad),
                pageSpacing = 14.dp,
                key = { features[it].key },
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val feature = features[page]
                val current = feature.lead.id == currentId
                Box(
                    Modifier.size(sleeve).graphicsLayer {
                        val offset = (pager.currentPage - page + pager.currentPageOffsetFraction).coerceIn(-1f, 1f)
                        val near = 1f - kotlin.math.abs(offset)
                        val scale = 0.82f + 0.18f * near
                        scaleX = scale
                        scaleY = scale
                        alpha = 0.45f + 0.55f * near
                        rotationY = offset * 14f
                        cameraDistance = 14f * density
                        shadowElevation = 28.dp.toPx() * near
                        shape = AtelierDesign.sleeveShape
                        clip = true
                    }
                        .levyraPressable(
                            onClick = { onPlay(feature) }, role = Role.Button,
                            onClickLabel = if (current && isPlaying) strings.pause else strings.play,
                            pressedScale = LevyraPressScale.Tile
                        )
                ) {
                    CoverImage(feature.lead, Modifier.fillMaxSize())
                    if (current) {
                        Box(
                            Modifier.align(Alignment.BottomEnd).padding(10.dp).size(40.dp).clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (current && isResolving) LevyraLoadingIndicator(color = AtelierDesign.accentBright, modifier = Modifier.size(18.dp))
                            else AtelierPlayGlyph(isPlaying)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            AnimatedContent(
                targetState = focused,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                contentKey = { it.key },
                label = "atelier-feature-copy"
            ) { feature ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        atelierUppercase(feature.overline), style = MaterialTheme.typography.labelSmall,
                        color = AtelierDesign.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        feature.title, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Light),
                        color = AtelierDesign.text, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(14.dp))
                    feature.tracks.take(3).forEachIndexed { index, track ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 36.dp).padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${'A'}${index + 1}", style = MaterialTheme.typography.labelSmall,
                                color = AtelierDesign.accent, modifier = Modifier.width(32.dp)
                            )
                            Text(
                                track.title, style = MaterialTheme.typography.bodyMedium, color = AtelierDesign.text,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                            )
                            Text(
                                track.artist, style = MaterialTheme.typography.bodySmall, color = AtelierDesign.textFaint,
                                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 10.dp).widthIn(max = 120.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AtelierPickSleeve(track: Track, width: Dp, isCurrent: Boolean, actionsLabel: String, modifier: Modifier, onPlay: () -> Unit, onActions: () -> Unit) {
    Column(
        modifier.width(width).levyraPressable(
            onClick = onPlay,
            role = Role.Button,
            pressedScale = LevyraPressScale.Tile,
            onLongClick = onActions,
            onLongClickLabel = actionsLabel
        )
    ) {
        Box(
            Modifier.size(width).graphicsLayer {
                shadowElevation = 18.dp.toPx()
                shape = AtelierDesign.sleeveShape
                clip = true
                ambientShadowColor = Color.Black
                spotShadowColor = Color.Black
            }.background(AtelierDesign.panel)
        ) {
            CoverImage(track, Modifier.fillMaxSize().clearAndSetSemantics {})
        }
        Text(
            text = track.title,
            style = MaterialTheme.typography.titleMedium,
            color = if (isCurrent) AtelierDesign.accentBright else AtelierDesign.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = track.artist,
            style = MaterialTheme.typography.bodySmall,
            color = AtelierDesign.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun AtelierCompactRow(track: Track, isCurrent: Boolean, actionsLabel: String, onPlay: () -> Unit, onActions: () -> Unit) {
    val playing = LocalLevyraStrings.current.playing
    val duration = remember(track.durationMs) { atelierDuration(track.durationMs) }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .levyraPressable(
                onClick = onPlay,
                role = Role.Button,
                pressedScale = LevyraPressScale.Row,
                onLongClick = onActions,
                onLongClickLabel = actionsLabel
            )
            .semantics { if (isCurrent) stateDescription = playing }
            .padding(start = AtelierDesign.gutter, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AtelierSleeveFrame(Modifier.size(40.dp)) {
            CoverImage(track, Modifier.fillMaxSize().clearAndSetSemantics {})
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isCurrent) AtelierDesign.accentBright else AtelierDesign.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = AtelierDesign.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (duration.isNotEmpty()) {
            Text(
                text = duration,
                style = MaterialTheme.typography.labelLarge,
                color = AtelierDesign.textFaint,
                maxLines = 1
            )
        }
        AtelierIconAction(Icons.Rounded.MoreVert, actionsLabel, onActions)
    }
}

@Composable
private fun AtelierWallRow(albums: List<Track>, onOpen: (Track) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter).padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        albums.forEach { track ->
            val description = remember(track.album, track.artist) { "${track.album}, ${track.artist}" }
            AtelierSleeveFrame(
                Modifier.weight(1f).aspectRatio(1f)
                    .levyraPressable(onClick = { onOpen(track) }, role = Role.Button, pressedScale = LevyraPressScale.Tile)
                    .semantics { contentDescription = description }
            ) {
                CoverImage(track, Modifier.fillMaxSize().clearAndSetSemantics {})
            }
        }
        repeat(AtelierWallColumns - albums.size) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun AtelierSleeveFan(sleeves: List<Track>) {
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    Box(Modifier.fillMaxWidth().height(136.dp), contentAlignment = Alignment.BottomCenter) {
        if (sleeves.isEmpty()) {
            AtelierVinyl(null, Modifier.size(118.dp))
        }
        val middle = (sleeves.size - 1) / 2f
        sleeves.forEachIndexed { index, track ->
            val position = index - middle
            Box(
                Modifier.size(100.dp).zIndex(-abs(position))
                    .graphicsLayer {
                        translationX = position * 46.dp.toPx() * direction
                        translationY = abs(position) * 7.dp.toPx()
                        rotationZ = position * 7f * direction
                        transformOrigin = TransformOrigin(0.5f, 1f)
                        shadowElevation = 12.dp.toPx()
                        shape = AtelierDesign.sleeveShape
                        clip = true
                        ambientShadowColor = Color.Black
                        spotShadowColor = Color.Black
                    }
                    .background(AtelierDesign.panel)
            ) {
                CoverImage(track, Modifier.fillMaxSize().clearAndSetSemantics {})
            }
        }
    }
}

@Composable
private fun AtelierShelfTeaser(sleeves: List<Track>, onOpen: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraAtelierStrings.forCode(strings.code) }
    Column(
        Modifier.fillMaxWidth().padding(top = AtelierDesign.sectionGap).padding(horizontal = AtelierDesign.gutter)
            .levyraPressable(onClick = onOpen, role = Role.Button, pressedScale = LevyraPressScale.Surface)
            .clip(AtelierDesign.panelShape)
            .background(Brush.verticalGradient(listOf(AtelierDesign.panelRaised, AtelierDesign.cabinet)))
            .border(1.dp, AtelierDesign.hairline, AtelierDesign.panelShape)
            .padding(top = 22.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AtelierSleeveFan(sleeves)
        Box(
            Modifier.fillMaxWidth(0.82f).height(3.dp).clip(RoundedCornerShape(2.dp))
                .background(Brush.horizontalGradient(listOf(Color.Transparent, AtelierDesign.accent.copy(alpha = 0.55f), Color.Transparent)))
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = atelierUppercase(copy.shelf),
            style = MaterialTheme.typography.labelMedium,
            color = AtelierDesign.accent,
            maxLines = 1,
            modifier = Modifier.clearAndSetSemantics {}
        )
        Row(
            Modifier.padding(horizontal = 16.dp).padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = copy.goToShelf,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Light),
                color = AtelierDesign.text,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                null,
                tint = AtelierDesign.accent,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun AtelierContinueAlbumTile(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    actionsLabel: String,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onActions: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val active = isCurrent && isPlaying
    Box(Modifier.width(AtelierContinueTile)) {
        Column(
            Modifier.fillMaxWidth().levyraPressable(
                onClick = onOpen,
                role = Role.Button,
                onClickLabel = if (track.album.isNotBlank()) strings.openAlbum else strings.play,
                pressedScale = LevyraPressScale.Tile,
                onLongClick = onActions,
                onLongClickLabel = actionsLabel
            )
        ) {
            AtelierSleeveFrame(Modifier.size(AtelierContinueTile)) {
                CoverImage(track, Modifier.fillMaxSize().clearAndSetSemantics {})
            }
            AtelierTileText(track.album.ifBlank { track.title }, track.artist, isCurrent)
        }
        AtelierHardwareKey(
            label = if (active) strings.pause else strings.play,
            lit = active,
            onClick = onPlay,
            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(48.dp),
            contentPadding = PaddingValues(0.dp)
        ) { AtelierPlayGlyph(active, 22.dp) }
    }
}

@Composable
internal fun AtelierStandingShelf(
    tracks: List<Track>,
    currentId: String?,
    actionsLabel: String,
    onPlay: (Track) -> Unit,
    onActions: (Track) -> Unit
) {
    LazyRow(contentPadding = AtelierRowPadding, horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap)) {
        itemsIndexed(tracks, key = { _, track -> track.id }, contentType = { _, _ -> "atelier-standing-sleeve" }) { index, track ->
            val width = if (index == 0) AtelierLeadSleeve else AtelierDesign.albumCard
            Column(
                Modifier.width(width).levyraPressable(
                    onClick = { onPlay(track) },
                    role = Role.Button,
                    pressedScale = LevyraPressScale.Tile,
                    onLongClick = { onActions(track) },
                    onLongClickLabel = actionsLabel
                )
            ) {
                Box(
                    Modifier.fillMaxWidth().height(AtelierLeadSleeve).atelierShelfEdge(AtelierDesign.itemGap),
                    contentAlignment = Alignment.BottomStart
                ) {
                    AtelierSleeveFrame(Modifier.size(width)) {
                        CoverImage(track, Modifier.fillMaxSize().clearAndSetSemantics {})
                    }
                }
                AtelierTileText(track.title, track.artist, track.id == currentId)
            }
        }
    }
}

@Composable
internal fun AtelierTrackStrip(
    tracks: List<Track>,
    width: Dp,
    currentId: String?,
    actionsLabel: String,
    onPlay: (Track) -> Unit,
    onActions: (Track) -> Unit
) {
    LazyRow(contentPadding = AtelierRowPadding, horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap)) {
        items(tracks, key = { it.id }, contentType = { "atelier-track-tile" }) { track ->
            AtelierTrackTile(track, width, track.id == currentId, actionsLabel, { onPlay(track) }, { onActions(track) })
        }
    }
}

@Composable
private fun AtelierArtistTile(artist: ArtistHit, portrait: Dp, onOpen: () -> Unit) {
    val strings = LocalLevyraStrings.current
    Column(
        Modifier.width(maxOf(portrait, 88.dp)).levyraPressable(
            onClick = onOpen,
            role = Role.Button,
            onClickLabel = strings.openArtist,
            pressedScale = LevyraPressScale.Tile
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height((AtelierArtistMax - portrait) / 2))
        Box(
            Modifier.size(portrait).clip(CircleShape).background(AtelierDesign.panel)
                .border(1.dp, AtelierDesign.hairlineStrong, CircleShape)
        ) { AtelierArtwork(artist.thumbnailUrl) }
        Spacer(Modifier.height(8.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.titleSmall,
            color = AtelierDesign.text,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AtelierCollectionTile(collection: AppleMusicHomeCollection, onOpen: () -> Unit, onPlay: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraAtelierStrings.forCode(strings.code) }
    val covers = remember(collection.tracks) { collection.tracks.distinctBy(Track::id).take(3) }
    Column(
        Modifier.width(AtelierDesign.albumCard + 24.dp).levyraPressable(
            onClick = onOpen,
            role = Role.Button,
            onClickLabel = copy.openDetails,
            pressedScale = LevyraPressScale.Tile,
            onLongClick = onPlay,
            onLongClickLabel = copy.playCollection
        )
    ) {
        AtelierStack(covers.size.coerceAtLeast(1), AtelierDesign.albumCard) { index, modifier ->
            AtelierSleeveFrame(modifier) {
                covers.getOrNull(index)?.let { track ->
                    CoverImage(track, Modifier.fillMaxSize().clearAndSetSemantics {})
                } ?: AtelierArtwork("")
            }
        }
        AtelierTileText(collection.title, copy.tracksCount.format(collection.tracks.size), highlighted = false)
    }
}

@Composable
private fun AtelierHomeEmpty(message: String, onRetry: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraAtelierStrings.forCode(strings.code) }
    Column(Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter).padding(top = AtelierDesign.sectionGap)) {
        Text(
            text = message,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Light),
            color = AtelierDesign.text
        )
        Spacer(Modifier.height(8.dp))
        AtelierTextAction(copy.retry, onRetry)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AtelierHomeEntry(
    data: AppleMusicHomeData,
    homeActions: AppleMusicHomeActions,
    viewModel: HomeViewModel,
    state: LevyraUiState,
    homeListState: LazyListState,
    homeBottomInset: Dp,
    homeRefreshing: Boolean,
    homePullState: PullToRefreshState,
    onPullRefresh: () -> Unit,
    composition: ComposedHomeRequest? = null
) {
    val strings = LocalLevyraStrings.current
    val render by viewModel.renderState.collectAsStateWithLifecycle()
    val releaseSection = render.derived.newReleases
    val showReleases = state.interfaceSettings.showNewReleases
    val needsReleases = composition == null && showReleases && releaseSection == null && state.exploreNewReleases.isEmpty()
    LaunchedEffect(needsReleases, strings.code) { if (needsReleases) viewModel.ensureExplore(strings) }
    AtelierHomeContent(
        data = data,
        actions = homeActions,
        viewModel = viewModel,
        state = state,
        releaseTitle = releaseSection?.title?.ifBlank { null } ?: strings.newReleases,
        releaseTracks = if (showReleases) releaseSection?.tracks.orEmpty() else emptyList(),
        releaseAlbums = if (showReleases) state.exploreNewReleases else emptyList(),
        homeListState = homeListState,
        homeBottomInset = homeBottomInset,
        homeRefreshing = homeRefreshing,
        homePullState = homePullState,
        onPullRefresh = onPullRefresh,
        composition = composition
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AtelierHomeContent(
    data: AppleMusicHomeData,
    actions: AppleMusicHomeActions,
    viewModel: HomeViewModel,
    state: LevyraUiState,
    releaseTitle: String,
    releaseTracks: List<Track>,
    releaseAlbums: List<AlbumHit>,
    homeListState: LazyListState,
    homeBottomInset: Dp,
    homeRefreshing: Boolean,
    homePullState: PullToRefreshState,
    onPullRefresh: () -> Unit,
    composition: ComposedHomeRequest? = null
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraAtelierStrings.forCode(strings.code) }
    val currentId = data.currentId
    val recordings = remember(state.recentListens) { LevyraPersonalOrbit.distinctRecordings(state.recentListens) }
    val recents = remember(recordings) { recordings.distinctBy(Track::id).take(20) }
    val recentAlbums = remember(recordings) { atelierRecentAlbums(recordings) }
    val heroTrack = atelierHomeHeroTrack(state, recents, data)
    val heroId = heroTrack?.id
    val shelfRecents = remember(recents, heroId) { recents.filterNot { it.id == heroId } }
    val orbit = remember(data.personalTracks, data.showPersonal) { atelierHomeOrbit(data) }
    val picks = remember(data.quickPicks) { data.quickPicks?.tracks.orEmpty().distinctBy(Track::id) }
    val picksTitle = atelierHomePicksTitle(data, strings)
    val albums = remember(data.albums, data.showAlbums) { atelierHomeAlbums(data) }
    val newTracks = remember(releaseTracks) { releaseTracks.distinctBy(Track::id).take(20) }
    val newAlbums = remember(releaseAlbums) { releaseAlbums.distinctBy(::atelierAlbumKey).take(20) }
    val artists = remember(data.artists, data.showArtists) { atelierHomeArtists(data) }
    val charts = remember(data.charts) { data.charts.distinctBy(Track::id).take(50) }
    val collections = remember(data.collections) { data.collections.distinctBy { it.id } }
    val forgotten = remember(data.forgottenFavorites) { data.forgottenFavorites.distinctBy(Track::id).take(20) }
    val fanSleeves = remember(orbit, recents) { (orbit + recents).distinctBy(Track::id).take(AtelierFanMax) }
    val features = remember(data.soundtrack, orbit, picks, picksTitle, collections, strings.code) {
        atelierFeatures(
            soundtrack = data.soundtrack,
            orbit = orbit,
            orbitTitle = strings.personalOrbitTitle,
            picks = picks,
            picksTitle = picksTitle,
            collections = collections,
            collectionsTitle = strings.collectionsTitle,
            copy = copy
        )
    }
    val nothingToShow = atelierHomeNothingToShow(
        heroTrack, data, orbit, picks, albums, newTracks, newAlbums, artists, charts, collections, forgotten
    )
    val stillLoading = state.isLoadingHome || state.isLoadingCharts || state.homeAlbumsLoading || state.homeArtistsLoading
    var picksExpanded by rememberSaveable { mutableStateOf(false) }
    var chartsExpanded by rememberSaveable { mutableStateOf(false) }
    val trackOptions = strings.songOptions
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val heroPlay: () -> Unit = { atelierHomeHeroPlay(heroTrack, state, recents, data, viewModel, actions) }
    val playFeature: (AtelierFeature) -> Unit = { feature -> atelierHomePlayFeature(feature, state, viewModel, actions) }
    val playRecent: (Track) -> Unit = { track -> atelierHomePlayRecent(track, recents, state, viewModel, actions) }
    val trackLine: @Composable (List<Track>, Track, Int?) -> Unit = { list, track, rank ->
        AtelierTrackLine(
            track = track,
            rank = rank,
            isCurrent = track.id == currentId,
            actionIcon = Icons.Rounded.MoreVert,
            actionLabel = trackOptions,
            onPlay = { actions.playFrom(list, track) },
            onAction = { actions.onTrackActions(track) }
        )
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(AtelierDesign.room)) {
        val columns = (maxWidth / AtelierGridCellMin).toInt().coerceIn(2, 6)
        val albumRows = remember(albums, columns) { albums.chunked(columns) }
        val wallRows = remember(recentAlbums) { recentAlbums.chunked(AtelierWallColumns) }
        val featureWidth = (maxWidth * 0.82f).coerceAtMost(420.dp)
        val pairWidth = (maxWidth * 0.40f).coerceAtMost(240.dp)
        val composedKit = remember(data, actions, pairWidth) { AtelierComposedKit(data, actions, pairWidth) }
        LazyColumn(
            state = homeListState,
            modifier = Modifier.fillMaxSize()
                .pullToRefresh(isRefreshing = homeRefreshing, state = homePullState, onRefresh = onPullRefresh),
            contentPadding = PaddingValues(bottom = homeBottomInset + AtelierDesign.sectionGap)
        ) {
            if (composition != null) {
                val feedLeadsPage = composition.blocks.firstOrNull() == HomeBlock.SkinFeed && composition.content.offlineMessage == null
                if (!feedLeadsPage) {
                    item(key = "atelier-page-title", contentType = "atelier-page-title") {
                        AtelierComposedPageTitle(statusTop, actions.openSettings)
                    }
                }
                composedHomeSections(composition, composedKit) {
                    atelierHomeLead(heroTrack, state, if (feedLeadsPage) statusTop else 0.dp, data, actions, heroPlay, showMasthead = feedLeadsPage)
                    atelierHomeFeatureRow(features, featureWidth, currentId, state, playFeature)
                    atelierHomeOrbitAndWall(orbit, wallRows, currentId, trackOptions, strings, copy, actions)
                    atelierHomeTeaserAndRecents(fanSleeves, shelfRecents, currentId, trackOptions, state, strings, viewModel, actions, playRecent)
                }
                atelierHomeFallback(nothingToShow && atelierComposedSectionsEmpty(composition.content), stillLoading, state, copy, viewModel)
                return@LazyColumn
            }
            atelierHomeLead(heroTrack, state, statusTop, data, actions, heroPlay)
            atelierHomeFallback(nothingToShow, stillLoading, state, copy, viewModel)
            atelierHomeFeatureRow(features, featureWidth, currentId, state, playFeature)
            atelierHomePicks(picks, picksTitle, pairWidth, currentId, trackOptions, picksExpanded, actions) {
                picksExpanded = !picksExpanded
            }
            atelierHomeOrbitAndWall(orbit, wallRows, currentId, trackOptions, strings, copy, actions)
            atelierHomeTeaserAndRecents(fanSleeves, shelfRecents, currentId, trackOptions, state, strings, viewModel, actions, playRecent)
            atelierHomeAlbumGrid(albumRows, columns, strings, actions)
            atelierHomeReleases(newTracks, newAlbums, releaseTitle, currentId, trackOptions, actions)
            atelierHomeTrendingArtists(artists, strings, actions)
            atelierHomeCharts(charts, chartsExpanded, data, strings, actions, trackLine) {
                chartsExpanded = !chartsExpanded
            }
            atelierHomeCollections(collections, strings, actions)
            atelierHomeForgotten(forgotten, currentId, trackOptions, strings, actions)
        }
        PullToRefreshDefaults.LoadingIndicator(
            state = homePullState,
            isRefreshing = homeRefreshing,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = statusTop + 16.dp)
        )
    }
}

private fun atelierComposedSectionsEmpty(content: ComposedHomeContent): Boolean =
    content.newReleases.isEmpty() && content.videos.isEmpty() && content.moreSections.isEmpty() &&
        content.speedDialPins.isEmpty() && content.releaseRadar.isEmpty() && content.similarArtists.isEmpty() &&
        content.resonanceTracks.isEmpty() && content.discovery == null && !content.radio.visible

private fun atelierRecentAlbums(recordings: List<Track>): List<Track> {
    val albums = recordings.asSequence()
        .filter { it.album.isNotBlank() }
        .distinctBy { "${it.album.trim().lowercase(Locale.ROOT)}|${atelierArtistKey(it)}" }
        .take(AtelierWallMax)
        .toList()
    return if (albums.size < AtelierWallColumns) emptyList() else albums.take(albums.size / AtelierWallColumns * AtelierWallColumns)
}

private fun atelierHomeHeroTrack(state: LevyraUiState, recents: List<Track>, data: AppleMusicHomeData): Track? =
    state.currentTrack ?: recents.firstOrNull() ?: data.soundtrack?.track

private fun atelierHomeOrbit(data: AppleMusicHomeData): List<Track> =
    if (data.showPersonal) data.personalTracks.distinctBy(Track::id).take(20) else emptyList()

private fun atelierHomePicksTitle(data: AppleMusicHomeData, strings: LevyraStrings): String =
    data.quickPicks?.title?.ifBlank { null } ?: strings.quickPicks

private fun atelierHomeAlbums(data: AppleMusicHomeData): List<AlbumHit> =
    if (data.showAlbums) data.albums.distinctBy(::atelierAlbumKey) else emptyList()

private fun atelierHomeArtists(data: AppleMusicHomeData): List<ArtistHit> =
    if (data.showArtists) {
        data.artists.filter { it.name.isNotBlank() }.distinctBy { it.browseId.ifBlank { it.name } }
    } else {
        emptyList()
    }

private fun atelierHomeNothingToShow(
    heroTrack: Track?,
    data: AppleMusicHomeData,
    orbit: List<Track>,
    picks: List<Track>,
    albums: List<AlbumHit>,
    newTracks: List<Track>,
    newAlbums: List<AlbumHit>,
    artists: List<ArtistHit>,
    charts: List<Track>,
    collections: List<AppleMusicHomeCollection>,
    forgotten: List<Track>
): Boolean = heroTrack == null && data.soundtrack == null && orbit.isEmpty() && picks.isEmpty() &&
    albums.isEmpty() && newTracks.isEmpty() && newAlbums.isEmpty() && artists.isEmpty() && charts.isEmpty() &&
    collections.isEmpty() && forgotten.isEmpty()

private fun atelierHomeHeroPlay(
    heroTrack: Track?,
    state: LevyraUiState,
    recents: List<Track>,
    data: AppleMusicHomeData,
    viewModel: HomeViewModel,
    actions: AppleMusicHomeActions
) {
    when {
        heroTrack == null -> Unit
        heroTrack.id == state.currentTrack?.id -> {
            if (!state.isResolving) viewModel.togglePlay()
        }
        recents.any { it.id == heroTrack.id } -> actions.playFrom(recents, heroTrack)
        else -> data.soundtrack?.onOpen?.invoke()
    }
}

private fun atelierHomePlayFeature(
    feature: AtelierFeature,
    state: LevyraUiState,
    viewModel: HomeViewModel,
    actions: AppleMusicHomeActions
) {
    val external = feature.external
    when {
        external != null -> external()
        feature.lead.id == state.currentTrack?.id -> {
            if (!state.isResolving) viewModel.togglePlay()
        }
        else -> actions.playFrom(feature.tracks, feature.lead)
    }
}

private fun atelierHomePlayRecent(
    track: Track,
    recents: List<Track>,
    state: LevyraUiState,
    viewModel: HomeViewModel,
    actions: AppleMusicHomeActions
) {
    when {
        track.id != state.currentTrack?.id -> actions.playFrom(recents, track)
        !state.isResolving -> viewModel.togglePlay()
    }
}

private fun LazyListScope.atelierHomeLead(
    heroTrack: Track?,
    state: LevyraUiState,
    statusTop: Dp,
    data: AppleMusicHomeData,
    actions: AppleMusicHomeActions,
    heroPlay: () -> Unit,
    showMasthead: Boolean = true
) {
    if (heroTrack != null) {
        item(key = "atelier-hero", contentType = "atelier-hero") {
            AtelierHomeHero(
                track = heroTrack,
                isCurrent = heroTrack.id == state.currentTrack?.id,
                isPlaying = state.isPlaying,
                isResolving = state.isResolving,
                statusTop = statusTop,
                showMasthead = showMasthead,
                listenerName = data.listener.name,
                onOpenSettings = actions.openSettings,
                onOpen = {
                    if (heroTrack.album.isNotBlank()) actions.openAlbum(trackAlbumHit(heroTrack)) else heroPlay()
                },
                onPlay = heroPlay
            )
        }
    } else if (showMasthead) {
        item(key = "atelier-masthead", contentType = "atelier-masthead") {
            Box(Modifier.padding(top = statusTop)) {
                AtelierHomeMasthead(data.listener.name, actions.openSettings)
            }
        }
    }
}

private fun LazyListScope.atelierHomeFallback(
    nothingToShow: Boolean,
    stillLoading: Boolean,
    state: LevyraUiState,
    copy: AtelierCopy,
    viewModel: HomeViewModel
) {
    if (nothingToShow) {
        if (stillLoading) {
            item(key = "atelier-skeleton-first", contentType = "atelier-skeleton") { AtelierShelfSkeleton() }
            item(key = "atelier-skeleton-second", contentType = "atelier-skeleton") { AtelierShelfSkeleton() }
        } else {
            item(key = "atelier-empty", contentType = "atelier-empty") {
                AtelierHomeEmpty(state.homeError ?: copy.homeEmptyBody, viewModel::retryHomeContent)
            }
        }
    }
}

private fun LazyListScope.atelierHomeFeatureRow(
    features: List<AtelierFeature>,
    featureWidth: Dp,
    currentId: String?,
    state: LevyraUiState,
    playFeature: (AtelierFeature) -> Unit
) {
    if (features.isNotEmpty()) {
        item(key = "atelier-features", contentType = "atelier-features") {
            AtelierFeatureRow(features, featureWidth, currentId, state.isPlaying, state.isResolving, playFeature)
        }
    }
}

private fun LazyListScope.atelierHomePicks(
    picks: List<Track>,
    picksTitle: String,
    pairWidth: Dp,
    currentId: String?,
    trackOptions: String,
    picksExpanded: Boolean,
    actions: AppleMusicHomeActions,
    onTogglePicks: () -> Unit
) {
    if (picks.isNotEmpty()) {
        item(key = "atelier-picks-label", contentType = "atelier-label") { AtelierSectionLabel(picksTitle) }
        item(key = "atelier-picks-pair", contentType = "atelier-pick-pair") {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter).padding(top = 4.dp, bottom = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                picks.take(AtelierPickPairCount).forEachIndexed { index, track ->
                    AtelierPickSleeve(
                        track = track,
                        width = pairWidth,
                        isCurrent = track.id == currentId,
                        actionsLabel = trackOptions,
                        modifier = if (index == 0) Modifier else Modifier.padding(top = 40.dp),
                        onPlay = { actions.playFrom(picks, track) },
                        onActions = { actions.onTrackActions(track) }
                    )
                }
            }
        }
        val compact = picks.drop(AtelierPickPairCount)
        items(
            items = if (picksExpanded) compact else compact.take(AtelierCollapsedPicks - AtelierPickPairCount),
            key = { track -> "atelier-picks-${track.id}" },
            contentType = { "atelier-compact-row" }
        ) { track ->
            AtelierCompactRow(
                track = track,
                isCurrent = track.id == currentId,
                actionsLabel = trackOptions,
                onPlay = { actions.playFrom(picks, track) },
                onActions = { actions.onTrackActions(track) }
            )
        }
        atelierExpandToggle("atelier-picks", picks.size, AtelierCollapsedPicks, picksExpanded, onTogglePicks)
    }
}

private fun LazyListScope.atelierHomeOrbitAndWall(
    orbit: List<Track>,
    wallRows: List<List<Track>>,
    currentId: String?,
    trackOptions: String,
    strings: LevyraStrings,
    copy: AtelierCopy,
    actions: AppleMusicHomeActions
) {
    if (orbit.isNotEmpty()) {
        item(key = "atelier-orbit", contentType = "atelier-standing-shelf") {
            Column {
                AtelierSectionLabel(strings.personalOrbitTitle)
                AtelierStandingShelf(orbit, currentId, trackOptions, { actions.playFrom(orbit, it) }, actions.onTrackActions)
            }
        }
    }
    if (wallRows.isNotEmpty()) {
        item(key = "atelier-wall-label", contentType = "atelier-magazine-heading") {
            AtelierMagazineHeading(copy.recentAlbums, centered = true)
        }
        items(wallRows, key = { row -> "atelier-wall-${row.first().id}" }, contentType = { "atelier-wall-row" }) { row ->
            AtelierWallRow(row) { track ->
                actions.openAlbum(trackAlbumHit(track))
            }
        }
    }
}

private fun LazyListScope.atelierHomeTeaserAndRecents(
    fanSleeves: List<Track>,
    shelfRecents: List<Track>,
    currentId: String?,
    trackOptions: String,
    state: LevyraUiState,
    strings: LevyraStrings,
    viewModel: HomeViewModel,
    actions: AppleMusicHomeActions,
    playRecent: (Track) -> Unit
) {
    item(key = "atelier-shelf-teaser", contentType = "atelier-shelf-teaser") {
        AtelierShelfTeaser(fanSleeves) { viewModel.selectTab(LevyraTab.Library) }
    }
    if (shelfRecents.isNotEmpty()) {
        item(key = "atelier-recents", contentType = "atelier-continue") {
            Column {
                AtelierSectionLabel(strings.continueListening)
                LazyRow(contentPadding = AtelierRowPadding, horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap)) {
                    items(shelfRecents, key = { it.id }, contentType = { "atelier-continue-tile" }) { track ->
                        AtelierContinueAlbumTile(
                            track = track,
                            isCurrent = track.id == currentId,
                            isPlaying = state.isPlaying,
                            actionsLabel = trackOptions,
                            onOpen = {
                                if (track.album.isNotBlank()) actions.openAlbum(trackAlbumHit(track)) else playRecent(track)
                            },
                            onPlay = { playRecent(track) },
                            onActions = { actions.onTrackActions(track) }
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListScope.atelierHomeAlbumGrid(
    albumRows: List<List<AlbumHit>>,
    columns: Int,
    strings: LevyraStrings,
    actions: AppleMusicHomeActions
) {
    if (albumRows.isNotEmpty()) {
        item(key = "atelier-albums-label", contentType = "atelier-label") { AtelierSectionLabel(strings.albumsForYou) }
        items(albumRows, key = { row -> "atelier-album-${atelierAlbumKey(row.first())}" }, contentType = { "atelier-album-row" }) { row ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = AtelierDesign.gutter).padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap)
            ) {
                row.forEach { album ->
                    AtelierAlbumTile(album, Modifier.weight(1f)) { actions.openAlbum(album) }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private fun LazyListScope.atelierHomeReleases(
    newTracks: List<Track>,
    newAlbums: List<AlbumHit>,
    releaseTitle: String,
    currentId: String?,
    trackOptions: String,
    actions: AppleMusicHomeActions
) {
    if (newTracks.isNotEmpty()) {
        item(key = "atelier-new-releases", contentType = "atelier-strip") {
            Column {
                AtelierSectionLabel(releaseTitle)
                AtelierTrackStrip(newTracks, AtelierDesign.albumCard, currentId, trackOptions, { actions.playFrom(newTracks, it) }, actions.onTrackActions)
            }
        }
    } else if (newAlbums.isNotEmpty()) {
        item(key = "atelier-new-albums", contentType = "atelier-album-strip") {
            Column {
                AtelierSectionLabel(releaseTitle)
                LazyRow(contentPadding = AtelierRowPadding, horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap)) {
                    items(newAlbums, key = { atelierAlbumKey(it) }, contentType = { "atelier-album-tile" }) { album ->
                        AtelierAlbumTile(album, Modifier.width(AtelierDesign.albumCard)) { actions.openAlbum(album) }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.atelierHomeTrendingArtists(
    artists: List<ArtistHit>,
    strings: LevyraStrings,
    actions: AppleMusicHomeActions
) {
    if (artists.isNotEmpty()) {
        item(key = "atelier-artists", contentType = "atelier-artists") {
            Column {
                AtelierSectionLabel(strings.trendingArtists)
                LazyRow(
                    contentPadding = AtelierRowPadding,
                    horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap),
                    verticalAlignment = Alignment.Top
                ) {
                    itemsIndexed(artists, key = { _, artist -> artist.browseId.ifBlank { artist.name } }, contentType = { _, _ -> "atelier-artist" }) { index, artist ->
                        AtelierArtistTile(artist, AtelierArtistSizes[index % AtelierArtistSizes.size]) { actions.openArtist(artist) }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.atelierHomeCharts(
    charts: List<Track>,
    chartsExpanded: Boolean,
    data: AppleMusicHomeData,
    strings: LevyraStrings,
    actions: AppleMusicHomeActions,
    trackLine: @Composable (List<Track>, Track, Int?) -> Unit,
    onToggleCharts: () -> Unit
) {
    if (charts.isNotEmpty()) {
        item(key = "atelier-charts-label", contentType = "atelier-label") {
            AtelierSectionLabel(data.chartsTitle.ifBlank { strings.top50Charts }) {
                if (data.countryLabel.isNotBlank()) {
                    AtelierTextAction(data.countryLabel, actions.pickCountry, onClickLabel = strings.chartMarketChange)
                }
                AtelierIconAction(Icons.Rounded.PlayArrow, strings.playAll, { actions.playAll(charts) }, AtelierDesign.accent)
            }
        }
        atelierTrackRows(
            key = "atelier-charts",
            tracks = charts,
            visible = if (chartsExpanded) charts.size else AtelierCollapsedCharts,
            ranked = true
        ) { track, rank -> trackLine(charts, track, rank) }
        atelierExpandToggle("atelier-charts", charts.size, AtelierCollapsedCharts, chartsExpanded, onToggleCharts)
    }
}

private fun LazyListScope.atelierHomeCollections(
    collections: List<AppleMusicHomeCollection>,
    strings: LevyraStrings,
    actions: AppleMusicHomeActions
) {
    if (collections.isNotEmpty()) {
        item(key = "atelier-collections", contentType = "atelier-collections") {
            Column {
                AtelierSectionLabel(strings.collectionsTitle)
                LazyRow(contentPadding = AtelierRowPadding, horizontalArrangement = Arrangement.spacedBy(AtelierDesign.itemGap)) {
                    items(collections, key = { it.id }, contentType = { "atelier-collection" }) { collection ->
                        AtelierCollectionTile(
                            collection = collection,
                            onOpen = { actions.openCollection(collection.id) },
                            onPlay = { actions.playAll(collection.tracks) }
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListScope.atelierHomeForgotten(
    forgotten: List<Track>,
    currentId: String?,
    trackOptions: String,
    strings: LevyraStrings,
    actions: AppleMusicHomeActions
) {
    if (forgotten.isNotEmpty()) {
        item(key = "atelier-forgotten", contentType = "atelier-strip") {
            Column {
                AtelierSectionLabel(strings.forgottenFavorites)
                AtelierTrackStrip(forgotten, AtelierSmallSleeve, currentId, trackOptions, { actions.playFrom(forgotten, it) }, actions.onTrackActions)
            }
        }
    }
}
