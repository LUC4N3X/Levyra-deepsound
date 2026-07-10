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

val InterFont = FontFamily(
    Font(googleFont = GoogleFont("Inter"), fontProvider = provider)
)

private val defaultTypography = Typography()
val LevyraTypography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = InterFont),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = InterFont),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = InterFont),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = InterFont),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = InterFont),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = InterFont),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = InterFont),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = InterFont),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = InterFont),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = InterFont),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = InterFont),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = InterFont),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = InterFont),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = InterFont),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = InterFont)
)
