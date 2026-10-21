package com.luc4n3x.levyra.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.theme.SpotifyDesign

@Composable
internal fun SpotifyTextAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Text(
        label, color = SpotifyDesign.subdued, style = MaterialTheme.typography.labelLarge, fontSize = 13.sp, maxLines = 1,
        modifier = modifier.heightIn(min = 48.dp).clip(CircleShape)
            .levyraPressable(onClick = onClick, enabled = enabled, onClickLabel = label, role = Role.Button)
            .wrapContentHeight().padding(horizontal = 8.dp)
    )
}
