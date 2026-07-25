package com.luc4n3x.levyra.desktop.player

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery

data class VlcDiscovery(
    val available: Boolean,
    val path: String,
    val searchedDirectories: List<String>
)

object VlcNativeLocator {
    private const val JNA_LIBRARY_PATH = "jna.library.path"

    fun discover(preferredDirectory: String = "", bundledDirectory: Path? = null): VlcDiscovery {
        val candidates = candidateDirectories(preferredDirectory, bundledDirectory)
        if (candidates.isNotEmpty()) {
            prependJnaLibraryPath(candidates)
        }
        val discovery = NativeDiscovery()
        val found = runCatching { discovery.discover() }.getOrDefault(false)
        return VlcDiscovery(
            available = found,
            path = if (found) discovery.discoveredPath().orEmpty() else "",
            searchedDirectories = candidates.map(Path::toString)
        )
    }

    fun candidateDirectories(preferredDirectory: String, bundledDirectory: Path?): List<Path> {
        val candidates = LinkedHashSet<Path>()
        preferredDirectory.trim().takeIf { it.isNotEmpty() }?.let { candidates.add(Paths.get(it)) }
        bundledDirectory?.let(candidates::add)
        System.getenv("LEVYRA_VLC_PATH")?.trim()?.takeIf { it.isNotEmpty() }?.let { candidates.add(Paths.get(it)) }
        System.getenv("VLC_HOME")?.trim()?.takeIf { it.isNotEmpty() }?.let { candidates.add(Paths.get(it)) }
        wellKnownDirectories().forEach(candidates::add)
        return candidates.filter { Files.isDirectory(it) }
    }

    private fun wellKnownDirectories(): List<Path> {
        val osName = System.getProperty("os.name").orEmpty().lowercase()
        if (!osName.contains("win")) return emptyList()
        val programFiles = System.getenv("ProgramFiles").orEmpty().ifBlank { "C:\\Program Files" }
        val programFilesX86 = System.getenv("ProgramFiles(x86)").orEmpty().ifBlank { "C:\\Program Files (x86)" }
        return listOf(
            Paths.get(programFiles, "VideoLAN", "VLC"),
            Paths.get(programFilesX86, "VideoLAN", "VLC")
        )
    }

    private fun prependJnaLibraryPath(directories: List<Path>) {
        val existing = System.getProperty(JNA_LIBRARY_PATH).orEmpty()
        val merged = LinkedHashSet<String>()
        directories.forEach { merged.add(it.toAbsolutePath().toString()) }
        existing.split(java.io.File.pathSeparator)
            .filter { it.isNotBlank() }
            .forEach(merged::add)
        System.setProperty(JNA_LIBRARY_PATH, merged.joinToString(java.io.File.pathSeparator))
    }
}
