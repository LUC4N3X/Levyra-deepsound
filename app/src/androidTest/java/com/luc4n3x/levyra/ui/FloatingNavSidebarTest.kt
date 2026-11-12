package com.luc4n3x.levyra.ui

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.luc4n3x.levyra.MainActivity
import com.luc4n3x.levyra.domain.FloatingNavSide
import com.luc4n3x.levyra.domain.LevyraFloatingNavSettings
import com.luc4n3x.levyra.domain.LevyraNavigationStudioSettings
import com.luc4n3x.levyra.domain.LevyraTab
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.floatingNav
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FloatingNavSidebarTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val strings = LevyraStrings.forCode("en")
    private val copy = strings.floatingNav()

    private class Host {
        var settings by mutableStateOf(LevyraFloatingNavSettings())
        var selected by mutableStateOf(LevyraTab.Home)
        var visible by mutableStateOf(true)
        val selections = mutableListOf<LevyraTab>()
    }

    private lateinit var activity: MainActivity

    private fun <T> main(action: () -> T): T {
        var result: T? = null
        instrumentation.runOnMainSync { result = action() }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    private fun composeRoot(view: View): ViewRootForTest? {
        if (view is ViewRootForTest) return view
        if (view is ViewGroup) {
            for (index in 0 until view.childCount) composeRoot(view.getChildAt(index))?.let { return it }
        }
        return null
    }

    private fun flatten(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::flatten)

    private fun nodes(): List<SemanticsNode> {
        val root = composeRoot(activity.window.decorView) ?: return emptyList()
        return flatten(root.semanticsOwner.rootSemanticsNode)
    }

    private fun exists(predicate: (SemanticsNode) -> Boolean): Boolean = main { nodes().any(predicate) }

    private fun await(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 5_000
        while (SystemClock.uptimeMillis() < deadline) {
            instrumentation.waitForIdleSync()
            if (condition()) return
            SystemClock.sleep(50)
        }
        assertTrue(message, condition())
    }

    private fun click(predicate: (SemanticsNode) -> Boolean) {
        val clicked = main {
            val node = nodes().first(predicate)
            node.config[SemanticsActions.OnClick].action?.invoke() == true
        }
        assertTrue(clicked)
    }

    private fun handle(label: String): (SemanticsNode) -> Boolean = {
        it.config.getOrNull(SemanticsProperties.ContentDescription) == listOf("${copy.handle}, $label")
    }

    private val collapseButton: (SemanticsNode) -> Boolean = {
        it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(copy.collapse) == true
    }

    private val isTab: (SemanticsNode) -> Boolean = {
        it.config.getOrNull(SemanticsProperties.Role) == Role.Tab
    }

    private fun tab(label: String): (SemanticsNode) -> Boolean = {
        isTab(it) && it.config.getOrNull(SemanticsProperties.Text)?.any { text -> text.text == label } == true
    }

    private val hasCustomActions: (SemanticsNode) -> Boolean = {
        it.config.getOrNull(SemanticsActions.CustomActions)?.any { action -> action.label == copy.moveDown } == true
    }

    private fun invokeCustomAction(label: String) {
        val handled = main {
            nodes().first(hasCustomActions).config[SemanticsActions.CustomActions].first { it.label == label }.action()
        }
        assertTrue(handled)
        instrumentation.waitForIdleSync()
    }

    private fun withSidebar(block: (Host) -> Unit) {
        val host = Host()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { launched ->
                activity = launched
                launched.setContent {
                    MaterialTheme {
                        CompositionLocalProvider(
                            LocalLevyraStrings provides strings,
                            LocalAnimationsEnabled provides false
                        ) {
                            Box(Modifier.size(400.dp, 800.dp)) {
                                FloatingNavSidebar(
                                    settings = host.settings,
                                    navigation = LevyraNavigationStudioSettings(),
                                    selected = host.selected,
                                    visible = host.visible,
                                    bottomClearancePx = { 0 },
                                    onSelect = {
                                        host.selected = it
                                        host.selections += it
                                    },
                                    onSettingsChange = { host.settings = it }
                                )
                            }
                        }
                    }
                }
            }
            await("Collapsed handle missing") { exists(handle(strings.home)) }
            block(host)
        }
    }

    private fun drag(fromLabel: String, dxPx: Float, dyPx: Float, cancel: Boolean = false) {
        val (startX, startY) = main {
            val bounds = nodes().first(handle(fromLabel)).boundsInWindow
            val origin = IntArray(2).also { activity.window.decorView.getLocationOnScreen(it) }
            origin[0] + bounds.center.x to origin[1] + bounds.center.y
        }
        val downTime = SystemClock.uptimeMillis()
        fun send(action: Int, x: Float, y: Float) {
            val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            instrumentation.sendPointerSync(event)
            event.recycle()
        }
        send(MotionEvent.ACTION_DOWN, startX, startY)
        val steps = 24
        for (step in 1..steps) {
            send(MotionEvent.ACTION_MOVE, startX + dxPx * step / steps, startY + dyPx * step / steps)
            SystemClock.sleep(8)
        }
        send(if (cancel) MotionEvent.ACTION_CANCEL else MotionEvent.ACTION_UP, startX + dxPx, startY + dyPx)
        instrumentation.waitForIdleSync()
    }

    private fun expand(label: String) {
        click(handle(label))
        await("Expanded panel missing") { exists(collapseButton) && exists(tab(strings.home)) }
    }

    @Test fun collapsedHandleExpandsIntoSelectableTabs() = withSidebar {
        assertTrue(main { nodes().first(handle(strings.home)).config.contains(SemanticsActions.OnClick) })
        expand(strings.home)
        val tabs = main {
            nodes().filter(isTab).map { node ->
                node.config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString { it.text } to
                    (node.config.getOrNull(SemanticsProperties.Selected) == true)
            }
        }
        assertTrue(tabs.size > 1)
        assertEquals(listOf(strings.home), tabs.filter { it.second }.map { it.first })
        assertTrue(tabs.any { it.first == strings.library && !it.second })
    }

    @Test fun selectingAnotherTabNotifiesAndCollapses() = withSidebar { host ->
        expand(strings.home)
        click(tab(strings.library))
        await("Panel did not collapse onto the new tab") {
            !exists(collapseButton) && exists(handle(strings.library))
        }
        assertEquals(listOf(LevyraTab.Library), main { host.selections.toList() })
        assertEquals(LevyraTab.Library, main { host.selected })
    }

    @Test fun collapseButtonClosesThePanel() = withSidebar { host ->
        expand(strings.home)
        click(collapseButton)
        await("Panel did not collapse") { !exists(collapseButton) && exists(handle(strings.home)) }
        assertTrue(main { host.selections.isEmpty() })
    }

    @Test fun accessibilityActionsMoveAndSwitchSide() = withSidebar { host ->
        val start = main { host.settings.anchor }
        assertEquals(LevyraFloatingNavSettings.DEFAULT_ANCHOR, start, 0.0001f)
        assertEquals(FloatingNavSide.Right, main { host.settings.side })

        invokeCustomAction(copy.moveDown)
        val lowered = main { host.settings.anchor }
        assertEquals(start + 0.12f, lowered, 0.001f)

        invokeCustomAction(copy.moveUp)
        val raised = main { host.settings.anchor }
        assertTrue(raised < lowered)
        assertEquals(start, raised, 0.001f)

        invokeCustomAction(copy.switchSide)
        assertEquals(FloatingNavSide.Left, main { host.settings.side })
        assertEquals(start, main { host.settings.anchor }, 0.001f)
    }

    @Test fun hiddenSidebarRemovesTheHandle() = withSidebar { host ->
        main { host.visible = false }
        await("Handle still present while hidden") { !exists(handle(strings.home)) }
        assertFalse(exists(collapseButton))
    }

    @Test fun draggingAcrossTheScreenSwitchesSideAndSavesPosition() = withSidebar { host ->
        val width = main { activity.window.decorView.width.toFloat() }
        val start = main { host.settings.anchor }
        drag(strings.home, dxPx = -width * 0.7f, dyPx = 160f)
        await("Drag did not move the sidebar to the left edge") { main { host.settings.side } == FloatingNavSide.Left }
        assertTrue(main { host.settings.anchor } > start)
        assertTrue(exists(handle(strings.home)))
        assertTrue(main { host.selections.isEmpty() })
    }

    @Test fun shortDragKeepsSideAndCancelledDragStillSaves() = withSidebar { host ->
        val start = main { host.settings.anchor }
        drag(strings.home, dxPx = -40f, dyPx = -160f, cancel = true)
        await("Cancelled drag was not saved") { main { host.settings.anchor } < start }
        assertEquals(FloatingNavSide.Right, main { host.settings.side })
        assertFalse(exists(collapseButton))
    }
}
