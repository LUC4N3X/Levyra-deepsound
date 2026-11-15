package com.luc4n3x.levyra.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.theme.SpotifyDesign

@Composable
internal fun SpotifyTextAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconOnlyIcon: ImageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight
) {
    if (LocalSectionActionIconOnly.current) {
        Box(
            modifier.size(48.dp).clip(CircleShape)
                .levyraPressable(onClick = onClick, enabled = enabled, onClickLabel = label, role = Role.Button)
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center
        ) {
            Icon(iconOnlyIcon, contentDescription = null, tint = SpotifyDesign.subdued, modifier = Modifier.size(24.dp))
        }
        return
    }
    Text(
        label, color = SpotifyDesign.subdued, style = MaterialTheme.typography.labelLarge, fontSize = 13.sp, maxLines = 1,
        modifier = modifier.heightIn(min = 48.dp).clip(CircleShape)
            .levyraPressable(onClick = onClick, enabled = enabled, onClickLabel = label, role = Role.Button)
            .wrapContentHeight().padding(horizontal = 8.dp)
    )
}
