package com.luc4n3x.levyra.ui

import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.luc4n3x.levyra.MainActivity
import com.luc4n3x.levyra.domain.LevyraSkin
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.components.LevyraMediaTitle
import com.luc4n3x.levyra.ui.components.LevyraRowText
import com.luc4n3x.levyra.ui.theme.LevyraTheme
import com.luc4n3x.levyra.ui.theme.LevyraThemeController
import com.luc4n3x.levyra.ui.theme.LevyraThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExplicitBadgeSkinsTest {
    @Test
    fun searchRailsAnnounceExplicitAlongWithTitleAndArtist() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val track = Track(
            id = "explicit-test", title = "Rated song", artist = "Artist", album = "", durationMs = 180_000,
            streamUrl = "", videoUrl = "", thumbnailUrl = "", largeThumbnailUrl = "", source = "youtube",
            moodTags = emptySet(), energy = 0, vocal = 0, replayScore = 0, cacheScore = 0, accentStart = 0, accentEnd = 0,
            explicit = true
        )
        var rail by mutableStateOf(0)
        var rated by mutableStateOf(true)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    LevyraTheme {
                        Surface {
                            Column(Modifier.padding(top = 64.dp)) {
                                Text("Rail $rail $rated")
                                val tracks = listOf(track.copy(explicit = rated))
                                when (rail) {
                                    0 -> DefaultRecentSearchesRail("Recent", tracks, {}) { _, _, _ -> }
                                    1 -> DefaultSearchSamplesRail("Samples", tracks, false) {}
                                    2 -> IvoryRecentSearchesRail("Recent", tracks, {}) { _, _, _ -> }
                                    3 -> IvorySearchSamplesRail("Samples", tracks, false) {}
                                    4 -> TorqueRecentSearchesRail("Recent", tracks, {}) { _, _, _ -> }
                                    else -> TorqueSearchSamplesRail("Samples", tracks, false) {}
                                }
                            }
                        }
                    }
                }
            }
            for (candidate in 0..5) for (explicit in listOf(true, false)) {
                scenario.onActivity { rail = candidate; rated = explicit }
                val expected = "Rated song, Artist" + if (explicit) ", Explicit" else ""
                val deadline = SystemClock.uptimeMillis() + 5_000
                var found = false
                do {
                    instrumentation.waitForIdleSync()
                    val root = instrumentation.uiAutomation.rootInActiveWindow
                    found = root != null && hasText(root, "Rail $candidate $explicit") && hasDescription(root, expected)
                    if (!found) SystemClock.sleep(50)
                } while (!found && SystemClock.uptimeMillis() < deadline)
                assertTrue("Rail $candidate must announce $expected", found)
            }
        }
    }

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
                            CompositionLocalProvider(LocalAnimationsEnabled provides true, LocalLayoutDirection provides LayoutDirection.Ltr) {
                                LevyraMediaTitle(
                                    longTitle, explicit = true, color = Color.White, fontFamily = FontFamily.Monospace,
                                    fontSize = 20.sp, repeatDelayMillis = 100,
                                    modifier = Modifier.background(Color.Black).semantics { heading() }
                                )
                            }
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
                assertFixedBadgeAndMovingTitle(titleNodes.single())
            }
        }
    }

    private fun assertFixedBadgeAndMovingTitle(node: AccessibilityNodeInfo) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val density = instrumentation.targetContext.resources.displayMetrics.density
        val row = Rect().also(node::getBoundsInScreen)
        val badgeSize = (14 * density).toInt()
        val badge = Rect(row.left, row.centerY() - badgeSize / 2, row.left + badgeSize, row.centerY() - badgeSize / 2 + badgeSize)
        val title = Rect(row.left + (20 * density).toInt(), row.top, row.right, row.bottom)
        val badgeBefore = screenshotPixels(badge)
        val titleBefore = screenshotPixels(title)
        assertTrue("The explicit glyph must contain visible pixels", badgeBefore.any { android.graphics.Color.red(it) > 128 })
        val deadline = SystemClock.uptimeMillis() + 3_000
        var moved = false
        do {
            SystemClock.sleep(100)
            assertArrayEquals("The explicit glyph pixels must stay fixed", badgeBefore, screenshotPixels(badge))
            moved = !titleBefore.contentEquals(screenshotPixels(title))
        } while (!moved && SystemClock.uptimeMillis() < deadline)
        assertTrue("The overflowing title pixels must move", moved)
    }

    private fun screenshotPixels(bounds: Rect): IntArray {
        val bitmap = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        return try {
            IntArray(bounds.width() * bounds.height()).also {
                bitmap.getPixels(it, 0, bounds.width(), bounds.left, bounds.top, bounds.width(), bounds.height())
            }
        } finally {
            bitmap.recycle()
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

    private fun hasDescription(node: AccessibilityNodeInfo, text: String): Boolean =
        node.contentDescription?.toString() == text || (0 until node.childCount).any { index ->
            node.getChild(index)?.let { hasDescription(it, text) } == true
        }
}
