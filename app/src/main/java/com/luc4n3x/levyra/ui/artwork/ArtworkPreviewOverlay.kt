package com.luc4n3x.levyra.ui.artwork

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.crossfade
import coil3.size.Size
import coil3.toBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

private const val PREVIEW_MAX_PX = 2048
private const val SAVE_QUALITY = 95
private const val FILE_NAME_ALLOWED_EXTRA = " -_"

@Composable
internal fun ArtworkPreviewOverlay(
    artworkUrl: String,
    title: String,
    previewLabel: String,
    closeLabel: String,
    saveLabel: String,
    savedMessage: String,
    saveFailedMessage: String,
    onFeedback: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    var artworkSize by remember { mutableStateOf(IntSize.Zero) }
    val scale = remember { mutableFloatStateOf(ARTWORK_PREVIEW_MIN_SCALE) }
    val offsetX = remember { mutableFloatStateOf(0f) }
    val offsetY = remember { mutableFloatStateOf(0f) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(artworkUrl) {
        scale.floatValue = ARTWORK_PREVIEW_MIN_SCALE
        offsetX.floatValue = 0f
        offsetY.floatValue = 0f
    }

    fun clampCurrentOffset() {
        val bounds = artworkPreviewFittedBounds(
            viewportWidth = viewport.width.toFloat(),
            viewportHeight = viewport.height.toFloat(),
            artworkWidth = artworkSize.width.toFloat(),
            artworkHeight = artworkSize.height.toFloat()
        )
        val clamped = artworkPreviewClampOffset(
            offset = ArtworkPreviewOffset(offsetX.floatValue, offsetY.floatValue),
            bounds = bounds,
            viewportWidth = viewport.width.toFloat(),
            viewportHeight = viewport.height.toFloat(),
            scale = scale.floatValue
        )
        offsetX.floatValue = clamped.x
        offsetY.floatValue = clamped.y
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.94f))
                .onSizeChanged { viewport = it }
                .pointerInput(artworkUrl) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale.floatValue = artworkPreviewClampScale(scale.floatValue * zoom)
                        offsetX.floatValue += pan.x
                        offsetY.floatValue += pan.y
                        clampCurrentOffset()
                    }
                }
                .pointerInput(artworkUrl) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale.floatValue > ARTWORK_PREVIEW_MIN_SCALE + 0.05f) {
                                scale.floatValue = ARTWORK_PREVIEW_MIN_SCALE
                                offsetX.floatValue = 0f
                                offsetY.floatValue = 0f
                            } else {
                                scale.floatValue = ARTWORK_PREVIEW_DOUBLE_TAP_SCALE
                            }
                            clampCurrentOffset()
                        }
                    )
                }
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(artworkUrl)
                    .size(Size(PREVIEW_MAX_PX, PREVIEW_MAX_PX))
                    .crossfade(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = previewLabel,
                contentScale = ContentScale.Fit,
                onSuccess = { state ->
                    artworkSize = IntSize(
                        state.result.image.width.coerceAtLeast(1),
                        state.result.image.height.coerceAtLeast(1)
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale.floatValue
                        scaleY = scale.floatValue
                        translationX = offsetX.floatValue
                        translationY = offsetY.floatValue
                    }
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .systemBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = closeLabel,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.86f),
                    maxLines = 1,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .semantics { contentDescription = previewLabel }
                )
                IconButton(
                    enabled = !saving,
                    onClick = {
                        saving = true
                        scope.launch {
                            val saved = try {
                                saveArtwork(context, artworkUrl, title)
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (error: Exception) {
                                Timber.w(error, "Artwork preview save failed")
                                false
                            }
                            saving = false
                            onFeedback(if (saved) savedMessage else saveFailedMessage)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = saveLabel,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

internal fun artworkPreviewFileName(title: String, timestampMs: Long): String {
    val builder = StringBuilder()
    for (character in title.trim()) {
        val allowed = character.isLetterOrDigit() || FILE_NAME_ALLOWED_EXTRA.contains(character)
        val next = if (allowed) character else ' '
        if (next == ' ' && builder.lastOrNull() == ' ') continue
        builder.append(next)
        if (builder.length >= 48) break
    }
    val safe = builder.toString().trim().ifBlank { "levyra" }
    return safe + "-" + timestampMs + ".jpg"
}

private suspend fun saveArtwork(context: Context, artworkUrl: String, title: String): Boolean {
    if (artworkUrl.isBlank()) return false
    val bitmap = withContext(Dispatchers.IO) {
        val request = ImageRequest.Builder(context)
            .data(artworkUrl)
            .size(Size(PREVIEW_MAX_PX, PREVIEW_MAX_PX))
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
        val result = SingletonImageLoader.get(context).execute(request)
        (result as? SuccessResult)?.image?.toBitmap()
    } ?: return false
    val fileName = artworkPreviewFileName(title, System.currentTimeMillis())
    return ArtworkMediaStore.save(context, fileName) { output ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, SAVE_QUALITY, output)
    }
}
