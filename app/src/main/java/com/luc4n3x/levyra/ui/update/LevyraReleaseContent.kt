package com.luc4n3x.levyra.ui.update

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.updateReleaseCopy
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
import com.luc4n3x.levyra.update.formatUpdateBytes
import com.luc4n3x.levyra.update.updateUnitsFor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val BrandCyan = Color(0xFF38C8FF)
private val BrandBlue = Color(0xFF2F62FF)
private val BrandViolet = Color(0xFF7A3BFF)
private val BrandMagenta = Color(0xFFE23DF2)
private val BrandNight = Color(0xFF0B0A18)
private const val BrandArtAspect = 1448f / 1086f
private const val SpectrumBars = 56
private const val SpectrumRiseMillis = 1_700
private const val SpectrumBreathMillis = 7_000
private const val SheenDelayMillis = 650L
private const val SheenMillis = 1_500
private const val DigitRollMillis = 1_050
private const val DigitRollStagger = 140
private val SettleEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
private val SheenEasing = CubicBezierEasing(0.45f, 0f, 0.2f, 1f)

internal data class ReleasePalette(
    val background: Color,
    val accent: Color,
    val onAccent: Color,
    val text: Color,
    val muted: Color,
    val panel: Color,
    val border: Color
)

private val LocalReleasePalette = staticCompositionLocalOf<ReleasePalette> { error("ReleasePalette is required") }

