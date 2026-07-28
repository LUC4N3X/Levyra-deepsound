package com.luc4n3x.levyra.desktop.app

import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinUser
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

enum class MediaKeyAction {
    PLAY_PAUSE,
    NEXT,
    PREVIOUS,
    STOP
}

internal class WindowsMediaKeys(private val onAction: (MediaKeyAction) -> Unit) {

    private val running = AtomicBoolean(false)
    private val pumpReady = CountDownLatch(1)
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
        thread = null
        Thread({ quitMessageLoop() }, "levyra-media-keys-stop").apply {
            isDaemon = true
            start()
        }
    }

    private fun quitMessageLoop() {
        runCatching { pumpReady.await(PUMP_READY_TIMEOUT_MS, TimeUnit.MILLISECONDS) }
        val threadId = messageThreadId
        if (threadId == 0) return
        runCatching { User32.INSTANCE.PostThreadMessage(threadId, WM_QUIT, null, null) }
    }

    private fun pumpMessages() {
        messageThreadId = runCatching { Kernel32.INSTANCE.GetCurrentThreadId() }.getOrDefault(0)
        val registered = HOTKEYS.filter { (id, virtualKey) ->
            runCatching { User32.INSTANCE.RegisterHotKey(null, id, 0, virtualKey) }.getOrDefault(false)
        }
        if (registered.isEmpty()) {
            running.set(false)
            messageThreadId = 0
            pumpReady.countDown()
            return
        }
        pumpReady.countDown()
        try {
            val message = WinUser.MSG()
            while (running.get()) {
                val result = User32.INSTANCE.GetMessage(message, null, 0, 0)
                if (result <= 0) break
                if (message.message == WM_HOTKEY) {
                    ACTIONS[message.wParam.toInt()]?.let { action ->
                        runCatching { onAction(action) }
                    }
                }
            }
        } finally {
            registered.keys.forEach { id ->
                runCatching { User32.INSTANCE.UnregisterHotKey(null, id) }
            }
            messageThreadId = 0
            running.set(false)
        }
    }

    private companion object {
        const val PUMP_READY_TIMEOUT_MS = 2_000L
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
    }
}
