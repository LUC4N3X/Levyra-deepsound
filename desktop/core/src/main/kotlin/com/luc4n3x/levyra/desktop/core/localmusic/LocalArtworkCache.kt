package com.luc4n3x.levyra.desktop.core.localmusic

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Locale

class LocalArtworkCache(private val directory: Path) {

    private val knownDirectoryCovers = HashMap<String, String>()

    fun prepare(): LocalArtworkCache {
        runCatching { Files.createDirectories(directory) }
        return this
    }

    fun store(artwork: EmbeddedArtwork): String {
        val extension = TagText.extensionFor(artwork.mimeType)
        if (extension.isEmpty()) return ""
        val name = "${digest(artwork.bytes)}.$extension"
        val target = directory.resolve(name)
        if (Files.isRegularFile(target)) return target.toString()
        var temporary: Path? = null
        return try {
            temporary = Files.createTempFile(directory, "$name.", ".tmp")
            Files.write(temporary, artwork.bytes)
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
            target.toString()
        } catch (_: Exception) {
            ""
        } finally {
            temporary?.let { runCatching { Files.deleteIfExists(it) } }
        }
    }

    fun folderCover(directoryPath: Path): String {
        val key = directoryPath.toString().lowercase(Locale.ROOT)
        knownDirectoryCovers[key]?.let { return it }
        val resolved = findFolderCover(directoryPath) ?: return ""
        knownDirectoryCovers[key] = resolved
        return resolved
    }

    fun clearDirectoryCache() {
        knownDirectoryCovers.clear()
    }

    private fun findFolderCover(directoryPath: Path): String? {
        val candidates = runCatching {
            Files.list(directoryPath).use { stream ->
                stream.filter { Files.isRegularFile(it) }
                    .limit(MAX_DIRECTORY_ENTRIES)
                    .toList()
            }
        }.getOrNull() ?: return null
        if (candidates.isEmpty()) return ""
        var best: Path? = null
        var bestRank = Int.MAX_VALUE
        candidates.forEach { candidate ->
            val name = candidate.fileName?.toString().orEmpty().lowercase(Locale.ROOT)
            val extension = name.substringAfterLast('.', "")
            if (extension !in IMAGE_EXTENSIONS) return@forEach
            val stem = name.substringBeforeLast('.', name)
            val rank = COVER_STEMS.indexOf(stem)
            if (rank >= 0 && rank < bestRank) {
                bestRank = rank
                best = candidate
            }
        }
        return best?.toString().orEmpty()
    }

    private fun digest(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.take(16).joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xFF) }
    }

    companion object {
        private const val MAX_DIRECTORY_ENTRIES = 400L
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "bmp")
        private val COVER_STEMS = listOf("cover", "folder", "front", "albumart", "album", "artwork")
    }
}
