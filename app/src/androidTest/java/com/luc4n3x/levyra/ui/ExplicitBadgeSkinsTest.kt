package com.luc4n3x.levyra.ui

import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.luc4n3x.levyra.MainActivity
import com.luc4n3x.levyra.domain.LevyraSkin
import com.luc4n3x.levyra.ui.components.LevyraMediaTitle
import com.luc4n3x.levyra.ui.components.LevyraRowText
import com.luc4n3x.levyra.ui.theme.LevyraTheme
import com.luc4n3x.levyra.ui.theme.LevyraThemeController
import com.luc4n3x.levyra.ui.theme.LevyraThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExplicitBadgeSkinsTest {
    @Test
    fun explicitBadgesStayVisibleAcrossEverySkinAndLongTitles() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        var skin by mutableStateOf(LevyraSkin.Default)
        val longTitle = "An explicit song with a title much longer than the available width"
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    LevyraTheme {
                      Surface {
                        Column(Modifier.width(220.dp).padding(top = 64.dp, start = 16.dp)) {
                            Text(skin.name)
                            LevyraMediaTitle(longTitle, explicit = true, modifier = Modifier.semantics { heading() })
                            LevyraRowText("Another long explicit song title in the library", "Artist", explicit = true)
                            LevyraMediaTitle("A song without an explicit badge", explicit = false)
                        }
                      }
                    }
                }
            }
            for (candidate in LevyraSkin.entries) {
                scenario.onActivity {
                    skin = candidate
                    LevyraThemeController.apply(LevyraThemes.COSMIC, skin = candidate)
                }
                val deadline = SystemClock.uptimeMillis() + 10_000
                var badges: List<AccessibilityNodeInfo>
                do {
                    instrumentation.waitForIdleSync()
                    val root = instrumentation.uiAutomation.rootInActiveWindow
                    badges = root?.let(::explicitNodes).orEmpty()
                    if (badges.size == 2 && root?.let { hasText(it, candidate.name) } == true) break
                    SystemClock.sleep(50)
                } while (SystemClock.uptimeMillis() < deadline)
                assertEquals("Explicit badges in ${candidate.name}", 2, badges.size)
                assertTrue("Skin ${candidate.name} rendered", instrumentation.uiAutomation.rootInActiveWindow?.let {
                    hasText(it, candidate.name)
                } == true)
                assertTrue(badges.all { it.isVisibleToUser })
                val titleNodes = badges.filter { it.contentDescription?.toString()?.startsWith(longTitle) == true }
                val headingEvidence = badges.joinToString { node ->
                    "${node.contentDescription}: heading=${node.isHeading}, parentHeading=${node.parent?.isHeading}"
                }
                assertTrue("Title keeps its heading semantics; matches=${titleNodes.size}; $headingEvidence", titleNodes.any { node ->
                    generateSequence(node) { it.parent }.any { it.isHeading }
                })
                val before = Rect().also { badges.first().getBoundsInScreen(it) }
                assertTrue(before.width() > 0 && before.height() > 0)
                if (candidate == LevyraSkin.Default) {
                    SystemClock.sleep(4_500)
                    val after = Rect().also {
                        explicitNodes(requireNotNull(instrumentation.uiAutomation.rootInActiveWindow)).first().getBoundsInScreen(it)
                    }
                    assertEquals("Badge must stay fixed while the title scrolls", before, after)
                }
            }
        }
    }

    private fun explicitNodes(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> = buildList {
        if (node.contentDescription?.toString()?.endsWith(", Explicit") == true) add(node)
        for (index in 0 until node.childCount) node.getChild(index)?.let { addAll(explicitNodes(it)) }
    }

    private fun hasText(node: AccessibilityNodeInfo, text: String): Boolean =
        node.text?.toString() == text || (0 until node.childCount).any { index ->
            node.getChild(index)?.let { hasText(it, text) } == true
        }
}
