package com.luc4n3x.levyra.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val OutfitFont = FontFamily(
    Font(googleFont = GoogleFont("Outfit"), fontProvider = provider)
)

private val defaultTypography = Typography()
val LevyraTypography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = OutfitFont, letterSpacing = (-1.2).sp, fontWeight = FontWeight.Bold),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = OutfitFont, letterSpacing = (-1).sp, fontWeight = FontWeight.Bold),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = OutfitFont, letterSpacing = (-0.8).sp, fontWeight = FontWeight.Bold),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = OutfitFont, letterSpacing = (-0.7).sp, fontWeight = FontWeight.Bold),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = OutfitFont, letterSpacing = (-0.5).sp, fontWeight = FontWeight.Bold),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = OutfitFont, letterSpacing = (-0.4).sp, fontWeight = FontWeight.SemiBold),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = OutfitFont, letterSpacing = (-0.3).sp, fontWeight = FontWeight.SemiBold),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = OutfitFont, letterSpacing = (-0.1).sp, fontWeight = FontWeight.SemiBold),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = OutfitFont, fontWeight = FontWeight.SemiBold),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = OutfitFont),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = OutfitFont),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = OutfitFont),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = OutfitFont, letterSpacing = 0.2.sp),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = OutfitFont, letterSpacing = 0.3.sp),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = OutfitFont, letterSpacing = 0.4.sp)
)
