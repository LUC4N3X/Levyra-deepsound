package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.luc4n3x.levyra.desktop.app.di.AppContainer
import com.luc4n3x.levyra.desktop.core.catalog.AppleArtistCanvas
import com.luc4n3x.levyra.desktop.player.VlcArtistCanvas
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

@Composable
internal fun ArtistCanvas(id: String, artist: String, country: String, vlcDirectory: String, modifier: Modifier) {
    val frames = remember(id, artist, country) { MutableStateFlow<ImageBitmap?>(null) }
    val frame by frames.collectAsState()
    LaunchedEffect(frames, vlcDirectory) {
        var file: Path? = null
        var player: VlcArtistCanvas? = null
        try {
            file = AppleArtistCanvas.download(artist, country)
            val clip = file ?: return@LaunchedEffect
            withContext(Dispatchers.IO) {
                player = VlcArtistCanvas.open(clip, vlcDirectory, AppContainer.bundledVlcDirectory()) {
                    frames.value = it.toComposeImageBitmap()
                }
            }
            if (player != null) awaitCancellation()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            frames.value = null
        } catch (_: LinkageError) {
            frames.value = null
        } finally {
            withContext(Dispatchers.IO + NonCancellable) {
                try {
                    player?.close()
                } finally {
                    file?.let { Files.deleteIfExists(it) }
                    frames.value = null
                }
            }
        }
    }
    frame?.let { bitmap ->
        Image(bitmap, contentDescription = null, modifier, alignment = Alignment.Center, contentScale = ContentScale.Crop)
    }
}
