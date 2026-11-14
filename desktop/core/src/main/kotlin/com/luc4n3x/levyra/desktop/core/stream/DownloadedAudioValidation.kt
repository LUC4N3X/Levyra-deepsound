package com.luc4n3x.levyra.desktop.core.stream

import com.luc4n3x.levyra.data.hqaudio.FlacStreamInfo
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

object DownloadedAudioValidation {
    fun requireFlac(path: Path, durationMs: Long) {
        val header = Files.newInputStream(path).use { it.readNBytes(42) }
        val info = FlacStreamInfo.parse(header) ?: throw IOException("Il contenuto scaricato non è FLAC")
        if (durationMs > 0L && !info.durationMatches((durationMs / 1000L).toInt())) {
            throw IOException("La durata FLAC non corrisponde al brano")
        }
    }
}
