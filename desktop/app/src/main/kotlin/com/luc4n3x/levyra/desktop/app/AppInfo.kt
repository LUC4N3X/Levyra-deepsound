package com.luc4n3x.levyra.desktop.app

import java.util.Properties

object AppInfo {
    fun version(): String {
        val packaged = System.getProperty("jpackage.app-version").orEmpty().trim()
        if (packaged.isNotBlank()) return packaged

        val bundled = runCatching {
            AppInfo::class.java.classLoader
                .getResourceAsStream(VERSION_RESOURCE)
                ?.use { stream ->
                    Properties().apply { load(stream) }
                        .getProperty(VERSION_KEY)
                        .orEmpty()
                        .trim()
                }
                .orEmpty()
        }.getOrDefault("")

        return bundled.ifBlank { DEVELOPMENT_VERSION }
    }

    private const val VERSION_RESOURCE = "levyra-desktop-version.properties"
    private const val VERSION_KEY = "levyraDesktopVersion"
    private const val DEVELOPMENT_VERSION = "0.0.0-dev"
}
