package com.luc4n3x.levyra.ui

import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import android.view.MotionEvent
import android.accessibilityservice.AccessibilityServiceInfo
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.luc4n3x.levyra.MainActivity
import com.luc4n3x.levyra.domain.RepeatMode
import com.luc4n3x.levyra.player.GentleWakeToneGenerator
import com.luc4n3x.levyra.player.PlaybackService
import com.luc4n3x.levyra.player.queue.PersistentQueueEngine
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.supportCopy
import com.luc4n3x.levyra.ui.player.LevyraNowPlaying
import com.luc4n3x.levyra.ui.player.PlayerCollapseActions
import com.luc4n3x.levyra.ui.player.rememberPlayerMorphAnchors
import com.luc4n3x.levyra.ui.theme.LevyraTheme
import com.luc4n3x.levyra.viewmodel.LevyraViewModel
import com.luc4n3x.levyra.viewmodel.PlayerViewModel
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerFocusDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    private fun <T> main(action: () -> T): T {
        var result: T? = null
        instrumentation.runOnMainSync { result = action() }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    private fun await(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 25_000
        while (SystemClock.uptimeMillis() < deadline) {
            find(instrumentation.uiAutomation.rootInActiveWindow) {
                it.viewIdResourceName?.endsWith(":id/permission_deny_button") == true
            }?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (condition()) return
            SystemClock.sleep(100)
        }
        instrumentation.uiAutomation.takeScreenshot()?.let { screenshot ->
            File(context.getExternalFilesDir(null), "player-focus-failure.png").outputStream().use {
                screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        assertTrue(message, condition())
    }

    private fun find(node: AccessibilityNodeInfo?, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (node == null) return null
        if (predicate(node)) return node
        for (index in 0 until node.childCount) find(node.getChild(index), predicate)?.let { return it }
        return null
    }

    private fun node(label: String) = find(instrumentation.uiAutomation.rootInActiveWindow) {
        it.contentDescription?.toString() == label || it.text?.toString() == label
    }

    private fun click(label: String) {
        await("Control not clickable: $label") {
            val found = node(label) ?: return@await false
            var target: AccessibilityNodeInfo? = found
            while (target != null && !target.isClickable) target = target.parent
            if (target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) true else {
                val bounds = android.graphics.Rect()
                found.getBoundsInScreen(bounds)
                if (bounds.isEmpty) false else {
                    val time = SystemClock.uptimeMillis()
                    listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP).forEach { action ->
                        val event = MotionEvent.obtain(time, time + action * 40L, action, bounds.exactCenterX(), bounds.exactCenterY(), 0)
                        instrumentation.uiAutomation.injectInputEvent(event, true)
                        event.recycle()
                    }
                    true
                }
            }
        }
    }

    @Test fun focusKeepsTheSharedPlayerAndRestoresTheNormalDeck() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("focusPlayback") == "true")
        val serviceInfo = instrumentation.uiAutomation.serviceInfo
        serviceInfo.flags = serviceInfo.flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        instrumentation.uiAutomation.serviceInfo = serviceInfo
        val fixture = GentleWakeToneGenerator.ensureToneTrack(context, "Focus fixture", "Device test")
        val tracks = (0..2).map { fixture.copy(id = "focus-fixture-$it", title = "Focus fixture $it", source = "Download") }
        val queue = PersistentQueueEngine.get(context)
        val preserved = runBlocking { queue.restore(emptyList(), -1, 0) }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var model: LevyraViewModel
            fun render() = scenario.onActivity { activity ->
                model = ViewModelProvider(activity)[LevyraViewModel::class.java]
                val playerModel = ViewModelProvider(activity, object : ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return PlayerViewModel(model) as T
                    }
                })[PlayerViewModel::class.java]
                activity.setContent {
                    val state by playerModel.state.collectAsStateWithLifecycle()
                    LevyraTheme {
                        CompositionLocalProvider(LocalLevyraStrings provides LevyraStrings.forCode(state.languageCode)) {
                            val anchors = rememberPlayerMorphAnchors { 1f }
                            val collapse = remember { PlayerCollapseActions({}, {}, {}, {}) }
                            LevyraNowPlaying(playerModel, state, anchors, { false }, collapse)
                        }
                    }
                }
            }
            render()
            val settings = model.state.value.interfaceSettings
            try {
                main {
                    queue.beginTransientPlayback(preserved, tracks, 0)
                    queue.setRepeatMode(RepeatMode.All)
                    model.setInterfaceSettings(settings.copy(playerFocusEnabled = false))
                    model.playAudioFrom(tracks, tracks.first())
                    model.openPlayerScreen()
                }
                await("Fixture must play") { main { PlaybackService.activePlayer?.isPlaying == true } }
                main { model.openPlayerScreen() }
                android.util.Log.i("FocusDeviceState", "tab=${model.state.value.selectedTab} onboarding=${model.state.value.showOnboarding} skin=${model.state.value.interfaceSettings.skin}")
                val strings = LevyraStrings.forCode(model.state.value.languageCode)
                val words = supportCopy(strings.code)
                main { model.seekTo(0f) }
                val player = main { PlaybackService.activePlayer }
                click(strings.more)
                click(words.focus)
                await("Focus mode missing") { node(words.exitFocus) != null }
                assertTrue(model.state.value.interfaceSettings.playerFocusEnabled)
                assertSame(player, main { PlaybackService.activePlayer })
                assertTrue(main { player?.isPlaying == true })
                click(strings.pause)
                await("Pause must win") { main { player?.playWhenReady == false } }
                click(strings.play)
                await("Playback must resume") { main { player?.isPlaying == true } }
                val slider = find(instrumentation.uiAutomation.rootInActiveWindow) {
                    it.rangeInfo != null && it.actionList.any { action -> action.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id }
                }
                assertNotNull("Shared seek control must remain accessible", slider)
                val range = slider!!.rangeInfo
                val seek = Bundle().apply { putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, (range.min + range.max) / 2f) }
                assertTrue(slider.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id, seek))
                await("Seek must reach player") { main { player!!.currentPosition >= 5_000 } }
                val first = main { player!!.currentMediaItem!!.mediaId }
                click(strings.next)
                await("Next must change track") { main { player!!.currentMediaItem?.mediaId != first && player.isPlaying } }
                click(strings.previous)
                await("Previous must return") { main { player!!.currentMediaItem?.mediaId == first && player.isPlaying } }
                val screenshot = instrumentation.uiAutomation.takeScreenshot()
                File(context.getExternalFilesDir(null), "player-focus-device.png").outputStream().use {
                    screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
                }
                click(words.exitFocus)
                await("Normal player must return") { !model.state.value.interfaceSettings.playerFocusEnabled && node(strings.more) != null }
                assertSame(player, main { PlaybackService.activePlayer })
                main { model.setInterfaceSettings(settings.copy(playerFocusEnabled = true)) }
                scenario.recreate()
                render()
                await("Focus preference must survive recreation") { node(words.exitFocus) != null }
                click(words.exitFocus)
            } finally {
                main {
                    PlaybackService.activePlayer?.pause()
                    model.setInterfaceSettings(settings)
                    queue.endTransientPlayback(preserved)
                }
            }
        }
    }
}
