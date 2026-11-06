package com.luc4n3x.levyra.ui.moodjourney

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.MoodTone
import com.luc4n3x.levyra.domain.MoodTone.Brooding
import com.luc4n3x.levyra.domain.MoodTone.Calm
import com.luc4n3x.levyra.domain.MoodTone.Driving
import com.luc4n3x.levyra.domain.MoodTone.Euphoric
import com.luc4n3x.levyra.domain.MoodTone.Flowing
import com.luc4n3x.levyra.domain.MoodTone.Melancholy
import com.luc4n3x.levyra.domain.MoodTone.Tender
import com.luc4n3x.levyra.domain.MoodTone.Tense
import com.luc4n3x.levyra.domain.MoodTone.Warm
import com.luc4n3x.levyra.ui.i18n.SmartMusicText
import com.luc4n3x.levyra.ui.theme.AppleMusicDesign
import com.luc4n3x.levyra.ui.theme.AtelierDesign
import com.luc4n3x.levyra.ui.theme.IvoryDesign
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraIsAppleMusic
import com.luc4n3x.levyra.ui.theme.LevyraIsAtelier
import com.luc4n3x.levyra.ui.theme.LevyraIsMono
import com.luc4n3x.levyra.ui.theme.MonoCodeFont
import com.luc4n3x.levyra.ui.theme.MonoDesign
import com.luc4n3x.levyra.ui.theme.LevyraIsIvory
import com.luc4n3x.levyra.ui.theme.LevyraIsNocturne
import com.luc4n3x.levyra.ui.theme.LevyraIsPrism
import com.luc4n3x.levyra.ui.theme.LevyraIsQobuz
import com.luc4n3x.levyra.ui.theme.LevyraIsSpotify
import com.luc4n3x.levyra.ui.theme.LevyraIsTorque
import com.luc4n3x.levyra.ui.theme.LevyraIsYouTubeMusic
import com.luc4n3x.levyra.ui.theme.Nocturne78Design
import com.luc4n3x.levyra.ui.theme.PrismDesign
import com.luc4n3x.levyra.ui.theme.QobuzDesign
import com.luc4n3x.levyra.ui.theme.SpotifyDesign
import com.luc4n3x.levyra.ui.theme.YouTubeMusicDesign
import com.materialkolor.blend.Blend
import com.materialkolor.hct.Hct

internal enum class MoodArcSkin { Expressive, Nocturne, Prism, Torque, Ivory, Spotify, Apple, YouTube, Qobuz, Atelier, Mono }

internal fun activeMoodArcSkin(): MoodArcSkin = when {
    LevyraIsTorque -> MoodArcSkin.Torque
    LevyraIsIvory -> MoodArcSkin.Ivory
    LevyraIsSpotify -> MoodArcSkin.Spotify
    LevyraIsAppleMusic -> MoodArcSkin.Apple
    LevyraIsYouTubeMusic -> MoodArcSkin.YouTube
    LevyraIsQobuz -> MoodArcSkin.Qobuz
    LevyraIsAtelier -> MoodArcSkin.Atelier
    LevyraIsMono -> MoodArcSkin.Mono
    LevyraIsPrism -> MoodArcSkin.Prism
    LevyraIsNocturne -> MoodArcSkin.Nocturne
    else -> MoodArcSkin.Expressive
}

internal class MoodArcStyle(
    val skin: MoodArcSkin,
    val panelShape: Shape,
    val rowShapes: Pair<Shape, Shape>,
    val coverShape: Shape,
    val stroke: Dp,
    val dot: Dp,
    val squareDots: Boolean,
    val uppercase: Boolean,
    val numerals: FontFamily?,
    val ramp: List<Color>
) {
    fun toneColor(energy: Float, valence: Float): Color {
        val e = ((energy.coerceIn(0f, 1f) - 0.15f) / 0.75f).coerceIn(0f, 1f)
        val t = (e + (valence.coerceIn(0f, 1f) - 0.5f) * 0.16f).coerceIn(0f, 1f) * (ramp.size - 1)
        val index = t.toInt().coerceAtMost(ramp.size - 2)
        return lerp(ramp[index], ramp[index + 1], t - index)
    }

    fun label(text: String): String = if (uppercase) text.uppercase() else text
}

