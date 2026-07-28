package com.luc4n3x.levyra.desktop.app

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import java.util.concurrent.atomic.AtomicBoolean

enum class MediaKeyAction {
    PLAY_PAUSE,
    NEXT,
    PREVIOUS,
    STOP
}

internal class WindowsMediaKeys(private val onAction: (MediaKeyAction) -> Unit) {

    private val running = AtomicBoolean(false)
    private var thread: Thread? = null

    @Volatile
    private var messageThreadId: Int = 0

    fun start(): Boolean {
        if (!isWindows) return false
        if (!running.compareAndSet(false, true)) return true
        val started = Thread({ pumpMessages() }, "levyra-media-keys").apply {
            isDaemon = true
            start()
        }
        thread = started
        return true
    }

    fun stop() {
        if (!running.compareAndSet(true, false)) return
        val threadId = messageThreadId
        if (threadId != 0) {
            runCatching { user32.PostThreadMessageW(threadId, WM_QUIT, null, null) }
        }
        thread = null
    }

    private fun pumpMessages() {
        messageThreadId = runCatching { kernel32.GetCurrentThreadId() }.getOrDefault(0)
        val registered = HOTKEYS.filter { (id, virtualKey) ->
            runCatching { user32.RegisterHotKey(null, id, 0, virtualKey) }.getOrDefault(false)
        }
        if (registered.isEmpty()) {
            running.set(false)
            messageThreadId = 0
            return
        }
        try {
            val message = WinUser.MSG()
            while (running.get()) {
                val result = user32.GetMessageW(message, null, 0, 0)
                if (result <= 0) break
                if (message.message == WM_HOTKEY) {
                    ACTIONS[message.wParam.toInt()]?.let { action ->
                        runCatching { onAction(action) }
                    }
                }
            }
        } finally {
            registered.keys.forEach { id -> runCatching { user32.UnregisterHotKey(null, id) } }
            messageThreadId = 0
            running.set(false)
        }
    }

    private interface User32Media : Library {
        fun RegisterHotKey(window: WinDef.HWND?, id: Int, modifiers: Int, virtualKey: Int): Boolean

        fun UnregisterHotKey(window: WinDef.HWND?, id: Int): Boolean

        fun GetMessageW(message: WinUser.MSG, window: WinDef.HWND?, filterMin: Int, filterMax: Int): Int

        fun PostThreadMessageW(
            threadId: Int,
            message: Int,
            wParam: WinDef.WPARAM?,
            lParam: WinDef.LPARAM?
        ): Boolean
    }

    private interface Kernel32Media : Library {
        fun GetCurrentThreadId(): Int
    }

    private companion object {
        const val WM_HOTKEY = 0x0312
        const val WM_QUIT = 0x0012

        const val HOTKEY_PLAY_PAUSE = 0xB301
        const val HOTKEY_NEXT = 0xB302
        const val HOTKEY_PREVIOUS = 0xB303
        const val HOTKEY_STOP = 0xB304

        const val VK_MEDIA_NEXT_TRACK = 0xB0
        const val VK_MEDIA_PREV_TRACK = 0xB1
        const val VK_MEDIA_STOP = 0xB2
        const val VK_MEDIA_PLAY_PAUSE = 0xB3

        val HOTKEYS = mapOf(
            HOTKEY_PLAY_PAUSE to VK_MEDIA_PLAY_PAUSE,
            HOTKEY_NEXT to VK_MEDIA_NEXT_TRACK,
            HOTKEY_PREVIOUS to VK_MEDIA_PREV_TRACK,
            HOTKEY_STOP to VK_MEDIA_STOP
        )

        val ACTIONS = mapOf(
            HOTKEY_PLAY_PAUSE to MediaKeyAction.PLAY_PAUSE,
            HOTKEY_NEXT to MediaKeyAction.NEXT,
            HOTKEY_PREVIOUS to MediaKeyAction.PREVIOUS,
            HOTKEY_STOP to MediaKeyAction.STOP
        )

        val isWindows: Boolean = System.getProperty("os.name")
            .orEmpty()
            .startsWith("Windows", ignoreCase = true)

        val user32: User32Media by lazy { Native.load("user32", User32Media::class.java) }
        val kernel32: Kernel32Media by lazy { Native.load("kernel32", Kernel32Media::class.java) }
    }
}
