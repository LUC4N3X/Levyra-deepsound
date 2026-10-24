package com.luc4n3x.levyra.ui.experiences

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.luc4n3x.levyra.MainActivity
import com.luc4n3x.levyra.data.DownloadHealthRepository
import com.luc4n3x.levyra.data.RewindRepository
import com.luc4n3x.levyra.domain.LevyraSkin
import com.luc4n3x.levyra.domain.chapters
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.downloadhealth.DownloadHealthPresentation
import com.luc4n3x.levyra.ui.downloadhealth.DownloadHealthState
import com.luc4n3x.levyra.ui.downloadhealth.HealthActions
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.rewind.RewindPresentation
import com.luc4n3x.levyra.ui.rewind.RewindState
import com.luc4n3x.levyra.ui.theme.LevyraTheme
import com.luc4n3x.levyra.ui.theme.LevyraThemeController
import com.luc4n3x.levyra.ui.theme.LevyraThemes
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class MusicExperienceVisualTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val strings = LevyraStrings.forCode("en")

    @Test fun realHistoryChaptersAndAllSkinsRenderOnDevice() = runBlocking {
        val repository = RewindRepository(context)
        val year = repository.years().firstOrNull()
        assumeTrue("This visual run requires saved listening history", year != null)
        val summary = repository.summary(requireNotNull(year))
        assertTrue(summary.snapshot.hasSignal)
        val state = RewindState(listOf(year), year, summary, loading = false)
        val health = DownloadHealthRepository(context).scan { _, _ -> }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            show(scenario, LevyraSkin.Default) { RewindPresentation(state, {}, {}, {}) }
            summary.chapters().forEachIndexed { index, chapter ->
                capture("default-${index}-${chapter.name}")
                if (index < summary.chapters().lastIndex) click(strings.next)
            }
            LevyraSkin.entries.filter { it != LevyraSkin.Default }.forEach { skin ->
                show(scenario, skin) { RewindPresentation(state, {}, {}, {}) }
                capture("skin-${skin.name}-rewind")
                show(scenario, skin) {
                    DownloadHealthPresentation(DownloadHealthState(results = health, finished = true), {}, {}, HealthActions({}, {}, {}, {}, {}))
                }
                capture("skin-${skin.name}-health")
            }
            show(scenario, LevyraSkin.Default) {
                DownloadHealthPresentation(DownloadHealthState(results = health, finished = true), {}, {}, HealthActions({}, {}, {}, {}, {}))
            }
            capture("default-health")
            show(scenario, LevyraSkin.Default) { RewindPresentation(RewindState(loading = false), {}, {}, {}) }
            capture("rewind-empty")
            show(scenario, LevyraSkin.Default) { RewindPresentation(RewindState(loading = false, failed = true), {}, {}, {}) }
            capture("rewind-error")
            show(scenario, LevyraSkin.Default) {
                DownloadHealthPresentation(DownloadHealthState(finished = true), {}, {}, HealthActions({}, {}, {}, {}, {}))
            }
            capture("health-empty")
        }
    }

    private fun show(scenario: ActivityScenario<MainActivity>, skin: LevyraSkin, content: @Composable () -> Unit) {
        scenario.onActivity { activity ->
            LevyraThemeController.apply(LevyraThemes.cosmic.id, skin = skin)
            activity.setContentView(ComposeView(activity).apply {
                setContent {
                    CompositionLocalProvider(LocalLevyraStrings provides strings, LocalAnimationsEnabled provides true) {
                        LevyraTheme(content = content)
                    }
                }
            }, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        instrumentation.waitForIdleSync()
        SystemClock.sleep(1_200)
    }

    private fun capture(name: String) {
        instrumentation.waitForIdleSync()
        SystemClock.sleep(900)
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        val directory = File(context.getExternalFilesDir(null), "rewind-health-evidence").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
    }

    private fun click(label: String) {
        val node = find(instrumentation.uiAutomation.rootInActiveWindow, label)
        assertNotNull("Missing accessible action: $label", node)
        val bounds = android.graphics.Rect()
        node!!.getBoundsInScreen(bounds)
        val now = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, bounds.exactCenterX(), bounds.exactCenterY(), 0)
        val up = MotionEvent.obtain(now, now + 80, MotionEvent.ACTION_UP, bounds.exactCenterX(), bounds.exactCenterY(), 0)
        try { instrumentation.sendPointerSync(down); instrumentation.sendPointerSync(up) } finally { down.recycle(); up.recycle() }
        SystemClock.sleep(1_000)
    }

    private fun find(node: AccessibilityNodeInfo?, label: String): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.contentDescription?.toString() == label || node.text?.toString() == label) return node
        for (index in 0 until node.childCount) find(node.getChild(index), label)?.let { return it }
        return null
    }
}
