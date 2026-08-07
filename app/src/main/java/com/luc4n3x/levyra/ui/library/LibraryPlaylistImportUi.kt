package com.luc4n3x.levyra.ui.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.playlistImportCopy
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraViolet

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LibraryImportPlaylistCard(
    onClick: () -> Unit,
    onDismiss: () -> Unit = {},
    @Suppress("UNUSED_PARAMETER") isItalian: Boolean = false
) {
    val copy = LocalLevyraStrings.current.playlistImportCopy()
    val shape = RoundedCornerShape(24.dp)
    Surface(
        color = Color.Transparent,
        shape = shape,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            LevyraPanel.copy(alpha = 0.92f),
                            LevyraPanel.copy(alpha = 0.82f),
                            LevyraViolet.copy(alpha = 0.055f)
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 13.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(76.dp)
            ) {
                val stroke = 1.15.dp.toPx()
                drawArc(
                    color = LevyraCyan.copy(alpha = 0.13f),
                    startAngle = -55f,
                    sweepAngle = 205f,
                    useCenter = false,
                    style = Stroke(width = stroke)
                )
                drawArc(
                    color = LevyraViolet.copy(alpha = 0.11f),
                    startAngle = 112f,
                    sweepAngle = 145f,
                    useCenter = false,
                    style = Stroke(width = stroke),
                    topLeft = Offset(9.dp.toPx(), 9.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(
                        width = size.width - 18.dp.toPx(),
                        height = size.height - 18.dp.toPx()
                    )
                )
                drawCircle(
                    color = LevyraCyan.copy(alpha = 0.48f),
                    radius = 1.9.dp.toPx(),
                    center = Offset(size.width * 0.78f, size.height * 0.24f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.045f), RoundedCornerShape(14.dp))
                        .border(1.dp, LevyraCyan.copy(alpha = 0.16f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        contentDescription = null,
                        tint = LevyraCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = copy.title,
                        color = LevyraText,
                        fontSize = 15.5.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = copy.subtitle,
                        color = LevyraMuted,
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = copy.cancel,
                    tint = LevyraMuted.copy(alpha = 0.78f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LibraryImportPlaylistCompactAction(
    onClick: () -> Unit
) {
    val copy = LocalLevyraStrings.current.playlistImportCopy()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            color = Color.White.copy(alpha = 0.045f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .combinedClickable(onClick = onClick)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = null,
                    tint = LevyraCyan,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = copy.action,
                    color = LevyraText,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
internal fun LibraryImportPlaylistDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
    @Suppress("UNUSED_PARAMETER") isItalian: Boolean = false
) {
    val copy = LocalLevyraStrings.current.playlistImportCopy()
    var input by remember { mutableStateOf("") }
    val canImport = input.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LevyraPanel,
        title = {
            Text(
                text = copy.title,
                color = LevyraText,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = copy.body,
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
                            text = copy.placeholder,
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
                    text = copy.note,
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
                    text = copy.action,
                    color = if (canImport) LevyraCyan else LevyraMuted.copy(alpha = 0.50f),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(copy.cancel, color = LevyraMuted)
            }
        }
    )
}
