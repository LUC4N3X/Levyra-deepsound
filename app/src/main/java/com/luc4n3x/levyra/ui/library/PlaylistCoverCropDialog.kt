package com.luc4n3x.levyra.ui.library

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.luc4n3x.levyra.data.PlaylistCoverCrop
import com.luc4n3x.levyra.data.playlistCoverMaxOffset
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.playlistProCopy
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPanelSoft
import com.luc4n3x.levyra.ui.theme.LevyraText
import kotlin.math.roundToInt

@Composable
internal fun PlaylistCoverCropDialog(
    source: Uri,
    onDismiss: () -> Unit,
    onConfirm: (PlaylistCoverCrop) -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = strings.playlistProCopy()
    val context = LocalContext.current
    var viewport by remember(source) { mutableStateOf(IntSize.Zero) }
    var imageSize by remember(source) { mutableStateOf(IntSize.Zero) }
    var zoom by remember(source) { mutableFloatStateOf(1f) }
    var offsetX by remember(source) { mutableFloatStateOf(0f) }
    var offsetY by remember(source) { mutableFloatStateOf(0f) }

    fun clampOffset() {
        val (maxX, maxY) = playlistCoverMaxOffset(
            imageWidth = imageSize.width,
            imageHeight = imageSize.height,
            viewportSizePx = viewport.width,
            zoom = zoom
        )
        offsetX = offsetX.coerceIn(-maxX, maxX)
        offsetY = offsetY.coerceIn(-maxY, maxY)
    }

    fun updateZoom(value: Float) {
        zoom = value.coerceIn(1f, 4f)
        clampOffset()
    }

    fun resetCrop() {
        zoom = 1f
        offsetX = 0f
        offsetY = 0f
        clampOffset()
    }

    fun moveCover(deltaX: Float, deltaY: Float): Boolean {
        offsetX += deltaX
        offsetY += deltaY
        clampOffset()
        return true
    }

    val panStep = viewport.width.coerceAtLeast(1) * 0.08f

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = LevyraPanel,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            modifier = Modifier
                .systemBarsPadding()
                .padding(20.dp)
                .fillMaxWidth()
                .widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = copy.adjustCover,
                        color = LevyraText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = strings.close, tint = LevyraText)
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(LevyraPanelSoft)
                        .semantics(mergeDescendants = true) {
                            contentDescription = copy.coverPreview
                            stateDescription = "${(zoom * 100).roundToInt()}%"
                            customActions = buildList {
                                if (zoom < 4f) {
                                    add(CustomAccessibilityAction(copy.zoomIn) {
                                        updateZoom(zoom + 0.25f)
                                        true
                                    })
                                }
                                if (zoom > 1f) {
                                    add(CustomAccessibilityAction(copy.zoomOut) {
                                        updateZoom(zoom - 0.25f)
                                        true
                                    })
                                }
                                add(CustomAccessibilityAction(copy.moveCoverLeft) {
                                    moveCover(-panStep, 0f)
                                })
                                add(CustomAccessibilityAction(copy.moveCoverRight) {
                                    moveCover(panStep, 0f)
                                })
                                add(CustomAccessibilityAction(copy.moveCoverUp) {
                                    moveCover(0f, -panStep)
                                })
                                add(CustomAccessibilityAction(copy.moveCoverDown) {
                                    moveCover(0f, panStep)
                                })
                                add(CustomAccessibilityAction(copy.resetCrop) {
                                    resetCrop()
                                    true
                                })
                            }
                        }
                        .onSizeChanged {
                            viewport = it
                            clampOffset()
                        }
                        .onKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) {
                                false
                            } else {
                                when (event.key) {
                                    Key.DirectionLeft -> moveCover(-panStep, 0f)
                                    Key.DirectionRight -> moveCover(panStep, 0f)
                                    Key.DirectionUp -> moveCover(0f, -panStep)
                                    Key.DirectionDown -> moveCover(0f, panStep)
                                    else -> false
                                }
                            }
                        }
                        .focusable()
                        .pointerInput(source, imageSize, viewport) {
                            detectTransformGestures { _, pan, gestureZoom, _ ->
                                zoom = (zoom * gestureZoom).coerceIn(1f, 4f)
                                offsetX += pan.x
                                offsetY += pan.y
                                clampOffset()
                            }
                        }
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(source)
                            .size(Size(2048, 2048))
                            .crossfade(true)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .diskCachePolicy(CachePolicy.DISABLED)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        onSuccess = { result ->
                            imageSize = IntSize(
                                result.result.image.width.coerceAtLeast(1),
                                result.result.image.height.coerceAtLeast(1)
                            )
                            clampOffset()
                        },
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                scaleX = zoom
                                scaleY = zoom
                                translationX = offsetX
                                translationY = offsetY
                            }
                    )
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.42f)),
                        modifier = Modifier.matchParentSize()
                    ) {}
                }
                Text(
                    text = copy.cropHint,
                    color = LevyraMuted,
                    fontSize = 12.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        enabled = imageSize.width > 0 && zoom > 1f,
                        onClick = { updateZoom(zoom - 0.25f) }
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = copy.zoomOut, tint = LevyraText)
                    }
                    Text(
                        text = "${(zoom * 100).roundToInt()}%",
                        color = LevyraText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    IconButton(
                        enabled = imageSize.width > 0 && zoom < 4f,
                        onClick = { updateZoom(zoom + 0.25f) }
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = copy.zoomIn, tint = LevyraText)
                    }
                    TextButton(
                        enabled = zoom != 1f || offsetX != 0f || offsetY != 0f,
                        onClick = ::resetCrop
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = null,
                            tint = LevyraText,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(copy.resetCrop, color = LevyraText, modifier = Modifier.padding(start = 6.dp))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text(strings.cancel) }
                    TextButton(
                        enabled = imageSize.width > 0 && viewport.width > 0,
                        onClick = {
                            onConfirm(
                                PlaylistCoverCrop(
                                    viewportSizePx = viewport.width,
                                    zoom = zoom,
                                    offsetX = offsetX,
                                    offsetY = offsetY
                                )
                            )
                        }
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = LevyraCyan, modifier = Modifier.size(18.dp))
                        Text(copy.confirmCover, color = LevyraCyan, modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
        }
    }
}
