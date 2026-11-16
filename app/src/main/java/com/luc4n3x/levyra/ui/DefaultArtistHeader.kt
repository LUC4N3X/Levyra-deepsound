package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.components.levyraMarquee
import androidx.compose.animation.core.snap
import com.luc4n3x.levyra.domain.Track
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.lazy.LazyListScope
import com.luc4n3x.levyra.ui.album.coverFieldTone
import com.luc4n3x.levyra.ui.media.ImmersiveTopBarButtonSize
import com.luc4n3x.levyra.ui.media.ImmersiveTopBarButtonFill
import com.luc4n3x.levyra.ui.components.PlayerGlassIconButton
import kotlinx.coroutines.delay
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.rounded.MoreVert
import com.luc4n3x.levyra.ui.components.LevyraShortPlayLabel
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAddAlt1
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import com.luc4n3x.levyra.ui.components.LevyraMediaTitle
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.material3.toShape
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.ArtistBiography
import com.luc4n3x.levyra.domain.ArtistProfile
import com.luc4n3x.levyra.domain.ArtistRelease
import com.luc4n3x.levyra.domain.LevyraCanvasQuality
import com.luc4n3x.levyra.domain.artistBiographyEditorial
import com.luc4n3x.levyra.feature.motion.MotionArtwork
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraExpressiveCorner
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LevyraSpotifySkinStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.playlistAlbumCopy
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraType

internal const val DefaultArtistFieldMaxLuminance = 0.06f
internal const val DefaultArtistToneSettleMs = 2_500L

internal fun defaultArtistHeroHeight(width: Dp, height: Dp, canvasAspect: Float?): Dp {
    val base = minOf(width * 1.08f, height * 0.62f)
    val aspect = canvasAspect?.takeIf { it.isFinite() && it > 0f } ?: return base
    val upper = max(base, height * 0.8f)
    return (width / aspect).coerceIn(minOf(width * 0.56f, upper), upper)
}

internal fun artistAtmosphereTargets(
    defaultSkin: Boolean,
    settledTone: Color?,
    neutralTone: Color,
    paletteStart: Color,
    paletteEnd: Color
): Pair<Color, Color> {
    val source = if (defaultSkin) settledTone ?: neutralTone else null
    val pair = harmonizePlayerAccents(source ?: paletteStart, source ?: paletteEnd)
    return playerAmbienceOf(pair.primary, pair.secondary).tint to playerHighlightOf(pair.primary, pair.secondary)
}

internal fun defaultArtistPageBase(tone: Color?, defaultPage: Boolean, lightTheme: Boolean, pureBlack: Boolean, neutral: Color): Color =
    if (tone != null && defaultPage && !lightTheme && !pureBlack) coverFieldTone(tone, DefaultArtistFieldMaxLuminance) else neutral

internal fun defaultArtistArtworkHeightPx(defaultPage: Boolean, width: Dp, height: Dp, density: Density, heroHeightPx: Float): Float =
    if (defaultPage) with(density) { defaultArtistHeroHeight(width, height, null).toPx() } else heroHeightPx

internal fun LazyListScope.defaultArtistAboutItem(defaultPage: Boolean, artist: ArtistProfile, imageUrl: String, accent: Color) {
    val biography = artist.biography
    if (!defaultPage || !artist.hasBio || biography == null) return
    item(key = "artist-about", contentType = "artist-about") {
        DefaultArtistAbout(artist, biography, imageUrl, accent, Modifier.padding(top = 8.dp))
    }
}

