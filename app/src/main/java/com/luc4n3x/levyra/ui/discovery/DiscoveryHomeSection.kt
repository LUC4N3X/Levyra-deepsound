package com.luc4n3x.levyra.ui.discovery

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luc4n3x.levyra.domain.HomeStyle
import com.luc4n3x.levyra.feature.discovery.DiscoveryCard
import com.luc4n3x.levyra.feature.discovery.DiscoveryReasonKind
import com.luc4n3x.levyra.ui.CoverImage
import com.luc4n3x.levyra.ui.HomeStyleSpec
import com.luc4n3x.levyra.ui.LevyraIsLight
import com.luc4n3x.levyra.ui.StudioSectionHeader
import com.luc4n3x.levyra.ui.components.LevyraLoadingIndicator
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.components.rememberLevyraPlayMorphShape
import com.luc4n3x.levyra.ui.i18n.DiscoveryCopy
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.discoveryCopy
import com.luc4n3x.levyra.ui.theme.LevyraHomeDesign
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraType
import com.luc4n3x.levyra.viewmodel.DiscoveryFeedStatus
import com.luc4n3x.levyra.viewmodel.DiscoveryUiState
import com.materialkolor.hct.Hct
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

@Stable
internal class DiscoveryHomeHandle(
    val state: StateFlow<DiscoveryUiState>,
    val onWarm: () -> Unit,
    val onOpen: () -> Unit,
    val onRetry: () -> Unit
)

internal val LocalHomeDiscovery = staticCompositionLocalOf<DiscoveryHomeHandle?> { null }

internal fun LazyListScope.discoverySection(
    handle: DiscoveryHomeHandle?,
    style: HomeStyle,
    spec: HomeStyleSpec,
    animationsEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (handle == null) return
    item(key = "home-discovery", contentType = "home-discovery") {
        val ui by handle.state.collectAsStateWithLifecycle()
        LaunchedEffect(handle) { handle.onWarm() }
        Box(modifier) { DiscoveryHomeSection(ui, style, spec, animationsEnabled, handle) }
    }
}

@Composable
private fun DiscoveryHomeSection(
    ui: DiscoveryUiState,
    style: HomeStyle,
    spec: HomeStyleSpec,
    animationsEnabled: Boolean,
    handle: DiscoveryHomeHandle
) {
    val strings = LocalLevyraStrings.current
    val copy = remember(strings.code) { discoveryCopy(strings.code) }
    val teaser = ui.teaser
    if (teaser.isEmpty() && (ui.status == DiscoveryFeedStatus.Empty || ui.status == DiscoveryFeedStatus.Error)) {
        DiscoveryHomeNotice(copy, spec.inset, ui.status == DiscoveryFeedStatus.Error, handle)
        return
    }
    val loading = ui.status == DiscoveryFeedStatus.Loading || ui.status == DiscoveryFeedStatus.Idle
    val model = DiscoveryTeaserModel(teaser, loading, copy, handle.onOpen)
    when (style) {
        HomeStyle.Original -> OriginalDiscoveryTeaser(model, spec, animationsEnabled)
        HomeStyle.ExpressiveMax -> ExpressiveMaxDiscoveryTeaser(model, spec, animationsEnabled)
        HomeStyle.Flow -> FlowDiscoveryTeaser(model, spec)
        HomeStyle.Editorial -> EditorialDiscoveryTeaser(model, spec, strings.code)
        HomeStyle.Minimal -> MinimalDiscoveryTeaser(model, spec)
        HomeStyle.Immersive -> ImmersiveDiscoveryTeaser(model, spec)
        HomeStyle.MaterialExpressive -> MaterialExpressiveDiscoveryTeaser(model, spec, animationsEnabled)
    }
}

private class DiscoveryTeaserModel(
    val cards: List<DiscoveryCard>,
    val loading: Boolean,
    val copy: DiscoveryCopy,
    val onOpen: () -> Unit
) {
    val lead: DiscoveryCard? get() = cards.firstOrNull()
    fun card(index: Int): DiscoveryCard? = cards.getOrNull(index)
}

