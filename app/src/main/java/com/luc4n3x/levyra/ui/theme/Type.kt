package com.luc4n3x.levyra.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.R
import com.luc4n3x.levyra.domain.LevyraFontPreset

private val googleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private fun googleFontFamily(name: String): FontFamily = FontFamily(
    Font(googleFont = GoogleFont(name), fontProvider = googleFontsProvider)
)

val OutfitFont: FontFamily = googleFontFamily("Outfit")
private val InterFont: FontFamily = googleFontFamily("Inter")
private val ManropeFont: FontFamily = googleFontFamily("Manrope")
private val PoppinsFont: FontFamily = googleFontFamily("Poppins")
private val MontserratFont: FontFamily = googleFontFamily("Montserrat")
private val NunitoFont: FontFamily = googleFontFamily("Nunito")
private val RobotoFont: FontFamily = googleFontFamily("Roboto")

private val defaultTypography = Typography()

private val LevyraLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

private val LevyraPlatformTextStyle = PlatformTextStyle(includeFontPadding = false)

private fun TextStyle.levyraRhythm(
    fontFamily: FontFamily,
    fontWeight: FontWeight? = null,
    letterSpacing: androidx.compose.ui.unit.TextUnit = this.letterSpacing,
    retuneLineHeight: Boolean = true
): TextStyle = copy(
    fontFamily = fontFamily,
    fontWeight = fontWeight ?: this.fontWeight,
    letterSpacing = letterSpacing,
    lineHeight = if (retuneLineHeight && fontSize.isSp) {
        LevyraTypeRhythm.lineHeight(fontSize)
    } else {
        lineHeight
    },
    lineHeightStyle = LevyraLineHeightStyle,
    platformStyle = LevyraPlatformTextStyle
)

private fun typographyForFamily(fontFamily: FontFamily): Typography = Typography(
    displayLarge = defaultTypography.displayLarge.levyraRhythm(fontFamily, FontWeight.Bold, (-0.25).sp, retuneLineHeight = false),
    displayMedium = defaultTypography.displayMedium.levyraRhythm(fontFamily, FontWeight.Bold, (-0.2).sp, retuneLineHeight = false),
    displaySmall = defaultTypography.displaySmall.levyraRhythm(fontFamily, FontWeight.Bold, (-0.15).sp, retuneLineHeight = false),
    headlineLarge = defaultTypography.headlineLarge.levyraRhythm(fontFamily, FontWeight.Bold, (-0.15).sp, retuneLineHeight = false),
    headlineMedium = defaultTypography.headlineMedium.levyraRhythm(fontFamily, FontWeight.Bold, (-0.1).sp, retuneLineHeight = false),
    headlineSmall = defaultTypography.headlineSmall.levyraRhythm(fontFamily, FontWeight.SemiBold, 0.sp, retuneLineHeight = false),
    titleLarge = defaultTypography.titleLarge.levyraRhythm(fontFamily, FontWeight.SemiBold, 0.sp),
    titleMedium = defaultTypography.titleMedium.levyraRhythm(fontFamily, FontWeight.SemiBold),
    titleSmall = defaultTypography.titleSmall.levyraRhythm(fontFamily, FontWeight.SemiBold),
    bodyLarge = defaultTypography.bodyLarge.levyraRhythm(fontFamily),
    bodyMedium = defaultTypography.bodyMedium.levyraRhythm(fontFamily),
    bodySmall = defaultTypography.bodySmall.levyraRhythm(fontFamily),
    labelLarge = defaultTypography.labelLarge.levyraRhythm(fontFamily),
    labelMedium = defaultTypography.labelMedium.levyraRhythm(fontFamily),
    labelSmall = defaultTypography.labelSmall.levyraRhythm(fontFamily)
)

private val typographyByPreset: Map<LevyraFontPreset, Typography> = mapOf(
    LevyraFontPreset.Outfit to typographyForFamily(OutfitFont),
    LevyraFontPreset.Inter to typographyForFamily(InterFont),
    LevyraFontPreset.Manrope to typographyForFamily(ManropeFont),
    LevyraFontPreset.Poppins to typographyForFamily(PoppinsFont),
    LevyraFontPreset.Montserrat to typographyForFamily(MontserratFont),
    LevyraFontPreset.Nunito to typographyForFamily(NunitoFont),
    LevyraFontPreset.Roboto to typographyForFamily(RobotoFont),
    LevyraFontPreset.System to typographyForFamily(FontFamily.Default)
)

val LevyraTypography: Typography = typographyByPreset.getValue(LevyraFontPreset.Outfit)

internal fun levyraTypographyFor(preset: LevyraFontPreset): Typography =
    typographyByPreset[preset] ?: LevyraTypography
