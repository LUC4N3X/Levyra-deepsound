package com.luc4n3x.levyra.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.ArtistBiography
import com.luc4n3x.levyra.domain.artistBiographyEditorial
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.player.*
import com.luc4n3x.levyra.ui.theme.*
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraType

@Composable
internal fun ArtistBio(
    biography: ArtistBiography,
    accentStart: Color,
    accentEnd: Color,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    val editorial = remember(biography.text, biography.description, biography.languageCode) {
        artistBiographyEditorial(
            text = biography.text,
            description = biography.description,
            languageCode = biography.languageCode
        )
    }
    val presentation = remember(
        biography.text,
        biography.sourceLabel,
        biography.languageCode,
        editorial.paragraphs,
        editorial.summary,
        strings.code
    ) {
        buildArtistBiographyPresentation(
            biography = biography,
            paragraphs = editorial.paragraphs,
            summary = editorial.summary,
            uiLanguageCode = strings.code
        )
    }
    val biographyText = remember(presentation.paragraphs, presentation.previewText) {
        presentation.paragraphs
            .filter { it.isNotBlank() }
            .joinToString("\n\n")
            .ifBlank { presentation.previewText }
    }
    var expanded by rememberSaveable(biography.pageId, biography.languageCode, biography.text) {
        mutableStateOf(false)
    }
    var hasOverflow by remember(biographyText) { mutableStateOf(false) }
    val accentBrush = remember(accentStart, accentEnd) {
        Brush.linearGradient(
            listOf(
                accentStart.copy(alpha = 0.16f),
                accentEnd.copy(alpha = 0.04f)
            )
        )
    }

    val animationsEnabled = LocalAnimationsEnabled.current
    val badgeShape = artistBiographyBadgeShape()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(artistBiographyBackground(accentBrush))
            .then(
                if (animationsEnabled) {
                    Modifier.animateContentSize(animationSpec = LevyraMotion.emphasized())
                } else {
                    Modifier
                }
            )
            .padding(start = 18.dp, end = 14.dp, top = 16.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(badgeShape)
                    .background(accentStart.copy(alpha = 0.32f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.FormatQuote,
                    contentDescription = null,
                    tint = LevyraText,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = strings.biography,
                color = LevyraText,
                style = LevyraType.sectionTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() }
            )
            if (presentation.showSource) {
                BiographyInlineSource(
                    sourceText = presentation.sourceText,
                    sourceUrl = biography.sourceUrl
                )
            }
        }

        Text(
            text = biographyText,
            color = LevyraText.copy(alpha = 0.86f),
            style = LevyraType.metadata.copy(fontSize = 15.sp, lineHeight = 23.sp),
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                if (!expanded) hasOverflow = result.hasVisualOverflow
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 4.dp)
        )

        if (expanded || hasOverflow) {
            ArtistTonalPill(
                text = if (expanded) strings.showLess else strings.readAll,
                icon = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                onClick = { expanded = !expanded }
            )
        }
    }
}

internal val ArtistBiographyShape = RoundedCornerShape(28.dp)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun artistBiographyBadgeShape(): Shape = MaterialShapes.Cookie9Sided.toShape()


@Composable
private fun BiographyInlineSource(
    sourceText: String,
    sourceUrl: String
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .clickable(enabled = sourceUrl.isNotBlank()) {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(sourceUrl)))
                }
            },
        contentAlignment = Alignment.Center
    ) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(LevyraText.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = sourceText,
            color = LevyraMuted,
            style = LevyraType.caption.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1
        )
        if (sourceUrl.isNotBlank()) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = sourceText,
                tint = LevyraMuted,
                modifier = Modifier.size(13.dp)
            )
        }
    }
    }
}
