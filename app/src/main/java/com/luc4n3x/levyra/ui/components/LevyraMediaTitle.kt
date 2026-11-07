package com.luc4n3x.levyra.ui.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explicit
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LevyraAppleMusicStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings

@Composable
internal fun trackContentDescription(track: Track): String {
    val explicitLabel = LevyraAppleMusicStrings.forCode(LocalLevyraStrings.current.code).explicit
    return "${track.title}, ${track.artist}" + if (track.explicit) ", $explicitLabel" else ""
}

@Composable
internal fun LevyraMediaTitle(
    text: String,
    modifier: Modifier = Modifier,
    explicit: Boolean = false,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    style: TextStyle = LocalTextStyle.current,
    marqueeEnabled: Boolean = true,
    repeatDelayMillis: Int = 3_200
) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val strings = LocalLevyraStrings.current
    val explicitLabel = LevyraAppleMusicStrings.forCode(strings.code).explicit
    Row(
        modifier.semantics(mergeDescendants = true) {
            contentDescription = if (explicit) "$text, $explicitLabel" else text
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = when (textAlign) {
            TextAlign.Center -> Arrangement.Center
            TextAlign.End, TextAlign.Right -> Arrangement.End
            else -> Arrangement.Start
        }
    ) {
        if (explicit) {
            Icon(
                Icons.Rounded.Explicit,
                contentDescription = null,
                tint = color.takeOrElse { style.color.takeOrElse { LocalContentColor.current } },
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = text,
            modifier = Modifier.weight(1f, fill = false)
                .then(if (explicit) Modifier.padding(start = 5.dp) else Modifier)
                .then(
                    if (animationsEnabled && marqueeEnabled && maxLines == 1) {
                        Modifier.basicMarquee(iterations = Int.MAX_VALUE, repeatDelayMillis = repeatDelayMillis)
                    } else Modifier
                ),
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            lineHeight = lineHeight,
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = overflow,
            style = style
        )
    }
}
