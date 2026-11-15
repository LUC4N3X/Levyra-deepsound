package com.luc4n3x.levyra.desktop.app

import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

internal object DesktopPlatform {
    val isLinux: Boolean = System.getProperty("os.name").orEmpty().startsWith("Linux", true)
    val displayName: String = if (isLinux) "Linux" else "Windows"

    fun updateTarget(): DesktopUpdateTarget? {
        val os = System.getProperty("os.name").orEmpty()
        val arch = System.getProperty("os.arch").orEmpty()
        val distribution = if (isLinux) runCatching { Files.readString(Path.of("/etc/os-release")) }.getOrDefault("") else ""
        return DesktopUpdateTarget.detect(os, arch, distribution)
    }
}

internal data class DesktopUpdateTarget(val tagPrefix: String, val platform: String, val extension: String) {
    fun assetName(version: String): String = "LEVYRA-$platform-$version-x64.$extension"

    companion object {
        fun detect(os: String, arch: String, distribution: String = ""): DesktopUpdateTarget? {
            if (arch.lowercase(Locale.ROOT) !in setOf("amd64", "x86_64")) return null
            if (os.startsWith("Windows", true)) return DesktopUpdateTarget("desktop-v", "Windows", "msi")
            if (!os.startsWith("Linux", true)) return null
            val ids = distribution.lineSequence().filter { it.startsWith("ID=") || it.startsWith("ID_LIKE=") }
                .flatMap { it.substringAfter('=').replace("\"", "").lowercase(Locale.ROOT).split(' ').asSequence() }.toSet()
            val extension = when {
                ids.any { it in setOf("debian", "ubuntu", "linuxmint", "pop") } -> "deb"
                ids.any { it in setOf("fedora", "rhel", "centos", "suse", "opensuse", "rocky", "almalinux") } -> "rpm"
                else -> return null
            }
            return DesktopUpdateTarget("desktop-linux-v", "Linux", extension)
        }
    }
}
