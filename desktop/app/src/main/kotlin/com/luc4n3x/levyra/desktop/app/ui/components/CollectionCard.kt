package com.luc4n3x.levyra.desktop.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.core.model.CollectionKind
import com.luc4n3x.levyra.desktop.core.model.CollectionRef

@Composable
fun CollectionCard(
    ref: CollectionRef,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val circular = ref.kind == CollectionKind.ARTIST
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Artwork(
            url = ref.artworkUrl,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            cornerRadius = if (circular) 999.dp else 12.dp,
            iconSize = 32.dp
        )
        Text(
            text = ref.title,
            style = MaterialTheme.typography.titleSmall,
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
        }
    }
}
