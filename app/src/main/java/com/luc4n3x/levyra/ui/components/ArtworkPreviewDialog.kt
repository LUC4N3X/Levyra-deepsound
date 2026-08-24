package com.luc4n3x.levyra.ui.components

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.toBitmap
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pure math helper to clamp zoom scale and pan offset so artwork never leaves the viewport.
 */
internal fun clampZoomPan(
    scale: Float,
    offset: Offset,
    contentSize: IntSize,
    minScale: Float = 1f,
    maxScale: Float = 4.5f
): Pair<Float, Offset> {
    val clampedScale = scale.coerceIn(minScale, maxScale)
    if (clampedScale <= 1.02f || contentSize.width <= 0 || contentSize.height <= 0) {
        return Pair(clampedScale, Offset.Zero)
    }
    val maxPanX = ((contentSize.width * (clampedScale - 1f)) / 2f).coerceAtLeast(0f)
    val maxPanY = ((contentSize.height * (clampedScale - 1f)) / 2f).coerceAtLeast(0f)
    val clampedOffset = Offset(
        x = offset.x.coerceIn(-maxPanX, maxPanX),
        y = offset.y.coerceIn(-maxPanY, maxPanY)
    )
    return Pair(clampedScale, clampedOffset)
}

/**
 * Immersive Cover Preview Dialog: provides pinch-to-zoom, panning, double-tap zoom/reset,
 * and media store cover saving.
 */
@Composable
fun ArtworkPreviewDialog(
    track: Track,
    artworkUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalLevyraStrings.current
    val scope = rememberCoroutineScope()

    var contentSize by remember { mutableStateOf(IntSize.Zero) }
    val scaleAnimatable = remember { Animatable(1f) }
    val offsetAnimatable = remember { Animatable(Offset.Zero, Offset.VectorConverter) }

    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(track.id, artworkUrl) {
        scaleAnimatable.snapTo(1f)
        offsetAnimatable.snapTo(Offset.Zero)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler(onBack = onDismiss)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF006060A))
                .onSizeChanged { contentSize = it }
        ) {
            // Main Zoomable Artwork Container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(track.id, contentSize) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val targetScale = (scaleAnimatable.value * zoom).coerceIn(1f, 4.5f)
                            val targetOffset = if (targetScale > 1.02f) {
                                offsetAnimatable.value + pan
                            } else {
                                Offset.Zero
                            }
                            val (clampedScale, clampedOffset) = clampZoomPan(
                                scale = targetScale,
                                offset = targetOffset,
                                contentSize = contentSize
                            )
                            scope.launch {
                                scaleAnimatable.snapTo(clampedScale)
                                offsetAnimatable.snapTo(clampedOffset)
                            }
                        }
                    }
                    .pointerInput(track.id, contentSize) {
                        detectTapGestures(
                            onDoubleTap = {
                                scope.launch {
                                    if (scaleAnimatable.value > 1.1f) {
                                        scaleAnimatable.animateTo(1f, tween(260))
                                        offsetAnimatable.animateTo(Offset.Zero, tween(260))
                                    } else {
                                        val (targetScale, targetOffset) = clampZoomPan(
                                            scale = 2.4f,
                                            offset = Offset.Zero,
                                            contentSize = contentSize
                                        )
                                        scaleAnimatable.animateTo(targetScale, tween(260))
                                        offsetAnimatable.animateTo(targetOffset, tween(260))
                                    }
                                }
                            },
                            onTap = {
                                if (scaleAnimatable.value <= 1.05f) {
                                    onDismiss()
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                val effectiveUrl = artworkUrl.ifBlank { track.largeThumbnailUrl.ifBlank { track.thumbnailUrl } }
                if (effectiveUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(effectiveUrl)
                            .crossfade(true)
                            .size(2048)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = track.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .graphicsLayer {
                                scaleX = scaleAnimatable.value
                                scaleY = scaleAnimatable.value
                                translationX = offsetAnimatable.value.x
                                translationY = offsetAnimatable.value.y
                            }
                    )
                }
            }

            // Top Header Bar with Close and Save Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = strings.close,
                        tint = Color.White
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = track.title.ifBlank { "Levyra" },
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (track.artist.isNotBlank()) {
                        Text(
                            text = track.artist,
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = {
                        if (!isSaving) {
                            isSaving = true
                            scope.launch {
                                saveArtworkToGallery(
                                    context = context,
                                    track = track,
                                    artworkUrl = artworkUrl.ifBlank { track.largeThumbnailUrl.ifBlank { track.thumbnailUrl } }
                                ) { success ->
                                    isSaving = false
                                    val message = if (success) strings.saved else strings.cannotOpenDownload
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = strings.save,
                        tint = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Saves artwork image cleanly to Android MediaStore gallery (Android 10+ scoped storage compliant).
 */
private suspend fun saveArtworkToGallery(
    context: Context,
    track: Track,
    artworkUrl: String,
    onComplete: (Boolean) -> Unit
) = withContext(Dispatchers.IO) {
    if (artworkUrl.isBlank()) {
        withContext(Dispatchers.Main) { onComplete(false) }
        return@withContext
    }
    val loader = ImageLoader(context)
    val request = ImageRequest.Builder(context)
        .data(artworkUrl)
        .size(2048)
        .allowHardware(false)
        .build()

    val result = loader.execute(request)
    val bitmap = (result as? SuccessResult)?.image?.toBitmap()
    if (bitmap == null) {
        withContext(Dispatchers.Main) { onComplete(false) }
        return@withContext
    }

    val safeArtist = track.artist.take(20).filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "artist" }
    val safeTitle = track.title.take(20).filter { it.isLetterOrDigit() || it == '_' }.ifBlank { "track" }
    val fileName = "Levyra_${safeArtist}_${safeTitle}_${System.currentTimeMillis()}.png"
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Levyra")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    if (uri == null) {
        withContext(Dispatchers.Main) { onComplete(false) }
        return@withContext
    }

    var written = false
    try {
        resolver.openOutputStream(uri)?.use { output ->
            written = bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }
    } catch (_: Exception) {
        written = false
        runCatching { resolver.delete(uri, null, null) }
    }

    withContext(Dispatchers.Main) {
        onComplete(written)
    }
}
