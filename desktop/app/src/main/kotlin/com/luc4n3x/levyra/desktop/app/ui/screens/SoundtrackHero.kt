package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.desktop.app.ui.components.Artwork
import com.luc4n3x.levyra.desktop.app.ui.components.CookieShape
import com.luc4n3x.levyra.desktop.app.ui.components.rememberArtworkRequest
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.ui.theme.ArtworkPalette
import com.luc4n3x.levyra.desktop.app.ui.theme.LevyraBrand
import java.util.Locale

@Composable
internal fun SoundtrackHero(
    overline: String,
    overlineHighlight: String,
    title: String,
    lead: String,
    portraitUrl: String,
    fallbackArtworkUrl: String,
    companionArtworkUrls: List<String>,
    playing: Boolean,
    loading: Boolean,
    playLabel: String,
    enabled: Boolean,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canvas = MaterialTheme.colorScheme.surfaceContainerLowest
    var portraitFailed by remember(portraitUrl) { mutableStateOf(false) }
    val artworkUrl = portraitUrl.takeUnless { portraitFailed || it.isBlank() }.orEmpty().ifBlank { fallbackArtworkUrl }
    val portraitTint by produceState(LevyraBrand.cyan, artworkUrl) {
        value = ArtworkPalette.accentFor(artworkUrl) ?: LevyraBrand.cyan
    }
    val glow by animateColorAsState(portraitTint, tween(500))
    val portrait = rememberArtworkRequest(artworkUrl)
    val portraitAlpha by animateFloatAsState(if (portrait != null) 1f else 0f, tween(420))

    Box(modifier = modifier.fillMaxWidth().height(HeroHeight).clipToBounds()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawRect(
                        Brush.radialGradient(
                            listOf(glow.copy(alpha = 0.30f), Color.Transparent),
                            center = Offset(size.width * 0.78f, size.height * 0.38f),
                            radius = size.width * 0.62f
                        )
                    )
                    drawContent()
                    drawRect(
                        Brush.verticalGradient(0f to Color.Black, 0.58f to Color.Black, 1f to Color.Transparent),
                        blendMode = BlendMode.DstIn
                    )
                }
        ) {
            if (portrait != null) {
                AsyncImage(
                    model = portrait,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = BiasAlignment(0f, -0.92f),
                    onError = { if (artworkUrl == portraitUrl) portraitFailed = true },
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxWidth(0.82f).fillMaxHeight().clipToBounds()
                        .graphicsLayer {
                            alpha = portraitAlpha
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                Brush.horizontalGradient(0f to Color.Transparent, 0.54f to Color.Black, 1f to Color.Black),
                                blendMode = BlendMode.DstIn
                            )
                        }
                )
            }
            Box(
                Modifier.fillMaxSize().drawBehind {
                    drawRect(Brush.horizontalGradient(0f to canvas.copy(alpha = 0.82f), 0.5f to canvas.copy(alpha = 0.2f), 1f to Color.Transparent))
                }
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.62f)
                .padding(start = HeroInset, bottom = 34.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = remember(overline, overlineHighlight) { highlighted(overline.uppercase(Locale.ROOT), overlineHighlight.uppercase(Locale.ROOT)) },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (companionArtworkUrls.isNotEmpty()) CompanionStack(companionArtworkUrls, ring = canvas)
                Text(
                    text = lead,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        HeroPlayButton(
            playing = playing,
            loading = loading,
            enabled = enabled,
            label = playLabel,
            onClick = onPlay,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = HeroInset, bottom = 30.dp)
        )
    }
}

@Composable
private fun CompanionStack(urls: List<String>, ring: Color) {
    val visible = urls.take(3)
    Box(Modifier.width(28.dp + 18.dp * (visible.size - 1)).height(28.dp)) {
        visible.forEachIndexed { index, url ->
            Artwork(
                url = url,
                cornerRadius = 14.dp,
                iconSize = 12.dp,
                modifier = Modifier
                    .offset(x = 18.dp * index)
                    .size(28.dp)
                    .border(2.dp, ring, CircleShape)
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
private fun HeroPlayButton(
    playing: Boolean,
    loading: Boolean,
    enabled: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val scale by animateFloatAsState(if (hovered && enabled) 1.04f else 1f, tween(160))
    Box(
        modifier = modifier
            .size(84.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CookieShape)
            .background(if (enabled) LevyraBrand.soft else MaterialTheme.colorScheme.surfaceContainerHighest)
            .hoverable(interaction)
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(26.dp), color = LevyraBrand.onSoft, strokeWidth = 2.5.dp)
        } else {
            Icon(
                imageVector = if (playing) LevyraIcons.Pause else LevyraIcons.Play,
                contentDescription = label,
                tint = LevyraBrand.onSoft,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

private fun highlighted(text: String, highlight: String): AnnotatedString = buildAnnotatedString {
    append(text)
    if (highlight.isBlank()) return@buildAnnotatedString
    val start = text.lastIndexOf(highlight)
    if (start >= 0) addStyle(SpanStyle(color = LevyraBrand.soft), start, start + highlight.length)
}

private val HeroHeight = 360.dp
private val HeroInset = 32.dp