@Composable
internal fun DefaultArtistHeroSection(
    artist: ArtistProfile,
    artworkUrl: String,
    height: Dp,
    pageBase: Color,
    highlight: Color,
    motionArtwork: MotionArtwork?,
    animationsEnabled: Boolean,
    motionArtworkEnabled: Boolean,
    canvasQuality: LevyraCanvasQuality,
    scroll: () -> Float,
    collapse: () -> Float,
    awaitingArtwork: Boolean,
    isFollowed: Boolean,
    canvasToneSettled: Boolean,
    onVideoAspect: (Float?) -> Unit,
    onCanvasTone: (Color) -> Unit,
    onPlay: (Track) -> Unit,
    onPlayAll: (List<Track>) -> Unit,
    onToggleFollow: () -> Unit,
    onOpenRelease: (ArtistRelease, String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        DefaultArtistHero(
            profile = artist,
            artworkUrl = artworkUrl,
            height = height,
            base = pageBase,
            motionArtwork = motionArtwork,
            motionEnabled = animationsEnabled && motionArtworkEnabled,
            canvasQuality = canvasQuality,
            scroll = scroll,
            collapse = collapse,
            awaitingArtwork = awaitingArtwork,
            onVideoAspect = onVideoAspect,
            onCanvasTone = { tone -> if (tone != null && !canvasToneSettled) onCanvasTone(tone) }
        )
        val neutral = LevyraBlack
        val fade = remember(pageBase, neutral) { Brush.verticalGradient(listOf(pageBase, neutral)) }
        Box(Modifier.fillMaxWidth().background(fade)) {
            DefaultArtistSummary(
                profile = artist,
                isFollowed = isFollowed,
                base = lerp(pageBase, Color.White, 0.05f),
                highlight = highlight,
                onPlay = artist.topSongs.firstOrNull()?.let { track -> { onPlay(track) } },
                onShuffle = artist.topSongs.takeIf { it.size > 1 }?.let { songs -> { onPlayAll(songs.shuffled()) } },
                onToggleFollow = onToggleFollow,
                onOpenRelease = onOpenRelease
            )
        }
    }
}

@Composable
internal fun SettleDefaultArtistTone(key: Any?, enabled: Boolean, photoReady: Boolean, settled: () -> Boolean, onSettle: () -> Unit) {
    val currentSettled by rememberUpdatedState(settled)
    val currentOnSettle by rememberUpdatedState(onSettle)
    LaunchedEffect(key, enabled, photoReady) {
        if (!enabled || !photoReady || currentSettled()) return@LaunchedEffect
        delay(DefaultArtistToneSettleMs)
        if (!currentSettled()) currentOnSettle()
    }
}

@Composable
internal fun DefaultArtistTopActions(
    visible: Boolean,
    profile: ArtistProfile?,
    isExcluded: Boolean,
    onToggleExclude: (String, String) -> Unit,
    isPinnedToHome: Boolean,
    onTogglePinToHome: (ArtistProfile) -> Unit
) {
    if (!visible || profile == null) return
    val strings = LocalLevyraStrings.current
    var expanded by remember(profile.browseId, profile.name) { mutableStateOf(false) }
    Box {
        PlayerGlassIconButton(
            icon = Icons.Rounded.MoreVert,
            contentDescription = strings.options,
            onClick = { expanded = true },
            size = ImmersiveTopBarButtonSize,
            tint = Color.White,
            fill = ImmersiveTopBarButtonFill,
            borderTop = Color.Transparent,
            borderBottom = Color.Transparent
        )
        ArtistOptionsMenu(
            expanded = expanded,
            onDismiss = { expanded = false },
            isExcluded = isExcluded,
            onToggleExclude = { onToggleExclude(profile.browseId, profile.name) },
            isPinnedToHome = isPinnedToHome,
            onTogglePinToHome = { onTogglePinToHome(profile) }
        )
    }
}

internal fun defaultArtistHeroHeightOr(defaultPage: Boolean, width: Dp, height: Dp, canvasAspect: Float?, fallback: Dp): Dp =
    if (defaultPage) defaultArtistHeroHeight(width, height, canvasAspect) else fallback

@Composable
internal fun ArtistHeroSlot(defaultPage: Boolean, defaultContent: @Composable () -> Unit, content: @Composable () -> Unit) {
    if (defaultPage) defaultContent() else content()
}

