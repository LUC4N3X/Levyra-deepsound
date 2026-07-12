package com.luc4n3x.levyra.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
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
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = OutfitFont),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = OutfitFont),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = OutfitFont),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = OutfitFont),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = OutfitFont),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = OutfitFont),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = OutfitFont),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = OutfitFont),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = OutfitFont),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = OutfitFont),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = OutfitFont),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = OutfitFont),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = OutfitFont),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = OutfitFont),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = OutfitFont)
)
