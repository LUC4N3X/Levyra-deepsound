package com.luc4n3x.levyra.ui.youtubemusic

import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.luc4n3x.levyra.MainActivity
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicAccount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class YoutubeMusicLibraryNavigationTest {
    @Test fun connectedLibraryShortcutInvokesAccountNavigation() {
        val calls = AtomicInteger()
        val laidOut = CountDownLatch(1)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var content: ComposeView
            scenario.onActivity { activity ->
                content = ComposeView(activity).apply {
                    addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                        if (view.width > 0 && view.height > 0) laidOut.countDown()
                    }
                    setContent {
                        MaterialTheme {
                            YoutubeMusicLibraryLink(YoutubeMusicAccount("test-account", "Test account", "@test", "", null, 0)) {
                                calls.incrementAndGet()
                            }
                        }
                    }
                }
                activity.setContentView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }
            assertTrue("Library shortcut did not lay out", laidOut.await(5, TimeUnit.SECONDS))
            instrumentation.waitForIdleSync()
            scenario.onActivity {
                assertTrue(content.width > 0)
                assertTrue(content.height > 0)
                val now = SystemClock.uptimeMillis()
                val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, content.width / 2f, content.height / 2f, 0)
                val up = MotionEvent.obtain(now, now + 50, MotionEvent.ACTION_UP, content.width / 2f, content.height / 2f, 0)
                try {
                    content.dispatchTouchEvent(down)
                    content.dispatchTouchEvent(up)
                } finally {
                    down.recycle()
                    up.recycle()
                }
            }
            instrumentation.waitForIdleSync()
            assertEquals(1, calls.get())
        }
    }
}
