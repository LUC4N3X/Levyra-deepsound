package com.luc4n3x.levyra.ui

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeCollectionsLayoutContractTest {
    private fun source(file: String): String {
        val path = sequenceOf(
            Path.of("app/src/main/java/com/luc4n3x/levyra/ui/$file"),
            Path.of("src/main/java/com/luc4n3x/levyra/ui/$file")
        ).firstOrNull(Files::exists) ?: error("$file not found")
        return Files.readString(path)
    }

    private fun functionBlock(source: String, signature: String): String {
        val start = source.indexOf(signature)
        require(start >= 0) { "$signature not found" }
        val bodyStart = source.indexOf('{', start)
        require(bodyStart > start)
        var depth = 0
        for (index in bodyStart until source.length) {
            when (source[index]) {
                '{' -> depth += 1
                '}' -> {
                    depth -= 1
                    if (depth == 0) return source.substring(start, index + 1)
                }
            }
        }
        error("$signature body is not balanced")
    }

    @Test
    fun `collections shelf is a single responsive editorial row`() {
        val shelf = functionBlock(source("LevyraApp.kt"), "private fun HomeEditorialCollectionsShelf(")
        assertFalse(shelf.contains(".chunked(2)"))
        assertTrue(shelf.contains("LazyRow("))
        assertTrue(shelf.contains("items = collections"))
        assertTrue(shelf.contains("key = { collection ->"))
        assertTrue(shelf.contains("DiscoveryEditorialCard("))
        assertTrue(shelf.contains("maxWidth - LevyraHomeDesign.EditorialPeek"))
        assertTrue(shelf.contains("coerceAtMost(LevyraHomeDesign.EditorialMaxWidth)"))
    }

    @Test
    fun `editorial collection cards have bold tonal backgrounds and anchored artwork`() {
        val card = functionBlock(source("HomeExploreEditorial.kt"), "internal fun DiscoveryEditorialCard(")
        assertTrue(card.contains("levyraTonalRole(accentStart, isLight, maxChroma = 46.0)"))
        assertTrue(card.contains("editorialContrastPalette(backgroundColor, backgroundColor)"))
        assertTrue(card.contains(".background(contrastPalette.start)"))
        assertTrue(card.contains("levyraExpressiveCorner("))
        assertTrue(card.contains("rest = EditorialCardRestCorner"))
        assertTrue(card.contains("pressed = EditorialCardPressedCorner"))
        assertTrue(card.contains(".clip(RoundedCornerShape(corner))"))
        assertTrue(card.contains("interactionSource = interaction"))
        assertTrue(card.contains(".align(Alignment.TopStart)"))
        assertTrue(card.contains(".align(Alignment.BottomEnd)"))
        assertTrue(card.contains(".offset(x = 16.dp, y = 18.dp)"))
        assertTrue(card.contains(".rotate(12f)"))
        assertTrue(card.contains(".clip(artworkShape ?: LevyraCardDesign.ArtworkShape)"))
        assertTrue(card.contains(".levyraPressable("))
        assertTrue(card.contains("onClick = onOpen"))
        assertFalse(card.contains("Brush.linearGradient("))
        assertFalse(card.contains("Brush.radialGradient("))
        assertFalse(card.contains("accentEnd.copy(alpha"))
    }

    @Test
    fun `soundtrack hero uses expressive tonal colour and companion artists`() {
        val app = source("LevyraApp.kt")
        val hero = functionBlock(app, "private fun HomeEditorialSpotlight(")
        assertTrue(hero.contains("homeSoundtrackHeroTones(heroSeed, isLight)"))
        assertTrue(hero.contains("rememberArtworkPalette("))
        assertTrue(hero.contains("HomeSoundtrackCompanionStack("))
        assertTrue(hero.indexOf("HomeSoundtrackCompanionStack(") > hero.indexOf("text = soundtrackTitle"))
        assertTrue(hero.indexOf("HomeSoundtrackCompanionStack(") < hero.indexOf("text = soundtrackLead"))
        assertFalse(hero.contains("lerp(accentStart, Color.White"))
        val stack = functionBlock(app, "private fun HomeSoundtrackCompanionStack(")
        assertTrue(stack.contains("Spring.DampingRatioMediumBouncy"))
        assertTrue(stack.contains("if (!animate)"))
        assertTrue(stack.contains("clip(CircleShape)"))
    }

    @Test
    fun `collection shelf and detail share distinct harmonized accents`() {
        val app = source("LevyraApp.kt")
        val shelf = functionBlock(app, "private fun HomeEditorialCollectionsShelf(")
        val builder = functionBlock(app, "private fun buildHomeCollectionAccents(")
        assertTrue(builder.contains("collection.accentStart != 0 && collection.accentEnd != 0"))
        assertTrue(builder.contains("distinctEditorialAccents(seeds)"))
        assertTrue(app.contains("buildHomeCollectionAccents(visibleEditorialCollections)"))
        assertTrue(app.contains("accents = homeCollectionAccents"))
        assertTrue(app.contains("homeCollectionAccents[collection.id]"))
        assertTrue(shelf.contains("accents[collection.id]"))
        assertFalse(app.contains("collection.accentStart != collection.accentEnd"))
    }

    @Test
    fun `collection text wraps and stays separate from artwork on narrow screens`() {
        val editorial = source("HomeExploreEditorial.kt")
        val card = functionBlock(editorial, "internal fun DiscoveryEditorialCard(")
        val text = functionBlock(editorial, "private fun DiscoveryEditorialText(")
        assertTrue(card.contains("maxWidth < 300.dp"))
        assertTrue(card.contains("LocalDensity.current.fontScale > 1.4f"))
        assertTrue(card.contains("if (stacked)"))
        assertTrue(card.contains("if (artwork == null)"))
        assertTrue(card.contains("maxWidth - LevyraHomeDesign.EditorialPadding * 2"))
        assertTrue(card.contains("(maxWidth - artworkSize"))
        assertTrue(card.contains(".width(textWidth)"))
        assertTrue(card.contains(".align(Alignment.TopStart)"))
        assertTrue(card.contains("Arrangement.spacedBy(LevyraHomeDesign.EditorialPadding)"))
        assertTrue(text.contains("maxLines = 2"))
        assertTrue(text.contains("maxLines = 1"))
        assertTrue(text.contains("TextOverflow.Ellipsis"))
    }
}
