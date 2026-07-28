package com.luc4n3x.levyra.desktop.app

internal object DesktopDiagnostics {

    fun background(operation: String, error: Throwable) {
        val reason = error.message?.takeIf { it.isNotBlank() } ?: error::class.simpleName.orEmpty()
        System.err.println("Levyra: $operation failed ($reason)")
    }
}