internal fun moodToneLabel(tone: MoodTone): SmartMusicText = when (tone) {
    Melancholy -> SmartMusicText.ToneMelancholy
    Calm -> SmartMusicText.ToneCalm
    Tender -> SmartMusicText.ToneTender
    Brooding -> SmartMusicText.ToneBrooding
    Flowing -> SmartMusicText.ToneFlowing
    Warm -> SmartMusicText.ToneWarm
    Tense -> SmartMusicText.ToneTense
    Driving -> SmartMusicText.ToneDriving
    Euphoric -> SmartMusicText.ToneEuphoric
}

private val ExpressiveHues = floatArrayOf(232f, 282f, 345f, 40f)

private fun hct(hue: Float, chroma: Double, tone: Double): Color =
    Color(Hct.from(hue.toDouble(), chroma, tone).toInt())

private fun harmonized(color: Color, source: Color): Color = Color(Blend.harmonize(color.toArgb(), source.toArgb()))

private fun accentRamp(accent: Color, rest: Color): List<Color> =
    listOf(lerp(rest, accent, 0.18f), lerp(rest, accent, 0.5f), lerp(rest, accent, 0.78f), accent)

@Composable
internal fun rememberMoodArcStyle(): MoodArcStyle {
    val skin = activeMoodArcSkin()
    val scheme = MaterialTheme.colorScheme
    val palette = LevyraActivePalette
    val atelierAccent = if (skin == MoodArcSkin.Atelier) AtelierDesign.accent else Color.Unspecified
    return remember(skin, scheme, palette, atelierAccent) {
        val light = palette.isLight
        val organicA = RoundedCornerShape(topStart = 26.dp, topEnd = 12.dp, bottomEnd = 26.dp, bottomStart = 12.dp)
        val organicB = RoundedCornerShape(topStart = 12.dp, topEnd = 26.dp, bottomEnd = 12.dp, bottomStart = 26.dp)
        when (skin) {
            MoodArcSkin.Expressive -> {
                val tone = if (light) 50.0 else 72.0
                MoodArcStyle(
                    skin = skin,
                    panelShape = RoundedCornerShape(topStart = 40.dp, topEnd = 20.dp, bottomEnd = 40.dp, bottomStart = 20.dp),
                    rowShapes = organicA to organicB,
                    coverShape = RoundedCornerShape(16.dp),
                    stroke = 5.dp,
                    dot = 5.dp,
                    squareDots = false,
                    uppercase = false,
                    numerals = null,
                    ramp = ExpressiveHues.map { harmonized(hct(it, 64.0, tone), scheme.primary) }
                )
            }
            MoodArcSkin.Nocturne -> MoodArcStyle(
                skin = skin,
                panelShape = RoundedCornerShape(Nocturne78Design.panelCorner),
                rowShapes = RoundedCornerShape(Nocturne78Design.cardCorner).let { it to it },
                coverShape = RoundedCornerShape(Nocturne78Design.artworkCorner),
                stroke = 3.dp,
                dot = 4.dp,
                squareDots = true,
                uppercase = true,
                numerals = FontFamily.Monospace,
                ramp = listOf(palette.blue, palette.cyan, palette.violet, palette.pink)
            )
            MoodArcSkin.Prism -> MoodArcStyle(
                skin = skin,
                panelShape = PrismDesign.cut(18.dp),
                rowShapes = PrismDesign.cut(12.dp).let { it to it },
                coverShape = PrismDesign.cut(10.dp),
                stroke = 4.dp,
                dot = 4.5.dp,
                squareDots = false,
                uppercase = false,
                numerals = null,
                ramp = listOf(palette.cyan, palette.blue, palette.violet, palette.pink, palette.orange)
            )
            MoodArcSkin.Torque -> MoodArcStyle(
                skin = skin,
                panelShape = RoundedCornerShape(2.dp),
                rowShapes = RoundedCornerShape(2.dp).let { it to it },
                coverShape = RoundedCornerShape(0.dp),
                stroke = 2.5.dp,
                dot = 3.5.dp,
                squareDots = true,
                uppercase = true,
                numerals = FontFamily.Monospace,
                ramp = listOf(palette.blue, lerp(palette.blue, palette.orange, 0.5f), palette.orange)
            )
            MoodArcSkin.Ivory -> MoodArcStyle(
                skin = skin,
                panelShape = RoundedCornerShape(IvoryDesign.surfaceCorner),
                rowShapes = RoundedCornerShape(IvoryDesign.tileCorner).let { it to it },
                coverShape = RoundedCornerShape(IvoryDesign.controlCorner),
                stroke = 3.dp,
                dot = 4.5.dp,
                squareDots = false,
                uppercase = false,
                numerals = null,
                ramp = accentRamp(IvoryDesign.ink, palette.muted)
            )
            MoodArcSkin.Spotify -> MoodArcStyle(
                skin = skin,
                panelShape = RoundedCornerShape(SpotifyDesign.cornerLarge),
                rowShapes = RoundedCornerShape(SpotifyDesign.corner).let { it to it },
                coverShape = RoundedCornerShape(SpotifyDesign.cornerSmall),
                stroke = 3.5.dp,
                dot = 4.dp,
                squareDots = false,
                uppercase = false,
                numerals = null,
                ramp = accentRamp(SpotifyDesign.green, scheme.onSurfaceVariant)
            )
            MoodArcSkin.Apple -> MoodArcStyle(
                skin = skin,
                panelShape = RoundedCornerShape(AppleMusicDesign.cornerXl),
                rowShapes = RoundedCornerShape(AppleMusicDesign.cornerL).let { it to it },
                coverShape = RoundedCornerShape(AppleMusicDesign.cornerM),
                stroke = 3.dp,
                dot = 4.dp,
                squareDots = false,
                uppercase = false,
                numerals = null,
                ramp = accentRamp(AppleMusicDesign.accent, scheme.onSurfaceVariant)
            )
            MoodArcSkin.YouTube -> MoodArcStyle(
                skin = skin,
                panelShape = RoundedCornerShape(YouTubeMusicDesign.discoveryCorner),
                rowShapes = RoundedCornerShape(YouTubeMusicDesign.cardCorner).let { it to it },
                coverShape = RoundedCornerShape(YouTubeMusicDesign.artworkCorner),
                stroke = 3.dp,
                dot = 4.dp,
                squareDots = false,
                uppercase = false,
                numerals = null,
                ramp = accentRamp(YouTubeMusicDesign.red, scheme.onSurfaceVariant)
            )
            MoodArcSkin.Qobuz -> MoodArcStyle(
                skin = skin,
                panelShape = RoundedCornerShape(4.dp),
                rowShapes = RoundedCornerShape(4.dp).let { it to it },
                coverShape = RoundedCornerShape(2.dp),
                stroke = 2.5.dp,
                dot = 3.5.dp,
                squareDots = false,
                uppercase = true,
                numerals = null,
                ramp = accentRamp(QobuzDesign.textAccent, scheme.onSurfaceVariant)
            )
            MoodArcSkin.Atelier -> MoodArcStyle(
                skin = skin,
                panelShape = RoundedCornerShape(6.dp),
                rowShapes = RoundedCornerShape(6.dp).let { it to it },
                coverShape = RoundedCornerShape(4.dp),
                stroke = 2.5.dp,
                dot = 3.5.dp,
                squareDots = true,
                uppercase = true,
                numerals = FontFamily.Monospace,
                ramp = accentRamp(atelierAccent, AtelierDesign.textFaint)
            )
            MoodArcSkin.Mono -> MoodArcStyle(
                skin = skin,
                panelShape = MonoDesign.panelShape,
                rowShapes = MonoDesign.cardShape.let { it to it },
                coverShape = MonoDesign.artShape,
                stroke = 2.dp,
                dot = 3.5.dp,
                squareDots = false,
                uppercase = true,
                numerals = MonoCodeFont,
                ramp = accentRamp(MonoDesign.accent, MonoDesign.textFaint)
            )
        }
    }
}
