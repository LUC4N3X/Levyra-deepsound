package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.desktop.app.ui.components.tracksTextInputFocus
import com.luc4n3x.levyra.desktop.app.ui.i18n.LocalStrings

@Composable
internal fun LibraryToolsCard(
    onCreatePlaylist: (String) -> Unit,
    onImportUrl: (String) -> Unit
) {
    val strings = LocalStrings.current
    var playlistName by remember { mutableStateOf("") }
    var importUrl by remember { mutableStateOf("") }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = LibraryShapes.Surface,
        color = librarySurface()
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(strings.homeLibraryTools, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold))
                Text(strings.homeLibraryToolsBody, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        label = { Text(strings.playlistName) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().tracksTextInputFocus()
                    )
                    Button(
                        onClick = {
                            onCreatePlaylist(playlistName)
                            playlistName = ""
                        },
                        enabled = playlistName.isNotBlank(),
                        modifier = Modifier.align(Alignment.End)
                    ) { Text(strings.playlistCreate) }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = importUrl,
                        onValueChange = { importUrl = it },
                        label = { Text(strings.importPlaylistHint) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().tracksTextInputFocus()
                    )
                    OutlinedButton(
                        onClick = {
                            onImportUrl(importUrl)
                            importUrl = ""
                        },
                        enabled = importUrl.isNotBlank(),
                        modifier = Modifier.align(Alignment.End)
                    ) { Text(strings.importAction) }
                }
            }
        }
    }
}