internal fun discoveryReasonLabel(card: DiscoveryCard, copy: DiscoveryCopy): String = when (card.reason.kind) {
    DiscoveryReasonKind.SimilarToTrack -> card.reason.seedTitle.takeIf(String::isNotBlank)?.let(copy::reasonBecause) ?: copy.reasonSimilarArtist
    DiscoveryReasonKind.FollowedArtist -> copy.reasonFollowedArtist
    DiscoveryReasonKind.NewRelease -> copy.reasonNewRelease
    DiscoveryReasonKind.SimilarArtist -> copy.reasonSimilarArtist
    DiscoveryReasonKind.Trending -> copy.reasonTrending
    DiscoveryReasonKind.Editorial -> copy.reasonEditorial
}

@Composable
internal fun discoveryAccent(card: DiscoveryCard?, fallback: Color = MaterialTheme.colorScheme.primary): Color =
    card?.track?.accentStart?.takeIf { it != 0 }?.let(::Color) ?: fallback

@Composable
internal fun DiscoveryArtwork(card: DiscoveryCard?, modifier: Modifier, highRes: Boolean = false) {
    if (card == null) {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest))
    } else {
        CoverImage(track = card.track, modifier = modifier, highRes = highRes)
    }
}

@Composable
private fun Modifier.discoveryEntry(model: DiscoveryTeaserModel): Modifier = this
    .semantics { contentDescription = model.copy.title + ". " + model.copy.lead }
    .levyraPressable(onClick = model.onOpen, onClickLabel = model.copy.start, role = Role.Button)

