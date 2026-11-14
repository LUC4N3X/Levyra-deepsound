package com.luc4n3x.levyra.desktop.core.stream

import com.luc4n3x.levyra.data.hqaudio.tidal.TidalDashManifest
import com.luc4n3x.levyra.data.hqaudio.tidal.TidalNetwork
import com.luc4n3x.levyra.player.offline.TidalDashFlac
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okhttp3.HttpUrl
import okhttp3.Request

object DashFlacDownload {
    suspend fun write(manifestFile: Path, output: Path, onProgress: (Long) -> Unit) {
        require(Files.size(manifestFile) <= 1_048_576L)
        val manifest = TidalDashManifest.parse(Files.readAllBytes(manifestFile))
        try {
            val header = TidalDashFlac.header(fetch(manifest.initialization))
            Files.newOutputStream(output).buffered().use { target ->
                target.write(header)
                var written = header.size.toLong()
                for (url in manifest.segmentUrls()) {
                    currentCoroutineContext().ensureActive()
                    val bytes = fetch(url)
                    if (written + bytes.size > 512L * 1024 * 1024) throw IOException("FLAC download exceeds size limit")
                    written += TidalDashFlac.writeFrames(bytes, target)
                    onProgress(written)
                }
            }
            DownloadedAudioValidation.requireFlac(output, manifest.durationMs)
        } catch (failure: Throwable) {
            Files.deleteIfExists(output)
            throw failure
        }
    }

    private suspend fun fetch(url: HttpUrl): ByteArray {
        val response = TidalNetwork.execute(Request.Builder().url(url).build(), 8 * 1024 * 1024)
        if (response.code == 403 || response.code == 410) throw StreamSourceExpiredException()
        if (!response.isSuccessful) throw IOException("FLAC segment HTTP ${response.code}")
        return response.body
    }
}
