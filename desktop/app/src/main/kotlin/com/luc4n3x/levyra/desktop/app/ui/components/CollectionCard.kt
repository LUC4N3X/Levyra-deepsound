package com.luc4n3x.levyra.desktop.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.ui.theme.LevyraMotion
import com.luc4n3x.levyra.desktop.app.ui.theme.LocalAccentColor
import com.luc4n3x.levyra.desktop.core.model.CollectionKind
import com.luc4n3x.levyra.desktop.core.model.CollectionRef

@Composable
fun CollectionCard(
    ref: CollectionRef,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val circular = ref.kind == CollectionKind.ARTIST
    val accent = LocalAccentColor.current
    val (interactionSource, hovered) = rememberHoverState(ref.id)
    val shape = RoundedCornerShape(13.dp)
    val background = if (hovered) {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = LevyraMotion.HOVER_ALPHA)
    } else {
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f)
    }

    Column(
        modifier = modifier
            .clip(shape)
            .background(background)
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box {
            Artwork(
                url = ref.artworkUrl,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                cornerRadius = if (circular) 999.dp else 10.dp,
                iconSize = 32.dp
            )
            if (hovered) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .size(38.dp),
                    shape = CircleShape,
                    color = accent,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = LevyraIcons.Play,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
        Text(
            text = ref.title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (ref.subtitle.isNotBlank()) {
            Text(
                text = ref.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Text(
                text = if (circular) "Artist" else "",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Transparent,
                maxLines = 1
            )
        }
    }
}