internal fun artistCountLabel(label: String, count: Int, more: Boolean): String =
    if (more) label.replaceFirst(count.toString(), "$count+") else label

@Composable
internal fun DefaultArtistHero(
    profile: ArtistProfile,
    artworkUrl: String,
    height: Dp,
    base: Color,
    motionArtwork: MotionArtwork?,
    motionEnabled: Boolean,
    canvasQuality: LevyraCanvasQuality,
    scroll: () -> Float,
    collapse: () -> Float,
    awaitingArtwork: Boolean,
    onVideoAspect: (Float?) -> Unit,
    onCanvasTone: (Color?) -> Unit
) {
    val animatedHeight by animateDpAsState(height, if (LocalAnimationsEnabled.current) tween(LevyraPlayerDesign.PaletteMillis) else snap(), label = "artist-hero-height")
    val fade = remember(base) {
        Brush.verticalGradient(
            0f to Color.Black.copy(alpha = 0.34f),
            0.16f to Color.Transparent,
            0.5f to Color.Transparent,
            0.78f to base.copy(alpha = 0.78f),
            1f to base
        )
    }
    val identity = remember(profile.browseId, profile.name) { profile.browseId.ifBlank { profile.name.trim().lowercase() } }
    Box(
        Modifier
            .fillMaxWidth()
            .height(animatedHeight)
            .clipToBounds()
            .background(base)
    ) {
        Box(Modifier.matchParentSize().graphicsLayer { translationY = scroll() * 0.42f }) {
            key(identity) {
                MotionArtworkLayer(
                    artwork = motionArtwork,
                    enabled = motionEnabled,
                    isPlaying = false,
                    pageMode = true,
                    cornerRadius = 0.dp,
                    presentation = MotionArtworkPresentation.Cinematic,
                    quality = canvasQuality,
                    staticArtworkMotionEnabled = false,
                    onVideoAspect = onVideoAspect,
                    dynamicBackdropEnabled = true,
                    onDynamicBackdropPalette = { onCanvasTone(it?.primary) },
                    modifier = Modifier.matchParentSize()
                ) {
                    if (artworkUrl.isNotBlank()) {
                        StableRemoteArtwork(
                            url = artworkUrl,
                            contentDescription = profile.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            highRes = true,
                            alignment = Alignment.TopCenter
                        )
                    } else if (!awaitingArtwork) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Person, null, tint = LevyraText.copy(alpha = 0.42f), modifier = Modifier.size(96.dp))
                        }
                    }
                }
            }
        }
        Box(Modifier.matchParentSize().background(fade))
        Text(
            text = profile.name,
            color = LevyraText,
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = if (profile.name.length > 16) 34.sp else 44.sp,
                lineHeight = if (profile.name.length > 16) 38.sp else 48.sp,
                letterSpacing = (-0.8).sp
            ),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp)
                .graphicsLayer { alpha = (1f - collapse() * 1.4f).coerceIn(0f, 1f) }
                .semantics { heading() }
        )
    }
}

