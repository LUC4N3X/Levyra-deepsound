package com.luc4n3x.levyra.diagnostics

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

internal class FatalCrashHandler(
    private val previous: Thread.UncaughtExceptionHandler?,
    private val capture: (Thread, Throwable) -> Unit,
    private val terminate: () -> Unit
) : Thread.UncaughtExceptionHandler {
    private val owner = AtomicReference<Thread?>()
    private val captured = CountDownLatch(1)

    override fun uncaughtException(thread: Thread, error: Throwable) {
        if (!owner.compareAndSet(null, Thread.currentThread())) {
            if (owner.get() !== Thread.currentThread()) {
                try {
                    captured.await(2, TimeUnit.SECONDS)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
            terminate()
            return
        }
        try {
            capture(thread, error)
        } catch (_: Throwable) {
        } finally {
            captured.countDown()
            try {
                previous?.uncaughtException(thread, error)
            } finally {
                terminate()
            }
        }
    }
}
