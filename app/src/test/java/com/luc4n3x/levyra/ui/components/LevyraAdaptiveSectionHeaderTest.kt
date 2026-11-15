package com.luc4n3x.levyra.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LevyraAdaptiveSectionHeaderTest {

    private fun arrange(width: Int, widest: Int, single: Int, action: Int, iconOnly: Int, gap: Int = 12) =
        sectionHeaderArrangement(width, gap, widest, single, action, iconOnly)

    @Test
    fun `short english title keeps the labelled action beside it`() {
        assertEquals(SectionHeaderArrangement.Inline, arrange(width = 328, widest = 70, single = 150, action = 110, iconOnly = 48))
    }

    @Test
    fun `title that wraps cleanly onto two lines keeps the labelled action`() {
        assertEquals(SectionHeaderArrangement.Inline, arrange(width = 328, widest = 95, single = 200, action = 100, iconOnly = 48))
    }

    @Test
    fun `long russian play all label collapses to its icon instead of starving the title`() {
        assertEquals(SectionHeaderArrangement.IconOnly, arrange(width = 328, widest = 95, single = 200, action = 190, iconOnly = 48))
    }

    @Test
    fun `title that cannot fit beside even the icon moves the action below`() {
        assertEquals(SectionHeaderArrangement.Stacked, arrange(width = 328, widest = 290, single = 290, action = 120, iconOnly = 48))
    }

    @Test
    fun `icon only action that never shrinks moves below when the title needs the row`() {
        assertEquals(SectionHeaderArrangement.Stacked, arrange(width = 328, widest = 300, single = 300, action = 48, iconOnly = 48))
    }

    @Test
    fun `empty action slot never changes the title layout`() {
        assertEquals(SectionHeaderArrangement.Inline, arrange(width = 200, widest = 400, single = 900, action = 0, iconOnly = 0, gap = 0))
    }

    @Test
    fun `beside arrangements always leave room for the widest word and two lines`() {
        val random = Random(1115)
        repeat(20_000) {
            val width = random.nextInt(120, 900)
            val widest = random.nextInt(10, 400)
            val single = widest + random.nextInt(0, 900)
            val action = random.nextInt(1, 300)
            val iconOnly = random.nextInt(1, action + 1)
            val gap = random.nextInt(0, 24)
            val result = sectionHeaderArrangement(width, gap, widest, single, action, iconOnly)
            val used = when (result) {
                SectionHeaderArrangement.Inline -> action
                SectionHeaderArrangement.IconOnly -> iconOnly
                SectionHeaderArrangement.Stacked -> return@repeat
            }
            val room = width - gap - used
            assertTrue("widest word must fit: $width $widest $single $action $iconOnly $gap", room >= widest)
            assertTrue("title must fit in two lines: $width $widest $single $action $iconOnly $gap", 2 * room >= single + widest)
        }
    }
}