@Composable
private fun DiscoveryLeadLine(model: DiscoveryTeaserModel, color: Color, style: TextStyle = MaterialTheme.typography.bodyMedium) {
    val lead = model.lead
    Text(
        text = when {
            model.loading && lead == null -> model.copy.loading
            lead != null -> discoveryReasonLabel(lead, model.copy)
            else -> model.copy.lead
        },
        style = style,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OriginalDiscoveryTeaser(model: DiscoveryTeaserModel, spec: HomeStyleSpec, animationsEnabled: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val isLight = LevyraIsLight
    val accent = discoveryAccent(model.lead)
    val action = remember(accent, isLight) { discoveryActionRole(accent, isLight) }
    val shape = LevyraHomeDesign.HeroShape
    val morph = rememberLevyraPlayMorphShape(playing = false, animated = animationsEnabled)
    val coverBorder = scheme.surfaceContainer
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spec.inset)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(lerp(scheme.surfaceContainer, accent, 0.24f), scheme.surfaceContainer)))
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.35f), shape)
            .discoveryEntry(model)
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                model.copy.title,
                style = LevyraType.sectionTitle,
                color = LevyraText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                if (model.loading && model.lead == null) model.copy.loading else model.copy.lead,
                style = LevyraType.metadata,
                color = LevyraMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        DiscoveryDeck(model, animationsEnabled, accent, action, coverBorder)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(52.dp).clip(morph).background(action.container), contentAlignment = Alignment.Center) {
                if (model.loading && model.lead == null) LevyraLoadingIndicator(Modifier.size(24.dp), action.content)
                else Icon(Icons.Rounded.PlayArrow, null, tint = action.content, modifier = Modifier.size(28.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(model.copy.start, style = LevyraType.cardTitle.copy(fontWeight = FontWeight.Bold), color = LevyraText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(model.copy.swipeHint, style = MaterialTheme.typography.bodySmall, color = LevyraMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private class DiscoveryActionRole(val container: Color, val content: Color)

private fun discoveryActionRole(accent: Color, isLight: Boolean): DiscoveryActionRole {
    val source = Hct.fromInt(accent.toArgb())
    val chroma = source.chroma.coerceAtMost(48.0)
    return DiscoveryActionRole(
        container = Color(Hct.from(source.hue, chroma, if (isLight) 40.0 else 80.0).toInt()),
        content = Color(Hct.from(source.hue, chroma, if (isLight) 100.0 else 20.0).toInt())
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DiscoveryDeck(
    model: DiscoveryTeaserModel,
    animationsEnabled: Boolean,
    accent: Color,
    action: DiscoveryActionRole,
    coverBorder: Color
) {
    val deckSize = model.cards.size.coerceAtMost(DeckMaxCards)
    val turning = animationsEnabled && deckSize >= DeckMinCards
    val turn = remember { Animatable(0f) }
    LaunchedEffect(turning, deckSize) {
        turn.snapTo(0f)
        if (!turning) return@LaunchedEffect
        repeat(deckSize * DeckLoops) {
            delay(DeckHoldMillis)
            turn.animateTo(turn.value.roundToInt() + 1f, tween(DeckTurnMillis, easing = DeckEasing))
        }
    }
    val base by remember { derivedStateOf { floor(turn.value).toInt() } }
    val front by remember { derivedStateOf { turn.value.roundToInt() } }
    fun cardAt(index: Int): DiscoveryCard? =
        if (deckSize >= DeckMinCards) model.cards[index.mod(deckSize)] else model.card(if (index < 0) 2 else index)
    val isLight = LevyraIsLight
    Box(Modifier.fillMaxWidth().height(164.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(184.dp).graphicsLayer { rotationZ = DeckBlobRest + DeckBlobStep * turn.value }
                .clip(MaterialShapes.SoftBurst.toShape())
                .background(accent.copy(alpha = if (isLight) 0.12f else 0.18f))
        )
        for (index in (base - 1)..(base + 2)) {
            key(index) {
                DiscoveryArtwork(
                    cardAt(index),
                    Modifier
                        .zIndex(DeckFrontZ - abs(index - front))
                        .size(DeckCoverSize)
                        .graphicsLayer {
                            val position = index - turn.value
                            translationX = deckValue(position) { it.x }.dp.toPx()
                            translationY = deckValue(position) { it.y }.dp.toPx()
                            rotationZ = deckValue(position) { it.rotation }
                            val scale = deckValue(position) { it.scale }
                            scaleX = scale
                            scaleY = scale
                            alpha = deckValue(position) { it.alpha }
                        }
                        .clip(DeckCoverShape)
                        .border(3.dp, coverBorder, DeckCoverShape),
                    highRes = true
                )
            }
        }
        val frontCard = cardAt(front)
        if (frontCard != null) {
            Surface(
                color = action.container,
                contentColor = action.content,
                shape = CircleShape,
                modifier = Modifier.align(Alignment.BottomCenter).zIndex(DeckFrontZ + 1f).padding(horizontal = 24.dp)
            ) {
                Row(
                    Modifier.padding(start = 8.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(14.dp))
                    AnimatedContent(
                        targetState = discoveryReasonLabel(frontCard, model.copy),
                        transitionSpec = {
                            fadeIn(tween(220, delayMillis = 60)) + slideInVertically(tween(220, delayMillis = 60)) { it / 3 } togetherWith
                                fadeOut(tween(140))
                        },
                        label = "discovery-reason"
                    ) { label ->
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private class DeckSlot(val x: Float, val y: Float, val rotation: Float, val scale: Float, val alpha: Float)

private val DeckSlots = listOf(
    DeckSlot(-34f, 14f, -6f, 0.58f, 0f),
    DeckSlot(-66f, 10f, -12f, 0.75f, 1f),
    DeckSlot(0f, 0f, 0f, 1f, 1f),
    DeckSlot(66f, 10f, 12f, 0.75f, 1f),
    DeckSlot(34f, 14f, 6f, 0.58f, 0f)
)

private inline fun deckValue(position: Float, pick: (DeckSlot) -> Float): Float {
    val offset = position.coerceIn(-2f, 2f) + 2f
    val lower = floor(offset).toInt().coerceAtMost(DeckSlots.lastIndex - 1)
    val fraction = offset - lower
    val start = pick(DeckSlots[lower])
    return start + (pick(DeckSlots[lower + 1]) - start) * fraction
}

private val DeckCoverSize = 128.dp
private val DeckCoverShape = RoundedCornerShape(26.dp)
private val DeckEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private const val DeckMaxCards = 6
private const val DeckMinCards = 3
private const val DeckLoops = 2
private const val DeckHoldMillis = 2_800L
private const val DeckTurnMillis = 850
private const val DeckFrontZ = 10f
private const val DeckBlobRest = 18f
private const val DeckBlobStep = 30f

@Composable
private fun rememberSlowSpin(active: Boolean, periodMillis: Int): State<Float> {
    if (!active) return remember { mutableFloatStateOf(0f) }
    val transition = rememberInfiniteTransition(label = "discovery-spin")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(periodMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "discovery-spin-value"
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpressiveMaxDiscoveryTeaser(model: DiscoveryTeaserModel, spec: HomeStyleSpec, animationsEnabled: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val accent = discoveryAccent(model.lead)
    val spin by rememberSlowSpin(animationsEnabled, 48_000)
    val morph = rememberLevyraPlayMorphShape(playing = false, animated = animationsEnabled)
    val ink = if (LevyraIsLight) LevyraText else Color.White
    BoxWithConstraints(Modifier.fillMaxWidth().discoveryEntry(model)) {
        val heroSize = (maxWidth * 0.62f).coerceIn(200.dp, 260.dp)
        Box(Modifier.fillMaxWidth().height(heroSize + 120.dp)) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.radialGradient(listOf(accent.copy(alpha = if (LevyraIsLight) 0.16f else 0.28f), Color.Transparent), radius = 900f)
                )
            )
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = 28.dp, y = 8.dp).size(heroSize)
                    .graphicsLayer { rotationZ = spin }
                    .clip(MaterialShapes.SoftBurst.toShape())
                    .background(scheme.tertiaryContainer)
                    .padding(6.dp)
                    .graphicsLayer { rotationZ = -spin }
            ) {
                DiscoveryArtwork(model.lead, Modifier.fillMaxSize().clip(MaterialShapes.Cookie12Sided.toShape()), highRes = true)
            }
            DiscoveryArtwork(
                model.card(1),
                Modifier.align(Alignment.TopStart).offset(x = spec.inset, y = 26.dp).size(92.dp)
                    .graphicsLayer { rotationZ = -10f }.clip(MaterialShapes.Clover8Leaf.toShape())
            )
            DiscoveryArtwork(
                model.card(2),
                Modifier.align(Alignment.TopStart).offset(x = spec.inset + 64.dp, y = 128.dp).size(66.dp).clip(MaterialShapes.Gem.toShape())
            )
            Column(
                Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(start = spec.inset, end = spec.inset + 92.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(color = scheme.tertiaryContainer, contentColor = scheme.onTertiaryContainer, shape = CircleShape) {
                    Row(
                        Modifier.padding(start = 8.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(16.dp))
                        DiscoveryLeadLine(model, scheme.onTertiaryContainer, MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
                Text(
                    model.copy.title,
                    style = MaterialTheme.typography.displaySmallEmphasized.copy(fontWeight = FontWeight.Black),
                    color = ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    model.copy.lead,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (LevyraIsLight) LevyraMuted else Color.White.copy(alpha = 0.78f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                Modifier.align(Alignment.BottomEnd).padding(end = spec.inset, bottom = 10.dp).size(76.dp).clip(morph)
                    .background(Brush.linearGradient(listOf(scheme.primary, scheme.tertiary))),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(68.dp).clip(morph).background(scheme.primary), contentAlignment = Alignment.Center) {
                    if (model.loading && model.lead == null) LevyraLoadingIndicator(Modifier.size(30.dp), scheme.onPrimary)
                    else Icon(Icons.Rounded.PlayArrow, model.copy.start, tint = scheme.onPrimary, modifier = Modifier.size(36.dp))
                }
            }
        }
    }
}

@Composable
private fun FlowDiscoveryTeaser(model: DiscoveryTeaserModel, spec: HomeStyleSpec) {
    val scheme = MaterialTheme.colorScheme
    val flowShape = RoundedCornerShape(topStart = 28.dp, topEnd = 8.dp, bottomEnd = 28.dp, bottomStart = 8.dp)
    val cards = if (model.cards.isEmpty()) List(4) { null } else model.cards.take(4)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        StudioSectionHeader(HomeStyle.Flow, spec, model.copy.title, featured = false, onPlayAll = null)
        Box(Modifier.padding(horizontal = spec.inset)) { DiscoveryLeadLine(model, LevyraMuted) }
        LazyRow(contentPadding = PaddingValues(horizontal = spec.inset), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(cards, key = { index, card -> card?.key ?: "placeholder-$index" }) { index, card ->
                val width: Dp = if (index == 0) spec.cardWidth + 24.dp else spec.cardWidth - 12.dp * index.coerceAtMost(2)
                Column(
                    Modifier
                        .width(width)
                        .graphicsLayer { alpha = 1f - 0.12f * index }
                        .levyraPressable(onClick = model.onOpen, onClickLabel = model.copy.start, role = Role.Button),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DiscoveryArtwork(card, Modifier.fillMaxWidth().aspectRatio(1f).clip(flowShape))
                    Text(card?.track?.title.orEmpty(), style = MaterialTheme.typography.titleSmall, color = LevyraText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(card?.track?.artist.orEmpty(), style = MaterialTheme.typography.bodySmall, color = LevyraMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            item(key = "discovery-flow-start") {
                Box(
                    Modifier
                        .size(spec.cardWidth - 24.dp)
                        .clip(flowShape)
                        .background(scheme.primaryContainer)
                        .levyraPressable(onClick = model.onOpen, onClickLabel = model.copy.start, role = Role.Button)
                        .padding(14.dp)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = scheme.onPrimaryContainer, modifier = Modifier.align(Alignment.TopEnd))
                    Text(
                        model.copy.start,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = scheme.onPrimaryContainer,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorialDiscoveryTeaser(model: DiscoveryTeaserModel, spec: HomeStyleSpec, languageCode: String) {
    val scheme = MaterialTheme.colorScheme
    val locale = remember(languageCode) { Locale.forLanguageTag(languageCode) }
    val dateline = remember(locale) {
        runCatching { LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)) }.getOrDefault("")
    }
    val serif = FontFamily.Serif
    Column(Modifier.fillMaxWidth().padding(horizontal = spec.inset).discoveryEntry(model), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.6f))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (dateline.isNotBlank()) {
                Text(dateline.uppercase(locale), style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.6.sp), color = scheme.primary)
            }
            Text(
                model.copy.title,
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = serif),
                color = LevyraText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                model.copy.lead,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = serif, fontStyle = FontStyle.Italic),
                color = LevyraMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(0.46f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DiscoveryArtwork(
                    model.lead,
                    Modifier.fillMaxWidth().aspectRatio(0.8f).clip(RoundedCornerShape(topStart = 4.dp, topEnd = 32.dp, bottomEnd = 4.dp, bottomStart = 4.dp)),
                    highRes = true
                )
                model.lead?.let { lead ->
                    Text(
                        discoveryReasonLabel(lead, model.copy).uppercase(locale),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                        color = scheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(Modifier.weight(0.54f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val entries = if (model.cards.isEmpty()) List(3) { null } else model.cards.take(3)
                entries.forEachIndexed { index, card ->
                    if (index > 0) HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.4f))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("%02d".format(Locale.ROOT, index + 1), style = MaterialTheme.typography.titleLarge.copy(fontFamily = serif), color = scheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text(card?.track?.title.orEmpty(), style = MaterialTheme.typography.titleMedium.copy(fontFamily = serif), color = LevyraText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(card?.track?.artist.orEmpty(), style = MaterialTheme.typography.bodySmall, color = LevyraMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                TextButton(onClick = model.onOpen, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)) {
                    Text(model.copy.start, fontFamily = serif)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun MinimalDiscoveryTeaser(model: DiscoveryTeaserModel, spec: HomeStyleSpec) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).discoveryEntry(model).padding(horizontal = spec.inset, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(Modifier.size(width = 88.dp, height = 52.dp)) {
            for (index in 2 downTo 0) {
                DiscoveryArtwork(
                    model.card(index),
                    Modifier.align(Alignment.CenterStart).offset(x = 18.dp * index).size(52.dp).clip(shape).border(1.5.dp, scheme.surface, shape)
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                model.copy.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = LevyraText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            DiscoveryLeadLine(model, LevyraMuted, MaterialTheme.typography.bodySmall)
        }
        OutlinedIconButton(onClick = model.onOpen, modifier = Modifier.size(48.dp)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, model.copy.start, tint = LevyraText)
        }
    }
}

@Composable
private fun ImmersiveDiscoveryTeaser(model: DiscoveryTeaserModel, spec: HomeStyleSpec) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(32.dp)
    val thumbShape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = spec.inset)
            .aspectRatio(0.92f)
            .clip(shape)
            .background(lerp(scheme.surfaceContainerHigh, discoveryAccent(model.lead), 0.3f))
            .discoveryEntry(model)
    ) {
        DiscoveryArtwork(model.lead, Modifier.fillMaxSize(), highRes = true)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color.Transparent, 0.45f to Color.Black.copy(alpha = 0.08f), 1f to Color.Black.copy(alpha = 0.82f))
            )
        )
        Surface(
            color = Color.Black.copy(alpha = 0.42f),
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(14.dp))
                DiscoveryLeadLine(model, Color.White, MaterialTheme.typography.labelMedium)
            }
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                model.copy.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                model.lead?.let { "${it.track.title} · ${it.track.artist}" } ?: model.copy.lead,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.82f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 2).forEach { index ->
                    DiscoveryArtwork(model.card(index), Modifier.size(44.dp).clip(thumbShape).border(1.dp, Color.White.copy(alpha = 0.3f), thumbShape))
                }
                Spacer(Modifier.weight(1f))
                FilledIconButton(
                    onClick = model.onOpen,
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Color.Black)
                ) {
                    Icon(Icons.Rounded.PlayArrow, model.copy.start, Modifier.size(30.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MaterialExpressiveDiscoveryTeaser(model: DiscoveryTeaserModel, spec: HomeStyleSpec, animationsEnabled: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val galleryShapes: List<Shape> = listOf(
        MaterialShapes.Cookie4Sided.toShape(),
        MaterialShapes.Pill.toShape(),
        MaterialShapes.Clover4Leaf.toShape(),
        MaterialShapes.Arch.toShape()
    )
    val drift by rememberSlowSpin(animationsEnabled, 36_000)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StudioSectionHeader(HomeStyle.MaterialExpressive, spec, model.copy.title, featured = false, onPlayAll = null)
        Surface(
            color = lerp(scheme.surfaceContainerHigh, scheme.primaryContainer, 0.55f),
            contentColor = scheme.onPrimaryContainer,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = spec.inset).discoveryEntry(model)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DiscoveryArtwork(model.card(0), Modifier.size(64.dp).graphicsLayer { rotationZ = drift }.clip(galleryShapes[0]).graphicsLayer { rotationZ = -drift })
                        DiscoveryArtwork(model.card(1), Modifier.size(width = 44.dp, height = 64.dp).clip(galleryShapes[1]))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DiscoveryArtwork(model.card(2), Modifier.size(width = 44.dp, height = 52.dp).clip(galleryShapes[3]))
                        DiscoveryArtwork(model.card(3), Modifier.size(width = 64.dp, height = 52.dp).clip(galleryShapes[2]))
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = scheme.secondaryContainer, contentColor = scheme.onSecondaryContainer, shape = RoundedCornerShape(10.dp)) {
                        Box(Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                            DiscoveryLeadLine(model, scheme.onSecondaryContainer, MaterialTheme.typography.labelMedium)
                        }
                    }
                    Text(
                        model.lead?.track?.title ?: model.copy.lead,
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    FilledTonalButton(onClick = model.onOpen, shapes = ButtonDefaults.shapes(), modifier = Modifier.heightIn(min = 48.dp)) {
                        if (model.loading && model.lead == null) LevyraLoadingIndicator(Modifier.size(20.dp))
                        else Icon(Icons.Rounded.PlayArrow, null, Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                        Text(model.copy.start, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoveryHomeNotice(copy: DiscoveryCopy, inset: Dp, error: Boolean, handle: DiscoveryHomeHandle) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(horizontal = inset).clip(RoundedCornerShape(24.dp)).background(scheme.surfaceContainer).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Rounded.AutoAwesome, null, tint = scheme.primary)
        Column(Modifier.weight(1f)) {
            Text(copy.title, style = MaterialTheme.typography.titleMedium, color = LevyraText, modifier = Modifier.semantics { heading() })
            Text(if (error) copy.error else copy.empty, style = MaterialTheme.typography.bodySmall, color = LevyraMuted, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        TextButton(onClick = handle.onRetry) {
            Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (error) copy.retry else copy.refresh)
        }
    }
}
