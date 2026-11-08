package com.luc4n3x.levyra.diagnostics

import com.luc4n3x.levyra.data.backupInterfaceSettingsFromJson
import com.luc4n3x.levyra.data.backupInterfaceSettingsToJson
import com.luc4n3x.levyra.domain.LevyraInterfaceSettings
import com.luc4n3x.levyra.domain.LevyraSkin
import com.luc4n3x.levyra.ui.i18n.supportCopy
import org.junit.Assert.*
import org.junit.Test

class PlayerFocusSettingsTest {
    @Test fun focusIsAnIndependentOptInForEverySkin() {
        LevyraSkin.entries.forEach { skin ->
            val original = LevyraInterfaceSettings(skin = skin)
            assertFalse(original.playerFocusEnabled)
            val focused = original.copy(playerFocusEnabled = true).normalized()
            assertTrue(focused.playerFocusEnabled)
            assertEquals(original, focused.copy(playerFocusEnabled = false))
        }
    }

    @Test fun backupRoundTripKeepsFocusAndLegacyBackupsStayOff() {
        val restored = backupInterfaceSettingsFromJson(backupInterfaceSettingsToJson(LevyraInterfaceSettings(playerFocusEnabled = true)))
        assertTrue(restored.playerFocusEnabled)
        assertFalse(backupInterfaceSettingsFromJson(org.json.JSONObject()).playerFocusEnabled)
    }

    @Test fun copyHasAnIntentionalFallbackAndTheRequestedItalianRecoveryMessage() {
        assertEquals(supportCopy("en"), supportCopy("unavailable"))
        assertEquals("Levyra si è chiusa inaspettatamente. Abbiamo preparato un report che può aiutare a risolvere il problema.", supportCopy("it").unexpectedClose)
    }

    @Test fun crashPresentationRoundTripsWithoutOpeningPreferences() {
        val expected = CrashPresentation("it", "Outfit", false, true, (0 until 12).map { it * 10 })
        assertEquals(expected, CrashPresentation.decode(expected.encode()))
        assertNull(CrashPresentation.decode("not json"))
        assertNull(CrashPresentation.decode("x".repeat(4_097)))
    }
}
