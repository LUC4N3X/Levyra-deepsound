package com.luc4n3x.levyra.ui.player

import androidx.compose.ui.graphics.vector.ImageVector
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerActionIconsTest {
    @Test
    fun favoriteIsAHeartOutsideTheApplePlayer() {
        assertEquals("Rounded.Favorite", LevyraPlayerActionIcons.favorite(true).name)
        assertEquals("Rounded.FavoriteBorder", LevyraPlayerActionIcons.favorite(false).name)
    }

    @Test
    fun applePlayerKeepsAppleGlyphs() {
        assertEquals("Rounded.Star", AppleMusicPlayerActionIcons.favorite(true).name)
        assertEquals("Rounded.StarBorder", AppleMusicPlayerActionIcons.favorite(false).name)
        assertEquals("Rounded.FastForward", AppleMusicPlayerActionIcons.next.name)
        assertEquals("Rounded.FastRewind", AppleMusicPlayerActionIcons.previous.name)
    }

    @Test
    fun everyActionHasItsOwnGlyph() {
        listOf(LevyraPlayerActionIcons, AppleMusicPlayerActionIcons).forEach { icons ->
            val glyphs = icons.glyphs()
            val shared = glyphs.groupBy { it.second.name }.filterValues { it.size > 1 }.mapValues { (_, v) -> v.map { it.first } }
            assertEquals(emptyMap<String, List<String>>(), shared)
        }
    }

    private fun PlayerActionIcons.glyphs(): List<Pair<String, ImageVector>> =
        PlayerActionIcons::class.java.declaredFields
            .filter { it.type == ImageVector::class.java }
            .map { field ->
                field.isAccessible = true
                field.name to field.get(this) as ImageVector
            }
}
