package com.luc4n3x.levyra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraCardDesign
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraType
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm

@Composable
internal fun Modifier.levyraDockSurface(): Modifier {
    val fill = LevyraPanel.copy(alpha = 0.98f)
    val shadowColor = Color.Black.copy(alpha = if (LevyraActivePalette.isLight) 0.18f else 0.55f)
    return this
        .shadow(
            elevation = 18.dp,
            shape = LevyraCardDesign.SurfaceShape,
            clip = false,
            ambientColor = shadowColor,
            spotColor = shadowColor
        )
        .clip(LevyraCardDesign.SurfaceShape)
        .background(fill)
}

@Composable
internal fun LevyraDockProgress(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(LevyraText.copy(alpha = 0.08f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(Brush.horizontalGradient(listOf(LevyraCyan, LevyraPink)))
        )
    }
}

@Composable
internal fun LevyraCardCaption(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    titleColor: Color = LevyraText,
    subtitleColor: Color = LevyraMuted,
    titleLines: Int = 2,
    reserveTitleLines: Boolean = true
) {
    val titleStyle = LevyraType.cardTitle
    val subtitleStyle = LevyraType.caption
    val minHeight = if (reserveTitleLines) {
        levyraCaptionHeight(titleLines)
    } else {
        0.dp
    }
    Column(
        modifier = modifier.heightIn(min = minHeight),
        verticalArrangement = Arrangement.spacedBy(LevyraCardDesign.CaptionLineGap)
    ) {
        Text(
            text = title,
            color = titleColor,
            style = titleStyle,
            maxLines = titleLines,
            overflow = TextOverflow.Ellipsis
        )
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                color = subtitleColor,
                style = subtitleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun levyraCaptionHeight(titleLines: Int): Dp {
    val density = LocalDensity.current
    return with(density) {
        LevyraTypeRhythm.lineHeight(LevyraCardDesign.CardTitleSize).toDp() * titleLines +
            LevyraTypeRhythm.lineHeight(LevyraCardDesign.CardSubtitleSize).toDp() +
            LevyraCardDesign.CaptionLineGap
    }
}

@Composable
internal fun LevyraRowText(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    titleColor: Color = LevyraText,
    subtitleColor: Color = LevyraMuted,
    downloaded: Boolean = false,
    favorite: Boolean = false,
    statusTint: Color = LevyraCyan,
    downloadedLabel: String? = null,
    favoriteLabel: String? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(LevyraCardDesign.RowLineGap)
    ) {
        Text(
            text = title,
            color = titleColor,
            style = LevyraType.contentTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (subtitle.isNotBlank() || downloaded || favorite) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                if (favorite) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = favoriteLabel,
                        tint = LevyraPink,
                        modifier = Modifier.size(LevyraCardDesign.StatusGlyph)
                    )
                }
                if (downloaded) {
                    Icon(
                        imageVector = Icons.Rounded.DownloadDone,
                        contentDescription = downloadedLabel,
                        tint = statusTint,
                        modifier = Modifier.size(LevyraCardDesign.StatusGlyph)
                    )
                }
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = subtitleColor,
                        style = LevyraType.metadata,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
