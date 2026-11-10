package com.luc4n3x.levyra.nexus.playlistimport

import com.luc4n3x.levyra.nexus.identity.MusicIdentityText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportTransliterationTest {
    @Test
    fun russianAndUkrainianRomanizationsShareOneKey() {
        listOf(
            "Обійми" to "Obiymy",
            "Обійми" to "Obijmy",
            "Океан Ельзи" to "Okean Elzy",
            "Київ" to "Kyiv",
            "Пам'ять" to "Pamyat",
            "Гайтана" to "Haitana",
            "Євген" to "Yevhen",
            "Виктор Цой" to "Viktor Tsoi",
            "Максим" to "Maxim",
            "Группа крови" to "Gruppa krovi",
            "Океан Эльзы" to "Океан Ельзи"
        ).forEach { (left, right) ->
            assertTrue("$left / $right", ImportTransliteration.sameRomanized(left, right))
        }
    }

    @Test
    fun romanizedKeysNeverMergeDifferentWordsOrLatinOnlyText() {
        assertFalse(ImportTransliteration.sameRomanized("Обійми", "Bez boyu"))
        assertFalse(ImportTransliteration.sameRomanized("Кино", "Kalush"))
        assertFalse(ImportTransliteration.sameRomanized("Gold", "Hold"))
        assertFalse(ImportTransliteration.sameRomanized("Obiymy", "Obiymy"))
        assertFalse(ImportTransliteration.sameRomanized("Ты", "Ty moya"))
    }

    @Test
    fun latinLookalikesInsideCyrillicWordsFoldToCyrillic() {
        assertEquals(MusicIdentityText.normalize("Обійми"), MusicIdentityText.normalize("Обiйми"))
        assertEquals(MusicIdentityText.normalize("Океан"), MusicIdentityText.normalize("Oкеaн"))
        assertEquals(MusicIdentityText.normalize("Максим"), MusicIdentityText.normalize("MakСим"))
        assertEquals("kalush orchestra", MusicIdentityText.normalize("Kalush Orchestra"))
        assertNotEquals(MusicIdentityText.normalize("Обійми"), MusicIdentityText.normalize("Obiymy"))
    }
}