@Composable
internal fun LevyraReleaseContent(
    version: String,
    notes: List<String>,
    meta: String,
    strings: LevyraStrings,
    animated: Boolean,
    palette: ReleasePalette,
    protectionDetail: String,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    brandArt: @Composable (Modifier) -> Unit,
    backButton: @Composable (Modifier) -> Unit,
    updateIcon: @Composable (Modifier) -> Unit,
    protectionIcon: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    bannerInsets: Modifier = Modifier,
    contentInsets: Modifier = Modifier,
    maxContentWidth: Dp = Dp.Unspecified,
    statusFooter: (@Composable () -> Unit)? = null
) {
    val copy = strings.updateReleaseCopy()
    CompositionLocalProvider(LocalReleasePalette provides palette) {
        BoxWithConstraints(
            modifier = modifier.fillMaxSize().background(palette.background),
            contentAlignment = Alignment.TopCenter
        ) {
            val compact = maxHeight < 700.dp
            val bannerHeight = (maxHeight * 0.36f).coerceIn(176.dp, 300.dp)
            Column(modifier = Modifier.fillMaxSize().then(contentInsets)) {
                Box(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        UpdateBanner(
                            animated = animated,
                            seed = version,
                            brandArt = brandArt,
                            insets = bannerInsets,
                            modifier = Modifier.height(bannerHeight)
                        )
                        Column(modifier = Modifier.widthIn(max = maxContentWidth).fillMaxWidth().padding(horizontal = 24.dp)) {
                            UpdateTitleBlock(copy.releaseReady, version, meta, animated, compact)
                            if (notes.isNotEmpty()) {
                                Spacer(Modifier.height(if (compact) 20.dp else 28.dp))
                                UpdateTracklist(copy.releaseHighlights, notes, animated, compact)
                            }
                            Spacer(Modifier.height(16.dp))
                            UpdateProtectionLine(copy.releaseProtection, protectionDetail, protectionIcon)
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                    if (statusFooter == null) {
                        backButton(bannerInsets.padding(start = 12.dp, top = 8.dp))
                    }
                }

                Column(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                        .widthIn(max = maxContentWidth).fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 10.dp, bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (statusFooter == null) {
                        ReleaseActions(strings, onUpdate, onLater, updateIcon, compact)
                    } else {
                        statusFooter()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ReleaseActions(
    strings: LevyraStrings,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    updateIcon: @Composable (Modifier) -> Unit,
    compact: Boolean
) {
    val palette = LocalReleasePalette.current
    Button(
        onClick = onUpdate,
        shapes = ButtonDefaults.shapes(),
        colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent),
        contentPadding = PaddingValues(horizontal = 28.dp),
        modifier = Modifier.fillMaxWidth().height(if (compact) 56.dp else 64.dp)
    ) {
        updateIcon(Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(strings.update, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
    TextButton(
        onClick = onLater,
        shapes = ButtonDefaults.shapes(),
        modifier = Modifier.height(48.dp)
    ) {
        Text(strings.later, color = palette.muted, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun UpdateBanner(
    animated: Boolean,
    seed: String,
    brandArt: @Composable (Modifier) -> Unit,
    insets: Modifier,
    modifier: Modifier = Modifier
) {
    val palette = LocalReleasePalette.current
    val rise = remember { Animatable(if (animated) 0f else 1f) }
    val sheen = remember { Animatable(if (animated) 0f else 1f) }
    LaunchedEffect(animated) {
        if (!animated) {
            rise.snapTo(1f)
            sheen.snapTo(1f)
            return@LaunchedEffect
        }
        launch { rise.animateTo(1f, tween(SpectrumRiseMillis, easing = LinearEasing)) }
        delay(SheenDelayMillis)
        sheen.animateTo(1f, tween(SheenMillis, easing = SheenEasing))
    }
    val breath = if (animated) {
        rememberInfiniteTransition(label = "update-spectrum").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(SpectrumBreathMillis, easing = LinearEasing), RepeatMode.Restart),
            label = "update-spectrum-breath"
        )
    } else {
        null
    }
    val profile = remember(seed) { spectrumProfile(seed) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .drawWithCache {
                val width = size.width
                val height = size.height
                val sky = Brush.verticalGradient(0f to BrandNight, 0.62f to BrandNight.copy(alpha = 0.6f), 1f to palette.background)
                val halo = Brush.radialGradient(
                    colors = listOf(BrandViolet.copy(alpha = 0.20f), BrandBlue.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(width / 2f, height * 0.5f),
                    radius = width * 0.6f
                )
                val bars = Brush.horizontalGradient(listOf(BrandCyan, BrandBlue, BrandViolet, BrandMagenta))
                val floor = Brush.verticalGradient(0.66f to Color.Transparent, 1f to palette.background)
                val span = width * 0.9f
                val step = span / SpectrumBars
                val stroke = (step * 0.34f).coerceIn(1.dp.toPx(), 3.dp.toPx())
                val left = (width - span) / 2f + step / 2f
                val axis = height * 0.6f
                val reach = height * 0.24f
                val half = (SpectrumBars - 1) / 2f
                onDrawBehind {
                    drawRect(sky)
                    drawRect(halo)
                    val progress = rise.value
                    val phase = (breath?.value ?: 0f) * 2f * PI.toFloat()
                    for (index in 0 until SpectrumBars) {
                        val distance = abs(index - half) / half
                        val local = ((progress - distance * 0.4f) / 0.6f).coerceIn(0f, 1f)
                        if (local <= 0f) continue
                        val settled = SettleEasing.transform(local)
                        val idle = 1f + 0.07f * sin(phase + index * 0.41f)
                        val amplitude = reach * profile[index] * settled * idle
                        val x = left + index * step
                        drawLine(
                            brush = bars,
                            start = Offset(x, axis - amplitude),
                            end = Offset(x, axis + amplitude * 0.42f),
                            strokeWidth = stroke,
                            cap = StrokeCap.Round,
                            alpha = 0.34f * settled
                        )
                    }
                    drawRect(floor)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        brandArt(
            insets
                .padding(top = 4.dp, bottom = 18.dp)
                .fillMaxHeight(0.78f)
                .aspectRatio(BrandArtAspect)
                .graphicsLayer {
                    val appear = (rise.value / 0.45f).coerceIn(0f, 1f)
                    alpha = SettleEasing.transform(appear)
                    translationY = (1f - SettleEasing.transform(appear)) * 10.dp.toPx()
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    val value = sheen.value
                    if (value > 0f && value < 1f) {
                        val center = -size.width * 0.3f + size.width * 1.6f * value
                        val band = size.width * 0.22f
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.42f), Color.Transparent),
                                start = Offset(center - band, 0f),
                                end = Offset(center + band, size.height)
                            ),
                            blendMode = BlendMode.SrcAtop
                        )
                    }
                }
        )
    }
}

private fun spectrumProfile(seed: String): FloatArray {
    val random = java.util.Random(seed.hashCode().toLong())
    val raw = FloatArray(SpectrumBars) { 0.35f + 0.65f * random.nextFloat() }
    return FloatArray(SpectrumBars) { index ->
        val smoothed = (raw[(index - 1).coerceAtLeast(0)] + raw[index] * 2f + raw[(index + 1).coerceAtMost(SpectrumBars - 1)]) / 4f
        val position = index / (SpectrumBars - 1f)
        val envelope = sin(position * PI.toFloat()).pow(1.6f)
        (envelope * smoothed).coerceIn(0.05f, 1f)
    }
}

@Composable
private fun UpdateTitleBlock(
    eyebrow: String,
    version: String,
    meta: String,
    animated: Boolean,
    compact: Boolean
) {
    val palette = LocalReleasePalette.current
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(palette.accent, RoundedCornerShape(50))
            )
            Text(
                text = eyebrow.uppercase(),
                color = palette.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(4.dp))
        BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val density = LocalDensity.current
            val ceiling = if (compact) 52f else 64f
            val fontSize = with(density) {
                (maxWidth.toPx() / (version.length.coerceAtLeast(1) * 0.64f)).toSp().value.coerceIn(32f, ceiling).sp
            }
            RollingVersion(
                version = version,
                style = TextStyle(
                    color = palette.text,
                    fontSize = fontSize,
                    lineHeight = LevyraTypeRhythm.lineHeight(fontSize),
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-2).sp,
                    fontFeatureSettings = "tnum"
                ),
                animated = animated
            )
        }
        if (meta.isNotBlank()) {
            Text(meta, color = palette.muted, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun RollingVersion(version: String, style: TextStyle, animated: Boolean) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = version
                heading()
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            var digitIndex = 0
            version.forEach { char ->
                if (char.isDigit()) {
                    RollingDigit(target = char.digitToInt(), order = digitIndex++, style = style, animated = animated)
                } else {
                    Text(char.toString(), style = style, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun RollingDigit(target: Int, order: Int, style: TextStyle, animated: Boolean) {
    val end = 10f + target
    val roll = remember { Animatable(if (animated) 0f else end) }
    LaunchedEffect(target, animated) {
        if (animated) {
            roll.animateTo(end, tween(DigitRollMillis + order * DigitRollStagger, easing = SettleEasing))
        } else {
            roll.snapTo(end)
        }
    }
    val whole by remember { derivedStateOf { floor(roll.value).toInt() } }
    Box(modifier = Modifier.clipToBounds()) {
        Text(
            text = (whole % 10).toString(),
            style = style,
            maxLines = 1,
            modifier = Modifier.graphicsLayer {
                val fraction = roll.value - floor(roll.value)
                translationY = -fraction * size.height
                alpha = 1f - fraction * 0.6f
            }
        )
        Text(
            text = ((whole + 1) % 10).toString(),
            style = style,
            maxLines = 1,
            modifier = Modifier.graphicsLayer {
                val fraction = roll.value - floor(roll.value)
                translationY = (1f - fraction) * size.height
                alpha = if (fraction == 0f) 0f else 0.4f + fraction * 0.6f
            }
        )
    }
}

@Composable
private fun UpdateTracklist(
    title: String,
    notes: List<String>,
    animated: Boolean,
    compact: Boolean
) {
    val palette = LocalReleasePalette.current
    val rowPadding: Dp = if (compact) 11.dp else 14.dp
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = palette.text,
            fontSize = if (compact) 19.sp else 21.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.3).sp,
            modifier = Modifier
                .padding(start = 4.dp, bottom = 10.dp)
                .semantics { heading() }
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.panel.copy(alpha = 0.72f), RoundedCornerShape(26.dp))
                .padding(vertical = 4.dp)
        ) {
            notes.forEachIndexed { index, note ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            if (index > 0) {
                                drawLine(
                                    color = palette.border,
                                    start = Offset(62.dp.toPx(), 0f),
                                    end = Offset(size.width - 20.dp.toPx(), 0f),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                        }
                        .padding(start = 18.dp, end = 20.dp, top = rowPadding, bottom = rowPadding),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(modifier = Modifier.size(width = 30.dp, height = 21.dp), contentAlignment = Alignment.CenterStart) {
                        if (index == 0) {
                            NowPlayingBars(animated = animated)
                        } else {
                            Text(
                                text = (index + 1).toString().padStart(2, '0'),
                                color = palette.muted,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                style = TextStyle(fontFeatureSettings = "tnum")
                            )
                        }
                    }
                    Text(
                        text = note,
                        color = if (index == 0) palette.text else palette.text.copy(alpha = 0.84f),
                        fontSize = if (compact) 14.sp else 15.sp,
                        lineHeight = if (compact) 19.sp else 21.sp,
                        fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun NowPlayingBars(animated: Boolean) {
    val palette = LocalReleasePalette.current
    val phase = if (animated) {
        rememberInfiniteTransition(label = "update-bars").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(1_400, easing = LinearEasing), RepeatMode.Restart),
            label = "update-bars-phase"
        )
    } else {
        null
    }
    Box(
        modifier = Modifier
            .size(width = 18.dp, height = 16.dp)
            .drawBehind {
                val bar = 3.5.dp.toPx()
                val gap = (size.width - bar * 3f) / 2f
                val t = (phase?.value ?: 0.3f) * 2f * PI.toFloat()
                for (index in 0 until 3) {
                    val level = 0.35f + 0.65f * (0.5f + 0.5f * sin(t + index * 2.1f))
                    val height = size.height * level
                    drawRoundRect(
                        color = palette.accent,
                        topLeft = Offset(index * (bar + gap), size.height - height),
                        size = Size(bar, height),
                        cornerRadius = CornerRadius(bar / 2f)
                    )
                }
            }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UpdateProtectionLine(title: String, detail: String, protectionIcon: @Composable (Modifier) -> Unit) {
    val palette = LocalReleasePalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(palette.accent.copy(alpha = 0.14f), MaterialShapes.Clover4Leaf.toShape()),
            contentAlignment = Alignment.Center
        ) {
            protectionIcon(Modifier.size(19.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = palette.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = palette.muted, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

internal fun updateMetaLine(publishedAtEpochMs: Long, assetSizeBytes: Long, languageCode: String): String {
    val parts = mutableListOf<String>()
    formatUpdateReleaseDate(publishedAtEpochMs, languageCode)?.let(parts::add)
    if (assetSizeBytes > 0L) parts += formatUpdateBytes(assetSizeBytes, updateUnitsFor(languageCode))
    return parts.joinToString(separator = " · ")
}

internal fun formatUpdateReleaseDate(publishedAtEpochMs: Long, languageCode: String): String? {
    if (publishedAtEpochMs <= 0L) return null
    return runCatching {
        val locale = Locale.forLanguageTag(languageCode.ifBlank { "en" })
        DateTimeFormatter.ofPattern("d MMM yyyy", locale)
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(publishedAtEpochMs))
    }.getOrNull()
}
