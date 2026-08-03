package com.luc4n3x.levyra.ui.theme

import androidx.compose.material3.Typography
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

private fun typographyForFamily(fontFamily: FontFamily): Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = fontFamily, letterSpacing = (-1.2).sp, fontWeight = FontWeight.Bold),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = fontFamily, letterSpacing = (-1).sp, fontWeight = FontWeight.Bold),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = fontFamily, letterSpacing = (-0.8).sp, fontWeight = FontWeight.Bold),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = fontFamily, letterSpacing = (-0.7).sp, fontWeight = FontWeight.Bold),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = fontFamily, letterSpacing = (-0.5).sp, fontWeight = FontWeight.Bold),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = fontFamily, letterSpacing = (-0.4).sp, fontWeight = FontWeight.SemiBold),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = fontFamily, letterSpacing = (-0.3).sp, fontWeight = FontWeight.SemiBold),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = fontFamily, letterSpacing = (-0.1).sp, fontWeight = FontWeight.SemiBold),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = fontFamily),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = fontFamily),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = fontFamily),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = fontFamily, letterSpacing = 0.2.sp),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = fontFamily, letterSpacing = 0.3.sp),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = fontFamily, letterSpacing = 0.4.sp)
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
