package com.luc4n3x.levyra.ui

import android.os.Bundle
import android.os.SystemClock
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.luc4n3x.levyra.MainActivity
import com.luc4n3x.levyra.ui.i18n.LevyraOnboardingArtistStrings
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraTheme
import com.luc4n3x.levyra.viewmodel.LevyraViewModel
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingArtistsDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val italian = LevyraStrings.forCode("it")

    @Test
    fun artistSelectionSearchPortraitAndLanguageChange() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            instrumentation.uiAutomation.executeShellCommand(
                "pm grant ${instrumentation.targetContext.packageName} android.permission.POST_NOTIFICATIONS"
            ).use { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes() }
        }
        var chosen by mutableStateOf(emptySet<String>())
        var artworkFailed by mutableStateOf(false)
        var languageCode by mutableStateOf("it")
        lateinit var model: LevyraViewModel
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                model = ViewModelProvider(activity)[LevyraViewModel::class.java]
                activity.setContentView(ComposeView(activity).apply {
                    setContent {
                        val artwork by model.onboardingArtwork.collectAsStateWithLifecycle()
                        CompositionLocalProvider(LocalAnimationsEnabled provides false) {
                            LevyraTheme {
                                val colors = androidx.compose.material3.MaterialTheme.colorScheme
                                androidx.compose.foundation.layout.Column(
                                    androidx.compose.ui.Modifier.fillMaxSize().background(colors.surface).statusBarsPadding().navigationBarsPadding()
                                ) {
                                    androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.weight(1f)) {
                                        SetupArtistStage(
                                            languageCode, if (artworkFailed) chosen.associateWith { emptyList() } else artwork,
                                            chosen, colors.primary, colors.onSurface,
                                            colors.onSurfaceVariant, colors.surfaceContainerHigh, colors.onPrimary,
                                            model::loadOnboardingArtwork,
                                            { id -> chosen = if (id in chosen) chosen - id else chosen + id }
                                        )
                                    }
                                    androidx.compose.material3.Button(onClick = { languageCode = "fr" }) {
                                        androidx.compose.material3.Text("Français")
                                    }
                                }
                            }
                        }
                    }
                }, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            }
            click("Sfera Ebbasta")
            click("Lazza")
            click("Marracash")
            assertEquals(setOf("artist:Sfera Ebbasta", "artist:Lazza", "artist:Marracash"), chosen)
            scenario.onActivity { artworkFailed = true }
            click("Lazza")
            assertEquals(setOf("artist:Sfera Ebbasta", "artist:Marracash"), chosen)
            click("Lazza")
            scenario.onActivity { artworkFailed = false }
            search("beyonce")
            await { find("Beyoncé") != null }
            await(45_000) { !model.onboardingArtwork.value["artist:Beyoncé"].isNullOrEmpty() }
            instrumentation.waitForIdleSync()
            SystemClock.sleep(2_000)
            capture("beyonce-loaded")
            click("Beyoncé")
            search("zzzzzz-no-artist")
            await { find(LevyraOnboardingArtistStrings.forCode("it").empty) != null }
            capture("it-empty-search")
            click(italian.clear)
            click("Français")
            await { find(LevyraOnboardingArtistStrings.forCode("fr").title) != null }
            assertEquals(setOf("artist:Sfera Ebbasta", "artist:Lazza", "artist:Marracash", "artist:Beyoncé"), chosen)
            capture("fr-preserved")
        }
    }

    private fun search(value: String) {
        repeat(6) {
            if (findNode(instrumentation.uiAutomation.rootInActiveWindow) { it.isEditable && it.isVisibleToUser } == null) {
                scroll(forward = false)
            }
        }
        val field = requireNotNull(findNode(instrumentation.uiAutomation.rootInActiveWindow) { it.isEditable })
        field.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id)
        val arguments = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value) }
        assertTrue(field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments))
        SystemClock.sleep(600)
    }

    private fun find(label: String): AccessibilityNodeInfo? = findNode(instrumentation.uiAutomation.rootInActiveWindow) {
        it.text?.toString() == label || it.contentDescription?.toString() == label
    }

    private fun findCheckable(label: String): AccessibilityNodeInfo? {
        var node = find(label)
        while (node != null && !node.isCheckable) node = node.parent
        return node
    }

    private fun findNode(node: AccessibilityNodeInfo?, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (node == null) return null
        if (predicate(node)) return node
        for (index in 0 until node.childCount) findNode(node.getChild(index), predicate)?.let { return it }
        return null
    }

    private fun click(label: String) {
        await { find(label) != null }
        var node = requireNotNull(find(label))
        while (!node.isClickable && node.parent != null) node = node.parent
        assertTrue("Action failed: $label", node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        SystemClock.sleep(600)
    }

    private fun scroll(forward: Boolean) {
        val node = requireNotNull(findNode(instrumentation.uiAutomation.rootInActiveWindow) { it.isScrollable })
        node.performAction(if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
        SystemClock.sleep(600)
    }

    private fun await(timeout: Long = 15_000, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + timeout
        while (!condition() && SystemClock.uptimeMillis() < deadline) {
            findNode(instrumentation.uiAutomation.rootInActiveWindow) {
                it.viewIdResourceName == "com.android.permissioncontroller:id/permission_deny_button"
            }?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            SystemClock.sleep(200)
        }
        assertTrue("Expected onboarding UI did not appear", condition())
    }

    private fun capture(name: String) {
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "onboarding-artists-evidence").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
    }
}
