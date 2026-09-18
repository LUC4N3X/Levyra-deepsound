package com.luc4n3x.levyra.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.data.local.LocalMediaEntity
import com.luc4n3x.levyra.data.locallibrary.LocalTagEdits
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LocalTagEditorSheet(
    media: LocalMediaEntity,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (LocalTagEdits) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(media.identityKey) { mutableStateOf(media.title) }
    var artist by remember(media.identityKey) { mutableStateOf(media.artist) }
    var album by remember(media.identityKey) { mutableStateOf(media.album) }
    var albumArtist by remember(media.identityKey) { mutableStateOf(media.albumArtist) }
    var genre by remember(media.identityKey) { mutableStateOf(media.genre) }
    var year by remember(media.identityKey) { mutableStateOf(media.year.takeIf { it > 0 }?.toString().orEmpty()) }
    var track by remember(media.identityKey) { mutableStateOf(media.trackNumber.takeIf { it > 0 }?.toString().orEmpty()) }
    var disc by remember(media.identityKey) { mutableStateOf(media.discNumber.takeIf { it > 0 }?.toString().orEmpty()) }
    var composer by remember(media.identityKey) { mutableStateOf(media.composer) }
    var lyricist by remember(media.identityKey) { mutableStateOf(media.lyricist) }
    var comment by remember(media.identityKey) { mutableStateOf(media.comment) }
    var copyright by remember(media.identityKey) { mutableStateOf(media.copyright) }

    ModalBottomSheet(
        onDismissRequest = { if (!saving) onDismiss() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Rounded.AudioFile, contentDescription = null, tint = LevyraCyan)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        strings.localTagEditorTitle,
                        color = LevyraText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        media.displayName,
                        color = LevyraMuted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(strings.localTagEditorSubtitle, color = LevyraMuted, fontSize = 12.sp)

            TagField(strings.localTagTitle, title, { title = it }, saving)
            TagField(strings.localTagArtist, artist, { artist = it }, saving)
            TagField(strings.localTagAlbum, album, { album = it }, saving)
            TagField(strings.localTagAlbumArtist, albumArtist, { albumArtist = it }, saving)
            TagField(strings.localTagGenre, genre, { genre = it }, saving)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TagField(
                    label = strings.localTagYear,
                    value = year,
                    onValueChange = { year = it.filter(Char::isDigit).take(4) },
                    enabled = !saving,
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number
                )
                TagField(
                    label = strings.localTagTrack,
                    value = track,
                    onValueChange = { track = it.filter(Char::isDigit).take(4) },
                    enabled = !saving,
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number
                )
                TagField(
                    label = strings.localTagDisc,
                    value = disc,
                    onValueChange = { disc = it.filter(Char::isDigit).take(3) },
                    enabled = !saving,
                    modifier = Modifier.weight(1f),
                    keyboardType = KeyboardType.Number
                )
            }

            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = LevyraMuted.copy(alpha = 0.18f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Badge, contentDescription = null, tint = LevyraMuted)
                Text(
                    strings.localTagCredits,
                    color = LevyraText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            TagField(strings.localTagComposer, composer, { composer = it }, saving)
            TagField(strings.localTagLyricist, lyricist, { lyricist = it }, saving)
            TagField(
                label = strings.localTagComment,
                value = comment,
                onValueChange = { comment = it.take(4_096) },
                enabled = !saving,
                singleLine = false,
                minLines = 2
            )
            TagField(strings.localTagCopyright, copyright, { copyright = it }, saving)

            if (!error.isNullOrBlank()) {
                Text(error, color = LevyraPink, fontSize = 12.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss, enabled = !saving) {
                    Text(strings.cancel)
                }
                Button(
                    onClick = {
                        onSave(
                            LocalTagEdits(
                                title = title,
                                artist = artist,
                                album = album,
                                albumArtist = albumArtist,
                                genre = genre,
                                year = year,
                                trackNumber = track,
                                discNumber = disc,
                                composer = composer,
                                lyricist = lyricist,
                                comment = comment,
                                copyright = copyright
                            )
                        )
                    },
                    enabled = !saving
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null)
                    Text(
                        if (saving) strings.localTagSaving else strings.localTagSave,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TagField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.bodyMedium
    )
}
