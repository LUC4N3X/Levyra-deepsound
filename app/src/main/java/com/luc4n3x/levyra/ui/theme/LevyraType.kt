package com.luc4n3x.levyra.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

object LevyraType {

    val heroTitle: TextStyle
        @Composable @ReadOnlyComposable
        get() = role(MaterialTheme.typography.displaySmall, 34.sp, FontWeight.ExtraBold, (-1.1).sp, 37.sp)

    val screenTitle: TextStyle
        @Composable @ReadOnlyComposable
        get() = role(MaterialTheme.typography.headlineMedium, 28.sp, FontWeight.Bold, (-0.7).sp)

    val sectionTitle: TextStyle
        @Composable @ReadOnlyComposable
        get() = role(MaterialTheme.typography.titleLarge, 21.sp, FontWeight.Bold, (-0.4).sp)

    val contentTitle: TextStyle
        @Composable @ReadOnlyComposable
        get() = role(MaterialTheme.typography.titleMedium, LevyraCardDesign.RowTitleSize, FontWeight.Medium, (-0.2).sp)

    val cardTitle: TextStyle
        @Composable @ReadOnlyComposable
        get() = role(MaterialTheme.typography.titleSmall, LevyraCardDesign.CardTitleSize, FontWeight.SemiBold, (-0.15).sp)

    val artist: TextStyle
        @Composable @ReadOnlyComposable
        get() = role(MaterialTheme.typography.titleMedium, 15.sp, FontWeight.SemiBold, (-0.1).sp)

    val metadata: TextStyle
        @Composable @ReadOnlyComposable
        get() = role(MaterialTheme.typography.bodyMedium, LevyraCardDesign.RowSubtitleSize, FontWeight.Normal, 0.sp)

    val caption: TextStyle
        @Composable @ReadOnlyComposable
        get() = role(MaterialTheme.typography.bodySmall, LevyraCardDesign.CardSubtitleSize, FontWeight.Normal, 0.sp)

    val overline: TextStyle
        @Composable @ReadOnlyComposable
        get() = role(MaterialTheme.typography.labelMedium, 11.sp, FontWeight.SemiBold, 1.4.sp)

    fun heroTitleSize(title: String, wide: Boolean): TextUnit {
        val length = title.trim().length
        return when {
            length <= 14 -> if (wide) 40.sp else 36.sp
            length <= 26 -> if (wide) 34.sp else 30.sp
            length <= 44 -> if (wide) 28.sp else 26.sp
            else -> if (wide) 24.sp else 22.sp
        }
    }

    fun heroLineHeight(size: TextUnit): TextUnit = (size.value * 1.08f).sp

    private fun role(
        base: TextStyle,
        size: TextUnit,
        weight: FontWeight,
        tracking: TextUnit,
        lineHeight: TextUnit = LevyraTypeRhythm.lineHeight(size)
    ): TextStyle = base.copy(
        fontSize = size,
        fontWeight = weight,
        letterSpacing = tracking,
        lineHeight = lineHeight
    )
}
