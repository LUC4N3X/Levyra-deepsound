package com.luc4n3x.levyra.desktop.app

import com.luc4n3x.levyra.desktop.app.ui.theme.LevyraChrome
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef.HWND
import java.awt.Color
import java.awt.Window

internal object WindowsWindowStyling {
    private const val DARK_MODE_ATTRIBUTE = 20
    private const val CORNER_PREFERENCE_ATTRIBUTE = 33
    private const val BORDER_COLOR_ATTRIBUTE = 34
    private const val CAPTION_COLOR_ATTRIBUTE = 35
    private const val TEXT_COLOR_ATTRIBUTE = 36
    private const val ROUNDED_CORNERS = 2
    private const val INT_SIZE = 4L

    private val isWindows: Boolean = System.getProperty("os.name")
        .orEmpty()
        .startsWith("Windows", ignoreCase = true)

    private val dwmApi: DwmApi by lazy {
        Native.load("dwmapi", DwmApi::class.java)
    }

    fun apply(window: Window, dark: Boolean) {
        val chrome = LevyraChrome.caption(dark)
        window.background = Color(chrome)
        if (!isWindows) return
        runCatching {
            val handle = HWND(Native.getWindowPointer(window) ?: return@runCatching)
            setInt(handle, DARK_MODE_ATTRIBUTE, if (dark) 1 else 0)
            setInt(handle, CORNER_PREFERENCE_ATTRIBUTE, ROUNDED_CORNERS)
            setInt(handle, CAPTION_COLOR_ATTRIBUTE, colorRef(chrome))
            setInt(handle, BORDER_COLOR_ATTRIBUTE, colorRef(LevyraChrome.border(dark)))
            setInt(handle, TEXT_COLOR_ATTRIBUTE, colorRef(LevyraChrome.captionText(dark)))
        }
    }

    private fun setInt(handle: HWND, attribute: Int, value: Int) {
        val memory = Memory(INT_SIZE)
        memory.setInt(0L, value)
        dwmApi.DwmSetWindowAttribute(handle, attribute, memory, INT_SIZE.toInt())
    }

    private fun colorRef(rgb: Int): Int {
        val red = rgb shr 16 and 0xFF
        val green = rgb shr 8 and 0xFF
        val blue = rgb and 0xFF
        return (blue shl 16) or (green shl 8) or red
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
