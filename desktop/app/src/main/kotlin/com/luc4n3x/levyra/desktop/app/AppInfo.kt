package com.luc4n3x.levyra.desktop.app

object AppInfo {
    const val FALLBACK_VERSION = "1.0.0"

    fun version(): String {
        val packaged = System.getProperty("jpackage.app-version").orEmpty()
        return packaged.ifBlank { FALLBACK_VERSION }
    }
}
