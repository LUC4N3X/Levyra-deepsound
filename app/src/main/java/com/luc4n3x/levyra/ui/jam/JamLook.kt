package com.luc4n3x.levyra.ui.jam

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.theme.AppleMusicDesign
import com.luc4n3x.levyra.ui.theme.AtelierDesign
import com.luc4n3x.levyra.ui.theme.IvoryDesign
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraGlassBorder
import com.luc4n3x.levyra.ui.theme.LevyraIsAppleMusic
import com.luc4n3x.levyra.ui.theme.LevyraIsAtelier
import com.luc4n3x.levyra.ui.theme.LevyraIsIvory
import com.luc4n3x.levyra.ui.theme.LevyraIsMono
import com.luc4n3x.levyra.ui.theme.LevyraIsNocturne
import com.luc4n3x.levyra.ui.theme.LevyraIsPrism
import com.luc4n3x.levyra.ui.theme.LevyraIsQobuz
import com.luc4n3x.levyra.ui.theme.LevyraIsSpotify
import com.luc4n3x.levyra.ui.theme.LevyraIsTorque
import com.luc4n3x.levyra.ui.theme.LevyraIsYouTubeMusic
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraOnAccent
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPanelSoft
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.MonoDesign
import com.luc4n3x.levyra.ui.theme.Nocturne78Design
import com.luc4n3x.levyra.ui.theme.PrismDesign
import com.luc4n3x.levyra.ui.theme.QobuzDesign
import com.luc4n3x.levyra.ui.theme.SpotifyDesign
import com.luc4n3x.levyra.ui.theme.YouTubeMusicDesign

internal class JamLook(
    val skinned: Boolean,
    val background: Color,
    val card: Color,
    val border: Color,
    val cardShape: Shape,
    val rowShape: Shape,
    val selectedRow: Color,
    val accent: Color,
    val onAccent: Color,
    val mark: Color,
    val text: Color,
    val textSecondary: Color,
    val buttonShape: Shape,
    val avatarShape: Shape,
    val titleWeight: FontWeight,
    val displayWeight: FontWeight,
    val uppercaseLabels: Boolean
)

internal val LocalJamLook = staticCompositionLocalOf { DefaultJamLook }

private val DefaultJamLook = JamLook(
    skinned = false, background = Color.Black, card = Color.Black, border = Color.Transparent,
    cardShape = RoundedCornerShape(28.dp), rowShape = RoundedCornerShape(16.dp), selectedRow = Color.Black,
    accent = Color.White, onAccent = Color.Black, mark = Color.White, text = Color.White, textSecondary = Color.White,
    buttonShape = CircleShape, avatarShape = CircleShape, titleWeight = FontWeight.Bold, displayWeight = FontWeight.Black,
    uppercaseLabels = false
)

