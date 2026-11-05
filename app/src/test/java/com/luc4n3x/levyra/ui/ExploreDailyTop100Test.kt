package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.ui.i18n.dailyTop100Copy
import com.luc4n3x.levyra.ui.i18n.dailyTop100LocalizationCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExploreDailyTop100Test {
    private val ids = listOf(
        "pl.737e067787df485a8062e2c4927d94db",
        "pl.d25f5d1181894928af76c85c967f8f31",
        "pl.606afcbb70264d2eb2b51d8dbcfa6a12",
        "pl.c2273b7e89b44121b3093f67228918e7",
        "pl.0d656d7feae64198bc5fb1b02786ed75",
        "pl.6e8cfd81d51042648fa36c9df5236b8d",
        "pl.11ac7cc7d09741c5822e8c66e5c7edbb",
        "pl.5437c1490ac74e9e9505fc7d1f201655"
    )

    @Test
    fun hueSlotsAreDeterministicDistinctAndInRange() {
        val slots = dailyTop100HueSlots(ids)
        assertEquals(slots, dailyTop100HueSlots(ids))
        assertEquals(ids.size, slots.toSet().size)
        assertTrue(slots.all { it in 0 until 12 })
        assertEquals(dailyTop100HueSlots(ids.take(1)), slots.take(1))
    }

    @Test
    fun hueSlotsStillAssignWhenMoreChartsThanSlots() {
        val many = (1..20).map { "pl.%032d".format(it) }
        val slots = dailyTop100HueSlots(many)
        assertEquals(20, slots.size)
        assertEquals(12, slots.take(12).toSet().size)
    }

    @Test
    fun everySelectableLanguageShipsDailyTop100Copy() {
        val selectable = LevyraLanguageCatalog.selectableLanguages.map { it.code }.toSet()
        assertEquals(selectable, dailyTop100LocalizationCodes())
        val english = dailyTop100Copy("en")
        selectable.forEach { code ->
            val copy = dailyTop100Copy(code)
            assertTrue(code, listOf(copy.title, copy.subtitle, copy.global, copy.failed, copy.retry).all(String::isNotBlank))
            assertFalse(code, copy.title.contains("Apple", ignoreCase = true) || copy.subtitle.contains("Apple", ignoreCase = true))
            if (code != "en") assertNotEquals(code, english.subtitle, copy.subtitle)
        }
        assertEquals("Top 100 · Oggi", dailyTop100Copy("it").title)
    }
}
