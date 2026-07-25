package com.luc4n3x.levyra.desktop.core.storage

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class AppPaths(val root: Path) {

    val settingsFile: Path get() = root.resolve("settings.json")
    val libraryFile: Path get() = root.resolve("library.json")
    val sessionFile: Path get() = root.resolve("session.json")
    val windowFile: Path get() = root.resolve("window.json")
    val artworkCache: Path get() = root.resolve("cache").resolve("artwork")

    fun prepare(): AppPaths {
        Files.createDirectories(root)
        Files.createDirectories(artworkCache)
        return this
    }

    companion object {
        private const val APP_DIRECTORY = "Levyra"

        fun default(): AppPaths = AppPaths(defaultRoot()).prepare()

        fun defaultRoot(): Path {
            val osName = System.getProperty("os.name").orEmpty().lowercase()
            val home = System.getProperty("user.home").orEmpty()
            return when {
                osName.contains("win") -> {
                    val appData = System.getenv("APPDATA")
                    if (appData.isNullOrBlank()) {
                        Paths.get(home, "AppData", "Roaming", APP_DIRECTORY)
                    } else {
                        Paths.get(appData, APP_DIRECTORY)
                    }
                }

                osName.contains("mac") -> Paths.get(home, "Library", "Application Support", APP_DIRECTORY)

                else -> {
                    val xdg = System.getenv("XDG_DATA_HOME")
                    if (xdg.isNullOrBlank()) {
                        Paths.get(home, ".local", "share", APP_DIRECTORY)
                    } else {
                        Paths.get(xdg, APP_DIRECTORY)
                    }
                }
            }
        }
    }
}