@Composable
internal fun DefaultArtistSummary(
    profile: ArtistProfile,
    isFollowed: Boolean,
    base: Color,
    highlight: Color,
    onPlay: (() -> Unit)?,
    onShuffle: (() -> Unit)?,
    onToggleFollow: () -> Unit,
    onOpenRelease: (ArtistRelease, String) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val audience = remember(profile.monthlyListeners, profile.subscribers) {
        listOf(profile.monthlyListeners, profile.subscribers).map(String::trim).filter(String::isNotBlank).joinToString("  •  ")
    }
    val catalog = remember(profile, strings) {
        listOfNotNull(
            profile.topSongs.size.takeIf { it > 0 }?.let { artistCountLabel(strings.formatTrackCount(it), it, profile.songsBrowseId.isNotBlank()) },
            profile.albums.size.takeIf { it > 0 }?.let { artistCountLabel(strings.playlistAlbumCopy().albumCount(it), it, profile.albumsBrowseId.isNotBlank()) }
        ).joinToString("  •  ")
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        listOf(audience, profile.views.trim(), catalog).filter(String::isNotBlank).forEach { line ->
            Text(
                line,
                color = LevyraText.copy(alpha = 0.66f),
                style = LevyraType.metadata.copy(fontSize = 14.sp),
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp).levyraMarquee()
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ArtistRoundAction(Icons.Rounded.Shuffle, strings.shuffle, enabled = onShuffle != null) { onShuffle?.invoke() }
            ArtistPlayPill(strings.play, onPlay, Modifier.weight(1f, fill = false))
            ArtistRoundAction(
                if (isFollowed) Icons.Rounded.Check else Icons.Rounded.PersonAddAlt1,
                if (isFollowed) strings.followingArtist else strings.followArtist,
                active = isFollowed,
                activeTint = highlight,
                onClick = onToggleFollow
            )
        }
        appleMusicLatestRelease(profile)?.let { release ->
            Spacer(Modifier.height(22.dp))
            ArtistLatestReleaseCard(release, base, highlight) { onOpenRelease(release, profile.name) }
        }
    }
}

