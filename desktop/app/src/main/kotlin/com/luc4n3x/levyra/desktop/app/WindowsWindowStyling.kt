package com.luc4n3x.levyra.desktop.app

import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef.HWND
import java.awt.Color
import java.awt.Window

internal object WindowsWindowStyling {
    private const val DARK_MODE_ATTRIBUTE = 20
    private const val BOOLEAN_SIZE = 4L

    private val isWindows: Boolean = System.getProperty("os.name")
        .orEmpty()
        .startsWith("Windows", ignoreCase = true)

    private val dwmApi: DwmApi by lazy {
        Native.load("dwmapi", DwmApi::class.java)
    }

    fun apply(window: Window, dark: Boolean) {
        window.background = if (dark) {
            Color(7, 9, 13)
        } else {
            Color(245, 247, 250)
        }
        if (!isWindows) return
        runCatching {
            val windowPointer = Native.getWindowPointer(window) ?: return@runCatching
            val value = Memory(BOOLEAN_SIZE)
            value.setInt(0L, if (dark) 1 else 0)
            dwmApi.DwmSetWindowAttribute(
                HWND(windowPointer),
                DARK_MODE_ATTRIBUTE,
                value,
                BOOLEAN_SIZE.toInt()
            )
        }
    }

    private interface DwmApi : Library {
        fun DwmSetWindowAttribute(
            window: HWND,
            attribute: Int,
            value: Pointer,
            valueSize: Int
        ): Int
    }
}
