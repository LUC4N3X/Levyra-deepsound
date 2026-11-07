package com.luc4n3x.levyra.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeRefreshStabilityDeviceTest {
    @Test
    fun sectionTitleIdentityFoldsLatinMarksOnDeviceRegexEngine() {
        assertEquals("cafe-del-mar", HomeRefreshStability.sectionTitleIdentity("Café del Mar"))
        assertEquals("pho-viet-nam", HomeRefreshStability.sectionTitleIdentity("Phở Việt Nam"))
        assertEquals("nandu-angstrom", HomeRefreshStability.sectionTitleIdentity("Ñandú Ångström"))
        assertEquals("cafe-東京", HomeRefreshStability.sectionTitleIdentity("Café 東京"))
    }

    @Test
    fun sectionTitleIdentityKeepsNonLatinMarksOnDeviceRegexEngine() {
        assertNotEquals(
            HomeRefreshStability.sectionTitleIdentity("مُوسِيقَى"),
            HomeRefreshStability.sectionTitleIdentity("موسيقى")
        )
        assertNotEquals(
            HomeRefreshStability.sectionTitleIdentity("ガイド"),
            HomeRefreshStability.sectionTitleIdentity("カイド")
        )
        assertEquals("हिंदी-गाने", HomeRefreshStability.sectionTitleIdentity("हिंदी गाने"))
        assertEquals("เพลงใหม่", HomeRefreshStability.sectionTitleIdentity("เพลงใหม่"))
        assertEquals("인기-음악", HomeRefreshStability.sectionTitleIdentity("인기 음악"))
    }
}