@Composable
private fun ArtistRoundAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean = true,
    active: Boolean = false,
    activeTint: Color = LevyraText,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(ArtistRoundActionSize)
            .clip(CircleShape)
            .background(LevyraText.copy(alpha = if (active) 0.16f else 0.10f))
            .levyraPressable(onClick = onClick, enabled = enabled, role = Role.Button, pressedScale = LevyraPressScale.Control, onClickLabel = label)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = if (!enabled) LevyraText.copy(alpha = 0.38f) else if (active) activeTint else LevyraText, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun ArtistPlayPill(label: String, onPlay: (() -> Unit)?, modifier: Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val corner = levyraExpressiveCorner(interactionSource = interaction, rest = ArtistPlayPillHeight / 2, pressed = 18.dp, label = "artist-play-corner")
    Row(
        modifier
            .widthIn(min = 150.dp, max = 240.dp)
            .heightIn(min = ArtistPlayPillHeight)
            .clip(RoundedCornerShape(corner))
            .background(if (onPlay != null) Color.White else Color.White.copy(alpha = 0.38f))
            .levyraPressable(
                onClick = { onPlay?.invoke() },
                enabled = onPlay != null,
                pressedScale = LevyraPressScale.Control,
                interactionSource = interaction,
                role = Role.Button,
                haptic = LevyraHapticAction.Transport,
                onClickLabel = label
            )
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.PlayArrow, null, tint = Color(0xFF111214), modifier = Modifier.size(28.dp))
        Text(
            LevyraShortPlayLabel,
            color = Color(0xFF111214),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ArtistLatestReleaseCard(release: ArtistRelease, base: Color, highlight: Color, onOpen: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraSpotifySkinStrings.forCode(strings.code) }
    val detail = remember(release) {
        if (release.year.isNotBlank() && release.subtitle.contains(release.year)) release.subtitle
        else listOf(release.subtitle, release.year).filter(String::isNotBlank).distinct().joinToString(" • ")
    }
    val glow = remember(base, highlight) {
        Brush.linearGradient(listOf(highlight.copy(alpha = 0.30f).compositeOver(base), base, base))
    }
    val burst = MaterialShapes.SoftBurst.toShape()
    val sleeve = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(ArtistCardShape)
            .background(glow)
            .levyraPressable(onClick = onOpen, role = Role.Button, pressedScale = LevyraPressScale.Tile, onClickLabel = release.title)
            .padding(start = 14.dp, end = 18.dp, top = 18.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(width = 136.dp, height = 120.dp)) {
            if (release.thumbnailUrl.isNotBlank()) {
                StableRemoteArtwork(
                    release.thumbnailUrl,
                    null,
                    Modifier
                        .align(Alignment.CenterEnd)
                        .size(96.dp)
                        .graphicsLayer {
                            rotationZ = 9f
                            alpha = 0.5f
                            shape = sleeve
                            clip = true
                        },
                    ContentScale.Crop
                )
                StableRemoteArtwork(
                    release.thumbnailUrl,
                    release.title,
                    Modifier
                        .align(Alignment.CenterStart)
                        .size(112.dp)
                        .graphicsLayer {
                            rotationZ = -4f
                            shadowElevation = 18.dp.toPx()
                            shape = sleeve
                            clip = true
                        },
                    ContentScale.Crop,
                    highRes = true
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(14.dp).clip(burst).background(highlight))
                LevyraMediaTitle(
                    copy.latestRelease,
                    color = highlight,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
            Text(
                release.title,
                color = LevyraText,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (detail.isNotBlank()) {
                Text(detail, color = LevyraText.copy(alpha = 0.66f), style = LevyraType.metadata, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DefaultArtistAbout(profile: ArtistProfile, biography: ArtistBiography, imageUrl: String, accent: Color, modifier: Modifier = Modifier) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { LevyraSpotifySkinStrings.forCode(strings.code) }
    val presentation = remember(biography) {
        val editorial = artistBiographyEditorial(biography.text, biography.description, biography.languageCode)
        buildArtistBiographyPresentation(biography, editorial.paragraphs, editorial.summary)
    }
    val photo = imageUrl.ifBlank { biography.originalImageUrl.ifBlank { biography.thumbnailUrl } }
    var sheetOpen by rememberSaveable(profile.browseId, profile.name) { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(ArtistCardShape)
            .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.16f), LevyraText.copy(alpha = 0.05f))))
            .levyraPressable(onClick = { sheetOpen = true }, role = Role.Button, pressedScale = LevyraPressScale.Tile, onClickLabel = copy.aboutArtist)
            .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "“",
            color = accent,
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
            modifier = Modifier.height(40.dp)
        )
        Text(
            presentation.previewText,
            color = LevyraText.copy(alpha = 0.9f),
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 25.sp),
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(LevyraText.copy(alpha = 0.10f))) {
                if (photo.isNotBlank()) StableRemoteArtwork(photo, null, Modifier.matchParentSize(), ContentScale.Crop, alignment = Alignment.TopCenter)
            }
            Text(
                listOf(profile.name, presentation.sourceText.takeIf { presentation.showSource }.orEmpty()).filter(String::isNotBlank).joinToString(" · "),
                color = LevyraText.copy(alpha = 0.62f),
                style = LevyraType.metadata,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(strings.readAll, color = accent, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), maxLines = 1)
        }
    }
    if (sheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { sheetOpen = false }, sheetState = sheetState) {
            ArtistAboutSheet(profile.name, photo, presentation, biography.sourceUrl)
        }
    }
}

@Composable
private fun ArtistAboutSheet(name: String, photo: String, presentation: ArtistBiographyPresentation, sourceUrl: String) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (photo.isNotBlank()) {
            StableRemoteArtwork(
                photo,
                name,
                Modifier.fillMaxWidth().aspectRatio(1.4f).clip(ArtistCardShape),
                ContentScale.Crop,
                highRes = true,
                alignment = Alignment.TopCenter
            )
        }
        Text(name, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.semantics { heading() })
        presentation.paragraphs.filter(String::isNotBlank).forEach { paragraph ->
            Text(paragraph, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 25.sp))
        }
        if (presentation.showSource && sourceUrl.isNotBlank()) {
            Row(
                Modifier
                    .heightIn(min = 48.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .clickable(role = Role.Button) { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(sourceUrl))) } }
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(presentation.sourceText, style = MaterialTheme.typography.labelLarge)
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

private val ArtistRoundActionSize = 56.dp
private val ArtistPlayPillHeight = 56.dp
private val ArtistCardShape = RoundedCornerShape(28.dp)