@Composable
internal fun jamLookForActiveSkin(): JamLook = when {
    LevyraIsAtelier -> JamLook(
        skinned = true, background = AtelierDesign.room, card = AtelierDesign.cabinet, border = AtelierDesign.hairlineStrong,
        cardShape = AtelierDesign.panelShape, rowShape = RoundedCornerShape(10.dp), selectedRow = AtelierDesign.panelRaised,
        accent = AtelierDesign.accent, onAccent = AtelierDesign.onAccent, mark = AtelierDesign.accentBright,
        text = AtelierDesign.text, textSecondary = AtelierDesign.textSecondary, buttonShape = AtelierDesign.capsuleShape,
        avatarShape = CircleShape, titleWeight = FontWeight.Medium, displayWeight = FontWeight.Light, uppercaseLabels = true
    )
    LevyraIsMono -> JamLook(
        skinned = true, background = MonoDesign.base, card = MonoDesign.surface, border = MonoDesign.hairline,
        cardShape = MonoDesign.panelShape, rowShape = MonoDesign.cardShape, selectedRow = MonoDesign.raised,
        accent = MonoDesign.accent, onAccent = MonoDesign.onAccent, mark = MonoDesign.accent,
        text = MonoDesign.text, textSecondary = MonoDesign.textSecondary, buttonShape = MonoDesign.pillShape,
        avatarShape = CircleShape, titleWeight = FontWeight.SemiBold, displayWeight = FontWeight.Bold, uppercaseLabels = true
    )
    LevyraIsAppleMusic -> JamLook(
        skinned = true, background = AppleMusicDesign.background, card = AppleMusicDesign.groupedBackground, border = Color.Transparent,
        cardShape = RoundedCornerShape(12.dp), rowShape = RoundedCornerShape(10.dp), selectedRow = AppleMusicDesign.raised,
        accent = AppleMusicDesign.accent, onAccent = Color.White, mark = AppleMusicDesign.accent,
        text = AppleMusicDesign.label, textSecondary = AppleMusicDesign.secondaryLabel, buttonShape = RoundedCornerShape(12.dp),
        avatarShape = CircleShape, titleWeight = FontWeight.SemiBold, displayWeight = FontWeight.Bold, uppercaseLabels = false
    )
    LevyraIsYouTubeMusic -> JamLook(
        skinned = true, background = YouTubeMusicDesign.background, card = YouTubeMusicDesign.surface, border = Color.Transparent,
        cardShape = RoundedCornerShape(12.dp), rowShape = RoundedCornerShape(8.dp), selectedRow = YouTubeMusicDesign.surfaceHigh,
        accent = YouTubeMusicDesign.text, onAccent = Color.Black, mark = YouTubeMusicDesign.red,
        text = YouTubeMusicDesign.text, textSecondary = YouTubeMusicDesign.textSecondary, buttonShape = CircleShape,
        avatarShape = CircleShape, titleWeight = FontWeight.Bold, displayWeight = FontWeight.Bold, uppercaseLabels = false
    )
    LevyraIsQobuz -> JamLook(
        skinned = true, background = QobuzDesign.surfacePrimary, card = QobuzDesign.surfaceSecondary, border = Color.Transparent,
        cardShape = QobuzDesign.cardShape, rowShape = QobuzDesign.buttonShape, selectedRow = QobuzDesign.surfaceTertiary,
        accent = QobuzDesign.surfaceInvert, onAccent = QobuzDesign.textInvert, mark = QobuzDesign.textAccent,
        text = QobuzDesign.textPrimary, textSecondary = QobuzDesign.textSecondary, buttonShape = QobuzDesign.buttonShape,
        avatarShape = CircleShape, titleWeight = FontWeight.SemiBold, displayWeight = FontWeight.Normal, uppercaseLabels = false
    )
    LevyraIsSpotify -> JamLook(
        skinned = true, background = SpotifyDesign.base, card = SpotifyDesign.elevated, border = Color.Transparent,
        cardShape = RoundedCornerShape(SpotifyDesign.cornerLarge), rowShape = RoundedCornerShape(SpotifyDesign.corner), selectedRow = SpotifyDesign.highlight,
        accent = SpotifyDesign.green, onAccent = Color.Black, mark = SpotifyDesign.green,
        text = SpotifyDesign.text, textSecondary = SpotifyDesign.subdued, buttonShape = CircleShape,
        avatarShape = CircleShape, titleWeight = FontWeight.Bold, displayWeight = FontWeight.Black, uppercaseLabels = false
    )
    LevyraIsTorque -> genericJamLook(RoundedCornerShape(2.dp), RoundedCornerShape(2.dp), RoundedCornerShape(2.dp), RoundedCornerShape(2.dp), uppercase = true)
    LevyraIsIvory -> genericJamLook(RoundedCornerShape(IvoryDesign.surfaceCorner), RoundedCornerShape(IvoryDesign.controlCorner), RoundedCornerShape(IvoryDesign.controlCorner), IvoryDesign.coverShape, uppercase = false)
    LevyraIsNocturne -> genericJamLook(RoundedCornerShape(Nocturne78Design.panelCorner), RoundedCornerShape(Nocturne78Design.controlCorner), RoundedCornerShape(Nocturne78Design.controlCorner), CircleShape, uppercase = true)
    LevyraIsPrism -> genericJamLook(PrismDesign.cut(PrismDesign.surfaceCorner), PrismDesign.cut(12.dp), PrismDesign.cut(12.dp), PrismDesign.cut(10.dp), uppercase = false)
    else -> DefaultJamLook
}

private fun genericJamLook(card: Shape, row: Shape, button: Shape, avatar: Shape, uppercase: Boolean) = JamLook(
    skinned = true, background = LevyraBlack, card = LevyraPanel, border = LevyraGlassBorder,
    cardShape = card, rowShape = row, selectedRow = LevyraPanelSoft,
    accent = LevyraCyan, onAccent = LevyraOnAccent, mark = LevyraCyan,
    text = LevyraText, textSecondary = LevyraMuted, buttonShape = button,
    avatarShape = avatar, titleWeight = FontWeight.Bold, displayWeight = FontWeight.Bold, uppercaseLabels = uppercase
)
