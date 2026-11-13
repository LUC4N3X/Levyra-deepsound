package com.luc4n3x.levyra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraType

private val GenreCoverShade = Brush.verticalGradient(
    0.42f to Color.Transparent,
    0.78f to Color.Black.copy(alpha = 0.42f),
    1f to Color.Black.copy(alpha = 0.7f)
)
private val GenreCoverPhotoGrade = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.94f) })
private val GenreCoverTextShadow = Shadow(color = Color.Black.copy(alpha = 0.5f), offset = Offset(0f, 2f), blurRadius = 12f)

internal val GenreChartCoverShape = RoundedCornerShape(16.dp)

internal fun genreChartTitleScale(title: String): Float {
    val longestWord = title.split(' ', '-', '&').maxOfOrNull { it.length } ?: 0
    return when {
        longestWord > 11 || title.length > 22 -> 0.085f
        longestWord > 8 || title.length > 14 -> 0.1f
        else -> 0.125f
    }
}

@Composable
internal fun GenreChartCover(
    genreKey: String,
    genreTitle: String,
    accent: Color,
    artworkUrl: String?,
    kicker: String,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = GenreChartCoverShape
) {
    val description = "$kicker $genreTitle"
    val highlight = remember(genreKey, accent) { lerp(accent, Color.White, 0.16f) }
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Image
            }
    ) {
        val unit = maxWidth
        val density = LocalDensity.current
        GenreChartArtwork(accent = accent, artworkUrl = artworkUrl, modifier = Modifier.fillMaxSize())
        val titleSize = with(density) { (unit * 0.125f).toSp() }
        val genreSize = with(density) { (unit * genreChartTitleScale(genreTitle)).toSp() }
        val titleStyle = LevyraType.heroTitle.copy(
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 1.04.em,
            letterSpacing = (-0.015).em,
            textAlign = TextAlign.Center,
            shadow = GenreCoverTextShadow
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = unit * 0.06f, end = unit * 0.06f, bottom = unit * 0.08f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BasicText(
                text = genreTitle,
                style = titleStyle.copy(fontSize = genreSize),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            BasicText(
                text = kicker,
                style = titleStyle.copy(color = highlight, fontSize = titleSize),
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun GenreChartArtwork(
    accent: Color,
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    photoModifier: Modifier = Modifier
) {
    val deep = remember(accent) { lerp(accent, LevyraBlack, 0.62f) }
    val photo = artworkUrl?.trim()?.takeIf(String::isNotEmpty)
    Box(modifier = modifier.background(Brush.linearGradient(listOf(lerp(accent, LevyraBlack, 0.35f), deep)))) {
        if (photo != null) {
            AsyncImage(
                model = photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = GenreCoverPhotoGrade,
                modifier = Modifier.fillMaxSize().then(photoModifier)
            )
        }
        Box(modifier = Modifier.fillMaxSize().background(GenreCoverShade))
    }
}
