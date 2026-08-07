package com.luc4n3x.levyra.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraViolet

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LibraryImportPlaylistCard(
    onClick: () -> Unit,
    isItalian: Boolean
) {
    val shape = RoundedCornerShape(22.dp)
    Surface(
        color = Color.Transparent,
        shape = shape,
        border = BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.22f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            LevyraCyan.copy(alpha = 0.11f),
                            LevyraViolet.copy(alpha = 0.055f),
                            LevyraPanel.copy(alpha = 0.76f)
                        )
                    )
                )
                .padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(LevyraCyan.copy(alpha = 0.12f), RoundedCornerShape(15.dp))
                    .border(1.dp, LevyraCyan.copy(alpha = 0.20f), RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Link,
                    contentDescription = null,
                    tint = LevyraCyan,
                    modifier = Modifier.size(23.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = if (isItalian) "Importa playlist" else "Import playlist",
                    color = LevyraText,
                    fontSize = 16.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isItalian) {
                        "YouTube Music · Spotify · backup JSON"
                    } else {
                        "YouTube Music · Spotify · JSON backup"
                    },
                    color = LevyraMuted,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                color = LevyraCyan.copy(alpha = 0.12f),
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.18f))
            ) {
                Text(
                    text = if (isItalian) "IMPORTA" else "IMPORT",
                    color = LevyraCyan,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.7.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
internal fun LibraryImportPlaylistDialog(
    isItalian: Boolean,
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }
    val canImport = input.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LevyraPanel,
        title = {
            Text(
                text = if (isItalian) "Importa playlist" else "Import playlist",
                color = LevyraText,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = if (isItalian) {
                        "Incolla un link di YouTube/YouTube Music, una playlist Spotify pubblica oppure il JSON di un backup ViMusic/InnerTune. Levyra importerà i brani nella tua libreria."
                    } else {
                        "Paste a YouTube/YouTube Music link, a public Spotify playlist, or ViMusic/InnerTune backup JSON. Levyra will import the tracks into your library."
                    },
                    color = LevyraMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 7,
                    shape = RoundedCornerShape(16.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = LevyraText),
                    placeholder = {
                        Text(
                            text = if (isItalian) "Incolla link o JSON…" else "Paste link or JSON…",
                            color = LevyraMuted.copy(alpha = 0.62f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = LevyraText,
                        unfocusedTextColor = LevyraText,
                        focusedBorderColor = LevyraCyan.copy(alpha = 0.72f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        cursorColor = LevyraCyan
                    )
                )
                Text(
                    text = if (isItalian) {
                        "Spotify viene usato solo per identificare i brani; la riproduzione resta gestita da Levyra."
                    } else {
                        "Spotify is only used to identify tracks; playback remains handled by Levyra."
                    },
                    color = LevyraMuted.copy(alpha = 0.78f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canImport,
                onClick = { onImport(input.trim()) }
            ) {
                Text(
                    text = if (isItalian) "Importa" else "Import",
                    color = if (canImport) LevyraCyan else LevyraMuted.copy(alpha = 0.50f),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isItalian) "Annulla" else "Cancel", color = LevyraMuted)
            }
        }
    )
}
