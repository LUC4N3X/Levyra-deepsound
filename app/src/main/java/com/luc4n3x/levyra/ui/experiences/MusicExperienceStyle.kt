package com.luc4n3x.levyra.ui.experiences

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.data.ArtworkPalette
import com.luc4n3x.levyra.data.ArtworkToneMapper
import com.luc4n3x.levyra.ui.artwork.rememberArtworkPalette
import com.luc4n3x.levyra.ui.insights.ListeningReceiptModel
import com.luc4n3x.levyra.ui.theme.*

internal data class MusicExperienceNavigation(val rewind: () -> Unit = {}, val health: () -> Unit = {})
internal val LocalMusicExperiences = staticCompositionLocalOf { MusicExperienceNavigation() }

internal data class MusicExperienceStyle(
    val background: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val companion: Color,
    val surface: Color,
    val shape: Shape,
    val expressive: Boolean
) {
    val onAccent: Color get() = if (accent.luminance() > 0.179f) Color.Black else Color.White
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun musicExperienceStyle(artwork: String = ""): MusicExperienceStyle {
    val fallback = ArtworkPalette(LevyraCyan.toArgb(), LevyraViolet.toArgb())
    val palette by rememberArtworkPalette("rewind:$artwork", artwork, fallback)
    val expressive = !LevyraSkinManagesLayout
    val rawAccent = if (expressive && artwork.isNotBlank()) Color(palette.start) else LevyraCyan
    val light = LevyraInk.luminance() > 0.5f
    val accent = if (androidx.core.graphics.ColorUtils.calculateContrast(rawAccent.toArgb(), LevyraInk.toArgb()) >= 4.5) rawAccent else Color(
        ArtworkToneMapper.withToneRange(rawAccent.toArgb(), if (light) 25.0 else 72.0, if (light) 35.0 else 82.0)
    )
    return MusicExperienceStyle(
        LevyraInk, LevyraText, LevyraMuted, accent,
        if (expressive) Color(palette.end) else LevyraViolet, LevyraPanel,
        when {
            expressive -> MaterialShapes.Puffy.toShape()
            LevyraIsIvory -> RoundedCornerShape(IvoryDesign.tileCorner)
            LevyraIsTorque -> PrismDesign.cut(2.dp)
            LevyraIsPrism && !LevyraIsSpotify && !LevyraIsAppleMusic && !LevyraIsYouTubeMusic -> PrismDesign.cut(PrismDesign.artworkCorner)
            else -> RoundedCornerShape(LevyraHomeDesign.ArtworkCorner)
        }, expressive
    )
}

@Composable
internal fun ExperienceArtwork(url: String, title: String, style: MusicExperienceStyle, modifier: Modifier, shape: Shape = style.shape) {
    Box(modifier.clip(shape).background(Brush.linearGradient(listOf(style.accent, style.companion))), contentAlignment = Alignment.Center) {
        Text(ListeningReceiptModel.initial(title), color = style.onAccent, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
        if (url.isNotBlank()) AsyncImage(url, null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}
